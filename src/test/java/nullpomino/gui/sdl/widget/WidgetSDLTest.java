package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Pins WidgetSDL's default behaviours that every widget subclass
 * inherits but rarely overrides: containsPoint geometry semantics,
 * update returning false by default, handleKey / handleTextInput
 * being silent no-ops, and setFocused storing the flag.
 */
class WidgetSDLTest {

	private static class TestableWidget extends WidgetSDL {
		TestableWidget() { super(); }
		TestableWidget(int x, int y, int w, int h) { super(x, y, w, h); }
		@Override public void render() { /* no-op for tests */ }
	}

	@Test
	void noArgConstructorLeavesGeometryAtZeroAndDefaultFlags() {
		TestableWidget w = new TestableWidget();

		assertEquals(0, w.x);
		assertEquals(0, w.y);
		assertEquals(0, w.w);
		assertEquals(0, w.h);
		assertTrue(w.visible, "visible defaults to true");
		assertTrue(w.enabled, "enabled defaults to true");
		assertFalse(w.focused, "focused defaults to false");
	}

	@Test
	void geometryConstructorStoresCoordinates() {
		TestableWidget w = new TestableWidget(10, 20, 100, 30);

		assertEquals(10, w.x);
		assertEquals(20, w.y);
		assertEquals(100, w.w);
		assertEquals(30, w.h);
	}

	@Test
	void containsPointTreatsTopLeftAsInclusiveAndBottomRightAsExclusive() {
		TestableWidget w = new TestableWidget(10, 20, 100, 30);

		assertTrue(w.containsPoint(10, 20), "exact top-left included");
		assertTrue(w.containsPoint(50, 30), "interior point included");
		assertTrue(w.containsPoint(109, 49), "last interior pixel included");

		assertFalse(w.containsPoint(110, 20), "right edge excluded");
		assertFalse(w.containsPoint(50, 50), "bottom edge excluded");
		assertFalse(w.containsPoint(9, 20), "left of widget excluded");
		assertFalse(w.containsPoint(50, 19), "above widget excluded");
	}

	@Test
	void defaultUpdateReturnsFalseRegardlessOfArgs() {
		TestableWidget w = new TestableWidget(0, 0, 100, 30);

		assertFalse(w.update(50, 15, true));
		assertFalse(w.update(0, 0, false));
		assertFalse(w.update(-1, -1, true));
	}

	@Test
	void defaultHandleKeyAndHandleTextInputDoNotThrowOrChangeState() {
		TestableWidget w = new TestableWidget(0, 0, 100, 30);

		w.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		w.handleTextInput("hello");

		// Sanity: state unchanged
		assertFalse(w.focused);
	}

	@Test
	void setFocusedFlipsTheFocusFlag() {
		TestableWidget w = new TestableWidget(0, 0, 100, 30);

		w.setFocused(true);
		assertTrue(w.focused);

		w.setFocused(false);
		assertFalse(w.focused);
	}
}
