package nullpomino.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MouseInputDummyTest {

	private static final class TestMouse extends MouseInputDummy {
		TestMouse() {
			super();
		}

		void setPosition(int x, int y) {
			this.mouseX = x;
			this.mouseY = y;
		}

		void setPrevPosition(int x, int y) {
			this.prevMouseX = x;
			this.prevMouseY = y;
		}

		void setLeftHold(int frames) {
			mousePressed[0] = frames;
		}

		void setMiddleHold(int frames) {
			mousePressed[1] = frames;
		}

		void setRightHold(int frames) {
			mousePressed[2] = frames;
		}

		void setBackHold(int frames) {
			mouseBackPressed = frames;
		}

		void setForwardHold(int frames) {
			mouseForwardPressed = frames;
		}
	}

	@Test
	void defaultConstructorAllocatesPressedArrayAndSentinelPrevPosition() {
		TestMouse m = new TestMouse();

		assertEquals(0, m.getMouseX());
		assertEquals(0, m.getMouseY());
		assertEquals(0, m.getLeftHoldFrames());
		assertFalse(m.isMousePressed());
		assertTrue(m.isMouseMoved(),
				"prev sentinel (-1,-1) should differ from default (0,0) coordinates");
	}

	@Test
	void isMouseMovedComparesAgainstPreviousCoordinates() {
		TestMouse m = new TestMouse();
		m.setPosition(5, 7);
		m.setPrevPosition(5, 7);
		assertFalse(m.isMouseMoved());

		m.setPosition(5, 8);
		assertTrue(m.isMouseMoved());

		m.setPosition(6, 7);
		assertTrue(m.isMouseMoved());
	}

	@Test
	void singleFrameClickHelpersOnlyFireWhenStateEqualsOne() {
		TestMouse m = new TestMouse();

		m.setLeftHold(1);
		assertTrue(m.isMouseClicked());
		m.setLeftHold(2);
		assertFalse(m.isMouseClicked());

		m.setMiddleHold(1);
		assertTrue(m.isMouseMiddleClicked());
		m.setMiddleHold(0);
		assertFalse(m.isMouseMiddleClicked());

		m.setRightHold(1);
		assertTrue(m.isMouseRightClicked());
		m.setRightHold(2);
		assertFalse(m.isMouseRightClicked());

		m.setBackHold(1);
		assertTrue(m.isMouseBackClicked());
		m.setBackHold(2);
		assertFalse(m.isMouseBackClicked());

		m.setForwardHold(1);
		assertTrue(m.isMouseForwardClicked());
		m.setForwardHold(0);
		assertFalse(m.isMouseForwardClicked());
	}

	@Test
	void heldHelpersTrueForAnyPositiveFrameCount() {
		TestMouse m = new TestMouse();

		m.setLeftHold(5);
		m.setMiddleHold(3);
		m.setRightHold(2);

		assertTrue(m.isMousePressed());
		assertTrue(m.isMouseMiddlePressed());
		assertTrue(m.isMouseRightPressed());
		assertEquals(5, m.getLeftHoldFrames());
	}

	@Test
	void menuRepeatHelpersFireOnEverySecondHoldFrameAfterDelay() {
		TestMouse m = new TestMouse();

		m.setLeftHold(28);
		assertTrue(m.isMenuRepeatLeft());
		m.setLeftHold(29);
		assertFalse(m.isMenuRepeatLeft());
		m.setLeftHold(27);
		assertFalse(m.isMenuRepeatLeft(), "frame 27 is below the >27 threshold");

		m.setMiddleHold(32);
		assertTrue(m.isMenuRepeatMiddle());
		m.setMiddleHold(31);
		assertFalse(m.isMenuRepeatMiddle());

		m.setRightHold(36);
		assertTrue(m.isMenuRepeatRight());
		m.setRightHold(33);
		assertFalse(m.isMenuRepeatRight());
	}
}
