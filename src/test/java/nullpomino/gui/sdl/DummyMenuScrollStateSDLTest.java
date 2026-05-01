package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Pins the page-jump arithmetic in {@link DummyMenuScrollStateSDL}. The
 * scroll-bar variant differs from the choose-state default ({@link
 * DummyMenuChooseStateSDL#onPageEvent}) — instead of jumping to the very
 * start or end of the list, page up / page down slide one page at a
 * time and clamp at the edges. Every concrete scrolling menu (replay
 * select, mode select, rule select, …) inherits this so a regression
 * here would silently break paging across the SDL frontend.
 */
class DummyMenuScrollStateSDLTest {

	private SoundManagerSDL originalSound;

	@BeforeEach
	void installSilentSoundManager() {
		originalSound = ResourceHolderSDL.soundManager;
		// SoundManagerSDL with mixerLib=null — play("cursor") returns early.
		ResourceHolderSDL.soundManager = new SoundManagerSDL();
	}

	@AfterEach
	void restoreSoundManager() {
		ResourceHolderSDL.soundManager = originalSound;
	}

	@Test
	void defaultConstructorInstallsZeroedScrollBarState() {
		ScrollStub state = new ScrollStub();

		assertEquals(0, state.minentry);
		assertEquals("", state.nullError);
		assertEquals("", state.emptyError);
		assertEquals(0, state.pUpMinY);
		assertEquals(0, state.pUpMaxY);
		assertEquals(0, state.pDownMinY);
		assertEquals(0, state.pDownMaxY);
		assertEquals(0, state.sbUsableTravel);
		assertFalse(state.sbDragging);
		assertEquals(0, state.sbDragOffset);
	}

	@Test
	void pageDownAtBottomAnchorJumpsCursorToMaxWithoutAdvancingMinentry() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 19;
		state.pageHeight = 10;
		state.minentry = 10; // already at the max anchor (19 - 10 + 1 = 10)
		state.cursor = 12;

		state.pageDown();

		assertEquals(19, state.cursor, "pageDown at bottom anchor must drop cursor on maxCursor");
		assertEquals(10, state.minentry, "minentry must not advance past the bottom anchor");
	}

	@Test
	void pageDownAdvancesByPageHeightWhenRoomRemains() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.minentry = 0;
		state.cursor = 5;

		state.pageDown();

		assertEquals(15, state.cursor);
		assertEquals(10, state.minentry);
	}

	@Test
	void pageDownClampsMinentryAndAdjustsCursorWhenSpillingPastBottomAnchor() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 19;
		state.pageHeight = 10;
		state.minentry = 5; // spilling past max=10 after one page advance
		state.cursor = 7;

		state.pageDown();

		// Raw advance: minentry=15, cursor=17. Clamp minentry to 10
		// (overshoot 5), adjust cursor by the same overshoot.
		assertEquals(10, state.minentry);
		assertEquals(12, state.cursor);
	}

	@Test
	void pageUpAtTopJumpsCursorToZeroWithoutWrapping() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 19;
		state.pageHeight = 10;
		state.minentry = 0;
		state.cursor = 7;

		state.pageUp();

		assertEquals(0, state.cursor);
		assertEquals(0, state.minentry);
	}

	@Test
	void pageUpRewindsByPageHeightWhenRoomRemains() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.minentry = 30;
		state.cursor = 35;

		state.pageUp();

		assertEquals(25, state.cursor);
		assertEquals(20, state.minentry);
	}

	@Test
	void pageUpClampsMinentryToZeroAndAdjustsCursorWhenSpillingPastTop() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.minentry = 5; // spilling past 0 after one page rewind
		state.cursor = 6;

		state.pageUp();

		// Raw rewind: minentry=-5, cursor=-4. Clamp minentry to 0
		// (overshoot 5), adjust cursor by the same overshoot back up.
		assertEquals(0, state.minentry);
		assertEquals(1, state.cursor);
	}

	@Test
	void onChangePositiveDelegatesToPageDown() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;

		state.onChange(1);

		assertEquals(10, state.minentry);
		assertEquals(10, state.cursor);
	}

	@Test
	void onChangeNegativeDelegatesToPageUp() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.minentry = 20;
		state.cursor = 25;

		state.onChange(-1);

		assertEquals(10, state.minentry);
		assertEquals(15, state.cursor);
	}

	@Test
	void onChangeZeroIsNoOp() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.minentry = 5;
		state.cursor = 7;

		state.onChange(0);

		assertEquals(5, state.minentry);
		assertEquals(7, state.cursor);
	}

	@Test
	void onPageEventIsNoOpWhenListIsNull() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.list = null;
		state.cursor = 7;

		state.onPageEvent(1);
		state.onPageEvent(-1);

		assertEquals(7, state.cursor);
	}

	@Test
	void onPageEventIsNoOpForEmptyList() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.list = new String[0];
		state.cursor = 7;

		state.onPageEvent(1);
		state.onPageEvent(-1);

		assertEquals(7, state.cursor);
	}

	@Test
	void onPageEventForwardsPositiveDirectionToPageDown() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.list = new String[50];

		state.onPageEvent(1);

		assertEquals(10, state.minentry);
		assertEquals(10, state.cursor);
	}

	@Test
	void onPageEventForwardsNegativeDirectionToPageUp() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.list = new String[50];
		state.minentry = 20;
		state.cursor = 25;

		state.onPageEvent(-1);

		assertEquals(10, state.minentry);
		assertEquals(15, state.cursor);
	}

	@Test
	void onPageEventIgnoresUnknownDirections() {
		ScrollStub state = new ScrollStub();
		state.maxCursor = 49;
		state.pageHeight = 10;
		state.list = new String[50];
		state.cursor = 7;

		state.onPageEvent(0);
		state.onPageEvent(2);

		assertEquals(7, state.cursor);
	}

	@Test
	void consumeJustEnteredIsExposedFromTheChooseStateBase() {
		// Inherited from DummyMenuChooseStateSDL — pin that the scroll
		// variant doesn't accidentally hide it.
		ScrollStub state = new ScrollStub();
		state.justEntered = true;

		assertTrue(state.consumeJustEntered());
		assertFalse(state.justEntered);
	}

	private static final class ScrollStub extends DummyMenuScrollStateSDL {
		ScrollStub() {
			mouseEnabled = false;
		}
	}
}
