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
 * Covers game-logic methods in {@link MarathonMode}: getName, playerInit,
 * loadSetting/saveSetting round-trip, ranking arrays, updateRanking,
 * calcScore (line clear scoring, T-spin, B2B, combo, all-clear, level-up,
 * game-ending), onLast (scgettime increment), startGame initialization,
 * and setSpeed.
 */
class MarathonModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("MARATHON", new MarathonMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_GREEN, engine.framecolor);
		assertEquals(0, readInt(mode, "goaltype"));
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 5);
		setInt(mode, "goaltype", 1);
		setInt(mode, "tspinEnableType", 2);
		setBoolean(mode, "enableB2B", true);
		setBoolean(mode, "enableCombo", true);
		setBoolean(mode, "big", true);

		CustomProperties prop = new CustomProperties();
		mode.saveSetting(prop);

		MarathonMode dest = new MarathonMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		dest.loadSetting(prop);

		assertEquals(5, readInt(dest, "startlevel"));
		assertEquals(1, readInt(dest, "goaltype"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		assertNotNull(rankingScore);
		assertEquals(3, rankingScore.length); // GAMETYPE_MAX = 3
		assertEquals(10, rankingScore[0].length); // RANKING_MAX = 10
		assertNotNull(rankingLines);
		assertNotNull(rankingTime);
	}

	@Test
	void startGameSetsOptions() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3);
		setBoolean(mode, "enableB2B", true);
		setBoolean(mode, "enableCombo", true);

		mode.startGame(engine, 0);

		assertEquals(3, engine.statistics.level);
		assertEquals(1, engine.statistics.levelDispAdd);
		assertTrue(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_NORMAL, engine.comboType);
	}

	@Test
	void calcScoreSingleLineGives100Points() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		// Place a block so field is not empty (prevents all-clear bonus)
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// Single: 100 * (level + 1) = 100 * 1 = 100
		assertEquals(1, readInt(mode, "lastevent"));
		assertEquals(100, readInt(mode, "lastscore"));
		assertEquals(100, engine.statistics.score);
	}

	@Test
	void calcScoreDoubleGives300Points() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		assertEquals(2, readInt(mode, "lastevent"));
		assertEquals(300, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTripleGives500Points() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 3);

		assertEquals(3, readInt(mode, "lastevent"));
		assertEquals(500, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreFourLinesGives800Points() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 4);

		assertEquals(4, readInt(mode, "lastevent"));
		assertEquals(800, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreFourLinesWithB2BGives1200Points() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.b2b = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 4);

		// B2B four: 1200 * (level + 1) = 1200
		assertEquals(1200, readInt(mode, "lastscore"));
		assertTrue(readBoolean(mode, "lastb2b"));
	}

	@Test
	void calcScoreTSpinSingleGives800Points() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = false;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(8, readInt(mode, "lastevent"));
		assertEquals(800, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinDoubleGives1200Points() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		assertEquals(10, readInt(mode, "lastevent"));
		assertEquals(1200, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreAllClearGives1800Bonus() throws Exception {
		MarathonMode mode = new MarathonMode();
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
		MarathonMode mode = new MarathonMode();
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
		assertEquals(3, readInt(mode, "lastcombo"));
	}

	@Test
	void calcScoreLevelUpOn10Lines() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;
		engine.statistics.lines = 10;

		mode.calcScore(engine, 0, 1);

		// lines = 10, which equals (level+1)*10 = 10 → level up
		assertEquals(1, engine.statistics.level);
	}

	@Test
	void calcScoreGameEndsAtGoal() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		setInt(mode, "goaltype", 0); // 150 lines goal
		engine.statistics.lines = 150;

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.ending);
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	@Test
	void setSpeedForLevel0() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);
	}

	@Test
	void setSpeedForLevel18() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 18;

		mode.setSpeed(engine);

		assertEquals(-1, engine.speed.gravity); // 20G at high levels
	}

	// ---- helpers ----

	private static GameEngine freshEngine(MarathonMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(MarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(MarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(MarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(MarathonMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(MarathonMode mode, String name, boolean value) throws Exception {
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
}
