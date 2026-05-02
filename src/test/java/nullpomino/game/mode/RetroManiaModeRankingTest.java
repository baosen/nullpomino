package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link RetroManiaMode}: checkRanking
 * (score-primary, lines-secondary, faster-time-tertiary) and updateRanking.
 */
class RetroManiaModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — score primary, lines secondary, faster time tertiary
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenScoreLosesToAll() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		freshEngine(mode);

		int[][] rScore = (int[][]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[0][i] = 999_999;

		int rank = invokeCheckRanking(mode, 500_000, 0, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenScoreBeatsAll() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		freshEngine(mode);
		// All zero-initialized -> score=1 beats all

		int rank = invokeCheckRanking(mode, 1, 0, 0, 0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesLinesAsSecondaryKey() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		freshEngine(mode);

		int[][] rScore = (int[][]) readField(mode, "rankingScore");
		int[][] rLines = (int[][]) readField(mode, "rankingLines");
		rScore[0][0] = 500;
		rLines[0][0] = 10;

		// same score, more lines -> rank 0
		int rank = invokeCheckRanking(mode, 500, 11, 0, 0);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesFasterTimeAsTertiaryKey() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		freshEngine(mode);

		int[][] rScore = (int[][]) readField(mode, "rankingScore");
		int[][] rLines = (int[][]) readField(mode, "rankingLines");
		int[][] rTime  = (int[][]) readField(mode, "rankingTime");
		rScore[0][0] = 500;
		rLines[0][0] = 10;
		rTime[0][0]  = 8000;

		// same score and lines, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 500, 10, 7999, 0);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		freshEngine(mode);

		int[][] rScore = (int[][]) readField(mode, "rankingScore");
		rScore[0][0] = 500;

		invokeUpdateRanking(mode, 600, 0, 0, 0);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][] scoreAfter = (int[][]) readField(mode, "rankingScore");
		assertEquals(600, scoreAfter[0][0]);
		assertEquals(500, scoreAfter[0][1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		freshEngine(mode);

		int[][] rScore = (int[][]) readField(mode, "rankingScore");
		for(int i = 0; i < 10; i++) rScore[0][i] = 999_999;

		invokeUpdateRanking(mode, 100, 0, 0, 0);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(RetroManiaMode mode) {
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

	private static int invokeCheckRanking(RetroManiaMode mode, int sc, int li, int time, int type)
			throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, li, time, type);
	}

	private static void invokeUpdateRanking(RetroManiaMode mode, int sc, int li, int time, int type)
			throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, li, time, type);
	}
}
