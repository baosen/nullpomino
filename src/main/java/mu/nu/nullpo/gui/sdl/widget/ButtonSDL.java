package mu.nu.nullpo.gui.sdl.widget;

import mu.nu.nullpo.gui.sdl.NormalFontSDL;

/**
 * Flat rectangular button with a centered label. Polling API: the owning screen
 * calls {@link #update(int, int, boolean)} each frame and acts on the returned flag.
 */
public class ButtonSDL extends WidgetSDL {
	public String label;
	/** If true, draw as a "primary" (filled blue) action — e.g. OK/Connect. */
	public boolean primary = false;
	private boolean hovering;
	private boolean pressing;

	public ButtonSDL(int x, int y, int w, int h, String label) {
		super(x, y, w, h);
		this.label = label;
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		if(!visible || !enabled) { hovering = false; pressing = false; return false; }
		hovering = containsPoint(mx, my);
		if(hovering && leftJustPressed) {
			pressing = true;
			return true;
		}
		pressing = false;
		return false;
	}

	/** Trigger the button (used for keyboard-activated shortcuts). */
	public boolean trigger() {
		if(!visible || !enabled) return false;
		pressing = true;
		return true;
	}

	@Override
	public void render() {
		if(!visible) return;

		int bgR, bgG, bgB, bgA;
		if(!enabled) { bgR = 32; bgG = 32; bgB = 32; bgA = 200; }
		else if(pressing) { bgR = 0; bgG = 0; bgB = 128; bgA = 255; }
		else if(primary) { bgR = 0; bgG = 64; bgB = 160; bgA = 220; }
		else if(hovering) { bgR = 64; bgG = 64; bgB = 96; bgA = 220; }
		else { bgR = 32; bgG = 32; bgB = 48; bgA = 200; }

		fillRect(x, y, w, h, bgR, bgG, bgB, bgA);
		drawRect(x, y, w, h, enabled ? (hovering ? 255 : 180) : 96,
		                     enabled ? (hovering ? 255 : 180) : 96,
		                     enabled ? (hovering ? 255 : 180) : 96, 255);

		if(label != null && label.length() > 0) {
			int textW = label.length() * 16;
			int tx = x + (w - textW) / 2;
			int ty = y + (h - 16) / 2;
			int color = enabled
					? (hovering || primary ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_WHITE)
					: NormalFontSDL.COLOR_DARKBLUE;
			NormalFontSDL.printFont(tx, ty, label, color);
		}
	}
}
