package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;

import org.junit.jupiter.api.Test;

/**
 * Pins constants and chain calculation on {@link AvalancheVSMode}.
 *
 * <p>{@code FEVER_POWERS} is a 24-element table (first=4, last=720);
 * {@code FEVER_METER_COLORS} is a 9-element colour table;
 * {@code calcChainNewPower} selects between {@code CHAIN_POWERS}
 * (16 elements) and {@code FEVER_POWERS} (24 elements) based on
 * the {@code inFever[playerID]} flag.
 */
class AvalancheVSModeCalculationTest {

	// =====================================================
	// FEVER_POWERS constant
	// =====================================================

	@Test
	void feverPowersHas24Elements() throws Exception {
		assertEquals(24, getFeverPowers().length);
	}

	@Test
	void feverPowersFirstElementIs4() throws Exception {
		assertEquals(4, getFeverPowers()[0]);
	}

	@Test
	void feverPowersLastElementIs720() throws Exception {
		assertEquals(720, getFeverPowers()[23]);
	}

	// =====================================================
	// FEVER_METER_COLORS constant
	// =====================================================

	@Test
	void feverMeterColorsHas9Elements() throws Exception {
		assertEquals(9, getFeverMeterColors().length);
	}

	@Test
	void feverMeterColorsFirstElementIsColorRed() throws Exception {
		assertEquals(EventReceiver.COLOR_RED, getFeverMeterColors()[0]);
	}

	@Test
	void feverMeterColorsLastElementIsColorPink() throws Exception {
		assertEquals(EventReceiver.COLOR_PINK, getFeverMeterColors()[8]);
	}

	// =====================================================
	// FEVER_POINT_CRITERIA_NAMES constant
	// =====================================================

	@Test
	void feverPointCriteriaNames() throws Exception {
		assertArrayEquals(
				new String[] {"COUNTER", "CLEAR", "BOTH"},
				getFeverPointCriteriaNames());
	}

	// =====================================================
	// FEVER_TIME_CRITERIA_NAMES constant
	// =====================================================

	@Test
	void feverTimeCriteriaNames() throws Exception {
		assertArrayEquals(
				new String[] {"COUNTER", "ATTACK"},
				getFeverTimeCriteriaNames());
	}

	// =====================================================
	// FEVER_POINT_CRITERIA_* constants
	// =====================================================

	@Test
	void feverPointCriteriaCounterIs0() throws Exception {
		assertEquals(0, getIntConstant("FEVER_POINT_CRITERIA_COUNTER"));
	}

	@Test
	void feverPointCriteriaClearIs1() throws Exception {
		assertEquals(1, getIntConstant("FEVER_POINT_CRITERIA_CLEAR"));
	}

	// =====================================================
	// FEVER_TIME_CRITERIA_* constants
	// =====================================================

	@Test
	void feverTimeCriteriaCounterIs0() throws Exception {
		assertEquals(0, getIntConstant("FEVER_TIME_CRITERIA_COUNTER"));
	}

	@Test
	void feverTimeCriteriaAttackIs1() throws Exception {
		assertEquals(1, getIntConstant("FEVER_TIME_CRITERIA_ATTACK"));
	}

	// =====================================================
	// calcChainNewPower — not in Fever (uses CHAIN_POWERS)
	// =====================================================

	@Test
	void calcChainNewPowerNotInFeverChain1Returns4() {
		TestableVSMode mode = createMode();
		assertEquals(4, mode.calcChainNewPower(1));
	}

	@Test
	void calcChainNewPowerNotInFeverChain2Returns12() {
		TestableVSMode mode = createMode();
		assertEquals(12, mode.calcChainNewPower(2));
	}

	@Test
	void calcChainNewPowerNotInFeverChain15Returns990() {
		TestableVSMode mode = createMode();
		assertEquals(990, mode.calcChainNewPower(15));
	}

	@Test
	void calcChainNewPowerNotInFeverChain16Returns999() {
		// CHAIN_POWERS has 16 elements; index 15 is 999 (last)
		TestableVSMode mode = createMode();
		assertEquals(999, mode.calcChainNewPower(16));
	}

