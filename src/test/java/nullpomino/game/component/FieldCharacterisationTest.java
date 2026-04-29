package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.util.CustomProperties;

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
	void downFloatingBlocksSingleLineDropsOneFlaggedRow() {
		Field f = newField();
		f.setBlockColor(0, 17, Block.BLOCK_COLOR_RED);
		f.setLineFlag(19, true);

		f.downFloatingBlocksSingleLine();

		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 18));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 17));
		assertFalse(f.getLineFlag(19));
	}

	@Test
	void pushUpMovesRowsTowardHiddenAreaAndClearsBottom() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GREEN);
		f.setLineFlag(19, true);

		f.pushUp();

		assertEquals(Block.BLOCK_COLOR_GREEN, f.getBlockColor(0, 18));
		assertTrue(f.getLineFlag(18));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertFalse(f.getLineFlag(19));
	}

	@Test
	void pushDownMovesRowsTowardBottomAndClearsTop() {
		Field f = newField();
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_BLUE);
		f.setLineFlag(0, true);

		f.pushDown();

		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 1));
		assertTrue(f.getLineFlag(1));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 0));
		assertFalse(f.getLineFlag(-f.getHiddenHeight()));
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

	@Test
	void safeAccessorsReturnSentinelsForInvalidCoordinates() {
		Field f = newField();

		assertNull(f.getRow(99));
		assertNull(f.getBlock(99, 0));
		assertEquals(Block.BLOCK_COLOR_INVALID, f.getBlockColor(99, 0));
		assertTrue(f.getBlockEmpty(99, 0));
		assertFalse(f.getBlockEmptyF(99, 0));
		assertFalse(f.setBlockColor(99, 0, Block.BLOCK_COLOR_RED));
		assertFalse(f.setBlock(99, 0, new Block(Block.BLOCK_COLOR_RED)));
		assertFalse(f.getLineFlag(99));
		assertFalse(f.setLineFlag(99, true));
	}

	@Test
	void throwingAccessorsRejectInvalidCoordinates() {
		Field f = newField();

		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.getRowE(99));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.getBlockE(99, 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.getBlockColorE(99, 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.setBlockColorE(99, 0, Block.BLOCK_COLOR_RED));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.getBlockEmptyE(99, 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.getLineFlagE(99));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.setLineFlagE(99, true));
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> f.setBlockE(99, 0, new Block(Block.BLOCK_COLOR_RED)));
	}

	/**
	 * Regression test: Block.toString / blockToChar are load-bearing for netplay.
	 * Field.rowToString feeds fieldToString, which is sent over the wire. If the
	 * per-block char encoding ever breaks, the opponent's field gets decoded into
	 * gem / gold-square / silver-square colors (color >= 9) and renders as an
	 * Avalanche-looking mess instead of normal tetromino blocks.
	 */
	@Test
	void fieldToStringRoundTripPreservesTetrominoColors() {
		Field src = newField();
		int[] colors = {
			Block.BLOCK_COLOR_GRAY, Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_ORANGE,
			Block.BLOCK_COLOR_YELLOW, Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_CYAN,
			Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_PURPLE
		};
		for (int i = 0; i < colors.length; i++) {
			src.setBlockColor(i, 19, colors[i]);
		}
		String encoded = src.fieldToString();

		Field dst = newField();
		dst.stringToField(encoded);

		for (int i = 0; i < colors.length; i++) {
			assertEquals(colors[i], dst.getBlockColor(i, 19),
				"round-trip must preserve color at (" + i + ",19); encoded=" + encoded);
			}
	}

	@Test
	void attrFieldToStringRoundTripPreservesColorsAndAttributes() {
		Field src = newField();
		Block block = new Block(Block.BLOCK_COLOR_RED);
		block.attribute = Block.BLOCK_ATTRIBUTE_VISIBLE
				| Block.BLOCK_ATTRIBUTE_OUTLINE
				| Block.BLOCK_ATTRIBUTE_GARBAGE;
		src.setBlock(4, 19, block);

		String encoded = src.attrFieldToString();
		Field dst = newField();
		dst.attrStringToField(encoded, 0);

		assertEquals(Block.BLOCK_COLOR_RED, dst.getBlockColor(4, 19));
		assertTrue(dst.getBlock(4, 19).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(dst.getBlock(4, 19).getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE));
		assertTrue(dst.getBlock(4, 19).getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
		assertEquals(Block.BLOCK_COLOR_NONE, dst.getBlockColor(5, 19));
	}

	@Test
	void propertyRowsRoundTripWithLegacyCommaSeparatedKeys() {
		Field src = new Field(4, 3, 1, false);
		src.setBlockColor(0, 1, Block.BLOCK_COLOR_RED);
		src.setBlockColor(2, 1, Block.BLOCK_COLOR_BLUE);
		CustomProperties props = new CustomProperties();

		src.writeProperty(props, 2);
		assertEquals("2,0,7,0", props.getProperty("2.field.map.1"));

		Field dst = new Field(4, 3, 1, false);
		dst.getBlock(0, 1).elapsedFrames = 99;
		dst.readProperty(props, 2);

		assertEquals(Block.BLOCK_COLOR_RED, dst.getBlockColor(0, 1));
		assertEquals(Block.BLOCK_COLOR_BLUE, dst.getBlockColor(2, 1));
		assertEquals(Block.BLOCK_COLOR_NONE, dst.getBlockColor(1, 1));
		assertEquals(-1, dst.getBlock(0, 1).elapsedFrames);
	}
}
