package mu.nu.nullpo.gui.sdl.widget;

import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Iterator;

import mu.nu.nullpo.gui.sdl.NormalFontSDL;
import mu.nu.nullpo.gui.sdl.NullpoMinoSDL;

/**
 * Bounded ring-buffer scrollable chat log. Entries are appended from any thread
 * (append methods are synchronized); rendering pulls a snapshot and draws the
 * visible tail. Scroll is sticky-to-bottom: appending new entries while the user
 * is scrolled away leaves their view stationary, but appending while at the
 * bottom keeps them at the bottom.
 */
public class ChatLogSDL extends WidgetSDL {
	public static class Entry {
		public final long timestampMs;
		public final String prefix;      // e.g. "[12:34:56] name: " — cached at append
		public final String body;
		public final int color;           // NormalFontSDL.COLOR_*
		public Entry(long t, String prefix, String body, int color) {
			this.timestampMs = t;
			this.prefix = prefix;
			this.body = body;
			this.color = color;
		}
	}

	private final ArrayDeque<Entry> entries = new ArrayDeque<>();
	public int capacity = 500;
	/** Characters visible per line before wrapping. Set by width, computed lazily. */
	public int lineWrapChars = -1;
	/** Lines scrolled up from the bottom. 0 = latest at bottom. */
	private int scrollUpLines = 0;

	public ChatLogSDL(int x, int y, int w, int h) {
		super(x, y, w, h);
	}

	public synchronized void appendSystem(String msg, int color) {
		String prefix = "[" + formatClock(Calendar.getInstance()) + "] ";
		push(new Entry(System.currentTimeMillis(), prefix, msg, color));
	}

	public synchronized void appendUser(String user, Calendar time, String msg) {
		String prefix = "[" + formatClock(time) + "] <" + user + "> ";
		push(new Entry(time.getTimeInMillis(), prefix, msg, NormalFontSDL.COLOR_WHITE));
	}

	public synchronized void clear() {
		entries.clear();
		scrollUpLines = 0;
	}

	private void push(Entry e) {
		entries.addLast(e);
		while(entries.size() > capacity) entries.removeFirst();
	}

	private static String formatClock(Calendar c) {
		return String.format("%02d:%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
	}

	@Override
	public boolean update(int mx, int my, boolean leftJustPressed) {
		if(!visible) return false;
		if(containsPoint(mx, my)) {
			float wheel = NullpoMinoSDL.mouseWheelDelta;
			if(wheel != 0) {
				scrollUpLines += (int)wheel;
				if(scrollUpLines < 0) scrollUpLines = 0;
			}
		}
		return false;
	}

	@Override
	public void handleKey(mu.nu.nullpo.gui.sdl.NullpoMinoSDL.KeyEvent ev) {
		// Handled by the owning screen (PageUp/PageDown binding is more natural there).
	}

	public synchronized void pageUp()   { scrollUpLines += Math.max(1, visibleLines() - 1); }
	public synchronized void pageDown() {
		scrollUpLines -= Math.max(1, visibleLines() - 1);
		if(scrollUpLines < 0) scrollUpLines = 0;
	}
	public synchronized void scrollToBottom() { scrollUpLines = 0; }

	private int visibleLines() { return Math.max(1, (h - 4) / 16); }

	private int wrapChars() {
		if(lineWrapChars > 0) return lineWrapChars;
		return Math.max(10, (w - 8) / 16);
	}

	@Override
	public synchronized void render() {
		if(!visible) return;

		fillRect(x, y, w, h, 0, 0, 0, 180);
		drawRect(x, y, w, h, focused ? 220 : 140, focused ? 220 : 140, focused ? 220 : 140, 255);

		int visible = visibleLines();
		int wrap = wrapChars();

		// Build a flat line list from entries, wrapping long bodies. Iterate oldest-first
		// so that the resulting array is oldest..newest; then render the visible tail.
		java.util.ArrayList<RenderedLine> lines = new java.util.ArrayList<>(entries.size() + 8);
		Iterator<Entry> it = entries.iterator();
		while(it.hasNext()) {
			Entry e = it.next();
			String full = e.prefix + e.body;
			if(full.length() <= wrap) {
				lines.add(new RenderedLine(full, e.color));
			} else {
				int pos = 0;
				while(pos < full.length()) {
					int end = Math.min(full.length(), pos + wrap);
					lines.add(new RenderedLine(full.substring(pos, end), e.color));
					pos = end;
				}
			}
		}

		int totalLines = lines.size();
		int maxScroll = Math.max(0, totalLines - visible);
		if(scrollUpLines > maxScroll) scrollUpLines = maxScroll;
		int startLine = Math.max(0, totalLines - visible - scrollUpLines);
		int endLine = Math.min(totalLines, startLine + visible);

		int drawY = y + 2;
		for(int i = startLine; i < endLine; i++) {
			RenderedLine ln = lines.get(i);
			NormalFontSDL.printFont(x + 4, drawY, ln.text, ln.color);
			drawY += 16;
		}

		// Scroll indicator (right edge, when scrolled up)
		if(scrollUpLines > 0) {
			int sbX = x + w - 4;
			int sbY = y + 2;
			int sbH = h - 4;
			fillRect(sbX, sbY, 4, sbH, 32, 32, 48, 200);
			int thumbH = Math.max(8, sbH * visible / Math.max(1, totalLines));
			int thumbY = sbY + (sbH - thumbH) * (maxScroll - scrollUpLines) / Math.max(1, maxScroll);
			fillRect(sbX, thumbY, 4, thumbH, 180, 180, 220, 255);
		}
	}

	private static final class RenderedLine {
		final String text;
		final int color;
		RenderedLine(String text, int color) { this.text = text; this.color = color; }
	}
}
