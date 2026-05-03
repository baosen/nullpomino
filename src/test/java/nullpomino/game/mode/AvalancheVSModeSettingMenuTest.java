package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link AvalancheVSMode#onSetting} menu branches (44 normal
 * cursor positions), UP/DOWN navigation, A-button confirm paths (load
 * preset, save preset, start, cheat code), B-button cancel, replayMode
 * path, renderSetting page boundaries, lineClearEnd fever/ojama/game-over
 * branches, onLast fever meter logic, addOjama, and saveReplay.
 */
class AvalancheVSModeSettingMenuTest {

	// ---------------------------------------------------------------
	// onSetting: UP/DOWN navigation (44 positions)
	// ---------------------------------------------------------------

	@Test
	void onSettingUpNavigatesThroughAllPositions() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);

		// Navigate up from 0 to 43
		setFieldInt(mode, "menuCursor", 0);
		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(43, readFieldInt(mode, "menuCursor"),
				"UP at cursor 0 should wrap to 43");

		// Press DOWN to go to 0, then navigate down through all positions
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"First DOWN from 43 should reach 0");

		for (int expected = 1; expected <= 43; expected++) {
			pressKey(engine, Controller.BUTTON_DOWN);
			mode.onSetting(engine, 0);
			assertEquals(expected, readFieldInt(mode, "menuCursor"),
					"Should be at cursor " + expected);
		}
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"DOWN from 43 should wrap to 0");
	}

	@Test
	void onSettingDownWrapsAtBottom() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);

		setFieldInt(mode, "menuCursor", 43);
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	// ---------------------------------------------------------------
	// onSetting: LEFT/RIGHT at each cursor position
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor0Gravity() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = engine.speed.gravity;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(before - 1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor1Denominator() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = engine.speed.denominator;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2Are() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = engine.speed.are;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLine() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		int before = engine.speed.areLine;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		int before = engine.speed.lineDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		int before = engine.speed.lockDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor6Das() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		int before = engine.speed.das;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.das);
	}

	@Test
	void onSettingCursor7CascadeDelay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		int before = engine.cascadeDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.cascadeDelay);
	}

	@Test
	void onSettingCursor8ClearDelay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		int before = engine.cascadeClearDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.cascadeClearDelay);
	}

	@Test
	void onSettingCursor9OjamaCounterMode() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		int before = getIntArray(mode, "ojamaCounterMode")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "ojamaCounterMode")[0]);
	}

	@Test
	void onSettingCursor10MaxAttack() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		int before = getIntArray(mode, "maxAttack")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "maxAttack")[0]);
	}

	@Test
	void onSettingCursor11NumColors() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 11);
		setIntArray(mode, "numColors", 4, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(5, getIntArray(mode, "numColors")[0]);
	}

	@Test
	void onSettingCursor12RensaShibari() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 12);
		int before = getIntArray(mode, "rensaShibari")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "rensaShibari")[0]);
	}

	@Test
	void onSettingCursor13ColorClearSize() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 13);
		int before = engine.colorClearSize;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.colorClearSize);
	}

	@Test
	void onSettingCursor14OjamaRate() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 14);
		int before = getIntArray(mode, "ojamaRate")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 10, getIntArray(mode, "ojamaRate")[0]);
	}

	@Test
	void onSettingCursor15Hurryup() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 15);
		int before = getIntArray(mode, "hurryupSeconds")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void onSettingCursor16NewChainPowerToggles() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16);
		boolean before = getBoolArray(mode, "newChainPower")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "newChainPower")[0]);
	}

	@Test
	void onSettingCursor17OutlineType() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 17);
		int before = getIntArray(mode, "outlineType")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "outlineType")[0]);
	}

	@Test
	void onSettingCursor18ChainDisplayType() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 18);
		int before = getIntArray(mode, "chainDisplayType")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "chainDisplayType")[0]);
	}

	@Test
	void onSettingCursor19CascadeSlowToggles() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 19);
		boolean before = getBoolArray(mode, "cascadeSlow")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "cascadeSlow")[0]);
	}

	@Test
	void onSettingCursor20BigToggles() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 20);
		boolean before = getBoolArray(mode, "big")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "big")[0]);
	}

	@Test
	void onSettingCursor21OjamaHard() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 21);
		int before = getIntArray(mode, "ojamaHard")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "ojamaHard")[0]);
	}

	@Test
	void onSettingCursor22DangerColumnDoubleToggles() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 22);
		boolean before = getBoolArray(mode, "dangerColumnDouble")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "dangerColumnDouble")[0]);
	}

	@Test
	void onSettingCursor23DangerColumnShowXToggles() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 23);
		boolean before = getBoolArray(mode, "dangerColumnShowX")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "dangerColumnShowX")[0]);
	}

	@Test
	void onSettingCursor24ZenKeshiType() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 24);
		int before = getIntArray(mode, "zenKeshiType")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "zenKeshiType")[0]);
	}

	@Test
	void onSettingCursor25ZenKeshiBonus() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 25);
		// Default zenKeshiType is 1 (ON), so this adjusts zenKeshiOjama
		int before = getIntArray(mode, "zenKeshiOjama")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "zenKeshiOjama")[0]);
	}

	@Test
	void onSettingCursor26FeverThreshold() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 26);
		int before = getIntArray(mode, "feverThreshold")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "feverThreshold")[0]);
	}

	@Test
	void onSettingCursor27FeverMapSet() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 27);
		int before = getIntArray(mode, "feverMapSet")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "feverMapSet")[0]);
	}

	@Test
	void onSettingCursor28FeverTimeMin() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 28);
		int before = getIntArray(mode, "feverTimeMin")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "feverTimeMin")[0]);
	}

	@Test
	void onSettingCursor29FeverTimeMax() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 29);
		int before = getIntArray(mode, "feverTimeMax")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "feverTimeMax")[0]);
	}

	@Test
	void onSettingCursor30FeverShowMeterToggles() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 30);
		boolean before = getBoolArray(mode, "feverShowMeter")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "feverShowMeter")[0]);
	}

	@Test
	void onSettingCursor31FeverPointCriteria() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 31);
		int before = getIntArray(mode, "feverPointCriteria")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "feverPointCriteria")[0]);
	}

	@Test
	void onSettingCursor32FeverTimeCriteria() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 32);
		int before = getIntArray(mode, "feverTimeCriteria")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "feverTimeCriteria")[0]);
	}

	@Test
	void onSettingCursor33FeverPower() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 33);
		int before = getIntArray(mode, "feverPower")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "feverPower")[0]);
	}

	@Test
	void onSettingCursor34FeverChainStart() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 34);
		// feverChainStart will be clamped to feverChainMin..feverChainMax range
		// which is 0..0 since no fever map is loaded. So the value won't change.
		// Just verify it doesn't crash.
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		// No exception means it worked
	}

	@Test
	void onSettingCursor35OjamaMeterToggle() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 35);
		boolean before = getBoolArray(mode, "ojamaMeter")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		// With feverThreshold=0: ojamaMeter[0] = (false || !ojamaMeter[0]) = true
		// so it should always be true
		assertTrue(getBoolArray(mode, "ojamaMeter")[0]);
	}

	@Test
	void onSettingCursor36UseMapToggles() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 36);
		boolean before = getBoolArray(mode, "useMap")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "useMap")[0]);
	}

	@Test
	void onSettingCursor37MapSet() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 37);
		int before = getIntArray(mode, "mapSet")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "mapSet")[0]);
	}

	@Test
	void onSettingCursor38MapNumber() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 38);
		int before = getIntArray(mode, "mapNumber")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertTrue(getIntArray(mode, "mapNumber")[0] != before,
				"mapNumber should change when RIGHT is pressed");
	}

	@Test
	void onSettingCursor39Bgmno() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 39);
		int before = readFieldInt(mode, "bgmno");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor40EnableSEToggles() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 40);
		boolean before = getBoolArray(mode, "enableSE")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "enableSE")[0]);
	}

	@Test
	void onSettingCursor41BigDisplayToggles() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 41);
		boolean before = readFieldBool(mode, "bigDisplay");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "bigDisplay"));
	}

	@Test
	void onSettingCursor42And43PresetNumber() throws Exception {
		for (int cursor : new int[]{42, 43}) {
			AvalancheVSMode m = new AvalancheVSMode();
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
	// A button paths
	// ---------------------------------------------------------------

	@Test
	void onSettingPressAAtCursor42LoadsPreset() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 42, 10);
		engine.speed.gravity = 42;

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(4, engine.speed.gravity,
				"A at cursor 42 should load preset defaults");
	}

	@Test
	void onSettingPressAAtCursor43SavesPreset() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 43, 10);
		engine.speed.gravity = 99;
		setIntArray(mode, "presetNumber", 0, 0);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(99, engine.owner.modeConfig.getProperty(
				"avalanchevs.gravity.0", -1));
	}

	@Test
	void onSettingPressAAtOtherCursorStartsGame() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
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
	void onSettingReplayModeAutoAdvances() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 0);

		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));

		setFieldInt(mode, "menuTime", 59);
		mode.onSetting(engine, 0);
		assertEquals(9, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 119);
		mode.onSetting(engine, 0);
		assertEquals(17, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 179);
		mode.onSetting(engine, 0);
		assertEquals(26, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 239);
		mode.onSetting(engine, 0);
		assertEquals(36, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 299);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4]);
	}

	// ---------------------------------------------------------------
	// renderSetting page boundaries
	// ---------------------------------------------------------------

	@Test
	void renderSettingPage1() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage2() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 9);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage3() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 17);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage4() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 26);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage5() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 36);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingWaitState() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 1;
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// lineClearEnd fever/ojama/game-over branches
	// ---------------------------------------------------------------

	@Test
	void lineClearEndTransfersOjamaAdd() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojamaAdd", 5, 1);
		mode.lineClearEnd(engine, 0);

		assertEquals(5, getIntArray(mode, "ojama")[1]);
		assertEquals(0, getIntArray(mode, "ojamaAdd")[1]);
	}

	@Test
	void lineClearEndGameOver() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();
		engine.field.setBlock(2, 0,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.lineClearEnd(engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	@Test
	void lineClearEndOjamaDrop() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojama", 5, 0);
		setIntArray(mode, "maxAttack", 10, 0);
		setBoolArray(mode, "ojamaDrop", false, 0);
		setBoolArray(mode, "cleared", false, 0);
		setIntArray(mode, "ojamaCounterMode",
				AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);

		boolean result = mode.lineClearEnd(engine, 0);

		assertTrue(result, "Ojama > 0 should trigger drop");
	}

	// ---------------------------------------------------------------
	// onLast: fever / meter
	// ---------------------------------------------------------------

	@Test
	void onLastDecrementsScgettime() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);

		setIntArray(mode, "scgettime", 3, 0);
		mode.onLast(engine, 0);
		assertEquals(2, getIntArray(mode, "scgettime")[0]);
	}

	@Test
	void onLastFeverTimerDecrementsWhenInFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setIntArray(mode, "feverThreshold", 3, 0);
		setIntArray(mode, "feverTime", 100, 0);
		setIntArray(mode, "feverTimeMin", 1, 0);
		setIntArray(mode, "feverTimeMax", 10, 0);
		setBoolArray(mode, "inFever", true, 0);
		engine.timerActive = true;

		int before = getIntArray(mode, "feverTime")[0];
		mode.onLast(engine, 0);

		assertEquals(before - 1, getIntArray(mode, "feverTime")[0],
				"Fever timer should decrement when in Fever mode");
	}

	@Test
	void onLastInFeverModeDecrementsFeverTimeLimitAddDisplay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setIntArray(mode, "feverThreshold", 3, 0);
		setBoolArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverTime", 100, 0);
		setIntArray(mode, "feverTimeMin", 1, 0);
		setIntArray(mode, "feverTimeMax", 10, 0);
		setIntArray(mode, "feverTimeLimitAddDisplay", 5, 0);
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertEquals(4, getIntArray(mode, "feverTimeLimitAddDisplay")[0],
				"feverTimeLimitAddDisplay should decrement each frame");
		assertEquals(99, getIntArray(mode, "feverTime")[0],
				"Fever timer should decrement when timer is active");
	}

	// ---------------------------------------------------------------
	// addOjama branches
	// ---------------------------------------------------------------

	@Test
	void addOjamaWithZenKeshiAddsBonus() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		setBoolArray(mode, "zenKeshi", true, 0);
		setIntArray(mode, "zenKeshiType",
				AvalancheVSDummyMode.ZENKESHI_MODE_ON, 0);
		setIntArray(mode, "zenKeshiOjama", 30, 0);

		invokeAddOjama(mode, engine, 0, 100);

		// Should have additional 30 ojama from zenkeshi
		assertTrue(getIntArray(mode, "ojamaSent")[0] >= 30,
				"ZenKeshi should add bonus ojama");
	}

	@Test
	void addOjamaWithCounter() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		setIntArray(mode, "ojamaCounterMode",
				AvalancheVSDummyMode.OJAMA_COUNTER_ON, 0);
		setIntArray(mode, "ojama", 10, 0);

		// 100 pts at rate 120 = 1 ojama ceiling, but counter reduces ojama[0]
		int before = getIntArray(mode, "ojama")[0];
		invokeAddOjama(mode, engine, 0, 100);

		// Ojama should have been decremented (countering)
		assertTrue(getIntArray(mode, "ojama")[0] < before,
				"Counter should reduce pending ojama");
	}

	// ---------------------------------------------------------------
	// onLast player win detection
	// ---------------------------------------------------------------

	@Test
	void onLastPlayer1WinDetection() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		GameEngine engine1 = manager.engine[1];
		manager.engine[0].stat = GameEngine.Status.GAMEOVER;
		manager.engine[1].stat = GameEngine.Status.MOVE;
		manager.engine[0].gameActive = true;
		manager.engine[1].gameActive = true;

		mode.onLast(engine1, 1);

		assertEquals(1, readFieldInt(mode, "winnerID"),
				"Player 1 should win when player 0 loses");
	}

	@Test
	void onLastDrawDetection() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		GameEngine engine1 = manager.engine[1];
		manager.engine[0].stat = GameEngine.Status.GAMEOVER;
		manager.engine[1].stat = GameEngine.Status.GAMEOVER;
		manager.engine[0].gameActive = true;
		manager.engine[1].gameActive = true;

		mode.onLast(engine1, 1);

		assertEquals(-1, readFieldInt(mode, "winnerID"),
				"Both players losing should result in a draw");
	}

	// ---------------------------------------------------------------
	// saveReplay
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesVersion() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.playerInit(manager.engine[0], 0);
		GameEngine engine = manager.engine[0];

		mode.saveReplay(engine, 0, manager.replayProp);

		assertEquals(0, manager.replayProp.getProperty("avalanchevs.version", -1));
	}

	@Test
	void addOjamaSendsOjamaToEnemy() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		invokeAddOjama(mode, engine, 0, 100);

		assertTrue(getIntArray(mode, "ojamaSent")[0] >= 1,
				"addOjama should have sent ojama");
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(AvalancheVSMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, AvalancheVSMode mode)
			throws Exception {
		setMenuState(engine, mode, 0, 0);
	}

	private static void setMenuState(GameEngine engine, AvalancheVSMode mode,
			int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, AvalancheVSMode mode,
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

	private static void invokeAddOjama(AvalancheVSMode mode, GameEngine engine,
			int playerID, int pts) throws Exception {
		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"addOjama", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID, pts);
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
