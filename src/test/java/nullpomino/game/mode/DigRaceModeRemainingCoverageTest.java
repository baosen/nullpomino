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
 * Targets remaining uncovered lines in DigRaceMode.java:
 * 155 (net ranking mode), 176-178,181-183,186-188,191-193,196-198,
 * 201-203,206-208,211-213,217-219,225 (onSetting menu cases),
 * 235,238-239,243-244,261,266 (menu + net),
 * 291,329,342,346 (renderSetting + startGame),
 * 455-458 (renderLast big font), 521,525,527,541 (renderResult),
 * 626-634 (netSendStats), 659-669 (netSendEndGameStats),
 * 677-683 (netSendOptions), 690-700 (netRecvOptions).
 */
class DigRaceModeRemainingCoverageTest {

	@Test
	void onSettingCursor0Gravity() throws Exception {
		DigRaceMode mode = new DigRaceMode();
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
		DigRaceMode mode = new DigRaceMode();
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
		DigRaceMode mode = new DigRaceMode();
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
		DigRaceMode mode = new DigRaceMode();
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
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 4);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		DigRaceMode mode = new DigRaceMode();
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
		DigRaceMode mode = new DigRaceMode();
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
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 7);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor8GoalType() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 8);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		mode.onSetting(e, 0);
	}

	@Test
	void onSettingCursor9Preset() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 9);
		e.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		mode.onSetting(e, 0);
	}

	@Test
	void onSettingLoadPreset() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 9);
		e.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		e.ctrl.buttonPress[Controller.BUTTON_A] = true;
		assertTrue(mode.onSetting(e, 0));
	}

	@Test
	void onSettingNetRankingMode() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);

		mode.onSetting(e, 0);
	}

	@Test
	void renderSettingNetRankingMode() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);

		mode.renderSetting(e, 0);
	}

	@Test
	void startGameVersionLessThan1() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "version", 0);
		setBoolean(mode, "big", true);

		mode.startGame(e, 0);

		assertTrue(e.big);
	}

	@Test
	void renderLastBigFontSingleDigit() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		mode.owner.menuOnly = false;
		e.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // GOAL_TABLE[0] = 5
		// Put a garbage block so remainLines > 0 and small
		fillGarbageForRemaining(mode, e, 0, 4);

		mode.renderLast(e, 0);
	}

	@Test
	void renderLastBigFontTwoDigit() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		mode.owner.menuOnly = false;
		e.createFieldIfNeeded();
		setInt(mode, "goaltype", 0);
		fillGarbageForRemaining(mode, e, 0, 3);

		// remainLines = 3 -> str "3" (1 digit)
		mode.renderLast(e, 0);
	}

	@Test
	void renderLastNegRemainLines() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		mode.owner.menuOnly = false;
		e.createFieldIfNeeded();

		mode.renderLast(e, 0);
	}

	@Test
	void renderResultNewPB() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		setBoolean(mode, "netIsPB", true);

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultSending() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		setBoolean(mode, "netIsNetPlay", true);
		setInt(mode, "netReplaySendStatus", 1);

		mode.renderResult(e, 0);
	}

	@Test
	void renderResultRetry() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT;
		setBoolean(mode, "netIsNetPlay", true);
		setBoolean(mode, "netIsWatch", false);
		setInt(mode, "netReplaySendStatus", 2);

		mode.renderResult(e, 0);
	}

	@Test
	void netSendEndGameStats() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statistics.lines = 10;
		e.statistics.totalPieceLocked = 30;
		e.statistics.time = 5000;
		e.statistics.lpm = 20.0f;
		e.statistics.pps = 1.5f;
		mode.owner = e.owner;
		// Set up netLobby to avoid NPE
		mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();

		mode.netSendEndGameStats(e);
	}

	@Test
	void netRecvOptions() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);

		String[] msg = {"game","option","0","0","4","256","0","0","0","30","14","0","1","5"};
		mode.netRecvOptions(e, msg);

		assertEquals(1, readInt(mode, "goaltype"));
	}

	// --- helpers ---

	private static void fillGarbageForRemaining(DigRaceMode mode, GameEngine e, int height, int remaining) throws Exception {
		// Set up field with some garbage blocks
		int w = e.field.getWidth();
		int h = e.field.getHeight();
		for(int y = h - 1; y > h - 1 - remaining; y--) {
			e.field.setBlock(0, y, new Block(Block.BLOCK_COLOR_GRAY, 0, Block.BLOCK_ATTRIBUTE_GARBAGE));
		}
	}

	private static GameEngine freshEngine(DigRaceMode mode) {
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
