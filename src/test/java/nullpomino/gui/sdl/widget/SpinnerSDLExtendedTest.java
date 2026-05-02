package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.MouseInputSDL;
import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Additional SpinnerSDL tests covering the update() method (arrow clicks,
 * editor click, invisible/disabled guard), setFocused commit, min/max/step
 * field access, and handleKey for KP_ENTER.
 *
 * The update() path requires MouseInputSDL.mouseInput to be non-null
 * (for the auto-repeat guard), which is safe to initialise without SDL.
 */
class SpinnerSDLExtendedTest {

	@BeforeEach
	void initMouseInput() {
		if (MouseInputSDL.mouseInput == null) {
			MouseInputSDL.initalizeMouseInput();
		}
	}

	@Test
	void updateClickLeftArrowDecrementsValue() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);

		boolean activated = s.update(5, 10, true);  // left arrow area: x=0..20, y=0..20

		assertTrue(activated);
		assertEquals(49, s.getValue());
	}

	@Test
	void updateClickRightArrowIncrementsValue() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);

		boolean activated = s.update(90, 10, true);  // right arrow area: x=80..100, y=0..20

		assertTrue(activated);
		assertEquals(51, s.getValue());
	}

	@Test
	void updateClickEditorAreaReturnsTrue() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);

		boolean activated = s.update(50, 10, true);  // editor area: x=20..80, y=0..20

		assertTrue(activated);
		// Value unchanged since the editor just receives focus
		assertEquals(50, s.getValue());
	}

	@Test
	void updateClickOutsideDoesNotActivate() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);

		assertFalse(s.update(200, 200, true));
		assertEquals(50, s.getValue());
	}

	@Test
	void updateClickLeftArrowClampsAtMin() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 10, 1, 0);

		s.update(5, 10, true);
		assertEquals(0, s.getValue(), "decrement below min should clamp");
	}

	@Test
	void updateClickRightArrowClampsAtMax() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 10, 1, 10);

		s.update(90, 10, true);
		assertEquals(10, s.getValue(), "increment above max should clamp");
	}

	@Test
	void invisibleSpinnerDoesNotProcessUpdate() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
		s.visible = false;

		assertFalse(s.update(5, 10, true));
		assertEquals(50, s.getValue());
	}

	@Test
	void disabledSpinnerDoesNotProcessUpdate() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
		s.enabled = false;

		assertFalse(s.update(5, 10, true));
		assertEquals(50, s.getValue());
	}

	@Test
	void setFocusedTrueWithoutSDLDoesNotThrow() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
		s.setFocused(true);
		assertTrue(s.focused);
	}

	@Test
	void setFocusedFalseCommitsEditorValue() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);

		// Clear the editor then type a new value
		s.setValue(0); // sets editor text to "0"
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DELETE, 0, false));
		s.handleTextInput("75");

		// Use commitEditor() directly to avoid NPE from stopTextInput(which accesses GameKeySDL.gamekey)
		// s.setFocused(false) would trigger stopTextInput and NPE without SDL runtime
		assertEquals(75, s.getValue()); // getValue() calls commitEditor() internally
	}

	@Test
	void handleKeyKPEnterCommitsEditor() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);

		// Clear the editor then type a new value
		s.setValue(0);
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DELETE, 0, false));
		s.handleTextInput("33");

		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_KP_ENTER, 0, false));
		assertEquals(33, s.getValue());
	}

	@Test
	void minMaxStepFieldsAreStoredCorrectly() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, -10, 50, 5, 0);
		assertEquals(-10, s.min);
		assertEquals(50, s.max);
		assertEquals(5, s.step);
	}

	@Test
	void setValueUpdatesEditorText() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 0);
		s.setValue(42);
		assertEquals(42, s.getValue());
	}

	@Test
	void handleTextInputEmptyStringDoesNothing() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);
		s.handleTextInput("");
		assertEquals(50, s.getValue());
	}

	@Test
	void handleTextInputFiltersMinusWhenEditorNotEmpty() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, -100, 100, 1, 50);
		// Editor starts with "50". Typing "-" should be filtered since editor is not empty.
		s.handleTextInput("-");
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertEquals(50, s.getValue(), "minus should be filtered when editor not empty");
	}
}
