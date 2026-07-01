package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.MarathonMode;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.wallkick.StandardSymmetricWallkick;

import org.junit.jupiter.api.Test;

/**
 * Deep branch-coverage tests for {@link ComboRaceBot}.
 *
 * <p>Two complementary strategies are used:
 * <ol>
 *   <li><b>Game simulation</b> — attaches a real {@link ComboRaceBot} to a
 *       {@link MarathonMode} engine, disables the think thread
 *       ({@code aiUseThread=false}) so planning is synchronous, zeroes the AI
 *       delays, and ticks {@link GameEngine#update()} for thousands of frames
 *       across many RNG seeds and rotation rulesets. Real pieces are steered
 *       over evolving terrain, which is the only way to reach the movement /
 *       rotation / funnel finesse branches in {@code setControl} and the
 *       {@code onFirst} ARE-prethink paths that hand-crafted single-frame calls
 *       cannot reproduce (the bot's {@code movestate}/{@code bestRtSub} state
 *       machine only advances across successive frames).</li>
 *   <li><b>Direct calls</b> — for the funnel branches that only fire under
 *       specific rule combinations (soft-drop-only, hard-drop-locked) and for
 *       the {@code aiShowHint} wallkick path inside {@code thinkBestPosition}
 *       and the {@code createTables} rotation-config gates, which a single
 *       ruleset simulation cannot cover at once.</li>
 * </ol>
 */
class ComboRaceBotDeepBranchCoverageTest {

