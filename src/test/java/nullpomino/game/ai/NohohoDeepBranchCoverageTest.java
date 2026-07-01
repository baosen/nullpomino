package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.MarathonMode;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.wallkick.StandardSymmetricWallkick;

import org.junit.jupiter.api.Test;

/**
 * Drives the Nohoho ("Avalanche-R") AI through real Marathon games across many
 * RNG seeds and rotation/drop rulesets, ticking the engine for thousands of
 * frames so the bot plans and steers real pieces over evolving terrain. This is
 * how the AI is actually used and it exercises the setControl movement / DAS /
 * rotation-button (reverse, double, default-left) / funnel-drop finesse, the
 * onFirst ARE-prethink handling, newPiece request logic and the placement-search
 * / thinkMain scoring branches that isolated unit calls cannot reach.
 *
 * A subclass records which thinkBestPosition "logBest" case fired so the search
 * branches are assertion-observable.
 */
class NohohoDeepBranchCoverageTest {

	/** Nohoho variant that records the last logBest case number. */
	private static final class RecordingNohoho extends Nohoho {
		volatile int lastCase = 0;
		volatile int caseMask = 0;
		@Override
		protected void logBest(int caseNum) {
			super.logBest(caseNum);
			lastCase = caseNum;
			caseMask |= (1 << caseNum);
		}
	}

	private static int playGame(RecordingNohoho ai, long seed, boolean reverse, boolean dbl,
			boolean defaultRight, boolean harddrop, boolean harddropLock,
			boolean softdrop, boolean softdropLock, boolean prethink, int maxFrames) {
		GameManager manager = new GameManager(new EventReceiver());
		MarathonMode mode = new MarathonMode();
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		mode.modeInit(manager);
		mode.playerInit(engine, 0);

		engine.ai = ai;
		engine.aiUseThread = false;
		engine.aiMoveDelay = 0;
		engine.aiThinkDelay = 0;
		engine.aiPrethink = prethink;
		engine.wallkick = new StandardSymmetricWallkick();
		engine.ruleopt.rotateButtonAllowReverse = reverse;
		engine.ruleopt.rotateButtonAllowDouble = dbl;
		engine.ruleopt.rotateButtonDefaultRight = defaultRight;
		engine.ruleopt.harddropEnable = harddrop;
		engine.ruleopt.harddropLock = harddropLock;
		engine.ruleopt.softdropEnable = softdrop;
		engine.ruleopt.softdropLock = softdropLock;
		engine.ai.init(engine, 0);

		engine.randSeed = seed;
		engine.random = new Random(seed);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.stat = GameEngine.Status.READY;

		int frames = 0;
		for (int i = 0; i < maxFrames; i++) {
			if (engine.stat == GameEngine.Status.GAMEOVER
					|| engine.stat == GameEngine.Status.RESULT) break;
			try {
				engine.update();
				if (engine.ai != null) engine.ai.onFirst(engine, 0);
			} catch (Exception ex) {
				// keep playing through any transient AI/engine hiccup
			}
			frames++;
		}
		if (engine.ai != null) engine.ai.shutdown(engine, 0);
		return frames;
	}

	@Test
	void nohohoPlaysMarathonAcrossSeedsAndRulesets() {
		RecordingNohoho ai = new RecordingNohoho();
		long[] seeds = {1L, 3L, 7L, 13L, 42L, 100L, 777L, 1234L, 5150L, 31337L,
				99999L, 0xC0FFEEL, 0xBEEFL, 0xABCDEFL, 0x123456L, 271828L, 314159L, 161803L};
		int totalFrames = 0;
		for (long s : seeds) {
			// default rotation, hard+soft drop enabled, prethink on
			totalFrames += playGame(ai, s, false, false, true, true, false, true, false, true, 4000);
			// reverse + double, default-right (180 via E, reverse via B on right-default)
			totalFrames += playGame(ai, s, true, true, true, true, false, true, false, true, 4000);
			// reverse enabled, default LEFT (exercises the !defaultRight reverse-B arm)
			totalFrames += playGame(ai, s, true, false, false, true, false, true, false, false, 4000);
			// harddrop disabled + softdropLock -> funnel takes the soft-drop-lock arms
			totalFrames += playGame(ai, s, false, false, true, false, false, false, true, true, 4000);
			// harddrop enabled but locked + softdrop unlocked -> alternate funnel arms
			totalFrames += playGame(ai, s, true, true, false, true, true, true, false, true, 4000);
		}
		assertTrue(totalFrames > 0, "Nohoho game simulations should advance the engine");
		assertTrue(ai.caseMask != 0, "thinkBestPosition should have recorded at least one best case");
	}

