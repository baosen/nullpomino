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
 * Covers the private helpers in {@link ScoreAttackMode} not exercised by
 * replay-based tests: setAverageSectionTime (positive and zero-sectionscomp)
 * and stNewRecordCheck (beats-best and equal-time cases).
 */
class ScoreAttackModeHelpersTest {

	@Test
	void setAverageSectionTimeComputesAverage() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 0);
		setInt(mode, "sectionscomp", 3);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 900;
		st[1] = 1200;
		st[2] = 1500;

		invokeSetAverageSectionTime(mode);

		assertEquals(1200, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenNoSectionsCompleted() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 7777);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	@Test
	void stNewRecordCheckFlagsNewRecordWhenSectionTimeBeatsBest() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		st[0] = 1799;
		bst[0] = 1800;

		invokeStNewRecordCheck(mode, 0);

		assertTrue(readBoolean(mode, "sectionAnyNewRecord"));
		assertTrue(((boolean[]) readField(mode, "sectionIsNewRecord"))[0]);
	}

	@Test
	void stNewRecordCheckDoesNotFlagWhenSectionTimeIsEqualToBest() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		st[0] = 1800;
		bst[0] = 1800;

		invokeStNewRecordCheck(mode, 0);

		assertFalse(readBoolean(mode, "sectionAnyNewRecord"));
	}

	private static GameEngine freshEngine(ScoreAttackMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static int readInt(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static boolean readBoolean(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(instance);
	}

	private static Object readField(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
	}

	private static void setInt(Object instance, String name, int value) throws Exception {
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

	private static void invokeSetAverageSectionTime(ScoreAttackMode mode) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeStNewRecordCheck(ScoreAttackMode mode, int sectionNumber) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("stNewRecordCheck", int.class);
		m.setAccessible(true);
		m.invoke(mode, sectionNumber);
	}
}
