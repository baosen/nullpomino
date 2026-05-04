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
 * Targeted branch coverage for {@link RetroManiaMode}.
 * Covers lines: 190-194 (startlevel change in onSetting),
 * 214 (B-button quit), 219-220/222-223 (replay-mode onSetting),
 * 249 (poweron onReady), 438-439 (saveReplay with ranking save).
 */
class RetroManiaModeBranchCoverageTest3 {

	@Test
	void onSettingChangeStartlevelLeftWrap() throws Exception {
		// Lines 190-194: menuCursor=1, change=-1, startlevel=0 wraps to 15
		RetroManiaMode m = new RetroManiaMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 1);
		setInt(m, "startlevel", 0);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		m.onSetting(e, 0);
		assertEquals(15, getInt(m, "startlevel"));
	}

	@Test
	void onSettingChangeStartlevelRightWrap() throws Exception {
		// Lines 190-194: menuCursor=1, change=1, startlevel=15 wraps to 0
		RetroManiaMode m = new RetroManiaMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 1);
		setInt(m, "startlevel", 15);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		m.onSetting(e, 0);
		assertEquals(0, getInt(m, "startlevel"));
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		// Line 214: isPush(BUTTON_B) -> quitflag = true
		RetroManiaMode m = new RetroManiaMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		m.onSetting(e, 0);
		assertTrue(e.quitflag);
	}

	@Test
	void onSettingReplayModeNotYet() throws Exception {
		// Lines 219-220: replay mode, menuTime < 60 -> returns true
		RetroManiaMode m = new RetroManiaMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = true;
		setInt(m, "menuTime", 30);
		assertTrue(m.onSetting(e, 0));
		assertEquals(31, getInt(m, "menuTime"));
		assertEquals(-1, getInt(m, "menuCursor"));
	}

	@Test
	void onSettingReplayModeReady() throws Exception {
		// Lines 222-223: replay mode, menuTime >= 60 -> returns false
		RetroManiaMode m = new RetroManiaMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = true;
		setInt(m, "menuTime", 60);
		assertFalse(m.onSetting(e, 0));
	}

	@Test
	void onReadyPoweron() throws Exception {
		// Line 249: poweron=true, statc[0]==0 -> creates nextPieceArrayID
		RetroManiaMode m = new RetroManiaMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		setBool(m, "poweron", true);
		e.statc[0] = 0;
		m.onReady(e, 0);
		assertNotNull(e.nextPieceArrayID);
	}

	@Test
	void onReadyPoweronFalse() throws Exception {
		// Line 249: poweron=false, statc[0]==0 -> does nothing
		RetroManiaMode m = new RetroManiaMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		setBool(m, "poweron", false);
		e.statc[0] = 0;
		e.nextPieceArrayID = null;
		m.onReady(e, 0);
		assertNull(e.nextPieceArrayID);
	}

	@Test
	void saveReplayWithRanking() throws Exception {
		// Lines 438-439: rankingRank != -1 -> saveRanking + saveModeConfig
		RetroManiaMode m = new RetroManiaMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setBool(m, "big", false);
		e.ai = null;
		e.statistics.score = 10000;
		e.statistics.lines = 50;
		e.statistics.time = 1000;
		CustomProperties prop = new CustomProperties();
		m.saveReplay(e, 0, prop);
		assertTrue(getInt(m, "rankingRank") >= 0,
			"Score 10000 should rank first (all initial rankings are 0)");
	}

	// ---- helpers (mirroring existing test convention) ----

	private static GameEngine fresh(RetroManiaMode m) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = m;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
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
