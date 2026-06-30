package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Coverage-boost tests for {@link ExtremeMode} aimed at the lines not yet
 * exercised by the other Extreme test classes:
 *
 * <ul>
 *   <li>onSetting menu navigation: every cursor case (0-8) with LEFT/RIGHT,
 *       the wrap-around branches, B-button cancel, and the net-ranking-display
 *       path.</li>
 *   <li>renderSetting net-ranking path and version-gated SPIN BONUS strings.</li>
 *   <li>startGame branches: combo-disable, watcher BGM, and the three
 *       tspinEnableType variants.</li>
 *   <li>renderLast in-game else branch: every scoring-event font case,
 *       combo line, roll-time meter, and the score "(+n)" display.</li>
 *   <li>renderResult net branches (PB / sending / retry).</li>
 *   <li>The four NET stat/option send+receive methods.</li>
 * </ul>
 */
class ExtremeModeCoverageBoostTest {

	// ===============================================================
	//  onSetting — menu navigation & branches (lines 135-186, 204, 210)
	// ===============================================================

	@Test
	void onSettingRightAtEachCursorAdjustsMatchingOption() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		// Cursor 0 — startlevel +1
		setInt(mode, "startlevel", 5);
		runCursorRight(engine, mode, 0);
		assertEquals(6, readInt(mode, "startlevel"));

		// Cursor 1 — tspinEnableType +1
		setInt(mode, "tspinEnableType", 0);
		runCursorRight(engine, mode, 1);
		assertEquals(1, readInt(mode, "tspinEnableType"));

		// Cursor 2 — enableTSpinKick toggle
		setBoolean(mode, "enableTSpinKick", false);
		runCursorRight(engine, mode, 2);
		assertTrue(readBoolean(mode, "enableTSpinKick"));

		// Cursor 3 — spinCheckType +1
		setInt(mode, "spinCheckType", 0);
		runCursorRight(engine, mode, 3);
		assertEquals(1, readInt(mode, "spinCheckType"));

		// Cursor 4 — tspinEnableEZ toggle
		setBoolean(mode, "tspinEnableEZ", false);
		runCursorRight(engine, mode, 4);
		assertTrue(readBoolean(mode, "tspinEnableEZ"));

		// Cursor 5 — enableB2B toggle
		setBoolean(mode, "enableB2B", false);
		runCursorRight(engine, mode, 5);
		assertTrue(readBoolean(mode, "enableB2B"));

		// Cursor 6 — enableCombo toggle
		setBoolean(mode, "enableCombo", false);
		runCursorRight(engine, mode, 6);
		assertTrue(readBoolean(mode, "enableCombo"));

		// Cursor 7 — endless toggle
		setBoolean(mode, "endless", false);
		runCursorRight(engine, mode, 7);
		assertTrue(readBoolean(mode, "endless"));

