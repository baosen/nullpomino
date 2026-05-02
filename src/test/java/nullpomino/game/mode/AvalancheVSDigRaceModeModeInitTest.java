package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheVSDigRaceMode}'s subclass-specific
 * {@code modeInit} array allocation. The DigRace flavor adds one
 * per-player array on top of the AvalancheVS shared arrays:
 * {@code handicapRows} (int[]) sized to MAX_PLAYERS=2. The parent's
 * super.modeInit must run first so the AvalancheVS shared arrays
 * (ojama, score, feverMapSet) coexist alongside the DigRace-specific
 * handicapRows array.
 */
class AvalancheVSDigRaceModeModeInitTest {

	@Test
	void modeInitAllocatesHandicapRowsAtMaxPlayers() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		int[] handicapRows = (int[]) read(mode, "handicapRows");
		assertNotNull(handicapRows,
				"handicapRows must be allocated by modeInit");
		assertEquals(2, handicapRows.length,
				"handicapRows is sized to MAX_PLAYERS=2");
	}

	@Test
	void modeInitParentArraysAreAlsoAllocated() throws Exception {
		// super.modeInit delegates to the AvalancheVS allocator first
		// so the shared family arrays exist alongside the DigRace one.
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertNotNull(read(mode, "ojama"),
				"parent ojama-counter array must be allocated");
		assertNotNull(read(mode, "score"),
				"parent score array must be allocated");
		assertNotNull(read(mode, "feverMapSet"),
				"parent feverMapSet array must be allocated");
	}

	@Test
	void handicapRowsIsZeroSeededByJvmDefault() throws Exception {
		// modeInit allocates a fresh int[] each call -> JVM zero-init
		// means every slot starts at 0 (DigRace explicitly overrides
		// to 6 only via loadOtherSetting on playerInit).
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		int[] handicapRows = (int[]) read(mode, "handicapRows");
		for(int v : handicapRows) {
			assertEquals(0, v,
					"fresh modeInit -> handicapRows slots zero-init by JVM");
		}
	}

	@Test
	void modeInitIsIdempotentReplacingTheArrayOnEachCall() throws Exception {
		// Calling modeInit twice replaces the array — pre-spoiled values
		// from the first allocation must not leak into the second call.
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);
		int[] firstAllocation = (int[]) read(mode, "handicapRows");
		firstAllocation[0] = 99;
		firstAllocation[1] = 99;

		mode.modeInit(manager);
		int[] secondAllocation = (int[]) read(mode, "handicapRows");

		assertEquals(0, secondAllocation[0],
				"second modeInit replaces the array, not mutates it");
		assertEquals(0, secondAllocation[1]);
	}

	private static Object read(AvalancheVSDigRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
