package nullpomino.tool.airankstool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests RanksResult's non-Swing inner classes: {@code SurfaceRank} and
 * {@code SurfaceComparator}.
 *
 * <p>RanksResult extends JDialog; its constructor calls pack() and
 * setVisible(true) which fail headless. We use {@code Unsafe.allocateInstance}
 * to create an instance, then populate the private fields the inner classes
 * read from the enclosing scope ({@code ranks} and {@code factorCompare}).
 */
class RanksResultInnerClassTest {

	private RanksResult result;
	private Ranks ranks;

	@BeforeEach
	void setUp() throws Exception {
		result = allocateRanksResult();
		ranks = new Ranks(2, 3); // small ranks: base=5, surfaceWidth=2, size=25

		// All RanksResult inner classes read 'ranks' and 'factorCompare' from
		// the enclosing instance.
		setPrivateField(result, "ranks", ranks);
		setPrivateField(result, "factorCompare", 1); // ascendant=false
	}

	// ------------------------------------------------------------------
	// SurfaceRank — data class
	// ------------------------------------------------------------------

	@Test
	void surfaceRankConstructorSetsSurfaceAndRank() {
		RanksResult.SurfaceRank sr = result.new SurfaceRank(42, 100);

		assertEquals(42, sr.getSurface());
		assertEquals(100, sr.getRank());
	}

	@Test
	void surfaceRankDefaultValues() {
		RanksResult.SurfaceRank sr = result.new SurfaceRank(0, 0);

		assertEquals(0, sr.getSurface());
		assertEquals(0, sr.getRank());
	}

	@Test
	void surfaceRankCompareToEqual() {
		// factorCompare=1 (ascendant=false): higher rank is "greater"
		RanksResult.SurfaceRank sr1 = result.new SurfaceRank(0, 50);
		RanksResult.SurfaceRank sr2 = result.new SurfaceRank(1, 50);

		assertEquals(0, sr1.compareTo(sr2));
	}

	@Test
	void surfaceRankCompareToGreaterWhenFactorCompareIsOne() {
		// factorCompare=1: compareTo uses o.rank > this.rank → return 1 (= this > o).
		// So 30.compareTo(70) = 1 (30 > 70 in ordering → descending: higher first).
		// And 70.compareTo(30) = -1 (70 < 30 in ordering → 70 comes before 30).
		RanksResult.SurfaceRank srLow = result.new SurfaceRank(0, 30);
		RanksResult.SurfaceRank srHigh = result.new SurfaceRank(1, 70);

		assertTrue(srLow.compareTo(srHigh) > 0,
				"factorCompare=1: lower rank compareTo(higher) should be > 0");
		assertTrue(srHigh.compareTo(srLow) < 0,
				"factorCompare=1: higher rank compareTo(lower) should be < 0");
	}

	@Test
	void surfaceRankCompareToReversedWhenFactorCompareIsNegative() throws Exception {
		// factorCompare=-1: compareTo uses -o.rank > -this.rank → return 1.
		// So 30.compareTo(70): -70 > -30? No → -70 < -30 → return -1 (30 < 70).
		// And 70.compareTo(30): -30 > -70? Yes → return 1 (70 > 30).
		// So higher rank compareTo(lower) > 0 → ascending (lower first).
		setPrivateField(result, "factorCompare", -1);

		RanksResult.SurfaceRank srLow = result.new SurfaceRank(0, 30);
		RanksResult.SurfaceRank srHigh = result.new SurfaceRank(1, 70);

		assertTrue(srLow.compareTo(srHigh) < 0,
				"factorCompare=-1: lower rank compareTo(higher) should be < 0");
		assertTrue(srHigh.compareTo(srLow) > 0,
				"factorCompare=-1: higher rank compareTo(lower) should be > 0");
	}

	@Test
	void surfaceRankCompareToReturnsZeroForEqualRanks() {
		RanksResult.SurfaceRank srA = result.new SurfaceRank(10, 100);
		RanksResult.SurfaceRank srB = result.new SurfaceRank(20, 100);

		// compareTo returns 0 for equal ranks regardless of surface index
		assertEquals(0, srA.compareTo(srB));
		assertEquals(0, srB.compareTo(srA));
	}

	@Test
	void surfaceRankCompareToIsConsistentWithEquals() {
		RanksResult.SurfaceRank srA = result.new SurfaceRank(10, 100);
		RanksResult.SurfaceRank srB = result.new SurfaceRank(20, 100);

		assertEquals(0, srA.compareTo(srB));
		assertEquals(0, srB.compareTo(srA));
	}

