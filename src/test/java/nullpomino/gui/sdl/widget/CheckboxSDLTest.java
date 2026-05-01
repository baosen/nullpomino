package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Pins CheckboxSDL's input contracts. The widget is used in lobby
 * dialogs for boolean toggles; mouse activation must require an
 * inside-pointer click and keyboard activation must respect SPACE
 * and RETURN. The render path is SDL-bound and skipped here.
 */
class CheckboxSDLTest {

	@Test
	void constructorStoresGeometryLabelAndInitialState() {
		CheckboxSDL c = new CheckboxSDL(10, 20, 100, 18, "Sticky", true);

		assertEquals(10, c.x);
		assertEquals(20, c.y);
		assertEquals(100, c.w);
		assertEquals(18, c.h);
		assertEquals("Sticky", c.label);
		assertTrue(c.checked);
	}

	@Test
	void clickInsideTogglesCheckedAndReturnsActivated() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);

		boolean activated = c.update(10, 8, true);

		assertTrue(activated);
		assertTrue(c.checked);
	}

	@Test
	void clickInsideAgainTogglesBackToFalse() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", true);

		c.update(10, 8, true);
		assertFalse(c.checked);
	}

	@Test
	void clickOutsideDoesNotToggle() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);

		boolean activated = c.update(100, 100, true);

		assertFalse(activated);
		assertFalse(c.checked);
	}

	@Test
	void hoverWithoutPressDoesNotToggle() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);

		assertFalse(c.update(10, 8, false));
		assertFalse(c.checked);
	}

	@Test
	void disabledCheckboxIgnoresClicks() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);
		c.enabled = false;

		assertFalse(c.update(10, 8, true));
		assertFalse(c.checked);
	}

	@Test
	void invisibleCheckboxIgnoresClicks() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);
		c.visible = false;

		assertFalse(c.update(10, 8, true));
		assertFalse(c.checked);
	}

	@Test
	void handleKeyTogglesOnSpaceAndReturn() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);

		c.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_SPACE, 0, false));
		assertTrue(c.checked);

		c.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertFalse(c.checked);
	}

	@Test
	void handleKeyIgnoresUnrelatedScancodes() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);

		c.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_PAGEUP, 0, false));
		assertFalse(c.checked);
	}

	@Test
	void handleKeyDoesNothingWhenDisabled() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);
		c.enabled = false;

		c.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_SPACE, 0, false));
		assertFalse(c.checked);
	}
}
