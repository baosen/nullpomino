package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap coverage for GradeMania3Mode, part 1:
 * onSetting (switch default, level wrap edges, toggle back-directions, F/A menuTime gate),
 * renderSetting stcolor, renderLast (all uncovered render branches),
 * onReady/renderReady, onMove/onARE grade decay + level-stop edges,
 * onLast section bounds, onGameOver exam edges, renderResult/onResult exam screens.
 */
class GradeMania3ModeBranchGapTest {

    /** EventReceiver that never touches the filesystem and has a settable next-display type. */
    static class QuietReceiver extends EventReceiver {
        int nextDisplayType = 0;
        @Override public int getNextDisplayType() { return nextDisplayType; }
        @Override public void saveModeConfig(CustomProperties modeConfig) { /* hermetic no-op */ }
    }

    // -------------------------------------------------------------------
    // onSetting
    // -------------------------------------------------------------------

    // L586: switch(menuCursor) default branch (menuCursor outside 0..10)
    @Test
    void onSettingSwitchDefaultBranch() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "startlevel", 0);
        setInt(mode, "menuCursor", 99);
        press(engine, Controller.BUTTON_RIGHT);
        mode.onSetting(engine, 0);
        assertEquals(0, readInt(mode, "startlevel"));
    }

    // L595: first condition false (startlevel lands on 10)
    @Test
    void onSettingLevelUpToTenSkipsSpeedShift() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "menuCursor", 0);
        setInt(mode, "startlevel", 9);
        setInt(mode, "internalStartLevel", 900);
        press(engine, Controller.BUTTON_RIGHT);
        mode.onSetting(engine, 0);
        assertEquals(10, readInt(mode, "startlevel"));
    }

    // L595: second condition false (startlevel came down from 10)
    @Test
    void onSettingLevelDownFromTenSkipsSpeedShift() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "menuCursor", 0);
        setInt(mode, "startlevel", 10);
        setInt(mode, "internalStartLevel", 1000);
        press(engine, Controller.BUTTON_LEFT);
        mode.onSetting(engine, 0);
        assertEquals(9, readInt(mode, "startlevel"));
        assertEquals(1000, readInt(mode, "internalStartLevel"));
    }

    // L616/619/622/625/628/642/645: cover both toggle directions for every boolean option
    @Test
    void onSettingTogglesBothDirections() throws Exception {
        toggleBothWays(2, "alwaysghost");
        toggleBothWays(3, "always20g");
        toggleBothWays(4, "lvstopse");
        toggleBothWays(5, "showsectiontime");
        toggleBothWays(6, "gradedisp");
        toggleBothWays(9, "big");
        toggleBothWays(10, "enableexam");
    }

    private void toggleBothWays(int cursor, String field) throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "menuCursor", cursor);
        boolean v0 = readBoolean(mode, field);
        press(engine, Controller.BUTTON_LEFT);
        mode.onSetting(engine, 0);
        assertEquals(!v0, readBoolean(mode, field), field + " first toggle");
        engine.ctrl.reset();
        press(engine, Controller.BUTTON_LEFT);
        mode.onSetting(engine, 0);
        assertEquals(v0, readBoolean(mode, field), field + " second toggle");
    }

    // L651 + L657: F and A pushed while menuTime < 5 (gate stays closed)
    @Test
    void onSettingButtonsIgnoredBeforeMenuTimeFive() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "menuTime", 0);
        press(engine, Controller.BUTTON_F);
        press(engine, Controller.BUTTON_A);
        boolean cont = mode.onSetting(engine, 0);
        assertTrue(cont);
        assertFalse(readBoolean(mode, "isShowBestSectionTime"));
    }

    // L653: F toggles isShowBestSectionTime from true back to false
    @Test
    void onSettingSectionTimeViewToggleFromTrue() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "menuTime", 10);
        setBoolean(mode, "isShowBestSectionTime", true);
        press(engine, Controller.BUTTON_F);
        mode.onSetting(engine, 0);
        assertFalse(readBoolean(mode, "isShowBestSectionTime"));
    }

    // L667: exam roll gate short-circuits on always20g / big
    @Test
    void onSettingExamGateBlockedByTwentyGAndBig() throws Exception {
        for (int variant = 0; variant < 2; variant++) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = fresh(mode);
            setBoolean(mode, "enableexam", true);
            setBoolean(mode, "always20g", variant == 0);
            setBoolean(mode, "big", variant == 1);
            setInt(mode, "menuTime", 10);
            press(engine, Controller.BUTTON_A);
            boolean cont = mode.onSetting(engine, 0);
            assertFalse(cont);
            assertFalse(readBoolean(mode, "promotionFlag"));
            assertFalse(readBoolean(mode, "demotionFlag"));
        }
    }

    // L673: exam fires but neither promotion (exam<=qualified) nor demotion (points<30).
    // rand.nextInt(3)==0 happens ~1/3 per press; 120 presses make a miss essentially impossible.
    @Test
    void onSettingExamFiresWithNoPromotionAndNoDemotion() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setBoolean(mode, "enableexam", true);
        setBoolean(mode, "always20g", false);
        setBoolean(mode, "big", false);
        setInt(mode, "qualifiedGrade", 20);
        setInt(mode, "demotionPoints", 0);
        int[] hist = (int[]) readField(mode, "gradeHistory");
        for (int attempt = 0; attempt < 120; attempt++) {
            java.util.Arrays.fill(hist, -1); // -> promotionalExam = 0 <= qualifiedGrade
            setInt(mode, "menuTime", 10);
            engine.ctrl.reset();
            press(engine, Controller.BUTTON_A);
            mode.onSetting(engine, 0);
            assertFalse(readBoolean(mode, "promotionFlag"));
            assertFalse(readBoolean(mode, "demotionFlag"));
        }
    }

    // L718: renderSetting stcolor == 2 and stcolor == 0
    @Test
    void renderSettingSectionColorVariants() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "stcolor", 2);
        mode.renderSetting(engine, 0);
        setInt(mode, "stcolor", 0);
        mode.renderSetting(engine, 0);
        assertEquals(0, readInt(mode, "stcolor"));
    }

    // -------------------------------------------------------------------
    // renderLast
    // -------------------------------------------------------------------

    // L790: RESULT + !replayMode (true via second operand) and RESULT + replayMode (else branch)
    @Test
    void renderLastResultStatusBothReplayModes() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.stat = GameEngine.Status.RESULT;
        engine.owner.replayMode = false;
        mode.renderLast(engine, 0);
        engine.owner.replayMode = true;
        mode.renderLast(engine, 0);
        engine.owner.replayMode = false;
        assertEquals(GameEngine.Status.RESULT, engine.stat);
    }

    // L791: each of the five ranking-display guards false in turn
    @Test
    void renderLastRankingGuardFalseDirections() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.stat = GameEngine.Status.SETTING;

        setInt(mode, "startlevel", 1);
        mode.renderLast(engine, 0);
        setInt(mode, "startlevel", 0);

        setBoolean(mode, "big", true);
        mode.renderLast(engine, 0);
        setBoolean(mode, "big", false);

        setBoolean(mode, "always20g", true);
        mode.renderLast(engine, 0);
        setBoolean(mode, "always20g", false);

        engine.owner.replayMode = true;
        mode.renderLast(engine, 0);
        engine.owner.replayMode = false;

        engine.ai = new DummyAI();
        mode.renderLast(engine, 0);
        engine.ai = null;

        assertEquals(0, readInt(mode, "startlevel"));
    }

    // L794/795 (next display type 2) + L806/807 (highlight row == rankingRank)
    @Test
    void renderLastRankingWithBspDisplayAndRankHighlight() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        QuietReceiver recv = new QuietReceiver();
        GameEngine engine = fresh(mode, recv);
        recv.nextDisplayType = 2;
        engine.stat = GameEngine.Status.SETTING;
        setInt(mode, "startlevel", 0);
        setBoolean(mode, "big", false);
        setBoolean(mode, "always20g", false);
        setInt(mode, "rankingRank", 2);
        setBoolean(mode, "isShowBestSectionTime", false);
        mode.renderLast(engine, 0);
        assertEquals(2, readInt(mode, "rankingRank"));
    }

    // L822 (enableexam type) + L830 (new record with and without exam)
    @Test
    void renderLastBestSectionTimeExamAndNewRecord() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.stat = GameEngine.Status.SETTING;
        setInt(mode, "startlevel", 0);
        setBoolean(mode, "big", false);
        setBoolean(mode, "always20g", false);
        setBoolean(mode, "isShowBestSectionTime", true);
        setBoolean(mode, "enableexam", true);
        boolean[] rec = (boolean[]) readField(mode, "sectionIsNewRecord");
        rec[0] = true;
        mode.renderLast(engine, 0); // no exam running: (true && true)
        setBoolean(mode, "promotionFlag", true);
        mode.renderLast(engine, 0); // exam running: !isAnyExam() false
        setBoolean(mode, "promotionFlag", false);
        assertTrue(rec[0]);
    }

    // L853 (exam grade cap combos) + L856 (gradeflash %4) + L862 (scgettime <= 0 with lastscore != 0)
    @Test
    void renderLastGradeCapFlashAndScoreBranches() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.stat = GameEngine.Status.MOVE;
        setBoolean(mode, "gradedisp", true);
        setBoolean(mode, "enableexam", true);

        setInt(mode, "grade", 32);
        setInt(mode, "qualifiedGrade", 31); // cap applies
        setInt(mode, "gradeflash", 4);      // flash true branch
        setInt(mode, "lastscore", 5);
        setInt(mode, "scgettime", 0);       // second operand true
        mode.renderLast(engine, 0);

        setInt(mode, "qualifiedGrade", 32); // qualified < 32 false
        mode.renderLast(engine, 0);

        setInt(mode, "grade", 5);           // rgrade >= 32 false
        mode.renderLast(engine, 0);

        assertEquals(5, readInt(mode, "grade"));
    }

    // L874 (negative level clamp) + L879 (negative gravity meter)
    @Test
    void renderLastNegativeLevelAndGravity() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.stat = GameEngine.Status.MOVE;
        engine.statistics.level = -1;
        engine.speed.gravity = -1;
        mode.renderLast(engine, 0);
        assertEquals(-1, engine.statistics.level);
    }

    // L891/893 (roll time clamp to 0, and <10min highlight) + L898/901 (REGRET/COOL flash off-frames)
    @Test
    void renderLastRollTimeAndFlashMessages() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.stat = GameEngine.Status.MOVE;
        engine.gameActive = true;
        engine.ending = 2;

        setInt(mode, "rolltime", 4000); // over ROLLTIMELIMIT -> time < 0 clamp, time > 0 false
        setInt(mode, "regretdispframe", 3); // %4 != 0
        mode.renderLast(engine, 0);

        setInt(mode, "rolltime", 3238 - 100); // time = 100 -> (>0 && <600) true
        setInt(mode, "regretdispframe", 0);
        setInt(mode, "cooldispframe", 3); // %4 != 0
        mode.renderLast(engine, 0);

        assertEquals(3138, readInt(mode, "rolltime"));
    }

    // L913: sectiontime == null with showsectiontime true
    @Test
    void renderLastSectionTimeNullArray() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.stat = GameEngine.Status.MOVE;
        setBoolean(mode, "showsectiontime", true);
        setField(mode, "sectiontime", null);
        mode.renderLast(engine, 0);
        assertTrue(readBoolean(mode, "showsectiontime"));
    }

    // L914/915 (display type 2 columns), L919 (zero entries skipped), L921 (temp > 999 clamp)
    @Test
    void renderLastSectionTimeBspColumnsAndClamp() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        QuietReceiver recv = new QuietReceiver();
        GameEngine engine = fresh(mode, recv);
        recv.nextDisplayType = 2;
        engine.stat = GameEngine.Status.MOVE;
        setBoolean(mode, "showsectiontime", true);
        int[] times = new int[11];
        times[10] = 100; // i*100 = 1000 > 999 -> clamp; indices 0..9 stay 0 -> skip branch
        setField(mode, "sectiontime", times);
        setField(mode, "sectionIsNewRecord", new boolean[11]);
        setInt(mode, "stcolor", 1);
        setInt(mode, "sectionavgtime", 100);
        mode.renderLast(engine, 0);
        assertEquals(100, times[10]);
    }

    // -------------------------------------------------------------------
    // onReady / renderReady
    // -------------------------------------------------------------------

    // L963: B button (not A) skips the promotion-exam intro
    @Test
    void onReadyPromotionSkipWithBButton() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setBoolean(mode, "promotionFlag", true);
        setInt(mode, "readyframe", 100);
        press(engine, Controller.BUTTON_B);
        boolean ret = mode.onReady(engine, 0);
        assertFalse(ret);
        assertEquals(0, readInt(mode, "readyframe"));
    }

    // L983: promotionFlag true but readyframe == 0; L986: readyframe % 4 != 0
    @Test
    void renderReadyPromotionFrames() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setBoolean(mode, "promotionFlag", true);
        setInt(mode, "readyframe", 0);
        mode.renderReady(engine, 0); // guard false via readyframe
        setInt(mode, "readyframe", 3);
        mode.renderReady(engine, 0); // flash false via %4
        assertEquals(3, readInt(mode, "readyframe"));
    }

    // -------------------------------------------------------------------
    // onMove / onARE
    // -------------------------------------------------------------------

    // L997: holdDisable true blocks the new-piece level up
    @Test
    void onMoveHoldDisableBlocksLevelUp() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.ending = 0;
        engine.statc[0] = 0;
        engine.holdDisable = true;
        setBoolean(mode, "lvupflag", false);
        engine.statistics.level = 5;
        setInt(mode, "nextseclv", 100);
        mode.onMove(engine, 0);
        assertEquals(5, engine.statistics.level);
    }

    // L1002: level reaches nextseclv-1 on move, with lvstopse both true and false
    @Test
    void onMoveLevelStopSoundBothSettings() throws Exception {
        for (boolean se : new boolean[] {true, false}) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = fresh(mode);
            engine.ending = 0;
            engine.statc[0] = 0;
            engine.holdDisable = false;
            setBoolean(mode, "lvupflag", false);
            setBoolean(mode, "lvstopse", se);
            engine.statistics.level = 98;
            setInt(mode, "nextseclv", 100);
            mode.onMove(engine, 0);
            assertEquals(99, engine.statistics.level);
        }
    }

    // L1009: version 0 evaluates holdDisable operand both ways
    @Test
    void onMoveVersionZeroHoldDisableOperand() throws Exception {
        for (boolean hold : new boolean[] {false, true}) {
            GradeMania3Mode mode = new GradeMania3Mode();
            GameEngine engine = fresh(mode);
            setInt(mode, "version", 0);
            engine.ending = 0;
            engine.statc[0] = 1;
            engine.holdDisable = hold;
            engine.timerActive = false;
            setBoolean(mode, "lvupflag", true);
            mode.onMove(engine, 0);
            assertEquals(hold, readBoolean(mode, "lvupflag"));
        }
    }

    // L1014: combo > 0 stops grade point decay
    @Test
    void onMoveComboBlocksGradeDecay() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.ending = 0;
        engine.statc[0] = 1;
        engine.timerActive = true;
        engine.combo = 1;
        setInt(mode, "gradeBasicPoint", 5);
        setInt(mode, "gradeBasicDecay", 0);
        mode.onMove(engine, 0);
        assertEquals(0, readInt(mode, "gradeBasicDecay"));
    }

    // L1018: gradeBasicInternal beyond decay table clamps index
    @Test
    void onMoveGradeDecayIndexClamp() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.ending = 0;
        engine.statc[0] = 1;
        engine.timerActive = true;
        engine.combo = 0;
        engine.lockDelayNow = 0;
        engine.speed.lockDelay = 30;
        setInt(mode, "gradeBasicInternal", 50); // > table length-1 (31)
        setInt(mode, "gradeBasicPoint", 5);
        setInt(mode, "gradeBasicDecay", 9); // rate[31] = 10 -> ++ reaches 10 -> point--
        mode.onMove(engine, 0);
        assertEquals(4, readInt(mode, "gradeBasicPoint"));
        assertEquals(0, readInt(mode, "gradeBasicDecay"));
    }

    // L1027: ending==2 but roll already started
    @Test
    void onMoveRollAlreadyStarted() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.ending = 2;
        setBoolean(mode, "rollstarted", true);
        int blockHidden = engine.blockHidden;
        mode.onMove(engine, 0);
        assertEquals(blockHidden, engine.blockHidden);
    }

    // L1051: onARE with ending != 0 does nothing
    @Test
    void onAREEndingNonZeroSkips() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.ending = 1;
        engine.statistics.level = 50;
        setBoolean(mode, "lvupflag", false);
        mode.onARE(engine, 0);
        assertEquals(50, engine.statistics.level);
        assertFalse(readBoolean(mode, "lvupflag"));
    }

    // L1052 false direction + L1055 both lvstopse settings
    @Test
    void onARELevelCapAndLevelStop() throws Exception {
        // level already at nextseclv-1: inner if false
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.ending = 0;
        engine.statc[0] = 5;
        engine.statc[1] = 2;
        setBoolean(mode, "lvupflag", false);
        engine.statistics.level = 99;
        setInt(mode, "nextseclv", 100);
        mode.onARE(engine, 0);
        assertEquals(99, engine.statistics.level);
        assertTrue(readBoolean(mode, "lvupflag"));

        // level hits nextseclv-1 during ARE, both sound settings
        for (boolean se : new boolean[] {true, false}) {
            GradeMania3Mode mode2 = new GradeMania3Mode();
            GameEngine engine2 = fresh(mode2);
            engine2.ending = 0;
            engine2.statc[0] = 5;
            engine2.statc[1] = 2;
            setBoolean(mode2, "lvupflag", false);
            setBoolean(mode2, "lvstopse", se);
            engine2.statistics.level = 98;
            setInt(mode2, "nextseclv", 100);
            mode2.onARE(engine2, 0);
            assertEquals(99, engine2.statistics.level);
        }
    }

    // L1088/1093: bgmlv at the -1 sentinel skips fadeout and change
    @Test
    void onARELevelUpWithBgmSentinel() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.ending = 0;
        engine.statc[0] = 5;
        engine.statc[1] = 2;
        setBoolean(mode, "lvupflag", false);
        engine.statistics.level = 50;
        setInt(mode, "nextseclv", 100);
        setInt(mode, "bgmlv", 2); // tableBGMFadeout[2] == -1, tableBGMChange[2] == -1
        setInt(mode, "internalLevel", 800);
        mode.onARE(engine, 0);
        assertEquals(2, readInt(mode, "bgmlv"));
    }

    // -------------------------------------------------------------------
    // onLast
    // -------------------------------------------------------------------

    // L1383: timerActive true but ending == 2
    @Test
    void onLastTimerActiveDuringRoll() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.timerActive = true;
        engine.ending = 2;
        engine.gameActive = true;
        setInt(mode, "rolltime", 0);
        int[] times = (int[]) readField(mode, "sectiontime");
        mode.onLast(engine, 0);
        assertEquals(1, readInt(mode, "rolltime"));
        assertEquals(0, times[0]);
    }

    // L1386: section index out of range on both sides
    @Test
    void onLastSectionIndexOutOfRange() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.timerActive = true;
        engine.ending = 0;
        engine.gameActive = false;

        engine.statistics.level = -100; // section -1
        mode.onLast(engine, 0);
        engine.statistics.level = 1500; // section 15 >= length
        mode.onLast(engine, 0);

        int[] times = (int[]) readField(mode, "sectiontime");
        for (int t : times) assertEquals(0, t);
    }

    // -------------------------------------------------------------------
    // onGameOver
    // -------------------------------------------------------------------

    // L1449: promotion exam failed (grade < promotionalExam)
    @Test
    void onGameOverPromotionExamFailed() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.createFieldIfNeeded();
        engine.statc[0] = 0;
        setBoolean(mode, "enableexam", true);
        setBoolean(mode, "promotionFlag", true);
        setInt(mode, "promotionalExam", 20);
        setInt(mode, "grade", 5);
        setInt(mode, "qualifiedGrade", 0);
        mode.onGameOver(engine, 0);
        assertEquals(0, readInt(mode, "qualifiedGrade"));
    }

    // L1453: demotion exam passed (grade >= demotionExamGrade)
    @Test
    void onGameOverDemotionExamPassed() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.createFieldIfNeeded();
        engine.statc[0] = 0;
        setBoolean(mode, "enableexam", true);
        setBoolean(mode, "demotionFlag", true);
        setInt(mode, "demotionExamGrade", 5);
        setInt(mode, "grade", 10);
        setInt(mode, "qualifiedGrade", 5);
        mode.onGameOver(engine, 0);
        assertEquals(5, readInt(mode, "qualifiedGrade"));
    }

    // L1455: demotion below grade 0 clamps qualifiedGrade to 0
    @Test
    void onGameOverDemotionClampsQualifiedGrade() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        engine.createFieldIfNeeded();
        engine.statc[0] = 0;
        setBoolean(mode, "enableexam", true);
        setBoolean(mode, "demotionFlag", true);
        setInt(mode, "demotionExamGrade", 0);
        setInt(mode, "grade", -1);
        setInt(mode, "qualifiedGrade", 3);
        mode.onGameOver(engine, 0);
        assertEquals(0, readInt(mode, "qualifiedGrade"));
    }

    // -------------------------------------------------------------------
    // renderResult / onResult
    // -------------------------------------------------------------------

    // L1481/1486/1488: promotion exam screen with passframe % 4 != 0, both outcomes
    @Test
    void renderResultPromotionExamOffFrame() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setBoolean(mode, "promotionFlag", true);
        setInt(mode, "promotionalExam", 20);
        setInt(mode, "passframe", 419);
        setInt(mode, "grade", 5); // FAIL
        mode.renderResult(engine, 0);
        setInt(mode, "grade", 25); // PASS!!
        mode.renderResult(engine, 0);
        assertEquals(419, readInt(mode, "passframe"));
    }

    // L1491 false direction + L1497/1499: demotion exam screen off-frame outcomes
    @Test
    void renderResultDemotionAndNeitherExam() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "passframe", 419);
        setBoolean(mode, "promotionFlag", false);
        setBoolean(mode, "demotionFlag", false);
        mode.renderResult(engine, 0); // neither exam drawn

        setBoolean(mode, "demotionFlag", true);
        setInt(mode, "demotionExamGrade", 10);
        setInt(mode, "grade", 5); // FAIL
        mode.renderResult(engine, 0);
        setInt(mode, "grade", 15); // PASS
        mode.renderResult(engine, 0);
        assertEquals(419, readInt(mode, "passframe"));
    }

    // L1508/1510/1511/1512: result page 1 grade colours
    @Test
    void renderResultGradePageColourVariants() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "passframe", 0);
        engine.statc[1] = 0;
        setBoolean(mode, "enableexam", true);

        setInt(mode, "grade", 32);
        setInt(mode, "qualifiedGrade", 31); // cap to 31
        setInt(mode, "rollclear", 3);       // green via second operand
        setInt(mode, "menuCursor", 0);      // GM flash even frame
        mode.renderResult(engine, 0);

        setInt(mode, "qualifiedGrade", 32); // no cap
        setInt(mode, "rollclear", 4);       // orange via second operand
        setInt(mode, "menuCursor", 1);      // GM flash odd frame
        mode.renderResult(engine, 0);

        setInt(mode, "grade", 5);           // rgrade >= 32 false
        setInt(mode, "rollclear", 0);
        mode.renderResult(engine, 0);

        assertEquals(5, readInt(mode, "grade"));
    }

    // L1530 (stcolor 1, not a record) + L1532 (stcolor 0)
    @Test
    void renderResultSectionPageColourFallbacks() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "passframe", 0);
        engine.statc[1] = 1;
        int[] times = (int[]) readField(mode, "sectiontime");
        times[0] = 500;

        setInt(mode, "stcolor", 1); // sectionIsNewRecord[0] false
        mode.renderResult(engine, 0);
        setInt(mode, "stcolor", 0); // else-if false
        mode.renderResult(engine, 0);
        assertEquals(500, times[0]);
    }

    // L1544: statc[1] outside all known pages
    @Test
    void renderResultUnknownPage() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "passframe", 0);
        engine.statc[1] = 3;
        mode.renderResult(engine, 0);
        assertEquals(3, engine.statc[1]);
    }

    // L1571 (B button) + clamp to 420
    @Test
    void onResultExamSkipWithBButton() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "passframe", 500);
        press(engine, Controller.BUTTON_B);
        assertTrue(mode.onResult(engine, 0));
        assertEquals(419, readInt(mode, "passframe")); // clamped to 420 then decremented
    }

    // L1574 false direction: 300 <= passframe <= 420, button press changes nothing
    @Test
    void onResultExamMidWindowPressIgnored() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "passframe", 350);
        press(engine, Controller.BUTTON_A);
        assertTrue(mode.onResult(engine, 0));
        assertEquals(349, readInt(mode, "passframe"));
    }

    // L1579/1587: exam flags set but passframe != 420 (no jingle frame)
    @Test
    void onResultExamNonJingleFrame() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setBoolean(mode, "promotionFlag", true);
        setInt(mode, "passframe", 100);
        mode.onResult(engine, 0);
        setBoolean(mode, "promotionFlag", false);
        setBoolean(mode, "demotionFlag", true);
        setInt(mode, "passframe", 100);
        mode.onResult(engine, 0);
        assertEquals(99, readInt(mode, "passframe"));
    }

    // L1610 (page wraps 2 -> 0) + L1616 (F toggles view back off)
    @Test
    void onResultPageWrapAndSectionViewToggle() throws Exception {
        GradeMania3Mode mode = new GradeMania3Mode();
        GameEngine engine = fresh(mode);
        setInt(mode, "passframe", 0);
        engine.statc[1] = 2;
        press(engine, Controller.BUTTON_DOWN);
        mode.onResult(engine, 0);
        assertEquals(0, engine.statc[1]);

        engine.ctrl.reset();
        setBoolean(mode, "isShowBestSectionTime", true);
        press(engine, Controller.BUTTON_F);
        mode.onResult(engine, 0);
        assertFalse(readBoolean(mode, "isShowBestSectionTime"));
    }

    // -------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------

    private static GameEngine fresh(GradeMania3Mode mode) {
        return fresh(mode, new QuietReceiver());
    }

    private static GameEngine fresh(GradeMania3Mode mode, EventReceiver receiver) {
        GameManager manager = new GameManager(receiver);
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

    private static void press(GameEngine engine, int button) {
        engine.ctrl.buttonPress[button] = true;
        engine.ctrl.buttonTime[button] = 1;
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

    private static void setField(Object o, String n, Object v) throws Exception {
        Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        f.set(o, v);
    }

    private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) { /* super */ }
        }
        throw new NoSuchFieldException(n);
    }
}
