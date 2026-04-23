package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins Field's grid-traversal contracts. These are stable, widely
 * reused by every mode's scoring path, and testable without any
 * GameEngine machinery — so a small unit-test suite is enough to
 * guard the Phase 3a cleanup (removing commented-out experimental
 * code) and any future refactor of the Field grid internals.
 *
 * The API here exercises the public entry points every mode uses:
 * setBlockColor, checkLine, clearLine, downFloatingBlocks, getLines,
 * isEmptyLine. If any of these shifts by one cell, every scoring
 * suite in the codebase drifts with it.
 */
class FieldCharacterisationTest {

	private static Field newField() {
		return new Field(10, 20, 3, false);
	}

	private static void fillRow(Field f, int y, int color) {
		for (int x = 0; x < f.getWidth(); x++) {
			f.setBlockColor(x, y, color);
		}
	}

	@Test
	void freshFieldIsEmptyAndReportsZeroLines() {
		Field f = newField();
		assertTrue(f.isEmpty());
		assertEquals(0, f.getLines());
		assertEquals(0, f.checkLine());
	}

	@Test
	void fullBottomRowIsCountedAsOneLine() {
		Field f = newField();
		fillRow(f, 19, Block.BLOCK_COLOR_RED);
		assertEquals(1, f.checkLine());
		assertTrue(f.getLineFlag(19));
		assertFalse(f.isEmptyLine(19));
	}

	@Test
	void rowMissingOneBlockIsNotACompleteLine() {
		Field f = newField();
		fillRow(f, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_NONE);
		assertEquals(0, f.checkLine());
		assertFalse(f.getLineFlag(19));
	}

	@Test
	void clearLineRemovesFlaggedRowsAndReturnsTheCount() {
		Field f = newField();
		fillRow(f, 18, Block.BLOCK_COLOR_GREEN);
		fillRow(f, 19, Block.BLOCK_COLOR_YELLOW);
		assertEquals(2, f.checkLine());
		assertEquals(2, f.clearLine());
		// After clear, both rows empty but still in place — gravity is a separate step.
		assertTrue(f.isEmptyLine(18));
		assertTrue(f.isEmptyLine(19));
	}

	@Test
	void downFloatingBlocksCollapsesStackedRowsAfterClear() {
		Field f = newField();
		fillRow(f, 17, Block.BLOCK_COLOR_CYAN);
		fillRow(f, 18, Block.BLOCK_COLOR_PURPLE);
		fillRow(f, 19, Block.BLOCK_COLOR_ORANGE);
		assertEquals(3, f.checkLine());
		assertEquals(3, f.clearLine());
		int dropped = f.downFloatingBlocks();
		assertTrue(dropped >= 0);
		// After gravity, the field must be empty (we cleared all filled rows).
		assertTrue(f.isEmpty(), "field must be empty after clearing every filled row");
		assertEquals(0, f.getLines());
	}

	@Test
	void checkLineNoFlagIsCountOnlyAndLeavesFlagsUntouched() {
		Field f = newField();
		fillRow(f, 19, Block.BLOCK_COLOR_RED);
		int count = f.checkLineNoFlag();
		assertEquals(1, count);
		assertFalse(f.getLineFlag(19), "checkLineNoFlag must not set line flags");
	}

	@Test
	void getLinesMatchesFlaggedRowsAfterCheckLine() {
		Field f = newField();
		fillRow(f, 19, Block.BLOCK_COLOR_BLUE);
		fillRow(f, 17, Block.BLOCK_COLOR_GREEN);
		assertEquals(2, f.checkLine());
		assertEquals(2, f.getLines());
	}
}
