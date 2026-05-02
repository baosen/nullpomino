package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Additional tests for WidgetSDL base class covering edge cases of
 * containsPoint geometry, setFocused, and no-op input handlers.
 */
class WidgetSDLExtendedTest {

	private static class TestableWidget extends WidgetSDL {
		TestableWidget() { super(); }
		TestableWidget(int x, int y, int w, int h) { super(x, y, w, h); }
		@Override public void render() { /* no-op for tests */ }
	}

	@Test
	void setFocusedFalseStoresFlag() {
		TestableWidget w = new TestableWidget(0, 0, 100, 30);
		w.setFocused(true);
		assertTrue(w.focused);
		w.setFocused(false);
		assertFalse(w.focused);
	}

	@Test
	void setFocusedTwiceKeepsFlag() {
		TestableWidget w = new TestableWidget(0, 0, 100, 30);
		w.setFocused(true);
		w.setFocused(true);
		assertTrue(w.focused);
	}

	@Test
	void defaultHandleKeyDoesNotThrowWhenDisabledOrUnfocused() {
		TestableWidget w = new TestableWidget(0, 0, 100, 30);
		w.enabled = false;
		w.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));

		w.enabled = true;
		w.focused = false;
		w.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_SPACE, 0, false));

		assertFalse(w.focused);
	}

	@Test
	void handleTextInputNullAndEmptyDoNotThrow() {
		TestableWidget w = new TestableWidget(0, 0, 100, 30);
		w.handleTextInput(null);
		w.handleTextInput("");
		assertFalse(w.focused);
	}

	@Test
	void visibleAndEnabledDefaultFlagsAreTrue() {
		TestableWidget w = new TestableWidget(10, 20, 100, 30);
		assertTrue(w.visible);
		assertTrue(w.enabled);
		assertFalse(w.focused);
	}

	@Test
	void containsPointNegativeCoordinates() {
		TestableWidget w = new TestableWidget(-50, -50, 100, 100);
		assertTrue(w.containsPoint(-50, -50));       // top-left inclusive
		assertTrue(w.containsPoint(0, 0));           // interior
		assertTrue(w.containsPoint(49, 49));         // last interior pixel
		assertFalse(w.containsPoint(-51, -50));      // left of x
		assertFalse(w.containsPoint(50, -50));       // right edge exclusive
		assertFalse(w.containsPoint(-50, 50));       // bottom edge exclusive
	}

	@Test
	void containsPointZeroSizedWidget() {
		TestableWidget w = new TestableWidget(0, 0, 0, 0);
		assertFalse(w.containsPoint(0, 0), "zero-sized widget contains no points");
		assertFalse(w.containsPoint(-1, -1));
	}

	@Test
	void containsPointLargeCoordinatesNoOverflow() {
		TestableWidget w = new TestableWidget(Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2,
				Integer.MAX_VALUE, Integer.MAX_VALUE);
		// The computation px >= x && px < x + w should not overflow
		assertTrue(w.containsPoint(0, 0));
	}

	@Test
	void containsPointOnePixelWidget() {
		TestableWidget w = new TestableWidget(5, 5, 1, 1);
		assertTrue(w.containsPoint(5, 5));
		assertFalse(w.containsPoint(6, 5));
		assertFalse(w.containsPoint(5, 6));
	}
}
