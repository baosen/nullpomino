package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link GameEngine#setTSpin} branches: null/non-T piece
 * skip, the !tspinAllowKick && kickused short-circuit, the 4-point
 * three-corners-or-more rule, and the immobile-spin fallback with
 * its EZ-spin branch.
 */
class GameEngineSetTSpinTest {

	@Test
	void setTSpinClearsFlagWhenPieceIsNull() {
		GameEngine engine = freshEngine();
		engine.tspin = true;

		engine.setTSpin(0, 0, null, engine.field);

		assertFalse(engine.tspin);
	}

	@Test
	void setTSpinClearsFlagWhenPieceIsNotT() {
		GameEngine engine = freshEngine();
		engine.tspin = true;
		Piece notT = new Piece(Piece.PIECE_O);

		engine.setTSpin(0, 0, notT, engine.field);

		assertFalse(engine.tspin);
	}

	@Test
	void setTSpinClearsFlagWhenKickWasUsedAndKicksAreDisallowed() {
		GameEngine engine = freshEngine();
		engine.tspin = true;
		engine.tspinAllowKick = false;
		engine.kickused = true;
		Piece tPiece = new Piece(Piece.PIECE_T);

		engine.setTSpin(0, 0, tPiece, engine.field);

		assertFalse(engine.tspin);
	}

	@Test
	void setTSpinFourPointRuleSetsTSpinWhenThreeOrMoreCornersAreFilled() {
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_4POINT;
		// WALLKICKFLAG mini-type avoids the ROTATECHECK branch that needs
		// nowPieceObject (null in this minimal setup).
		engine.tspinminiType = GameEngine.TSPINMINI_TYPE_WALLKICKFLAG;
		engine.tspinAllowKick = true;
		engine.kickused = false;
		Piece tPiece = new Piece(Piece.PIECE_T);

		Field fld = engine.field;
		// Plant three filled corners around piece origin (0, 0). The 4-point
		// corners (without the rule offset) are (0,0), (2,0), (0,2), (2,2).
		fld.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(2, 0, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(0, 2, Block.BLOCK_COLOR_RED);
		// Leave (2,2) empty — three of four corners filled, still a T-spin.
		fld.setBlockColor(2, 2, Block.BLOCK_COLOR_NONE);

		engine.setTSpin(0, 0, tPiece, fld);

		assertTrue(engine.tspin,
				"three of four corners filled satisfies the 4-point rule");
	}

	@Test
	void setTSpinFourPointRuleLeavesFlagWhenFewerThanThreeCornersAreFilled() {
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_4POINT;
		// WALLKICKFLAG mini-type avoids the ROTATECHECK branch that
		// would dereference nowPieceObject (null in this minimal setup).
		engine.tspinminiType = GameEngine.TSPINMINI_TYPE_WALLKICKFLAG;
		engine.tspinAllowKick = true;
		engine.kickused = false;
		engine.tspin = false;
		Piece tPiece = new Piece(Piece.PIECE_T);

		Field fld = engine.field;
		// Only two corners filled — below the 3-corner threshold.
		fld.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(2, 0, Block.BLOCK_COLOR_RED);

		engine.setTSpin(0, 0, tPiece, fld);

		assertFalse(engine.tspin);
	}

	@Test
	void setTSpinFourPointRuleWithWallkickFlagSetsTSpinMiniToKickUsed() {
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_4POINT;
		engine.tspinminiType = GameEngine.TSPINMINI_TYPE_WALLKICKFLAG;
		engine.tspinAllowKick = true;
		engine.kickused = true;
		Piece tPiece = new Piece(Piece.PIECE_T);

		// Mini logic runs regardless of corner count. Just verify the flag
		// reflects kickused.
		engine.setTSpin(0, 0, tPiece, engine.field);

		assertTrue(engine.tspinmini,
				"WALLKICKFLAG mini-detect sets tspinmini to kickused");
	}

	@Test
	void setTSpinImmobileRuleDoesNotSetTSpinWhenPieceCanStillMove() {
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		engine.tspinAllowKick = true;
		engine.kickused = false;
		engine.tspin = false;
		Piece tPiece = new Piece(Piece.PIECE_T);
		// Plant the piece in mid-field with no walls or blocks around — it
		// can move in every direction, so the immobile rule does not fire.
		int x = 5;
		int y = 5;

		engine.setTSpin(x, y, tPiece, engine.field);

		assertFalse(engine.tspin);
	}

	@Test
	void setTSpinImmobileRuleEZBranchFiresWhenKickUsedAndEZEnabled() {
		GameEngine engine = freshEngine();
		engine.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		engine.tspinAllowKick = true;
		engine.kickused = true;
		engine.tspinEnableEZ = true;
		engine.tspin = false;
		engine.tspinez = false;
		Piece tPiece = new Piece(Piece.PIECE_T);

		// Plant the piece in mid-field so the immobile checks fail and the
		// EZ branch fires instead.
		engine.setTSpin(5, 5, tPiece, engine.field);

		assertTrue(engine.tspin,
				"EZ branch sets tspin when kickused and tspinEnableEZ");
		assertTrue(engine.tspinez,
				"EZ branch also flips the tspinez flag");
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		gm.engine[0].createFieldIfNeeded();
		return gm.engine[0];
	}
}
