package nullpomino.game.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.event.EventReceiver;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

class MenuItemPersistenceTest {

	@Test
	void integerItemSavesAndLoadsGlobalLegacyKey() {
		IntegerMenuItem item = new IntegerMenuItem("startlevel", "LEVEL",
				EventReceiver.COLOR_BLUE, 0, 0, 20);
		CustomProperties props = new CustomProperties();

		item.value = 12;
		item.save(-1, props, "marathon");
		assertEquals(12, props.getProperty("marathon.startlevel", -1));

		item.value = 0;
		item.load(-1, props, "marathon");
		assertEquals(12, item.value);
	}

	@Test
	void integerItemValueStringRendersCurrentValue() {
		IntegerMenuItem item = new IntegerMenuItem("startlevel", "LEVEL",
				EventReceiver.COLOR_BLUE, 0, 0, 20);

		assertEquals("0", item.getValueString());
		item.value = 7;
		assertEquals("7", item.getValueString());
	}

	@Test
	void booleanItemSavesAndLoadsPlayerLegacyKey() {
		BooleanMenuItem item = new BooleanMenuItem("big", "BIG",
				EventReceiver.COLOR_BLUE, false);
		CustomProperties props = new CustomProperties();

		item.value = true;
		item.save(1, props, "vsbattle");
		assertEquals(true, props.getProperty("vsbattle.big.p1", false));

		item.value = false;
		item.load(1, props, "vsbattle");
		assertEquals(true, item.value);
	}

	@Test
	void missingValuesLoadDefaults() {
		IntegerMenuItem integerItem = new IntegerMenuItem("goal", "GOAL",
				EventReceiver.COLOR_BLUE, 40, 0, 200);
		BooleanMenuItem booleanItem = new BooleanMenuItem("endless", "ENDLESS",
				EventReceiver.COLOR_BLUE, true);
		CustomProperties props = new CustomProperties();

		integerItem.value = 0;
		booleanItem.value = false;
		integerItem.load(-1, props, "linerace");
		booleanItem.load(-1, props, "linerace");

		assertEquals(40, integerItem.value);
		assertEquals(true, booleanItem.value);
	}

	@Test
	void valueStringFormatsStayStable() {
		BooleanMenuItem booleanItem = new BooleanMenuItem("big", "BIG",
				EventReceiver.COLOR_BLUE, false);
		OnOffMenuItem onOffItem = new OnOffMenuItem("ghost", "GHOST",
				EventReceiver.COLOR_BLUE, false);
		OXMenuItem oxItem = new OXMenuItem("spin", "SPIN",
				EventReceiver.COLOR_BLUE, false);
		EnumMenuItem enumItem = new EnumMenuItem("goal", "GOAL",
				EventReceiver.COLOR_BLUE, 1, new String[] {"A", "B"}) {};

		assertEquals("FALSE", booleanItem.getValueString());
		assertEquals("OFF", onOffItem.getValueString());
		assertEquals("e", oxItem.getValueString());
		assertEquals("B", enumItem.getValueString());

		booleanItem.change(1, 0);
		onOffItem.change(1, 0);
		oxItem.change(1, 0);
		enumItem.change(1, 0);

		assertEquals("TRUE", booleanItem.getValueString());
		assertEquals("ON", onOffItem.getValueString());
		assertEquals("c", oxItem.getValueString());
		assertEquals("A", enumItem.getValueString());
	}

	@Test
	void integerItemChangeWrapsAcrossBounds() {
		IntegerMenuItem item = new IntegerMenuItem("level", "LEVEL",
				EventReceiver.COLOR_BLUE, 0, 0, 2);

		item.change(-1, 0);
		assertEquals(2, item.value);

		item.change(1, 0);
		assertEquals(0, item.value);
	}

	@Test
	void timeItemChangeUsesIncrementAndSharedBoundsWrapping() {
		TimeMenuItem item = new TimeMenuItem("time", "TIME",
				EventReceiver.COLOR_BLUE, 60, 0, 120, 60);

		item.change(1, 0);
		assertEquals(120, item.value);

		item.change(1, 0);
		assertEquals(0, item.value);
	}
}
