package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link GradeManiaMode}: checkRanking
 * (grade-primary/level/time ordering), updateRanking, and
 * updateBestSectionTime.
 */
class GradeManiaModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — grade primary, level secondary, time tiebreaker
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenGradeLosesToAll() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		freshEngine(mode);

		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		for(int i = 0; i < 10; i++) rGrade[i] = 20;

		// grade=5 can't beat grade=20 slots
		int rank = invokeCheckRanking(mode, 5, 999, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenGradeBeatsAll() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		freshEngine(mode);
		// All zero-initialized -> grade=1 beats all

		int rank = invokeCheckRanking(mode, 1, 0, 99999);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesLevelAsSecondaryKey() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		freshEngine(mode);

		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		rGrade[0] = 10;
		rLevel[0] = 700;

		// same grade, higher level -> rank 0
		int rank = invokeCheckRanking(mode, 10, 701, 99999);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsTiebreakerWhenGradeAndLevelEqual() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		freshEngine(mode);

		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		int[] rTime  = (int[]) readField(mode, "rankingTime");
		rGrade[0] = 8;
		rLevel[0] = 600;
		rTime[0]  = 9000;

		// same grade and level, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 8, 600, 8999);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		freshEngine(mode);

		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		rGrade[0] = 10;

		// Better grade
		invokeUpdateRanking(mode, 11, 0, 99999);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] gradeAfter = (int[]) readField(mode, "rankingGrade");
		assertEquals(11, gradeAfter[0]);
		assertEquals(10, gradeAfter[1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		freshEngine(mode);

		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		for(int i = 0; i < 10; i++) rGrade[i] = 30;

		invokeUpdateRanking(mode, 5, 0, 99999);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// updateBestSectionTime
	// -----------------------------------------------------------------------

	@Test
	void updateBestSectionTimeCopiesOnlyNewRecordSections() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		freshEngine(mode);

		int[] st    = (int[]) readField(mode, "sectiontime");
		int[] bst   = (int[]) readField(mode, "bestSectionTime");
		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");

		st[2]    = 1800;
		bst[2]   = 2400;
		isNew[2] = true;

		st[3]    = 1000;
		bst[3]   = 1200;
		isNew[3] = false;

		invokeUpdateBestSectionTime(mode);

		int[] bstAfter = (int[]) readField(mode, "bestSectionTime");
		assertEquals(1800, bstAfter[2], "new-record section updated");
		assertEquals(1200, bstAfter[3], "non-new-record section unchanged");
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

	private static int invokeCheckRanking(GradeManiaMode mode, int gr, int lv, int time)
			throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, gr, lv, time);
	}

	private static void invokeUpdateRanking(GradeManiaMode mode, int gr, int lv, int time)
			throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time);
	}

	private static void invokeUpdateBestSectionTime(GradeManiaMode mode) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
