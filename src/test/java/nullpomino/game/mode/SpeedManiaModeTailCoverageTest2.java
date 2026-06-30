package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.BooleanMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Tail-coverage for SpeedManiaMode uncovered lines:
 * 692-693 (big mode CO medal level 2: combo >= 3, medalCO < 2)
 * 703-704 (non-big mode CO medal level 2: combo >= 5, medalCO < 2)
 *
 * The level 3 CO medal branches (combo>=4 big, combo>=7 non-big) are already
 * covered by SpeedManiaModeCoverageBoostTest.
 */
class SpeedManiaModeTailCoverageTest2 {

    // -----------------------------------------------------------------------
    // Lines 692-693: big=true, combo >= 3, medalCO < 2 → medalCO becomes 2
    // -----------------------------------------------------------------------

    @Test
    void calcScoreBigModeCOMedalLevel2() throws Exception {
        SpeedManiaMode mode = new SpeedManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setBoolMenuValue(mode, "big", true);
        setInt(mode, "medalCO", 1); // < 2; combo=3 enters the branch
        engine.combo = 3;

        mode.calcScore(engine, 0, 1);

        assertEquals(2, readInt(mode, "medalCO"));
    }

    // -----------------------------------------------------------------------
    // Lines 703-704: big=false, combo >= 5, medalCO < 2 → medalCO becomes 2
    // -----------------------------------------------------------------------

    @Test
    void calcScoreNonBigModeCOMedalLevel2() throws Exception {
        SpeedManiaMode mode = new SpeedManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setBoolMenuValue(mode, "big", false);
        setInt(mode, "medalCO", 1); // < 2; combo=5 enters the branch
        engine.combo = 5;

        mode.calcScore(engine, 0, 1);

        assertEquals(2, readInt(mode, "medalCO"));
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(SpeedManiaMode mode) {
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
        ((BooleanMenuItem) f.get(o)).value = val;
    }

    private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) { /* try super */ }
        }
        throw new NoSuchFieldException(n);
    }
}
