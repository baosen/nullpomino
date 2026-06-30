package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class PhysicianWallkickTest {

	@Test
	void noCollisionAtStartReturnsNull() {
		PhysicianWallkick wallkick = new PhysicianWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);

		assertNull(result);
	}

	@Test
	void upwardCollisionShiftsLeftByOne() {
		PhysicianWallkick wallkick = new PhysicianWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(6, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
		assertEquals(0, result.offsetY);
	}

	@Test
	void downwardCollisionAlsoShiftsLeftByOne() {
		PhysicianWallkick wallkick = new PhysicianWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(6, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_UP, Piece.DIRECTION_DOWN, true,
				piece, field, null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
	}

	@Test
	void leftCollisionShiftsRightByOne() {
		PhysicianWallkick wallkick = new PhysicianWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(5, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_UP, Piece.DIRECTION_LEFT, true,
				piece, field, null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
	}

	@Test
	void rightCollisionAlsoShiftsRightByOne() {
		PhysicianWallkick wallkick = new PhysicianWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(5, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_UP, Piece.DIRECTION_RIGHT, true,
				piece, field, null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
	}

	@Test
	void collisionWithoutClearanceFallsThroughToNull() {
		PhysicianWallkick wallkick = new PhysicianWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		// Wall the piece in on every adjacent column.
		Field field = new Field();
		field.setBlock(5, 5, new Block(Block.BLOCK_COLOR_GRAY));
		field.setBlock(4, 5, new Block(Block.BLOCK_COLOR_GRAY));
		field.setBlock(7, 5, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);

		assertNull(result);
	}

	@Test
	void rightRotationWithoutClearanceFallsThroughToNull() {
		// rtNew==RIGHT reaches the LEFT/RIGHT switch arm; both the start cell and
		// the x+check shift collide, so the L32 clearance check is false -> null.
		PhysicianWallkick wallkick = new PhysicianWallkick();
		Field field = new Field();
		for(int dx = 4; dx <= 8; dx++) {
			for(int dy = 4; dy <= 7; dy++) {
				field.setBlock(dx, dy, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_UP, Piece.DIRECTION_RIGHT, true,
				new Piece(Piece.PIECE_O), field, null);

		assertNull(result);
	}

	private static Field fieldWithBlockAt(int x, int y) {
		Field field = new Field();
		field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
		return field;
	}
}
