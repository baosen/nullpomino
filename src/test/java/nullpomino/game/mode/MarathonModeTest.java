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
 * Pins the headless slice of {@link MarathonMode}: the registry-facing
 * surface, the property-prefix / game-type-count contract that pulls
 * from {@link AbstractMarathonMode}, the loadSetting / saveSetting
 * round-trip with the marathon-only gametype extra, and the level →
 * gravity table lookup driven by setSpeed.
 */
class MarathonModeTest {

	@Test
	void getNameReturnsLegacyConstantUsedByModeRegistry() {
		assertEquals("MARATHON", new MarathonMode().getName());
	}

	@Test
	void propertyPrefixIsMarathonAndGameTypeCountIsThree() throws Exception {
		MarathonMode mode = new MarathonMode();

		assertEquals("marathon", invokeStringMethod(mode, "getPropertyPrefix"));
		assertEquals(3, invokeIntMethod(mode, "getGameTypeCount"));
	}

	@Test
	void loadSettingReadsCoreSettingsAndTheGoalTypeExtra() throws Exception {
		MarathonMode mode = new MarathonMode();
		CustomProperties prop = new CustomProperties();
		prop.setProperty("marathon.startlevel", 8);
		prop.setProperty("marathon.gametype", 2);
		prop.setProperty("marathon.tspinEnableType", 2);
		prop.setProperty("marathon.enableTSpin", false);
		prop.setProperty("marathon.big", true);
		prop.setProperty("marathon.version", 1);

		invokeLoadSetting(mode, prop);

		assertEquals(8, readInt(mode, "startlevel"));
		assertEquals(2, readInt(mode, "goaltype"));
		assertEquals(2, readInt(mode, "tspinEnableType"));
		assertEquals(false, readBoolean(mode, "enableTSpin"));
		assertEquals(true, readBoolean(mode, "big"));
		assertEquals(1, readInt(mode, "version"));
	}

	@Test
	void loadSettingFallsBackToDocumentedDefaultsForMissingKeys() throws Exception {
		MarathonMode mode = new MarathonMode();

		invokeLoadSetting(mode, new CustomProperties());

		// Inherited defaults from AbstractMarathonMode...
		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(1, readInt(mode, "tspinEnableType"));
		assertEquals(true, readBoolean(mode, "enableTSpin"));
		assertEquals(true, readBoolean(mode, "enableB2B"));
		assertEquals(true, readBoolean(mode, "enableCombo"));
		// ...plus the marathon-only goaltype extra defaulting to 0 (the
		// classic 150-line preset).
		assertEquals(0, readInt(mode, "goaltype"));
	}

	@Test
	void saveSettingAndLoadSettingRoundTripUnderMarathonPrefix() throws Exception {
		MarathonMode source = new MarathonMode();
		setInt(source, "startlevel", 9);
		setInt(source, "goaltype", 2);
		setInt(source, "tspinEnableType", 0);
		setBoolean(source, "enableTSpin", false);
		setBoolean(source, "big", true);
		setInt(source, "version", 7);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(source, prop);

		// Spot-check that the keys land under "marathon.*".
		assertEquals(9, prop.getProperty("marathon.startlevel", -1));
		assertEquals(2, prop.getProperty("marathon.gametype", -1));
		assertEquals(true, prop.getProperty("marathon.big", false));

		MarathonMode dest = new MarathonMode();
		invokeLoadSetting(dest, prop);
		assertEquals(9, readInt(dest, "startlevel"));
		assertEquals(2, readInt(dest, "goaltype"));
		assertEquals(false, readBoolean(dest, "enableTSpin"));
		assertEquals(true, readBoolean(dest, "big"));
		assertEquals(7, readInt(dest, "version"));
	}

	@Test
	void setSpeedClampsLevelToTableBoundsAndReadsTheRightCells() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);

		// Level 0 → gravity 1 / denominator 63 (the slowest classic step).
		engine.statistics.level = 0;
		mode.setSpeed(engine);
		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);

		// Level 13 → first row in the maxgravity zone.
		engine.statistics.level = 13;
		mode.setSpeed(engine);
		assertEquals(465, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);

		// Above-bounds level clamps to the last row (-1 sentinel = 20G).
		engine.statistics.level = 9999;
		mode.setSpeed(engine);
		assertEquals(-1, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);

		// Negative level clamps to zero (slowest row).
		engine.statistics.level = -50;
		mode.setSpeed(engine);
		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);
	}

	@Test
	void playerInitInstallsFreshScoringStateAndAllocatesRankingArrays() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "lastcombo"));
		assertEquals(0, readInt(mode, "bgmlv"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertNotNull(readField(mode, "rankingScore"),
				"playerInit must allocate the ranking arrays via "
						+ "AbstractMarathonMode.allocateRankingArrays");
		assertNotNull(readField(mode, "rankingLines"));
		assertNotNull(readField(mode, "rankingTime"));
		// Ranking arrays are dimensioned [GAMETYPE_MAX][RANKING_MAX].
		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		assertEquals(3, rankingScore.length, "GAMETYPE_MAX is 3");
		assertEquals(10, rankingScore[0].length, "RANKING_MAX is 10");
		assertEquals(GameEngine.FRAME_COLOR_GREEN, engine.framecolor);
	}

	@Test
	void playerInitInReplayModeStampsMarathonEndlessLegacyFlagOntoGoaltype() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		// Pre-populate the manager's replayProp so playerInit takes the
		// replay branch and applies the marathon.endless legacy mapping.
		engine.owner.replayMode = true;
		engine.owner.replayProp.setProperty("marathon.endless", true);
		// version is left at default (0) so the legacy mapping fires.

		mode.playerInit(engine, 0);

		assertEquals(2, readInt(mode, "goaltype"),
				"a v0 replay with the legacy 'endless=true' flag must remap "
						+ "to goaltype 2");
	}

	@Test
	void playerInitTwiceLeavesRankingScoreArrayShapedConsistently() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);
		Object firstAlloc = readField(mode, "rankingScore");
		mode.playerInit(engine, 0);
		Object secondAlloc = readField(mode, "rankingScore");

		assertNotNull(firstAlloc);
		assertNotNull(secondAlloc);
		// allocateRankingArrays reallocates each time; both shapes must be
		// the same so subsequent updates / loads do not crash.
		assertEquals(((int[][]) firstAlloc).length, ((int[][]) secondAlloc).length);
		assertEquals(((int[][]) firstAlloc)[0].length, ((int[][]) secondAlloc)[0].length);
		assertTrue(true, "second playerInit must keep the ranking shape stable");
	}

	private static GameEngine freshEngine(MarathonMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(MarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(MarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(MarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(MarathonMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(MarathonMode mode, String name, boolean value) throws Exception {
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

	private static int invokeIntMethod(MarathonMode mode, String name) throws Exception {
		Method m = findMethod(mode.getClass(), name);
		m.setAccessible(true);
		return (int) m.invoke(mode);
	}

	private static String invokeStringMethod(MarathonMode mode, String name) throws Exception {
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

	private static void invokeLoadSetting(MarathonMode mode, CustomProperties prop) throws Exception {
		Method m = MarathonMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(MarathonMode mode, CustomProperties prop) throws Exception {
		Method m = MarathonMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}
}
