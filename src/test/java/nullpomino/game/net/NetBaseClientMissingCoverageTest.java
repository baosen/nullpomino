package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Tests targeting uncovered lines in {@link NetBaseClient}:
 * lines 139-140, 178, 182-183, 193, 197-198, 275-279, 281-282, 284-285, 292-294.
 *
 * <p>Uses reflection and local ServerSocket where needed.
 */
class NetBaseClientMissingCoverageTest {

    private static final int TEST_PORT = 19430;

    // ─── Helper: one-shot server ─────────────────────────────────────

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
                    Thread.sleep(100);
                }
            } catch (Exception e) {
                // Expected on interrupt/timeout
            } finally {
                done = true;
            }
        }
    }

    // ─── Helper: listener that throws on disconnect ──────────────────

    static class ThrowingOnDisconnectListener implements NetMessageListener {
        final List<String[]> messages = new ArrayList<>();

        @Override
        public void netOnMessage(NetBaseClient client, String[] message) {
            messages.add(message);
        }

        @Override
        public void netOnDisconnect(NetBaseClient client, Throwable ex) {
            throw new RuntimeException("Intentional disconnect exception");
        }
    }

    // ─── Helper: listener that records disconnect calls ──────────────

    static class CaptureDisconnectListener implements NetMessageListener {
        volatile boolean disconnectedCalled;
        volatile Throwable disconnectReason;

        @Override
        public void netOnMessage(NetBaseClient client, String[] message) {
        }

        @Override
        public void netOnDisconnect(NetBaseClient client, Throwable ex) {
            disconnectedCalled = true;
            disconnectReason = ex;
        }
    }

    // ─── Helper: connect client to a local server ────────────────────

    /** Connects a NetBaseClient to a local server on the given port.
     *  Returns the started server for cleanup. */
    static OneShotServer connectClient(NetBaseClient client, int port,
                                        String... responses) throws Exception {
        OneShotServer server = new OneShotServer(port, responses);
        server.start();
        assertTrue(server.accepted.await(5, TimeUnit.SECONDS),
                "Server should accept within timeout");

        // Use reflection to set socket directly (simpler than starting client thread)
        Socket socket = new Socket("127.0.0.1", port);
        Field socketField = NetBaseClient.class.getDeclaredField("socket");
        socketField.setAccessible(true);
        socketField.set(client, socket);
        client.connectedFlag = true;
        client.ip = socket.getInetAddress().getHostAddress();

        return server;
    }

    // ─────────────────────────────────────────────────────────────────
    // Lines 139-140: listener's netOnDisconnect throws
    // Path: run() → disconnect → listener notification → catch (Exception e2)
    // ─────────────────────────────────────────────────────────────────

    @Test
    void line139_listenerDisconnectException() throws Exception {
        // Start a server that will hang up quickly
        OneShotServer server = new OneShotServer(TEST_PORT);
        server.start();
        assertTrue(server.accepted.await(5, TimeUnit.SECONDS));

        NetBaseClient client = new NetBaseClient("127.0.0.1", TEST_PORT);
        ThrowingOnDisconnectListener listener = new ThrowingOnDisconnectListener();
        client.addListener(listener);

        // Start client thread (calls run(), connects, reads, then disconnects)
        client.start();

        // Wait for client to finish (server closes after accept)
        client.join(10000);

        assertFalse(client.threadRunning);
        assertFalse(client.connectedFlag);

        server.join(2000);
    }

    // ─────────────────────────────────────────────────────────────────
    // Lines 178, 182-183: send(byte[]) success path
    // socket.getOutputStream().write(bytes) → return true
    // ─────────────────────────────────────────────────────────────────

    @Test
    void line178_sendBytesSuccess() throws Exception {
        NetBaseClient client = new NetBaseClient();
        OneShotServer server = connectClient(client, TEST_PORT + 1);

        boolean result = client.send(new byte[]{0x68, 0x65, 0x6c, 0x6c, 0x6f}); // "hello"

        assertTrue(result, "send(byte[]) should return true on success");

        // Cleanup
        client.socket.close();
        server.join(2000);
    }

    // ─────────────────────────────────────────────────────────────────
    // Lines 193, 197-198: send(String) success path
    // socket.getOutputStream().write(NetUtil.stringToBytes(msg)) → return true
    // ─────────────────────────────────────────────────────────────────

    @Test
    void line193_sendStringSuccess() throws Exception {
        NetBaseClient client = new NetBaseClient();
        OneShotServer server = connectClient(client, TEST_PORT + 2);

        boolean result = client.send("hello");

        assertTrue(result, "send(String) should return true on success");

        client.socket.close();
        server.join(2000);
    }

    // ─────────────────────────────────────────────────────────────────
    // Lines 275-279: PingTask timeout path
    // pingCount >= PING_AUTO_DISCONNECT_COUNT → disconnect
    // ─────────────────────────────────────────────────────────────────

    @Test
    void line275_pingTaskTimeout() throws Exception {
        NetBaseClient client = new NetBaseClient();
        OneShotServer server = connectClient(client, TEST_PORT + 3);

        // Set pingCount to trigger timeout
        Field pingCountField = NetBaseClient.class.getDeclaredField("pingCount");
        pingCountField.setAccessible(true);
        pingCountField.set(client, NetBaseClient.PING_AUTO_DISCONNECT_COUNT);

        // Create and run PingTask
        NetBaseClient.PingTask task = client.new PingTask();
        task.run();

        // Should have set flags to false and cancelled timer
        assertFalse(client.connectedFlag,
                "Ping timeout should set connectedFlag to false");
        assertFalse(client.threadRunning,
                "Ping timeout should set threadRunning to false");

        client.socket.close();
        server.join(2000);
    }

    // ─────────────────────────────────────────────────────────────────
    // Lines 281-282: PingTask normal ping
    // pingCount < PING_AUTO_DISCONNECT_COUNT → send("ping\n"), pingCount++
    // ─────────────────────────────────────────────────────────────────

    @Test
    void line281_pingTaskNormalPing() throws Exception {
        NetBaseClient client = new NetBaseClient();
        OneShotServer server = connectClient(client, TEST_PORT + 4);

        // Set pingCount to a normal value
        Field pingCountField = NetBaseClient.class.getDeclaredField("pingCount");
        pingCountField.setAccessible(true);
        pingCountField.set(client, 1);

        NetBaseClient.PingTask task = client.new PingTask();
        task.run();

        // pingCount should have been incremented
        int newPingCount = (int) pingCountField.get(client);
        assertEquals(2, newPingCount, "pingCount should increment by 1");

        client.socket.close();
        server.join(2000);
    }

    // ─────────────────────────────────────────────────────────────────
    // Lines 284-285: PingTask debug logging at half threshold
    // pingCount >= PING_AUTO_DISCONNECT_COUNT / 2 → log debug
    // ─────────────────────────────────────────────────────────────────

    @Test
    void line284_pingTaskAtHalfThreshold() throws Exception {
        NetBaseClient client = new NetBaseClient();
        OneShotServer server = connectClient(client, TEST_PORT + 5);

        // Set pingCount to half the threshold
        Field pingCountField = NetBaseClient.class.getDeclaredField("pingCount");
        pingCountField.setAccessible(true);
        pingCountField.set(client, NetBaseClient.PING_AUTO_DISCONNECT_COUNT / 2);

        NetBaseClient.PingTask task = client.new PingTask();
        task.run();

        // pingCount should be incremented
        int newPingCount = (int) pingCountField.get(client);
        assertEquals(NetBaseClient.PING_AUTO_DISCONNECT_COUNT / 2 + 1,
                newPingCount, "pingCount should increment at half threshold");

        client.socket.close();
        server.join(2000);
    }

    // ─────────────────────────────────────────────────────────────────
    // Lines 292-294: PingTask exception handling
    // catch (Exception e) → log error, cancel timer
    // ─────────────────────────────────────────────────────────────────

    @Test
    void line292_pingTaskExceptionHandling() throws Exception {
        NetBaseClient client = new NetBaseClient();
        OneShotServer server = connectClient(client, TEST_PORT + 6);

        // Set connected flag so that PingTask tries to send
        client.connectedFlag = true;

        // Close the socket so send() will throw an IOException
        client.socket.close();

        NetBaseClient.PingTask task = client.new PingTask();
        task.run();

        // Exception should have been caught - no exception should propagate
        // Timer should have been cancelled
        assertNull(client.timerPing,
                "Timer should be null after exception in PingTask");

        server.join(2000);
    }
}
