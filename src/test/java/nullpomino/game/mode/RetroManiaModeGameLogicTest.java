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
 * Covers game-logic methods in {@link RetroManiaMode}: getName, playerInit,
 * loadSetting/saveSetting round-trip, ranking arrays, updateRanking,
 * calcScore (line-clear scoring with level multiplier, all-clear *10,
 * level-up via lines or timer), onLast, startGame, and drop scoring.
 */
class RetroManiaModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("RETRO MANIA", new RetroManiaMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		// Must set speed defaults before playerInit since gametype defaults to 0
		// and playerInit calls loadSetting
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "levelTimer"));
		assertEquals(0, readInt(mode, "linesAfterLastLevelUp"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
		assertFalse(engine.tspinEnable);
		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 5);
		setInt(mode, "gametype", 2);
		setBoolean(mode, "big", true);
		setBoolean(mode, "poweron", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		RetroManiaMode dest = new RetroManiaMode();
		invokeLoadSetting(dest, prop);

		assertEquals(5, readInt(dest, "startlevel"));
		assertEquals(2, readInt(dest, "gametype"));
		assertTrue(readBoolean(dest, "big"));
		assertTrue(readBoolean(dest, "poweron"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(4, rankingScore.length); // RANKING_TYPE=4
		assertEquals(10, rankingScore[0].length); // RANKING_MAX=10
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		assertNotNull(rankingLines);
		assertNotNull(rankingTime);
	}

	@Test
	void startGameSetsLevelAndBigAndSpeed() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3);

		mode.startGame(engine, 0);

		assertEquals(3, engine.statistics.level);
		assertEquals(1, engine.statistics.levelDispAdd);
	}

	@Test
	void onLastIncrementsScgettimeAndLevelTimer() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);
		setInt(mode, "levelTimer", 0);
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
		assertEquals(1, readInt(mode, "levelTimer"));
	}

	// ---- calcScore tests ----

	@Test
	void calcScoreSingleLineGives100Points() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		// Single: 100 * mult, where mult = min(level/2 + 1, 5) = min(0/2+1,5) = 1
		assertEquals(100, readInt(mode, "lastscore"));
		assertEquals(100, engine.statistics.score);
	}

	@Test
	void calcScoreDoubleGives400Points() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 2);

		// Double: 400 * mult = 400 * 1 = 400
		assertEquals(400, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTripleGives900Points() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 3);

		// Triple: 900 * mult = 900 * 1 = 900
		assertEquals(900, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreFourGives2000Points() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 4);

		// Four: 2000 * mult = 2000 * 1 = 2000
		assertEquals(2000, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreAllClearMultipliesBy10() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// All-clear: pts *= 10 → 100 * 10 = 1000
		assertEquals(1000, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreHigherLevelIncreasesMultiplier() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 8; // mult = min(8/2+1,5) = 5
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		// Single: 100 * 5 = 500
		assertEquals(500, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreLevelUpAfter4LinesSinceLastLevelUp() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		setInt(mode, "linesAfterLastLevelUp", 3);
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		// linesAfterLastLevelUp becomes 4, which triggers level up
		assertEquals(1, engine.statistics.level);
		assertEquals(0, readInt(mode, "linesAfterLastLevelUp"));
	}

	@Test
	void calcScoreLevelUpByTimer() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		// levelTime[0] = 3584, so set levelTimer high enough
		setInt(mode, "levelTimer", 3584);

		mode.calcScore(engine, 0, 0);

		// levelTimer >= levelTime[level], level up via timer
		assertEquals(1, engine.statistics.level);
		assertEquals(0, readInt(mode, "levelTimer"));
	}

	@Test
	void afterSoftDropFallAddsScore() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.afterSoftDropFall(engine, 0, 5);

		assertEquals(5, engine.statistics.scoreFromSoftDrop);
		assertEquals(5, engine.statistics.score);
	}

	@Test
	void afterHardDropFallAddsScore() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.afterHardDropFall(engine, 0, 5);

		assertEquals(5, engine.statistics.scoreFromHardDrop);
		assertEquals(5, engine.statistics.score);
	}

	@Test
	void updateRankingInsertsCorrectly() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rs = (int[][]) readField(mode, "rankingScore");
		rs[0][0] = 50000;
		rs[0][1] = 30000;

		invokeUpdateRanking(mode, 40000, 100, 50, 0);

		int rank = readInt(mode, "rankingRank");
		assertEquals(1, rank);
		assertEquals(40000, rs[0][1]);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(RetroManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeSaveSetting(RetroManiaMode mode, CustomProperties prop) throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeLoadSetting(RetroManiaMode mode, CustomProperties prop) throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(RetroManiaMode mode, int sc, int li, int time, int type) throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, li, time, type);
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
