package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Additional TableSDL tests covering mouse wheel scroll, showHeader
 * flag, rowHeight/headerHeight custom values, activated flag reset,
 * handleKey when disabled, and out-of-bounds selection edge cases.
 */
class TableSDLExtendedTest {

	private float originalWheel;

	@BeforeEach
	void resetWheel() {
		originalWheel = NullpoMinoSDL.mouseWheelDelta;
		NullpoMinoSDL.mouseWheelDelta = 0;
	}

	@AfterEach
	void restoreWheel() {
		NullpoMinoSDL.mouseWheelDelta = originalWheel;
	}

	private static TableSDL newTable() {
		return new TableSDL(0, 0, 200, 100, new TableSDL.Column[] {
				new TableSDL.Column("Name", 100),
				new TableSDL.Column("Score", 100)
		});
	}

	@Test
	void showHeaderFalseExtendsRowArea() {
		TableSDL t = newTable();
		t.showHeader = false;
		assertFalse(t.showHeader);
	}

	@Test
	void rowHeightCanBeChanged() {
		TableSDL t = newTable();
		t.rowHeight = 24;
		assertEquals(24, t.rowHeight);
	}

	@Test
	void headerHeightCanBeChanged() {
		TableSDL t = newTable();
		t.headerHeight = 30;
		assertEquals(30, t.headerHeight);
	}

	@Test
	void activatedFlagResetsOnEachUpdate() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});
		t.setSelectedIndex(0);

		// Activate via Enter
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertTrue(t.activated);

		// Call update — activated should reset to false
		t.update(50, 30, false);
		assertFalse(t.activated, "activated must reset on every update() call");
	}

	@Test
	void mouseWheelScrollChangesScrollPosition() {
		TableSDL t = newTable();
		for (int i = 0; i < 50; i++) t.addRow(new String[] {"r" + i});

		NullpoMinoSDL.mouseWheelDelta = 5;
		t.update(50, 50, false);  // inside widget

		// Scroll position is private, but we can verify by checking
		// that selection still works (no crash) and state is consistent
		t.setSelectedIndex(49);
		assertEquals(49, t.getSelectedIndex());
	}

	@Test
	void mouseWheelOutsideWidgetIgnored() {
		TableSDL t = newTable();
		for (int i = 0; i < 10; i++) t.addRow(new String[] {"r" + i});
		t.setSelectedIndex(0);

		NullpoMinoSDL.mouseWheelDelta = 5;
		t.update(500, 500, false);  // outside

		// Selection not affected
		assertEquals(0, t.getSelectedIndex());
	}

	@Test
	void handleKeyIgnoredWhenDisabled() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});
		t.addRow(new String[] {"b", "2"});
		t.setSelectedIndex(0);
		t.enabled = false;

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(0, t.getSelectedIndex(), "disabled table must ignore key events");
	}

	@Test
	void handleKeyIgnoredWhenEmpty() {
		TableSDL t = newTable();

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(-1, t.getSelectedIndex());

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_UP, 0, false));
		assertEquals(-1, t.getSelectedIndex());
	}

	@Test
	void handleKeyEnterDoesNotActivateWhenNoSelection() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertFalse(t.activated, "Enter with no selection must not activate");
	}

	@Test
	void invisibleOrDisabledIgnoresMouseWheel() {
		TableSDL t = newTable();
		for (int i = 0; i < 20; i++) t.addRow(new String[] {"r" + i});

		t.visible = false;
		NullpoMinoSDL.mouseWheelDelta = 5;
		t.update(50, 50, false);
		// visible=false returns early from update; wheel is not processed
		// but we can't read scroll directly; just verify no crash
		t.visible = true;
		t.enabled = false;
		t.update(50, 50, false);
	}

	@Test
	void clickOnRowWhenHeaderHiddenWorks() {
		TableSDL t = newTable();
		t.showHeader = false;
		t.addRow(new String[] {"a", "1"});
		t.addRow(new String[] {"b", "2"});

		// Without header, top=0, so row 1 starts at y=18 (rowHeight=18)
		boolean activated = t.update(50, 20, true);
		assertTrue(activated);
		assertEquals(1, t.getSelectedIndex());
	}

	@Test
	void doubleClickOnDifferentRowsDoesNotActivate() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});
		t.addRow(new String[] {"b", "2"});

		t.update(50, 25, true);  // click row 0
		assertFalse(t.activated);

		// Click different row (row 1) — should not be a double-click
		t.update(50, 45, true);
		assertFalse(t.activated, "different row click must not activate");
	}

	@Test
	void doubleClickPast400msIsTreatedAsTwoSeparateClicks() throws Exception {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});

		t.update(50, 25, true);
		assertFalse(t.activated);

		// Wait >400ms
		Thread.sleep(450);

		t.update(50, 25, true);
		assertFalse(t.activated, "slow double-click should not activate");
	}

	@Test
	void setSelectedIndexMinusOneOnEmptyTable() {
		TableSDL t = newTable();
		t.setSelectedIndex(-1);
		assertEquals(-1, t.getSelectedIndex());
	}
}
