package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Boosts line coverage of {@link TimeAttackMode} for branches not exercised by
 * the existing tests: onSetting menu navigation (each cursor LEFT/RIGHT toggle,
 * net-ranking display path, B/D buttons, replay auto-advance), renderLast
 * in-game HUD (level/time/norm/speed/roll/section rendering), onLast countdown
 * sound and meter for 20-level types, calcScore section-complete increment,
 * renderResult section pages, and the NET send/recv stat & option helpers.
 */
class TimeAttackModeCoverageBoostTest {

	// ----------------------------------------------------------------
	// onSetting menu navigation
	// ----------------------------------------------------------------

	@Test
	void onSettingLeftRightAtCursor0ChangesGameTypeAndWraps() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		setInt(mode, "menuCursor", 0);
		setInt(mode, "goaltype", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		boolean cont = mode.onSetting(engine, 0);
		assertTrue(cont, "onSetting should keep running while in menu");
		// goaltype 0 - 1 wraps to GAMETYPE_MAX - 1 = 10
		assertEquals(10, readInt(mode, "goaltype"));

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		// 10 + 1 wraps to 0
		assertEquals(0, readInt(mode, "goaltype"));
	}

	@Test
	void onSettingLeftRightAtCursor1ChangesStartLevelAndWraps() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		setInt(mode, "menuCursor", 1);
		setInt(mode, "goaltype", 0);
		setInt(mode, "startlevel", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		// startlevel 0 - 1 wraps to tableGoalLevel[0] - 1 = 14
		assertEquals(14, readInt(mode, "startlevel"));

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		// 14 + 1 wraps to 0
		assertEquals(0, readInt(mode, "startlevel"));
	}

