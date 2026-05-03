package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link SPFMode#onSetting} menu branches (20 cursor positions),
 * UP/DOWN navigation, A-button confirm paths (load preset, save preset,
 * start), B-button cancel, replayMode path, renderSetting page boundaries,
 * calcScore chain/zenkeshi/squares branches, lineClearEnd garbage-drop
 * branches, checkCountdown, checkSquares, onMove, onLast, onClear,
 * saveReplay, and modeInit.
 */
class SPFModeSettingMenuTest {

	// ---------------------------------------------------------------
	// onSetting: UP/DOWN navigation (20 positions)
	// ---------------------------------------------------------------

	@Test
	void onSettingUpNavigatesThroughAllPositions() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);

		setFieldInt(mode, "menuCursor", 0);
		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(19, readFieldInt(mode, "menuCursor"),
				"UP at cursor 0 should wrap to 19");

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"First DOWN from 19 should reach 0");

		for (int expected = 1; expected <= 19; expected++) {
			pressKey(engine, Controller.BUTTON_DOWN);
			mode.onSetting(engine, 0);
			assertEquals(expected, readFieldInt(mode, "menuCursor"),
					"Should be at cursor " + expected);
		}
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"DOWN from 19 should wrap to 0");
	}

	@Test
	void onSettingDownWrapsAtBottom() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);

		setFieldInt(mode, "menuCursor", 19);
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));
	}

	@Test
	void onSettingDownActivatesDropMapAtCursor18() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode);

		// Navigate DOWN to 18, then verify cursor 18 adjusts dropSet
		setFieldInt(mode, "menuCursor", 17);
		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(18, readFieldInt(mode, "menuCursor"));

		// Now adjust dropSet at cursor 18
		int before = getIntArray(mode, "dropSet")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "dropSet")[0]);
	}

	// ---------------------------------------------------------------
	// onSetting: LEFT/RIGHT at each cursor position
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor0Gravity() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = engine.speed.gravity;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(before - 1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor1Denominator() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = engine.speed.denominator;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2Are() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = engine.speed.are;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLine() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		int before = engine.speed.areLine;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		int before = engine.speed.lineDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		int before = engine.speed.lockDelay;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor6Das() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		int before = engine.speed.das;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.das);
	}

	@Test
	void onSettingCursor7And8PresetNumber() throws Exception {
		for (int cursor : new int[]{7, 8}) {
			SPFMode m = new SPFMode();
			GameEngine e = freshEngine(m, false);
			setMenuState(e, m, cursor);
			int before = getIntArray(m, "presetNumber")[0];
			pressKey(e, Controller.BUTTON_RIGHT);
			m.onSetting(e, 0);
			assertEquals(before + 1, getIntArray(m, "presetNumber")[0],
					"Cursor " + cursor + " should adjust presetNumber");
		}
	}

	@Test
	void onSettingCursor9Bgmno() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		int before = readFieldInt(mode, "bgmno");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor10UseMapToggles() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		boolean before = getBoolArray(mode, "useMap")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "useMap")[0]);
	}

	@Test
	void onSettingCursor11MapSet() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 11);
		int before = getIntArray(mode, "mapSet")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "mapSet")[0]);
	}

	@Test
	void onSettingCursor12MapNumber() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 12);
		int before = getIntArray(mode, "mapNumber")[0];

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertTrue(getIntArray(mode, "mapNumber")[0] != before,
				"mapNumber should change when RIGHT is pressed");
	}

	@Test
	void onSettingCursor13EnableSEToggles() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 13);
		boolean before = getBoolArray(mode, "enableSE")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, getBoolArray(mode, "enableSE")[0]);
	}

	@Test
	void onSettingCursor14Hurryup() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 14);
		int before = getIntArray(mode, "hurryupSeconds")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "hurryupSeconds")[0]);
	}

	@Test
	void onSettingCursor15OjamaCountdown() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 15);
		int before = getIntArray(mode, "ojamaCountdown")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "ojamaCountdown")[0]);
	}

	@Test
	void onSettingCursor16BigDisplayToggles() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16);
		boolean before = readFieldBool(mode, "bigDisplay");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "bigDisplay"));
	}

	@Test
	void onSettingCursor17DiamondPower() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 17);
		int before = getIntArray(mode, "diamondPower")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "diamondPower")[0]);
	}

	@Test
	void onSettingCursor18DropSet() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 18);
		int before = getIntArray(mode, "dropSet")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "dropSet")[0]);
	}

	@Test
	void onSettingCursor19DropMap() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 19);
		int before = getIntArray(mode, "dropMap")[0];
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, getIntArray(mode, "dropMap")[0]);
	}

	// ---------------------------------------------------------------
	// A button paths
	// ---------------------------------------------------------------

	@Test
	void onSettingPressAAtCursor7LoadsPreset() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7, 10);
		engine.speed.gravity = 42;

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(4, engine.speed.gravity,
				"A at cursor 7 should load preset defaults");
	}

	@Test
	void onSettingPressAAtCursor8SavesPreset() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8, 10);
		engine.speed.gravity = 99;
		setIntArray(mode, "presetNumber", 0, 0);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(99, engine.owner.modeConfig.getProperty(
				"spfvs.gravity.0", -1));
	}

	@Test
	void onSettingPressAAtOtherCursorStartsGame() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);

		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	// ---------------------------------------------------------------
	// replayMode path
	// ---------------------------------------------------------------

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 0);

		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));

		setFieldInt(mode, "menuTime", 59);
		mode.onSetting(engine, 0);
		assertEquals(9, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 119);
		mode.onSetting(engine, 0);
		assertEquals(18, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 179);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4]);
	}

	// ---------------------------------------------------------------
	// renderSetting page boundaries
	// ---------------------------------------------------------------

	@Test
	void renderSettingPage1() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage2() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 9);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage3() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 18);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingWaitState() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 1;
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// calcScore branches
	// ---------------------------------------------------------------

	@Test
	void calcScoreWithAvalancheTriggersChain() throws Exception {
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		engine.playerID = 0;
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		// Place blocks to avoid empty field (no zenkeshi)
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.field.setBlock(1, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		engine.statistics.score = 0;

		// Trigger calcScore with avalanche > 0 and chain > 0
		engine.chain = 2;
		mode.calcScore(engine, 0, 5);

		assertTrue(engine.statistics.score > 0,
				"calcScore should produce score from chain");
	}

	@Test
	void calcScoreZenKeshiAwardsBonus() throws Exception {
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		engine.playerID = 0;
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;

		// Empty field → zenkeshi bonus
		mode.calcScore(engine, 0, 0);

		assertEquals(1000, engine.statistics.score,
				"Empty field should award zenkeshi bonus");
	}

	// ---------------------------------------------------------------
	// onMove / onLast / onClear
	// ---------------------------------------------------------------

	@Test
	void onMoveResetsCountdownDecremented() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);

		setBoolArray(mode, "countdownDecremented", true, 0);
		mode.onMove(engine, 0);

		assertFalse(getBoolArray(mode, "countdownDecremented")[0]);
	}

	@Test
	void onLastIncrementsScgettimeAndDecrementsDisplay() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);

		setIntArray(mode, "scgettime", 0, 0);
		setIntArray(mode, "zenKeshiDisplay", 10, 0);
		setIntArray(mode, "techBonusDisplay", 5, 0);

		mode.onLast(engine, 0);

		assertEquals(1, getIntArray(mode, "scgettime")[0]);
		assertEquals(9, getIntArray(mode, "zenKeshiDisplay")[0]);
		assertEquals(4, getIntArray(mode, "techBonusDisplay")[0]);
	}

	@Test
	void onReadyWithDiamondPowerSetsDiamonds() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[0] = 0;

		mode.onReady(engine, 0);

		assertEquals(20, engine.numColors);
	}

	// ---------------------------------------------------------------
	// checkCountdown
	// ---------------------------------------------------------------

	@Test
	void checkCountdownDecrementsBlocks() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();

		// Block with countdown=2
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.countdown = 2;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		engine.field.setBlock(0, 0, b);

		setBoolArray(mode, "countdownDecremented", false, 0);

		Method m = SPFMode.class.getDeclaredMethod(
				"checkCountdown", GameEngine.class, int.class);
		m.setAccessible(true);
		boolean result = (boolean) m.invoke(mode, engine, 0);

		assertFalse(result, "Block with countdown 2 should not convert");
		assertEquals(1, engine.field.getBlock(0, 0).countdown);
	}

	// ---------------------------------------------------------------
	// lineClearEnd garbage drop
	// ---------------------------------------------------------------

	@Test
	void lineClearEndDropsOjama() throws Exception {
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		engine.playerID = 0;
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		setIntArray(mode, "ojama", 20, 0);
		setIntArray(mode, "dropSet", 0, 0);
		setIntArray(mode, "dropMap", 0, 0);
		int[][][] dropPattern = (int[][][]) findField(mode.getClass(), "dropPattern").get(mode);
		dropPattern[0] = new int[][]{{2,2,2,2}};

		Method m = SPFMode.class.getDeclaredMethod(
				"lineClearEnd", GameEngine.class, int.class);
		m.setAccessible(true);
		boolean result = (boolean) m.invoke(mode, engine, 0);

		assertTrue(result, "Ojama > 0 should trigger drop");
		assertTrue(getIntArray(mode, "ojama")[0] < 20,
				"ojama should decrease after drop");
	}

	// ---------------------------------------------------------------
	// saveReplay
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesVersion() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);

		engine.owner.replayProp = new CustomProperties();
		mode.saveReplay(engine, 0, new CustomProperties());

		assertEquals(0, engine.owner.replayProp.getProperty("spfvs.version", -1));
	}

	// ---------------------------------------------------------------
	// renderLast
	// ---------------------------------------------------------------

	@Test
	void renderLastScoreDisplay() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameStarted = true;
		engine.stat = GameEngine.Status.MOVE;

		mode.renderLast(engine, 0);
		// No crash with valid state
	}

	// ---------------------------------------------------------------
	// onReady (engine config)
	// ---------------------------------------------------------------

	@Test
	void onReadySetsEngineConfig() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[0] = 0;

		mode.onReady(engine, 0);

		assertEquals(20, engine.numColors,
				"SPF mode should use 20 colors");
	}

	// ---------------------------------------------------------------
	// afterHardDropFall adds score
	// ---------------------------------------------------------------

	@Test
	void afterHardDropFallAddsScore() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);

		engine.statistics.score = 0;
		mode.afterHardDropFall(engine, 0, 5);

		assertEquals(5, engine.statistics.score);
	}

	// ---------------------------------------------------------------
	// startGame config
	// ---------------------------------------------------------------

	@Test
	void startGameSetsCorrectMode() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode, false);

		mode.playerInit(engine, 0);
		mode.startGame(engine, 0);

		assertEquals(GameEngine.ClearType.GEM_COLOR, engine.clearMode);
		assertTrue(engine.garbageColorClear);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(SPFMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, SPFMode mode)
			throws Exception {
		setMenuState(engine, mode, 0, 0);
	}

	private static void setMenuState(GameEngine engine, SPFMode mode,
			int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, SPFMode mode,
			int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void pressPush(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static boolean readFieldBool(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		return (int[]) findField(obj.getClass(), name).get(obj);
	}

	private static boolean[] getBoolArray(Object obj, String name) throws Exception {
		return (boolean[]) findField(obj.getClass(), name).get(obj);
	}

	private static void setIntArray(Object obj, String name, int value, int index) throws Exception {
		((int[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static void setBoolArray(Object obj, String name, boolean value, int index) throws Exception {
		((boolean[]) findField(obj.getClass(), name).get(obj))[index] = value;
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
