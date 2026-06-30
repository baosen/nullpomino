package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class AvalancheClassicWallkickTest {

	@Test
	void noCollisionAtStartReturnsNull() {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);

		assertNull(result);
	}

	@Test
	void upwardRotationCollisionShiftsLeft() {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(6, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
		assertEquals(0, result.offsetY);
	}

	@Test
	void rightwardRotationCollisionShiftsUp() {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(6, 6);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, Piece.DIRECTION_RIGHT, true, piece, field, null);

		assertNotNull(result);
		assertEquals(0, result.offsetX);
		assertEquals(-1, result.offsetY);
	}

	@Test
	void downwardRotationCollisionShiftsRight() {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(5, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, Piece.DIRECTION_DOWN, true, piece, field, null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
		assertEquals(0, result.offsetY);
	}

	@Test
	void leftwardRotationCollisionShiftsDown() {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(5, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, Piece.DIRECTION_LEFT, true, piece, field, null);

		assertNotNull(result);
		assertEquals(0, result.offsetX);
		assertEquals(1, result.offsetY);
	}

	@Test
	void collisionWithoutClearanceFallsThroughToNull() {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = new Field();
		// Surround the start cell so neither shift arm finds clearance.
		for(int dx = 4; dx <= 7; dx++) {
			for(int dy = 4; dy <= 7; dy++) {
				field.setBlock(dx, dy, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);

		assertNull(result);
	}

	@Test
	void rightwardRotationWithoutClearanceFallsThroughToNull() {
		// Collides at the start cell AND after the upward shift, so the RIGHT
		// arm's clearance check is false and the switch breaks to null.
		assertNull(surroundedKick(Piece.DIRECTION_RIGHT));
	}

	@Test
	void downwardRotationWithoutClearanceFallsThroughToNull() {
		assertNull(surroundedKick(Piece.DIRECTION_DOWN));
	}

	@Test
	void leftwardRotationWithoutClearanceFallsThroughToNull() {
		assertNull(surroundedKick(Piece.DIRECTION_LEFT));
	}

	private static WallkickResult surroundedKick(int rtNew) {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = new Field();
		for(int dx = 4; dx <= 7; dx++) {
			for(int dy = 4; dy <= 7; dy++) {
				field.setBlock(dx, dy, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}
		return wallkick.executeWallkick(5, 5, 1, 0, rtNew, true, piece, field, null);
	}

	private static Field fieldWithBlockAt(int x, int y) {
		Field field = new Field();
		field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
		return field;
	}
}
