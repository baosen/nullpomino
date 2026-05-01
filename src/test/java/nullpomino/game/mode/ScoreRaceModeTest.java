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
 * Pins the headless slice of {@link ScoreRaceMode}: the registry-facing
 * surface, the preset round-trip with T-spin / B2B / Combo extras, and
 * the per-goal-type ranking I/O including the SPL-default-from-lines
 * fallback. The settings UI, render path, and net pipeline stay out
 * of scope.
 */
class ScoreRaceModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("SCORE RACE", new ScoreRaceMode().getName());
	}

	@Test
	void loadPresetReadsSpeedAndScoringConfigurationFromTheGivenPreset() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("scorerace.gravity.4", 64);
		prop.setProperty("scorerace.denominator.4", 256);
		prop.setProperty("scorerace.are.4", 30);
		prop.setProperty("scorerace.areLine.4", 25);
		prop.setProperty("scorerace.lineDelay.4", 40);
		prop.setProperty("scorerace.lockDelay.4", 30);
		prop.setProperty("scorerace.das.4", 14);
		prop.setProperty("scorerace.bgmno.4", 4);
		prop.setProperty("scorerace.tspinEnableType.4", 2);
		prop.setProperty("scorerace.enableTSpin.4", false);
		prop.setProperty("scorerace.enableTSpinKick.4", false);
		prop.setProperty("scorerace.spinCheckType.4", 1);
		prop.setProperty("scorerace.tspinEnableEZ.4", true);
		prop.setProperty("scorerace.enableB2B.4", false);
		prop.setProperty("scorerace.enableCombo.4", false);
		prop.setProperty("scorerace.big.4", true);
		prop.setProperty("scorerace.goaltype.4", 2);

		invokeLoadPreset(mode, engine, prop, 4);

		assertEquals(64, engine.speed.gravity);
		assertEquals(2, readInt(mode, "tspinEnableType"));
		assertEquals(false, readBoolean(mode, "enableTSpin"));
		assertEquals(true, readBoolean(mode, "tspinEnableEZ"));
		assertEquals(false, readBoolean(mode, "enableB2B"));
		assertEquals(false, readBoolean(mode, "enableCombo"));
		assertEquals(true, readBoolean(mode, "big"));
		assertEquals(2, readInt(mode, "goaltype"));
	}

	@Test
	void loadPresetAppliesDocumentedDefaultsForMissingPresetEntries() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
		// T-spin/B2B/Combo all on by default; tspinEnableEZ off.
		assertEquals(1, readInt(mode, "tspinEnableType"));
		assertEquals(true, readBoolean(mode, "enableTSpin"));
		assertEquals(true, readBoolean(mode, "enableTSpinKick"));
		assertEquals(false, readBoolean(mode, "tspinEnableEZ"));
		assertEquals(true, readBoolean(mode, "enableB2B"));
		assertEquals(true, readBoolean(mode, "enableCombo"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(1, readInt(mode, "goaltype"),
				"goaltype defaults to the legacy 25000-pt preset (index 1)");
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderScoreRacePrefix() throws Exception {
		ScoreRaceMode source = new ScoreRaceMode();
		GameEngine engine = freshEngine(source);
		engine.speed.gravity = 99;
		engine.speed.lockDelay = 25;
		setInt(source, "bgmno", 7);
		setInt(source, "tspinEnableType", 0);
		setBoolean(source, "enableTSpin", false);
		setBoolean(source, "enableB2B", false);
		setBoolean(source, "big", true);
		setInt(source, "goaltype", 0);

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, engine, prop, 1);

		assertEquals(99, prop.getProperty("scorerace.gravity.1", -1));
		assertEquals(true, prop.getProperty("scorerace.big.1", false));
		assertEquals(0, prop.getProperty("scorerace.goaltype.1", -1));
		assertEquals(false, prop.getProperty("scorerace.enableTSpin.1", true));

		ScoreRaceMode dest = new ScoreRaceMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1);

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(7, readInt(dest, "bgmno"));
		assertEquals(0, readInt(dest, "tspinEnableType"));
		assertEquals(false, readBoolean(dest, "enableTSpin"));
		assertEquals(false, readBoolean(dest, "enableB2B"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(0, readInt(dest, "goaltype"));
	}

	@Test
	void loadRankingFillsWithDocumentedDefaultsWhenPropFileIsEmpty() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		double[][] rankingSPL = (double[][]) readField(mode, "rankingSPL");
		assertNotNull(rankingTime);
		assertNotNull(rankingLines);
		assertNotNull(rankingSPL);
		// time defaults to -1 (unranked sentinel), lines and SPL to 0.
		for(int goal = 0; goal < rankingTime.length; goal++) {
			for(int slot = 0; slot < rankingTime[goal].length; slot++) {
				assertEquals(-1, rankingTime[goal][slot]);
				assertEquals(0, rankingLines[goal][slot]);
				assertEquals(0.0, rankingSPL[goal][slot]);
			}
		}
	}

	@Test
	void loadRankingDerivesSPLFromLinesWhenSPLKeyIsAbsent() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Save just time + lines (no spl key) and confirm load derives SPL
		// as goal-table-score / lines.
		CustomProperties prop = new CustomProperties();
		prop.setProperty("scorerace.ranking.Standard.0.time.0", 3600);
		prop.setProperty("scorerace.ranking.Standard.0.lines.0", 50);
		// goal 0 is 10000pts, so default SPL = 10000 / 50 = 200.0.

		invokeLoadRanking(mode, prop, "Standard");

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		double[][] rankingSPL = (double[][]) readField(mode, "rankingSPL");
		assertEquals(3600, rankingTime[0][0]);
		assertEquals(50, rankingLines[0][0]);
		assertEquals(200.0, rankingSPL[0][0],
				"missing SPL key must default to GOAL_TABLE[goal] / lines");
	}

	@Test
	void loadRankingAndSaveRankingRoundTripPerGoalTypeBucket() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		double[][] rankingSPL = (double[][]) readField(mode, "rankingSPL");
		// Stamp every (goalType, slot) cell with a unique value.
		for(int goal = 0; goal < rankingTime.length; goal++) {
			for(int slot = 0; slot < rankingTime[goal].length; slot++) {
				rankingTime[goal][slot] = 5000 + goal * 100 + slot;
				rankingLines[goal][slot] = 30 + slot;
				rankingSPL[goal][slot] = 100.0 + slot;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(5000, prop.getProperty("scorerace.ranking.Standard.0.time.0", -1));
		assertEquals(31, prop.getProperty("scorerace.ranking.Standard.0.lines.1", -1));

		ScoreRaceMode dest = new ScoreRaceMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		int[][] destLines = (int[][]) readField(dest, "rankingLines");
		double[][] destSPL = (double[][]) readField(dest, "rankingSPL");
		for(int goal = 0; goal < destTime.length; goal++) {
			assertArrayEquals(rankingTime[goal], destTime[goal]);
			assertArrayEquals(rankingLines[goal], destLines[goal]);
			for(int slot = 0; slot < destSPL[goal].length; slot++) {
				assertEquals(rankingSPL[goal][slot], destSPL[goal][slot]);
			}
		}
	}

	@Test
	void playerInitInstallsFreshScoringStateAndAllocatesRankingArrays() throws Exception {
		ScoreRaceMode mode = new ScoreRaceMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "lastcombo"));
		assertEquals(false, readBoolean(mode, "lastb2b"));

		// Ranking arrays are dimensioned [GOALTYPE_MAX=3][RANKING_MAX=10].
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		assertNotNull(rankingTime);
		assertEquals(3, rankingTime.length);
		assertEquals(10, rankingTime[0].length);
	}

	private static GameEngine freshEngine(ScoreRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(ScoreRaceMode mode, String name) throws Exception {
		Field f = ScoreRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ScoreRaceMode mode, String name) throws Exception {
		Field f = ScoreRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(ScoreRaceMode mode, String name) throws Exception {
		Field f = ScoreRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(ScoreRaceMode mode, String name, int value) throws Exception {
		Field f = ScoreRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ScoreRaceMode mode, String name, boolean value) throws Exception {
		Field f = ScoreRaceMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void invokeLoadPreset(ScoreRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = ScoreRaceMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(ScoreRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = ScoreRaceMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeLoadRanking(ScoreRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = ScoreRaceMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(ScoreRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = ScoreRaceMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
