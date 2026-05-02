package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheVSFeverMode}'s subclass-specific arrays
 * allocated in {@code modeInit}, plus the public {@code
 * CHAIN_DISPLAY_FEVERSIZE} constant.
 *
 * <p>{@code modeInit} delegates to the parent's allocator (so the
 * shared AvalancheVS arrays exist) and then allocates five Fever-
 * specific per-player arrays at MAX_PLAYERS=2 length:
 * {@code ojamaHandicapLeft, feverChain, ojamaHandicap,
 * feverChainDisplay, feverChainStart}. {@code CHAIN_DISPLAY_FEVERSIZE
 * = 4} is the chain-display-mode-id used by the SETTING menu.
 */
class AvalancheVSFeverModeModeInitTest {

	@Test
	void chainDisplayFeverSizeConstantIsFour() {
		// CHAIN_DISPLAY_FEVERSIZE is referenced by the chain-display
		// option enum on the SETTING menu — its position determines
		// which menu slot triggers the fever-size renderer.
		assertEquals(4, AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE);
	}

	@Test
	void modeInitAllocatesFeverPerPlayerArraysAtMaxPlayers() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		// Fever-specific per-player arrays are sized to MAX_PLAYERS=2
		// (the AVALANCHE-VS family is 1v1).
		assertEquals(2, ((int[]) read(mode, "ojamaHandicapLeft")).length);
		assertEquals(2, ((int[]) read(mode, "feverChain")).length);
		assertEquals(2, ((int[]) read(mode, "ojamaHandicap")).length);
		assertEquals(2, ((int[]) read(mode, "feverChainDisplay")).length);
		assertEquals(2, ((int[]) read(mode, "feverChainStart")).length);
	}

	@Test
	void modeInitParentArraysAreAlsoAllocatedForTheFeverSubclass() throws Exception {
		// super.modeInit delegates to the parent allocator first so
		// the AvalancheVS shared arrays exist alongside the Fever-
		// specific ones.
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		assertNotNull(read(mode, "ojama"),
				"parent ojama-counter array must be allocated");
		assertNotNull(read(mode, "ojamaSent"));
		assertNotNull(read(mode, "score"),
				"parent score array must be allocated");
		// Parent fever-map arrays — separate from the
		// Fever-specific subclass ones tested above.
		assertNotNull(read(mode, "feverMapSet"));
	}

	@Test
	void feverPerPlayerArraysAreZeroSeededByJvmDefault() throws Exception {
		// modeInit allocates new int[] arrays each call; JVM zero-init
		// means every slot starts at 0.
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameManager manager = new GameManager(new EventReceiver());

		mode.modeInit(manager);

		int[] ojamaHandicapLeft = (int[]) read(mode, "ojamaHandicapLeft");
		int[] feverChain = (int[]) read(mode, "feverChain");
		for(int v : ojamaHandicapLeft) assertEquals(0, v);
		for(int v : feverChain) assertEquals(0, v);
	}

	private static Object read(AvalancheVSFeverMode mode, String name) throws Exception {
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
