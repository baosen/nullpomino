package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the just-entered handshake that lets a freshly opened menu screen
 * snap its cursor to the row already under the mouse pointer, without
 * waiting for the user to nudge the mouse.
 *
 * <p>The hover-selection in {@code DummyMenuChooseStateSDL.updateMouseInput()}
 * is gated on {@code MouseInputSDL.isMouseMoved()} so a stationary pointer
 * can't fight keyboard navigation. That gate hides the initial position
 * on entry: the mouse is already where it is, but no movement event fires,
 * so the cursor sits at whatever default the screen booted with until the
 * user wiggles. The fix runs the gate as {@code (moved || justEntered)},
 * with {@code justEntered} flipped on by {@link NullpoMinoSDL#doTransition(int)}
 * after {@code enter()} returns.
 *
 * <p>Two tests cover the two surfaces of that contract:
 *   - the transition setter actually flips the flag (regardless of whether
 *     the destination subclass overrides {@code enter()} or chains to super);
 *   - the consumer reads-and-clears so the snap fires once per entry, not
 *     every frame.
 */
class DummyMenuChooseStateJustEnteredTest {

	private BaseStateSDL[] originalStates;
	private int originalCurrent;
	private boolean originalQuit;
	private Deque<Integer> originalBack;
	private Deque<Integer> originalForward;

	@BeforeEach
	void setUp() throws Exception {
		originalStates = NullpoMinoSDL.gameStates;
		originalCurrent = NullpoMinoSDL.currentState;
		originalQuit = NullpoMinoSDL.quit;
		originalBack = snapshot("backStack");
		originalForward = snapshot("forwardStack");

		BaseStateSDL[] stubs = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < stubs.length; i++) stubs[i] = new BaseStateSDL();
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.quit = false;
		stack("backStack").clear();
		stack("forwardStack").clear();
	}

	@AfterEach
	void tearDown() throws Exception {
		NullpoMinoSDL.gameStates = originalStates;
		NullpoMinoSDL.currentState = originalCurrent;
		NullpoMinoSDL.quit = originalQuit;
		restore("backStack", originalBack);
		restore("forwardStack", originalForward);
	}

	@Test
	void transitionFlipsJustEnteredOnMenuDestination() {
		// Subclasses override enter() without chaining to super, so the
		// flag-set has to live in doTransition() rather than in the base
		// class enter(). This pins that wiring.
		MenuStub menu = new MenuStub();
		NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_SELECTMODE] = menu;

		assertFalse(menu.justEntered, "fresh stub starts unflagged");

		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);

		assertTrue(menu.justEntered, "doTransition should set justEntered after enter()");
	}

	@Test
	void consumeJustEnteredReadsAndClears() {
		// The hover snap must fire once per entry, not every frame —
		// otherwise the keyboard-vs-mouse arbitration that the isMouseMoved
		// gate provides would be lost.
		MenuStub menu = new MenuStub();
		menu.justEntered = true;

		assertTrue(menu.consumeJustEntered(), "first consume returns the set value");
		assertFalse(menu.justEntered, "consume clears the flag");
		assertFalse(menu.consumeJustEntered(), "second consume returns the cleared value");
	}

	/**
	 * Concrete {@link DummyMenuChooseStateSDL} that overrides {@code enter()}
	 * without calling super, mirroring the pattern used by the real menu
	 * subclasses. This guards against a regression where the flag-set
	 * accidentally moves into the base class {@code enter()} — which would
	 * silently stop firing for any subclass that doesn't chain to super.
	 */
	private static final class MenuStub extends DummyMenuChooseStateSDL {
		MenuStub() {
			mouseEnabled = false;
			maxCursor = 3;
		}

		@Override
		public void enter() {
			// deliberately no super.enter()
		}
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
		Integer[] arr = snapshot.toArray(new Integer[0]);
		for(int i = arr.length - 1; i >= 0; i--) live.push(arr[i]);
	}
}
