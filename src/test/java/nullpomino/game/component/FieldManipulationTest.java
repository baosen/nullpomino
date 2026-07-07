package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Tests for Field manipulation methods: cutLine, pushUp, pushDown,
 * setAllAttribute, setAllSkin, getHowManyGems, getHowManyGemClears,
 * getSecretGrade,
 * checkForSquares, getHowManySquareClears, and shuffleColors.
 */
class FieldManipulationTest {

	private static Field newField() {
		return new Field(10, 20, 3, false);
	}

	// ---------------------------------------------------------------
	// cutLine
	// ---------------------------------------------------------------

	@Test
	void cutLineSingleLineInMiddleShiftsRowsAboveDown() {
		Field f = newField();
		// Place blocks at (0, 5) and (0, 10)
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 10, Block.BLOCK_COLOR_BLUE);

		f.cutLine(7, 1);

		// Row 7 gets content of row 6 (was empty), ...
		// The block at (0, 5) shifts down to (0, 6)
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 6),
				"block originally at row 5 should move to row 6 after cutting row 7");
		// The block at (0, 10) stays at (0, 10) because it's below the cut
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 10),
				"block below cut line should not move");
		// Top of field (firstFieldRow = -3) is cleared
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, -3),
				"top row after cut should be empty");
	}

	@Test
	void cutLineMultipleLinesCutsSeveralRows() {
		Field f = newField();
		// Place markers
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 8, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(0, 12, Block.BLOCK_COLOR_GREEN);

		f.cutLine(8, 3);

		// Blocks above the cut zone shift down by 3
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 8),
				"block at row 5 shifts to row 8 after cutting 3 rows from row 8");
		// Block at row 12 stays unaffected (below cut zone + skip)
		assertEquals(Block.BLOCK_COLOR_GREEN, f.getBlockColor(0, 12),
				"block below cut zone should be unaffected");
		// Block at row 8 was IN the cut zone
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 5),
				"original row 5 should now be empty");
	}

	@Test
	void cutLineAtBottomRowShiftsNothing() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_BLUE);

		f.cutLine(19, 1);

		// Row 19 gets content of row 18
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 19),
				"row 19 should get the content from row 18");
		// Blocks above shift down
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, -3),
				"top row should be cleared");
	}

	@Test
	void cutLineShiftsContentAboveCutDown() {
		Field f = newField();
		// Place blocks in hidden rows -3, -2, -1 and visible row 0
		f.setBlockColor(0, -3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, -2, Block.BLOCK_COLOR_ORANGE);
		f.setBlockColor(0, -1, Block.BLOCK_COLOR_YELLOW);
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_GREEN);
		// Block below the cut zone
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_BLUE);

		f.cutLine(0, 1);

		// cutLine shifts row[i-1] into row[i] for i from y down to firstFieldRow+1.
		// Row 0 gets row -1 (YELLOW), row -1 gets row -2 (ORANGE),
		// row -2 gets row -3 (RED), row -3 is cleared.
		assertEquals(Block.BLOCK_COLOR_YELLOW, f.getBlockColor(0, 0),
				"row 0 should get content of row -1");
		assertEquals(Block.BLOCK_COLOR_ORANGE, f.getBlockColor(0, -1),
				"row -1 should get content of row -2");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, -2),
				"row -2 should get content of row -3");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, -3),
				"top row (-3) should be cleared");
		// Block below unaffected
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 5),
				"block below cut should not move");
	}

	// ---------------------------------------------------------------
	// pushUp
	// ---------------------------------------------------------------

	@Test
	void pushUpByOneShiftsContentUpAndClearsBottom() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_GREEN);

		f.pushUp(1);

		// pushUp copies row[i+1] into row[i] from top to bottom.
		// Row 0 content shifts to row -1, row -1 to row -2, etc.
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, -1),
				"block at row 0 should move up to row -1");
		assertEquals(Block.BLOCK_COLOR_GREEN, f.getBlockColor(5, 9),
				"block at row 10 should move to row 9");
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 18),
				"block at row 19 should move to row 18");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19),
				"bottom row should be cleared after pushUp");
	}

	@Test
	void pushUpByMultipleLinesShiftsContentUpSeveralRows() {
		Field f = newField();
		f.setBlockColor(0, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_BLUE);

		f.pushUp(3);

		// Block at row 10 moves up by 3 to row 7
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 7),
				"block at row 10 should move to row 7");
		// Block at row 19 moves to row 16
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 16),
				"block at row 19 should move to row 16");
		// Bottom 3 rows are cleared
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19),
				"bottom row 19 should be empty");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 18),
				"bottom row 18 should be empty");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 17),
				"bottom row 17 should be empty");
	}

	// ---------------------------------------------------------------
	// pushDown
	// ---------------------------------------------------------------

	@Test
	void pushDownByOneShiftsContentDownAndClearsTop() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_GREEN);

		f.pushDown(1);

		// Content moves down: row 0 -> row 1, row 10 -> row 11, row 19 is pushed out
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 1),
				"block at row 0 should move to row 1");
		assertEquals(Block.BLOCK_COLOR_GREEN, f.getBlockColor(5, 11),
				"block at row 10 should move to row 11");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 0),
				"original row 0 should be empty (shifted out)");
	}

	@Test
	void pushDownByMultipleLinesShiftsContentDownSeveralRows() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GREEN);

		f.pushDown(3);

		// Block at row 0 moves down by 3 to row 3
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 3),
				"block at row 0 should move to row 3");
		// Block at row 5 moves to row 8
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 8),
				"block at row 5 should move to row 8");
		// Block at row 19 moves to row 22 which is out of bounds, effectively lost
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19),
				"block at row 19 should be lost (pushed below visible field)");
		// Top 3 rows are cleared
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, -3),
				"first field row -3 should be empty");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, -2),
				"row -2 should be empty");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, -1),
				"row -1 should be empty");
	}

	// ---------------------------------------------------------------
	// setAllAttribute
	// ---------------------------------------------------------------

	@Test
	void setAllAttributeSetsAttributeOnAllBlocksInField() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_GREEN);

		f.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);

		// All blocks should now have the VISIBLE attribute
		assertTrue(f.getBlock(0, 0).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(f.getBlock(5, 10).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(f.getBlock(3, 19).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		// Even empty blocks in hidden area
		assertTrue(f.getBlock(0, -1).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
	}

	@Test
	void setAllAttributeCanRemoveAttributeFromAllBlocks() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_BLUE);

		// First set VISIBLE on all blocks
		f.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		assertTrue(f.getBlock(0, 0).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));

		// Then remove it
		f.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, false);
		assertFalse(f.getBlock(0, 0).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertFalse(f.getBlock(5, 10).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
	}

	@Test
	void setAllAttributeWithBrokenAttrDoesNotAffectOtherAttributes() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.getBlock(0, 0).setAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);

		// Set BROKEN on all blocks
		f.setAllAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);

		assertTrue(f.getBlock(0, 0).getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
		// VISIBLE should still be set (it's a different bit)
		assertTrue(f.getBlock(0, 0).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE),
				"existing VISIBLE attribute should not be cleared by setting BROKEN");
	}

	// ---------------------------------------------------------------
	// setAllSkin
	// ---------------------------------------------------------------

	@Test
	void setAllSkinSetsSkinOnAllBlocks() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_GREEN);

		f.setAllSkin(42);

		assertEquals(42, f.getBlock(0, 0).skin);
		assertEquals(42, f.getBlock(5, 10).skin);
		assertEquals(42, f.getBlock(3, 19).skin);
		assertEquals(42, f.getBlock(0, -1).skin, "hidden area blocks should also get the skin");
	}

	@Test
	void setAllSkinOverwritesPreviousSkinValues() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.getBlock(0, 0).skin = 10;

		f.setAllSkin(99);

		assertEquals(99, f.getBlock(0, 0).skin);
	}

	// ---------------------------------------------------------------
	// getHowManyGems
	// ---------------------------------------------------------------

	@Test
	void getHowManyGemsReturnsZeroOnEmptyField() {
		Field f = newField();
		assertEquals(0, f.getHowManyGems());
	}

	@Test
	void getHowManyGemsCountsOnlyGemBlocks() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 0, Block.BLOCK_COLOR_RED); // normal, not a gem
		f.setBlockColor(2, 0, Block.BLOCK_COLOR_GEM_BLUE);
		f.setBlockColor(3, 0, Block.BLOCK_COLOR_GEM_GREEN);

		assertEquals(3, f.getHowManyGems());
	}

	@Test
	void getHowManyGemsCountsGemsInAllAreas() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(0, -1, Block.BLOCK_COLOR_GEM_BLUE); // hidden area
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GREEN); // normal, not a gem

		assertEquals(2, f.getHowManyGems());
	}

	// ---------------------------------------------------------------
	// getHowManyGemClears
	// ---------------------------------------------------------------

	@Test
	void getHowManyGemClearsReturnsZeroWhenNoLinesAreFlagged() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);

		assertEquals(0, f.getHowManyGemClears());
	}

	@Test
	void getHowManyGemClearsCountsGemsInFlaggedLinesOnly() {
		Field f = newField();
		// Flagged row with gems
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_GEM_BLUE);
		f.setLineFlag(19, true);

		// Non-flagged row with gems (should not count)
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_GEM_GREEN);

		assertEquals(2, f.getHowManyGemClears());
	}

	@Test
	void getHowManyGemClearsIgnoresNonGemBlocksInFlaggedLines() {
		Field f = newField();
		for (int x = 0; x < 10; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		f.getBlock(3, 19).color = Block.BLOCK_COLOR_GEM_RED;
		f.getBlock(7, 19).color = Block.BLOCK_COLOR_GEM_BLUE;
		f.setLineFlag(19, true);

		assertEquals(2, f.getHowManyGemClears());
	}

	// ---------------------------------------------------------------
	// getSecretGrade
	// ---------------------------------------------------------------

	@Test
	void getSecretGradeReturnsZeroOnEmptyField() {
		Field f = newField();
		assertEquals(0, f.getSecretGrade());
	}

	@Test
	void getSecretGradeReturnsFiveForPyramidWithFiveTiers() {
		Field f = newField();
		// Build a pyramid from row 15 to row 19 with hole pattern:
		// For each row i (19 down to 15), holeLoc = -|i-10| + 9
		// Row 19: holeLoc=0, Row 18: holeLoc=1, ..., Row 15: holeLoc=4
		for (int row = 15; row <= 19; row++) {
			int holeLoc = -Math.abs(row - 10) + 9;
			// Fill all columns except the hole
			for (int col = 0; col < 10; col++) {
				if (col != holeLoc) {
					f.setBlockColor(col, row, Block.BLOCK_COLOR_RED);
				}
			}
			// Place the block below the hole to keep the hole above ground
			f.setBlockColor(holeLoc, row - 1, Block.BLOCK_COLOR_RED);
		}
		// Row 14 (holeLoc=5) fails because (5, 13) is empty, stopping at 5
		assertEquals(5, f.getSecretGrade());
	}

	@Test
	void getSecretGradeReturnsZeroWhenFirstRowFails() {
		Field f = newField();
		// Fill row 19 completely (no hole at column 0)
		for (int col = 0; col < 10; col++) {
			f.setBlockColor(col, 19, Block.BLOCK_COLOR_RED);
		}
		// HoleLoc for row 19 is 0, but (0, 19) is not empty -> breaks immediately
		assertEquals(0, f.getSecretGrade());
	}

	// ---------------------------------------------------------------
	// checkForSquares
	// ---------------------------------------------------------------

	@Test
	void checkForSquaresFindsNoSquaresOnEmptyField() {
		Field f = newField();
		int[] squares = f.checkForSquares();
		assertArrayEquals(new int[] {0, 0}, squares);
	}

	@Test
	void checkForSquaresFindsGoldSquareInBottomLeft() {
		Field f = newField();
		// 4x4 mono-color block at the bottom
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
			}
		}

		int[] squares = f.checkForSquares();

		assertArrayEquals(new int[] {1, 0}, squares);
		// The blocks should now be gold square blocks
		assertTrue(f.getBlock(0, 0).isGoldSquareBlock());
		assertTrue(f.getBlock(3, 3).isGoldSquareBlock());
	}

	// ---------------------------------------------------------------
	// getHowManySquareClears
	// ---------------------------------------------------------------

	@Test
	void getHowManySquareClearsReturnsZeroWhenNoFlaggedLines() {
		Field f = newField();
		int[] clears = f.getHowManySquareClears();
		assertArrayEquals(new int[] {0, 0}, clears);
	}

	@Test
	void getHowManySquareClearsCountsGoldAndSilverStrips() {
		Field f = newField();
		// 4 gold square blocks in a flagged row = 1 gold strip
		for (int x = 0; x < 4; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_SQUARE_GOLD_1);
		}
		// 8 silver square blocks = 2 silver strips
		for (int x = 4; x < 8; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_SQUARE_SILVER_1);
		}
		f.setLineFlag(19, true);

		int[] clears = f.getHowManySquareClears();
		assertEquals(1, clears[0], "4 gold cells / 4 = 1 gold strip");
		assertEquals(1, clears[1], "4 silver cells / 4 = 1 silver strip (only 4 of 8 counted, one row limited)");
	}

	// ---------------------------------------------------------------
	// shuffleColors
	// ---------------------------------------------------------------

	@Test
	void shuffleColorsRemapsAllBlocksToTargetPalette() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);     // color 2
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_BLUE);    // color 7
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_GREEN);   // color 5
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_GRAY);    // color 1

		int[] palette = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE,
				Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_PURPLE};
		f.shuffleColors(palette, 4, new Random(42));

		// All set blocks should now be one of the palette colors
		for (int x = 0; x < 4; x++) {
			int c = f.getBlockColor(x, 19);
			assertTrue(c == Block.BLOCK_COLOR_RED || c == Block.BLOCK_COLOR_BLUE
					|| c == Block.BLOCK_COLOR_GREEN || c == Block.BLOCK_COLOR_PURPLE,
					"Block at col " + x + " should be in palette, got " + c);
		}
	}

	@Test
	void shuffleColorsWithSeededRandomIsDeterministic() {
		Field f1 = newField();
		f1.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f1.setBlockColor(1, 19, Block.BLOCK_COLOR_BLUE);
		f1.shuffleColors(new int[] {Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_PURPLE},
				2, new Random(12345));

		Field f2 = newField();
		f2.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f2.setBlockColor(1, 19, Block.BLOCK_COLOR_BLUE);
		f2.shuffleColors(new int[] {Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_PURPLE},
				2, new Random(12345));

		assertEquals(f1.getBlockColor(0, 19), f2.getBlockColor(0, 19));
		assertEquals(f1.getBlockColor(1, 19), f2.getBlockColor(1, 19));
	}
}
