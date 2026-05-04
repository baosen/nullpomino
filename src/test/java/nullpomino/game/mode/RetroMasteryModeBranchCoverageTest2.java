package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Additional branch coverage for {@link RetroMasteryMode}: covers the
 * replay-mode {@link #playerInit} path, {@link #onSetting} startlevel
 * change and quit, and {@link #renderLast} HUD branches (score with/without
 * pending lastscore, lines, level, time).
 */
class RetroMasteryModeBranchCoverageTest2 {

	@Test
	void playerInitReplayModeLoadsReplayProp() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		e.owner.replayMode = true;
		m.playerInit(e, 0);
		// version is not updated to CURRENT_VERSION in this branch
		assertEquals(0, getInt(m, "version"));
	}

	@Test
	void onSettingChangeStartlevelRight() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 1);
		setInt(m, "startlevel", 5);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		m.onSetting(e, 0);
		assertEquals(6, getInt(m, "startlevel"));
	}

	@Test
	void onSettingChangeStartlevelLeftBelowZero() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 1);
		setInt(m, "startlevel", 0);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		m.onSetting(e, 0);
		assertEquals(19, getInt(m, "startlevel")); // wraps to max
	}

	@Test
	void onSettingChangeStartlevelAboveMax() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 1);
		setInt(m, "startlevel", 19);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		m.onSetting(e, 0);
		assertEquals(0, getInt(m, "startlevel")); // wraps to min
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
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
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 5000;
		e.statistics.lines = 10;
		e.statistics.level = 3;
		e.statistics.time = 4500;
		setInt(m, "gametype", 0);
		setInt(m, "loons", 10);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 0);
		m.renderLast(e, 0);
		// Hits HUD else branch with score-only display
	}

	@Test
	void renderLastHudScoreWithPendingLastscore() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 5000;
		e.statistics.lines = 10;
		e.statistics.level = 3;
		e.statistics.time = 4500;
		setInt(m, "gametype", 0);
		setInt(m, "loons", 10);
		setInt(m, "lastscore", 400);
		setInt(m, "scgettime", 50);
		m.renderLast(e, 0);
		// Hits else branch: score(+lastscore) display
	}

	@Test
	void renderLastHudExpiredLastscore() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 5000;
		e.statistics.lines = 10;
		e.statistics.level = 3;
		e.statistics.time = 4500;
		setInt(m, "gametype", 0);
		setInt(m, "loons", 10);
		setInt(m, "lastscore", 400);
		setInt(m, "scgettime", 120); // >= 120 so falls back to plain score
		m.renderLast(e, 0);
		// Hits score-only via scgettime >= 120
	}

	@Test
	void renderLastHudAllFields() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 100;
		e.statistics.lines = 10;
		e.statistics.level = 5;
		e.statistics.time = 60000;
		setInt(m, "gametype", 0);
		setInt(m, "loons", 8);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		m.renderLast(e, 0);
		// Hits LINES, LEVEL, TIME rendering
	}

	// ---- helpers ----

	private static GameEngine fresh(RetroMasteryMode m) {
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
