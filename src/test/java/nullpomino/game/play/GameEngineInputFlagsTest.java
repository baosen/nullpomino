package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;

/**
 * Pins the per-frame input-flag plumbing on GameEngine: initial rotation
 * direction snapshot, initial hold latching, and the continuous-use
 * release rules in checkDropContinuousUse. These run during piece
 * spawn / lock / move and govern whether buffered inputs survive across
 * piece transitions.
 */
class GameEngineInputFlagsTest {

	private static GameEngine newEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	@Test
	void initialRotateClearsBothFlagsWhenNoButtonPressed() {
		GameEngine eng = newEngine();
		eng.initialRotateDirection = -1;
		eng.initialHoldFlag = true;

		eng.initialRotate();

		assertEquals(0, eng.initialRotateDirection);
		assertFalse(eng.initialHoldFlag);
	}

	@Test
	void initialRotatePicksLeftRotationFromButtonA() {
		GameEngine eng = newEngine();
		eng.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		eng.initialRotate();

		assertEquals(-1, eng.initialRotateDirection);
	}

	@Test
	void initialRotatePicksLeftRotationFromButtonC() {
		GameEngine eng = newEngine();
		eng.ctrl.buttonTime[Controller.BUTTON_C] = 1;

		eng.initialRotate();

		assertEquals(-1, eng.initialRotateDirection);
	}

	@Test
	void initialRotatePicksRightRotationFromButtonB() {
		GameEngine eng = newEngine();
		eng.ctrl.buttonTime[Controller.BUTTON_B] = 1;

		eng.initialRotate();

		assertEquals(1, eng.initialRotateDirection);
	}

	@Test
	void initialRotatePicks180FromButtonE() {
		GameEngine eng = newEngine();
		eng.ctrl.buttonTime[Controller.BUTTON_E] = 1;

		eng.initialRotate();

		assertEquals(2, eng.initialRotateDirection);
	}

	@Test
	void initialRotateLatchesHoldFromButtonD() {
		GameEngine eng = newEngine();
		eng.ctrl.buttonTime[Controller.BUTTON_D] = 1;

		eng.initialRotate();

		assertTrue(eng.initialHoldFlag);
		assertTrue(eng.initialHoldContinuousUse,
				"initial-hold must mark continuous use until the player releases D");
	}

	@Test
	void initialRotateSkipsLatchingWhenContinuousUseAlreadyHeld() {
		GameEngine eng = newEngine();
		eng.initialRotateContinuousUse = true;
		eng.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		eng.initialRotate();

		assertEquals(0, eng.initialRotateDirection,
				"continuous-use blocks re-arming the rotation");
	}

	@Test
	void initialRotateSkipsLatchingWhenRuleOptDisablesIt() {
		GameEngine eng = newEngine();
		eng.ruleopt.rotateInitial = false;
		eng.ctrl.buttonTime[Controller.BUTTON_B] = 1;

		eng.initialRotate();

		assertEquals(0, eng.initialRotateDirection);
	}

	@Test
	void checkDropContinuousUseReleasesAllFlagsWhenButtonsAreNotHeld() {
		GameEngine eng = newEngine();
		eng.gameActive = true;
		eng.softdropContinuousUse = true;
		eng.harddropContinuousUse = true;
		eng.initialHoldContinuousUse = true;
		eng.initialRotateContinuousUse = true;
		eng.ruleopt.softdropLimit = true;
		eng.ruleopt.harddropLimit = true;
		eng.ruleopt.holdInitialLimit = true;
		eng.ruleopt.rotateInitialLimit = true;

		eng.checkDropContinuousUse();

		assertFalse(eng.softdropContinuousUse,
				"DOWN released → softdrop continuous use cleared");
		assertFalse(eng.harddropContinuousUse,
				"UP released → harddrop continuous use cleared");
		assertFalse(eng.initialHoldContinuousUse,
				"D released → initial-hold continuous use cleared");
	}

	@Test
	void checkDropContinuousUseLeavesFlagsAloneWhenGameIsInactive() {
		GameEngine eng = newEngine();
		eng.gameActive = false;
		eng.softdropContinuousUse = true;

		eng.checkDropContinuousUse();

		assertTrue(eng.softdropContinuousUse,
				"checkDropContinuousUse no-ops when gameActive is false");
	}

	@Test
	void checkDropContinuousUseClearsRotateFlagsWhenLimitDisabled() {
		GameEngine eng = newEngine();
		eng.gameActive = true;
		eng.initialRotateContinuousUse = true;
		eng.ruleopt.rotateInitialLimit = false;

		eng.checkDropContinuousUse();

		assertFalse(eng.initialRotateContinuousUse,
				"rotate limit off → continuous use must clear");
	}
}
