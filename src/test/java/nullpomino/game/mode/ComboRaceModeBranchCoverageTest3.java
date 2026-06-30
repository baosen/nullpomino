package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

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
 * Branch-coverage fills for {@link ComboRaceMode} that the existing
 * ComboRaceMode* test classes leave half-taken. Targets, by source line:
 * <ul>
 *   <li>L375 / L386: netSendOptions on a menu change / load-preset with spectators.</li>
 *   <li>L379: BUTTON_A pushed while menuTime &lt; 5 (confirm gated off).</li>
 *   <li>L396: netIsNetPlay true at game-start confirm (sends "start1p").</li>
 *   <li>L403: BUTTON_B pushed while netIsNetPlay (quit suppressed).</li>
 *   <li>L408: BUTTON_D pushed but netIsNetPlay false (ranking screen NOT entered).</li>
 *   <li>L468 / L473: onReady with statc[0] != 0, and onReady in watch mode.</li>
 *   <li>L539: fillStack with comboWidth != 4 (no starting shape inserted).</li>
 *   <li>L562-L569: renderLast in RESULT state with a matching rankingRank highlight.</li>
 *   <li>L588: lastevent set but scgettime &gt;= 120 (event banner suppressed, combo only).</li>
 *   <li>L627 / L629 / L631: combo banner sub-branches.</li>
 *   <li>L668: tspin double with mini set but useAllSpinBonus false -&gt; EVENT_TSPIN_DOUBLE.</li>
 *   <li>L685: lines &gt;= 4 -&gt; EVENT_FOUR; L675 tspin triple (lines &gt;= 3).</li>
 *   <li>L697: all-clear bravo (lines &gt;= 1 && field empty).</li>
 *   <li>L721 / L745 / L749: endless meter sub-meter, and endless game-over /
 *       EXCELLENT threshold when no lines are cleared.</li>
 *   <li>L781: renderResult retry status (net play, not watch, send status 2).</li>
 *   <li>L795 / L800: saveReplay with empty player name and the replay/big/ai guard.</li>
 *   <li>L972: netIsNetRankingViewOK with engine.ai != null.</li>
 * </ul>
 */
class ComboRaceModeBranchCoverageTest3 {

	// =======================================================================
	// onSetting net / confirm / cancel branches
	// =======================================================================

	@Test
	void onSettingChangeWithSpectatorsSendsOptions() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		wireNetLobby(mode);
		setBoolean(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 1);
		setInt(mode, "goaltype", 1);

