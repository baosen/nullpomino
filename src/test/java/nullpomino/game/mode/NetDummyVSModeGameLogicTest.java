package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link NetDummyVSMode}: modeInit,
 * playerInit, netPlayerInit, onReady, onMove, startGame, onLast
 * (play timer, auto-start timer), pieceLocked, onGameOver,
 * onExcellent, onResult, and netvsResetFlags.
 */
class NetDummyVSModeGameLogicTest {

	@Test
	void getNameReturnsExpected() {
		assertEquals("NET-VS-DUMMY", new NetDummyVSMode().getName());
	}

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(new NetDummyVSMode().isVSMode());
	}

	@Test
	void getPlayersReturns6() {
		assertEquals(6, new NetDummyVSMode().getPlayers());
	}

	@Test
	void isNetplayModeReturnsTrue() {
		assertTrue(new NetDummyVSMode().isNetplayMode());
	}

	@Test
	void modeInitInitializesArrays() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		assertTrue(readBoolean(mode, "netForceSendMovements"));
		assertEquals(-1, readInt(mode, "netvsMySeatID"));
		assertEquals(0, readInt(mode, "netvsNumPlayers"));

		boolean[] playerExist = (boolean[]) readFieldByClass(mode, NetDummyVSMode.class, "netvsPlayerExist");
		int[] playerSeatID = (int[]) readFieldByClass(mode, NetDummyVSMode.class, "netvsPlayerSeatID");
		assertEquals(6, playerExist.length);
		assertEquals(6, playerSeatID.length);
	}

	@Test
	void netvsResetFlagsSetsAllToFalse() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		mode.netvsResetFlags();

		assertFalse(readBoolean(mode, "netvsIsGameActive"));
		assertFalse(readBoolean(mode, "netvsIsGameFinished"));
		assertFalse(readBoolean(mode, "netvsIsReadyChangePending"));
		assertFalse(readBoolean(mode, "netvsIsDeadPending"));
		assertFalse(readBoolean(mode, "netvsIsNewcomer"));
		assertFalse(readBoolean(mode, "netvsPlayTimerActive"));
		assertFalse(readBoolean(mode, "netvsIsPractice"));
		assertEquals(0, readInt(mode, "netvsPlayTimer"));
		assertEquals(0, readInt(mode, "netvsPieceMoveTimer"));
	}

	@Test
	void playerInitResetsMenuFields() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "menuTime"));
		assertEquals(0, readInt(mode, "menuCursor"));
	}

	@Test
	void netPlayerInitSetsEngineDefaults() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));

		mode.netPlayerInit(engine, 0);

		assertEquals(10, engine.fieldWidth);
		assertEquals(20, engine.fieldHeight);
		assertFalse(engine.gameoverAll);
	}

	@Test
	void onReadyDoesNotThrow() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.init();
		mode.modeInit(mgr);
		NetRoomInfo room = new NetRoomInfo();
		room.useMap = false;
		java.lang.reflect.Field f = NetDummyVSMode.class.getSuperclass().getDeclaredField("netCurrentRoomInfo");
		f.setAccessible(true);
		f.set(mode, room);

		boolean result = mode.onReady(engine, 0);

		assertFalse(result);
	}

	@Test
	void onMoveReturnsTrueForNonZeroPlayer() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));

		boolean result = mode.onMove(engine, 1);

		assertTrue(result);
	}

	@Test
	void onMoveReturnsFalseForPlayer0WhenNotWatch() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		setBoolean(mode, "netIsWatch", false);
		setBoolean(mode, "netIsNetPlay", false);

		boolean result = mode.onMove(engine, 0);

		assertFalse(result);
	}

	private static void setupOwner(GameManager manager) {
		if (manager.bgmStatus == null) {
			manager.bgmStatus = new BGMStatus();
		}
	}

	@Test
	void startGameSetsBgmAndResetsTimer() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.init();
		mode.modeInit(mgr);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertEquals(0, readInt(mode, "netvsPieceMoveTimer"));
	}

	@Test
	void startGameSetsPracticeBgmWhenInPractice() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.init();
		mode.modeInit(mgr);
		setBoolean(mode, "netvsIsPractice", true);
		engine.playerID = 0;

		mode.startGame(engine, 0);

		assertEquals(BGMStatus.BGM_NOTHING,
				mode.owner.bgmStatus.bgm);
	}

	@Test
	void onLastIncrementsPlayTimer() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.init();
		mode.modeInit(mgr);
		setBoolean(mode, "netvsPlayTimerActive", true);
		engine.playerID = 0;

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "netvsPlayTimer"));
	}

	@Test
	void onLastDoesNotIncrementForNonZeroPlayer() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.init();
		mode.modeInit(mgr);
		setBoolean(mode, "netvsPlayTimerActive", true);
		engine.playerID = 1;

		// onLast checks playerID parameter (not engine.playerID), so pass 1
		mode.onLast(engine, 1);

		// Non-zero playerID -> onLast should NOT increment playTimer
		assertEquals(0, readInt(mode, "netvsPlayTimer"));
	}

	@Test
	void pieceLockedResetsMoveTimer() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.init();
		mode.modeInit(mgr);
		setInt(mode, "netvsPieceMoveTimer", 100);

		mode.pieceLocked(engine, 0, 0);

		assertEquals(0, readInt(mode, "netvsPieceMoveTimer"));
	}

	@Test
	void onGameOverReturnsTrueForPractice() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.init();
		mode.modeInit(mgr);
		setupOwner(mgr);
		engine.createFieldIfNeeded();
		setBoolean(mode, "netvsIsPractice", true);
		setBoolean(mode, "netvsIsPracticeExitAllowed", true);
		engine.statc[0] = 999; // past field height + 1

		boolean result = mode.onGameOver(engine, 0);

		assertTrue(result);
		assertEquals(GameEngine.Status.RESULT, engine.stat);
	}

	@Test
	void onExcellentCallsGameEnded() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.init();
		mode.modeInit(mgr);
		setupOwner(mgr);
		engine.createFieldIfNeeded();
		engine.playerID = 0;

		boolean result = mode.onExcellent(engine, 0);

		assertTrue(result);
		assertFalse(engine.allowTextRenderByReceiver);
	}

	@Test
	void onResultSetsAllowTextRenderFalse() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		engine.allowTextRenderByReceiver = true;

		boolean result = mode.onResult(engine, 0);

		assertTrue(result);
		assertFalse(engine.allowTextRenderByReceiver);
	}

	@Test
	void netvsGetNumberOfTeamsAliveReturnsZeroWhenNoPlayers() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));

		int result = mode.netvsGetNumberOfTeamsAlive();

		assertEquals(0, result);
	}

	@Test
	void netvsIsAttackableReturnsFalseForSelf() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		mode.modeInit(new GameManager(new EventReceiver()));

		assertFalse(mode.netvsIsAttackable(0));
	}

	@Test
	void netvsGetPlayerIDbySeatIDMapsCorrectly() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();

		// With myseat=0, seat 0 -> player 0, seat 1 -> player 1
		assertEquals(0, mode.netvsGetPlayerIDbySeatID(0, 0));
		assertEquals(1, mode.netvsGetPlayerIDbySeatID(1, 0));
		// With myseat=1, seat 0 -> player 1, seat 1 -> player 0
		assertEquals(1, mode.netvsGetPlayerIDbySeatID(0, 1));
		assertEquals(0, mode.netvsGetPlayerIDbySeatID(1, 1));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(NetDummyVSMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readFieldByClass(Object mode, Class<?> cls, String name) throws Exception {
		Field f = cls.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setBoolean(Object mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void setInt(Object mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
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
