// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Loopback-socket tests for {@link TcpRoomTransport} and {@link RoomPeerLink}:
 * accept/dial, line delivery both ways, partial-packet reassembly,
 * close-once semantics, ephemeral fallback, dial failure, and write-overflow.
 */
class RoomTransportTest {

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

    /** Drive the async dial API synchronously for test convenience */
    private static RoomPeerLink dialSync(TcpRoomTransport transport, String host, int port)
            throws InterruptedException {
        final BlockingQueue<Object> result = new LinkedBlockingQueue<Object>();
        transport.dial(host, port, 2000, new RoomTransport.DialCallback() {
            public void onDialed(RoomLink link) { result.add(link); }
            public void onDialFailed(String reason) { result.add("FAILED: " + reason); }
        });
        Object outcome = result.poll(5, TimeUnit.SECONDS);
        assertTrue(outcome instanceof RoomPeerLink, String.valueOf(outcome));
        return (RoomPeerLink) outcome;
    }

    @Test
    void dialAcceptAndExchangeLines() throws Exception {
        RecordingSink sinkA = new RecordingSink();
        RecordingSink sinkB = new RecordingSink();
        transportA = new TcpRoomTransport(sinkA);
        transportB = new TcpRoomTransport(sinkB);
        int portA = transportA.startListening(0);

        RoomPeerLink bToA = dialSync(transportB, "127.0.0.1", portA);
        RoomLink aToB = sinkA.accepted.poll(5, TimeUnit.SECONDS);
        assertNotNull(aToB);
        assertTrue(bToA.outbound);
        assertFalse(((RoomPeerLink) aToB).outbound);

        bToA.sendLine("room\thello\tjoin\t7.5\tfalse\t9202\tSomeone");
        assertEquals("room\thello\tjoin\t7.5\tfalse\t9202\tSomeone",
                sinkA.lines.poll(5, TimeUnit.SECONDS));

        aToB.sendLine("room\tpeerok\t0");
        assertEquals("room\tpeerok\t0", sinkB.lines.poll(5, TimeUnit.SECONDS));
    }

    @Test
    void partialPacketsAreReassembled() throws Exception {
        RecordingSink sinkA = new RecordingSink();
        transportA = new TcpRoomTransport(sinkA);
        int portA = transportA.startListening(0);

        try (Socket raw = new Socket("127.0.0.1", portA)) {
            OutputStream out = raw.getOutputStream();
            out.write("room\tpi".getBytes(StandardCharsets.UTF_8));
            out.flush();
            Thread.sleep(50);
            out.write("ng\nroom\tpong\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            assertEquals("room\tping", sinkA.lines.poll(5, TimeUnit.SECONDS));
            assertEquals("room\tpong", sinkA.lines.poll(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void closeFiresExactlyOncePerSide() throws Exception {
        RecordingSink sinkA = new RecordingSink();
        RecordingSink sinkB = new RecordingSink();
        transportA = new TcpRoomTransport(sinkA);
        transportB = new TcpRoomTransport(sinkB);
        int portA = transportA.startListening(0);

        RoomPeerLink bToA = dialSync(transportB, "127.0.0.1", portA);
        RoomLink aToB = sinkA.accepted.poll(5, TimeUnit.SECONDS);
        assertNotNull(aToB);

        bToA.close("test close");
        bToA.close("second close is a no-op");
        assertNotNull(sinkB.closes.poll(5, TimeUnit.SECONDS));
        // A observes EOF from B's close
        assertNotNull(sinkA.closes.poll(5, TimeUnit.SECONDS));

        assertEquals(1, sinkB.closeCount.get());
        assertEquals(1, sinkA.closeCount.get());
        assertTrue(bToA.isClosed());
    }

    @Test
    void busyPortFallsBackToEphemeral() throws Exception {
        try (ServerSocket blocker = new ServerSocket(0)) {
            RecordingSink sink = new RecordingSink();
            transportA = new TcpRoomTransport(sink);
            int bound = transportA.startListening(blocker.getLocalPort());

            assertTrue(bound > 0);
            assertNotEquals(blocker.getLocalPort(), bound);
            assertEquals(bound, transportA.getListenPort());
        }
    }

    @Test
    void dialFailureReportsThroughCallback() throws Exception {
        RecordingSink sink = new RecordingSink();
        transportA = new TcpRoomTransport(sink);

        // A port that nothing listens on: grab an ephemeral port and close it
        int deadPort;
        try (ServerSocket probe = new ServerSocket(0)) {
            deadPort = probe.getLocalPort();
        }

        final BlockingQueue<String> failures = new LinkedBlockingQueue<String>();
        transportA.dial("127.0.0.1", deadPort, 1000, new RoomTransport.DialCallback() {
            public void onDialed(RoomLink link) { failures.add("UNEXPECTED SUCCESS"); }
            public void onDialFailed(String reason) { failures.add("failed"); }
        });
        assertEquals("failed", failures.poll(5, TimeUnit.SECONDS));
    }

    @Test
    void writeQueueOverflowClosesLink() throws Exception {
        RecordingSink sinkA = new RecordingSink();
        transportA = new TcpRoomTransport(sinkA);
        int portA = transportA.startListening(0);

        // Raw socket that never reads, so the peer's writes eventually queue up
        try (Socket raw = new Socket("127.0.0.1", portA)) {
            RoomLink aLink = sinkA.accepted.poll(5, TimeUnit.SECONDS);
            assertNotNull(aLink);

            // Tiny queue via the package-private capacity: rebuild a link on the same
            // socket is not possible, so simulate overflow directly on a fresh pair.
            RecordingSink sinkC = new RecordingSink();
            try (ServerSocket server = new ServerSocket(0)) {
                Socket writerSide = new Socket("127.0.0.1", server.getLocalPort());
                try (Socket readerSide = server.accept()) {
                    RoomPeerLink tiny = new RoomPeerLink(writerSide, true, sinkC, 2);
                    // Do NOT start() - without a writer thread the queue only fills
                    tiny.sendLine("one");
                    tiny.sendLine("two");
                    assertFalse(tiny.isClosed());
                    tiny.sendLine("three - overflow");
                    assertTrue(tiny.isClosed());
                    assertEquals("write queue overflow", sinkC.closes.poll(5, TimeUnit.SECONDS));
                }
            }
        }
    }
}
