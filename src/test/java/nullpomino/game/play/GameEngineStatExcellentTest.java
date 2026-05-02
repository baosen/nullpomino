package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GameEngine#statExcellent}'s observable contract: the
 * mode override short-circuit, the first-frame setup (gameEnded(),
 * BGM fadesw, resetFieldVisible, the 'excellent' SE), the BUTTON_A
 * skip that snaps statc[0] to 600 once the animation has played for
 * 120 frames, and the GAMEOVER transition once the 600-frame display
 * elapses.
 */
class GameEngineStatExcellentTest {

	@Test
	void modeOverrideShortCircuitsBeforeFirstFrameSetup() {
		// onExcellent returning true means the mode took over the
		// excellent-screen animation — the engine must not run
		// gameEnded() or touch BGM fade.
		BlockingMode mode = new BlockingMode();
		mode.onExcellentResult = true;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.gameActive = true;
		engine.timerActive = true;
		engine.statc[0] = 0;
		engine.stat = GameEngine.Status.EXCELLENT;

		engine.statExcellent();

		assertTrue(mode.onExcellentCalled);
		assertTrue(engine.gameActive,
				"mode override must skip gameEnded() so gameActive stays set");
		assertTrue(engine.timerActive);
		assertFalse(engine.owner.bgmStatus.fadesw,
				"mode override must skip the BGM fade-trigger");
		assertEquals(0, engine.statc[0],
				"mode override must skip statc[0]++");
	}

	@Test
	void firstFrameRunsGameEndedTriggersBgmFadeAndAdvancesStatc() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.timerActive = true;
		engine.gameStarted = true;
		engine.statc[0] = 0;
		engine.stat = GameEngine.Status.EXCELLENT;

		engine.statExcellent();

		assertFalse(engine.gameActive,
				"first frame -> gameEnded() ran and gameActive cleared");
		assertFalse(engine.timerActive);
		assertTrue(engine.owner.bgmStatus.fadesw,
				"first frame triggers BGM fade-out");
		assertEquals(1, engine.statc[0],
				"non-skip frame advances statc[0] by 1");
	}

	@Test
	void buttonAAfterFrame120SnapsStatcToSixHundred() {
		// Once the animation has played for 120 frames, BUTTON_A skips
		// to the end (statc[0] = 600). statc[0]++ then runs (so 601).
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 200; // already past the 120 gate
		engine.statc[1] = 1; // suppress the GAMEOVER guard
		engine.stat = GameEngine.Status.EXCELLENT;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.statExcellent();

		// statc[0] was 200, BUTTON_A bumped it to 600, then the +1
		// branch ran (statc[1] != 0 means we skip the GAMEOVER else).
		assertEquals(601, engine.statc[0],
				"BUTTON_A past frame 120 -> snap to 600 and tick once more");
		assertEquals(GameEngine.Status.EXCELLENT, engine.stat,
				"statc[1] != 0 keeps us in EXCELLENT for one more frame");
	}

	@Test
	void buttonABeforeFrame120DoesNotSkipForward() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 50; // below the 120 gate
		engine.stat = GameEngine.Status.EXCELLENT;

		engine.ctrl = new Controller();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.statExcellent();

		assertEquals(51, engine.statc[0],
				"BUTTON_A before frame 120 does not snap; just normal advance");
	}

	@Test
	void hittingFrameSixHundredWithStatcOneZeroTransitionsToGameOver() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 600;
		engine.statc[1] = 0;
		engine.stat = GameEngine.Status.EXCELLENT;

		engine.statExcellent();

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat,
				"statc[0] >= 600 and statc[1] == 0 -> transition to GAMEOVER");
		assertEquals(0, engine.statc[0], "resetStatc clears the counter");
	}

	@Test
	void hittingFrameSixHundredWithStatcOneNonZeroDoesNotTransitionYet() {
		// statc[1] != 0 keeps the engine in EXCELLENT — used by modes
		// that want a longer hold past the 600-frame default.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.statc[0] = 600;
		engine.statc[1] = 1;
		engine.stat = GameEngine.Status.EXCELLENT;

		engine.statExcellent();

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
		assertEquals(601, engine.statc[0]);
	}

	private static final class BlockingMode extends AbstractMode {
		boolean onExcellentCalled;
		boolean onExcellentResult;

		@Override
		public String getName() { return "stub"; }

		@Override
		public void modeInit(GameManager manager) {}

		@Override
		public void playerInit(GameEngine engine, int playerID) {}

		@Override
		public void renderInput(GameEngine engine, int playerID) {}

		@Override
		public boolean onExcellent(GameEngine engine, int playerID) {
			onExcellentCalled = true;
			return onExcellentResult;
		}
	}
}
