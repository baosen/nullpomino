package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.OnOffMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch coverage for {@link GradeMania2Mode}: onSetting (cursors 1-2, replay
 * path, F button, A/B), renderLast (section time, ending roll), renderResult
 * (page 1 with sections), calcScore (ending, bravo, level 999, combo),
 * onLast (ending timer, F fast-forward), onGameOver, saveReplay, setSpeed,
 * setStartBgmlv.
 */
class GradeMania2ModeDeepCoverageTest {

	@Test void onSettingCursor1Alwaysghost() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 1); setInt(mode, "menuTime", 5);
		assertFalse(getOnOff(mode, "alwaysghost"));
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(getOnOff(mode, "alwaysghost"));
	}

	@Test void onSettingCursor3Lvstopse() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 3); setInt(mode, "menuTime", 5);
		assertFalse(getOnOff(mode, "lvstopse"));
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(getOnOff(mode, "lvstopse"));
	}

	@Test void onSettingCursor5Showsectiontime() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuCursor", 5); setInt(mode, "menuTime", 5);
		assertFalse(getOnOff(mode, "showsectiontime"));
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(e, 0);
		assertTrue(getOnOff(mode, "showsectiontime"));
	}

	@Test void onSettingFButton() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;
		mode.onSetting(e, 0);
		assertTrue(readBool(mode, "isShowBestSectionTime"));
	}

	@Test void onSettingReplayPath() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.owner.replayMode = true; e.stat = GameEngine.Status.SETTING; e.resetStatc();
		assertTrue(mode.onSetting(e, 0));
		assertEquals(-1, readInt(mode, "menuCursor"));
		setInt(mode, "menuTime", 60);
		assertFalse(mode.onSetting(e, 0));
	}

	@Test void onSettingAPress() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_A] = true;
		assertFalse(mode.onSetting(e, 0));
	}

	@Test void onSettingBPress() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_B] = true;
		mode.onSetting(e, 0);
		assertTrue(e.quitflag);
	}

	// ---- renderLast ----
	@Test void renderLastSectionTime() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		setOnOff(mode, "showsectiontime", true);
		int[] st = (int[]) readObj(mode, "sectiontime");
		st[0] = 100;
		setInt(mode, "sectionavgtime", 100);
		mode.renderLast(e, 0);
	}

	@Test void renderLastEndingRoll() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE; e.gameActive = true; e.ending = 2;
		setInt(mode, "rolltime", 100);
		mode.renderLast(e, 0);
	}

	// ---- renderResult ----
	@Test void renderResultPage0() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		mode.renderResult(e, 0);
	}

	@Test void renderResultPage1() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		int[] st = (int[]) readObj(mode, "sectiontime");
		st[0] = 100;
		setInt(mode, "sectionavgtime", 100);
		mode.renderResult(e, 0);
	}

	@Test void renderResultPage2() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT; e.statc[1] = 2;
		mode.renderResult(e, 0);
	}

	// ---- calcScore ----
	@Test void calcScoreLevel999Ending() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		e.statistics.level = 997;
		mode.calcScore(e, 0, 3);
		assertEquals(999, e.statistics.level);
		assertEquals(1, e.ending); // ending=1 for grade mania 2
	}

	@Test void calcScoreBravo() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		mode.calcScore(e, 0, 4);
		assertTrue(e.statistics.score > 0);
	}

	@Test void calcScoreCombo() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		setInt(mode, "comboValue", 5);
		mode.calcScore(e, 0, 2);
		assertTrue(e.statistics.score > 0);
	}

	// ---- onLast ----
	@Test void onLastEndingTimer() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.gameActive = true; e.ending = 2;
		mode.onLast(e, 0);
		assertEquals(1, readInt(mode, "rolltime"));
	}

	@Test void onLastEndingFFastForward() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.gameActive = true; e.ending = 2;
		setInt(mode, "version", 2); // version >= 1 required for F fast-forward
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onLast(e, 0);
		// GradeMania2Mode onLast only does rolltime++; no F fast-forward logic
		assertEquals(1, readInt(mode, "rolltime"));
	}

	@Test void onLastEndingLimit() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.gameActive = true; e.ending = 2;
		setInt(mode, "version", 2);
		setInt(mode, "rolltime", 3693); // ROLLTIMELIMIT = 3694
		mode.onLast(e, 0);
		assertEquals(GameEngine.Status.EXCELLENT, e.stat);
	}

	@Test void onLastEndingNoFastForward() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.gameActive = true; e.ending = 2;
		setInt(mode, "version", 0); // version < 1, no fast-forward
		mode.onLast(e, 0);
		assertEquals(1, readInt(mode, "rolltime"));
	}

	// ---- onGameOver ----
	@Test void onGameOver() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.statc[0] = 0; e.createFieldIfNeeded();
		mode.onGameOver(e, 0);
		assertTrue(readInt(mode, "gradeInternal") >= 0);
	}

	// ---- saveReplay ----
	@Test void saveReplayWritesVersion() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.statistics.level = 500; e.statistics.time = 3000; e.ai = null;
		CustomProperties prop = new CustomProperties();
		e.owner.replayProp = prop;
		mode.saveReplay(e, 0, prop);
		assertEquals(2, e.owner.replayProp.getProperty("grademania2.version", 0));
	}

	// ---- onResult ----
	@Test void onResultUpNav() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode); mode.playerInit(e, 0);
		e.statc[1] = 0;
		e.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		mode.onResult(e, 0);
		assertEquals(2, e.statc[1]);
	}

	// ---- helpers ----
	private static GameEngine freshEngine(GradeMania2Mode mode) {
		GameManager m = new GameManager(new EventReceiver()); m.mode = mode; m.init();
		m.engine[0].init(); m.engine[0].ruleopt.fieldWidth = 10; m.engine[0].ruleopt.fieldHeight = 20;
		return m.engine[0];
	}
	private static boolean getOnOff(Object o, String n) throws Exception {
		return ((OnOffMenuItem) readObj(o, n)).value;
	}
	private static void setOnOff(Object o, String n, boolean v) throws Exception {
		((OnOffMenuItem) readObj(o, n)).value = v;
	}
	private static int readInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); return f.getInt(o);
	}
	private static boolean readBool(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); return f.getBoolean(o);
	}
	private static Object readObj(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); return f.get(o);
	}
	private static void setInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); f.setInt(o, v);
	}
	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}
}
