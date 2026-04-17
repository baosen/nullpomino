package mu.nu.nullpo.gui.sdl.widget;

import mu.nu.nullpo.gui.sdl.NormalFontSDL;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;

/**
 * Labeled boolean toggle. Rendered as a 12x12 box + label; click or Enter toggles.
 */
public class CheckboxSDL extends WidgetSDL {
	public String label;
	public boolean checked;
	private boolean hovering;

	public CheckboxSDL(int x, int y, int w, int h, String label, boolean initial) {
		super(x, y, w, h);
		this.label = label;
		this.checked = initial;
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		if(!visible || !enabled) { hovering = false; return false; }
		hovering = containsPoint(mx, my);
		if(hovering && leftJustPressed) {
			checked = !checked;
			return true;
		}
		return false;
	}

	@Override
	public void handleKey(mu.nu.nullpo.gui.sdl.NullpoMinoSDL.KeyEvent ev) {
		if(!enabled) return;
		if(ev.scancode == SDLConstants.SDL_SCANCODE_SPACE || ev.scancode == SDLConstants.SDL_SCANCODE_RETURN) {
			checked = !checked;
		}
	}

	@Override
	public void render() {
		if(!visible) return;

		int boxSize = 14;
		int boxY = y + (h - boxSize) / 2;
		boolean active = focused || hovering;

		// Box background
		fillRect(x, boxY, boxSize, boxSize, 16, 16, 24, 220);
		drawRect(x, boxY, boxSize, boxSize,
				enabled ? (active ? 255 : 180) : 96,
				enabled ? (active ? 255 : 180) : 96,
				enabled ? (active ? 255 : 180) : 96, 255);

		// Check mark: filled inner rect when checked
		if(checked) {
			fillRect(x + 3, boxY + 3, boxSize - 6, boxSize - 6,
					enabled ? 0 : 64,
					enabled ? 200 : 96,
					enabled ? 80 : 64, 255);
		}

		if(label != null && label.length() > 0) {
			int ty = y + (h - 16) / 2;
			int color = enabled ? (active ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_WHITE) : NormalFontSDL.COLOR_DARKBLUE;
			NormalFontSDL.printFont(x + boxSize + 6, ty, label, color);
		}
	}
}
