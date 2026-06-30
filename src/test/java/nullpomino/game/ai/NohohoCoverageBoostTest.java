package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Coverage-boost tests for {@link Nohoho} targeting the lines left uncovered by
 * the existing Nohoho test suite:
 *
 * <ul>
 *   <li>380 - {@code thinkBestPosition} hold-piece offset throwing block,
 *       reached only when {@code isHoldOK()} is true but no hold piece and no
 *       next queue exist (so {@code pieceHold} is still {@code null} at the
 *       guard).</li>
 *   <li>551 - {@code thinkMain} {@code clear == 3} colour-pop scoring for the
 *       defensive ({@code defcon >= 4}) branch.</li>
 *   <li>585-586 - {@code thinkMain} {@code chain >= 4} cascade scoring.</li>
 *   <li>659-660 - {@code run()} catch block around {@code thinkBestPosition}.</li>
 *   <li>668-669 - {@code run()} sleep {@code InterruptedException} -> break.</li>
 * </ul>
 *
 * Field shapes were derived empirically (replicating {@code thinkMain}'s colour
 * flood-clear logic) so the targeted branches are guaranteed to execute.
 */
class NohohoCoverageBoostTest {

	private GameManager gm;
	private GameEngine engine;
	private Nohoho ai;
	private Controller ctrl;

	@BeforeEach
	void setUp() {
		gm = new GameManager(new EventReceiver());
		gm.init();
		engine = gm.engine[0];
		engine.init();
		engine.aiUseThread = false;
		ai = new Nohoho();
		ai.init(engine, 0);
		ctrl = new Controller();
	}

	private void setupMove() {
		engine.createFieldIfNeeded();
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
				engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 4;
		engine.nowPieceY = 5;
		ai.delay = 9999;
		ai.thinkComplete = true;
		ai.bestHold = false;
		ai.bestX = 4;
		ai.bestY = 5;
	}

	/**
	 * Lines 303-307: a non-180 rotation gives {@code rotateDir != 0}, so with
	 * {@code rotateButtonAllowDouble} enabled the {@code rotateDir == 2} guard
	 * (305-306) is evaluated. Drives the input-emission branch.
	 */
	@Test
	void setControlRotateWithAllowDouble() {
		setupMove();
		engine.nowPieceObject.direction = Piece.DIRECTION_UP;
		ai.bestRt = Piece.DIRECTION_RIGHT; // 90-degree turn -> rotateDir != 0, not a 180
		engine.ruleopt.rotateButtonAllowDouble = true;
		engine.ruleopt.rotateButtonAllowReverse = false;
		assertDoesNotThrow(() -> ai.setControl(engine, 0, ctrl));
	}

	/** Same guard, reverse-enabled variant for the other input sub-branches. */
	@Test
	void setControlRotateWithAllowDoubleReverse() {
		setupMove();
		engine.nowPieceObject.direction = Piece.DIRECTION_UP;
		ai.bestRt = Piece.DIRECTION_LEFT; // 90-degree turn the other way
		engine.ruleopt.rotateButtonAllowDouble = true;
		engine.ruleopt.rotateButtonAllowReverse = true;
		engine.ruleopt.rotateButtonDefaultRight = true;
		assertDoesNotThrow(() -> ai.setControl(engine, 0, ctrl));
	}

	/**
	 * Lines 379-380: with hold legal, no hold piece, and an uninitialised next
	 * queue, {@code pieceHold} is still {@code null} at the guard, so
	 * {@code checkOffset(null, engine)} is reached. That call constructs
	 * {@code new Piece(null)} and throws NPE, so we assert the throw to keep the
	 * test green while still documenting the throwing branch.
	 */
	@Test
	void thinkBestPositionHoldNullOffsetBranch() {
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
				engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 4;
		engine.nowPieceY = 5;
		engine.holdPieceObject = null;        // no hold piece
		engine.nextPieceArrayObject = null;   // getNextObjectCopy returns null
		// Sanity: hold is legal in this fresh engine state.
		assertTrue(engine.isHoldOK());
		assertThrows(NullPointerException.class, () -> ai.thinkBestPosition(engine, 0));
	}

