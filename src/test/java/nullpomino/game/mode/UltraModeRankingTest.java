package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link UltraMode}: checkRanking
 * (type-0: score-primary/lines-secondary; type-1: lines-primary/score-secondary)
 * and updateRanking (iterates both ranking types).
 */
class UltraModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking type 0 — score primary, lines tiebreaker
	// -----------------------------------------------------------------------

	@Test
	void checkRankingType0ReturnsMinus1WhenScoreLosesToAll() throws Exception {
		UltraMode mode = new UltraMode();
		freshEngine(mode);

		int[][][] rScore = (int[][][]) readField(mode, "rankingScore");
		for(int i = 0; i < 5; i++) rScore[0][0][i] = 1_000_000;

		int rank = invokeCheckRanking(mode, 500_000, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingType0ReturnsZeroWhenScoreBeatsAll() throws Exception {
		UltraMode mode = new UltraMode();
		freshEngine(mode);
		// All zero-initialized -> score=1 beats all

		int rank = invokeCheckRanking(mode, 1, 0, 0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingType0UsesLinesAsTiebreakerWhenScoresEqual() throws Exception {
		UltraMode mode = new UltraMode();
		freshEngine(mode);

		int[][][] rScore = (int[][][]) readField(mode, "rankingScore");
		int[][][] rLines = (int[][][]) readField(mode, "rankingLines");
		rScore[0][0][0] = 500;
		rLines[0][0][0] = 10;

		// same score, more lines -> rank 0
		int rank = invokeCheckRanking(mode, 500, 11, 0);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// checkRanking type 1 — lines primary, score tiebreaker
	// -----------------------------------------------------------------------

	@Test
	void checkRankingType1ReturnsMinus1WhenLinesLoseToAll() throws Exception {
		UltraMode mode = new UltraMode();
		freshEngine(mode);

		int[][][] rLines = (int[][][]) readField(mode, "rankingLines");
		for(int i = 0; i < 5; i++) rLines[0][1][i] = 100;

		int rank = invokeCheckRanking(mode, 0, 50, 1);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingType1ReturnsZeroWhenLinesBeatsAll() throws Exception {
		UltraMode mode = new UltraMode();
		freshEngine(mode);
		// All zero-initialized -> lines=1 beats all

		int rank = invokeCheckRanking(mode, 0, 1, 1);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingType1UsesScoreAsTiebreakerWhenLinesEqual() throws Exception {
		UltraMode mode = new UltraMode();
		freshEngine(mode);

		int[][][] rLines = (int[][][]) readField(mode, "rankingLines");
		int[][][] rScore = (int[][][]) readField(mode, "rankingScore");
		rLines[0][1][0] = 20;
		rScore[0][1][0] = 500;

		// same lines, higher score -> rank 0
		int rank = invokeCheckRanking(mode, 600, 20, 1);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking — iterates both types
	// -----------------------------------------------------------------------

	@Test
	void updateRankingUpdatesRankingRankForBothTypes() throws Exception {
		UltraMode mode = new UltraMode();
		freshEngine(mode);

		// Starting from zero-initialized arrays, any sc/li > 0 ranks at 0 for both types
		invokeUpdateRanking(mode, 100, 5);

		int[] rankingRank = (int[]) readField(mode, "rankingRank");
		assertEquals(0, rankingRank[0]);
		assertEquals(0, rankingRank[1]);
	}

	@Test
	void updateRankingInsertsAtRank0AndShiftsExistingForType0() throws Exception {
		UltraMode mode = new UltraMode();
		freshEngine(mode);

		int[][][] rScore = (int[][][]) readField(mode, "rankingScore");
		rScore[0][0][0] = 500;

		invokeUpdateRanking(mode, 600, 0);

		int[] rankingRank = (int[]) readField(mode, "rankingRank");
		assertEquals(0, rankingRank[0]);
		int[][][] scoreAfter = (int[][][]) readField(mode, "rankingScore");
		assertEquals(600, scoreAfter[0][0][0]);
		assertEquals(500, scoreAfter[0][0][1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRankForType0() throws Exception {
		UltraMode mode = new UltraMode();
		freshEngine(mode);

		int[][][] rScore = (int[][][]) readField(mode, "rankingScore");
		for(int i = 0; i < 5; i++) rScore[0][0][i] = 1_000_000;

		invokeUpdateRanking(mode, 100, 0);

		int[] rankingRank = (int[]) readField(mode, "rankingRank");
		assertEquals(-1, rankingRank[0]);
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(UltraMode mode) throws Exception {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		// loadSetting defaults goaltype=2; pin to 0 so tests use a known array index
		Field f = findField(mode.getClass(), "goaltype");
		f.setAccessible(true);
		f.setInt(mode, 0);
		return gm.engine[0];
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

	private static int invokeCheckRanking(UltraMode mode, int sc, int li, int rankingtype)
			throws Exception {
		Method m = UltraMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, li, rankingtype);
	}

	private static void invokeUpdateRanking(UltraMode mode, int sc, int li)
			throws Exception {
		Method m = UltraMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, li);
	}
}
