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
 * Pins the headless slice of {@link PhantomManiaMode}: registry surface,
 * the playerInit grade-state defaults, and the per-rule ranking I/O
 * with the rollclear bucket plus best-section-time tracking.
 */
class PhantomManiaModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("PHANTOM MANIA", new PhantomManiaMode().getName());
	}

	@Test
	void playerInitInstallsFreshGradeStateAndAllocatesRankingArrays() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
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
		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "gradeflash"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(0, readInt(mode, "rollclear"));
		assertEquals(false, readBoolean(mode, "rollstarted"));
		assertEquals(0, readInt(mode, "bgmlv"));
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
	void loadRankingFillsZeroDefaultsAndDefaultSectionTimeWhenPropFileIsEmpty() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
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
			assertEquals(3600, bestSectionTime[i],
					"PHANTOM MANIA bestSectionTime defaults to DEFAULT_SECTION_TIME = 3600 (60s)");
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderPhantomManiaPrefix() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] rankingRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingGrade.length; i++) {
			rankingGrade[i] = 6 - i / 2; // grades 6..1
			rankingLevel[i] = 999 - i;
			rankingTime[i] = 100000 + i;
			rankingRollclear[i] = i % 4;
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			bestSectionTime[i] = 2500 + i * 50;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(6, prop.getProperty("phantommania.ranking.Standard.grade.0", -1));
		assertEquals(999, prop.getProperty("phantommania.ranking.Standard.level.0", -1));
		assertEquals(100000, prop.getProperty("phantommania.ranking.Standard.time.0", -1));
		assertEquals(0, prop.getProperty("phantommania.ranking.Standard.rollclear.0", -1));
		assertEquals(2, prop.getProperty("phantommania.ranking.Standard.rollclear.2", -1));
		assertEquals(2500, prop.getProperty("phantommania.bestSectionTime.Standard.0", -1));
		assertEquals(2950, prop.getProperty("phantommania.bestSectionTime.Standard.9", -1));

		PhantomManiaMode dest = new PhantomManiaMode();
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
	void backToBackPlayerInitKeepsRankingShapeStable() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		int[] firstGrade = (int[]) readField(mode, "rankingGrade");
		mode.playerInit(engine, 0);
		int[] secondGrade = (int[]) readField(mode, "rankingGrade");

		assertEquals(firstGrade.length, secondGrade.length);
	}

	private static GameEngine freshEngine(PhantomManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(PhantomManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(PhantomManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(PhantomManiaMode mode, String name) throws Exception {
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

	private static void invokeLoadRanking(PhantomManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(PhantomManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