		// Cursor 8 — big toggle
		setBoolean(mode, "big", false);
		runCursorRight(engine, mode, 8);
		assertTrue(readBoolean(mode, "big"));
	}

	@Test
	void onSettingLeftWrapsStartlevelAndTspinAndSpinType() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		// startlevel below 0 wraps to 19
		setInt(mode, "startlevel", 0);
		runCursorLeft(engine, mode, 0);
		assertEquals(19, readInt(mode, "startlevel"));

		// tspinEnableType below 0 wraps to 2
		setInt(mode, "tspinEnableType", 0);
		runCursorLeft(engine, mode, 1);
		assertEquals(2, readInt(mode, "tspinEnableType"));

		// spinCheckType below 0 wraps to 1
		setInt(mode, "spinCheckType", 0);
		runCursorLeft(engine, mode, 3);
		assertEquals(1, readInt(mode, "spinCheckType"));
	}

	@Test
	void onSettingRightWrapsStartlevelAndTspinAndSpinTypeAtTop() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		// startlevel above 19 wraps to 0
		setInt(mode, "startlevel", 19);
		runCursorRight(engine, mode, 0);
		assertEquals(0, readInt(mode, "startlevel"));

		// tspinEnableType above 2 wraps to 0
		setInt(mode, "tspinEnableType", 2);
		runCursorRight(engine, mode, 1);
		assertEquals(0, readInt(mode, "tspinEnableType"));

		// spinCheckType above 1 wraps to 0
		setInt(mode, "spinCheckType", 1);
		runCursorRight(engine, mode, 3);
		assertEquals(0, readInt(mode, "spinCheckType"));
	}

	@Test
	void onSettingBButtonSetsQuitFlag() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		mode.netIsNetPlay = false;

		pressKey(engine, Controller.BUTTON_B);
		boolean cont = mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
		assertTrue(cont);
	}

	@Test
	void onSettingNetRankingDisplayModeUsesNetRankingPath() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		// Drive the netIsNetRankingDisplayMode branch (line 136).
		// playerInit set up the netRanking arrays; with no data + no key press
		// netOnUpdateNetPlayRanking simply returns.
		mode.netIsNetRankingDisplayMode = true;

		pressKey(engine, -1); // clear all buttons
		boolean cont = mode.onSetting(engine, 0);
		assertTrue(cont);
	}

	@Test
	void onSettingReplayModeAdvancesAfterTimeout() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;

		setInt(mode, "menuTime", 0);
		assertTrue(mode.onSetting(engine, 0));
		assertEquals(-1, readInt(mode, "menuCursor"));

		setInt(mode, "menuTime", 60);
		assertFalse(mode.onSetting(engine, 0));
	}

	// ===============================================================
	//  renderSetting (lines 235, 243)
	// ===============================================================

	@Test
	void renderSettingNetRankingDisplayBranch() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netIsNetRankingDisplayMode = true; // line 235 path
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingLegacyVersionUsesEnableTSpinString() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netIsNetRankingDisplayMode = false;
		setInt(mode, "version", 0); // forces the legacy strTSpinEnable branch (line 243)
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingCurrentVersionEachSpinBonusString() throws Exception {
		for (int t = 0; t <= 2; t++) {
			ExtremeMode mode = new ExtremeMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			mode.netIsNetRankingDisplayMode = false;
			setInt(mode, "version", 1);
			setInt(mode, "tspinEnableType", t);
			mode.renderSetting(engine, 0);
		}
	}

	// ===============================================================
	//  startGame branches (lines 269, 273-274, 282)
	// ===============================================================

	@Test
	void startGameComboDisabledAndWatcherBgm() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "enableCombo", false); // line 269
		mode.netIsWatch = true;                 // line 274
		setInt(mode, "tspinEnableType", 0);     // line 282
		setInt(mode, "version", 1);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertEquals(BGMStatus.BGM_NOTHING, engine.owner.bgmStatus.bgm);
		assertFalse(engine.tspinEnable);
	}

	@Test
	void startGameTspinTypeOneEnablesTspin() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1);
		setInt(mode, "tspinEnableType", 1);

		mode.startGame(engine, 0);

		assertTrue(engine.tspinEnable);
	}

	@Test
	void startGameTspinTypeTwoEnablesAllSpinBonus() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1);
		setInt(mode, "tspinEnableType", 2);

		mode.startGame(engine, 0);

		assertTrue(engine.tspinEnable);
		assertTrue(engine.useAllSpinBonus);
	}

	// ===============================================================
	//  renderLast in-game else branch (lines 325-405)
	// ===============================================================

	@Test
	void renderLastInGameWithRollTimeAndScoreDelta() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE; // forces the else (in-game) branch
		engine.gameActive = true;
		engine.ending = 2;                     // roll-time display
		engine.statistics.level = 0;
		engine.statistics.lines = 5;
		setInt(mode, "lastscore", 100);        // score "(+n)" branch
		setInt(mode, "scgettime", 0);
		setInt(mode, "rolltime", 100);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastInGameHighLevelLineDisplay() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 0;
		setBoolean(mode, "endless", true);     // endless && level>=19 → bare line count
		engine.statistics.level = 19;
		engine.statistics.lines = 200;
		setInt(mode, "scgettime", 200);        // no "(+n)" branch

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastEveryScoringEventFontIsDrawn() throws Exception {
		// Walk every lastevent case (lines 358-405) including b2b true/false
		// and the combo line.
		int[] events = {
			AbstractMarathonMode.EVENT_SINGLE,
			AbstractMarathonMode.EVENT_DOUBLE,
			AbstractMarathonMode.EVENT_TRIPLE,
			AbstractMarathonMode.EVENT_FOUR,
			AbstractMarathonMode.EVENT_TSPIN_ZERO_MINI,
			AbstractMarathonMode.EVENT_TSPIN_ZERO,
			AbstractMarathonMode.EVENT_TSPIN_SINGLE_MINI,
			AbstractMarathonMode.EVENT_TSPIN_SINGLE,
			AbstractMarathonMode.EVENT_TSPIN_DOUBLE_MINI,
			AbstractMarathonMode.EVENT_TSPIN_DOUBLE,
			AbstractMarathonMode.EVENT_TSPIN_TRIPLE,
			AbstractMarathonMode.EVENT_TSPIN_EZ,
		};
		for (int ev : events) {
			for (boolean b2b : new boolean[]{true, false}) {
				ExtremeMode mode = new ExtremeMode();
				GameEngine engine = freshEngine(mode);
				mode.playerInit(engine, 0);
				engine.stat = GameEngine.Status.MOVE;
				engine.gameActive = false;
				engine.ending = 0;
				engine.statistics.level = 3;
				setInt(mode, "lastevent", ev);
				setBoolean(mode, "lastb2b", b2b);
				setInt(mode, "lastcombo", 4);  // combo line (line 404-405)
				setInt(mode, "scgettime", 0);  // < 120 so events render
				setInt(mode, "lastpiece", Piece.PIECE_T);

				mode.renderLast(engine, 0);
			}
		}
	}

	@Test
	void renderLastSettingStateDrawsRankingTable() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING; // ranking-table branch
		setBoolean(mode, "big", false);
		setBoolean(mode, "endless", true);       // endlessIndex = 1
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastMenuOnlyReturnsEarly() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = true;
		mode.renderLast(engine, 0); // early return path
	}

	// ===============================================================
	//  renderResult net branches (lines 621, 625, 627)
	// ===============================================================

	@Test
	void renderResultDrawsPersonalBestMarker() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netIsPB = true; // line 621
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultDrawsSendingStatus() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netIsNetPlay = true;
		setInt(mode, "netReplaySendStatus", 1); // line 625
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultDrawsRetryStatus() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		setInt(mode, "netReplaySendStatus", 2); // line 627
		mode.renderResult(engine, 0);
	}

	// ===============================================================
	//  NET stat / option send & receive (lines 678-732, 743-760)
	// ===============================================================

	@Test
	void netSendStatsBuildsAndSendsMessage() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupNetLobby(mode);
		engine.owner.backgroundStatus.fadesw = true; // bg = fadebg branch

		invoke(mode, "netSendStats", engine);
	}

	@Test
	void netSendStatsNonFadeBackgroundBranch() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupNetLobby(mode);
		engine.owner.backgroundStatus.fadesw = false; // bg = bg branch

		invoke(mode, "netSendStats", engine);
	}

	@Test
	void netRecvStatsParsesMessageIntoEngineState() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		String[] msg = new String[24];
		for (int i = 0; i < 4; i++) msg[i] = "";
		msg[4] = "12345";   // score
		msg[5] = "42";      // lines
		msg[6] = "100";     // totalPieceLocked
		msg[7] = "3600";    // time
		msg[8] = "7";       // level
		msg[9] = "5.5";     // lpm
		msg[10] = "2.25";   // spl
		msg[11] = "true";   // endless
		msg[12] = "true";   // gameActive
		msg[13] = "false";  // timerActive
		msg[14] = "800";    // lastscore
		msg[15] = "30";     // scgettime
		msg[16] = "4";      // lastevent
		msg[17] = "true";   // lastb2b
		msg[18] = "3";      // lastcombo
		msg[19] = "1";      // lastpiece
		msg[20] = "9";      // bg
		msg[21] = "1500";   // rolltime
		msg[22] = "60";     // meterValue
		msg[23] = "2";      // meterColor

		invokeWithArray(mode, "netRecvStats", engine, msg);

		assertEquals(12345, engine.statistics.score);
		assertEquals(42, engine.statistics.lines);
		assertEquals(7, engine.statistics.level);
		assertTrue(readBoolean(mode, "endless"));
		assertEquals(1500, readInt(mode, "rolltime"));
	}

	@Test
	void netSendEndGameStatsBuildsAndSendsMessage() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupNetLobby(mode);
		engine.statistics.score = 99999;
		engine.statistics.lines = 80;

		invoke(mode, "netSendEndGameStats", engine);
	}

	@Test
	void netSendOptionsBuildsAndSendsMessage() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupNetLobby(mode);
		setInt(mode, "startlevel", 4);
		setBoolean(mode, "endless", true);

		invoke(mode, "netSendOptions", engine);
	}

	@Test
	void netRecvOptionsParsesMessageIntoSettings() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		String[] msg = new String[13];
		for (int i = 0; i < 4; i++) msg[i] = "";
		msg[4] = "6";       // startlevel
		msg[5] = "2";       // tspinEnableType
		msg[6] = "false";   // enableTSpinKick
		msg[7] = "true";    // enableB2B
		msg[8] = "false";   // enableCombo
		msg[9] = "true";    // endless
		msg[10] = "true";   // big
		msg[11] = "1";      // spinCheckType
		msg[12] = "true";   // tspinEnableEZ

		invokeWithArray(mode, "netRecvOptions", engine, msg);

		assertEquals(6, readInt(mode, "startlevel"));
		assertEquals(2, readInt(mode, "tspinEnableType"));
		assertTrue(readBoolean(mode, "endless"));
		assertTrue(readBoolean(mode, "big"));
		assertEquals(1, readInt(mode, "spinCheckType"));
	}

	// ===============================================================
	//  Helpers
	// ===============================================================

	/** Set the cursor, press RIGHT, and run a single onSetting tick. */
	private static void runCursorRight(GameEngine engine, ExtremeMode mode, int cursor) throws Exception {
		setInt(mode, "menuCursor", cursor);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
	}

	/** Set the cursor, press LEFT, and run a single onSetting tick. */
	private static void runCursorLeft(GameEngine engine, ExtremeMode mode, int cursor) throws Exception {
		setInt(mode, "menuCursor", cursor);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
	}

	private static void setupNetLobby(ExtremeMode mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		NetPlayerClient client = new NetPlayerClient();
		setField(lobby, "netPlayerClient", client);
		setField(mode, "netLobby", lobby);
	}

	private static GameEngine freshEngine(ExtremeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	/** A menu key press (held-but-fresh, satisfies isMenuRepeatKey). btn < 0 clears all. */
	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		if (btn >= 0) {
			engine.ctrl.buttonPress[btn] = true;
			engine.ctrl.buttonTime[btn] = 1;
		}
	}

	private static void invoke(ExtremeMode mode, String name, GameEngine engine) throws Exception {
		Method m = findMethod(mode.getClass(), name, GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeWithArray(ExtremeMode mode, String name, GameEngine engine, String[] message) throws Exception {
		Method m = findMethod(mode.getClass(), name, GameEngine.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, message);
	}

	private static Method findMethod(Class<?> cls, String name, Class<?>... params) throws NoSuchMethodException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredMethod(name, params); }
			catch (NoSuchMethodException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchMethodException(name);
	}

	private static int readInt(ExtremeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ExtremeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static void setInt(ExtremeMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ExtremeMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
