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
 * Pins the headless slice of {@link SpeedMania2Mode}: registry surface,
 * the playerInit defaults including the staff-roll wiring and the
 * SECTION_MAX=13 sectional layout, and the per-rule ranking I/O with
 * the rollclear bucket added in this mode.
 */
class SpeedMania2ModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("SPEED MANIA 2", new SpeedMania2Mode().getName());
	}

	@Test
	void playerInitInstallsFreshStateAllocatesArraysAndWiresStaffRoll() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
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
		assertEquals(false, readBoolean(mode, "rollstarted"));
		assertEquals(0, readInt(mode, "rollclear"));
		assertEquals(0, readInt(mode, "garbageCount"));
		assertEquals(0, readInt(mode, "regretdispframe"));
		assertEquals(0, readInt(mode, "secretGrade"));
		assertEquals(0, readInt(mode, "bgmlv"));
		assertEquals(0, readInt(mode, "sectionscomp"));
		assertEquals(0, readInt(mode, "sectionavgtime"));
		assertEquals(0, readInt(mode, "sectionlasttime"));
		assertEquals(0, readInt(mode, "medalAC"));
		assertEquals(0, readInt(mode, "medalST"));
		assertEquals(0, readInt(mode, "medalSK"));
		assertEquals(0, readInt(mode, "medalCO"));
		assertEquals(false, readBoolean(mode, "isShowBestSectionTime"));
		// startlevel / lvstopse / big / torikan / showsectiontime / gradedisp
		// are loaded from owner.modeConfig immediately after playerInit's
		// pre-load block, so their final value depends on what the bundled
		// mode.cfg holds. The pre-load block does pin them to their
		// documented defaults; see the loadSetting tests for that
		// contract directly, without the environmental coupling.
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at length RANKING_MAX=10; section arrays at SECTION_MAX=13.
		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		boolean[] sectionIsNewRecord = (boolean[]) readField(mode, "sectionIsNewRecord");
		assertNotNull(rankingGrade);
		assertEquals(10, rankingGrade.length);
		assertEquals(13, sectiontime.length);
		assertEquals(13, bestSectionTime.length);
		assertEquals(13, sectionIsNewRecord.length);

		// SpeedMania2 staff-roll wiring.
		assertEquals(false, engine.tspinEnable);
		assertEquals(false, engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DOUBLE, engine.comboType);
		assertEquals(GameEngine.FRAME_COLOR_RED, engine.framecolor);
		assertEquals(true, engine.bighalf);
		assertEquals(true, engine.bigmove);
		assertEquals(true, engine.staffrollEnable);
		assertEquals(false, engine.staffrollNoDeath,
				"staffrollNoDeath is OFF — the SpeedMania 2 credits roll is dangerous");
	}

	@Test
	void loadRankingFillsZeroDefaultsWithDefaultSectionTimeFallback() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
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
			assertEquals(2520, bestSectionTime[i],
					"DEFAULT_SECTION_TIME = 2520 (the legacy SpeedMania 2 section pace)");
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderSpeedMania2Prefix() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] rankingRollclear = (int[]) readField(mode, "rankingRollclear");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingGrade.length; i++) {
			rankingGrade[i] = i;
			rankingLevel[i] = 999 - i;
			rankingTime[i] = 50000 + i;
			rankingRollclear[i] = i % 3;
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			bestSectionTime[i] = 1500 + i * 50;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(0, prop.getProperty("speedmania2.ranking.Standard.grade.0", -1));
		assertEquals(50001, prop.getProperty("speedmania2.ranking.Standard.time.1", -1));
		assertEquals(1, prop.getProperty("speedmania2.ranking.Standard.rollclear.1", -1));
		assertEquals(2, prop.getProperty("speedmania2.ranking.Standard.rollclear.2", -1));
		assertEquals(1500, prop.getProperty("speedmania2.bestSectionTime.Standard.0", -1));
		assertEquals(2100, prop.getProperty("speedmania2.bestSectionTime.Standard.12", -1));

		SpeedMania2Mode dest = new SpeedMania2Mode();
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
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		int[] firstGrade = (int[]) readField(mode, "rankingGrade");
		mode.playerInit(engine, 0);
		int[] secondGrade = (int[]) readField(mode, "rankingGrade");

		assertEquals(firstGrade.length, secondGrade.length);
	}

	private static GameEngine freshEngine(SpeedMania2Mode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(SpeedMania2Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(SpeedMania2Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(SpeedMania2Mode mode, String name) throws Exception {
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

	private static void invokeLoadRanking(SpeedMania2Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(SpeedMania2Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
