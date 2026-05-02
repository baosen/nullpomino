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
 * Pins {@link SPFMode}'s private {@code loadOtherSetting} /
 * {@code saveOtherSetting} per-player settings persistence. Most keys
 * use the {@code spfvs.*} prefix; {@code hurryupSeconds} reads/writes
 * under the shared {@code vsbattle.*} prefix because it predates the
 * SPF-specific keyspace and rest of the VS-battle family already
 * read/write that key. {@code bgmno} and {@code bigDisplay} are
 * shared across players (no .p suffix); the rest are per-player.
 */
class SPFModeOtherSettingTest {

	@Test
	void loadOtherSettingReadsDocumentedDefaults() throws Exception {
		// Empty properties -> every field falls back to its documented
		// default (no SPF key written yet).
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		invokeLoadOther(mode, engine, new CustomProperties());

		assertEquals(0, getInt(mode, "bgmno"));
		assertTrue(getBooleanArray(mode, "enableSE")[0],
				"enableSE default true");
		assertFalse(getBooleanArray(mode, "useMap")[0],
				"useMap default false");
		assertEquals(0, getIntArray(mode, "mapSet")[0]);
		assertEquals(-1, getIntArray(mode, "mapNumber")[0],
				"mapNumber default -1 (none)");
		assertEquals(0, getIntArray(mode, "presetNumber")[0]);
		assertEquals(5, getIntArray(mode, "ojamaCountdown")[0],
				"ojamaCountdown default 5");
		assertFalse(getBoolean(mode, "bigDisplay"),
				"bigDisplay default false");
		assertEquals(0, getIntArray(mode, "dropSet")[0]);
		assertEquals(0, getIntArray(mode, "dropMap")[0]);
		assertEquals(2, getIntArray(mode, "diamondPower")[0],
				"diamondPower default 2");
	}

	@Test
	void loadOtherSettingReadsSpfvsKeysFromProperties() throws Exception {
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("spfvs.bgmno", 4);
		prop.setProperty("spfvs.enableSE.p0", false);
		prop.setProperty("spfvs.useMap.p0", true);
		prop.setProperty("spfvs.mapSet.p0", 7);
		prop.setProperty("spfvs.mapNumber.p0", 12);
		prop.setProperty("spfvs.presetNumber.p0", 3);
		prop.setProperty("spfvs.ojamaHard.p0", 9);
		prop.setProperty("spfvs.bigDisplay", true);
		prop.setProperty("spfvs.dropSet.p0", 6);
		prop.setProperty("spfvs.dropMap.p0", 2);
		prop.setProperty("spfvs.rainbowPower.p0", 8);

		invokeLoadOther(mode, engine, prop);

		assertEquals(4, getInt(mode, "bgmno"));
		assertFalse(getBooleanArray(mode, "enableSE")[0]);
		assertTrue(getBooleanArray(mode, "useMap")[0]);
		assertEquals(7, getIntArray(mode, "mapSet")[0]);
		assertEquals(12, getIntArray(mode, "mapNumber")[0]);
		assertEquals(3, getIntArray(mode, "presetNumber")[0]);
		assertEquals(9, getIntArray(mode, "ojamaCountdown")[0],
				"ojamaCountdown reads from 'spfvs.ojamaHard.p0' "
						+ "(legacy key name despite the field rename)");
		assertTrue(getBoolean(mode, "bigDisplay"));
		assertEquals(6, getIntArray(mode, "dropSet")[0]);
		assertEquals(2, getIntArray(mode, "dropMap")[0]);
		assertEquals(8, getIntArray(mode, "diamondPower")[0],
				"diamondPower reads from 'spfvs.rainbowPower.p0' (legacy key)");
	}

	@Test
	void loadOtherSettingReadsHurryupSecondsFromVsbattlePrefix() throws Exception {
		// hurryupSeconds is under vsbattle.* not spfvs.* -- it predates
		// the SPF-specific keyspace and shares the VS-battle family key.
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("vsbattle.hurryupSeconds.p0", 180);

		invokeLoadOther(mode, engine, prop);

		assertEquals(180, getIntArray(mode, "hurryupSeconds")[0],
				"hurryupSeconds reads from vsbattle.* not spfvs.*");
	}

