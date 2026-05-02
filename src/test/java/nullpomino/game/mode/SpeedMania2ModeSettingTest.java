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
 * Pins {@link SpeedMania2Mode}'s {@code loadSetting}/{@code saveSetting} pair,
 * including the legacy (version &lt; 2) torikan path and the rule-name-keyed
 * (version &ge; 2) path.
 */
class SpeedMania2ModeSettingTest {

	// -----------------------------------------------------------------------
	// loadSetting — version >= 2 (rule-name-keyed torikan)
	// -----------------------------------------------------------------------

	@Test
	void loadSettingVersion2ReadsAllFieldsIncludingRuleNamedTorikan() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);
		setInt(mode, "version", 2);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("speedmania2.startlevel", 5);
		prop.setProperty("speedmania2.lvstopse", false);
		prop.setProperty("speedmania2.showsectiontime", true);
		prop.setProperty("speedmania2.big", true);
		prop.setProperty("speedmania2.torikan.STANDARD", 12000);
		prop.setProperty("speedmania2.gradedisp", true);

		invokeLoadSetting(mode, prop, "STANDARD");

		assertEquals(5, readInt(mode, "startlevel"));
		assertEquals(false, readBoolean(mode, "lvstopse"));
		assertEquals(true, readBoolean(mode, "showsectiontime"));
		assertEquals(true, readBoolean(mode, "big"));
		assertEquals(12000, readInt(mode, "torikan"));
		assertEquals(true, readBoolean(mode, "gradedisp"));
	}

	@Test
	void loadSettingVersion2UsesCLASSICDefaultWhenRuleNameContainsClassic() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);
		setInt(mode, "version", 2);
		// No torikan key for this rule -> falls back to CLASSIC default (8880)
		CustomProperties prop = new CustomProperties();

		invokeLoadSetting(mode, prop, "CLASSIC");

		assertEquals(8880, readInt(mode, "torikan"), "CLASSIC rule default torikan");
	}

	@Test
	void loadSettingVersion2UsesStandardDefaultForNonCLASSICRule() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);
		setInt(mode, "version", 2);
		// No torikan key for this rule -> falls back to DEFAULT_TORIKAN (10980)
		CustomProperties prop = new CustomProperties();

		invokeLoadSetting(mode, prop, "WORLD");

		assertEquals(10980, readInt(mode, "torikan"), "non-CLASSIC rule default torikan");
	}

	// -----------------------------------------------------------------------
	// loadSetting — version < 2 (legacy unkeyed torikan)
	// -----------------------------------------------------------------------

	@Test
	void loadSettingLegacyVersionReadsUnkeyedTorikan() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);
		setInt(mode, "version", 1);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("speedmania2.torikan", 9000);

		invokeLoadSetting(mode, prop, "WHATEVER");

		assertEquals(9000, readInt(mode, "torikan"));
	}

	@Test
	void loadSettingLegacyVersionUsesDefaultWhenTorikanAbsent() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);
		setInt(mode, "version", 0);
		CustomProperties prop = new CustomProperties();

		invokeLoadSetting(mode, prop, "ANY");

		assertEquals(10980, readInt(mode, "torikan"), "version 0 default torikan");
	}

	// -----------------------------------------------------------------------
	// saveSetting — version >= 2 (rule-name-keyed torikan)
	// -----------------------------------------------------------------------

	@Test
	void saveSettingVersion2WritesAllFieldsWithRuleNamedTorikan() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);
		setInt(mode, "version", 2);
		setInt(mode, "startlevel", 3);
		setBoolean(mode, "lvstopse", false);
		setBoolean(mode, "showsectiontime", true);
		setBoolean(mode, "big", true);
		setInt(mode, "torikan", 11500);
		setBoolean(mode, "gradedisp", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop, "STANDARD");

		assertEquals(3, prop.getProperty("speedmania2.startlevel", -1));
		assertEquals(false, prop.getProperty("speedmania2.lvstopse", true));
		assertEquals(true, prop.getProperty("speedmania2.showsectiontime", false));
		assertEquals(true, prop.getProperty("speedmania2.big", false));
		assertEquals(11500, prop.getProperty("speedmania2.torikan.STANDARD", -1));
		assertEquals(true, prop.getProperty("speedmania2.gradedisp", false));
	}

	// -----------------------------------------------------------------------
	// saveSetting — version < 2 (legacy unkeyed torikan)
	// -----------------------------------------------------------------------

	@Test
	void saveSettingLegacyVersionWritesUnkeyedTorikan() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);
		setInt(mode, "version", 1);
		setInt(mode, "torikan", 8000);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop, "ANY");

		assertEquals(8000, prop.getProperty("speedmania2.torikan", -1));
	}

	// -----------------------------------------------------------------------
	// Round-trip
	// -----------------------------------------------------------------------

	@Test
	void saveLoadRoundTripPreservesAllSettings() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		freshEngine(mode);
		setInt(mode, "version", 2);
		setInt(mode, "startlevel", 7);
		setBoolean(mode, "lvstopse", true);
		setBoolean(mode, "showsectiontime", true);
		setBoolean(mode, "big", false);
		setInt(mode, "torikan", 9500);
		setBoolean(mode, "gradedisp", false);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop, "RULE");

		SpeedMania2Mode mode2 = new SpeedMania2Mode();
		freshEngine(mode2);
		setInt(mode2, "version", 2);
		invokeLoadSetting(mode2, prop, "RULE");

		assertEquals(7, readInt(mode2, "startlevel"));
		assertEquals(true, readBoolean(mode2, "lvstopse"));
		assertEquals(true, readBoolean(mode2, "showsectiontime"));
		assertEquals(false, readBoolean(mode2, "big"));
		assertEquals(9500, readInt(mode2, "torikan"));
		assertEquals(false, readBoolean(mode2, "gradedisp"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(SpeedMania2Mode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static int readInt(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static boolean readBoolean(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(instance);
	}

	private static void setInt(Object instance, String name, int value) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static void setBoolean(Object instance, String name, boolean value) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(instance, value);
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

	private static void invokeLoadSetting(SpeedMania2Mode mode, CustomProperties prop, String ruleName)
			throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod("loadSetting", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveSetting(SpeedMania2Mode mode, CustomProperties prop, String ruleName)
			throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod("saveSetting", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}
