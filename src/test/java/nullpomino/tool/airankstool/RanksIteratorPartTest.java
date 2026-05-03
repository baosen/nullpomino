package nullpomino.tool.airankstool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests RanksIteratorPart constructor logic and field initialisation.
 *
 * <p>RanksIteratorPart extends Thread and is the per-part worker that
 * the {@link RanksIterator.OneIteration} SwingWorker spawns. The
 * constructor divides the total surface range ({@code size}) into
 * {@code totalParts} contiguous sub-ranges and stores the per-part
 * iteration bounds ({@code sMin}, {@code sMax}) and initial decoded
 * surfaces.
 *
 * <p>Since RanksIteratorPart is package-private and its fields are
 * all private with no getters, we use reflection to verify
 * constructor-side effects and check the Thread name/priority.
 */
class RanksIteratorPartTest {

	private Ranks ranks;
	private RanksIterator ranksIterator;

	@BeforeEach
	void setUp() throws Exception {
		ranks = new Ranks(4, 9);            // maxJump=4, stackWidth=9, surfaceWidth=8, base=9, size=9^8=43046721
		ranksIterator = allocateRanksIterator();
	}

	@Test
	void constructorCreatesNonNullInstance() throws Exception {
		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(2, ranks);
		RanksIteratorPart part = new RanksIteratorPart(oi, ranks, 0, 1);

		assertNotNull(part);
	}

	@Test
	void constructorSetsThreadName() throws Exception {
		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(2, ranks);
		RanksIteratorPart part = new RanksIteratorPart(oi, ranks, 0, 1);

		assertTrue(part.getName() != null);
	}

	@Test
	void constructorSetsMinimumPriority() throws Exception {
		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(2, ranks);
		RanksIteratorPart part = new RanksIteratorPart(oi, ranks, 0, 1);

		assertEquals(Thread.MIN_PRIORITY, part.getPriority());
	}

	@Test
	void singlePartCoversFullRange() throws Exception {
		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(1, ranks);
		RanksIteratorPart part = new RanksIteratorPart(oi, ranks, 0, 1);

		int size = ranks.getSize();
		assertEquals(0, getIntField(part, "sMin"));
		assertEquals(size, getIntField(part, "sMax"));
	}

	@Test
	void twoPartsDivideRangeEqually() throws Exception {
		int totalParts = 2;
		int size = ranks.getSize();
		int half = size / totalParts; // size is even for 9^8

		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(totalParts, ranks);

		RanksIteratorPart part0 = new RanksIteratorPart(oi, ranks, 0, totalParts);
		assertEquals(0, getIntField(part0, "sMin"));
		assertEquals(half, getIntField(part0, "sMax"));

		RanksIteratorPart part1 = new RanksIteratorPart(oi, ranks, 1, totalParts);
		assertEquals(half, getIntField(part1, "sMin"));
		assertEquals(size, getIntField(part1, "sMax"));
	}

	@Test
	void firstPartGetsFirstSliceThreeParts() throws Exception {
		int totalParts = 3;
		int size = ranks.getSize();
		int slice = size / totalParts; // integer division

		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(totalParts, ranks);
		RanksIteratorPart part0 = new RanksIteratorPart(oi, ranks, 0, totalParts);

		assertEquals(0, getIntField(part0, "sMin"));
		assertEquals(slice, getIntField(part0, "sMax"));
	}

	@Test
	void lastPartGetsRemainder() throws Exception {
		int totalParts = 3;
		int size = ranks.getSize();
		int slice = size / totalParts;

		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(totalParts, ranks);
		RanksIteratorPart part2 = new RanksIteratorPart(oi, ranks, 2, totalParts);

		assertEquals(2 * slice, getIntField(part2, "sMin"));
		assertEquals(size, getIntField(part2, "sMax"), "last part covers the remainder");
	}

