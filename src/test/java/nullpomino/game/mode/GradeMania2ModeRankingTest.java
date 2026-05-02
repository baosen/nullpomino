package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link GradeMania2Mode}: checkRanking
 * (rollclear-primary, grade, level, faster-time) and updateRanking, plus
 * updateBestSectionTime.
 */
class GradeMania2ModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — rollclear primary, grade, level, faster time
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenRollclearLosesToAll() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		freshEngine(mode);

		int[] rClear = (int[]) readField(mode, "rankingRollclear");
		for(int i = 0; i < 10; i++) rClear[i] = 2;

		// rollclear=0 can't beat rollclear=2 slots -> -1
		int rank = invokeCheckRanking(mode, 0, 0, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenRollclearBeatsAll() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		freshEngine(mode);
		// All zero-initialized -> rollclear=1 beats all

		int rank = invokeCheckRanking(mode, 0, 0, 0, 1);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesGradeAsSecondaryKey() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		freshEngine(mode);

		int[] rClear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		rClear[0] = 1;
		rGrade[0] = 5;

		// same rollclear, higher grade -> rank 0
		int rank = invokeCheckRanking(mode, 6, 0, 0, 1);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesLevelAsTertiaryKey() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		freshEngine(mode);

		int[] rClear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		rClear[0] = 1;
		rGrade[0] = 5;
		rLevel[0] = 100;

		// same rollclear and grade, higher level -> rank 0
		int rank = invokeCheckRanking(mode, 5, 101, 0, 1);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesFasterTimeAsQuaternaryKey() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		freshEngine(mode);

		int[] rClear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		int[] rTime  = (int[]) readField(mode, "rankingTime");
		rClear[0] = 1;
		rGrade[0] = 5;
		rLevel[0] = 100;
		rTime[0]  = 9000;

		// same rollclear, grade and level, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 5, 100, 8999, 1);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		freshEngine(mode);

		int[] rClear = (int[]) readField(mode, "rankingRollclear");
		rClear[0] = 1;

		invokeUpdateRanking(mode, 0, 0, 0, 2);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] clearAfter = (int[]) readField(mode, "rankingRollclear");
		assertEquals(2, clearAfter[0]);
		assertEquals(1, clearAfter[1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		freshEngine(mode);

		int[] rClear = (int[]) readField(mode, "rankingRollclear");
		for(int i = 0; i < 10; i++) rClear[i] = 2;

		invokeUpdateRanking(mode, 0, 0, 0, 0);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// updateBestSectionTime
	// -----------------------------------------------------------------------

	@Test
	void updateBestSectionTimeCopiesOnlyNewRecordSections() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		freshEngine(mode);

		int[] st     = (int[]) readField(mode, "sectiontime");
		int[] bst    = (int[]) readField(mode, "bestSectionTime");
		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");

		st[2]    = 1800;
		bst[2]   = 2400;
		isNew[2] = true;

		st[3]    = 900;
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

	private static GameEngine freshEngine(GradeMania2Mode mode) {
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

	private static int invokeCheckRanking(GradeMania2Mode mode, int gr, int lv, int time, int clear)
			throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, gr, lv, time, clear);
	}

	private static void invokeUpdateRanking(GradeMania2Mode mode, int gr, int lv, int time, int clear)
			throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time, clear);
	}

	private static void invokeUpdateBestSectionTime(GradeMania2Mode mode) throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
