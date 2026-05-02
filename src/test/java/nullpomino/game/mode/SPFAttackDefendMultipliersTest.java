package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pins the static drop-pattern multipliers and row-value lookup that
 * SPFMode and AvalancheVSSPFMode use to compute ojama-send and
 * ojama-receive scaling for each drop-pattern map. Both modes expose
 * identical {@code getAttackMultiplier} / {@code getDefendMultiplier}
 * tables (set 0 = CLASSIC, set 1 = REMIX), and SPFMode adds
 * {@code getRowValue} for per-row attack scaling.
 *
 * <p>Out-of-range lookups must fall through to a 1.0 fallback rather
 * than throwing — the engine reaches into these tables every chain so
 * an invalid set/map index would crash mid-game.
 */
class SPFAttackDefendMultipliersTest {

	@Test
	void spfAttackMultiplierSetZeroIsAllOnesExceptTwoMapsAtZeroSeven() {
		// set 0 = CLASSIC: maps 8 and 9 attack at 0.7x.
		assertEquals(1.0, SPFMode.getAttackMultiplier(0, 0));
		assertEquals(1.0, SPFMode.getAttackMultiplier(0, 7));
		assertEquals(0.7, SPFMode.getAttackMultiplier(0, 8));
		assertEquals(0.7, SPFMode.getAttackMultiplier(0, 9));
		assertEquals(1.0, SPFMode.getAttackMultiplier(0, 10));
	}

	@Test
	void spfAttackMultiplierSetOneRaisesMapOneAndDimsMapNine() {
		// set 1 = REMIX: map 1 attacks at 1.2x, map 9 at 0.85x.
		assertEquals(1.0, SPFMode.getAttackMultiplier(1, 0));
		assertEquals(1.2, SPFMode.getAttackMultiplier(1, 1));
		assertEquals(0.85, SPFMode.getAttackMultiplier(1, 9));
		assertEquals(1.0, SPFMode.getAttackMultiplier(1, 10));
	}

	@Test
	void spfDefendMultiplierSetZeroIsAllOnes() {
		// set 0: every map defends at base 1.0x.
		for(int map = 0; map <= 10; map++) {
			assertEquals(1.0, SPFMode.getDefendMultiplier(0, map),
					"set 0, map " + map + " must default to 1.0x defend");
		}
	}

	@Test
	void spfDefendMultiplierSetOneRaisesMapEight() {
		// set 1: only map 8 has a non-1.0 defend value (1.2x).
		assertEquals(1.0, SPFMode.getDefendMultiplier(1, 0));
		assertEquals(1.0, SPFMode.getDefendMultiplier(1, 7));
		assertEquals(1.2, SPFMode.getDefendMultiplier(1, 8));
		assertEquals(1.0, SPFMode.getDefendMultiplier(1, 9));
		assertEquals(1.0, SPFMode.getDefendMultiplier(1, 10));
	}

	@Test
	void spfMultipliersFallBackToOnePointZeroForOutOfRangeSet() {
		// set out of range -> AIOOBE caught and 1.0 returned.
		assertEquals(1.0, SPFMode.getAttackMultiplier(99, 0));
		assertEquals(1.0, SPFMode.getAttackMultiplier(-1, 5));
		assertEquals(1.0, SPFMode.getDefendMultiplier(99, 0));
		assertEquals(1.0, SPFMode.getDefendMultiplier(-1, 5));
	}

	@Test
	void spfMultipliersFallBackToOnePointZeroForOutOfRangeMap() {
		// map out of range for a known set -> still 1.0.
		assertEquals(1.0, SPFMode.getAttackMultiplier(0, 99));
		assertEquals(1.0, SPFMode.getAttackMultiplier(0, -1));
		assertEquals(1.0, SPFMode.getDefendMultiplier(1, 99));
		assertEquals(1.0, SPFMode.getDefendMultiplier(1, -1));
	}

	@Test
	void avalancheVSSPFTablesMatchSPFTablesForBothMultipliers() {
		// AvalancheVSSPFMode duplicates the same DROP_PATTERNS_*
		// multipliers; pin that the byte shape stays identical so a
		// future refactor that consolidates them doesn't accidentally
		// shift values.
		for(int set = 0; set < 2; set++) {
			for(int map = 0; map < 11; map++) {
				assertEquals(SPFMode.getAttackMultiplier(set, map),
						AvalancheVSSPFMode.getAttackMultiplier(set, map),
						"attack multiplier mismatch at set=" + set + " map=" + map);
				assertEquals(SPFMode.getDefendMultiplier(set, map),
						AvalancheVSSPFMode.getDefendMultiplier(set, map),
						"defend multiplier mismatch at set=" + set + " map=" + map);
			}
		}
	}

	@Test
	void avalancheVSSPFMultipliersFallBackToOnePointZeroForOutOfRange() {
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(99, 0));
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(99, 0));
	}

	@Test
	void getRowValueRangesFromTwoPointThreeAtTopToOnePointZeroAtBottom() {
		// ROW_VALUES = {2.3, 2.2, 2.1, 2.0, 1.9, 1.8, 1.7, 1.6, 1.5,
		// 1.4, 1.3, 1.2, 1.1, 1.0}. row 0 = top of board (newest).
		assertEquals(2.3, SPFMode.getRowValue(0));
		assertEquals(2.2, SPFMode.getRowValue(1));
		assertEquals(1.0, SPFMode.getRowValue(13));
	}

	@Test
	void getRowValueClampsNegativeRowsToTop() {
		// Below the visible range -> clamp to row 0 (highest value).
		assertEquals(2.3, SPFMode.getRowValue(-1));
		assertEquals(2.3, SPFMode.getRowValue(-100));
	}

	@Test
	void getRowValueClampsPastBottomToOnePointZero() {
		// Beyond the table -> clamp to the last row (lowest value).
		assertEquals(1.0, SPFMode.getRowValue(14));
		assertEquals(1.0, SPFMode.getRowValue(99));
	}
}
