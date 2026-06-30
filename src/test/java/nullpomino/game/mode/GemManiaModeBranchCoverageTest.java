package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch coverage for {@link GemManiaMode} beyond the settings-menu wraparound
 * (covered in {@link GemManiaModeSettingWraparoundTest}). Exercises:
 * <ul>
 *   <li>edit-menu decide / cancel switch cases (A/B/D buttons)</li>
 *   <li>onResult page wraparound</li>
 *   <li>calcScore gem-clear + level-up branches</li>
 *   <li>onCustom time-bonus / all-clear / next-screen branches</li>
 *   <li>onGameOver continue YES/NO branches</li>
 *   <li>render hooks across mode states (no-assertion, threshold branches)</li>
 * </ul>
 */
class GemManiaModeBranchCoverageTest {

	private static final int MAX_STAGE_TOTAL = 27;
	private static final int MAX_STAGE_NORMAL = 20;

	/**
	 * EventReceiver whose persistence methods are no-ops, so the edit-menu
	 * save-map / save-stage-set paths can be exercised without writing the
	 * TRACKED files under config/map/gemmania/.
	 */
	private static final class NonPersistingReceiver extends EventReceiver {
		@Override
		public boolean saveProperties(String filename, CustomProperties prop) {
			return true;
		}
		@Override
		public void saveModeConfig(CustomProperties modeConfig) {
			// no-op
		}
	}

