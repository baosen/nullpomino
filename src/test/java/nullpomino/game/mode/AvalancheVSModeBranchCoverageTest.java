package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Targets the still-uncovered <em>branch</em> outcomes of
 * {@link AvalancheVSMode}. The pre-existing {@code AvalancheVSModeSettingMenuTest}
 * drives each {@code onSetting} cursor with a single LEFT/RIGHT, which only ever
 * fires the value-change side of each wraparound {@code if}. This class pushes
 * every numeric cursor past both of its bounds (under- and overflow) so the
 * paired {@code if(x < min)} / {@code if(x > max)} branches both execute, holds
 * BUTTON_E (x10/x100) on the multiplier cursors, walks the {@code xyzzy} cheat
 * code state machine, and drives the observable {@code addOjama} /
 * {@code lineClearEnd} / {@code onLast} / {@code renderLast} game-logic branches.
 */
class AvalancheVSModeBranchCoverageTest {

	// ===============================================================
	// onSetting: wraparound branches (both bounds) for every cursor
	// ===============================================================

	@Test
	void speedCursorsWrapAtBothBounds() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 0: gravity (with x100 multiplier via BUTTON_E)
		engine.speed.gravity = -1;
		change(mode, engine, 0, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(99999, engine.speed.gravity);
		engine.speed.gravity = 99999;
		change(mode, engine, 0, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		assertEquals(-1, engine.speed.gravity);

		// case 1: denominator
		engine.speed.denominator = -1;
		change(mode, engine, 1, Controller.BUTTON_LEFT, -1);
		assertEquals(99999, engine.speed.denominator);
		engine.speed.denominator = 99999;
		change(mode, engine, 1, Controller.BUTTON_RIGHT, -1);
		assertEquals(-1, engine.speed.denominator);

		// case 2: are
		engine.speed.are = 0;
		change(mode, engine, 2, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.are);
		engine.speed.are = 99;
		change(mode, engine, 2, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.are);

		// case 3: areLine
		engine.speed.areLine = 0;
		change(mode, engine, 3, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.areLine);
		engine.speed.areLine = 99;
		change(mode, engine, 3, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.areLine);

		// case 4: lineDelay
		engine.speed.lineDelay = 0;
		change(mode, engine, 4, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.lineDelay);
		engine.speed.lineDelay = 99;
		change(mode, engine, 4, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.lineDelay);
	}

	@Test
	void lockDelayDasFallWrapAtBothBounds() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 5: lockDelay -- exercise the m>=10 (x10) branch on underflow
		engine.speed.lockDelay = 0;
		change(mode, engine, 5, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(999, engine.speed.lockDelay);
		engine.speed.lockDelay = 999;
		change(mode, engine, 5, Controller.BUTTON_RIGHT, -1); // m==1 else branch
		assertEquals(0, engine.speed.lockDelay);

		// case 6: das
		engine.speed.das = 0;
		change(mode, engine, 6, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.das);
		engine.speed.das = 99;
		change(mode, engine, 6, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.das);

		// case 7: cascadeDelay
		engine.cascadeDelay = 0;
		change(mode, engine, 7, Controller.BUTTON_LEFT, -1);
		assertEquals(20, engine.cascadeDelay);
		engine.cascadeDelay = 20;
		change(mode, engine, 7, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.cascadeDelay);

		// case 8: cascadeClearDelay
		engine.cascadeClearDelay = 0;
		change(mode, engine, 8, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.cascadeClearDelay);
		engine.cascadeClearDelay = 99;
		change(mode, engine, 8, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.cascadeClearDelay);
	}

	@Test
	void counterMaxAttackColorsMinChainWrapAtBothBounds() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 9: ojamaCounterMode
		setIntArr(mode, "ojamaCounterMode", 0, 0);
		change(mode, engine, 9, Controller.BUTTON_LEFT, -1);
		assertEquals(2, intArr(mode, "ojamaCounterMode")[0]);
		setIntArr(mode, "ojamaCounterMode", 2, 0);
		change(mode, engine, 9, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "ojamaCounterMode")[0]);

		// case 10: maxAttack -- m>=10 (x10) branch on overflow
		setIntArr(mode, "maxAttack", 99, 0);
		change(mode, engine, 10, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		assertEquals(0, intArr(mode, "maxAttack")[0]);
		setIntArr(mode, "maxAttack", 0, 0);
		change(mode, engine, 10, Controller.BUTTON_LEFT, -1); // else (x1) underflow
		assertEquals(99, intArr(mode, "maxAttack")[0]);

		// case 11: numColors (range 3..5)
		setIntArr(mode, "numColors", 3, 0);
		change(mode, engine, 11, Controller.BUTTON_LEFT, -1);
		assertEquals(5, intArr(mode, "numColors")[0]);
		setIntArr(mode, "numColors", 5, 0);
		change(mode, engine, 11, Controller.BUTTON_RIGHT, -1);
		assertEquals(3, intArr(mode, "numColors")[0]);

		// case 12: rensaShibari (range 1..20)
		setIntArr(mode, "rensaShibari", 1, 0);
		change(mode, engine, 12, Controller.BUTTON_LEFT, -1);
		assertEquals(20, intArr(mode, "rensaShibari")[0]);
		setIntArr(mode, "rensaShibari", 20, 0);
		change(mode, engine, 12, Controller.BUTTON_RIGHT, -1);
		assertEquals(1, intArr(mode, "rensaShibari")[0]);
	}

