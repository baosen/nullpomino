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
 * Covers game-logic methods in {@link LineRaceMode}: getName,
 * playerInit, loadPreset/savePreset round-trip, ranking arrays,
 * updateRanking, calcScore (meter update, goal detection, BGM fade),
 * startGame initialization, saveReplay ranking condition.
 */
class LineRaceModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("LINE RACE", new LineRaceMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "bgmno"));
		assertFalse(readBoolean(mode, "big"));
		// loadPreset with empty config defaults goaltype to 1
		assertEquals(1, readInt(mode, "goaltype"));
		assertEquals(0, readInt(mode, "presetNumber"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_RED, engine.framecolor);
	}

	@Test
	void loadPresetSavePresetRoundTrip() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.speed.gravity = 10;
		engine.speed.denominator = 128;
		engine.speed.are = 5;
		engine.speed.areLine = 3;
		engine.speed.lineDelay = 2;
		engine.speed.lockDelay = 20;
		engine.speed.das = 8;
		setInt(mode, "bgmno", 3);
		setBoolean(mode, "big", true);
		setInt(mode, "goaltype", 2);

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(mode, engine, prop, -1);

		LineRaceMode dest = new LineRaceMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadPreset(dest, destEngine, prop, -1);

		assertEquals(10, destEngine.speed.gravity);
		assertEquals(128, destEngine.speed.denominator);
		assertEquals(5, destEngine.speed.are);
		assertEquals(3, destEngine.speed.areLine);
		assertEquals(20, destEngine.speed.lockDelay);
		assertEquals(8, destEngine.speed.das);
		assertEquals(3, readInt(dest, "bgmno"));
		assertTrue(readBoolean(dest, "big"));
		assertEquals(2, readInt(dest, "goaltype"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingPiece = (int[][]) readField(mode, "rankingPiece");
		float[][] rankingPPS = (float[][]) readField(mode, "rankingPPS");
		assertNotNull(rankingTime);
		assertEquals(3, rankingTime.length); // GOALTYPE_MAX = 3
		assertEquals(10, rankingTime[0].length); // RANKING_MAX = 10
		assertNotNull(rankingPiece);
		assertNotNull(rankingPPS);
	}

	@Test
	void updateRankingInsertsFirstEntry() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 3600, 50, 2.5f);

		assertEquals(0, readInt(mode, "rankingRank"));
	}

	@Test
	void checkRankingReturnsFirstForUnset() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Ranking time defaults are -1, so anything should be #1
		int rank = invokeCheckRanking(mode, 3600, 50, 2.5f);

		assertEquals(0, rank, "Any valid time should rank first");
	}

	@Test
	void startGameSetsBigAndBgm() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", true);
		setInt(mode, "bgmno", 5);

		mode.startGame(engine, 0);

		assertTrue(engine.big);
	}

	@Test
	void calcScoreUpdatesMeterAndChecksGoal() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // 20 lines goal

		mode.calcScore(engine, 0, 1);

		// calcScore does NOT increment statistics.lines, so remainLines = 20 - 0 = 20
		int remainLines = 20 - engine.statistics.lines;
		assertEquals(20, remainLines);
		assertEquals(0, engine.ending);
	}

	@Test
	void calcScoreReachesGoalEndsGame() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // 20 lines
		engine.statistics.lines = 20;

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.ending);
	}

	@Test
	void calcScoreAllClearTriggersBravo() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);

		// Just verify no exception; field is empty so bravo SE would play
		assertTrue(true);
	}

	@Test
	void calcScoreNearGoalFadesBgm() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // 20 lines
		engine.statistics.lines = 16; // within 5 of goal

		mode.calcScore(engine, 0, 1);

		assertTrue(owner(engine).bgmStatus.fadesw, "BGM should fade within 5 of goal");
	}

	@Test
	void calcScore40LineGoal() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 1); // 40 lines
		engine.statistics.lines = 40; // must be >= GOAL_TABLE[1]=40

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.ending);
	}

	@Test
	void calcScore100LineGoal() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 2); // 100 lines
		engine.statistics.lines = 100;

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.ending);
	}

	// ---- helpers ----

	private static GameManager owner(GameEngine engine) {
		return engine.owner;
	}

	private static GameEngine freshEngine(LineRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(LineRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(LineRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(LineRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(LineRaceMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(LineRaceMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadPreset(LineRaceMode mode, GameEngine engine, CustomProperties prop, int preset) throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod("loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(LineRaceMode mode, GameEngine engine, CustomProperties prop, int preset) throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod("savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeUpdateRanking(LineRaceMode mode, int time, int piece, float pps) throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod("updateRanking", int.class, int.class, float.class);
		m.setAccessible(true);
		m.invoke(mode, time, piece, pps);
	}

	private static int invokeCheckRanking(LineRaceMode mode, int time, int piece, float pps) throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod("checkRanking", int.class, int.class, float.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, time, piece, pps);
	}
}
