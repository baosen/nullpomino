package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Deque;

import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StateSelectRuleFromListSDLTest {

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
		stubs[NullpoMinoSDL.STATE_INGAME] = new InGameStub();
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_SELECTRULEFROMLIST;
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
	void constructorLoadsBundledRecommendedRulesListIntoModeMap() throws Exception {
		StateSelectRuleFromListSDL state = new StateSelectRuleFromListSDL();

		Field pageHeightField = DummyMenuScrollStateSDL.class.getDeclaredField("pageHeight");
		pageHeightField.setAccessible(true);
		assertEquals(StateSelectRuleFromListSDL.PAGE_HEIGHT, pageHeightField.getInt(state));

		Field mapField = StateSelectRuleFromListSDL.class.getDeclaredField("mapRuleEntries");
		mapField.setAccessible(true);
		java.util.Map<?, ?> map = (java.util.Map<?, ?>) mapField.get(state);
		assertNotNull(map, "mapRuleEntries should be non-null after the constructor runs");
		// The bundled list ships with at least the MARATHON section.
		assertTrue(map.containsKey("MARATHON"),
				"bundled recommended_rules.lst defines a MARATHON section");
	}

	@Test
	void prepareRuleListWithUnknownModeFallsBackToCurrentRulePlaceholderOnly() throws Exception {
		StateSelectRuleFromListSDL state = new StateSelectRuleFromListSDL();
		NullpoMinoSDL.propGlobal.setProperty("name.mode", "MODE_THAT_DOES_NOT_EXIST");

		state.enter();

		String[] list = readList(state);
		assertEquals(1, list.length);
		assertEquals("(CURRENT RULE)", list[0]);
		assertEquals(0, readCursor(state));
	}

	@Test
	void prepareRuleListPrependsCurrentRulePlaceholderForKnownMode() throws Exception {
		StateSelectRuleFromListSDL state = new StateSelectRuleFromListSDL();
		NullpoMinoSDL.propGlobal.setProperty("name.mode", "MARATHON");

		state.enter();

		String[] list = readList(state);
		assertTrue(list.length >= 2,
				"a known mode in the bundled list must produce >1 entries");
		assertEquals("(CURRENT RULE)", list[0]);
		// Whatever the rules are, they must be non-null strings.
		for(int i = 1; i < list.length; i++) assertNotNull(list[i]);
	}

	@Test
	void prepareRuleListSeedsCursorFromLastRulePropertyForCurrentMode() throws Exception {
		StateSelectRuleFromListSDL state = new StateSelectRuleFromListSDL();
		NullpoMinoSDL.propGlobal.setProperty("name.mode", "MARATHON");
		// Discover the second entry's display name and pin it as the saved
		// last-rule for MARATHON, then re-enter to verify the cursor lands
		// on the matching slot.
		state.enter();
		String[] list = readList(state);
		if(list.length < 2) return; // bundled list always has ≥2; defensive.
		NullpoMinoSDL.propGlobal.setProperty("lastrule.MARATHON", list[1]);

		state.enter();

		assertEquals(1, readCursor(state));
	}

	@Test
	void onCancelDelegatesToGoBackAndDoesNotClaimTheInput() throws Exception {
		stack("backStack").push(NullpoMinoSDL.STATE_TITLE);
		StateSelectRuleFromListSDL state = new StateSelectRuleFromListSDL();

		Method onCancel = DummyMenuChooseStateSDL.class.getDeclaredMethod("onCancel");
		onCancel.setAccessible(true);
		Object handled = onCancel.invoke(state);

		assertEquals(Boolean.FALSE, handled);
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	@Test
	void onDecideAtCursorZeroSavesEmptyLastRuleAndStartsGameWithoutRulePath() throws Exception {
		StateSelectRuleFromListSDL state = new StateSelectRuleFromListSDL();
		NullpoMinoSDL.propGlobal.setProperty("name.mode", "MARATHON");
		state.enter();
		setCursor(state, 0);

		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		Object handled = onDecide.invoke(state);

		assertEquals(Boolean.FALSE, handled);
		assertEquals("", NullpoMinoSDL.propGlobal.getProperty("lastrule.MARATHON", "<missing>"));
		assertEquals(NullpoMinoSDL.STATE_INGAME, NullpoMinoSDL.currentState);
		InGameStub stub = (InGameStub) NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_INGAME];
		assertTrue(stub.startedWithNullPath, "cursor=0 must start a game with no rule override");
	}

	@Test
	void onDecideOnRecommendedRuleSavesItsNameAndPassesItsPathDownstream() throws Exception {
		StateSelectRuleFromListSDL state = new StateSelectRuleFromListSDL();
		NullpoMinoSDL.propGlobal.setProperty("name.mode", "MARATHON");
		state.enter();
		String[] list = readList(state);
		if(list.length < 2) return;
		setCursor(state, 1);

		Method onDecide = DummyMenuChooseStateSDL.class.getDeclaredMethod("onDecide");
		onDecide.setAccessible(true);
		onDecide.invoke(state);

		assertEquals(list[1], NullpoMinoSDL.propGlobal.getProperty("lastrule.MARATHON"));
		assertEquals(NullpoMinoSDL.STATE_INGAME, NullpoMinoSDL.currentState);
		InGameStub stub = (InGameStub) NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_INGAME];
		assertNotNull(stub.startedWithRulePath, "cursor>=1 must pass a non-null rule path");
		assertTrue(stub.startedWithRulePath.endsWith(".rul"),
				"the rule path must point at a .rul file");
	}

	private static String[] readList(StateSelectRuleFromListSDL state) throws Exception {
		Field f = DummyMenuScrollStateSDL.class.getDeclaredField("list");
		f.setAccessible(true);
		return (String[]) f.get(state);
	}

	private static int readCursor(StateSelectRuleFromListSDL state) throws Exception {
		Field f = DummyMenuChooseStateSDL.class.getDeclaredField("cursor");
		f.setAccessible(true);
		return f.getInt(state);
	}

	private static void setCursor(StateSelectRuleFromListSDL state, int cursor) throws Exception {
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

	private static final class InGameStub extends StateInGameSDL {
		boolean startedWithNullPath;
		String startedWithRulePath;

		@Override
		public void startNewGame(String strRulePath) {
			if(strRulePath == null) startedWithNullPath = true;
			else startedWithRulePath = strRulePath;
		}

		@Override public void enter() {}
	}
}
