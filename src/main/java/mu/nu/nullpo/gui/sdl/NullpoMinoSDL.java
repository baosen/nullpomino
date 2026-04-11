/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
        * Neither the name of NullNoname nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package mu.nu.nullpo.gui.sdl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import javax.swing.JOptionPane;

import mu.nu.nullpo.game.net.NetObserverClient;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.util.CustomProperties;
import mu.nu.nullpo.util.ModeManager;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import sdljava.SDLException;
import sdljava.SDLMain;
import sdljava.SDLVersion;
import sdljava.event.SDLEvent;
import sdljava.event.SDLKey;
import sdljava.event.SDLKeyboardEvent;
import sdljava.event.SDLQuitEvent;
import sdljava.event.SDLResizeEvent;
import sdljava.video.SDLGrabMode;
import sdljava.video.SDLPixelFormat;
import sdljava.video.SDLRect;
import sdljava.joystick.HatState;
import sdljava.joystick.SDLJoystick;
import sdljava.mixer.SDLMixer;
import sdljava.ttf.SDLTTF;
import sdljava.video.SDLSurface;
import sdljava.video.SDLVideo;

/**
 * NullpoMino SDLVersion
 */
public class NullpoMinoSDL {
	/** Log */
	static Logger log = Logger.getLogger(NullpoMinoSDL.class);

	/** SDL key names */
	public static final String[] SDL_KEYNAMES =
	{
		"NONE","(1)","(2)","(3)","(4)","(5)","(6)","(7)","BACKSPACE","TAB","(10)","(11)","CLEAR","RETURN",
		"(14)","(15)","(16)","(17)","(18)","PAUSE","(20)","(21)","(22)","(23)","(24)","(25)","(26)","ESCAPE",
		"(28)","(29)","(30)","(31)","SPACE","EXCLAIM","QUOTEDBL","HASH","DOLLAR","(37)","AMPERSAND","QUOTE",
		"LEFTPAREN","RIGHTPAREN","ASTERISK","PLUS","COMMA","MINUS","PERIOD","SLASH","0","1","2","3","4","5",
		"6","7","8","9","COLON","SEMICOLON","LESS","EQUALS","GREATER","QUESTION","AT","(65)","(66)","(67)",
		"(68)","(69)","(70)","(71)","(72)","(73)","(74)","(75)","(76)","(77)","(78)","(79)","(80)","(81)",
		"(82)","(83)","(84)","(85)","(86)","(87)","(88)","(89)","(90)","LEFTBRACKET","BACKSLASH","RIGHTBRACKET",
		"CARET","UNDERSCORE","BACKQUOTE","A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R",
		"S","T","U","V","W","X","Y","Z","(123)","(124)","(125)","(126)","DELETE","(128)","(129)","(130)","(131)",
		"(132)","(133)","(134)","(135)","(136)","(137)","(138)","(139)","(140)","(141)","(142)","(143)","(144)",
		"(145)","(146)","(147)","(148)","(149)","(150)","(151)","(152)","(153)","(154)","(155)","(156)","(157)",
		"(158)","(159)","WORLD_0","WORLD_1","WORLD_2","WORLD_3","WORLD_4","WORLD_5","WORLD_6","WORLD_7","WORLD_8",
		"WORLD_9","WORLD_10","WORLD_11","WORLD_12","WORLD_13","WORLD_14","WORLD_15","WORLD_16","WORLD_17","WORLD_18",
		"WORLD_19","WORLD_20","WORLD_21","WORLD_22","WORLD_23","WORLD_24","WORLD_25","WORLD_26","WORLD_27","WORLD_28",
		"WORLD_29","WORLD_30","WORLD_31","WORLD_32","WORLD_33","WORLD_34","WORLD_35","WORLD_36","WORLD_37","WORLD_38",
		"WORLD_39","WORLD_40","WORLD_41","WORLD_42","WORLD_43","WORLD_44","WORLD_45","WORLD_46","WORLD_47","WORLD_48",
		"WORLD_49","WORLD_50","WORLD_51","WORLD_52","WORLD_53","WORLD_54","WORLD_55","WORLD_56","WORLD_57","WORLD_58",
		"WORLD_59","WORLD_60","WORLD_61","WORLD_62","WORLD_63","WORLD_64","WORLD_65","WORLD_66","WORLD_67","WORLD_68",
		"WORLD_69","WORLD_70","WORLD_71","WORLD_72","WORLD_73","WORLD_74","WORLD_75","WORLD_76","WORLD_77","WORLD_78",
		"WORLD_79","WORLD_80","WORLD_81","WORLD_82","WORLD_83","WORLD_84","WORLD_85","WORLD_86","WORLD_87","WORLD_88",
		"WORLD_89","WORLD_90","WORLD_91","WORLD_92","WORLD_93","WORLD_94","WORLD_95","KP0","KP1","KP2","KP3","KP4",
		"KP5","KP6","KP7","KP8","KP9","KP_PERIOD","KP_DIVIDE","KP_MULTIPLY","KP_MINUS","KP_PLUS","KP_ENTER","KP_EQUALS",
		"UP","DOWN","RIGHT","LEFT","INSERT","HOME","END","PAGEUP","PAGEDOWN","F1","F2","F3","F4","F5","F6","F7","F8","F9",
		"F10","F11","F12","F13","F14","F15","(297)","(298)","(299)","NUMLOCK","CAPSLOCK","SCROLLOCK","RSHIFT","LSHIFT","RCTRL",
		"LCTRL","RALT","LALT","RMETA","LMETA","LSUPER","RSUPER","MODE","COMPOSE","HELP","PRINT","SYSREQ","BREAK","MENU",
		"POWER","EURO","UNDO"
	};

	/** State of the gameID */
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
							STATE_SELECTMODEFOLDER = 18;

