package nullpomino.gui.sdl.widget;

import nullpomino.gui.sdl.NormalFontSDL;
import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.ResourceHolderSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Flat rectangular button with a centered label. Activated by mouse click or by
 * pressing Enter/Space while focused.  When activated, the {@link #action} is
 * run (if non-null) and {@link #update(int, int, boolean)} returns true for the
 * current frame so callers can also chain logic the old way.
 */
public class ButtonSDL extends WidgetSDL {
	/** Colour themes used to group related buttons visually. */
	public static final int THEME_DEFAULT = 0;   // dark slate — generic
	public static final int THEME_BLUE    = 1;   // blue — "interact with existing thing" (same as primary)
	public static final int THEME_GREEN   = 2;   // green — "create / new"
	public static final int THEME_VIOLET  = 3;   // violet — "info / secondary"

	public String label;
	/** When true, {@link #render()} draws a left-pointing arrow glyph before the
	 *  label. Used by the standard top-right "back" button (see {@link #newCloseButton}). */
	public boolean iconLeftArrow = false;
	/** Shortcut for {@link #theme} = {@link #THEME_BLUE}; preserved for legacy callers. */
	public boolean primary = false;
	/** Colour theme — picks the background/tint colours in {@link #render()}. */
	public int theme = THEME_DEFAULT;
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

	/** Standard geometry for the top-right "back" button. Width holds "◀ BACK";
	 *  the right edge stays at 632 (an 8px margin on the 640-wide screen). */
	public static final int CLOSE_W = 92, CLOSE_H = 24;

	/**
	 * The standard top-right corner "back" button shared by menu/config/netplay
	 * screens: a left-pointing arrow + "BACK", wired to {@code action} (may be
	 * null for screens that hit-test the button manually). Centralizes the
	 * geometry, label, and arrow so every screen's close button stays identical.
	 */
	public static ButtonSDL newCloseButton(Runnable action) {
		ButtonSDL b = new ButtonSDL(632 - CLOSE_W, 4, CLOSE_W, CLOSE_H, "BACK", action);
		b.iconLeftArrow = true;
		return b;
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

	@Override
	public void handleKey(NullpoMinoSDL.KeyEvent ev) {
		if(!enabled || !focused || ev.repeat) return;
		if(ev.scancode == SDLConstants.SDL_SCANCODE_RETURN
				|| ev.scancode == SDLConstants.SDL_SCANCODE_KP_ENTER
				|| ev.scancode == SDLConstants.SDL_SCANCODE_SPACE) {
			keyboardFire = true;
		}
	}

	/** Per-theme background colours for the non-hover, non-pressed state.
	 *  Order matches the THEME_* constants above. */
	private static final int[][] THEME_BG = {
		{ 32, 32, 48 },   // THEME_DEFAULT
		{  0, 64, 160 },  // THEME_BLUE
		{  0, 96,  48 },  // THEME_GREEN
		{ 80, 40, 120 },  // THEME_VIOLET
	};

	/** Per-theme hover tint (slightly brighter / more saturated). */
	private static final int[][] THEME_HOVER = {
		{ 64, 64,  96 },   // THEME_DEFAULT
		{ 32, 96, 200 },   // THEME_BLUE
		{ 32,144,  80 },   // THEME_GREEN
		{110, 64, 160 },   // THEME_VIOLET
	};

	@Override
	public void render() {
		if(!visible) return;

		int effectiveTheme = (theme != THEME_DEFAULT) ? theme : (primary ? THEME_BLUE : THEME_DEFAULT);
		if(effectiveTheme < 0 || effectiveTheme >= THEME_BG.length) effectiveTheme = THEME_DEFAULT;

		boolean highlight = hovering || focused;
		int bgR, bgG, bgB, bgA;
		if(!enabled) { bgR = 32; bgG = 32; bgB = 32; bgA = 200; }
		else if(pressing) { bgR = 0; bgG = 0; bgB = 128; bgA = 255; }
		else if(highlight) {
			int[] t = THEME_HOVER[effectiveTheme];
			bgR = t[0]; bgG = t[1]; bgB = t[2]; bgA = 230;
		}
		else {
			int[] t = THEME_BG[effectiveTheme];
			bgR = t[0]; bgG = t[1]; bgB = t[2];
			bgA = (effectiveTheme == THEME_DEFAULT) ? 200 : 220;
		}

		fillRect(x, y, w, h, bgR, bgG, bgB, bgA);
		int borderShade = enabled ? (highlight ? 255 : 180) : 96;
		drawRect(x, y, w, h, borderShade, borderShade, borderShade, 255);
		// Extra highlight ring for the keyboard-focused button so it's visually obvious.
		if(focused && enabled) drawRect(x + 1, y + 1, w - 2, h - 2, 255, 255, 0, 255);

		if(label != null && label.length() > 0) {
			boolean themed = effectiveTheme != THEME_DEFAULT;
			int color = enabled
					? (highlight || themed ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_WHITE)
					: NormalFontSDL.COLOR_DARKBLUE;
			int ty = y + (h - 16) / 2;
			// Optional left-arrow icon reserves a 16px glyph + 4px gap before the label.
			int iconW = iconLeftArrow ? 20 : 0;
			String safe = NormalFontSDL.safeString(label);
			int maxChars = Math.max(1, (w - 4 - iconW) / 16);
			if(safe.length() > maxChars) safe = safe.substring(0, maxChars);
			int textW = safe.length() * 16;
			int gx = x + (w - iconW - textW) / 2;
			if(iconLeftArrow) {
				// 'b' is the atlas's right-pointing arrow; mirrored → left arrow.
				NormalFontSDL.printFontFlippedH(gx, ty, 'b', color);
				gx += iconW;
			}
			NormalFontSDL.printFont(gx, ty, safe, color);
		}
	}
}
