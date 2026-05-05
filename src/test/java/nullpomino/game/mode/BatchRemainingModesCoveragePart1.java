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
 * Coverage for AvalancheFeverMode, SpeedManiaMode, MarathonMode, SpeedMania2Mode.
 * Targets remaining uncovered lines in calcScore, onLast, onMove, onARE,
 * onGameOver, lineClearEnd, loadSetting, saveSetting, loadRanking, saveRanking,
 * setSpeed, helpers, and rendering smoke tests.
 */
class BatchRemainingModesCoveragePart1 {

    // =====================================================================
    // AvalancheFeverMode
    // =====================================================================

    @Test
    void avalancheFeverRenderLastScoreWithMultiplier() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "lastscore", 500);
        setInt(m, "lastmultiplier", 3);
        setInt(m, "scgettime", 30);
        e.gameActive = true;
        e.stat = GameEngine.Status.MOVE;
        e.createFieldIfNeeded();
        setInt(m, "timeLimitAddDisplay", 60);
        setInt(m, "timeLimitAdd", 180);
        setInt(m, "garbageAdd", 5);
        setInt(m, "blocksCleared", 42);
        setInt(m, "level", 7);
        e.statistics.score = 10000;
        e.statistics.time = 500;
        e.statistics.maxChain = 10;
        setInt(m, "garbageSent", 200);
        setInt(m, "zenKeshiCount", 3);
        setInt(m, "timeLimit", 3000);
        m.renderLast(e, 0);
    }

    @Test
    void avalancheFeverRenderLastChainColorOrange() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.stat = GameEngine.Status.MOVE;
        e.createFieldIfNeeded();
        e.chain = 5;
        setInt(m, "chainDisplay", 30);
        setInt(m, "chainDisplayType", 2);
        setInt(m, "feverChainDisplay", 9);
        m.renderLast(e, 0);
    }

    @Test
    void avalancheFeverRenderLastChainColorRed() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.stat = GameEngine.Status.MOVE;
        e.createFieldIfNeeded();
        e.chain = 3;
        setInt(m, "chainDisplay", 30);
        setInt(m, "chainDisplayType", 2);
        setInt(m, "feverChainDisplay", 8);
        m.renderLast(e, 0);
    }

    @Test
    void avalancheFeverRenderLastChainDisplayYellow() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.stat = GameEngine.Status.MOVE;
        e.createFieldIfNeeded();
        e.chain = 7;
        setInt(m, "chainDisplay", 30);
        setInt(m, "chainDisplayType", 1);
        setInt(m, "feverChainDisplay", 6);
        m.renderLast(e, 0);
    }

    @Test
    void avalancheFeverRenderLastZenKeshiDisplay() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.gameActive = true;
        e.stat = GameEngine.Status.MOVE;
        e.createFieldIfNeeded();
        setInt(m, "zenKeshiDisplay", 30);
        m.renderLast(e, 0);
    }

    @Test
    void avalancheFeverOnMoveResetsCleared() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "cleared", true);
        setBoolean(m, "zenKeshi", true);
        assertFalse(m.onMove(e, 0));
        assertFalse(readBoolean(m, "cleared"));
        assertFalse(readBoolean(m, "zenKeshi"));
    }

    @Test
    void avalancheFeverOnLastChainDisplayAndZenKeshi() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.timerActive = true;
        setInt(m, "chainDisplay", 10);
        setInt(m, "zenKeshiDisplay", 10);
        setInt(m, "timeLimit", 361);
        m.onLast(e, 0);
        assertEquals(9, readInt(m, "chainDisplay"));
        assertEquals(9, readInt(m, "zenKeshiDisplay"));
        assertEquals(360, readInt(m, "timeLimit"));
    }

    @Test
    void avalancheFeverOnLastTimeLimitZero() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.timerActive = true;
        setInt(m, "timeLimit", 1);
        m.onLast(e, 0);
        assertEquals(0, readInt(m, "timeLimit"));
    }

    @Test
    void avalancheFeverOnLastMeterColors() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.timerActive = true;

        setInt(m, "timeLimit", 2000);
        m.onLast(e, 0);
        assertEquals(GameEngine.METER_COLOR_YELLOW, e.meterColor);

        setInt(m, "timeLimit", 1000);
        m.onLast(e, 0);
        assertEquals(GameEngine.METER_COLOR_ORANGE, e.meterColor);

        setInt(m, "timeLimit", 300);
        m.onLast(e, 0);
        assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
    }

    @Test
    void avalancheFeverOnLastFastForwardAll() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.stat = GameEngine.Status.MOVE;
        setInt(m, "fastenable", 2);
        setBoolean(m, "fastinuse", false);
        e.ctrl = new SimpleController(Controller.BUTTON_F);
        m.onLast(e, 0);
    }

    @Test
    void avalancheFeverCalcPtsCalcOjamaCalcChainMultiplier() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        setInt(m, "ojamaRate", 30);
        setInt(m, "chainLevelMultiplier", 10);
        Method calcPts = AvalancheFeverMode.class.getDeclaredMethod("calcPts", int.class);
        calcPts.setAccessible(true);
        assertEquals(50, (int) calcPts.invoke(m, 5));

        Method calcOjama = AvalancheFeverMode.class.getDeclaredMethod("calcOjama", int.class, int.class, int.class, int.class);
        calcOjama.setAccessible(true);
        assertEquals(6, (int) calcOjama.invoke(m, 100, 5, 50, 3));

        assertEquals(800, m.calcChainMultiplier(25));
        assertEquals(4, m.calcChainMultiplier(1));
    }

    @Test
    void avalancheFeverLineClearEndZenKeshi() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setField(m, "mapSubsets", new String[]{"dummy"});
        setField(m, "propFeverMap", new CustomProperties());
        e.createFieldIfNeeded();
        e.chain = 2;
        setBoolean(m, "cleared", true);
        setBoolean(m, "zenKeshi", true);
        setInt(m, "feverChainMin", 3);
        setInt(m, "feverChainMax", 15);
        setInt(m, "feverChain", 5);
        setInt(m, "timeLimit", 3600);
        m.lineClearEnd(e, 0);
        assertTrue(readInt(m, "timeLimit") > 3600);
    }

    @Test
    void avalancheFeverLineClearEndGameOver() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "cleared", false);
        e.field.setBlock(2, 0, new Block(Block.BLOCK_COLOR_RED));
        m.lineClearEnd(e, 0);
        assertEquals(GameEngine.Status.GAMEOVER, e.stat);
    }

    @Test
    void avalancheFeverLineClearEndOutOfTime() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "cleared", false);
        setInt(m, "timeLimit", 0);
        e.timerActive = true;
        m.lineClearEnd(e, 0);
        assertEquals(GameEngine.Status.ENDINGSTART, e.stat);
    }

    @Test
    void avalancheFeverDrawXorTimer() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        setInt(m, "timeLimit", 360);
        Method drawXor = AvalancheFeverMode.class.getDeclaredMethod("drawXorTimer", GameEngine.class, int.class);
        drawXor.setAccessible(true);
        drawXor.invoke(m, e, 0);
    }

    @Test
    void avalancheFeverRenderLastWithGameNotActive() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.gameActive = false;
        e.stat = GameEngine.Status.MOVE;
        m.renderLast(e, 0);
    }

    @Test
    void avalancheFeverRenderSettingXyzzyMode() throws Exception {
        AvalancheFeverMode m = new AvalancheFeverMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "xyzzy", 573);
        setInt(m, "menuCursor", 6);
        setField(m, "mapSubsets", new String[]{"dummy"});
        m.renderSetting(e, 0);
    }

    // =====================================================================
    // SpeedManiaMode
    // =====================================================================

    @Test
    void speedManiaModePlayerInitReplayMode() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        e.owner.replayMode = true;
        m.playerInit(e, 0);
        int[] bs = (int[]) readField(m, "bestSectionTime");
        assertNotNull(bs);
        for (int i = 0; i < bs.length; i++) {
            assertEquals(2520, bs[i]);
        }
    }

    @Test
    void speedManiaModeLevelUpMeter() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);

        e.statistics.level = 0;
        inv(m, "levelUp", e);
        assertEquals(GameEngine.METER_COLOR_GREEN, e.meterColor);

        e.statistics.level = 150;
        inv(m, "levelUp", e);
        assertEquals(GameEngine.METER_COLOR_YELLOW, e.meterColor);

        e.statistics.level = 180;
        inv(m, "levelUp", e);
        assertEquals(GameEngine.METER_COLOR_ORANGE, e.meterColor);

        setInt(m, "nextseclv", 201);
        e.statistics.level = 200;
        inv(m, "levelUp", e);
        assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
    }

    @Test
    void speedManiaModeOnMoveLevelUp() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0;
        e.statc[0] = 0;
        e.holdDisable = false;
        setBoolean(m, "lvupflag", false);
        e.statistics.level = 99;
        setInt(m, "nextseclv", 200);
        e.createFieldIfNeeded();
        assertFalse(m.onMove(e, 0));
    }

    @Test
    void speedManiaModeOnMoveREMedalOldVersion() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0;
        e.statc[0] = 0;
        e.holdDisable = false;
        setBoolean(m, "lvupflag", false);
        setInt(m, "version", 1);
        e.timerActive = true;
        setBoolean(m, "recoveryFlag", false);
        e.createFieldIfNeeded();
        fillField(e, 150);
        e.statistics.level = 99;
        setInt(m, "nextseclv", 200);
        m.onMove(e, 0);
        assertTrue(readBoolean(m, "recoveryFlag"));
    }

    @Test
    void speedManiaModeOnMoveREMedalOldVersionRecovery() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0;
        e.statc[0] = 0;
        e.holdDisable = false;
        setBoolean(m, "lvupflag", false);
        setInt(m, "version", 1);
        e.timerActive = true;
        setBoolean(m, "recoveryFlag", true);
        setInt(m, "medalRE", 0);
        e.createFieldIfNeeded();
        e.statistics.level = 99;
        setInt(m, "nextseclv", 200);
        m.onMove(e, 0);
        assertTrue(readInt(m, "medalRE") >= 1);
    }

    @Test
    void speedManiaModeOnMoveFlagReset() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0;
        e.statc[0] = 1;
        e.holdDisable = false;
        setBoolean(m, "lvupflag", true);
        m.onMove(e, 0);
        assertFalse(readBoolean(m, "lvupflag"));
    }

    @Test
    void speedManiaModeStartGameWithBig() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "big", true);
        m.startGame(e, 0);
        assertTrue(e.big);
    }

    @Test
    void speedManiaModeOnARE() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0;
        e.statc[0] = 10;
        e.statc[1] = 10;
        setBoolean(m, "lvupflag", false);
        e.statistics.level = 99;
        setInt(m, "nextseclv", 200);
        assertFalse(m.onARE(e, 0));
        assertTrue(readBoolean(m, "lvupflag"));
    }

    @Test
    void speedManiaModeCalcScoreSKMedalBig() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "big", true);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.statistics.totalFour = 1;
        e.ending = 0;
        m.calcScore(e, 0, 4);
        assertTrue(readInt(m, "medalSK") >= 1);
    }

    @Test
    void speedManiaModeCalcScoreACMedal() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.ending = 0;
        setInt(m, "medalAC", 0);
        m.calcScore(e, 0, 1);
        assertTrue(e.statistics.score > 0);
    }

    @Test
    void speedManiaModeCalcScoreCOMedalBig() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "big", true);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.combo = 4;
        e.ending = 0;
        m.calcScore(e, 0, 1);
        assertTrue(readInt(m, "medalCO") >= 3);
    }

    @Test
    void speedManiaModeCalcScoreCOMedalNormal() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "big", false);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.combo = 7;
        e.ending = 0;
        m.calcScore(e, 0, 1);
        assertTrue(readInt(m, "medalCO") >= 3);
    }

    @Test
    void speedManiaModeLevelUpREMedalVersion3() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "version", 3);
        setInt(m, "medalRE", 0);
        setBoolean(m, "recoveryFlag", false);
        e.timerActive = true;
        e.createFieldIfNeeded();
        fillField(e, 150);
        e.statistics.level = 100;
        inv(m, "levelUp", e);
        assertTrue(readBoolean(m, "recoveryFlag"));
    }

    @Test
    void speedManiaModeSaveReplayWithRanking() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.statistics.time = 3000;
        setInt(m, "medalST", 3);
        CustomProperties prop = new CustomProperties();
        m.saveReplay(e, 0, prop);
    }

    @Test
    void speedManiaModeOnResultPageSwitch() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ctrl = new SimpleController(Controller.BUTTON_UP);
        e.statc[1] = 0;
        m.onResult(e, 0);
        assertEquals(2, e.statc[1]);
        e.ctrl = new SimpleController(Controller.BUTTON_DOWN);
        m.onResult(e, 0);
        assertEquals(0, e.statc[1]);
    }

    @Test
    void speedManiaModeOnGameOverSecretGrade() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.statc[0] = 0;
        m.onGameOver(e, 0);
    }

    @Test
    void speedManiaModeSetAverageSectionTime() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        setInt(m, "sectionscomp", 1);
        setField(m, "sectiontime", new int[]{3000, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        inv(m, "setAverageSectionTime");
        assertEquals(3000, readInt(m, "sectionavgtime"));
    }

    @Test
    void speedManiaModeStMedalCheck() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        setField(m, "bestSectionTime", new int[]{5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000});
        setInt(m, "sectionlasttime", 3000);
        inv(m, "stMedalCheck", e, 0);
        assertEquals(3, readInt(m, "medalST"));
    }

    @Test
    void speedManiaModeRoMedalCheck() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        GameEngine e = fe(m);
        setInt(m, "rotateCount", 120);
        e.statistics.totalPieceLocked = 100;
        inv(m, "roMedalCheck", e);
        assertTrue(readInt(m, "medalRO") >= 1);
    }

    @Test
    void speedManiaModeGetMedalFontColor() throws Exception {
        assertEquals(EventReceiver.COLOR_RED, (int) inv(m("SpeedManiaMode"), "getMedalFontColor", 1));
        assertEquals(EventReceiver.COLOR_WHITE, (int) inv(m("SpeedManiaMode"), "getMedalFontColor", 2));
        assertEquals(EventReceiver.COLOR_YELLOW, (int) inv(m("SpeedManiaMode"), "getMedalFontColor", 3));
        assertEquals(-1, (int) inv(m("SpeedManiaMode"), "getMedalFontColor", 0));
    }

    @Test
    void speedManiaModeUpdateBestSectionTime() throws Exception {
        SpeedManiaMode m = new SpeedManiaMode();
        setField(m, "sectiontime", new int[]{1000, 2000, 3000, 0, 0, 0, 0, 0, 0, 0});
        setField(m, "sectionIsNewRecord", new boolean[]{true, false, true, false, false, false, false, false, false, false});
        setField(m, "bestSectionTime", new int[]{5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000});
        inv(m, "updateBestSectionTime");
        int[] bst = (int[]) readField(m, "bestSectionTime");
        assertEquals(1000, bst[0]);
        assertEquals(5000, bst[1]);
        assertEquals(3000, bst[2]);
    }

    // =====================================================================
    // MarathonMode
    // =====================================================================

    @Test
    void marathonModeCalcScoreTSpinBranches() throws Exception {
        MarathonMode m = new MarathonMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "enableB2B", true);
        setBoolean(m, "enableCombo", true);

        // T-Spin 0 mini
        e.tspin = true; e.tspinez = false; e.tspinmini = true; e.b2b = true;
        e.combo = 1; e.nowPieceObject = new Piece(Piece.PIECE_T);
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

        // T-Spin double mini all spin no b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = true;
        e.useAllSpinBonus = true; e.b2b = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 2);
        assertEquals(9, readInt(m, "lastevent"));

        // T-Spin double b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = false; e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 2);
        assertEquals(10, readInt(m, "lastevent"));

        // T-Spin double no b2b
        e.tspin = true; e.tspinez = false; e.tspinmini = false; e.b2b = false;
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
    void marathonModeCalcScoreNonTSpinBranches() throws Exception {
        MarathonMode m = new MarathonMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        e.createFieldIfNeeded();
        setBoolean(m, "enableB2B", false);
        setBoolean(m, "enableCombo", false);
        e.tspin = false;

        // Single
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertTrue(readInt(m, "lastevent") > 0);

        // Double
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 2);
        assertEquals(2, readInt(m, "lastevent"));

        // Triple
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 3);
        assertEquals(3, readInt(m, "lastevent"));

        // Four no b2b
        e.b2b = false;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 4);
        assertEquals(4, readInt(m, "lastevent"));

        // Four with b2b
        e.b2b = true;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 4);
        assertEquals(4, readInt(m, "lastevent"));
    }

    @Test
    void marathonModeCalcScoreLevelUpAndEnding() throws Exception {
        MarathonMode m = new MarathonMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        e.createFieldIfNeeded();
        e.tspin = false;
        setBoolean(m, "enableB2B", false);
        setBoolean(m, "enableCombo", false);

        // Level up (lines >= (level+1)*10)
        e.statistics.lines = 9;
        e.statistics.level = 0;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(1, e.statistics.level);

        // Ending (lines >= goal)
        e.statistics.lines = 149;
        e.statistics.level = 14;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertEquals(1, e.ending);
    }

    @Test
    void marathonModeCalcScoreBGMChangeAndMeter() throws Exception {
        MarathonMode m = new MarathonMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        e.createFieldIfNeeded();
        e.tspin = false;
        e.statistics.lines = 50;
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        m.calcScore(e, 0, 1);
        assertTrue(e.meterValue >= 0);
    }

    @Test
    void marathonModeRenderLastEvents() throws Exception {
        MarathonMode m = new MarathonMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.stat = GameEngine.Status.MOVE;
        e.gameActive = true;
        setInt(m, "scgettime", 30);
        setInt(m, "lastcombo", 3);

        int[] events = {1,2,3,4,5,6,7,8,9,10,11,12};
        for (int evt : events) {
            setInt(m, "lastevent", evt);
            setInt(m, "lastpiece", 0);
            setBoolean(m, "lastb2b", true);
            m.renderLast(e, 0);
        }
    }

    @Test
    void marathonModeRenderLastNonB2BEvents() throws Exception {
        MarathonMode m = new MarathonMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.stat = GameEngine.Status.MOVE;
        e.gameActive = true;
        setInt(m, "scgettime", 30);
        setInt(m, "lastcombo", 1);
        setBoolean(m, "lastb2b", false);

        int[] events = {4, 7, 8, 9, 10, 11, 12};
        for (int evt : events) {
            setInt(m, "lastevent", evt);
            setInt(m, "lastpiece", 0);
            m.renderLast(e, 0);
        }
    }

    @Test
    void marathonModeOnLastIncrementsScgettime() throws Exception {
        MarathonMode m = new MarathonMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "scgettime", 0);
        m.onLast(e, 0);
        assertTrue(readInt(m, "scgettime") > 0);
    }

    @Test
    void marathonModeStartGameOptions() throws Exception {
        MarathonMode m = new MarathonMode();
        GameEngine e = fe(m);
        setInt(m, "version", 2);
        setInt(m, "tspinEnableType", 2);
        setBoolean(m, "enableTSpinKick", true);
        setInt(m, "spinCheckType", 0);
        setBoolean(m, "tspinEnableEZ", false);
        setBoolean(m, "enableB2B", true);
        setBoolean(m, "enableCombo", true);
        setBoolean(m, "big", true);
        m.playerInit(e, 0);
        m.startGame(e, 0);
        assertTrue(e.big);
        assertTrue(e.tspinEnable);
        assertTrue(e.useAllSpinBonus);
    }

    @Test
    void marathonModeSaveReplayWithName() throws Exception {
        MarathonMode m = new MarathonMode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setField(m, "netPlayerName", "TestPlayer");
        CustomProperties prop = new CustomProperties();
        e.statistics.score = 10000;
        e.statistics.lines = 50;
        e.statistics.time = 3000;
        m.saveReplay(e, 0, prop);
        assertEquals("TestPlayer", prop.getProperty("0.net.netPlayerName", ""));
    }

    // =====================================================================
    // SpeedMania2Mode
    // =====================================================================

    @Test
    void speedMania2ModePlayerInitReplayMode() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        e.owner.replayMode = true;
        m.playerInit(e, 0);
        int[] bs = (int[]) readField(m, "bestSectionTime");
        assertNotNull(bs);
        for (int i = 0; i < bs.length; i++) {
            assertEquals(2520, bs[i]);
        }
    }

    @Test
    void speedMania2ModeLoadSaveSettingVersion2() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        setInt(m, "version", 2);
        setInt(m, "startlevel", 5);
        setBoolean(m, "lvstopse", true);
        setBoolean(m, "showsectiontime", true);
        setBoolean(m, "big", false);
        setInt(m, "torikan", 10980);
        setBoolean(m, "gradedisp", true);
        CustomProperties prop = new CustomProperties();
        inv(m, "saveSetting", prop, "Standard");
        assertEquals(5, prop.getProperty("speedmania2.startlevel", -1));

        SpeedMania2Mode m2 = new SpeedMania2Mode();
        setInt(m2, "version", 2);
        inv(m2, "loadSetting", prop, "Standard");
        assertEquals(5, readInt(m2, "startlevel"));
        assertEquals(10980, readInt(m2, "torikan"));
    }

    @Test
    void speedMania2ModeLoadSettingVersion1() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        CustomProperties prop = new CustomProperties();
        prop.setProperty("speedmania2.torikan", 7000);
        setInt(m, "version", 1);
        inv(m, "loadSetting", prop, "Standard");
        assertEquals(7000, readInt(m, "torikan"));
    }

    @Test
    void speedMania2ModeStartGameBig() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "big", true);
        m.startGame(e, 0);
        assertTrue(e.big);
    }

    @Test
    void speedMania2ModeStartGameLevel13() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "startlevel", 13);
        m.startGame(e, 0);
        assertEquals(1300, e.statistics.level);
        assertEquals(2, e.ending);
    }

    @Test
    void speedMania2ModeOnMoveLevelUp() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0; e.statc[0] = 0; e.holdDisable = false;
        setBoolean(m, "lvupflag", false);
        e.statistics.level = 199;
        setInt(m, "nextseclv", 300);
        e.createFieldIfNeeded();
        assertFalse(m.onMove(e, 0));
    }

    @Test
    void speedMania2ModeOnMoveGarbageRise() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0; e.statc[0] = 0; e.holdDisable = false;
        e.statistics.level = 500;
        setInt(m, "garbageCount", 25);
        e.createFieldIfNeeded();
        assertFalse(m.onMove(e, 0));
    }

    @Test
    void speedMania2ModeOnARE() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ending = 0; e.statc[0] = 10; e.statc[1] = 10;
        setBoolean(m, "lvupflag", false);
        e.statistics.level = 199;
        setInt(m, "nextseclv", 300);
        assertFalse(m.onARE(e, 0));
        assertTrue(readBoolean(m, "lvupflag"));
    }

    @Test
    void speedMania2ModeCalcScoreSKMedalBig() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "big", true);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.statistics.totalFour = 2;
        e.ending = 0;
        m.calcScore(e, 0, 4);
        assertTrue(readInt(m, "medalSK") >= 1);
    }

    @Test
    void speedMania2ModeCalcScoreSKMedalNormal() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "big", false);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.statistics.totalFour = 5;
        e.ending = 0;
        m.calcScore(e, 0, 4);
        assertTrue(readInt(m, "medalSK") >= 1);
    }

    @Test
    void speedMania2ModeCalcScoreACMedal() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        setInt(m, "medalAC", 0);
        e.ending = 0;
        m.calcScore(e, 0, 1);
    }

    @Test
    void speedMania2ModeCalcScoreCOMedalBig() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "big", true);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.combo = 4; e.ending = 0;
        m.calcScore(e, 0, 1);
        assertTrue(readInt(m, "medalCO") >= 3);
    }

    @Test
    void speedMania2ModeCalcScoreCOMedalNormal() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setBoolean(m, "big", false);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.combo = 7; e.ending = 0;
        m.calcScore(e, 0, 1);
        assertTrue(readInt(m, "medalCO") >= 3);
    }

    @Test
    void speedMania2ModeCalcScoreLevelUp() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.nowPieceObject = new Piece(Piece.PIECE_T);
        e.ending = 0;
        e.statistics.level = 0;
        setInt(m, "nextseclv", 100);
        m.calcScore(e, 0, 1);
    }

    @Test
    void speedMania2ModeLevelUpMeter() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.statistics.level = 0;
        inv(m, "levelUp", e);
        assertEquals(GameEngine.METER_COLOR_GREEN, e.meterColor);
    }

    @Test
    void speedMania2ModeOnLastRegretFrame() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "regretdispframe", 10);
        m.onLast(e, 0);
        assertEquals(9, readInt(m, "regretdispframe"));
    }

    @Test
    void speedMania2ModeOnGameOver() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.createFieldIfNeeded();
        e.statc[0] = 0; e.gameActive = true;
        m.onGameOver(e, 0);
    }

    @Test
    void speedMania2ModeOnReadyBone() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "startlevel", 11);
        e.statc[0] = 0;
        m.onReady(e, 0);
        assertTrue(e.bone);
    }

    @Test
    void speedMania2ModeSaveReplay() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        setInt(m, "medalST", 3);
        setField(m, "sectionIsNewRecord", new boolean[]{true, false, false, false, false, false, false, false, false, false, false, false, false});
        setField(m, "sectiontime", new int[]{1000, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0});
        setField(m, "bestSectionTime", new int[]{5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000});
        CustomProperties prop = new CustomProperties();
        m.saveReplay(e, 0, prop);
    }

    @Test
    void speedMania2ModeOnResult() throws Exception {
        SpeedMania2Mode m = new SpeedMania2Mode();
        GameEngine e = fe(m);
        m.playerInit(e, 0);
        e.ctrl = new SimpleController(Controller.BUTTON_UP);
        e.statc[1] = 0;
        m.onResult(e, 0);
        assertEquals(2, e.statc[1]);
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    @SuppressWarnings("serial")
    private static class SimpleController extends Controller {
        private final int btn;
        SimpleController(int b) { super(); btn = b; }
        @Override public boolean isPush(int b) { return (btn & b) != 0; }
        @Override public boolean isMenuRepeatKey(int b) { return (btn & b) != 0; }
        @Override public boolean isPress(int b) { return (btn & b) != 0; }
    }

    private static GameEngine fe(GameMode m) {
        GameManager mg = new GameManager(new EventReceiver());
        mg.mode = m;
        mg.init();
        mg.engine[0].init();
        return mg.engine[0];
    }

    private static void fillField(GameEngine e, int count) {
        int w = e.field.getWidth();
        int h = e.field.getHeight();
        int filled = 0;
        for (int y = h - 1; y >= 0 && filled < count; y--) {
            for (int x = 0; x < w && filled < count; x++) {
                e.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_RED));
                filled++;
            }
        }
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
    private static boolean readBoolean(Object o, String n) throws Exception { return ff(o, n).getBoolean(o); }
    private static Object readField(Object o, String n) throws Exception { return ff(o, n).get(o); }

    private static void setInt(Object o, String n, int v) throws Exception {
        Field f = ff(o, n);
        if (f.getType().isArray()) {
            f.set(o, v);
        } else {
            f.setInt(o, v);
        }
    }
    private static void setBoolean(Object o, String n, boolean v) throws Exception {
        Field f = ff(o, n);
        if (f.getType().isArray()) {
            f.set(o, v);
        } else {
            f.setBoolean(o, v);
        }
    }
    private static void setField(Object o, String n, Object v) throws Exception { ff(o, n).set(o, v); }

    private static Object m(String cn) throws Exception {
        return Class.forName("nullpomino.game.mode." + cn).getDeclaredConstructor().newInstance();
    }

    private static Object inv(Object obj, String name, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
        for (int i = 0; i < args.length; i++) {
            if (types[i] == Integer.class) types[i] = int.class;
            else if (types[i] == Boolean.class) types[i] = boolean.class;
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