	@Test
	void calcChainNewPowerNotInFeverChainAboveLengthClampsTo999() {
		TestableVSMode mode = createMode();
		assertEquals(999, mode.calcChainNewPower(20));
		assertEquals(999, mode.calcChainNewPower(100));
	}

	// =====================================================
	// calcChainNewPower — in Fever (uses FEVER_POWERS)
	// =====================================================

	@Test
	void calcChainNewPowerInFeverChain1Returns4() throws Exception {
		TestableVSMode mode = createMode();
		setInFever(mode, 0, true);
		assertEquals(4, mode.calcChainNewPower(1));
	}

	@Test
	void calcChainNewPowerInFeverChain2Returns10() throws Exception {
		TestableVSMode mode = createMode();
		setInFever(mode, 0, true);
		assertEquals(10, mode.calcChainNewPower(2));
	}

	@Test
	void calcChainNewPowerInFeverChain23Returns684() throws Exception {
		TestableVSMode mode = createMode();
		setInFever(mode, 0, true);
		assertEquals(684, mode.calcChainNewPower(23));
	}

	@Test
	void calcChainNewPowerInFeverChain24Returns720() throws Exception {
		TestableVSMode mode = createMode();
		setInFever(mode, 0, true);
		assertEquals(720, mode.calcChainNewPower(24));
	}

	@Test
	void calcChainNewPowerInFeverChainAbove24ClampsTo720() throws Exception {
		TestableVSMode mode = createMode();
		setInFever(mode, 0, true);
		assertEquals(720, mode.calcChainNewPower(25));
		assertEquals(720, mode.calcChainNewPower(100));
	}

	// =====================================================
	// calcChainNewPower — edge cases
	// =====================================================

	@Test
	void calcChainNewPowerChain0Throws() {
		TestableVSMode mode = createMode();
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> mode.calcChainNewPower(0));
	}

	@Test
	void calcChainNewPowerNegativeChainThrows() {
		TestableVSMode mode = createMode();
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> mode.calcChainNewPower(-1));
	}

	// =====================================================
	// Helpers
	// =====================================================

	private static int[] getFeverPowers() throws Exception {
		Field f = findField(AvalancheVSMode.class, "FEVER_POWERS");
		f.setAccessible(true);
		return (int[]) f.get(null);
	}

	private static int[] getFeverMeterColors() throws Exception {
		Field f = findField(AvalancheVSMode.class, "FEVER_METER_COLORS");
		f.setAccessible(true);
		return (int[]) f.get(null);
	}

	private static String[] getFeverPointCriteriaNames() throws Exception {
		Field f = findField(AvalancheVSMode.class, "FEVER_POINT_CRITERIA_NAMES");
		f.setAccessible(true);
		return (String[]) f.get(null);
	}

	private static String[] getFeverTimeCriteriaNames() throws Exception {
		Field f = findField(AvalancheVSMode.class, "FEVER_TIME_CRITERIA_NAMES");
		f.setAccessible(true);
		return (String[]) f.get(null);
	}

	private static int getIntConstant(String name) throws Exception {
		Field f = findField(AvalancheVSMode.class, name);
		f.setAccessible(true);
		return f.getInt(null);
	}

	private static void setInFever(AvalancheVSMode mode, int playerID, boolean value) throws Exception {
		Field f = findField(AvalancheVSMode.class, "inFever");
		f.setAccessible(true);
		boolean[] arr = (boolean[]) f.get(mode);
		arr[playerID] = value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name + " not found in hierarchy");
	}

	private static TestableVSMode createMode() {
		TestableVSMode mode = new TestableVSMode();
		// Initialize the inFever array (private field) so that
		// calcChainNewPower does not NPE when accessing inFever[playerID].
		try {
			Field f = findField(AvalancheVSMode.class, "inFever");
			f.setAccessible(true);
			f.set(mode, new boolean[2]); // MAX_PLAYERS = 2
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return mode;
	}

	// =====================================================
	// Concrete test subclass
	// =====================================================

	private static class TestableVSMode extends AvalancheVSMode {
		@Override
		public String getName() {
			return "TEST_VS";
		}

		/** Convenience: engine and playerID are unused by calcChainNewPower. */
		public int calcChainNewPower(int chain) {
			return calcChainNewPower(null, 0, chain);
		}
	}
}
