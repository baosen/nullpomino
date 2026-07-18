package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import nullpomino.game.play.GameEngine;
import nullpomino.gui.GameKeyDummy;
import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Image;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlBackend;
import nullpomino.gui.sdl.binding.SdlHandles.MixMixer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlGamepad;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;
import nullpomino.gui.sdl.binding.SdlHandles.SdlWindow;
import nullpomino.util.CustomProperties;
import nullpomino.util.DataDir;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Exact residual matrix for the SDL application shell and its native seams. */
class NullpoMinoSDLResidualBranchMatrixTest {

	private static final SdlWindow WINDOW = new SdlWindow() {};
	private static final SdlRenderer RENDERER = new SdlRenderer() {};
	private static final SdlSurface SURFACE = new SdlSurface() {};
	private static final SdlGamepad PAD_A = new SdlGamepad() {};
	private static final SdlGamepad PAD_B = new SdlGamepad() {};
	private static final MixMixer MIXER = new MixMixer() {};

	private static FakeSdl fake;
	private static SDL3 sdl;
	private static SDL3Mixer mixer;

	@TempDir Path tempDir;

	@BeforeAll
	static void installBackendBeforeBindingsInitialize() {
		fake = new FakeSdl();
		SdlBackend.Backend backend = new SdlBackend.Backend() {
			private SDL3Image image;
			private SDL3TTF ttf;
			@Override public SDL3 sdl3() {
				if(sdl == null) sdl = proxy(SDL3.class, fake);
				return sdl;
			}
			@Override public SDL3Image image() {
				if(image == null) image = proxy(SDL3Image.class,
					(proxy, method, args) -> defaultValue(method.getReturnType(), true));
				return image;
			}
			@Override public SDL3TTF ttf() {
				if(ttf == null) ttf = proxy(SDL3TTF.class,
					(proxy, method, args) -> defaultValue(method.getReturnType(), true));
				return ttf;
			}
			@Override public SDL3Mixer mixerOrNull() {
				if(mixer == null) mixer = proxy(SDL3Mixer.class, (proxy, method, args) -> {
					if(method.getName().equals("MIX_CreateMixerDevice"))
						return fake.mixerDeviceAvailable ? MIXER : null;
					return defaultValue(method.getReturnType(), true);
				});
				return fake.mixerEnabled ? mixer : null;
			}
		};
		SdlBackend.set(backend);
	}

	@BeforeEach
	void resetStaticApplicationState() throws Exception {
		fake.reset();
		DataDir.setRoot(tempDir.toString());
		Files.createDirectories(tempDir.resolve("config/setting"));

		NullpoMinoSDL.propConfig = new CustomProperties();
		NullpoMinoSDL.propGlobal = new CustomProperties();
		NullpoMinoSDL.propMusic = new CustomProperties();
		NullpoMinoSDL.propLang = new CustomProperties();
		NullpoMinoSDL.propLangDefault = new CustomProperties();
		NullpoMinoSDL.keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		setStatic("emptyKeyState", new boolean[SDLConstants.SDL_SCANCODE_COUNT]);
		setStatic("event", new SDLStructs.SDL_Event());
		setStatic("backStack", new ArrayDeque<Integer>());
		setStatic("forwardStack", new ArrayDeque<Integer>());

		GameKeySDL.initGlobalGameKeySDL();
		for(GameKeySDL key : GameKeySDL.gamekey) key.loadDefaultKeymap();
		NullpoMinoSDL.joyUseNumber = new int[]{-1, -1};
		NullpoMinoSDL.joystickMax = 0;
		NullpoMinoSDL.gamepad = new SdlGamepad[0];
		NullpoMinoSDL.joyName = new String[0];
		NullpoMinoSDL.joyAxisX = new int[0];
		NullpoMinoSDL.joyAxisY = new int[0];
		NullpoMinoSDL.joyHatState = new int[0];
		NullpoMinoSDL.joyPressedState = new boolean[0][0];
		NullpoMinoSDL.frameKeyEvents.clear();
		NullpoMinoSDL.pendingTextInput.setLength(0);
		NullpoMinoSDL.gameStates = new BaseStateSDL[NullpoMinoSDL.STATE_MAX];
		NullpoMinoSDL.currentState = -1;
		NullpoMinoSDL.quit = false;
		NullpoMinoSDL.webMode = false;
		NullpoMinoSDL.textInputActive = false;
		NullpoMinoSDL.disableAutoInputUpdate = false;
		NullpoMinoSDL.fullscreen = false;
		setStatic("prevF11Pressed", false);
		NullpoMinoSDL.window = WINDOW;
		NullpoMinoSDL.renderer = RENDERER;
		NullpoMinoSDL.mixerLib = null;
		NullpoMinoSDL.mixer = null;
		NullpoMinoSDL.netLobby = null;
		NullpoMinoSDL.roomSession = null;
		NullpoMinoSDL.resourceLoader = () -> {};
		NullpoMinoSDL.fpsRenderer = () -> fake.fpsRenders++;
		setClockStep(1_000_000L);
	}

