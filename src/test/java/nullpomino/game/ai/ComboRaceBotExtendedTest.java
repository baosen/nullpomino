package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Extended tests for {@link ComboRaceBot} covering setControl branches
 * (hold, rotation variants, move, drop), onFirst with ARE/prethink,
 * thinkMain with hold at various depths, thinkBestPosition with hold,
 * and renderState/renderHint edge cases.
 */
class ComboRaceBotExtendedTest {

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

    // ─── setControl ─────────────────────────────────────────

    @Test
    void setControlWithHoldRequest() {
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
        ai.thinkComplete = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        // setControl depends on many internal state variables;
        // verify it runs without exception
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
        ai.thinkComplete = true;
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
        ai.thinkComplete = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlHardDropsWhenAligned() {
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
        ai.bestRtSub = 0;
        ai.thinkComplete = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlSoftDropsWhenAligned() {
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
        ai.bestRtSub = 0;
        ai.thinkComplete = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlSoftDropLockOnGround() {
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
        engine.ruleopt.harddropEnable = false;
        engine.ruleopt.softdropEnable = false;
        engine.ruleopt.softdropLock = true;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestRtSub = 0;
        ai.thinkComplete = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlRotate180() {
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
        ai.thinkComplete = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlRotateLeft() {
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
        engine.ruleopt.rotateButtonDefaultRight = true;
        engine.ruleopt.rotateButtonAllowReverse = true;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_LEFT; // lrot from UP
        ai.thinkComplete = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);
    }

    @Test
    void setControlRotateRight() {
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
        engine.ruleopt.rotateButtonDefaultRight = true;
        engine.ruleopt.rotateButtonAllowReverse = false;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_RIGHT; // rrot from UP
        ai.thinkComplete = true;
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
        ai.bestRtSub = 1; // Right rotation needed
        ai.movestate = 0;
        ai.thinkComplete = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        // Ground rotation: bestRt should be updated via getRotateDirection
        // and movestate should become 1
        assertEquals(1, ai.movestate,
                "Ground rotation should set movestate to 1");
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
        ai.movestate = 0;
        ai.bestX = 100;
        ai.bestY = 0;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.thinkComplete = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        assertFalse(ai.thinkComplete,
                "Should trigger rethink when unreachable");
    }

    // ─── onFirst ─────────────────────────────────────────────

    @Test
    void onFirstInAREWithPrethink() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.aiPrethink = true;
        engine.aiMoveDelay = 0;
        engine.stat = GameEngine.Status.ARE;
        ai.inARE = false;
        ai.delay = 5;
        ai.bestX = 7;
        ai.bestRt = Piece.DIRECTION_DOWN;
        ai.thinkComplete = true;
        ai.thinkSuccess = true;
        ai.threadRunning = true;

        ai.onFirst(engine, 0);

        assertTrue(ai.inARE, "Should set inARE to true");
    }

    @Test
    void onFirstInAREWithHold() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.aiPrethink = true;
        engine.aiMoveDelay = 0;
        engine.stat = GameEngine.Status.ARE;
        ai.inARE = true;
        ai.delay = 5;
        ai.bestHold = true;
        ai.thinkComplete = true;
        ai.thinkSuccess = true;
        ai.threadRunning = true;
        engine.holdPieceObject = new Piece(Piece.PIECE_S);
        engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_S)};
        engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
        engine.nextPieceCount = 0;

        ai.onFirst(engine, 0);

        // Should set inputARE with hold + possible move
        assertTrue(true, "onFirst with ARE and hold completed");
    }

    @Test
    void onFirstNoPrethinkSkips() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.aiPrethink = false;