	@Test
	void clearSizeOjamaRateHurryupWrapAtBothBounds() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 13: colorClearSize (range 2..36)
		engine.colorClearSize = 2;
		change(mode, engine, 13, Controller.BUTTON_LEFT, -1);
		assertEquals(36, engine.colorClearSize);
		engine.colorClearSize = 36;
		change(mode, engine, 13, Controller.BUTTON_RIGHT, -1);
		assertEquals(2, engine.colorClearSize);

		// case 14: ojamaRate -- m>=10 (x100) branch on overflow, else (x10) underflow
		setIntArr(mode, "ojamaRate", 1000, 0);
		change(mode, engine, 14, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		assertEquals(10, intArr(mode, "ojamaRate")[0]);
		setIntArr(mode, "ojamaRate", 10, 0);
		change(mode, engine, 14, Controller.BUTTON_LEFT, -1); // -=10 -> 0 < 10 -> 1000
		assertEquals(1000, intArr(mode, "ojamaRate")[0]);

		// case 15: hurryupSeconds -- m>10 (BUTTON_F gives 1000) branch on overflow
		setIntArr(mode, "hurryupSeconds", 300, 0);
		change(mode, engine, 15, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		assertEquals(0, intArr(mode, "hurryupSeconds")[0]);
		setIntArr(mode, "hurryupSeconds", 0, 0);
		change(mode, engine, 15, Controller.BUTTON_LEFT, -1); // else (x1) underflow
		assertEquals(300, intArr(mode, "hurryupSeconds")[0]);
	}

	@Test
	void toggleCursorsFlip() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 16: newChainPower toggles
		setBoolArr(mode, "newChainPower", false, 0);
		change(mode, engine, 16, Controller.BUTTON_RIGHT, -1);
		assertTrue(boolArr(mode, "newChainPower")[0]);

		// case 17: outlineType (range 0..2)
		setIntArr(mode, "outlineType", 0, 0);
		change(mode, engine, 17, Controller.BUTTON_LEFT, -1);
		assertEquals(2, intArr(mode, "outlineType")[0]);
		setIntArr(mode, "outlineType", 2, 0);
		change(mode, engine, 17, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "outlineType")[0]);

