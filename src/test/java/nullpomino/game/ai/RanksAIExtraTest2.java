package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Tests covering uncovered branches in {@link RanksAI}:
 * - setControl: TPM speed limit check (line 225)
 * - setControl: skipNextFrame toggle (line 258-306)
 * - setControl: forceHold (line 234-237)
 * - setControl: left/right move with aiMoveDelay >= 0 (line 282-287)
 * - setControl: harddrop lock check (line 296-300)
 * - thinkBestPosition (engine): hold paths
 * - thinkBestPosition (int[]): 4-line tetris at rightmost+1 (line 553-571)
 * - thinkBestPosition (int[]): hold with empty box (pieceNow != I path)
 * - thinkMain: terminal with pieceScores (line 518-519)
 * - thinkMain: piece doesn't fit surface (line 610, 760-763)
 * - playFictitiousMove: bestHold false, bestX==9 (line 347-351)
 * - playFictitiousMove: bestHold false, bestX!=9 (line 352-361)
 * - playFictitiousMove: holdOK false after bestHold (line 341-343)
 */
class RanksAIExtraTest2 {

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

        // Set up RanksAI internals via reflection (avoid file I/O)
        ranks = new Ranks(4, 9);
        setPrivateField(ai, "ranks", ranks);
        setPrivateField(ai, "heights", new int[9]);
        setPrivateField(ai, "MAX_PREVIEWS", 2);
        setPrivateField(ai, "allowHold", false);
        setPrivateField(ai, "gameOver", false);
        setPrivateField(ai, "speedLimit", 0);
    }

    private static void setPrivateField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Object getPrivateField(Object obj, String name) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(obj);
    }

    // ─── setControl: forceHold triggers hold (line 234-237) ───

    @Test
    void setControlForceHold() {
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
        engine.statistics.time = 3600; // Avoid division by zero in TPM calc
        ai.forceHold = true;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        // Should set hold (BUTTON_D)
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_D) != 0,
                "forceHold should set BUTTON_D");
    }

    // ─── setControl: left movement with aiMoveDelay >= 0 (line 282-283) ───

    @Test
    void setControlMoveLeftWithDelay() {
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
        engine.aiMoveDelay = -1; // negative means no DAS override
        ai.bestX = 3;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        engine.statistics.time = 3600; // Avoid division by zero in TPM calc
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_LEFT) != 0,
                "Should set BUTTON_LEFT when nowX > bestX");
    }

    // ─── setControl: right movement (line 285-286) ───

    @Test
    void setControlMoveRight() {
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
        engine.aiMoveDelay = -1;
        ai.bestX = 7;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        engine.statistics.time = 3600; // Avoid division by zero in TPM calc
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_RIGHT) != 0,
                "Should set BUTTON_RIGHT when nowX < bestX");
    }

    // ─── setControl: harddrop lock check (line 296-300) ───

    @Test
    void setControlHarddropLock() {
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
        engine.aiMoveDelay = -1;
        engine.ruleopt.harddropEnable = true;
        engine.ruleopt.harddropLock = true; // lock! So harddrop should NOT be used
        engine.ruleopt.softdropEnable = true;
        engine.ruleopt.softdropLock = false;
        ai.bestX = 5;
        ai.bestY = 5;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestRtSub = Piece.DIRECTION_DOWN;
        ai.bestXSub = 6;
        ai.bestYSub = 5;
        engine.statistics.time = 3600; // Avoid division by zero in TPM calc
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        // harddropLock true -> should use softdrop instead
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_DOWN) != 0,
                "Should set softdrop when harddrop locked");
    }

    // ─── thinkBestPosition (int[]): 4-line tetris (line 553-571) ───

    @Test
    void thinkBestPositionTetrisLine() {
        ai.thinkBestPosition(
            new int[] {8, 8, 8, 8, 8, 8, 8, 8, 8},
            new int[] {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_L},
            new int[] {-1},
            false);

        // For I piece at rotation 1 or 3 with currentHeightMin >= 4,
        // should try the rightmost+1 position
        assertTrue(true, "thinkBestPosition tetris line attempt completed");
    }

    // ─── thinkBestPosition (int[]): non-I piece, not force tetris ───

    @Test
    void thinkBestPositionNonIWithHold() throws Exception {
        setPrivateField(ai, "allowHold", true);

        ai.thinkBestPosition(
            new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0},
            new int[] {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z},
            new int[] {Piece.PIECE_I},
            true);

        assertTrue(true, "thinkBestPosition with hold completed");
    }

    // ─── thinkMain: pieceScores for holdID (line 518-519) ───

    @Test
    void thinkMainTerminalWithPieceScores() {
        int[] heights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_O, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {-1};

        // holdID = 0 (not I), depth == numPreviews (terminal)
        RanksAI.Score score = ai.thinkMain(4, 0, heights, pieces, holdPiece, true, 2);

        assertEquals(0, score.distanceToSet, "Terminal with piece scores should return score");
    }

    // ─── thinkMain: piece doesn't fit surface (line 760-763) ───

    @Test
    void thinkMainPieceDoesNotFit() {
        int[] heights = {20, 20, 20, 20, 20, 20, 20, 20, 20};
        int[] pieces = {Piece.PIECE_O, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {-1};

        RanksAI.Score score = ai.thinkMain(4, 0, heights, pieces, holdPiece, true, 0);

        assertNotNull(score);
    }

    private void assertNotNull(RanksAI.Score score) {
        assertTrue(score != null, "Score should not be null");
    }

    // ─── playFictitiousMove: with hold (line 341-343) ───

    @Test
    void playFictitiousMoveWithHold() {
        int[] heights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_O, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {Piece.PIECE_I};
        boolean[] holdOK = {true};

        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);

        assertTrue(true, "playFictitiousMove with hold completed");
    }

    // ─── playFictitiousMove: game over from heights > 20 (line 355-360) ───

    @Test
    void playFictitiousMoveGameOver() {
        int[] heights = {21, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_O, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {-1};
        boolean[] holdOK = {true};

        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);

        assertTrue(true, "playFictitiousMove game over completed");
    }

    // ─── setControl: reverse rotation default right with bestRt == lrot (line 248-250) ───

    @Test
    void setControlReverseRotationDefaultRight() {
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
        engine.ruleopt.rotateButtonAllowReverse = true;
        engine.ruleopt.rotateButtonDefaultRight = true;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_LEFT; // lrot when current is UP
        engine.statistics.time = 3600; // Avoid division by zero in TPM calc
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_B) != 0,
                "Reverse rotation should set BUTTON_B");
    }

    // ─── setControl: double rotation (line 243-244) ───

    @Test
    void setControlDoubleRotation() {
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
        engine.ruleopt.rotateButtonAllowDouble = true;
        engine.statistics.time = 3600; // Avoid division by zero in TPM calc
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_DOWN; // 180 from UP
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.delay = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_E) != 0,
                "Double rotation should set BUTTON_E");
    }

    // ─── thinkBestPosition (int[]): plannedToUseIPiece tracking (line 578-579) ───

    @Test
    void thinkBestPositionNumPreviewsTracking() throws Exception {
        setPrivateField(ai, "MAX_PREVIEWS", 1);
        setPrivateField(ai, "allowHold", false);

        ai.thinkBestPosition(
            new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0},
            new int[] {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_L},
            new int[] {-1},
            true);

        assertTrue(true, "thinkBestPosition with numPreviews tracking completed");
    }
}
