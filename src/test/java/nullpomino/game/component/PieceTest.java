package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class PieceTest {

	@Test
	void copyConstructorCopiesMutableArraysAndBlocksWithoutSharing() {
		Piece original = new Piece(Piece.PIECE_T);
		original.direction = Piece.DIRECTION_RIGHT;
		original.setColor(Block.BLOCK_COLOR_RED);
		original.applyOffsetArray(
				new int[] {1, 2, 3, 4},
				new int[] {5, 6, 7, 8});

		Piece copy = new Piece(original);
		original.dataX[Piece.DIRECTION_RIGHT][0] = 99;
		original.dataY[Piece.DIRECTION_RIGHT][0] = 88;
		original.dataOffsetX[Piece.DIRECTION_RIGHT] = 77;
		original.dataOffsetY[Piece.DIRECTION_RIGHT] = 66;
		original.block[0].color = Block.BLOCK_COLOR_BLUE;

		assertEquals(Piece.PIECE_T, copy.id);
		assertEquals(Piece.DIRECTION_RIGHT, copy.direction);
		assertEquals(Block.BLOCK_COLOR_RED, copy.block[0].color);
		assertEquals(2, copy.dataOffsetX[Piece.DIRECTION_RIGHT]);
		assertEquals(6, copy.dataOffsetY[Piece.DIRECTION_RIGHT]);
		assertEquals(Piece.DEFAULT_PIECE_DATA_X[Piece.PIECE_T][Piece.DIRECTION_RIGHT][0] + 2,
				copy.dataX[Piece.DIRECTION_RIGHT][0]);
		assertEquals(Piece.DEFAULT_PIECE_DATA_Y[Piece.PIECE_T][Piece.DIRECTION_RIGHT][0] + 6,
				copy.dataY[Piece.DIRECTION_RIGHT][0]);
		assertNotSame(original.block[0], copy.block[0]);
	}
}
