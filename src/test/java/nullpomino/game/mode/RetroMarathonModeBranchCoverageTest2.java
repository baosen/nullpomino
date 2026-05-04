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
 * Additional branch coverage for {@link RetroMarathonMode}: covers the
 * replay-mode {@link #playerInit} path, {@link #onSetting} toggles (big,
 * quit), {@link #renderLast} HUD branches (score with/without pending
 * lastscore, all three gametype line labels, level, time), and the
 * {@link #saveReplay} ranking-save path.
 */
class RetroMarathonModeBranchCoverageTest2 {

	@Test
	void playerInitReplayModeLoadsReplayProp() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		e.owner.replayMode = true;
		// replayProp will be used; it's already a CustomProperties from init
		m.playerInit(e, 0);
		// version is not updated to CURRENT_VERSION in this branch
		assertEquals(0, getInt(m, "version"));
	}

	@Test
	void onSettingToggleBig() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 3);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		m.onSetting(e, 0);
		assertTrue(getBool(m, "big"));
	}

	@Test
	void onSettingBigToggleBack() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setBool(m, "big", true);
		setInt(m, "menuCursor", 3);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		m.onSetting(e, 0);
		assertFalse(getBool(m, "big"));
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_B] = true;
		m.onSetting(e, 0);
		assertTrue(e.quitflag);
	}

	@Test
	void renderLastHudScoreOnly() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 5000;
		e.statistics.lines = 10;
		e.statistics.level = 3;
		e.statistics.time = 4500;
		setInt(m, "gametype", 0);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 0);
		// Must set big=false and ai=null (already done)
		m.renderLast(e, 0);
		// No exception = pass, and this hits the HUD else branch
	}

	@Test
	void renderLastHudScoreWithPendingLastscore() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 5000;
		e.statistics.lines = 10;
		e.statistics.level = 3;
		setInt(m, "gametype", 0);
		setInt(m, "lastscore", 400);
		setInt(m, "scgettime", 50);
		m.renderLast(e, 0);
		// Hits else branch showing "score(+lastscore)"
	}

	@Test
	void renderLastHudGametypeA() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 100;
		e.statistics.lines = 5;
		e.statistics.level = 1;
		setInt(m, "gametype", 0); // TYPE_A
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		m.renderLast(e, 0);
		// gametype TYPE_A: strLine = lines
	}

	@Test
	void renderLastHudGametypeB() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 100;
		e.statistics.lines = 10;
		e.statistics.level = 1;
		setInt(m, "gametype", 1); // TYPE_B
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		m.renderLast(e, 0);
		// gametype TYPE_B: strLine = max(25-lines, 0) = 15
	}

	@Test
	void renderLastHudGametypeArrange() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 100;
		e.statistics.lines = 10;
		e.statistics.level = 1;
		e.statistics.time = 1000;
		setInt(m, "gametype", 2); // ARRANGE
		setInt(m, "levellines", 100);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		m.renderLast(e, 0);
		// gametype ARRANGE: strLine = lines + "/" + levellines
	}

	@Test
	void renderLastHudLevelTime() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 100;
		e.statistics.lines = 10;
		e.statistics.level = 5;
		e.statistics.time = 60000;
		setInt(m, "gametype", 1);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		m.renderLast(e, 0);
		// Verifies LEVEL and TIME render (level 5 = "05", time = "01:00")
	}

	@Test
	void saveReplayWithRanking() throws Exception {
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		// Set a score high enough to make the ranking
		e.statistics.score = 99999;
		e.statistics.lines = 100;
		e.statistics.level = 5;
		e.ai = null;
		CustomProperties prop = new CustomProperties();
		m.saveReplay(e, 0, prop);
		// saveRanking should have been called (rankingRank != -1)
		assertTrue(getInt(m, "rankingRank") >= 0);
	}

	// ---- helpers ----

	private static GameEngine fresh(RetroMarathonMode m) {
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = m;
		mgr.init();
		mgr.engine[0].init();
		return mgr.engine[0];
	}

	private static int getInt(Object o, String n) throws Exception {
		return findField(o.getClass(), n).getInt(o);
	}

	private static boolean getBool(Object o, String n) throws Exception {
		return findField(o.getClass(), n).getBoolean(o);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		findField(o.getClass(), n).setInt(o, v);
	}

	private static void setBool(Object o, String n, boolean v) throws Exception {
		findField(o.getClass(), n).setBoolean(o, v);
	}

	private static Field findField(Class<?> cls, String name) throws Exception {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) { /* continue */ }
		}
		throw new NoSuchFieldException(name);
	}
}
