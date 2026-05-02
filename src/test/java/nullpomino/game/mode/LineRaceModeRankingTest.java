package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link LineRaceMode}: checkRanking
 * (time-primary with negative sentinel, piece-secondary, pps-tertiary)
 * and updateRanking.
 */
class LineRaceModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — faster time is better; negative sentinel = slot unused
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenTimeIsSlowerAndAllSlotsAreFaster() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		for(int i = 0; i < 10; i++) rTime[0][i] = 3000;

		int rank = invokeCheckRanking(mode, 4000, 0, 0f);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenSlotHasNegativeSentinelTime() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		rTime[0][0] = -1;

		int rank = invokeCheckRanking(mode, 5000, 0, 0f);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenTimeBeatsBestSlot() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		rTime[0][0] = 8000;

		int rank = invokeCheckRanking(mode, 7999, 0, 0f);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesPieceAsTiebreakerWhenTimesEqual() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		freshEngine(mode);

		int[][] rTime  = (int[][]) readField(mode, "rankingTime");
		int[][] rPiece = (int[][]) readField(mode, "rankingPiece");
		rTime[0][0]  = 6000;
		rPiece[0][0] = 50;

		// same time, fewer pieces -> rank 0
		int rank = invokeCheckRanking(mode, 6000, 49, 0f);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesPPSAsTertiaryKeyWhenTimeAndPieceAreEqual() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		freshEngine(mode);

		int[][] rTime   = (int[][]) readField(mode, "rankingTime");
		int[][] rPiece  = (int[][]) readField(mode, "rankingPiece");
		float[][] rPPS  = (float[][]) readField(mode, "rankingPPS");
		rTime[0][0]  = 6000;
		rPiece[0][0] = 40;
		rPPS[0][0]   = 1.5f;

		// same time and piece, higher pps -> rank 0
		int rank = invokeCheckRanking(mode, 6000, 40, 1.6f);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		rTime[0][0] = 8000;

		invokeUpdateRanking(mode, 7000, 0, 0f);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][] timeAfter = (int[][]) readField(mode, "rankingTime");
		assertEquals(7000, timeAfter[0][0]);
		assertEquals(8000, timeAfter[0][1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		freshEngine(mode);

		int[][] rTime = (int[][]) readField(mode, "rankingTime");
		for(int i = 0; i < 10; i++) rTime[0][i] = 3000;

		invokeUpdateRanking(mode, 4000, 0, 0f);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(LineRaceMode mode) throws Exception {
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

	private static int invokeCheckRanking(LineRaceMode mode, int time, int piece, float pps)
			throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, float.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, time, piece, pps);
	}

	private static void invokeUpdateRanking(LineRaceMode mode, int time, int piece, float pps)
			throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, float.class);
		m.setAccessible(true);
		m.invoke(mode, time, piece, pps);
	}
}
