package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Additional TextInputSDL tests covering password mode, placeholder,
 * caret boundary operations, handleTextInput null/empty, maxChars=-1
 * unlimited, and narrow widget edge cases.
 *
 * Clipboard operations (Ctrl+V/C/C/X) and setFocused are not tested
 * here because they call through to SDL3 native methods.
 */
class TextInputSDLExtendedTest {

	@Test
	void passwordModeDefaultsToFalse() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		assertFalse(t.password);
	}

	@Test
	void passwordModeCanBeSetAndRead() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.password = true;
		assertTrue(t.password);
		t.password = false;
		assertFalse(t.password);
	}

	@Test
	void placeholderDefaultsToEmpty() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		assertEquals("", t.placeholder);
	}

	@Test
	void placeholderCanBeSet() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.placeholder = "Enter name";
		assertEquals("Enter name", t.placeholder);
	}

	@Test
	void setTextWithNullBecomesEmpty() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("hello");
		t.setText(null);
		assertEquals("", t.getText());
	}

	@Test
	void handleTextInputNullDoesNothing() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("abc");
		t.handleTextInput(null);
		assertEquals("abc", t.getText());
	}

	@Test
	void handleTextInputEmptyStringDoesNothing() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("abc");
		t.handleTextInput("");
		assertEquals("abc", t.getText());
	}

	@Test
	void backspaceAtStartOfTextDoesNothing() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("hello");

		// Move caret to start
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_BACKSPACE, 0, false));

		assertEquals("hello", t.getText(), "backspace at start should not change text");
	}

	@Test
	void deleteAtEndOfTextDoesNothing() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("hello");

		// Caret is already at end
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DELETE, 0, false));

		assertEquals("hello", t.getText(), "delete at end should not change text");
	}

	@Test
	void leftArrowAtStartDoesNothing() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("abc");

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, false));
		t.handleTextInput("X");

		assertEquals("Xabc", t.getText(), "left at start should insert at position 0");
	}

	@Test
	void rightArrowAtEndDoesNothing() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("abc");

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_END, 0, false));
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RIGHT, 0, false));
		t.handleTextInput("X");

		assertEquals("abcX", t.getText(), "right at end should insert at end");
	}

	@Test
	void maxCharsUnlimitedAllowsManyCharacters() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		assertEquals(-1, t.maxChars);

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 1000; i++) sb.append('a');
		t.handleTextInput(sb.toString());

		assertEquals(1000, t.getText().length());
	}

	@Test
	void maxCharsZeroActsAsUnlimited() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.maxChars = 0;  // only > 0 triggers truncation

		t.handleTextInput("hello");
		assertEquals("hello", t.getText(), "maxChars=0 means unlimited in this implementation");
	}

	@Test
	void maxCharsOneBlocksLongerInput() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.maxChars = 1;

		t.handleTextInput("hello");
		assertEquals("h", t.getText());
	}

	@Test
	void setTextTruncatesAtMaxCharsWithExistingContent() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.maxChars = 5;
		t.setText("hello world");
		assertEquals("hello", t.getText());
	}

	@Test
	void insertInMiddleOfTextWorks() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("ab");

		// Move caret between a and b
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, false));
		t.handleTextInput("XY");

		assertEquals("aXYb", t.getText());
	}

	@Test
	void clickToPositionCaretAtSpecificLocation() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("abcdef");

		// Click at x=40 (the 4th character area: (40 - (0+4)) / 16 = 2, so caret=2)
		t.update(40, 8, true);
		t.handleTextInput("Z");

		assertEquals("abZcdef", t.getText(), "insert should land at clicked caret position");
	}

	@Test
	void clickOnEmptyWidgetPositionsCaretAtZero() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		assertEquals("", t.getText());

		boolean activated = t.update(50, 8, true);
		assertTrue(activated);

		t.handleTextInput("X");
		assertEquals("X", t.getText());
	}

	@Test
	void narrowWidgetStillAcceptsInput() {
		// Very narrow widget: maxVisibleChars = max(1, (w-8)/16) = max(1, 0) = 1
		TextInputSDL t = new TextInputSDL(0, 0, 10, 18);
		t.handleTextInput("hello");

		assertEquals("hello", t.getText(), "text accepted even in narrow widget");
	}

	@Test
	void handleTextInputRespectsMaxCharsWithPartialRoom() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.maxChars = 5;
		t.setText("abc");

		// room = 2, input "hello" truncated to "he"
		t.handleTextInput("hello");
		assertEquals("abche", t.getText());
	}

	@Test
	void handleKeyDoesNothingWhenDisabled() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("abc");
		t.enabled = false;

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, false));
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_BACKSPACE, 0, false));
		assertEquals("abc", t.getText());
	}
}
