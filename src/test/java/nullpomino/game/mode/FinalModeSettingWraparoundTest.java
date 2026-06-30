package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link FinalMode#onSetting} across the settings-menu cursor cases so
 * the wraparound and toggle branches all fire:
 * <ul>
 *   <li>cursor 0: startlevel wraps at both bounds (&lt;0 -&gt; 9, &gt;9 -&gt; 0)</li>
 *   <li>cursor 1/2/3: lvstopse / showsectiontime / big boolean toggles (switch arms)</li>
 *   <li>BUTTON_F flips isShowBestSectionTime</li>
 *   <li>BUTTON_A decides (returns false), BUTTON_B sets quitflag</li>
 * </ul>
 * Uses a non-persisting receiver so the BUTTON_A save path does not write config.
 */
class FinalModeSettingWraparoundTest {

	/** EventReceiver that never writes any config/properties file. */
	private static final class NonPersistingReceiver extends EventReceiver {
		@Override public boolean saveProperties(String f, CustomProperties p) { return true; }
		@Override public void saveModeConfig(CustomProperties c) { }
	}

	private static GameEngine freshEngine(FinalMode mode) {
		GameManager manager = new GameManager(new NonPersistingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT (and optional extra) press, then runs onSetting. */
	private static void change(FinalMode mode, GameEngine engine, int cursor, int dirButton, int extraButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		if(dirButton >= 0) {
			engine.ctrl.buttonPress[dirButton] = true;
			engine.ctrl.buttonTime[dirButton] = 1;
		}
		if(extraButton >= 0) {
			engine.ctrl.buttonPress[extraButton] = true;
			engine.ctrl.buttonTime[extraButton] = 1;
		}
		mode.onSetting(engine, 0);
	}

	@Test
	void startlevelWrapsAtBothBounds() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// underflow: 0 - 1 < 0 -> 9
		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT, -1);
		assertEquals(9, readInt(mode, "startlevel"));

		// overflow: 9 + 1 > 9 -> 0
		setInt(mode, "startlevel", 9);
		change(mode, engine, 0, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "startlevel"));
	}

	@Test
	void lvstopseShowsectiontimeBigToggle() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 1: lvstopse toggles
		setBool(mode, "lvstopse", false);
		change(mode, engine, 1, Controller.BUTTON_RIGHT, -1);
		assertTrue(readBool(mode, "lvstopse"));
		change(mode, engine, 1, Controller.BUTTON_LEFT, -1);
		assertFalse(readBool(mode, "lvstopse"));

		// case 2: showsectiontime toggles
		setBool(mode, "showsectiontime", false);
		change(mode, engine, 2, Controller.BUTTON_RIGHT, -1);
		assertTrue(readBool(mode, "showsectiontime"));

		// case 3: big toggles
		setBool(mode, "big", false);
		change(mode, engine, 3, Controller.BUTTON_RIGHT, -1);
		assertTrue(readBool(mode, "big"));
	}

	@Test
	void fButtonFlipsBestSectionTimeView() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "isShowBestSectionTime", false);
		// No direction press; only F (menuTime >= 5 already set by change()).
		change(mode, engine, 0, -1, Controller.BUTTON_F);
		assertTrue(readBool(mode, "isShowBestSectionTime"));

		change(mode, engine, 0, -1, Controller.BUTTON_F);
		assertFalse(readBool(mode, "isShowBestSectionTime"));
	}

	@Test
	void aButtonDecidesAndExitsSetting() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		// BUTTON_A with menuTime >= 5 -> onSetting returns false (start game).
		assertFalse(mode.onSetting(engine, 0));
	}

	@Test
	void bButtonSetsQuitFlag() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;

		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	@Test
	void replayModeSettingAutoExitsAfter60Frames() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;

		setInt(mode, "menuTime", 59);
		// menuTime becomes 60 -> returns false (exit setting), cursor forced to -1.
		assertFalse(mode.onSetting(engine, 0));
		assertEquals(-1, readInt(mode, "menuCursor"));

		// Below threshold -> stays in setting (returns true).
		setInt(mode, "menuTime", 0);
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
