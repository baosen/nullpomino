package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link UltraMode#onSetting} across every numeric configuration cursor,
 * pushing each value past both its lower and upper bound so the wraparound
 * branches (value &lt; min and value &gt; max) all fire. These were the
 * branches the existing UltraMode tests never reached, because they only ever
 * nudged values one step away from their default. Also exercises the BUTTON_E
 * (x100) and BUTTON_F (x1000) speed multipliers on the gravity cursor.
 *
 * <p>Each assertion confirms the wrapped value, so these tests are
 * self-verifying.
 */
class UltraModeSettingWraparoundTest {

	private static GameEngine freshEngine(UltraMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT (and optional E/F) press, then runs onSetting. */
	private static void change(UltraMode mode, GameEngine engine, int cursor, int dirButton, int multButton)
			throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		// reset() clears both buttonPress and buttonTime; clearButtonState() would
		// leave stale buttonTime entries so a prior LEFT keeps overriding RIGHT
		// (updateCursor checks LEFT before RIGHT).
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		if (multButton >= 0) {
			engine.ctrl.buttonPress[multButton] = true;
			engine.ctrl.buttonTime[multButton] = 1;
		}
		mode.onSetting(engine, 0);
	}

	@Test
	void gravityWrapsAtBothBoundsWithMultipliers() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow with the x100 multiplier (BUTTON_E): -1 - 100 < -1 -> 99999
		engine.speed.gravity = -1;
		change(mode, engine, 0, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(99999, engine.speed.gravity);

		// Overflow with the x1000 multiplier (BUTTON_F): 99999 + 1000 > 99999 -> -1
		engine.speed.gravity = 99999;
		change(mode, engine, 0, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		assertEquals(-1, engine.speed.gravity);
	}

	@Test
	void denominatorWrapsAtBothBounds() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.speed.denominator = -1;
		change(mode, engine, 1, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(99999, engine.speed.denominator);

		engine.speed.denominator = 99999;
		change(mode, engine, 1, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		assertEquals(-1, engine.speed.denominator);
	}

	@Test
	void areAreLineLineDelayLockDelayDasWrapAtBothBounds() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

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

		// case 5: lockDelay
		engine.speed.lockDelay = 0;
		change(mode, engine, 5, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.lockDelay);
		engine.speed.lockDelay = 99;
		change(mode, engine, 5, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.lockDelay);

		// case 6: das
		engine.speed.das = 0;
		change(mode, engine, 6, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.das);
		engine.speed.das = 99;
		change(mode, engine, 6, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.das);
	}

	@Test
	void bgmnoWrapsAtBothBounds() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 7: bgmno
		setInt(mode, "bgmno", 0);
		change(mode, engine, 7, Controller.BUTTON_LEFT, -1);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));
		setInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
		change(mode, engine, 7, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "bgmno"));
	}

	@Test
	void goaltypeWrapsAtBothBounds() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 9: goaltype (GOALTYPE_MAX == 5, so range is 0..4)
		setInt(mode, "goaltype", 0);
		change(mode, engine, 9, Controller.BUTTON_LEFT, -1);
		assertEquals(4, readInt(mode, "goaltype"));
		setInt(mode, "goaltype", 4);
		change(mode, engine, 9, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "goaltype"));
	}

	@Test
	void tspinEnableTypeWrapsAtBothBounds() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 10: tspinEnableType (range 0..2)
		setInt(mode, "tspinEnableType", 0);
		change(mode, engine, 10, Controller.BUTTON_LEFT, -1);
		assertEquals(2, readInt(mode, "tspinEnableType"));
		setInt(mode, "tspinEnableType", 2);
		change(mode, engine, 10, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "tspinEnableType"));
	}

	@Test
	void spinCheckTypeWrapsAtBothBounds() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 12: spinCheckType (range 0..1)
		setInt(mode, "spinCheckType", 0);
		change(mode, engine, 12, Controller.BUTTON_LEFT, -1);
		assertEquals(1, readInt(mode, "spinCheckType"));
		setInt(mode, "spinCheckType", 1);
		change(mode, engine, 12, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "spinCheckType"));
	}

	@Test
	void presetNumberWrapsAtBothBounds() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 16/17: presetNumber (range 0..99)
		setInt(mode, "presetNumber", 0);
		change(mode, engine, 16, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readInt(mode, "presetNumber"));
		setInt(mode, "presetNumber", 99);
		change(mode, engine, 17, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "presetNumber"));
	}

	// --- reflection helpers ---

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
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
