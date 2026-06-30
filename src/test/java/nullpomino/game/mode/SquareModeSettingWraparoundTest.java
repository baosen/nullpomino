package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link SquareMode#onSetting} across every numeric configuration cursor
 * (cursors 0-4), pushing each value past both its lower and upper bound so the
 * wraparound branches (value &lt; min and value &gt; max) all fire. SquareMode's
 * {@code onSetting} uses {@code updateCursor(engine, 4)} with no x100/x1000
 * multiplier handling, so only LEFT/RIGHT presses are injected.
 */
class SquareModeSettingWraparoundTest {

	private static GameEngine freshEngine(SquareMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT press, then runs onSetting. */
	private static void change(SquareMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
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
	void gametypeWrapsAtBothBounds() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 0: gametype, range 0..2 (GAMETYPE_MAX - 1 == 2)
		setInt(mode, "gametype", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "gametype"));

		setInt(mode, "gametype", 2);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "gametype"));
	}

	@Test
	void outlinetypeWrapsAtBothBounds() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 1: outlinetype, range 0..2
		setInt(mode, "outlinetype", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "outlinetype"));

		setInt(mode, "outlinetype", 2);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "outlinetype"));
	}

	@Test
	void tspinEnableTypeWrapsAtBothBounds() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 2: tspinEnableType, range 0..2
		setInt(mode, "tspinEnableType", 0);
		change(mode, engine, 2, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "tspinEnableType"));

		setInt(mode, "tspinEnableType", 2);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "tspinEnableType"));
	}

	@Test
	void tntAvalancheTogglesOnChange() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 3: tntAvalanche toggles regardless of direction (fills the switch case)
		setBool(mode, "tntAvalanche", false);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertEquals(true, readBool(mode, "tntAvalanche"));

		change(mode, engine, 3, Controller.BUTTON_LEFT);
		assertEquals(false, readBool(mode, "tntAvalanche"));
	}

	@Test
	void grayoutEnableWrapsAtBothBounds() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 4: grayoutEnable, range 0..2
		setInt(mode, "grayoutEnable", 0);
		change(mode, engine, 4, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "grayoutEnable"));

		setInt(mode, "grayoutEnable", 2);
		change(mode, engine, 4, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "grayoutEnable"));
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