	/** State of the gamecount */
	public static final int STATE_MAX = 19;

	/** To recognize the keyMaximumValue */
	public static final int SDL_KEY_MAX = 322;

	/** Command that was passed to the programLinesArgumentcount */
	public static String[] programArgs;

	/** Save settingsUseProperty file */
	public static CustomProperties propConfig;

	/** Save settingsUseProperty file (AllVersionCommon) */
	public static CustomProperties propGlobal;

	/** Music ListProperty file */
	public static CustomProperties propMusic;

	/** ObserverFor the functionProperty file */
	public static CustomProperties propObserver;

	/** Default language file */
	public static CustomProperties propLangDefault;

	/** Language file */
	public static CustomProperties propLang;

	/** Default game mode description file */
	public static CustomProperties propDefaultModeDesc;

	/** Game mode description file */
	public static CustomProperties propModeDesc;

	/** Mode Management */
	public static ModeManager modeManager;

	/** End flag */
	public static boolean quit = false;

	/** FPSDisplay */
	public static boolean showfps = true;

	/** FPSFor calculation */
	protected static long calcInterval = 0;

	/** FPSFor calculation */
	protected static long prevCalcTime = 0;

	/**  frame count */
	protected static long frameCount = 0;

	/** ActualFPS */
	public static double actualFPS = 0.0;

	/** FPSDisplayDecimalFormat */
	public static DecimalFormat df = new DecimalFormat("0.0");

	/** Used by perfect fps mode */
	public static long perfectFPSDelay = 0;

	/** True to use perfect FPS */
	public static boolean perfectFPSMode = false;

	/** Execute Thread.yield() during Perfect FPS mode */
	public static boolean perfectYield = true;

	/** If you hold down the keytrue */
	public static boolean[] keyPressedState;

	/** UseJoystick Of number */
	public static int[] joyUseNumber;

	/** Joystick Ignore analog sticks */
	public static boolean[] joyIgnoreAxis;

	/** Joystick Hat switch ignores */
	public static boolean[] joyIgnorePOV;

	/** Joystick Ofcount */
	public static int joystickMax;

	/** Joystick */
	public static SDLJoystick[] joystick;

	/** Joystick direction key State */
	public static int[] joyAxisX, joyAxisY;

	/** Joystick Hat switchcount */
	public static int[] joyMaxHat;

	/** Joystick Hat switch state */
	public static HatState[] joyHatState;

	/** Joystick Of buttonOfcount */
	public static int[] joyMaxButton;

	/** Joystick Of buttonIf you press thetrue */
	public static boolean[][] joyPressedState;

	/** State game */
	public static BaseStateSDL[] gameStates;

	/** Current State */
	public static int currentState;

	/** In-game flag (if false, Perfect FPS will not used) */
	public static boolean isInGame;

	/** Exit buttonYaScreenshot buttonPermission to use */
	public static boolean enableSpecialKeys;

	/** Exit buttonLicense */
	public static boolean allowQuit;

	/** true if disable automatic input update */
	public static boolean disableAutoInputUpdate;

	/** Current fullscreen state */
	public static boolean fullscreen;

	public static final int LOGICAL_WIDTH = 640;
	public static final int LOGICAL_HEIGHT = 480;

	/** Common fullscreen fallback sizes used if the requested mode is not advertised. */
	private static final int[][] FULLSCREEN_SIZE_CANDIDATES =
	{
		{320, 240}, {400, 300}, {480, 360}, {512, 384}, {640, 480}, {800, 480}, {800, 600}, {854, 480},
		{960, 540}, {960, 600}, {1024, 576}, {1024, 600}, {1024, 768}, {1152, 648}, {1152, 720},
		{1152, 864}, {1280, 720}, {1280, 768}, {1280, 800}, {1280, 960}, {1280, 1024}, {1360, 768},
		{1366, 768}, {1400, 900}, {1440, 810}, {1440, 900}, {1440, 1080}, {1600, 900}, {1600, 1200},
		{1680, 1050}, {1920, 1080}, {1920, 1200}, {2048, 1536}, {2560, 1440}, {2560, 1600}, {2560, 1920},
		{2880, 1800}, {2880, 2160}
	};

	private static int windowedModeWidth = LOGICAL_WIDTH;
	private static int windowedModeHeight = LOGICAL_HEIGHT;

	/** Off-screen 640x480 surface for game rendering (scaled to window on flip) */
	public static SDLSurface gameSurface;

	private static int renderOffsetX;
	private static int renderOffsetY;
	private static int renderWidth = LOGICAL_WIDTH;
	private static int renderHeight = LOGICAL_HEIGHT;

	/** Precomputed source X indices for nearest-neighbor scaling (length = renderWidth) */
	private static int[] scaleMapX;

	/** Precomputed source Y indices for nearest-neighbor scaling (length = renderHeight) */
	private static int[] scaleMapY;

	private static int gameSurfaceBpp;

	/** Previous F11 key state for edge detection */
	private static boolean prevF11Pressed;

	/** MaximumFPS */
	public static int maxFPS;

	/** ObserverClient */
	public static NetObserverClient netObserverClient;

