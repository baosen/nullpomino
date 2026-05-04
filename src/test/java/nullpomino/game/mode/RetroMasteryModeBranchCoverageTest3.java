package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Targeted branch coverage for {@link RetroMasteryMode}.
 * Covers lines: 161 (playerInit replay mode), 223-227 (startlevel change),
 * 244 (B-button quit), 325-344 (renderLast HUD else branch with
 * score with/without lastscore, LINES, LEVEL, TIME).
 */
class RetroMasteryModeBranchCoverageTest3 {

	@Test
	void playerInitReplayModeLoadsReplayProp() throws Exception {
		// Line 161: replayMode -> loadSetting(owner.replayProp)
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		e.owner.replayMode = true;
		m.playerInit(e, 0);
		// version is not set to CURRENT_VERSION in this path (remains 0)
		assertEquals(0, getInt(m, "version"));
	}

	@Test
	void onSettingChangeStartlevelRight() throws Exception {
		// Lines 223-227: menuCursor=1, change=1, startlevel=5 -> 6
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 1);
		setInt(m, "startlevel", 5);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		m.onSetting(e, 0);
		assertEquals(6, getInt(m, "startlevel"));
	}

	@Test
	void onSettingChangeStartlevelLeftWrap() throws Exception {
		// Lines 223-227: menuCursor=1, change=-1, startlevel=0 -> wraps to 19
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 1);
		setInt(m, "startlevel", 0);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		m.onSetting(e, 0);
		assertEquals(19, getInt(m, "startlevel"));
	}

	@Test
	void onSettingChangeStartlevelRightWrap() throws Exception {
		// Lines 223-227: menuCursor=1, change=1, startlevel=19 -> wraps to 0
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 1);
		setInt(m, "startlevel", 19);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		m.onSetting(e, 0);
		assertEquals(0, getInt(m, "startlevel"));
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		// Line 244: isPush(BUTTON_B) -> quitflag = true
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		m.onSetting(e, 0);
		assertTrue(e.quitflag);
	}

	@Test
	void renderLastHudScoreOnly() throws Exception {
		// Lines 325-344: else branch, lastscore==0 -> plain score
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY; // not SETTING/RESULT -> else
		e.statistics.score = 5000;
		e.statistics.lines = 10;
		e.statistics.level = 3;
		e.statistics.time = 4500;
		setInt(m, "gametype", 0);
		setInt(m, "loons", 10);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 0);
		m.renderLast(e, 0);
		// Covers lines 325, 327-328, 332, 335, 337-338, 340-341, 343-344
	}

	@Test
	void renderLastHudScoreWithPendingLastscore() throws Exception {
		// Lines 330: lastscore!=0 && scgettime<120 -> score(+lastscore)
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
		// Covers line 330
	}

	@Test
	void renderLastHudScoreExpiredLastscore() throws Exception {
		// Lines 327-328: scgettime >= 120 -> plain score (not +lastscore)
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
		setInt(m, "scgettime", 120);
		m.renderLast(e, 0);
		// Covers lines 327-328 (scgettime>=120 branch)
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

	private static void setInt(Object o, String n, int v) throws Exception {
		findField(o.getClass(), n).setInt(o, v);
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
