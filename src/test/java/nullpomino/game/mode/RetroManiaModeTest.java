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
 * Pins the headless slice of {@link RetroManiaMode}: registry surface,
 * the playerInit defaults including the retro engine wiring (T-spin /
 * B2B / Combo all off, fixed legacy speed values, gray frame), the
 * per-gametype ranking I/O with score / lines clamps, and the
 * loadSetting / saveSetting round-trip under 'retromania.*'.
 */
class RetroManiaModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("RETRO MANIA", new RetroManiaMode().getName());
	}

	@Test
	void playerInitInstallsRetroEngineDefaultsAndAllocatesRankingArrays() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
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
		assertEquals(0, readInt(mode, "levelTimer"));
		assertEquals(0, readInt(mode, "linesAfterLastLevelUp"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at shape [RANKING_TYPE=4][RANKING_MAX=10].
		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(4, rankingScore.length);
		assertEquals(10, rankingScore[0].length);
		assertNotNull(readField(mode, "rankingLines"));
		assertNotNull(readField(mode, "rankingTime"));

		// Retro engine wiring.
		assertEquals(false, engine.tspinEnable);
		assertEquals(false, engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertEquals(false, engine.bighalf);
		assertEquals(false, engine.bigmove);
		assertEquals(30, engine.speed.are);
		assertEquals(30, engine.speed.areLine);
		assertEquals(42, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(20, engine.speed.das);
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
	}

	@Test
	void loadRankingFillsZeroDefaultsForAllGameTypes() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		for(int t = 0; t < rankingScore.length; t++) {
			for(int s = 0; s < rankingScore[t].length; s++) {
				assertEquals(0, rankingScore[t][s]);
				assertEquals(0, rankingLines[t][s]);
				assertEquals(0, rankingTime[t][s]);
			}
		}
	}

	@Test
	void loadRankingClampsScoreAndLinesAtMaxValues() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Plant a value above MAX_SCORE / MAX_LINES so the clamp branch
		// fires.
		CustomProperties prop = new CustomProperties();
		prop.setProperty("retromania.ranking.Standard.0.score.0", 9999999);
		prop.setProperty("retromania.ranking.Standard.0.lines.0", 5000);
		prop.setProperty("retromania.ranking.Standard.0.time.0", 12345);

		invokeLoadRanking(mode, prop, "Standard");

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		assertEquals(999999, rankingScore[0][0],
				"score above MAX_SCORE must clamp to 999999");
		assertEquals(999, rankingLines[0][0],
				"lines above MAX_LINES must clamp to 999");
		assertEquals(12345, rankingTime[0][0]);
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderRetroManiaPrefix() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		for(int t = 0; t < rankingScore.length; t++) {
			for(int s = 0; s < rankingScore[t].length; s++) {
				rankingScore[t][s] = 1000 + t * 100 + s;
				rankingLines[t][s] = 30 + s;
				rankingTime[t][s] = 5000 + s;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(1000, prop.getProperty("retromania.ranking.Standard.0.score.0", -1));
		assertEquals(1100, prop.getProperty("retromania.ranking.Standard.1.score.0", -1));
		assertEquals(31, prop.getProperty("retromania.ranking.Standard.0.lines.1", -1));

		RetroManiaMode dest = new RetroManiaMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destScore = (int[][]) readField(dest, "rankingScore");
		int[][] destLines = (int[][]) readField(dest, "rankingLines");
		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		for(int t = 0; t < destScore.length; t++) {
			assertArrayEquals(rankingScore[t], destScore[t]);
			assertArrayEquals(rankingLines[t], destLines[t]);
			assertArrayEquals(rankingTime[t], destTime[t]);
		}
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(0, readInt(mode, "gametype"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(false, readBoolean(mode, "poweron"));
		assertEquals(0, readInt(mode, "version"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderRetroManiaPrefix() throws Exception {
		RetroManiaMode source = new RetroManiaMode();
		setInt(source, "startlevel", 9);
		setInt(source, "gametype", 3);
		setBoolean(source, "big", true);
		setBoolean(source, "poweron", true);
		setInt(source, "version", 2);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(9, prop.getProperty("retromania.startlevel", -1));
		assertEquals(3, prop.getProperty("retromania.gametype", -1));
		assertEquals(true, prop.getProperty("retromania.big", false));
		assertEquals(true, prop.getProperty("retromania.poweron", false));
		assertEquals(2, prop.getProperty("retromania.version", -1));

		RetroManiaMode dest = new RetroManiaMode();
		invokeLoadSetting(dest, prop);
		assertEquals(9, readInt(dest, "startlevel"));
		assertEquals(3, readInt(dest, "gametype"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(true, readBoolean(dest, "poweron"));
		assertEquals(2, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(RetroManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(RetroManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(RetroManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(RetroManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(RetroManiaMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(RetroManiaMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadRanking(RetroManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(RetroManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(RetroManiaMode mode, CustomProperties prop) throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(RetroManiaMode mode, CustomProperties prop) throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
