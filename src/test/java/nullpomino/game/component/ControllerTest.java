package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ControllerTest {

	@Test
	void buttonBitsRoundTripThroughPressFlags() {
		Controller controller = new Controller();
		int mask = Controller.BUTTON_BIT_UP | Controller.BUTTON_BIT_A | Controller.BUTTON_BIT_F;

		controller.setButtonBit(mask);

		assertTrue(controller.buttonPress[Controller.BUTTON_UP]);
		assertTrue(controller.buttonPress[Controller.BUTTON_A]);
		assertTrue(controller.buttonPress[Controller.BUTTON_F]);
		assertFalse(controller.buttonPress[Controller.BUTTON_DOWN]);
		assertEquals(mask, controller.getButtonBit());
	}

	@Test
	void updateButtonTimeIncrementsPressedAndClearsReleased() {
		Controller controller = new Controller();
		controller.buttonPress[Controller.BUTTON_A] = true;
		controller.updateButtonTime();
		controller.updateButtonTime();
		assertEquals(2, controller.buttonTime[Controller.BUTTON_A]);
		assertTrue(controller.isPress(Controller.BUTTON_A));
		assertFalse(controller.isPush(Controller.BUTTON_A));

		controller.buttonPress[Controller.BUTTON_A] = false;
		controller.updateButtonTime();

		assertEquals(0, controller.buttonTime[Controller.BUTTON_A]);
		assertFalse(controller.isPress(Controller.BUTTON_A));
	}

	@Test
	void copyConstructorCopiesArraysWithoutSharing() {
		Controller original = new Controller();
		original.buttonPress[Controller.BUTTON_LEFT] = true;
		original.buttonTime[Controller.BUTTON_LEFT] = 12;

		Controller copy = new Controller(original);

		assertArrayEquals(original.buttonPress, copy.buttonPress);
		assertArrayEquals(original.buttonTime, copy.buttonTime);
		original.buttonPress[Controller.BUTTON_LEFT] = false;
		original.buttonTime[Controller.BUTTON_LEFT] = 0;
		assertTrue(copy.buttonPress[Controller.BUTTON_LEFT]);
		assertEquals(12, copy.buttonTime[Controller.BUTTON_LEFT]);
	}

	@Test
	void clearButtonStateDoesNotClearButtonTimes() {
		Controller controller = new Controller();
		controller.buttonPress[Controller.BUTTON_B] = true;
		controller.buttonTime[Controller.BUTTON_B] = 3;

		controller.clearButtonState();

		assertFalse(controller.buttonPress[Controller.BUTTON_B]);
		assertEquals(3, controller.buttonTime[Controller.BUTTON_B]);
	}

	@Test
	void menuRepeatUsesInitialDelayedAndCButtonAcceleration() {
		Controller controller = new Controller();
		controller.buttonTime[Controller.BUTTON_DOWN] = 1;
		assertTrue(controller.isMenuRepeatKey(Controller.BUTTON_DOWN));

		controller.buttonTime[Controller.BUTTON_DOWN] = 25;
		assertFalse(controller.isMenuRepeatKey(Controller.BUTTON_DOWN));
		controller.buttonTime[Controller.BUTTON_DOWN] = 27;
		assertTrue(controller.isMenuRepeatKey(Controller.BUTTON_DOWN));

		controller.buttonTime[Controller.BUTTON_DOWN] = 2;
		controller.buttonTime[Controller.BUTTON_C] = 1;
		assertTrue(controller.isMenuRepeatKey(Controller.BUTTON_DOWN));
		assertFalse(controller.isMenuRepeatKey(Controller.BUTTON_DOWN, false));
	}
}
