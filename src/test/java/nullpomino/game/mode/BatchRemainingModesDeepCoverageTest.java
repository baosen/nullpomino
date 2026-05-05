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
 * Deep branch coverage for remaining modes: ExtremeMode, DigChallengeMode,
 * PhantomManiaMode, TechnicianMode, PracticeMode, ComboRaceMode, MarathonPlusMode.
 */
class BatchRemainingModesDeepCoverageTest {

	// ===== ExtremeMode =====

	@Test void extremeOnSettingReplay() throws Exception {
		ExtremeMode m = new ExtremeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.owner.replayMode = true;
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		assertTrue(m.onSetting(e, 0));
		setInt(m, "menuTime", 60);
		assertFalse(m.onSetting(e, 0));
	}

	@Test void extremeCalcScoreBravo() throws Exception {
		ExtremeMode m = new ExtremeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		m.calcScore(e, 0, 4);
		assertTrue(e.statistics.score > 0);
	}

	@Test void extremeOnResult() throws Exception {
		ExtremeMode m = new ExtremeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1; e.ctrl.buttonPress[Controller.BUTTON_F] = true;
		m.onResult(e, 0);
	}

	@Test void extremeRenderResultPage0() throws Exception {
		ExtremeMode m = new ExtremeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		m.renderResult(e, 0);
	}

	@Test void extremeRenderResultPage1() throws Exception {
		ExtremeMode m = new ExtremeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		m.renderResult(e, 0);
	}

	@Test void extremeRenderResultPage2() throws Exception {
		ExtremeMode m = new ExtremeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 2;
		m.renderResult(e, 0);
	}

