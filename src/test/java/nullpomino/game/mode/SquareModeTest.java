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
 * Pins the headless slice of {@link SquareMode}: registry surface,
 * playerInit defaults, the per-gametype ranking I/O, and the
 * loadSetting / saveSetting round-trip including the v0 → v1
 * grayoutEnable boolean → tri-state migration.
 */
class SquareModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("SQUARE", new SquareMode().getName());
	}

	@Test
	void playerInitInstallsFreshSquareStateAndAllocatesRankingArrays() throws Exception {
		SquareMode mode = new SquareMode();
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
		assertEquals(0, readInt(mode, "squares"));
		// outlinetype / tspinEnableType / grayoutEnable are seeded by
		// playerInit and then potentially overwritten by loadSetting if
		// the cached modeConfig has matching keys; outlinetype lands in
		// [0, 2], tspinEnableType in [0, 2], grayoutEnable in [0, 2].
		assertTrue(readInt(mode, "outlinetype") >= 0
				&& readInt(mode, "outlinetype") <= 2);
		assertTrue(readInt(mode, "tspinEnableType") >= 0
				&& readInt(mode, "tspinEnableType") <= 2);
		assertTrue(readInt(mode, "grayoutEnable") >= 0
				&& readInt(mode, "grayoutEnable") <= 2);
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at shape [RANKING_TYPE=3][RANKING_MAX=10].
		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(3, rankingScore.length);
		assertEquals(10, rankingScore[0].length);
		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "rankingSquares"));

		assertEquals(GameEngine.FRAME_COLOR_PURPLE, engine.framecolor);
	}

	@Test
	void loadRankingAppliesZeroScoreSquaresAndUnrankedTimeSentinel() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingSquares = (int[][]) readField(mode, "rankingSquares");
		for(int t = 0; t < rankingScore.length; t++) {
			for(int s = 0; s < rankingScore[t].length; s++) {
				assertEquals(0, rankingScore[t][s]);
				assertEquals(-1, rankingTime[t][s],
						"time defaults to -1 (unranked sentinel)");
				assertEquals(0, rankingSquares[t][s]);
			}
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderSquarePrefix() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingSquares = (int[][]) readField(mode, "rankingSquares");
		for(int t = 0; t < rankingScore.length; t++) {
			for(int s = 0; s < rankingScore[t].length; s++) {
				rankingScore[t][s] = 1000 + t * 100 + s;
				rankingTime[t][s] = 5000 + s;
				rankingSquares[t][s] = 30 + s;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(1000, prop.getProperty("square.ranking.Standard.0.score.0", -1));
		assertEquals(1100, prop.getProperty("square.ranking.Standard.1.score.0", -1));
		assertEquals(5001, prop.getProperty("square.ranking.Standard.0.time.1", -1));
		assertEquals(31, prop.getProperty("square.ranking.Standard.0.squares.1", -1));

		SquareMode dest = new SquareMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destScore = (int[][]) readField(dest, "rankingScore");
		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		int[][] destSquares = (int[][]) readField(dest, "rankingSquares");
		for(int t = 0; t < destScore.length; t++) {
			assertArrayEquals(rankingScore[t], destScore[t]);
			assertArrayEquals(rankingTime[t], destTime[t]);
			assertArrayEquals(rankingSquares[t], destSquares[t]);
		}
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		SquareMode mode = new SquareMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "gametype"));
		assertEquals(0, readInt(mode, "outlinetype"));
		assertEquals(2, readInt(mode, "tspinEnableType"),
				"SQUARE defaults T-spin to ALL-SPIN (type 2)");
		assertEquals(false, readBoolean(mode, "tntAvalanche"));
		// version starts at 0, so the legacy boolean grayoutEnable branch
		// fires and the missing key resolves to 0 (false → 0).
		assertEquals(0, readInt(mode, "grayoutEnable"),
				"v0 grayoutEnable resolves missing-key as boolean false → 0");
		assertEquals(0, readInt(mode, "version"));
	}

	@Test
	void loadSettingMigratesV0BooleanGrayoutToTriState() throws Exception {
		SquareMode mode = new SquareMode();
		// version starts at 0, so the v0 branch reads the grayoutEnable
		// key as a boolean and maps true → 2, false → 0.
		CustomProperties prop = new CustomProperties();
		prop.setProperty("square.grayoutEnable", true);

		invokeLoadSetting(mode, prop);

		assertEquals(2, readInt(mode, "grayoutEnable"),
				"v0 'grayoutEnable=true' must migrate to the tri-state value 2");
	}

	@Test
	void loadSettingReadsV1GrayoutAsTriState() throws Exception {
		SquareMode mode = new SquareMode();
		// Bump version to 1 so the v1 branch reads grayoutEnable as int.
		setInt(mode, "version", 1);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("square.grayoutEnable", 1);

		invokeLoadSetting(mode, prop);

		assertEquals(1, readInt(mode, "grayoutEnable"),
				"v1+ grayoutEnable is read as int (the tri-state value)");
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderSquarePrefix() throws Exception {
		SquareMode source = new SquareMode();
		setInt(source, "version", 1);
		setInt(source, "gametype", 2);
		setInt(source, "outlinetype", 1);
		setInt(source, "tspinEnableType", 0);
		setBoolean(source, "tntAvalanche", true);
		setInt(source, "grayoutEnable", 2);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(2, prop.getProperty("square.gametype", -1));
		assertEquals(1, prop.getProperty("square.outlinetype", -1));
		assertEquals(0, prop.getProperty("square.tspinEnableType", -1));
		assertEquals(true, prop.getProperty("square.tntAvalanche", false));
		assertEquals(2, prop.getProperty("square.grayoutEnable", -1));
		assertEquals(1, prop.getProperty("square.version", -1));

		SquareMode dest = new SquareMode();
		setInt(dest, "version", 1);
		invokeLoadSetting(dest, prop);
		assertEquals(2, readInt(dest, "gametype"));
		assertEquals(1, readInt(dest, "outlinetype"));
		assertEquals(0, readInt(dest, "tspinEnableType"));
		assertEquals(true, readBoolean(dest, "tntAvalanche"));
		assertEquals(2, readInt(dest, "grayoutEnable"));
		assertEquals(1, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(SquareMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(SquareMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(SquareMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(SquareMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(SquareMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(SquareMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadRanking(SquareMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = SquareMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(SquareMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = SquareMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(SquareMode mode, CustomProperties prop) throws Exception {
		Method m = SquareMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(SquareMode mode, CustomProperties prop) throws Exception {
		Method m = SquareMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
