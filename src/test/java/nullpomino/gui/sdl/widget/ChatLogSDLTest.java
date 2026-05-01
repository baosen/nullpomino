package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Calendar;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NormalFontSDL;

/**
 * Pins ChatLogSDL's append, capacity, and clear contracts. The
 * lobby chat panel feeds into this widget from any thread, so
 * appendSystem / appendUser / clear are synchronized — but the
 * round-trip behaviour (prefix routing, capacity drop, colour
 * tagging, sticky-to-bottom scroll on entry) is what the consumer
 * sees and must keep working. Render is SDL-bound and skipped here.
 */
class ChatLogSDLTest {

	@SuppressWarnings("unchecked")
	private static ArrayDeque<ChatLogSDL.Entry> entries(ChatLogSDL log) throws Exception {
		Field f = ChatLogSDL.class.getDeclaredField("entries");
		f.setAccessible(true);
		return (ArrayDeque<ChatLogSDL.Entry>) f.get(log);
	}

	private static int scrollUpLines(ChatLogSDL log) throws Exception {
		Field f = ChatLogSDL.class.getDeclaredField("scrollUpLines");
		f.setAccessible(true);
		return f.getInt(log);
	}

	@Test
	void appendSystemStoresMessageWithEmptyPrefixAndChosenColor() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);

		log.appendSystem("server up", NormalFontSDL.COLOR_RED);

		ArrayDeque<ChatLogSDL.Entry> q = entries(log);
		assertEquals(1, q.size());
		ChatLogSDL.Entry e = q.peekFirst();
		assertEquals("", e.prefix);
		assertEquals("server up", e.body);
		assertEquals(NormalFontSDL.COLOR_RED, e.bodyColor);
	}

	@Test
	void appendUserStoresPrefixWithCyanLabelAndWhiteBody() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		Calendar t = Calendar.getInstance();

		log.appendUser("alice", t, "hi there");

		ArrayDeque<ChatLogSDL.Entry> q = entries(log);
		assertEquals(1, q.size());
		ChatLogSDL.Entry e = q.peekFirst();
		assertEquals("alice:", e.prefix);
		assertEquals(NormalFontSDL.COLOR_CYAN, e.prefixColor);
		assertEquals("hi there", e.body);
		assertEquals(NormalFontSDL.COLOR_WHITE, e.bodyColor);
		assertEquals(t.getTimeInMillis(), e.timestampMs);
	}

	@Test
	void capacityCapDropsOldestEntriesOverLimit() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		log.capacity = 3;

		log.appendSystem("a", NormalFontSDL.COLOR_WHITE);
		log.appendSystem("b", NormalFontSDL.COLOR_WHITE);
		log.appendSystem("c", NormalFontSDL.COLOR_WHITE);
		log.appendSystem("d", NormalFontSDL.COLOR_WHITE);

		ArrayDeque<ChatLogSDL.Entry> q = entries(log);
		assertEquals(3, q.size());
		assertEquals("b", q.peekFirst().body, "oldest entry dropped on overflow");
		assertEquals("d", q.peekLast().body);
	}

	@Test
	void clearEmptiesEntriesAndResetsScroll() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		log.appendSystem("a", NormalFontSDL.COLOR_WHITE);
		log.appendSystem("b", NormalFontSDL.COLOR_WHITE);
		log.pageUp();
		assertTrue(scrollUpLines(log) > 0, "pageUp should have moved scroll up");

		log.clear();

		assertEquals(0, entries(log).size());
		assertEquals(0, scrollUpLines(log), "clear resets scroll to bottom");
	}

	@Test
	void pageUpAdvancesScrollAndPageDownReverses() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		// Need entries so visibleLines() > 1
		for (int i = 0; i < 50; i++) {
			log.appendSystem("line " + i, NormalFontSDL.COLOR_WHITE);
		}

		int before = scrollUpLines(log);
		log.pageUp();
		assertTrue(scrollUpLines(log) > before, "pageUp must advance scroll");

		log.pageDown();
		assertEquals(before, scrollUpLines(log));
	}

	@Test
	void pageDownClampsAtZeroBottomScroll() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		log.appendSystem("a", NormalFontSDL.COLOR_WHITE);

		log.pageDown();
		assertEquals(0, scrollUpLines(log), "pageDown can't go below the latest entry");
	}

	@Test
	void scrollToBottomZeroesScrollEvenAfterPagingUp() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		for (int i = 0; i < 50; i++) {
			log.appendSystem("line " + i, NormalFontSDL.COLOR_WHITE);
		}
		log.pageUp();
		log.pageUp();

		log.scrollToBottom();

		assertEquals(0, scrollUpLines(log));
	}

	@Test
	void inheritedContainsPointReflectsWidgetGeometry() {
		ChatLogSDL log = new ChatLogSDL(10, 20, 100, 50);

		assertTrue(log.containsPoint(50, 30));
		assertTrue(log.containsPoint(10, 20)); // top-left inclusive
		// containsPoint uses < x+w and < y+h, so the right/bottom edge is exclusive
		assertEquals(false, log.containsPoint(110, 30));
	}

	@Test
	void appendUserAndSystemBothLandInEntriesQueueInOrder() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);

		log.appendSystem("server up", NormalFontSDL.COLOR_GREEN);
		log.appendUser("alice", Calendar.getInstance(), "hi");
		log.appendSystem("server down", NormalFontSDL.COLOR_RED);

		ArrayDeque<ChatLogSDL.Entry> q = entries(log);
		assertEquals(3, q.size());
		ChatLogSDL.Entry[] arr = q.toArray(new ChatLogSDL.Entry[0]);
		assertEquals("server up", arr[0].body);
		assertEquals("hi", arr[1].body);
		assertEquals("alice:", arr[1].prefix);
		assertEquals("server down", arr[2].body);
	}

	@Test
	void entryConstructorBundlesAllFields() {
		ChatLogSDL.Entry e = new ChatLogSDL.Entry(
				12345L, "alice:", NormalFontSDL.COLOR_CYAN, "hi", NormalFontSDL.COLOR_WHITE);

		assertEquals(12345L, e.timestampMs);
		assertEquals("alice:", e.prefix);
		assertEquals(NormalFontSDL.COLOR_CYAN, e.prefixColor);
		assertEquals("hi", e.body);
		assertEquals(NormalFontSDL.COLOR_WHITE, e.bodyColor);
	}

	@Test
	void freshLogHasZeroEntriesAndZeroScroll() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 100, 50);

		assertNotNull(entries(log));
		assertEquals(0, entries(log).size());
		assertEquals(0, scrollUpLines(log));
	}
}
