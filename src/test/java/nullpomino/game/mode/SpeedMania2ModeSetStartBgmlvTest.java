package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link SpeedMania2Mode}'s private {@code setStartBgmlv}. Same
 * shape as the canonical Mania family setStartBgmlv (walks against
 * {@code statistics.level} until either crossing a threshold or
 * hitting the -1 sentinel), with three thresholds:
 * {@code tableBGMChange = {500, 700, 1000, -1}}. The third tier at
 * level 1000 is what separates SpeedMania2 from GarbageMania (which
 * uses 900) and from the simpler 2-tier modes (SpeedMania uses
 * {300, 500, -1}).
 */
class SpeedMania2ModeSetStartBgmlvTest {

	@Test
	void levelZeroLeavesBgmlvAtZero() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"));
	}

	@Test
	void levelFourNinetyNineStaysBeforeFirstThreshold() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 499;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"));
	}

	@Test
	void levelFiveHundredCrossesFirstThreshold() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 500;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"));
	}

	@Test
	void levelSevenHundredCrossesSecondThreshold() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 700;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(2, getInt(mode, "bgmlv"));
	}

	@Test
	void levelNineHundredNinetyNineStaysBeforeThirdThreshold() throws Exception {
		// 999: crosses 500 and 700 thresholds -> bgmlv 2, but 999<1000.
		// (Garbage Mania would already be at bgmlv 3 here since its
		// third threshold is 900.)
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 999;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(2, getInt(mode, "bgmlv"),
				"level 999 < threshold[2]=1000 -> bgmlv 2 (Garbage Mania "
						+ "would already be at 3 since its 3rd threshold is 900)");
	}

	@Test
	void levelOneThousandCrossesThirdThreshold() throws Exception {
		// 1000: crosses all 3 thresholds -> bgmlv 3, then -1 sentinel stops.
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 1000;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(3, getInt(mode, "bgmlv"),
				"level 1000 crosses all 3 thresholds -> bgmlv 3");
	}

	@Test
	void veryHighLevelStopsAtSentinel() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 9999;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(3, getInt(mode, "bgmlv"));
	}

	@Test
	void setStartBgmlvResetsBgmlvBeforeWalking() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "bgmlv", 5);
		engine.statistics.level = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"setStartBgmlv resets bgmlv before walking");
	}

	private static GameEngine freshEngine(SpeedMania2Mode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetStartBgmlv(SpeedMania2Mode mode, GameEngine engine)
			throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod(
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
