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
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

class FinalModeDeepCoverageTest {

	@Test void onSettingCursor1Lvstopse() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 1); setInt(mode, "menuTime", 5);
		assertFalse(readBool(mode, "lvstopse"));
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1; e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(readBool(mode, "lvstopse"));
	}

	@Test void onSettingCursor2Showsectiontime() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 2); setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1; e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(readBool(mode, "showsectiontime"));
	}

	@Test void onSettingCursor3Big() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 3); setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1; e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(readBool(mode, "big"));
	}

	@Test void onSettingFButton() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1; e.ctrl.buttonPress[Controller.BUTTON_F] = true;
		mode.onSetting(e, 0);
		assertTrue(readBool(mode, "isShowBestSectionTime"));
	}

	@Test void onSettingReplayPath() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.owner.replayMode = true;
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		assertTrue(mode.onSetting(e, 0));
		setInt(mode, "menuTime", 60);
		assertFalse(mode.onSetting(e, 0));
	}

	@Test void onSettingAPress() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_A] = 1; e.ctrl.buttonPress[Controller.BUTTON_A] = true;
		assertFalse(mode.onSetting(e, 0));
	}

	@Test void onSettingBPress() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_B] = 1; e.ctrl.buttonPress[Controller.BUTTON_B] = true;
		mode.onSetting(e, 0);
		assertTrue(e.quitflag);
	}

	@Test void renderLastSectionTimeDisplay() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.MOVE;
		setBool(mode, "showsectiontime", true); setIntArr(mode, "sectiontime", 0, 100);
		setInt(mode, "sectionavgtime", 100);
		mode.renderLast(e, 0);
	}

	@Test void renderLastGradeDisplay() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.MOVE;
		setInt(mode, "grade", 1); setInt(mode, "gradeflash", 1);
		mode.renderLast(e, 0);
	}

	@Test void renderLastEndingRoll() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.MOVE;
		e.gameActive = true; e.ending = 2;
		mode.renderLast(e, 0);
	}

	@Test void renderResultPage0() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		setInt(mode, "rankingRank", 0); setInt(mode, "medalST", 2); setInt(mode, "medalAC", 3);
		mode.renderResult(e, 0);
	}

	@Test void renderResultPage1() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		setIntArr(mode, "sectiontime", 0, 100); setInt(mode, "sectionavgtime", 100);
		mode.renderResult(e, 0);
	}

	@Test void onResultUpNav() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.statc[1] = 0;
		e.ctrl.buttonTime[Controller.BUTTON_UP] = 1; e.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		mode.onResult(e, 0);
		assertEquals(2, e.statc[1]);
	}

	@Test void calcScoreLevel999Ending() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		e.statistics.level = 997;
		mode.calcScore(e, 0, 3);
		assertEquals(1, e.ending);
	}

	@Test void calcScoreBravo() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		mode.calcScore(e, 0, 4);
		assertTrue(e.statistics.score > 0);
	}

	@Test void calcScoreCombo2() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		setInt(mode, "comboValue", 5);
		mode.calcScore(e, 0, 2);
		assertTrue(e.statistics.score > 0);
	}

	@Test void onLastEndingTimer() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.gameActive = true; e.ending = 2;
		setInt(mode, "version", 3); setInt(mode, "rolltime", 0);
		mode.onLast(e, 0);
		assertEquals(1, readInt(mode, "rolltime"));
	}

	@Test void onLastEndingLimitNew() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.gameActive = true; e.ending = 2;
		setInt(mode, "version", 3); setInt(mode, "rolltime", 3237);
		mode.onLast(e, 0);
		assertEquals(GameEngine.Status.EXCELLENT, e.stat);
	}

	@Test void onLastEndingLimitOld() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.gameActive = true; e.ending = 2;
		setInt(mode, "version", 2); setInt(mode, "rolltime", 1981);
		mode.onLast(e, 0);
		assertEquals(GameEngine.Status.EXCELLENT, e.stat);
	}

	@Test void onGameOverSetsGrade() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); e.statc[0] = 0; e.createFieldIfNeeded();
		mode.onGameOver(e, 0);
		assertTrue(readInt(mode, "grade") >= 0);
	}

	@Test void saveReplayWritesVersion() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0); setInt(mode, "startlevel", 0);
		e.statistics.level = 500; e.statistics.time = 3000; e.ai = null;
		CustomProperties prop = new CustomProperties();
		e.owner.replayProp = prop;
		mode.saveReplay(e, 0, prop);
		assertEquals(3, e.owner.replayProp.getProperty("final.version", 0));
	}

	@Test void setSpeedVersion3() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		setInt(mode, "version", 3);
		e.ruleopt.lockresetMove = false; e.ruleopt.lockresetRotate = false;
		invokeSetSpeed(mode, e);
		assertTrue(e.speed.lockDelay > 0);
	}

	@Test void setSpeedVersion2() throws Exception {
		FinalMode mode = new FinalMode(); GameEngine e = freshEngine(mode);
		setInt(mode, "version", 2);
		e.ruleopt.lockresetMove = true;
		invokeSetSpeed(mode, e);
		assertTrue(e.speed.lockDelay > 0);
	}

	private static GameEngine freshEngine(FinalMode mode) {
		GameManager m = new GameManager(new EventReceiver()); m.mode = mode; m.init();
		m.engine[0].init(); m.engine[0].ruleopt.fieldWidth = 10; m.engine[0].ruleopt.fieldHeight = 20;
		return m.engine[0];
	}
	private static int readInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); return f.getInt(o);
	}
	private static boolean readBool(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); return f.getBoolean(o);
	}
	private static void setInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); f.setInt(o, v);
	}
	private static void setBool(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); f.setBoolean(o, v);
	}
	private static void setIntArr(Object o, String n, int idx, int v) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true);
		((int[])f.get(o))[idx] = v;
	}
	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}
	private static void invokeSetSpeed(FinalMode mode, GameEngine e) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true); m.invoke(mode, e);
	}
}
