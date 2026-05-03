package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link PhysicianVSMode}: playerInit,
 * modeInit, startGame, calcScore (gem clearing formula),
 * lineClearEnd (garbage collection), onLast (settlement, meter),
 * loadSetting/saveSetting, saveReplay, and mode-specific helpers.
 */
class PhysicianVSModeGameLogicTest {

	@Test
	void getNameReturnsExpected() {
		assertEquals("PHYSICIAN VS-BATTLE (RC1)", new PhysicianVSMode().getName());
	}

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(new PhysicianVSMode().isVSMode());
	}

	@Test
	void getPlayersReturns2() {
		assertEquals(2, new PhysicianVSMode().getPlayers());
	}

	@Test
	void modeInitCreatesAllArrays() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		int[] score = (int[]) readFieldByClass(mode, PhysicianVSMode.class, "score");
		int[] hoverBlocks = (int[]) readFieldByClass(mode, PhysicianVSMode.class, "hoverBlocks");
		int[] speed = (int[]) readFieldByClass(mode, PhysicianVSMode.class, "speed");
		int[] rest = (int[]) readFieldByClass(mode, PhysicianVSMode.class, "rest");
		boolean[] flash = (boolean[]) readFieldByClass(mode, PhysicianVSMode.class, "flash");

		assertEquals(2, score.length);
		assertEquals(2, hoverBlocks.length);
		assertEquals(2, speed.length);
		assertEquals(2, rest.length);
		assertEquals(2, flash.length);
	}

	@Test
	void playerInitSetsEngineDefaults() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(GameEngine.ClearType.LINE_COLOR, engine.clearMode);
		assertFalse(engine.garbageColorClear);
		assertEquals(4, engine.colorClearSize);
		assertTrue(engine.randomBlockColor);
		assertTrue(engine.connectBlocks);
		assertEquals(0, readIntArray(mode, "score", 0));
	}

	@Test
	void startGameDisablesB2bAndCombo() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertFalse(engine.tspinEnable);
		assertFalse(engine.useAllSpinBonus);
	}

	@Test
	void calcScoreWithGemsAndLinesAccumulatesScore() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		// Place blocks that can be cleared as gems
		// A horizontal line of same-color blocks
		int h = engine.field.getHeight() - 1;
		for (int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlock(x, h, new Block(Block.BLOCK_COLOR_RED));
		}
		engine.field.setLineFlag(h, true);
		engine.field.gemsCleared = 3; // Simulate 3 gems cleared
		setIntArray(mode, "speed", 1, 0); // Speed MED -> multiplier (1+1)*100 = 200

		mode.calcScore(engine, 0, 1); // lines=1

		// Formula: pts = 0 initially (gem 0: 1<<0=1, gem 1: 1<<1=2, gem 2: 1<<2=4) = 7
		// gemsClearedChainTotal = 3
		// pts *= (speed+1)*100 = 7 * 200 = 1400
		// But it's complex due to the while loop over gemsCleared
		// For 3 gems: pts = (1<<0)+(1<<1)+(1<<2) = 1+2+4 = 7
		//   then * (1+1)*100 = 200 => 1400
		assertEquals(1400, readIntArray(mode, "score", 0));
	}

	@Test
	void calcScoreMoreThan5GemsUsesShiftFormula() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		int h = engine.field.getHeight() - 1;
		for (int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlock(x, h, new Block(Block.BLOCK_COLOR_RED));
		}
		engine.field.setLineFlag(h, true);
		engine.field.gemsCleared = 7; // 7 gems cleared
		setIntArray(mode, "speed", 1, 0);

		mode.calcScore(engine, 0, 1);

		// gems 0-4: 1+2+4+8+16=31
		// gems 5-6: 2 << 5 = 64
		// Total: 31+64=95 * 200 = 19000
		assertEquals(19000, readIntArray(mode, "score", 0));
	}

	@Test
	void calcScoreWithNoLinesDoesNothing() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.field.gemsCleared = 3;

		mode.calcScore(engine, 0, 0); // lines=0, no scoring

		assertEquals(0, readIntArray(mode, "score", 0));
	}

	@Test
	void lineClearEndTransfersClearedColorsToEnemyGarbage() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine1 = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine1, 0);
		mode.playerInit(engine2, 1);
		engine1.createFieldIfNeeded();
		engine1.nowPieceObject = new Piece(Piece.PIECE_T);

		// Simulate clearing colors
		java.util.ArrayList<Integer> cleared = new java.util.ArrayList<Integer>();
		cleared.add(Block.BLOCK_COLOR_RED);
		cleared.add(Block.BLOCK_COLOR_BLUE);
		engine1.field.lineColorsCleared = cleared;

		mode.lineClearEnd(engine1, 0);

		// P1's cleared colors should be transferred to P2's garbageColors
		assertTrue(readFieldArray(mode, "garbageColors", 1) != null);
	}

	@Test
	void onLastUpdatesRestAndSettlement() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine1 = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine1, 0);
		mode.playerInit(engine2, 1);
		engine1.createFieldIfNeeded();
		engine2.createFieldIfNeeded();

		// Place gems in P1's field
		for (int x = 0; x < engine1.field.getWidth(); x++) {
			engine1.field.setBlock(x, engine1.field.getHeight() - 1,
					new Block(Block.BLOCK_COLOR_GEM_RED));
		}
		engine2.stat = GameEngine.Status.GAMEOVER; // P2 dies

		mode.onLast(engine2, 1); // P2's onLast triggers settlement

		// P2's field has no gems -> p1Lose=true, P2 is GAMEOVER -> p2Lose=true
		// Both lose -> draw
		assertEquals(-1, readInt(mode, "winnerID"));
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "scgettime", 0, 0);

		mode.onLast(engine, 0);

		assertEquals(1, readIntArray(mode, "scgettime", 0));
	}

	@Test
	void loadSaveSettingRoundTrip() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		setIntArray(mode, "hoverBlocks", 50, 0);
		setIntArray(mode, "speed", 2, 0);
		setBooleanArray(mode, "flash", true, 0);

		invokeSaveOtherSetting(mode, engine, prop);
		invokeLoadOtherSetting(mode, engine, prop);

		assertEquals(50, readIntArray(mode, "hoverBlocks", 0));
		assertEquals(2, readIntArray(mode, "speed", 0));
		assertTrue(readBooleanArray(mode, "flash", 0));
	}

	@Test
	void saveReplayStoresVersion() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(0, prop.getProperty("physicianvs.version", -1));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(PhysicianVSMode mode) {
		return freshEngine(mode, 0);
	}

	private static GameEngine freshEngine(PhysicianVSMode mode, int playerID) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		for (int i = 0; i <= playerID; i++) {
			manager.engine[i].init();
		}
		return manager.engine[playerID];
	}

	private static int readInt(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static int readIntArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((int[]) f.get(mode))[index];
	}

	private static boolean readBooleanArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((boolean[]) f.get(mode))[index];
	}

	private static Object readFieldArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		Object[] arr = (Object[]) f.get(mode);
		return arr[index];
	}

	private static Object readFieldByClass(Object mode, Class<?> cls, String name) throws Exception {
		Field f = cls.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setIntArray(Object mode, String name, int value, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		((int[]) f.get(mode))[index] = value;
	}

	private static void setBooleanArray(Object mode, String name, boolean value, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		((boolean[]) f.get(mode))[index] = value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeSaveOtherSetting(PhysicianVSMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = PhysicianVSMode.class.getDeclaredMethod("saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeLoadOtherSetting(PhysicianVSMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = PhysicianVSMode.class.getDeclaredMethod("loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}
}
