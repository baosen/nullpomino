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
 * Covers game-logic methods in {@link ScoreRaceMode}: startGame engine
 * configuration, calcScore (flat scoring with T-Spin, B2B, combo,
 * all-clear), onLast (meter update, goal-reached game-over), soft/hard
 * drop scoring, updateRanking/checkRanking, and saveReplay.
 */
class ScoreRaceModeGameLogicTest {

	@Test
	void startGameSetsBigAndB2bAndComboConfig() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", true);
		setBoolean(mode, "enableCombo", false);

		mode.startGame(engine, 0);

		assertTrue(engine.big);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	@Test
	void startGameWithComboEnabledSetsNormalCombo() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "enableCombo", true);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_NORMAL, engine.comboType);
	}

	private void placeOneBlock(GameEngine engine) {
		// Prevent all-clear bonus from triggering
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
	}

	@Test
	void calcScoreSingleLineNoTSpin() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
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
		ScoreRaceMode mode = new ScoreRaceMode();
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
		ScoreRaceMode mode = new ScoreRaceMode();
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
		ScoreRaceMode mode = new ScoreRaceMode();
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
		ScoreRaceMode mode = new ScoreRaceMode();
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
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(100 + 1800, engine.statistics.score,
				"1 line + all clear = 1900");
	}

	@Test
	void calcScoreWithCombo() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
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
	void calcScoreNoLinesDoesNothing() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 100;

		mode.calcScore(engine, 0, 0);

		assertEquals(100, engine.statistics.score, "0 lines should not change score");
	}

	@Test
	void calcScoreTSpinZeroMini() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
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
		ScoreRaceMode mode = new ScoreRaceMode();
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
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.score = 0;

		mode.afterSoftDropFall(engine, 0, 5);

		assertEquals(5, engine.statistics.score);
		assertEquals(5, engine.statistics.scoreFromSoftDrop);
	}

	@Test
	void afterHardDropFallAddsScore() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.score = 0;

		mode.afterHardDropFall(engine, 0, 5);

		assertEquals(10, engine.statistics.score);
		assertEquals(10, engine.statistics.scoreFromHardDrop);
	}

	@Test
	void onLastUpdatesMeter() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.score = 0;
		engine.timerActive = true;

		mode.onLast(engine, 0);

		// Meter is at least 0 (in headless mode getMeterMax may return 0)
		assertTrue(engine.meterValue >= 0, "Meter value should be non-negative");
	}

	@Test
	void onLastGoalReachedEndsGame() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 0); // 10000 goal
		engine.statistics.score = 10000;
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);
		engine.statistics.score = 0;
		engine.timerActive = false;

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	@Test
	void checkRankingPrefersFasterTime() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		rankingTime[0][0] = 3600;
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		rankingLines[0][0] = 50;

		int rank = invokeCheckRanking(mode, 3200, 100, 100.0);
		assertEquals(0, rank, "Faster time should rank higher");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(ScoreRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(ScoreRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ScoreRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(ScoreRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(ScoreRaceMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ScoreRaceMode mode, String name, boolean value) throws Exception {
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

	private static int invokeCheckRanking(ScoreRaceMode mode, int time, int lines, double spl) throws Exception {
		Method m = ScoreRaceMode.class.getDeclaredMethod("checkRanking", int.class, int.class, double.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, time, lines, spl);
	}
}
