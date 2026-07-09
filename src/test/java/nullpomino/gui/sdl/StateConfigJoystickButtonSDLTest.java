package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.util.CustomProperties;

/**
 * Pins the headless slice of {@link StateConfigJoystickButtonSDL}: the
 * KEYACCEPTFRAME constant, reset() seeding from the player's GameKeySDL
 * buttonmap and from joyUseNumber / joystickMax, the no-gamepad
 * branch that leaves previousJoyPressedState null, and
 * getPressedKeyNumber's first-difference scan.
 */
class StateConfigJoystickButtonSDLTest {

	private GameKeySDL[] originalGameKey;
	private int[] originalJoyUseNumber;
	private int originalJoystickMax;

	@BeforeEach
	void setUp() {
		originalGameKey = GameKeySDL.gamekey;
		originalJoyUseNumber = NullpoMinoSDL.joyUseNumber;
		originalJoystickMax = NullpoMinoSDL.joystickMax;
		GameKeySDL.initGlobalGameKeySDL();
		NullpoMinoSDL.joyUseNumber = new int[] {-1, -1};
		NullpoMinoSDL.joystickMax = 0;
	}

	@AfterEach
	void tearDown() {
		GameKeySDL.gamekey = originalGameKey;
		NullpoMinoSDL.joyUseNumber = originalJoyUseNumber;
		NullpoMinoSDL.joystickMax = originalJoystickMax;
	}

	@Test
	void keyacceptframeIsTwentyFrames() {
		assertEquals(20, StateConfigJoystickButtonSDL.KEYACCEPTFRAME);
	}

	@Test
	void resetLeavesPreviousJoyPressedStateNullWhenNoJoystickIsBound() throws Exception {
		StateConfigJoystickButtonSDL state = new StateConfigJoystickButtonSDL();
		state.player = 0;
		// joyUseNumber[0] stays at -1 -> no joystick.

		invokeReset(state);

		assertNull(readField(state, "previousJoyPressedState"),
				"joyNumber = -1 means render() shows 'NO JOYSTICK' and the "
						+ "press-state buffer stays null");
		assertEquals(-1, readInt(state, "joyNumber"));
	}

	@Test
	void resetAllocatesPreviousJoyPressedStateAtGamepadButtonCount() throws Exception {
		// Simulate one connected gamepad; player 1 picks it.
		NullpoMinoSDL.joystickMax = 1;
		NullpoMinoSDL.joyUseNumber = new int[] {-1, 0};

		StateConfigJoystickButtonSDL state = new StateConfigJoystickButtonSDL();
		state.player = 1;

		invokeReset(state);

		boolean[] previous = (boolean[]) readField(state, "previousJoyPressedState");
		assertNotNull(previous);
		assertEquals(SDLConstants.SDL_GAMEPAD_NUM_BUTTONS, previous.length,
				"buffer covers all standardized gamepad button ordinals");
		assertEquals(0, readInt(state, "joyNumber"));
	}

	@Test
	void resetCopiesGameKeyButtonmapIntoTheLocalDraft() throws Exception {
		// Spoil player 0's buttonmap so we can prove reset() pulls it in.
		for(int i = 0; i < GameKeySDL.MAX_BUTTON; i++) {
			GameKeySDL.gamekey[0].buttonmap[i] = 50 + i;
		}

		StateConfigJoystickButtonSDL state = new StateConfigJoystickButtonSDL();
		state.player = 0;

		invokeReset(state);

		int[] buttonmap = (int[]) readField(state, "buttonmap");
		assertEquals(GameKeySDL.MAX_BUTTON, buttonmap.length);
		for(int i = 0; i < buttonmap.length; i++) {
			assertEquals(50 + i, buttonmap[i],
					"reset() must snapshot gamekey[player].buttonmap so the "
							+ "user can cancel without losing it");
		}
	}

	@Test
	void resetSeedsKeynumAtFourSoTheCursorStartsOnButtonA() throws Exception {
		// First configurable row is 'A (L/R-ROT)' at keynum=4. Anything else
		// would render the cursor on the no-op rows above.
		StateConfigJoystickButtonSDL state = new StateConfigJoystickButtonSDL();
		state.player = 0;

		invokeReset(state);

		assertEquals(4, readInt(state, "keynum"));
		assertEquals(0, readInt(state, "frame"));
		assertEquals(0, readInt(state, "upInputState"));
		assertEquals(0, readInt(state, "downInputState"));
	}

