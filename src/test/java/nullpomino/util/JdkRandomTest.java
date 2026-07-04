package nullpomino.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Random;

import org.junit.jupiter.api.Test;

class JdkRandomTest {

	@Test
	void matchesJdkForNonPowerOfTwoBound() {
		Random reference = new Random(12345L);
		JdkRandom jdkRandom = new JdkRandom(12345L);

		for (int i = 0; i < 1000; i++) {
			assertEquals(reference.nextInt(7), jdkRandom.nextInt(7));
		}
	}

	@Test
	void matchesJdkForPowerOfTwoBound() {
		Random reference = new Random(98765L);
		JdkRandom jdkRandom = new JdkRandom(98765L);

		for (int i = 0; i < 1000; i++) {
			assertEquals(reference.nextInt(8), jdkRandom.nextInt(8));
		}
	}

	@Test
	void matchesKnownJdkReferenceValues() {
		// Independently verified against java.util.Random(0).nextInt(7), a
		// widely-published JDK reference sequence.
		JdkRandom jdkRandom = new JdkRandom(0L);
		int[] expected = {5, 2, 4, 2, 4, 0, 2, 1};
		for (int value : expected) {
			assertEquals(value, jdkRandom.nextInt(7));
		}
	}

	@Test
	void rejectsNonPositiveBound() {
		JdkRandom jdkRandom = new JdkRandom(1L);
		assertThrows(IllegalArgumentException.class, () -> jdkRandom.nextInt(0));
		assertThrows(IllegalArgumentException.class, () -> jdkRandom.nextInt(-1));
	}
}
