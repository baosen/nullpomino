package nullpomino.game.mode;

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
 * Pins the headless slice of {@link AvalancheMode}: registry surface
 * (AVALANCHE style inherited from Avalanche1PDummyMode), the playerInit
 * defaults including the 4-D ranking allocation per (scoretype × color
 * count × game type × slot), and the loadSetting / saveSetting
 * round-trip under 'avalanche.*'.
 */
class AvalancheModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("AVALANCHE 1P (RC2)", new AvalancheMode().getName());
	}

	@Test
	void getGameStyleIsAvalancheInheritedFromTheDummyParent() {
		assertEquals(GameEngine.GAMESTYLE_AVALANCHE, new AvalancheMode().getGameStyle());
	}

	@Test
	void getPlayersDefaultsToOneFromAbstractMode() {
		assertEquals(1, new AvalancheMode().getPlayers());
	}

	@Test
	void playerInitInstallsFreshAvalancheStateAndAllocatesRankingArrays() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertTrue(readBoolean(mode, "showChains"),
				"showChains defaults to true (the legacy on-screen chain counter)");
		assertEquals(0, readInt(mode, "scoreType"));
		assertEquals(0, readInt(mode, "sprintTarget"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at shape [SCORETYPE_MAX=2][3 colors][RANKING_TYPE=7][RANKING_MAX=10].
		int[][][][] rankingScore = (int[][][][]) readField(mode, "rankingScore");
		int[][][][] rankingTime = (int[][][][]) readField(mode, "rankingTime");
		assertNotNull(rankingScore);
		assertEquals(2, rankingScore.length, "SCORETYPE_MAX = 2");
		assertEquals(3, rankingScore[0].length, "colors 3..5 → 3 buckets");
		assertEquals(7, rankingScore[0][0].length, "RANKING_TYPE = 7");
		assertEquals(10, rankingScore[0][0][0].length, "RANKING_MAX = 10");
		assertEquals(2, rankingTime.length);
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		AvalancheMode mode = new AvalancheMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "gametype"));
		assertEquals(0, readInt(mode, "sprintTarget"));
		assertEquals(0, readInt(mode, "scoreType"));
		assertEquals(0, readInt(mode, "outlinetype"));
		assertEquals(4, readInt(mode, "numColors"),
				"numColors defaults to 4 (medium difficulty)");
		assertEquals(0, readInt(mode, "version"));
		assertEquals(false, readBoolean(mode, "dangerColumnDouble"));
		assertEquals(false, readBoolean(mode, "dangerColumnShowX"));
		assertEquals(true, readBoolean(mode, "showChains"),
				"showChains defaults to true so the on-screen chain counter renders");
		assertEquals(false, readBoolean(mode, "cascadeSlow"));
		assertEquals(false, readBoolean(mode, "bigDisplay"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderAvalanchePrefix() throws Exception {
		AvalancheMode source = new AvalancheMode();
		setInt(source, "gametype", 2);
		setInt(source, "sprintTarget", 3);
		setInt(source, "scoreType", 1);
		setInt(source, "outlinetype", 2);
		setInt(source, "numColors", 5);
		setInt(source, "version", 6);
		setBoolean(source, "dangerColumnDouble", true);
		setBoolean(source, "dangerColumnShowX", true);
		setBoolean(source, "showChains", false);
		setBoolean(source, "cascadeSlow", true);
		setBoolean(source, "bigDisplay", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(2, prop.getProperty("avalanche.gametype", -1));
		assertEquals(3, prop.getProperty("avalanche.sprintTarget", -1));
		assertEquals(1, prop.getProperty("avalanche.scoreType", -1));
		assertEquals(5, prop.getProperty("avalanche.numcolors", -1));
		assertEquals(6, prop.getProperty("avalanche.version", -1));
		assertEquals(true, prop.getProperty("avalanche.dangerColumnDouble", false));
		assertEquals(false, prop.getProperty("avalanche.showChains", true));

		AvalancheMode dest = new AvalancheMode();
		invokeLoadSetting(dest, prop);
		assertEquals(2, readInt(dest, "gametype"));
		assertEquals(3, readInt(dest, "sprintTarget"));
		assertEquals(1, readInt(dest, "scoreType"));
		assertEquals(2, readInt(dest, "outlinetype"));
		assertEquals(5, readInt(dest, "numColors"));
		assertEquals(6, readInt(dest, "version"));
		assertEquals(true, readBoolean(dest, "dangerColumnDouble"));
		assertEquals(true, readBoolean(dest, "dangerColumnShowX"));
		assertEquals(false, readBoolean(dest, "showChains"));
		assertEquals(true, readBoolean(dest, "cascadeSlow"));
		assertEquals(true, readBoolean(dest, "bigDisplay"));
	}

	@Test
	void backToBackPlayerInitKeepsRankingShapeStable() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		int[][][][] firstScore = (int[][][][]) readField(mode, "rankingScore");
		mode.playerInit(engine, 0);
		int[][][][] secondScore = (int[][][][]) readField(mode, "rankingScore");

		assertEquals(firstScore.length, secondScore.length);
		assertEquals(firstScore[0][0][0].length, secondScore[0][0][0].length);
	}

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
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
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
}
