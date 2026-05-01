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
 * Pins the headless slice of {@link GradeMania3Mode}: registry surface,
 * playerInit defaults (the GM3 cool/regret + grade-history model on top
 * of the AbstractGradeMode shared scaffolding), and the per-rule
 * 2-D ranking I/O including the GM3-specific exam track and the
 * grade-history / qualified-grade / demotion-points trio.
 */
class GradeMania3ModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("GRADE MANIA 3", new GradeMania3Mode().getName());
	}

	@Test
	void playerInitInstallsFreshGradeStateAndAllocatesAllRankingArrays() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
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
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(false, readBoolean(mode, "rollstarted"));
		assertEquals(true, readBoolean(mode, "lvupflag"));
		assertEquals(false, readBoolean(mode, "mrollFlag"));

		// GM3-specific cool/regret state.
		assertEquals(false, readBoolean(mode, "cool"));
		assertEquals(0, readInt(mode, "coolcount"));
		assertEquals(false, readBoolean(mode, "previouscool"));
		assertEquals(false, readBoolean(mode, "coolchecked"));
		assertEquals(false, readBoolean(mode, "cooldisplayed"));
		assertEquals(0, readInt(mode, "cooldispframe"));
		assertEquals(0, readInt(mode, "regretdispframe"));

		// GM3-specific medal counters.
		assertEquals(0, readInt(mode, "medalAC"));
		assertEquals(0, readInt(mode, "medalST"));
		assertEquals(0, readInt(mode, "medalSK"));
		assertEquals(0, readInt(mode, "medalCO"));

		// 2-D ranking arrays for normal vs exam tracks.
		assertNotNull(readField(mode, "rankingGrade"));
		assertNotNull(readField(mode, "rankingLevel"));
		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "rankingRollclear"));
		assertNotNull(readField(mode, "bestSectionTime"));
		assertNotNull(readField(mode, "gradeHistory"));
		assertNotNull(readField(mode, "regretsection"));
		assertNotNull(readField(mode, "coolsection"));

		int[][] rankingGrade = (int[][]) readField(mode, "rankingGrade");
		assertEquals(10, rankingGrade.length, "RANKING_MAX = 10");
		assertEquals(2, rankingGrade[0].length, "RANKING_TYPE = 2 (normal + exam)");
		int[] gradeHistory = (int[]) readField(mode, "gradeHistory");
		assertEquals(7, gradeHistory.length, "GRADE_HISTORY_SIZE = 7");
	}

	@Test
	void loadRankingFillsZeroDefaultsAndSpecialSentinelsWhenPropFileIsEmpty() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingGrade = (int[][]) readField(mode, "rankingGrade");
		int[][] bestSectionTime = (int[][]) readField(mode, "bestSectionTime");
		int[] gradeHistory = (int[]) readField(mode, "gradeHistory");
		// Both ranking tracks default to 0.
		for(int i = 0; i < rankingGrade.length; i++) {
			for(int j = 0; j < rankingGrade[i].length; j++) {
				assertEquals(0, rankingGrade[i][j]);
			}
		}
		// bestSectionTime defaults to DEFAULT_SECTION_TIME = 5400.
		for(int i = 0; i < bestSectionTime.length; i++) {
			for(int j = 0; j < bestSectionTime[i].length; j++) {
				assertEquals(5400, bestSectionTime[i][j]);
			}
		}
		// gradeHistory defaults to -1 (unranked sentinel) for every slot.
		for(int i = 0; i < gradeHistory.length; i++) {
			assertEquals(-1, gradeHistory[i]);
		}
		assertEquals(0, readInt(mode, "qualifiedGrade"));
		assertEquals(0, readInt(mode, "demotionPoints"));
	}

	@Test
	void saveRankingAndLoadRankingRoundTripBothNormalAndExamTracks() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingGrade = (int[][]) readField(mode, "rankingGrade");
		int[][] rankingLevel = (int[][]) readField(mode, "rankingLevel");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingRollclear = (int[][]) readField(mode, "rankingRollclear");
		int[][] bestSectionTime = (int[][]) readField(mode, "bestSectionTime");
		int[] gradeHistory = (int[]) readField(mode, "gradeHistory");
		// Stamp every cell with a unique value across both tracks.
		for(int i = 0; i < rankingGrade.length; i++) {
			for(int j = 0; j < rankingGrade[i].length; j++) {
				rankingGrade[i][j] = 30 - i + j * 100;
				rankingLevel[i][j] = 999 - i + j * 1000;
				rankingTime[i][j] = 100000 + i + j;
				rankingRollclear[i][j] = (i + j) % 4;
			}
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			for(int j = 0; j < bestSectionTime[i].length; j++) {
				bestSectionTime[i][j] = 4000 + i * 100 + j * 10;
			}
		}
		for(int i = 0; i < gradeHistory.length; i++) {
			gradeHistory[i] = 10 + i;
		}
		setInt(mode, "qualifiedGrade", 13);
		setInt(mode, "demotionPoints", 5);

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		// Spot-check both tracks land under their own key prefixes.
		assertEquals(30, prop.getProperty("grademania3.ranking.Standard.grade.0", -1));
		assertEquals(130, prop.getProperty("grademania3.ranking.exam.Standard.grade.0", -1));
		assertEquals(4000, prop.getProperty("grademania3.bestSectionTime.0.Standard.0", -1));
		assertEquals(4010, prop.getProperty("grademania3.bestSectionTime.1.Standard.0", -1));
		assertEquals(10, prop.getProperty("grademania3.gradehistory.Standard.0", -1));
		assertEquals(13, prop.getProperty("grademania3.qualified.Standard", -1));
		assertEquals(5, prop.getProperty("grademania3.demopoint.Standard", -1));

		GradeMania3Mode dest = new GradeMania3Mode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destGrade = (int[][]) readField(dest, "rankingGrade");
		int[][] destLevel = (int[][]) readField(dest, "rankingLevel");
		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		int[][] destRollclear = (int[][]) readField(dest, "rankingRollclear");
		int[][] destBest = (int[][]) readField(dest, "bestSectionTime");
		int[] destHistory = (int[]) readField(dest, "gradeHistory");
		for(int i = 0; i < rankingGrade.length; i++) {
			assertArrayEquals(rankingGrade[i], destGrade[i]);
			assertArrayEquals(rankingLevel[i], destLevel[i]);
			assertArrayEquals(rankingTime[i], destTime[i]);
			assertArrayEquals(rankingRollclear[i], destRollclear[i]);
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			assertArrayEquals(bestSectionTime[i], destBest[i]);
		}
		assertArrayEquals(gradeHistory, destHistory);
		assertEquals(13, readInt(dest, "qualifiedGrade"));
		assertEquals(5, readInt(dest, "demotionPoints"));
	}

	@Test
	void backToBackPlayerInitKeepsRankingAndHistoryShapesStable() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		int[][] firstGrade = (int[][]) readField(mode, "rankingGrade");
		int[] firstHistory = (int[]) readField(mode, "gradeHistory");
		mode.playerInit(engine, 0);
		int[][] secondGrade = (int[][]) readField(mode, "rankingGrade");
		int[] secondHistory = (int[]) readField(mode, "gradeHistory");

		assertEquals(firstGrade.length, secondGrade.length);
		assertEquals(firstGrade[0].length, secondGrade[0].length);
		assertEquals(firstHistory.length, secondHistory.length);
		assertTrue(true, "second playerInit must keep array shapes stable");
	}

	private static GameEngine freshEngine(GradeMania3Mode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(GradeMania3Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(GradeMania3Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(GradeMania3Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(GradeMania3Mode mode, String name, int value) throws Exception {
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

	private static void invokeLoadRanking(GradeMania3Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(GradeMania3Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
