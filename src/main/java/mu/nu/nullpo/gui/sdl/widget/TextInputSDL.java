package mu.nu.nullpo.gui.sdl.widget;

import mu.nu.nullpo.gui.sdl.NormalFontSDL;
import mu.nu.nullpo.gui.sdl.NullpoMinoSDL;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;
import mu.nu.nullpo.gui.sdl.binding.SDLStructs;

/**
 * Single-line text input field. Owns a char buffer + caret index. Supports:
 * - Typing (SDL_EVENT_TEXT_INPUT, already marshalled through NullpoMinoSDL)
 * - IME preedit composition (rendered with an underline under uncommitted text)
 * - Caret movement (Left/Right, Home/End)
 * - Backspace / Delete
 * - Clipboard: Ctrl+C (copy), Ctrl+V (paste), Ctrl+X (cut), Ctrl+A (select all) [copy/paste only, no selection]
 *
 * Uses the 16px bitmap font for Latin-1 input. Non-Latin characters are accepted
 * (stored in the internal String) but rendered as the bitmap font's fallback glyph.
 */
public class TextInputSDL extends WidgetSDL {
	/** Current text content. */
	private String text = "";
	/** Caret position (number of chars before the cursor). */
	private int caret = 0;
	/** View scroll offset in characters (when text is wider than the field). */
	private int scrollChar = 0;
	/** Max characters accepted; -1 for unlimited. */
	public int maxChars = -1;
	/** When true, renders bullets (for passwords). */
	public boolean password = false;
	/** Optional hint rendered dim when text is empty and widget not focused. */
	public String placeholder = "";

	public TextInputSDL(int x, int y, int w, int h) {
		super(x, y, w, h);
	}

	public String getText() { return text; }
	public void setText(String s) {
		text = (s == null) ? "" : s;
		if(maxChars > 0 && text.length() > maxChars) text = text.substring(0, maxChars);
		caret = text.length();
		scrollChar = Math.max(0, caret - maxVisibleChars());
	}

	@Override
	public void setFocused(boolean on) {
		if(focused == on) return;
		focused = on;
		if(on) {
			SDLStructs.SDL_Rect area = new SDLStructs.SDL_Rect(x, y, w, h);
			NullpoMinoSDL.startTextInput(area);
		} else {
			NullpoMinoSDL.stopTextInput();
		}
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		if(!visible || !enabled) return false;
		if(leftJustPressed && containsPoint(mx, my)) {
			// Place caret under the click
			int charX = Math.max(0, (mx - (x + 4)) / 16);
			caret = Math.min(text.length(), scrollChar + charX);
			return true;
		}
		return false;
	}

	@Override
	public void handleKey(NullpoMinoSDL.KeyEvent ev) {
		if(!enabled) return;
		boolean ctrl = (ev.keymod & SDLConstants.SDL_KMOD_CTRL) != 0;

		switch(ev.scancode) {
			case SDLConstants.SDL_SCANCODE_BACKSPACE:
				if(caret > 0) {
					text = text.substring(0, caret - 1) + text.substring(caret);
					caret--;
				}
				break;
			case SDLConstants.SDL_SCANCODE_DELETE:
				if(caret < text.length()) {
					text = text.substring(0, caret) + text.substring(caret + 1);
				}
				break;
			case SDLConstants.SDL_SCANCODE_LEFT:
				if(caret > 0) caret--;
				break;
			case SDLConstants.SDL_SCANCODE_RIGHT:
				if(caret < text.length()) caret++;
				break;
			case SDLConstants.SDL_SCANCODE_HOME:
				caret = 0;
				break;
			case SDLConstants.SDL_SCANCODE_END:
				caret = text.length();
				break;
			case SDLConstants.SDL_SCANCODE_V:
				if(ctrl) insert(NullpoMinoSDL.getClipboardText());
				break;
			case SDLConstants.SDL_SCANCODE_C:
				if(ctrl) NullpoMinoSDL.setClipboardText(text);
				break;
			case SDLConstants.SDL_SCANCODE_X:
				if(ctrl) {
					NullpoMinoSDL.setClipboardText(text);
					text = "";
					caret = 0;
				}
				break;
			default:
				break;
		}
		clampScroll();
	}

	@Override
	public void handleTextInput(String s) {
		if(!enabled || s == null || s.length() == 0) return;
		insert(s);
	}

	private void insert(String s) {
		if(s == null || s.length() == 0) return;
		// Strip any stray control characters that slipped in via text-input events.
		StringBuilder clean = new StringBuilder(s.length());
		for(int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if(c >= 0x20 || c == '\t') clean.append(c);
		}
		String addition = clean.toString();
		if(addition.length() == 0) return;
		if(maxChars > 0) {
			int room = maxChars - text.length();
			if(room <= 0) return;
			if(addition.length() > room) addition = addition.substring(0, room);
		}
		text = text.substring(0, caret) + addition + text.substring(caret);
		caret += addition.length();
		clampScroll();
	}

	private int maxVisibleChars() {
		return Math.max(1, (w - 8) / 16);
	}

	private void clampScroll() {
		int visible = maxVisibleChars();
		if(caret < scrollChar) scrollChar = caret;
		else if(caret > scrollChar + visible) scrollChar = caret - visible;
		if(scrollChar < 0) scrollChar = 0;
	}

	@Override
	public void render() {
		if(!visible) return;

		panelBackground(x, y, w, h, focused);

		int innerX = x + 4;
		int innerY = y + (h - 16) / 2;
		int visible = maxVisibleChars();

		if(text.length() == 0 && !focused && placeholder != null && placeholder.length() > 0) {
			String hint = NormalFontSDL.safeString(placeholder);
			if(hint.length() > visible) hint = hint.substring(0, visible);
			NormalFontSDL.printFont(innerX, innerY, hint, NormalFontSDL.COLOR_DARKBLUE);
		} else {
			String display = text;
			if(password && display.length() > 0) {
				StringBuilder sb = new StringBuilder(display.length());
				for(int i = 0; i < display.length(); i++) sb.append('*');
				display = sb.toString();
			} else {
				display = NormalFontSDL.safeString(display);
			}
			int end = Math.min(display.length(), scrollChar + visible);
			if(scrollChar < end) {
				NormalFontSDL.printFont(innerX, innerY, display.substring(scrollChar, end), NormalFontSDL.COLOR_WHITE);
			}
		}

		// Caret: 1px vertical bar drawn when focused and blink state is on.
		if(focused) {
			long t = System.currentTimeMillis() / 500L;
			if((t & 1L) == 0L) {
				int caretX = innerX + (caret - scrollChar) * 16;
				fillRect(caretX, innerY, 2, 16, 255, 255, 255, 255);
			}
			// IME preedit underline (if any)
			String ime = NullpoMinoSDL.imeComposition;
			if(ime != null && ime.length() > 0) {
				int caretX = innerX + (caret - scrollChar) * 16;
				int imeW = Math.min(ime.length(), visible - (caret - scrollChar)) * 16;
				if(imeW > 0) {
					fillRect(caretX, innerY + 14, imeW, 2, 255, 255, 0, 255);
				}
			}
		}
	}
}
