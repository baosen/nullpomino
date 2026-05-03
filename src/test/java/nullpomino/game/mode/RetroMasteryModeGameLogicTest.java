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
 * Covers game-logic methods in {@link RetroMasteryMode}: getName,
 * playerInit, loadSetting/saveSetting round-trip, ranking arrays,
 * updateRanking, calcScore (line clear scoring, drop scoring,
 * level-up, 200-line ending), onLast (scgettime increment),
 * startGame (game type setup), setSpeed, afterSoftDropFall,
 * afterHardDropFall.
 */
class RetroMasteryModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("RETRO MASTERY", new RetroMasteryMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "softdropscore"));
		assertEquals(0, readInt(mode, "harddropscore"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
		assertFalse(engine.tspinEnable);
		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 1);
		setInt(mode, "startlevel", 5);
		setBoolean(mode, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		RetroMasteryMode dest = new RetroMasteryMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);

		assertEquals(1, readInt(dest, "gametype"));
		assertEquals(5, readInt(dest, "startlevel"));
		assertTrue(readBoolean(dest, "big"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingLevel = (int[][]) readField(mode, "rankingLevel");
		assertNotNull(rankingScore);
		assertEquals(3, rankingScore.length); // RANKING_TYPE = 3
		assertEquals(10, rankingScore[0].length); // RANKING_MAX = 10
		assertNotNull(rankingLines);
		assertNotNull(rankingLevel);
	}

	@Test
	void updateRankingInsertsFirstEntry() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 50000, 200, 19, 0);

		assertEquals(0, readInt(mode, "rankingRank"));
	}

	@Test
	void startGame200Mode() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0); // 200 mode
		setInt(mode, "startlevel", 5);

		mode.startGame(engine, 0);

		assertEquals(5, engine.statistics.level);
		assertEquals(60, readInt(mode, "levellines")); // 10 * (5+1)
	}

	@Test
	void startGameEndlessMode() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 1);
		setInt(mode, "startlevel", 5);

		mode.startGame(engine, 0);

		assertEquals(5, engine.statistics.level);
		// startlevel <= 9: (startlevel+1)*10 = 60
		assertEquals(60, readInt(mode, "levellines"));
	}

	@Test
	void startGameEndlessModeHighLevel() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 1);
		setInt(mode, "startlevel", 12);

		mode.startGame(engine, 0);

		assertEquals(12, engine.statistics.level);
		// startlevel > 9: (startlevel+11)*5 = 23*5 = 115
		assertEquals(115, readInt(mode, "levellines"));
	}

	@Test
	void startGamePressureMode() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 2); // PRESSURE mode
		setInt(mode, "startlevel", 5);

		mode.startGame(engine, 0);

		assertEquals(0, engine.statistics.level);
		assertEquals(5, readInt(mode, "levellines"));
	}

	@Test
	void calcScoreSingleLine() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// Single: 40 * (level+1) = 40
		assertEquals(40, readInt(mode, "lastscore"));
		assertEquals(1, readInt(mode, "loons"));
		assertEquals(1, readInt(mode, "actions"));
	}

	@Test
	void calcScoreDoubleLine() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		assertEquals(100, readInt(mode, "lastscore"));
		assertEquals(2, readInt(mode, "loons"));
	}

	@Test
	void calcScoreTripleLine() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 3);

		assertEquals(200, readInt(mode, "lastscore"));
		assertEquals(3, readInt(mode, "loons"));
	}

	@Test
	void calcScoreFourLines() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 4);

		// Four: 300 * (level+1) = 300, loons += 3
		assertEquals(300, readInt(mode, "lastscore"));
		assertEquals(3, readInt(mode, "loons"));
	}

	@Test
	void calcScore200ModeEnding() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "gametype", 0); // 200 mode
		setInt(mode, "loons", 199);

		mode.calcScore(engine, 0, 1);

		assertEquals(200, readInt(mode, "loons"));
		assertEquals(1, engine.ending);
	}

	@Test
	void calcScoreLevelUp() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "levellines", 15);
		setInt(mode, "loons", 15);
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.statistics.level);
		assertEquals(25, readInt(mode, "levellines")); // +10 for non-pressure
	}

	@Test
	void calcScoreSoftAndHardDropApplied() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "softdropscore", 20);
		setInt(mode, "harddropscore", 10);

		mode.calcScore(engine, 0, 0);

		// softdropscore/2 = 10, harddropscore/2 = 5 added to score
		assertEquals(0, readInt(mode, "softdropscore"));
		assertEquals(0, readInt(mode, "harddropscore"));
		assertEquals(15, engine.statistics.score);
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	@Test
	void afterSoftDropFallAccumulates() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);

		mode.afterSoftDropFall(engine, 0, 5);

		assertEquals(5, readInt(mode, "softdropscore"));
	}

	@Test
	void afterHardDropFallAccumulates() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);

		mode.afterHardDropFall(engine, 0, 7);

		assertEquals(7, readInt(mode, "harddropscore"));
	}

	@Test
	void setSpeedForLevel0() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(48, engine.speed.denominator);
		assertEquals(60, engine.speed.lockDelay);
		assertEquals(25, engine.speed.lineDelay);
	}

	@Test
	void setSpeedForHighLevel() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 30;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(1, engine.speed.denominator);
		assertEquals(6, engine.speed.lockDelay);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(RetroMasteryMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(RetroMasteryMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(RetroMasteryMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(RetroMasteryMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(RetroMasteryMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(RetroMasteryMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadSetting(RetroMasteryMode mode, CustomProperties prop) throws Exception {
		Method m = RetroMasteryMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(RetroMasteryMode mode, CustomProperties prop) throws Exception {
		Method m = RetroMasteryMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(RetroMasteryMode mode, int sc, int li, int lv, int type) throws Exception {
		Method m = RetroMasteryMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, li, lv, type);
	}

	private static void invokeSetSpeed(RetroMasteryMode mode, GameEngine engine) throws Exception {
		Method m = RetroMasteryMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
