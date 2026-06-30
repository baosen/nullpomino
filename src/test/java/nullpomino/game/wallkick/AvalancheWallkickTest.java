package nullpomino.game.wallkick;

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

	@Test
	void upRotationWithLeftAlsoBlockedFallsThroughToNull() {
		// rtNew==UP, up shift blocked (L23 false) AND left shift blocked, so the
		// L25 clearance check is false and the DOWN guard at L27 is also false.
		assertNull(surroundedKick(Piece.DIRECTION_UP));
	}

	@Test
	void downRotationWithRightAlsoBlockedFallsThroughToNull() {
		// rtNew==DOWN, up shift blocked, right shift blocked -> L27 clearance false.
		assertNull(surroundedKick(Piece.DIRECTION_DOWN));
	}

	private static WallkickResult surroundedKick(int rtNew) {
		AvalancheWallkick wallkick = new AvalancheWallkick();
		Field field = new Field();
		for(int dx = 4; dx <= 7; dx++) {
			for(int dy = 4; dy <= 7; dy++) {
				field.setBlock(dx, dy, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}
		return wallkick.executeWallkick(5, 5, 1, Piece.DIRECTION_LEFT, rtNew, true,
				new Piece(Piece.PIECE_O), field, null);
	}

	private static Field fieldWithBlockAt(int x, int y) {
		Field field = new Field();
		field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
		return field;
	}
}
