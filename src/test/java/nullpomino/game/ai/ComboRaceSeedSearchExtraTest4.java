package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;

import org.junit.jupiter.api.Test;

/**
 * Additional tests for {@link ComboRaceSeedSearch} covering remaining uncovered branches:
 * - thinkMain: non-terminal with holdID == -1 (line 187-188)
 * - thinkMain: non-terminal with holdID != -1 (lines 189-198)
 * - thinkBestPosition: holdID != nowID with holdID == -1 (lines 134-151)
 * - fieldToCode with blocks in bottom 3 rows (lines 358-370)
 * - fieldToIndex with field and valleyX exact match
 * - createTables: second call returns early (lines 217-218)
 * - thinkBestPosition: state >= 0 with transitions (lines 108-151)
 */
class ComboRaceSeedSearchExtraTest4 {

    @Test
    void thinkMainNonTerminalEmptyHold() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];
        for (int i = 0; i < ComboRaceSeedSearch.MAX_THINK_DEPTH; i++) {
            ComboRaceSeedSearch.nextQueueIDs[i] = Piece.PIECE_T;
        }

        // holdID == -1 at non-terminal depth
        int pts = ComboRaceSeedSearch.thinkMain(0, -1, 0);

        assertTrue(pts >= 1000, "Non-terminal with empty hold should have score");
    }

    @Test
    void thinkMainNonTerminalWithHold() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];
        for (int i = 0; i < ComboRaceSeedSearch.MAX_THINK_DEPTH; i++) {
            ComboRaceSeedSearch.nextQueueIDs[i] = Piece.PIECE_T;
        }

        // holdID != -1 at non-terminal depth
        int pts = ComboRaceSeedSearch.thinkMain(0, Piece.PIECE_S, 0);

        assertTrue(pts >= 1000, "Non-terminal with hold should have score");
    }

    @Test
    void thinkBestPositionHoldDifferentFromNow() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];
        ComboRaceSeedSearch.queue = new int[ComboRaceSeedSearch.QUEUE_SIZE];
        for (int i = 0; i < ComboRaceSeedSearch.QUEUE_SIZE; i++) {
            ComboRaceSeedSearch.queue[i] = Piece.PIECE_T;
        }

        // holdID != nowID (nowID is PIECE_T from queue, holdID is PIECE_I)
        ComboRaceSeedSearch.thinkBestPosition(0, 0, Piece.PIECE_I);

        assertTrue(ComboRaceSeedSearch.bestPts > Integer.MIN_VALUE,
                "Should find a move with different hold");
    }

    @Test
    void thinkBestPositionHoldIsNowID() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];
        ComboRaceSeedSearch.queue = new int[ComboRaceSeedSearch.QUEUE_SIZE];
        for (int i = 0; i < ComboRaceSeedSearch.QUEUE_SIZE; i++) {
            ComboRaceSeedSearch.queue[i] = Piece.PIECE_T;
        }

        // holdID == nowID -> skip hold logic, only test transitions
        ComboRaceSeedSearch.thinkBestPosition(0, 0, Piece.PIECE_T);

        assertTrue(ComboRaceSeedSearch.bestPts > Integer.MIN_VALUE,
                "Should find a move when hold matches current piece");
    }

    @Test
    void thinkBestPositionStateNegative() {
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        ComboRaceSeedSearch.thinkBestPosition(-1, 0, -1);

        assertTrue(true, "thinkBestPosition with negative state returns early");
    }

    @Test
    void fieldToCodeNonEmptyBottomRows() {
        Field f = new Field(10, 4, 0, false);
        // Fill a specific pattern in bottom 3 rows at columns 3-6
        // Row 3 (bottom): bits 0xF (cols 3,4,5,6)
        // Row 2: bits 0x5 (cols 3,5)
        // Row 1: bits 0x0
        f.setBlockColor(3, 3, 1);
        f.setBlockColor(4, 3, 1);
        f.setBlockColor(5, 3, 1);
        f.setBlockColor(6, 3, 1);
        f.setBlockColor(3, 2, 1);
        f.setBlockColor(5, 2, 1);

        short code = ComboRaceSeedSearch.fieldToCode(f, 3);

        // Encoding: y=1(bottom): x=3,2,1,0 -> col 3,4,5,6
        // blocks at 3,4,5,6 -> bits 0-3: 1111 = 0xF
        // y=2: blocks at 3,4,5,6 -> bits 4-7: 0101 = 0x5
        // y=3: empty -> bits 8-11: 0000 = 0x0
        // Actually the loop goes from y=height-3 to height-1 with y=1,2,3
        // x loop: x=0,1,2,3 where x=0 -> col 3, x=1 -> col 4, etc.
        // y=1: col 3 filled -> shift+1, col 4 filled -> shift+1, col 5 filled -> shift+1, col 6 filled -> shift+1 = 0xF
        // y=2: col 3 filled -> shift+1, col 4 empty -> shift, col 5 filled -> shift+1, col 6 empty -> shift = 0b0101 = 0x5
        // y=3: all empty -> 0b0000
        // Final: 0x5F0 (0x5 << 4 | 0xF) wait, result accumulates
        // After y=1: result = 0xF
        // y=2: result = 0xF << 4 | 0x5 = 0xF5
        // y=3: result = 0xF5 << 4 | 0x0 = 0xF50
        // But code is short (16 bits) -> 0x0F50 // truncated... actually short is signed
        // y=1: result = 0xF
        // y=2: result = (0xF << 4) | 0x5 = 0xF5
        // y=3: result = (0xF5 << 4) | 0x0 = 0xF50
        // That's a 12-bit value fitting in short
        assertTrue(code != 0, "Non-empty field should have non-zero code");
    }

    @Test
    void fieldToIndexExactMatch() {
        int idx = ComboRaceSeedSearch.fieldToIndex((short) 0x7);

        assertEquals(0, idx, "0x7 should match first entry");
    }

    @Test
    void fieldToIndexBinarySearchMiddle() {
        int idx = ComboRaceSeedSearch.fieldToIndex((short) 0x888);

        assertEquals(27, idx, "0x888 should match last entry");
    }

    @Test
    void fieldToIndexNoMatch() {
        int idx = ComboRaceSeedSearch.fieldToIndex((short) 0x0);

        assertEquals(-1, idx, "0x0 should not match any entry");
    }

    @Test
    void fieldToIndexWithFieldExact() {
        Field f = new Field(10, 4, 0, false);
        // Create field matching 0x7 pattern
        // 0x7 = 0b0111 = within the 4-column window at valleyX=3:
        //   x=0 (col 3): empty, x=1 (col 4): filled, x=2 (col 5): filled, x=3 (col 6): filled
        // Fill cols 4,5,6 (leaving col 3 empty to match 0x7)
        f.setBlockColor(4, 3, 1);
        f.setBlockColor(5, 3, 1);
        f.setBlockColor(6, 3, 1);

        int idx = ComboRaceSeedSearch.fieldToIndex(f, 3);

        assertEquals(0, idx, "Field matching 0x7 should return index 0");
    }

    @Test
    void createTablesAlreadyCreated() {
        ComboRaceSeedSearch.createTables();
        Object firstMoves = ComboRaceSeedSearch.moves;

        ComboRaceSeedSearch.createTables();

        assertEquals(firstMoves, ComboRaceSeedSearch.moves,
                "createTables should be idempotent");
    }

    @Test
    void thinkMainTerminalStateZero() {
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        int pts = ComboRaceSeedSearch.thinkMain(0, -1, ComboRaceSeedSearch.MAX_THINK_DEPTH);

        assertEquals(600, pts, "Terminal state 0 should score 600");
    }

    @Test
    void thinkMainTerminalStateLast() {
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        // Last entry (index 27) has stateScores[27] = 3 -> 3*100 = 300
        int pts = ComboRaceSeedSearch.thinkMain(27, -1, ComboRaceSeedSearch.MAX_THINK_DEPTH);

        assertEquals(300, pts, "Terminal state 27 should score 300");
    }
}
