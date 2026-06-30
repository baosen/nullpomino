package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link ScoreAttackMode#onSetting} across the {@code startlevel}
 * numeric cursor (case 0 of the {@code switch(menuCursor)}), pushing the value
 * past both its lower bound (0 -&gt; -1 wraps to 2) and upper bound
 * (2 -&gt; 3 wraps to 0), plus a middle step that takes neither wrap branch.
 * This fills in the unexercised {@code case 0} switch arm together with both
 * boundary {@code if} outcomes of the startlevel guard.
 */
class ScoreAttackModeSettingWraparoundTest {

	private static GameEngine freshEngine(ScoreAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT press, then runs onSetting. */
	private static void change(ScoreAttackMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 5);
		// reset() clears both buttonPress and buttonTime; clearButtonState() would
		// leave stale buttonTime entries so a prior LEFT keeps overriding RIGHT.
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		mode.onSetting(engine, 0);
	}

	@Test
	void startlevelUnderflowWrapsToTwo() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// 0 - 1 = -1 < 0 -> wraps to 2
		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "startlevel"));
		// startlevel feeds the background selector.
		assertEquals(2, engine.owner.backgroundStatus.bg);
	}

	@Test
	void startlevelOverflowWrapsToZero() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// 2 + 1 = 3 > 2 -> wraps to 0
		setInt(mode, "startlevel", 2);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(0, engine.owner.backgroundStatus.bg);
	}

	@Test
	void startlevelMiddleStepTakesNeitherWrap() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// 0 + 1 = 1, in [0,2] -> no wrap (covers the false outcome of both guards)
		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(1, readInt(mode, "startlevel"));
		assertEquals(1, engine.owner.backgroundStatus.bg);

		// 2 - 1 = 1, still in range
		setInt(mode, "startlevel", 2);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(1, readInt(mode, "startlevel"));
	}

	@Test
	void booleanTogglesFlipBackFromTrue() throws Exception {
		// The boolean toggles (alwaysghost/always20g/showsectiontime/big) compile
		// to a conditional negation; existing tests only exercise the false->true
		// direction, so drive the true->false direction here.
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "alwaysghost", true);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(false, readBool(mode, "alwaysghost"));

		setBool(mode, "always20g", true);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertEquals(false, readBool(mode, "always20g"));

		setBool(mode, "showsectiontime", true);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertEquals(false, readBool(mode, "showsectiontime"));

		setBool(mode, "big", true);
		change(mode, engine, 4, Controller.BUTTON_RIGHT);
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
