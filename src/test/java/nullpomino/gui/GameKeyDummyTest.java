package nullpomino.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Controller;
import nullpomino.util.CustomProperties;

class GameKeyDummyTest {

	private static final class TestKey extends GameKeyDummy {
		TestKey() {
			super();
		}

		TestKey(int player) {
			super(player);
		}
	}

	@Test
	void defaultConstructorUsesPlayerZeroAndAllocatesArrays() {
		TestKey key = new TestKey();

		assertEquals(0, key.player);
		assertEquals(GameKeyDummy.MAX_BUTTON, key.keymap.length);
		assertEquals(GameKeyDummy.MAX_BUTTON, key.keymapNav.length);
		assertEquals(GameKeyDummy.MAX_BUTTON, key.buttonmap.length);
		for (int joy : key.buttonmap) {
			assertEquals(-1, joy);
		}
	}

	@Test
	void playerConstructorAssignsPlayerNumber() {
		TestKey key = new TestKey(2);

		assertEquals(2, key.player);
	}

	@Test
	void loadConfigDefaultsPlayerOneFaceButtonsToGamepadOrdinals() {
		TestKey key = new TestKey(0);

		key.loadConfig(new CustomProperties());

		assertEquals(0, key.buttonmap[GameKeyDummy.BUTTON_A], "SOUTH");
		assertEquals(1, key.buttonmap[GameKeyDummy.BUTTON_B], "EAST");
		assertEquals(2, key.buttonmap[GameKeyDummy.BUTTON_C], "WEST");
		assertEquals(9, key.buttonmap[GameKeyDummy.BUTTON_D], "LEFT_SHOULDER");
		assertEquals(-1, key.buttonmap[GameKeyDummy.BUTTON_E]);
	}

	@Test
	void loadConfigLeavesPlayerTwoButtonmapUnbound() {
		TestKey key = new TestKey(1);

		key.loadConfig(new CustomProperties());

		for (int i = GameKeyDummy.BUTTON_A; i <= GameKeyDummy.BUTTON_D; i++) {
			assertEquals(-1, key.buttonmap[i]);
		}
	}

	@Test
	void clearZeroesEveryInputState() {
		TestKey key = new TestKey();
		for (int i = 0; i < GameKeyDummy.MAX_BUTTON; i++) {
			key.setInputState(i, 5);
		}

		key.clear();

		for (int i = 0; i < GameKeyDummy.MAX_BUTTON; i++) {
			assertEquals(0, key.getInputState(i));
		}
	}

	@Test
	void isPushKeyOnlyTrueOnTheFirstHeldFrame() {
		TestKey key = new TestKey();

		key.setInputState(GameKeyDummy.BUTTON_A, 1);
		assertTrue(key.isPushKey(GameKeyDummy.BUTTON_A));

		key.setInputState(GameKeyDummy.BUTTON_A, 2);
		assertFalse(key.isPushKey(GameKeyDummy.BUTTON_A));

		key.setInputState(GameKeyDummy.BUTTON_A, 0);
		assertFalse(key.isPushKey(GameKeyDummy.BUTTON_A));
	}

	@Test
	void isPressKeyTrueWhenStateIsAtLeastOne() {
		TestKey key = new TestKey();

		assertFalse(key.isPressKey(GameKeyDummy.BUTTON_B));
		key.setInputState(GameKeyDummy.BUTTON_B, 1);
		assertTrue(key.isPressKey(GameKeyDummy.BUTTON_B));
		key.setInputState(GameKeyDummy.BUTTON_B, 100);
		assertTrue(key.isPressKey(GameKeyDummy.BUTTON_B));
	}

	@Test
	void isMenuRepeatKeyHandlesInitialPressDelayedRepeatAndCAccelerator() {
		TestKey key = new TestKey();

		key.setInputState(GameKeyDummy.BUTTON_DOWN, 1);
		assertTrue(key.isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN));

		key.setInputState(GameKeyDummy.BUTTON_DOWN, 26);
		assertFalse(key.isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN));
		key.setInputState(GameKeyDummy.BUTTON_DOWN, 27);
		assertTrue(key.isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN));

		key.setInputState(GameKeyDummy.BUTTON_DOWN, 5);
		key.setInputState(GameKeyDummy.BUTTON_C, 1);
		assertTrue(key.isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN));

		key.setInputState(GameKeyDummy.BUTTON_DOWN, 0);
		key.setInputState(GameKeyDummy.BUTTON_C, 0);
		assertFalse(key.isMenuRepeatKey(GameKeyDummy.BUTTON_DOWN));
	}

	@Test
	void getAndSetInputStateRoundTrip() {
		TestKey key = new TestKey();

		key.setInputState(GameKeyDummy.BUTTON_F, 42);

		assertEquals(42, key.getInputState(GameKeyDummy.BUTTON_F));
	}

	@Test
	void saveAndLoadConfigRoundTripsKeymapsAndJoyBorder() {
		TestKey original = new TestKey(1);
		for (int i = 0; i < GameKeyDummy.MAX_BUTTON; i++) {
			original.keymap[i] = 100 + i;
			original.keymapNav[i] = 200 + i;
			original.buttonmap[i] = 300 + i;
		}
		original.joyBorder = 64;

		CustomProperties prop = new CustomProperties();
		original.saveConfig(prop);
		TestKey loaded = new TestKey(1);
		loaded.loadConfig(prop);

		for (int i = 0; i < GameKeyDummy.MAX_BUTTON; i++) {
			assertEquals(original.keymap[i], loaded.keymap[i], "keymap[" + i + "]");
			assertEquals(original.keymapNav[i], loaded.keymapNav[i], "keymapNav[" + i + "]");
		}
		// Only the action buttons (A..SCREENSHOT) are persisted to button.* keys.
		for (int i = GameKeyDummy.BUTTON_A; i < GameKeyDummy.MAX_BUTTON; i++) {
			assertEquals(original.buttonmap[i], loaded.buttonmap[i], "buttonmap[" + i + "]");
		}
		assertEquals(64, loaded.joyBorder);
	}

	@Test
	void loadConfigKeymapNavFallsBackToKeymapWhenAbsent() {
		CustomProperties prop = new CustomProperties();
		prop.setProperty("key.p0.up", 99);

		TestKey key = new TestKey();
		key.loadConfig(prop);

		assertEquals(99, key.keymap[GameKeyDummy.BUTTON_UP]);
		assertEquals(99, key.keymapNav[GameKeyDummy.BUTTON_UP]);
	}

	@Test
	void inputStatusUpdatePropagatesPressedKeysIntoController() {
		TestKey key = new TestKey();
		key.setInputState(GameKeyDummy.BUTTON_LEFT, 1);
		key.setInputState(GameKeyDummy.BUTTON_A, 5);

		Controller ctrl = new Controller();
		key.inputStatusUpdate(ctrl);

		assertTrue(ctrl.buttonPress[Controller.BUTTON_LEFT]);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_A]);
		assertFalse(ctrl.buttonPress[Controller.BUTTON_RIGHT]);
	}

	@Test
	void isNavKeyAlwaysReturnsFalse() {
		assertFalse(GameKeyDummy.isNavKey(0));
		assertFalse(GameKeyDummy.isNavKey(99));
	}
}
