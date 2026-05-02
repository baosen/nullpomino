package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link PhantomManiaMode}:
 * checkRanking and updateRanking (grade/level/time/rollclear ordering)
 * and updateBestSectionTime.
 */
class PhantomManiaModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenEntryIsWorseOnAllCriteria() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		freshEngine(mode);

		// Fill all 10 slots with clear=1, grade=20, lv=999, time=1000
		int[] rGrade    = (int[]) readField(mode, "rankingGrade");
		int[] rLevel    = (int[]) readField(mode, "rankingLevel");
		int[] rTime     = (int[]) readField(mode, "rankingTime");
		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		for(int i = 0; i < 10; i++) {
			rGrade[i]     = 20;
			rLevel[i]     = 999;
			rTime[i]      = 1000;
			rRollclear[i] = 1;
		}

		// Candidate: clear=0, grade=20, lv=999, time=999 -> clear is lower -> rank -1
		int rank = invokeCheckRanking(mode, 20, 999, 999, 0);

		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenRollclearBeatsAll() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		freshEngine(mode);
		// All zero-initialized -> candidate with clear=1 beats all

		int rank = invokeCheckRanking(mode, 0, 0, 99999, 1);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenGradeBeatsTopEntry() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		freshEngine(mode);

		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		rRollclear[0] = 1;
		rGrade[0]     = 10;

		// Same clear, higher grade -> rank 0
		int rank = invokeCheckRanking(mode, 11, 0, 99999, 1);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsTiebreakerWhenGradeAndLevelEqual() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		freshEngine(mode);

		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		int[] rLevel     = (int[]) readField(mode, "rankingLevel");
		int[] rTime      = (int[]) readField(mode, "rankingTime");
		rRollclear[0] = 1;
		rGrade[0]     = 15;
		rLevel[0]     = 500;
		rTime[0]      = 8000;

		// Same clear, same grade, same level, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 15, 500, 7999, 1);

		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExistingEntry() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		freshEngine(mode);

		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		int[] rLevel     = (int[]) readField(mode, "rankingLevel");
		int[] rTime      = (int[]) readField(mode, "rankingTime");
		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		rGrade[0]     = 10;
		rLevel[0]     = 400;
		rTime[0]      = 5000;
		rRollclear[0] = 1;

		// Better: same clear, higher grade
		invokeUpdateRanking(mode, 12, 400, 5000, 1);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] gradeAfter = (int[]) readField(mode, "rankingGrade");
		assertEquals(12, gradeAfter[0], "new entry at rank 0");
		assertEquals(10, gradeAfter[1], "old rank 0 shifted to rank 1");
	}

	@Test
	void updateRankingSetsMinus1WhenEntryDoesNotQualify() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		freshEngine(mode);

		// Fill all slots with clear=1, grade=20 -> any clear=0 entry won't qualify
		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		for(int i = 0; i < 10; i++) {
			rRollclear[i] = 1;
			rGrade[i]     = 20;
		}

		invokeUpdateRanking(mode, 25, 999, 0, 0);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// updateBestSectionTime
	// -----------------------------------------------------------------------

	@Test
	void updateBestSectionTimeOnlyCopiesSectionsMarkedAsNewRecord() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		freshEngine(mode);

		int[] st  = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");

		st[0]    = 1500;
		bst[0]   = 2000;
		isNew[0] = true;   // should update

		st[1]    = 800;
		bst[1]   = 900;
		isNew[1] = false;  // should NOT update

		invokeUpdateBestSectionTime(mode);

		int[] bstAfter = (int[]) readField(mode, "bestSectionTime");
		assertEquals(1500, bstAfter[0], "new record section must be updated");
		assertEquals(900, bstAfter[1], "non-record section must not change");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(PhantomManiaMode mode) {
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

	private static Object readField(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
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

	private static int invokeCheckRanking(PhantomManiaMode mode, int gr, int lv, int time, int clear)
			throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, gr, lv, time, clear);
	}

	private static void invokeUpdateRanking(PhantomManiaMode mode, int gr, int lv, int time, int clear)
			throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time, clear);
	}

	private static void invokeUpdateBestSectionTime(PhantomManiaMode mode) throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
