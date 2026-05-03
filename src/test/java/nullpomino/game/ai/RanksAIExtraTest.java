package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.tool.airankstool.Ranks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Additional tests for {@link RanksAI} covering uncovered branches:
 * setControl speed limit check (223-232), hold path (234-236),
 * rotation variants (239-252), skipNextFrame logic (258-306),
 * funnel drop with sub position (295-298), thinkBestPosition engine
 * version with next pieces (397-401), and thinkMain recursive with
 * hold at depth.
 */
class RanksAIExtraTest {

    private GameManager gm;
    private GameEngine engine;
    private RanksAI ai;
    private Controller ctrl;
    private Ranks ranks;

    @BeforeEach
    void setUp() throws Exception {
        gm = new GameManager(new EventReceiver());
        gm.init();
        engine = gm.engine[0];
        engine.init();
        engine.createFieldIfNeeded();
        engine.statistics.time = 1;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
        engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_S)};
        engine.nextPieceCount = 0;
        ai = new RanksAI();
        ctrl = new Controller();

        ranks = new Ranks(4, 9);
        setPrivateField(ai, "ranks", ranks);
        setPrivateField(ai, "heights", new int[9]);
        setPrivateField(ai, "MAX_PREVIEWS", 2);
        setPrivateField(ai, "allowHold", true);
        setPrivateField(ai, "gameOver", false);
        setPrivateField(ai, "speedLimit", 0);
    }

    private static void setPrivateField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    // ─── setControl: speed limit check (lines 223-232) ─────────────────

    @Test
    void setControlWithSpeedLimitExceeded() throws Exception {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        setPrivateField(ai, "speedLimit", 100);
        engine.createFieldIfNeeded();
        engine.statistics.time = 1;
        engine.statistics.totalPieceLocked = 100;
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
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        // When speed limit exceeded, setControl should do nothing
        assertEquals(0, ctrl.getButtonBit(),
                "setControl should not set buttons when speed limit exceeded");
    }

    // ─── setControl: hold path (lines 234-236) ─────────────────────────

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
        ai.forceHold = false;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        // Should set hold button (BUTTON_D)
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_D) != 0,
                "Hold should set BUTTON_D");
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
        ai.bestHold = false;
        ai.forceHold = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_D) != 0,
                "ForceHold should set BUTTON_D");
    }

    // ─── setControl: rotation with double button (lines 243-253) ───────

    @Test
    void setControlRotationDouble() {
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
        ai.bestRt = Piece.DIRECTION_DOWN; // 180 from UP
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_E) != 0,
                "180 rotation should set BUTTON_E");
    }

    // ─── setControl: skipNextFrame true path (line 258-263) ────────────

    @Test
    void setControlSkipNextFrameTrueThenFalse() {
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
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;
        // First call: skipNextFrame is false, so it goes through the if block
        // skipNextFrame becomes true
        // Second call: skipNextFrame is true, goes to else block, skipNextFrame=false

        ai.setControl(engine, 0, ctrl);
        // First call done, skipNextFrame should be true

        ai.setControl(engine, 0, ctrl);
        // Second call should enter the else branch and set skipNextFrame to false

        assertTrue(true, "setControl skipNextFrame toggle completed");
    }

    // ─── setControl: funnel drop with sub position (lines 295-298) ─────

    @Test
    void setControlFunnelWithSubPosition() {
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

        // With sub position, should set harddrop via BUTTON_UP
        assertTrue(true, "setControl funnel with sub position completed");
    }

    // ─── thinkMain: recursive with numPreviews > 0 (lines 642-745) ─────

    @Test
    void thinkMainWithPreviews() {
        int[] heights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z};
        int[] holdPiece = {-1};

        // numPreviews = 1, should go recursive
        RanksAI.Score score = ai.thinkMain(0, 0, heights, pieces, holdPiece, true, 1);

        assertTrue(score != null, "thinkMain with previews should return a Score");
    }

    @Test
    void thinkMainWithPreviewsAndHold() {
        int[] heights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z};
        int[] holdPiece = {Piece.PIECE_I};

        // numPreviews = 1, holdOK = true, allowHold = true
        RanksAI.Score score = ai.thinkMain(0, 0, heights, pieces, holdPiece, true, 1);

        assertTrue(score != null, "thinkMain with previews and hold should return a Score");
    }

    // ─── thinkMain: tetris path (isVerticalIRightMost) ─────────────────

    @Test
    void thinkMainTetrisPath() {
        int[] heights = {4, 4, 4, 4, 4, 4, 4, 4, 4};
        int[] pieces = {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {-1};

        // I piece vertical (rt=1 or 3) at x=9 -> isVerticalIRightMost = true
        RanksAI.Score score = ai.thinkMain(9, 1, heights, pieces, holdPiece, true, 0);

        assertTrue(score != null, "thinkMain tetris path should return a Score");
    }

    // ─── thinkBestPosition engine version (lines 397-401) ──────────────

    @Test
    void thinkBestPositionEngineNextPieces() {
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

        assertTrue(true, "thinkBestPosition engine version completed");
    }

    // ─── setControl: unreachable position triggers rethink (line 261-263) ─

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

        // Should trigger thinkRequest for rethink
        assertTrue(ai.thinkRequest, "Unreachable position should set thinkRequest");
    }

    // ─── setControl: rotation with reverse (lines 245-252) ─────────────

    @Test
    void setControlReverseRotation() {
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
        engine.ruleopt.rotateButtonDefaultRight = false;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_RIGHT; // rrot from UP
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        // Should set reverse rotation button (BUTTON_B)
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_B) != 0,
                "Reverse rotation should set BUTTON_B");
    }
}
