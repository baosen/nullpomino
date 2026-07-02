// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

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
 * Loopback-socket tests for {@link MeshTransport} and {@link MeshPeerLink}:
 * accept/dial, line delivery both ways, partial-packet reassembly,
 * close-once semantics, ephemeral fallback, and write-overflow.
 */
class MeshTransportTest {

    /** Records every sink event into inspectable queues */
    private static final class RecordingSink implements MeshEventSink {
        final BlockingQueue<MeshPeerLink> accepted = new LinkedBlockingQueue<MeshPeerLink>();
        final BlockingQueue<String> lines = new LinkedBlockingQueue<String>();
        final BlockingQueue<String> closes = new LinkedBlockingQueue<String>();
        final AtomicInteger closeCount = new AtomicInteger();

        public void onLinkAccepted(MeshPeerLink link) { accepted.add(link); }
        public void onLine(MeshPeerLink link, String line) { lines.add(line); }
        public void onLinkClosed(MeshPeerLink link, String reason) {
            closeCount.incrementAndGet();
            closes.add(reason);
        }
    }

    private MeshTransport transportA;
    private MeshTransport transportB;

    @AfterEach
    void tearDown() {
        if (transportA != null) transportA.shutdown();
        if (transportB != null) transportB.shutdown();
    }

    @Test
    void dialAcceptAndExchangeLines() throws Exception {
        RecordingSink sinkA = new RecordingSink();
        RecordingSink sinkB = new RecordingSink();
        transportA = new MeshTransport(sinkA);
        transportB = new MeshTransport(sinkB);
        int portA = transportA.startListening(0);

        MeshPeerLink bToA = transportB.dial("127.0.0.1", portA, 2000);
        MeshPeerLink aToB = sinkA.accepted.poll(5, TimeUnit.SECONDS);
        assertNotNull(aToB);
        assertTrue(bToA.outbound);
        assertFalse(aToB.outbound);

        bToA.sendLine("mesh\thello\tjoin\t7.5\tfalse\t9202\tSomeone");
        assertEquals("mesh\thello\tjoin\t7.5\tfalse\t9202\tSomeone",
                sinkA.lines.poll(5, TimeUnit.SECONDS));

        aToB.sendLine("mesh\tpeerok\t0");
        assertEquals("mesh\tpeerok\t0", sinkB.lines.poll(5, TimeUnit.SECONDS));
    }

    @Test
    void partialPacketsAreReassembled() throws Exception {
        RecordingSink sinkA = new RecordingSink();
        transportA = new MeshTransport(sinkA);
        int portA = transportA.startListening(0);

        try (Socket raw = new Socket("127.0.0.1", portA)) {
            OutputStream out = raw.getOutputStream();
            out.write("mesh\tpi".getBytes(StandardCharsets.UTF_8));
            out.flush();
            Thread.sleep(50);
            out.write("ng\nmesh\tpong\n".getBytes(StandardCharsets.UTF_8));
            out.flush();

            assertEquals("mesh\tping", sinkA.lines.poll(5, TimeUnit.SECONDS));
            assertEquals("mesh\tpong", sinkA.lines.poll(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void closeFiresExactlyOncePerSide() throws Exception {
        RecordingSink sinkA = new RecordingSink();
        RecordingSink sinkB = new RecordingSink();
        transportA = new MeshTransport(sinkA);
        transportB = new MeshTransport(sinkB);
        int portA = transportA.startListening(0);

        MeshPeerLink bToA = transportB.dial("127.0.0.1", portA, 2000);
        MeshPeerLink aToB = sinkA.accepted.poll(5, TimeUnit.SECONDS);
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
            transportA = new MeshTransport(sink);
            int bound = transportA.startListening(blocker.getLocalPort());

            assertTrue(bound > 0);
            assertNotEquals(blocker.getLocalPort(), bound);
            assertEquals(bound, transportA.getListenPort());
        }
    }

    @Test
    void writeQueueOverflowClosesLink() throws Exception {
        RecordingSink sinkA = new RecordingSink();
        transportA = new MeshTransport(sinkA);
        int portA = transportA.startListening(0);

        // Raw socket that never reads, so the peer's writes eventually queue up
        try (Socket raw = new Socket("127.0.0.1", portA)) {
            MeshPeerLink aLink = sinkA.accepted.poll(5, TimeUnit.SECONDS);
            assertNotNull(aLink);

            // Tiny queue via the package-private capacity: rebuild a link on the same
            // socket is not possible, so simulate overflow directly on a fresh pair.
            RecordingSink sinkC = new RecordingSink();
            try (ServerSocket server = new ServerSocket(0)) {
                Socket writerSide = new Socket("127.0.0.1", server.getLocalPort());
                try (Socket readerSide = server.accept()) {
                    MeshPeerLink tiny = new MeshPeerLink(writerSide, true, sinkC, 2);
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