	@Test
	void getPressedKeyNumberReturnsMinusOneWhenStatesAgree() throws Exception {
		StateConfigJoystickButtonSDL state = new StateConfigJoystickButtonSDL();
		boolean[] prev = new boolean[6];
		boolean[] now = new boolean[6];

		assertEquals(-1, invokeGetPressedKeyNumber(state, prev, now));
	}

	@Test
	void getPressedKeyNumberReturnsLowestDifferingIndex() throws Exception {
		StateConfigJoystickButtonSDL state = new StateConfigJoystickButtonSDL();
		boolean[] prev = new boolean[8];
		boolean[] now = new boolean[8];
		now[2] = true;
		now[5] = true;

		assertEquals(2, invokeGetPressedKeyNumber(state, prev, now));
	}

	@Test
	void leaveAppliesAndPersistsTheDraftBindings() throws Exception {
		// Regression: every exit path must save — a gamepad-first user cannot
		// press a keyboard "confirm" key, and the old silent-cancel semantics
		// threw their bindings away.
		CustomProperties originalProp = NullpoMinoSDL.propConfig;
		CustomProperties originalGlobal = NullpoMinoSDL.propGlobal;
		NullpoMinoSDL.propConfig = new CustomProperties();
		NullpoMinoSDL.propGlobal = new CustomProperties();
		try {
			StateConfigJoystickButtonSDL state = new StateConfigJoystickButtonSDL();
			state.player = 0;
			invokeReset(state);
			int[] draft = (int[]) readField(state, "buttonmap");
			draft[GameKeySDL.BUTTON_A] = SDLConstants.SDL_GAMEPAD_BUTTON_NORTH;

			state.leave();

			assertEquals(SDLConstants.SDL_GAMEPAD_BUTTON_NORTH,
					GameKeySDL.gamekey[0].buttonmap[GameKeySDL.BUTTON_A],
					"leave() must copy the draft into the live gamekey");
			assertEquals(SDLConstants.SDL_GAMEPAD_BUTTON_NORTH,
					NullpoMinoSDL.propConfig.getProperty("button.p0.a", -999),
					"leave() must persist the draft to the config properties");
		} finally {
			NullpoMinoSDL.propConfig = originalProp;
			NullpoMinoSDL.propGlobal = originalGlobal;
		}
	}

	@Test
	void getPressedKeyNumberIgnoresDpadOrdinals() throws Exception {
		// D-pad (ordinals 11-14) drives the directions exclusively and must not
		// be bindable to actions; ordinals up to 10 (RB/R1) still register.
		StateConfigJoystickButtonSDL state = new StateConfigJoystickButtonSDL();
		boolean[] prev = new boolean[SDLConstants.SDL_GAMEPAD_NUM_BUTTONS];
		boolean[] now = new boolean[SDLConstants.SDL_GAMEPAD_NUM_BUTTONS];

		now[SDLConstants.SDL_GAMEPAD_BUTTON_DPAD_DOWN] = true;
		assertEquals(-1, invokeGetPressedKeyNumber(state, prev, now));

		now[SDLConstants.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER] = true;
		assertEquals(SDLConstants.SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER,
				invokeGetPressedKeyNumber(state, prev, now));
	}

	@Test
	void getPressedKeyNumberDetectsReleasesAsWell() throws Exception {
		// Releases (now=false where prev=true) also count as "first difference"
		// — the user un-pressing a button is still an event the loop wants to
		// see; it just won't be saved into buttonmap because the caller checks
		// for now[i] before storing.
		StateConfigJoystickButtonSDL state = new StateConfigJoystickButtonSDL();
		boolean[] prev = new boolean[] {false, true, true};
		boolean[] now  = new boolean[] {false, true, false};

		assertEquals(2, invokeGetPressedKeyNumber(state, prev, now));
	}

	private static int readInt(Object instance, String name) throws Exception {
		Field f = instance.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static Object readField(Object instance, String name) throws Exception {
		Field f = instance.getClass().getDeclaredField(name);
		f.setAccessible(true);
		return f.get(instance);
	}

	private static void invokeReset(StateConfigJoystickButtonSDL state) throws Exception {
		Method m = StateConfigJoystickButtonSDL.class.getDeclaredMethod("reset");
		m.setAccessible(true);
		m.invoke(state);
	}

	private static int invokeGetPressedKeyNumber(StateConfigJoystickButtonSDL state,
			boolean[] prev, boolean[] now) throws Exception {
		Method m = StateConfigJoystickButtonSDL.class.getDeclaredMethod(
				"getPressedKeyNumber", boolean[].class, boolean[].class);
		m.setAccessible(true);
		return (int) m.invoke(state, prev, now);
	}
}
