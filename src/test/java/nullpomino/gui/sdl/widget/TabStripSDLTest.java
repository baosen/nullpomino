package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins TabStripSDL's hit-testing and active-tab clamping. The
 * computed per-tab pixel widths are private but observable through
 * the click handler — clicking inside a tab's segment switches the
 * active index.
 */
class TabStripSDLTest {

	@Test
	void constructorAcceptsLabelsAndDefaultsActiveToZero() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"One", "Two", "Three"});

		assertEquals(0, strip.getActiveTab());
	}

	@Test
	void constructorTreatsNullLabelsAsEmptyStrip() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24, null);

		assertEquals(0, strip.getActiveTab());
		assertFalse(strip.update(10, 10, true),
				"empty strip ignores clicks");
	}

	@Test
	void setActiveTabClampsBelowZeroToZero() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B"});

		strip.setActiveTab(-5);
		assertEquals(0, strip.getActiveTab());
	}

	@Test
	void setActiveTabClampsBeyondLastToLast() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B"});

		strip.setActiveTab(99);
		assertEquals(1, strip.getActiveTab());
	}

	@Test
	void setActiveTabIsNoOpForEmptyStrip() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24, new String[0]);

		strip.setActiveTab(5);
		assertEquals(0, strip.getActiveTab());
	}

	@Test
	void clickOnDifferentTabSwitchesActiveAndReturnsTrue() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B"});

		// First tab spans some left portion; clicking near right edge should
		// land on the second tab on a 300-wide strip.
		boolean activated = strip.update(290, 12, true);

		assertTrue(activated);
		assertEquals(1, strip.getActiveTab());
	}

	@Test
	void clickOnAlreadyActiveTabReturnsFalse() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24, new String[] {"A", "B"});

		// Click within tab 0 first to ensure layout is exercised
		assertFalse(strip.update(5, 12, true), "first tab already active");
	}

	@Test
	void clickOutsideStripVerticallyIsIgnored() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B"});

		assertFalse(strip.update(290, 100, true));
		assertFalse(strip.update(290, -10, true));
	}

	@Test
	void hoverWithoutPressDoesNotSwitchTab() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B"});

		assertFalse(strip.update(290, 12, false));
		assertEquals(0, strip.getActiveTab());
	}

	@Test
	void invisibleOrDisabledStripIgnoresClicks() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B"});
		strip.visible = false;

		assertFalse(strip.update(290, 12, true));

		strip.visible = true;
		strip.enabled = false;
		assertFalse(strip.update(290, 12, true));
	}
}
