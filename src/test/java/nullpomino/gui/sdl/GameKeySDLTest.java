package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.GameKeyDummy;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.util.CustomProperties;

class GameKeySDLTest {

	private GameKeySDL[] originalGameKey;

	@BeforeEach
	void snapshotStaticState() {
		originalGameKey = GameKeySDL.gamekey;
	}

	@AfterEach
	void restoreStaticState() {
		GameKeySDL.gamekey = originalGameKey;
	}

	@Test
	void defaultKeysHaveExpectedShape() {
		// Two contexts (ingame, menu), three preset types each, MAX_BUTTON
		// scancodes per preset.
		assertEquals(2, GameKeySDL.DEFAULTKEYS.length);
		assertEquals(3, GameKeySDL.DEFAULTKEYS[0].length);
		assertEquals(3, GameKeySDL.DEFAULTKEYS[1].length);
		for(int ctx = 0; ctx < GameKeySDL.DEFAULTKEYS.length; ctx++) {
			for(int preset = 0; preset < GameKeySDL.DEFAULTKEYS[ctx].length; preset++) {
				assertEquals(GameKeyDummy.MAX_BUTTON, GameKeySDL.DEFAULTKEYS[ctx][preset].length,
						"ctx=" + ctx + " preset=" + preset);
			}
		}
	}

	@Test
	void initGlobalGameKeySDLAllocatesTwoPlayerInstances() {
		GameKeySDL.initGlobalGameKeySDL();

		assertNotNull(GameKeySDL.gamekey);
		assertEquals(2, GameKeySDL.gamekey.length);
		assertNotNull(GameKeySDL.gamekey[0]);
		assertNotNull(GameKeySDL.gamekey[1]);
		assertEquals(0, GameKeySDL.gamekey[0].player);
		assertEquals(1, GameKeySDL.gamekey[1].player);
		assertNotSame(GameKeySDL.gamekey[0], GameKeySDL.gamekey[1]);
	}

	@Test
	void hatBitmaskConstantsAreSingleBitsInDocumentedOrder() {
		assertEquals(0x01, GameKeySDL.SDL_HAT_UP);
		assertEquals(0x02, GameKeySDL.SDL_HAT_RIGHT);
		assertEquals(0x04, GameKeySDL.SDL_HAT_DOWN);
		assertEquals(0x08, GameKeySDL.SDL_HAT_LEFT);
	}

	@Test
	void loadDefaultGameKeymapPopulatesIngameKeymap() {
		GameKeySDL key = new GameKeySDL(0);

		key.loadDefaultGameKeymap(0);

		assertArrayEquals(GameKeySDL.DEFAULTKEYS[0][0], key.keymap);
	}

	@Test
	void loadDefaultGameKeymapDoesNotMutateMenuKeymap() {
		GameKeySDL key = new GameKeySDL(0);
		java.util.Arrays.fill(key.keymapNav, 12345);

		key.loadDefaultGameKeymap(1);

		assertArrayEquals(GameKeySDL.DEFAULTKEYS[0][1], key.keymap);
		for(int v : key.keymapNav) assertEquals(12345, v);
	}

	@Test
	void loadDefaultMenuKeymapPopulatesNavKeymapWithoutTouchingGameKeymap() {
		GameKeySDL key = new GameKeySDL(0);
		java.util.Arrays.fill(key.keymap, 99999);

		key.loadDefaultMenuKeymap(2);

		assertArrayEquals(GameKeySDL.DEFAULTKEYS[1][2], key.keymapNav);
		for(int v : key.keymap) assertEquals(99999, v);
	}

	@Test
	void loadDefaultKeymapNoArgUsesBlockboxPresetForBothContexts() {
		GameKeySDL key = new GameKeySDL(0);

		key.loadDefaultKeymap();

		assertArrayEquals(GameKeySDL.DEFAULTKEYS[0][0], key.keymap);
		assertArrayEquals(GameKeySDL.DEFAULTKEYS[1][0], key.keymapNav);
	}

	@Test
	void loadDefaultKeymapWithTypePopulatesBothContexts() {
		GameKeySDL key = new GameKeySDL(0);

		key.loadDefaultKeymap(2);

		assertArrayEquals(GameKeySDL.DEFAULTKEYS[0][2], key.keymap);
		assertArrayEquals(GameKeySDL.DEFAULTKEYS[1][2], key.keymapNav);
	}

	@Test
	void loadConfigDelegatesToSuperAndPullsJoyBorder() {
		CustomProperties prop = new CustomProperties();
		prop.setProperty("key.p1.up", SDLConstants.SDL_SCANCODE_UP);
		prop.setProperty("joyBorder.p1", 32);

		GameKeySDL key = new GameKeySDL(1);
		key.loadConfig(prop);

		assertEquals(SDLConstants.SDL_SCANCODE_UP, key.keymap[GameKeyDummy.BUTTON_UP]);
		assertEquals(32, key.joyBorder);
	}

