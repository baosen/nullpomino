package nullpomino.game.mode;

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
 * Pins the headless slice of {@link TechnicianMode}: registry surface,
 * the AbstractMarathonMode contract overrides, the playerInit defaults
 * (which include the time-attack 'goal' and 'levelTimer' bookkeeping),
 * the level-21 gravity-table cell lookup via setSpeed, and the
 * loadSetting / saveSetting round-trip with the technician-only
 * 'gametype' extra written *before* core settings.
 */
class TechnicianModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("TECHNICIAN", new TechnicianMode().getName());
	}

	@Test
	void propertyPrefixIsTechnicianAndGameTypeCountIsFive() throws Exception {
		TechnicianMode mode = new TechnicianMode();

		assertEquals("technician", invokeStringMethod(mode, "getPropertyPrefix"));
		// TECHNICIAN ships five ranking buckets (one per game-type variant).
		assertEquals(5, invokeIntMethod(mode, "getGameTypeCount"));
	}

	@Test
	void playerInitInstallsFreshTimeAttackBookkeepingAndAllocatesRankingTables() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		Field menuTime = AbstractMode.class.getDeclaredField("menuTime");
		menuTime.setAccessible(true);
		Field menuCursor = AbstractMode.class.getDeclaredField("menuCursor");
		menuCursor.setAccessible(true);
		assertEquals(0, menuTime.getInt(mode));
		assertEquals(0, menuCursor.getInt(mode));

		// Inherited per-piece scoring counters.
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "lastcombo"));
		assertEquals(0, readInt(mode, "lastpiece"));
		assertEquals(false, readBoolean(mode, "lastb2b"));
		assertEquals(0, readInt(mode, "rolltime"));

		// TECHNICIAN-specific time-attack fields.
		assertEquals(0, readInt(mode, "goal"));
		assertEquals(0, readInt(mode, "levelTimer"));
		assertEquals(false, readBoolean(mode, "levelTimeOut"));
		assertEquals(0, readInt(mode, "totalTimer"));
		assertEquals(0, readInt(mode, "lastgoal"));
		assertEquals(0, readInt(mode, "lasttimebonus"));
		assertEquals(0, readInt(mode, "regretdispframe"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		// AbstractMarathonMode.allocateRankingArrays uses
		// [getGameTypeCount=5][RANKING_MAX=10].
		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertNotNull(rankingScore);
		assertEquals(5, rankingScore.length);
		assertEquals(10, rankingScore[0].length);

		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
	}

	@Test
	void setSpeedReadsGravityTableCellsAndClampsLevelToTableBounds() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);

		// Level 0 → gravity 1 / denominator 63 (slowest classic step,
		// shared with MARATHON's table head).
		engine.statistics.level = 0;
		invokeSetSpeed(mode, engine);
		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);

		// Above-bounds clamps to the last cell (-1 sentinel = 20G).
		engine.statistics.level = 9999;
		invokeSetSpeed(mode, engine);
		assertEquals(-1, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);

		// Negative level clamps to row 0.
		engine.statistics.level = -50;
		invokeSetSpeed(mode, engine);
		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);
	}

	@Test
	void loadSettingReadsCoreSettingsAndTheTechnicianGameTypeExtra() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		CustomProperties prop = new CustomProperties();
		prop.setProperty("technician.gametype", 4);
		prop.setProperty("technician.startlevel", 7);
		prop.setProperty("technician.tspinEnableType", 2);
		prop.setProperty("technician.enableTSpin", false);
		prop.setProperty("technician.big", true);

		invokeLoadSetting(mode, prop);

		assertEquals(4, readInt(mode, "goaltype"));
		assertEquals(7, readInt(mode, "startlevel"));
		assertEquals(2, readInt(mode, "tspinEnableType"));
		assertEquals(false, readBoolean(mode, "enableTSpin"));
		assertEquals(true, readBoolean(mode, "big"));
	}

	@Test
	void loadSettingDefaultsTechnicianGoalTypeToZeroWhenKeyAbsent() throws Exception {
		TechnicianMode mode = new TechnicianMode();

		invokeLoadSetting(mode, new CustomProperties());

		assertEquals(0, readInt(mode, "goaltype"),
				"goaltype defaults to the legacy 'normal' preset (index 0)");
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderTechnicianPrefix() throws Exception {
		TechnicianMode source = new TechnicianMode();
		setInt(source, "goaltype", 3);
		setInt(source, "startlevel", 12);
		setInt(source, "tspinEnableType", 0);
		setBoolean(source, "enableTSpin", false);
		setBoolean(source, "big", true);
		setInt(source, "version", 6);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		assertEquals(3, prop.getProperty("technician.gametype", -1));
		assertEquals(12, prop.getProperty("technician.startlevel", -1));
		assertEquals(0, prop.getProperty("technician.tspinEnableType", -1));
		assertEquals(true, prop.getProperty("technician.big", false));
		assertEquals(6, prop.getProperty("technician.version", -1));

		TechnicianMode dest = new TechnicianMode();
		invokeLoadSetting(dest, prop);
		assertEquals(3, readInt(dest, "goaltype"));
		assertEquals(12, readInt(dest, "startlevel"));
		assertEquals(0, readInt(dest, "tspinEnableType"));
		assertEquals(false, readBoolean(dest, "enableTSpin"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(6, readInt(dest, "version"));
	}

	private static GameEngine freshEngine(TechnicianMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(TechnicianMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(TechnicianMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(TechnicianMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(TechnicianMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(TechnicianMode mode, String name, boolean value) throws Exception {
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

	private static int invokeIntMethod(TechnicianMode mode, String name) throws Exception {
		Method m = findMethod(mode.getClass(), name);
		m.setAccessible(true);
		return (int) m.invoke(mode);
	}

	private static String invokeStringMethod(TechnicianMode mode, String name) throws Exception {
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

	private static void invokeLoadSetting(TechnicianMode mode, CustomProperties prop) throws Exception {
		Method m = TechnicianMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(TechnicianMode mode, CustomProperties prop) throws Exception {
		Method m = TechnicianMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSetSpeed(TechnicianMode mode, GameEngine engine) throws Exception {
		Method m = TechnicianMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
