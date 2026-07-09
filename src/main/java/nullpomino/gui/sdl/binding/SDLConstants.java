package nullpomino.gui.sdl.binding;

/**
 * SDL3 constants mapped from C headers.
 */
public final class SDLConstants {
	private SDLConstants() {}

	// SDL_MessageBoxFlags
	public static final int SDL_MESSAGEBOX_ERROR = 0x00000010;

	// SDL_Init flags
	public static final int SDL_INIT_AUDIO = 0x00000010;
	public static final int SDL_INIT_VIDEO = 0x00000020;
	public static final int SDL_INIT_JOYSTICK = 0x00000200;
	/** Implies SDL_INIT_JOYSTICK */
	public static final int SDL_INIT_GAMEPAD  = 0x00002000;

	// SDL_WindowFlags
	public static final long SDL_WINDOW_FULLSCREEN = 0x0000000000000001L;
	public static final long SDL_WINDOW_RESIZABLE  = 0x0000000000000020L;

	// SDL_RendererLogicalPresentation
	public static final int SDL_LOGICAL_PRESENTATION_LETTERBOX = 2;

	// SDL_BlendMode
	public static final int SDL_BLENDMODE_NONE  = 0x00000000;
	public static final int SDL_BLENDMODE_BLEND = 0x00000001;

	// SDL_FlipMode
	public static final int SDL_FLIP_NONE       = 0;
	public static final int SDL_FLIP_HORIZONTAL = 1;
	public static final int SDL_FLIP_VERTICAL   = 2;

	// SDL_EventType
	public static final int SDL_EVENT_QUIT                  = 0x100;
	public static final int SDL_EVENT_WINDOW_SHOWN          = 0x202;
	public static final int SDL_EVENT_WINDOW_RESIZED        = 0x206;
	public static final int SDL_EVENT_WINDOW_CLOSE_REQUESTED = 0x212;
	public static final int SDL_EVENT_WINDOW_ENTER_FULLSCREEN = 0x217;
	public static final int SDL_EVENT_WINDOW_LEAVE_FULLSCREEN = 0x218;
	public static final int SDL_EVENT_KEY_DOWN              = 0x300;
	public static final int SDL_EVENT_KEY_UP                = 0x301;
	public static final int SDL_EVENT_TEXT_EDITING          = 0x302;
	public static final int SDL_EVENT_TEXT_INPUT            = 0x303;
	public static final int SDL_EVENT_MOUSE_MOTION          = 0x400;
	public static final int SDL_EVENT_MOUSE_BUTTON_DOWN     = 0x401;
	public static final int SDL_EVENT_MOUSE_BUTTON_UP       = 0x402;
	public static final int SDL_EVENT_MOUSE_WHEEL           = 0x403;
	public static final int SDL_EVENT_JOYSTICK_ADDED        = 0x605;
	public static final int SDL_EVENT_JOYSTICK_REMOVED      = 0x606;

	// SDL_GamepadButton ordinals
	public static final int SDL_GAMEPAD_BUTTON_SOUTH          = 0;
	public static final int SDL_GAMEPAD_BUTTON_EAST           = 1;
	public static final int SDL_GAMEPAD_BUTTON_WEST           = 2;
	public static final int SDL_GAMEPAD_BUTTON_NORTH          = 3;
	public static final int SDL_GAMEPAD_BUTTON_BACK           = 4;
	public static final int SDL_GAMEPAD_BUTTON_GUIDE          = 5;
	public static final int SDL_GAMEPAD_BUTTON_START          = 6;
	public static final int SDL_GAMEPAD_BUTTON_LEFT_STICK     = 7;
	public static final int SDL_GAMEPAD_BUTTON_RIGHT_STICK    = 8;
	public static final int SDL_GAMEPAD_BUTTON_LEFT_SHOULDER  = 9;
	public static final int SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER = 10;
	public static final int SDL_GAMEPAD_BUTTON_DPAD_UP        = 11;
	public static final int SDL_GAMEPAD_BUTTON_DPAD_DOWN      = 12;
	public static final int SDL_GAMEPAD_BUTTON_DPAD_LEFT      = 13;
	public static final int SDL_GAMEPAD_BUTTON_DPAD_RIGHT     = 14;
	/** Buttons we poll per gamepad (SOUTH..DPAD_RIGHT; paddles/misc excluded). */
	public static final int SDL_GAMEPAD_NUM_BUTTONS = 15;

	// SDL_GamepadAxis ordinals
	public static final int SDL_GAMEPAD_AXIS_LEFTX = 0;
	public static final int SDL_GAMEPAD_AXIS_LEFTY = 1;

