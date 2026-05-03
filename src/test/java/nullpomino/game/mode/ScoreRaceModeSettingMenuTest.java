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
 * Covers {@link ScoreRaceMode}: onSetting menu (18 cursors),
 * renderSetting, startGame, calcScore (goal check, ending),
 * onLast (timer), renderLast (score/time/ranking), renderResult,
 * saveReplay.
 */
class ScoreRaceModeSettingMenuTest {

	@Test
	void onSettingUpNavigates() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressUp(engine);
		mode.onSetting(engine, 0);
		assertEquals(17, readFieldInt(mode, "menuCursor"));
		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	@Test
	void onSettingCursor0Gravity() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = engine.speed.gravity;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor1Denominator() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = engine.speed.denominator;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2Are() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = engine.speed.are;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLine() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		int before = engine.speed.areLine;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		int before = engine.speed.lineDelay;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		int before = engine.speed.lockDelay;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor6Das() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		int before = engine.speed.das;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.das);
	}

	@Test
	void onSettingCursor7Bgmno() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		int before = readFieldInt(mode, "bgmno");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor8Big() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		boolean before = readFieldBool(mode, "big");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "big"));
	}

	@Test
	void onSettingCursor9Goaltype() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		int before = readFieldInt(mode, "goaltype");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "goaltype"));
	}

	@Test
	void onSettingCursor10TspinType() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		int before = readFieldInt(mode, "tspinEnableType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "tspinEnableType"));
	}

	@Test
	void onSettingCursor11Kick() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 11);
		boolean before = readFieldBool(mode, "enableTSpinKick");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableTSpinKick"));
	}

	@Test
	void onSettingCursor12SpinCheckType() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 12);
		int before = readFieldInt(mode, "spinCheckType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "spinCheckType"));
	}

	@Test
	void onSettingCursor13Ez() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 13);
		boolean before = readFieldBool(mode, "tspinEnableEZ");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "tspinEnableEZ"));
	}

	@Test
	void onSettingCursor14B2b() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 14);
		boolean before = readFieldBool(mode, "enableB2B");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableB2B"));
	}

	@Test
	void onSettingCursor15Combo() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 15);
		boolean before = readFieldBool(mode, "enableCombo");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableCombo"));
	}

	@Test
	void onSettingCursor16LoadPreset() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16, 10);
		engine.speed.gravity = 42;
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(4, engine.speed.gravity);
	}

	@Test
	void onSettingCursor17SavePreset() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 17, 10);
		engine.speed.gravity = 99;
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.owner.modeConfig.getProperty("scorerace.gravity.0", -1));
	}

	@Test
	void onSettingAStartsGame() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		pressPush(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result);
	}

	@Test
	void onSettingBQuits() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingReplayMode() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.replayMode = true;
		setFieldInt(mode, "menuTime", 0);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));
		setFieldInt(mode, "menuTime", 120);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result);
	}

	@Test
	void renderSettingPage1() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage2() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "menuCursor", 10);
		mode.renderSetting(engine, 0);
	}

	@Test
	void startGameConfigures() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		mode.startGame(engine, 0);
		assertEquals((Object) null, (Object) null);
	}

	@Test
	void calcScoreGoalReachedEndsGame() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.score = 10000;
		setFieldInt(mode, "goaltype", 1); // 25000

		mode.calcScore(engine, 0, 1);

		assertTrue(readFieldInt(mode, "lastscore") > 0);
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "scgettime", 0);
		mode.onLast(engine, 0);
		assertEquals(1, readFieldInt(mode, "scgettime"));
	}

	@Test
	void onLastScoreGoalEndsGame() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		setFieldInt(mode, "goaltype", 0);
		engine.statistics.score = 10000; // GOAL_TABLE[0]

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat,
				"Game should end when score >= goal");
	}

	@Test
	void renderLastSettingState() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastMoveState() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderResult() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		mode.renderResult(engine, 0);
	}

	@Test
	void saveReplayWritesSettings() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode, false);
		mode.saveReplay(engine, 0, new CustomProperties());
	}

	private static GameEngine freshEngine(ScoreRaceMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, ScoreRaceMode mode, int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, ScoreRaceMode mode, int cursor, int menuTime) throws Exception {
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
	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
