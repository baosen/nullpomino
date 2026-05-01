package nullpomino.game.mode;

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
 * Pins the headless slice of {@link ExtremeMode}: registry surface,
 * the AbstractMarathonMode contract overrides, the playerInit defaults
 * (which include the staff-roll wiring that distinguishes this mode
 * from MARATHON / MARATHON+), and the loadSetting / saveSetting
 * round-trip with the extreme-only 'endless' extra.
 */
class ExtremeModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("EXTREME", new ExtremeMode().getName());
	}

	@Test
	void propertyPrefixIsExtremeAndGameTypeCountIsTwo() throws Exception {
		ExtremeMode mode = new ExtremeMode();

		assertEquals("extreme", invokeStringMethod(mode, "getPropertyPrefix"));
		// EXTREME uses RANKING_TYPE = 2 → two game-type buckets (one for
		// each ranking flavour the mode tracks).
		assertEquals(2, invokeIntMethod(mode, "getGameTypeCount"));
	}

	@Test
	void playerInitInstallsFreshScoringStateAndAllocatesRankingTables() throws Exception {
		ExtremeMode mode = new ExtremeMode();
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
		assertEquals(0, readInt(mode, "lastcombo"));
		assertEquals(0, readInt(mode, "lastpiece"));
		assertEquals(false, readBoolean(mode, "lastb2b"));
		assertEquals(0, readInt(mode, "rolltime"),
				"rolltime starts at 0 — this is one of the EXTREME-only resets");
		assertEquals(-1, readInt(mode, "rankingRank"));

		// AbstractMarathonMode.allocateRankingArrays should leave each table
		// dimensioned [GAMETYPE_MAX=2][RANKING_MAX=10].
		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(2, rankingScore.length);
		assertEquals(10, rankingScore[0].length);
	}

	@Test
	void playerInitWiresEngineForStaffRollAndPaintsFrameRed() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		// EXTREME pulls the player past level 19 into the staff-roll
		// sequence, so all three roll flags must flip on.
		assertTrue(engine.staffrollEnable,
				"staff roll must be enabled for the post-clear EXTREME credits sequence");
		assertTrue(engine.staffrollNoDeath,
				"the credits roll must keep the player alive on death");
		assertTrue(engine.staffrollEnableStatistics,
				"the credits roll must keep statistics ticking so the result screen has data");

		// Frame is RED for EXTREME (vs GREEN for MARATHON, GRAY for M+).
		assertEquals(GameEngine.FRAME_COLOR_RED, engine.framecolor);
	}

	@Test
	void loadSettingReadsExtremeEndlessExtraAndCoreSettings() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		CustomProperties prop = new CustomProperties();
		prop.setProperty("extreme.startlevel", 6);
		prop.setProperty("extreme.endless", true);
		prop.setProperty("extreme.tspinEnableType", 0);
		prop.setProperty("extreme.enableTSpin", false);
		prop.setProperty("extreme.big", true);

		invokeLoadSetting(mode, prop);

		assertEquals(6, readInt(mode, "startlevel"));
		assertTrue(readBoolean(mode, "endless"),
				"endless flag must reflect the extreme.endless prop key");
		assertEquals(0, readInt(mode, "tspinEnableType"));
		assertEquals(false, readBoolean(mode, "enableTSpin"));
		assertEquals(true, readBoolean(mode, "big"));
	}

	@Test
	void loadSettingDefaultsExtremeEndlessToFalseWhenKeyAbsent() throws Exception {
		ExtremeMode mode = new ExtremeMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(false, readBoolean(mode, "endless"),
				"endless default must stay false so a fresh config caps the player at the natural EXTREME ending");
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderExtremePrefix() throws Exception {
		ExtremeMode source = new ExtremeMode();
		setInt(source, "startlevel", 11);
		setBoolean(source, "endless", true);
		setInt(source, "tspinEnableType", 2);
		setBoolean(source, "enableTSpin", false);
		setBoolean(source, "big", true);
		setInt(source, "version", 4);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(11, prop.getProperty("extreme.startlevel", -1));
		assertEquals(true, prop.getProperty("extreme.endless", false));
		assertEquals(2, prop.getProperty("extreme.tspinEnableType", -1));
		assertEquals(true, prop.getProperty("extreme.big", false));
		assertEquals(4, prop.getProperty("extreme.version", -1));

		ExtremeMode dest = new ExtremeMode();
		invokeLoadSetting(dest, prop);
		assertEquals(11, readInt(dest, "startlevel"));
		assertTrue(readBoolean(dest, "endless"));
		assertEquals(2, readInt(dest, "tspinEnableType"));
		assertEquals(false, readBoolean(dest, "enableTSpin"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(4, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(ExtremeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(ExtremeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ExtremeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(ExtremeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(ExtremeMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ExtremeMode mode, String name, boolean value) throws Exception {
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

	private static int invokeIntMethod(ExtremeMode mode, String name) throws Exception {
		Method m = findMethod(mode.getClass(), name);
		m.setAccessible(true);
		return (int) m.invoke(mode);
	}

	private static String invokeStringMethod(ExtremeMode mode, String name) throws Exception {
		Method m = findMethod(mode.getClass(), name);
		m.setAccessible(true);
		return (String) m.invoke(mode);
	}

	private static Method findMethod(Class<?> cls, String name) throws NoSuchMethodException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredMethod(name);
			} catch(NoSuchMethodException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchMethodException(name);
	}

	private static void invokeLoadSetting(ExtremeMode mode, CustomProperties prop) throws Exception {
		Method m = ExtremeMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(ExtremeMode mode, CustomProperties prop) throws Exception {
		Method m = ExtremeMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
