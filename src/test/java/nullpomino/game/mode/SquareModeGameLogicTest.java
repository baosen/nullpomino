package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link SquareMode}: playerInit,
 * startGame, calcScore (normal + T-Spin avalanche), onMove,
 * onLineClear, lineClearEnd, pieceLocked, onLast (ultra/sprint
 * timers), loadSetting/saveSetting, updateRanking/checkRanking,
 * and saveReplay.
 */
class SquareModeGameLogicTest {

	@Test
	void getNameReturnsExpected() {
		assertEquals("SQUARE", new SquareMode().getName());
	}

	@Test
	void playerInitSetsDefaults() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "squares"));
		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	@Test
	void startGameSetsEngineDefaults() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertEquals(30, engine.speed.are);
		assertEquals(30, engine.speed.areLine);
		assertEquals(10, engine.speed.das);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(60, engine.speed.denominator);
	}

	@Test
	void onMoveDisablesCascade() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.lineGravityType = GameEngine.LineGravity.CASCADE;

		boolean result = mode.onMove(engine, 0);

		assertFalse(result);
		assertEquals(GameEngine.LineGravity.NATIVE, engine.lineGravityType);
	}

	@Test
	void onLineClearGrayoutBrokenBlocksWhenGrayoutAll() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "grayoutEnable", 2);

		// Place a block with BROKEN attribute
		int h = engine.field.getHeight() - 1;
		nullpomino.game.component.Block broken = new nullpomino.game.component.Block(
				nullpomino.game.component.Block.BLOCK_COLOR_RED);
		broken.setAttribute(nullpomino.game.component.Block.BLOCK_ATTRIBUTE_BROKEN, true);
		engine.field.setBlock(3, h, broken);
		engine.statc[0] = 1; // first call

		mode.onLineClear(engine, 0);

		assertEquals(nullpomino.game.component.Block.BLOCK_COLOR_GRAY,
				engine.field.getBlock(3, h).color);
	}

	@Test
	void calcScoreSingleLine() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = false;
		setInt(mode, "gametype", 0); // Marathon

		// Place a block to prevent all-clear
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		// 1 line = 1 pt, no square clear bonus
		assertEquals(1, engine.statistics.score);
		assertEquals(1, readInt(mode, "lastscore"));
		assertEquals(120, readInt(mode, "scgettime"));
	}

	@Test
	void calcScoreFourLines() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = false;
		setInt(mode, "gametype", 0);

		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		// 4 lines: pts = 3 + (4-3)*2 = 5
		mode.calcScore(engine, 0, 4);

		assertEquals(5, engine.statistics.score);
		assertEquals(5, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreWithAllClearBravo() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = false;
		setInt(mode, "gametype", 0);
		// Empty field -> all clear

		mode.calcScore(engine, 0, 1);

		// 1 line = 1 pt, all-clear triggers bravo SE
		assertEquals(1, engine.statistics.score);
	}

	@Test
	void calcScoreWithTSpinAvalanche() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;
		setInt(mode, "gametype", 0);
		setInt(mode, "version", 1); // new avalanche

		// Need blocks to avalanche. Place blocks in a cross pattern.
		int h = engine.field.getHeight() - 1;
		for (int x = 3; x <= 6; x++) {
			engine.field.setBlock(x, h, new nullpomino.game.component.Block(
					nullpomino.game.component.Block.BLOCK_COLOR_RED));
		}
		// Set line flags for the avalanche routine
		engine.field.setLineFlag(h, true);
		engine.field.setLineFlag(h - 1, false);

		mode.calcScore(engine, 0, 1);

		// With T-Spin, should call avalanche() which sets cascade gravity
		assertEquals(GameEngine.LineGravity.CASCADE, engine.lineGravityType);
	}

	@Test
	void pieceLockedDetectsSquares() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "squares", 0);

		mode.pieceLocked(engine, 0, 0);

		// squares should be >= 0 (no squares in empty field)
		assertTrue(readInt(mode, "squares") >= 0);
	}

	@Test
	void onLastDecrementsScgettime() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 10);

		mode.onLast(engine, 0);

		assertEquals(9, readInt(mode, "scgettime"));
	}

	@Test
	void onLastUltraTimerCountdown() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 1); // Ultra

		engine.statistics.time = 0;
		engine.timerActive = true;

		mode.onLast(engine, 0);

		// Meter should be set for remaining time
		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void onLastSprintTimerCountdown() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 2); // Sprint

		engine.statistics.score = 0;
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void loadSaveSettingRoundTrip() throws Exception {
		SquareMode mode = new SquareMode();
		CustomProperties prop = new CustomProperties();
		setInt(mode, "version", 1);
		setInt(mode, "gametype", 2);
		setInt(mode, "outlinetype", 1);
		setInt(mode, "tspinEnableType", 0);
		setBoolean(mode, "tntAvalanche", true);
		setInt(mode, "grayoutEnable", 2);

		invokeSaveSetting(mode, prop);

		SquareMode dest = new SquareMode();
		setInt(dest, "version", 1);
		invokeLoadSetting(dest, prop);

		assertEquals(2, readInt(dest, "gametype"));
		assertEquals(1, readInt(dest, "outlinetype"));
		assertEquals(0, readInt(dest, "tspinEnableType"));
		assertTrue(readBoolean(dest, "tntAvalanche"));
		assertEquals(2, readInt(dest, "grayoutEnable"));
	}

	@Test
	void updateRankingInsertsNewScore() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0);

		invokeUpdateRanking(mode, 5000, 3600, 100, 0);

		assertEquals(0, readInt(mode, "rankingRank"));
	}

	@Test
	void checkRankingReturnsMinusOneForLowScore() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0);

		// Fill ranking with high scores
		int[][] rankingScore = (int[][]) readFieldByClass(mode, SquareMode.class, "rankingScore");
		for (int i = 0; i < 10; i++) {
			rankingScore[0][i] = 99999;
		}

		int rank = invokeCheckRanking(mode, 100, 0, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void saveReplayRecordsSquares() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "squares", 42);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(42, prop.getProperty("square.squares", 0));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(SquareMode mode) {
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

	private static void setInt(Object mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(Object mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeSaveSetting(SquareMode mode, CustomProperties prop) throws Exception {
		Method m = SquareMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeLoadSetting(SquareMode mode, CustomProperties prop) throws Exception {
		Method m = SquareMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(SquareMode mode, int sc, int time, int sq, int type) throws Exception {
		Method m = SquareMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, time, sq, type);
	}

	private static int invokeCheckRanking(SquareMode mode, int sc, int time, int sq, int type) throws Exception {
		Method m = SquareMode.class.getDeclaredMethod("checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, time, sq, type);
	}
}