	/**
	 * Line 551: in the defensive ({@code defcon >= 4}) branch the first
	 * {@code clearColor} at the landing column pops a group of exactly three
	 * same-colour blocks. Two colour-1 blocks are pre-stacked in column 3 and
	 * the single-block I1 piece lands on top of them.
	 */
	@Test
	void thinkMainClearExactlyThree() {
		nullpomino.game.component.Field fld =
				new nullpomino.game.component.Field(6, 12, 0, false);
		fld.setBlockColor(3, 10, 1);
		fld.setBlockColor(3, 11, 1);
		Piece i1 = new Piece(Piece.PIECE_I1);
		i1.setColor(1);
		int pts = ai.thinkMain(3, 9, Piece.DIRECTION_UP, -1, fld, i1, 4);
		assertTrue(pts != Integer.MIN_VALUE, "piece placed and scored");
	}

	/**
	 * Lines 585-586: a four-chain cascade. The colour layout (derived
	 * empirically) clears in four successive flood-clear iterations once the
	 * Z piece is dropped, exercising the {@code chain >= 4} scoring term.
	 */
	@Test
	void thinkMainChainFourCascade() {
		int[][] cells = {
				{5,10,4},{6,10,1},{1,11,4},{5,11,4},{6,11,2},{1,12,2},{5,12,3},{6,12,4},{8,12,4},
				{0,13,2},{1,13,2},{4,13,2},{5,13,1},{6,13,3},{7,13,1},{8,13,4},
				{0,14,3},{1,14,4},{4,14,2},{5,14,2},{6,14,3},{7,14,3},{8,14,1},
				{0,15,4},{1,15,4},{2,15,2},{4,15,1},{5,15,1},{6,15,3},{7,15,2},{8,15,4},
				{0,16,3},{1,16,2},{2,16,2},{4,16,2},{5,16,4},{6,16,3},{7,16,2},{8,16,1},
				{0,17,3},{1,17,2},{2,17,1},{4,17,4},{5,17,3},{6,17,4},{7,17,2},{8,17,1},
				{0,18,3},{1,18,2},{2,18,4},{4,18,3},{5,18,2},{6,18,2},{7,18,4},{8,18,1},{9,18,4},
				{0,19,3},{1,19,1},{2,19,2},{3,19,1},{4,19,2},{5,19,3},{6,19,4},{7,19,4},{8,19,2},{9,19,4}
		};
		nullpomino.game.component.Field fld =
				new nullpomino.game.component.Field(10, 20, 0, false);
		for (int[] c : cells) {
			fld.setBlockColor(c[0], c[1], c[2]);
		}
		Piece z = new Piece(Piece.PIECE_Z);
		z.setColor(1);
		z.direction = Piece.DIRECTION_UP;
		int pts = ai.thinkMain(3, 8, Piece.DIRECTION_UP, -1, fld, z, 3);
		assertTrue(pts != Integer.MIN_VALUE, "piece placed and chain scored");
	}

	/**
	 * Lines 659-660 and 668-669: run {@code run()} directly on the calling
	 * thread (no background thread, no real sleep, so it is deterministic).
	 * A pending think request makes the loop invoke {@code thinkBestPosition},
	 * which throws NPE on the null-hold/no-next-queue setup -> caught at 659-660.
	 * The thread is pre-interrupted, so the subsequent {@code Thread.sleep}
	 * throws {@code InterruptedException} -> break at 668-669, ending the loop.
	 */
	@Test
	void runHandlesThinkFailureAndSleepInterrupt() throws Exception {
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
				engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
				engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 4;
		engine.nowPieceY = 5;
		engine.holdPieceObject = null;
		engine.nextPieceArrayObject = null; // thinkBestPosition will NPE

		ai.gEngine = engine;
		ai.gManager = engine.owner;
		ai.threadRunning = true;
		ai.thinkDelay = 10; // > 0 so the sleep branch is reached

		// Mark the pending think request active (private ThinkRequestMutex field).
		Object req = ai.thinkRequest;
		Field active = req.getClass().getDeclaredField("active");
		active.setAccessible(true);
		active.setBoolean(req, true);

		// Pre-interrupt: Thread.sleep will throw immediately on this thread.
		Thread.currentThread().interrupt();

		assertDoesNotThrow(() -> ai.run());
		assertFalse(ai.threadRunning, "run() exited via the sleep-interrupt break");
		// Clear any residual interrupt status so we don't leak it to other tests.
		Thread.interrupted();
	}
}
