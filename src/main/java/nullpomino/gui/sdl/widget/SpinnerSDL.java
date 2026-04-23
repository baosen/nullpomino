package nullpomino.gui.sdl.widget;

import nullpomino.gui.sdl.NormalFontSDL;
import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Numeric spinner with {@code [ - | value | + ]} layout. Clicking arrows or pressing
 * Left/Right adjusts by {@code step}; clicking the value area allows direct typing
 * (value commits on focus loss or Enter).
 *
 * Stored value is clamped to [min, max]. Use {@code getValue()} to read.
 */
public class SpinnerSDL extends WidgetSDL {
	public int min, max, step;
	private int value;
	private final TextInputSDL editor;
	private boolean hoverLeft, hoverRight, hoverEditor;
	/** Frames the current arrow button has been held (for auto-repeat). */
	private int holdLeft, holdRight;

	public SpinnerSDL(int x, int y, int w, int h, int min, int max, int step, int initial) {
		super(x, y, w, h);
		this.min = min;
		this.max = max;
		this.step = step;
		this.value = clamp(initial);
		this.editor = new TextInputSDL(x + h, y, w - 2 * h, h);
		this.editor.maxChars = Math.max(4, String.valueOf(max).length() + 1);
		this.editor.setText(String.valueOf(value));
	}

	public int getValue() {
		commitEditor();
		return value;
	}

	public void setValue(int v) {
		value = clamp(v);
		editor.setText(String.valueOf(value));
	}

	private int clamp(int v) {
		if(v < min) return min;
		if(v > max) return max;
		return v;
	}

	private void commitEditor() {
		try {
			int v = Integer.parseInt(editor.getText().trim());
			value = clamp(v);
		} catch(NumberFormatException ignore) {}
		editor.setText(String.valueOf(value));
	}

	@Override
	public void setFocused(boolean on) {
		super.setFocused(on);
		editor.setFocused(on);
		if(!on) commitEditor();
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		if(!visible || !enabled) {
			hoverLeft = hoverRight = hoverEditor = false;
			holdLeft = holdRight = 0;
			return false;
		}

		hoverLeft = mx >= x && mx < x + h && my >= y && my < y + h;
		hoverRight = mx >= x + w - h && mx < x + w && my >= y && my < y + h;
		hoverEditor = mx >= x + h && mx < x + w - h && my >= y && my < y + h;

		boolean activated = false;
		if(leftJustPressed) {
			if(hoverLeft) { setValue(value - step); activated = true; }
			else if(hoverRight) { setValue(value + step); activated = true; }
			else if(hoverEditor) { editor.update(mx, my, true); activated = true; }
		}
		// Auto-repeat while held (one tick per ~3 frames after initial 20-frame delay)
		int mouseButtons = nullpomino.gui.sdl.MouseInputSDL.mouseInput.getLeftHoldFrames();
		if(mouseButtons > 0 && hoverLeft) {
			holdLeft = mouseButtons;
			if(holdLeft > 20 && (holdLeft % 3 == 0)) setValue(value - step);
		} else holdLeft = 0;
		if(mouseButtons > 0 && hoverRight) {
			holdRight = mouseButtons;
			if(holdRight > 20 && (holdRight % 3 == 0)) setValue(value + step);
		} else holdRight = 0;

		return activated;
	}

	@Override
	public void handleKey(NullpoMinoSDL.KeyEvent ev) {
		if(!enabled) return;
		if(ev.scancode == SDLConstants.SDL_SCANCODE_LEFT) { setValue(value - step); return; }
		if(ev.scancode == SDLConstants.SDL_SCANCODE_RIGHT) { setValue(value + step); return; }
		if(ev.scancode == SDLConstants.SDL_SCANCODE_UP)   { setValue(value + step); return; }
		if(ev.scancode == SDLConstants.SDL_SCANCODE_DOWN) { setValue(value - step); return; }
		if(ev.scancode == SDLConstants.SDL_SCANCODE_RETURN || ev.scancode == SDLConstants.SDL_SCANCODE_KP_ENTER) {
			commitEditor();
			return;
		}
		editor.handleKey(ev);
	}

	@Override
	public void handleTextInput(String s) {
		if(!enabled) return;
		// Restrict to digits and leading minus.
		if(s == null) return;
		StringBuilder filtered = new StringBuilder(s.length());
		for(int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if((c >= '0' && c <= '9') || (c == '-' && editor.getText().length() == 0)) {
				filtered.append(c);
			}
		}
		if(filtered.length() > 0) editor.handleTextInput(filtered.toString());
	}

	@Override
	public void render() {
		if(!visible) return;

		// Left arrow button
		drawArrowButton(x, y, h, h, "-", hoverLeft);
		// Middle: editor
		editor.focused = focused;
		editor.render();
		// Right arrow button
		drawArrowButton(x + w - h, y, h, h, "+", hoverRight);
	}

	private void drawArrowButton(int bx, int by, int bw, int bh, String glyph, boolean hover) {
		int r = 32, g = 32, b = 48, a = 220;
		if(!enabled) { r = 32; g = 32; b = 32; a = 180; }
		else if(hover) { r = 64; g = 64; b = 96; a = 240; }
		fillRect(bx, by, bw, bh, r, g, b, a);
		drawRect(bx, by, bw, bh, enabled ? 200 : 96, enabled ? 200 : 96, enabled ? 200 : 96, 255);
		int tx = bx + (bw - 16) / 2;
		int ty = by + (bh - 16) / 2;
		NormalFontSDL.printFont(tx, ty, glyph, enabled
				? (hover ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_WHITE)
				: NormalFontSDL.COLOR_DARKBLUE);
	}
}
