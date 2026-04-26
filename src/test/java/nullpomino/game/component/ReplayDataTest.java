package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

class ReplayDataTest {

	@Test
	void copyConstructorCopiesInputDataWithoutSharing() {
		ReplayData original = new ReplayData();
		original.setInputData(Controller.BUTTON_BIT_A, 0);
		original.setInputData(Controller.BUTTON_BIT_B, 1);

		ReplayData copy = new ReplayData(original);
		original.setInputData(Controller.BUTTON_BIT_C, 0);

		assertEquals(Controller.BUTTON_BIT_A, copy.getInputData(0));
		assertEquals(Controller.BUTTON_BIT_B, copy.getInputData(1));
		assertEquals(Controller.BUTTON_BIT_C, original.getInputData(0));
	}

	@Test
	void setInputDataAppendsWhenFrameIsOutOfRange() {
		ReplayData replay = new ReplayData();

		replay.setInputData(Controller.BUTTON_BIT_A, -1);
		replay.setInputData(Controller.BUTTON_BIT_B, 99);

		assertEquals(Controller.BUTTON_BIT_A, replay.getInputData(0));
		assertEquals(Controller.BUTTON_BIT_B, replay.getInputData(1));
		assertEquals(0, replay.getInputData(-1));
		assertEquals(0, replay.getInputData(99));
	}

	@Test
	void writeReadPropertyStoresOnlyInputTransitions() {
		ReplayData replay = new ReplayData();
		replay.setInputData(Controller.BUTTON_BIT_A, 0);
		replay.setInputData(Controller.BUTTON_BIT_A, 1);
		replay.setInputData(Controller.BUTTON_BIT_B, 2);
		CustomProperties props = new CustomProperties();

		replay.writeProperty(props, 0, -1);

		assertEquals(3, props.getProperty("0.r.max", -1));
		assertEquals(Controller.BUTTON_BIT_A, props.getProperty("0.r.0", -1));
		assertEquals(-1, props.getProperty("0.r.1", -1));
		assertEquals(Controller.BUTTON_BIT_B, props.getProperty("0.r.2", -1));

		ReplayData imported = new ReplayData();
		imported.readProperty(props, 0);
		assertEquals(Controller.BUTTON_BIT_A, imported.getInputData(0));
		assertEquals(Controller.BUTTON_BIT_A, imported.getInputData(1));
		assertEquals(Controller.BUTTON_BIT_B, imported.getInputData(2));
	}

	@Test
	void writePropertyCanLimitMaxFrame() {
		ReplayData replay = new ReplayData();
		replay.setInputData(Controller.BUTTON_BIT_A, 0);
		replay.setInputData(Controller.BUTTON_BIT_B, 1);
		CustomProperties props = new CustomProperties();

		replay.writeProperty(props, 0, 1);

		assertEquals(1, props.getProperty("0.r.max", -1));
		assertEquals(Controller.BUTTON_BIT_A, props.getProperty("0.r.0", -1));
		assertEquals(-1, props.getProperty("0.r.1", -1));
	}
}
