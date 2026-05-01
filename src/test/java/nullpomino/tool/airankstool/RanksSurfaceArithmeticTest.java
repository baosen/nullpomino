package nullpomino.tool.airankstool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Piece;

import org.junit.jupiter.api.Test;

/**
 * Pins the surface-arithmetic helpers on {@link Ranks}. These power the
 * iterate / fit / add-to-heights pipeline that the AI tool uses to walk
 * every reachable surface state. The base-N counter, the heights-to-
 * surface diff, and the piece-fit / piece-add checks all need to stay
 * tied to {@link Ranks#PIECES_HEIGHTS} and friends; a regression here
 * would silently desync the persisted rank tables from the surface
 * states that produced them.
 */
class RanksSurfaceArithmeticTest {

	@Test
	void iterateSurfaceIncrementsLikeABaseNCounter() {
		Ranks r = withRanksFrom(2, 3); // surfaceWidth=2, base=5, maxJump=2
		int[] surface = {-2, -2};
		int[] work = {-2, -2};

		r.iterateSurface(surface, work);

		// Lowest digit goes from -2 to -1, no carry.
		assertArrayEquals(new int[] {-1, -2}, surface);
		assertArrayEquals(surface, work);
	}

	@Test
	void iterateSurfaceCarriesWhenLowestDigitOverflowsTheMaxJumpRange() {
		Ranks r = withRanksFrom(2, 3);
		int[] surface = {2, -2};
		int[] work = {2, -2};

		r.iterateSurface(surface, work);

		// Lowest digit overflowed: rolls back to -maxJump and carries 1
		// into the next slot.
		assertArrayEquals(new int[] {-2, -1}, surface);
		assertArrayEquals(surface, work);
	}

	@Test
	void iterateSurfaceWrapsAtAllMaxBackToAllMin() {
		Ranks r = withRanksFrom(2, 3);
		int[] surface = {2, 2};
		int[] work = {2, 2};

		r.iterateSurface(surface, work);

		// Every digit overflowed → wraps back to (-maxJump, -maxJump).
		assertArrayEquals(new int[] {-2, -2}, surface);
		assertArrayEquals(surface, work);
	}

	@Test
	void addToHeightsAccumulatesPerColumnHeightForOPiece() {
		Ranks r = new Ranks(4, 9);
		int[] heights = new int[9];

		// O piece (rotation 0) adds {2, 2} starting at column 0.
		r.addToHeights(heights, Piece.PIECE_O, 0, 0);

		assertEquals(2, heights[0]);
		assertEquals(2, heights[1]);
		// Columns the piece does not occupy stay at zero.
		for(int x = 2; x < heights.length; x++) assertEquals(0, heights[x]);
	}

	@Test
	void addToHeightsAccumulatesPerColumnHeightForLPieceAtOffset() {
		Ranks r = new Ranks(4, 9);
		int[] heights = new int[9];
		// Pre-populate so addToHeights's accumulation contract is observable.
		for(int x = 0; x < heights.length; x++) heights[x] = 1;

		// L piece rotation 0 adds {1, 1, 2} starting at column 3.
		r.addToHeights(heights, Piece.PIECE_L, 0, 3);

		assertEquals(2, heights[3]);
		assertEquals(2, heights[4]);
		assertEquals(3, heights[5]);
		// Untouched columns retain the pre-fill.
		assertEquals(1, heights[0]);
		assertEquals(1, heights[8]);
	}

	@Test
	void heightsToSurfaceProducesAdjacentDifferences() {
		Ranks r = new Ranks(4, 9);
		// Height[i+1] - Height[i] across the row, all within +-maxJump.
		int[] heights = {0, 1, 3, 0, 0, 0, 0, 0, 0};

		int[] surface = r.heightsToSurface(heights);

		// surface length is stackWidth - 1 == 8.
		assertEquals(8, surface.length);
		assertEquals(1, surface[0]);   // 1 - 0
		assertEquals(2, surface[1]);   // 3 - 1
		assertEquals(-3, surface[2]);  // 0 - 3
		assertEquals(0, surface[3]);
	}

	@Test
	void heightsToSurfaceClampsDifferencesAboveMaxJumpAndBelowNegativeMaxJump() {
		Ranks r = new Ranks(2, 4); // maxJump=2 → clamp at +-2
		int[] heights = {0, 5, 0, -10};

		int[] surface = r.heightsToSurface(heights);

		assertEquals(2, surface[0], "5 - 0 must clamp to +maxJump (2)");
		assertEquals(-2, surface[1], "0 - 5 must clamp to -maxJump (-2)");
		assertEquals(-2, surface[2], "-10 - 0 must clamp to -maxJump (-2)");
	}

	@Test
	void surfaceFitsPieceTrueForOPieceAtAFlatSurface() {
		Ranks r = new Ranks(4, 9);
		// Flat (all zeros) surface; PIECES_LOWESTS[O][0] = {1, 1} → no slope.
		int[] surface = new int[8];

		assertTrue(r.surfaceFitsPiece(surface, Piece.PIECE_O, 0, 0));
	}

	@Test
	void surfaceFitsPieceFalseWhenLocalSlopeDoesNotMatchPieceContour() {
		Ranks r = new Ranks(4, 9);
		// L piece rotation 2 lowests {2, 1, 1} → required deltas {1, 0}.
		// A flat surface has deltas {0, 0} so the first slot fails the
		// match against the required +1 step.
		int[] surface = new int[8];

		assertFalse(r.surfaceFitsPiece(surface, Piece.PIECE_L, 2, 0));
	}

	@Test
	void surfaceFitsPieceTrueWhenLocalSlopeMatchesPieceContour() {
		Ranks r = new Ranks(4, 9);
		// L piece rotation 2 lowests {2, 1, 1} → required deltas {1, 0}.
		// Plant surface[0] = 1 so the first delta matches; the rest stay 0.
		int[] surface = new int[8];
		surface[0] = 1;

		assertTrue(r.surfaceFitsPiece(surface, Piece.PIECE_L, 2, 0));
	}

	@Test
	void surfaceAddPossibleFalseWhenAddingTakesSlopeOutsideMaxJump() {
		Ranks r = new Ranks(2, 9); // maxJump=2 → tight clamp
		// Place a J piece at column 0 (PIECES_HEIGHTS[J][0] = {2, 1, 1}).
		// Pre-fill the work surface so the very first column delta would
		// exceed the maxJump clamp once the piece adds its heights.
		int[] work = new int[8];
		work[0] = 2;
		work[1] = 0;
		work[2] = 0;

		assertFalse(r.surfaceAddPossible(work, Piece.PIECE_J, 0, 1),
				"adding the J piece at column 1 with this base would push the "
						+ "left-neighbour delta past +maxJump");
	}

	@Test
	void surfaceAddPossibleTrueForLegalOPiecePlacementOnFlatSurface() {
		Ranks r = new Ranks(4, 9);
		// O piece at column 0 keeps the surface inside +-maxJump.
		int[] work = new int[8];

		assertTrue(r.surfaceAddPossible(work, Piece.PIECE_O, 0, 0));
	}

	private static Ranks withRanksFrom(int maxJump, int stackWidth) {
		// iterateSurface calls setRank → getRank → ranksFrom.getRankValue, so
		// every Ranks instance under test needs a ranksFrom backing table.
		// The copy constructor wires the original as ranksFrom.
		Ranks original = new Ranks(maxJump, stackWidth);
		return new Ranks(original);
	}
}
