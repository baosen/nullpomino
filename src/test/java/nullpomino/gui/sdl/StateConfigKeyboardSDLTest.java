package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.gui.sdl.binding.SDLConstants;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the headless slice of {@link StateConfigKeyboardSDL}: the
 * KEYACCEPTFRAME / NUM_KEYS constants, reset() seeding from the right
 * keymap (gamekey[player].keymap vs keymapNav based on isNavSetting),
 * getPressedKeyNumber's first-difference scan, getKeyName lookup with
 * the "(N)" fallback for out-of-range codes, and isMenuRepeatKey's
 * one-frame-then-every-third-after-25 cadence.
 */
class StateConfigKeyboardSDLTest {

	private GameKeySDL[] originalGameKey;

	@BeforeEach
	void setUp() {
		originalGameKey = GameKeySDL.gamekey;
		GameKeySDL.initGlobalGameKeySDL();
	}

	@AfterEach
	void tearDown() {
		GameKeySDL.gamekey = originalGameKey;
	}

	@Test
	void constantsPinKeyacceptframeFifteenAndNumKeysSixteen() {
		assertEquals(15, StateConfigKeyboardSDL.KEYACCEPTFRAME);
		assertEquals(16, StateConfigKeyboardSDL.NUM_KEYS);
	}

	@Test
	void resetCopiesGameKeymapWhenIsNavSettingIsFalse() throws Exception {
		StateConfigKeyboardSDL state = new StateConfigKeyboardSDL();
		state.player = 0;
		state.isNavSetting = false;

		// Seed gamekey[0].keymap with a recognisable pattern.
		for(int i = 0; i < StateConfigKeyboardSDL.NUM_KEYS; i++) {
			GameKeySDL.gamekey[0].keymap[i] = 100 + i;
			GameKeySDL.gamekey[0].keymapNav[i] = 200 + i;
		}

		invokeReset(state);

		int[] keymap = (int[]) readField(state, "keymap");
		assertEquals(StateConfigKeyboardSDL.NUM_KEYS, keymap.length);
		for(int i = 0; i < keymap.length; i++) {
			assertEquals(100 + i, keymap[i],
					"isNavSetting=false must read from gamekey.keymap (in-game)");
		}
	}

	@Test
	void resetCopiesGameKeymapNavWhenIsNavSettingIsTrue() throws Exception {
		StateConfigKeyboardSDL state = new StateConfigKeyboardSDL();
		state.player = 1;
		state.isNavSetting = true;

		for(int i = 0; i < StateConfigKeyboardSDL.NUM_KEYS; i++) {
			GameKeySDL.gamekey[1].keymap[i] = 100 + i;
			GameKeySDL.gamekey[1].keymapNav[i] = 200 + i;
		}

		invokeReset(state);

		int[] keymap = (int[]) readField(state, "keymap");
		for(int i = 0; i < keymap.length; i++) {
			assertEquals(200 + i, keymap[i],
					"isNavSetting=true must read from gamekey.keymapNav (menu)");
		}
	}

	@Test
	void resetClearsKeynumFrameAndPreviousKeyState() throws Exception {
		StateConfigKeyboardSDL state = new StateConfigKeyboardSDL();
		setInt(state, "keynum", 5);
		setInt(state, "frame", 99);
		setInt(state, "upInputState", 7);
		setInt(state, "downInputState", 11);
		setInt(state, "keyConfigRestFrame", 13);

		invokeReset(state);

		assertEquals(0, readInt(state, "keynum"));
		assertEquals(0, readInt(state, "frame"));
		assertEquals(0, readInt(state, "upInputState"));
		assertEquals(0, readInt(state, "downInputState"));
		assertEquals(0, readInt(state, "keyConfigRestFrame"));

		boolean[] previous = (boolean[]) readField(state, "previousKeyPressedState");
		assertEquals(SDLConstants.SDL_SCANCODE_COUNT, previous.length);
	}

	@Test
	void getPressedKeyNumberReturnsMinusOneWhenStatesAgree() throws Exception {
		StateConfigKeyboardSDL state = new StateConfigKeyboardSDL();
		boolean[] prev = new boolean[8];
		boolean[] now = new boolean[8];
		// All identical -> -1.

		assertEquals(-1, invokeGetPressedKeyNumber(state, prev, now));
	}

	@Test
	void getPressedKeyNumberReturnsFirstDifferentIndex() throws Exception {
		StateConfigKeyboardSDL state = new StateConfigKeyboardSDL();
		boolean[] prev = new boolean[8];
		boolean[] now = new boolean[8];
		now[3] = true;

		assertEquals(3, invokeGetPressedKeyNumber(state, prev, now));

		// Even a release counts as a difference (first index that flipped).
		boolean[] prev2 = new boolean[] {false, true, true, false};
		boolean[] now2  = new boolean[] {false, true, false, false};
		assertEquals(2, invokeGetPressedKeyNumber(state, prev2, now2));
	}

