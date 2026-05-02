package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link GarbageManiaMode}: checkRanking
 * (level-primary/time-tiebreaker ordering), updateRanking, and
 * updateBestSectionTime.
 */
class GarbageManiaModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — level primary, time tiebreaker
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenLevelLosesToAll() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		freshEngine(mode);

		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		for(int i = 0; i < 10; i++) rLevel[i] = 99;

		// lv=50 can't beat lv=99 slots
		int rank = invokeCheckRanking(mode, 50, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenLevelBeatsAll() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		freshEngine(mode);
		// All zero-initialized -> lv=1 beats all

		int rank = invokeCheckRanking(mode, 1, 99999);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsTiebreakerWhenLevelIsEqual() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		freshEngine(mode);

		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		int[] rTime  = (int[]) readField(mode, "rankingTime");
		rLevel[0] = 75;
		rTime[0]  = 6000;

		// same level, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 75, 5999);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		freshEngine(mode);

		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		rLevel[0] = 80;

		invokeUpdateRanking(mode, 90, 5000);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] lvAfter = (int[]) readField(mode, "rankingLevel");
		assertEquals(90, lvAfter[0]);
		assertEquals(80, lvAfter[1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		freshEngine(mode);

		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		for(int i = 0; i < 10; i++) rLevel[i] = 99;

		invokeUpdateRanking(mode, 50, 1000);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// updateBestSectionTime
	// -----------------------------------------------------------------------

	@Test
	void updateBestSectionTimeCopiesOnlyNewRecordSections() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		freshEngine(mode);

		int[] st    = (int[]) readField(mode, "sectiontime");
		int[] bst   = (int[]) readField(mode, "bestSectionTime");
		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");

		st[4]    = 1500;
		bst[4]   = 2000;
		isNew[4] = true;

		st[5]    = 800;
		bst[5]   = 1000;
		isNew[5] = false;

		invokeUpdateBestSectionTime(mode);

		int[] bstAfter = (int[]) readField(mode, "bestSectionTime");
		assertEquals(1500, bstAfter[4], "new-record section updated");
		assertEquals(1000, bstAfter[5], "non-new-record section unchanged");
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

	private static int invokeCheckRanking(GarbageManiaMode mode, int lv, int time)
			throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, lv, time);
	}

	private static void invokeUpdateRanking(GarbageManiaMode mode, int lv, int time)
			throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, lv, time);
	}

	private static void invokeUpdateBestSectionTime(GarbageManiaMode mode) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
