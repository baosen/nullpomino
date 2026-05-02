package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.mode.AbstractMode;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GameEngine#statLockFlash}'s observable contract: the
 * mode override short-circuit, the per-frame statc[0] advance, and
 * the two-way transition at the ruleopt.lockflash limit (LINECLEAR
 * when lines are pending, ARE otherwise).
 */
class GameEngineStatLockFlashTest {

	@Test
	void modeOverrideShortCircuitsBeforeStatcAdvance() {
		BlockingMode mode = new BlockingMode();
		mode.onLockFlashResult = true;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.ruleopt.lockflash = 5;
		engine.statc[0] = 1;
		engine.stat = GameEngine.Status.LOCKFLASH;

		engine.statLockFlash();

		assertTrue(mode.onLockFlashCalled);
		assertEquals(1, engine.statc[0],
				"mode override returning true must skip statc[0]++");
		assertEquals(GameEngine.Status.LOCKFLASH, engine.stat);
	}

	@Test
	void normalFrameAdvancesStatcAndStaysInLockFlashUntilLimit() {
		// statc[0] = 0, ruleopt.lockflash = 3: each call bumps statc[0]
		// by 1 and stays in LOCKFLASH until the third call hits the
		// lockflash limit and transitions out.
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.ruleopt.lockflash = 3;
		engine.statc[0] = 0;
		engine.stat = GameEngine.Status.LOCKFLASH;
		engine.lineClearing = 0;

		engine.statLockFlash();
		assertEquals(1, engine.statc[0]);
		assertEquals(GameEngine.Status.LOCKFLASH, engine.stat);

		engine.statLockFlash();
		assertEquals(2, engine.statc[0]);
		assertEquals(GameEngine.Status.LOCKFLASH, engine.stat);

		// Third call: statc[0]++ to 3 -> hits limit -> transition to ARE
		// (lineClearing == 0) and reset statc.
		engine.statLockFlash();
		assertEquals(GameEngine.Status.ARE, engine.stat);
		assertEquals(0, engine.statc[0], "transition resets statc");
	}

	@Test
	void hittingLockflashLimitWithoutLineClearingTransitionsToAre() {
		// statc[0] = lockflash - 1: one more call hits the limit and
		// transitions to ARE with statc[1] = getARE().
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.ruleopt.lockflash = 1;
		engine.statc[0] = 0;
		engine.stat = GameEngine.Status.LOCKFLASH;
		engine.lineClearing = 0;
		// Set ARE to a recognisable value so we can confirm getARE() ran.
		engine.speed.are = 25;

		engine.statLockFlash();

		assertEquals(GameEngine.Status.ARE, engine.stat);
		assertEquals(25, engine.statc[1],
				"transition to ARE must seed statc[1] with getARE()");
	}

	@Test
	void hittingLockflashLimitWithLineClearingTransitionsToLineClear() {
		// lineClearing > 0: when the lockflash limit is hit, transition
		// to LINECLEAR (statLineClear is called recursively).
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = null;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.createFieldIfNeeded();
		engine.ruleopt.lockflash = 1;
		engine.statc[0] = 0;
		engine.stat = GameEngine.Status.LOCKFLASH;
		engine.lineClearing = 1;
		engine.speed.lineDelay = 0;

		engine.statLockFlash();

		// statLineClear progresses past LINECLEAR on its own once the
		// embedded line-delay is exceeded, but at minimum the transition
		// must have flipped stat away from LOCKFLASH.
		assertFalse(engine.stat == GameEngine.Status.LOCKFLASH,
				"hitting the lockflash limit with lineClearing > 0 must "
						+ "leave LOCKFLASH");
	}

	@Test
	void modeOverrideFalseFallsThroughToReceiverAndAdvancesStatc() {
		BlockingMode mode = new BlockingMode();
		mode.onLockFlashResult = false;
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];
		engine.ruleopt.lockflash = 100;
		engine.statc[0] = 0;
		engine.stat = GameEngine.Status.LOCKFLASH;

		engine.statLockFlash();

		assertTrue(mode.onLockFlashCalled);
		assertEquals(1, engine.statc[0],
				"mode returning false must let the engine advance statc");
	}

	private static final class BlockingMode extends AbstractMode {
		boolean onLockFlashCalled;
		boolean onLockFlashResult;

		@Override
		public String getName() { return "stub"; }

		@Override
		public void modeInit(GameManager manager) {}

		@Override
		public void playerInit(GameEngine engine, int playerID) {}

		@Override
		public void renderInput(GameEngine engine, int playerID) {}

		@Override
		public boolean onLockFlash(GameEngine engine, int playerID) {
			onLockFlashCalled = true;
			return onLockFlashResult;
		}
	}
}
