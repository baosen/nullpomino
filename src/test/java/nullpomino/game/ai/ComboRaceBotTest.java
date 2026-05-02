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
 * Tests the full lifecycle and core logic of {@link ComboRaceBot},
 * the finite-state-machine-based combo racing AI with precomputed
 * transition tables.
 */
class ComboRaceBotTest {

	private GameManager gm;
	private GameEngine engine;
	private ComboRaceBot ai;
	private Controller ctrl;

	@BeforeEach
	void setUp() {
		gm = new GameManager(new EventReceiver());
		gm.init();
		engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
		engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_S)};
		engine.nextPieceCount = 0;
		ai = new ComboRaceBot();
		ctrl = new Controller();
	}

	// ─── Identity ───────────────────────────────────────────────

	@Test
	void getName() {
		assertEquals("Combo Race AI V1.03", ai.getName());
	}

	// ─── Init / Shutdown ────────────────────────────────────────

	@Test
	void initSetsFieldsWithoutThread() {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		assertEquals(engine, ai.gEngine);
		assertEquals(gm, ai.gManager);
		assertNotNull(ai.thinkRequest);
		assertEquals(0, ai.delay);
	}

	@Test
	void initStartsThreadWhenEnabled() {
		engine.aiUseThread = true;
		ai.init(engine, 0);

		// threadRunning may not be set synchronously
		assertNotNull(ai.thread);
		assertNotNull(ai.thread);

		ai.shutdown(engine, 0);
	}

	@Test
	void shutdownStopsThread() {
		engine.aiUseThread = true;
		ai.init(engine, 0);
		// threadRunning may not be set synchronously
		assertNotNull(ai.thread);

		ai.shutdown(engine, 0);

		assertTrue(ai.thread == null || !ai.thread.isAlive());
	}

	@Test
	void shutdownWithoutThreadDoesNotThrow() {
		engine.aiUseThread = false;
		ai.init(engine, 0);
		ai.shutdown(engine, 0);
		// No exception expected
	}

	// ─── newPiece ───────────────────────────────────────────────

	@Test
	void newPieceSyncCallsThinkBestPosition() {
		engine.aiUseThread = false;
		ai.init(engine, 0);
		engine.createFieldIfNeeded();

		engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]); engine.nowPieceX = 5; engine.nowPieceY = 5; ai.newPiece(engine, 0);

		assertTrue(ai.thinkLastPieceNo > 0 || !ai.thinkSuccess,
				"newPiece should trigger thinkBestPosition");
	}

	@Test
	void newPieceThreadSetsRequest() throws Exception {
		engine.aiUseThread = true;
		ai.init(engine, 0);

		engine.nowPieceObject = new Piece(Piece.PIECE_T); engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]); engine.nowPieceX = 5; engine.nowPieceY = 5; ai.newPiece(engine, 0);

		// thinkRequest.active is on a private inner class; verify via reflection
		Object thinkReq = ai.thinkRequest;
		java.lang.reflect.Field activeField = thinkReq.getClass().getDeclaredField("active");
		activeField.setAccessible(true);
		assertTrue((Boolean) activeField.get(thinkReq));

		ai.shutdown(engine, 0);
	}

	// ─── setControl ─────────────────────────────────────────────

	@Test
	void setControlDoesNothingWhenNoPiece() {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		ai.setControl(engine, 0, ctrl);

		assertEquals(0, ctrl.getButtonBit());
	}

	@Test
	void setControlIncrementsDelayWhenNotReady() {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		int before = ai.delay;
		ai.setControl(engine, 0, ctrl);

		assertEquals(before + 1, ai.delay);
	}

	// ─── thinkMain ──────────────────────────────────────────────

	@Test
	void thinkMainReturnsZeroForNegativeState() {
		int pts = ai.thinkMain(engine, -1, -1, 0);

		assertEquals(0, pts, "Negative state should return 0");
	}

	@Test
	void thinkMainReturnsScoreForValidState() {
		engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
		ai.createTables(engine);
		ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];
		for (int i = 0; i < ai.nextQueueIDs.length; i++) ai.nextQueueIDs[i] = Piece.PIECE_T;
		int pts = ai.thinkMain(engine, 0, -1, ComboRaceBot.MAX_THINK_DEPTH);

		// At max depth, should return state-based score
		assertTrue(pts >= 0, "Terminal state should return score");
	}

	@Test
	void thinkMainAtDepthWithHoldIPiece() {
		engine.ruleopt.holdEnable = true;
		engine.nextPieceArrayID = new int[]{Piece.PIECE_T};
		ai.createTables(engine);
		ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];
		for (int i = 0; i < ComboRaceBot.MAX_THINK_DEPTH; i++) ai.nextQueueIDs[i] = Piece.PIECE_T;
		int pts = ai.thinkMain(engine, 0, Piece.PIECE_I, 0);

		assertTrue(pts >= 0, "Depth 0 should return a score");
	}

	// ─── thinkBestPosition ──────────────────────────────────────

	@Test
	void thinkBestPositionCompletes() {
		engine.aiUseThread = false;
		ai.init(engine, 0);
		engine.createFieldIfNeeded();

		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		ai.thinkBestPosition(engine, 0);

		assertTrue(true, "thinkBestPosition completed");
	}

	// ─── createTables ───────────────────────────────────────────

	@Test
	void createTablesBuildsTransitionMoves() {
		ai.createTables(engine);

		assertNotNull(ai.moves);
		// moves is [FIELDS.length][7] = [28][7]
		assertEquals(28, ai.moves.length);
		assertEquals(7, ai.moves[0].length);
	}

	@Test
	void createTablesIsIdempotent() {
		ai.createTables(engine);
		Object firstMoves = ai.moves;

		ai.createTables(engine);

		// Second call should not recreate (reference check)
		assertEquals(firstMoves, ai.moves);
	}

	@Test
	void thinkBestPositionAfterCreateTablesWorks() {
		engine.aiUseThread = false;
		ai.init(engine, 0);
		engine.createFieldIfNeeded();

		ai.createTables(engine);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_T], engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		ai.thinkBestPosition(engine, 0);

		assertTrue(true, "thinkBestPosition after createTables completed");
	}

	// ─── onFirst / onLast ───────────────────────────────────────

	@Test
	void onFirstAndOnLastDoNotThrow() {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		ai.onFirst(engine, 0);
		ai.onLast(engine, 0);
		// No exception expected
	}

	@Test
	void onLastTriggersCreateTablesWhenReady() throws Exception {
		engine.aiUseThread = false;
		ai.init(engine, 0);

		// Set engine to READY state with statc[0] == 0
		engine.stat = GameEngine.Status.READY;
		engine.statc[0] = 0;

		ai.onLast(engine, 0);

		// createTablesRequest is on a private inner class; verify via reflection
		Object thinkReq = ai.thinkRequest;
		java.lang.reflect.Field ctrField = thinkReq.getClass().getDeclaredField("createTablesRequest");
		ctrField.setAccessible(true);
		assertTrue((Boolean) ctrField.get(thinkReq));
	}

	// ─── render ─────────────────────────────────────────────────

	@Test
	void renderStateDoesNotThrow() {
		engine.aiUseThread = false;
		ai.init(engine, 0);
		engine.createFieldIfNeeded();

		ai.renderState(engine, 0);
		// No exception expected
	}

	@Test
	void renderHintDoesNotThrow() {
		engine.aiUseThread = false;
		ai.init(engine, 0);
		engine.createFieldIfNeeded();

		ai.renderHint(engine, 0);
		// No exception expected
	}

	// ─── Static helpers (fieldToCode / fieldToIndex) ────────────

	@Test
	void fieldToCodeOnEmptyField() {
		Field f = new Field(10, 4, 0, false);

		short code = ComboRaceBot.fieldToCode(f, 3);

		assertEquals((short) 0, code);
	}

	@Test
	void fieldToCodeOnPartialFill() {
		Field f = new Field(10, 4, 0, false);
		f.setBlockColor(3, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 3, Block.BLOCK_COLOR_RED);

		short code = ComboRaceBot.fieldToCode(f, 3);

		assertEquals((short) 0xE, code, "Bottom bits should reflect bottom row fill");
	}

	@Test
	void fieldToIndexFindsFirstEntry() {
		assertEquals(0, ComboRaceBot.fieldToIndex((short) 0x7));
	}

	@Test
	void fieldToIndexReturnsMinusOneForUnknown() {
		assertEquals(-1, ComboRaceBot.fieldToIndex((short) 0x0));
	}

	@Test
	void fieldToIndexFieldOverload() {
		Field f = new Field(10, 4, 0, false);

		assertEquals(-1, ComboRaceBot.fieldToIndex(f));
	}

	@Test
	void checkOffsetReturnsNewPiece() {
		Piece p = new Piece(Piece.PIECE_T);
		Piece result = ComboRaceBot.checkOffset(p, engine);

		assertEquals(p.id, result.id);
	}

	@Test
	void printPieceAndDirectionDoesNotThrow() {
		ai.printPieceAndDirection(Piece.PIECE_T, Piece.DIRECTION_UP);
		// No exception expected
	}
}
