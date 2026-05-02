package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheVSBombBattleMode}'s subclass-specific arrays
 * allocated in {@code modeInit}. Two Bomb-Battle arrays are added on
 * top of the AvalancheVS shared arrays:
 * {@code ojamaCountdown} (per-player countdown timer for ojama
 * blocks) and {@code newChainPower} (per-player flag for the new
 * chain-power table). Both at MAX_PLAYERS=2 length.
 */
class AvalancheVSBombBattleModeModeInitTest {

	@Test
	void modeInitAllocatesBombBattlePerPlayerArraysAtMaxPlayers() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertEquals(2, ((int[]) read(mode, "ojamaCountdown")).length,
				"ojamaCountdown sized to MAX_PLAYERS=2");
		assertEquals(2, ((boolean[]) read(mode, "newChainPower")).length,
				"newChainPower sized to MAX_PLAYERS=2");
	}

	@Test
	void modeInitParentArraysAreAlsoAllocatedForTheBombBattleSubclass() throws Exception {
		// super.modeInit must run first so the AvalancheVS shared
		// arrays exist alongside the BombBattle-specific ones.
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertNotNull(read(mode, "ojama"));
		assertNotNull(read(mode, "score"));
		assertNotNull(read(mode, "feverMapSet"));
	}

	@Test
	void bombBattleArraysAreZeroSeededByJvmDefault() throws Exception {
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		int[] ojamaCountdown = (int[]) read(mode, "ojamaCountdown");
		boolean[] newChainPower = (boolean[]) read(mode, "newChainPower");
		for(int v : ojamaCountdown) assertEquals(0, v);
		for(boolean v : newChainPower) assertEquals(false, v);
	}

	private static Object read(AvalancheVSBombBattleMode mode, String name) throws Exception {
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
