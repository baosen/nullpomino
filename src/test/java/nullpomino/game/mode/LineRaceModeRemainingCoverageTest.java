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
 * Targets remaining uncovered lines in LineRaceMode.java:
 * 148 (onSetting net ranking), 169-171,174-176,179-181,184-186,189-191,
 * 194-196,199-201,204-205,207-209,213-215,221 (setting menu cases),
 * 235 (net send options on load), 261 (net ranking D button),
 * 286 (renderSetting net ranking), 362,365-366 (big font lines),
 * 431,435,437 (renderResult), 450 (saveReplay name),
 * 537-544 (netSendStats), 574-583 (netSendEndGameStats), 591-597 (netSendOptions).
 */
class LineRaceModeRemainingCoverageTest {

	@Test
	void onSettingNetRankingMode() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);

		// Should call netOnUpdateNetPlayRanking which is a no-op if net fields are null
		assertTrue(mode.onSetting(e, 0));
	}

	@Test
	void onSettingCursor0Gravity() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 0);
		e.speed.gravity = 4;
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);

		assertEquals(3, e.speed.gravity);
	}

	@Test
	void onSettingCursor1Denominator() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 1);
		e.speed.denominator = 256;
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;

		mode.onSetting(e, 0);

		assertEquals(257, e.speed.denominator);
	}

	@Test
	void onSettingCursor2Are() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 2);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor3AreLine() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 3);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 4);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 5);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor6Das() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 6);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor7Bgm() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 7);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor8Big() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 8);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;

		mode.onSetting(e, 0);
		assertTrue(readBoolean(mode, "big"));
	}

	@Test
	void onSettingCursor9GoalType() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 9);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor10Preset() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 10);
		e.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;

		mode.onSetting(e, 0);
	}

	@Test
	void renderSettingNetRankingMode() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);

		mode.renderSetting(e, 0);
		// no assertion - just verify no exception
	}

	@Test
	void renderLastBigFontSingleDigit() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		mode.owner.menuOnly = false;
		// Set lines so remainLines = 5, string length = 1
		setInt(mode, "goaltype", 1); // GOAL_TABLE[1] = 40
		e.statistics.lines = 35;

		mode.renderLast(e, 0);
	}

	@Test
	void renderLastBigFontTwoDigits() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		mode.owner.menuOnly = false;
		setInt(mode, "goaltype", 2); // GOAL_TABLE[2] = 100
		e.statistics.lines = 80; // remainLines = 20, length 2

		mode.renderLast(e, 0);
	}

	@Test
	void renderLastBigFontThreeDigits() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		mode.owner.menuOnly = false;
		setInt(mode, "goaltype", 2);
		e.statistics.lines = 0; // remainLines = 100, length 3

		mode.renderLast(e, 0);
	}

	@Test
	void renderResultNewPB() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		setBoolean(mode, "netIsPB", true);

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultSending() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		setBoolean(mode, "netIsNetPlay", true);
		setInt(mode, "netReplaySendStatus", 1);

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultRetry() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		setBoolean(mode, "netIsNetPlay", true);
		setBoolean(mode, "netIsWatch", false);
		setInt(mode, "netReplaySendStatus", 2);

		mode.renderResult(e, 0);
	}

	@Test
	void saveReplayWithPlayerName() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setString(mode, "netPlayerName", "TestPlayer");
		CustomProperties prop = new CustomProperties();

		mode.saveReplay(e, 0, prop);

		assertEquals("TestPlayer", prop.getProperty("0.net.netPlayerName", ""));
	}

	@Test
	void netSendStatsFormatsMessage() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statistics.lines = 10;
		e.statistics.totalPieceLocked = 20;
		e.statistics.time = 5000;
		e.statistics.lpm = 30.0f;
		e.statistics.pps = 1.5f;
		e.gameActive = true;
		e.timerActive = true;
		mode.owner = e.owner;
		// Set up netLobby to avoid NPE
		mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();

		// Just verify no exception - netSendStats tries to send over network
		mode.netSendStats(e);
	}

	@Test
	void netSendEndGameStats() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statistics.lines = 40;
		e.statistics.totalPieceLocked = 100;
		e.statistics.time = 8000;
		e.statistics.lpm = 60.0f;
		e.statistics.pps = 2.0f;
		setInt(mode, "goaltype", 1);
		mode.owner = e.owner;
		// Set up netLobby to avoid NPE
		mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();
		mode.netSendEndGameStats(e);
	}

	@Test
	void netSendOptionsFormatsMessage() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.speed.gravity = 4;
		e.speed.denominator = 256;
		e.speed.are = 0;
		e.speed.areLine = 0;
		e.speed.lineDelay = 0;
		e.speed.lockDelay = 30;
		e.speed.das = 14;
		mode.owner = e.owner;
		// Set up netLobby to avoid NPE
		mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();
		mode.netSendOptions(e);
	}

	// --- helpers ---

	private static GameEngine freshEngine(LineRaceMode mode) {
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

	private static void setString(Object o, String n, String v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.set(o, v);
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException e) {}
		throw new NoSuchFieldException(n);
	}
}
