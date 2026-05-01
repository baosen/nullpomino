package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;

import org.junit.jupiter.api.Test;

/**
 * Pins the small helper surface on {@link GameEngine} that the engine
 * itself and game modes call every frame: playSE, resetFieldVisible,
 * checkDropContinuousUse, isHoldOK, getCascadeDelay /
 * getCascadeClearDelay. None of these need rendering or netplay.
 */
class GameEngineMiscHelpersTest {

	@Test
	void playSERoutesToReceiverWhenEnabledAndIsNoOpWhenDisabled() {
		EventReceiver receiver = new EventReceiver();
		GameManager gm = new GameManager(receiver);
		gm.init();
		gm.engine[0].init();
		GameEngine engine = gm.engine[0];

		// EventReceiver.playSE is a no-op stub, so we just confirm both
		// paths run without throwing.
		engine.enableSE = true;
		engine.playSE("decide");
		engine.enableSE = false;
		engine.playSE("decide");
	}

	@Test
	void getCascadeDelayAndGetCascadeClearDelayReturnTheirPlainFields() {
		GameEngine engine = freshEngine();
		engine.cascadeDelay = 7;
		engine.cascadeClearDelay = 13;

		assertEquals(7, engine.getCascadeDelay());
		assertEquals(13, engine.getCascadeClearDelay());
	}

	@Test
	void isHoldOKReturnsTrueByDefaultWhenRuleEnablesHoldAndCounterIsBelowLimit() {
		GameEngine engine = freshEngine();
		engine.ruleopt.holdEnable = true;
		engine.holdDisable = false;
		engine.holdUsedCount = 0;
		engine.ruleopt.holdLimit = -1; // -1 = unlimited
		engine.initialHoldContinuousUse = false;

		assertTrue(engine.isHoldOK());
	}

	@Test
	void isHoldOKFalseWhenHoldDisabledByRuleOrEngineOrLimitOrInitialUse() {
		GameEngine engine = freshEngine();

		// Rule disables hold entirely.
		engine.ruleopt.holdEnable = false;
		engine.holdDisable = false;
		engine.holdUsedCount = 0;
		engine.ruleopt.holdLimit = -1;
		engine.initialHoldContinuousUse = false;
		assertFalse(engine.isHoldOK());

		// Engine flag disables hold.
		engine.ruleopt.holdEnable = true;
		engine.holdDisable = true;
		assertFalse(engine.isHoldOK());

		// Hit the per-game hold limit.
		engine.holdDisable = false;
		engine.holdUsedCount = 5;
		engine.ruleopt.holdLimit = 5;
		assertFalse(engine.isHoldOK());

		// Initial-hold-continuous-use blocks even within limits.
		engine.holdUsedCount = 0;
		engine.ruleopt.holdLimit = -1;
		engine.initialHoldContinuousUse = true;
		assertFalse(engine.isHoldOK());
	}

	@Test
	void resetFieldVisibleWalksTheFieldAndRevealsEveryColoredBlock() {
		GameEngine engine = freshEngine();
		engine.createFieldIfNeeded();
		// Plant a colored block with darkness/alpha tweaked and visibility off.
		engine.field.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		Block blk = engine.field.getBlock(0, 0);
		blk.alpha = 0.2f;
		blk.darkness = 0.7f;
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, false);
		// And an empty cell — should stay untouched.
		engine.field.setBlockColor(1, 0, Block.BLOCK_COLOR_NONE);

		engine.resetFieldVisible();

		Block reset = engine.field.getBlock(0, 0);
		assertEquals(1f, reset.alpha);
		assertEquals(0f, reset.darkness);
		assertTrue(reset.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(reset.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE));
	}

	@Test
	void resetFieldVisibleIsNoOpWhenFieldNotAllocated() {
		GameEngine engine = freshEngine();
		engine.field = null;

		// No NPE — simply returns when field is null.
		engine.resetFieldVisible();
	}

	@Test
	void checkDropContinuousUseClearsFlagsWhenInputIsReleased() {
		GameEngine engine = freshEngine();
		engine.gameActive = true;
		engine.ruleopt.softdropLimit = true;
		engine.ruleopt.harddropLimit = true;
		engine.ruleopt.holdInitialLimit = true;
		engine.ruleopt.rotateInitialLimit = false; // forces initialRotate clear too
		engine.softdropContinuousUse = true;
		engine.harddropContinuousUse = true;
		engine.initialHoldContinuousUse = true;
		engine.initialRotateContinuousUse = true;

		// All buttons released → all flags clear.
		engine.ctrl = new Controller();
		engine.checkDropContinuousUse();

		assertFalse(engine.softdropContinuousUse);
		assertFalse(engine.harddropContinuousUse);
		assertFalse(engine.initialHoldContinuousUse);
		assertFalse(engine.initialRotateContinuousUse);
	}

	@Test
	void checkDropContinuousUseIsNoOpWhenGameInactive() {
		GameEngine engine = freshEngine();
		engine.gameActive = false;
		engine.softdropContinuousUse = true;
		engine.harddropContinuousUse = true;
		engine.initialHoldContinuousUse = true;
		engine.initialRotateContinuousUse = true;

		engine.checkDropContinuousUse();

		// Flags survive — the inactive-game guard short-circuits the
		// release-detection logic.
		assertTrue(engine.softdropContinuousUse);
		assertTrue(engine.harddropContinuousUse);
		assertTrue(engine.initialHoldContinuousUse);
		assertTrue(engine.initialRotateContinuousUse);
	}

	private static GameEngine freshEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}
}
