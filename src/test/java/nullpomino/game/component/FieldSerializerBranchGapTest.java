package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link FieldSerializer}'s attribute-string parsing.
 *
 * <p>Covers the empty-token case of {@code attrStringToRow} (a cell string
 * consisting only of separators splits into a zero-length array) and the
 * per-row exception fallback of {@code attrStringToField}, which blanks the
 * row when writing a parsed row into the field throws.
 */
class FieldSerializerBranchGapTest {

	/**
	 * A token of only '/' separators splits into a zero-length array, so both
	 * the color and the attribute stay at their defaults.
	 */
	@Test
	void attrStringToRowTreatsSeparatorOnlyTokenAsEmptyCell() {
		Field f = new Field(3, 3, 0, false);

		Block[] row = FieldSerializer.attrStringToRow(f,
				new String[] {"/", "5/1", "6"}, 0);

		assertEquals(Block.BLOCK_COLOR_NONE, row[0].color,
				"separator-only token yields an empty block");
		assertEquals(5, row[1].color);
		assertEquals(6, row[2].color);
	}

	/**
	 * If storing a parsed row into the field throws, attrStringToField falls
	 * back to filling that row with empty blocks instead of propagating.
	 * (Reachable through Field subclasses with stricter setBlock overrides.)
	 */
	@Test
	void attrStringToFieldBlanksRowWhenSetBlockThrows() {
		Field f = new Field(3, 3, 0, false) {
			@Override
			public boolean setBlock(int x, int y, Block blk) {
				if (blk != null && blk.color == Block.BLOCK_COLOR_RED) {
					throw new IllegalStateException("reject red");
				}
				return super.setBlock(x, y, blk);
			}
		};

		// Bottom row would be all red (color 2); the override rejects it.
		FieldSerializer.attrStringToField(f, "2/0;2/0;2/0", 0);

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(x, y),
						"row is blanked by the fallback at (" + x + "," + y + ")");
			}
		}
	}

	/** Control: without the throwing override the same string loads normally. */
	@Test
	void attrStringToFieldLoadsBottomRow() {
		Field f = new Field(3, 3, 0, false);

		FieldSerializer.attrStringToField(f, "2/0;2/0;2/0", 0);

		for (int x = 0; x < 3; x++) {
			assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(x, 2));
		}
	}
}
