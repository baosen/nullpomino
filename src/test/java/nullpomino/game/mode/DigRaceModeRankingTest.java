package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link DigRaceMode}: checkRanking
 * (time-primary with negative sentinel, lines-secondary, piece-tertiary)
 * and updateRanking.
 */
class DigRaceModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — faster time is better; negative sentinel = slot unused
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenTimeIsSlowerAndAllSlotsAreFaster() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		for(int i = 0; i < 10; i++) rTime[0][i] = 3000;

		// time=4000 is slower -> can't beat any slot -> -1
		int rank = invokeCheckRanking(mode, 4000, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenSlotHasNegativeSentinelTime() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		rTime[0][0] = -1;  // sentinel: slot not yet used

		int rank = invokeCheckRanking(mode, 5000, 0, 0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenTimeBeatsBestSlot() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		rTime[0][0] = 8000;

		int rank = invokeCheckRanking(mode, 7999, 0, 0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesLinesAsTiebreakerWhenTimesEqual() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		freshEngine(mode);

		int[][] rTime  = (int[][]) readField(mode, "rankingTime");
		int[][] rLines = (int[][]) readField(mode, "rankingLines");
		rTime[0][0]  = 6000;
		rLines[0][0] = 15;

		// same time, fewer lines -> rank 0
		int rank = invokeCheckRanking(mode, 6000, 14, 0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesPieceAsTertiaryKeyWhenTimeAndLinesAreEqual() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		freshEngine(mode);

		int[][] rTime  = (int[][]) readField(mode, "rankingTime");
		int[][] rLines = (int[][]) readField(mode, "rankingLines");
		int[][] rPiece = (int[][]) readField(mode, "rankingPiece");
		rTime[0][0]  = 6000;
		rLines[0][0] = 10;
		rPiece[0][0] = 50;

		// same time and lines, fewer pieces -> rank 0
		int rank = invokeCheckRanking(mode, 6000, 10, 49);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		rTime[0][0] = 8000;

		invokeUpdateRanking(mode, 7000, 0, 0);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][] timeAfter = (int[][]) readField(mode, "rankingTime");
		assertEquals(7000, timeAfter[0][0]);
		assertEquals(8000, timeAfter[0][1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		for(int i = 0; i < 10; i++) rTime[0][i] = 3000;

		invokeUpdateRanking(mode, 4000, 0, 0);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(DigRaceMode mode) throws Exception {
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

	private static int invokeCheckRanking(DigRaceMode mode, int time, int lines, int piece)
			throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, time, lines, piece);
	}

	private static void invokeUpdateRanking(DigRaceMode mode, int time, int lines, int piece)
			throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, time, lines, piece);
	}
}
