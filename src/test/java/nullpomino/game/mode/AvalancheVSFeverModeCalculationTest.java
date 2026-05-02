package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins the chain calculation, chain colour display logic, and
 * constants on {@link AvalancheVSFeverMode}.
 *
 * <p>{@code calcChainNewPower} looks up the 24-element
 * {@code FEVER_POWERS} table (first=4, last=720) and clamps
 * values above chain 24 to the final entry; {@code getChainColor}
 * returns a colour based on the current chain relative to the
 * fever target when {@code CHAIN_DISPLAY_FEVERSIZE} is active,
 * and delegates to the parent class otherwise.
 */
class AvalancheVSFeverModeCalculationTest {

	// =====================================================
	// FEVER_POWERS constant (private, accessed via reflection)
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

	@Test
	void feverPowersAllValuesMatchExpected() throws Exception {
		assertArrayEquals(new int[] {
				 4,  10,  18,  21,  29,  46,  76, 113,
				150, 223, 259, 266, 313, 364, 398, 432,
				468, 504, 540, 576, 612, 648, 684, 720
		}, getFeverPowers());
	}

	// =====================================================
	// CHAIN_DISPLAY_FEVERSIZE constant
	// =====================================================

	@Test
	void chainDisplayFeverSizeConstantIsFour() {
		assertEquals(4, AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE);
	}

	// =====================================================
	// calcChainNewPower
	// =====================================================

	@Test
	void calcChainNewPowerChain1Returns4() {
		TestableVSFeverMode mode = new TestableVSFeverMode();
		assertEquals(4, mode.calcChainNewPower(1));
	}

	@Test
	void calcChainNewPowerChain2Returns10() {
		TestableVSFeverMode mode = new TestableVSFeverMode();
		assertEquals(10, mode.calcChainNewPower(2));
	}

	@Test
	void calcChainNewPowerChain23Returns684() {
		TestableVSFeverMode mode = new TestableVSFeverMode();
		assertEquals(684, mode.calcChainNewPower(23));
	}

	@Test
	void calcChainNewPowerChain24Returns720() {
		TestableVSFeverMode mode = new TestableVSFeverMode();
		// Last element — still in range via chain-1 index
		assertEquals(720, mode.calcChainNewPower(24));
	}

	@Test
	void calcChainNewPowerChain25ClampsToLastElement720() {
		TestableVSFeverMode mode = new TestableVSFeverMode();
		// Above FEVER_POWERS.length → FEVER_POWERS[FEVER_POWERS.length-1]
		assertEquals(720, mode.calcChainNewPower(25));
	}

	@Test
	void calcChainNewPowerLargeChainClampsTo720() {
		TestableVSFeverMode mode = new TestableVSFeverMode();
		assertEquals(720, mode.calcChainNewPower(100));
		assertEquals(720, mode.calcChainNewPower(1000));
	}

	@Test
	void calcChainNewPowerChain0ThrowsArrayIndexOutOfBounds() {
		TestableVSFeverMode mode = new TestableVSFeverMode();
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> mode.calcChainNewPower(0));
	}

	@Test
	void calcChainNewPowerNegativeChainThrowsArrayIndexOutOfBounds() {
		TestableVSFeverMode mode = new TestableVSFeverMode();
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> mode.calcChainNewPower(-1));
	}

	// =====================================================
	// getChainColor — Fever-display path
	// =====================================================

	@Test
	void getChainColorFeverPathChainAboveTargetReturnsGreen() {
		TestableVSFeverMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE;
		setFeverChainDisplay(mode, 0, 5);
		engine.chain = 7;   // 7 >= 5
		assertEquals(EventReceiver.COLOR_GREEN, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorFeverPathChainEqualToTargetReturnsGreen() {
		TestableVSFeverMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE;
		setFeverChainDisplay(mode, 0, 5);
		engine.chain = 5;   // 5 >= 5
		assertEquals(EventReceiver.COLOR_GREEN, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorFeverPathChainTwoBelowTargetReturnsOrange() {
		TestableVSFeverMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE;
		setFeverChainDisplay(mode, 0, 5);
		engine.chain = 3;   // 3 == 5-2
		assertEquals(EventReceiver.COLOR_ORANGE, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorFeverPathChainMoreThanTwoBelowTargetReturnsRed() {
		TestableVSFeverMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE;
		setFeverChainDisplay(mode, 0, 5);
		engine.chain = 1;   // 1 < 3
		assertEquals(EventReceiver.COLOR_RED, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorFeverPathChainExactlyOneBelowTargetReturnsYellow() {
		TestableVSFeverMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE;
		setFeverChainDisplay(mode, 0, 5);
		engine.chain = 4;   // not >=5, not ==3, not <3 → YELLOW
		assertEquals(EventReceiver.COLOR_YELLOW, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorFeverPathWithZeroTargetAndZeroChainReturnsGreen() {
		TestableVSFeverMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE;
		// feverChainDisplay[0] defaults to 0 after modeInit
		engine.chain = 0;   // 0 >= 0
		assertEquals(EventReceiver.COLOR_GREEN, mode.getChainColor(engine, 0));
	}

	// =====================================================
	// getChainColor — delegation to parent
	// =====================================================

	@Test
	void getChainColorYellowDisplayReturnsYellow() {
		TestableVSFeverMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSDummyMode.CHAIN_DISPLAY_YELLOW;
		assertEquals(EventReceiver.COLOR_YELLOW, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorSizeDisplayUsesParentLogic() {
		TestableVSFeverMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSDummyMode.CHAIN_DISPLAY_SIZE;
		mode.rensaShibari[0] = 5;
		engine.chain = 3;   // 3 < 5 → RED
		assertEquals(EventReceiver.COLOR_RED, mode.getChainColor(engine, 0));
	}

	@Test
	void getChainColorPlayerDisplayUsesParentLogic() {
		TestableVSFeverMode mode = initMode();
		GameEngine engine = createEngine();
		mode.chainDisplayType[0] = AvalancheVSDummyMode.CHAIN_DISPLAY_PLAYER;
		// playerID=0 → RED per parent logic
		assertEquals(EventReceiver.COLOR_RED, mode.getChainColor(engine, 0));
	}

	// =====================================================
	// Helpers
	// =====================================================

	private static int[] getFeverPowers() throws Exception {
		Field f = findField(AvalancheVSFeverMode.class, "FEVER_POWERS");
		f.setAccessible(true);
		return (int[]) f.get(null);
	}

	private static void setFeverChainDisplay(AvalancheVSFeverMode mode,
			int playerID, int value) {
		try {
			Field f = findField(AvalancheVSFeverMode.class, "feverChainDisplay");
			f.setAccessible(true);
			int[] arr = (int[]) f.get(mode);
			arr[playerID] = value;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Field findField(Class<?> cls, String name)
			throws NoSuchFieldException {
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

	private static TestableVSFeverMode initMode() {
		TestableVSFeverMode mode = new TestableVSFeverMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		return mode;
	}

	private static GameEngine createEngine() {
		return new GameEngine(new GameManager(new EventReceiver()), 0);
	}

	// =====================================================
	// Concrete test subclass
	// =====================================================

	private static class TestableVSFeverMode extends AvalancheVSFeverMode {
		@Override
		public String getName() {
			return "TEST_FEVER";
		}

		/** Convenience: engine and playerID are unused by calcChainNewPower. */
		public int calcChainNewPower(int chain) {
			return calcChainNewPower(null, 0, chain);
		}
	}
}
