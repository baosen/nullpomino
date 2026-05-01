package nullpomino.game.subsystem.wallkick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class BaseStandardWallkickTest {

	@Test
	void unspecialisedBaseClassReturnsNullKickTable() {
		BaseStandardWallkick wallkick = new BaseStandardWallkick();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNull(result);
	}

	@Test
	void bigPieceDoublesKickOffsetsThroughBaseLoop() {
		StandardWallkick wallkick = new StandardWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		piece.big = true;
		Field field = new Field(20, 25, 5);

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, true, piece, field, null);

		assertNotNull(result);
		// WALLKICK_NORMAL_R[0][0] = (-1, 0) → doubled to (-2, 0).
		assertEquals(-2, result.offsetX);
		assertEquals(0, result.offsetY);
	}
}
