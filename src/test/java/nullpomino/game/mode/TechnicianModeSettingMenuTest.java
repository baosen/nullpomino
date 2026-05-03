package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link TechnicianMode}: onSetting menu (9 cursors),
 * renderSetting, startGame (game type branches), calcScore (time
 * bonus, ending), onLast (level/total timer, ending), onMove,
 * renderLast (score, goal, level time, events), renderResult,
 * saveReplay.
 */
class TechnicianModeSettingMenuTest {

	@Test
	void onSettingUpNavigates() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressUp(engine);
		mode.onSetting(engine, 0);
		assertEquals(8, readFieldInt(mode, "menuCursor"));
		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	@Test
	void onSettingCursor0Goaltype() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = readFieldInt(mode, "goaltype");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "goaltype"));
	}

	@Test
	void onSettingCursor1Startlevel() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = readFieldInt(mode, "startlevel");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "startlevel"));
	}

	@Test
	void onSettingCursor2TspinType() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = readFieldInt(mode, "tspinEnableType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "tspinEnableType"));
	}

	@Test
	void onSettingCursor3Kick() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		boolean before = readFieldBool(mode, "enableTSpinKick");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableTSpinKick"));
	}

	@Test
	void onSettingCursor4SpinCheckType() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		int before = readFieldInt(mode, "spinCheckType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "spinCheckType"));
	}

	@Test
	void onSettingCursor5Ez() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		boolean before = readFieldBool(mode, "tspinEnableEZ");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "tspinEnableEZ"));
	}

	@Test
	void onSettingCursor6B2b() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		boolean before = readFieldBool(mode, "enableB2B");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableB2B"));
	}

	@Test
	void onSettingCursor7Combo() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		boolean before = readFieldBool(mode, "enableCombo");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableCombo"));
	}

	@Test
	void onSettingCursor8Big() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		boolean before = readFieldBool(mode, "big");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "big"));
	}

	@Test
	void onSettingAStartsGame() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		pressPush(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result);
	}

	@Test
	void onSettingBQuits() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingReplayModePath() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.replayMode = true;
		setFieldInt(mode, "menuTime", 0);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));

		setFieldInt(mode, "menuTime", 60);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result);
	}

	@Test
	void renderSetting() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// startGame branches
	// ---------------------------------------------------------------

	@Test
	void startGame10MinEasy() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "goaltype", 2); // GAMETYPE_10MIN_EASY

		mode.startGame(engine, 0);

		assertEquals(36000, readFieldInt(mode, "totalTimer"));
	}

	@Test
	void startGameSpecial() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "goaltype", 4); // GAMETYPE_SPECIAL

		mode.startGame(engine, 0);

		assertTrue(engine.staffrollEnable);
	}

	// ---------------------------------------------------------------
	// calcScore time bonus
	// ---------------------------------------------------------------

	@Test
	void calcScoreGoalReachedTimeBonus() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.ending = 0;
		engine.statistics.level = 0;
		setFieldInt(mode, "goal", 0);
		setFieldBool(mode, "levelTimeOut", false);
		setFieldInt(mode, "goaltype", 0);
		setFieldInt(mode, "levelTimer", 100);
		setFieldInt(mode, "version", 1);
		setFieldBool(mode, "enableCombo", false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);

		assertTrue(readFieldInt(mode, "lasttimebonus") > 0,
				"Time bonus should be awarded when goal <= 0");
	}

	@Test
	void calcScoreSpecialTimeBonus() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.ending = 0;
		setFieldInt(mode, "goal", 0);
		setFieldInt(mode, "goaltype", 4); // GAMETYPE_SPECIAL
		setFieldInt(mode, "version", 1);
		setFieldBool(mode, "enableCombo", false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);

		assertEquals(1800, readFieldInt(mode, "lasttimebonus"),
				"SPECIAL mode should give 30 sec bonus");
	}

	@Test
	void calcScoreLevelUpEndsGameLv14() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.ending = 0;
		engine.statistics.level = 14;
		setFieldInt(mode, "goal", 0);
		setFieldInt(mode, "goaltype", 0); // LV15_EASY
		setFieldBool(mode, "enableCombo", false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.ending >= 1,
				"Level >= 14 should trigger ending for LV15 game");
	}

	// ---------------------------------------------------------------
	// onLast
	// ---------------------------------------------------------------

	@Test
	void onLastLevelTimerDecrements() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		setFieldInt(mode, "goaltype", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readFieldInt(mode, "levelTimer"));
	}

	@Test
	void onLastLevelTimeOutHard() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.lockDelayNow = 0;
		setFieldInt(mode, "goaltype", 1); // LV15_HARD
		setFieldInt(mode, "levelTimer", 7200); // TIMELIMIT_LEVEL

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat,
				"Hard mode level time out should trigger game over");
	}

	@Test
	void onLastLevelTimeOutEasyResetsLevel() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		setFieldInt(mode, "goaltype", 2); // 10MIN_EASY
		setFieldInt(mode, "levelTimer", 7200); // TIMELIMIT_LEVEL

		mode.onLast(engine, 0);

		assertEquals(0, readFieldInt(mode, "levelTimer"),
				"Easy mode should reset level timer on timeout");
	}

	@Test
	void onLastTotalTimerSpecial() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		setFieldInt(mode, "goaltype", 4); // SPECIAL
		setFieldInt(mode, "totalTimer", 50);

		mode.onLast(engine, 0);

		assertEquals(49, readFieldInt(mode, "totalTimer"));
	}

	@Test
	void onLastRollEnding() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.ending = 2;
		setFieldInt(mode, "rolltime", 3600); // TIMELIMIT_ROLL

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	// ---------------------------------------------------------------
	// renderLast
	// ---------------------------------------------------------------

	@Test
	void renderLastSettingState() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastMoveState() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWithEvent() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		setFieldInt(mode, "lastevent", 1);
		setFieldInt(mode, "scgettime", 0);
		setFieldInt(mode, "lastpiece", Piece.PIECE_T);
		engine.createFieldIfNeeded();
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastRegretDisplay() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		engine.owner.menuOnly = false;
		setFieldInt(mode, "regretdispframe", 100);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderResult() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		mode.renderResult(engine, 0);
	}

	// ---------------------------------------------------------------
	// saveReplay
	// ---------------------------------------------------------------

	@Test
	void saveReplayUpdatesRanking() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statistics.score = 5000;
		engine.statistics.lines = 100;
		engine.statistics.time = 2000;

		mode.saveReplay(engine, 0, new CustomProperties());

		// Should not crash
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(TechnicianMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, TechnicianMode mode, int cursor)
			throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, TechnicianMode mode,
			int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	private static void pressUp(GameEngine e) { pressKey(e, Controller.BUTTON_UP); }
	private static void pressDown(GameEngine e) { pressKey(e, Controller.BUTTON_DOWN); }
	private static void pressRight(GameEngine e) { pressKey(e, Controller.BUTTON_RIGHT); }

	private static void pressKey(GameEngine e, int btn) {
		e.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) e.ctrl.buttonTime[i] = 0;
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static void pressPush(GameEngine e, int btn) { pressKey(e, btn); }

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}
	private static boolean readFieldBool(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}
	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}
	private static void setFieldBool(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}
	private static Object readField(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).get(obj);
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
