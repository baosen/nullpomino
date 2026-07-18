package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.gui.sdl.widget.ButtonSDL;

/**
 * Headless branch-gap coverage for {@link DummyMenuChooseStateSDL}: the
 * full {@code update()} input matrix (keyboard cursor wrap, page events,
 * left/right change, decide/cancel/D short-circuits, mouse forward/back),
 * the real {@code updateMouseInput()} hover + click hit-testing, and the
 * render paths (close button, renderChoices). The SDL binding is a stub
 * backend whose mouse state is driven by static fields.
 */
class DummyMenuChooseStateSDLBranchGapTest {

	private static final SdlRenderer RENDERER = new SdlRenderer() {};
	private static final SdlTexture TEXTURE = new SdlTexture() {};

	private static int mouseX;
	private static int mouseY;
	private static int mouseButtons;
	private static int renderTextureCalls;

	@BeforeAll
	static void stubSdlBackend() {
		SdlBackend.set(new SdlBackend.Backend() {
			public SDL3 sdl3() { return sdlStub(); }
			public SDL3Image image() { return stub(SDL3Image.class); }
			public SDL3TTF ttf() { return stub(SDL3TTF.class); }
			public SDL3Mixer mixerOrNull() { return null; }
		});
	}

	private SdlRenderer origRenderer;
	private BaseStateSDL[] origStates;
	private int origCurrent;
	private boolean origQuit;
	private boolean[] origKeyPressed;
	private float origWheel;
	private GameKeySDL[] origGamekey;
	private MouseInputSDL origMouse;
	private SoundManagerSDL origSound;
	private SdlTexture origImgFont, origImgFontSmall, origImgFontBig, origImgMenu;
	private Deque<Integer> origBack, origForward;

	private TestKey key;

	@BeforeEach
	void setUp() throws Exception {
		origRenderer = NullpoMinoSDL.renderer;
		origStates = NullpoMinoSDL.gameStates;
		origCurrent = NullpoMinoSDL.currentState;
		origQuit = NullpoMinoSDL.quit;
		origKeyPressed = NullpoMinoSDL.keyPressedState;
		origWheel = NullpoMinoSDL.mouseWheelDelta;
		origGamekey = GameKeySDL.gamekey;
		origMouse = MouseInputSDL.mouseInput;
		origSound = ResourceHolderSDL.soundManager;
		origImgFont = ResourceHolderSDL.imgFont;
		origImgFontSmall = ResourceHolderSDL.imgFontSmall;
		origImgFontBig = ResourceHolderSDL.imgFontBig;
		origImgMenu = ResourceHolderSDL.imgMenu;
		origBack = snapshot("backStack");
		origForward = snapshot("forwardStack");

		NullpoMinoSDL.renderer = RENDERER;
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.mouseWheelDelta = 0;
		PageNavigationSDL.checkPageEvent(); // sync edge detector to "released"
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
		ResourceHolderSDL.imgFont = TEXTURE;
		ResourceHolderSDL.imgFontSmall = TEXTURE;
		ResourceHolderSDL.imgFontBig = TEXTURE;
		ResourceHolderSDL.imgMenu = TEXTURE;

		key = new TestKey();
		GameKeySDL.gamekey = new GameKeySDL[] {key, new TestKey()};

		MouseInputSDL.initalizeMouseInput();
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		MouseInputSDL.mouseInput.update(); // settle prev == current

		renderTextureCalls = 0;
	}

	@AfterEach
	void tearDown() throws Exception {
		NullpoMinoSDL.renderer = origRenderer;
		NullpoMinoSDL.gameStates = origStates;
		NullpoMinoSDL.currentState = origCurrent;
		NullpoMinoSDL.quit = origQuit;
		NullpoMinoSDL.keyPressedState = origKeyPressed;
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.mouseWheelDelta = origWheel;
		GameKeySDL.gamekey = origGamekey;
		MouseInputSDL.mouseInput = origMouse;
		ResourceHolderSDL.soundManager = origSound;
		ResourceHolderSDL.imgFont = origImgFont;
		ResourceHolderSDL.imgFontSmall = origImgFontSmall;
		ResourceHolderSDL.imgFontBig = origImgFontBig;
		ResourceHolderSDL.imgMenu = origImgMenu;
		restore("backStack", origBack);
		restore("forwardStack", origForward);
	}

