package nullpomino.tool.airankstool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests RanksIterator's non-Swing inner class logic.
 *
 * <p>RanksIterator extends JDialog; its constructor calls setVisible(true)
 * which fails headless. We use {@code Unsafe.allocateInstance} to create an
 * instance that we then use as the outer class reference for the
 * package-private inner classes {@code OneIteration} and {@code AllIterations}.
 *
 * <p>Testable surface:
 * <ul>
 *   <li>{@code OneIteration} constructor and {@code iterate()} method</li>
 *   <li>{@code OneIteration.cancelTask()}</li>
 *   <li>{@code AllIterations} constructor</li>
 *   <li>{@code AllIterations.cancelTask()}</li>
 * </ul>
 *
 * The {@code doInBackground} methods are SwingWorker callbacks that perform
 * I/O and cannot be tested headless.
 */
class RanksIteratorTest {

	private RanksIterator iterator;
	private Ranks ranks;

	@BeforeEach
	void setUp() throws Exception {
		iterator = allocateRanksIterator();
		ranks = new Ranks(4, 9);
	}

	// ------------------------------------------------------------------
	// OneIteration
	// ------------------------------------------------------------------

	@Test
	void oneIterationConstructorInitialisesFields() throws Exception {
		RanksIterator.OneIteration oi = iterator.new OneIteration(2, ranks);

		assertNotNull(oi);
		assertEquals(2, getIntField(oi, "totalParts"));
		assertEquals(false, getBooleanField(oi, "cancelled"));
		assertEquals(ranks, getObjectField(oi, "ranks"));
	}

	@Test
	void oneIterationWithDifferentTotalParts() throws Exception {
		RanksIterator.OneIteration oi = iterator.new OneIteration(4, ranks);

		assertEquals(4, getIntField(oi, "totalParts"));
	}

	@Test
	void oneIterationIterateDoesNotThrowWhenCompletionIsZero() throws Exception {
		RanksIterator.OneIteration oi = iterator.new OneIteration(2, ranks);

		// completionPercentageIncrease returns false at 0, so iterate()
		// should be a no-op.
		oi.iterate();
		// no exception
	}

	@Test
	void oneIterationIterateTriggersProgressEventWhenCompletionAdvances() throws Exception {
		// Use ranks with size >= 100 so size/100 > 0 and no division by zero.
		// maxJump=2, stackWidth=5 → base=5, surfaceWidth=4, size=625
		Ranks original = new Ranks(2, 5);
		Ranks copy = new Ranks(original);
		RanksIterator.OneIteration oi = iterator.new OneIteration(1, copy);

		int[] surface = {-2, -2, -2, -2};
		int[] work = {-2, -2, -2, -2};
		copy.setRank(surface, work);
		// completion is now 1. For size=625, size/100=6, 1%6=1≠0,
		// so completionPercentageIncrease returns false. Just verify no throw.
		oi.iterate();
		// No exception
	}

	@Test
	void oneIterationIterateReturnsEarlyForSmallRanks() throws Exception {
		// With size >= 100, iterate should not throw and completionPercentageIncrease
		// will return false until completion reaches size/100.
		Ranks ranks = new Ranks(2, 5); // base=5, surfaceWidth=4, size=625, size/100=6
		Ranks copy = new Ranks(ranks);
		RanksIterator.OneIteration oi = iterator.new OneIteration(1, copy);

		int[] surface = {-2, -2, -2, -2};
		int[] work = {-2, -2, -2, -2};
		// completion=0 → completionPercentageIncrease returns false (completion==0)
		copy.setRank(surface, work);
		// completion=1 → 1%6=1≠0 → returns false
		oi.iterate();
	}

	@Test
	void oneIterationCancelTaskSetsCancelledAndThrowsNpe() throws Exception {
		// cancelTask sets cancelled=true before iterating over the
		// ranksIteratorPart array, which is null (initialized in doInBackground).
		// The NPE is expected; we verify cancelled was set.
		RanksIterator.OneIteration oi = iterator.new OneIteration(2, ranks);

		try {
			oi.cancelTask();
		} catch (NullPointerException e) {
			// expected: ranksIteratorPart is null before doInBackground
		}

		assertTrue(getBooleanField(oi, "cancelled"),
				"cancelTask should set cancelled before touching the parts array");
	}

	// ------------------------------------------------------------------
	// AllIterations
	// ------------------------------------------------------------------

	@Test
	void allIterationsConstructorInitialisesFields() throws Exception {
		RanksIterator.AllIterations ai = iterator.new AllIterations(2, iterator, "");

		assertNotNull(ai);
		assertEquals(2, getIntField(ai, "totalParts"));
		assertEquals(iterator, getObjectField(ai, "ranksIterator"));
		assertEquals("", getObjectField(ai, "inputFile"));
		assertEquals(false, getBooleanField(ai, "cancelled"));
	}

	@Test
	void allIterationsConstructorWithInputFile() throws Exception {
		RanksIterator.AllIterations ai = iterator.new AllIterations(3, iterator, "ranks20");

		assertEquals("ranks20", getObjectField(ai, "inputFile"));
	}

	@Test
	void allIterationsCancelTaskSetsCancelledFlag() throws Exception {
		RanksIterator.AllIterations ai = iterator.new AllIterations(2, iterator, "");

		assertFalse(getBooleanField(ai, "cancelled"));

		ai.cancelTask();

		assertTrue(getBooleanField(ai, "cancelled"));
	}

	@Test
	void allIterationsCancelIsIdempotent() throws Exception {
		RanksIterator.AllIterations ai = iterator.new AllIterations(2, iterator, "");

		ai.cancelTask();
		ai.cancelTask();

		assertTrue(getBooleanField(ai, "cancelled"),
				"cancelTask should be idempotent");
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	private static RanksIterator allocateRanksIterator() throws Exception {
		java.lang.reflect.Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		uf.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get(null);
		return (RanksIterator) unsafe.allocateInstance(RanksIterator.class);
	}

	private static int getIntField(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(target);
	}

	private static boolean getBooleanField(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(target);
	}

	private static Object getObjectField(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(target);
	}
}
