package nullpomino.game.subsystem.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class WallOnlyWallkickTest {

	@Test
	void iPieceShortCircuitsToNull() {
		WallOnlyWallkick wallkick = new WallOnlyWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true,
				new Piece(Piece.PIECE_I), new Field(), null);

		assertNull(result);
	}

	@Test
	void normalPieceFarFromWallsReturnsNull() {
		WallOnlyWallkick wallkick = new WallOnlyWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 0, true,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNull(result);
	}

	@Test
	void normalPieceAtRightWallShiftsLeft() {
		WallOnlyWallkick wallkick = new WallOnlyWallkick();
		WallkickResult result = wallkick.executeWallkick(
				8, 18, 1, 0, 0, true,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNotNull(result);
		assertEquals(-1, result.offsetX);
		assertEquals(0, result.offsetY);
	}

	@Test
	void normalPieceAtLeftWallShiftsRight() {
		WallOnlyWallkick wallkick = new WallOnlyWallkick();
		WallkickResult result = wallkick.executeWallkick(
				-1, 5, 1, 0, 0, true,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNotNull(result);
		assertEquals(1, result.offsetX);
	}

	@Test
	void normalPieceWedgedBetweenWallsReturnsNull() {
		WallOnlyWallkick wallkick = new WallOnlyWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field narrow = new Field(2, 20, 3);

		WallkickResult result = wallkick.executeWallkick(
				0, 5, 1, 0, 0, true, piece, narrow, null);

		assertNull(result);
	}

	@Test
	void bigI3FarFromWallsReturnsNullCoveringCheckCollisionKickBigFallthrough() {
		WallOnlyWallkick wallkick = new WallOnlyWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(30, 30, 5);

		WallkickResult result = wallkick.executeWallkick(
				4, 5, 1, 0, 0, true, piece, field, null);

		assertNull(result);
	}

	@Test
	void bigI3AtRightWallShiftsLeftWithDoubledOffset() {
		WallOnlyWallkick wallkick = new WallOnlyWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				15, 5, 1, 0, 0, true, piece, field, null);

		assertNotNull(result);
		assertEquals(-2, result.offsetX);
	}

	@Test
	void bigI3AtLeftWallShiftsRightWithDoubledOffset() {
		WallOnlyWallkick wallkick = new WallOnlyWallkick();
		Piece piece = new Piece(Piece.PIECE_I3);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				-2, 5, 1, 0, 0, true, piece, field, null);

		assertNotNull(result);
		assertEquals(2, result.offsetX);
	}
}
