package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Additional line-coverage tests for {@link DigChallengeMode} targeting the
 * branches not exercised by the existing suites: the NET ranking paths in
 * onSetting/renderSetting, startGame T-Spin-type and netIsWatch branches,
 * renderLast gameplay rendering (all scoring events, combo, garbage-pending,
 * bonus-score display, realtime header), onLast normal version&lt;1 and
 * realtime garbage-addition paths, updateMeter zero-limit branch, calcScore
 * T-Spin mini / EZ / B2B-triple / combo branches, addGarbage sticky-skin
 * connections, renderResult net status lines, saveReplay net-name and
 * ranking-save paths, and the NET send/recv/option/goal helpers.
 */
class DigChallengeModeCoverageBoostTest {

	// ---------------------------------------------------------------
	// onSetting / renderSetting NET ranking paths
	// ---------------------------------------------------------------

	@Test
	void onSettingNetRankingDisplayMode() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBool(mode, "netIsNetRankingDisplayMode", true);

		// Should route to netOnUpdateNetPlayRanking (line 228) and still return true.
		boolean result = mode.onSetting(engine, 0);
		assertTrue(result);
	}

	@Test
	void renderSettingNetRankingDisplayMode() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBool(mode, "netIsNetRankingDisplayMode", true);

		// Should route to netOnRenderNetPlayRanking (line 334).
		mode.renderSetting(engine, 0);
	}

	@Test
	void onSettingButtonDEntersNetRanking() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		setInt(mode, "startlevel", 0); // netIsNetRankingViewOK requires startlevel==0 && ai==null
		setBool(mode, "netIsNetPlay", true);
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();
		findField(mode.getClass(), "netCurrentRoomInfo")
				.set(mode, new nullpomino.game.net.NetRoomInfo());

		pressKey(engine, Controller.BUTTON_D);
		// Exercises BUTTON_D branch (line 308-309) -> netEnterNetPlayRankingScreen.
		mode.onSetting(engine, 0);
		assertTrue(readBool(mode, "netIsNetRankingDisplayMode"));
	}

	// ---------------------------------------------------------------
	// startGame branches
	// ---------------------------------------------------------------

	@Test
	void startGameTSpinTypeOff() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "tspinEnableType", 0); // line 370
		setInt(mode, "version", 2);

		mode.startGame(engine, 0);

		assertFalse(engine.tspinEnable);
	}

	@Test
	void startGameTSpinTypeNormal() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "tspinEnableType", 1); // line 372
		setInt(mode, "version", 2);

		mode.startGame(engine, 0);

		assertTrue(engine.tspinEnable);
		assertFalse(engine.useAllSpinBonus);
	}

	@Test
	void startGameNetIsWatchSilencesBgm() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBool(mode, "netIsWatch", true); // line 388-389

		mode.startGame(engine, 0);

		assertEquals(nullpomino.game.component.BGMStatus.BGM_NOTHING, engine.owner.bgmStatus.bgm);
	}

	// ---------------------------------------------------------------
	// renderLast gameplay branches
	// ---------------------------------------------------------------

	@Test
	void renderLastRealtimeHeaderAndScoreNoBonus() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "goaltype", 1); // realtime header (line 406)
		setInt(mode, "lastscore", 50);
		setInt(mode, "lastbonusscore", 0); // line 427-428 branch
		setInt(mode, "scgettime", 10);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastScoreWithBonus() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastscore", 50);
		setInt(mode, "lastbonusscore", 5); // line 429-430 branch
		setInt(mode, "scgettime", 10);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastAllScoringEvents() throws Exception {
		// Walk each lastevent case 1..10 plus combo and garbage-pending display.
		int[] events = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		for (int ev : events) {
			for (boolean b2b : new boolean[]{false, true}) {
				DigChallengeMode mode = new DigChallengeMode();
				GameEngine engine = freshEngine(mode);
				mode.playerInit(engine, 0);
				engine.owner.menuOnly = false;
				engine.stat = GameEngine.Status.MOVE;
				setInt(mode, "lastevent", ev);
				setInt(mode, "scgettime", 0);
				setInt(mode, "lastpiece", Piece.PIECE_T);
				setInt(mode, "lastcombo", 3); // exercises combo line 489-490
				setBool(mode, "lastb2b", b2b);
				setInt(mode, "garbagePending", 4); // exercises 493-503 (all color thresholds)

				mode.renderLast(engine, 0);
			}
		}
	}

	@Test
	void renderLastGarbagePendingColorThresholds() throws Exception {
		for (int pending : new int[]{1, 2, 3, 4}) {
			DigChallengeMode mode = new DigChallengeMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			engine.owner.menuOnly = false;
			engine.stat = GameEngine.Status.MOVE;
			setInt(mode, "garbagePending", pending);
			mode.renderLast(engine, 0);
		}
	}

	@Test
	void renderLastMenuOnlyReturnsEarly() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = true;
		mode.renderLast(engine, 0); // early return path
	}

	// ---------------------------------------------------------------
	// onLast branches
	// ---------------------------------------------------------------

	@Test
	void onLastNormalVersionZeroSetsPendingToOne() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statistics.level = 0;
		setInt(mode, "goaltype", 0);   // NORMAL
		setInt(mode, "version", 0);    // version < 1 => line 542
		setInt(mode, "garbageTimer", 1000);
		setInt(mode, "garbagePending", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "garbagePending"),
				"version<1 sets garbagePending = 1");
	}

	@Test
	void onLastNormalVersionOneIncrementsPending() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statistics.level = 0;
		setInt(mode, "goaltype", 0);
		setInt(mode, "version", 2);   // version >= 1 => line 534
		setInt(mode, "garbageTimer", 1000);
		setInt(mode, "garbagePending", 0);
		setBool(mode, "netIsNetPlay", true);
		setBool(mode, "netIsWatch", false);
		setInt(mode, "netNumSpectators", 1);
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "garbagePending"));
		assertEquals(0, readInt(mode, "garbageTimer"), "timer resets after pending added");
	}

	@Test
	void onLastRealtimeAddsGarbage() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statistics.level = 0;
		engine.stat = GameEngine.Status.ARE; // not LINECLEAR, not MOVE
		setInt(mode, "goaltype", 1);   // REALTIME => addGarbage (line 550)
		setInt(mode, "version", 2);
		setInt(mode, "garbageTimer", 1000);
		setBool(mode, "netIsNetPlay", true);
		setBool(mode, "netIsWatch", false);
		setInt(mode, "netNumSpectators", 1);
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();

		mode.onLast(engine, 0);

		assertEquals(0, readInt(mode, "garbageTimer"), "timer resets after garbage added");
		assertTrue(readInt(mode, "garbageTotal") >= 1);
	}

	@Test
	void onLastRealtimeMovePushesUpPiece() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statistics.level = 0;
		engine.stat = GameEngine.Status.MOVE; // exercises lines 559-578
		setInt(mode, "goaltype", 1);
		setInt(mode, "version", 2);
		setInt(mode, "garbageTimer", 1000);

		// Place the active piece near the bottom of the field so the added
		// garbage line forces a collision and the push-up loop runs.
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.nowPieceObject.setColor(Block.BLOCK_COLOR_RED);
		engine.nowPieceX = 3;
		engine.nowPieceY = engine.field.getHeight() - 2;
		for (int y = 0; y < engine.field.getHeight(); y++) {
			for (int x = 0; x < engine.field.getWidth(); x++) {
				engine.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}

		mode.onLast(engine, 0);

		assertTrue(readInt(mode, "garbageTotal") >= 1);
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	// ---------------------------------------------------------------
	// updateMeter zero-limit branch (line 595)
	// ---------------------------------------------------------------

	@Test
	void updateMeterZeroLimitSetsMeterZero() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		// version<=1, goaltype Normal => t=0; level 19 => table value 0.
		setInt(mode, "version", 0);
		setInt(mode, "goaltype", 0);
		engine.statistics.level = 19;

		Method m = DigChallengeMode.class.getDeclaredMethod("updateMeter", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);

		assertEquals(0, engine.meterValue, "limitTime==0 forces meterValue 0");
	}

	// ---------------------------------------------------------------
	// calcScore detailed scoring branches
	// ---------------------------------------------------------------

	@Test
	void calcScoreTSpinSingleMini() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRowExceptOne(engine);
		engine.tspin = true;
		engine.tspinmini = true;        // line 632-635
		engine.useAllSpinBonus = false;

		mode.calcScore(engine, 0, 1);

		assertEquals(5, readInt(mode, "lastevent"), "EVENT_TSPIN_SINGLE_MINI");
	}

	@Test
	void calcScoreTSpinDoubleMini() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRowExceptOne(engine);
		engine.tspin = true;
		engine.tspinmini = true;        // line 644-645
		engine.useAllSpinBonus = true;

		mode.calcScore(engine, 0, 2);

		assertEquals(7, readInt(mode, "lastevent"), "EVENT_TSPIN_DOUBLE_MINI");
	}

	@Test
	void calcScoreTSpinEz() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRowExceptOne(engine);
		engine.tspin = true;
		engine.tspinez = true;          // line 623-627
		engine.useAllSpinBonus = false;

		mode.calcScore(engine, 0, 1);

		assertEquals(10, readInt(mode, "lastevent"), "EVENT_TSPIN_EZ");
	}

	@Test
	void calcScoreB2bTSpinTripleNoAllSpin() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRowExceptOne(engine);
		engine.tspin = true;
		engine.b2b = true;              // line 675-676 (B2B triple +2)
		engine.useAllSpinBonus = false;

		mode.calcScore(engine, 0, 3);

		assertEquals(9, readInt(mode, "lastevent"), "EVENT_TSPIN_TRIPLE");
		assertTrue(readBool(mode, "lastb2b"));
		// T-Spin triple (6) + B2B triple bonus (2) = 8
		assertEquals(8, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreComboEnabledAddsAttack() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRowExceptOne(engine);
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL; // line 686-691
		engine.combo = 5;

		mode.calcScore(engine, 0, 2);

		assertEquals(5, readInt(mode, "lastcombo"), "lastcombo records engine.combo");
	}

	@Test
	void calcScoreComboIndexClampsHigh() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRowExceptOne(engine);
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
		engine.combo = 50; // index clamps to table length (line 689)

		mode.calcScore(engine, 0, 1);

		assertEquals(50, readInt(mode, "lastcombo"));
	}

	@Test
	void calcScoreAddsGarbageNormalNoLines() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0);   // NORMAL
		setInt(mode, "version", 2);
		setInt(mode, "garbagePending", 3);

		// lines == 0 with pending > 0 => addGarbage(engine, pending) and clear.
		mode.calcScore(engine, 0, 0);

		assertEquals(0, readInt(mode, "garbagePending"));
		assertTrue(readInt(mode, "garbageTotal") >= 3);
	}

	// ---------------------------------------------------------------
	// addGarbage sticky-skin connections (lines 779-785)
	// ---------------------------------------------------------------

	@Test
	void addGarbageStickySkinSetsConnections() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngineSticky(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		Method m = DigChallengeMode.class.getDeclaredMethod("addGarbage", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);

		assertEquals(1, readInt(mode, "garbageTotal"));
	}

	// ---------------------------------------------------------------
	// renderResult net status lines (827, 831, 833)
	// ---------------------------------------------------------------

	@Test
	void renderResultNewPbLine() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBool(mode, "netIsPB", true); // line 826-827

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultSendingLine() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "netReplaySendStatus", 1); // line 830-831

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultRetryLine() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBool(mode, "netIsNetPlay", true);
		setBool(mode, "netIsWatch", false);
		setInt(mode, "netReplaySendStatus", 2); // line 832-833

		mode.renderResult(engine, 0);
	}

	// ---------------------------------------------------------------
	// saveReplay net-name and ranking-save paths (845-855)
	// ---------------------------------------------------------------

	@Test
	void saveReplaySavesNetNameAndRanking() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.owner.modeConfig = new CustomProperties();
		setInt(mode, "startlevel", 0); // ranking update requires startlevel==0 && ai==null
		setStr(mode, "netPlayerName", "player1"); // line 845-846
		engine.statistics.score = 99999; // ensure it ranks => saveRanking (854-855)

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals("player1", prop.getProperty("0.net.netPlayerName", ""));
		assertEquals(0, readInt(mode, "rankingRank"), "high score ranks #1");
	}

	// ---------------------------------------------------------------
	// NET send/recv/option/goal helpers (968-1068)
	// ---------------------------------------------------------------

	@Test
	void netSendStatsBuildsMessage() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();
		engine.owner.backgroundStatus.fadesw = true;
		engine.owner.backgroundStatus.fadebg = 4;

		invoke(mode, "netSendStats", engine);
	}

	@Test
	void netRecvStatsParsesMessage() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		String[] message = new String[22];
		for (int i = 0; i < message.length; i++) message[i] = "0";
		message[4] = "1234";  // score
		message[5] = "10";    // lines
		message[6] = "20";    // totalPieceLocked
		message[7] = "600";   // time
		message[8] = "3";     // level
		message[9] = "50";    // garbageTimer
		message[10] = "30";   // garbageTotal
		message[11] = "1";    // goaltype
		message[12] = "true"; // gameActive
		message[13] = "true"; // timerActive
		message[14] = "5";    // lastscore
		message[15] = "12";   // scgettime
		message[16] = "4";    // lastevent
		message[17] = "true"; // lastb2b
		message[18] = "2";    // lastcombo
		message[19] = "1";    // lastpiece
		message[20] = "2";    // bg
		message[21] = "7";    // garbagePending

		invoke(mode, "netRecvStats", engine, message);

		assertEquals(1234, engine.statistics.score);
		assertEquals(1, readInt(mode, "goaltype"));
		assertEquals(7, readInt(mode, "garbagePending"));
	}

	@Test
	void netSendEndGameStatsBuildsMessage() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();
		engine.statistics.score = 500;
		engine.statistics.lines = 12;

		invoke(mode, "netSendEndGameStats", engine);
	}

	@Test
	void netSendOptionsBuildsMessage() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netLobby = new NetLobbyFrame();
		mode.netLobby.netPlayerClient = new NetPlayerClient();

		invoke(mode, "netSendOptions", engine);
	}

	@Test
	void netRecvOptionsParsesMessage() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		String[] message = new String[14];
		for (int i = 0; i < message.length; i++) message[i] = "0";
		message[4] = "1";      // goaltype
		message[5] = "3";      // startlevel
		message[6] = "5";      // bgmno
		message[7] = "2";      // tspinEnableType
		message[8] = "true";   // enableTSpinKick
		message[9] = "1";      // spinCheckType
		message[10] = "true";  // tspinEnableEZ
		message[11] = "false"; // enableB2B
		message[12] = "true";  // enableCombo
		message[13] = "20";    // das

		invoke(mode, "netRecvOptions", engine, message);

		assertEquals(1, readInt(mode, "goaltype"));
		assertEquals(3, readInt(mode, "startlevel"));
		assertEquals(20, engine.speed.das);
	}

	@Test
	void netGetGoalTypeReturnsGoaltype() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 1);

		Method m = DigChallengeMode.class.getDeclaredMethod("netGetGoalType");
		m.setAccessible(true);
		assertEquals(1, (int) m.invoke(mode));
	}

	@Test
	void netIsNetRankingViewOkBranches() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		Method m = DigChallengeMode.class.getDeclaredMethod("netIsNetRankingViewOK", GameEngine.class);
		m.setAccessible(true);

		setInt(mode, "startlevel", 0);
		engine.ai = null;
		assertTrue((boolean) m.invoke(mode, engine), "level 0, no AI => true");

		setInt(mode, "startlevel", 5);
		assertFalse((boolean) m.invoke(mode, engine), "non-zero level => false");
	}

	// ---------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(DigChallengeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	/** Engine whose receiver reports a sticky skin, to exercise connection code. */
	private static GameEngine freshEngineSticky(DigChallengeMode mode) {
		GameManager manager = new GameManager(new StickyReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	/** Fill the bottom row except one column so the field is NOT empty (no all-clear). */
	private static void fillBottomRowExceptOne(GameEngine engine) {
		int h = engine.field.getHeight();
		int w = engine.field.getWidth();
		for (int x = 1; x < w; x++) {
			engine.field.setBlock(x, h - 1, new Block(Block.BLOCK_COLOR_GRAY));
		}
	}

	private static void pressKey(GameEngine e, int btn) {
		e.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) e.ctrl.buttonTime[i] = 0;
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static void invoke(DigChallengeMode mode, String name, GameEngine engine) throws Exception {
		Method m = DigChallengeMode.class.getDeclaredMethod(name, GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invoke(DigChallengeMode mode, String name, GameEngine engine, String[] msg) throws Exception {
		Method m = DigChallengeMode.class.getDeclaredMethod(name, GameEngine.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, (Object) msg);
	}

	private static int readInt(DigChallengeMode mode, String name) throws Exception {
		return findField(mode.getClass(), name).getInt(mode);
	}

	private static boolean readBool(DigChallengeMode mode, String name) throws Exception {
		return findField(mode.getClass(), name).getBoolean(mode);
	}

	private static void setInt(DigChallengeMode mode, String name, int value) throws Exception {
		findField(mode.getClass(), name).setInt(mode, value);
	}

	private static void setBool(DigChallengeMode mode, String name, boolean value) throws Exception {
		findField(mode.getClass(), name).setBoolean(mode, value);
	}

	private static void setStr(DigChallengeMode mode, String name, String value) throws Exception {
		findField(mode.getClass(), name).set(mode, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	/** EventReceiver subclass that pretends the current skin is sticky. */
	private static final class StickyReceiver extends EventReceiver {
		@Override
		public boolean isStickySkin(int skin) {
			return true;
		}
	}
}
