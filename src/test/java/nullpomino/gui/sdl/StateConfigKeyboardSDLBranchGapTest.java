package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlTexture;
import nullpomino.util.CustomProperties;
import nullpomino.util.DataDir;

/**
 * Headless branch matrix for the render()/update() flow of
 * {@link StateConfigKeyboardSDL}: every cursor row render, the key-capture
 * mode, mouse hover/click row selection, UP/DOWN auto-repeat with wraparound,
 * page jumps, save-and-exit for both keymap and keymapNav, DELETE unbinding,
 * and every cancel/forward exit alternative.
 */
class StateConfigKeyboardSDLBranchGapTest {
	private static final SdlRenderer RENDERER = new SdlRenderer() {};
	private static final SdlTexture TEXTURE = new SdlTexture() {};
	private static int mouseX = -1, mouseY = -1, mouseButtons;

	@TempDir Path tempDir;
	private StateConfigKeyboardSDL state;

	@BeforeAll
	static void stubSdlBackend() {
		SdlBackend.set(new SdlBackend.Backend() {
			public SDL3 sdl3() { return sdlStub(); }
			public SDL3Image image() { return stub(SDL3Image.class); }
			public SDL3TTF ttf() { return stub(SDL3TTF.class); }
			public SDL3Mixer mixerOrNull() { return null; }
		});
	}

