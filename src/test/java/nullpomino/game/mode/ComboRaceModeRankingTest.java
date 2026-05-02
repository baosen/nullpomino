package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private ranking helpers in {@link ComboRaceMode}: checkRanking
 * (maxcombo primary, time tiebreaker with -1 sentinel) and updateRanking.
 */
class ComboRaceModeRankingTest {

	// -----------------------------------------------------------------------
	// checkRanking — maxcombo primary, time tiebreaker
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenMaxcomboLosesToAll() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		freshEngine(mode);

		int[][] rCombo = (int[][]) readField(mode, "rankingCombo");
		for(int i = 0; i < 10; i++) rCombo[0][i] = 50;

		// maxcombo=20 can't beat maxcombo=50 slots
		int rank = invokeCheckRanking(mode, 20, 1000);
		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenMaxcomboBeatsAll() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		freshEngine(mode);
		// All zero-initialized -> maxcombo=1 beats all

		int rank = invokeCheckRanking(mode, 1, 99999);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsTiebreakerWhenMaxcomboIsEqual() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		freshEngine(mode);

		int[][] rCombo = (int[][]) readField(mode, "rankingCombo");
		int[][] rTime  = (int[][]) readField(mode, "rankingTime");
		rCombo[0][0] = 30;
		rTime[0][0]  = 8000;

		// same maxcombo, faster time (time >= 0 required) -> rank 0
		int rank = invokeCheckRanking(mode, 30, 7999);
		assertEquals(0, rank);
	}

	@Test
	void checkRankingBeatsSlotWithSentinelMinusOneTime() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		freshEngine(mode);

		int[][] rCombo = (int[][]) readField(mode, "rankingCombo");
		int[][] rTime  = (int[][]) readField(mode, "rankingTime");
		rCombo[0][0] = 30;
		rTime[0][0]  = -1;  // sentinel: no time recorded

		// same maxcombo, any time >= 0 beats -1 sentinel
		int rank = invokeCheckRanking(mode, 30, 5000);
		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRank0AndShiftsExisting() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		freshEngine(mode);

		int[][] rCombo = (int[][]) readField(mode, "rankingCombo");
		rCombo[0][0] = 20;

		invokeUpdateRanking(mode, 25, 8000);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][] comboAfter = (int[][]) readField(mode, "rankingCombo");
		assertEquals(25, comboAfter[0][0]);
		assertEquals(20, comboAfter[0][1]);
	}

	@Test
	void updateRankingSetsMinus1WhenOutOfRank() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		freshEngine(mode);

		int[][] rCombo = (int[][]) readField(mode, "rankingCombo");
		for(int i = 0; i < 10; i++) rCombo[0][i] = 50;

		invokeUpdateRanking(mode, 20, 1000);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(ComboRaceMode mode) throws Exception {
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

	private static int invokeCheckRanking(ComboRaceMode mode, int maxcombo, int time)
			throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, maxcombo, time);
	}

	private static void invokeUpdateRanking(ComboRaceMode mode, int maxcombo, int time)
			throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, maxcombo, time);
	}
}
