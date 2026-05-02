package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheVSSPFMode}'s subclass-specific arrays
 * allocated in {@code modeInit}. Eight SPF arrays are added on top of
 * the AvalancheVS shared arrays:
 * - ojamaCountdown / dropSet / dropMap (int[])
 * - dropPattern (int[][][] — slot per player, with the inner shape
 *   filled in by the load path)
 * - attackMultiplier / defendMultiplier (double[])
 * - countdownDecremented / ojamaChecked (boolean[])
 *
 * <p>All at MAX_PLAYERS=2 length.
 */
class AvalancheVSSPFModeModeInitTest {

	@Test
	void modeInitAllocatesAllSpfPerPlayerArraysAtMaxPlayers() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertEquals(2, ((int[]) read(mode, "ojamaCountdown")).length);
		assertEquals(2, ((int[]) read(mode, "dropSet")).length);
		assertEquals(2, ((int[]) read(mode, "dropMap")).length);
		assertEquals(2, ((int[][][]) read(mode, "dropPattern")).length,
				"dropPattern is int[2][][] — outer slot per player, "
						+ "inner shape filled by the load path");
		assertEquals(2, ((double[]) read(mode, "attackMultiplier")).length);
		assertEquals(2, ((double[]) read(mode, "defendMultiplier")).length);
		assertEquals(2, ((boolean[]) read(mode, "countdownDecremented")).length);
		assertEquals(2, ((boolean[]) read(mode, "ojamaChecked")).length);
	}

	@Test
	void modeInitDropPatternHasNullInnerSlotsBeforeLoadFills() throws Exception {
		// dropPattern is allocated but each slot is null — the inner
		// shape is filled in by loadOtherSetting / startGame.
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		int[][][] dropPattern = (int[][][]) read(mode, "dropPattern");
		for(int[][] slot : dropPattern) {
			assertNull(slot,
					"each player's dropPattern slot starts null; "
							+ "filled in later by load/startGame");
		}
	}

	@Test
	void modeInitParentArraysAreAlsoAllocatedForTheSpfSubclass() throws Exception {
		// super.modeInit must run first so the AvalancheVS shared
		// arrays exist alongside the SPF-specific ones.
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertNotNull(read(mode, "ojama"));
		assertNotNull(read(mode, "score"));
		assertNotNull(read(mode, "feverMapSet"));
	}

	@Test
	void spfPerPlayerArraysAreZeroSeededByJvmDefault() throws Exception {
		AvalancheVSSPFMode mode = new AvalancheVSSPFMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		int[] ojamaCountdown = (int[]) read(mode, "ojamaCountdown");
		double[] attackMultiplier = (double[]) read(mode, "attackMultiplier");
		boolean[] ojamaChecked = (boolean[]) read(mode, "ojamaChecked");
		for(int v : ojamaCountdown) assertEquals(0, v);
		for(double v : attackMultiplier) assertEquals(0d, v);
		for(boolean v : ojamaChecked) assertEquals(false, v);
	}

	private static Object read(AvalancheVSSPFMode mode, String name) throws Exception {
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
