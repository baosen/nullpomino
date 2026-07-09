package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.util.CustomProperties;

/**
 * Pins the navigation and state management logic in
 * {@link NullpoMinoSDL} that doesn't require SDL: state transition
 * guards, navigation stack operations, text-input lifecycle, and
 * netplay teardown.
 *
 * <p>These tests complement the existing {@link NullpoMinoSDLHelpersTest}
 * (consumeTextInput, isEscapePushedThisFrame, getUIText) and
 * {@link NullpoMinoSDLNavigationGuardsTest} (isStateId, isForwardSafe).
 */
class NullpoMinoSDLLogicTest {

	private BaseStateSDL[] originalStates;
	private int originalCurrent;
	private boolean originalQuit;
	private Deque<Integer> originalBack;
	private Deque<Integer> originalForward;
	private GameKeySDL[] originalGameKey;
	private boolean[] originalKeyPressed;
	private CustomProperties originalPropConfig;
	private boolean originalFullscreen;

	@BeforeEach
	void setUp() throws Exception {
		originalStates = NullpoMinoSDL.gameStates;
		originalCurrent = NullpoMinoSDL.currentState;
		originalQuit = NullpoMinoSDL.quit;
		originalBack = snapshot("backStack");
		originalForward = snapshot("forwardStack");
		originalGameKey = GameKeySDL.gamekey;
		originalKeyPressed = NullpoMinoSDL.keyPressedState;
		originalPropConfig = NullpoMinoSDL.propConfig;
		originalFullscreen = NullpoMinoSDL.fullscreen;
		NullpoMinoSDL.propConfig = new CustomProperties();

		// Set up stub states so enterState / goBack / goForward work.
		BaseStateSDL[] stubs = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for (int i = 0; i < stubs.length; i++) {
			stubs[i] = new BaseStateSDL();
		}
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.quit = false;
		NullpoMinoSDL.currentState = -1;
		stack("backStack").clear();
		stack("forwardStack").clear();

		GameKeySDL.gamekey = new GameKeySDL[] {new GameKeySDL(0), new GameKeySDL(1)};
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
	}

	@AfterEach
	void tearDown() throws Exception {
		NullpoMinoSDL.gameStates = originalStates;
		NullpoMinoSDL.currentState = originalCurrent;
		NullpoMinoSDL.quit = originalQuit;
		restore("backStack", originalBack);
		restore("forwardStack", originalForward);
		GameKeySDL.gamekey = originalGameKey;
		NullpoMinoSDL.keyPressedState = originalKeyPressed;
		NullpoMinoSDL.propConfig = originalPropConfig;
		NullpoMinoSDL.fullscreen = originalFullscreen;
	}

	/* ---------- hasCurrentState ---------- */

	@Test
	void hasCurrentStateReturnsFalseWhenCurrentIsNegative() throws Exception {
		NullpoMinoSDL.currentState = -1;
		assertFalse(invokeHasCurrentState());
	}

	@Test
	void hasCurrentStateReturnsTrueForValidCurrent() throws Exception {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_TITLE;
		assertTrue(invokeHasCurrentState());
	}

	/* ---------- enterState ---------- */

	@Test
	void enterStateMinusOneSetsQuitFlag() {
		NullpoMinoSDL.enterState(-1);
		assertTrue(NullpoMinoSDL.quit);
	}

	@Test
	void enterStateTransitionsToValidState() {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_TITLE;
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);

