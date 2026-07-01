package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.component.Piece;

import org.junit.jupiter.api.Test;

/**
 * Deep branch-coverage tests for {@link ComboRaceSeedSearch}.
 *
 * <p>The remaining residual branch that is actually reachable from a unit test
 * is the {@code holdID < pieceScores.length} guard in {@code thinkMain}'s
 * terminal case (line 172): all normal piece ids are 0..6 which is always
 * {@code < pieceScores.length (7)}, so the false side only fires when a
 * deliberately out-of-range hold id is supplied. This test pins both the true
 * side (in-range, non-I hold gets a scaled bonus) and the false side
 * (out-of-range hold gets no bonus).
 *
 * <p>The other {@code taken<total} lines reported by the coverage tool live in
 * {@code main()} (the {@code seed < Long.MAX_VALUE} loop guard and the endless-
 * loop detection threshold) and inside the deterministic static
 * {@code createTables()} construction — see the class-level report for why those
 * are unreachable from a unit test.
 */
class ComboRaceSeedSearchDeepBranchCoverageTest {

	/** Terminal, in-range non-I hold: adds pieceScores[hold]*100/28 (line 172 true side). */
	@Test
	void thinkMainTerminalInRangeHoldAddsScaledBonus() {
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		// state 0 -> stateScores[0]*100 = 600. Hold = O (id 2), pieceScores[2] = 10,
		// bonus = 10*100/28 = 35. Total = 635.
		int pts = ComboRaceSeedSearch.thinkMain(0, Piece.PIECE_O,
				ComboRaceSeedSearch.MAX_THINK_DEPTH);

		assertEquals(600 + (10 * 100 / 28), pts,
				"In-range non-I hold at terminal depth adds scaled piece bonus");
	}

	/** Terminal, hold id == I: adds the flat 1000 bonus (guards the I branch). */
	@Test
	void thinkMainTerminalHoldIAddsFlatBonus() {
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		int pts = ComboRaceSeedSearch.thinkMain(0, Piece.PIECE_I,
				ComboRaceSeedSearch.MAX_THINK_DEPTH);

		assertEquals(600 + 1000, pts,
				"I hold at terminal depth adds the flat 1000 bonus");
	}

	/**
	 * Terminal, out-of-range hold id (>= pieceScores.length): the else-if guard
	 * {@code holdID < pieceScores.length} is false, so no bonus is added
	 * (line 172 false side).
	 */
	@Test
	void thinkMainTerminalOutOfRangeHoldAddsNoBonus() {
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		// holdID = 7 == pieceScores.length -> not I, and NOT < 7 -> no bonus.
		int pts = ComboRaceSeedSearch.thinkMain(0, 7,
				ComboRaceSeedSearch.MAX_THINK_DEPTH);

		assertEquals(600, pts,
				"Out-of-range hold id at terminal depth adds no bonus");
	}

	/**
	 * Terminal, negative hold id other than -1 path: holdID < 0 also makes the
	 * else-if false (holdID >= 0 is false), covering the first conjunct's false
	 * side without an I match.
	 */
	@Test
	void thinkMainTerminalNegativeHoldAddsNoBonus() {
		ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

		int pts = ComboRaceSeedSearch.thinkMain(0, -1,
				ComboRaceSeedSearch.MAX_THINK_DEPTH);

		assertEquals(600, pts,
				"Negative hold id at terminal depth adds no bonus");
	}
}
