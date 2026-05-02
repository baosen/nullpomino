package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import nullpomino.gui.sdl.binding.SDLConstants;

import org.junit.jupiter.api.Test;

/**
 * Pins the static helper {@code isListJumpKey} on
 * {@link StateNetServerSelectSDL}: it returns true for the four
 * keyboard navigation keys that should drop directly into the server
 * list (PageUp, PageDown, Home, End) and false for everything else.
 *
 * <p>The full server-select state needs a live netplay session, but
 * this private static can be exercised through reflection so a
 * regression in the hot keymap surfaces here.
 */
class StateNetServerSelectSDLTest {

	@Test
	void isListJumpKeyReturnsTrueForPageUp() throws Exception {
		assertTrue(invoke(eventOf(SDLConstants.SDL_SCANCODE_PAGEUP, false)));
	}

	@Test
	void isListJumpKeyReturnsTrueForPageDown() throws Exception {
		assertTrue(invoke(eventOf(SDLConstants.SDL_SCANCODE_PAGEDOWN, false)));
	}

	@Test
	void isListJumpKeyReturnsTrueForHome() throws Exception {
		assertTrue(invoke(eventOf(SDLConstants.SDL_SCANCODE_HOME, false)));
	}

	@Test
	void isListJumpKeyReturnsTrueForEnd() throws Exception {
		assertTrue(invoke(eventOf(SDLConstants.SDL_SCANCODE_END, false)));
	}

	@Test
	void isListJumpKeyReturnsFalseForUnrelatedKeys() throws Exception {
		// Sample of unrelated keys: arrow keys, letters, modifier keys.
		// None should be claimed as list-jump.
		assertFalse(invoke(eventOf(SDLConstants.SDL_SCANCODE_UP, false)));
		assertFalse(invoke(eventOf(SDLConstants.SDL_SCANCODE_DOWN, false)));
		assertFalse(invoke(eventOf(SDLConstants.SDL_SCANCODE_LEFT, false)));
		assertFalse(invoke(eventOf(SDLConstants.SDL_SCANCODE_RIGHT, false)));
		assertFalse(invoke(eventOf(SDLConstants.SDL_SCANCODE_RETURN, false)));
		assertFalse(invoke(eventOf(SDLConstants.SDL_SCANCODE_ESCAPE, false)));
		assertFalse(invoke(eventOf(SDLConstants.SDL_SCANCODE_TAB, false)));
		assertFalse(invoke(eventOf(SDLConstants.SDL_SCANCODE_A, false)));
	}

	@Test
	void isListJumpKeyIgnoresTheRepeatFlag() throws Exception {
		// repeat=true should still claim the four jump keys — the
		// helper only inspects scancode, so auto-repeat at PageDown
		// keeps scrolling.
		assertTrue(invoke(eventOf(SDLConstants.SDL_SCANCODE_PAGEDOWN, true)));
		assertTrue(invoke(eventOf(SDLConstants.SDL_SCANCODE_HOME, true)));
		// And still rejects unrelated keys regardless of repeat.
		assertFalse(invoke(eventOf(SDLConstants.SDL_SCANCODE_UP, true)));
	}

	private static NullpoMinoSDL.KeyEvent eventOf(int scancode, boolean repeat) {
		return new NullpoMinoSDL.KeyEvent(scancode, 0, repeat);
	}

	private static boolean invoke(NullpoMinoSDL.KeyEvent ev) throws Exception {
		Method m = StateNetServerSelectSDL.class.getDeclaredMethod(
				"isListJumpKey", NullpoMinoSDL.KeyEvent.class);
		m.setAccessible(true);
		return (boolean) m.invoke(null, ev);
	}
}
