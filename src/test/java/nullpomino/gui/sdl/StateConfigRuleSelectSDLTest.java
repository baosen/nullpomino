package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateConfigRuleSelectSDLTest {

	private BaseStateSDL[] originalStates;
	private int originalCurrent;
	private boolean originalQuit;
	private Deque<Integer> originalBack;
	private Deque<Integer> originalForward;
	private SoundManagerSDL originalSound;
	private CustomProperties originalPropGlobal;
	private CustomProperties originalPropConfig;

	@BeforeEach
	void setUp() throws Exception {
		originalStates = NullpoMinoSDL.gameStates;
		originalCurrent = NullpoMinoSDL.currentState;
		originalQuit = NullpoMinoSDL.quit;
		originalBack = snapshot("backStack");
		originalForward = snapshot("forwardStack");
		originalSound = ResourceHolderSDL.soundManager;
		originalPropGlobal = NullpoMinoSDL.propGlobal;
		originalPropConfig = NullpoMinoSDL.propConfig;

		BaseStateSDL[] stubs = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < stubs.length; i++) stubs[i] = new BaseStateSDL();
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_CONFIG_RULESELECT;
		NullpoMinoSDL.quit = false;
		stack("backStack").clear();
		stack("forwardStack").clear();
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
		NullpoMinoSDL.propGlobal = new CustomProperties();
		NullpoMinoSDL.propConfig = new CustomProperties();
	}

	@AfterEach
	void tearDown() throws Exception {
		NullpoMinoSDL.gameStates = originalStates;
		NullpoMinoSDL.currentState = originalCurrent;
		NullpoMinoSDL.quit = originalQuit;
		restore("backStack", originalBack);
		restore("forwardStack", originalForward);
		ResourceHolderSDL.soundManager = originalSound;
		NullpoMinoSDL.propGlobal = originalPropGlobal;
		NullpoMinoSDL.propConfig = originalPropConfig;
	}

	@Test
	void constructorInstallsScrollPageHeightAndErrorLabels() throws Exception {
		StateConfigRuleSelectSDL state = new StateConfigRuleSelectSDL();

		assertEquals(0, state.player);
		assertEquals(0, state.style);
		Field pageHeight = DummyMenuScrollStateSDL.class.getDeclaredField("pageHeight");
		pageHeight.setAccessible(true);
		assertEquals(StateConfigRuleSelectSDL.PAGE_HEIGHT, pageHeight.getInt(state));
		Field nullError = DummyMenuScrollStateSDL.class.getDeclaredField("nullError");
		nullError.setAccessible(true);
		assertEquals("RULE DIRECTORY NOT FOUND", nullError.get(state));
		Field emptyError = DummyMenuScrollStateSDL.class.getDeclaredField("emptyError");
		emptyError.setAccessible(true);
		assertEquals("NO RULE FILE", emptyError.get(state));
	}

	@Test
	void enterPopulatesRuleListsFromTheBundledConfigDirectory() throws Exception {
		// The Bazel sandbox makes config/rule available as a runfile, so
		// dir.list returns the bundled rule set rather than null.
		StateConfigRuleSelectSDL state = new StateConfigRuleSelectSDL();
		state.player = 0;
		state.style = 0;

		state.enter();

		Field listField = DummyMenuScrollStateSDL.class.getDeclaredField("list");
		listField.setAccessible(true);
		String[] list = (String[]) listField.get(state);
		assertNotNull(list, "config/rule should exist in the test sandbox");
		assertTrue(list.length > 0, "config/rule should contain at least one .rul");

		Field maxCursor = DummyMenuChooseStateSDL.class.getDeclaredField("maxCursor");
		maxCursor.setAccessible(true);
		assertEquals(list.length - 1, maxCursor.getInt(state));
	}

	@Test
	void enterSeedsCursorFromCurrentRulePropertyForStyleZero() throws Exception {
		StateConfigRuleSelectSDL state = new StateConfigRuleSelectSDL();
		state.player = 0;
		state.style = 0;
		// Pre-load to discover the rule list, then pick the second entry as
		// the property and re-enter to confirm the cursor lands on it.
		state.enter();
		Field listField = DummyMenuScrollStateSDL.class.getDeclaredField("list");
		listField.setAccessible(true);

		String[] firstList = (String[]) listField.get(state);
		if(firstList.length < 2) return; // suite ships ≥2 rules; defensive guard.

		// Look up the filename for cursor index 1 via the rule entries linked
		// list, since the visible list shows rule names not filenames.
		Field entriesField = StateConfigRuleSelectSDL.class.getDeclaredField("ruleEntries");
		entriesField.setAccessible(true);
		java.util.LinkedList<?> entries = (java.util.LinkedList<?>) entriesField.get(state);
		Object secondEntry = entries.get(1);
		Field filenameField = secondEntry.getClass().getDeclaredField("filename");
		filenameField.setAccessible(true);
		String secondFilename = (String) filenameField.get(secondEntry);

		NullpoMinoSDL.propGlobal.setProperty("0.rulefile", secondFilename);
		state.enter();

		Field cursorField = DummyMenuChooseStateSDL.class.getDeclaredField("cursor");
		cursorField.setAccessible(true);
		assertEquals(1, cursorField.getInt(state),
				"cursor should snap to the saved rule on re-entry");
	}

	@Test
	void onPushButtonDTogglesBetweenRuleNamesAndFilenames() throws Exception {
		StateConfigRuleSelectSDL state = new StateConfigRuleSelectSDL();
		state.enter();

		Field listField = DummyMenuScrollStateSDL.class.getDeclaredField("list");
		listField.setAccessible(true);
		Object names = listField.get(state);

		Method onPushD = DummyMenuChooseStateSDL.class.getDeclaredMethod("onPushButtonD");
		onPushD.setAccessible(true);

		Object handled = onPushD.invoke(state);
		assertEquals(Boolean.FALSE, handled,
				"D-toggle should not claim the press, letting other input run");
		Object filenames = listField.get(state);
		assertNotSame(names, filenames, "first D press swaps to filename list");

		onPushD.invoke(state);
		assertSame(names, listField.get(state), "second D press restores name list");
	}

	@Test
	void onCancelDelegatesToGoBackAndClaimsTheInputAsHandled() throws Exception {
		stack("backStack").push(NullpoMinoSDL.STATE_TITLE);
		StateConfigRuleSelectSDL state = new StateConfigRuleSelectSDL();

		Method onCancel = DummyMenuChooseStateSDL.class.getDeclaredMethod("onCancel");
		onCancel.setAccessible(true);
		Object handled = onCancel.invoke(state);

		assertEquals(Boolean.TRUE, handled);
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void onDecideStyleZeroWritesUnstyledPropertyKeysAndGoesBack() throws Exception {
		stack("backStack").push(NullpoMinoSDL.STATE_TITLE);
		StateConfigRuleSelectSDL state = new StateConfigRuleSelectSDL();
		state.player = 1;
		state.style = 0;
		state.enter();
		setCursor(state, 0);

		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		Object handled = onDecide.invoke(state);

		assertEquals(Boolean.TRUE, handled);
		assertNotNull(NullpoMinoSDL.propGlobal.getProperty("1.rule"));
		assertNotNull(NullpoMinoSDL.propGlobal.getProperty("1.rulefile"));
		assertNotNull(NullpoMinoSDL.propGlobal.getProperty("1.rulename"));
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void onDecideNonZeroStyleWritesStyleSuffixedKeys() throws Exception {
		stack("backStack").push(NullpoMinoSDL.STATE_TITLE);
		StateConfigRuleSelectSDL state = new StateConfigRuleSelectSDL();
		state.player = 0;
		state.style = 2;
		state.enter();
		// If no rules with style=2 exist in the bundled config, the cursor
		// would index into an empty list. Skip rather than fake a rule entry.
		Field listField = DummyMenuScrollStateSDL.class.getDeclaredField("list");
		listField.setAccessible(true);
		String[] list = (String[]) listField.get(state);
		if(list.length == 0) return;

		setCursor(state, 0);

		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		onDecide.invoke(state);

		assertNotNull(NullpoMinoSDL.propGlobal.getProperty("0.rule.2"));
		assertNotNull(NullpoMinoSDL.propGlobal.getProperty("0.rulefile.2"));
		assertNotNull(NullpoMinoSDL.propGlobal.getProperty("0.rulename.2"));
	}

	private static void setCursor(StateConfigRuleSelectSDL state, int cursor) throws Exception {
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