	private static GameEngine freshEngine(GemManiaMode mode) {
		GameManager manager = new GameManager(new NonPersistingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void press(GameEngine engine, int button) {
		engine.ctrl.reset();
		engine.ctrl.buttonPress[button] = true;
		engine.ctrl.buttonTime[button] = 1;
	}

	private static void pressBoth(GameEngine engine, int a, int b) {
		engine.ctrl.reset();
		engine.ctrl.buttonPress[a] = true;
		engine.ctrl.buttonTime[a] = 1;
		engine.ctrl.buttonPress[b] = true;
		engine.ctrl.buttonTime[b] = 1;
	}

	/** Builds a single full row of gem blocks at row y and flags it as a line. */
	private static void makeGemLine(GameEngine engine, int y) {
		for(int x = 0; x < engine.field.getWidth(); x++) {
			Block b = new Block();
			b.color = Block.BLOCK_COLOR_GEM_RED;
			engine.field.setBlock(x, y, b);
		}
		engine.field.setLineFlag(y, true);
	}

	// ----------------------------------------------------------------------
	// onResult page wraparound (L1499, L1504)
	// ----------------------------------------------------------------------

	@Test
	void onResultPageWrapsBothDirections() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// UP from page 0 wraps to 2
		engine.statc[1] = 0;
		press(engine, Controller.BUTTON_UP);
		mode.onResult(engine, 0);
		assertEquals(2, engine.statc[1]);

		// DOWN from page 2 wraps to 0
		engine.statc[1] = 2;
		press(engine, Controller.BUTTON_DOWN);
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1]);
	}

	// ----------------------------------------------------------------------
	// edit-main screen decide switch (L558 cases) + cancel (L585)
	// ----------------------------------------------------------------------

	@Test
	void editMainDecideEnterStageScreen() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		// case 0 -> editModeScreen becomes 2
		assertEquals(2, readInt(mode, "editModeScreen"));
	}

	@Test
	void editMainDecideLoadAndSaveMapAndSet() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setField(mode, "propStageSet", new CustomProperties());

		// case 1: load map (propStageSet != null && field != null)
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 1);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// case 2: save map
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// case 3: load stage set
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// case 4: save stage set
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 4);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// still on edit-main screen (cases 1-4 do not change editModeScreen)
		assertEquals(1, readInt(mode, "editModeScreen"));
	}

	@Test
	void editMainCancelWithDplusE() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "editModeScreen", 1);
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 10);
		pressBoth(engine, Controller.BUTTON_D, Controller.BUTTON_E);
		mode.onSetting(engine, 0);
		// D+E exits the edit menu entirely
		assertEquals(0, readInt(mode, "editModeScreen"));
	}

	// ----------------------------------------------------------------------
	// edit-stage screen decide (L647/L650 enterFieldEdit, else) + cancel (L661)
	// ----------------------------------------------------------------------

	@Test
	void editStageDecideEntersFieldEdit() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "editModeScreen", 2);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);
		boolean ret = mode.onSetting(engine, 0);
		// cursor 0 -> enterFieldEdit, returns true and switches engine status
		assertTrue(ret);
		assertEquals(GameEngine.Status.FIELDEDIT, engine.stat);
	}

	@Test
	void editStageDecideNonZeroReturnsToMain() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "editModeScreen", 2);
		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		// non-zero cursor -> back to edit-main screen
		assertEquals(1, readInt(mode, "editModeScreen"));
	}

	@Test
	void editStageCancelWithB() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "editModeScreen", 2);
		setInt(mode, "menuCursor", 3);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		// B cancels back to edit-main screen
		assertEquals(1, readInt(mode, "editModeScreen"));
	}

	// ----------------------------------------------------------------------
	// normal menu decide (A saves + returns false) / cancel (B quit) / edit (D)
	// ----------------------------------------------------------------------

	@Test
	void normalMenuDecideReturnsFalse() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "editModeScreen", 0);
		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);
		boolean ret = mode.onSetting(engine, 0);
		assertFalse(ret);
	}

	@Test
	void normalMenuCancelSetsQuitFlag() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "editModeScreen", 0);
		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	@Test
	void normalMenuEditButtonEntersEditWithDefaultStageSet() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "editModeScreen", 0);
		setInt(mode, "menuCursor", 2);
		setInt(mode, "menuTime", 10);
		setInt(mode, "stageset", -1); // negative -> reset to 0 inside the D handler
		press(engine, Controller.BUTTON_D);
		mode.onSetting(engine, 0);
		assertEquals(1, readInt(mode, "editModeScreen"));
		assertEquals(0, readInt(mode, "stageset"));
	}

	@Test
	void replayModeMenuTimesOut() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = true;
		mode.playerInit(engine, 0);

		setInt(mode, "editModeScreen", 0);
		setInt(mode, "menuTime", 59);
		engine.ctrl.reset();
		boolean ret = mode.onSetting(engine, 0);
		// menuTime hits 60 -> returns false, menuCursor forced to -1
		assertFalse(ret);
		assertEquals(-1, readInt(mode, "menuCursor"));
	}

	// ----------------------------------------------------------------------
	// calcScore: gem clear, rest depletion, level-up, section bump, clamp
	// ----------------------------------------------------------------------

	@Test
	void calcScoreGemClearSetsClearFlagWhenRestDepleted() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		setInt(mode, "rest", 1);          // 10 gems clearing drives rest <= 0
		setInt(mode, "speedlevel", 0);
		setInt(mode, "nextseclv", 100);
		setInt(mode, "limittimeNow", 0);
		makeGemLine(engine, 0);

		mode.calcScore(engine, 0, 1);

		assertTrue(readBool(mode, "clearflag"), "rest depleted -> clearflag set");
		assertTrue(readInt(mode, "limittimeNow") > 0, "gem clear extends limit time");
	}

	@Test
	void calcScoreClampsLevelAt998() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		setInt(mode, "speedlevel", 997);
		setInt(mode, "nextseclv", 999999);
		makeGemLine(engine, 0);

		// lines == 4 -> levelplus 6 -> 997 + 6 = 1003 -> clamped to 998
		mode.calcScore(engine, 0, 4);
		assertEquals(998, readInt(mode, "speedlevel"));
	}

	@Test
	void calcScoreAdvancesToNextSection() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		setInt(mode, "speedlevel", 98);
		setInt(mode, "nextseclv", 100);
		makeGemLine(engine, 0);

		// lines == 3 -> levelplus 4 -> 98 + 4 = 102 >= 100 -> next section
		mode.calcScore(engine, 0, 3);
		assertEquals(200, readInt(mode, "nextseclv"));
	}

	// ----------------------------------------------------------------------
	// onCustom: time bonus tiers, all-clear, next-screen branches
	// ----------------------------------------------------------------------

	@Test
	void onCustomTimeBonusFiveSecondsForMidCleartime() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.resetStatc();
		setBool(mode, "clearflag", true);
		setInt(mode, "cleartime", 15 * 60);   // >=600 and <1200 -> 5 sec tier
		setInt(mode, "stage", 0);             // not the boss stage
		setInt(mode, "trystage", 0);
		setInt(mode, "clearstage", 0);
		mode.onCustom(engine, 0);
		assertEquals(5, readInt(mode, "timeextendStageClearSeconds"));
	}

	@Test
	void onCustomTrainingBestTimeUpdated() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.resetStatc();
		setBool(mode, "clearflag", true);
		setInt(mode, "trainingType", 1);
		setInt(mode, "trainingBestTime", -1);  // first clear -> records time
		setInt(mode, "cleartime", 5 * 60);
		setInt(mode, "stage", 0);
		mode.onCustom(engine, 0);
		assertEquals(5 * 60, readInt(mode, "trainingBestTime"));
	}

	@Test
	void onCustomAllClearTwoOnFinalStage() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.resetStatc();
		engine.statc[0] = 300;                 // jump straight to next-screen logic
		setBool(mode, "clearflag", true);
		setInt(mode, "trainingType", 0);
		setInt(mode, "stage", MAX_STAGE_TOTAL - 1);
		setInt(mode, "laststage", MAX_STAGE_TOTAL - 1);
		mode.onCustom(engine, 0);
		assertEquals(2, readInt(mode, "allclear"));
		assertEquals(1, engine.ending);
	}

	@Test
	void onCustomTrainingResetRestoresNextPieceCount() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.resetStatc();
		engine.statc[0] = 300;
		setBool(mode, "clearflag", true);
		setInt(mode, "trainingType", 2);       // ON+RESET
		setInt(mode, "continueNextPieceCount", 42);
		setInt(mode, "timeextendStageClearSeconds", 0);
		mode.onCustom(engine, 0);
		assertEquals(42, engine.nextPieceCount);
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	@Test
	void onCustomNextStageIncrementsStage() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.resetStatc();
		engine.statc[0] = 300;
		setBool(mode, "clearflag", true);
		setInt(mode, "trainingType", 0);
		setInt(mode, "stage", 0);
		setInt(mode, "laststage", MAX_STAGE_NORMAL - 1);
		setInt(mode, "limittimeNow", 100);
		setInt(mode, "timeextendStageClearSeconds", 1);
		mode.onCustom(engine, 0);
		assertEquals(1, readInt(mode, "stage"));
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	// ----------------------------------------------------------------------
	// onGameOver continue screen YES / NO branches
	// ----------------------------------------------------------------------

	@Test
	void onGameOverContinueYesRestartsReady() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		setBool(mode, "noContinue", false);
		setInt(mode, "trainingType", 0);
		setInt(mode, "limittimeStart", 500);
		// Jump into the continue-selection window
		engine.resetStatc();
		engine.statc[0] = engine.field.getHeight() + 1;
		engine.statc[1] = 0; // YES
		press(engine, Controller.BUTTON_A);
		mode.onGameOver(engine, 0);
		assertEquals(GameEngine.Status.READY, engine.stat);
		assertEquals(500, readInt(mode, "limittimeNow"));
	}

	@Test
	void onGameOverContinueNoEndsRun() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		setBool(mode, "noContinue", false);
		engine.resetStatc();
		engine.statc[0] = engine.field.getHeight() + 1;
		engine.statc[1] = 1; // NO
		press(engine, Controller.BUTTON_A);
		mode.onGameOver(engine, 0);
		// NO jumps statc[0] to the terminal threshold
		assertEquals((engine.field.getHeight() + 1) + 600, engine.statc[0]);
	}

	@Test
	void onGameOverCursorWrapDownToZero() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		setBool(mode, "noContinue", false);
		engine.resetStatc();
		engine.statc[0] = engine.field.getHeight() + 1;
		engine.statc[1] = 1;
		press(engine, Controller.BUTTON_DOWN);
		mode.onGameOver(engine, 0);
		// statc[1] increments to 2 then wraps back to 0
		assertEquals(0, engine.statc[1]);
	}

	@Test
	void onGameOverTerminalSetsNoContinue() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		setBool(mode, "noContinue", false);
		engine.resetStatc();
		engine.statc[0] = (engine.field.getHeight() + 1) + 600;
		engine.ctrl.reset();
		mode.onGameOver(engine, 0);
		assertTrue(readBool(mode, "noContinue"));
	}

	// ----------------------------------------------------------------------
	// onLast: skip-button branch + countdown thresholds
	// ----------------------------------------------------------------------

	@Test
	void onLastSkipButtonTriggersSkip() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.gameActive = true;
		engine.timerActive = true;
		setInt(mode, "skipbuttonPressTime", 59); // ++ -> 60 triggers
		setInt(mode, "stage", 0);                // < MAX_STAGE_NORMAL - 1
		setInt(mode, "trainingType", 0);
		setInt(mode, "limittimeNow", 40 * 60);   // > 30*60
		setBool(mode, "clearflag", false);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 2; // isPress true
		mode.onLast(engine, 0);
		assertTrue(readBool(mode, "skipflag"));
		assertEquals(GameEngine.Status.CUSTOM, engine.stat);
	}

	@Test
	void onLastCountdownSoundsNearLimit() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.gameActive = true;
		engine.timerActive = true;
		setInt(mode, "limittimeNow", 10 * 60 + 1);  // decrements to 600 -> countdown
		setInt(mode, "limittimeStart", 3600);
		setInt(mode, "stagetimeNow", 10 * 60 + 1);  // decrements to 600 -> countdown
		engine.ctrl.reset();
		mode.onLast(engine, 0);
		assertEquals(10 * 60, readInt(mode, "limittimeNow"));
		assertEquals(10 * 60, readInt(mode, "stagetimeNow"));
	}

	// ----------------------------------------------------------------------
	// onMove / onARE level-up branches
	// ----------------------------------------------------------------------

	@Test
	void onMoveLevelStopSoundAtSectionBoundary() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		engine.resetStatc();
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBool(mode, "lvupflag", false);
		setBool(mode, "lvstopse", true);
		setInt(mode, "speedlevel", 98);  // < nextseclv-1 (99) -> ++ to 99 == nextseclv-1
		setInt(mode, "nextseclv", 100);
		mode.onMove(engine, 0);
		assertEquals(99, readInt(mode, "speedlevel"));
	}

	@Test
	void onAreLevelUpSetsLvupFlag() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		engine.resetStatc();
		engine.statc[0] = 5;
		engine.statc[1] = 5; // statc[0] >= statc[1]-1
		setBool(mode, "lvupflag", false);
		setBool(mode, "lvstopse", true);
		setInt(mode, "speedlevel", 98);
		setInt(mode, "nextseclv", 100);
		mode.onARE(engine, 0);
		assertEquals(99, readInt(mode, "speedlevel"));
		assertTrue(readBool(mode, "lvupflag"));
	}

	// ----------------------------------------------------------------------
	// pieceLocked mirror interrupt
	// ----------------------------------------------------------------------

	@Test
	void pieceLockedMirrorInterruptOnInterval() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gimmickMirror", 1);            // every piece
		setInt(mode, "thisStageTotalPieceLockCount", 0); // ++ -> 1, 1 % 1 == 0
		mode.pieceLocked(engine, 0, 1);
		assertEquals(GameEngine.INTERRUPTITEM_MIRROR, engine.interruptItemNumber);
	}

	// ----------------------------------------------------------------------
	// Render hooks across mode states (no-assertion threshold branches).
	// These just confirm the render code paths execute without throwing.
	// ----------------------------------------------------------------------

	@Test
	void renderHooksDoNotThrow() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// renderSetting: all three screens
		setInt(mode, "editModeScreen", 1);
		setInt(mode, "gimmickMirror", 5);
		mode.renderSetting(engine, 0);
		setInt(mode, "editModeScreen", 2);
		mode.renderSetting(engine, 0);
		setInt(mode, "editModeScreen", 0);
		setInt(mode, "trainingType", 2);
		setInt(mode, "stageset", 3);
		mode.renderSetting(engine, 0);

		// renderLast in playing state with various gimmicks / section time / timeextend
		engine.stat = GameEngine.Status.MOVE;
		setBool(mode, "showsectiontime", true);
		setInt(mode, "gimmickRoll", 2);
		setInt(mode, "timeextendDisp", 5);
		setInt(mode, "stagetimeStart", 3600);
		setInt(mode, "limittimeStart", 3600);
		setInt(mode, "stage", 0);
		engine.timerActive = true;
		mode.renderLast(engine, 0);

		// renderLast in setting state (ranking table path)
		engine.stat = GameEngine.Status.SETTING;
		setInt(mode, "startstage", 0);
		setBool(mode, "always20g", false);
		setInt(mode, "trainingType", 0);
		setInt(mode, "startnextc", 0);
		setInt(mode, "stageset", -1);
		mode.renderLast(engine, 0);

		// renderReady at the title-flash point
		engine.statc[0] = 200;
		setInt(mode, "stage", MAX_STAGE_NORMAL); // EX-stage label branch
		mode.renderReady(engine, 0);
		setInt(mode, "stage", 0);
		mode.renderReady(engine, 0);

		// renderCustom clear / skip / time-up branches
		engine.statc[0] = 4;
		setBool(mode, "clearflag", true);
		mode.renderCustom(engine, 0);
		setBool(mode, "clearflag", false);
		setBool(mode, "skipflag", true);
		mode.renderCustom(engine, 0);
		setBool(mode, "skipflag", false);
		setInt(mode, "stagetimeNow", 0);
		setInt(mode, "stagetimeStart", 3600);
		mode.renderCustom(engine, 0);

		// renderGameOver continue prompt
		engine.ending = 0;
		setBool(mode, "noContinue", false);
		engine.statc[0] = engine.field.getHeight() + 1;
		engine.statc[1] = 0;
		mode.renderGameOver(engine, 0);

		// renderResult all three pages
		engine.statc[1] = 0;
		setInt(mode, "allclear", 2);
		mode.renderResult(engine, 0);
		engine.statc[1] = 1;
		mode.renderResult(engine, 0);
		engine.statc[1] = 2;
		mode.renderResult(engine, 0);

		assertTrue(true);
	}

	// --- reflection helpers ---

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		field(obj.getClass(), name).set(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
	}

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
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
