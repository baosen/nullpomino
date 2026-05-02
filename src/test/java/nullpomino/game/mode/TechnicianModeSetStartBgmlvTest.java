package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link TechnicianMode}'s private {@code setStartBgmlv}.
 * Distinct from the other Mania-family setStartBgmlv variants in two
 * ways: it has FIVE thresholds (most modes have 2 or 3), and the
 * thresholds are small two-digit level numbers rather than 100s of
 * level: {@code tableBGMChange = {9, 15, 19, 23, 27, -1}}. This
 * matches Technician's compact 20-entry gravity table where
 * "endgame" sits at level 17+ rather than 500+.
 */
class TechnicianModeSetStartBgmlvTest {

	@Test
	void levelZeroLeavesBgmlvAtZero() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"level 0 < threshold[0]=9 -> bgmlv 0");
	}

	@Test
	void levelEightStaysBeforeFirstThreshold() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 8;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"));
	}

	@Test
	void levelNineCrossesFirstThreshold() throws Exception {
		// Level 9 is the lowest threshold.
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 9;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"));
	}

	@Test
	void levelFifteenCrossesSecondThreshold() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 15;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(2, getInt(mode, "bgmlv"));
	}

	@Test
	void levelNineteenCrossesThirdThreshold() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 19;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(3, getInt(mode, "bgmlv"));
	}

	@Test
	void levelTwentyThreeCrossesFourthThreshold() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 23;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(4, getInt(mode, "bgmlv"));
	}

	@Test
	void levelTwentySevenCrossesAllThresholds() throws Exception {
		// All 5 thresholds crossed -> bgmlv 5, then -1 sentinel stops.
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 27;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(5, getInt(mode, "bgmlv"),
				"level 27 crosses all 5 thresholds -> bgmlv 5 (deepest BGM stage)");
	}

	@Test
	void veryHighLevelStopsAtSentinel() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 999;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(5, getInt(mode, "bgmlv"),
				"-1 sentinel pins bgmlv at 5 regardless of level");
	}

	@Test
	void setStartBgmlvResetsBgmlvBeforeWalking() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "bgmlv", 10);
		engine.statistics.level = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"setStartBgmlv resets bgmlv before walking");
	}

	private static GameEngine freshEngine(TechnicianMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetStartBgmlv(TechnicianMode mode, GameEngine engine)
			throws Exception {
		Method m = TechnicianMode.class.getDeclaredMethod(
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
