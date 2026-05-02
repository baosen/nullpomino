package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;

import org.junit.jupiter.api.Test;

/**
 * Pins the static FIELDS-table lookup helpers on {@link ComboRaceBot}:
 * fieldToCode encodes the bottom 3x4 region starting at valleyX as a
 * 12-bit short (top-row first, left-most first, MSB-first), and
 * fieldToIndex binary-searches that code in the curated FIELDS table
 * of stable combo-state surfaces. The default no-arg overloads use
 * valleyX=3.
 *
 * <p>These are pure-logic helpers used to score combo-race moves; the
 * encoding has to stay byte-stable so the parallel stateScores table
 * keeps lining up with the FIELDS entries.
 */
class ComboRaceBotStaticHelpersTest {

	@Test
	void fieldToCodeIsZeroForAllEmptyBottomRegion() {
		// 10x4 field, all empty -> the bottom 3 rows are all empty for
		// any valleyX, so the bit pattern is all-zero.
		Field f = new Field(10, 4, 0, false);

		assertEquals((short) 0, ComboRaceBot.fieldToCode(f, 3));
	}

	@Test
	void fieldToCodeBuildsBitPatternBottomRowLast() {
		// Bottom row only filled across the four valley columns x=3..6:
		// reading order is top-row first within the 3x4 region, so the
		// final bottom row contributes the lowest 4 bits.
		Field f = new Field(10, 4, 0, false);
		f.setBlockColor(3, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 3, Block.BLOCK_COLOR_RED);

		// Bottom row contributes bits 1+2+4+8 = 0xF, top two rows zero.
		// Then the upper rows of the 3x4 region precede the bottom row in
		// shift order, so the final code has a left-shift of 8 over 0xF
		// — but only because both upper rows are zero, the visible code
		// is just 0xF (= 15) -- only if the two upper rows shifted in
		// before the bottom row. Reading the loop:
		// y from height-3 to height-1 = 1, 2, 3 (since height=4).
		// y=1: 4 zero bits, code stays 0.
		// y=2: 4 zero bits, code stays 0.
		// y=3: 4 ones, code becomes 0b1111 = 0xF.
		assertEquals((short) 0xF, ComboRaceBot.fieldToCode(f, 3));
	}

	@Test
	void fieldToCodeReadsTopRowOfRegionIntoHighBits() {
		// Plant a block in the top row of the bottom-3 region (y=height-3).
		// That row is read first (lowest shift count), so it ends up in
		// the *highest* 4 bits of the 12-bit short.
		Field f = new Field(10, 4, 0, false);
		f.setBlockColor(3, 1, Block.BLOCK_COLOR_RED); // y = 1 (top of region)

		// Loop body for y=1 only sets bit 0 of the running result (after
		// pre-shift), then shifts 8 more times for the lower 8 bits.
		// y=1, x=0..3: result = (((0<<1)|1)<<1)|0)<<1)|0)<<1)|0 = 0b1000 = 0x8
		// y=2 and y=3 contribute zero bits but shift result up by 8 places:
		// 0x8 << 8 = 0x800.
		assertEquals((short) 0x800, ComboRaceBot.fieldToCode(f, 3));
	}

	@Test
	void fieldToCodeNoArgUsesValleyXOfThree() {
		Field f = new Field(10, 4, 0, false);
		f.setBlockColor(3, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 3, Block.BLOCK_COLOR_RED);
		// Column 6 left empty so the result is 0b1110 = 0xE.

		assertEquals(ComboRaceBot.fieldToCode(f, 3),
				ComboRaceBot.fieldToCode(f),
				"no-arg overload must use valleyX=3");
		assertEquals((short) 0xE, ComboRaceBot.fieldToCode(f));
	}

	@Test
	void fieldToCodeTreatsOutOfRangeReadsAsFilled() {
		// getBlockEmptyF returns false for out-of-range coordinates, which
		// fieldToCode treats as "filled". A valley starting at x=7 for a
		// 10-wide field reads x=7,8,9,10 — column 10 is out of range
		// across all three bottom rows so its bit is set in every row.
		Field f = new Field(10, 4, 0, false);

		short code = ComboRaceBot.fieldToCode(f, 7);

		// Each row contributes 0b0001 in column 10's slot, so the 12-bit
		// pattern is 0b000100010001 = 0x111.
		assertEquals((short) 0x111, code);
	}

	@Test
	void fieldToIndexReturnsSearchedFieldIndex() {
		// 0x7 is the first entry in FIELDS -> index 0.
		assertEquals(0, ComboRaceBot.fieldToIndex((short) 0x7));
		// 0x888 is the last entry -> index 27.
		assertEquals(27, ComboRaceBot.fieldToIndex((short) 0x888));
	}

	@Test
	void fieldToIndexReturnsMinusOneForCodeNotInTable() {
		// Adjacent codes that don't appear in FIELDS table should map to -1.
		assertEquals(-1, ComboRaceBot.fieldToIndex((short) 0x0));
		assertEquals(-1, ComboRaceBot.fieldToIndex((short) 0x1));
		assertEquals(-1, ComboRaceBot.fieldToIndex((short) 0x2));
		// Just past 0x111 in the table — 0x112 is not in FIELDS.
		assertEquals(-1, ComboRaceBot.fieldToIndex((short) 0x112));
	}

	@Test
	void fieldToIndexFieldOverloadDelegatesToShortLookup() {
		// All-empty bottom region maps to code 0 -> index -1 (not in table).
		Field empty = new Field(10, 4, 0, false);
		assertEquals(-1, ComboRaceBot.fieldToIndex(empty));
		assertEquals(-1, ComboRaceBot.fieldToIndex(empty, 3));
	}

	@Test
	void fieldToIndexFieldOverloadFindsACuratedSurface() {
		// Build a field that produces the FIELDS[0] = 0x7 code: bottom
		// row has columns 1..3 filled (out of valley x=0..3), and rows
		// above empty. Code = 0b000000000111 = 0x7.
		Field f = new Field(10, 4, 0, false);
		f.setBlockColor(1, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 3, Block.BLOCK_COLOR_RED);

		assertEquals((short) 0x7, ComboRaceBot.fieldToCode(f, 0));
		assertEquals(0, ComboRaceBot.fieldToIndex(f, 0),
				"x=0 valley with bottom 1..3 filled -> FIELDS[0]");
	}

	@Test
	void allCuratedFieldsAreReachableByFieldToIndex() {
		// Round-trip every FIELDS entry through fieldToIndex(short) so a
		// future re-sort or accidental dupe in the table is caught.
		short[] curated = {
				0x7, 0xB, 0xD, 0xE,
				0x13, 0x15, 0x16, 0x19, 0x1A, 0x1C,
				0x23, 0x29,
				0x31, 0x32,
				0x49, 0x4C,
				0x61, 0x68,
				0x83, 0x85, 0x86, 0x89, 0x8A, 0x8C,
				0xC4, 0xC8,
				0x111, 0x888
		};
		for(int i = 0; i < curated.length; i++) {
			int idx = ComboRaceBot.fieldToIndex(curated[i]);
			assertTrue(idx >= 0,
					"FIELDS[" + i + "] = 0x" + Integer.toHexString(curated[i] & 0xFFFF)
							+ " must be reachable");
			assertEquals(i, idx,
					"FIELDS[" + i + "] must round-trip to its own index");
		}
	}
}
