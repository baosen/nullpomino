package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;

import org.junit.jupiter.api.Test;

/**
 * Pins the static heuristic helpers on {@link Nohoho} that don't
 * need a live GameEngine: {@link Nohoho#getColumnDepths} which
 * returns per-column highest-block positions.
 *
 * <p>This mirrors the existing {@link PoochyBotStaticHelpersTest}
 * for the PoochyBot version of the same helper.
 */
class NohohoStaticHelpersTest {

	@Test
	void getColumnDepthsReadsHighestBlockYPerColumn() {
		Field f = new Field(4, 6, 0, false);
		// Plant blocks at known heights:
		//   col 0 -> highest at y=5 (bottom only)
		//   col 1 -> empty (highest = field height = 6)
		//   col 2 -> highest at y=3
		//   col 3 -> highest at y=2
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 3, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 2, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 4, Block.BLOCK_COLOR_RED);

		int[] depths = Nohoho.getColumnDepths(f);

		assertEquals(4, depths.length, "one entry per column");
		assertEquals(5, depths[0]);
		// Empty columns surface as field height
		assertEquals(6, depths[1]);
		assertEquals(3, depths[2]);
		assertEquals(2, depths[3]);
	}

	@Test
	void getColumnDepthsOnEmptyFieldReturnsFieldHeightForAllColumns() {
		Field f = new Field(10, 20, 4);

		int[] depths = Nohoho.getColumnDepths(f);

		assertEquals(10, depths.length);
		for (int i = 0; i < depths.length; i++) {
			assertEquals(20, depths[i],
					"empty column " + i + " should report field height");
		}
	}

	@Test
	void getColumnDepthsOnSingleColumnField() {
		Field f = new Field(1, 10, 0, false);
		f.setBlockColor(0, 7, Block.BLOCK_COLOR_RED);

		int[] depths = Nohoho.getColumnDepths(f);

		assertEquals(1, depths.length);
		assertEquals(7, depths[0]);
	}

	@Test
	void getColumnDepthsOnFullFieldReturnsZeroForAllColumns() {
		Field f = new Field(3, 4, 0, false);
		// Fill every cell
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 4; y++) {
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
			}
		}

		int[] depths = Nohoho.getColumnDepths(f);

		assertArrayEquals(new int[] {0, 0, 0}, depths,
				"fully filled field should have highest block at y=0");
	}

	@Test
	void getColumnDepthsMatchesPoochyBotImplementation() {
		// Both Nohoho.getColumnDepths and PoochyBot.getColumnDepths
		// should produce identical results for the same field.
		Field f = new Field(6, 12, 2, false);
		f.setBlockColor(0, 11, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 9, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 0, Block.BLOCK_COLOR_RED);

		int[] nohohoDepths = Nohoho.getColumnDepths(f);
		int[] poochyDepths = PoochyBot.getColumnDepths(f);

		assertArrayEquals(nohohoDepths, poochyDepths,
				"Nohoho and PoochyBot getColumnDepths should agree");
	}
}