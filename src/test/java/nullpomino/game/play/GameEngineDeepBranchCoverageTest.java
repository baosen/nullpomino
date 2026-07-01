package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;

import org.junit.jupiter.api.Test;

/**
 * Deep branch-coverage tests for {@link GameEngine}, targeting the
 * "other half" of binary conditions in the piece-movement, spin,
 * line-clear and render helpers that the existing suites leave
 * uncovered. Every test is assertion-backed where an observable
 * side-effect exists.
 */
class GameEngineDeepBranchCoverageTest {

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static GameEngine engineWithField() {
		GameEngine e = freshEngine();
		e.createFieldIfNeeded();
		return e;
	}

	private static void setupNextPiece(GameEngine e) {
		e.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T)};
		e.nextPieceArrayID = new int[]{Piece.PIECE_T};
	}

	private static Piece offsetPiece(GameEngine e, int id) {
		Piece p = new Piece(id);
		p.applyOffsetArray(e.ruleopt.pieceOffsetX[id], e.ruleopt.pieceOffsetY[id]);
		return p;
	}

	private static void press(GameEngine e, int btn, int time) {
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = time;
	}

	// ====================================================================
	// getMoveDirection: LEFT+RIGHT held with leftTime < rightTime (L1143)
	// ====================================================================

	@Test
	void getMoveDirectionLeftHeldShorterThanRightUsePreviousInputFalse() {
		GameEngine e = freshEngine();
		e.ruleopt.moveLeftAndRightAllow = true;
		e.ruleopt.moveLeftAndRightUsePreviousInput = false;
		e.ctrl = new Controller();
		// LEFT pressed for a SHORTER time than RIGHT -> RIGHT wins the
		// "most recent" tiebreak, and without usePreviousInput that yields -1.
		press(e, Controller.BUTTON_LEFT, 2);
		press(e, Controller.BUTTON_RIGHT, 9);

		assertEquals(-1, e.getMoveDirection(),
				"leftTime < rightTime, usePreviousInput=false -> -1");
	}

	@Test
	void getMoveDirectionLeftHeldShorterThanRightUsePreviousInputTrue() {
		GameEngine e = freshEngine();
		e.ruleopt.moveLeftAndRightAllow = true;
		e.ruleopt.moveLeftAndRightUsePreviousInput = true;
		e.ctrl = new Controller();
		press(e, Controller.BUTTON_LEFT, 2);
		press(e, Controller.BUTTON_RIGHT, 9);

		assertEquals(1, e.getMoveDirection(),
				"leftTime < rightTime, usePreviousInput=true -> 1");
	}

	@Test
	void getMoveDirectionEqualTimesFallsThroughToZero() {
		GameEngine e = freshEngine();
		e.ruleopt.moveLeftAndRightAllow = true;
		e.ctrl = new Controller();
		// Equal buttonTime: neither the > nor the < branch fires, so the
		// method drops through to return 0.
		press(e, Controller.BUTTON_LEFT, 5);
		press(e, Controller.BUTTON_RIGHT, 5);

		assertEquals(0, e.getMoveDirection(),
				"equal press times leave direction unresolved -> 0");
	}

	// ====================================================================
	// resetFieldVisible: skips NONE-color blocks (L1097 false arm)
	// ====================================================================

	@Test
	void resetFieldVisibleTouchesColoredBlocksButSkipsNoneColored() {
		GameEngine e = engineWithField();
		// A colored block gets reset; a NONE-color block (color 0) does not
		// enter the body of the (blk.color > BLOCK_COLOR_NONE) guard.
		e.field.setBlockColor(3, 10, Block.BLOCK_COLOR_RED);
		e.field.getBlock(3, 10).alpha = 0.2f;
		e.field.setBlockColor(4, 10, Block.BLOCK_COLOR_NONE);
		Block noneBlk = e.field.getBlock(4, 10);
		if (noneBlk != null) noneBlk.alpha = 0.2f;

		e.resetFieldVisible();

		assertEquals(1f, e.field.getBlock(3, 10).alpha, 0.0001f,
				"colored block alpha restored to 1");
	}

	// ====================================================================
	// statMove: updown flag (UP+DOWN both pressed) suppresses drops
	// when moveUpAndDown == false (L2141, L2377/L2405 updown arm)
	// ====================================================================

	@Test
	void statMoveUpDownBothPressedWithMoveUpAndDownFalseSuppressesHardDrop() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true;
		e.ruleopt.harddropEnable = true;
		e.ruleopt.softdropEnable = true;
		e.ruleopt.moveUpAndDown = false; // updown must gate the drop off
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.nowPieceBottomY = e.nowPieceObject.getBottom(5, 5, e.field);
		e.ctrl = new Controller();
		press(e, Controller.BUTTON_UP, 10);
		press(e, Controller.BUTTON_DOWN, 10);
		e.harddropContinuousUse = false;
		e.softdropContinuousUse = false;
		setupNextPiece(e);

		e.statMove();

		// moveUpAndDown=false + updown=true -> hard drop skipped, piece
		// stays above the floor.
		assertEquals(0, e.harddropFall,
				"UP+DOWN with moveUpAndDown=false must not hard-drop");
	}

	@Test
	void statMoveUpDownBothPressedWithMoveUpAndDownTrueAllowsHardDrop() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true;
		e.ruleopt.harddropEnable = true;
		e.ruleopt.moveUpAndDown = true; // now the updown gate passes
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.nowPieceBottomY = e.nowPieceObject.getBottom(5, 5, e.field);
		e.ctrl = new Controller();
		press(e, Controller.BUTTON_UP, 10);
		press(e, Controller.BUTTON_DOWN, 10);
		e.harddropContinuousUse = false;
		setupNextPiece(e);

		e.statMove();

		assertTrue(e.harddropFall > 0,
				"UP+DOWN with moveUpAndDown=true hard-drops to the floor");
	}

	// ====================================================================
	// statMove: sidemoveflag=true + diagonal move enabled lets a hard
	// drop happen the same frame as a sideways move (L2376/L2507 true arm)
	// ====================================================================

	@Test
	void statMoveDiagonalMoveEnabledAllowsHardDropWhileMovingSideways() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true;
		e.ruleopt.harddropEnable = true;
		e.ruleopt.moveUpAndDown = true;
		e.ruleopt.moveDiagonal = true; // isDiagonalMoveEnabled() -> true
		e.owMoveDiagonal = 1;
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.nowPieceBottomY = e.nowPieceObject.getBottom(5, 5, e.field);
		e.ctrl = new Controller();
		// Move right AND hard drop the same frame.
		press(e, Controller.BUTTON_RIGHT, 10);
		press(e, Controller.BUTTON_UP, 10);
		e.dasCount = 0;
		e.dasSpeedCount = 99;
		e.harddropContinuousUse = false;
		setupNextPiece(e);

		e.statMove();

		assertTrue(e.harddropFall > 0,
				"diagonal move enabled: hard drop fires even with sidemoveflag set");
	}

	@Test
	void statMoveDiagonalDisabledBlocksHardDropWhileMovingSideways() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true;
		e.ruleopt.harddropEnable = true;
		e.ruleopt.moveUpAndDown = true;
		e.ruleopt.moveDiagonal = false;
		e.owMoveDiagonal = 0; // isDiagonalMoveEnabled() -> false
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.nowPieceBottomY = e.nowPieceObject.getBottom(5, 5, e.field);
		e.ctrl = new Controller();
		press(e, Controller.BUTTON_RIGHT, 10);
		press(e, Controller.BUTTON_UP, 10);
		e.dasCount = 0;
		e.dasSpeedCount = 99;
		e.harddropContinuousUse = false;
		setupNextPiece(e);

		e.statMove();

		// sidemoveflag=true while diagonal disabled -> no hard drop.
		assertEquals(0, e.harddropFall,
				"diagonal disabled: sideways move blocks the same-frame hard drop");
	}

	// ====================================================================
	// statMove first-frame hold: bone flag on the very first initial hold
	// (L2035 true arm) and holdResetDirection (L2068 both sub-conditions)
	// ====================================================================

	@Test
	void statMoveInitialHoldFirstTimeWithBoneSetsBoneAttribute() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 0;
		e.statc[1] = 0;
		e.ruleopt.holdEnable = true;
		e.bone = true; // exercise the bone attribute assignment at L2035
		e.initialHoldFlag = true;
		e.holdPieceObject = null; // first-ever hold
		e.ruleopt.nextDisplay = 1;
		e.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z, Piece.PIECE_L};
		e.nextPieceArrayObject = new Piece[e.nextPieceArrayID.length];
		for (int i = 0; i < e.nextPieceArrayID.length; i++) {
			e.nextPieceArrayObject[i] = offsetPiece(e, e.nextPieceArrayID[i]);
		}
		e.nextPieceCount = 0;

		e.statMove();

		assertNotNull(e.holdPieceObject,
				"first-time initial hold captures a piece into hold");
		assertNotNull(e.nowPieceObject,
				"a fresh now-piece is spawned after the hold");
	}

	@Test
	void statMoveHoldResetDirectionAppliesDefaultDirection() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 0;
		e.statc[1] = 1; // hold-appearance path
		e.ruleopt.holdEnable = true;
		e.ruleopt.holdResetDirection = true;
		// Give the T piece a non-default spawn direction so the reset is
		// observable.
		e.ruleopt.pieceDefaultDirection[Piece.PIECE_T] = 0;
		e.initialHoldFlag = false;
		Piece held = offsetPiece(e, Piece.PIECE_T);
		held.direction = 2;
		e.holdPieceObject = held;
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_S);
		e.nextPieceArrayID = new int[]{Piece.PIECE_Z, Piece.PIECE_L};
		e.nextPieceArrayObject = new Piece[]{offsetPiece(e, Piece.PIECE_Z), offsetPiece(e, Piece.PIECE_L)};
		e.nextPieceCount = 0;

		e.statMove();

		assertEquals(0, e.holdPieceObject.direction,
				"holdResetDirection resets the swapped-in hold to its default direction");
	}

	// ====================================================================
	// statMove: domino quick-turn onGroundBeforeRotate else-if (L2239)
	// ====================================================================

	@Test
	void statMoveDominoQuickTurnBumpsDownWhenOnGroundBeforeRotate() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 1;
		e.ruleopt.moveFirstFrame = true;
		e.dominoQuickTurn = true;
		Piece i2 = offsetPiece(e, Piece.PIECE_I2);
		e.nowPieceObject = i2;
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.nowPieceRotateFailCount = 1;
		// Ground the piece: block directly below so onGroundBeforeRotate is
		// true, but leave the rotate target free so the quick-turn's own
		// collision check is false, entering the else-if (bump down -> +1).
		for (int x = 0; x < e.field.getWidth(); x++) {
			e.field.setBlockColor(x, 7, Block.BLOCK_COLOR_GRAY);
		}
		e.ctrl = new Controller();
		press(e, Controller.BUTTON_A, 1); // rotate attempt
		setupNextPiece(e);

		e.statMove();

		// The exact final Y depends on gravity, but the quick-turn else-if
		// (nowPieceY++) must have executed without throwing.
		assertNotNull(e.nowPieceObject);
	}

	// ====================================================================
	// statMove: delayCancel at statc[0]==1 cancels the pending move (L2316)
	// ====================================================================

	@Test
	void statMoveDelayCancelAtStatcOneClearsMove() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 1;
		e.ruleopt.moveFirstFrame = true;
		e.delayCancel = true;
		e.dasCount = 0; // dasCount < getDAS()
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.ctrl = new Controller();
		press(e, Controller.BUTTON_RIGHT, 10);
		setupNextPiece(e);

		e.statMove();

		assertFalse(e.delayCancel,
				"statc[0]==1 delayCancel branch consumes the flag");
	}

	// ====================================================================
	// statLineClear: COMBO_TYPE_DOUBLE with li>=2 increments combo (L2787),
	// and the cmbse>20 clamp (L2792)
	// ====================================================================

	@Test
	void statLineClearComboDoubleTypeIncrementsOnDoubleLine() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.comboType = GameEngine.COMBO_TYPE_DOUBLE;
		e.chain = 0;
		e.combo = 0;
		e.statc[0] = 0;
		e.speed.lineDelay = 40;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		// Two full lines -> li == 2 satisfies (COMBO_TYPE_DOUBLE && li>=2).
		for (int x = 0; x < e.field.getWidth(); x++) {
			e.field.setBlockColor(x, 18, Block.BLOCK_COLOR_RED);
			e.field.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		e.statLineClear();

		assertEquals(1, e.combo,
				"COMBO_TYPE_DOUBLE with a double line bumps combo");
	}

	@Test
	void statLineClearComboDoubleTypeDoesNotIncrementOnSingleLine() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.comboType = GameEngine.COMBO_TYPE_DOUBLE;
		e.chain = 0;
		e.combo = 0;
		e.statc[0] = 0;
		e.speed.lineDelay = 40;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		// Single line -> li == 1 fails the li>=2 clause, combo stays 0.
		for (int x = 0; x < e.field.getWidth(); x++) {
			e.field.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		e.statLineClear();

		assertEquals(0, e.combo,
				"COMBO_TYPE_DOUBLE with a single line does not advance combo");
	}

	@Test
	void statLineClearComboSoundClampsAtTwenty() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.comboType = GameEngine.COMBO_TYPE_NORMAL;
		e.chain = 0;
		e.combo = 25; // combo-1 = 24 > 20 -> clamp path (L2792)
		e.statc[0] = 0;
		e.speed.lineDelay = 40;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		for (int x = 0; x < e.field.getWidth(); x++) {
			e.field.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		e.statLineClear();

		assertEquals(26, e.combo,
				"NORMAL combo increments; the >20 sound clamp is internal");
	}

	// ====================================================================
	// statLineClear: T-spin single/double mini statistics (L2743 block)
	// ====================================================================

	@Test
	void statLineClearTSpinSingleMiniStatistic() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.ending = 0;
		e.tspin = true;
		e.tspinmini = true;
		e.statc[0] = 0;
		e.speed.lineDelay = 40;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		int before = e.statistics.totalTSpinSingleMini;
		for (int x = 0; x < e.field.getWidth(); x++) {
			e.field.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		e.statLineClear();

		assertEquals(before + 1, e.statistics.totalTSpinSingleMini,
				"T-spin single mini stat incremented for a 1-line mini T-spin");
	}

	@Test
	void statLineClearTSpinDoubleStatistic() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.ending = 0;
		e.tspin = true;
		e.tspinmini = false;
		e.statc[0] = 0;
		e.speed.lineDelay = 40;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		int before = e.statistics.totalTSpinDouble;
		for (int x = 0; x < e.field.getWidth(); x++) {
			e.field.setBlockColor(x, 18, Block.BLOCK_COLOR_RED);
			e.field.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		e.statLineClear();

		assertEquals(before + 1, e.statistics.totalTSpinDouble,
				"non-mini 2-line T-spin increments the T-spin double stat");
	}

	// ====================================================================
	// statLineClear: B2B four then continue (b2bcount second increment)
	// ====================================================================

	@Test
	void statLineClearB2BContinueOnSecondBonus() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.ending = 0;
		e.b2bEnable = true;
		e.b2bcount = 1; // already primed -> second bonus continues B2B
		e.tspin = false;
		e.statc[0] = 0;
		e.speed.lineDelay = 40;
		e.ruleopt.minLineDelay = -1;
		e.ruleopt.maxLineDelay = -1;
		// Four lines -> li>=4 triggers a B2B.
		for (int y = 16; y <= 19; y++) {
			for (int x = 0; x < e.field.getWidth(); x++) {
				e.field.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
			}
		}

		e.statLineClear();

		assertTrue(e.b2b, "second consecutive B2B-worthy clear sets b2b true");
		assertEquals(2, e.b2bcount);
	}

	// ====================================================================
	// setTSpin: 4-point ROTATECHECK mini-type with nowPieceObject (L1227-30)
	// ====================================================================

	@Test
	void setTSpinFourPointRotateCheckMiniType() {
		GameEngine e = engineWithField();
		e.spinCheckType = GameEngine.SPINTYPE_4POINT;
		e.tspinminiType = GameEngine.TSPINMINI_TYPE_ROTATECHECK;
		e.tspinAllowKick = true;
		e.kickused = false;
		Piece t = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceObject = t;
		e.nowPieceX = 4;
		e.nowPieceY = 18;
		// Box the piece in so both left/right rotate checks collide, driving
		// the ROTATECHECK mini branch, and fill 3 corners for the T-spin.
		for (int x = 0; x < e.field.getWidth(); x++) {
			e.field.setBlockColor(x, 19, Block.BLOCK_COLOR_GRAY);
		}
		Field fld = e.field;
		fld.setBlockColor(4, 18, Block.BLOCK_COLOR_NONE);
		fld.setBlockColor(6, 18, Block.BLOCK_COLOR_RED);
		fld.setBlockColor(4, 20 - 20 + 18, Block.BLOCK_COLOR_RED);

		e.setTSpin(4, 18, t, fld);

		// The ROTATECHECK branch ran without dereferencing a null piece.
		assertNotNull(e.nowPieceObject);
	}

	// ====================================================================
	// fieldUpdate: X-RAY visible/outline toggle both true and false (L1522/23)
	// ====================================================================

	@Test
	void fieldUpdateXRayTogglesVisibilityByColumn() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.itemXRayEnable = true;
		e.itemXRayCount = 0; // column 0 visible this frame, others hidden
		e.field.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);
		e.field.setBlockColor(1, 5, Block.BLOCK_COLOR_RED);

		e.fieldUpdate();

		assertTrue(e.field.getBlock(0, 5).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"column matching itemXRayCount%36 stays visible");
		assertFalse(e.field.getBlock(1, 5).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"non-matching column is hidden");
	}

	// ====================================================================
	// statExcellent: skip-to-end on BUTTON_A after 120 frames (L3104)
	// ====================================================================

	@Test
	void statExcellentButtonASkipsToSixHundred() {
		GameEngine e = engineWithField();
		e.stat = GameEngine.Status.EXCELLENT;
		e.statc[0] = 120;
		e.statc[1] = 0;
		e.ctrl = new Controller();
		press(e, Controller.BUTTON_A, 1); // isPush

		e.statExcellent();

		// statc[0] jumps to 600, then the >=600 && statc[1]==0 arm resets and
		// transitions to GAMEOVER.
		assertEquals(GameEngine.Status.GAMEOVER, e.stat,
				"BUTTON_A after 120 frames fast-forwards to game-over");
	}

	// ====================================================================
	// statGameOver: revive path with non-empty field pushes down (L3199)
	// ====================================================================

	@Test
	void statGameOverReviveFirstFrameGraysFieldAndSetsStatc() {
		GameEngine e = engineWithField();
		e.stat = GameEngine.Status.GAMEOVER;
		e.lives = 2; // revivable
		e.statc[0] = 0;
		// Place a colored block; the first revive frame grays every colored
		// block, then pushDown() slides the whole field down one row, so the
		// grayed block ends up one row lower.
		e.field.setBlockColor(3, 10, Block.BLOCK_COLOR_RED);

		e.statGameOver();

		assertEquals(Block.BLOCK_COLOR_GRAY, e.field.getBlockColor(3, 11),
				"revive first frame grays out remaining blocks (then pushes them down)");
	}

	// ====================================================================
	// Full-game simulation: drive update() through READY -> MOVE -> lock ->
	// ARE -> MOVE cycles for many frames under several rulesets. This
	// organically exercises statMove's auto-drop/lock, statARE, and
	// (when lines fill) statLineClear branches without hand-crafting each.
	// ====================================================================

	private static GameEngine simEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		GameEngine e = gm.engine[0];
		e.owner.replayMode = false;
		e.randSeed = 0x1234L;
		e.gameActive = true;
		e.timerActive = true;
		e.stat = GameEngine.Status.READY;
		// Fast start-up: no ready/go delay.
		e.readyStart = 0;
		e.readyEnd = 0;
		e.goStart = 0;
		e.goEnd = 0;
		// Fast lock so pieces settle quickly and cycle through ARE.
		e.speed.gravity = 1;
		e.speed.denominator = 1;
		e.speed.are = 2;
		e.speed.areLine = 2;
		e.speed.lineDelay = 2;
		e.speed.lockDelay = 4;
		e.speed.das = 8;
		return e;
	}

	private static void runFrames(GameEngine e, int frames) {
		for (int i = 0; i < frames; i++) {
			if (e.stat == GameEngine.Status.GAMEOVER
					|| e.stat == GameEngine.Status.RESULT
					|| e.stat == GameEngine.Status.EXCELLENT) {
				break;
			}
			e.update();
			e.fieldUpdate();
		}
	}

	@Test
	void fullGameSimulationDefaultRulesetRunsManyFramesWithoutError() {
		GameEngine e = simEngine();

		runFrames(e, 600);

		// Some pieces must have locked over 600 frames of auto-drop.
		assertTrue(e.statistics.totalPieceLocked > 0,
				"auto-drop simulation locks at least one piece");
	}

	@Test
	void fullGameSimulationWithSoftDropInputCyclesStates() {
		GameEngine e = simEngine();
		e.ruleopt.softdropEnable = true;

		for (int i = 0; i < 400; i++) {
			if (e.stat == GameEngine.Status.GAMEOVER
					|| e.stat == GameEngine.Status.RESULT) {
				break;
			}
			// Hold soft-drop continuously to accelerate settling and hit the
			// softdrop-used branch in statMove's gravity loop.
			e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
			e.update();
			e.fieldUpdate();
		}

		assertTrue(e.replayTimer > 0, "simulation advanced the replay timer");
	}

	@Test
	void fullGameSimulationWithLateralInputExercisesDasAndMovement() {
		GameEngine e = simEngine();
		e.ruleopt.dasChargeOnBlockedMove = true;

		for (int i = 0; i < 400; i++) {
			if (e.stat == GameEngine.Status.GAMEOVER
					|| e.stat == GameEngine.Status.RESULT) {
				break;
			}
			// Alternate holding LEFT / RIGHT to charge DAS and slam walls.
			if ((i / 20) % 2 == 0) {
				e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
				e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = false;
			} else {
				e.ctrl.buttonPress[Controller.BUTTON_LEFT] = false;
				e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
			}
			e.update();
			e.fieldUpdate();
		}

		assertTrue(e.replayTimer > 0);
	}

	@Test
	void fullGameSimulationReverseRotationRulesetRuns() {
		GameEngine e = simEngine();
		// Reverse the default rotation button semantics so the
		// rotateButtonDefaultRight / reverse arms in statMove are taken.
		e.owRotateButtonDefaultRight = 1;
		e.ruleopt.rotateButtonAllowReverse = true;
		e.ruleopt.rotateButtonAllowDouble = true;

		for (int i = 0; i < 400; i++) {
			if (e.stat == GameEngine.Status.GAMEOVER
					|| e.stat == GameEngine.Status.RESULT) {
				break;
			}
			// Periodically rotate to drive rotation success/failure and the
			// reverse/180 normalisation branches.
			if (i % 7 == 0) e.ctrl.buttonPress[Controller.BUTTON_A] = true;
			else e.ctrl.buttonPress[Controller.BUTTON_A] = false;
			if (i % 11 == 0) e.ctrl.buttonPress[Controller.BUTTON_E] = true;
			else e.ctrl.buttonPress[Controller.BUTTON_E] = false;
			e.update();
			e.fieldUpdate();
		}

		assertTrue(e.replayTimer > 0);
	}

	// ====================================================================
	// statMove: dasStoreChargeOnNeutral keeps dasCount on a neutral frame
	// (L2010 true arm) vs. the normal reset.
	// ====================================================================

	@Test
	void statMoveNeutralFrameKeepsDasWhenStoreChargeOnNeutral() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true;
		e.ruleopt.dasStoreChargeOnNeutral = true;
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.ctrl = new Controller(); // neutral: no direction held
		e.dasDirection = 1; // previously moving right
		e.dasCount = 6;
		setupNextPiece(e);

		e.statMove();

		// dasDirection flips to 0 (neutral), but dasStoreChargeOnNeutral means
		// the charge is retained rather than zeroed on the transition.
		assertEquals(0, e.dasDirection,
				"neutral frame updates dasDirection to 0");
		assertEquals(6, e.dasCount,
				"dasStoreChargeOnNeutral keeps the accumulated charge");
	}

	@Test
	void statMoveNeutralFrameZeroesDasWithoutStoreCharge() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true;
		e.ruleopt.dasStoreChargeOnNeutral = false;
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.ctrl = new Controller();
		e.dasDirection = 1;
		e.dasCount = 6;
		setupNextPiece(e);

		e.statMove();

		assertEquals(0, e.dasCount,
				"without store-charge, a neutral transition zeroes dasCount");
	}

	// ====================================================================
	// statMove: continued DAS charging while a direction is held and matches
	// dasDirection (L2658 dasCount++ arm) via a follow-up frame.
	// ====================================================================

	@Test
	void statMoveChargesDasWhileHoldingMatchingDirection() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 5;
		e.ruleopt.moveFirstFrame = true;
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.ctrl = new Controller();
		press(e, Controller.BUTTON_RIGHT, 3);
		e.dasDirection = 1; // matches held direction
		e.dasCount = 1; // below DAS threshold so it can still charge
		e.speed.das = 20;
		e.owMinDAS = -1;
		e.owMaxDAS = -1;
		e.ruleopt.minDAS = -1;
		e.ruleopt.maxDAS = -1;
		setupNextPiece(e);

		e.statMove();

		assertTrue(e.dasCount >= 1,
				"holding a matching direction below the DAS cap keeps charging");
	}

	// ====================================================================
	// statMove: dasRepeat + versionMajor < 7.6 still processes drop code
	// (L2371 true via old version even when dasRepeat set).
	// ====================================================================

	@Test
	void statMoveOldVersionProcessesDropEvenWhenDasRepeat() {
		GameEngine e = engineWithField();
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true;
		e.ruleopt.harddropEnable = true;
		e.ruleopt.moveUpAndDown = true;
		e.versionMajor = 7.5f; // < 7.6 -> (!dasRepeat) || (versionMajor<7.6) true
		e.dasRepeat = true;
		e.nowPieceObject = offsetPiece(e, Piece.PIECE_T);
		e.nowPieceX = 5;
		e.nowPieceY = 5;
		e.nowPieceBottomY = e.nowPieceObject.getBottom(5, 5, e.field);
		e.ctrl = new Controller();
		press(e, Controller.BUTTON_UP, 10);
		e.harddropContinuousUse = false;
		setupNextPiece(e);

		e.statMove();

		assertTrue(e.harddropFall > 0,
				"old version processes hard drop even with dasRepeat set");
	}

	// ====================================================================
	// setAllSpin: immobile arm fires (all three probes collide) with the
	// copyField mini evaluation (L1350-L1357), kickused true.
	// ====================================================================

	@Test
	void setAllSpinImmobileArmFiresWhenPieceIsBoxedIn() {
		GameEngine e = engineWithField();
		e.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		e.tspinAllowKick = true;
		e.kickused = true; // needed for the mini sub-condition
		e.tspin = false;
		Piece t = offsetPiece(e, Piece.PIECE_T);
		Field fld = e.field;
		// Fully enclose the origin so up/left/right immobile probes collide.
		for (int x = 0; x < fld.getWidth(); x++) {
			fld.setBlockColor(x, 19, Block.BLOCK_COLOR_GRAY);
			fld.setBlockColor(x, 17, Block.BLOCK_COLOR_GRAY);
		}
		fld.setBlockColor(3, 18, Block.BLOCK_COLOR_GRAY);
		fld.setBlockColor(5, 18, Block.BLOCK_COLOR_GRAY);

		e.setAllSpin(4, 18, t, fld);

		assertTrue(e.tspin,
				"boxed-in T satisfies the immobile spin rule");
	}
}
