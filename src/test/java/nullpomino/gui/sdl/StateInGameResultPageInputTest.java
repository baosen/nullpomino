package nullpomino.gui.sdl;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;

import org.junit.jupiter.api.Test;

/**
 * Pins the routing for post-game RESULT-screen page navigation extras
 * (mouse wheel, PageUp / PageDown). The mode-side onResult handlers
 * already poll {@code engine.ctrl.isMenuRepeatKey(BUTTON_UP/DOWN)}; this
 * helper feeds wheel / page-key state into the same buttonPress slots so
 * pagination works without requiring the user to map a physical arrow.
 *
 * <p>OR semantics matter: {@code inputStatusUpdate} writes the keyboard
 * arrow state into {@code buttonPress} just before this helper runs, so
 * the helper must not clobber a held arrow back to {@code false}.
 */
class StateInGameResultPageInputTest {

	@Test
	void wheelUpPressesButtonUp() {
		Controller ctrl = new Controller();
		StateInGameSDL.applyResultPageInputs(ctrl, 1, false, false);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_UP]);
		assertFalse(ctrl.buttonPress[Controller.BUTTON_DOWN]);
	}

	@Test
	void wheelDownPressesButtonDown() {
		Controller ctrl = new Controller();
		StateInGameSDL.applyResultPageInputs(ctrl, -1, false, false);
		assertFalse(ctrl.buttonPress[Controller.BUTTON_UP]);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_DOWN]);
	}

	@Test
	void wheelZeroLeavesButtonsUntouched() {
		Controller ctrl = new Controller();
		StateInGameSDL.applyResultPageInputs(ctrl, 0, false, false);
		assertFalse(ctrl.buttonPress[Controller.BUTTON_UP]);
		assertFalse(ctrl.buttonPress[Controller.BUTTON_DOWN]);
	}

	@Test
	void pageUpKeyPressesButtonUp() {
		Controller ctrl = new Controller();
		StateInGameSDL.applyResultPageInputs(ctrl, 0, true, false);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_UP]);
		assertFalse(ctrl.buttonPress[Controller.BUTTON_DOWN]);
	}

	@Test
	void pageDownKeyPressesButtonDown() {
		Controller ctrl = new Controller();
		StateInGameSDL.applyResultPageInputs(ctrl, 0, false, true);
		assertFalse(ctrl.buttonPress[Controller.BUTTON_UP]);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_DOWN]);
	}

	@Test
	void multiTickWheelStillCountsAsOnePress() {
		// Result pages number only 2–3 deep, so a fast scroll yielding
		// wheel=3 should not skip past every page in one frame.
		Controller ctrl = new Controller();
		StateInGameSDL.applyResultPageInputs(ctrl, 3, false, false);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_UP]);
		// Boolean press: held vs. multi-press is a no-op distinction.
	}

	@Test
	void heldArrowKeyIsNotClobbered() {
		// inputStatusUpdate runs first and may have set BUTTON_UP from the
		// physical arrow. The helper must OR (not assign) so we don't drop
		// the held key when no wheel / page event happens this frame.
		Controller ctrl = new Controller();
		ctrl.buttonPress[Controller.BUTTON_UP] = true;
		StateInGameSDL.applyResultPageInputs(ctrl, 0, false, false);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_UP]);
	}

	@Test
	void heldArrowSurvivesOppositeWheel() {
		// User is holding UP and also flicks the wheel down: both buttons
		// end up pressed. The mode's onResult will apply the most recent
		// repeat first; either outcome is acceptable, but neither should
		// be silently lost.
		Controller ctrl = new Controller();
		ctrl.buttonPress[Controller.BUTTON_UP] = true;
		StateInGameSDL.applyResultPageInputs(ctrl, -1, false, false);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_UP]);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_DOWN]);
	}

	@Test
	void pageUpAndPageDownTogetherPressBoth() {
		// Pathological keyboard state but cheap to defend against — the
		// mode's onResult deals with simultaneous UP+DOWN by running both
		// branches, which cancels out in the wrap-around math.
		Controller ctrl = new Controller();
		StateInGameSDL.applyResultPageInputs(ctrl, 0, true, true);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_UP]);
		assertTrue(ctrl.buttonPress[Controller.BUTTON_DOWN]);
	}

	@Test
	void otherButtonsAreUntouched() {
		// The helper has no business touching anything besides UP / DOWN.
		// If a future change widens it (e.g. Home → BUTTON_A confirm),
		// this test should fail and force a deliberate update.
		Controller ctrl = new Controller();
		StateInGameSDL.applyResultPageInputs(ctrl, 1, true, true);
		for(int i = 0; i < Controller.BUTTON_COUNT; i++) {
			if(i == Controller.BUTTON_UP || i == Controller.BUTTON_DOWN) continue;
			assertFalse(ctrl.buttonPress[i], "button " + i + " must not be touched");
		}
	}
}
