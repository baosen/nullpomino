package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateConfigJoystickMainSDLTest {

	private CustomProperties originalPropConfig;

	@BeforeEach
	void snapshotPropConfig() {
		originalPropConfig = NullpoMinoSDL.propConfig;
		NullpoMinoSDL.propConfig = new CustomProperties();
	}

	@AfterEach
	void restorePropConfig() {
		NullpoMinoSDL.propConfig = originalPropConfig;
	}

	@Test
	void constructorPinsPlayerAndCursorToZero() {
		StateConfigJoystickMainSDL state = new StateConfigJoystickMainSDL();

		assertEquals(0, state.player);
		assertEquals(0, readInt(state, "cursor"));
	}

	@Test
	void loadConfigUsesPlayerSuffixedKeysAndAppliesDocumentedDefaultsWhenAbsent() throws Exception {
		StateConfigJoystickMainSDL state = new StateConfigJoystickMainSDL();
		state.player = 1;

		// No keys set → defaults: joyUseNumber=-1, joyBorder=0, axis/POV=false.
		invokeLoadConfig(state, NullpoMinoSDL.propConfig);

		assertEquals(-1, readInt(state, "joyUseNumber"));
		assertEquals(0, readInt(state, "joyBorder"));
		assertFalse(readBoolean(state, "joyIgnoreAxis"));
		assertFalse(readBoolean(state, "joyIgnorePOV"));
	}

	@Test
	void loadConfigReadsPlayerSuffixedKeysFromTheProperties() throws Exception {
		StateConfigJoystickMainSDL state = new StateConfigJoystickMainSDL();
		state.player = 0;
		NullpoMinoSDL.propConfig.setProperty("joyUseNumber.p0", 2);
		NullpoMinoSDL.propConfig.setProperty("joyBorder.p0", 16384);
		NullpoMinoSDL.propConfig.setProperty("joyIgnoreAxis.p0", true);
		NullpoMinoSDL.propConfig.setProperty("joyIgnorePOV.p0", true);

		invokeLoadConfig(state, NullpoMinoSDL.propConfig);

		assertEquals(2, readInt(state, "joyUseNumber"));
		assertEquals(16384, readInt(state, "joyBorder"));
		assertTrue(readBoolean(state, "joyIgnoreAxis"));
		assertTrue(readBoolean(state, "joyIgnorePOV"));
	}

	@Test
	void saveConfigWritesPlayerSuffixedKeysAndIsRoundTrippable() throws Exception {
		StateConfigJoystickMainSDL state = new StateConfigJoystickMainSDL();
		state.player = 1;
		setInt(state, "joyUseNumber", 3);
		setInt(state, "joyBorder", 8192);
		setBoolean(state, "joyIgnoreAxis", true);
		setBoolean(state, "joyIgnorePOV", false);

		CustomProperties prop = new CustomProperties();
		invokeSaveConfig(state, prop);

		assertEquals(3, prop.getProperty("joyUseNumber.p1", -999));
		assertEquals(8192, prop.getProperty("joyBorder.p1", -999));
		assertTrue(prop.getProperty("joyIgnoreAxis.p1", false));
		assertFalse(prop.getProperty("joyIgnorePOV.p1", true));

		// Round-trip: load into a fresh state instance and confirm the
		// fields are observed back identically.
		StateConfigJoystickMainSDL loaded = new StateConfigJoystickMainSDL();
		loaded.player = 1;
		invokeLoadConfig(loaded, prop);

		assertEquals(3, readInt(loaded, "joyUseNumber"));
		assertEquals(8192, readInt(loaded, "joyBorder"));
		assertTrue(readBoolean(loaded, "joyIgnoreAxis"));
		assertFalse(readBoolean(loaded, "joyIgnorePOV"));
	}

	@Test
	void loadAndSaveConfigKeepDifferentPlayersUnderDifferentKeyNamespaces() throws Exception {
		// Player 0 and player 1 must not collide on the prop file.
		CustomProperties prop = new CustomProperties();

		StateConfigJoystickMainSDL p0 = new StateConfigJoystickMainSDL();
		p0.player = 0;
		setInt(p0, "joyUseNumber", 5);
		invokeSaveConfig(p0, prop);

		StateConfigJoystickMainSDL p1 = new StateConfigJoystickMainSDL();
		p1.player = 1;
		setInt(p1, "joyUseNumber", 9);
		invokeSaveConfig(p1, prop);

		assertEquals(5, prop.getProperty("joyUseNumber.p0", -1));
		assertEquals(9, prop.getProperty("joyUseNumber.p1", -1));
	}

	@Test
	void enterPullsConfigFromTheGlobalNullpoMinoSDLPropConfig() throws Exception {
		NullpoMinoSDL.propConfig.setProperty("joyUseNumber.p0", 4);
		NullpoMinoSDL.propConfig.setProperty("joyBorder.p0", 1024);
		NullpoMinoSDL.propConfig.setProperty("joyIgnoreAxis.p0", true);

		StateConfigJoystickMainSDL state = new StateConfigJoystickMainSDL();
		state.player = 0;
		state.enter();

		assertEquals(4, readInt(state, "joyUseNumber"));
		assertEquals(1024, readInt(state, "joyBorder"));
		assertTrue(readBoolean(state, "joyIgnoreAxis"));
	}

	private static void invokeLoadConfig(StateConfigJoystickMainSDL state, CustomProperties prop) throws Exception {
		Method m = StateConfigJoystickMainSDL.class.getDeclaredMethod("loadConfig", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(state, prop);
	}

	private static void invokeSaveConfig(StateConfigJoystickMainSDL state, CustomProperties prop) throws Exception {
		Method m = StateConfigJoystickMainSDL.class.getDeclaredMethod("saveConfig", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(state, prop);
	}

	private static int readInt(StateConfigJoystickMainSDL state, String name) {
		try {
			Field f = StateConfigJoystickMainSDL.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.getInt(state);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static boolean readBoolean(StateConfigJoystickMainSDL state, String name) {
		try {
			Field f = StateConfigJoystickMainSDL.class.getDeclaredField(name);
			f.setAccessible(true);
			return f.getBoolean(state);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void setInt(StateConfigJoystickMainSDL state, String name, int value) throws Exception {
		Field f = StateConfigJoystickMainSDL.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(state, value);
	}

	private static void setBoolean(StateConfigJoystickMainSDL state, String name, boolean value) throws Exception {
		Field f = StateConfigJoystickMainSDL.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(state, value);
	}
}
