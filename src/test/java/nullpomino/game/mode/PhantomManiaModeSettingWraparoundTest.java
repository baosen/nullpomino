package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link PhantomManiaMode#onSetting} across its menu so the numeric
 * wraparound branches in the underlying {@link IntegerMenuItem#changeBy}
 * (value &lt; min and value &gt; max) all fire for the only numeric cursor
 * ({@code startlevel}, range 0..9). Also exercises the BUTTON_F section-time
 * flip, the BUTTON_B quit branch, and the BUTTON_A decide branch of
 * {@code onSetting}, plus the replay-mode timeout branch.
 */
class PhantomManiaModeSettingWraparoundTest {

	private static GameEngine freshEngine(PhantomManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT press, then runs onSetting. */
	private static void change(PhantomManiaMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
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
	void startLevelWrapsAtBothBounds() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		IntegerMenuItem startlevel = (IntegerMenuItem) field(mode.getClass(), "startlevel").get(mode);

		// cursor 0 = startlevel. Underflow: 0 - 1 < 0 -> wraps to max (9).
		startlevel.value = 0;
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(9, startlevel.value, "startlevel should wrap from 0 to max on LEFT");

		// Overflow: 9 + 1 > 9 -> wraps to min (0).
		startlevel.value = 9;
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, startlevel.value, "startlevel should wrap from max to 0 on RIGHT");
	}

	@Test
	void startLevelNormalDecrementWithinBounds() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		IntegerMenuItem startlevel = (IntegerMenuItem) field(mode.getClass(), "startlevel").get(mode);

		// In-bounds decrement: 5 - 1 = 4 (neither wrap branch taken).
		startlevel.value = 5;
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(4, startlevel.value);

		// In-bounds increment: 4 + 1 = 5.
		startlevel.value = 4;
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(5, startlevel.value);
	}

	@Test
	void buttonFFlipsSectionTimeDisplay() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// menuTime must be >= 5 for the F branch to fire.
		setInt(mode, "menuTime", 10);
		setBool(mode, "isShowBestSectionTime", false);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onSetting(engine, 0);
		assertTrue(readBool(mode, "isShowBestSectionTime"), "F should flip section-time display on");

		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onSetting(engine, 0);
		assertFalse(readBool(mode, "isShowBestSectionTime"), "F should flip section-time display off");
	}

	@Test
	void buttonADecidesAndReturnsFalse() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuTime", 10);
		setBool(mode, "isShowBestSectionTime", true);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		boolean cont = mode.onSetting(engine, 0);
		assertFalse(cont, "A should leave the settings screen (return false)");
		assertFalse(readBool(mode, "isShowBestSectionTime"), "A should reset isShowBestSectionTime");
	}

	@Test
	void buttonBSetsQuitFlag() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuTime", 10);
		engine.quitflag = false;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;

		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag, "B should set the engine quit flag");
	}

	@Test
	void replayModeReturnsFalseAfterTimeout() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.owner.replayMode = true;

		// menuTime < 60 keeps the screen up.
		setInt(mode, "menuTime", 10);
		assertTrue(mode.onSetting(engine, 0), "replay menu stays up while menuTime < 60");
		assertEquals(-1, readInt(mode, "menuCursor"), "replay mode forces menuCursor to -1");

		// menuTime >= 60 dismisses it.
		setInt(mode, "menuTime", 60);
		assertFalse(mode.onSetting(engine, 0), "replay menu exits once menuTime reaches 60");
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
