package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.util.CustomProperties;

/**
 * Pins the settings logic in {@link StateConfigGeneralSDL}:
 * config load/save round-trips, {@code rebuildList()} label generation,
 * {@code onChange()} value cycling, {@code syncRuntimeFullscreen()},
 * and the {@code onPageEvent()} boundary-jump logic.
 *
 * <p>These tests construct the state object but never call
 * {@code render()} or {@code update()} — both require an SDL context.
 */
class StateConfigGeneralSDLLogicTest {

	private StateConfigGeneralSDL state;
	private CustomProperties originalPropConfig;
	private CustomProperties originalPropGlobal;

	/**
	 * The fullscreen-toggle path calls {@code SDL3.INSTANCE}, whose default
	 * backend loads the native SDL3 library. Inject a no-op stub backend so
	 * the test is hermetic (no libSDL3 on CI runners).
	 */
	@BeforeAll
	static void stubSdlBackend() {
		SdlBackend.set(new SdlBackend.Backend() {
			public SDL3 sdl3() { return stub(SDL3.class); }
			public SDL3Image image() { return stub(SDL3Image.class); }
			public SDL3TTF ttf() { return stub(SDL3TTF.class); }
			public SDL3Mixer mixerOrNull() { return null; }
		});
	}

	private static <T> T stub(Class<T> iface) {
		return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
			(proxy, method, args) -> {
				Class<?> rt = method.getReturnType();
				if(rt == byte.class) return (byte) 0;
				if(rt == int.class) return 0;
				if(rt == long.class) return 0L;
				if(rt == float.class) return 0f;
				if(rt == double.class) return 0d;
				if(rt == boolean.class) return false;
				return null;
			}));
	}

	@BeforeEach
	void setUp() {
		originalPropConfig = NullpoMinoSDL.propConfig;
		originalPropGlobal = NullpoMinoSDL.propGlobal;
		NullpoMinoSDL.propConfig = new CustomProperties();
		NullpoMinoSDL.propGlobal = new CustomProperties();
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
		state = new StateConfigGeneralSDL();
	}

	@AfterEach
	void tearDown() {
		NullpoMinoSDL.propConfig = originalPropConfig;
		NullpoMinoSDL.propGlobal = originalPropGlobal;
	}

	/* ---------- Constructor defaults ---------- */

	@Test
	void constructorSetsPageHeightAndMaxCursor() {
		assertEquals(23, state.pageHeight);
		assertEquals(24, state.maxCursor);
		assertEquals(0, state.cursor);
	}

	/* ---------- loadConfig defaults ---------- */

	@Test
	void loadConfigUsesSaneDefaults() {
		// Constructor already called loadConfig with empty props.
		// Verify all the defaults are as expected.
		assertTrue(state.se);
		assertFalse(state.bgm);
		assertTrue(state.showbg);
		assertFalse(state.showfps);
		assertEquals(60, state.maxfps);
		assertTrue(state.showlineeffect);
		assertEquals(0, state.lineeffectspeed);
		assertEquals(1024, state.soundbuffer);
		assertEquals(128, state.fieldbgbright);
		assertEquals(128, state.sevolume);
		assertEquals(128, state.bgmvolume);
		assertFalse(state.enableframestep);
		assertFalse(state.perfectFPSMode);
		assertFalse(state.perfectYield);
		assertFalse(state.outlineghost);
		assertFalse(state.nextshadow);
		assertEquals(0, state.nexttype);
		assertTrue(state.showmeter);
		assertTrue(state.darknextarea);
		assertTrue(state.showfieldbggrid);
		assertFalse(state.showInput);
		assertFalse(state.heavyeffect);
		assertEquals(15, state.soundChannels);
	}

	@Test
	void loadConfigReadsCustomValues() {
		CustomProperties prop = new CustomProperties();
		prop.setProperty("option.fullscreen", "true");
		prop.setProperty("option.se", "false");
		prop.setProperty("option.bgm", "true");
		prop.setProperty("option.showbg", "false");
		prop.setProperty("option.showfps", "false");
		prop.setProperty("option.maxfps", "30");
		prop.setProperty("option.sevolume", "64");
		prop.setProperty("option.bgmvolume", "96");
		prop.setProperty("option.enableframestep", "true");
		prop.setProperty("option.perfectFPSMode", "true");
		prop.setProperty("option.perfectYield", "true");
		prop.setProperty("option.sidenext", "true");
		prop.setProperty("option.bigsidenext", "true");
		prop.setProperty("option.soundChannels", "8");

		state.loadConfig(prop);

		assertTrue(state.fullscreen);
		assertFalse(state.se);
		assertTrue(state.bgm);
		assertFalse(state.showbg);
		assertFalse(state.showfps);
		assertEquals(30, state.maxfps);
		assertEquals(64, state.sevolume);
		assertEquals(96, state.bgmvolume);
		assertTrue(state.enableframestep);
		assertTrue(state.perfectFPSMode);
		assertTrue(state.perfectYield);
		assertEquals(2, state.nexttype); // sidenext=true + bigsidenext=true
		assertEquals(8, state.soundChannels);
	}

	/* ---------- saveConfig round-trip ---------- */

	@Test
	void saveConfigPreservesAllValues() {
		CustomProperties prop = new CustomProperties();

		state.fullscreen = true;
		state.se = false;
		state.bgm = true;
		state.bgmpreload = true;
		state.showbg = false;
		state.showfps = false;
		state.maxfps = 30;
		state.showlineeffect = false;
		state.lineeffectspeed = 5;
		state.soundbuffer = 2048;
		state.heavyeffect = true;
		state.fieldbgbright = 64;
		state.showfieldbggrid = false;
		state.darknextarea = false;
		state.sevolume = 64;
		state.bgmvolume = 32;
		state.soundChannels = 4;
		state.showmeter = false;
		state.nextshadow = true;
		state.outlineghost = true;
		state.perfectFPSMode = true;
		state.perfectYield = true;
		state.showInput = true;
		state.enableframestep = true;
		state.nexttype = 1;

		state.saveConfig(prop);

		assertEquals(true, prop.getProperty("option.fullscreen", false));
		assertEquals(false, prop.getProperty("option.se", true));
		assertEquals(true, prop.getProperty("option.bgm", false));
		assertEquals(true, prop.getProperty("option.bgmpreload", false));
		assertEquals(false, prop.getProperty("option.showbg", true));
		assertEquals(false, prop.getProperty("option.showfps", true));
		assertEquals(30, prop.getProperty("option.maxfps", 60));
		assertEquals(false, prop.getProperty("option.showlineeffect", true));
		assertEquals(5, prop.getProperty("option.lineeffectspeed", 0));
		assertEquals(2048, prop.getProperty("option.soundbuffer", 1024));
		assertEquals(true, prop.getProperty("option.heavyeffect", false));
		assertEquals(64, prop.getProperty("option.fieldbgbright", 128));
		assertEquals(false, prop.getProperty("option.showfieldbggrid", true));
		assertEquals(false, prop.getProperty("option.darknextarea", true));
		assertEquals(64, prop.getProperty("option.sevolume", 128));
		assertEquals(32, prop.getProperty("option.bgmvolume", 128));
		assertEquals(4, prop.getProperty("option.soundChannels", 15));
		assertEquals(false, prop.getProperty("option.showmeter", true));
		assertEquals(true, prop.getProperty("option.nextshadow", false));
		assertEquals(true, prop.getProperty("option.outlineghost", false));
		assertEquals(true, prop.getProperty("option.perfectFPSMode", false));
		assertEquals(true, prop.getProperty("option.perfectYield", false));
		assertEquals(true, prop.getProperty("option.showInput", false));
		assertEquals(true, prop.getProperty("option.enableframestep", false));
		assertEquals(true, prop.getProperty("option.sidenext", false));
		assertEquals(false, prop.getProperty("option.bigsidenext", false));
	}

	/* ---------- rebuildList ---------- */

	@Test
	void rebuildListGeneratesExpectedLabels() {
		state.se = true;
		state.bgm = false;
		state.showbg = true;
		state.showfps = true;
		state.maxfps = 60;
		state.sevolume = 128;
		state.bgmvolume = 128;
		state.nexttype = 0;
		state.fieldbgbright = 128;
		state.lineeffectspeed = 0;
		state.soundbuffer = 1024;
		state.soundChannels = 15;

		state.rebuildList();

		assertEquals(25, state.list.length);
		assertEquals("SE:c", state.list[0]);
		assertEquals("BGM:e", state.list[1]);
		assertEquals("MAX FPS:60", state.list[19]);
		assertEquals("SOUND BUFFER SIZE:1024", state.list[23]);
		assertEquals("MAX SOUND CHANNELS:15", state.list[24]);
	}

	@Test
	void rebuildListShowsNextTypeOptions() {
		state.nexttype = 0;
		state.rebuildList();
		assertEquals("NEXT DISPLAY TYPE:TOP", state.list[12]);

		state.nexttype = 1;
		state.rebuildList();
		assertEquals("NEXT DISPLAY TYPE:SIDE(SMALL)", state.list[12]);

		state.nexttype = 2;
		state.rebuildList();
		assertEquals("NEXT DISPLAY TYPE:SIDE(BIG)", state.list[12]);
	}

	/* ---------- syncRuntimeFullscreen ---------- */

	@Test
	void syncRuntimeFullscreenForcesValueFromStaticField() {
		state.fullscreen = false;
		NullpoMinoSDL.fullscreen = true;

		state.syncRuntimeFullscreen(true);

		assertTrue(state.fullscreen);
	}

	@Test
	void syncRuntimeFullscreenDoesNothingWhenSynced() {
		state.fullscreen = true;
		NullpoMinoSDL.fullscreen = true;
		state.lastRuntimeFullscreen = true;

		state.syncRuntimeFullscreen(false);

		assertTrue(state.fullscreen);
	}

	@Test
	void syncRuntimeFullscreenResyncsWhenRuntimeChanges() {
		state.fullscreen = false;
		state.lastRuntimeFullscreen = false;
		NullpoMinoSDL.fullscreen = true;

		state.syncRuntimeFullscreen(false);

		assertTrue(state.fullscreen);
		assertTrue(state.lastRuntimeFullscreen);
	}

	/* ---------- onChange value cycling ---------- */

	@Test
	void onChangeCursor0TogglesSe() {
		state.cursor = 0;
		state.se = true;
		state.onChange(1);
		assertFalse(state.se);
		state.onChange(1);
		assertTrue(state.se);
	}

	@Test
	void onChangeCursor1TogglesBgm() {
		state.cursor = 1;
		state.bgm = false;
		state.onChange(1);
		assertTrue(state.bgm);
	}

	@Test
	void onChangeCursor3CyclesSeVolume() {
		state.cursor = 3;
		state.sevolume = 64;
		state.onChange(1);
		assertEquals(65, state.sevolume);
		state.onChange(-1);
		assertEquals(64, state.sevolume);
	}

	@Test
	void onChangeCursor3SeVolumeWraps() {
		state.cursor = 3;
		state.sevolume = 128;
		state.onChange(1);
		assertEquals(0, state.sevolume);

		state.sevolume = 0;
		state.onChange(-1);
		assertEquals(128, state.sevolume);
	}

	@Test
	void onChangeCursor4CyclesBgmVolume() {
		state.cursor = 4;
		state.bgmvolume = 128;
		state.onChange(1);
		assertEquals(0, state.bgmvolume);
	}

	@Test
	void onChangeCursor12CyclesNextType() {
		state.cursor = 12;
		state.nexttype = 0;
		state.onChange(1);
		assertEquals(1, state.nexttype);
		state.onChange(1);
		assertEquals(2, state.nexttype);
		state.onChange(1);
		assertEquals(0, state.nexttype);
	}

	@Test
	void onChangeCursor14CyclesFieldBgbright() {
		state.cursor = 14;
		state.fieldbgbright = 128;
		state.onChange(1);
		assertEquals(129, state.fieldbgbright);
		state.fieldbgbright = 255;
		state.onChange(1);
		assertEquals(0, state.fieldbgbright);
	}

	@Test
	void onChangeCursor17TogglesFullscreen() {
		state.cursor = 17;
		state.fullscreen = false;
		state.onChange(1);
		assertTrue(state.fullscreen);
	}

	@Test
	void onChangeCursor19MaxFpsCycles() {
		state.cursor = 19;
		state.maxfps = 60;
		state.onChange(1);
		assertEquals(61, state.maxfps);
		state.maxfps = 99;
		state.onChange(1);
		assertEquals(0, state.maxfps);
	}

	@Test
	void onChangeCursor21TogglesPerfectFpsMode() {
		state.cursor = 21;
		state.perfectFPSMode = false;
		state.onChange(1);
		assertTrue(state.perfectFPSMode);
	}

	@Test
	void onChangeCursor22TogglesPerfectYield() {
		state.cursor = 22;
		state.perfectYield = false;
		state.onChange(1);
		assertTrue(state.perfectYield);
	}

	@Test
	void onChangeCursor23SoundBufferStepsBy256() {
		state.cursor = 23;
		state.soundbuffer = 1024;
		state.onChange(1);
		assertEquals(1280, state.soundbuffer);
		state.onChange(-1);
		assertEquals(1024, state.soundbuffer);
	}

	@Test
	void onChangeCursor23SoundBufferWraps() {
		state.cursor = 23;
		state.soundbuffer = 65535;
		state.onChange(1);
		assertEquals(0, state.soundbuffer);
	}

	@Test
	void onChangeCursor24SoundChannelsCycles() {
		state.cursor = 24;
		state.soundChannels = 15;
		state.onChange(1);
		assertEquals(16, state.soundChannels);
		state.soundChannels = 50;
		state.onChange(1);
		assertEquals(0, state.soundChannels);
	}

	/* ---------- onPageEvent ---------- */

	@Test
	void onPageEventPositiveJumpsToNextBoundary() {
		state.cursor = 5;
		state.onPageEvent(1);
		assertEquals(17, state.cursor);
	}

	@Test
	void onPageEventPositiveFromAboveLastBoundaryStays() {
		state.cursor = 23;
		state.onPageEvent(1);
		assertEquals(23, state.cursor);
	}

	@Test
	void onPageEventNegativeJumpsToPreviousBoundary() {
		state.cursor = 20;
		state.onPageEvent(-1);
		assertEquals(17, state.cursor);
	}

	@Test
	void onPageEventNegativeFromBeforeFirstBoundaryStays() {
		state.cursor = 0;
		state.onPageEvent(-1);
		assertEquals(0, state.cursor);
	}

	@Test
	void onPageEventNegativeFromMiddleJumpsDown() {
		state.cursor = 18;
		state.onPageEvent(-1);
		assertEquals(17, state.cursor);
	}

	/* ---------- onDecide / onCancel ---------- */

	@Test
	void onDecideReturnsTrue() {
		assertTrue(state.onDecide());
	}

	@Test
	void onCancelReturnsTrue() {
		assertTrue(state.onCancel());
	}
}
