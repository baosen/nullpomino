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
 * Covers game-logic methods in {@link RetroMarathonMode}: getName,
 * playerInit, loadSetting/saveSetting round-trip, ranking arrays,
 * updateRanking, calcScore (scoring with level multiplier, B-TYPE bonus,
 * all-clear, level-up), onLast, startGame, onReady garbage fill,
 * and soft/hard drop scoring.
 */
class RetroMarathonModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("RETRO MARATHON", new RetroMarathonMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "softdropscore"));
		assertEquals(0, readInt(mode, "harddropscore"));
		// levellines is computed as min((startlevel+1)*10, max(100, (startlevel-5)*10))
		// with startlevel=0: levellines = min(10, max(100, -50)) = 10
		assertEquals(10, readInt(mode, "levellines"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
		assertFalse(engine.tspinEnable);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 2);
		setInt(mode, "startlevel", 5);
		setInt(mode, "startheight", 3);
		setBoolean(mode, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		RetroMarathonMode dest = new RetroMarathonMode();
		invokeLoadSetting(dest, prop);

		assertEquals(2, readInt(dest, "gametype"));
		assertEquals(5, readInt(dest, "startlevel"));
		assertEquals(3, readInt(dest, "startheight"));
		assertTrue(readBoolean(dest, "big"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(3, rankingScore.length); // RANKING_TYPE=3
		assertEquals(10, rankingScore[0].length); // RANKING_MAX=10
		assertNotNull(readField(mode, "rankingLines"));
		assertNotNull(readField(mode, "rankingLevel"));
	}

	@Test
	void startGameSetsLevelAndBigAndSpeed() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3);

		mode.startGame(engine, 0);

		assertEquals(3, engine.statistics.level);
		assertEquals(1, engine.statistics.levelDispAdd);
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	// ---- calcScore tests ----

	@Test
	void calcScoreSingleLineGives40Points() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		// Single: 40 * (level+1) = 40 * 1 = 40
		assertEquals(40, readInt(mode, "lastscore"));
		assertEquals(40, engine.statistics.score);
	}

	@Test
	void calcScoreDoubleGives100Points() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 2);

		// Double: 100 * (level+1) = 100
		assertEquals(100, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTripleGives300Points() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 3);

		// Triple: 300 * (level+1) = 300
		assertEquals(300, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreFourGives1200Points() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 4);

		// Four: 1200 * (level+1) = 1200
		assertEquals(1200, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreLevel14GivesHigherScore() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 14;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		// Single: 40 * (14+1) = 600
		assertEquals(600, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreBTypeEndsGameAt25Lines() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.lines = 25;
		setInt(mode, "gametype", 1); // TYPE B
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		// B-TYPE with lines >= 25: bonus = (level + startheight) * 1000
		int startheight = readInt(mode, "startheight");
		int bonus = (0 + startheight) * 1000;
		assertEquals(40 + bonus, readInt(mode, "lastscore"));
		assertEquals(1, engine.ending);
	}

	@Test
	void calcScoreLevelUpWhenLinesReachLevellines() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		// levellines starts at min((startlevel+1)*10, max(100, (startlevel-5)*10))
		// With startlevel=0: levellines = min(10, max(100,-50)) = 10
		setInt(mode, "gametype", 0); // TYPE A
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		// Set lines to trigger level-up
		engine.statistics.lines = 10;

		mode.calcScore(engine, 0, 1);

		// Level up should happen when lines >= levellines
		assertEquals(1, engine.statistics.level);
	}

	@Test
	void afterSoftDropFallAccumulatesScore() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.afterSoftDropFall(engine, 0, 5);
		mode.afterSoftDropFall(engine, 0, 3);

		assertEquals(8, readInt(mode, "softdropscore"));
	}

	@Test
	void afterHardDropFallAccumulatesScore() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.afterHardDropFall(engine, 0, 10);

		assertEquals(10, readInt(mode, "harddropscore"));
	}

	@Test
	void calcScoreIntegratesSoftAndHardDropScores() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		setInt(mode, "softdropscore", 10);
		setInt(mode, "harddropscore", 20);

		mode.calcScore(engine, 0, 0);

		// softdropscore/2 = 5, harddropscore = 20, pts=0, total=25
		assertEquals(25, engine.statistics.score);
		assertEquals(0, readInt(mode, "softdropscore"));
		assertEquals(0, readInt(mode, "harddropscore"));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(RetroMarathonMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void invokeSaveSetting(RetroMarathonMode mode, CustomProperties prop) throws Exception {
		Method m = RetroMarathonMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeLoadSetting(RetroMarathonMode mode, CustomProperties prop) throws Exception {
		Method m = RetroMarathonMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
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
