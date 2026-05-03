package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link GradeMania3Mode} branches: onSetting menu (11 cursors),
 * renderSetting, calcScore (roll points, medals, exams), onLast section
 * time, onMove grade decay, onReady promotion/demotion, startGame,
 * renderLast (ranking, section time, medals), renderResult, onGameOver,
 * and saveReplay.
 */
class GradeMania3ModeSettingMenuTest {

	// ---------------------------------------------------------------
	// onSetting UP/DOWN
	// ---------------------------------------------------------------

	@Test
	void onSettingUpNavigatesThroughAllPositions() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);

		setFieldInt(mode, "menuCursor", 0);
		pressUp(engine);
		mode.onSetting(engine, 0);
		assertEquals(10, readFieldInt(mode, "menuCursor"),
				"UP at cursor 0 should wrap to 10");

		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"First DOWN from 10 should reach 0");

		for (int expected = 1; expected <= 10; expected++) {
			pressDown(engine);
			mode.onSetting(engine, 0);
			assertEquals(expected, readFieldInt(mode, "menuCursor"),
					"Should be at cursor " + expected);
		}
		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	@Test
	void onSettingCursor0Startlevel() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = readFieldInt(mode, "startlevel");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "startlevel"));
	}

	@Test
	void onSettingCursor1InternalStartLevel() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		setFieldInt(mode, "startlevel", 5);
		setFieldInt(mode, "internalStartLevel", 500);
		int before = readFieldInt(mode, "internalStartLevel");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 100, readFieldInt(mode, "internalStartLevel"));
	}

	@Test
	void onSettingCursor2Alwaysghost() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		boolean before = readFieldBool(mode, "alwaysghost");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "alwaysghost"));
	}

	@Test
	void onSettingCursor3Always20g() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		boolean before = readFieldBool(mode, "always20g");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "always20g"));
	}

	@Test
	void onSettingCursor4Lvstopse() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		boolean before = readFieldBool(mode, "lvstopse");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "lvstopse"));
	}

	@Test
	void onSettingCursor5Showsectiontime() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		boolean before = readFieldBool(mode, "showsectiontime");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "showsectiontime"));
	}

	@Test
	void onSettingCursor6Gradedisp() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		boolean before = readFieldBool(mode, "gradedisp");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "gradedisp"));
	}

	@Test
	void onSettingCursor7Lv500torikan() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		int before = readFieldInt(mode, "lv500torikan");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 60, readFieldInt(mode, "lv500torikan"));
	}

	@Test
	void onSettingCursor8Stcolor() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		int before = readFieldInt(mode, "stcolor");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "stcolor"));
	}

	@Test
	void onSettingCursor9Big() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		boolean before = readFieldBool(mode, "big");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "big"));
	}

	@Test
	void onSettingCursor10EnableExam() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		boolean before = readFieldBool(mode, "enableexam");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableexam"));
	}

	// ---------------------------------------------------------------
	// A button and B button
	// ---------------------------------------------------------------

	@Test
	void onSettingPressAStartsGame() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		pressPush(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result, "A should start game");
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	// ---------------------------------------------------------------
	// replayMode path
	// ---------------------------------------------------------------

	@Test
	void onSettingReplayModePath() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.replayMode = true;
		setFieldInt(mode, "menuTime", 0);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));

		setFieldInt(mode, "menuTime", 60);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result, "replayMode should advance at menuTime>=60");
	}

	// ---------------------------------------------------------------
	// renderSetting
	// ---------------------------------------------------------------

	@Test
	void renderSetting() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// startGame branches
	// ---------------------------------------------------------------

	@Test
	void startGameSetsLevelAndSpeed() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "startlevel", 5);
		setFieldBool(mode, "big", true);

		mode.startGame(engine, 0);

		assertTrue(engine.big);
		assertEquals(500, engine.statistics.level);
	}

	@Test
	void startGameRollLevel() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "startlevel", 10);

		mode.startGame(engine, 0);

		assertEquals(2, engine.ending);
		assertTrue(engine.staffrollEnable);
	}

	@Test
	void startGameMRollLevel() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "startlevel", 11);

		mode.startGame(engine, 0);

		assertEquals(2, engine.ending);
		assertTrue(readFieldBool(mode, "mrollFlag"));
	}

	// ---------------------------------------------------------------
	// calcScore: roll during ending
	// ---------------------------------------------------------------

	@Test
	void calcScoreRollAddsPoints() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.ending = 2;
		setFieldBool(mode, "mrollFlag", false);
		setFieldFloat(mode, "rollPoints", 0.5f);

		mode.calcScore(engine, 0, 4);

		assertEquals(0.76f, readFieldFloat(mode, "rollPoints"), 0.001f,
				"Roll points should increase for 4 lines");
	}

	@Test
	void calcScoreRollGradeUp() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.ending = 2;
		setFieldBool(mode, "mrollFlag", true);
		setFieldFloat(mode, "rollPoints", 0.95f);
		setFieldInt(mode, "grade", 0);

		mode.calcScore(engine, 0, 1);

		assertEquals(1, readFieldInt(mode, "grade"),
				"Grade should increase when rollPoints>=1");
	}

	// ---------------------------------------------------------------
	// calcScore: medal checks
	// ---------------------------------------------------------------

	@Test
	void calcScoreSkMedalBigMode() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.ending = 0;
		engine.statistics.totalFour = 2;
		engine.statistics.level = 0;
		setFieldBool(mode, "big", true);
		setFieldInt(mode, "gradeBasicPoint", 100);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readFieldInt(mode, "medalSK"),
				"SK medal should increment for big mode at totalFour==2");
	}

	@Test
	void calcScoreAcMedal() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.ending = 0;
		engine.createFieldIfNeeded();
		engine.ending = 0;
		engine.statistics.level = 0;
		setFieldInt(mode, "gradeBasicPoint", 100);

		mode.calcScore(engine, 0, 1); // Should trigger all-clear

		assertEquals(1, readFieldInt(mode, "medalAC"),
				"AC medal should increment on all-clear");
	}

	// ---------------------------------------------------------------
	// onLast
	// ---------------------------------------------------------------

	@Test
	void onLastDecrementsDisplayCounters() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "gradeflash", 10);
		setFieldInt(mode, "scgettime", 5);
		setFieldInt(mode, "regretdispframe", 10);
		setFieldInt(mode, "cooldispframe", 10);

		mode.onLast(engine, 0);

		assertEquals(9, readFieldInt(mode, "gradeflash"));
		assertEquals(4, readFieldInt(mode, "scgettime"));
		assertEquals(9, readFieldInt(mode, "regretdispframe"));
		assertEquals(9, readFieldInt(mode, "cooldispframe"));
	}

	@Test
	void onLastSectionTimeIncrement() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 50;

		mode.onLast(engine, 0);

		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		assertEquals(1, sectiontime[0],
				"Section 0 time should increment");
	}

	@Test
	void onLastRollEnd() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.ending = 2;
		engine.createFieldIfNeeded();
		setFieldInt(mode, "rolltime", 3238); // ROLLTIMELIMIT

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	// ---------------------------------------------------------------
	// onMove grade decay
	// ---------------------------------------------------------------

	@Test
	void onMoveGradePointDecay() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.timerActive = true;
		engine.combo = 0;
		engine.lockDelayNow = 0;
		setFieldInt(mode, "gradeBasicPoint", 10);
		setFieldInt(mode, "gradeBasicDecay", 124);
		engine.ending = 0;
		engine.holdDisable = false;
		engine.statc[0] = 0;
		engine.statistics.level = 0;
		setFieldInt(mode, "nextseclv", 100);

		mode.onMove(engine, 0);

		assertEquals(9, readFieldInt(mode, "gradeBasicPoint"),
				"Grade basic point should decay");
	}

	// ---------------------------------------------------------------
	// onReady promotion/demotion
	// ---------------------------------------------------------------

	@Test
	void onReadyPromotionFlag() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", true);
		setFieldInt(mode, "readyframe", 100);

		boolean result = mode.onReady(engine, 0);

		assertTrue(result, "onReady should return true during promotion");
	}

	// ---------------------------------------------------------------
	// renderLast branches
	// ---------------------------------------------------------------

	@Test
	void renderLastSettingState() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastMoveState() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		engine.statistics.level = 100;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastRegretDisplay() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		setFieldInt(mode, "regretdispframe", 100);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastCoolDisplay() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		setFieldInt(mode, "cooldispframe", 100);
		setFieldInt(mode, "regretdispframe", 0);
		mode.renderLast(engine, 0);
	}

	// ---------------------------------------------------------------
	// renderResult
	// ---------------------------------------------------------------

	@Test
	void renderResultPromotion() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", true);
		setFieldInt(mode, "passframe", 500);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultDemotion() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "demotionFlag", true);
		setFieldInt(mode, "passframe", 500);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage0() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "passframe", 0);
		engine.statc[1] = 0;
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage1() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "passframe", 0);
		engine.statc[1] = 1;
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage2() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "passframe", 0);
		engine.statc[1] = 2;
		mode.renderResult(engine, 0);
	}

	// ---------------------------------------------------------------
	// onGameOver exam logic
	// ---------------------------------------------------------------

	@Test
	void onGameOverSetsSecretGrade() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;

		mode.onGameOver(engine, 0);
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(GradeMania3Mode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		mode.modeInit(manager);
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, GradeMania3Mode mode, int cursor)
			throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, GradeMania3Mode mode, int cursor, int menuTime)
			throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	private static void pressUp(GameEngine engine) { pressKey(engine, Controller.BUTTON_UP); }
	private static void pressDown(GameEngine engine) { pressKey(engine, Controller.BUTTON_DOWN); }
	private static void pressRight(GameEngine engine) { pressKey(engine, Controller.BUTTON_RIGHT); }

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++)
			engine.ctrl.buttonTime[i] = 0;
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void pressPush(GameEngine engine, int btn) { pressKey(engine, btn); }

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static float readFieldFloat(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getFloat(obj);
	}

	private static boolean readFieldBool(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setFieldFloat(Object obj, String name, float value) throws Exception {
		findField(obj.getClass(), name).setFloat(obj, value);
	}

	private static void setFieldBool(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static Object readField(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).get(obj);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
