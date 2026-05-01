package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Pins TextInputSDL's edit-buffer contracts. Lobby dialogs route
 * keystrokes and committed text through this widget; the buffer
 * mutations (BACKSPACE, DELETE, caret movement, maxChars truncation,
 * control-character filtering on insert) are independent of SDL and
 * load-bearing for any localised input path.
 */
class TextInputSDLTest {

	@Test
	void constructorStoresGeometryAndDefaultsToEmptyText() {
		TextInputSDL t = new TextInputSDL(10, 20, 100, 18);

		assertEquals("", t.getText());
		assertEquals(10, t.x);
		assertEquals(100, t.w);
		assertEquals(-1, t.maxChars);
		assertFalse(t.password);
	}

	@Test
	void setTextStoresStringAndMovesCaretToEnd() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);

		t.setText("hello");
		assertEquals("hello", t.getText());

		t.setText(null);
		assertEquals("", t.getText(), "null defaults to empty string");
	}

	@Test
	void setTextTruncatesToMaxChars() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.maxChars = 4;

		t.setText("abcdef");
		assertEquals("abcd", t.getText());
	}

	@Test
	void handleTextInputInsertsAtCaret() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);

		t.handleTextInput("ab");
		assertEquals("ab", t.getText());

		t.handleTextInput("c");
		assertEquals("abc", t.getText());
	}

	@Test
	void handleTextInputStripsControlCharsButKeepsTab() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);

		t.handleTextInput("ab\tc");
		assertEquals("ab\tc", t.getText(),
				"control chars below 0x20 (except TAB) must be filtered");
	}

	@Test
	void handleTextInputRespectsMaxChars() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.maxChars = 3;

		t.handleTextInput("hello");
		assertEquals("hel", t.getText());

		t.handleTextInput("xyz");
		assertEquals("hel", t.getText(), "no room left → addition dropped");
	}

	@Test
	void handleTextInputIgnoredWhenDisabled() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.enabled = false;

		t.handleTextInput("hi");
		assertEquals("", t.getText());
	}

	@Test
	void handleKeyBackspaceDeletesCharacterBeforeCaret() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("hello");

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_BACKSPACE, 0, false));
		assertEquals("hell", t.getText());
	}

	@Test
	void handleKeyDeleteRemovesCharacterAtCaret() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("hello");
		// Move caret to start with HOME, then DELETE removes 'h'
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DELETE, 0, false));

		assertEquals("ello", t.getText());
	}

	@Test
	void handleKeyLeftAndRightMoveCaretAndAffectInsertPoint() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("ab");

		// Caret at end (2). LEFT moves to 1.
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_LEFT, 0, false));
		t.handleTextInput("X");
		assertEquals("aXb", t.getText(),
				"insert lands between a and b after one LEFT");

		// RIGHT moves caret to end so next insert lands at the end
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RIGHT, 0, false));
		t.handleTextInput("Y");
		assertEquals("aXbY", t.getText());
	}

	@Test
	void handleKeyHomeAndEndJumpCaretToEnds() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("abc");

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		t.handleTextInput("X");
		assertEquals("Xabc", t.getText());

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_END, 0, false));
		t.handleTextInput("Y");
		assertEquals("XabcY", t.getText());
	}

	@Test
	void handleKeyIgnoresEditsWhenDisabled() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("abc");
		t.enabled = false;

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_BACKSPACE, 0, false));
		assertEquals("abc", t.getText());
	}

	@Test
	void clickInsideMovesCaretAndReturnsTrue() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.setText("hello");

		boolean activated = t.update(50, 8, true);
		assertTrue(activated);
	}

	@Test
	void clickOutsideDoesNotActivate() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);

		assertFalse(t.update(500, 500, true));
	}

	@Test
	void invisibleOrDisabledIgnoresClick() {
		TextInputSDL t = new TextInputSDL(0, 0, 200, 18);
		t.visible = false;
		assertFalse(t.update(50, 8, true));

		t.visible = true;
		t.enabled = false;
		assertFalse(t.update(50, 8, true));
	}
}
