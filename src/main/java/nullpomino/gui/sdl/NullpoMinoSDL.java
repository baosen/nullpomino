// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.sdl;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Deque;
import java.util.Locale;
import java.util.function.LongSupplier;

import nullpomino.game.net.LanLoungeService;
import nullpomino.game.net.NetPlatform;
import nullpomino.game.net.room.LanRoomNet;
import nullpomino.game.net.room.RoomSession;
import nullpomino.gui.GameKeyDummy;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.game.play.GameEngine;
import nullpomino.gui.sdl.binding.Ref.FloatRef;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDL3Mixer;
import nullpomino.gui.sdl.binding.SDL3TTF;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;
import nullpomino.gui.sdl.binding.SdlHandles.MixMixer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlGamepad;
import nullpomino.gui.sdl.binding.SdlHandles.SdlRenderer;
import nullpomino.gui.sdl.binding.SdlHandles.SdlSurface;
import nullpomino.gui.sdl.binding.SdlHandles.SdlWindow;
import nullpomino.util.CustomProperties;
import nullpomino.util.LogConfig;
import nullpomino.util.ModeManager;
import nullpomino.util.ModeRegistry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NullpoMino SDL3 Version
 */
public class NullpoMinoSDL {
	/** Log */
	static Logger log = LoggerFactory.getLogger(NullpoMinoSDL.class);

	/** State of the game ID */
	public static final int STATE_TITLE = 0,
							STATE_CONFIG_MAINMENU = 1,
							STATE_CONFIG_RULESELECT = 2,
							STATE_CONFIG_GENERAL = 3,
							STATE_CONFIG_KEYBOARD = 4,
							STATE_CONFIG_JOYSTICK_BUTTON = 5,
							STATE_SELECTMODE = 6,
							STATE_INGAME = 7,
							STATE_REPLAYSELECT = 8,
							STATE_CONFIG_AISELECT = 9,
							STATE_NETGAME = 10,
							STATE_CONFIG_JOYSTICK_MAIN = 11,
							STATE_CONFIG_JOYSTICK_TEST = 12,
							STATE_CONFIG_GAMETUNING = 13,
							STATE_CONFIG_RULESTYLESELECT = 14,
							STATE_CONFIG_KEYBOARD_NAVI = 15,
							STATE_CONFIG_KEYBOARD_RESET = 16,
							STATE_SELECTRULEFROMLIST = 17,
							STATE_NET_LOBBY = 18,
							STATE_NET_CREATEROOM = 19,
							STATE_NET_RANKING = 20,
							STATE_NET_RULECHANGE = 21;

	/** State of the game count */
	public static final int STATE_MAX = 22;

	public static final int LOGICAL_WIDTH = 640;
	public static final int LOGICAL_HEIGHT = 480;
	public static final String GAME_NAME = "NullpoMino+";
	public static final String GAME_NAME_UPPER = "NULLPOMINO+";

	/** Command line arguments */
	public static String[] programArgs;

	/** Settings property file */
	public static CustomProperties propConfig;

	/** Global settings property file */
	public static CustomProperties propGlobal;

	/** Music list property file */
	public static CustomProperties propMusic;

	/** Default language file */
	public static CustomProperties propLangDefault;

	/** Language file */
	public static CustomProperties propLang;

	/** Default game mode description file */
	public static CustomProperties propDefaultModeDesc;

	/** Game mode description file */
	public static CustomProperties propModeDesc;

	/** Mode manager */
	public static ModeManager modeManager;

	/** End flag */
	public static boolean quit = false;

	/** FPS display */
	public static boolean showfps = false;

	/** FPS calculation interval */
	protected static long calcInterval = 0;

	/** FPS calculation previous time */
	protected static long prevCalcTime = 0;

	/** Frame count */
	protected static long frameCount = 0;

	/** Actual FPS */
	public static double actualFPS = 0.0;

	/** FPS display decimal format */
	public static DecimalFormat df = new DecimalFormat("0.0");

	/** Replaceable package-private seams keep the frame loop deterministic in tests. */
	static Runnable resourceLoader = ResourceHolderSDL::load;
	static Runnable fpsRenderer = () -> NormalFontSDL.printFont(0, 480 - 16,
		df.format(actualFPS), NormalFontSDL.COLOR_BLUE, 1.0f);
	static LongSupplier nanoTime = System::nanoTime;

	/** Used by perfect fps mode */
	public static long perfectFPSDelay = 0;

	/** True to use perfect FPS */
	public static boolean perfectFPSMode = false;

	/** Execute Thread.yield() during Perfect FPS mode */
	public static boolean perfectYield = true;

	/** If a key is held down, true */
	public static boolean[] keyPressedState;

	/** Key-down events that fired during the current frame (widget-facing). Cleared each frame. */
	public static final java.util.List<KeyEvent> frameKeyEvents = new java.util.ArrayList<>();

	/** UTF-8 text committed via SDL_EVENT_TEXT_INPUT this frame. Widgets append consumed text via consumeTextInput(). */
	public static final StringBuilder pendingTextInput = new StringBuilder();

	/** Current IME preedit (composition) string; empty if none in progress. Updated on SDL_EVENT_TEXT_EDITING. */
	public static String imeComposition = "";

	/** Caret offset within imeComposition (UTF-8 byte offset as reported by SDL). */
	public static int imeCompositionStart = 0;

	/** Selected range length within imeComposition (SDL reports both cursor + highlighted segment). */
	public static int imeCompositionLength = 0;

	/** Vertical mouse wheel delta accumulated this frame (positive = scroll up). Reset each frame. */
	public static float mouseWheelDelta = 0;

	/** When true, text-input keyboard events are consumed by the focused widget and not by GameKeySDL. */
	public static boolean textInputActive = false;

	/**
	 * Single key-down event delivered to widgets during a frame.
	 * Bundles scancode, active keymod bitmask, and whether it was a hold-repeat.
	 */
	public static class KeyEvent {
		public final int scancode;
		public final int keymod;
		public final boolean repeat;
		public KeyEvent(int scancode, int keymod, boolean repeat) {
			this.scancode = scancode;
			this.keymod = keymod;
			this.repeat = repeat;
		}
	}

	/** Use gamepad number per player (-1 = none) */
	public static int[] joyUseNumber;

