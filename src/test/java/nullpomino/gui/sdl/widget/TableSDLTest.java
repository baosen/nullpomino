package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Pins TableSDL's row-selection and keyboard-navigation contracts.
 * Lobby room and replay lists rely on the selection model and the
 * activate-on-Enter / activate-on-double-click behaviour. Render
 * is SDL-bound and skipped here.
 */
class TableSDLTest {

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
	void constructorWithNullColumnsArrayIsTreatedAsEmpty() {
		TableSDL t = new TableSDL(0, 0, 200, 100, null);

		assertEquals(0, t.columns.length);
		assertEquals(0, t.getRowCount());
	}

	@Test
	void addRowAppendsRowsAndIncrementsCount() {
		TableSDL t = newTable();

		t.addRow(new String[] {"a", "1"});
		t.addRow(new String[] {"b", "2"});

		assertEquals(2, t.getRowCount());
		assertArrayEquals(new String[] {"a", "1"}, t.getRow(0));
	}

	@Test
	void addRowWithFontColorOverrideIsAccepted() {
		TableSDL t = newTable();

		// Just exercises the override path; the colour is observed by render().
		t.addRow(new String[] {"r", "9"}, /* COLOR_RED */ 2);

		assertEquals(1, t.getRowCount());
	}

	@Test
	void getRowReturnsNullForOutOfRangeIndex() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});

		assertNull(t.getRow(-1));
		assertNull(t.getRow(99));
	}

	@Test
	void clearRemovesRowsAndResetsSelection() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});
		t.addRow(new String[] {"b", "2"});
		t.setSelectedIndex(1);

		t.clear();

		assertEquals(0, t.getRowCount());
		assertEquals(-1, t.getSelectedIndex());
	}

	@Test
	void setSelectedIndexClampsToMinusOneForOutOfRange() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});

		t.setSelectedIndex(99);
		assertEquals(-1, t.getSelectedIndex(),
				"out-of-range falls back to no selection");

		t.setSelectedIndex(0);
		assertEquals(0, t.getSelectedIndex());
	}

	@Test
	void setRowReplacesExistingRow() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});

		t.setRow(0, new String[] {"z", "9"});
		assertArrayEquals(new String[] {"z", "9"}, t.getRow(0));
	}

	@Test
	void setRowOutOfRangeIsNoOp() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});

		t.setRow(99, new String[] {"z", "9"});
		assertArrayEquals(new String[] {"a", "1"}, t.getRow(0));
	}

	@Test
	void clickInRowAreaSelectsTheRow() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});
		t.addRow(new String[] {"b", "2"});

		// Header is 20px high. Row 0 starts at y=20, row 1 at y=38 (rowHeight=18)
		boolean activated = t.update(50, 30, true);

		assertTrue(activated);
		assertEquals(0, t.getSelectedIndex());
	}

	@Test
	void clickInHeaderAreaIsIgnored() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});

		assertFalse(t.update(50, 5, true), "click in header area should not select");
	}

	@Test
	void doubleClickActivatesRow() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});

		t.update(50, 30, true);
		assertFalse(t.activated, "first click only selects");

		// Second click on same row within 400ms → activated
		t.update(50, 30, true);
		assertTrue(t.activated, "second quick click on same row activates");
	}

	@Test
	void handleKeyUpAndDownAdjustSelectionWithinBounds() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a"});
		t.addRow(new String[] {"b"});
		t.addRow(new String[] {"c"});
		t.setSelectedIndex(0);

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(1, t.getSelectedIndex());
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(2, t.getSelectedIndex());
		// at end → stays
		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(2, t.getSelectedIndex());

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_UP, 0, false));
		assertEquals(1, t.getSelectedIndex());
	}

	@Test
	void handleKeyHomeAndEndJumpToBoundsOfList() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a"});
		t.addRow(new String[] {"b"});
		t.addRow(new String[] {"c"});
		t.setSelectedIndex(1);

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_HOME, 0, false));
		assertEquals(0, t.getSelectedIndex());

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_END, 0, false));
		assertEquals(2, t.getSelectedIndex());
	}

	@Test
	void handleKeyPageDownAdvancesByVisibleRowCount() {
		TableSDL t = newTable();
		for (int i = 0; i < 20; i++) t.addRow(new String[] {"r" + i});
		t.setSelectedIndex(0);

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_PAGEDOWN, 0, false));
		// visibleRowCount is (100-20)/18 = 4. So selected goes 0 → 4.
		assertEquals(4, t.getSelectedIndex());

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_PAGEUP, 0, false));
		assertEquals(0, t.getSelectedIndex());
	}

	@Test
	void handleKeyEnterActivatesWhenRowSelected() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a"});
		t.setSelectedIndex(0);

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		assertTrue(t.activated);
	}

	@Test
	void handleKeyIgnoredForEmptyTable() {
		TableSDL t = newTable();

		t.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(-1, t.getSelectedIndex());
	}

	@Test
	void invisibleOrDisabledIgnoresClicks() {
		TableSDL t = newTable();
		t.addRow(new String[] {"a", "1"});
		t.visible = false;
		assertFalse(t.update(50, 30, true));

		t.visible = true;
		t.enabled = false;
		assertFalse(t.update(50, 30, true));
	}
}
