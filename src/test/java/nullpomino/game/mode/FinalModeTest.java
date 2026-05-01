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
 * Pins the headless slice of {@link FinalMode}: registry surface,
 * playerInit defaults including grade-state and medal counters,
 * the per-rule ranking I/O with the rollclear bucket and a 30-second
 * (1800-frame) default section pace, and the loadSetting / saveSetting
 * round-trip under 'final.*'.
 */
class FinalModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("FINAL", new FinalMode().getName());
	}

	@Test
	void playerInitInstallsFreshGradeStateAndAllocatesRankingArrays() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(0, readInt(mode, "nextseclv"));
		assertEquals(true, readBoolean(mode, "lvupflag"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(false, readBoolean(mode, "rollstarted"));
		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "gradeflash"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "secretGrade"));
		assertEquals(0, readInt(mode, "sectionscomp"));
		assertEquals(0, readInt(mode, "sectionavgtime"));
		assertEquals(0, readInt(mode, "sectionlasttime"));

		// FINAL-specific medal counters reset.
		assertEquals(0, readInt(mode, "medalAC"));
		assertEquals(0, readInt(mode, "medalST"));
		assertEquals(0, readInt(mode, "medalSK"));
		assertEquals(0, readInt(mode, "medalCO"));
		assertEquals(false, readBoolean(mode, "isShowBestSectionTime"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Four ranking arrays at length RANKING_MAX=10 (grade / level /
		// time / rollclear) and the section-time arrays at SECTION_MAX=10.
		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] rankingRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		boolean[] sectionIsNewRecord = (boolean[]) readField(mode, "sectionIsNewRecord");
		assertNotNull(rankingGrade);
		assertEquals(10, rankingGrade.length);
		assertEquals(10, rankingLevel.length);
		assertEquals(10, rankingTime.length);
		assertEquals(10, rankingRollclear.length);
		assertEquals(10, sectiontime.length);
		assertEquals(10, bestSectionTime.length);
		assertEquals(10, sectionIsNewRecord.length);
	}

	@Test
	void loadRankingFillsZeroDefaultsWithDefaultSectionTimeFallback() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] rankingRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingGrade.length; i++) {
			assertEquals(0, rankingGrade[i]);
			assertEquals(0, rankingLevel[i]);
			assertEquals(0, rankingTime[i]);
			assertEquals(0, rankingRollclear[i]);
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			assertEquals(1800, bestSectionTime[i],
					"FINAL bestSectionTime defaults to DEFAULT_SECTION_TIME = 1800 (30s)");
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderFinalPrefix() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] rankingRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingGrade.length; i++) {
			rankingGrade[i] = 3 - i / 4; // grades 3..0
			rankingLevel[i] = 999 - i;
			rankingTime[i] = 80000 + i;
			rankingRollclear[i] = i % 3;
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			bestSectionTime[i] = 1500 + i * 50;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(3, prop.getProperty("final.ranking.Standard.grade.0", -1));
		assertEquals(999, prop.getProperty("final.ranking.Standard.level.0", -1));
		assertEquals(80001, prop.getProperty("final.ranking.Standard.time.1", -1));
		assertEquals(1, prop.getProperty("final.ranking.Standard.rollclear.1", -1));
		assertEquals(2, prop.getProperty("final.ranking.Standard.rollclear.2", -1));
		assertEquals(1500, prop.getProperty("final.bestSectionTime.Standard.0", -1));
		assertEquals(1950, prop.getProperty("final.bestSectionTime.Standard.9", -1));

		FinalMode dest = new FinalMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		assertArrayEquals(rankingGrade, (int[]) readField(dest, "rankingGrade"));
		assertArrayEquals(rankingLevel, (int[]) readField(dest, "rankingLevel"));
		assertArrayEquals(rankingTime, (int[]) readField(dest, "rankingTime"));
		assertArrayEquals(rankingRollclear, (int[]) readField(dest, "rankingRollclear"));
		assertArrayEquals(bestSectionTime, (int[]) readField(dest, "bestSectionTime"));
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		FinalMode mode = new FinalMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(false, readBoolean(mode, "lvstopse"));
		assertEquals(false, readBoolean(mode, "showsectiontime"));
		assertEquals(false, readBoolean(mode, "big"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderFinalPrefix() throws Exception {
		FinalMode source = new FinalMode();
		setInt(source, "startlevel", 7);
		setBoolean(source, "lvstopse", true);
		setBoolean(source, "showsectiontime", true);
		setBoolean(source, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(7, prop.getProperty("final.startlevel", -1));
		assertEquals(true, prop.getProperty("final.lvstopse", false));
		assertEquals(true, prop.getProperty("final.showsectiontime", false));
		assertEquals(true, prop.getProperty("final.big", false));

		FinalMode dest = new FinalMode();
		invokeLoadSetting(dest, prop);
		assertEquals(7, readInt(dest, "startlevel"));
		assertEquals(true, readBoolean(dest, "lvstopse"));
		assertEquals(true, readBoolean(dest, "showsectiontime"));
		assertEquals(true, readBoolean(dest, "big"));
	}

	private static GameEngine freshEngine(FinalMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(FinalMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(FinalMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(FinalMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(FinalMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(FinalMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadRanking(FinalMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(FinalMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(FinalMode mode, CustomProperties prop) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(FinalMode mode, CustomProperties prop) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
