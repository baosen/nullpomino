package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class NetBaseClientTest {

	@Test
	void defaultConstructorLeavesHostUnsetAndUsesDefaultPort() {
		NetBaseClient client = new NetBaseClient();

		assertNull(client.getHost());
		assertEquals(NetBaseClient.DEFAULT_PORT, client.getPort());
		assertNull(client.getIP());
		assertFalse(client.isConnected());
	}

	@Test
	void hostConstructorSetsHostAndDefaultsPort() {
		NetBaseClient client = new NetBaseClient("example.invalid");

		assertEquals("example.invalid", client.getHost());
		assertEquals(NetBaseClient.DEFAULT_PORT, client.getPort());
	}

	@Test
	void hostAndPortConstructorSetsBoth() {
		NetBaseClient client = new NetBaseClient("example.invalid", 4242);

		assertEquals("example.invalid", client.getHost());
		assertEquals(4242, client.getPort());
	}

	@Test
	void addListenerIsIdempotentAndRemoveListenerReportsRemoval() {
		NetBaseClient client = new NetBaseClient();
		RecordingListener listener = new RecordingListener();

		client.addListener(listener);
		client.addListener(listener); // duplicate is silently ignored

		assertEquals(1, client.listeners.size());
		assertTrue(client.removeListener(listener));
		assertFalse(client.removeListener(listener));
		assertEquals(0, client.listeners.size());
	}

	@Test
	void sendWithNullSocketReturnsFalseAndDoesNotThrow() {
		NetBaseClient client = new NetBaseClient();

		assertFalse(client.send(new byte[] {1, 2, 3}));
		assertFalse(client.send("hello"));
	}

	@Test
	void processPacketDispatchesToEveryListenerAndStopsOnNoise() throws IOException {
		NetBaseClient client = new NetBaseClient();
		RecordingListener first = new RecordingListener();
		RecordingListener second = new RecordingListener();
		client.addListener(first);
		client.addListener(second);

		client.processPacket("hello\tworld");

		assertEquals(1, first.calls.size());
		assertEquals(1, second.calls.size());
		assertEquals("hello", first.calls.get(0)[0]);
		assertEquals("world", first.calls.get(0)[1]);
	}

	@Test
	void processPacketSwallowsListenerExceptionsToProtectOtherListeners() throws IOException {
		NetBaseClient client = new NetBaseClient();
		ThrowingListener bad = new ThrowingListener();
		RecordingListener good = new RecordingListener();
		client.addListener(bad);
		client.addListener(good);

		client.processPacket("hello");

		assertEquals(1, good.calls.size());
	}

	@Test
	void processPacketResetsPingCountOnPong() throws IOException {
		NetBaseClient client = new NetBaseClient();
		client.pingCount = NetBaseClient.PING_AUTO_DISCONNECT_COUNT / 2 + 1;

		client.processPacket("pong");

		assertEquals(0, client.pingCount);
	}

	@Test
	void processPacketResetsPingCountFromBelowDebugThreshold() throws IOException {
		NetBaseClient client = new NetBaseClient();
		client.pingCount = 1;

		client.processPacket("pong");

		assertEquals(0, client.pingCount);
	}

	@Test
	void startPingTaskWithNonPositiveIntervalDoesNotCreateTimer() {
		NetBaseClient client = new NetBaseClient();

		client.startPingTask(0);
		assertNull(client.timerPing);

		client.startPingTask(-1);
		assertNull(client.timerPing);
	}

	@Test
	void startPingTaskWithPositiveIntervalCreatesTimerAndCancelsPriorTimer() {
		NetBaseClient client = new NetBaseClient();
		try {
			client.startPingTask(NetBaseClient.PING_INTERVAL);
			assertNotNull(client.timerPing);
			java.util.Timer first = client.timerPing;

			// A second invocation cancels the prior timer and installs a new one.
			client.startPingTask(NetBaseClient.PING_INTERVAL);
			assertNotNull(client.timerPing);
			if(first == client.timerPing) {
				throw new AssertionError("expected a fresh Timer instance after re-arming");
			}
		} finally {
			if(client.timerPing != null) client.timerPing.cancel();
		}
	}

	@Test
	void startPingTaskNoArgsUsesDefaultInterval() {
		NetBaseClient client = new NetBaseClient();
		try {
			client.startPingTask();
			assertNotNull(client.timerPing);
		} finally {
			if(client.timerPing != null) client.timerPing.cancel();
		}
	}

	@Test
	void isConnectedReturnsFalseWhenSocketIsNullEvenWithConnectedFlagSet() {
		NetBaseClient client = new NetBaseClient();
		client.connectedFlag = true;

		assertFalse(client.isConnected());
	}

	private static final class RecordingListener implements NetMessageListener {
		final List<String[]> calls = new ArrayList<>();

		@Override
		public void netOnMessage(NetBaseClient client, String[] message) {
			calls.add(message);
		}

		@Override
		public void netOnDisconnect(NetBaseClient client, Throwable ex) {
		}
	}

	private static final class ThrowingListener implements NetMessageListener {
		@Override
		public void netOnMessage(NetBaseClient client, String[] message) {
			throw new RuntimeException("boom");
		}

		@Override
		public void netOnDisconnect(NetBaseClient client, Throwable ex) {
		}
	}
}
