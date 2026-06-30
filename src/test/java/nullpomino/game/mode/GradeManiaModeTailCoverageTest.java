package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.OnOffMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Tail-coverage for GradeManiaMode uncovered lines:
 * 633 (calcScore version=0 gm500=true branch)
 * 649 (calcScore levelstop SE)
 * 759-761 (onResult UP button page navigation)
 *
 * Phantom / dead:
 * None identified in this batch.
 */
class GradeManiaModeTailCoverageTest {

    // -----------------------------------------------------------------------
    // Line 633: gm500 = true in version=0 branch when nextseclv==500,
    // grade >= GM_500_GRADE_REQUIRE, time <= GM_500_TIME_REQUIRE_V0
    // -----------------------------------------------------------------------

    @Test
    void calcScoreVersion0Gm500SetTrue() throws Exception {
        GradeManiaMode mode = new GradeManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        // version=0 → else-branch; nextseclv=500 triggers gm500 check
        setInt(mode, "version", 0);
        // grade >= GM_500_GRADE_REQUIRE (value = 10 from constants, safe to use high value)
        setInt(mode, "grade", 18);
        // time <= GM_500_TIME_REQUIRE_V0 = 25200
        engine.statistics.time = 20000;
        engine.statistics.level = 450;
        setInt(mode, "nextseclv", 500);

        mode.calcScore(engine, 0, 50);

        assertTrue(readBoolean(mode, "gm500"));
    }

    // -----------------------------------------------------------------------
    // Line 649: levelstop SE when level == nextseclv - 1 and lvstopse.value==true
    // -----------------------------------------------------------------------

    @Test
    void calcScoreLevelStopSE() throws Exception {
        GradeManiaMode mode = new GradeManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setInt(mode, "nextseclv", 200);
        engine.statistics.level = 198; // +1 line -> nextseclv - 1 = 199
        // lvstopse is an OnOffMenuItem with public field 'value' from AbstractMenuItem
        ((OnOffMenuItem) readField(mode, "lvstopse")).value = true;

        // lines>=1 is required to enter the level-stop block; +1 lands at 199.
        mode.calcScore(engine, 0, 1);

        // SE was played (no-op receiver); level stays at 199
        assertEquals(199, engine.statistics.level);
    }

    // -----------------------------------------------------------------------
    // Lines 759-761: onResult UP button decrements page and wraps at 0 → 2
    // -----------------------------------------------------------------------

    @Test
    void onResultUpButtonWrapsPageToTwo() throws Exception {
        GradeManiaMode mode = new GradeManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.statc[1] = 0; // will wrap to 2

        // isMenuRepeatKey returns true when buttonTime[key] == 1
        engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;

        mode.onResult(engine, 0);

        assertEquals(2, engine.statc[1]);
    }

    @Test
    void onResultUpButtonDecrementsPage() throws Exception {
        GradeManiaMode mode = new GradeManiaMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.statc[1] = 2; // decrement to 1

        engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;

        mode.onResult(engine, 0);

        assertEquals(1, engine.statc[1]);
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(GradeManiaMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[0].ruleopt.fieldWidth = 10;
        manager.engine[0].ruleopt.fieldHeight = 20;
        manager.engine[0].playerID = 0;
        return manager.engine[0];
    }

    private static boolean readBoolean(Object o, String n) throws Exception {
        Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        return f.getBoolean(o);
    }

    private static Object readField(Object o, String n) throws Exception {
        Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        return f.get(o);
    }

    private static void setInt(Object o, String n, int v) throws Exception {
        Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        f.setInt(o, v);
    }

    private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) { /* try super */ }
        }
        throw new NoSuchFieldException(n);
    }
}
