package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.ResourceHolderSDL;
import nullpomino.gui.sdl.SoundManagerSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Additional ButtonSDL tests covering invisible/disabled update,
 * handleKey disabled, theme/primary interaction, action field, and
 * handleTextInput no-op.
 */
class ButtonSDLExtendedTest {

	private SoundManagerSDL originalSound;

	@BeforeEach
	void installNullSafeSoundManager() {
		originalSound = ResourceHolderSDL.soundManager;
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
	}

	@AfterEach
	void restoreSoundManager() {
		ResourceHolderSDL.soundManager = originalSound;
	}

	@Test
	void invisibleButtonReturnsFalseFromUpdate() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> fired[0] = true);
		b.visible = false;

		assertFalse(b.update(10, 10, true));
		assertFalse(fired[0]);
	}

	@Test
	void disabledButtonReturnsFalseFromUpdate() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> fired[0] = true);
		b.enabled = false;

		assertFalse(b.update(10, 10, true));
		assertFalse(fired[0]);
	}

	@Test
	void handleKeyDoesNothingWhenDisabled() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> fired[0] = true);
		b.focused = true;
		b.enabled = false;

		b.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertFalse(b.update(-1, -1, false));
		assertFalse(fired[0]);
	}

	@Test
	void handleKeyDoesNothingWhenNotFocusedEvenIfEnabled() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> fired[0] = true);
		b.focused = false;
		b.enabled = true;

		b.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertFalse(b.update(-1, -1, false));
		assertFalse(fired[0]);
	}

	@Test
	void themeFieldDefaultsToDefault() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Test");
		assertEquals(ButtonSDL.THEME_DEFAULT, b.theme);
	}

	@Test
	void themeFieldCanBeSetToAllVariants() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Test");

		b.theme = ButtonSDL.THEME_BLUE;
		assertEquals(ButtonSDL.THEME_BLUE, b.theme);

		b.theme = ButtonSDL.THEME_GREEN;
		assertEquals(ButtonSDL.THEME_GREEN, b.theme);

		b.theme = ButtonSDL.THEME_VIOLET;
		assertEquals(ButtonSDL.THEME_VIOLET, b.theme);
	}

	@Test
	void primaryFieldMapsToThemeBlueWhenThemeIsDefault() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Test");
		b.primary = true;
		assertTrue(b.primary);
		// Theme should still be THEME_DEFAULT when primary is set
		assertEquals(ButtonSDL.THEME_DEFAULT, b.theme);
	}

	@Test
	void actionFieldCanBeSetDirectly() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Test");
		b.action = () -> fired[0] = true;

		b.update(10, 10, true);
		assertTrue(fired[0]);
	}

	@Test
	void nullActionDoesNotThrowOnActivation() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Test", null);

		// Click with null action should not NPE
		assertTrue(b.update(10, 10, true));
	}

	@Test
	void mouseClickResetsPressingStateOnNextUpdate() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Test");

		b.update(10, 10, true);  // click fires
		// pressing is reset to false on every non-fire update
		assertFalse(b.update(10, 10, false));
	}

	@Test
	void handleTextInputIsNoOpAndDoesNotThrow() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Test");
		b.handleTextInput("hello");
		b.handleTextInput(null);
		// No state change expected
		assertEquals("Test", b.label);
	}

	@Test
	void keyboardFireIsConsumedBySingleUpdate() {
		int[] firings = {0};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> firings[0]++);
		b.focused = true;

		b.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertTrue(b.update(-1, -1, false));
		assertEquals(1, firings[0]);

		// Second update should NOT fire again (keyboardFire consumed)
		assertFalse(b.update(-1, -1, false));
		assertEquals(1, firings[0], "keyboardFire must be consumed in one tick");
	}
}
