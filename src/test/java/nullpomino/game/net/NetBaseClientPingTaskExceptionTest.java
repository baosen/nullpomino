package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.net.ServerSocket;
import java.net.Socket;

import org.junit.jupiter.api.Test;

/**
 * Tests for NetBaseClient lines 292-294: PingTask exception handling.
 *
 * The PingTask catch block (lines 292-294) catches exceptions from send().
 * To trigger this path, we create a subclass that overrides send(String)
 * to let the exception propagate, and use a properly connected socket
 * so that isConnected() returns true.
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
        // Create a local server socket so we can connect to it
        ServerSocket server = new ServerSocket(0);
        int localPort = server.getLocalPort();

        SendThrowingClient client = new SendThrowingClient();

        // Connect the socket to the local server
        client.socket = new Socket("127.0.0.1", localPort);
        client.connectedFlag = true;
        client.ip = "127.0.0.1";

        // Accept connection on server side (so socket.isConnected() is true)
        server.accept();

        // Start a PingTask timer so timerPing is non-null
        client.startPingTask(10000);

        // Run PingTask: isConnected() returns true (connected socket + connectedFlag),
        // so it tries send("ping\n"), which throws RuntimeException via our override.
        // The catch block at lines 292-294 catches it, logs the error,
        // and calls timerPing.cancel().
        NetBaseClient.PingTask task = client.new PingTask();
        task.run();

        // After the catch, timerPing was cancelled but still referenced
        assertNotNull(client.timerPing,
                "timerPing should still be non-null after cancel");

        // Verify the socket is still present and connected
        assertNotNull(client.socket);
        assertTrue(client.connectedFlag,
                "connectedFlag should remain true (exception doesn't reset it)");

        server.close();
    }
}
