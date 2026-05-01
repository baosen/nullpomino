package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class GBCWallkickTest {

	@Test
	void iPiecesAreNeverKicked() {
		GBCWallkick wallkick = new GBCWallkick();
		Field field = new Field();

		assertNull(wallkick.executeWallkick(4, 4, 1, 0, 1, true,
				new Piece(Piece.PIECE_I), field, null));
		assertNull(wallkick.executeWallkick(4, 4, -1, 0, 3, true,
				new Piece(Piece.PIECE_I2), field, null));
		assertNull(wallkick.executeWallkick(4, 4, 1, 0, 1, true,
				new Piece(Piece.PIECE_I3), field, null));
	}

	@Test
	void rightRotationDispatchesIntoRightKickTable() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true, piece, field, null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
		assertEquals(-1, result.offsetY);
	}

	@Test
	void leftRotationDispatchesIntoLeftKickTable() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, -1, 0, 3, true, piece, field, null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
		assertEquals(-1, result.offsetY);
	}

	@Test
	void kickAtTopOfFieldIsRejectedByYPlusY2GuardrailAtMinusTwo() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, -1, 1, 0, 1, true, piece, field, null);

		assertNull(result);
	}

	@Test
	void bigPieceDoublesKickOffsets() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true, piece, field, null);

		assertNotNull(result);
		assertEquals(-2, result.offsetX);
		assertEquals(-2, result.offsetY);
	}

	@Test
	void collisionAtKickedPositionReturnsNull() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();
		// KICKTABLE_R[0] = (-1,-1). Piece-T at (3, 3, dir=1) covers
		// (5,3),(4,3),(4,4),(4,5). Plant blocks across that zone.
		for (int dx = 0; dx < 6; dx++) {
			for (int dy = 0; dy < 6; dy++) {
				field.setBlock(dx, dy, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true, piece, field, null);

		assertNull(result);
	}
}
