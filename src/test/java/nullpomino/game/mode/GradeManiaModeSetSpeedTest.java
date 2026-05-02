package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.OnOffMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GradeManiaMode}'s private {@code setSpeed} via
 * reflection. always20g (an OnOffMenuItem) pegs gravity at -1 when on;
 * otherwise the level walks tableGravityChangeLevel to advance
 * gravityindex past every threshold the level reaches, then reads
 * gravity from tableGravityValue.
 *
 * <p>The default tables share the byte shape used by GarbageManiaMode
 * (30 entries, gravity 4 at level 30, ramping up to a -1 sentinel at
 * level 500+).
 */
class GradeManiaModeSetSpeedTest {

	@Test
	void always20gMenuItemOnPegsGravityAtMinusOne() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, true);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"always20g.value=true -> gravity = -1");
	}

	@Test
	void levelZeroLeavesGravityIndexAtZeroForFirstTableEntry() throws Exception {
		// Level 0 < tableGravityChangeLevel[0] = 30 -> gravityindex
		// stays at 0 -> gravity = tableGravityValue[0] = 4.
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(4, engine.speed.gravity,
				"level 0 -> gravity table entry [0] = 4");
	}

	@Test
	void levelOneHundredAdvancesGravityIndexThroughNineThresholds() throws Exception {
		// Level 100 advances past 30, 35, 40, 50, 60, 70, 80, 90, 100
		// -> 9 thresholds passed -> gravityindex 9 -> gravity 80.
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 100;

		invokeSetSpeed(mode, engine);

		assertEquals(80, engine.speed.gravity,
				"level 100 -> 9 thresholds passed -> gravity 80");
	}

	@Test
	void gravityIndexIsMonotonicAcrossSetSpeedCalls() throws Exception {
		// gravityindex never rewinds.
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, false);
		setInt(mode, "gravityindex", 0);

		engine.statistics.level = 100;
		invokeSetSpeed(mode, engine);
		// gravityindex now 9.

		engine.statistics.level = 0;
		invokeSetSpeed(mode, engine);
		assertEquals(80, engine.speed.gravity,
				"gravityindex doesn't rewind -> gravity stays at table[9]=80");
	}

	@Test
	void levelFiveHundredAdvancesGravityIndexToFinalEntry() throws Exception {
		// Level 500 hits the 500 threshold -> gravityindex advances to
		// 29 -> gravity = tableGravityValue[29] = -1 (instant fall).
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 500;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"level 500 -> gravityindex 29 -> instant fall sentinel");
	}

	private static GameEngine freshEngine(GradeManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void setAlways20gValue(GradeManiaMode mode, boolean value)
			throws Exception {
		Field f = findField(mode.getClass(), "always20g");
		f.setAccessible(true);
		OnOffMenuItem item = (OnOffMenuItem) f.get(mode);
		item.value = value;
	}

	private static void invokeSetSpeed(GradeManiaMode mode, GameEngine engine)
			throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod(
				"setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void setInt(Object instance, String name, int value)
			throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
