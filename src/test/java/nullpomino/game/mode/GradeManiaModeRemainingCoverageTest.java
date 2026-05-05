package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import nullpomino.game.menu.OnOffMenuItem;

import org.junit.jupiter.api.Test;

/**
 * Targets remaining uncovered lines in GradeManiaMode.java:
 * 317-318 (onSetting F button), 333 (onSetting B/cancel), 363 (startGame bgmlv),
 * 397,399-402,405,407,409,412-415,417-418 (renderLast section time display),
 * 453-456 (renderLast roll time), 461-472,475,477,481-483 (section time rendering),
 * 626,628,630-633 (calcScore GM conditions), 641-643,649 (BGM fadeout),
 * 721-722,725,727-729,733-735 (renderResult pages), 759-761,764-766,770-771 (onResult).
 */
class GradeManiaModeRemainingCoverageTest {

	@Test
	void onSettingFButton() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;

		mode.onSetting(e, 0);

		assertTrue(readBoolean(mode, "isShowBestSectionTime"));
	}

	@Test
	void onSettingBPressCancel() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_B] = true;

		mode.onSetting(e, 0);

		assertTrue(e.quitflag);
	}

	@Test
	void startGameBgmlv() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		// Set startlevel so level >= 500 => bgmlv = 1 (value is a public Integer field on IntegerMenuItem)
		((nullpomino.game.menu.IntegerMenuItem) readField(mode, "startlevel")).value = 5;

		mode.startGame(e, 0);

		assertEquals(500, e.statistics.level);
	}

	@Test
	void renderLastRollTimeDisplay() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.gameActive = true;
		e.ending = 2;
		setInt(mode, "rolltime", 100);

		mode.renderLast(e, 0);
	}

	@Test
	void renderLastSectionTimeDisplay() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		((OnOffMenuItem) readField(mode, "showsectiontime")).value = true;
		int[] sectime = (int[]) readField(mode, "sectiontime");
		sectime[0] = 100;
		setInt(mode, "sectionavgtime", 200);

		mode.renderLast(e, 0);
	}

	@Test
	void renderLastBestSectionTimeView() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		setBoolean(mode, "isShowBestSectionTime", true);
		int[] best = (int[]) readField(mode, "bestSectionTime");
		for (int i = 0; i < best.length; i++) best[i] = (i + 1) * 500;

		mode.renderLast(e, 0);
	}

	@Test
	void calcScoreGmConditions() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(mode, "version", 1);
		setInt(mode, "grade", 12); // >= GM_500_GRADE_REQUIRE
		e.statistics.score = 100000;
		e.statistics.time = 10000; // within time requirements
		// Level up to cross section boundaries
		e.statistics.level = 250;
		setInt(mode, "nextseclv", 300);

		mode.calcScore(e, 0, 50);

		assertTrue(readBoolean(mode, "gm300"));
	}

	@Test
	void calcScoreGm500Condition() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(mode, "version", 1);
		setInt(mode, "grade", 12);
		e.statistics.score = 100000;
		e.statistics.time = 20000;
		e.statistics.level = 450;
		setInt(mode, "nextseclv", 500);

		mode.calcScore(e, 0, 50);

		assertTrue(readBoolean(mode, "gm500"));
	}

	@Test
	void calcScoreBgmFadeout() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		// At level 490+, bgmlv=0 triggers fadeout
		setInt(mode, "bgmlv", 0);
		e.statistics.level = 490;
		setInt(mode, "nextseclv", 600);

		mode.calcScore(e, 0, 2);
	}

	@Test
	void renderResultPage1() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		e.statc[1] = 1;
		int[] sectime = (int[]) readField(mode, "sectiontime");
		sectime[0] = 100;
		setInt(mode, "sectionavgtime", 100);

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultPage2() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		e.statc[1] = 2;
		setInt(mode, "grade", 18);
		e.statistics.time = 30000;

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultSecretGrade() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		e.statc[1] = 0;
		setInt(mode, "secretGrade", 10);

		mode.renderResult(e, 0);
	}

	@Test
	void onResultDownButton() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statc[1] = 0;
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;

		mode.onResult(e, 0);

		assertEquals(1, e.statc[1]);
	}

	@Test
	void onResultFButton() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;

		assertFalse(readBoolean(mode, "isShowBestSectionTime"));
		mode.onResult(e, 0);
		assertTrue(readBoolean(mode, "isShowBestSectionTime"));
	}

	@Test
	void calcScoreLevel999EndingGM() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		e.statistics.score = 200000; // >= tableGradeScore[17]
		e.statistics.time = 40000; // <= GM_999_TIME_REQUIRE
		setBoolean(mode, "gm300", true);
		setBoolean(mode, "gm500", true);
		e.statistics.level = 990;
		setInt(mode, "nextseclv", 999);

		mode.calcScore(e, 0, 10);

		assertEquals(2, e.ending);
	}

	@Test
	void calcScoreVersion0GmConditions() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(mode, "version", 0);
		setInt(mode, "grade", 12);
		e.statistics.score = 100000;
		e.statistics.time = 20000;
		e.statistics.level = 250;
		setInt(mode, "nextseclv", 300);

		mode.calcScore(e, 0, 50);

		assertTrue(readBoolean(mode, "gm300"));
	}

	// --- helpers ---

	private static GameEngine freshEngine(GradeManiaMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		return m.engine[0];
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
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}
}
