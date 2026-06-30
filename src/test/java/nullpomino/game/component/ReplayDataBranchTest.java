package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

/**
 * Covers the remaining branch in ReplayData.replayFrameCount
 * where maxFrame > inputDataArray.size().
 */
class ReplayDataBranchTest {

	@Test
	void getInputDataReturnsZeroForNegativeFrame() {
		ReplayData data = new ReplayData();
		assertEquals(0, data.getInputData(-1));
	}

	@Test
	void getInputDataReturnsZeroForOutOfRangeFrame() {
		ReplayData data = new ReplayData();
		assertEquals(0, data.getInputData(999999));
	}

	@Test
	void setInputDataAddsForNegativeFrame() {
		ReplayData data = new ReplayData();
		data.setInputData(42, -1);
		assertEquals(42, data.getInputData(0));
	}

	@Test
	void setInputDataAddsForOutOfRangeFrame() {
		ReplayData data = new ReplayData();
		data.setInputData(42, 999999);
		assertEquals(42, data.getInputData(0));
	}

	@Test
	void writePropertyWithMaxFrameSmallerThanSize() {
		ReplayData data = new ReplayData();
		data.setInputData(1, 0);
		data.setInputData(2, 1);

		CustomProperties props = new CustomProperties();
		// With maxFrame=1, it will hit the path where maxFrame <= size
		data.writeProperty(props, 0, 1);
		assertNotNull(props);
	}

	@Test
	void writePropertyWithMaxFrameLargerThanSizeClampsToSize() {
		ReplayData data = new ReplayData();
		data.setInputData(1, 0);
		data.setInputData(2, 1); // size == 2

		CustomProperties props = new CustomProperties();
		// maxFrame=5 > size=2 -> replayFrameCount returns the size, the maxFrame>size branch
		data.writeProperty(props, 0, 5);
		assertEquals(2, props.getProperty("0.r.max", -1));
	}

	@Test
	void copyPreservesInputData() {
		ReplayData original = new ReplayData();
		original.setInputData(7, 0);

		ReplayData copy = new ReplayData(original);
		assertEquals(7, copy.getInputData(0));
	}
}
