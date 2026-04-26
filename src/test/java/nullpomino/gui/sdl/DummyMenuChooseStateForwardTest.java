package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;

import nullpomino.gui.MouseInputDummy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the menu-base wiring that routes a mouse forward (X2) click to
 * {@link NullpoMinoSDL#goForward()}. Every menu screen that extends
 * {@link DummyMenuChooseStateSDL} (title, mode/replay/rule selectors, the
 * config menus that don't override {@code update()}) inherits this
 * branch, so a regression here drops forward navigation across the
 * whole menu tree at once.
 *
 * <p>Drives an instance of the menu base (with {@code mouseEnabled=false}
 * so the SDL polling is skipped) after manually populating the back/forward
 * stacks; asserts the state transition matches what {@code goForward()}
 * would produce.
 */
class DummyMenuChooseStateForwardTest {

	private BaseStateSDL[] originalStates;
	private int originalCurrent;
	private boolean originalQuit;
	private Deque<Integer> originalBack;
	private Deque<Integer> originalForward;
	private GameKeySDL[] originalKeys;
	private MouseInputSDL originalMouse;

	@BeforeEach
	void setUp() throws Exception {
		originalStates = NullpoMinoSDL.gameStates;
		originalCurrent = NullpoMinoSDL.currentState;
		originalQuit = NullpoMinoSDL.quit;
		originalBack = snapshot("backStack");
		originalForward = snapshot("forwardStack");
		originalKeys = GameKeySDL.gamekey;
		originalMouse = MouseInputSDL.mouseInput;

		BaseStateSDL[] stubs = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < stubs.length; i++) stubs[i] = new BaseStateSDL();
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.quit = false;
		stack("backStack").clear();
		stack("forwardStack").clear();

		GameKeySDL.gamekey = new GameKeySDL[] {new GameKeySDL(0), new GameKeySDL(1)};
		MouseInputSDL.mouseInput = newSilentMouse();
	}

	@AfterEach
	void tearDown() throws Exception {
		NullpoMinoSDL.gameStates = originalStates;
		NullpoMinoSDL.currentState = originalCurrent;
		NullpoMinoSDL.quit = originalQuit;
		restore("backStack", originalBack);
		restore("forwardStack", originalForward);
		GameKeySDL.gamekey = originalKeys;
		MouseInputSDL.mouseInput = originalMouse;
	}

	@Test
	void forwardClickFromMenuRoutesToGoForward() throws Exception {
		// SELECTMODE → REPLAYSELECT then back to SELECTMODE leaves
		// REPLAYSELECT on the forward stack.
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_REPLAYSELECT);
		NullpoMinoSDL.goBack();
		assertEquals(NullpoMinoSDL.STATE_SELECTMODE, NullpoMinoSDL.currentState);
		assertEquals(1, stack("forwardStack").size());

		setForwardPressed(1);
		new MenuStateUnderTest().update();

		// goForward() popped REPLAYSELECT and re-entered it.
		assertEquals(NullpoMinoSDL.STATE_REPLAYSELECT, NullpoMinoSDL.currentState);
	}

	@Test
	void forwardClickWithEmptyForwardStackIsNoOp() throws Exception {
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		assertEquals(0, stack("forwardStack").size());

		setForwardPressed(1);
		new MenuStateUnderTest().update();

		// goForward() bailed early on the empty stack; we stayed put.
		assertEquals(NullpoMinoSDL.STATE_SELECTMODE, NullpoMinoSDL.currentState);
	}

	/**
	 * Concrete {@link DummyMenuChooseStateSDL} for the test. Disables mouse
	 * polling (so the SDL bindings don't fire) and sets {@code maxCursor=-1}
	 * to skip cursor / decision handling — leaves only the cancel and
	 * forward branches at the tail of {@code update()}.
	 */
	private static final class MenuStateUnderTest extends DummyMenuChooseStateSDL {
		MenuStateUnderTest() {
			mouseEnabled = false;
			maxCursor = -1;
		}
	}

	private static MouseInputSDL newSilentMouse() throws Exception {
		Constructor<MouseInputSDL> ctor = MouseInputSDL.class.getDeclaredConstructor();
		ctor.setAccessible(true);
		return ctor.newInstance();
	}

	private static void setForwardPressed(int frames) throws Exception {
		Field f = MouseInputDummy.class.getDeclaredField("mouseForwardPressed");
		f.setAccessible(true);
		f.setInt(MouseInputSDL.mouseInput, frames);
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
