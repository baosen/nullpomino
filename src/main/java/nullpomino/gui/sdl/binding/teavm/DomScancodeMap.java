package nullpomino.gui.sdl.binding.teavm;

import java.util.HashMap;
import java.util.Map;

import org.teavm.jso.dom.events.KeyboardEvent;

import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Maps DOM {@code KeyboardEvent.code} values to SDL3 scancodes. The game's key
 * config stores SDL3 scancodes, so the web backend must speak the same
 * language as the desktop backend. DOM {@code code} is a physical-key model
 * (layout-independent), which lines up directly with SDL scancodes — so this
 * is a plain lookup table rather than the location-disambiguation the AWT map
 * needed.
 */
final class DomScancodeMap {
	private DomScancodeMap() {}

	private static final Map<String, Integer> CODES = build();

	private static Map<String, Integer> build() {
		Map<String, Integer> m = new HashMap<>();
		// Letters
		for (char c = 'A'; c <= 'Z'; c++) {
			m.put("Key" + c, SDLConstants.SDL_SCANCODE_A + (c - 'A'));
		}
		// Number row (SDL orders 1..9 then 0)
		for (int d = 1; d <= 9; d++) {
			m.put("Digit" + d, SDLConstants.SDL_SCANCODE_1 + (d - 1));
		}
		m.put("Digit0", SDLConstants.SDL_SCANCODE_0);
		// Function keys
		for (int f = 1; f <= 12; f++) {
			m.put("F" + f, SDLConstants.SDL_SCANCODE_F1 + (f - 1));
		}
		// Numpad digits (SDL orders KP_1..KP_9 then KP_0)
		for (int d = 1; d <= 9; d++) {
			m.put("Numpad" + d, SDLConstants.SDL_SCANCODE_KP_1 + (d - 1));
		}
		m.put("Numpad0", SDLConstants.SDL_SCANCODE_KP_0);

		m.put("Enter", SDLConstants.SDL_SCANCODE_RETURN);
		m.put("NumpadEnter", SDLConstants.SDL_SCANCODE_KP_ENTER);
		m.put("Escape", SDLConstants.SDL_SCANCODE_ESCAPE);
		m.put("Backspace", SDLConstants.SDL_SCANCODE_BACKSPACE);
		m.put("Tab", SDLConstants.SDL_SCANCODE_TAB);
		m.put("Space", SDLConstants.SDL_SCANCODE_SPACE);
		m.put("Minus", SDLConstants.SDL_SCANCODE_MINUS);
		m.put("Equal", SDLConstants.SDL_SCANCODE_EQUALS);
		m.put("BracketLeft", SDLConstants.SDL_SCANCODE_LEFTBRACKET);
		m.put("BracketRight", SDLConstants.SDL_SCANCODE_RIGHTBRACKET);
		m.put("Backslash", SDLConstants.SDL_SCANCODE_BACKSLASH);
		m.put("Semicolon", SDLConstants.SDL_SCANCODE_SEMICOLON);
		m.put("Quote", SDLConstants.SDL_SCANCODE_APOSTROPHE);
		m.put("Backquote", SDLConstants.SDL_SCANCODE_GRAVE);
		m.put("Comma", SDLConstants.SDL_SCANCODE_COMMA);
		m.put("Period", SDLConstants.SDL_SCANCODE_PERIOD);
		m.put("Slash", SDLConstants.SDL_SCANCODE_SLASH);
		m.put("CapsLock", SDLConstants.SDL_SCANCODE_CAPSLOCK);
		m.put("Insert", SDLConstants.SDL_SCANCODE_INSERT);
		m.put("Home", SDLConstants.SDL_SCANCODE_HOME);
		m.put("PageUp", SDLConstants.SDL_SCANCODE_PAGEUP);
		m.put("Delete", SDLConstants.SDL_SCANCODE_DELETE);
		m.put("End", SDLConstants.SDL_SCANCODE_END);
		m.put("PageDown", SDLConstants.SDL_SCANCODE_PAGEDOWN);
		m.put("ArrowRight", SDLConstants.SDL_SCANCODE_RIGHT);
		m.put("ArrowLeft", SDLConstants.SDL_SCANCODE_LEFT);
		m.put("ArrowDown", SDLConstants.SDL_SCANCODE_DOWN);
		m.put("ArrowUp", SDLConstants.SDL_SCANCODE_UP);
		m.put("NumpadDivide", SDLConstants.SDL_SCANCODE_KP_DIVIDE);
		m.put("NumpadMultiply", SDLConstants.SDL_SCANCODE_KP_MULTIPLY);
		m.put("NumpadSubtract", SDLConstants.SDL_SCANCODE_KP_MINUS);
		m.put("NumpadAdd", SDLConstants.SDL_SCANCODE_KP_PLUS);
		m.put("NumpadDecimal", SDLConstants.SDL_SCANCODE_KP_PERIOD);
		m.put("ControlLeft", SDLConstants.SDL_SCANCODE_LCTRL);
		m.put("ControlRight", SDLConstants.SDL_SCANCODE_RCTRL);
		m.put("ShiftLeft", SDLConstants.SDL_SCANCODE_LSHIFT);
		m.put("ShiftRight", SDLConstants.SDL_SCANCODE_RSHIFT);
		m.put("AltLeft", SDLConstants.SDL_SCANCODE_LALT);
		m.put("AltRight", SDLConstants.SDL_SCANCODE_RALT);
		return m;
	}

	/** @return the SDL3 scancode for a DOM key code, or -1 if unmapped */
	static int map(String code) {
		Integer scancode = code == null ? null : CODES.get(code);
		return scancode == null ? -1 : scancode;
	}

	/** Build an SDL keymod bitmask from a keyboard event's modifier flags. */
	static int keymod(KeyboardEvent e) {
		int mod = SDLConstants.SDL_KMOD_NONE;
		if (e.isShiftKey()) mod |= SDLConstants.SDL_KMOD_LSHIFT;
		if (e.isCtrlKey()) mod |= SDLConstants.SDL_KMOD_LCTRL;
		if (e.isAltKey()) mod |= SDLConstants.SDL_KMOD_LALT;
		if (e.isMetaKey()) mod |= SDLConstants.SDL_KMOD_LGUI;
		return mod;
	}
}
