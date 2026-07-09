package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

		// No keys set → player 1 defaults to the first gamepad, player 2 to none
		// (must match NullpoMinoSDL.initJoysticks or OK would disable the pad).
		state.player = 0;
		invokeLoadConfig(state, NullpoMinoSDL.propConfig);
		assertEquals(0, readInt(state, "joyUseNumber"));

		state.player = 1;
		invokeLoadConfig(state, NullpoMinoSDL.propConfig);
		assertEquals(-1, readInt(state, "joyUseNumber"));
	}

	@Test
	void loadConfigReadsPlayerSuffixedKeysFromTheProperties() throws Exception {
		StateConfigJoystickMainSDL state = new StateConfigJoystickMainSDL();
		state.player = 0;
		NullpoMinoSDL.propConfig.setProperty("joyUseNumber.p0", 2);

		invokeLoadConfig(state, NullpoMinoSDL.propConfig);

		assertEquals(2, readInt(state, "joyUseNumber"));
	}

	@Test
	void saveConfigWritesPlayerSuffixedKeysAndIsRoundTrippable() throws Exception {
		StateConfigJoystickMainSDL state = new StateConfigJoystickMainSDL();
		state.player = 1;
		setInt(state, "joyUseNumber", 3);

		CustomProperties prop = new CustomProperties();
		invokeSaveConfig(state, prop);

		assertEquals(3, prop.getProperty("joyUseNumber.p1", -999));

		// Round-trip: load into a fresh state instance and confirm the
		// field is observed back identically.
		StateConfigJoystickMainSDL loaded = new StateConfigJoystickMainSDL();
		loaded.player = 1;
		invokeLoadConfig(loaded, prop);

		assertEquals(3, readInt(loaded, "joyUseNumber"));
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

		StateConfigJoystickMainSDL state = new StateConfigJoystickMainSDL();
		state.player = 0;
		state.enter();

		assertEquals(4, readInt(state, "joyUseNumber"));
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

	private static void setInt(StateConfigJoystickMainSDL state, String name, int value) throws Exception {
		Field f = StateConfigJoystickMainSDL.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(state, value);
	}
}
