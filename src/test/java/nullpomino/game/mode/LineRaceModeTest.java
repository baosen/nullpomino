package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link LineRaceMode}: the registry-facing
 * surface and the preset / ranking property-key contracts. The
 * settings UI, render path, and net pipeline are out of scope here —
 * they need an SDL renderer or a live netplay session.
 */
class LineRaceModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByTheModeRegistry() {
		assertEquals("LINE RACE", new LineRaceMode().getName());
	}

	@Test
	void loadPresetReadsEngineSpeedFieldsAndModeOptionsFromPropFile() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("linerace.gravity.5", 64);
		prop.setProperty("linerace.denominator.5", 256);
		prop.setProperty("linerace.are.5", 30);
		prop.setProperty("linerace.areLine.5", 25);
		prop.setProperty("linerace.lineDelay.5", 40);
		prop.setProperty("linerace.lockDelay.5", 30);
		prop.setProperty("linerace.das.5", 14);
		prop.setProperty("linerace.bgmno.5", 4);
		prop.setProperty("linerace.big.5", true);
		prop.setProperty("linerace.goaltype.5", 2);

		invokeLoadPreset(mode, engine, prop, 5);

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
		LineRaceMode mode = new LineRaceMode();
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
				"goaltype defaults to the legacy 40-line preset (index 1)");
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderPlayerScopedKeys() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		engine.speed.gravity = 99;
		engine.speed.denominator = 60;
		engine.speed.are = 12;
		engine.speed.areLine = 11;
		engine.speed.lineDelay = 5;
		engine.speed.lockDelay = 25;
		engine.speed.das = 9;
		setInt(mode, "bgmno", 7);
		setBoolean(mode, "big", true);
		setInt(mode, "goaltype", 0);

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(mode, engine, prop, 1);

		assertEquals(99, prop.getProperty("linerace.gravity.1", -1));
		assertEquals(60, prop.getProperty("linerace.denominator.1", -1));
		assertEquals(true, prop.getProperty("linerace.big.1", false));
		assertEquals(0, prop.getProperty("linerace.goaltype.1", -1));

		LineRaceMode loaded = new LineRaceMode();
		GameEngine loadedEngine = freshEngine(loaded);
		invokeLoadPreset(loaded, loadedEngine, prop, 1);

		assertEquals(99, loadedEngine.speed.gravity);
		assertEquals(60, loadedEngine.speed.denominator);
		assertEquals(12, loadedEngine.speed.are);
		assertEquals(11, loadedEngine.speed.areLine);
		assertEquals(5, loadedEngine.speed.lineDelay);
		assertEquals(25, loadedEngine.speed.lockDelay);
		assertEquals(9, loadedEngine.speed.das);
		assertEquals(7, readInt(loaded, "bgmno"));
		assertEquals(true, readBoolean(loaded, "big"));
		assertEquals(0, readInt(loaded, "goaltype"));
	}

	@Test
	void loadRankingFillsWithDocumentedDefaultsWhenPropFileIsEmpty() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		// playerInit allocates the ranking arrays.
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingPiece = (int[][]) readField(mode, "rankingPiece");
		float[][] rankingPPS = (float[][]) readField(mode, "rankingPPS");
		assertNotNull(rankingTime);
		assertNotNull(rankingPiece);
		assertNotNull(rankingPPS);
		// time defaults to -1 (the "unranked" sentinel), the rest to 0.
		for(int goal = 0; goal < rankingTime.length; goal++) {
			for(int slot = 0; slot < rankingTime[goal].length; slot++) {
				assertEquals(-1, rankingTime[goal][slot]);
				assertEquals(0, rankingPiece[goal][slot]);
				assertEquals(0f, rankingPPS[goal][slot]);
			}
		}
	}

	@Test
	void loadRankingAndSaveRankingRoundTripPerGoalTypeBucket() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingPiece = (int[][]) readField(mode, "rankingPiece");
		float[][] rankingPPS = (float[][]) readField(mode, "rankingPPS");
		// Stamp every slot with a unique value so a swapped goal-type or
		// slot index would show up as a wrong number on the way back in.
		for(int goal = 0; goal < rankingTime.length; goal++) {
			for(int slot = 0; slot < rankingTime[goal].length; slot++) {
				rankingTime[goal][slot] = 1000 + goal * 100 + slot;
				rankingPiece[goal][slot] = 50 + slot;
				rankingPPS[goal][slot] = 1f + slot;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		// Spot-check the property key shape.
		assertEquals(1000, prop.getProperty("linerace.ranking.Standard.0.time.0", -1));
		assertEquals(51, prop.getProperty("linerace.ranking.Standard.0.piece.1", -1));

		LineRaceMode dest = new LineRaceMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		int[][] destPiece = (int[][]) readField(dest, "rankingPiece");
		float[][] destPPS = (float[][]) readField(dest, "rankingPPS");
		for(int goal = 0; goal < destTime.length; goal++) {
			assertArrayEquals(rankingTime[goal], destTime[goal]);
			assertArrayEquals(rankingPiece[goal], destPiece[goal]);
			for(int slot = 0; slot < destPPS[goal].length; slot++) {
				assertEquals(rankingPPS[goal][slot], destPPS[goal][slot]);
			}
		}
	}

	@Test
	void playerInitInstallsFreshMenuStateAndAllocatesRankingArrays() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "rankingPiece"));
		assertNotNull(readField(mode, "rankingPPS"));
		// goaltype must default to 1 (40-line) so the meter renders the
		// classic Line Race progress bar without options being touched.
		assertTrue(readInt(mode, "goaltype") >= 0
				&& readInt(mode, "goaltype") < 3,
				"goaltype must land in [0, 3) to index GOAL_TABLE safely");
	}

	private static GameEngine freshEngine(LineRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(LineRaceMode mode, String name) throws Exception {
		Field f = LineRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(LineRaceMode mode, String name) throws Exception {
		Field f = LineRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(LineRaceMode mode, String name) throws Exception {
		Field f = LineRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(LineRaceMode mode, String name, int value) throws Exception {
		Field f = LineRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(LineRaceMode mode, String name, boolean value) throws Exception {
		Field f = LineRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void invokeLoadPreset(LineRaceMode mode, GameEngine engine, CustomProperties prop, int preset) throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(LineRaceMode mode, GameEngine engine, CustomProperties prop, int preset) throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeLoadRanking(LineRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(LineRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
