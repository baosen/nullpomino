package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Branch-gap tests for {@link FieldGarbage}.
 *
 * <p>Covers the out-of-range avoid-column guard, the random re-roll loops in
 * both partial-row placement strategies (exercised across many fixed seeds so
 * the collision branches deterministically occur), and the flash-mode gem
 * selection with a zero-count palette color and an out-of-gem-range color.
 */
class FieldGarbageBranchGapTest {

	private static Field field() {
		return new Field(10, 10, 0, false);
	}

	private static GameEngine engine(long seed) {
		GameManager gm = new GameManager(new EventReceiver());
		GameEngine engine = new GameEngine(gm, 0, new RuleOptions(), null, null);
		engine.random = new Random(seed);
		return engine;
	}

	private static int garbageCountInRow(Field f, int y) {
		int n = 0;
		for (int x = 0; x < f.width; x++) {
			Block b = f.getBlock(x, y);
			if (!b.isEmpty() && b.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE)) {
				n++;
			}
		}
		return n;
	}

	// ──────────────────────────────────────────────────────────
	// garbageDrop: partial-row placement
	// ──────────────────────────────────────────────────────────

	/**
	 * An avoid column at or beyond the field width is ignored: the drop
	 * behaves as if no column were protected. Run across many seeds so the
	 * removal loop's "re-roll on already-removed column" branch also fires.
	 */
	@Test
	void garbageDropIgnoresOutOfRangeAvoidColumn() {
		for (long seed = 0; seed < 20; seed++) {
			Field f = field();
			FieldGarbage.garbageDrop(f, engine(seed), 6, false, 0, 0, 99,
					Block.BLOCK_COLOR_GRAY);
			assertEquals(6, garbageCountInRow(f, 0), "seed " + seed);
		}
	}

	/**
	 * Dense partial row (more than half the width) with a valid avoid column:
	 * the avoid column stays empty and exactly {@code drop} blocks land.
	 */
	@Test
	void garbageDropDenseRowLeavesAvoidColumnEmpty() {
		for (long seed = 0; seed < 20; seed++) {
			Field f = field();
			FieldGarbage.garbageDrop(f, engine(seed), 6, false, 0, 0, 3,
					Block.BLOCK_COLOR_GRAY);
			assertEquals(6, garbageCountInRow(f, 0), "seed " + seed);
			assertTrue(f.getBlock(3, 0).isEmpty(), "avoid column, seed " + seed);
		}
	}

	/**
	 * Sparse partial row (at most half the width) with a valid avoid column.
	 * The re-roll loop exits either on an unused column or when the random
	 * draw lands on the avoid column a second time (which re-marks it and can
	 * leave fewer than {@code drop} distinct columns filled); across 20 seeds
	 * both exits occur.
	 */
	@Test
	void garbageDropSparseRowWithAvoidColumn() {
		for (long seed = 0; seed < 20; seed++) {
			Field f = field();
			FieldGarbage.garbageDrop(f, engine(seed), 5, false, 0, 0, 3,
					Block.BLOCK_COLOR_GRAY);
			int placed = garbageCountInRow(f, 0);
			assertTrue(placed >= 1 && placed <= 5,
					"between 1 and drop blocks, seed " + seed + ": " + placed);
		}
	}

	// ──────────────────────────────────────────────────────────
	// addRandomHoverBlocks: avoid-lines rewriting
	// ──────────────────────────────────────────────────────────

	/**
	 * Fill an entire region with a two-color palette and line avoidance on:
	 * a dense random grid exercises every reachable colorUp/colorLeft
	 * combination of the two-color rewrite rules across seeds.
	 */
	@Test
	void addRandomHoverBlocksDenseTwoColorAvoidLines() {
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};
		for (long seed = 0; seed < 10; seed++) {
			Field f = field();
			FieldGarbage.addRandomHoverBlocks(f, engine(seed), 60, colors, 4,
					true, false);
			for (int y = 4; y < 10; y++) {
				for (int x = 0; x < 10; x++) {
					int c = f.getBlockColor(x, y);
					assertTrue(c == Block.BLOCK_COLOR_RED || c == Block.BLOCK_COLOR_BLUE,
							"palette color at (" + x + "," + y + "), seed " + seed);
				}
			}
		}
	}

	// ──────────────────────────────────────────────────────────
	// addRandomHoverBlocks: flash mode gem selection
	// ──────────────────────────────────────────────────────────

	/**
	 * Flash mode with a single placed block and a two-color palette: one
	 * palette color ends with a zero count, so the gem pass must skip it and
	 * still turn the placed block into its gem variant.
	 */
	@Test
	void flashModeSkipsPaletteColorWithZeroCount() {
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};
		Field f = field();
		FieldGarbage.addRandomHoverBlocks(f, engine(42), 1, colors, 8, true, true);

		int gems = 0;
		for (int y = 8; y < 10; y++) {
			for (int x = 0; x < 10; x++) {
				int c = f.getBlockColor(x, y);
				if (c != Block.BLOCK_COLOR_NONE) {
					assertTrue(c == Block.BLOCK_COLOR_GEM_RED || c == Block.BLOCK_COLOR_GEM_BLUE,
							"lone block becomes a gem, got " + c);
					gems++;
				}
			}
		}
		assertEquals(1, gems);
	}

	/**
	 * Flash mode with a palette color outside the normal 2..8 range: the gem
	 * pass must reject it on the upper-bound check and only gem the normal
	 * color.
	 */
	@Test
	void flashModeIgnoresPaletteColorAboveGemRange() {
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_GEM_RED};
		Field f = field();
		FieldGarbage.addRandomHoverBlocks(f, engine(7), 2, colors, 8, true, true);

		int blocks = 0;
		for (int y = 8; y < 10; y++) {
			for (int x = 0; x < 10; x++) {
				int c = f.getBlockColor(x, y);
				if (c != Block.BLOCK_COLOR_NONE) {
					assertEquals(Block.BLOCK_COLOR_GEM_RED, c,
							"red gets gemmed, gem-red palette color stays");
					blocks++;
				}
			}
		}
		assertEquals(2, blocks);
	}
}
