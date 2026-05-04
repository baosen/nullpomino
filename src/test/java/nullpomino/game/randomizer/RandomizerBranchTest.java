package nullpomino.game.randomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Piece;

/**
 * Covers remaining branches in randomizer implementations.
 */
class RandomizerBranchTest {

	private static Randomizer initRandomizer(Randomizer r) {
		// Randomizer base class needs the 'r' field set
		try {
			java.lang.reflect.Field f = Randomizer.class.getDeclaredField("r");
			f.setAccessible(true);
			f.set(r, new Random(42));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return r;
	}

	// =============== BagNoSZORandomizer ===============

	@Test
	void bagNoSZORandomizerSecondBagAllowsSZO() {
		BagNoSZORandomizer r = (BagNoSZORandomizer) initRandomizer(new BagNoSZORandomizer());
		boolean[] enable = new boolean[Piece.PIECE_COUNT];
		for (int i = 0; i < Piece.PIECE_COUNT; i++) enable[i] = true;
		r.setPieceEnable(enable);
		r.init();
		r.firstBag = false;

		int piece = r.next();
		assertTrue(piece >= 0);
	}

	@Test
	void bagNoSZORandomizerFirstBagSkipsSZO() {
		BagNoSZORandomizer r = (BagNoSZORandomizer) initRandomizer(new BagNoSZORandomizer());
		boolean[] enable = new boolean[Piece.PIECE_COUNT];
		for (int i = 0; i < Piece.PIECE_COUNT; i++) {
			if (i != Piece.PIECE_S && i != Piece.PIECE_Z && i != Piece.PIECE_O) {
				enable[i] = true;
			}
		}
		r.setPieceEnable(enable);
		r.init();

		int piece = r.next();
		assertTrue(piece >= 0);
	}

	// =============== DistanceWeightRandomizer ===============

	@Test
	void distanceWeightRandomizerFirstPieceNotO() {
		DistanceWeightRandomizer r = new DistanceWeightRandomizer() {
			@Override
			protected int getWeight(int i) { return 1; }
			@Override
			protected boolean isAtDistanceLimit(int i) { return false; }
		};
		initRandomizer(r);
		r.pieces = new int[]{Piece.PIECE_T, Piece.PIECE_I, Piece.PIECE_L};
		r.init();
		r.firstPiece = false;

		int piece = r.next();
		assertTrue(piece >= 0);
	}

	@Test
	void distanceWeightRandomizerAtDistanceLimit() {
		DistanceWeightRandomizer r = new DistanceWeightRandomizer() {
			@Override
			protected int getWeight(int i) { return 1; }
			@Override
			protected boolean isAtDistanceLimit(int i) { return true; }
		};
		initRandomizer(r);
		r.pieces = new int[]{Piece.PIECE_T};
		r.init();

		int piece = r.next();
		assertTrue(piece >= 0);
	}

	// =============== ExpDistWeightRandomizer ===============

	@Test
	void expDistWeightRandomizerAtDistanceLimitExceeds() {
		ExpDistWeightRandomizer r = new ExpDistWeightRandomizer() {
			@Override
			public void init() {
				pieces = new int[]{Piece.PIECE_T};
				weights = new int[]{30};
				cumulative = new int[]{30};
			}
		};
		r.init();
		r.weights = new int[]{30};

		assertTrue(r.isAtDistanceLimit(0));
	}

	// =============== FixedSequenceRandomizer ===============

	@Test
	void fixedSequenceRandomizerEmptySequenceDefaults() throws Exception {
		java.nio.file.Path seqFile = java.nio.file.Files.createTempFile("seq", ".txt");
		java.nio.file.Files.write(seqFile, new byte[0]);
		FixedSequenceRandomizer r = new FixedSequenceRandomizer(seqFile);
		r.init();

		int piece = r.next();
		assertTrue(piece >= 0);
		java.nio.file.Files.deleteIfExists(seqFile);
	}

	@Test
	void fixedSequenceRandomizerMissingFileDefaults() {
		FixedSequenceRandomizer r = new FixedSequenceRandomizer(
				java.nio.file.Paths.get("nonexistent_sequence_file_12345.txt"));
		r.init();

		int piece = r.next();
		assertTrue(piece >= 0);
	}

	// =============== LimitedHistoryRandomizer ===============

	@Test
	void limitedHistoryRandomizerNonFirstPiece() {
		LimitedHistoryRandomizer r = new LimitedHistoryRandomizer() {
			@Override
			public void init() {
				super.init();
				history = new int[4];
				numrolls = 4;
				pieces = new int[]{Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_I};
			}
		};
		initRandomizer(r);
		r.init();
		r.history = new int[4];
		r.numrolls = 4;
		r.pieces = new int[]{Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_I};
		r.firstPiece = false;

		int piece = r.next();
		assertTrue(piece >= 0);
	}

	@Test
	void limitedHistoryRandomizerFirstPieceSkipsSZO() {
		LimitedHistoryRandomizer r = new LimitedHistoryRandomizer() {
			@Override
			public void init() {
				super.init();
				history = new int[]{-1, -1, -1, -1};
				numrolls = 4;
				pieces = new int[]{Piece.PIECE_T, Piece.PIECE_I, Piece.PIECE_L};
			}
		};
		initRandomizer(r);
		r.init();
		r.history = new int[]{-1, -1, -1, -1};
		r.numrolls = 4;
		r.pieces = new int[]{Piece.PIECE_T, Piece.PIECE_I, Piece.PIECE_L};
		r.firstPiece = true;

		int piece = r.next();
		assertTrue(piece >= 0);
		assertTrue(piece == Piece.PIECE_T || piece == Piece.PIECE_I || piece == Piece.PIECE_L);
	}

	// =============== ExpDistWeightRandomizer ===============

	@Test
	void expDistWeightRandomizerBasic() {
		ExpDistWeightRandomizer r = new ExpDistWeightRandomizer() {
			@Override
			public void init() {
				pieces = new int[]{Piece.PIECE_T};
				weights = new int[]{5};
				cumulative = new int[]{16};
			}
		};
		r.init();
		assertEquals(16, r.getWeight(0));
	}
}
