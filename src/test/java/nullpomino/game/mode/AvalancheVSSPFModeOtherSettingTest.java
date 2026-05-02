package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheVSSPFMode}'s private {@code loadOtherSetting} /
 * {@code saveOtherSetting} per-player persistence under the
 * {@code avalanchevsspf.*} prefix. The unusual surface here:
 * {@code ojamaHard[playerID] = 4} is hard-coded in loadOtherSetting
 * with no fallback to a property — SPF always uses ojamaHard=4
 * regardless of what the .properties file holds. ojamaRate, ojamaCountdown,
 * dropSet, and dropMap are read normally with their own defaults.
 * saveOtherSetting writes ojamaCountdown, dropSet, dropMap directly;
 * ojamaRate goes through super.saveOtherSetting with the "spf" suffix
 * and ojamaHard is never written (since it's a hardcoded constant).
 */
class AvalancheVSSPFModeOtherSettingTest {

	@Test
	void loadOtherSettingHardcodesOjamaHardToFourRegardlessOfProperty() throws Exception {
		// Even with avalanchevsspf.ojamaHard.p0=99 in the props,
		// SPF mode unconditionally sets ojamaHard[0] = 4.
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsspf.ojamaHard.p0", 99);

		invokeLoadOther(mode, engine, prop);

		assertEquals(4, getIntArray(mode, "ojamaHard")[0],
				"ojamaHard is hard-coded to 4 in SPF; "
						+ "the avalanchevsspf.ojamaHard property is ignored");
	}

	@Test
	void loadOtherSettingReadsDocumentedDefaults() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		invokeLoadOther(mode, engine, new CustomProperties());

		assertEquals(4, getIntArray(mode, "ojamaHard")[0],
				"ojamaHard hard-coded 4");
		assertEquals(120, getIntArray(mode, "ojamaRate")[0],
				"ojamaRate default 120");
		assertEquals(3, getIntArray(mode, "ojamaCountdown")[0],
				"ojamaCountdown default 3");
		assertEquals(4, getIntArray(mode, "dropSet")[0],
				"dropSet default 4");
		assertEquals(0, getIntArray(mode, "dropMap")[0],
				"dropMap default 0");
	}

	@Test
	void loadOtherSettingReadsPropertiesForNonHardcodedFields() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 0);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevsspf.ojamaRate.p0", 200);
		prop.setProperty("avalanchevsspf.ojamaCountdown.p0", 8);
		prop.setProperty("avalanchevsspf.dropSet.p0", 7);
		prop.setProperty("avalanchevsspf.dropMap.p0", 3);

		invokeLoadOther(mode, engine, prop);

		assertEquals(200, getIntArray(mode, "ojamaRate")[0]);
		assertEquals(8, getIntArray(mode, "ojamaCountdown")[0]);
		assertEquals(7, getIntArray(mode, "dropSet")[0]);
		assertEquals(3, getIntArray(mode, "dropMap")[0]);
	}

	@Test
	void saveOtherSettingWritesOjamaCountdownDropSetDropMap() throws Exception {
		// saveOtherSetting writes ojamaCountdown/dropSet/dropMap directly.
		// ojamaHard is also written via super.saveOtherSetting(...,"spf"),
		// but always with the hardcoded value 4 since load forces it
		// regardless of property input.
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		GameEngine engine = freshEngine(mode, manager, 1);

		// Force ojamaHard to a non-default value to confirm the
		// save-out path uses whatever's in the array (not the hardcoded
		// constant) — but in real flow load always pins it to 4.
		getIntArray(mode, "ojamaHard")[1] = 4;
		getIntArray(mode, "ojamaCountdown")[1] = 6;
		getIntArray(mode, "dropSet")[1] = 5;
		getIntArray(mode, "dropMap")[1] = 2;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(mode, engine, prop);

		assertEquals(6, prop.getProperty("avalanchevsspf.ojamaCountdown.p1", -1));
		assertEquals(5, prop.getProperty("avalanchevsspf.dropSet.p1", -1));
		assertEquals(2, prop.getProperty("avalanchevsspf.dropMap.p1", -1));
		assertEquals(4, prop.getProperty("avalanchevsspf.ojamaHard.p1", -1),
				"ojamaHard goes through parent's super.saveOtherSetting "
						+ "with 'spf' suffix; the value is always 4 because "
						+ "loadOtherSetting hardcodes it regardless of input");
	}

	@Test
	void saveAndLoadOtherSettingRoundTripSPFSpecificFields() throws Exception {
		AvalancheVSSPFMode source = new AvalancheVSSPFMode();
		GameManager sourceMgr = new GameManager(new EventReceiver());
		source.modeInit(sourceMgr);
		GameEngine sourceEngine = freshEngine(source, sourceMgr, 0);

		getIntArray(source, "ojamaCountdown")[0] = 9;
		getIntArray(source, "dropSet")[0] = 6;
		getIntArray(source, "dropMap")[0] = 4;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, sourceEngine, prop);

		AvalancheVSSPFMode dest = new AvalancheVSSPFMode();
		GameManager destMgr = new GameManager(new EventReceiver());
		dest.modeInit(destMgr);
		GameEngine destEngine = freshEngine(dest, destMgr, 0);

		invokeLoadOther(dest, destEngine, prop);

		assertEquals(9, getIntArray(dest, "ojamaCountdown")[0]);
		assertEquals(6, getIntArray(dest, "dropSet")[0]);
		assertEquals(4, getIntArray(dest, "dropMap")[0]);
		assertEquals(4, getIntArray(dest, "ojamaHard")[0],
				"ojamaHard remains hardcoded 4 across save/load");
	}

	private static GameEngine freshEngine(AvalancheVSSPFMode mode,
			GameManager manager, int playerID) {
		manager.mode = mode;
		manager.init();
		manager.engine[playerID].init();
		return manager.engine[playerID];
	}

	private static void invokeLoadOther(AvalancheVSSPFMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSSPFMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(AvalancheVSSPFMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = AvalancheVSSPFMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return (int[]) f.get(obj);
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
