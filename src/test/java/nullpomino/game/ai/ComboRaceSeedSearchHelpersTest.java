package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;

import org.junit.jupiter.api.Test;

/**
 * Pins the static field-to-code helpers in {@link ComboRaceSeedSearch}
 * (the seed-search bot's bridge between Field state and the
 * 28-entry FIELDS lookup table). {@code fieldToCode} encodes the
 * bottom 3 rows of a 4-wide valley as a 12-bit code, MSB first,
 * row-major; {@code fieldToIndex} binary-searches the FIELDS table
 * for that code and returns -1 for unknown patterns. The 4-column
 * valley defaults to x=3 unless an explicit valleyX is given.
 */
class ComboRaceSeedSearchHelpersTest {

	@Test
	void fieldToCodeOfEmptyFieldIsZero() {
		// Empty field -> all 12 bits clear -> 0.
		Field field = new Field(10, 20, 4);

		assertEquals(0, ComboRaceSeedSearch.fieldToCode(field));
	}

	@Test
	void fieldToCodeReadsBottomRowAsLowBits() {
		// Bottom row at columns 4,5,6 (valleyX=3 -> reads x=3,4,5,6
		// inside the 4-wide valley). With x=4,5,6 filled the bits 2,1,0
		// are set -> code 0x7.
		Field field = new Field(10, 20, 4);
		int height = field.getHeight(); // 20
		// x positions inside the valley (valleyX=3): 3,4,5,6.
		// We want code 0x7 = bits 2,1,0 = bottom row, columns x=4,5,6.
		field.setBlockColor(4, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(5, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(6, height - 1, Block.BLOCK_COLOR_RED);

		assertEquals(0x7, ComboRaceSeedSearch.fieldToCode(field));
	}

	@Test
	void fieldToCodeReadsRowsMostSignificantFirst() {
		// Single block at the TOP of the 3-row window (y=height-3) and
		// x=valleyX -> highest bit (bit 11) -> code 0x800.
		Field field = new Field(10, 20, 4);
		int height = field.getHeight();
		field.setBlockColor(3, height - 3, Block.BLOCK_COLOR_RED);

		assertEquals(0x800, ComboRaceSeedSearch.fieldToCode(field),
				"top row, leftmost column of valley -> bit 11 set");
	}

	@Test
	void fieldToCodeRespectsExplicitValleyX() {
		// Same single block at (x=0, y=height-3) but valleyX=0 means
		// the read window starts at x=0 -> still bit 11.
		Field field = new Field(10, 20, 4);
		int height = field.getHeight();
		field.setBlockColor(0, height - 3, Block.BLOCK_COLOR_RED);

		assertEquals(0x800, ComboRaceSeedSearch.fieldToCode(field, 0));
	}

	@Test
	void fieldToCodeOutOfWindowBlocksAreIgnored() {
		// Block at x=0, y=height-1 with default valleyX=3 -> read window
		// is x=[3..6], so this block is outside the window and the
		// resulting code is 0.
		Field field = new Field(10, 20, 4);
		int height = field.getHeight();
		field.setBlockColor(0, height - 1, Block.BLOCK_COLOR_RED);

		assertEquals(0, ComboRaceSeedSearch.fieldToCode(field),
				"block outside the 4-wide valley window doesn't contribute");
	}

	@Test
	void fieldToIndexFindsFirstFieldsEntry() {
		// FIELDS[0] = 0x7 -> binary search returns 0.
		assertEquals(0, ComboRaceSeedSearch.fieldToIndex((short) 0x7));
	}

	@Test
	void fieldToIndexFindsLastFieldsEntry() {
		// FIELDS[27] = 0x888 -> binary search returns 27.
		assertEquals(27, ComboRaceSeedSearch.fieldToIndex((short) 0x888));
	}

	@Test
	void fieldToIndexFindsMidTableEntry() {
		// FIELDS[10] = 0x23 -> middle-ish entry, binary search hits it.
		assertEquals(10, ComboRaceSeedSearch.fieldToIndex((short) 0x23));
	}

	@Test
	void fieldToIndexReturnsMinusOneForUnknownCode() {
		// 0x100 isn't in FIELDS -> binary search bottoms out -> -1.
		assertEquals(-1, ComboRaceSeedSearch.fieldToIndex((short) 0x100));
	}

	@Test
	void fieldToIndexReturnsMinusOneForEmptyCode() {
		assertEquals(-1, ComboRaceSeedSearch.fieldToIndex((short) 0));
	}

	@Test
	void fieldToIndexFromFieldDelegatesThroughFieldToCode() {
		// Build a field whose code matches FIELDS[0]=0x7 — bottom row
		// columns x=4,5,6 filled (valleyX=3 default) -> index 0.
		Field field = new Field(10, 20, 4);
		int height = field.getHeight();
		field.setBlockColor(4, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(5, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(6, height - 1, Block.BLOCK_COLOR_RED);

		assertEquals(0, ComboRaceSeedSearch.fieldToIndex(field));
	}

	@Test
	void fieldToIndexFromFieldRespectsExplicitValleyX() {
		// Same code 0x7 but at x=0,1,2 -> needs valleyX=-1 to slide the
		// window over... or we can use x=1,2,3 with valleyX=0 since the
		// read window then covers x=0,1,2,3 -> bit-2 is x=2, bit-1 is x=3,
		// bit-0 is OOB and treated as filled (false from getBlockEmptyF).
		// Easier scenario: filled blocks at x=1,2,3 in bottom row, valleyX=0.
		// Window x=0,1,2,3: bit3=empty(0), bit2=fill(1), bit1=fill(1), bit0=fill(1) -> 0x7.
		Field field = new Field(10, 20, 4);
		int height = field.getHeight();
		field.setBlockColor(1, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(2, height - 1, Block.BLOCK_COLOR_RED);
		field.setBlockColor(3, height - 1, Block.BLOCK_COLOR_RED);

		assertEquals(0, ComboRaceSeedSearch.fieldToIndex(field, 0));
	}
}
