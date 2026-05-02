package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins the observable contract on {@link GameEngine#statGameOver}:
 * mode-override short-circuit, gameEnded() side-effects on the
 * dead-end first frame, and the revivable-empty-field branch that
 * decrements lives and returns to MOVE once the ARE delay elapses.
 */
class GameEngineStatGameOverTest {

	@Test
	void modeOverrideShortCircuitsBeforeAnyEngineSideEffect() {
		// onGameOver returning true means the mode took over the screen —
		// the engine must not run gameEnded(), reset BGM, or touch the
		// field.
		BlockingMode mode = new BlockingMode();
		mode.onGameOverResult = true;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.gameActive = true;
		engine.timerActive = true;
		engine.lives = 0;
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;

		engine.statGameOver();

		assertTrue(mode.onGameOverCalled);
		assertTrue(engine.gameActive,
				"mode override must skip gameEnded()");
		assertTrue(engine.timerActive);
		assertEquals(0, engine.statc[0]);
	}

	@Test
	void deadEndFirstFrameWithEmptyFieldRunsGameEndedAndAdvancesPastTheRowSweep() {
		// First frame of dead-end (lives==0, statc[0]==0) on an empty
		// field: gameEnded() runs, the empty-field shortcut bumps
		// statc[0] to height+1, and the same call's elif also fires
		// (statc[0] == height+1) which plays the gameover SE and bumps
		// to height+2.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.timerActive = true;
		engine.gameStarted = true;
		engine.lives = 0;
		engine.statc[0] = 0;

		engine.statGameOver();

		assertFalse(engine.gameActive,
				"gameEnded() ran -> gameActive cleared");
		assertEquals(engine.field.getHeight() + 2, engine.statc[0],
				"empty-field shortcut + the same-call elif advance statc[0] "
						+ "by height+2");
	}

	@Test
	void deadEndFlowEventuallyTransitionsToResultAfterTheTimedHoldElapses() {
		// Drive the entire dead-end sequence on an empty field. After
		// height+1+180 frames of holding (the auto-timeout), the engine
		// transitions to RESULT for the player(s) hit by gameoverAll.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.gameStarted = true;
		engine.lives = 0;

		// Run enough frames for statc[0] to walk from 0 past
		// field.getHeight() + 1 + 180. Stop when stat flips to RESULT.
		for(int i = 0; i < engine.field.getHeight() + 1 + 200; i++) {
			engine.statGameOver();
			if(engine.stat == GameEngine.Status.RESULT) break;
		}

		assertEquals(GameEngine.Status.RESULT, engine.stat,
				"dead-end auto-timeout transitions to RESULT");
	}

	@Test
	void revivableEmptyFieldAdvancesAreCounterAndDecrementsLivesAtAreLimit() {
		// Past the first-frame init (statc[0]=1), an empty field counts
		// statc[1] up to ARE; on the frame ARE is reached, lives
		// decrement, statc resets, and stat returns to MOVE.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.gameStarted = true;
		engine.lives = 2;
		engine.statc[0] = 1;
		// Set ARE = 0 so the first revival call commits to MOVE immediately.
		engine.speed.are = 0;
		engine.statc[1] = 0;

		engine.statGameOver();

		assertEquals(GameEngine.Status.MOVE, engine.stat,
				"reaching ARE -> stat returns to MOVE");
		assertEquals(1, engine.lives, "one life consumed");
		assertEquals(0, engine.statc[0], "resetStatc clears the counter");
	}

	private static final class BlockingMode extends AbstractMode {
		boolean onGameOverCalled;
		boolean onGameOverResult;

		@Override
		public String getName() { return "stub"; }

		@Override
		public void modeInit(GameManager manager) {}

		@Override
		public void playerInit(GameEngine engine, int playerID) {}

		@Override
		public void renderInput(GameEngine engine, int playerID) {}

		@Override
		public boolean onGameOver(GameEngine engine, int playerID) {
			onGameOverCalled = true;
			return onGameOverResult;
		}
	}
}
