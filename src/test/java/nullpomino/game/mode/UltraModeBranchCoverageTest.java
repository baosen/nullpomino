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
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link UltraMode} branches still missed after the existing
 * UltraMode*Test suites:
 * <ul>
 *   <li>onSetting NET option-send on value change with spectators (L311),
 *       on cursor-16 load-preset confirm (L322), the start1p signal on the
 *       game-start confirm (L332), the {@code menuTime < 5} A-gate (L315) and
 *       the {@code netIsNetPlay} B-cancel guard (L339).</li>
 *   <li>renderSetting version&gt;=1 spin labels for each tspinEnableType plus
 *       the IMMOBILE spinCheckType branch (L389-391, L398).</li>
 *   <li>renderLast time-band colours (L498-500), the score-no-delta path
 *       (L478) and combo suppression for T-Spin-zero events (L552).</li>
 *   <li>onLast watch-mode guard (L721) and the inactive-timer guard (L710).</li>
 *   <li>calcScore non-T-Spin zero-line no-op (L578 false arm) and combo bonus
 *       (L665).</li>
 *   <li>saveReplay net-name and ranking-update branches (L795, L800, L803).</li>
 *   <li>netRecvStats meter-colour thresholds (L927-929).</li>
 * </ul>
 */
class UltraModeBranchCoverageTest {

	// ------------------------------------------------------------------
	// onSetting: NET option send on a value change with spectators (L311)
	// ------------------------------------------------------------------

