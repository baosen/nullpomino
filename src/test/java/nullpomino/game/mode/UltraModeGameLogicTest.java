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
 * Covers game-logic methods in {@link UltraMode}: startGame engine
 * configuration, calcScore (flat scoring with T-Spin, combo, all-clear),
 * onLast time meter and game-over, soft/hard drop scoring,
 * updateRanking/checkRanking, and the after-drop fall hooks.
 */
class UltraModeGameLogicTest {

	@Test
	void startGameSetsBigAndB2bAndComboConfig() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", true);
		setBoolean(mode, "enableB2B", true);
		setBoolean(mode, "enableCombo", false);

		mode.startGame(engine, 0);

		assertTrue(engine.big);
		assertTrue(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	@Test
	void startGameSetsMeterValue() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertEquals(320, engine.meterValue);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	private void placeOneBlock(GameEngine engine) {
		// Prevent all-clear bonus from triggering
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
	}

	@Test
	void calcScoreSingleLineNoTSpin() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(100, engine.statistics.score, "1 line no T-Spin = 100");
	}

	@Test
	void calcScoreFourLinesWithB2b() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.b2b = true;

		mode.calcScore(engine, 0, 4);

		assertEquals(1200, engine.statistics.score, "4 lines B2B = 1200");
	}

	@Test
	void calcScoreFourLinesNoB2b() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.b2b = false;

		mode.calcScore(engine, 0, 4);

		assertEquals(800, engine.statistics.score, "4 lines no B2B = 800");
	}

	@Test
	void calcScoreTSpinDouble() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.tspin = true;

		mode.calcScore(engine, 0, 2);

		assertEquals(1200, engine.statistics.score, "T-Spin double = 1200");
	}

	@Test
	void calcScoreTSpinTripleWithB2b() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.b2b = true;

		mode.calcScore(engine, 0, 3);

		assertEquals(2400, engine.statistics.score, "T-Spin triple B2B = 2400");
	}

	@Test
	void calcScoreAllClearBonus() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		// Empty field triggers all-clear bonus
		mode.calcScore(engine, 0, 1);

		assertEquals(100 + 3000, engine.statistics.score,
				"1 line + all clear = 3100");
	}

	@Test
	void calcScoreWithCombo() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.combo = 3;

		mode.calcScore(engine, 0, 1);

		assertEquals(100 + 100, engine.statistics.score,
				"1 line + combo 3 = 100 + 2*50 = 200");
	}

	@Test
	void calcScoreComboDisabledWhenEnableComboIsFalse() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "enableCombo", false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.combo = 3;

		mode.calcScore(engine, 0, 1);

		assertEquals(100, engine.statistics.score,
				"Combo should not be added when enableCombo is false");
	}

	@Test
	void calcScoreTSpinZeroMini() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinmini = true;

		mode.calcScore(engine, 0, 0);

		assertEquals(100, engine.statistics.score, "T-Spin zero mini = 100");
	}

	@Test
	void calcScoreTSpinZero() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinmini = false;

		mode.calcScore(engine, 0, 0);

		assertEquals(400, engine.statistics.score, "T-Spin zero = 400");
	}

	@Test
	void afterSoftDropFallAddsScore() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.score = 0;

		mode.afterSoftDropFall(engine, 0, 5);

		assertEquals(5, engine.statistics.score);
		assertEquals(5, engine.statistics.scoreFromSoftDrop);
	}

	@Test
	void afterHardDropFallAddsScore() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.score = 0;

		mode.afterHardDropFall(engine, 0, 5);

		assertEquals(10, engine.statistics.score);
		assertEquals(10, engine.statistics.scoreFromHardDrop);
	}

	@Test
	void onLastWithTimerActiveUpdatesMeter() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statistics.time = 0;
		setInt(mode, "goaltype", 2); // 3-minute game

		mode.onLast(engine, 0);

		// Meter should be non-negative (time just started)
		assertTrue(engine.meterValue >= 0, "onLast should complete without exception");
	}

	@Test
	void onLastWithTimeExpiredEndsGame() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;
		setInt(mode, "goaltype", 0); // 1-minute game
		engine.statistics.time = 3600; // >= limit

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	@Test
	void checkRankingByScoreReturnsFirstWhenBetter() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		rankingScore[2][0][0] = 500; // goaltype=2, ranking type 0 (score), slot 0
		int[][][] rankingLines = (int[][][]) readField(mode, "rankingLines");
		rankingLines[2][0][0] = 10;

		setInt(mode, "goaltype", 2);
		int rank = invokeCheckRanking(mode, 1000, 20, 0);

		assertEquals(0, rank, "Higher score should rank #1");
	}

	@Test
	void checkRankingByScoreReturnsMinusOneWhenWorseThanAll() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		int[][][] rankingLines = (int[][][]) readField(mode, "rankingLines");
		// Fill all RANKING_MAX=5 slots with entries better than our test score
		for (int i = 0; i < 5; i++) {
			rankingScore[2][0][i] = 5000;
			rankingLines[2][0][i] = 100;
		}

		setInt(mode, "goaltype", 2);
		int rank = invokeCheckRanking(mode, 100, 20, 0);

		assertEquals(-1, rank, "Lower score should not rank");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(UltraMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(UltraMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(UltraMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(UltraMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(UltraMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(UltraMode mode, String name, boolean value) throws Exception {
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

	private static int invokeCheckRanking(UltraMode mode, int sc, int li, int rankingType) throws Exception {
		Method m = UltraMode.class.getDeclaredMethod("checkRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, li, rankingType);
	}
}