	@Test
	void getPressedKeyNumberReportsTheLowestIndexWhenManyDiffer() throws Exception {
		StateConfigKeyboardSDL state = new StateConfigKeyboardSDL();
		boolean[] prev = new boolean[5];
		boolean[] now = new boolean[5];
		now[1] = true;
		now[3] = true;

		assertEquals(1, invokeGetPressedKeyNumber(state, prev, now),
				"first difference -> caller picks the earliest scancode");
	}

	@Test
	void getKeyNameReturnsScancodeTableEntryForKnownScancodes() throws Exception {
		StateConfigKeyboardSDL state = new StateConfigKeyboardSDL();
		// Pick a few scancodes that have stable names.
		assertEquals(SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_A],
				invokeGetKeyName(state, SDLConstants.SDL_SCANCODE_A));
		assertEquals(SDLConstants.SCANCODE_NAMES[SDLConstants.SDL_SCANCODE_RETURN],
				invokeGetKeyName(state, SDLConstants.SDL_SCANCODE_RETURN));
	}

	@Test
	void getKeyNameSurroundsCodeWithParensWhenOutOfRange() throws Exception {
		StateConfigKeyboardSDL state = new StateConfigKeyboardSDL();

		assertEquals("(-1)", invokeGetKeyName(state, -1),
				"-1 == 'unset' must render as (-1) so the user sees the raw code");
		assertEquals("(99999)", invokeGetKeyName(state, 99999));
	}

	@Test
	void isMenuRepeatKeyFiresOnFirstFrameAndEveryThirdFrameAfterTwentyFive() throws Exception {
		// Frame 1 = initial press; frames 25, 28, 31, ... = auto-repeat.
		assertTrue(invokeIsMenuRepeatKey(1));
		assertFalse(invokeIsMenuRepeatKey(2));
		assertFalse(invokeIsMenuRepeatKey(24));
		// 25 % 3 == 1 -> not a multiple of 3.
		assertFalse(invokeIsMenuRepeatKey(25));
		assertFalse(invokeIsMenuRepeatKey(26));
		assertTrue(invokeIsMenuRepeatKey(27),
				"first auto-repeat fires at the first frame >= 25 divisible by 3");
		assertFalse(invokeIsMenuRepeatKey(28));
		assertTrue(invokeIsMenuRepeatKey(30));
		assertFalse(invokeIsMenuRepeatKey(0),
				"frame 0 (no press at all) must not register");
	}

	@Test
	void resetSizesPreviousKeyStateToScancodeCount() throws Exception {
		StateConfigKeyboardSDL state = new StateConfigKeyboardSDL();
		invokeReset(state);

		boolean[] prev = (boolean[]) readField(state, "previousKeyPressedState");
		// All entries default to false on a fresh allocation — pin that the
		// initial diff against an all-false NullpoMinoSDL.keyPressedState
		// returns -1 (no key pressed yet).
		boolean[] zeros = new boolean[prev.length];
		assertArrayEquals(zeros, prev);
		assertEquals(-1, invokeGetPressedKeyNumber(state, prev, zeros));
	}

	private static int readInt(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static Object readField(Object instance, String name) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
	}

	private static void setInt(Object instance, String name, int value) throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
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

	private static void invokeReset(StateConfigKeyboardSDL state) throws Exception {
		Method m = StateConfigKeyboardSDL.class.getDeclaredMethod("reset");
		m.setAccessible(true);
		m.invoke(state);
	}

	private static int invokeGetPressedKeyNumber(StateConfigKeyboardSDL state,
			boolean[] prev, boolean[] now) throws Exception {
		Method m = StateConfigKeyboardSDL.class.getDeclaredMethod(
				"getPressedKeyNumber", boolean[].class, boolean[].class);
		m.setAccessible(true);
		return (int) m.invoke(state, prev, now);
	}

	private static String invokeGetKeyName(StateConfigKeyboardSDL state, int key) throws Exception {
		Method m = StateConfigKeyboardSDL.class.getDeclaredMethod("getKeyName", int.class);
		m.setAccessible(true);
		return (String) m.invoke(state, key);
	}

	private static boolean invokeIsMenuRepeatKey(int holdFrames) throws Exception {
		Method m = StateConfigKeyboardSDL.class.getDeclaredMethod("isMenuRepeatKey", int.class);
		m.setAccessible(true);
		return (boolean) m.invoke(null, holdFrames);
	}
}
