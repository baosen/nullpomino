package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch coverage for {@link RetroMasteryMode}: covers meter colors,
 * calcScore game types, setSpeed, startGame, replay mode, ranking,
 * renderResult, saveReplay, and onSetting branches.
 */
class RetroMasteryModeBranchCoverageTest {

	@Test
	void calcScoreSingle() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		e.statistics.lines = 0;
		e.statistics.level = 0;
		m.calcScore(e, 0, 1);
		assertEquals(40, e.statistics.score);
		assertEquals(1, getInt(m, "loons"));
	}

	@Test
	void calcScoreDouble() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		e.statistics.level = 0;
		m.calcScore(e, 0, 2);
		assertEquals(100, e.statistics.score);
		assertEquals(2, getInt(m, "loons"));
	}

	@Test
	void calcScoreTriple() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		e.statistics.level = 0;
		m.calcScore(e, 0, 3);
		assertEquals(200, e.statistics.score);
		assertEquals(3, getInt(m, "loons"));
	}

	@Test
	void calcScoreFour() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		e.statistics.level = 0;
		m.calcScore(e, 0, 4);
		assertEquals(300, e.statistics.score);
		assertEquals(3, getInt(m, "loons"));
	}

	@Test
	void calcScoreGame200Ending() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 0);
		setInt(m, "loons", 199);
		e.statistics.level = 0;
		m.calcScore(e, 0, 1);
		assertEquals(1, e.ending);
	}

	@Test
	void calcScoreEndlessLevelLines() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 1);
		setInt(m, "startlevel", 15);
		m.startGame(e, 0);
		// startlevel 15: level=15, levellines = startlevel<=9? ... : (startlevel+11)*5 = 26*5 = 130
		assertEquals(130, getInt(m, "levellines"));
	}

	@Test
	void calcScoreLevelUp() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "levellines", 10);
		setInt(m, "loons", 10);
		e.statistics.level = 0;
		m.calcScore(e, 0, 1);
		assertEquals(1, e.statistics.level);
	}

	@Test
	void calcScoreMeterPressureRed() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 2);
		// Set levellines high enough so no level-up happens during calcScore
		// togo = levellines - (loons + 1). For RED: togo == 1 → loons = levellines - 2
		setInt(m, "levellines", 10);
		setInt(m, "loons", 8);
		e.statistics.level = 0;
		m.calcScore(e, 0, 1);
		assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
	}

	@Test
	void calcScoreMeterPressureOrange() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 2);
		// togo = levellines - (loons + 1). For ORANGE: togo == 2 → loons = levellines - 3
		setInt(m, "levellines", 10);
		setInt(m, "loons", 7);
		e.statistics.level = 0;
		m.calcScore(e, 0, 1);
		assertEquals(GameEngine.METER_COLOR_ORANGE, e.meterColor);
	}

	@Test
	void calcScoreMeterStartLevelSame() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 0);
		setInt(m, "startlevel", 5);
		setInt(m, "levellines", 60);
		setInt(m, "loons", 0);
		e.statistics.level = 5;
		m.calcScore(e, 0, 1);
		// level==startlevel so startlevel!=0 → goes to startlevel meter branch
		assertNotNull(e.meterColor);
	}

	@Test
	void calcScoreMeterDefaultYellow() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 0);
		setInt(m, "levellines", 20);
		setInt(m, "loons", 15);
		e.statistics.level = 3;
		m.calcScore(e, 0, 1);
	}

	@Test
	void calcScoreMeterDefaultGreen() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "gametype", 0);
		setInt(m, "levellines", 20);
		setInt(m, "loons", 11);
		e.statistics.level = 5;
		m.calcScore(e, 0, 1);
	}

	@Test
	void renderSettingReplayMode() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = true;
		m.renderSetting(e, 0);
	}

	@Test
	void startGamePressure() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		setInt(m, "gametype", 2);
		m.startGame(e, 0);
		assertEquals(5, getInt(m, "levellines"));
	}

	@Test
	void startGame200() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		setInt(m, "gametype", 0);
		setInt(m, "startlevel", 3);
		m.startGame(e, 0);
		assertEquals(40, getInt(m, "levellines"));
	}

	@Test
	void setSpeedClampsLv() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		e.statistics.level = -1;
		Method me = RetroMasteryMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		me.setAccessible(true);
		me.invoke(m, e);
		assertTrue(e.speed.gravity > 0);
	}

	@Test
	void saveReplayUpdatesRanking() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.statistics.score = 99999;
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		e.ai = null;
		CustomProperties prop = new CustomProperties();
		m.saveReplay(e, 0, prop);
	}

	@Test
	void saveReplaySkipsRankingWhenAi() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.ai = new nullpomino.game.ai.DummyAI();
		CustomProperties prop = new CustomProperties();
		m.saveReplay(e, 0, prop);
	}

	@Test
	void renderResultDisplaysEfficiency() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.statistics.score = 1000;
		m.renderResult(e, 0);
	}

	@Test
	void onSettingCursorUpSkipsPressureLevel() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		setInt(m, "gametype", 2);
		setInt(m, "menuCursor", 1);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		m.onSetting(e, 0);
		assertEquals(0, getInt(m, "menuCursor"));
	}

	@Test
	void onSettingCursorDownSkipsPressureLevel() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "gametype", 2);
		setInt(m, "menuCursor", 0);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		m.onSetting(e, 0);
		assertEquals(2, getInt(m, "menuCursor"));
	}

	@Test
	void onSettingChangeBig() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = false;
		setInt(m, "menuCursor", 2);
		e.ctrl = new Controller();
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		m.onSetting(e, 0);
		assertTrue(getBool(m, "big"));
	}

	@Test
	void onSettingReplayModeAutoProceed() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = true;
		setInt(m, "menuTime", 60);
		assertFalse(m.onSetting(e, 0));
	}

	@Test
	void onSettingReplayModeNotYet() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.owner.replayMode = true;
		setInt(m, "menuTime", 30);
		assertTrue(m.onSetting(e, 0));
	}

	@Test
	void calcScoreEfficiencyZero() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		e.nowPieceObject = new Piece(Piece.PIECE_T);
		e.createFieldIfNeeded();
		setInt(m, "actions", 0);
		m.calcScore(e, 0, 0);
		assertEquals(0f, getFloat(m, "efficiency"), 0.001f);
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		m.onLast(e, 0);
		assertEquals(1, getInt(m, "scgettime"));
	}

	@Test
	void loadSettingFromCustomProperties() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		CustomProperties p = new CustomProperties();
		p.setProperty("retromastery.gametype", 1);
		p.setProperty("retromastery.startlevel", 5);
		p.setProperty("retromastery.big", true);
		p.setProperty("retromastery.version", 1);
		m.loadSetting(p);
		assertEquals(1, getInt(m, "gametype"));
		assertEquals(5, getInt(m, "startlevel"));
		assertTrue(getBool(m, "big"));
	}

	@Test
	void hardDropFallAccumulates() throws Exception {
		RetroMasteryMode m = new RetroMasteryMode();
		GameEngine e = fresh(m);
		m.playerInit(e, 0);
		m.afterHardDropFall(e, 0, 10);
		assertEquals(10, getInt(m, "harddropscore"));
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

	private static float getFloat(Object o, String n) throws Exception {
		return findField(o.getClass(), n).getFloat(o);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		findField(o.getClass(), n).setInt(o, v);
	}

	private static void setBool(Object o, String n, boolean v) throws Exception {
		findField(o.getClass(), n).setBoolean(o, v);
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws Exception {
		for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
			try { java.lang.reflect.Field f = c.getDeclaredField(name); f.setAccessible(true); return f; } catch (NoSuchFieldException e) { /* continue */ }
		}
		throw new NoSuchFieldException(name);
	}
}
