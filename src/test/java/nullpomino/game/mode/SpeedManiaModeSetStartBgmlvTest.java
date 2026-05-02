package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link SpeedManiaMode}'s private {@code setStartBgmlv}, the
 * "advance the BGM slot to match the starting level" helper called
 * once at game-start. Walks {@code tableBGMChange = {300, 500, -1}}
 * comparing each threshold against {@code statistics.level} and stops
 * either when level drops below the threshold or when it hits the -1
 * sentinel. The resulting bgmlv is one of 0/1/2.
 */
class SpeedManiaModeSetStartBgmlvTest {

	@Test
	void levelZeroLeavesBgmlvAtZero() throws Exception {
		// level 0 < threshold[0]=300 -> bgmlv stays 0.
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "bgmlv", 99);
		engine.statistics.level = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"setStartBgmlv resets and walks from 0; level 0 keeps it 0");
	}

	@Test
	void levelTwoNinetyNineStaysBeforeFirstThreshold() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 299;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"level 299 < 300 -> bgmlv 0");
	}

	@Test
	void levelThreeHundredAdvancesPastFirstThreshold() throws Exception {
		// level 300 >= 300 -> advance to 1; 300 < 500 -> stop.
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 300;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"),
				"level 300 crosses first threshold -> bgmlv 1");
	}

	@Test
	void levelFourNinetyNineStaysBeforeSecondThreshold() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 499;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"));
	}

	@Test
	void levelFiveHundredAdvancesPastSecondThreshold() throws Exception {
		// level 500: 500 >= 300 -> 1, 500 >= 500 -> 2, then -1 sentinel stops.
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 500;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(2, getInt(mode, "bgmlv"),
				"level 500 crosses second threshold -> bgmlv 2");
	}

	@Test
	void veryHighLevelStopsAtSentinelEntry() throws Exception {
		// Level 9999: walk past both thresholds, then the -1 sentinel
		// short-circuits the loop -> bgmlv 2.
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 9999;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(2, getInt(mode, "bgmlv"),
				"-1 sentinel pins bgmlv at 2 regardless of how high level goes");
	}

	@Test
	void setStartBgmlvResetsBgmlvBeforeWalking() throws Exception {
		// Pre-spoil bgmlv to a non-zero value to confirm the helper
		// re-initializes from 0 each call (this is the contract — the
		// table walk is a stateless one-shot).
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "bgmlv", 5);
		engine.statistics.level = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"setStartBgmlv resets bgmlv to 0 before the walk; level 0 keeps it 0");
	}

	private static GameEngine freshEngine(SpeedManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetStartBgmlv(SpeedManiaMode mode, GameEngine engine)
			throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod(
				"setStartBgmlv", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void setInt(Object instance, String name, int value)
			throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static int getInt(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getInt(instance);
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
