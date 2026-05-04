package nullpomino.game.randomizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Covers the remaining branch in ExpDistWeightRandomizer.isAtDistanceLimit.
 */
class ExpDistWeightRandomizerTest {

	@Test
	void isAtDistanceLimitReturnsFalseForSmallWeight() {
		ExpDistWeightRandomizer r = new ExpDistWeightRandomizer() {
			@Override
			public void init() {
				weights = new int[]{1};
				cumulative = new int[]{1};
				pieces = new int[]{0};
			}
		};
		r.init();
		r.weights = new int[]{10}; // not > 25
		assertEquals(1 << 9, r.getWeight(0));
	}

	@Test
	void getWeightReturnsZeroWhenWeightIsZero() {
		ExpDistWeightRandomizer r = new ExpDistWeightRandomizer() {
			@Override
			public void init() {
				weights = new int[]{0};
				cumulative = new int[]{0};
				pieces = new int[]{0};
			}
		};
		r.init();
		r.weights = new int[]{0};
		assertEquals(0, r.getWeight(0));
	}

	@Test
	void getWeightReturnsPowerOfTwoMinusOne() {
		ExpDistWeightRandomizer r = new ExpDistWeightRandomizer() {
			@Override
			public void init() {
				weights = new int[]{1};
				cumulative = new int[]{1};
				pieces = new int[]{0};
			}
		};
		r.init();
		r.weights = new int[]{5};
		assertEquals(16, r.getWeight(0));
	}
}
