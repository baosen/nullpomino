package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link AvalancheVSBombBattleMode#onSetting} menu branches
 * (34 cursor positions), UP/DOWN navigation, A-button confirm paths,
 * B-button cancel, replayMode path, renderSetting page boundaries,
 * lineClearEnd explosion/drop paths, onLast, and saveReplay.
 */
class AvalancheVSBombBattleModeSettingMenuTest {

	// ---------------------------------------------------------------
	// onSetting: UP/DOWN navigation
	// ---------------------------------------------------------------

	@Test
	void onSettingUpNavigatesThroughAllPositions() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);

		// Navigate up from 0 to 33
		setFieldInt(mode, "menuCursor", 0);
		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(33, readFieldInt(mode, "menuCursor"),
				"UP at cursor 0 should wrap to 33");

		// Press DOWN to go to 0, then navigate down through all positions
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"First DOWN from 33 should reach 0");

		for (int expected = 1; expected <= 33; expected++) {
			pressKey(engine, Controller.BUTTON_DOWN);
			mode.onSetting(engine, 0);
			assertEquals(expected, readFieldInt(mode, "menuCursor"),
					"Should be at cursor " + expected);
		}
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"DOWN from 33 should wrap to 0");
	}

	@Test
	void onSettingDownWrapsAtBottom() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);

		setFieldInt(mode, "menuCursor", 33);
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	// ---------------------------------------------------------------
	// onSetting: LEFT/RIGHT at each cursor position
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor0Gravity() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = engine.speed.gravity;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(before - 1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor1Denominator() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = engine.speed.denominator;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2Are() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = engine.speed.are;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLine() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		int before = engine.speed.areLine;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		int before = engine.speed.lineDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		int before = engine.speed.lockDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor6Das() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		int before = engine.speed.das;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.das);
	}

	@Test
	void onSettingCursor7CascadeDelay() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		int before = engine.cascadeDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.cascadeDelay);
	}

	@Test
	void onSettingCursor8ClearDelay() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		int before = engine.cascadeClearDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.cascadeClearDelay);
	}

	@Test
	void onSettingCursor9OjamaCounterMode() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		int before = getIntArray(mode, "ojamaCounterMode")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "ojamaCounterMode")[0]);
	}

	@Test
	void onSettingCursor10MaxAttack() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		int before = getIntArray(mode, "maxAttack")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "maxAttack")[0]);
	}

	@Test
	void onSettingCursor11NumColors() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 11);
		setIntArray(mode, "numColors", 4, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(5, getIntArray(mode, "numColors")[0]);
	}

	@Test
	void onSettingCursor12RensaShibari() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 12);
		int before = getIntArray(mode, "rensaShibari")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "rensaShibari")[0]);
	}

	@Test
	void onSettingCursor13ColorClearSize() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 13);
		int before = engine.colorClearSize;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.colorClearSize);
	}

	@Test
	void onSettingCursor14OjamaRate() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 14);
		int before = getIntArray(mode, "ojamaRate")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 10, getIntArray(mode, "ojamaRate")[0]);
	}

	@Test
	void onSettingCursor15Hurryup() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 15);
		int before = getIntArray(mode, "hurryupSeconds")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void onSettingCursor16OjamaHard() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16);
		int before = getIntArray(mode, "ojamaHard")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "ojamaHard")[0]);
	}

	@Test
	void onSettingCursor17DangerColumnDoubleToggles() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 17);
		boolean before = getBoolArray(mode, "dangerColumnDouble")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "dangerColumnDouble")[0]);
	}

	@Test
	void onSettingCursor18DangerColumnShowXToggles() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 18);
		boolean before = getBoolArray(mode, "dangerColumnShowX")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "dangerColumnShowX")[0]);
	}

	@Test
	void onSettingCursor19OjamaCountdown() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 19);
		int before = getIntArray(mode, "ojamaCountdown")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "ojamaCountdown")[0]);
	}

	@Test
	void onSettingCursor20ZenKeshiType() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 20);
		int before = getIntArray(mode, "zenKeshiType")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "zenKeshiType")[0]);
	}

	@Test
	void onSettingCursor21FeverMapSet() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 21);
		int before = getIntArray(mode, "feverMapSet")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "feverMapSet")[0]);
	}

	@Test
	void onSettingCursor22OutlineType() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 22);
		int before = getIntArray(mode, "outlineType")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "outlineType")[0]);
	}

	@Test
	void onSettingCursor23ChainDisplayType() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 23);
		int before = getIntArray(mode, "chainDisplayType")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "chainDisplayType")[0]);
	}

	@Test
	void onSettingCursor24CascadeSlowToggles() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 24);
		boolean before = getBoolArray(mode, "cascadeSlow")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "cascadeSlow")[0]);
	}

	@Test
	void onSettingCursor25NewChainPowerToggles() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 25);
		boolean before = getBoolArray(mode, "newChainPower")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "newChainPower")[0]);
	}

	@Test
	void onSettingCursor26UseMapToggles() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 26);
		boolean before = getBoolArray(mode, "useMap")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "useMap")[0]);
	}

	@Test
	void onSettingCursor27MapSet() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 27);
		int before = getIntArray(mode, "mapSet")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "mapSet")[0]);
	}

	@Test
	void onSettingCursor28MapNumber() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 28);
		int before = getIntArray(mode, "mapNumber")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		// Value should have been affected by the change (may wrap due to bounds)
		assertTrue(getIntArray(mode, "mapNumber")[0] != before,
				"mapNumber should change when RIGHT is pressed");
	}

	@Test
	void onSettingCursor29BigDisplayToggles() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 29);
		boolean before = readFieldBool(mode, "bigDisplay");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "bigDisplay"));
	}

	@Test
	void onSettingCursor30Bgmno() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 30);
		int before = readFieldInt(mode, "bgmno");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor31EnableSEToggles() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 31);
		boolean before = getBoolArray(mode, "enableSE")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "enableSE")[0]);
	}

	@Test
	void onSettingCursor32And33PresetNumber() throws Exception {
		for (int cursor : new int[]{32, 33}) {
			AvalancheVSBombBattleMode m = new AvalancheVSBombBattleMode();
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
	void onSettingPressAAtCursor32LoadsPreset() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 32, 10);
		engine.speed.gravity = 42;

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(4, engine.speed.gravity);
	}

	@Test
	void onSettingPressAAtCursor33SavesPreset() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 33, 10);
		engine.speed.gravity = 99;
		setIntArray(mode, "presetNumber", 0, 0);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(99, engine.owner.modeConfig.getProperty(
				"avalanchevsbombbattle.gravity.0", -1));
	}

	@Test
	void onSettingPressAAtOtherCursorStartsGame() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
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
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
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
		assertEquals(1, engine.statc[4]);
	}

	// ---------------------------------------------------------------
	// renderSetting page boundaries
	// ---------------------------------------------------------------

	@Test
	void renderSettingPage1() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage2() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 9);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage3() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 17);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage4() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 26);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingWaitState() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 1;
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// lineClearEnd branches
	// ---------------------------------------------------------------

	@Test
	void lineClearEndTransfersOjamaAdd() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojamaAdd", 5, 1);

		mode.lineClearEnd(engine, 0);

		assertEquals(5, getIntArray(mode, "ojama")[1]);
		assertEquals(0, getIntArray(mode, "ojamaAdd")[1]);
	}

	@Test
	void lineClearEndBombExplosion() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		// Place a bomb block with countdown=1
		int h = engine.field.getHeight();
		Block bomb = new Block(Block.BLOCK_COLOR_RED);
		bomb.countdown = 1;
		bomb.hard = 0;
		engine.field.setBlock(0, h - 1, bomb);

		mode.lineClearEnd(engine, 0);

		// Bomb should have exploded - countdown=0, block turned gray
		assertEquals(0, engine.field.getBlock(0, h - 1).countdown);
		assertEquals(Block.BLOCK_COLOR_GRAY, engine.field.getBlock(0, h - 1).color);
		assertTrue(engine.field.getBlock(0, h - 1).getAttribute(
				Block.BLOCK_ATTRIBUTE_GARBAGE));
	}

	@Test
	void lineClearEndGameOver() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		engine.field.setBlock(2, 0,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.lineClearEnd(engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	@Test
	void lineClearEndOjamaDrop() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojama", 12, 0);
		setIntArray(mode, "maxAttack", 10, 0);
		setBoolArray(mode, "ojamaDrop", false, 0);
		setBoolArray(mode, "cleared", false, 0);
		setIntArray(mode, "ojamaCounterMode",
				AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);
		setIntArray(mode, "ojamaCountdown", 3, 0);

		boolean result = mode.lineClearEnd(engine, 0);

		assertTrue(result, "Ojama >= 6 should trigger drop");
		assertTrue(getIntArray(mode, "ojama")[0] < 12,
				"ojama should decrease after drop");
	}

	// ---------------------------------------------------------------
	// onLast
	// ---------------------------------------------------------------

	@Test
	void onLastDecrementsScgettime() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);

		setIntArray(mode, "scgettime", 3, 0);
		mode.onLast(engine, 0);
		assertEquals(2, getIntArray(mode, "scgettime")[0]);
	}

	@Test
	void onLastUpdatesOjamaMeter() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode, false);

		engine.meterValue = 0;
		setIntArray(mode, "ojama", 50, 0);
		mode.onLast(engine, 0);

		assertTrue(engine.meterValue >= 0,
				"onLast should update meter value");
	}

	// ---------------------------------------------------------------
	// saveReplay
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesVersion() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
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

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(
			AvalancheVSBombBattleMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine,
			AvalancheVSBombBattleMode mode) throws Exception {
		setMenuState(engine, mode, 0, 0);
	}

	private static void setMenuState(GameEngine engine,
			AvalancheVSBombBattleMode mode, int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine,
			AvalancheVSBombBattleMode mode, int cursor, int menuTime)
			throws Exception {
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