	// ------------------------------------------------------------------
	// SurfaceComparator — Comparator<Integer>
	// ------------------------------------------------------------------

	@Test
	void surfaceComparatorCompareEqual() {
		// Set up ranks so that both indices have the same rank value.
		// Initially all ranks are Integer.MAX_VALUE.
		int val = 100;
		setRankValue(0, val);
		setRankValue(1, val);

		RanksResult.SurfaceComparator cmp = result.new SurfaceComparator();

		assertEquals(0, cmp.compare(0, 1));
	}

	@Test
	void surfaceComparatorCompareGreater() {
		setRankValue(0, 30);
		setRankValue(1, 70);

		RanksResult.SurfaceComparator cmp = result.new SurfaceComparator();

		// factorCompare=1, rank(0)=30, rank(1)=70
		// compare(0,1): rank(o2=1)=70 > rank(o1=0)=30 → return 1 → o1(30) > o2(70)
		// So lower rank compares as "greater" → descending order (higher rank first)
		assertTrue(cmp.compare(0, 1) > 0,
				"factorCompare=1: lower rank index compare(higher) should be > 0");
		assertTrue(cmp.compare(1, 0) < 0,
				"factorCompare=1: higher rank index compare(lower) should be < 0");
	}

	@Test
	void surfaceComparatorCompareReversed() throws Exception {
		setPrivateField(result, "factorCompare", -1);

		setRankValue(0, 30);
		setRankValue(1, 70);

		RanksResult.SurfaceComparator cmp = result.new SurfaceComparator();

		// factorCompare=-1, rank(0)=30, rank(1)=70
		// compare(0,1): -1*rank(1)=-70 > -1*rank(0)=-30? No → -70 < -30 → return -1
		// So compare(0,1) < 0 → ascending order (lower rank first)
		assertTrue(cmp.compare(0, 1) < 0,
				"factorCompare=-1: lower rank compare(higher) should be < 0");
		assertTrue(cmp.compare(1, 0) > 0,
				"factorCompare=-1: higher rank compare(lower) should be > 0");
	}

	@Test
	void surfaceComparatorCompareWithMaxValueSentinel() {
		// Unset ranks are Integer.MAX_VALUE.
		setRankValue(0, 100); // set one

		RanksResult.SurfaceComparator cmp = result.new SurfaceComparator();

		// factorCompare=1, rank(0)=100, rank(1)=MAX_VALUE
		// compare(0,1): MAX_VALUE > 100 → return 1 → o1(0) > o2(1)
		// So 100 > MAX_VALUE in ordering → MAX_VALUE "less" than 100
		// Actually: compare(0,1): o2.rank=MAX_VALUE > o1.rank=100 → return 1
		// So o1 > o2 → index 0 (rank=100) > index 1 (MAX_VALUE)
		// MAX_VALUE sorts as "lesser" → comes first (lower first)
		assertTrue(cmp.compare(0, 1) > 0,
				"set rank compare(MAX_VALUE) should be > 0 (MAX_VALUE sentinel appears smaller)");
		assertTrue(cmp.compare(1, 0) < 0,
				"MAX_VALUE compare(set rank) should be < 0");
	}

	@Test
	void surfaceComparatorIsSerializable() {
		RanksResult.SurfaceComparator cmp = result.new SurfaceComparator();
		assertTrue(cmp instanceof Comparator, "should implement Comparator");
	}

	// ------------------------------------------------------------------
	// Helpers — private
	// ------------------------------------------------------------------

	@SuppressWarnings("deprecation")
	private static RanksResult allocateRanksResult() throws Exception {
		Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		uf.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe) uf.get(null);
		return (RanksResult) unsafe.allocateInstance(RanksResult.class);
	}

	private static void setPrivateField(Object target, String name, Object value) throws Exception {
		Field f = target.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.set(target, value);
	}

	/** Utility: set the rank value at a surface index directly on the Ranks array. */
	private void setRankValue(int surfaceIndex, int value) {
		// ranks is private; use reflection to write into ranks.ranks[]
		try {
			Field ranksField = Ranks.class.getDeclaredField("ranks");
			ranksField.setAccessible(true);
			int[] arr = (int[]) ranksField.get(ranks);
			arr[surfaceIndex] = value;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
