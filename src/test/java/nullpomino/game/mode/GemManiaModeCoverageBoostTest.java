package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import org.junit.jupiter.api.Test;

/**
 * Coverage-boost tests for {@link GemManiaMode} aimed at the lines not exercised
 * by the existing GemManiaMode suites. Targets:
 * <ul>
 *   <li>playerInit replay-mode + version&lt;=0 branches (303-311)</li>
 *   <li>startStage background fade (331-333) and saveStageSet (383-392)</li>
 *   <li>onSetting edit-screen 1 and 2 menus (531-668)</li>
 *   <li>renderReady EX/training, renderLast gimmick + section-time
 *       (855, 860-861, 921-923, 952-998)</li>
 *   <li>onLast countdown SEs (1048, 1058)</li>
 *   <li>onMove lvupflag reset / X-Ray / Color else branches (1078, 1092-1103)</li>
 *   <li>calcScore gem clears + levelstop (1141-1145, 1171)</li>
 *   <li>pieceLocked mirror (1196)</li>
 *   <li>onCustom training/ending/next-stage paths (1225, 1234-1322)</li>
 *   <li>renderCustom, onGameOver, renderGameOver, onResult, renderResult
 *       (1338-1551)</li>
 *   <li>saveReplay ranking-update path (1580-1581)</li>
 * </ul>
 */
class GemManiaModeCoverageBoostTest {

	/**
	 * EventReceiver that redirects property writes to the system temp dir so the
	 * mode's saveMap/saveStageSet lines still execute (and stay covered) without
	 * clobbering the repository's tracked {@code config/map/gemmania/*.map} files.
	 */
	private static class TempWriteReceiver extends EventReceiver {
		@Override
		public boolean saveProperties(String filename, CustomProperties prop) {
			String redirected = System.getProperty("java.io.tmpdir")
					+ "/gemmania-coverage-test-"
					+ filename.replaceAll("[/\\\\]", "_");
			return super.saveProperties(redirected, prop);
		}
	}

	/** TempWriteReceiver whose getNextDisplayType() returns 2 (BSP layout). */
	private static final class BspReceiver extends TempWriteReceiver {
		@Override
		public int getNextDisplayType() {
			return 2;
		}
	}

	// ---------------------------------------------------------------
	// playerInit: replay-mode + version<=0 branches (303-311)
	// ---------------------------------------------------------------

