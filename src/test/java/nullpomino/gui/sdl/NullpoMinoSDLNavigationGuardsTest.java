package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

/**
 * Pins the small private-static guards on {@link NullpoMinoSDL} that
 * gate the navigation stack: {@code isStateId} validates a state ID
 * against the gameStates array bounds, and {@code isForwardSafe}
 * rejects the two states (INGAME, NETGAME) that aren't safe to
 * re-enter via the forward stack — re-entering them would replay a
 * game that's already torn down.
 */
class NullpoMinoSDLNavigationGuardsTest {

	@Test
	void isStateIdAcceptsZeroThroughMaxMinusOne() throws Exception {
		// Boundary check: 0 and STATE_MAX-1 are both valid IDs.
		assertTrue(invokeIsStateId(0));
		assertTrue(invokeIsStateId(NullpoMinoSDL.STATE_MAX - 1));
		// In-range mid-value.
		assertTrue(invokeIsStateId(NullpoMinoSDL.STATE_TITLE));
	}

	@Test
	void isStateIdRejectsNegativeIds() throws Exception {
		assertFalse(invokeIsStateId(-1));
		assertFalse(invokeIsStateId(-999));
	}

	@Test
	void isStateIdRejectsIdsAtOrAboveStateMax() throws Exception {
		assertFalse(invokeIsStateId(NullpoMinoSDL.STATE_MAX));
		assertFalse(invokeIsStateId(NullpoMinoSDL.STATE_MAX + 1));
		assertFalse(invokeIsStateId(99999));
	}

	@Test
	void isForwardSafeRejectsInGameAndNetGameStates() throws Exception {
		// goBack() out of an in-game state tears the session down;
		// re-entering via goForward() would land on the dead session
		// so isForwardSafe must reject both.
		assertFalse(invokeIsForwardSafe(NullpoMinoSDL.STATE_INGAME));
		assertFalse(invokeIsForwardSafe(NullpoMinoSDL.STATE_NETGAME));
	}

	@Test
	void isForwardSafeAcceptsEveryOtherStandardState() throws Exception {
		// Every menu state is safely re-enterable via goForward.
		// Spot-check the major ones.
		assertTrue(invokeIsForwardSafe(NullpoMinoSDL.STATE_TITLE));
		assertTrue(invokeIsForwardSafe(NullpoMinoSDL.STATE_SELECTMODE));
		assertTrue(invokeIsForwardSafe(NullpoMinoSDL.STATE_REPLAYSELECT));
		assertTrue(invokeIsForwardSafe(NullpoMinoSDL.STATE_CONFIG_MAINMENU));
		assertTrue(invokeIsForwardSafe(NullpoMinoSDL.STATE_NET_SERVERSELECT));
		assertTrue(invokeIsForwardSafe(NullpoMinoSDL.STATE_NET_LOBBY));
	}

	@Test
	void isForwardSafeAcceptsOutOfRangeIdsBecauseGuardChecksOnlyTwoIds() throws Exception {
		// isForwardSafe only excludes the two known unsafe IDs; any
		// other ID — including invalid ones — passes. The caller is
		// responsible for validating the ID via isStateId before
		// using the result.
		assertTrue(invokeIsForwardSafe(-1));
		assertTrue(invokeIsForwardSafe(NullpoMinoSDL.STATE_MAX));
		assertTrue(invokeIsForwardSafe(99999));
	}

	private static boolean invokeIsStateId(int id) throws Exception {
		Method m = NullpoMinoSDL.class.getDeclaredMethod("isStateId", int.class);
		m.setAccessible(true);
		return (boolean) m.invoke(null, id);
	}

	private static boolean invokeIsForwardSafe(int id) throws Exception {
		Method m = NullpoMinoSDL.class.getDeclaredMethod("isForwardSafe", int.class);
		m.setAccessible(true);
		return (boolean) m.invoke(null, id);
	}
}
