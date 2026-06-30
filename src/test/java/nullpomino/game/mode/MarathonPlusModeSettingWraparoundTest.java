package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link MarathonPlusMode#onSetting} across every numeric configuration
 * cursor, pushing each value past both its lower and upper bound so the
 * wraparound branches ({@code value < min} and {@code value > max}) all fire.
 *
 * <p>The boolean-toggle cursors (2/4/5/6/7) are exercised from BOTH starting
 * values so the conditional inside {@code x = !x} is taken in both directions.
 *
 * <p>These tests are self-verifying: each asserts the wrapped/toggled field
 * value, so they do not depend on absolute ranking positions and are immune to
 * the cross-test ranking-state leak warned about in the brief.
 */
class MarathonPlusModeSettingWraparoundTest {

	private static GameEngine freshEngine(MarathonPlusMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT press, then runs onSetting. */
	private static void change(MarathonPlusMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		// reset() clears both buttonPress and buttonTime; clearButtonState() would
		// leave stale buttonTime entries so a prior LEFT keeps overriding RIGHT.
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		mode.onSetting(engine, 0);
	}

	@Test
	void startlevelWrapsAtBothBounds() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 0 underflow: 0 - 1 < 0 -> 20
		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(20, readInt(mode, "startlevel"));

		// case 0 overflow: 20 + 1 > 20 -> 0
		setInt(mode, "startlevel", 20);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"));
	}

	@Test
	void startlevelUnderflowClampsBackground() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 0 underflow wraps startlevel to 20, which is > 19 so the
		// background clamp (L160) fires: bg = 20 then clamped to 19.
		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(20, readInt(mode, "startlevel"));
		assertEquals(19, engine.owner.backgroundStatus.bg);
	}

	@Test
	void tspinEnableTypeWrapsAtBothBounds() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 1 underflow: 0 - 1 < 0 -> 2
		setInt(mode, "tspinEnableType", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "tspinEnableType"));

		// case 1 overflow: 2 + 1 > 2 -> 0
		setInt(mode, "tspinEnableType", 2);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "tspinEnableType"));
	}

	@Test
	void spinCheckTypeWrapsAtBothBounds() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 3 underflow: 0 - 1 < 0 -> 1
		setInt(mode, "spinCheckType", 0);
		change(mode, engine, 3, Controller.BUTTON_LEFT);
		assertEquals(1, readInt(mode, "spinCheckType"));

		// case 3 overflow: 1 + 1 > 1 -> 0
		setInt(mode, "spinCheckType", 1);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "spinCheckType"));
	}

	@Test
	void enableTSpinKickTogglesBothDirections() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 2: !x covered for x == false then x == true
		setBool(mode, "enableTSpinKick", false);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertEquals(true, readBool(mode, "enableTSpinKick"));
		change(mode, engine, 2, Controller.BUTTON_LEFT);
		assertEquals(false, readBool(mode, "enableTSpinKick"));
	}

	@Test
	void tspinEnableEZTogglesBothDirections() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 4
		setBool(mode, "tspinEnableEZ", false);
		change(mode, engine, 4, Controller.BUTTON_RIGHT);
		assertEquals(true, readBool(mode, "tspinEnableEZ"));
		change(mode, engine, 4, Controller.BUTTON_LEFT);
		assertEquals(false, readBool(mode, "tspinEnableEZ"));
	}

	@Test
	void enableB2BTogglesBothDirections() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 5
		setBool(mode, "enableB2B", false);
		change(mode, engine, 5, Controller.BUTTON_RIGHT);
		assertEquals(true, readBool(mode, "enableB2B"));
		change(mode, engine, 5, Controller.BUTTON_LEFT);
		assertEquals(false, readBool(mode, "enableB2B"));
	}

	@Test
	void enableComboTogglesBothDirections() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 6
		setBool(mode, "enableCombo", false);
		change(mode, engine, 6, Controller.BUTTON_RIGHT);
		assertEquals(true, readBool(mode, "enableCombo"));
		change(mode, engine, 6, Controller.BUTTON_LEFT);
		assertEquals(false, readBool(mode, "enableCombo"));
	}

	@Test
	void bigTogglesBothDirections() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 7
		setBool(mode, "big", false);
		change(mode, engine, 7, Controller.BUTTON_RIGHT);
		assertEquals(true, readBool(mode, "big"));
		change(mode, engine, 7, Controller.BUTTON_LEFT);
		assertEquals(false, readBool(mode, "big"));
	}

	// --- reflection helpers ---

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
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
