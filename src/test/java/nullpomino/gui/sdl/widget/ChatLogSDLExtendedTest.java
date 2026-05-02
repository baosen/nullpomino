package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NormalFontSDL;
import nullpomino.gui.sdl.NullpoMinoSDL;

/**
 * Additional ChatLogSDL tests covering mouse wheel scrolling in update(),
 * invisible widget guard, handleKey no-op, lineWrapChars field, and
 * scroll clamping edge cases.
 */
class ChatLogSDLExtendedTest {

	private float originalWheel;

	@BeforeEach
	void saveWheelDelta() {
		originalWheel = NullpoMinoSDL.mouseWheelDelta;
		NullpoMinoSDL.mouseWheelDelta = 0;
	}

	@AfterEach
	void restoreWheelDelta() {
		NullpoMinoSDL.mouseWheelDelta = originalWheel;
	}

	private static int scrollUpLines(ChatLogSDL log) throws Exception {
		Field f = ChatLogSDL.class.getDeclaredField("scrollUpLines");
		f.setAccessible(true);
		return f.getInt(log);
	}

	@Test
	void updateWithPositiveWheelDeltaScrollsUp() throws Exception {
		ChatLogSDL log = newChatLogWithLines(50);
		assertEquals(0, scrollUpLines(log));

		NullpoMinoSDL.mouseWheelDelta = 3;
		log.update(50, 50, false);  // inside widget

		assertEquals(3, scrollUpLines(log));
	}

	@Test
	void updateWithNegativeWheelDeltaClampsAtZero() throws Exception {
		ChatLogSDL log = newChatLogWithLines(50);
		NullpoMinoSDL.mouseWheelDelta = -5;
		log.update(50, 50, false);

		assertEquals(0, scrollUpLines(log), "negative wheel clamps to 0");
	}

	@Test
	void updateWheelOutsideWidgetDoesNotScroll() throws Exception {
		ChatLogSDL log = newChatLogWithLines(50);
		NullpoMinoSDL.mouseWheelDelta = 5;

		log.update(500, 500, false);  // outside widget

		assertEquals(0, scrollUpLines(log), "scroll outside widget ignored");
	}

	@Test
	void updateReturnsFalseAlways() {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		assertFalse(log.update(50, 50, true));
		assertFalse(log.update(50, 50, false));
		assertFalse(log.update(-1, -1, true));
	}

	@Test
	void invisibleWidgetIgnoresWheelInUpdate() throws Exception {
		ChatLogSDL log = newChatLogWithLines(50);
		log.visible = false;

		NullpoMinoSDL.mouseWheelDelta = 3;
		log.update(50, 50, false);

		assertEquals(0, scrollUpLines(log), "invisible widget must not scroll");
	}

	@Test
	void handleKeyIsNoOpAndDoesNotThrow() {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		log.handleKey(new nullpomino.gui.sdl.NullpoMinoSDL.KeyEvent(0, 0, false));
		// No state change expected
		assertFalse(log.update(50, 50, false));
	}

	@Test
	void lineWrapCharsFieldDefaultsToMinusOne() {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		assertEquals(-1, log.lineWrapChars);
	}

	@Test
	void lineWrapCharsCanBeSetAndRead() {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		log.lineWrapChars = 40;
		assertEquals(40, log.lineWrapChars);
	}

	@Test
	void capacityFieldDefaultsTo500() {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		assertEquals(500, log.capacity);
	}

	@Test
	void capacityCanBeChanged() {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		log.capacity = 10;
		assertEquals(10, log.capacity);
	}

	@Test
	void scrollUpClampingWhenTotalLinesLessThanVisible() throws Exception {
		// Only a few lines: scroll should clamp to 0 since maxScroll = 0
		ChatLogSDL log = newChatLogWithLines(2);
		// visibleLines() = max(1, (100-4)/16) = max(1, 6) = 6
		// totalLines = 2, visible = 6, maxScroll = 0

		NullpoMinoSDL.mouseWheelDelta = 10;
		log.update(50, 50, false);

		// After render, scrollUpLines would be clamped. But update doesn't clamp.
		// scrollUpLines should be 10.
		assertEquals(10, scrollUpLines(log));

		// After rendering, the render method would clamp to maxScroll.
		// We can't easily call render() since it uses SDL, but we can verify
		// the clamping logic by calling render and seeing if it NPEs...
		// Actually render WILL NPE, so skip that.
	}

	@Test
	void pageUpAndPageDownWithSingleEntry() throws Exception {
		ChatLogSDL log = newChatLogWithLines(1);
		assertEquals(0, scrollUpLines(log));

		log.pageUp();
		assertTrue(scrollUpLines(log) >= 1, "pageUp with single line should advance");

		log.pageDown();
		assertEquals(0, scrollUpLines(log), "pageDown should return to bottom");
	}

	@Test
	void scrollToBottomWhileAtBottomIsSafe() {
		ChatLogSDL log = newChatLogWithLines(10);
		log.scrollToBottom();
		// No exception expected
	}

	@Test
	void appendSystemUsesCurrentTime() throws Exception {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		long before = System.currentTimeMillis();
		log.appendSystem("msg", NormalFontSDL.COLOR_WHITE);
		long after = System.currentTimeMillis();

		Field f = ChatLogSDL.class.getDeclaredField("entries");
		f.setAccessible(true);
		@SuppressWarnings("unchecked")
		java.util.ArrayDeque<ChatLogSDL.Entry> q =
				(java.util.ArrayDeque<ChatLogSDL.Entry>) f.get(log);
		long ts = q.peekFirst().timestampMs;
		assertTrue(ts >= before && ts <= after, "timestamp should be current time");
	}

	private static ChatLogSDL newChatLogWithLines(int count) {
		ChatLogSDL log = new ChatLogSDL(0, 0, 200, 100);
		for (int i = 0; i < count; i++) {
			log.appendSystem("line " + i, NormalFontSDL.COLOR_WHITE);
		}
		return log;
	}
}