	@Test
	void onSettingValueChangeWithSpectatorsSendsOptions() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 1);
		setMenuState(engine, mode, 2); // ARE cursor, plain +1, no wrap

		pressKey(engine, Controller.BUTTON_RIGHT);
		int before = engine.speed.are;
		mode.onSetting(engine, 0);

		// The value still changed and netSendOptions ran without throwing.
		assertEquals(before + 1, engine.speed.are);
	}

	// ------------------------------------------------------------------
	// onSetting: cursor 16 load-preset A-confirm with spectators (L322)
	// ------------------------------------------------------------------

	@Test
	void onSettingLoadPresetConfirmWithSpectatorsSendsOptions() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 1);
		setMenuState(engine, mode, 16, 10); // menuTime >= 5 so A confirms
		engine.speed.gravity = 42;

		pressKey(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// Default preset gravity is 4 => loadPreset overwrote the staged value.
		assertEquals(4, engine.speed.gravity);
	}

	// ------------------------------------------------------------------
	// onSetting: game-start confirm sends start1p in net play (L332)
	// ------------------------------------------------------------------

	@Test
	void onSettingStartConfirmInNetPlaySendsStart1p() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		setBool(mode, "netIsNetPlay", true);
		setMenuState(engine, mode, 0, 10); // cursor 0 => "else" start branch

		pressKey(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);

		assertFalse(result, "Confirming start should leave the settings screen");
	}

	// ------------------------------------------------------------------
	// onSetting: A pressed but menuTime < 5 is ignored (L315 false arm)
	// ------------------------------------------------------------------

	@Test
	void onSettingAIgnoredWhenMenuTimeTooLow() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenuState(engine, mode, 0, 0); // menuTime 0 < 5

		pressKey(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);

		assertTrue(result, "A with menuTime < 5 must not confirm");
	}

	// ------------------------------------------------------------------
	// onSetting: B pressed in net play does NOT quit (L339 false arm)
	// ------------------------------------------------------------------

	@Test
	void onSettingBInNetPlayDoesNotQuit() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		setBool(mode, "netIsNetPlay", true);
		setMenuState(engine, mode, 0, 10);

		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertFalse(engine.quitflag, "B in net play must not set quitflag");
	}

	// ------------------------------------------------------------------
	// renderSetting: version>=1 labels for each tspinEnableType + IMMOBILE
	// (L389, L390, L391, L398)
	// ------------------------------------------------------------------

	@Test
	void renderSettingVersion1SpinLabelsAndImmobile() throws Exception {
		for (int type = 0; type <= 2; type++) {
			UltraMode mode = new UltraMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			setInt(mode, "version", 1);
			setInt(mode, "menuCursor", 10);
			setInt(mode, "tspinEnableType", type);
			setInt(mode, "spinCheckType", 1); // IMMOBILE branch of L398

			mode.renderSetting(engine, 0); // must not throw
		}
	}

	// ------------------------------------------------------------------
	// renderLast: time-band colours YELLOW/ORANGE/RED (L498, L499, L500)
	// ------------------------------------------------------------------

	@Test
	void renderLastTimeBandColours() throws Exception {
		// renderLast colours by remaining frames (time>0 and time<30*60/20*60/10*60,
		// sequential ifs so the lowest match wins): 1500 -> YELLOW, 900 -> ORANGE,
		// 300 -> RED.
		int[] remainingFrames = {1500, 900, 300};
		for (int remain : remainingFrames) {
			UltraMode mode = new UltraMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			engine.owner.menuOnly = false;
			engine.stat = GameEngine.Status.MOVE;
			engine.nowPieceObject = new Piece(Piece.PIECE_T);
			// Largest goaltype so the limit comfortably exceeds the remaining
			// time we want to display.
			setInt(mode, "goaltype", 4); // limit = 5 * 3600 = 18000 frames
			engine.statistics.time = (5 * 3600) - remain;

			mode.renderLast(engine, 0); // must not throw
		}
	}

	// ------------------------------------------------------------------
	// renderLast: time clamps to 0 when expired (L496 true arm)
	// ------------------------------------------------------------------

	@Test
	void renderLastTimeClampsToZeroWhenExpired() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "goaltype", 0); // limit 3600
		engine.statistics.time = 4000; // beyond limit => remaining negative => clamps

		mode.renderLast(engine, 0); // must not throw
	}

	// ------------------------------------------------------------------
	// renderLast: no score-delta when scgettime >= 120 (L478 true via right)
	// ------------------------------------------------------------------

	@Test
	void renderLastNoDeltaWhenScgettimeElapsed() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "lastscore", 500);
		setInt(mode, "scgettime", 150); // >= 120 => no "(+...)" suffix
		setInt(mode, "lastevent", 0);

		mode.renderLast(engine, 0); // must not throw
	}

	// ------------------------------------------------------------------
	// renderLast: combo suppressed for T-Spin zero events (L552 short-circuit)
	// ------------------------------------------------------------------

	@Test
	void renderLastTSpinZeroSuppressesComboLine() throws Exception {
		for (int ev : new int[]{5, 6}) { // EVENT_TSPIN_ZERO_MINI, EVENT_TSPIN_ZERO
			UltraMode mode = new UltraMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			engine.owner.menuOnly = false;
			engine.stat = GameEngine.Status.MOVE;
			engine.nowPieceObject = new Piece(Piece.PIECE_T);
			setInt(mode, "lastevent", ev);
			setInt(mode, "scgettime", 0);
			setInt(mode, "lastpiece", Piece.PIECE_T);
			setInt(mode, "lastcombo", 5); // >= 2 but event suppresses combo line

			mode.renderLast(engine, 0); // must not throw
		}
	}

	// ------------------------------------------------------------------
	// renderLast: owner.menuOnly short-circuits (L450 true arm)
	// ------------------------------------------------------------------

	@Test
	void renderLastMenuOnlyReturnsEarly() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = true;

		mode.renderLast(engine, 0); // returns immediately, must not throw
	}

	// ------------------------------------------------------------------
	// renderLast: RESULT state draws the ranking tables (L455, L456, L462...)
	// ------------------------------------------------------------------

	@Test
	void renderLastResultStateDrawsRanking() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.owner.replayMode = false;
		engine.ai = null;
		engine.stat = GameEngine.Status.RESULT;
		setBool(mode, "big", false);
		// Mark some ranking slots so the "(i == rankingRank)" highlight branch
		// (L462/463/471/472) flips both ways across the loop.
		int[] rankingRank = (int[]) readObject(mode, "rankingRank");
		rankingRank[0] = 0;
		rankingRank[1] = 1;

		mode.renderLast(engine, 0); // must not throw
	}

	// ------------------------------------------------------------------
	// onLast: watch mode skips the end-game/countdown block (L721 false arm)
	// ------------------------------------------------------------------

	@Test
	void onLastWatchModeSkipsEndGameBlock() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;
		setBool(mode, "netIsWatch", true);
		setInt(mode, "goaltype", 0);
		engine.statistics.time = 4000; // would end the game if not watching

		mode.onLast(engine, 0);

		// Watch mode must NOT end the game.
		assertFalse(engine.stat == GameEngine.Status.ENDINGSTART,
				"Watch mode should not end the game on time-out");
	}

	// ------------------------------------------------------------------
	// onLast: inactive timer skips the meter block (L710 false arm)
	// ------------------------------------------------------------------

	@Test
	void onLastInactiveTimerSkipsMeterBlock() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = false; // gate false
		setInt(mode, "scgettime", 7);

		mode.onLast(engine, 0);

		assertEquals(8, readInt(mode, "scgettime"),
				"scgettime still advances even when the timer is inactive");
	}

	// ------------------------------------------------------------------
	// calcScore: non-T-Spin zero lines is a no-op (L578 false arm, no event)
	// ------------------------------------------------------------------

	@Test
	void calcScoreNonTSpinZeroLinesScoresNothing() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		engine.tspin = false;

		mode.calcScore(engine, 0, 0); // 0 lines, no T-Spin => nothing happens

		assertEquals(0, engine.statistics.score);
		assertEquals(0, readInt(mode, "lastevent"), "no event recorded");
	}

	// ------------------------------------------------------------------
	// calcScore: combo bonus is applied (L665 true arm)
	// ------------------------------------------------------------------

	@Test
	void calcScoreAppliesComboBonus() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.score = 0;
		engine.tspin = false;
		setBool(mode, "enableCombo", true);
		engine.combo = 3; // (combo - 1) * 50 = 100 bonus on top of single

		mode.calcScore(engine, 0, 1); // single => 100, plus combo 100 => 200

		assertEquals(200, engine.statistics.score);
		assertEquals(3, readInt(mode, "lastcombo"));
	}

	// ------------------------------------------------------------------
	// saveReplay: net-name + ranking update branches (L795, L800, L803)
	// ------------------------------------------------------------------

	@Test
	void saveReplaySavesNameAndUpdatesRanking() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		setBool(mode, "big", false);
		setObject(mode, "netPlayerName", "tester"); // non-empty => L795 true
		engine.statistics.score = 100000; // beats the all-zero ranking => L803 true
		engine.statistics.lines = 50;

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals("tester",
				prop.getProperty("0.net.netPlayerName", ""),
				"net player name should be persisted into the replay prop");
		int[] rankingRank = (int[]) readObject(mode, "rankingRank");
		assertTrue(rankingRank[0] != -1 || rankingRank[1] != -1,
				"a top score should produce a ranking entry");
	}

	// ------------------------------------------------------------------
	// netRecvStats: meter-colour thresholds YELLOW/ORANGE/RED (L927-929)
	// ------------------------------------------------------------------

	@Test
	void netRecvStatsMeterColourThresholds() throws Exception {
		// Thresholds (frames, sequential ifs so the lowest match wins):
		//   remainTime <= 1800 -> YELLOW, <= 1200 -> ORANGE, <= 600 -> RED.
		int[][] cases = {
			{1500, GameEngine.METER_COLOR_YELLOW}, // in (1200, 1800]
			{900, GameEngine.METER_COLOR_ORANGE},  // in (600, 1200]
			{300, GameEngine.METER_COLOR_RED},     // <= 600
		};
		for (int[] c : cases) {
			int remainFrames = c[0];
			int expectedColor = c[1];

			UltraMode mode = new UltraMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);

			int goaltype = 4; // limit = 18000 frames
			int limit = (goaltype + 1) * 3600;
			int time = limit - remainFrames;

			String[] msg = new String[21];
			msg[0] = "game"; msg[1] = "stats"; msg[2] = "x"; msg[3] = "y";
			msg[4] = "0";      // score
			msg[5] = "0";      // lines
			msg[6] = "0";      // totalPieceLocked
			msg[7] = String.valueOf(time); // time
			msg[8] = "0.0";    // spm
			msg[9] = "0.0";    // lpm
			msg[10] = "0.0";   // spl
			msg[11] = String.valueOf(goaltype);
			msg[12] = "true";  // gameActive
			msg[13] = "true";  // timerActive
			msg[14] = "0";     // lastscore
			msg[15] = "0";     // scgettime
			msg[16] = "0";     // lastevent
			msg[17] = "false"; // lastb2b
			msg[18] = "0";     // lastcombo
			msg[19] = "0";     // lastpiece
			msg[20] = "0";     // bg

			invoke(mode, "netRecvStats",
					new Class[]{GameEngine.class, String[].class}, engine, msg);

			assertEquals(expectedColor, engine.meterColor,
					"meter colour for remaining " + c[0] + " minutes");
		}
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	private static GameEngine freshEngine(UltraMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void placeOneBlock(GameEngine engine) {
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
	}

	private static void attachNetLobby(UltraMode mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		NetPlayerClient client = new NetPlayerClient();
		setObject(lobby, "netPlayerClient", client);
		setObject(mode, "netLobby", lobby);
	}

	private static void setMenuState(GameEngine engine, UltraMode mode, int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, UltraMode mode, int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", menuTime);
	}

	private static void pressKey(GameEngine e, int btn) {
		e.ctrl.reset();
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static Object readObject(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).get(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setObject(Object obj, String name, Object value) throws Exception {
		field(obj.getClass(), name).set(obj, value);
	}

	private static Object invoke(Object obj, String name, Class<?>[] sig, Object... args) throws Exception {
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

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
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
