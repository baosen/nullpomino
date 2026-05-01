package nullpomino.game.subsystem.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class AvalancheWallkickTest {

	@Test
	void leftRotationTargetReturnsNullWithoutAttemptingKick() {
		AvalancheWallkick wallkick = new AvalancheWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(5, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_DOWN, Piece.DIRECTION_LEFT, true,
				piece, field, null);

		assertNull(result);
	}

	@Test
	void noCollisionAtStartReturnsNull() {
		AvalancheWallkick wallkick = new AvalancheWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);

		assertNull(result);
	}

	@Test
	void collisionThatClearsByMovingUpReturnsUpwardKick() {
		AvalancheWallkick wallkick = new AvalancheWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(6, 6);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);

		assertNotNull(result);
		assertEquals(0, result.offsetX);
		assertEquals(-1, result.offsetY);
		assertEquals(Piece.DIRECTION_UP, result.direction);
	}

	@Test
	void upwardCollisionThatClearsByMovingLeftReturnsLeftKick() {
		AvalancheWallkick wallkick = new AvalancheWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(6, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
		assertEquals(0, result.offsetY);
		assertEquals(Piece.DIRECTION_UP, result.direction);
	}

	@Test
	void downwardCollisionThatClearsByMovingRightReturnsRightKick() {
		AvalancheWallkick wallkick = new AvalancheWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(5, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_DOWN, true,
				piece, field, null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
		assertEquals(0, result.offsetY);
		assertEquals(Piece.DIRECTION_DOWN, result.direction);
	}

	@Test
	void rightRotationFallsThroughToNullWhenAllProbesCollide() {
		AvalancheWallkick wallkick = new AvalancheWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = fieldWithBlockAt(5, 5);

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_UP, Piece.DIRECTION_RIGHT, true,
				piece, field, null);

		assertNull(result);
	}

	private static Field fieldWithBlockAt(int x, int y) {
		Field field = new Field();
		field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
		return field;
	}
}
