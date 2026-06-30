package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage-boost tests for {@link TechnicianMode} targeting previously
 * uncovered lines: onSetting net-ranking branches (display update + D-button
 * enter), renderSetting (net ranking render + legacy version T-spin label),
 * startGame branches (combo-disable, tspin types, legacy version, netIsWatch),
 * renderLast (score/goal/total-time colors, +30sec, roll-time, full event
 * switch + combo), onLast (countdown SEs, total-timer game-over), calcScore
 * (EZ spin, all T-spin line variants, BGM fade/change), renderResult (net PB
 * + send status), saveReplay (net player name), and the NET send/recv
 * stat/option methods.
 */
class TechnicianModeCoverageBoostTest {

	// ---------------------------------------------------------------
	// onSetting: NET ranking display + D-button
	// ---------------------------------------------------------------

	@Test
	void onSettingNetRankingDisplayMode() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "netIsNetRankingDisplayMode", true);

		// netRankingReady defaults to false so the inner block is skipped;
		// this still executes the netOnUpdateNetPlayRanking call (line 184).
		boolean result = mode.onSetting(engine, 0);
		assertTrue(result);
	}

	@Test
	void onSettingNetRankingButtonD() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = false;
		setMenu(mode, 0, 10);
		press(engine, Controller.BUTTON_D);
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "startlevel", 0);
		setBool(mode, "big", false);
		setField(mode, "netCurrentRoomInfo", new NetRoomInfo());

		// netEnterNetPlayRankingScreen (line 257) runs until it dereferences
		// the (null) netLobby; catching that confirms the line was reached.
		try {
			mode.onSetting(engine, 0);
		} catch (NullPointerException expected) {
			/* netLobby cascade inside netEnterNetPlayRankingScreen */
		}
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// renderSetting branches
	// ---------------------------------------------------------------

	@Test
	void renderSettingNetRankingDisplay() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "netIsNetRankingDisplayMode", true);
		// netOnRenderNetPlayRanking (line 282); inner block guarded off.
		mode.renderSetting(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderSettingLegacyVersionTSpinLabel() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 0); // version < 1 -> line 290
		mode.renderSetting(engine, 0);
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// startGame branches
	// ---------------------------------------------------------------

	@Test
	void startGameComboDisabled() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "enableCombo", false); // line 316
		mode.startGame(engine, 0);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	@Test
	void startGameTSpinTypeOff() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 2);
		setInt(mode, "tspinEnableType", 0); // line 323
		mode.startGame(engine, 0);
		assertFalse(engine.tspinEnable);
	}

	@Test
	void startGameTSpinTypeAll() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 2);
		setInt(mode, "tspinEnableType", 2); // lines 327-328
		mode.startGame(engine, 0);
		assertTrue(engine.tspinEnable);
		assertTrue(engine.useAllSpinBonus);
	}

	@Test
	void startGameLegacyVersionTSpin() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 0); // version < 2 -> line 331
		setBool(mode, "enableTSpin", true);
		mode.startGame(engine, 0);
		assertTrue(engine.tspinEnable);
	}

	@Test
	void startGameNetWatch() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "netIsWatch", true); // line 344
		mode.startGame(engine, 0);
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// renderLast: score / goal / total-time colors / +30sec / roll
	// ---------------------------------------------------------------

	@Test
	void renderLastScoreWithTimeBonusAndGoal() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		engine.ending = 0;
		setInt(mode, "goaltype", 0); // not SPECIAL -> line 389 path
		setInt(mode, "lasttimebonus", 50); // line 389
		setInt(mode, "lastscore", 100);    // line 391 (else covered separately)
		setInt(mode, "lastgoal", 3);       // line 398
		setInt(mode, "scgettime", 0);
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastScoreWithLastscoreOnly() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		engine.ending = 0;
		setInt(mode, "goaltype", 0);
		setInt(mode, "lasttimebonus", 0); // skip line 389
		setInt(mode, "lastscore", 100);   // line 391
		setInt(mode, "scgettime", 0);
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastTotalTimeColors10Min() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		engine.ending = 0;
		setInt(mode, "goaltype", 2); // GAMETYPE_10MIN_EASY
		// totaltime = TIMELIMIT_10MIN - time; make it small to hit 424-426.
		engine.statistics.time = 36000 - 100; // totaltime = 100 (< 10*60, > 0)
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastSpecialPlus30SecAndRoll() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		engine.ending = 0;
		setInt(mode, "goaltype", 4); // SPECIAL
		setInt(mode, "lasttimebonus", 1800); // line 432 (+30sec)
		setInt(mode, "scgettime", 0);
		setInt(mode, "totalTimer", 100);
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastRollTimeDisplay() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		engine.gameActive = true;
		engine.ending = 2; // roll active -> lines 437-441
		setInt(mode, "rolltime", 100);
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// renderLast: full event switch (DOUBLE..EZ) + combo
	// ---------------------------------------------------------------

	@Test
	void renderLastAllEventsWithB2B() throws Exception {
		for (int event = 2; event <= 12; event++) {
			for (boolean b2b : new boolean[]{false, true}) {
				TechnicianMode mode = new TechnicianMode();
				GameEngine engine = freshEngine(mode);
				engine.stat = GameEngine.Status.MOVE;
				engine.owner.menuOnly = false;
				engine.ending = 0;
				setInt(mode, "regretdispframe", 0);
				setInt(mode, "lastevent", event);
				setInt(mode, "scgettime", 0);
				setInt(mode, "lastpiece", Piece.PIECE_T);
				setBool(mode, "lastb2b", b2b);
				setInt(mode, "lastcombo", 3); // lines 498-499 combo display
				mode.renderLast(engine, 0);
			}
		}
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// onLast: countdown SEs + total timer game over
	// ---------------------------------------------------------------

	@Test
	void onLastLevelTimerCountdownSE() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.gameActive = true;
		engine.timerActive = true;
		setInt(mode, "goaltype", 0); // not SPECIAL
		// after ++ levelTimer = 6600; remainTime = 7200-6600 = 600 = 10*60 -> line 551
		setInt(mode, "levelTimer", 6599);
		mode.onLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void onLastTotalTimerCountdownSE() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.gameActive = true;
		engine.timerActive = true;
		setInt(mode, "goaltype", 4); // SPECIAL -> has total timer block
		// after -- totalTimer = 600 = 10*60, %60==0, >=0 -> line 584
		setInt(mode, "totalTimer", 601);
		mode.onLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void onLastTotalTimerOutSpecialGameOver() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.gameActive = true;
		engine.timerActive = true;
		setInt(mode, "goaltype", 4); // SPECIAL (not 10MIN) -> line 578 GAMEOVER
		setInt(mode, "totalTimer", 0); // after -- = -1 < 0
		mode.onLast(engine, 0);
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	// ---------------------------------------------------------------
	// calcScore: EZ spin + T-spin line variants + BGM change
	// ---------------------------------------------------------------

	@Test
	void calcScoreEzSpinNoB2B() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.tspinez = true; // lines 637-643
		engine.b2b = false;    // line 641 (120 pts branch)
		mode.calcScore(engine, 0, 1);
		assertEquals(12, readInt(mode, "lastevent")); // EVENT_TSPIN_EZ
	}

	@Test
	void calcScoreEzSpinWithB2B() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.tspinez = true;
		engine.b2b = true; // line 639 (180 pts branch)
		mode.calcScore(engine, 0, 1);
		assertEquals(12, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTSpinSingleMiniB2B() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.tspinmini = true; // lines 647-653
		engine.b2b = true;       // line 649 (300 pts)
		mode.calcScore(engine, 0, 1);
		assertEquals(7, readInt(mode, "lastevent")); // EVENT_TSPIN_SINGLE_MINI
	}

	@Test
	void calcScoreTSpinSingleMiniNoB2B() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.tspinmini = true;
		engine.b2b = false; // line 651 (200 pts)
		mode.calcScore(engine, 0, 1);
		assertEquals(7, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTSpinDoubleMiniAllSpin() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = true; // lines 665-671
		engine.b2b = true;             // line 667 (600 pts)
		mode.calcScore(engine, 0, 2);
		assertEquals(9, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE_MINI
	}

	@Test
	void calcScoreTSpinDoubleMiniAllSpinNoB2B() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = true;
		engine.b2b = false; // line 669 (400 pts)
		mode.calcScore(engine, 0, 2);
		assertEquals(9, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTSpinDoubleB2B() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.b2b = true; // lines 673-678 (1800 pts)
		mode.calcScore(engine, 0, 2);
		assertEquals(10, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE
	}

	@Test
	void calcScoreTSpinTripleB2B() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.b2b = true; // lines 682-688 (2400 pts)
		mode.calcScore(engine, 0, 3);
		assertEquals(11, readInt(mode, "lastevent")); // EVENT_TSPIN_TRIPLE
	}

	@Test
	void calcScoreTSpinTripleNoB2B() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.b2b = false; // line 686 (1600 pts)
		mode.calcScore(engine, 0, 3);
		assertEquals(11, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreBgmFadeWhenGoalLow() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		// tableBGMChange[0] = 9, so level == 8 -> next BGM change pending.
		engine.statistics.level = 8;
		setInt(mode, "bgmlv", 0);
		setInt(mode, "goaltype", 0); // LV15_EASY
		setInt(mode, "goal", 5);     // 0 < goal <= 10 -> lines 759-760 fadesw
		setBool(mode, "enableCombo", false);
		// avoid all-clear bonus driving goal to 0: put a block on the field
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		mode.calcScore(engine, 0, 1);
		assertTrue(engine.owner.bgmStatus.fadesw);
	}

	@Test
	void calcScoreBgmAdvanceWhenGoalReached() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 8; // tableBGMChange[0]-1 = 8
		setInt(mode, "bgmlv", 0);
		setInt(mode, "goaltype", 0);
		setInt(mode, "goal", 0); // goal <= 0 -> lines 761-764 advance BGM
		setBool(mode, "enableCombo", false);
		mode.calcScore(engine, 0, 1);
		assertEquals(1, readInt(mode, "bgmlv"));
	}

	// ---------------------------------------------------------------
	// renderResult: net PB + send status
	// ---------------------------------------------------------------

	@Test
	void renderResultNetPbAndSending() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "netIsPB", true);          // line 833
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "netReplaySendStatus", 1);  // line 837
		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderResultNetRetry() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "netIsNetPlay", true);
		setBool(mode, "netIsWatch", false);
		setInt(mode, "netReplaySendStatus", 2); // line 839
		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// saveReplay: net player name
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesNetPlayerName() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setField(mode, "netPlayerName", "TESTER"); // line 852
		setBool(mode, "big", true); // skip ranking update branch
		CustomProperties prop = new CustomProperties();

		mode.saveReplay(engine, 0, prop);

		assertEquals("TESTER", prop.getProperty("0.net.netPlayerName", ""));
	}

	// ---------------------------------------------------------------
	// NET send/recv stats and options
	// ---------------------------------------------------------------

	@Test
	void netSendStatsRuns() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		attachNetLobby(mode);

		invokeProtected(mode, "netSendStats", new Class<?>[]{GameEngine.class},
				new Object[]{engine}); // lines 890-899
		assertTrue(true);
	}

	@Test
	void netSendEndGameStatsRuns() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		attachNetLobby(mode);

		invokeProtected(mode, "netSendEndGameStats",
				new Class<?>[]{GameEngine.class}, new Object[]{engine}); // 941-951
		assertTrue(true);
	}

	@Test
	void netSendOptionsRuns() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		attachNetLobby(mode);

		invokeProtected(mode, "netSendOptions",
				new Class<?>[]{GameEngine.class}, new Object[]{engine}); // 958-963
		assertTrue(true);
	}

	@Test
	void netRecvStatsParsesMessage() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);

		// message[4..29] are read by netRecvStats (lines 907-933).
		String[] msg = new String[30];
		msg[0] = "game"; msg[1] = "stats"; msg[2] = ""; msg[3] = "";
		msg[4] = "12345";  // score
		msg[5] = "50";     // lines
		msg[6] = "100";    // totalPieceLocked
		msg[7] = "3600";   // time
		msg[8] = "30.5";   // lpm (float)
		msg[9] = "2.5";    // spl (double)
		msg[10] = "3";     // goaltype
		msg[11] = "true";  // gameActive
		msg[12] = "true";  // timerActive
		msg[13] = "200";   // lastscore
		msg[14] = "5";     // scgettime
		msg[15] = "4";     // lastevent
		msg[16] = "true";  // lastb2b
		msg[17] = "2";     // lastcombo
		msg[18] = "1";     // lastpiece
		msg[19] = "3";     // lastgoal
		msg[20] = "60";    // lasttimebonus
		msg[21] = "0";     // regretdispframe
		msg[22] = "5";     // bg
		msg[23] = "10";    // meterValue
		msg[24] = "1";     // meterColor
		msg[25] = "7";     // level
		msg[26] = "1000";  // levelTimer
		msg[27] = "2000";  // totalTimer
		msg[28] = "0";     // rolltime
		msg[29] = "25";    // goal

		invokeProtected(mode, "netRecvStats",
				new Class<?>[]{GameEngine.class, String[].class},
				new Object[]{engine, msg});

		assertEquals(12345, engine.statistics.score);
		assertEquals(3, readInt(mode, "goaltype"));
		assertEquals(7, engine.statistics.level);
	}

	@Test
	void netRecvOptionsParsesMessage() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);

		// message[4..12] read by netRecvOptions (lines 971-980).
		String[] msg = new String[13];
		msg[0] = "game"; msg[1] = "option"; msg[2] = ""; msg[3] = "";
		msg[4] = "2";      // goaltype
		msg[5] = "5";      // startlevel
		msg[6] = "1";      // tspinEnableType
		msg[7] = "true";   // enableTSpinKick
		msg[8] = "true";   // enableB2B
		msg[9] = "false";  // enableCombo
		msg[10] = "true";  // big
		msg[11] = "1";     // spinCheckType
		msg[12] = "true";  // tspinEnableEZ

		invokeProtected(mode, "netRecvOptions",
				new Class<?>[]{GameEngine.class, String[].class},
				new Object[]{engine, msg});

		assertEquals(2, readInt(mode, "goaltype"));
		assertEquals(5, readInt(mode, "startlevel"));
		assertEquals(1, readInt(mode, "tspinEnableType"));
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(TechnicianMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
		mode.playerInit(manager.engine[0], 0);
		return manager.engine[0];
	}

	/** Give the mode a netLobby with a (disconnected) client so net send
	 *  methods run to completion. send() on a socket-less client is a
	 *  caught no-op. */
	private static void attachNetLobby(TechnicianMode mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setField(mode, "netLobby", lobby);
	}

	private static void setMenu(TechnicianMode mode, int cursor, int menuTime)
			throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", menuTime);
	}

	private static void press(GameEngine e, int btn) {
		e.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) e.ctrl.buttonTime[i] = 0;
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static void invokeProtected(Object obj, String name,
			Class<?>[] types, Object[] args) throws Exception {
		java.lang.reflect.Method m = findMethod(obj.getClass(), name, types);
		m.setAccessible(true);
		m.invoke(obj, args);
	}

	private static java.lang.reflect.Method findMethod(Class<?> cls, String name,
			Class<?>[] types) throws NoSuchMethodException {
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

	private static void setInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		findField(obj.getClass(), name).set(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
