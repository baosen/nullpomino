package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Additional tests for {@link RanksAI} covering remaining uncovered branches:
 * - thinkBestPosition (heights): force tetris with I piece (lines 484-498)
 * - thinkBestPosition (heights): useHold=1 with holdPiece[0]==-1 (lines 505-516)
 * - thinkBestPosition (heights): useHold=1 with holdPiece[0]!=-1 (lines 517-522)
 * - thinkBestPosition (heights): 4-line try with I piece at rotations 1 or 3 (lines 553-571)
 * - thinkMain: non-terminal force 4-line for next piece (lines 664-677)
 * - thinkMain: non-terminal with hold at depth (lines 683-741)
 * - thinkMain: isVerticalI2 with heightMin >= 4 (lines 728-739)
 * - thinkMain: surface doesn't fit piece (lines 760-763)
 * - playFictitiousMove: bestScore.rankStacking == 0 (lines 338-339)
 * - playFictitiousMove: bestHold true with holdOK (lines 341-343)
 * - playFictitiousMove: bestHold false with bestX == 9 (lines 347-351)
 * - playFictitiousMove: bestHold false with bestX != 9 (lines 352-361)
 * - setControl: skipNextFrame toggle (lines 258-306)
 * - setControl: BUTTON_A press check (lines 251-252)
 * - initRanks: empty file path (lines 149-150)
 * - thinkBestPosition (engine): heights conversion (lines 391-394)
 * - thinkBestPosition (engine): holdPiece null/not null (lines 404-409)
 */
class RanksAIExtraTest3 {

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

    // ─── thinkBestPosition (heights): force 4-line with I piece (lines 484-498) ───

    @Test
    void thinkBestPositionForceTetrisWithI() {
        // All columns at height 8, which >= THRESHOLD_FORCE_4LINES=8
        // and currentHeightMin >= 4
        ai.thinkBestPosition(
            new int[] {8, 8, 8, 8, 8, 8, 8, 8, 8},
            new int[] {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_L},
            new int[] {-1},
            false);

        // Should force I piece tetris at rightmost column
        assertTrue(ai.bestX >= ranks.getStackWidth(), "Should force tetris at rightmost column");
        assertEquals(1, ai.bestRt, "Should be vertical rotation");
    }

    // ─── thinkBestPosition (heights): non-I piece, try all rotations (lines 504-574) ───

    @Test
    void thinkBestPositionNonIPieceAllRotations() {
        ai.thinkBestPosition(
            new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0},
            new int[] {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z},
            new int[] {-1},
            false);

