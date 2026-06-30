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
 * Drives {@link SpeedMania2Mode#onSetting} across every configuration cursor,
 * pushing the numeric cursors (startlevel, torikan) past both bounds so the
 * wraparound branches fire, toggling the boolean cursors, and exercising the
 * BUTTON_F (best-section-time view) and BUTTON_A (confirm) branches.
 */
class SpeedMania2ModeSettingWraparoundTest {

	private static GameEngine freshEngine(SpeedMania2Mode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT press, then runs onSetting. */
	private static void change(SpeedMania2Mode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
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
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 0: startlevel underflow (0 - 1 < 0 -> 13)
		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(13, readInt(mode, "startlevel"));

		// case 0: startlevel overflow (13 + 1 > 13 -> 0)
		setInt(mode, "startlevel", 13);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"));
	}

	@Test
	void torikanWrapsAtBothBounds() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 4: torikan underflow (0 - 60 < 0 -> 72000)
		setInt(mode, "torikan", 0);
		change(mode, engine, 4, Controller.BUTTON_LEFT);
		assertEquals(72000, readInt(mode, "torikan"));

		// case 4: torikan overflow (72000 + 60 > 72000 -> 0)
		setInt(mode, "torikan", 72000);
		change(mode, engine, 4, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "torikan"));
	}

	@Test
	void booleanCursorsToggle() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 1: lvstopse
		setBool(mode, "lvstopse", false);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "lvstopse"));

		// case 2: showsectiontime
		setBool(mode, "showsectiontime", false);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "showsectiontime"));

		// case 3: big
		setBool(mode, "big", false);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "big"));

		// case 5: gradedisp
		setBool(mode, "gradedisp", false);
		change(mode, engine, 5, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "gradedisp"));
	}

	@Test
	void buttonFTogglesBestSectionTimeView() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "isShowBestSectionTime", false);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 5); // menuTime >= 5 required
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1; // isPush == (buttonTime == 1)
		boolean keepGoing = mode.onSetting(engine, 0);

		assertTrue(readBool(mode, "isShowBestSectionTime"),
				"BUTTON_F with menuTime>=5 should toggle isShowBestSectionTime");
		assertTrue(keepGoing, "F toggle should not exit the settings screen");
	}

	@Test
	void buttonAConfirmsAndExits() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 5); // menuTime >= 5 required
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		boolean keepGoing = mode.onSetting(engine, 0);

		assertFalse(keepGoing, "BUTTON_A confirm should exit the settings screen");
		assertEquals(0, readInt(mode, "sectionscomp"), "confirm resets sectionscomp");
	}

	@Test
	void buttonBCancelsSetsQuitFlag() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 5);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag, "BUTTON_B should request quit");
	}

	@Test
	void replayModeBranchAutoAdvances() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Drive the replayMode == true branch (else arm of onSetting).
		engine.owner.replayMode = true;
		setInt(mode, "menuTime", 0);
		boolean keepGoing = mode.onSetting(engine, 0);
		assertTrue(keepGoing, "replay settings should keep running before 60 frames");
		assertEquals(-1, readInt(mode, "menuCursor"), "replay sets menuCursor to -1");

		// At >= 60 frames the replay settings screen finishes.
		setInt(mode, "menuTime", 59);
		boolean done = mode.onSetting(engine, 0);
		assertFalse(done, "replay settings should end at 60 frames");
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
