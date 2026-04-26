package nullpomino.gui;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins the edge-detector contract on the mouse forward (X2) button.
 *
 * <p>The frontend polls the SDL button mask each frame and increments a
 * hold counter; {@code isMouseForwardClicked()} fires only on the 0→1
 * transition (the first frame of a press). The same pattern guards the
 * back button. Without this, holding the button would re-fire forward
 * navigation every frame and burn through the back stack.
 */
class MouseInputForwardTest {

	@Test
	void forwardClickFiresOnlyOnFirstFrameOfPress() {
		FakeMouse mouse = new FakeMouse();

		mouse.setForward(0);
		assertFalse(mouse.isMouseForwardClicked(), "released → no click");

		mouse.setForward(1);
		assertTrue(mouse.isMouseForwardClicked(), "first frame held → click");

		mouse.setForward(2);
		assertFalse(mouse.isMouseForwardClicked(), "still held → no repeat click");

		mouse.setForward(60);
		assertFalse(mouse.isMouseForwardClicked(), "long-held → still no repeat");
	}

	@Test
	void forwardAndBackEdgesAreIndependent() {
		FakeMouse mouse = new FakeMouse();

		mouse.setForward(1);
		mouse.setBack(0);
		assertTrue(mouse.isMouseForwardClicked());
		assertFalse(mouse.isMouseBackClicked());

		mouse.setForward(0);
		mouse.setBack(1);
		assertFalse(mouse.isMouseForwardClicked());
		assertTrue(mouse.isMouseBackClicked());
	}

	/** Test-only stub — exposes the protected hold counters. */
	private static final class FakeMouse extends MouseInputDummy {
		void setForward(int frames) { mouseForwardPressed = frames; }
		void setBack(int frames) { mouseBackPressed = frames; }
	}
}
