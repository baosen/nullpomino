package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;

import org.junit.jupiter.api.Test;

/**
 * Pins the {@link GameEngine#initialRotate} input → state mapping
 * (BUTTON_A/C → dir=-1, BUTTON_B → dir=1, BUTTON_E → dir=2) plus the
 * BUTTON_D initial-hold handshake, and the {@link GameEngine#shutdown}
 * field-clearing contract that releases every nullable reference so the
 * GC can collect the engine after a game ends.
 */
class GameEngineInitialRotateTest {

	@Test
	void initialRotateMapsButtonAToCounterClockwise() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = true;
		engine.initialRotateContinuousUse = false;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		engine.initialRotate();

		assertEquals(-1, engine.initialRotateDirection,
				"BUTTON_A maps to counter-clockwise rotation (-1)");
		assertFalse(engine.initialHoldFlag);
	}

	@Test
	void initialRotateMapsButtonBToClockwise() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = true;
		engine.initialRotateContinuousUse = false;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;

		engine.initialRotate();

		assertEquals(1, engine.initialRotateDirection,
				"BUTTON_B maps to clockwise rotation (1)");
	}

	@Test
	void initialRotateMapsButtonEToOneEightyFlip() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = true;
		engine.initialRotateContinuousUse = false;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1;

		engine.initialRotate();

		assertEquals(2, engine.initialRotateDirection,
				"BUTTON_E maps to 180-degree flip (2)");
	}

	@Test
	void initialRotateMapsButtonCToCounterClockwise() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = true;
		engine.initialRotateContinuousUse = false;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_C] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_C] = 1;

		engine.initialRotate();

		// BUTTON_C is an alias for counter-clockwise alongside BUTTON_A.
		assertEquals(-1, engine.initialRotateDirection);
	}

	@Test
	void initialRotateLeavesDirectionAtZeroWhenNoRotateButtonPressed() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = true;
		engine.initialRotateContinuousUse = false;
		engine.ctrl = new Controller();

		engine.initialRotate();

		assertEquals(0, engine.initialRotateDirection);
	}

	@Test
	void initialRotateSkipsButtonReadWhenRuleOptDisablesIt() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = false;
		engine.initialRotateContinuousUse = false;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		engine.initialRotate();

		assertEquals(0, engine.initialRotateDirection,
				"rule disables initial rotate → button presses are ignored");
	}

	@Test
	void initialRotateSkipsButtonReadWhenContinuousUseIsLatched() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = true;
		engine.initialRotateContinuousUse = true;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		engine.initialRotate();

		assertEquals(0, engine.initialRotateDirection,
				"continuous-use latched → button presses are ignored");
	}

	@Test
	void initialRotateSetsInitialHoldFlagAndContinuousUseWhenButtonDPressedAndHoldOK() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = false;
		engine.ruleopt.holdInitial = true;
		engine.ruleopt.holdEnable = true;
		engine.holdDisable = false;
		engine.holdUsedCount = 0;
		engine.ruleopt.holdLimit = -1;
		engine.initialHoldContinuousUse = false;
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

		engine.initialRotate();

		assertTrue(engine.initialHoldFlag);
		assertTrue(engine.initialHoldContinuousUse,
				"successful initial-hold latches the continuous-use flag");
	}

	@Test
	void initialRotateSkipsHoldWhenRuleOptDisablesIt() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = false;
		engine.ruleopt.holdInitial = false; // disabled
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

		engine.initialRotate();

		assertFalse(engine.initialHoldFlag);
		assertFalse(engine.initialHoldContinuousUse);
	}

	@Test
	void initialRotateSkipsHoldWhenIsHoldOKReturnsFalse() {
		GameEngine engine = freshEngine();
		engine.ruleopt.rotateInitial = false;
		engine.ruleopt.holdInitial = true;
		engine.ruleopt.holdEnable = false; // isHoldOK returns false
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

		engine.initialRotate();

		assertFalse(engine.initialHoldFlag);
	}

	@Test
	void shutdownNullsEveryNullableEngineReferenceForGCRelease() {
		GameEngine engine = freshEngine();
		engine.createFieldIfNeeded();

		engine.shutdown();

		assertNull(engine.owner);
		assertNull(engine.ruleopt);
		assertNull(engine.wallkick);
		assertNull(engine.randomizer);
		assertNull(engine.field);
		assertNull(engine.ctrl);
		assertNull(engine.statistics);
		assertNull(engine.speed);
		assertNull(engine.random);
		assertNull(engine.replayData);
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}
}
