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
 * Pins the headless slice of {@link SpeedManiaMode}: registry surface,
 * the playerInit defaults including the medal counters and section
 * arrays, and the per-rule ranking + best-section-time I/O under the
 * 'speedmania.*' prefix. The settings UI, render path, and gameplay
 * loop need an SDL renderer and stay out of scope.
 */
class SpeedManiaModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("SPEED MANIA", new SpeedManiaMode().getName());
	}

	@Test
	void playerInitInstallsFreshGradeStateAndAllocatesRankingAndSectionArrays() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
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
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(false, readBoolean(mode, "rollstarted"));
		assertEquals(0, readInt(mode, "bgmlv"));
		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "gradeflash"));
		assertEquals(0, readInt(mode, "secretGrade"));
		assertEquals(0, readInt(mode, "sectionscomp"));
		assertEquals(0, readInt(mode, "sectionavgtime"));
		assertEquals(0, readInt(mode, "sectionlasttime"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// SpeedMania medal state.
		assertEquals(0, readInt(mode, "medalAC"));
		assertEquals(0, readInt(mode, "medalST"));
		assertEquals(0, readInt(mode, "medalSK"));
		assertEquals(0, readInt(mode, "medalRE"));
		assertEquals(0, readInt(mode, "medalRO"));

		// Ranking arrays at length RANKING_MAX=10; section arrays at SECTION_MAX=10.
		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		boolean[] sectionIsNewRecord = (boolean[]) readField(mode, "sectionIsNewRecord");
		assertNotNull(rankingGrade);
		assertEquals(10, rankingGrade.length);
		assertEquals(10, rankingLevel.length);
		assertEquals(10, rankingTime.length);
		assertEquals(10, sectiontime.length);
		assertEquals(10, bestSectionTime.length);
		assertEquals(10, sectionIsNewRecord.length);
	}

	@Test
	void loadRankingAppliesZeroDefaultsAndDefaultSectionTimeWhenPropFileIsEmpty() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingGrade.length; i++) {
			assertEquals(0, rankingGrade[i]);
			assertEquals(0, rankingLevel[i]);
			assertEquals(0, rankingTime[i]);
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			assertEquals(2520, bestSectionTime[i],
					"SpeedMania bestSectionTime defaults to DEFAULT_SECTION_TIME = 2520 (faster than the standard mania pace)");
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderSpeedManiaPrefix() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingGrade.length; i++) {
			rankingGrade[i] = 2;
			rankingLevel[i] = 999 - i;
			rankingTime[i] = 50000 + i;
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			bestSectionTime[i] = 2000 + i * 50;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(2, prop.getProperty("speedmania.ranking.Standard.grade.0", -1));
		assertEquals(999, prop.getProperty("speedmania.ranking.Standard.level.0", -1));
		assertEquals(50000, prop.getProperty("speedmania.ranking.Standard.time.0", -1));
		assertEquals(2000, prop.getProperty("speedmania.bestSectionTime.Standard.0", -1));
		assertEquals(2450, prop.getProperty("speedmania.bestSectionTime.Standard.9", -1));

		SpeedManiaMode dest = new SpeedManiaMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		assertArrayEquals(rankingGrade, (int[]) readField(dest, "rankingGrade"));
		assertArrayEquals(rankingLevel, (int[]) readField(dest, "rankingLevel"));
		assertArrayEquals(rankingTime, (int[]) readField(dest, "rankingTime"));
		assertArrayEquals(bestSectionTime, (int[]) readField(dest, "bestSectionTime"));
	}

	@Test
	void backToBackPlayerInitKeepsRankingShapeStable() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		int[] firstGrade = (int[]) readField(mode, "rankingGrade");
		mode.playerInit(engine, 0);
		int[] secondGrade = (int[]) readField(mode, "rankingGrade");

		assertEquals(firstGrade.length, secondGrade.length);
	}

	private static GameEngine freshEngine(SpeedManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(SpeedManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(SpeedManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(SpeedManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
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

	private static void invokeLoadRanking(SpeedManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(SpeedManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
