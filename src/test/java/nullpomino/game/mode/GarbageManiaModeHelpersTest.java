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
 * Covers the private helpers in {@link GarbageManiaMode} that are not
 * exercised by the existing replay-based tests: setAverageSectionTime
 * (positive and zero-sectionscomp branches) and stNewRecordCheck (record
 * and no-record cases).
 */
class GarbageManiaModeHelpersTest {

	// -----------------------------------------------------------------------
	// setAverageSectionTime
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeComputesAverageWhenSectionsAreCompleted() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 0);
		setInt(mode, "sectionscomp", 3);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 600;
		st[1] = 900;
		st[2] = 1200;

		invokeSetAverageSectionTime(mode);

		assertEquals(900, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenNoSectionsCompleted() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 5555);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// stNewRecordCheck
	// -----------------------------------------------------------------------

	@Test
	void stNewRecordCheckFlagsNewRecordWhenSectionTimeBeatsBest() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		st[2] = 2999;
		bst[2] = 3000;

		invokeStNewRecordCheck(mode, 2);

		assertTrue(readBoolean(mode, "sectionAnyNewRecord"));
		assertTrue(((boolean[]) readField(mode, "sectionIsNewRecord"))[2]);
	}

	@Test
	void stNewRecordCheckDoesNotFlagWhenSectionTimeMeetsBestExactly() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		st[1] = 3000;
		bst[1] = 3000;  // equal, not less than

		invokeStNewRecordCheck(mode, 1);

		assertFalse(readBoolean(mode, "sectionAnyNewRecord"));
		assertFalse(((boolean[]) readField(mode, "sectionIsNewRecord"))[1]);
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GarbageManiaMode mode) {
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

	private static void invokeSetAverageSectionTime(GarbageManiaMode mode) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeStNewRecordCheck(GarbageManiaMode mode, int sectionNumber) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod("stNewRecordCheck", int.class);
		m.setAccessible(true);
		m.invoke(mode, sectionNumber);
	}
}
