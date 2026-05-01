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
 * Pins the headless slice of {@link AvalancheFeverMode}: registry
 * surface (name + AVALANCHE style inherited from
 * {@link Avalanche1PDummyMode}), the playerInit defaults including the
 * 3-D ranking table layout per (color count × map set × slot), and
 * the loadSetting / saveSetting round-trip under 'avalanchefever.*'.
 */
class AvalancheFeverModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("AVALANCHE 1P FEVER MARATHON (RC2)", new AvalancheFeverMode().getName());
	}

	@Test
	void getGameStyleIsAvalancheInheritedFromTheDummyParent() {
		assertEquals(GameEngine.GAMESTYLE_AVALANCHE, new AvalancheFeverMode().getGameStyle());
	}

	@Test
	void playerInitInstallsFreshFeverStateAndAllocatesRankingArrays() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(false, readBoolean(mode, "cleared"));
		assertEquals(0, readInt(mode, "boardsPlayed"));
		assertEquals(0, readInt(mode, "feverChainDisplay"));
		assertEquals(5, readInt(mode, "feverChain"),
				"feverChain defaults to 5 (the legacy starting tier)");
		assertEquals(0, readInt(mode, "xyzzy"));
		assertEquals(0, readInt(mode, "fastenable"));
		assertEquals(false, readBoolean(mode, "fastinuse"));
		assertEquals(5, readInt(mode, "previewChain"));
		assertEquals(0, readInt(mode, "previewSubset"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at shape [3 colors][FEVER_MAPS.length=5][RANKING_MAX=10].
		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		int[][][] rankingTime = (int[][][]) readField(mode, "rankingTime");
		assertNotNull(rankingScore);
		assertEquals(3, rankingScore.length);
		assertEquals(5, rankingScore[0].length);
		assertEquals(10, rankingScore[0][0].length);
		assertEquals(3, rankingTime.length);
	}

	@Test
	void loadRankingFillsScoreZeroAndTimeUnrankedSentinelWhenPropFileIsEmpty() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		int[][][] rankingTime = (int[][][]) readField(mode, "rankingTime");
		for(int c = 0; c < rankingScore.length; c++) {
			for(int m = 0; m < rankingScore[c].length; m++) {
				for(int s = 0; s < rankingScore[c][m].length; s++) {
					assertEquals(0, rankingScore[c][m][s]);
					assertEquals(-1, rankingTime[c][m][s],
							"ranking time defaults to -1 (unranked sentinel)");
				}
			}
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderAvalancheFeverPrefix() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][][] rankingScore = (int[][][]) readField(mode, "rankingScore");
		int[][][] rankingTime = (int[][][]) readField(mode, "rankingTime");
		for(int c = 0; c < rankingScore.length; c++) {
			for(int m = 0; m < rankingScore[c].length; m++) {
				for(int s = 0; s < rankingScore[c][m].length; s++) {
					rankingScore[c][m][s] = 100 + c * 1000 + m * 100 + s;
					rankingTime[c][m][s] = 5000 + s;
				}
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		// Spot-check the documented key shape including the human-friendly
		// map-set name ('Fever' is index 0 of FEVER_MAPS).
		assertEquals(100, prop.getProperty(
				"avalanchefever.ranking.Standard.3colors.Fever.score.0", -1));
		assertEquals(5000, prop.getProperty(
				"avalanchefever.ranking.Standard.3colors.Fever.time.0", -1));
		// 4 colors should land under '4colors'.
		assertEquals(1100, prop.getProperty(
				"avalanchefever.ranking.Standard.4colors.Fever.score.0", -1));

		AvalancheFeverMode dest = new AvalancheFeverMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][][] destScore = (int[][][]) readField(dest, "rankingScore");
		int[][][] destTime = (int[][][]) readField(dest, "rankingTime");
		for(int c = 0; c < destScore.length; c++) {
			for(int m = 0; m < destScore[c].length; m++) {
				assertArrayEquals(rankingScore[c][m], destScore[c][m]);
				assertArrayEquals(rankingTime[c][m], destTime[c][m]);
			}
		}
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "mapSet"));
		assertEquals(0, readInt(mode, "outlinetype"));
		assertEquals(4, readInt(mode, "numColors"),
				"numColors defaults to 4 (medium difficulty)");
		assertEquals(1, readInt(mode, "chainDisplayType"));
		assertEquals(false, readBoolean(mode, "bigDisplay"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderAvalancheFeverPrefix() throws Exception {
		AvalancheFeverMode source = new AvalancheFeverMode();
		setInt(source, "mapSet", 3);
		setInt(source, "outlinetype", 2);
		setInt(source, "numColors", 5);
		setInt(source, "chainDisplayType", 0);
		setBoolean(source, "bigDisplay", true);
		setInt(source, "version", 7);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(3, prop.getProperty("avalanchefever.gametype", -1));
		assertEquals(2, prop.getProperty("avalanchefever.outlinetype", -1));
		assertEquals(5, prop.getProperty("avalanchefever.numcolors", -1));
		assertEquals(0, prop.getProperty("avalanchefever.chainDisplayType", -1));
		assertEquals(true, prop.getProperty("avalanchefever.bigDisplay", false));
		assertEquals(7, prop.getProperty("avalanchefever.version", -1));

		AvalancheFeverMode dest = new AvalancheFeverMode();
		invokeLoadSetting(dest, prop);
		assertEquals(3, readInt(dest, "mapSet"));
		assertEquals(2, readInt(dest, "outlinetype"));
		assertEquals(5, readInt(dest, "numColors"));
		assertEquals(0, readInt(dest, "chainDisplayType"));
		assertEquals(true, readBoolean(dest, "bigDisplay"));
		assertEquals(7, readInt(dest, "version"));
	}

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
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeLoadRanking(AvalancheFeverMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(AvalancheFeverMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(AvalancheFeverMode mode, CustomProperties prop) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(AvalancheFeverMode mode, CustomProperties prop) throws Exception {
		Method m = AvalancheFeverMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
