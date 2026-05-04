package nullpomino.game.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

/**
 * Covers the remaining branch in BooleanMenuItem.loadValue
 * where the property key is missing and the DEFAULT_VALUE is returned.
 */
class BooleanMenuItemBranchTest {

	@Test
	void loadValueReturnsDefaultWhenKeyMissing() {
		BooleanMenuItem item = new BooleanMenuItem("big", "BIG",
				0, true);
		CustomProperties props = new CustomProperties();

		item.loadValue(props, "nonexistent.key");

		assertEquals(true, item.value);
	}

	@Test
	void changeTogglesValue() {
		BooleanMenuItem item = new BooleanMenuItem("big", "BIG",
				0, false);

		assertFalse(item.value);
		item.change(1, 0);
		assertEquals(true, item.value);
		item.change(-1, 0);
		assertFalse(item.value);
	}
}
