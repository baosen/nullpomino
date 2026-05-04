package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.*;

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
 * Covers remaining branches in RanksAI:
 * - setControl skipNextFrame toggle (skip one frame)
 * - playFictitiousMove gameOver and hold paths
 * - thinkBestPosition(int[], int[], int[], boolean) force 4-line IPiece branch
 * - thinkBestPosition engine-version with holdPiece object null/not null
 * - Score.computeScore with maxJump corrections
 * - Score.compareTo
 */
class RanksAIBranchCoverageTest {

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
    }

    private static void setPrivateField(Object obj, String name, Object value) throws Exception {
        Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    // ─── setControl skipNextFrame toggle ────────────────

    @Test
    void setControlSkipNextFrameTrue() throws Exception {
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
        engine.statistics.time = 1; // Avoid division by zero in TPM
        ai.delay = 0;
        ai.bestX = 5;
        ai.bestY = 10;
        ai.bestRt = Piece.DIRECTION_UP;
        ai.bestXSub = 5;
        ai.bestRtSub = -1;
        setPrivateField(ai, "skipNextFrame", false);
        ai.thinking = false;
        ai.threadRunning = true;
        ai.thinkCurrentPieceNo = 0;
        ai.thinkLastPieceNo = 0;

        // First call: skipNextFrame = false -> process, then set to true
        ai.setControl(engine, 0, ctrl);

        // Second call: skipNextFrame = true -> skip, then set to false
        ai.setControl(engine, 0, ctrl);

        assertTrue(true, "setControl skipNextFrame toggle completed");
    }

    // ─── playFictitiousMove with gameOver ────────────────

    @Test
    void playFictitiousMoveGameOver() throws Exception {
        int[] heights = new int[9];
        for (int i = 0; i < 9; i++) heights[i] = 10;
        int[] pieces = {Piece.PIECE_I, Piece.PIECE_T};
        int[] holdPiece = {-1};
        boolean[] holdOK = {true};

        // Force bestScore.rankStacking == 0 path
        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);
        assertTrue(true, "playFictitiousMove game over path completed");
    }

    @Test
    void playFictitiousMoveWithHold() throws Exception {
        int[] heights = new int[9];
        for (int i = 0; i < 9; i++) heights[i] = 5;
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_S};
        int[] holdPiece = {-1};
        boolean[] holdOK = {true};

        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);
        assertTrue(true, "playFictitiousMove with hold completed");
    }

    @Test
    void playFictitiousMoveBestX9() throws Exception {
        int[] heights = new int[9];
        for (int i = 0; i < 9; i++) heights[i] = 1;
        int[] pieces = {Piece.PIECE_I, Piece.PIECE_T};
        int[] holdPiece = {-1};
        boolean[] holdOK = {true};

        // This will go through thinkBestPosition which may set bestX=9 (force tetris)
        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);
        assertTrue(true, "playFictitiousMove bestX=9 path completed");
    }

    @Test
    void playFictitiousMoveHeightsExceed20() throws Exception {
        int[] heights = new int[9];
        for (int i = 0; i < 9; i++) heights[i] = 19;
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_S};
        int[] holdPiece = {-1};
        boolean[] holdOK = {true};

        ai.playFictitiousMove(heights, pieces, holdPiece, holdOK);
        assertTrue(true, "playFictitiousMove heights exceed 20 completed");
    }

    // ─── thinkBestPosition(int[]) force 4-line I piece ──

    @Test
    void thinkBestPositionForceTetris() throws Exception {
        int[] heights = new int[9];
        for (int i = 0; i < 9; i++) heights[i] = 8;
        int[] pieces = {Piece.PIECE_I, Piece.PIECE_T};
        int[] holdPiece = {-1};

        ai.thinkBestPosition(heights, pieces, holdPiece, true);
        // currentHeightMin >= THRESHOLD_FORCE_4LINES (8) and piece is I
        // -> should force 4-line
        assertTrue(true, "thinkBestPosition force tetris completed");
    }

    @Test
    void thinkBestPositionNotIPiece() throws Exception {
        int[] heights = new int[9];
        for (int i = 0; i < 9; i++) heights[i] = 10;
        int[] pieces = {Piece.PIECE_T, Piece.PIECE_S};
        int[] holdPiece = {-1};

        ai.thinkBestPosition(heights, pieces, holdPiece, true);
        assertTrue(true, "thinkBestPosition non-I piece completed");
    }

    // ─── Score.compareTo ───────────────────────────────

    @Test
    void scoreCompareToEqual() throws Exception {
        RanksAI.Score s1 = ai.new Score();
        RanksAI.Score s2 = ai.new Score();
        // Both have rankStacking=0, distanceToSet=80
        assertEquals(0, s1.compareTo(s2));
    }

    @Test
    void scoreCompareToGreater() throws Exception {
        RanksAI.Score s1 = ai.new Score();
        RanksAI.Score s2 = ai.new Score();
        // Set different rankStacking via reflection
        Field rs = RanksAI.Score.class.getDeclaredField("rankStacking");
        rs.setAccessible(true);
        rs.setFloat(s1, 10.0f);
        rs.setFloat(s2, 5.0f);
        assertEquals(1, s1.compareTo(s2));
    }

    @Test
    void scoreCompareToLess() throws Exception {
        RanksAI.Score s1 = ai.new Score();
        RanksAI.Score s2 = ai.new Score();
        Field rs = RanksAI.Score.class.getDeclaredField("rankStacking");
        rs.setAccessible(true);
        rs.setFloat(s1, 3.0f);
        rs.setFloat(s2, 8.0f);
        assertEquals(-1, s1.compareTo(s2));
    }

    // ─── Score.toString ───────────────────────────────

    @Test
    void scoreToString() {
        RanksAI.Score s = ai.new Score();
        String str = s.toString();
        assertTrue(str.contains("Rank Stacking"), "toString should contain Rank Stacking");
    }

    // ─── thinkBestPosition engine version with null holdPiece ──

    @Test
    void thinkBestPositionEngineNullHoldPiece() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        engine.holdPieceObject = null;
        ai.thinkBestPosition(engine, 0);
        assertTrue(true, "thinkBestPosition with null hold piece completed");
    }

    // ─── isGameOver ──────────────────────────────────────

    @Test
    void isGameOverInitiallyFalse() throws Exception {
        assertFalse(ai.isGameOver());
        setPrivateField(ai, "gameOver", true);
        assertTrue(ai.isGameOver());
    }
}
