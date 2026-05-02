package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Pins Field's color-clear parameter combinations and edge cases not
 * covered by {@link FieldClearAndCascadeTest}. Focuses on:
 * <ul>
 *   <li>{@link Field#clearColor(int, boolean, boolean, boolean)} &mdash;
 *       size-based color clear with garbageClear, gemSame, ignoreHidden</li>
 *   <li>{@link Field#checkColor(int, boolean, boolean, boolean, boolean)}
 *       &mdash; check-only variant, tracking of
 *       {@link Field#colorClearExtraCount}, {@link Field#colorsCleared},
 *       {@link Field#garbageCleared}</li>
 *   <li>{@link Field#checkLineColor(int, boolean, boolean, boolean)}
 *       &mdash; vertical runs, diagonals, {@link Field#lineColorsCleared}</li>
 *   <li>{@link Field#clearLineColor(int, boolean, boolean)} &mdash;
 *       diagonal-aware clearing</li>
 *   <li>{@link Field#gemClearColor(int, boolean, boolean)} &mdash;
 *       garbageClear and ignoreHidden variants</li>
 *   <li>{@link Field#allClearColor(int, boolean, boolean)} &mdash;
 *       edge cases</li>
 * </ul>
 */
class FieldColorClearTest {

	private static Field newField() {
		return new Field(10, 20, 3, false);
	}

	// ==================================================================
	// clearColor(int size, boolean garbageClear, boolean gemSame,
	//             boolean ignoreHidden)
	// ==================================================================

	@Test
	void clearColorSizeBasedWithGarbageClearRemovesAdjacentGarbage() {
		Field f = newField();
		// 5-block RED cluster at (0,19),(1,19),(2,19),(0,18),(1,18)
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		// Adjacent garbage at (3,19) and (2,18)
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_GRAY);
		f.getBlock(3, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		f.setBlockColor(2, 18, Block.BLOCK_COLOR_GRAY);
		f.getBlock(2, 18).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		// Separate garbage (not adjacent to any RED cluster)
		f.setBlockColor(9, 19, Block.BLOCK_COLOR_GRAY);
		f.getBlock(9, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);

		int cleared = f.clearColor(5, true, false, false);

		assertEquals(5, cleared, "5 RED blocks cleared, garbage does not count");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(1, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(3, 19),
				"adjacent garbage at (3,19) must be cleared");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(2, 18),
				"adjacent garbage at (2,18) must be cleared");
		assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(9, 19),
				"non-adjacent garbage must remain");
	}

	@Test
	void clearColorSizeBasedWithoutGarbageClearLeavesAdjacentGarbage() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		// Adjacent garbage
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_GRAY);
		f.getBlock(2, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);

		int cleared = f.clearColor(4, false, false, false);

		assertEquals(4, cleared, "4 RED blocks cleared");
		assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(2, 19),
				"garbageClear=false: adjacent garbage must NOT be cleared");
	}

	@Test
	void clearColorSizeBasedWithGemSameMergesGemAndNormalClusters() {
		Field f = newField();
		// 4-block cluster mixing gem and normal RED
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		// Separate GEM_RED singleton that should remain (cluster size=1 < 4)
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_GEM_RED);

		int cleared = f.clearColor(4, false, true, false);

		assertEquals(4, cleared,
				"gemSame=true: gem blocks merge with same normal color");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_GEM_RED, f.getBlockColor(5, 19),
				"singleton gem cluster below threshold must survive");
	}

	@Test
	void clearColorSizeBasedWithGemSameFalseTreatsGemsAsDistinct() {
		Field f = newField();
		// 4-block RED cluster (qualifies at threshold 4)
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		// 3-block GEM_RED cluster (below threshold)
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(4, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(3, 18, Block.BLOCK_COLOR_GEM_RED);

		int cleared = f.clearColor(4, false, false, false);

		assertEquals(4, cleared,
				"gemSame=false: only the normal RED cluster qualifies at size 4");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_GEM_RED, f.getBlockColor(3, 19),
				"gem cluster (size 3) must be left intact when gemSame=false");
	}

	@Test
	void clearColorSizeBasedWithIgnoreHiddenSkipsHiddenArea() {
		Field f = newField(); // hidden_height = 3, so y=-1,-2,-3 are hidden
		// 4-block cluster in visible area (row 0)
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 1, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 1, Block.BLOCK_COLOR_RED);
		// 4-block cluster in hidden area (row -1)
		f.setBlockColor(0, -1, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, -1, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, -2, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, -2, Block.BLOCK_COLOR_RED);

		int cleared = f.clearColor(4, false, false, true);

		assertEquals(4, cleared,
				"ignoreHidden=true: only the visible cluster is counted");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 0));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, -1),
				"hidden cluster must not be touched");
	}

	@Test
	void clearColorSizeBasedPreservesClustersBelowThreshold() {
		Field f = newField();
		// 3-block RED cluster (below size=4)
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		// 5-block BLUE cluster (above threshold)
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(6, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(7, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(6, 18, Block.BLOCK_COLOR_BLUE);

		int cleared = f.clearColor(4, false, false, false);

		assertEquals(5, cleared, "only BLUE cluster (size=5) passes threshold");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19),
				"RED cluster (size=3) must be preserved");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 19),
				"BLUE cluster must be cleared");
	}

	@Test
	void clearColorSizeBasedWithSizeOneClearsAllNonGarbageClusters() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(9, 5, Block.BLOCK_COLOR_GREEN);

		int cleared = f.clearColor(1, false, false, false);

		assertEquals(3, cleared, "size=1 clears every non-empty, non-garbage block");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 10));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(9, 5));
	}

	@Test
	void clearColorSizeBasedSkipsGarbageSeeds() {
		Field f = newField();
		// A garbage block that could start a flood-fill — should be skipped
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GRAY);
		f.getBlock(0, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		// Non-garbage block of a different color — should NOT merge with garbage
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);

		int cleared = f.clearColor(2, true, false, false);

		assertEquals(0, cleared,
				"garbage block must not serve as a seed, RED singleton is below size 2");
		assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(0, 19),
				"garbage block color must be unchanged by clearColor");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(1, 19),
				"RED singleton (size 1) must not be cleared at threshold 2");
	}

	// ==================================================================
	// checkColor(int size, boolean flag, boolean garbageClear,
	//             boolean gemSame, boolean ignoreHidden)
	// ==================================================================

	@Test
	void checkColorWithGarbageClearTracksGarbageCleared() {
		Field f = newField();
		// 4-block RED cluster
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		// Adjacent garbage
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_GRAY);
		f.getBlock(2, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		f.setBlockColor(2, 18, Block.BLOCK_COLOR_GRAY);
		f.getBlock(2, 18).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);

		int total = f.checkColor(4, true, true, false, false);

		assertEquals(4, total, "only same-color blocks counted");
		assertEquals(2, f.garbageCleared,
				"garbageCleared must count the two adjacent garbage blocks");
	}

	@Test
	void checkColorCountsColorClearExtra() {
		Field f = newField();
		// 6-block RED cluster (size=4 threshold, so extra = 2)
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);

		int total = f.checkColor(4, true, false, false, false);

		assertEquals(6, total);
		assertEquals(2, f.colorClearExtraCount,
				"extra = cluster size - threshold = 6 - 4");
	}

	@Test
	void checkColorCountsDistinctColorsCleared() {
		Field f = newField();
		// 4-block RED cluster
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		// 4-block BLUE cluster (separate)
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(6, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(6, 18, Block.BLOCK_COLOR_BLUE);

		int total = f.checkColor(4, true, false, false, false);

		assertEquals(8, total, "both clusters qualify");
		assertEquals(2, f.colorsCleared,
				"two distinct colors (RED and BLUE) must be recorded");
	}

	@Test
	void checkColorWithFlagFalseDoesNotMutateField() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);

		int total = f.checkColor(4, false, false, false, false);

		assertEquals(4, total);
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19),
				"flag=false: original field must not be modified");
		assertFalse(f.getBlock(0, 19).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
				"flag=false: ERASE attribute must not be set");
	}

	@Test
	void checkColorWithGemSameCountsMergedClusters() {
		Field f = newField();
		// 2 normal RED + 2 GEM_RED = 4 when gemSame=true
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_GEM_RED);

		int total = f.checkColor(4, true, false, true, false);

		assertEquals(4, total,
				"gemSame=true: gem and normal RED counted as one cluster");
		assertTrue(f.getBlock(0, 19).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
				"flag=true: qualifying blocks must have ERASE set");
	}

	@Test
	void checkColorWithIgnoreHiddenSkipsHiddenArea() {
		Field f = newField();
		// 4-block cluster in visible area
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 1, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 1, Block.BLOCK_COLOR_RED);
		// 4-block cluster in hidden area
		f.setBlockColor(0, -1, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, -1, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, -2, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, -2, Block.BLOCK_COLOR_RED);

		int total = f.checkColor(4, true, false, false, true);

		assertEquals(4, total,
				"ignoreHidden=true: only visible cluster counted");
		assertFalse(f.getBlock(0, -1).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
				"hidden block must not have ERASE set");
	}

	// ==================================================================
	// checkLineColor(int size, boolean flag, boolean diagonals,
	//                 boolean gemSame)
	// ==================================================================

	@Test
	void checkLineColorDetectsVerticalRuns() {
		Field f = newField();
		// 4-cell vertical run in column 5
		f.setBlockColor(5, 16, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 17, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);

		int total = f.checkLineColor(4, false, false, false);

		assertEquals(4, total,
				"4-cell vertical run must be detected at (5,16)");
	}

	@Test
	void checkLineColorWithDiagonalsDetectsDiagonalRuns() {
		Field f = newField();
		// 4-cell diagonal run (size=4): (0,16),(1,17),(2,18),(3,19)
		for (int i = 0; i < 4; i++) {
			f.setBlockColor(i, 16 + i, Block.BLOCK_COLOR_RED);
		}

		int total = f.checkLineColor(4, false, true, false);

		assertEquals(4, total,
				"diagonals=true: 4-cell diagonal run must be detected");
	}

	@Test
	void checkLineColorWithoutDiagonalsMissesDiagonalRuns() {
		Field f = newField();
		for (int i = 0; i < 4; i++) {
			f.setBlockColor(i, 16 + i, Block.BLOCK_COLOR_RED);
		}

		int total = f.checkLineColor(4, false, false, false);

		assertEquals(0, total,
				"diagonals=false: diagonal run must NOT be counted");
	}

	@Test
	void checkLineColorWithDiagonalsDetectsHorizontalRuns() {
		Field f = newField();
		// 4-cell horizontal run — should be detected regardless of diagonals
		for (int x = 0; x < 4; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		int total = f.checkLineColor(4, false, true, false);

		assertEquals(4, total,
				"diagonals=true: horizontal runs still detected");
	}

	@Test
	void checkLineColorWithDiagonalsAndFlagSetsErase() {
		Field f = newField();
		// 4-cell diagonal run
		for (int i = 0; i < 4; i++) {
			f.setBlockColor(i, 16 + i, Block.BLOCK_COLOR_RED);
		}

		int total = f.checkLineColor(4, true, true, false);

		assertEquals(4, total);
		for (int i = 0; i < 4; i++) {
			assertTrue(f.getBlock(i, 16 + i).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
					"flag=true: diagonal run blocks must have ERASE set at (" + i + "," + (16 + i) + ")");
		}
	}

	@Test
	void checkLineColorWithDiagonalsPopulatesLineColorsCleared() {
		Field f = newField();
		// 4-cell diagonal run of RED — count == size, so RED is recorded
		for (int i = 0; i < 4; i++) {
			f.setBlockColor(i, 16 + i, Block.BLOCK_COLOR_RED);
		}

		f.checkLineColor(4, true, true, false);

		assertEquals(1, f.lineColorsCleared.size(),
				"one color recorded in lineColorsCleared");
		assertEquals(Integer.valueOf(Block.BLOCK_COLOR_RED), f.lineColorsCleared.get(0));
	}

	@Test
	void checkLineColorRecordsLineColorWhenSubRunExactlyMatchesSize() {
		Field f = newField();
		// 5-cell horizontal run of RED.  The sub-run starting at x=1
		// (cells 1,2,3,4) has length 4 == size, so RED is recorded.
		for (int x = 0; x < 5; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		f.checkLineColor(4, true, false, false);

		assertEquals(1, f.lineColorsCleared.size(),
				"sub-run of exactly size within overlong run appends the color");
		assertEquals(Integer.valueOf(Block.BLOCK_COLOR_RED), f.lineColorsCleared.get(0));
	}

	@Test
	void checkLineColorWithDiagonalsTracksGems() {
		Field f = newField();
		// 4-cell diagonal run with one gem
		f.setBlockColor(0, 16, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 17, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);

		f.checkLineColor(4, true, true, true);

		assertEquals(1, f.gemsCleared,
				"gem in diagonal run must be counted in gemsCleared");
	}

	@Test
	void checkLineColorWithGemSameCountsGemRuns() {
		Field f = newField();
		// 4-cell vertical run of GEM_RED with gemSame=true
		for (int y = 16; y <= 19; y++) {
			f.setBlockColor(5, y, Block.BLOCK_COLOR_GEM_RED);
		}

		int total = f.checkLineColor(4, true, false, true);

		assertEquals(4, total,
				"gemSame=true: gem-only vertical run must be detected");
	}

	// ==================================================================
	// clearLineColor(int size, boolean diagonals, boolean gemSame)
	// ==================================================================

	@Test
	void clearLineColorWithDiagonalsErasesMarkedDiagonalRun() {
		Field f = newField();
		// 4-cell diagonal run of RED
		for (int i = 0; i < 4; i++) {
			f.setBlockColor(i, 16 + i, Block.BLOCK_COLOR_RED);
		}

		int marked = f.checkLineColor(4, true, true, false);
		int cleared = f.clearLineColor(4, true, false);

		assertEquals(4, marked);
		assertEquals(4, cleared);
		for (int i = 0; i < 4; i++) {
			assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(i, 16 + i),
					"diagonal block at (" + i + "," + (16 + i) + ") must be cleared");
		}
	}

	@Test
	void clearLineColorWithGemSameErasesGemBlocks() {
		Field f = newField();
		// 4-cell horizontal run of GEM_RED with gemSame=true
		for (int x = 0; x < 4; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_GEM_RED);
		}

		int marked = f.checkLineColor(4, true, false, true);
		int cleared = f.clearLineColor(4, false, true);

		assertEquals(4, marked);
		assertEquals(4, cleared);
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
	}

	// ==================================================================
	// gemClearColor(int size, boolean garbageClear, boolean ignoreHidden)
	// ==================================================================

	@Test
	void gemClearColorWithGarbageClearRemovesAdjacentGarbage() {
		Field f = newField();
		// 4-block cluster containing a gem
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);
		// Adjacent garbage
		f.setBlockColor(4, 19, Block.BLOCK_COLOR_GRAY);
		f.getBlock(4, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);

		int cleared = f.gemClearColor(4, true, false);

		assertEquals(4, cleared, "garbage does not count in total");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(4, 19),
				"adjacent garbage must be cleared");
	}

	@Test
	void gemClearColorWithIgnoreHiddenSkipsHiddenArea() {
		Field f = newField();
		// Visible 4-block gem cluster
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 1, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 1, Block.BLOCK_COLOR_RED);
		// Hidden 4-block gem cluster
		f.setBlockColor(0, -1, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, -1, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, -2, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, -2, Block.BLOCK_COLOR_RED);

		int cleared = f.gemClearColor(4, false, true);

		assertEquals(4, cleared,
				"ignoreHidden=true: only visible gem cluster cleared");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 0));
		assertEquals(Block.BLOCK_COLOR_GEM_RED, f.getBlockColor(0, -1),
				"hidden gem cluster must survive");
	}

	@Test
	void gemClearColorPreservesClustersBelowSize() {
		Field f = newField();
		// 2-block gem cluster (below threshold of 4)
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);

		int cleared = f.gemClearColor(4, false, false);

		assertEquals(0, cleared, "cluster of size 2 must not be cleared at threshold 4");
		assertEquals(Block.BLOCK_COLOR_GEM_RED, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(1, 19));
	}

	@Test
	void gemClearColorDoesNotClearNonGemClusters() {
		Field f = newField();
		// 4-block RED cluster with NO gem
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);

		int cleared = f.gemClearColor(4, false, false);

		assertEquals(0, cleared,
				"cluster without any gem block must not be cleared by gemClearColor");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19));
	}

	@Test
	void gemClearColorClearsMultipleGemClusters() {
		Field f = newField();
		// 4-block cluster with GEM_RED at top-left
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		// 4-block cluster with GEM_BLUE at top-left
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_GEM_BLUE);
		f.setBlockColor(6, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(6, 18, Block.BLOCK_COLOR_BLUE);

		int cleared = f.gemClearColor(4, false, false);

		assertEquals(8, cleared, "both gem-containing clusters must be cleared");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 19));
	}

	// ==================================================================
	// allClearColor(int targetColor, boolean flag, boolean gemSame)
	// ==================================================================

	@Test
	void allClearColorWithNoMatchingBlocksReturnsZero() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_BLUE);

		int cleared = f.allClearColor(Block.BLOCK_COLOR_GREEN, false, false);

		assertEquals(0, cleared, "no GREEN blocks in field");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19),
				"field must be unmodified");
	}

	@Test
	void allClearColorWithAllBlocksOfTargetColor() {
		Field f = newField();
		// Fill entire field with RED
		for (int y = 0; y < f.getHeight(); y++) {
			for (int x = 0; x < f.getWidth(); x++) {
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
			}
		}

		int cleared = f.allClearColor(Block.BLOCK_COLOR_RED, false, false);

		assertEquals(f.getWidth() * f.getHeight(), cleared,
				"all cells are RED, so all must be cleared");
		assertTrue(f.isEmpty(), "field must be empty after clearing all color");
	}

	@Test
	void allClearColorWithGemSameMatchesNormalAndGemBlocks() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_BLUE);

		// gemSame=true + target=RED should also match GEM_RED
		int cleared = f.allClearColor(Block.BLOCK_COLOR_RED, false, true);

		assertEquals(3, cleared,
				"gemSame=true: RED target matches both RED and GEM_RED blocks");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(1, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(2, 19));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(5, 10));
	}

	@Test
	void allClearColorWithGemSameAndGemTargetNormalizes() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_GEM_RED);

		int cleared = f.allClearColor(Block.BLOCK_COLOR_GEM_RED, false, true);

		assertEquals(2, cleared,
				"GEM_RED target with gemSame=true normalizes to RED and matches both");
	}

	@Test
	void allClearColorWithFlagSetsEraseOnMatchingBlocks() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_BLUE);

		int marked = f.allClearColor(Block.BLOCK_COLOR_RED, true, false);

		assertEquals(1, marked);
		assertTrue(f.getBlock(0, 19).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
				"flag=true: matching block must have ERASE set");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19),
				"flag=true: block color is preserved");
		assertFalse(f.getBlock(1, 19).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
				"non-matching block must not have ERASE set");
	}

	@Test
	void allClearColorWithNoFieldModificationForInvalidTarget() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);

		int cleared = f.allClearColor(Block.BLOCK_COLOR_INVALID, false, false);

		assertEquals(0, cleared, "INVALID target yields zero");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19));
	}

	@Test
	void allClearColorWithGemSameFalseLeavesGemBlocks() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_GEM_RED);

		int cleared = f.allClearColor(Block.BLOCK_COLOR_RED, false, false);

		assertEquals(1, cleared, "gemSame=false: only the normal RED block matches");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_GEM_RED, f.getBlockColor(1, 19),
				"GEM_RED must survive when gemSame=false");
	}
}