	@Test
	void saveOtherSettingWritesAllPerPlayerKeysUnderSpfvsPrefix() throws Exception {
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 1);

		setInt(mode, "bgmno", 6);
		getBooleanArray(mode, "enableSE")[1] = false;
		getIntArray(mode, "hurryupSeconds")[1] = 240;
		getBooleanArray(mode, "useMap")[1] = true;
		getIntArray(mode, "mapSet")[1] = 5;
		getIntArray(mode, "mapNumber")[1] = 11;
		getIntArray(mode, "presetNumber")[1] = 4;
		getIntArray(mode, "ojamaCountdown")[1] = 7;
		setBoolean(mode, "bigDisplay", true);
		getIntArray(mode, "dropSet")[1] = 9;
		getIntArray(mode, "dropMap")[1] = 3;
		getIntArray(mode, "diamondPower")[1] = 6;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(mode, engine, prop);

		assertEquals(6, prop.getProperty("spfvs.bgmno", -1));
		assertEquals(false, prop.getProperty("spfvs.enableSE.p1", true));
		assertEquals(240, prop.getProperty("vsbattle.hurryupSeconds.p1", -1),
				"hurryupSeconds writes to vsbattle.* (cross-family key)");
		assertEquals(true, prop.getProperty("spfvs.useMap.p1", false));
		assertEquals(5, prop.getProperty("spfvs.mapSet.p1", -1));
		assertEquals(11, prop.getProperty("spfvs.mapNumber.p1", -1));
		assertEquals(4, prop.getProperty("spfvs.presetNumber.p1", -1));
		assertEquals(7, prop.getProperty("spfvs.ojamaHard.p1", -1),
				"ojamaCountdown saves to legacy 'ojamaHard' key");
		assertEquals(true, prop.getProperty("spfvs.bigDisplay", false));
		assertEquals(9, prop.getProperty("spfvs.dropSet.p1", -1));
		assertEquals(3, prop.getProperty("spfvs.dropMap.p1", -1));
		assertEquals(6, prop.getProperty("spfvs.rainbowPower.p1", -1),
				"diamondPower saves to legacy 'rainbowPower' key");
	}

	@Test
	void saveAndLoadOtherSettingRoundTripPerPlayerSlot() throws Exception {
		SPFMode source = new SPFMode();
		GameManager sourceMgr = new GameManager(new EventReceiver());
		source.modeInit(sourceMgr);
		GameEngine sourceEngine = freshEngine(source, sourceMgr, 0);

		setInt(source, "bgmno", 11);
		getBooleanArray(source, "enableSE")[0] = false;
		getIntArray(source, "hurryupSeconds")[0] = 90;
		getIntArray(source, "ojamaCountdown")[0] = 8;
		getIntArray(source, "diamondPower")[0] = 4;
		setBoolean(source, "bigDisplay", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		SPFMode dest = new SPFMode();
		GameManager destMgr = new GameManager(new EventReceiver());
		dest.modeInit(destMgr);
		GameEngine destEngine = freshEngine(dest, destMgr, 0);

		invokeLoadOther(dest, destEngine, prop);

		assertEquals(11, getInt(dest, "bgmno"));
		assertFalse(getBooleanArray(dest, "enableSE")[0]);
		assertEquals(90, getIntArray(dest, "hurryupSeconds")[0]);
		assertEquals(8, getIntArray(dest, "ojamaCountdown")[0]);
		assertEquals(4, getIntArray(dest, "diamondPower")[0]);
		assertTrue(getBoolean(dest, "bigDisplay"));
	}

	private static GameEngine freshEngine(SPFMode mode, GameManager manager,
			int playerID) {
		manager.mode = mode;
		manager.init();
		manager.engine[playerID].init();
		return manager.engine[playerID];
	}

	private static void invokeLoadOther(SPFMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = SPFMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(SPFMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = SPFMode.class.getDeclaredMethod(
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

	private static boolean getBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
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
