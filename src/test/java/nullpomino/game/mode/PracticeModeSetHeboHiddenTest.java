package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link PracticeMode}'s private {@code setHeboHidden}, the
 * configuration switch for the "hebo hidden" gimmick (rows progressively
 * fade out from the top of the field at increasing speeds). Level 0
 * disables the effect entirely; levels 1-7 each set a distinct
 * {@code heboHiddenYLimit} (top row past which fade kicks in) and a
 * timer formula that consumes {@code heboHiddenYNow} (rows currently
 * faded out).
 */
class PracticeModeSetHeboHiddenTest {

	@Test
	void levelZeroDisablesHeboHidden() throws Exception {
		// Level 0 -> heboHiddenEnable cleared; YLimit/TimerMax untouched.
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 0);
		// Spoil engine state so we can prove the disable path doesn't write.
		engine.heboHiddenEnable = true;
		engine.heboHiddenYLimit = 99;
		engine.heboHiddenTimerMax = 999;

		invokeSetHeboHidden(mode, engine);

		assertFalse(engine.heboHiddenEnable,
				"level 0 -> heboHiddenEnable cleared");
	}

	@Test
	void levelOneSetsYLimitFifteenAndTwoFrameOffsetTimer() throws Exception {
		// Level 1 -> YLimit 15; TimerMax = (YNow + 2) * 120.
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 1);
		engine.heboHiddenYNow = 3;

		invokeSetHeboHidden(mode, engine);

		assertTrue(engine.heboHiddenEnable);
		assertEquals(15, engine.heboHiddenYLimit);
		assertEquals((3 + 2) * 120, engine.heboHiddenTimerMax);
	}

	@Test
	void levelTwoSetsYLimitSeventeenAndOneFrameOffsetTimer() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 2);
		engine.heboHiddenYNow = 2;

		invokeSetHeboHidden(mode, engine);

		assertTrue(engine.heboHiddenEnable);
		assertEquals(17, engine.heboHiddenYLimit);
		assertEquals((2 + 1) * 100, engine.heboHiddenTimerMax);
	}

	@Test
	void levelThreeSetsYLimitNineteenAndSixtyTickFloor() throws Exception {
		// Level 3 -> YLimit 19; TimerMax = YNow * 60 + 60.
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 3);
		engine.heboHiddenYNow = 4;

		invokeSetHeboHidden(mode, engine);

		assertEquals(19, engine.heboHiddenYLimit);
		assertEquals(4 * 60 + 60, engine.heboHiddenTimerMax);
	}

	@Test
	void levelFourSharesYLimitNineteenWithFasterTimer() throws Exception {
		// Level 4 -> YLimit 19; TimerMax = YNow * 30 + 45.
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 4);
		engine.heboHiddenYNow = 5;

		invokeSetHeboHidden(mode, engine);

		assertEquals(19, engine.heboHiddenYLimit);
		assertEquals(5 * 30 + 45, engine.heboHiddenTimerMax);
	}

	@Test
	void levelFiveDropsTimerFloorToThirty() throws Exception {
		// Level 5 -> YLimit 19; TimerMax = YNow * 30 + 30.
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 5);
		engine.heboHiddenYNow = 5;

		invokeSetHeboHidden(mode, engine);

		assertEquals(19, engine.heboHiddenYLimit);
		assertEquals(5 * 30 + 30, engine.heboHiddenTimerMax);
	}

	@Test
	void levelSixDropsTimerSlopeToTwo() throws Exception {
		// Level 6 -> YLimit 19; TimerMax = YNow * 2 + 15. The 30->2 slope
		// drop is the largest jump in the table.
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 6);
		engine.heboHiddenYNow = 10;

		invokeSetHeboHidden(mode, engine);

		assertEquals(19, engine.heboHiddenYLimit);
		assertEquals(10 * 2 + 15, engine.heboHiddenTimerMax);
	}

	@Test
	void levelSevenRaisesYLimitToTwentyAndUsesUnitTimerSlope() throws Exception {
		// Level 7 (max) -> YLimit 20; TimerMax = YNow + 15. Only level
		// 7 reaches YLimit 20 -- one more than levels 3-6.
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 7);
		engine.heboHiddenYNow = 10;

		invokeSetHeboHidden(mode, engine);

		assertEquals(20, engine.heboHiddenYLimit,
				"only level 7 reaches YLimit 20");
		assertEquals(10 + 15, engine.heboHiddenTimerMax);
	}

	@Test
	void heboHiddenEnableSetEachTimeForNonZeroLevels() throws Exception {
		// Even with heboHiddenEnable already true, calling for level 5
		// should set it (idempotent on the enable flag).
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 5);
		engine.heboHiddenEnable = false;

		invokeSetHeboHidden(mode, engine);

		assertTrue(engine.heboHiddenEnable,
				"levels >= 1 unconditionally enable hebo hidden");
	}

	private static GameEngine freshEngine(PracticeMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetHeboHidden(PracticeMode mode, GameEngine engine)
			throws Exception {
		Method m = PracticeMode.class.getDeclaredMethod(
				"setHeboHidden", GameEngine.class);
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
