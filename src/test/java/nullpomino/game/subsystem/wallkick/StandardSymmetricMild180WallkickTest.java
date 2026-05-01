package nullpomino.game.subsystem.wallkick;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class StandardSymmetricMild180WallkickTest {

	@Test
	void leftRotationDispatchesAcrossEveryPieceFamily() {
		assertKick(Piece.PIECE_T, -1);
		assertKick(Piece.PIECE_I, -1);
		assertKick(Piece.PIECE_I2, -1);
		assertKick(Piece.PIECE_I3, -1);
		assertKick(Piece.PIECE_L3, -1);
	}

	@Test
	void rightRotationDispatchesAcrossEveryPieceFamily() {
		assertKick(Piece.PIECE_T, 1);
		assertKick(Piece.PIECE_I, 1);
		assertKick(Piece.PIECE_I2, 1);
		assertKick(Piece.PIECE_I3, 1);
		assertKick(Piece.PIECE_L3, 1);
	}

	@Test
	void halfTurnDispatchesNormalAndIPieceTables() {
		assertKick(Piece.PIECE_T, 2);
		assertKick(Piece.PIECE_I, 2);
	}

	@Test
	void zeroRotationFallsThroughToNull() {
		StandardSymmetricMild180Wallkick wallkick = new StandardSymmetricMild180Wallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 0, 0, 1, true,
				new Piece(Piece.PIECE_T), new Field(), null);

		assertNull(result);
	}

	private static void assertKick(int pieceId, int rtDir) {
		StandardSymmetricMild180Wallkick wallkick = new StandardSymmetricMild180Wallkick();
		Piece piece = new Piece(pieceId);
		Field field = new Field();
		int rtNew = (rtDir == 2) ? 2 : (rtDir == -1 ? 3 : 1);

		WallkickResult result = wallkick.executeWallkick(
				4, 4, rtDir, 0, rtNew, true, piece, field, null);

		assertNotNull(result, "expected kick for piece=" + pieceId + " dir=" + rtDir);
	}
}
