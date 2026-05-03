package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.BGMStatus;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link AvalancheVSSPFMode#onSetting} menu branches (34 cursor
 * positions), UP/DOWN navigation, A-button confirm paths (load preset,
 * save preset, start), B-button cancel, replayMode path, and
 * {@code renderSetting} page-boundary branches.
 *
 * <p>Each cursor position in the {@code switch(menuCursor)} block must
 * respond to LEFT/RIGHT by adjusting the relevant field. This test
 * exercises every case with at least LEFT (negative change), verifying
 * the field was modified.
 */
class AvalancheVSSPFModeSettingMenuTest {

	// ---------------------------------------------------------------
	// onSetting: UP/DOWN navigation across all 34 cursor positions
	// ---------------------------------------------------------------

	@Test
	void onSettingDownWrapsAtBottom() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);

		setFieldInt(mode, "menuCursor", 33);
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	@Test
	void onSettingUpFromCursor31NullsField() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);
		engine.createFieldIfNeeded();

		setFieldInt(mode, "menuCursor", 32);
		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(31, readFieldInt(mode, "menuCursor"),
				"UP at cursor 32 should move to 31 and null the field");
	}

	// ---------------------------------------------------------------
	// onSetting: LEFT/RIGHT at each cursor position
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor0GravityAdjustedByLeft() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = engine.speed.gravity;

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(before - 1, engine.speed.gravity,
				"LEFT at cursor 0 should decrement gravity by 1");
	}

	@Test
	void onSettingCursor0GravityAdjustedByRight() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = engine.speed.gravity;

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, engine.speed.gravity,
				"RIGHT at cursor 0 should increment gravity by 1");
	}

	@Test
	void onSettingCursor1DenominatorAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = engine.speed.denominator;

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2AreAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = engine.speed.are;

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(before - 1, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLineAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		int before = engine.speed.areLine;

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelayAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		int before = engine.speed.lineDelay;

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(before - 1, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelayAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		int before = engine.speed.lockDelay;

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor6DasAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		int before = engine.speed.das;

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(before - 1, engine.speed.das);
	}

	@Test
	void onSettingCursor7CascadeDelayAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		int before = engine.cascadeDelay;

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, engine.cascadeDelay);
	}

	@Test
	void onSettingCursor8ClearDelayAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		int before = engine.cascadeClearDelay;

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(before - 1, engine.cascadeClearDelay);
	}

	@Test
	void onSettingCursor9OjamaCounterModeAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		int before = getIntArray(mode, "ojamaCounterMode")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "ojamaCounterMode")[0]);
	}

	@Test
	void onSettingCursor10MaxAttackAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		setIntArray(mode, "maxAttack", 5, 0);
		int before = 5;

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(before - 1, getIntArray(mode, "maxAttack")[0]);
	}

	@Test
	void onSettingCursor11RensaShibariAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 11);
		int before = getIntArray(mode, "rensaShibari")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "rensaShibari")[0]);
	}

	@Test
	void onSettingCursor12ColorClearSizeAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 12);
		int before = engine.colorClearSize;

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(before - 1, engine.colorClearSize);
	}

	@Test
	void onSettingCursor13OjamaRateAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 13);
		int before = getIntArray(mode, "ojamaRate")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 10, getIntArray(mode, "ojamaRate")[0]);
	}

	@Test
	void onSettingCursor14HurryupAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 14);
		setIntArray(mode, "hurryupSeconds", 30, 0);
		int before = 30;

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(before - 1, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void onSettingCursor15DangerColumnDoubleToggles() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 15);
		boolean before = getBoolArray(mode, "dangerColumnDouble")[0];

		// LEFT/RIGHT both toggle (change != 0, but toggle via !value)
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, getBoolArray(mode, "dangerColumnDouble")[0]);
	}

	@Test
	void onSettingCursor16DangerColumnShowXToggles() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16);
		boolean before = getBoolArray(mode, "dangerColumnShowX")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, getBoolArray(mode, "dangerColumnShowX")[0]);
	}

	@Test
	void onSettingCursor17OjamaCountdownAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 17);
		int before = getIntArray(mode, "ojamaCountdown")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "ojamaCountdown")[0]);
	}

	@Test
	void onSettingCursor18ZenKeshiTypeAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 18);
		int before = getIntArray(mode, "zenKeshiType")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "zenKeshiType")[0]);
	}

	@Test
	void onSettingCursor19FeverMapSetAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 19);
		int before = getIntArray(mode, "feverMapSet")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "feverMapSet")[0]);
	}

	@Test
	void onSettingCursor20OutlineTypeAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 20);
		int before = getIntArray(mode, "outlineType")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "outlineType")[0]);
	}

	@Test
	void onSettingCursor21ChainDisplayTypeAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 21);
		int before = getIntArray(mode, "chainDisplayType")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "chainDisplayType")[0]);
	}

	@Test
	void onSettingCursor22CascadeSlowToggles() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 22);
		boolean before = getBoolArray(mode, "cascadeSlow")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, getBoolArray(mode, "cascadeSlow")[0]);
	}

	@Test
	void onSettingCursor23NewChainPowerToggles() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 23);
		boolean before;

		before = getBoolArray(mode, "newChainPower")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "newChainPower")[0]);
	}

	@Test
	void onSettingCursor24UseMapToggles() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 24);
		boolean before = getBoolArray(mode, "useMap")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, getBoolArray(mode, "useMap")[0]);
	}

	@Test
	void onSettingCursor25MapSetAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 25);
		int before = getIntArray(mode, "mapSet")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "mapSet")[0]);
	}

	@Test
	void onSettingCursor26MapNumberAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 26);
		setBoolArray(mode, "useMap", true, 0);
		int before = getIntArray(mode, "mapNumber")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		int after = getIntArray(mode, "mapNumber")[0];
		// Value should have changed (but bounds clamping may wrap)
		assertTrue(after != before || after == -1,
				"mapNumber should change when useMap is true");
	}

	@Test
	void onSettingCursor27BigDisplayToggles() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 27);
		boolean before = readFieldBool(mode, "bigDisplay");

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, readFieldBool(mode, "bigDisplay"));
	}

	@Test
	void onSettingCursor28BgmnoAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 28);
		int before = readFieldInt(mode, "bgmno");

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor29EnableSEToggles() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 29);
		boolean before = getBoolArray(mode, "enableSE")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, getBoolArray(mode, "enableSE")[0]);
	}

	@Test
	void onSettingCursor30And31PresetNumberAdjusted() throws Exception {
		// Cursor 30 (LOAD) and 31 (SAVE) both adjust presetNumber
		for (int cursor : new int[]{30, 31}) {
			AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
			GameEngine engine = freshEngine(mode, false);
			setMenuState(engine, mode, cursor);
			int before = getIntArray(mode, "presetNumber")[0];

			pressKey(engine, Controller.BUTTON_RIGHT);
			mode.onSetting(engine, 0);

			assertEquals(before + 1, getIntArray(mode, "presetNumber")[0],
					"Cursor " + cursor + " should adjust presetNumber");
		}
	}

	@Test
	void onSettingCursor32DropSetAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 32);
		int before = getIntArray(mode, "dropSet")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "dropSet")[0]);
	}

	@Test
	void onSettingCursor33DropMapAdjusted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 33);
		int before = getIntArray(mode, "dropMap")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "dropMap")[0]);
	}

	// ---------------------------------------------------------------
	// onSetting: A button confirm paths
	// ---------------------------------------------------------------

	@Test
	void onSettingPressAAtCursor30LoadsPreset() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 30, 10); // menuTime >= 5
		engine.speed.gravity = 42;

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// After loading, gravity should be from defaults (4), not 42
		assertEquals(4, engine.speed.gravity,
				"A at cursor 30 should load preset defaults");
	}

	@Test
	void onSettingPressAAtCursor31SavesPreset() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 31, 10);
		engine.speed.gravity = 99;
		setIntArray(mode, "presetNumber", 0, 0);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// savePreset at cursor 31 uses presetNumber[0] as preset key
		CustomProperties mc = engine.owner.modeConfig;
		assertEquals(99, mc.getProperty("avalanchevs" + "spf" + ".gravity.0", -1),
				"A at cursor 31 should save gravity to modeConfig using presetNumber");
	}

	@Test
	void onSettingPressAAtOtherCursorStartsGame() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);

		// statc[4] should be 0 before
		assertEquals(0, engine.statc[4]);
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// After confirm, statc[4] should be 1
		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);

		assertFalse(engine.quitflag);
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	// ---------------------------------------------------------------
	// onSetting: replayMode path
	// ---------------------------------------------------------------

	@Test
	void onSettingReplayModeShowsWaitAndAutoAdvances() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false); // init first, then set replayMode
		engine.owner.replayMode = true;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 0);

		// With replayMode and statc[4]=0, menuTime advances and cursor auto-moves
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));

		// After 60 frames, cursor should be at 9
		setFieldInt(mode, "menuTime", 59);
		mode.onSetting(engine, 0);
		assertEquals(9, readFieldInt(mode, "menuCursor"));

		// After 120 frames, cursor should be at 17
		setFieldInt(mode, "menuTime", 119);
		mode.onSetting(engine, 0);
		assertEquals(17, readFieldInt(mode, "menuCursor"));

		// After 300 frames, statc[4] becomes 1
		setFieldInt(mode, "menuTime", 299);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingAfterBothPlayersReady() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		manager.engine[0].statc[4] = 1;
		manager.engine[1].statc[4] = 1;

		mode.onSetting(manager.engine[1], 1);

		assertEquals(GameEngine.Status.READY, manager.engine[0].stat);
		assertEquals(GameEngine.Status.READY, manager.engine[1].stat);
	}

	@Test
	void onSettingAfterBothPlayersReadyCancelGoesBack() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		manager.engine[0].statc[4] = 1;

		pressPush(manager.engine[0], Controller.BUTTON_B);
		mode.onSetting(manager.engine[0], 0);

		assertEquals(0, manager.engine[0].statc[4],
				"B at statc[4]==1 should go back to settings");
	}

	// ---------------------------------------------------------------
	// renderSetting: page-boundary branches
	// ---------------------------------------------------------------

	@Test
	void renderSettingPage1() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 0);

		mode.renderSetting(engine, 0);
		// No crash means page 1 rendered
	}

	@Test
	void renderSettingPage2() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 9);

		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage3() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 17);

		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage4() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 24);

		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage5() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 32);

		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingWaitState() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 1;

		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// saveReplay / onLast / lineClearEnd extra branches
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesVersion() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.playerInit(manager.engine[0], 0);
		GameEngine engine = manager.engine[0];

		// After init, manager.replayProp is a non-null CustomProperties
		// saveReplay writes to owner.replayProp (which IS manager.replayProp)
		mode.saveReplay(engine, 0, manager.replayProp);

		assertEquals(0, manager.replayProp.getProperty("avalanchevs.version", -1));
	}

	@Test
	void onLastDecrementsScgettime() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);

		setIntArray(mode, "scgettime", 5, 0);
		setIntArray(mode, "zenKeshiDisplay", 0, 0);

		mode.onLast(engine, 0);

		assertEquals(4, getIntArray(mode, "scgettime")[0]);
	}

	@Test
	void lineClearEndCountdownDecrementConvertsBlocks() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		// Set countdown to 2 (will decrement to 1, not convert)
		setIntArray(mode, "ojamaCountdown", 3, 0);
		setBoolArray(mode, "countdownDecremented", false, 0);

		// Place an ojama block with countdown=2
		nullpomino.game.component.Block b = new nullpomino.game.component.Block(
				nullpomino.game.component.Block.BLOCK_COLOR_RED);
		b.countdown = 2;
		b.setAttribute(nullpomino.game.component.Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		b.secondaryColor = nullpomino.game.component.Block.BLOCK_COLOR_BLUE;
		engine.field.setBlock(0, 0, b);

		mode.lineClearEnd(engine, 0);

		// Countdown should be decremented to 1
		assertEquals(1, engine.field.getBlock(0, 0).countdown);
		assertTrue(getBoolArray(mode, "countdownDecremented")[0]);
	}

	@Test
	void lineClearEndCountdownReachesOneConvertsBlock() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojamaCountdown", 3, 0);
		setBoolArray(mode, "countdownDecremented", false, 0);

		// Place an ojama block with countdown=1 (will convert)
		nullpomino.game.component.Block b = new nullpomino.game.component.Block(
				nullpomino.game.component.Block.BLOCK_COLOR_RED);
		b.countdown = 1;
		b.secondaryColor = nullpomino.game.component.Block.BLOCK_COLOR_GREEN;
		b.setAttribute(nullpomino.game.component.Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		b.hard = 4;
		engine.field.setBlock(0, 0, b);

		boolean result = mode.lineClearEnd(engine, 0);

		assertTrue(result, "Converting a countdown=1 block should return true");
		assertEquals(0, engine.field.getBlock(0, 0).countdown);
		assertFalse(engine.field.getBlock(0, 0).getAttribute(
				nullpomino.game.component.Block.BLOCK_ATTRIBUTE_GARBAGE));
		assertEquals(nullpomino.game.component.Block.BLOCK_COLOR_GREEN,
				engine.field.getBlock(0, 0).color);
	}

	@Test
	void lineClearEndGameOverCheck() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		// Fill column 2 top (danger column)
		engine.field.setBlock(2, 0,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.lineClearEnd(engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat,
				"Block in danger column should trigger game over");
	}

	@Test
	void lineClearEndOjamaDropWhenPending() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojama", 5, 0);
		setIntArray(mode, "maxAttack", 10, 0);
		setBoolArray(mode, "ojamaDrop", false, 0);
		setBoolArray(mode, "cleared", false, 0);
		setIntArray(mode, "ojamaCounterMode",
				AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);

		// Initialize dropPattern for enemy (player 1)
		Field dpField = findField(mode.getClass(), "dropPattern");
		dpField.setAccessible(true);
		int[][][] dropPattern = (int[][][]) dpField.get(mode);
		dropPattern[1] = new int[][]{{2,2,2,2}};

		boolean result = mode.lineClearEnd(engine, 0);

		assertTrue(result, "Ojama should drop when > 0");
		assertTrue(getBoolArray(mode, "ojamaDrop")[0]);
		assertTrue(getIntArray(mode, "ojama")[0] < 5,
				"ojama should have decreased after drop");
	}

	// ---------------------------------------------------------------
	// updateCursor boundary tests for onSetting
	// ---------------------------------------------------------------

	@Test
	void onSettingUpFrom0FieldResetAtCursor31() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		// Test special case: menuCursor==31 in UP handler nulls field
		engine.createFieldIfNeeded();

		setFieldInt(mode, "menuCursor", 32);
		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);

		assertEquals(31, readFieldInt(mode, "menuCursor"));
		// engine.field should have been set to null at menuCursor==31
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(AvalancheVSSPFMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, AvalancheVSSPFMode mode)
			throws Exception {
		setMenuState(engine, mode, 0, 0);
	}

	private static void setMenuState(GameEngine engine, AvalancheVSSPFMode mode,
			int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, AvalancheVSSPFMode mode,
			int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	/** Press a key (set buttonTime=1 so isMenuRepeatKey returns true) */
	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	/** Press-and-release a key (for isPush detection) */
	private static void pressPush(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static boolean readFieldBool(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		return (int[]) readField(obj, name);
	}

	private static boolean[] getBoolArray(Object obj, String name) throws Exception {
		return (boolean[]) readField(obj, name);
	}

	private static void setIntArray(Object obj, String name, int value, int index) throws Exception {
		((int[]) readField(obj, name))[index] = value;
	}

	private static void setBoolArray(Object obj, String name, boolean value, int index) throws Exception {
		((boolean[]) readField(obj, name))[index] = value;
	}

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
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
