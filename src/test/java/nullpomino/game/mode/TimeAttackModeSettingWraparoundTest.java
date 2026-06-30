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
 * Drives {@link TimeAttackMode#onSetting} across each settings-menu cursor,
 * pushing the numeric cursors past both their lower and upper bound so the
 * wraparound branches all fire, exercising the boolean toggles, and covering
 * the A/B/D-button branches. Unlike LineRaceMode, this mode applies a fixed
 * {@code change} of +/-1 (no x100/x1000 multiplier), so no E/F buttons.
 */
class TimeAttackModeSettingWraparoundTest {

	// Game type count (mirrors private GAMETYPE_MAX = 11)
	private static final int GAMETYPE_MAX = 11;

	private static GameEngine freshEngine(TimeAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT press, then runs onSetting. */
	private static void change(TimeAttackMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		// reset() clears both buttonPress and buttonTime; clearButtonState() would
		// leave stale buttonTime entries so a prior LEFT keeps overriding RIGHT.
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		mode.onSetting(engine, 0);
	}

	// case 0: goaltype wraps at both bounds (L466 underflow, L467 overflow)
	@Test
	void goaltypeWrapsAtBothBounds() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow: 0 - 1 < 0 -> GAMETYPE_MAX - 1
		setInt(mode, "goaltype", 0);
		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(GAMETYPE_MAX - 1, readInt(mode, "goaltype"));

		// Overflow: (MAX-1) + 1 > MAX-1 -> 0
		setInt(mode, "goaltype", GAMETYPE_MAX - 1);
		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "goaltype"));
	}

	// case 0: startlevel clamp when switching to a goaltype with a lower goal level (L468)
	@Test
	void goaltypeChangeClampsStartLevel() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// goaltype 5 = NORMAL200 (goal level 20). startlevel 19 is legal there.
		// LEFT -> goaltype 4 = ANOTHER2 (goal level 15). 19 > 14 -> clamp to 14.
		setInt(mode, "goaltype", 5);
		setInt(mode, "startlevel", 19);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(4, readInt(mode, "goaltype"));
		assertEquals(14, readInt(mode, "startlevel"));
		// backgroundStatus.bg follows the clamped startlevel
		assertEquals(14, engine.owner.backgroundStatus.bg);
	}

	// case 1: startlevel wraps at both bounds (L473 underflow, L474 overflow)
	@Test
	void startLevelWrapsAtBothBounds() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// goaltype 0 = NORMAL, goal level 15. Underflow: 0 - 1 < 0 -> 14.
		setInt(mode, "goaltype", 0);
		setInt(mode, "startlevel", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(14, readInt(mode, "startlevel"));
		assertEquals(14, engine.owner.backgroundStatus.bg);

		// Overflow: 14 + 1 > 14 -> 0.
		setInt(mode, "goaltype", 0);
		setInt(mode, "startlevel", 14);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(0, engine.owner.backgroundStatus.bg);
	}

	// case 2: showsectiontime toggles (L478)
	@Test
	void showSectionTimeToggles() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "showsectiontime", true);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertFalse(readBool(mode, "showsectiontime"));

		change(mode, engine, 2, Controller.BUTTON_LEFT);
		assertTrue(readBool(mode, "showsectiontime"));
	}

	// case 3: big toggles (L481)
	@Test
	void bigToggles() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "big", false);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "big"));

		change(mode, engine, 3, Controller.BUTTON_LEFT);
		assertFalse(readBool(mode, "big"));
	}

	// A button starts the game once menuTime is high enough (L492 true branch -> return false)
	@Test
	void aButtonStartsGame() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10); // >= 5
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		assertFalse(mode.onSetting(engine, 0), "A with menuTime>=5 should leave the setting screen");
	}

	// A button ignored while menuTime is too low (L492 false branch -> stays true)
	@Test
	void aButtonIgnoredWhenMenuTimeTooLow() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0); // < 5
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		assertTrue(mode.onSetting(engine, 0), "A with menuTime<5 should stay on the setting screen");
	}

	// B button quits when not in net play (L504 true branch)
	@Test
	void bButtonQuitsOffline() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;

		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag, "B offline should set quitflag");
	}

	// replayMode branch: cursor forced to -1, returns false after menuTime>=60 (L514/L518)
	@Test
	void replayModeAutoAdvancesAfterTimeout() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.owner.replayMode = true;
		setInt(mode, "menuTime", 59); // becomes 60 inside onSetting
		boolean keepGoing = mode.onSetting(engine, 0);
		assertFalse(keepGoing, "replay menu should auto-advance once menuTime hits 60");
		assertEquals(-1, readInt(mode, "menuCursor"));

		// Below the timeout it keeps the screen open.
		engine.owner.replayMode = true;
		setInt(mode, "menuTime", 0);
		assertTrue(mode.onSetting(engine, 0), "replay menu stays open before timeout");
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
