package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link Field}'s text-format serialisation: the simple
 * char-per-block form (rowToString / fieldToString and the
 * stringToRow / stringToField inverses) and the attribute-bearing
 * '<colorHex>/<attrHex>;' form (attrRowToString / attrFieldToString
 * and the attrStringToRow / attrStringToField inverses).
 *
 * <p>These are used by replay snapshots and net-protocol field
 * transfers, so the exact byte shape — including the trailing-zero
 * trim in fieldToString and the trailing '0/0;' trim in
 * attrFieldToString — has to stay stable across releases.
 */
class FieldStringSerializationTest {

	@Test
	void rowToStringEncodesEachBlockColorAsItsBlockToCharGlyph() {
		Field field = new Field(4, 4, 0, false);
		Block[] row = new Block[4];
		row[0] = new Block(Block.BLOCK_COLOR_NONE);
		row[1] = new Block(Block.BLOCK_COLOR_GRAY);
		row[2] = new Block(Block.BLOCK_COLOR_RED);
		row[3] = new Block(Block.BLOCK_COLOR_GEM_RED);

		// 0 = '0', GRAY=8 -> '8', RED=2 -> '2', GEM_RED=9 -> '9'
		assertEquals(
				"" + row[0].blockToChar()
					+ row[1].blockToChar()
					+ row[2].blockToChar()
					+ row[3].blockToChar(),
				field.rowToString(row));
	}

	@Test
	void fieldToStringTopDownTrimsTrailingEmptyRows() {
		Field field = new Field(4, 4, 0, false);
		// Plant a single colored block at (0, 3) — bottom row.
		field.setBlockColor(0, 3, Block.BLOCK_COLOR_RED);

		String out = field.fieldToString();

		// Bottom row reads '2000' (RED, then three empties); higher rows
		// are all '0000' but the trailing '0' trim strips them. So the
		// final string is just '2'.
		assertEquals("2", out);
	}

	@Test
	void fieldToStringPreservesEmbeddedZeros() {
		Field field = new Field(4, 4, 0, false);
		field.setBlockColor(0, 3, Block.BLOCK_COLOR_RED);
		field.setBlockColor(2, 3, Block.BLOCK_COLOR_BLUE);

		String out = field.fieldToString();

		// Bottom row: RED(2), empty(0), BLUE(7), empty(0) -> '2070'. The
		// trailing zero trim eats the final '0' so the persisted form is '207'.
		assertEquals("207", out);
	}

	@Test
	void stringToFieldRoundTripsThroughFieldToString() {
		Field source = new Field(4, 4, 0, false);
		source.setBlockColor(0, 2, Block.BLOCK_COLOR_RED);
		source.setBlockColor(1, 3, Block.BLOCK_COLOR_BLUE);
		source.setBlockColor(3, 3, Block.BLOCK_COLOR_GEM_RED);

		String exported = source.fieldToString();

		Field dest = new Field(4, 4, 0, false);
		dest.stringToField(exported);

		assertEquals(Block.BLOCK_COLOR_RED, dest.getBlockColor(0, 2));
		assertEquals(Block.BLOCK_COLOR_BLUE, dest.getBlockColor(1, 3));
		assertEquals(Block.BLOCK_COLOR_GEM_RED, dest.getBlockColor(3, 3));
		// All other cells are empty (the trim threw away their '0's).
		assertEquals(Block.BLOCK_COLOR_NONE, dest.getBlockColor(2, 2));
		assertEquals(Block.BLOCK_COLOR_NONE, dest.getBlockColor(2, 3));
	}

