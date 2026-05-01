package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Pins the edge-detection contract for menu Page Up / Page Down keys.
 * Every SDL menu state calls {@link PageNavigationSDL#checkPageEvent}
 * once per frame; if the rising-edge logic regresses to "while held"
 * the menus would skip multiple cursor positions per keypress.
 */
class PageNavigationSDLTest {

	@BeforeEach
	void resetEdgeState() {
		// Allocate the shared key-state array (NullpoMinoSDL would normally
		// size it during SDL init) and clear PAGEUP/PAGEDOWN so the static
		// prev-pressed flags inside PageNavigationSDL get zeroed.
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		PageNavigationSDL.checkPageEvent();
	}

	@Test
	void checkPageEventReturnsZeroWhenNothingPressed() {
		assertEquals(0, PageNavigationSDL.checkPageEvent());
	}

	@Test
	void checkPageEventFiresMinusOneOnPageUpRisingEdge() {
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_PAGEUP] = true;

		assertEquals(-1, PageNavigationSDL.checkPageEvent());
	}

	@Test
	void checkPageEventFiresPlusOneOnPageDownRisingEdge() {
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_PAGEDOWN] = true;

		assertEquals(1, PageNavigationSDL.checkPageEvent());
	}

	@Test
	void checkPageEventReturnsZeroWhilePageKeyIsHeld() {
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_PAGEUP] = true;
		assertEquals(-1, PageNavigationSDL.checkPageEvent());
		assertEquals(0, PageNavigationSDL.checkPageEvent(),
				"holding the key should not retrigger the edge");
	}

	@Test
	void checkPageEventPrefersPageUpWhenBothEdgesHappenSameFrame() {
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_PAGEUP] = true;
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_PAGEDOWN] = true;

		assertEquals(-1, PageNavigationSDL.checkPageEvent());
	}

	@Test
	void jumpToEndReturnsCurrentWhenNoPageEvent() {
		assertEquals(5, PageNavigationSDL.jumpToEnd(0, 5, 0, 10));
	}

	@Test
	void jumpToEndReturnsCurrentWhenAlreadyAtMin() {
		assertEquals(0, PageNavigationSDL.jumpToEnd(-1, 0, 0, 10),
				"page-up while at min must short-circuit (no sound effect either)");
	}

	@Test
	void jumpToEndReturnsCurrentWhenAlreadyAtMax() {
		assertEquals(10, PageNavigationSDL.jumpToEnd(1, 10, 0, 10));
	}
}