	/**
	 * Main functioncount
	 * @param args Argument that was passed to the programcount
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("config/etc/log_sdl.cfg");
		log.info("NullpoMinoSDL Start");

		programArgs = args;
		propConfig = new CustomProperties();
		propGlobal = new CustomProperties();
		propMusic = new CustomProperties();

		// Read configuration file
		try {
			FileInputStream in = new FileInputStream("config/setting/sdl.cfg");
			propConfig.load(in);
			in.close();
		} catch(IOException e) {}
		try {
			FileInputStream in = new FileInputStream("config/setting/global.cfg");
			propGlobal.load(in);
			in.close();
		} catch(IOException e) {}
		try {
			FileInputStream in = new FileInputStream("config/setting/music.cfg");
			propMusic.load(in);
			in.close();
		} catch(IOException e) {}

		// Read language file
		propLangDefault = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/lang/sdl_default.properties");
			propLangDefault.load(in);
			in.close();
		} catch (IOException e) {
			log.error("Failed to load default UI language file", e);
		}

		propLang = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/lang/sdl_" + Locale.getDefault().getCountry() + ".properties");
			propLang.load(in);
			in.close();
		} catch(IOException e) {}

		// Game mode description
		propDefaultModeDesc = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/lang/modedesc_default.properties");
			propDefaultModeDesc.load(in);
			in.close();
		} catch(IOException e) {
			log.error("Couldn't load default mode description file", e);
		}

		propModeDesc = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/lang/modedesc_" + Locale.getDefault().getCountry() + ".properties");
			propModeDesc.load(in);
			in.close();
		} catch(IOException e) {}

		// ModeRead
		modeManager = new ModeManager();
		try {
			BufferedReader txtMode = new BufferedReader(new FileReader("config/list/mode.lst"));
			modeManager.loadGameModes(txtMode);
			txtMode.close();
		} catch (IOException e) {
			log.error("Failed to load game mode list", e);
		}

		// Set default rule selections
		try {
			CustomProperties propDefaultRule = new CustomProperties();
			FileInputStream in = new FileInputStream("config/list/global_defaultrule.properties");
			propDefaultRule.load(in);
			in.close();

			for(int pl = 0; pl < 2; pl++)
				for(int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
					// TETROMINO
					if(i == 0) {
						if(propGlobal.getProperty(pl + ".rule") == null) {
							propGlobal.setProperty(pl + ".rule", propDefaultRule.getProperty("default.rule", ""));
							propGlobal.setProperty(pl + ".rulefile", propDefaultRule.getProperty("default.rulefile", ""));
							propGlobal.setProperty(pl + ".rulename", propDefaultRule.getProperty("default.rulename", ""));
						}
					}
					// etc
					else {
						if(propGlobal.getProperty(pl + ".rule." + i) == null) {
							propGlobal.setProperty(pl + ".rule." + i, propDefaultRule.getProperty("default.rule." + i, ""));
							propGlobal.setProperty(pl + ".rulefile." + i, propDefaultRule.getProperty("default.rulefile." + i, ""));
							propGlobal.setProperty(pl + ".rulename." + i, propDefaultRule.getProperty("default.rulename." + i, ""));
						}
					}
				}
		} catch (Exception e) {}

		// Key input OfInitialization
		keyPressedState = new boolean[SDL_KEY_MAX];
		GameKeySDL.initGlobalGameKeySDL();
		GameKeySDL.gamekey[0].loadConfig(propConfig);
		GameKeySDL.gamekey[1].loadConfig(propConfig);
		MouseInputSDL.initalizeMouseInput();

		// State initialization
		currentState = -1;
		gameStates = new BaseStateSDL[STATE_MAX];
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
		gameStates[STATE_SELECTMODEFOLDER] = new StateSelectModeFolderSDL();

		// Mac? Too bad.
		if(System.getProperty("os.name").contains("Mac OS")) {
			String strErrorTitle = getUIText("InitFailedMessageMac_Title");
			String strErrorMessage = getUIText("InitFailedMessageMac_Body");
			JOptionPane.showMessageDialog(null, strErrorMessage, strErrorTitle, JOptionPane.ERROR_MESSAGE);
			System.exit(-2);
		}

		// SDL init
		try {
			init();
		} catch (Throwable e) {
			// Init failed
			log.fatal("SDL init failed", e);

			// Show error dialog. But unfortunately, most SDL-related-failure are not catchable...
			// (Maybe) x64?
			if(System.getProperty("os.arch").contains("64")) {
				String strErrorTitle = getUIText("InitFailedMessage64bit_Title");
				String strErrorMessage = String.format(getUIText("InitFailedMessage64bit_Body"), e.toString());
				JOptionPane.showMessageDialog(null, strErrorMessage, strErrorTitle, JOptionPane.ERROR_MESSAGE);
			}
			// Other
			else {
				String strErrorTitle = getUIText("InitFailedMessageGeneral_Title");
				String strErrorMessage = String.format(getUIText("InitFailedMessageGeneral_Body"), e.toString());
				JOptionPane.showMessageDialog(null, strErrorMessage, strErrorTitle, JOptionPane.ERROR_MESSAGE);
			}

			System.exit(-1);
		}

		// Run
		try {
			run();
		} catch (Throwable e) {
			log.fatal("Uncaught Exception", e);
		} finally {
			shutdown();
		}

		System.exit(0);
	}

	/**
	 * SDLOfInitialization
	 * @throws SDLException SDLIf an error has occurred
	 */
	public static void init() throws SDLException {
		log.info("Now initializing SDL...");

		SDLMain.init(SDLMain.SDL_INIT_VIDEO | SDLMain.SDL_INIT_AUDIO | SDLMain.SDL_INIT_JOYSTICK);

		SDLVersion ver = SDLMain.getSDLVersion();
		log.info("SDL Version:" + ver.getMajor() + "." + ver.getMinor() + "." + ver.getPatch());

		SDLVideo.wmSetCaption("NullpoMino (Now Loading...)", null);

		fullscreen = propConfig.getProperty("option.fullscreen", false);
		applyVideoMode(640, 480);

		SDLTTF.init();
		SDLVersion ttfver = SDLTTF.getTTFVersion();
		log.info("TTF Version:" + ttfver.getMajor() + "." + ttfver.getMinor() + "." + ttfver.getPatch());

		SDLMixer.openAudio(44100, SDLMixer.AUDIO_S16SYS, 2, propConfig.getProperty("option.soundbuffer", 1024));

		joyUseNumber = new int[2];
		joyUseNumber[0] = propConfig.getProperty("joyUseNumber.p0", -1);
		joyUseNumber[1] = propConfig.getProperty("joyUseNumber.p1", -1);
		joyIgnoreAxis = new boolean[2];
		joyIgnoreAxis[0] = propConfig.getProperty("joyIgnoreAxis.p0", false);
		joyIgnoreAxis[1] = propConfig.getProperty("joyIgnoreAxis.p1", false);
		joyIgnorePOV = new boolean[2];
		joyIgnorePOV[0] = propConfig.getProperty("joyIgnorePOV.p0", false);
		joyIgnorePOV[1] = propConfig.getProperty("joyIgnorePOV.p1", false);

		joystickMax = SDLJoystick.numJoysticks();
		log.info("Number of Joysticks:" + joystickMax);

		if(joystickMax > 0) {
			joystick = new SDLJoystick[joystickMax];
			joyAxisX = new int[joystickMax];
			joyAxisY = new int[joystickMax];
			joyMaxHat = new int[joystickMax];
			joyMaxButton = new int[joystickMax];
			joyHatState = new HatState[joystickMax];

			int max = 0;

			for(int i = 0; i < joystickMax; i++) {
				try {
					joystick[i] = SDLJoystick.joystickOpen(i);

					joyMaxButton[i] = joystick[i].joystickNumButtons();
					if(joyMaxButton[i] > max) max = joyMaxButton[i];

					joyMaxHat[i] = joystick[i].joystickNumHats();
					joyHatState[i] = null;
				} catch (Throwable e) {
					log.warn("Failed to open Joystick #" + i, e);
				}
			}

			joyPressedState = new boolean[joystickMax][max];
		}
	}

