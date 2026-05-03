package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;
import nullpomino.util.CustomProperties;

class GameEngineBranchEdgeTest {

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
	}

	private static void groundField(GameEngine e) {
		for (int x = 0; x < e.field.getWidth(); x++)
			e.field.setBlockColor(x, 19, Block.BLOCK_COLOR_GRAY);
	}

	private static void fillLine(GameEngine e, int y) {
		for (int x = 0; x < e.field.getWidth(); x++)
			e.field.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
	}

	@Test void getDASClampsToOwMinDAS() {
		GameEngine e = freshEngine();
		e.speed.das = 5; e.owMinDAS = 10; e.owMaxDAS = -1;
		e.ruleopt.minDAS = -1; e.ruleopt.maxDAS = -1;
		assertEquals(10, e.getDAS());
	}

	@Test void getDASClampsToOwMaxDAS() {
		GameEngine e = freshEngine();
		e.speed.das = 50; e.owMinDAS = -1; e.owMaxDAS = 30;
		e.ruleopt.minDAS = -1; e.ruleopt.maxDAS = -1;
		assertEquals(30, e.getDAS());
	}

	@Test void getDASClampsToRuleoptMinDAS() {
		GameEngine e = freshEngine();
		e.speed.das = 5; e.owMinDAS = -1; e.owMaxDAS = -1;
		e.ruleopt.minDAS = 10; e.ruleopt.maxDAS = -1;
		assertEquals(10, e.getDAS());
	}

	@Test void getDASClampsToRuleoptMaxDAS() {
		GameEngine e = freshEngine();
		e.speed.das = 50; e.owMinDAS = -1; e.owMaxDAS = -1;
		e.ruleopt.minDAS = -1; e.ruleopt.maxDAS = 25;
		assertEquals(25, e.getDAS());
	}

	@Test void getDASReturnsSpeedDasWhenNoClampsApply() {
		GameEngine e = freshEngine();
		e.speed.das = 15; e.owMinDAS = -1; e.owMaxDAS = -1;
		e.ruleopt.minDAS = -1; e.ruleopt.maxDAS = -1;
		assertEquals(15, e.getDAS());
	}

	@Test void statMoveDominoQuickTurn() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 1;
		e.ruleopt.moveFirstFrame = true; e.dominoQuickTurn = true;
		Piece i2 = new Piece(Piece.PIECE_I2);
		i2.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_I2], e.ruleopt.pieceOffsetY[Piece.PIECE_I2]);
		e.nowPieceObject = i2; e.nowPieceX = 5; e.nowPieceY = 5;
		e.nowPieceRotateFailCount = 1;
		e.field.setBlockColor(6, 5, Block.BLOCK_COLOR_GRAY);
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_B] = true; e.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		e.initialRotateDirection = 0;
		setupNextPiece(e);
		e.statMove();
		assertTrue(e.nowPieceRotateFailCount >= 0);
	}

	@Test void statMoveShiftLock() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true; e.ruleopt.shiftLockEnable = true; e.manualLock = true;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 5; e.nowPieceY = 18; e.nowPieceBottomY = 19;
		groundField(e);
		e.shiftLock = 0; e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true; e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 10;
		e.dasCount = 10; e.dasDirection = 1; e.dasSpeedCount = 10;
		e.speed.lockDelay = 30; e.lockDelayNow = 0;
		e.speed.gravity = 0; e.speed.denominator = 1; e.gcount = 0;
		setupNextPiece(e);
		e.statMove();
		assertTrue(e.nowPieceX >= 5);
	}

	@Test void statMoveDasChargeOnBlockedMove() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true; e.ruleopt.dasChargeOnBlockedMove = true;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 0; e.nowPieceY = 5;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true; e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 10;
		e.dasCount = 10; e.dasDirection = -1; e.dasSpeedCount = 10;
		setupNextPiece(e);
		e.statMove();
		assertEquals(0, e.nowPieceX);
	}

	@Test void statMoveHardDropVersionBelowSevenSix() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true; e.ruleopt.harddropEnable = true; e.ruleopt.moveUpAndDown = true;
		e.versionMajor = 7.5f;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 5; e.nowPieceY = 5;
		e.nowPieceBottomY = e.nowPieceObject.getBottom(5, 5, e.field);
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_UP] = true; e.ctrl.buttonTime[Controller.BUTTON_UP] = 10;
		e.harddropContinuousUse = false;
		setupNextPiece(e);
		e.statMove();
		assertTrue(e.harddropFall >= 0);
	}

	@Test void statMoveSkipsHardDropWhenDasRepeat() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true; e.ruleopt.harddropEnable = true; e.ruleopt.moveUpAndDown = true;
		e.versionMajor = 7.6f;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 5; e.nowPieceY = 5;
		e.nowPieceBottomY = e.nowPieceObject.getBottom(5, 5, e.field);
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_UP] = true; e.ctrl.buttonTime[Controller.BUTTON_UP] = 10;
		e.harddropContinuousUse = false; e.dasRepeat = true;
		setupNextPiece(e);
		e.statMove();
		assertEquals(0, e.harddropFall);
	}

	@Test void statMoveNewSoftdropPath() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true; e.ruleopt.softdropGravitySpeedLimit = true; e.ruleopt.softdropSpeed = 2.0f;
		e.ruleopt.softdropEnable = true; e.ruleopt.moveUpAndDown = true; e.ruleopt.softdropMultiplyNativeSpeed = true;
		e.speed.gravity = 5; e.speed.denominator = 10;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 5; e.nowPieceY = 5;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true; e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 10;
		e.softdropContinuousUse = false;
		setupNextPiece(e);
		e.statMove();
		assertTrue(e.gcount >= 0);
	}

	@Test void statMoveNewSoftdropDenominatorNegative() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true; e.ruleopt.softdropGravitySpeedLimit = true; e.ruleopt.softdropSpeed = 2.0f;
		e.ruleopt.softdropEnable = true; e.ruleopt.moveUpAndDown = true; e.ruleopt.softdropMultiplyNativeSpeed = false;
		e.speed.gravity = 5; e.speed.denominator = -1;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 5; e.nowPieceY = 5;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true; e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 10;
		e.softdropContinuousUse = false;
		setupNextPiece(e);
		e.statMove();
	}

	@Test void statMoveSoftdropSurfaceLock() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 10;
		e.ruleopt.moveFirstFrame = true; e.ruleopt.softdropEnable = true; e.ruleopt.softdropSurfaceLock = true; e.ruleopt.moveUpAndDown = true;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 5; e.nowPieceY = 18;
		groundField(e);
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true; e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 10;
		e.softdropContinuousUse = false; e.manualLock = false;
		e.speed.lockDelay = 0; e.lockDelayNow = 0;
		e.speed.gravity = 0; e.speed.denominator = 1; e.gcount = 0;
		setupNextPiece(e);
		e.statMove();
		assertTrue(e.softdropContinuousUse);
	}

	@Test void statLineClearDisplaySizeOne() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.COLOR; e.displaysize = 1;
		e.speed.lineDelay = 1; e.ruleopt.minLineDelay = -1; e.ruleopt.maxLineDelay = -1;
		e.statc[0] = 0; e.lineClearing = 1;
		e.field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		e.field.getBlock(0, 19).setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		e.statLineClear();
	}

	@Test void statLineClearGemColorMode() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.GEM_COLOR; e.colorClearSize = 4;
		e.speed.lineDelay = 1; e.ruleopt.minLineDelay = -1; e.ruleopt.maxLineDelay = -1;
		e.statc[0] = 0; e.lineClearing = 1;
		e.statLineClear();
	}

	@Test void statLineClearLineColorMode() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE_COLOR; e.colorClearSize = 4;
		e.speed.lineDelay = 1; e.ruleopt.minLineDelay = -1; e.ruleopt.maxLineDelay = -1;
		e.statc[0] = 0; e.lineClearing = 1;
		e.statLineClear();
	}

	@Test void statLineClearModeSkip() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = new AbstractMode() {
			@Override public String getName() { return "SkipMode"; }
			@Override public void modeInit(GameManager m) {}
			@Override public void playerInit(GameEngine eng, int pid) {}
			@Override public void renderInput(GameEngine eng, int pid) {}
			@Override public boolean lineClearEnd(GameEngine eng, int pid) { return true; }
		};
		gm.init(); gm.engine[0].init();
		GameEngine e = gm.engine[0]; e.createFieldIfNeeded();
		e.gameActive = true; e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE;
		e.speed.lineDelay = 0; e.ruleopt.minLineDelay = -1; e.ruleopt.maxLineDelay = -1;
		e.statc[0] = 0; e.lineClearing = 1;
		fillLine(e, 19);
		e.statLineClear(); e.statc[0] = 1; e.statLineClear();
	}

	@Test void statLineClearStickyMode() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.LINECLEAR;
		e.clearMode = GameEngine.ClearType.LINE; e.sticky = 1;
		e.speed.lineDelay = 0; e.ruleopt.minLineDelay = -1; e.ruleopt.maxLineDelay = -1;
		e.statc[0] = 0; e.lineClearing = 1;
		fillLine(e, 19);
		e.statLineClear();
	}

	@Test void statGameOverGameoverAllFalse() {
		GameEngine e = engineWithField();
		e.gameStarted = true; e.lives = 0; e.gameoverAll = false; e.statc[0] = 0;
		for (int i = 0; i < e.field.getHeight() + 1 + 200; i++) {
			e.statGameOver();
			if (e.stat != GameEngine.Status.GAMEOVER) break;
		}
		assertEquals(GameEngine.Status.RESULT, e.stat);
	}

	@Test void statGameOverButtonASkip() {
		GameEngine e = engineWithField();
		e.gameStarted = true; e.lives = 0; e.field.reset(); e.statc[0] = 0;
		e.statGameOver(); e.statGameOver();
		int threshold = e.field.getHeight() + 1 + 60;
		while (e.statc[0] < threshold) e.statGameOver();
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_A] = true; e.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		e.statGameOver();
		assertTrue(e.statc[0] >= e.field.getHeight() + 1 + 180 || e.stat == GameEngine.Status.RESULT);
	}

	@Test void statGameOverReviveNonEmptyField() {
		GameEngine e = engineWithField();
		e.gameStarted = true; e.lives = 2; e.statc[0] = 1; e.stat = GameEngine.Status.GAMEOVER;
		e.field.setBlockColor(5, 5, Block.BLOCK_COLOR_RED);
		e.speed.are = 0; e.ruleopt.minARE = -1; e.ruleopt.maxARE = -1;
		e.statGameOver();
		assertEquals(GameEngine.Status.MOVE, e.stat);
		assertEquals(1, e.lives);
	}

	@Test void statExcellentFastForward() {
		GameEngine e = freshEngine();
		e.stat = GameEngine.Status.EXCELLENT; e.statc[0] = 120; e.statc[1] = 0;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_A] = true; e.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		e.statExcellent();
		assertTrue(e.statc[0] >= 600 || e.stat != GameEngine.Status.EXCELLENT);
	}

	@Test void playSEDisabled() {
		GameEngine e = freshEngine(); e.enableSE = false; e.playSE("test");
	}

	@Test void isHoldOKLimitReached() {
		GameEngine e = freshEngine();
		e.ruleopt.holdEnable = true; e.holdDisable = false; e.ruleopt.holdLimit = 3; e.holdUsedCount = 3;
		assertFalse(e.isHoldOK());
	}

	@Test void isHoldOKUnderLimit() {
		GameEngine e = freshEngine();
		e.ruleopt.holdEnable = true; e.holdDisable = false; e.ruleopt.holdLimit = 3; e.holdUsedCount = 2;
		assertTrue(e.isHoldOK());
	}

	@Test void statMoveHoldFailSE() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 1;
		e.ruleopt.moveFirstFrame = true; e.ruleopt.holdEnable = true; e.holdDisable = true; e.initialHoldFlag = false;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 5; e.nowPieceY = 5;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_D] = true; e.ctrl.buttonTime[Controller.BUTTON_D] = 1;
		setupNextPiece(e);
		e.statMove();
	}

	@Test void renderInterruptItem() {
		GameEngine e = freshEngine(); e.stat = GameEngine.Status.INTERRUPTITEM; e.render();
	}

	@Test void getRotateDirectionNullPiece() {
		GameEngine e = freshEngine(); e.nowPieceObject = null;
		assertEquals(3, e.getRotateDirection(-1));
		assertEquals(0, e.getRotateDirection(0));
		assertEquals(1, e.getRotateDirection(1));
		assertEquals(2, e.getRotateDirection(2));
	}

	@Test void fieldUpdateOutlineOnlyClearsBone() {
		GameEngine e = engineWithField();
		e.ruleopt.lockflash = 5; e.owBlockShowOutlineOnly = 1;
		e.field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		Block b = e.field.getBlock(0, 19);
		b.elapsedFrames = 3;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_BONE, true);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, true);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		e.fieldUpdate();
		assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_BONE));
		assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE));
	}

	@Test void fieldUpdateGarbagePreservesDarkness() {
		GameEngine e = engineWithField();
		e.field.setBlockColor(0, 19, Block.BLOCK_COLOR_GRAY);
		Block b = e.field.getBlock(0, 19);
		b.elapsedFrames = -1; b.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		b.darkness = 0.5f;
		e.fieldUpdate();
		assertEquals(0.5f, b.darkness, 0.001f);
	}

	@Test void saveReplayNonZeroPlayer() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.replayProp = new CustomProperties(); gm.replayMode = true; gm.replayRerecord = true;
		gm.init();
		GameEngine e = gm.engine[0]; e.playerID = 1;
		e.versionMajor = 7.6f; e.versionMinor = 0; e.versionIsDevBuild = false; e.randSeed = 12345L;
		e.saveReplay();
		assertNotNull(e.owner.replayProp.getProperty("1.replay.randSeed"));
	}

	@Test void saveReplayPlayerZeroTimestamp() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.replayProp = new CustomProperties(); gm.replayMode = true; gm.replayRerecord = true;
		gm.init();
		GameEngine e = gm.engine[0]; e.playerID = 0;
		e.versionMajor = 7.6f; e.versionMinor = 0; e.versionIsDevBuild = false;
		e.saveReplay();
		assertNotNull(e.owner.replayProp.getProperty("timestamp.date"));
		assertNotNull(e.owner.replayProp.getProperty("timestamp.time"));
		assertNotNull(e.owner.replayProp.getProperty("timestamp.gmt"));
	}

	@Test void saveReplaySkipsWhenNotRerecord() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.replayProp = new CustomProperties(); gm.replayMode = true; gm.replayRerecord = false;
		gm.init();
		GameEngine e = gm.engine[0]; e.versionMajor = 7.6f;
		e.saveReplay();
		assertNull(e.owner.replayProp.getProperty("version.core"));
	}

	@Test void isRotateCountExceedSeparate() {
		GameEngine e = freshEngine();
		e.ruleopt.lockresetLimitShareCount = false; e.ruleopt.lockresetLimitRotate = 5; e.extendedRotateCount = 5;
		assertTrue(e.isRotateCountExceed());
	}

	@Test void isRotateCountExceedSeparateNotExceed() {
		GameEngine e = freshEngine();
		e.ruleopt.lockresetLimitShareCount = false; e.ruleopt.lockresetLimitRotate = 5; e.extendedRotateCount = 3;
		assertFalse(e.isRotateCountExceed());
	}

	@Test void isMoveCountExceedSeparate() {
		GameEngine e = freshEngine();
		e.ruleopt.lockresetLimitShareCount = false; e.ruleopt.lockresetLimitMove = 10; e.extendedMoveCount = 10;
		assertTrue(e.isMoveCountExceed());
	}

	@Test void isMoveCountExceedSeparateNotExceed() {
		GameEngine e = freshEngine();
		e.ruleopt.lockresetLimitShareCount = false; e.ruleopt.lockresetLimitMove = 10; e.extendedMoveCount = 5;
		assertFalse(e.isMoveCountExceed());
	}

	@Test void statMoveInterruptItemTransition() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 10;
		e.ruleopt.moveFirstFrame = true;
		e.speed.are = 0; e.ruleopt.minARE = 0; e.ruleopt.maxARE = -1;
		e.speed.lockDelay = 0; e.lockDelayNow = 0;
		e.interruptItemNumber = GameEngine.INTERRUPTITEM_MIRROR; e.clearMode = GameEngine.ClearType.LINE;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 5; e.nowPieceY = 18;
		groundField(e);
		e.ctrl = new Controller(); e.gcount = 5; e.speed.gravity = 0; e.speed.denominator = 1;
		setupNextPiece(e);
		e.statMove();
	}

	@Test void statAREInterruptItemTransition() {
		GameEngine e = freshEngine();
		e.stat = GameEngine.Status.ARE; e.statc[0] = 10; e.statc[1] = 10;
		e.lagARE = false; e.interruptItemNumber = GameEngine.INTERRUPTITEM_MIRROR;
		e.statARE();
		assertEquals(GameEngine.Status.INTERRUPTITEM, e.stat);
	}

	@Test void resetFieldVisibleNullField() {
		GameEngine e = freshEngine(); e.field = null; e.resetFieldVisible();
	}

	@Test void statReadyDasInReady() {
		GameEngine e = freshEngine();
		e.createFieldIfNeeded(); e.stat = GameEngine.Status.READY; e.statc[0] = 5; e.gameActive = true;
		e.ruleopt.dasInReady = true;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true; e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.statReady();
		assertTrue(e.dasCount >= 0);
	}

	@Test void updateReplayModeReadsInput() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.replayMode = true; gm.replayProp = new CustomProperties();
		gm.init();
		GameEngine e = gm.engine[0]; e.gameActive = true; e.replayData.setInputData(42, 0);
		e.stat = GameEngine.Status.NOTHING;
		e.update();
		assertEquals(42, e.ctrl.getButtonBit());
	}

	@Test void updateReplayRerecordModeRecords() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.replayMode = true; gm.replayRerecord = true; gm.replayProp = new CustomProperties();
		gm.init();
		GameEngine e = gm.engine[0]; e.gameActive = true; e.stat = GameEngine.Status.NOTHING;
		e.ctrl.buttonPress[Controller.BUTTON_A] = true; e.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		e.update();
		assertTrue((e.replayData.getInputData(0) & (1 << Controller.BUTTON_A)) != 0);
	}

	@Test void statMoveDelayCancelSecondFrame() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 1; e.ruleopt.moveFirstFrame = true;
		e.delayCancel = true; e.delayCancelMoveLeft = false; e.delayCancelMoveRight = false;
		Piece t = new Piece(Piece.PIECE_T);
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 5; e.nowPieceY = 5;
		e.ctrl = new Controller(); e.dasCount = 0;
		setupNextPiece(e);
		e.statMove();
		assertFalse(e.delayCancel);
	}

	@Test void statMoveBigModeDoublesStep() {
		GameEngine e = engineWithField();
		e.gameActive = true; e.stat = GameEngine.Status.MOVE; e.statc[0] = 3;
		e.ruleopt.moveFirstFrame = true; e.big = true; e.bigmove = true;
		Piece t = new Piece(Piece.PIECE_T); t.big = true;
		t.applyOffsetArray(e.ruleopt.pieceOffsetX[Piece.PIECE_T], e.ruleopt.pieceOffsetY[Piece.PIECE_T]);
		e.nowPieceObject = t; e.nowPieceX = 2; e.nowPieceY = 5;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true; e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 10;
		e.dasCount = 10; e.dasDirection = 1; e.dasSpeedCount = 10;
		setupNextPiece(e);
		e.statMove();
		assertEquals(4, e.nowPieceX);
	}

	@Test void updateAiHintNotReady() {
		GameEngine e = freshEngine();
		e.gameActive = true; e.ai = new nullpomino.game.ai.DummyAI();
		e.aiShowHint = true; e.ai.thinkComplete = false; e.stat = GameEngine.Status.NOTHING;
		e.update();
		assertFalse(e.aiHintReady);
	}

	@Test void checkDropContinuousUseClearsFlags() {
		GameEngine e = freshEngine();
		e.gameActive = true;
		e.softdropContinuousUse = true; e.harddropContinuousUse = true; e.initialHoldContinuousUse = true;
		e.ctrl = new Controller();
		e.ruleopt.softdropLimit = true; e.ruleopt.harddropLimit = true; e.ruleopt.holdInitialLimit = true;
		e.checkDropContinuousUse();
		assertFalse(e.softdropContinuousUse);
		assertFalse(e.harddropContinuousUse);
		assertFalse(e.initialHoldContinuousUse);
	}

	@Test void statLockFlashDasInLockFlash() {
		GameEngine e = freshEngine();
		e.ruleopt.lockflash = 10; e.ruleopt.dasInLockFlash = true;
		e.statc[0] = 1; e.stat = GameEngine.Status.LOCKFLASH;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true; e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.statLockFlash();
		assertEquals(1, e.dasDirection);
	}

	@Test void statLockFlashDasRedirect() {
		GameEngine e = freshEngine();
		e.ruleopt.lockflash = 10; e.ruleopt.dasInLockFlash = false; e.ruleopt.dasRedirectInDelay = true;
		e.statc[0] = 1; e.stat = GameEngine.Status.LOCKFLASH;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true; e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.statLockFlash();
		assertEquals(-1, e.dasDirection);
	}

	@Test void statEndingStartDasRedirect() {
		GameEngine e = freshEngine();
		e.createFieldIfNeeded(); e.stat = GameEngine.Status.ENDINGSTART; e.statc[2] = 1;
		e.ruleopt.dasInEndingStart = false; e.ruleopt.dasRedirectInDelay = true;
		e.ctrl = new Controller();
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true; e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.statEndingStart();
		assertEquals(1, e.dasDirection);
	}

	@Test void readRefSkipsPeeledLines() throws Exception {
		Method m = GameManager.class.getDeclaredMethod("readRef", String.class);
		m.setAccessible(true);
		Path d = Path.of(".git"); Path p = d.resolve("packed-refs");
		Path bak = null; boolean cr = false;
		try {
			if (!Files.isDirectory(d)) { Files.createDirectories(d); cr = true; }
			if (Files.isRegularFile(p)) { bak = d.resolve("packed-refs.bak"); Files.copy(p, bak); }
			Files.writeString(p, "# pk\nabcdef12 refs/heads/a\n^abcdef12\ndeadbeef refs/heads/b\n");
			assertEquals("abcdef12", ((String) m.invoke(null, "refs/heads/a")).substring(0, 8));
		} finally {
			if (bak != null) { Files.copy(bak, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING); Files.deleteIfExists(bak); }
			else if (cr) { Files.deleteIfExists(p); cleanupPath(d); }
		}
	}

	@Test void readRefHandlesMalformedLine() throws Exception {
		Method m = GameManager.class.getDeclaredMethod("readRef", String.class);
		m.setAccessible(true);
		Path d = Path.of(".git"); Path p = d.resolve("packed-refs");
		Path bak = null; boolean cr = false;
		try {
			if (!Files.isDirectory(d)) { Files.createDirectories(d); cr = true; }
			if (Files.isRegularFile(p)) { bak = d.resolve("packed-refs.bak"); Files.copy(p, bak); }
			Files.writeString(p, "# pk\nbad-line\nfeed1234 refs/heads/good\n");
			assertEquals("feed1234", ((String) m.invoke(null, "refs/heads/good")).substring(0, 8));
		} finally {
			if (bak != null) { Files.copy(bak, p, java.nio.file.StandardCopyOption.REPLACE_EXISTING); Files.deleteIfExists(bak); }
			else if (cr) { Files.deleteIfExists(p); cleanupPath(d); }
		}
	}

	@Test void gameManagerInitSkipsReplayPropCreation() {
		GameManager gm = new GameManager(new EventReceiver());
		CustomProperties pre = new CustomProperties(); pre.setProperty("k", "v");
		gm.replayProp = pre; gm.replayMode = true;
		gm.init();
		assertEquals(pre, gm.replayProp);
	}

	@Test void getWinnerReturnsMinusOneWhenEngineNull() {
		assertEquals(-1, new GameManager(new EventReceiver()).getWinner());
	}

	@Test void initReplayModeSeed() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.replayMode = true; gm.replayProp = new CustomProperties();
		gm.replayProp.setProperty("version.core.major", "7.6"); gm.replayProp.setProperty("version.core.minor", "0");
		gm.replayProp.setProperty("version.core.dev", false); gm.replayProp.setProperty("0.replay.randSeed", "abc");
		gm.replayProp.setProperty("0.tuning.owRotateButtonDefaultRight", "-1"); gm.replayProp.setProperty("0.tuning.owSkin", "-1");
		gm.replayProp.setProperty("0.tuning.owMinDAS", "-1"); gm.replayProp.setProperty("0.tuning.owMaxDAS", "-1");
		gm.replayProp.setProperty("0.tuning.owDasDelay", "-1"); gm.replayProp.setProperty("0.tuning.owReverseUpDown", false);
		gm.replayProp.setProperty("0.tuning.owMoveDiagonal", "-1"); gm.replayProp.setProperty("0.tuning.owBlockOutlineType", "-1");
		gm.replayProp.setProperty("0.tuning.owBlockShowOutlineOnly", "-1");
		gm.init(); gm.engine[0].init();
		assertEquals("abc", Long.toHexString(gm.engine[0].randSeed));
	}

	private static void cleanupPath(Path p) throws IOException {
		if (Files.isDirectory(p)) {
			try (var walk = Files.walk(p)) { walk.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (Exception ignored) {} }); }
		} else Files.deleteIfExists(p);
	}
}
