package nullpomino.game.wallkick;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.WallkickResult;

/**
 * Covers remaining branches in wallkick implementations.
 * Tests target specific branch conditions in each wallkick.
 */
class WallkickBranchTest {

	// =============== AvalancheClassicWallkick ===============

	@Test
	void avalancheClassicWallkickBigPieceNoCollision() {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		piece.big = true;
		Field field = playField();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);
		assertNull(result);
	}

	@Test
	void avalancheClassicWallkickBlockedInAllDirections() {
		AvalancheClassicWallkick wallkick = new AvalancheClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = playField();
		// Place blocks to block all kicks
		for (int dx = 3; dx <= 7; dx++) {
			for (int dy = 3; dy <= 7; dy++) {
				field.setBlock(dx, dy, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);
		assertNull(result);
	}

	// =============== PhysicianWallkick ===============

	@Test
	void physicianWallkickBigPiece() {
		PhysicianWallkick wallkick = new PhysicianWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		piece.big = true;
		Field field = playField();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);
		assertNull(result);
	}

	@Test
	void physicianWallkickFullyBlocked() {
		PhysicianWallkick wallkick = new PhysicianWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = playField();
		field.setBlock(4, 5, new Block(Block.BLOCK_COLOR_GRAY));
		field.setBlock(7, 5, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);
		assertNull(result);
	}

	// =============== AvalancheWallkick ===============

	@Test
	void avalancheWallkickBigPiece() {
		AvalancheWallkick wallkick = new AvalancheWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		piece.big = true;
		Field field = playField();

		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);
		assertNull(result);
	}

	@Test
	void avalancheWallkickUpDirectionBlocked() {
		AvalancheWallkick wallkick = new AvalancheWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		Field field = playField();
		field.setBlock(6, 5, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);
		assertNotNull(result);
	}

	// =============== BaseStandardWallkick ===============

	@Test
	void baseStandardWallkickAllowUpwardFalse() {
		BaseStandardWallkick wallkick = new BaseStandardWallkick();
		WallkickResult result = wallkick.executeWallkick(
				4, 4, 1, 0, 1, false,
				new Piece(Piece.PIECE_T), new Field(), null);
		assertNull(result);
	}

	// =============== ClassicPlusWallkick ===============

	@Test
	void classicPlusWallkickBigPieceWithCollision() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		piece.big = true;
		Field field = playField();
		field.setBlock(4, 5, new Block(Block.BLOCK_COLOR_GRAY));
		field.setBlock(4, 6, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);
		assertNull(result);
	}

	@Test
	void classicPlusWallkickTUpwardKickBlocked() {
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = playField();
		field.setBlock(4, 5, new Block(Block.BLOCK_COLOR_GRAY));
		field.setBlock(5, 4, new Block(Block.BLOCK_COLOR_GRAY));

		WallkickResult result = wallkick.executeWallkick(
				5, 4, 1, Piece.DIRECTION_LEFT, Piece.DIRECTION_UP, true,
				piece, field, null);
		assertNull(result);
	}

	// =============== GBCWallkick ===============

	@Test
	void gbcWallkickWithAllowUpwardFalseAndUpwardKickNeeded() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		// KICKTABLE_L[0] = {1, -1}. y2 = -1 < 0, allowUpward = false.
		WallkickResult result = wallkick.executeWallkick(
				4, 4, -1, 0, 3, false, piece, field, null);
		assertNull(result);
	}

	@Test
	void gbcWallkickWithAllowUpwardTrueAndGuardrailOk() {
		GBCWallkick wallkick = new GBCWallkick();
		Piece piece = new Piece(Piece.PIECE_T);
		Field field = new Field();

		// KICKTABLE_R[0] = {-1, -1}. y2 = -1 < 0, allowUpward = true.
		// y = 0, so y + y2 = -1 > -2 → should pass guard
		WallkickResult result = wallkick.executeWallkick(
				4, 0, 1, 0, 1, true, piece, field, null);
		assertNotNull(result);
	}

	// =============== ClassicWallkick CheckCollisionKickBig ===============

	@Test
	void classicWallkickCollisionKickNonIBigPiece() {
		// Test the checkCollisionKickBig path with piece.id != PIECE_I
		ClassicWallkick wallkick = new ClassicWallkick();
		Piece piece = new Piece(Piece.PIECE_L3); // Not PIECE_I but triggers big collision
		piece.big = true;
		Field field = playField();
		// Fill the field to make all kicks fail
		for (int x = 0; x < 20; x++) {
			for (int y = 0; y < 25; y++) {
				field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);
		assertNull(result);
	}

	@Test
	void classicPlusWallkickCheckCollisionKickBig() {
		// Test the checkCollisionKickBig path in ClassicPlusWallkick
		ClassicPlusWallkick wallkick = new ClassicPlusWallkick();
		Piece piece = new Piece(Piece.PIECE_O);
		piece.big = true;
		Field field = playField();
		for (int x = 0; x < 20; x++) {
			for (int y = 0; y < 25; y++) {
				field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}

		WallkickResult result = wallkick.executeWallkick(
				5, 5, 1, 0, Piece.DIRECTION_UP, true, piece, field, null);
		assertNull(result);
	}

	// =============== Helper ===============

	private static Field playField() {
		return new Field(20, 25, 5);
	}
}
