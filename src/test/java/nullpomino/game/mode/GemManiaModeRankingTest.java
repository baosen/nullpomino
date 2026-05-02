package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link GemManiaMode}: checkRanking
 * (allclear-primary, stage-secondary, clearper-tertiary, faster-time
 * quaternary) and updateRanking.
 */
class GemManiaModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — allclear primary, stage, clearper, time
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenAllClearAndStageLoseToAll() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		freshEngine(mode);

		int[][] rAllClear = (int[][]) readField(mode, "rankingAllClear");
		int[][] rStage    = (int[][]) readField(mode, "rankingStage");
		for(int i = 0; i < 10; i++) {
			rAllClear[0][i] = 2;
			rStage[0][i]    = 50;
		}

		// allclear=0 loses to allclear=2 -> -1
		int rank = invokeCheckRanking(mode, 0, 0, 0, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenAllClearBeatsAll() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		freshEngine(mode);
		// All zero-initialized -> allclear=1 beats all

		int rank = invokeCheckRanking(mode, 0, 0, 0, 0, 1);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesStageAsSecondaryKey() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		freshEngine(mode);

		int[][] rAllClear = (int[][]) readField(mode, "rankingAllClear");
		int[][] rStage    = (int[][]) readField(mode, "rankingStage");
		rAllClear[0][0] = 1;
		rStage[0][0]    = 10;

		// same allclear, higher stage -> rank 0
		int rank = invokeCheckRanking(mode, 0, 11, 0, 0, 1);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesClearPerAsTertiaryKey() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		freshEngine(mode);

		int[][] rAllClear = (int[][]) readField(mode, "rankingAllClear");
		int[][] rStage    = (int[][]) readField(mode, "rankingStage");
		int[][] rClearPer = (int[][]) readField(mode, "rankingClearPer");
		rAllClear[0][0] = 1;
		rStage[0][0]    = 10;
		rClearPer[0][0] = 80;

		// same allclear and stage, higher clearper -> rank 0
		int rank = invokeCheckRanking(mode, 0, 10, 81, 0, 1);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesFasterTimeAsQuaternaryKey() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		freshEngine(mode);

		int[][] rAllClear = (int[][]) readField(mode, "rankingAllClear");
		int[][] rStage    = (int[][]) readField(mode, "rankingStage");
		int[][] rClearPer = (int[][]) readField(mode, "rankingClearPer");
		int[][] rTime     = (int[][]) readField(mode, "rankingTime");
		rAllClear[0][0] = 1;
		rStage[0][0]    = 10;
		rClearPer[0][0] = 80;
		rTime[0][0]     = 9000;

		// same allclear, stage and clearper, faster time -> rank 0
		int rank = invokeCheckRanking(mode, 0, 10, 80, 8999, 1);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		freshEngine(mode);

		int[][] rAllClear = (int[][]) readField(mode, "rankingAllClear");
		rAllClear[0][0] = 1;

		invokeUpdateRanking(mode, 0, 0, 0, 0, 2);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][] allClearAfter = (int[][]) readField(mode, "rankingAllClear");
		assertEquals(2, allClearAfter[0][0]);
		assertEquals(1, allClearAfter[0][1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		freshEngine(mode);

		int[][] rAllClear = (int[][]) readField(mode, "rankingAllClear");
		int[][] rStage    = (int[][]) readField(mode, "rankingStage");
		for(int i = 0; i < 10; i++) {
			rAllClear[0][i] = 2;
			rStage[0][i]    = 99;
		}

		invokeUpdateRanking(mode, 0, 0, 0, 0, 0);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GemManiaMode mode) {
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

	private static int invokeCheckRanking(GemManiaMode mode, int type, int stg, int clper, int time, int clear)
			throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, type, stg, clper, time, clear);
	}

	private static void invokeUpdateRanking(GemManiaMode mode, int type, int stg, int clper, int time, int clear)
			throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, type, stg, clper, time, clear);
	}
}