	/**
	 * Toggle fullscreen mode.
	 * @throws SDLException if an SDL error occurs
	 */
	public static void toggleFullscreen() throws SDLException {
		if(!fullscreen) {
			rememberWindowedModeSize(SDLVideo.getVideoSurface());
		}
		fullscreen = !fullscreen;
		applyVideoMode(windowedModeWidth, windowedModeHeight);
		propConfig.setProperty("option.fullscreen", fullscreen);
		saveConfig();
		log.debug("Fullscreen toggled: " + fullscreen);
	}

	/**
	 * Apply SDL video mode flags and set the video mode.
	 */
	private static void applyVideoMode(int width, int height) throws SDLException {
		long flags = SDLVideo.SDL_ANYFORMAT | SDLVideo.SDL_DOUBLEBUF | SDLVideo.SDL_HWSURFACE;
		SDLSurface surface;
		if(fullscreen) {
			flags |= SDLVideo.SDL_FULLSCREEN;
			surface = setFullscreenVideoMode(width, height, flags);
		} else {
			flags |= SDLVideo.SDL_RESIZABLE;
			surface = SDLVideo.setVideoMode(width, height, 0, flags);
			rememberWindowedModeSize(surface);
			SDLVideo.wmGrabInput(SDLGrabMode.SDL_GRAB_OFF);
		}
		updateRenderViewport(surface);
		recreateGameSurfaceIfNeeded(surface);
	}

	/**
	 * Remember the last successful windowed-mode size so fullscreen can round-trip back to it.
	 */
	private static void rememberWindowedModeSize(SDLSurface surface) {
		if(surface == null) return;
		windowedModeWidth = Math.max(1, surface.getWidth());
		windowedModeHeight = Math.max(1, surface.getHeight());
	}

	/**
	 * Try the requested fullscreen mode first, then fall back to safer alternatives only if it fails.
	 */
	private static SDLSurface setFullscreenVideoMode(int requestedWidth, int requestedHeight, long flags) throws SDLException {
		SDLException requestedFailure;
		try {
			return SDLVideo.setVideoMode(requestedWidth, requestedHeight, 0, flags);
		} catch (SDLException e) {
			requestedFailure = e;
		}

		int configuredWidth = propConfig.getProperty("option.screenwidth", LOGICAL_WIDTH);
		int configuredHeight = propConfig.getProperty("option.screenheight", LOGICAL_HEIGHT);
		if((configuredWidth != requestedWidth) || (configuredHeight != requestedHeight)) {
			try {
				SDLSurface surface = SDLVideo.setVideoMode(configuredWidth, configuredHeight, 0, flags);
				log.warn("Fullscreen mode " + requestedWidth + "x" + requestedHeight +
					" failed; using configured mode " + configuredWidth + "x" + configuredHeight);
				return surface;
			} catch (SDLException e) {
				// Try the next fullscreen fallback.
			}
		}

		int bitsPerPixel = getCurrentVideoBitsPerPixel();
		int[] fallbackSize = findClosestSupportedFullscreenMode(requestedWidth, requestedHeight, bitsPerPixel, flags);
		if((fallbackSize != null) &&
			((fallbackSize[0] != requestedWidth) || (fallbackSize[1] != requestedHeight)) &&
			((fallbackSize[0] != configuredWidth) || (fallbackSize[1] != configuredHeight))) {
			try {
				SDLSurface surface = SDLVideo.setVideoMode(fallbackSize[0], fallbackSize[1], 0, flags);
				log.warn("Fullscreen mode " + requestedWidth + "x" + requestedHeight +
					" failed; using fallback mode " + fallbackSize[0] + "x" + fallbackSize[1]);
				return surface;
			} catch (SDLException e) {
				// No more fullscreen fallbacks remain after this.
			}
		}

		throw requestedFailure;
	}

	/**
	 * Return the current video surface bit depth, or 0 if no surface exists yet.
	 */
	private static int getCurrentVideoBitsPerPixel() {
		try {
			SDLSurface currentSurface = SDLVideo.getVideoSurface();
			if(currentSurface != null) {
				return currentSurface.getFormat().getBitsPerPixel();
			}
		} catch (SDLException e) {
			// No video surface exists yet; allow SDL to choose the display format.
		}
		return 0;
	}

