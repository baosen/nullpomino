package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Covers edge-case branches in {@link Field}.
 */
class FieldEdgeCoverageTest {

	@Test
	void checkLineColorSkipsNoneAndInvalidColors() {
		Field f = new Field(10, 20, 3, false);
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_NONE);
		f.setBlockColor(1, 0, Block.BLOCK_COLOR_INVALID);
		f.setBlockColor(2, 0, Block.BLOCK_COLOR_RED);
		int total = f.checkLineColor(3, false, false, false);
		assertEquals(0, total);
	}

	@Test
	void gemClearColorSkipsNullBlocks() {
		Field f = new Field(10, 20, 3, false);
		f.setBlockColor(5, 5, Block.BLOCK_COLOR_GEM_RED);
		int total = f.gemClearColor(1, false, false);
		assertEquals(1, total);
	}

	@Test
	void clearColorReturnsZeroWhenBlockIsNull() {
		Field f = new Field(10, 20, 3, false);
		int result = f.clearColor(100, 100, false, false, false, false);
		assertEquals(0, result);
	}

	@Test
	void clearColorLastRowStillWorks() {
		// getBlock(x, y) returns a valid block for y = height-1
		// since it delegates to getBlockE which accesses the array directly.
		// The method should still function correctly for the last row.
		Field f = new Field(10, 20, 3, false);
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		int result = f.clearColor(5, 19, false, true, false, false);
		// The clear should succeed (return 1) and clear the block
		assertEquals(1, result);
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 19));
	}

	@Test
	void clearColorDecrementsHardOnAdjacentGarbageBlock() {
		Field f = new Field(10, 20, 3, false);
		Block garbage = new Block(Block.BLOCK_COLOR_RED);
		garbage.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		garbage.hard = 3;
		f.setBlockE(5, 5, garbage);
		f.setBlockColor(5, 6, Block.BLOCK_COLOR_RED);
		f.clearColor(5, 6, false, true, false, false);
		// The private clearColor decrements hard twice:
		// once in the garbage-handling section (b.hard--)
		// and once in the general block-handling section (b.hard-- again)
		assertEquals(1, f.getBlock(5, 5).hard);
	}

	@Test
	void clearColorRemovesAdjacentGarbageBlockWhenHardIsZero() {
		Field f = new Field(10, 20, 3, false);
		Block garbage = new Block(Block.BLOCK_COLOR_RED);
		garbage.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		garbage.hard = 0;
		f.setBlockE(5, 5, garbage);
		f.setBlockColor(5, 6, Block.BLOCK_COLOR_RED);
		f.clearColor(5, 6, false, true, false, false);
		// When hard==0 the private clearColor sets color to NONE
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 5));
	}

	@Test
	void attrStringToFieldHandlesMalformedInput() {
		Field f = new Field(10, 20, 3, false);
		f.attrStringToField("zz/zz;", 0);
		for (int x = 0; x < 10; x++)
			assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(x, 19));
	}

	@Test
	void attrStringToFieldHandlesEmptyRow() {
		Field f = new Field(10, 20, 3, false);
		f.attrStringToField(";;;", 0);
	}

	@Test
	void garbageDropFillsRandomColumnsWhenDropIsSmall() {
		GameEngine engine = makeEngine();
		Field f = new Field(10, 20, 3, false);
		f.garbageDrop(engine, 3, false, 0, 0, -1, Block.BLOCK_COLOR_GRAY);
		int count = 0;
		for (int x = 0; x < 10; x++)
			if (f.getBlockColor(x, -3) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(3, count);
	}

	@Test
	void garbageDropFillsMostColumnsWhenDropIsLarge() {
		GameEngine engine = makeEngine();
		Field f = new Field(10, 20, 3, false);
		f.garbageDrop(engine, 8, false, 0, 0, -1, Block.BLOCK_COLOR_GRAY);
		int count = 0;
		for (int x = 0; x < 10; x++)
			if (f.getBlockColor(x, -3) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(8, count);
	}

	@Test
	void garbageDropWithBigMode() {
		GameEngine engine = makeEngine();
		Field f = new Field(10, 20, 3, false);
		f.garbageDrop(engine, 10, true, 0, 0, -1, Block.BLOCK_COLOR_GRAY);
		// Big mode with drop=10 places 2 rows of big blocks.
		// Just verify that blocks were placed in multiple rows
		boolean hasVisible = false;
		for (int y = -3; y < 1; y++)
			for (int x = 0; x < 10; x++)
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE)
					hasVisible = true;
		assertTrue(hasVisible, "big mode should place blocks");
	}

	@Test
	void addRandomHoverBlocksFillThenRemoveLogic() {
		GameEngine engine = makeEngine();
		Field f = new Field(6, 8, 0, false);
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};
		f.addRandomHoverBlocks(engine, 30, colors, 0, false, false);
		int count = 0;
		for (int y = 0; y < 8; y++)
			for (int x = 0; x < 6; x++)
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(30, count);
	}

	@Test
	void addRandomHoverBlocksFlashModeAddsGems() {
		GameEngine engine = makeEngine();
		Field f = new Field(6, 8, 0, false);
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};
		f.addRandomHoverBlocks(engine, 24, colors, 0, true, true);
		boolean hasGem = false;
		for (int y = 0; y < 8; y++)
			for (int x = 0; x < 6; x++) {
				int c = f.getBlockColor(x, y);
				if (c >= Block.BLOCK_COLOR_GEM_RED && c <= Block.BLOCK_COLOR_GEM_PURPLE)
					hasGem = true;
			}
		assertTrue(hasGem);
	}

	@Test
	void shuffleColorsWithThreeColors() {
		Field f = new Field(10, 20, 3, false);
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 0, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(2, 0, Block.BLOCK_COLOR_GREEN);
		f.shuffleColors(new int[]{Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_RED}, 3, new java.util.Random(99));
	}

	@Test
	void gemColorCheckSkipsNullAndNonGemBlocks() {
		Field f = new Field(10, 20, 3, false);
		assertEquals(0, f.gemColorCheck(1, false, false, false));
	}

	@Test
	void freeFallReturnsFalseOnEmptyField() {
		Field f = new Field(10, 20, 3, false);
		assertFalse(f.freeFall());
	}

	@Test
	void freeFallReturnsTrueAfterDroppingBlocks() {
		Field f = new Field(10, 20, 3, false);
		f.setBlockColor(5, 0, Block.BLOCK_COLOR_RED);
		assertTrue(f.freeFall());
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 0));
	}

	@Test
	void attrStringToFieldTriggersCatchBlock() {
		// The catch block at lines 2392-2394 fires when attrStringToRow throws.
		// An empty cell string causes parseInt("") to throw NumberFormatException.
		Field f = new Field(10, 20, 0, false);
		// With hidden_height=0, i=-1 triggers the catch because the data doesn't
		// have enough rows, causing index+j >= strArray.length for all j,
		// then attrStringToRow gets empty strings which fail to parse.
		f.attrStringToField("", 0);
		// Should survive the catch blocks without exception.
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 0));
	}

	@Test
	void addRandomHoverBlocksWithPreExistingBlocksHitsPlaceBlockContinue() {
		// Line 2746: continue when !placeBlock[x][y-minY] in the while(!done) loop.
		// Set up pre-existing blocks on the field, then call addRandomHoverBlocks
		// with avoidLines=true so the balancing loop iterates over ALL cells.
		// Pre-existing blocks are not in placeBlock, so placeBlock[x][y-minY] is false.
		GameEngine engine = makeEngine();
		Field f = new Field(4, 4, 0, false);
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 0, Block.BLOCK_COLOR_BLUE);
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_GREEN};
		// Use count >= placeSize/2 so the else branch places many blocks.
		f.addRandomHoverBlocks(engine, 10, colors, 0, true, false);
		// Should not throw: pre-existing blocks are not in placeBlock,
		// so the balancing loop skips them (hit line 2746).
	}

	@Test
	void addRandomHoverBlocksWithUnknownColorHitsCIndexContinue() {
		// Line 2754: continue when cIndex == -1.
		// Place a block with a color NOT in the colors array, then call
		// addRandomHoverBlocks with avoidLines=true. The balancing loop
		// finds the pre-existing block, but its color is not in colors[],
		// so cIndex stays -1 and the continue at line 2754 fires.
		GameEngine engine = makeEngine();
		Field f = new Field(4, 4, 0, false);
		// BLOCK_COLOR_GRAY (1) is not in the colors array below.
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_GRAY);
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_GREEN};
		f.addRandomHoverBlocks(engine, 10, colors, 0, true, false);
		// Should not throw.
	}

	@Test
	void addRandomHoverBlocksBalancingLoopExcessRemoval() {
		// Exercise the balancing while(!done) loop including the removal
		// of excess blocks and the balance check (lines 2838-2839).
		// Use a small field with a heavily imbalanced initial placement
		// so the only way to balance is through the fill/switch path.
		GameEngine engine = makeEngine();
		Field f = new Field(4, 4, 0, false);
		// 12 blocks on 16 cells with 3 colors. After initial placement,
		// some colors will exceed maxCount=4. The balancing loop should
		// adjust colors and remove excess.
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_GREEN};
		f.addRandomHoverBlocks(engine, 12, colors, 0, true, false);
		int count = 0;
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(12, count);
	}

	@Test
	void addRandomHoverBlocksFlashModeGemLoopOutsideRange() {
		// Line 2856: gemNeeded[i] = false when color outside 2-8 range.
		// Use BLOCK_COLOR_GRAY (1) and BLOCK_COLOR_WHITE (9) so some colors
		// are outside the gem-mappable range (2-8).
		GameEngine engine = makeEngine();
		Field f = new Field(6, 6, 0, false);
		int[] colors = {Block.BLOCK_COLOR_GRAY, Block.BLOCK_COLOR_RED};
		// Use avoidLines=true and flashMode=true.
		f.addRandomHoverBlocks(engine, 18, colors, 0, true, true);
		// Should not throw.
	}

	@Test
	void addRandomHoverBlocksFlashModeGemLoopContinue() {
		// Line 2863: continue in the gem while loop when !placeBlock[x][y-minY].
		// The gem loop randomly selects cells; some will not be in placeBlock.
		GameEngine engine = makeEngine();
		Field f = new Field(4, 4, 0, false);
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};
		f.addRandomHoverBlocks(engine, 8, colors, 0, true, true);
		// Should not throw; the random cell selection in the gem loop
		// will eventually hit a placed cell.
		int count = 0;
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(8, count);
	}

	private static GameEngine makeEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}
}
