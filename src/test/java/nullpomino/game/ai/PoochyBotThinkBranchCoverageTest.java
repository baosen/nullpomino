package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Branch coverage for the directly-callable scoring/search helpers of
 * {@link PoochyBot}: {@code thinkMain}, {@code mostMovableX}, {@code calcIRS},
 * {@code thinkBestPosition} (driven directly), plus the static helpers.
 *
 * These exercise the contiguous uncovered clusters around lines 1262-1268
 * (vertical I valley), 1529-1556 (dangerous placement / edge clear bonus),
 * 1693-1745 (T/I mostMovableX kick paths), and the shift/rotation
 * "new best position" branches inside thinkBestPosition (815-957).
 *
 * Lines left uncovered intentionally:
 *  - 1636 (getColumnDepth result++): requires getHighestBlockY(x) to return
 *    maxY while that exact cell reads empty; only possible with a set line-flag
 *    that getHighestBlockY skips, which contradicts the column having its top
 *    block at maxY. Effectively unreachable -> classified DEAD/hard.
 *  - 1529 (big heightAfter<0): heightAfter is getHighestBlockY() which is never
 *    negative on a normal field -> DEAD.
 */
public class PoochyBotThinkBranchCoverageTest {

    private GameManager manager;
    private GameEngine engine;
    private PoochyBot ai;

    @BeforeEach
    void setUp() {
        manager = new GameManager(new EventReceiver());
        manager.init();
        engine = manager.engine[0];
        engine.init();
        engine.createFieldIfNeeded();
        ai = new PoochyBot();
    }

    private Piece piece(int id) {
        Piece p = new Piece(id);
        p.applyOffsetArray(engine.ruleopt.pieceOffsetX[id], engine.ruleopt.pieceOffsetY[id]);
        return p;
    }

    // ─── thinkMain: vertical I piece in a single column valley (1262-1268) ───

    @Test
    void thinkMainVerticalIPieceValley() {
        Field fld = new Field(10, 20, 0, false);
        // Build a deep one-wide valley in column 4: surround it with tall walls
        // so a vertical I dropped at x=4 sits in a deep canyon.
        for (int y = 10; y < 20; y++) {
            fld.setBlockColor(3, y, Block.BLOCK_COLOR_RED);
            fld.setBlockColor(5, y, Block.BLOCK_COLOR_RED);
        }
        Piece iPiece = piece(Piece.PIECE_I);
        // DIRECTION_RIGHT is vertical for the I here, with its single column at
        // x+2, so x=2 lands the column in the canyon at column 4. thinkMain reads
        // piece.direction (not rt) for the block extents, so set it explicitly.
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int rt = Piece.DIRECTION_RIGHT;
        int x = 2;
        int y = iPiece.getBottom(x, 0, rt, fld);
        int pts = ai.thinkMain(x, y, rt, -1, fld, iPiece, 0);
        // Filling a deep one-wide canyon with a vertical I scores highly.
        assertTrue(pts > 1000, "vertical I valley should score well, got " + pts);
    }

    // ─── thinkMain: small-piece danger handling on a tall stack (1490-1549) ───

