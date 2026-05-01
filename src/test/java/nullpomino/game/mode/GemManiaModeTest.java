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
 * Pins the headless slice of {@link GemManiaMode}: the registry surface,
 * playerInit defaults (stage / gimmick / training bookkeeping unique to
 * this mode), the per-rule ranking I/O contract over the four-array
 * stage / clearper / time / allclear table, and the loadSetting /
 * saveSetting round-trip under the 'gemmania.*' prefix.
 */
class GemManiaModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("GEM MANIA", new GemManiaMode().getName());
	}

	@Test
	void playerInitInstallsFreshStageStateAndAllocatesAllRankingArrays() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		// Stage progression bookkeeping zeroed.
		assertEquals(0, readInt(mode, "rest"));
		assertEquals(0, readInt(mode, "stage"));
		assertEquals(19, readInt(mode, "laststage"),
				"laststage defaults to MAX_STAGE_NORMAL - 1 = 19");
		assertEquals(0, readInt(mode, "trystage"));
		assertEquals(0, readInt(mode, "clearstage"));
		assertEquals(0, readInt(mode, "clearper"));
		assertEquals(false, readBoolean(mode, "clearflag"));
		assertEquals(false, readBoolean(mode, "skipflag"));
		assertEquals(0, readInt(mode, "limittimeNow"));
		assertEquals(0, readInt(mode, "limittimeStart"));
		assertEquals(0, readInt(mode, "stagetimeNow"));
		assertEquals(0, readInt(mode, "stagetimeStart"));
		assertEquals(0, readInt(mode, "cleartime"));

		// Mode-internal level controller defaults.
		assertEquals(0, readInt(mode, "gravityindex"));
		assertEquals(0, readInt(mode, "nextseclv"));
		assertEquals(0, readInt(mode, "speedlevel"));
		assertEquals(false, readBoolean(mode, "lvupflag"));

		// Time-extension counters zeroed.
		assertEquals(0, readInt(mode, "timeextendSeconds"));
		assertEquals(0, readInt(mode, "timeextendDisp"));
		assertEquals(0, readInt(mode, "timeextendStageClearSeconds"));

		// Continue / training counters.
		assertEquals(0, readInt(mode, "thisStageTotalPieceLockCount"));
		assertEquals(0, readInt(mode, "skipbuttonPressTime"));
		assertEquals(0, readInt(mode, "continueNextPieceCount"));
		assertEquals(false, readBoolean(mode, "noContinue"));
		assertEquals(0, readInt(mode, "allclear"));
		assertEquals(-1, readInt(mode, "trainingBestTime"),
				"trainingBestTime defaults to -1 so the first run beats it");

		// Section-time array dimensioned [MAX_STAGE_TOTAL=27].
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		assertNotNull(sectiontime);
		assertEquals(27, sectiontime.length);

		// Ranking arrays dimensioned [RANKING_TYPE=2][RANKING_MAX=10].
		int[][] rankingStage = (int[][]) readField(mode, "rankingStage");
		assertNotNull(rankingStage);
		assertEquals(2, rankingStage.length);
		assertEquals(10, rankingStage[0].length);
		assertNotNull(readField(mode, "rankingClearPer"));
		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "rankingAllClear"));
	}

	@Test
	void loadRankingFillsZeroDefaultsWhenPropFileIsEmpty() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingStage = (int[][]) readField(mode, "rankingStage");
		int[][] rankingClearPer = (int[][]) readField(mode, "rankingClearPer");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingAllClear = (int[][]) readField(mode, "rankingAllClear");
		for(int type = 0; type < rankingStage.length; type++) {
			for(int slot = 0; slot < rankingStage[type].length; slot++) {
				assertEquals(0, rankingStage[type][slot]);
				assertEquals(0, rankingClearPer[type][slot]);
				assertEquals(0, rankingTime[type][slot]);
				assertEquals(0, rankingAllClear[type][slot]);
			}
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripBothRankingTypes() throws Exception {
		GemManiaMode mode = new GemManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingStage = (int[][]) readField(mode, "rankingStage");
		int[][] rankingClearPer = (int[][]) readField(mode, "rankingClearPer");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingAllClear = (int[][]) readField(mode, "rankingAllClear");
		for(int type = 0; type < rankingStage.length; type++) {
			for(int slot = 0; slot < rankingStage[type].length; slot++) {
				rankingStage[type][slot] = 100 + type * 10 + slot;
				rankingClearPer[type][slot] = type * 50 + slot;
				rankingTime[type][slot] = 1000 + slot;
				rankingAllClear[type][slot] = (slot % 2 == 0) ? 1 : 0;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(100, prop.getProperty("gemmania.ranking.Standard.0.stage.0", -1));
		assertEquals(110, prop.getProperty("gemmania.ranking.Standard.1.stage.0", -1));
		assertEquals(0, prop.getProperty("gemmania.ranking.Standard.0.clearper.0", -1));
		assertEquals(1000, prop.getProperty("gemmania.ranking.Standard.0.time.0", -1));
		assertEquals(1, prop.getProperty("gemmania.ranking.Standard.0.allclear.0", -1));

		GemManiaMode dest = new GemManiaMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destStage = (int[][]) readField(dest, "rankingStage");
		int[][] destClearPer = (int[][]) readField(dest, "rankingClearPer");
		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		int[][] destAllClear = (int[][]) readField(dest, "rankingAllClear");
		for(int type = 0; type < destStage.length; type++) {
			assertArrayEquals(rankingStage[type], destStage[type]);
			assertArrayEquals(rankingClearPer[type], destClearPer[type]);
			assertArrayEquals(rankingTime[type], destTime[type]);
			assertArrayEquals(rankingAllClear[type], destAllClear[type]);
		}
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		GemManiaMode mode = new GemManiaMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "startstage"));
		assertEquals(-1, readInt(mode, "stageset"),
				"stageset defaults to -1 (the random-rotation marker)");
		assertEquals(false, readBoolean(mode, "alwaysghost"));
		assertEquals(false, readBoolean(mode, "always20g"));
		assertEquals(true, readBoolean(mode, "lvstopse"));
		assertEquals(false, readBoolean(mode, "showsectiontime"));
		assertEquals(false, readBoolean(mode, "randomnext"));
		assertEquals(0, readInt(mode, "trainingType"));
		assertEquals(0, readInt(mode, "startnextc"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderGemManiaPrefix() throws Exception {
		GemManiaMode source = new GemManiaMode();
		setInt(source, "startstage", 5);
		setInt(source, "stageset", 3);
		setBoolean(source, "alwaysghost", true);
		setBoolean(source, "always20g", true);
		setBoolean(source, "lvstopse", false);
		setBoolean(source, "showsectiontime", true);
		setBoolean(source, "randomnext", true);
		setInt(source, "trainingType", 2);
		setInt(source, "startnextc", 7);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(5, prop.getProperty("gemmania.startstage", -1));
		assertEquals(3, prop.getProperty("gemmania.stageset", -2));
		assertEquals(true, prop.getProperty("gemmania.alwaysghost", false));
		assertEquals(false, prop.getProperty("gemmania.lvstopse", true));
		assertEquals(2, prop.getProperty("gemmania.trainingType", -1));

		GemManiaMode dest = new GemManiaMode();
		invokeLoadSetting(dest, prop);
		assertEquals(5, readInt(dest, "startstage"));
		assertEquals(3, readInt(dest, "stageset"));
		assertEquals(true, readBoolean(dest, "alwaysghost"));
		assertEquals(true, readBoolean(dest, "always20g"));
		assertEquals(false, readBoolean(dest, "lvstopse"));
		assertEquals(true, readBoolean(dest, "showsectiontime"));
		assertEquals(true, readBoolean(dest, "randomnext"));
		assertEquals(2, readInt(dest, "trainingType"));
		assertEquals(7, readInt(dest, "startnextc"));
	}

	private static GameEngine freshEngine(GemManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(GemManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(GemManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(GemManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(GemManiaMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(GemManiaMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadRanking(GemManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(GemManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(GemManiaMode mode, CustomProperties prop) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(GemManiaMode mode, CustomProperties prop) throws Exception {
		Method m = GemManiaMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