	/** Number of connected gamepads */
	public static int joystickMax;

	/** Open gamepad handles (only devices with a standard SDL gamepad mapping) */
	public static SdlGamepad[] gamepad;

	/** Gamepad display names (normalized via {@link #padDisplayName}) */
	public static String[] joyName;

	/** Left stick state */
	public static int[] joyAxisX, joyAxisY;

	/** D-pad state (SDL hat bitmask) */
	public static int[] joyHatState;

	/** Gamepad button pressed state, indexed by SDL_GAMEPAD_BUTTON_* ordinal */
	public static boolean[][] joyPressedState;

	/** State game */
	public static BaseStateSDL[] gameStates;

	/** Current state */
	public static int currentState;

	/** In-game flag (if false, Perfect FPS will not be used) */
	public static boolean isInGame;

	/** Exit button and Screenshot button permission to use */
	public static boolean enableSpecialKeys;

	/** Exit button permission */
	public static boolean allowQuit;

	/** True if disable automatic input update */
	public static boolean disableAutoInputUpdate;

	/** Current fullscreen state */
	public static boolean fullscreen;

	/** Previous F11 key state for edge detection */
	private static boolean prevF11Pressed;

	/** Maximum FPS */
	public static int maxFPS;

	/**
	 * True when running on the browser (TeaVM) backend. Spin waits would
	 * starve the browser tab's event loop, so the FPS cap must always sleep;
	 * the "perfect FPS" option is ignored in this mode.
	 * Non-final so the web entry point can set it directly (the TeaVM target
	 * has no {@code -D} system properties at launch).
	 */
	public static boolean webMode = Boolean.getBoolean("nullpomino.web");

	/** P2P room session while one is active, null otherwise */
	public static RoomSession roomSession;

	/**
	 * Shared netplay session (protocol client, chat buffers, room list, rule catalogue).
	 * Created by {@code StateNetLobbySDL.enter()} and destroyed by
	 * {@link #endNetplay()}.  All {@code StateNet*SDL} classes read/mutate this.
	 */
	public static NetLobbyFrame netLobby;

	/** SDL3 window handle */
	public static SdlWindow window;

	/** SDL3 renderer handle */
	public static SdlRenderer renderer;

	/** SDL3_mixer library (null if unavailable) */
	public static SDL3Mixer mixerLib;

	/** SDL3_mixer mixer device (null if unavailable) */
	public static MixMixer mixer;

	/** Shared event struct for polling */
	private static SDLStructs.SDL_Event event;

	/** Zero-filled keyboard state passed to GameKeySDL when textInputActive is true. */
	private static boolean[] emptyKeyState;

	/**
	 * Main function
	 * @param args command line arguments
	 */
	public static void main(String[] args) {
		LogConfig.configure("config/etc/log_sdl.xml");
		log.info("NullpoMinoSDL Start");

		bootstrap(args);
		NetPlatform.install(new LanRoomNet(), new LanLoungeService());
		modeManager.loadGameModes(ModeRegistry.all());
		registerAllStates();

		// SDL init
		try {
			init();
		} catch (Throwable e) {
			log.error("SDL init failed", e);
			String strErrorTitle = getUIText("InitFailedMessageGeneral_Title");
			String strErrorMessage = String.format(getUIText("InitFailedMessageGeneral_Body"), e.toString());
			try {
				SDL3.INSTANCE.SDL_ShowSimpleMessageBox(
					SDLConstants.SDL_MESSAGEBOX_ERROR, strErrorTitle, strErrorMessage, null);
			} catch (Throwable t) {
				System.err.println(strErrorTitle + ": " + strErrorMessage);
			}
			System.exit(-1);
		}

		// Run
		try {
			run();
		} catch (Throwable e) {
			log.error("Uncaught Exception", e);
		} finally {
			shutdown();
		}

		System.exit(0);
	}

	/**
	 * Shared bootstrap used by both the desktop {@link #main} and the web
	 * entry point: loads config/language/mode-description properties, seeds
	 * default rule selections, initializes key and mouse input, and allocates
	 * the mode manager and the (still-empty) state array. It deliberately does
	 * NOT load game modes or register states — each entry point does that
	 * itself, so the web build can exclude the netplay modes and states.
	 * @param args command line arguments
	 */
	public static void bootstrap(String[] args) {
		programArgs = args;
		// Read configuration file
		propConfig = CustomProperties.loadFromFileOrEmpty("config/setting/sdl.cfg");
		propGlobal = CustomProperties.loadFromFileOrEmpty("config/setting/global.cfg");
		propMusic = CustomProperties.loadFromFileOrEmpty("config/setting/music.cfg");

		// Read language file
		propLangDefault = new CustomProperties();
		try {
			propLangDefault = CustomProperties.loadFromFile("config/lang/sdl_default.properties");
		} catch (IOException e) {
			log.error("Failed to load default UI language file", e);
		}

		propLang = CustomProperties.loadFromFileOrEmpty(
				"config/lang/sdl_" + Locale.getDefault().getCountry() + ".properties");

		// Game mode description
		propDefaultModeDesc = new CustomProperties();
		try {
			propDefaultModeDesc = CustomProperties.loadFromFile("config/lang/modedesc_default.properties");
		} catch(IOException e) {
			log.error("Couldn't load default mode description file", e);
		}

		propModeDesc = CustomProperties.loadFromFileOrEmpty(
				"config/lang/modedesc_" + Locale.getDefault().getCountry() + ".properties");

		// Mode manager (modes are loaded by the caller)
		modeManager = new ModeManager();

		// Set default rule selections
		try {
			CustomProperties propDefaultRule =
					CustomProperties.loadFromFile("config/list/global_defaultrule.properties");

			for(int pl = 0; pl < 2; pl++)
				for(int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
					if(i == 0) {
						if(propGlobal.getProperty(pl + ".rule") == null) {
							propGlobal.setProperty(pl + ".rule", propDefaultRule.getProperty("default.rule", ""));
							propGlobal.setProperty(pl + ".rulefile", propDefaultRule.getProperty("default.rulefile", ""));
							propGlobal.setProperty(pl + ".rulename", propDefaultRule.getProperty("default.rulename", ""));
						}
					} else {
						if(propGlobal.getProperty(pl + ".rule." + i) == null) {
							propGlobal.setProperty(pl + ".rule." + i, propDefaultRule.getProperty("default.rule." + i, ""));
							propGlobal.setProperty(pl + ".rulefile." + i, propDefaultRule.getProperty("default.rulefile." + i, ""));
							propGlobal.setProperty(pl + ".rulename." + i, propDefaultRule.getProperty("default.rulename." + i, ""));
						}
					}
				}
		} catch (Exception e) {}

		// Key input initialization (scancodes: 0..511)
		keyPressedState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		emptyKeyState = new boolean[SDLConstants.SDL_SCANCODE_COUNT];
		GameKeySDL.initGlobalGameKeySDL();
		GameKeySDL.gamekey[0].loadConfig(propConfig);
		GameKeySDL.gamekey[1].loadConfig(propConfig);

		// One-time migration: old configs store SDL 1.2 keysym values (e.g. UP=273)
		// which are out of range for SDL3 scancodes (UP=82, max=512).
		// Detect this and reset to defaults.
		if(!propConfig.getProperty("option.sdl3KeyMigrated", false)) {
			log.info("Migrating key config from SDL 1.2 keysyms to SDL3 scancodes");
			GameKeySDL.gamekey[0].loadDefaultKeymap();
			GameKeySDL.gamekey[1].loadDefaultKeymap();
			GameKeySDL.gamekey[0].saveConfig(propConfig);
			GameKeySDL.gamekey[1].saveConfig(propConfig);
			propConfig.setProperty("option.sdl3KeyMigrated", true);
		}

		MouseInputSDL.initalizeMouseInput();

		// State array (populated by the caller)
		currentState = -1;
		gameStates = new BaseStateSDL[STATE_MAX];
	}

