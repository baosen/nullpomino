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
 * Pins the headless slice of {@link PhysicianMode}: the registry
 * surface, the playerInit defaults including the cascade-mode wiring
 * (line-color clear / cascade gravity / random block color / 4-block
 * minimum), the per-rule ranking I/O, and the loadSetting / saveSetting
 * round-trip under the 'physician.*' prefix.
 */
class PhysicianModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("PHYSICIAN (RC1)", new PhysicianMode().getName());
	}

	@Test
	void getGameStyleReturnsPhysicianStyleSoTheRendererPicksTheRightField() {
		assertEquals(GameEngine.GAMESTYLE_PHYSICIAN, new PhysicianMode().getGameStyle());
	}

	@Test
	void playerInitInstallsFreshCascadeWiringAndAllocatesRankingArrays() throws Exception {
		PhysicianMode mode = new PhysicianMode();
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
		assertEquals(0, readInt(mode, "gemsClearedChainTotal"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays dimensioned [RANKING_MAX=10].
		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		assertNotNull(rankingScore);
		assertEquals(10, rankingScore.length);
		assertEquals(10, rankingTime.length);

		// Frame is purple; the cascade-style engine wiring takes effect:
		// line-color clear, cascade gravity, 4-block minimum, random
		// colours, connect blocks, gemSameColor on, cascade delay 18.
		assertEquals(GameEngine.FRAME_COLOR_PURPLE, engine.framecolor);
		assertEquals(GameEngine.ClearType.LINE_COLOR, engine.clearMode);
		assertEquals(false, engine.garbageColorClear);
		assertEquals(4, engine.colorClearSize);
		assertEquals(GameEngine.LineGravity.CASCADE, engine.lineGravityType);
		assertTrue(engine.randomBlockColor);
		assertTrue(engine.connectBlocks);
		assertEquals(18, engine.cascadeDelay);
		assertTrue(engine.gemSameColor);
	}

	@Test
	void loadRankingFillsZeroScoreAndUnrankedTimeSentinelWhenPropFileIsEmpty() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		for(int i = 0; i < rankingScore.length; i++) {
			assertEquals(0, rankingScore[i]);
			assertEquals(-1, rankingTime[i],
					"ranking time defaults to -1 (unranked sentinel)");
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderPhysicianPrefix() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		for(int i = 0; i < rankingScore.length; i++) {
			rankingScore[i] = 1000 + i;
			rankingTime[i] = 5000 + i;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(1000, prop.getProperty("physician.ranking.Standard.score.0", -1));
		assertEquals(1009, prop.getProperty("physician.ranking.Standard.score.9", -1));
		assertEquals(5000, prop.getProperty("physician.ranking.Standard.time.0", -1));

		PhysicianMode dest = new PhysicianMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		assertArrayEquals(rankingScore, (int[]) readField(dest, "rankingScore"));
		assertArrayEquals(rankingTime, (int[]) readField(dest, "rankingTime"));
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		PhysicianMode mode = new PhysicianMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(40, readInt(mode, "hoverBlocks"),
				"hoverBlocks defaults to 40 (the legacy stack height)");
		assertEquals(1, readInt(mode, "speed"),
				"speed defaults to 1 (medium tempo)");
		assertEquals(0, readInt(mode, "version"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderPhysicianPrefix() throws Exception {
		PhysicianMode source = new PhysicianMode();
		setInt(source, "hoverBlocks", 60);
		setInt(source, "speed", 3);
		setInt(source, "version", 5);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(60, prop.getProperty("physician.hoverBlocks", -1));
		assertEquals(3, prop.getProperty("physician.speed", -1));
		assertEquals(5, prop.getProperty("physician.version", -1));

		PhysicianMode dest = new PhysicianMode();
		invokeLoadSetting(dest, prop);
		assertEquals(60, readInt(dest, "hoverBlocks"));
		assertEquals(3, readInt(dest, "speed"));
		assertEquals(5, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(PhysicianMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(PhysicianMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static Object readField(PhysicianMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(PhysicianMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
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

	private static void invokeLoadRanking(PhysicianMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(PhysicianMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(PhysicianMode mode, CustomProperties prop) throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(PhysicianMode mode, CustomProperties prop) throws Exception {
		Method m = PhysicianMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
