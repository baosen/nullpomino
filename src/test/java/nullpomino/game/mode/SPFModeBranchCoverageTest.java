package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link SPFMode#onSetting}'s settings-menu
 * wrap-around arithmetic. The pre-existing {@code SPFModeSettingMenuTest}
 * only nudges each value by +/-1 (so it never reaches a boundary); this
 * class drives each numeric cursor to its min/max edge and presses
 * LEFT (underflow) / RIGHT (overflow) so BOTH {@code if(x<min) x=max} and
 * {@code if(x>max) x=min} outcomes are taken. It also exercises the
 * BUTTON_E (x100) / BUTTON_F (x1000) multiplier branches and the
 * {@code if(m>10)} hurryup-scaling branch.
 */
class SPFModeBranchCoverageTest {

	// ---------------------------------------------------------------
	// Cursor 0: gravity (-1 .. 99999)
	// ---------------------------------------------------------------

	@Test
	void cursor0GravityUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 0);
		engine.speed.gravity = -1;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99999, engine.speed.gravity, "gravity < -1 should wrap to 99999");
	}

	@Test
	void cursor0GravityOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 0);
		engine.speed.gravity = 99999;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(-1, engine.speed.gravity, "gravity > 99999 should wrap to -1");
	}

	// ---------------------------------------------------------------
	// Cursor 1: denominator (-1 .. 99999)
	// ---------------------------------------------------------------

	@Test
	void cursor1DenominatorUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 1);
		engine.speed.denominator = -1;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99999, engine.speed.denominator);
	}

	@Test
	void cursor1DenominatorOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 1);
		engine.speed.denominator = 99999;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(-1, engine.speed.denominator);
	}

	// ---------------------------------------------------------------
	// Cursor 2: are (0 .. 99)
	// ---------------------------------------------------------------

	@Test
	void cursor2AreUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 2);
		engine.speed.are = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.are);
	}

	@Test
	void cursor2AreOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 2);
		engine.speed.are = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.are);
	}

	// ---------------------------------------------------------------
	// Cursor 3: areLine (0 .. 99)
	// ---------------------------------------------------------------

	@Test
	void cursor3AreLineUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 3);
		engine.speed.areLine = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.areLine);
	}

	@Test
	void cursor3AreLineOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 3);
		engine.speed.areLine = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.areLine);
	}

	// ---------------------------------------------------------------
	// Cursor 4: lineDelay (0 .. 99)
	// ---------------------------------------------------------------

	@Test
	void cursor4LineDelayUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 4);
		engine.speed.lineDelay = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.lineDelay);
	}

	@Test
	void cursor4LineDelayOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 4);
		engine.speed.lineDelay = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.lineDelay);
	}

	// ---------------------------------------------------------------
	// Cursor 5: lockDelay (0 .. 99)
	// ---------------------------------------------------------------

	@Test
	void cursor5LockDelayUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 5);
		engine.speed.lockDelay = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.lockDelay);
	}

	@Test
	void cursor5LockDelayOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 5);
		engine.speed.lockDelay = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.lockDelay);
	}

	// ---------------------------------------------------------------
	// Cursor 6: das (0 .. 99)
	// ---------------------------------------------------------------

	@Test
	void cursor6DasUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 6);
		engine.speed.das = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.das);
	}

	@Test
	void cursor6DasOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 6);
		engine.speed.das = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.das);
	}

	// ---------------------------------------------------------------
	// Cursor 7/8: presetNumber (0 .. 99)
	// ---------------------------------------------------------------

	@Test
	void cursor7PresetNumberUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 7);
		setIntArray(mode, "presetNumber", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, getIntArray(mode, "presetNumber")[0]);
	}

	@Test
	void cursor8PresetNumberOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 8);
		setIntArray(mode, "presetNumber", 99, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "presetNumber")[0]);
	}

	// ---------------------------------------------------------------
	// Cursor 9: bgmno (0 .. BGM_COUNT-1)
	// ---------------------------------------------------------------

	@Test
	void cursor9BgmnoUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 9);
		setFieldInt(mode, "bgmno", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(BGMStatus.BGM_COUNT - 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void cursor9BgmnoOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 9);
		setFieldInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "bgmno"));
	}

	// ---------------------------------------------------------------
	// Cursor 11: mapSet (0 .. 99) — keep useMap off so no preview side-effects
	// ---------------------------------------------------------------

	@Test
	void cursor11MapSetUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 11);
		setBoolArray(mode, "useMap", false, 0);
		setIntArray(mode, "mapSet", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, getIntArray(mode, "mapSet")[0]);
	}

	@Test
	void cursor11MapSetOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 11);
		setBoolArray(mode, "useMap", false, 0);
		setIntArray(mode, "mapSet", 99, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "mapSet")[0]);
	}

	// ---------------------------------------------------------------
	// Cursor 12: mapNumber (-1 .. mapMaxNo-1) — needs useMap on.
	// Keep propMap null + menuTime not a multiple of 30 so the random
	// preview block (L717) stays out of the way.
	// ---------------------------------------------------------------

	@Test
	void cursor12MapNumberUnderflowWrapsToMaxNo() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 12, 1);
		setBoolArray(mode, "useMap", true, 0);
		setIntArray(mode, "mapMaxNo", 5, 0);
		setIntArray(mode, "mapNumber", -1, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		// -1 + (-1) = -2 < -1 -> wraps to mapMaxNo-1 = 4
		assertEquals(4, getIntArray(mode, "mapNumber")[0], "mapNumber < -1 should wrap to mapMaxNo-1");
	}

	@Test
	void cursor12MapNumberOverflowWrapsToRandom() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 12, 1);
		setBoolArray(mode, "useMap", true, 0);
		setIntArray(mode, "mapMaxNo", 5, 0);
		setIntArray(mode, "mapNumber", 4, 0); // mapMaxNo-1
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		// 4 + 1 = 5 > mapMaxNo-1 -> wraps to -1 (RANDOM)
		assertEquals(-1, getIntArray(mode, "mapNumber")[0], "mapNumber > mapMaxNo-1 should wrap to -1");
	}

	// ---------------------------------------------------------------
	// Cursor 14: hurryupSeconds (0 .. 300)
	// ---------------------------------------------------------------

	@Test
	void cursor14HurryupUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 14);
		setIntArray(mode, "hurryupSeconds", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(300, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void cursor14HurryupOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 14);
		setIntArray(mode, "hurryupSeconds", 300, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void cursor14HurryupWithMultiplierScalesByMOver10() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 14);
		setIntArray(mode, "hurryupSeconds", 100, 0);
		// Hold E so m = 100 (>10): change*m/10 = 1*100/10 = 10
		pressKeyWithModifier(engine, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		mode.onSetting(engine, 0);
		assertEquals(110, getIntArray(mode, "hurryupSeconds")[0],
				"m>10 should add change*m/10 = 10 seconds");
	}

	// ---------------------------------------------------------------
	// Cursor 15: ojamaCountdown (1 .. 9)
	// ---------------------------------------------------------------

	@Test
	void cursor15CountdownUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 15);
		setIntArray(mode, "ojamaCountdown", 1, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(9, getIntArray(mode, "ojamaCountdown")[0]);
	}

	@Test
	void cursor15CountdownOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 15);
		setIntArray(mode, "ojamaCountdown", 9, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(1, getIntArray(mode, "ojamaCountdown")[0]);
	}

	// ---------------------------------------------------------------
	// Cursor 17: diamondPower (0 .. 3)
	// ---------------------------------------------------------------

	@Test
	void cursor17DiamondPowerUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 17);
		setIntArray(mode, "diamondPower", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(3, getIntArray(mode, "diamondPower")[0]);
	}

	@Test
	void cursor17DiamondPowerOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 17);
		setIntArray(mode, "diamondPower", 3, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "diamondPower")[0]);
	}

	// ---------------------------------------------------------------
	// Cursor 18: dropSet (0 .. DROP_PATTERNS.length-1)
	// ---------------------------------------------------------------

	@Test
	void cursor18DropSetUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 18);
		setIntArray(mode, "dropSet", 0, 0);
		setIntArray(mode, "dropMap", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		int max = dropPatternsLength() - 1;
		assertEquals(max, getIntArray(mode, "dropSet")[0], "dropSet<0 should wrap to last set");
	}

	@Test
	void cursor18DropSetOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 18);
		setIntArray(mode, "dropSet", dropPatternsLength() - 1, 0);
		setIntArray(mode, "dropMap", 0, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "dropSet")[0], "dropSet>=length should wrap to 0");
	}

	@Test
	void cursor18DropSetClampsOutOfRangeDropMap() throws Exception {
		// L677: if(dropMap >= DROP_PATTERNS[dropSet].length) dropMap = 0
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 18);
		setIntArray(mode, "dropSet", 0, 0);
		// dropMap larger than the (smaller) maps available in set 1
		setIntArray(mode, "dropMap", 999, 0);
		pressKey(engine, Controller.BUTTON_RIGHT); // -> dropSet 1
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "dropMap")[0],
				"out-of-range dropMap should be clamped to 0 after dropSet change");
	}

	// ---------------------------------------------------------------
	// Cursor 19: dropMap (0 .. DROP_PATTERNS[dropSet].length-1)
	// ---------------------------------------------------------------

	@Test
	void cursor19DropMapUnderflowWrapsToMax() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 19);
		setIntArray(mode, "dropSet", 0, 0);
		setIntArray(mode, "dropMap", 0, 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		int max = dropPatternsSetLength(0) - 1;
		assertEquals(max, getIntArray(mode, "dropMap")[0], "dropMap<0 should wrap to last map");
	}

	@Test
	void cursor19DropMapOverflowWrapsToMin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 19);
		setIntArray(mode, "dropSet", 0, 0);
		setIntArray(mode, "dropMap", dropPatternsSetLength(0) - 1, 0);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, getIntArray(mode, "dropMap")[0], "dropMap>=length should wrap to 0");
	}

	// ---------------------------------------------------------------
	// BUTTON_E (x100) and BUTTON_F (x1000) multiplier branches (L574/L575)
	// ---------------------------------------------------------------

	@Test
	void buttonEMultipliesChangeBy100() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 0);
		engine.speed.gravity = 0;
		pressKeyWithModifier(engine, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		mode.onSetting(engine, 0);
		assertEquals(100, engine.speed.gravity, "holding E should apply x100 multiplier");
	}

	@Test
	void buttonFMultipliesChangeBy1000() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 0);
		engine.speed.gravity = 0;
		pressKeyWithModifier(engine, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		mode.onSetting(engine, 0);
		assertEquals(1000, engine.speed.gravity, "holding F should apply x1000 multiplier");
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(SPFMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, SPFMode mode, int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, SPFMode mode, int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.reset();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	/** Press a direction key while also holding a modifier (E/F) so isPress() is true. */
	private static void pressKeyWithModifier(GameEngine engine, int btn, int modifier) {
		engine.ctrl.reset();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
		// A held modifier: isPress() is buttonPress[]==true.
		engine.ctrl.buttonPress[modifier] = true;
		engine.ctrl.buttonTime[modifier] = 10;
	}

	private static int dropPatternsLength() throws Exception {
		Object arr = staticField("DROP_PATTERNS");
		return java.lang.reflect.Array.getLength(arr);
	}

	private static int dropPatternsSetLength(int set) throws Exception {
		Object arr = staticField("DROP_PATTERNS");
		Object setArr = java.lang.reflect.Array.get(arr, set);
		return java.lang.reflect.Array.getLength(setArr);
	}

	private static Object staticField(String name) throws Exception {
		Field f = findField(SPFMode.class, name);
		return f.get(null);
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		return (int[]) findField(obj.getClass(), name).get(obj);
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
