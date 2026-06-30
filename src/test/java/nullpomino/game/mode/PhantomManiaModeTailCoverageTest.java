package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.OnOffMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Tail-coverage for PhantomManiaMode uncovered lines:
 * 679-684 (big-mode CO medal level 2 and level 3 branches)
 * 691-692 (non-big CO medal level 2: combo >= 5)
 * 694-695 (non-big CO medal level 3: combo >= 7)
 *
 * Strategy: call calcScore with the right combo value and starting medalCO
 * to verify the exact post-state (medalCO becomes 2 or 3).
 */
class PhantomManiaModeTailCoverageTest {

    // -----------------------------------------------------------------------
    // Lines 679-681: big mode, combo >= 3, medalCO < 2 → medalCO becomes 2
    // -----------------------------------------------------------------------

    @Test
    void calcScoreBigModeCOMedalLevel2() throws Exception {
        PhantomManiaMode mode = new PhantomManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setBoolMenuValue(mode, "big", true);
        setInt(mode, "medalCO", 1); // < 2, combo=3 satisfies the branch
        engine.combo = 3;

        mode.calcScore(engine, 0, 1);

        assertEquals(2, readInt(mode, "medalCO"));
    }

    // -----------------------------------------------------------------------
    // Lines 682-684: big mode, combo >= 4, medalCO < 3 → medalCO becomes 3
    // -----------------------------------------------------------------------

    @Test
    void calcScoreBigModeCOMedalLevel3() throws Exception {
        PhantomManiaMode mode = new PhantomManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setBoolMenuValue(mode, "big", true);
        setInt(mode, "medalCO", 2); // < 3, combo=4 satisfies the branch
        engine.combo = 4;

        mode.calcScore(engine, 0, 1);

        assertEquals(3, readInt(mode, "medalCO"));
    }

    // -----------------------------------------------------------------------
    // Lines 691-692: non-big mode, combo >= 5, medalCO < 2 → medalCO becomes 2
    // -----------------------------------------------------------------------

    @Test
    void calcScoreNonBigModeCOMedalLevel2() throws Exception {
        PhantomManiaMode mode = new PhantomManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setBoolMenuValue(mode, "big", false);
        setInt(mode, "medalCO", 1); // < 2, combo=5 satisfies the branch
        engine.combo = 5;

        mode.calcScore(engine, 0, 1);

        assertEquals(2, readInt(mode, "medalCO"));
    }

    // -----------------------------------------------------------------------
    // Lines 694-695: non-big mode, combo >= 7, medalCO < 3 → medalCO becomes 3
    // -----------------------------------------------------------------------

    @Test
    void calcScoreNonBigModeCOMedalLevel3() throws Exception {
        PhantomManiaMode mode = new PhantomManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setBoolMenuValue(mode, "big", false);
        setInt(mode, "medalCO", 2); // < 3, combo=7 satisfies the branch
        engine.combo = 7;

        mode.calcScore(engine, 0, 1);

        assertEquals(3, readInt(mode, "medalCO"));
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(PhantomManiaMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[0].ruleopt.fieldWidth = 10;
        manager.engine[0].ruleopt.fieldHeight = 20;
        manager.engine[0].playerID = 0;
        return manager.engine[0];
    }

    private static int readInt(Object o, String n) throws Exception {
        Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        return f.getInt(o);
    }

    private static void setInt(Object o, String n, int v) throws Exception {
        Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        f.setInt(o, v);
    }

    private static void setBoolMenuValue(Object o, String fieldName, boolean val) throws Exception {
        Field f = findField(o.getClass(), fieldName);
        f.setAccessible(true);
        ((OnOffMenuItem) f.get(o)).value = val;
    }

    private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) { /* try super */ }
        }
        throw new NoSuchFieldException(n);
    }
}
