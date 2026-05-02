package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link MarathonPlusMode}'s private {@code setStartBgmlv}, the
 * once-at-game-start helper that picks the BGM slot. Differs from the
 * Mania-family setStartBgmlv in two ways: it walks against
 * {@code engine.statistics.lines} (not level), and it has a fast-path
 * branch — when {@code startlevel >= 20} (i.e. starting in the bonus
 * stage) it short-circuits to bgmlv=4 without walking the table.
 * Otherwise walks {@code tableBGMChange = {50, 100, 150, 200, -1}}.
 */
class MarathonPlusModeSetStartBgmlvTest {

	@Test
	void startlevelTwentyOrAboveShortCircuitsToBgmlvFour() throws Exception {
		// Bonus-stage start (startlevel >= 20) -> bgmlv=4 regardless of
		// lines count.
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 20);
		// Lines count must NOT be consulted for this fast path.
		engine.statistics.lines = 9999;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(4, getInt(mode, "bgmlv"),
				"startlevel >= 20 -> bgmlv 4 (bonus stage) without table walk");
	}

	@Test
	void startlevelTwentyFiveAlsoTakesShortCircuit() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 25);
		engine.statistics.lines = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(4, getInt(mode, "bgmlv"),
				"startlevel 25 (>20) -> bgmlv 4");
	}

	@Test
	void linesZeroLeavesBgmlvAtZero() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 0);
		engine.statistics.lines = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"lines 0 < threshold[0]=50 -> bgmlv 0");
	}

	@Test
	void linesFortyNineStaysBeforeFirstThreshold() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 0);
		engine.statistics.lines = 49;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"));
	}

	@Test
	void linesFiftyAdvancesPastFirstThreshold() throws Exception {
		// lines 50: 50>=50 -> 1, 50<100 -> stop.
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 0);
		engine.statistics.lines = 50;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"));
	}

	@Test
	void linesOneFiftyAdvancesPastThirdThreshold() throws Exception {
		// 50, 100, 150 thresholds crossed -> bgmlv 3, then 150<200 -> stop.
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 0);
		engine.statistics.lines = 150;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(3, getInt(mode, "bgmlv"));
	}

	@Test
	void linesTwoHundredHitsLastThresholdAndStopsAtSentinel() throws Exception {
		// All 4 thresholds crossed -> bgmlv 4, then -1 sentinel stops.
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 0);
		engine.statistics.lines = 200;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(4, getInt(mode, "bgmlv"),
				"lines 200 crosses all thresholds -> bgmlv 4 (-1 sentinel stops walk)");
	}

	@Test
	void veryHighLinesStopsAtSentinelEntry() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 19);
		engine.statistics.lines = 9999;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(4, getInt(mode, "bgmlv"),
				"-1 sentinel pins bgmlv at 4 regardless of how high lines go "
						+ "(when startlevel < 20)");
	}

	@Test
	void startlevelNineteenStillUsesTableWalk() throws Exception {
		// startlevel 19 (the boundary just below the 20 short-circuit)
		// must use the table walk, not the bonus fast-path.
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 19);
		engine.statistics.lines = 75;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"),
				"startlevel 19 (boundary below short-circuit) -> walks the table");
	}

	@Test
	void setStartBgmlvResetsBgmlvBeforeWalking() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "bgmlv", 5);
		setInt(mode, "startlevel", 0);
		engine.statistics.lines = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"setStartBgmlv resets bgmlv before walking");
	}

	private static GameEngine freshEngine(MarathonPlusMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetStartBgmlv(MarathonPlusMode mode, GameEngine engine)
			throws Exception {
		Method m = MarathonPlusMode.class.getDeclaredMethod(
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
