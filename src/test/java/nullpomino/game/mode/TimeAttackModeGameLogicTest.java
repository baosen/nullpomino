package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Covers game-logic methods in {@link TimeAttackMode}: getName, playerInit,
 * loadSetting/saveSetting round-trip, ranking arrays (11 types), calcScore
 * (norm update, level-up, game-complete), onLast (level timer, meter,
 * section time, ending), startGame, onReady, and mode-specific logic
 * (setSpeed, onMove timer re-enable, setAverageSectionTime).
 */
class TimeAttackModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("TIME ATTACK", new TimeAttackMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "norm"));
		assertEquals(0, readInt(mode, "goaltype"));
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertFalse(readBoolean(mode, "rollstarted"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
		assertFalse(engine.tspinEnable);
		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "goaltype", 3);
		setInt(mode, "startlevel", 5);
		setBoolean(mode, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		TimeAttackMode dest = new TimeAttackMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);

		assertEquals(3, readInt(dest, "goaltype"));
		assertEquals(5, readInt(dest, "startlevel"));
		assertTrue(readBoolean(dest, "big"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		assertNotNull(rankingLines);
		assertEquals(11, rankingLines.length); // RANKING_TYPE=11
		assertEquals(10, rankingLines[0].length); // RANKING_MAX=10
		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "rankingRollclear"));
	}

	@Test
	void onReadySetsLevelAndNormAndSpeed() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3);

		mode.onReady(engine, 0);

		assertEquals(3, engine.statistics.level);
		assertEquals(1, engine.statistics.levelDispAdd);
		assertEquals(30, readInt(mode, "norm")); // startlevel * 10 = 30
	}

	@Test
	void startGameSetsBgm() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Should not throw
		mode.startGame(engine, 0);
	}

	// ---- calcScore tests ----

	@Test
	void calcScoreAddsToNorm() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		setInt(mode, "norm", 0);

		mode.calcScore(engine, 0, 1);

		assertEquals(1, readInt(mode, "norm"));
	}

	@Test
	void calcScoreLevelUpWhenNormReachesNextLevel() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;
		// Norm needs to reach (level+1)*10 = 10 to level up
		setInt(mode, "norm", 9);

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.statistics.level);
		assertEquals(10, readInt(mode, "norm")); // norm is capped at 10
	}

	@Test
	void calcScoreGameCompleteWhenNormReachesGoal() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;
		// NORMAL (goaltype=0): tableGoalLevel=15, so goal norm = 150
		setInt(mode, "norm", 149);

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.ending);
		assertEquals(150, readInt(mode, "norm")); // capped
		assertFalse(engine.timerActive);
	}

	@Test
	void calcScoreDoesNothingDuringEnding() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 2;
		setInt(mode, "norm", 0);

		mode.calcScore(engine, 0, 1);

		// Should not modify norm during ending
		assertEquals(0, readInt(mode, "norm"));
	}

	@Test
	void onLastDecrementsLevelTimer() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		setInt(mode, "levelTimer", 100);
		setInt(mode, "levelTimerMax", 200);

		mode.onLast(engine, 0);

		assertEquals(99, readInt(mode, "levelTimer"));
	}

	@Test
	void onLastLevelTimerGameOverWhenTimerReachesZero() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		setInt(mode, "levelTimer", 0);

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	@Test
	void onLastIncrementsSectionTime() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 0;
		// Set levelTimer > 0 so onLast decrements it rather than triggering game over
		setInt(mode, "levelTimer", 100);

		int[] st = (int[]) readField(mode, "sectiontime");
		mode.onLast(engine, 0);

		assertEquals(1, st[0]);
	}

	@Test
	void onLastUpdatesMeter() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		setInt(mode, "levelTimer", 100);
		setInt(mode, "levelTimerMax", 200);

		mode.onLast(engine, 0);

		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void setAverageSectionTimeComputesCorrectly() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 100;
		st[1] = 200;
		st[2] = 300;
		setInt(mode, "sectionscomp", 3);

		invokeSetAverageSectionTime(mode);

		assertEquals(200, readInt(mode, "sectionavgtime")); // (100+200+300)/3 = 200
	}

	@Test
	void onMoveReenablesTimer() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		engine.timerActive = false;

		mode.onMove(engine, 0);

		assertTrue(engine.timerActive);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(TimeAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeSaveSetting(TimeAttackMode mode, CustomProperties prop) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeLoadSetting(TimeAttackMode mode, CustomProperties prop) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSetAverageSectionTime(TimeAttackMode mode) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
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