	/**
	 * Check whether SDL advertises a fullscreen mode for the requested size.
	 */
	private static boolean isSupportedFullscreenMode(int width, int height, int bitsPerPixel, long flags) throws SDLException {
		if((width <= 0) || (height <= 0)) return false;
		return SDLVideo.videoModeOK(width, height, bitsPerPixel, getFullscreenModeCheckFlags(flags)) != 0;
	}

	/**
	 * Ask SDL whether a fullscreen resolution exists without requiring hardware-surface hints.
	 */
	private static int getFullscreenModeCheckFlags(long flags) {
		int checkFlags = (int) SDLVideo.SDL_FULLSCREEN;
		if((flags & SDLVideo.SDL_ANYFORMAT) != 0) checkFlags |= (int) SDLVideo.SDL_ANYFORMAT;
		return checkFlags;
	}

	/**
	 * Pick the closest supported mode from the common screen-size table.
	 */
	private static int[] findClosestSupportedFullscreenMode(int requestedWidth, int requestedHeight, int bitsPerPixel, long flags) throws SDLException {
		int closestWidth = -1;
		int closestHeight = -1;
		long closestDistance = Long.MAX_VALUE;

		for(int i = 0; i < FULLSCREEN_SIZE_CANDIDATES.length; i++) {
			int width = FULLSCREEN_SIZE_CANDIDATES[i][0];
			int height = FULLSCREEN_SIZE_CANDIDATES[i][1];
			if(!isSupportedFullscreenMode(width, height, bitsPerPixel, flags)) continue;

			long widthDelta = width - requestedWidth;
			long heightDelta = height - requestedHeight;
			long distance = widthDelta * widthDelta + heightDelta * heightDelta;
			if(distance < closestDistance) {
				closestWidth = width;
				closestHeight = height;
				closestDistance = distance;
			}
		}

		return (closestWidth >= 0) ? new int[] {closestWidth, closestHeight} : null;
	}

	/**
	 * Nearest-neighbor scale blit from gameSurface onto dst using the current
	 * render viewport and precomputed scaleMapX/scaleMapY lookup tables.
	 */
	private static void scaleBlitToSurface(SDLSurface dst) throws SDLException {
		int bpp = gameSurfaceBpp;
		int dstX = renderOffsetX;
		int dstY = renderOffsetY;
		int dstW = renderWidth;
		int dstH = renderHeight;

		if(!gameSurface.lockSurface()) return;
		try {
			if(!dst.lockSurface()) return;
			try {
				ByteBuffer srcBuf = gameSurface.getPixelData();
				ByteBuffer dstBuf = dst.getPixelData();
				srcBuf.order(ByteOrder.nativeOrder());
				dstBuf.order(ByteOrder.nativeOrder());
				int srcPitch = gameSurface.getPitch();
				int dstPitch = dst.getPitch();

				if(bpp == 4) {
					for(int y = 0; y < dstH; y++) {
						int srcRowOff = scaleMapY[y] * srcPitch;
						int dstRowOff = (dstY + y) * dstPitch + dstX * 4;
						for(int x = 0; x < dstW; x++) {
							dstBuf.putInt(dstRowOff + x * 4, srcBuf.getInt(srcRowOff + scaleMapX[x] * 4));
						}
					}
				} else if(bpp == 2) {
					for(int y = 0; y < dstH; y++) {
						int srcRowOff = scaleMapY[y] * srcPitch;
						int dstRowOff = (dstY + y) * dstPitch + dstX * 2;
						for(int x = 0; x < dstW; x++) {
							dstBuf.putShort(dstRowOff + x * 2, srcBuf.getShort(srcRowOff + scaleMapX[x] * 2));
						}
					}
				} else {
					for(int y = 0; y < dstH; y++) {
						int srcRowOff = scaleMapY[y] * srcPitch;
						int dstRowOff = (dstY + y) * dstPitch + dstX * bpp;
						for(int x = 0; x < dstW; x++) {
							int si = srcRowOff + scaleMapX[x] * bpp;
							int di = dstRowOff + x * bpp;
							for(int b = 0; b < bpp; b++) {
								dstBuf.put(di + b, srcBuf.get(si + b));
							}
						}
					}
				}
			} finally {
				dst.unlockSurface();
			}
		} finally {
			gameSurface.unlockSurface();
		}
	}

	/**
	 * Recreate the off-screen game surface when the display format changes.
	 */
	private static void recreateGameSurfaceIfNeeded(SDLSurface surface) throws SDLException {
		if((gameSurface != null) && hasSamePixelFormat(gameSurface, surface)) return;

		SDLPixelFormat format = surface.getFormat();
		SDLSurface newGameSurface = SDLVideo.createRGBSurface(
			SDLVideo.SDL_SWSURFACE,
			LOGICAL_WIDTH,
			LOGICAL_HEIGHT,
			format.getBitsPerPixel(),
			format.getRMask(),
			format.getGMask(),
			format.getBMask(),
			format.getAMask()
		);

		if(gameSurface != null) {
			gameSurface.freeSurface();
		}
		gameSurface = newGameSurface;
		gameSurfaceBpp = format.getBytesPerPixel();
		NormalFontSDL.dest = gameSurface;
		notifyGameSurfaceChanged();
	}

	/**
	 * Notify all initialized states that the shared game surface was recreated.
	 * All states must be notified — not just the current one — because inactive
	 * states may hold a stale (freed) surface reference in their GameManager.
	 */
	private static void notifyGameSurfaceChanged() {
		if(gameStates == null) return;
		for(int i = 0; i < STATE_MAX; i++) {
			if(gameStates[i] != null) {
				gameStates[i].onGameSurfaceChanged(gameSurface);
			}
		}
	}

