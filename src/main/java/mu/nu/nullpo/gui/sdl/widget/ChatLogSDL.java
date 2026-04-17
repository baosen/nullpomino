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
		/** Leading label (e.g. "name: " for user chat, "" for system messages). */
		public final String prefix;
		public final String body;
		/** Colour for {@link #body}. */
		public final int bodyColor;
		/** Colour for {@link #prefix}; ignored when the prefix is empty. */
		public final int prefixColor;
		public Entry(long t, String prefix, int prefixColor, String body, int bodyColor) {
			this.timestampMs = t;
			this.prefix = prefix;
			this.prefixColor = prefixColor;
			this.body = body;
			this.bodyColor = bodyColor;
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
		push(new Entry(System.currentTimeMillis(), "", color, msg, color));
	}

	public synchronized void appendUser(String user, Calendar time, String msg) {
		// 'name: ' label in cyan, message body in plain white so the speaker
		// stands out at a glance.
		String prefix = user + ": ";
		push(new Entry(time.getTimeInMillis(), prefix, NormalFontSDL.COLOR_CYAN, msg, NormalFontSDL.COLOR_WHITE));
	}

	public synchronized void clear() {
		entries.clear();
		scrollUpLines = 0;
	}

	private void push(Entry e) {
		entries.addLast(e);
		while(entries.size() > capacity) entries.removeFirst();
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

		// Build a flat list of rendered lines from oldest to newest. Each line can
		// have an optional prefix segment (rendered in a different colour, e.g.
		// the speaker's name) followed by a body segment. Only the first line of
		// a wrapped entry carries the prefix; continuation lines inherit the body
		// colour.
		java.util.ArrayList<RenderedLine> lines = new java.util.ArrayList<>(entries.size() + 8);
		Iterator<Entry> it = entries.iterator();
		while(it.hasNext()) {
			Entry e = it.next();
			int prefLen = e.prefix.length();
			int bodyLen = e.body.length();

			if(prefLen + bodyLen <= wrap) {
				lines.add(new RenderedLine(e.prefix, e.prefixColor, e.body, e.bodyColor));
				continue;
			}

			// First line: prefix + however much body fits alongside it.
			int firstBodyEnd = Math.max(0, Math.min(bodyLen, wrap - prefLen));
			lines.add(new RenderedLine(e.prefix, e.prefixColor,
					e.body.substring(0, firstBodyEnd), e.bodyColor));

			// Continuation lines: wrap the remainder of the body in the body colour only.
			int pos = firstBodyEnd;
			while(pos < bodyLen) {
				int end = Math.min(pos + wrap, bodyLen);
				lines.add(new RenderedLine("", e.bodyColor, e.body.substring(pos, end), e.bodyColor));
				pos = end;
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
			if(ln.prefix.length() > 0) {
				String prefSafe = NormalFontSDL.safeString(ln.prefix);
				NormalFontSDL.printFont(x + 4, drawY, prefSafe, ln.prefixColor);
				NormalFontSDL.printFont(x + 4 + prefSafe.length() * 16, drawY,
						NormalFontSDL.safeString(ln.body), ln.bodyColor);
			} else {
				NormalFontSDL.printFont(x + 4, drawY, NormalFontSDL.safeString(ln.body), ln.bodyColor);
			}
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
		final String prefix; final int prefixColor;
		final String body;   final int bodyColor;
		RenderedLine(String prefix, int prefixColor, String body, int bodyColor) {
			this.prefix = prefix;
			this.prefixColor = prefixColor;
			this.body = body;
			this.bodyColor = bodyColor;
		}
	}
}