	@Test
	void updateMenuKeyboardOnlyOverloadIncrementsHeldButtons() {
		GameKeySDL key = new GameKeySDL(0);
		key.loadDefaultKeymap(0);

		boolean[] keyboard = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		keyboard[key.keymapNav[GameKeyDummy.BUTTON_DOWN]] = true;

		key.update(keyboard);
		key.update(keyboard);

		assertEquals(2, key.getInputState(GameKeyDummy.BUTTON_DOWN));
	}

	@Test
	void updateIngameOverloadReadsFromKeymapInsteadOfKeymapNav() {
		GameKeySDL key = new GameKeySDL(0);
		key.loadDefaultKeymap(0);
		// Re-aim menu UP at a different scancode so the two overloads can
		// be told apart by which slot they listen to.
		key.keymapNav[GameKeyDummy.BUTTON_UP] = SDLConstants.SDL_SCANCODE_F1;
		key.keymap[GameKeyDummy.BUTTON_UP] = SDLConstants.SDL_SCANCODE_W;

		boolean[] keyboard = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		keyboard[SDLConstants.SDL_SCANCODE_W] = true;

		key.update(keyboard, true);

		assertEquals(1, key.getInputState(GameKeyDummy.BUTTON_UP));
	}

	@Test
	void updateJoystickAxisAndHatDriveDirectionalInputsWhenKeyboardIsCold() {
		GameKeySDL key = new GameKeySDL(0);
		key.loadDefaultKeymap(0);
		key.joyBorder = 100;
		boolean[] keyboard = new boolean[SDLConstants.SDL_SCANCODE_COUNT];

		// Y axis below -joyBorder fires UP; hat-down fires DOWN; X axis
		// above +joyBorder fires RIGHT.
		key.update(keyboard, null, 200, -200, GameKeySDL.SDL_HAT_DOWN);

		assertEquals(1, key.getInputState(GameKeyDummy.BUTTON_UP));
		assertEquals(1, key.getInputState(GameKeyDummy.BUTTON_DOWN));
		assertEquals(1, key.getInputState(GameKeyDummy.BUTTON_RIGHT));
		assertEquals(0, key.getInputState(GameKeyDummy.BUTTON_LEFT));
	}

	@Test
	void updateClearsInputStateWhenAllSourcesGoCold() {
		GameKeySDL key = new GameKeySDL(0);
		key.loadDefaultKeymap(0);
		key.joyBorder = 100;
		boolean[] keyboard = new boolean[SDLConstants.SDL_SCANCODE_COUNT];

		// Press LEFT for two frames, then release everything.
		key.update(keyboard, null, -200, 0, 0);
		key.update(keyboard, null, -200, 0, 0);
		assertEquals(2, key.getInputState(GameKeyDummy.BUTTON_LEFT));

		key.update(keyboard, null, 0, 0, 0);

		assertEquals(0, key.getInputState(GameKeyDummy.BUTTON_LEFT));
	}

	@Test
	void updateRespondsToHatLeftAndUpAndPicksAxisThresholdsCorrectly() {
		GameKeySDL key = new GameKeySDL(0);
		key.loadDefaultKeymap(0);
		key.joyBorder = 50;
		boolean[] keyboard = new boolean[SDLConstants.SDL_SCANCODE_COUNT];

		key.update(keyboard, null, 0, 0, GameKeySDL.SDL_HAT_LEFT | GameKeySDL.SDL_HAT_UP);

		assertEquals(1, key.getInputState(GameKeyDummy.BUTTON_LEFT));
		assertEquals(1, key.getInputState(GameKeyDummy.BUTTON_UP));
		assertEquals(0, key.getInputState(GameKeyDummy.BUTTON_RIGHT));
		assertEquals(0, key.getInputState(GameKeyDummy.BUTTON_DOWN));
	}

	@Test
	void updateMiscButtonReadsFromJoyButtonArrayWhenMappedAndIgnoresOutOfRangeIndex() {
		GameKeySDL key = new GameKeySDL(0);
		key.loadDefaultKeymap(0);
		key.buttonmap[GameKeyDummy.BUTTON_A] = 2;
		// BUTTON_B's mapping is intentionally out of range to exercise the
		// ArrayIndexOutOfBoundsException catch in update().
		key.buttonmap[GameKeyDummy.BUTTON_B] = 99;

		boolean[] keyboard = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		boolean[] joyButton = new boolean[3];
		joyButton[2] = true;

		key.update(keyboard, joyButton, 0, 0, 0);

		assertEquals(1, key.getInputState(GameKeyDummy.BUTTON_A));
		assertEquals(0, key.getInputState(GameKeyDummy.BUTTON_B));
	}

	@Test
	void updateMiscButtonIgnoresJoystickWhenJoyButtonArrayIsNull() {
		GameKeySDL key = new GameKeySDL(0);
		key.loadDefaultKeymap(0);
		boolean[] keyboard = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		// Press a key that maps to BUTTON_A.
		keyboard[key.keymapNav[GameKeyDummy.BUTTON_A]] = true;

		key.update(keyboard, null, 0, 0, 0);

		assertEquals(1, key.getInputState(GameKeyDummy.BUTTON_A));
	}
}
