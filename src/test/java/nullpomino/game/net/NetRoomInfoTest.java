package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class NetRoomInfoTest {

	@Test
	void exportImportPreservesRoomWireFields() {
		NetRoomInfo original = new NetRoomInfo();
		original.roomID = 12;
		original.strName = "Room; Name";
		original.maxPlayers = 4;
		original.playerSeatedCount = 2;
		original.spectatorCount = 1;
		original.playerListCount = 3;
		original.playing = true;
		original.ruleLock = true;
		original.ruleName = "Rule+Name";
		original.autoStartSeconds = 30;
		original.gravity = 2;
		original.denominator = 3;
		original.are = 4;
		original.areLine = 5;
		original.lineDelay = 6;
		original.lockDelay = 7;
		original.das = 8;
		original.tspinEnableType = 2;
		original.b2b = false;
		original.combo = false;
		original.rensaBlock = false;
		original.counter = false;
		original.bravo = false;
		original.reduceLineSend = true;
		original.hurryupSeconds = 9;
		original.hurryupInterval = 10;
		original.autoStartTNET2 = true;
		original.disableTimerAfterSomeoneCancelled = true;
		original.useMap = true;
		original.useFractionalGarbage = true;
		original.garbageChangePerAttack = false;
		original.garbagePercent = 55;
		original.spinCheckType = NetRoomInfo.SPINTYPE_IMMOBILE;
		original.tspinEnableEZ = true;
		original.b2bChunk = true;
		original.strMode = "Mode; Name";
		original.singleplayer = true;
		original.rated = true;
		original.customRated = true;
		original.style = 2;
		original.divideChangeRateByPlayers = true;
		original.isTarget = true;
		original.targetTimer = 90;

		NetRoomInfo imported = new NetRoomInfo(original.exportString());

		assertEquals(original.roomID, imported.roomID);
		assertEquals(original.strName, imported.strName);
		assertEquals(original.maxPlayers, imported.maxPlayers);
		assertEquals(original.playerSeatedCount, imported.playerSeatedCount);
		assertEquals(original.spectatorCount, imported.spectatorCount);
		assertEquals(original.playerListCount, imported.playerListCount);
		assertEquals(original.playing, imported.playing);
		assertEquals(original.ruleLock, imported.ruleLock);
		assertEquals(original.ruleName, imported.ruleName);
		assertEquals(original.autoStartSeconds, imported.autoStartSeconds);
		assertEquals(original.gravity, imported.gravity);
		assertEquals(original.denominator, imported.denominator);
		assertEquals(original.are, imported.are);
		assertEquals(original.areLine, imported.areLine);
		assertEquals(original.lineDelay, imported.lineDelay);
		assertEquals(original.lockDelay, imported.lockDelay);
		assertEquals(original.das, imported.das);
		assertEquals(original.tspinEnableType, imported.tspinEnableType);
		assertEquals(original.b2b, imported.b2b);
		assertEquals(original.combo, imported.combo);
		assertEquals(original.rensaBlock, imported.rensaBlock);
		assertEquals(original.counter, imported.counter);
		assertEquals(original.bravo, imported.bravo);
		assertEquals(original.reduceLineSend, imported.reduceLineSend);
		assertEquals(original.hurryupSeconds, imported.hurryupSeconds);
		assertEquals(original.hurryupInterval, imported.hurryupInterval);
		assertEquals(original.autoStartTNET2, imported.autoStartTNET2);
		assertEquals(original.disableTimerAfterSomeoneCancelled, imported.disableTimerAfterSomeoneCancelled);
		assertEquals(original.useMap, imported.useMap);
		assertEquals(original.useFractionalGarbage, imported.useFractionalGarbage);
		assertEquals(original.garbageChangePerAttack, imported.garbageChangePerAttack);
		assertEquals(original.garbagePercent, imported.garbagePercent);
		assertEquals(original.spinCheckType, imported.spinCheckType);
		assertEquals(original.tspinEnableEZ, imported.tspinEnableEZ);
		assertEquals(original.b2bChunk, imported.b2bChunk);
		assertEquals(original.strMode, imported.strMode);
		assertEquals(original.singleplayer, imported.singleplayer);
		assertEquals(original.rated, imported.rated);
		assertEquals(original.customRated, imported.customRated);
		assertEquals(original.style, imported.style);
		assertEquals(original.divideChangeRateByPlayers, imported.divideChangeRateByPlayers);
		assertEquals(original.isTarget, imported.isTarget);
		assertEquals(original.targetTimer, imported.targetTimer);
	}

	@Test
	void exportStringArrayKeepsLegacyFieldCount() {
		assertEquals(43, new NetRoomInfo().exportStringArray().length);
	}

	@Test
	void importStringArrayAcceptsLegacyDataWithoutTargetFields() {
		NetRoomInfo original = new NetRoomInfo();
		original.isTarget = true;
		original.targetTimer = 90;
		String[] legacyFields = Arrays.copyOf(original.exportStringArray(), 41);

		NetRoomInfo imported = new NetRoomInfo();
		imported.isTarget = false;
		imported.targetTimer = 60;
		imported.importStringArray(legacyFields);

		assertFalse(imported.isTarget);
		assertEquals(60, imported.targetTimer);
	}

	@Test
	void joinSeatFillsVacanciesRemovesQueueAndHonorsCapacity() {
		NetRoomInfo room = new NetRoomInfo();
		room.maxPlayers = 2;
		NetPlayerInfo first = new NetPlayerInfo();
		NetPlayerInfo second = new NetPlayerInfo();
		NetPlayerInfo third = new NetPlayerInfo();
		room.joinQueue(first);

		assertEquals(0, room.joinSeat(first));
		assertFalse(room.playerQueue.contains(first));
		assertEquals(1, room.joinSeat(second));
		assertEquals(-1, room.joinSeat(third));

		room.exitSeat(first);

		assertEquals(0, room.joinSeat(third));
		assertSame(third, room.playerSeat.get(0));
	}

	@Test
	void seatAndQueueIndexHelpersReturnExistingPositions() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo first = player("A", "192.0.2.1");
		NetPlayerInfo second = player("B", "192.0.2.2");

		assertEquals(0, room.joinSeat(first));
		assertEquals(1, room.joinSeat(second));
		assertEquals(0, room.getPlayerSeatNumber(first));
		assertEquals(1, room.getPlayerSeatNumber(second));
		assertEquals(-1, room.getPlayerSeatNumber(player("missing", "192.0.2.3")));

		assertEquals(0, room.joinQueue(first));
		assertEquals(1, room.joinQueue(second));
		assertEquals(0, room.joinQueue(first));
	}

	@Test
	void playerCountHelpersIgnoreNullSeatsAndInactivePlayers() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo ready = player("A", "192.0.2.1");
		NetPlayerInfo inactive = player("B", "192.0.2.2");
		ready.ready = true;
		ready.playing = true;
		room.playerSeat.add(null);
		room.playerSeat.add(ready);
		room.playerSeat.add(inactive);
		room.gameStart();

		assertEquals(2, room.getNumberOfPlayerSeated());
		assertEquals(1, room.getHowManyPlayersReady());
		assertEquals(1, room.getHowManyPlayersPlaying());
	}

	@Test
	void gameStartCopiesSeatsAndWinnerUsesActiveConnectedSeatedPlayers() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo winner = player("A", "192.0.2.1");
		NetPlayerInfo loser = player("B", "192.0.2.2");
		room.joinSeat(winner);
		room.joinSeat(loser);
		room.gameStart();
		room.playing = true;
		winner.playing = true;
		winner.connected = true;
		loser.playing = false;
		loser.connected = true;

		assertEquals(2, room.startPlayers);
		assertEquals(1, room.getHowManyPlayersPlaying());
		assertSame(winner, room.getWinner());
		assertNull(room.getWinnerTeam());
	}

	@Test
	void teamAndIpHelpersDetectDuplicatesAmongStartedPlayers() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo first = player("A", "192.0.2.1");
		NetPlayerInfo second = player("A", "192.0.2.1");
		room.joinSeat(first);
		room.joinSeat(second);
		room.gameStart();
		room.playing = true;
		first.playing = true;
		first.connected = true;
		second.playing = true;
		second.connected = true;

		assertTrue(room.isTeamGame());
		assertTrue(room.isTeamWin());
		assertEquals("A", room.getWinnerTeam());
		assertTrue(room.hasSameIPPlayers());
	}

	@Test
	void teamAndIpDuplicateHelpersIgnoreBlankValues() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo first = player("", "");
		NetPlayerInfo second = player("", "");
		room.joinSeat(first);
		room.joinSeat(second);
		room.gameStart();
		room.playing = true;
		first.playing = true;
		first.connected = true;
		second.playing = true;
		second.connected = true;

		assertFalse(room.isTeamGame());
		assertFalse(room.isTeamWin());
		assertNull(room.getWinnerTeam());
		assertFalse(room.hasSameIPPlayers());
	}

	@Test
	void copyConstructorCopiesListContainers() {
		NetRoomInfo original = new NetRoomInfo();
		NetPlayerInfo player = player("A", "192.0.2.1");
		NetChatMessage chat = new NetChatMessage("hello");
		original.mapList.add("map");
		original.playerList.add(player);
		original.playerSeat.add(player);
		original.playerQueue.add(player);
		original.playerSeatDead.add(player);
		original.chatList.add(chat);

		NetRoomInfo copy = new NetRoomInfo(original);
		original.mapList.clear();
		original.playerList.clear();
		original.playerSeat.clear();
		original.playerQueue.clear();
		original.playerSeatDead.clear();
		original.chatList.clear();

		assertEquals(1, copy.mapList.size());
		assertEquals(1, copy.playerList.size());
		assertEquals(1, copy.playerSeat.size());
		assertEquals(1, copy.playerQueue.size());
		assertEquals(1, copy.playerSeatDead.size());
		assertEquals(1, copy.chatList.size());
	}

	private static NetPlayerInfo player(String team, String ip) {
		NetPlayerInfo player = new NetPlayerInfo();
		player.strTeam = team;
		player.strRealIP = ip;
		return player;
	}
}
