package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link PhysicianMode}: checkRanking
 * (score-primary, faster-time-secondary) and updateRanking.
 */
class PhysicianModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — score primary, faster time tiebreaker
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenScoreLosesToAll() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		freshEngine(mode);

		int[] rScore = (int[]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[i] = 1_000_000;

		int rank = invokeCheckRanking(mode, 500_000, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenScoreBeatsAll() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		freshEngine(mode);
		// All zero-initialized -> score=1 beats all

		int rank = invokeCheckRanking(mode, 1, 99999);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsTiebreakerWhenScoresEqual() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		freshEngine(mode);

		int[] rScore = (int[]) readField(mode, "rankingScore");
		int[] rTime  = (int[]) readField(mode, "rankingTime");
		rScore[0] = 500;
		rTime[0]  = 8000;

		// same score, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 500, 7999);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		freshEngine(mode);

		int[] rScore = (int[]) readField(mode, "rankingScore");
		rScore[0] = 500;

		invokeUpdateRanking(mode, 600, 99999);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] scoreAfter = (int[]) readField(mode, "rankingScore");
		assertEquals(600, scoreAfter[0]);
		assertEquals(500, scoreAfter[1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		freshEngine(mode);

		int[] rScore = (int[]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[i] = 1_000_000;

		invokeUpdateRanking(mode, 100, 99999);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(PhysicianMode mode) {
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

	private static int invokeCheckRanking(PhysicianMode mode, int sc, int time)
			throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, time);
	}

	private static void invokeUpdateRanking(PhysicianMode mode, int sc, int time)
			throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, time);
	}
}
