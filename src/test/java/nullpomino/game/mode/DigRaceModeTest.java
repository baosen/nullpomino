package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link DigRaceMode}: the registry-facing
 * surface, the preset / ranking property-key contracts, and the
 * playerInit defaults. Settings UI, render path, and net pipeline
 * need an SDL renderer or a live netplay session and stay out of scope.
 */
class DigRaceModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("DIG RACE", new DigRaceMode().getName());
	}

	@Test
	void loadPresetReadsEngineSpeedFieldsAndModeOptionsFromPropFile() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("digrace.gravity.7", 64);
		prop.setProperty("digrace.denominator.7", 256);
		prop.setProperty("digrace.are.7", 30);
		prop.setProperty("digrace.areLine.7", 25);
		prop.setProperty("digrace.lineDelay.7", 40);
		prop.setProperty("digrace.lockDelay.7", 30);
		prop.setProperty("digrace.das.7", 14);
		prop.setProperty("digrace.bgmno.7", 4);
		prop.setProperty("digrace.big.7", true);
		prop.setProperty("digrace.goaltype.7", 2);

		invokeLoadPreset(mode, engine, prop, 7);

		assertEquals(64, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(30, engine.speed.are);
		assertEquals(25, engine.speed.areLine);
		assertEquals(40, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
		assertEquals(4, readInt(mode, "bgmno"));
		assertEquals(true, readBoolean(mode, "big"));
		assertEquals(2, readInt(mode, "goaltype"));
	}

	@Test
	void loadPresetAppliesDocumentedDefaultsForMissingPresetEntries() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(0, engine.speed.are);
		assertEquals(0, engine.speed.areLine);
		assertEquals(0, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
		assertEquals(0, readInt(mode, "bgmno"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(1, readInt(mode, "goaltype"),
				"goaltype defaults to the legacy 10-line preset (index 1)");
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderDigracePrefix() throws Exception {
		DigRaceMode source = new DigRaceMode();
		GameEngine engine = freshEngine(source);
		engine.speed.gravity = 99;
		engine.speed.denominator = 60;
		engine.speed.are = 12;
		engine.speed.areLine = 11;
		engine.speed.lineDelay = 5;
		engine.speed.lockDelay = 25;
		engine.speed.das = 9;
		setInt(source, "bgmno", 7);
		setBoolean(source, "big", true);
		setInt(source, "goaltype", 0);

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, engine, prop, 1);

		assertEquals(99, prop.getProperty("digrace.gravity.1", -1));
		assertEquals(true, prop.getProperty("digrace.big.1", false));
		assertEquals(0, prop.getProperty("digrace.goaltype.1", -1));

		DigRaceMode dest = new DigRaceMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1);

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(60, destEngine.speed.denominator);
		assertEquals(7, readInt(dest, "bgmno"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(0, readInt(dest, "goaltype"));
	}

	@Test
	void loadRankingFillsWithDocumentedDefaultsWhenPropFileIsEmpty() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingPiece = (int[][]) readField(mode, "rankingPiece");
		assertNotNull(rankingTime);
		assertNotNull(rankingLines);
		assertNotNull(rankingPiece);
		// time defaults to -1 (unranked sentinel), lines and pieces to 0.
		for(int goal = 0; goal < rankingTime.length; goal++) {
			for(int slot = 0; slot < rankingTime[goal].length; slot++) {
				assertEquals(-1, rankingTime[goal][slot]);
				assertEquals(0, rankingLines[goal][slot]);
				assertEquals(0, rankingPiece[goal][slot]);
			}
		}
	}

	@Test
	void loadRankingAndSaveRankingRoundTripPerGoalTypeBucket() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingPiece = (int[][]) readField(mode, "rankingPiece");
		// Stamp every cell with a unique value so a swapped goal-type or
		// slot index would surface as a wrong number on load.
		for(int goal = 0; goal < rankingTime.length; goal++) {
			for(int slot = 0; slot < rankingTime[goal].length; slot++) {
				rankingTime[goal][slot] = 1000 + goal * 100 + slot;
				rankingLines[goal][slot] = 50 + slot;
				rankingPiece[goal][slot] = 100 + slot;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		// Spot-check the property key shape.
		assertEquals(1000, prop.getProperty("digrace.ranking.Standard.0.time.0", -1));
		assertEquals(101, prop.getProperty("digrace.ranking.Standard.0.piece.1", -1));

		DigRaceMode dest = new DigRaceMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		int[][] destLines = (int[][]) readField(dest, "rankingLines");
		int[][] destPiece = (int[][]) readField(dest, "rankingPiece");
		for(int goal = 0; goal < destTime.length; goal++) {
			assertArrayEquals(rankingTime[goal], destTime[goal]);
			assertArrayEquals(rankingLines[goal], destLines[goal]);
			assertArrayEquals(rankingPiece[goal], destPiece[goal]);
		}
	}

	@Test
	void playerInitInstallsFreshMenuStateAndAllocatesRankingArrays() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "rankingLines"));
		assertNotNull(readField(mode, "rankingPiece"));
		// Ranking arrays are dimensioned [GOALTYPE_MAX][RANKING_MAX] = [3][10].
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		assertEquals(3, rankingTime.length);
		assertEquals(10, rankingTime[0].length);
	}

	private static GameEngine freshEngine(DigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(DigRaceMode mode, String name) throws Exception {
		Field f = DigRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(DigRaceMode mode, String name) throws Exception {
		Field f = DigRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(DigRaceMode mode, String name) throws Exception {
		Field f = DigRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(DigRaceMode mode, String name, int value) throws Exception {
		Field f = DigRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(DigRaceMode mode, String name, boolean value) throws Exception {
		Field f = DigRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void invokeLoadPreset(DigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(DigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeLoadRanking(DigRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(DigRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = DigRaceMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
