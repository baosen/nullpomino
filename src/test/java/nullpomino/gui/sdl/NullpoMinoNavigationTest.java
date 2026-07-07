package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the browser-style back/forward stack semantics on
 * {@link NullpoMinoSDL}. Driven through the public API
 * (enterState / goBack / goForward / enterStateClear); state assertions
 * read {@code currentState} directly and the private stacks via reflection
 * for size checks.
 *
 * <p>Each test installs a fresh {@code BaseStateSDL[]} of no-op stubs so
 * {@code doTransition()} can call {@code leave()/enter()} without crashing
 * on the real states (which expect SDL to be initialized).
 */
class NullpoMinoNavigationTest {

	private BaseStateSDL[] originalStates;
	private int originalCurrent;
	private boolean originalQuit;
	private boolean originalWebMode;
	private Deque<Integer> originalBack;
	private Deque<Integer> originalForward;

	@BeforeEach
	void setUp() throws Exception {
		originalStates = NullpoMinoSDL.gameStates;
		originalCurrent = NullpoMinoSDL.currentState;
		originalQuit = NullpoMinoSDL.quit;
		originalWebMode = NullpoMinoSDL.webMode;
		originalBack = snapshot("backStack");
		originalForward = snapshot("forwardStack");

		BaseStateSDL[] stubs = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < stubs.length; i++) stubs[i] = new BaseStateSDL();
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_TITLE;
		NullpoMinoSDL.quit = false;
		NullpoMinoSDL.webMode = false;
		stack("backStack").clear();
		stack("forwardStack").clear();
	}

	@AfterEach
	void tearDown() throws Exception {
		NullpoMinoSDL.gameStates = originalStates;
		NullpoMinoSDL.currentState = originalCurrent;
		NullpoMinoSDL.quit = originalQuit;
		NullpoMinoSDL.webMode = originalWebMode;
		restore("backStack", originalBack);
		restore("forwardStack", originalForward);
	}

	@Test
	void enterStatePushesOutgoingOntoBackStack() throws Exception {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);

		assertEquals(NullpoMinoSDL.STATE_SELECTMODE, NullpoMinoSDL.currentState);
		assertEquals(1, stack("backStack").size());
		assertEquals(0, stack("forwardStack").size());
	}

	@Test
	void goBackUnwindsToPreviousStateAndPopulatesForwardStack() throws Exception {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_REPLAYSELECT);

		NullpoMinoSDL.goBack();

		assertEquals(NullpoMinoSDL.STATE_SELECTMODE, NullpoMinoSDL.currentState);
		// REPLAYSELECT is forward-safe, so the back hop is replayable.
		assertEquals(1, stack("forwardStack").size());
	}

	@Test
	void goForwardReplaysTheBackHop() throws Exception {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_REPLAYSELECT);
		NullpoMinoSDL.goBack();

		NullpoMinoSDL.goForward();

		assertEquals(NullpoMinoSDL.STATE_REPLAYSELECT, NullpoMinoSDL.currentState);
		assertEquals(0, stack("forwardStack").size());
		// SELECTMODE went onto the back stack so the trip is reversible.
		assertTrue(stack("backStack").contains(NullpoMinoSDL.STATE_SELECTMODE));
	}

	@Test
	void enterStateClearsForwardStackOnFreshNavigation() throws Exception {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_REPLAYSELECT);
		NullpoMinoSDL.goBack();
		assertEquals(1, stack("forwardStack").size());

		// Branching off — the redo history becomes invalid (browser semantics).
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_MAINMENU);

		assertEquals(0, stack("forwardStack").size());
	}

	@Test
	void enterStateClearWipesBothStacksAndSeedsTitle() throws Exception {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_REPLAYSELECT);
		NullpoMinoSDL.goBack();

		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_CONFIG_MAINMENU);

		assertEquals(NullpoMinoSDL.STATE_CONFIG_MAINMENU, NullpoMinoSDL.currentState);
		assertEquals(0, stack("forwardStack").size());
		// Back stack is re-seeded with TITLE so cancel doesn't quit immediately.
		assertEquals(1, stack("backStack").size());
		assertEquals(NullpoMinoSDL.STATE_TITLE, stack("backStack").peek());
	}

	@Test
	void enterStateClearToTitleLeavesBackStackEmpty() throws Exception {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);

		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);

		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
		assertEquals(0, stack("backStack").size());
		assertEquals(0, stack("forwardStack").size());
	}

	@Test
	void goBackFromInGameDoesNotPushToForwardStack() throws Exception {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_INGAME);

		NullpoMinoSDL.goBack();

		assertEquals(NullpoMinoSDL.STATE_SELECTMODE, NullpoMinoSDL.currentState);
		// STATE_INGAME's leave() tears down gameManager — re-entering would
		// crash, so it must be excluded from the redo history.
		assertEquals(0, stack("forwardStack").size());
	}

	@Test
	void goBackFromNetGameDoesNotPushToForwardStack() throws Exception {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NET_LOBBY);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_NETGAME);

		NullpoMinoSDL.goBack();

		assertEquals(NullpoMinoSDL.STATE_NET_LOBBY, NullpoMinoSDL.currentState);
		// STATE_NETGAME's leave() releases room state — same restriction.
		assertEquals(0, stack("forwardStack").size());
	}

	@Test
	void goBackOnEmptyStackSetsQuitFlag() throws Exception {
		// Fresh setUp: backStack is empty, currentState is TITLE.
		NullpoMinoSDL.goBack();

		assertTrue(NullpoMinoSDL.quit, "empty back stack must quit");
	}

	@Test
	void goBackOnEmptyStackDoesNotQuitInWebMode() throws Exception {
		// Escape at the title screen (root) lands here. In a browser tab there
		// is no process to quit, so breaking the loop would freeze the canvas
		// and the game would appear to hang. The request must be a no-op.
		NullpoMinoSDL.webMode = true;

		NullpoMinoSDL.goBack();

		assertFalse(NullpoMinoSDL.quit, "web build must not quit on empty back stack");
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void enterStateMinusOneDoesNotQuitInWebMode() throws Exception {
		// The BUTTON_QUIT special key routes through enterState(-1); same rule.
		NullpoMinoSDL.webMode = true;

		NullpoMinoSDL.enterState(-1);

		assertFalse(NullpoMinoSDL.quit, "web build must not quit on enterState(-1)");
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void goForwardOnEmptyStackIsNoOp() throws Exception {
		int before = NullpoMinoSDL.currentState;

		NullpoMinoSDL.goForward();

		assertEquals(before, NullpoMinoSDL.currentState);
		assertEquals(0, stack("backStack").size());
	}

	@SuppressWarnings("unchecked")
	private static Deque<Integer> stack(String name) throws Exception {
		Field f = NullpoMinoSDL.class.getDeclaredField(name);
		f.setAccessible(true);
		return (Deque<Integer>) f.get(null);
	}

	private static Deque<Integer> snapshot(String name) throws Exception {
		return new ArrayDeque<>(stack(name));
	}

	private static void restore(String name, Deque<Integer> snapshot) throws Exception {
		Deque<Integer> live = stack(name);
		live.clear();
		// snapshot iteration order is top → bottom; pushAll restores order.
		Integer[] arr = snapshot.toArray(new Integer[0]);
		for(int i = arr.length - 1; i >= 0; i--) live.push(arr[i]);
	}
}
