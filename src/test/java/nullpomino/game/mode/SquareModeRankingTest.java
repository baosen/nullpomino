package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link SquareMode}: checkRanking
 * (gametype-0: score+squares+time; gametype-1: ultra score+squares with
 * time guard; gametype-2: sprint time+squares with score guard/sentinel)
 * and updateRanking.
 */
class SquareModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking gametype=0 — score primary, squares secondary, faster time
	// -----------------------------------------------------------------------

	@Test
	void checkRankingType0ReturnsMinus1WhenScoreLosesToAll() throws Exception {
		SquareMode mode = new SquareMode();
		freshEngine(mode, 0);

		int[][] rScore = (int[][]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[0][i] = 1_000_000;

		int rank = invokeCheckRanking(mode, 500_000, 0, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingType0ReturnsZeroWhenScoreBeatsAll() throws Exception {
		SquareMode mode = new SquareMode();
		freshEngine(mode, 0);

		int rank = invokeCheckRanking(mode, 1, 0, 0, 0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingType0UsesSquaresAsSecondaryKey() throws Exception {
		SquareMode mode = new SquareMode();
		freshEngine(mode, 0);

		int[][] rScore   = (int[][]) readField(mode, "rankingScore");
		int[][] rSquares = (int[][]) readField(mode, "rankingSquares");
		rScore[0][0]   = 500;
		rSquares[0][0] = 5;

		// same score, more squares -> rank 0
		int rank = invokeCheckRanking(mode, 500, 0, 6, 0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingType0UsesFasterTimeAsTertiaryKey() throws Exception {
		SquareMode mode = new SquareMode();
		freshEngine(mode, 0);

		int[][] rScore   = (int[][]) readField(mode, "rankingScore");
		int[][] rTime    = (int[][]) readField(mode, "rankingTime");
		int[][] rSquares = (int[][]) readField(mode, "rankingSquares");
		rScore[0][0]   = 500;
		rSquares[0][0] = 5;
		rTime[0][0]    = 9000;

		// same score and squares, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 500, 8999, 5, 0);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// checkRanking gametype=1 — Ultra: requires time >= ULTRA_MAX_TIME (10800)
	// -----------------------------------------------------------------------

	@Test
	void checkRankingType1ReturnsMinus1WhenTimeUnderUltraLimit() throws Exception {
		SquareMode mode = new SquareMode();
		freshEngine(mode, 1);
		// time < 10800 -> never ranks in Ultra mode

		int rank = invokeCheckRanking(mode, 999_999, 10799, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingType1ReturnsZeroWhenTimeAtUltraLimitAndScoreBeatsAll() throws Exception {
		SquareMode mode = new SquareMode();
		freshEngine(mode, 1);
		// time >= 10800 and score=1 beats all zeros

		int rank = invokeCheckRanking(mode, 1, 10800, 0, 0);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// checkRanking gametype=2 — Sprint: faster time with score guard (>=150)
	// -----------------------------------------------------------------------

	@Test
	void checkRankingType2ReturnsMinus1WhenScoreBelowSprintTarget() throws Exception {
		SquareMode mode = new SquareMode();
		freshEngine(mode, 2);
		// sc < SPRINT_MAX_SCORE=150 never qualifies

		int rank = invokeCheckRanking(mode, 149, 5000, 0, 0);
		assertEquals(-1, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExistingForType0() throws Exception {
		SquareMode mode = new SquareMode();
		freshEngine(mode, 0);

		int[][] rScore = (int[][]) readField(mode, "rankingScore");
		rScore[0][0] = 500;

		invokeUpdateRanking(mode, 600, 0, 0, 0);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][] scoreAfter = (int[][]) readField(mode, "rankingScore");
		assertEquals(600, scoreAfter[0][0]);
		assertEquals(500, scoreAfter[0][1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRankForType0() throws Exception {
		SquareMode mode = new SquareMode();
		freshEngine(mode, 0);

		int[][] rScore = (int[][]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[0][i] = 1_000_000;

		invokeUpdateRanking(mode, 100, 0, 0, 0);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(SquareMode mode, int gametype) throws Exception {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		Field f = findField(mode.getClass(), "gametype");
		f.setAccessible(true);
		f.setInt(mode, gametype);
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

	private static int invokeCheckRanking(SquareMode mode, int sc, int time, int sq, int type)
			throws Exception {
		Method m = SquareMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, time, sq, type);
	}

	private static void invokeUpdateRanking(SquareMode mode, int sc, int time, int sq, int type)
			throws Exception {
		Method m = SquareMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, time, sq, type);
	}
}