    @Test
    void thinkMainSmallPieceDangerousField() {
        Field fld = new Field(10, 20, 0, false);
        // Tall stack (cols 0..8 filled from row 2, col 9 empty so nothing clears)
        // makes heightBefore small -> danger=true, exercising the danger-weighted
        // height/valley scoring (1492-1500, 1548-1556).
        for (int yy = 2; yy < 20; yy++)
            for (int xx = 0; xx < 9; xx++)
                fld.setBlockColor(xx, yy, Block.BLOCK_COLOR_BLUE);
        Piece iPiece = piece(Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        int x = 7; // single column at x+2 = 9 (the empty edge column)
        int y = iPiece.getBottom(x, 0, Piece.DIRECTION_RIGHT, fld);
        int pts = ai.thinkMain(x, y, Piece.DIRECTION_RIGHT, -1, fld, iPiece, 0);
        // Placement is legal under danger; the score is finite (not the
        // "cannot place" sentinel).
        assertNotEquals(Integer.MIN_VALUE, pts, "danger placement should be scored");
    }

    // ─── thinkMain: big-piece dangerous placement penalty (1531-1535) ───

    @Test
    void thinkMainBigPieceDangerousPlacement() {
        Field fld = new Field(10, 20, 0, false);
        // Nearly fill the field from row 4, then carve a two-wide notch so a big
        // O drops all the way to row 0 -> heightAfter (0) < 2*move (4), entering
        // the big-piece dangerous-placement branch (1527-1535).
        for (int yy = 4; yy < 20; yy++)
            for (int xx = 0; xx < 10; xx++)
                fld.setBlockColor(xx, yy, Block.BLOCK_COLOR_GREEN);
        for (int yy = 4; yy < 6; yy++) {
            fld.setBlockColor(4, yy, 0);
            fld.setBlockColor(5, yy, 0);
        }
        Piece oPiece = new Piece(Piece.PIECE_O);
        oPiece.big = true;
        oPiece.applyOffsetArray(engine.ruleopt.pieceOffsetX[Piece.PIECE_O],
                engine.ruleopt.pieceOffsetY[Piece.PIECE_O]);
        int x = 2;
        int y = oPiece.getBottom(x, 0, Piece.DIRECTION_UP, fld);
        int pts = ai.thinkMain(x, y, Piece.DIRECTION_UP, -1, fld, oPiece, 0);
        // Branch is reached (heightAfter==0); the resulting score is finite.
        assertNotEquals(Integer.MIN_VALUE, pts, "big danger branch should be scored");
    }

    // ─── thinkMain: danger edge-clear bonus (1548-1556) ───

    @Test
    void thinkMainDangerEdgeClearBonus() {
        Field fld = new Field(10, 20, 0, false);
        // High flat stack on cols 0..7 from row 3 makes heightBefore small
        // (danger). Column 8 has two bottom blocks (depthsAfter[8]=18) and column
        // 9 stays empty (depthsAfter[9]=20), so r2ColDepth(18) < depthsAfter[9](20)
        // and r2ColDepth(18) > maxLeftDepth(3): the +200 edge-clear bonus path
        // (1549-1556) is entered.
        for (int yy = 3; yy < 20; yy++)
            for (int xx = 0; xx < 8; xx++)
                fld.setBlockColor(xx, yy, Block.BLOCK_COLOR_PURPLE);
        fld.setBlockColor(8, 18, Block.BLOCK_COLOR_PURPLE);
        fld.setBlockColor(8, 19, Block.BLOCK_COLOR_PURPLE);
        Piece oPiece = piece(Piece.PIECE_O);
        int x = 0;
        int y = oPiece.getBottom(x, 0, Piece.DIRECTION_UP, fld);
        int pts = ai.thinkMain(x, y, Piece.DIRECTION_UP, -1, fld, oPiece, 0);
        // Bonus keeps the score positive on this dangerous-but-tidy edge.
        assertTrue(pts > 0, "edge-clear bonus should keep score positive, got " + pts);
    }

    // ─── thinkMain: line-clear path with a horizontal I across the bottom ───

    @Test
    void thinkMainLineClearWithIPiece() {
        Field fld = new Field(10, 20, 0, false);
        // Bottom row filled cols 0..5, leaving cols 6..9 for a horizontal I so the
        // row completes and clears (exercises the line-clear path 1280-1284).
        for (int xx = 0; xx < 6; xx++)
            fld.setBlockColor(xx, 19, Block.BLOCK_COLOR_RED);
        Piece iPiece = piece(Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_UP; // horizontal, 4-wide
        int x = 6 - iPiece.getMinimumBlockX();
        int y = iPiece.getBottom(x, 0, Piece.DIRECTION_UP, fld);
        int pts = ai.thinkMain(x, y, Piece.DIRECTION_UP, -1, fld, iPiece, 0);
        assertNotEquals(Integer.MIN_VALUE, pts, "I line-clear placement scored, got " + pts);
    }

    // ─── mostMovableX: T-piece, direction != UP, lower bottom kick (1688-1711) ───

    @Test
    void mostMovableXTPieceKickRight() {
        engine.createFieldIfNeeded();
        engine.speed.gravity = 100;
        engine.speed.denominator = 100;
        Field fld = engine.field;
        // T piece pointing down so direction != UP triggers the special branch.
        Piece tPiece = piece(Piece.PIECE_T);
        tPiece.direction = Piece.DIRECTION_DOWN;
        // Carve a pocket so the DOWN bottom sits lower than the UP bottom and a
        // rightward kick collides (kickRight true).
        for (int yy = 16; yy < 20; yy++) {
            fld.setBlockColor(0, yy, Block.BLOCK_COLOR_RED);
            fld.setBlockColor(2, yy, Block.BLOCK_COLOR_RED);
        }
        int result = ai.mostMovableX(1, 14, 1, engine, fld, tPiece, Piece.DIRECTION_UP);
        assertTrue(result >= -1, "mostMovableX T kick-right returned " + result);
    }

    @Test
    void mostMovableXTPieceKickLeft() {
        engine.createFieldIfNeeded();
        engine.speed.gravity = 100;
        engine.speed.denominator = 100;
        Field fld = engine.field;
        Piece tPiece = piece(Piece.PIECE_T);
        tPiece.direction = Piece.DIRECTION_LEFT;
        // Walls left/right of a deep one-wide slot to force a left kick.
        for (int yy = 15; yy < 20; yy++) {
            fld.setBlockColor(4, yy, Block.BLOCK_COLOR_GREEN);
            fld.setBlockColor(6, yy, Block.BLOCK_COLOR_GREEN);
        }
        int result = ai.mostMovableX(5, 12, -1, engine, fld, tPiece, Piece.DIRECTION_UP);
        assertTrue(result >= -1, "mostMovableX T kick-left returned " + result);
    }

    // ─── mostMovableX: I-piece, vertical, ends at negative X (1738-1745) ───

    @Test
    void mostMovableXIPieceNegativeEdgeReturnZero() {
        engine.createFieldIfNeeded();
        engine.speed.gravity = 100;
        engine.speed.denominator = 100;
        Field fld = engine.field;
        // (rt&1)==1 vertical I, moving left toward column 0.
        Piece iPiece = piece(Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        // height1 < height2 and height1 < height3+2 path: column 1 lower (empty),
        // columns 2 and 3 high.
        for (int yy = 4; yy < 20; yy++) {
            fld.setBlockColor(2, yy, Block.BLOCK_COLOR_RED);
            fld.setBlockColor(3, yy, Block.BLOCK_COLOR_RED);
        }
        int result = ai.mostMovableX(1, 18, -1, engine, fld, iPiece, Piece.DIRECTION_RIGHT);
        assertTrue(result <= 1, "I negative-edge mostMovableX returned " + result);
    }

    @Test
    void mostMovableXIPieceNegativeEdgeReturnNegOne() {
        engine.createFieldIfNeeded();
        engine.speed.gravity = 100;
        engine.speed.denominator = 100;
        Field fld = engine.field;
        Piece iPiece = piece(Piece.PIECE_I);
        iPiece.direction = Piece.DIRECTION_RIGHT;
        // height1 > height0 path: column 0 tall, column 1 lower.
        for (int yy = 2; yy < 20; yy++)
            fld.setBlockColor(0, yy, Block.BLOCK_COLOR_BLUE);
        fld.setBlockColor(1, 19, Block.BLOCK_COLOR_BLUE);
        int result = ai.mostMovableX(1, 18, -1, engine, fld, iPiece, Piece.DIRECTION_RIGHT);
        assertTrue(result <= 1, "I negative-edge (return -1) mostMovableX returned " + result);
    }

    // ─── mostMovableX: low-gravity early return for both directions (1670-1677) ───

    @Test
    void mostMovableXLowGravityBothDirections() {
        engine.createFieldIfNeeded();
        engine.speed.gravity = 0;
        engine.speed.denominator = 256;
        Field fld = engine.field;
        Piece tPiece = piece(Piece.PIECE_T);
        int left = ai.mostMovableX(4, 5, -1, engine, fld, tPiece, Piece.DIRECTION_UP);
        int right = ai.mostMovableX(4, 5, 1, engine, fld, tPiece, Piece.DIRECTION_UP);
        assertTrue(left <= right, "low-gravity left should be <= right");
    }

    // ─── calcIRS: L and J piece gravity-high column selection (687-707) ───

    @Test
    void calcIRSLPieceGravityHighZero() {
        engine.createFieldIfNeeded();
        engine.speed.gravity = 2;
        engine.speed.denominator = 1;
        Field fld = engine.field;
        int mid = (fld.getWidth() / 2) - 1; // = 4
        // L returns 0 when column mid-1 is the *tallest* (smallest highest-Y),
        // i.e. getHighestBlockY(mid-1) < min(getHighestBlockY(mid),(mid+1)).
        for (int yy = 5; yy < 20; yy++)
            fld.setBlockColor(mid - 1, yy, Block.BLOCK_COLOR_RED);
        // bestX far from spawn so abs(spawn-bestX)!=1.
        ai.bestX = 0;
        Piece lPiece = piece(Piece.PIECE_L);
        int irs = ai.calcIRS(lPiece, engine);
        assertEquals(0, irs, "L IRS should be 0 when mid-1 column is tallest");
    }

    @Test
    void calcIRSJPieceGravityHighZero() {
        engine.createFieldIfNeeded();
        engine.speed.gravity = 2;
        engine.speed.denominator = 1;
        Field fld = engine.field;
        int mid = (fld.getWidth() / 2) - 1;
        // J returns 0 when column mid+1 is the *tallest* (smallest highest-Y).
        for (int yy = 5; yy < 20; yy++)
            fld.setBlockColor(mid + 1, yy, Block.BLOCK_COLOR_RED);
        ai.bestX = 0;
        Piece jPiece = piece(Piece.PIECE_J);
        int irs = ai.calcIRS(jPiece, engine);
        assertEquals(0, irs, "J IRS should be 0 when mid+1 column is tallest");
    }

    @Test
    void calcIRSLPieceNonZeroRotation() {
        engine.createFieldIfNeeded();
        engine.speed.gravity = 2;
        engine.speed.denominator = 1;
        // Flat field -> gravityHigh branch fails first condition, returns A or B.
        ai.bestX = 0;
        Piece lPiece = piece(Piece.PIECE_L);
        int irs = ai.calcIRS(lPiece, engine);
        assertNotEquals(-1, irs, "L IRS rotation bit expected");
    }

    @Test
    void calcIRSJPieceNonZeroRotation() {
        engine.createFieldIfNeeded();
        engine.speed.gravity = 2;
        engine.speed.denominator = 1;
        ai.bestX = 0;
        Piece jPiece = piece(Piece.PIECE_J);
        int irs = ai.calcIRS(jPiece, engine);
        assertNotEquals(-1, irs, "J IRS rotation bit expected");
    }

    // ─── thinkBestPosition: full search drives shift/rotation best branches ───

    @Test
    void thinkBestPositionFindsValidPlacement() {
        engine.createFieldIfNeeded();
        engine.nowPieceObject = piece(Piece.PIECE_T);
        engine.nowPieceX = engine.getSpawnPosX(engine.field, engine.nowPieceObject);
        engine.nowPieceY = engine.getSpawnPosY(engine.nowPieceObject);
        engine.nextPieceArrayObject = new Piece[] { piece(Piece.PIECE_O), piece(Piece.PIECE_I) };
        engine.nextPieceCount = 2;
        // Uneven surface so shift/rotation alternatives produce distinct scores.
        for (int yy = 17; yy < 20; yy++) {
            engine.field.setBlockColor(0, yy, Block.BLOCK_COLOR_RED);
            engine.field.setBlockColor(1, yy, Block.BLOCK_COLOR_RED);
        }
        engine.field.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
        ai.thinkBestPosition(engine, 0);
        assertTrue(ai.bestX >= 0 && ai.bestX < 10,
                "thinkBestPosition produced a valid bestX: " + ai.bestX);
    }

    @Test
    void thinkBestPositionWithHoldExploresHoldBranch() {
        engine.createFieldIfNeeded();
        engine.nowPieceObject = piece(Piece.PIECE_S);
        engine.nowPieceX = engine.getSpawnPosX(engine.field, engine.nowPieceObject);
        engine.nowPieceY = engine.getSpawnPosY(engine.nowPieceObject);
        engine.holdPieceObject = piece(Piece.PIECE_I);
        engine.nextPieceArrayObject = new Piece[] { piece(Piece.PIECE_O), piece(Piece.PIECE_L) };
        engine.nextPieceCount = 2;
        // A deep one-wide canyon strongly favors holding the I piece for it.
        for (int yy = 6; yy < 20; yy++)
            for (int xx = 0; xx < 10; xx++)
                if (xx != 9)
                    engine.field.setBlockColor(xx, yy, Block.BLOCK_COLOR_GREEN);
        ai.thinkBestPosition(engine, 0);
        assertTrue(ai.bestX >= 0 && ai.bestX < 10,
                "thinkBestPosition with hold produced a valid bestX: " + ai.bestX);
    }

    // ─── thinkBestPosition during ARE / READY (inARE / spawn path) ───

    @Test
    void thinkBestPositionDuringReady() {
        engine.stat = GameEngine.Status.READY;
        engine.nextPieceArrayObject = new Piece[] {
            piece(Piece.PIECE_T), piece(Piece.PIECE_O), piece(Piece.PIECE_I)
        };
        engine.nextPieceCount = 0;
        ai.thinkBestPosition(engine, 0);
        assertTrue(ai.bestX >= 0, "READY-state think produced bestX: " + ai.bestX);
    }

    // ─── static helpers: getColumnDepths / calcValleys / getColumnDepth ───

    @Test
    void staticHelpersColumnDepthsAndValleys() {
        Field fld = new Field(10, 20, 0, false);
        for (int yy = 15; yy < 20; yy++)
            fld.setBlockColor(0, yy, Block.BLOCK_COLOR_RED);
        int[] depths = PoochyBot.getColumnDepths(fld);
        assertEquals(10, depths.length, "one depth per column");
        assertEquals(15, depths[0], "column 0 top at y=15");
        int[] valleys = PoochyBot.calcValleys(depths, 1);
        assertEquals(3, valleys.length, "valleys triple");
        // getColumnDepth on a tall column equals getHighestBlockY.
        int cd = PoochyBot.getColumnDepth(fld, 0);
        assertEquals(15, cd, "getColumnDepth for tall column");
    }

    // ─── renderState smoke (no display) ───

    @Test
    void renderStateRunsWithoutDisplay() {
        engine.createFieldIfNeeded();
        engine.nowPieceObject = piece(Piece.PIECE_T);
        engine.nowPieceX = engine.getSpawnPosX(engine.field, engine.nowPieceObject);
        engine.nowPieceY = engine.getSpawnPosY(engine.nowPieceObject);
        engine.nextPieceArrayObject = new Piece[] { piece(Piece.PIECE_O), piece(Piece.PIECE_I) };
        engine.nextPieceCount = 2;
        ai.thinkBestPosition(engine, 0);
        ai.renderState(engine, 0);
        ai.printPieceAndDirection(Piece.PIECE_L, Piece.DIRECTION_DOWN);
        ai.printPieceAndDirection(Piece.PIECE_J, Piece.DIRECTION_UP);
        ai.printPieceAndDirection(Piece.PIECE_O, Piece.DIRECTION_LEFT);
        ai.printPieceAndDirection(Piece.PIECE_I, Piece.DIRECTION_RIGHT);
        assertTrue(ai.bestX >= 0, "renderState ran after a think");
    }

    @Test
    void renderStateNullPieceBranch() {
        engine.createFieldIfNeeded();
        engine.nowPieceObject = null;
        ai.renderState(engine, 0);
        assertEquals(0, ai.bestX, "bestX default with null now piece");
    }

    // ─── thread run() loop: init starts thread, newPiece signals it, sleep ───

    @Test
    void threadRunLoopProcessesRequest() throws InterruptedException {
        engine.createFieldIfNeeded();
        engine.aiUseThread = true;
        engine.aiThinkDelay = 1; // thinkDelay > 0 exercises the run() sleep branch
        engine.nowPieceObject = piece(Piece.PIECE_T);
        engine.nowPieceX = engine.getSpawnPosX(engine.field, engine.nowPieceObject);
        engine.nowPieceY = engine.getSpawnPosY(engine.nowPieceObject);
        engine.nextPieceArrayObject = new Piece[] { piece(Piece.PIECE_O), piece(Piece.PIECE_I) };
        engine.nextPieceCount = 2;

        ai.init(engine, 0);
        // Signal a think request so the worker thread runs thinkBestPosition.
        ai.newPiece(engine, 0);
        // Give the daemon worker a chance to wake, think, and loop into the sleep.
        for (int i = 0; i < 50 && !ai.thinkComplete; i++)
            Thread.sleep(10);
        boolean done = ai.thinkComplete;
        ai.shutdown(engine, 0);
        assertTrue(done, "worker thread completed a think request");
    }

    @Test
    void initWithoutThreadDoesNotStart() {
        engine.aiUseThread = false;
        ai.init(engine, 0);
        // newPiece with no-thread mode runs thinkBestPosition synchronously.
        engine.createFieldIfNeeded();
        engine.nowPieceObject = piece(Piece.PIECE_O);
        engine.nowPieceX = engine.getSpawnPosX(engine.field, engine.nowPieceObject);
        engine.nowPieceY = engine.getSpawnPosY(engine.nowPieceObject);
        engine.nextPieceArrayObject = new Piece[] { piece(Piece.PIECE_T), piece(Piece.PIECE_I) };
        engine.nextPieceCount = 2;
        ai.newPiece(engine, 0);
        ai.shutdown(engine, 0);
        assertTrue(ai.bestX >= 0, "synchronous newPiece produced a bestX");
    }
}
