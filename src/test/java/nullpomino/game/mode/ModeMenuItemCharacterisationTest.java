package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.menu.AbstractMenuItem;
import nullpomino.game.menu.IntegerMenuItem;

import org.junit.jupiter.api.Test;

class ModeMenuItemCharacterisationTest {

	@Test
	void maniaStartLevelItemsFormatHundreds() {
		assertStartLevel(new GradeManiaMode(), "100");
		assertStartLevel(new SpeedManiaMode(), "100");
		assertStartLevel(new PhantomManiaMode(), "100");
	}

	@Test
	void gradeMania2StartLevelPreservesReplayOnlyRollLabels() {
		IntegerMenuItem item = startLevel(new GradeMania2Mode());

		item.value = 3;
		assertEquals("300", item.getValueString());
		item.value = 10;
		assertEquals("ROLL", item.getValueString());
		item.value = 11;
		assertEquals("M-ROLL", item.getValueString());
	}

	@Test
	void speedManiaLevel500TorikanUsesNoneAtZero() {
		SpeedManiaMode mode = new SpeedManiaMode();
		IntegerMenuItem item = (IntegerMenuItem) mode.menu.get(4);

		item.value = 0;
		assertEquals("NONE", item.getValueString());
		item.value = 60;
		assertEquals("00:01.00", item.getValueString());
	}

	@Test
	void modeMenuOrdersStayStable() {
		assertMenuDisplayNames(new GradeManiaMode(),
				"LEVEL", "FULL GHOST", "20G MODE", "LVSTOPSE", "SHOW STIME", "BIG");
		assertMenuDisplayNames(new GradeMania2Mode(),
				"LEVEL", "FULL GHOST", "20G MODE", "LVSTOPSE", "BIG", "SHOW STIME");
		assertMenuDisplayNames(new SpeedManiaMode(),
				"LEVEL", "LVSTOPSE", "SHOW STIME", "BIG", "LV500LIMIT");
		assertMenuDisplayNames(new PhantomManiaMode(),
				"LEVEL", "LVSTOPSE", "SHOW STIME", "BIG");
	}

	private static void assertStartLevel(AbstractMode mode, String expected) {
		IntegerMenuItem item = startLevel(mode);
		item.value = 1;
		assertEquals(expected, item.getValueString());
	}

	private static IntegerMenuItem startLevel(AbstractMode mode) {
		return (IntegerMenuItem) mode.menu.get(0);
	}

	private static void assertMenuDisplayNames(AbstractMode mode, String... displayNames) {
		assertEquals(displayNames.length, mode.menu.size());
		for(int i = 0; i < displayNames.length; i++) {
			AbstractMenuItem<?> item = mode.menu.get(i);
			assertEquals(displayNames[i], item.displayName, "menu item " + i);
		}
	}
}
