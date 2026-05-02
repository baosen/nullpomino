package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link ScoreRaceMode}: checkRanking
 * (time-primary with negative sentinel / lines / spl tiebreakers) and
 * updateRanking.
 */
class ScoreRaceModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — faster time is better; negative sentinel means "not set"
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenTimeIsSlowerAndAllSlotsAreFaster() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		for(int i = 0; i < 10; i++) rTime[0][i] = 3000;

		// time=4000 is slower -> can't beat any slot -> -1
		int rank = invokeCheckRanking(mode, 4000, 0, 0.0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenSlotHasNegativeSentinelTime() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		rTime[0][0] = -1;  // sentinel: slot not yet used

		// Any positive time beats the -1 sentinel
		int rank = invokeCheckRanking(mode, 5000, 0, 0.0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenTimeBeatsBestSlot() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		rTime[0][0] = 8000;

		int rank = invokeCheckRanking(mode, 7999, 0, 0.0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesLinesAsTiebreakerWhenTimesEqual() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		freshEngine(mode);

		int[][] rTime  = (int[][]) readField(mode, "rankingTime");
		int[][] rLines = (int[][]) readField(mode, "rankingLines");
		rTime[0][0]  = 6000;
		rLines[0][0] = 15;  // non-zero: fewer lines (lower) is better

		// same time, fewer lines (candidate=14 < 15) -> rank 0
		int rank = invokeCheckRanking(mode, 6000, 14, 0.0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesSplAsTiebreakerWhenTimeAndLinesAreEqual() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		freshEngine(mode);

		int[][] rTime  = (int[][]) readField(mode, "rankingTime");
		int[][] rLines = (int[][]) readField(mode, "rankingLines");
		double[][] rSPL = (double[][]) readField(mode, "rankingSPL");
		rTime[0][0]  = 6000;
		rLines[0][0] = 10;
		rSPL[0][0]   = 1.0;

		// same time and lines, higher spl -> rank 0
		int rank = invokeCheckRanking(mode, 6000, 10, 1.1);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		rTime[0][0] = 8000;

		invokeUpdateRanking(mode, 7000, 0, 0.0);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][] timeAfter = (int[][]) readField(mode, "rankingTime");
		assertEquals(7000, timeAfter[0][0]);
		assertEquals(8000, timeAfter[0][1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		for(int i = 0; i < 10; i++) rTime[0][i] = 3000;

		invokeUpdateRanking(mode, 4000, 0, 0.0);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(ScoreRaceMode mode) throws Exception {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		// loadSetting defaults goaltype=1; pin to 0 so tests use a known array index
		Field f = findField(mode.getClass(), "goaltype");
		f.setAccessible(true);
		f.setInt(mode, 0);
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

	private static int invokeCheckRanking(ScoreRaceMode mode, int time, int lines, double spl)
			throws Exception {
		Method m = ScoreRaceMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, double.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, time, lines, spl);
	}

	private static void invokeUpdateRanking(ScoreRaceMode mode, int time, int lines, double spl)
			throws Exception {
		Method m = ScoreRaceMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, double.class);
		m.setAccessible(true);
		m.invoke(mode, time, lines, spl);
	}
}
