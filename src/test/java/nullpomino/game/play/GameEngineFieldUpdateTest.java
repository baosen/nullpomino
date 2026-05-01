package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.event.EventReceiver;

/**
 * Pins GameEngine.fieldUpdate's per-block lock-flash, hidden-block,
 * and item-effect counters. fieldUpdate runs every frame; the
 * elapsed-frame increment, darkness handover from lock flash to
 * outline mode, and the X-RAY / COLOR / hebo-hidden timer resets
 * are all consumer-visible state and must keep working.
 */
class GameEngineFieldUpdateTest {

	private static GameEngine newEngineWithField() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine eng = gm.engine[0];
		eng.init();
		eng.field = new Field(10, 20, 3, false);
		return eng;
	}

	@Test
	void fieldUpdateIsNoOpWhenFieldIsNull() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		GameEngine eng = gm.engine[0];
		eng.init();

		// Should not throw or otherwise change state when field is null.
		eng.fieldUpdate();
	}

	@Test
	void fieldUpdateIncrementsElapsedFramesOnColoredBlocks() {
		GameEngine eng = newEngineWithField();
		eng.field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		Block b = eng.field.getBlock(0, 19);
		b.elapsedFrames = 0;

		eng.fieldUpdate();

		assertEquals(1, b.elapsedFrames);
	}

	@Test
	void fieldUpdateLeavesNegativeElapsedFramesAlone() {
		GameEngine eng = newEngineWithField();
		eng.field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		Block b = eng.field.getBlock(0, 19);
		b.elapsedFrames = -1;

		eng.fieldUpdate();

		assertEquals(-1, b.elapsedFrames,
				"negative elapsedFrames marks pre-lock blocks; must not increment");
	}

	@Test
	void fieldUpdateAppliesLockFlashDarknessWithinFlashWindow() {
		GameEngine eng = newEngineWithField();
		eng.ruleopt.lockflash = 5;
		eng.field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		Block b = eng.field.getBlock(0, 19);
		b.elapsedFrames = 1;
		b.darkness = 0f;

		eng.fieldUpdate();

		assertEquals(-0.8f, b.darkness, 0.001f,
				"during lockflash window, darkness goes to -0.8");
	}

	@Test
	void fieldUpdateClearsDarknessAndSetsOutlineAfterLockFlash() {
		GameEngine eng = newEngineWithField();
		eng.ruleopt.lockflash = 5;
		eng.field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		Block b = eng.field.getBlock(0, 19);
		b.elapsedFrames = 10;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE, false);

		eng.fieldUpdate();

		assertEquals(0f, b.darkness, 0.001f);
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE),
				"after lockflash, OUTLINE must be on");
	}

	@Test
	void fieldUpdateResetsItemCountersWhenItemsDisabled() {
		GameEngine eng = newEngineWithField();
		eng.gameActive = true;
		eng.itemXRayCount = 7;
		eng.itemColorCount = 13;
		eng.itemXRayEnable = false;
		eng.itemColorEnable = false;

		eng.fieldUpdate();

		assertEquals(0, eng.itemXRayCount, "X-RAY off → counter reset to 0");
		assertEquals(0, eng.itemColorCount, "COLOR off → counter reset to 0");
	}

	@Test
	void fieldUpdateAdvancesXRayCounterWhenEnabledAndActive() {
		GameEngine eng = newEngineWithField();
		eng.gameActive = true;
		eng.itemXRayEnable = true;
		eng.itemXRayCount = 0;

		eng.fieldUpdate();

		assertEquals(1, eng.itemXRayCount);
	}

	@Test
	void fieldUpdateAdvancesItemColorCounterWhenEnabledAndActive() {
		GameEngine eng = newEngineWithField();
		eng.gameActive = true;
		eng.itemColorEnable = true;
		eng.itemColorCount = 0;

		eng.fieldUpdate();

		assertEquals(1, eng.itemColorCount);
	}

	@Test
	void fieldUpdateAdvancesHeboHiddenTimerWhileActive() {
		GameEngine eng = newEngineWithField();
		eng.gameActive = true;
		eng.heboHiddenEnable = true;
		eng.heboHiddenTimerNow = 0;
		eng.heboHiddenTimerMax = 100;

		eng.fieldUpdate();

		assertEquals(1, eng.heboHiddenTimerNow);
	}

	@Test
	void fieldUpdateAdvancesHeboHiddenYWhenTimerExpires() {
		GameEngine eng = newEngineWithField();
		eng.gameActive = true;
		eng.heboHiddenEnable = true;
		eng.heboHiddenTimerNow = 5;
		eng.heboHiddenTimerMax = 5;
		eng.heboHiddenYNow = 0;
		eng.heboHiddenYLimit = 10;

		eng.fieldUpdate();

		assertEquals(0, eng.heboHiddenTimerNow, "timer rolls over after max");
		assertEquals(1, eng.heboHiddenYNow);
	}

	@Test
	void fieldUpdateClampsHeboHiddenYToLimit() {
		GameEngine eng = newEngineWithField();
		eng.gameActive = true;
		eng.heboHiddenEnable = true;
		eng.heboHiddenTimerNow = 5;
		eng.heboHiddenTimerMax = 5;
		eng.heboHiddenYNow = 10;
		eng.heboHiddenYLimit = 10;

		eng.fieldUpdate();

		assertEquals(10, eng.heboHiddenYNow,
				"already at limit → must not exceed");
	}

	@Test
	void fieldUpdateOwBlockShowOutlineOnlyOverrideForcesOff() {
		GameEngine eng = newEngineWithField();
		eng.ruleopt.lockflash = 5;
		eng.blockShowOutlineOnly = true;
		eng.owBlockShowOutlineOnly = 0; // override → off
		eng.field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		Block b = eng.field.getBlock(0, 19);
		b.elapsedFrames = 10;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);

		eng.fieldUpdate();

		// With override forcing outlineOnly=false, VISIBLE must remain on
		// after the post-lockflash branch.
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
	}

	@Test
	void fieldUpdateOwBlockShowOutlineOnlyOverrideForcesOnHidesBlock() {
		GameEngine eng = newEngineWithField();
		eng.ruleopt.lockflash = 5;
		eng.owBlockShowOutlineOnly = 1; // override → on
		eng.field.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		Block b = eng.field.getBlock(0, 19);
		b.elapsedFrames = 10;
		b.setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);

		eng.fieldUpdate();

		assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"outlineOnly forced on → VISIBLE cleared in post-lockflash branch");
	}
}
