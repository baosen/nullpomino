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
 * Pins the headless slice of {@link DigChallengeMode}: registry surface,
 * playerInit defaults including the garbage bookkeeping that is unique
 * to this mode, the per-goal-type ranking I/O contract, the
 * realtime / level-based setSpeed branches, and the loadSetting /
 * saveSetting round-trip under the 'digchallenge.*' prefix.
 */
class DigChallengeModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("DIG CHALLENGE", new DigChallengeMode().getName());
	}

	@Test
	void playerInitInstallsFreshGarbageStateAndAllocatesRankingArrays() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "lastbonusscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "lastcombo"));
		assertEquals(0, readInt(mode, "lastpiece"));
		assertEquals(false, readBoolean(mode, "lastb2b"));

		// Garbage bookkeeping unique to DIG CHALLENGE.
		assertEquals(-1, readInt(mode, "garbageHole"),
				"garbageHole must default to -1 so the first level chooses a fresh column");
		assertEquals(0, readInt(mode, "garbageTimer"));
		assertEquals(0, readInt(mode, "garbageTotal"));
		assertEquals(0, readInt(mode, "garbageNextLevelLines"));
		assertEquals(0, readInt(mode, "garbagePending"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays dimensioned [GOALTYPE_MAX=2][RANKING_MAX=10].
		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(2, rankingScore.length);
		assertEquals(10, rankingScore[0].length);

		assertEquals(GameEngine.FRAME_COLOR_GREEN, engine.framecolor);
		assertEquals(1, engine.statistics.levelDispAdd,
				"DIG CHALLENGE displays 1-indexed levels, so levelDispAdd is 1");
	}

	@Test
	void setSpeedRealtimeBranchUsesGravityZeroAndSixtyDenominator() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 1); // GOALTYPE_REALTIME
		engine.statistics.level = 0;

		mode.setSpeed(engine);

		assertEquals(0, engine.speed.gravity,
				"realtime mode disables natural gravity (0/60 = no falling)");
		assertEquals(60, engine.speed.denominator);
	}

	@Test
	void setSpeedNormalBranchReadsGravityTableAndClampsLevelToBounds() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "goaltype", 0); // GOALTYPE_NORMAL

		// Level 0 → slowest classic step.
		engine.statistics.level = 0;
		mode.setSpeed(engine);
		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);

		// Above-bounds clamps to last cell (-1 sentinel = 20G).
		engine.statistics.level = 9999;
		mode.setSpeed(engine);
		assertEquals(-1, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);

		// Negative level clamps to row 0.
		engine.statistics.level = -50;
		mode.setSpeed(engine);
		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);
	}

	@Test
	void loadRankingAppliesZeroDefaultsForMissingKeys() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		for(int goal = 0; goal < rankingScore.length; goal++) {
			for(int slot = 0; slot < rankingScore[goal].length; slot++) {
				assertEquals(0, rankingScore[goal][slot]);
				assertEquals(0, rankingLines[goal][slot]);
				assertEquals(0, rankingTime[goal][slot]);
			}
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripPerGoalTypeBucket() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		for(int goal = 0; goal < rankingScore.length; goal++) {
			for(int slot = 0; slot < rankingScore[goal].length; slot++) {
				rankingScore[goal][slot] = 9000 + goal * 100 + slot;
				rankingLines[goal][slot] = 30 + slot;
				rankingTime[goal][slot] = 5000 + slot;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(9000, prop.getProperty("digchallenge.ranking.Standard.0.score.0", -1));
		assertEquals(30, prop.getProperty("digchallenge.ranking.Standard.0.lines.0", -1));
		assertEquals(5001, prop.getProperty("digchallenge.ranking.Standard.0.time.1", -1));

		DigChallengeMode dest = new DigChallengeMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destScore = (int[][]) readField(dest, "rankingScore");
		int[][] destLines = (int[][]) readField(dest, "rankingLines");
		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		for(int goal = 0; goal < destScore.length; goal++) {
			assertArrayEquals(rankingScore[goal], destScore[goal]);
			assertArrayEquals(rankingLines[goal], destLines[goal]);
			assertArrayEquals(rankingTime[goal], destTime[goal]);
		}
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		// Need to set owner so loadSetting can write to engine[0].speed.das.
		mode.playerInit(engine, 0);

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "goaltype"));
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(0, readInt(mode, "bgmno"));
		assertEquals(2, readInt(mode, "tspinEnableType"),
				"DIG CHALLENGE defaults T-spin to ALL-SPIN (type 2)");
		assertEquals(true, readBoolean(mode, "enableTSpinKick"));
		assertEquals(0, readInt(mode, "spinCheckType"));
		assertEquals(false, readBoolean(mode, "tspinEnableEZ"));
		assertEquals(true, readBoolean(mode, "enableB2B"));
		assertEquals(true, readBoolean(mode, "enableCombo"));
		assertEquals(11, engine.speed.das,
				"DIG CHALLENGE defaults DAS to 11 (faster than the engine default)");
		assertEquals(0, readInt(mode, "version"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderDigChallengePrefix() throws Exception {
		DigChallengeMode source = new DigChallengeMode();
		GameEngine sourceEngine = freshEngine(source);
		source.playerInit(sourceEngine, 0);
		setInt(source, "goaltype", 1);
		setInt(source, "startlevel", 5);
		setInt(source, "bgmno", 3);
		setInt(source, "tspinEnableType", 1);
		setBoolean(source, "enableTSpinKick", false);
		setBoolean(source, "tspinEnableEZ", true);
		setBoolean(source, "enableB2B", false);
		setBoolean(source, "enableCombo", false);
		sourceEngine.speed.das = 7;
		setInt(source, "version", 2);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(1, prop.getProperty("digchallenge.goaltype", -1));
		assertEquals(5, prop.getProperty("digchallenge.startlevel", -1));
		assertEquals(3, prop.getProperty("digchallenge.bgmno", -1));
		assertEquals(7, prop.getProperty("digchallenge.das", -1));
		assertEquals(2, prop.getProperty("digchallenge.version", -1));
		assertEquals(false, prop.getProperty("digchallenge.enableB2B", true));

		DigChallengeMode dest = new DigChallengeMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);
		assertEquals(1, readInt(dest, "goaltype"));
		assertEquals(5, readInt(dest, "startlevel"));
		assertEquals(3, readInt(dest, "bgmno"));
		assertEquals(1, readInt(dest, "tspinEnableType"));
		assertEquals(false, readBoolean(dest, "enableTSpinKick"));
		assertEquals(true, readBoolean(dest, "tspinEnableEZ"));
		assertEquals(false, readBoolean(dest, "enableB2B"));
		assertEquals(false, readBoolean(dest, "enableCombo"));
		assertEquals(7, destEngine.speed.das);
		assertEquals(2, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(DigChallengeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(DigChallengeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(DigChallengeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(DigChallengeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(DigChallengeMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(DigChallengeMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeLoadRanking(DigChallengeMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = DigChallengeMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(DigChallengeMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = DigChallengeMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(DigChallengeMode mode, CustomProperties prop) throws Exception {
		Method m = DigChallengeMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(DigChallengeMode mode, CustomProperties prop) throws Exception {
		Method m = DigChallengeMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
