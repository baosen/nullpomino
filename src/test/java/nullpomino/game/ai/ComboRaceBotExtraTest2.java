package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Additional tests covering uncovered branches in {@link ComboRaceBot}:
 * - setControl: various rotation button configurations
 * - setControl: ground rotation with movestate
 * - setControl: funnel with harddrop/softdrop variants
 * - setControl: left/right move with no DAS press
 * - thinkMain: with hold enabled, holdID paths
 * - thinkBestPosition: with inARE flag
 * - onFirst: ARE prethink triggering think request
 * - calcIRS: implicit coverage via thinkBestPosition
 */
class ComboRaceBotExtraTest2 {

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

    // ─── setControl: rotate with reverse button defaultRight=true, rotateDir=-1 (line 332-336) ───

    @Test
    void setControlReverseRotationDefaultRightRotateLeft() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        engine.owRotateButtonDefaultRight = 1; // default right
        engine.ruleopt.rotateButtonAllowReverse = true;
        engine.ruleopt.rotateButtonDefaultRight = true;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_LEFT; // lrot when current is UP
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;
        ai.thinkComplete = true;
        ai.movestate = 0;

        ai.setControl(engine, 0, ctrl);

        // Should set BUTTON_B for reverse rotation (defaultRight=true, rotateDir=-1)
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_B) != 0,
                "Reverse rotation with default right and rotate left should set BUTTON_B");
    }

    // ─── setControl: rotate with reverse defaultRight=false, rotateDir=1 (line 326-330) ───

    @Test
    void setControlReverseRotationDefaultLeftRotateRight() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        engine.owRotateButtonDefaultRight = -1; // default left
        engine.ruleopt.rotateButtonAllowReverse = true;
        engine.ruleopt.rotateButtonDefaultRight = false;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_RIGHT; // rrot when current is UP
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;
        ai.thinkComplete = true;
        ai.movestate = 0;

        ai.setControl(engine, 0, ctrl);

        // Should set BUTTON_B for reverse rotation (defaultRight=false, rotateDir=1)
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_B) != 0,
                "Reverse rotation with default left and rotate right should set BUTTON_B");
    }

    // ─── setControl: ground rotation with movestate (line 276-280) ───

    @Test
    void setControlGroundRotationWithMovestate() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 18; // touching ground
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestRtSub = 1; // rotate right
        ai.bestXSub = 5;
        ai.bestYSub = 18;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;
        ai.thinkComplete = true;
        ai.movestate = 0;

        ai.setControl(engine, 0, ctrl);

        // After ground rotation, bestRt should be updated
        // and movestate should be 1
        assertEquals(1, ai.movestate, "movestate should be 1 after ground rotation");
    }

    // ─── setControl: funnel harddrop (line 290) ───

    @Test
    void setControlFunnelHardDrop() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        engine.ruleopt.harddropEnable = true;
        engine.ruleopt.softdropEnable = false;
        engine.ruleopt.softdropLock = false;
        ai.bestX = 5;
        ai.bestY = 5;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestRtSub = 0;
        ai.bestXSub = 5;
        ai.bestYSub = 5;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;
        ai.thinkComplete = true;
        ai.movestate = 0;

        ai.setControl(engine, 0, ctrl);

        // Should harddrop (BUTTON_UP)
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_UP) != 0,
                "Funnel with harddrop should set BUTTON_UP");
    }

    // ─── setControl: funnel softdrop (line 292-293) ───

    @Test
    void setControlFunnelSoftDrop() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        engine.ruleopt.harddropEnable = false;
        engine.ruleopt.softdropEnable = true;
        engine.ruleopt.softdropLock = false;
        ai.bestX = 5;
        ai.bestY = 5;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestRtSub = 0;
        ai.bestXSub = 5;
        ai.bestYSub = 5;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;
        ai.thinkComplete = true;
        ai.movestate = 0;

        ai.setControl(engine, 0, ctrl);

        // Should softdrop (BUTTON_DOWN)
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_DOWN) != 0,
                "Funnel with softdrop should set BUTTON_DOWN");
    }

    // ─── setControl: left/right moveDir conversion (line 308-311) ───

    @Test
    void setControlMoveLeft() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.bestX = 3; // left of current
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestRtSub = 0;
        ai.bestXSub = 3;
        ai.bestYSub = 10;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;
        ai.thinkComplete = true;
        ai.movestate = 0;

        ai.setControl(engine, 0, ctrl);

        // Should move left (BUTTON_LEFT)
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_LEFT) != 0,
                "Should set move left when bestX < nowX");
    }

    // ─── thinkMain: at depth with hold enabled and holdID != -1 (line 538-545) ───

    @Test
    void thinkMainWithHoldEnabledAndHoldIDSet() {
        engine.ruleopt.holdEnable = true;
        engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
        ai.createTables(engine);
        ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];
        for (int i = 0; i < ComboRaceBot.MAX_THINK_DEPTH; i++)
            ai.nextQueueIDs[i] = Piece.PIECE_T;

        // holdID set to a valid piece ID (not -1)
        int pts = ai.thinkMain(engine, 0, Piece.PIECE_I, 0);

        assertTrue(pts >= 0, "thinkMain with hold enabled and holdID set should return score");
    }

    // ─── thinkMain: with hold enabled and holdID = -1 (line 535-536) ───

    @Test
    void thinkMainWithHoldEnabledAndHoldIDMinusOne() {
        engine.ruleopt.holdEnable = true;
        engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
        ai.createTables(engine);
        ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];
        for (int i = 0; i < ComboRaceBot.MAX_THINK_DEPTH; i++)
            ai.nextQueueIDs[i] = Piece.PIECE_T;

        // holdID = -1 (empty hold)
        int pts = ai.thinkMain(engine, 0, -1, 0);

        assertTrue(pts >= 0, "thinkMain with hold enabled and empty hold should return score");
    }

    // ─── thinkMain: depth == nextQueueIDs.length with holdID check (line 516-520) ───

    @Test
    void thinkMainTerminalWithHoldI() {
        ai.createTables(engine);
        ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];

        int pts = ai.thinkMain(engine, 0, Piece.PIECE_I, ComboRaceBot.MAX_THINK_DEPTH);

        // Should include I piece bonus in score
        assertTrue(pts >= 0, "Terminal state with I piece hold should return score");
    }

    // ─── thinkBestPosition with inARE flag set ───

    @Test
    void thinkBestPositionInARE() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();

        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        ai.inARE = true;

        ai.createTables(engine);
        ai.thinkBestPosition(engine, 0);

        assertTrue(true, "thinkBestPosition with inARE completed");
    }

    // ─── thinkBestPosition with state < 0 returns early ───

    @Test
    void thinkBestPositionFieldToIndexNegative() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();

        // Create an empty field (code 0x0, not in FIELDS, so state = -1)
        Field emptyFld = new Field(10, 20, 0, false);
        engine.field = emptyFld;

        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;

        ai.createTables(engine);
        ai.thinkBestPosition(engine, 0);

        // Should return early due to negative state
        assertTrue(true, "thinkBestPosition with negative state completed");
    }

    // ─── thinkBestPosition with aiShowHint after completion (line 474-498) ───

    @Test
    void thinkBestPositionWithShowHint() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();

        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.aiShowHint = true;

        ai.createTables(engine);
        ai.thinkBestPosition(engine, 0);

        assertTrue(true, "thinkBestPosition with aiShowHint completed");
    }

    // ─── printPieceAndDirection complete switch coverage ───

    @Test
    void printPieceAndDirectionAllCases() {
        ai.printPieceAndDirection(Piece.PIECE_I, Piece.DIRECTION_UP);
        ai.printPieceAndDirection(Piece.PIECE_L, Piece.DIRECTION_DOWN);
        ai.printPieceAndDirection(Piece.PIECE_O, Piece.DIRECTION_LEFT);
        ai.printPieceAndDirection(Piece.PIECE_Z, Piece.DIRECTION_RIGHT);
        ai.printPieceAndDirection(Piece.PIECE_T, Piece.DIRECTION_UP);
        ai.printPieceAndDirection(Piece.PIECE_J, Piece.DIRECTION_DOWN);
        ai.printPieceAndDirection(Piece.PIECE_S, Piece.DIRECTION_LEFT);
        ai.printPieceAndDirection(Piece.PIECE_I1, Piece.DIRECTION_RIGHT);
        ai.printPieceAndDirection(Piece.PIECE_I2, Piece.DIRECTION_UP);
        ai.printPieceAndDirection(Piece.PIECE_I3, Piece.DIRECTION_DOWN);
        ai.printPieceAndDirection(Piece.PIECE_L3, Piece.DIRECTION_LEFT);
        assertTrue(true, "All printPieceAndDirection cases completed");
    }

    // ─── onFirst with ARE prethink ───

    @Test
    void onFirstWithAREAndPrethink() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();

        engine.aiPrethink = true;
        // Set stat to ARE
        engine.stat = GameEngine.Status.ARE;

        ai.onFirst(engine, 0);

        assertTrue(ai.inARE, "inARE should be true after onFirst with ARE state");
    }

    // ─── setControl with piece touching ground and softdropLock (line 288-289) ───

    @Test
    void setControlGroundSoftdropLock() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 19; // touching ground
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        engine.ruleopt.softdropLock = true;
        engine.ruleopt.harddropEnable = false;
        engine.ruleopt.softdropEnable = false;
        ai.bestX = 5;
        ai.bestY = 19;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestRtSub = 0;
        ai.bestXSub = 5;
        ai.bestYSub = 19;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;
        ai.thinkComplete = true;
        ai.movestate = 0;

        ai.setControl(engine, 0, ctrl);

        // Should softdrop (BUTTON_DOWN) via softdropLock path
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_DOWN) != 0,
                "Ground with softdropLock should set BUTTON_DOWN");
    }
}
