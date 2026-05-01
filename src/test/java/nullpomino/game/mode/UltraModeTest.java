package nullpomino.game.mode;

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
 * Pins the headless slice of {@link UltraMode}: the registry-facing
 * surface, the preset round-trip with the Ultra-specific T-spin /
 * B2B / Combo extras, the 3-D ranking table I/O contract
 * ([GOALTYPE_MAX][RANKING_TYPE][RANKING_MAX]), and the playerInit
 * defaults. Settings UI, render path, and net pipeline stay out of
 * scope — they need an SDL renderer or a live netplay session.
 */
class UltraModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("ULTRA", new UltraMode().getName());
	}

	@Test
	void loadPresetReadsAllSpeedOptionsTSpinSettingsAndGoalType() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("ultra.gravity.5", 64);
		prop.setProperty("ultra.denominator.5", 256);
		prop.setProperty("ultra.are.5", 30);
		prop.setProperty("ultra.areLine.5", 25);
		prop.setProperty("ultra.lineDelay.5", 40);
		prop.setProperty("ultra.lockDelay.5", 30);
		prop.setProperty("ultra.das.5", 14);
		prop.setProperty("ultra.bgmno.5", 4);
		prop.setProperty("ultra.tspinEnableType.5", 2);
		prop.setProperty("ultra.enableTSpin.5", false);
		prop.setProperty("ultra.enableTSpinKick.5", false);
		prop.setProperty("ultra.spinCheckType.5", 1);
		prop.setProperty("ultra.tspinEnableEZ.5", true);
		prop.setProperty("ultra.enableB2B.5", false);
		prop.setProperty("ultra.enableCombo.5", false);
		prop.setProperty("ultra.big.5", true);
		prop.setProperty("ultra.goaltype.5", 4);

		invokeLoadPreset(mode, engine, prop, 5);

		assertEquals(64, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(2, readInt(mode, "tspinEnableType"));
		assertEquals(false, readBoolean(mode, "enableTSpin"));
		assertEquals(true, readBoolean(mode, "tspinEnableEZ"));
		assertEquals(false, readBoolean(mode, "enableB2B"));
		assertEquals(false, readBoolean(mode, "enableCombo"));
		assertEquals(true, readBoolean(mode, "big"));
		assertEquals(4, readInt(mode, "goaltype"));
	}

	@Test
	void loadPresetAppliesDocumentedDefaultsForMissingPresetEntries() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(0, engine.speed.are);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
		// T-spin/B2B/combo are on by default and tspinEnableEZ is off — the
		// classic Ultra preset.
		assertEquals(1, readInt(mode, "tspinEnableType"));
		assertEquals(true, readBoolean(mode, "enableTSpin"));
		assertEquals(true, readBoolean(mode, "enableTSpinKick"));
		assertEquals(false, readBoolean(mode, "tspinEnableEZ"));
		assertEquals(true, readBoolean(mode, "enableB2B"));
		assertEquals(true, readBoolean(mode, "enableCombo"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(2, readInt(mode, "goaltype"),
				"goaltype defaults to the legacy 3-minute preset (index 2)");
	}

	@Test
	void savePresetAndLoadPresetRoundTripUnderUltraPrefix() throws Exception {
		UltraMode source = new UltraMode();
		GameEngine engine = freshEngine(source);
		engine.speed.gravity = 99;
		engine.speed.lockDelay = 25;
		setInt(source, "bgmno", 7);
		setInt(source, "tspinEnableType", 0);
		setBoolean(source, "enableTSpin", false);
		setBoolean(source, "enableB2B", false);
		setBoolean(source, "big", true);
		setInt(source, "goaltype", 1);

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(source, engine, prop, 1);

		assertEquals(99, prop.getProperty("ultra.gravity.1", -1));
		assertEquals(true, prop.getProperty("ultra.big.1", false));
		assertEquals(1, prop.getProperty("ultra.goaltype.1", -1));
		assertEquals(false, prop.getProperty("ultra.enableTSpin.1", true));

		UltraMode dest = new UltraMode();
		GameEngine destEngine = freshEngine(dest);
		invokeLoadPreset(dest, destEngine, prop, 1);

		assertEquals(99, destEngine.speed.gravity);
		assertEquals(25, destEngine.speed.lockDelay);
		assertEquals(7, readInt(dest, "bgmno"));
		assertEquals(0, readInt(dest, "tspinEnableType"));
		assertEquals(false, readBoolean(dest, "enableTSpin"));
		assertEquals(false, readBoolean(dest, "enableB2B"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(1, readInt(dest, "goaltype"));
	}

	@Test
	void loadRankingFillsWithZeroDefaultsWhenPropFileIsEmpty() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		int[][][] rankingLines = (int[][][]) readField(mode, "rankingLines");
		assertNotNull(rankingScore);
		assertNotNull(rankingLines);
		// Both rankings default to zero — Ultra's score / lines tables do not
		// have an unranked sentinel, so a missing file leaves the table at all
		// zeros.
		for(int goal = 0; goal < rankingScore.length; goal++) {
			for(int type = 0; type < rankingScore[goal].length; type++) {
				for(int slot = 0; slot < rankingScore[goal][type].length; slot++) {
					assertEquals(0, rankingScore[goal][type][slot]);
					assertEquals(0, rankingLines[goal][type][slot]);
				}
			}
		}
	}

	@Test
	void loadRankingAndSaveRankingRoundTripPerGoalTypePerRankingTypeAndSlot() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		int[][][] rankingLines = (int[][][]) readField(mode, "rankingLines");
		// Stamp every (goalType, rankingType, slot) cell with a unique value
		// so a swapped index would surface as a wrong number on load.
		for(int goal = 0; goal < rankingScore.length; goal++) {
			for(int type = 0; type < rankingScore[goal].length; type++) {
				for(int slot = 0; slot < rankingScore[goal][type].length; slot++) {
					rankingScore[goal][type][slot] = 10000 + goal * 1000 + type * 100 + slot;
					rankingLines[goal][type][slot] = 10 + slot;
				}
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(10000, prop.getProperty("ultra.ranking.Standard.0.0.score.0", -1));
		assertEquals(10101, prop.getProperty("ultra.ranking.Standard.0.1.score.1", -1));
		assertEquals(11, prop.getProperty("ultra.ranking.Standard.0.0.lines.1", -1));

		UltraMode dest = new UltraMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][][] destScore = (int[][][]) readField(dest, "rankingScore");
		int[][][] destLines = (int[][][]) readField(dest, "rankingLines");
		for(int goal = 0; goal < destScore.length; goal++) {
			for(int type = 0; type < destScore[goal].length; type++) {
				for(int slot = 0; slot < destScore[goal][type].length; slot++) {
					assertEquals(rankingScore[goal][type][slot], destScore[goal][type][slot]);
					assertEquals(rankingLines[goal][type][slot], destLines[goal][type][slot]);
				}
			}
		}
	}

	@Test
	void playerInitInstallsFreshMenuStateAndAllocatesThreeDimensionalRankingTables() throws Exception {
		UltraMode mode = new UltraMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertNotNull(readField(mode, "rankingScore"));
		assertNotNull(readField(mode, "rankingLines"));
		// rankingScore and rankingLines are dimensioned
		// [GOALTYPE_MAX=5][RANKING_TYPE=2][RANKING_MAX=5].
		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		assertEquals(5, rankingScore.length);
		assertEquals(2, rankingScore[0].length);
		assertEquals(5, rankingScore[0][0].length);

		// rankingRank is per-rankingType so it can hold separate rank slots
		// for SPRINT vs ULTRA scoring.
		int[] rankingRank = (int[]) readField(mode, "rankingRank");
		assertNotNull(rankingRank);
		assertEquals(2, rankingRank.length);
	}

	private static GameEngine freshEngine(UltraMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(UltraMode mode, String name) throws Exception {
		Field f = UltraMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(UltraMode mode, String name) throws Exception {
		Field f = UltraMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(UltraMode mode, String name) throws Exception {
		Field f = UltraMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(UltraMode mode, String name, int value) throws Exception {
		Field f = UltraMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(UltraMode mode, String name, boolean value) throws Exception {
		Field f = UltraMode.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void invokeLoadPreset(UltraMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = UltraMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(UltraMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = UltraMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeLoadRanking(UltraMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = UltraMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(UltraMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = UltraMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