	@Test
	void constructorDecodesSurfaceAtSMin() throws Exception {
		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(2, ranks);
		RanksIteratorPart part = new RanksIteratorPart(oi, ranks, 0, 2);

		// surface and surfaceDecodedWork should both decode sMin=0, so all
		// entries should be -maxJump = -4.
		int[] surface = getIntArrayField(part, "surface");
		assertNotNull(surface);
		assertEquals(ranks.getStackWidth() - 1, surface.length);
		for (int val : surface) {
			assertEquals(-ranks.getMaxJump(), val);
		}

		int[] surfaceDecodedWork = getIntArrayField(part, "surfaceDecodedWork");
		assertNotNull(surfaceDecodedWork);
		assertEquals(surface.length, surfaceDecodedWork.length);
		assertArrayEquals(surface, surfaceDecodedWork);
	}

	@Test
	void constructorDecodesNonZeroSMinCorrectly() throws Exception {
		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(2, ranks);
		int half = ranks.getSize() / 2;
		RanksIteratorPart part = new RanksIteratorPart(oi, ranks, 1, 2);

		int sMin = getIntField(part, "sMin");
		assertEquals(half, sMin);

		// Verify the decoded surface matches what Ranks.decode produces.
		int[] surface = getIntArrayField(part, "surface");
		int[] expected = new int[ranks.getStackWidth() - 1];
		ranks.decode(half, expected);
		assertArrayEquals(expected, surface);
	}

	@Test
	void differentPartsDecodeDifferentSurfaces() throws Exception {
		int totalParts = 3;
		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(totalParts, ranks);

		RanksIteratorPart part0 = new RanksIteratorPart(oi, ranks, 0, totalParts);
		RanksIteratorPart part1 = new RanksIteratorPart(oi, ranks, 1, totalParts);

		int[] surface0 = getIntArrayField(part0, "surface");
		int[] surface1 = getIntArrayField(part1, "surface");

		boolean allSame = true;
		for (int i = 0; i < surface0.length; i++) {
			if (surface0[i] != surface1[i]) {
				allSame = false;
				break;
			}
		}
		// For a non-trivial ranks (size > totalParts), the two parts should
		// start at different surface states.
		assertTrue(ranks.getSize() > totalParts);
		// sMin differs for part 0 and part 1 (unless totalParts==1).
		// Since sMin differs, decode(sMin) should differ for identical ranks
		// unless sMin happens to encode to the same surface (very unlikely
		// when totalParts < size and maxJump>0).
		assertEquals(false, allSame,
				"Parts should start at different encoded surfaces");
	}

	@Test
	void constructorInitialisesArraysWithCorrectLength() throws Exception {
		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(2, ranks);
		RanksIteratorPart part = new RanksIteratorPart(oi, ranks, 0, 2);

		int expectedLen = ranks.getStackWidth() - 1; // = 8 for 4,9
		int[] surface = getIntArrayField(part, "surface");
		int[] surfaceDecodedWork = getIntArrayField(part, "surfaceDecodedWork");

		assertEquals(expectedLen, surface.length);
		assertEquals(expectedLen, surfaceDecodedWork.length);
	}

	@Test
	void constructorStoresSizeField() throws Exception {
		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(2, ranks);
		RanksIteratorPart part = new RanksIteratorPart(oi, ranks, 0, 2);

		assertEquals(ranks.getSize(), getIntField(part, "size"));
	}

	@Test
	void lastPartHandlesExactDivision() throws Exception {
		// When size is evenly divisible by totalParts, the last part gets
		// exactly its nominal slice (not the remainder extension).
		Ranks smallRanks = new Ranks(2, 3); // base=5, surfaceWidth=2, size=25
		int totalParts = 5; // 25 / 5 = 5, exact
		int slice = 5;

		RanksIterator.OneIteration oi = ranksIterator.new OneIteration(totalParts, smallRanks);
		RanksIteratorPart part4 = new RanksIteratorPart(oi, smallRanks, 4, totalParts);

		assertEquals(4 * slice, getIntField(part4, "sMin"));
		assertEquals(smallRanks.getSize(), getIntField(part4, "sMax"),
				"Last part covers up to size when totalParts divides size evenly");
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	/** Allocate a RanksIterator without running its constructor (which calls setVisible). */
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

	private static int[] getIntArrayField(Object target, String name) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return (int[]) f.get(target);
	}
}