	@AfterEach
	void restoreGlobalSeams() {
		NullpoMinoSDL.resourceLoader = ResourceHolderSDL::load;
		NullpoMinoSDL.fpsRenderer = () -> NormalFontSDL.printFont(0, 480 - 16,
			NullpoMinoSDL.df.format(NullpoMinoSDL.actualFPS), NormalFontSDL.COLOR_BLUE, 1.0f);
		NullpoMinoSDL.nanoTime = System::nanoTime;
		NullpoMinoSDL.netLobby = null;
		NullpoMinoSDL.roomSession = null;
		DataDir.setRoot("");
	}

	@Test
	void bootstrapSeedsMissingRulesAndPreservesExistingRules() throws Exception {
		write("config/lang/sdl_default.properties", "Title_Start=PLAY\n");
		write("config/lang/modedesc_default.properties", "mode=description\n");
		write("config/list/global_defaultrule.properties",
			"default.rule=DEFAULT\ndefault.rulefile=default.rul\ndefault.rulename=Default\n" +
			"default.rule.1=STYLE1\ndefault.rulefile.1=style1.rul\ndefault.rulename.1=Style1\n");

		NullpoMinoSDL.bootstrap(new String[]{"first"});
		assertEquals("DEFAULT", NullpoMinoSDL.propGlobal.getProperty("0.rule"));
		assertTrue(NullpoMinoSDL.propConfig.getProperty("option.sdl3KeyMigrated", false));

		StringBuilder global = new StringBuilder();
		for(int player = 0; player < 2; player++) {
			global.append(player).append(".rule=KEPT\n");
			for(int style = 1; style < GameEngine.MAX_GAMESTYLE; style++)
				global.append(player).append(".rule.").append(style).append("=KEPT\n");
		}
		write("config/setting/global.cfg", global.toString());
		write("config/setting/sdl.cfg", "option.sdl3KeyMigrated=true\n");
		NullpoMinoSDL.bootstrap(new String[]{"second"});

		assertEquals("KEPT", NullpoMinoSDL.propGlobal.getProperty("1.rule"));
		assertEquals("KEPT", NullpoMinoSDL.propGlobal.getProperty("1.rule.1"));
	}

