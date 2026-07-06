package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link FieldColorClear#checkForSquares(Field)}.
 *
 * <p>Targets the inner-cell rejection branches where a candidate 4x4 window
 * (whose root is a normal block) contains a block that already belongs to a
 * previously formed gold or silver square. Both the gold pass and the silver
 * pass must reject such windows.
 */
class FieldColorClearBranchGapTest {

	private static Field field() {
		return new Field(10, 10, 0, false);
	}

	private static void fillSquare(Field f, int x0, int y0, int color) {
		for (int y = y0; y < y0 + 4; y++) {
			for (int x = x0; x < x0 + 4; x++) {
				f.setBlock(x, y, new Block(color, 0, 0));
			}
		}
	}

	/** Control: the mono-color 4x4 used below really is a gold square when intact. */
	@Test
	void intactMonoColorSquareIsDetectedAsGold() {
		Field f = field();
		fillSquare(f, 0, 0, Block.BLOCK_COLOR_RED);

		assertArrayEquals(new int[] {1, 0}, FieldColorClear.checkForSquares(f));
		assertEquals(Block.BLOCK_COLOR_SQUARE_GOLD_1, f.getBlockColor(0, 0));
	}

	/**
	 * A 4x4 window whose root is a normal block but which contains a block of
	 * an existing gold square must be rejected by both the gold pass (color
	 * check would also fail, but the gold-square check short-circuits first)
	 * and the color-blind silver pass.
	 */
	@Test
	void windowContainingGoldSquareBlockIsRejected() {
		Field f = field();
		fillSquare(f, 0, 0, Block.BLOCK_COLOR_RED);
		// Impostor: bottom-right cell already belongs to another gold square.
		f.getBlock(3, 3).color = Block.BLOCK_COLOR_SQUARE_GOLD_5;

		assertArrayEquals(new int[] {0, 0}, FieldColorClear.checkForSquares(f));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 0),
				"rejected window must stay untouched");
		assertEquals(Block.BLOCK_COLOR_SQUARE_GOLD_5, f.getBlockColor(3, 3));
	}

	/**
	 * Same as above with a silver-square member inside the window: both the
	 * gold pass and the silver pass must reject via the silver-square check.
	 */
	@Test
	void windowContainingSilverSquareBlockIsRejected() {
		Field f = field();
		fillSquare(f, 5, 5, Block.BLOCK_COLOR_BLUE);
		f.getBlock(8, 8).color = Block.BLOCK_COLOR_SQUARE_SILVER_3;

		assertArrayEquals(new int[] {0, 0}, FieldColorClear.checkForSquares(f));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(5, 5),
				"rejected window must stay untouched");
		assertEquals(Block.BLOCK_COLOR_SQUARE_SILVER_3, f.getBlockColor(8, 8));
	}

	/**
	 * Multi-color (but connection-free, unbroken) 4x4 area: rejected by the
	 * gold pass on the color check, accepted by the silver pass — with a
	 * gold-square impostor variant next to it rejected by both.
	 */
	@Test
	void multiColorSquareIsSilverUnlessItContainsSquareBlocks() {
		Field f = field();
		fillSquare(f, 0, 0, Block.BLOCK_COLOR_GREEN);
		f.getBlock(1, 1).color = Block.BLOCK_COLOR_YELLOW; // multi-color => not gold

		assertArrayEquals(new int[] {0, 1}, FieldColorClear.checkForSquares(f));
		assertEquals(Block.BLOCK_COLOR_SQUARE_SILVER_1, f.getBlockColor(0, 0));

		// Second pass on the same field: the silver square's own blocks now
		// poison every overlapping window rooted at a normal block.
		f.setBlock(4, 0, new Block(Block.BLOCK_COLOR_GREEN, 0, 0));
		assertArrayEquals(new int[] {0, 0}, FieldColorClear.checkForSquares(f));
	}
}
