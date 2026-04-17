package mu.nu.nullpo.gui.sdl.widget;

import mu.nu.nullpo.gui.sdl.NormalFontSDL;
import mu.nu.nullpo.gui.sdl.NullpoMinoSDL;
import mu.nu.nullpo.gui.sdl.ResourceHolderSDL;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;

/**
 * Flat rectangular button with a centered label. Activated by mouse click or by
 * pressing Enter/Space while focused.  When activated, the {@link #action} is
 * run (if non-null) and {@link #update(int, int, boolean)} returns true for the
 * current frame so callers can also chain logic the old way.
 */
public class ButtonSDL extends WidgetSDL {
	public String label;
	/** If true, draw as a "primary" (filled blue) action — e.g. OK/Connect. */
	public boolean primary = false;
	/** Activation callback. Fired on mouse click or Enter/Space keypress while focused. */
	public Runnable action;

	private boolean hovering;
	private boolean pressing;
	/** True for the frame a keyboard activation happened; consumed by the next update(). */
	private boolean keyboardFire;

	public ButtonSDL(int x, int y, int w, int h, String label) {
		super(x, y, w, h);
		this.label = label;
	}

	public ButtonSDL(int x, int y, int w, int h, String label, Runnable action) {
		this(x, y, w, h, label);
		this.action = action;
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		if(!visible || !enabled) { hovering = false; pressing = false; keyboardFire = false; return false; }
		hovering = containsPoint(mx, my);
		boolean fire = false;
		if(keyboardFire) { keyboardFire = false; fire = true; }
		if(hovering && leftJustPressed) fire = true;
		if(fire) {
			pressing = true;
			if(ResourceHolderSDL.soundManager != null) ResourceHolderSDL.soundManager.play("decide");
			if(action != null) action.run();
			return true;
		}
		pressing = false;
		return false;
	}

	/** Trigger the button programmatically. Used for "default" button shortcuts. */
	public boolean trigger() {
		if(!visible || !enabled) return false;
		keyboardFire = true;
		return true;
	}

	@Override
	public void handleKey(NullpoMinoSDL.KeyEvent ev) {
		if(!enabled || !focused || ev.repeat) return;
		if(ev.scancode == SDLConstants.SDL_SCANCODE_RETURN
				|| ev.scancode == SDLConstants.SDL_SCANCODE_KP_ENTER
				|| ev.scancode == SDLConstants.SDL_SCANCODE_SPACE) {
			keyboardFire = true;
		}
	}

	@Override
	public void render() {
		if(!visible) return;

		int bgR, bgG, bgB, bgA;
		boolean highlight = hovering || focused;
		if(!enabled) { bgR = 32; bgG = 32; bgB = 32; bgA = 200; }
		else if(pressing) { bgR = 0; bgG = 0; bgB = 128; bgA = 255; }
		else if(primary) { bgR = 0; bgG = 64; bgB = 160; bgA = 220; }
		else if(highlight) { bgR = 64; bgG = 64; bgB = 96; bgA = 220; }
		else { bgR = 32; bgG = 32; bgB = 48; bgA = 200; }

		fillRect(x, y, w, h, bgR, bgG, bgB, bgA);
		int borderShade = enabled ? (highlight ? 255 : 180) : 96;
		drawRect(x, y, w, h, borderShade, borderShade, borderShade, 255);
		// Extra highlight ring for the keyboard-focused button so it's visually obvious.
		if(focused && enabled) drawRect(x + 1, y + 1, w - 2, h - 2, 255, 255, 0, 255);

		if(label != null && label.length() > 0) {
			String safe = NormalFontSDL.safeString(label);
			int maxChars = Math.max(1, (w - 4) / 16);
			if(safe.length() > maxChars) safe = safe.substring(0, maxChars);
			int textW = safe.length() * 16;
			int tx = x + (w - textW) / 2;
			int ty = y + (h - 16) / 2;
			int color = enabled
					? (highlight || primary ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_WHITE)
					: NormalFontSDL.COLOR_DARKBLUE;
			NormalFontSDL.printFont(tx, ty, safe, color);
		}
	}
}
