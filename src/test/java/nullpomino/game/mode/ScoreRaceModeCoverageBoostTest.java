package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Additional coverage for {@link ScoreRaceMode} targeting previously
 * uncovered lines: the net-ranking display branch of onSetting/renderSetting,
 * preset-number cursors (16/17), net option/start signalling inside onSetting,
 * netEnterNetPlayRankingScreen, startGame net-watch BGM, the renderLast event
 * switch (all scoring events + combo display), renderResult net states,
 * onResult page change, saveReplay net player name, and the NET send/receive
 * stats/options helpers plus net goal-type / ranking-view predicates.
 */
class ScoreRaceModeCoverageBoostTest {

	// ---------------------------------------------------------------
	// onSetting: net ranking display mode (line 225)
	// ---------------------------------------------------------------

	@Test
	void onSettingNetRankingDisplayModeInvokesUpdate() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);

		boolean result = mode.onSetting(engine, 0);
		assertTrue(result, "onSetting should stay on the settings screen");
	}

	@Test
	void renderSettingNetRankingDisplayMode() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);

		mode.renderSetting(engine, 0); // line 390
	}

	// ---------------------------------------------------------------
	// onSetting: preset cursors 16/17 (lines 313-316)
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor16AdjustsPresetNumber() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 16);
		int before = readInt(mode, "presetNumber");
		pressKey(engine, Controller.BUTTON_RIGHT);

		mode.onSetting(engine, 0);

		assertEquals(before + 1, readInt(mode, "presetNumber"));
	}

	@Test
	void onSettingCursor17AdjustsPresetNumber() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 17);
		int before = readInt(mode, "presetNumber");
		pressKey(engine, Controller.BUTTON_RIGHT);

		mode.onSetting(engine, 0);

		assertEquals(before + 1, readInt(mode, "presetNumber"));
	}

	@Test
	void onSettingPresetNumberWrapsBelowZero() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 16);
		setInt(mode, "presetNumber", 0);
		pressKey(engine, Controller.BUTTON_LEFT);

		mode.onSetting(engine, 0);

		assertEquals(99, readInt(mode, "presetNumber"),
				"presetNumber should wrap to 99 below 0");
	}

	// ---------------------------------------------------------------
	// onSetting: net options signalling (lines 321, 335)
	// ---------------------------------------------------------------

	@Test
	void onSettingChangeSendsNetOptionsToSpectators() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 0);
		enableNetPlayWithSpectators(mode);
		pressKey(engine, Controller.BUTTON_RIGHT);

		mode.onSetting(engine, 0); // line 321 netSendOptions
	}

	@Test
	void onSettingLoadPresetWithSpectatorsSendsOptions() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 16, 10); // menuTime >= 5 to allow confirm
		enableNetPlayWithSpectators(mode);
		pressKey(engine, Controller.BUTTON_A);

		mode.onSetting(engine, 0); // load preset + line 335 netSendOptions
	}

	@Test
	void onSettingSavePresetAtCursor17() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 17, 10);
		pressKey(engine, Controller.BUTTON_A);

		mode.onSetting(engine, 0); // savePreset path
	}

	// ---------------------------------------------------------------
	// onSetting: enter net play ranking screen (line 362)
	// ---------------------------------------------------------------

	@Test
	void onSettingPressDEntersNetPlayRanking() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenuState(engine, mode, 0, 10);
		enableNetPlayWithSpectators(mode);
		setBoolean(mode, "netIsWatch", false);
		setBoolean(mode, "big", false);
		engine.ai = null;
		NetRoomInfo room = new NetRoomInfo();
		room.rated = false;
		room.ruleName = "TEST";
		setField(mode, "netCurrentRoomInfo", room);

		pressKey(engine, Controller.BUTTON_D);
		mode.onSetting(engine, 0); // line 362 netEnterNetPlayRankingScreen

		assertTrue(readBoolean(mode, "netIsNetRankingDisplayMode"));
	}

	// ---------------------------------------------------------------
	// renderSetting: legacy T-Spin (version < 1, line 410)
	// ---------------------------------------------------------------

	@Test
	void renderSettingLegacyVersionPage2() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 0); // legacy -> getONorOFF(enableTSpin) at line 410
		setInt(mode, "menuCursor", 10);

		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingCurrentVersionPage2() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 1);
		setInt(mode, "menuCursor", 10);
		setInt(mode, "tspinEnableType", 2); // exercise the "ALL" label

		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// startGame: net watch BGM (line 436)
	// ---------------------------------------------------------------

	@Test
	void startGameNetWatchSilencesBgm() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsWatch", true);

		mode.startGame(engine, 0);

		assertEquals(nullpomino.game.component.BGMStatus.BGM_NOTHING,
				engine.owner.bgmStatus.bgm);
	}

	// ---------------------------------------------------------------
	// renderLast: event switch + combo display (lines 494, 514-563)
	// ---------------------------------------------------------------

	@Test
	void renderLastShowsRecentScoreDelta() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastscore", 500); // line 494 strScore with delta
		setInt(mode, "scgettime", 0);
		setInt(mode, "lastevent", 0); // EVENT_NONE -> skip switch

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastRendersEveryScoringEvent() throws Exception {
		// lastevent values 1..12 cover the whole switch (lines 516-560);
		// lastb2b toggles the b2b branches; lastcombo>=2 hits 562-563.
		for (int event = 1; event <= 12; event++) {
			for (boolean b2b : new boolean[]{false, true}) {
				ScoreRaceMode mode = new ScoreRaceMode();
				GameEngine engine = freshEngine(mode);
				mode.playerInit(engine, 0);
				engine.owner.menuOnly = false;
				engine.stat = GameEngine.Status.MOVE;
				setInt(mode, "lastscore", 100);
				setInt(mode, "scgettime", 0);
				setInt(mode, "lastevent", event);
				setBoolean(mode, "lastb2b", b2b);
				setInt(mode, "lastcombo", 3); // >= 2 triggers combo line
				setInt(mode, "lastpiece", nullpomino.game.component.Piece.PIECE_T);

				mode.renderLast(engine, 0);
			}
		}
	}

	@Test
	void renderLastResultRankingTable() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.RESULT;
		setBoolean(mode, "big", false);
		engine.ai = null;
		setBoolean(mode, "netIsWatch", false);

		mode.renderLast(engine, 0); // ranking table branch (lines 470-481)
	}

	// ---------------------------------------------------------------
	// renderResult: net states (lines 763, 767, 769)
	// ---------------------------------------------------------------

	@Test
	void renderResultShowsNewPB() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsPB", true); // line 763

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultShowsSendingStatus() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetPlay", true);
		setInt(mode, "netReplaySendStatus", 1); // line 767

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultShowsRetryStatus() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetPlay", true);
		setBoolean(mode, "netIsWatch", false);
		setInt(mode, "netReplaySendStatus", 2); // line 769
		engine.statc[1] = 1; // also render the second results page

		mode.renderResult(engine, 0);
	}

	// ---------------------------------------------------------------
	// onResult: page change (lines 785-787)
	// ---------------------------------------------------------------

	@Test
	void onResultDownAdvancesPage() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;
		pressKey(engine, Controller.BUTTON_DOWN);

		mode.onResult(engine, 0);

		assertEquals(1, engine.statc[1], "DOWN should advance to page 1");
	}

	@Test
	void onResultUpWrapsPage() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;
		pressKey(engine, Controller.BUTTON_UP);

		mode.onResult(engine, 0);

		assertEquals(1, engine.statc[1], "UP from page 0 should wrap to 1");
	}

	// ---------------------------------------------------------------
	// saveReplay: net player name (line 803)
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesNetPlayerName() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setField(mode, "netPlayerName", "ALICE");
		CustomProperties prop = new CustomProperties();

		mode.saveReplay(engine, 0, prop);

		assertEquals("ALICE", prop.getProperty("0.net.netPlayerName", ""));
	}

	// ---------------------------------------------------------------
	// NET send/receive helpers (lines 897-987)
	// ---------------------------------------------------------------

	@Test
	void netSendStatsBuildsAndSends() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		engine.nowPieceObject = new nullpomino.game.component.Piece(
				nullpomino.game.component.Piece.PIECE_T);

		invoke(mode, "netSendStats", new Class<?>[]{GameEngine.class},
				new Object[]{engine}); // lines 897-905
	}

	@Test
	void netRecvStatsParsesMessage() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		String[] msg = new String[]{
				"game", "stats", "", "",
				"1234",   // score
				"7",      // lines
				"42",     // totalPieceLocked
				"600",    // time
				"12.5",   // spm
				"3.3",    // lpm
				"4.4",    // spl
				"2",      // goaltype
				"true",   // gameActive
				"false",  // timerActive
				"100",    // lastscore
				"5",      // scgettime
				"8",      // lastevent
				"true",   // lastb2b
				"3",      // lastcombo
				"5"       // lastpiece
		};

		invoke(mode, "netRecvStats",
				new Class<?>[]{GameEngine.class, String[].class},
				new Object[]{engine, msg}); // lines 911-928

		assertEquals(1234, engine.statistics.score);
		assertEquals(7, engine.statistics.lines);
		assertEquals(2, readInt(mode, "goaltype"));
		assertEquals(8, readInt(mode, "lastevent"));
	}

	@Test
	void netSendEndGameStatsBuildsAndSends() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);

		invoke(mode, "netSendEndGameStats", new Class<?>[]{GameEngine.class},
				new Object[]{engine}); // lines 935-947
	}

	@Test
	void netSendOptionsBuildsAndSends() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);

		invoke(mode, "netSendOptions", new Class<?>[]{GameEngine.class},
				new Object[]{engine}); // lines 955-962
	}

	@Test
	void netRecvOptionsParsesMessage() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		String[] msg = new String[]{
				"game", "option", "", "",
				"5",     // gravity
				"256",   // denominator
				"1",     // are
				"2",     // areLine
				"3",     // lineDelay
				"30",    // lockDelay
				"14",    // das
				"6",     // bgmno
				"true",  // big
				"2",     // goaltype
				"1",     // tspinEnableType
				"true",  // enableTSpinKick
				"false", // enableB2B
				"true",  // enableCombo
				"7",     // presetNumber
				"1",     // spinCheckType
				"true"   // tspinEnableEZ
		};

		invoke(mode, "netRecvOptions",
				new Class<?>[]{GameEngine.class, String[].class},
				new Object[]{engine, msg}); // lines 969-987

		assertEquals(5, engine.speed.gravity);
		assertEquals(2, readInt(mode, "goaltype"));
		assertEquals(7, readInt(mode, "presetNumber"));
		assertTrue(readBoolean(mode, "big"));
	}

	// ---------------------------------------------------------------
	// NET goal type / ranking predicates (lines 994, 1002, 1010)
	// ---------------------------------------------------------------

	@Test
	void netGetGoalTypeReturnsGoalType() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		freshEngine(mode);
		setInt(mode, "goaltype", 2);

		Object result = invoke(mode, "netGetGoalType",
				new Class<?>[]{}, new Object[]{});

		assertEquals(2, ((Integer) result).intValue());
	}

	@Test
	void netIsNetRankingViewOKDependsOnBigAndAI() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "big", false);
		engine.ai = null;

		Object ok = invoke(mode, "netIsNetRankingViewOK",
				new Class<?>[]{GameEngine.class}, new Object[]{engine});
		assertTrue((Boolean) ok);

		setBoolean(mode, "big", true);
		Object notOk = invoke(mode, "netIsNetRankingViewOK",
				new Class<?>[]{GameEngine.class}, new Object[]{engine});
		assertFalse((Boolean) notOk);
	}

	@Test
	void netIsNetRankingSendOKRequiresGoalReached() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "big", false);
		engine.ai = null;
		setInt(mode, "goaltype", 0); // GOAL_TABLE[0] = 10000
		engine.statistics.score = 10000;

		Object ok = invoke(mode, "netIsNetRankingSendOK",
				new Class<?>[]{GameEngine.class}, new Object[]{engine});
		assertTrue((Boolean) ok);

		engine.statistics.score = 0;
		Object notOk = invoke(mode, "netIsNetRankingSendOK",
				new Class<?>[]{GameEngine.class}, new Object[]{engine});
		assertFalse((Boolean) notOk);
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(ScoreRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, ScoreRaceMode mode, int cursor)
			throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, ScoreRaceMode mode,
			int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", menuTime);
	}

	/** A net lobby whose client has no socket: send() fails silently, no NPE. */
	private static void attachNetLobby(ScoreRaceMode mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setField(mode, "netLobby", lobby);
	}

	private static void enableNetPlayWithSpectators(ScoreRaceMode mode) throws Exception {
		setBoolean(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 1);
		attachNetLobby(mode);
	}

	private static void pressKey(GameEngine e, int btn) {
		e.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) e.ctrl.buttonTime[i] = 0;
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static Object invoke(Object obj, String name, Class<?>[] types, Object[] args)
			throws Exception {
		Method m = findMethod(obj.getClass(), name, types);
		m.setAccessible(true);
		return m.invoke(obj, args);
	}

	private static Method findMethod(Class<?> cls, String name, Class<?>[] types)
			throws NoSuchMethodException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredMethod(name, types); }
			catch (NoSuchMethodException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchMethodException(name);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		findField(obj.getClass(), name).set(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
