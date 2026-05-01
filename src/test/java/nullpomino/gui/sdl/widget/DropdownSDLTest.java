package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Pins DropdownSDL's selection, list-open/close, and clamping
 * contracts. Lobby dialogs use this widget for fixed-list pickers
 * (rule, randomiser, AI, etc.). The selection-clamp behaviour and
 * the close-on-outside-click rule are both load-bearing UX rules.
 */
class DropdownSDLTest {

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

	@Test
	void freshDropdownHasNoSelection() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20);

		assertEquals(-1, d.getSelectedIndex());
		assertEquals("", d.getSelectedItem(),
				"empty list returns sentinel empty string");
	}

	@Test
	void setItemsArrayInitialisesSelectionToZero() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20);

		d.setItems(new String[] {"a", "b", "c"});
		assertEquals(0, d.getSelectedIndex(),
				"first item selected when no prior selection");
		assertEquals("a", d.getSelectedItem());
	}

	@Test
	void setItemsListAcceptsEmptyAndKeepsSelectionMinusOne() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20);

		d.setItems(java.util.Arrays.asList());
		assertEquals(-1, d.getSelectedIndex());
	}

	@Test
	void setItemsClampsSelectionWhenListShrinks() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a", "b", "c", "d"});
		d.setSelectedIndex(3);

		d.setItems(new String[] {"a", "b"});
		assertEquals(1, d.getSelectedIndex(),
				"selection clamps to last item on shrink");
	}

	@Test
	void setSelectedIndexClampsToValidRange() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a", "b", "c"});

		d.setSelectedIndex(99);
		assertEquals(2, d.getSelectedIndex());

		d.setSelectedIndex(-1);
		assertEquals(-1, d.getSelectedIndex());
	}

	@Test
	void getSelectedItemHandlesOutOfRangeIndex() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a"});

		d.setSelectedIndex(-1);
		assertEquals("", d.getSelectedItem());
	}

	@Test
	void clickOnHeaderOpensTheDropdown() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a", "b"});

		assertTrue(d.update(50, 10, true), "click in header returns activated");
	}

	@Test
	void clickOnDropListItemSelectsItAndClosesList() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a", "b", "c"});

		d.update(50, 10, true);
		// drop area starts at y=20 (y + h). itemH = 20. So item index 2 ranges 60..80
		boolean activated = d.update(50, 65, true);

		assertTrue(activated);
		assertEquals(2, d.getSelectedIndex());
	}

	@Test
	void clickOutsideDropListClosesItWithoutSelecting() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a", "b"});
		int beforeSel = d.getSelectedIndex();

		d.update(50, 10, true);   // open
		d.update(500, 500, true); // click outside

		assertEquals(beforeSel, d.getSelectedIndex(),
				"selection unchanged on outside-click close");

		// Confirm closed by pressing the same outside spot — no further activation.
		assertFalse(d.update(500, 500, true));
	}

	@Test
	void handleKeyDownAndUpAdjustSelection() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a", "b", "c"});

		d.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(1, d.getSelectedIndex());
		d.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(2, d.getSelectedIndex());
		// Already at last → stays
		d.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(2, d.getSelectedIndex());

		d.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_UP, 0, false));
		assertEquals(1, d.getSelectedIndex());
	}

	@Test
	void handleKeyEnterTogglesOpenAndEscapeCloses() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a", "b"});

		// We can't read open directly; check via update behaviour.
		d.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_RETURN, 0, false));
		// Now open: clicking outside closes
		d.update(500, 500, true);
		// State should now be closed; clicking outside again returns false
		assertFalse(d.update(500, 500, true));

		// Open again with SPACE, then ESCAPE should close
		d.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_SPACE, 0, false));
		d.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_ESCAPE, 0, false));
		assertFalse(d.update(500, 500, true), "ESCAPE should have closed");
	}

	@Test
	void handleKeyIgnoredWhenItemListEmpty() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20);

		// Not even a NPE on UP/DOWN with empty list
		d.handleKey(new NullpoMinoSDL.KeyEvent(SDLConstants.SDL_SCANCODE_DOWN, 0, false));
		assertEquals(-1, d.getSelectedIndex());
	}

	@Test
	void closeMethodForceClosesTheDropdown() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a", "b"});
		d.update(50, 10, true);  // open

		d.close();

		// Closed: click outside no longer triggers the closing branch
		assertFalse(d.update(500, 500, true));
	}

	@Test
	void invisibleOrDisabledIgnoresUpdate() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"a", "b"});
		d.visible = false;

		assertFalse(d.update(50, 10, true));

		d.visible = true;
		d.enabled = false;
		assertFalse(d.update(50, 10, true));
	}

	@Test
	void setItemsWithListOverloadStoresEntries() {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20);

		d.setItems(Arrays.asList("x", "y"));
		assertEquals(0, d.getSelectedIndex());
		assertEquals("x", d.getSelectedItem());
	}
}
