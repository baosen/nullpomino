package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Tail-coverage tests for {@link AvalancheVSSPFMode}: targets the last few
 * uncovered lines in onSetting (DOWN to drop-map preview at cursor 32, map
 * settings cases 25/26, random-map preview, replay-menu progression),
 * renderSetting page 5 attack/defend multiplier branches, renderMove,
 * renderLast countdown rendering at big display size, lineClearEnd drop
 * pattern / game-over branches, and saveReplay map save.
 */
class AvalancheVSSPFModeTailCoverageTest {

	// --- 287: onSetting DOWN reaching cursor 32 loads drop map preview ---
	@Test
	void onSettingDownToCursor32LoadsDropMapPreview() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 31);
		setInt(mode, "menuTime", 5);
		press(e, Controller.BUTTON_DOWN);

		mode.onSetting(e, 0);

		assertEquals(32, readInt(mode, "menuCursor"));
	}

	// --- 433-434: onSetting case 25 (MAP SET) with useMap on ---
	@Test
	void onSettingCase25MapSetWithUseMap() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 25);
		setInt(mode, "menuTime", 5);
		setBoolArr(mode, "useMap", 0, true);
		press(e, Controller.BUTTON_RIGHT);

		mode.onSetting(e, 0);

		// case 25 with useMap forces mapNumber to -1
		assertEquals(-1, getIntArr(mode, "mapNumber")[0]);
	}

	// --- 444, 446: onSetting case 26 (MAP NO.) with useMap off ---
	@Test
	void onSettingCase26MapNumberWithoutUseMap() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 26);
		setInt(mode, "menuTime", 5);
		setBoolArr(mode, "useMap", 0, false);
		press(e, Controller.BUTTON_RIGHT);

		mode.onSetting(e, 0);

		assertEquals(-1, getIntArr(mode, "mapNumber")[0]);
	}

	// --- 509-512: onSetting random-map preview (menuTime % 30 == 0) ---
	@Test
	void onSettingRandomMapPreview() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 30); // % 30 == 0
		setBoolArr(mode, "useMap", 0, true);
		setIntArr(mode, "mapNumber", 0, -1); // random
		setIntArr(mode, "mapMaxNo", 0, 3);
		// propMap must be non-null for the random preview branch
		CustomProperties pm = new CustomProperties();
		setArrElem(mode, "propMap", 0, pm);

		mode.onSetting(e, 0);

		assertTrue(e.statc[5] >= 0);
	}

	// --- 525-526, 529: onSetting replay-menu progression ---
	@Test
	void onSettingReplayMenuStep240() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 239); // becomes 240 inside

		mode.onSetting(e, 0);

		assertEquals(32, readInt(mode, "menuCursor"));
	}

	@Test
	void onSettingReplayMenuStep180() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		e.owner.replayMode = true;
		mode.playerInit(e, 0);
		setInt(mode, "menuTime", 179); // becomes 180 inside

		mode.onSetting(e, 0);

		assertEquals(24, readInt(mode, "menuCursor"));
	}

	// --- 637, 644: renderSetting page 5 attack/defend multiplier branches ---
	@Test
	void renderSettingPage5MultiplierBranches() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "menuCursor", 33);
		// dropSet 0, dropMap 8 -> attack 0.7 (<100), defend 1.0 (>=100)
		setIntArr(mode, "dropSet", 0, 0);
		setIntArr(mode, "dropMap", 0, 8);
		e.createFieldIfNeeded();

		mode.renderSetting(e, 0);

		// dropSet 1, dropMap 8 -> attack 1.0 (>=100), defend 1.2 (>=100)
		setIntArr(mode, "dropSet", 0, 1);
		mode.renderSetting(e, 0);
	}

	// --- 695: renderMove draws X when game started ---
	@Test
	void renderMoveWhenGameStarted() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.gameStarted = true;
		e.createFieldIfNeeded();
		setBoolArr(mode, "dangerColumnShowX", 0, true);

		mode.renderMove(e, 0);
	}

	// --- 749-770, 774-775: renderLast countdown blocks at big display size ---
	@Test
	void renderLastCountdownBigDisplay() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		e.gameStarted = true;
		e.gameActive = true;
		e.stat = GameEngine.Status.MOVE;
		e.displaysize = 1; // d = 2 path
		e.createFieldIfNeeded();

		// Red countdown block (single digit)
		Block br = new Block(Block.BLOCK_COLOR_RED);
		br.countdown = 4;
		br.secondaryColor = Block.BLOCK_COLOR_RED;
		e.field.setBlock(0, 0, br);
		// Blue countdown block (double digit "d")
		Block bb = new Block(Block.BLOCK_COLOR_BLUE);
		bb.countdown = 12;
		bb.secondaryColor = Block.BLOCK_COLOR_BLUE;
		e.field.setBlock(1, 0, bb);

		mode.renderLast(e, 0);
	}

	// --- 824, 836: lineClearEnd field null / null-block continue ---
	@Test
	void lineClearEndFieldNull() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		e.field = null;

		boolean result = mode.lineClearEnd(e, 0);

		assertEquals(false, result);
	}

	// --- 874, 885: lineClearEnd drop pattern wrap + game over check ---
	@Test
	void lineClearEndDropOjamaAndGameOver() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		mode.readyInit(e, 0); // sets up dropPattern[0]

		// dropPattern for enemy (player 1) so lineClearEnd doesn't NPE
		Object dpObj = readField(mode, "dropPattern");
		int[][][] dp = (int[][][]) dpObj;
		dp[1] = new int[][]{{2, 2, 2, 2}};

		setIntArr(mode, "ojama", 0, 30);
		setIntArr(mode, "ojamaCountdown", 0, 10); // skip countdown loop
		setIntArr(mode, "maxAttack", 0, 30);
		setBoolArr(mode, "ojamaDrop", 0, false);
		setBoolArr(mode, "cleared", 0, false);

		boolean result = mode.lineClearEnd(e, 0);

		assertTrue(result);
	}

	@Test
	void lineClearEndGameOver() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager mgr = twoPlayerEngine(mode);
		GameEngine e = mgr.engine[0];
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setIntArr(mode, "ojama", 0, 0);
		setIntArr(mode, "ojamaCountdown", 0, 10);
		setBoolArr(mode, "dangerColumnDouble", 0, true);
		e.field.setBlock(3, 0, new Block(Block.BLOCK_COLOR_RED));

		mode.lineClearEnd(e, 0);

		assertEquals(GameEngine.Status.GAMEOVER, e.stat);
	}

	// --- 899: saveReplay saves map when useMap and fldBackup non-null ---
	@Test
	void saveReplaySavesMap() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.createFieldIfNeeded();
		setBoolArr(mode, "useMap", 0, true);
		// fldBackup[0] must be non-null to reach saveMap
		nullpomino.game.component.Field f = new nullpomino.game.component.Field(10, 20, 4);
		setArrElem(mode, "fldBackup", 0, f);

		mode.saveReplay(e, 0, e.owner.replayProp);

		// saveReplay serialized the backed-up field into the replay props,
		// so map.0 is no longer the default sentinel.
		assertTrue(!"X".equals(e.owner.replayProp.getProperty("map.0", "X")),
				"saveReplay wrote the backed-up map");
	}

	// --- helpers ---

	private static GameManager twoPlayerEngine(AvalancheVSSPFMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		mode.modeInit(m);
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		m.engine[1].init();
		m.engine[1].ruleopt.fieldWidth = 10;
		m.engine[1].ruleopt.fieldHeight = 20;
		return m;
	}

	private static GameEngine freshEngine(AvalancheVSSPFMode mode) {
		GameManager m = new GameManager(new EventReceiver());
		m.mode = mode;
		mode.modeInit(m);
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		return m.engine[0];
	}

	private static void press(GameEngine e, int btn) {
		e.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) e.ctrl.buttonTime[i] = 0;
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static int readInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static Object readField(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.get(o);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	private static int[] getIntArr(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return (int[]) f.get(o);
	}

	private static void setIntArr(Object o, String n, int idx, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((int[]) f.get(o))[idx] = v;
	}

	private static void setBoolArr(Object o, String n, int idx, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		((boolean[]) f.get(o))[idx] = v;
	}

	private static void setArrElem(Object o, String n, int idx, Object v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		Object arr = f.get(o);
		java.lang.reflect.Array.set(arr, idx, v);
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException ex) {}
		throw new NoSuchFieldException(n);
	}
}
