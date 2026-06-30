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
 * {@link SpeedManiaMode} uses the {@link nullpomino.game.menu.AbstractMenuItem}
 * driven settings menu rather than a per-mode {@code switch(menuCursor)}, so
 * the numeric wraparound lives in the menu items themselves. These tests cover
 * the settings-screen guard branches that ARE owned by the mode:
 * <ul>
 *   <li>{@code onSetting} BUTTON_F / BUTTON_A {@code menuTime >= 5} guards
 *       (line 362 / 368) when the timer has NOT yet reached 5 (false arm).</li>
 *   <li>{@code startGame} {@code level >= 900} (line 404) start-level
 *       boundary. (The {@code level < 0} arm at line 403 is effectively dead:
 *       a negative level immediately crashes {@code setSpeed} on
 *       {@code tableARE[-1]}, so it is unreachable through the real flow.)</li>
 * </ul>
 */
class SpeedManiaModeSettingWraparoundTest {

	// -----------------------------------------------------------------------
	// onSetting BUTTON_F guard: menuTime < 5 -> toggle must NOT fire (line 362 false)
	// -----------------------------------------------------------------------

	@Test
	void onSettingFButtonIgnoredBeforeMenuTimeThreshold() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuTime", 0); // < 5
		setBoolean(mode, "isShowBestSectionTime", false);

		pressKey(engine, Controller.BUTTON_F);
		mode.onSetting(engine, 0);

		assertFalse(readBoolean(mode, "isShowBestSectionTime"),
				"section-time view must not toggle while menuTime < 5");
	}

	// -----------------------------------------------------------------------
	// onSetting BUTTON_A guard: menuTime < 5 -> decide must NOT confirm (line 368 false)
	// -----------------------------------------------------------------------

	@Test
	void onSettingAButtonIgnoredBeforeMenuTimeThreshold() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuTime", 0); // < 5
		setInt(mode, "sectionscomp", 7);

		pressKey(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);

		assertTrue(result, "onSetting must keep returning true (stay in menu) when A is too early");
		assertEquals(7, readInt(mode, "sectionscomp"),
				"sectionscomp must be untouched while menuTime < 5");
	}

	// -----------------------------------------------------------------------
	// startGame start-level boundary (line 404)
	// -----------------------------------------------------------------------

	@Test
	void startGameMaxStartLevelClampsNextSecToNineNineNine() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntMenuValue(mode, "startlevel", 9); // level = 900 -> line 404 true arm

		mode.startGame(engine, 0);

		assertEquals(900, engine.statistics.level);
		assertEquals(999, readInt(mode, "nextseclv"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(SpeedManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.reset();
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static int readInt(SpeedManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(SpeedManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static void setInt(SpeedManiaMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(SpeedManiaMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void setIntMenuValue(SpeedManiaMode mode, String menuFieldName, int value) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		((IntegerMenuItem) f.get(mode)).value = value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
