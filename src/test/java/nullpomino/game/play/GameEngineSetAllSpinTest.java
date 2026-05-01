package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link GameEngine#setAllSpin} branches: the early-exit
 * matrix (null piece, kick-used while disallowed, big piece), the
 * 4-point spin-bonus detection on a flat field, the immobile-spin
 * skip path, and the EZ-spin branch.
 */
class GameEngineSetAllSpinTest {

	@Test
	void setAllSpinClearsAllFlagsBeforeChecking() {
		GameEngine engine = freshEngine();
		engine.tspin = true;
		engine.tspinmini = true;
		engine.tspinez = true;

		// Null piece returns early, but the three flag clears at the top
		// of setAllSpin run unconditionally.
		engine.setAllSpin(0, 0, null, engine.field);

		assertFalse(engine.tspin);
		assertFalse(engine.tspinmini);
		assertFalse(engine.tspinez);
	}

	@Test
	void setAllSpinSkipsBigPieces() {
		GameEngine engine = freshEngine();
		Piece big = new Piece(Piece.PIECE_T);
		big.big = true;

		engine.setAllSpin(5, 5, big, engine.field);

		// Even with the immobile/4-point rules in play, big pieces are
		// excluded from spin detection — pin the early-return.
		assertFalse(engine.tspin);
	}

	@Test
	void setAllSpinSkipsWhenKicksAreDisallowedAndKickWasUsed() {
		GameEngine engine = freshEngine();
		engine.tspinAllowKick = false;
		engine.kickused = true;
		Piece tPiece = new Piece(Piece.PIECE_T);

		engine.setAllSpin(5, 5, tPiece, engine.field);

		assertFalse(engine.tspin);
	}

	@Test
	void setAllSpinFourPointRuleDoesNotFireOnAFlatField() {
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_4POINT;
		engine.tspinAllowKick = true;
		engine.kickused = false;
		Piece tPiece = new Piece(Piece.PIECE_T);

		// A flat (empty) field has no high or low spots filled, so neither
		// of the spin patterns can match.
		engine.setAllSpin(5, 5, tPiece, engine.field);

		assertFalse(engine.tspin);
		assertFalse(engine.tspinmini);
	}

	@Test
	void setAllSpinImmobileRuleDoesNotFireWhenPieceCanMove() {
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		engine.tspinAllowKick = true;
		engine.kickused = false;
		Piece tPiece = new Piece(Piece.PIECE_T);

		// Mid-field placement on an empty field — every immobile probe
		// passes (no collision), so the immobile rule fails to fire.
		engine.setAllSpin(5, 5, tPiece, engine.field);

		assertFalse(engine.tspin);
	}

	@Test
	void setAllSpinImmobileRuleEZBranchFiresWhenKickUsedAndEZEnabled() {
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		engine.tspinAllowKick = true;
		engine.kickused = true;
		engine.tspinEnableEZ = true;
		Piece tPiece = new Piece(Piece.PIECE_T);

		engine.setAllSpin(5, 5, tPiece, engine.field);

		assertTrue(engine.tspin,
				"EZ branch sets tspin when kickused and tspinEnableEZ");
		assertTrue(engine.tspinez);
	}

	@Test
	void setAllSpinImmobileRuleEZBranchSkippedWhenKickFlagNotSet() {
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		engine.tspinAllowKick = true;
		engine.kickused = false;
		engine.tspinEnableEZ = true;
		Piece tPiece = new Piece(Piece.PIECE_T);

		engine.setAllSpin(5, 5, tPiece, engine.field);

		assertFalse(engine.tspin,
				"EZ branch needs kickused to fire — without it, immobile fails silently");
		assertFalse(engine.tspinez);
	}

	@Test
	void setAllSpinFourPointAndImmobileBothLeaveFlagsClearForOPiece() {
		// The O piece has empty SPINBONUSDATA arrays (no high/low spots),
		// so the 4-point loop does not iterate and tspin stays false.
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_4POINT;
		engine.tspinAllowKick = true;
		Piece oPiece = new Piece(Piece.PIECE_O);

		engine.setAllSpin(5, 5, oPiece, engine.field);

		assertFalse(engine.tspin);
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		gm.engine[0].createFieldIfNeeded();
		return gm.engine[0];
	}
}
