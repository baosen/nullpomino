package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gaps in {@link NetPlayerClient}:
 * <ul>
 * <li>welcome message without the optional trailing fields</li>
 * <li>L216 changestatus "joinseat" seat assignment</li>
 * <li>L251 getPlayerInfoByUID skipping null list entries</li>
 * </ul>
 *
 * <p>The clients are never connected; the login line sent while handling
 * "welcome" goes to the base class's no-op {@code send()}.
 */
class NetPlayerClientBranchGapTest {

	private static NetPlayerClient freshClient() {
		return new NetPlayerClient("127.0.0.1", 9999, "GapTester", "GapTeam");
	}

	@Test
	void shortWelcomeWithoutOptionalFieldsStillUpdatesCounts() throws Exception {
		NetPlayerClient client = freshClient();

		client.processPacket("welcome\t1\t2\t3");

		assertEquals(2, client.getPlayerCount());
		assertEquals(3, client.getObserverCount());
	}

	@Test
	void changeStatusJoinSeatAssignsSeatAndClearsQueue() throws Exception {
		NetPlayerClient client = freshClient();
		NetPlayerInfo info = new NetPlayerInfo();
		info.uid = 7;
		info.seatID = -1;
		info.queueID = 4;
		client.playerInfoList.add(info);

		client.processPacket("changestatus\tjoinseat\t7\tGapTester\t2");

		assertEquals(2, info.seatID);
		assertEquals(-1, info.queueID);
	}

	@Test
	void changeStatusWithUnknownStatusLeavesSeatAndQueueAlone() throws Exception {
		NetPlayerClient client = freshClient();
		NetPlayerInfo info = new NetPlayerInfo();
		info.uid = 7;
		info.seatID = 5;
		info.queueID = 6;
		client.playerInfoList.add(info);

		client.processPacket("changestatus\tspectate\t7\tGapTester\t2");

		assertEquals(5, info.seatID);
		assertEquals(6, info.queueID);
	}

	@Test
	void addListenerIgnoresDuplicates() {
		NetPlayerClient client = freshClient();
		NetMessageListener listener = new NetMessageListener() {
			@Override public void netOnMessage(NetBaseClient c, String[] m) {}
			@Override public void netOnDisconnect(NetBaseClient c, Throwable ex) {}
		};

		client.addListener(listener);
		client.addListener(listener);

		assertTrue(client.removeListener(listener), "first remove drops the single registration");
		assertFalse(client.removeListener(listener), "second remove finds nothing - duplicate was never added");
	}

	@Test
	void getPlayerInfoByUIDSkipsNullEntries() {
		NetPlayerClient client = freshClient();
		NetPlayerInfo info = new NetPlayerInfo();
		info.uid = 7;
		client.playerInfoList.add(null);
		client.playerInfoList.add(info);

		assertSame(info, client.getPlayerInfoByUID(7));
		assertNull(client.getPlayerInfoByUID(8));
	}
}
