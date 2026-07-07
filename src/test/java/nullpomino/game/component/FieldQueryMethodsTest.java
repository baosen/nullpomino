package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for Field query methods. Focuses on {@link Field#isEmptyLine(int)}
 * which had minimal coverage, plus edge-case scenarios for other query
 * methods to ensure correctness across boundary conditions.
 */
class FieldQueryMethodsTest {

	private static Field newField() {
		return new Field(10, 20, 3, false);
	}

	// ================================================================
	// isEmptyLine(int y)
	// ================================================================

	@Test
	void isEmptyLineReturnsTrueForFreshlyClearedLine() {
		Field f = newField();
		assertTrue(f.isEmptyLine(0));
		assertTrue(f.isEmptyLine(10));
		assertTrue(f.isEmptyLine(19));
	}

	@Test
	void isEmptyLineReturnsFalseForFullyFilledLine() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 5, Block.BLOCK_COLOR_RED);
		}
		assertFalse(f.isEmptyLine(5));
	}

	@Test
	void isEmptyLineReturnsFalseForPartiallyFilledLine() {
		Field f = newField();
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(9, 5, Block.BLOCK_COLOR_BLUE);
		assertFalse(f.isEmptyLine(5));
	}

	@Test
	void isEmptyLineReturnsFalseForSingleBlockInLine() {
		Field f = newField();
		f.setBlockColor(5, 7, Block.BLOCK_COLOR_GREEN);
		assertFalse(f.isEmptyLine(7));
	}

	@Test
	void isEmptyLineReturnsTrueForLineWithAllButOneCellFilled() {
		Field f = newField();
		for (int x = 1; x < f.getWidth(); x++) {
			f.setBlockColor(x, 12, Block.BLOCK_COLOR_RED);
		}
		assertFalse(f.isEmptyLine(12),
				"a line missing one cell is not empty");
	}

	@Test
	void isEmptyLineReturnsTrueForHiddenAreaEmptyLines() {
		Field f = newField();
		assertTrue(f.isEmptyLine(-1));
		assertTrue(f.isEmptyLine(-2));
		assertTrue(f.isEmptyLine(-3));
	}

	@Test
	void isEmptyLineReturnsTrueForOutOfBoundsY() {
		Field f = newField();
		assertTrue(f.isEmptyLine(f.getHeight()));
		assertTrue(f.isEmptyLine(-999));
	}

	@Test
	void isEmptyLineReturnsFalseForLineWithAllGemBlocks() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 10, Block.BLOCK_COLOR_GEM_RED);
		}
		assertFalse(f.isEmptyLine(10));
	}

	@Test
	void isEmptyLineReturnsFalseForLineWithWallAttributeBlocks() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 15, Block.BLOCK_COLOR_GRAY);
			f.getBlock(x, 15).setAttribute(Block.BLOCK_ATTRIBUTE_WALL, true);
		}
		assertFalse(f.isEmptyLine(15));
	}

	@Test
	void isEmptyLineReturnsFalseForLineWithEraseAttributeBlocks() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 14, Block.BLOCK_COLOR_RED);
			f.getBlock(x, 14).setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		}
		// BLOCK_ATTRIBUTE_ERASE does not affect isEmptyLine — block still has color
		assertFalse(f.isEmptyLine(14));
	}

	@Test
	void isEmptyLineReflectsLatestSetBlockColor() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 8, Block.BLOCK_COLOR_RED);
		}
		assertFalse(f.isEmptyLine(8));

		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 8, Block.BLOCK_COLOR_NONE);
		}
		assertTrue(f.isEmptyLine(8));
	}

	// ================================================================
	// getHighestBlockY() — edge cases
	// ================================================================

	@Test
	void getHighestBlockYReturnsHeightWhenAllRowsAreFlagged() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setLineFlag(19, true);
		assertEquals(f.getHeight(), f.getHighestBlockY());
	}

	@Test
	void getHighestBlockYIgnoresFlaggedRows() {
		Field f = newField();
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_BLUE);
		f.setLineFlag(5, true);
		assertEquals(19, f.getHighestBlockY());
	}

	@Test
	void getHighestBlockYConsidersHiddenAreaWhenCeilingIsAbsent() {
		Field f = newField();
		f.setBlockColor(0, -1, Block.BLOCK_COLOR_RED);
		assertEquals(-1, f.getHighestBlockY());
	}

	// ================================================================
	// getHighestBlockY(int x) — edge cases
	// ================================================================

	@Test
	void getHighestBlockYByColumnReturnsHeightForOutOfRangeX() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		assertEquals(f.getHeight(), f.getHighestBlockY(-1));
		assertEquals(f.getHeight(), f.getHighestBlockY(f.getWidth()));
	}

	@Test
	void getHighestBlockYByColumnReturnsHeightForEmptyColumn() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		assertEquals(f.getHeight(), f.getHighestBlockY(5));
	}

	@Test
	void getHighestBlockYByColumnIgnoresFlaggedRowsInColumn() {
		Field f = newField();
		f.setBlockColor(3, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_BLUE);
		f.setLineFlag(10, true);
		assertEquals(19, f.getHighestBlockY(3));
	}

	// ================================================================
	// isHoleBelow(int x, int y) — edge cases
	// ================================================================

	@Test
	void isHoleBelowReturnsTrueForBottomRowBecauseYPlusOneIsOutOfBounds() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		// y+1 = 20 is out of bounds → getBlockEmpty returns true → hole below
		assertTrue(f.isHoleBelow(0, 19));
	}

	@Test
	void isHoleBelowReturnsFalseForEmptyCell() {
		Field f = newField();
		assertFalse(f.isHoleBelow(5, 10));
	}

	@Test
	void isHoleBelowReturnsFalseWhenBothFilled() {
		Field f = newField();
		f.setBlockColor(3, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 11, Block.BLOCK_COLOR_RED);
		assertFalse(f.isHoleBelow(3, 10));
	}

	@Test
	void isHoleBelowReturnsFalseWhenBothEmpty() {
		Field f = newField();
		assertFalse(f.isHoleBelow(3, 10));
	}

	@Test
	void isHoleBelowReturnsTrueForFilledAboveEmpty() {
		Field f = newField();
		f.setBlockColor(3, 10, Block.BLOCK_COLOR_RED);
		assertTrue(f.isHoleBelow(3, 10));
	}

	// ================================================================
	// getValleyDepth(int x) — edge cases
	// ================================================================

	@Test
	void getValleyDepthReturnsZeroForFlatSurface() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		assertEquals(0, f.getValleyDepth(5));
	}

	@Test
	void getValleyDepthReturnsZeroForColumnWithoutBothWalls() {
		Field f = newField();
		f.setBlockColor(4, 19, Block.BLOCK_COLOR_RED);
		// Column 5 has a wall at col 4 but no wall at col 6 → no valley
		assertEquals(0, f.getValleyDepth(5));
	}

	@Test
	void getValleyDepthReturnsZeroWhenColumnHasBlockButNoWalls() {
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		assertEquals(0, f.getValleyDepth(5));
	}

	// ================================================================
	// getHowManyBlocksFromLeft() / getHowManyBlocksFromRight() — edge cases
	// ================================================================

	@Test
	void getHowManyBlocksFromLeftReturnsZeroForEmptyField() {
		Field f = newField();
		assertEquals(0, f.getHowManyBlocksFromLeft());
	}

	@Test
	void getHowManyBlocksFromLeftCountsFullRowCorrectly() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		assertEquals(f.getWidth(), f.getHowManyBlocksFromLeft());
	}

	@Test
	void getHowManyBlocksFromLeftSkipsFlaggedRows() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setLineFlag(19, true);
		assertEquals(1, f.getHowManyBlocksFromLeft());
	}

	@Test
	void getHowManyBlocksFromLeftStopsAtFirstGapInEachRow() {
		Field f = newField();
		// Row 19: blocks at 0,1,2 gap at 3 → counts 3
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		// Row 18: blocks at 0 only → counts 1
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_BLUE);

		assertEquals(4, f.getHowManyBlocksFromLeft());
	}

	@Test
	void getHowManyBlocksFromRightReturnsZeroForEmptyField() {
		Field f = newField();
		assertEquals(0, f.getHowManyBlocksFromRight());
	}

	@Test
	void getHowManyBlocksFromRightCountsFullRowExcludingColumnZero() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		// The loop is j > 0 (not j >= 0), so column 0 is excluded
		assertEquals(f.getWidth() - 1, f.getHowManyBlocksFromRight());
	}

	@Test
	void getHowManyBlocksFromRightStopsAtFirstGapInEachRow() {
		Field f = newField();
		// Row 19: blocks at 9,8,7 gap at 6 → counts 3
		f.setBlockColor(9, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(8, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 19, Block.BLOCK_COLOR_RED);
		// Row 18: block at 9 → counts 1
		f.setBlockColor(9, 18, Block.BLOCK_COLOR_BLUE);

		assertEquals(4, f.getHowManyBlocksFromRight());
	}

	// ================================================================
	// getHowManyHoles() — edge cases
	// ================================================================

	@Test
	void getHowManyHolesReturnsZeroForEmptyField() {
		Field f = newField();
		assertEquals(0, f.getHowManyHoles());
	}

	@Test
	void getHowManyHolesReturnsZeroForFlatBottom() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		assertEquals(0, f.getHowManyHoles());
	}

	@Test
	void getHowManyHolesCountsSingleColumnPit() {
		Field f = newField();
		// Column 0: filled at 17, empty at 18, empty at 19 → 2 holes
		f.setBlockColor(0, 17, Block.BLOCK_COLOR_RED);
		assertEquals(2, f.getHowManyHoles());
	}

	@Test
	void getHowManyHolesCountsAcrossMultipleColumns() {
		Field f = newField();
		// Column 0: filled at 17 → holes at 18,19 = 2
		f.setBlockColor(0, 17, Block.BLOCK_COLOR_RED);
		// Column 1: filled at 18 → hole at 19 = 1
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);

		assertEquals(3, f.getHowManyHoles());
	}

	// ================================================================
	// getHeightWithoutHurryupFloor()
	// ================================================================

	@Test
	void getHeightWithoutHurryupFloorEqualsHeightInitially() {
		Field f = newField();
		assertEquals(f.getHeight(), f.getHeightWithoutHurryupFloor());
	}

	@Test
	void hurryUpFloorLinesAccountedInHeightWithoutHurryupFloor() {
		Field f = newField();
		f.addHurryupFloor(3, 0);
		assertEquals(f.getHeight() - 3, f.getHeightWithoutHurryupFloor());
	}

	// ================================================================
	// freeFall() — edge cases
	// ================================================================

	@Test
	void freeFallReturnsFalseForAlreadySettledBlocks() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		assertFalse(f.freeFall());
	}

	@Test
	void freeFallReturnsFalseForEmptyField() {
		Field f = newField();
		assertFalse(f.freeFall());
	}

	@Test
	void freeFallHandlesMultipleColumnsIndependently() {
		Field f = newField();
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 10, Block.BLOCK_COLOR_BLUE);

		assertTrue(f.freeFall());

		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(1, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 5));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(1, 10));
	}

	@Test
	void freeFallLeavesSettledBlocksAtBottomAndStacksAbove() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 10, Block.BLOCK_COLOR_BLUE);

		assertTrue(f.freeFall());

		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 18));
	}

	@Test
	void freeFallStacksLowestBlockAtBottomWhenMultipleInSameColumn() {
		Field f = newField();
		// freeFall drops the lowest floating block (closest to bottom) first
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 7, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(0, 9, Block.BLOCK_COLOR_GREEN);

		assertTrue(f.freeFall());

		// Lowest original y (9) reaches bottom; highest (5) stacks on top
		assertEquals(Block.BLOCK_COLOR_GREEN, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 18));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 17));
	}

	// ================================================================
	// canCascade() — edge cases
	// ================================================================

	@Test
	void canCascadeReturnsFalseForEmptyField() {
		Field f = newField();
		assertFalse(f.canCascade());
	}

	@Test
	void canCascadeReturnsFalseForAntigravityBlocks() {
		Field f = newField();
		f.addHoverBlock(5, 10, Block.BLOCK_COLOR_RED);
		assertFalse(f.canCascade(), "antigravity blocks should be ignored by cascade");
	}

	@Test
	void canCascadeReturnsTrueForConnectedBlocksOverEmpty() {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 11, Block.BLOCK_COLOR_RED);
		f.getBlock(5, 10).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);
		f.getBlock(5, 11).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);
		assertTrue(f.canCascade());
	}

	@Test
	void canCascadeReturnsFalseForBlockOnFloor() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		assertFalse(f.canCascade());
	}

	// ================================================================
	// addHoverBlock(int x, int y, int color) — edge cases
	// ================================================================

	@Test
	void addHoverBlockReturnsFalseForOutOfBounds() {
		Field f = newField();
		assertFalse(f.addHoverBlock(-1, 0, Block.BLOCK_COLOR_RED));
		assertFalse(f.addHoverBlock(f.getWidth(), 0, Block.BLOCK_COLOR_RED));
		assertFalse(f.addHoverBlock(0, f.getHeight(), Block.BLOCK_COLOR_RED));
	}

	@Test
	void addHoverBlockOverwritesExistingBlockColor() {
		Field f = newField();
		f.setBlockColor(5, 5, Block.BLOCK_COLOR_RED);
		assertTrue(f.addHoverBlock(5, 5, Block.BLOCK_COLOR_BLUE));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(5, 5));
	}

	@Test
	void addHoverBlockSetsAntigravityAndBrokenFlags() {
		Field f = newField();
		assertTrue(f.addHoverBlock(3, 8, Block.BLOCK_COLOR_RED));

		Block b = f.getBlock(3, 8);
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
		assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_ERASE));
	}

	// ================================================================
	// checkLine() / checkLineNoFlag() — edge cases
	// ================================================================

	@Test
	void checkLineReturnsZeroForEmptyField() {
		Field f = newField();
		assertEquals(0, f.checkLine());
	}

	@Test
	void checkLineReturnsZeroWhenNoCompleteLines() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		assertEquals(0, f.checkLine());
	}

	@Test
	void checkLineNoFlagReturnsCountWithoutSettingFlags() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 18, Block.BLOCK_COLOR_BLUE);
		}

		assertEquals(2, f.checkLineNoFlag());
		assertFalse(f.getLineFlag(18));
		assertFalse(f.getLineFlag(19));
	}

	@Test
	void checkLineNoFlagReturnsZeroForLinesWithWallBlocks() {
		Field f = newField();
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_GRAY);
			f.getBlock(x, 19).setAttribute(Block.BLOCK_ATTRIBUTE_WALL, true);
		}
		assertEquals(0, f.checkLineNoFlag(),
				"lines containing wall blocks must not count as complete");
	}

	@Test
	void checkLineDetectsMultipleCompleteLines() {
		Field f = newField();
		for (int y = 17; y <= 19; y++) {
			for (int x = 0; x < f.getWidth(); x++) {
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
			}
		}
		assertEquals(3, f.checkLine());
		assertTrue(f.getLineFlag(17));
		assertTrue(f.getLineFlag(18));
		assertTrue(f.getLineFlag(19));
	}

	// ================================================================
	// getSecretGrade() — edge cases
	// ================================================================

	@Test
	void getSecretGradeReturnsZeroForEmptyField() {
		Field f = newField();
		assertEquals(0, f.getSecretGrade());
	}

	@Test
	void getSecretGradeReturnsZeroWhenHoleCellIsOccupied() {
		Field f = newField();
		// Fill entire row 19 — holeLoc=0 is filled, loop breaks immediately
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		assertEquals(0, f.getSecretGrade());
	}

	@Test
	void getSecretGradeCountsMultipleConsecutiveMatchingRows() {
		Field f = newField();
		// height=20, so:
		// i=19 → holeLoc = -|19-10| + 10 - 1 = 0
		// i=18 → holeLoc = -|18-10| + 10 - 1 = 1
		// Both rows 19 and 18 need their hole columns empty and rest filled.

		// Row 19: holeLoc=0 empty, all other columns filled, (0,18) filled
		for (int x = 1; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);

		// Row 18: holeLoc=1 empty, all other columns filled, (1,17) filled
		for (int x = 0; x < f.getWidth(); x++) {
			if (x != 1) {
				f.setBlockColor(x, 18, Block.BLOCK_COLOR_RED);
			}
		}
		f.setBlockColor(1, 17, Block.BLOCK_COLOR_RED);

		assertEquals(2, f.getSecretGrade());
	}

	@Test
	void getSecretGradeStopsAtFirstNonMatchingRow() {
		Field f = newField();
		// Row 19 matches, but row 18 has a gap in a non-hole column
		for (int x = 1; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);

		// Row 18: holeLoc=1, but column 3 is also empty → fails
		for (int x = 0; x < f.getWidth(); x++) {
			if (x != 1 && x != 3) {
				f.setBlockColor(x, 18, Block.BLOCK_COLOR_RED);
			}
		}

		assertEquals(1, f.getSecretGrade(),
				"only row 19 should match; row 18 has an extra gap");
	}
}
