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
 * Pins the headless slice of {@link RetroMasteryMode}: registry surface,
 * the playerInit defaults including the retro engine wiring (T-spin /
 * B2B off, combo disabled, big-half + big-move on, fixed legacy ARE /
 * areLine / DAS), the per-gametype ranking I/O, and the loadSetting /
 * saveSetting round-trip under 'retromastery.*'.
 */
class RetroMasteryModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("RETRO MASTERY", new RetroMasteryMode().getName());
	}

	@Test
	void playerInitInstallsRetroEngineDefaultsAndAllocatesRankingArrays() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
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
		assertEquals(0, readInt(mode, "softdropscore"));
		assertEquals(0, readInt(mode, "harddropscore"));
		assertEquals(0, readInt(mode, "levellines"));
		assertEquals(0, readInt(mode, "loons"));
		assertEquals(0, readInt(mode, "actions"));
		assertEquals(0f, readFloat(mode, "efficiency"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at shape [RANKING_TYPE=3][RANKING_MAX=10].
		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(3, rankingScore.length);
		assertEquals(10, rankingScore[0].length);
		assertNotNull(readField(mode, "rankingLines"));
		assertNotNull(readField(mode, "rankingLevel"));

		// Retro engine wiring with big-half / big-move ON (the distinguishing
		// feature vs RETRO MARATHON), and the legacy ARE / areLine / DAS feel.
		assertEquals(false, engine.tspinEnable);
		assertEquals(false, engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertEquals(true, engine.bighalf,
				"RETRO MASTERY runs with bighalf=true (the legacy big-block half-move)");
		assertEquals(true, engine.bigmove,
				"RETRO MASTERY runs with bigmove=true (big-block movement)");
		assertEquals(12, engine.speed.are);
		assertEquals(15, engine.speed.areLine);
		assertEquals(12, engine.speed.das);
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
	}

	@Test
	void loadRankingFillsZeroDefaultsForAllGameTypes() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingLevel = (int[][]) readField(mode, "rankingLevel");
		for(int t = 0; t < rankingScore.length; t++) {
			for(int s = 0; s < rankingScore[t].length; s++) {
				assertEquals(0, rankingScore[t][s]);
				assertEquals(0, rankingLines[t][s]);
				assertEquals(0, rankingLevel[t][s]);
			}
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderRetroMasteryPrefix() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingLevel = (int[][]) readField(mode, "rankingLevel");
		for(int t = 0; t < rankingScore.length; t++) {
			for(int s = 0; s < rankingScore[t].length; s++) {
				rankingScore[t][s] = 5000 + t * 100 + s;
				rankingLines[t][s] = 30 + s;
				rankingLevel[t][s] = 99 - s;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(5000, prop.getProperty("retromastery.ranking.Standard.0.score.0", -1));
		assertEquals(5100, prop.getProperty("retromastery.ranking.Standard.1.score.0", -1));
		assertEquals(31, prop.getProperty("retromastery.ranking.Standard.0.lines.1", -1));
		assertEquals(98, prop.getProperty("retromastery.ranking.Standard.0.level.1", -1));

		RetroMasteryMode dest = new RetroMasteryMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destScore = (int[][]) readField(dest, "rankingScore");
		int[][] destLines = (int[][]) readField(dest, "rankingLines");
		int[][] destLevel = (int[][]) readField(dest, "rankingLevel");
		for(int t = 0; t < destScore.length; t++) {
			assertArrayEquals(rankingScore[t], destScore[t]);
			assertArrayEquals(rankingLines[t], destLines[t]);
			assertArrayEquals(rankingLevel[t], destLevel[t]);
		}
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "gametype"));
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(0, readInt(mode, "version"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderRetroMasteryPrefix() throws Exception {
		RetroMasteryMode source = new RetroMasteryMode();
		setInt(source, "gametype", 2);
		setInt(source, "startlevel", 9);
		setBoolean(source, "big", true);
		setInt(source, "version", 4);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(2, prop.getProperty("retromastery.gametype", -1));
		assertEquals(9, prop.getProperty("retromastery.startlevel", -1));
		assertEquals(true, prop.getProperty("retromastery.big", false));
		assertEquals(4, prop.getProperty("retromastery.version", -1));

		RetroMasteryMode dest = new RetroMasteryMode();
		invokeLoadSetting(dest, prop);
		assertEquals(2, readInt(dest, "gametype"));
		assertEquals(9, readInt(dest, "startlevel"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(4, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(RetroMasteryMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(RetroMasteryMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static float readFloat(RetroMasteryMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getFloat(mode);
	}

	private static boolean readBoolean(RetroMasteryMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(RetroMasteryMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(RetroMasteryMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(RetroMasteryMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadRanking(RetroMasteryMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = RetroMasteryMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(RetroMasteryMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = RetroMasteryMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(RetroMasteryMode mode, CustomProperties prop) throws Exception {
		Method m = RetroMasteryMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(RetroMasteryMode mode, CustomProperties prop) throws Exception {
		Method m = RetroMasteryMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
