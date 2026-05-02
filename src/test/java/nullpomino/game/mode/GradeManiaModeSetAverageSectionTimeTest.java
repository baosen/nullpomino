package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GradeManiaMode}'s private {@code setAverageSectionTime}.
 * Sums {@code sectiontime[startlevel.value .. startlevel.value +
 * sectionscomp - 1]} and divides by {@code sectionscomp} to compute
 * {@code sectionavgtime}. Skips slots whose index falls outside
 * [0, sectiontime.length). When {@code sectionscomp == 0} the
 * average is 0 (no division-by-zero).
 */
class GradeManiaModeSetAverageSectionTimeTest {

	@Test
	void zeroCompletedSectionsLeavesAverageAtZero() throws Exception {
		// sectionscomp=0 -> short-circuit avoids div-by-zero.
		GradeManiaMode mode = new GradeManiaMode();
		freshGameManager(mode);
		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 99999);

		invokeSetAverage(mode);

		assertEquals(0, getInt(mode, "sectionavgtime"),
				"sectionscomp=0 -> sectionavgtime forced to 0");
	}

	@Test
	void averagesAcrossCompletedSectionsStartingAtStartlevel() throws Exception {
		// startlevel=2, sectionscomp=3 -> sums sectiontime[2,3,4] / 3.
		GradeManiaMode mode = new GradeManiaMode();
		freshGameManager(mode);
		// Stamp the section times.
		int[] sectiontime = (int[]) read(mode, "sectiontime");
		sectiontime[2] = 6000;
		sectiontime[3] = 5400;
		sectiontime[4] = 6600;
		IntegerMenuItem startlevel = (IntegerMenuItem) read(mode, "startlevel");
		startlevel.value = 2;
		setInt(mode, "sectionscomp", 3);

		invokeSetAverage(mode);

		assertEquals((6000 + 5400 + 6600) / 3, getInt(mode, "sectionavgtime"),
				"average over the 3 sections starting at startlevel=2");
	}

	@Test
	void integerDivisionTruncatesTowardZero() throws Exception {
		// 10 / 3 = 3 (Java integer division).
		GradeManiaMode mode = new GradeManiaMode();
		freshGameManager(mode);
		int[] sectiontime = (int[]) read(mode, "sectiontime");
		sectiontime[0] = 10;
		IntegerMenuItem startlevel = (IntegerMenuItem) read(mode, "startlevel");
		startlevel.value = 0;
		setInt(mode, "sectionscomp", 3);

		invokeSetAverage(mode);

		// Only sectiontime[0]=10, sectiontime[1]=0, sectiontime[2]=0.
		// Sum = 10. 10 / 3 = 3.
		assertEquals(3, getInt(mode, "sectionavgtime"),
				"int division: 10 / 3 = 3 (truncates toward zero)");
	}

	@Test
	void outOfBoundsIndicesAreSkipped() throws Exception {
		// startlevel=8, sectionscomp=5 -> would walk indices 8..12,
		// but sectiontime only has SECTION_MAX=10 slots. Indices 10,
		// 11, 12 are skipped (the `i < sectiontime.length` guard).
		GradeManiaMode mode = new GradeManiaMode();
		freshGameManager(mode);
		int[] sectiontime = (int[]) read(mode, "sectiontime");
		sectiontime[8] = 1000;
		sectiontime[9] = 2000;
		IntegerMenuItem startlevel = (IntegerMenuItem) read(mode, "startlevel");
		startlevel.value = 8;
		setInt(mode, "sectionscomp", 5);

		invokeSetAverage(mode);

		// Sum = 1000 + 2000 = 3000. Divided by sectionscomp=5 = 600.
		assertEquals((1000 + 2000) / 5, getInt(mode, "sectionavgtime"),
				"OOB indices skipped, denominator stays at sectionscomp=5");
	}

	@Test
	void negativeStartlevelStillSkipsOutOfBoundsIndices() throws Exception {
		// startlevel=-2, sectionscomp=4 -> walks indices -2,-1,0,1.
		// Negative indices are skipped, [0..1] contribute.
		GradeManiaMode mode = new GradeManiaMode();
		freshGameManager(mode);
		int[] sectiontime = (int[]) read(mode, "sectiontime");
		sectiontime[0] = 100;
		sectiontime[1] = 200;
		IntegerMenuItem startlevel = (IntegerMenuItem) read(mode, "startlevel");
		startlevel.value = -2;
		setInt(mode, "sectionscomp", 4);

		invokeSetAverage(mode);

		assertEquals((100 + 200) / 4, getInt(mode, "sectionavgtime"),
				"negative startlevel: OOB indices skipped, denominator unchanged");
	}

	private static void freshGameManager(GradeManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
	}

	private static void invokeSetAverage(GradeManiaMode mode) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod(
				"setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static int getInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static Object read(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
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
