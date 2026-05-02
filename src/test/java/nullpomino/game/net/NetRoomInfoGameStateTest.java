package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for game-state-related methods of NetRoomInfo:
 * canJoinSeat, getNumberOfPlayerSeated, getHowManyPlayersReady,
 * getHowManyPlayersPlaying, isTeamGame, hasSameIPPlayers,
 * joinSeat/exitSeat, joinQueue/exitQueue, updatePlayerCount.
 */
class NetRoomInfoGameStateTest {

	// ---- canJoinSeat() -------------------------------------------------

	@Test
	void canJoinSeatReturnsTrueWhenRoomIsEmpty() {
		NetRoomInfo room = new NetRoomInfo();
		room.maxPlayers = 4;
		assertTrue(room.canJoinSeat());
	}

	@Test
	void canJoinSeatReturnsFalseWhenRoomIsFull() {
		NetRoomInfo room = new NetRoomInfo();
		room.maxPlayers = 2;
		room.playerSeat.add(new NetPlayerInfo());
		room.playerSeat.add(new NetPlayerInfo());
		assertFalse(room.canJoinSeat());
	}

	@Test
	void canJoinSeatReturnsTrueAtBoundaryWhenOneSeatRemains() {
		NetRoomInfo room = new NetRoomInfo();
		room.maxPlayers = 3;
		room.playerSeat.add(new NetPlayerInfo());
		room.playerSeat.add(new NetPlayerInfo());
		assertTrue(room.canJoinSeat());
	}

	@Test
	void canJoinSeatReturnsFalseAtBoundaryWhenExactlyFull() {
		NetRoomInfo room = new NetRoomInfo();
		room.maxPlayers = 2;
		room.playerSeat.add(new NetPlayerInfo());
		room.playerSeat.add(new NetPlayerInfo());
		room.playerSeat.add(new NetPlayerInfo()); // extra seat list entry beyond maxPlayers
		assertFalse(room.canJoinSeat());
	}

	// ---- getNumberOfPlayerSeated() --------------------------------------

