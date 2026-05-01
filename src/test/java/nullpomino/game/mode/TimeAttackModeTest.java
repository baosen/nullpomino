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
 * Pins the headless slice of {@link TimeAttackMode}: the registry
 * surface, the playerInit defaults including the engine wiring (no
 * T-spin / B2B / Combo, big-half + big-move, staff-roll off, gray
 * frame), the per-gametype ranking I/O across 11 game types with
 * the rollclear bucket, and the loadSetting / saveSetting round-trip
 * under the 'timeattack.*' prefix.
 */
class TimeAttackModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("TIME ATTACK", new TimeAttackMode().getName());
	}

	@Test
	void playerInitInstallsFreshSectionStateAndAllocatesRankingArrays() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		assertEquals(0, readInt(mode, "norm"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(false, readBoolean(mode, "rollstarted"));
		assertEquals(0, readInt(mode, "sectionscomp"));
		assertEquals(0, readInt(mode, "sectionavgtime"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// Ranking arrays at shape [GAMETYPE_MAX=11][RANKING_MAX=10].
		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingRollclear = (int[][]) readField(mode, "rankingRollclear");
		assertNotNull(rankingLines);
		assertEquals(11, rankingLines.length);
		assertEquals(10, rankingLines[0].length);
		assertEquals(11, rankingTime.length);
		assertEquals(11, rankingRollclear.length);

		// Section-time array is fixed at 20 (covers max-level layout).
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		assertEquals(20, sectiontime.length);

		// TimeAttack engine wiring: no T-spin / B2B / Combo, big-half +
		// big-move on, staff roll off, gray frame.
		assertEquals(false, engine.tspinEnable);
		assertEquals(false, engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
		assertEquals(true, engine.bighalf);
		assertEquals(true, engine.bigmove);
		assertEquals(false, engine.staffrollEnable);
		assertEquals(false, engine.staffrollNoDeath);
	}

	@Test
	void loadRankingFillsZeroDefaultsAcrossEveryGameType() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "EmptyRule");

		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingRollclear = (int[][]) readField(mode, "rankingRollclear");
		for(int t = 0; t < rankingLines.length; t++) {
			for(int s = 0; s < rankingLines[t].length; s++) {
				assertEquals(0, rankingLines[t][s]);
				assertEquals(0, rankingTime[t][s]);
				assertEquals(0, rankingRollclear[t][s]);
			}
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTripUnderTimeAttackPrefix() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingLines = (int[][]) readField(mode, "rankingLines");
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		int[][] rankingRollclear = (int[][]) readField(mode, "rankingRollclear");
		for(int t = 0; t < rankingLines.length; t++) {
			for(int s = 0; s < rankingLines[t].length; s++) {
				rankingLines[t][s] = 100 + t * 10 + s;
				rankingTime[t][s] = 50000 + t * 1000 + s;
				rankingRollclear[t][s] = (t + s) % 4;
			}
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		assertEquals(100, prop.getProperty("timeattack.ranking.Standard.0.lines.0", -1));
		assertEquals(110, prop.getProperty("timeattack.ranking.Standard.1.lines.0", -1));
		assertEquals(50000, prop.getProperty("timeattack.ranking.Standard.0.time.0", -1));
		assertEquals(2, prop.getProperty("timeattack.ranking.Standard.0.rollclear.2", -1));
		assertEquals(0, prop.getProperty("timeattack.ranking.Standard.0.rollclear.0", -1));
		// Confirm last game type writes its bucket too.
		assertEquals(200, prop.getProperty("timeattack.ranking.Standard.10.lines.0", -1));

		TimeAttackMode dest = new TimeAttackMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadRanking(dest, prop, "Standard");

		int[][] destLines = (int[][]) readField(dest, "rankingLines");
		int[][] destTime = (int[][]) readField(dest, "rankingTime");
		int[][] destRollclear = (int[][]) readField(dest, "rankingRollclear");
		for(int t = 0; t < destLines.length; t++) {
			assertArrayEquals(rankingLines[t], destLines[t]);
			assertArrayEquals(rankingTime[t], destTime[t]);
			assertArrayEquals(rankingRollclear[t], destRollclear[t]);
		}
	}

	@Test
	void loadSettingAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "goaltype"));
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(false, readBoolean(mode, "big"));
		assertEquals(true, readBoolean(mode, "showsectiontime"),
				"TIME ATTACK defaults showsectiontime to ON (it's the whole point)");
		assertEquals(0, readInt(mode, "version"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderTimeAttackPrefix() throws Exception {
		TimeAttackMode source = new TimeAttackMode();
		setInt(source, "goaltype", 5);
		setInt(source, "startlevel", 9);
		setBoolean(source, "big", true);
		setBoolean(source, "showsectiontime", false);
		setInt(source, "version", 4);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(5, prop.getProperty("timeattack.gametype", -1));
		assertEquals(9, prop.getProperty("timeattack.startlevel", -1));
		assertEquals(true, prop.getProperty("timeattack.big", false));
		assertEquals(false, prop.getProperty("timeattack.showsectiontime", true));
		assertEquals(4, prop.getProperty("timeattack.version", -1));

		TimeAttackMode dest = new TimeAttackMode();
		invokeLoadSetting(dest, prop);
		assertEquals(5, readInt(dest, "goaltype"));
		assertEquals(9, readInt(dest, "startlevel"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(false, readBoolean(dest, "showsectiontime"));
		assertEquals(4, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(TimeAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(TimeAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(TimeAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(TimeAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(TimeAttackMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(TimeAttackMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadRanking(TimeAttackMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod(
				"loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(TimeAttackMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod(
				"saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeLoadSetting(TimeAttackMode mode, CustomProperties prop) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(TimeAttackMode mode, CustomProperties prop) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
