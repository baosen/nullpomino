package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.net.room.RoomEndpoint;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gap in the {@link NetRoomPlayerClient}
 * constructor (L30): the null-team fallback.
 */
class NetRoomPlayerClientBranchGapTest {

	/** Inert endpoint; the constructor only reads host and port from it. */
	private static final class StubEndpoint implements RoomEndpoint {
		@Override
		public void setLineListener(LineListener listener) {
		}

		@Override
		public void setClosedListener(ClosedListener listener) {
		}

		@Override
		public void sendLine(String line) {
		}

		@Override
		public void clientReady() {
		}

		@Override
		public boolean isOpen() {
			return false;
		}

		@Override
		public int getListenPort() {
			return 12345;
		}

		@Override
		public String getDisplayHost() {
			return "gap-host";
		}

		@Override
		public String getSessionId() {
			return "gap-session";
		}

		@Override
		public void shutdown() {
		}
	}

	@Test
	void nullTeamBecomesEmptyString() {
		NetRoomPlayerClient client = new NetRoomPlayerClient(new StubEndpoint(), "Gap", null);
		assertEquals("", client.playerTeam);
	}

	@Test
	void nonNullTeamIsTrimmed() {
		NetRoomPlayerClient client = new NetRoomPlayerClient(new StubEndpoint(), "Gap", "  crew  ");
		assertEquals("crew", client.playerTeam);
		assertEquals("gap-host", client.getHost());
		assertEquals(12345, client.getPort());
	}
}
