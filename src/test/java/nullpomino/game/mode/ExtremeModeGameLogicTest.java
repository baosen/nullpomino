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
 * Covers game-logic methods in {@link ExtremeMode}: getName, playerInit,
 * loadSetting/saveSetting round-trip, ranking arrays (2 types), updateRanking,
 * calcScore (scoring with T-spin, B2B, combo, all-clear, level-up, ending,
 * endless mode), onLast (roll time and scgettime), startGame, and setSpeed.
 */
class ExtremeModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("EXTREME", new ExtremeMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_RED, engine.framecolor);
		assertTrue(engine.staffrollEnable);
		assertTrue(engine.staffrollNoDeath);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 5);
		setInt(mode, "tspinEnableType", 2);
		setBoolean(mode, "enableB2B", true);
		setBoolean(mode, "enableCombo", false);
		setBoolean(mode, "big", true);
		setBoolean(mode, "endless", true);

		CustomProperties prop = new CustomProperties();
		mode.saveSetting(prop);

		ExtremeMode dest = new ExtremeMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		dest.loadSetting(prop);

		assertEquals(5, readInt(dest, "startlevel"));
		assertEquals(2, readInt(dest, "tspinEnableType"));
		assertTrue(readBoolean(dest, "endless"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(2, rankingScore.length); // RANKING_TYPE = 2
		assertEquals(10, rankingScore[0].length); // RANKING_MAX = 10
		assertNotNull(readField(mode, "rankingLines"));
		assertNotNull(readField(mode, "rankingTime"));
	}

	@Test
	void startGameSetsOptions() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3);
		setBoolean(mode, "enableB2B", true);
		setBoolean(mode, "enableCombo", true);
		setBoolean(mode, "big", false);

		mode.startGame(engine, 0);

		assertEquals(3, engine.statistics.level);
		assertEquals(1, engine.statistics.levelDispAdd);
		assertTrue(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_NORMAL, engine.comboType);
	}

	@Test
	void setSpeedForLevel0() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		mode.setSpeed(engine);

		assertEquals(-1, engine.speed.gravity); // Extreme is always 20G
		assertEquals(25, engine.speed.are);
	}

	@Test
	void setSpeedForHighLevel() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 18;

		mode.setSpeed(engine);

		assertEquals(-1, engine.speed.gravity);
		assertEquals(0, engine.speed.are);
		assertEquals(0, engine.speed.areLine);
		assertEquals(0, engine.speed.lineDelay);
	}

	// ---- calcScore tests ----

	@Test
	void calcScoreSingleLineGives100Points() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// Single: 100 * (level+1) = 100
		assertEquals(100, readInt(mode, "lastscore"));
		assertEquals(100, engine.statistics.score);
	}

	@Test
	void calcScoreDoubleGives300Points() throws Exception {
		ExtremeMode mode = new ExtremeMode();
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
		ExtremeMode mode = new ExtremeMode();
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
		ExtremeMode mode = new ExtremeMode();
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
		ExtremeMode mode = new ExtremeMode();
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
		assertTrue(readBoolean(mode, "lastb2b"));
	}

	@Test
	void calcScoreTSpinSingleGives800Points() throws Exception {
		ExtremeMode mode = new ExtremeMode();
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
	void calcScoreTSpinDoubleGives1200Points() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		assertEquals(1200, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinTripleGives1600Points() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 3);

		assertEquals(1600, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreAllClearGives1800Bonus() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// Single (100) + all-clear (1800) = 1900
		assertEquals(1900, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreComboBonus() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.combo = 3;
		setBoolean(mode, "enableCombo", true);

		mode.calcScore(engine, 0, 1);

		// Single (100) + combo (2*50=100) + all-clear (1800) = 2000
		assertEquals(2000, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreLevelUpOn10Lines() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;
		engine.statistics.lines = 10;

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.statistics.level);
	}

	@Test
	void calcScoreEndingAt200Lines() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;
		engine.statistics.lines = 200;

		mode.calcScore(engine, 0, 1);

		assertEquals(2, engine.ending); // roll ending
	}

	@Test
	void calcScoreEndlessModeNoEnding() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;
		engine.statistics.lines = 200;
		setBoolean(mode, "endless", true);

		mode.calcScore(engine, 0, 1);

		assertEquals(0, engine.ending); // No ending in endless mode
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);
		setInt(mode, "rolltime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	@Test
	void onLastIncrementsRolltimeDuringEnding() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "rolltime"));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(ExtremeMode mode) {
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
