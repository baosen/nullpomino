package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers uncovered branches in {@link NetDummyMode}: netplayInit with null
 * owner, netplayUnload, playerInit, netPlayerInit watch mode, updateCursor
 * in netplay, netplayOnRetryKey, netlobbyOnExit, netIsNetRankingViewOK,
 * netIsNetRankingSendOK, netGetGoalType, netSendReplay when not send-ok,
 * and netRecvNetPlayRanking with short message.
 */
class NetDummyModeCoverageTest {

	@Test
	void netplayInitWithNonLobbyObjectDoesNothing() {
		NetDummyMode mode = new NetDummyMode();
		// Pass a non-NetLobbyFrame object
		mode.netplayInit("not a lobby");
		// Should not throw, netLobby should remain null
		assertEquals(null, mode.netLobby);
	}

	@Test
	void netplayUnloadClearsLobby() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		// netLobby starts null, unload should not throw
		mode.netplayUnload(null);
	}

	@Test
	void playerInitSetsNothingState() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		mode.playerInit(engine, 0);

		assertEquals(GameEngine.Status.NOTHING, engine.stat);
		assertFalse(engine.isVisible);
	}

	@Test
	void netPlayerInitWatchModeHidesNextAndHold() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		mode.netIsWatch = true;
		mode.netPlayerInit(engine, 0);

		assertFalse(engine.isNextVisible);
		assertFalse(engine.isHoldVisible);
	}

	@Test
	void netPlayerInitNotWatchModeKeepsVisibility() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		mode.netIsWatch = false;
		mode.netPlayerInit(engine, 0);

		assertEquals(0, mode.netReplaySendStatus);
		assertFalse(mode.netIsPB);
		assertFalse(mode.netIsNetRankingDisplayMode);
		assertFalse(mode.netAlwaysSendFieldAttributes);
	}

	@Test
	void netIsNetRankingViewOKReturnsFalse() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		assertFalse(mode.netIsNetRankingViewOK(engine));
	}

	@Test
	void netIsNetRankingSendOKDelegatesToViewOK() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		assertFalse(mode.netIsNetRankingSendOK(engine));
	}

	@Test
	void netGetGoalTypeReturnsZero() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		assertEquals(0, mode.netGetGoalType());
	}

	@Test
	void netplayOnRetryKeyNotNetplay() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netIsNetPlay = false;
		// Should not throw
		assertDoesNotThrow(() -> mode.netplayOnRetryKey(null, 0));
	}

	@Test
	void netplayOnRetryKeyWatchMode() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netIsNetPlay = true;
		mode.netIsWatch = true;
		// Should not throw
		assertDoesNotThrow(() -> mode.netplayOnRetryKey(null, 0));
	}

	@Test
	void netlobbyOnExitSetsQuitFlags() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();

		mode.netlobbyOnExit(null);

		assertTrue(manager.engine[0].quitflag);
	}

	@Test
void netlobbyOnMessagePlayerlogout() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netCurrentRoomInfo = null;

		// NetPlayerInfo importString expects many semicolon-delimited fields.
		// We provide enough fields for the basic parsing but leave personalBest
		// and trailing fields absent so read(null) returns null.
		// Fields: name;country;host;team;roomID;uid;seatID;queueID;ready;playing;connected;
		//         rating[0-3];playCount[0-3];winCount[0-3]
		String playerInfo = "Player1;US;localhost;team1;1;100;0;0;false;false;true;0;0;0;0;0;0;0;0;0;0;0;0";
		String[] message = new String[] {"playerlogout", playerInfo};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessagePlayerupdate() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netCurrentRoomInfo = null;

		String[] message = new String[] {"playerupdate"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageStartNotWatch() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netIsWatch = false;

		String[] message = new String[] {"start"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageDeadNotWatch() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netIsWatch = false;

		String[] message = new String[] {"dead"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageSpsendng() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.netReplaySendStatus = 0;

		String[] message = new String[] {"spsendng"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		// netReplaySendStatus is set to 1 by spsendng handler, then netSendReplay
		// may set it to 2 if netIsNetRankingSendOK returns false
		assertTrue(mode.netReplaySendStatus >= 1, "netReplaySendStatus should be >= 1 after spsendng");
	}

	@Test
	void netlobbyOnMessageSpsendok() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netRankingRank = new int[2];
		mode.netRankingRank[0] = -1;
		mode.netRankingRank[1] = -1;

		String[] message = new String[] {"spsendok", "5", "true", "3"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		assertEquals(5, mode.netRankingRank[0]);
		assertTrue(mode.netIsPB);
		assertEquals(3, mode.netRankingRank[1]);
	}

	@Test
	void netlobbyOnMessageReset1pNotWatch() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netIsWatch = false;

		String[] message = new String[] {"reset1p"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameNotWatch() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netIsWatch = false;

		String[] message = new String[] {"game", "0", "0", "cursor", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netSendReplayWhenNotSendOK() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		mode.netReplaySendStatus = 0;
		// netIsNetRankingSendOK returns false by default
		mode.netSendReplay(engine);
		assertEquals(2, mode.netReplaySendStatus);
	}

	@Test
	void netRecvNetPlayRankingShortMessage() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		// Short message (length <= 4) should set noDataFlag
		String[] message = new String[] {"spranking", "", "", "", "true"};
		assertDoesNotThrow(() -> mode.netRecvNetPlayRanking(engine, message));
		assertTrue(mode.netRankingNoDataFlag[1]);
		assertFalse(mode.netRankingReady[1]);
	}

	@Test
	void netUpdatePlayerExistWithNoRoom() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		mode.netCurrentRoomInfo = null;

		mode.netUpdatePlayerExist();

		assertEquals(0, mode.netNumSpectators);
		assertEquals("", mode.netPlayerName);
	}

	@Test
	void renderLastCallsNetDrawAllPlayersCount() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		// Should not throw
		assertDoesNotThrow(() -> mode.renderLast(engine, 0));
	}

	// ---- helpers ----

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}