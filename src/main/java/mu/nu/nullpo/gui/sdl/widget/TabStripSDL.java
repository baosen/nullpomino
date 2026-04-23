package mu.nu.nullpo.gui.sdl.widget;

import mu.nu.nullpo.gui.sdl.NormalFontSDL;

/**
 * Horizontal tab header strip. Each tab is sized by its label length so no
 * label clips unless the strip's total width {@code w} is smaller than the
 * sum of required widths; any spare width is shared equally across tabs as
 * extra padding. Clicking a tab sets the active index; {@link #getActiveTab()}
 * reads it.
 */
public class TabStripSDL extends WidgetSDL {
	private String[] labels;
	/** Per-tab starting x offset from {@link #x}, recomputed on label changes. */
	private int[] tabX;
	/** Per-tab width in pixels, recomputed on label changes. */
	private int[] tabW;
	private int active = 0;

	public TabStripSDL(int x, int y, int w, int h, String[] labels) {
		super(x, y, w, h);
		this.labels = (labels == null) ? new String[0] : labels;
		computeLayout();
	}

	public int getActiveTab() { return active; }
	public void setActiveTab(int i) {
		if(i < 0) i = 0;
		if(labels.length == 0) { active = 0; return; }
		if(i >= labels.length) i = labels.length - 1;
		active = i;
	}

	/**
	 * Compute per-tab widths: each tab gets its minimum label width
	 * ({@code 16 * chars + 4} per the NormalFontSDL render contract) plus an
	 * equal share of the strip's spare width. If the minimums overflow
	 * {@link #w}, widths are scaled down proportionally and labels will clip.
	 */
	private void computeLayout() {
		int n = labels.length;
		tabX = new int[n];
		tabW = new int[n];
		if(n == 0) return;

		int[] minW = new int[n];
		int sumMin = 0;
		for(int i = 0; i < n; i++) {
			minW[i] = labels[i].length() * 16 + 4;
			sumMin += minW[i];
		}

		if(sumMin >= w) {
			// Labels overflow: divide the strip proportionally to label length.
			// Accumulate via running total so all pixels are used.
			int used = 0;
			for(int i = 0; i < n - 1; i++) {
				int wi = (int)((long)minW[i] * w / sumMin);
				tabX[i] = used;
				tabW[i] = wi;
				used += wi;
			}
			tabX[n - 1] = used;
			tabW[n - 1] = w - used;
			return;
		}

		// Spare width is shared equally across all tabs.
		int slack = w - sumMin;
		int perTab = slack / n;
		int leftover = slack - perTab * n;
		int cx = 0;
		for(int i = 0; i < n; i++) {
			int wi = minW[i] + perTab + (i < leftover ? 1 : 0);
			tabX[i] = cx;
			tabW[i] = wi;
			cx += wi;
		}
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		if(!visible || !enabled || labels.length == 0) return false;
		if(!leftJustPressed) return false;
		if(my < y || my >= y + h) return false;
		for(int i = 0; i < labels.length; i++) {
			int tx = x + tabX[i];
			if(mx >= tx && mx < tx + tabW[i]) {
				if(i != active) { active = i; return true; }
				return false;
			}
		}
		return false;
	}

	@Override
	public void render() {
		if(!visible || labels.length == 0) return;
		for(int i = 0; i < labels.length; i++) {
			int tx = x + tabX[i];
			int tw = tabW[i];
			boolean isActive = (i == active);
			int r = isActive ? 64 : 24;
			int g = isActive ? 64 : 24;
			int b = isActive ? 128 : 40;
			fillRect(tx, y, tw, h, r, g, b, 220);
			drawRect(tx, y, tw, h, isActive ? 255 : 128, isActive ? 255 : 128, isActive ? 255 : 128, 255);
			String s = NormalFontSDL.safeString(labels[i]);
			int maxChars = Math.max(1, (tw - 4) / 16);
			if(s.length() > maxChars) s = s.substring(0, maxChars);
			int textX = tx + (tw - s.length() * 16) / 2;
			int textY = y + (h - 16) / 2;
			NormalFontSDL.printFont(textX, textY, s,
					isActive ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_WHITE);
		}
		// Bottom accent line under active tab
		int accentX = x + tabX[active];
		fillRect(accentX, y + h - 2, tabW[active], 2, 255, 255, 0, 255);
	}
}
