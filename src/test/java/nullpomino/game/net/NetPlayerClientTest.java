package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class NetPlayerClientTest {

	@Test
	void defaultConstructorLeavesHostUnsetAndUsesDefaultPort() {
		NetPlayerClient client = new NetPlayerClient();

		assertNull(client.getHost());
		assertEquals(NetBaseClient.DEFAULT_PORT, client.getPort());
		assertNull(client.getPlayerName());
		assertEquals(0, client.getPlayerUID());
		assertEquals(-1, client.getPlayerCount());
		assertEquals(-1, client.getObserverCount());
	}

	@Test
	void hostConstructorSetsHostAndDefaultPort() {
		NetPlayerClient client = new NetPlayerClient("example.invalid");

		assertEquals("example.invalid", client.getHost());
		assertEquals(NetBaseClient.DEFAULT_PORT, client.getPort());
	}

	@Test
	void hostAndPortConstructorSetsBoth() {
		NetPlayerClient client = new NetPlayerClient("example.invalid", 5000);

		assertEquals("example.invalid", client.getHost());
		assertEquals(5000, client.getPort());
	}

	@Test
	void hostPortNameConstructorSetsAllFieldsAndDefaultsTeamToEmpty() {
		NetPlayerClient client = new NetPlayerClient("example.invalid", 5000, "Nullpo");

		assertEquals("example.invalid", client.getHost());
		assertEquals(5000, client.getPort());
		assertEquals("Nullpo", client.getPlayerName());
		assertEquals("", client.playerTeam);
	}

	@Test
	void hostPortNameTeamConstructorSetsAllFourFields() {
		NetPlayerClient client = new NetPlayerClient("example.invalid", 5000, "Nullpo", "Reds");

		assertEquals("example.invalid", client.getHost());
		assertEquals(5000, client.getPort());
		assertEquals("Nullpo", client.getPlayerName());
		assertEquals("Reds", client.playerTeam);
	}

	@Test
	void welcomePacketUpdatesPlayerAndObserverCountsAndDoesNotStartPingTimerForDefault()
			throws IOException {
		NetPlayerClient client = new NetPlayerClient("example.invalid", 5000, "Nullpo");

		client.processPacket(
				"welcome\t1.0\t11\t4\t0\t1.0a\t" + NetBaseClient.PING_INTERVAL);

		assertEquals(11, client.getPlayerCount());
		assertEquals(4, client.getObserverCount());
		assertNull(client.timerPing);
	}

	@Test
	void welcomePacketWithCustomPingIntervalStartsPingTask() throws IOException {
		NetPlayerClient client = new NetPlayerClient("example.invalid", 5000, "Nullpo");

		try {
			client.processPacket(
					"welcome\t1.0\t11\t4\t0\t1.0a\t" + (NetBaseClient.PING_INTERVAL * 2));

			assertEquals(11, client.getPlayerCount());
			assertEquals(4, client.getObserverCount());
			assertNotNull(client.timerPing);
		} finally {
			if (client.timerPing != null) {
				client.timerPing.cancel();
			}
		}
	}

	@Test
	void observerUpdatePacketRefreshesCounts() throws IOException {
		NetPlayerClient client = new NetPlayerClient();

		client.processPacket("observerupdate\t30\t12");

		assertEquals(30, client.getPlayerCount());
		assertEquals(12, client.getObserverCount());
	}

	@Test
	void loginSuccessPacketStoresUrlDecodedNameAndUid() throws IOException {
		NetPlayerClient client = new NetPlayerClient();

		client.processPacket("loginsuccess\t" + NetUtil.urlEncode("Null Po") + "\t42");

		assertEquals("Null Po", client.getPlayerName());
		assertEquals(42, client.getPlayerUID());
	}

	@Test
	void playerListPacketAppendsEachPlayerToTheList() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		String wire1 = playerWire(1, "Alice");
		String wire2 = playerWire(2, "Bob");

		client.processPacket("playerlist\t2\t" + wire1 + "\t" + wire2);

		assertEquals(2, client.getPlayerInfoList().size());
		assertEquals(1, client.getPlayerInfoList().get(0).uid);
		assertEquals(2, client.getPlayerInfoList().get(1).uid);
	}

	@Test
	void playerNewAddsAndPlayerUpdateReplacesExisting() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		String wire = playerWire(7, "Charlie");

		client.processPacket("playernew\t" + wire);
		assertEquals(1, client.getPlayerInfoList().size());

		// Same UID, new name → replaces in place rather than duplicating.
		String wire2 = playerWire(7, "Charlie2");
		client.processPacket("playerupdate\t" + wire2);

		assertEquals(1, client.getPlayerInfoList().size());
		assertEquals(7, client.getPlayerInfoList().get(0).uid);
	}

	@Test
	void playerLogoutRemovesMatchingPlayer() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		String wire = playerWire(9, "Dave");
		client.processPacket("playernew\t" + wire);
		assertEquals(1, client.getPlayerInfoList().size());

		client.processPacket("playerlogout\t" + wire);

		assertEquals(0, client.getPlayerInfoList().size());
	}

	@Test
	void playerLogoutForUnknownUidLeavesListUnchanged() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("playernew\t" + playerWire(1, "Alice"));

		client.processPacket("playerlogout\t" + playerWire(99, "Ghost"));

		assertEquals(1, client.getPlayerInfoList().size());
		assertEquals(1, client.getPlayerInfoList().get(0).uid);
	}

	@Test
	void roomListAppendsRoomsAndRoomUpdateReplacesExisting() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		String room1 = roomWire(1, "Lobby A");
		String room2 = roomWire(2, "Lobby B");

		client.processPacket("roomlist\t2\t" + room1 + "\t" + room2);
		assertEquals(2, client.getRoomInfoList().size());

		String updated = roomWire(1, "Lobby A renamed");
		client.processPacket("roomupdate\t" + updated);
		assertEquals(2, client.getRoomInfoList().size());
		assertEquals(1, client.getRoomInfo(1).roomID);
	}

	@Test
	void roomCreateAddsNewRoomAndRoomDeleteRemovesIt() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		String wire = roomWire(5, "Test Room");

		client.processPacket("roomcreate\t" + wire);
		assertEquals(1, client.getRoomInfoList().size());

		client.processPacket("roomdelete\t" + wire);
		assertEquals(0, client.getRoomInfoList().size());
	}

	@Test
	void roomDeleteForUnknownRoomLeavesListUnchanged() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("roomcreate\t" + roomWire(1, "Test Room"));

		client.processPacket("roomdelete\t" + roomWire(99, "Ghost Room"));

		assertEquals(1, client.getRoomInfoList().size());
	}

	@Test
	void changeStatusWatchOnlyClearsSeatAndQueueIds() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("playernew\t" + playerWire(7, "Eve"));
		NetPlayerInfo p = client.getPlayerInfoByUID(7);
		p.seatID = 4;
		p.queueID = 8;

		client.processPacket("changestatus\twatchonly\t7");

		assertEquals(-1, p.seatID);
		assertEquals(-1, p.queueID);
	}

	@Test
	void changeStatusJoinQueueSetsQueueAndClearsSeat() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("playernew\t" + playerWire(7, "Eve"));

		client.processPacket("changestatus\tjoinqueue\t7\t\t3");

		NetPlayerInfo p = client.getPlayerInfoByUID(7);
		assertEquals(-1, p.seatID);
		assertEquals(3, p.queueID);
	}

	@Test
	void changeStatusJoinSeatSetsSeatAndClearsQueue() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("playernew\t" + playerWire(7, "Eve"));

		client.processPacket("changestatus\tjoinseat\t7\t\t2");

		NetPlayerInfo p = client.getPlayerInfoByUID(7);
		assertEquals(2, p.seatID);
		assertEquals(-1, p.queueID);
	}

	@Test
	void changeStatusForUnknownUidIsNoOp() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("playernew\t" + playerWire(7, "Eve"));

		client.processPacket("changestatus\twatchonly\t999");

		// Existing player is unaffected.
		assertEquals(7, client.getPlayerInfoByUID(7).uid);
	}

	@Test
	void getRoomInfoReturnsNullForNegativeIdAndForMissingId() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("roomcreate\t" + roomWire(1, "Test Room"));

		assertNull(client.getRoomInfo(-1));
		assertNull(client.getRoomInfo(99));
		assertNotNull(client.getRoomInfo(1));
	}

	@Test
	void getYourPlayerInfoReturnsTheEntryWithMatchingUid() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("playernew\t" + playerWire(7, "Eve"));
		client.processPacket("loginsuccess\t" + NetUtil.urlEncode("Eve") + "\t7");

		NetPlayerInfo me = client.getYourPlayerInfo();
		assertNotNull(me);
		assertEquals(7, me.uid);
		assertSame(me, client.getPlayerInfoByUID(7));
	}

	@Test
	void getCurrentRoomIdReturnsMinusOneWhenSelfNotFound() {
		NetPlayerClient client = new NetPlayerClient();

		// playerUID defaults to 0 and the list is empty → null self → catch
		// branch returns -1.
		assertEquals(-1, client.getCurrentRoomID());
		assertNull(client.getCurrentRoomInfo());
	}

	@Test
	void getCurrentRoomIdReturnsRoomIdOfOwnPlayer() throws IOException {
		NetPlayerClient client = new NetPlayerClient();
		client.processPacket("playernew\t" + playerWire(7, "Eve"));
		client.processPacket("roomcreate\t" + roomWire(3, "Room 3"));
		client.getPlayerInfoByUID(7).roomID = 3;
		client.playerUID = 7;

		assertEquals(3, client.getCurrentRoomID());
		assertSame(client.getRoomInfo(3), client.getCurrentRoomInfo());
	}

	private static String playerWire(int uid, String name) {
		NetPlayerInfo p = new NetPlayerInfo();
		p.uid = uid;
		p.strName = name;
		return p.exportString();
	}

	private static String roomWire(int roomID, String name) {
		NetRoomInfo r = new NetRoomInfo();
		r.roomID = roomID;
		r.strName = name;
		return r.exportString();
	}
}
