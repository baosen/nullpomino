package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class ClassicPlusWallkickTest {

	@Test
	void normalPieceFarFromWallsReturnsNull() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 0, true,
				new Piece(Piece.PIECE_O), new Field(), null);

		assertNull(result);
	}

	@Test
	void i2PieceShortCircuitsToShiftRight() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 0, true,
				new Piece(Piece.PIECE_I2), new Field(), null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
	}

	@Test
	void l3PieceShortCircuitsToShiftRight() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 0, true,
				new Piece(Piece.PIECE_L3), new Field(), null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
	}

	@Test
	void normalPieceAtRightWallShiftsLeft() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				8, 18, 1, 0, 0, true,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
	}

	@Test
	void tPieceEscapesUpwardWhenAllowed() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_UP, true,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNotNull(result);
		assertEquals(0, result.offsetX);
		assertEquals(-1, result.offsetY);
	}

	@Test
	void tPieceEscapeBlockedByCeilingFallsThroughToNull() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field(10, 20, 3, true);

		WallkickResult result = wallkick.executeWallkick(
				4, -2, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);

		assertNull(result);
	}

	@Test
	void iPieceUpRotationLeftKickFirstBranch() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_UP, true,
				new Piece(Piece.PIECE_I), new Field(), null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
	}

	@Test
	void iPieceUpRotationFallsToRightKickSecondBranch() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Field field = new Field();
		field.setBlock(3, 5, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_UP, true,
				new Piece(Piece.PIECE_I), field, null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
	}

	@Test
	void iPieceUpRotationFallsToPlusTwoKickThirdBranch() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Field field = new Field();
		field.setBlock(3, 5, new Block(Block.BLOCK_COLOR_GRAY));
		field.setBlock(5, 5, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_UP, true,
				new Piece(Piece.PIECE_I), field, null);

		assertNotNull(result);
		assertEquals(2, result.offsetX);
	}

	@Test
	void iPieceDownRotationAlsoUsesIWallkickBlock() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_DOWN, true,
				new Piece(Piece.PIECE_I), new Field(), null);

		assertNotNull(result);
	}

	@Test
	void iPieceUpRotationAllBranchesBlockedReturnsNull() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Field field = new Field();
		for(int dx = 0; dx < field.getWidth(); dx++) {
			field.setBlock(dx, 5, new Block(Block.BLOCK_COLOR_GRAY));
		}

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_UP, true,
				new Piece(Piece.PIECE_I), field, null);

		assertNull(result);
	}

	@Test
	void iPieceLeftRotationOnFloorLiftsByOne() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I);
		// Direction UP (default) with cells y=18 -> floor check at y+1=19 collides
		// because dataY[UP] = {1,1,1,1}, so checkCollision(x, 19, UP) puts cells
		// at y=20 which is >= height.
		WallkickResult result = wallkick.executeWallkick(
				4, 18, 1, Piece.DIRECTION_UP, Piece.DIRECTION_LEFT, true,
				piece, new Field(), null);

		assertNotNull(result);
		assertEquals(0, result.offsetX);
	}

	@Test
	void iPieceLeftRotationLiftsByOneWhenSpaceAboveIsClear() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I);
		// Set piece.direction = LEFT so the floor check (which uses
		// piece.direction) also takes the LEFT-rotated cells. At y=16 the
		// LEFT rotation reaches row 20 (floor contact), but the cells one
		// row up at y=15 stay in bounds — exercises the first I-floor-kick
		// branch (`temp = -1 - i`).
		piece.direction = Piece.DIRECTION_LEFT;

		WallkickResult result = wallkick.executeWallkick(
				4, 16, 1, Piece.DIRECTION_UP, Piece.DIRECTION_LEFT, true,
				piece, new Field(), null);

		assertNotNull(result);
		assertEquals(0, result.offsetX);
		assertEquals(-1, result.offsetY);
	}

	@Test
	void iPieceLeftRotationNotOnFloorSkipsBlock() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, Piece.DIRECTION_UP, Piece.DIRECTION_LEFT, true,
				new Piece(Piece.PIECE_I), new Field(), null);

		assertNull(result);
	}

	@Test
	void iPieceRightRotationOnFloorLiftsByOne() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();

		WallkickResult result = wallkick.executeWallkick(
				4, 18, 1, Piece.DIRECTION_UP, Piece.DIRECTION_RIGHT, true,
				new Piece(Piece.PIECE_I), new Field(), null);

		assertNotNull(result);
	}

	@Test
	void normalPieceWithLeftWallTouchShiftsRight() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				-1, 5, 1, 0, 0, true,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
	}

	@Test
	void floorContactReturnsNullWhenBothShiftsCollide() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Field field = new Field();
		// rtNew = DOWN avoids the T-escape and I-floor branches; PIECE_T at
		// floor exposes its right block to the floor sentinel and both
		// shifts collide.
		WallkickResult result = wallkick.executeWallkick(
				5, 19, 1, 0, Piece.DIRECTION_DOWN, true,
				new Piece(Piece.PIECE_T), field, null);

		assertNull(result);
	}

	@Test
	void occupiedCellTriggersKickEligible() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Field field = new Field();
		field.setBlock(7, 6, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, 0, true,
				new Piece(Piece.PIECE_T), field, null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
	}

	@Test
	void bigI3WidthBranchTriggersAndShiftsLeftWithDoubledOffset() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				15, 5, 1, 0, 0, true, piece, field, null);

		assertNotNull(result);
		assertEquals(-2, result.offsetX);
	}

	@Test
	void bigI3FloorBranchAcknowledgesFloorContact() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				6, 22, 1, 0, 0, true, piece, field, null);

		assertNull(result);
	}

	@Test
	void bigI3WallBranchTriggersOnNegativeXAndShiftsRight() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				-2, 5, 1, 0, 0, true, piece, field, null);

		assertNotNull(result);
		assertEquals(2, result.offsetX);
	}

	@Test
	void bigI3OccupiedBranchTriggersKickEligible() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);
		field.setBlock(8, 6, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 0, true, piece, field, null);

		assertNotNull(result);
	}

	@Test
	void bigI3FarFromObstaclesReturnsNull() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(30, 30, 5);

		WallkickResult result = wallkick.executeWallkick(
				4, 5, 1, 0, 0, true, piece, field, null);

		assertNull(result);
	}

	@Test
	void bigIPieceUpRotationKickSucceeds() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);

		assertNotNull(result);
	}

	@Test
	void tPieceUpRotationWithUpwardDisallowedSkipsEscape() {
		// piece.id==T is true but allowUpward is false, exercising the
		// short-circuit false outcome of the T-escape guard at line 37.
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_UP, false,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNull(result);
	}

	@Test
	void iPieceSideRotationWithUpwardDisallowedSkipsFloorKick() {
		// piece.id==I is true but allowUpward is false, exercising the
		// short-circuit false outcome of the I-floor-kick guard at line 63.
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, Piece.DIRECTION_UP, Piece.DIRECTION_LEFT, false,
				new Piece(Piece.PIECE_I), new Field(), null);

		assertNull(result);
	}

	@Test
	void bigIPieceLeftRotationOnFloorLifts() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_I);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				4, 21, 1, Piece.DIRECTION_UP, Piece.DIRECTION_LEFT, true,
				piece, field, null);

		assertNotNull(result);
	}
}
