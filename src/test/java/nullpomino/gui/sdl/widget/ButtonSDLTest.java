package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.ResourceHolderSDL;
import nullpomino.gui.sdl.SoundManagerSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Pins ButtonSDL's input contracts. The button has three independent
 * activation paths (mouse click while hovering, keyboard fire-on-next-
 * update via SPACE/RETURN/KP_ENTER, and runnable execution) that all
 * need to keep working when the lobby dialogs route input through it.
 */
class ButtonSDLTest {

	private SoundManagerSDL originalSound;

	@BeforeEach
	void installNullSafeSoundManager() {
		// ButtonSDL.update calls soundManager.play("decide") on activation;
		// the sound manager null-safely no-ops without SDL, but we still
		// need a non-null instance so the call site is exercised.
		originalSound = ResourceHolderSDL.soundManager;
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
	}

	@AfterEach
	void restoreSoundManager() {
		ResourceHolderSDL.soundManager = originalSound;
	}

	@Test
	void constructorWithoutActionStoresGeometryAndLabel() {
		ButtonSDL b = new ButtonSDL(10, 20, 100, 30, "OK");

		assertEquals(10, b.x);
		assertEquals(100, b.w);
		assertEquals("OK", b.label);
	}

	@Test
	void constructorWithActionStoresRunnable() {
		Runnable r = () -> {};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", r);

		assertEquals(r, b.action);
	}

	@Test
	void clickInsideFiresActionAndReturnsTrue() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> fired[0] = true);

		boolean activated = b.update(10, 10, true);

		assertTrue(activated);
		assertTrue(fired[0]);
	}

	@Test
	void clickOutsideDoesNotFire() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> fired[0] = true);

		assertFalse(b.update(100, 100, true));
		assertFalse(fired[0]);
	}

	@Test
	void disabledButtonIgnoresClicks() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> fired[0] = true);
		b.enabled = false;

		assertFalse(b.update(10, 10, true));
		assertFalse(fired[0]);
	}

	@Test
	void hoverWithoutPressDoesNotFire() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> fired[0] = true);

		assertFalse(b.update(10, 10, false));
		assertFalse(fired[0]);
	}

	@Test
	void handleKeyArmsKeyboardFireForNextUpdate() {
		boolean[] fired = {false};
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", () -> fired[0] = true);
		b.focused = true;

		b.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		// Pointer is offscreen — keyboard activation should still fire on the
		// next update tick.
		assertTrue(b.update(-1, -1, false));
		assertTrue(fired[0]);
	}

	@Test
	void handleKeyAcceptsKpEnterAndSpace() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", null);
		b.focused = true;

		b.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_KP_ENTER, 0, false));
		assertTrue(b.update(-1, -1, false));

		b.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_SPACE, 0, false));
		assertTrue(b.update(-1, -1, false));
	}

	@Test
	void handleKeyIgnoresWhenUnfocused() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", null);
		b.focused = false;

		b.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertFalse(b.update(-1, -1, false));
	}

	@Test
	void handleKeyIgnoresRepeatEvents() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", null);
		b.focused = true;

		// repeat=true must not arm the fire — otherwise holding Enter would
		// activate the button every frame.
		b.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, true));
		assertFalse(b.update(-1, -1, false));
	}

	@Test
	void handleKeyIgnoresUnrelatedScancodes() {
		ButtonSDL b = new ButtonSDL(0, 0, 50, 20, "Go", null);
		b.focused = true;

		b.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_PAGEUP, 0, false));
		assertFalse(b.update(-1, -1, false));
	}
}
