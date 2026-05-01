package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateConfigMainMenuTest {

	private BaseStateSDL[] originalStates;
	private int originalCurrent;
	private boolean originalQuit;
	private Deque<Integer> originalBack;
	private Deque<Integer> originalForward;
	private SoundManagerSDL originalSound;

	@BeforeEach
	void setUp() throws Exception {
		originalStates = NullpoMinoSDL.gameStates;
		originalCurrent = NullpoMinoSDL.currentState;
		originalQuit = NullpoMinoSDL.quit;
		originalBack = snapshot("backStack");
		originalForward = snapshot("forwardStack");
		originalSound = ResourceHolderSDL.soundManager;

		BaseStateSDL[] stubs = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < stubs.length; i++) stubs[i] = new BaseStateSDL();
		stubs[NullpoMinoSDL.STATE_CONFIG_RULESTYLESELECT] = new RuleStyleStub();
		stubs[NullpoMinoSDL.STATE_CONFIG_GAMETUNING] = new GameTuningStub();
		stubs[NullpoMinoSDL.STATE_CONFIG_AISELECT] = new AISelectStub();
		stubs[NullpoMinoSDL.STATE_CONFIG_KEYBOARD] = new KeyboardStub();
		stubs[NullpoMinoSDL.STATE_CONFIG_KEYBOARD_NAVI] = new KeyboardNaviStub();
		stubs[NullpoMinoSDL.STATE_CONFIG_KEYBOARD_RESET] = new KeyboardResetStub();
		stubs[NullpoMinoSDL.STATE_CONFIG_JOYSTICK_MAIN] = new JoystickMainStub();

		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_CONFIG_MAINMENU;
		NullpoMinoSDL.quit = false;
		stack("backStack").clear();
		stack("forwardStack").clear();
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
	}

	@AfterEach
	void tearDown() throws Exception {
		NullpoMinoSDL.gameStates = originalStates;
		NullpoMinoSDL.currentState = originalCurrent;
		NullpoMinoSDL.quit = originalQuit;
		restore("backStack", originalBack);
		restore("forwardStack", originalForward);
		ResourceHolderSDL.soundManager = originalSound;
	}

	@Test
	void maxCursorCoversAllChoices() throws Exception {
		StateConfigMainMenuSDL state = new StateConfigMainMenuSDL();
		Field uiText = StateConfigMainMenuSDL.class.getDeclaredField("UI_TEXT");
		uiText.setAccessible(true);
		Field maxCursor = DummyMenuChooseStateSDL.class.getDeclaredField("maxCursor");
		maxCursor.setAccessible(true);

		assertEquals(((String[])uiText.get(null)).length - 1, maxCursor.getInt(state));
	}

	@Test
	void playerSelectionWrapsBetweenTwoPlayers() throws Exception {
		StateConfigMainMenuSDL state = new StateConfigMainMenuSDL();
		invokeOnChange(state, -1);
		assertEquals(1, state.player);

		invokeOnChange(state, 1);
		assertEquals(0, state.player);
	}

	@Test
	void onDecideRoutesGeneralOptionsWithoutPlayerMutation() throws Exception {
		invokeOnDecide(0, 1);

		assertEquals(NullpoMinoSDL.STATE_CONFIG_GENERAL, NullpoMinoSDL.currentState);
	}

	@Test
	void onDecidePropagatesPlayerToPlayerSpecificStates() throws Exception {
		invokeOnDecide(1, 1);
		assertEquals(1, ((StateConfigRuleStyleSelectSDL)NullpoMinoSDL.gameStates[
				NullpoMinoSDL.STATE_CONFIG_RULESTYLESELECT]).player);

		resetCurrent();
		invokeOnDecide(2, 1);
		assertEquals(1, ((StateConfigGameTuningSDL)NullpoMinoSDL.gameStates[
				NullpoMinoSDL.STATE_CONFIG_GAMETUNING]).player);

		resetCurrent();
		invokeOnDecide(3, 1);
		assertEquals(1, ((StateConfigAISelectSDL)NullpoMinoSDL.gameStates[
				NullpoMinoSDL.STATE_CONFIG_AISELECT]).player);

		resetCurrent();
		invokeOnDecide(5, 1);
		assertEquals(1, ((StateConfigKeyboardNaviSDL)NullpoMinoSDL.gameStates[
				NullpoMinoSDL.STATE_CONFIG_KEYBOARD_NAVI]).player);

		resetCurrent();
		invokeOnDecide(6, 1);
		assertEquals(1, ((StateConfigKeyboardResetSDL)NullpoMinoSDL.gameStates[
				NullpoMinoSDL.STATE_CONFIG_KEYBOARD_RESET]).player);

		resetCurrent();
		invokeOnDecide(7, 1);
		assertEquals(1, ((StateConfigJoystickMainSDL)NullpoMinoSDL.gameStates[
				NullpoMinoSDL.STATE_CONFIG_JOYSTICK_MAIN]).player);
	}

	@Test
	void keyboardSettingRouteSelectsGameKeyConfiguration() throws Exception {
		invokeOnDecide(4, 1);
		StateConfigKeyboardSDL keyboard = (StateConfigKeyboardSDL)NullpoMinoSDL.gameStates[
				NullpoMinoSDL.STATE_CONFIG_KEYBOARD];

		assertEquals(NullpoMinoSDL.STATE_CONFIG_KEYBOARD, NullpoMinoSDL.currentState);
		assertEquals(1, keyboard.player);
		assertFalse(keyboard.isNavSetting);
	}

	private static void invokeOnDecide(int cursor, int player) throws Exception {
		StateConfigMainMenuSDL state = new StateConfigMainMenuSDL();
		state.player = player;
		Field cursorField = DummyMenuChooseStateSDL.class.getDeclaredField("cursor");
		cursorField.setAccessible(true);
		cursorField.setInt(state, cursor);

		Method onDecide = StateConfigMainMenuSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		onDecide.invoke(state);
	}

	private static void invokeOnChange(StateConfigMainMenuSDL state, int change) throws Exception {
		Method onChange = StateConfigMainMenuSDL.class.getDeclaredMethod("onChange", int.class);
		onChange.setAccessible(true);
		onChange.invoke(state, change);
	}

	private static void resetCurrent() {
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_CONFIG_MAINMENU;
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

	private static final class RuleStyleStub extends StateConfigRuleStyleSelectSDL {
		@Override public void enter() {}
	}
	private static final class GameTuningStub extends StateConfigGameTuningSDL {
		@Override public void enter() {}
	}
	private static final class AISelectStub extends StateConfigAISelectSDL {
		@Override public void enter() {}
	}
	private static final class KeyboardStub extends StateConfigKeyboardSDL {
		@Override public void enter() {}
	}
	private static final class KeyboardNaviStub extends StateConfigKeyboardNaviSDL {
		@Override public void enter() {}
	}
	private static final class KeyboardResetStub extends StateConfigKeyboardResetSDL {
		@Override public void enter() {}
	}
	private static final class JoystickMainStub extends StateConfigJoystickMainSDL {
		@Override public void enter() {}
	}
}
