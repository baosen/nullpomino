package nullpomino.game.randomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.Random;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Piece;

/**
 * Fills the last remaining branches in BagNoSZORandomizer and
 * LimitedHistoryRandomizer that the existing RandomizerBranchTest leaves
 * uncovered.
 */
class RandomizerBranchCoverageSweepTest {

	private static void setRandom(Randomizer r, long seed) {
		try {
			Field f = Randomizer.class.getDeclaredField("r");
			f.setAccessible(true);
			f.set(r, new Random(seed));
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	// =============== BagNoSZORandomizer L13 ===============

	/**
	 * BagNoSZORandomizer.shuffle(): firstBag==true but the piece set is SZO-only,
	 * so {@code !isPieceSZOOnly()} is false and the {@code if} takes its else arm
	 * (the only-S/Z/O sub-condition that the existing tests never reach).
	 */
	@Test
	void bagNoSZOFirstBagButSZOOnlyTakesElseArm() {
		BagNoSZORandomizer r = new BagNoSZORandomizer();
		setRandom(r, 12345L);
		boolean[] enable = new boolean[Piece.PIECE_COUNT];
		enable[Piece.PIECE_S] = true;
		enable[Piece.PIECE_Z] = true;
		enable[Piece.PIECE_O] = true;
		r.setPieceEnable(enable);
		r.init();

		// All enabled pieces are S/Z/O, so isPieceSZOOnly() is true and the
		// firstBag flag is never cleared (the do/while skip-SZO loop is bypassed).
		assertTrue(r.firstBag);

		int piece = r.next();
		assertTrue(piece == Piece.PIECE_S || piece == Piece.PIECE_Z || piece == Piece.PIECE_O);
	}

	/**
	 * BagNoSZORandomizer.shuffle() second invocation: with a mixed piece set the
	 * first bag clears {@code firstBag}, so the second shuffle (triggered by the
	 * else arm) runs with firstBag==false.
	 */
	@Test
	void bagNoSZOSecondShuffleRunsElseArm() {
		BagNoSZORandomizer r = new BagNoSZORandomizer();
		setRandom(r, 7L);
		boolean[] enable = new boolean[Piece.PIECE_COUNT];
		for (int i = 0; i < Piece.PIECE_COUNT; i++) enable[i] = true;
		r.setPieceEnable(enable);
		r.init();

		// First bag must have skipped S/Z/O at index 0 and cleared the flag.
		assertFalse(r.firstBag);
		assertFalse(r.isSZOPiece(r.bag[0]));

		// Second shuffle goes through the else arm; flag stays cleared.
		r.shuffle();
		assertFalse(r.firstBag);
	}

	// =============== LimitedHistoryRandomizer L19 ===============

	/**
	 * LimitedHistoryRandomizer.next(): on the first piece the do/while loop must
	 * repeat at least once when the first randomPieceIndex() lands on an S/Z/O
	 * piece. With seed 0 and pieces {T,S,Z,I} the first draw is index 2 (Z, an
	 * S/Z/O piece) which forces a re-roll to index 3 (I).
	 */
	@Test
	void limitedHistoryFirstPieceReRollsPastSZO() {
		History4RollsRandomizer r = new History4RollsRandomizer();
		setRandom(r, 0L);
		r.pieces = new int[]{Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_I};
		r.init();
		// init() resets history to Z's and numrolls=4; firstPiece is true.

		int piece = r.next();

		// The first draw (Z) is rejected by the while guard, the loop repeats and
		// returns I — proving the do/while body executed more than once.
		assertEquals(Piece.PIECE_I, piece);
		assertFalse(r.firstPiece);
	}
}
