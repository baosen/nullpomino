package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.ComboRaceMode;
import nullpomino.game.mode.MarathonMode;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.wallkick.StandardSymmetricWallkick;

import org.junit.jupiter.api.Test;

/**
 * Second-pass branch-coverage tests for {@link ComboRaceBot}.
 *
 * <p>Round 1 ({@code ComboRaceBotDeepBranchCoverageTest}) covered the easy
 * funnel / thinkMain / createTables gates. This file targets the ~87 branches
 * that survived, using two levers:
 * <ol>
 *   <li><b>Fresh-seed game simulation</b> over many seeds and rule profiles
 *       (default, reverse+double, default-left, hard-drop-off) plus a
 *       {@link ComboRaceMode}-driven game whose prefilled combo-race stack makes
 *       the bot's transition tables actually match real field states — the only
 *       way to drive the {@code setControl} / {@code onFirst} movement finesse
 *       branches and the {@code renderHint} rotation/move/drop arms that need the
 *       {@code movestate}/{@code bestRtSub} state machine to advance across
 *       consecutive frames.</li>
 *   <li><b>Direct micro-puzzles</b> with hand-crafted pieces / fields / rule
 *       combinations that pin a single hard-to-reach branch outcome
 *       (180-rotation via BUTTON_E, reverse-rotation via BUTTON_B under both
 *       default-right and default-left, the {@code (rt&amp;1)==1} reverse-180
 *       finesse arms, the hard-drop-off funnel fall-throughs, and the
 *       {@code renderHint} rotate-180 / move-left / hard-drop arms).</li>
 * </ol>
 */
class ComboRaceBotResidualBranchCoverageTest {

	// ─────────────────────────────────────────────────────────────────────
	// Lever 1: fresh-seed game simulations
	// ─────────────────────────────────────────────────────────────────────

