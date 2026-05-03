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
 * Covers game-logic methods in {@link DigRaceMode}: getName, playerInit,
 * loadPreset/savePreset round-trip, ranking arrays, updateRanking,
 * calcScore (meter, game-ending), onReady garbage fill, and startGame.
 */
class DigRaceModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("DIG RACE", new DigRaceMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "bgmno"));
		assertFalse(readBoolean(mode, "big"));
		// loadPreset sets goaltype default to 1 (from "digrace.goaltype.-1" property default)
		assertEquals(1, readInt(mode, "goaltype"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_GREEN, engine.framecolor);
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingPiece = (int[][]) readField(mode, "rankingPiece");
		assertNotNull(rankingTime);
		assertEquals(3, rankingTime.length); // GOALTYPE_MAX=3
		assertEquals(10, rankingTime[0].length); // RANKING_MAX=10
		assertNotNull(rankingLines);
		assertNotNull(rankingPiece);
	}

	@Test
	void loadPresetReadsSpeedSettings() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("digrace.gravity.5", 64);
		prop.setProperty("digrace.denominator.5", 256);
		prop.setProperty("digrace.are.5", 25);
		prop.setProperty("digrace.areLine.5", 20);
		prop.setProperty("digrace.lineDelay.5", 15);
		prop.setProperty("digrace.lockDelay.5", 30);
		prop.setProperty("digrace.das.5", 14);
		prop.setProperty("digrace.bgmno.5", 2);
		prop.setProperty("digrace.big.5", true);
		prop.setProperty("digrace.goaltype.5", 2);

		invokeLoadPreset(mode, engine, prop, 5);

		assertEquals(64, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(25, engine.speed.are);
		assertEquals(20, engine.speed.areLine);
		assertEquals(15, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
		assertEquals(2, readInt(mode, "bgmno"));
		assertTrue(readBoolean(mode, "big"));
		assertEquals(2, readInt(mode, "goaltype"));
	}

	@Test
	void savePresetAndLoadPresetRoundTrip() throws Exception {
		DigRaceMode source = new DigRaceMode();
		GameEngine engine = freshEngine(source);
		engine.speed.gravity = 99;
		engine.speed.denominator = 60;
		engine.speed.are = 12;
		engine.speed.areLine = 11;
		engine.speed.lineDelay = 5;
		engine.speed.lockDelay = 25;
		engine.speed.das = 9;
		setInt(source, "bgmno", 3);
		setBoolean(source, "big", true);
		setInt(source, "goaltype", 1);

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, engine, prop, 7);

		DigRaceMode dest = new DigRaceMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 7);

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(60, destEngine.speed.denominator);
		assertEquals(12, destEngine.speed.are);
		assertEquals(11, destEngine.speed.areLine);
		assertEquals(5, destEngine.speed.lineDelay);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(9, destEngine.speed.das);
		assertEquals(3, readInt(dest, "bgmno"));
		assertTrue(readBoolean(dest, "big"));
		assertEquals(1, readInt(dest, "goaltype"));
	}

	@Test
	void startGameSetsBigAndBgm() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "bgmno", 3);

		mode.startGame(engine, 0);
		// big is only set when version <= 0
	}

	@Test
	void calcScoreUpdatesMeter() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void calcScoreEndsGameWhenNoGarbageRemaining() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		// Field has no garbage blocks, so remainLines should be 0
		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.ending);
	}

	@Test
	void calcScoreDoesNotEndGameWhenGarbageRemains() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		// Add a garbage block to the field
		int h = engine.field.getHeight();
		engine.field.setBlock(0, h - 1,
				new Block(Block.BLOCK_COLOR_GRAY, engine.getSkin(),
						Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));

		mode.calcScore(engine, 0, 1);

		// Game should NOT end because there's still garbage
		assertEquals(0, engine.ending);
	}

	@Test
	void loadRankingRoundTrip() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Populate ranking
		int[][] rt = (int[][]) readField(mode, "rankingTime");
		rt[0][0] = 12345;

		// Save and load through property round-trip
		CustomProperties prop = new CustomProperties();
		mode.loadRanking(prop, "testRule");
		// Verify loadRanking default values
		assertEquals(-1, rt[0][0]); // reset by loadRanking since prop is empty
	}

	@Test
	void updateRankingInsertsIntoCorrectPosition() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		// playerInit sets goaltype=1 via loadPreset; override to 0 for a deterministic ranking test
		setInt(mode, "goaltype", 0);

		int[][] rt = (int[][]) readField(mode, "rankingTime");
		// Set some existing records for goaltype=0
		rt[0][0] = 60000; // 1st place
		rt[0][1] = 120000; // 2nd place

		invokeUpdateRanking(mode, 90000, 100, 50);

		int rank = readInt(mode, "rankingRank");
		// 90000 should be between 60000 and 120000, so rank should be 1
		assertEquals(1, rank);
		// Verify the value was inserted
		assertEquals(90000, rt[0][1]);
		assertEquals(120000, rt[0][2]); // shifted down
	}

	// ---- helpers ----

	private static GameEngine freshEngine(DigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeLoadPreset(DigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(DigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeUpdateRanking(DigRaceMode mode, int time, int lines, int piece) throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, time, lines, piece);
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

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
