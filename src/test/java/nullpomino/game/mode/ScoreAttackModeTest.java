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
 * Pins the headless slice of {@link ScoreAttackMode}: the registry
 * surface, the playerInit defaults including the engine wiring (no
 * T-spin / B2B / Combo, no big-half / big-move, staffroll-no-death
 * on, COMBO_TYPE_DOUBLE), the per-rule ranking + best-section-time
 * I/O at SECTION_MAX=3, and the loadSetting / saveSetting round-trip
 * under the 'scoreattack.*' prefix.
 */
class ScoreAttackModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("SCORE ATTACK", new ScoreAttackMode().getName());
	}

	@Test
	void playerInitInstallsFreshScoringStateAndAllocatesRankingArrays() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
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
		assertEquals(true, readBoolean(mode, "lvupflag"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(0, readInt(mode, "bgmlv"));
		assertEquals(0, readInt(mode, "sectionscomp"));
		assertEquals(0, readInt(mode, "sectionavgtime"));
		assertEquals(false, readBoolean(mode, "sectionAnyNewRecord"));
		assertEquals(false, readBoolean(mode, "isShowBestSectionTime"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at length RANKING_MAX=10; section arrays at
		// SECTION_MAX=3 (the three race tiers).
		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		boolean[] sectionIsNewRecord = (boolean[]) readField(mode, "sectionIsNewRecord");
		assertNotNull(rankingScore);
		assertEquals(10, rankingScore.length);
		assertEquals(10, rankingLevel.length);
		assertEquals(10, rankingTime.length);
		assertEquals(3, sectiontime.length);
		assertEquals(3, bestSectionTime.length);
		assertEquals(3, sectionIsNewRecord.length);

		// ScoreAttack engine wiring: no T-spin / B2B, comboType=DOUBLE,
		// no big-half / big-move (regular block size), staffrollNoDeath ON.
		assertEquals(false, engine.tspinEnable);
		assertEquals(false, engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DOUBLE, engine.comboType);
		assertEquals(false, engine.bighalf);
		assertEquals(false, engine.bigmove);
		assertEquals(true, engine.staffrollNoDeath);
	}

	@Test
	void loadRankingFillsZeroDefaultsAndDefaultSectionTimeFallback() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingScore.length; i++) {
			assertEquals(0, rankingScore[i]);
			assertEquals(0, rankingLevel[i]);
			assertEquals(0, rankingTime[i]);
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			assertEquals(6000, bestSectionTime[i],
					"DEFAULT_SECTION_TIME = 6000 (100s, the legacy ScoreAttack section pace)");
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderScoreAttackPrefix() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < rankingScore.length; i++) {
			rankingScore[i] = 50000 + i * 100;
			rankingLevel[i] = 99 - i;
			rankingTime[i] = 50000 + i;
		}
		for(int i = 0; i < bestSectionTime.length; i++) {
			bestSectionTime[i] = 5000 + i * 100;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(50000, prop.getProperty("scoreattack.ranking.item.Standard.score.0", -1));
		assertEquals(99, prop.getProperty("scoreattack.ranking.item.Standard.level.0", -1));
		assertEquals(50001, prop.getProperty("scoreattack.ranking.item.Standard.time.1", -1));
		assertEquals(5000, prop.getProperty("scoreattack.bestSectionTime.item.Standard.0", -1));
		assertEquals(5200, prop.getProperty("scoreattack.bestSectionTime.item.Standard.2", -1));

		ScoreAttackMode dest = new ScoreAttackMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		assertArrayEquals(rankingScore, (int[]) readField(dest, "rankingScore"));
		assertArrayEquals(rankingLevel, (int[]) readField(dest, "rankingLevel"));
		assertArrayEquals(rankingTime, (int[]) readField(dest, "rankingTime"));
		assertArrayEquals(bestSectionTime, (int[]) readField(dest, "bestSectionTime"));
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(false, readBoolean(mode, "alwaysghost"));
		assertEquals(false, readBoolean(mode, "always20g"));
		assertEquals(false, readBoolean(mode, "showsectiontime"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(true, readBoolean(mode, "enableitem"));
		assertEquals(0, readInt(mode, "version"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderScoreAttackPrefix() throws Exception {
		ScoreAttackMode source = new ScoreAttackMode();
		setInt(source, "startlevel", 9);
		setBoolean(source, "alwaysghost", true);
		setBoolean(source, "always20g", true);
		setBoolean(source, "showsectiontime", true);
		setBoolean(source, "big", true);
		setBoolean(source, "enableitem", false);
		setInt(source, "version", 2);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(9, prop.getProperty("scoreattack.startlevel", -1));
		assertEquals(true, prop.getProperty("scoreattack.alwaysghost", false));
		assertEquals(true, prop.getProperty("scoreattack.always20g", false));
		assertEquals(true, prop.getProperty("scoreattack.showsectiontime", false));
		assertEquals(true, prop.getProperty("scoreattack.big", false));
		assertEquals(false, prop.getProperty("scoreattack.enableitem", true));
		assertEquals(2, prop.getProperty("scoreattack.version", -1));

		ScoreAttackMode dest = new ScoreAttackMode();
		invokeLoadSetting(dest, prop);
		assertEquals(9, readInt(dest, "startlevel"));
		assertEquals(true, readBoolean(dest, "alwaysghost"));
		assertEquals(true, readBoolean(dest, "always20g"));
		assertEquals(true, readBoolean(dest, "showsectiontime"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(false, readBoolean(dest, "enableitem"));
		assertEquals(2, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(ScoreAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(ScoreAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ScoreAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(ScoreAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(ScoreAttackMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ScoreAttackMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadRanking(ScoreAttackMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(ScoreAttackMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(ScoreAttackMode mode, CustomProperties prop) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(ScoreAttackMode mode, CustomProperties prop) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
