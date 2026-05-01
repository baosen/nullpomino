package nullpomino.game.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.event.EventReceiver;

import org.junit.jupiter.api.Test;

class MenuItemValueStringTest {

	@Test
	void levelMenuItemMultipliesRawValueByOneHundred() {
		LevelMenuItem item = new LevelMenuItem("level", "LEVEL",
				EventReceiver.COLOR_BLUE, 0, 0, 99);

		assertEquals("0", item.getValueString());

		item.value = 1;
		assertEquals("100", item.getValueString());

		item.value = 99;
		assertEquals("9900", item.getValueString());
	}

	@Test
	void rollLevelMenuItemRendersTenAndElevenAsStaffRollLabels() {
		RollLevelMenuItem item = new RollLevelMenuItem("rolllv", "ROLL",
				EventReceiver.COLOR_BLUE, 0, 0, 11);

		item.value = 9;
		assertEquals("900", item.getValueString());

		item.value = 10;
		assertEquals("ROLL", item.getValueString());

		item.value = 11;
		assertEquals("M-ROLL", item.getValueString());
	}

	@Test
	void optionalTimeMenuItemRendersDisabledLabelOnZeroAndTimeOtherwise() {
		OptionalTimeMenuItem item = new OptionalTimeMenuItem("limit", "LIMIT",
				EventReceiver.COLOR_BLUE, 0, 0, 7200, "NONE");

		assertEquals("NONE", item.getValueString());

		item.value = 60;
		assertEquals("00:01.00", item.getValueString());

		item.value = 3600;
		assertEquals("01:00.00", item.getValueString());
	}

	@Test
	void timeMenuItemFormatsValueAsMinutesSecondsFrames() {
		TimeMenuItem item = new TimeMenuItem("time", "TIME",
				EventReceiver.COLOR_BLUE, 0, 0, 7200);

		assertEquals("00:00.00", item.getValueString());

		item.value = 60;
		assertEquals("00:01.00", item.getValueString());

		item.value = 3661;
		// hours = 1, minutes = 1, seconds = 1 → frames = 1 * 5 / 3 = 1
		assertEquals("01:01.01", item.getValueString());
	}

	@Test
	void timeMenuItemDefaultIncrementIsSixtyTicks() {
		TimeMenuItem item = new TimeMenuItem("time", "TIME",
				EventReceiver.COLOR_BLUE, 0, 0, 1200);

		item.change(1, 0);
		assertEquals(60, item.value);

		item.change(1, 0);
		assertEquals(120, item.value);
	}

	@Test
	void timeMenuItemRespectsCustomIncrementOnChange() {
		TimeMenuItem item = new TimeMenuItem("time", "TIME",
				EventReceiver.COLOR_BLUE, 0, 0, 1200, 30);

		item.change(1, 0);
		assertEquals(30, item.value);

		item.change(-1, 0);
		assertEquals(0, item.value);
	}

	@Test
	void enumMenuItemBoundsLockToChoiceCountMinusOne() {
		EnumMenuItem item = new EnumMenuItem("kind", "KIND",
				EventReceiver.COLOR_BLUE, 0, new String[] {"FIRST", "SECOND", "THIRD"}) {};

		assertEquals(0, item.min);
		assertEquals(2, item.max);
		assertEquals("FIRST", item.getValueString());

		item.change(-1, 0);
		assertEquals("THIRD", item.getValueString());

		item.change(1, 0);
		assertEquals("FIRST", item.getValueString());

		item.change(1, 0);
		assertEquals("SECOND", item.getValueString());
	}

	@Test
	void onOffMenuItemRendersUppercaseOnOff() {
		OnOffMenuItem item = new OnOffMenuItem("ghost", "GHOST",
				EventReceiver.COLOR_BLUE, false);

		assertEquals("OFF", item.getValueString());
		item.change(1, 0);
		assertEquals("ON", item.getValueString());
	}

	@Test
	void oxMenuItemRendersAsOXFontGlyphs() {
		OXMenuItem item = new OXMenuItem("spin", "SPIN",
				EventReceiver.COLOR_BLUE, false);

		assertEquals("e", item.getValueString());
		item.change(1, 0);
		assertEquals("c", item.getValueString());
	}
}
