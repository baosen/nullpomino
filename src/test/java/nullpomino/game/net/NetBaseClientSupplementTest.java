package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Supplementary tests for {@link NetBaseClient} covering additional edge cases
 * not covered in {@link NetBaseClientTest}.
 */
class NetBaseClientSupplementTest {

	@Test
	void hostConstructorSetsThreadName() {
		NetBaseClient client = new NetBaseClient("testhost");
		assertEquals("NET_testhost", client.getName());
	}

	@Test
	void hostAndPortConstructorSetsThreadNameWithPort() {
		NetBaseClient client = new NetBaseClient("testhost", 9876);
		assertEquals("NET_testhost:9876", client.getName());
	}

	@Test
	void isConnectedReturnsFalseWhenSocketIsNullAndConnectedFlagIsFalse() {
		NetBaseClient client = new NetBaseClient();
		client.socket = null;
		client.connectedFlag = false;
		assertFalse(client.isConnected());
	}

	@Test
	void processPacketPongResetsPingCountFromZero() throws IOException {
		NetBaseClient client = new NetBaseClient();
		client.pingCount = 0;

		client.processPacket("pong");

		assertEquals(0, client.pingCount);
	}

	@Test
	void processPacketNonPongStillDispatchesToListeners() throws IOException {
		NetBaseClient client = new NetBaseClient();
		CapturingListener listener = new CapturingListener();
		client.addListener(listener);

		client.processPacket("somecommand\targ1\targ2");

		assertEquals(1, listener.calls.size());
		assertEquals("somecommand", listener.calls.get(0)[0]);
		assertEquals("arg1", listener.calls.get(0)[1]);
		assertEquals("arg2", listener.calls.get(0)[2]);
	}

	@Test
	void processPacketEmptyMessageDispatchesToListeners() throws IOException {
		NetBaseClient client = new NetBaseClient();
		CapturingListener listener = new CapturingListener();
		client.addListener(listener);

		client.processPacket("");

		assertEquals(1, listener.calls.size());
		assertEquals("", listener.calls.get(0)[0]);
	}

	@Test
	void processPacketWithTabOnlyDispatchesToListener() throws IOException {
		NetBaseClient client = new NetBaseClient();
		CapturingListener listener = new CapturingListener();
		client.addListener(listener);

		client.processPacket("a\tb");

		assertEquals(1, listener.calls.size());
		String[] msg = listener.calls.get(0);
		assertEquals(2, msg.length);
		assertEquals("a", msg[0]);
		assertEquals("b", msg[1]);
	}

	/**
	 * A simple NetMessageListener that records all message invocations.
	 */
	static final class CapturingListener implements NetMessageListener {
		final List<String[]> calls = new ArrayList<>();

		@Override
		public void netOnMessage(NetBaseClient client, String[] message) {
			calls.add(message);
		}

		@Override
		public void netOnDisconnect(NetBaseClient client, Throwable ex) {
		}
	}
}
