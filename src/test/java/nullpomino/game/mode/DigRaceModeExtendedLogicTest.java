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
 * Covers additional branches in {@link DigRaceMode}: fillGarbage with
 * sticky skin, getRemainGarbageLines with null engine/field, meter color
 * thresholds in calcScore, onReady netIsWatch path, startGame version>0
 * path, saveReplay conditions, and netIsNetRankingViewOK/SendOK.
 */
class DigRaceModeExtendedLogicTest {

	// -----------------------------------------------------------------------
	// fillGarbage with sticky skin
	// -----------------------------------------------------------------------

	@Test
	void fillGarbageSetsConnectionsForStickySkin() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		// Set sticky skin receiver BEFORE playerInit so mode.receiver picks it up
		engine.owner.receiver = new EventReceiver() {
			@Override public boolean isStickySkin(GameEngine engine) { return true; }
		};
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		setInt(mode, "goaltype", 2); // 18 garbage lines
		invokeFillGarbage(mode, engine, 2);

		// Verify some blocks have connection attributes set
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		boolean hasConnection = false;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				Block b = engine.field.getBlock(x, y);
				if (b != null && (b.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT) ||
						b.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT))) {
					hasConnection = true;
					break;
				}
			}
		}
		assertTrue(hasConnection, "Sticky skin should set connection attributes");
	}

	// -----------------------------------------------------------------------
	// getRemainGarbageLines null engine/field
	// -----------------------------------------------------------------------

	@Test
	void getRemainGarbageLinesReturnsMinusOneForNullEngine() throws Exception {
		DigRaceMode mode = new DigRaceMode();

		int result = invokeGetRemainGarbageLines(mode, null, 0);

		assertEquals(-1, result);
	}

	@Test
	void getRemainGarbageLinesReturnsMinusOneForNullField() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);

		// field is null before createFieldIfNeeded
		int result = invokeGetRemainGarbageLines(mode, engine, 0);

		assertEquals(-1, result);
	}

	// -----------------------------------------------------------------------
	// calcScore meter color thresholds
	// -----------------------------------------------------------------------

	@Test
	void calcScoreMeterColorThresholds() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // 5 garbage lines

		// With no garbage blocks at all, remainLines = 0 (no gem blocks) → red (0 <= 4)
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);

		// Add garbage blocks with a gem at bottom row (needed for hasGemBlock check)
		int h = engine.field.getHeight();
		int w = engine.field.getWidth();
		// Add gem at bottom row
		engine.field.setBlock(0, h - 1,
				new Block(Block.BLOCK_COLOR_GEM_RED, engine.getSkin(),
						Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		// Add garbage blocks above
		for (int y = h - 2; y >= h - 8 && y >= 0; y--) {
			engine.field.setBlock(0, y,
					new Block(Block.BLOCK_COLOR_GRAY, engine.getSkin(),
							Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		}

		mode.calcScore(engine, 0, 0);
		// remainLines should be 5 (goaltype=0 → GOAL_TABLE=5, rows h-1 to h-5 have garbage) → <= 14 and <= 8, > 4, so ORANGE
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void calcScoreMeterColorRedWhen4OrLess() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 1); // 10 garbage lines

		int h = engine.field.getHeight();
		int w = engine.field.getWidth();
		// Add gem at bottom row
		engine.field.setBlock(0, h - 1,
				new Block(Block.BLOCK_COLOR_GEM_RED, engine.getSkin(),
						Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		// Add 3 garbage blocks above
		for (int y = h - 2; y >= h - 4 && y >= 0; y--) {
			engine.field.setBlock(0, y,
					new Block(Block.BLOCK_COLOR_GRAY, engine.getSkin(),
							Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
		}

		mode.calcScore(engine, 0, 0);

		// remainLines = 3 → RED
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	// -----------------------------------------------------------------------
	// onReady netIsWatch path
	// -----------------------------------------------------------------------

	@Test
	void onReadySkipsFieldCreationWhenNetIsWatch() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetPlay", true);
		setBoolean(mode, "netIsWatch", true);

		mode.onReady(engine, 0);

		// Field should not be created in watch mode
		assertEquals(null, engine.field);
	}

	// -----------------------------------------------------------------------
	// startGame version>0 path (big not set)
	// -----------------------------------------------------------------------

	@Test
	void startGameVersionAboveZeroDoesNotSetBig() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1);
		setBoolean(mode, "big", true);

		mode.startGame(engine, 0);

		// version > 0 → big should NOT be set from mode
		assertFalse(engine.big);
	}

	// -----------------------------------------------------------------------
	// saveReplay with ranking
	// -----------------------------------------------------------------------

	@Test
	void saveReplayUpdatesRankingWhenGameCompleted() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		setBoolean(mode, "netIsWatch", false);
		engine.ending = 1;
		engine.statistics.time = 3600;
		engine.statistics.lines = 10;
		engine.statistics.totalPieceLocked = 30;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // 5 garbage lines

		// Field is empty → remainLines = 0
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(0, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// netIsNetRankingViewOK / SendOK
	// -----------------------------------------------------------------------

	@Test
	void netIsNetRankingViewOKRequiresNoAI() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ai = null;
		assertTrue(mode.netIsNetRankingViewOK(engine));

		engine.ai = new nullpomino.game.ai.DummyAI();
		assertFalse(mode.netIsNetRankingViewOK(engine));
	}

	@Test
	void netIsNetRankingSendOKRequiresComplete() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ai = null;
		engine.ending = 1;

		// Empty field → remainLines = 0
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0);

		assertTrue(mode.netIsNetRankingSendOK(engine));
	}

	// -----------------------------------------------------------------------
	// playerInit replay branch version read
	// -----------------------------------------------------------------------

	@Test
	void playerInitReplayBranchSetsVersionFromProp() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = true;
		engine.owner.replayProp.setProperty("digrace.version", 5);

		mode.playerInit(engine, 0);

		assertEquals(5, readInt(mode, "version"));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(DigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(DigRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(DigRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(DigRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(DigRaceMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(DigRaceMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeFillGarbage(DigRaceMode mode, GameEngine engine, int height) throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod("fillGarbage", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, height);
	}

	private static int invokeGetRemainGarbageLines(DigRaceMode mode, GameEngine engine, int height) throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod("getRemainGarbageLines", GameEngine.class, int.class);
		m.setAccessible(true);
		Object result = m.invoke(mode, engine, height);
		return (result != null) ? (int) result : -1;
	}
}