	@Test
	void initAndGamepadLifecycleCoverFailureOptionalAndCompactionPaths() {
		fake.initResult = 0;
		assertThrows(RuntimeException.class, NullpoMinoSDL::init);

		fake.initResult = 1;
		fake.windowAvailable = false;
		assertThrows(RuntimeException.class, NullpoMinoSDL::init);
		fake.windowAvailable = true;
		fake.rendererAvailable = false;
		assertThrows(RuntimeException.class, NullpoMinoSDL::init);

		fake.rendererAvailable = true;
		fake.mixerEnabled = false;
		NullpoMinoSDL.propConfig.setProperty("option.fullscreen", false);
		NullpoMinoSDL.init();

		fake.mixerEnabled = true;
		fake.mixerDeviceAvailable = false;
		NullpoMinoSDL.propConfig.setProperty("option.fullscreen", true);
		NullpoMinoSDL.init();
		fake.mixerDeviceAvailable = true;
		NullpoMinoSDL.init();
		assertNotNull(NullpoMinoSDL.mixer);
		assertTrue(fake.lastWindowFlags != SDLConstants.SDL_WINDOW_RESIZABLE);

		fake.joystickIds = new int[]{1, 2, 3, 4};
		fake.mappedPads.addAll(Arrays.asList(2, 3, 4));
		fake.openPads.put(2, null);
		fake.openPads.put(3, PAD_A);
		fake.throwingPadIds.add(4);
		NullpoMinoSDL.initJoysticks();
		assertEquals(1, NullpoMinoSDL.joystickMax);
		assertEquals("PAD THREE", NullpoMinoSDL.joyName[0]);

		NullpoMinoSDL.gamepad = new SdlGamepad[]{PAD_A, null};
		NullpoMinoSDL.joystickMax = 2;
		NullpoMinoSDL.closeJoysticks();
		assertEquals(1, fake.closedPads);
		assertEquals(0, NullpoMinoSDL.joystickMax);
	}

	@Test
	void coordinateClipboardTextAndJoystickHelpersCoverNativeResultMatrices() {
		fake.coordinateSuccess = false;
		assertEquals(-1, NullpoMinoSDL.windowToLogicalX(10));
		assertEquals(-1, NullpoMinoSDL.windowToLogicalY(10));
		fake.coordinateSuccess = true;
		for(float x : new float[]{-1, 12, NullpoMinoSDL.LOGICAL_WIDTH}) {
			fake.logicalX = x;
			NullpoMinoSDL.windowToLogicalX(10);
		}
		for(float y : new float[]{-1, 12, NullpoMinoSDL.LOGICAL_HEIGHT}) {
			fake.logicalY = y;
			NullpoMinoSDL.windowToLogicalY(10);
		}

		fake.clipboard = null;
		assertEquals("", NullpoMinoSDL.getClipboardText());
		fake.clipboard = "clip";
		assertEquals("clip", NullpoMinoSDL.getClipboardText());
		NullpoMinoSDL.setClipboardText(null);
		NullpoMinoSDL.setClipboardText("copy");
		assertEquals("copy", fake.clipboard);

		NullpoMinoSDL.window = null;
		NullpoMinoSDL.startTextInput(null);
		NullpoMinoSDL.window = WINDOW;
		NullpoMinoSDL.startTextInput(null);
		NullpoMinoSDL.startTextInput(new SDLStructs.SDL_Rect());

		GameKeySDL.gamekey[0] = null;
		GameKeySDL.gamekey[1].keymapNav[0] = -1;
		GameKeySDL.gamekey[1].keymapNav[1] = NullpoMinoSDL.keyPressedState.length;
		GameKeySDL.gamekey[1].keymapNav[2] = SDLConstants.SDL_SCANCODE_A;
		GameKeySDL.gamekey[1].keymapNav[3] = SDLConstants.SDL_SCANCODE_B;
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_B] = true;
		NullpoMinoSDL.window = null;
		NullpoMinoSDL.stopTextInput();
		NullpoMinoSDL.window = WINDOW;
		NullpoMinoSDL.stopTextInput();
		assertEquals(2, GameKeySDL.gamekey[1].getInputState(3));

