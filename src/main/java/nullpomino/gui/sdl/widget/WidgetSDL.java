package nullpomino.gui.sdl.widget;

import com.sun.jna.Pointer;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDL3;
import nullpomino.gui.sdl.binding.SDLConstants;
import nullpomino.gui.sdl.binding.SDLStructs;

/**
 * Base class for all SDL3 lobby widgets. Provides position, enable/focus flags,
 * and drawing helpers shared by buttons, checkboxes, tables, text inputs, etc.
 *
 * Widgets follow an immediate-mode API: the owning state calls {@code update(...)}
 * each frame with pointer state and {@code render()} to draw. Key presses and
 * typed text are delivered to the focused widget via {@code handleKey} /
 * {@code handleTextInput}.
 */
public abstract class WidgetSDL {
	public int x, y, w, h;
	public boolean visible = true;
	public boolean enabled = true;
	public boolean focused = false;

	protected WidgetSDL() {}

	protected WidgetSDL(int x, int y, int w, int h) {
		this.x = x; this.y = y; this.w = w; this.h = h;
	}

	public boolean containsPoint(int px, int py) {
		return px >= x && px < x + w && py >= y && py < y + h;
	}

	/**
	 * Handle pointer input. Default returns false (not activated).
	 * @param mx logical mouse X
	 * @param my logical mouse Y
	 * @param leftJustPressed true on the frame the left button was first pressed
	 * @return true if the widget was activated (e.g. button click)
	 */
	public boolean update(int mx, int my, boolean leftJustPressed) { return false; }

	/** Draw the widget. */
	public abstract void render();

	/** Handle a key-down event while focused. Default: ignore. */
	public void handleKey(NullpoMinoSDL.KeyEvent ev) {}

	/** Handle committed UTF-8 text while focused. Default: ignore. */
	public void handleTextInput(String text) {}

	/** Called by the screen when focus is granted or revoked. Override to start/stop IME. */
	public void setFocused(boolean on) { this.focused = on; }

	// --- Drawing helpers shared by concrete widgets ---

	protected static void fillRect(int x, int y, int w, int h, int r, int g, int b, int a) {
		Pointer rnd = NullpoMinoSDL.renderer;
		SDL3.INSTANCE.SDL_SetRenderDrawBlendMode(rnd, SDLConstants.SDL_BLENDMODE_BLEND);
		SDL3.setDrawColor(rnd, r, g, b, a);
		SDL3.INSTANCE.SDL_RenderFillRect(rnd, new SDLStructs.SDL_FRect(x, y, w, h));
	}

	protected static void drawRect(int x, int y, int w, int h, int r, int g, int b, int a) {
		Pointer rnd = NullpoMinoSDL.renderer;
		SDL3.INSTANCE.SDL_SetRenderDrawBlendMode(rnd, SDLConstants.SDL_BLENDMODE_BLEND);
		SDL3.setDrawColor(rnd, r, g, b, a);
		SDL3.INSTANCE.SDL_RenderRect(rnd, new SDLStructs.SDL_FRect(x, y, w, h));
	}

	/** Panel chrome: dark fill + light border. Used by most widgets for background. */
	protected static void panelBackground(int x, int y, int w, int h, boolean active) {
		fillRect(x, y, w, h, 0, 0, 0, 192);
		if(active) drawRect(x, y, w, h, 255, 255, 255, 255);
		else drawRect(x, y, w, h, 128, 128, 128, 255);
	}
}
