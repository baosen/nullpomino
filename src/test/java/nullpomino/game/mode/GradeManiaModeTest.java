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
 * Pins the headless slice of {@link GradeManiaMode}: registry surface,
 * playerInit defaults (the score-based GM1 model uses its own ranking
 * arrays — sibling AbstractGradeMode is reserved for GM2/GM3), and
 * the per-rule ranking + best-section-time property keys.
 */
class GradeManiaModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("GRADE MANIA", new GradeManiaMode().getName());
	}

	@Test
	void playerInitInstallsFreshGradeStateAndAllocatesRankingAndSectionArrays() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(false, readBoolean(mode, "gm300"));
		assertEquals(false, readBoolean(mode, "gm500"));
		assertEquals(0, readInt(mode, "secretGrade"));
		assertEquals(true, readBoolean(mode, "lvupflag"),
				"the level-up pending flag must default to true so the first "
						+ "section level-up animation fires");
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertNotNull(readField(mode, "rankingGrade"));
		assertNotNull(readField(mode, "rankingLevel"));
		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "sectiontime"));
		assertNotNull(readField(mode, "bestSectionTime"));

		// Ranking arrays are 1-D RANKING_MAX = 10. Section-time arrays
		// are SECTION_MAX = 10.
		assertEquals(10, ((int[]) readField(mode, "rankingGrade")).length);
		assertEquals(10, ((int[]) readField(mode, "sectiontime")).length);
	}

	@Test
	void playerInitOverridesEngineSpinBookkeepingAndSpeedDefaults() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		// GM1 disables T-spin and B2B (it is the score-based legacy model).
		assertEquals(false, engine.tspinEnable);
		assertEquals(false, engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DOUBLE, engine.comboType);
		assertEquals(false, engine.bighalf);
		assertEquals(false, engine.bigmove);
		assertTrue(engine.staffrollNoDeath,
				"GM1 staff-roll must keep the player alive for the credits sequence");

		// Speed defaults pinned by the constructor — drift here would change
		// every replay's playable feel.
		assertEquals(25, engine.speed.are);
		assertEquals(25, engine.speed.areLine);
		assertEquals(41, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(15, engine.speed.das);
	}

	@Test
	void loadRankingFillsWithZeroAndDefaultSectionTimeWhenPropFileIsEmpty() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
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
		// bestSectionTime defaults to DEFAULT_SECTION_TIME (5400) so that the
		// first run of every section beats it.
		for(int i = 0; i < bestSectionTime.length; i++) {
			assertEquals(5400, bestSectionTime[i]);
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderGradeManiaPrefix() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		// Stamp every cell with a unique value.
		for(int i = 0; i < rankingGrade.length; i++) {
			rankingGrade[i] = 30 - i;
			rankingLevel[i] = 999 - i;
			rankingTime[i] = 100000 + i;
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			bestSectionTime[i] = 4000 + i * 100;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(30, prop.getProperty("grademania.ranking.Standard.grade.0", -1));
		assertEquals(999, prop.getProperty("grademania.ranking.Standard.level.0", -1));
		assertEquals(100000, prop.getProperty("grademania.ranking.Standard.time.0", -1));
		assertEquals(4000, prop.getProperty("grademania.bestSectionTime.Standard.0", -1));

		GradeManiaMode dest = new GradeManiaMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		assertArrayEquals(rankingGrade, (int[]) readField(dest, "rankingGrade"));
		assertArrayEquals(rankingLevel, (int[]) readField(dest, "rankingLevel"));
		assertArrayEquals(rankingTime, (int[]) readField(dest, "rankingTime"));
		assertArrayEquals(bestSectionTime, (int[]) readField(dest, "bestSectionTime"));
	}

	@Test
	void rankingArraysSurviveBackToBackPlayerInitWithoutShapeDrift() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		int[] firstGrade = (int[]) readField(mode, "rankingGrade");
		mode.playerInit(engine, 0);
		int[] secondGrade = (int[]) readField(mode, "rankingGrade");

		assertEquals(firstGrade.length, secondGrade.length,
				"a second playerInit must keep the ranking shape stable so "
						+ "subsequent updates / loads do not crash");
	}

	private static GameEngine freshEngine(GradeManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(GradeManiaMode mode, String name) throws Exception {
		Field f = GradeManiaMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(GradeManiaMode mode, String name) throws Exception {
		Field f = GradeManiaMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(GradeManiaMode mode, String name) throws Exception {
		Field f = GradeManiaMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void invokeLoadRanking(GradeManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(GradeManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
