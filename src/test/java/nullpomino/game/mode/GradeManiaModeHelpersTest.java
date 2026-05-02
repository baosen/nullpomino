package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private helpers in {@link GradeManiaMode} not exercised by the
 * existing ranking / settings tests: setAverageSectionTime and stNewRecordCheck.
 */
class GradeManiaModeHelpersTest {

	// -----------------------------------------------------------------------
	// setAverageSectionTime
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeComputesAverageWhenSectionsCompleted() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		startlevelValue(mode, 0);
		setInt(mode, "sectionscomp", 3);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 300;
		st[1] = 600;
		st[2] = 900;

		invokeSetAverageSectionTime(mode);

		assertEquals(600, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenNoSectionsCompleted() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 8888);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// stNewRecordCheck
	// -----------------------------------------------------------------------

	@Test
	void stNewRecordCheckFlagsNewRecordWhenSectionTimeBeatsBest() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		st[3] = 2999;
		bst[3] = 3000;

		invokeStNewRecordCheck(mode, 3);

		assertTrue(readBoolean(mode, "sectionAnyNewRecord"));
		assertTrue(((boolean[]) readField(mode, "sectionIsNewRecord"))[3]);
	}

	@Test
	void stNewRecordCheckDoesNotFlagWhenSectionTimeEqualsBest() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		st[0] = 3000;
		bst[0] = 3000;

		invokeStNewRecordCheck(mode, 0);

		assertFalse(readBoolean(mode, "sectionAnyNewRecord"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GradeManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void startlevelValue(GradeManiaMode mode, int value) throws Exception {
		Field f = findField(mode.getClass(), "startlevel");
		f.setAccessible(true);
		IntegerMenuItem item = (IntegerMenuItem) f.get(mode);
		Field vf = findField(item.getClass(), "value");
		vf.setAccessible(true);
		vf.set(item, value);
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

	private static void invokeSetAverageSectionTime(GradeManiaMode mode) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeStNewRecordCheck(GradeManiaMode mode, int sectionNumber) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod("stNewRecordCheck", int.class);
		m.setAccessible(true);
		m.invoke(mode, sectionNumber);
	}
}
