package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link AvalancheMode}: checkRanking
 * (type-0: score+time; type-1: score only; type-2: sprint/time with score
 * guard) and updateRanking.
 *
 * Array layout: rankingScore/Time[sctype][colors-3][type][rank].
 * Tests use sctype=0, colors=3 (index 0), and pinned gametype/sprintTarget=0.
 */
class AvalancheModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking type=0 — score primary, faster time secondary
	// -----------------------------------------------------------------------

	@Test
	void checkRankingType0ReturnsMinus1WhenScoreLosesToAll() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		freshEngine(mode);

		int[][][][] rScore = (int[][][][]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[0][0][0][i] = 1_000_000;

		int rank = invokeCheckRanking(mode, 500_000, 0, 0, 0, 3);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingType0ReturnsZeroWhenScoreBeatsAll() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		freshEngine(mode);
		// All zero-initialized -> score=1 beats all

		int rank = invokeCheckRanking(mode, 1, 99999, 0, 0, 3);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingType0UsesTimeAsTiebreakerWhenScoresEqual() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		freshEngine(mode);

		int[][][][] rScore = (int[][][][]) readField(mode, "rankingScore");
		int[][][][] rTime  = (int[][][][]) readField(mode, "rankingTime");
		rScore[0][0][0][0] = 500;
		rTime[0][0][0][0]  = 8000;

		// same score, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 500, 7999, 0, 0, 3);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// checkRanking type=1 — score only (no time tiebreaker)
	// -----------------------------------------------------------------------

	@Test
	void checkRankingType1ReturnsMinus1WhenScoreLosesToAll() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		freshEngine(mode);

		int[][][][] rScore = (int[][][][]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[0][0][1][i] = 1_000_000;

		int rank = invokeCheckRanking(mode, 500_000, 0, 1, 0, 3);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingType1ReturnsZeroWhenScoreBeatsAll() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		freshEngine(mode);

		int rank = invokeCheckRanking(mode, 1, 0, 1, 0, 3);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// checkRanking type=2 — sprint: score guard then time-primary
	// -----------------------------------------------------------------------

	@Test
	void checkRankingType2ReturnsMinus1WhenScoreBelowSprintTarget() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		freshEngine(mode);
		// sprintTarget=0 -> SPRINT_MAX_SCORE[0]=15000; sc < 15000 always -1

		int rank = invokeCheckRanking(mode, 14999, 0, 2, 0, 3);
		assertEquals(-1, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExistingForType0() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		freshEngine(mode);

		int[][][][] rScore = (int[][][][]) readField(mode, "rankingScore");
		rScore[0][0][0][0] = 500;

		invokeUpdateRanking(mode, 600, 99999, 0, 0, 3);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][][][] scoreAfter = (int[][][][]) readField(mode, "rankingScore");
		assertEquals(600, scoreAfter[0][0][0][0]);
		assertEquals(500, scoreAfter[0][0][0][1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRankForType0() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		freshEngine(mode);

		int[][][][] rScore = (int[][][][]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[0][0][0][i] = 1_000_000;

		invokeUpdateRanking(mode, 100, 99999, 0, 0, 3);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(AvalancheMode mode) {
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

	private static int invokeCheckRanking(AvalancheMode mode, int sc, int time, int type, int sctype, int colors)
			throws Exception {
		Method m = AvalancheMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, time, type, sctype, colors);
	}

	private static void invokeUpdateRanking(AvalancheMode mode, int sc, int time, int type, int sctype, int colors)
			throws Exception {
		Method m = AvalancheMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, time, type, sctype, colors);
	}
}
