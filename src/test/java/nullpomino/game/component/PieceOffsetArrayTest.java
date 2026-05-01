package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link Piece} per-direction offset helpers
 * (applyOffsetArray, applyOffsetArrayX, applyOffsetArrayY,
 * resetOffsetArray). Custom rule files can shift a piece's spawn
 * position via these arrays; a regression here would silently
 * desync stored replays from their re-rendered playback.
 */
class PieceOffsetArrayTest {

	@Test
	void applyOffsetArrayXShiftsAllBlocksInEachDirectionAndStampsTheArray() {
		Piece piece = new Piece(Piece.PIECE_T);
		// Snapshot the original block coords to verify the shift.
		int[][] originalX = copyDataX(piece);

		int[] offsetX = {1, 2, -1, 3};
		piece.applyOffsetArrayX(offsetX);

		assertTrue(piece.offsetApplied,
				"applyOffsetArrayX must set the offsetApplied flag");
		assertArrayEquals(offsetX, piece.dataOffsetX);

		// Each direction's block xs are shifted by the per-direction offset.
		for(int dir = 0; dir < Piece.DIRECTION_COUNT; dir++) {
			for(int b = 0; b < piece.getMaxBlock(); b++) {
				int delta = piece.dataX[dir][b] - originalX[dir][b];
				assertEquals(offsetX[dir], delta,
						"dir=" + dir + " block=" + b + " expected x shift " + offsetX[dir]);
			}
		}
	}

	@Test
	void applyOffsetArrayYShiftsAllBlocksInEachDirectionAndStampsTheArray() {
		Piece piece = new Piece(Piece.PIECE_T);
		int[][] originalY = copyDataY(piece);

		int[] offsetY = {2, -1, 0, 4};
		piece.applyOffsetArrayY(offsetY);

		assertTrue(piece.offsetApplied);
		assertArrayEquals(offsetY, piece.dataOffsetY);

		for(int dir = 0; dir < Piece.DIRECTION_COUNT; dir++) {
			for(int b = 0; b < piece.getMaxBlock(); b++) {
				int delta = piece.dataY[dir][b] - originalY[dir][b];
				assertEquals(offsetY[dir], delta);
			}
		}
	}

	@Test
	void applyOffsetArrayCallsBothXAndYAtTheSameTime() {
		Piece piece = new Piece(Piece.PIECE_L);
		int[][] originalX = copyDataX(piece);
		int[][] originalY = copyDataY(piece);

		int[] offsetX = {1, 0, -1, 2};
		int[] offsetY = {0, 1, -2, 1};
		piece.applyOffsetArray(offsetX, offsetY);

		assertTrue(piece.offsetApplied);
		assertArrayEquals(offsetX, piece.dataOffsetX);
		assertArrayEquals(offsetY, piece.dataOffsetY);

		for(int dir = 0; dir < Piece.DIRECTION_COUNT; dir++) {
			for(int b = 0; b < piece.getMaxBlock(); b++) {
				assertEquals(offsetX[dir],
						piece.dataX[dir][b] - originalX[dir][b]);
				assertEquals(offsetY[dir],
						piece.dataY[dir][b] - originalY[dir][b]);
			}
		}
	}

	@Test
	void resetOffsetArrayRestoresEveryBlockBackToTheDefaultPieceData() {
		Piece piece = new Piece(Piece.PIECE_J);
		int[][] originalX = copyDataX(piece);
		int[][] originalY = copyDataY(piece);

		// Apply non-trivial offsets to mutate every (dir, block) cell.
		piece.applyOffsetArray(new int[] {3, 1, -1, 2}, new int[] {-1, 2, 0, 5});

		piece.resetOffsetArray();

		// resetOffsetArray writes the canonical block coords back from
		// DEFAULT_PIECE_DATA_X/Y, so the contents match the snapshot.
		for(int dir = 0; dir < Piece.DIRECTION_COUNT; dir++) {
			assertArrayEquals(originalX[dir], piece.dataX[dir]);
			assertArrayEquals(originalY[dir], piece.dataY[dir]);
		}
	}

	@Test
	void resetOffsetArrayClearsOffsetAppliedFlag() {
		// resetOffsetArray restores block coords AND clears the
		// offsetApplied flag, putting the piece back into its pristine
		// 'no offsets stamped' state.
		Piece piece = new Piece(Piece.PIECE_S);
		piece.applyOffsetArrayX(new int[] {1, 1, 1, 1});
		assertTrue(piece.offsetApplied);

		piece.resetOffsetArray();

		assertFalse(piece.offsetApplied,
				"resetOffsetArray clears offsetApplied so subsequent calls "
						+ "see the piece as un-shifted");
	}

	@Test
	void freshPieceHasOffsetAppliedFalseAndZeroDataOffsetArrays() {
		Piece piece = new Piece(Piece.PIECE_O);

		assertFalse(piece.offsetApplied);
		// dataOffsetX / dataOffsetY are sized to DIRECTION_COUNT and
		// initially all-zero (no offsets seeded).
		assertEquals(Piece.DIRECTION_COUNT, piece.dataOffsetX.length);
		for(int v : piece.dataOffsetX) assertEquals(0, v);
		for(int v : piece.dataOffsetY) assertEquals(0, v);
	}

	private static int[][] copyDataX(Piece piece) {
		int[][] copy = new int[Piece.DIRECTION_COUNT][];
		for(int i = 0; i < Piece.DIRECTION_COUNT; i++) {
			copy[i] = piece.dataX[i].clone();
		}
		return copy;
	}

	private static int[][] copyDataY(Piece piece) {
		int[][] copy = new int[Piece.DIRECTION_COUNT][];
		for(int i = 0; i < Piece.DIRECTION_COUNT; i++) {
			copy[i] = piece.dataY[i].clone();
		}
		return copy;
	}
}
