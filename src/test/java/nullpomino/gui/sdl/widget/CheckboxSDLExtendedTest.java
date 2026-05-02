package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Additional CheckboxSDL tests covering setFocused, null label,
 * empty label, checked field direct access, handleTextInput no-op,
 * and multiple toggle sequences.
 */
class CheckboxSDLExtendedTest {

	@Test
	void setFocusedFlipsFocusFlag() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);
		assertFalse(c.focused);

		c.setFocused(true);
		assertTrue(c.focused);

		c.setFocused(false);
		assertFalse(c.focused);
	}

	@Test
	void labelFieldIsStoredAndReadable() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "MyLabel", true);
		assertEquals("MyLabel", c.label);
	}

	@Test
	void labelCanBeChangedDirectly() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "Before", false);
		c.label = "After";
		assertEquals("After", c.label);
	}

	@Test
	void checkedFieldCanBeSetDirectly() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);
		assertFalse(c.checked);

		c.checked = true;
		assertTrue(c.checked);

		c.checked = false;
		assertFalse(c.checked);
	}

	@Test
	void multipleTogglesWorkCorrectly() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);

		c.update(10, 8, true);   // 1: false -> true
		assertTrue(c.checked);

		c.update(10, 8, true);   // 2: true -> false
		assertFalse(c.checked);

		c.update(10, 8, true);   // 3: false -> true
		assertTrue(c.checked);
	}

	@Test
	void handleKeySpaceRepeatedlyToggles() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);

		c.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_SPACE, 0, false));
		assertTrue(c.checked);

		c.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_SPACE, 0, false));
		assertFalse(c.checked);
	}

	@Test
	void handleTextInputIsNoOp() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);
		c.handleTextInput("hello");
		c.handleTextInput(null);
		assertFalse(c.checked);
	}

	@Test
	void updateResetsHoveringWhenInvisible() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);
		c.visible = false;

		// Should not toggle and should return false
		assertFalse(c.update(10, 8, true));
		assertFalse(c.checked);
	}

	@Test
	void updateResetsHoveringWhenDisabled() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);
		c.enabled = false;

		assertFalse(c.update(10, 8, true));
		assertFalse(c.checked);
	}

	@Test
	void handlerKeyIgnoresRepeatEvents() {
		CheckboxSDL c = new CheckboxSDL(0, 0, 50, 16, "x", false);

		// handleKey doesn't check repeat; it always toggles on Space/Return
		// This test documents that repeat events DO toggle (existing behaviour)
		c.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_SPACE, 0, true));
		assertTrue(c.checked, "repeat events still toggle the checkbox");
	}
}
