package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

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
 * Branch-coverage tests for {@link TechnicianMode} targeting partially covered
 * conditional branches not reached by the existing Technician test suite:
 * onSetting net-send-options after a menu change, calcScore false-side
 * branches (T-spin zero with EZ enabled, combo with zero-line clear, combo
 * index clamp, level-time-out time-bonus skip, level-up background-clamp and
 * legacy-version paths, SPECIAL ending, level-cap clamp), onLast level/total
 * timeout variants, renderLast EVENT_SINGLE and zero-spin combo guard, and the
 * netIsNetRankingViewOK / saveReplay ranking guards.
 */
class TechnicianModeBranchCoverageTest3 {

	// ---------------------------------------------------------------
	// onSetting: net send options on a menu value change (line 235)
	// ---------------------------------------------------------------

	@Test
	void onSettingMenuChangeSignalsNetOptions() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		attachNetLobby(mode);
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 1);
		setInt(mode, "menuCursor", 6); // enableB2B toggle, any change != 0
		setInt(mode, "menuTime", 10);

		press(engine, Controller.BUTTON_RIGHT);

		// netSendOptions sends over the socket-less client (caught no-op).
		boolean result = mode.onSetting(engine, 0);
		assertTrue(result);
	}

	// ---------------------------------------------------------------
	// calcScore: T-spin zero-line with EZ enabled -> skips line-627 block
	// ---------------------------------------------------------------

	@Test
	void calcScoreTSpinZeroLinesWithEzDoesNotScore() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		engine.tspin = true;
		engine.tspinez = true; // (!engine.tspinez) is false at line 627
		setInt(mode, "lastevent", 99);

		// lines == 0 with tspinez true: none of the T-spin branches set lastevent.
		mode.calcScore(engine, 0, 0);

		assertEquals(99, readInt(mode, "lastevent"),
				"no scoring event should be recorded for a zero-line EZ T-spin");
	}

	// ---------------------------------------------------------------
	// calcScore: combo guard false-side (lines >= 1 fails) at line 714
	// ---------------------------------------------------------------

	@Test
	void calcScoreComboSkippedWhenNoLines() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		setBool(mode, "enableCombo", true);
		engine.combo = 3; // combo >= 1 true, but lines == 0 -> guard false
		engine.tspin = true;
		engine.tspinmini = true; // zero-line mini T-spin (scores pts, no combo)
		setInt(mode, "lastcombo", 0);

		mode.calcScore(engine, 0, 0);

		assertEquals(0, readInt(mode, "lastcombo"),
				"combo should not be recorded for a zero-line clear");
	}

	// ---------------------------------------------------------------
	// calcScore: combo index clamp (line 736) with a very large combo
	// ---------------------------------------------------------------

	@Test
	void calcScoreComboIndexClamped() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		setBool(mode, "enableCombo", true);
		engine.combo = 50; // cmbindex = 49 >= COMBO_GOAL_TABLE.length (12)
		setInt(mode, "goal", 100);
		// solid block on the floor so the field is not empty (skip all-clear)
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		// COMBO_GOAL_TABLE last entry is 5; single = pts/100/(lv+1) = 1, so 1 + 5.
		assertEquals(6, readInt(mode, "lastgoal"),
				"combo index should clamp to the table maximum (5)");
	}

	// ---------------------------------------------------------------
	// calcScore: goal reached but level timed out -> time bonus skipped (744)
	// ---------------------------------------------------------------

	@Test
	void calcScoreNoTimeBonusWhenLevelTimedOut() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 0;
		setInt(mode, "goal", 0);
		setBool(mode, "levelTimeOut", true); // false-side of levelTimeOut==false
		setInt(mode, "goaltype", 0); // LV15_EASY (not SPECIAL)
		setInt(mode, "lasttimebonus", 0);
		setBool(mode, "enableCombo", false);
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		assertEquals(0, readInt(mode, "lasttimebonus"),
				"no level time bonus when the level already timed out");
	}

	// ---------------------------------------------------------------
	// calcScore: SPECIAL ending at level >= 29 (line 773)
	// ---------------------------------------------------------------

	@Test
	void calcScoreSpecialEndingAtLevel29() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 29;
		setInt(mode, "goal", 0);
		setInt(mode, "goaltype", 4); // SPECIAL
		setInt(mode, "bgmlv", 5); // tableBGMChange[5] == -1, skip BGM block
		setBool(mode, "enableCombo", false);
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		assertEquals(2, engine.ending, "SPECIAL at level 29 should start the roll");
	}

	// ---------------------------------------------------------------
	// calcScore: plain level-up with background already at max and legacy
	// version (lines 785 false-side, 794 false-side)
	// ---------------------------------------------------------------

	@Test
	void calcScoreLevelUpBackgroundClampedLegacyVersion() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 25; // < 29 and not an ending trigger here
		setInt(mode, "goal", 0);
		setInt(mode, "goaltype", 2); // 10MIN_EASY -> ordinary level-up
		setInt(mode, "bgmlv", 5); // skip BGM change block
		setInt(mode, "version", 0); // version < 1 -> line 794 false-side
		setBool(mode, "enableCombo", false);
		engine.owner.backgroundStatus.bg = 19; // bg < 19 is false -> line 785 false
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		assertEquals(26, engine.statistics.level, "ordinary level-up increments level");
		assertFalse(engine.owner.backgroundStatus.fadesw,
				"background fade should not start when bg already at max");
	}

	// ---------------------------------------------------------------
	// calcScore: level cap clamp at 29 (line 783)
	// ---------------------------------------------------------------

	@Test
	void calcScoreLevelCapClampsAt29() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.ending = 0;
		engine.statistics.level = 29;
		setInt(mode, "goal", 0);
		setInt(mode, "goaltype", 3); // 10MIN_HARD -> ordinary level-up branch
		setInt(mode, "bgmlv", 5);
		setBool(mode, "enableCombo", false);
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		assertEquals(29, engine.statistics.level, "level must be capped at 29");
	}

	// ---------------------------------------------------------------
	// onLast: level time out in 10MIN_HARD (line 539 second operand)
	// ---------------------------------------------------------------

	@Test
	void onLastLevelTimeOut10MinHardGameOver() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.gameActive = true;
		engine.timerActive = true;
		setInt(mode, "goaltype", 3); // 10MIN_HARD
		setInt(mode, "levelTimer", 7200); // TIMELIMIT_LEVEL

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat,
				"10MIN-HARD level timeout should trigger game over");
	}

	// ---------------------------------------------------------------
	// onLast: total timer out in 10MIN_EASY -> ending start (line 576)
	// ---------------------------------------------------------------

	@Test
	void onLastTotalTimerOut10MinEasyEndingStart() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.gameActive = true;
		engine.timerActive = true;
		setInt(mode, "goaltype", 2); // 10MIN_EASY
		setInt(mode, "totalTimer", 0); // after -- = -1 < 0

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat,
				"10MIN total timeout should start the ending");
	}

	// ---------------------------------------------------------------
	// renderLast: EVENT_SINGLE switch case (line 453)
	// ---------------------------------------------------------------

	@Test
	void renderLastEventSingle() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		engine.ending = 0;
		setInt(mode, "regretdispframe", 0);
		setInt(mode, "lastevent", 1); // EVENT_SINGLE
		setInt(mode, "scgettime", 0);
		setInt(mode, "lastpiece", Piece.PIECE_T);
		setInt(mode, "lastcombo", 1); // < 2 -> combo display guard false (line 498)

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// renderLast: zero-spin event with combo -> combo-display guard false
	// (lastevent == EVENT_TSPIN_ZERO at line 498)
	// ---------------------------------------------------------------

	@Test
	void renderLastZeroSpinComboGuardFalse() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		engine.ending = 0;
		setInt(mode, "regretdispframe", 0);
		setInt(mode, "lastevent", 6); // EVENT_TSPIN_ZERO
		setInt(mode, "scgettime", 0);
		setInt(mode, "lastpiece", Piece.PIECE_T);
		setInt(mode, "lastcombo", 5); // >= 2 but event is ZERO -> no combo line

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// netIsNetRankingViewOK false-side (line 995): big enabled
	// ---------------------------------------------------------------

	@Test
	void netIsNetRankingViewOKFalseWhenBig() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "big", true); // (!big) is false

		Object result = invoke(mode, "netIsNetRankingViewOK",
				new Class<?>[]{GameEngine.class}, new Object[]{engine});

		assertEquals(Boolean.FALSE, result);
	}

	@Test
	void netIsNetRankingViewOKTrueAtDefaults() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "big", false);
		setInt(mode, "startlevel", 0);
		// engine.ai is null at default.

		Object result = invoke(mode, "netIsNetRankingViewOK",
				new Class<?>[]{GameEngine.class}, new Object[]{engine});

		assertEquals(Boolean.TRUE, result);
	}

	// ---------------------------------------------------------------
	// saveReplay: ranking guard false-side (line 856) when big is set
	// ---------------------------------------------------------------

	@Test
	void saveReplaySkipsRankingWhenBig() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		setBool(mode, "big", true); // (big == false) is false -> ranking skipped
		setInt(mode, "rankingRank", -1);
		engine.statistics.score = 9999;

		mode.saveReplay(engine, 0, new CustomProperties());

		assertEquals(-1, readInt(mode, "rankingRank"),
				"ranking must not update when big is enabled");
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
		manager.engine[0].owner.replayMode = false;
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
		mode.playerInit(manager.engine[0], 0);
		return manager.engine[0];
	}

	private static void attachNetLobby(TechnicianMode mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setField(mode, "netLobby", lobby);
	}

	private static void press(GameEngine e, int btn) {
		e.ctrl.reset();
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static Object invoke(Object obj, String name, Class<?>[] types, Object[] args)
			throws Exception {
		java.lang.reflect.Method m = findMethod(obj.getClass(), name, types);
		m.setAccessible(true);
		return m.invoke(obj, args);
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
		return field(obj.getClass(), name).getInt(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		field(obj.getClass(), name).set(obj, value);
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
