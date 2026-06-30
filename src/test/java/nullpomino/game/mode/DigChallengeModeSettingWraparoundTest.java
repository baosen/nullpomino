package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link DigChallengeMode#onSetting} across every numeric configuration
 * cursor, pushing each value past both its lower and upper bound so the
 * wraparound branches (value &lt; min and value &gt; max) all fire. The
 * existing menu suite only exercises the in-range increment path, so these
 * cover the previously-missing under/overflow branches at lines 241/242,
 * 246/247, 252/253, 257/258, 265/266 and 279/280.
 */
class DigChallengeModeSettingWraparoundTest {

	private static GameEngine freshEngine(DigChallengeMode mode) {
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
	private static void change(DigChallengeMode mode, GameEngine engine, int cursor, int dirButton) throws Exception {
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
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 0: goaltype, range 0..GOALTYPE_MAX-1 (==1)
		setInt(mode, "goaltype", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT);
		assertEquals(1, readInt(mode, "goaltype"), "goaltype underflow wraps to GOALTYPE_MAX-1");

		setInt(mode, "goaltype", 1);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "goaltype"), "goaltype overflow wraps to 0");
	}

	@Test
	void startlevelWrapsAtBothBounds() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 1: startlevel, range 0..19
		setInt(mode, "startlevel", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(19, readInt(mode, "startlevel"), "startlevel underflow wraps to 19");

		setInt(mode, "startlevel", 19);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"), "startlevel overflow wraps to 0");
	}

	@Test
	void bgmnoWrapsAtBothBounds() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 2: bgmno, range -1..BGM_COUNT-1
		setInt(mode, "bgmno", -1);
		change(mode, engine, 2, Controller.BUTTON_LEFT);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"), "bgmno underflow wraps to BGM_COUNT-1");

		setInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertEquals(-1, readInt(mode, "bgmno"), "bgmno overflow wraps to -1");
	}

	@Test
	void tspinEnableTypeWrapsAtBothBounds() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 3: tspinEnableType, range 0..2
		setInt(mode, "tspinEnableType", 0);
		change(mode, engine, 3, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "tspinEnableType"), "tspinEnableType underflow wraps to 2");

		setInt(mode, "tspinEnableType", 2);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "tspinEnableType"), "tspinEnableType overflow wraps to 0");
	}

	@Test
	void spinCheckTypeWrapsAtBothBounds() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 5: spinCheckType, range 0..1
		setInt(mode, "spinCheckType", 0);
		change(mode, engine, 5, Controller.BUTTON_LEFT);
		assertEquals(1, readInt(mode, "spinCheckType"), "spinCheckType underflow wraps to 1");

		setInt(mode, "spinCheckType", 1);
		change(mode, engine, 5, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "spinCheckType"), "spinCheckType overflow wraps to 0");
	}

	@Test
	void dasWrapsAtBothBounds() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 9: engine.speed.das, range 0..99
		engine.speed.das = 0;
		change(mode, engine, 9, Controller.BUTTON_LEFT);
		assertEquals(99, engine.speed.das, "das underflow wraps to 99");

		engine.speed.das = 99;
		change(mode, engine, 9, Controller.BUTTON_RIGHT);
		assertEquals(0, engine.speed.das, "das overflow wraps to 0");
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
