package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class NetObserverClientTest {

	@Test
	void defaultConstructorLeavesHostUnsetAndUsesDefaultPort() {
		NetObserverClient client = new NetObserverClient();

		assertNull(client.getHost());
		assertEquals(NetBaseClient.DEFAULT_PORT, client.getPort());
		assertNull(client.getIP());
		assertEquals(0, client.getPlayerCount());
		assertEquals(0, client.getObserverCount());
		assertEquals(0f, client.serverVersion);
	}

	@Test
	void hostConstructorSetsHostAndDefaultsPort() {
		NetObserverClient client = new NetObserverClient("example.invalid");

		assertEquals("example.invalid", client.getHost());
		assertEquals(NetBaseClient.DEFAULT_PORT, client.getPort());
	}

	@Test
	void hostAndPortConstructorSetsBoth() {
		NetObserverClient client = new NetObserverClient("example.invalid", 1234);

		assertEquals("example.invalid", client.getHost());
		assertEquals(1234, client.getPort());
	}

	@Test
	void welcomePacketParsesVersionAndCountsAndDoesNotStartPingTimerForDefaultInterval()
			throws IOException {
		NetObserverClient client = new NetObserverClient();

		// Four required fields plus the optional minor/string pieces; no ping
		// override → default branch leaves the ping timer null.
		client.processPacket(
				"welcome\t1.0\t10\t5\t0\t1.0a\t" + NetBaseClient.PING_INTERVAL);

		assertEquals(1.0f, client.serverVersion);
		assertEquals(10, client.getPlayerCount());
		assertEquals(5, client.getObserverCount());
		assertNull(client.timerPing);
	}

	@Test
	void welcomePacketWithoutPingFieldUsesDefaultAndSkipsTimer() throws IOException {
		NetObserverClient client = new NetObserverClient();

		// message.length == 4, so the optional fields fall through to defaults.
		client.processPacket("welcome\t2.0\t3\t1");

		assertEquals(2.0f, client.serverVersion);
		assertEquals(3, client.getPlayerCount());
		assertEquals(1, client.getObserverCount());
		assertNull(client.timerPing);
	}

	@Test
	void observerUpdatePacketRefreshesPlayerAndObserverCounts() throws IOException {
		NetObserverClient client = new NetObserverClient();

		client.processPacket("observerupdate\t42\t7");

		assertEquals(42, client.getPlayerCount());
		assertEquals(7, client.getObserverCount());
	}

	@Test
	void unrelatedPacketLeavesObserverStateUntouched() throws IOException {
		NetObserverClient client = new NetObserverClient();
		client.processPacket("observerupdate\t11\t9");

		// Anything else is delegated up to the base class — a "pong" is the
		// canonical no-op message that does not mutate observer state.
		client.processPacket("pong");

		assertEquals(11, client.getPlayerCount());
		assertEquals(9, client.getObserverCount());
	}

	@Test
	void welcomePacketWithCustomPingIntervalStartsPingTimer() throws IOException {
		NetObserverClient client = new NetObserverClient();

		try {
			client.processPacket(
					"welcome\t1.0\t1\t1\t0\t1.0a\t" + (NetBaseClient.PING_INTERVAL * 2));

			// Custom interval differs from PING_INTERVAL → the conditional
			// branch fires startPingTask, which creates a Timer.
			if (client.timerPing == null) {
				throw new AssertionError("timerPing should have been started");
			}
		} finally {
			if (client.timerPing != null) client.timerPing.cancel();
		}
	}
}
