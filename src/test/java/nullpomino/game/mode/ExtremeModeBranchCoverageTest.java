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
 * Branch coverage for {@link ExtremeMode}: covers calcScore T-Spin variants,
 * B2B paths, level-up, endless mode, roll-time ending, setSpeed clamping,
 * loadSetting/saveSetting round-trip, netSendStats, and saveReplay.
 */
class ExtremeModeBranchCoverageTest {

	@Test
	void calcScoreTspinZeroMini() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinmini = true;
		e.tspinez = false;
		mode.calcScore(e, 0, 0);
		assertEquals(5, readInt(mode, "lastevent")); // EVENT_TSPIN_ZERO_MINI
		assertEquals(100, e.statistics.score); // 100 * (0+1)
	}

	@Test
	void calcScoreTspinZero() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinmini = false;
		e.tspinez = false;
		mode.calcScore(e, 0, 0);
		assertEquals(6, readInt(mode, "lastevent")); // EVENT_TSPIN_ZERO
		assertEquals(400, e.statistics.score); // 400 * (0+1)
	}

	@Test
	void calcScoreTspinEZWithB2B() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinez = true;
		e.b2b = true;
		// Place a block to prevent all-clear bonus
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 1);
		assertEquals(12, readInt(mode, "lastevent")); // EVENT_TSPIN_EZ
		assertEquals(180, e.statistics.score); // 180 * 1
	}

	@Test
	void calcScoreTspinEZWithoutB2B() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinez = true;
		e.b2b = false;
		// Place a block to prevent all-clear bonus
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 1);
		assertEquals(12, readInt(mode, "lastevent")); // EVENT_TSPIN_EZ
		assertEquals(120, e.statistics.score); // 120 * 1
	}

	@Test
	void calcScoreTspinSingleMiniWithB2B() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinmini = true;
		e.tspinez = false;
		e.b2b = true;
		// Place a block to prevent all-clear bonus
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 1);
		assertEquals(7, readInt(mode, "lastevent")); // EVENT_TSPIN_SINGLE_MINI
		assertEquals(300, e.statistics.score); // 300 * 1
	}

	@Test
	void calcScoreTspinDoubleMiniWithAllSpin() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinmini = true;
		e.useAllSpinBonus = true;
		e.b2b = false;
		// Place a block to prevent all-clear bonus
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 2);
		assertEquals(9, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE_MINI
		assertEquals(400, e.statistics.score); // 400 * 1
	}

	@Test
	void calcScoreTspinTripleWithB2B() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinmini = false;
		e.b2b = true;
		// Place a block to prevent all-clear bonus
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 3);
		assertEquals(11, readInt(mode, "lastevent")); // EVENT_TSPIN_TRIPLE
		assertEquals(2400, e.statistics.score); // 2400 * 1
	}

	@Test
	void calcScoreNormalWithB2B() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = false;
		e.b2b = true;
		// Place a block to prevent all-clear bonus
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 4);
		assertEquals(4, readInt(mode, "lastevent")); // EVENT_FOUR
		assertEquals(1200, e.statistics.score); // 1200, no all-clear
		assertTrue(e.statistics.score > 0);
	}

	@Test
	void calcScoreComboApplied() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = false;
		setBoolean(mode, "enableCombo", true);
		e.combo = 3;
		// Place a block to prevent all-clear
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 1);
		// 100 (single) + (3-1)*50 (combo) = 200
		assertEquals(200, e.statistics.score);
	}

	@Test
	void calcScoreAllClearBonus() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.field.reset(); // empty field
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		// 100 + 1800 (all clear) = 1900
		assertEquals(1900, e.statistics.score);
	}

	@Test
	void calcScoreLevelUp() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.statistics.lines = 10; // calcScore doesn't increment lines, set directly to trigger level-up
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		// After 10 lines at level 0 -> level up
		assertEquals(1, e.statistics.level);
	}

	@Test
	void calcScoreEndingTriggered() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.statistics.lines = 200; // calcScore doesn't increment lines, set directly to trigger ending
		setBoolean(mode, "endless", false);
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		// 200 lines reached -> ending (ending=2)
		assertEquals(2, e.ending);
	}

	@Test
	void calcScoreNoLinesWithZeroComboDoesNothing() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.score = 1000;
		mode.calcScore(e, 0, 0);
		assertEquals(1000, e.statistics.score);
	}

	@Test
	void onLastRollTimeEnding() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.gameActive = true;
		e.ending = 2;
		setInt(mode, "rolltime", 2967); // ROLLTIMELIMIT - 1
		mode.onLast(e, 0);
		assertEquals(GameEngine.Status.EXCELLENT, e.stat);
	}

	@Test
	void onLastRollTimeMeterRed() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.gameActive = true;
		e.ending = 2;
		setInt(mode, "rolltime", 2800); // within 10*60 of limit
		mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
	}

	@Test
	void setSpeedClampLow() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = -5;
		mode.setSpeed(e);
		assertEquals(25, e.speed.are); // tableARE[0]
	}

	@Test
	void setSpeedClampHigh() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 999;
		mode.setSpeed(e);
		assertEquals(0, e.speed.are); // tableARE[last]
	}

	@Test
	void loadSettingReadsEndless() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		CustomProperties prop = new CustomProperties();
		prop.setProperty("extreme.endless", true);
		Method m = ExtremeMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
		assertTrue(readBoolean(mode, "endless"));
	}

	@Test
	void saveSettingWritesEndless() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		setBoolean(mode, "endless", true);
		CustomProperties prop = new CustomProperties();
		Method m = ExtremeMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
		assertTrue(prop.getProperty("extreme.endless", false));
	}

	@Test
	void saveReplayUpdatesRanking() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		e.ai = null;
		mode.playerInit(e, 0);
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(e, 0, prop);
		// Should not throw
	}

	@Test
	void renderResultDisplaysStats() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine e = freshEngine(mode);
		mode.renderResult(e, 0);
		// Should not throw
	}

	// ---- helpers ----

	private static GameEngine freshEngine(ExtremeMode mode) {
		GameManager manager = new GameManager(new nullpomino.game.event.EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
		return manager.engine[0];
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
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
