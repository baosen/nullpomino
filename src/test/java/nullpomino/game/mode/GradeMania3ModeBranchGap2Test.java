package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap coverage for GradeMania3Mode, part 2:
 * playerInit replay exam branches, setAverageSectionTime bounds, stMedalCheck edges,
 * checkCool previous-cool chain, calcScore (grade points/medals/torikan/mroll/roll points),
 * saveReplay guard directions, checkRanking comparison chain,
 * setPromotionalGrade exits, updateBestSectionTime exam type.
 */
class GradeMania3ModeBranchGap2Test {

    /** EventReceiver that never touches the filesystem. */
    static class QuietReceiver extends EventReceiver {
        @Override public void saveModeConfig(CustomProperties modeConfig) { /* hermetic no-op */ }
    }

    // -------------------------------------------------------------------
    // playerInit (replay exam data)
    // -------------------------------------------------------------------

    // L365: replay with exam enabled, no promotion, demotionPoints >= 30 / < 30
    @Test
    void playerInitReplayDemotionBranchBothDirections() throws Exception {
        for (int points : new int[] {30, 0}) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameManager manager = new GameManager(new QuietReceiver());
            manager.mode = mode;
            manager.init();
            manager.engine[0].init();
            manager.replayMode = true;
            manager.replayProp = new CustomProperties();
            manager.replayProp.setProperty("grademania3.enableexam", true);
            manager.replayProp.setProperty("grademania3.exam", 0);
            manager.replayProp.setProperty("grademania3.demopoint", points);
            mode.playerInit(manager.engine[0], 0);
            assertEquals(points >= 30, readBoolean(mode, "demotionFlag"));
            assertFalse(readBoolean(mode, "promotionFlag"));
            manager.replayMode = false;
        }
    }

    // -------------------------------------------------------------------
    // setAverageSectionTime
    // -------------------------------------------------------------------

    // L464: index runs past the end of sectiontime, and below zero
    @Test
    void setAverageSectionTimeIndexBounds() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        fresh(mode);
        int[] times = (int[]) readField(mode, "sectiontime");
        Arrays.fill(times, 100);
        Method m = findMethod(GradeMania3Mode.class, "setAverageSectionTime");

        setInt(mode, "startlevel", 9);
        setInt(mode, "sectionscomp", 3); // i = 9, 10, 11 -> 10 and 11 out of range
        m.invoke(mode);
        assertEquals(100 / 3, readInt(mode, "sectionavgtime"));

        setInt(mode, "startlevel", -2);
        setInt(mode, "sectionscomp", 1); // i = -2 -> i >= 0 false
        m.invoke(mode);
        assertEquals(0, readInt(mode, "sectionavgtime"));
    }

    // -------------------------------------------------------------------
    // stMedalCheck
    // -------------------------------------------------------------------

    // L483 false (medalST already 3), L487 false (replay mode), L490 second-op false, L493 first-op false
    @Test
    void stMedalCheckEdgeDirections() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setBoolean(mode, "enableexam", false); // type 0 column regardless of shared config
        int[][] best = (int[][]) readField(mode, "bestSectionTime");
        best[0][0] = 1000;
        boolean[] rec = (boolean[]) readField(mode, "sectionIsNewRecord");

        // new record but medalST already 3 (L483 false); replayMode true (L487 false)
        engine.owner.replayMode = true;
        setInt(mode, "medalST", 3);
        setInt(mode, "sectionlasttime", 500);
        mode.stMedalCheck(engine, 0);
        assertFalse(rec[0]);
        assertEquals(3, readInt(mode, "medalST"));
        engine.owner.replayMode = false;

        // within best+300 but medalST already 2 (L490 second operand false)
        setInt(mode, "medalST", 2);
        setInt(mode, "sectionlasttime", 1100);
        mode.stMedalCheck(engine, 0);
        assertEquals(2, readInt(mode, "medalST"));

        // beyond best+600 (L493 first operand false)
        setInt(mode, "medalST", 0);
        setInt(mode, "sectionlasttime", 1700);
        mode.stMedalCheck(engine, 0);
        assertEquals(0, readInt(mode, "medalST"));
    }

    // -------------------------------------------------------------------
    // checkCool
    // -------------------------------------------------------------------

    // L508/509: previouscool chain (pass within +120, fail beyond +120), slow section, display-suppressed
    @Test
    void checkCoolPreviousCoolChain() throws Exception {
        Method m = findMethod(GradeMania3Mode.class, "checkCool", GameEngine.class);

        // previouscool, within coolprevtime + 120 -> cool
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.statistics.level = 170;
        int[] times = (int[]) readField(mode, "sectiontime");
        times[1] = 2000;
        setBoolean(mode, "previouscool", true);
        setInt(mode, "coolprevtime", 1900);
        m.invoke(mode, engine);
        assertTrue(readBoolean(mode, "cool"));

        // previouscool, slower than coolprevtime + 120 -> no cool
        GradeMania3Mode mode2 = new GradeMania3Mode();
        GameEngine engine2 = fresh(mode2);
        engine2.statistics.level = 170;
        int[] times2 = (int[]) readField(mode2, "sectiontime");
        times2[1] = 2000;
        setBoolean(mode2, "previouscool", true);
        setInt(mode2, "coolprevtime", 1000);
        m.invoke(mode2, engine2);
        assertFalse(readBoolean(mode2, "cool"));

        // section time over the COOL table -> first operand false
        GradeMania3Mode mode3 = new GradeMania3Mode();
        GameEngine engine3 = fresh(mode3);
        engine3.statistics.level = 170;
        int[] times3 = (int[]) readField(mode3, "sectiontime");
        times3[1] = 4000; // > tableTimeCool[1] = 3120
        m.invoke(mode3, engine3);
        assertFalse(readBoolean(mode3, "cool"));

        // L522: level%100 >= 82 with cool but already displayed
        GradeMania3Mode mode4 = new GradeMania3Mode();
        GameEngine engine4 = fresh(mode4);
        engine4.statistics.level = 182;
        setBoolean(mode4, "cool", true);
        setBoolean(mode4, "coolchecked", true);
        setBoolean(mode4, "cooldisplayed", true);
        m.invoke(mode4, engine4);
        assertEquals(0, readInt(mode4, "cooldispframe"));
    }

    // -------------------------------------------------------------------
    // calcScore: grade points and medals
    // -------------------------------------------------------------------

    // L1114: lines >= 1 with ending == 1 does nothing
    @Test
    void calcScoreEndingOneIgnored() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        engine.ending = 1;
        mode.calcScore(engine, 0, 1);
        assertEquals(0, engine.statistics.score);
    }

    // L1122/1127: grade point index clamp and combo bonus index clamp
    @Test
    void calcScoreGradePointIndexClamps() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setInt(mode, "gradeBasicInternal", 15); // > 10 -> clamp
        engine.combo = 20;                      // indexcombo 19 -> clamp to table end
        mode.calcScore(engine, 0, 1);
        assertTrue(readInt(mode, "gradeBasicPoint") > 0);
    }

    // L1141: grade change table sentinel (-1) and threshold not reached
    @Test
    void calcScoreGradeChangeSentinelAndThreshold() throws Exception {
        // tableGradeChange[17] == -1: first operand false
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setInt(mode, "gradeBasicReal", 17);
        setInt(mode, "gradeBasicInternal", 31);
        setInt(mode, "gradeBasicPoint", 99);
        mode.calcScore(engine, 0, 1);
        assertEquals(17, readInt(mode, "gradeBasicReal"));

        // internal grade below the requirement: second operand false
        GradeMania3Mode mode2 = new GradeMania3Mode();
        GameEngine engine2 = scoringEngine(mode2);
        setInt(mode2, "gradeBasicReal", 5); // requires 7
        setInt(mode2, "gradeBasicInternal", 2);
        setInt(mode2, "gradeBasicPoint", 99);
        mode2.calcScore(engine2, 0, 1);
        assertEquals(5, readInt(mode2, "gradeBasicReal"));
        assertEquals(3, readInt(mode2, "gradeBasicInternal"));
    }

    // L1142 (gradeup SE with gradedisp) + L1145 (grade clamp at 31)
    @Test
    void calcScoreGradeUpWithDisplayAndClamp() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setBoolean(mode, "gradedisp", true);
        setInt(mode, "gradeBasicReal", 0);
        setInt(mode, "gradeBasicInternal", 0);
        setInt(mode, "gradeBasicPoint", 99);
        setInt(mode, "grade", 31);
        mode.calcScore(engine, 0, 1);
        assertEquals(1, readInt(mode, "gradeBasicReal"));
        assertEquals(31, readInt(mode, "grade")); // clamped
    }

    // L1157: big-mode SK medal counts 1/2/4 plus a non-matching count
    @Test
    void calcScoreBigSkMedalCounts() throws Exception {
        for (int four : new int[] {1, 2, 4, 3}) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = scoringEngine(mode);
            setBoolean(mode, "big", true);
            engine.statistics.totalFour = four;
            mode.calcScore(engine, 0, 4);
            assertEquals(four == 3 ? 0 : 1, readInt(mode, "medalSK"));
        }
    }

    // L1162: normal-mode SK medal counts 20 and 35
    @Test
    void calcScoreNormalSkMedalCounts() throws Exception {
        for (int four : new int[] {20, 35}) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = scoringEngine(mode);
            engine.statistics.totalFour = four;
            mode.calcScore(engine, 0, 4);
            assertEquals(1, readInt(mode, "medalSK"));
        }
    }

    // L1173: bravo with medalAC already maxed
    @Test
    void calcScoreBravoMedalAlreadyMaxed() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setInt(mode, "medalAC", 3);
        mode.calcScore(engine, 0, 1); // empty field -> bravo
        assertEquals(3, readInt(mode, "medalAC"));
    }

    // L1187/L1198: CO medal already maxed in big and normal mode
    @Test
    void calcScoreComboMedalAlreadyMaxed() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setBoolean(mode, "big", true);
        setInt(mode, "medalCO", 3);
        engine.combo = 4;
        mode.calcScore(engine, 0, 1);
        assertEquals(3, readInt(mode, "medalCO"));

        GradeMania3Mode mode2 = new GradeMania3Mode();
        GameEngine engine2 = scoringEngine(mode2);
        setInt(mode2, "medalCO", 3);
        engine2.combo = 7;
        mode2.calcScore(engine2, 0, 1);
        assertEquals(3, readInt(mode2, "medalCO"));
    }

    // -------------------------------------------------------------------
    // calcScore: level 999 ending, torikan, section cross
    // -------------------------------------------------------------------

    // L1238: version 2 mroll gate false directions (grade low; coolcount low)
    @Test
    void calcScoreMrollGateVersion2FalseDirections() throws Exception {
        for (int variant = 0; variant < 2; variant++) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = scoringEngine(mode);
            setInt(mode, "grade", variant == 0 ? 10 : 24);
            setInt(mode, "coolcount", variant == 0 ? 9 : 5);
            setInt(mode, "nextseclv", 1000);
            engine.statistics.level = 998;
            mode.calcScore(engine, 0, 1);
            assertFalse(readBoolean(mode, "mrollFlag"));
            assertEquals(1, engine.ending);
        }
    }

    // L1240: version < 2 mroll gate false directions
    @Test
    void calcScoreMrollGateVersion1FalseDirections() throws Exception {
        for (int variant = 0; variant < 2; variant++) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = scoringEngine(mode);
            setInt(mode, "version", 1);
            setInt(mode, "grade", variant == 0 ? 10 : 15);
            setInt(mode, "coolcount", variant == 0 ? 9 : 5);
            setInt(mode, "nextseclv", 1000);
            engine.statistics.level = 998;
            mode.calcScore(engine, 0, 1);
            assertFalse(readBoolean(mode, "mrollFlag"));
        }
    }

    // L1242/1243: torikan operand false directions
    @Test
    void calcScoreTorikanOperandDirections() throws Exception {
        // level stays below 500 at the 500 boundary
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setInt(mode, "nextseclv", 500);
        setInt(mode, "lv500torikan", 25200);
        engine.statistics.level = 498;
        engine.statistics.time = 999999;
        mode.calcScore(engine, 0, 1); // -> 499, torikan level check false
        assertEquals(499, engine.statistics.level);
        assertEquals(0, engine.ending);

        // torikan disabled (lv500torikan == 0)
        GradeMania3Mode mode2 = new GradeMania3Mode();
        GameEngine engine2 = scoringEngine(mode2);
        setInt(mode2, "nextseclv", 500);
        setInt(mode2, "lv500torikan", 0);
        engine2.statistics.level = 499;
        engine2.statistics.time = 999999;
        mode2.calcScore(engine2, 0, 1); // crosses into section advance instead
        assertEquals(0, engine2.ending);
        assertEquals(600, readInt(mode2, "nextseclv"));

        // promotion exam disables torikan
        GradeMania3Mode mode3 = new GradeMania3Mode();
        GameEngine engine3 = scoringEngine(mode3);
        setInt(mode3, "nextseclv", 500);
        setInt(mode3, "lv500torikan", 25200);
        setBoolean(mode3, "promotionFlag", true);
        engine3.statistics.level = 499;
        engine3.statistics.time = 999999;
        mode3.calcScore(engine3, 0, 1);
        assertEquals(0, engine3.ending);

        // demotion exam disables torikan
        GradeMania3Mode mode4 = new GradeMania3Mode();
        GameEngine engine4 = scoringEngine(mode4);
        setInt(mode4, "nextseclv", 500);
        setInt(mode4, "lv500torikan", 25200);
        setBoolean(mode4, "demotionFlag", true);
        engine4.statistics.level = 499;
        engine4.statistics.time = 999999;
        mode4.calcScore(engine4, 0, 1);
        assertEquals(0, engine4.ending);
    }

    // L1272: BGM change sentinel at section cross
    @Test
    void calcScoreSectionCrossBgmSentinel() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setInt(mode, "nextseclv", 400);
        setInt(mode, "bgmlv", 2); // tableBGMChange[2] == -1
        setInt(mode, "internalLevel", 800);
        engine.statistics.level = 398;
        mode.calcScore(engine, 0, 2);
        assertEquals(2, readInt(mode, "bgmlv"));
        assertEquals(500, readInt(mode, "nextseclv"));
    }

    // L1295 (grade clamp on COOL) + L1298 both gradedisp directions
    @Test
    void calcScoreCoolSectionGradeClampAndSound() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setInt(mode, "nextseclv", 400);
        setBoolean(mode, "cool", true);
        setBoolean(mode, "gradedisp", true);
        setInt(mode, "grade", 31);
        engine.statistics.level = 398;
        mode.calcScore(engine, 0, 2);
        assertEquals(31, readInt(mode, "grade")); // clamped
        assertTrue(readBoolean(mode, "previouscool"));

        GradeMania3Mode mode2 = new GradeMania3Mode();
        GameEngine engine2 = scoringEngine(mode2);
        setInt(mode2, "nextseclv", 400);
        setBoolean(mode2, "cool", true);
        setBoolean(mode2, "gradedisp", false);
        setInt(mode2, "grade", 5);
        engine2.statistics.level = 398;
        mode2.calcScore(engine2, 0, 2);
        assertEquals(6, readInt(mode2, "grade"));
    }

    // L1311: nextseclv clamps to 999 when crossing section 900
    @Test
    void calcScoreNextSectionClampAt999() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setInt(mode, "nextseclv", 900);
        engine.statistics.level = 898;
        mode.calcScore(engine, 0, 2);
        assertEquals(999, readInt(mode, "nextseclv"));
    }

    // L1312: level stops at nextseclv-1 with lvstopse false
    @Test
    void calcScoreLevelStopWithSoundOff() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        setInt(mode, "nextseclv", 200);
        setBoolean(mode, "lvstopse", false);
        engine.statistics.level = 197;
        mode.calcScore(engine, 0, 2);
        assertEquals(199, engine.statistics.level);
    }

    // L1326: negative speed bonus clamps to 0
    @Test
    void calcScoreNegativeSpeedBonusClamped() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = scoringEngine(mode);
        engine.statc[0] = 100; // > lock delay -> negative bonus
        mode.calcScore(engine, 0, 1);
        assertEquals(120, readInt(mode, "scgettime")); // addPlayScore ran with clamped bonus
    }

    // -------------------------------------------------------------------
    // calcScore: roll points
    // -------------------------------------------------------------------

    // L1339-1347: per-line roll points in both roll types
    @Test
    void calcScoreRollPointsPerLineCounts() throws Exception {
        float[][] expected = {
            {0.04f, 0.08f, 0.12f, 0.26f}, // normal roll
            {0.1f, 0.2f, 0.3f, 1.0f},     // invisible (m) roll
        };
        for (int type = 0; type < 2; type++) {
            for (int lines = 1; lines <= 3; lines++) {
                GradeMania3Mode mode = new GradeMania3Mode();
                GameEngine engine = fresh(mode);
                engine.ending = 2;
                setBoolean(mode, "mrollFlag", type == 1);
                mode.calcScore(engine, 0, lines);
                assertEquals(expected[type][lines - 1], readFloat(mode, "rollPointsTotal"), 1e-6);
            }
        }
    }

    // L1352: roll point grade-up loop blocked at grade 31; L1356: gradeup SE during roll
    @Test
    void calcScoreRollGradeUpCapAndSound() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.ending = 2;
        setBoolean(mode, "mrollFlag", true);
        setInt(mode, "grade", 31);
        mode.calcScore(engine, 0, 4); // rollPoints 1.0 but grade already 31
        assertEquals(31, readInt(mode, "grade"));
        assertEquals(1.0f, readFloat(mode, "rollPoints"), 1e-6);

        GradeMania3Mode mode2 = new GradeMania3Mode();
        GameEngine engine2 = fresh(mode2);
        engine2.ending = 2;
        setBoolean(mode2, "mrollFlag", true);
        setBoolean(mode2, "gradedisp", true);
        setInt(mode2, "grade", 10);
        mode2.calcScore(engine2, 0, 4);
        assertEquals(11, readInt(mode2, "grade"));
    }

    // -------------------------------------------------------------------
    // saveReplay
    // -------------------------------------------------------------------

    // L1638: each ranking-update guard false in turn
    @Test
    void saveReplayGuardFalseDirections() throws Exception {
        for (int variant = 0; variant < 5; variant++) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = fresh(mode);
            engine.owner.replayProp = new CustomProperties();
            // baseline: all guards true, then falsify exactly one
            setInt(mode, "startlevel", 0);
            setBoolean(mode, "always20g", false);
            setBoolean(mode, "big", false);
            switch (variant) {
            case 0: engine.owner.replayMode = true; break;
            case 1: setInt(mode, "startlevel", 1); break;
            case 2: setBoolean(mode, "always20g", true); break;
            case 3: setBoolean(mode, "big", true); break;
            case 4: engine.ai = new DummyAI(); break;
            }
            setInt(mode, "rankingRank", -1);
            mode.saveReplay(engine, 0, engine.owner.replayProp);
            assertEquals(-1, readInt(mode, "rankingRank"));
            engine.owner.replayMode = false;
        }
    }

    // L1640 (exam grade cap combos) + L1643/1644 (exam ranking with type 1)
    @Test
    void saveReplayExamGradeCapCombos() throws Exception {
        int[][] combos = { {32, 31}, {32, 32}, {5, 0} }; // {grade, qualifiedGrade}
        for (int[] combo : combos) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = fresh(mode);
            engine.owner.replayProp = new CustomProperties();
            setInt(mode, "startlevel", 0);
            setBoolean(mode, "always20g", false);
            setBoolean(mode, "big", false);
            setBoolean(mode, "enableexam", true);
            setInt(mode, "grade", combo[0]);
            setInt(mode, "qualifiedGrade", combo[1]);
            clearRankings(mode);
            mode.saveReplay(engine, 0, engine.owner.replayProp);
            int expected = (combo[0] >= 32 && combo[1] < 32) ? 31 : combo[0];
            int[][] rankingGrade = (int[][]) readField(mode, "rankingGrade");
            assertEquals(0, readInt(mode, "rankingRank"));
            assertEquals(expected, rankingGrade[0][1]);
        }
    }

    // L1651: ST medal earned but an exam is running (no best-section update)
    @Test
    void saveReplayStMedalDuringExamSkipsSectionUpdate() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.owner.replayProp = new CustomProperties();
        setInt(mode, "startlevel", 0);
        setBoolean(mode, "always20g", false);
        setBoolean(mode, "big", false);
        setBoolean(mode, "enableexam", true);
        setBoolean(mode, "promotionFlag", true);
        setInt(mode, "medalST", 3);
        boolean[] rec = (boolean[]) readField(mode, "sectionIsNewRecord");
        rec[0] = true;
        int[][] best = (int[][]) readField(mode, "bestSectionTime");
        best[0][1] = 5400;
        int[] times = (int[]) readField(mode, "sectiontime");
        times[0] = 100;
        mode.saveReplay(engine, 0, engine.owner.replayProp);
        assertEquals(5400, best[0][1]); // untouched
        assertEquals(-1, readInt(mode, "rankingRank"));
    }

    // L1653: save triggered solely by medalST == 3 (unranked, no exam)
    @Test
    void saveReplaySaveTriggeredByStMedalOnly() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.owner.replayProp = new CustomProperties();
        setInt(mode, "startlevel", 0);
        setBoolean(mode, "always20g", false);
        setBoolean(mode, "big", false);
        setBoolean(mode, "enableexam", false);
        setInt(mode, "medalST", 3);
        setInt(mode, "grade", 0);
        // fill rankings so grade 0 cannot place
        int[][] rankingGrade = (int[][]) readField(mode, "rankingGrade");
        for (int i = 0; i < rankingGrade.length; i++) rankingGrade[i][0] = 33;
        mode.saveReplay(engine, 0, engine.owner.replayProp);
        assertEquals(-1, readInt(mode, "rankingRank"));
    }

    // -------------------------------------------------------------------
    // checkRanking comparison chain
    // -------------------------------------------------------------------

    // L1762: every operand of the tie-breaker chain in both directions
    @Test
    void checkRankingTieBreakerChain() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        fresh(mode);
        int[][] rankingGrade = (int[][]) readField(mode, "rankingGrade");
        int[][] rankingLevel = (int[][]) readField(mode, "rankingLevel");
        int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
        int[][] rankingRollclear = (int[][]) readField(mode, "rankingRollclear");
        for (int i = 0; i < rankingGrade.length; i++) {
            rankingGrade[i][0] = 10;
            rankingRollclear[i][0] = 2;
            rankingLevel[i][0] = 500;
            rankingTime[i][0] = 1000;
        }
        Method m = findMethod(GradeMania3Mode.class, "checkRanking",
                int.class, int.class, int.class, int.class, int.class);

        assertEquals(0, m.invoke(mode, 11, 0, 0, 0, 0));        // higher grade
        assertEquals(-1, m.invoke(mode, 9, 999, 0, 9, 0));      // lower grade
        assertEquals(0, m.invoke(mode, 10, 0, 0, 3, 0));        // same grade, better clear
        assertEquals(-1, m.invoke(mode, 10, 999, 0, 1, 0));     // same grade, worse clear
        assertEquals(0, m.invoke(mode, 10, 600, 9999, 2, 0));   // tie, higher level
        assertEquals(0, m.invoke(mode, 10, 500, 500, 2, 0));    // full tie, faster time
        assertEquals(-1, m.invoke(mode, 10, 500, 2000, 2, 0));  // full tie, slower time
        assertEquals(-1, m.invoke(mode, 10, 400, 500, 2, 0));   // tie, lower level
    }

    // -------------------------------------------------------------------
    // setPromotionalGrade
    // -------------------------------------------------------------------

    // L1795: loop exhausts without a match (history holds non -1 values below grade 0)
    @Test
    void setPromotionalGradeLoopExhaustsWithoutMatch() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        fresh(mode);
        int[] hist = (int[]) readField(mode, "gradeHistory");
        Arrays.fill(hist, -2); // never == -1, never >= 0
        setInt(mode, "promotionalExam", 7);
        Method m = findMethod(GradeMania3Mode.class, "setPromotionalGrade");
        m.invoke(mode);
        assertEquals(7, readInt(mode, "promotionalExam")); // untouched
    }

    // L1809: GM exam stays at 32 when already qualified >= 31
    @Test
    void setPromotionalGradeGmExamKeptWhenQualified() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        fresh(mode);
        int[] hist = (int[]) readField(mode, "gradeHistory");
        Arrays.fill(hist, 32);
        setInt(mode, "qualifiedGrade", 31);
        Method m = findMethod(GradeMania3Mode.class, "setPromotionalGrade");
        m.invoke(mode);
        assertEquals(32, readInt(mode, "promotionalExam"));
    }

    // -------------------------------------------------------------------
    // updateBestSectionTime
    // -------------------------------------------------------------------

    // L1824: exam type column receives the new record
    @Test
    void updateBestSectionTimeExamType() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        fresh(mode);
        setBoolean(mode, "enableexam", true);
        boolean[] rec = (boolean[]) readField(mode, "sectionIsNewRecord");
        rec[0] = true;
        int[] times = (int[]) readField(mode, "sectiontime");
        times[0] = 555;
        mode.updateBestSectionTime();
        int[][] best = (int[][]) readField(mode, "bestSectionTime");
        assertEquals(555, best[0][1]);
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private static GameEngine fresh(GradeMania3Mode mode) {
        GameManager manager = new GameManager(new QuietReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[0].ruleopt.fieldWidth = 10;
        manager.engine[0].ruleopt.fieldHeight = 20;
        manager.engine[0].playerID = 0;
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);
        return manager.engine[0];
    }

    /** Engine ready for a direct calcScore call in normal play (ending 0). */
    private static GameEngine scoringEngine(GradeMania3Mode mode) throws Exception {
        GameEngine engine = fresh(mode);
        engine.ending = 0;
        engine.createFieldIfNeeded();
        setInt(mode, "nextseclv", 100);
        return engine;
    }

    private static void clearRankings(GradeMania3Mode mode) throws Exception {
        for (String name : new String[] {"rankingGrade", "rankingLevel", "rankingTime", "rankingRollclear"}) {
            int[][] arr = (int[][]) readField(mode, name);
            for (int[] row : arr) Arrays.fill(row, 0);
        }
        setInt(mode, "rankingRank", -1);
    }

    private static int readInt(Object o, String n) throws Exception {
        Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        return f.getInt(o);
    }

    private static float readFloat(Object o, String n) throws Exception {
        Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        return f.getFloat(o);
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
            try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) { /* super */ }
        }
        throw new NoSuchFieldException(n);
    }

    private static Method findMethod(Class<?> cls, String n, Class<?>... params) throws NoSuchMethodException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try {
                Method m = c.getDeclaredMethod(n, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) { /* super */ }
        }
        throw new NoSuchMethodException(n);
    }
}