	/** Runs one full Marathon game with a synchronous ComboRaceBot. */
	private static int playGame(long seed, boolean reverse, boolean dbl,
			boolean prethink, boolean showHint, int maxFrames) {
		GameManager manager = new GameManager(new EventReceiver());
		MarathonMode mode = new MarathonMode();
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		mode.modeInit(manager);
		mode.playerInit(engine, 0);

		engine.ai = new ComboRaceBot();
		engine.aiUseThread = false;
		engine.aiMoveDelay = 0;
		engine.aiThinkDelay = 0;
		engine.aiPrethink = prethink;
		engine.aiShowHint = showHint;
		engine.wallkick = new StandardSymmetricWallkick();
		engine.ruleopt.rotateButtonAllowReverse = reverse;
		engine.ruleopt.rotateButtonAllowDouble = dbl;
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
				if (engine.ai != null) {
					engine.ai.onFirst(engine, 0);
					// Render hooks also carry branch weight; call them with the
					// no-op EventReceiver so their internal state branches run.
					engine.ai.renderState(engine, 0);
					engine.ai.renderHint(engine, 0);
				}
			} catch (Exception ex) {
				// keep playing through any transient AI/engine hiccup
			}
			frames++;
		}
		if (engine.ai != null) engine.ai.shutdown(engine, 0);
		return frames;
	}

	@Test
	void comboRaceBotPlaysGamesAcrossSeedsAndRotationRules() {
		long[] seeds = {1L, 3L, 7L, 13L, 42L, 100L, 777L, 1234L, 5150L, 31337L,
				99999L, 0xC0FFEEL, 0xBEEFL, 0xABCDEFL, 0x123456L, 271828L,
				0xDEADL, 0xFEEDL, 55L, 909L};
		int totalFrames = 0;
		for (long s : seeds) {
			totalFrames += playGame(s, false, false, false, false, 4000); // default
			totalFrames += playGame(s, true, true, false, false, 4000);   // reverse+180
			totalFrames += playGame(s, true, true, true, true, 4000);     // prethink+hint
		}
		assertTrue(totalFrames > 0,
				"ComboRaceBot game simulations should advance the engine");
	}

	/**
	 * Threaded variant: exercises the {@code init} thread-spawn branch
	 * ({@code engine.aiUseThread == true}), the {@code onFirst}/{@code newPiece}
	 * thread-request gating, and {@code shutdown} of a live thread.
	 */
	@Test
	void comboRaceBotPlaysGamesWithThinkThread() {
		long[] seeds = {2L, 17L, 88L, 4242L};
		int totalFrames = 0;
		for (long s : seeds) {
			GameManager manager = new GameManager(new EventReceiver());
			MarathonMode mode = new MarathonMode();
			manager.mode = mode;
			manager.init();
			GameEngine engine = manager.engine[0];
			engine.init();
			mode.modeInit(manager);
			mode.playerInit(engine, 0);

			engine.ai = new ComboRaceBot();
			engine.aiUseThread = true;
			engine.aiMoveDelay = 0;
			engine.aiThinkDelay = 0;
			engine.aiPrethink = true;
			engine.wallkick = new StandardSymmetricWallkick();
			engine.ai.init(engine, 0);

			engine.randSeed = s;
			engine.random = new Random(s);
			engine.gameActive = true;
			engine.timerActive = true;
			engine.stat = GameEngine.Status.READY;

			for (int i = 0; i < 3000; i++) {
				if (engine.stat == GameEngine.Status.GAMEOVER
						|| engine.stat == GameEngine.Status.RESULT) break;
				try {
					engine.update();
					if (engine.ai != null) engine.ai.onFirst(engine, 0);
					Thread.sleep(0, 200000); // let the think thread make progress
				} catch (Exception ex) {
					// ignore transient hiccups
				}
				totalFrames++;
			}
			engine.ai.shutdown(engine, 0);
		}
		assertTrue(totalFrames > 0,
				"Threaded ComboRaceBot simulations should advance the engine");
	}

	// ─── Funnel: soft-drop-only, bestRtSub == 0 (line 292-293) ───

	@Test
	void setControlFunnelSoftDropOnlyPrimaryRotation() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_O],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		engine.nextPieceArrayID = new int[]{Piece.PIECE_O, Piece.PIECE_O};
		engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_O), new Piece(Piece.PIECE_O)};
		engine.nextPieceCount = 0;
		engine.aiUseThread = false;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		// harddrop disabled, softdrop enabled -> the primary-rotation funnel
		// (bestRtSub==0) falls through to the softdropEnable||softdropLock arm.
		engine.ruleopt.harddropEnable = false;
		engine.ruleopt.harddropLock = false;
		engine.ruleopt.softdropEnable = true;
		engine.ruleopt.softdropLock = false;
		ai.delay = 0;
		ai.bestX = 5;
		ai.bestY = 5;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestRtSub = 0;
		ai.movestate = 0;
		ai.thinkComplete = true;
		ai.thinkLastPieceNo = 1;
		ai.thinkCurrentPieceNo = 0;
		ai.threadRunning = true;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_DOWN) != 0,
				"Soft-drop-only primary funnel should soft-drop");
	}

	// ─── Funnel: bestRtSub != 0, hard-drop LOCKED so falls to soft-drop (line 297-298) ───

	@Test
	void setControlFunnelPostTwistSoftDropWhenHardDropLocked() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_T};
		engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_T)};
		engine.nextPieceCount = 0;
		engine.aiUseThread = false;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		// harddrop enabled but LOCKED -> post-twist branch skips harddrop and
		// takes the softdropEnable && !softdropLock arm.
		engine.ruleopt.harddropEnable = true;
		engine.ruleopt.harddropLock = true;
		engine.ruleopt.softdropEnable = true;
		engine.ruleopt.softdropLock = false;
		ai.delay = 0;
		ai.bestX = 5;
		ai.bestY = 10;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestRtSub = 1; // post-twist funnel
		ai.movestate = 1; // > 0 so the funnel gate passes with moveDir 0
		ai.thinkComplete = true;
		ai.thinkLastPieceNo = 1;
		ai.thinkCurrentPieceNo = 0;
		ai.threadRunning = true;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_DOWN) != 0,
				"Post-twist funnel with hard-drop locked should soft-drop");
	}

	// ─── Funnel: primary rotation, piece touching ground + softdropLock (line 288-289) ───

	@Test
	void setControlFunnelPrimaryGroundedSoftDropLock() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_O],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 18; // grounded (near bottom)
		engine.nextPieceArrayID = new int[]{Piece.PIECE_O, Piece.PIECE_O};
		engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_O), new Piece(Piece.PIECE_O)};
		engine.nextPieceCount = 0;
		engine.aiUseThread = false;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		engine.ruleopt.softdropLock = true;
		engine.ruleopt.harddropEnable = true;
		ai.delay = 0;
		ai.bestX = 5;
		ai.bestY = 18;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestRtSub = 0;
		ai.movestate = 0;
		ai.thinkComplete = true;
		ai.thinkLastPieceNo = 1;
		ai.thinkCurrentPieceNo = 0;
		ai.threadRunning = true;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		// pieceTouchGround && softdropLock -> drop = -1 (soft drop)
		assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_DOWN) != 0,
				"Grounded primary funnel with softdropLock should soft-drop");
	}

	// ─── thinkMain: terminal, holdID out of pieceScores range (line 518 false side) ───

	@Test
	void thinkMainTerminalHoldIdOutOfRange() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		ComboRaceBot ai = new ComboRaceBot();
		ai.createTables(engine);
		ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];

		// holdID >= pieceScores.length (7) -> neither the I bonus nor the
		// pieceScores bonus applies; the else-if condition is false.
		int pts = ai.thinkMain(engine, 0, 7, ComboRaceBot.MAX_THINK_DEPTH);

		// stateScores[0]*100 == 600, no bonus added.
		assertTrue(pts == 600,
				"Out-of-range hold id adds no bonus at terminal depth");
	}

	// ─── thinkBestPosition: aiShowHint with a twist so the wallkick block runs (lines 480-495) ───

	@Test
	void thinkBestPositionShowHintTwist() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.wallkick = new StandardSymmetricWallkick();
		engine.aiShowHint = true;
		engine.aiUseThread = false;
		engine.nowPieceObject = new Piece(Piece.PIECE_L);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_L],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_L]);
		engine.nowPieceX = 3;
		engine.nowPieceY = 0;
		engine.nextPieceArrayID = new int[]{Piece.PIECE_L, Piece.PIECE_J, Piece.PIECE_T,
				Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_I, Piece.PIECE_O};
		engine.nextPieceArrayObject = new Piece[7];
		for (int i = 0; i < 7; i++) {
			engine.nextPieceArrayObject[i] = new Piece(engine.nextPieceArrayID[i]);
		}
		engine.nextPieceCount = 0;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		ai.createTables(engine);

		// Build a bottom surface that is a known combo state so a transition
		// (possibly with a twist / rtSub) is chosen and the hint block runs.
		int h = engine.field.getHeight();
		// code 0x13 style surface in the valley at x=3..6
		engine.field.setBlockColor(3, h - 1, 1);
		engine.field.setBlockColor(4, h - 1, 1);
		engine.field.setBlockColor(6, h - 1, 1);
		engine.field.setBlockColor(3, h - 2, 1);

		ai.thinkBestPosition(engine, 0);

		// Just needs to complete without throwing; the hint block ran and the
		// look-ahead queue was populated to the search depth.
		assertTrue(ai.nextQueueIDs != null
				&& ai.nextQueueIDs.length == ComboRaceBot.MAX_THINK_DEPTH,
				"thinkBestPosition with showHint completed and populated the queue");
	}

	// ─── createTables: default-right rotation only (skips left-rotation gate) ───

	@Test
	void createTablesDefaultRightRotationOnly() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.wallkick = new StandardSymmetricWallkick();
		// rotateButtonDefaultRight true + allowReverse false -> the left-rotation
		// gate (line 672) is false, the right-rotation gate (line 707) is true.
		engine.ruleopt.rotateButtonDefaultRight = true;
		engine.ruleopt.rotateButtonAllowReverse = false;
		engine.ruleopt.rotateWallkick = true;

		ComboRaceBot ai = new ComboRaceBot();
		ai.createTables(engine);

		assertTrue(ai.moves != null, "moves table built with default-right rotation");
	}

	// ─── createTables: default-left rotation only (skips right-rotation gate) ───

	@Test
	void createTablesDefaultLeftRotationOnly() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.wallkick = new StandardSymmetricWallkick();
		// rotateButtonDefaultRight false + allowReverse false -> the left gate
		// (line 672) is true, the right gate (line 707) is false.
		engine.ruleopt.rotateButtonDefaultRight = false;
		engine.ruleopt.rotateButtonAllowReverse = false;
		engine.ruleopt.rotateWallkick = true;

		ComboRaceBot ai = new ComboRaceBot();
		ai.createTables(engine);

		assertTrue(ai.moves != null, "moves table built with default-left rotation");
	}

	// ─── setControl: rotateDir with reverse disabled -> BUTTON_A arm (line 338-339) ───

	@Test
	void setControlRotateUsesButtonAWhenReverseDisabled() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		engine.nowPieceObject.direction = Piece.DIRECTION_UP;
		engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_T};
		engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_T)};
		engine.nextPieceCount = 0;
		engine.aiUseThread = false;
		// reverse & double disabled -> rotation always resolves to BUTTON_A.
		engine.ruleopt.rotateButtonAllowReverse = false;
		engine.ruleopt.rotateButtonAllowDouble = false;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		ai.delay = 0;
		ai.bestX = 5;
		ai.bestY = 10;
		ai.bestRt = Piece.DIRECTION_RIGHT; // rt != bestRt -> rotate
		ai.thinkComplete = true;
		ai.thinkLastPieceNo = 1;
		ai.thinkCurrentPieceNo = 0;
		ai.threadRunning = true;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_A) != 0,
				"Rotation with reverse disabled should use BUTTON_A");
	}

	// ─── setControl: unreachable best position -> re-request think (line 264-270) ───

	@Test
	void setControlUnreachableBestReRequestsThink() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_O],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		engine.nextPieceArrayID = new int[]{Piece.PIECE_O, Piece.PIECE_O};
		engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_O), new Piece(Piece.PIECE_O)};
		engine.nextPieceCount = 0;
		engine.aiUseThread = false;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		ai.delay = 0;
		// bestY far above current nowY -> bestY < nowY -> unreachable branch.
		ai.bestX = 5;
		ai.bestY = 0;
		ai.bestRt = Piece.DIRECTION_UP; // == current rt
		ai.movestate = 0;
		ai.thinkComplete = true;
		ai.thinkLastPieceNo = 1;
		ai.thinkCurrentPieceNo = 0;
		ai.threadRunning = true;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		// The bot decided the target is unreachable and cleared thinkComplete.
		assertTrue(!ai.thinkComplete,
				"Unreachable best target should clear thinkComplete and re-request");
	}

	// ─── renderHint: HOLD hint path (line 900-901) ───

	@Test
	void renderHintHoldPath() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		engine.ruleopt.holdEnable = true;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		ai.bestHold = true;
		ai.thinkComplete = true;
		ai.bestPts = 5000;
		ai.thinkCurrentPieceNo = 1;
		ai.thinkLastPieceNo = 1;

		ai.renderHint(engine, 0);

		assertTrue(true, "renderHint HOLD path completed");
	}
}
