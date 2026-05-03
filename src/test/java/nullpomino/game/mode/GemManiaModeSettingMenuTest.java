package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link GemManiaMode#onSetting} menu branches (9 cursor
 * positions + edit screens), UP/DOWN navigation, A-button confirm,
 * B-button cancel, replayMode path, renderSetting page boundaries,
 * calcScore level-up/section logic, onLast timer/skip logic,
 * onMove gimmick logic, onCustom stage-end logic, and renderLast
 * ranking/time-display branches.
 */
class GemManiaModeSettingMenuTest {

	// ---------------------------------------------------------------
	// onSetting: UP/DOWN navigation (9 positions)
	// ---------------------------------------------------------------

	@Test
	void onSettingUpNavigatesThroughAllPositions() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);

		setFieldInt(mode, "menuCursor", 0);
		pressUp(engine);
		mode.onSetting(engine, 0);
		assertEquals(8, readFieldInt(mode, "menuCursor"),
				"UP at cursor 0 should wrap to 8");

		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"First DOWN from 8 should reach 0");

		for (int expected = 1; expected <= 8; expected++) {
			pressDown(engine);
			mode.onSetting(engine, 0);
			assertEquals(expected, readFieldInt(mode, "menuCursor"),
					"Should be at cursor " + expected);
		}
		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"DOWN from 8 should wrap to 0");
	}

	// ---------------------------------------------------------------
	// onSetting: LEFT/RIGHT at each cursor position
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor0Startstage() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = readFieldInt(mode, "startstage");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "startstage"));
	}

	@Test
	void onSettingCursor1Stageset() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = readFieldInt(mode, "stageset");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "stageset"));
	}

	@Test
	void onSettingCursor2Alwaysghost() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		boolean before = readFieldBool(mode, "alwaysghost");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "alwaysghost"));
	}

	@Test
	void onSettingCursor3Always20g() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		boolean before = readFieldBool(mode, "always20g");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "always20g"));
	}

	@Test
	void onSettingCursor4Lvstopse() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		boolean before = readFieldBool(mode, "lvstopse");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "lvstopse"));
	}

	@Test
	void onSettingCursor5Showsectiontime() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		boolean before = readFieldBool(mode, "showsectiontime");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "showsectiontime"));
	}

	@Test
	void onSettingCursor6Randomnext() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		boolean before = readFieldBool(mode, "randomnext");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "randomnext"));
	}

	@Test
	void onSettingCursor7TrainingType() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		int before = readFieldInt(mode, "trainingType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "trainingType"));
	}

	@Test
	void onSettingCursor8Startnextc() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		int before = readFieldInt(mode, "startnextc");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "startnextc"));
	}

	// ---------------------------------------------------------------
	// A button confirm and B button cancel
	// ---------------------------------------------------------------

	@Test
	void onSettingPressAStartsGame() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		pressPush(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result, "A should start game (return false)");
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingPressDEntersEditMode() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		pressPush(engine, Controller.BUTTON_D);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "editModeScreen"),
				"D should enter edit mode screen 1");
	}

	// ---------------------------------------------------------------
	// replayMode path
	// ---------------------------------------------------------------

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.replayMode = true;
		setFieldInt(mode, "menuTime", 0);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));

		setFieldInt(mode, "menuTime", 60);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result, "replayMode at menuTime>=60 should start game");
	}

	// ---------------------------------------------------------------
	// renderSetting pages
	// ---------------------------------------------------------------

	@Test
	void renderSettingNormalScreen() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "editModeScreen", 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingEditScreen1() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "editModeScreen", 1);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingEditScreen2() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "editModeScreen", 2);
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// calcScore branches
	// ---------------------------------------------------------------

	@Test
	void calcScoreGemClear() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		setFieldInt(mode, "rest", 10);

		mode.calcScore(engine, 0, 1);

		assertTrue(readFieldInt(mode, "rest") <= 10);
	}

	@Test
	void calcScoreLevelUpSection() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();
		engine.field.setLineFlag(engine.field.getHeight() - 1, true);
		engine.ending = 0;
		engine.statistics.level = 0;
		setFieldInt(mode, "speedlevel", 0);
		setFieldInt(mode, "nextseclv", 100);

		mode.calcScore(engine, 0, 4);

		assertTrue(readFieldInt(mode, "speedlevel") >= 4,
				"speedlevel should increase after 4-line clear");
	}

	// ---------------------------------------------------------------
	// onLast timer/skip branches
	// ---------------------------------------------------------------

	@Test
	void onLastDecrementsTimeextendDisp() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "timeextendDisp", 10);
		mode.onLast(engine, 0);
		assertEquals(9, readFieldInt(mode, "timeextendDisp"));
	}

	@Test
	void onLastSkipButtonHeldFor60Frames() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		setFieldInt(mode, "stage", 0);
		setFieldInt(mode, "limittimeNow", 3600);
		setFieldBool(mode, "clearflag", false);
		setFieldInt(mode, "skipbuttonPressTime", 59);
		pressPush(engine, Controller.BUTTON_F);
		mode.onLast(engine, 0);

		assertTrue(readFieldBool(mode, "skipflag"),
				"Holding F for 60 frames should trigger skip");
	}

	@Test
	void onLastTimerDecrements() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		setFieldInt(mode, "limittimeNow", 100);
		setFieldInt(mode, "stagetimeNow", 100);

		mode.onLast(engine, 0);

		assertEquals(99, readFieldInt(mode, "limittimeNow"));
		assertEquals(99, readFieldInt(mode, "stagetimeNow"));
	}

	// ---------------------------------------------------------------
	// onMove gimmick logic
	// ---------------------------------------------------------------

	@Test
	void onMoveSetsGimmickFlags() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.ending = 0;
		engine.holdDisable = false;
		engine.statc[0] = 0;
		engine.statistics.level = 0;
		setFieldInt(mode, "nextseclv", 100);
		setFieldInt(mode, "speedlevel", 0);
		setFieldInt(mode, "gimmickRoll", 1);
		setFieldInt(mode, "gimmickBig", 1);
		setFieldInt(mode, "gimmickXRay", 1);
		setFieldInt(mode, "gimmickColor", 1);
		setFieldInt(mode, "thisStageTotalPieceLockCount", 0);

		mode.onMove(engine, 0);

		assertTrue(engine.itemRollRollEnable,
				"Roll roll should be enabled when gimmickRoll triggers");
		assertTrue(engine.big,
				"Big should be enabled when gimmickBig triggers");
		assertTrue(engine.itemXRayEnable,
				"X-Ray should be enabled when gimmickXRay triggers");
		assertTrue(engine.itemColorEnable,
				"Color should be enabled when gimmickColor triggers");
	}

	// ---------------------------------------------------------------
	// onCustom stage end logic
	// ---------------------------------------------------------------

	@Test
	void onCustomStageClear() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.statc[0] = 0;
		setFieldBool(mode, "clearflag", true);
		setFieldInt(mode, "stage", 0);
		setFieldInt(mode, "laststage", 26); // MAX_STAGE_TOTAL-1

		mode.onCustom(engine, 0);

		assertTrue(readFieldInt(mode, "trystage") > 0,
				"trystage should increment");
	}

	@Test
	void onCustomStageSkip() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[0] = 0;
		setFieldBool(mode, "skipflag", true);
		setFieldInt(mode, "stage", 0);
		setFieldInt(mode, "limittimeStart", 3600);

		mode.onCustom(engine, 0);

		assertEquals(-2, ((int[]) readField(mode, "sectiontime"))[0],
				"Skipped stage sectiontime should be -2");
	}

	// ---------------------------------------------------------------
	// renderLast branches
	// ---------------------------------------------------------------

	@Test
	void renderLastSettingState() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.SETTING;
		setFieldInt(mode, "startstage", 0);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastGameplayState() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 0;
		engine.createFieldIfNeeded();
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWithGimmickMirror() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 0;
		setFieldInt(mode, "gimmickMirror", 5);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWithGimmickRoll() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 0;
		setFieldInt(mode, "gimmickRoll", 5);
		setFieldInt(mode, "gimmickMirror", 0);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWithGimmickBig() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 0;
		setFieldInt(mode, "gimmickBig", 5);
		setFieldInt(mode, "gimmickMirror", 0);
		setFieldInt(mode, "gimmickRoll", 0);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastTrainingMode() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		setFieldInt(mode, "trainingType", 1);
		mode.renderLast(engine, 0);
	}

	// ---------------------------------------------------------------
	// pieceLocked
	// ---------------------------------------------------------------

	@Test
	void pieceLockedIncrementsCount() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.pieceLocked(engine, 0, 0);
		assertEquals(1, readFieldInt(mode, "thisStageTotalPieceLockCount"));
	}

	// ---------------------------------------------------------------
	// onReady
	// ---------------------------------------------------------------

	@Test
	void onReadyLoadsStageSet() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[0] = 0;
		engine.readyDone = false;

		mode.onReady(engine, 0);

		assertEquals(0, readFieldInt(mode, "stage"),
				"Stage should be initialized to startstage(0)");
	}

	// ---------------------------------------------------------------
	// startGame
	// ---------------------------------------------------------------

	@Test
	void startGameEnablesItems() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "gimmickXRay", 5);
		setFieldInt(mode, "gimmickColor", 3);

		mode.startGame(engine, 0);

		assertTrue(engine.itemXRayEnable);
		assertTrue(engine.itemColorEnable);
	}

	// ---------------------------------------------------------------
	// onGameOver
	// ---------------------------------------------------------------

	@Test
	void onGameOverContinueYes() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		manager.modeConfig = new CustomProperties();
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		GameEngine engine = manager.engine[0];
		engine.ending = 0;
		engine.createFieldIfNeeded();
		setFieldBool(mode, "noContinue", false);
		engine.statc[0] = engine.field.getHeight() + 1;
		engine.statc[0] = 1; // Yes selected
		pressPush(engine, Controller.BUTTON_A);

		// Move statc past the field height for continue screen
		engine.statc[0] = engine.field.getHeight() + 1;

		// onGameOver should handle the continue selection
		assertTrue(mode.onGameOver(engine, 0),
				"Should return true from gameover (continue selected)");
	}

	// ---------------------------------------------------------------
	// renderResult
	// ---------------------------------------------------------------

	@Test
	void renderResultPage0() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[1] = 0;
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage1() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[1] = 1;
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage2() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[1] = 2;
		mode.renderResult(engine, 0);
	}

	// ---------------------------------------------------------------
	// saveReplay
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesSettings() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		manager.modeConfig = new CustomProperties();
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		GameEngine engine = manager.engine[0];

		mode.saveReplay(engine, 0, engine.owner.replayProp);

		assertEquals(0, engine.owner.replayProp.getProperty(
				"gemmania.startstage", -1));
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(GemManiaMode mode, boolean replayMode) {
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

	private static void setMenuState(GameEngine engine, GemManiaMode mode, int cursor)
			throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, GemManiaMode mode, int cursor, int menuTime)
			throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	private static void pressUp(GameEngine engine) {
		pressKey(engine, Controller.BUTTON_UP);
	}

	private static void pressDown(GameEngine engine) {
		pressKey(engine, Controller.BUTTON_DOWN);
	}

	private static void pressRight(GameEngine engine) {
		pressKey(engine, Controller.BUTTON_RIGHT);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void pressPush(GameEngine engine, int btn) {
		pressKey(engine, btn);
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static boolean readFieldBool(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
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