		// case 18: chainDisplayType (range 0..3)
		setIntArr(mode, "chainDisplayType", 0, 0);
		change(mode, engine, 18, Controller.BUTTON_LEFT, -1);
		assertEquals(3, intArr(mode, "chainDisplayType")[0]);
		setIntArr(mode, "chainDisplayType", 3, 0);
		change(mode, engine, 18, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "chainDisplayType")[0]);

		// case 19: cascadeSlow toggles
		setBoolArr(mode, "cascadeSlow", false, 0);
		change(mode, engine, 19, Controller.BUTTON_RIGHT, -1);
		assertTrue(boolArr(mode, "cascadeSlow")[0]);

		// case 20: big toggles
		setBoolArr(mode, "big", false, 0);
		change(mode, engine, 20, Controller.BUTTON_RIGHT, -1);
		assertTrue(boolArr(mode, "big")[0]);
	}

	@Test
	void hardOjamaDangerColumnZenkeshiWrapAndToggle() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 21: ojamaHard (range 0..9)
		setIntArr(mode, "ojamaHard", 0, 0);
		change(mode, engine, 21, Controller.BUTTON_LEFT, -1);
		assertEquals(9, intArr(mode, "ojamaHard")[0]);
		setIntArr(mode, "ojamaHard", 9, 0);
		change(mode, engine, 21, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "ojamaHard")[0]);

		// case 22: dangerColumnDouble toggles
		setBoolArr(mode, "dangerColumnDouble", false, 0);
		change(mode, engine, 22, Controller.BUTTON_RIGHT, -1);
		assertTrue(boolArr(mode, "dangerColumnDouble")[0]);

		// case 23: dangerColumnShowX toggles
		setBoolArr(mode, "dangerColumnShowX", false, 0);
		change(mode, engine, 23, Controller.BUTTON_RIGHT, -1);
		assertTrue(boolArr(mode, "dangerColumnShowX")[0]);

		// case 24: zenKeshiType (range 0..2)
		setIntArr(mode, "zenKeshiType", 0, 0);
		change(mode, engine, 24, Controller.BUTTON_LEFT, -1);
		assertEquals(2, intArr(mode, "zenKeshiType")[0]);
		setIntArr(mode, "zenKeshiType", 2, 0);
		change(mode, engine, 24, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "zenKeshiType")[0]);
	}

	@Test
	void zenKeshiOjamaBonusWrapsWhenNotFeverMode() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 25 (else branch: zenKeshiType != FEVER) -- zenKeshiOjama, m>=10 path
		setIntArr(mode, "zenKeshiType", AvalancheVSDummyMode.ZENKESHI_MODE_ON, 0);
		setIntArr(mode, "zenKeshiOjama", 99, 0);
		change(mode, engine, 25, Controller.BUTTON_RIGHT, Controller.BUTTON_E); // +=10 -> >99 -> 1
		assertEquals(1, intArr(mode, "zenKeshiOjama")[0]);
		setIntArr(mode, "zenKeshiOjama", 1, 0);
		change(mode, engine, 25, Controller.BUTTON_LEFT, -1); // else (x1) -> 0 < 1 -> 99
		assertEquals(99, intArr(mode, "zenKeshiOjama")[0]);
	}

	@Test
	void feverThresholdWrapsAtBothBounds() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 26: feverThreshold (range 0..9)
		setIntArr(mode, "feverThreshold", 0, 0);
		change(mode, engine, 26, Controller.BUTTON_LEFT, -1);
		assertEquals(9, intArr(mode, "feverThreshold")[0]);
		setIntArr(mode, "feverThreshold", 9, 0);
		change(mode, engine, 26, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "feverThreshold")[0]);
	}

	@Test
	void feverTimeMinMaxWrapWithMultiplier() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 28: feverTimeMin -- m>=10 (x10) overflow then x1 underflow
		setIntArr(mode, "feverTimeMin", 10, 0);
		setIntArr(mode, "feverTimeMax", 30, 0);
		change(mode, engine, 28, Controller.BUTTON_RIGHT, Controller.BUTTON_E); // +10 -> 20 ok
		assertEquals(20, intArr(mode, "feverTimeMin")[0]);
		setIntArr(mode, "feverTimeMin", 30, 0); // == max
		change(mode, engine, 28, Controller.BUTTON_RIGHT, -1); // 31 > max -> 1
		assertEquals(1, intArr(mode, "feverTimeMin")[0]);
		setIntArr(mode, "feverTimeMin", 1, 0);
		change(mode, engine, 28, Controller.BUTTON_LEFT, -1); // 0 < 1 -> max(30)
		assertEquals(30, intArr(mode, "feverTimeMin")[0]);

		// case 29: feverTimeMax -- x10 overflow then x1 underflow
		setIntArr(mode, "feverTimeMin", 10, 0);
		setIntArr(mode, "feverTimeMax", 99, 0);
		change(mode, engine, 29, Controller.BUTTON_RIGHT, Controller.BUTTON_E); // 109 > 99 -> min(10)
		assertEquals(10, intArr(mode, "feverTimeMax")[0]);
		setIntArr(mode, "feverTimeMax", 10, 0);
		change(mode, engine, 29, Controller.BUTTON_LEFT, -1); // 9 < min(10) -> 99
		assertEquals(99, intArr(mode, "feverTimeMax")[0]);
	}

	@Test
	void feverDisplayCriteriaPowerWrapAndToggle() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 30: feverShowMeter toggles
		setBoolArr(mode, "feverShowMeter", true, 0);
		change(mode, engine, 30, Controller.BUTTON_RIGHT, -1);
		assertFalse(boolArr(mode, "feverShowMeter")[0]);

		// case 31: feverPointCriteria (range 0..2)
		setIntArr(mode, "feverPointCriteria", 0, 0);
		change(mode, engine, 31, Controller.BUTTON_LEFT, -1);
		assertEquals(2, intArr(mode, "feverPointCriteria")[0]);
		setIntArr(mode, "feverPointCriteria", 2, 0);
		change(mode, engine, 31, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "feverPointCriteria")[0]);

		// case 32: feverTimeCriteria (range 0..1)
		setIntArr(mode, "feverTimeCriteria", 0, 0);
		change(mode, engine, 32, Controller.BUTTON_LEFT, -1);
		assertEquals(1, intArr(mode, "feverTimeCriteria")[0]);
		setIntArr(mode, "feverTimeCriteria", 1, 0);
		change(mode, engine, 32, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "feverTimeCriteria")[0]);

		// case 33: feverPower (range 0..20)
		setIntArr(mode, "feverPower", 0, 0);
		change(mode, engine, 33, Controller.BUTTON_LEFT, -1);
		assertEquals(20, intArr(mode, "feverPower")[0]);
		setIntArr(mode, "feverPower", 20, 0);
		change(mode, engine, 33, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "feverPower")[0]);
	}

	@Test
	void sideMeterToggleBranch() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 35: ojamaMeter = (feverThreshold > 0 || !ojamaMeter)
		// feverThreshold==0 && ojamaMeter==true  -> false (the !ojamaMeter==false side)
		setIntArr(mode, "feverThreshold", 0, 0);
		setBoolArr(mode, "ojamaMeter", true, 0);
		change(mode, engine, 35, Controller.BUTTON_RIGHT, -1);
		assertFalse(boolArr(mode, "ojamaMeter")[0]);
		// feverThreshold==0 && ojamaMeter==false -> true (the !ojamaMeter==true side)
		setBoolArr(mode, "ojamaMeter", false, 0);
		change(mode, engine, 35, Controller.BUTTON_RIGHT, -1);
		assertTrue(boolArr(mode, "ojamaMeter")[0]);
	}

	@Test
	void useMapToggleResetsFieldWhenTurnedOff() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 36: useMap true->false with a non-null field -> field.reset() branch
		engine.createFieldIfNeeded();
		assertNotNull(engine.field);
		setBoolArr(mode, "useMap", true, 0);
		change(mode, engine, 36, Controller.BUTTON_RIGHT, -1);
		assertFalse(boolArr(mode, "useMap")[0]);
	}

	@Test
	void mapSetAndMapNumberWrapAtBothBounds() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 37: mapSet wraparound (useMap false so no map preview load)
		setBoolArr(mode, "useMap", false, 0);
		setIntArr(mode, "mapSet", 0, 0);
		change(mode, engine, 37, Controller.BUTTON_LEFT, -1);
		assertEquals(99, intArr(mode, "mapSet")[0]);
		setIntArr(mode, "mapSet", 99, 0);
		change(mode, engine, 37, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "mapSet")[0]);

		// case 38: mapNumber else branch (useMap false) -> mapNumber = -1
		setBoolArr(mode, "useMap", false, 0);
		setIntArr(mode, "mapNumber", 5, 0);
		change(mode, engine, 38, Controller.BUTTON_RIGHT, -1);
		assertEquals(-1, intArr(mode, "mapNumber")[0]);
	}

	@Test
	void mapNumberWrapsWhenUseMapEnabled() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 38 (useMap true): mapNumber wraps both bounds. With no map file
		// loaded mapMaxNo stays 0, so mapMaxNo-1 == -1.
		setBoolArr(mode, "useMap", true, 0);
		setIntArr(mode, "mapMaxNo", 0, 0);
		setIntArr(mode, "mapNumber", -1, 0);
		// RIGHT: 0 > (mapMaxNo-1 == -1) -> wraps to -1
		change(mode, engine, 38, Controller.BUTTON_RIGHT, -1);
		assertEquals(-1, intArr(mode, "mapNumber")[0]);
		// LEFT from -1: -2 < -1 -> mapMaxNo-1 == -1
		setIntArr(mode, "mapNumber", -1, 0);
		change(mode, engine, 38, Controller.BUTTON_LEFT, -1);
		assertEquals(-1, intArr(mode, "mapNumber")[0]);
	}

	@Test
	void bgmAndPresetWrapAndToggle() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// case 39: bgmno wraps at both bounds
		setInt(mode, "bgmno", 0);
		change(mode, engine, 39, Controller.BUTTON_LEFT, -1);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));
		setInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
		change(mode, engine, 39, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "bgmno"));

		// case 40: enableSE toggles
		setBoolArr(mode, "enableSE", false, 0);
		change(mode, engine, 40, Controller.BUTTON_RIGHT, -1);
		assertTrue(boolArr(mode, "enableSE")[0]);

		// case 41: bigDisplay toggles
		setBool(mode, "bigDisplay", false);
		change(mode, engine, 41, Controller.BUTTON_RIGHT, -1);
		assertTrue(readBool(mode, "bigDisplay"));

		// case 42/43: presetNumber wraps at both bounds
		setIntArr(mode, "presetNumber", 0, 0);
		change(mode, engine, 42, Controller.BUTTON_LEFT, -1);
		assertEquals(99, intArr(mode, "presetNumber")[0]);
		setIntArr(mode, "presetNumber", 99, 0);
		change(mode, engine, 43, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, intArr(mode, "presetNumber")[0]);
	}

	// ===============================================================
	// onSetting: xyzzy cheat-code state machine
	// ===============================================================

	@Test
	void xyzzyUpDownLeftRightSequence() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setInt(mode, "menuTime", 0);

		// UP: xyzzy 0 -> 1 (the "else if (xyzzy != 2) xyzzy = 1" branch)
		pressPush(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(1, readInt(mode, "xyzzy"));

		// UP again: xyzzy == 1 -> 2 (the "if (xyzzy == 1) xyzzy++" branch)
		pressPush(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(2, readInt(mode, "xyzzy"));

		// DOWN: xyzzy == 2 -> 3 (the "if (xyzzy == 2 || xyzzy == 3) xyzzy++" branch)
		pressPush(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(3, readInt(mode, "xyzzy"));

		// DOWN: xyzzy == 3 -> 4
		pressPush(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(4, readInt(mode, "xyzzy"));

		// LEFT: xyzzy == 4 -> 5 (the "if (xyzzy == 4 || xyzzy == 6) xyzzy++" branch)
		pressPush(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(5, readInt(mode, "xyzzy"));

		// RIGHT: xyzzy == 5 -> 6 (the "if (xyzzy == 5 || xyzzy == 7) xyzzy++" branch)
		pressPush(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(6, readInt(mode, "xyzzy"));

		// LEFT: xyzzy == 6 -> 7
		pressPush(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(7, readInt(mode, "xyzzy"));

		// RIGHT: xyzzy == 7 -> 8
		pressPush(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(8, readInt(mode, "xyzzy"));
	}

	@Test
	void xyzzyDownResetsWhenNotInChain() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setInt(mode, "menuTime", 0);
		setInt(mode, "xyzzy", 5); // not 2 or 3

		// DOWN with xyzzy not in {2,3}: else branch resets xyzzy = 0
		pressPush(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "xyzzy"));
	}

	@Test
	void xyzzyBButtonAdvancesFrom8() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setInt(mode, "menuTime", 10);
		setInt(mode, "xyzzy", 8);

		// B with xyzzy == 8 && playerID == 0 -> xyzzy++ (does NOT quit)
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertEquals(9, readInt(mode, "xyzzy"));
		assertFalse(engine.quitflag, "B with xyzzy==8 should advance code, not quit");
	}

	@Test
	void xyzzyAButtonFrom9EnablesDebugCode() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setInt(mode, "menuTime", 10);
		setInt(mode, "xyzzy", 9);

		// A with xyzzy == 9 && playerID == 0 -> xyzzy = 573 (the cheat unlock)
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(573, readInt(mode, "xyzzy"));
	}

	@Test
	void onSettingAButtonLoadPreset() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setInt(mode, "menuCursor", 42);
		setInt(mode, "menuTime", 10);
		setInt(mode, "xyzzy", 0);

		// A at cursor 42 -> loadPreset path (does not set statc[4])
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.statc[4]);
	}

	@Test
	void onSettingAButtonSavePreset() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setInt(mode, "menuCursor", 43);
		setInt(mode, "menuTime", 10);
		setInt(mode, "xyzzy", 0);

		// A at cursor 43 -> savePreset + saveModeConfig (NonPersistingReceiver no-ops save)
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.statc[4]);
	}

	@Test
	void onSettingAButtonConfirmAdvancesState() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		setInt(mode, "xyzzy", 0);

		// A at a normal cursor -> save + statc[4] = 1
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4]);
	}

	// ===============================================================
	// onSetting: start synchronisation + per-player-2 path
	// ===============================================================

	@Test
	void bothEnginesReadyStartsGameOnPlayer1() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new NonPersistingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.modeInit(manager);
		manager.engine[0].owner.replayMode = false;
		manager.engine[1].owner.replayMode = false;

		// Both engines have confirmed (statc[4]==1); player 1 triggers READY
		manager.engine[0].statc[4] = 1;
		manager.engine[1].statc[4] = 1;
		mode.onSetting(manager.engine[1], 1);

		assertEquals(GameEngine.Status.READY, manager.engine[0].stat);
		assertEquals(GameEngine.Status.READY, manager.engine[1].stat);
	}

	@Test
	void confirmedPlayerCanCancelBackToMenu() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new NonPersistingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.modeInit(manager);
		manager.engine[0].owner.replayMode = false;

		// Player 0 confirmed but player 1 has not -> B cancels back to menu (statc[4]=0)
		manager.engine[0].statc[4] = 1;
		manager.engine[1].statc[4] = 0;
		pressPush(manager.engine[0], Controller.BUTTON_B);
		mode.onSetting(manager.engine[0], 0);
		assertEquals(0, manager.engine[0].statc[4]);
	}

	@Test
	void replayModeAutoAdvanceMenuTimings() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;

		// menuTime 60 -> cursor 9
		setInt(mode, "menuTime", 60);
		mode.onSetting(engine, 0);
		assertEquals(9, readInt(mode, "menuCursor"));

		// menuTime 120 -> cursor 17
		setInt(mode, "menuTime", 120);
		mode.onSetting(engine, 0);
		assertEquals(17, readInt(mode, "menuCursor"));

		// menuTime 180 -> cursor 26
		setInt(mode, "menuTime", 180);
		mode.onSetting(engine, 0);
		assertEquals(26, readInt(mode, "menuCursor"));

		// menuTime 240 -> cursor 36
		setInt(mode, "menuTime", 240);
		mode.onSetting(engine, 0);
		assertEquals(36, readInt(mode, "menuCursor"));

		// menuTime 300 -> statc[4] = 1
		setInt(mode, "menuTime", 300);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4]);
	}

	// ===============================================================
	// addOjama: counter, fever, zenkeshi, hurryup branches
	// ===============================================================

	@Test
	void addOjamaInFeverUsesFeverPower() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		setBoolArr(mode, "inFever", true, 0);
		setIntArr(mode, "feverPower", 10, 0);
		setIntArr(mode, "ojamaRate", 120, 0);
		setIntArr(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);

		invokeAddOjama(mode, engine, 0, 1000);
		// ojamaNew should be positive and sent to enemy (player 1)
		assertTrue(intArr(mode, "ojamaSent")[0] > 0);
		assertTrue(intArr(mode, "ojamaAdd")[1] > 0);
	}

	@Test
	void addOjamaHurryupHalvesRate() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		setBoolArr(mode, "inFever", false, 0);
		setIntArr(mode, "ojamaRate", 100, 0);
		setIntArr(mode, "hurryupSeconds", 1, 0);
		engine.statistics.time = 120; // > hurryupSeconds, so rate >>= time/(hs*60)
		setIntArr(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);

		invokeAddOjama(mode, engine, 0, 50);
		assertTrue(intArr(mode, "ojamaSent")[0] > 0,
				"hurry-up should shift the rate down, raising sent ojama");
	}

	@Test
	void addOjamaCounterModeCancelsIncoming() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		// inFever true so the fever counter (ojamaFever / ojamaAdd) branches run
		setBoolArr(mode, "inFever", true, 0);
		setIntArr(mode, "feverPower", 10, 0);
		setIntArr(mode, "ojamaRate", 10, 0);
		setIntArr(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_ON, 0);
		setIntArr(mode, "ojamaFever", 5, 0);
		setIntArr(mode, "ojamaAdd", 5, 0);
		setIntArr(mode, "ojama", 100, 0);
		setIntArr(mode, "feverThreshold", 5, 0);
		setIntArr(mode, "feverPoints", 0, 0);
		setIntArr(mode, "feverPointCriteria",
				0 /* COUNTER */, 0);

		invokeAddOjama(mode, engine, 0, 200);
		// countered path should have incremented feverPoints
		assertTrue(intArr(mode, "feverPoints")[0] >= 1,
				"countering should award a fever point");
	}

	@Test
	void addOjamaCounterEnemyFeverTimeOnCounter() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		// Force a garbageCleared so the second half of the feverPoint condition fires
		engine.field.garbageCleared = 3;

		setBoolArr(mode, "inFever", false, 0);
		setIntArr(mode, "ojamaRate", 10, 0);
		setIntArr(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_ON, 0);
		// CLEAR criteria (==1): the garbageCleared>0 && criteria!=COUNTER clause fires
		setIntArr(mode, "feverPointCriteria", 1 /* CLEAR */, 0);
		// Enemy (player 1) set up for COUNTER fever-time award
		setIntArr(mode, "feverThreshold", 5, 1);
		setIntArr(mode, "feverTimeCriteria", 0 /* COUNTER */, 1);
		setBoolArr(mode, "inFever", false, 1);
		setIntArr(mode, "feverTimeMax", 30, 1);
		setIntArr(mode, "feverTime", 60, 1);

		invokeAddOjama(mode, engine, 0, 50);
		assertEquals(60, intArr(mode, "feverTimeLimitAdd")[1],
				"enemy COUNTER criteria should add fever time");
	}

	@Test
	void addOjamaAttackCriteriaAddsOwnFeverTime() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		setBoolArr(mode, "inFever", false, 0);
		setIntArr(mode, "ojamaRate", 10, 0);
		setIntArr(mode, "feverThreshold", 5, 0);
		setIntArr(mode, "feverTimeCriteria", 1 /* ATTACK */, 0);
		setIntArr(mode, "feverTimeMax", 30, 0);
		setIntArr(mode, "feverTime", 60, 0);
		setIntArr(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);

		invokeAddOjama(mode, engine, 0, 50);
		assertEquals(60, intArr(mode, "feverTimeLimitAdd")[0],
				"ATTACK criteria should add fever time on attack");
	}

	// ===============================================================
	// lineClearEnd: fever / zenkeshi / ojama-drop / fever-start branches
	// ===============================================================

	@Test
	void lineClearEndZenkeshiFeverAddsTime() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		setBoolArr(mode, "zenKeshi", true, 0);
		setIntArr(mode, "zenKeshiType", AvalancheVSDummyMode.ZENKESHI_MODE_FEVER, 0);
		setBoolArr(mode, "inFever", true, 0);
		setIntArr(mode, "feverTime", 100, 0);
		setIntArr(mode, "feverTimeMax", 30, 0);
		setIntArr(mode, "feverThreshold", 5, 0);
		setIntArr(mode, "feverPoints", 5, 0);
		setIntArr(mode, "feverChain", 5, 0);
		setIntArr(mode, "feverChainMax", 15, 0);
		setBoolArr(mode, "cleared", false, 0);
		setBoolArr(mode, "ojamaDrop", true, 0);

		mode.lineClearEnd(engine, 0);
		assertEquals(300, intArr(mode, "feverTimeLimitAdd")[0],
				"zenkeshi-fever should add 300 to fever time limit");
	}

	@Test
	void lineClearEndFeverEndsWhenTimeZero() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		setBoolArr(mode, "inFever", true, 0);
		setIntArr(mode, "feverTime", 0, 0);
		setIntArr(mode, "feverTimeMin", 15, 0);
		setIntArr(mode, "feverPoints", 3, 0);
		setBoolArr(mode, "cleared", false, 0);
		setBoolArr(mode, "ojamaDrop", true, 0);
		setIntArr(mode, "ojama", 4, 0);
		setIntArr(mode, "ojamaFever", 2, 0);
		// backup field present so the engine.field swap path runs
		setObjArr(mode, "feverBackupField", new nullpomino.game.component.Field(), 0);
		setBoolArr(mode, "ojamaMeter", true, 0);

		mode.lineClearEnd(engine, 0);
		assertFalse(boolArr(mode, "inFever")[0],
				"fever should end when feverTime hits 0");
		assertEquals(0, intArr(mode, "ojamaFever")[0]);
	}

	@Test
	void lineClearEndDropsOjama() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		setBoolArr(mode, "inFever", false, 0);
		setIntArr(mode, "ojama", 5, 0);
		setIntArr(mode, "maxAttack", 3, 0);
		setBoolArr(mode, "ojamaDrop", false, 0);
		setBoolArr(mode, "cleared", false, 0);
		setIntArr(mode, "ojamaHard", 0, 0);
		setIntArr(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);

		boolean dropped = mode.lineClearEnd(engine, 0);
		assertTrue(dropped, "ojama present should trigger a garbage drop (returns true)");
		assertEquals(2, intArr(mode, "ojama")[0], "5 - min(5,3) == 2 should remain");
	}

	// ===============================================================
	// onLast: fever-meter color thresholds + debug cheat point
	// ===============================================================

	@Test
	void onLastDebugCheatAddsFeverPoint() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "xyzzy", 573);
		setIntArr(mode, "feverThreshold", 5, 0);
		setIntArr(mode, "feverPoints", 0, 0);
		setBoolArr(mode, "ojamaMeter", true, 0);

		pressPush(engine, Controller.BUTTON_F);
		mode.onLast(engine, 0);
		assertEquals(1, intArr(mode, "feverPoints")[0],
				"BUTTON_F with xyzzy==573 should add a fever point");
	}

	@Test
	void onLastFeverMeterColorPointsBelowThreshold() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		// ojamaMeter false + feverThreshold>0 + not inFever -> fever-point meter colors
		setBoolArr(mode, "ojamaMeter", false, 0);
		setIntArr(mode, "feverThreshold", 5, 0);
		setBoolArr(mode, "inFever", false, 0);

		// feverPoints == threshold-1 -> ORANGE
		setIntArr(mode, "feverPoints", 4, 0);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);

		// feverPoints < threshold-1 -> YELLOW
		setIntArr(mode, "feverPoints", 2, 0);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);

		// feverPoints == threshold -> RED
		setIntArr(mode, "feverPoints", 5, 0);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void onLastFeverMeterColorWhenInFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		setBoolArr(mode, "ojamaMeter", false, 0);
		setIntArr(mode, "feverThreshold", 5, 0);
		setBoolArr(mode, "inFever", true, 0);
		setIntArr(mode, "feverTimeMax", 30, 0);
		setIntArr(mode, "feverTimeMin", 10, 0);
		engine.timerActive = false; // avoid decrementing feverTime in this check

		// feverTime <= feverTimeMin*15 (==150) -> RED
		setIntArr(mode, "feverTime", 100, 0);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);

		// feverTime <= feverTimeMin*30 (==300) -> ORANGE
		setIntArr(mode, "feverTime", 200, 0);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);

		// feverTime <= feverTimeMin*60 (==600) -> YELLOW
		setIntArr(mode, "feverTime", 400, 0);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void onLastFeverTimerCountdownAndStop() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);

		setBoolArr(mode, "inFever", true, 0);
		setIntArr(mode, "feverThreshold", 5, 0);
		setBoolArr(mode, "ojamaMeter", true, 0);
		engine.timerActive = true;

		// feverTime 61 -> 60: hits the countdown SE branch (<=360, %60==0)
		setIntArr(mode, "feverTime", 61, 0);
		mode.onLast(engine, 0);
		assertEquals(60, intArr(mode, "feverTime")[0]);

		// feverTime 1 -> 0: hits the levelstop SE branch
		setIntArr(mode, "feverTime", 1, 0);
		mode.onLast(engine, 0);
		assertEquals(0, intArr(mode, "feverTime")[0]);
	}

	// ===============================================================
	// renderLast / drawXorTimer: color thresholds
	// ===============================================================

	@Test
	void renderLastOjamaCounterColorsAndScore() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.gameStarted = true;
		engine.displaysize = 0;

		// ojama >= 12 -> RED branch; ojamaAdd present (not in fever) -> "(+n)" string
		setIntArr(mode, "ojama", 12, 0);
		setIntArr(mode, "ojamaAdd", 3, 0);
		setBoolArr(mode, "inFever", false, 0);
		setIntArr(mode, "ojamaFever", 12, 0);
		// score multiplier branch
		setIntArr(mode, "lastscore", 5, 0);
		setIntArr(mode, "lastmultiplier", 2, 0);
		setIntArr(mode, "scgettime", 10, 0);
		setIntArr(mode, "feverThreshold", 0, 0);
		setIntArr(mode, "ojamaHard", 0, 0);

		mode.renderLast(engine, 0); // smoke: must not throw
	}

	@Test
	void renderLastFeverMeterAndTimerDisplaySize1() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.gameStarted = true;
		engine.displaysize = 1;

		setIntArr(mode, "feverThreshold", 5, 0);
		setBoolArr(mode, "feverShowMeter", true, 0);
		setBoolArr(mode, "inFever", true, 0);
		setIntArr(mode, "feverTime", 120, 0);
		setIntArr(mode, "feverTimeLimitAddDisplay", 5, 0);
		setIntArr(mode, "feverTimeLimitAdd", 60, 0);
		setIntArr(mode, "ojamaHard", 1, 0);
		engine.stat = GameEngine.Status.MOVE;

		mode.renderLast(engine, 0); // smoke: drawXorTimer + fever meter (inFever)
	}

	@Test
	void drawXorTimerFeverShowsRedTimerOnLowTime() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.displaysize = 1;
		setBoolArr(mode, "inFever", true, 0);
		setIntArr(mode, "feverTime", 100, 0); // < 360 -> RED color branch
		engine.field = null; // exercises the (field == null) short-circuit

		mode.drawXorTimer(engine, 0); // smoke: must not throw
	}

	@Test
	void drawXorTimerShowsXWhenNotInFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArr(mode, "inFever", false, 0);
		setBoolArr(mode, "dangerColumnShowX", true, 0);

		mode.drawXorTimer(engine, 0); // smoke: the dangerColumnShowX branch
	}

	// ===============================================================
	// saveReplay: debug-cheat + map backup branches
	// ===============================================================

	@Test
	void saveReplayWritesDebugCheatAndMap() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();

		setInt(mode, "xyzzy", 573); // debugcheatenable branch
		setBoolArr(mode, "useMap", true, 0);
		setObjArr(mode, "fldBackup", new nullpomino.game.component.Field(), 0);

		mode.saveReplay(engine, 0, prop);
		assertTrue(prop.getProperty("avalanchevs.debugcheatenable", false),
				"xyzzy==573 should persist the debug cheat flag");
	}

	// ===============================================================
	// Helpers
	// ===============================================================

	/** EventReceiver that never touches disk (no map/config writes). */
	private static final class NonPersistingReceiver extends EventReceiver {
		@Override public boolean saveProperties(String f, CustomProperties p) { return true; }
		@Override public void saveModeConfig(CustomProperties c) { }
	}

	private static GameEngine freshEngine(AvalancheVSMode mode) {
		GameManager manager = new GameManager(new NonPersistingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.modeInit(manager);
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a LEFT/RIGHT (and optional E/F multiplier) press, then runs onSetting. */
	private static void change(AvalancheVSMode mode, GameEngine engine, int cursor,
			int dirButton, int multButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		setInt(mode, "xyzzy", 0);
		engine.statc[4] = 0;
		// reset() clears both buttonPress and buttonTime; clearButtonState() would
		// leave stale buttonTime so a prior LEFT keeps overriding RIGHT.
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		if (multButton >= 0) {
			engine.ctrl.buttonPress[multButton] = true;
			engine.ctrl.buttonTime[multButton] = 1;
		}
		mode.onSetting(engine, 0);
	}

	private static void pressPush(GameEngine engine, int btn) {
		engine.ctrl.reset();
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

	// reflection field accessors

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
	}

	private static int[] intArr(Object obj, String name) throws Exception {
		return (int[]) field(obj.getClass(), name).get(obj);
	}

	private static boolean[] boolArr(Object obj, String name) throws Exception {
		return (boolean[]) field(obj.getClass(), name).get(obj);
	}

	private static void setIntArr(Object obj, String name, int value, int index) throws Exception {
		((int[]) field(obj.getClass(), name).get(obj))[index] = value;
	}

	private static void setBoolArr(Object obj, String name, boolean value, int index) throws Exception {
		((boolean[]) field(obj.getClass(), name).get(obj))[index] = value;
	}

	private static void setObjArr(Object obj, String name, Object value, int index) throws Exception {
		((Object[]) field(obj.getClass(), name).get(obj))[index] = value;
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
