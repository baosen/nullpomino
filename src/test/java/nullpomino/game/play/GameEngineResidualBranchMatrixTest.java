package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

/** Exercises residual short-circuit combinations in {@link GameEngine}. */
class GameEngineResidualBranchMatrixTest {

	private static GameEngine freshEngine() {
		GameManager manager = new GameManager(new EventReceiver());
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static GameEngine engineWithField() {
		GameEngine engine = freshEngine();
		engine.createFieldIfNeeded();
		return engine;
	}

	private static void press(GameEngine engine, int button) {
		engine.ctrl.buttonPress[button] = true;
		engine.ctrl.buttonTime[button] = 1;
	}

	private static void fillLines(GameEngine engine, int count) {
		for(int y = engine.field.getHeight() - count; y < engine.field.getHeight(); y++) {
			for(int x = 0; x < engine.field.getWidth(); x++) {
				engine.field.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
			}
		}
	}

	@Test
	void resetAndContinuousInputPredicatesCoverTheirResidualCombinations() {
		GameEngine engine = engineWithField();
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_NONE, 0, 0));
		engine.resetFieldVisible();

		engine.gameActive = true;
		engine.ruleopt.softdropLimit = true;
		engine.ruleopt.harddropLimit = true;
		engine.ruleopt.holdInitialLimit = true;
		engine.ruleopt.rotateInitialLimit = true;
		engine.softdropContinuousUse = true;
		engine.harddropContinuousUse = true;
		engine.initialHoldContinuousUse = true;
		engine.initialRotateContinuousUse = true;
		engine.initialRotateLastDirection = -1;
		press(engine, engine.getDown());
		press(engine, engine.getUp());
		press(engine, Controller.BUTTON_D);
		press(engine, Controller.BUTTON_C);
		engine.checkDropContinuousUse();
		assertTrue(engine.softdropContinuousUse);
		assertTrue(engine.harddropContinuousUse);
		assertTrue(engine.initialHoldContinuousUse);
		assertTrue(engine.initialRotateContinuousUse);

		engine.ctrl = new Controller();
		engine.initialRotateContinuousUse = true;
		engine.initialRotateLastDirection = 1;
		press(engine, Controller.BUTTON_B);
		engine.checkDropContinuousUse();
		assertTrue(engine.initialRotateContinuousUse);