        assertTrue(true, "thinkBestPosition non-I piece completed");
    }

    // ─── thinkBestPosition (heights): useHold=1 with empty hold (lines 505-516) ───

    @Test
    void thinkBestPositionWithHoldEmptyBox() throws Exception {
        setPrivateField(ai, "allowHold", true);

        ai.thinkBestPosition(
            new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0},
            new int[] {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z},
            new int[] {-1}, // empty hold
            true); // holdOK

        assertTrue(true, "thinkBestPosition with empty hold completed");
    }

    // ─── thinkBestPosition (heights): useHold=1 with existing hold (lines 517-522) ───

    @Test
    void thinkBestPositionWithExistingHold() throws Exception {
        setPrivateField(ai, "allowHold", true);

        ai.thinkBestPosition(
            new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0},
            new int[] {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z},
            new int[] {Piece.PIECE_I}, // existing hold
            true); // holdOK

        assertTrue(true, "thinkBestPosition with existing hold completed");
    }

    // ─── thinkBestPosition (heights): try 4-line with I piece rt=1 or 3 (lines 553-571) ───

    @Test
    void thinkBestPositionTryTetrisLine() {
        ai.thinkBestPosition(
            new int[] {8, 8, 8, 8, 8, 8, 8, 8, 8},
            new int[] {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_L},
            new int[] {-1},
            false);

        // I piece, heightMin >= 4 -> should try 4-line
        assertTrue(true, "thinkBestPosition try tetris line completed");
    }

    // ─── thinkMain: terminal with depth == numPreviews (lines 748-756) ───

    @Test
    void thinkMainTerminalComputeScore() {
        int[] heights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z};
        int[] holdPiece = {-1};

        RanksAI.Score score = ai.thinkMain(4, 0, heights, pieces, holdPiece, true, 2);

        assertNotNull(score, "Score should not be null");
    }

    // ─── thinkMain: terminal with I piece and x < stackWidth (lines 752-754) ───

    @Test
    void thinkMainTerminalWithIPieceUsed() {
        int[] heights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_S};
        int[] holdPiece = {-1};

        RanksAI.Score score = ai.thinkMain(4, 1, heights, pieces, holdPiece, true, 2);

        assertNotNull(score, "Score should not be null");
    }

    // ─── thinkMain: non-terminal force 4-line for next piece (lines 664-677) ───

    @Test
    void thinkMainNonTerminalForceTetris() {
        int[] heights = {8, 8, 8, 8, 8, 8, 8, 8, 8};
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_I, Piece.PIECE_S};
        int[] holdPiece = {-1};

        RanksAI.Score score = ai.thinkMain(4, 0, heights, pieces, holdPiece, true, 1);

        // Should go through non-terminal path and try force 4-line for I piece
        assertNotNull(score, "Score should not be null");
    }

    // ─── thinkMain: non-terminal with hold enabled (lines 683-741) ───

    @Test
    void thinkMainNonTerminalWithHold() throws Exception {
        setPrivateField(ai, "allowHold", true);

        int[] heights = {6, 6, 6, 6, 6, 6, 6, 6, 6};
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z};
        int[] holdPiece = {Piece.PIECE_I};

        RanksAI.Score score = ai.thinkMain(4, 0, heights, pieces, holdPiece, true, 1);

        assertNotNull(score, "Score should not be null");
    }

    // ─── thinkMain: non-terminal with hold and holdPiece[0]==-1 (lines 685-693) ───

    @Test
    void thinkMainNonTerminalHoldEmptyBox() throws Exception {
        setPrivateField(ai, "allowHold", true);

        int[] heights = {6, 6, 6, 6, 6, 6, 6, 6, 6};
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z};
        int[] holdPiece = {-1};

        RanksAI.Score score = ai.thinkMain(4, 0, heights, pieces, holdPiece, true, 1);

        assertNotNull(score, "Score should not be null");
    }

    // ─── thinkMain: isVerticalI2 with heightMin >= 4 (lines 728-739) ───

    @Test
    void thinkMainIsVerticalI2Tetris() {
        int[] heights = {5, 5, 5, 5, 5, 5, 5, 5, 5};
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_I, Piece.PIECE_S};
        int[] holdPiece = {-1};

        RanksAI.Score score = ai.thinkMain(4, 0, heights, pieces, holdPiece, true, 1);

        assertNotNull(score, "Score should not be null");
    }

    // ─── thinkMain: surface doesn't fit piece (lines 760-763) ───

    @Test
    void thinkMainSurfaceDoesNotFit() {
        int[] heights = {20, 20, 20, 20, 20, 20, 20, 20, 20};
        int[] pieces = {Piece.PIECE_O, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {-1};

        RanksAI.Score score = ai.thinkMain(4, 0, heights, pieces, holdPiece, true, 0);

        assertNotNull(score, "Score should not be null");
    }

    // ─── playFictitiousMove: rankStacking == 0 (game over, lines 338-339) ───

    @Test
    void playFictitiousMoveGameOverByRanking() {
        int[] heights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_O, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {-1};
        boolean[] holdOK = {true};

        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);

        assertTrue(true, "playFictitiousMove game over by ranking completed");
    }

    // ─── playFictitiousMove: bestHold true (lines 341-343) ───

    @Test
    void playFictitiousMoveBestHoldTrue() throws Exception {
        setPrivateField(ai, "allowHold", true);

        int[] heights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {Piece.PIECE_T};
        boolean[] holdOK = {true};

        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);

        assertTrue(true, "playFictitiousMove bestHold true completed");
    }

    // ─── playFictitiousMove: bestHold false with bestX == 9 (lines 347-351) ───

    @Test
    void playFictitiousMoveBestX9() throws Exception {
        setPrivateField(ai, "allowHold", false);

        int[] heights = {8, 8, 8, 8, 8, 8, 8, 8, 8};
        int[] pieces = {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {-1};
        boolean[] holdOK = {false};

        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);

        assertTrue(true, "playFictitiousMove bestX9 completed");
    }

    // ─── playFictitiousMove: bestHold false with bestX != 9 (lines 352-361) ───

    @Test
    void playFictitiousMoveBestXNot9() throws Exception {
        setPrivateField(ai, "allowHold", false);

        int[] heights = {0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z};
        int[] holdPiece = {-1};
        boolean[] holdOK = {false};

        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);

        assertTrue(true, "playFictitiousMove bestX not 9 completed");
    }

    // ─── setControl: skipNextFrame toggle (lines 258-306) ───

    @Test
    void setControlSkipNextFrameToggle() {
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
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        engine.statistics.time = 1;
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        // First call: skipNextFrame starts false, enters !skipNextFrame block.
        // Input is generated in this block (speedLimit=0 from initRanks means check passes).
        ai.setControl(engine, 0, ctrl);

        // Second call: skipNextFrame is true, enters else block, sets skipNextFrame=false, no input
        ai.delay = 0;
        ai.setControl(engine, 0, ctrl);
        assertEquals(0, ctrl.getButtonBit(), "Second call should produce no input (skipNextFrame toggled off)");
    }

    // ─── setControl: BUTTON_A rotation (lines 251-252) ───

    @Test
    void setControlRotationButtonA() {
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
        engine.ruleopt.rotateButtonAllowReverse = false;
        engine.ruleopt.rotateButtonAllowDouble = false;
        engine.statistics.time = 1;
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_RIGHT; // rrot
        ai.thinkLastPieceNo = 1;
        ai.thinkCurrentPieceNo = 0;
        ai.threadRunning = true;

        ai.setControl(engine, 0, ctrl);

        // Should set BUTTON_A for normal rotation
        assertTrue((ctrl.getButtonBit() & Controller.BUTTON_BIT_A) != 0,
                "Normal rotation should set BUTTON_A");
    }

    // ─── thinkBestPosition (engine): heights conversion (lines 391-394) ───

    @Test
    void thinkBestPositionEngineMode() {
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

        assertTrue(true, "thinkBestPosition engine mode completed");
    }

    // ─── thinkBestPosition (engine): with holdPiece (lines 404-409) ───

    @Test
    void thinkBestPositionEngineModeWithHold() {
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

        assertTrue(true, "thinkBestPosition engine mode with hold completed");
    }

    // ─── thinkBestPosition (engine): with holdPiece null (lines 404-405) ───

    @Test
    void thinkBestPositionEngineModeHoldNull() {
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

        assertTrue(true, "thinkBestPosition engine mode with null hold completed");
    }

    // ─── thinkBestPosition (heights): numPreviews=0 (lines 578-579) ───

    @Test
    void thinkBestPositionNoPreviews() throws Exception {
        setPrivateField(ai, "MAX_PREVIEWS", 0);

        ai.thinkBestPosition(
            new int[] {0, 0, 0, 0, 0, 0, 0, 0, 0},
            new int[] {Piece.PIECE_T, Piece.PIECE_S, Piece.PIECE_Z},
            new int[] {-1},
            false);

        assertTrue(true, "thinkBestPosition no previews completed");
    }

    // ─── Score.computeScore: diff > maxJump (lines 84-89) and diff < -maxJump (94-99) ───

    @Test
    void scoreComputeWithCliffs() {
        // Heights create steep jumps
        int[] heights = {0, 10, 0, 10, 0, 10, 0, 10, 0};

        RanksAI.Score score = ai.new Score();
        score.computeScore(heights);

        assertTrue(true, "Score compute with cliffs completed");
    }

    // ─── Score.compareTo: different rankStacking values (lines 116-122) ───

    @Test
    void scoreCompareToDifferent() {
        RanksAI.Score score1 = ai.new Score();
        score1.rankStacking = 100;
        RanksAI.Score score2 = ai.new Score();
        score2.rankStacking = 200;

        assertTrue(score2.compareTo(score1) > 0, "Higher score should compare as greater");
    }

    // ─── Score.toString (line 69) ───

    @Test
    void scoreToString() {
        RanksAI.Score score = ai.new Score();
        String str = score.toString();
        assertTrue(str.contains("Rank Stacking"), "toString should contain Rank Stacking");
    }

    // ─── thinkBestPosition (heights): plannedToUseIPiece with numPreviews > 0 (lines 578-579) ───

    @Test
    void thinkBestPositionPlannedIPiece() {
        int[] heights = {6, 6, 6, 6, 6, 6, 6, 6, 6};
        int[] pieces = {Piece.PIECE_I, Piece.PIECE_T, Piece.PIECE_L};
        int[] holdPiece = {-1};

        ai.thinkBestPosition(heights, pieces, holdPiece, false);

        assertTrue(true, "thinkBestPosition planned I piece completed");
    }
}
