package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Tail-coverage for SpeedMania2Mode uncovered lines:
 * 768-769  (big mode CO medal level 2: combo >= 3)
 * 779-780  (non-big mode CO medal level 2: combo >= 5)
 * 819-820  (ending at level 1300 with regret: sectionlasttime > tableTimeRegret)
 * 856-857-858 (torikan path grade++ when no regret: sectionlasttime <= tableTimeRegret)
 */
class SpeedMania2ModeTailCoverageTest {

    // -----------------------------------------------------------------------
    // Lines 768-769: big=true, combo >= 3, medalCO < 2 → medalCO becomes 2
    // -----------------------------------------------------------------------

    @Test
    void calcScoreBigModeCOMedalLevel2() throws Exception {
        SpeedMania2Mode mode = new SpeedMania2Mode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setBoolean(mode, "big", true);
        setInt(mode, "medalCO", 1); // < 2; combo=3 triggers the branch
        engine.combo = 3;

        mode.calcScore(engine, 0, 1);

        assertEquals(2, readInt(mode, "medalCO"));
    }

    // -----------------------------------------------------------------------
    // Lines 779-780: big=false, combo >= 5, medalCO < 2 → medalCO becomes 2
    // -----------------------------------------------------------------------

    @Test
    void calcScoreNonBigModeCOMedalLevel2() throws Exception {
        SpeedMania2Mode mode = new SpeedMania2Mode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setBoolean(mode, "big", false);
        setInt(mode, "medalCO", 1); // < 2; combo=5 triggers the branch
        engine.combo = 5;

        mode.calcScore(engine, 0, 1);

        assertEquals(2, readInt(mode, "medalCO"));
    }

    // -----------------------------------------------------------------------
    // Lines 819-820: level reaches 1300 (ending), sectionlasttime > tableTimeRegret
    // → regretdispframe = 180 (regret displayed)
    // tableTimeRegret[12] = 3000; set sectiontime[12] = 3001 > 3000
    // -----------------------------------------------------------------------

    @Test
    void calcScoreEndingRegretAtLevel1300() throws Exception {
        SpeedMania2Mode mode = new SpeedMania2Mode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        // level 1299 + 1 line = 1300 → endings path
        engine.statistics.level = 1299;
        setInt(mode, "nextseclv", 1300);
        // sectiontime[12] = 3001 > tableTimeRegret[12] = 3000 → regret
        int[] sectiontime = (int[]) readField(mode, "sectiontime");
        sectiontime[12] = 3001;

        mode.calcScore(engine, 0, 1);

        assertEquals(180, readInt(mode, "regretdispframe"));
    }

    // -----------------------------------------------------------------------
    // Lines 856-858: torikan path (nextseclv=500, level>=500, time>torikan),
    // sectionlasttime <= tableTimeRegret → grade++, gradeflash=180
    // tableTimeRegret[4] = 3000; set sectiontime[4] = 2999 <= 3000 → grade up
    // -----------------------------------------------------------------------

    @Test
    void calcScoreTorikanPathGradeUp() throws Exception {
        SpeedMania2Mode mode = new SpeedMania2Mode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        // Set up for torikan at nextseclv=500
        setInt(mode, "nextseclv", 500);
        setInt(mode, "torikan", 100); // > 0
        engine.statistics.time = 200; // > torikan=100 so torikan triggers
        // level=499+1=500 >= nextseclv=500 → torikan condition met
        engine.statistics.level = 499;

        // sectiontime[4] = 2999 <= tableTimeRegret[4] = 3000 → no regret → grade++
        int[] sectiontime = (int[]) readField(mode, "sectiontime");
        sectiontime[4] = 2999;

        int gradeBefore = readInt(mode, "grade");
        mode.calcScore(engine, 0, 1);
        int gradeAfter = readInt(mode, "grade");

        assertEquals(gradeBefore + 1, gradeAfter);
        assertEquals(180, readInt(mode, "gradeflash"));
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(SpeedMania2Mode mode) {
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

    private static void setBoolean(Object o, String n, boolean v) throws Exception {
        Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        f.setBoolean(o, v);
    }

    private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) { /* try super */ }
        }
        throw new NoSuchFieldException(n);
    }
}
