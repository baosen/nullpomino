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
 * Covers game-logic methods in {@link PhysicianMode}: getName,
 * getGameStyle, playerInit, loadSetting/saveSetting round-trip,
 * ranking arrays, updateRanking, calcScore (gem-clearing scoring,
 * chain tracking), onLast (meter, gem count, excellent condition),
 * onReady (hover blocks), lineClearEnd (chain reset), startGame
 * initialization, and setSpeed.
 */
class PhysicianModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("PHYSICIAN (RC1)", new PhysicianMode().getName());
	}

	@Test
	void getGameStyleReturnsPhysician() {
		assertEquals(GameEngine.GAMESTYLE_PHYSICIAN, new PhysicianMode().getGameStyle());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "gemsClearedChainTotal"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_PURPLE, engine.framecolor);
		assertEquals(GameEngine.ClearType.LINE_COLOR, engine.clearMode);
		assertFalse(engine.garbageColorClear);
		assertEquals(4, engine.colorClearSize);
		assertEquals(GameEngine.LineGravity.CASCADE, engine.lineGravityType);
		assertTrue(engine.randomBlockColor);
		assertTrue(engine.connectBlocks);
		assertEquals(18, engine.cascadeDelay);
		assertTrue(engine.gemSameColor);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "hoverBlocks", 50);
		setInt(mode, "speed", 2);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		PhysicianMode dest = new PhysicianMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);

		assertEquals(50, readInt(dest, "hoverBlocks"));
		assertEquals(2, readInt(dest, "speed"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		assertNotNull(rankingScore);
		assertEquals(10, rankingScore.length);
		assertNotNull(rankingTime);
		assertEquals(10, rankingTime.length);
	}

	@Test
	void updateRankingInsertsFirstEntry() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 50000, 3600);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		assertEquals(50000, rankingScore[0]);
	}

	@Test
	void checkRankingReturnsMinusOneForUnranked() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		for (int i = 0; i < 10; i++) rankingScore[i] = 999999;

		int rank = invokeCheckRanking(mode, 100, 0);
		assertEquals(-1, rank);
	}

	@Test
	void startGameSetsDefaults() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "speed", 1);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertEquals(30, engine.speed.are);
		assertEquals(30, engine.speed.areLine);
		assertEquals(10, engine.speed.das);
		assertEquals(30, engine.speed.lockDelay);
	}

	@Test
	void setSpeedCalculatesCorrectly() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "speed", 1);
		engine.statistics.totalPieceLocked = 10;

		mode.setSpeed(engine);

		// BASE_SPEEDS[1] = 20, so gravity = 20 * (10 + (10/10)) = 20 * 11 = 220
		assertEquals(220, engine.speed.gravity);
		assertEquals(3600, engine.speed.denominator);
	}

	@Test
	void calcScoreWithGemsClearedAndLines() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		// Simulate gem clearing: set gemsCleared on field
		engine.field.gemsCleared = 3;
		setInt(mode, "speed", 1);

		mode.calcScore(engine, 0, 1);

		// 3 gems cleared with chain starting from 0:
		// gem 0: 1 << 0 = 1, chainTotal=1
		// gem 1: 1 << 1 = 2, chainTotal=2
		// gem 2: 1 << 2 = 4, chainTotal=3
		// pts = (1+2+4) * (1+1) * 100 = 7 * 2 * 100 = 1400
		assertEquals(1400, readInt(mode, "lastscore"));
		assertEquals(120, readInt(mode, "scgettime"));
		assertTrue(engine.statistics.score >= 1400);
	}

	@Test
	void calcScoreChainContinuesAfter5Gems() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		// 7 gems cleared
		engine.field.gemsCleared = 7;
		setInt(mode, "speed", 0);

		mode.calcScore(engine, 0, 1);

		// First 5 gems: 1 + 2 + 4 + 8 + 16 = 31
		// Remaining 2 gems: 2 << 5 = 64
		// pts = (31 + 64) * (0+1) * 100 = 95 * 100 = 9500
		assertEquals(9500, readInt(mode, "lastscore"));
		assertEquals(7, readInt(mode, "gemsClearedChainTotal"));
	}

	@Test
	void calcScoreNoGemsNoScore() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		engine.field.gemsCleared = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(0, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreNoLinesNoScore() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		engine.field.gemsCleared = 3;

		mode.calcScore(engine, 0, 0);

		// lines == 0, so no score even with gems
		assertEquals(0, readInt(mode, "lastscore"));
	}

	@Test
	void lineClearEndResetsChainTotal() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gemsClearedChainTotal", 10);

		mode.lineClearEnd(engine, 0);

		assertEquals(0, readInt(mode, "gemsClearedChainTotal"));
	}

	@Test
	void onReadyWithHoverBlocksPopulatesField() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setInt(mode, "hoverBlocks", 10);

		mode.onReady(engine, 0);

		assertNotNull(engine.field);
	}

	@Test
	void onReadyWithZeroHoverBlocksDoesNothing() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setInt(mode, "hoverBlocks", 0);

		mode.onReady(engine, 0);

		// No exception expected
	}

	@Test
	void onLastDecrementsScgettime() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 50);

		mode.onLast(engine, 0);

		assertEquals(49, readInt(mode, "scgettime"));
	}

	@Test
	void onLastMeterAndExcellentCondition() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.createFieldIfNeeded();
		setInt(mode, "hoverBlocks", 40);

		mode.onLast(engine, 0);

		// No gems -> rest = 0 -> excellent
		assertFalse(engine.timerActive);
	}

	@Test
	void onLastWithGemsDoesNotTriggerExcellent() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.createFieldIfNeeded();
		setInt(mode, "hoverBlocks", 40);

		// Put a gem block on field
		Block gem = new Block(Block.BLOCK_COLOR_GEM_RED);
		engine.field.setBlock(0, 0, gem);

		mode.onLast(engine, 0);

		assertTrue(engine.timerActive, "Should still be active with gems present");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(PhysicianMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(PhysicianMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(PhysicianMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(PhysicianMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(PhysicianMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(PhysicianMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadSetting(PhysicianMode mode, CustomProperties prop) throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(PhysicianMode mode, CustomProperties prop) throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(PhysicianMode mode, int sc, int time) throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod("updateRanking", int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, time);
	}

	private static int invokeCheckRanking(PhysicianMode mode, int sc, int time) throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod("checkRanking", int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, time);
	}
}
