package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the default method implementations in
 * {@link DummyMenuChooseStateSDL}: constructor defaults,
 * {@code consumeJustEntered()}, and the no-op default
 * overrides for {@code onChange()}, {@code onDecide()},
 * {@code onCancel()}, and {@code onPushButtonD()}.
 *
 * <p>The {@code justEntered} handshake with
 * {@link NullpoMinoSDL#doTransition(int)} is covered in
 * {@link DummyMenuChooseStateJustEnteredTest}, and the
 * forward-click wiring is covered in
 * {@link DummyMenuChooseStateForwardTest}.
 */
class DummyMenuChooseStateSDLLogicTest {

	@BeforeEach
	void setUp() {
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
	}

	@Test
	void constructorSetsDefaultValues() {
		MenuStub state = new MenuStub();

		assertEquals(-1, state.maxCursor);
		assertEquals(3, state.minChoiceY);
		assertFalse(state.mouseEnabled); // MenuStub overrides to false
		assertEquals(0, state.cursor);
	}

	@Test
	void constructorInitializesJustEnteredToFalse() {
		MenuStub state = new MenuStub();
		assertFalse(state.justEntered);
	}

	@Test
	void consumeJustEnteredReadsAndClears() {
		MenuStub state = new MenuStub();
		state.justEntered = true;

		assertTrue(state.consumeJustEntered());
		assertFalse(state.justEntered);
		assertFalse(state.consumeJustEntered());
	}

	@Test
	void consumeJustEnteredReturnsFalseByDefault() {
		MenuStub state = new MenuStub();
		assertFalse(state.consumeJustEntered());
	}

	@Test
	void onChangeDefaultIsNoOp() {
		MenuStub state = new MenuStub();
		// Must not throw
		state.onChange(1);
		state.onChange(-1);
		state.onChange(0);
	}

	@Test
	void onDecideDefaultReturnsFalse() {
		MenuStub state = new MenuStub();
		assertFalse(state.onDecide());
	}

	@Test
	void onCancelDefaultReturnsFalse() {
		MenuStub state = new MenuStub();
		assertFalse(state.onCancel());
	}

	@Test
	void onPushButtonDDefaultReturnsFalse() {
		MenuStub state = new MenuStub();
		assertFalse(state.onPushButtonD());
	}

	@Test
	void onPageEventDefaultJumpsToStartForPageUp() {
		MenuStub state = new MenuStub();
		state.maxCursor = 10;
		state.cursor = 5;

		state.onPageEvent(-1);

		assertEquals(0, state.cursor);
	}

	@Test
	void onPageEventDefaultJumpsToEndForPageDown() {
		MenuStub state = new MenuStub();
		state.maxCursor = 10;
		state.cursor = 5;

		state.onPageEvent(1);

		assertEquals(10, state.cursor);
	}

	@Test
	void onPageEventDefaultIsNoOpForOtherDirections() {
		MenuStub state = new MenuStub();
		state.maxCursor = 10;
		state.cursor = 5;

		state.onPageEvent(0);
		assertEquals(5, state.cursor);

		state.onPageEvent(2);
		assertEquals(5, state.cursor);
	}

	@Test
	void onPageEventDefaultClampsCursorAtBounds() {
		MenuStub state = new MenuStub();
		state.maxCursor = 10;

		state.cursor = 0;
		state.onPageEvent(-1);
		assertEquals(0, state.cursor, "page up at start stays at start");

		state.cursor = 10;
		state.onPageEvent(1);
		assertEquals(10, state.cursor, "page down at end stays at end");
	}

	/**
	 * Concrete {@link DummyMenuChooseStateSDL} with mouse disabled
	 * so no SDL polling happens during tests.
	 */
	private static final class MenuStub extends DummyMenuChooseStateSDL {
		MenuStub() {
			mouseEnabled = false;
		}
	}
}
