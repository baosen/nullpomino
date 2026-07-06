package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Fills remaining branch-coverage gaps in {@link GemManiaMode} game logic:
 * startStage BGM match, saveStageSet guards, setSpeed ghost guard,
 * checkStageEnd timer-inactive, edit-screen menu corner cases, onReady
 * re-entry, onLast skip/countdown combos, onMove/onARE level-stop guards,
 * calcScore gem/ending guards, onCustom time bonuses and training paths,
 * onGameOver continue screen, onResult wrap guards, saveReplay ranking
 * gate, and checkRanking tie-break comparisons.
 */
class GemManiaModeBranchGapTest {

	// ---------------------------------------------------------------
	// startStage / saveStageSet / setSpeed / checkStageEnd
	// ---------------------------------------------------------------

	@Test
	void startStageWithMatchingBgmDoesNotFade() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// First onReady loads the stage set and stagebgm
		engine.statc[0] = 0;
		engine.readyDone = false;
		mode.onReady(engine, 0);

		engine.owner.bgmStatus.bgm = readInt(mode, "stagebgm");
		engine.owner.bgmStatus.fadesw = false;

		invoke(mode, "startStage", new Class<?>[]{GameEngine.class}, engine);

		assertFalse(engine.owner.bgmStatus.fadesw,
				"No BGM fade when already playing the stage BGM");
	}

	@Test
	void saveStageSetWithNullPropDoesNothing() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		QuietReceiver rec = new QuietReceiver();
		GameEngine engine = freshEngine(mode, rec);
		mode.playerInit(engine, 0);

		assertNull(readField(mode, "propStageSet"), "propStageSet starts null");
		invoke(mode, "saveStageSet", new Class<?>[]{int.class}, -1);

		assertEquals(0, rec.savePropertiesCalls, "No save when propStageSet is null");
	}

	@Test
	void saveStageSetInReplayModeDoesNothing() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		QuietReceiver rec = new QuietReceiver();
		GameEngine engine = freshEngine(mode, rec);
		mode.playerInit(engine, 0);

		invoke(mode, "loadStageSet", new Class<?>[]{int.class}, -1);
		assertNotNull(readField(mode, "propStageSet"));
		engine.owner.replayMode = true;

		invoke(mode, "saveStageSet", new Class<?>[]{int.class}, -1);

		assertEquals(0, rec.savePropertiesCalls, "No save in replay mode");
	}

	@Test
	void setSpeedHighLevelWithAlwaysGhostKeepsGhost() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "alwaysghost", true);
		setBoolean(mode, "always20g", false);
		setInt(mode, "speedlevel", 150);
		setInt(mode, "gravityindex", 0);
		engine.ghost = true;

		invoke(mode, "setSpeed", new Class<?>[]{GameEngine.class}, engine);

		assertTrue(engine.ghost, "Ghost stays on with alwaysghost at level >= 100");
	}

	@Test
	void checkStageEndStageTimeoutWithTimerInactiveDoesNothing() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "clearflag", false);
		setInt(mode, "stagetimeNow", 0);
		setInt(mode, "stagetimeStart", 100);
		setInt(mode, "limittimeNow", 100);
		engine.timerActive = false;
		engine.stat = GameEngine.Status.MOVE;

		invoke(mode, "checkStageEnd", new Class<?>[]{GameEngine.class}, engine);

		assertEquals(GameEngine.Status.MOVE, engine.stat,
				"Stage timeout with inactive timer must not end the stage");
	}

	// ---------------------------------------------------------------
	// onSetting: edit screen 1
	// ---------------------------------------------------------------

	@Test
	void editScreen1ChangeAtCursor0DoesNothing() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		int startstage = readInt(mode, "startstage");
		int stageset = readInt(mode, "stageset");
		pressKey(engine, Controller.BUTTON_LEFT);

		mode.onSetting(engine, 0);

		assertEquals(startstage, readInt(mode, "startstage"));
		assertEquals(stageset, readInt(mode, "stageset"));
		assertEquals(1, readInt(mode, "editModeScreen"));
	}

	@Test
	void editScreen1PushAWithLowMenuTimeIgnored() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0);
		pressKey(engine, Controller.BUTTON_A);

		mode.onSetting(engine, 0);

		assertEquals(1, readInt(mode, "editModeScreen"),
				"Decide before 5 frames must be ignored");
	}

	@Test
	void editScreen1DecideAtCursor0OpensMapEditScreen() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_A);

		mode.onSetting(engine, 0);

		assertEquals(2, readInt(mode, "editModeScreen"),
				"Cursor 0 decide opens the map edit screen");
	}

	@Test
	void editScreen1LoadStageWithNullPropSkipsLoad() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 1);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_A);

		mode.onSetting(engine, 0);

		assertNull(readField(mode, "propStageSet"), "Nothing loaded without a stage set");
		assertEquals(1, readInt(mode, "editModeScreen"));
	}

	@Test
	void editScreen1LoadStageWithNullFieldSkipsLoad() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		invoke(mode, "loadStageSet", new Class<?>[]{int.class}, -1);
		engine.field = null;
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 1);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_A);

		mode.onSetting(engine, 0);

		assertNull(engine.field, "Load skipped when field is null");
	}

	@Test
	void editScreen1SaveStageWithNullPropSkipsSave() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		QuietReceiver rec = new QuietReceiver();
		GameEngine engine = freshEngine(mode, rec);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_A);

		mode.onSetting(engine, 0);

		assertEquals(0, rec.savePropertiesCalls);
	}

	@Test
	void editScreen1SaveStageWithNullFieldSkipsSave() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		QuietReceiver rec = new QuietReceiver();
		GameEngine engine = freshEngine(mode, rec);
		mode.playerInit(engine, 0);
		invoke(mode, "loadStageSet", new Class<?>[]{int.class}, -1);
		engine.field = null;
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_A);

		mode.onSetting(engine, 0);

		assertEquals(0, rec.savePropertiesCalls);
	}

	@Test
	void editScreen1PressDOnlyDoesNotExit() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_D);

		mode.onSetting(engine, 0);

		assertEquals(1, readInt(mode, "editModeScreen"),
				"D without E must not exit the edit screen");
	}

	// ---------------------------------------------------------------
	// onSetting: edit screen 2
	// ---------------------------------------------------------------

	@Test
	void editScreen2CursorUpNoWrap() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 2);
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_UP);

		mode.onSetting(engine, 0);

		assertEquals(2, readInt(mode, "menuCursor"), "UP from 3 goes to 2, no wrap");
	}

	@Test
	void editScreen2CursorDownNoWrap() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 2);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_DOWN);

		mode.onSetting(engine, 0);

		assertEquals(1, readInt(mode, "menuCursor"), "DOWN from 0 goes to 1, no wrap");
	}

	@Test
	void editScreen2ChangeAtCursor0DoesNothing() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 2);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		int stagetimeStart = readInt(mode, "stagetimeStart");
		pressKey(engine, Controller.BUTTON_LEFT);

		mode.onSetting(engine, 0);

		assertEquals(stagetimeStart, readInt(mode, "stagetimeStart"),
				"Change at cursor 0 does nothing on the map edit screen");
	}

	@Test
	void editScreen2PushAWithLowMenuTimeIgnored() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 2);
		setInt(mode, "menuCursor", 1);
		setInt(mode, "menuTime", 0);
		pressKey(engine, Controller.BUTTON_A);

		mode.onSetting(engine, 0);

		assertEquals(2, readInt(mode, "editModeScreen"));
	}

	@Test
	void editScreen2PushBWithLowMenuTimeIgnored() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "editModeScreen", 2);
		setInt(mode, "menuCursor", 1);
		setInt(mode, "menuTime", 0);
		pressKey(engine, Controller.BUTTON_B);

		mode.onSetting(engine, 0);

		assertEquals(2, readInt(mode, "editModeScreen"),
				"B before 5 frames must not leave the map edit screen");
	}

	// ---------------------------------------------------------------
	// onSetting: normal menu
	// ---------------------------------------------------------------

	@Test
	void normalMenuChangeAtCursor0ChangesStartStage() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_RIGHT);

		mode.onSetting(engine, 0);

		assertEquals(1, readInt(mode, "startstage"));
		assertNotNull(readField(mode, "propStageSet"), "Stage set loaded for preview");
	}

	@Test
	void normalMenuTogglesFromOppositeDefaults() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		// alwaysghost: true -> false
		setBoolean(mode, "alwaysghost", true);
		menuToggle(mode, engine, 2);
		assertFalse(readBoolean(mode, "alwaysghost"));

		// always20g: true -> false
		setBoolean(mode, "always20g", true);
		menuToggle(mode, engine, 3);
		assertFalse(readBoolean(mode, "always20g"));

		// lvstopse: false -> true
		setBoolean(mode, "lvstopse", false);
		menuToggle(mode, engine, 4);
		assertTrue(readBoolean(mode, "lvstopse"));

		// showsectiontime: true -> false
		setBoolean(mode, "showsectiontime", true);
		menuToggle(mode, engine, 5);
		assertFalse(readBoolean(mode, "showsectiontime"));

		// randomnext: true -> false
		setBoolean(mode, "randomnext", true);
		menuToggle(mode, engine, 6);
		assertFalse(readBoolean(mode, "randomnext"));
	}

	private void menuToggle(GemManiaMode mode, GameEngine engine, int cursor) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
	}

	@Test
	void normalMenuPushAWithLowMenuTimeIgnored() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0);
		pressKey(engine, Controller.BUTTON_A);

		boolean result = mode.onSetting(engine, 0);

		assertTrue(result, "Setting screen continues when A is pressed too early");
	}

	@Test
	void normalMenuPushDWithNonNegativeStagesetKeepsValue() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "stageset", 0);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_D);

		mode.onSetting(engine, 0);

		assertEquals(0, readInt(mode, "stageset"), "stageset >= 0 is kept");
		assertEquals(1, readInt(mode, "editModeScreen"));
	}

	// ---------------------------------------------------------------
	// onReady
	// ---------------------------------------------------------------

	@Test
	void onReadyWithNonzeroStatcSkipsInit() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 1;
		engine.readyDone = false;

		mode.onReady(engine, 0);

		assertNull(readField(mode, "propStageSet"),
				"Nothing loaded when statc[0] != 0");
	}

	@Test
	void onReadyWithReadyDoneSkipsFirstTimeInit() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// First (normal) onReady loads the stage set
		engine.statc[0] = 0;
		engine.readyDone = false;
		mode.onReady(engine, 0);

		// Second onReady with readyDone: keeps limit time, no next-list reset
		engine.readyDone = true;
		engine.statc[0] = 0;
		setInt(mode, "limittimeNow", 1234);
		mode.onReady(engine, 0);

		assertEquals(1234, readInt(mode, "limittimeNow"),
				"limittimeNow not reset when readyDone");
	}

	@Test
	void onReadyWithRandomNextSkipsFixedNextList() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "randomnext", true);
		engine.nextPieceArrayID = null;
		engine.statc[0] = 0;
		engine.readyDone = false;

		mode.onReady(engine, 0);

		assertNull(engine.nextPieceArrayID,
				"Random next mode must not install the fixed next list");
	}

	// ---------------------------------------------------------------
	// onLast
	// ---------------------------------------------------------------

	@Test
	void onLastSkipButtonWithTimerInactiveResetsPressTime() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = false;
		setInt(mode, "skipbuttonPressTime", 5);
		pressKey(engine, Controller.BUTTON_F);

		mode.onLast(engine, 0);

		assertEquals(0, readInt(mode, "skipbuttonPressTime"),
				"Press time resets when the timer is not active");
	}

	@Test
	void onLastSkipCombos() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "limittimeStart", 10000);
		setInt(mode, "stagetimeNow", 0);

		// a) press time still below threshold
		skipCase(mode, engine, 10, 0, 0, 4000, false);
		assertFalse(readBoolean(mode, "skipflag"), "Below 60 frames: no skip");

		// b) all conditions met on a normal stage: skip fires
		skipCase(mode, engine, 100, 0, 0, 4000, false);
		assertTrue(readBoolean(mode, "skipflag"), "Skip fires on normal stage");
		assertEquals(GameEngine.Status.CUSTOM, engine.stat);

		// c) last normal stage but training mode: skip fires
		skipCase(mode, engine, 100, 19, 1, 4000, false);
		assertTrue(readBoolean(mode, "skipflag"), "Skip fires in training on stage 20");

		// d) last normal stage, no training: no skip
		skipCase(mode, engine, 100, 19, 0, 4000, false);
		assertFalse(readBoolean(mode, "skipflag"), "No skip on stage 20 outside training");

		// e) not enough limit time left: no skip
		skipCase(mode, engine, 100, 0, 0, 1000, false);
		assertFalse(readBoolean(mode, "skipflag"), "No skip with 30s or less remaining");

		// f) stage already cleared: no skip
		skipCase(mode, engine, 100, 0, 0, 4000, true);
		assertFalse(readBoolean(mode, "skipflag"), "No skip after stage clear");
	}

	private void skipCase(GemManiaMode mode, GameEngine engine, int pressTime,
			int stage, int trainingType, int limittime, boolean clearflag) throws Exception {
		engine.gameActive = true;
		engine.timerActive = true;
		engine.stat = GameEngine.Status.MOVE;
		setBoolean(mode, "skipflag", false);
		setBoolean(mode, "clearflag", clearflag);
		setInt(mode, "skipbuttonPressTime", pressTime);
		setInt(mode, "stage", stage);
		setInt(mode, "trainingType", trainingType);
		setInt(mode, "limittimeNow", limittime);
		pressKey(engine, Controller.BUTTON_F);
		mode.onLast(engine, 0);
	}

	@Test
	void onLastLimitTimeMeterColorsAndCountdown() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "limittimeStart", 10000);
		setInt(mode, "stagetimeNow", 0);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ctrl.reset();

		setInt(mode, "limittimeNow", 4000);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);

		setInt(mode, "limittimeNow", 3001);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);

		setInt(mode, "limittimeNow", 1501);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);

		// 301 -> 300: red zone, countdown second boundary
		setInt(mode, "limittimeNow", 301);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);

		// 1 -> 0: countdown suppressed at zero
		setInt(mode, "limittimeNow", 1);
		mode.onLast(engine, 0);
		assertEquals(0, readInt(mode, "limittimeNow"));
	}

	@Test
	void onLastStageTimeCountdownBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "limittimeNow", 0);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ctrl.reset();

		// 1 -> 0: no countdown at zero
		setInt(mode, "stagetimeNow", 1);
		mode.onLast(engine, 0);
		assertEquals(0, readInt(mode, "stagetimeNow"));

		// 1000 -> 999: above the 10-second window
		setInt(mode, "stagetimeNow", 1000);
		mode.onLast(engine, 0);
		assertEquals(999, readInt(mode, "stagetimeNow"));

		// 61 -> 60: countdown boundary
		setInt(mode, "stagetimeNow", 61);
		mode.onLast(engine, 0);
		assertEquals(60, readInt(mode, "stagetimeNow"));
	}

	// ---------------------------------------------------------------
	// onMove / onARE
	// ---------------------------------------------------------------

	@Test
	void onMoveEndingNonzeroDoesNothing() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 1;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setInt(mode, "speedlevel", 5);
		setInt(mode, "nextseclv", 100);

		mode.onMove(engine, 0);

		assertEquals(5, readInt(mode, "speedlevel"), "No level up during ending");
	}

	@Test
	void onMoveHoldDisableSkipsLevelUp() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = true;
		setInt(mode, "speedlevel", 5);
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "lvupflag", false);

		mode.onMove(engine, 0);

		assertEquals(5, readInt(mode, "speedlevel"), "No level up while hold is disabled");
	}

	@Test
	void onMoveLvupflagTrueSkipsFirstBlock() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setInt(mode, "speedlevel", 5);
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "lvupflag", true);

		mode.onMove(engine, 0);

		assertEquals(5, readInt(mode, "speedlevel"),
				"lvupflag already set: no second level up");
	}

	@Test
	void onMoveAtLevelStopBoundaryNoIncrement() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		setInt(mode, "speedlevel", 99);
		setInt(mode, "nextseclv", 100);
		setInt(mode, "gravityindex", 0);

		mode.onMove(engine, 0);

		assertEquals(99, readInt(mode, "speedlevel"),
				"Level stops at nextseclv - 1");
	}

	@Test
	void onMoveLevelStopWithoutSe() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		setBoolean(mode, "lvstopse", false);
		setInt(mode, "speedlevel", 98);
		setInt(mode, "nextseclv", 100);
		setInt(mode, "gravityindex", 0);

		mode.onMove(engine, 0);

		assertEquals(99, readInt(mode, "speedlevel"),
				"Reaches level stop without playing the SE");
	}

	@Test
	void onMoveGimmickRollBigNotTriggered() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		setInt(mode, "nextseclv", 100);
		setInt(mode, "gimmickRoll", 5);
		setInt(mode, "gimmickBig", 7);
		setInt(mode, "thisStageTotalPieceLockCount", 1);

		mode.onMove(engine, 0);

		assertFalse(engine.itemRollRollEnable, "Roll Roll off between intervals");
		assertFalse(engine.big, "Big off between intervals");
	}

	@Test
	void onAreBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// a) ending: nothing happens
		engine.ending = 1;
		engine.statc[0] = 9;
		engine.statc[1] = 10;
		setBoolean(mode, "lvupflag", false);
		setInt(mode, "speedlevel", 5);
		setInt(mode, "nextseclv", 100);
		mode.onARE(engine, 0);
		assertEquals(5, readInt(mode, "speedlevel"));

		// b) not yet at the last ARE frame
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.statc[1] = 10;
		mode.onARE(engine, 0);
		assertEquals(5, readInt(mode, "speedlevel"));

		// c) lvupflag already set
		engine.statc[0] = 9;
		setBoolean(mode, "lvupflag", true);
		mode.onARE(engine, 0);
		assertEquals(5, readInt(mode, "speedlevel"));

		// d) fires at the level stop boundary: no increment
		setBoolean(mode, "lvupflag", false);
		setInt(mode, "speedlevel", 99);
		setInt(mode, "gravityindex", 0);
		mode.onARE(engine, 0);
		assertEquals(99, readInt(mode, "speedlevel"));
		assertTrue(readBoolean(mode, "lvupflag"));

		// e) reaches the boundary with the level stop SE disabled
		setBoolean(mode, "lvupflag", false);
		setBoolean(mode, "lvstopse", false);
		setInt(mode, "speedlevel", 98);
		setInt(mode, "gravityindex", 0);
		mode.onARE(engine, 0);
		assertEquals(99, readInt(mode, "speedlevel"));
	}

	// ---------------------------------------------------------------
	// calcScore / pieceLocked
	// ---------------------------------------------------------------

	@Test
	void calcScoreEndingNonzeroIgnoresLines() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		fillRow(engine, engine.field.getHeight() - 1, Block.BLOCK_COLOR_RED);
		engine.field.checkLine();
		engine.ending = 1;
		setInt(mode, "speedlevel", 0);

		mode.calcScore(engine, 0, 1);

		assertEquals(0, readInt(mode, "speedlevel"), "No level up during ending");
	}

	@Test
	void calcScoreGemClearLeavesRestPositive() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.createFieldIfNeeded();
		int row = engine.field.getHeight() - 1;
		fillRow(engine, row, Block.BLOCK_COLOR_RED);
		engine.field.setBlock(0, row, new Block(Block.BLOCK_COLOR_GEM_RED));
		engine.field.checkLine();
		setInt(mode, "rest", 5);
		setInt(mode, "limittimeNow", 100);
		setInt(mode, "nextseclv", 100);

		mode.calcScore(engine, 0, 1);

		assertEquals(4, readInt(mode, "rest"), "One gem cleared");
		assertFalse(readBoolean(mode, "clearflag"), "Gems remain: stage not cleared");
		assertEquals(160, readInt(mode, "limittimeNow"), "One second per gem");
	}

	@Test
	void calcScoreLevelStopWithoutSe() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.createFieldIfNeeded();
		fillRow(engine, engine.field.getHeight() - 1, Block.BLOCK_COLOR_RED);
		engine.field.checkLine();
		setBoolean(mode, "lvstopse", false);
		setInt(mode, "speedlevel", 98);
		setInt(mode, "nextseclv", 100);
		setInt(mode, "gravityindex", 0);

		mode.calcScore(engine, 0, 1);

		assertEquals(99, readInt(mode, "speedlevel"),
				"Level reaches stop point without the SE");
	}

	@Test
	void pieceLockedMirrorNotTriggeredBetweenIntervals() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gimmickMirror", 5);
		setInt(mode, "thisStageTotalPieceLockCount", 0);
		engine.interruptItemNumber = GameEngine.INTERRUPTITEM_NONE;

		mode.pieceLocked(engine, 0, 1);

		assertEquals(GameEngine.INTERRUPTITEM_NONE, engine.interruptItemNumber,
				"Mirror only fires on the interval");
		assertEquals(1, readInt(mode, "thisStageTotalPieceLockCount"));
	}

	// ---------------------------------------------------------------
	// onCustom
	// ---------------------------------------------------------------

	@Test
	void onCustomSlowClearNoTimeExtend() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setBoolean(mode, "clearflag", true);
		setInt(mode, "cleartime", 2000);
		setInt(mode, "stage", 0);

		mode.onCustom(engine, 0);

		assertEquals(0, readInt(mode, "timeextendStageClearSeconds"),
				"Clear slower than 20 seconds earns no bonus");
	}

	@Test
	void onCustomFinalStageInTrainingNoBgmFade() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setBoolean(mode, "clearflag", false);
		setBoolean(mode, "skipflag", false);
		setInt(mode, "stage", 19);
		setInt(mode, "trainingType", 1);
		engine.owner.bgmStatus.fadesw = false;

		mode.onCustom(engine, 0);

		assertFalse(engine.owner.bgmStatus.fadesw,
				"No BGM fade on the last stage in training mode");
	}

	@Test
	void onCustomTrainingBestTimeBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "trainingType", 1);
		setInt(mode, "stage", 0);
		setInt(mode, "limittimeStart", 10000);
		setInt(mode, "limittimeNow", 5000);

		// a) not cleared: best time untouched
		engine.statc[0] = 0;
		setBoolean(mode, "clearflag", false);
		setBoolean(mode, "skipflag", true);
		setInt(mode, "trainingBestTime", 500);
		mode.onCustom(engine, 0);
		assertEquals(500, readInt(mode, "trainingBestTime"));

		// b) cleared but slower than the best: untouched
		engine.statc[0] = 0;
		setBoolean(mode, "clearflag", true);
		setBoolean(mode, "skipflag", false);
		setInt(mode, "trainingBestTime", 100);
		setInt(mode, "cleartime", 200);
		mode.onCustom(engine, 0);
		assertEquals(100, readInt(mode, "trainingBestTime"));

		// c) cleared faster than the best: updated
		engine.statc[0] = 0;
		setInt(mode, "cleartime", 50);
		mode.onCustom(engine, 0);
		assertEquals(50, readInt(mode, "trainingBestTime"));

		// d) no best time yet: recorded
		engine.statc[0] = 0;
		setInt(mode, "trainingBestTime", -1);
		setInt(mode, "cleartime", 300);
		mode.onCustom(engine, 0);
		assertEquals(300, readInt(mode, "trainingBestTime"));
	}

	@Test
	void onCustomMeterColorsDuringTimeExtend() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "limittimeStart", 10000);
		setInt(mode, "timeextendStageClearSeconds", 10);
		setBoolean(mode, "skipflag", false);

		meterCase(mode, engine, 8000);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);

		meterCase(mode, engine, 3000);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);

		meterCase(mode, engine, 1500);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);

		meterCase(mode, engine, 200);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	private void meterCase(GemManiaMode mode, GameEngine engine, int limittime) throws Exception {
		engine.statc[0] = 1;
		engine.statc[1] = 0;
		engine.ctrl.reset();
		setInt(mode, "limittimeNow", limittime);
		setInt(mode, "timeextendStageClearSeconds", 10);
		mode.onCustom(engine, 0);
	}

	@Test
	void onCustomTrainingTransitions() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "limittimeStart", 10000);

		// a) training clear: time bonus added, back to READY
		engine.statc[0] = 300;
		engine.statc[1] = 0;
		setInt(mode, "trainingType", 1);
		setBoolean(mode, "clearflag", true);
		setBoolean(mode, "skipflag", false);
		setInt(mode, "timeextendStageClearSeconds", 10);
		setInt(mode, "limittimeNow", 1000);
		mode.onCustom(engine, 0);
		assertEquals(1600, readInt(mode, "limittimeNow"), "Clear bonus added");
		assertEquals(GameEngine.Status.READY, engine.stat);

		// b) training skip: penalty subtracted
		engine.statc[0] = 300;
		engine.statc[1] = 0;
		setBoolean(mode, "clearflag", false);
		setBoolean(mode, "skipflag", true);
		setInt(mode, "timeextendStageClearSeconds", 30);
		setInt(mode, "limittimeNow", 5000);
		mode.onCustom(engine, 0);
		assertEquals(3200, readInt(mode, "limittimeNow"), "Skip penalty subtracted");

		// c) training type 2: next piece counter restored
		engine.statc[0] = 300;
		engine.statc[1] = 0;
		setInt(mode, "trainingType", 2);
		setBoolean(mode, "skipflag", false);
		setInt(mode, "timeextendStageClearSeconds", 0);
		setInt(mode, "continueNextPieceCount", 42);
		engine.nextPieceCount = 0;
		mode.onCustom(engine, 0);
		assertEquals(42, engine.nextPieceCount, "Piece counter reset in ON+RESET mode");
	}

	@Test
	void onCustomEndingAllClearValues() throws Exception {
		// a) final EX stage: perfect all clear
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 300;
		engine.statc[1] = 0;
		setInt(mode, "trainingType", 0);
		setInt(mode, "stage", 26);
		setInt(mode, "laststage", 26);
		setInt(mode, "timeextendStageClearSeconds", 0);
		mode.onCustom(engine, 0);
		assertEquals(2, readInt(mode, "allclear"), "Stage 27 ends with all clear 2");
		assertEquals(1, engine.ending);
		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);

		// b) normal last stage: all clear 1
		GemManiaMode mode2 = new GemManiaMode();
		GameEngine engine2 = freshEngine(mode2);
		mode2.playerInit(engine2, 0);
		engine2.statc[0] = 300;
		engine2.statc[1] = 0;
		setInt(mode2, "trainingType", 0);
		setInt(mode2, "stage", 19);
		setInt(mode2, "laststage", 19);
		setInt(mode2, "timeextendStageClearSeconds", 0);
		mode2.onCustom(engine2, 0);
		assertEquals(1, readInt(mode2, "allclear"), "Stage 20 ends with all clear 1");
	}

	@Test
	void onCustomNextStageTimeAdjust() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "trainingType", 0);
		setInt(mode, "laststage", 19);
		setInt(mode, "limittimeStart", 10000);

		// a) cleared: bonus added, advance
		engine.statc[0] = 300;
		engine.statc[1] = 0;
		setInt(mode, "stage", 0);
		setBoolean(mode, "clearflag", true);
		setBoolean(mode, "skipflag", false);
		setInt(mode, "timeextendStageClearSeconds", 10);
		setInt(mode, "limittimeNow", 1000);
		mode.onCustom(engine, 0);
		assertEquals(1, readInt(mode, "stage"));
		assertEquals(1600, readInt(mode, "limittimeNow"));
		assertEquals(GameEngine.Status.READY, engine.stat);

		// b) skipped: penalty subtracted
		engine.statc[0] = 300;
		engine.statc[1] = 0;
		setInt(mode, "stage", 0);
		setBoolean(mode, "clearflag", false);
		setBoolean(mode, "skipflag", true);
		setInt(mode, "timeextendStageClearSeconds", 30);
		setInt(mode, "limittimeNow", 5000);
		mode.onCustom(engine, 0);
		assertEquals(3200, readInt(mode, "limittimeNow"));

		// c) timed out: unchanged
		engine.statc[0] = 300;
		engine.statc[1] = 0;
		setInt(mode, "stage", 0);
		setBoolean(mode, "clearflag", false);
		setBoolean(mode, "skipflag", false);
		setInt(mode, "limittimeNow", 5000);
		mode.onCustom(engine, 0);
		assertEquals(5000, readInt(mode, "limittimeNow"));
	}

	// ---------------------------------------------------------------
	// onGameOver / onResult
	// ---------------------------------------------------------------

	@Test
	void onGameOverEndingNonzeroReturnsFalse() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 1;

		assertFalse(mode.onGameOver(engine, 0),
				"No continue screen after the ending");
	}

	@Test
	void onGameOverGrayRowBeyondFieldHandlesNullBlocks() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		setBoolean(mode, "noContinue", false);
		engine.ctrl.reset();
		// Row index == field height: getBlockColor reports INVALID but getBlock is null
		engine.statc[0] = engine.field.getHeight();

		assertTrue(mode.onGameOver(engine, 0));

		assertEquals(engine.field.getHeight() + 1, engine.statc[0],
				"Graying advances past the out-of-range row without crashing");
	}

	@Test
	void onGameOverContinueCursorUp() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		setBoolean(mode, "noContinue", false);
		engine.statc[0] = engine.field.getHeight() + 1;
		engine.statc[1] = 0;
		pressKey(engine, Controller.BUTTON_UP);

		mode.onGameOver(engine, 0);

		assertEquals(1, engine.statc[1], "UP moves the continue cursor");
	}

	@Test
	void onGameOverContinueYesInTraining() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		setBoolean(mode, "noContinue", false);
		setInt(mode, "trainingType", 1);
		setInt(mode, "limittimeStart", 5000);
		setInt(mode, "continueNextPieceCount", 7);
		engine.statistics.time = 999;
		engine.statc[0] = engine.field.getHeight() + 1;
		engine.statc[1] = 0;
		pressKey(engine, Controller.BUTTON_A);

		mode.onGameOver(engine, 0);

		assertEquals(5000, readInt(mode, "limittimeNow"), "Limit time restored");
		assertEquals(7, engine.nextPieceCount);
		assertEquals(999, engine.statistics.time,
				"No 2-minute penalty in training mode");
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	@Test
	void onResultUpNoWrap() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 2;
		pressKey(engine, Controller.BUTTON_UP);

		mode.onResult(engine, 0);

		assertEquals(1, engine.statc[1], "UP from page 3 goes to page 2");
	}

	@Test
	void onResultDownNoWrap() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;
		pressKey(engine, Controller.BUTTON_DOWN);

		mode.onResult(engine, 0);

		assertEquals(1, engine.statc[1], "DOWN from page 1 goes to page 2");
	}

	// ---------------------------------------------------------------
	// saveReplay / checkRanking
	// ---------------------------------------------------------------

	@Test
	void saveReplayRankingGateEachConditionFalse() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		QuietReceiver rec = new QuietReceiver();
		GameEngine engine = freshEngine(mode, rec);
		mode.playerInit(engine, 0);
		zeroRankings(mode);

		// 1) replay mode
		engine.owner.replayMode = true;
		assertNoRankingUpdate(mode, engine);
		engine.owner.replayMode = false;

		// 2) non-default start stage
		setInt(mode, "startstage", 1);
		assertNoRankingUpdate(mode, engine);
		setInt(mode, "startstage", 0);

		// 3) training mode
		setInt(mode, "trainingType", 1);
		assertNoRankingUpdate(mode, engine);
		setInt(mode, "trainingType", 0);

		// 4) non-default next counter
		setInt(mode, "startnextc", 5);
		assertNoRankingUpdate(mode, engine);
		setInt(mode, "startnextc", 0);

		// 5) custom stage set
		setInt(mode, "stageset", 0);
		assertNoRankingUpdate(mode, engine);
		setInt(mode, "stageset", -1);

		// 6) 20G mode
		setBoolean(mode, "always20g", true);
		assertNoRankingUpdate(mode, engine);
		setBoolean(mode, "always20g", false);

		// 7) AI in control
		engine.ai = new DummyAI();
		assertNoRankingUpdate(mode, engine);
		engine.ai = null;
	}

	private void assertNoRankingUpdate(GemManiaMode mode, GameEngine engine) throws Exception {
		setInt(mode, "rankingRank", -1);
		mode.saveReplay(engine, 0, new CustomProperties());
		assertEquals(-1, readInt(mode, "rankingRank"),
				"Ranking must not update when a gate condition fails");
	}

	@Test
	void saveReplayRandomNextUpdatesTypeOneRanking() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		QuietReceiver rec = new QuietReceiver();
		GameEngine engine = freshEngine(mode, rec);
		mode.playerInit(engine, 0);
		zeroRankings(mode);
		engine.owner.replayMode = false;
		setBoolean(mode, "randomnext", true);
		setInt(mode, "stage", 5);
		setInt(mode, "clearper", 60);
		setInt(mode, "allclear", 1);
		engine.statistics.time = 100;
		setInt(mode, "rankingRank", -1);

		mode.saveReplay(engine, 0, new CustomProperties());

		assertEquals(0, readInt(mode, "rankingRank"), "New best goes to rank 1");
		int[][] rankingStage = (int[][]) readField(mode, "rankingStage");
		assertEquals(5, rankingStage[1][0], "Random-next result stored under type 1");
	}

	@Test
	void checkRankingStageMismatchBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		zeroRankings(mode);
		fillRanking(mode, 1, 5, 50, 100);

		// clear ties, stage lower: no disjunct matches
		invoke(mode, "updateRanking",
				new Class<?>[]{int.class, int.class, int.class, int.class, int.class},
				0, 3, 50, 100, 1);

		assertEquals(-1, readInt(mode, "rankingRank"),
				"Lower stage with equal clear flag does not rank");
	}

	@Test
	void checkRankingClearPerMismatchBranch() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		zeroRankings(mode);
		fillRanking(mode, 1, 5, 50, 100);

		// clear and stage tie, clear rate lower: no disjunct matches
		invoke(mode, "updateRanking",
				new Class<?>[]{int.class, int.class, int.class, int.class, int.class},
				0, 5, 30, 100, 1);

		assertEquals(-1, readInt(mode, "rankingRank"),
				"Lower clear rate with equal stage does not rank");
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static class QuietReceiver extends EventReceiver {
		int savePropertiesCalls = 0;

		@Override
		public boolean saveProperties(String filename, CustomProperties prop) {
			savePropertiesCalls++;
			return true;
		}

		@Override
		public void saveModeConfig(CustomProperties modeConfig) {
			// Do not touch shared config on disk
		}
	}

	private static GameEngine freshEngine(GemManiaMode mode) {
		return freshEngine(mode, new QuietReceiver());
	}

	private static GameEngine freshEngine(GemManiaMode mode, QuietReceiver rec) {
		GameManager manager = new GameManager(rec);
		manager.replayMode = false;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void fillRow(GameEngine engine, int row, int color) {
		for (int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlock(x, row, new Block(color));
		}
	}

	private static void zeroRankings(GemManiaMode mode) throws Exception {
		findField(mode.getClass(), "rankingStage").set(mode, new int[2][10]);
		findField(mode.getClass(), "rankingClearPer").set(mode, new int[2][10]);
		findField(mode.getClass(), "rankingTime").set(mode, new int[2][10]);
		findField(mode.getClass(), "rankingAllClear").set(mode, new int[2][10]);
	}

	private static void fillRanking(GemManiaMode mode, int allclear, int stage,
			int clearper, int time) throws Exception {
		int[][] rAll = (int[][]) readField(mode, "rankingAllClear");
		int[][] rStage = (int[][]) readField(mode, "rankingStage");
		int[][] rPer = (int[][]) readField(mode, "rankingClearPer");
		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		for (int i = 0; i < rAll[0].length; i++) {
			rAll[0][i] = allclear;
			rStage[0][i] = stage;
			rPer[0][i] = clearper;
			rTime[0][i] = time;
		}
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.reset();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static Object invoke(GemManiaMode mode, String name, Class<?>[] types,
			Object... args) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod(name, types);
		m.setAccessible(true);
		return m.invoke(mode, args);
	}

	private static int readInt(GemManiaMode mode, String name) throws Exception {
		return findField(mode.getClass(), name).getInt(mode);
	}

	private static boolean readBoolean(GemManiaMode mode, String name) throws Exception {
		return findField(mode.getClass(), name).getBoolean(mode);
	}

	private static Object readField(GemManiaMode mode, String name) throws Exception {
		return findField(mode.getClass(), name).get(mode);
	}

	private static void setInt(GemManiaMode mode, String name, int value) throws Exception {
		findField(mode.getClass(), name).setInt(mode, value);
	}

	private static void setBoolean(GemManiaMode mode, String name, boolean value) throws Exception {
		findField(mode.getClass(), name).setBoolean(mode, value);
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
