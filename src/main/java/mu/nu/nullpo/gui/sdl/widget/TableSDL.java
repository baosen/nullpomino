package mu.nu.nullpo.gui.sdl.widget;

import java.util.ArrayList;
import java.util.List;

import mu.nu.nullpo.gui.sdl.NormalFontSDL;
import mu.nu.nullpo.gui.sdl.NullpoMinoSDL;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;

/**
 * Fixed-schema tabular data display with selectable rows and vertical scroll.
 * Rows are {@code String[]} arrays matching the column count. Supports keyboard
 * navigation (Up/Down + Page Up/Down), click selection, double-click activation,
 * and mouse wheel scroll.
 */
public class TableSDL extends WidgetSDL {
	public static class Column {
		public final String header;
		public final int widthPx;
		public Column(String header, int widthPx) {
			this.header = header;
			this.widthPx = widthPx;
		}
	}

	public final Column[] columns;
	private final List<String[]> rows = new ArrayList<>();
	/** Optional per-row color override (indexes into NormalFontSDL.COLOR_*). -1 = default. */
	private final List<Integer> rowColors = new ArrayList<>();
	public int rowHeight = 18;
	public int headerHeight = 20;

	private int selected = -1;
	private int scroll = 0;
	private long lastClickAt = 0;
	private int lastClickRow = -1;
	/** Set to true for one frame after a double-click on a row. */
	public boolean activated = false;

	public TableSDL(int x, int y, int w, int h, Column[] columns) {
		super(x, y, w, h);
		this.columns = (columns == null) ? new Column[0] : columns;
	}

	public int getSelectedIndex() { return selected; }
	public void setSelectedIndex(int i) {
		if(i < 0 || i >= rows.size()) { selected = -1; return; }
		selected = i;
		ensureVisible(selected);
	}
	public int getRowCount() { return rows.size(); }
	public String[] getRow(int i) {
		if(i < 0 || i >= rows.size()) return null;
		return rows.get(i);
	}

	public void clear() {
		rows.clear();
		rowColors.clear();
		selected = -1;
		scroll = 0;
	}

	public void addRow(String[] row) {
		rows.add(row);
		rowColors.add(-1);
	}

	public void addRow(String[] row, int fontColor) {
		rows.add(row);
		rowColors.add(fontColor);
	}

	public void setRow(int i, String[] row) {
		if(i < 0 || i >= rows.size()) return;
		rows.set(i, row);
	}

	public void setRowColor(int i, int fontColor) {
		if(i < 0 || i >= rows.size()) return;
		rowColors.set(i, fontColor);
	}

	private int visibleRowCount() {
		return Math.max(1, (h - headerHeight) / rowHeight);
	}

	private void ensureVisible(int idx) {
		int vis = visibleRowCount();
		if(idx < scroll) scroll = idx;
		else if(idx >= scroll + vis) scroll = idx - vis + 1;
		clampScroll();
	}

