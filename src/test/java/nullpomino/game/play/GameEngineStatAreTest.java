package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GameEngine#statARE}'s observable contract: the mode
 * override short-circuit, the per-frame counter advance, the
 * delay-cancel bitfields driven by ruleopt.areCancel*, the
 * INTERRUPTITEM transition when interruptItemNumber is set, and the
 * MOVE transition when statc[0] reaches statc[1].
 */
class GameEngineStatAreTest {

	@Test
	void modeOverrideShortCircuitsBeforeStatcAdvance() {
		BlockingMode mode = new BlockingMode();
		mode.onAREResult = true;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 5;
		engine.statc[1] = 10;
		engine.stat = GameEngine.Status.ARE;

		engine.statARE();

		assertTrue(mode.onAREcalled);
		assertEquals(5, engine.statc[0],
				"mode override returning true must skip statc[0]++");
		assertEquals(GameEngine.Status.ARE, engine.stat);
	}

	@Test
	void normalFrameAdvancesStatcAndStaysInAreUntilLimitReached() {
		// statc[0] = 0, statc[1] = 5: each call bumps statc[0] until it
		// hits statc[1], at which point stat -> MOVE.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 0;
		engine.statc[1] = 3;
		engine.stat = GameEngine.Status.ARE;
		engine.gameActive = true;

		engine.statARE();
		assertEquals(1, engine.statc[0]);
		assertEquals(GameEngine.Status.ARE, engine.stat);

		engine.statARE();
		assertEquals(2, engine.statc[0]);
		assertEquals(GameEngine.Status.ARE, engine.stat);

		// Third call advances statc[0] to 3, hits statc[1], transitions
		// to MOVE and resets statc.
		engine.statARE();
		assertEquals(GameEngine.Status.MOVE, engine.stat);
		assertEquals(0, engine.statc[0], "transition to MOVE resets statc");
	}

	@Test
	void delayCancelMoveBitsTrackLeftAndRightPushes() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 0;
		engine.statc[1] = 100; // never transition
		engine.stat = GameEngine.Status.ARE;

		// Press LEFT — delayCancelMoveLeft becomes true.
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		engine.statARE();
		assertTrue(engine.delayCancelMoveLeft);
		assertFalse(engine.delayCancelMoveRight);

		// Press RIGHT — delayCancelMoveRight true, left false.
		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		engine.statARE();
		assertFalse(engine.delayCancelMoveLeft);
		assertTrue(engine.delayCancelMoveRight);
	}

	@Test
	void delayCancelFiresWhenAreCancelMoveAllowsAndJumpsStatcToLimit() {
		// areCancelMove=true and a Left press while statc[0] < statc[1]
		// short-circuits the ARE wait — statc[0] is bumped to statc[1]
		// and the next frame transitions to MOVE.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.ruleopt.areCancelMove = true;
		engine.statc[0] = 1;
		engine.statc[1] = 30;
		engine.stat = GameEngine.Status.ARE;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		engine.statARE();

		assertTrue(engine.delayCancel,
				"delayCancel must reflect that the move-cancel branch ran");
		// After the cancel statc[0] was bumped to statc[1] (30) and then
		// the transition fires (statc[0] >= statc[1]) -> stat = MOVE.
		assertEquals(GameEngine.Status.MOVE, engine.stat);
	}

	@Test
	void delayCancelDoesNotFireWhenRuleOptDoesNotAllowMoveCancel() {
		// ruleopt.areCancelMove=false: pressing LEFT should not flip the
		// delayCancel bit.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.ruleopt.areCancelMove = false;
		engine.ruleopt.areCancelRotate = false;
		engine.ruleopt.areCancelHold = false;
		engine.statc[0] = 0;
		engine.statc[1] = 30;
		engine.stat = GameEngine.Status.ARE;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		engine.statARE();

		assertFalse(engine.delayCancel,
				"areCancelMove=false must keep delayCancel cleared");
		assertEquals(GameEngine.Status.ARE, engine.stat,
				"no cancel + statc[0] still below statc[1] -> stay in ARE");
	}

	@Test
	void delayCancelRotateFiresOnAbceWhenAreCancelRotateAllows() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.ruleopt.areCancelRotate = true;
		engine.statc[0] = 0;
		engine.statc[1] = 30;
		engine.stat = GameEngine.Status.ARE;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.statARE();

		assertTrue(engine.delayCancel);
		assertEquals(GameEngine.Status.MOVE, engine.stat);
	}

	@Test
	void delayCancelHoldFiresOnButtonDWhenAreCancelHoldAllows() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.ruleopt.areCancelHold = true;
		engine.statc[0] = 0;
		engine.statc[1] = 30;
		engine.stat = GameEngine.Status.ARE;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;
		engine.statARE();

		assertTrue(engine.delayCancel);
		assertEquals(GameEngine.Status.MOVE, engine.stat);
	}

	@Test
	void interruptItemPendingTransitionsToInterruptItemInsteadOfMove() {
		// When interruptItemNumber is set, finishing ARE goes through
		// INTERRUPTITEM rather than directly to MOVE.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 0;
		engine.statc[1] = 1;
		engine.interruptItemNumber = GameEngine.INTERRUPTITEM_MIRROR;
		engine.stat = GameEngine.Status.ARE;

		engine.statARE();

		assertEquals(GameEngine.Status.INTERRUPTITEM, engine.stat);
		assertEquals(GameEngine.Status.MOVE, engine.interruptItemPreviousStat,
				"the post-interrupt return-stat is recorded as MOVE");
	}

	@Test
	void lagAreSuppressesTheTransitionEvenAtCounterLimit() {
		// lagARE locks the engine in ARE — the transition guard
		// (statc[0] >= statc[1]) is conditional on !lagARE.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 50;
		engine.statc[1] = 1; // trivially exceeded
		engine.lagARE = true;
		engine.stat = GameEngine.Status.ARE;

		engine.statARE();

		assertEquals(GameEngine.Status.ARE, engine.stat,
				"lagARE keeps the engine in ARE even with statc[0] past statc[1]");
	}

	private static final class BlockingMode extends AbstractMode {
		boolean onAREcalled;
		boolean onAREResult;

		@Override
		public String getName() { return "stub"; }

		@Override
		public void modeInit(GameManager manager) {}

		@Override
		public void playerInit(GameEngine engine, int playerID) {}

		@Override
		public void renderInput(GameEngine engine, int playerID) {}

		@Override
		public boolean onARE(GameEngine engine, int playerID) {
			onAREcalled = true;
			return onAREResult;
		}
	}
}
