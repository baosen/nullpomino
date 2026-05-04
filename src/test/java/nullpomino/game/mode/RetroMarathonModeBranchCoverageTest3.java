package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Targeted branch coverage for {@link RetroMarathonMode}.
 * Covers lines: 191 (playerInit replay mode), 254 (big toggle),
 * 269 (B-button quit), 337-361 (renderLast HUD else branch,
 * all gametype line labels, level, time),
 * 506-507 (saveReplay with ranking save).
 */
class RetroMarathonModeBranchCoverageTest3 {

	@Test
	void playerInitReplayModeLoadsReplayProp() throws Exception {
		// Line 191: replayMode -> loadSetting(owner.replayProp)
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		e.owner.replayMode = true;
		m.playerInit(e, 0);
		// version is not updated to CURRENT_VERSION in this path
		assertEquals(0, getInt(m, "version"));
	}

	@Test
	void onSettingToggleBig() throws Exception {
		// Line 254: menuCursor=3, change != 0 -> big = !big (false->true)
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 3);
		setBool(m, "big", false);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		m.onSetting(e, 0);
		assertTrue(getBool(m, "big"));
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		// Line 269: isPush(BUTTON_B) -> quitflag = true
		RetroMarathonMode m = new RetroMarathonMode();
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
		// Lines 337-361: else branch, lastscore==0 -> plain score
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
		m.renderLast(e, 0);
		// Covers lines 337-340, 344, 347-348, 354-355, 357-358, 360-361
	}

	@Test
	void renderLastHudScoreWithPendingLastscore() throws Exception {
		// Lines 342: lastscore!=0 && scgettime<120 -> score(+lastscore)
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
		// Covers line 342
	}

	@Test
	void renderLastHudGametypeA() throws Exception {
		// Line 348: GAMETYPE_TYPE_A -> strLine = lines
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 100;
		e.statistics.lines = 5;
		e.statistics.level = 1;
		e.statistics.time = 1000;
		setInt(m, "gametype", 0);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		m.renderLast(e, 0);
	}

	@Test
	void renderLastHudGametypeB() throws Exception {
		// Line 349: GAMETYPE_TYPE_B -> strLine = max(25-lines, 0)
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 100;
		e.statistics.lines = 10;
		e.statistics.level = 1;
		e.statistics.time = 1000;
		setInt(m, "gametype", 1);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		m.renderLast(e, 0);
	}

	@Test
	void renderLastHudGametypeArrange() throws Exception {
		// Line 350: GAMETYPE_ARRANGE -> strLine = lines + "/" + levellines
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.stat = GameEngine.Status.READY;
		e.statistics.score = 100;
		e.statistics.lines = 10;
		e.statistics.level = 1;
		e.statistics.time = 1000;
		setInt(m, "gametype", 2);
		setInt(m, "levellines", 100);
		setInt(m, "lastscore", 0);
		setInt(m, "scgettime", 200);
		m.renderLast(e, 0);
	}

	@Test
	void saveReplayWithRanking() throws Exception {
		// Lines 506-507: rankingRank != -1 -> saveRanking + saveModeConfig
		RetroMarathonMode m = new RetroMarathonMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setBool(m, "big", false);
		e.ai = null;
		e.statistics.score = 99999;
		e.statistics.lines = 100;
		e.statistics.level = 5;
		CustomProperties prop = new CustomProperties();
		m.saveReplay(e, 0, prop);
		assertTrue(getInt(m, "rankingRank") >= 0,
			"High score should rank in empty leaderboard");
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
