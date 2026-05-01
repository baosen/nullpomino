package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Pins SpinnerSDL's clamping and keyboard-step contracts. Lobby
 * dialogs use the spinner for bounded numeric inputs (DAS, ARE,
 * volume); the [min,max] clamp on setValue and on getValue
 * (commit-from-editor) is what keeps stale text-input from
 * smuggling out-of-range values into config.
 *
 * The render path and mouse interaction touch SDL bindings via
 * MouseInputSDL.mouseInput; we test the keyboard and direct API
 * paths only.
 */
class SpinnerSDLTest {

	@Test
	void constructorClampsInitialValueToMinAndMax() {
		SpinnerSDL low = new SpinnerSDL(0, 0, 100, 20, 0, 10, 1, -5);
		assertEquals(0, low.getValue());

		SpinnerSDL high = new SpinnerSDL(0, 0, 100, 20, 0, 10, 1, 99);
		assertEquals(10, high.getValue());

		SpinnerSDL mid = new SpinnerSDL(0, 0, 100, 20, 0, 10, 1, 5);
		assertEquals(5, mid.getValue());
	}

	@Test
	void setValueClampsToBounds() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 50);

		s.setValue(-1);
		assertEquals(0, s.getValue());

		s.setValue(200);
		assertEquals(100, s.getValue());

		s.setValue(42);
		assertEquals(42, s.getValue());
	}

	@Test
	void handleKeyRightAndUpAdvanceByStep() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 5, 10);

		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RIGHT, 0, false));
		assertEquals(15, s.getValue());

		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_UP, 0, false));
		assertEquals(20, s.getValue());
	}

	@Test
	void handleKeyLeftAndDownDecrementByStep() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 5, 30);

		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, false));
		assertEquals(25, s.getValue());

		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(20, s.getValue());
	}

	@Test
	void handleKeyClampsAtMinAndMaxBounds() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 10, 5, 0);

		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, false));
		assertEquals(0, s.getValue(), "decrement past min stays at min");

		s.setValue(10);
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RIGHT, 0, false));
		assertEquals(10, s.getValue(), "increment past max stays at max");
	}

	@Test
	void handleKeyEnterCommitsEditedTextThroughClamp() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 50, 1, 5);

		// Simulate the user typing "999" into the inner TextInput
		s.handleTextInput("9");
		s.handleTextInput("9");
		s.handleTextInput("9");

		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));

		// Commit clamps "5999" → max 50
		assertEquals(50, s.getValue());
	}

	@Test
	void handleTextInputAcceptsDigitsAndLeadingMinus() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, -100, 100, 1, 0);

		// Setting via the editor: clear the prefilled "0" first by simulating
		// HOME + DELETE on the inner text via setValue (which resets the editor).
		s.setValue(0);
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DELETE, 0, false));
		// Now editor empty; type "-25"
		s.handleTextInput("-25");
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));

		assertEquals(-25, s.getValue());
	}

	@Test
	void handleTextInputFiltersNonDigitNonMinusCharacters() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 0);
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DELETE, 0, false));

		// Non-digit input stripped — this should leave the editor as "12"
		s.handleTextInput("a1b2c");
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));

		assertEquals(12, s.getValue());
	}

	@Test
	void handleKeyDoesNothingWhenDisabled() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 5, 50);
		s.enabled = false;

		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RIGHT, 0, false));
		assertEquals(50, s.getValue());
	}

	@Test
	void handleTextInputDoesNothingWhenDisabled() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 7);
		s.enabled = false;

		s.handleTextInput("9");
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertEquals(7, s.getValue());
	}

	@Test
	void getValueCommitsAnyPendingEditorTextOnRead() {
		SpinnerSDL s = new SpinnerSDL(0, 0, 100, 20, 0, 100, 1, 0);
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		s.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DELETE, 0, false));
		s.handleTextInput("33");

		// No explicit RETURN — getValue itself commits.
		assertEquals(33, s.getValue());
	}
}
