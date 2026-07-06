package nullpomino.game.randomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gap in {@link DistanceWeightRandomizer#next}
 * (L30): the roll-search loop running to completion without a break. A
 * spec-conforming {@link Random#nextInt(int)} always returns less than the
 * cumulative total, so the exhaustion path is only reachable through an
 * out-of-contract Random; the defensive fallback then selects index 0.
 */
class DistanceWeightRandomizerBranchGapTest {

	@Test
	void rollBeyondCumulativeTotalFallsBackToFirstPiece() {
		LinearDistWeightRandomizer randomizer = new LinearDistWeightRandomizer();
		randomizer.pieces = new int[] { 0, 1 }; // initWeights 3 and 3
		randomizer.init();
		randomizer.r = new Random() {
			@Override
			public int nextInt(int bound) {
				return bound; // out of contract: roll == sum >= every cumulative entry
			}
		};

		assertEquals(0, randomizer.next(), "exhausted search must fall back to index 0");
	}
}