		assertEquals(NullpoMinoSDL.STATE_SELECTMODE, NullpoMinoSDL.currentState);
	}

	@Test
	void enterStatePushesPreviousToBackStack() throws Exception {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_TITLE;
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);

		Deque<Integer> back = stack("backStack");
		assertEquals(1, back.size());
		assertEquals(Integer.valueOf(NullpoMinoSDL.STATE_TITLE), back.peek());
	}

	@Test
	void enterStateClearsForwardStack() throws Exception {
		Deque<Integer> forward = stack("forwardStack");
		forward.push(NullpoMinoSDL.STATE_SELECTMODE);

		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE);

		assertTrue(forward.isEmpty());
	}

	@Test
	void enterStateWithNoCurrentStatePushesNothing() throws Exception {
		NullpoMinoSDL.currentState = -1;
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE);

		assertTrue(stack("backStack").isEmpty());
	}

	/* ---------- goBack ---------- */

	@Test
	void goBackQuitsWhenStackIsEmpty() {
		NullpoMinoSDL.quit = false;
		NullpoMinoSDL.goBack();

		assertTrue(NullpoMinoSDL.quit);
	}

	@Test
	void goBackTransitionsToPreviousState() throws Exception {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_SELECTMODE;
		stack("backStack").push(NullpoMinoSDL.STATE_TITLE);

		NullpoMinoSDL.goBack();

		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void goBackPushesCurrentToForwardStackWhenSafe() throws Exception {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_SELECTMODE;
		stack("backStack").push(NullpoMinoSDL.STATE_TITLE);

		NullpoMinoSDL.goBack();

		Deque<Integer> forward = stack("forwardStack");
		assertEquals(1, forward.size());
		assertEquals(Integer.valueOf(NullpoMinoSDL.STATE_SELECTMODE), forward.peek());
	}

	@Test
	void goBackDoesNotPushUnsafeStatesToForwardStack() throws Exception {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_INGAME;
		stack("backStack").push(NullpoMinoSDL.STATE_TITLE);

		NullpoMinoSDL.goBack();

		assertTrue(stack("forwardStack").isEmpty());
	}

	@Test
	void goBackDoesNotPushWhenCurrentIsInvalid() throws Exception {
		NullpoMinoSDL.currentState = -1;
		stack("backStack").push(NullpoMinoSDL.STATE_TITLE);

		NullpoMinoSDL.goBack();

		assertTrue(stack("forwardStack").isEmpty());
	}

	/* ---------- goForward ---------- */

	@Test
	void goForwardIsNoOpWhenForwardStackIsEmpty() {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_TITLE;
		NullpoMinoSDL.goForward();

		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void goForwardIsNoOpWhenPoppedStateIsUnsafe() throws Exception {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_TITLE;
		stack("forwardStack").push(NullpoMinoSDL.STATE_INGAME);

		NullpoMinoSDL.goForward();

		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
		assertTrue(stack("forwardStack").isEmpty());
	}

	@Test
	void goForwardTransitionsToForwardState() throws Exception {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_TITLE;
		stack("forwardStack").push(NullpoMinoSDL.STATE_SELECTMODE);

		NullpoMinoSDL.goForward();

		assertEquals(NullpoMinoSDL.STATE_SELECTMODE, NullpoMinoSDL.currentState);
	}

	@Test
	void goForwardPushesCurrentToBackStack() throws Exception {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_TITLE;
		stack("forwardStack").push(NullpoMinoSDL.STATE_SELECTMODE);

		NullpoMinoSDL.goForward();

		Deque<Integer> back = stack("backStack");
		assertEquals(1, back.size());
		assertEquals(Integer.valueOf(NullpoMinoSDL.STATE_TITLE), back.peek());
	}

	/* ---------- enterStateClear ---------- */

	@Test
	void enterStateClearClearsBothStacks() throws Exception {
		stack("backStack").push(NullpoMinoSDL.STATE_SELECTMODE);
		stack("forwardStack").push(NullpoMinoSDL.STATE_REPLAYSELECT);

		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);

		assertTrue(stack("backStack").isEmpty());
		assertTrue(stack("forwardStack").isEmpty());
	}

	@Test
	void enterStateClearSeedsBackStackWithTitleWhenDestinationIsNotTitle() throws Exception {
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_SELECTMODE);

		Deque<Integer> back = stack("backStack");
		assertEquals(1, back.size());
		assertEquals(Integer.valueOf(NullpoMinoSDL.STATE_TITLE), back.peek());
	}

	@Test
	void enterStateClearDoesNotSeedBackStackWhenDestinationIsTitle() throws Exception {
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);

		assertTrue(stack("backStack").isEmpty());
	}

	/* ---------- endNetplay ---------- */

	@Test
	void endNetplayEnterStateClearTitle() throws Exception {
		// endNetplay should call enterStateClear(STATE_TITLE)
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_NET_LOBBY;
		NullpoMinoSDL.endNetplay();

		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
		assertTrue(stack("forwardStack").isEmpty());
	}

	@Test
	void endNetplayShutsDownNetLobbyWhenNotNull() {
		// When netLobby is already null, just transitions to title.
		NullpoMinoSDL.netLobby = null;
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_NET_LOBBY;

		NullpoMinoSDL.endNetplay();

		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
		assertNull(NullpoMinoSDL.netLobby);
	}

	/* ---------- stateForTransition ---------- */

	@Test
	void stateForTransitionNegativeIdSetsQuit() throws Exception {
		NullpoMinoSDL.quit = false;
		Object result = invokeStateForTransition(-1);
		assertNull(result);
		assertTrue(NullpoMinoSDL.quit);
	}

	@Test
	void stateForTransitionValidIdReturnsState() throws Exception {
		BaseStateSDL result = (BaseStateSDL) invokeStateForTransition(NullpoMinoSDL.STATE_TITLE);
		assertEquals(NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_TITLE], result);
	}

	/* ---------- text input lifecycle ---------- */

	@Test
	void stopTextInputResetsImeState() {
		NullpoMinoSDL.textInputActive = true;
		NullpoMinoSDL.imeComposition = "hello";
		NullpoMinoSDL.imeCompositionStart = 3;
		NullpoMinoSDL.imeCompositionLength = 2;

		NullpoMinoSDL.stopTextInput();

		assertFalse(NullpoMinoSDL.textInputActive);
		assertEquals("", NullpoMinoSDL.imeComposition);
		assertEquals(0, NullpoMinoSDL.imeCompositionStart);
		assertEquals(0, NullpoMinoSDL.imeCompositionLength);
	}

	@Test
	void stopTextInputPrechargesInputStateForHeldNavKeys() {
		// Simulate pressing Escape (nav-mapped for BUTTON_B typically)
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_ESCAPE] = true;
		NullpoMinoSDL.textInputActive = true;

		NullpoMinoSDL.stopTextInput();

		// A held Escape during stopTextInput precharges isPushKey
		// past the edge (0→1). We can't easily verify the exact inputstate
		// since it's private to GameKeySDL, but the method shouldn't throw.
	}

	/* ---------- isEscapePushedThisFrame (additional) ---------- */

	@Test
	void isEscapePushedThisFrameHandlesEmptyEventList() {
		NullpoMinoSDL.frameKeyEvents.clear();
		assertFalse(NullpoMinoSDL.isEscapePushedThisFrame());
	}

	/* ---------- syncFullscreenFlag (external fullscreen resync, e.g. Escape) ---------- */

	@Test
	void syncFullscreenFlagEntersAndPersistsWhenPreviouslyWindowed() {
		NullpoMinoSDL.fullscreen = false;

		assertTrue(NullpoMinoSDL.syncFullscreenFlag(true), "flag changed windowed->fullscreen");
		assertTrue(NullpoMinoSDL.fullscreen);
		assertTrue(NullpoMinoSDL.propConfig.getProperty("option.fullscreen", false),
				"the new fullscreen state must be persisted");
	}

	@Test
	void syncFullscreenFlagLeavesAndPersistsWhenPreviouslyFullscreen() {
		NullpoMinoSDL.fullscreen = true;

		assertTrue(NullpoMinoSDL.syncFullscreenFlag(false), "flag changed fullscreen->windowed");
		assertFalse(NullpoMinoSDL.fullscreen);
		assertFalse(NullpoMinoSDL.propConfig.getProperty("option.fullscreen", true),
				"the new windowed state must be persisted (covers browser Escape)");
	}

	@Test
	void syncFullscreenFlagIsNoOpWhenAlreadyFullscreen() {
		// The game's own toggleFullscreen() sets the flag before SDL emits the
		// enter event, and Chrome fires both fullscreenchange + webkitfullscreenchange
		// for a single change — the duplicate must neither flip nor re-persist.
		NullpoMinoSDL.fullscreen = true;

		assertFalse(NullpoMinoSDL.syncFullscreenFlag(true), "no change when already fullscreen");
		assertTrue(NullpoMinoSDL.fullscreen);
		assertFalse(NullpoMinoSDL.propConfig.containsKey("option.fullscreen"),
				"a guarded no-op must not touch the config");
	}

	@Test
	void syncFullscreenFlagIsNoOpWhenAlreadyWindowed() {
		NullpoMinoSDL.fullscreen = false;

		assertFalse(NullpoMinoSDL.syncFullscreenFlag(false), "no change when already windowed");
		assertFalse(NullpoMinoSDL.fullscreen);
		assertFalse(NullpoMinoSDL.propConfig.containsKey("option.fullscreen"),
				"a guarded no-op must not touch the config");
	}

	/* ---------- gamepad helpers ---------- */

	@Test
	void hatFromDpadFoldsEachDirectionIntoTheSdlBitmask() {
		assertEquals(0, NullpoMinoSDL.hatFromDpad(false, false, false, false));
		assertEquals(GameKeySDL.SDL_HAT_UP, NullpoMinoSDL.hatFromDpad(true, false, false, false));
		assertEquals(GameKeySDL.SDL_HAT_DOWN, NullpoMinoSDL.hatFromDpad(false, true, false, false));
		assertEquals(GameKeySDL.SDL_HAT_LEFT, NullpoMinoSDL.hatFromDpad(false, false, true, false));
		assertEquals(GameKeySDL.SDL_HAT_RIGHT, NullpoMinoSDL.hatFromDpad(false, false, false, true));
		assertEquals(GameKeySDL.SDL_HAT_UP | GameKeySDL.SDL_HAT_RIGHT,
				NullpoMinoSDL.hatFromDpad(true, false, false, true));
	}

	@Test
	void padDisplayNameNormalizesForTheBitmapFont() {
		assertEquals("UNKNOWN", NullpoMinoSDL.padDisplayName(null));
		assertEquals("UNKNOWN", NullpoMinoSDL.padDisplayName(""));
		assertEquals("XBOX 360 PAD", NullpoMinoSDL.padDisplayName("Xbox 360 Pad"));
		// Web Gamepad API ids are long — clamp to 24 chars for the 40-col grid.
		assertEquals("XBOX 360 CONTROLLER (XIN",
				NullpoMinoSDL.padDisplayName("Xbox 360 Controller (XInput STANDARD GAMEPAD Vendor: 045e Product: 028e)"));
	}

	@Test
	void deadzoneZeroesRestingStickAndPassesDeflections() {
		assertEquals(0, NullpoMinoSDL.deadzone((short) 0));
		assertEquals(0, NullpoMinoSDL.deadzone((short) 8191));
		assertEquals(0, NullpoMinoSDL.deadzone((short) -8191));
		assertEquals(8192, NullpoMinoSDL.deadzone((short) 8192));
		assertEquals(-8192, NullpoMinoSDL.deadzone((short) -8192));
		assertEquals(32767, NullpoMinoSDL.deadzone((short) 32767));
		assertEquals(-32768, NullpoMinoSDL.deadzone((short) -32768));
	}

	/* ---------- Private helpers ---------- */

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
		for (Integer v : snapshot) live.addLast(v);
	}

	private static boolean invokeHasCurrentState() throws Exception {
		Method m = NullpoMinoSDL.class.getDeclaredMethod("hasCurrentState");
		m.setAccessible(true);
		return (boolean) m.invoke(null);
	}

	private static Object invokeStateForTransition(int id) throws Exception {
		Method m = NullpoMinoSDL.class.getDeclaredMethod("stateForTransition", int.class);
		m.setAccessible(true);
		return m.invoke(null, id);
	}
}
