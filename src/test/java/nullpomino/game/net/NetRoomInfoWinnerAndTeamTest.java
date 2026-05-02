package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.RuleOptions;

import org.junit.jupiter.api.Test;

/**
 * Tests for winner/team determination, seat lookup, game start, and delete
 * in NetRoomInfo:
 *   getWinner, getWinnerTeam, isTeamWin, getPlayerSeatNumber,
 *   gameStart, delete.
 */
class NetRoomInfoWinnerAndTeamTest {

	// ---- getWinner() -------------------------------------------------
	// Returns the last surviving connected, playing, seated player when
	// the game is a started multiplayer game with fewer than 2 survivors.

	@Test
	void getWinnerReturnsNullWhenStartPlayersLessThanTwo() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 1;
		room.playing = true;
		NetPlayerInfo p = activePlayer("A");
		room.playerSeat.add(p);
		room.playerSeatNowPlaying.add(p);
		assertNull(room.getWinner());
	}

	@Test
	void getWinnerReturnsNullWhenPlayingFlagIsFalse() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = false;
		NetPlayerInfo p = activePlayer("A");
		room.playerSeat.add(p);
		room.playerSeatNowPlaying.add(p);
		assertNull(room.getWinner());
	}

	@Test
	void getWinnerReturnsNullWhenNowPlayingIsEmpty() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		assertNull(room.getWinner());
	}

	@Test
	void getWinnerReturnsNullWhenAllPlayersDead() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("A");
		NetPlayerInfo p2 = activePlayer("B");
		p1.playing = false;
		p2.playing = false;
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertNull(room.getWinner());
	}

	@Test
	void getWinnerReturnsLoneSurvivor() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo alive = activePlayer("A");
		NetPlayerInfo dead = activePlayer("B");
		dead.playing = false;
		room.playerSeat.add(alive);
		room.playerSeat.add(dead);
		room.playerSeatNowPlaying.add(dead);
		room.playerSeatNowPlaying.add(alive);
		assertSame(alive, room.getWinner());
	}

	@Test
	void getWinnerReturnsNullWhenMultiplePlayersAlive() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 3;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("A");
		NetPlayerInfo p2 = activePlayer("B");
		NetPlayerInfo p3 = activePlayer("C");
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeat.add(p3);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		room.playerSeatNowPlaying.add(p3);
		assertNull(room.getWinner());
	}

	@Test
	void getWinnerIgnoresPlayerNotInSeat() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo alive = activePlayer("A");
		NetPlayerInfo dead = activePlayer("B");
		dead.playing = false;
		room.playerSeat.add(alive);
		// dead is in nowPlaying but NOT in playerSeat
		room.playerSeatNowPlaying.add(dead);
		room.playerSeatNowPlaying.add(alive);
		assertSame(alive, room.getWinner());
	}

	@Test
	void getWinnerIgnoresDisconnectedPlayer() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo alive = activePlayer("A");
		NetPlayerInfo disconnected = activePlayer("B");
		disconnected.connected = false;
		disconnected.playing = false;
		room.playerSeat.add(alive);
		room.playerSeat.add(disconnected);
		room.playerSeatNowPlaying.add(disconnected);
		room.playerSeatNowPlaying.add(alive);
		assertSame(alive, room.getWinner());
	}

	@Test
	void getWinnerSkipsNullEntriesInNowPlaying() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo alive = activePlayer("A");
		room.playerSeat.add(alive);
		// null entry between dead and alive
		room.playerSeatNowPlaying.add(null);
		room.playerSeatNowPlaying.add(alive);
		assertSame(alive, room.getWinner());
	}

	// ---- getWinnerTeam() ---------------------------------------------
	// Returns the team name of the first connected active seated player
	// when the game is a started multiplayer game with 2+ survivors.
	// Returns null otherwise.

	@Test
	void getWinnerTeamReturnsNullWhenStartPlayersLessThanTwo() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 1;
		assertNull(room.getWinnerTeam());
	}

	@Test
	void getWinnerTeamReturnsNullWhenPlayingFlagIsFalse() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = false;
		assertNull(room.getWinnerTeam());
	}

	@Test
	void getWinnerTeamReturnsNullWhenOnlyOnePlayerActive() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("TeamA");
		NetPlayerInfo p2 = activePlayer("TeamA");
		p2.playing = false; // only one active
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertNull(room.getWinnerTeam());
	}

	@Test
	void getWinnerTeamReturnsNullWhenActivePlayerHasEmptyTeam() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("");
		NetPlayerInfo p2 = activePlayer("");
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertNull(room.getWinnerTeam());
	}

	@Test
	void getWinnerTeamReturnsTeamNameOfFirstActivePlayer() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("Red");
		NetPlayerInfo p2 = activePlayer("Blue");
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertEquals("Red", room.getWinnerTeam());
	}

	@Test
	void getWinnerTeamSkipsInactivePlayers() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo inactive = activePlayer("Red");
		inactive.playing = false;
		NetPlayerInfo active = activePlayer("Blue");
		room.playerSeat.add(inactive);
		room.playerSeat.add(active);
		room.playerSeatNowPlaying.add(inactive);
		room.playerSeatNowPlaying.add(active);
		// only one active -> getHowManyPlayersPlaying returns 1 -> condition fails -> null
		assertNull(room.getWinnerTeam());
	}

	@Test
	void getWinnerTeamReturnsNullWhenNowPlayingIsEmpty() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		assertNull(room.getWinnerTeam());
	}

	// ---- isTeamWin() -------------------------------------------------
	// True iff the game is a started multiplayer game with 2+ survivors,
	// all of whom belong to the same non-empty team.

	@Test
	void isTeamWinReturnsFalseWhenStartPlayersLessThanTwo() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 1;
		assertFalse(room.isTeamWin());
	}

	@Test
	void isTeamWinReturnsFalseWhenPlayingFlagIsFalse() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = false;
		assertFalse(room.isTeamWin());
	}

	@Test
	void isTeamWinReturnsFalseWhenOnlyOnePlayerActive() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("Red");
		NetPlayerInfo p2 = activePlayer("Red");
		p2.playing = false;
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertFalse(room.isTeamWin());
	}

	@Test
	void isTeamWinReturnsFalseWhenActivePlayerHasEmptyTeam() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("");
		NetPlayerInfo p2 = activePlayer("");
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertFalse(room.isTeamWin());
	}

	@Test
	void isTeamWinReturnsFalseWhenActivePlayersOnDifferentTeams() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("Red");
		NetPlayerInfo p2 = activePlayer("Blue");
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertFalse(room.isTeamWin());
	}

	@Test
	void isTeamWinReturnsTrueWhenAllActivePlayersSameTeam() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("Red");
		NetPlayerInfo p2 = activePlayer("Red");
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertTrue(room.isTeamWin());
	}

	@Test
	void isTeamWinReturnsTrueForThreePlayersSameTeam() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 3;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("Red");
		NetPlayerInfo p2 = activePlayer("Red");
		NetPlayerInfo p3 = activePlayer("Red");
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeat.add(p3);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		room.playerSeatNowPlaying.add(p3);
		assertTrue(room.isTeamWin());
	}

	@Test
	void isTeamWinIgnoresInactivePlayersWithDifferentTeams() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 3;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("Red");
		NetPlayerInfo p2 = activePlayer("Red");
		NetPlayerInfo p3 = activePlayer("Blue");
		p3.playing = false; // inactive, should be ignored
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeat.add(p3);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		room.playerSeatNowPlaying.add(p3);
		assertTrue(room.isTeamWin());
	}

	@Test
	void isTeamWinSkipsNullEntriesInNowPlaying() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		NetPlayerInfo p1 = activePlayer("Red");
		NetPlayerInfo p2 = activePlayer("Red");
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.playerSeatNowPlaying.add(null);
		room.playerSeatNowPlaying.add(p1);
		room.playerSeatNowPlaying.add(p2);
		assertTrue(room.isTeamWin());
	}

	@Test
	void isTeamWinReturnsFalseWhenNowPlayingIsEmpty() {
		NetRoomInfo room = new NetRoomInfo();
		room.startPlayers = 2;
		room.playing = true;
		assertFalse(room.isTeamWin());
	}

	// ---- getPlayerSeatNumber() ---------------------------------------
	// Returns index in playerSeat list, or -1 if not found.

	@Test
	void getPlayerSeatNumberReturnsCorrectIndex() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		assertEquals(0, room.getPlayerSeatNumber(p1));
		assertEquals(1, room.getPlayerSeatNumber(p2));
	}

	@Test
	void getPlayerSeatNumberReturnsMinusOneForUnseatedPlayer() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeat.add(new NetPlayerInfo());
		NetPlayerInfo unseated = new NetPlayerInfo();
		assertEquals(-1, room.getPlayerSeatNumber(unseated));
	}

	@Test
	void getPlayerSeatNumberReturnsMinusOneForNull() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeat.add(new NetPlayerInfo());
		assertEquals(-1, room.getPlayerSeatNumber(null));
	}

	@Test
	void getPlayerSeatNumberReturnsMinusOneWhenSeatListEmpty() {
		NetRoomInfo room = new NetRoomInfo();
		assertEquals(-1, room.getPlayerSeatNumber(new NetPlayerInfo()));
	}

	// ---- gameStart() -------------------------------------------------
	// Initialises game-start state: copies seats, clears transient lists,
	// sets startPlayers, resets flags.

	@Test
	void gameStartCopiesPlayerSeatToNowPlaying() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		NetPlayerInfo p2 = new NetPlayerInfo();
		room.playerSeat.add(p1);
		room.playerSeat.add(p2);
		room.gameStart();
		assertEquals(2, room.playerSeatNowPlaying.size());
		assertSame(p1, room.playerSeatNowPlaying.get(0));
		assertSame(p2, room.playerSeatNowPlaying.get(1));
	}

	@Test
	void gameStartClearsPlayerSeatDead() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeatDead.add(new NetPlayerInfo());
		room.gameStart();
		assertTrue(room.playerSeatDead.isEmpty());
	}

	@Test
	void gameStartClearsChatList() {
		NetRoomInfo room = new NetRoomInfo();
		room.chatList.add(new NetChatMessage());
		room.gameStart();
		assertTrue(room.chatList.isEmpty());
	}

	@Test
	void gameStartSetsStartPlayersFromSeatedCount() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeat.add(new NetPlayerInfo());
		room.playerSeat.add(new NetPlayerInfo());
		room.gameStart();
		assertEquals(2, room.startPlayers);
	}

	@Test
	void gameStartResetsDeadCountToZero() {
		NetRoomInfo room = new NetRoomInfo();
		room.deadCount = 5;
		room.gameStart();
		assertEquals(0, room.deadCount);
	}

	@Test
	void gameStartDisablesAutoStartActive() {
		NetRoomInfo room = new NetRoomInfo();
		room.autoStartActive = true;
		room.gameStart();
		assertFalse(room.autoStartActive);
	}

	@Test
	void gameStartDisablesIsSomeoneCancelled() {
		NetRoomInfo room = new NetRoomInfo();
		room.isSomeoneCancelled = true;
		room.gameStart();
		assertFalse(room.isSomeoneCancelled);
	}

	@Test
	void gameStartUpdatesPlayerCounts() {
		NetRoomInfo room = new NetRoomInfo();
		NetPlayerInfo p1 = new NetPlayerInfo();
		room.playerList.add(p1);
		room.playerList.add(new NetPlayerInfo());
		room.playerSeat.add(p1);
		room.gameStart();
		assertEquals(1, room.playerSeatedCount);
		assertEquals(2, room.playerListCount);
	}

	// ---- delete() ----------------------------------------------------
	// Clears all room data: ruleOpt to null, all lists empty.

	@Test
	void deleteSetsRuleOptToNull() {
		NetRoomInfo room = new NetRoomInfo();
		room.ruleOpt = new RuleOptions();
		room.delete();
		assertNull(room.ruleOpt);
	}

	@Test
	void deleteClearsMapList() {
		NetRoomInfo room = new NetRoomInfo();
		room.mapList.add("map1");
		room.delete();
		assertTrue(room.mapList.isEmpty());
	}

	@Test
	void deleteClearsPlayerList() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerList.add(new NetPlayerInfo());
		room.delete();
		assertTrue(room.playerList.isEmpty());
	}

	@Test
	void deleteClearsPlayerSeat() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeat.add(new NetPlayerInfo());
		room.delete();
		assertTrue(room.playerSeat.isEmpty());
	}

	@Test
	void deleteClearsPlayerSeatNowPlaying() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeatNowPlaying.add(new NetPlayerInfo());
		room.delete();
		assertTrue(room.playerSeatNowPlaying.isEmpty());
	}

	@Test
	void deleteClearsPlayerQueue() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerQueue.add(new NetPlayerInfo());
		room.delete();
		assertTrue(room.playerQueue.isEmpty());
	}

	@Test
	void deleteClearsPlayerSeatDead() {
		NetRoomInfo room = new NetRoomInfo();
		room.playerSeatDead.add(new NetPlayerInfo());
		room.delete();
		assertTrue(room.playerSeatDead.isEmpty());
	}

	@Test
	void deleteClearsChatList() {
		NetRoomInfo room = new NetRoomInfo();
		room.chatList.add(new NetChatMessage());
		room.delete();
		assertTrue(room.chatList.isEmpty());
	}

	@Test
	void deleteOnEmptyRoomDoesNotThrow() {
		NetRoomInfo room = new NetRoomInfo();
		room.delete(); // should not throw
		assertNull(room.ruleOpt);
		assertTrue(room.mapList.isEmpty());
		assertTrue(room.playerList.isEmpty());
		assertTrue(room.playerSeat.isEmpty());
		assertTrue(room.playerSeatNowPlaying.isEmpty());
		assertTrue(room.playerQueue.isEmpty());
		assertTrue(room.playerSeatDead.isEmpty());
		assertTrue(room.chatList.isEmpty());
	}

	// ---- helpers -----------------------------------------------------

	private static NetPlayerInfo activePlayer(String team) {
		NetPlayerInfo p = new NetPlayerInfo();
		p.strTeam = team;
		p.playing = true;
		p.connected = true;
		return p;
	}
}
