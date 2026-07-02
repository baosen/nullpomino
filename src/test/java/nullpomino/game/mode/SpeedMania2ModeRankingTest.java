package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link SpeedMania2Mode}: checkRanking
 * (rollclear-primary, grade, level, faster-time), updateRanking, and
 * updateBestSectionTime.
 */
class SpeedMania2ModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — rollclear primary, grade, level, faster time
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenRollclearLosesToAll() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);

		int[] rClear = (int[]) readField(mode, "rankingRollclear");
		for(int i = 0; i < 10; i++) rClear[i] = 2;

		int rank = invokeCheckRanking(mode, 0, 0, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenRollclearBeatsAll() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);
		// All zero-initialized -> rollclear=1 beats all

		int rank = invokeCheckRanking(mode, 0, 0, 0, 1);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesGradeAsSecondaryKey() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
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
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);

		int[] rClear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		rClear[0] = 1;
		rGrade[0] = 5;
		rLevel[0] = 200;

		// same rollclear and grade, higher level -> rank 0
		int rank = invokeCheckRanking(mode, 5, 201, 0, 1);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesFasterTimeAsQuaternaryKey() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);

		int[] rClear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade = (int[]) readField(mode, "rankingGrade");
		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		int[] rTime  = (int[]) readField(mode, "rankingTime");
		rClear[0] = 1;
		rGrade[0] = 5;
		rLevel[0] = 200;
		rTime[0]  = 10800;

		int rank = invokeCheckRanking(mode, 5, 200, 10799, 1);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
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
		SpeedMania2Mode mode = new SpeedMania2Mode();
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
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);

		int[] st     = (int[]) readField(mode, "sectiontime");
		int[] bst    = (int[]) readField(mode, "bestSectionTime");
		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");

		st[1]    = 1600;
		bst[1]   = 2000;
		isNew[1] = true;

		st[2]    = 700;
		bst[2]   = 900;
		isNew[2] = false;

		invokeUpdateBestSectionTime(mode);

		int[] bstAfter = (int[]) readField(mode, "bestSectionTime");
		assertEquals(1600, bstAfter[1], "new-record section updated");
		assertEquals(900, bstAfter[2], "non-new-record section unchanged");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(SpeedMania2Mode mode) {
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

	private static int invokeCheckRanking(SpeedMania2Mode mode, int gr, int lv, int time, int clear)
			throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, gr, lv, time, clear);
	}

	private static void invokeUpdateRanking(SpeedMania2Mode mode, int gr, int lv, int time, int clear)
			throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time, clear);
	}

	private static void invokeUpdateBestSectionTime(SpeedMania2Mode mode) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
