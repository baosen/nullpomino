package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

class StandardWallkickTest {

	@Test
	void leftRotationOfNormalTetrominoUsesNormalLeftKickTable() {
		assertKickReturnsResult(Piece.PIECE_T, -1);
	}

	@Test
	void rightRotationOfNormalTetrominoUsesNormalRightKickTable() {
		assertKickReturnsResult(Piece.PIECE_T, 1);
	}

	@Test
	void leftAndRightRotationOfIPieceUsesITable() {
		assertKickReturnsResult(Piece.PIECE_I, -1);
		assertKickReturnsResult(Piece.PIECE_I, 1);
	}

	@Test
	void leftAndRightRotationOfI2PieceUsesI2Table() {
		assertKickReturnsResult(Piece.PIECE_I2, -1);
		assertKickReturnsResult(Piece.PIECE_I2, 1);
	}

	@Test
	void leftAndRightRotationOfI3PieceUsesI3Table() {
		assertKickReturnsResult(Piece.PIECE_I3, -1);
		assertKickReturnsResult(Piece.PIECE_I3, 1);
	}

	@Test
	void leftAndRightRotationOfL3PieceUsesL3Table() {
		assertKickReturnsResult(Piece.PIECE_L3, -1);
		assertKickReturnsResult(Piece.PIECE_L3, 1);
	}

	@Test
	void halfTurnOfNormalTetrominoUsesNormal180Table() {
		assertKickReturnsResult(Piece.PIECE_T, 2);
	}

	@Test
	void halfTurnOfIPieceUsesI180Table() {
		assertKickReturnsResult(Piece.PIECE_I, 2);
	}

	@Test
	void zeroRotationDirectionFallsThroughToNullKickTable() {
		StandardWallkick wallkick = new StandardWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, /* rtDir */ 0, 0, 1, true, piece, field, null);

		assertNull(result);
	}

	private static void assertKickReturnsResult(int pieceId, int rtDir) {
		StandardWallkick wallkick = new StandardWallkick();
		Piece piece = new Piece(pieceId);
		Field field = new Field();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, rtDir, 0, rotationAfter(rtDir), true, piece, field, null);

		assertNotNull(result, "expected kick result for piece " + pieceId + " dir " + rtDir);
	}

	private static int rotationAfter(int rtDir) {
		if(rtDir == 2) return 2;
		if(rtDir == -1) return 3;
		if(rtDir == 1) return 1;
		return 0;
	}
}
