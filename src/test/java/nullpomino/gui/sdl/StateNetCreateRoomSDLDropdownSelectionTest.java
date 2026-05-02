package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.gui.sdl.widget.DropdownSDL;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link StateNetCreateRoomSDL}'s private static
 * {@code setDropdownSelection} helper. It walks the dropdown by
 * incrementing the selected index until either the item matches or
 * the dropdown clamps (when setSelectedIndex doesn't advance, the
 * walker has gone past the last entry).
 *
 * <p>The fallback chain:
 * <ul>
 *   <li>null / empty item: select index 0.</li>
 *   <li>match found: select the matching index.</li>
 *   <li>no match found: restore the saved index (or 0 if there was
 *       no prior selection).</li>
 * </ul>
 */
class StateNetCreateRoomSDLDropdownSelectionTest {

	@Test
	void nullItemSetsSelectionToZero() throws Exception {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"alpha", "beta", "gamma"});
		d.setSelectedIndex(2);

		invokeSetDropdownSelection(d, null);

		assertEquals(0, d.getSelectedIndex(),
				"null item -> select index 0");
	}

	@Test
	void emptyItemSetsSelectionToZero() throws Exception {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"alpha", "beta", "gamma"});
		d.setSelectedIndex(2);

		invokeSetDropdownSelection(d, "");

		assertEquals(0, d.getSelectedIndex(),
				"empty item -> select index 0");
	}

	@Test
	void matchingItemSelectsThatIndex() throws Exception {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"alpha", "beta", "gamma"});

		invokeSetDropdownSelection(d, "beta");
		assertEquals(1, d.getSelectedIndex());

		invokeSetDropdownSelection(d, "gamma");
		assertEquals(2, d.getSelectedIndex());

		invokeSetDropdownSelection(d, "alpha");
		assertEquals(0, d.getSelectedIndex());
	}

	@Test
	void noMatchRestoresSavedIndex() throws Exception {
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20,
				new String[] {"alpha", "beta", "gamma"});
		d.setSelectedIndex(1);

		invokeSetDropdownSelection(d, "nope");

		assertEquals(1, d.getSelectedIndex(),
				"no match -> restore the saved selection");
	}

	@Test
	void noMatchWithUnselectedDropdownFallsBackToZero() throws Exception {
		// A fresh dropdown with empty item list has selected = -1.
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20);
		// selected == -1 (no items, no selection).
		d.setItems(new String[] {"alpha", "beta"});
		// setItems(non-empty) initialises selection to 0; reset to -1
		// to exercise the saved < 0 fallback branch.
		setSelectedToMinusOne(d);

		invokeSetDropdownSelection(d, "nope");

		assertEquals(0, d.getSelectedIndex(),
				"no match + saved < 0 -> fall back to 0");
	}

	@Test
	void emptyDropdownWithSelectedZeroFallsBackToZero() throws Exception {
		// Empty items list — setSelectedIndex(0) clamps so no walking
		// loop iteration finds a match. The saved=-1 fallback fires.
		DropdownSDL d = new DropdownSDL(0, 0, 100, 20);

		invokeSetDropdownSelection(d, "looking-for-anything");

		// On an empty dropdown, getSelectedIndex returns -1 because
		// setSelectedIndex(0) clamps to no-items.
		assertEquals(-1, d.getSelectedIndex(),
				"empty dropdown can't be selected; saved=-1 fallback "
						+ "leaves selection at -1");
	}

	private static void invokeSetDropdownSelection(DropdownSDL d, String item)
			throws Exception {
		Method m = StateNetCreateRoomSDL.class.getDeclaredMethod(
				"setDropdownSelection", DropdownSDL.class, String.class);
		m.setAccessible(true);
		m.invoke(null, d, item);
	}

	private static void setSelectedToMinusOne(DropdownSDL d) throws Exception {
		java.lang.reflect.Field f = DropdownSDL.class.getDeclaredField("selected");
		f.setAccessible(true);
		f.setInt(d, -1);
	}
}
