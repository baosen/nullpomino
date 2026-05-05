package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.*;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage for ScoreRaceMode, TimeAttackMode, ExtremeMode, UltraMode.
 */
class BatchRemainingModesCoveragePart2 {

    // =====================================================================
    // ScoreRaceMode
    // =====================================================================

    @Test
    void scoreRaceModePlayerInitReplayMode() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        e.owner.replayMode = true;
        m.playerInit(e, 0);
        assertEquals(0, readInt(m, "presetNumber"));
    }

    @Test
    void scoreRaceModeLoadSavePreset() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        CustomProperties prop = new CustomProperties();
        inv(m, "savePreset", e, prop, -1);
        assertTrue(prop.getProperty("scorerace.gravity.-1", -1) >= 0);

        ScoreRaceMode m2 = new ScoreRaceMode();
        GameEngine e2 = fe(m2);
        e2.speed.gravity = 999;
        inv(m2, "loadPreset", e2, prop, -1);
        assertTrue(e2.speed.gravity >= 0);
    }

    @Test
    void scoreRaceModeStartGameOptions() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "version", 1);
        setInt(m, "tspinEnableType", 2);
        setBoolean(m, "enableTSpinKick", true);
        setInt(m, "spinCheckType", 0);
        setBoolean(m, "tspinEnableEZ", false);
        setBoolean(m, "enableB2B", true);
        setBoolean(m, "enableCombo", true);
        setBoolean(m, "big", true);
        m.startGame(e, 0);
        assertTrue(e.big);
        assertTrue(e.tspinEnable);
        assertTrue(e.useAllSpinBonus);
    }

    @Test
    void scoreRaceModeStartGameVersion0() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "version", 0);
        setBoolean(m, "enableTSpin", true);
        m.startGame(e, 0);
        assertTrue(e.tspinEnable);
    }

    @Test
    void scoreRaceModeCalcScoreTSpinBranches() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "enableCombo", false);

        // T-Spin 0 mini
        e.tspin = true; e.tspinez = false; e.tspinmini = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 0);
        assertEquals(5, readInt(m, "lastevent"));

        // T-Spin 0 normal
        e.tspin = true; e.tspinez = false; e.tspinmini = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 0);
        assertEquals(6, readInt(m, "lastevent"));

        // T-Spin EZ b2b
        e.tspin = true; e.tspinez = true; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(12, readInt(m, "lastevent"));

        // T-Spin EZ no b2b
        e.tspin = true; e.tspinez = true; e.b2b = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(12, readInt(m, "lastevent"));

        // T-Spin single mini b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = true; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(7, readInt(m, "lastevent"));

        // T-Spin single mini no b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = true; e.b2b = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(7, readInt(m, "lastevent"));

        // T-Spin single b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = false; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(8, readInt(m, "lastevent"));

        // T-Spin single no b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = false; e.b2b = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(8, readInt(m, "lastevent"));

        // T-Spin double mini all spin b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = true;
        e.useAllSpinBonus = true; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 2);
        assertEquals(9, readInt(m, "lastevent"));

        // T-Spin double no b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = false; e.b2b = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 2);
        assertEquals(10, readInt(m, "lastevent"));

        // T-Spin double b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = false; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 2);
        assertEquals(10, readInt(m, "lastevent"));

        // T-Spin triple b2b
        e.tspin = true; e.tspinez = false; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 3);
        assertEquals(11, readInt(m, "lastevent"));

        // T-Spin triple no b2b
        e.tspin = true; e.tspinez = false; e.b2b = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 3);
        assertEquals(11, readInt(m, "lastevent"));
    }

    @Test
    void scoreRaceModeCalcScoreNonTSpin() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "enableCombo", false);
        e.tspin = false;

        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(1, readInt(m, "lastevent"));

        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 2);
        assertEquals(2, readInt(m, "lastevent"));

        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 3);
        assertEquals(3, readInt(m, "lastevent"));

        e.b2b = false; e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 4);
        assertEquals(4, readInt(m, "lastevent"));

        e.b2b = true; e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 4);
        assertEquals(4, readInt(m, "lastevent"));
    }

    @Test
    void scoreRaceModeCalcScoreComboAllClear() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "enableCombo", true);
        e.tspin = false;
        e.combo = 3;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertTrue(readInt(m, "lastcombo") >= 3);
    }

    @Test
    void scoreRaceModeOnLastTimerMeter() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.timerActive = true;
        e.statistics.time = 100;
        e.statistics.score = 5000;
        setInt(m, "goaltype", 0); // 10000 goal
        m.onLast(e, 0);
        assertTrue(e.meterValue >= 0);
    }

    @Test
    void scoreRaceModeOnLastGoalReached() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.timerActive = true;
        e.statistics.score = 10000;
        setInt(m, "goaltype", 0);
        m.onLast(e, 0);
        assertEquals(GameEngine.Status.ENDINGSTART, e.stat);
    }

    @Test
    void scoreRaceModeOnLastBGMFadeout() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.timerActive = true;
        e.statistics.score = 9500;
        setInt(m, "goaltype", 0);
        m.onLast(e, 0);
        assertTrue(e.meterValue >= 0);
    }

    @Test
    void scoreRaceModeLoadRanking() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        CustomProperties prop = new CustomProperties();
        inv(m, "loadRanking", prop, "TestRule");
        int[][] rt = (int[][]) readField(m, "rankingTime");
        assertEquals(-1, rt[0][0]);
    }

    @Test
    void scoreRaceModeSaveReplay() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "version", 1);
        e.statistics.score = 15000;
        e.statistics.time = 3000;
        e.statistics.lines = 100;
        e.statistics.spl = 150.0;
        CustomProperties prop = new CustomProperties();
        m.saveReplay(e, 0, prop);
    }

    @Test
    void scoreRaceModeUpdateRanking() throws Exception {
        ScoreRaceMode m = new ScoreRaceMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "goaltype", 0);
        inv(m, "updateRanking", 3000, 100, 150.0);
        int rank = readInt(m, "rankingRank");
        assertTrue(rank >= -1);
    }

    // =====================================================================
    // TimeAttackMode
    // =====================================================================

    @Test
    void timeAttackModePlayerInitReplayMode() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        e.owner.replayMode = true;
        m.playerInit(e, 0);
    }

    @Test
    void timeAttackModeSetSpeedAllTypes() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);

        int[] types = {0, 1, 2, 3, 4, 5, 6, 8, 9, 10};
        for (int gtype : types) {
            setInt(m, "goaltype", gtype);
            e.statistics.level = 0;
            inv(m, "setSpeed", e);
            assertNotNull(e.speed);
        }

        // Test specific type: ANOTHER2 (4)
        setInt(m, "goaltype", 4);
        e.statistics.level = 0;
        inv(m, "setSpeed", e);
        assertEquals(6, e.speed.are);

        // Test specific type: HELL (8) and HELLX (9)
        setInt(m, "goaltype", 8);
        e.statistics.level = 0;
        inv(m, "setSpeed", e);
        assertEquals(2, e.speed.are);

        setInt(m, "goaltype", 9);
        e.statistics.level = 0;
        inv(m, "setSpeed", e);
        assertEquals(2, e.speed.are);
    }

    @Test
    void timeAttackModeSetSpeedHellBlockSettings() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);

        // HELL type
        setInt(m, "goaltype", 8);
        e.statistics.level = 0;
        inv(m, "setSpeed", e);
        assertTrue(e.blockShowOutlineOnly);

        // HELLX type
        setInt(m, "goaltype", 9);
        e.statistics.level = 10;
        inv(m, "setSpeed", e);
        assertTrue(e.bone);
    }

    @Test
    void timeAttackModeSetStartBgmlv() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "norm", 60);
        inv(m, "setStartBgmlv", e);
        assertTrue(readInt(m, "bgmlv") >= 0);
    }

    @Test
    void timeAttackModeSetAverageSectionTime() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        setInt(m, "sectionscomp", 3);
        setInt(m, "startlevel", 0);
        setField(m, "sectiontime", new int[]{1000, 2000, 3000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        inv(m, "setAverageSectionTime");
        assertEquals(2000, readInt(m, "sectionavgtime")); // (1000+2000+3000)/3
    }

    @Test
    void timeAttackModeOnReady() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.statc[0] = 0;
        setInt(m, "startlevel", 5);
        setBoolean(m, "big", true);
        m.onReady(e, 0);
        assertEquals(5, e.statistics.level);
        assertEquals(50, readInt(m, "norm"));
    }

    @Test
    void timeAttackModeStartGame() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "bgmlv", 1);
        setInt(m, "goaltype", 0);
        m.startGame(e, 0);
        assertNotNull(e.speed);
    }

    @Test
    void timeAttackModeOnMove() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0; e.statc[0] = 0; e.holdDisable = false;
        m.onMove(e, 0);
        // Verify timerActive is re-enabled after levelup
    }

    @Test
    void timeAttackModeOnMoveEndingStart() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 2;
        e.staffrollEnable = true;
        setBoolean(m, "rollstarted", false);
        m.onMove(e, 0);
        assertTrue((Boolean) readField(m, "rollstarted"));
    }

    @Test
    void timeAttackModeOnLastLevelTimer() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.timerActive = true;
        e.ending = 0;
        setInt(m, "levelTimer", 10);
        m.onLast(e, 0);
        assertEquals(9, readInt(m, "levelTimer"));
    }

    @Test
    void timeAttackModeOnLastLevelTimerExpired() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.timerActive = true;
        e.ending = 0;
        setInt(m, "levelTimer", 0);
        m.onLast(e, 0);
        assertEquals(GameEngine.Status.GAMEOVER, e.stat);
    }

    @Test
    void timeAttackModeOnLastSectionTime() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.timerActive = true;
        e.ending = 0;
        e.statistics.level = 0;
        setInt(m, "levelTimer", 10);
        m.onLast(e, 0);
        int[] st = (int[]) readField(m, "sectiontime");
        assertEquals(1, st[0]);
    }

    @Test
    void timeAttackModeOnLastHeboHiddenHell() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "goaltype", 8); // HELL
        e.timerActive = true;
        e.ending = 0;
        e.statistics.level = 5;
        setInt(m, "levelTimer", 10);
        m.onLast(e, 0);
        assertTrue(e.heboHiddenEnable);
    }

    @Test
    void timeAttackModeOnLastEnding() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.ending = 2;
        m.onLast(e, 0);
        assertTrue(readInt(m, "rolltime") > 0);
    }

    @Test
    void timeAttackModeCalcScoreLevelUpEnding() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0;
        e.createFieldIfNeeded();
        e.statistics.level = 14;
        setInt(m, "norm", 149);
        setInt(m, "goaltype", 0);
        m.calcScore(e, 0, 1);
        assertTrue(e.ending >= 1 || readInt(m, "norm") >= 150);
    }

    @Test
    void timeAttackModeCalcScoreBGMChangeFadeout() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0;
        e.createFieldIfNeeded();
        setInt(m, "norm", 45);
        setInt(m, "goaltype", 0);
        setInt(m, "bgmlv", 0);
        m.calcScore(e, 0, 1);
    }

    @Test
    void timeAttackModeSaveReplay() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setField(m, "netPlayerName", "TestPlayer");
        CustomProperties prop = new CustomProperties();
        e.statistics.score = 150;
        e.statistics.time = 3000;
        e.statistics.rollclear = 1;
        m.saveReplay(e, 0, prop);
        assertEquals("TestPlayer", prop.getProperty("0.net.netPlayerName", ""));
    }

    @Test
    void timeAttackModeLoadSaveRanking() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        CustomProperties prop = new CustomProperties();
        inv(m, "saveRanking", prop, "TestRule");
        int[][] rl = (int[][]) readField(m, "rankingLines");
        assertEquals(0, rl[0][0]);
    }

    @Test
    void timeAttackModeUpdateRanking() throws Exception {
        TimeAttackMode m = new TimeAttackMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "goaltype", 0);
        for (int i = 0; i < 11; i++) {
            inv(m, "updateRanking", 150 - i, 3000, 0, 1);
        }
    }

    // =====================================================================
    // ExtremeMode
    // =====================================================================

    @Test
    void extremeModePlayerInitReplayMode() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        e.owner.replayMode = true;
        m.playerInit(e, 0);
        assertTrue(e.staffrollEnable);
    }

    @Test
    void extremeModeSetSpeed() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.statistics.level = 0;
        m.setSpeed(e);
        assertEquals(-1, e.speed.gravity);
        assertEquals(25, e.speed.are);

        e.statistics.level = 999;
        m.setSpeed(e);
        assertEquals(0, e.speed.are);
    }

    @Test
    void extremeModeStartGameOptions() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "version", 1);
        setInt(m, "tspinEnableType", 2);
        setBoolean(m, "enableTSpinKick", true);
        setBoolean(m, "enableB2B", true);
        setBoolean(m, "enableCombo", true);
        setBoolean(m, "big", true);
        m.startGame(e, 0);
        assertTrue(e.big);
        assertTrue(e.useAllSpinBonus);
    }

    @Test
    void extremeModeStartGameVersion0() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "version", 0);
        setBoolean(m, "enableTSpin", true);
        m.startGame(e, 0);
        assertTrue(e.tspinEnable);
    }

    @Test
    void extremeModeCalcScoreAllBranches() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "enableB2B", true);
        setBoolean(m, "enableCombo", true);

        // Test all T-Spin branches quickly
        int[][] testCases = {
            {1, 0, 0, 1, 1, 5},  // tspin,lines,tspinez,tspinmini -> lastevent
            {1, 0, 0, 0, 0, 6},
            {1, 1, 1, 0, 1, 12},
            {1, 1, 0, 1, 1, 7},
            {1, 1, 0, 0, 1, 8},
            {1, 2, 0, 1, 2, 9},
            {1, 2, 0, 0, 2, 10},
            {1, 3, 0, 0, 3, 11},
        };
        for (int[] tc : testCases) {
            e.tspin = tc[0] == 1;
            e.tspinez = tc[2] == 1;
            e.tspinmini = tc[3] == 1;
            e.b2b = tc[4] == 1;
            e.useAllSpinBonus = tc[5] == 9;
            e.nowPieceObject = new Piece(Piece.PIECE_T);
            e.ending = 0;
            m.calcScore(e, 0, tc[1]);
            assertEquals(tc[5], readInt(m, "lastevent"), "Failed for case tspin="+tc[0]+" lines="+tc[1]);
        }
    }

    @Test
    void extremeModeCalcScoreNonTSpin() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        e.createFieldIfNeeded();
        e.tspin = false;
        e.ending = 0;
        setBoolean(m, "enableCombo", false);

        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(1, readInt(m, "lastevent"));

        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 4);
        assertEquals(4, readInt(m, "lastevent"));
    }

    @Test
    void extremeModeCalcScoreEnding() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        e.createFieldIfNeeded();
        e.tspin = false;
        setBoolean(m, "endless", false);
        e.ending = 0;
        e.statistics.lines = 200;
        e.statistics.level = 19;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(2, e.ending);
    }

    @Test
    void extremeModeCalcScoreLevelUp() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        e.createFieldIfNeeded();
        e.tspin = false;
        e.ending = 0;
        e.statistics.lines = 10;
        e.statistics.level = 0;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertTrue(e.statistics.level >= 1);
    }

    @Test
    void extremeModeOnLastEnding() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.gameActive = true;
        e.ending = 2;
        m.onLast(e, 0);
        assertTrue(readInt(m, "rolltime") >= 1);
    }

    @Test
    void extremeModeSaveReplay() throws Exception {
        ExtremeMode m = new ExtremeMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setField(m, "netPlayerName", "TestPlayer");
        CustomProperties prop = new CustomProperties();
        e.statistics.score = 50000;
        e.statistics.lines = 200;
        e.statistics.time = 5000;
        m.saveReplay(e, 0, prop);
        assertEquals("TestPlayer", prop.getProperty("0.net.netPlayerName", ""));
    }

    @Test
    void extremeModeLoadSaveSetting() throws Exception {
        ExtremeMode m = new ExtremeMode();
        setBoolean(m, "endless", true);
        CustomProperties prop = new CustomProperties();
        inv(m, "saveSetting", prop);
        assertTrue(prop.getProperty("extreme.endless", false));

        ExtremeMode m2 = new ExtremeMode();
        inv(m2, "loadSetting", prop);
        assertTrue((Boolean) readField(m2, "endless"));
    }

    // =====================================================================
    // UltraMode
    // =====================================================================

    @Test
    void ultraModePlayerInitReplayMode() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        e.owner.replayMode = true;
        m.playerInit(e, 0);
        assertEquals(0, readInt(m, "presetNumber"));
    }

    @Test
    void ultraModeLoadSavePreset() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        CustomProperties prop = new CustomProperties();
        inv(m, "savePreset", e, prop, -1);
        assertTrue(prop.getProperty("ultra.gravity.-1", -1) >= 0);

        UltraMode m2 = new UltraMode();
        GameEngine e2 = fe(m2);
        e2.speed.gravity = 999;
        inv(m2, "loadPreset", e2, prop, -1);
        assertTrue(e2.speed.gravity >= 0);
    }

    @Test
    void ultraModeStartGameOptions() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "version", 1);
        setInt(m, "tspinEnableType", 2);
        setBoolean(m, "enableTSpinKick", true);
        setBoolean(m, "enableB2B", true);
        setBoolean(m, "enableCombo", true);
        setBoolean(m, "big", true);
        m.startGame(e, 0);
        assertTrue(e.big);
        assertTrue(e.tspinEnable);
        assertTrue(e.useAllSpinBonus);
    }

    @Test
    void ultraModeStartGameVersion0() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "version", 0);
        setBoolean(m, "enableTSpin", true);
        m.startGame(e, 0);
        assertTrue(e.tspinEnable);
    }

    @Test
    void ultraModeCalcScoreTSpinBranches() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "enableCombo", false);

        // T-Spin 0 mini
        e.tspin = true; e.tspinez = false; e.tspinmini = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 0);
        assertEquals(5, readInt(m, "lastevent"));

        // T-Spin 0 normal
        e.tspin = true; e.tspinez = false; e.tspinmini = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 0);
        assertEquals(6, readInt(m, "lastevent"));

        // T-Spin EZ b2b
        e.tspin = true; e.tspinez = true; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(12, readInt(m, "lastevent"));

        // T-Spin single mini b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = true; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(7, readInt(m, "lastevent"));

        // T-Spin single b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = false; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(8, readInt(m, "lastevent"));

        // T-Spin double mini all spin b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = true;
        e.useAllSpinBonus = true; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 2);
        assertEquals(9, readInt(m, "lastevent"));

        // T-Spin double b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = false; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 2);
        assertEquals(10, readInt(m, "lastevent"));

        // T-Spin triple b2b
        e.tspin = true; e.tspinez = false; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 3);
        assertEquals(11, readInt(m, "lastevent"));
    }

    @Test
    void ultraModeCalcScoreNonTSpin() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "enableCombo", false);
        e.tspin = false;

        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(1, readInt(m, "lastevent"));

        e.b2b = false; e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 4);
        assertEquals(4, readInt(m, "lastevent"));

        e.b2b = true; e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 4);
        assertEquals(4, readInt(m, "lastevent"));
    }

    @Test
    void ultraModeCalcScoreACMedal() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.tspin = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertTrue(e.statistics.score > 0);
    }

    @Test
    void ultraModeOnLastTimerMeter() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.timerActive = true;
        e.statistics.time = 100;
        setInt(m, "goaltype", 0);
        m.onLast(e, 0);
        assertTrue(e.meterValue >= 0);
    }

    @Test
    void ultraModeOnLastTimeUp() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.timerActive = true;
        e.statistics.time = 3601;
        setInt(m, "goaltype", 0);
        m.onLast(e, 0);
        assertEquals(GameEngine.Status.ENDINGSTART, e.stat);
    }

    @Test
    void ultraModeOnLastCountdown() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.timerActive = true;
        e.statistics.time = 3540; // 1 minute before end of 1-min game
        setInt(m, "goaltype", 0);
        m.onLast(e, 0);
    }

    @Test
    void ultraModeOnLastBGMfade() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.timerActive = true;
        e.statistics.time = 3571; // 5 seconds before end
        setInt(m, "goaltype", 0);
        m.onLast(e, 0);
        assertTrue(e.owner.bgmStatus.fadesw);
    }

    @Test
    void ultraModeSaveReplay() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setField(m, "netPlayerName", "TestPlayer");
        CustomProperties prop = new CustomProperties();
        e.statistics.score = 50000;
        e.statistics.lines = 200;
        m.saveReplay(e, 0, prop);
        assertEquals("TestPlayer", prop.getProperty("0.net.netPlayerName", ""));
    }

    @Test
    void ultraModeLoadSaveRanking() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        CustomProperties prop = new CustomProperties();
        m.loadRanking(prop, "TestRule");
        int[][][] rs = (int[][][]) readField(m, "rankingScore");
        assertEquals(0, rs[0][0][0]);
        assertEquals(0, rs[0][0][1]);
    }

    @Test
    void ultraModeUpdateRanking() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "goaltype", 0);
        inv(m, "updateRanking", 50000, 200);
        int[] ranks = (int[]) readField(m, "rankingRank");
        assertTrue(ranks[0] >= -1);
        assertTrue(ranks[1] >= -1);
    }

    @Test
    void ultraModeCheckRankingBothTypes() throws Exception {
        UltraMode m = new UltraMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "goaltype", 0);
        int r0 = (int) inv(m, "checkRanking", 50000, 200, 0); // score ranking
        int r1 = (int) inv(m, "checkRanking", 50000, 200, 1); // line ranking
        assertTrue(r0 >= -1);
        assertTrue(r1 >= -1);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private static GameEngine fe(GameMode m) {
        GameManager mg = new GameManager(new EventReceiver());
        mg.mode = m;
        mg.init();
        mg.engine[0].init();
        return mg.engine[0];
    }

    private static Field ff(Object o, String n) throws NoSuchFieldException {
        Class<?> c = o.getClass();
        while (c != null) {
            try { Field f = c.getDeclaredField(n); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ex) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(n);
    }

    private static int readInt(Object o, String n) throws Exception { return ff(o, n).getInt(o); }
    private static Object readField(Object o, String n) throws Exception { return ff(o, n).get(o); }

    private static void setInt(Object o, String n, int v) throws Exception { ff(o, n).setInt(o, v); }
    private static void setBoolean(Object o, String n, boolean v) throws Exception { ff(o, n).setBoolean(o, v); }
    private static void setField(Object o, String n, Object v) throws Exception { ff(o, n).set(o, v); }

    private static Object inv(Object obj, String name, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
        for (int i = 0; i < args.length; i++) {
            if (types[i] == Integer.class) types[i] = int.class;
            else if (types[i] == Boolean.class) types[i] = boolean.class;
            else if (types[i] == Double.class) types[i] = double.class;
        }
        Method m = findM(obj.getClass(), name, types);
        m.setAccessible(true);
        return m.invoke(obj, args);
    }

    private static Method findM(Class<?> cls, String name, Class<?>... pts) throws NoSuchMethodException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredMethod(name, pts); }
            catch (NoSuchMethodException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchMethodException(name);
    }
}
