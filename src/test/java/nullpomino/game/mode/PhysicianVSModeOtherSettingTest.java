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
 * Pins {@link PhysicianVSMode}'s private {@code loadOtherSetting} /
 * {@code saveOtherSetting}, the per-player settings persistence under
 * the {@code physicianvs.*} key prefix. {@code bgmno} is shared (no
 * player suffix); the rest are namespaced with {@code .p&lt;playerID&gt;}.
 * The defaults baked into loadOtherSetting are: bgmno=0, enableSE=true,
 * useMap=false, mapSet=0, mapNumber=-1, presetNumber=0, speed=1,
 * hoverBlocks=40, flash=false.
 */
class PhysicianVSModeOtherSettingTest {

	@Test
	void loadOtherSettingReadsDocumentedDefaultsForMissingKeys() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		invokeLoadOther(mode, engine, new CustomProperties());

		assertEquals(0, getInt(mode, "bgmno"),
				"bgmno default 0");
		assertTrue(getBooleanArray(mode, "enableSE")[0],
				"enableSE default true");
		assertFalse(getBooleanArray(mode, "useMap")[0],
				"useMap default false");
		assertEquals(0, getIntArray(mode, "mapSet")[0],
				"mapSet default 0");
		assertEquals(-1, getIntArray(mode, "mapNumber")[0],
				"mapNumber default -1");
		assertEquals(0, getIntArray(mode, "presetNumber")[0],
				"presetNumber default 0");
		assertEquals(1, getIntArray(mode, "speed")[0],
				"speed default 1");
		assertEquals(40, getIntArray(mode, "hoverBlocks")[0],
				"hoverBlocks default 40");
		assertFalse(getBooleanArray(mode, "flash")[0],
				"flash default false");
	}

	@Test
	void loadOtherSettingReadsExistingKeysFromTheGivenProperties() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("physicianvs.bgmno", 7);
		prop.setProperty("physicianvs.enableSE.p0", false);
		prop.setProperty("physicianvs.useMap.p0", true);
		prop.setProperty("physicianvs.mapSet.p0", 3);
		prop.setProperty("physicianvs.mapNumber.p0", 9);
		prop.setProperty("physicianvs.presetNumber.p0", 2);
		prop.setProperty("physicianvs.speed.p0", 5);
		prop.setProperty("physicianvs.hoverBlocks.p0", 100);
		prop.setProperty("physicianvs.flash.p0", true);

		invokeLoadOther(mode, engine, prop);

		assertEquals(7, getInt(mode, "bgmno"));
		assertFalse(getBooleanArray(mode, "enableSE")[0]);
		assertTrue(getBooleanArray(mode, "useMap")[0]);
		assertEquals(3, getIntArray(mode, "mapSet")[0]);
		assertEquals(9, getIntArray(mode, "mapNumber")[0]);
		assertEquals(2, getIntArray(mode, "presetNumber")[0]);
		assertEquals(5, getIntArray(mode, "speed")[0]);
		assertEquals(100, getIntArray(mode, "hoverBlocks")[0]);
		assertTrue(getBooleanArray(mode, "flash")[0]);
	}

	@Test
	void saveOtherSettingWritesAllPerPlayerKeysUnderPhysicianvsPrefix() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 1);

		setInt(mode, "bgmno", 5);
		getBooleanArray(mode, "enableSE")[1] = false;
		getBooleanArray(mode, "useMap")[1] = true;
		getIntArray(mode, "mapSet")[1] = 4;
		getIntArray(mode, "mapNumber")[1] = 8;
		getIntArray(mode, "presetNumber")[1] = 6;
		getIntArray(mode, "speed")[1] = 9;
		getIntArray(mode, "hoverBlocks")[1] = 50;
		getBooleanArray(mode, "flash")[1] = true;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(mode, engine, prop);

		assertEquals(5, prop.getProperty("physicianvs.bgmno", -1));
		assertEquals(false, prop.getProperty("physicianvs.enableSE.p1", true));
		assertEquals(true, prop.getProperty("physicianvs.useMap.p1", false));
		assertEquals(4, prop.getProperty("physicianvs.mapSet.p1", -1));
		assertEquals(8, prop.getProperty("physicianvs.mapNumber.p1", -1));
		assertEquals(6, prop.getProperty("physicianvs.presetNumber.p1", -1));
		assertEquals(9, prop.getProperty("physicianvs.speed.p1", -1));
		assertEquals(50, prop.getProperty("physicianvs.hoverBlocks.p1", -1));
		assertEquals(true, prop.getProperty("physicianvs.flash.p1", false));
	}

	@Test
	void saveAndLoadOtherSettingRoundTripPerPlayerSlot() throws Exception {
		PhysicianVSMode source = new PhysicianVSMode();
		GameManager sourceMgr = new GameManager(new EventReceiver());
		source.modeInit(sourceMgr);
		GameEngine sourceEngine = freshEngine(source, sourceMgr, 0);

		setInt(source, "bgmno", 11);
		getBooleanArray(source, "enableSE")[0] = false;
		getIntArray(source, "speed")[0] = 7;
		getIntArray(source, "hoverBlocks")[0] = 25;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		PhysicianVSMode dest = new PhysicianVSMode();
		GameManager destMgr = new GameManager(new EventReceiver());
		dest.modeInit(destMgr);
		GameEngine destEngine = freshEngine(dest, destMgr, 0);

		invokeLoadOther(dest, destEngine, prop);

		assertEquals(11, getInt(dest, "bgmno"));
		assertFalse(getBooleanArray(dest, "enableSE")[0]);
		assertEquals(7, getIntArray(dest, "speed")[0]);
		assertEquals(25, getIntArray(dest, "hoverBlocks")[0]);
	}

	@Test
	void perPlayerKeysAreNamespacedByPlayerSuffix() throws Exception {
		// Save under p0 and read back with p1 -> p1 gets defaults, p0
		// gets the values; this is the property-key namespacing.
		PhysicianVSMode source = new PhysicianVSMode();
		GameManager sourceMgr = new GameManager(new EventReceiver());
		source.modeInit(sourceMgr);
		GameEngine sourceEngine = freshEngine(source, sourceMgr, 0);

		getIntArray(source, "speed")[0] = 9;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		// p1 doesn't appear in prop -> p1 should fall back to defaults.
		assertEquals(-1, prop.getProperty("physicianvs.speed.p1", -1),
				"per-player keys are p0/p1 namespaced, no p1 written");
		assertEquals(9, prop.getProperty("physicianvs.speed.p0", -1));
	}

	private static GameEngine freshEngine(PhysicianVSMode mode, GameManager manager,
			int playerID) {
		manager.mode = mode;
		manager.init();
		manager.engine[playerID].init();
		return manager.engine[playerID];
	}

	private static void invokeLoadOther(PhysicianVSMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = PhysicianVSMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(PhysicianVSMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = PhysicianVSMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static int getInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
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
