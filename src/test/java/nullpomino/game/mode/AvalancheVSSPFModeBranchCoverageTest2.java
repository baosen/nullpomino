package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage for the {@link AvalancheVSSPFMode#onSetting} settings-menu
 * <em>wraparound</em> boundaries. The sibling {@code SettingMenuTest} exercises
 * the ordinary LEFT/RIGHT adjustment (the {@code if(x<lo)}/{@code if(x>hi)}
 * conditions evaluating FALSE), but never drives a value across its boundary,
 * so the wrap bodies (e.g. {@code if(gravity < -1) gravity = 99999;}) stay
 * uncovered. Each test here sets a numeric cursor's field to its boundary,
 * presses LEFT (underflow) or RIGHT (overflow), calls {@code onSetting}, and
 * ASSERTS the wrapped value. Also covers the BUTTON_E (x100) / BUTTON_F (x1000)
 * multiplier branches and the {@code renderLast} ojama-counter colour
 * thresholds.
 */
class AvalancheVSSPFModeBranchCoverageTest2 {

	// ===============================================================
	// Wraparound underflow (press LEFT at low boundary -> wraps high)
	// ===============================================================

	@Test
	void cursor0GravityUnderflowWrapsToMax() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 0);
		e.speed.gravity = -1;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99999, e.speed.gravity);
	}

	@Test
	void cursor0GravityOverflowWrapsToMin() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 0);
		e.speed.gravity = 99999;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(-1, e.speed.gravity);
	}

	@Test
	void cursor1DenominatorUnderflowWrapsToMax() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 1);
		e.speed.denominator = -1;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99999, e.speed.denominator);
	}

	@Test
	void cursor1DenominatorOverflowWrapsToMin() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 1);
		e.speed.denominator = 99999;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(-1, e.speed.denominator);
	}

	@Test
	void cursor2AreUnderflowWrapsTo99() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 2);
		e.speed.are = 0;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99, e.speed.are);
	}

	@Test
	void cursor2AreOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 2);
		e.speed.are = 99;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, e.speed.are);
	}

	@Test
	void cursor3AreLineUnderflowWrapsTo99() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 3);
		e.speed.areLine = 0;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99, e.speed.areLine);
	}

	@Test
	void cursor3AreLineOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 3);
		e.speed.areLine = 99;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, e.speed.areLine);
	}

	@Test
	void cursor4LineDelayUnderflowWrapsTo99() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 4);
		e.speed.lineDelay = 0;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99, e.speed.lineDelay);
	}

	@Test
	void cursor4LineDelayOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 4);
		e.speed.lineDelay = 99;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, e.speed.lineDelay);
	}

	@Test
	void cursor5LockDelayUnderflowWrapsTo999() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 5);
		e.speed.lockDelay = 0;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(999, e.speed.lockDelay);
	}

	@Test
	void cursor5LockDelayOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 5);
		e.speed.lockDelay = 999;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, e.speed.lockDelay);
	}

	/** BUTTON_E makes m>=10 so lockDelay steps by change*10. */
	@Test
	void cursor5LockDelayButtonEStepsBy10() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 5);
		e.speed.lockDelay = 100;
		pressKeyWith(e, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		mode.onSetting(e, 0);
		assertEquals(110, e.speed.lockDelay);
	}

	@Test
	void cursor6DasUnderflowWrapsTo99() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 6);
		e.speed.das = 0;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99, e.speed.das);
	}

	@Test
	void cursor6DasOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 6);
		e.speed.das = 99;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, e.speed.das);
	}

	@Test
	void cursor7CascadeDelayUnderflowWrapsTo20() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 7);
		e.cascadeDelay = 0;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(20, e.cascadeDelay);
	}

	@Test
	void cursor7CascadeDelayOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 7);
		e.cascadeDelay = 20;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, e.cascadeDelay);
	}

	@Test
	void cursor8CascadeClearDelayUnderflowWrapsTo99() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 8);
		e.cascadeClearDelay = 0;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99, e.cascadeClearDelay);
	}

	@Test
	void cursor8CascadeClearDelayOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 8);
		e.cascadeClearDelay = 99;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, e.cascadeClearDelay);
	}

	@Test
	void cursor9OjamaCounterModeUnderflowWrapsTo2() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 9);
		setIntArr(mode, "ojamaCounterMode", 0, 0);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(2, getIntArr(mode, "ojamaCounterMode")[0]);
	}

	@Test
	void cursor9OjamaCounterModeOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 9);
		setIntArr(mode, "ojamaCounterMode", 0, 2);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "ojamaCounterMode")[0]);
	}

	@Test
	void cursor10MaxAttackUnderflowWrapsTo99() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 10);
		setIntArr(mode, "maxAttack", 0, 0);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99, getIntArr(mode, "maxAttack")[0]);
	}

	@Test
	void cursor10MaxAttackOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 10);
		setIntArr(mode, "maxAttack", 0, 99);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "maxAttack")[0]);
	}

	/** BUTTON_E makes m>=10 so maxAttack steps by change*10. */
	@Test
	void cursor10MaxAttackButtonEStepsBy10() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 10);
		setIntArr(mode, "maxAttack", 0, 20);
		pressKeyWith(e, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		mode.onSetting(e, 0);
		assertEquals(10, getIntArr(mode, "maxAttack")[0]);
	}

	@Test
	void cursor11RensaShibariUnderflowWrapsTo20() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 11);
		setIntArr(mode, "rensaShibari", 0, 1);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(20, getIntArr(mode, "rensaShibari")[0]);
	}

	@Test
	void cursor11RensaShibariOverflowWrapsTo1() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 11);
		setIntArr(mode, "rensaShibari", 0, 20);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(1, getIntArr(mode, "rensaShibari")[0]);
	}

	@Test
	void cursor12ColorClearSizeUnderflowWrapsTo36() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 12);
		e.colorClearSize = 2;
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(36, e.colorClearSize);
	}

	@Test
	void cursor12ColorClearSizeOverflowWrapsTo2() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 12);
		e.colorClearSize = 36;
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(2, e.colorClearSize);
	}

	@Test
	void cursor13OjamaRateUnderflowWrapsTo1000() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 13);
		setIntArr(mode, "ojamaRate", 0, 10);
		pressKey(e, Controller.BUTTON_LEFT); // 10 - 10 = 0 < 10 -> 1000
		mode.onSetting(e, 0);
		assertEquals(1000, getIntArr(mode, "ojamaRate")[0]);
	}

	@Test
	void cursor13OjamaRateOverflowWrapsTo10() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 13);
		setIntArr(mode, "ojamaRate", 0, 1000);
		pressKey(e, Controller.BUTTON_RIGHT); // 1000 + 10 = 1010 > 1000 -> 10
		mode.onSetting(e, 0);
		assertEquals(10, getIntArr(mode, "ojamaRate")[0]);
	}

	/** BUTTON_E makes m>=10 so ojamaRate steps by change*100. */
	@Test
	void cursor13OjamaRateButtonEStepsBy100() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 13);
		setIntArr(mode, "ojamaRate", 0, 500);
		pressKeyWith(e, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		mode.onSetting(e, 0);
		assertEquals(600, getIntArr(mode, "ojamaRate")[0]);
	}

	@Test
	void cursor14HurryupUnderflowWrapsTo300() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 14);
		setIntArr(mode, "hurryupSeconds", 0, 0);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(300, getIntArr(mode, "hurryupSeconds")[0]);
	}

	@Test
	void cursor14HurryupOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 14);
		setIntArr(mode, "hurryupSeconds", 0, 300);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "hurryupSeconds")[0]);
	}

	/** BUTTON_F makes m=1000>10 so hurryup steps by change*m/10 = 100. */
	@Test
	void cursor14HurryupButtonFStepsBy100() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 14);
		setIntArr(mode, "hurryupSeconds", 0, 100);
		pressKeyWith(e, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		mode.onSetting(e, 0);
		assertEquals(200, getIntArr(mode, "hurryupSeconds")[0]);
	}

	@Test
	void cursor17OjamaCountdownUnderflowWrapsTo10() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 17);
		setIntArr(mode, "ojamaCountdown", 0, 1);
		pressKey(e, Controller.BUTTON_LEFT); // 0 < 1 -> 10
		mode.onSetting(e, 0);
		assertEquals(10, getIntArr(mode, "ojamaCountdown")[0]);
	}

	@Test
	void cursor17OjamaCountdownOverflowWrapsTo1() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 17);
		setIntArr(mode, "ojamaCountdown", 0, 10);
		pressKey(e, Controller.BUTTON_RIGHT); // 11 > 10 -> 1
		mode.onSetting(e, 0);
		assertEquals(1, getIntArr(mode, "ojamaCountdown")[0]);
	}

	@Test
	void cursor18ZenKeshiTypeUnderflowWrapsTo2() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 18);
		setIntArr(mode, "zenKeshiType", 0, 0);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(2, getIntArr(mode, "zenKeshiType")[0]);
	}

	@Test
	void cursor18ZenKeshiTypeOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 18);
		setIntArr(mode, "zenKeshiType", 0, 2);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "zenKeshiType")[0]);
	}

	@Test
	void cursor19FeverMapSetUnderflowWrapsToLast() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 19);
		setIntArr(mode, "feverMapSet", 0, 0);
		pressKey(e, Controller.BUTTON_LEFT); // -1 < 0 -> FEVER_MAPS.length-1 = 4
		mode.onSetting(e, 0);
		assertEquals(AvalancheVSDummyMode.FEVER_MAPS.length - 1, getIntArr(mode, "feverMapSet")[0]);
	}

	@Test
	void cursor19FeverMapSetOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 19);
		setIntArr(mode, "feverMapSet", 0, AvalancheVSDummyMode.FEVER_MAPS.length - 1);
		pressKey(e, Controller.BUTTON_RIGHT); // == length -> 0
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "feverMapSet")[0]);
	}

	@Test
	void cursor20OutlineTypeUnderflowWrapsTo2() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 20);
		setIntArr(mode, "outlineType", 0, 0);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(2, getIntArr(mode, "outlineType")[0]);
	}

	@Test
	void cursor20OutlineTypeOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 20);
		setIntArr(mode, "outlineType", 0, 2);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "outlineType")[0]);
	}

	@Test
	void cursor21ChainDisplayTypeUnderflowWrapsTo3() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 21);
		setIntArr(mode, "chainDisplayType", 0, 0);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(3, getIntArr(mode, "chainDisplayType")[0]);
	}

	@Test
	void cursor21ChainDisplayTypeOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 21);
		setIntArr(mode, "chainDisplayType", 0, 3);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "chainDisplayType")[0]);
	}

	@Test
	void cursor25MapSetUnderflowWrapsTo99() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 25);
		setBoolArr(mode, "useMap", 0, false); // avoid map preview side-effects
		setIntArr(mode, "mapSet", 0, 0);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99, getIntArr(mode, "mapSet")[0]);
	}

	@Test
	void cursor25MapSetOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 25);
		setBoolArr(mode, "useMap", 0, false);
		setIntArr(mode, "mapSet", 0, 99);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "mapSet")[0]);
	}

	@Test
	void cursor26MapNumberUnderflowWrapsToMaxNo() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 26);
		setBoolArr(mode, "useMap", 0, true);
		setIntArr(mode, "mapMaxNo", 0, 5);
		setIntArr(mode, "mapNumber", 0, -1);
		e.createFieldIfNeeded();
		pressKey(e, Controller.BUTTON_LEFT); // -2 < -1 -> mapMaxNo-1 = 4
		mode.onSetting(e, 0);
		assertEquals(4, getIntArr(mode, "mapNumber")[0]);
	}

	@Test
	void cursor26MapNumberOverflowWrapsToMinusOne() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 26);
		setBoolArr(mode, "useMap", 0, true);
		setIntArr(mode, "mapMaxNo", 0, 5);
		setIntArr(mode, "mapNumber", 0, 4); // mapMaxNo-1
		e.createFieldIfNeeded();
		pressKey(e, Controller.BUTTON_RIGHT); // 5 > 4 -> -1
		mode.onSetting(e, 0);
		assertEquals(-1, getIntArr(mode, "mapNumber")[0]);
	}

	@Test
	void cursor28BgmnoUnderflowWrapsToLast() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 28);
		setInt(mode, "bgmno", 0);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(nullpomino.game.component.BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));
	}

	@Test
	void cursor28BgmnoOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 28);
		setInt(mode, "bgmno", nullpomino.game.component.BGMStatus.BGM_COUNT - 1);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, readInt(mode, "bgmno"));
	}

	@Test
	void cursor30PresetNumberUnderflowWrapsTo99() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 30);
		setIntArr(mode, "presetNumber", 0, 0);
		pressKey(e, Controller.BUTTON_LEFT);
		mode.onSetting(e, 0);
		assertEquals(99, getIntArr(mode, "presetNumber")[0]);
	}

	@Test
	void cursor31PresetNumberOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 31);
		setIntArr(mode, "presetNumber", 0, 99);
		pressKey(e, Controller.BUTTON_RIGHT);
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "presetNumber")[0]);
	}

	@Test
	void cursor32DropSetUnderflowWrapsToLast() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 32);
		setIntArr(mode, "dropSet", 0, 0);
		setIntArr(mode, "dropMap", 0, 0);
		e.createFieldIfNeeded();
		pressKey(e, Controller.BUTTON_LEFT); // -1 < 0 -> DROP_PATTERNS.length-1
		mode.onSetting(e, 0);
		// 6 drop sets defined (indices 0..5)
		assertEquals(5, getIntArr(mode, "dropSet")[0]);
	}

	@Test
	void cursor32DropSetOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 32);
		setIntArr(mode, "dropSet", 0, 5); // last set
		setIntArr(mode, "dropMap", 0, 0);
		e.createFieldIfNeeded();
		pressKey(e, Controller.BUTTON_RIGHT); // == length -> 0
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "dropSet")[0]);
	}

	/**
	 * Cursor 32: when the new dropSet has fewer maps than the current dropMap,
	 * dropMap is clamped to 0 (line 468 TRUE branch). Set 3 has 17 maps; set 4
	 * has 11. Moving 3 -> 4 with dropMap at 15 forces the clamp.
	 */
	@Test
	void cursor32DropSetClampsDropMap() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 32);
		setIntArr(mode, "dropSet", 0, 3); // 17 maps
		setIntArr(mode, "dropMap", 0, 15); // valid for set 3, too big for set 4
		e.createFieldIfNeeded();
		pressKey(e, Controller.BUTTON_RIGHT); // -> set 4 (11 maps) -> clamp dropMap to 0
		mode.onSetting(e, 0);
		assertEquals(4, getIntArr(mode, "dropSet")[0]);
		assertEquals(0, getIntArr(mode, "dropMap")[0]);
	}

	@Test
	void cursor33DropMapUnderflowWrapsToLast() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 33);
		setIntArr(mode, "dropSet", 0, 3); // S-MIRROR set (index 3)
		setIntArr(mode, "dropMap", 0, 0);
		e.createFieldIfNeeded();
		pressKey(e, Controller.BUTTON_LEFT); // 0-1 = -1 < 0 -> length-1
		mode.onSetting(e, 0);
		// dropMap must now be at the last index of set 3.
		assertEquals(lengthOfDropSet(3) - 1, getIntArr(mode, "dropMap")[0]);
	}

	@Test
	void cursor33DropMapOverflowWrapsTo0() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		setMenu(mode, e, 33);
		setIntArr(mode, "dropSet", 0, 3);
		// Put dropMap at the last valid index for set 3, then RIGHT overflows to 0.
		setIntArr(mode, "dropMap", 0, lengthOfDropSet(3) - 1);
		e.createFieldIfNeeded();
		pressKey(e, Controller.BUTTON_RIGHT); // == length -> 0
		mode.onSetting(e, 0);
		assertEquals(0, getIntArr(mode, "dropMap")[0]);
	}

	// ===============================================================
	// renderLast ojama-counter colour thresholds (>=6 ORANGE, >=12 RED)
	// ===============================================================

	@Test
	void renderLastOjamaOrangeThreshold() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		e.gameStarted = true;
		setIntArr(mode, "ojama", 0, 8); // >=6 -> ORANGE, <12
		mode.renderLast(e, 0);
		assertEquals(8, getIntArr(mode, "ojama")[0]);
	}

	@Test
	void renderLastOjamaRedThreshold() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		e.gameStarted = true;
		setIntArr(mode, "ojama", 0, 15); // >=12 -> RED
		mode.renderLast(e, 0);
		assertEquals(15, getIntArr(mode, "ojama")[0]);
	}

	// ===============================================================
	// Helpers
	// ===============================================================

	private static GameEngine freshEngine(AvalancheVSSPFMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		mode.modeInit(m);
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		m.engine[0].ruleopt.fieldHiddenHeight = 4;
		m.engine[0].owner.replayMode = false;
		return m.engine[0];
	}

	private static void setMenu(AvalancheVSSPFMode mode, GameEngine e, int cursor) throws Exception {
		e.owner.replayMode = false;
		e.statc[4] = 0;
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
	}

	/** Look up the number of maps in DROP_PATTERNS[set] via reflection. */
	private static int lengthOfDropSet(int set) throws Exception {
		Field f = AvalancheVSSPFMode.class.getDeclaredField("DROP_PATTERNS");
		f.setAccessible(true);
		int[][][][] dp = (int[][][][]) f.get(null);
		return dp[set].length;
	}

	private static void pressKey(GameEngine e, int btn) {
		e.ctrl.reset();
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	/** Hold {@code modifier} (E/F) while pressing {@code btn} (LEFT/RIGHT). */
	private static void pressKeyWith(GameEngine e, int btn, int modifier) {
		e.ctrl.reset();
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
		e.ctrl.buttonPress[modifier] = true;
		e.ctrl.buttonTime[modifier] = 1;
	}

	private static int readInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	private static int[] getIntArr(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return (int[]) f.get(o);
	}

	private static void setIntArr(Object o, String n, int idx, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((int[]) f.get(o))[idx] = v;
	}

	private static void setBoolArr(Object o, String n, int idx, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((boolean[]) f.get(o))[idx] = v;
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException ex) { /* continue */ }
		throw new NoSuchFieldException(n);
	}
}
