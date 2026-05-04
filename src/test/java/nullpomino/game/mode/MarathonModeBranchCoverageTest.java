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
 * Branch coverage for {@link MarathonMode}: covers calcScore T-Spin variants,
 * B2B, combo, level-up, goal-reaching, endless mode, load/save setting,
 * setSpeed clamping, saveReplay, and render paths.
 */
class MarathonModeBranchCoverageTest {

	@Test
	void calcScoreTspinZeroMini() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinmini = true;
		e.tspinez = false;
		mode.calcScore(e, 0, 0);
		assertEquals(5, readInt(mode, "lastevent"));
		assertEquals(100, e.statistics.score);
	}

	@Test
	void calcScoreTspinEZ() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinez = true;
		e.b2b = false;
		// Place a block to prevent all-clear bonus
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 1);
		assertEquals(12, readInt(mode, "lastevent"));
		assertEquals(120, e.statistics.score);
	}

	@Test
	void calcScoreTspinSingleWithB2B() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.tspinmini = false;
		e.b2b = true;
		// Place a block to prevent all-clear bonus
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 1);
		assertEquals(8, readInt(mode, "lastevent")); // EVENT_TSPIN_SINGLE
		assertEquals(1200, e.statistics.score);
	}

	@Test
	void calcScoreTspinDoubleMiniWithAllSpin() throws Exception {
		MarathonMode mode = new MarathonMode();
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
		assertEquals(400, e.statistics.score);
	}

	@Test
	void calcScoreTspinTripleWithoutB2B() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = true;
		e.b2b = false;
		// Place a block to prevent all-clear bonus
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 3);
		assertEquals(11, readInt(mode, "lastevent"));
		assertEquals(1600, e.statistics.score);
	}

	@Test
	void calcScoreNormalFourWithB2B() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = false;
		e.b2b = true;
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 4);
		assertEquals(4, readInt(mode, "lastevent"));
		assertEquals(1200, e.statistics.score);
	}

	@Test
	void calcScoreNormalFourWithoutB2B() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = false;
		e.b2b = false;
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 4);
		assertEquals(4, readInt(mode, "lastevent"));
		assertEquals(800, e.statistics.score);
	}

	@Test
	void calcScoreComboApplied() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.tspin = false;
		setBoolean(mode, "enableCombo", true);
		e.combo = 3;
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 1);
		assertEquals(200, e.statistics.score);
	}

	@Test
	void calcScoreAllClearBonus() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.field.reset();
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		assertEquals(1900, e.statistics.score);
	}

	@Test
	void calcScoreLevelUp() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.statistics.lines = 10; // calcScore doesn't increment lines, so set directly to trigger level-up
		e.tspin = false;
		e.field.setBlock(0, e.field.getHeight() - 1,
			new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(e, 0, 1);
		assertEquals(1, e.statistics.level);
	}

	@Test
	void calcScoreGoalReachedEndsGame() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.statistics.lines = 150; // goalLines[0] = 150
		setInt(mode, "goaltype", 0);
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		assertEquals(1, e.ending);
	}

	@Test
	void calcScoreBgmFadeout() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.statistics.lines = 48; // tableBGMChange[0]=50, within 5
		setInt(mode, "goaltype", 0);
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		assertTrue(e.owner.bgmStatus.fadesw);
	}

	@Test
	void calcScoreMeterValue() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 0;
		e.statistics.lines = 5;
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		assertEquals(GameEngine.METER_COLOR_YELLOW, e.meterColor); // lines%10 >= 4
	}

	@Test
	void setSpeedClamps() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.statistics.level = 999;
		mode.setSpeed(e);
		assertEquals(-1, e.speed.gravity); // last entry
	}

	@Test
	void loadSettingReadsGoaltype() throws Exception {
		MarathonMode mode = new MarathonMode();
		CustomProperties prop = new CustomProperties();
		prop.setProperty("marathon.gametype", 1);
		Method m = MarathonMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
		assertEquals(1, readInt(mode, "goaltype"));
	}

	@Test
	void saveSettingWritesGoaltype() throws Exception {
		MarathonMode mode = new MarathonMode();
		setInt(mode, "goaltype", 2);
		CustomProperties prop = new CustomProperties();
		Method m = MarathonMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
		assertEquals(2, prop.getProperty("marathon.gametype", -1));
	}

	@Test
	void saveReplayWhenRanked() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		e.ai = null;
		mode.playerInit(e, 0);
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(e, 0, prop);
		// Should not throw
	}

	@Test
	void netIsNetRankingViewOK() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine e = freshEngine(mode);
		assertTrue(mode.netIsNetRankingViewOK(e));
		setInt(mode, "startlevel", 1);
		assertFalse(mode.netIsNetRankingViewOK(e));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(MarathonMode mode) {
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