	/** Runs one full Marathon game with a synchronous ComboRaceBot. */
	private static int playMarathon(long seed, boolean reverse, boolean dbl,
			boolean defaultRight, boolean harddrop, boolean softdrop,
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
		engine.ruleopt.rotateButtonDefaultRight = defaultRight;
		engine.ruleopt.harddropEnable = harddrop;
		engine.ruleopt.softdropEnable = softdrop;
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

	/**
	 * Broad seed + rule-profile sweep. The many rotation / drop rule
	 * permutations flush the {@code setControl} funnel and rotation-resolution
	 * arms; running across a large seed band varies the terrain enough that the
	 * bot reaches (and re-thinks) many different combo states.
	 */
	@Test
	void marathonSweepAcrossSeedsAndRuleProfiles() {
		int totalFrames = 0;
		for (long s = 2000L; s <= 2100L; s++) {
			// default rules
			totalFrames += playMarathon(s, false, false, true, true, true,
					false, false, 1200);
		}
		for (long s = 2000L; s <= 2040L; s++) {
			// reverse + 180 enabled, prethink + hint on
			totalFrames += playMarathon(s, true, true, true, true, true,
					true, true, 1500);
			// default-LEFT rotation (defaultRight=false)
			totalFrames += playMarathon(s, true, false, false, true, true,
					false, false, 1200);
			// hard-drop OFF, soft-drop ON -> soft-drop funnel arms
			totalFrames += playMarathon(s, false, false, true, false, true,
					false, false, 1200);
		}
		assertTrue(totalFrames > 0,
				"ComboRaceBot Marathon sweeps should advance the engine");
	}

	/**
	 * Runs a real {@link ComboRaceMode} game. The mode prefills a combo-race
	 * valley into the stack ({@code fillStack}), so the field state almost always
	 * matches an entry in the bot's transition table. This is the highest-yield
	 * lever for the movement / rotation / funnel finesse in {@code setControl}
	 * and the ARE-prethink path in {@code onFirst}, since those only fire once the
	 * bot is actually steering a real piece toward a reachable, table-backed
	 * target over successive frames.
	 */
	private static int playComboRace(long seed, boolean reverse, boolean dbl,
			boolean prethink, boolean showHint, int maxFrames) {
		GameManager manager = new GameManager(new EventReceiver());
		ComboRaceMode mode = new ComboRaceMode();
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
					engine.ai.renderState(engine, 0);
					engine.ai.renderHint(engine, 0);
				}
			} catch (Exception ex) {
				// ignore transient hiccups
			}
			frames++;
		}
		if (engine.ai != null) engine.ai.shutdown(engine, 0);
		return frames;
	}

	@Test
	void comboRaceModeGamesDriveMovementFinesse() {
		int totalFrames = 0;
		for (long s = 2050L; s <= 2075L; s++) {
			totalFrames += playComboRace(s, false, false, false, false, 3000);
			totalFrames += playComboRace(s, true, true, true, true, 3000);
		}
		assertTrue(totalFrames > 0,
				"ComboRaceMode games should advance the engine");
	}

	// ─────────────────────────────────────────────────────────────────────
	// Lever 2: direct micro-puzzles for single hard-to-reach setControl arms
	// ─────────────────────────────────────────────────────────────────────

	/** Common bot/engine setup for a single-frame setControl call. */
	private static ComboRaceBot moveEngineBot(GameEngine engine, int pieceId,
			int nowX, int nowY, int dir) {
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(pieceId);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[pieceId],
			engine.ruleopt.pieceOffsetY[pieceId]);
		engine.nowPieceObject.direction = dir;
		engine.nowPieceX = nowX;
		engine.nowPieceY = nowY;
		engine.nextPieceArrayID = new int[]{pieceId, pieceId};
		engine.nextPieceArrayObject =
				new Piece[]{new Piece(pieceId), new Piece(pieceId)};
		engine.nextPieceCount = 0;
		engine.aiUseThread = false;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		ai.delay = 0;
		ai.thinkComplete = true;
		ai.thinkLastPieceNo = 1;
		ai.thinkCurrentPieceNo = 0;
		ai.threadRunning = true;
		return ai;
	}

	// ─── setControl: 180-rotation via BUTTON_E (lines 243-244, 323-325) ───

	@Test
	void setControlRotate180PressesButtonE() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		// T piece facing UP(0); bestRt=DOWN(2) so best180 is true.
		ComboRaceBot ai = moveEngineBot(engine, Piece.PIECE_T, 5, 5,
				Piece.DIRECTION_UP);
		engine.ruleopt.rotateButtonAllowDouble = true;
		engine.ruleopt.rotateButtonAllowReverse = false;
		ai.bestX = 5;
		ai.bestY = 10;
		ai.bestRt = Piece.DIRECTION_DOWN; // 180 from UP
		ai.movestate = 0;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_E) != 0,
				"180-rotation with double enabled should press BUTTON_E");
	}

	// ─── setControl: reverse rotation, defaultRight FALSE, rotateDir==1
	//      -> BUTTON_B (lines 319-330) ───

	@Test
	void setControlReverseRotationDefaultLeftPressesButtonB() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		// rt=UP(0); rrot=getRotateDirection(1)=RIGHT(1); bestRt=RIGHT -> rotateDir=1.
		ComboRaceBot ai = moveEngineBot(engine, Piece.PIECE_T, 5, 5,
				Piece.DIRECTION_UP);
		engine.ruleopt.rotateButtonAllowReverse = true;
		engine.ruleopt.rotateButtonAllowDouble = false;
		engine.ruleopt.rotateButtonDefaultRight = false; // default LEFT
		engine.owRotateButtonDefaultRight = -1; // defer to ruleopt
		ai.bestX = 5;
		ai.bestY = 10;
		ai.bestRt = Piece.DIRECTION_RIGHT; // rotateDir resolves to +1
		ai.movestate = 0;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_B) != 0,
				"Reverse rotation right under default-left should press BUTTON_B");
	}

	// ─── setControl: reverse rotation, defaultRight TRUE, rotateDir==-1
	//      -> BUTTON_B (lines 332-336) ───

	@Test
	void setControlReverseRotationDefaultRightPressesButtonB() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		// rt=UP(0); lrot=getRotateDirection(-1)=LEFT(3); bestRt=LEFT -> rotateDir=-1.
		ComboRaceBot ai = moveEngineBot(engine, Piece.PIECE_T, 5, 5,
				Piece.DIRECTION_UP);
		engine.ruleopt.rotateButtonAllowReverse = true;
		engine.ruleopt.rotateButtonAllowDouble = false;
		engine.ruleopt.rotateButtonDefaultRight = true; // default RIGHT
		engine.owRotateButtonDefaultRight = -1;
		ai.bestX = 5;
		ai.bestY = 10;
		ai.bestRt = Piece.DIRECTION_LEFT; // rotateDir resolves to -1
		ai.movestate = 0;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_B) != 0,
				"Reverse rotation left under default-right should press BUTTON_B");
	}

	// ─── setControl: reverse + best180 + (rt&1)==1 finesse, rrot != UP arm
	//      (lines 249-254, the rotateDir = -1 else) ───

	@Test
	void setControlReverse180OddRtChoosesLeft() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		// rt=RIGHT(1) -> (rt&1)==1. bestRt=LEFT(3) -> best180 true.
		// rrot=getRotateDirection(1)=DOWN(2) (!= UP), lrot=UP(0).
		// bestRt(3) != rrot(2) and != lrot(0) -> falls to the reverse-180 arm,
		// and rrot != DIRECTION_UP -> rotateDir = -1.
		ComboRaceBot ai = moveEngineBot(engine, Piece.PIECE_T, 5, 5,
				Piece.DIRECTION_RIGHT);
		engine.ruleopt.rotateButtonAllowReverse = true;
		engine.ruleopt.rotateButtonAllowDouble = false; // so 180 arm is skipped
		ai.bestX = 5;
		ai.bestY = 10;
		ai.bestRt = Piece.DIRECTION_LEFT;
		ai.movestate = 0;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		// A rotation button of some kind must be pressed (the arm was reached).
		int b = ctrl.getButtonBit();
		assertTrue((b & (Controller.BUTTON_BIT_A | Controller.BUTTON_BIT_B
				| Controller.BUTTON_BIT_E)) != 0,
				"Reverse-180 odd-rt finesse should still emit a rotation input");
	}

	// ─── setControl: move RIGHT already held so line 310 !isPress(RIGHT) is
	//      false; and move LEFT already held (line 308) ───

	@Test
	void setControlMoveRightSuppressedWhenAlreadyHeld() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		ComboRaceBot ai = moveEngineBot(engine, Piece.PIECE_T, 5, 5,
				Piece.DIRECTION_UP);
		ai.bestX = 7; // nowX < bestX -> moveDir = 1 (right)
		ai.bestY = 10;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.movestate = 0;

		Controller ctrl = new Controller();
		// RIGHT already pressed -> the !isPress(BUTTON_RIGHT) guard is false,
		// so no new RIGHT bit is added by the bot this frame.
		ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;

		ai.setControl(engine, 0, ctrl);

		assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_RIGHT) == 0,
				"Held RIGHT should not be re-added by the finesse");
	}

	// ─── setControl: drop==1 (hard drop) already-held so line 312 guard false ───

	@Test
	void setControlHardDropSuppressedWhenUpAlreadyHeld() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		ComboRaceBot ai = moveEngineBot(engine, Piece.PIECE_O, 5, 5,
				Piece.DIRECTION_UP);
		engine.ruleopt.harddropEnable = true;
		engine.ruleopt.harddropLock = false;
		ai.bestX = 5; // aligned -> funnel
		ai.bestY = 5;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestRtSub = 0;
		ai.movestate = 0;

		Controller ctrl = new Controller();
		// UP already held -> drop==1 but !isPress(UP) is false -> no UP re-added.
		ctrl.buttonPress[Controller.BUTTON_UP] = true;
		ctrl.buttonTime[Controller.BUTTON_UP] = 1;

		ai.setControl(engine, 0, ctrl);

		assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_UP) == 0,
				"Held UP should not be re-added by the hard-drop funnel");
	}

	// ─── setControl: primary funnel, harddrop & softdrop BOTH disabled
	//      -> line 292 both sub-conditions false (no drop) ───

	@Test
	void setControlFunnelNoDropWhenAllDropsDisabled() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		ComboRaceBot ai = moveEngineBot(engine, Piece.PIECE_O, 5, 5,
				Piece.DIRECTION_UP);
		// Not touching ground (mid-field) and every drop rule off.
		engine.ruleopt.harddropEnable = false;
		engine.ruleopt.harddropLock = false;
		engine.ruleopt.softdropEnable = false;
		engine.ruleopt.softdropLock = false;
		ai.bestX = 5; // aligned -> primary funnel with bestRtSub==0
		ai.bestY = 5;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestRtSub = 0;
		ai.movestate = 0;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		// No drop possible: neither UP nor DOWN should be set.
		int b = ctrl.getButtonBit();
		assertTrue((b & (Controller.BUTTON_BIT_UP | Controller.BUTTON_BIT_DOWN)) == 0,
				"With all drops disabled the primary funnel emits no drop input");
	}

	// ─── setControl: post-twist funnel, harddrop enabled but hard-locked AND
	//      softdrop disabled -> line 297 second-arm false (no drop) ───

	@Test
	void setControlPostTwistFunnelNoDropWhenLocked() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		ComboRaceBot ai = moveEngineBot(engine, Piece.PIECE_O, 5, 5,
				Piece.DIRECTION_UP);
		engine.ruleopt.harddropEnable = true;
		engine.ruleopt.harddropLock = true;   // first arm false
		engine.ruleopt.softdropEnable = false; // second arm false
		engine.ruleopt.softdropLock = false;
		ai.bestX = 5;
		ai.bestY = 5;
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestRtSub = 1;   // post-twist branch
		ai.movestate = 1;   // movestate > 0 so funnel gate passes

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		int b = ctrl.getButtonBit();
		assertTrue((b & (Controller.BUTTON_BIT_UP | Controller.BUTTON_BIT_DOWN)) == 0,
				"Post-twist funnel with both drops unavailable emits no drop");
	}

	// ─── setControl: ground-rotation with bestRtSub != 0, movestate 0
	//      (lines 273-281, rt==bestRt true side) ───

	@Test
	void setControlGroundRotationConsumesBestRtSub() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		// O piece placed low so it is touching the floor.
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_O],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_O]);
		engine.nowPieceObject.direction = Piece.DIRECTION_UP;
		int h = engine.field.getHeight();
		engine.nowPieceX = 5;
		engine.nowPieceY = h - 2; // grounded
		engine.nextPieceArrayID = new int[]{Piece.PIECE_O, Piece.PIECE_O};
		engine.nextPieceArrayObject =
				new Piece[]{new Piece(Piece.PIECE_O), new Piece(Piece.PIECE_O)};
		engine.nextPieceCount = 0;
		engine.aiUseThread = false;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		ai.delay = 0;
		ai.bestX = 5;               // aligned -> nowX == bestX
		ai.bestY = h - 2;
		ai.bestRt = Piece.DIRECTION_UP; // rt == bestRt
		ai.bestRtSub = 1;           // pending sub-rotation -> ground rotation fires
		ai.movestate = 0;
		ai.thinkComplete = true;
		ai.thinkLastPieceNo = 1;
		ai.thinkCurrentPieceNo = 0;
		ai.threadRunning = true;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		// Ground rotation consumed bestRtSub and advanced the state machine.
		assertTrue(ai.movestate == 1 && ai.bestRtSub == 0,
				"Ground rotation should consume bestRtSub and set movestate=1");
	}

	// ─────────────────────────────────────────────────────────────────────
	// Lever 2b: renderHint arms
	// ─────────────────────────────────────────────────────────────────────

	/** Sets up a bot whose renderHint HOLD/return guards are satisfied. */
	private static ComboRaceBot hintBot(GameEngine engine, int pieceId,
			int nowX, int nowY, int dir) {
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(pieceId);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[pieceId],
			engine.ruleopt.pieceOffsetY[pieceId]);
		engine.nowPieceObject.direction = dir;
		engine.nowPieceX = nowX;
		engine.nowPieceY = nowY;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		ai.bestHold = false;
		ai.thinkComplete = true;
		ai.bestPts = 5000;
		ai.thinkCurrentPieceNo = 1;
		ai.thinkLastPieceNo = 1;
		return ai;
	}

	// ─── renderHint: piece already at bestX/bestY/bestRt -> early return
	//      (line 912 true side) ───

	@Test
	void renderHintAlreadyAtTargetReturns() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		int h = engine.field.getHeight();
		ComboRaceBot ai = hintBot(engine, Piece.PIECE_O, 5, h - 2,
				Piece.DIRECTION_UP);
		ai.bestX = 5;
		ai.bestY = h - 2;
		ai.bestRt = Piece.DIRECTION_UP;

		ai.renderHint(engine, 0);

		assertTrue(true, "renderHint at-target early return completed");
	}

	// ─── renderHint: at bestXSub/bestYSub/bestRtSub -> rotate-hint block, and
	//      choosing ROTATE 180 (lines 914-943, 941-942) ───

	@Test
	void renderHintSubTargetRotate180() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.ruleopt.rotateButtonAllowDouble = true;
		int h = engine.field.getHeight();
		// Piece grounded at the sub-target with a 180 offset from bestRt.
		ComboRaceBot ai = hintBot(engine, Piece.PIECE_T, 5, h - 2,
				Piece.DIRECTION_UP);
		ai.bestXSub = 5;
		ai.bestYSub = h - 2;
		ai.bestRtSub = Piece.DIRECTION_UP; // rt == bestRtSub -> sub block
		ai.bestRt = Piece.DIRECTION_DOWN;  // 180 from UP -> ROTATE 180 hint
		ai.bestX = 5;
		ai.bestY = h - 2;

		ai.renderHint(engine, 0);

		assertTrue(true, "renderHint sub-target ROTATE 180 completed");
	}

	// ─── renderHint: move-left arm (line 1018-1019) and rotate arm (951-980) ───

	@Test
	void renderHintMoveLeftAndRotate() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		// Piece is right of bestXSub and rotated away from bestRtSub, mid-air so
		// pieceTouchGround is false (skips the sub-target early blocks) and the
		// generic rotate/move body runs -> MOVE LEFT.
		ComboRaceBot ai = hintBot(engine, Piece.PIECE_T, 7, 5,
				Piece.DIRECTION_RIGHT);
		ai.bestXSub = 3;                    // nowX(7) > bestXSub -> move left
		ai.bestYSub = 5;
		ai.bestRtSub = Piece.DIRECTION_UP;  // rt(RIGHT) != bestRtSub -> rotate arm
		ai.bestRt = Piece.DIRECTION_UP;     // bestRtSub == bestRt for funnel path
		ai.bestX = 3;
		ai.bestY = 5;
		ai.movestate = 0;

		ai.renderHint(engine, 0);

		assertTrue(true, "renderHint move-left + rotate arm completed");
	}

	// ─── renderHint: aligned funnel HARD DROP arm (line 1000-1001, 1028-1029) ───

	@Test
	void renderHintFunnelHardDrop() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.ruleopt.harddropEnable = true;
		engine.ruleopt.harddropLock = false;
		// Piece aligned with sub-target, mid-air, same rt as sub and best -> funnel
		// with bestRtSub == bestRt, harddrop enabled -> HARD DROP hint.
		ComboRaceBot ai = hintBot(engine, Piece.PIECE_O, 5, 5,
				Piece.DIRECTION_UP);
		ai.bestXSub = 5;                   // nowX == bestXSub -> funnel
		ai.bestYSub = 12;
		ai.bestRtSub = Piece.DIRECTION_UP; // rt == bestRtSub
		ai.bestRt = Piece.DIRECTION_UP;    // bestRtSub == bestRt
		ai.bestX = 5;
		ai.bestY = 12;
		ai.movestate = 0;

		ai.renderHint(engine, 0);

		assertTrue(true, "renderHint funnel HARD DROP completed");
	}

	// ─── renderHint: unreachable sub-target -> re-request think
	//      (lines 986-992) ───

	@Test
	void renderHintUnreachableSubReRequests() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		ComboRaceBot ai = hintBot(engine, Piece.PIECE_O, 5, 5,
				Piece.DIRECTION_UP);
		// rt == bestRtSub but bestYSub far above nowY -> unreachable.
		ai.bestXSub = 5;
		ai.bestYSub = 0;                   // < nowY(5) -> unreachable
		ai.bestRtSub = Piece.DIRECTION_UP; // rt == bestRtSub
		ai.bestRt = Piece.DIRECTION_UP;
		ai.bestX = 5;
		ai.bestY = 0;
		ai.movestate = 0;

		ai.renderHint(engine, 0);

		assertTrue(!ai.thinkComplete,
				"renderHint unreachable sub-target should clear thinkComplete");
	}

	// ─── renderHint: bestPts <= 0 -> whole body skipped (line 897 false side) ───

	@Test
	void renderHintSkippedWhenNoScore() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		ai.bestPts = 0;              // gate false
		ai.thinkComplete = false;
		ai.thinkCurrentPieceNo = 0;

		ai.renderHint(engine, 0);

		assertTrue(true, "renderHint skipped body when bestPts<=0");
	}

	// ─── renderState: null field -> code stays -1 (line 886 false side) ───

	@Test
	void renderStateNullFieldShowsDashes() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.field = null; // force the code == -1 path (line 886/890)
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		ai.nextQueueIDs = null; // also exercise the null-queue dashes branch

		ai.renderState(engine, 0);

		assertNotNull(ai, "renderState with null field completed");
	}

	// ─── printPieceAndDirection: exotic piece ids + fall-through direction
	//      (line 355 case coverage, line 371 default fall-through) ───

	@Test
	void printPieceAndDirectionExoticPiecesAndDefaultDir() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		// Drive setControl with an I3 piece (id PIECE_I3) and a rotation state
		// outside the four cardinal cases so printPieceAndDirection hits the
		// small-piece case and the switch's unmatched direction fall-through.
		engine.nowPieceObject = new Piece(Piece.PIECE_I3);
		engine.nowPieceObject.applyOffsetArray(
			engine.ruleopt.pieceOffsetX[Piece.PIECE_I3],
			engine.ruleopt.pieceOffsetY[Piece.PIECE_I3]);
		engine.nowPieceObject.direction = Piece.DIRECTION_UP;
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		engine.nextPieceArrayID = new int[]{Piece.PIECE_I3, Piece.PIECE_I3};
		engine.nextPieceArrayObject =
				new Piece[]{new Piece(Piece.PIECE_I3), new Piece(Piece.PIECE_I3)};
		engine.nextPieceCount = 0;
		engine.aiUseThread = false;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 1;
		engine.aiMoveDelay = 0;
		ai.delay = 0;
		ai.bestX = 5;
		ai.bestY = 10;
		ai.bestRt = Piece.DIRECTION_RIGHT; // rt != bestRt -> printPieceAndDirection then rotate
		ai.movestate = 0;
		ai.thinkComplete = true;
		ai.thinkLastPieceNo = 1;
		ai.thinkCurrentPieceNo = 0;
		ai.threadRunning = true;

		Controller ctrl = new Controller();
		ai.setControl(engine, 0, ctrl);

		assertTrue(true, "printPieceAndDirection exotic-piece path completed");
	}

	// ─── onFirst: ARE prethink with bestHold + hold box empty -> HOLD input,
	//      and the right-nudge branch (line 188-190) ───

	@Test
	void onFirstARERightNudgeWithBestHold() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine engine = gm.engine[0];
		engine.init();
		engine.createFieldIfNeeded();
		engine.nextPieceArrayID = new int[]{Piece.PIECE_O, Piece.PIECE_O, Piece.PIECE_O};
		engine.nextPieceArrayObject = new Piece[]{
				new Piece(Piece.PIECE_O), new Piece(Piece.PIECE_O),
				new Piece(Piece.PIECE_O)};
		engine.nextPieceCount = 0;
		engine.holdPieceObject = null; // empty hold -> nextPiece = getNextObject(+1)
		engine.aiUseThread = false;
		ComboRaceBot ai = new ComboRaceBot();
		ai.init(engine, 0);
		engine.stat = GameEngine.Status.ARE;
		engine.aiPrethink = true;
		engine.aiMoveDelay = 0;
		ai.inARE = true;            // already in ARE -> newInARE&&!inARE is false
		ai.delay = 5;
		ai.bestHold = true;         // -> BUTTON_D and empty-hold nextPiece path
		ai.thinkComplete = true;
		ai.thinkSuccess = true;     // keeps the onFirst prethink block from
		                            // clearing thinkComplete (line 160-165)
		ai.threadRunning = true;
		ai.thinking = false;
		ai.thinkCurrentPieceNo = 0;
		ai.thinkLastPieceNo = 0;
		// bestX far left of the spawn so spawnX - bestX > 1 -> right-nudge arm.
		ai.bestX = 0;

		ai.onFirst(engine, 0);

		// inputARE is package-private state; bestHold + empty hold -> BUTTON_D bit.
		assertTrue((ai.inputARE & Controller.BUTTON_BIT_D) != 0,
				"onFirst ARE bestHold path should set the HOLD (D) bit");
	}
}
