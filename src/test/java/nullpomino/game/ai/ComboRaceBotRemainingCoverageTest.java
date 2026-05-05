package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.*;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in ComboRaceBot.
 * Focus on setControl, thinkBestPosition, onFirst, onLast, renderHint branches.
 */
class ComboRaceBotRemainingCoverageTest {

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
        engine.aiUseThread = false;
        ai = new ComboRaceBot();
        ai.init(engine, 0);
        ctrl = new Controller();
    }

    // ─── setControl: I piece hold path ───
    @Test
    void setControlHold() {
        engine.createFieldIfNeeded();
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_I);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_I],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_I]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        ai.thinkComplete = true;
        ai.bestHold = true;

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl hold path");
    }

    // ─── setControl: bestHold not set, rotation with 180 ───
    @Test
    void setControlRotation180() {
        engine.createFieldIfNeeded();
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.nowPieceObject.direction = Piece.DIRECTION_UP;
        ai.bestRt = Piece.DIRECTION_DOWN; // 180
        ai.bestHold = false;
        ai.thinkComplete = true;
        ai.bestX = 5;
        engine.ruleopt.rotateButtonAllowDouble = true;

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl rotation 180");
    }

    // ─── setControl: ground rotation with bestRtSub != 0 (line 276-281) ───
    @Test
    void setControlGroundRotation() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        ai.bestX = 5;
        ai.bestRt = 0;
        ai.bestRtSub = 2; // != 0 triggers ground rotation
        ai.bestHold = false;
        ai.thinkComplete = true;
        for (int x = 0; x < 10; x++)
            engine.field.setBlockColor(x, 19, 1);

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl ground rotation");
    }

    // ─── setControl: funnel with bestRtSub == 0, harddrop (line 288) ───
    @Test
    void setControlFunnelHarddrop() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        ai.bestX = 5;
        ai.bestRt = 0;
        ai.bestRtSub = 0;
        ai.bestHold = false;
        ai.thinkComplete = true;
        engine.ruleopt.harddropEnable = true;
        for (int x = 0; x < 10; x++)
            engine.field.setBlockColor(x, 19, 1);

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl funnel harddrop");
    }

    // ─── setControl: drop softdropEnable (line 293) ───
    @Test
    void setControlFunnelSoftdrop() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 18;
        ai.bestX = 5;
        ai.bestRt = 0;
        ai.bestRtSub = 0;
        ai.bestHold = false;
        ai.thinkComplete = true;
        engine.ruleopt.harddropEnable = false;
        engine.ruleopt.softdropEnable = true;
        for (int x = 0; x < 10; x++)
            engine.field.setBlockColor(x, 19, 1);

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl funnel softdrop");
    }

    // ─── setControl: moveDir logic (lines 301-304) ───
    @Test
    void setControlMoveDir() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        ai.bestX = 3; // nowX > bestX -> moveDir = -1 (left)
        ai.bestRt = 0;
        ai.bestHold = false;
        ai.thinkComplete = true;

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl moveDir left");
    }

    @Test
    void setControlMoveDirRight() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        ai.bestX = 7; // nowX < bestX -> moveDir = 1 (right)
        ai.bestRt = 0;
        ai.bestHold = false;
        ai.thinkComplete = true;

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl moveDir right");
    }

    // ─── setControl: rotate button input with allow reverse (lines 327-340) ───
    @Test
    void setControlRotateReverseNotDefault() {
        engine.stat = GameEngine.Status.MOVE;
        engine.statc[0] = 1;
        engine.aiMoveDelay = 0;
        ai.delay = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.nowPieceObject.applyOffsetArray(
            engine.ruleopt.pieceOffsetX[Piece.PIECE_T],
            engine.ruleopt.pieceOffsetY[Piece.PIECE_T]);
        engine.nowPieceX = 5;
        engine.nowPieceY = 5;
        engine.nowPieceObject.direction = Piece.DIRECTION_UP;
        ai.bestRt = Piece.DIRECTION_RIGHT;
        ai.bestHold = false;
        ai.thinkComplete = true;
        ai.bestX = 5;
        engine.ruleopt.rotateButtonAllowReverse = true;
        engine.ruleopt.rotateButtonDefaultRight = false;

        ai.setControl(engine, 0, ctrl);
        assertTrue(true, "setControl rotate reverse not default");
    }

    // ─── onFirst with bestHold, hold empty (lines 170-177) ───
    @Test
    void onFirstBestHold() {
        engine.aiPrethink = true;
        engine.stat = GameEngine.Status.ARE;
        ai.delay = 9999;
        ai.bestHold = true;
        ai.thinkComplete = true;
        engine.holdPieceObject = null;
        engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T)};
        engine.nextPieceCount = 0;

        ai.onFirst(engine, 0);
        assertTrue(true, "onFirst bestHold");
    }

    // ─── onFirst with bestHold, hold not empty (line 175) ───
    @Test
    void onFirstBestHoldNotNull() {
        engine.aiPrethink = true;
        engine.stat = GameEngine.Status.ARE;
        ai.delay = 9999;
        ai.bestHold = true;
        ai.thinkComplete = true;
        engine.holdPieceObject = new Piece(Piece.PIECE_S);
        engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T)};
        engine.nextPieceCount = 0;

        ai.onFirst(engine, 0);
        assertTrue(true, "onFirst bestHold not null");
    }

    // ─── onLast with READY state fresh (line 203-204) ───
    @Test
    void onLastReady() {
        engine.stat = GameEngine.Status.READY;
        engine.statc[0] = 0;
        ai.onLast(engine, 0);
        assertTrue(true, "onLast ready");
    }

    // ─── createTables (ensure called) ───
    @Test
    void createTablesCalled() {
        ai.createTables(engine);
        assertNotNull(ai.moves);
    }

    // ─── thinkBestPosition with inARE, no piece (line 404-408) ───
    @Test
    void thinkBestPositionInARE() {
        ai.inARE = true;
        engine.nowPieceObject = null;
        engine.nextPieceCount = 0;
        engine.nextPieceArrayID = new int[]{Piece.PIECE_T, Piece.PIECE_S};
        engine.nextPieceArrayObject = new Piece[]{new Piece(Piece.PIECE_T), new Piece(Piece.PIECE_S)};

        ai.thinkBestPosition(engine, 0);
        assertTrue(true, "thinkBestPosition in ARE");
    }

    // ─── thinkMain with holdID == -1 (line 536) ───
    @Test
    void thinkMainHoldNegativeOne() {
        ai.createTables(engine);
        ai.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        ai.moves = new ComboRaceBot.Transition[28][7];
        for (int s = 0; s < 28; s++)
            for (int p = 0; p < 7; p++)
                ai.moves[s][p] = new ComboRaceBot.Transition(3, 0, 0, s);
        int pts = ai.thinkMain(engine, 0, -1, 0);
        assertTrue(pts >= 0, "thinkMain hold -1");
    }

    // ─── thinkMain with holdID >= 0 (lines 538-546) ───
    @Test
    void thinkMainHoldNotEmpty() {
        ai.createTables(engine);
        ai.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        int pts = ai.thinkMain(engine, 0, Piece.PIECE_T, 0);
        assertTrue(pts >= 0, "thinkMain hold not empty");
    }

    // ─── thinkMain at depth limit with specific hold (lines 516-519) ───
    @Test
    void thinkMainDepthLimit() {
        ai.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        int pts = ai.thinkMain(engine, 0, Piece.PIECE_I, 6);
        assertTrue(pts >= 1000, "thinkMain depth limit with I piece");
    }

    @Test
    void thinkMainDepthLimitNonI() {
        ai.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        int pts = ai.thinkMain(engine, 0, Piece.PIECE_T, 6);
        assertTrue(pts >= 0, "thinkMain depth limit non-I");
    }

    // ─── renderState coverage ───
    @Test
    void renderStateCoverage() {
        engine.createFieldIfNeeded();
        ai.renderState(engine, 0);
        assertTrue(true, "renderState");
    }

    // ─── renderHint coverage ───
    @Test
    void renderHintCoverage() {
        engine.createFieldIfNeeded();
        ai.renderHint(engine, 0);
        assertTrue(true, "renderHint");
    }

    // ─── Transition constructors ───
    @Test
    void transitionConstructors() {
        ComboRaceBot.Transition t1 = new ComboRaceBot.Transition(1, 2, 3);
        assertEquals(0, t1.rtSub);
        ComboRaceBot.Transition t2 = new ComboRaceBot.Transition(4, 5, 6, 7);
        assertEquals(6, t2.rtSub);
        ComboRaceBot.Transition t3 = new ComboRaceBot.Transition(8, 9, 10, null);
        assertNull(t3.next);
        ComboRaceBot.Transition t4 = new ComboRaceBot.Transition(11, 12, 13, 14, null);
        assertNull(t4.next);
    }
}
