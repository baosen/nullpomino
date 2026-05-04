package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers additional branches in {@link ScoreRaceMode}: calcScore T-Spin
 * double-mini/no-allspin, EZ-spin, T-Spin triple no B2B, pts=0 path,
 * combo-with-zero-lines, onLast meter-color thresholds and BGM-fade,
 * startGame T-Spin enableType variations and old-version path,
 * saveReplay ranking conditions, and loadRanking SPL default line=0 path.
 */
class ScoreRaceModeExtendedLogicTest {

	// -----------------------------------------------------------------------
	// calcScore additional branches
	// -----------------------------------------------------------------------

	@Test
	void calcScoreTSpinDoubleMiniWithAllSpin() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = true;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 2);

		// T-Spin double mini with all-spin: 400
		assertEquals(9, readInt(mode, "lastevent"));
		assertEquals(400, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinDoubleMiniNoAllSpin() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = false;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 2);

		// Falls back to T-Spin double: 1200
		assertEquals(10, readInt(mode, "lastevent"));
		assertEquals(1200, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinEZWithoutB2B() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinez = true;
		engine.b2b = false;
		engine.statistics.level = 0;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		// EZ spin no B2B: 120 * (0+1) = 120
		assertEquals(12, readInt(mode, "lastevent"));
		assertEquals(120, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinEZWithB2B() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinez = true;
		engine.b2b = true;
		engine.statistics.level = 0;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		// EZ spin B2B: 180 * (0+1) = 180
		assertEquals(12, readInt(mode, "lastevent"));
		assertEquals(180, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinTripleNoB2B() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.b2b = false;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 3);

		// T-Spin triple no B2B: 1600
		assertEquals(11, readInt(mode, "lastevent"));
		assertEquals(1600, readInt(mode, "lastscore"));
	}

	@Test
	void calcScorePtsZeroLeavesStateUntouched() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 500;
		setInt(mode, "lastscore", 300);
		setInt(mode, "scgettime", 50);

		mode.calcScore(engine, 0, 0);

		assertEquals(300, readInt(mode, "lastscore"));
		assertEquals(50, readInt(mode, "scgettime"));
		assertEquals(500, engine.statistics.score);
	}

	@Test
	void calcScoreComboNotAppliedWhenLinesZero() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 500;
		engine.combo = 3;
		setBoolean(mode, "enableCombo", true);

		mode.calcScore(engine, 0, 0);

		assertEquals(500, engine.statistics.score);
	}

	// -----------------------------------------------------------------------
	// onLast meter-color thresholds and BGM fade
	// -----------------------------------------------------------------------

	@Test
	void onLastMeterColorThresholds() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		setInt(mode, "goaltype", 0); // 10000 goal

		// remainScore = 10000 - 0 = 10000 → green
		engine.statistics.score = 0;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);

		// remainScore = 10000 - 2000 = 8000 → yellow (<=9600)
		engine.statistics.score = 2000;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);

		// remainScore = 10000 - 6000 = 4000 → orange (<=4800)
		engine.statistics.score = 6000;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);

		// remainScore = 10000 - 9000 = 1000 → red (<=2400)
		engine.statistics.score = 9000;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void onLastBGMFadeAtLowScore() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		setInt(mode, "goaltype", 0); // 10000 goal
		engine.statistics.score = 9100; // remainScore = 900

		mode.onLast(engine, 0);

		assertTrue(engine.owner.bgmStatus.fadesw);
	}

	@Test
	void onLastTimerInactiveSkipsGoalCheck() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = false;
		setInt(mode, "goaltype", 0);
		engine.statistics.score = 10000;

		mode.onLast(engine, 0);

		// Goal not checked when timerActive == false
		assertFalse(engine.stat == GameEngine.Status.ENDINGSTART);
	}

	// -----------------------------------------------------------------------
	// startGame T-Spin variations
	// -----------------------------------------------------------------------

	@Test
	void startGameWithTSpinDisabledType0() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1);
		setInt(mode, "tspinEnableType", 0);

		mode.startGame(engine, 0);

		assertFalse(engine.tspinEnable);
	}

	@Test
	void startGameWithAllSpinBonusType2() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1);
		setInt(mode, "tspinEnableType", 2);

		mode.startGame(engine, 0);

		assertTrue(engine.tspinEnable);
		assertTrue(engine.useAllSpinBonus);
	}

	@Test
	void startGameWithOldVersionUsesEnableTSpin() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 0);
		setBoolean(mode, "enableTSpin", false);

		mode.startGame(engine, 0);

		assertFalse(engine.tspinEnable);
	}

	// -----------------------------------------------------------------------
	// saveReplay ranking conditions
	// -----------------------------------------------------------------------

	@Test
	void saveReplaySkipsRankingWhenBigEnabled() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBoolean(mode, "big", true);
		engine.ai = null;
		engine.statistics.score = 10000;
		setInt(mode, "goaltype", 0); // 10000 goal

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	@Test
	void saveReplayUpdatesRankingWhenGoalReached() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		setBoolean(mode, "big", false);
		engine.statistics.score = 10000;
		engine.statistics.lines = 50;
		engine.statistics.time = 3600;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		// Should be ranked #1 since ranking table is empty
		assertEquals(0, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// loadRanking SPL default with zero lines
	// -----------------------------------------------------------------------

	@Test
	void loadRankingWithZeroLinesSetsSPLToZero() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("scorerace.ranking.Standard.0.time.0", 3600);
		prop.setProperty("scorerace.ranking.Standard.0.lines.0", 0);

		invokeLoadRanking(mode, prop, "Standard");

		double[][] rankingSPL = (double[][]) readField(mode, "rankingSPL");
		assertEquals(0.0, rankingSPL[0][0]);
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

	private static void invokeLoadRanking(ScoreRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = ScoreRaceMode.class.getDeclaredMethod("loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
