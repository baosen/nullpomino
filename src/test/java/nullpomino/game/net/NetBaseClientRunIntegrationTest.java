package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Integration tests for {@link NetBaseClient#run()} using a local
 * {@link ServerSocket} to exercise the connection, read-loop, and
 * disconnect paths.
 *
 * <p>Covers the lines in the run() method that are not exercised by
 * unit-level tests.
 */
class NetBaseClientRunIntegrationTest {

    private static final int TEST_PORT = 19420;

    // ─────────────────────────────────────────────────────────
    // Helper: a trivial server that sends one message and closes
    // ─────────────────────────────────────────────────────────

    /**
     * A simple one-shot server that accepts a connection, sends each
     * line from {@code responses} (terminated with {@code '\n'}),
     * then closes.
     */
    static class OneShotServer extends Thread {
        final int port;
        final String[] responses;
        final CountDownLatch accepted = new CountDownLatch(1);
        volatile boolean done;

        OneShotServer(int port, String... responses) {
            super("OneShotServer");
            this.port = port;
            this.responses = responses;
        }

        @Override
        public void run() {
            try (ServerSocket ss = new ServerSocket(port)) {
                ss.setSoTimeout(5000);
                accepted.countDown();
                try (Socket client = ss.accept()) {
                    OutputStream out = client.getOutputStream();
                    for (String r : responses) {
                        out.write((r + "\n").getBytes(StandardCharsets.UTF_8));
                        out.flush();
                    }
                    // Give the client a moment to read before closing
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                // Expected if interrupted or timeout
            } finally {
                done = true;
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    // Helper listener that captures events
    // ─────────────────────────────────────────────────────────

    static class CapturingListener implements NetMessageListener {
        final List<String[]> messages = new ArrayList<>();
        volatile Throwable disconnectReason;
        final CountDownLatch disconnected = new CountDownLatch(1);

        @Override
        public void netOnMessage(NetBaseClient client, String[] message) {
            messages.add(message);
        }

        @Override
        public void netOnDisconnect(NetBaseClient client, Throwable ex) {
            disconnectReason = ex;
            disconnected.countDown();
        }
    }

    // ─────────────────────────────────────────────────────────
    // Tests
    // ─────────────────────────────────────────────────────────

    @Test
    void clientConnectsReceivesMessagesAndDisconnects() throws Exception {
        OneShotServer server = new OneShotServer(TEST_PORT,
                "ping",
                "hello\tworld",
                "pong");
        server.start();

        // Wait for server to be ready
        assertTrue(server.accepted.await(5, TimeUnit.SECONDS),
                "Server should accept within timeout");

        NetBaseClient client = new NetBaseClient("127.0.0.1", TEST_PORT);
        CapturingListener listener = new CapturingListener();
        client.addListener(listener);

        // Start the client thread (calls run())
        client.start();

        // Wait for disconnect notification
        assertTrue(listener.disconnected.await(10, TimeUnit.SECONDS),
                "Client should disconnect within timeout");

        // Verify state after run() completes
        assertFalse(client.threadRunning);
        assertFalse(client.connectedFlag);
        assertNotNull(client.getIP());

        // Verify messages were received
        assertFalse(listener.messages.isEmpty(),
                "Client should have received at least one message");

        // The "ping" message should have reset pingCount
        assertEquals(0, client.pingCount);

        // Cleanup
        client.join(2000);
        server.join(2000);
    }

    @Test
    void clientHandlesConnectionRefused() throws Exception {
        // Use a port that nobody is listening on
        NetBaseClient client = new NetBaseClient("127.0.0.1", 19421);
        CapturingListener listener = new CapturingListener();
        client.addListener(listener);

        client.start();

        // Should disconnect quickly due to connection refused
        assertTrue(listener.disconnected.await(10, TimeUnit.SECONDS),
                "Client should disconnect after connection refused");

        assertFalse(client.threadRunning);
        assertFalse(client.connectedFlag);
        assertNotNull(listener.disconnectReason,
                "Disconnect reason should be set on connection failure");
    }

    @Test
    void clientWithMultipleMessagesAndPongProcessing() throws Exception {
        // Send several messages including pong to test pingCount reset
        OneShotServer server = new OneShotServer(TEST_PORT + 2,
                "pong",
                "msg1\tdata1",
                "pong",
                "msg2\tdata2\tdata3",
                "pong");
        server.start();

        assertTrue(server.accepted.await(5, TimeUnit.SECONDS),
                "Server should accept within timeout");

        NetBaseClient client = new NetBaseClient("127.0.0.1", TEST_PORT + 2);
        CapturingListener listener = new CapturingListener();
        client.addListener(listener);

        client.start();

        assertTrue(listener.disconnected.await(10, TimeUnit.SECONDS),
                "Client should disconnect within timeout");

        assertFalse(client.connectedFlag);
        assertEquals(0, client.pingCount);

        // Verify pong was processed (pingCount reset) and messages dispatched
        assertFalse(listener.messages.isEmpty());

        boolean foundMsg1 = false;
        boolean foundMsg2 = false;
        for (String[] msg : listener.messages) {
            if (msg[0].equals("msg1") && msg[1].equals("data1")) foundMsg1 = true;
            if (msg[0].equals("msg2") && msg[1].equals("data2") && msg[2].equals("data3")) foundMsg2 = true;
        }
        assertTrue(foundMsg1, "Should have received msg1");
        assertTrue(foundMsg2, "Should have received msg2");

        client.join(2000);
        server.join(2000);
    }

    @Test
    void clientWithPartialPacketsHandledCorrectly() throws Exception {
        // Send data that arrives in chunks that span packet boundaries
        OneShotServer server = new OneShotServer(TEST_PORT + 3,
                "first\tmessage\nsecond\t");  // second has no trailing \n
        server.start();

        assertTrue(server.accepted.await(5, TimeUnit.SECONDS));

        NetBaseClient client = new NetBaseClient("127.0.0.1", TEST_PORT + 3);
        CapturingListener listener = new CapturingListener();
        client.addListener(listener);

        client.start();

        assertTrue(listener.disconnected.await(10, TimeUnit.SECONDS),
                "Client should disconnect within timeout");

        // "first\tmessage" should have been delivered as a complete packet
        boolean foundFirst = false;
        for (String[] msg : listener.messages) {
            if (msg[0].equals("first") && msg[1].equals("message")) foundFirst = true;
        }
        assertTrue(foundFirst, "Should have received 'first' message");

        client.join(2000);
        server.join(2000);
    }

    @Test
    void listenerDisconnectCalledOnGracefulShutdown() throws Exception {
        OneShotServer server = new OneShotServer(TEST_PORT + 4);
        server.start();

        assertTrue(server.accepted.await(5, TimeUnit.SECONDS));

        NetBaseClient client = new NetBaseClient("127.0.0.1", TEST_PORT + 4);
        CapturingListener listener = new CapturingListener();
        client.addListener(listener);

        client.start();

        assertTrue(listener.disconnected.await(10, TimeUnit.SECONDS),
                "Client should disconnect when server closes");

        // When the server closes cleanly without sending data,
        // the read loop exits normally (read() returns -1),
        // so exDisconnectReason may be null.
        // The important thing is that the listener was notified.
        assertFalse(client.connectedFlag);
        assertFalse(client.threadRunning);

        client.join(2000);
        server.join(2000);
    }
}
