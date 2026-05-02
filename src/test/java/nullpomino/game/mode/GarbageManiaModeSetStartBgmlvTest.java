package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GarbageManiaMode}'s private {@code setStartBgmlv}.
 * Same shape as the canonical Mania family setStartBgmlv (walks
 * against {@code statistics.level} until either crossing a threshold
 * or hitting the -1 sentinel) but with three thresholds:
 * {@code tableBGMChange = {500, 700, 900, -1}}. The third tier at
 * level 900 separates Garbage Mania from the simpler 2-tier modes.
 */
class GarbageManiaModeSetStartBgmlvTest {

	@Test
	void levelZeroLeavesBgmlvAtZero() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"));
	}

	@Test
	void levelFourNinetyNineStaysBeforeFirstThreshold() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 499;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"));
	}

	@Test
	void levelFiveHundredAdvancesPastFirstThreshold() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 500;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"));
	}

	@Test
	void levelSevenHundredAdvancesPastSecondThreshold() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 700;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(2, getInt(mode, "bgmlv"));
	}

	@Test
	void levelNineHundredAdvancesPastThirdThreshold() throws Exception {
		// 900: crosses all 3 thresholds -> bgmlv 3, -1 sentinel stops.
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 900;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(3, getInt(mode, "bgmlv"),
				"level 900 crosses all 3 thresholds -> bgmlv 3 (third tier, "
						+ "distinct from simpler 2-tier Mania modes)");
	}

	@Test
	void veryHighLevelStopsAtSentinelEntry() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 9999;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(3, getInt(mode, "bgmlv"),
				"-1 sentinel pins bgmlv at 3 regardless of level");
	}

	@Test
	void setStartBgmlvResetsBgmlvBeforeWalking() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "bgmlv", 5);
		engine.statistics.level = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"setStartBgmlv resets bgmlv before walking");
	}

	private static GameEngine freshEngine(GarbageManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetStartBgmlv(GarbageManiaMode mode, GameEngine engine)
			throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod(
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
