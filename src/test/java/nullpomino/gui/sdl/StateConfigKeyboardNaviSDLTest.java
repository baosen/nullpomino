package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateConfigKeyboardNaviSDLTest {

	private BaseStateSDL[] originalStates;
	private int originalCurrent;
	private boolean originalQuit;
	private Deque<Integer> originalBack;
	private Deque<Integer> originalForward;
	private SoundManagerSDL originalSound;
	private GameKeySDL[] originalGameKey;
	private CustomProperties originalPropConfig;
	private CustomProperties originalPropGlobal;

	@BeforeEach
	void setUp() throws Exception {
		originalStates = NullpoMinoSDL.gameStates;
		originalCurrent = NullpoMinoSDL.currentState;
		originalQuit = NullpoMinoSDL.quit;
		originalBack = snapshot("backStack");
		originalForward = snapshot("forwardStack");
		originalSound = ResourceHolderSDL.soundManager;
		originalGameKey = GameKeySDL.gamekey;
		originalPropConfig = NullpoMinoSDL.propConfig;
		originalPropGlobal = NullpoMinoSDL.propGlobal;

		BaseStateSDL[] stubs = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < stubs.length; i++) stubs[i] = new BaseStateSDL();
		stubs[NullpoMinoSDL.STATE_CONFIG_KEYBOARD] = new KeyboardStub();
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_CONFIG_KEYBOARD_NAVI;
		NullpoMinoSDL.quit = false;
		stack("backStack").clear();
		stack("forwardStack").clear();
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
		// saveConfig() writes to disk and swallows IOException, so a fresh
		// CustomProperties is enough — the actual file write is best-effort.
		NullpoMinoSDL.propConfig = new CustomProperties();
		NullpoMinoSDL.propGlobal = new CustomProperties();
		GameKeySDL.initGlobalGameKeySDL();
	}

	@AfterEach
	void tearDown() throws Exception {
		NullpoMinoSDL.gameStates = originalStates;
		NullpoMinoSDL.currentState = originalCurrent;
		NullpoMinoSDL.quit = originalQuit;
		restore("backStack", originalBack);
		restore("forwardStack", originalForward);
		ResourceHolderSDL.soundManager = originalSound;
		GameKeySDL.gamekey = originalGameKey;
		NullpoMinoSDL.propConfig = originalPropConfig;
		NullpoMinoSDL.propGlobal = originalPropGlobal;
	}

	@Test
	void constructorInstallsTwoEntryCursorAndDefaultPlayerIndex() throws Exception {
		StateConfigKeyboardNaviSDL state = new StateConfigKeyboardNaviSDL();

		assertEquals(0, state.player);
		Field maxCursor = DummyMenuChooseStateSDL.class.getDeclaredField("maxCursor");
		maxCursor.setAccessible(true);
		assertEquals(1, maxCursor.getInt(state));
		Field minChoiceY = DummyMenuChooseStateSDL.class.getDeclaredField("minChoiceY");
		minChoiceY.setAccessible(true);
		assertEquals(3, minChoiceY.getInt(state));
	}

	@Test
	void getKeyNameReturnsScancodeNamesForInRangeKeys() throws Exception {
		StateConfigKeyboardNaviSDL state = new StateConfigKeyboardNaviSDL();

		assertEquals("A", invokeGetKeyName(state, SDLConstants.SDL_SCANCODE_A));
		assertEquals("ESCAPE", invokeGetKeyName(state, SDLConstants.SDL_SCANCODE_ESCAPE));
	}

	@Test
	void getKeyNameClampsNegativeAndOutOfRangeKeysToParenthesizedFallback() throws Exception {
		StateConfigKeyboardNaviSDL state = new StateConfigKeyboardNaviSDL();

		assertEquals("(-1)", invokeGetKeyName(state, -1));
		int oob = SDLConstants.SCANCODE_NAMES.length;
		assertEquals("(" + oob + ")", invokeGetKeyName(state, oob));
	}

	@Test
	void enterAndLeaveAreEmptyStubs() {
		StateConfigKeyboardNaviSDL state = new StateConfigKeyboardNaviSDL();

		// The framework calls enter()/leave() unconditionally on every state
		// transition; both must accept the call without side-effects.
		state.enter();
		state.leave();
	}

	@Test
	void onCancelDelegatesToGoBackAndReturnsFalse() throws Exception {
		// Push a known back-stack entry so goBack has somewhere to return to.
		stack("backStack").push(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		StateConfigKeyboardNaviSDL state = new StateConfigKeyboardNaviSDL();

		Method onCancel = DummyMenuChooseStateSDL.class.getDeclaredMethod("onCancel");
		onCancel.setAccessible(true);
		Object handled = onCancel.invoke(state);

		assertEquals(Boolean.FALSE, handled);
		assertEquals(NullpoMinoSDL.STATE_CONFIG_MAINMENU, NullpoMinoSDL.currentState);
	}

	@Test
	void onDecideCursorOneEntersKeyboardConfigInNavSettingMode() throws Exception {
		StateConfigKeyboardNaviSDL state = new StateConfigKeyboardNaviSDL();
		state.player = 1;
		setCursor(state, 1);

		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		Object handled = onDecide.invoke(state);

		assertEquals(Boolean.TRUE, handled, "cursor=1 path must short-circuit further menu input");
		assertEquals(NullpoMinoSDL.STATE_CONFIG_KEYBOARD, NullpoMinoSDL.currentState);
		KeyboardStub stub = (KeyboardStub) NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_KEYBOARD];
		assertEquals(1, stub.player);
		assertTrue(stub.isNavSetting);
	}

	@Test
	void onDecideCursorZeroCopiesGameKeymapIntoNavKeymapAndGoesBack() throws Exception {
		// Distinguish the two arrays so the copy is observable.
		for(int i = 0; i < GameKeySDL.MAX_BUTTON; i++) {
			GameKeySDL.gamekey[0].keymap[i] = 100 + i;
			GameKeySDL.gamekey[0].keymapNav[i] = -1;
		}
		assertNotEquals(GameKeySDL.gamekey[0].keymap[0], GameKeySDL.gamekey[0].keymapNav[0]);
		stack("backStack").push(NullpoMinoSDL.STATE_CONFIG_MAINMENU);

		StateConfigKeyboardNaviSDL state = new StateConfigKeyboardNaviSDL();
		state.player = 0;
		setCursor(state, 0);

		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		Object handled = onDecide.invoke(state);

		assertEquals(Boolean.TRUE, handled);
		assertArrayEquals(GameKeySDL.gamekey[0].keymap, GameKeySDL.gamekey[0].keymapNav);
		assertEquals(NullpoMinoSDL.STATE_CONFIG_MAINMENU, NullpoMinoSDL.currentState,
				"cursor=0 path must navigate back after copying the keymap");
		assertFalse(NullpoMinoSDL.quit);
	}

	private static String invokeGetKeyName(StateConfigKeyboardNaviSDL state, int key) throws Exception {
		Method m = StateConfigKeyboardNaviSDL.class.getDeclaredMethod("getKeyName", int.class);
		m.setAccessible(true);
		return (String) m.invoke(state, key);
	}

	private static void setCursor(StateConfigKeyboardNaviSDL state, int cursor) throws Exception {
		Field f = DummyMenuChooseStateSDL.class.getDeclaredField("cursor");
		f.setAccessible(true);
		f.setInt(state, cursor);
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

	private static final class KeyboardStub extends StateConfigKeyboardSDL {
		@Override public void enter() {}
	}
}
