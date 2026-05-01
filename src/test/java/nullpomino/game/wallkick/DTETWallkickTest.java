package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class DTETWallkickTest {

	@Test
	void rightRotationFlipsXOffsetSign() {
		DTETWallkick wallkick = new DTETWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true, piece, field, null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
		assertEquals(0, result.offsetY);
	}

	@Test
	void leftRotationKeepsXOffsetSignFromTable() {
		DTETWallkick wallkick = new DTETWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, -1, 0, 3, true, piece, field, null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
	}

	@Test
	void halfTurnTakesSameSignAsLeftRotation() {
		DTETWallkick wallkick = new DTETWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 2, 0, 2, true, piece, field, null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
	}

	@Test
	void bigPieceDoublesOffsets() {
		DTETWallkick wallkick = new DTETWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true, piece, field, null);

		assertNotNull(result);
		assertEquals(2, result.offsetX);
	}

	@Test
	void allKicksBlockedReturnsNull() {
		DTETWallkick wallkick = new DTETWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();
		for(int dx = 0; dx < field.getWidth(); dx++) {
			for(int dy = 0; dy < field.getHeight(); dy++) {
				field.setBlock(dx, dy, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true, piece, field, null);

		assertNull(result);
	}
}