	@BeforeEach
	void setUp() throws Exception {
		DataDir.setRoot(tempDir.toString());
		Files.createDirectories(tempDir.resolve("config/setting"));
		NullpoMinoSDL.renderer = RENDERER;
		NullpoMinoSDL.propConfig = new CustomProperties();
		NullpoMinoSDL.propGlobal = new CustomProperties();
		NullpoMinoSDL.propLang = new CustomProperties();
		NullpoMinoSDL.propLangDefault = new CustomProperties();
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.quit = false;
		GameKeySDL.initGlobalGameKeySDL();
		NullpoMinoSDL.joystickMax = 0;
		NullpoMinoSDL.joyUseNumber = new int[] {-1, -1};
		ResourceHolderSDL.imgMenu = TEXTURE;
		ResourceHolderSDL.imgFont = TEXTURE;
		ResourceHolderSDL.imgFontSmall = TEXTURE;
		ResourceHolderSDL.imgFontBig = TEXTURE;
		ResourceHolderSDL.ttfFont = null;
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
		MouseInputSDL.initalizeMouseInput();
		setMouse(-1, -1, 0);
		MouseInputSDL.mouseInput.update();
		PageNavigationSDL.checkPageEvent(); // drain stale page-key edge state
		NullpoMinoSDL.gameStates = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		for(int i = 0; i < NullpoMinoSDL.gameStates.length; i++) NullpoMinoSDL.gameStates[i] = new NoopState();
		NullpoMinoSDL.currentState = -1;
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_CONFIG_MAINMENU); // backStack = [TITLE]
		state = new StateConfigKeyboardSDL();
		state.enter();
	}

	@AfterEach
	void tearDown() {
		DataDir.setRoot(System.getProperty("nullpomino.datadir", ""));
	}

	/* ---------- render ---------- */

	@Test
	void renderCoversEveryCursorRowNavModeAndKeySetPrompt() {
		for(int i = 0; i <= StateConfigKeyboardSDL.NUM_KEYS; i++) {
			state.keynum = i;
			state.render();
		}
		state.keyConfigRestFrame = 1;
		state.render(); // "PUSH KEY..." prompt

		state.isNavSetting = true;
		state.reset();
		state.render(); // nav header + nav row labels
		assertEquals(0, state.keynum);
	}

	/* ---------- update: gates ---------- */

	@Test
	void closeButtonClickGoesBackBeforeInputHandling() {
		state.keynum = 3;
		click(600, 10); // inside the top-right BACK button
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
		assertEquals(3, state.keynum); // early return: no menu handling ran
	}

	@Test
	void entryGateRearmsHoldCountersUntilKeyAcceptFrame() {
		state.upInputState = 9;
		state.downInputState = 9;
		press(SDLConstants.SDL_SCANCODE_UP, true);
		state.update(); // frame 0 < KEYACCEPTFRAME
		assertEquals(0, state.upInputState);
		assertEquals(0, state.downInputState);
		assertEquals(1, state.frame);
		press(SDLConstants.SDL_SCANCODE_UP, false);
	}

	/* ---------- key-capture mode ---------- */

	@Test
	void keySetModeCountsDownThenBindsFirstChangedScancode() {
		state.frame = 100;
		state.keyConfigRestFrame = 3;
		state.update(); // no key pressed -> countdown only
		assertEquals(2, state.keyConfigRestFrame);

		press(SDLConstants.SDL_SCANCODE_A, true);
		state.update(); // binds A to row 0 and leaves capture mode
		assertEquals(SDLConstants.SDL_SCANCODE_A, state.keymap[0]);
		assertEquals(0, state.keyConfigRestFrame);
		assertEquals(0, state.frame);
		press(SDLConstants.SDL_SCANCODE_A, false);
	}

	/* ---------- menu mode: mouse ---------- */

	@Test
	void hoverMovesCursorOnlyInsideRowRange() {
		hover(50, 133); // row 5
		assertEquals(5, state.keynum);
		hover(51, 133); // moved but same row == keynum
		assertEquals(5, state.keynum);
		hover(50, 32);  // row -1, below range
		assertEquals(5, state.keynum);
		hover(50, 322); // row 17, above range
		assertEquals(5, state.keynum);
	}

	@Test
	void clickOnRowActsAsEnterAndStartsKeyCapture() {
		click(50, 48); // row 0
		assertEquals(0, state.keynum);
		assertEquals(60 * 5, state.keyConfigRestFrame);
	}

	@Test
	void clickOutsideRowsDoesNotConfirm() {
		click(50, 322); // row 17
		assertEquals(0, state.keyConfigRestFrame);
		click(50, 32);  // row -1
		assertEquals(0, state.keyConfigRestFrame);
	}

	/* ---------- menu mode: keyboard cursor ---------- */

	@Test
	void upDownKeysMoveWrapAndThrottleWhileHeld() {
		press(SDLConstants.SDL_SCANCODE_UP, true);
		upd(); // 0 -> wraps to 16
		assertEquals(StateConfigKeyboardSDL.NUM_KEYS, state.keynum);
		upd(); // held for 2 frames: no auto-repeat yet
		assertEquals(StateConfigKeyboardSDL.NUM_KEYS, state.keynum);
		press(SDLConstants.SDL_SCANCODE_UP, false);
		upd(); // re-arm
		press(SDLConstants.SDL_SCANCODE_UP, true);
		upd(); // 16 -> 15, no wrap
		assertEquals(15, state.keynum);
		press(SDLConstants.SDL_SCANCODE_UP, false);
		press(SDLConstants.SDL_SCANCODE_DOWN, true);
		upd(); // 15 -> 16, no wrap
		assertEquals(StateConfigKeyboardSDL.NUM_KEYS, state.keynum);
		press(SDLConstants.SDL_SCANCODE_DOWN, false);
		upd(); // re-arm
		press(SDLConstants.SDL_SCANCODE_DOWN, true);
		upd(); // 16 -> wraps to 0
		assertEquals(0, state.keynum);
		press(SDLConstants.SDL_SCANCODE_DOWN, false);
	}

	@Test
	void pageKeysJumpToEndsAndResetFrame() {
		press(SDLConstants.SDL_SCANCODE_PAGEDOWN, true);
		upd();
		assertEquals(StateConfigKeyboardSDL.NUM_KEYS, state.keynum);
		assertEquals(1, state.frame); // jump reset frame to 0, then frame++
		press(SDLConstants.SDL_SCANCODE_PAGEDOWN, false);
		state.update(); // drain page edge while gate is closed
		press(SDLConstants.SDL_SCANCODE_PAGEUP, true);
		upd();
		assertEquals(0, state.keynum);
		press(SDLConstants.SDL_SCANCODE_PAGEUP, false);
	}

	/* ---------- save & exit ---------- */

	@Test
	void enterOnSaveExitPersistsKeymapAndGoesBack() {
		state.keynum = StateConfigKeyboardSDL.NUM_KEYS;
		state.keymap[0] = 55;
		press(SDLConstants.SDL_SCANCODE_RETURN, true);
		upd();
		press(SDLConstants.SDL_SCANCODE_RETURN, false);
		assertEquals(55, GameKeySDL.gamekey[0].keymap[0]);
		assertEquals(55, NullpoMinoSDL.propConfig.getProperty("key.p0.up", -1));
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
		assertTrue(Files.exists(tempDir.resolve("config/setting/sdl.cfg")));
	}

	@Test
	void enterOnSaveExitInNavModeWritesKeymapNav() {
		state.isNavSetting = true;
		state.reset();
		state.keynum = StateConfigKeyboardSDL.NUM_KEYS;
		state.keymap[0] = 66;
		press(SDLConstants.SDL_SCANCODE_RETURN, true);
		upd();
		press(SDLConstants.SDL_SCANCODE_RETURN, false);
		assertEquals(66, GameKeySDL.gamekey[0].keymapNav[0]);
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	/* ---------- delete ---------- */

	@Test
	void deleteClearsBindingOnlyWhenSetAndOnKeyRows() {
		state.keymap[0] = 7;
		press(SDLConstants.SDL_SCANCODE_DELETE, true);
		upd();
		assertEquals(-1, state.keymap[0]);
		upd(); // still held, already -1: no-op branch
		assertEquals(-1, state.keymap[0]);
		state.keynum = StateConfigKeyboardSDL.NUM_KEYS;
		upd(); // save row: keynum < NUM_KEYS guard short-circuits
		assertEquals(StateConfigKeyboardSDL.NUM_KEYS, state.keynum);
		press(SDLConstants.SDL_SCANCODE_DELETE, false);
	}

	/* ---------- cancel / forward exits ---------- */

	@Test
	void everyCancelAlternativeGoesBack() {
		assertGoesBack(() -> press(SDLConstants.SDL_SCANCODE_BACKSPACE, true));
		press(SDLConstants.SDL_SCANCODE_BACKSPACE, false);
		assertGoesBack(() -> press(SDLConstants.SDL_SCANCODE_ESCAPE, true));
		press(SDLConstants.SDL_SCANCODE_ESCAPE, false);
		assertGoesBack(() -> setMouse(50, 400, SDLConstants.SDL_BUTTON_X1MASK));
		setMouse(50, 400, 0);
		assertGoesBack(() -> setMouse(50, 400, SDLConstants.SDL_BUTTON_RMASK));
		setMouse(50, 400, 0);
	}

	@Test
	void forwardClickGoesForwardAfterABack() {
		press(SDLConstants.SDL_SCANCODE_ESCAPE, true);
		upd(); // goBack -> forwardStack = [CONFIG_MAINMENU]
		press(SDLConstants.SDL_SCANCODE_ESCAPE, false);
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);

		state = new StateConfigKeyboardSDL();
		state.enter();
		setMouse(50, 400, SDLConstants.SDL_BUTTON_X2MASK);
		upd();
		setMouse(50, 400, 0);
		assertEquals(NullpoMinoSDL.STATE_CONFIG_MAINMENU, NullpoMinoSDL.currentState);
	}

	/* ---------- helpers ---------- */

	private void assertGoesBack(Runnable arm) {
		NullpoMinoSDL.currentState = -1;
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		state = new StateConfigKeyboardSDL();
		state.enter();
		arm.run();
		upd();
		assertEquals(NullpoMinoSDL.STATE_TITLE, NullpoMinoSDL.currentState);
	}

	/** update() with the entry gate forced open. */
	private void upd() {
		state.frame = 100;
		state.update();
	}

	private void hover(int x, int y) {
		setMouse(x, y, 0);
		upd();
	}

	private void click(int x, int y) {
		hover(x, y);
		setMouse(x, y, SDLConstants.SDL_BUTTON_LMASK);
		upd();
		setMouse(x, y, 0);
	}

	private static void press(int scancode, boolean down) {
		NullpoMinoSDL.keyPressedState[scancode] = down;
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

	private static final class NoopState extends BaseStateSDL {
	}
}
