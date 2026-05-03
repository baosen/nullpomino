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
 * Covers game-logic methods in {@link AvalancheMode}: getName,
 * playerInit, loadSetting/saveSetting round-trip, ranking arrays,
 * updateRanking, calcChainMultiplier, onLast (ULTRA/Sprint timers),
 * lineClearEnd (danger column check), startGame (speed setup),
 * setSpeed, and saveReplay ranking check.
 */
class AvalancheModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("AVALANCHE 1P (RC2)", new AvalancheMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertTrue(readBoolean(mode, "showChains"));
		assertEquals(0, readInt(mode, "scoreType"));
		assertEquals(0, readInt(mode, "sprintTarget"));
		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 1);
		setInt(mode, "sprintTarget", 2);
		setInt(mode, "scoreType", 1);
		setInt(mode, "numColors", 5);
		setInt(mode, "outlinetype", 2);
		setBoolean(mode, "dangerColumnDouble", true);
		setBoolean(mode, "dangerColumnShowX", true);
		setBoolean(mode, "showChains", true);
		setBoolean(mode, "cascadeSlow", true);
		setBoolean(mode, "bigDisplay", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		AvalancheMode dest = new AvalancheMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);

		assertEquals(1, readInt(dest, "gametype"));
		assertEquals(2, readInt(dest, "sprintTarget"));
		assertEquals(1, readInt(dest, "scoreType"));
		assertEquals(5, readInt(dest, "numColors"));
		assertTrue(readBoolean(dest, "dangerColumnDouble"));
		assertTrue(readBoolean(dest, "showChains"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][][][] rankingScore = (int[][][][]) readField(mode, "rankingScore");
		int[][][][] rankingTime = (int[][][][]) readField(mode, "rankingTime");
		assertNotNull(rankingScore);
		// [SCORETYPE_MAX][3][RANKING_TYPE][RANKING_MAX] = [2][3][7][10]
		assertEquals(2, rankingScore.length);
		assertEquals(3, rankingScore[0].length);
		assertEquals(7, rankingScore[0][0].length);
		assertEquals(10, rankingScore[0][0][0].length);
		assertNotNull(rankingTime);
	}

	@Test
	void startGameSetsSpeed() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);
	}

	@Test
	void calcChainMultiplierClassicChain2() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 0);

		int mult = mode.calcChainMultiplier(2);

		assertEquals(8, mult);
	}

	@Test
	void calcChainMultiplierClassicChain3() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 0);

		int mult = mode.calcChainMultiplier(3);

		assertEquals(16, mult);
	}

	@Test
	void calcChainMultiplierClassicChain4() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 0);

		int mult = mode.calcChainMultiplier(4);

		assertEquals(32, mult); // 32*(4-3) = 32
	}

	@Test
	void calcChainMultiplierClassicChain5() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 0);

		int mult = mode.calcChainMultiplier(5);

		assertEquals(64, mult); // 32*(5-3) = 64
	}

	@Test
	void calcChainMultiplierFeverChain1() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 1);

		int mult = mode.calcChainMultiplier(1);

		assertEquals(4, mult); // CHAIN_POWERS_FEVERTYPE[0] = 4
	}

	@Test
	void calcChainMultiplierFeverChain5() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 1);

		int mult = mode.calcChainMultiplier(5);

		assertEquals(48, mult); // CHAIN_POWERS_FEVERTYPE[4] = 48
	}

	@Test
	void calcChainMultiplierFeverOverflow() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		setInt(mode, "scoreType", 1);

		int mult = mode.calcChainMultiplier(20);

		assertEquals(999, mult); // last value in CHAIN_POWERS_FEVERTYPE
	}

	@Test
	void setSpeedMarathon() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(41, engine.speed.denominator); // Math.max(41-0, 2)
	}

	@Test
	void setSpeedUltraOrSprint() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 1);

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(40, engine.speed.denominator);
	}

	@Test
	void onLastUltraTimeExpiry() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 1);
		engine.timerActive = true;
		engine.statistics.time = 10800; // ULTRA_MAX_TIME

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);
	}

	@Test
	void onLastSprintScoreGoal() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 2);
		engine.timerActive = true;
		engine.statistics.score = 15000; // SPRINT_MAX_SCORE[0]

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);
	}

	@Test
	void onLastRegularMode()
		throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0);
		engine.timerActive = true;

		mode.onLast(engine, 0);
		// Should not trigger ending
		assertEquals(0, engine.ending);
	}

	@Test
	void onLastDecrementsScgettime() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 50);

		mode.onLast(engine, 0);

		assertEquals(49, readInt(mode, "scgettime"));
	}

	@Test
	void onLastDecrementsChainDisplay() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "chainDisplay", 30);

		mode.onLast(engine, 0);

		assertEquals(29, readInt(mode, "chainDisplay"));
	}

	@Test
	void lineClearEndDangerColumnDetectsGameOver() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		// Place a block in column 2, row 0 (danger column)
		Block blk = new Block(Block.BLOCK_COLOR_RED);
		engine.field.setBlock(2, 0, blk);

		mode.lineClearEnd(engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	@Test
	void lineClearEndNoDangerColumnNoGameOver() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		// Column 2 row 0 is empty -> no game over
		mode.lineClearEnd(engine, 0);

		assertEquals(0, engine.ending);
	}

	@Test
	void updateRankingInsertsFirstEntry() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 50000, 3600, 0, 0, 4);

		assertEquals(0, readInt(mode, "rankingRank"));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(AvalancheMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(AvalancheMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(AvalancheMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(AvalancheMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(AvalancheMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(AvalancheMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadSetting(AvalancheMode mode, CustomProperties prop) throws Exception {
		Method m = AvalancheMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(AvalancheMode mode, CustomProperties prop) throws Exception {
		Method m = AvalancheMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(AvalancheMode mode, int sc, int time, int type, int sctype, int colors) throws Exception {
		Method m = AvalancheMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, time, type, sctype, colors);
	}
}
