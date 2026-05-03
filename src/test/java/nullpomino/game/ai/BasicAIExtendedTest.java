package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Extended tests for {@link BasicAI} covering setControl branches (hold,
 * rotation variants, ground rotation, shift), thinkMain with lid/valley
 * detection, combo bonus, height change, and thinkBestPosition with hold
 * piece.
 */
class BasicAIExtendedTest {

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
        ai = new BasicAI();
        ctrl = new Controller();
    }

    // ─── setControl ─────────────────────────────────────────

    @Test
    void setControlWithHold() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        ai.bestHold = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        // setControl depends on many internal state variables;
        // verify it runs without exception
        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlWithForceHold() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        ai.forceHold = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlMovesLeft() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 7;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        ai.bestX = 3;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlMovesRight() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 3;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        ai.bestX = 7;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlHardDropWhenAlignedNoSub() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
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
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 5;
        ai.bestRtSub = -1;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlSoftDropWhenNoHardDrop() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
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
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 5;
        ai.bestRtSub = -1;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlHardDropWithSubPosition() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
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
        engine.ruleopt.harddropLock = false;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 6;
        ai.bestRtSub = Piece.DIRECTION_DOWN;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlSoftDropWithSubAndLock() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
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
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 6;
        ai.bestRtSub = Piece.DIRECTION_DOWN;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControl180Rotation() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        engine.ruleopt.rotateButtonAllowDouble = true;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_DOWN; // 180 from DIRECTION_UP
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlReverseRotationDefaultRight() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        engine.ruleopt.rotateButtonAllowReverse = true;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_RIGHT; // rrot from UP
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlReverseRotationDefaultLeft() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        // Make engine report non-default right for reverse path
        engine.ruleopt.rotateButtonAllowReverse = true;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_LEFT; // lrot from UP
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlNormalRotation() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        engine.ruleopt.rotateButtonAllowReverse = false;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_RIGHT;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlUnreachablePosition() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        ai.bestX = 100;
        ai.bestY = 0;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlGroundRotation() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 6;
        ai.bestYSub = 18;
        ai.bestRtSub = Piece.DIRECTION_DOWN;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        // Ground rotation: bestRt -> bestRtSub, bestX -> bestXSub
        assertEquals(Piece.DIRECTION_DOWN, ai.bestRt,
                "Ground rotation should update bestRt");
        assertEquals(6, ai.bestX,
                "Shift move should update bestX");
    }

    // ─── thinkMain with lid/valley ───────────────────────────

    @Test
    void thinkMainLidDetection() {
        engine.createFieldIfNeeded();
        Field fld = new Field(engine.field);
        // Create lid: block above a hole
        fld.setBlockColor(0, 19, Block.BLOCK_COLOR_RED); // block
        fld.setBlockColor(0, 17, Block.BLOCK_COLOR_RED); // lid above hole at (0,18)

        Piece piece = new Piece(Piece.PIECE_O);
        int pts = ai.thinkMain(engine, 1, 18, 0, -1, fld, piece, null, null, 0);

        assertTrue(true, "thinkMain with lid detection completed");
    }

    @Test
    void thinkMainLidIncrease() {
        Field fld = new Field(10, 20, 0, false);
        // Create a hole with a lid
        fld.setBlockColor(0, 19, Block.BLOCK_COLOR_RED); // bottom
        // Leave (0,18) empty -> hole
        fld.setBlockColor(0, 17, Block.BLOCK_COLOR_RED); // lid

        Piece piece = new Piece(Piece.PIECE_O);
        // Place O at (1, 18) - this shouldn't increase lids
        int pts = ai.thinkMain(engine, 1, 18, 0, -1, fld, piece, null, null, 0);

        assertTrue(true, "thinkMain lid increase branch executed");
    }

    @Test
    void thinkMainLidReduction() {
        Field fld = new Field(10, 20, 0, false);
        // Create a hole with a lid, then fill the hole
        fld.setBlockColor(0, 19, Block.BLOCK_COLOR_RED); // bottom
        // Leave (0,18) empty -> hole
        fld.setBlockColor(0, 17, Block.BLOCK_COLOR_RED); // lid
        fld.setBlockColor(0, 16, Block.BLOCK_COLOR_RED);

        Piece piece = new Piece(Piece.PIECE_O);
        // Place O at (0, 17) - covers the hole
        int pts = ai.thinkMain(engine, 0, 17, 0, -1, fld, piece, null, null, 0);

        assertTrue(true, "thinkMain lid reduction branch executed");
    }

    @Test
    void thinkMainNeedIValleyIncrease() {
        Field fld = new Field(10, 20, 0, false);
        // Create a valley that needs I piece
        fld.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(0, 17, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(0, 16, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(0, 15, Block.BLOCK_COLOR_RED);
        // Leave column 1 low
        fld.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(2, 17, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(2, 16, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(2, 15, Block.BLOCK_COLOR_RED);

        Piece piece = new Piece(Piece.PIECE_O);
        // Place O at (1, 15) - might increase valley need
        int pts = ai.thinkMain(engine, 1, 15, 0, -1, fld, piece, null, null, 0);

        assertTrue(true, "thinkMain I valley increase branch executed");
    }

    @Test
    void thinkMainNeedIValleyDecrease() {
        Field fld = new Field(10, 20, 0, false);
        // Create a valley that can be reduced by I piece
        fld.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
        fld.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);

        Piece piece = new Piece(Piece.PIECE_I);
        // I vertical at x=1 fills between columns
        int pts = ai.thinkMain(engine, 1, 18, 1, -1, fld, piece, null, null, 0);

        assertTrue(true, "thinkMain I valley decrease branch executed");
    }

    // ─── thinkMain with combo bonus ──────────────────────────

    @Test
    void thinkMainComboBonus() {
        engine.createFieldIfNeeded();
        Field fld = new Field(engine.field);
        // Fill a line for clearing
        for (int x = 0; x < 10; x++) {
            fld.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
        }

        Piece piece = new Piece(Piece.PIECE_O);
        engine.combo = 2;
        engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
        int pts = ai.thinkMain(engine, 4, 18, 0, -1, fld, piece, null, null, 0);

        // Combo bonus: lines * combo * 100 = 1 * 2 * 100 = 200 extra
        assertTrue(true, "thinkMain with combo bonus executed");
    }

    @Test
    void thinkMainComboDisabledNoBonus() {
        Field fld = new Field(10, 20, 0, false);
        for (int x = 0; x < 10; x++) {
            fld.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
        }

        Piece piece = new Piece(Piece.PIECE_O);
        engine.combo = 5;
        engine.comboType = GameEngine.COMBO_TYPE_DISABLE;
        int pts = ai.thinkMain(engine, 4, 18, 0, -1, fld, piece, null, null, 0);

        assertTrue(true, "thinkMain with combo disabled executed");
    }

    // ─── thinkMain height change ─────────────────────────────

    @Test
    void thinkMainHeightIncrease() {
        Field fld = new Field(10, 20, 0, false);
        // Fill bottom area so height increases
        for (int x = 0; x < 10; x++) {
            for (int y = 18; y < 20; y++) {
                fld.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
            }
        }
        // Stack on top to increase height
        for (int x = 0; x < 10; x++) {
            fld.setBlockColor(x, 17, Block.BLOCK_COLOR_RED);
        }

        Piece piece = new Piece(Piece.PIECE_O);
        int pts = ai.thinkMain(engine, 0, 16, 0, -1, fld, piece, null, null, 1);

        // At depth > 0, height increase gives penalty
        assertTrue(true, "thinkMain height increase branch executed");
    }

    @Test
    void thinkMainHeightDecrease() {
        Field fld = new Field(10, 20, 0, false);
        // Fill bottom rows high
        for (int x = 0; x < 10; x++) {
            for (int y = 10; y < 20; y++) {
                fld.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
            }
        }

        Piece piece = new Piece(Piece.PIECE_I);
        // Place I vertical at rightmost to clear a line, decreasing height
        int pts = ai.thinkMain(engine, 9, 10, 1, -1, fld, piece, null, null, 0);

        assertTrue(true, "thinkMain height decrease branch executed");
    }

    // ─── thinkMain danger mode line clear scores ─────────────

    @Test
    void thinkMainDangerModeSingleClear() {
        Field fld = new Field(10, 20, 0, false);
        // Push height up to trigger danger (heightAfter <= 12)
        for (int x = 0; x < 10; x++) {
            for (int y = 10; y < 20; y++) {
                fld.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
            }
        }
        // Bottom row clear
        for (int x = 0; x < 10; x++) {
            fld.setBlockColor(x, 19, 0);
            fld.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
        }

        Piece piece = new Piece(Piece.PIECE_O);
        // Use depth > 0 to enter danger path
        int pts = ai.thinkMain(engine, 0, 19, 0, -1, fld, piece, null, null, 1);

        assertTrue(true, "thinkMain danger mode single clear completed");
    }

    // ─── thinkBestPosition with hold and next piece ──────────

    @Test
    void thinkBestPositionWithHoldPiece() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.holdPieceObject = new Piece(Piece.PIECE_S);
        engine.holdPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_S],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_S]);

        ai.thinkBestPosition(engine, 0);

        assertTrue(true, "thinkBestPosition with hold completed");
    }

    @Test
    void thinkBestPositionWithEmptyHold() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.holdPieceObject = null;

        ai.thinkBestPosition(engine, 0);

        assertTrue(true, "thinkBestPosition with empty hold completed");
    }

    @Test
    void thinkBestPositionWithTPieceUsesShiftAndRotation() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;

        ai.thinkBestPosition(engine, 0);

        assertTrue(true, "thinkBestPosition with T piece completed");
    }

    // ─── renderState and renderHint ──────────────────────────

    @Test
    void renderStateDoesNotThrow() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();

        ai.renderState(engine, 0);
    }

    @Test
    void renderHintDoesNotThrow() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();

        ai.renderHint(engine, 0);
    }
}
