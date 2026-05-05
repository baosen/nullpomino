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

import org.junit.jupiter.api.Test;

/**
 * Targets remaining uncovered lines in FinalMode.java:
 * 220-221 (replay playerInit), 459,461-480 (best section time render),
 * 495 (score with lastscore), 577 (onMove lvupflag), 599-604 (onARE),
 * 644-646 (SK medal big), 668-676 (CO medal big), 680-687 (CO medal non-big),
 * 736-737 (grade at 500), 744 (levelstop), 828-833 (renderResult grade colors),
 * 840-841 (secret grade), 856-863 (result medals), 880-882 (onResult down),
 * 886-887 (onResult F).
 */
class FinalModeRemainingCoverageTest {

	@Test
	void playerInitReplayPath() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		e.owner.replayProp.setProperty("final.startlevel", "5");
		e.owner.replayProp.setProperty("final.lvstopse", "true");
		e.owner.replayProp.setProperty("final.version", "2");

		mode.playerInit(e, 0);

		assertEquals(5, readInt(mode, "startlevel"));
		assertTrue(readBoolean(mode, "lvstopse"));
		assertEquals(2, readInt(mode, "version"));
	}

	@Test
	void renderLastBestSectionTime() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;
		e.owner.replayMode = false;
		setBoolean(mode, "isShowBestSectionTime", true);
		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);
		// Set some best section times
		int[] best = (int[]) readField(mode, "bestSectionTime");
		for (int i = 0; i < best.length; i++) best[i] = (i + 1) * 100;
		boolean[] newRec = (boolean[]) readField(mode, "sectionIsNewRecord");
		newRec[1] = true;

		mode.renderLast(e, 0);
		// Just verify it doesn't throw
	}

	@Test
	void renderLastScoreWithLastscore() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.statistics.score = 50000;
		setInt(mode, "lastscore", 2500);
		setInt(mode, "scgettime", 60);

		mode.renderLast(e, 0);
		// Should render with (+2500)
	}

	@Test
	void onMoveLvupflagFalse() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.statc[0] = 1; // > 0
		e.holdDisable = false;
		setBoolean(mode, "lvupflag", true);

		mode.onMove(e, 0);

		assertFalse(readBoolean(mode, "lvupflag"));
	}

	@Test
	void onARELastFrame() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.statc[0] = 5;
		e.statc[1] = 5; // last frame: statc[0] >= statc[1] - 1
		setBoolean(mode, "lvupflag", false);
		setInt(mode, "nextseclv", 300);
		e.statistics.level = 298;

		mode.onARE(e, 0);

		assertEquals(299, e.statistics.level);
		assertTrue(readBoolean(mode, "lvupflag"));
	}

	@Test
	void calcScoreSKMedalBigMode() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setBoolean(mode, "big", true);
		e.statistics.totalFour = 1; // triggers SK medal

		mode.calcScore(e, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	@Test
	void calcScoreSKMedalBigModeTotalFour2() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setBoolean(mode, "big", true);
		e.statistics.totalFour = 2;

		mode.calcScore(e, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	@Test
	void calcScoreSKMedalBigModeTotalFour4() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setBoolean(mode, "big", true);
		e.statistics.totalFour = 4;

		mode.calcScore(e, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	@Test
	void calcScoreCOMedalBigMode() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setBoolean(mode, "big", true);
		e.combo = 4; // triggers first else-if: combo >= 2 && medalCO < 1

		mode.calcScore(e, 0, 2);

		assertEquals(1, readInt(mode, "medalCO"));
	}

	@Test
	void calcScoreCOMedalNonBig() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setBoolean(mode, "big", false);
		e.combo = 7; // triggers first else-if: combo >= 4 && medalCO < 1

		mode.calcScore(e, 0, 2);

		assertEquals(1, readInt(mode, "medalCO"));
	}

	@Test
	void calcScoreGradeAt500() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(mode, "nextseclv", 500);
		e.statistics.level = 400;
		// Set a section time so sectionlasttime won't be 0
		int[] sectime = (int[]) readField(mode, "sectiontime");
		sectime[4] = 200;

		mode.calcScore(e, 0, 100);

		assertEquals(2, readInt(mode, "grade")); // grade = 2 at nextseclv == 500
	}

	@Test
	void calcScoreLevelstop() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setBoolean(mode, "lvstopse", true);
		// Set level so after adding lines it equals nextseclv - 1
		setInt(mode, "nextseclv", 300);
		e.statistics.level = 299 - 2; // add 2 lines will reach 299 = nextseclv - 1

		mode.calcScore(e, 0, 2);

		assertEquals(299, e.statistics.level);
	}

	@Test
	void renderResultGradeColors() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		e.statc[1] = 0;
		setInt(mode, "grade", 2);
		setInt(mode, "rollclear", 1);
		setInt(mode, "rankingRank", 0);
		// secretGrade < 5 so secret line not drawn

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultSecretGrade() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		e.statc[1] = 0;
		setInt(mode, "grade", 0);
		setInt(mode, "secretGrade", 10);

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultPage2Medals() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		e.statc[1] = 2;
		setInt(mode, "medalAC", 3);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 1);
		setInt(mode, "medalCO", 1);

		mode.renderResult(e, 0);
	}

	@Test
	void onResultDownButton() throws Exception {
		FinalMode mode = new FinalMode();
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
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statc[1] = 0;
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;

		assertFalse(readBoolean(mode, "isShowBestSectionTime"));
		mode.onResult(e, 0);
		assertTrue(readBoolean(mode, "isShowBestSectionTime"));
	}

	@Test
	void renderLastEndingRollTimeDisplay() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.gameActive = true;
		e.ending = 2;
		setInt(mode, "rolltime", 100);
		setInt(mode, "version", 3);

		mode.renderLast(e, 0);
	}

	// --- helpers ---

	private static GameEngine freshEngine(FinalMode mode) {
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
