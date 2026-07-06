package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Covers the rejection-sampling retry branch in
 * {@link JdkRandom#nextInt(int)}: with a bound just above 2^30, roughly
 * half of all raw 31-bit draws fall in the biased tail and force another
 * next(31) call. JdkRandom is final, so the retry is observed by
 * replicating java.util.Random's LCG and asserting the returned value
 * comes from the second raw draw, not the rejected first one.
 */
public class JdkRandomRejectionSamplingTest {
	private static final long MULTIPLIER = 0x5DEECE66DL;
	private static final long INCREMENT = 0xBL;
	private static final long MASK = (1L << 48) - 1;

	@Test
	public void retriesWhenDrawFallsInBiasedTail() {
		int bound = (1 << 30) + 1;
		int m = bound - 1;
		boolean verifiedRetry = false;

		for (long seed = 0; seed < 64 && !verifiedRetry; seed++) {
			long state = (seed ^ MULTIPLIER) & MASK;
			state = (state * MULTIPLIER + INCREMENT) & MASK;
			int first = (int) (state >>> 17);
			if (first - (first % bound) + m >= 0) continue; // first draw accepted

			// replicate the retry loop to find the accepted draw
			int expected;
			int draws = 1;
			int u = first;
			while (true) {
				int r = u % bound;
				if (u - r + m >= 0) { expected = r; break; }
				state = (state * MULTIPLIER + INCREMENT) & MASK;
				u = (int) (state >>> 17);
				draws++;
			}

			int actual = new JdkRandom(seed).nextInt(bound);
			assertEquals(expected, actual);
			assertNotEquals(first % bound, actual,
					"retry must discard the first, biased draw");
			assertTrue(draws >= 2);
			verifiedRetry = true;
		}
		assertTrue(verifiedRetry, "no seed in 0..63 hit the rejection tail");
	}
}