		pressKey(engine, Controller.BUTTON_RIGHT); // value change -> netSendOptions (L375 true)
		mode.onSetting(engine, 0);
		assertEquals(2, readInt(mode, "goaltype"));
	}

	@Test
	void onSettingLoadPresetWithSpectatorsSendsOptions() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 14); // cursor 14 = LOAD preset
		wireNetLobby(mode);
		setBoolean(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 1);
		setInt(mode, "menuTime", 10);
		setInt(mode, "presetNumber", 0);
		engine.owner.modeConfig.setProperty("comborace.gravity.0", 321);

		pressKey(engine, Controller.BUTTON_A); // load + netSendOptions (L386 true)
		boolean cont = mode.onSetting(engine, 0);
		assertTrue(cont, "load preset stays in setting screen");
		assertEquals(321, engine.speed.gravity);
	}

	@Test
	void onSettingConfirmGatedByMenuTime() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		setInt(mode, "menuTime", 0); // < 5 -> A press does NOT confirm (L379 false arm)

		pressKey(engine, Controller.BUTTON_A);
		boolean cont = mode.onSetting(engine, 0);
		assertTrue(cont, "A with menuTime < 5 must not start the game");
	}

	@Test
	void onSettingStartGameInNetPlaySendsStart1p() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0); // not 14/15 -> start branch
		wireNetLobby(mode);
		setBoolean(mode, "netIsNetPlay", true);
		setInt(mode, "menuTime", 10);

		pressKey(engine, Controller.BUTTON_A);
		boolean cont = mode.onSetting(engine, 0); // L396 true -> send("start1p")
		assertFalse(cont, "confirming at a normal cursor starts the game");
	}

	@Test
	void onSettingCancelSuppressedInNetPlay() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		setBoolean(mode, "netIsNetPlay", true); // !netIsNetPlay false -> quit suppressed (L403)
		setInt(mode, "menuTime", 10);

		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertFalse(engine.quitflag, "B in net play must not set quitflag");
	}

	@Test
	void onSettingDButtonIgnoredOffline() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		setBoolean(mode, "netIsNetPlay", false); // L408: D && netIsNetPlay(false) -> skip
		setInt(mode, "menuTime", 10);

		pressKey(engine, Controller.BUTTON_D);
		mode.onSetting(engine, 0);
		assertFalse(readBoolean(mode, "netIsNetRankingDisplayMode"),
				"D offline must not open the net ranking screen");
	}

	// =======================================================================
	// onReady branches
	// =======================================================================

	@Test
	void onReadyNonZeroStatcSkipsSetup() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 1; // L468 false arm -> body skipped
		boolean result = mode.onReady(engine, 0);
		assertFalse(result);
	}

	@Test
	void onReadyWatchModeSkipsFill() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setBoolean(mode, "netIsWatch", true); // L473 false arm -> no fillStack/send
		boolean result = mode.onReady(engine, 0);
		assertFalse(result);
	}

	// =======================================================================
	// fillStack comboWidth != 4 branch (L539)
	// =======================================================================

	@Test
	void fillStackNonFourWidthSkipsStartingShape() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "comboColumn", 1);
		setInt(mode, "comboWidth", 5); // != 4 -> skip shape insertion (L539 false)
		invokeFillStack(mode, engine, 0);
		// No exception, remainStack computed for finite goal.
		assertTrue(readInt(mode, "remainStack") >= 0);
	}

	// =======================================================================
	// renderLast RESULT-state ranking highlight (L562-L569)
	// =======================================================================

	@Test
	void renderLastResultStateHighlightsRankingRank() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBoolean(mode, "big", false);
		engine.ai = null;
		setInt(mode, "goaltype", 0);
		setInt(mode, "rankingRank", 3); // makes (rankingRank == i) true for one row
		engine.stat = GameEngine.Status.RESULT;
		mode.renderLast(engine, 0);
	}

	// =======================================================================
	// renderLast in-game combo sub-branches
	// =======================================================================

	@Test
	void renderLastEventStaleSuppressesBanner() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastevent", 1);    // != EVENT_NONE
		setInt(mode, "scgettime", 200);  // >= 120 -> banner suppressed (L588 false)
		engine.combo = 0;
		engine.gameActive = false;
		engine.statistics.maxCombo = 5;  // -> max-combo line (L631)
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastEventWithLowComboSkipsComboLine() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastevent", 1);   // EVENT_SINGLE banner shown
		setInt(mode, "scgettime", 0);   // < 120
		setInt(mode, "lastcombo", 1);   // < 2 -> combo line skipped (L627 false)
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastActiveComboBranch() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastevent", 0);   // EVENT_NONE -> first if false
		engine.combo = 4;               // >= 2
		engine.gameActive = true;       // L629 true arm
		setInt(mode, "lastcombo", 4);
		mode.renderLast(engine, 0);
	}

	// =======================================================================
	// calcScore branches
	// =======================================================================

	@Test
	void calcScoreTSpinDoubleNotMiniWhenAllSpinDisabled() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = false; // L668 second operand false -> EVENT_TSPIN_DOUBLE

		mode.calcScore(engine, 0, 2);
		assertEquals(7, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE
	}

	@Test
	void calcScoreTSpinTriple() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;
		engine.tspinmini = false;

		mode.calcScore(engine, 0, 3); // lines >= 3 (L675) -> EVENT_TSPIN_TRIPLE
		assertEquals(8, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreFourLines() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		engine.tspin = false;

		mode.calcScore(engine, 0, 4); // lines >= 4 (L685) -> EVENT_FOUR
		assertEquals(4, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreAllClearPlaysBravo() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		// Empty field -> field.isEmpty() true with lines >= 1 (L697 true arm).
		engine.tspin = false;

		mode.calcScore(engine, 0, 1);
		assertTrue(readInt(mode, "lastevent") != 0, "single recorded");
	}

	@Test
	void calcScoreEndlessHighComboSubMeter() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 3); // ENDLESS
		setInt(mode, "ceilingAdjust", -2);
		enableMeter(engine);
		// maxCombo high enough that colorIndex > 0 -> meterValueSub = meterMax (L721 true)
		engine.statistics.maxCombo = 500;

		mode.calcScore(engine, 0, 1);
		assertTrue(engine.meterValueSub > 0, "sub-meter set when colorIndex > 0");
	}

	@Test
	void calcScoreEndlessNoLinesGameOver() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 3); // ENDLESS
		engine.statistics.maxCombo = 5; // >= 2, <= 40 -> GAMEOVER (L745/L749 false arm)

		mode.calcScore(engine, 0, 0); // no lines -> else-if (L745)
		assertEquals(1, engine.ending);
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	@Test
	void calcScoreEndlessNoLinesExcellent() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 3); // ENDLESS
		engine.statistics.maxCombo = 50; // > 40 -> EXCELLENT (L749 true arm)

		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	// =======================================================================
	// renderResult retry-status branch (L781)
	// =======================================================================

	@Test
	void renderResultRetryStatus() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetPlay", true);
		setBoolean(mode, "netIsWatch", false);
		setInt(mode, "netReplaySendStatus", 2); // L781 all-true -> "A: RETRY"
		mode.renderResult(engine, 0);
	}

	// =======================================================================
	// saveReplay guard branches
	// =======================================================================

	@Test
	void saveReplayEmptyNameAndReplayGuardSkipsRanking() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setObject(mode, "netPlayerName", ""); // length 0 -> name not written (L795 false)
		engine.owner.replayMode = true;       // replayMode==false is false -> ranking skip (L800)
		setInt(mode, "rankingRank", -1);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals("", prop.getProperty("0.net.netPlayerName", ""));
		assertEquals(-1, readInt(mode, "rankingRank"), "ranking not updated under replay guard");
	}

	@Test
	void saveReplayBigSkipsRanking() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setObject(mode, "netPlayerName", null); // null -> name not written (L795 first operand)
		engine.owner.replayMode = false;
		setBoolean(mode, "big", true);          // !big false -> ranking skip (L800)
		setInt(mode, "rankingRank", -1);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);
		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// =======================================================================
	// netIsNetRankingViewOK with AI present (L972)
	// =======================================================================

	@Test
	void netIsNetRankingViewOKFalseWhenAiPresent() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", false);
		engine.ai = new nullpomino.game.ai.BasicAI(); // ai != null -> false arm

		Method m = ComboRaceMode.class.getDeclaredMethod("netIsNetRankingViewOK", GameEngine.class);
		m.setAccessible(true);
		assertFalse((boolean) m.invoke(mode, engine));
	}

	// =======================================================================
	// D-button enters net ranking with full net wiring (L408 all-true)
	// =======================================================================

	@Test
	void onSettingDButtonEntersNetRankingWhenWired() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		wireNetLobby(mode);
		setBoolean(mode, "netIsNetPlay", true);
		setBoolean(mode, "big", false);
		engine.ai = null;
		setObject(mode, "netCurrentRoomInfo", new NetRoomInfo());
		setInt(mode, "menuTime", 10);

		pressKey(engine, Controller.BUTTON_D);
		mode.onSetting(engine, 0);
		assertTrue(readBoolean(mode, "netIsNetRankingDisplayMode"));
	}

	// =======================================================================
	// Helpers
	// =======================================================================

	private static GameEngine freshEngine(ComboRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static GameEngine settingEngine(ComboRaceMode mode, int cursor) throws Exception {
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 10);
		return engine;
	}

	private static void enableMeter(GameEngine engine) throws Exception {
		Field f = findField(engine.owner.receiver.getClass(), "showmeter");
		f.setAccessible(true);
		f.setBoolean(engine.owner.receiver, true);
	}

	private static void wireNetLobby(Object mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setObject(mode, "netLobby", lobby);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.reset();
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
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

	private static void setObject(Object obj, String name, Object value) throws Exception {
		findField(obj.getClass(), name).set(obj, value);
	}

	private static void invokeFillStack(ComboRaceMode mode, GameEngine engine, int height) throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod("fillStack", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, height);
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
