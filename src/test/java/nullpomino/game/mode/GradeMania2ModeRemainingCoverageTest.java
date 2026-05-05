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
 * Targets remaining uncovered lines in GradeMania2Mode.java:
 * 518,520-523,526,528,530,533-536,538-539,553 (renderLast best section time & score)
 * 637,640-641,643-645,648-651 (onMove RE medal v1),
 * 679-681,747-749,810-812,816-817,834-842,846-847,849-850,852-853 (calcScore medals),
 * 887,899-901,922,941,995-998,1000-1001,1055-1056,1097-1099,1103-1104 (various).
 */
class GradeMania2ModeRemainingCoverageTest {

	@Test
	void renderLastBestSectionTime() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		setBoolean(mode, "isShowBestSectionTime", true);
		int[] best = (int[]) readField(mode, "bestSectionTime");
		for (int i = 0; i < best.length; i++) best[i] = (i + 1) * 500;

		mode.renderLast(e, 0);
	}

	@Test
	void renderLastSectionTimeDisplay() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
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
	void renderLastScoreWithLastscore() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.statistics.score = 50000;
		setInt(mode, "lastscore", 2500);
		setInt(mode, "scgettime", 60);

		mode.renderLast(e, 0);
	}

	@Test
	void onMoveREMetalV1() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.statc[0] = 0;
		e.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		setInt(mode, "version", 1);
		e.timerActive = true;
		setInt(mode, "medalRE", 0);
		e.createFieldIfNeeded();
		setBoolean(mode, "recoveryFlag", true);
		// Put <= 70 blocks
		int w = e.field.getWidth();
		for (int x = 0; x < w; x++)
			e.field.setBlock(x, 0, new Block(Block.BLOCK_COLOR_RED));

		mode.onMove(e, 0);

		assertEquals(1, readInt(mode, "medalRE"));
	}

	@Test
	void calcScoreSKMedalNonBig() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		// Set big mode via the public value field on the menu item
		// big is false by default (non-big mode)
		e.statistics.totalFour = 10; // {10, 20, 35} triggers medalSK++ in non-big mode
		// Set level
		e.statistics.level = 0;

		mode.calcScore(e, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	@Test
	void calcScoreCOMedalBigMode() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		((OnOffMenuItem) readField(mode, "big")).value = true;
		e.combo = 4;

		mode.calcScore(e, 0, 2);

		assertEquals(1, readInt(mode, "medalCO"));
	}

	@Test
	void calcScoreCOMedalNonBig() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		e.combo = 7;

		mode.calcScore(e, 0, 2);

		assertEquals(1, readInt(mode, "medalCO"));
	}

	@Test
	void calcScoreMrollSectionTimeCheck() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		e.statistics.level = 995;
		setInt(mode, "nextseclv", 999);
		setInt(mode, "grade", 17);
		e.statistics.time = 20000; // <= M_ROLL_TIME_REQUIRE
		int[] sectime = (int[]) readField(mode, "sectiontime");
		// Set section 9 so sectionlasttime is set when level crosses boundary
		// levelb=995, 995/100=9
		sectime[9] = 100;
		int[] fourline = (int[]) readField(mode, "sectionfourline");
		fourline[9] = 2;

		mode.calcScore(e, 0, 4);

		assertTrue(readBoolean(mode, "mrollFlag"));
	}

	@Test
	void onLastMrollRollClear3() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.gameActive = true;
		e.ending = 2;
		setBoolean(mode, "mrollFlag", true);
		setInt(mode, "rolltime", 3693); // just before ROLLTIMELIMIT
		setInt(mode, "grade", 18);
		setInt(mode, "mrollLines", 10);

		mode.onLast(e, 0);

		assertEquals(3, readInt(mode, "rollclear"));
	}

	@Test
	void onLastMrollRollClear4() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.gameActive = true;
		e.ending = 2;
		setBoolean(mode, "mrollFlag", true);
		setInt(mode, "rolltime", 3693);
		setInt(mode, "mrollLines", 35); // >= 32

		mode.onLast(e, 0);

		assertEquals(4, readInt(mode, "rollclear"));
	}

	@Test
	void onGameOverMroll() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBoolean(mode, "mrollFlag", true);
		setInt(mode, "grade", 16); // < 18
		e.ending = 2;
		e.statc[0] = 0;
		e.createFieldIfNeeded();

		mode.onGameOver(e, 0);

		assertEquals(18, readInt(mode, "grade"));
	}

	@Test
	void renderResultSecretGrade() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		e.statc[1] = 0;
		setInt(mode, "secretGrade", 10);
		setInt(mode, "rollclear", 1);

		mode.renderResult(e, 0);
	}

	@Test
	void onResultFButton() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;

		assertFalse(readBoolean(mode, "isShowBestSectionTime"));
		mode.onResult(e, 0);
		assertTrue(readBoolean(mode, "isShowBestSectionTime"));
	}

	@Test
	void onResultDownButton() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statc[1] = 0;
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;

		mode.onResult(e, 0);

		assertEquals(1, e.statc[1]);
	}

	@Test
	void calcScoreVanishRollLineClear() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 2;
		setBoolean(mode, "mrollFlag", true);
		// lines >= 1 and mrollFlag and ending == 2
		mode.calcScore(e, 0, 3);

		assertEquals(3, readInt(mode, "mrollLines"));
	}

	@Test
	void renderResultPage1Section() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		e.statc[1] = 1;
		int[] sectime = (int[]) readField(mode, "sectiontime");
		sectime[0] = 200;
		setInt(mode, "sectionavgtime", 200);

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultPage2Medals() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		e.statc[1] = 2;
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 3);
		setInt(mode, "medalRE", 1);
		setInt(mode, "medalRO", 1);
		setInt(mode, "medalCO", 1);

		mode.renderResult(e, 0);
	}

	// --- helpers ---

	private static GameEngine freshEngine(GradeMania2Mode mode) {
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
