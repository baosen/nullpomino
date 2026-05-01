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
 * Pins the headless slice of {@link GradeMania2Mode}: registry surface,
 * playerInit defaults (the GM2 medal model on top of AbstractGradeMode's
 * shared scaffolding), and the per-rule ranking + best-section-time
 * property keys (which include the rollclear bucket added in GM2).
 */
class GradeMania2ModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("GRADE MANIA 2", new GradeMania2Mode().getName());
	}

	@Test
	void playerInitInstallsFreshGradeStateAndAllocatesRankingAndSectionArrays() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		// Inherited from AbstractGradeMode.
		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(false, readBoolean(mode, "rollstarted"));
		assertEquals(0, readInt(mode, "secretGrade"));
		assertEquals(true, readBoolean(mode, "lvupflag"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(false, readBoolean(mode, "mrollFlag"));

		// GM2-specific medal counters reset to 0.
		assertEquals(0, readInt(mode, "medalAC"));
		assertEquals(0, readInt(mode, "medalST"));
		assertEquals(0, readInt(mode, "medalSK"));
		assertEquals(0, readInt(mode, "medalRE"));
		assertEquals(0, readInt(mode, "medalRO"));
		assertEquals(0, readInt(mode, "medalCO"));

		assertNotNull(readField(mode, "rankingGrade"));
		assertNotNull(readField(mode, "rankingLevel"));
		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "rankingRollclear"));
		assertNotNull(readField(mode, "sectiontime"));
		assertNotNull(readField(mode, "bestSectionTime"));
		assertNotNull(readField(mode, "sectionfourline"));

		assertEquals(10, ((int[]) readField(mode, "rankingGrade")).length);
		assertEquals(10, ((int[]) readField(mode, "sectiontime")).length);
	}

	@Test
	void loadRankingFillsWithZeroAndDefaultSectionTimeWhenPropFileIsEmpty() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
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
			assertEquals(5400, bestSectionTime[i],
					"DEFAULT_SECTION_TIME defaults to 5400 (90s @ 60fps)");
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderGradeMania2Prefix() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] rankingRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		// Stamp every cell.
		for(int i = 0; i < rankingGrade.length; i++) {
			rankingGrade[i] = 30 - i;
			rankingLevel[i] = 999 - i;
			rankingTime[i] = 100000 + i;
			rankingRollclear[i] = i % 3;
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			bestSectionTime[i] = 4000 + i * 100;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(30, prop.getProperty("grademania2.ranking.Standard.grade.0", -1));
		assertEquals(999, prop.getProperty("grademania2.ranking.Standard.level.0", -1));
		assertEquals(100000, prop.getProperty("grademania2.ranking.Standard.time.0", -1));
		assertEquals(0, prop.getProperty("grademania2.ranking.Standard.rollclear.0", -1));
		assertEquals(1, prop.getProperty("grademania2.ranking.Standard.rollclear.1", -1));
		assertEquals(4000, prop.getProperty("grademania2.bestSectionTime.Standard.0", -1));

		GradeMania2Mode dest = new GradeMania2Mode();
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
	void rollclearBucketIsAdditiveOnTopOfTheGM1RankingShape() throws Exception {
		// The GM2 ranking carries the GM1 grade/level/time triple plus the
		// rollclear bucket that records whether the run cleared the staff
		// roll. Pin that the bucket sits next to the others under the
		// 'rollclear' key segment.
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		int[] rankingRollclear = (int[]) readField(mode, "rankingRollclear");
		assertEquals(10, rankingRollclear.length);

		rankingRollclear[2] = 7;

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(7, prop.getProperty("grademania2.ranking.Standard.rollclear.2", -1));
		assertEquals(0, prop.getProperty("grademania2.ranking.Standard.rollclear.5", -1));
	}

	@Test
	void backToBackPlayerInitKeepsRankingShapeStable() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		int[] firstGrade = (int[]) readField(mode, "rankingGrade");
		mode.playerInit(engine, 0);
		int[] secondGrade = (int[]) readField(mode, "rankingGrade");

		assertEquals(firstGrade.length, secondGrade.length);
		assertTrue(true, "second playerInit must keep the ranking shape stable");
	}

	private static GameEngine freshEngine(GradeMania2Mode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(GradeMania2Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(GradeMania2Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(GradeMania2Mode mode, String name) throws Exception {
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

	private static void invokeLoadRanking(GradeMania2Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(GradeMania2Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