		engine.ctrl = new Controller();
		engine.initialRotateContinuousUse = true;
		engine.initialRotateLastDirection = 2;
		press(engine, Controller.BUTTON_E);
		engine.checkDropContinuousUse();
		assertTrue(engine.initialRotateContinuousUse);
	}

	@Test
	void spinDetectionCoversGuardCollisionAndSpotMatrices() {
		GameEngine engine = engineWithField();
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(engine.ruleopt.pieceOffsetX[t.id], engine.ruleopt.pieceOffsetY[t.id]);
		engine.nowPieceObject = t;
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;

		engine.tspinAllowKick = false;
		engine.kickused = false;
		engine.spinCheckType = 99;
		engine.setTSpin(5, 5, t, engine.field);
		engine.setAllSpin(5, 5, t, engine.field);

		engine.tspinAllowKick = true;
		engine.spinCheckType = GameEngine.SPINTYPE_4POINT;
		engine.tspinminiType = 99;
		engine.setTSpin(5, 5, t, engine.field);

		SequencePiece rotateCheck = new SequencePiece(true, false);
		engine.nowPieceObject = rotateCheck;
		engine.tspinminiType = GameEngine.TSPINMINI_TYPE_ROTATECHECK;
		engine.setTSpin(5, 5, rotateCheck, engine.field);

		boolean[][] collisionResults = {
			{false}, {true, false}, {true, true, false}, {true, true, true}
		};
		for(boolean[] results : collisionResults) {
			engine.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
			engine.tspinEnableEZ = true;
			engine.kickused = false;
			engine.setTSpin(5, 5, new SequencePiece(results), engine.field);
			engine.setAllSpin(5, 5, new SequencePiece(results), engine.field);
		}

		for(boolean kick : new boolean[]{false, true}) {
			engine.kickused = kick;
			engine.tspinEnableEZ = true;
			engine.setTSpin(5, 5, new SequencePiece(false), engine.field);
			engine.setAllSpin(5, 5, new SequencePiece(false), engine.field);
		}

		engine.spinCheckType = GameEngine.SPINTYPE_4POINT;
		engine.tspinAllowKick = true;
		engine.kickused = false;
		for(int mask = 0; mask < 16; mask++) {
			engine.field.reset();
			Piece piece = new Piece(Piece.PIECE_T);
			int direction = piece.direction;
			int offsetX = engine.ruleopt.pieceOffsetX[piece.id][direction];
			int offsetY = engine.ruleopt.pieceOffsetY[piece.id][direction];
			int[] xs = {
				Piece.SPINBONUSDATA_HIGH_X[piece.id][direction][0],
				Piece.SPINBONUSDATA_HIGH_X[piece.id][direction][1],
				Piece.SPINBONUSDATA_LOW_X[piece.id][direction][0],
				Piece.SPINBONUSDATA_LOW_X[piece.id][direction][1]
			};
			int[] ys = {
				Piece.SPINBONUSDATA_HIGH_Y[piece.id][direction][0],
				Piece.SPINBONUSDATA_HIGH_Y[piece.id][direction][1],
				Piece.SPINBONUSDATA_LOW_Y[piece.id][direction][0],
				Piece.SPINBONUSDATA_LOW_Y[piece.id][direction][1]
			};
			for(int i = 0; i < 4; i++) {
				if((mask & (1 << i)) != 0) {
					engine.field.setBlockColor(5 + xs[i] + offsetX, 5 + ys[i] + offsetY,
						Block.BLOCK_COLOR_GRAY);
				}
			}
			engine.setAllSpin(5, 5, piece, engine.field);
		}
	}

	@Test
	void fieldUpdateCoversHiddenItemAndNullBlockMatrices() {
		GameEngine engine = engineWithField();
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_NONE, 0, 0));
		engine.field.setBlockColor(1, 0, Block.BLOCK_COLOR_GRAY);
		Block block = engine.field.getBlock(1, 0);

		engine.blockHidden = 100;
		block.elapsedFrames = 0;
		engine.gameActive = true;
		engine.fieldUpdate();
		block.elapsedFrames = 95;
		engine.gameActive = false;
		engine.fieldUpdate();
		block.elapsedFrames = 95;
		engine.gameActive = true;
		engine.blockHiddenAnim = false;
		engine.fieldUpdate();
		block.elapsedFrames = 95;
		block.alpha = 0.05f;
		engine.blockHiddenAnim = true;
		engine.fieldUpdate();

		engine.itemXRayEnable = true;
		engine.field = null;
		engine.fieldUpdate();
		engine.createFieldIfNeeded();
		engine.gameActive = false;
		engine.fieldUpdate();
		engine.gameActive = true;
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_NONE, 0, 0));
		engine.field.setBlockColor(1, 0, Block.BLOCK_COLOR_GRAY);
		engine.fieldUpdate();

		engine.itemColorEnable = true;
		engine.gameActive = false;
		engine.fieldUpdate();
		engine.gameActive = true;
		engine.itemColorCount = 39;
		engine.fieldUpdate();

		engine.heboHiddenEnable = true;
		engine.gameActive = false;
		engine.fieldUpdate();
		engine.gameActive = true;
		engine.heboHiddenTimerMax = 0;
		engine.heboHiddenYLimit = 0;
		engine.fieldUpdate();
		assertTrue(engine.itemColorCount > 0);
	}

	@Test
	void nullableBlockLoopsObserveEmptyAndOccupiedCellsTogether() {
		GameEngine engine = freshEngine();
		engine.field = new MixedBlockField();
		engine.resetFieldVisible();

		engine.gameActive = true;
		engine.itemXRayEnable = true;
		engine.itemColorEnable = true;
		engine.fieldUpdate();

		assertTrue(engine.field.getBlock(1, 0).alpha >= 0f);
	}

	@Test
	void updateCoversAiHintReplayStatusAndStatisticsMatrices() {
		GameEngine engine = freshEngine();
		DummyAI ai = new DummyAI();
		engine.ai = ai;
		engine.aiShowHint = true;
		engine.gameActive = true;
		engine.stat = GameEngine.Status.NOTHING;
		engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T)};

		ai.thinkComplete = false;
		ai.thinkCurrentPieceNo = 0;
		ai.thinkLastPieceNo = 0;
		engine.update();
		ai.thinkCurrentPieceNo = 1;
		engine.update();
		ai.thinkLastPieceNo = 1;
		engine.update();
		ai.thinkComplete = true;
		ai.bestHold = true;
		engine.holdPieceObject = new Piece(Piece.PIECE_I);
		engine.update();
		assertNotNull(engine.aiHintPiece);
		engine.holdPieceObject = null;
		engine.update();
		assertNotNull(engine.aiHintPiece);
		ai.bestHold = false;
		engine.nowPieceObject = null;
		engine.update();
		engine.nowPieceObject = new Piece(Piece.PIECE_L);
		engine.update();
		assertNotNull(engine.aiHintPiece);

		engine.ending = 1;
		engine.staffrollEnableStatistics = false;
		engine.update();
		engine.staffrollEnableStatistics = true;
		engine.update();
		engine.owner.replayMode = true;
		engine.owner.replayRerecord = false;
		engine.update();
		engine.owner.replayRerecord = true;
		engine.update();
	}

	@Test
	void saveReplayCoversModeAndRuleNamePresence() {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = new NamedMode();
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.ruleopt.strRuleName = "matrix-rule";
		engine.saveReplay();
		assertNotNull(manager.replayProp.getProperty("name.mode"));
		assertNotNull(manager.replayProp.getProperty("name.rule"));
	}

	@Test
	void lineClearInputPredicatesCoverEveryShortCircuitPosition() {
		int[] movement = {Controller.BUTTON_UP, Controller.BUTTON_DOWN,
			Controller.BUTTON_LEFT, Controller.BUTTON_RIGHT};
		for(int button : movement) runLineCancel(button, 0);
		int[] rotation = {Controller.BUTTON_A, Controller.BUTTON_B,
			Controller.BUTTON_C, Controller.BUTTON_E};
		for(int button : rotation) runLineCancel(button, 1);
		runLineCancel(Controller.BUTTON_D, 2);

		GameEngine redirect = engineWithField();
		redirect.stat = GameEngine.Status.LINECLEAR;
		redirect.statc[0] = 1;
		redirect.speed.lineDelay = 100;
		redirect.ruleopt.dasInLineClear = false;
		redirect.ruleopt.dasRedirectInDelay = true;
		press(redirect, Controller.BUTTON_RIGHT);
		redirect.statLineClear();
	}

	private static void runLineCancel(int button, int kind) {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.statc[0] = 1;
		engine.speed.lineDelay = 100;
		if(kind == 0) engine.ruleopt.lineCancelMove = true;
		if(kind == 1) engine.ruleopt.lineCancelRotate = true;
		if(kind == 2) engine.ruleopt.lineCancelHold = true;
		press(engine, button);
		engine.statLineClear();
		assertTrue(engine.delayCancel);
	}

	@Test
	void lineClearFirstFrameCoversStatisticsB2bComboAndModeMatrices() {
		for(int lines = 1; lines <= 4; lines++) {
			GameEngine normal = lineClearEngine(lines);
			normal.statLineClear();
			GameEngine spin = lineClearEngine(lines);
			spin.tspin = true;
			spin.tspinmini = (lines <= 2);
			spin.statLineClear();
		}

		GameEngine big = lineClearEngine(2);
		big.big = true;
		big.bighalf = true;
		big.statLineClear();
		big = lineClearEngine(2);
		big.big = true;
		big.bighalf = false;
		big.statLineClear();

		for(boolean spin : new boolean[]{false, true}) {
			GameEngine b2b = lineClearEngine(spin ? 1 : 4);
			b2b.tspin = spin;
			b2b.b2bEnable = true;
			b2b.b2bcount = 1;
			b2b.ending = 1;
			b2b.staffrollEnableStatistics = true;
			b2b.statLineClear();
		}

		for(int type : new int[]{GameEngine.COMBO_TYPE_NORMAL, GameEngine.COMBO_TYPE_DOUBLE}) {
			for(int lines : new int[]{1, 2}) {
				GameEngine combo = lineClearEngine(lines);
				combo.comboType = type;
				combo.combo = 21;
				combo.ending = 1;
				combo.staffrollEnableStatistics = true;
				combo.statLineClear();
			}
		}

		GameEngine noKnownClearMode = lineClearEngine(0);
		noKnownClearMode.clearMode = null;
		noKnownClearMode.statLineClear();
	}

	private static GameEngine lineClearEngine(int lines) {
		GameEngine engine = engineWithField();
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.speed.lineDelay = 100;
		engine.statc[0] = 0;
		engine.clearMode = GameEngine.ClearType.LINE;
		fillLines(engine, lines);
		return engine;
	}

	@Test
	void areInputAndTransitionPredicatesCoverEveryShortCircuitPosition() {
		int[] movement = {Controller.BUTTON_UP, Controller.BUTTON_DOWN,
			Controller.BUTTON_LEFT, Controller.BUTTON_RIGHT};
		for(int button : movement) runAreCancel(button, 0);
		int[] rotation = {Controller.BUTTON_A, Controller.BUTTON_B,
			Controller.BUTTON_C, Controller.BUTTON_E};
		for(int button : rotation) runAreCancel(button, 1);
		runAreCancel(Controller.BUTTON_D, 2);

		GameEngine charging = freshEngine();
		charging.stat = GameEngine.Status.ARE;
		charging.statc[1] = 10;
		charging.ruleopt.dasInARE = true;
		charging.statARE();
		GameEngine lastFrame = freshEngine();
		lastFrame.stat = GameEngine.Status.ARE;
		lastFrame.statc[0] = 8;
		lastFrame.statc[1] = 10;
		lastFrame.ruleopt.dasInARE = true;
		lastFrame.ruleopt.dasInARELastFrame = true;
		lastFrame.statARE();
		GameEngine redirect = freshEngine();
		redirect.stat = GameEngine.Status.ARE;
		redirect.statc[1] = 10;
		redirect.ruleopt.dasRedirectInDelay = true;
		press(redirect, Controller.BUTTON_LEFT);
		redirect.statARE();

		GameEngine lagged = freshEngine();
		lagged.stat = GameEngine.Status.ARE;
		lagged.statc[1] = 0;
		lagged.lagARE = true;
		lagged.statARE();
		assertTrue(lagged.stat == GameEngine.Status.ARE);
		GameEngine interrupted = freshEngine();
		interrupted.stat = GameEngine.Status.ARE;
		interrupted.statc[1] = 0;
		interrupted.interruptItemNumber = GameEngine.INTERRUPTITEM_NONE + 1;
		interrupted.statARE();
		assertTrue(interrupted.stat == GameEngine.Status.INTERRUPTITEM);
	}

	private static void runAreCancel(int button, int kind) {
		GameEngine engine = freshEngine();
		engine.stat = GameEngine.Status.ARE;
		engine.statc[1] = 100;
		engine.lagARE = true;
		if(kind == 0) engine.ruleopt.areCancelMove = true;
		if(kind == 1) engine.ruleopt.areCancelRotate = true;
		if(kind == 2) engine.ruleopt.areCancelHold = true;
		press(engine, button);
		engine.statARE();
		assertTrue(engine.delayCancel);
	}

	private static GameEngine moveEngine() {
		GameEngine engine = engineWithField();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 2;
		engine.ruleopt.moveFirstFrame = true;
		engine.speed.gravity = 0;
		engine.speed.denominator = 1;
		engine.speed.lockDelay = 30;
		Piece piece = new Piece(Piece.PIECE_T);
		piece.applyOffsetArray(engine.ruleopt.pieceOffsetX[piece.id],
			engine.ruleopt.pieceOffsetY[piece.id]);
		engine.nowPieceObject = piece;
		engine.nowPieceX = 5;
		engine.nowPieceY = 5;
		engine.nowPieceBottomY = piece.getBottom(5, 5, engine.field);
		engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T),
			new Piece(Piece.PIECE_I), new Piece(Piece.PIECE_L)};
		return engine;
	}

	@Test
	void statMoveCoversDasDelayCancelAndFramePredicates() {
		GameEngine firstFrame = moveEngine();
		firstFrame.statc[0] = 0;
		firstFrame.ruleopt.dasInMoveFirstFrame = true;
		firstFrame.nextPieceCount = 0;
		press(firstFrame, Controller.BUTTON_LEFT);
		firstFrame.statMove();

		for(boolean left : new boolean[]{false, true}) {
			for(boolean right : new boolean[]{false, true}) {
				GameEngine delayed = moveEngine();
				delayed.statc[0] = 0;
				delayed.delayCancel = true;
				delayed.delayCancelMoveLeft = left;
				delayed.delayCancelMoveRight = right;
				delayed.statMove();
			}
		}

		for(boolean belowDas : new boolean[]{false, true}) {
			GameEngine delayed = moveEngine();
			delayed.statc[0] = 1;
			delayed.delayCancel = true;
			delayed.speed.das = 5;
			delayed.dasCount = belowDas ? 0 : 5;
			delayed.statMove();
		}

		for(boolean big : new boolean[]{false, true}) {
			for(boolean bigMove : new boolean[]{false, true}) {
				GameEngine movement = moveEngine();
				movement.big = big;
				movement.bigmove = bigMove;
				press(movement, Controller.BUTTON_RIGHT);
				movement.statMove();
			}
		}

		GameEngine storedNeutral = moveEngine();
		storedNeutral.dasDirection = 1;
		storedNeutral.dasCount = 5;
		storedNeutral.ruleopt.dasStoreChargeOnNeutral = true;
		storedNeutral.statMove();
	}

	@Test
	void statMoveCoversHardDropPredicateMatrix() {
		for(int disabled = -1; disabled < 6; disabled++) {
			GameEngine engine = moveEngine();
			engine.ruleopt.harddropEnable = true;
			engine.ruleopt.moveUpAndDown = true;
			engine.owMoveDiagonal = 1;
			press(engine, Controller.BUTTON_UP);
			if(disabled == 0) engine.ctrl = new Controller();
			if(disabled == 1) engine.harddropContinuousUse = true;
			if(disabled == 2) engine.ruleopt.harddropEnable = false;
			if(disabled == 3) {
				engine.owMoveDiagonal = 0;
				press(engine, Controller.BUTTON_RIGHT);
			}
			if(disabled == 4) {
				engine.ruleopt.moveUpAndDown = false;
				press(engine, Controller.BUTTON_DOWN);
			}
			if(disabled == 5) engine.nowPieceBottomY = engine.nowPieceY;
			engine.statMove();
		}

		for(boolean reset : new boolean[]{false, true}) {
			GameEngine engine = moveEngine();
			engine.ruleopt.harddropEnable = true;
			engine.ruleopt.moveUpAndDown = true;
			engine.ruleopt.lockresetFall = reset;
			press(engine, Controller.BUTTON_UP);
			engine.statMove();
		}
	}

	@Test
	void statMoveCoversOldAndNewSoftDropPredicateMatrices() {
		for(int disabled = -1; disabled < 5; disabled++) {
			GameEngine engine = moveEngine();
			engine.ruleopt.softdropGravitySpeedLimit = false;
			engine.ruleopt.softdropEnable = true;
			engine.ruleopt.moveUpAndDown = true;
			engine.owMoveDiagonal = 1;
			engine.ruleopt.softdropSpeed = 2f;
			press(engine, Controller.BUTTON_DOWN);
			if(disabled == 0) engine.ctrl = new Controller();
			if(disabled == 1) engine.softdropContinuousUse = true;
			if(disabled == 2) engine.ruleopt.softdropEnable = false;
			if(disabled == 3) {
				engine.owMoveDiagonal = 0;
				press(engine, Controller.BUTTON_RIGHT);
			}
			if(disabled == 4) {
				engine.ruleopt.moveUpAndDown = false;
				press(engine, Controller.BUTTON_UP);
			}
			engine.statMove();
		}

		for(int disabled = -1; disabled < 6; disabled++) {
			GameEngine engine = moveEngine();
			engine.ruleopt.softdropGravitySpeedLimit = true;
			engine.ruleopt.softdropSpeed = 2f;
			engine.ruleopt.softdropEnable = true;
			engine.ruleopt.moveUpAndDown = true;
			engine.ruleopt.softdropMultiplyNativeSpeed = true;
			engine.owMoveDiagonal = 1;
			press(engine, Controller.BUTTON_DOWN);
			if(disabled == 0) engine.ctrl = new Controller();
			if(disabled == 1) engine.softdropContinuousUse = true;
			if(disabled == 2) engine.ruleopt.softdropEnable = false;
			if(disabled == 3) {
				engine.owMoveDiagonal = 0;
				press(engine, Controller.BUTTON_RIGHT);
			}
			if(disabled == 4) {
				engine.ruleopt.moveUpAndDown = false;
				press(engine, Controller.BUTTON_UP);
			}
			if(disabled == 5) {
				engine.ruleopt.softdropMultiplyNativeSpeed = false;
				engine.speed.gravity = 100;
				engine.speed.denominator = 1;
			}
			engine.statMove();
		}

		for(boolean multiply : new boolean[]{false, true}) {
			GameEngine engine = moveEngine();
			engine.ruleopt.softdropGravitySpeedLimit = true;
			engine.ruleopt.softdropSpeed = 2f;
			engine.ruleopt.softdropEnable = true;
			engine.ruleopt.softdropMultiplyNativeSpeed = multiply;
			engine.speed.denominator = -1;
			press(engine, Controller.BUTTON_DOWN);
			engine.statMove();
		}
	}

	@Test
	void statMoveCoversGravityStatisticsAndDasTailPredicates() {
		for(boolean resetFall : new boolean[]{false, true}) {
			GameEngine engine = moveEngine();
			engine.speed.gravity = 1;
			engine.speed.denominator = 1;
			engine.ruleopt.lockresetFall = resetFall;
			engine.statMove();
		}

		GameEngine negativeGravity = moveEngine();
		negativeGravity.speed.gravity = -1;
		negativeGravity.speed.denominator = 100;
		negativeGravity.statMove();

		for(boolean statistics : new boolean[]{false, true}) {
			GameEngine engine = moveEngine();
			engine.ending = 1;
			engine.staffrollEnableStatistics = statistics;
			engine.statMove();
		}

		for(boolean firstFrameDas : new boolean[]{false, true}) {
			GameEngine engine = moveEngine();
			engine.statc[0] = 0;
			engine.ruleopt.dasInMoveFirstFrame = firstFrameDas;
			engine.speed.das = 0;
			press(engine, Controller.BUTTON_LEFT);
			engine.statMove();
		}
	}

	@Test
	void statMoveAppearanceCoversCounterOverflowAndHoldPaths() {
		GameEngine normal = moveEngine();
		normal.statc[0] = 0;
		normal.statc[1] = 0;
		normal.nextPieceCount = Integer.MAX_VALUE;
		normal.statMove();

		GameEngine firstInitialHold = moveEngine();
		firstInitialHold.statc[0] = 0;
		firstInitialHold.statc[1] = 1;
		firstInitialHold.initialHoldFlag = true;
		firstInitialHold.holdPieceObject = null;
		firstInitialHold.nextPieceCount = Integer.MAX_VALUE;
		firstInitialHold.statMove();

		GameEngine secondOverflow = moveEngine();
		secondOverflow.statc[0] = 0;
		secondOverflow.statc[1] = 1;
		secondOverflow.initialHoldFlag = true;
		secondOverflow.holdPieceObject = null;
		secondOverflow.nextPieceCount = Integer.MAX_VALUE - 1;
		secondOverflow.statMove();

		GameEngine laterInitialHold = moveEngine();
		laterInitialHold.statc[0] = 0;
		laterInitialHold.statc[1] = 1;
		laterInitialHold.initialHoldFlag = true;
		laterInitialHold.holdPieceObject = new Piece(Piece.PIECE_I);
		laterInitialHold.nextPieceCount = Integer.MAX_VALUE;
		laterInitialHold.statMove();

		GameEngine usualHold = moveEngine();
		usualHold.statc[0] = 0;
		usualHold.statc[1] = 1;
		usualHold.initialHoldFlag = false;
		usualHold.holdPieceObject = null;
		usualHold.nextPieceCount = Integer.MAX_VALUE;
		usualHold.statMove();
	}

	@Test
	void statMoveHoldAndRotationCoverResidualInputCombinations() {
		GameEngine initialHold = moveEngine();
		initialHold.initialHoldFlag = true;
		initialHold.holdDisable = true;
		initialHold.statMove();

		for(int rollCase = 0; rollCase < 3; rollCase++) {
			GameEngine engine = moveEngine();
			engine.itemRollRollEnable = rollCase != 0;
			engine.itemRollRollInterval = 3;
			engine.replayTimer = rollCase == 2 ? 3 : 1;
			engine.statMove();
		}

		GameEngine cRotate = moveEngine();
		press(cRotate, Controller.BUTTON_C);
		cRotate.statMove();
		GameEngine noDouble = moveEngine();
		noDouble.ruleopt.rotateButtonAllowDouble = false;
		press(noDouble, Controller.BUTTON_E);
		noDouble.statMove();

		for(boolean statistics : new boolean[]{false, true}) {
			GameEngine rotate = moveEngine();
			rotate.ending = 1;
			rotate.staffrollEnableStatistics = statistics;
			press(rotate, Controller.BUTTON_B);
			rotate.statMove();
		}
	}

	private static GameEngine groundEngine() {
		GameEngine engine = moveEngine();
		engine.nowPieceY = 18;
		engine.nowPieceBottomY = 18;
		for(int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlockColor(x, engine.field.getHeight() - 1, Block.BLOCK_COLOR_GRAY);
		}
		return engine;
	}

	@Test
	void statMoveGroundHardDropCoversEveryPredicatePosition() {
		for(int disabled = -1; disabled < 6; disabled++) {
			GameEngine engine = groundEngine();
			engine.ruleopt.harddropEnable = true;
			engine.ruleopt.harddropLock = true;
			engine.ruleopt.moveUpAndDown = true;
			engine.owMoveDiagonal = 1;
			press(engine, Controller.BUTTON_UP);
			if(disabled == 0) engine.ctrl = new Controller();
			if(disabled == 1) engine.harddropContinuousUse = true;
			if(disabled == 2) engine.ruleopt.harddropEnable = false;
			if(disabled == 3) {
				engine.owMoveDiagonal = 0;
				press(engine, Controller.BUTTON_RIGHT);
			}
			if(disabled == 4) {
				engine.ruleopt.moveUpAndDown = false;
				press(engine, Controller.BUTTON_DOWN);
			}
			if(disabled == 5) engine.ruleopt.harddropLock = false;
			engine.statMove();
		}
	}

	@Test
	void statMoveGroundSoftDropAndSurfaceLockCoverEveryPredicatePosition() {
		for(boolean surface : new boolean[]{false, true}) {
			for(int disabled = -1; disabled < 6; disabled++) {
				GameEngine engine = groundEngine();
				engine.ruleopt.softdropEnable = true;
				engine.ruleopt.softdropLock = !surface;
				engine.ruleopt.softdropSurfaceLock = surface;
				engine.ruleopt.moveUpAndDown = true;
				engine.owMoveDiagonal = 1;
				press(engine, Controller.BUTTON_DOWN);
				if(disabled == 0) engine.ctrl = new Controller();
				if(disabled == 1) engine.softdropContinuousUse = true;
				if(disabled == 2) engine.ruleopt.softdropEnable = false;
				if(disabled == 3) {
					engine.owMoveDiagonal = 0;
					press(engine, Controller.BUTTON_RIGHT);
				}
				if(disabled == 4) {
					engine.ruleopt.moveUpAndDown = false;
					press(engine, Controller.BUTTON_UP);
				}
				if(disabled == 5) {
					engine.ruleopt.softdropLock = false;
					engine.ruleopt.softdropSurfaceLock = false;
				}
				engine.statMove();
			}
		}
	}

	@Test
	void statMoveGroundInstantLockCoversLimitAndGravityMatrices() {
		GameEngine wrongLimitType = groundEngine();
		wrongLimitType.ruleopt.lockresetLimitOver = 99;
		wrongLimitType.statMove();

		GameEngine moveExceeded = groundEngine();
		moveExceeded.ruleopt.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT;
		moveExceeded.ruleopt.lockresetLimitShareCount = false;
		moveExceeded.ruleopt.lockresetLimitMove = 0;
		moveExceeded.extendedMoveCount = 0;
		moveExceeded.statMove();

		GameEngine rotateExceeded = groundEngine();
		rotateExceeded.ruleopt.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_INSTANT;
		rotateExceeded.ruleopt.lockresetLimitShareCount = false;
		rotateExceeded.ruleopt.lockresetLimitMove = 100;
		rotateExceeded.ruleopt.lockresetLimitRotate = 0;
		rotateExceeded.extendedRotateCount = 0;
		rotateExceeded.statMove();

		for(boolean negativeGravity : new boolean[]{false, true}) {
			GameEngine engine = groundEngine();
			engine.speed.lockDelay = 0;
			engine.speed.denominator = 10;
			engine.gcount = negativeGravity ? 0 : 10;
			engine.speed.gravity = negativeGravity ? -1 : 0;
			engine.statMove();
		}
	}

	@Test
	void statMoveLockTransitionsCoverStatusEndingLockoutAndDelayMatrices() {
		for(boolean statistics : new boolean[]{false, true}) {
			GameEngine engine = lockEngine(true, false);
			engine.ending = 1;
			engine.staffrollEnableStatistics = statistics;
			engine.statMove();
		}

		GameEngine gem = lockEngine(true, false);
		gem.clearMode = GameEngine.ClearType.GEM_COLOR;
		gem.colorClearSize = 4;
		gem.statMove();

		for(boolean mini : new boolean[]{false, true}) {
			GameEngine spin = lockEngine(true, false);
			spin.tspin = true;
			spin.tspinmini = mini;
			spin.lastmove = GameEngine.LastMove.NONE;
			spin.ending = 1;
			spin.staffrollEnableStatistics = true;
			spin.statMove();
		}

		for(boolean oldVersion : new boolean[]{false, true}) {
			HookMode mode = new HookMode();
			mode.statusAfterLock = GameEngine.Status.CUSTOM;
			GameEngine engine = lockEngine(true, false, mode);
			engine.versionMajor = oldVersion ? 6.0f : 7.0f;
			engine.statMove();
		}

		for(boolean noDeath : new boolean[]{false, true}) {
			GameEngine lockout = lockEngine(false, false);
			lockout.ruleopt.fieldLockoutDeath = true;
			lockout.ending = 2;
			lockout.staffrollNoDeath = noDeath;
			lockout.statMove();
			GameEngine partial = lockEngine(true, true);
			partial.ruleopt.fieldPartialLockoutDeath = true;
			partial.ending = 2;
			partial.staffrollNoDeath = noDeath;
			partial.statMove();
		}

		for(GameEngine.LineGravity gravity : GameEngine.LineGravity.values()) {
			GameEngine cascade = lockEngine(true, false);
			cascade.lineGravityType = gravity;
			cascade.connectBlocks = false;
			cascade.statMove();
		}

		GameEngine line = lockEngine(true, false);
		line.lineClearing = 1;
		line.ruleopt.lockflash = 0;
		line.statMove();
		GameEngine lockFlash = lockEngine(true, false);
		lockFlash.speed.are = 1;
		lockFlash.ruleopt.lockflash = 1;
		lockFlash.ruleopt.lockflashBeforeLineClear = true;
		lockFlash.ruleopt.lockflashOnlyFrame = true;
		lockFlash.statMove();
		GameEngine lag = lockEngine(true, false);
		lag.lagARE = true;
		lag.statMove();
		GameEngine noFirstFrame = lockEngine(true, false);
		noFirstFrame.ruleopt.moveFirstFrame = false;
		noFirstFrame.statMove();
	}

	private static GameEngine lockEngine(boolean put, boolean partial) {
		return lockEngine(put, partial, null);
	}

	private static GameEngine lockEngine(boolean put, boolean partial, HookMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 2;
		engine.ruleopt.moveFirstFrame = true;
		engine.speed.lockDelay = 0;
		engine.speed.gravity = 0;
		engine.speed.denominator = 1;
		engine.gcount = 1;
		engine.nowPieceObject = new LockPiece(put, partial);
		engine.nowPieceX = 5;
		engine.nowPieceY = 18;
		engine.nowPieceBottomY = 18;
		engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T)};
		return engine;
	}

	@Test
	void renderAndReadyCoverAiFlagsSkipAndReadyDoneBranches() {
		GameEngine rendered = freshEngine();
		rendered.ai = new DummyAI();
		rendered.aiShowState = true;
		rendered.aiShowHint = true;
		rendered.render();

		GameEngine initialized = freshEngine();
		initialized.stat = GameEngine.Status.READY;
		initialized.statc[0] = 0;
		initialized.nextPieceArrayID = new int[]{Piece.PIECE_T};
		initialized.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T)};
		initialized.statReady();

		for(int disabled = -1; disabled < 5; disabled++) {
			GameEngine skip = freshEngine();
			skip.stat = GameEngine.Status.READY;
			skip.statc[0] = 1;
			skip.goEnd = 100;
			skip.holdButtonNextSkip = true;
			skip.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T)};
			skip.nextPieceCount = disabled == -1 ? Integer.MAX_VALUE : 0;
			press(skip, Controller.BUTTON_D);
			if(disabled == 0) skip.statc[0] = 0;
			if(disabled == 1) skip.statc[0] = skip.goEnd;
			if(disabled == 2) skip.holdButtonNextSkip = false;
			if(disabled == 3) skip.holdDisable = true;
			if(disabled == 4) skip.ctrl = new Controller();
			skip.statReady();
		}

		GameEngine alreadyReady = freshEngine();
		alreadyReady.stat = GameEngine.Status.READY;
		alreadyReady.readyDone = true;
		alreadyReady.statc[0] = alreadyReady.goEnd;
		alreadyReady.nextPieceArrayID = new int[]{Piece.PIECE_T};
		alreadyReady.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T)};
		alreadyReady.statReady();
	}

	@Test
	void stubbornUtilityPredicatesCoverFalseAndInvariantSides() {
		GameEngine rendered = freshEngine();
		rendered.ai = new DummyAI();
		rendered.aiShowState = false;
		rendered.aiShowHint = false;
		rendered.render();

		GameEngine spawn = engineWithField();
		spawn.big = true;
		spawn.bigmove = false;
		spawn.getSpawnPosX(new Field(9, 20, 0, false), new Piece(Piece.PIECE_T));
		spawn.bigmove = true;
		spawn.getSpawnPosX(new Field(8, 20, 0, false), new Piece(Piece.PIECE_T));

		GameEngine colors = engineWithField();
		colors.gameActive = true;
		colors.itemColorEnable = true;
		colors.field.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		for(int count = -100; count <= 100; count++) {
			colors.itemColorCount = count;
			colors.fieldUpdate();
		}

		GameEngine unnamed = freshEngine();
		unnamed.ruleopt.strRuleName = null;
		unnamed.saveReplay();

		GameEngine spin = engineWithField();
		spin.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		spin.tspinAllowKick = true;
		for(boolean kick : new boolean[]{false, true}) {
			spin.kickused = kick;
			FullLineCollisionPiece piece = new FullLineCollisionPiece();
			spin.setTSpin(5, 5, piece, spin.field);
			spin.setAllSpin(5, 5, piece, spin.field);
		}
	}

	@Test
	void statMoveCoversModeAndRemainingLateralPredicateCombinations() {
		for(boolean stop : new boolean[]{false, true}) {
			HookMode mode = new HookMode();
			mode.stopMove = stop;
			GameManager manager = new GameManager(new EventReceiver());
			manager.mode = mode;
			manager.init();
			manager.engine[0].init();
			GameEngine engine = manager.engine[0];
			if(!stop) {
				engine.createFieldIfNeeded();
				engine.gameActive = true;
				engine.stat = GameEngine.Status.MOVE;
				engine.statc[0] = 0;
				engine.ruleopt.moveFirstFrame = false;
				engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T)};
			}
			engine.statMove();
		}

		GameEngine shiftLocked = moveEngine();
		shiftLocked.speed.das = 1;
		shiftLocked.dasCount = 1;
		shiftLocked.shiftLock = Controller.BUTTON_BIT_RIGHT;
		press(shiftLocked, Controller.BUTTON_RIGHT);
		shiftLocked.statMove();

		GameEngine waitingDas = moveEngine();
		waitingDas.speed.das = 1;
		waitingDas.ruleopt.dasDelay = 2;
		waitingDas.dasCount = 1;
		waitingDas.dasSpeedCount = 0;
		press(waitingDas, Controller.BUTTON_RIGHT);
		waitingDas.statMove();

		GameEngine exhaustedReset = moveEngine();
		exhaustedReset.ruleopt.lockresetMove = true;
		exhaustedReset.ruleopt.lockresetLimitShareCount = false;
		exhaustedReset.ruleopt.lockresetLimitMove = 0;
		exhaustedReset.extendedMoveCount = 0;
		press(exhaustedReset, Controller.BUTTON_RIGHT);
		exhaustedReset.statMove();

		GameEngine disabledReset = moveEngine();
		disabledReset.ruleopt.lockresetMove = false;
		press(disabledReset, Controller.BUTTON_RIGHT);
		disabledReset.statMove();

		for(boolean statistics : new boolean[]{false, true}) {
			GameEngine movement = moveEngine();
			movement.ending = 1;
			movement.staffrollEnableStatistics = statistics;
			press(movement, Controller.BUTTON_RIGHT);
			movement.statMove();
		}

		GameEngine oldInstantDas = moveEngine();
		oldInstantDas.speed.das = 1;
		oldInstantDas.ruleopt.dasDelay = 0;
		oldInstantDas.dasCount = 1;
		oldInstantDas.versionMajor = 7.0f;
		press(oldInstantDas, Controller.BUTTON_RIGHT);
		oldInstantDas.statMove();
	}

	@Test
	void dropPredicatesCoverNonDiagonalNoSideAndNonpositiveDenominators() {
		GameEngine hard = moveEngine();
		hard.owMoveDiagonal = 0;
		hard.ruleopt.harddropEnable = true;
		press(hard, Controller.BUTTON_UP);
		hard.statMove();

		GameEngine oldSoft = moveEngine();
		oldSoft.owMoveDiagonal = 0;
		oldSoft.ruleopt.softdropGravitySpeedLimit = false;
		oldSoft.ruleopt.softdropEnable = true;
		oldSoft.speed.denominator = -1;
		press(oldSoft, Controller.BUTTON_DOWN);
		oldSoft.statMove();

		GameEngine newSoft = moveEngine();
		newSoft.owMoveDiagonal = 0;
		newSoft.ruleopt.softdropGravitySpeedLimit = true;
		newSoft.ruleopt.softdropSpeed = 2f;
		newSoft.ruleopt.softdropEnable = true;
		press(newSoft, Controller.BUTTON_DOWN);
		newSoft.statMove();

		GameEngine newNegativeDenominator = moveEngine();
		newNegativeDenominator.ruleopt.softdropGravitySpeedLimit = true;
		newNegativeDenominator.ruleopt.softdropSpeed = 2f;
		newNegativeDenominator.ruleopt.softdropEnable = true;
		newNegativeDenominator.ruleopt.softdropMultiplyNativeSpeed = false;
		newNegativeDenominator.speed.gravity = -3;
		newNegativeDenominator.speed.denominator = -1;
		press(newNegativeDenominator, Controller.BUTTON_DOWN);
		newNegativeDenominator.statMove();

		GameEngine nonpositiveDas = moveEngine();
		nonpositiveDas.speed.das = 0;
		nonpositiveDas.dasDirection = 1;
		nonpositiveDas.dasCount = 0;
		press(nonpositiveDas, Controller.BUTTON_RIGHT);
		nonpositiveDas.statMove();
	}

	@Test
	void appearanceCoversHoldDirectionItemVersionGravityTimerAndAiMatrices() {
		for(int directionCase = 0; directionCase < 3; directionCase++) {
			GameEngine engine = moveEngine();
			engine.statc[0] = 0;
			engine.statc[1] = 1;
			engine.initialHoldFlag = false;
			engine.holdPieceObject = new Piece(Piece.PIECE_I);
			engine.ruleopt.holdResetDirection = directionCase != 0;
			engine.ruleopt.pieceDefaultDirection[engine.nowPieceObject.id] =
				directionCase == 2 ? Piece.DIRECTION_COUNT : Piece.DIRECTION_UP;
			engine.statMove();
		}

		GameEngine item = moveEngine();
		item.statc[0] = 0;
		item.itemRollRollEnable = true;
		item.statMove();
		GameEngine old = moveEngine();
		old.statc[0] = 0;
		old.versionMajor = 7.0f;
		old.statMove();
		GameEngine gravity = moveEngine();
		gravity.statc[0] = 0;
		gravity.speed.gravity = 3;
		gravity.speed.denominator = 2;
		gravity.statMove();
		GameEngine nonpositive = moveEngine();
		nonpositive.statc[0] = 0;
		nonpositive.speed.gravity = 3;
		nonpositive.speed.denominator = 0;
		nonpositive.statMove();
		GameEngine ending = moveEngine();
		ending.statc[0] = 0;
		ending.ending = 1;
		ending.statMove();

		for(int replayCase = 0; replayCase < 3; replayCase++) {
			GameEngine aiEngine = moveEngine();
			aiEngine.statc[0] = 0;
			if(replayCase != 0) aiEngine.ai = new DummyAI();
			aiEngine.owner.replayMode = replayCase == 2;
			aiEngine.owner.replayRerecord = false;
			aiEngine.statMove();
		}
	}

	@Test
	void lineClearResidualStatisticsAndCancelFalseSides() {
		for(boolean statistics : new boolean[]{false, true}) {
			GameEngine spin = lineClearEngine(1);
			spin.tspin = true;
			spin.ending = 1;
			spin.staffrollEnableStatistics = statistics;
			spin.statLineClear();
			GameEngine b2b = lineClearEngine(4);
			b2b.b2bEnable = true;
			b2b.b2bcount = 1;
			b2b.ending = 1;
			b2b.staffrollEnableStatistics = statistics;
			b2b.statLineClear();
			GameEngine combo = lineClearEngine(1);
			combo.comboType = GameEngine.COMBO_TYPE_NORMAL;
			combo.ending = 1;
			combo.staffrollEnableStatistics = statistics;
			combo.statLineClear();
		}

		for(int type : new int[]{GameEngine.COMBO_TYPE_DISABLE, GameEngine.COMBO_TYPE_NORMAL}) {
			GameEngine zero = lineClearEngine(0);
			zero.comboType = type;
			zero.statLineClear();
		}

		GameEngine noButtons = engineWithField();
		noButtons.stat = GameEngine.Status.LINECLEAR;
		noButtons.statc[0] = 1;
		noButtons.speed.lineDelay = 100;
		noButtons.ruleopt.lineCancelMove = true;
		noButtons.ruleopt.lineCancelRotate = true;
		noButtons.ruleopt.lineCancelHold = true;
		noButtons.statLineClear();
	}

	@Test
	void cascadeLineClearCoversAllClearModeChecksAndTransitionSides() {
		for(GameEngine.ClearType type : GameEngine.ClearType.values()) {
			GameEngine empty = cascadeEngine(type, false);
			empty.statLineClear();
			GameEngine clearing = cascadeEngine(type, true);
			clearing.statistics.maxChain = 0;
			clearing.statLineClear();
		}
		GameEngine noMode = cascadeEngine(null, false);
		noMode.statLineClear();

		GameEngine stickyDelay = cascadeEngine(GameEngine.ClearType.LINE, false);
		stickyDelay.cascadeClearDelay = 1;
		stickyDelay.sticky = 1;
		stickyDelay.statLineClear();

		for(boolean oldVersion : new boolean[]{false, true}) {
			HookMode mode = new HookMode();
			mode.statusAfterLine = GameEngine.Status.CUSTOM;
			GameEngine engine = cascadeEngine(GameEngine.ClearType.LINE, false, mode);
			engine.versionMajor = oldVersion ? 6.0f : 8.0f;
			engine.statLineClear();
		}

		GameEngine lag = cascadeEngine(GameEngine.ClearType.LINE, false);
		lag.lagARE = true;
		lag.speed.areLine = 0;
		lag.statLineClear();
		GameEngine oldNoAre = cascadeEngine(GameEngine.ClearType.LINE, false);
		oldNoAre.versionMajor = 7.0f;
		oldNoAre.speed.areLine = 0;
		oldNoAre.statLineClear();
	}

	private static GameEngine cascadeEngine(GameEngine.ClearType type, boolean clearing) {
		return cascadeEngine(type, clearing, null);
	}

	private static GameEngine cascadeEngine(GameEngine.ClearType type, boolean clearing, HookMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.field = new CascadeField(type, clearing);
		engine.stat = GameEngine.Status.LINECLEAR;
		engine.clearMode = type;
		engine.lineGravityType = GameEngine.LineGravity.CASCADE;
		engine.speed.lineDelay = 0;
		engine.cascadeDelay = 0;
		engine.cascadeClearDelay = 0;
		return engine;
	}

	@Test
	void endingStartAndGameOverCoverResidualModeReplayAndPlayerMatrices() {
		HookMode mode = new HookMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine ending = manager.engine[0];
		ending.createFieldIfNeeded();
		ending.speed.lineDelay = 0;
		ending.statc[1] = 6;
		ending.field.setBlockColor(0, ending.field.getHeight() - 1, Block.BLOCK_COLOR_RED);
		ending.ruleopt.dasRedirectInDelay = true;
		ending.statEndingStart();
		mode.stopEnding = true;
		ending.statEndingStart();

		for(int display : new int[]{-1, 0}) {
			GameEngine over = engineWithField();
			over.lives = 0;
			over.statc[0] = 1;
			over.displaysize = display;
			over.field.setBlockColor(0, over.field.getHeight() - 1, Block.BLOCK_COLOR_RED);
			over.statGameOver();
		}

		GameManager two = new GameManager(new EventReceiver());
		two.init();
		two.engine[0].init();
		GameEngine first = two.engine[0];
		GameEngine second = new GameEngine(two, 1);
		second.init();
		first.createFieldIfNeeded();
		second.field = null;
		two.engine = new GameEngine[]{first, second};
		for(boolean all : new boolean[]{false, true}) {
			first.gameoverAll = all;
			first.lives = 0;
			first.statc[0] = first.field.getHeight() + 1 + 180;
			first.owner.replayMode = true;
			first.owner.replayRerecord = all;
			first.statGameOver();
		}
	}

	@Test
	void customFieldsCoverStoredEmptyBlockAndItemColorInvariants() {
		GameEngine engine = freshEngine();
		AlwaysBlockField emptyBlockField = new AlwaysBlockField(Block.BLOCK_COLOR_NONE);
		engine.field = emptyBlockField;
		engine.resetFieldVisible();
		engine.gameActive = true;
		engine.itemXRayEnable = true;
		engine.itemColorEnable = true;
		engine.itemColorCount = -1000;
		engine.fieldUpdate();

		GameEngine emptyColorField = freshEngine();
		emptyColorField.field = new Field(1, 1, 0, false);
		emptyColorField.gameActive = true;
		emptyColorField.itemColorEnable = true;
		emptyColorField.fieldUpdate();

		engine.field = new AlwaysBlockField(Block.BLOCK_COLOR_RED);
		engine.itemColorCount = 0;
		engine.fieldUpdate();
	}

	@Test
	void allSpinReachesPriorSpinAndEqualLineCountBranches() {
		GameEngine engine = engineWithField();
		engine.spinCheckType = GameEngine.SPINTYPE_4POINT;
		for(int id = 0; id < Piece.PIECE_COUNT; id++) {
			Piece piece = new Piece(id);
			for(int direction = 0; direction < Piece.DIRECTION_COUNT; direction++) {
				piece.direction = direction;
				int[] highX = Piece.SPINBONUSDATA_HIGH_X[id][direction];
				int[] highY = Piece.SPINBONUSDATA_HIGH_Y[id][direction];
				int[] lowX = Piece.SPINBONUSDATA_LOW_X[id][direction];
				int[] lowY = Piece.SPINBONUSDATA_LOW_Y[id][direction];
				if(highX.length < 4) continue;
				engine.field.reset();
				int offsetX = engine.ruleopt.pieceOffsetX[id][direction];
				int offsetY = engine.ruleopt.pieceOffsetY[id][direction];
				for(int index : new int[]{0, 1}) {
					engine.field.setBlockColor(5 + highX[index] + offsetX,
						5 + highY[index] + offsetY, Block.BLOCK_COLOR_GRAY);
				}
				engine.field.setBlockColor(5 + lowX[0] + offsetX,
					5 + lowY[0] + offsetY, Block.BLOCK_COLOR_GRAY);
				engine.setAllSpin(5, 5, piece, engine.field);
			}
		}

		engine.spinCheckType = GameEngine.SPINTYPE_IMMOBILE;
		engine.tspinAllowKick = true;
		engine.kickused = true;
		FullLineCollisionPiece probe = new FullLineCollisionPiece();
		FullLineCollisionPiece equalLines = new FullLineCollisionPiece(probe.getHeight() + 1);
		engine.setAllSpin(5, 5, equalLines, engine.field);
	}

	@Test
	void remainingMoveInputLimitsAndAiReplaySides() {
		GameEngine shiftLocked = moveEngine();
		shiftLocked.speed.das = 1;
		shiftLocked.dasDirection = 1;
		shiftLocked.dasCount = 1;
		shiftLocked.shiftLock = Controller.BUTTON_BIT_RIGHT;
		press(shiftLocked, Controller.BUTTON_RIGHT);
		shiftLocked.statMove();

		GameEngine exhausted = moveEngine();
		exhausted.ruleopt.lockresetMove = true;
		exhausted.ruleopt.lockresetLimitShareCount = false;
		exhausted.ruleopt.lockresetLimitMove = 0;
		exhausted.extendedMoveCount = 0;
		press(exhausted, Controller.BUTTON_RIGHT);
		exhausted.statMove();

		GameEngine oldRepeat = moveEngine();
		oldRepeat.speed.das = 1;
		oldRepeat.ruleopt.dasDelay = 0;
		oldRepeat.dasDirection = 1;
		oldRepeat.dasCount = 1;
		oldRepeat.versionMajor = 7.0f;
		press(oldRepeat, Controller.BUTTON_RIGHT);
		oldRepeat.statMove();

		GameEngine hardVerticalOnly = moveEngine();
		hardVerticalOnly.owMoveDiagonal = 0;
		hardVerticalOnly.ruleopt.harddropEnable = true;
		hardVerticalOnly.ruleopt.moveUpAndDown = false;
		press(hardVerticalOnly, Controller.BUTTON_UP);
		hardVerticalOnly.statMove();

		GameEngine oldSoftLimited = moveEngine();
		oldSoftLimited.ruleopt.softdropGravitySpeedLimit = false;
		oldSoftLimited.ruleopt.softdropEnable = true;
		oldSoftLimited.ruleopt.softdropLimit = true;
		oldSoftLimited.softdropContinuousUse = true;
		press(oldSoftLimited, Controller.BUTTON_DOWN);
		oldSoftLimited.statMove();

		GameEngine oldSoftVerticalOnly = moveEngine();
		oldSoftVerticalOnly.owMoveDiagonal = 0;
		oldSoftVerticalOnly.ruleopt.softdropGravitySpeedLimit = false;
		oldSoftVerticalOnly.ruleopt.softdropEnable = true;
		oldSoftVerticalOnly.ruleopt.moveUpAndDown = false;
		press(oldSoftVerticalOnly, Controller.BUTTON_DOWN);
		oldSoftVerticalOnly.statMove();

		GameEngine newSoftLimited = moveEngine();
		newSoftLimited.ruleopt.softdropGravitySpeedLimit = true;
		newSoftLimited.ruleopt.softdropSpeed = 2f;
		newSoftLimited.ruleopt.softdropEnable = true;
		newSoftLimited.ruleopt.softdropLimit = true;
		newSoftLimited.softdropContinuousUse = true;
		press(newSoftLimited, Controller.BUTTON_DOWN);
		newSoftLimited.statMove();

		GameEngine newSoftVerticalOnly = moveEngine();
		newSoftVerticalOnly.owMoveDiagonal = 0;
		newSoftVerticalOnly.ruleopt.softdropGravitySpeedLimit = true;
		newSoftVerticalOnly.ruleopt.softdropSpeed = 2f;
		newSoftVerticalOnly.ruleopt.softdropEnable = true;
		newSoftVerticalOnly.ruleopt.moveUpAndDown = false;
		press(newSoftVerticalOnly, Controller.BUTTON_DOWN);
		newSoftVerticalOnly.statMove();

		GameEngine aiReplay = moveEngine();
		aiReplay.statc[0] = 0;
		aiReplay.ai = new DummyAI();
		aiReplay.owner.replayMode = true;
		aiReplay.owner.replayRerecord = true;
		aiReplay.statMove();
	}

	@Test
	void holdRotationWallkickQuickTurnAndGameOverMatrices() throws Exception {
		GameEngine hold = moveEngine();
		press(hold, Controller.BUTTON_D);
		hold.statMove();
		GameEngine initialHold = moveEngine();
		initialHold.initialHoldFlag = true;
		initialHold.holdDisable = false;
		initialHold.initialHoldContinuousUse = false;
		initialHold.statMove();

		for(boolean initialKick : new boolean[]{false, true}) {
			GameEngine kick = moveEngine();
			kick.nowPieceObject = new RotationCollisionPiece(false, true, false);
			kick.wallkick = (x, y, rtDir, rtOld, rtNew, allowUpward, piece, field, ctrl) ->
				new nullpomino.game.component.WallkickResult(0, -1, rtNew);
			kick.ruleopt.rotateWallkick = true;
			kick.ruleopt.rotateInitialWallkick = initialKick;
			kick.ruleopt.lockresetWallkick = true;
			kick.initialRotateDirection = 1;
			kick.statMove();
		}

		GameEngine noWallkickLimit = moveEngine();
		noWallkickLimit.nowPieceObject = new RotationCollisionPiece(false, true, false);
		noWallkickLimit.wallkick = (x, y, rtDir, rtOld, rtNew, allowUpward, piece, field, ctrl) ->
			new nullpomino.game.component.WallkickResult(0, 0, rtNew);
		noWallkickLimit.ruleopt.rotateWallkick = true;
		noWallkickLimit.ruleopt.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_NOWALLKICK;
		noWallkickLimit.ruleopt.lockresetLimitShareCount = false;
		noWallkickLimit.ruleopt.lockresetLimitRotate = 100;
		press(noWallkickLimit, Controller.BUTTON_A);
		noWallkickLimit.statMove();
		GameEngine exceededWallkickLimit = moveEngine();
		exceededWallkickLimit.nowPieceObject = new RotationCollisionPiece(false, true, false);
		exceededWallkickLimit.wallkick = (x, y, rtDir, rtOld, rtNew, allowUpward, piece, field, ctrl) ->
			new nullpomino.game.component.WallkickResult(0, 0, rtNew);
		exceededWallkickLimit.ruleopt.rotateWallkick = true;
		exceededWallkickLimit.ruleopt.lockresetWallkick = true;
		exceededWallkickLimit.ruleopt.lockresetLimitOver = RuleOptions.LOCKRESET_LIMIT_OVER_NOWALLKICK;
		exceededWallkickLimit.ruleopt.lockresetLimitShareCount = false;
		exceededWallkickLimit.ruleopt.lockresetLimitRotate = 0;
		exceededWallkickLimit.extendedRotateCount = 0;
		press(exceededWallkickLimit, Controller.BUTTON_A);
		exceededWallkickLimit.statMove();

		for(int quickCase = 0; quickCase < 4; quickCase++) {
			GameEngine quick = moveEngine();
			quick.dominoQuickTurn = true;
			quick.nowPieceObject = new RotationCollisionPiece(
				quickCase >= 2, true, quickCase == 3);
			quick.nowPieceObject.id = quickCase == 0 ? Piece.PIECE_T : Piece.PIECE_I2;
			quick.nowPieceRotateFailCount = quickCase >= 2 ? 1 : 0;
			quick.ruleopt.rotateWallkick = false;
			press(quick, Controller.BUTTON_A);
			quick.statMove();
		}
		GameEngine quickAir = moveEngine();
		quickAir.dominoQuickTurn = true;
		quickAir.nowPieceObject = new RotationCollisionPiece(false, true, false);
		quickAir.nowPieceObject.id = Piece.PIECE_I2;
		quickAir.nowPieceRotateFailCount = 1;
		quickAir.ruleopt.rotateWallkick = false;
		press(quickAir, Controller.BUTTON_A);
		quickAir.statMove();

		GameEngine exhaustedRotate = moveEngine();
		exhaustedRotate.ruleopt.lockresetRotate = true;
		exhaustedRotate.ruleopt.lockresetLimitShareCount = false;
		exhaustedRotate.ruleopt.lockresetLimitRotate = 0;
		exhaustedRotate.extendedRotateCount = 0;
		press(exhaustedRotate, Controller.BUTTON_A);
		exhaustedRotate.statMove();
		GameEngine noRotateReset = moveEngine();
		noRotateReset.ruleopt.lockresetRotate = false;
		press(noRotateReset, Controller.BUTTON_A);
		noRotateReset.statMove();

		for(int endingCase = 0; endingCase < 3; endingCase++) {
			GameEngine death = moveEngine();
			death.statc[0] = 0;
			death.nowPieceObject = new RotationCollisionPiece(true, true, true);
			death.ruleopt.pieceEnterMaxDistanceY = 0;
			death.ending = endingCase == 0 ? 0 : 2;
			death.staffrollNoDeath = endingCase == 2;
			invokeHoldAndRotate(death);
		}
	}

	private static boolean invokeHoldAndRotate(GameEngine engine) throws Exception {
		var method = GameEngine.class.getDeclaredMethod("statMoveHoldAndRotate");
		method.setAccessible(true);
		return (boolean)method.invoke(engine);
	}

	@Test
	void groundAndPostLockResidualPredicates() throws Exception {
		GameEngine notGroundedForFrame = lockEngine(true, false);
		notGroundedForFrame.statc[0] = 0;
		notGroundedForFrame.ruleopt.moveFirstFrame = false;
		invokeGroundLock(notGroundedForFrame, false, false);

		GameEngine cap = groundEngine();
		cap.speed.lockDelay = 99;
		cap.lockDelayNow = 97;
		cap.statMove();

		GameEngine groundHardVertical = groundEngine();
		groundHardVertical.owMoveDiagonal = 0;
		groundHardVertical.ruleopt.harddropEnable = true;
		groundHardVertical.ruleopt.harddropLock = true;
		groundHardVertical.ruleopt.moveUpAndDown = false;
		press(groundHardVertical, Controller.BUTTON_UP);
		groundHardVertical.statMove();

		for(boolean surface : new boolean[]{false, true}) {
			GameEngine groundSoft = groundEngine();
			groundSoft.owMoveDiagonal = 0;
			groundSoft.ruleopt.softdropEnable = true;
			groundSoft.ruleopt.softdropLimit = true;
			groundSoft.softdropContinuousUse = true;
			groundSoft.ruleopt.softdropLock = !surface;
			groundSoft.ruleopt.softdropSurfaceLock = surface;
			groundSoft.ruleopt.moveUpAndDown = false;
			press(groundSoft, Controller.BUTTON_DOWN);
			groundSoft.statMove();
		}
		GameEngine groundSoftVertical = groundEngine();
		groundSoftVertical.owMoveDiagonal = 0;
		groundSoftVertical.ruleopt.softdropEnable = true;
		groundSoftVertical.ruleopt.softdropLock = true;
		groundSoftVertical.ruleopt.moveUpAndDown = false;
		press(groundSoftVertical, Controller.BUTTON_DOWN);
		groundSoftVertical.statMove();

		GameEngine gem = lockEngine(true, false);
		gem.clearMode = GameEngine.ClearType.GEM_COLOR;
		gem.statMove();
		GameEngine noClearMode = lockEngine(true, false);
		noClearMode.clearMode = null;
		noClearMode.statMove();
		GameEngine spinNoStats = lockEngine(true, false);
		spinNoStats.tspin = true;
		spinNoStats.ending = 1;
		spinNoStats.staffrollEnableStatistics = false;
		spinNoStats.statMove();

		GameEngine minorOld = lockEngine(true, false);
		minorOld.ending = 1;
		minorOld.versionMajor = 7.0f;
		minorOld.versionMinorOld = 0f;
		minorOld.statMove();
		GameEngine oldMajor = lockEngine(true, false);
		oldMajor.ending = 1;
		oldMajor.versionMajor = 6.0f;
		oldMajor.statMove();
		GameEngine cascadeConnected = lockEngine(true, false);
		cascadeConnected.lineGravityType = GameEngine.LineGravity.CASCADE;
		cascadeConnected.connectBlocks = true;
		cascadeConnected.statMove();
		for(boolean put : new boolean[]{false, true}) {
			for(boolean partial : new boolean[]{false, true}) {
				for(boolean fieldDeath : new boolean[]{false, true}) {
					GameEngine lockout = lockEngine(put, partial);
					lockout.ruleopt.fieldLockoutDeath = fieldDeath;
					lockout.ruleopt.fieldPartialLockoutDeath = !fieldDeath;
					lockout.statMove();
				}
			}
		}

		for(boolean before : new boolean[]{false, true}) {
			GameEngine line = lockEngine(true, false);
			line.lineClearing = 1;
			line.ruleopt.lockflash = 1;
			line.ruleopt.lockflashBeforeLineClear = before;
			line.ruleopt.lockflashOnlyFrame = false;
			line.speed.are = 0;
			line.statMove();
		}
		for(boolean before : new boolean[]{false, true}) {
			GameEngine line = lockEngine(true, false);
			line.nowPieceObject = new FullLineCollisionPiece();
			line.ruleopt.lockflash = 1;
			line.ruleopt.lockflashBeforeLineClear = before;
			line.ruleopt.lockflashOnlyFrame = false;
			line.speed.are = 0;
			line.statMove();
		}

		GameEngine lagOnly = lockEngine(true, false);
		lagOnly.speed.are = 0;
		lagOnly.lagARE = true;
		lagOnly.statMove();
		GameEngine areOnly = lockEngine(true, false);
		areOnly.speed.are = 1;
		areOnly.lagARE = false;
		areOnly.statMove();
		GameEngine neitherAre = lockEngine(true, false);
		neitherAre.speed.are = 0;
		neitherAre.lagARE = false;
		neitherAre.statMove();
		GameEngine recursive = lockEngine(true, false);
		recursive.speed.are = 0;
		recursive.ruleopt.moveFirstFrame = false;
		recursive.statMove();
	}

	private static boolean invokeGroundLock(GameEngine engine, boolean side, boolean updown)
		throws Exception {
		var method = GameEngine.class.getDeclaredMethod("statMoveGroundLock",
			boolean.class, boolean.class);
		method.setAccessible(true);
		return (boolean)method.invoke(engine, side, updown);
	}

	@Test
	void lineClearAndAreFinalResidualPredicates() {
		GameEngine halfZero = lineClearEngine(1);
		halfZero.big = true;
		halfZero.bighalf = true;
		halfZero.comboType = GameEngine.COMBO_TYPE_NORMAL;
		halfZero.statLineClear();
		GameEngine chainedCombo = lineClearEngine(1);
		chainedCombo.comboType = GameEngine.COMBO_TYPE_NORMAL;
		chainedCombo.chain = 1;
		chainedCombo.statLineClear();

		GameEngine gem = freshEngine();
		gem.field = new GemCountField();
		gem.stat = GameEngine.Status.LINECLEAR;
		gem.clearMode = GameEngine.ClearType.GEM_COLOR;
		gem.speed.lineDelay = 100;
		gem.statLineClear();

		GameEngine nullLineBlock = freshEngine();
		nullLineBlock.field = new NullLineBlockField();
		nullLineBlock.stat = GameEngine.Status.LINECLEAR;
		nullLineBlock.clearMode = GameEngine.ClearType.LINE;
		nullLineBlock.speed.lineDelay = 100;
		nullLineBlock.statLineClear();

		GameEngine colorBlock = engineWithField();
		colorBlock.stat = GameEngine.Status.LINECLEAR;
		colorBlock.clearMode = GameEngine.ClearType.COLOR;
		colorBlock.colorClearSize = 1;
		colorBlock.speed.lineDelay = 100;
		colorBlock.field.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		colorBlock.statLineClear();
		GameEngine eraseBlock = freshEngine();
		eraseBlock.field = new EraseBlockField();
		eraseBlock.stat = GameEngine.Status.LINECLEAR;
		eraseBlock.clearMode = GameEngine.ClearType.COLOR;
		eraseBlock.colorClearSize = 1;
		eraseBlock.speed.lineDelay = 100;
		eraseBlock.statLineClear();

		for(int fallCase = 0; fallCase < 4; fallCase++) {
			GameEngine fall = engineWithField();
			fall.stat = GameEngine.Status.LINECLEAR;
			fall.clearMode = GameEngine.ClearType.LINE;
			fall.lineGravityType = GameEngine.LineGravity.NATIVE;
			fall.lineClearing = fallCase == 0 ? 3 : 1;
			fall.speed.lineDelay = fallCase == 0 ? 0 : 5;
			fall.statc[0] = fallCase == 1 ? 1 : 5;
			fall.ruleopt.lineFallAnim = fallCase == 3;
			fall.statLineClear();
		}

		GameEngine maxChain = cascadeEngine(GameEngine.ClearType.LINE, true);
		maxChain.statistics.maxChain = 10;
		maxChain.statLineClear();
		for(int sticky : new int[]{0, 1}) {
			GameEngine stickyDelay = cascadeEngine(GameEngine.ClearType.LINE, false);
			stickyDelay.cascadeClearDelay = 1;
			stickyDelay.sticky = sticky;
			stickyDelay.statLineClear();
		}
		GameEngine unknownCombo = lineClearEngine(1);
		unknownCombo.comboType = 99;
		unknownCombo.statLineClear();

		for(int cancelKind = 0; cancelKind < 3; cancelKind++) {
			GameEngine are = freshEngine();
			are.stat = GameEngine.Status.ARE;
			are.statc[1] = 100;
			are.lagARE = true;
			are.ruleopt.areCancelMove = cancelKind == 0;
			are.ruleopt.areCancelRotate = cancelKind == 1;
			are.ruleopt.areCancelHold = cancelKind == 2;
			are.statARE();
		}

		GameEngine areLastFalse = freshEngine();
		areLastFalse.stat = GameEngine.Status.ARE;
		areLastFalse.statc[0] = 8;
		areLastFalse.statc[1] = 10;
		areLastFalse.ruleopt.dasInARE = true;
		areLastFalse.ruleopt.dasInARELastFrame = false;
		areLastFalse.ruleopt.dasRedirectInDelay = true;
		areLastFalse.statARE();

		GameEngine noRedirect = freshEngine();
		noRedirect.stat = GameEngine.Status.ARE;
		noRedirect.statc[1] = 100;
		noRedirect.ruleopt.dasInARE = false;
		noRedirect.ruleopt.dasRedirectInDelay = false;
		noRedirect.statARE();

		GameEngine lockRedirect = freshEngine();
		lockRedirect.stat = GameEngine.Status.LOCKFLASH;
		lockRedirect.ruleopt.lockflash = 100;
		lockRedirect.ruleopt.dasInLockFlash = false;
		lockRedirect.ruleopt.dasRedirectInDelay = true;
		lockRedirect.statLockFlash();
		GameEngine lockNoRedirect = freshEngine();
		lockNoRedirect.stat = GameEngine.Status.LOCKFLASH;
		lockNoRedirect.ruleopt.lockflash = 100;
		lockNoRedirect.ruleopt.dasInLockFlash = false;
		lockNoRedirect.ruleopt.dasRedirectInDelay = false;
		lockNoRedirect.statLockFlash();

		GameEngine lineNoRedirect = engineWithField();
		lineNoRedirect.stat = GameEngine.Status.LINECLEAR;
		lineNoRedirect.statc[0] = 1;
		lineNoRedirect.speed.lineDelay = 100;
		lineNoRedirect.ruleopt.dasInLineClear = false;
		lineNoRedirect.ruleopt.dasRedirectInDelay = false;
		lineNoRedirect.statLineClear();
	}

	@Test
	void customEndingAndFieldEditFalseSidePredicates() {
		HookMode mode = new HookMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].statCustom();
		freshEngine().statCustom();

		GameEngine endingNoRedirect = engineWithField();
		endingNoRedirect.ruleopt.dasInEndingStart = false;
		endingNoRedirect.ruleopt.dasRedirectInDelay = false;
		endingNoRedirect.statEndingStart();

		GameEngine edit = engineWithField();
		edit.stat = GameEngine.Status.FIELDEDIT;
		edit.fldeditFrames = 9;
		press(edit, Controller.BUTTON_D);
		edit.statFieldEdit();
	}

	private static final class SequencePiece extends Piece {
		private final boolean[] results;
		private int index;

		SequencePiece(boolean... results) {
			super(Piece.PIECE_T);
			this.results = results;
		}

		private boolean nextResult() {
			return results[Math.min(index++, results.length - 1)];
		}

		@Override public boolean checkCollision(int x, int y, Field field) {
			return nextResult();
		}

		@Override public boolean checkCollision(int x, int y, int direction, Field field) {
			return nextResult();
		}
	}

	private static final class LockPiece extends Piece {
		private final boolean put;
		private final boolean partial;

		LockPiece(boolean put, boolean partial) {
			super(Piece.PIECE_T);
			this.put = put;
			this.partial = partial;
		}

		@Override public boolean checkCollision(int x, int y, Field field) {
			return true;
		}

		@Override public boolean checkCollision(int x, int y, int direction, Field field) {
			return true;
		}

		@Override public boolean placeToField(int x, int y, Field field) {
			return put;
		}

		@Override public boolean isPartialLockOut(int x, int y, Field field) {
			return partial;
		}
	}

	private static final class FullLineCollisionPiece extends Piece {
		private final int lines;

		FullLineCollisionPiece() {
			this(1);
		}

		FullLineCollisionPiece(int lines) {
			super(Piece.PIECE_T);
			this.lines = lines;
		}

		@Override public boolean checkCollision(int x, int y, Field field) {
			return true;
		}

		@Override public boolean placeToField(int x, int y, Field field) {
			for(int row = field.getHeight() - lines; row < field.getHeight(); row++) {
				for(int column = 0; column < field.getWidth(); column++) {
					field.setBlockColor(column, row, Block.BLOCK_COLOR_RED);
				}
			}
			return true;
		}
	}

	private static final class RotationCollisionPiece extends Piece {
		private final boolean groundCollision;
		private final boolean rotationCollision;
		private final boolean quickCollision;
		private int rotationChecks;

		RotationCollisionPiece(boolean groundCollision, boolean rotationCollision,
			boolean quickCollision) {
			super(Piece.PIECE_T);
			this.groundCollision = groundCollision;
			this.rotationCollision = rotationCollision;
			this.quickCollision = quickCollision;
		}

		@Override public boolean checkCollision(int x, int y, Field field) {
			return groundCollision;
		}

		@Override public boolean checkCollision(int x, int y, int direction, Field field) {
			return rotationChecks++ == 0 ? rotationCollision : quickCollision;
		}
	}

	private static final class AlwaysBlockField extends Field {
		private final Block block;

		AlwaysBlockField(int color) {
			super(1, 1, 0, false);
			block = new Block(color, 0, 0);
			block.color = color;
		}

		@Override public Block getBlock(int x, int y) {
			return block;
		}
	}

	private static final class GemCountField extends Field {
		GemCountField() {
			super(2, 2, 0, false);
		}

		@Override public int gemColorCheck(int size, boolean flag, boolean garbage,
			boolean ignoreHidden) {
			return 1;
		}

		@Override public int gemClearColor(int size, boolean garbage, boolean ignoreHidden) {
			return 1;
		}

		@Override public int getHowManyGemClears() {
			return 1;
		}
	}

	private static final class NullLineBlockField extends Field {
		NullLineBlockField() {
			super(1, 1, 0, false);
		}

		@Override public int checkLine() { return 1; }
		@Override public boolean getLineFlag(int y) { return true; }
		@Override public Block getBlock(int x, int y) { return null; }
	}

	private static final class EraseBlockField extends Field {
		private final Block block;

		EraseBlockField() {
			super(2, 1, 0, false);
			block = new Block(Block.BLOCK_COLOR_RED);
			block.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		}

		@Override public int checkColor(int size, boolean flag, boolean garbage,
			boolean gemSame, boolean ignoreHidden) {
			return 1;
		}

		@Override public int clearColor(int size, boolean garbage, boolean gemSame,
			boolean ignoreHidden) {
			return 1;
		}

		@Override public Block getBlock(int x, int y) { return x == 0 ? null : block; }
	}

	private static final class MixedBlockField extends Field {
		private final Block block = new Block(Block.BLOCK_COLOR_RED);

		MixedBlockField() {
			super(2, 1, 0, false);
		}

		@Override public Block getBlock(int x, int y) { return x == 0 ? null : block; }
	}

	private static final class CascadeField extends Field {
		private final GameEngine.ClearType type;
		private final boolean clearing;

		CascadeField(GameEngine.ClearType type, boolean clearing) {
			super(10, 20, 0, false);
			this.type = type;
			this.clearing = clearing;
		}

		@Override public int checkLine() {
			return type == GameEngine.ClearType.LINE && clearing ? 1 : 0;
		}

		@Override public int checkLineNoFlag() {
			return type == GameEngine.ClearType.LINE && clearing ? 1 : 0;
		}

		@Override public int checkColor(int size, boolean flag, boolean garbage,
			boolean gemSame, boolean ignoreHidden) {
			return type == GameEngine.ClearType.COLOR && clearing ? 1 : 0;
		}

		@Override public int checkLineColor(int size, boolean flag, boolean diagonals,
			boolean gemSame) {
			return type == GameEngine.ClearType.LINE_COLOR && clearing ? 1 : 0;
		}

		@Override public int gemColorCheck(int size, boolean flag, boolean garbage,
			boolean ignoreHidden) {
			return type == GameEngine.ClearType.GEM_COLOR && clearing ? 1 : 0;
		}
	}

	private static final class HookMode extends AbstractMode {
		GameEngine.Status statusAfterLock;
		GameEngine.Status statusAfterLine;
		boolean stopMove;
		boolean stopEnding;

		@Override public String getName() { return "hook"; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void renderInput(GameEngine engine, int playerID) {}
		@Override public boolean onMove(GameEngine engine, int playerID) { return stopMove; }
		@Override public boolean onEndingStart(GameEngine engine, int playerID) { return stopEnding; }
		@Override public void pieceLocked(GameEngine engine, int playerID, int lines) {
			if(statusAfterLock != null) engine.stat = statusAfterLock;
		}
		@Override public boolean lineClearEnd(GameEngine engine, int playerID) {
			if(statusAfterLine != null) engine.stat = statusAfterLine;
			return false;
		}
	}

	private static final class NamedMode extends AbstractMode {
		@Override public String getName() { return "matrix-mode"; }
		@Override public void modeInit(GameManager manager) {}
		@Override public void playerInit(GameEngine engine, int playerID) {}
		@Override public void renderInput(GameEngine engine, int playerID) {}
	}
}
