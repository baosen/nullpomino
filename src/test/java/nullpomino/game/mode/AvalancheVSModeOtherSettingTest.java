package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheVSMode}'s private {@code loadOtherSetting} /
 * {@code saveOtherSetting} per-player settings persistence under the
 * {@code avalanchevs.*} prefix. The save/load surface is asymmetric
 * — load reads {@code ojamaRate} and {@code ojamaHard} (with defaults
 * 120 and 0), but the parent's saveOtherSetting at {@code prefix=""}
 * is responsible for writing those back, while AvalancheVSMode itself
 * only writes the fever / zen-keshi specific fields. The replay-mode
 * debug flag {@code xyzzy=573} is set via the {@code avalanchevs
 * .debugcheatenable} property under replay only.
 */
class AvalancheVSModeOtherSettingTest {

	@Test
	void loadOtherSettingReadsDocumentedDefaults() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		invokeLoadOther(mode, engine, new CustomProperties());

		assertEquals(120, getIntArray(mode, "ojamaRate")[0],
				"ojamaRate default 120");
		assertEquals(0, getIntArray(mode, "ojamaHard")[0],
				"ojamaHard default 0");
		assertEquals(0, getIntArray(mode, "feverThreshold")[0]);
		assertEquals(15, getIntArray(mode, "feverTimeMin")[0],
				"feverTimeMin default 15");
		assertEquals(30, getIntArray(mode, "feverTimeMax")[0],
				"feverTimeMax default 30");
		assertTrue(getBooleanArray(mode, "feverShowMeter")[0],
				"feverShowMeter default true");
		assertTrue(getBooleanArray(mode, "ojamaMeter")[0],
				"ojamaMeter default true");
		assertEquals(0, getIntArray(mode, "feverPointCriteria")[0]);
		assertEquals(0, getIntArray(mode, "feverTimeCriteria")[0]);
		assertEquals(10, getIntArray(mode, "feverPower")[0],
				"feverPower default 10");
		assertEquals(5, getIntArray(mode, "feverChainStart")[0],
				"feverChainStart default 5");
		assertEquals(4, getIntArray(mode, "zenKeshiChain")[0],
				"zenKeshiChain default 4");
		assertEquals(30, getIntArray(mode, "zenKeshiOjama")[0],
				"zenKeshiOjama default 30");
	}

	@Test
	void loadOtherSettingReadsAvalanchevsKeysFromProperties() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevs.ojamaRate.p0", 200);
		prop.setProperty("avalanchevs.ojamaHard.p0", 3);
		prop.setProperty("avalanchevs.feverThreshold.p0", 50);
		prop.setProperty("avalanchevs.feverTimeMin.p0", 20);
		prop.setProperty("avalanchevs.feverTimeMax.p0", 60);
		prop.setProperty("avalanchevs.feverShowMeter.p0", false);
		prop.setProperty("avalanchevs.ojamaMeter.p0", false);
		prop.setProperty("avalanchevs.feverPointCriteria.p0", 2);
		prop.setProperty("avalanchevs.feverTimeCriteria.p0", 1);
		prop.setProperty("avalanchevs.feverPower.p0", 25);
		prop.setProperty("avalanchevs.feverChainStart.p0", 7);
		prop.setProperty("avalanchevs.zenKeshiChain.p0", 6);
		prop.setProperty("avalanchevs.zenKeshiOjama.p0", 50);

		invokeLoadOther(mode, engine, prop);

		assertEquals(200, getIntArray(mode, "ojamaRate")[0]);
		assertEquals(3, getIntArray(mode, "ojamaHard")[0]);
		assertEquals(50, getIntArray(mode, "feverThreshold")[0]);
		assertEquals(20, getIntArray(mode, "feverTimeMin")[0]);
		assertEquals(60, getIntArray(mode, "feverTimeMax")[0]);
		assertFalse(getBooleanArray(mode, "feverShowMeter")[0]);
		assertFalse(getBooleanArray(mode, "ojamaMeter")[0]);
		assertEquals(2, getIntArray(mode, "feverPointCriteria")[0]);
		assertEquals(1, getIntArray(mode, "feverTimeCriteria")[0]);
		assertEquals(25, getIntArray(mode, "feverPower")[0]);
		assertEquals(7, getIntArray(mode, "feverChainStart")[0]);
		assertEquals(6, getIntArray(mode, "zenKeshiChain")[0]);
		assertEquals(50, getIntArray(mode, "zenKeshiOjama")[0]);
	}

	@Test
	void saveOtherSettingWritesAllFeverAndZenKeshiKeysButNotOjamaRateOrHard() throws Exception {
		// AvalancheVSMode.saveOtherSetting writes the fever and zen-keshi
		// fields directly. ojamaRate / ojamaHard go through the parent
		// AvalancheVSDummyMode.saveOtherSetting so they share key shapes
		// with the dummy/sibling modes — AvalancheVSMode itself does NOT
		// write them again.
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 1);

		getIntArray(mode, "feverThreshold")[1] = 80;
		getIntArray(mode, "feverTimeMin")[1] = 30;
		getIntArray(mode, "feverTimeMax")[1] = 90;
		getBooleanArray(mode, "feverShowMeter")[1] = false;
		getBooleanArray(mode, "ojamaMeter")[1] = false;
		getIntArray(mode, "feverPointCriteria")[1] = 3;
		getIntArray(mode, "feverTimeCriteria")[1] = 2;
		getIntArray(mode, "feverPower")[1] = 99;
		getIntArray(mode, "feverChainStart")[1] = 9;
		getIntArray(mode, "zenKeshiChain")[1] = 8;
		getIntArray(mode, "zenKeshiOjama")[1] = 70;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(mode, engine, prop);

		assertEquals(80, prop.getProperty("avalanchevs.feverThreshold.p1", -1));
		assertEquals(30, prop.getProperty("avalanchevs.feverTimeMin.p1", -1));
		assertEquals(90, prop.getProperty("avalanchevs.feverTimeMax.p1", -1));
		assertEquals(false, prop.getProperty("avalanchevs.feverShowMeter.p1", true));
		assertEquals(false, prop.getProperty("avalanchevs.ojamaMeter.p1", true));
		assertEquals(3, prop.getProperty("avalanchevs.feverPointCriteria.p1", -1));
		assertEquals(2, prop.getProperty("avalanchevs.feverTimeCriteria.p1", -1));
		assertEquals(99, prop.getProperty("avalanchevs.feverPower.p1", -1));
		assertEquals(9, prop.getProperty("avalanchevs.feverChainStart.p1", -1));
		assertEquals(8, prop.getProperty("avalanchevs.zenKeshiChain.p1", -1));
		assertEquals(70, prop.getProperty("avalanchevs.zenKeshiOjama.p1", -1));
	}

	@Test
	void saveAndLoadOtherSettingRoundTripFeverAndZenKeshiFields() throws Exception {
		// Round-trip the fever/zen-keshi half. ojamaRate / ojamaHard
		// don't survive a save+load through saveOtherSetting alone —
		// that's the parent's responsibility.
		AvalancheVSMode source = new AvalancheVSMode();
		GameManager sourceMgr = new GameManager(new EventReceiver());
		source.modeInit(sourceMgr);
		GameEngine sourceEngine = freshEngine(source, sourceMgr, 0);

		getIntArray(source, "feverThreshold")[0] = 75;
		getIntArray(source, "feverPower")[0] = 18;
		getIntArray(source, "zenKeshiOjama")[0] = 45;
		getBooleanArray(source, "feverShowMeter")[0] = false;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		AvalancheVSMode dest = new AvalancheVSMode();
		GameManager destMgr = new GameManager(new EventReceiver());
		dest.modeInit(destMgr);
		GameEngine destEngine = freshEngine(dest, destMgr, 0);

		invokeLoadOther(dest, destEngine, prop);

		assertEquals(75, getIntArray(dest, "feverThreshold")[0]);
		assertEquals(18, getIntArray(dest, "feverPower")[0]);
		assertEquals(45, getIntArray(dest, "zenKeshiOjama")[0]);
		assertFalse(getBooleanArray(dest, "feverShowMeter")[0]);
	}

	@Test
	void debugCheatEnableUnderReplayModeSetsXyzzyToFiveSevenThree() throws Exception {
		// avalanchevs.debugcheatenable=true under replay mode -> xyzzy=573.
		// GameManager.init() resets replayMode to false when replayProp
		// is null, so we set replayMode AFTER engine init.
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);
		manager.replayMode = true;

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevs.debugcheatenable", true);

		invokeLoadOther(mode, engine, prop);

		assertEquals(573, getInt(mode, "xyzzy"),
				"debugcheatenable under replay -> xyzzy = 573");
	}

	@Test
	void debugCheatEnableOutsideReplayModeIsIgnored() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);
		manager.replayMode = false;

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevs.debugcheatenable", true);

		invokeLoadOther(mode, engine, prop);

		assertEquals(0, getInt(mode, "xyzzy"),
				"debugcheatenable outside replay -> xyzzy stays at 0");
	}

	private static GameEngine freshEngine(AvalancheVSMode mode, GameManager manager,
			int playerID) {
		manager.mode = mode;
		manager.init();
		manager.engine[playerID].init();
		return manager.engine[playerID];
	}

	private static void invokeLoadOther(AvalancheVSMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(AvalancheVSMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static int getInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (int[]) f.get(obj);
	}

	private static boolean[] getBooleanArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (boolean[]) f.get(obj);
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
}