	// SDL_Keymod bitmask (Uint16 in SDL_KeyboardEvent.mod)
	public static final int SDL_KMOD_NONE   = 0x0000;
	public static final int SDL_KMOD_LSHIFT = 0x0001;
	public static final int SDL_KMOD_RSHIFT = 0x0002;
	public static final int SDL_KMOD_LCTRL  = 0x0040;
	public static final int SDL_KMOD_RCTRL  = 0x0080;
	public static final int SDL_KMOD_LALT   = 0x0100;
	public static final int SDL_KMOD_RALT   = 0x0200;
	public static final int SDL_KMOD_LGUI   = 0x0400;
	public static final int SDL_KMOD_CTRL   = SDL_KMOD_LCTRL  | SDL_KMOD_RCTRL;
	public static final int SDL_KMOD_SHIFT  = SDL_KMOD_LSHIFT | SDL_KMOD_RSHIFT;
	public static final int SDL_KMOD_ALT    = SDL_KMOD_LALT   | SDL_KMOD_RALT;

	// SDL_Scancode values (from SDL_scancode.h)
	public static final int SDL_SCANCODE_A = 4;
	public static final int SDL_SCANCODE_B = 5;
	public static final int SDL_SCANCODE_C = 6;
	public static final int SDL_SCANCODE_D = 7;
	public static final int SDL_SCANCODE_E = 8;
	public static final int SDL_SCANCODE_F = 9;
	public static final int SDL_SCANCODE_G = 10;
	public static final int SDL_SCANCODE_H = 11;
	public static final int SDL_SCANCODE_I = 12;
	public static final int SDL_SCANCODE_J = 13;
	public static final int SDL_SCANCODE_K = 14;
	public static final int SDL_SCANCODE_L = 15;
	public static final int SDL_SCANCODE_M = 16;
	public static final int SDL_SCANCODE_N = 17;
	public static final int SDL_SCANCODE_O = 18;
	public static final int SDL_SCANCODE_P = 19;
	public static final int SDL_SCANCODE_Q = 20;
	public static final int SDL_SCANCODE_R = 21;
	public static final int SDL_SCANCODE_S = 22;
	public static final int SDL_SCANCODE_T = 23;
	public static final int SDL_SCANCODE_U = 24;
	public static final int SDL_SCANCODE_V = 25;
	public static final int SDL_SCANCODE_W = 26;
	public static final int SDL_SCANCODE_X = 27;
	public static final int SDL_SCANCODE_Y = 28;
	public static final int SDL_SCANCODE_Z = 29;
	public static final int SDL_SCANCODE_1 = 30;
	public static final int SDL_SCANCODE_2 = 31;
	public static final int SDL_SCANCODE_3 = 32;
	public static final int SDL_SCANCODE_4 = 33;
	public static final int SDL_SCANCODE_5 = 34;
	public static final int SDL_SCANCODE_6 = 35;
	public static final int SDL_SCANCODE_7 = 36;
	public static final int SDL_SCANCODE_8 = 37;
	public static final int SDL_SCANCODE_9 = 38;
	public static final int SDL_SCANCODE_0 = 39;
	public static final int SDL_SCANCODE_RETURN = 40;
	public static final int SDL_SCANCODE_ESCAPE = 41;
	public static final int SDL_SCANCODE_BACKSPACE = 42;
	public static final int SDL_SCANCODE_TAB = 43;
	public static final int SDL_SCANCODE_SPACE = 44;
	public static final int SDL_SCANCODE_MINUS = 45;
	public static final int SDL_SCANCODE_EQUALS = 46;
	public static final int SDL_SCANCODE_LEFTBRACKET = 47;
	public static final int SDL_SCANCODE_RIGHTBRACKET = 48;
	public static final int SDL_SCANCODE_BACKSLASH = 49;
	public static final int SDL_SCANCODE_SEMICOLON = 51;
	public static final int SDL_SCANCODE_APOSTROPHE = 52;
	public static final int SDL_SCANCODE_GRAVE = 53;
	public static final int SDL_SCANCODE_COMMA = 54;
	public static final int SDL_SCANCODE_PERIOD = 55;
	public static final int SDL_SCANCODE_SLASH = 56;
	public static final int SDL_SCANCODE_CAPSLOCK = 57;
	public static final int SDL_SCANCODE_F1 = 58;
	public static final int SDL_SCANCODE_F2 = 59;
	public static final int SDL_SCANCODE_F3 = 60;
	public static final int SDL_SCANCODE_F4 = 61;
	public static final int SDL_SCANCODE_F5 = 62;
	public static final int SDL_SCANCODE_F6 = 63;
	public static final int SDL_SCANCODE_F7 = 64;
	public static final int SDL_SCANCODE_F8 = 65;
	public static final int SDL_SCANCODE_F9 = 66;
	public static final int SDL_SCANCODE_F10 = 67;
	public static final int SDL_SCANCODE_F11 = 68;
	public static final int SDL_SCANCODE_F12 = 69;
	public static final int SDL_SCANCODE_INSERT = 73;
	public static final int SDL_SCANCODE_HOME = 74;
	public static final int SDL_SCANCODE_PAGEUP = 75;
	public static final int SDL_SCANCODE_DELETE = 76;
	public static final int SDL_SCANCODE_END = 77;
	public static final int SDL_SCANCODE_PAGEDOWN = 78;
	public static final int SDL_SCANCODE_RIGHT = 79;
	public static final int SDL_SCANCODE_LEFT = 80;
	public static final int SDL_SCANCODE_DOWN = 81;
	public static final int SDL_SCANCODE_UP = 82;
	public static final int SDL_SCANCODE_KP_DIVIDE = 84;
	public static final int SDL_SCANCODE_KP_MULTIPLY = 85;
	public static final int SDL_SCANCODE_KP_MINUS = 86;
	public static final int SDL_SCANCODE_KP_PLUS = 87;
	public static final int SDL_SCANCODE_KP_ENTER = 88;
	public static final int SDL_SCANCODE_KP_1 = 89;
	public static final int SDL_SCANCODE_KP_2 = 90;
	public static final int SDL_SCANCODE_KP_3 = 91;
	public static final int SDL_SCANCODE_KP_4 = 92;
	public static final int SDL_SCANCODE_KP_5 = 93;
	public static final int SDL_SCANCODE_KP_6 = 94;
	public static final int SDL_SCANCODE_KP_7 = 95;
	public static final int SDL_SCANCODE_KP_8 = 96;
	public static final int SDL_SCANCODE_KP_9 = 97;
	public static final int SDL_SCANCODE_KP_0 = 98;
	public static final int SDL_SCANCODE_KP_PERIOD = 99;
	public static final int SDL_SCANCODE_LCTRL = 224;
	public static final int SDL_SCANCODE_LSHIFT = 225;
	public static final int SDL_SCANCODE_LALT = 226;
	public static final int SDL_SCANCODE_RCTRL = 228;
	public static final int SDL_SCANCODE_RSHIFT = 229;
	public static final int SDL_SCANCODE_RALT = 230;
	public static final int SDL_SCANCODE_COUNT = 512;

