package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GradeManiaMode}'s private {@code stNewRecordCheck}, the
 * per-section best-time bookkeeping. Marks
 * {@code sectionIsNewRecord[sectionNumber] = true} and sets the
 * mode-wide {@code sectionAnyNewRecord = true} when both conditions
 * hold: the just-completed sectiontime beats the stored bestSectionTime
 * AND the game is not in replay mode (so re-watching a recorded game
 * doesn't keep re-flagging the same sections as new records).
 */
class GradeManiaModeStNewRecordCheckTest {

	@Test
	void sectionTimeBelowBestSetsNewRecordFlags() throws Exception {
		// sectiontime[3]=5400 < bestSectionTime[3]=6000 -> new record.
		GradeManiaMode mode = new GradeManiaMode();
		GameManager manager = freshGameManager(mode);
		manager.replayMode = false;
		int[] sectiontime = (int[]) read(mode, "sectiontime");
		int[] bestSectionTime = (int[]) read(mode, "bestSectionTime");
		sectiontime[3] = 5400;
		bestSectionTime[3] = 6000;

		invokeStNewRecordCheck(mode, 3);

		boolean[] sectionIsNewRecord = (boolean[]) read(mode, "sectionIsNewRecord");
		assertTrue(sectionIsNewRecord[3],
				"section 3 -> new record (5400 < 6000)");
		assertTrue(getBoolean(mode, "sectionAnyNewRecord"),
				"sectionAnyNewRecord lifted to true on first new record");
	}

	@Test
	void sectionTimeEqualOrAboveBestLeavesFlagsUnchanged() throws Exception {
		// sectiontime[3]=6000 == bestSectionTime[3]=6000 -> NOT a new
		// record (the strict-less-than check requires actually beating it).
		GradeManiaMode mode = new GradeManiaMode();
		GameManager manager = freshGameManager(mode);
		manager.replayMode = false;
		int[] sectiontime = (int[]) read(mode, "sectiontime");
		int[] bestSectionTime = (int[]) read(mode, "bestSectionTime");
		sectiontime[3] = 6000;
		bestSectionTime[3] = 6000;

		invokeStNewRecordCheck(mode, 3);

		boolean[] sectionIsNewRecord = (boolean[]) read(mode, "sectionIsNewRecord");
		assertFalse(sectionIsNewRecord[3],
				"section 3 -> not a new record (tie doesn't count)");
		assertFalse(getBoolean(mode, "sectionAnyNewRecord"),
				"sectionAnyNewRecord untouched on a non-new-record");
	}

	@Test
	void slowerSectionTimeIsNotANewRecord() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameManager manager = freshGameManager(mode);
		manager.replayMode = false;
		int[] sectiontime = (int[]) read(mode, "sectiontime");
		int[] bestSectionTime = (int[]) read(mode, "bestSectionTime");
		sectiontime[2] = 7200;
		bestSectionTime[2] = 6000;

		invokeStNewRecordCheck(mode, 2);

		boolean[] sectionIsNewRecord = (boolean[]) read(mode, "sectionIsNewRecord");
		assertFalse(sectionIsNewRecord[2]);
		assertFalse(getBoolean(mode, "sectionAnyNewRecord"));
	}

	@Test
	void replayModeShortCircuitsSoNoNewRecordIsRecorded() throws Exception {
		// Even when the time is faster, replay mode prevents the flag
		// from flipping — re-watching a recorded game shouldn't keep
		// re-flagging the same sections.
		GradeManiaMode mode = new GradeManiaMode();
		GameManager manager = freshGameManager(mode);
		manager.replayMode = true;
		int[] sectiontime = (int[]) read(mode, "sectiontime");
		int[] bestSectionTime = (int[]) read(mode, "bestSectionTime");
		sectiontime[1] = 1000;
		bestSectionTime[1] = 9000;

		invokeStNewRecordCheck(mode, 1);

		boolean[] sectionIsNewRecord = (boolean[]) read(mode, "sectionIsNewRecord");
		assertFalse(sectionIsNewRecord[1],
				"replay mode -> no new record flag set "
						+ "even though time is much faster");
		assertFalse(getBoolean(mode, "sectionAnyNewRecord"));
	}

	@Test
	void sectionAnyNewRecordIsLatchingNotResettable() throws Exception {
		// First section beats best -> sectionAnyNewRecord=true. A second
		// non-record call doesn't reset the latch.
		GradeManiaMode mode = new GradeManiaMode();
		GameManager manager = freshGameManager(mode);
		manager.replayMode = false;
		int[] sectiontime = (int[]) read(mode, "sectiontime");
		int[] bestSectionTime = (int[]) read(mode, "bestSectionTime");
		sectiontime[0] = 5000;
		bestSectionTime[0] = 6000;
		sectiontime[1] = 7000;
		bestSectionTime[1] = 6500;

		invokeStNewRecordCheck(mode, 0);
		invokeStNewRecordCheck(mode, 1);

		boolean[] sectionIsNewRecord = (boolean[]) read(mode, "sectionIsNewRecord");
		assertTrue(sectionIsNewRecord[0]);
		assertFalse(sectionIsNewRecord[1],
				"second section is a regression -> not flagged");
		assertTrue(getBoolean(mode, "sectionAnyNewRecord"),
				"sectionAnyNewRecord stays true once latched by section 0");
	}

	private static GameManager freshGameManager(GradeManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm;
	}

	private static void invokeStNewRecordCheck(GradeManiaMode mode, int sectionNumber)
			throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("stNewRecordCheck", int.class);
		m.setAccessible(true);
		m.invoke(mode, sectionNumber);
	}

	private static Object read(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static boolean getBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
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
}