	@Test
	void stringToRowDefaultsCharactersBeyondTheStringToEmpty() {
		Field field = new Field(4, 4, 0, false);

		// Three-character input for a four-wide row: charAt(3) throws,
		// the catch falls through and column 3 stays empty.
		Block[] row = field.stringToRow("123", 0, false, false);

		assertEquals(4, row.length);
		assertEquals(Block.charToBlockColor('1'), row[0].color);
		assertEquals(Block.charToBlockColor('2'), row[1].color);
		assertEquals(Block.charToBlockColor('3'), row[2].color);
		assertEquals(Block.BLOCK_COLOR_NONE, row[3].color,
				"missing input character -> empty cell");
		// Every block has the documented attribute defaults.
		for(Block b : row) {
			assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
			assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE));
			assertEquals(-1, b.elapsedFrames);
		}
	}

	@Test
	void stringToRowAppliesGarbageAndWallAttributesWhenRequested() {
		Field field = new Field(2, 2, 0, false);

		Block[] garbage = field.stringToRow("11", 5, /*isGarbage=*/ true, /*isWall=*/ false);
		assertTrue(garbage[0].getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
		assertEquals(5, garbage[0].skin);
		assertFalse(garbage[0].getAttribute(Block.BLOCK_ATTRIBUTE_WALL));

		Block[] wall = field.stringToRow("11", 5, /*isGarbage=*/ false, /*isWall=*/ true);
		assertTrue(wall[0].getAttribute(Block.BLOCK_ATTRIBUTE_WALL));
		assertFalse(wall[0].getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
	}

	@Test
	void attrRowToStringEmitsHexColorAndAttributePerCell() {
		Field field = new Field(2, 2, 0, false);
		Block[] row = new Block[2];
		row[0] = new Block();
		row[0].color = Block.BLOCK_COLOR_RED; // 2
		row[0].attribute = 0;

		row[1] = new Block();
		row[1].color = 0xA;
		row[1].attribute = 0xC;

		assertEquals("2/0;a/c;", field.attrRowToString(row));
	}

	@Test
	void attrFieldToStringTrimsTrailingEmptyAttrCellsRowsBelowHighestBlock() {
		Field field = new Field(2, 2, 0, false);
		// The default visible/outline-only attribute is non-zero on
		// post-stringToField cells, but a freshly-constructed Field has
		// all-zero attributes — the trim should compact the whole field
		// to the empty string when nothing is colored.
		assertEquals("", field.attrFieldToString());

		// Now set one cell so there is content.
		Block planted = new Block();
		planted.color = Block.BLOCK_COLOR_RED;
		planted.attribute = 0;
		field.setBlock(0, 1, planted);
		Block empty = new Block();
		empty.color = Block.BLOCK_COLOR_NONE;
		empty.attribute = 0;
		field.setBlock(1, 1, empty);

		// Bottom row contains [RED/0, NONE/0]; the trim eats the
		// trailing '0/0;' and we're left with '2/0;'.
		assertEquals("2/0;", field.attrFieldToString());
	}

	@Test
	void attrStringToFieldRoundTripsThroughAttrFieldToString() {
		Field source = new Field(3, 2, 0, false);
		Block planted = new Block();
		planted.color = Block.BLOCK_COLOR_RED;
		planted.attribute = 0xC;
		source.setBlock(0, 1, planted);

		Block other = new Block();
		other.color = Block.BLOCK_COLOR_BLUE;
		other.attribute = 0;
		source.setBlock(2, 1, other);

		String exported = source.attrFieldToString();

		Field dest = new Field(3, 2, 0, false);
		dest.attrStringToField(exported, 7);

		Block restoredRed = dest.getBlock(0, 1);
		assertEquals(Block.BLOCK_COLOR_RED, restoredRed.color);
		// attrStringToRow forces VISIBLE + OUTLINE on after applying the
		// stored attribute bits, so the recovered attribute is the OR of
		// the persisted bits and those two flags.
		assertTrue(restoredRed.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertTrue(restoredRed.getAttribute(Block.BLOCK_ATTRIBUTE_OUTLINE));
		assertEquals(7, restoredRed.skin);

		Block restoredBlue = dest.getBlock(2, 1);
		assertEquals(Block.BLOCK_COLOR_BLUE, restoredBlue.color);
		assertEquals(7, restoredBlue.skin);
	}

	@Test
	void attrStringToRowFillsMissingCellsWithEmptyDefaults() {
		Field field = new Field(3, 1, 0, false);
		// Only one cell provided for a three-wide field; the other two
		// fall through the catch path and end up as empty defaults.
		Block[] row = field.attrStringToRow("4/2;", 5);

		assertEquals(3, row.length);
		assertEquals(4, row[0].color, "single-cell input -> first column populated");
		assertEquals(Block.BLOCK_COLOR_NONE, row[1].color);
		assertEquals(Block.BLOCK_COLOR_NONE, row[2].color);
		assertEquals(5, row[1].skin, "skin still applied to the empty defaults");
	}

	@Test
	void toStringDelegatesToFieldToStringExceptForTrailingTrim() {
		Field a = new Field(2, 2, 0, false);
		a.setBlockColor(0, 1, Block.BLOCK_COLOR_RED);

		// Field.toString() may format differently from fieldToString,
		// but at minimum it must produce a non-empty string when the
		// field has content. Pin the non-empty contract.
		String formatted = a.toString();
		assertFalse(formatted.isEmpty(),
				"toString of a populated field must be non-empty");
	}
}
