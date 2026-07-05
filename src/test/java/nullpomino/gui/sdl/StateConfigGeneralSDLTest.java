package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link StateConfigGeneralSDL}: the
 * cursor / page bounds set by the constructor, the documented defaults
 * applied by loadConfig, the saveConfig key shape, the
 * sidenext / bigsidenext byte encoding for nexttype, and the
 * applyAndSave snapshot it writes.
 *
 * <p>The constructor reaches for {@link NullpoMinoSDL#propConfig}, so
 * tests snapshot and restore the static.
 */
class StateConfigGeneralSDLTest {

	private CustomProperties originalPropConfig;

	@BeforeEach
	void setUp() {
		originalPropConfig = NullpoMinoSDL.propConfig;
		NullpoMinoSDL.propConfig = new CustomProperties();
	}

	@AfterEach
	void tearDown() {
		NullpoMinoSDL.propConfig = originalPropConfig;
	}

	@Test
	void constructorPinsPageHeightMaxCursorAndCursorZero() throws Exception {
		StateConfigGeneralSDL state = new StateConfigGeneralSDL();

		assertEquals(23, readInt(state, DummyMenuScrollStateSDL.class, "pageHeight"));
		assertEquals(24, readInt(state, DummyMenuChooseStateSDL.class, "maxCursor"),
				"twenty-five entries -> maxCursor pinned to 24");
		assertEquals(0, readInt(state, DummyMenuChooseStateSDL.class, "cursor"));
	}

	@Test
	void pageBoundariesLandOnTheThreeDocumentedJumpAnchors() throws Exception {
		// PAGE_BOUNDARIES drives Page Up / Page Down; anchors are
		// 0 (top), 17 (display group), 23 (sound buffers).
		Field f = StateConfigGeneralSDL.class.getDeclaredField("PAGE_BOUNDARIES");
		f.setAccessible(true);
		assertArrayEquals(new int[] {0, 17, 23}, (int[]) f.get(null));
	}

	@Test
	void nextTypeOptionsListsAllThreePreviewLayouts() throws Exception {
		Field f = StateConfigGeneralSDL.class.getDeclaredField("NEXTTYPE_OPTIONS");
		f.setAccessible(true);
		assertArrayEquals(
				new String[] {"TOP", "SIDE(SMALL)", "SIDE(BIG)"},
				(String[]) f.get(null));
	}

	@Test
	void loadConfigAppliesDocumentedDefaultsForMissingKeys() throws Exception {
		StateConfigGeneralSDL state = new StateConfigGeneralSDL();

		invokeLoadConfig(state, new CustomProperties());

		assertFalse(readBoolean(state, "fullscreen"));
		assertTrue(readBoolean(state, "se"), "SE defaults to ON");
		assertFalse(readBoolean(state, "bgm"), "BGM defaults to OFF");
		assertFalse(readBoolean(state, "bgmpreload"));
		assertTrue(readBoolean(state, "showbg"));
		assertFalse(readBoolean(state, "showfps"));
		assertFalse(readBoolean(state, "enableframestep"));
		assertEquals(60, readInt(state, "maxfps"));
		assertTrue(readBoolean(state, "showlineeffect"));
		assertEquals(0, readInt(state, "lineeffectspeed"));
		assertEquals(1024, readInt(state, "soundbuffer"));
		assertFalse(readBoolean(state, "heavyeffect"));
		assertEquals(128, readInt(state, "fieldbgbright"));
		assertTrue(readBoolean(state, "showfieldbggrid"));
		assertTrue(readBoolean(state, "darknextarea"));
		assertEquals(128, readInt(state, "sevolume"));
		assertEquals(128, readInt(state, "bgmvolume"));
		assertEquals(15, readInt(state, "soundChannels"));
		assertTrue(readBoolean(state, "showmeter"));
		assertFalse(readBoolean(state, "nextshadow"));
		assertFalse(readBoolean(state, "outlineghost"));
		assertFalse(readBoolean(state, "perfectFPSMode"));
		assertFalse(readBoolean(state, "perfectYield"));
		assertFalse(readBoolean(state, "showInput"));
		assertEquals(0, readInt(state, "nexttype"),
				"defaults: sidenext=false -> nexttype=0");
	}

	@Test
	void loadConfigDecodesNexttypeAcrossSidenextAndBigsidenextCombinations() throws Exception {
		StateConfigGeneralSDL state = new StateConfigGeneralSDL();
		CustomProperties prop = new CustomProperties();

		// sidenext=false, bigsidenext=anything -> 0 (TOP)
		prop.setProperty("option.sidenext", false);
		prop.setProperty("option.bigsidenext", true);
		invokeLoadConfig(state, prop);
		assertEquals(0, readInt(state, "nexttype"));

		// sidenext=true, bigsidenext=false -> 1 (SIDE small)
		prop.setProperty("option.sidenext", true);
		prop.setProperty("option.bigsidenext", false);
		invokeLoadConfig(state, prop);
		assertEquals(1, readInt(state, "nexttype"));

		// sidenext=true, bigsidenext=true -> 2 (SIDE big)
		prop.setProperty("option.sidenext", true);
		prop.setProperty("option.bigsidenext", true);
		invokeLoadConfig(state, prop);
		assertEquals(2, readInt(state, "nexttype"));
	}

	@Test
	void saveConfigEncodesNexttypeAsTwoBooleansSidenextAndBigsidenext() throws Exception {
		StateConfigGeneralSDL state = new StateConfigGeneralSDL();
		CustomProperties prop = new CustomProperties();

		// nexttype=0 -> sidenext=false, bigsidenext=false
		setInt(state, "nexttype", 0);
		invokeSaveConfig(state, prop);
		assertFalse(prop.getProperty("option.sidenext", true));
		assertFalse(prop.getProperty("option.bigsidenext", true));

		// nexttype=1 -> sidenext=true, bigsidenext=false
		setInt(state, "nexttype", 1);
		invokeSaveConfig(state, prop);
		assertTrue(prop.getProperty("option.sidenext", false));
		assertFalse(prop.getProperty("option.bigsidenext", true));

		// nexttype=2 -> sidenext=true, bigsidenext=true
		setInt(state, "nexttype", 2);
		invokeSaveConfig(state, prop);
		assertTrue(prop.getProperty("option.sidenext", false));
		assertTrue(prop.getProperty("option.bigsidenext", false));
	}

	@Test
	void saveConfigAndLoadConfigRoundTripUnderOptionPrefix() throws Exception {
		StateConfigGeneralSDL source = new StateConfigGeneralSDL();
		setBoolean(source, "fullscreen", true);
		setBoolean(source, "se", false);
		setBoolean(source, "bgm", true);
		setBoolean(source, "bgmpreload", true);
		setInt(source, "maxfps", 120);
		setBoolean(source, "showlineeffect", false);
		setInt(source, "lineeffectspeed", 5);
		setInt(source, "soundbuffer", 4096);
		setBoolean(source, "heavyeffect", true);
		setInt(source, "fieldbgbright", 64);
		setBoolean(source, "showfieldbggrid", false);
		setBoolean(source, "darknextarea", false);
		setInt(source, "sevolume", 96);
		setInt(source, "bgmvolume", 32);
		setInt(source, "soundChannels", 30);
		setBoolean(source, "showmeter", false);
		setBoolean(source, "nextshadow", true);
		setBoolean(source, "outlineghost", true);
		setBoolean(source, "perfectFPSMode", true);
		setBoolean(source, "perfectYield", true);
		setBoolean(source, "showInput", true);
		setInt(source, "nexttype", 2);

		CustomProperties prop = new CustomProperties();
		invokeSaveConfig(source, prop);

		// Spot check a few keys land under the documented option.* prefix.
		assertTrue(prop.getProperty("option.fullscreen", false));
		assertEquals(120, prop.getProperty("option.maxfps", -1));
		assertEquals(96, prop.getProperty("option.sevolume", -1));
		assertEquals(4096, prop.getProperty("option.soundbuffer", -1));
		assertEquals(30, prop.getProperty("option.soundChannels", -1));

		// Round-trip back to a fresh state.
		StateConfigGeneralSDL dest = new StateConfigGeneralSDL();
		invokeLoadConfig(dest, prop);

		assertTrue(readBoolean(dest, "fullscreen"));
		assertFalse(readBoolean(dest, "se"));
		assertTrue(readBoolean(dest, "bgm"));
		assertTrue(readBoolean(dest, "bgmpreload"));
		assertEquals(120, readInt(dest, "maxfps"));
		assertFalse(readBoolean(dest, "showlineeffect"));
		assertEquals(5, readInt(dest, "lineeffectspeed"));
		assertEquals(4096, readInt(dest, "soundbuffer"));
		assertTrue(readBoolean(dest, "heavyeffect"));
		assertEquals(64, readInt(dest, "fieldbgbright"));
		assertFalse(readBoolean(dest, "showfieldbggrid"));
		assertFalse(readBoolean(dest, "darknextarea"));
		assertEquals(96, readInt(dest, "sevolume"));
		assertEquals(32, readInt(dest, "bgmvolume"));
		assertEquals(30, readInt(dest, "soundChannels"));
		assertFalse(readBoolean(dest, "showmeter"));
		assertTrue(readBoolean(dest, "nextshadow"));
		assertTrue(readBoolean(dest, "outlineghost"));
		assertTrue(readBoolean(dest, "perfectFPSMode"));
		assertTrue(readBoolean(dest, "perfectYield"));
		assertTrue(readBoolean(dest, "showInput"));
		assertEquals(2, readInt(dest, "nexttype"));
	}

	@Test
	void syncRuntimeFullscreenForcesResyncFromRuntimeStateWhenAsked() throws Exception {
		StateConfigGeneralSDL state = new StateConfigGeneralSDL();

		// Spoil pending menu edit, then force-resync should pull the
		// runtime value back in.
		setBoolean(state, "fullscreen", true);
		boolean savedRuntime = NullpoMinoSDL.fullscreen;
		try {
			NullpoMinoSDL.fullscreen = false;
			invokeSyncRuntimeFullscreen(state, true);

			assertFalse(readBoolean(state, "fullscreen"),
					"force=true must sync regardless of last-runtime cache");
			assertFalse(readBoolean(state, "lastRuntimeFullscreen"));
		} finally {
			NullpoMinoSDL.fullscreen = savedRuntime;
		}
	}

	@Test
	void syncRuntimeFullscreenSkipsResyncWhenRuntimeMatchesLastObserved() throws Exception {
		StateConfigGeneralSDL state = new StateConfigGeneralSDL();
		boolean savedRuntime = NullpoMinoSDL.fullscreen;
		try {
			// Observation: runtime == last-runtime, so a non-forced sync
			// must leave a pending menu edit alone.
			NullpoMinoSDL.fullscreen = false;
			setBoolean(state, "lastRuntimeFullscreen", false);
			setBoolean(state, "fullscreen", true);

			invokeSyncRuntimeFullscreen(state, false);

			assertTrue(readBoolean(state, "fullscreen"),
					"non-forced sync must not overwrite a pending menu edit "
							+ "when runtime hasn't changed");
		} finally {
			NullpoMinoSDL.fullscreen = savedRuntime;
		}
	}

	@Test
	void syncRuntimeFullscreenAdoptsRuntimeStateWhenItDivergesFromLastObserved() throws Exception {
		StateConfigGeneralSDL state = new StateConfigGeneralSDL();
		boolean savedRuntime = NullpoMinoSDL.fullscreen;
		try {
			// Simulates an F11 toggle outside the menu: runtime fullscreen
			// flipped while we were on the screen, so the next sync
			// adopts the new state and discards any pending menu edit.
			NullpoMinoSDL.fullscreen = true;
			setBoolean(state, "lastRuntimeFullscreen", false);
			setBoolean(state, "fullscreen", false);

			invokeSyncRuntimeFullscreen(state, false);

			assertTrue(readBoolean(state, "fullscreen"));
			assertTrue(readBoolean(state, "lastRuntimeFullscreen"));
		} finally {
			NullpoMinoSDL.fullscreen = savedRuntime;
		}
	}

	private static int readInt(Object instance, String name) throws Exception {
		return readInt(instance, instance.getClass(), name);
	}

	private static int readInt(Object instance, Class<?> declaringClass, String name) throws Exception {
		Field f = declaringClass.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static boolean readBoolean(Object instance, String name) throws Exception {
		Field f = instance.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.getBoolean(instance);
	}

	private static void setInt(Object instance, String name, int value) throws Exception {
		Field f = instance.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static void setBoolean(Object instance, String name, boolean value) throws Exception {
		Field f = instance.getClass().getDeclaredField(name);
		f.setAccessible(true);
		f.setBoolean(instance, value);
	}

	private static void invokeLoadConfig(StateConfigGeneralSDL state, CustomProperties prop) throws Exception {
		Method m = StateConfigGeneralSDL.class.getDeclaredMethod("loadConfig", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(state, prop);
	}

	private static void invokeSaveConfig(StateConfigGeneralSDL state, CustomProperties prop) throws Exception {
		Method m = StateConfigGeneralSDL.class.getDeclaredMethod("saveConfig", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(state, prop);
	}

	private static void invokeSyncRuntimeFullscreen(StateConfigGeneralSDL state, boolean force) throws Exception {
		Method m = StateConfigGeneralSDL.class.getDeclaredMethod("syncRuntimeFullscreen", boolean.class);
		m.setAccessible(true);
		m.invoke(state, force);
	}
}
