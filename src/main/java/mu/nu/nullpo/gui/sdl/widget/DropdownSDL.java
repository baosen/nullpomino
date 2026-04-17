package mu.nu.nullpo.gui.sdl.widget;

import java.util.ArrayList;
import java.util.List;

import mu.nu.nullpo.gui.sdl.NormalFontSDL;
import mu.nu.nullpo.gui.sdl.NullpoMinoSDL;
import mu.nu.nullpo.gui.sdl.binding.SDLConstants;

/**
 * Closed-by-default combobox. Shows the selected item in a box with a caret.
 * Clicking opens an inline drop list; clicking an item selects it and closes.
 *
 * The drop list is clamped to at most {@code maxDropVisibleItems}; mouse wheel
 * scrolls within that window.
 */
public class DropdownSDL extends WidgetSDL {
	private final List<String> items = new ArrayList<>();
	private int selected = -1;
	private boolean open;
	private int dropScroll;
	public int maxDropVisibleItems = 10;
	private boolean hoveringClosed;

	public DropdownSDL(int x, int y, int w, int h) {
		super(x, y, w, h);
	}

	public DropdownSDL(int x, int y, int w, int h, String[] initialItems) {
		this(x, y, w, h);
		setItems(initialItems);
	}

	public void setItems(String[] arr) {
		items.clear();
		if(arr != null) for(String s : arr) items.add(s);
		if(selected >= items.size()) selected = items.size() - 1;
		if(selected < 0 && !items.isEmpty()) selected = 0;
	}

	public void setItems(List<String> list) {
		items.clear();
		if(list != null) items.addAll(list);
		if(selected >= items.size()) selected = items.size() - 1;
		if(selected < 0 && !items.isEmpty()) selected = 0;
	}

	public int getSelectedIndex() { return selected; }
	public String getSelectedItem() {
		if(selected < 0 || selected >= items.size()) return "";
		return items.get(selected);
	}
	public void setSelectedIndex(int i) {
		selected = (i < 0) ? -1 : (i >= items.size() ? items.size() - 1 : i);
	}

	public boolean isOpen() { return open; }
	public void close() { open = false; }

	@Override
	public void setFocused(boolean on) {
		super.setFocused(on);
		if(!on) open = false;
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		if(!visible || !enabled) { hoveringClosed = false; return false; }
		hoveringClosed = containsPoint(mx, my);

		float wheel = NullpoMinoSDL.mouseWheelDelta;

		if(open) {
			int dropX = x;
			int dropY = y + h;
			int itemH = h;
			int visibleCount = Math.min(items.size(), maxDropVisibleItems);
			int dropH = itemH * Math.max(1, visibleCount);

			if(wheel != 0 && mx >= dropX && mx < dropX + w && my >= dropY && my < dropY + dropH) {
				dropScroll -= (int)wheel;
				clampScroll(visibleCount);
			}

			if(leftJustPressed) {
				if(mx >= dropX && mx < dropX + w && my >= dropY && my < dropY + dropH) {
					int idx = dropScroll + (my - dropY) / itemH;
					if(idx >= 0 && idx < items.size()) {
						selected = idx;
						open = false;
						return true;
					}
				} else if(!containsPoint(mx, my)) {
					open = false;
				}
			}
		} else if(leftJustPressed && hoveringClosed) {
			open = true;
			int visibleCount = Math.min(items.size(), maxDropVisibleItems);
			if(selected >= 0) {
				dropScroll = Math.max(0, selected - visibleCount / 2);
			} else {
				dropScroll = 0;
			}
			clampScroll(visibleCount);
			return true;
		}
		return false;
	}

	private void clampScroll(int visibleCount) {
		int maxScroll = Math.max(0, items.size() - visibleCount);
		if(dropScroll < 0) dropScroll = 0;
		if(dropScroll > maxScroll) dropScroll = maxScroll;
	}

	@Override
	public void handleKey(NullpoMinoSDL.KeyEvent ev) {
		if(!enabled || items.isEmpty()) return;
		switch(ev.scancode) {
			case SDLConstants.SDL_SCANCODE_DOWN:
				selected = Math.min(items.size() - 1, selected + 1);
				break;
			case SDLConstants.SDL_SCANCODE_UP:
				selected = Math.max(0, selected - 1);
				break;
			case SDLConstants.SDL_SCANCODE_SPACE:
			case SDLConstants.SDL_SCANCODE_RETURN:
				open = !open;
				break;
			case SDLConstants.SDL_SCANCODE_ESCAPE:
				open = false;
				break;
			default:
				break;
		}
	}

	@Override
	public void render() {
		if(!visible) return;

		panelBackground(x, y, w, h, focused || hoveringClosed);

		String label = getSelectedItem();
		int innerX = x + 4;
		int innerY = y + (h - 16) / 2;
		int maxChars = Math.max(1, (w - 20) / 16);
		if(label.length() > maxChars) label = label.substring(0, maxChars);
		NormalFontSDL.printFont(innerX, innerY, label,
				enabled ? NormalFontSDL.COLOR_WHITE : NormalFontSDL.COLOR_DARKBLUE);

		// Caret glyph on the right edge
		NormalFontSDL.printFont(x + w - 18, innerY, open ? "^" : "v",
				enabled ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_DARKBLUE);
	}

	/**
	 * Draw the open drop list, if any. Call AFTER render() of surrounding widgets so the
	 * dropdown renders on top.
	 */
	public void renderOverlay(int mx, int my) {
		if(!visible || !open) return;

		int dropX = x;
		int dropY = y + h;
		int itemH = h;
		int visibleCount = Math.min(items.size(), maxDropVisibleItems);
		int dropH = itemH * Math.max(1, visibleCount);

		fillRect(dropX, dropY, w, dropH, 16, 16, 32, 240);
		drawRect(dropX, dropY, w, dropH, 255, 255, 255, 255);

		for(int i = 0; i < visibleCount; i++) {
			int idx = dropScroll + i;
			if(idx >= items.size()) break;
			int iy = dropY + i * itemH;
			boolean hover = mx >= dropX && mx < dropX + w && my >= iy && my < iy + itemH;
			if(hover) fillRect(dropX + 1, iy + 1, w - 2, itemH - 2, 64, 64, 96, 255);
			if(idx == selected) drawRect(dropX + 2, iy + 2, w - 4, itemH - 4, 255, 255, 0, 255);
			String s = items.get(idx);
			int maxChars = Math.max(1, (w - 8) / 16);
			if(s.length() > maxChars) s = s.substring(0, maxChars);
			NormalFontSDL.printFont(dropX + 4, iy + (itemH - 16) / 2, s,
					hover ? NormalFontSDL.COLOR_YELLOW : NormalFontSDL.COLOR_WHITE);
		}
	}
}
