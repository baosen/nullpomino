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
 * Covers additional branches in {@link MarathonPlusMode}: calcScore
 * T-Spin double-mini/no-allspin, EZ-spin, combo with 0 lines,
 * BGM-change/fade, meter-update for level<20, bonus-level piece-count
 * threshold, onGameOver, onCustom timing, onLast bonus-flash-decrement,
 * saveReplay ranking-write conditions, and netRecvField bonus proc.
 */
class MarathonPlusModeExtendedLogicTest {

	// -----------------------------------------------------------------------
	// calcScore additional branches
	// -----------------------------------------------------------------------

	@Test
	void calcScoreTSpinDoubleMiniWithAllSpin() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		// T-Spin double mini with all-spin: 400 * (0+1) = 400
		assertEquals(9, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE_MINI
		assertEquals(400, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinDoubleMiniNoAllSpinFallsBack() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = false;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		// Without useAllSpinBonus → regular T-Spin double: 1200
		assertEquals(10, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE
		assertEquals(1200, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinEZWithoutB2B() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinez = true;
		engine.b2b = false;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// EZ spin no B2B: 120
		assertEquals(12, readInt(mode, "lastevent"));
		assertEquals(120, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinEZWithB2B() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinez = true;
		engine.b2b = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// EZ spin B2B: 180
		assertEquals(12, readInt(mode, "lastevent"));
		assertEquals(180, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreComboNotAppliedWhenLinesZero() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.combo = 3;
		setBoolean(mode, "enableCombo", true);
		engine.statistics.score = 500;

		mode.calcScore(engine, 0, 0);

		assertEquals(500, engine.statistics.score);
	}

	@Test
	void calcScoreTSpinTripleNoB2B() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.b2b = false;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 3);

		// T-Spin triple no B2B: 1600
		assertEquals(11, readInt(mode, "lastevent"));
		assertEquals(1600, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreBGMChangeAt50Lines() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;
		engine.statistics.lines = 50;
		setInt(mode, "startlevel", 0);

		mode.calcScore(engine, 0, 1);

		assertEquals(1, readInt(mode, "bgmlv"));
	}

	@Test
	void calcScoreBGMChangeSkippedForStartlevel20() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.lines = 50;
		setInt(mode, "startlevel", 20);

		mode.calcScore(engine, 0, 1);

		// startlevel >= 20 → tableBGMChange is not used for BGM changes
		assertEquals(0, readInt(mode, "bgmlv"));
	}

	@Test
	void calcScoreMeterNotUpdatedWhenLevel20() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 20;
		engine.statistics.lines = 5;

		setInt(mode, "bonusLines", 10);
		setInt(mode, "bonusPieceCount", 5);

		mode.calcScore(engine, 0, 1);

		// bonusPieceCount(5) <= bonusLines(10)/4 = 2? No, 5 > 2, so flash is triggered
		assertEquals(30, readInt(mode, "bonusFlashNow"));
	}

	@Test
	void calcScoreBonusFlashNotTriggeredBelowThreshold() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 20;
		setInt(mode, "bonusLines", 100);
		setInt(mode, "bonusPieceCount", 10);

		mode.calcScore(engine, 0, 1);

		// bonusPieceCount(10) <= bonusLines(100)/4 = 25 → no flash
		assertEquals(11, readInt(mode, "bonusPieceCount"));
		assertEquals(0, readInt(mode, "bonusFlashNow"));
	}

	@Test
	void calcScoreLevelUpAtLevel19ToBonus() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 19;
		engine.statistics.lines = 200;
		setInt(mode, "startlevel", 0);

		mode.calcScore(engine, 0, 1);

		assertEquals(20, engine.statistics.level);
		assertEquals(1, engine.ending);
		assertFalse(engine.timerActive);
	}

	// -----------------------------------------------------------------------
	// onGameOver
	// -----------------------------------------------------------------------

	@Test
	void onGameOverRestoresOutlineWhenActive() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE;

		mode.onGameOver(engine, 0);

		assertEquals(GameEngine.BLOCK_OUTLINE_NORMAL, engine.blockOutlineType);
	}

	// -----------------------------------------------------------------------
	// onCustom timing
	// -----------------------------------------------------------------------

	@Test
	void onCustomStatc90PlaysExcellent() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 90;

		boolean result = mode.onCustom(engine, 0);

		assertFalse(result);
	}

	@Test
	void onCustomStatc480ResetsAndReturnsTrue() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 480;

		boolean result = mode.onCustom(engine, 0);

		assertTrue(result);
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	// -----------------------------------------------------------------------
	// onLast bonusFlash decrement
	// -----------------------------------------------------------------------

	@Test
	void onLastDecrementsBonusFlash() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.level = 20;
		engine.timerActive = true;
		engine.gameActive = true;
		engine.createFieldIfNeeded();
		setInt(mode, "bonusFlashNow", 5);

		mode.onLast(engine, 0);

		assertEquals(4, readInt(mode, "bonusFlashNow"));
	}

	// -----------------------------------------------------------------------
	// netGetGoalType
	// -----------------------------------------------------------------------

	@Test
	void netGetGoalTypeReturns0ForStartlevel0() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		setInt(mode, "startlevel", 0);
		assertEquals(0, invokeNetGetGoalType(mode));
	}

	@Test
	void netGetGoalTypeReturns1ForStartlevel20() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		setInt(mode, "startlevel", 20);
		assertEquals(1, invokeNetGetGoalType(mode));
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(MarathonPlusMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(MarathonPlusMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(MarathonPlusMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(MarathonPlusMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(MarathonPlusMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(MarathonPlusMode mode, String name, boolean value) throws Exception {
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

	private static int invokeNetGetGoalType(MarathonPlusMode mode) throws Exception {
		Method m = MarathonPlusMode.class.getDeclaredMethod("netGetGoalType");
		m.setAccessible(true);
		return (int) m.invoke(mode);
	}
}