	private void clampScroll() {
		int vis = visibleRowCount();
		int maxScroll = Math.max(0, rows.size() - vis);
		if(scroll < 0) scroll = 0;
		if(scroll > maxScroll) scroll = maxScroll;
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		activated = false;
		if(!visible || !enabled) return false;

		// Wheel scroll
		float wheel = NullpoMinoSDL.mouseWheelDelta;
		if(wheel != 0 && containsPoint(mx, my)) {
			scroll -= (int)wheel;
			clampScroll();
		}

		if(leftJustPressed && containsPoint(mx, my) && my >= y + headerHeight) {
			int rowIdx = scroll + (my - (y + headerHeight)) / rowHeight;
			if(rowIdx >= 0 && rowIdx < rows.size()) {
				long now = System.currentTimeMillis();
				boolean doubleClicked = (rowIdx == lastClickRow && (now - lastClickAt) < 400);
				selected = rowIdx;
				lastClickRow = rowIdx;
				lastClickAt = now;
				if(doubleClicked) {
					activated = true;
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void handleKey(NullpoMinoSDL.KeyEvent ev) {
		if(!enabled || rows.isEmpty()) return;
		int vis = visibleRowCount();
		switch(ev.scancode) {
			case SDLConstants.SDL_SCANCODE_UP:
				if(selected > 0) { selected--; ensureVisible(selected); }
				break;
			case SDLConstants.SDL_SCANCODE_DOWN:
				if(selected < rows.size() - 1) { selected++; ensureVisible(selected); }
				break;
			case SDLConstants.SDL_SCANCODE_PAGEUP:
				selected = Math.max(0, selected - vis);
				ensureVisible(selected);
				break;
			case SDLConstants.SDL_SCANCODE_PAGEDOWN:
				selected = Math.min(rows.size() - 1, selected + vis);
				ensureVisible(selected);
				break;
			case SDLConstants.SDL_SCANCODE_HOME:
				selected = 0; ensureVisible(0);
				break;
			case SDLConstants.SDL_SCANCODE_END:
				selected = rows.size() - 1; ensureVisible(selected);
				break;
			case SDLConstants.SDL_SCANCODE_RETURN:
			case SDLConstants.SDL_SCANCODE_KP_ENTER:
				if(selected >= 0) activated = true;
				break;
			default:
				break;
		}
	}

	@Override
	public void render() {
		if(!visible) return;

		// Background
		fillRect(x, y, w, h, 0, 0, 0, 200);

		// Header
		fillRect(x, y, w, headerHeight, 32, 32, 64, 240);
		int colX = x + 4;
		for(Column c : columns) {
			String s = c.header == null ? "" : c.header;
			int maxChars = Math.max(1, (c.widthPx - 4) / 16);
			if(s.length() > maxChars) s = s.substring(0, maxChars);
			NormalFontSDL.printFont(colX, y + (headerHeight - 16) / 2, s, NormalFontSDL.COLOR_YELLOW);
			colX += c.widthPx;
		}
		fillRect(x, y + headerHeight - 1, w, 1, 128, 128, 128, 255);

		// Rows
		int vis = visibleRowCount();
		for(int i = 0; i < vis; i++) {
			int rowIdx = scroll + i;
			if(rowIdx >= rows.size()) break;
			int ry = y + headerHeight + i * rowHeight;
			if(rowIdx == selected) {
				fillRect(x + 1, ry, w - 2, rowHeight, 48, 48, 128, 220);
			}
			String[] row = rows.get(rowIdx);
			int rowColor = rowColors.get(rowIdx);
			if(rowColor < 0) rowColor = (rowIdx == selected) ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_WHITE;

			int cx = x + 4;
			for(int c = 0; c < columns.length && c < row.length; c++) {
				String s = row[c] == null ? "" : row[c];
				int colWidthChars = Math.max(1, (columns[c].widthPx - 4) / 16);
				if(s.length() > colWidthChars) s = s.substring(0, colWidthChars);
				NormalFontSDL.printFont(cx, ry + (rowHeight - 16) / 2, s, rowColor);
				cx += columns[c].widthPx;
			}
		}

		// Border
		drawRect(x, y, w, h, focused ? 255 : 160, focused ? 255 : 160, focused ? 255 : 160, 255);

		// Scrollbar (drawn on the right edge if rows overflow)
		if(rows.size() > vis) {
			int sbX = x + w - 4;
			int sbY = y + headerHeight;
			int sbH = h - headerHeight;
			fillRect(sbX, sbY, 4, sbH, 32, 32, 48, 200);
			int thumbH = Math.max(8, sbH * vis / rows.size());
			int thumbY = sbY + (sbH - thumbH) * scroll / Math.max(1, rows.size() - vis);
			fillRect(sbX, thumbY, 4, thumbH, 180, 180, 220, 255);
		}
	}
}
