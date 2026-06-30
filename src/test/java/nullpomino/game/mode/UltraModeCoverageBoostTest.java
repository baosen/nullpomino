package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
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
 * Boosts {@link UltraMode} line coverage for the branches still missed by the
 * existing UltraMode tests: the preset-number cursor (16/17) adjust+wrap path,
 * the NET ranking display branches of onSetting/renderSetting, the D-button
 * "enter net ranking" path, the renderLast score-delta and per-event drawMenu
 * switch (every EVENT_* case), the renderResult ranking/PB/replay-status
 * branches, the no-B2B calcScore arms, the per-minute background switch in
 * onLast, the watch/spin-off branches of startGame, and the NET stat/option
 * send/recv pipeline (netSendStats, netRecvStats, netSendEndGameStats,
 * netSendOptions, netRecvOptions, netGetGoalType, netIsNetRankingViewOK).
 */
class UltraModeCoverageBoostTest {

	// ------------------------------------------------------------------
	// onSetting: preset-number cursor 16/17 adjust + wrap (lines 304-306)
	// ------------------------------------------------------------------

	@Test
	void onSettingCursor16PresetNumberRightIncrements() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16);
		setFieldInt(mode, "presetNumber", 0);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(1, readFieldInt(mode, "presetNumber"));
	}

	@Test
	void onSettingCursor16PresetNumberLeftWrapsTo99() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16);
		setFieldInt(mode, "presetNumber", 0);

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(99, readFieldInt(mode, "presetNumber"),
				"presetNumber should wrap from 0 to 99 on LEFT");
	}

	@Test
	void onSettingCursor17PresetNumberRightWrapsToZero() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 17);
		setFieldInt(mode, "presetNumber", 99);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(0, readFieldInt(mode, "presetNumber"),
				"presetNumber should wrap from 99 to 0 on RIGHT");
	}

	// ------------------------------------------------------------------
	// onSetting: NET ranking display mode (line 217)
	// ------------------------------------------------------------------

	@Test
	void onSettingNetRankingDisplayModeDelegates() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		attachNetLobby(mode);
		setFieldBool(mode, "netIsNetRankingDisplayMode", true);
		setFieldInt(mode, "goaltype", 2);

		// No key pressed and no ranking data ready => inner branches are skipped,
		// but the dispatch on line 217 still executes.
		boolean result = mode.onSetting(engine, 0);

		assertTrue(result, "onSetting should stay on the settings screen");
	}

	// ------------------------------------------------------------------
	// onSetting: D button enters net ranking screen (line 345)
	// ------------------------------------------------------------------

	@Test
	void onSettingDButtonEntersNetRankingScreen() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		attachNetLobby(mode);
		setFieldBool(mode, "netIsNetPlay", true);
		setFieldBool(mode, "netIsWatch", false);
		setFieldBool(mode, "big", false);
		setFieldObject(mode, "netCurrentRoomInfo", new NetRoomInfo());
		setMenuState(engine, mode, 0);
		engine.ai = null;

		pressKey(engine, Controller.BUTTON_D);
		mode.onSetting(engine, 0);

		assertTrue(readFieldBool(mode, "netIsNetRankingDisplayMode"),
				"D button should enter the net ranking display mode");
	}

	// ------------------------------------------------------------------
	// renderSetting branches (lines 373, 393)
	// ------------------------------------------------------------------

	@Test
	void renderSettingNetRankingDisplayMode() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		attachNetLobby(mode);
		setFieldBool(mode, "netIsNetRankingDisplayMode", true);

		// netRankingNoDataFlag/Ready default false => "LOADING..." render path.
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingLegacyVersionUsesEnableTSpinLabel() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "version", 0); // legacy version => line 393 branch
		setFieldInt(mode, "menuCursor", 10);

		mode.renderSetting(engine, 0);
	}

	// ------------------------------------------------------------------
	// startGame branches (lines 421, 429)
	// ------------------------------------------------------------------

	@Test
	void startGameWatchModeSilencesBgm() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldBool(mode, "netIsWatch", true);

		mode.startGame(engine, 0);

		assertEquals(BGMStatus.BGM_NOTHING, engine.owner.bgmStatus.bgm,
				"Watch mode should silence the BGM");
	}

	@Test
	void startGameTspinTypeZeroDisablesTspin() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "version", 1);
		setFieldInt(mode, "tspinEnableType", 0);

		mode.startGame(engine, 0);

		assertFalse(engine.tspinEnable, "tspinEnableType 0 should disable T-Spins");
	}

	// ------------------------------------------------------------------
	// calcScore: the no-B2B arms (592, 602, 609, 620, 637) and the
	// non-T-Spin double/triple events (646-650)
	// ------------------------------------------------------------------

	@Test
	void calcScoreEZSpinWithoutB2B() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.tspinez = true;
		engine.b2b = false;

		mode.calcScore(engine, 0, 1);

		// EZ spin, no B2B => 120 * (level + 1) = 120
		assertEquals(120, engine.statistics.score);
		assertEquals(12, readFieldInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTSpinSingleMiniWithoutB2B() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinmini = true;
		engine.b2b = false;

		mode.calcScore(engine, 0, 1);

		// T-Spin single mini, no B2B => 200 (line 602)
		assertEquals(200, engine.statistics.score);
		assertEquals(7, readFieldInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTSpinSingleWithoutB2B() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinmini = false;
		engine.b2b = false;

		mode.calcScore(engine, 0, 1);

		// T-Spin single, no B2B => 800 (line 609)
		assertEquals(800, engine.statistics.score);
		assertEquals(8, readFieldInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTSpinDoubleMiniWithoutB2B() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = true;
		engine.b2b = false;

		mode.calcScore(engine, 0, 2);

		// T-Spin double mini, no B2B => 400 * (level + 1) = 400 (line 620)
		assertEquals(400, engine.statistics.score);
		assertEquals(9, readFieldInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTSpinTripleWithoutB2B() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.b2b = false;

		mode.calcScore(engine, 0, 3);

		// T-Spin triple, no B2B => 1600 (line 637)
		assertEquals(1600, engine.statistics.score);
		assertEquals(11, readFieldInt(mode, "lastevent"));
	}

	@Test
	void calcScoreFlatDoubleEvent() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.tspin = false;

		mode.calcScore(engine, 0, 2);

		// Double, no T-Spin => 300 (lines 646-647)
		assertEquals(300, engine.statistics.score);
		assertEquals(2, readFieldInt(mode, "lastevent"));
	}

	@Test
	void calcScoreFlatTripleEvent() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.tspin = false;

		mode.calcScore(engine, 0, 3);

		// Triple, no T-Spin => 500 (lines 649-650)
		assertEquals(500, engine.statistics.score);
		assertEquals(3, readFieldInt(mode, "lastevent"));
	}

	// ------------------------------------------------------------------
	// onLast: per-minute background switch (lines 742-744)
	// ------------------------------------------------------------------

	@Test
	void onLastPerMinuteBackgroundSwitch() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;
		setFieldBool(mode, "netIsWatch", false);
		setFieldInt(mode, "goaltype", 4); // 5-minute game so limit not yet reached
		engine.statistics.time = 3600; // exactly one minute => background switch

		int beforeBg = engine.owner.backgroundStatus.bg;
		mode.onLast(engine, 0);

		assertTrue(engine.owner.backgroundStatus.fadesw,
				"Per-minute mark should enable background fade");
		assertEquals(beforeBg + 1, engine.owner.backgroundStatus.fadebg);
	}

	// ------------------------------------------------------------------
	// renderLast: score-delta display (line 481) and the event switch
	// (lines 504-553)
	// ------------------------------------------------------------------

	@Test
	void renderLastShowsScoreDelta() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setFieldInt(mode, "lastscore", 500);
		setFieldInt(mode, "scgettime", 10); // < 120 => "(+score)" path on line 481
		setFieldInt(mode, "lastevent", 0); // EVENT_NONE => skip switch this time

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastDrawsEveryEventCase() throws Exception {
		// Walk every lastevent value through renderLast so each drawMenuFont
		// case (lines 504-553) executes, with and without B2B/combo.
		int[] events = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
		for (int ev : events) {
			for (boolean b2b : new boolean[]{false, true}) {
				UltraMode mode = new UltraMode();
				GameEngine engine = freshEngine(mode, false);
				mode.playerInit(engine, 0);
				engine.owner.menuOnly = false;
				engine.stat = GameEngine.Status.MOVE;
				engine.nowPieceObject = new Piece(Piece.PIECE_T);
				setFieldInt(mode, "lastevent", ev);
				setFieldInt(mode, "scgettime", 0); // < 120 => event switch runs
				setFieldInt(mode, "lastpiece", Piece.PIECE_T);
				setFieldInt(mode, "lastcombo", 3); // >= 2 => combo line 552-553
				setFieldBool(mode, "lastb2b", b2b);

				mode.renderLast(engine, 0); // must not throw
			}
		}
	}

	// ------------------------------------------------------------------
	// renderResult branches (759-766, 776, 780, 782)
	// ------------------------------------------------------------------

	@Test
	void renderResultWithRanksPbAndSending() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		int[] rankingRank = (int[]) readFieldObject(mode, "rankingRank");
		rankingRank[0] = 0; // != -1 => lines 759-760
		rankingRank[1] = 1; // != -1 => lines 765-766

		setFieldBool(mode, "netIsPB", true);                 // line 776
		setFieldBool(mode, "netIsNetPlay", true);
		setFieldInt(mode, "netReplaySendStatus", 1);         // line 780

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultRetryStatus() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		setFieldBool(mode, "netIsNetPlay", true);
		setFieldBool(mode, "netIsWatch", false);
		setFieldInt(mode, "netReplaySendStatus", 2);         // line 782

		mode.renderResult(engine, 0);
	}

	// ------------------------------------------------------------------
	// NET pipeline (lines 888-988, 995, 1003)
	// ------------------------------------------------------------------

	@Test
	void netSendStatsBuildsAndSends() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.owner.backgroundStatus.fadesw = true; // exercise the fadebg branch

		invoke(mode, "netSendStats", new Class[]{GameEngine.class}, engine);
	}

	@Test
	void netRecvStatsParsesMessage() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		String[] msg = new String[21];
		// indices 0..3 are header fields, parsing starts at index 4
		msg[0] = "game"; msg[1] = "stats"; msg[2] = "x"; msg[3] = "y";
		msg[4] = "12345";   // score
		msg[5] = "20";      // lines
		msg[6] = "50";      // totalPieceLocked
		msg[7] = "600";     // time
		msg[8] = "1234.5";  // spm
		msg[9] = "30.0";    // lpm
		msg[10] = "55.5";   // spl
		msg[11] = "0";      // goaltype
		msg[12] = "true";   // gameActive
		msg[13] = "true";   // timerActive
		msg[14] = "100";    // lastscore
		msg[15] = "5";      // scgettime
		msg[16] = "4";      // lastevent
		msg[17] = "true";   // lastb2b
		msg[18] = "2";      // lastcombo
		msg[19] = "1";      // lastpiece
		msg[20] = "3";      // bg

		invoke(mode, "netRecvStats",
				new Class[]{GameEngine.class, String[].class}, engine, msg);

		assertEquals(12345, engine.statistics.score);
		assertEquals(20, engine.statistics.lines);
		assertEquals(0, readFieldInt(mode, "goaltype"));
	}

	@Test
	void netSendEndGameStatsSends() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);

		invoke(mode, "netSendEndGameStats", new Class[]{GameEngine.class}, engine);
	}

	@Test
	void netSendOptionsSends() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);

		invoke(mode, "netSendOptions", new Class[]{GameEngine.class}, engine);
	}

	@Test
	void netRecvOptionsParsesMessage() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		String[] msg = new String[21];
		msg[0] = "game"; msg[1] = "option"; msg[2] = "x"; msg[3] = "y";
		msg[4] = "8";       // gravity
		msg[5] = "256";     // denominator
		msg[6] = "5";       // are
		msg[7] = "6";       // areLine
		msg[8] = "7";       // lineDelay
		msg[9] = "30";      // lockDelay
		msg[10] = "14";     // das
		msg[11] = "3";      // bgmno
		msg[12] = "false";  // big
		msg[13] = "1";      // goaltype
		msg[14] = "2";      // tspinEnableType
		msg[15] = "true";   // enableTSpinKick
		msg[16] = "true";   // enableB2B
		msg[17] = "false";  // enableCombo
		msg[18] = "4";      // presetNumber
		msg[19] = "1";      // spinCheckType
		msg[20] = "true";   // tspinEnableEZ

		invoke(mode, "netRecvOptions",
				new Class[]{GameEngine.class, String[].class}, engine, msg);

		assertEquals(8, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(1, readFieldInt(mode, "goaltype"));
		assertEquals(2, readFieldInt(mode, "tspinEnableType"));
		assertEquals(4, readFieldInt(mode, "presetNumber"));
	}

	@Test
	void netGetGoalTypeReturnsGoaltype() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		setFieldInt(mode, "goaltype", 3);

		int g = (int) invoke(mode, "netGetGoalType", new Class[]{});

		assertEquals(3, g);
	}

	@Test
	void netIsNetRankingViewOkReflectsBigAndAi() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		setFieldBool(mode, "big", false);
		engine.ai = null;
		boolean ok = (boolean) invoke(mode, "netIsNetRankingViewOK",
				new Class[]{GameEngine.class}, engine);
		assertTrue(ok, "Non-big, no-AI run should allow leaderboard");

		setFieldBool(mode, "big", true);
		boolean blocked = (boolean) invoke(mode, "netIsNetRankingViewOK",
				new Class[]{GameEngine.class}, engine);
		assertFalse(blocked, "Big mode should block leaderboard");
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private static GameEngine freshEngine(UltraMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void placeOneBlock(GameEngine engine) {
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
	}

	/** Give the mode a usable (offline) net lobby + client so send() is a no-op. */
	private static void attachNetLobby(UltraMode mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		NetPlayerClient client = new NetPlayerClient();
		setFieldObject(lobby, "netPlayerClient", client);
		setFieldObject(mode, "netLobby", lobby);
	}

	private static void setMenuState(GameEngine engine, UltraMode mode, int cursor)
			throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", 0);
	}

	private static void pressKey(GameEngine e, int btn) {
		e.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) e.ctrl.buttonTime[i] = 0;
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static boolean readFieldBool(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static Object readFieldObject(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).get(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setFieldBool(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setFieldObject(Object obj, String name, Object value) throws Exception {
		findField(obj.getClass(), name).set(obj, value);
	}

	private static Object invoke(Object obj, String name, Class<?>[] sig, Object... args)
			throws Exception {
		Class<?> c = obj.getClass();
		while (c != null) {
			try {
				Method m = c.getDeclaredMethod(name, sig);
				m.setAccessible(true);
				return m.invoke(obj, args);
			} catch (NoSuchMethodException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchMethodException(name);
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
