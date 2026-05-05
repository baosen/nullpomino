package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in {@link ScoreAttackMode}:
 * playerInit replay branch, onSetting menu cursor/button branches,
 * renderLast section-time display, renderResult page branches,
 * onResult page change, onLast roll F-button, calcScore,
 * and onMove lvupflag.
 */
class ScoreAttackModeFinalCoverageTest {

    // ────────────────────────────────────────────────────────────────
    // playerInit replay branch (lines 197-198)
    // ────────────────────────────────────────────────────────────────
    @Test
    void playerInitReplayBranch() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameManager manager = new GameManager(new EventReceiver());
        manager.replayMode = true;
        manager.replayProp = new CustomProperties();
        manager.replayProp.setProperty("scoreattack.version", 1);
        manager.replayProp.setProperty("scoreattack.startlevel", 2);
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.playerInit(engine, 0);

        assertEquals(1, readInt(mode, "version"));
        assertEquals(2, readInt(mode, "startlevel"));
    }

    // ────────────────────────────────────────────────────────────────
    // onSetting: menu cursor cases (lines 284-293)
    // ────────────────────────────────────────────────────────────────
    @Test
    void onSettingMenuCursorAlwaysGhost() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "menuCursor", 1);
        setInt(mode, "menuTime", 10);
        engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

        boolean before = readBoolean(mode, "alwaysghost");
        mode.onSetting(engine, 0);
        assertEquals(!before, readBoolean(mode, "alwaysghost"));
    }

    @Test
    void onSettingMenuCursorAlways20g() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "menuCursor", 2);
        setInt(mode, "menuTime", 10);
        engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

        boolean before = readBoolean(mode, "always20g");
        mode.onSetting(engine, 0);
        assertEquals(!before, readBoolean(mode, "always20g"));
    }

    @Test
    void onSettingMenuCursorShowStime() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "menuCursor", 3);
        setInt(mode, "menuTime", 10);
        engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

        boolean before = readBoolean(mode, "showsectiontime");
        mode.onSetting(engine, 0);
        assertEquals(!before, readBoolean(mode, "showsectiontime"));
    }

    @Test
    void onSettingMenuCursorBig() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "menuCursor", 4);
        setInt(mode, "menuTime", 10);
        engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

        boolean before = readBoolean(mode, "big");
        mode.onSetting(engine, 0);
        assertEquals(!before, readBoolean(mode, "big"));
    }

    // ────────────────────────────────────────────────────────────────
    // onSetting: F button flip (lines 300-301)
    // ────────────────────────────────────────────────────────────────
    @Test
    void onSettingFButtonFlipsSectionTime() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "menuTime", 10);
        engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_F] = true;

        mode.onSetting(engine, 0);
        assertTrue(readBoolean(mode, "isShowBestSectionTime"));
    }

    // ────────────────────────────────────────────────────────────────
    // onSetting: B button quit (line 316)
    // ────────────────────────────────────────────────────────────────
    @Test
    void onSettingBButtonQuits() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_B] = true;

        mode.onSetting(engine, 0);
        assertTrue(engine.quitflag);
    }

    // ────────────────────────────────────────────────────────────────
    // onSetting: replay branch (lines 321-325)
    // ────────────────────────────────────────────────────────────────
    @Test
    void onSettingReplayBranch() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.owner.replayMode = true;
        setInt(mode, "menuTime", 59);

        mode.onSetting(engine, 0);
        assertEquals(60, readInt(mode, "menuTime"));
        assertEquals(-1, readInt(mode, "menuCursor"));

        // After 60, should return false
        boolean result = mode.onSetting(engine, 0);
        assertFalse(result);
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: section time display (lines 389-410, 416, 438-469)
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderLastSectionTimeDisplay() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.stat = GameEngine.Status.SETTING;
        setBoolean(mode, "isShowBestSectionTime", true);
        setInt(mode, "startlevel", 0);
        setBoolean(mode, "big", false);
        setBoolean(mode, "always20g", false);
        setBoolean(mode, "showsectiontime", false);
        engine.ai = null;

        int[] bestSectionTime = new int[]{3000, 4000, 5000};
        setField(mode, "bestSectionTime", bestSectionTime);
        boolean[] sectionIsNewRecord = new boolean[]{true, false, true};
        setField(mode, "sectionIsNewRecord", sectionIsNewRecord);

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastRollTimeDisplay() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.stat = GameEngine.Status.MOVE;
        engine.gameActive = true;
        engine.ending = 2;
        engine.statistics.score = 50000;
        int timeLeft = 100 * 60; // 100 seconds
        setInt(mode, "rolltime", 1956 - timeLeft);

        mode.renderLast(engine, 0);
    }

    @Test
    void renderLastSectionTimeActiveGame() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.stat = GameEngine.Status.MOVE;
        setBoolean(mode, "showsectiontime", true);
        int[] sectiontime = new int[]{3000, 0, 2000};
        setField(mode, "sectiontime", sectiontime);
        boolean[] sectionIsNewRecord = new boolean[]{true, false, false};
        setField(mode, "sectionIsNewRecord", sectionIsNewRecord);
        setInt(mode, "sectionavgtime", 2500);
        engine.statistics.level = 0;
        engine.ending = 0;

        mode.renderLast(engine, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: score display without lastscore (line 416)
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderLastScoreWithoutLastScore() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.stat = GameEngine.Status.MOVE;
        engine.statistics.score = 50000;
        engine.statistics.time = 3600;
        setInt(mode, "lastscore", 0);
        setInt(mode, "scgettime", 0);

        mode.renderLast(engine, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // calcScore: background fade (lines 569-571)
    // ────────────────────────────────────────────────────────────────
	@Test
	void calcScoreBackgroundFade() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statistics.level = 100;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.manualLock = false;
		engine.statc[0] = 10;
		// Set nextseclv so that (nextseclv-100)/100 > owner.backgroundStatus.bg
		setInt(mode, "nextseclv", 200);

		mode.calcScore(engine, 0, 1);

		// Should trigger background fade at level >= nextseclv boundary
		assertTrue(engine.owner.backgroundStatus.fadesw);
    }

    // ────────────────────────────────────────────────────────────────
    // onMove: lvupflag false (line 485)
    // ────────────────────────────────────────────────────────────────
    @Test
    void onMoveSetsLvupflagFalse() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.ending = 0;
        engine.statc[0] = 1;
        setBoolean(mode, "lvupflag", true);

        mode.onMove(engine, 0);
        assertFalse(readBoolean(mode, "lvupflag"));
    }

    // ────────────────────────────────────────────────────────────────
    // onLast: roll with F button (line 618)
    // ────────────────────────────────────────────────────────────────
    @Test
    void onLastRollWithFButton() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.gameActive = true;
        engine.ending = 2;
        engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;

        int before = readInt(mode, "rolltime");
        mode.onLast(engine, 0);
        int after = readInt(mode, "rolltime");
        assertEquals(5, after - before);
    }

    // ────────────────────────────────────────────────────────────────
    // renderResult: page branches (lines 660-677)
    // ────────────────────────────────────────────────────────────────
    @Test
    void renderResultPage0WithSecretGrade() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.statc[1] = 0;
        setInt(mode, "secretGrade", 10);

        mode.renderResult(engine, 0);
    }

    @Test
    void renderResultPage1() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.statc[1] = 1;
        int[] sectiontime = new int[]{3000, 0, 2000};
        setField(mode, "sectiontime", sectiontime);
        setInt(mode, "sectionavgtime", 2500);

        mode.renderResult(engine, 0);
    }

    @Test
    void renderResultPage2() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.statc[1] = 2;

        mode.renderResult(engine, 0);
    }

    // ────────────────────────────────────────────────────────────────
    // onResult: page change (lines 689-701)
    // ────────────────────────────────────────────────────────────────
    @Test
    void onResultPageUpDown() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.statc[1] = 0;

		// Up
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		mode.onResult(engine, 0);
		assertEquals(2, engine.statc[1]);

		// Reset button state for DOWN (UP still has buttonTime=1 from previous call)
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 0;
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = false;
		engine.statc[1] = 2;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1]);
    }

    @Test
    void onResultFButtonFlipsSectionTime() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_F] = true;

        mode.onResult(engine, 0);
        assertTrue(readBoolean(mode, "isShowBestSectionTime"));
    }

    // ────────────────────────────────────────────────────────────────
    // Helper: onSetting confirm (A button) with start
    // ────────────────────────────────────────────────────────────────
    @Test
    void onSettingAButtonStartsGame() throws Exception {
        ScoreAttackMode mode = new ScoreAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "menuTime", 10);
        engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
        engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

        boolean result = mode.onSetting(engine, 0);
        assertFalse(result);
    }

    // ────────────────────────────────────────────────────────────────
    // renderLast: section time with nextDisplay type 2
    // ────────────────────────────────────────────────────────────────
	@Test
	void renderLastSectionTimeActiveGameNextType2() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setBoolean(mode, "showsectiontime", true);
		int[] sectiontime = new int[]{3000, 0, 2000};
		setField(mode, "sectiontime", sectiontime);
		boolean[] sectionIsNewRecord = new boolean[]{true, false, false};
		setField(mode, "sectionIsNewRecord", sectionIsNewRecord);
		setInt(mode, "sectionavgtime", 2500);
		engine.statistics.level = 0;
		engine.ending = 0;

		mode.renderLast(engine, 0);
	}

    // ================================================================
    // Helpers
    // ================================================================

    private static GameEngine freshEngine(ScoreAttackMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        return manager.engine[0];
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException e) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name + " in " + cls.getName());
    }

    private static int readInt(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.getInt(obj);
    }

    private static boolean readBoolean(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.getBoolean(obj);
    }

    private static void setInt(Object obj, String name, int value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private static void setBoolean(Object obj, String name, boolean value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setBoolean(obj, value);
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, value);
    }
}
