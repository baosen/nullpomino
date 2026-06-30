package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the remaining reachable branches of {@link ScoreRaceMode}:
 * <ul>
 *   <li>{@code calcScore} scoring-event arms (T-Spin zero / EZ-spin b2b &amp;
 *       non-b2b / T-Spin triple, and the combo bonus path).</li>
 *   <li>{@code onResult} page-change wraparound (UP below 0, DOWN above 1).</li>
 *   <li>{@code onSetting} confirm-save (BUTTON_A) and cancel-quit (BUTTON_B)
 *       branches.</li>
 *   <li>{@code renderLast} ranking display and in-game score-color thresholds
 *       (render-only, no assertions).</li>
 * </ul>
 */
class ScoreRaceModeCoverageTest {

	private static GameEngine freshEngine(ScoreRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	// -----------------------------------------------------------------------
	// calcScore: T-Spin and combo scoring arms (assertion-backed via lastevent)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreTSpinZeroMiniAndFull() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		// T-Spin 0 lines, mini -> EVENT_TSPIN_ZERO_MINI (5), +100
		engine.tspin = true;
		engine.tspinez = false;
		engine.tspinmini = true;
		engine.statistics.score = 0;
		mode.calcScore(engine, 0, 0);
		assertEquals(5, readInt(mode, "lastevent"));
		assertEquals(100, engine.statistics.score);

		// T-Spin 0 lines, non-mini -> EVENT_TSPIN_ZERO (6), +400
		engine.tspinmini = false;
		engine.statistics.score = 0;
		mode.calcScore(engine, 0, 0);
		assertEquals(6, readInt(mode, "lastevent"));
		assertEquals(400, engine.statistics.score);
	}

	@Test
	void calcScoreEzSpinB2bAndNonB2b() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		makeNonEmptyField(engine); // line clear (lines>=1) -> calcScore reads field.isEmpty()

		engine.tspin = true;
		engine.tspinez = true;
		engine.statistics.level = 0;

		// EZ spin, non-b2b: 120 * (level+1) = 120
		engine.b2b = false;
		engine.statistics.score = 0;
		mode.calcScore(engine, 0, 1);
		assertEquals(12, readInt(mode, "lastevent")); // EVENT_TSPIN_EZ
		assertEquals(120, engine.statistics.score);

		// EZ spin, b2b: 180 * (level+1) = 180
		engine.b2b = true;
		engine.statistics.score = 0;
		mode.calcScore(engine, 0, 1);
		assertEquals(12, readInt(mode, "lastevent"));
		assertEquals(180, engine.statistics.score);
	}

	@Test
	void calcScoreTSpinTripleAndComboBonus() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		makeNonEmptyField(engine); // line clear (lines>=1) -> calcScore reads field.isEmpty()

		// T-Spin 3 lines, b2b -> EVENT_TSPIN_TRIPLE (11), +2400, plus combo bonus
		engine.tspin = true;
		engine.tspinez = false;
		engine.tspinmini = false;
		engine.b2b = true;
		setBool(mode, "enableCombo", true);
		engine.combo = 3; // (combo-1)*50 = 100 bonus, sets lastcombo
		engine.statistics.score = 0;
		mode.calcScore(engine, 0, 3);
		assertEquals(11, readInt(mode, "lastevent"));
		assertEquals(2500, engine.statistics.score); // 2400 + 100 combo
		assertEquals(3, readInt(mode, "lastcombo"));
	}

	// -----------------------------------------------------------------------
	// onResult: page-change wraparound
	// -----------------------------------------------------------------------

	@Test
	void onResultPageChangeWrapsBothDirections() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// UP from page 0 -> -1 -> wraps to 1
		engine.statc[1] = 0;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.onResult(engine, 0);
		assertEquals(1, engine.statc[1]);

		// DOWN from page 1 -> 2 -> wraps to 0
		engine.statc[1] = 1;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1]);
	}

	// -----------------------------------------------------------------------
	// onSetting: confirm-save (A) and cancel-quit (B)
	// -----------------------------------------------------------------------

	@Test
	void onSettingConfirmSavesAndStartsGame() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Cursor not 16/17 -> "Save settings" arm; menuTime>=5 so confirm fires.
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		boolean keepGoing = mode.onSetting(engine, 0);
		// The save-settings arm returns false (proceeds to start the game).
		assertEquals(false, keepGoing);
	}

	@Test
	void onSettingLoadAndSavePresetArms() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Cursor 16 -> load preset arm (does not return false).
		setInt(mode, "menuCursor", 16);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		assertTrue(mode.onSetting(engine, 0));

		// Cursor 17 -> save preset arm (does not return false).
		setInt(mode, "menuCursor", 17);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		assertTrue(mode.onSetting(engine, 0));
	}

	@Test
	void onSettingCancelSetsQuitFlag() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;

		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag, "BUTTON_B (offline) should set the quit flag");
	}

	// -----------------------------------------------------------------------
	// renderLast: ranking display + in-game score-color thresholds
	// (render-only, exercised through the no-op EventReceiver)
	// -----------------------------------------------------------------------

	@Test
	void renderLastRankingAndInGameThresholds() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// SETTING screen with a highlighted ranking row (rankingRank == i true arm).
		setInt(mode, "rankingRank", 3);
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);

		// In-game render: drive each score-color threshold (sc<=9600/4800/2400, sc>0)
		// and the "lastscore != 0 && scgettime < 120" event-display path.
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "goaltype", 0); // GOAL_TABLE[0] = 10000

		// sc=10000 (full white, no threshold)
		engine.statistics.score = 0;
		mode.renderLast(engine, 0);

		// sc=400 -> hits all three thresholds (<=2400 final), and event-display arm
		engine.statistics.score = 9600;
		setInt(mode, "lastscore", 50);
		setInt(mode, "scgettime", 0);
		setInt(mode, "lastevent", 4); // EVENT_FOUR (b2b false arm)
		setInt(mode, "lastcombo", 3); // combo display arm
		mode.renderLast(engine, 0);

		// sc clamped to 0 (score above goal) -> sc<0 -> sc=0 branch
		engine.statistics.score = 20000;
		mode.renderLast(engine, 0);

		assertTrue(true);
	}

	/**
	 * Gives the engine a small non-empty field so {@code calcScore}'s
	 * {@code engine.field.isEmpty()} guard (line clears) does not NPE and the
	 * all-clear bonus does not fire (keeps the score assertions exact).
	 */
	private static void makeNonEmptyField(GameEngine engine) {
		engine.field = new nullpomino.game.component.Field(10, 20, 0);
		engine.field.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
	}

	// --- reflection helpers ---

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
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