	/**
	 * Instantiate every SDL state, including the netplay states, for both the
	 * desktop and the browser (TeaVM) entry points. Netplay is safe on the web
	 * build: it runs the room engine over WebRTC DataChannels behind the
	 * RoomNet seam, and the transport-free NetBaseClient keeps {@code java.net}
	 * socket code out of the TeaVM-compiled call graph.
	 */
	public static void registerAllStates() {
		gameStates[STATE_TITLE] = new StateTitleSDL();
		gameStates[STATE_CONFIG_MAINMENU] = new StateConfigMainMenuSDL();
		gameStates[STATE_CONFIG_RULESELECT] = new StateConfigRuleSelectSDL();
		gameStates[STATE_CONFIG_GENERAL] = new StateConfigGeneralSDL();
		gameStates[STATE_CONFIG_KEYBOARD] = new StateConfigKeyboardSDL();
		gameStates[STATE_CONFIG_JOYSTICK_BUTTON] = new StateConfigJoystickButtonSDL();
		gameStates[STATE_SELECTMODE] = new StateSelectModeSDL();
		gameStates[STATE_INGAME] = new StateInGameSDL();
		gameStates[STATE_REPLAYSELECT] = new StateReplaySelectSDL();
		gameStates[STATE_CONFIG_AISELECT] = new StateConfigAISelectSDL();
		gameStates[STATE_NETGAME] = new StateNetGameSDL();
		gameStates[STATE_CONFIG_JOYSTICK_MAIN] = new StateConfigJoystickMainSDL();
		gameStates[STATE_CONFIG_JOYSTICK_TEST] = new StateConfigJoystickTestSDL();
		gameStates[STATE_CONFIG_GAMETUNING] = new StateConfigGameTuningSDL();
		gameStates[STATE_CONFIG_RULESTYLESELECT] = new StateConfigRuleStyleSelectSDL();
		gameStates[STATE_CONFIG_KEYBOARD_NAVI] = new StateConfigKeyboardNaviSDL();
		gameStates[STATE_CONFIG_KEYBOARD_RESET] = new StateConfigKeyboardResetSDL();
		gameStates[STATE_SELECTRULEFROMLIST] = new StateSelectRuleFromListSDL();
		gameStates[STATE_NET_LOBBY] = new StateNetLobbySDL();
		gameStates[STATE_NET_CREATEROOM] = new StateNetCreateRoomSDL();
		gameStates[STATE_NET_RANKING] = new StateNetRankingSDL();
		gameStates[STATE_NET_RULECHANGE] = new StateNetRuleChangeSDL();
	}

	/**
	 * SDL3 initialization
	 */
	public static void init() {
		log.info("Now initializing SDL3...");

		int initFlags = SDLConstants.SDL_INIT_VIDEO | SDLConstants.SDL_INIT_AUDIO | SDLConstants.SDL_INIT_GAMEPAD;
		if(SDL3.INSTANCE.SDL_Init(initFlags) == 0) {
			throw new RuntimeException("SDL_Init failed: " + SDL3.INSTANCE.SDL_GetError());
		}

		fullscreen = propConfig.getProperty("option.fullscreen", false);
		int windowWidth = propConfig.getProperty("option.screenwidth", LOGICAL_WIDTH);
		int windowHeight = propConfig.getProperty("option.screenheight", LOGICAL_HEIGHT);

		long windowFlags = SDLConstants.SDL_WINDOW_RESIZABLE;
		if(fullscreen) windowFlags |= SDLConstants.SDL_WINDOW_FULLSCREEN;

		window = SDL3.INSTANCE.SDL_CreateWindow(GAME_NAME + " (Now Loading...)", windowWidth, windowHeight, windowFlags);
		if(window == null) {
			throw new RuntimeException("SDL_CreateWindow failed: " + SDL3.INSTANCE.SDL_GetError());
		}

		renderer = SDL3.INSTANCE.SDL_CreateRenderer(window, null);
		if(renderer == null) {
			throw new RuntimeException("SDL_CreateRenderer failed: " + SDL3.INSTANCE.SDL_GetError());
		}

		// Set logical presentation — SDL3 handles all scaling and letterboxing
		SDL3.INSTANCE.SDL_SetRenderLogicalPresentation(renderer,
			LOGICAL_WIDTH, LOGICAL_HEIGHT, SDLConstants.SDL_LOGICAL_PRESENTATION_LETTERBOX);

		// TTF init
		SDL3TTF.INSTANCE.TTF_Init();
		log.info("SDL3_ttf initialized");

		// Mixer init (optional — may not be available)
		mixerLib = SDL3Mixer.loadOrNull();
		if(mixerLib != null) {
			mixerLib.MIX_Init();
			// SDL_AUDIO_DEVICE_DEFAULT_PLAYBACK = 0xFFFFFFFF
			// Let SDL choose the device's native playback format. Forcing a fixed
			// 44.1 kHz S16 stereo path can add an extra conversion stage on modern
			// systems, which is unnecessary for SDL3_mixer.
			mixer = mixerLib.MIX_CreateMixerDevice(0xFFFFFFFF, null);
			if(mixer != null) {
				log.info("SDL3_mixer initialized");
			} else {
				log.warn("SDL3_mixer MIX_CreateMixerDevice failed: {}", SDL3.INSTANCE.SDL_GetError());
				mixerLib = null;
			}
		} else {
			log.warn("SDL3_mixer not available — audio disabled");
		}

		// Event buffer
		event = new SDLStructs.SDL_Event();

		// Gamepad setup
		initJoysticks();
	}