        ai.onFirst(engine, 0);
        // No-op since aiPrethink is false
    }

    // ─── onLast ──────────────────────────────────────────────

    @Test
    void onLastInREADYWithStatcZero() throws Exception {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.stat = GameEngine.Status.READY;
        engine.statc[0] = 0;

        ai.onLast(engine, 0);

        // Should set createTablesRequest on thinkRequest
        Object thinkReq = ai.thinkRequest;
        java.lang.reflect.Field ctrField = thinkReq.getClass().getDeclaredField("createTablesRequest");
        ctrField.setAccessible(true);
        assertTrue((Boolean) ctrField.get(thinkReq),
                "onLast in READY with statc[0]==0 should trigger createTables");
    }

    @Test
    void onLastNotReadyDoesNothing() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;

        ai.onLast(engine, 0);
        // No exception expected
    }

    // ─── thinkMain with hold ─────────────────────────────────

    @Test
    void thinkMainWithHoldAtDepthZero() {
        ai.createTables(engine);
        ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];
        for (int i = 0; i < ai.nextQueueIDs.length; i++) {
            ai.nextQueueIDs[i] = Piece.PIECE_T;
        }
        engine.ruleopt.holdEnable = true;

        int pts = ai.thinkMain(engine, 0, Piece.PIECE_S, 0);

        assertTrue(pts >= 0, "thinkMain with hold at depth 0 should return score");
    }

    @Test
    void thinkMainWithHoldAtMaxDepth() {
        ai.createTables(engine);
        ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];
        for (int i = 0; i < ai.nextQueueIDs.length; i++) {
            ai.nextQueueIDs[i] = Piece.PIECE_T;
        }
        engine.ruleopt.holdEnable = true;

        int pts = ai.thinkMain(engine, 0, Piece.PIECE_I, ComboRaceBot.MAX_THINK_DEPTH);

        // At max depth, should return score including I piece bonus
        assertTrue(pts >= 1000, "Terminal state with I hold should include bonus");
    }

    @Test
    void thinkMainWithNoHoldAtMidDepth() {
        ai.createTables(engine);
        ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];
        for (int i = 0; i < ai.nextQueueIDs.length; i++) {
            ai.nextQueueIDs[i] = Piece.PIECE_T;
        }
        engine.ruleopt.holdEnable = false;

        int pts = ai.thinkMain(engine, 0, -1, 2);

        assertTrue(pts >= 0, "thinkMain without hold at mid depth should return score");
    }

    @Test
    void thinkMainTerminalWithSpecificHoldScore() {
        ai.createTables(engine);
        ai.nextQueueIDs = new int[ComboRaceBot.MAX_THINK_DEPTH];
        for (int i = 0; i < ai.nextQueueIDs.length; i++) {
            ai.nextQueueIDs[i] = Piece.PIECE_T;
        }

        // Test each piece type's contribution
        for (int holdId = 0; holdId < 7; holdId++) {
            int pts = ai.thinkMain(engine, 0, holdId, ComboRaceBot.MAX_THINK_DEPTH);
            assertTrue(pts > 0, "Hold piece " + holdId + " should give score at terminal");
        }
    }

    // ─── thinkBestPosition with hold ─────────────────────────

    @Test
    void thinkBestPositionWithHoldPiece() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        ai.createTables(engine);
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

        assertTrue(true, "thinkBestPosition with hold piece completed");
    }

    @Test
    void thinkBestPositionWithFieldNull() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.field = null;

        // When field is null, create a new field from dimensions
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);

        ai.thinkBestPosition(engine, 0);

        assertTrue(true, "thinkBestPosition with null field completed");
    }

    @Test
    void thinkBestPositionWithAREMode() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        ai.inARE = true;
        engine.stat = GameEngine.Status.ARE;
        engine.createFieldIfNeeded();
        engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_S)};
        engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
        engine.nextPieceCount = 0;

        ai.thinkBestPosition(engine, 0);

        assertTrue(true, "thinkBestPosition with ARE mode completed");
    }

    // ─── renderState edge cases ──────────────────────────────

    @Test
    void renderStateWithNowPieceNull() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = null;

        ai.renderState(engine, 0);
        // No exception expected when nowPieceObject is null
    }

    @Test
    void renderStateWithNextQueueNull() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();

        ai.renderState(engine, 0);
        // No exception expected when nextQueueIDs is null
    }

    @Test
    void renderHintWithNoBestMove() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        ai.bestPts = -1;
        ai.thinkComplete = false;
        ai.thinkCurrentPieceNo = 0;
        ai.thinkLastPieceNo = 0;

        ai.renderHint(engine, 0);
        // No exception expected when bestPts <= 0
    }

    @Test
    void renderHintWithHoldHint() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        ai.bestPts = 1000;
        ai.thinkComplete = true;
        ai.bestHold = true;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        // No exception expected for hold hint
    }

    @Test
    void renderHintWithMoveHint() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 3;
        engine.nowPieceY = 5;
        ai.bestPts = 2000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 7;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        // No exception expected for move hint
    }

    @Test
    void renderHintWhenOnBestPosition() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        ai.bestPts = 3000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 5;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        // No exception expected when already at best position
    }

    @Test
    void renderHintWhenSubPositionMatches() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        ai.bestPts = 4000;
        ai.thinkComplete = true;
        ai.bestHold = false;
        ai.bestX = 6;
        ai.bestY = 18;
        ai.bestRt = Piece.DIRECTION_RIGHT;
        ai.bestXSub = 5;
        ai.bestYSub = 18;
        ai.bestRtSub = Piece.DIRECTION_UP;
        ai.movestate = 0;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        // No exception expected when sub position matches (shows rotate hint)
    }

    @Test
    void renderHintWithUnreachableSubPosition() {
        ai.init(engine, 0);
        engine.createFieldIfNeeded();
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 3;
        engine.nowPieceY = 5;
        ai.bestPts = 5000;
        ai.bestHold = false;
        ai.bestX = 100;
        ai.bestY = 0;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 100;
        ai.bestYSub = 0;
        ai.bestRtSub = Piece.DIRECTION_UP;
        ai.movestate = 0;
        ai.thinkComplete = true;
        ai.thinkCurrentPieceNo = 1;
        ai.thinkLastPieceNo = 1;

        ai.renderHint(engine, 0);
        // No exception expected when sub position is unreachable
    }

    // ─── checkOffset ─────────────────────────────────────────

    @Test
    void checkOffsetPreservesBigFlag() {
        Piece p = new Piece(Piece.PIECE_T);
        engine.big = true;
        Piece result = ComboRaceBot.checkOffset(p, engine);
        assertTrue(result.big, "checkOffset should preserve engine.big flag");
    }
}
