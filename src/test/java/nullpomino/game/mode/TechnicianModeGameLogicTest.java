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
 * Covers game-logic methods in {@link TechnicianMode}: getName, playerInit,
 * loadSetting/saveSetting round-trip, ranking arrays (5 types), calcScore
 * (T-spin/B2B/combo/all-clear scoring, goal reduction, level-up, ending),
 * onLast (level timer, total timer, roll time), startGame, setSpeed,
 * and drop scoring.
 */
class TechnicianModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("TECHNICIAN", new TechnicianMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "goal"));
		assertEquals(0, readInt(mode, "levelTimer"));
		assertFalse(readBoolean(mode, "levelTimeOut"));
		assertEquals(0, readInt(mode, "totalTimer"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 3);
		setInt(mode, "goaltype", 2);
		setBoolean(mode, "enableB2B", true);
		setBoolean(mode, "enableCombo", false);
		setBoolean(mode, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		TechnicianMode dest = new TechnicianMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);

		assertEquals(3, readInt(dest, "startlevel"));
		assertEquals(2, readInt(dest, "goaltype"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(5, rankingScore.length); // RANKING_TYPE=5
		assertEquals(10, rankingScore[0].length); // RANKING_MAX=10
		assertNotNull(readField(mode, "rankingLines"));
		assertNotNull(readField(mode, "rankingTime"));
	}

	@Test
	void startGameSetsOptions() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3);
		setBoolean(mode, "enableB2B", true);
		setBoolean(mode, "enableCombo", true);

		mode.startGame(engine, 0);

		assertEquals(3, engine.statistics.level);
		assertTrue(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_NORMAL, engine.comboType);
		assertEquals((3 + 1) * 5, readInt(mode, "goal"));
		assertEquals(8, engine.speed.lineDelay);
	}

	// ---- calcScore tests ----

	@Test
	void calcScoreSingleLineGives100Points() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		// Set goal high enough so the time-bonus branch (goal <= 0) does not fire
		setInt(mode, "goal", 100);

		mode.calcScore(engine, 0, 1);

		// Single: 100 * (level+1) = 100
		assertEquals(100, readInt(mode, "lastscore"));
		assertEquals(100, engine.statistics.score);
	}

	@Test
	void calcScoreDoubleGives300Points() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		assertEquals(300, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTripleGives500Points() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 3);

		assertEquals(500, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreFourLinesGives800Points() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 4);

		assertEquals(800, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreFourLinesWithB2BGives1200Points() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.b2b = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 4);

		// B2B four: 1200 * (level+1) = 1200
		assertEquals(1200, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinSingleGives800Points() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = false;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(800, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreAllClearGives1800Bonus() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(1900, readInt(mode, "lastscore")); // 100 + 1800
	}

	@Test
	void calcScoreReducesGoal() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;
		setInt(mode, "goal", 10);

		mode.calcScore(engine, 0, 1);

		// lastgoal = ((pts/100)/(level+1)) + COMBO_GOAL_TABLE[0]
		// = (100/100)/1 + 0 = 1
		// goal = 10 - 1 = 9
		assertEquals(9, readInt(mode, "goal"));
	}

	@Test
	void calcScoreGoalReachedLevelsUp() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;
		setInt(mode, "goal", 0); // Already at goal

		mode.calcScore(engine, 0, 1);

		// Should level up since goal <= 0
		assertEquals(1, engine.statistics.level);
		assertEquals((1 + 1) * 5, readInt(mode, "goal")); // new goal
	}

	@Test
	void calcScoreEndsAtLevel14ForLv15Game() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 14;
		setInt(mode, "goal", 0);
		setInt(mode, "goaltype", 0); // LV15-EASY

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.ending); // Game ending
	}

	@Test
	void calcScoreEnding2ForSpecialAtLevel29() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 29;
		setInt(mode, "goal", 0);
		setInt(mode, "goaltype", 4); // SPECIAL

		mode.calcScore(engine, 0, 1);

		assertEquals(2, engine.ending); // Roll ending
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	@Test
	void onLastDecrementsLevelTimerWhenActive() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertTrue(readInt(mode, "levelTimer") > 0);
	}

	@Test
	void afterSoftDropFallAddsScore() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.afterSoftDropFall(engine, 0, 5);

		assertEquals(5, engine.statistics.scoreFromSoftDrop);
		assertEquals(5, engine.statistics.score);
	}

	@Test
	void afterHardDropFallAddsDoubleScore() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.afterHardDropFall(engine, 0, 10);

		assertEquals(20, engine.statistics.scoreFromHardDrop);
		assertEquals(20, engine.statistics.score);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(TechnicianMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeSaveSetting(TechnicianMode mode, CustomProperties prop) throws Exception {
		Method m = TechnicianMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeLoadSetting(TechnicianMode mode, CustomProperties prop) throws Exception {
		Method m = TechnicianMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
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
