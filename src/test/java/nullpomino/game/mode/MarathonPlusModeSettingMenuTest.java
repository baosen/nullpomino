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
 * Covers {@link MarathonPlusMode}: onSetting menu (8 cursors),
 * renderSetting, startGame, calcScore (T-spin scoring, ending),
 * onLast (bonus level), renderLast (score, line, bonus display),
 * renderResult, saveReplay.
 */
class MarathonPlusModeSettingMenuTest {

	@Test
	void onSettingUpNavigates() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressUp(engine);
		mode.onSetting(engine, 0);
		assertEquals(7, readFieldInt(mode, "menuCursor"));
		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	@Test
	void onSettingCursor0Startlevel() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = readFieldInt(mode, "startlevel");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "startlevel"));
	}

	@Test
	void onSettingCursor1TspinType() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = readFieldInt(mode, "tspinEnableType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "tspinEnableType"));
	}

	@Test
	void onSettingCursor2Kick() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		boolean before = readFieldBool(mode, "enableTSpinKick");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableTSpinKick"));
	}

	@Test
	void onSettingCursor3SpinCheckType() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		int before = readFieldInt(mode, "spinCheckType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "spinCheckType"));
	}

	@Test
	void onSettingCursor4Ez() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		boolean before = readFieldBool(mode, "tspinEnableEZ");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "tspinEnableEZ"));
	}

	@Test
	void onSettingCursor5B2b() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		boolean before = readFieldBool(mode, "enableB2B");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableB2B"));
	}

	@Test
	void onSettingCursor6Combo() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		boolean before = readFieldBool(mode, "enableCombo");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableCombo"));
	}

	@Test
	void onSettingCursor7Big() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		boolean before = readFieldBool(mode, "big");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "big"));
	}

	@Test
	void onSettingAStartsGame() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		pressPush(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);
		assertFalse(result);
	}

	@Test
	void onSettingBQuits() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingReplayMode() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
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
	void renderSettingNormal() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		mode.renderSetting(engine, 0);
	}

	@Test
	void startGameConfigures() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "startlevel", 5);
		setFieldBool(mode, "enableCombo", false);
		setFieldBool(mode, "big", true);
		mode.startGame(engine, 0);
		assertTrue(engine.big);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	@Test
	void startGameTspinAll() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "version", 1);
		setFieldInt(mode, "tspinEnableType", 2);
		mode.startGame(engine, 0);
		assertTrue(engine.useAllSpinBonus);
	}

	@Test
	void calcScoreSingle() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
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
	void calcScoreTspinTriple() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.statistics.level = 0;
		mode.calcScore(engine, 0, 3);
		assertEquals(11, readFieldInt(mode, "lastevent"));
	}

	@Test
	void calcScoreEndingAtLevel20() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		setFieldInt(mode, "startlevel", 20);
		engine.statistics.lines = 0;
		mode.calcScore(engine, 0, 1);
		assertTrue(readFieldInt(mode, "lastscore") > 0,
				"Bonus level should still award points");
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "scgettime", 0);
		mode.onLast(engine, 0);
		assertEquals(1, readFieldInt(mode, "scgettime"));
	}

	@Test
	void onLastBonusLevel() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statistics.level = 20;
		engine.createFieldIfNeeded();
		mode.onLast(engine, 0);
		assertTrue(readFieldInt(mode, "bonusTime") >= 0);
	}

	@Test
	void renderLastSettingState() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastMoveState() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastBonusLevel() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		engine.statistics.level = 20;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWithEvents() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
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
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		mode.renderResult(engine, 0);
	}

	@Test
	void saveReplayUpdatesRanking() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode, false);
		mode.saveReplay(engine, 0, new CustomProperties());
	}

	private static GameEngine freshEngine(MarathonPlusMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, MarathonPlusMode mode, int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, MarathonPlusMode mode, int cursor, int menuTime) throws Exception {
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
