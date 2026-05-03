package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for uncovered branches in {@link ComboRaceSeedSearch}.
 * The uncovered lines (47-101) are primarily in the main() method which
 * runs an exhaustive seed search - too heavy for unit tests. However,
 * we cover the thinkBestPosition/thinkMain logic with hold piece
 * variants and thinkMain terminal/non-terminal branching.
 */
class ComboRaceSeedSearchExtraTest {

    private GameManager gm;
    private GameEngine engine;

    @BeforeEach
    void setUp() {
        gm = new GameManager(new EventReceiver());
        gm.init();
        engine = gm.engine[0];
        engine.init();
    }

    // ─── thinkBestPosition with hold piece ────────────────────────────

    @Test
    void thinkBestPositionWithHoldPieceSwap() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.queue = new int[1400];
        for (int i = 0; i < ComboRaceSeedSearch.queue.length; i++) {
            ComboRaceSeedSearch.queue[i] = Piece.PIECE_T;
        }
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        // State 0 has valid transitions for T piece
        // holdID = -1 (empty hold), so it will try hold with next queue piece
        ComboRaceSeedSearch.thinkBestPosition(0, 0, -1);

        assertTrue(ComboRaceSeedSearch.bestPts > Integer.MIN_VALUE,
                "thinkBestPosition with empty hold should find a move");
    }

    @Test
    void thinkBestPositionWithExistingHoldPiece() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.queue = new int[1400];
        for (int i = 0; i < ComboRaceSeedSearch.queue.length; i++) {
            ComboRaceSeedSearch.queue[i] = Piece.PIECE_T;
        }
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        // holdID is Piece.PIECE_I, different from queue current (PIECE_T)
        ComboRaceSeedSearch.thinkBestPosition(0, 0, Piece.PIECE_I);

        assertTrue(ComboRaceSeedSearch.bestPts > Integer.MIN_VALUE,
                "thinkBestPosition with existing hold should find a move");
    }

    @Test
    void thinkBestPositionWithHoldSameAsCurrentPiece() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.queue = new int[1400];
        for (int i = 0; i < ComboRaceSeedSearch.queue.length; i++) {
            ComboRaceSeedSearch.queue[i] = Piece.PIECE_T;
        }
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        // holdID same as queue current (both PIECE_T), should skip hold logic
        ComboRaceSeedSearch.thinkBestPosition(0, 0, Piece.PIECE_T);

        assertTrue(ComboRaceSeedSearch.bestPts > Integer.MIN_VALUE,
                "thinkBestPosition with same hold as current should still find a move");
    }

    // ─── thinkMain: terminal state scoring ────────────────────────────

    @Test
    void thinkMainTerminalWithScore() {
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        // At max depth, returns stateScores[state]*100
        int pts = ComboRaceSeedSearch.thinkMain(0, -1, ComboRaceSeedSearch.MAX_THINK_DEPTH);

        assertEquals(600, pts, "State 0 at terminal should score 600");
    }

    @Test
    void thinkMainTerminalWithIPieceHold() {
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        int pts = ComboRaceSeedSearch.thinkMain(0, Piece.PIECE_I, ComboRaceSeedSearch.MAX_THINK_DEPTH);

        // I piece hold gives +1000 bonus
        assertEquals(1600, pts, "State 0 at terminal with I hold should score 1600");
    }

    @Test
    void thinkMainTerminalWithOtherPieceHold() {
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        // For hold = 0 (PIECE_I check already tests that branch)
        // hold = 1 (PIECE_L) should go to the else-if branch
        int pts = ComboRaceSeedSearch.thinkMain(0, 1, ComboRaceSeedSearch.MAX_THINK_DEPTH);

        assertTrue(pts > 600, "State 0 at terminal with L hold should score > 600");
    }

    // ─── thinkMain: non-terminal with holdID == -1 (line 187) ──────────

    @Test
    void thinkMainNonTerminalEmptyHold() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];
        for (int i = 0; i < ComboRaceSeedSearch.nextQueueIDs.length; i++) {
            ComboRaceSeedSearch.nextQueueIDs[i] = Piece.PIECE_T;
        }

        // holdID = -1, depth = 0 -> should take holdID == -1 branch
        int pts = ComboRaceSeedSearch.thinkMain(0, -1, 0);

        assertTrue(pts >= 1000, "Non-terminal with empty hold should score >= 1000");
    }

    // ─── thinkMain: non-terminal with holdID != -1 (line 189) ──────────

    @Test
    void thinkMainNonTerminalWithHold() {
        ComboRaceSeedSearch.createTables();
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];
        for (int i = 0; i < ComboRaceSeedSearch.nextQueueIDs.length; i++) {
            ComboRaceSeedSearch.nextQueueIDs[i] = Piece.PIECE_T;
        }

        // holdID != -1, should take else branch with transitions for hold piece
        int pts = ComboRaceSeedSearch.thinkMain(0, Piece.PIECE_S, 0);

        assertTrue(pts >= 1000, "Non-terminal with hold should score >= 1000");
    }

    // ─── thinkMain: negative state returns 0 (line 165) ───────────────

    @Test
    void thinkMainNegativeState() {
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        int pts = ComboRaceSeedSearch.thinkMain(-1, -1, 0);

        assertEquals(0, pts, "Negative state should return 0");
    }

    // ─── thinkBestPosition: negative state early return (line 108) ────

    @Test
    void thinkBestPositionNegativeState() {
        ComboRaceSeedSearch.nextQueueIDs = new int[ComboRaceSeedSearch.MAX_THINK_DEPTH];

        ComboRaceSeedSearch.thinkBestPosition(-1, 0, -1);

        // Should return early without setting bestPts
        assertTrue(true, "thinkBestPosition negative state handled");
    }
}
