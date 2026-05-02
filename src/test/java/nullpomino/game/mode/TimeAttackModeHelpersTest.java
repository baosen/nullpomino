package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the private helpers in {@link TimeAttackMode} not exercised by
 * replay-based tests: setAverageSectionTime, checkRanking, and updateRanking.
 */
class TimeAttackModeHelpersTest {

	// -----------------------------------------------------------------------
	// setAverageSectionTime
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeComputesAverageAcrossCompletedSections() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		freshEngine(mode);

		setInt(mode, "startlevel", 0);
		setInt(mode, "sectionscomp", 4);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 1000;
		st[1] = 2000;
		st[2] = 3000;
		st[3] = 4000;

		invokeSetAverageSectionTime(mode);

		// sum = 10000, sectionscomp = 4 -> avg = 2500
		assertEquals(2500, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeStartsFromStartlevel() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		freshEngine(mode);

		// startlevel=2 means sections 0 and 1 are skipped
		setInt(mode, "startlevel", 2);
		setInt(mode, "sectionscomp", 4);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 9999;  // skipped
		st[1] = 9999;  // skipped
		st[2] = 600;
		st[3] = 900;

		invokeSetAverageSectionTime(mode);

		// sum of st[2]+st[3] = 1500, divided by sectionscomp=4 -> 375
		assertEquals(375, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeIsZeroWhenNoSectionsCompleted() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		freshEngine(mode);

		setInt(mode, "sectionscomp", 0);
		setInt(mode, "sectionavgtime", 6666);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// checkRanking
	// -----------------------------------------------------------------------

	@Test
	void checkRankingReturnsMinus1WhenEntryDoesNotMakeRanking() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		freshEngine(mode);

		// Fill type-0 ranking with 10 entries that all beat the candidate.
		int[][] lines = (int[][]) readField(mode, "rankingLines");
		int[][] time = (int[][]) readField(mode, "rankingTime");
		int[][] rollclear = (int[][]) readField(mode, "rankingRollclear");
		for(int i = 0; i < 10; i++) {
			lines[0][i] = 300;
			time[0][i] = 1000;
			rollclear[0][i] = 1;  // all completed
		}

		// Candidate: 0 rollclear, 250 lines, time 500 -> loses on rollclear
		int rank = invokeCheckRanking(mode, 250, 500, 0, 0);

		assertEquals(-1, rank);
	}

	@Test
	void checkRankingReturnsZeroWhenEntryBeatsAllOnRollclear() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		freshEngine(mode);

		// All ranking entries have rollclear=0; candidate has rollclear=1 -> beats all
		// rankingRollclear is zero-initialized, so no setup needed.

		int rank = invokeCheckRanking(mode, 10, 9000, 0, 1);

		assertEquals(0, rank);
	}

	@Test
	void checkRankingUsesTimeAsTiebreakerWhenLinesEqual() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		freshEngine(mode);

		int[][] lines = (int[][]) readField(mode, "rankingLines");
		int[][] time = (int[][]) readField(mode, "rankingTime");
		int[][] rollclear = (int[][]) readField(mode, "rankingRollclear");
		// rank 0: same rollclear, same lines, but slower time (bigger)
		lines[0][0] = 200;
		time[0][0] = 5000;
		rollclear[0][0] = 1;

		// candidate: same rollclear=1, same lines=200, faster time=4000 -> beats rank 0
		int rank = invokeCheckRanking(mode, 200, 4000, 0, 1);

		assertEquals(0, rank);
	}

	// -----------------------------------------------------------------------
	// updateRanking
	// -----------------------------------------------------------------------

	@Test
	void updateRankingInsertsAtRankZeroAndShiftsDown() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		freshEngine(mode);

		int[][] lines = (int[][]) readField(mode, "rankingLines");
		int[][] time = (int[][]) readField(mode, "rankingTime");
		int[][] rollclear = (int[][]) readField(mode, "rankingRollclear");
		// Seed rank 0 with a completed run
		lines[0][0] = 100;
		time[0][0] = 8000;
		rollclear[0][0] = 1;

		// A better run: same rollclear=1, more lines=150, faster time=5000 -> rank 0
		invokeUpdateRanking(mode, 150, 5000, 0, 1);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[][] linesAfter = (int[][]) readField(mode, "rankingLines");
		assertEquals(150, linesAfter[0][0], "new entry at rank 0");
		assertEquals(100, linesAfter[0][1], "old rank 0 shifted to rank 1");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(TimeAttackMode mode) {
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

	private static void setInt(Object instance, String name, int value) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
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

	private static void invokeSetAverageSectionTime(TimeAttackMode mode) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static int invokeCheckRanking(TimeAttackMode mode, int li, int time, int type, int clear)
			throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, li, time, type, clear);
	}

	private static void invokeUpdateRanking(TimeAttackMode mode, int li, int time, int type, int clear)
			throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, li, time, type, clear);
	}
}
