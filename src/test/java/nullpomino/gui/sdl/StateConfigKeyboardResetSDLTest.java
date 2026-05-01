package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateConfigKeyboardResetSDLTest {

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
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_CONFIG_KEYBOARD_RESET;
		NullpoMinoSDL.quit = false;
		stack("backStack").clear();
		stack("forwardStack").clear();
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
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
	void constructorPinsThreePresetCursorAndMinChoiceYFour() throws Exception {
		StateConfigKeyboardResetSDL state = new StateConfigKeyboardResetSDL();

		assertEquals(0, state.player);
		Field maxCursor = DummyMenuChooseStateSDL.class.getDeclaredField("maxCursor");
		maxCursor.setAccessible(true);
		assertEquals(2, maxCursor.getInt(state),
				"three preset entries -> maxCursor pinned to 2 (Blockbox / Guideline / Classic)");
		Field minChoiceY = DummyMenuChooseStateSDL.class.getDeclaredField("minChoiceY");
		minChoiceY.setAccessible(true);
		assertEquals(4, minChoiceY.getInt(state));
	}

	@Test
	void onDecideAtEachCursorLoadsTheMatchingDefaultKeymapForThePlayer() throws Exception {
		stack("backStack").push(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		StateConfigKeyboardResetSDL state = new StateConfigKeyboardResetSDL();
		state.player = 1;

		// cursor 0 = Blockbox, cursor 1 = Guideline, cursor 2 = Classic.
		for(int cursor = 0; cursor <= 2; cursor++) {
			// Spoil the player's keymap so we can prove the reset writes it.
			java.util.Arrays.fill(GameKeySDL.gamekey[1].keymap, -123);
			java.util.Arrays.fill(GameKeySDL.gamekey[1].keymapNav, -456);

			setCursor(state, cursor);
			Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
			onDecide.setAccessible(true);
			Object handled = onDecide.invoke(state);

			assertEquals(Boolean.FALSE, handled);
			assertArrayEquals(GameKeySDL.DEFAULTKEYS[0][cursor], GameKeySDL.gamekey[1].keymap,
					"cursor=" + cursor + " ingame keymap");
			assertArrayEquals(GameKeySDL.DEFAULTKEYS[1][cursor], GameKeySDL.gamekey[1].keymapNav,
					"cursor=" + cursor + " menu keymap");
			// Cleanup the back-stack push the previous goBack consumed so the
			// next iteration has somewhere to land.
			stack("backStack").push(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		}
	}

	@Test
	void onDecideOnlyResetsTheKeymapOfThePlayerOnTheScreen() throws Exception {
		stack("backStack").push(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		StateConfigKeyboardResetSDL state = new StateConfigKeyboardResetSDL();
		state.player = 0;
		java.util.Arrays.fill(GameKeySDL.gamekey[1].keymap, 9999);

		setCursor(state, 0);
		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		onDecide.invoke(state);

		// Player 1's keymap stays untouched because the reset is per-player.
		for(int v : GameKeySDL.gamekey[1].keymap) assertEquals(9999, v);
	}

	@Test
	void onCancelDelegatesToGoBackAndReturnsFalse() throws Exception {
		stack("backStack").push(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		StateConfigKeyboardResetSDL state = new StateConfigKeyboardResetSDL();

		Method onCancel = DummyMenuChooseStateSDL.class.getDeclaredMethod("onCancel");
		onCancel.setAccessible(true);
		Object handled = onCancel.invoke(state);

		assertEquals(Boolean.FALSE, handled);
		assertEquals(NullpoMinoSDL.STATE_CONFIG_MAINMENU, NullpoMinoSDL.currentState);
	}

	private static void setCursor(StateConfigKeyboardResetSDL state, int cursor) throws Exception {
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
}
