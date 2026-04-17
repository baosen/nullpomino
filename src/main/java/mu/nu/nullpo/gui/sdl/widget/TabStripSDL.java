package mu.nu.nullpo.gui.sdl.widget;

import mu.nu.nullpo.gui.sdl.NormalFontSDL;

/**
 * Horizontal tab header strip. Tabs share the full width {@code w}. Clicking a tab
 * sets the active index; {@link #getActiveTab()} reads it.
 */
public class TabStripSDL extends WidgetSDL {
	private String[] labels;
	private int active = 0;

	public TabStripSDL(int x, int y, int w, int h, String[] labels) {
		super(x, y, w, h);
		this.labels = (labels == null) ? new String[0] : labels;
	}

	public int getActiveTab() { return active; }
	public void setActiveTab(int i) {
		if(i < 0) i = 0;
		if(labels.length == 0) { active = 0; return; }
		if(i >= labels.length) i = labels.length - 1;
		active = i;
	}

	public void setLabels(String[] labels) {
		this.labels = (labels == null) ? new String[0] : labels;
		if(active >= this.labels.length) active = Math.max(0, this.labels.length - 1);
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		if(!visible || !enabled || labels.length == 0) return false;
		if(!leftJustPressed) return false;
		if(my < y || my >= y + h) return false;
		int tabW = w / labels.length;
		if(mx < x || mx >= x + tabW * labels.length) return false;
		int idx = (mx - x) / tabW;
		if(idx >= 0 && idx < labels.length && idx != active) {
			active = idx;
			return true;
		}
		return false;
	}

	@Override
	public void render() {
		if(!visible || labels.length == 0) return;
		int tabW = w / labels.length;
		for(int i = 0; i < labels.length; i++) {
			int tx = x + i * tabW;
			boolean isActive = (i == active);
			int r = isActive ? 64 : 24;
			int g = isActive ? 64 : 24;
			int b = isActive ? 128 : 40;
			fillRect(tx, y, tabW, h, r, g, b, 220);
			drawRect(tx, y, tabW, h, isActive ? 255 : 128, isActive ? 255 : 128, isActive ? 255 : 128, 255);
			String s = NormalFontSDL.safeString(labels[i]);
			int maxChars = Math.max(1, (tabW - 4) / 16);
			if(s.length() > maxChars) s = s.substring(0, maxChars);
			int textX = tx + (tabW - s.length() * 16) / 2;
			int textY = y + (h - 16) / 2;
			NormalFontSDL.printFont(textX, textY, s,
					isActive ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_WHITE);
		}
		// Bottom accent line under active tab
		int accentX = x + active * tabW;
		fillRect(accentX, y + h - 2, tabW, 2, 255, 255, 0, 255);
	}
}