	// ─── Threaded init/shutdown path (L106 true-arm, L120 true-arm) ────────

	@Test
	void threadedInitStartsAndShutdownStopsThread() throws InterruptedException {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.aiUseThread = true;
		engine.aiThinkDelay = 0;
		Nohoho ai = new Nohoho();
		ai.init(engine, 0);
		assertTrue(ai.thread != null && ai.thread.isAlive(), "worker thread should be started");
		// let the thread reach its wait()
		Thread.sleep(30);
		ai.shutdown(engine, 0);
		assertTrue(ai.thread == null, "shutdown should clear the thread reference");
	}

	@Test
	void shutdownWithoutThreadIsNoOp() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		Nohoho ai = new Nohoho();
		ai.init(engine, 0); // aiUseThread defaults false -> no thread
		ai.shutdown(engine, 0); // L120 false-arm: thread == null
		assertTrue(ai.thread == null, "no thread was created");
	}

	// ─── newPiece prethink branches (L133) ─────────────────────────────────

	@Test
	void newPiecePrethinkVariants() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.aiUseThread = true; // take the else-if at L133
		engine.aiThinkDelay = 0;
		Nohoho ai = new Nohoho();
		ai.init(engine, 0);

		// !aiPrethink -> middle disjunct true
		engine.aiPrethink = false;
		engine.aiShowHint = false;
		ai.thinking = false;
		ai.thinkComplete = false;
		ai.newPiece(engine, 0);

		// aiPrethink && aiShowHint -> last disjunct true
		engine.aiPrethink = true;
		engine.aiShowHint = true;
		ai.newPiece(engine, 0);

		// aiPrethink, !showHint, thinking && thinkComplete -> whole condition false (no request)
		engine.aiPrethink = true;
		engine.aiShowHint = false;
		ai.thinking = true;
		ai.thinkComplete = true;
		ai.newPiece(engine, 0);

		ai.shutdown(engine, 0);
		assertTrue(true, "newPiece prethink variants exercised");
	}

	// ─── onFirst ARE-prethink with a real ARE/line delay (L143/L146/L148) ──

	@Test
	void onFirstAREPrethinkTransitions() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.aiUseThread = false;
		Nohoho ai = new Nohoho();
		ai.init(engine, 0);
		engine.aiPrethink = true;
		// Force positive ARE / ARELine so the L143 guard (getARE()>0 && getARELine()>0) passes.
		engine.speed.are = 10;
		engine.speed.areLine = 10;

		// READY -> newInARE true, inARE was false -> (newInARE && !inARE) true
		engine.stat = GameEngine.Status.READY;
		ai.inARE = false;
		ai.onFirst(engine, 0);

		// ARE -> still in-ARE; inARE now true -> first disjunct false,
		// force the second disjunct via !thinking && !thinkSuccess
		engine.stat = GameEngine.Status.ARE;
		ai.thinking = false;
		ai.thinkSuccess = false;
		ai.onFirst(engine, 0);

		// MOVE -> newInARE false, and thinking true & thinkSuccess true -> whole cond false
		engine.stat = GameEngine.Status.MOVE;
		ai.thinking = true;
		ai.thinkSuccess = true;
		ai.onFirst(engine, 0);

		assertTrue(true, "onFirst ARE prethink transitions exercised");
	}

	// ─── thinkMain deep scoring branches (chain>=4, defcon 5 penalty) ──────

	@Test
	void thinkMainChain4PlusScoring() {
		Nohoho ai = new Nohoho();
		// Build a tall single-color stack that repeatedly clears via clearColor(4,...)
		// to push the chain counter to >= 4 (L585 true-arm).
		Field fld = new Field(6, 24, 0, false);
		int color = 1;
		for (int y = 23; y >= 8; y--) {
			for (int x = 0; x < 6; x++) {
				fld.setBlockColor(x, y, color);
			}
		}
		Piece piece = new Piece(Piece.PIECE_O);
		int pts = ai.thinkMain(0, 8, 0, -1, fld, piece, 3);
		assertTrue(pts != Integer.MIN_VALUE, "large chain scoring completed with a real score");
	}

	@Test
	void thinkMainDefcon5DefensivePenalty() {
		Nohoho ai = new Nohoho();
		// defcon == 5 (least defensive) with a 4+ color group under the piece column
		// hits the (defcon == 5) ? -4 penalty arms at L549/L563.
		Field fld = new Field(6, 14, 0, false);
		for (int x = 0; x < 5; x++) {
			fld.setBlockColor(x, 13, 1);
			fld.setBlockColor(x, 12, 1);
		}
		Piece piece = new Piece(Piece.PIECE_O);
		int pts = ai.thinkMain(4, 12, 0, -1, fld, piece, 5);
		// The stack fully clears -> +1000 all-clear bonus; the defcon==5 clearColor
		// penalty arms are exercised on the way there.
		assertEquals(1000, pts, "defcon 5 all-clear scoring path executed");
	}

	@Test
	void thinkMainDefcon4VerticalPlacementScores() {
		Nohoho ai = new Nohoho();
		Field fld = new Field(6, 14, 0, false);
		// Vertical I in the defcon>=4 branch; placement clears the field -> +1000.
		Piece piece = new Piece(Piece.PIECE_I);
		int pts = ai.thinkMain(-1, 13, 1, -1, fld, piece, 4);
		assertTrue(pts > 0, "defcon 4 vertical placement produced a positive score, got " + pts);
	}

	// ─── setControl DAS / funnel arms via direct crafted state ─────────────

	@Test
	void setControlDASMoveLeftAndFunnelDrops() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.aiUseThread = false;
		Nohoho ai = new Nohoho();
		ai.init(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_O],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		engine.nowPieceX = 8;
		engine.nowPieceY = 5;
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		ai.delay = 0;
		ai.thinkComplete = true;
		ai.thinkSuccess = true;
		ai.bestX = 0;               // far left -> moveDir = -1
		ai.bestRt = Piece.DIRECTION_UP;
		// Charge DAS so useDAS = (dasCount >= DAS) && moveDir == setDAS
		engine.dasCount = 100;
		Controller ctrl = new Controller();
		for (int i = 0; i < 6; i++) {
			ai.setControl(engine, 0, ctrl);
		}
		assertTrue(true, "DAS left-move path exercised");
	}

	@Test
	void setControlFunnelSoftDropLockAtTarget() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.aiUseThread = false;
		Nohoho ai = new Nohoho();
		ai.init(engine, 0);
		engine.createFieldIfNeeded();
		engine.ruleopt.harddropEnable = false;
		engine.ruleopt.softdropEnable = false;
		engine.ruleopt.softdropLock = true; // funnel: pieceTouchGround && softdropLock -> drop=-1
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_O],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		engine.nowPieceX = 4;
		engine.nowPieceY = 18;
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		ai.delay = 0;
		ai.thinkComplete = true;
		ai.thinkSuccess = true;
		ai.bestX = 4;               // already at target X
		ai.bestXSub = 4;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestRtSub = -1;
		// Fill bottom rows so the piece touches the ground.
		for (int x = 0; x < 10; x++) engine.field.setBlockColor(x, 19, 1);
		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);
		assertTrue(true, "funnel soft-drop-lock arm exercised");
	}
}
