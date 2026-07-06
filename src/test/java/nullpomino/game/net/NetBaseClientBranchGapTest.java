package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gaps in {@link NetBaseClient}:
 * <ul>
 * <li>L121 read loop re-check with threadRunning already cleared</li>
 * <li>L206 isConnected with an unconnected socket and with connectedFlag off</li>
 * <li>L280/L291/L295 PingTask cancel guards with timerPing both null and set</li>
 * </ul>
 *
 * <p>Uses loopback sockets only (same technique as
 * {@link NetBaseClientRunIntegrationTest}); every socket and timer is
 * released before the test returns.
 */
class NetBaseClientBranchGapTest {

	/** send() override that throws so PingTask's catch block runs. */
	private static final class ThrowingSendClient extends NetBaseClient {
		@Override
		public boolean send(String msg) {
			throw new RuntimeException("simulated send failure");
		}
	}

	private static Socket[] loopbackPair(ServerSocket server) throws Exception {
		Socket clientSide = new Socket("127.0.0.1", server.getLocalPort());
		Socket serverSide = server.accept();
		return new Socket[] { clientSide, serverSide };
	}

	@Test
	void isConnectedCoversUnconnectedSocketAndConnectedFlagStates() throws Exception {
		NetBaseClient client = new NetBaseClient();

		client.socket = new Socket(); // created but never connected
		client.connectedFlag = true;
		assertFalse(client.isConnected(), "unconnected socket must report not connected");
		client.socket.close();

		try (ServerSocket server = new ServerSocket(0)) {
			Socket[] pair = loopbackPair(server);
			client.socket = pair[0];

			client.connectedFlag = true;
			assertTrue(client.isConnected());

			client.connectedFlag = false;
			assertFalse(client.isConnected(), "connectedFlag off must report not connected");

			pair[0].close();
			pair[1].close();
		}
	}

	@Test
	void pingTaskTimeoutPathRunsWithAndWithoutTimer() throws Exception {
		try (ServerSocket server = new ServerSocket(0)) {
			Socket[] pair = loopbackPair(server);
			NetBaseClient client = new NetBaseClient();
			client.socket = pair[0];

			// Timeout with no timer to cancel.
			client.connectedFlag = true;
			client.pingCount = NetBaseClient.PING_AUTO_DISCONNECT_COUNT;
			client.timerPing = null;
			client.new PingTask().run();
			assertFalse(client.connectedFlag);
			assertFalse(client.threadRunning);

			// Timeout again with a timer present.
			client.connectedFlag = true;
			client.pingCount = NetBaseClient.PING_AUTO_DISCONNECT_COUNT;
			client.timerPing = new Timer(true);
			client.new PingTask().run();
			assertFalse(client.connectedFlag);
			client.timerPing.cancel();

			pair[0].close();
			pair[1].close();
		}
	}

	@Test
	void pingTaskNotConnectedPathRunsWithAndWithoutTimer() {
		NetBaseClient client = new NetBaseClient(); // socket == null -> not connected

		client.timerPing = new Timer(true);
		client.new PingTask().run(); // cancels the timer
		client.timerPing.cancel();

		client.timerPing = null;
		client.new PingTask().run(); // nothing to cancel
		assertFalse(client.isConnected());
	}

	@Test
	void pingTaskExceptionPathRunsWithAndWithoutTimer() throws Exception {
		try (ServerSocket server = new ServerSocket(0)) {
			Socket[] pair = loopbackPair(server);
			ThrowingSendClient client = new ThrowingSendClient();
			client.socket = pair[0];
			client.connectedFlag = true;
			client.pingCount = 0;

			// Exception with no timer to cancel.
			client.timerPing = null;
			client.new PingTask().run();

			// Exception with a timer present.
			client.timerPing = new Timer(true);
			client.new PingTask().run();
			client.timerPing.cancel();

			assertTrue(client.connectedFlag, "the exception path must not clear connectedFlag");

			pair[0].close();
			pair[1].close();
		}
	}

	@Test
	void readLoopExitsWhenThreadRunningIsCleared() throws Exception {
		try (ServerSocket server = new ServerSocket(0)) {
			NetBaseClient client = new NetBaseClient("127.0.0.1", server.getLocalPort());
			CountDownLatch firstMessage = new CountDownLatch(1);
			client.addListener(new NetMessageListener() {
				@Override
				public void netOnMessage(NetBaseClient c, String[] message) {
					firstMessage.countDown();
				}

				@Override
				public void netOnDisconnect(NetBaseClient c, Throwable ex) {
				}
			});

			client.start();
			try (Socket serverSide = server.accept()) {
				OutputStream out = serverSide.getOutputStream();
				out.write("hello\n".getBytes(StandardCharsets.UTF_8));
				out.flush();
				assertTrue(firstMessage.await(10, TimeUnit.SECONDS), "first message must arrive");

				// Clear the flag while the client blocks in read(); the next
				// message (or the immediate re-check) makes the while condition
				// evaluate threadRunning == false and leave the loop.
				client.threadRunning = false;
				out.write("bye\n".getBytes(StandardCharsets.UTF_8));
				out.flush();

				client.join(10_000);
				assertFalse(client.isAlive(), "read loop must exit once threadRunning is cleared");
				assertFalse(client.connectedFlag);
			}
		}
	}
}
