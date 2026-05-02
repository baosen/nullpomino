package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GradeMania3Mode}'s private {@code setStartBgmlv}. Unlike
 * the other Mania modes (which walk against {@code statistics.level}),
 * GradeMania3 uses its dedicated {@code internalLevel} field — the
 * uncapped GM3 internal level counter that climbs past the visible
 * level cap. Walks {@code tableBGMChange = {500, 700, -1}}.
 */
class GradeMania3ModeSetStartBgmlvTest {

	@Test
	void internalLevelZeroLeavesBgmlvAtZero() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "internalLevel", 0);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"internalLevel 0 < threshold[0]=500 -> bgmlv 0");
	}

	@Test
	void internalLevelFourNinetyNineStaysBeforeFirstThreshold() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "internalLevel", 499);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"));
	}

	@Test
	void internalLevelFiveHundredAdvancesPastFirstThreshold() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "internalLevel", 500);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"),
				"internalLevel 500 crosses first threshold -> bgmlv 1");
	}

	@Test
	void internalLevelSixNinetyNineStaysBeforeSecondThreshold() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "internalLevel", 699);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"));
	}

	@Test
	void internalLevelSevenHundredAdvancesPastSecondThreshold() throws Exception {
		// 700: 700>=500 -> 1, 700>=700 -> 2, then -1 sentinel.
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "internalLevel", 700);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(2, getInt(mode, "bgmlv"),
				"internalLevel 700 crosses second threshold -> bgmlv 2");
	}

	@Test
	void veryHighInternalLevelStopsAtSentinel() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "internalLevel", 9999);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(2, getInt(mode, "bgmlv"),
				"-1 sentinel pins bgmlv at 2 regardless of internalLevel");
	}

	@Test
	void engineStatisticsLevelIsIgnored() throws Exception {
		// Critical distinction from the other Mania modes: GradeMania3
		// reads internalLevel, NOT engine.statistics.level. A high
		// statistics.level with internalLevel=0 should keep bgmlv=0.
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "internalLevel", 0);
		engine.statistics.level = 9999;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"GradeMania3 reads internalLevel, not statistics.level — "
						+ "the visible level cap is decoupled from BGM staging");
	}

	@Test
	void setStartBgmlvResetsBgmlvBeforeWalking() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "bgmlv", 5);
		setInt(mode, "internalLevel", 0);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"setStartBgmlv resets bgmlv before walking");
	}

	private static GameEngine freshEngine(GradeMania3Mode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetStartBgmlv(GradeMania3Mode mode, GameEngine engine)
			throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod(
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
