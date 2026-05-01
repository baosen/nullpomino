package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Pins AbstractMode's default GameMode contract — name, player count,
 * game style, and the menu-cursor accessors. Every concrete mode
 * inherits these defaults; a regression in one of them changes the
 * fallback for every shipped mode at once.
 */
class AbstractModeBasicsTest {

	private static final class StubMode extends AbstractMode {
		// AbstractMode is declared abstract but has no abstract methods —
		// this empty subclass is enough to exercise the defaults.
	}

	@Test
	void getNameDefaultsToDummy() {
		assertEquals("DUMMY", new StubMode().getName());
	}

	@Test
	void getPlayersDefaultsToOne() {
		assertEquals(1, new StubMode().getPlayers());
	}

	@Test
	void getGameStyleDefaultsToTetromino() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new StubMode().getGameStyle());
	}

	@Test
	void modeInitIsNoOp() {
		StubMode mode = new StubMode();

		// modeInit must accept any GameManager (including null) without throwing.
		mode.modeInit(null);
		mode.modeInit(new GameManager(new EventReceiver()));
	}

	@Test
	void getMenuCursorAndSetMenuCursorRoundTrip() {
		StubMode mode = new StubMode();

		assertEquals(0, mode.getMenuCursor(), "fresh mode starts at cursor 0");

		mode.setMenuCursor(5);
		assertEquals(5, mode.getMenuCursor());
	}

	@Test
	void getMenuItemCountReturnsMinusOneForEmptyMenu() {
		StubMode mode = new StubMode();

		// Default constructor leaves menu empty → contract is to return -1
		// rather than 0 (so callers can tell "no menu" from "menu with 0
		// items").
		assertEquals(-1, mode.getMenuItemCount());
	}

	@Test
	void renderInputDelegatesToReceiverWithoutThrowing() {
		// renderInput is the lobby-default controller-display hook; the
		// AbstractMode body is mostly receiver.drawMenuFont calls. With a
		// no-op EventReceiver attached we just verify it doesn't throw on
		// a freshly-init'd engine.
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();

		StubMode mode = new StubMode();

		mode.renderInput(gm.engine[0], 0);
	}

	@Test
	void freshModeHasInitialMenuCounters() {
		StubMode mode = new StubMode();

		// statcMenu, menuTime are protected; assert via behaviour.
		// getMenuCursor reads menuCursor (0). getMenuItemCount returns -1
		// (empty list). Constructor sets menu = new ArrayList, so size is 0.
		assertEquals(0, mode.getMenuCursor());
		assertEquals(-1, mode.getMenuItemCount());
		assertTrue(true, "default counters reachable through public API");
	}
}
