package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class ClassicWallkickTest {

	@Test
	void iPieceShortCircuitsToNull() {
		ClassicWallkick wallkick = new ClassicWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true,
				new Piece(Piece.PIECE_I), new Field(), null);

		assertNull(result);
	}

	@Test
	void normalPieceFarFromWallsReturnsNull() {
		ClassicWallkick wallkick = new ClassicWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 0, true,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNull(result);
	}

	@Test
	void i2PieceAlwaysAttemptsKickEvenWithoutCollision() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_I2);
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 0, true, piece, new Field(), null);

		assertNotNull(result);
	}

	@Test
	void l3PieceAlsoTakesShortCircuitKickPath() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_L3);
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 0, true, piece, new Field(), null);

		assertNotNull(result);
	}

	@Test
	void normalPieceAtRightWallShiftsLeft() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		WallkickResult result = wallkick.executeWallkick(
				8, 18, 1, 0, 0, true, piece, new Field(), null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
		assertEquals(0, result.offsetY);
	}

	@Test
	void normalPieceWithLeftWallTouchShiftsRight() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		WallkickResult result = wallkick.executeWallkick(
				-1, 5, 1, 0, 0, true, piece, new Field(), null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
	}

	@Test
	void floorContactSetsKickEligibleAndReturnsNullWhenBothShiftsCollide() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		WallkickResult result = wallkick.executeWallkick(
				5, 19, 1, 0, 0, true, piece, new Field(), null);

		assertNull(result);
	}

	@Test
	void occupiedCellTriggersKickEligible() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();
		field.setBlock(7, 6, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, 0, true, piece, field, null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
	}

	@Test
	void bigI3WidthBranchTriggersAndShiftsLeftWithDoubledOffset() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				15, 5, 1, 0, 0, true, piece, field, null);

		assertNotNull(result);
		assertEquals(-2, result.offsetX);
	}

	@Test
	void bigI3HeightBranchAcknowledgesFloorContact() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				6, 22, 1, 0, 0, true, piece, field, null);

		assertNull(result);
	}

	@Test
	void bigI3WallBranchTriggersOnNegativeXAndShiftsRight() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				-2, 5, 1, 0, 0, true, piece, field, null);

		assertNotNull(result);
		assertEquals(2, result.offsetX);
	}

	@Test
	void bigI3FarFromObstaclesReturnsNull() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(30, 30, 5);

		WallkickResult result = wallkick.executeWallkick(
				4, 5, 1, 0, 0, true, piece, field, null);

		assertNull(result);
	}

	@Test
	void bigI3OccupiedCellBranchTriggersKickEligible() {
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);
		field.setBlock(8, 6, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 0, true, piece, field, null);

		assertNotNull(result);
	}
}