	/**
	 * Update the letterboxed destination rectangle for the logical 640x480 framebuffer.
	 */
	private static void updateRenderViewport(SDLSurface surface) {
		int winW = surface.getWidth();
		int winH = surface.getHeight();

		double scale = Math.min((double)winW / LOGICAL_WIDTH, (double)winH / LOGICAL_HEIGHT);
		renderWidth = Math.max(1, (int)Math.round(LOGICAL_WIDTH * scale));
		renderHeight = Math.max(1, (int)Math.round(LOGICAL_HEIGHT * scale));
		renderOffsetX = (winW - renderWidth) / 2;
		renderOffsetY = (winH - renderHeight) / 2;

		if((renderWidth != LOGICAL_WIDTH) || (renderHeight != LOGICAL_HEIGHT)) {
			// Precompute source coordinate lookup tables for nearest-neighbor scaling
			if((scaleMapX == null) || (scaleMapX.length != renderWidth)) {
				scaleMapX = new int[renderWidth];
				for(int x = 0; x < renderWidth; x++) scaleMapX[x] = x * LOGICAL_WIDTH / renderWidth;
			}
			if((scaleMapY == null) || (scaleMapY.length != renderHeight)) {
				scaleMapY = new int[renderHeight];
				for(int y = 0; y < renderHeight; y++) scaleMapY[y] = y * LOGICAL_HEIGHT / renderHeight;
			}
		}
	}

	/**
	 * Check if 2 surfaces share the same pixel format.
	 */
	private static boolean hasSamePixelFormat(SDLSurface a, SDLSurface b) {
		SDLPixelFormat formatA = a.getFormat();
		SDLPixelFormat formatB = b.getFormat();
		return (formatA.getBitsPerPixel() == formatB.getBitsPerPixel()) &&
			(formatA.getRMask() == formatB.getRMask()) &&
			(formatA.getGMask() == formatB.getGMask()) &&
			(formatA.getBMask() == formatB.getBMask()) &&
			(formatA.getAMask() == formatB.getAMask());
	}

	/** Returns logical X, or -1 if outside the game viewport. */
	static int windowToLogicalX(int windowX) {
		return windowToLogicalCoordinate(windowX, renderOffsetX, renderWidth, LOGICAL_WIDTH);
	}

	/** Returns logical Y, or -1 if outside the game viewport. */
	static int windowToLogicalY(int windowY) {
		return windowToLogicalCoordinate(windowY, renderOffsetY, renderHeight, LOGICAL_HEIGHT);
	}

	private static int windowToLogicalCoordinate(int value, int offset, int size, int logicalSize) {
		if((size <= 0) || (value < offset) || (value >= offset + size)) return -1;
		return (value - offset) * logicalSize / size;
	}

	/**
	 * Main loop
	 * @throws SDLException SDLIf an error has occurred
	 */
	public static void run() throws SDLException {
		maxFPS = propConfig.getProperty("option.maxfps", 60);

		boolean sleepFlag;
		long period;
		long beforeTime, afterTime, timeDiff, sleepTime, sleepTimeInMillis;
		long overSleepTime = 0L;
		int noDelays = 0;

		showfps = propConfig.getProperty("option.showfps", true);
		perfectFPSMode = propConfig.getProperty("option.perfectFPSMode", false);
		perfectYield = propConfig.getProperty("option.perfectYield", false);

		beforeTime = System.nanoTime();
		prevCalcTime = beforeTime;

		quit = false;
		enableSpecialKeys = true;
		allowQuit = true;

		SDLSurface surface = SDLVideo.getVideoSurface();

		// Reading, such as image
		ResourceHolderSDL.load();
		NormalFontSDL.dest = gameSurface;

		// First run
		if(propConfig.getProperty("option.firstSetupMode", true) == true) {
			// Set various default settings here
			GameKeySDL.gamekey[0].loadDefaultKeymap();
			GameKeySDL.gamekey[0].saveConfig(propConfig);
			propConfig.setProperty("option.firstSetupMode", false);

			// Set default rotation button setting (only for first run)
			if(propGlobal.getProperty("global.firstSetupMode", true) == true) {
				for(int pl = 0; pl < 2; pl++) {
					if(propGlobal.getProperty(pl + ".tuning.owRotateButtonDefaultRight") == null) {
						propGlobal.setProperty(pl + ".tuning.owRotateButtonDefaultRight", 0);
					}
				}
				propGlobal.setProperty("global.firstSetupMode", false);
			}

			// Save settings
			saveConfig();

			// Go to title screen
			enterState(STATE_TITLE);
		}
		// Second+ run
		else {
			enterState(STATE_TITLE);
		}

		perfectFPSDelay = System.nanoTime();

		// Main loop
		while(quit == false) {
			//  event Processing
			processEvent();
			if(quit == true) break;

			// F11 fullscreen toggle (checked before key mapping to consume the event)
			boolean f11Pressed = keyPressedState[SDLKey.SDLK_F11];
			if(f11Pressed && !prevF11Pressed) {
				toggleFullscreen();
				keyPressedState[SDLKey.SDLK_F11] = false;
			}
			prevF11Pressed = f11Pressed;

			// Joystick Updates
			if(joystickMax > 0) joyUpdate();

			// Update key input states
			if(!disableAutoInputUpdate) {
				for(int i = 0; i < 2; i++) {
					int joynum = joyUseNumber[i];

					if((joystickMax > 0) && (joynum >= 0) && (joynum < joystickMax)) {
						GameKeySDL.gamekey[i].update(keyPressedState, joyPressedState[joynum], joyAxisX[joynum], joyAxisY[joynum], joyHatState[joynum]);
					} else {
						GameKeySDL.gamekey[i].update(keyPressedState);
					}
				}
			}

			// Processing is executed for each state
			gameStates[currentState].update();
			// Render game content to the off-screen 640x480 surface
			gameSurface.fillRect(0);
			gameStates[currentState].render(gameSurface);

			// FPSDrawing
			if(showfps) NormalFontSDL.printFont(0, 480 - 16, NullpoMinoSDL.df.format(NullpoMinoSDL.actualFPS), NormalFontSDL.COLOR_BLUE, 1.0f);

			// ObserverClient
			if((netObserverClient != null) && netObserverClient.isConnected()) {
				int fontcolor = NormalFontSDL.COLOR_BLUE;
				if(netObserverClient.getObserverCount() > 1) fontcolor = NormalFontSDL.COLOR_GREEN;
				if(netObserverClient.getObserverCount() > 0 && netObserverClient.getPlayerCount() > 0) fontcolor = NormalFontSDL.COLOR_RED;
				String strObserverInfo = String.format("%d/%d", netObserverClient.getObserverCount(), netObserverClient.getPlayerCount());
				String strObserverString = String.format("%40s", strObserverInfo);
				NormalFontSDL.printFont(0, 480 - 16, strObserverString, fontcolor);
			}

			// Special key
			if(enableSpecialKeys) {
				// Screenshot
				if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_SCREENSHOT) || GameKeySDL.gamekey[1].isPushKey(GameKeySDL.BUTTON_SCREENSHOT))
					saveScreenShot();

				// Exit button
				if(allowQuit) {
					if(GameKeySDL.gamekey[0].isPushKey(GameKeySDL.BUTTON_QUIT) || GameKeySDL.gamekey[1].isPushKey(GameKeySDL.BUTTON_QUIT))
						enterState(-1);
				}
			}