	@Test
	void playerInitReplayModeReadsVersionAndSetsReadyTimings() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, true);
		engine.owner.replayMode = true;
		// version key absent -> defaults to 0 -> version<=0 branch runs.

		mode.playerInit(engine, 0);

		assertEquals(0, readFieldInt(mode, "version"),
				"replay version defaults to 0 when key absent");
		assertEquals(45, engine.readyStart, "version<=0 sets readyStart");
		assertEquals(155, engine.readyEnd);
		assertEquals(160, engine.goStart);
		assertEquals(225, engine.goEnd);
	}

	// ---------------------------------------------------------------
	// startStage background fade (331-333)
	// ---------------------------------------------------------------

	@Test
	void onReadyTriggersBackgroundFadeWhenBgNonZero() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.owner.backgroundStatus.bg = 3; // non-zero -> fade branch
		engine.owner.backgroundStatus.fadesw = false;
		engine.statc[0] = 0;
		engine.readyDone = false;

		mode.onReady(engine, 0); // calls startStage internally

		assertTrue(engine.owner.backgroundStatus.fadesw,
				"non-zero background should start a fade");
		assertEquals(0, engine.owner.backgroundStatus.fadecount);
		assertEquals(0, engine.owner.backgroundStatus.fadebg);
	}

	// ---------------------------------------------------------------
	// saveStageSet (383-392)
	// ---------------------------------------------------------------

	@Test
	void saveStageSetCustomAndDefault() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		// Establish a non-null propStageSet first.
		invokeLoadStageSet(mode, 0);
		assertNotNull(readField(mode, "propStageSet"));

		// custom branch (id >= 0)
		invokeSaveStageSet(mode, 5);
		// default branch (id < 0)
		invokeSaveStageSet(mode, -1);
		// no assertions needed; the no-op receiver swallows the I/O.
	}

	// ---------------------------------------------------------------
	// onSetting edit-screen 1 (531-589)
	// ---------------------------------------------------------------

	@Test
	void onSettingEditScreen1ChangeAndSelectAndCancel() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "editModeScreen", 1);
		engine.createFieldIfNeeded();
		invokeLoadStageSet(mode, 0); // non-null propStageSet

		// cursor 1: change startstage (LEFT decrements -> wrap to MAX-1)
		setFieldInt(mode, "menuCursor", 1);
		setFieldInt(mode, "startstage", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(26, readFieldInt(mode, "startstage"),
				"startstage LEFT-wrap to MAX_STAGE_TOTAL-1");

		// cursor 3: change stageset (LEFT decrements -> wrap to 99)
		setFieldInt(mode, "menuCursor", 3);
		setFieldInt(mode, "stageset", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, readFieldInt(mode, "stageset"),
				"stageset LEFT-wrap to 99");

		// cursor 0 + A -> editModeScreen = 2
		setFieldInt(mode, "menuCursor", 0);
		setFieldInt(mode, "menuTime", 10);
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(2, readFieldInt(mode, "editModeScreen"),
				"A at cursor 0 enters map-edit screen 2");
	}

	@Test
	void onSettingEditScreen1LoadAndSaveMapAndStageSet() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		invokeLoadStageSet(mode, 0);

		// cursor 1 + A -> loadMap
		setFieldInt(mode, "editModeScreen", 1);
		setFieldInt(mode, "menuCursor", 1);
		setFieldInt(mode, "menuTime", 10);
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// cursor 2 + A -> saveMap
		setFieldInt(mode, "editModeScreen", 1);
		setFieldInt(mode, "menuCursor", 2);
		setFieldInt(mode, "menuTime", 10);
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// cursor 3 + A -> loadStageSet
		setFieldInt(mode, "editModeScreen", 1);
		setFieldInt(mode, "menuCursor", 3);
		setFieldInt(mode, "menuTime", 10);
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// cursor 4 + A -> saveStageSet
		setFieldInt(mode, "editModeScreen", 1);
		setFieldInt(mode, "menuCursor", 4);
		setFieldInt(mode, "menuTime", 10);
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(1, readFieldInt(mode, "editModeScreen"),
				"map load/save actions stay on screen 1");
	}

	@Test
	void onSettingEditScreen1CancelWithDAndE() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "editModeScreen", 1);
		setFieldInt(mode, "menuCursor", 2);

		pressTwo(engine, Controller.BUTTON_D, Controller.BUTTON_E);
		mode.onSetting(engine, 0);

		assertEquals(0, readFieldInt(mode, "editModeScreen"),
				"D+E cancels back to normal menu");
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	// ---------------------------------------------------------------
	// onSetting edit-screen 2 (594-667)
	// ---------------------------------------------------------------

	@Test
	void onSettingEditScreen2UpDownNavigation() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "editModeScreen", 2);

		setFieldInt(mode, "menuCursor", 0);
		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(4, readFieldInt(mode, "menuCursor"), "UP at 0 wraps to 4");

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"), "DOWN at 4 wraps to 0");
	}

	@Test
	void onSettingEditScreen2ChangesEachField() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "editModeScreen", 2);

		// cursor 1: stagetimeStart (RIGHT increments by 60)
		setFieldInt(mode, "menuCursor", 1);
		setFieldInt(mode, "stagetimeStart", 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(60, readFieldInt(mode, "stagetimeStart"));

		// cursor 2: limittimeStart (LEFT wraps to 3600*20)
		setFieldInt(mode, "editModeScreen", 2);
		setFieldInt(mode, "menuCursor", 2);
		setFieldInt(mode, "limittimeStart", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(3600 * 20, readFieldInt(mode, "limittimeStart"),
				"limittimeStart LEFT-wraps to 3600*20");

		// cursor 3: stagebgm (LEFT wraps to BGM_COUNT-1)
		setFieldInt(mode, "editModeScreen", 2);
		setFieldInt(mode, "menuCursor", 3);
		setFieldInt(mode, "stagebgm", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(BGMStatus.BGM_COUNT - 1, readFieldInt(mode, "stagebgm"));

		// cursor 4: gimmickMirror (RIGHT increments)
		setFieldInt(mode, "editModeScreen", 2);
		setFieldInt(mode, "menuCursor", 4);
		setFieldInt(mode, "gimmickMirror", 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "gimmickMirror"));
	}

	@Test
	void onSettingEditScreen2ChangeWithEMultiplier() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "editModeScreen", 2);
		setFieldInt(mode, "menuCursor", 1);
		setFieldInt(mode, "stagetimeStart", 0);

		// RIGHT with E held -> multiplier 100 -> +60*100 = 6000
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) engine.ctrl.buttonTime[i] = 0;
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1;
		mode.onSetting(engine, 0);

		assertEquals(6000, readFieldInt(mode, "stagetimeStart"),
				"E multiplier applies 100x to the 60-frame step");
	}

	@Test
	void onSettingEditScreen2SelectEntersFieldEditAndBackToScreen1() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		// cursor 0 + A -> enterFieldEdit, returns true
		setFieldInt(mode, "editModeScreen", 2);
		setFieldInt(mode, "menuCursor", 0);
		setFieldInt(mode, "menuTime", 10);
		pressPush(engine, Controller.BUTTON_A);
		boolean ret = mode.onSetting(engine, 0);
		assertTrue(ret, "A at cursor 0 enters field edit and returns true");
		assertEquals(GameEngine.Status.FIELDEDIT, engine.stat);

		// cursor != 0 + A -> back to screen 1
		setFieldInt(mode, "editModeScreen", 2);
		setFieldInt(mode, "menuCursor", 2);
		setFieldInt(mode, "menuTime", 10);
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "editModeScreen"),
				"A at non-zero cursor returns to screen 1");
	}

	@Test
	void onSettingEditScreen2CancelWithB() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "editModeScreen", 2);
		setFieldInt(mode, "menuCursor", 3);
		setFieldInt(mode, "menuTime", 10);

		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertEquals(1, readFieldInt(mode, "editModeScreen"),
				"B cancels screen 2 back to screen 1");
	}

	// ---------------------------------------------------------------
	// renderReady EX-stage / training (855, 860-861)
	// ---------------------------------------------------------------

	@Test
	void renderReadyExStageAndTraining() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.statc[0] = engine.readyStart; // past readyStart threshold
		setFieldInt(mode, "trainingType", 1); // training banner
		setFieldInt(mode, "stage", 21);       // >= MAX_STAGE_NORMAL -> EX branch

		mode.renderReady(engine, 0);
	}

	@Test
	void renderReadyNormalStage() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.statc[0] = engine.readyStart;
		setFieldInt(mode, "trainingType", 0);
		setFieldInt(mode, "stage", 3); // normal-stage branch

		mode.renderReady(engine, 0);
	}

	// ---------------------------------------------------------------
	// renderLast gimmick X-Ray/Color + section time (921-923, 952-998)
	// ---------------------------------------------------------------

	@Test
	void renderLastGimmickXRay() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 0;
		setFieldInt(mode, "gimmickMirror", 0);
		setFieldInt(mode, "gimmickRoll", 0);
		setFieldInt(mode, "gimmickBig", 0);
		setFieldInt(mode, "gimmickXRay", 5);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastGimmickColor() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 0;
		setFieldInt(mode, "gimmickMirror", 0);
		setFieldInt(mode, "gimmickRoll", 0);
		setFieldInt(mode, "gimmickBig", 0);
		setFieldInt(mode, "gimmickXRay", 0);
		setFieldInt(mode, "gimmickColor", 5);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWithStageTimeLimitTimeAndSectionTime() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ending = 0;
		engine.createFieldIfNeeded();

		// stage time + limit time display (with flashing low-time branches)
		setFieldInt(mode, "stagetimeStart", 3600);
		setFieldInt(mode, "stagetimeNow", 4);   // < 600 && %4==0 -> flash
		setFieldInt(mode, "limittimeStart", 3600);
		setFieldInt(mode, "limittimeNow", 8);   // < 600 && %4==0 -> flash
		setFieldInt(mode, "timeextendDisp", 30); // adds (+N SEC.) line
		setFieldInt(mode, "timeextendSeconds", 5);

		// section time display
		setFieldBool(mode, "showsectiontime", true);
		setFieldInt(mode, "stage", 2);
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 120;  // normal time
		sectiontime[1] = -1;   // FAILED
		sectiontime[2] = -2;   // SKIPPED (and i==stage -> 'b' separator)
		sectiontime[3] = 300;

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastSectionTimeBspLayout() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false, new BspReceiver());
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 0;

		setFieldBool(mode, "showsectiontime", true);
		setFieldInt(mode, "stage", 16); // exercises pos = i - max(stage-14,0)
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		for (int i = 0; i < sectiontime.length; i++) sectiontime[i] = (i + 1) * 60;

		mode.renderLast(engine, 0); // BSP (getNextDisplayType()==2) branch
	}

	@Test
	void renderLastRankingScreenWithRankHighlight() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.SETTING;
		// default-ish settings so the ranking table renders
		setFieldInt(mode, "startstage", 0);
		setFieldBool(mode, "always20g", false);
		setFieldInt(mode, "trainingType", 0);
		setFieldInt(mode, "startnextc", 0);
		setFieldInt(mode, "stageset", -1);
		setFieldInt(mode, "rankingRank", 0);
		int[][] allclear = (int[][]) readField(mode, "rankingAllClear");
		allclear[0][0] = 1; // GREEN
		allclear[0][1] = 2; // ORANGE

		mode.renderLast(engine, 0);
	}

	// ---------------------------------------------------------------
	// onLast countdown SEs (1048, 1058)
	// ---------------------------------------------------------------

	@Test
	void onLastLimitTimeCountdownSE() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;
		setFieldInt(mode, "limittimeStart", 3600);
		// after decrement limittimeNow==600 -> <=10*60 && %60==0 -> countdown
		setFieldInt(mode, "limittimeNow", 601);
		setFieldInt(mode, "stagetimeNow", 0);

		mode.onLast(engine, 0);

		assertEquals(600, readFieldInt(mode, "limittimeNow"));
	}

	@Test
	void onLastStageTimeCountdownSE() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;
		setFieldInt(mode, "limittimeNow", 0);
		// after decrement stagetimeNow==600 -> <=10*60 && %60==0 -> countdown
		setFieldInt(mode, "stagetimeNow", 601);

		mode.onLast(engine, 0);

		assertEquals(600, readFieldInt(mode, "stagetimeNow"));
	}

	// ---------------------------------------------------------------
	// onMove lvupflag reset / X-Ray / Color else branches (1078, 1092-1103)
	// ---------------------------------------------------------------

	@Test
	void onMoveResetsLvupflagWhenStatcPositive() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 1; // > 0 -> lvupflag reset path
		setFieldBool(mode, "lvupflag", true);

		mode.onMove(engine, 0);

		assertFalse(readFieldBool(mode, "lvupflag"),
				"statc[0]>0 should reset lvupflag");
	}

	@Test
	void onMoveXRayAndColorElseBranches() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setFieldBool(mode, "lvupflag", false);
		setFieldInt(mode, "nextseclv", 100);
		setFieldInt(mode, "speedlevel", 0);
		setFieldInt(mode, "gimmickXRay", 3);
		setFieldInt(mode, "gimmickColor", 3);
		// lock count % gimmick != 0 -> else branches (disable + resetFieldVisible)
		setFieldInt(mode, "thisStageTotalPieceLockCount", 1);
		engine.itemXRayEnable = true;
		engine.itemColorEnable = true;

		mode.onMove(engine, 0);

		assertFalse(engine.itemXRayEnable, "X-Ray disabled on off-beat piece");
		assertFalse(engine.itemColorEnable, "Color disabled on off-beat piece");
	}

	// ---------------------------------------------------------------
	// calcScore gem clears + levelstop (1141-1145, 1171)
	// ---------------------------------------------------------------

	@Test
	void calcScoreClearsGemsAndExtendsTime() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;

		int row = engine.field.getHeight() - 1;
		// Fill the row and place gem blocks on it, then flag the line.
		for (int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlock(x, row, new Block(Block.BLOCK_COLOR_GEM_RED));
		}
		engine.field.setLineFlag(row, true);
		setFieldInt(mode, "rest", 5);
		setFieldInt(mode, "limittimeNow", 100);
		setFieldInt(mode, "speedlevel", 0);

		mode.calcScore(engine, 0, 1);

		int gems = engine.field.getWidth();
		assertEquals(5 - gems, readFieldInt(mode, "rest"),
				"rest should decrease by the number of gem clears");
		assertTrue(readFieldBool(mode, "clearflag"),
				"rest<=0 should set clearflag");
		assertEquals(100 + 60 * gems, readFieldInt(mode, "limittimeNow"),
				"limit time extends by 60 frames per gem");
		assertEquals(gems, readFieldInt(mode, "timeextendSeconds"));
		assertEquals(120, readFieldInt(mode, "timeextendDisp"));
	}

	@Test
	void calcScoreLevelStopSoundAtNextSecMinusOne() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		engine.field.setLineFlag(engine.field.getHeight() - 1, true);

		// speedlevel 98 + 1 line -> 99 == nextseclv(100)-1 -> levelstop branch
		setFieldInt(mode, "speedlevel", 98);
		setFieldInt(mode, "nextseclv", 100);
		setFieldBool(mode, "lvstopse", true);

		mode.calcScore(engine, 0, 1);

		assertEquals(99, readFieldInt(mode, "speedlevel"));
	}

	@Test
	void calcScoreNextSectionAdvance() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		engine.field.setLineFlag(engine.field.getHeight() - 1, true);

		// speedlevel 99 + 4 lines (+6) -> 105 >= nextseclv(100) -> section advance
		setFieldInt(mode, "speedlevel", 99);
		setFieldInt(mode, "nextseclv", 100);

		mode.calcScore(engine, 0, 4);

		assertEquals(200, readFieldInt(mode, "nextseclv"),
				"crossing the section bumps nextseclv by 100");
		assertTrue(engine.owner.backgroundStatus.fadesw,
				"section advance triggers background fade");
	}

	// ---------------------------------------------------------------
	// pieceLocked mirror (1196)
	// ---------------------------------------------------------------

	@Test
	void pieceLockedMirrorInterrupt() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "gimmickMirror", 1); // every piece triggers mirror
		setFieldInt(mode, "thisStageTotalPieceLockCount", 0);

		mode.pieceLocked(engine, 0, 1);

		assertEquals(GameEngine.INTERRUPTITEM_MIRROR, engine.interruptItemNumber,
				"mirror gimmick should queue the mirror interrupt item");
		assertEquals(1, readFieldInt(mode, "thisStageTotalPieceLockCount"));
	}

	// ---------------------------------------------------------------
	// onCustom: training / ending / next-stage paths (1225, 1234-1322)
	// ---------------------------------------------------------------

	@Test
	void onCustomFinalNormalStageSetsLaststageByClearRate() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		setFieldBool(mode, "clearflag", true);
		setFieldInt(mode, "stage", 19);   // MAX_STAGE_NORMAL - 1 (final normal)
		setFieldInt(mode, "cleartime", 5 * 60); // < 10*60 -> +10 sec bonus
		setFieldInt(mode, "clearstage", 8);
		setFieldInt(mode, "trystage", 9);
		setFieldInt(mode, "trainingType", 0);
		engine.statistics.time = 0;

		mode.onCustom(engine, 0);

		// clearper = 9*100/10 = 90 -> 90..99 -> laststage = 22
		assertEquals(22, readFieldInt(mode, "laststage"),
				"clear rate 90 maps to EX3 (laststage 22)");
	}

	@Test
	void onCustomTrainingModeReturnsToReady() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		setFieldBool(mode, "clearflag", true);
		setFieldInt(mode, "trainingType", 2); // resets nextPieceCount path too
		setFieldInt(mode, "stage", 0);
		setFieldInt(mode, "limittimeNow", 100);
		setFieldInt(mode, "timeextendStageClearSeconds", 0);
		setFieldInt(mode, "continueNextPieceCount", 7);

		// Force the "advance to next screen" branch via A press.
		pressPush(engine, Controller.BUTTON_A);
		boolean ret = mode.onCustom(engine, 0);

		assertTrue(ret);
		assertEquals(GameEngine.Status.READY, engine.stat,
				"training advances back to READY");
		assertEquals(7, engine.nextPieceCount,
				"trainingType 2 restores continueNextPieceCount");
	}

	@Test
	void onCustomEndingWhenStageReachesLast() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		setFieldBool(mode, "clearflag", true);
		setFieldInt(mode, "trainingType", 0);
		setFieldInt(mode, "stage", 26);   // MAX_STAGE_TOTAL - 1
		setFieldInt(mode, "laststage", 26);

		pressPush(engine, Controller.BUTTON_A);
		boolean ret = mode.onCustom(engine, 0);

		assertTrue(ret);
		assertEquals(1, engine.ending, "reaching last stage triggers ending");
		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);
		assertEquals(2, readFieldInt(mode, "allclear"),
				"clearing the very last stage flags full all-clear (2)");
	}

	@Test
	void onCustomNextStageAdvances() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		setFieldBool(mode, "clearflag", true);
		setFieldInt(mode, "trainingType", 0);
		setFieldInt(mode, "stage", 0);
		setFieldInt(mode, "laststage", 19);
		setFieldInt(mode, "limittimeNow", 100);
		setFieldInt(mode, "timeextendStageClearSeconds", 0);

		pressPush(engine, Controller.BUTTON_A);
		boolean ret = mode.onCustom(engine, 0);

		assertTrue(ret);
		assertEquals(1, readFieldInt(mode, "stage"), "stage advances by 1");
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	@Test
	void onCustomTimeMeterAnimatesDuringExtension() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		// statc[0] already past first frame so the bonus is computed already
		engine.statc[0] = 1;
		engine.statc[1] = 0;
		setFieldInt(mode, "timeextendStageClearSeconds", 10); // < 30 -> +4 / frame
		setFieldInt(mode, "limittimeNow", 100);
		setFieldInt(mode, "limittimeStart", 3600);
		setFieldBool(mode, "skipflag", false);

		boolean ret = mode.onCustom(engine, 0);

		assertTrue(ret);
		assertEquals(4, engine.statc[1], "meter animation advances statc[1] by 4");
	}

	// ---------------------------------------------------------------
	// renderCustom (1338-1391)
	// ---------------------------------------------------------------

	@Test
	void renderCustomEarlyReturnBeforeFirstFrame() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0; // < 1 -> early return
		mode.renderCustom(engine, 0);
	}

	@Test
	void renderCustomClearScreen() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.statc[0] = 2;
		engine.statc[1] = 10;
		setFieldBool(mode, "clearflag", true);
		setFieldInt(mode, "stage", 5);
		setFieldInt(mode, "limittimeNow", 1000);
		setFieldInt(mode, "cleartime", 300);
		setFieldInt(mode, "timeextendStageClearSeconds", 10);

		mode.renderCustom(engine, 0);
	}

	@Test
	void renderCustomSkipScreen() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.statc[0] = 2;
		engine.statc[1] = 5;
		setFieldBool(mode, "clearflag", false);
		setFieldBool(mode, "skipflag", true);
		setFieldInt(mode, "stage", 3);
		setFieldInt(mode, "limittimeNow", 1000);
		setFieldInt(mode, "trainingType", 0);
		setFieldInt(mode, "clearper", 50);

		mode.renderCustom(engine, 0);
	}

	@Test
	void renderCustomTimeUpScreen() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.statc[0] = 2;
		setFieldBool(mode, "clearflag", false);
		setFieldBool(mode, "skipflag", false);
		setFieldInt(mode, "stagetimeNow", 0);
		setFieldInt(mode, "stagetimeStart", 3600); // time-up branch
		setFieldInt(mode, "stage", 2);
		setFieldInt(mode, "limittimeNow", 1000);
		setFieldInt(mode, "trainingType", 0);
		setFieldInt(mode, "clearper", 33);

		mode.renderCustom(engine, 0);
	}

	// ---------------------------------------------------------------
	// onGameOver / renderGameOver (continue screen)
	// ---------------------------------------------------------------

	@Test
	void onGameOverFirstFrameSetsUpContinue() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		setFieldBool(mode, "noContinue", false);
		engine.statc[0] = 0;

		boolean ret = mode.onGameOver(engine, 0);

		assertTrue(ret, "continue flow returns true");
		assertFalse(engine.timerActive, "first frame stops the timer");
		assertFalse(engine.allowTextRenderByReceiver,
				"first frame suppresses GAMEOVER text");
	}

	@Test
	void onGameOverContinueSelectNavigationAndYes() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		setFieldBool(mode, "noContinue", false);
		setFieldInt(mode, "trainingType", 0);
		setFieldInt(mode, "limittimeStart", 3600);
		setFieldInt(mode, "continueNextPieceCount", 4);

		int continueStart = engine.field.getHeight() + 1;

		// UP/DOWN toggles the YES/NO cursor (statc[1])
		engine.statc[0] = continueStart;
		engine.statc[1] = 0;
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onGameOver(engine, 0);
		assertEquals(1, engine.statc[1], "DOWN toggles continue cursor to NO");

		// A with cursor 0 (YES) -> resume to READY
		engine.statc[0] = continueStart;
		engine.statc[1] = 0;
		pressPush(engine, Controller.BUTTON_A);
		mode.onGameOver(engine, 0);
		assertEquals(GameEngine.Status.READY, engine.stat,
				"YES continues into READY");
	}

	@Test
	void onGameOverContinueSelectNoEndsContinue() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		setFieldBool(mode, "noContinue", false);

		int continueStart = engine.field.getHeight() + 1;
		engine.statc[0] = continueStart;
		engine.statc[1] = 1; // NO
		pressPush(engine, Controller.BUTTON_A);

		mode.onGameOver(engine, 0);

		assertEquals(continueStart + 600, engine.statc[0],
				"NO jumps statc[0] to the end of the continue window");
	}

	@Test
	void onGameOverTimeoutSetsNoContinue() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		setFieldBool(mode, "noContinue", false);
		engine.statc[0] = engine.field.getHeight() + 1 + 600; // past window

		boolean ret = mode.onGameOver(engine, 0);

		assertTrue(ret);
		assertTrue(readFieldBool(mode, "noContinue"),
				"timeout sets noContinue");
	}

	@Test
	void onGameOverReturnsFalseWhenNoContinue() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		setFieldBool(mode, "noContinue", true);

		assertFalse(mode.onGameOver(engine, 0),
				"noContinue short-circuits to false");
	}

	@Test
	void renderGameOverContinuePrompt() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		setFieldBool(mode, "noContinue", false);
		setFieldInt(mode, "trainingType", 0);
		engine.statc[0] = engine.field.getHeight() + 1; // inside the continue window
		engine.statc[1] = 0;

		mode.renderGameOver(engine, 0);
	}

	// ---------------------------------------------------------------
	// onResult / renderResult (1497-1551)
	// ---------------------------------------------------------------

	@Test
	void onResultUpDownNavigatesPages() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		engine.statc[1] = 0;
		pressKey(engine, Controller.BUTTON_UP);
		mode.onResult(engine, 0);
		assertEquals(2, engine.statc[1], "UP at page 0 wraps to page 2");

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1], "DOWN at page 2 wraps to page 0");
	}

	@Test
	void renderResultAllThreePages() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "stage", 10);
		setFieldInt(mode, "clearper", 80);
		setFieldInt(mode, "allclear", 1);
		setFieldInt(mode, "rankingRank", 3);
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		for (int i = 0; i < sectiontime.length; i++) {
			sectiontime[i] = (i % 3 == 0) ? (i + 1) * 60 : (i % 3 == 1 ? -1 : -2);
		}

		engine.statc[1] = 0; // page 1 (stats + rank)
		mode.renderResult(engine, 0);
		engine.statc[1] = 1; // page 2 (section 1/2)
		mode.renderResult(engine, 0);
		engine.statc[1] = 2; // page 3 (section 2/2)
		mode.renderResult(engine, 0);
	}

	// ---------------------------------------------------------------
	// saveReplay ranking-update path (1580-1581)
	// ---------------------------------------------------------------

	@Test
	void saveReplayUpdatesRankingWhenQualified() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		// Default settings qualify for ranking, and a fresh table means any
		// finish ranks (#1), so rankingRank != -1 -> saveRanking branch.
		setFieldInt(mode, "startstage", 0);
		setFieldInt(mode, "trainingType", 0);
		setFieldInt(mode, "startnextc", 0);
		setFieldInt(mode, "stageset", -1);
		setFieldBool(mode, "always20g", false);
		setFieldInt(mode, "stage", 12);
		setFieldInt(mode, "clearper", 100);
		setFieldInt(mode, "allclear", 1);
		engine.statistics.time = 1234;
		engine.ai = null;

		mode.saveReplay(engine, 0, engine.owner.replayProp);

		assertTrue(readFieldInt(mode, "rankingRank") >= 0,
				"a qualifying finish should land a ranking slot");
		assertEquals(1, engine.owner.replayProp.getProperty(
				"gemmania.version", -1), "replay version is written");
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(GemManiaMode mode, boolean replayMode) {
		return freshEngine(mode, replayMode, new TempWriteReceiver());
	}

	private static GameEngine freshEngine(GemManiaMode mode, boolean replayMode,
			EventReceiver receiver) {
		GameManager manager = new GameManager(receiver);
		manager.replayMode = replayMode;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		mode.modeInit(manager);
		return manager.engine[0];
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

	private static void pressTwo(GameEngine engine, int btnA, int btnB) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btnA] = true;
		engine.ctrl.buttonTime[btnA] = 1;
		engine.ctrl.buttonPress[btnB] = true;
		engine.ctrl.buttonTime[btnB] = 1;
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static boolean readFieldBool(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static Object readField(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).get(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setFieldBool(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void invokeLoadStageSet(GemManiaMode mode, int id) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod("loadStageSet", int.class);
		m.setAccessible(true);
		m.invoke(mode, id);
	}

	private static void invokeSaveStageSet(GemManiaMode mode, int id) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod("saveStageSet", int.class);
		m.setAccessible(true);
		m.invoke(mode, id);
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
