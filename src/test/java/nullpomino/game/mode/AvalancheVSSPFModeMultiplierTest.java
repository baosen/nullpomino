package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pins the static multiplier lookups on {@link AvalancheVSSPFMode}:
 * {@code getAttackMultiplier} and {@code getDefendMultiplier} return
 * values from the DROP_PATTERNS_ATTACK/DEFEND_MULTIPLIERS tables with
 * bounds-checking that falls back to 1.0 on out-of-range indices.
 */
class AvalancheVSSPFModeMultiplierTest {

	@Test
	void getAttackMultiplierReturnsTableValueForValidIndices() {
		// DROP_PATTERNS_ATTACK_MULTIPLIERS[0] has 11 entries
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(0, 0));
		assertEquals(0.7, AvalancheVSSPFMode.getAttackMultiplier(0, 8));
		assertEquals(0.7, AvalancheVSSPFMode.getAttackMultiplier(0, 9));
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(0, 10));
	}

	@Test
	void getAttackMultiplierReturnsSecondSetValues() {
		// DROP_PATTERNS_ATTACK_MULTIPLIERS[1] has 11 entries
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(1, 0));
		assertEquals(1.2, AvalancheVSSPFMode.getAttackMultiplier(1, 1));
		assertEquals(0.85, AvalancheVSSPFMode.getAttackMultiplier(1, 9));
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(1, 10));
	}

	@Test
	void getAttackMultiplierReturnsOneForOutOfBoundsSet() {
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(-1, 0),
				"negative set index should fall back to 1.0");
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(2, 0),
				"set index beyond array length should fall back to 1.0");
	}

	@Test
	void getAttackMultiplierReturnsOneForOutOfBoundsMap() {
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(0, -1),
				"negative map index should fall back to 1.0");
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(0, 11),
				"map index beyond array length should fall back to 1.0");
		assertEquals(1.0, AvalancheVSSPFMode.getAttackMultiplier(1, 100),
				"large map index should fall back to 1.0");
	}

	@Test
	void getDefendMultiplierReturnsTableValueForValidIndices() {
		// DROP_PATTERNS_DEFEND_MULTIPLIERS[0] has 11 entries, all 1.0
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(0, 0));
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(0, 10));

		// DROP_PATTERNS_DEFEND_MULTIPLIERS[1] has 11 entries
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(1, 0));
		assertEquals(1.2, AvalancheVSSPFMode.getDefendMultiplier(1, 8));
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(1, 9));
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(1, 10));
	}

	@Test
	void getDefendMultiplierReturnsOneForOutOfBoundsSet() {
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(-1, 0),
				"negative set index should fall back to 1.0");
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(2, 0),
				"set index beyond array length should fall back to 1.0");
	}

	@Test
	void getDefendMultiplierReturnsOneForOutOfBoundsMap() {
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(0, -1),
				"negative map index should fall back to 1.0");
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(0, 11),
				"map index beyond array length should fall back to 1.0");
		assertEquals(1.0, AvalancheVSSPFMode.getDefendMultiplier(1, 100),
				"large map index should fall back to 1.0");
	}

	@Test
	void attackMultiplierFirstSetHasExpectedValues() {
		// Spot-check all values in the first set
		double[] expected = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 0.7, 0.7, 1.0};
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], AvalancheVSSPFMode.getAttackMultiplier(0, i),
					"attack multiplier set 0, map " + i);
		}
	}

	@Test
	void defendMultiplierSecondSetHasExpectedValues() {
		// Spot-check all values in the second set
		double[] expected = {1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.2, 1.0, 1.0};
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], AvalancheVSSPFMode.getDefendMultiplier(1, i),
					"defend multiplier set 1, map " + i);
		}
	}
}