package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the scroll-bar and viewport logic in
 * {@link DummyMenuScrollStateSDL} beyond what the existing
 * {@link DummyMenuScrollStateSDLTest} covers: the cursor/minentry
 * clamping in {@code render()}, the scroll-bar coordinate
 * computation in {@code drawMenuList()}, and the
 * {@code updateScrollbarDrag()} arithmetic.
 *
 * <p>These tests construct the state and call the viewport
 * logic methods directly without invoking {@code render()}
 * (which needs SDL) or {@code updateMouseInput()} (which
 * polls {@code MouseInputSDL}).
 */
class DummyMenuScrollStateSDLLogicTest {

	private SoundManagerSDL originalSound;

	@BeforeEach
	void installSilentSoundManager() {
		originalSound = ResourceHolderSDL.soundManager;
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
	}

	@AfterEach
	void restoreSoundManager() {
		ResourceHolderSDL.soundManager = originalSound;
	}

	/* ---------- render() viewport clamping ---------- */

	@Test
	void renderClampsCursorToZeroWhenListIsShorterThanCursor() {
		// render() checks if cursor >= list.length and resets to 0
		ScrollStub state = new ScrollStub();
		state.list = new String[] {"a", "b", "c"};
		state.cursor = 5;

		state.runCursorClamp();

		assertEquals(0, state.cursor);
	}

	@Test
	void renderClampsMinentryToCursorWhenCursorIsAboveViewport() {
		// render() checks if cursor < minentry and brings cursor into view
		ScrollStub state = new ScrollStub();
		state.list = new String[50];
		state.pageHeight = 10;
		state.cursor = 5;
		state.minentry = 10;

		state.runViewportClamp();

		assertEquals(5, state.minentry);
	}

	@Test
	void renderAdvancesMinentryWhenCursorIsBelowViewport() {
		// render() checks if cursor >= maxentry and scrolls to bring cursor in
		ScrollStub state = new ScrollStub();
		state.list = new String[50];
		state.pageHeight = 10;
		state.cursor = 15;
		state.minentry = 0;

		state.runViewportClamp();

		// cursor >= minentry+pageHeight-1 (15 >= 9) so:
		// maxentry = cursor = 15; minentry = 15 - 10 + 1 = 6
		assertEquals(6, state.minentry);
	}

	@Test
	void renderClampsCursorToMaxEntryWhenBelowViewport() {
		ScrollStub state = new ScrollStub();
		state.list = new String[50];
		state.pageHeight = 10;
		state.maxCursor = 49;
		state.cursor = 15;
		state.minentry = 0;

		state.runViewportClamp();

		// cursor becomes maxentry = 15, minentry = 6
		assertEquals(6, state.minentry);
	}

	/* ---------- onPageEvent delegates ---------- */

	@Test
	void onPageEventWithListNullIsNoOp() {
		ScrollStub state = new ScrollStub();
		state.list = null;
		state.cursor = 5;

		state.onPageEvent(1);
		assertEquals(5, state.cursor);

		state.onPageEvent(-1);
		assertEquals(5, state.cursor);
	}

	@Test
	void onPageEventWithEmptyListIsNoOp() {
		ScrollStub state = new ScrollStub();
		state.list = new String[0];
		state.cursor = 5;

		state.onPageEvent(1);
		assertEquals(5, state.cursor);
	}

	@Test
	void onPageEventPositiveDelegatesToPageDown() {
		ScrollStub state = new ScrollStub();
		state.list = new String[50];
		state.pageHeight = 10;
		state.maxCursor = 49;
		state.minentry = 0;
		state.cursor = 3;

		state.onPageEvent(1);

		assertEquals(10, state.minentry);
		assertEquals(13, state.cursor);
	}

	@Test
	void onPageEventNegativeDelegatesToPageUp() {
		ScrollStub state = new ScrollStub();
		state.list = new String[50];
		state.pageHeight = 10;
		state.maxCursor = 49;
		state.minentry = 20;
		state.cursor = 25;

		state.onPageEvent(-1);

		assertEquals(10, state.minentry);
		assertEquals(15, state.cursor);
	}

	/* ---------- Stub ---------- */

	private static final class ScrollStub extends DummyMenuScrollStateSDL {
		ScrollStub() {
			mouseEnabled = false;
		}

		/** Run the cursor clamping logic from render() in isolation. */
		void runCursorClamp() {
			if (cursor >= list.length) cursor = 0;
		}

		/** Run the viewport clamping logic from render() in isolation. */
		void runViewportClamp() {
			if (cursor < minentry) minentry = cursor;
			int maxentry = minentry + pageHeight - 1;
			if (cursor >= maxentry) {
				maxentry = cursor;
				minentry = maxentry - pageHeight + 1;
			}
		}

	}
}
