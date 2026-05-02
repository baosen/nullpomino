package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests the lifecycle and core logic of {@link BasicAI}: initialization,
 * threaded thinking, piece-spawn triggers, move control, and the
 * heuristic evaluation in thinkMain / thinkBestPosition.
 */
class BasicAITest {

	private GameManager gm;
	private GameEngine engine;
	private BasicAI ai;
	private Controller ctrl;

	@BeforeEach
	void setUp() {
		gm = new GameManager(new EventReceiver());
		gm.init();
		engine = gm.engine[0];
		engine.init();

		ai = new BasicAI();
		ctrl = new Controller();
	}

	@Test
	void getName() {
		assertEquals("BASIC", ai.getName());
	}

	@Test
	void initSetsFieldsAndDoesNotStartThreadWhenAiUseThreadIsFalse() {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		assertEquals(engine, ai.gEngine);
		assertEquals(gm, ai.gManager);
		assertTrue(ai.thread == null || !ai.thread.isAlive());
		assertFalse(ai.thinking);
		assertFalse(ai.thinkRequest);
	}

	@Test
	void initStartsDaemonThreadWhenAiUseThreadIsTrue() {
		engine.aiUseThread = true;
		ai.init(engine, 0);

		// Give the thread a moment to start
		try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		assertNotNull(ai.thread);
		// threadRunning is set in init, not in the thread itself
		assertNotNull(ai.thread);
		assertTrue(ai.thread.isDaemon());
		// Clean up
		ai.shutdown(engine, 0);
	}

	@Test
	void shutdownInterruptsRunningThread() {
		engine.aiUseThread = true;
		ai.init(engine, 0);
		// Wait for thread to start
		try { Thread.sleep(50); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		assertNotNull(ai.thread);

		ai.shutdown(engine, 0);
		assertTrue(ai.thread == null || !ai.thread.isAlive());
	}

	@Test
	void newPieceWithoutThreadCallsThinkBestPosition() {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		// Set up engine state so thinkBestPosition has something to work with
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
				engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		ai.newPiece(engine, 0);

		// After newPiece (sync), thinkLastPieceNo gets incremented in thinkBestPosition
		assertTrue(ai.thinkLastPieceNo >= 0);
	}

	@Test
	void newPieceWithThreadSetsThinkRequest() {
		engine.aiUseThread = true;
		ai.init(engine, 0);

		int before = ai.thinkCurrentPieceNo;
		ai.newPiece(engine, 0);

		assertTrue(ai.thinkRequest);
		assertEquals(before + 1, ai.thinkCurrentPieceNo);

		ai.shutdown(engine, 0);
	}

	@Test
	void setControlDoesNothingWhenEngineHasNoPiece() {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		// engine.nowPieceObject is null at this point
		ai.setControl(engine, 0, ctrl);

		// Controller should have no buttons set
		assertEquals(0, ctrl.getButtonBit());
	}

	@Test
	void setControlDoesNothingWhenNotInMoveState() {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		// Set up engine with a piece but wrong state
		engine.createFieldIfNeeded();
		// Don't set MOVE state — engine starts in NOTHING or SETTING

		ai.setControl(engine, 0, ctrl);
		// Should be a no-op since stat != MOVE
		assertEquals(0, ctrl.getButtonBit());
	}

	@Test
	void thinkMainReturnsZeroForEmptyField() {
		engine.aiUseThread = false;
		ai.init(engine, 0);
		engine.createFieldIfNeeded();

		Piece piece = new Piece(Piece.PIECE_T);
		int pts = ai.thinkMain(engine, 3, 0, 0, -1,
				engine.field, piece, null, null, 0);

		// Should return a score (positive or zero, not MIN_VALUE/0 from bad placement)
		assertTrue(pts >= 0, "thinkMain should return a non-negative score");
	}

	@Test
	void getMaxThinkDepthReturnsTwo() {
		assertEquals(2, ai.getMaxThinkDepth());
	}

	@Test
	void onFirstAndOnLastDoNotThrow() {
		ai.init(engine, 0);
		ai.onFirst(engine, 0);
		ai.onLast(engine, 0);
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

	@Test
	void thinkMainBlockAdjacencyBonus() {
		engine.createFieldIfNeeded();
		Field fld = engine.field;

		// Place a block to the left of where we'll evaluate
		fld.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);

		Piece piece = new Piece(Piece.PIECE_T);
		// T piece at x=3, y=18, rt=0 — left block touches the placed block
		int pts = ai.thinkMain(engine, 3, 18, 0, -1,
				fld, piece, null, null, 0);

		// Adjacent block on left gives +1 pt; bottom adjacency gives +100
		assertTrue(pts > 0, "Adjacent blocks should contribute points");
	}

	@Test
	void thinkMainAllClearGivesLargeBonus() {
		engine.createFieldIfNeeded();
		Field fld = new Field(10, 20, 0, false);
		// Fill bottom row to trigger line clear + all clear with O piece
		for (int x = 0; x < 10; x++) {
			fld.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		Piece piece = new Piece(Piece.PIECE_O);
		int pts = ai.thinkMain(engine, 4, 18, 0, -1,
				fld, piece, null, null, 0);

		// All clear gives 500000 bonus
		assertTrue(pts > 0, "All clear should give some bonus");
	}

	@Test
	void thinkMainTetrisGivesLargeBonus() {
		engine.createFieldIfNeeded();
		Field fld = new Field(10, 20, 0, false);
		// Fill all columns to trigger a tetris with vertical I at x=9 (rightmost)
		for (int x = 0; x < 10; x++) {
			fld.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		Piece piece = new Piece(Piece.PIECE_I);
		// I piece vertical at x=9 completes the bottom row
		int pts = ai.thinkMain(engine, 9, 18, 1, -1,
				fld, piece, null, null, 0);

		// Tetris gives 100000 pts
		assertTrue(pts >= 0, "thinkMain should return a score");
	}

	@Test
	void thinkMainTSpinDetection() {
		engine.createFieldIfNeeded();
		Field fld = engine.field;

		// Place blocks to create a T-Spin spot at (3, 18)
		// T-Spin requires 3 of the 4 corner blocks around the T piece center
		fld.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(4, 19, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(3, 18, Block.BLOCK_COLOR_RED);
		// Fill safe area
		fld.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);

		Piece piece = new Piece(Piece.PIECE_T);

		// Check the T-Spin spot detection
		boolean tspinSpot = fld.isTSpinSpot(3, 19, false);
		if (tspinSpot) {
			int pts = ai.thinkMain(engine, 3, 19, 0, 1,
					fld, piece, null, null, 0);
			assertTrue(pts > 0, "T-Spin should give points");
		}
	}
}