	@Test
	void onSettingCursor2TogglesShowSectionTime() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		setInt(mode, "menuCursor", 2);
		boolean before = readBoolean(mode, "showsectiontime");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readBoolean(mode, "showsectiontime"));
	}

	@Test
	void onSettingCursor3TogglesBig() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		setInt(mode, "menuCursor", 3);
		boolean before = readBoolean(mode, "big");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readBoolean(mode, "big"));
	}

	@Test
	void onSettingChangeNotifiesSpectators() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBoolean(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 1);
		mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();

		setInt(mode, "menuCursor", 2);
		pressKey(engine, Controller.BUTTON_RIGHT);
		// Toggling a setting under netplay with spectators sends options (line 487)
		mode.onSetting(engine, 0);
	}

	@Test
	void onSettingPressAStartsGame() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10); // >= 5 so decide is accepted
		pressKey(engine, Controller.BUTTON_A);
		boolean cont = mode.onSetting(engine, 0);
		assertFalse(cont, "A button should confirm and end the setting screen");
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0);
		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag, "B button should set quitflag");
	}

	@Test
	void onSettingPressDEntersNetRankingWhenNetPlay() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBoolean(mode, "netIsNetPlay", true);
		setBoolean(mode, "big", false);
		mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();
		mode.netCurrentRoomInfo = new nullpomino.game.net.NetRoomInfo();

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0);
		pressKey(engine, Controller.BUTTON_D);
		// Should not throw; net ranking entry path executes
		mode.onSetting(engine, 0);
	}

	@Test
	void onSettingNetRankingDisplayMode() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);
		// Mark data as missing so the inner navigation block is skipped (no NPE)
		boolean[] noData = (boolean[]) readField(mode, "netRankingNoDataFlag");
		noData[0] = true;

		boolean cont = mode.onSetting(engine, 0);
		assertTrue(cont, "Net ranking display path should keep onSetting running");
	}

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;

		setInt(mode, "menuTime", 0);
		boolean cont = mode.onSetting(engine, 0);
		assertTrue(cont);
		assertEquals(1, readInt(mode, "menuTime"));
		assertEquals(-1, readInt(mode, "menuCursor"));

		setInt(mode, "menuTime", 59);
		boolean cont2 = mode.onSetting(engine, 0);
		assertFalse(cont2, "Replay setting should end after 60 frames");
	}

	@Test
	void startGameInWatchModeSilencesBgm() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.owner = engine.owner;
		setBoolean(mode, "netIsWatch", true);

		mode.startGame(engine, 0);
		assertEquals(nullpomino.game.component.BGMStatus.BGM_NOTHING,
				engine.owner.bgmStatus.bgm);
	}

	// ----------------------------------------------------------------
	// renderSetting
	// ----------------------------------------------------------------

	@Test
	void renderSettingDrawsMenu() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingNetRankingDisplayMode() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);
		mode.renderSetting(engine, 0);
	}

	// ----------------------------------------------------------------
	// renderLast in-game HUD (the "else" branch)
	// ----------------------------------------------------------------

	@Test
	void renderLastInGameHudDrawsStats() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE; // not SETTING / RESULT
		engine.statistics.level = 3;
		engine.statistics.time = 1234;
		setInt(mode, "norm", 35);
		setInt(mode, "levelTimer", 500); // < 600 to hit blink condition
		// Populate a couple of section times so the section loop runs
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 120;
		sectiontime[3] = 240;
		setBoolean(mode, "showsectiontime", true);
		setInt(mode, "sectionavgtime", 180);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastInGameHudWithRollTime() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statistics.level = 5;
		engine.gameActive = true;
		engine.ending = 2;
		engine.staffrollEnable = true;
		setInt(mode, "rolltime", 100);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastRankingInSettingState() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;
		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);
		// Set some ranking entries with rollclear states to hit the color branches
		int[][] rollclear = (int[][]) readField(mode, "rankingRollclear");
		rollclear[0][0] = 1;
		rollclear[0][1] = 2;
		setInt(mode, "rankingRank", 0);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastMenuOnlyReturnsEarly() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = true;
		mode.renderLast(engine, 0);
	}

	// ----------------------------------------------------------------
	// onLast: countdown sound + meter for 20-level types
	// ----------------------------------------------------------------

	@Test
	void onLastPlaysCountdownSound() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 0;
		// levelTimer = 361 -> after decrement 360 (<=600 and %60==0) plays countdown
		setInt(mode, "levelTimer", 361);
		setInt(mode, "levelTimerMax", 600);

		mode.onLast(engine, 0);
		assertEquals(360, readInt(mode, "levelTimer"));
	}

	@Test
	void onLastUpdatesMeterFor20LevelType() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 7); // BASIC: tableGoalLevel = 20
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 0;
		setInt(mode, "levelTimerMax", 1800);
		setInt(mode, "levelTimer", 5 * 60); // <= 10*60 -> RED meter color
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void onLastHellDisablesHeboHiddenOutsideLevelRange() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 8); // HELL
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 0; // outside 5..14 -> else branch disables hebo
		engine.heboHiddenEnable = true;
		setInt(mode, "levelTimer", 1000);
		setInt(mode, "levelTimerMax", 1800);

		mode.onLast(engine, 0);
		assertFalse(engine.heboHiddenEnable, "HELL outside lvl range disables heboHidden");
	}

	// ----------------------------------------------------------------
	// calcScore: section-complete increment at game complete
	// ----------------------------------------------------------------

	@Test
	void calcScoreGameCompleteIncrementsSectionsComp() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.timerActive = true; // so sectionscomp++ branch (lines 811-812) runs
		setInt(mode, "goaltype", 0); // NORMAL: goal level 15 -> 150 norm
		engine.statistics.level = 14;
		setInt(mode, "norm", 149);
		int before = readInt(mode, "sectionscomp");

		mode.calcScore(engine, 0, 1); // norm becomes 150 -> game complete

		assertEquals(1, engine.ending);
		assertEquals(before + 1, readInt(mode, "sectionscomp"));
	}

	// ----------------------------------------------------------------
	// renderResult section pages
	// ----------------------------------------------------------------

	@Test
	void renderResultPage0() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;
		engine.statistics.rollclear = 2;
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage1SectionTimes() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 1;
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		for (int i = 0; i < 10; i++) sectiontime[i] = (i + 1) * 60;
		setInt(mode, "sectionavgtime", 300);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage2SectionTimes() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 2;
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		for (int i = 10; i < sectiontime.length; i++) sectiontime[i] = (i + 1) * 60;
		setInt(mode, "sectionavgtime", 300);

		mode.renderResult(engine, 0);
	}

	@Test
	void onResultNavigatesPages() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.statc[1] = 0;
		pressKey(engine, Controller.BUTTON_UP);
		mode.onResult(engine, 0);
		assertEquals(2, engine.statc[1], "UP from page 0 wraps to page 2");

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1], "DOWN from page 2 wraps to page 0");
	}

	// ----------------------------------------------------------------
	// NET send/recv helpers
	// ----------------------------------------------------------------

	@Test
	void netSendStatsBuildsMessage() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.owner = engine.owner;
		mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();

		mode.netSendStats(engine);
	}

	@Test
	void netRecvStatsParsesMessage() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		String[] msg = new String[] {
			"game", "stats", "x", "x",
			"42",     // 4 lines
			"30",     // 5 totalPieceLocked
			"5000",   // 6 time
			"20.5",   // 7 lpm
			"1.5",    // 8 pps
			"3",      // 9 goaltype
			"true",   // 10 gameActive
			"false",  // 11 timerActive
			"7",      // 12 level
			"600",    // 13 levelTimer
			"1800",   // 14 levelTimerMax
			"120",    // 15 rolltime
			"55",     // 16 norm
			"4",      // 17 bg
			"50",     // 18 meterValue
			"2",      // 19 meterColor
			"true",   // 20 heboHiddenEnable
			"3",      // 21 heboHiddenTimerNow/Max
			"5",      // 22 heboHiddenYNow
			"19"      // 23 heboHiddenYLimit
		};

		mode.netRecvStats(engine, msg);

		assertEquals(42, engine.statistics.lines);
		assertEquals(3, readInt(mode, "goaltype"));
		assertEquals(55, readInt(mode, "norm"));
	}

	@Test
	void netSendEndGameStatsBuildsMessage() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.owner = engine.owner;
		engine.statistics.time = 5000;
		setInt(mode, "norm", 70);
		setInt(mode, "sectionavgtime", 300);
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 120;
		sectiontime[1] = 240;
		mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();

		mode.netSendEndGameStats(engine);
	}

	@Test
	void netSendOptionsBuildsMessage() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netLobby = new nullpomino.gui.net.NetLobbyFrame();
		mode.netLobby.netPlayerClient = new nullpomino.game.net.NetPlayerClient();

		mode.netSendOptions(engine);
	}

	@Test
	void netRecvOptionsParsesMessage() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		String[] msg = new String[] {"game", "option", "x", "x", "3", "5", "false", "true"};
		mode.netRecvOptions(engine, msg);

		assertEquals(3, readInt(mode, "goaltype"));
		assertEquals(5, readInt(mode, "startlevel"));
		assertFalse(readBoolean(mode, "showsectiontime"));
		assertTrue(readBoolean(mode, "big"));
	}

	@Test
	void netGetGoalTypeReturnsGoaltype() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 6);

		java.lang.reflect.Method m = TimeAttackMode.class.getDeclaredMethod("netGetGoalType");
		m.setAccessible(true);
		assertEquals(6, m.invoke(mode));
	}

	@Test
	void netIsNetRankingViewOK() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);

		java.lang.reflect.Method m = TimeAttackMode.class.getDeclaredMethod(
				"netIsNetRankingViewOK", GameEngine.class);
		m.setAccessible(true);
		assertTrue((boolean) m.invoke(mode, engine));
	}

	// ----------------------------------------------------------------
	// helpers
	// ----------------------------------------------------------------

	private static GameEngine freshEngine(TimeAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
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