	/**
	 * Enumerate and open all connected gamepads. Only devices with a standard
	 * SDL gamepad mapping (standardized button ordinals, SOUTH=0 etc.) are
	 * opened; other joystick-class devices are ignored.
	 * Idempotent — also called on hotplug after {@link #closeJoysticks()}.
	 */
	protected static void initJoysticks() {
		joyUseNumber = new int[2];
		// Player 1 uses the first gamepad out of the box (everything is
		// guarded by joystickMax, so keyboard-only setups are unaffected).
		joyUseNumber[0] = propConfig.getProperty("joyUseNumber.p0", 0);
		joyUseNumber[1] = propConfig.getProperty("joyUseNumber.p1", -1);

		int[] joystickIds = SDL3.INSTANCE.SDL_GetJoysticks();
		SdlGamepad[] opened = new SdlGamepad[joystickIds.length];
		String[] names = new String[joystickIds.length];
		int n = 0;
		for(int id : joystickIds) {
			try {
				if(SDL3.INSTANCE.SDL_IsGamepad(id) != 0) {
					SdlGamepad g = SDL3.INSTANCE.SDL_OpenGamepad(id);
					if(g != null) {
						names[n] = padDisplayName(SDL3.INSTANCE.SDL_GetGamepadNameForID(id));
						opened[n++] = g; // compact: skip failed opens
					}
				}
			} catch (Throwable e) {
				log.warn("Failed to open gamepad id {}", id, e);
			}
		}
		joystickMax = n;
		gamepad = java.util.Arrays.copyOf(opened, n);
		joyName = java.util.Arrays.copyOf(names, n);
		joyAxisX = new int[n];
		joyAxisY = new int[n];
		joyHatState = new int[n];
		joyPressedState = new boolean[n][SDLConstants.SDL_GAMEPAD_NUM_BUTTONS];
		log.info("Number of Gamepads:{}", joystickMax);
	}

	/** Close all open gamepad handles (inverse of {@link #initJoysticks()}). */
	protected static void closeJoysticks() {
		for(int i = 0; i < joystickMax; i++) {
			if(gamepad[i] != null) {
				SDL3.INSTANCE.SDL_CloseGamepad(gamepad[i]);
				gamepad[i] = null;
			}
		}
		joystickMax = 0;
	}

	/**
	 * Toggle fullscreen mode.
	 */
	public static void toggleFullscreen() {
		fullscreen = !fullscreen;
		SDL3.INSTANCE.SDL_SetWindowFullscreen(window, fullscreen ? 1 : 0);
		propConfig.setProperty("option.fullscreen", fullscreen);
		saveConfig();
		log.debug("Fullscreen toggled: {}", fullscreen);
	}

	/**
	 * Resync the fullscreen flag and config to a state the browser/OS reported.
	 * On the web build this is the primary path for the F11 toggle: the DOM event
	 * bridge drives the browser Fullscreen API directly (within the user gesture,
	 * which the browser requires to re-enter fullscreen) and the resulting
	 * fullscreenchange feeds the real state back here. It also catches changes
	 * made outside our control, e.g. the user pressing Escape to leave browser
	 * fullscreen. Guarded so it is a no-op when the flag already matches — the
	 * case for the config screen's own {@link #toggleFullscreen()} (which sets the
	 * flag before SDL emits the enter/leave event), the desktop path, and the
	 * duplicate fullscreenchange Chrome fires. Returns true when the flag actually
	 * changed, so the caller persists only genuine changes.
	 */
	static boolean syncFullscreenFlag(boolean nowFullscreen) {
		if(fullscreen == nowFullscreen) return false;
		fullscreen = nowFullscreen;
		propConfig.setProperty("option.fullscreen", nowFullscreen);
		return true;
	}

	/**
	 * Map window coordinates to logical 640x480 coordinates.
	 * Returns logical X, or -1 if mapping fails.
	 */
	static int windowToLogicalX(float windowX) {
		FloatRef lx = new FloatRef();
		FloatRef ly = new FloatRef();
		if(SDL3.INSTANCE.SDL_RenderCoordinatesFromWindow(renderer, windowX, 0, lx, ly) != 0) {
			float val = lx.getValue();
			if(val < 0 || val >= LOGICAL_WIDTH) return -1;
			return (int)val;
		}
		return -1;
	}

	/**
	 * Map window coordinates to logical 640x480 coordinates.
	 * Returns logical Y, or -1 if mapping fails.
	 */
	static int windowToLogicalY(float windowY) {
		FloatRef lx = new FloatRef();
		FloatRef ly = new FloatRef();
		if(SDL3.INSTANCE.SDL_RenderCoordinatesFromWindow(renderer, 0, windowY, lx, ly) != 0) {
			float val = ly.getValue();
			if(val < 0 || val >= LOGICAL_HEIGHT) return -1;
			return (int)val;
		}
		return -1;
	}

