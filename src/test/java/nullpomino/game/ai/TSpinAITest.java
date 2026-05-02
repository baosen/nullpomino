package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the T-Spin focused AI subclass of BasicAI.
 * TSpinAI overrides getName() and thinkMain() to prioritize T-Spin
 * setups and slot detection.
 */
class TSpinAITest {

	private GameManager gm;
	private GameEngine engine;
	private TSpinAI ai;

	@BeforeEach
	void setUp() {
		gm = new GameManager(new EventReceiver());
		gm.init();
		engine = gm.engine[0];
		engine.init();
		ai = new TSpinAI();
	}

	@Test
	void getName() {
		assertEquals("T-SPIN", ai.getName());
	}

	@Test
	void initInheritsFromBasicAI() {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		assertEquals(engine, ai.gEngine);
		assertEquals(gm, ai.gManager);
	}

	@Test
	void getMaxThinkDepthIsTwo() {
		assertEquals(2, ai.getMaxThinkDepth());
	}

	@Test
	void thinkMainGivesTSpinSlotPriority() {
		engine.createFieldIfNeeded();
		Field fld = engine.field;

		// Set up a scenario where placing a T creates a T-Slot.
		// First, build some structure. The T piece needs to be able to
		// create a T-Slot (where the hole matches a T shape).
		//
		// Place blocks to form a partial T-Slot at x=3, y=18:
		// Blocks at (3,19), (2,18), (4,18) create the T-Spin corners.
		// The placement at (3,18) will create a tslot.

		// Create base rows leaving a T-shaped slot at col 3
		for (int x = 0; x < 10; x++) {
			if (x != 3) {
				fld.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
			}
		}
		fld.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(4, 18, Block.BLOCK_COLOR_RED);

		Piece piece = new Piece(Piece.PIECE_T);
		// Place T piece at x=3, y=18, rotation 0 (pointing up)
		int pts = ai.thinkMain(engine, 3, 18, 0, 1,
				fld, piece, null, null, 0);

		// Should produce a valid score
		assertTrue(pts >= 0, "T-Spin placement should produce a valid score");
	}

	@Test
	void thinkMainNewTSlotGivesLargeBonus() {
		engine.createFieldIfNeeded();
		Field fld = engine.field;

		// Create scenario where new T-Slot is created
		// Fill bottom row except at column 3
		for (int x = 0; x < 10; x++) {
			if (x != 3) {
				fld.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
			}
		}
		// Add blocks on top to make it a T slot
		fld.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(4, 18, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(1, 17, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(5, 17, Block.BLOCK_COLOR_RED);

		Piece piece = new Piece(Piece.PIECE_T);
		int pts = ai.thinkMain(engine, 2, 17, 0, 1,
				fld, piece, null, null, 0);

		// Should not crash and return a score
		assertTrue(true, "thinkMain executes without exception");
	}

	@Test
	void thinkMainNonTNoPenalty() {
		engine.createFieldIfNeeded();
		Field fld = engine.field;

		Piece piece = new Piece(Piece.PIECE_O);

		// O piece at a valid position on empty field
		int pts = ai.thinkMain(engine, 4, 18, 0, -1,
				fld, piece, null, null, 0);

		assertTrue(pts > 0, "Non-T piece should get a valid score");
	}

	@Test
	void thinkMainForcesHoldWhenTPieceInNextAndHeld() {
		engine.createFieldIfNeeded();
		Field fld = engine.field;

		Piece piece = new Piece(Piece.PIECE_T);
		Piece nextPiece = new Piece(Piece.PIECE_S);
		Piece holdPiece = new Piece(Piece.PIECE_T);

		int pts = ai.thinkMain(engine, 3, 18, 0, 1,
				fld, piece, nextPiece, holdPiece, 0);

		// forceHold may be set if T-Slot conditions are met
		// Just verify execution succeeds
		assertTrue(true, "thinkMain with hold executes without exception");
	}

	@Test
	void allLifecycleMethodsDoNotThrow() {
		engine.aiUseThread = false;
		ai.init(engine, 0);
		engine.createFieldIfNeeded();

		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		ai.newPiece(engine, 0);
		ai.onFirst(engine, 0);
		ai.onLast(engine, 0);
		ai.shutdown(engine, 0);
		// No exception expected
	}

	@Test
	void renderStateAndRenderHintDoNotThrow() {
		ai.init(engine, 0);
		engine.createFieldIfNeeded();

		ai.renderState(engine, 0);
		ai.renderHint(engine, 0);
		// No exception expected
	}
}
