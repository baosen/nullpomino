package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Tail-coverage tests for {@link DigRaceMode}: targets onSetting net-send
 * branches (option change with spectators, load/save preset confirm, cancel,
 * enter net ranking), onReady net field-send, startGame watch BGM, renderLast
 * two-digit remaining-lines branch, saveReplay net name save, and the
 * netSendStats / netSendOptions message builders.
 */
class DigRaceModeTailCoverageTest {

	// --- 225: onSetting option change sends options when spectators present ---
	@Test
	void onSettingOptionChangeSendsOptions() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		attachNet(mode, e, /*spectators*/ 1);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 0);
		press(e, Controller.BUTTON_RIGHT);

		assertTrue(mode.onSetting(e, 0));
	}

	// --- 239: onSetting A at cursor 9 loads preset and sends options ---
	@Test
	void onSettingLoadPresetSendsOptions() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		attachNet(mode, e, 1);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 9);
		press(e, Controller.BUTTON_A);

		assertTrue(mode.onSetting(e, 0));
	}

	// --- 243-244: onSetting A at cursor 10 saves preset + mode config ---
	@Test
	void onSettingSavePreset() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 10);
		press(e, Controller.BUTTON_A);

		assertTrue(mode.onSetting(e, 0));
	}

	// --- 261: onSetting B cancel sets quitflag when not netplay ---
	@Test
	void onSettingCancelQuits() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 0);
		press(e, Controller.BUTTON_B);

		mode.onSetting(e, 0);

		assertTrue(e.quitflag);
	}

	// --- 266: onSetting D enters net play ranking screen ---
	@Test
	void onSettingDEntersNetRanking() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		attachNet(mode, e, 0);
		setBool(mode, "netIsNetPlay", true);
		setBool(mode, "big", false);
		setObj(mode, "netCurrentRoomInfo", new NetRoomInfo());
		e.ai = null;
		setInt(mode, "menuTime", 5);
		setInt(mode, "menuCursor", 0);
		press(e, Controller.BUTTON_D);

		mode.onSetting(e, 0);

		assertTrue(readBool(mode, "netIsNetRankingDisplayMode"));
	}

	// --- 329: onReady sends field when spectators present ---
	@Test
	void onReadySendsFieldWithSpectators() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		attachNet(mode, e, 1);
		setBool(mode, "netIsNetPlay", true);
		setBool(mode, "netIsWatch", false);
		e.statc[0] = 0;
		e.createFieldIfNeeded();

		boolean result = mode.onReady(e, 0);

		assertEquals(false, result);
	}

	// --- 346: startGame in watch mode silences BGM ---
	@Test
	void startGameWatchSilencesBgm() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setBool(mode, "netIsWatch", true);
		setInt(mode, "version", 1);

		mode.startGame(e, 0);

		assertEquals(nullpomino.game.component.BGMStatus.BGM_NOTHING, e.owner.bgmStatus.bgm);
	}

	// --- 455-458: renderLast two-digit remaining garbage lines ---
	@Test
	void renderLastTwoDigitRemainingLines() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		mode.owner.menuOnly = false;
		setInt(mode, "goaltype", 2); // GOAL_TABLE[2] = 18 rows scanned
		e.createFieldIfNeeded();
		int h = e.field.getHeight();
		// Fill 12 garbage rows -> remainLines = 12 (2-digit, str length 2)
		for (int y = h - 1; y > h - 1 - 12; y--) {
			e.field.setBlock(0, y, new Block(Block.BLOCK_COLOR_GRAY, 0, Block.BLOCK_ATTRIBUTE_GARBAGE));
		}

		mode.renderLast(e, 0);
	}

	// --- 541: saveReplay saves net player name ---
	@Test
	void saveReplaySavesNetName() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setObj(mode, "netPlayerName", "tester");

		mode.saveReplay(e, 0, e.owner.replayProp);

		assertEquals("tester", e.owner.replayProp.getProperty("0.net.netPlayerName", ""));
	}

	// --- 626-634: netSendStats builds and sends the stats message ---
	@Test
	void netSendStats() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		attachNet(mode, e, 0);
		e.statistics.lines = 5;
		e.statistics.totalPieceLocked = 12;
		e.statistics.time = 3000;
		e.statistics.lpm = 10.0f;
		e.statistics.pps = 1.0f;

		mode.netSendStats(e);
	}

	// --- 677-683: netSendOptions builds and sends the options message ---
	@Test
	void netSendOptions() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		attachNet(mode, e, 0);

		mode.netSendOptions(e);
	}

	// --- helpers ---

	private static void attachNet(DigRaceMode mode, GameEngine e, int spectators) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setObj(mode, "netLobby", lobby);
		mode.owner = e.owner;
		setInt(mode, "netNumSpectators", spectators);
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

	private static void press(GameEngine e, int btn) {
		e.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) e.ctrl.buttonTime[i] = 0;
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static int readInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static boolean readBool(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getBoolean(o);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	private static void setBool(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setBoolean(o, v);
	}

	private static void setObj(Object o, String n, Object v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.set(o, v);
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException ex) {}
		throw new NoSuchFieldException(n);
	}
}
