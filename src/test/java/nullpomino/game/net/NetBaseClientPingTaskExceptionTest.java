package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Tests for NetBaseClient lines 292-294: PingTask exception handling.
 *
 * The PingTask catch block (lines 292-294) is unreachable via normal code
 * because the send() method catches all Exception types internally.
 * To trigger this path, we create a subclass that overrides send(String)
 * to let the exception propagate.
 */
class NetBaseClientPingTaskExceptionTest {

    /**
     * Subclass of NetBaseClient that overrides send(String) to throw
     * a RuntimeException, so the exception propagates up to PingTask's
     * catch block (lines 292-294).
     */
    static class SendThrowingClient extends NetBaseClient {
        @Override
        public boolean send(String msg) {
            throw new RuntimeException("Simulated send failure in PingTask");
        }
    }

    @Test
    void pingTaskExceptionHandlingCatchBlock() throws Exception {
        SendThrowingClient client = new SendThrowingClient();

        // Set up socket so isConnected() returns true
        client.socket = new java.net.Socket();
        client.connectedFlag = true;
        client.ip = "127.0.0.1";

        // Start a PingTask timer so timerPing is non-null
        client.startPingTask(10000);

        // Run PingTask: isConnected() is true, so it tries send("ping\n"),
        // which throws RuntimeException via our override.
        // The catch block at lines 292-294 catches it, logs the error,
        // and calls timerPing.cancel().
        NetBaseClient.PingTask task = client.new PingTask();
        task.run();

        // After the catch, timerPing was cancelled but still referenced
        assertNotNull(client.timerPing,
                "timerPing should still be non-null after cancel (only cancelled, not nulled)");

        // Verify the socket is still present and connected
        assertNotNull(client.socket);
        assertTrue(client.connectedFlag,
                "connectedFlag should remain true (exception doesn't reset it)");
    }
}