	// SDL_MouseButtonFlags
	public static final int SDL_BUTTON_LEFT   = 1;
	public static final int SDL_BUTTON_MIDDLE = 2;
	public static final int SDL_BUTTON_RIGHT  = 3;
	public static final int SDL_BUTTON_X1     = 4;
	public static final int SDL_BUTTON_X2     = 5;
	public static final int SDL_BUTTON_LMASK  = (1 << (SDL_BUTTON_LEFT - 1));
	public static final int SDL_BUTTON_MMASK  = (1 << (SDL_BUTTON_MIDDLE - 1));
	public static final int SDL_BUTTON_RMASK  = (1 << (SDL_BUTTON_RIGHT - 1));
	public static final int SDL_BUTTON_X1MASK = (1 << (SDL_BUTTON_X1 - 1));
	public static final int SDL_BUTTON_X2MASK = (1 << (SDL_BUTTON_X2 - 1));

	// SDL_ScaleMode
	public static final int SDL_SCALEMODE_NEAREST = 0;

	/** Scancode name table for display in config screens. */
	public static final String[] SCANCODE_NAMES = buildScancodeNames();

	private static String[] buildScancodeNames() {
		String[] names = new String[SDL_SCANCODE_COUNT];
		for(int i = 0; i < names.length; i++) names[i] = "(" + i + ")";
		names[SDL_SCANCODE_A] = "A"; names[SDL_SCANCODE_B] = "B"; names[SDL_SCANCODE_C] = "C";
		names[SDL_SCANCODE_D] = "D"; names[SDL_SCANCODE_E] = "E"; names[SDL_SCANCODE_F] = "F";
		names[SDL_SCANCODE_G] = "G"; names[SDL_SCANCODE_H] = "H"; names[SDL_SCANCODE_I] = "I";
		names[SDL_SCANCODE_J] = "J"; names[SDL_SCANCODE_K] = "K"; names[SDL_SCANCODE_L] = "L";
		names[SDL_SCANCODE_M] = "M"; names[SDL_SCANCODE_N] = "N"; names[SDL_SCANCODE_O] = "O";
		names[SDL_SCANCODE_P] = "P"; names[SDL_SCANCODE_Q] = "Q"; names[SDL_SCANCODE_R] = "R";
		names[SDL_SCANCODE_S] = "S"; names[SDL_SCANCODE_T] = "T"; names[SDL_SCANCODE_U] = "U";
		names[SDL_SCANCODE_V] = "V"; names[SDL_SCANCODE_W] = "W"; names[SDL_SCANCODE_X] = "X";
		names[SDL_SCANCODE_Y] = "Y"; names[SDL_SCANCODE_Z] = "Z";
		names[SDL_SCANCODE_1] = "1"; names[SDL_SCANCODE_2] = "2"; names[SDL_SCANCODE_3] = "3";
		names[SDL_SCANCODE_4] = "4"; names[SDL_SCANCODE_5] = "5"; names[SDL_SCANCODE_6] = "6";
		names[SDL_SCANCODE_7] = "7"; names[SDL_SCANCODE_8] = "8"; names[SDL_SCANCODE_9] = "9";
		names[SDL_SCANCODE_0] = "0";
		names[SDL_SCANCODE_RETURN] = "RETURN"; names[SDL_SCANCODE_ESCAPE] = "ESCAPE";
		names[SDL_SCANCODE_BACKSPACE] = "BACKSPACE"; names[SDL_SCANCODE_TAB] = "TAB";
		names[SDL_SCANCODE_SPACE] = "SPACE"; names[SDL_SCANCODE_MINUS] = "MINUS";
		names[SDL_SCANCODE_EQUALS] = "EQUALS";
		names[SDL_SCANCODE_LEFTBRACKET] = "LEFTBRACKET"; names[SDL_SCANCODE_RIGHTBRACKET] = "RIGHTBRACKET";
		names[SDL_SCANCODE_BACKSLASH] = "BACKSLASH"; names[SDL_SCANCODE_SEMICOLON] = "SEMICOLON";
		names[SDL_SCANCODE_APOSTROPHE] = "APOSTROPHE"; names[SDL_SCANCODE_GRAVE] = "GRAVE";
		names[SDL_SCANCODE_COMMA] = "COMMA"; names[SDL_SCANCODE_PERIOD] = "PERIOD";
		names[SDL_SCANCODE_SLASH] = "SLASH"; names[SDL_SCANCODE_CAPSLOCK] = "CAPSLOCK";
		names[SDL_SCANCODE_F1] = "F1"; names[SDL_SCANCODE_F2] = "F2"; names[SDL_SCANCODE_F3] = "F3";
		names[SDL_SCANCODE_F4] = "F4"; names[SDL_SCANCODE_F5] = "F5"; names[SDL_SCANCODE_F6] = "F6";
		names[SDL_SCANCODE_F7] = "F7"; names[SDL_SCANCODE_F8] = "F8"; names[SDL_SCANCODE_F9] = "F9";
		names[SDL_SCANCODE_F10] = "F10"; names[SDL_SCANCODE_F11] = "F11"; names[SDL_SCANCODE_F12] = "F12";
		names[SDL_SCANCODE_INSERT] = "INSERT"; names[SDL_SCANCODE_HOME] = "HOME";
		names[SDL_SCANCODE_PAGEUP] = "PAGEUP"; names[SDL_SCANCODE_DELETE] = "DELETE";
		names[SDL_SCANCODE_END] = "END"; names[SDL_SCANCODE_PAGEDOWN] = "PAGEDOWN";
		names[SDL_SCANCODE_RIGHT] = "RIGHT"; names[SDL_SCANCODE_LEFT] = "LEFT";
		names[SDL_SCANCODE_DOWN] = "DOWN"; names[SDL_SCANCODE_UP] = "UP";
		names[SDL_SCANCODE_KP_DIVIDE] = "KP_DIVIDE"; names[SDL_SCANCODE_KP_MULTIPLY] = "KP_MULTIPLY";
		names[SDL_SCANCODE_KP_MINUS] = "KP_MINUS"; names[SDL_SCANCODE_KP_PLUS] = "KP_PLUS";
		names[SDL_SCANCODE_KP_ENTER] = "KP_ENTER";
		names[SDL_SCANCODE_KP_1] = "KP_1"; names[SDL_SCANCODE_KP_2] = "KP_2";
		names[SDL_SCANCODE_KP_3] = "KP_3"; names[SDL_SCANCODE_KP_4] = "KP_4";
		names[SDL_SCANCODE_KP_5] = "KP_5"; names[SDL_SCANCODE_KP_6] = "KP_6";
		names[SDL_SCANCODE_KP_7] = "KP_7"; names[SDL_SCANCODE_KP_8] = "KP_8";
		names[SDL_SCANCODE_KP_9] = "KP_9"; names[SDL_SCANCODE_KP_0] = "KP_0";
		names[SDL_SCANCODE_KP_PERIOD] = "KP_PERIOD";
		names[SDL_SCANCODE_LCTRL] = "LCTRL"; names[SDL_SCANCODE_LSHIFT] = "LSHIFT";
		names[SDL_SCANCODE_LALT] = "LALT";
		names[SDL_SCANCODE_RCTRL] = "RCTRL"; names[SDL_SCANCODE_RSHIFT] = "RSHIFT";
		names[SDL_SCANCODE_RALT] = "RALT";
		return names;
	}
}
