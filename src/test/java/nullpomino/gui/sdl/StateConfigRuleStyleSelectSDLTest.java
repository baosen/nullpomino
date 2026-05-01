package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import nullpomino.game.play.GameEngine;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateConfigRuleStyleSelectSDLTest {

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
		stubs[NullpoMinoSDL.STATE_CONFIG_RULESELECT] = new RuleSelectStub();
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_CONFIG_RULESTYLESELECT;
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
	void constructorPinsMaxCursorToOneLessThanMaxGameStyleAndMinChoiceYToThree() throws Exception {
		StateConfigRuleStyleSelectSDL state = new StateConfigRuleStyleSelectSDL();

		assertEquals(0, state.player);
		Field maxCursor = DummyMenuChooseStateSDL.class.getDeclaredField("maxCursor");
		maxCursor.setAccessible(true);
		assertEquals(GameEngine.MAX_GAMESTYLE - 1, maxCursor.getInt(state));
		Field minChoiceY = DummyMenuChooseStateSDL.class.getDeclaredField("minChoiceY");
		minChoiceY.setAccessible(true);
		assertEquals(3, minChoiceY.getInt(state));
	}

	@Test
	void onDecidePropagatesPlayerAndCursorAsStyleIntoTheRuleSelectScreen() throws Exception {
		StateConfigRuleStyleSelectSDL state = new StateConfigRuleStyleSelectSDL();
		state.player = 1;
		setCursor(state, 2);

		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		Object handled = onDecide.invoke(state);

		assertEquals(Boolean.FALSE, handled);
		assertEquals(NullpoMinoSDL.STATE_CONFIG_RULESELECT, NullpoMinoSDL.currentState);
		StateConfigRuleSelectSDL ruleSelect = (StateConfigRuleSelectSDL)
				NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_RULESELECT];
		assertEquals(1, ruleSelect.player);
		assertEquals(2, ruleSelect.style);
	}

	@Test
	void onDecideAtCursorZeroSetsTetrominoStyle() throws Exception {
		StateConfigRuleStyleSelectSDL state = new StateConfigRuleStyleSelectSDL();
		state.player = 0;
		setCursor(state, 0);

		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		onDecide.invoke(state);

		StateConfigRuleSelectSDL ruleSelect = (StateConfigRuleSelectSDL)
				NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_RULESELECT];
		assertEquals(0, ruleSelect.style,
				"cursor 0 maps to TETROMINO via the GAMESTYLE_NAMES indexing");
	}

	@Test
	void onCancelDelegatesToGoBackAndReturnsFalse() throws Exception {
		stack("backStack").push(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		StateConfigRuleStyleSelectSDL state = new StateConfigRuleStyleSelectSDL();

		Method onCancel = DummyMenuChooseStateSDL.class.getDeclaredMethod("onCancel");
		onCancel.setAccessible(true);
		Object handled = onCancel.invoke(state);

		assertEquals(Boolean.FALSE, handled);
		assertEquals(NullpoMinoSDL.STATE_CONFIG_MAINMENU, NullpoMinoSDL.currentState);
	}

	private static void setCursor(StateConfigRuleStyleSelectSDL state, int cursor) throws Exception {
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

	private static final class RuleSelectStub extends StateConfigRuleSelectSDL {
		@Override public void enter() {}
	}
}