	/* ---------- update(): keyboard cursor movement ---------- */

	@Test
	void cursorUpWrapsFromZeroToMaxCursor() {
		ChooseStub state = new ChooseStub(4);
		key.press(GameKeySDL.BUTTON_UP);
		state.update();
		assertEquals(4, state.cursor);
	}

	@Test
	void cursorUpDecrementsWithoutWrap() {
		ChooseStub state = new ChooseStub(4);
		state.cursor = 3;
		key.press(GameKeySDL.BUTTON_UP);
		state.update();
		assertEquals(2, state.cursor);
	}

	@Test
	void cursorDownWrapsPastMaxCursor() {
		ChooseStub state = new ChooseStub(4);
		state.cursor = 4;
		key.press(GameKeySDL.BUTTON_DOWN);
		state.update();
		assertEquals(0, state.cursor);
	}

	@Test
	void cursorDownIncrementsWithoutWrap() {
		ChooseStub state = new ChooseStub(4);
		state.cursor = 1;
		key.press(GameKeySDL.BUTTON_DOWN);
		state.update();
		assertEquals(2, state.cursor);
	}

	@Test
	void noKeysLeavesCursorAlone() {
		ChooseStub state = new ChooseStub(4);
		state.cursor = 2;
		state.update();
		assertEquals(2, state.cursor);
		assertEquals(0, state.decideCalls);
		assertEquals(0, state.cancelCalls);
	}

	@Test
	void maxCursorNegativeSkipsCursorBlock() {
		ChooseStub state = new ChooseStub(-1);
		key.press(GameKeySDL.BUTTON_UP);
		key.press(GameKeySDL.BUTTON_A);
		state.update();
		assertEquals(0, state.cursor, "cursor block must be skipped when maxCursor < 0");
		assertEquals(0, state.decideCalls, "decide check lives inside the maxCursor block");
	}

	/* ---------- update(): page events ---------- */

