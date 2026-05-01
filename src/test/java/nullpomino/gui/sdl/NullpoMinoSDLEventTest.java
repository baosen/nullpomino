package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Pins the per-frame event-buffer helpers on NullpoMinoSDL. Menu
 * states call these every frame to detect Escape presses and read
 * committed text input. Both helpers operate on static buffers, so
 * we save/restore them around each test to keep isolation.
 */
class NullpoMinoSDLEventTest {

	private NullpoMinoSDL.KeyEvent[] originalEvents;
	private String originalPending;

	@BeforeEach
	void saveStaticBuffers() {
		originalEvents = NullpoMinoSDL.frameKeyEvents.toArray(new NullpoMinoSDL.KeyEvent[0]);
		originalPending = NullpoMinoSDL.pendingTextInput.toString();
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.pendingTextInput.setLength(0);
	}

	@AfterEach
	void restoreStaticBuffers() {
		NullpoMinoSDL.frameKeyEvents.clear();
		for (NullpoMinoSDL.KeyEvent ev : originalEvents) {
			NullpoMinoSDL.frameKeyEvents.add(ev);
		}
		NullpoMinoSDL.pendingTextInput.setLength(0);
		NullpoMinoSDL.pendingTextInput.append(originalPending);
	}

	@Test
	void isEscapePushedThisFrameReturnsFalseWhenNoEvents() {
		assertFalse(NullpoMinoSDL.isEscapePushedThisFrame());
	}

	@Test
	void isEscapePushedThisFrameReturnsTrueOnNonRepeatEscape() {
		NullpoMinoSDL.frameKeyEvents.add(
				new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));

		assertTrue(NullpoMinoSDL.isEscapePushedThisFrame());
	}

	@Test
	void isEscapePushedThisFrameSkipsRepeatedEscapes() {
		// Hold-repeat shouldn't count — menus would back out endlessly while
		// the user holds Escape.
		NullpoMinoSDL.frameKeyEvents.add(
				new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, true));

		assertFalse(NullpoMinoSDL.isEscapePushedThisFrame());
	}

	@Test
	void isEscapePushedThisFrameIgnoresNonEscapeKeys() {
		NullpoMinoSDL.frameKeyEvents.add(
				new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_PAGEUP, 0, false));
		NullpoMinoSDL.frameKeyEvents.add(
				new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_PAGEDOWN, 0, false));

		assertFalse(NullpoMinoSDL.isEscapePushedThisFrame());
	}

	@Test
	void consumeTextInputReturnsEmptyWhenBufferIsEmpty() {
		assertEquals("", NullpoMinoSDL.consumeTextInput());
	}

	@Test
	void consumeTextInputReadsAndClearsBuffer() {
		NullpoMinoSDL.pendingTextInput.append("hello");

		assertEquals("hello", NullpoMinoSDL.consumeTextInput());
		assertEquals("", NullpoMinoSDL.consumeTextInput(),
				"second consume must observe an empty buffer");
	}

	@Test
	void keyEventConstructorBundlesScancodeKeymodAndRepeatFlag() {
		NullpoMinoSDL.KeyEvent ev = new NullpoMinoSDL.KeyEvent(
				SDLConstants.SDL_SCANCODE_ESCAPE, 0x40, true);

		assertEquals(SDLConstants.SDL_SCANCODE_ESCAPE, ev.scancode);
		assertEquals(0x40, ev.keymod);
		assertTrue(ev.repeat);
	}
}
