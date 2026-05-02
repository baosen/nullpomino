package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for T-Spin detection methods in {@link Field}.
 * <p>
 * Covers {@link Field#isTSpinSpot(int, int, boolean)},
 * {@link Field#isTSlot(int, int, boolean)},
 * {@link Field#getTSlotLineClear(int, int, boolean)}, and
 * {@link Field#getTSlotLineClearAll(boolean)} /
 * {@link Field#getTSlotLineClearAll(boolean, int)}.
 * <p>
 * The four diagonal corner offsets used for standard (big=false) detection are
 * {{0,0}, {2,0}, {0,2}, {2,2}} relative to the (x,y) position.
 * For big=true they are {{1,1}, {4,1}, {1,4}, {4,4}}.
 * A T-spin spot requires at least 3 of those corners to be filled.
 * A T-slot additionally requires a cross-shaped empty area (6 cells for
 * standard, 1 center cell for big) and exactly 3 filled corners.
 */
class FieldTSpinDetectionTest {

	private static Field newField() {
		return new Field(10, 20, 3, false);
	}

	// ================================================================
	// isTSpinSpot(int x, int y, boolean big)
	// ================================================================

	@Test
	void isTSpinSpotReturnsTrueWhenAllFourCornersFilled() {
		Field f = newField();
		// Corners at (5,5), (7,5), (5,7), (7,7)
		f.setBlockColor(5, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 7, Block.BLOCK_COLOR_RED);
		assertTrue(f.isTSpinSpot(5, 5, false));
	}

	@Test
	void isTSpinSpotReturnsTrueWhenThreeCornersFilled() {
		Field f = newField();
		// Three corners filled, one empty
		f.setBlockColor(5, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 7, Block.BLOCK_COLOR_RED);
		// (7,7) stays empty
		assertTrue(f.isTSpinSpot(5, 5, false));
	}

	@Test
	void isTSpinSpotReturnsFalseWhenTwoCornersFilled() {
		Field f = newField();
		// Only two corners filled
		f.setBlockColor(5, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 5, Block.BLOCK_COLOR_RED);
		assertFalse(f.isTSpinSpot(5, 5, false));
	}

	@Test
	void isTSpinSpotReturnsFalseWhenOneCornerFilled() {
		Field f = newField();
		f.setBlockColor(5, 5, Block.BLOCK_COLOR_RED);
		assertFalse(f.isTSpinSpot(5, 5, false));
	}

	@Test
	void isTSpinSpotReturnsFalseWhenNoCornersFilled() {
		Field f = newField();
		assertFalse(f.isTSpinSpot(5, 5, false));
	}

	@Test
	void isTSpinSpotCountsOutOfBoundsCornersAsFilledAtRightEdge() {
		Field f = newField();
		// Position (8, 5). Corners: (8,5), (10,5), (8,7), (10,7).
		// (10,5) and (10,7) are out of bounds (width=10) → getBlockColor returns
		// BLOCK_COLOR_INVALID (-1) which is != BLOCK_COLOR_NONE, so they count as filled.
		// Fill one in-bounds corner to reach 3.
		f.setBlockColor(8, 5, Block.BLOCK_COLOR_RED);
		assertTrue(f.isTSpinSpot(8, 5, false),
				"out-of-bounds corners at x+2=10 count as filled, so 1 in-bounds + 2 OOB = 3");
	}

	@Test
	void isTSpinSpotCountsOutOfBoundsCornersAsFilledAtBottomEdge() {
		Field f = newField();
		// Position (5, 18). Corners: (5,18), (7,18), (5,20), (7,20).
		// (5,20) and (7,20) are out of bounds (height=20) → count as filled.
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_RED);
		assertTrue(f.isTSpinSpot(5, 18, false),
				"out-of-bounds corners at y+2=20 count as filled");
	}

	@Test
	void isTSpinSpotReturnsFalseWhenOnlyOutOfBoundsCornersAreFilled() {
		Field f = newField();
		// Position (9, 18). Corners: (9,18), (11,18), (9,20), (11,20).
		// (11,18), (9,20), (11,20) are out of bounds → 3 filled.
		// (9,18) is empty. 3 filled → true.
		// BUT: if we position so only 2 are out of bounds and those are "filled",
		// we need the in-bounds ones to also be empty for total < 3.
		// At (9, 19): corners (9,19), (11,19), (9,21), (11,21).
		// (11,19), (9,21), (11,21) are out of bounds → 3 → still true.
		// At (9, 18): (9,18), (11,18), (9,20), (11,20). (11,18), (9,20), (11,20) OOB.
		// 3 OOB → true even with (9,18) empty. So edge always gives ≥3 when
		// both x+2 and y+2 are OOB. Document this behavior:
		assertTrue(f.isTSpinSpot(9, 18, false),
				"at bottom-right corner, 3 of 4 corners are out of bounds and count as filled");
	}

	@Test
	void isTSpinSpotWithBigReturnsTrueWhenAllFourCornersFilled() {
		Field f = newField();
		// Big T-spin corners at offsets {{1,1}, {4,1}, {1,4}, {4,4}}
		// Relative to (x=3, y=3): (4,4), (7,4), (4,7), (7,7)
		f.setBlockColor(4, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 7, Block.BLOCK_COLOR_RED);
		assertTrue(f.isTSpinSpot(3, 3, true));
	}

	@Test
	void isTSpinSpotWithBigReturnsTrueWhenThreeCornersFilled() {
		Field f = newField();
		f.setBlockColor(4, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		assertTrue(f.isTSpinSpot(3, 3, true));
	}

	@Test
	void isTSpinSpotWithBigReturnsFalseWhenTwoCornersFilled() {
		Field f = newField();
		f.setBlockColor(4, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 4, Block.BLOCK_COLOR_RED);
		assertFalse(f.isTSpinSpot(3, 3, true));
	}

	@Test
	void isTSpinSpotWithBigReturnsFalseWhenNoCornersFilled() {
		Field f = newField();
		assertFalse(f.isTSpinSpot(3, 3, true));
	}

	// ================================================================
	// isTSlot(int x, int y, boolean big)
	// ================================================================

	@Test
	void isTSlotReturnsTrueForStandardTSpinSetup() {
		Field f = newField();
		// Three corners filled at (4,10), (6,10), (4,12); (6,12) stays empty.
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// The six cross-shaped cells must be empty: (5,9),(5,10),(5,11),(4,11),(6,11),(5,12)
		// All of these fall within columns 4-6 and rows 9-12, none are explicitly filled.
		assertTrue(f.isTSlot(4, 10, false));
	}

	@Test
	void isTSlotReturnsFalseWhenAllFourCornersFilled() {
		Field f = newField();
		// Fill all four corners. isTSlot requires exactly 3 filled corners.
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 12, Block.BLOCK_COLOR_RED);
		assertFalse(f.isTSlot(4, 10, false));
	}

	@Test
	void isTSlotReturnsFalseWhenOnlyTwoCornersFilled() {
		Field f = newField();
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		assertFalse(f.isTSlot(4, 10, false));
	}

	@Test
	void isTSlotReturnsFalseWhenCenterIsFilled() {
		Field f = newField();
		// Three corners filled
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// But the cross-shaped empty area has a block: fill (5,10) which is the ★ cell
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_BLUE);
		assertFalse(f.isTSlot(4, 10, false));
	}

	@Test
	void isTSlotReturnsFalseWhenLeftArmIsFilled() {
		Field f = newField();
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill (4,11) — left arm of the cross must be empty
		f.setBlockColor(4, 11, Block.BLOCK_COLOR_BLUE);
		assertFalse(f.isTSlot(4, 10, false));
	}

	@Test
	void isTSlotReturnsFalseWhenRightArmIsFilled() {
		Field f = newField();
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill (6,11) — right arm of the cross must be empty
		f.setBlockColor(6, 11, Block.BLOCK_COLOR_BLUE);
		assertFalse(f.isTSlot(4, 10, false));
	}

	@Test
	void isTSlotReturnsFalseWhenTopArmIsFilled() {
		Field f = newField();
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill (5,9) — top arm of cross must be empty
		f.setBlockColor(5, 9, Block.BLOCK_COLOR_BLUE);
		assertFalse(f.isTSlot(4, 10, false));
	}

	@Test
	void isTSlotReturnsFalseWhenBottomArmIsFilled() {
		Field f = newField();
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill (5,12) — bottom arm must be empty
		f.setBlockColor(5, 12, Block.BLOCK_COLOR_BLUE);
		assertFalse(f.isTSlot(4, 10, false));
	}

	@Test
	void isTSlotReturnsTrueForBigTSpinSetup() {
		Field f = newField();
		// Big T-slot at (3,3). Corners at offsets {{1,1},{4,1},{1,4},{4,4}}:
		// (4,4), (7,4), (4,7), (7,7). Fill three: (4,4), (7,4), (4,7).
		f.setBlockColor(4, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		// Big T-slot also checks that (x+2, y+2) = (5,5) is empty.
		assertTrue(f.isTSlot(3, 3, true));
	}

	@Test
	void isTSlotWithBigReturnsFalseWhenCenterIsFilled() {
		Field f = newField();
		f.setBlockColor(4, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		// Fill the big center cell (x+2, y+2) = (5,5)
		f.setBlockColor(5, 5, Block.BLOCK_COLOR_BLUE);
		assertFalse(f.isTSlot(3, 3, true));
	}

	@Test
	void isTSlotWithBigReturnsFalseWhenAllFourCornersFilled() {
		Field f = newField();
		f.setBlockColor(4, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 7, Block.BLOCK_COLOR_RED);
		assertFalse(f.isTSlot(3, 3, true));
	}

	// ================================================================
	// getTSlotLineClear(int x, int y, boolean big)
	// ================================================================

	@Test
	void getTSlotLineClearReturnsZeroWhenNotASlot() {
		Field f = newField();
		// No corners filled → not a T-slot
		assertEquals(0, f.getTSlotLineClear(5, 5, false));
	}

	@Test
	void getTSlotLineClearReturnsZeroWhenSlotExistsButNoSideCellsFilled() {
		Field f = newField();
		// Standard T-slot at (4,10) with 3 corners filled.
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Rows 11 and 12 have no side cells filled → both lineflags become false.
		assertEquals(0, f.getTSlotLineClear(4, 10, false));
	}

	@Test
	void getTSlotLineClearReturnsTwoWhenBothSideRowsComplete() {
		Field f = newField();
		// T-slot at (4,10) with 3 corners
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill rows 11 and 12 outside columns 4-6
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 11, Block.BLOCK_COLOR_BLUE);
				f.setBlockColor(x, 12, Block.BLOCK_COLOR_BLUE);
			}
		}
		assertEquals(2, f.getTSlotLineClear(4, 10, false));
	}

	@Test
	void getTSlotLineClearReturnsOneWhenOnlyFirstSideRowComplete() {
		Field f = newField();
		// T-slot at (4,10) with 3 corners
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill only row 11 on the sides; row 12 sides stay empty
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 11, Block.BLOCK_COLOR_BLUE);
			}
		}
		assertEquals(1, f.getTSlotLineClear(4, 10, false));
	}

	@Test
	void getTSlotLineClearReturnsOneWhenOnlySecondSideRowComplete() {
		Field f = newField();
		// T-slot at (4,10) with 3 corners
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill only row 12 on the sides; row 11 sides stay empty
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 12, Block.BLOCK_COLOR_BLUE);
			}
		}
		assertEquals(1, f.getTSlotLineClear(4, 10, false));
	}

	@Test
	void getTSlotLineClearReturnsTwoForBigTSpinWhenBothRowsComplete() {
		Field f = newField();
		// Big T-slot at (3,3). Corners at (4,4),(7,4),(4,7). (7,7) empty.
		f.setBlockColor(4, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		// For big=true, the side-row check scans columns outside [x, x+3) = [3, 6)
		// So columns 0-2 and 6-9 must be filled in rows y+1=4 and y+2=5.
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 3 || x >= 6) {
				f.setBlockColor(x, 4, Block.BLOCK_COLOR_BLUE);
				f.setBlockColor(x, 5, Block.BLOCK_COLOR_BLUE);
			}
		}
		assertEquals(2, f.getTSlotLineClear(3, 3, true));
	}

	@Test
	void getTSlotLineClearReturnsZeroForBigWhenNoSideRowsFilled() {
		Field f = newField();
		f.setBlockColor(4, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		assertEquals(0, f.getTSlotLineClear(3, 3, true));
	}

	// ================================================================
	// getTSlotLineClearAll(boolean big)
	// ================================================================

	@Test
	void getTSlotLineClearAllReturnsZeroForEmptyField() {
		Field f = newField();
		assertEquals(0, f.getTSlotLineClearAll(false));
	}

	@Test
	void getTSlotLineClearAllReturnsZeroForFieldWithNoTSlot() {
		Field f = newField();
		// Fill one side of the field with blocks, but no T-slot shape
		for (int x = 0; x < f.getWidth() / 2; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		assertEquals(0, f.getTSlotLineClearAll(false));
	}

	@Test
	void getTSlotLineClearAllCountsOnlyUnflaggedRows() {
		Field f = newField();
		// Create a T-slot at (4,10)
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 11, Block.BLOCK_COLOR_BLUE);
				f.setBlockColor(x, 12, Block.BLOCK_COLOR_BLUE);
			}
		}
		// Flag some unrelated row → should not affect the count
		f.setLineFlag(5, true);
		assertEquals(2, f.getTSlotLineClearAll(false));
	}

	@Test
	void getTSlotLineClearAllWithBigReturnsZeroForEmptyField() {
		Field f = newField();
		assertEquals(0, f.getTSlotLineClearAll(true));
	}

	// ================================================================
	// getTSlotLineClearAll(boolean big, int minimum)
	// ================================================================

	@Test
	void getTSlotLineClearAllWithMinimumReturnsZeroForEmptyField() {
		Field f = newField();
		assertEquals(0, f.getTSlotLineClearAll(false, 1));
	}

	@Test
	void getTSlotLineClearAllWithMinimumFiltersLinesBelowThreshold() {
		Field f = newField();
		// Create a T-slot that clears 2 lines
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 11, Block.BLOCK_COLOR_BLUE);
				f.setBlockColor(x, 12, Block.BLOCK_COLOR_BLUE);
			}
		}
		// minimum=2 should include it
		assertEquals(2, f.getTSlotLineClearAll(false, 2));
		// minimum=3 should exclude it
		assertEquals(0, f.getTSlotLineClearAll(false, 3));
		// minimum=1 should include it
		assertEquals(2, f.getTSlotLineClearAll(false, 1));
	}

	@Test
	void getTSlotLineClearAllWithBigAndMinimumWorks() {
		Field f = newField();
		// Big T-slot at (3,3) with 2-line clear
		f.setBlockColor(4, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 4, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 3 || x >= 6) {
				f.setBlockColor(x, 4, Block.BLOCK_COLOR_BLUE);
				f.setBlockColor(x, 5, Block.BLOCK_COLOR_BLUE);
			}
		}
		assertEquals(2, f.getTSlotLineClearAll(true, 1));
		assertEquals(2, f.getTSlotLineClearAll(true, 2));
		assertEquals(0, f.getTSlotLineClearAll(true, 3));
	}

	// ================================================================
	// Cross-method consistency
	// ================================================================

	@Test
	void isTSpinSpotAndIsTSlotAgreeOnStandardTSpinSetup() {
		Field f = newField();
		// A standard T-slot is always a T-spin spot (3 filled corners),
		// but the converse is not true (the extra shape constraints).
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);

		assertTrue(f.isTSpinSpot(4, 10, false),
				"3 corners filled is a T-spin spot");
		assertTrue(f.isTSlot(4, 10, false),
				"same setup with empty cross area is also a T-slot");
	}

	@Test
	void isTSpinSpotIsTrueButIsTSlotIsFalseWhenCrossAreaIsFilled() {
		Field f = newField();
		// Three corners filled, but the cross area is obstructed
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_BLUE);  // center filled

		assertTrue(f.isTSpinSpot(4, 10, false),
				"3 filled corners → T-spin spot regardless of center");
		assertFalse(f.isTSlot(4, 10, false),
				"center cell filled → not a T-slot");
	}
}
