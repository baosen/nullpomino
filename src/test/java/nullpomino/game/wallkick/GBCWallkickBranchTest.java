package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * Covers remaining branches in GBCWallkick:
 * - y2 >= 0 is false
 * - allowUpward is false when y2 < 0
 */
class GBCWallkickBranchTest {

	@Test
	void leftRotationWithY2NegativeAndAllowUpwardFalseReturnsNull() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		// KICKTABLE_L[0] = {1, -1}. y2 = -1 < 0, allowUpward = false.
		// The OR condition fails, so no kick is attempted.
		WallkickResult result = wallkick.executeWallkick(
				4, 4, -1, 0, 3, false, piece, field, null);

		assertNull(result);
	}

	@Test
	void rightRotationWithY2NegativeAndAllowUpwardTrueAndYCloseToTop() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		// KICKTABLE_R[0] = {-1, -1}. y2 = -1 < 0.
		// y = 0, so y + y2 = -1 > -2 → should pass guard
		// But the kick position might still collide...
		WallkickResult result = wallkick.executeWallkick(
				4, 0, 1, 0, 1, true, piece, field, null);

		assertNotNull(result);
	}

	@Test
	void leftRotationWithY2NegativeAndAllowUpwardTrueAndYPlusY2AtMinusTwo() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		// KICKTABLE_L[0] = {1, -1}. y2 = -1 < 0, allowUpward = true.
		// y = -1, so y + y2 = -2, which is NOT > -2 → guard fails.
		WallkickResult result = wallkick.executeWallkick(
				4, -1, -1, 0, 3, true, piece, field, null);

		assertNull(result);
	}
}
