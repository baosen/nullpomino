package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link FinalMode}: checkRanking
 * (rollclear → grade → level → time ordering) and updateRanking
 * (insertion + shift), and updateBestSectionTime.
 */
class FinalModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenCandidateLosesOnRollclear() throws Exception {
		FinalMode mode = new FinalMode();
		freshEngine(mode);

		// All 10 slots have clear=1; candidate has clear=0 -> out of rank
		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		for(int i = 0; i < 10; i++) {
			rRollclear[i] = 1;
			rGrade[i]     = 5;
		}

		int rank = invokeCheckRanking(mode, 10, 999, 0, 0);

		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenRollclearBeatsAllSlots() throws Exception {
		FinalMode mode = new FinalMode();
		freshEngine(mode);
		// All zero-initialized -> any clear=1 entry beats all

		int rank = invokeCheckRanking(mode, 0, 0, 99999, 1);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesGradeAsSecondaryKey() throws Exception {
		FinalMode mode = new FinalMode();
		freshEngine(mode);

		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		rRollclear[0] = 1;
		rGrade[0]     = 8;

		// same clear=1, higher grade=9 -> rank 0
		int rank = invokeCheckRanking(mode, 9, 0, 99999, 1);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesLevelAsTertiaryKey() throws Exception {
		FinalMode mode = new FinalMode();
		freshEngine(mode);

		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		int[] rLevel     = (int[]) readField(mode, "rankingLevel");
		rRollclear[0] = 1;
		rGrade[0]     = 10;
		rLevel[0]     = 400;

		// same clear, same grade, higher level -> rank 0
		int rank = invokeCheckRanking(mode, 10, 401, 99999, 1);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsQuaternaryKey() throws Exception {
		FinalMode mode = new FinalMode();
		freshEngine(mode);

		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		int[] rLevel     = (int[]) readField(mode, "rankingLevel");
		int[] rTime      = (int[]) readField(mode, "rankingTime");
		rRollclear[0] = 1;
		rGrade[0]     = 10;
		rLevel[0]     = 500;
		rTime[0]      = 7000;

		// same clear/grade/level, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 10, 500, 6999, 1);

		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsNewEntryAndShiftsExisting() throws Exception {
		FinalMode mode = new FinalMode();
		freshEngine(mode);

		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		int[] rLevel     = (int[]) readField(mode, "rankingLevel");
		int[] rTime      = (int[]) readField(mode, "rankingTime");
		rRollclear[0] = 1;
		rGrade[0]     = 10;
		rLevel[0]     = 500;
		rTime[0]      = 5000;

		// Better: same clear, higher grade
		invokeUpdateRanking(mode, 11, 500, 5000, 1);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] gradeAfter = (int[]) readField(mode, "rankingGrade");
		assertEquals(11, gradeAfter[0], "new entry at rank 0");
		assertEquals(10, gradeAfter[1], "old rank 0 moved to rank 1");
	}

	@Test
	void updateRankingSetsMinus1RankWhenOutOfRanking() throws Exception {
		FinalMode mode = new FinalMode();
		freshEngine(mode);

		// 10 slots all clear=1, grade=20 -> clear=0 entry won't make it
		int[] rRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] rGrade     = (int[]) readField(mode, "rankingGrade");
		for(int i = 0; i < 10; i++) {
			rRollclear[i] = 1;
			rGrade[i]     = 20;
		}

		invokeUpdateRanking(mode, 30, 999, 0, 0);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// updateBestSectionTime
	// -----------------------------------------------------------------------

	@Test
	void updateBestSectionTimeCopiesOnlySectionsMarkedAsNewRecord() throws Exception {
		FinalMode mode = new FinalMode();
		freshEngine(mode);

		int[] st       = (int[]) readField(mode, "sectiontime");
		int[] bst      = (int[]) readField(mode, "bestSectionTime");
		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");

		st[0]    = 1200;
		bst[0]   = 1800;
		isNew[0] = true;

		st[2]    = 999;
		bst[2]   = 1000;
		isNew[2] = false;

		invokeUpdateBestSectionTime(mode);

		int[] bstAfter = (int[]) readField(mode, "bestSectionTime");
		assertEquals(1200, bstAfter[0], "new-record section must update bestSectionTime");
		assertEquals(1000, bstAfter[2], "non-new-record section must stay unchanged");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(FinalMode mode) {
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

	private static int invokeCheckRanking(FinalMode mode, int gr, int lv, int time, int clear)
			throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, gr, lv, time, clear);
	}

	private static void invokeUpdateRanking(FinalMode mode, int gr, int lv, int time, int clear)
			throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time, clear);
	}

	private static void invokeUpdateBestSectionTime(FinalMode mode) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