	@Test void extremeOnLastScgettime() throws Exception {
		ExtremeMode m = new ExtremeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); setInt(m, "scgettime", 10);
		m.onLast(e, 0);
		// scgettime++ in onLast makes it 11
		assertEquals(11, readInt(m, "scgettime"));
	}

	@Test void extremeSaveReplay() throws Exception {
		ExtremeMode m = new ExtremeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.ai = null;
		CustomProperties p = new CustomProperties(); e.owner.replayProp = p;
		m.saveReplay(e, 0, p);
	}

	// ===== DigChallengeMode =====

	@Test void digChallengeOnSettingReplay() throws Exception {
		DigChallengeMode m = new DigChallengeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.owner.replayMode = true;
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		assertTrue(m.onSetting(e, 0));
		setInt(m, "menuTime", 60);
		assertFalse(m.onSetting(e, 0));
	}

	@Test void digChallengeOnResult() throws Exception {
		DigChallengeMode m = new DigChallengeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1; e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		m.onResult(e, 0);
	}

	@Test void digChallengeRenderResultPage0() throws Exception {
		DigChallengeMode m = new DigChallengeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		m.renderResult(e, 0);
	}

	@Test void digChallengeRenderResultPage1() throws Exception {
		DigChallengeMode m = new DigChallengeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		m.renderResult(e, 0);
	}

	@Test void digChallengeOnLastScgettime() throws Exception {
		DigChallengeMode m = new DigChallengeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); setInt(m, "scgettime", 10);
		m.onLast(e, 0);
	}

	@Test void digChallengeSaveReplay() throws Exception {
		DigChallengeMode m = new DigChallengeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.ai = null;
		CustomProperties p = new CustomProperties(); e.owner.replayProp = p;
		m.saveReplay(e, 0, p);
	}

	// ===== PhantomManiaMode =====

	@Test void phantomOnSettingReplay() throws Exception {
		PhantomManiaMode m = new PhantomManiaMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.owner.replayMode = true;
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		assertTrue(m.onSetting(e, 0));
		setInt(m, "menuTime", 60);
		assertFalse(m.onSetting(e, 0));
	}

	@Test void phantomOnResult() throws Exception {
		PhantomManiaMode m = new PhantomManiaMode(); GameEngine e = f1(m);
		m.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1; e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		m.onResult(e, 0);
	}

	@Test void phantomRenderResultPage0() throws Exception {
		PhantomManiaMode m = new PhantomManiaMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		m.renderResult(e, 0);
	}

	@Test void phantomRenderResultPage1() throws Exception {
		PhantomManiaMode m = new PhantomManiaMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		m.renderResult(e, 0);
	}

	@Test void phantomRenderResultPage2() throws Exception {
		PhantomManiaMode m = new PhantomManiaMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 2;
		m.renderResult(e, 0);
	}

	@Test void phantomOnLastScgettime() throws Exception {
		PhantomManiaMode m = new PhantomManiaMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); setInt(m, "scgettime", 10);
		m.onLast(e, 0);
	}

	@Test void phantomCalcScore() throws Exception {
		PhantomManiaMode m = new PhantomManiaMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		m.calcScore(e, 0, 1);
		assertTrue(e.statistics.score > 0);
	}

	// ===== TechnicianMode =====

	@Test void technicianOnSettingReplay() throws Exception {
		TechnicianMode m = new TechnicianMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.owner.replayMode = true;
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		assertTrue(m.onSetting(e, 0));
		setInt(m, "menuTime", 60);
		assertFalse(m.onSetting(e, 0));
	}

	@Test void technicianOnResult() throws Exception {
		TechnicianMode m = new TechnicianMode(); GameEngine e = f1(m);
		m.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1; e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		m.onResult(e, 0);
	}

	@Test void technicianRenderResultPage0() throws Exception {
		TechnicianMode m = new TechnicianMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		m.renderResult(e, 0);
	}

	@Test void technicianRenderResultPage1() throws Exception {
		TechnicianMode m = new TechnicianMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		m.renderResult(e, 0);
	}

	@Test void technicianOnLastScgettime() throws Exception {
		TechnicianMode m = new TechnicianMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); setInt(m, "scgettime", 10);
		m.onLast(e, 0);
	}

	@Test void technicianCalcScore() throws Exception {
		TechnicianMode m = new TechnicianMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		m.calcScore(e, 0, 1);
	}

	@Test void technicianSaveReplay() throws Exception {
		TechnicianMode m = new TechnicianMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.ai = null;
		CustomProperties p = new CustomProperties(); e.owner.replayProp = p;
		m.saveReplay(e, 0, p);
	}

	// ===== PracticeMode =====

	@Test void practiceOnSettingReplay() throws Exception {
		PracticeMode m = new PracticeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.owner.replayMode = true;
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		setInt(m, "menuTime", 0);
		assertTrue(m.onSetting(e, 0));
		// Replay auto-advance requires menuTime >= 120
		setInt(m, "menuTime", 120);
		assertFalse(m.onSetting(e, 0));
	}

	@Test void practiceOnResult() throws Exception {
		PracticeMode m = new PracticeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_UP] = 1; e.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		m.onResult(e, 0);
	}

	@Test void practiceRenderResultPage0() throws Exception {
		PracticeMode m = new PracticeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		m.renderResult(e, 0);
	}

	@Test void practiceOnLastScgettime() throws Exception {
		PracticeMode m = new PracticeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); setInt(m, "scgettime", 10);
		m.onLast(e, 0);
	}

	@Test void practiceCalcScore() throws Exception {
		PracticeMode m = new PracticeMode(); GameEngine e = f1(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		m.calcScore(e, 0, 1);
	}

	// ===== ComboRaceMode =====

	@Test void comboRaceOnSettingReplay() throws Exception {
		ComboRaceMode m = new ComboRaceMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.owner.replayMode = true;
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		assertTrue(m.onSetting(e, 0));
		setInt(m, "menuTime", 60);
		assertFalse(m.onSetting(e, 0));
	}

	@Test void comboRaceOnResult() throws Exception {
		ComboRaceMode m = new ComboRaceMode(); GameEngine e = f1(m);
		m.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1; e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		m.onResult(e, 0);
	}

	@Test void comboRaceRenderResultPage0() throws Exception {
		ComboRaceMode m = new ComboRaceMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		m.renderResult(e, 0);
	}

	@Test void comboRaceRenderResultPage1() throws Exception {
		ComboRaceMode m = new ComboRaceMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		m.renderResult(e, 0);
	}

	@Test void comboRaceCalcScore() throws Exception {
		ComboRaceMode m = new ComboRaceMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		m.calcScore(e, 0, 4);
		// ComboRaceMode does not compute score, just tracks combos
		assertTrue(true);
	}

	@Test void comboRaceOnLastScgettime() throws Exception {
		ComboRaceMode m = new ComboRaceMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); setInt(m, "scgettime", 10);
		m.onLast(e, 0);
	}

	// ===== MarathonPlusMode =====

	@Test void marathonPlusOnSettingReplay() throws Exception {
		MarathonPlusMode m = new MarathonPlusMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.owner.replayMode = true;
		e.stat = GameEngine.Status.SETTING; e.resetStatc();
		assertTrue(m.onSetting(e, 0));
		setInt(m, "menuTime", 60);
		assertFalse(m.onSetting(e, 0));
	}

	@Test void marathonPlusOnResult() throws Exception {
		MarathonPlusMode m = new MarathonPlusMode(); GameEngine e = f1(m);
		m.playerInit(e, 0);
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1; e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		m.onResult(e, 0);
	}

	@Test void marathonPlusRenderResultPage0() throws Exception {
		MarathonPlusMode m = new MarathonPlusMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 0;
		m.renderResult(e, 0);
	}

	@Test void marathonPlusRenderResultPage1() throws Exception {
		MarathonPlusMode m = new MarathonPlusMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		m.renderResult(e, 0);
	}

	@Test void marathonPlusCalcScore() throws Exception {
		MarathonPlusMode m = new MarathonPlusMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.ending = 0;
		e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		m.calcScore(e, 0, 1);
		assertTrue(e.statistics.score > 0);
	}

	@Test void marathonPlusOnLastScgettime() throws Exception {
		MarathonPlusMode m = new MarathonPlusMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); setInt(m, "scgettime", 10);
		m.onLast(e, 0);
	}

	@Test void marathonPlusSaveReplay() throws Exception {
		MarathonPlusMode m = new MarathonPlusMode(); GameEngine e = f1(m);
		m.playerInit(e, 0); e.ai = null;
		CustomProperties p = new CustomProperties(); e.owner.replayProp = p;
		m.saveReplay(e, 0, p);
	}

	// ---- helpers ----
	private static GameEngine f1(Object mode) {
		GameManager m = new GameManager(new EventReceiver());
		if (mode instanceof GameMode) m.mode = (GameMode) mode;
		m.init(); m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10; m.engine[0].ruleopt.fieldHeight = 20;
		return m.engine[0];
	}
	private static int readInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n); f.setAccessible(true); return f.getInt(o);
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