	@Test
	void getNumberOfPlayerSeatedReturnsZeroWhenAllSeatsNull() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeat.add(null);
		room.playerSeat.add(null);
		assertEquals(0, room.getNumberOfPlayerSeated());
	}

	@Test
	void getNumberOfPlayerSeatedReturnsZeroWhenEmptyList() {
		NetRoomInfo room = new NetRoomInfo();
		assertEquals(0, room.getNumberOfPlayerSeated());
	}

	@Test
	void getNumberOfPlayerSeatedCountsOnlyNonNullEntries() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeat.add(new NetPlayerInfo());
		room.playerSeat.add(null);
		room.playerSeat.add(new NetPlayerInfo());
		assertEquals(2, room.getNumberOfPlayerSeated());
	}

	@Test
	void getNumberOfPlayerSeatedReturnsAllWhenAllOccupied() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeat.add(new NetPlayerInfo());
		room.playerSeat.add(new NetPlayerInfo());
		room.playerSeat.add(new NetPlayerInfo());
		assertEquals(3, room.getNumberOfPlayerSeated());
	}

	// ---- getHowManyPlayersReady() ---------------------------------------

	@Test
	void getHowManyPlayersReadyReturnsZeroWhenNoneReady() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.ready = false;
		p2.ready = false;
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		assertEquals(0, room.getHowManyPlayersReady());
	}

	@Test
	void getHowManyPlayersReadyCountsOnlyReadyPlayers() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		NetPlayerInfo p3 = new NetPlayerInfo();
		p1.ready = true;
		p2.ready = false;
		p3.ready = true;
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeat.add(p3);
		assertEquals(2, room.getHowManyPlayersReady());
	}

	@Test
	void getHowManyPlayersReadyReturnsAllWhenAllReady() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.ready = true;
		p2.ready = true;
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		assertEquals(2, room.getHowManyPlayersReady());
	}

	@Test
	void getHowManyPlayersReadyIgnoresNullSeats() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		p1.ready = true;
		room.playerSeat.add(null);
		room.playerSeat.add(p1);
		assertEquals(1, room.getHowManyPlayersReady());
	}

	// ---- getHowManyPlayersPlaying() -------------------------------------

	@Test
	void getHowManyPlayersPlayingReturnsZeroWhenNoActivePlayers() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		p1.playing = false;
		room.playerSeat.add(p1);
		room.playerSeatNowPlaying.add(p1);
		assertEquals(0, room.getHowManyPlayersPlaying());
	}

	@Test
	void getHowManyPlayersPlayingCountsPlayingSeatedPlayers() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.playing = true;
		p2.playing = true;
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertEquals(2, room.getHowManyPlayersPlaying());
	}

	@Test
	void getHowManyPlayersPlayingExcludesPlayersNotInSeat() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.playing = true;
		p2.playing = true;
		room.playerSeat.add(p1);
		// p2 intentionally omitted from playerSeat
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertEquals(1, room.getHowManyPlayersPlaying());
	}

	@Test
	void getHowManyPlayersPlayingExcludesNulls() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		p1.playing = true;
		room.playerSeat.add(p1);
		room.playerSeatNowPlaying.add(null);
		room.playerSeatNowPlaying.add(p1);
		assertEquals(1, room.getHowManyPlayersPlaying());
	}

	// ---- isTeamGame() ---------------------------------------------------

	@Test
	void isTeamGameReturnsFalseWhenStartPlayersLessThanTwo() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 1;
		assertFalse(room.isTeamGame());
	}

	@Test
	void isTeamGameReturnsFalseWhenNoDuplicateTeams() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.strTeam = "Red";
		p2.strTeam = "Blue";
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertFalse(room.isTeamGame());
	}

	@Test
	void isTeamGameReturnsTrueWhenDuplicateTeamsExist() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.strTeam = "Red";
		p2.strTeam = "Red";
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertTrue(room.isTeamGame());
	}

	@Test
	void isTeamGameIgnoresBlankTeamNames() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.strTeam = "";
		p2.strTeam = "";
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertFalse(room.isTeamGame());
	}

	@Test
	void isTeamGameIgnoresNullPlayersInNowPlaying() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.strTeam = "Red";
		p2.strTeam = "Blue";
		room.playerSeatNowPlaying.add(null);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertFalse(room.isTeamGame());
	}

	// ---- hasSameIPPlayers() ---------------------------------------------

	@Test
	void hasSameIPPlayersReturnsFalseWhenStartPlayersLessThanTwo() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 1;
		assertFalse(room.hasSameIPPlayers());
	}

	@Test
	void hasSameIPPlayersReturnsFalseWhenUniqueIPs() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.strRealIP = "192.168.1.1";
		p2.strRealIP = "192.168.1.2";
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertFalse(room.hasSameIPPlayers());
	}

	@Test
	void hasSameIPPlayersReturnsTrueWhenDuplicateIPs() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.strRealIP = "192.168.1.1";
		p2.strRealIP = "192.168.1.1";
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertTrue(room.hasSameIPPlayers());
	}

	@Test
	void hasSameIPPlayersIgnoresBlankIPs() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		p1.strRealIP = "";
		p2.strRealIP = "";
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertFalse(room.hasSameIPPlayers());
	}

	// ---- joinSeat() / exitSeat() ----------------------------------------

	@Test
	void joinSeatReturnsMinusOneWhenRoomIsFull() {
		NetRoomInfo room = new NetRoomInfo();
		room.maxPlayers = 1;
		room.playerSeat.add(new NetPlayerInfo());
		assertEquals(-1, room.joinSeat(new NetPlayerInfo()));
	}

	@Test
	void joinSeatFillsFirstNullSlot() {
		NetRoomInfo room = new NetRoomInfo();
		room.maxPlayers = 3;
		NetPlayerInfo first = new NetPlayerInfo();
		NetPlayerInfo second = new NetPlayerInfo();
		room.playerSeat.add(first);
		room.playerSeat.add(null);
		assertEquals(1, room.joinSeat(second));
		assertSame(second, room.playerSeat.get(1));
		assertSame(first, room.playerSeat.get(0));
	}

	@Test
	void joinSeatAppendsWhenNoNullSlots() {
		NetRoomInfo room = new NetRoomInfo();
		room.maxPlayers = 5;
		NetPlayerInfo p = new NetPlayerInfo();
		assertEquals(0, room.joinSeat(p));
		assertSame(p, room.playerSeat.get(0));
		assertEquals(1, room.playerSeat.size());
	}

	@Test
	void exitSeatSetsMatchingSlotToNull() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p = new NetPlayerInfo();
		room.joinSeat(p);
		room.exitSeat(p);
		assertTrue(room.playerSeat.contains(null));
	}

	@Test
	void exitSeatOnlyNullifiesMatchingPlayer() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		room.joinSeat(p1);
		room.joinSeat(p2);
		room.exitSeat(p1);
		assertSame(p2, room.playerSeat.get(1));
	}

	@Test
	void exitSeatDoesNothingWhenPlayerNotSeated() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p = new NetPlayerInfo();
		room.playerSeat.add(new NetPlayerInfo());
		room.exitSeat(p); // should not throw or alter any entries
		assertEquals(1, room.playerSeat.size());
	}

	@Test
	void joinSeatRemovesPlayerFromQueue() {
		NetRoomInfo room = new NetRoomInfo();
		room.maxPlayers = 4;
		NetPlayerInfo p = new NetPlayerInfo();
		room.joinQueue(p);
		assertTrue(room.playerQueue.contains(p));
		room.joinSeat(p);
		assertFalse(room.playerQueue.contains(p));
	}

	// ---- joinQueue() / exitQueue() --------------------------------------

	@Test
	void joinQueueAddsPlayerToQueue() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p = new NetPlayerInfo();
		assertEquals(0, room.joinQueue(p));
		assertTrue(room.playerQueue.contains(p));
		assertEquals(1, room.playerQueue.size());
	}

	@Test
	void joinQueueReturnsExistingIndexIfAlreadyInQueue() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p = new NetPlayerInfo();
		int first = room.joinQueue(p);
		int second = room.joinQueue(p);
		assertEquals(first, second);
		assertEquals(1, room.playerQueue.size());
	}

	@Test
	void joinQueueReturnsNextIndexForNewPlayers() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		assertEquals(0, room.joinQueue(p1));
		assertEquals(1, room.joinQueue(p2));
	}

	@Test
	void exitQueueRemovesPlayerFromQueue() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p = new NetPlayerInfo();
		room.joinQueue(p);
		room.exitQueue(p);
		assertFalse(room.playerQueue.contains(p));
		assertTrue(room.playerQueue.isEmpty());
	}

	@Test
	void exitQueueDoesNothingWhenPlayerNotInQueue() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p = new NetPlayerInfo();
		room.exitQueue(p); // should not throw
		assertTrue(room.playerQueue.isEmpty());
	}

	// ---- updatePlayerCount() --------------------------------------------

	@Test
	void updatePlayerCountRecomputesAllCounts() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();

		room.playerList.add(p1);
		room.playerList.add(p2);
		room.playerList.add(new NetPlayerInfo());

		room.playerSeat.add(p1);
		room.playerSeat.add(null);
		room.playerSeat.add(p2);

		room.updatePlayerCount();

		assertEquals(2, room.playerSeatedCount);
		assertEquals(3, room.playerListCount);
		assertEquals(1, room.spectatorCount);
	}

	@Test
	void updatePlayerCountWorksWithEmptyRoom() {
		NetRoomInfo room = new NetRoomInfo();
		room.updatePlayerCount();
		assertEquals(0, room.playerSeatedCount);
		assertEquals(0, room.playerListCount);
		assertEquals(0, room.spectatorCount);
	}

	@Test
	void updatePlayerCountSpectatorZeroWhenAllSeated() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		room.playerList.add(p1);
		room.playerList.add(p2);
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.updatePlayerCount();
		assertEquals(2, room.playerSeatedCount);
		assertEquals(2, room.playerListCount);
		assertEquals(0, room.spectatorCount);
	}

	@Test
	void updatePlayerCountAllSpectatorsWhenNoSeatedPlayers() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		room.playerList.add(p1);
		// playerSeat is empty, so no one is seated
		room.updatePlayerCount();
		assertEquals(0, room.playerSeatedCount);
		assertEquals(1, room.playerListCount);
		assertEquals(1, room.spectatorCount);
	}

	@Test
	void updatePlayerCountWorksWithMixedNullSeats() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		room.playerList.add(p1);
		room.playerList.add(p2);
		room.playerList.add(new NetPlayerInfo());
		room.playerSeat.add(p1);
		room.playerSeat.add(null);
		room.playerSeat.add(p2);
		room.playerSeat.add(null);
		room.updatePlayerCount();
		assertEquals(2, room.playerSeatedCount);
		assertEquals(3, room.playerListCount);
		assertEquals(1, room.spectatorCount);
	}
}
