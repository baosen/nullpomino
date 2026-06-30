package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage-boost tests for {@link AvalancheVSFeverMode}, targeting the large
 * uncovered {@code onSetting} menu (cursor 0-29 LEFT/RIGHT/E/F adjustments,
 * A-button load/save preset and save-other-setting confirm paths, B-button
 * cancel/quit, the replay auto-advance branch, and the post-config WAIT
 * branch), {@code renderSetting}'s four pages and WAIT state, {@code renderLast}
 * color/displaysize/handicap branches, the replay {@code playerInit} path,
 * fever-map loading via cursor 19, and {@code lineClearEnd}'s garbage-drop
 * return.
 *
 * <p>To avoid mutating tracked config files, the engine is built with a
 * {@link RedirectReceiver} that diverts {@code saveModeConfig} and
 * {@code saveProperties} writes into the JVM temp directory; the production
 * code paths still execute.
 */
class AvalancheVSFeverModeCoverageBoostTest {

	// ------------------------------------------------------------------
	// onSetting: LEFT/RIGHT adjustments per cursor position (134-280)
	// ------------------------------------------------------------------

	@Test
	void onSettingCursor0Gravity() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 0);
		int before = engine.speed.gravity;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor0GravityWrapWithMultiplier() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 0);
		engine.speed.gravity = 0;
		// BUTTON_F gives m=1000 -> gravity goes negative -> wraps to 99999
		pressKeyWith(engine, Controller.BUTTON_LEFT, Controller.BUTTON_F);
		mode.onSetting(engine, 0);
		assertEquals(99999, engine.speed.gravity);
	}

	@Test
	void onSettingCursor1Denominator() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 1);
		engine.speed.denominator = 99999;
		pressKeyWith(engine, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		mode.onSetting(engine, 0);
		assertEquals(-1, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2Are() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 2);
		engine.speed.are = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLine() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 3);
		engine.speed.areLine = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 4);
		engine.speed.lineDelay = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 5);
		engine.speed.lockDelay = 30;
		// m>=10 path adds change*10
		pressKeyWith(engine, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		mode.onSetting(engine, 0);
		assertEquals(40, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor5LockDelaySingleStepAndWrap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 5);
		engine.speed.lockDelay = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(999, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor6Das() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 6);
		engine.speed.das = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.das);
	}

	@Test
	void onSettingCursor7CascadeDelay() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 7);
		engine.cascadeDelay = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(20, engine.cascadeDelay);
	}

	@Test
	void onSettingCursor8CascadeClearDelay() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 8);
		engine.cascadeClearDelay = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.cascadeClearDelay);
	}

	@Test
	void onSettingCursor9ZenKeshiType() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 9);
		setIntArray(mode, "zenKeshiType", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(2, getIntArray(mode, "zenKeshiType")[0]);
	}

	@Test
	void onSettingCursor10MaxAttack() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 10);
		setIntArray(mode, "maxAttack", 0, 10);
		// m>=10 -> *10
		pressKeyWith(engine, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		mode.onSetting(engine, 0);
		assertEquals(20, getIntArray(mode, "maxAttack")[0]);
	}

	@Test
	void onSettingCursor10MaxAttackSingleStepWrap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 10);
		setIntArray(mode, "maxAttack", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, getIntArray(mode, "maxAttack")[0]);
	}

	@Test
	void onSettingCursor11NumColorsWrapDown() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 11);
		setIntArray(mode, "numColors", 0, 3);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(5, getIntArray(mode, "numColors")[0]);
	}

	@Test
	void onSettingCursor11NumColorsWrapUp() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 11);
		setIntArray(mode, "numColors", 0, 5);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(3, getIntArray(mode, "numColors")[0]);
	}

	@Test
	void onSettingCursor12RensaShibariWrap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 12);
		setIntArray(mode, "rensaShibari", 0, 1);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(20, getIntArray(mode, "rensaShibari")[0]);
	}

	@Test
	void onSettingCursor13OjamaRate() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 13);
		setIntArray(mode, "ojamaRate", 0, 100);
		// single step adds change*10
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(110, getIntArray(mode, "ojamaRate")[0]);
	}

	@Test
	void onSettingCursor13OjamaRateFastAndWrap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 13);
		setIntArray(mode, "ojamaRate", 0, 1000);
		// m>=10 -> change*100 -> above 1000 wraps to 10
		pressKeyWith(engine, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		mode.onSetting(engine, 0);
		assertEquals(10, getIntArray(mode, "ojamaRate")[0]);
	}

	@Test
	void onSettingCursor14HurryupSingleStep() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 14);
		setIntArray(mode, "hurryupSeconds", 0, 10);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(11, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void onSettingCursor14HurryupFastPathAndWrap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 14);
		setIntArray(mode, "hurryupSeconds", 0, 0);
		// m>10 (1000) -> change*m/10 (negative) -> wraps to 300
		pressKeyWith(engine, Controller.BUTTON_LEFT, Controller.BUTTON_F);
		mode.onSetting(engine, 0);
		assertEquals(300, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void onSettingCursor15OjamaHardWrap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 15);
		setIntArray(mode, "ojamaHard", 0, 9);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "ojamaHard")[0]);
	}

	@Test
	void onSettingCursor16DangerColumnDoubleToggles() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 16);
		boolean before = getBoolArray(mode, "dangerColumnDouble")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "dangerColumnDouble")[0]);
	}

	@Test
	void onSettingCursor17DangerColumnShowXToggles() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 17);
		boolean before = getBoolArray(mode, "dangerColumnShowX")[0];
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "dangerColumnShowX")[0]);
	}

	@Test
	void onSettingCursor18OjamaHandicap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 18);
		setIntArray(mode, "ojamaHandicap", 0, 0);
		// LEFT wraps to 9999
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(9999, getIntArray(mode, "ojamaHandicap")[0]);
	}

	@Test
	void onSettingCursor18OjamaHandicapWrapHigh() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 18);
		setIntArray(mode, "ojamaHandicap", 0, 9999);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "ojamaHandicap")[0]);
	}

	@Test
	void onSettingCursor19FeverMapSetLoadsMap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 19);
		setIntArray(mode, "feverMapSet", 0, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(1, getIntArray(mode, "feverMapSet")[0]);
		// loadMapSetFever populated chain min/max from the loaded map
		assertTrue(getIntArray(mode, "feverChainMax")[0] >= getIntArray(mode, "feverChainMin")[0]);
	}

	@Test
	void onSettingCursor19FeverMapSetWrapDown() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 19);
		setIntArray(mode, "feverMapSet", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(AvalancheVSDummyMode.FEVER_MAPS.length - 1,
				getIntArray(mode, "feverMapSet")[0]);
	}

	@Test
	void onSettingCursor19FeverMapSetWrapUp() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 19);
		setIntArray(mode, "feverMapSet", 0,
				AvalancheVSDummyMode.FEVER_MAPS.length - 1);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "feverMapSet")[0]);
	}

	@Test
	void onSettingCursor20FeverChainStartWraps() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		// Establish min/max via a map load first
		setIntArray(mode, "feverMapSet", 0, 0);
		invokeLoadMapSetFever(mode, engine, 0, 0);
		int min = getIntArray(mode, "feverChainMin")[0];
		int max = getIntArray(mode, "feverChainMax")[0];

		menu(engine, mode, 20);
		setIntArray(mode, "feverChainStart", 0, min);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(max, getIntArray(mode, "feverChainStart")[0]);

		menu(engine, mode, 20);
		setIntArray(mode, "feverChainStart", 0, max);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(min, getIntArray(mode, "feverChainStart")[0]);
	}

	@Test
	void onSettingCursor21OutlineTypeWrap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 21);
		setIntArray(mode, "outlineType", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(2, getIntArray(mode, "outlineType")[0]);
	}

	@Test
	void onSettingCursor22ChainDisplayTypeWrap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 22);
		setIntArray(mode, "chainDisplayType", 0, 4);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "chainDisplayType")[0]);
	}

	@Test
	void onSettingCursor23CascadeSlowToggles() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 23);
		boolean before = getBoolArray(mode, "cascadeSlow")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "cascadeSlow")[0]);
	}

	@Test
	void onSettingCursor24NewChainPowerToggles() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 24);
		boolean before = getBoolArray(mode, "newChainPower")[0];
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "newChainPower")[0]);
	}

	@Test
	void onSettingCursor25BgmnoWrap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 25);
		setFieldInt(mode, "bgmno", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertTrue(readFieldInt(mode, "bgmno") > 0);
	}

	@Test
	void onSettingCursor26EnableSEToggles() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 26);
		boolean before = getBoolArray(mode, "enableSE")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "enableSE")[0]);
	}

	@Test
	void onSettingCursor27BigDisplayToggles() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 27);
		boolean before = readFieldBool(mode, "bigDisplay");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "bigDisplay"));
	}

	@Test
	void onSettingCursor28And29PresetNumber() throws Exception {
		for (int cursor : new int[]{28, 29}) {
			AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			menu(engine, mode, cursor);
			setIntArray(mode, "presetNumber", 0, 0);
			pressKey(engine, Controller.BUTTON_LEFT);
			mode.onSetting(engine, 0);
			assertEquals(99, getIntArray(mode, "presetNumber")[0],
					"Cursor " + cursor + " should wrap presetNumber");
		}
	}

	// ------------------------------------------------------------------
	// onSetting: A-button confirm paths (284-298)
	// ------------------------------------------------------------------

	@Test
	void onSettingPressAAtCursor28LoadsPreset() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 28, 10);
		engine.speed.gravity = 12345;

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// loadPreset overwrote gravity from the (default) preset
		assertNotEquals(12345, engine.speed.gravity,
				"A at cursor 28 should load preset and overwrite gravity");
	}

	@Test
	void onSettingPressAAtCursor29SavesPreset() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 29, 10);
		setIntArray(mode, "presetNumber", 0, 0);
		engine.speed.gravity = 77;

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// savePreset wrote gravity into modeConfig under the fever preset key
		assertEquals(77, engine.owner.modeConfig.getProperty(
				"avalanchevsfever.gravity.0", -1));
	}

	@Test
	void onSettingPressAAtOtherCursorSavesAndAdvances() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 18, 10);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(1, engine.statc[4],
				"A at a config cursor should save settings and set statc[4]=1");
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		menu(engine, mode, 0, 10);

		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	// ------------------------------------------------------------------
	// onSetting: replay auto-advance (305-316) and WAIT branch (317-328)
	// ------------------------------------------------------------------

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;

		setFieldInt(mode, "menuTime", 59);
		mode.onSetting(engine, 0);
		assertEquals(9, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 119);
		mode.onSetting(engine, 0);
		assertEquals(18, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 179);
		mode.onSetting(engine, 0);
		assertEquals(24, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 239);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingWaitBranchCancelResets() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.engine[1].init();
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.statc[4] = 1;
		// Only this player ready -> B cancels back to config
		engine.owner.engine[0].statc[4] = 1;
		engine.owner.engine[1].statc[4] = 0;

		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertEquals(0, engine.statc[4]);
	}

	@Test
	void onSettingWaitBranchBothReadyStartsGame() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		// init both engines
		engine.owner.engine[1].init();
		mode.playerInit(engine.owner.engine[0], 0);
		mode.playerInit(engine.owner.engine[1], 1);

		engine.owner.replayMode = false;
		engine.owner.engine[0].statc[4] = 1;
		engine.owner.engine[1].statc[4] = 1;

		// Called as player 1, both ready -> both engines transition to READY
		mode.onSetting(engine.owner.engine[1], 1);

		assertEquals(GameEngine.Status.READY, engine.owner.engine[0].stat);
		assertEquals(GameEngine.Status.READY, engine.owner.engine[1].stat);
	}

	// ------------------------------------------------------------------
	// renderSetting: 4 pages + WAIT state (338-399)
	// ------------------------------------------------------------------

	@Test
	void renderSettingAllPagesAndWait() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		// Ensure fever map subset data is loaded so page 3 names render
		invokeLoadMapSetFever(mode, engine, 0, 0);

		engine.statc[4] = 0;
		for (int cursor : new int[]{0, 9, 18, 25}) {
			setFieldInt(mode, "menuCursor", cursor);
			mode.renderSetting(engine, 0);
		}
		// FEVERSIZE chain display branch on page 3
		setFieldInt(mode, "menuCursor", 18);
		setIntArray(mode, "chainDisplayType", 0,
				AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE);
		mode.renderSetting(engine, 0);

		// WAIT state
		engine.statc[4] = 1;
		mode.renderSetting(engine, 0);
	}

	// ------------------------------------------------------------------
	// playerInit: replay-mode path (110-113)
	// ------------------------------------------------------------------

	@Test
	void playerInitReplayModeReadsVersion() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = true;
		engine.owner.replayProp = new CustomProperties();
		engine.owner.replayProp.setProperty("avalanchevsfever.version", 1);

		mode.playerInit(engine, 0);

		assertEquals(1, readFieldInt(mode, "version"));
		assertEquals(0, getIntArray(mode, "ojama")[0]);
	}

	// ------------------------------------------------------------------
	// renderLast: color/displaysize/handicap branches (438-496)
	// ------------------------------------------------------------------

	@Test
	void renderLastSmallDisplayWithCounters() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameStarted = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.displaysize = 0;

		setIntArray(mode, "ojama", 0, 15);          // RED ojama color path
		setIntArray(mode, "ojamaAdd", 0, 3);        // "(+3)" suffix
		setIntArray(mode, "ojamaHandicap", 0, 100);
		setIntArray(mode, "ojamaHandicapLeft", 0, 10); // < handicap/4 -> RED
		setIntArray(mode, "ojamaHard", 0, 1);       // drawHardOjama path
		setIntArray(mode, "lastscore", 0, 50);
		setIntArray(mode, "lastmultiplier", 0, 3);
		setIntArray(mode, "scgettime", 0, 5);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastBigDisplayPlayerOne() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.engine[1].init();
		mode.playerInit(engine.owner.engine[0], 0);
		mode.playerInit(engine.owner.engine[1], 1);

		GameEngine e1 = engine.owner.engine[1];
		e1.gameStarted = true;
		e1.stat = GameEngine.Status.RESULT;
		e1.displaysize = 1;

		setIntArray(mode, "ojama", 1, 8);   // ORANGE ojama color path
		setIntArray(mode, "ojamaHandicap", 1, 100);
		setIntArray(mode, "ojamaHandicapLeft", 1, 40); // < handicap/2 -> YELLOW
		setIntArray(mode, "ojamaHard", 1, 0);

		mode.renderLast(e1, 1);
	}

	// ------------------------------------------------------------------
	// lineClearEnd: garbage-drop return path (600-607)
	// ------------------------------------------------------------------

	@Test
	void lineClearEndDropsOjamaReturnsTrue() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine1 = freshEngine(mode);
		engine1.owner.engine[1].init();
		mode.playerInit(engine1, 0);
		mode.playerInit(engine1.owner.engine[1], 1);
		engine1.createFieldIfNeeded();
		engine1.nowPieceObject = new Piece(Piece.PIECE_T);

		setIntArray(mode, "ojama", 0, 20);
		setBoolArray(mode, "ojamaDrop", false, 0);
		setBoolArray(mode, "cleared", false, 0);
		setIntArray(mode, "maxAttack", 0, 10);
		setIntArray(mode, "ojamaHard", 0, 0);
		setIntArray(mode, "ojamaAdd", 0, 1);

		boolean result = mode.lineClearEnd(engine1, 0);

		assertTrue(result, "ojama>0 with no prior drop should trigger garbage drop");
		assertTrue(getIntArray(mode, "ojama")[0] < 20,
				"ojama should decrease after drop");
		assertTrue(getBoolArray(mode, "ojamaDrop")[0]);
	}

	@Test
	void lineClearEndClearResetsFeverBoardClampMin() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.engine[1].init();
		mode.playerInit(engine, 0);
		mode.playerInit(engine.owner.engine[1], 1);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setBoolArray(mode, "cleared", true, 0);
		setIntArray(mode, "feverChain", 0, 4);
		setIntArray(mode, "feverChainMin", 0, 8);   // force clamp to min
		setIntArray(mode, "feverChainMax", 0, 15);
		setIntArray(mode, "ojamaAdd", 0, 1);
		engine.chain = 1; // newFeverChain = max(2, 2) = 2 -> clamped up to min 8

		mode.lineClearEnd(engine, 0);

		assertEquals(8, getIntArray(mode, "feverChain")[0]);
	}

	// ==================================================================
	// Helpers
	// ==================================================================

	private static GameEngine freshEngine(AvalancheVSFeverMode mode) {
		GameManager manager = new GameManager(new RedirectReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void menu(GameEngine engine, AvalancheVSFeverMode mode, int cursor)
			throws Exception {
		menu(engine, mode, cursor, 0);
	}

	private static void menu(GameEngine engine, AvalancheVSFeverMode mode, int cursor,
			int menuTime) throws Exception {
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

	private static void pressKeyWith(GameEngine engine, int btn, int modifier) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
		engine.ctrl.buttonPress[modifier] = true;
		engine.ctrl.buttonTime[modifier] = 1;
	}

	private static void pressPush(GameEngine engine, int btn) {
		pressKey(engine, btn);
	}

	private static void invokeLoadMapSetFever(AvalancheVSFeverMode mode, GameEngine engine,
			int playerID, int id) throws Exception {
		Method m = findMethod(mode.getClass(), "loadMapSetFever",
				GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID, id, true);
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

	private static void setIntArray(Object obj, String name, int index, int value)
			throws Exception {
		((int[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static void setBoolArray(Object obj, String name, boolean value, int index)
			throws Exception {
		((boolean[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				// keep walking up
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static Method findMethod(Class<?> cls, String name, Class<?>... params)
			throws NoSuchMethodException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try {
				return c.getDeclaredMethod(name, params);
			} catch (NoSuchMethodException e) {
				// keep walking up
			}
		}
		throw new NoSuchMethodException(name);
	}

	/**
	 * No-op {@link EventReceiver} that redirects any config/property write
	 * into the JVM temp directory so the production save paths execute
	 * without touching tracked files under config/.
	 */
	private static final class RedirectReceiver extends EventReceiver {
		@Override
		public void saveModeConfig(CustomProperties modeConfig) {
			String tmp = System.getProperty("java.io.tmpdir");
			saveProperties(tmp + "/avalanchevsfever_test_mode.cfg", modeConfig);
		}

		@Override
		public boolean saveProperties(String filename, CustomProperties prop) {
			String tmp = System.getProperty("java.io.tmpdir");
			String redirected = tmp + "/avalanchevsfever_test_" +
					new java.io.File(filename).getName();
			return super.saveProperties(redirected, prop);
		}
	}
}
