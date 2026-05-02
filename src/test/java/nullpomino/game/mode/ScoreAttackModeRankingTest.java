package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link ScoreAttackMode}: checkRanking
 * (score-primary/level/time ordering), updateRanking, and
 * updateBestSectionTime.
 */
class ScoreAttackModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — score primary, level secondary, time tiebreaker
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenScoreLosesToAll() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		freshEngine(mode);

		int[] rScore = (int[]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[i] = 1_000_000;

		int rank = invokeCheckRanking(mode, 500_000, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenScoreBeatsAll() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		freshEngine(mode);
		// All zero-initialized -> any score > 0 beats all

		int rank = invokeCheckRanking(mode, 1, 0, 99999);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesLevelAsSecondaryKey() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		freshEngine(mode);

		int[] rScore = (int[]) readField(mode, "rankingScore");
		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		rScore[0] = 500_000;
		rLevel[0] = 10;

		// same score, higher level -> rank 0
		int rank = invokeCheckRanking(mode, 500_000, 11, 99999);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsTiebreakerWhenScoreAndLevelEqual() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		freshEngine(mode);

		int[] rScore = (int[]) readField(mode, "rankingScore");
		int[] rLevel = (int[]) readField(mode, "rankingLevel");
		int[] rTime  = (int[]) readField(mode, "rankingTime");
		rScore[0] = 800_000;
		rLevel[0] = 15;
		rTime[0]  = 7200;

		// same score and level, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 800_000, 15, 7199);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		freshEngine(mode);

		int[] rScore = (int[]) readField(mode, "rankingScore");
		rScore[0] = 500_000;

		invokeUpdateRanking(mode, 600_000, 0, 99999);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] scoreAfter = (int[]) readField(mode, "rankingScore");
		assertEquals(600_000, scoreAfter[0]);
		assertEquals(500_000, scoreAfter[1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		freshEngine(mode);

		int[] rScore = (int[]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[i] = 1_000_000;

		invokeUpdateRanking(mode, 100_000, 0, 99999);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// updateBestSectionTime
	// -----------------------------------------------------------------------

	@Test
	void updateBestSectionTimeCopiesOnlyNewRecordSections() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		freshEngine(mode);

		int[] st    = (int[]) readField(mode, "sectiontime");
		int[] bst   = (int[]) readField(mode, "bestSectionTime");
		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");

		st[0]    = 2400;
		bst[0]   = 3000;
		isNew[0] = true;

		st[1]    = 1000;
		bst[1]   = 1500;
		isNew[1] = false;

		invokeUpdateBestSectionTime(mode);

		int[] bstAfter = (int[]) readField(mode, "bestSectionTime");
		assertEquals(2400, bstAfter[0], "new-record section updated");
		assertEquals(1500, bstAfter[1], "non-new-record section unchanged");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(ScoreAttackMode mode) {
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

	private static int invokeCheckRanking(ScoreAttackMode mode, int sc, int lv, int time)
			throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, lv, time);
	}

	private static void invokeUpdateRanking(ScoreAttackMode mode, int sc, int lv, int time)
			throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, lv, time);
	}

	private static void invokeUpdateBestSectionTime(ScoreAttackMode mode) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
