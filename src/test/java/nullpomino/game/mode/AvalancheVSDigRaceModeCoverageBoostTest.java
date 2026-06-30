package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage booster for {@link AvalancheVSDigRaceMode}. Walks the large
 * {@code onSetting} configuration menu (29 cursor positions, 0..28) via
 * LEFT/RIGHT changes and A/B confirm/cancel, the replay auto-advance
 * branch, the two-player start branch, every {@code renderSetting} page,
 * and the score/ojama branches in {@code renderLast} / {@code lineClearEnd}
 * / {@code onLast}.
 */
class AvalancheVSDigRaceModeCoverageBoostTest {

	// ---------------------------------------------------------------
	// onSetting: UP/DOWN navigation across all 29 positions
	// ---------------------------------------------------------------

	@Test
	void onSettingUpNavigatesThroughAllPositions() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);

		setFieldInt(mode, "menuCursor", 0);
		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(28, readFieldInt(mode, "menuCursor"),
				"UP at cursor 0 should wrap to 28");

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"First DOWN from 28 should reach 0");

		for (int expected = 1; expected <= 28; expected++) {
			pressKey(engine, Controller.BUTTON_DOWN);
			mode.onSetting(engine, 0);
			assertEquals(expected, readFieldInt(mode, "menuCursor"),
					"Should be at cursor " + expected);
		}
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"DOWN from 28 should wrap to 0");
	}

	// ---------------------------------------------------------------
	// onSetting: LEFT/RIGHT at each cursor position
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor0Gravity() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = engine.speed.gravity;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(before - 1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor1Denominator() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = engine.speed.denominator;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2Are() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = engine.speed.are;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLine() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		int before = engine.speed.areLine;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		int before = engine.speed.lineDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelaySingleStep() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		int before = engine.speed.lockDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor5LockDelayTenStepWithButtonE() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		engine.speed.lockDelay = 20;
		pressKey(engine, Controller.BUTTON_RIGHT);
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1;
		mode.onSetting(engine, 0);
		assertEquals(30, engine.speed.lockDelay, "m>=10 should step lockDelay by 10");
	}

	@Test
	void onSettingCursor6Das() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		int before = engine.speed.das;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.das);
	}

	@Test
	void onSettingCursor7CascadeDelay() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		int before = engine.cascadeDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.cascadeDelay);
	}

	@Test
	void onSettingCursor8CascadeClearDelay() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		int before = engine.cascadeClearDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.cascadeClearDelay);
	}

	@Test
	void onSettingCursor9OjamaCounterMode() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		int before = getIntArray(mode, "ojamaCounterMode")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "ojamaCounterMode")[0]);
	}

	@Test
	void onSettingCursor10MaxAttackSingleStep() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		setIntArray(mode, "maxAttack", 5, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(6, getIntArray(mode, "maxAttack")[0]);
	}

	@Test
	void onSettingCursor10MaxAttackTenStepWithButtonE() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		setIntArray(mode, "maxAttack", 5, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1;
		mode.onSetting(engine, 0);
		assertEquals(15, getIntArray(mode, "maxAttack")[0], "m>=10 steps maxAttack by 10");
	}

	@Test
	void onSettingCursor11NumColors() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 11);
		setIntArray(mode, "numColors", 4, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(5, getIntArray(mode, "numColors")[0]);
	}

	@Test
	void onSettingCursor12RensaShibari() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 12);
		setIntArray(mode, "rensaShibari", 3, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(4, getIntArray(mode, "rensaShibari")[0]);
	}

	@Test
	void onSettingCursor13OjamaRateTenStep() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 13);
		int before = getIntArray(mode, "ojamaRate")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 10, getIntArray(mode, "ojamaRate")[0]);
	}

	@Test
	void onSettingCursor13OjamaRateHundredStepWithButtonE() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 13);
		setIntArray(mode, "ojamaRate", 500, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1;
		mode.onSetting(engine, 0);
		assertEquals(600, getIntArray(mode, "ojamaRate")[0], "m>=10 steps ojamaRate by 100");
	}

	@Test
	void onSettingCursor14HurryupSingleStep() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 14);
		int before = getIntArray(mode, "hurryupSeconds")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void onSettingCursor14HurryupScaledWithButtonF() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 14);
		setIntArray(mode, "hurryupSeconds", 50, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		// BUTTON_F => m=1000, m>10 => += change*m/10 = +100
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onSetting(engine, 0);
		assertEquals(150, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void onSettingCursor15OjamaHard() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 15);
		int before = getIntArray(mode, "ojamaHard")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "ojamaHard")[0]);
	}

	@Test
	void onSettingCursor16DangerColumnDoubleToggles() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16);
		boolean before = getBoolArray(mode, "dangerColumnDouble")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "dangerColumnDouble")[0]);
	}

	@Test
	void onSettingCursor17DangerColumnShowXToggles() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 17);
		boolean before = getBoolArray(mode, "dangerColumnShowX")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "dangerColumnShowX")[0]);
	}

	@Test
	void onSettingCursor18HandicapRows() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 18);
		setIntArray(mode, "handicapRows", 5, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(6, getIntArray(mode, "handicapRows")[0]);
	}

	@Test
	void onSettingCursor18HandicapRowsWrapsLow() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 18);
		setIntArray(mode, "handicapRows", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(11, getIntArray(mode, "handicapRows")[0], "0 - 1 wraps to 11");
	}

	@Test
	void onSettingCursor19NewChainPowerToggles() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 19);
		boolean before = getBoolArray(mode, "newChainPower")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "newChainPower")[0]);
	}

	@Test
	void onSettingCursor20ColorClearSize() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 20);
		int before = engine.colorClearSize;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.colorClearSize);
	}

	@Test
	void onSettingCursor21OutlineType() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 21);
		int before = getIntArray(mode, "outlineType")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "outlineType")[0]);
	}

	@Test
	void onSettingCursor22ChainDisplayType() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 22);
		int before = getIntArray(mode, "chainDisplayType")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "chainDisplayType")[0]);
	}

	@Test
	void onSettingCursor23CascadeSlowToggles() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 23);
		boolean before = getBoolArray(mode, "cascadeSlow")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "cascadeSlow")[0]);
	}

	@Test
	void onSettingCursor24Bgmno() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 24);
		setFieldInt(mode, "bgmno", 1);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(2, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor24BgmnoWrapsLow() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 24);
		setFieldInt(mode, "bgmno", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(BGMStatus.BGM_COUNT - 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor25EnableSEToggles() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 25);
		boolean before = getBoolArray(mode, "enableSE")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "enableSE")[0]);
	}

	@Test
	void onSettingCursor26BigDisplayToggles() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 26);
		boolean before = readFieldBool(mode, "bigDisplay");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "bigDisplay"));
	}

	@Test
	void onSettingCursor27And28PresetNumber() throws Exception {
		for (int cursor : new int[]{27, 28}) {
			AvalancheVSDigRaceMode m = new AvalancheVSDigRaceMode();
			GameEngine e = freshEngine(m, false);
			setMenuState(e, m, cursor);
			int before = getIntArray(m, "presetNumber")[0];
			pressKey(e, Controller.BUTTON_RIGHT);
			m.onSetting(e, 0);
			assertEquals(before + 1, getIntArray(m, "presetNumber")[0],
					"Cursor " + cursor + " should adjust presetNumber");
		}
	}

	// ---------------------------------------------------------------
	// A button confirm paths
	// ---------------------------------------------------------------

	@Test
	void onSettingPressAAtCursor27LoadsPreset() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 27, 10);
		engine.speed.gravity = 42;

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(4, engine.speed.gravity,
				"A at cursor 27 should load the preset's default gravity");
	}

	@Test
	void onSettingPressAAtCursor28SavesPreset() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 28, 10);
		engine.speed.gravity = 99;
		setIntArray(mode, "presetNumber", 0, 0);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(99, engine.owner.modeConfig.getProperty(
				"avalanchevsdigrace.gravity.0", -1));
	}

	@Test
	void onSettingPressAAtOtherCursorSavesSettingAndStarts() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		setIntArray(mode, "handicapRows", 7, 0);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(1, engine.statc[4], "non-preset cursor advances statc[4]");
		assertEquals(7, engine.owner.modeConfig.getProperty(
				"avalanchevsdigrace.ojamaHandicap.p0", -1),
				"other-setting save persists handicap rows");
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);

		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	// ---------------------------------------------------------------
	// replay-mode auto-advance branch (statc[4]==0, replayMode==true)
	// ---------------------------------------------------------------

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 0);

		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));
		assertEquals(0, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 60);
		mode.onSetting(engine, 0);
		assertEquals(9, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 120);
		mode.onSetting(engine, 0);
		assertEquals(18, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 180);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4]);
	}

	// ---------------------------------------------------------------
	// statc[4]==1 branch: two-player start and cancel
	// ---------------------------------------------------------------

	@Test
	void onSettingBothReadyStartsBothEngines() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		manager.engine[0].statc[4] = 1;
		manager.engine[1].statc[4] = 1;

		// Player 1 is the one that triggers the dual READY transition.
		mode.onSetting(manager.engine[1], 1);

		assertEquals(GameEngine.Status.READY, manager.engine[0].stat);
		assertEquals(GameEngine.Status.READY, manager.engine[1].stat);
	}

	@Test
	void onSettingWaitStateCancelResetsStatc() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		// Only this engine is in wait; the dual-ready guard fails so we
		// fall through to the cancel branch.
		manager.engine[0].statc[4] = 1;
		manager.engine[1].statc[4] = 0;
		pressPush(manager.engine[0], Controller.BUTTON_B);

		mode.onSetting(manager.engine[0], 0);

		assertEquals(0, manager.engine[0].statc[4],
				"B in wait state returns to the menu");
	}

	// ---------------------------------------------------------------
	// renderSetting: all three pages and wait state
	// ---------------------------------------------------------------

	@Test
	void renderSettingPage1() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage2() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 9);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage3() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 18);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingWaitState() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 1;
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// renderLast: ojama / score display branches
	// ---------------------------------------------------------------

	@Test
	void renderLastBigDisplayWithOjamaAndScore() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.gameStarted = true;
		engine.displaysize = 1;
		engine.stat = GameEngine.Status.MOVE;
		setIntArray(mode, "ojama", 12, 0);
		setIntArray(mode, "ojamaAdd", 3, 0);
		setIntArray(mode, "lastscore", 100, 0);
		setIntArray(mode, "lastmultiplier", 2, 0);
		setIntArray(mode, "scgettime", 5, 0);
		setIntArray(mode, "score", 1234, 0);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastNormalDisplayPlayerOne() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		GameEngine engine = manager.engine[1];
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.gameStarted = true;
		engine.displaysize = 0;
		engine.stat = GameEngine.Status.ARE;
		setIntArray(mode, "ojama", 6, 1);
		setIntArray(mode, "ojamaAdd", 0, 1);
		setIntArray(mode, "score", 50, 1);

		mode.renderLast(engine, 1);
	}

	@Test
	void renderLastReturnsEarlyWhenGameInactive() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();
		engine.gameActive = false;
		engine.gameStarted = true;
		engine.displaysize = 0;

		mode.renderLast(engine, 0);
	}

	// ---------------------------------------------------------------
	// lineClearEnd: garbage drop and game over
	// ---------------------------------------------------------------

	@Test
	void lineClearEndDropsOjama() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojama", 12, 0);
		setIntArray(mode, "maxAttack", 10, 0);
		setBoolArray(mode, "ojamaDrop", false, 0);
		setBoolArray(mode, "cleared", false, 0);
		setIntArray(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);

		boolean result = mode.lineClearEnd(engine, 0);

		assertTrue(result, "ojama>0 should trigger a garbage drop");
		assertTrue(getIntArray(mode, "ojama")[0] < 12,
				"ojama count should shrink after the drop");
	}

	@Test
	void lineClearEndTransfersOjamaAddToEnemy() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojamaAdd", 5, 1);
		setIntArray(mode, "ojama", 0, 0);
		setIntArray(mode, "ojama", 0, 1);

		mode.lineClearEnd(engine, 0);

		assertEquals(5, getIntArray(mode, "ojama")[1]);
		assertEquals(0, getIntArray(mode, "ojamaAdd")[1]);
	}

	@Test
	void lineClearEndGameOverWhenTopFilled() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojama", 0, 0);
		setIntArray(mode, "ojamaAdd", 0, 1);
		engine.field.setBlock(2, 0, new Block(Block.BLOCK_COLOR_GRAY));

		boolean result = mode.lineClearEnd(engine, 0);

		assertFalse(result);
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	// ---------------------------------------------------------------
	// onLast: settlement branch
	// ---------------------------------------------------------------

	@Test
	void onLastDecrementsScgettimeAndChainDisplay() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		GameEngine engine = manager.engine[0];

		setIntArray(mode, "scgettime", 3, 0);
		setIntArray(mode, "chainDisplay", 2, 0);

		mode.onLast(engine, 0);

		assertEquals(2, getIntArray(mode, "scgettime")[0]);
		assertEquals(1, getIntArray(mode, "chainDisplay")[0]);
	}

	@Test
	void onLastSettlesWhenPlayerTwoFieldHasNoGems() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		GameEngine e0 = manager.engine[0];
		GameEngine e1 = manager.engine[1];

		e0.createFieldIfNeeded();
		e1.createFieldIfNeeded();
		e0.gameActive = true;
		e1.gameActive = true;
		e0.stat = GameEngine.Status.MOVE;
		e1.stat = GameEngine.Status.MOVE;

		// Player 1 (index 1) is settled by this call. Both fields have
		// zero gems by default, so both lose => a draw.
		mode.onLast(e1, 1);

		assertEquals(-1, readFieldInt(mode, "winnerID"), "no gems on either side => draw");
		assertEquals(GameEngine.Status.GAMEOVER, e0.stat);
		assertEquals(GameEngine.Status.GAMEOVER, e1.stat);
	}

	@Test
	void onLastPlayerOneWinsWhenEnemyTopsOut() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = newManager(mode);
		GameEngine e0 = manager.engine[0];
		GameEngine e1 = manager.engine[1];

		e0.createFieldIfNeeded();
		e1.createFieldIfNeeded();
		e0.gameActive = true;
		e1.gameActive = true;
		// engine[0] (1P) still has its empty field but is not topped out.
		e0.stat = GameEngine.Status.MOVE;
		// engine[1] (2P) topped out => p2Lose; it keeps a gem so the
		// settlement does not also mark p1 as a loser.
		e1.stat = GameEngine.Status.GAMEOVER;
		int h = e1.field.getHeight();
		e1.field.setBlock(0, h - 1, new Block(Block.BLOCK_COLOR_GEM_RED));

		mode.onLast(e1, 1);

		assertEquals(0, readFieldInt(mode, "winnerID"), "player 1 should win");
		assertEquals(GameEngine.Status.EXCELLENT, e0.stat);
	}

	// ---------------------------------------------------------------
	// saveReplay
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesHandicapAndVersion() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setIntArray(mode, "handicapRows", 9, 0);
		CustomProperties prop = new CustomProperties();

		mode.saveReplay(engine, 0, prop);

		assertEquals(9, prop.getProperty("avalanchevsdigrace.ojamaHandicap.p0", -1));
		assertEquals(0, prop.getProperty("avalanchevsdigrace.version", -1));
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	/** EventReceiver whose saveModeConfig is a no-op so the menu's
	 *  confirm path does not touch the tracked config/setting/mode.cfg. */
	private static final class NoSaveReceiver extends EventReceiver {
		@Override
		public void saveModeConfig(CustomProperties modeConfig) {
			// no-op: keep the on-disk mode config untouched
		}
	}

	private static GameManager newManager(AvalancheVSDigRaceMode mode) {
		GameManager manager = new GameManager(new NoSaveReceiver());
		manager.receiver = new NoSaveReceiver();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.modeInit(manager);
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		return manager;
	}

	private static GameEngine freshEngine(AvalancheVSDigRaceMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new NoSaveReceiver());
		manager.receiver = new NoSaveReceiver();
		manager.replayMode = replayMode;
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		mode.playerInit(manager.engine[0], 0);
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, AvalancheVSDigRaceMode mode)
			throws Exception {
		setMenuState(engine, mode, 0, 0);
	}

	private static void setMenuState(GameEngine engine, AvalancheVSDigRaceMode mode,
			int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, AvalancheVSDigRaceMode mode,
			int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
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
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
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

	private static int[] getIntArray(Object obj, String name) throws Exception {
		return (int[]) findField(obj.getClass(), name).get(obj);
	}

	private static boolean[] getBoolArray(Object obj, String name) throws Exception {
		return (boolean[]) findField(obj.getClass(), name).get(obj);
	}

	private static void setIntArray(Object obj, String name, int value, int index) throws Exception {
		((int[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static void setBoolArray(Object obj, String name, boolean value, int index) throws Exception {
		((boolean[]) findField(obj.getClass(), name).get(obj))[index] = value;
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
