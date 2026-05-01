package nullpomino.tool.airankstool;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

/**
 * Pins Ranks's positional surface encoding and copy-constructor
 * meta propagation. Ranks is the on-disk format for the RanksAI
 * surface table; if encode/decode round-trip ever drifts, every
 * persisted ranks file becomes garbage.
 */
class RanksTest {

	@Test
	void constructorInitialisesShapeMetadata() {
		Ranks r = new Ranks(4, 9);

		assertEquals(4, r.getMaxJump());
		assertEquals(9, r.getStackWidth());
		// surfaceWidth = stackWidth - 1 = 8; base = 2*4+1 = 9; size = 9^8
		assertEquals((int) Math.pow(9, 8), r.getSize());
	}

	@Test
	void freshRanksTableIsFilledWithMaxValueSentinel() {
		Ranks r = new Ranks(2, 3);

		// Every slot should be Integer.MAX_VALUE (the unset sentinel)
		for (int i = 0; i < r.getSize(); i++) {
			assertEquals(Integer.MAX_VALUE, r.getRankValue(i));
		}
	}

	@Test
	void encodeAtZeroReturnsZeroForAllMinSurface() {
		Ranks r = new Ranks(2, 3);

		// surfaceWidth=2, maxJump=2, base=5
		// All -maxJump → encoded as 0
		assertEquals(0, r.encode(new int[] {-2, -2}));
	}

	@Test
	void encodeIsPositionalLittleEndianInBase() {
		Ranks r = new Ranks(2, 3);

		// base=5, maxJump=2. Encoding [0, 0] = (0+2)*1 + (0+2)*5 = 12
		assertEquals(12, r.encode(new int[] {0, 0}));
		// [-2, -1] = (-2+2) + (-1+2)*5 = 0+5 = 5
		assertEquals(5, r.encode(new int[] {-2, -1}));
		// [2, 2] = 4 + 4*5 = 24 = (size - 1)
		assertEquals(r.getSize() - 1, r.encode(new int[] {2, 2}));
	}

	@Test
	void encodeAndDecodeAreRoundTripInverses() {
		Ranks r = new Ranks(2, 3);

		int[] surface = new int[] {1, -2};
		int encoded = r.encode(surface);
		int[] decoded = new int[2];
		r.decode(encoded, decoded);

		assertArrayEquals(surface, decoded);
	}

	@Test
	void copyConstructorLinksToOriginalAsRanksFromAndCopiesGeometry() {
		Ranks original = new Ranks(2, 3);

		Ranks copy = new Ranks(original);

		assertEquals(2, copy.getMaxJump());
		assertEquals(3, copy.getStackWidth());
		assertEquals(original.getSize(), copy.getSize());
		assertSame(original, copy.getRanksFrom(),
				"copy must record original as ranksFrom for error tracking");
	}

	@Test
	void setRanksFromReplacesLinkAndResetsErrorCounters() {
		Ranks a = new Ranks(2, 3);
		Ranks b = new Ranks(2, 3);
		Ranks copy = new Ranks(a);

		copy.setRanksFrom(b);

		assertSame(b, copy.getRanksFrom());
	}

	@Test
	void freeRanksFromBreaksTheLink() {
		Ranks a = new Ranks(2, 3);
		Ranks copy = new Ranks(a);

		copy.freeRanksFrom();

		assertNull(copy.getRanksFrom());
	}

	@Test
	void getCompletionPercentageIsZeroOnFreshRanks() {
		Ranks r = new Ranks(2, 3);

		// completion=0 → ((0+1)*100)/size
		int pct = r.getCompletionPercentage();
		assertEquals(100 / r.getSize(), pct);
	}

	@Test
	void getMaxErrorReturnsZeroBeforeAnyRankSet() {
		Ranks r = new Ranks(2, 3);

		assertEquals(0f, r.getMaxError());
		assertEquals(0, r.getError());
	}

	@Test
	void piecesNumRotationsLineUpWithStandardTetrominoes() {
		// Pin the per-piece rotation counts the encoder/decoder reads through —
		// 7 standard pieces + 4 i1/i2/i3/l3 size-class variants, totalling 11.
		assertEquals(11, Ranks.PIECES_NUM_ROTATIONS.length);
		assertEquals(2, Ranks.PIECES_NUM_ROTATIONS[0], "I rotates between 2 orientations");
		assertEquals(1, Ranks.PIECES_NUM_ROTATIONS[2], "O is rotation-symmetric");
		assertEquals(4, Ranks.PIECES_NUM_ROTATIONS[4], "T has 4 distinct rotations");
	}

	@Test
	void pieceTablesAllAgreeOnPieceCount() {
		// PIECES_LEFTMOSTS / RIGHTMOSTS / WIDTHS / HEIGHTS / LOWESTS must all
		// have the same first-axis size or the indexing in RanksIterator NPEs.
		assertEquals(Ranks.PIECES_NUM_ROTATIONS.length, Ranks.PIECES_LEFTMOSTS.length);
		assertEquals(Ranks.PIECES_NUM_ROTATIONS.length, Ranks.PIECES_RIGHTMOSTS.length);
		assertEquals(Ranks.PIECES_NUM_ROTATIONS.length, Ranks.PIECES_WIDTHS.length);
		assertEquals(Ranks.PIECES_NUM_ROTATIONS.length, Ranks.PIECES_HEIGHTS.length);
		assertEquals(Ranks.PIECES_NUM_ROTATIONS.length, Ranks.PIECES_LOWESTS.length);
	}

	@Test
	void aiRanksConstantsHaveExpectedDefaults() {
		// Pins the legacy on-disk paths the AI tools read at startup; bumping
		// these silently breaks every distributed ranksAI checkpoint.
		assertNotNull(AIRanksConstants.RANKSAI_DIR);
		assertEquals("res/ranksAI/", AIRanksConstants.RANKSAI_DIR);
		assertEquals("config/setting/ranksai.cfg",
				AIRanksConstants.RANKSAI_CONFIG_FILE);
		assertEquals("ranks", AIRanksConstants.DEFAULT_RANKS_FILE);
	}
}
