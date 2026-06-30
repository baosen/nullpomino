package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers uncovered lines in {@link MarathonPlusMode}:
 * <ul>
 *   <li>Line 512: T-Spin single no-B2B branch (pts += 800)</li>
 *   <li>Lines 549-550: normal double (pts += 300, lastevent = EVENT_DOUBLE)</li>
 *   <li>Lines 552-553: normal triple (pts += 500, lastevent = EVENT_TRIPLE)</li>
 * </ul>
 */
class MarathonPlusModeBranchCoverageTest2 {

    // -----------------------------------------------------------------------
    // Line 512: T-Spin single, not mini, B2B == false
    // -----------------------------------------------------------------------

    @Test
    void calcScoreTSpinSingleNoB2B() throws Exception {
        MarathonPlusMode mode = new MarathonPlusMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.createFieldIfNeeded();
        // Non-empty field to avoid all-clear bonus
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));
        engine.nowPieceObject = new Piece(Piece.PIECE_T);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = false;
        engine.tspinez = false;
        engine.b2b = false; // <- the no-B2B branch at line 512

        mode.calcScore(engine, 0, 1);

        // T-Spin single no-B2B = 800 * (level+1) = 800
        assertEquals(800, engine.statistics.score);
        assertEquals(readInt(mode, "EVENT_TSPIN_SINGLE"), readInt(mode, "lastevent"));
    }

    // -----------------------------------------------------------------------
    // Lines 549-550: normal double (engine.tspin == false, lines == 2)
    // -----------------------------------------------------------------------

    @Test
    void calcScoreNormalDouble() throws Exception {
        MarathonPlusMode mode = new MarathonPlusMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.createFieldIfNeeded();
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));
        engine.nowPieceObject = new Piece(Piece.PIECE_T);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = false;
        engine.tspinez = false;

        mode.calcScore(engine, 0, 2);

        // Normal double = 300 * (0+1) = 300
        assertEquals(300, engine.statistics.score);
        assertEquals(readInt(mode, "EVENT_DOUBLE"), readInt(mode, "lastevent"));
    }

    // -----------------------------------------------------------------------
    // Lines 552-553: normal triple (engine.tspin == false, lines == 3)
    // -----------------------------------------------------------------------

    @Test
    void calcScoreNormalTriple() throws Exception {
        MarathonPlusMode mode = new MarathonPlusMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.createFieldIfNeeded();
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));
        engine.nowPieceObject = new Piece(Piece.PIECE_T);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = false;
        engine.tspinez = false;

        mode.calcScore(engine, 0, 3);

        // Normal triple = 500 * (0+1) = 500
        assertEquals(500, engine.statistics.score);
        assertEquals(readInt(mode, "EVENT_TRIPLE"), readInt(mode, "lastevent"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(MarathonPlusMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        return manager.engine[0];
    }

    private static int readInt(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        return f.getInt(obj);
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
