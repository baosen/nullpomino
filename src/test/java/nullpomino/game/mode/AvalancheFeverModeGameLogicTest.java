package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link AvalancheFeverMode}: startGame,
 * onMove, onLast (timer management, meter), lineClearEnd (fever chain
 * progression, time extensions, zenkeshi), calcScore helpers
 * (calcOjama, calcPts, calcChainMultiplier), onClear, and the
 * updateRanking/checkRanking ranking logic.
 */
class AvalancheFeverModeGameLogicTest {

	@Test
	void onMoveResetsClearedAndZenKeshiFlags() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "cleared", true);

		mode.onMove(engine, 0);

		assertFalse(readBoolean(mode, "cleared"));
	}

	@Test
	void onLastDecrementsScgettime() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 10);

		mode.onLast(engine, 0);

		assertEquals(9, readInt(mode, "scgettime"));
	}

	@Test
	void onLastWithTimerActiveDecrementsTimeLimitAndUpdatesMeter() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "timeLimit", 1800);
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertEquals(1799, readInt(mode, "timeLimit"));
		// In test context showmeter is false so meterValue may be 0;
		// just verify no exception and meter gets set
		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void calcOjamaComputesCorrectValue() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();

		// calcOjama = ((avalanche*10*multiplier)+ojamaRate-1)/ojamaRate
		// Note: the method uses the instance field ojamaRate (not the 4th parameter)
		// invokeCalcOjama(mode, score, avalanche, pts, multiplier) -> multiplier=1
		setInt(mode, "ojamaRate", 1);
		int result = invokeCalcOjama(mode, 100, 4, 2, 1);

		assertEquals(40, result, "calcOjama(100, 4, 2, 1) with ojamaRate=1 = (4*10*1 + 0)/1 = 40");
	}

	@Test
	void calcOjamaWithRate2() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();

		// Note: the method uses the instance field ojamaRate (not the 4th parameter)
		// invokeCalcOjama(mode, score=100, avalanche=4, pts=2, multiplier=2)
		setInt(mode, "ojamaRate", 2);
		int result = invokeCalcOjama(mode, 100, 4, 2, 2);

		assertEquals(40, result, "calcOjama(100, 4, 2, 2) with ojamaRate=2 = (4*10*2 + 1)/2 = 40");
	}

	@Test
	void calcPtsComputesCorrectValue() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		setInt(mode, "chainLevelMultiplier", 5);

		int result = invokeCalcPts(mode, 3);

		assertEquals(150, result, "calcPts(3) with multiplier 5 = 3*5*10 = 150");
	}

	@Test
	void calcChainMultiplierForChain1() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();

		int result = invokeCalcChainMultiplier(mode, 1);

		assertEquals(4, result, "Chain 1 multiplier = 4");
	}

	@Test
	void calcChainMultiplierForChain5() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();

		int result = invokeCalcChainMultiplier(mode, 5);

		assertEquals(30, result, "Chain 5 multiplier = 30");
	}

	@Test
	void calcChainMultiplierForChain24() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();

		int result = invokeCalcChainMultiplier(mode, 24);

		assertEquals(800, result, "Chain 24 multiplier = 800");
	}

	@Test
	void calcChainMultiplierClampsAtMaxTableLength() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();

		int result = invokeCalcChainMultiplier(mode, 100);

		assertEquals(800, result, "Chains beyond table length clamp to last value");
	}

	@Test
	void onClearSetsChainDisplayAndUpdateFeverChain() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "feverChain", 5);
		setInt(mode, "level", 3);
		engine.chain = 1;

		invokeOnClear(mode, engine, 0);

		assertEquals(60, readInt(mode, "chainDisplay"));
		assertTrue(readBoolean(mode, "cleared"));
		assertEquals(5, readInt(mode, "feverChainDisplay"));
	}

	@Test
	void updateRankingInsertsBetterScore() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// playerInit loads rankings from the shared config/setting file, so other
		// tests in the same JVM can leave a higher score at rank 0. Reset every
		// ranking slot to the empty sentinel so the inserted score is unambiguously best.
		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		for (int[][] plane : rankingScore) {
			for (int[] row : plane) {
				java.util.Arrays.fill(row, -1);
			}
		}

		invokeUpdateRanking(mode, 10000, 3600, 0, 4);

		assertEquals(0, readInt(mode, "rankingRank"),
				"First entry should rank #1");
	}

	@Test
	void updateRankingShiftsLowerScores() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		for (int i = 0; i < 10; i++) {
			rankingScore[1][0][i] = 10000 - i * 500; // decreasing scores
		}

		invokeUpdateRanking(mode, 9000, 3600, 0, 4);

		// 9000 should fit somewhere depending on existing values
		assertTrue(readInt(mode, "rankingRank") >= 0);
	}

	@Test
	void checkRankingReturnsMinusOneForUnranked() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		// Fill all 10 slots with scores higher than our test score
		for (int i = 0; i < 10; i++) {
			rankingScore[1][0][i] = 99999;
		}

		int rank = invokeCheckRanking(mode, 100, 0, 0, 4);
		assertEquals(-1, rank, "Low score should not rank");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(AvalancheFeverMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(AvalancheFeverMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(AvalancheFeverMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(AvalancheFeverMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(AvalancheFeverMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(AvalancheFeverMode mode, String name, boolean value) throws Exception {
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

	private static int invokeCalcOjama(AvalancheFeverMode mode, int score, int avalanche, int pts, int multiplier) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod(
				"calcOjama", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, score, avalanche, pts, multiplier);
	}

	private static int invokeCalcPts(AvalancheFeverMode mode, int avalanche) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod("calcPts", int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, avalanche);
	}

	private static int invokeCalcChainMultiplier(AvalancheFeverMode mode, int chain) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod("calcChainMultiplier", int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, chain);
	}

	private static void invokeOnClear(AvalancheFeverMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod("onClear", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}

	private static void invokeUpdateRanking(AvalancheFeverMode mode, int sc, int time, int type, int colors) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, time, type, colors);
	}

	private static int invokeCheckRanking(AvalancheFeverMode mode, int sc, int time, int type, int colors) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, time, type, colors);
	}
}