	@Test
	void pageUpEventJumpsCursorToTop() {
		ChooseStub state = new ChooseStub(9);
		state.cursor = 5;
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_PAGEUP] = true;
		state.update();
		assertEquals(-1, state.lastPageEvent);
		assertEquals(0, state.cursor);
	}

	@Test
	void pageDownEventJumpsCursorToBottom() {
		ChooseStub state = new ChooseStub(9);
		state.cursor = 5;
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_PAGEDOWN] = true;
		state.update();
		assertEquals(1, state.lastPageEvent);
		assertEquals(9, state.cursor);
	}

	/* ---------- update(): left/right change ---------- */

	@Test
	void leftKeyFiresOnChangeMinusOne() {
		ChooseStub state = new ChooseStub(4);
		key.press(GameKeySDL.BUTTON_LEFT);
		state.update();
		assertEquals(-1, state.lastChange);
		assertEquals(1, state.changeCalls);
	}

	@Test
	void rightKeyFiresOnChangePlusOne() {
		ChooseStub state = new ChooseStub(4);
		key.press(GameKeySDL.BUTTON_RIGHT);
		state.update();
		assertEquals(1, state.lastChange);
		assertEquals(1, state.changeCalls);
	}

	/* ---------- update(): decide ---------- */

	@Test
	void buttonADecideTrueShortCircuitsRestOfUpdate() {
		ChooseStub state = new ChooseStub(4);
		state.decideResult = true;
		state.buttonDResult = true;
		key.press(GameKeySDL.BUTTON_A);
		key.press(GameKeySDL.BUTTON_D);
		state.update();
		assertEquals(1, state.decideCalls);
		assertEquals(0, state.dCalls, "onDecide()==true must return before the D check");
	}

	@Test
	void buttonADecideFalseFallsThroughToButtonD() {
		ChooseStub state = new ChooseStub(4);
		key.press(GameKeySDL.BUTTON_A);
		key.press(GameKeySDL.BUTTON_D);
		state.update();
		assertEquals(1, state.decideCalls);
		assertEquals(1, state.dCalls, "onDecide()==false must fall through");
	}

	@Test
	void mouseConfirmAloneTriggersDecide() {
		ChooseStub state = new ChooseStub(4);
		state.mouseConfirmNext = true;
		state.update();
		assertEquals(1, state.decideCalls);
	}

	/* ---------- update(): button D ---------- */

	@Test
	void buttonDTrueShortCircuitsCancelCheck() {
		ChooseStub state = new ChooseStub(4);
		state.buttonDResult = true;
		key.press(GameKeySDL.BUTTON_D);
		key.press(GameKeySDL.BUTTON_B);
		state.update();
		assertEquals(1, state.dCalls);
		assertEquals(0, state.cancelCalls, "onPushButtonD()==true must return early");
	}

	/* ---------- update(): cancel sources ---------- */

	@Test
	void buttonBTriggersCancel() {
		ChooseStub state = new ChooseStub(4);
		key.press(GameKeySDL.BUTTON_B);
		state.update();
		assertEquals(1, state.cancelCalls);
	}

	@Test
	void escapeKeyEventTriggersCancel() {
		ChooseStub state = new ChooseStub(4);
		NullpoMinoSDL.frameKeyEvents.add(
				new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		state.update();
		assertEquals(1, state.cancelCalls);
	}

	@Test
	void rightClickTriggersCancel() {
		ChooseStub state = new ChooseStub(4);
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_RMASK);
		MouseInputSDL.mouseInput.update();
		state.update();
		assertEquals(1, state.cancelCalls);
	}

	@Test
	void mouseBackButtonTriggersCancel() {
		ChooseStub state = new ChooseStub(4);
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X1MASK);
		MouseInputSDL.mouseInput.update();
		state.update();
		assertEquals(1, state.cancelCalls);
	}

	@Test
	void cancelTrueReturnsBeforeForwardNavigation() throws Exception {
		installNavStubs();
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		NullpoMinoSDL.goBack(); // forward stack now holds SELECTMODE

		ChooseStub state = new ChooseStub(4);
		state.cancelResult = true;
		key.press(GameKeySDL.BUTTON_B);
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X2MASK);
		MouseInputSDL.mouseInput.update();
		state.update();
		assertEquals(1, state.cancelCalls);
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState,
				"onCancel()==true must return before goForward()");
	}

	@Test
	void mouseForwardButtonReplaysForwardStack() throws Exception {
		installNavStubs();
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_SELECTMODE);
		NullpoMinoSDL.goBack(); // forward stack now holds SELECTMODE

		ChooseStub state = new ChooseStub(4);
		setMouse(-1, -1, SDLConstants.SDL_BUTTON_X2MASK);
		MouseInputSDL.mouseInput.update();
		state.update();
		assertEquals(NullpoMinoSDL.STATE_SELECTMODE, NullpoMinoSDL.currentState,
				"forward click must replay the back navigation");
	}

	/* ---------- update(): mouseEnabled and close button ---------- */

	@Test
	void mouseDisabledSkipsMouseInput() {
		ChooseStub state = new ChooseStub(4);
		state.mouseEnabled = false;
		state.update();
		assertEquals(0, state.mouseUpdateCalls);
	}

	@Test
	void mouseEnabledRunsMouseInput() {
		ChooseStub state = new ChooseStub(4);
		state.update();
		assertEquals(1, state.mouseUpdateCalls);
	}

	@Test
	void closeButtonClickFiresActionAndReturnsEarly() {
		ChooseStub state = new ChooseStub(4);
		state.overrideMouse = false;
		boolean[] fired = {false};
		state.closeBtn = ButtonSDL.newCloseButton(() -> fired[0] = true);
		key.press(GameKeySDL.BUTTON_A);

		settleMouseAt(600, 10);
		setMouse(600, 10, SDLConstants.SDL_BUTTON_LMASK);
		state.update();

		assertTrue(fired[0], "click inside the close button must run its action");
		assertEquals(0, state.decideCalls, "close button hit must short-circuit update()");
	}

	@Test
	void closeButtonMissFallsThroughToRestOfUpdate() {
		ChooseStub state = new ChooseStub(4);
		state.overrideMouse = false;
		boolean[] fired = {false};
		state.closeBtn = ButtonSDL.newCloseButton(() -> fired[0] = true);
		key.press(GameKeySDL.BUTTON_A);

		settleMouseAt(100, 100);
		state.update();

		assertFalse(fired[0]);
		assertEquals(1, state.decideCalls, "missed close button must not block the A press");
	}

	/* ---------- updateMouseInput(): hover ---------- */

	@Test
	void hoverMoveSlidesCursorToRowUnderPointer() {
		ChooseStub state = new ChooseStub(4);
		setMouse(100, 80, 0); // grid y=5, row = 5-3 = 2
		assertFalse(state.callRealMouse());
		assertEquals(2, state.cursor);
	}

	@Test
	void justEnteredSnapsCursorWithoutMovement() {
		ChooseStub state = new ChooseStub(4);
		settleMouseAt(100, 80);
		state.justEntered = true;
		assertFalse(state.callRealMouse());
		assertEquals(2, state.cursor, "entry snap must fire even without mouse movement");
	}

	@Test
	void stationaryPointerWithoutEntryLeavesCursor() {
		ChooseStub state = new ChooseStub(4);
		settleMouseAt(100, 80);
		state.cursor = 0;
		assertFalse(state.callRealMouse());
		assertEquals(0, state.cursor);
	}

	@Test
	void hoverAboveFirstChoiceIgnored() {
		ChooseStub state = new ChooseStub(4);
		setMouse(100, 40, 0); // grid y=2 -> newCursor=-1
		state.cursor = 1;
		assertFalse(state.callRealMouse());
		assertEquals(1, state.cursor);
	}

	@Test
	void hoverBelowLastChoiceIgnored() {
		ChooseStub state = new ChooseStub(4);
		setMouse(100, 200, 0); // grid y=12 -> newCursor=9 > maxCursor
		state.cursor = 1;
		assertFalse(state.callRealMouse());
		assertEquals(1, state.cursor);
	}

	@Test
	void hoverOverCurrentRowIsNoOp() {
		ChooseStub state = new ChooseStub(4);
		state.cursor = 2;
		setMouse(100, 80, 0); // row 2 == cursor
		assertFalse(state.callRealMouse());
		assertEquals(2, state.cursor);
	}

	/* ---------- updateMouseInput(): click ---------- */

	@Test
	void clickOnRowSelectsAndConfirms() {
		ChooseStub state = new ChooseStub(4);
		settleMouseAt(100, 80);
		setMouse(100, 80, SDLConstants.SDL_BUTTON_LMASK);
		assertTrue(state.callRealMouse());
		assertEquals(2, state.cursor);
	}

	@Test
	void clickAboveChoicesReturnsFalse() {
		ChooseStub state = new ChooseStub(4);
		settleMouseAt(100, 40);
		setMouse(100, 40, SDLConstants.SDL_BUTTON_LMASK);
		state.cursor = 1;
		assertFalse(state.callRealMouse());
		assertEquals(1, state.cursor);
	}

	@Test
	void clickBelowChoicesReturnsFalse() {
		ChooseStub state = new ChooseStub(4);
		settleMouseAt(100, 200);
		setMouse(100, 200, SDLConstants.SDL_BUTTON_LMASK);
		state.cursor = 1;
		assertFalse(state.callRealMouse());
		assertEquals(1, state.cursor);
	}

	/* ---------- render paths ---------- */

	@Test
	void renderWithoutCloseButtonDrawsNothing() {
		ChooseStub state = new ChooseStub(4);
		state.render();
		assertEquals(0, renderTextureCalls);
	}

	@Test
	void renderWithCloseButtonDrawsIt() {
		ChooseStub state = new ChooseStub(4);
		state.closeBtn = ButtonSDL.newCloseButton(null);
		state.render();
		assertTrue(renderTextureCalls > 0, "close button label must be drawn");
	}

	@Test
	void renderChoicesDrawsMarkerAndEveryChoice() {
		ChooseStub state = new ChooseStub(1);
		state.cursor = 0;
		state.renderChoices(2, new String[] {"A", "B"});
		// cursor marker "b" + one char per single-letter choice
		assertEquals(3, renderTextureCalls);
	}

	/* ---------- helpers ---------- */

	private void installNavStubs() {
		BaseStateSDL[] stubs = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < stubs.length; i++) stubs[i] = new BaseStateSDL();
		NullpoMinoSDL.gameStates = stubs;
		NullpoMinoSDL.currentState = -1;
		NullpoMinoSDL.quit = false;
	}

	/** Run two mouse updates at (x, y) so prev == current (isMouseMoved() == false). */
	private static void settleMouseAt(int x, int y) {
		setMouse(x, y, 0);
		MouseInputSDL.mouseInput.update();
		MouseInputSDL.mouseInput.update();
	}

	private static void setMouse(int x, int y, int buttons) {
		mouseX = x;
		mouseY = y;
		mouseButtons = buttons;
	}

	private static SDL3 sdlStub() {
		return (SDL3) Proxy.newProxyInstance(SDL3.class.getClassLoader(), new Class<?>[] {SDL3.class},
				(proxy, method, args) -> {
					if(method.getName().equals("SDL_GetMouseState")) {
						((FloatRef) args[0]).value = mouseX;
						((FloatRef) args[1]).value = mouseY;
						return mouseButtons;
					}
					if(method.getName().equals("SDL_RenderCoordinatesFromWindow")) {
						((FloatRef) args[3]).value = (Float) args[1];
						((FloatRef) args[4]).value = (Float) args[2];
						return (byte) 1;
					}
					if(method.getName().equals("SDL_RenderTexture")) {
						renderTextureCalls++;
					}
					return defaultValue(method.getReturnType());
				});
	}

	private static <T> T stub(Class<T> iface) {
		return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface},
				(proxy, method, args) -> defaultValue(method.getReturnType())));
	}

	private static Object defaultValue(Class<?> rt) {
		if(rt == byte.class) return (byte) 1;
		if(rt == short.class) return (short) 0;
		if(rt == int.class) return 0;
		if(rt == long.class) return 0L;
		if(rt == float.class) return 0f;
		if(rt == double.class) return 0d;
		if(rt == boolean.class) return false;
		return null;
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

	/** GameKeySDL whose input state is set directly instead of polled from SDL. */
	private static final class TestKey extends GameKeySDL {
		TestKey() { super(0); }
		void press(int button) { inputstate[button] = 1; }
	}

	/**
	 * Concrete choose-state. {@code overrideMouse} (default true) replaces
	 * {@code updateMouseInput()} with a canned result so update()-matrix tests
	 * don't depend on mouse geometry; {@link #callRealMouse()} exercises the
	 * real base implementation directly.
	 */
	private static final class ChooseStub extends DummyMenuChooseStateSDL {
		boolean overrideMouse = true;
		boolean mouseConfirmNext;
		boolean decideResult, cancelResult, buttonDResult;
		int decideCalls, cancelCalls, dCalls, changeCalls, mouseUpdateCalls;
		int lastChange, lastPageEvent;

		ChooseStub(int maxCursor) {
			this.maxCursor = maxCursor;
		}

		boolean callRealMouse() {
			return super.updateMouseInput();
		}

		@Override
		protected boolean updateMouseInput() {
			if(!overrideMouse) return super.updateMouseInput();
			mouseUpdateCalls++;
			return mouseConfirmNext;
		}

		@Override
		protected void onChange(int change) {
			lastChange = change;
			changeCalls++;
		}

		@Override
		protected void onPageEvent(int direction) {
			lastPageEvent = direction;
			super.onPageEvent(direction);
		}

		@Override
		protected boolean onDecide() {
			decideCalls++;
			return decideResult;
		}

		@Override
		protected boolean onCancel() {
			cancelCalls++;
			return cancelResult;
		}

		@Override
		protected boolean onPushButtonD() {
			dCalls++;
			return buttonDResult;
		}
	}
}
