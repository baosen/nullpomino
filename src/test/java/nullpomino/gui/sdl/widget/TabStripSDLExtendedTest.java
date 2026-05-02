package nullpomino.gui.sdl.widget;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.gui.sdl.NullpoMinoSDL;
import nullpomino.gui.sdl.binding.SDLConstants;

/**
 * Additional TabStripSDL tests covering single tab, many tabs,
 * overflow label widths, setActiveTab with valid index, and
 * enabled/visible guards.
 */
class TabStripSDLExtendedTest {

	@Test
	void singleTabAlwaysActive() {
		TabStripSDL strip = new TabStripSDL(0, 0, 200, 24,
				new String[] {"Only"});
		assertEquals(0, strip.getActiveTab());

		// Click anywhere on the strip should still have tab 0 active
		// (no switch since only one tab)
		assertFalse(strip.update(100, 12, true));
		assertEquals(0, strip.getActiveTab());
	}

	@Test
	void manyTabsAreAllClickable() {
		String[] labels = new String[20];
		for (int i = 0; i < 20; i++) labels[i] = "T" + i;
		TabStripSDL strip = new TabStripSDL(0, 0, 800, 24, labels);

		// Click on the last tab
		boolean activated = strip.update(790, 12, true);
		assertTrue(activated);
		assertEquals(19, strip.getActiveTab());
	}

	@Test
	void tabWidthsComputeCorrectlyForOverflow() {
		// Very narrow strip with long labels -> overflow mode
		TabStripSDL strip = new TabStripSDL(0, 0, 50, 24,
				new String[] {"VeryLongLabel", "AnotherLongOne", "Short"});

		// Should not crash, layout divides proportionally
		strip.setActiveTab(2);
		assertEquals(2, strip.getActiveTab());
	}

	@Test
	void setActiveTabWithValidIndexWorks() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B", "C"});

		strip.setActiveTab(1);
		assertEquals(1, strip.getActiveTab());

		strip.setActiveTab(0);
		assertEquals(0, strip.getActiveTab());

		strip.setActiveTab(2);
		assertEquals(2, strip.getActiveTab());
	}

	@Test
	void clickOnTabSwitchesBackAndForth() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"Left", "Right"});

		// Click on right tab
		strip.update(290, 12, true);
		assertEquals(1, strip.getActiveTab());

		// Click on left tab
		strip.update(10, 12, true);
		assertEquals(0, strip.getActiveTab());
	}

	@Test
	void disabledStripIgnoresClicksForAllTabs() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B", "C"});
		strip.enabled = false;

		assertFalse(strip.update(290, 12, true));
		assertEquals(0, strip.getActiveTab());
	}

	@Test
	void invisibleStripIgnoresClicksForAllTabs() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B", "C"});
		strip.visible = false;

		assertFalse(strip.update(290, 12, true));
		assertEquals(0, strip.getActiveTab());
	}

	@Test
	void emptyLabelsArrayClampsSetActiveTabToZero() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24, new String[0]);

		strip.setActiveTab(5);
		assertEquals(0, strip.getActiveTab());

		assertFalse(strip.update(50, 12, true), "empty strip ignores clicks");
	}

	@Test
	void clickOnTabEdgeActivatesCorrectTab() {
		TabStripSDL strip = new TabStripSDL(0, 0, 300, 24,
				new String[] {"A", "B"});

		// Click at x=149 (middle of strip) - should hit tab 0 if layout puts
		// each tab at ~150px each. Tab A has min width 16*1+4=20. Slack = 300-40=260.
		// perTab=130, leftover=0. Tab 0: 20+130=150 wide. Tab 1: 20+130=150 wide.
		// So x=149 hits tab 0, x=150 hits tab 1.
		strip.update(149, 12, true);
		assertEquals(0, strip.getActiveTab());

		strip.update(150, 12, true);
		assertEquals(1, strip.getActiveTab());
	}
}
