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
 * Drives {@link TechnicianMode#onSetting} across every numeric configuration
 * cursor, pushing each value past both its lower and upper bound so the
 * wraparound branches (value &lt; min and value &gt; max) all fire. These
 * boundary outcomes are otherwise unexercised because the other Technician
 * tests only press a single step away from the default value.
 *
 * <p>TechnicianMode's {@code onSetting} does not consume the BUTTON_E/F speed
 * multipliers, so only LEFT (underflow, change == -1) and RIGHT (overflow,
 * change == +1) are injected.
 */
class TechnicianModeSettingWraparoundTest {

	private static GameEngine freshEngine(TechnicianMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT press, then runs onSetting. */
	private static void change(TechnicianMode mode, GameEngine engine, int cursor, int dirButton)
			throws Exception {
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
	void goaltypeWrapsAtBothBounds() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow: 0 - 1 < 0 -> GAMETYPE_MAX - 1 == 4
		setInt(mode, "goaltype", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(4, readInt(mode, "goaltype"));

		// Overflow: 4 + 1 > 4 -> 0
		setInt(mode, "goaltype", 4);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "goaltype"));
	}

	@Test
	void startlevelWrapsAtBothBoundsAndClampsBackground() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow: 0 - 1 < 0 -> 29. startlevel 29 > 19 so background clamps to 19.
		setInt(mode, "startlevel", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(29, readInt(mode, "startlevel"));
		assertEquals(19, engine.owner.backgroundStatus.bg);

		// Overflow: 29 + 1 > 29 -> 0
		setInt(mode, "startlevel", 29);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(0, engine.owner.backgroundStatus.bg);
	}

	@Test
	void tspinEnableTypeWrapsAtBothBounds() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow: 0 - 1 < 0 -> 2
		setInt(mode, "tspinEnableType", 0);
		change(mode, engine, 2, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "tspinEnableType"));

		// Overflow: 2 + 1 > 2 -> 0
		setInt(mode, "tspinEnableType", 2);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "tspinEnableType"));
	}

	@Test
	void spinCheckTypeWrapsAtBothBounds() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow: 0 - 1 < 0 -> 1
		setInt(mode, "spinCheckType", 0);
		change(mode, engine, 4, Controller.BUTTON_LEFT);
		assertEquals(1, readInt(mode, "spinCheckType"));

		// Overflow: 1 + 1 > 1 -> 0
		setInt(mode, "spinCheckType", 1);
		change(mode, engine, 4, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "spinCheckType"));
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
