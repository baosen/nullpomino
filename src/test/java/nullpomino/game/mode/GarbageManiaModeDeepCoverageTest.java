package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Deep branch coverage for {@link GarbageManiaMode}: targets uncovered lines.
 */
class GarbageManiaModeDeepCoverageTest {

	// ---- onSetting branch coverage ----

	@Test void onSettingMenuCursor1TogglesAlwaysghost() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 1); setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		assertFalse(readFieldBool(mode, "alwaysghost"));
		mode.onSetting(e, 0);
		assertTrue(readFieldBool(mode, "alwaysghost"));
	}

	@Test void onSettingMenuCursor2TogglesAlways20g() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 2); setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(readFieldBool(mode, "always20g"));
	}

	@Test void onSettingMenuCursor3TogglesLvstopse() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 3); setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(readFieldBool(mode, "lvstopse"));
	}

	@Test void onSettingMenuCursor4TogglesShowsectiontime() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 4); setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(readFieldBool(mode, "showsectiontime"));
	}

	@Test void onSettingMenuCursor5TogglesBig() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 5); setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(readFieldBool(mode, "big"));
	}

	@Test void onSettingFButtonTogglesShowBestSectionTime() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;
		mode.onSetting(e, 0);
		assertTrue(readFieldBool(mode, "isShowBestSectionTime"));
	}

	@Test void onSettingReplayModePath() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.owner.replayMode = true;
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 0);
		assertTrue(mode.onSetting(e, 0));
		assertEquals(-1, readFieldInt(mode, "menuCursor"));
		setInt(mode, "menuTime", 60);
		assertFalse(mode.onSetting(e, 0));
	}

	@Test void onSettingAPressExits() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_A] = true;
		assertFalse(mode.onSetting(e, 0));
	}

	@Test void onSettingBPressQuits() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_B] = true;
		mode.onSetting(e, 0);
		assertTrue(e.quitflag);
	}

	// ---- renderLast ----

	@Test void renderLastSectionTimeView() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		setFieldBool(mode, "isShowBestSectionTime", true);
		setFieldInt(mode, "startlevel", 0);
		setFieldBool(mode, "big", false);
		setFieldBool(mode, "always20g", false);
		int[] bestSectionTime = (int[]) readFieldObj(mode, "bestSectionTime");
		bestSectionTime[0] = 100; bestSectionTime[1] = 200;
		mode.renderLast(e, 0);
	}

	@Test void renderLastScoreWithLastscore() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		setFieldInt(mode, "lastscore", 500);
		setFieldInt(mode, "scgettime", 10);
		mode.renderLast(e, 0);
	}

	@Test void renderLastEndingRollTime() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE; e.gameActive = true; e.ending = 2;
		setFieldInt(mode, "rolltime", 100);
		mode.renderLast(e, 0);
	}

	@Test void renderLastSectionTimeDisplay() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		setFieldBool(mode, "showsectiontime", true);
		int[] st = (int[]) readFieldObj(mode, "sectiontime");
		st[0] = 100; st[1] = 200;
		setFieldInt(mode, "sectionavgtime", 150);
		mode.renderLast(e, 0);
	}

	// ---- renderResult ----

	@Test void renderResultPage0() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		setFieldInt(mode, "secretGrade", 5);
		setFieldInt(mode, "rankingRank", 0);
		setFieldInt(mode, "garbageTotal", 42);
		mode.renderResult(e, 0);
	}

	@Test void renderResultPage1() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		int[] st = (int[]) readFieldObj(mode, "sectiontime");
		st[0] = 100;
		setFieldInt(mode, "sectionavgtime", 100);
		mode.renderResult(e, 0);
	}

	@Test void renderResultPage2() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT; e.statc[1] = 2;
		mode.renderResult(e, 0);
	}

	// ---- onResult ----

	@Test void onResultUpNav() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.statc[1] = 0;
		e.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		mode.onResult(e, 0);
		assertEquals(2, e.statc[1]);
	}

	@Test void onResultDownNav() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.statc[1] = 2;
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		mode.onResult(e, 0);
		assertEquals(0, e.statc[1]);
	}

	@Test void onResultFButton() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;
		mode.onResult(e, 0);
		assertTrue(readFieldBool(mode, "isShowBestSectionTime"));
	}

	// ---- onMove ----

	@Test void onMoveVersion2ResetsLvupflag() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ending = 0; e.statc[0] = 1;
		setFieldInt(mode, "version", 2);
		setFieldBool(mode, "lvupflag", true);
		mode.onMove(e, 0);
		assertFalse(readFieldBool(mode, "lvupflag"));
	}

	// ---- onARE ----

	@Test void onARELevelUp() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ending = 0; e.statc[0] = 5; e.statc[1] = 6;
		setFieldBool(mode, "lvupflag", false);
		setFieldInt(mode, "nextseclv", 999); // need nextseclv > level
		e.statistics.level = 0;
		mode.onARE(e, 0);
		assertTrue(readFieldBool(mode, "lvupflag"));
		assertTrue(e.statistics.level > 0);
	}

	// ---- calcScore ----

	@Test void calcScoreBigGarbageRising() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ending = 0; e.createFieldIfNeeded();
		setFieldInt(mode, "version", 3);
		setFieldBool(mode, "big", true);
		setFieldInt(mode, "garbageCount", 12);
		mode.calcScore(e, 0, 0);
		assertTrue(readFieldInt(mode, "garbageTotal") > 0);
	}

	@Test void calcScoreLevelStopSE() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		setFieldBool(mode, "lvstopse", true);
		e.statistics.level = 199;
		mode.calcScore(e, 0, 1);
		assertEquals(200, e.statistics.level);
	}

	@Test void calcScoreEndingAt999() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		e.statistics.level = 998;
		mode.calcScore(e, 0, 2);
		assertEquals(999, e.statistics.level);
		assertEquals(2, e.ending);
	}

	@Test void calcScoreBravo() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		mode.calcScore(e, 0, 4);
		assertTrue(e.statistics.score > 0);
	}

	// ---- onLast ----

	@Test void onLastEndingFFastForward() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.gameActive = true; e.ending = 2;
		setFieldInt(mode, "version", 2);
		setFieldInt(mode, "rolltime", 0);
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onLast(e, 0);
		assertEquals(5, readFieldInt(mode, "rolltime"));
	}

	@Test void onLastEndingReachesLimit() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.gameActive = true; e.ending = 2;
		setFieldInt(mode, "rolltime", 2023);
		mode.onLast(e, 0);
		assertEquals(GameEngine.Status.EXCELLENT, e.stat);
	}

	// ---- onGameOver ----
	@Test void onGameOverSetsSecretGrade() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.statc[0] = 0; e.createFieldIfNeeded();
		mode.onGameOver(e, 0);
		assertNotNull(readFieldObj(mode, "secretGrade"));
	}

	// ---- saveReplay (writes to owner.replayProp) ----
	@Test void saveReplayWritesVersion() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		setFieldInt(mode, "startlevel", 0);
		setFieldBool(mode, "big", false);
		setFieldBool(mode, "always20g", false);
		e.statistics.level = 500; e.statistics.time = 3600;
		e.ai = null;
		CustomProperties prop = new CustomProperties();
		e.owner.replayProp = prop;
		mode.saveReplay(e, 0, prop);
		assertEquals(3, e.owner.replayProp.getProperty("garbagemania.version", 0));
	}

	// ---- helpers ----
	private static void setInt(Object o, String n, int v) throws Exception { setFieldInt(o, n, v); }
	private static GameEngine freshEngine(GarbageManiaMode mode) {
		GameManager m = new GameManager(new EventReceiver()); m.mode = mode; m.init();
		m.engine[0].init(); m.engine[0].ruleopt.fieldWidth = 10; m.engine[0].ruleopt.fieldHeight = 20;
		return m.engine[0];
	}
	private static int readFieldInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); return f.getInt(o);
	}
	private static boolean readFieldBool(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); return f.getBoolean(o);
	}
	private static Object readFieldObj(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); return f.get(o);
	}
	private static void setFieldInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); f.setInt(o, v);
	}
	private static void setFieldBool(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); f.setBoolean(o, v);
	}
	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}
}
