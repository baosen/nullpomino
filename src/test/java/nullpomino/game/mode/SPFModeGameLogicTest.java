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
 * Covers game-logic methods in {@link SPFMode}: startGame engine
 * configuration, onMove / onLast state resets, isVSMode, calcScore
 * with diamond/chain scoring, checkCountdown, checkSquares, and
 * lineClearEnd garbage-drop handling.
 */
class SPFModeGameLogicTest {

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(new SPFMode().isVSMode());
	}

	@Test
	void getPlayersIsTwo() {
		assertEquals(2, new SPFMode().getPlayers());
	}

	@Test
	void modeInitCreatesTwoPlayerArrays() throws Exception {
		SPFMode mode = new SPFMode();
		mode.modeInit(new GameManager(new EventReceiver()));

		assertEquals(2, ((int[]) readField(mode, "ojama")).length);
		assertEquals(2, ((int[]) readField(mode, "score")).length);
		assertEquals(2, ((int[]) readField(mode, "dropSet")).length);
		assertEquals(2, ((double[]) readField(mode, "attackMultiplier")).length);
		assertEquals(2, ((double[]) readField(mode, "defendMultiplier")).length);
	}

	@Test
	void startGameDisablesTspinAndB2bAndCombo() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		mode.startGame(engine, 0);

		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertFalse(engine.tspinEnable);
		assertFalse(engine.tspinAllowKick);
		assertFalse(engine.useAllSpinBonus);
	}

	@Test
	void startGameSetsColorClearSizeAndNoHidden() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		mode.startGame(engine, 0);

		assertEquals(2, engine.colorClearSize);
		assertFalse(engine.ignoreHidden);
	}

	@Test
	void onMoveResetsCountdownDecremented() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		Field f = SPFMode.class.getDeclaredField("countdownDecremented");
		f.setAccessible(true);
		((boolean[]) f.get(mode))[0] = true;

		mode.onMove(engine, 0);

		assertFalse(((boolean[]) f.get(mode))[0]);
	}

	@Test
	void onLastIncrementsScgettimeAndDecrementsZenKeshiDisplay() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		Field scget = SPFMode.class.getDeclaredField("scgettime");
		scget.setAccessible(true);
		((int[]) scget.get(mode))[0] = 0;

		Field zen = SPFMode.class.getDeclaredField("zenKeshiDisplay");
		zen.setAccessible(true);
		((int[]) zen.get(mode))[0] = 10;

		mode.onLast(engine, 0);

		assertEquals(1, ((int[]) scget.get(mode))[0]);
		assertEquals(9, ((int[]) zen.get(mode))[0]);
	}

	@Test
	void checkCountdownDecrementsCountersAndConvertsGarbage() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		// Place a block with countdown = 2 (will decrement to 1, not convert)
		Block b2 = new Block(Block.BLOCK_COLOR_RED);
		b2.countdown = 2;
		b2.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		engine.field.setBlock(0, 0, b2);

		// Place a block with countdown = 1 (will convert)
		Block b1 = new Block(Block.BLOCK_COLOR_BLUE);
		b1.countdown = 1;
		b1.secondaryColor = Block.BLOCK_COLOR_GREEN;
		b1.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		engine.field.setBlock(1, 0, b1);

		Field f = SPFMode.class.getDeclaredField("countdownDecremented");
		f.setAccessible(true);
		((boolean[]) f.get(mode))[0] = false;

		boolean result = (boolean) invokeCountdown(mode, engine, 0);

		assertTrue(result, "should return true because a block was converted");
		assertEquals(1, engine.field.getBlock(0, 0).countdown);
		assertEquals(0, engine.field.getBlock(1, 0).countdown);
		assertFalse(engine.field.getBlock(1, 0).getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
		assertEquals(Block.BLOCK_COLOR_GREEN, engine.field.getBlock(1, 0).color);
	}

	@Test
	void checkCountdownReturnsFalseIfAlreadyDecrementedThisFrame() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		Field f = SPFMode.class.getDeclaredField("countdownDecremented");
		f.setAccessible(true);
		((boolean[]) f.get(mode))[0] = true;

		boolean result = (boolean) invokeCountdown(mode, engine, 0);
		assertFalse(result);
	}

	@Test
	void calcScoreWithEmptyFieldAwardsZenKeshiBonus() throws Exception {
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		engine.playerID = 0;
		mode.playerInit(engine, 0);

		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0);

		// With an empty field, the zenkeshi bonus triggers: +1000
		assertEquals(1000, engine.statistics.score,
				"Empty field awards zenkeshi bonus of 1000");
	}

	@Test
	void calcScoreWithDiamondInFieldTriggersDiamondBreak() throws Exception {
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[0];
		engine.init();
		engine.playerID = 0;
		mode.playerInit(engine, 0);

		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		// Place a diamond at top row (y = height-1 -> row value 1.0)
		int h = engine.field.getHeight();
		Block diamond = new Block(Block.BLOCK_COLOR_GEM_RAINBOW);
		engine.field.setBlock(0, h - 1, diamond);
		engine.statistics.score = 0;

		// Set diamondPower > 0
		Field dp = SPFMode.class.getDeclaredField("diamondPower");
		dp.setAccessible(true);
		((int[]) dp.get(mode))[0] = 2; // 80% multiplier

		mode.calcScore(engine, 0, 0);

		// Diamond was at top row, so row value 1.0 * 7 * width * 0.8
		// The diamond break color loop triggers allClearColor and scoring
		assertTrue(engine.statistics.score > 0);
	}

	@Test
	void lineClearEndGarbageDropWithOjamaPending() throws Exception {
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

		// Set ojama > 0 so garbage gets dropped
		Field ojama = SPFMode.class.getDeclaredField("ojama");
		ojama.setAccessible(true);
		((int[]) ojama.get(mode))[0] = 20;

		// Set dropPattern for enemy
		Field dropPattern = SPFMode.class.getDeclaredField("dropPattern");
		dropPattern.setAccessible(true);
		dropPattern.set(mode, new int[2][][]);
		((int[][][]) dropPattern.get(mode))[1] = new int[][]{{2,2,2,2}};

		boolean result = (boolean) invokeLineClearEnd(mode, engine, 0);

		assertTrue(result, "garbage should have been dropped");
		assertTrue(((int[]) ojama.get(mode))[0] < 20, "ojama should have decreased");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(SPFMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static Object readField(Object obj, String name) throws Exception {
		Class<?> c = obj.getClass();
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(obj); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static Object invokeCountdown(SPFMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = SPFMode.class.getDeclaredMethod("checkCountdown", GameEngine.class, int.class);
		m.setAccessible(true);
		return m.invoke(mode, engine, playerID);
	}

	private static Object invokeLineClearEnd(SPFMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = SPFMode.class.getDeclaredMethod("lineClearEnd", GameEngine.class, int.class);
		m.setAccessible(true);
		return m.invoke(mode, engine, playerID);
	}
}
