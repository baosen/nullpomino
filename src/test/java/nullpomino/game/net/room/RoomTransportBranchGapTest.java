// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link TcpRoomTransport} and {@link RoomPeerLink}:
 * read/write error close reasons, flush-close queue overflow, unconnected
 * sockets, unexpected listen-socket death, and shutdown observed from
 * inside a dial callback.
 */
class RoomTransportBranchGapTest {

    /** Records every sink event into inspectable queues */
    private static final class RecordingSink implements RoomEventSink {
        final BlockingQueue<RoomLink> accepted = new LinkedBlockingQueue<RoomLink>();
        final BlockingQueue<String> lines = new LinkedBlockingQueue<String>();
        final BlockingQueue<String> closes = new LinkedBlockingQueue<String>();
        final AtomicInteger closeCount = new AtomicInteger();

        public void onLinkAccepted(RoomLink link) { accepted.add(link); }
        public void onLine(RoomLink link, String line) { lines.add(line); }
        public void onLinkClosed(RoomLink link, String reason) {
            closeCount.incrementAndGet();
            closes.add(reason);
        }
    }

    private TcpRoomTransport transportA;
    private TcpRoomTransport transportB;

    @AfterEach
    void tearDown() {
        if (transportA != null) transportA.shutdown();
        if (transportB != null) transportB.shutdown();
    }

    // ---------------------------------------------------------------- RoomPeerLink

    @Test
    void unconnectedSocketHasPlaceholderRemoteAddress() throws Exception {
        try (Socket unconnected = new Socket()) {
            RoomPeerLink link = new RoomPeerLink(unconnected, true, new RecordingSink());
            assertEquals("?", link.getRemoteAddress());
        }
    }

    @Test
    void closeAfterFlushFallsBackToImmediateCloseWhenQueueIsFull() throws Exception {
        RecordingSink sink = new RecordingSink();
        try (ServerSocket server = new ServerSocket(0);
             Socket writerSide = new Socket("127.0.0.1", server.getLocalPort());
             Socket readerSide = server.accept()) {
            // Capacity 1, never started: the queue can only fill
            RoomPeerLink link = new RoomPeerLink(writerSide, true, sink, 1);
            link.sendLine("fills the queue");
            assertFalse(link.isClosed());

            link.closeAfterFlush("farewell");
            assertTrue(link.isClosed(), "no room for the flush marker: closed immediately");
            assertEquals("farewell", sink.closes.poll(5, TimeUnit.SECONDS));

            // And once closed, closeAfterFlush is a no-op
            link.closeAfterFlush("again");
            assertEquals(1, sink.closeCount.get());
        }
    }

    @Test
    void connectionResetClosesWithReadErrorReason() throws Exception {
        RecordingSink sink = new RecordingSink();
        try (ServerSocket server = new ServerSocket(0)) {
            Socket peerSide = new Socket("127.0.0.1", server.getLocalPort());
            Socket linkSide = server.accept();
            RoomPeerLink link = new RoomPeerLink(linkSide, false, sink);
            link.start();

            // SO_LINGER 0 makes close() send RST: the blocked read fails hard
            peerSide.setSoLinger(true, 0);
            peerSide.close();

            String reason = sink.closes.poll(5, TimeUnit.SECONDS);
            assertNotNull(reason);
            assertTrue(reason.startsWith("read error: "), reason);
            assertTrue(link.isClosed());
        }
    }

    @Test
    void failedWriteClosesWithWriteErrorReason() throws Exception {
        RecordingSink sink = new RecordingSink();
        try (ServerSocket server = new ServerSocket(0);
             Socket writerSide = new Socket("127.0.0.1", server.getLocalPort());
             Socket readerSide = server.accept()) {
            RoomPeerLink link = new RoomPeerLink(writerSide, true, sink);
            link.start();

            // The writer's next flush fails while the link still counts as open
            writerSide.shutdownOutput();
            link.sendLine("doomed");

            String reason = sink.closes.poll(5, TimeUnit.SECONDS);
            assertNotNull(reason);
            assertTrue(reason.startsWith("write error: "), reason);
        }
    }

    @Test
    void closeWhileWriterIsBlockedReportsTheCloseReasonOnce() throws Exception {
        RecordingSink sink = new RecordingSink();
        try (ServerSocket server = new ServerSocket(0)) {
            Socket writerSide = new Socket("127.0.0.1", server.getLocalPort());
            writerSide.setSendBufferSize(8192);
            try (Socket readerSide = server.accept()) {
                readerSide.setReceiveBufferSize(8192);
                RoomPeerLink link = new RoomPeerLink(writerSide, true, sink);
                link.start();

                // The peer never reads: enough data jams the writer inside write()
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < 8192; i++) sb.append('x');
                String bigLine = sb.toString();
                for (int i = 0; i < 500; i++) link.sendLine(bigLine);
                Thread.sleep(300);

                // Closing the socket under the blocked writer: the resulting write
                // error is attributed to the close, not reported separately
                link.close("test close");
                assertEquals("test close", sink.closes.poll(5, TimeUnit.SECONDS));
                Thread.sleep(200);
                assertEquals(1, sink.closeCount.get(), "the writer's IOException fires no second close");
            }
        }
    }

    // ---------------------------------------------------------------- TcpRoomTransport

    @Test
    void listenPortIsMinusOneBeforeStartListening() {
        transportA = new TcpRoomTransport(new RecordingSink());
        assertEquals(-1, transportA.getListenPort());
        assertNotNull(transportA.getDisplayAddress());
    }

    @Test
    void acceptLoopWarnsWhenTheListenSocketDiesUnexpectedly() throws Exception {
        RecordingSink sink = new RecordingSink();
        transportA = new TcpRoomTransport(sink);
        int port = transportA.startListening(0);

        // Kill the listen socket WITHOUT requesting shutdown: the accept loop
        // must take its "unexpected stop" exit
        Field field = TcpRoomTransport.class.getDeclaredField("serverSocket");
        field.setAccessible(true);
        ((ServerSocket) field.get(transportA)).close();

        // The listener is gone: fresh connects are refused
        assertThrows(ConnectException.class, () -> {
            try (Socket probe = new Socket("127.0.0.1", port)) {
                fail("connect to " + probe.getPort() + " should have been refused");
            }
        });
    }

    @Test
    void dialerLoopExitsWhenShutdownArrivesInsideACallback() throws Exception {
        RecordingSink sinkA = new RecordingSink();
        RecordingSink sinkB = new RecordingSink();
        transportA = new TcpRoomTransport(sinkA);
        transportB = new TcpRoomTransport(sinkB);
        int portA = transportA.startListening(0);

        final BlockingQueue<RoomLink> dialed = new LinkedBlockingQueue<RoomLink>();
        transportB.dial("127.0.0.1", portA, 2000, new RoomTransport.DialCallback() {
            public void onDialed(RoomLink link) {
                // Shutdown from inside the dial job: the dialer loop must see the
                // flag when the job returns and exit without a pending take()
                transportB.shutdown();
                dialed.add(link);
            }
            public void onDialFailed(String reason) { throw new AssertionError(reason); }
        });

        RoomLink link = dialed.poll(5, TimeUnit.SECONDS);
        assertNotNull(link);
        assertTrue(link.isClosed(), "shutdown closed the freshly dialed link");
        assertEquals(RoomProtocol.DENY_SHUTDOWN, sinkB.closes.poll(5, TimeUnit.SECONDS));
    }

    // A BindException on an ephemeral bind (startListening(0)) and the
    // NetworkInterface-dependent arms of getDisplayAddress() need a machine
    // with no free ports / a specific NIC layout: not reachable hermetically.
}
