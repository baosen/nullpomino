package mu.nu.nullpo.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pins the CustomProperties overloads the codebase actually exercises.
 * Guards the dead-overload cleanup that removes byte / short / char /
 * double get/set pairs — grep turned up zero callers for those, but
 * if someone reaches for them in a future commit this test keeps the
 * kept surface (int/long/float/boolean/String) visible.
 */
class CustomPropertiesRoundTripTest {

	@Test
	void intRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("i", 42);
		assertEquals(42, p.getProperty("i", -1));
		assertEquals(-1, p.getProperty("missing", -1));
	}

	@Test
	void longRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("l", 1234567890123L);
		assertEquals(1234567890123L, p.getProperty("l", 0L));
	}

	@Test
	void floatRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("f", 6.5f);
		assertEquals(6.5f, p.getProperty("f", 0f));
	}

	@Test
	void doubleRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("d", 3.141592653589793);
		assertEquals(3.141592653589793, p.getProperty("d", 0.0));
	}

	@Test
	void booleanRoundTrip() {
		CustomProperties p = new CustomProperties();
		p.setProperty("b", true);
		assertEquals(true, p.getProperty("b", false));
		p.setProperty("b", false);
		assertEquals(false, p.getProperty("b", true));
	}

	@Test
	void stringFallsBackToDefaultOnMalformed() {
		CustomProperties p = new CustomProperties();
		p.setProperty("bad", "not-a-number");
		assertEquals(7, p.getProperty("bad", 7));
	}
}
