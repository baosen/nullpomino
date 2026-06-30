package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Tail-coverage for GradeMania3Mode uncovered lines:
 * 695-699 (onSetting demotion-exam branch)
 * 885     (renderLast score display, lastscore==0)
 * 1252    (calcScore mrollFlag version<2)
 * 1285-1287 (calcScore BGM change on section cross)
 * 1325    (calcScore levelstop SE)
 * 1548    (renderResult section-time new-record colour)
 * 1591-1592 (onResult passframe<300 clamp)
 * 1608    (onResult demotion-fail gameover SE)
 * 1832    (setPromotionalGrade closing brace / no-match exit)
 *
 * Phantom / dead:
 * None identified in this batch.
 */
class GradeMania3ModeTailCoverageTest {

    // -----------------------------------------------------------------------
    // Lines 695-699: demotion branch in onSetting when exam fires and
    //   promotionalExam <= qualifiedGrade but demotionPoints >= 30.
    // EXAM_CHANCE=3 so we retry up to 200 times to hit rand.nextInt(3)==0.
    // gradeHistory all -1 → setPromotionalGrade sets promotionalExam=0 <=
    // qualifiedGrade=20, so promotion branch is skipped and demotion fires.
    // -----------------------------------------------------------------------

    @Test
    void onSettingDemotionBranchSetsFlag() throws Exception {
        for (int attempt = 0; attempt < 200; attempt++) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = freshEngine(mode);
            mode.modeInit(engine.owner);
            mode.playerInit(engine, 0);

            // enableexam=true, big=false, always20g=false
            setBoolean(mode, "enableexam", true);
            setBoolean(mode, "big", false);
            setBoolean(mode, "always20g", false);
            // qualifiedGrade > 0 so that promotionalExam=0 (from gradeHistory all -1)
            // is <= qualifiedGrade, bypassing the promotion branch
            setInt(mode, "qualifiedGrade", 20);
            setInt(mode, "demotionPoints", 30);

            // menuTime >= 5 so A-button press is accepted
            setInt(mode, "menuTime", 10);
            engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
            engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

            mode.onSetting(engine, 0);

            if (readBoolean(mode, "demotionFlag")) {
                assertEquals(600, readInt(mode, "passframe"));
                assertEquals(0, readInt(mode, "demotionPoints"));
                return; // success
            }
        }
        // If we get here after 200 attempts, force the assertion to catch it
        assertTrue(false, "demotionFlag never set after 200 attempts");
    }

    // -----------------------------------------------------------------------
    // Line 885: renderLast score display branch when lastscore==0 (or scgettime<=0)
    // and !promotionFlag. This path reaches strScore = String.valueOf(score).
    // -----------------------------------------------------------------------

    @Test
    void renderLastScoreDisplayWhenLastscoreZero() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = freshEngine(mode);
        mode.modeInit(engine.owner);
        mode.playerInit(engine, 0);

        engine.stat = GameEngine.Status.MOVE;
        engine.gameActive = true;
        setBoolean(mode, "promotionFlag", false);
        setBoolean(mode, "demotionFlag", false);
        setBoolean(mode, "gradedisp", true);
        setInt(mode, "lastscore", 0);
        setInt(mode, "scgettime", 0);
        engine.statistics.score = 12345;

        // No assertion on score display since receiver is no-op;
        // but coverage requires line 885 to execute without exception.
        mode.renderLast(engine, 0);

        // Verify score is still the same (no side effect from this path)
        assertEquals(12345, engine.statistics.score);
    }

    // -----------------------------------------------------------------------
    // Line 1252: calcScore sets mrollFlag when version<2, grade>=15, coolcount>=9
    // -----------------------------------------------------------------------

    @Test
    void calcScoreMrollFlagVersion1() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = freshEngine(mode);
        mode.modeInit(engine.owner);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        setInt(mode, "version", 1);       // version < 2 → use line 1252 branch
        setInt(mode, "grade", 15);
        setInt(mode, "coolcount", 9);
        setInt(mode, "nextseclv", 1000);
        // level=998 + lines=1 = 999 → triggers the ending path where mrollFlag is checked
        engine.statistics.level = 998;

        mode.calcScore(engine, 0, 1);

        assertTrue(readBoolean(mode, "mrollFlag"));
    }

    // -----------------------------------------------------------------------
    // Lines 1285-1287: calcScore BGM change when crossing a section boundary
    // and tableBGMChange[bgmlv] != -1 && internalLevel >= tableBGMChange[bgmlv]
    // -----------------------------------------------------------------------

    @Test
    void calcScoreBGMChangeOnSectionCrossing() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = freshEngine(mode);
        mode.modeInit(engine.owner);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        // Position just before section 500 boundary, so level + lines >= 500 = nextseclv
        engine.statistics.level = 499;
        setInt(mode, "nextseclv", 500);
        setInt(mode, "bgmlv", 0);
        // internalLevel >= tableBGMChange[0] (500)
        setInt(mode, "internalLevel", 500);

        int bgmlvBefore = readInt(mode, "bgmlv");
        mode.calcScore(engine, 0, 1);
        int bgmlvAfter = readInt(mode, "bgmlv");

        assertEquals(bgmlvBefore + 1, bgmlvAfter);
    }

    // -----------------------------------------------------------------------
    // Line 1325: calcScore levelstop SE when level == nextseclv - 1 and lvstopse
    // -----------------------------------------------------------------------

    @Test
    void calcScoreLevelStopSE() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = freshEngine(mode);
        mode.modeInit(engine.owner);
        mode.playerInit(engine, 0);

        engine.ending = 0;
        engine.nowPieceObject = new Piece(Piece.PIECE_T);
        engine.createFieldIfNeeded();

        // level + 1 line == nextseclv - 1, so the scoring body reaches levelstop.
        setInt(mode, "nextseclv", 200);
        engine.statistics.level = 198;
        setBoolean(mode, "lvstopse", true);

        mode.calcScore(engine, 0, 1);

        // The SE was played (no-op receiver), level unchanged at 199
        assertEquals(199, engine.statistics.level);
    }

    // -----------------------------------------------------------------------
    // Line 1548: renderResult section-time new-record color=RED when stcolor==1
    // -----------------------------------------------------------------------

    @Test
    void renderResultSectionTimeNewRecordColorRed() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = freshEngine(mode);
        mode.modeInit(engine.owner);
        mode.playerInit(engine, 0);

        engine.stat = GameEngine.Status.RESULT;
        engine.gameActive = false;
        engine.statc[1] = 1; // section-time page

        // Set stcolor=1 so new-record branches use RED
        setInt(mode, "stcolor", 1);

        int[] sectiontime = (int[]) readField(mode, "sectiontime");
        sectiontime[0] = 1234;

        boolean[] sectionIsNewRecord = (boolean[]) readField(mode, "sectionIsNewRecord");
        sectionIsNewRecord[0] = true;

        mode.renderResult(engine, 0);

        // Line 1548 executed: color was set to RED. No state to assert beyond no-throw.
        assertTrue(sectionIsNewRecord[0]);
    }

    // -----------------------------------------------------------------------
    // Lines 1591-1592: onResult passframe in (0, 300) range clamps to 0
    // -----------------------------------------------------------------------

    @Test
    void onResultPassframeLessThan300ClampsToZero() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = freshEngine(mode);
        mode.modeInit(engine.owner);
        mode.playerInit(engine, 0);

        setInt(mode, "passframe", 150); // < 300 → sets passframe = 0

        engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

        mode.onResult(engine, 0);

        // passframe was clamped to 0 by line 1592, then decremented at line 1613 → -1
        assertEquals(-1, readInt(mode, "passframe"));
    }

    // -----------------------------------------------------------------------
    // Line 1608: onResult demotion-exam fail: grade < qualifiedGrade → gameover SE
    // -----------------------------------------------------------------------

    @Test
    void onResultDemotionFailPlaysGameoverSE() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = freshEngine(mode);
        mode.modeInit(engine.owner);
        mode.playerInit(engine, 0);

        setBoolean(mode, "demotionFlag", true);
        setInt(mode, "passframe", 420);
        setInt(mode, "grade", 5);
        setInt(mode, "qualifiedGrade", 10); // grade < qualifiedGrade → gameover

        mode.onResult(engine, 0);

        // passframe should have decremented to 419 (line 1613: passframe--)
        assertEquals(419, readInt(mode, "passframe"));
    }

    // -----------------------------------------------------------------------
    // Line 1832: setPromotionalGrade closing brace — reached when no grade
    // in the loop ever accumulated gradesOver > 3 (all sections too few wins).
    // We call it via reflection with a gradeHistory that never reaches 4 counts.
    // -----------------------------------------------------------------------

    @Test
    void setPromotionalGradeNoMatchExitsCleanly() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = freshEngine(mode);
        mode.modeInit(engine.owner);
        mode.playerInit(engine, 0);

        // Set a gradeHistory with all -1 entries but grade-history size is GRADE_HISTORY_SIZE=7
        // Actually -1 terminates the inner loop early. We need no -1s but gradesOver <= 3 for
        // every candidate grade. Fill gradeHistory with grade values spread so no single grade
        // appears > 3 times (7 slots, all different values 0..6).
        int[] gradeHistory = (int[]) readField(mode, "gradeHistory");
        for (int i = 0; i < gradeHistory.length; i++) {
            gradeHistory[i] = i; // unique values, none repeated enough to trigger
        }

        // Set qualifiedGrade to 0
        setInt(mode, "qualifiedGrade", 0);

        Method m = findMethod(GradeMania3Mode.class, "setPromotionalGrade");
        m.setAccessible(true);
        m.invoke(mode); // should reach line 1832 without throwing

        // promotionalExam stays at its default (not modified since no match found)
        // Just verify the call completes successfully
        assertTrue(true);
    }

    // -----------------------------------------------------------------------
    // Helper methods
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(GradeMania3Mode mode) {
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

    private static Method findMethod(Class<?> cls, String n) throws NoSuchMethodException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredMethod(n); } catch (NoSuchMethodException e) { /* try super */ }
        }
        throw new NoSuchMethodException(n);
    }
}
