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
 * Pins the headless slice of {@link ComboRaceMode}: the registry-facing
 * surface, the preset round-trip with the combo-shape extras
 * (shapetype, comboWidth, comboColumn, ceilingAdjust, spawnAboveField),
 * the per-goal-type ranking I/O, and the playerInit defaults. Settings
 * UI, render path, and net pipeline stay out of scope.
 */
class ComboRaceModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("COMBO RACE", new ComboRaceMode().getName());
	}

	@Test
	void loadPresetReadsAllSpeedComboShapeAndGoalOptions() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("comborace.gravity.3", 64);
		prop.setProperty("comborace.denominator.3", 256);
		prop.setProperty("comborace.are.3", 30);
		prop.setProperty("comborace.areLine.3", 25);
		prop.setProperty("comborace.lineDelay.3", 40);
		prop.setProperty("comborace.lockDelay.3", 30);
		prop.setProperty("comborace.das.3", 14);
		prop.setProperty("comborace.bgmno.3", 4);
		prop.setProperty("comborace.big.3", true);
		prop.setProperty("comborace.goaltype.3", 2);
		prop.setProperty("comborace.shapetype.3", 0);
		prop.setProperty("comborace.comboWidth.3", 6);
		prop.setProperty("comborace.comboColumn.3", 2);
		prop.setProperty("comborace.ceilingAdjust.3", 4);
		prop.setProperty("comborace.spawnAboveField.3", false);

		invokeLoadPreset(mode, engine, prop, 3);

		assertEquals(64, engine.speed.gravity);
		assertEquals(40, engine.speed.lineDelay);
		assertEquals(4, readInt(mode, "bgmno"));
		assertEquals(true, readBoolean(mode, "big"));
		assertEquals(2, readInt(mode, "goaltype"));
		assertEquals(0, readInt(mode, "shapetype"));
		assertEquals(6, readInt(mode, "comboWidth"));
		assertEquals(2, readInt(mode, "comboColumn"));
		assertEquals(4, readInt(mode, "ceilingAdjust"));
		assertEquals(false, readBoolean(mode, "spawnAboveField"));
	}

	@Test
	void loadPresetAppliesDocumentedDefaultsForMissingPresetEntries() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
		assertEquals(0, readInt(mode, "bgmno"));
		assertEquals(false, readBoolean(mode, "big"));
		// goaltype defaults to 40-line preset.
		assertEquals(1, readInt(mode, "goaltype"));
		// Default combo shape lives under the 4-wide column-4 ladder.
		assertEquals(1, readInt(mode, "shapetype"));
		assertEquals(4, readInt(mode, "comboWidth"));
		assertEquals(4, readInt(mode, "comboColumn"));
		assertEquals(-2, readInt(mode, "ceilingAdjust"),
				"ceilingAdjust defaults to -2 to give the combo column overhead room");
		assertEquals(true, readBoolean(mode, "spawnAboveField"));
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderComboRacePrefix() throws Exception {
		ComboRaceMode source = new ComboRaceMode();
		GameEngine engine = freshEngine(source);
		engine.speed.gravity = 99;
		engine.speed.lockDelay = 25;
		setInt(source, "bgmno", 7);
		setBoolean(source, "big", true);
		setInt(source, "goaltype", 0);
		setInt(source, "shapetype", 0);
		setInt(source, "comboWidth", 8);
		setInt(source, "comboColumn", 1);
		setInt(source, "ceilingAdjust", 5);
		setBoolean(source, "spawnAboveField", false);

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, engine, prop, 1);

		assertEquals(99, prop.getProperty("comborace.gravity.1", -1));
		assertEquals(true, prop.getProperty("comborace.big.1", false));
		assertEquals(8, prop.getProperty("comborace.comboWidth.1", -1));
		assertEquals(false, prop.getProperty("comborace.spawnAboveField.1", true));

		ComboRaceMode dest = new ComboRaceMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1);

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(7, readInt(dest, "bgmno"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(0, readInt(dest, "goaltype"));
		assertEquals(0, readInt(dest, "shapetype"));
		assertEquals(8, readInt(dest, "comboWidth"));
		assertEquals(1, readInt(dest, "comboColumn"));
		assertEquals(5, readInt(dest, "ceilingAdjust"));
		assertEquals(false, readBoolean(dest, "spawnAboveField"));
	}

	@Test
	void loadRankingFillsWithDocumentedDefaultsWhenPropFileIsEmpty() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingCombo = (int[][]) readField(mode, "rankingCombo");
		assertNotNull(rankingTime);
		assertNotNull(rankingCombo);
		// time defaults to -1 (the "unranked" sentinel), max-combo to 0.
		for(int goal = 0; goal < rankingTime.length; goal++) {
			for(int slot = 0; slot < rankingTime[goal].length; slot++) {
				assertEquals(-1, rankingTime[goal][slot]);
				assertEquals(0, rankingCombo[goal][slot]);
			}
		}
	}

	@Test
	void loadRankingAndSaveRankingRoundTripPerGoalTypeBucket() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingCombo = (int[][]) readField(mode, "rankingCombo");
		// Stamp every (goalType, slot) cell with a unique value.
		for(int goal = 0; goal < rankingTime.length; goal++) {
			for(int slot = 0; slot < rankingTime[goal].length; slot++) {
				rankingTime[goal][slot] = 5000 + goal * 100 + slot;
				rankingCombo[goal][slot] = 30 + slot;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(5000, prop.getProperty("comborace.ranking.Standard.0.time.0", -1));
		assertEquals(31, prop.getProperty("comborace.ranking.Standard.0.maxcombo.1", -1));

		ComboRaceMode dest = new ComboRaceMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		int[][] destCombo = (int[][]) readField(dest, "rankingCombo");
		for(int goal = 0; goal < destTime.length; goal++) {
			assertArrayEquals(rankingTime[goal], destTime[goal]);
			assertArrayEquals(rankingCombo[goal], destCombo[goal]);
		}
	}

	@Test
	void playerInitInstallsFreshComboBookkeepingAndAllocatesRankingArrays() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "lastcombo"));
		assertEquals(0, readInt(mode, "lastpiece"));
		assertEquals(false, readBoolean(mode, "lastb2b"));

		// rankingTime and rankingCombo are dimensioned [GOAL_TABLE.length][RANKING_MAX].
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		assertNotNull(rankingTime);
		assertEquals(4, rankingTime.length, "GOAL_TABLE has 4 entries: 20, 40, 100, -1");
		assertEquals(10, rankingTime[0].length, "RANKING_MAX is 10");
	}

	private static GameEngine freshEngine(ComboRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(ComboRaceMode mode, String name) throws Exception {
		Field f = ComboRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ComboRaceMode mode, String name) throws Exception {
		Field f = ComboRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(ComboRaceMode mode, String name) throws Exception {
		Field f = ComboRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(ComboRaceMode mode, String name, int value) throws Exception {
		Field f = ComboRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ComboRaceMode mode, String name, boolean value) throws Exception {
		Field f = ComboRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void invokeLoadPreset(ComboRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(ComboRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeLoadRanking(ComboRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(ComboRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
