package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link VSBattleMode}: onSetting menu (28 cursors across 3
 * pages), renderSetting, calcScore (attack pts, B2B, combo, all-clear,
 * garbage rise), onLast (hurryup, garbage meter, settlement),
 * renderLast (hurryup display, event display), renderResult, onReady,
 * startGame, and saveReplay.
 */
class VSBattleModeSettingMenuTest {

	@Test
	void onSettingUpNavigates() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressUp(engine);
		mode.onSetting(engine, 0);
		assertEquals(27, readFieldInt(mode, "menuCursor"));
		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	@Test
	void onSettingCursor0Gravity() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = engine.speed.gravity;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor1Denominator() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = engine.speed.denominator;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2Are() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = engine.speed.are;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLine() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		int before = engine.speed.areLine;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		int before = engine.speed.lineDelay;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		int before = engine.speed.lockDelay;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor6Das() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		int before = engine.speed.das;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.das);
	}

	@Test
	void onSettingCursor7PresetLoad() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7, 10);
		engine.speed.gravity = 42;
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(4, engine.speed.gravity,
				"A at cursor 7 should load preset");
	}

	@Test
	void onSettingCursor8PresetSave() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8, 10);
		engine.speed.gravity = 99;
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.owner.modeConfig.getProperty(
				"vsbattle.gravity.0", -1),
				"A at cursor 8 should save preset");
	}

	@Test
	void onSettingCursor9GarbageType() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		pressRight(engine);
		mode.onSetting(engine, 0);
	}

	@Test
	void onSettingCursor10GarbagePercent() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		((int[]) readField(mode, "garbagePercent"))[0] = 50;
		int before = ((int[]) readField(mode, "garbagePercent"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, ((int[]) readField(mode, "garbagePercent"))[0]);
	}

	@Test
	void onSettingCursor11GarbageCounter() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 11);
		boolean before = ((boolean[]) readField(mode, "garbageCounter"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, ((boolean[]) readField(mode, "garbageCounter"))[0]);
	}

	@Test
	void onSettingCursor12GarbageBlocking() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 12);
		boolean before = ((boolean[]) readField(mode, "garbageBlocking"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, ((boolean[]) readField(mode, "garbageBlocking"))[0]);
	}

	@Test
	void onSettingCursor13TspinType() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 13);
		int before = ((int[]) readField(mode, "tspinEnableType"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, ((int[]) readField(mode, "tspinEnableType"))[0]);
	}

	@Test
	void onSettingCursor14Kick() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 14);
		boolean before = ((boolean[]) readField(mode, "enableTSpinKick"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, ((boolean[]) readField(mode, "enableTSpinKick"))[0]);
	}

	@Test
	void onSettingCursor15SpinType() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 15);
		int before = ((int[]) readField(mode, "spinCheckType"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, ((int[]) readField(mode, "spinCheckType"))[0]);
	}

	@Test
	void onSettingCursor16Ez() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16);
		boolean before = ((boolean[]) readField(mode, "tspinEnableEZ"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, ((boolean[]) readField(mode, "tspinEnableEZ"))[0]);
	}

	@Test
	void onSettingCursor17B2b() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 17);
		int before = ((int[]) readField(mode, "b2bType"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, ((int[]) readField(mode, "b2bType"))[0]);
	}

	@Test
	void onSettingCursor18Combo() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 18);
		boolean before = ((boolean[]) readField(mode, "enableCombo"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, ((boolean[]) readField(mode, "enableCombo"))[0]);
	}

	@Test
	void onSettingCursor19Big() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 19);
		boolean before = ((boolean[]) readField(mode, "big"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, ((boolean[]) readField(mode, "big"))[0]);
	}

	@Test
	void onSettingCursor20Se() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 20);
		boolean before = ((boolean[]) readField(mode, "enableSE"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, ((boolean[]) readField(mode, "enableSE"))[0]);
	}

	@Test
	void onSettingCursor21Hurryup() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 21);
		int before = ((int[]) readField(mode, "hurryupSeconds"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, ((int[]) readField(mode, "hurryupSeconds"))[0]);
	}

	@Test
	void onSettingCursor22HurryupInterval() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 22);
		int before = ((int[]) readField(mode, "hurryupInterval"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, ((int[]) readField(mode, "hurryupInterval"))[0]);
	}

	@Test
	void onSettingCursor23Bgmno() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 23);
		int before = readFieldInt(mode, "bgmno");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor24ShowStats() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 24);
		boolean before = readFieldBool(mode, "showStats");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "showStats"));
	}

	@Test
	void onSettingCursor25UseMap() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 25);
		boolean before = ((boolean[]) readField(mode, "useMap"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, ((boolean[]) readField(mode, "useMap"))[0]);
	}

	@Test
	void onSettingCursor26MapSet() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 26);
		int before = ((int[]) readField(mode, "mapSet"))[0];
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, ((int[]) readField(mode, "mapSet"))[0]);
	}

	@Test
	void onSettingCursor27MapNumber() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 27);
		pressRight(engine);
		mode.onSetting(engine, 0);
	}

	@Test
	void onSettingAAtCursor7Start() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4],
				"A should advance to waiting state");
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	// ---------------------------------------------------------------
	// renderSetting pages
	// ---------------------------------------------------------------

	@Test
	void renderSettingCursorBelow9() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingCursorBetween9And19() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 9);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingCursor19AndAbove() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 19);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingWaitState() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 1;
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// calcScore branches
	// ---------------------------------------------------------------

	@Test
	void calcScoreSingleNoAttack() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		// Set a block so field is not empty
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		mode.modeInit(engine.owner);
		initGarbageEntries(mode);

		mode.calcScore(engine, 0, 1);

		assertEquals(1, ((int[]) readField(mode, "lastevent"))[0]);
	}

	@Test
	void calcScoreFourLinesWithB2B() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.b2b = true;
		mode.modeInit(engine.owner);
		initGarbageEntries(mode);

		mode.calcScore(engine, 0, 4);

		assertTrue(((boolean[]) readField(mode, "lastb2b"))[0]);
	}

	@Test
	void calcScoreTspinDouble() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;
		engine.tspinmini = false;
		mode.modeInit(engine.owner);
		initGarbageEntries(mode);

		mode.calcScore(engine, 0, 2);
	}

	@Test
	void calcScoreHurryupFloor() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();
		engine.timerActive = true;
		mode.modeInit(engine.owner);
		initGarbageEntries(mode);
		((int[]) readField(mode, "hurryupSeconds"))[0] = 0;
		((int[]) readField(mode, "hurryupInterval"))[0] = 1;
		engine.statistics.time = 1;

		mode.calcScore(engine, 0, 0);
		// Verify no crash - hurryup floor added
	}

	// ---------------------------------------------------------------
	// onLast branches
	// ---------------------------------------------------------------

	@Test
	void onLastHurryupSound() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		mode.modeInit(engine.owner);
		engine.timerActive = true;
		((int[]) readField(mode, "hurryupSeconds"))[0] = 1;
		engine.statistics.time = 60;
		mode.onLast(engine, 0);
	}

	@Test
	void onLastMeterUpdate() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		mode.modeInit(engine.owner);
		((int[]) readField(mode, "garbage"))[0] = 3;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void onLastPlayer1Wins() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		manager.modeConfig = new CustomProperties();
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		manager.engine[1].owner.modeConfig = new CustomProperties();
		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		engine0.gameActive = true;
		engine1.gameActive = true;
		engine0.stat = GameEngine.Status.MOVE;
		engine1.stat = GameEngine.Status.GAMEOVER;

		mode.onLast(engine1, 1);

		assertEquals(0, readFieldInt(mode, "winnerID"));
	}

	@Test
	void onLastDraw() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = false;
		manager.modeConfig = new CustomProperties();
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		manager.engine[1].owner.modeConfig = new CustomProperties();
		GameEngine engine1 = manager.engine[1];

		engine1.gameActive = true;
		manager.engine[0].stat = GameEngine.Status.GAMEOVER;
		manager.engine[1].stat = GameEngine.Status.GAMEOVER;

		mode.onLast(engine1, 1);

		assertEquals(-1, readFieldInt(mode, "winnerID"));
	}

	// ---------------------------------------------------------------
	// renderLast
	// ---------------------------------------------------------------

	@Test
	void renderLastPlayer0WithHurryup() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		mode.modeInit(engine.owner);
		engine.timerActive = true;
		setFieldBool(mode, "showStats", true);
		((int[]) readField(mode, "hurryupSeconds"))[0] = 1;
		engine.statistics.time = 61;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWithLineEvents() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		mode.modeInit(engine.owner);
		((int[]) readField(mode, "lastevent"))[0] = 1;
		((int[]) readField(mode, "scgettime"))[0] = 0;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderResult() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "winnerID", 0);
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultDraw() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "winnerID", -1);
		mode.renderResult(engine, 0);
	}

	// ---------------------------------------------------------------
	// startGame
	// ---------------------------------------------------------------

	@Test
	void startGameDisablesCombo() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		mode.modeInit(engine.owner);
		((boolean[]) readField(mode, "enableCombo"))[0] = false;

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	@Test
	void startGameVersion4TspinAll() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		mode.modeInit(engine.owner);
		setFieldInt(mode, "version", 4);
		((int[]) readField(mode, "tspinEnableType"))[0] = 2;

		mode.startGame(engine, 0);

		assertTrue(engine.useAllSpinBonus);
	}

	// ---------------------------------------------------------------
	// onReady map logic
	// ---------------------------------------------------------------

	@Test
	void onReadyResetsField() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		mode.modeInit(engine.owner);
		engine.createFieldIfNeeded();

		mode.onReady(engine, 0);
	}

	// ---------------------------------------------------------------
	// saveReplay
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesVersion() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, false);
		mode.modeInit(engine.owner);

		mode.saveReplay(engine, 0, new CustomProperties());

		assertEquals(5, engine.owner.replayProp.getProperty("vsbattle.version", -1));
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(VSBattleMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, VSBattleMode mode, int cursor)
			throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, VSBattleMode mode,
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
	@SuppressWarnings({"unchecked", "rawtypes"})
	private static void initGarbageEntries(Object mode) throws Exception {
		Field f = findField(mode.getClass(), "garbageEntries");
		java.util.LinkedList[] arr = (java.util.LinkedList[]) f.get(mode);
		if (arr != null) {
			for (int i = 0; i < arr.length; i++) {
				if (arr[i] == null) arr[i] = new java.util.LinkedList<>();
			}
		}
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
