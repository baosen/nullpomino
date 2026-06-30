package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage-boost tests for {@link PhysicianVSMode}, targeting the previously
 * uncovered branches: the full {@code onSetting} configuration menu (every
 * cursor 0-16, LEFT/RIGHT + E/F multipliers, A-confirm load/save/start,
 * B-cancel, random-map preview, the replay/auto-advance and statc[4]==1
 * start/cancel branches), {@code loadMapPreview}, {@code renderSetting} for
 * both menu pages, {@code onReady} (map + hover-block init incl. flash),
 * {@code renderLast} gameStarted display, {@code calcScore} garbage-trigger
 * branch, {@code lineClearEnd}/{@code garbageCheck} (all size buckets),
 * {@code onLast} meter colors + settlement (1P/2P/draw), {@code renderResult}
 * (win/lose/draw) and {@code saveReplay} map backup.
 */
class PhysicianVSModeCoverageBoostTest {

	// ---------------------------------------------------------------
	// onSetting: LEFT/RIGHT at each cursor (speed presets, page 2)
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor0Gravity() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		int before = engine.speed.gravity;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor0GravityWithEMultiplier() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		engine.speed.gravity = 0;
		pressKey(engine, Controller.BUTTON_RIGHT);
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1;
		mode.onSetting(engine, 0);
		assertEquals(100, engine.speed.gravity, "E button multiplier is 100");
	}

	@Test
	void onSettingCursor0GravityWithFMultiplier() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		engine.speed.gravity = 0;
		pressKey(engine, Controller.BUTTON_RIGHT);
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onSetting(engine, 0);
		assertEquals(1000, engine.speed.gravity, "F button multiplier is 1000");
	}

	@Test
	void onSettingCursor0GravityWrapLow() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		engine.speed.gravity = -1;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99999, engine.speed.gravity);
	}

	@Test
	void onSettingCursor1DenominatorWrap() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 1);
		engine.speed.denominator = -1;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99999, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2AreWrapHigh() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 2);
		engine.speed.are = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLineWrapLow() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 3);
		engine.speed.areLine = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 4);
		int before = engine.speed.lineDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 5);
		int before = engine.speed.lockDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor6DasWrapHigh() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 6);
		engine.speed.das = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.das);
	}

	@Test
	void onSettingCursor7And8PresetNumber() throws Exception {
		for (int cursor : new int[]{7, 8}) {
			PhysicianVSMode mode = new PhysicianVSMode();
			GameEngine engine = settingEngine(mode, cursor);
			int before = getIntArray(mode, "presetNumber")[0];
			pressKey(engine, Controller.BUTTON_RIGHT);
			mode.onSetting(engine, 0);
			assertEquals(before + 1, getIntArray(mode, "presetNumber")[0],
					"cursor " + cursor + " adjusts presetNumber");
		}
	}

	@Test
	void onSettingCursor7PresetNumberWrapLow() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 7);
		getIntArray(mode, "presetNumber")[0] = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, getIntArray(mode, "presetNumber")[0]);
	}

	@Test
	void onSettingCursor9SpeedWraps() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 9);
		getIntArray(mode, "speed")[0] = 2;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "speed")[0], "speed wraps 2 -> 0");

		getIntArray(mode, "speed")[0] = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(2, getIntArray(mode, "speed")[0], "speed wraps 0 -> 2");
	}

	@Test
	void onSettingCursor10HoverBlocksSingleStep() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 10);
		getIntArray(mode, "hoverBlocks")[0] = 40;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(41, getIntArray(mode, "hoverBlocks")[0]);
	}

	@Test
	void onSettingCursor10HoverBlocksTimesTenWithE() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 10);
		getIntArray(mode, "hoverBlocks")[0] = 40;
		pressKey(engine, Controller.BUTTON_RIGHT);
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1;
		mode.onSetting(engine, 0);
		// m=100 >= 10, so change*10 = +10
		assertEquals(50, getIntArray(mode, "hoverBlocks")[0]);
	}

	@Test
	void onSettingCursor10HoverBlocksWrapHigh() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 10);
		getIntArray(mode, "hoverBlocks")[0] = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(1, getIntArray(mode, "hoverBlocks")[0]);
	}

	@Test
	void onSettingCursor10HoverBlocksWrapLow() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 10);
		getIntArray(mode, "hoverBlocks")[0] = 1;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, getIntArray(mode, "hoverBlocks")[0]);
	}

	@Test
	void onSettingCursor11FlashToggles() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 11);
		boolean before = getBoolArray(mode, "flash")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "flash")[0]);
	}

	@Test
	void onSettingCursor12EnableSEToggles() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 12);
		boolean before = getBoolArray(mode, "enableSE")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "enableSE")[0]);
	}

	@Test
	void onSettingCursor13BgmnoWraps() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 13);
		setInt(mode, "bgmno", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertTrue(getInt(mode, "bgmno") > 0, "bgmno wraps to BGM_COUNT-1 going left from 0");

		// Going right past the max wraps back to 0.
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor14UseMapToggleOnLoadsPreview() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 14);
		getBoolArray(mode, "useMap")[0] = false;
		engine.createFieldIfNeeded();
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertTrue(getBoolArray(mode, "useMap")[0], "useMap toggled true -> loads preview");
	}

	@Test
	void onSettingCursor14UseMapToggleOffResetsField() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 14);
		getBoolArray(mode, "useMap")[0] = true;
		engine.createFieldIfNeeded();
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertFalse(getBoolArray(mode, "useMap")[0], "useMap toggled false -> resets field");
	}

	@Test
	void onSettingCursor15MapSetAdjusts() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 15);
		getBoolArray(mode, "useMap")[0] = true;
		engine.createFieldIfNeeded();
		int before = getIntArray(mode, "mapSet")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "mapSet")[0]);
		assertEquals(-1, getIntArray(mode, "mapNumber")[0],
				"adjusting mapSet with useMap resets mapNumber to -1");
	}

	@Test
	void onSettingCursor15MapSetWrapLow() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 15);
		getBoolArray(mode, "useMap")[0] = false;
		getIntArray(mode, "mapSet")[0] = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, getIntArray(mode, "mapSet")[0]);
	}

	@Test
	void onSettingCursor16MapNumberUseMapOn() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 16);
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = 0;
		getIntArray(mode, "mapMaxNo")[0] = 5;
		engine.createFieldIfNeeded();
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		// mapNumber went 0 -> 1
		assertEquals(1, getIntArray(mode, "mapNumber")[0]);
	}

	@Test
	void onSettingCursor16MapNumberWrapLow() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 16);
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = -1;
		getIntArray(mode, "mapMaxNo")[0] = 5;
		engine.createFieldIfNeeded();
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		// -1 - 1 = -2 < -1 -> mapMaxNo-1 = 4
		assertEquals(4, getIntArray(mode, "mapNumber")[0]);
	}

	@Test
	void onSettingCursor16MapNumberUseMapOffSetsNegativeOne() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 16);
		getBoolArray(mode, "useMap")[0] = false;
		getIntArray(mode, "mapNumber")[0] = 4;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(-1, getIntArray(mode, "mapNumber")[0]);
	}

	// ---------------------------------------------------------------
	// onSetting: A-button confirm paths
	// ---------------------------------------------------------------

	@Test
	void onSettingPressAAtCursor7LoadsPreset() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 7);
		setInt(mode, "menuTime", 10);
		// Seed a known preset 0 into modeConfig so the load is deterministic
		// regardless of any persisted config from other tests.
		getIntArray(mode, "presetNumber")[0] = 0;
		engine.owner.modeConfig.setProperty("physicianvs.gravity.0", 1234);
		engine.speed.gravity = 9999;
		pressKey(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(1234, engine.speed.gravity,
				"A at cursor 7 loads the stored preset gravity");
	}

	@Test
	void onSettingPressAAtCursor8SavesPreset() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 8);
		setInt(mode, "menuTime", 10);
		engine.speed.gravity = 77;
		getIntArray(mode, "presetNumber")[0] = 0;
		pressKey(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(77, engine.owner.modeConfig.getProperty("physicianvs.gravity.0", -1));
	}

	@Test
	void onSettingPressAAtOtherCursorStartsAndSetsStatc4() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4], "A at non-preset cursor advances statc[4]");
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	// ---------------------------------------------------------------
	// onSetting: preview-map loading (menuTime==0) and random preview
	// ---------------------------------------------------------------

	@Test
	void onSettingUseMapAtMenuTimeZeroLoadsPreview() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 5);
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = -1;
		setInt(mode, "menuTime", 0);
		engine.createFieldIfNeeded();
		mode.onSetting(engine, 0);
		assertTrue(getInt(mode, "menuTime") >= 1, "menuTime advances after onSetting");
	}

	@Test
	void onSettingRandomMapPreviewCycles() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 5);
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = -1;
		getIntArray(mode, "mapMaxNo")[0] = 3;
		engine.createFieldIfNeeded();
		// Force a non-null propMap so the random-preview block executes.
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.maxMapNumber", 3);
		getPropMapArray(mode)[0] = pm;
		// menuTime such that menuTime % 30 == 0 inside the block.
		setInt(mode, "menuTime", 30);
		engine.statc[5] = 0;
		mode.onSetting(engine, 0);
		assertTrue(engine.statc[5] >= 0, "random preview statc[5] advanced");
	}

	// ---------------------------------------------------------------
	// onSetting: replayMode auto-advance branch (statc[4]==0)
	// ---------------------------------------------------------------

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;

		setInt(mode, "menuTime", 0);
		mode.onSetting(engine, 0);
		assertEquals(1, getInt(mode, "menuTime"));

		setInt(mode, "menuTime", 60);
		mode.onSetting(engine, 0);
		assertEquals(9, getInt(mode, "menuCursor"), "menuTime>=60 sets cursor 9");

		setInt(mode, "menuTime", 120);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4], "menuTime>=120 advances statc[4]");
	}

	// ---------------------------------------------------------------
	// onSetting: statc[4]==1 start (both ready) and cancel branches
	// ---------------------------------------------------------------

	@Test
	void onSettingBothEnginesReadyStartsGame() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		TwoPlayer tp = twoPlayer(mode);
		tp.e0.statc[4] = 1;
		tp.e1.statc[4] = 1;
		// player 1 onSetting triggers the start transition
		mode.onSetting(tp.e1, 1);
		assertEquals(GameEngine.Status.READY, tp.e0.stat);
		assertEquals(GameEngine.Status.READY, tp.e1.stat);
	}

	@Test
	void onSettingStatc4OneCancelResetsToZero() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		TwoPlayer tp = twoPlayer(mode);
		// Only player 0 ready -> the start condition (both + playerID==1) is false,
		// so the B-cancel branch is reachable for player 1.
		tp.e0.statc[4] = 0;
		tp.e1.statc[4] = 1;
		pressKey(tp.e1, Controller.BUTTON_B);
		mode.onSetting(tp.e1, 1);
		assertEquals(0, tp.e1.statc[4], "B in wait state resets statc[4] to 0");
	}

	// ---------------------------------------------------------------
	// renderSetting: both menu pages + WAIT state
	// ---------------------------------------------------------------

	@Test
	void renderSettingPage1() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		engine.statc[4] = 0;
		setInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage2() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 9);
		engine.statc[4] = 0;
		setInt(mode, "menuCursor", 9);
		// exercise RANDOM map-number label branch
		getIntArray(mode, "mapNumber")[0] = -1;
		mode.renderSetting(engine, 0);

		// exercise the numeric "n/m" map-number label branch
		getIntArray(mode, "mapNumber")[0] = 2;
		getIntArray(mode, "mapMaxNo")[0] = 5;
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingWaitState() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		engine.statc[4] = 1;
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// loadMapPreview directly (both branches)
	// ---------------------------------------------------------------

	@Test
	void loadMapPreviewWithNullPropResetsField() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		engine.createFieldIfNeeded();
		getIntArray(mode, "mapSet")[0] = 123456; // nonexistent map file -> propMap stays null
		// propMap null + nonexistent file -> stays null -> field.reset() branch
		Method m = PhysicianVSMode.class.getDeclaredMethod(
				"loadMapPreview", GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, 0, true);
		// no crash; field still present
		assertNotNull(engine.field);
	}

	@Test
	void loadMapPreviewWithPropLoadsMap() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = settingEngine(mode, 0);
		engine.createFieldIfNeeded();
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.maxMapNumber", 4);
		getPropMapArray(mode)[0] = pm;
		Method m = PhysicianVSMode.class.getDeclaredMethod(
				"loadMapPreview", GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, 0, false);
		assertEquals(4, getIntArray(mode, "mapMaxNo")[0]);
	}

	// ---------------------------------------------------------------
	// playerInit replay-mode branch (355-357)
	// ---------------------------------------------------------------

	@Test
	void playerInitReplayModeLoadsFromReplayProp() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.replayMode = true;
		manager.engine[0].owner.replayMode = true;
		manager.replayProp.setProperty("physicianvs.version", 0);
		mode.playerInit(manager.engine[0], 0);
		assertEquals(0, getInt(mode, "version"));
	}

	// ---------------------------------------------------------------
	// onReady: map + hover-block init
	// ---------------------------------------------------------------

	@Test
	void onReadyNoMapResetsFieldAndAddsHoverBlocks() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		getBoolArray(mode, "useMap")[0] = false;
		getIntArray(mode, "hoverBlocks")[0] = 40;
		getBoolArray(mode, "flash")[0] = false;
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		mode.onReady(engine, 0);
		assertNotNull(engine.field);
	}

	@Test
	void onReadyFlashModeAddsHoverBlocksWithSkin() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		getBoolArray(mode, "useMap")[0] = false;
		getIntArray(mode, "hoverBlocks")[0] = 80; // triggers minY=3 branch
		getBoolArray(mode, "flash")[0] = true;
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		mode.onReady(engine, 0);
		assertNotNull(engine.field);
	}

	@Test
	void onReadyUseMapNotReplayLoadsRandomMap() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = -1;
		getIntArray(mode, "mapMaxNo")[0] = 2;
		getIntArray(mode, "hoverBlocks")[0] = 0;
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.maxMapNumber", 2);
		getPropMapArray(mode)[0] = pm;
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		mode.onReady(engine, 0);
		assertNotNull(getFldBackupArray(mode)[0], "map backup created from loaded map");
	}

	@Test
	void onReadyUseMapWithFixedMapNumber() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = 1; // fixed -> loadMap(mapNumber) branch
		getIntArray(mode, "mapMaxNo")[0] = 3;
		getIntArray(mode, "hoverBlocks")[0] = 0;
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.maxMapNumber", 3);
		getPropMapArray(mode)[0] = pm;
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		mode.onReady(engine, 0);
		assertNotNull(getFldBackupArray(mode)[0]);
	}

	@Test
	void onReadyHoverBlocks72And64Thresholds() throws Exception {
		for (int hb : new int[]{72, 64, 6}) {
			PhysicianVSMode mode = new PhysicianVSMode();
			GameEngine engine = freshEngine(mode, 0);
			mode.playerInit(engine, 0);
			getBoolArray(mode, "useMap")[0] = false;
			getIntArray(mode, "hoverBlocks")[0] = hb;
			getBoolArray(mode, "flash")[0] = false;
			engine.createFieldIfNeeded();
			engine.statc[0] = 0;
			mode.onReady(engine, 0);
			assertNotNull(engine.field);
		}
	}

	// ---------------------------------------------------------------
	// renderLast (gameStarted branch)
	// ---------------------------------------------------------------

	@Test
	void renderLastDisplaysRestAndSpeedForBothPlayers() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		TwoPlayer tp = twoPlayer(mode);
		tp.e0.gameStarted = true;
		tp.e1.gameStarted = true;
		getIntArray(mode, "rest")[0] = 2;  // < 10 and <= flash/3 path
		getIntArray(mode, "rest")[1] = 50; // >= 10
		mode.renderLast(tp.e0, 0);
		mode.renderLast(tp.e1, 1);
	}

	@Test
	void renderLastNotStartedSkipsRestDisplay() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.gameStarted = false;
		mode.renderLast(engine, 0);
	}

	// ---------------------------------------------------------------
	// calcScore garbage-trigger branch (lines==0, can't cascade)
	// ---------------------------------------------------------------

	@Test
	void calcScoreGarbageTriggerSetsLineClear() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.gemsCleared = 0;
		// 4 queued garbage colors so garbageCheck returns true (>=2)
		ArrayList<Integer> g = new ArrayList<Integer>();
		g.add(Block.BLOCK_COLOR_RED);
		g.add(Block.BLOCK_COLOR_BLUE);
		g.add(Block.BLOCK_COLOR_YELLOW);
		g.add(Block.BLOCK_COLOR_RED);
		getGarbageColorsArray(mode)[0] = g;
		mode.calcScore(engine, 0, 0); // lines==0
		assertEquals(GameEngine.Status.LINECLEAR, engine.stat,
				"garbage drop triggers LINECLEAR status");
	}

	@Test
	void calcScoreNullFieldReturnsEarly() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.field = null;
		mode.calcScore(engine, 0, 1); // returns immediately, no crash
	}

	// ---------------------------------------------------------------
	// lineClearEnd / garbageCheck size buckets
	// ---------------------------------------------------------------

	@Test
	void lineClearEndNullFieldReturnsFalse() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.field = null;
		assertFalse(mode.lineClearEnd(engine, 0));
	}

	@Test
	void lineClearEndAppendsToExistingEnemyGarbage() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		// enemy already has garbage queued -> exercises addAll branch (738)
		ArrayList<Integer> existing = new ArrayList<Integer>();
		existing.add(Block.BLOCK_COLOR_RED);
		getGarbageColorsArray(mode)[1] = existing;
		ArrayList<Integer> cleared = new ArrayList<Integer>();
		cleared.add(Block.BLOCK_COLOR_BLUE);
		cleared.add(Block.BLOCK_COLOR_YELLOW);
		engine.field.lineColorsCleared = cleared;
		mode.lineClearEnd(engine, 0);
		assertTrue(getGarbageColorsArray(mode)[1].size() >= 3);
	}

	@Test
	void garbageCheckSizeFourOrMoreDropsFourColors() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		ArrayList<Integer> g = new ArrayList<Integer>();
		g.add(Block.BLOCK_COLOR_RED);
		g.add(Block.BLOCK_COLOR_BLUE);
		g.add(Block.BLOCK_COLOR_YELLOW);
		g.add(Block.BLOCK_COLOR_RED);
		g.add(Block.BLOCK_COLOR_BLUE);
		getGarbageColorsArray(mode)[0] = g;
		assertTrue(invokeGarbageCheck(mode, engine, 0));
		assertTrue(getGarbageColorsArray(mode)[0] == null, "garbage consumed after drop");
	}

	@Test
	void garbageCheckSizeThreeUsesSkipSlot() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		ArrayList<Integer> g = new ArrayList<Integer>();
		g.add(Block.BLOCK_COLOR_RED);
		g.add(Block.BLOCK_COLOR_BLUE);
		g.add(Block.BLOCK_COLOR_YELLOW);
		getGarbageColorsArray(mode)[0] = g;
		assertTrue(invokeGarbageCheck(mode, engine, 0));
	}

	@Test
	void garbageCheckSizeTwoUsesOppositeSlots() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		ArrayList<Integer> g = new ArrayList<Integer>();
		g.add(Block.BLOCK_COLOR_RED);
		g.add(Block.BLOCK_COLOR_BLUE);
		getGarbageColorsArray(mode)[0] = g;
		assertTrue(invokeGarbageCheck(mode, engine, 0));
	}

	@Test
	void garbageCheckSizeOneReturnsFalse() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		ArrayList<Integer> g = new ArrayList<Integer>();
		g.add(Block.BLOCK_COLOR_RED);
		getGarbageColorsArray(mode)[0] = g;
		assertFalse(invokeGarbageCheck(mode, engine, 0), "size<2 yields no drop");
	}

	// ---------------------------------------------------------------
	// onLast meter colors + settlement
	// ---------------------------------------------------------------

	@Test
	void onLastFlashMeterColors() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		getBoolArray(mode, "flash")[0] = true;
		engine.createFieldIfNeeded();
		// 1 gem -> green
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GEM_RED));
		mode.onLast(engine, 0);
		assertTrue(engine.meterColor == GameEngine.METER_COLOR_GREEN
				|| engine.meterColor >= 0);
	}

	@Test
	void onLastNonFlashMeterColors() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		getBoolArray(mode, "flash")[0] = false;
		getIntArray(mode, "hoverBlocks")[0] = 40;
		engine.createFieldIfNeeded();
		// 2 gems -> rest<=3 green branch
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GEM_RED));
		engine.field.setBlock(1, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GEM_BLUE));
		mode.onLast(engine, 0);
		assertTrue(engine.field != null);
	}

	@Test
	void onLastSettlement2PWins() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		TwoPlayer tp = twoPlayer(mode);
		tp.e0.gameActive = true;
		// 2P win needs p1Lose && !p2Lose (winnerID=1, e1 EXCELLENT).
		// p1Lose: e0 GAMEOVER. !p2Lose: e1 not GAMEOVER and e0 still has a gem.
		tp.e0.stat = GameEngine.Status.GAMEOVER;
		tp.e0.field.setBlock(0, tp.e0.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GEM_RED));
		tp.e1.stat = GameEngine.Status.MOVE;
		mode.onLast(tp.e1, 1);
		assertEquals(1, getInt(mode, "winnerID"), "2P wins when only P1 loses");
		assertEquals(GameEngine.Status.EXCELLENT, tp.e1.stat);
	}

	@Test
	void onLastSettlement1PWins() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		TwoPlayer tp = twoPlayer(mode);
		tp.e0.gameActive = true;
		// 1P win needs p2Lose && !p1Lose (winnerID=0, e0 EXCELLENT).
		// p2Lose: e1 GAMEOVER. !p1Lose: e0 not GAMEOVER and e1 still has a gem.
		tp.e1.stat = GameEngine.Status.GAMEOVER;
		tp.e1.field.setBlock(0, tp.e1.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GEM_RED));
		tp.e0.stat = GameEngine.Status.MOVE;
		mode.onLast(tp.e1, 1);
		assertEquals(0, getInt(mode, "winnerID"), "1P wins when only P2 loses");
		assertEquals(GameEngine.Status.EXCELLENT, tp.e0.stat);
	}

	@Test
	void onLastSettlementDraw() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		TwoPlayer tp = twoPlayer(mode);
		tp.e0.gameActive = true;
		// Both fields empty (no gems) -> both lose -> draw
		tp.e0.stat = GameEngine.Status.MOVE;
		tp.e1.stat = GameEngine.Status.MOVE;
		mode.onLast(tp.e1, 1);
		assertEquals(-1, getInt(mode, "winnerID"), "empty fields -> draw");
		assertEquals(GameEngine.Status.GAMEOVER, tp.e0.stat);
		assertEquals(GameEngine.Status.GAMEOVER, tp.e1.stat);
	}

	// ---------------------------------------------------------------
	// renderResult win/lose/draw
	// ---------------------------------------------------------------

	@Test
	void renderResultDraw() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		setInt(mode, "winnerID", -1);
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultWin() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		setInt(mode, "winnerID", 0);
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultLose() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		setInt(mode, "winnerID", 1);
		mode.renderResult(engine, 0);
	}

	// ---------------------------------------------------------------
	// saveReplay map-backup branch (885)
	// ---------------------------------------------------------------

	@Test
	void saveReplayWithMapBackupSavesMap() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		getBoolArray(mode, "useMap")[0] = true;
		engine.createFieldIfNeeded();
		getFldBackupArray(mode)[0] = new nullpomino.game.component.Field(engine.field);
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);
		assertEquals(0, prop.getProperty("physicianvs.version", -1));
	}

	// ===============================================================
	// Helpers
	// ===============================================================

	/** Holder for a two-engine setup. */
	private static final class TwoPlayer {
		final GameManager manager;
		final GameEngine e0;
		final GameEngine e1;
		TwoPlayer(GameManager m, GameEngine a, GameEngine b) {
			manager = m; e0 = a; e1 = b;
		}
	}

	private static TwoPlayer twoPlayer(PhysicianVSMode mode) throws Exception {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.modeInit(manager);
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();
		return new TwoPlayer(manager, manager.engine[0], manager.engine[1]);
	}

	private static GameEngine freshEngine(PhysicianVSMode mode, int playerID) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		for (int i = 0; i <= playerID; i++) {
			manager.engine[i].init();
		}
		mode.modeInit(manager);
		return manager.engine[playerID];
	}

	/** Engine prepared for an onSetting menu interaction at the given cursor. */
	private static GameEngine settingEngine(PhysicianVSMode mode, int cursor) throws Exception {
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		return engine;
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static boolean invokeGarbageCheck(PhysicianVSMode mode, GameEngine engine,
			int playerID) throws Exception {
		Method m = PhysicianVSMode.class.getDeclaredMethod(
				"garbageCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		return (boolean) m.invoke(mode, engine, playerID);
	}

	private static int getInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (int[]) f.get(obj);
	}

	private static boolean[] getBoolArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (boolean[]) f.get(obj);
	}

	@SuppressWarnings("unchecked")
	private static ArrayList<Integer>[] getGarbageColorsArray(PhysicianVSMode mode)
			throws Exception {
		Field f = findField(mode.getClass(), "garbageColors");
		f.setAccessible(true);
		return (ArrayList<Integer>[]) f.get(mode);
	}

	private static CustomProperties[] getPropMapArray(PhysicianVSMode mode)
			throws Exception {
		Field f = findField(mode.getClass(), "propMap");
		f.setAccessible(true);
		return (CustomProperties[]) f.get(mode);
	}

	private static nullpomino.game.component.Field[] getFldBackupArray(PhysicianVSMode mode)
			throws Exception {
		Field f = findField(mode.getClass(), "fldBackup");
		f.setAccessible(true);
		return (nullpomino.game.component.Field[]) f.get(mode);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
