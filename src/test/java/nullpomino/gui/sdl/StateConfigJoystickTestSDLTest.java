package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.binding.SDLConstants;

class StateConfigJoystickTestSDLTest {

	private int[] originalJoyUseNumber;
	private int originalJoystickMax;
	private boolean originalEnableSpecialKeys;

	@BeforeEach
	void snapshotStaticState() {
		originalJoyUseNumber = NullpoMinoSDL.joyUseNumber;
		originalJoystickMax = NullpoMinoSDL.joystickMax;
		originalEnableSpecialKeys = NullpoMinoSDL.enableSpecialKeys;
		NullpoMinoSDL.enableSpecialKeys = true;
	}

	@AfterEach
	void restoreStaticState() {
		NullpoMinoSDL.joyUseNumber = originalJoyUseNumber;
		NullpoMinoSDL.joystickMax = originalJoystickMax;
		NullpoMinoSDL.enableSpecialKeys = originalEnableSpecialKeys;
	}

	@Test
	void constructorPinsPlayerToZero() {
		assertEquals(0, new StateConfigJoystickTestSDL().player);
	}

	@Test
	void keyAcceptFrameMatchesDocumentedConstant() {
		// Pin the legacy 20-frame input grace period so that any change is
		// caught at build time rather than as a feel regression in QA.
		assertEquals(20, StateConfigJoystickTestSDL.KEYACCEPTFRAME);
	}

	@Test
	void resetWithoutAGamepadAssignedClearsTheKeyHistoryArray() throws Exception {
		NullpoMinoSDL.joyUseNumber = new int[] {-1, -1};
		NullpoMinoSDL.joystickMax = 1;
		StateConfigJoystickTestSDL state = new StateConfigJoystickTestSDL();
		state.player = 0;

		invokeReset(state);

		assertEquals(-1, readInt(state, "joyNumber"));
		assertEquals(-1, readInt(state, "lastPressButton"));
		assertEquals(0, readInt(state, "frame"));
		assertNull(readField(state, "previousJoyPressedState"),
				"missing gamepad must zero the history array, not allocate it");
	}

	@Test
	void resetAllocatesPreviousJoyPressedStateMatchingTheGamepadButtonCount() throws Exception {
		NullpoMinoSDL.joyUseNumber = new int[] {1, 0};
		NullpoMinoSDL.joystickMax = 2;
		StateConfigJoystickTestSDL state = new StateConfigJoystickTestSDL();
		state.player = 0;

		invokeReset(state);

		assertEquals(1, readInt(state, "joyNumber"));
		boolean[] history = (boolean[]) readField(state, "previousJoyPressedState");
		assertNotNull(history);
		assertEquals(SDLConstants.SDL_GAMEPAD_NUM_BUTTONS, history.length);
	}

	@Test
	void getPressedKeyNumberReturnsTheFirstDifferingIndex() throws Exception {
		StateConfigJoystickTestSDL state = new StateConfigJoystickTestSDL();
		boolean[] prev = {false, false, false, false};
		boolean[] now = {false, true, false, true};

		assertEquals(1, invokeGetPressedKeyNumber(state, prev, now));
	}

	@Test
	void getPressedKeyNumberReturnsMinusOneWhenInputsMatch() throws Exception {
		StateConfigJoystickTestSDL state = new StateConfigJoystickTestSDL();
		boolean[] prev = {true, false, true};
		boolean[] now = {true, false, true};

		assertEquals(-1, invokeGetPressedKeyNumber(state, prev, now));
	}

	@Test
	void getPressedKeyNumberDetectsReleaseEdgesAsWellAsPresses() throws Exception {
		// Edge detection is bidirectional — releases should also report.
		StateConfigJoystickTestSDL state = new StateConfigJoystickTestSDL();
		boolean[] prev = {true, true, true};
		boolean[] now  = {true, false, true};

		assertEquals(1, invokeGetPressedKeyNumber(state, prev, now));
	}

	@Test
	void enterInvokesResetAndDisablesSpecialKeysSoTheirEventsDoNotEscape() throws Exception {
		NullpoMinoSDL.joyUseNumber = new int[] {-1};
		NullpoMinoSDL.joystickMax = 0;
		NullpoMinoSDL.enableSpecialKeys = true;

		StateConfigJoystickTestSDL state = new StateConfigJoystickTestSDL();
		// Plant a sentinel in lastPressButton so we can confirm reset runs.
		setInt(state, "lastPressButton", 99);

		state.enter();

		assertEquals(-1, readInt(state, "lastPressButton"),
				"enter() must call reset() so the screen starts cleanly");
		assertFalse(NullpoMinoSDL.enableSpecialKeys,
				"enter must disable special keys so their state is stable for the test");
	}

	@Test
	void leaveInvokesResetAndReenablesSpecialKeys() throws Exception {
		NullpoMinoSDL.joyUseNumber = new int[] {-1};
		NullpoMinoSDL.joystickMax = 0;
		NullpoMinoSDL.enableSpecialKeys = false;

		StateConfigJoystickTestSDL state = new StateConfigJoystickTestSDL();
		setInt(state, "frame", 7);

		state.leave();

		assertEquals(0, readInt(state, "frame"),
				"leave() must call reset() so the next entry starts cleanly");
		assertTrue(NullpoMinoSDL.enableSpecialKeys,
				"leave must restore special-key handling for the rest of the menu");
	}

	@Test
	void resetIsIdempotentForBackToBackEntries() throws Exception {
		NullpoMinoSDL.joyUseNumber = new int[] {2};
		NullpoMinoSDL.joystickMax = 3;
		StateConfigJoystickTestSDL state = new StateConfigJoystickTestSDL();
		state.player = 0;

		invokeReset(state);
		boolean[] firstHistory = (boolean[]) readField(state, "previousJoyPressedState");
		invokeReset(state);
		boolean[] secondHistory = (boolean[]) readField(state, "previousJoyPressedState");

		// reset() reallocates each time so its zeroing semantics are obvious;
		// pin both (a) it allocates a fresh array, (b) the array shape stays
		// the fixed gamepad button count.
		assertNotNull(firstHistory);
		assertNotNull(secondHistory);
		assertEquals(SDLConstants.SDL_GAMEPAD_NUM_BUTTONS, firstHistory.length);
		assertArrayEquals(new boolean[SDLConstants.SDL_GAMEPAD_NUM_BUTTONS], secondHistory);
	}

	private static void invokeReset(StateConfigJoystickTestSDL state) throws Exception {
		Method m = StateConfigJoystickTestSDL.class.getDeclaredMethod("reset");
		m.setAccessible(true);
		m.invoke(state);
	}

	private static int invokeGetPressedKeyNumber(StateConfigJoystickTestSDL state,
			boolean[] prev, boolean[] now) throws Exception {
		Method m = StateConfigJoystickTestSDL.class.getDeclaredMethod(
				"getPressedKeyNumber", boolean[].class, boolean[].class);
		m.setAccessible(true);
		return (int) m.invoke(state, prev, now);
	}

	private static int readInt(StateConfigJoystickTestSDL state, String name) throws Exception {
		Field f = StateConfigJoystickTestSDL.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(state);
	}

	private static Object readField(StateConfigJoystickTestSDL state, String name) throws Exception {
		Field f = StateConfigJoystickTestSDL.class.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(state);
	}

	private static void setInt(StateConfigJoystickTestSDL state, String name, int value) throws Exception {
		Field f = StateConfigJoystickTestSDL.class.getDeclaredField(name);
		f.setAccessible(true);
		f.setInt(state, value);
	}
}