			// Scale game surface to window with letterboxing
			surface = SDLVideo.getVideoSurface();
			if((renderWidth == LOGICAL_WIDTH) && (renderHeight == LOGICAL_HEIGHT)) {
				gameSurface.blitSurface(surface);
			} else {
				if((renderOffsetX > 0) || (renderOffsetY > 0)) surface.fillRect(0);
				scaleBlitToSurface(surface);
			}

			// Displayed on the screen
			surface.flip();

			// FPS cap
			sleepFlag = false;

			afterTime = System.nanoTime();
			timeDiff = afterTime - beforeTime;

			period = (long) (1.0 / maxFPS * 1000000000);
			sleepTime = (period - timeDiff) - overSleepTime;
			sleepTimeInMillis = sleepTime / 1000000L;

			if((sleepTimeInMillis >= 4) && (!perfectFPSMode || !isInGame)) {
				// If it is possible to use sleep
				if(maxFPS > 0) {
					try {
						Thread.sleep(sleepTimeInMillis);
					} catch(InterruptedException e) {}
				}
				// sleep() oversleep
				overSleepTime = (System.nanoTime() - afterTime) - sleepTime;
				perfectFPSDelay = System.nanoTime();
				sleepFlag = true;
			} else if((perfectFPSMode && isInGame) || (sleepTime > 0)) {
				// Perfect FPS
				overSleepTime = 0L;
				if(perfectYield) {
					while(System.nanoTime() < perfectFPSDelay + 1000000000 / maxFPS) {Thread.yield();}
				} else {
					while(System.nanoTime() < perfectFPSDelay + 1000000000 / maxFPS) {}
				}
				perfectFPSDelay += 1000000000 / maxFPS;

				// Don't run in super fast after the heavy slowdown
				if(System.nanoTime() > perfectFPSDelay + 2000000000 / maxFPS) {
					perfectFPSDelay = System.nanoTime();
				}

				sleepFlag = true;
			}

			if(!sleepFlag) {
				// Impossible to sleep!
				overSleepTime = 0L;
				if(++noDelays >= 16) {
					Thread.yield();
					noDelays = 0;
				}
				perfectFPSDelay = System.nanoTime();
			}

