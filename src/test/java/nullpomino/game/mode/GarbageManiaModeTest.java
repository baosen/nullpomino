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
 * Pins the headless slice of {@link GarbageManiaMode}: the registry
 * surface, the playerInit defaults including the section-time and
 * garbage bookkeeping, the per-rule ranking + best-section-time I/O,
 * and the loadSetting / saveSetting round-trip under the
 * 'garbagemania.*' prefix.
 */
class GarbageManiaModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("GARBAGE MANIA", new GarbageManiaMode().getName());
	}

	@Test
	void playerInitInstallsFreshSectionStateAndAllocatesRankingArrays() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(0, readInt(mode, "gravityindex"));
		assertEquals(0, readInt(mode, "nextseclv"));
		assertEquals(true, readBoolean(mode, "lvupflag"),
				"lvupflag defaults to true so the first section's level-up "
						+ "animation fires");
		assertEquals(0, readInt(mode, "harddropBonus"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(0, readInt(mode, "secretGrade"));
		assertEquals(0, readInt(mode, "bgmlv"));
		assertEquals(0, readInt(mode, "sectionscomp"));
		assertEquals(0, readInt(mode, "sectionavgtime"));
		assertEquals(0, readInt(mode, "garbagePos"));
		assertEquals(0, readInt(mode, "garbageCount"));
		assertEquals(0, readInt(mode, "garbageTotal"));
		assertEquals(false, readBoolean(mode, "isShowBestSectionTime"));
		assertEquals(false, readBoolean(mode, "alwaysghost"));
		assertEquals(false, readBoolean(mode, "always20g"));
		assertEquals(false, readBoolean(mode, "lvstopse"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at length RANKING_MAX=10; section-time arrays at
		// SECTION_MAX=10.
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		boolean[] sectionIsNewRecord = (boolean[]) readField(mode, "sectionIsNewRecord");
		assertNotNull(rankingLevel);
		assertEquals(10, rankingLevel.length);
		assertEquals(10, rankingTime.length);
		assertEquals(10, sectiontime.length);
		assertEquals(10, bestSectionTime.length);
		assertEquals(10, sectionIsNewRecord.length);

		// Engine speed defaults pinned by playerInit.
		assertEquals(23, engine.speed.are);
		assertEquals(23, engine.speed.areLine);
	}

	@Test
	void loadRankingFillsZeroLevelTimeAndDefaultSectionTimeWhenPropFileIsEmpty() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingLevel.length; i++) {
			assertEquals(0, rankingLevel[i]);
			assertEquals(0, rankingTime[i]);
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			assertEquals(5400, bestSectionTime[i],
					"bestSectionTime defaults to DEFAULT_SECTION_TIME = 5400 (90s @ 60fps)");
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderGarbageManiaPrefix() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingLevel.length; i++) {
			rankingLevel[i] = 999 - i;
			rankingTime[i] = 100000 + i;
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			bestSectionTime[i] = 4000 + i * 100;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(999, prop.getProperty("garbagemania.ranking.Standard.level.0", -1));
		assertEquals(100000, prop.getProperty("garbagemania.ranking.Standard.time.0", -1));
		assertEquals(4000, prop.getProperty("garbagemania.bestSectionTime.Standard.0", -1));
		assertEquals(4900, prop.getProperty("garbagemania.bestSectionTime.Standard.9", -1));

		GarbageManiaMode dest = new GarbageManiaMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		assertArrayEquals(rankingLevel, (int[]) readField(dest, "rankingLevel"));
		assertArrayEquals(rankingTime, (int[]) readField(dest, "rankingTime"));
		assertArrayEquals(bestSectionTime, (int[]) readField(dest, "bestSectionTime"));
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(false, readBoolean(mode, "alwaysghost"));
		assertEquals(false, readBoolean(mode, "always20g"));
		assertEquals(false, readBoolean(mode, "lvstopse"),
				"GARBAGE MANIA defaults lvstopse to OFF (continuous flow)");
		assertEquals(false, readBoolean(mode, "showsectiontime"));
		assertEquals(false, readBoolean(mode, "big"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderGarbageManiaPrefix() throws Exception {
		GarbageManiaMode source = new GarbageManiaMode();
		setInt(source, "startlevel", 9);
		setBoolean(source, "alwaysghost", true);
		setBoolean(source, "always20g", true);
		setBoolean(source, "lvstopse", true);
		setBoolean(source, "showsectiontime", true);
		setBoolean(source, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(9, prop.getProperty("garbagemania.startlevel", -1));
		assertEquals(true, prop.getProperty("garbagemania.alwaysghost", false));
		assertEquals(true, prop.getProperty("garbagemania.lvstopse", false));
		assertEquals(true, prop.getProperty("garbagemania.big", false));

		GarbageManiaMode dest = new GarbageManiaMode();
		invokeLoadSetting(dest, prop);
		assertEquals(9, readInt(dest, "startlevel"));
		assertEquals(true, readBoolean(dest, "alwaysghost"));
		assertEquals(true, readBoolean(dest, "always20g"));
		assertEquals(true, readBoolean(dest, "lvstopse"));
		assertEquals(true, readBoolean(dest, "showsectiontime"));
		assertEquals(true, readBoolean(dest, "big"));
	}

	private static GameEngine freshEngine(GarbageManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(GarbageManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(GarbageManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(GarbageManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(GarbageManiaMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(GarbageManiaMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadRanking(GarbageManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(GarbageManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(GarbageManiaMode mode, CustomProperties prop) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(GarbageManiaMode mode, CustomProperties prop) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
