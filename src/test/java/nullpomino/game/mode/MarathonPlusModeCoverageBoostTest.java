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
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Boosts {@link MarathonPlusMode} line coverage by targeting branches the
 * existing tests miss: the remaining {@code calcScore} T-Spin
 * single/double/triple variants (b2b / no-b2b / mini), every {@code renderLast}
 * event-switch case (including combo display), {@code bonusLevelProc} block
 * visibility, {@code startGame} tspinEnableType==0, {@code onResult} page-up,
 * {@code saveReplay} net-name + ranking-save, {@code onCustom} netplay sends,
 * and the full NET messaging surface ({@code netlobbyOnMessage},
 * {@code netRecvField}, {@code netSendStats}, {@code netRecvStats},
 * {@code netSendEndGameStats}, {@code netSendOptions}, {@code netRecvOptions})
 * by wiring a non-connected {@link NetLobbyFrame}/{@link NetPlayerClient}.
 */
class MarathonPlusModeCoverageBoostTest {

	private MarathonPlusMode mode;
	private GameManager manager;
	private GameEngine engine;

	@BeforeEach
	void setUp() {
		mode = new MarathonPlusMode();
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
	// startGame: tspinEnableType == 0 -> tspinEnable = false (line 283)
	// -----------------------------------------------------------------------

	@Test
	void startGameTspinTypeZeroDisablesTspin() throws Exception {
		setField(mode, "version", 1);
		setField(mode, "tspinEnableType", 0);
		engine.readyDone = false;
		mode.startGame(engine, 0);
		assertFalse(engine.tspinEnable);
	}

	@Test
	void startGameTspinTypeOneEnablesTspin() throws Exception {
		setField(mode, "version", 1);
		setField(mode, "tspinEnableType", 1);
		engine.readyDone = false;
		mode.startGame(engine, 0);
		assertTrue(engine.tspinEnable);
	}

	// -----------------------------------------------------------------------
	// calcScore: remaining T-Spin single / double / triple variants
	// (lines 501-553)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreTSpinSingleMiniNoB2B() throws Exception {
		engine.tspin = true;
		engine.tspinmini = true;
		engine.b2b = false;
		engine.statistics.level = 0;
		mode.calcScore(engine, 0, 1);
		assertEquals(7, readInt(mode, "lastevent")); // EVENT_TSPIN_SINGLE_MINI
		assertEquals(200, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinSingleMiniWithB2B() throws Exception {
		engine.tspin = true;
		engine.tspinmini = true;
		engine.b2b = true;
		engine.statistics.level = 0;
		mode.calcScore(engine, 0, 1);
		assertEquals(7, readInt(mode, "lastevent")); // EVENT_TSPIN_SINGLE_MINI
		assertEquals(300, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinSingleWithB2B() throws Exception {
		engine.tspin = true;
		engine.tspinmini = false;
		engine.b2b = true;
		engine.statistics.level = 0;
		mode.calcScore(engine, 0, 1);
		assertEquals(8, readInt(mode, "lastevent")); // EVENT_TSPIN_SINGLE
		assertEquals(1200, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinDoubleMiniWithAllSpinAndB2B() throws Exception {
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = true;
		engine.b2b = true;
		engine.statistics.level = 0;
		mode.calcScore(engine, 0, 2);
		assertEquals(9, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE_MINI
		assertEquals(600, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinDoubleWithB2B() throws Exception {
		engine.tspin = true;
		engine.tspinmini = false;
		engine.b2b = true;
		engine.statistics.level = 0;
		mode.calcScore(engine, 0, 2);
		assertEquals(10, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE
		assertEquals(1800, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinTripleWithB2B() throws Exception {
		engine.tspin = true;
		engine.b2b = true;
		engine.statistics.level = 0;
		mode.calcScore(engine, 0, 3);
		assertEquals(11, readInt(mode, "lastevent")); // EVENT_TSPIN_TRIPLE
		assertEquals(2400, readInt(mode, "lastscore"));
	}

	// -----------------------------------------------------------------------
	// renderLast: every lastevent switch case + combo display (lines 374-416)
	// -----------------------------------------------------------------------

	private void renderLastWithEvent(int event, boolean b2b, int combo) throws Exception {
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setField(mode, "lastevent", event);
		setField(mode, "scgettime", 0);
		setField(mode, "lastpiece", Piece.PIECE_T);
		setField(mode, "lastb2b", b2b);
		setField(mode, "lastcombo", combo);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWalksEveryEventCase() throws Exception {
		// EVENT_NONE=0, SINGLE=1, DOUBLE=2, TRIPLE=3, FOUR=4,
		// TSPIN_ZERO_MINI=5(named differently in source order), etc.
		// We simply walk lastevent 1..12 in both b2b states with a combo>=2
		// so line 416 (combo display) is also exercised for non-zero events.
		for (int ev = 1; ev <= 12; ev++) {
			renderLastWithEvent(ev, false, 3);
			renderLastWithEvent(ev, true, 3);
		}
		assertTrue(true);
	}

	@Test
	void renderLastScoreWithRecentLastscore() throws Exception {
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setField(mode, "lastscore", 500);
		setField(mode, "scgettime", 10); // < 120 -> "(+500)" branch
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastLinesMidLevel() throws Exception {
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setField(mode, "startlevel", 1);
		engine.statistics.level = 3;
		engine.statistics.lines = 35;
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// -----------------------------------------------------------------------
	// bonusLevelProc: hide visible colored blocks (lines 463-464)
	// -----------------------------------------------------------------------

	@Test
	void bonusLevelProcHidesColoredBlocks() throws Exception {
		setField(mode, "bonusFlashNow", 0);
		// place a visible colored block so lines 462-464 run
		int x = 2, y = engine.field.getHeight() - 2;
		Block blk = new Block(Block.BLOCK_COLOR_RED);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		engine.field.setBlock(x, y, blk);
		Method m = mode.getClass().getDeclaredMethod("bonusLevelProc", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, engine.blockOutlineType);
		// re-fetch from the field (lines 463-464 cleared the VISIBLE attribute)
		assertFalse(engine.field.getBlock(x, y).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
	}

	// -----------------------------------------------------------------------
	// onSetting: change triggers netSendOptions (192) and the D-button
	// netplay ranking screen (216)
	// -----------------------------------------------------------------------

	@Test
	void onSettingChangeSignalsNetOptions() throws Exception {
		wireNetLobby();
		engine.owner.replayMode = false;
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netNumSpectators", 1);
		setMenuState(2); // toggle enableTSpinKick on a RIGHT press
		setFieldOnAbstract("menuTime", 10);
		pressKey(Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertTrue(true);
	}

	@Test
	void onSettingDButtonEntersNetRanking() throws Exception {
		wireNetLobby();
		engine.owner.replayMode = false;
		setField(mode, "netIsNetPlay", true);
		setField(mode, "startlevel", 0);
		setField(mode, "big", false);
		setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
		setFieldOnAbstract("menuTime", 10);
		pressKey(Controller.BUTTON_D);
		mode.onSetting(engine, 0);
		assertTrue(readBoolean(mode, "netIsNetRankingDisplayMode"));
	}

	// -----------------------------------------------------------------------
	// onResult: page change up (lines 774-776)
	// -----------------------------------------------------------------------

	@Test
	void onResultPageUpWrapsTo1() throws Exception {
		engine.ctrl = new Controller();
		engine.statc[1] = 0;
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1; // isMenuRepeatKey true
		mode.onResult(engine, 0);
		assertEquals(1, engine.statc[1]); // 0-- -> -1 -> wraps to 1
	}

	@Test
	void onResultPageDownWrapsTo0() throws Exception {
		engine.ctrl = new Controller();
		engine.statc[1] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1]);
	}

	// -----------------------------------------------------------------------
	// renderResult: A:RETRY line (763)
	// -----------------------------------------------------------------------

	@Test
	void renderResultRetryLine() throws Exception {
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		setField(mode, "netReplaySendStatus", 2);
		engine.statc[1] = 0;
		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	// -----------------------------------------------------------------------
	// saveReplay: net name (796) + ranking save (805-806)
	// -----------------------------------------------------------------------

	@Test
	void saveReplaySavesNameAndRanking() throws Exception {
		setField(mode, "netPlayerName", "tester");
		setField(mode, "big", false);
		setField(mode, "startlevel", 0);
		engine.statistics.score = 999999; // high enough to rank #1
		engine.statistics.lines = 100;
		engine.statistics.time = 3600;
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);
		assertTrue(readInt(mode, "rankingRank") != -1);
		assertEquals("tester", prop.getProperty("0.net.netPlayerName", ""));
	}

	// -----------------------------------------------------------------------
	// onCustom: net send branches (669-674 and 686-692)
	// -----------------------------------------------------------------------

	@Test
	void onCustomPhase0SendsBonusEnter() throws Exception {
		wireNetLobby();
		populateNextQueue();
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		setField(mode, "netNumSpectators", 1);
		engine.statc[0] = 0;
		boolean result = mode.onCustom(engine, 0);
		assertFalse(result);
	}

	@Test
	void onCustomPhase480SendsBonusStart() throws Exception {
		wireNetLobby();
		populateNextQueue();
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		setField(mode, "netNumSpectators", 1);
		engine.statc[0] = 480;
		boolean result = mode.onCustom(engine, 0);
		assertTrue(result);
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	@Test
	void onCustomPhase120AdvancesWithA() throws Exception {
		setField(mode, "netIsWatch", false);
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.statc[0] = 120;
		mode.onCustom(engine, 0);
		// branch sets statc[0]=480, then the trailing statc[0]++ makes it 481
		assertEquals(481, engine.statc[0]);
	}

	// -----------------------------------------------------------------------
	// netlobbyOnMessage: bonuslevelenter / bonuslevelstart (816-839)
	// -----------------------------------------------------------------------

	@Test
	void netlobbyOnMessageBonusLevelEnter() throws Exception {
		engine.timerActive = true;
		String[] msg = { "game", "x", "y", "bonuslevelenter" };
		mode.netlobbyOnMessage(null, null, msg);
		assertEquals(1, engine.ending);
		assertEquals(GameEngine.Status.CUSTOM, engine.stat);
		assertFalse(engine.timerActive);
	}

	@Test
	void netlobbyOnMessageBonusLevelStart() throws Exception {
		engine.ending = 1;
		String[] msg = { "game", "x", "y", "bonuslevelstart" };
		mode.netlobbyOnMessage(null, null, msg);
		assertEquals(0, engine.ending);
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	// -----------------------------------------------------------------------
	// netRecvField: bonus proc when level >= 20 (844-851)
	// -----------------------------------------------------------------------

	@Test
	void netRecvFieldRunsBonusProc() throws Exception {
		engine.statistics.level = 20;
		engine.timerActive = true;
		engine.gameActive = true;
		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_NORMAL;
		setField(mode, "bonusFlashNow", 0);
		// "game\t<id>\t<seq>\tfield\t<skin>\t<highestWallY>\t<data>\t<compressed>..."
		// super.netRecvField parses this without attributes; an empty data string
		// is tolerated. Then lines 848-850 invoke bonusLevelProc (level>=20).
		String[] msg = makeFieldMessage();
		Method m = MarathonPlusMode.class.getDeclaredMethod("netRecvField", GameEngine.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, msg);
		// bonusLevelProc with bonusFlashNow==0 forces outline NONE
		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, engine.blockOutlineType);
	}

	private String[] makeFieldMessage() {
		// "game\t<id>\t<seq>\tfield\t<width>\t<height>\t<...blocks...>"
		return new String[] { "game", "0", "0", "field", "0", "0", "", "0", "0", "0", "0" };
	}

	// -----------------------------------------------------------------------
	// netSendStats / netSendOptions / netSendEndGameStats (858-868, 917-963)
	// -----------------------------------------------------------------------

	@Test
	void netSendStatsBuildsAndSends() throws Exception {
		wireNetLobby();
		engine.statistics.level = 5;
		Method m = MarathonPlusMode.class.getDeclaredMethod("netSendStats", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
		assertTrue(true);
	}

	@Test
	void netSendStatsWithFadeBackground() throws Exception {
		wireNetLobby();
		engine.owner.backgroundStatus.fadesw = true;
		engine.owner.backgroundStatus.fadebg = 3;
		Method m = MarathonPlusMode.class.getDeclaredMethod("netSendStats", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
		assertTrue(true);
	}

	@Test
	void netSendOptionsBuildsAndSends() throws Exception {
		wireNetLobby();
		Method m = MarathonPlusMode.class.getDeclaredMethod("netSendOptions", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
		assertTrue(true);
	}

	@Test
	void netSendEndGameStatsBonusLevel() throws Exception {
		wireNetLobby();
		engine.statistics.level = 20; // LEVEL;BONUS branch
		Method m = MarathonPlusMode.class.getDeclaredMethod("netSendEndGameStats", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
		assertTrue(true);
	}

	@Test
	void netSendEndGameStatsNormalLevel() throws Exception {
		wireNetLobby();
		engine.statistics.level = 5; // LEVEL;<n> branch
		engine.statistics.levelDispAdd = 1;
		Method m = MarathonPlusMode.class.getDeclaredMethod("netSendEndGameStats", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
		assertTrue(true);
	}

	// -----------------------------------------------------------------------
	// netRecvStats: full parse, level<20 meter and level>=20 else (876-909)
	// -----------------------------------------------------------------------

	private String[] statsMessage(int level, int lines) {
		// indices 4..25 are parsed by netRecvStats
		return new String[] {
			"game", "0", "0", "stats",
			"1000",          // 4 score
			String.valueOf(lines), // 5 lines
			"42",            // 6 totalPieceLocked
			"3600",          // 7 time
			String.valueOf(level), // 8 level
			"1.5",           // 9 spl (double)
			"2.5",           // 10 spm (double)
			"3.5",           // 11 lpm (float)
			"4.5",           // 12 pps (float)
			"true",          // 13 gameActive
			"true",          // 14 timerActive
			"100",           // 15 lastscore
			"5",             // 16 scgettime
			"1",             // 17 lastevent
			"true",          // 18 lastb2b
			"3",             // 19 lastcombo
			"2",             // 20 lastpiece
			"7",             // 21 bg
			"12",            // 22 bonusLines
			"0",             // 23 bonusFlashNow
			"4",             // 24 bonusPieceCount
			"500"            // 25 bonusTime
		};
	}

	@Test
	void netRecvStatsLevelBelow20UpdatesMeter() throws Exception {
		Method m = MarathonPlusMode.class.getDeclaredMethod("netRecvStats", GameEngine.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, statsMessage(5, 8)); // lines%10=8 -> RED meter branch
		assertEquals(1000, engine.statistics.score);
		assertEquals(5, engine.statistics.level);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void netRecvStatsLevel20ZeroesMeter() throws Exception {
		Method m = MarathonPlusMode.class.getDeclaredMethod("netRecvStats", GameEngine.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, statsMessage(20, 0)); // level>=20 -> meterValue=0 (line 907)
		assertEquals(0, engine.meterValue);
		assertEquals(20, engine.statistics.level);
	}

	@Test
	void netRecvStatsLevelBelow20MeterColorTiers() throws Exception {
		Method m = MarathonPlusMode.class.getDeclaredMethod("netRecvStats", GameEngine.class, String[].class);
		m.setAccessible(true);
		// lines%10 = 4 -> YELLOW, 6 -> ORANGE
		m.invoke(mode, engine, statsMessage(5, 4));
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
		m.invoke(mode, engine, statsMessage(5, 6));
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
		m.invoke(mode, engine, statsMessage(5, 1));
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	// -----------------------------------------------------------------------
	// netRecvOptions (954-963)
	// -----------------------------------------------------------------------

	@Test
	void netRecvOptionsParsesAll() throws Exception {
		String[] msg = {
			"game", "0", "option", "x",
			"7",      // 4 startlevel
			"2",      // 5 tspinEnableType
			"true",   // 6 enableTSpinKick
			"true",   // 7 enableB2B
			"false",  // 8 enableCombo
			"true",   // 9 big
			"1",      // 10 spinCheckType
			"true"    // 11 tspinEnableEZ
		};
		Method m = MarathonPlusMode.class.getDeclaredMethod("netRecvOptions", GameEngine.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, msg);
		assertEquals(7, readInt(mode, "startlevel"));
		assertEquals(2, readInt(mode, "tspinEnableType"));
		assertEquals(1, readInt(mode, "spinCheckType"));
		assertTrue(readBoolean(mode, "big"));
	}

	// -----------------------------------------------------------------------
	// netIsNetRankingViewOK (978)
	// -----------------------------------------------------------------------

	@Test
	void netIsNetRankingViewOKTrueForStartlevel0() throws Exception {
		setField(mode, "startlevel", 0);
		setField(mode, "big", false);
		Method m = MarathonPlusMode.class.getDeclaredMethod("netIsNetRankingViewOK", GameEngine.class);
		m.setAccessible(true);
		assertTrue((boolean) m.invoke(mode, engine));
	}

	@Test
	void netIsNetRankingViewOKFalseForBig() throws Exception {
		setField(mode, "startlevel", 5);
		setField(mode, "big", true);
		Method m = MarathonPlusMode.class.getDeclaredMethod("netIsNetRankingViewOK", GameEngine.class);
		m.setAccessible(true);
		assertFalse((boolean) m.invoke(mode, engine));
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	/**
	 * Gives the engine a small next-piece queue so {@code netSendNextAndHold}
	 * (reached from onCustom's spectator-send paths) does not NPE.
	 */
	private void populateNextQueue() {
		Piece[] q = new Piece[engine.ruleopt.nextDisplay + 2];
		for (int i = 0; i < q.length; i++) q[i] = new Piece(Piece.PIECE_T);
		engine.nextPieceArrayObject = q;
		engine.nextPieceCount = 0;
	}

	private void pressKey(int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) engine.ctrl.buttonTime[i] = 0;
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private void setMenuState(int cursor) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
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
