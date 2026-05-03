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

	private static GameEngine makeEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}
}
