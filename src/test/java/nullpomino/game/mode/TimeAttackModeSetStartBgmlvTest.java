package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link TimeAttackMode}'s private {@code setStartBgmlv}.
 * Different shape from the Mania family: no -1 sentinel — instead
 * the loop bounds-checks bgmlv against {@code
 * tableBGMChange[goaltype].length}. Walks against TimeAttackMode's
 * dedicated {@code norm} (lines-cleared) field, NOT
 * {@code engine.statistics.level} or {@code statistics.lines}. The
 * 2D BGM-change table is indexed by goaltype: NORMAL/HISPEED1/
 * HISPEED2 share {50, 100}; ANOTHER/ANOTHER2 share {40, 100}; the
 * 200-line variants and HELL/HELL-X use {50, 150} or {40, 150};
 * VOID has an empty array, so bgmlv stays 0 for any norm.
 */
class TimeAttackModeSetStartBgmlvTest {

	@Test
	void normalGoaltypeNormZeroLeavesBgmlvAtZero() throws Exception {
		// goaltype 0 (NORMAL) thresholds {50, 100}; norm 0 < 50 -> 0.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 0);
		setInt(mode, "norm", 0);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"));
	}

	@Test
	void normalGoaltypeNormFiftyCrossesFirstThreshold() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 0);
		setInt(mode, "norm", 50);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"),
				"NORMAL norm 50 -> bgmlv 1");
	}

	@Test
	void normalGoaltypeNormOneHundredCrossesAllThresholds() throws Exception {
		// goaltype 0 has 2 thresholds; norm 100 saturates at length=2.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 0);
		setInt(mode, "norm", 100);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(2, getInt(mode, "bgmlv"),
				"NORMAL norm 100 -> bgmlv 2 (the loop bounds-checks at "
						+ "tableBGMChange[goaltype].length=2)");
	}

	@Test
	void anotherGoaltypeUsesDistinctThresholds() throws Exception {
		// goaltype 3 (ANOTHER) thresholds {40, 100}.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 3);
		setInt(mode, "norm", 40);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"),
				"ANOTHER threshold[0]=40, norm 40 -> bgmlv 1 "
						+ "(40 line threshold, not 50 like NORMAL)");
	}

	@Test
	void normalTwoHundredHigherSecondThreshold() throws Exception {
		// goaltype 5 (NORMAL_200) thresholds {50, 150}.
		// norm 100 is past first but not second.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 5);
		setInt(mode, "norm", 100);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(1, getInt(mode, "bgmlv"),
				"NORMAL_200 second threshold is 150, so norm 100 stays at 1");

		setInt(mode, "norm", 150);
		invokeSetStartBgmlv(mode, engine);
		assertEquals(2, getInt(mode, "bgmlv"));
	}

	@Test
	void voidGoaltypeWithEmptyTableLeavesBgmlvAtZero() throws Exception {
		// goaltype 10 (VOID) has an EMPTY tableBGMChange[VOID] = {}.
		// The loop's bounds check (bgmlv < length=0) fails immediately.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 10);
		setInt(mode, "norm", 9999);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"VOID empty BGM table -> bgmlv stays 0 even at norm 9999");
	}

	@Test
	void engineStatisticsLevelAndLinesAreIgnored() throws Exception {
		// TimeAttackMode reads norm, not statistics.level/lines.
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 0);
		setInt(mode, "norm", 0);
		engine.statistics.level = 9999;
		engine.statistics.lines = 9999;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"TimeAttackMode reads norm, not statistics.level or lines");
	}

	@Test
	void setStartBgmlvResetsBgmlvBeforeWalking() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "bgmlv", 5);
		setInt(mode, "goaltype", 0);
		setInt(mode, "norm", 0);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, getInt(mode, "bgmlv"),
				"setStartBgmlv resets bgmlv before walking");
	}

	private static GameEngine freshEngine(TimeAttackMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetStartBgmlv(TimeAttackMode mode, GameEngine engine)
			throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod(
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
