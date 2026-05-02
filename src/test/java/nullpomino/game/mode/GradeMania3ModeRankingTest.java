package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link GradeMania3Mode}:
 * checkRanking (grade-primary ordering with rollclear/level/time tiebreakers),
 * updateRanking (insertion and shift), and updateGradeHistory (FIFO shift).
 */
class GradeMania3ModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — grade is primary key
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenCandidateGradeIsLower() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		freshEngine(mode);

		// All 10 type-0 slots: grade=20 -> a grade=10 candidate can't enter
		int[][] rGrade = (int[][]) readField(mode, "rankingGrade");
		for(int i = 0; i < 10; i++) rGrade[i][0] = 20;

		int rank = invokeCheckRanking(mode, 10, 999, 0, 1, 0);

		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenGradeBeatsAll() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		freshEngine(mode);
		// All zero-initialized -> grade=1 beats all

		int rank = invokeCheckRanking(mode, 1, 0, 99999, 0, 0);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesRollclearAsSecondaryKey() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		freshEngine(mode);

		int[][] rGrade     = (int[][]) readField(mode, "rankingGrade");
		int[][] rRollclear = (int[][]) readField(mode, "rankingRollclear");
		rGrade[0][0]     = 10;
		rRollclear[0][0] = 0;

		// same grade=10, higher clear=1 -> rank 0
		int rank = invokeCheckRanking(mode, 10, 0, 99999, 1, 0);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesLevelAsTertiaryKey() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		freshEngine(mode);

		int[][] rGrade     = (int[][]) readField(mode, "rankingGrade");
		int[][] rRollclear = (int[][]) readField(mode, "rankingRollclear");
		int[][] rLevel     = (int[][]) readField(mode, "rankingLevel");
		rGrade[0][0]     = 8;
		rRollclear[0][0] = 1;
		rLevel[0][0]     = 700;

		// same grade, same clear, higher level -> rank 0
		int rank = invokeCheckRanking(mode, 8, 701, 99999, 1, 0);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsQuaternaryKey() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		freshEngine(mode);

		int[][] rGrade     = (int[][]) readField(mode, "rankingGrade");
		int[][] rRollclear = (int[][]) readField(mode, "rankingRollclear");
		int[][] rLevel     = (int[][]) readField(mode, "rankingLevel");
		int[][] rTime      = (int[][]) readField(mode, "rankingTime");
		rGrade[0][0]     = 12;
		rRollclear[0][0] = 1;
		rLevel[0][0]     = 999;
		rTime[0][0]      = 6000;

		// same grade/clear/level, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 12, 999, 5999, 1, 0);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingOperatesOnTypeOneSlotIndependentlyOfTypeZero() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		freshEngine(mode);

		// type-1 slots: all grade=20; type-0 slots stay 0
		int[][] rGrade = (int[][]) readField(mode, "rankingGrade");
		for(int i = 0; i < 10; i++) rGrade[i][1] = 20;

		// Grade=5 can't enter type-1
		int rank = invokeCheckRanking(mode, 5, 0, 99999, 1, 1);
		assertEquals(-1, rank);

		// Grade=5 easily enters type-0 (all zeros)
		rank = invokeCheckRanking(mode, 5, 0, 99999, 1, 0);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtZeroAndShiftsExistingEntry() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		freshEngine(mode);

		int[][] rGrade = (int[][]) readField(mode, "rankingGrade");
		int[][] rLevel = (int[][]) readField(mode, "rankingLevel");
		rGrade[0][0] = 10;
		rLevel[0][0] = 500;

		// Better grade
		invokeUpdateRanking(mode, 11, 500, 5000, 0, 0);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][] gradeAfter = (int[][]) readField(mode, "rankingGrade");
		assertEquals(11, gradeAfter[0][0], "new entry at rank 0");
		assertEquals(10, gradeAfter[1][0], "old rank 0 shifted to rank 1");
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		freshEngine(mode);

		int[][] rGrade = (int[][]) readField(mode, "rankingGrade");
		for(int i = 0; i < 10; i++) rGrade[i][0] = 30;

		invokeUpdateRanking(mode, 5, 0, 99999, 0, 0);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// updateGradeHistory
	// -----------------------------------------------------------------------

	@Test
	void updateGradeHistoryShiftsExistingEntriesAndInsertsAtFront() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		freshEngine(mode);

		int[] history = (int[]) readField(mode, "gradeHistory");
		history[0] = 5;
		history[1] = 3;
		history[2] = 1;

		invokeUpdateGradeHistory(mode, 7);

		int[] after = (int[]) readField(mode, "gradeHistory");
		assertEquals(7, after[0], "new grade at index 0");
		assertEquals(5, after[1], "previous [0] shifted to [1]");
		assertEquals(3, after[2], "previous [1] shifted to [2]");
		assertEquals(1, after[3], "previous [2] shifted to [3]");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GradeMania3Mode mode) {
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

	private static int invokeCheckRanking(GradeMania3Mode mode,
			int gr, int lv, int time, int clear, int type) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, gr, lv, time, clear, type);
	}

	private static void invokeUpdateRanking(GradeMania3Mode mode,
			int gr, int lv, int time, int clear, int type) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time, clear, type);
	}

	private static void invokeUpdateGradeHistory(GradeMania3Mode mode, int gr) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("updateGradeHistory", int.class);
		m.setAccessible(true);
		m.invoke(mode, gr);
	}
}
