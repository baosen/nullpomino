package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GarbageManiaMode}'s private {@code setSpeed} via
 * reflection. always20g pegs gravity at -1; otherwise the level walks
 * through tableGravityChangeLevel, advancing gravityindex past every
 * threshold the level reaches, then reads gravity from
 * tableGravityValue.
 *
 * <p>The default tables (30 entries) ramp gravity from 4 at level 30
 * up to higher fall speeds, with a discontinuity around 233 and a
 * final -1 (instant fall) sentinel at the last bucket.
 */
class GarbageManiaModeSetSpeedTest {

	@Test
	void always20gPegsGravityAtMinusOneIgnoringLevel() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", true);
		engine.statistics.level = 0;
		// gravity should be set regardless of any prior value.
		engine.speed.gravity = 99999;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"always20g -> gravity = -1 instant fall");
	}

	@Test
	void levelZeroLeavesGravityIndexAtZeroForFirstTableEntry() throws Exception {
		// Level 0 < tableGravityChangeLevel[0] = 30 -> gravityindex
		// stays at 0 -> gravity = tableGravityValue[0] = 4.
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(4, engine.speed.gravity,
				"level 0 -> gravity table entry [0] = 4");
	}

	@Test
	void levelThirtyAdvancesGravityIndexPastTheFirstThreshold() throws Exception {
		// Level 30 == tableGravityChangeLevel[0] -> gravityindex
		// advances to 1 -> gravity = tableGravityValue[1] = 6.
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 30;

		invokeSetSpeed(mode, engine);

		assertEquals(6, engine.speed.gravity,
				"level 30 -> gravityindex 1 -> gravity 6");
	}

	@Test
	void levelOneHundredAdvancesGravityIndexThroughEightThresholds() throws Exception {
		// Level 100 advances past 30, 35, 40, 50, 60, 70, 80, 90, 100
		// -> 9 thresholds passed -> gravityindex 9 -> gravity 80.
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 100;

		invokeSetSpeed(mode, engine);

		assertEquals(80, engine.speed.gravity,
				"level 100 -> 9 thresholds passed -> gravity 80");
	}

	@Test
	void gravityIndexIsMonotonicAcrossSetSpeedCalls() throws Exception {
		// gravityindex never rewinds — once advanced, dropping the
		// level back doesn't reset it.
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
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
	void levelFiveHundredAdvancesGravityIndexToTwentyEightOrTwentyNine() throws Exception {
		// Level 500 advances past tableGravityChangeLevel entries up
		// through index 28 (changeLevel=500 itself) -> gravityindex=29
		// -> gravity = tableGravityValue[29] = -1 (instant fall).
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 500;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"level 500 advances to the final gravityindex -> -1 instant fall");
	}

	private static GameEngine freshEngine(GarbageManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(GarbageManiaMode mode, GameEngine engine)
			throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod(
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

	private static void setBoolean(Object instance, String name, boolean value)
			throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(instance, value);
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
