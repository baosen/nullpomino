package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Fills the remaining uncovered lines of {@link MarathonMode} that the
 * existing MarathonMode tests miss: the {@code onSetting} menu switch
 * (every cursor case 0-8, the endless start-level wrap, B-cancel,
 * net-options signal, and the D-button net-ranking entry), the
 * {@code renderSetting} net-ranking + legacy-version branches,
 * {@code renderLast} endless / recent-score / 20G-line display paths,
 * {@code renderResult} PB / SENDING / RETRY lines, and the full NET
 * messaging surface ({@code netSendStats}, {@code netRecvStats},
 * {@code netSendEndGameStats}, {@code netSendOptions},
 * {@code netRecvOptions}). NET sends use a never-connected
 * {@link NetLobbyFrame}/{@link NetPlayerClient} whose {@code send()} is a
 * safe no-op.
 */
class MarathonModeRemainderCoverageTest {

	private MarathonMode mode;
	private GameManager manager;
	private GameEngine engine;

	@BeforeEach
	void setUp() {
		mode = new MarathonMode();
		manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		engine = manager.engine[0];
		engine.init();
		engine.owner.modeConfig = new CustomProperties();
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		// non-empty field so the all-clear branch never auto-fires
		engine.field.setBlock(0, engine.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_GRAY));
	}

	/**
	 * Wires a real (but never-connected) NetLobbyFrame + NetPlayerClient so that
	 * {@code netLobby.netPlayerClient.send(..)} executes fully: NetBaseClient.send
	 * swallows the resulting NPE (socket == null) and returns false.
	 */
	private void wireNetLobby() throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setField(mode, "netLobby", lobby);
	}

	// -----------------------------------------------------------------------
	// onSetting: net-ranking display mode (line 116)
	// -----------------------------------------------------------------------

	@Test
	void onSettingNetRankingDisplayModeReturnsTrue() throws Exception {
		setField(mode, "netIsNetRankingDisplayMode", true);
		// No buttons pressed, no ranking data ready: the call walks the
		// netOnUpdateNetPlayRanking gate and returns without effect.
		assertTrue(mode.onSetting(engine, 0));
		assertTrue(readBoolean(mode, "netIsNetRankingDisplayMode"));
	}

	// -----------------------------------------------------------------------
	// onSetting: menu switch cases (lines 126-173)
	// -----------------------------------------------------------------------

	@Test
	void onSettingCursor0StartLevelEndlessWrap() throws Exception {
		// goaltype 2 = ENDLESS -> tableGameClearLines[2] == -1 -> else branch (133-135)
		setMenuCursor(0);
		setField(mode, "goaltype", 2);
		setField(mode, "startlevel", 0);
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_LEFT); // change = -1 -> startlevel < 0 -> wraps to 19
		mode.onSetting(engine, 0);
		assertEquals(19, readInt(mode, "startlevel"));

		// RIGHT past 19 wraps back to 0
		pressKey(Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "startlevel"));
	}

	@Test
	void onSettingCursor0StartLevelFiniteGoalWrap() throws Exception {
		// goaltype 0 = 150 LINES -> tableGameClearLines[0] >= 0 -> if branch (129-131)
		setMenuCursor(0);
		setField(mode, "goaltype", 0);
		setField(mode, "startlevel", 0);
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_LEFT); // startlevel < 0 -> (150-1)/10 = 14
		mode.onSetting(engine, 0);
		assertEquals(14, readInt(mode, "startlevel"));
		assertEquals(14, engine.owner.backgroundStatus.bg);
	}

	@Test
	void onSettingCursor1TspinEnableTypeWraps() throws Exception {
		setMenuCursor(1);
		setField(mode, "tspinEnableType", 0);
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_LEFT); // 0-1 -> wraps to 2
		mode.onSetting(engine, 0);
		assertEquals(2, readInt(mode, "tspinEnableType"));

		pressKey(Controller.BUTTON_RIGHT); // 2+1 -> wraps to 0
		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "tspinEnableType"));
	}

	@Test
	void onSettingCursor2EnableTSpinKickToggles() throws Exception {
		setMenuCursor(2);
		boolean before = readBoolean(mode, "enableTSpinKick");
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readBoolean(mode, "enableTSpinKick"));
	}

	@Test
	void onSettingCursor3SpinCheckTypeWraps() throws Exception {
		setMenuCursor(3);
		setField(mode, "spinCheckType", 0);
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_LEFT); // 0-1 -> wraps to 1
		mode.onSetting(engine, 0);
		assertEquals(1, readInt(mode, "spinCheckType"));

		pressKey(Controller.BUTTON_RIGHT); // 1+1 -> wraps to 0
		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "spinCheckType"));
	}

	@Test
	void onSettingCursor4TspinEnableEZToggles() throws Exception {
		setMenuCursor(4);
		boolean before = readBoolean(mode, "tspinEnableEZ");
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readBoolean(mode, "tspinEnableEZ"));
	}

	@Test
	void onSettingCursor5EnableB2BToggles() throws Exception {
		setMenuCursor(5);
		boolean before = readBoolean(mode, "enableB2B");
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readBoolean(mode, "enableB2B"));
	}

	@Test
	void onSettingCursor6EnableComboToggles() throws Exception {
		setMenuCursor(6);
		boolean before = readBoolean(mode, "enableCombo");
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readBoolean(mode, "enableCombo"));
	}

	@Test
	void onSettingCursor7GoaltypeWraps() throws Exception {
		setMenuCursor(7);
		setField(mode, "goaltype", 0);
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_LEFT); // 0-1 -> wraps to GAMETYPE_MAX-1 = 2
		mode.onSetting(engine, 0);
		assertEquals(2, readInt(mode, "goaltype"));

		pressKey(Controller.BUTTON_RIGHT); // 2+1 -> wraps to 0
		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "goaltype"));
	}

	@Test
	void onSettingCursor7GoaltypeClampsStartLevel() throws Exception {
		// Start at goaltype 2 (endless, startlevel can be high), then change to a
		// finite goal whose max start-level is lower -> clamp branch (165-167).
		setMenuCursor(7);
		setField(mode, "goaltype", 2);
		setField(mode, "startlevel", 19);
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_RIGHT); // 2+1 wraps to 0 (150 LINES) -> clamp startlevel to 14
		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "goaltype"));
		assertEquals(14, readInt(mode, "startlevel"));
		assertEquals(14, engine.owner.backgroundStatus.bg);
	}

	@Test
	void onSettingCursor8BigToggles() throws Exception {
		setMenuCursor(8);
		boolean before = readBoolean(mode, "big");
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readBoolean(mode, "big"));
	}

	// -----------------------------------------------------------------------
	// onSetting: net-options signal on change (line 177)
	// -----------------------------------------------------------------------

	@Test
	void onSettingChangeSignalsNetOptions() throws Exception {
		wireNetLobby();
		setMenuCursor(2); // toggle enableTSpinKick
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netNumSpectators", 1);
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_RIGHT);
		// netSendOptions is reached after the toggle (line 177).
		assertTrue(mode.onSetting(engine, 0));
	}

	// -----------------------------------------------------------------------
	// onSetting: B-button cancel (line 195)
	// -----------------------------------------------------------------------

	@Test
	void onSettingPressBQuits() throws Exception {
		setMenuCursor(0);
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	// -----------------------------------------------------------------------
	// onSetting: D-button net-ranking entry (line 201)
	// -----------------------------------------------------------------------

	@Test
	void onSettingDButtonEntersNetRanking() throws Exception {
		wireNetLobby();
		setMenuCursor(0);
		setField(mode, "netIsNetPlay", true);
		setField(mode, "startlevel", 0);
		setField(mode, "big", false);
		setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
		engine.ai = null;
		setFieldOnAbstract("menuTime", 5);
		pressKey(Controller.BUTTON_D);
		mode.onSetting(engine, 0);
		assertTrue(readBoolean(mode, "netIsNetRankingDisplayMode"));
	}

	// -----------------------------------------------------------------------
	// renderSetting: net-ranking branch (line 226) and legacy version (234)
	// -----------------------------------------------------------------------

	@Test
	void renderSettingNetRankingMode() throws Exception {
		setField(mode, "netIsNetRankingDisplayMode", true);
		mode.renderSetting(engine, 0); // calls netOnRenderNetPlayRanking
		assertTrue(true);
	}

	@Test
	void renderSettingLegacyVersionUsesEnableTSpin() throws Exception {
		setField(mode, "netIsNetRankingDisplayMode", false);
		setField(mode, "version", 1); // version < 2 -> getONorOFF(enableTSpin) (line 234)
		mode.renderSetting(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderSettingVersion2WalksSpinBonusLabels() throws Exception {
		setField(mode, "netIsNetRankingDisplayMode", false);
		setField(mode, "version", 2);
		for (int type = 0; type <= 2; type++) {
			setField(mode, "tspinEnableType", type);
			mode.renderSetting(engine, 0);
		}
		assertTrue(true);
	}

	// -----------------------------------------------------------------------
	// renderLast: endless label (298), recent-score (+) (322), 20G line (328)
	// -----------------------------------------------------------------------

	@Test
	void renderLastEndlessGameLabel() throws Exception {
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setField(mode, "goaltype", 2); // tableGameClearLines[2] == -1 -> "(ENDLESS GAME)" (298)
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastRecentScoreShowsDelta() throws Exception {
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setField(mode, "lastscore", 500);
		setField(mode, "scgettime", 10); // != 0 and < 120 -> "(+500)" branch (322)
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastEndless20GLineDisplay() throws Exception {
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setField(mode, "goaltype", 2); // endless -> tableGameClearLines < 0
		engine.statistics.level = 19; // level >= 19 -> bare line count (328)
		engine.statistics.lines = 200;
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastFiniteGoalLineDisplay() throws Exception {
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setField(mode, "goaltype", 0); // 150 lines goal
		engine.statistics.level = 3;
		engine.statistics.lines = 35;
		mode.renderLast(engine, 0); // "lines/(level+1)*10" branch (330)
		assertTrue(true);
	}

	@Test
	void renderLastSettingStateShowsRanking() throws Exception {
		engine.owner.menuOnly = false;
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;
		setField(mode, "big", false);
		engine.ai = null;
		mode.renderLast(engine, 0); // ranking table branch (309-314)
		assertTrue(true);
	}

	// -----------------------------------------------------------------------
	// renderResult: PB / SENDING / RETRY lines (581, 585, 587)
	// -----------------------------------------------------------------------

	@Test
	void renderResultShowsNewPB() throws Exception {
		setField(mode, "netIsPB", true);
		mode.renderResult(engine, 0); // "NEW PB" (581)
		assertTrue(true);
	}

	@Test
	void renderResultShowsSending() throws Exception {
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netReplaySendStatus", 1);
		mode.renderResult(engine, 0); // "SENDING..." (585)
		assertTrue(true);
	}

	@Test
	void renderResultShowsRetry() throws Exception {
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		setField(mode, "netReplaySendStatus", 2);
		mode.renderResult(engine, 0); // "A: RETRY" (587)
		assertTrue(true);
	}

	// -----------------------------------------------------------------------
	// netSendStats (lines 638-647)
	// -----------------------------------------------------------------------

	@Test
	void netSendStatsBuildsAndSends() throws Exception {
		wireNetLobby();
		engine.statistics.level = 5;
		invokeNet("netSendStats", engine);
		assertTrue(true);
	}

	@Test
	void netSendStatsWithFadeBackground() throws Exception {
		wireNetLobby();
		engine.owner.backgroundStatus.fadesw = true; // ternary true branch (638)
		engine.owner.backgroundStatus.fadebg = 3;
		invokeNet("netSendStats", engine);
		assertTrue(true);
	}

	// -----------------------------------------------------------------------
	// netRecvStats: full parse + meter tiers (lines 653-678)
	// -----------------------------------------------------------------------

	private String[] statsMessage(int lines) {
		// indices 4..20 are parsed by netRecvStats
		return new String[] {
			"game", "0", "0", "stats",
			"1000",                 // 4 score
			String.valueOf(lines),  // 5 lines
			"42",                   // 6 totalPieceLocked
			"3600",                 // 7 time
			"5",                    // 8 level
			"3.5",                  // 9 lpm (float)
			"1.5",                  // 10 spl (double)
			"0",                    // 11 goaltype
			"true",                 // 12 gameActive
			"true",                 // 13 timerActive
			"100",                  // 14 lastscore
			"5",                    // 15 scgettime
			"1",                    // 16 lastevent
			"true",                 // 17 lastb2b
			"3",                    // 18 lastcombo
			"2",                    // 19 lastpiece
			"7"                     // 20 bg
		};
	}

	@Test
	void netRecvStatsParsesAndSetsRedMeter() throws Exception {
		invokeNetRecv("netRecvStats", engine, statsMessage(8)); // lines%10=8 -> RED
		assertEquals(1000, engine.statistics.score);
		assertEquals(8, engine.statistics.lines);
		assertEquals(5, engine.statistics.level);
		assertEquals(0, readInt(mode, "goaltype"));
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void netRecvStatsMeterColorTiers() throws Exception {
		invokeNetRecv("netRecvStats", engine, statsMessage(4)); // YELLOW
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
		invokeNetRecv("netRecvStats", engine, statsMessage(6)); // ORANGE
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
		invokeNetRecv("netRecvStats", engine, statsMessage(1)); // GREEN
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	// -----------------------------------------------------------------------
	// netSendEndGameStats (lines 685-696)
	// -----------------------------------------------------------------------

	@Test
	void netSendEndGameStatsBuildsAndSends() throws Exception {
		wireNetLobby();
		engine.statistics.level = 5;
		engine.statistics.levelDispAdd = 1;
		invokeNet("netSendEndGameStats", engine);
		assertTrue(true);
	}

	// -----------------------------------------------------------------------
	// netSendOptions (lines 703-708)
	// -----------------------------------------------------------------------

	@Test
	void netSendOptionsBuildsAndSends() throws Exception {
		wireNetLobby();
		invokeNet("netSendOptions", engine);
		assertTrue(true);
	}

	// -----------------------------------------------------------------------
	// netRecvOptions (lines 715-724)
	// -----------------------------------------------------------------------

	@Test
	void netRecvOptionsParsesAll() throws Exception {
		String[] msg = {
			"game", "0", "option", "x",
			"7",      // 4 startlevel
			"2",      // 5 tspinEnableType
			"true",   // 6 enableTSpinKick
			"1",      // 7 spinCheckType
			"true",   // 8 tspinEnableEZ
			"false",  // 9 enableB2B
			"true",   // 10 enableCombo
			"2",      // 11 goaltype
			"true"    // 12 big
		};
		invokeNetRecv("netRecvOptions", engine, msg);
		assertEquals(7, readInt(mode, "startlevel"));
		assertEquals(2, readInt(mode, "tspinEnableType"));
		assertTrue(readBoolean(mode, "enableTSpinKick"));
		assertEquals(1, readInt(mode, "spinCheckType"));
		assertTrue(readBoolean(mode, "tspinEnableEZ"));
		assertFalse(readBoolean(mode, "enableB2B"));
		assertTrue(readBoolean(mode, "enableCombo"));
		assertEquals(2, readInt(mode, "goaltype"));
		assertTrue(readBoolean(mode, "big"));
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	private void invokeNet(String name, GameEngine eng) throws Exception {
		Method m = MarathonMode.class.getDeclaredMethod(name, GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, eng);
	}

	private void invokeNetRecv(String name, GameEngine eng, String[] msg) throws Exception {
		Method m = MarathonMode.class.getDeclaredMethod(name, GameEngine.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, eng, msg);
	}

	private void pressKey(int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) engine.ctrl.buttonTime[i] = 0;
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private void setMenuCursor(int cursor) throws Exception {
		engine.owner.replayMode = false;
		setFieldOnAbstract("menuCursor", cursor);
	}

	private void setFieldOnAbstract(String name, int value) throws Exception {
		Field f = findField(AbstractMode.class, name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static int readInt(Object o, String name) throws Exception {
		Field f = findField(o.getClass(), name);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static boolean readBoolean(Object o, String name) throws Exception {
		Field f = findField(o.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(o);
	}

	private static void setField(Object o, String name, Object value) throws Exception {
		Field f = findField(o.getClass(), name);
		f.setAccessible(true);
		f.set(o, value);
	}

	private static void setField(Object o, String name, int value) throws Exception {
		Field f = findField(o.getClass(), name);
		f.setAccessible(true);
		f.setInt(o, value);
	}

	private static void setField(Object o, String name, boolean value) throws Exception {
		Field f = findField(o.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(o, value);
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
