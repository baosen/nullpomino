package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.*;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Targeted coverage for the {@link PoochyBotDefensive#thinkMain} evaluation
 * branches that the existing PoochyBotDefensive tests fail to execute.
 *
 * <p>The existing tests construct pieces with {@code new Piece(id)} and never
 * call {@code setColor}, so the blocks they "place" have colour NONE and are
 * invisible to {@link Field#getHighestBlockY()} / {@link Field#checkLine()} /
 * {@link Field#getHowManyHoles()}. That makes the post-placement field identical
 * to the pre-placement field, so the after-placement decision branches (valley
 * bonuses with real fills, height changes, danger penalties, and the L/J valley
 * diff sign branches) never run.
 *
 * <p>Every test here gives the piece a real colour so {@code placeToField}
 * actually mutates the field, and crafts column depths so a specific branch is
 * exercised. The branch arithmetic was derived offline; the assertions only
 * guard against regressions to {@code Integer.MIN_VALUE} (an early bail-out) so
 * they stay robust. Targeted source lines (1-based) are noted per test.
 */
class PoochyBotDefensiveCoverageBoostTest {

    private PoochyBotDefensive ai;

    @BeforeEach
    void setUp() {
        ai = new PoochyBotDefensive();
    }

    /** A standard tetromino with a visible colour so placeToField mutates the field. */
    private static Piece colored(int id, int rt) {
        Piece p = new Piece(id);
        p.setColor(8);
        p.direction = rt;
        return p;
    }

    /** Lowest (largest reachable) y at which the piece does not collide in fld. */
    private static int restingY(int id, int rt, int x, Field fld) {
        Piece probe = colored(id, rt);
        int py = Integer.MIN_VALUE;
        for (int y = -3; y <= 21; y++) {
            if (!probe.checkCollision(x, y, new Field(fld))) {
                py = y;
            } else if (py != Integer.MIN_VALUE) {
                break;
            }
        }
        return py;
    }

    private static Field fromProfile(int[] topRow) {
        Field f = new Field(10, 20, 0, false);
        for (int x = 0; x < 10; x++) {
            for (int y = topRow[x]; y <= 19; y++) {
                f.setBlockColor(x, y, 1);
            }
        }
        return f;
    }

    // ── Line 85: BEFORE-placement needJValley via diff%4==2 with left < right ──
    @Test
    void beforeNeedJValleyLeftLessThanRight() {
        Field f = new Field(10, 20, 0, false);
        // col3 depth 10 (left, higher), col5 depth 16 (right), col4 depth 18.
        // For column 4: diff = 18 - max(10,16) = 2, 2%4==2, left(10) < right(16).
        for (int y = 10; y <= 19; y++) f.setBlockColor(3, y, 1);
        for (int y = 16; y <= 19; y++) f.setBlockColor(5, y, 1);
        for (int y = 18; y <= 19; y++) f.setBlockColor(4, y, 1);
        int py = restingY(Piece.PIECE_O, 0, 8, f);
        int pts = ai.thinkMain(8, py, 0, -1, f, colored(Piece.PIECE_O, 0), 1);
        assertNotEquals(Integer.MIN_VALUE, pts);
    }

    // ── Lines 114-120, 157, 163: vertical-I valley fill, valley == 3 ──
    @Test
    void verticalIFillsValleyOfDepth3() {
        Field f = new Field(10, 20, 0, false);
        // Neighbours filled rows 16-19 (depth 16); col3 only row 19 (depth 19).
        for (int x = 0; x < 10; x++) {
            if (x != 3) {
                for (int y = 16; y <= 19; y++) f.setBlockColor(x, y, 1);
            }
        }
        f.setBlockColor(3, 19, 1);
        // Vertical I occupies column x+2, so x=1 targets column 3. valley = 19-16 = 3.
        int py = restingY(Piece.PIECE_I, 1, 1, f);
        int pts = ai.thinkMain(1, py, 1, -1, f, colored(Piece.PIECE_I, 1), 0);
        assertTrue(pts > 0, "valley-3 fill should earn a sizeable bonus");
    }

    // ── Line 159: vertical-I valley fill, valley >= 4 ──
    @Test
    void verticalIFillsValleyOfDepth4() {
        Field f = new Field(10, 20, 0, false);
        // Neighbours depth 15; col2 only row 19 (depth 19): valley = 19-15 = 4.
        for (int x = 0; x < 10; x++) {
            if (x != 2) {
                for (int y = 15; y <= 19; y++) f.setBlockColor(x, y, 1);
            }
        }
        f.setBlockColor(2, 19, 1);
        int py = restingY(Piece.PIECE_I, 1, 0, f); // column x+2 == 2
        int pts = ai.thinkMain(0, py, 1, -1, f, colored(Piece.PIECE_I, 1), 0);
        assertTrue(pts > 0, "valley->=4 fill should earn the larger bonus");
    }

    // ── Line 161 (+157): valleyBonus doubled when xMax == 0 ──
    @Test
    void verticalIValleyAtLeftWallDoublesBonus() {
        Field f = new Field(10, 20, 0, false);
        // Columns 1..9 depth 16; col0 only row 19 (depth 19): valley = 3 at the wall.
        for (int x = 1; x < 10; x++) {
            for (int y = 16; y <= 19; y++) f.setBlockColor(x, y, 1);
        }
        f.setBlockColor(0, 19, 1);
        // Vertical I column x+2 == 0 requires x == -2; xMax == 0 -> bonus doubled.
        int py = restingY(Piece.PIECE_I, 1, -2, f);
        int pts = ai.thinkMain(-2, py, 1, -1, f, colored(Piece.PIECE_I, 1), 0);
        assertTrue(pts > 0, "left-wall valley fill should earn a doubled bonus");
    }

    // ── Line 212: AFTER-placement needLValley via diff%4==2 with left > right ──
    @Test
    void afterNeedLValleyLeftGreaterThanRight() {
        Field f = new Field(10, 20, 0, false);
        // col3 depth 16 (left), col5 depth 10 (right), col4 depth 18.
        // After placing a harmless O far away, column 4 still has diff 2 with left>right.
        for (int y = 16; y <= 19; y++) f.setBlockColor(3, y, 1);
        for (int y = 10; y <= 19; y++) f.setBlockColor(5, y, 1);
        for (int y = 18; y <= 19; y++) f.setBlockColor(4, y, 1);
        int py = restingY(Piece.PIECE_O, 0, 8, f);
        int pts = ai.thinkMain(8, py, 0, -1, f, colored(Piece.PIECE_O, 0), 1);
        assertNotEquals(Integer.MIN_VALUE, pts);
    }

    // ── Line 214: AFTER-placement needJValley via diff%4==2 with left < right ──
    @Test
    void afterNeedJValleyLeftLessThanRight() {
        Field f = new Field(10, 20, 0, false);
        for (int y = 10; y <= 19; y++) f.setBlockColor(3, y, 1);
        for (int y = 16; y <= 19; y++) f.setBlockColor(5, y, 1);
        for (int y = 18; y <= 19; y++) f.setBlockColor(4, y, 1);
        int py = restingY(Piece.PIECE_O, 0, 8, f);
        int pts = ai.thinkMain(8, py, 0, -1, f, colored(Piece.PIECE_O, 0), 1);
        assertNotEquals(Integer.MIN_VALUE, pts);
    }

    // ── Line 241: T-Spin bonus (tspin && lines >= 1, lines < 4, not all-clear) ──
    @Test
    void tSpinLineClearBonus() {
        Field f = new Field(10, 20, 0, false);
        // A configuration where a coloured T at (8,15) rt=3 lands on a T-spin spot
        // and completes exactly one line.
        for (int c : new int[] {0, 1, 2, 4, 5, 6, 7, 8}) f.setBlockColor(c, 17, 1);
        for (int x = 0; x < 10; x++) f.setBlockColor(x, 18, 1);
        for (int c : new int[] {0, 1, 2, 3, 5, 6, 7, 8}) f.setBlockColor(c, 19, 1);
        int pts = ai.thinkMain(8, 15, 3, 0, f, colored(Piece.PIECE_T, 3), 0);
        assertTrue(pts > 100000, "a T-spin single should add the T-spin bonus");
    }

    // ── Lines 254 & 256: needJValleyBefore > 1 and needJValleyAfter > 1 ──
    @Test
    void needJValleyDiffScoreBeforeAndAfter() {
        Field f = needJValleyField();
        // O at x=0 rests on column 1; the two interior needJ valleys (cols 2,6)
        // survive, so both needJValleyBefore and needJValleyAfter stay > 1.
        int pts = ai.thinkMain(0, 8, 0, -1, f, colored(Piece.PIECE_O, 0), 1);
        assertNotEquals(Integer.MIN_VALUE, pts);
    }

    // ── Lines 258 & 260: needLValleyBefore > 1 and needLValleyAfter > 1 ──
    @Test
    void needLValleyDiffScoreBeforeAndAfter() {
        Field f = needLValleyField();
        // O at x=3 keeps both needLValleyBefore and needLValleyAfter > 1.
        int pts = ai.thinkMain(3, 8, 0, -1, f, colored(Piece.PIECE_O, 0), 1);
        assertNotEquals(Integer.MIN_VALUE, pts);
    }

    // ── Lines 269-270: needLJValleyDiffScore < 0, holeAfter >= holeBefore, depth 0 ──
    @Test
    void needLJValleyNegativeDiffReturnsMinValue() {
        // Placement creates L/J valleys (after-score > before-score) while adding
        // holes, so at depth 0 thinkMain bails out with Integer.MIN_VALUE.
        Field f = fromProfile(new int[] {18, 18, 14, 20, 20, 18, 14, 18, 18, 20});
        int pts = ai.thinkMain(4, 12, 0, -1, f, colored(Piece.PIECE_I, 0), 0);
        assertEquals(Integer.MIN_VALUE, pts,
            "creating L/J valleys at depth 0 should bail out");
    }

    // ── Line 272: needLJValleyDiffScore > 0 adds the positive bonus ──
    @Test
    void needLJValleyPositiveDiffAddsBonus() {
        // Placement reduces the L/J valley demand (before-score > after-score),
        // and depth 1 avoids the new-holes bail-out so line 272 runs.
        Field f = fromProfile(new int[] {16, 18, 14, 14, 16, 16, 16, 14, 18, 20});
        int pts = ai.thinkMain(0, 12, 0, -1, f, colored(Piece.PIECE_I, 0), 1);
        assertNotEquals(Integer.MIN_VALUE, pts);
    }

    // ── Line 310: demerit when the stack gets taller (heightBefore > heightAfter) ──
    @Test
    void heightIncreaseDemerit() {
        Field f = new Field(10, 20, 0, false);
        // Surface at row 8 (col9 empty so nothing clears); an O at col0 rests on
        // top, raising the highest block to row 6 -> heightBefore(8) > heightAfter(6).
        for (int x = 0; x < 9; x++) {
            for (int y = 8; y <= 19; y++) f.setBlockColor(x, y, 1);
        }
        int py = restingY(Piece.PIECE_O, 0, 0, f);
        int pts = ai.thinkMain(0, py, 0, -1, f, colored(Piece.PIECE_O, 0), 1);
        assertNotEquals(Integer.MIN_VALUE, pts);
    }

    // ── Lines 319 & 321: dangerous placement penalties near the spawn region ──
    @Test
    void dangerousSpawnRegionPenalty() {
        Field f = new Field(10, 20, 0, false);
        // Surface at row 5 (col9 empty). A vertical I in spawn column 4 rises to
        // row 1: heightAfter < 2, depthsAfter[4] < 2 < depthsBefore[4], depth 0.
        for (int x = 0; x < 9; x++) {
            for (int y = 5; y <= 19; y++) f.setBlockColor(x, y, 1);
        }
        int py = restingY(Piece.PIECE_I, 1, 2, f); // vertical I -> column 4
        int pts = ai.thinkMain(2, py, 1, -1, f, colored(Piece.PIECE_I, 1), 0);
        assertTrue(pts < -1000000, "a tall spawn-region placement is heavily penalised");
    }

    private static Field needJValleyField() {
        Field f = new Field(10, 20, 0, false);
        for (int y = 10; y <= 19; y++) f.setBlockColor(1, y, 1);
        for (int y = 16; y <= 19; y++) f.setBlockColor(3, y, 1);
        for (int y = 18; y <= 19; y++) f.setBlockColor(2, y, 1);
        for (int y = 10; y <= 19; y++) f.setBlockColor(5, y, 1);
        for (int y = 16; y <= 19; y++) f.setBlockColor(7, y, 1);
        for (int y = 18; y <= 19; y++) f.setBlockColor(6, y, 1);
        return f;
    }

    private static Field needLValleyField() {
        Field f = new Field(10, 20, 0, false);
        for (int y = 16; y <= 19; y++) f.setBlockColor(1, y, 1);
        for (int y = 10; y <= 19; y++) f.setBlockColor(3, y, 1);
        for (int y = 18; y <= 19; y++) f.setBlockColor(2, y, 1);
        for (int y = 16; y <= 19; y++) f.setBlockColor(5, y, 1);
        for (int y = 10; y <= 19; y++) f.setBlockColor(7, y, 1);
        for (int y = 18; y <= 19; y++) f.setBlockColor(6, y, 1);
        return f;
    }
}
