package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Covers additional branches in {@link VSDigRaceMode}: fillGarbage with
 * variable hole change rate, getRemainGarbageLines null engine/field,
 * calcScore meter-color thresholds and game completion with remaining
 * lines zero, onLast 1P-win detection, onReady field creation, startGame
 * BGM for player 1, and saveReplay.
 */
class VSDigRaceModeExtendedLogicTest {

	// -----------------------------------------------------------------------
	// fillGarbage with variable hole change rate
	// -----------------------------------------------------------------------

	@Test
	void fillGarbageWithZeroPercentChangeRate() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		((int[]) readField(mode, "goalLines"))[0] = 3;
		((int[]) readField(mode, "garbagePercent"))[0] = 0; // never change hole

		invokeFillGarbage(mode, engine, 0);

		// Should create garbage lines with few holes (same hole reused)
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		boolean hasBlocks = false;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (engine.field.getBlock(x, y) != null) {
					hasBlocks = true;
					break;
				}
			}
		}
		assertTrue(hasBlocks, "Field should have blocks after fillGarbage");
	}

	// -----------------------------------------------------------------------
	// getRemainGarbageLines null engine/field
	// -----------------------------------------------------------------------

	@Test
	void getRemainGarbageLinesReturnsMinus1ForNullEngine() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();

		int result = invokeGetRemainGarbageLines(mode, null, 0);
		assertEquals(-1, result);
	}

	@Test
	void getRemainGarbageLinesReturnsMinus1ForNullField() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		int result = invokeGetRemainGarbageLines(mode, engine, 0);
		assertEquals(-1, result);
	}

	// -----------------------------------------------------------------------
	// calcScore meter-color thresholds
	// -----------------------------------------------------------------------

	@Test
	void calcScoreMeterColorYellowWhen14OrLess() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		// getRemainGarbageLines needs gem blocks at bottom row and garbage blocks
		int h = engine.field.getHeight();
		int w = engine.field.getWidth();
		// Add a gem block at bottom row (needed for hasGemBlock check)
		engine.field.setBlock(0, h - 1,
				new Block(Block.BLOCK_COLOR_GEM_RED, engine.getSkin(),
						Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		// Add garbage blocks across rows (not bottom row to avoid replacing gem)
		for (int y = h - 2; y >= h - 12 && y >= 0; y--) {
			engine.field.setBlock(0, y,
					new Block(Block.BLOCK_COLOR_GRAY, engine.getSkin(),
							Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		}

		mode.calcScore(engine, 0, 0);
		// With enough garbage blocks, remainLines should be > 8 and <= 14 → YELLOW
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void calcScoreMeterColorRedWhen4OrLess() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		int w = engine.field.getWidth();
		// Add a gem block at bottom row
		engine.field.setBlock(0, h - 1,
				new Block(Block.BLOCK_COLOR_GEM_RED, engine.getSkin(),
						Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		// Add 3 garbage blocks
		for (int y = h - 2; y >= h - 4 && y >= 0; y--) {
			engine.field.setBlock(0, y,
					new Block(Block.BLOCK_COLOR_GRAY, engine.getSkin(),
							Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		}

		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	// -----------------------------------------------------------------------
	// calcScore game completion
	// -----------------------------------------------------------------------

	@Test
	void calcScoreWithNoLinesDoesNotTriggerWin() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.timerActive = true;

		// Empty field with no lines cleared → no win
		mode.calcScore(engine, 0, 0);

		assertTrue(engine.timerActive);
	}

	// -----------------------------------------------------------------------
	// onLast 1P-win detection
	// -----------------------------------------------------------------------

	@Test
	void onLastDetectsPlayer1Win() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		// Set up: P2 is game over, P1 is not → P1 wins
		engine0.stat = GameEngine.Status.MOVE;
		engine1.stat = GameEngine.Status.GAMEOVER;
		engine0.gameActive = true;

		((int[]) readField(mode, "winCount"))[0] = 0;
		((int[]) readField(mode, "winCount"))[1] = 0;

		mode.onLast(engine1, 1);

		assertEquals(0, readInt(mode, "winnerID"));
		assertEquals(1, ((int[]) readField(mode, "winCount"))[0]);
	}

	// -----------------------------------------------------------------------
	// onReady field creation
	// -----------------------------------------------------------------------

	@Test
	void onReadyCreatesField() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		((int[]) readField(mode, "goalLines"))[0] = 5;
		mode.onReady(engine, 0);

		assertNotNull(engine.field);
	}

	// -----------------------------------------------------------------------
	// startGame BGM for player 1
	// -----------------------------------------------------------------------

	@Test
	void startGamePlayer1SetsBgm() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "bgmno", 3);

		mode.startGame(engine, 1);

		assertEquals(3, engine.owner.bgmStatus.bgm);
	}

	// -----------------------------------------------------------------------
	// saveReplay persists version
	// -----------------------------------------------------------------------

	@Test
	void saveReplayPersistsVersion() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.owner.replayProp = new CustomProperties();
		mode.saveReplay(engine, 0, engine.owner.replayProp);

		assertEquals(0, engine.owner.replayProp.getProperty("vsdigrace.version", -1));
	}

	// -----------------------------------------------------------------------
	// playerInit replay branch
	// -----------------------------------------------------------------------

	@Test
	void playerInitReplayBranchLoadsFromReplayProp() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.owner.replayMode = true;
		engine.owner.replayProp.setProperty("vsdigrace.goalLines.p0", 10);

		mode.playerInit(engine, 0);

		assertEquals(10, ((int[]) readField(mode, "goalLines"))[0]);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(VSDigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeFillGarbage(VSDigRaceMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod("fillGarbage", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}

	private static int invokeGetRemainGarbageLines(VSDigRaceMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod("getRemainGarbageLines", GameEngine.class, int.class);
		m.setAccessible(true);
		Object result = m.invoke(mode, engine, playerID);
		return (result != null) ? (int) result : -1;
	}
}
