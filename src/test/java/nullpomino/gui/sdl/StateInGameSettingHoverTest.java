package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.mode.GameMode;
import nullpomino.game.mode.MarathonMode;
import nullpomino.game.mode.PracticeMode;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link StateInGameSDL#settingHoverItem}, the pixel->item math that
 * moves the pre-game SETTING cursor to the row under the mouse. The
 * geometry has to mirror {@code RendererSDL.drawMenuFont} exactly, and
 * that math differs by mode:
 *
 * <ul>
 *   <li>The common {@link MarathonMode}-style screen draws with the field
 *       offset applied (+52 normally, +4 in small-display) and packs each
 *       option into two rows (label + value), 16 px each.</li>
 *   <li>{@link PracticeMode} sets {@code owner.menuOnly}, so its options
 *       are drawn at raw {@code row * 16} with no field offset, one option
 *       per row, starting at row 3, across two pages of 23. This is the
 *       case the reported "cursor sits below the option it highlights" bug
 *       lived in — the old handler assumed the +52 offset and two-row
 *       spacing for every mode.</li>
 * </ul>
 */
class StateInGameSettingHoverTest {

	// ---- Standard (non-menuOnly) two-row layout, e.g. MarathonMode ----

	@Test
	void standardMenuMapsFieldOffsetAndTwoRowsPerItem() {
		GameMode mode = new MarathonMode();
		int offsetY = 80;
		int baseY = offsetY + 52; // drawMenuFont row 0

		// Both rows of item 0 (label at baseY, value at baseY+16) select 0.
		assertEquals(0, StateInGameSDL.settingHoverItem(mode, baseY, offsetY, false, false));
		assertEquals(0, StateInGameSDL.settingHoverItem(mode, baseY + 15, offsetY, false, false));
		// Item 1 occupies the next two rows.
		assertEquals(1, StateInGameSDL.settingHoverItem(mode, baseY + 32, offsetY, false, false));
		// Above the first row selects nothing.
		assertEquals(-1, StateInGameSDL.settingHoverItem(mode, baseY - 1, offsetY, false, false));
	}

	@Test
	void standardMenuSmallDisplayUsesFourPixelOffset() {
		GameMode mode = new MarathonMode();
		int offsetY = 80;
		int baseY = offsetY + 4; // small-display drawMenuFont row 0
		assertEquals(0, StateInGameSDL.settingHoverItem(mode, baseY, offsetY, false, true));
		assertEquals(1, StateInGameSDL.settingHoverItem(mode, baseY + 32, offsetY, false, true));
	}

	// ---- PracticeMode: menuOnly, single row per item, row 3, paged ----

	@Test
	void practiceMenuIgnoresFieldOffsetUnderMenuOnly() {
		PracticeMode mode = new PracticeMode();
		mode.setMenuCursor(0); // page 0

		// GRAVITY is drawn at row 3 -> pixel y 48, regardless of offsetY,
		// because menuOnly drops the field offset. The pre-fix handler would
		// have added offsetY + 52 and divided by 32, landing several rows off.
		assertEquals(0, StateInGameSDL.settingHoverItem(mode, 48, 999, true, false));
		assertEquals(0, StateInGameSDL.settingHoverItem(mode, 63, 999, true, false));
		// G-MAX at row 4 -> pixel y 64.
		assertEquals(1, StateInGameSDL.settingHoverItem(mode, 64, 999, true, false));
		// Last option on page 0 (TIME LIMIT RESET, item 22) at row 25 -> 400.
		assertEquals(22, StateInGameSDL.settingHoverItem(mode, 25 * 16, 999, true, false));
		// The title rows (0-2) and anything below the page select nothing.
		assertEquals(-1, StateInGameSDL.settingHoverItem(mode, 2 * 16, 999, true, false));
		assertEquals(-1, StateInGameSDL.settingHoverItem(mode, 26 * 16, 999, true, false));
	}

	@Test
	void practiceMenuHoverStaysOnCurrentPage() {
		PracticeMode mode = new PracticeMode();
		mode.setMenuCursor(30); // page 1 (items 23-45)

		// Row 3 now maps to the first option of page 1 (USE BONE BLOCKS = 23),
		// not page 0's GRAVITY, so a hover never silently flips pages.
		assertEquals(23, StateInGameSDL.settingHoverItem(mode, 3 * 16, 0, true, false));
		assertEquals(45, StateInGameSDL.settingHoverItem(mode, 25 * 16, 0, true, false));
	}

	@Test
	void practiceGetMenuItemForRowRejectsOutOfRangeRows() {
		PracticeMode mode = new PracticeMode();
		mode.setMenuCursor(0);
		assertEquals(-1, mode.getMenuItemForRow(2));  // title area
		assertEquals(0, mode.getMenuItemForRow(3));   // first option
		assertEquals(22, mode.getMenuItemForRow(25)); // last option on page
		assertEquals(-1, mode.getMenuItemForRow(26)); // past the page
	}
}
