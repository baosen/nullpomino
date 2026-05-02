package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link SpeedManiaMode}: checkRanking
 * (level-primary/time-tiebreaker ordering), updateRanking (insert+shift),
 * and updateBestSectionTime.
 */
class SpeedManiaModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — level primary, time tiebreaker
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenLevelAndTimeLoseToBoth() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		freshEngine(mode);

		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		int[] rTime  = (int[]) readField(mode, "rankingTime");
		for(int i = 0; i < 10; i++) {
			rLevel[i] = 999;
			rTime[i]  = 1000;
		}

		// Candidate: lv=500, time=0 (fast but lower level) -> out of rank
		int rank = invokeCheckRanking(mode, 1, 500, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenLevelBeatsAll() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		freshEngine(mode);
		// All zero-initialized -> any lv=1 beats all

		int rank = invokeCheckRanking(mode, 1, 1, 99999);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsTiebreakerWhenLevelIsEqual() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		freshEngine(mode);

		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		int[] rTime  = (int[]) readField(mode, "rankingTime");
		rLevel[0] = 900;
		rTime[0]  = 8000;

		// Same level, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 1, 900, 7999);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		freshEngine(mode);

		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		int[] rTime  = (int[]) readField(mode, "rankingTime");
		rLevel[0] = 800;
		rTime[0]  = 5000;

		// Better: higher level
		invokeUpdateRanking(mode, 5, 900, 5000);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] lvAfter = (int[]) readField(mode, "rankingLevel");
		assertEquals(900, lvAfter[0], "new entry at rank 0");
		assertEquals(800, lvAfter[1], "old rank 0 shifted to rank 1");
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		freshEngine(mode);

		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		for(int i = 0; i < 10; i++) rLevel[i] = 999;

		invokeUpdateRanking(mode, 1, 500, 1000);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// updateBestSectionTime
	// -----------------------------------------------------------------------

	@Test
	void updateBestSectionTimeCopiesOnlyNewRecordSections() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		freshEngine(mode);

		int[] st    = (int[]) readField(mode, "sectiontime");
		int[] bst   = (int[]) readField(mode, "bestSectionTime");
		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");

		st[0]    = 2000;
		bst[0]   = 3000;
		isNew[0] = true;

		st[1]    = 1000;
		bst[1]   = 1500;
		isNew[1] = false;

		invokeUpdateBestSectionTime(mode);

		int[] bstAfter = (int[]) readField(mode, "bestSectionTime");
		assertEquals(2000, bstAfter[0], "new-record section updated");
		assertEquals(1500, bstAfter[1], "non-new-record section unchanged");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(SpeedManiaMode mode) {
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

	private static int invokeCheckRanking(SpeedManiaMode mode, int gr, int lv, int time)
			throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, gr, lv, time);
	}

	private static void invokeUpdateRanking(SpeedManiaMode mode, int gr, int lv, int time)
			throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time);
	}

	private static void invokeUpdateBestSectionTime(SpeedManiaMode mode) throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