			beforeTime = System.nanoTime();
			calcFPS(period);
		}
	}

	/**
	 * SDLEnd processing of
	 */
	public static void shutdown() {
		log.info("NullpoMinoSDL shutdown()");

		try {
			stopObserverClient();
			for(int i = 0; i < joystickMax; i++) {
				joystick[i].joystickClose();
			}
			if(gameSurface != null) {
				gameSurface.freeSurface();
				gameSurface = null;
			}
			SDLMixer.close();
			SDLMain.quit();
		} catch (Throwable e) {}
	}

	/**
	 * Switching state
	 * @param id Destination state switchingID (-1Ends at)
	 * @throws SDLException SDLIf an error has occurred
	 */
	public static void enterState(int id) throws SDLException {
		if((currentState >= 0) && (currentState < STATE_MAX) && (gameStates[currentState] != null)) {
			gameStates[currentState].leave();
		}
		if((id >= 0) && (id < STATE_MAX) && (gameStates[id] != null)) {
			currentState = id;
			gameStates[currentState].enter();
		} else if(id < 0) {
			quit = true;
		} else {
			throw new NullPointerException("Game state #" + id + " is null");
		}
	}

	/**
	 * Save the configuration file
	 */
	public static void saveConfig() {
		try {
			FileOutputStream out = new FileOutputStream("config/setting/sdl.cfg");
			propConfig.store(out, "NullpoMino SDL-frontend Config");
			out.close();
			log.debug("Saved SDL-frontend config");
		} catch(IOException e) {
			log.error("Failed to save SDL-specific config", e);
		}

		try {
			FileOutputStream out = new FileOutputStream("config/setting/global.cfg");
			propGlobal.store(out, "NullpoMino Global Config");
			out.close();
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
			FileInputStream in = new FileInputStream("config/setting/global.cfg");
			propGlobal.load(in);
			in.close();
		} catch(IOException e) {}
	}

	/**
	 * ScreenshotSave the
	 * @throws SDLException If I fail to save
	 */
	public static void saveScreenShot() throws SDLException {
		// FilenameI decided to
		String dir = NullpoMinoSDL.propGlobal.getProperty("custom.screenshot.directory", "ss");
		Calendar c = Calendar.getInstance();
		DateFormat dfm = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
		String filename = dir + "/" + dfm.format(c.getTime()) + ".bmp";
		log.info("Saving screenshot to " + filename);

		File ssfolder = new File(dir);
		if (!ssfolder.exists()) {
			if (ssfolder.mkdir()) {
				log.info("Created screenshot folder: " + dir);
			} else {
				log.info("Couldn't create screenshot folder at "+ dir);
			}
		}

		// Save to File
		gameSurface.saveBMP(filename);
	}

	/**
	 * So you can draw the image properly protrude outside the screenSDLRectModify the
	 * @param rectSrc FixSDLRect (Rendering source)
	 * @param rectDst FixSDLRect (Which to draw)
	 */
	public static void fixRect(SDLRect rectSrc, SDLRect rectDst) {
		if(rectSrc == null) return;
		if(rectDst == null) return;

		if(rectDst.x < 0) {
			int prevX = rectDst.x;
			rectDst.width += prevX;
			rectDst.x = 0;
			rectSrc.width += prevX;
			rectSrc.x -= prevX;
		}

		if(rectDst.y < 0) {
			int prevY = rectDst.y;
			rectDst.height += prevY;
			rectDst.y = 0;
			rectSrc.height += prevY;
			rectSrc.y -= prevY;
		}
	}

	/**
	 * PosttranslationalUIGets a string of
	 * @param str String
	 * @return PosttranslationalUIString (If you do not acceptstrReturns)
	 */
	public static String getUIText(String str) {
		String result = propLang.getProperty(str);
		if(result == null) {
			result = propLangDefault.getProperty(str, str);
		}
		return result;
	}

	/**
	 *  event Processing
	 * @throws SDLException SDLIf an error has occurred
	 */
	protected static void processEvent() throws SDLException {
		while(true) {
			SDLEvent event = SDLEvent.pollEvent();
			if(event == null) break;

			if(event instanceof SDLQuitEvent) {
				// Exit button
				enterState(-1);
			} else if(event instanceof SDLResizeEvent) {
				SDLResizeEvent resizeEvent = (SDLResizeEvent) event;
				applyVideoMode(resizeEvent.getWidth(), resizeEvent.getHeight());
			} else if(event instanceof SDLKeyboardEvent) {
				// Key input
				SDLKeyboardEvent keyevent = (SDLKeyboardEvent)event;

				int keysym = keyevent.getSym();

				if(keyevent.getType() == SDLKeyboardEvent.SDL_KEYDOWN) {
					if(keysym < keyPressedState.length) keyPressedState[keysym] = true;
				} else if(keyevent.getType() == SDLKeyboardEvent.SDL_KEYUP) {
					if(keysym < keyPressedState.length) keyPressedState[keysym] = false;
				}
			}
		}
	}

	/**
	 * Joystick  stateUpdates
	 */
	protected static void joyUpdate() {
		try {
			SDLJoystick.joystickUpdate();

			for(int i = 0; i < joystickMax; i++) {
				if(joyIgnoreAxis[i] == false) {
					joyAxisX[i] = joystick[i].joystickGetAxis(0);
					joyAxisY[i] = joystick[i].joystickGetAxis(1);
				} else {
					joyAxisX[i] = 0;
					joyAxisY[i] = 0;
				}

				for(int j = 0; j < joyMaxButton[i]; j++) {
					joyPressedState[i][j] = joystick[i].joystickGetButton(j);
				}

				if((joyMaxHat[i] > 0) && (joyIgnorePOV[i] == false)) {
					joyHatState[i] = joystick[i].joystickGetHat(0);
				} else {
					joyHatState[i] = null;
				}
			}
		} catch (Throwable e) {
			log.warn("Joystick state update failed", e);
		}
	}

	/**
	 * FPSCalculation of
	 * @param period FPSInterval to calculate the
	 */
	protected static void calcFPS(long period) {
		frameCount++;
		calcInterval += period;

		// 1Second intervalsFPSRecalculate the
		if(calcInterval >= 1000000000L) {
			long timeNow = System.nanoTime();

			// Actual elapsed timeMeasure
			long realElapsedTime = timeNow - prevCalcTime; // Unit: ns

			// FPSCalculate the
			// realElapsedTimeThe unit ofnsSosConverted to
			actualFPS = ((double) frameCount / realElapsedTime) * 1000000000L;

			frameCount = 0L;
			calcInterval = 0L;
			prevCalcTime = timeNow;
		}
	}

	/**
	 * ObserverStart the client
	 */
	public static void startObserverClient() {
		log.debug("startObserverClient called");

		propObserver = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/setting/netobserver.cfg");
			propObserver.load(in);
			in.close();
		} catch (IOException e) {}

		if(propObserver.getProperty("observer.enable", false) == false) return;
		if((netObserverClient != null) && netObserverClient.isConnected()) return;

		String host = propObserver.getProperty("observer.host", "");
		int port = propObserver.getProperty("observer.port", NetObserverClient.DEFAULT_PORT);

		if((host.length() > 0) && (port > 0)) {
			netObserverClient = new NetObserverClient(host, port);
			netObserverClient.start();
		}
	}

	/**
	 * ObserverStop the client
	 */
	public static void stopObserverClient() {
		log.debug("stopObserverClient called");

		if(netObserverClient != null) {
			if(netObserverClient.isConnected()) {
				netObserverClient.send("disconnect\n");
			}
			netObserverClient.threadRunning = false;
			netObserverClient.connectedFlag = false;
			netObserverClient = null;
		}
		propObserver = null;
	}
}
