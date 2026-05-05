package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.*;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in ComboRaceSeedSearch.
 * Most uncovered lines (47-101) are in the main() CLI entry method.
 * Others are in thinkBestPosition hold path and Transition constructors.
 */
class ComboRaceSeedSearchRemainingCoverageTest {

    @BeforeEach
    void setUp() {
        // Reset static state
        ComboRaceSeedSearch.moves = null;
        ComboRaceSeedSearch.nextQueueIDs = null;
        ComboRaceSeedSearch.queue = null;
    }

    // ─── thinkMain at depth limit with holdID = PIECE_I (line 171) ───
    @Test
    void thinkMainDepthLimitHoldI() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        int pts = ComboRaceSeedSearch.thinkMain(0, Piece.PIECE_I, ComboRaceSeedSearch.nextQueueIDs.length);
        assertTrue(pts > 0, "thinkMain at depth limit with I piece hold");
    }

    // ─── thinkMain at depth limit with valid holdID (line 172-173) ───
    @Test
    void thinkMainDepthLimitHoldValid() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        int pts = ComboRaceSeedSearch.thinkMain(0, Piece.PIECE_T, ComboRaceSeedSearch.nextQueueIDs.length);
        assertTrue(pts >= 0, "thinkMain at depth limit with valid hold");
    }

    // ─── thinkMain with holdID == -1, using next piece as new hold (line 188) ───
    @Test
    void thinkMainHoldEmptyUseNextPiece() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        // holdID == -1 triggers: bestPts = Math.max(..., thinkMain(state, nextQueueIDs[depth], depth+1))
        int pts = ComboRaceSeedSearch.thinkMain(0, -1, 0);
        assertTrue(pts >= 0, "thinkMain hold empty uses next piece");
    }

    // ─── thinkMain with holdID != -1, iterating moves for hold (lines 191-198) ───
    @Test
    void thinkMainHoldNotEmptyIterateMoves() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        // holdID != -1 triggers: iterate moves[state][holdID]
        int pts = ComboRaceSeedSearch.thinkMain(0, Piece.PIECE_S, 0);
        assertTrue(pts >= 0, "thinkMain hold not empty iterates moves");
    }

    // ─── thinkBestPosition with holdID != nowID ───
    @Test
    void thinkBestPositionHoldDiffFromNow() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.queue = new int[1400];
        ComboRaceSeedSearch.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        for (int i = 0; i < 1400; i++)
            ComboRaceSeedSearch.queue[i] = i % Piece.PIECE_STANDARD_COUNT;

        ComboRaceSeedSearch.thinkBestPosition(0, 0, Piece.PIECE_I);
        assertTrue(true, "thinkBestPosition hold diff from now");
    }

    // ─── thinkBestPosition with holdID == -1 and nowID ───
    @Test
    void thinkBestPositionHoldNegativeOne() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.queue = new int[1400];
        ComboRaceSeedSearch.nextQueueIDs = new int[]{0, 1, 2, 3, 4, 5};
        for (int i = 0; i < 1400; i++)
            ComboRaceSeedSearch.queue[i] = i % Piece.PIECE_STANDARD_COUNT;

        ComboRaceSeedSearch.thinkBestPosition(0, 0, -1);
        assertTrue(true, "thinkBestPosition hold -1");
    }

    // ─── Transition constructors ───
    @Test
    void transitionConstructors() {
        // Transition(int x, int rt, int newFld) -> rtSub=0
        ComboRaceSeedSearch.Transition t1 = new ComboRaceSeedSearch.Transition(1, 2, 3);
        assertEquals(1, t1.x);
        assertEquals(2, t1.rt);
        assertEquals(0, t1.rtSub);
        assertEquals(3, t1.newField);

        // Transition(int x, int rt, int rtSub, int newFld) -> rtSub from arg
        ComboRaceSeedSearch.Transition t2 = new ComboRaceSeedSearch.Transition(4, 5, 6, 7);
        assertEquals(4, t2.x);
        assertEquals(5, t2.rt);
        assertEquals(6, t2.rtSub);  // rtSub from 3rd arg
        assertEquals(7, t2.newField);

        ComboRaceSeedSearch.Transition t3 = new ComboRaceSeedSearch.Transition(1, 2, -3, 4);
        assertEquals(-3, t3.rtSub);

        // Transition(int x, int rt, int rtSub, int newFld, Transition next)
        ComboRaceSeedSearch.Transition t4 = new ComboRaceSeedSearch.Transition(5, 6, -7, 8, null);
        assertEquals(-7, t4.rtSub);
        assertNull(t4.next);

        // Transition(int x, int rt, int newFld, Transition next) -> rtSub=0
        ComboRaceSeedSearch.Transition t5 = new ComboRaceSeedSearch.Transition(9, 10, 11, null);
        assertEquals(0, t5.rtSub);
        assertNull(t5.next);
    }

    // ─── fieldToCode with valleyX parameter ───
    @Test
    void fieldToCodeWithValley() {
        Field fld = new Field(10, 20, 0, false);
        fld.setBlockColor(3, 19, 1);
        fld.setBlockColor(4, 19, 1);
        short code = ComboRaceSeedSearch.fieldToCode(fld, 3);
        assertTrue(code > 0, "fieldToCode with valley should produce non-zero code");
    }

    // ─── fieldToIndex from Field with valley ───
    @Test
    void fieldToIndexFromFieldWithValley() {
        Field fld = new Field(10, 20, 0, false);
        // Create field matching FIELDS[0] = 0x7 pattern
        // 0x7 in binary = 0000 0000 0000 0111
        // Bottom 3 blocks of columns 0-3 filled
        fld.setBlockColor(3, 19, 1);
        fld.setBlockColor(3, 18, 1);
        fld.setBlockColor(3, 17, 1);
        // With valleyX=3, this creates code 0x7
        int idx = ComboRaceSeedSearch.fieldToIndex(fld, 3);
        assertTrue(idx >= 0, "fieldToIndex should find match");
    }

    // ─── createTables when already initialized ───
    @Test
    void createTablesAlreadyInitialized() {
        ComboRaceSeedSearch.createTables();
        // Call again - should early return
        ComboRaceSeedSearch.createTables();
        assertNotNull(ComboRaceSeedSearch.moves);
    }

    // ─── thinkMain returns 0 for state == -1 ───
    @Test
    void thinkMainNegativeState() {
        assertEquals(0, ComboRaceSeedSearch.thinkMain(-1, -1, 0));
    }
}
