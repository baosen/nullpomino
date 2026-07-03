package nullpomino.gui.sdl.binding.web;

import java.awt.event.KeyEvent;

import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Maps AWT key codes (plus key location for numpad/left-right modifiers) to
 * SDL3 scancodes. The game's key config stores SDL3 scancodes, so the web
 * backend must speak the same language as the desktop backend.
 */
final class AwtScancodeMap {
	private AwtScancodeMap() {}

	/**
	 * @return the SDL3 scancode for an AWT key event, or -1 if unmapped
	 */
	static int map(int keyCode, int keyLocation) {
		// Letters
		if(keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
			return SDLConstants.SDL_SCANCODE_A + (keyCode - KeyEvent.VK_A);
		}
		// Number row (SDL: 1..9 then 0)
		if(keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {
			return SDLConstants.SDL_SCANCODE_1 + (keyCode - KeyEvent.VK_1);
		}
		if(keyCode == KeyEvent.VK_0) return SDLConstants.SDL_SCANCODE_0;
		// Function keys
		if(keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12) {
			return SDLConstants.SDL_SCANCODE_F1 + (keyCode - KeyEvent.VK_F1);
		}
		// Numpad digits (SDL: KP_1..KP_9 then KP_0)
		if(keyCode >= KeyEvent.VK_NUMPAD1 && keyCode <= KeyEvent.VK_NUMPAD9) {
			return SDLConstants.SDL_SCANCODE_KP_1 + (keyCode - KeyEvent.VK_NUMPAD1);
		}
		if(keyCode == KeyEvent.VK_NUMPAD0) return SDLConstants.SDL_SCANCODE_KP_0;

		switch(keyCode) {
		case KeyEvent.VK_ENTER:
			return keyLocation == KeyEvent.KEY_LOCATION_NUMPAD
				? SDLConstants.SDL_SCANCODE_KP_ENTER : SDLConstants.SDL_SCANCODE_RETURN;
		case KeyEvent.VK_ESCAPE: return SDLConstants.SDL_SCANCODE_ESCAPE;
		case KeyEvent.VK_BACK_SPACE: return SDLConstants.SDL_SCANCODE_BACKSPACE;
		case KeyEvent.VK_TAB: return SDLConstants.SDL_SCANCODE_TAB;
		case KeyEvent.VK_SPACE: return SDLConstants.SDL_SCANCODE_SPACE;
		case KeyEvent.VK_MINUS: return SDLConstants.SDL_SCANCODE_MINUS;
		case KeyEvent.VK_EQUALS: return SDLConstants.SDL_SCANCODE_EQUALS;
		case KeyEvent.VK_OPEN_BRACKET: return SDLConstants.SDL_SCANCODE_LEFTBRACKET;
		case KeyEvent.VK_CLOSE_BRACKET: return SDLConstants.SDL_SCANCODE_RIGHTBRACKET;
		case KeyEvent.VK_BACK_SLASH: return SDLConstants.SDL_SCANCODE_BACKSLASH;
		case KeyEvent.VK_SEMICOLON: return SDLConstants.SDL_SCANCODE_SEMICOLON;
		case KeyEvent.VK_QUOTE: return SDLConstants.SDL_SCANCODE_APOSTROPHE;
		case KeyEvent.VK_BACK_QUOTE: return SDLConstants.SDL_SCANCODE_GRAVE;
		case KeyEvent.VK_COMMA: return SDLConstants.SDL_SCANCODE_COMMA;
		case KeyEvent.VK_PERIOD: return SDLConstants.SDL_SCANCODE_PERIOD;
		case KeyEvent.VK_SLASH: return SDLConstants.SDL_SCANCODE_SLASH;
		case KeyEvent.VK_CAPS_LOCK: return SDLConstants.SDL_SCANCODE_CAPSLOCK;
		case KeyEvent.VK_INSERT: return SDLConstants.SDL_SCANCODE_INSERT;
		case KeyEvent.VK_HOME: return SDLConstants.SDL_SCANCODE_HOME;
		case KeyEvent.VK_PAGE_UP: return SDLConstants.SDL_SCANCODE_PAGEUP;
		case KeyEvent.VK_DELETE: return SDLConstants.SDL_SCANCODE_DELETE;
		case KeyEvent.VK_END: return SDLConstants.SDL_SCANCODE_END;
		case KeyEvent.VK_PAGE_DOWN: return SDLConstants.SDL_SCANCODE_PAGEDOWN;
		case KeyEvent.VK_RIGHT: return SDLConstants.SDL_SCANCODE_RIGHT;
		case KeyEvent.VK_LEFT: return SDLConstants.SDL_SCANCODE_LEFT;
		case KeyEvent.VK_DOWN: return SDLConstants.SDL_SCANCODE_DOWN;
		case KeyEvent.VK_UP: return SDLConstants.SDL_SCANCODE_UP;
		case KeyEvent.VK_DIVIDE: return SDLConstants.SDL_SCANCODE_KP_DIVIDE;
		case KeyEvent.VK_MULTIPLY: return SDLConstants.SDL_SCANCODE_KP_MULTIPLY;
		case KeyEvent.VK_SUBTRACT: return SDLConstants.SDL_SCANCODE_KP_MINUS;
		case KeyEvent.VK_ADD: return SDLConstants.SDL_SCANCODE_KP_PLUS;
		case KeyEvent.VK_DECIMAL: return SDLConstants.SDL_SCANCODE_KP_PERIOD;
		case KeyEvent.VK_CONTROL:
			return keyLocation == KeyEvent.KEY_LOCATION_RIGHT
				? SDLConstants.SDL_SCANCODE_RCTRL : SDLConstants.SDL_SCANCODE_LCTRL;
		case KeyEvent.VK_SHIFT:
			return keyLocation == KeyEvent.KEY_LOCATION_RIGHT
				? SDLConstants.SDL_SCANCODE_RSHIFT : SDLConstants.SDL_SCANCODE_LSHIFT;
		case KeyEvent.VK_ALT:
			return keyLocation == KeyEvent.KEY_LOCATION_RIGHT
				? SDLConstants.SDL_SCANCODE_RALT : SDLConstants.SDL_SCANCODE_LALT;
		default:
			return -1;
		}
	}

	/** Build an SDL keymod bitmask from AWT extended modifiers. */
	static int keymod(int modifiersEx) {
		int mod = SDLConstants.SDL_KMOD_NONE;
		if((modifiersEx & KeyEvent.SHIFT_DOWN_MASK) != 0) mod |= SDLConstants.SDL_KMOD_LSHIFT;
		if((modifiersEx & KeyEvent.CTRL_DOWN_MASK) != 0) mod |= SDLConstants.SDL_KMOD_LCTRL;
		if((modifiersEx & KeyEvent.ALT_DOWN_MASK) != 0) mod |= SDLConstants.SDL_KMOD_LALT;
		if((modifiersEx & KeyEvent.META_DOWN_MASK) != 0) mod |= SDLConstants.SDL_KMOD_LGUI;
		return mod;
	}
}
