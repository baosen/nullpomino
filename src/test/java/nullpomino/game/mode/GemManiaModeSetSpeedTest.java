package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GemManiaMode}'s private {@code setSpeed} via
 * reflection. always20g pegs gravity at -1; otherwise the speedlevel
 * walks tableGravityChangeLevel to advance gravityindex past every
 * threshold reached, then reads gravity from tableGravityValue.
 *
 * <p>ARE/ARELine/LockDelay are constants (23/23/31). LineDelay and
 * DAS split on speedlevel: above 300 they shrink (25/15) for the
 * harder phase; below 300 they're slower (40/9).
 */
class GemManiaModeSetSpeedTest {

	@Test
	void always20gPegsGravityAtMinusOneIgnoringSpeedlevel() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", true);
		setInt(mode, "speedlevel", 0);
		engine.speed.gravity = 99999;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"always20g -> gravity = -1 instant fall");
	}

	@Test
	void speedlevelZeroLeavesGravityIndexAtZero() throws Exception {
		// speedlevel 0 < tableGravityChangeLevel[0]=20 -> gravityindex
		// stays at 0 -> gravity = tableGravityValue[0] = 4.
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		setInt(mode, "speedlevel", 0);

		invokeSetSpeed(mode, engine);

		assertEquals(4, engine.speed.gravity);
	}

	@Test
	void speedlevelTwentyAdvancesPastFirstThreshold() throws Exception {
		// speedlevel 20 == threshold[0] -> gravityindex 1 -> gravity 32.
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		setInt(mode, "speedlevel", 20);

		invokeSetSpeed(mode, engine);

		assertEquals(32, engine.speed.gravity);
	}

	@Test
	void speedlevelOneHundredAdvancesPastEightThresholds() throws Exception {
		// thresholds at 20, 30, 33, 36, 39, 43, 47, 51, 100, ... ->
		// 100 advances past 9 thresholds -> gravityindex 9 -> 512.
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		setInt(mode, "speedlevel", 100);

		invokeSetSpeed(mode, engine);

		assertEquals(512, engine.speed.gravity);
	}

	@Test
	void areAndAreLineAndLockDelayAreConstants() throws Exception {
		// ARE=23, ARELine=23, LockDelay=31 regardless of speedlevel.
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		setInt(mode, "speedlevel", 0);

		invokeSetSpeed(mode, engine);

		assertEquals(23, engine.speed.are);
		assertEquals(23, engine.speed.areLine);
		assertEquals(31, engine.speed.lockDelay);
	}

	@Test
	void belowThreeHundredLineDelayIsFortyAndDasIsNine() throws Exception {
		// speedlevel 0 < 300 -> LineDelay=40, DAS=9.
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		setInt(mode, "speedlevel", 0);

		invokeSetSpeed(mode, engine);

		assertEquals(40, engine.speed.lineDelay);
		assertEquals(9, engine.speed.das);
	}

	@Test
	void atThreeHundredLineDelayShrinksToTwentyFiveAndDasToFifteen() throws Exception {
		// speedlevel >= 300 -> LineDelay=25, DAS=15.
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		setInt(mode, "speedlevel", 300);

		invokeSetSpeed(mode, engine);

		assertEquals(25, engine.speed.lineDelay);
		assertEquals(15, engine.speed.das);
	}

	@Test
	void aboveThreeHundredKeepsTheTighterDelays() throws Exception {
		// speedlevel 999 well above 300 -> LineDelay=25, DAS=15.
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		setInt(mode, "speedlevel", 999);

		invokeSetSpeed(mode, engine);

		assertEquals(25, engine.speed.lineDelay);
		assertEquals(15, engine.speed.das);
	}

	private static GameEngine freshEngine(GemManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(GemManiaMode mode, GameEngine engine)
			throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod(
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
