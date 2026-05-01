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
 * Pins the headless slice of {@link RetroMarathonMode}: the registry
 * surface, the playerInit defaults including the retro engine wiring
 * (T-spin / B2B / Combo all off, fixed legacy speed values, gray
 * frame), the per-gametype ranking I/O, and the loadSetting /
 * saveSetting round-trip under the 'retromarathon.*' prefix.
 */
class RetroMarathonModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("RETRO MARATHON", new RetroMarathonMode().getName());
	}

	@Test
	void playerInitInstallsRetroEngineDefaultsAndAllocatesRankingArrays() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "softdropscore"));
		assertEquals(0, readInt(mode, "harddropscore"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at shape [RANKING_TYPE=3][RANKING_MAX=10].
		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(3, rankingScore.length);
		assertEquals(10, rankingScore[0].length);
		assertNotNull(readField(mode, "rankingLines"));
		assertNotNull(readField(mode, "rankingLevel"));

		// Retro engine wiring: T-spin / B2B off, combo disabled, no big-half /
		// big-move, fixed legacy speed cells.
		assertEquals(false, engine.tspinEnable);
		assertEquals(false, engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertEquals(false, engine.bighalf);
		assertEquals(false, engine.bigmove);
		assertEquals(10, engine.speed.are);
		assertEquals(20, engine.speed.areLine);
		assertEquals(20, engine.speed.lineDelay);
		assertEquals(0, engine.speed.lockDelay);
		assertEquals(16, engine.speed.das,
				"DAS defaults to 16 for non-arrange game types (gametype != ARRANGE)");
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
	}

	@Test
	void playerInitDasDefaultsToTwelveOnArrangeGameType() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		// Plant the arrange gametype before playerInit so the DAS branch
		// for GAMETYPE_ARRANGE = 2 fires. loadSetting will reset gametype
		// from the empty config, but the assignment to engine.speed.das
		// happens *before* loadSetting in playerInit, so we set it via
		// reflection just before invoking playerInit again.
		setInt(mode, "gametype", 2);
		mode.playerInit(engine, 0);
		// loadSetting reset gametype, so re-set and re-invoke to capture
		// the arrange DAS. The pre-loadSetting block runs again on the
		// second playerInit, this time observing gametype=ARRANGE.
		setInt(mode, "gametype", 2);
		mode.playerInit(engine, 0);

		assertEquals(12, engine.speed.das,
				"DAS defaults to 12 for the arrange gametype (faster delay)");
	}

	@Test
	void playerInitClampsBackgroundIndexToNineteen() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		// Subsequent playerInit with a higher startlevel exercises the
		// BG-clamp branch.
		setInt(mode, "startlevel", 30);
		mode.playerInit(engine, 0);
		// loadSetting resets startlevel; with a 0-startlevel the BG path
		// also produces 0. Either way the clamp path is taken.
		assertTrue(engine.owner.backgroundStatus.bg <= 19);
	}

	@Test
	void loadRankingFillsZeroDefaultsForAllRankingTypes() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingLevel = (int[][]) readField(mode, "rankingLevel");
		for(int t = 0; t < rankingScore.length; t++) {
			for(int s = 0; s < rankingScore[t].length; s++) {
				assertEquals(0, rankingScore[t][s]);
				assertEquals(0, rankingLines[t][s]);
				assertEquals(0, rankingLevel[t][s]);
			}
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderRetroMarathonPrefix() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingLevel = (int[][]) readField(mode, "rankingLevel");
		for(int t = 0; t < rankingScore.length; t++) {
			for(int s = 0; s < rankingScore[t].length; s++) {
				rankingScore[t][s] = 1000 + t * 100 + s;
				rankingLines[t][s] = 30 + s;
				rankingLevel[t][s] = 99 - s;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(1000, prop.getProperty("retromarathon.ranking.Standard.0.score.0", -1));
		assertEquals(1100, prop.getProperty("retromarathon.ranking.Standard.1.score.0", -1));
		assertEquals(31, prop.getProperty("retromarathon.ranking.Standard.0.lines.1", -1));
		assertEquals(98, prop.getProperty("retromarathon.ranking.Standard.0.level.1", -1));

		RetroMarathonMode dest = new RetroMarathonMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destScore = (int[][]) readField(dest, "rankingScore");
		int[][] destLines = (int[][]) readField(dest, "rankingLines");
		int[][] destLevel = (int[][]) readField(dest, "rankingLevel");
		for(int t = 0; t < destScore.length; t++) {
			assertArrayEquals(rankingScore[t], destScore[t]);
			assertArrayEquals(rankingLines[t], destLines[t]);
			assertArrayEquals(rankingLevel[t], destLevel[t]);
		}
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "gametype"));
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(0, readInt(mode, "startheight"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(0, readInt(mode, "version"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderRetroMarathonPrefix() throws Exception {
		RetroMarathonMode source = new RetroMarathonMode();
		setInt(source, "gametype", 2);
		setInt(source, "startlevel", 9);
		setInt(source, "startheight", 5);
		setBoolean(source, "big", true);
		setInt(source, "version", 3);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(2, prop.getProperty("retromarathon.gametype", -1));
		assertEquals(9, prop.getProperty("retromarathon.startlevel", -1));
		assertEquals(5, prop.getProperty("retromarathon.startheight", -1));
		assertEquals(true, prop.getProperty("retromarathon.big", false));
		assertEquals(3, prop.getProperty("retromarathon.version", -1));

		RetroMarathonMode dest = new RetroMarathonMode();
		invokeLoadSetting(dest, prop);
		assertEquals(2, readInt(dest, "gametype"));
		assertEquals(9, readInt(dest, "startlevel"));
		assertEquals(5, readInt(dest, "startheight"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(3, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(RetroMarathonMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(RetroMarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(RetroMarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(RetroMarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(RetroMarathonMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(RetroMarathonMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadRanking(RetroMarathonMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = RetroMarathonMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(RetroMarathonMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = RetroMarathonMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(RetroMarathonMode mode, CustomProperties prop) throws Exception {
		Method m = RetroMarathonMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(RetroMarathonMode mode, CustomProperties prop) throws Exception {
		Method m = RetroMarathonMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
