package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link GarbageManiaMode#onSetting} across the numeric/boolean
 * configuration cursors, covering the wraparound branches that the existing
 * suite leaves untouched:
 * <ul>
 *   <li>cursor 0 (startlevel) underflow {@code 0 -> 9} and overflow {@code 9 -> 0}
 *       (the missing {@code switch(menuCursor)} arm),</li>
 *   <li>each ON/OFF toggle driven from the {@code true} state back to {@code false}
 *       (the existing tests only drive {@code false -> true}),</li>
 *   <li>the BUTTON_F / BUTTON_A guards with {@code menuTime < 5} so their
 *       second condition is observed as false.</li>
 * </ul>
 */
class GarbageManiaModeSettingWraparoundTest {

	private static GameEngine freshEngine(GarbageManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT press, then runs onSetting. */
	private static void change(GarbageManiaMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 5);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		mode.onSetting(engine, 0);
	}

	@Test
	void startlevelWrapsAtBothBounds() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow: 0 - 1 < 0 -> 9
		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(9, readInt(mode, "startlevel"));
		assertEquals(9, engine.owner.backgroundStatus.bg);

		// Overflow: 9 + 1 > 9 -> 0
		setInt(mode, "startlevel", 9);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(0, engine.owner.backgroundStatus.bg);
	}

	@Test
	void booleanTogglesFlipBackToFalse() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 1: alwaysghost true -> false
		setBool(mode, "alwaysghost", true);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertFalse(readBool(mode, "alwaysghost"));

		// case 2: always20g true -> false
		setBool(mode, "always20g", true);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertFalse(readBool(mode, "always20g"));

		// case 3: lvstopse true -> false
		setBool(mode, "lvstopse", true);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertFalse(readBool(mode, "lvstopse"));

		// case 4: showsectiontime true -> false
		setBool(mode, "showsectiontime", true);
		change(mode, engine, 4, Controller.BUTTON_RIGHT);
		assertFalse(readBool(mode, "showsectiontime"));

		// case 5: big true -> false
		setBool(mode, "big", true);
		change(mode, engine, 5, Controller.BUTTON_RIGHT);
		assertFalse(readBool(mode, "big"));
	}

	@Test
	void fButtonGuardFailsWhenMenuTimeTooLow() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// F pushed but menuTime < 5 -> toggle must NOT happen (second condition false)
		setInt(mode, "menuTime", 0);
		setBool(mode, "isShowBestSectionTime", false);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onSetting(engine, 0);
		assertFalse(readBool(mode, "isShowBestSectionTime"));
	}

	@Test
	void aButtonGuardFailsWhenMenuTimeTooLow() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// A pushed but menuTime < 5 -> onSetting must NOT confirm/return false
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		assertTrue(mode.onSetting(engine, 0));
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