	/**
	 * Main loop
	 */
	public static void run() {
		maxFPS = propConfig.getProperty("option.maxfps", 60);

		boolean sleepFlag;
		long period;
		long beforeTime, afterTime, timeDiff, sleepTime, sleepTimeInMillis;
		long overSleepTime = 0L;
		int noDelays = 0;

		showfps = propConfig.getProperty("option.showfps", false);
		perfectFPSMode = propConfig.getProperty("option.perfectFPSMode", false);
		perfectYield = propConfig.getProperty("option.perfectYield", false);

		beforeTime = nanoTime.getAsLong();
		prevCalcTime = beforeTime;

		quit = false;
		enableSpecialKeys = true;
		allowQuit = true;

		// Loading resources
		resourceLoader.run();

		// First run
		if(propConfig.getProperty("option.firstSetupMode", true) == true) {
			GameKeySDL.gamekey[0].loadDefaultKeymap();
			GameKeySDL.gamekey[0].saveConfig(propConfig);
			propConfig.setProperty("option.firstSetupMode", false);

			if(propGlobal.getProperty("global.firstSetupMode", true) == true) {
				for(int pl = 0; pl < 2; pl++) {
					if(propGlobal.getProperty(pl + ".tuning.owRotateButtonDefaultRight") == null) {
						propGlobal.setProperty(pl + ".tuning.owRotateButtonDefaultRight", 0);
					}
				}
				propGlobal.setProperty("global.firstSetupMode", false);
			}

			saveConfig();
			enterState(STATE_TITLE);
		} else {
			enterState(STATE_TITLE);
		}

		perfectFPSDelay = nanoTime.getAsLong();

		// Main loop
		while(quit == false) {
			// Event processing
			processEvent();
			if(quit == true) break;

			// F11 fullscreen toggle
			boolean f11Pressed = keyPressedState[SDLConstants.SDL_SCANCODE_F11];
			if(f11Pressed && !prevF11Pressed) {
				toggleFullscreen();
				keyPressedState[SDLConstants.SDL_SCANCODE_F11] = false;
			}
			prevF11Pressed = f11Pressed;

			// Joystick updates
			if(joystickMax > 0) joyUpdate();

			// Update key input states. When a text-input widget has focus, mask out the raw
			// keyboard state so user typing doesn't also fire game buttons (e.g. 'A' bound to
			// BUTTON_A would otherwise activate menu items while typing).
			if(!disableAutoInputUpdate) {
				boolean[] kbdForGameKey = textInputActive ? emptyKeyState : keyPressedState;
				for(int i = 0; i < 2; i++) {
					int joynum = joyUseNumber[i];

					if((joystickMax > 0) && (joynum >= 0) && (joynum < joystickMax)) {
						GameKeySDL.gamekey[i].update(kbdForGameKey, joyPressedState[joynum], joyAxisX[joynum], joyAxisY[joynum], joyHatState[joynum]);
					} else {
						GameKeySDL.gamekey[i].update(kbdForGameKey);
					}
				}
			}

			// Render frame
			SDL3.setDrawColor(renderer, 0, 0, 0, 255);
			SDL3.INSTANCE.SDL_RenderClear(renderer);

			// Processing is executed for each state
			gameStates[currentState].update();
			gameStates[currentState].render();

			// FPS drawing
			if(showfps) fpsRenderer.run();

			// Special keys
			if(enableSpecialKeys) {
				if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_SCREENSHOT) || GameKeySDL.gamekey[1].isPushKey(GameKeySDL.BUTTON_SCREENSHOT))
					saveScreenShot();

				if(allowQuit) {
					if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_QUIT) || GameKeySDL.gamekey[1].isPushKey(GameKeySDL.BUTTON_QUIT))
						enterState(-1);
				}
			}

			// Present the rendered frame — SDL3 handles scaling via logical presentation
			SDL3.INSTANCE.SDL_RenderPresent(renderer);

			// FPS cap
			sleepFlag = false;

			afterTime = nanoTime.getAsLong();
			timeDiff = afterTime - beforeTime;

			period = (long) (1.0 / maxFPS * 1000000000);
			sleepTime = (period - timeDiff) - overSleepTime;
			sleepTimeInMillis = sleepTime / 1000000L;

			if((sleepTimeInMillis >= 4) && (!perfectFPSMode || !isInGame)) {
				if(maxFPS > 0) {
					try {
						Thread.sleep(sleepTimeInMillis);
					} catch(InterruptedException e) {}
				}
				overSleepTime = (nanoTime.getAsLong() - afterTime) - sleepTime;
				perfectFPSDelay = nanoTime.getAsLong();
				sleepFlag = true;
			} else if(webMode) {
				// Never spin-wait in a browser tab; a plain sleep is the only
				// cooperative way to yield the event loop.
				if(sleepTime > 0) {
					try {
						Thread.sleep(Math.max(1, sleepTimeInMillis));
					} catch(InterruptedException e) {}
				}
				overSleepTime = 0L;
				perfectFPSDelay = nanoTime.getAsLong();
				sleepFlag = true;
			} else if((perfectFPSMode && isInGame) || (sleepTime > 0)) {
				overSleepTime = 0L;
				if(perfectYield) {
					while(nanoTime.getAsLong() < perfectFPSDelay + 1000000000 / maxFPS) {Thread.yield();}
				} else {
					while(nanoTime.getAsLong() < perfectFPSDelay + 1000000000 / maxFPS) {}
				}
				perfectFPSDelay += 1000000000 / maxFPS;

				if(nanoTime.getAsLong() > perfectFPSDelay + 2000000000 / maxFPS) {
					perfectFPSDelay = nanoTime.getAsLong();
				}

				sleepFlag = true;
			}

			if(!sleepFlag) {
				overSleepTime = 0L;
				if(++noDelays >= 16) {
					Thread.yield();
					noDelays = 0;
				}
				perfectFPSDelay = nanoTime.getAsLong();
			}

			beforeTime = nanoTime.getAsLong();
			calcFPS(period);
		}
	}

	/**
	 * SDL3 shutdown
	 */
	public static void shutdown() {
		log.info("NullpoMinoSDL shutdown()");

		try {
			// Flush netplay state (saves netlobby.cfg incl. the lounge nickname
			// and sends the graceful disconnect) before tearing the session down
			if(netLobby != null) {
				try {
					netLobby.shutdown();
				} catch (Throwable e) {
					log.warn("netLobby shutdown failed", e);
				}
				netLobby = null;
			}
			stopRoomSession();
			closeJoysticks();
			ResourceHolderSDL.destroy();
			if(renderer != null) {
				SDL3.INSTANCE.SDL_DestroyRenderer(renderer);
				renderer = null;
			}
			if(window != null) {
				SDL3.INSTANCE.SDL_DestroyWindow(window);
				window = null;
			}
			if(mixerLib != null) {
				if(mixer != null) {
					mixerLib.MIX_DestroyMixer(mixer);
					mixer = null;
				}
				mixerLib.MIX_Quit();
			}
			SDL3TTF.INSTANCE.TTF_Quit();
			SDL3.INSTANCE.SDL_Quit();
		} catch (Throwable e) {}
	}

	/**
	 * End an ongoing netplay session and return to the title screen.  Safe to call
	 * with {@code netLobby == null}. Intended for "Exit" buttons across the
	 * netplay lobby states and for hard-disconnect recovery paths.
	 */
	public static void endNetplay() {
		stopRoomSession();
		if(netLobby != null) {
			try { netLobby.shutdown(); }
			catch(Throwable t) { log.warn("netLobby shutdown failed", t); }
			netLobby = null;
		}
		enterStateClear(STATE_TITLE);
	}

	/**
	 * Navigation history. Every forward transition via {@link #enterState(int)}
	 * pushes the outgoing state onto this deque, so {@link #goBack()} can
	 * unwind the navigation the way a browser back button does. Cleared by
	 * {@link #enterStateClear(int)} for transitions that represent a reset
	 * (endNetplay, quit-to-title from gameplay, nl == null error bailouts).
	 */
	private static Deque<Integer> backStack = new ArrayDeque<Integer>();

	/**
	 * Redo history for {@link #goForward()}. {@link #goBack()} pushes the
	 * outgoing state here so the mouse forward button can re-enter screens
	 * the user just stepped back from. Cleared by {@link #enterState(int)}
	 * (a fresh forward navigation invalidates the redo branch, browser-style)
	 * and by {@link #enterStateClear(int)} (full reset).
	 */
	private static Deque<Integer> forwardStack = new ArrayDeque<Integer>();

	/**
	 * States whose {@code leave()} tears down per-session resources
	 * ({@code gameManager}, network room state) and so cannot safely be
	 * re-entered by replaying a navigation. {@link #goBack()} skips pushing
	 * these onto {@link #forwardStack}; {@link #goForward()} treats a
	 * popped entry that's no longer safe as a no-op for the same reason.
	 */
	private static boolean isForwardSafe(int id) {
		return id != STATE_INGAME && id != STATE_NETGAME;
	}

	private static boolean isStateId(int id) {
		return (id >= 0) && (id < STATE_MAX);
	}

	private static boolean hasCurrentState() {
		return isStateId(currentState);
	}

	private static void pushCurrent(Deque<Integer> stack) {
		if(hasCurrentState()) stack.push(currentState);
	}

	/**
	 * Request that the main loop exit. A no-op on the web build: a browser tab
	 * has no process to terminate, so letting {@link #run()} fall out of its
	 * loop would just freeze the canvas on its last frame — the game would
	 * appear to hang (e.g. pressing Escape at the title, whose cancel path
	 * lands on the empty back stack). The desktop build sets the flag, breaks
	 * the loop, and proceeds to {@link #shutdown()} / {@code System.exit}.
	 */
	private static void requestQuit() {
		if(webMode) return;
		quit = true;
	}

	/**
	 * Switch state, pushing the outgoing state onto the back stack so
	 * {@link #goBack()} can return here later.
	 * @param id Destination state ID (-1 to end the program; ignored on web)
	 */
	public static void enterState(int id) {
		if(id == -1) {
			requestQuit();
			return;
		}
		pushCurrent(backStack);
		forwardStack.clear();
		doTransition(id);
	}

	/**
	 * Pop the back stack and transition to the revealed state, or quit the
	 * program if the stack is empty (the title screen's cancel path lands
	 * here; a no-op on web — see {@link #requestQuit()}). Pushes the outgoing
	 * state onto {@link #forwardStack} when it's safely re-enterable so
	 * {@link #goForward()} can replay this hop.
	 */
	public static void goBack() {
		if(backStack.isEmpty()) {
			requestQuit();
			return;
		}
		if(hasCurrentState() && isForwardSafe(currentState)) {
			forwardStack.push(currentState);
		}
		doTransition(backStack.pop());
	}

	/**
	 * Pop the forward stack and re-enter the state {@link #goBack()} last
	 * stepped away from, pushing the current state onto the back stack so
	 * the trip remains reversible. No-op when the forward stack is empty
	 * or the recorded state isn't safe to re-enter (its session has since
	 * been torn down).
	 */
	public static void goForward() {
		if(forwardStack.isEmpty()) return;
		int id = forwardStack.pop();
		if(!isForwardSafe(id)) return;
		pushCurrent(backStack);
		doTransition(id);
	}

	/**
	 * Switch state, clearing the back stack first and re-seeding it with
	 * the title screen when the destination isn't already title. Use this
	 * for transitions that reset the navigation (endNetplay, quit-to-title
	 * from an in-game menu, error bailouts when netLobby has gone null):
	 * the prior stack is stale, and the user should still be able to
	 * escape back to the title from wherever we've landed them rather than
	 * immediately quitting the game on the next cancel press.
	 * @param id Destination state ID
	 */
	public static void enterStateClear(int id) {
		backStack.clear();
		forwardStack.clear();
		if(id != STATE_TITLE) backStack.push(STATE_TITLE);
		doTransition(id);
	}

	/**
	 * Shared state-transition body. Runs {@code leave()} on the outgoing
	 * state and {@code enter()} on the incoming one. Does not touch
	 * {@link #backStack} — callers are responsible for push/pop/clear.
	 */
	private static void doTransition(int id) {
		BaseStateSDL previous = hasCurrentState() ? gameStates[currentState] : null;
		if(previous != null) previous.leave();

		BaseStateSDL next = stateForTransition(id);
		if(next == null) return;

		currentState = id;
		next.enter();
		// Tell menu screens they were just entered, so the next mouse-
		// hover check snaps the cursor to the row under the pointer
		// without waiting for a movement event. Done here (after
		// enter()) rather than in the base class enter() so subclasses
		// that override enter() without chaining to super still pick
		// it up.
		if (next instanceof DummyMenuChooseStateSDL) {
			((DummyMenuChooseStateSDL) next).justEntered = true;
		}
	}

	private static BaseStateSDL stateForTransition(int id) {
		if(id < 0) {
			requestQuit();
			return null;
		}
		if(isStateId(id) && (gameStates[id] != null)) return gameStates[id];
		throw new NullPointerException("Game state #" + id + " is null");
	}

	/**
	 * Save configuration file
	 */
	public static void saveConfig() {
		try {
			propConfig.storeToFile("config/setting/sdl.cfg", "NullpoMino SDL-frontend Config");
			log.debug("Saved SDL-frontend config");
		} catch(IOException e) {
			log.error("Failed to save SDL-specific config", e);
		}

		try {
			propGlobal.storeToFile("config/setting/global.cfg", "NullpoMino Global Config");
			log.debug("Saved global config");
		} catch(IOException e) {
			log.error("Failed to save global config", e);
		}
	}

	/**
	 * (Re-)Load global config file
	 */
	public static void loadGlobalConfig() {
		try {
			propGlobal = CustomProperties.loadFromFile("config/setting/global.cfg");
		} catch(IOException e) {}
	}

	/**
	 * Save screenshot
	 */
	public static void saveScreenShot() {
		String dir = propGlobal.getProperty("custom.screenshot.directory", "ss");
		Calendar c = Calendar.getInstance();
		DateFormat dfm = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String filename = dir + "/" + dfm.format(c.getTime()) + ".bmp";
		log.info("Saving screenshot to {}", filename);

		File ssfolder = nullpomino.util.DataDir.file(dir);
		if (!ssfolder.exists()) {
			if (ssfolder.mkdir()) {
				log.info("Created screenshot folder: {}", dir);
			} else {
				log.info("Couldn't create screenshot folder at {}", dir);
			}
		}

		SdlSurface surface = SDL3.INSTANCE.SDL_RenderReadPixels(renderer, null);
		if(surface != null) {
			SDL3.INSTANCE.SDL_SaveBMP(surface, nullpomino.util.DataDir.path(filename));
			SDL3.INSTANCE.SDL_DestroySurface(surface);
		}
	}

	/**
	 * Get UI text string
	 * @param str key
	 * @return translated string (or key if not found)
	 */
	public static String getUIText(String str) {
		String result = propLang.getProperty(str);
		if(result == null) {
			result = propLangDefault.getProperty(str, str);
		}
		return result;
	}

	/**
	 * Event processing
	 */
	protected static void processEvent() {
		// Per-frame widget event buffers reset at the top of each poll.
		frameKeyEvents.clear();
		mouseWheelDelta = 0;
		boolean joyChanged = false;

		while(SDL3.INSTANCE.SDL_PollEvent(event) != 0) {
			int type = event.getType();

			if(type == SDLConstants.SDL_EVENT_QUIT) {
				enterState(-1);
			} else if(type == SDLConstants.SDL_EVENT_KEY_DOWN) {
				int scancode = event.getScancode();
				if(scancode >= 0 && scancode < keyPressedState.length) {
					keyPressedState[scancode] = true;
				}
				frameKeyEvents.add(new KeyEvent(scancode, event.getKeymod(), event.isKeyRepeat()));
			} else if(type == SDLConstants.SDL_EVENT_KEY_UP) {
				int scancode = event.getScancode();
				if(scancode >= 0 && scancode < keyPressedState.length) {
					keyPressedState[scancode] = false;
				}
			} else if(type == SDLConstants.SDL_EVENT_TEXT_INPUT) {
				pendingTextInput.append(event.getTextInputText());
			} else if(type == SDLConstants.SDL_EVENT_TEXT_EDITING) {
				imeComposition = event.getTextInputText();
				imeCompositionStart = event.getTextEditingStart();
				imeCompositionLength = event.getTextEditingLength();
			} else if(type == SDLConstants.SDL_EVENT_MOUSE_WHEEL) {
				mouseWheelDelta += event.getMouseWheelY();
			} else if(type == SDLConstants.SDL_EVENT_WINDOW_ENTER_FULLSCREEN) {
				// Resync when fullscreen changes outside our control (e.g. the web
				// backend reporting a browser-driven change). Persist only real changes.
				if(syncFullscreenFlag(true)) saveConfig();
			} else if(type == SDLConstants.SDL_EVENT_WINDOW_LEAVE_FULLSCREEN) {
				if(syncFullscreenFlag(false)) saveConfig();
			} else if(type == SDLConstants.SDL_EVENT_JOYSTICK_ADDED
					|| type == SDLConstants.SDL_EVENT_JOYSTICK_REMOVED) {
				joyChanged = true;
			}
			// Window resize events are handled automatically by SDL_SetRenderLogicalPresentation
		}

		if(joyChanged) {
			// Hotplug: no per-device bookkeeping, just re-enumerate everything.
			closeJoysticks();
			initJoysticks();
			log.info("Joystick change detected, re-enumerated: {}", joystickMax);
		}
	}

	/**
	 * Whether Escape was pressed this frame (excluding key-repeat). Useful for menu
	 * states that need a guaranteed back-to-previous-screen affordance regardless of
	 * how BUTTON_B is configured in the user's keymap.
	 */
	public static boolean isEscapePushedThisFrame() {
		for(KeyEvent ev : frameKeyEvents) {
			if(ev.scancode == SDLConstants.SDL_SCANCODE_ESCAPE && !ev.repeat) return true;
		}
		return false;
	}

	/**
	 * Consume (read and clear) all text typed since last call. Intended for a focused text-input widget.
	 * @return UTF-8 text that was committed this frame, or empty string if none
	 */
	public static String consumeTextInput() {
		if(pendingTextInput.length() == 0) return "";
		String s = pendingTextInput.toString();
		pendingTextInput.setLength(0);
		return s;
	}

	/**
	 * Read the system clipboard as UTF-8.
	 * @return clipboard text, or empty string if empty/unavailable
	 */
	public static String getClipboardText() {
		String text = SDL3.INSTANCE.SDL_GetClipboardText();
		return text == null ? "" : text;
	}

	/**
	 * Write a string to the system clipboard (UTF-8).
	 * @param text new clipboard contents (null treated as empty)
	 */
	public static void setClipboardText(String text) {
		SDL3.INSTANCE.SDL_SetClipboardText(text == null ? "" : text);
	}

	/**
	 * Enable SDL text input on the main window. Called by text-input widgets on focus.
	 * Sets {@link #textInputActive} so game-key input is suppressed while typing.
	 * @param rect input area (used by IMEs for composition window placement); may be null
	 */
	public static void startTextInput(SDLStructs.SDL_Rect rect) {
		if(window == null) return;
		textInputActive = true;
		SDL3.INSTANCE.SDL_StartTextInput(window);
		if(rect != null) SDL3.INSTANCE.SDL_SetTextInputArea(window, rect, 0);
	}

	/**
	 * Disable SDL text input. Called by text-input widgets on focus loss.
	 *
	 * Also pre-charges {@link GameKeySDL#gamekey}'s inputstate for any
	 * nav-mapped scancode currently held down. While textInputActive was
	 * true the main loop fed GameKeySDL an empty keyboard array, so
	 * inputstate sat at 0; the next unmasked update would otherwise step
	 * from 0→1 on held keys and register as a fresh {@code isPushKey}
	 * press in whichever state we've transitioned into. For instance:
	 * Escape-to-leave the netplay lobby while chat has focus would then
	 * trigger the title screen's cancel-to-exit path on the very next
	 * frame, quitting the game on one keypress. Seeding inputstate to 2
	 * means the follow-up update increments to 3 and isPushKey stays
	 * false until the user physically releases and presses again.
	 */
	public static void stopTextInput() {
		textInputActive = false;
		imeComposition = "";
		imeCompositionStart = 0;
		imeCompositionLength = 0;

		for(int p = 0; p < GameKeySDL.gamekey.length; p++) {
			GameKeySDL gk = GameKeySDL.gamekey[p];
			if(gk == null) continue;
			for(int b = 0; b < GameKeyDummy.MAX_BUTTON; b++) {
				int sc = gk.keymapNav[b];
				if(sc >= 0 && sc < keyPressedState.length && keyPressedState[sc]) {
					gk.setInputState(b, 2);
				}
			}
		}

		if(window == null) return;
		SDL3.INSTANCE.SDL_StopTextInput(window);
	}

	/**
	 * Joystick state update
	 */
	protected static void joyUpdate() {
		try {
			for(int i = 0; i < joystickMax; i++) {
				if(gamepad[i] == null) continue;

				joyAxisX[i] = deadzone(SDL3.INSTANCE.SDL_GetGamepadAxis(gamepad[i], SDLConstants.SDL_GAMEPAD_AXIS_LEFTX));
				joyAxisY[i] = deadzone(SDL3.INSTANCE.SDL_GetGamepadAxis(gamepad[i], SDLConstants.SDL_GAMEPAD_AXIS_LEFTY));

				for(int j = 0; j < SDLConstants.SDL_GAMEPAD_NUM_BUTTONS; j++) {
					joyPressedState[i][j] = SDL3.INSTANCE.SDL_GetGamepadButton(gamepad[i], j) != 0;
				}

				joyHatState[i] = hatFromDpad(
					joyPressedState[i][SDLConstants.SDL_GAMEPAD_BUTTON_DPAD_UP],
					joyPressedState[i][SDLConstants.SDL_GAMEPAD_BUTTON_DPAD_DOWN],
					joyPressedState[i][SDLConstants.SDL_GAMEPAD_BUTTON_DPAD_LEFT],
					joyPressedState[i][SDLConstants.SDL_GAMEPAD_BUTTON_DPAD_RIGHT]);
			}
		} catch (Throwable e) {
			log.warn("Gamepad state update failed", e);
		}
	}

	/** Fold d-pad buttons into the SDL hat bitmask GameKeySDL expects. */
	static int hatFromDpad(boolean up, boolean down, boolean left, boolean right) {
		return (up ? GameKeySDL.SDL_HAT_UP : 0) | (down ? GameKeySDL.SDL_HAT_DOWN : 0)
			| (left ? GameKeySDL.SDL_HAT_LEFT : 0) | (right ? GameKeySDL.SDL_HAT_RIGHT : 0);
	}

	// ponytail: fixed deadzone floor because the default joyBorder is 0 and SDL
	// documents sticks resting within ~8000 of center; the user's JOYSTICK
	// BORDER setting still applies on top in GameKeySDL.
	static int deadzone(short v) {
		return Math.abs(v) < 8192 ? 0 : v;
	}

	/** Display names for SDL_GamepadButton ordinals 0-14 (Xbox/PS combined). */
	static final String[] GAMEPAD_BUTTON_NAMES = {
		"A/CROSS", "B/CIRCLE", "X/SQUARE", "Y/TRIANGLE",
		"BACK/SHARE", "GUIDE", "START/OPTIONS", "L3", "R3",
		"LB/L1", "RB/R1",
		"D-PAD UP", "D-PAD DOWN", "D-PAD LEFT", "D-PAD RIGHT",
	};

	/** @return display name for a gamepad button ordinal; -1/out-of-range → "NONE" */
	static String gamepadButtonName(int button) {
		return (button >= 0 && button < GAMEPAD_BUTTON_NAMES.length) ? GAMEPAD_BUTTON_NAMES[button] : "NONE";
	}

	/** Normalize a device name for the 40-col bitmap font: uppercase, max 24
	 *  chars (web Gamepad ids are long, e.g. "Xbox 360 Controller (XInput
	 *  STANDARD GAMEPAD Vendor: 045e ...)"). */
	static String padDisplayName(String raw) {
		if(raw == null || raw.isEmpty()) return "UNKNOWN";
		String s = raw.toUpperCase(Locale.ROOT);
		return s.length() <= 24 ? s : s.substring(0, 24);
	}

	/**
	 * FPS calculation
	 * @param period FPS interval
	 */
	protected static void calcFPS(long period) {
		frameCount++;
		calcInterval += period;

		if(calcInterval >= 1000000000L) {
			long timeNow = nanoTime.getAsLong();
			long realElapsedTime = timeNow - prevCalcTime;
			actualFPS = ((double) frameCount / realElapsedTime) * 1000000000L;
			frameCount = 0L;
			calcInterval = 0L;
			prevCalcTime = timeNow;
		}
	}

	/**
	 * Leave the P2P room session, if one is active
	 */
	public static void stopRoomSession() {
		log.debug("stopRoomSession called");

		if(roomSession != null) {
			try {
				roomSession.shutdown();
			} catch (Throwable e) {
				log.warn("room session shutdown failed", e);
			}
			roomSession = null;
		}
	}

}
