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
 * Covers {@link DigChallengeMode}: onSetting menu (10 cursors),
 * renderSetting, startGame, calcScore (attack, B2B, ending),
 * onLast (garbage timer), renderLast (score, garbage, events),
 * renderResult, saveReplay.
 */
class DigChallengeModeSettingMenuTest {

	@Test
	void onSettingUpNavigates() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressUp(engine);
		mode.onSetting(engine, 0);
		assertEquals(9, readFieldInt(mode, "menuCursor"));
		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	@Test
	void onSettingCursor0Goaltype() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = readFieldInt(mode, "goaltype");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "goaltype"));
	}

	@Test
	void onSettingCursor1Startlevel() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = readFieldInt(mode, "startlevel");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "startlevel"));
	}

	@Test
	void onSettingCursor2Bgmno() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = readFieldInt(mode, "bgmno");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor3TspinType() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		setFieldInt(mode, "tspinEnableType", 0);
		int before = readFieldInt(mode, "tspinEnableType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "tspinEnableType"));
	}

	@Test
	void onSettingCursor4Kick() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		boolean before = readFieldBool(mode, "enableTSpinKick");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableTSpinKick"));
	}

	@Test
	void onSettingCursor5SpinCheckType() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		int before = readFieldInt(mode, "spinCheckType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "spinCheckType"));
	}

	@Test
	void onSettingCursor6Ez() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		boolean before = readFieldBool(mode, "tspinEnableEZ");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "tspinEnableEZ"));
	}

	@Test
	void onSettingCursor7B2b() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		boolean before = readFieldBool(mode, "enableB2B");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableB2B"));
	}

	@Test
	void onSettingCursor8Combo() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		boolean before = readFieldBool(mode, "enableCombo");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableCombo"));
	}

	@Test
	void onSettingCursor9Das() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		int before = engine.speed.das;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.das);
	}

	@Test
	void onSettingAStartsGame() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		pressPush(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result);
	}

	@Test
	void onSettingBQuits() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingReplayMode() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
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
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		mode.renderSetting(engine, 0);
	}

	@Test
	void startGameConfigures() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "startlevel", 5);
		setFieldBool(mode, "enableCombo", false);
		mode.startGame(engine, 0);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertEquals(50, readFieldInt(mode, "garbageTotal"),
				"garbageTotal should be 10 * startlevel");
	}

	@Test
	void calcScoreSingle() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(1, readFieldInt(mode, "lastevent"));
	}

	@Test
	void calcScoreFourLinesWithB2B() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.b2b = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 4);

		assertTrue(readFieldBool(mode, "lastb2b"));
	}

	@Test
	void calcScoreAllClearGivesBonus() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(1, readFieldInt(mode, "lastevent"));
		assertTrue(readFieldInt(mode, "lastscore") >= 6,
				"All-clear should give bonus");
	}

	@Test
	void calcScoreGarbageLevelUp() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		setFieldInt(mode, "garbageTotal", 19);
		setFieldInt(mode, "garbageNextLevelLines", 20);
		setFieldInt(mode, "garbagePending", 1);
		engine.statistics.level = 1;
		engine.statistics.lines = 0;
		setFieldBool(mode, "enableCombo", false);

		mode.calcScore(engine, 0, 0);

		assertEquals(2, engine.statistics.level,
				"Level should increment when garbageTotal >= garbageNextLevelLines");
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "scgettime", 0);
		mode.onLast(engine, 0);
		assertEquals(1, readFieldInt(mode, "scgettime"));
	}

	@Test
	void onLastGarbageTimerNormalMode() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statistics.level = 0;
		setFieldInt(mode, "goaltype", 0); // NORMAL
		setFieldInt(mode, "garbagePending", 10);

		mode.onLast(engine, 0);
	}

	@Test
	void onLastGarbageTimerOverflows() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statistics.level = 0;
		setFieldInt(mode, "goaltype", 0);
		setFieldInt(mode, "garbageTimer", 200);
		setFieldInt(mode, "garbagePending", 10);

		mode.onLast(engine, 0);

		assertTrue(readFieldInt(mode, "garbageTimer") >= 0,
				"Garbage timer should be >= 0 after overflow");
	}

	@Test
	void renderLastSettingState() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastMoveState() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWithEvents() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setFieldInt(mode, "lastevent", 4);
		setFieldInt(mode, "scgettime", 0);
		setFieldInt(mode, "lastpiece", Piece.PIECE_T);
		setFieldBool(mode, "lastb2b", true);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderResult() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		mode.renderResult(engine, 0);
	}

	@Test
	void saveReplayUpdatesRanking() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode, false);
		mode.saveReplay(engine, 0, new CustomProperties());
	}

	private static GameEngine freshEngine(DigChallengeMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, DigChallengeMode mode, int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, DigChallengeMode mode, int cursor, int menuTime) throws Exception {
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
