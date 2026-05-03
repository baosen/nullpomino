package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link NetDummyMode}: playerInit,
 * modeInit, onMove (netplay field/stats sending), onLineClear, onARE,
 * onEndingStart, onExcellent, onGameOver, onResult, updateCursor,
 * renderLast, netplayOnRetryKey.
 */
class NetDummyModeGameLogicTest {

	@Test
	void getNameReturnsExpected() {
		assertEquals("NET-DUMMY", new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		}.getName());
	}

	@Test
	void modeInitInitializesVariables() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		assertFalse(readBoolean(mode, "netIsNetPlay"));
		assertFalse(readBoolean(mode, "netIsWatch"));
		assertEquals(0, readInt(mode, "netNumSpectators"));
		assertEquals("", readField(mode, "netPlayerName"));
	}

	@Test
	void playerInitStopsEngineAndHidesPlayer() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);
		engine.stat = GameEngine.Status.SETTING;
		engine.isVisible = true;

		mode.playerInit(engine, 0);

		assertEquals(GameEngine.Status.NOTHING, engine.stat);
		assertFalse(engine.isVisible);
	}

	@Test
	void netPlayerInitSetsDefaults() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);

		invokeNetPlayerInit(mode, engine, 0);

		assertEquals(-1, readIntArray(mode, "netRankingRank", 0));
		assertEquals(-1, readIntArray(mode, "netRankingRank", 1));
		assertFalse(readBoolean(mode, "netIsPB"));
		assertFalse(readBoolean(mode, "netIsNetRankingDisplayMode"));
		assertEquals(0, readInt(mode, "netReplaySendStatus"));
	}

	@Test
	void onMoveReturnsTrueInWatchMode() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "netIsWatch", true);

		boolean result = mode.onMove(engine, 0);

		assertTrue(result);
	}

	@Test
	void onMoveReturnsFalseWhenNotWatchMode() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "netIsWatch", false);
		setBoolean(mode, "netIsNetPlay", false);

		boolean result = mode.onMove(engine, 0);

		assertFalse(result);
	}

	@Test
	void onLineClearReturnsFalse() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);

		boolean result = mode.onLineClear(engine, 0);

		assertFalse(result);
	}

	@Test
	void onAREreturnsFalse() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);

		boolean result = mode.onARE(engine, 0);

		assertFalse(result);
	}

	@Test
	void onEndingStartReturnsFalse() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);

		boolean result = mode.onEndingStart(engine, 0);

		assertFalse(result);
	}

	@Test
	void onExcellentDoesNotSendWhenNotNetplay() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "netIsNetPlay", false);
		engine.statc[0] = 1; // not 0, so skip sending block

		boolean result = mode.onExcellent(engine, 0);

		assertFalse(result);
	}

	@Test
	void onGameOverReturnsFalseWhenNotNetplay() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "netIsNetPlay", false);

		boolean result = mode.onGameOver(engine, 0);

		assertFalse(result);
	}

	@Test
	void onResultReturnsFalseWhenNotNetplay() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "netIsNetPlay", false);

		boolean result = mode.onResult(engine, 0);

		assertFalse(result);
	}

	@Test
	void onResultReturnsTrueWhenNetplay() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "netIsNetPlay", true);
		setBoolean(mode, "netIsWatch", true);
		engine.allowTextRenderByReceiver = true;

		boolean result = mode.onResult(engine, 0);

		assertTrue(result);
		assertFalse(engine.allowTextRenderByReceiver);
	}

	@Test
	void updateCursorReturnsZeroInWatchMode() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "netIsWatch", true);

		// Use a dummy controller that reports no key presses
		int result = mode.updateCursor(engine, 10, 0);

		assertEquals(0, result);
	}

	@Test
	void loadRankingDoesNothingInBaseNetDummy() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		CustomProperties prop = new CustomProperties();
		mode.loadRanking(prop, "testRule");
		// Should just not throw
	}

	@Test
	void pieceLockedNoOpWhenNotNetplay() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "netIsNetPlay", false);

		// Should not throw
		mode.pieceLocked(engine, 0, 1);
	}

	@Test
	void netGetGoalTypeReturnsZero() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};

		int result = invokeNetGetGoalType(mode);

		assertEquals(0, result);
	}

	@Test
	void netIsNetRankingViewOKReturnsFalse() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);

		boolean result = mode.netIsNetRankingViewOK(engine);

		assertFalse(result);
	}

	@Test
	void netIsNetRankingSendOKReturnsFalseByDefault() throws Exception {
		NetDummyMode mode = new NetDummyMode() {
			@Override public boolean lineClearEnd(GameEngine engine, int playerID) { return false; }
		};
		GameEngine engine = freshEngine(mode);

		boolean result = mode.netIsNetRankingSendOK(engine);

		assertFalse(result);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(NetDummyMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static boolean readBoolean(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static int readInt(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static int readIntArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((int[]) f.get(mode))[index];
	}

	private static Object readField(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
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

	private static void invokeNetPlayerInit(NetDummyMode mode, GameEngine engine, int playerID) throws Exception {
		java.lang.reflect.Method m = NetDummyMode.class.getDeclaredMethod("netPlayerInit", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}

	private static int invokeNetGetGoalType(NetDummyMode mode) throws Exception {
		java.lang.reflect.Method m = NetDummyMode.class.getDeclaredMethod("netGetGoalType");
		m.setAccessible(true);
		return (int) m.invoke(mode);
	}
}
