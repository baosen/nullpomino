package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.IntegerMenuItem;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gap in {@link GameMode#getMenuItemForRow}
 * (L257): the ternary between a known positive item count and the
 * 20-item fallback when the count is unknown.
 */
class GameModeBranchGapTest {

	private static final class RowStubMode extends AbstractMode {
	}

	@Test
	void knownItemCountBoundsTheRowMapping() {
		RowStubMode mode = new RowStubMode();
		mode.addMenuItems(
				new IntegerMenuItem("a", "A", EventReceiver.COLOR_WHITE, 0, 0, 9),
				new IntegerMenuItem("b", "B", EventReceiver.COLOR_WHITE, 0, 0, 9));

		assertEquals(0, mode.getMenuItemForRow(0));
		assertEquals(0, mode.getMenuItemForRow(1));
		assertEquals(1, mode.getMenuItemForRow(3));
		assertEquals(-1, mode.getMenuItemForRow(4), "row past the 2-item menu maps to no item");
	}

	@Test
	void unknownItemCountFallsBackToTwentyItems() {
		RowStubMode mode = new RowStubMode(); // empty menu -> getMenuItemCount() == -1

		assertEquals(-1, mode.getMenuItemForRow(-1));
		assertEquals(2, mode.getMenuItemForRow(5));
		assertEquals(19, mode.getMenuItemForRow(39));
		assertEquals(-1, mode.getMenuItemForRow(40), "fallback caps at item 19");
	}
}