		NullpoMinoSDL.joystickMax = 2;
		NullpoMinoSDL.gamepad = new SdlGamepad[]{null, PAD_B};
		NullpoMinoSDL.joyAxisX = new int[2];
		NullpoMinoSDL.joyAxisY = new int[2];
		NullpoMinoSDL.joyHatState = new int[2];
		NullpoMinoSDL.joyPressedState = new boolean[2][SDLConstants.SDL_GAMEPAD_NUM_BUTTONS];
		fake.axisX = 100;
		fake.axisY = 9000;
		fake.pressedButton = SDLConstants.SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
		NullpoMinoSDL.joyUpdate();
		assertEquals(0, NullpoMinoSDL.joyAxisX[1]);
		assertEquals(9000, NullpoMinoSDL.joyAxisY[1]);
		assertTrue(NullpoMinoSDL.joyPressedState[1][fake.pressedButton]);
	}

	@Test
	void processEventConsumesEveryEventShapeAndHotplugPredicate() throws Exception {
		queue(event(SDLConstants.SDL_EVENT_QUIT));
		queue(keyEvent(SDLConstants.SDL_EVENT_KEY_DOWN, -1));
		queue(keyEvent(SDLConstants.SDL_EVENT_KEY_DOWN, NullpoMinoSDL.keyPressedState.length));
		queue(keyEvent(SDLConstants.SDL_EVENT_KEY_DOWN, SDLConstants.SDL_SCANCODE_A));
		queue(keyEvent(SDLConstants.SDL_EVENT_KEY_UP, -1));
		queue(keyEvent(SDLConstants.SDL_EVENT_KEY_UP, NullpoMinoSDL.keyPressedState.length));
		queue(keyEvent(SDLConstants.SDL_EVENT_KEY_UP, SDLConstants.SDL_SCANCODE_A));
		SDLStructs.SDL_Event text = event(SDLConstants.SDL_EVENT_TEXT_INPUT);
		text.text = "hello";
		queue(text);
		SDLStructs.SDL_Event editing = event(SDLConstants.SDL_EVENT_TEXT_EDITING);
		editing.text = "pre";
		editing.editingStart = 2;
		editing.editingLength = 3;
		queue(editing);
		SDLStructs.SDL_Event wheel = event(SDLConstants.SDL_EVENT_MOUSE_WHEEL);
		wheel.wheelY = 2;
		queue(wheel);
		queue(event(SDLConstants.SDL_EVENT_WINDOW_ENTER_FULLSCREEN));
		queue(event(SDLConstants.SDL_EVENT_WINDOW_ENTER_FULLSCREEN));
		queue(event(SDLConstants.SDL_EVENT_WINDOW_LEAVE_FULLSCREEN));
		queue(event(SDLConstants.SDL_EVENT_WINDOW_LEAVE_FULLSCREEN));
		queue(event(SDLConstants.SDL_EVENT_JOYSTICK_ADDED));
		queue(event(SDLConstants.SDL_EVENT_JOYSTICK_REMOVED));
		queue(event(0x7fffffff));

		NullpoMinoSDL.processEvent();

		assertTrue(NullpoMinoSDL.quit);
		assertEquals("hello", NullpoMinoSDL.pendingTextInput.toString());
		assertEquals("pre", NullpoMinoSDL.imeComposition);
		assertEquals(2f, NullpoMinoSDL.mouseWheelDelta);
		assertFalse(NullpoMinoSDL.fullscreen);
	}

	@Test
	void screenshotShutdownAndSessionCleanupCoverNullableResources() throws Exception {
		Files.createDirectories(tempDir.resolve("existing"));
		NullpoMinoSDL.propGlobal.setProperty("custom.screenshot.directory", "existing");
		fake.surfaceAvailable = false;
		NullpoMinoSDL.saveScreenShot();
		NullpoMinoSDL.propGlobal.setProperty("custom.screenshot.directory", "created");
		fake.surfaceAvailable = true;
		NullpoMinoSDL.saveScreenShot();
		NullpoMinoSDL.propGlobal.setProperty("custom.screenshot.directory", "missing/child");
		fake.surfaceAvailable = false;
		NullpoMinoSDL.saveScreenShot();
		assertTrue(fake.savedBmps > 0);

		setUninitializedStatic("roomSession");
		NullpoMinoSDL.stopRoomSession();
		assertNull(NullpoMinoSDL.roomSession);

		NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_TITLE] = new CountingState(null);
		setUninitializedStatic("netLobby");
		NullpoMinoSDL.endNetplay();
		assertNull(NullpoMinoSDL.netLobby);

		setUninitializedStatic("netLobby");
		setUninitializedStatic("roomSession");
		NullpoMinoSDL.gamepad = new SdlGamepad[]{PAD_A, null};
		NullpoMinoSDL.joystickMax = 2;
		NullpoMinoSDL.renderer = RENDERER;
		NullpoMinoSDL.window = WINDOW;
		fake.mixerEnabled = true;
		SDL3Mixer liveMixer = SDL3Mixer.loadOrNull();
		assertNotNull(liveMixer);
		NullpoMinoSDL.mixerLib = liveMixer;
		NullpoMinoSDL.mixer = MIXER;
		NullpoMinoSDL.shutdown();

		NullpoMinoSDL.mixerLib = liveMixer;
		NullpoMinoSDL.mixer = null;
		NullpoMinoSDL.shutdown();
		NullpoMinoSDL.mixerLib = null;
		NullpoMinoSDL.shutdown();
		assertTrue(fake.closedPads > 0);
	}

	@Test
	void navigationResidualsCoverNullUnsafeAndInvalidTransitions() throws Exception {
		CountingState plain = new CountingState(null);
		MenuState menu = new MenuState();
		NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_TITLE] = plain;
		NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_CONFIG_MAINMENU] = menu;

		invoke("doTransition", new Class<?>[]{int.class}, -1);
		assertThrows(NullPointerException.class,
			() -> NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_MAX));
		assertThrows(NullPointerException.class,
			() -> NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_GENERAL));
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_TITLE);
		NullpoMinoSDL.enterState(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		assertTrue(menu.justEntered);

		Deque<Integer> back = deque("backStack");
		Deque<Integer> forward = deque("forwardStack");
		back.clear();
		NullpoMinoSDL.webMode = true;
		NullpoMinoSDL.goBack();
		NullpoMinoSDL.webMode = false;
		NullpoMinoSDL.goBack();
		back.push(NullpoMinoSDL.STATE_TITLE);
		NullpoMinoSDL.currentState = NullpoMinoSDL.STATE_INGAME;
		NullpoMinoSDL.goBack();

		forward.clear();
		NullpoMinoSDL.goForward();
		forward.push(NullpoMinoSDL.STATE_NETGAME);
		NullpoMinoSDL.goForward();
		forward.push(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		NullpoMinoSDL.goForward();
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_TITLE);
		NullpoMinoSDL.enterStateClear(NullpoMinoSDL.STATE_CONFIG_MAINMENU);
		assertTrue(plain.leaves > 0);
	}

	@Test
	void runCoversFirstSetupInputSpecialKeyAndTimingMatrices() throws Exception {
		NullpoMinoSDL.fullscreen = false;
		NullpoMinoSDL.toggleFullscreen();
		NullpoMinoSDL.toggleFullscreen();

		NullpoMinoSDL.propConfig.setProperty("option.firstSetupMode", true);
		NullpoMinoSDL.propGlobal.setProperty("1.tuning.owRotateButtonDefaultRight", 1);
		NullpoMinoSDL.propGlobal.setProperty("global.firstSetupMode", true);
		runFrames(1, 0, false, false, false, 1_000_000L, null);

		NullpoMinoSDL.propConfig.setProperty("option.firstSetupMode", true);
		NullpoMinoSDL.propGlobal.setProperty("global.firstSetupMode", false);
		runFrames(1, 60, false, false, false, 1_000_000L, null);
		NullpoMinoSDL.propConfig.setProperty("option.showfps", true);
		runFrames(1, 60, false, false, false, 1_000_000L, null);
		assertTrue(fake.fpsRenders > 0);
		NullpoMinoSDL.textInputActive = true;
		NullpoMinoSDL.disableAutoInputUpdate = false;
		runFrames(1, 60, false, false, false, 1_000_000L, null);

		runFrames(1, 60, true, true, true, 1_000_000L, null);
		runFrames(1, 1_000_000_000, false, true, false, 1_000_000L, null);
		runFrames(1, 60, true, false, false, 1_000_000L, null);
		NullpoMinoSDL.propConfig.setProperty("option.perfectYield", true);
		runFrames(1, 60, true, false, true, 1_000_000L, null);
		NullpoMinoSDL.propConfig.setProperty("option.perfectYield", false);
		runFrames(1, 60, true, false, true, 50_000_000L, null);
		runFrames(1, 400, false, false, false, 500_000L, null);
		runFrames(1, 400, true, false, false, 500_000L, null);

		NullpoMinoSDL.disableAutoInputUpdate = true;
		NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_F11] = true;
		setStatic("prevF11Pressed", false);
		runFrames(17, 1_000_000_000, false, false, false, 1_000_000L, frame -> {
			if(frame == 1) {
				NullpoMinoSDL.keyPressedState[SDLConstants.SDL_SCANCODE_F11] = true;
				NullpoMinoSDL.enableSpecialKeys = false;
				NullpoMinoSDL.allowQuit = false;
				NullpoMinoSDL.textInputActive = true;
			}
		});
		NullpoMinoSDL.disableAutoInputUpdate = false;
		runFrames(2, 1_000_000_000, false, false, false, 1_000_000L, frame -> {
			if(frame == 1) NullpoMinoSDL.allowQuit = false;
		});

		NullpoMinoSDL.disableAutoInputUpdate = false;
		for(int joyCase = 0; joyCase < 4; joyCase++) {
			configureRunJoystickCase(joyCase);
			runFrames(1, 1_000_000_000, false, false, false, 1_000_000L, null);
		}

		NullpoMinoSDL.disableAutoInputUpdate = true;
		GameKeySDL.gamekey[0].setInputState(GameKeyDummy.BUTTON_SCREENSHOT, 1);
		GameKeySDL.gamekey[1].setInputState(GameKeyDummy.BUTTON_QUIT, 1);
		NullpoMinoSDL.propGlobal.setProperty("custom.screenshot.directory", "existing");
		Files.createDirectories(tempDir.resolve("existing"));
		runFrames(1, 1_000_000_000, false, false, false, 1_000_000L, null);

		GameKeySDL.initGlobalGameKeySDL();
		for(GameKeySDL key : GameKeySDL.gamekey) key.loadDefaultKeymap();
		GameKeySDL.gamekey[1].setInputState(GameKeyDummy.BUTTON_SCREENSHOT, 1);
		GameKeySDL.gamekey[0].setInputState(GameKeyDummy.BUTTON_QUIT, 1);
		runFrames(1, 1_000_000_000, false, false, false, 1_000_000L, null);

		queue(event(SDLConstants.SDL_EVENT_QUIT));
		runFrames(1, 60, false, false, false, 1_000_000L, null);
		assertTrue(NullpoMinoSDL.quit);
	}

	@Test
	void calcFpsCoversAccumulationAndPublication() {
		NullpoMinoSDL.calcInterval = 0;
		NullpoMinoSDL.frameCount = 0;
		NullpoMinoSDL.prevCalcTime = 0;
		setClockStep(1_000_000_000L);
		NullpoMinoSDL.calcFPS(1);
		NullpoMinoSDL.calcFPS(1_000_000_000L);
		assertEquals(0, NullpoMinoSDL.frameCount);
		assertTrue(NullpoMinoSDL.actualFPS >= 0);
	}

	private void configureRunJoystickCase(int joyCase) {
		if(joyCase == 0) {
			NullpoMinoSDL.joystickMax = 0;
			NullpoMinoSDL.joyUseNumber = new int[]{-1, -1};
			return;
		}
		NullpoMinoSDL.joystickMax = 1;
		NullpoMinoSDL.gamepad = new SdlGamepad[]{PAD_A};
		NullpoMinoSDL.joyAxisX = new int[1];
		NullpoMinoSDL.joyAxisY = new int[1];
		NullpoMinoSDL.joyHatState = new int[1];
		NullpoMinoSDL.joyPressedState = new boolean[1][SDLConstants.SDL_GAMEPAD_NUM_BUTTONS];
		NullpoMinoSDL.joyUseNumber = joyCase == 1 ? new int[]{-1, -1}
			: joyCase == 2 ? new int[]{1, 1} : new int[]{0, 0};
	}

	private void runFrames(int frames, int maxFps, boolean perfect, boolean web,
		boolean inGame, long clockStep, Consumer<Integer> afterFrame) {
		NullpoMinoSDL.propConfig.setProperty("option.maxfps", maxFps);
		NullpoMinoSDL.propConfig.setProperty("option.perfectFPSMode", perfect);
		NullpoMinoSDL.propConfig.setProperty("option.firstSetupMode",
			NullpoMinoSDL.propConfig.getProperty("option.firstSetupMode", false));
		NullpoMinoSDL.webMode = web;
		NullpoMinoSDL.isInGame = inGame;
		setClockStep(clockStep);
		CountingState state = new CountingState(frame -> {
			if(afterFrame != null) afterFrame.accept(frame);
			if(frame >= frames) NullpoMinoSDL.quit = true;
		});
		NullpoMinoSDL.gameStates[NullpoMinoSDL.STATE_TITLE] = state;
		NullpoMinoSDL.currentState = -1;
		deque("backStack").clear();
		deque("forwardStack").clear();
		NullpoMinoSDL.run();
	}

	private static void setClockStep(long step) {
		AtomicLong clock = new AtomicLong();
		NullpoMinoSDL.nanoTime = () -> clock.addAndGet(step);
	}

	private void write(String relative, String contents) throws Exception {
		Path path = tempDir.resolve(relative);
		Files.createDirectories(path.getParent());
		Files.writeString(path, contents);
	}

	private static SDLStructs.SDL_Event event(int type) {
		SDLStructs.SDL_Event event = new SDLStructs.SDL_Event();
		event.type = type;
		return event;
	}

	private static SDLStructs.SDL_Event keyEvent(int type, int scancode) {
		SDLStructs.SDL_Event event = event(type);
		event.scancode = scancode;
		return event;
	}

	private static void queue(SDLStructs.SDL_Event event) {
		fake.events.add(event);
	}

	private static Object invoke(String name, Class<?>[] parameterTypes, Object... args)
		throws Exception {
		Method method = NullpoMinoSDL.class.getDeclaredMethod(name, parameterTypes);
		method.setAccessible(true);
		return method.invoke(null, args);
	}

	@SuppressWarnings("unchecked")
	private static Deque<Integer> deque(String name) {
		try {
			Field field = NullpoMinoSDL.class.getDeclaredField(name);
			field.setAccessible(true);
			return (Deque<Integer>)field.get(null);
		} catch(ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static void setStatic(String name, Object value) throws Exception {
		Field field = NullpoMinoSDL.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(null, value);
	}

	private static void setUninitializedStatic(String name) throws Exception {
		Field field = NullpoMinoSDL.class.getDeclaredField(name);
		field.setAccessible(true);
		Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
		unsafeField.setAccessible(true);
		sun.misc.Unsafe unsafe = (sun.misc.Unsafe)unsafeField.get(null);
		field.set(null, unsafe.allocateInstance(field.getType()));
	}

	private static <T> T proxy(Class<T> type, InvocationHandler handler) {
		return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
	}

	private static Object defaultValue(Class<?> type, boolean successfulByte) {
		if(type == void.class) return null;
		if(type == byte.class) return (byte)(successfulByte ? 1 : 0);
		if(type == short.class) return (short)0;
		if(type == int.class) return 0;
		if(type == long.class) return 0L;
		if(type == float.class) return 0f;
		if(type == double.class) return 0d;
		if(type == boolean.class) return false;
		if(type == int[].class) return new int[0];
		return null;
	}

	private static final class CountingState extends BaseStateSDL {
		private final Consumer<Integer> updater;
		int updates;
		int renders;
		int enters;
		int leaves;

		CountingState(Consumer<Integer> updater) { this.updater = updater; }
		@Override public void enter() { enters++; }
		@Override public void leave() { leaves++; }
		@Override public void update() {
			updates++;
			if(updater != null) updater.accept(updates);
		}
		@Override public void render() { renders++; }
	}

	private static final class MenuState extends DummyMenuChooseStateSDL {}

	private static final class FakeSdl implements InvocationHandler {
		final Deque<SDLStructs.SDL_Event> events = new ArrayDeque<>();
		final Set<Integer> mappedPads = new HashSet<>();
		final Set<Integer> throwingPadIds = new HashSet<>();
		final Map<Integer, SdlGamepad> openPads = new HashMap<>();
		int[] joystickIds = new int[0];
		byte initResult = 1;
		boolean windowAvailable = true;
		boolean rendererAvailable = true;
		boolean mixerEnabled;
		boolean mixerDeviceAvailable;
		boolean coordinateSuccess;
		float logicalX;
		float logicalY;
		String clipboard;
		boolean surfaceAvailable;
		short axisX;
		short axisY;
		int pressedButton = -1;
		long lastWindowFlags;
		int closedPads;
		int savedBmps;
		int fpsRenders;

		void reset() {
			events.clear();
			mappedPads.clear();
			throwingPadIds.clear();
			openPads.clear();
			joystickIds = new int[0];
			initResult = 1;
			windowAvailable = true;
			rendererAvailable = true;
			mixerEnabled = false;
			mixerDeviceAvailable = false;
			coordinateSuccess = false;
			logicalX = logicalY = 0;
			clipboard = null;
			surfaceAvailable = false;
			axisX = axisY = 0;
			pressedButton = -1;
			lastWindowFlags = 0;
			closedPads = savedBmps = fpsRenders = 0;
		}

		@Override public Object invoke(Object proxy, Method method, Object[] args) {
			String name = method.getName();
			if(name.equals("toString")) return "FakeSDL3";
			if(name.equals("hashCode")) return System.identityHashCode(proxy);
			if(name.equals("equals")) return proxy == args[0];
			if(name.equals("SDL_Init")) return initResult;
			if(name.equals("SDL_GetError")) return "fake error";
			if(name.equals("SDL_CreateWindow")) {
				lastWindowFlags = (long)args[3];
				return windowAvailable ? WINDOW : null;
			}
			if(name.equals("SDL_CreateRenderer")) return rendererAvailable ? RENDERER : null;
			if(name.equals("SDL_PollEvent")) {
				if(events.isEmpty()) return (byte)0;
				copy(events.removeFirst(), (SDLStructs.SDL_Event)args[0]);
				return (byte)1;
			}
			if(name.equals("SDL_RenderCoordinatesFromWindow")) {
				((FloatRef)args[3]).value = logicalX;
				((FloatRef)args[4]).value = logicalY;
				return (byte)(coordinateSuccess ? 1 : 0);
			}
			if(name.equals("SDL_GetClipboardText")) return clipboard;
			if(name.equals("SDL_SetClipboardText")) {
				clipboard = (String)args[0];
				return (byte)1;
			}
			if(name.equals("SDL_GetJoysticks")) return joystickIds.clone();
			if(name.equals("SDL_IsGamepad")) {
				int id = (int)args[0];
				if(throwingPadIds.contains(id)) throw new IllegalStateException("pad failure");
				return (byte)(mappedPads.contains(id) ? 1 : 0);
			}
			if(name.equals("SDL_OpenGamepad")) return openPads.get((int)args[0]);
			if(name.equals("SDL_GetGamepadNameForID")) return "Pad Three";
			if(name.equals("SDL_CloseGamepad")) { closedPads++; return null; }
			if(name.equals("SDL_GetGamepadAxis"))
				return (int)args[1] == SDLConstants.SDL_GAMEPAD_AXIS_LEFTX ? axisX : axisY;
			if(name.equals("SDL_GetGamepadButton"))
				return (byte)((int)args[1] == pressedButton ? 1 : 0);
			if(name.equals("SDL_RenderReadPixels")) return surfaceAvailable ? SURFACE : null;
			if(name.equals("SDL_SaveBMP")) { savedBmps++; return (byte)1; }
			return defaultValue(method.getReturnType(), true);
		}

		private static void copy(SDLStructs.SDL_Event from, SDLStructs.SDL_Event to) {
			to.type = from.type;
			to.scancode = from.scancode;
			to.keycode = from.keycode;
			to.keymod = from.keymod;
			to.keyDown = from.keyDown;
			to.keyRepeat = from.keyRepeat;
			to.text = from.text;
			to.editingStart = from.editingStart;
			to.editingLength = from.editingLength;
			to.wheelX = from.wheelX;
			to.wheelY = from.wheelY;
			to.windowData1 = from.windowData1;
			to.windowData2 = from.windowData2;
		}
	}
}
