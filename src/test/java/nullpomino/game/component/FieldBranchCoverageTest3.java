// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Additional branch-coverage tests for {@link Field} that target the specific
 * branch <em>outcomes</em> still missing after the existing Field* test suite.
 * All tests are deterministic and assertion-backed.
 */
class FieldBranchCoverageTest3 {

	private static GameEngine createEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		GameEngine engine = new GameEngine(gm, 0);
		engine.random = new Random(42);
		return engine;
	}

	/** Place a normal (non-empty) block of the given color at (x,y). */
	private static void put(Field f, int x, int y, int color) {
		f.setBlockColor(x, y, color);
	}

	// ══════════════════════════════════════════════════════════════════════
	// getHowManyHoles — L1026 if(getLineFlag(i) == false): cover the TRUE arm
	// (a flagged line inside the highest..height scan range is skipped).
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getHowManyHolesSkipsFlaggedLines() {
		Field f = new Field(4, 6, 0, false);
		// Column 0: block at row 3, gap (hole) at row 4, block at row 5 -> 1 hole.
		put(f, 0, 3, Block.BLOCK_COLOR_RED);
		put(f, 0, 5, Block.BLOCK_COLOR_RED);
		int holesNoFlag = f.getHowManyHoles();
		assertEquals(1, holesNoFlag, "baseline: one covered hole");

		// Now flag row 4 (the hole row). getLineFlag(4) == true makes the inner
		// body skip that row, so isHoleBelow/samehole logic never counts it.
		f.setLineFlag(4, true);
		int holesWithFlag = f.getHowManyHoles();
		assertEquals(0, holesWithFlag, "flagged hole row is excluded from the count");
	}

	// ══════════════════════════════════════════════════════════════════════
	// getValleyDepth — L1110 compound guard, right-edge sub-condition
	// (x >= width - 1) true while the left neighbour is empty.
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getValleyDepthAtRightEdgeColumnUsesImplicitRightWall() {
		Field f = new Field(4, 8, 0, false);
		int x = f.getWidth() - 1; // right-most column
		// Left neighbour column (x-1) walled high so (!empty(x-1) ) is true,
		// the valley column itself empty, and the implicit right wall via
		// (x >= width - 1) provides the right boundary.
		for (int y = 5; y < 8; y++) {
			put(f, x - 1, y, Block.BLOCK_COLOR_RED);
		}
		int depth = f.getValleyDepth(x);
		assertEquals(3, depth, "right-edge column forms a depth-3 valley against the wall");
	}

	@Test
	void getValleyDepthAtLeftEdgeColumnUsesImplicitLeftWall() {
		Field f = new Field(4, 8, 0, false);
		// Left edge: x <= 0 sub-condition supplies the left wall.
		for (int y = 5; y < 8; y++) {
			put(f, 1, y, Block.BLOCK_COLOR_BLUE);
		}
		int depth = f.getValleyDepth(0);
		assertEquals(3, depth, "left-edge column forms a depth-3 valley against the wall");
	}

	// ══════════════════════════════════════════════════════════════════════
	// getItemClears — L1412: cover blk.item <= MAX_ITEM being FALSE
	// (an item id greater than MAX_ITEM is ignored on a cleared line).
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getItemClearsIgnoresItemIdsAboveMax() {
		Field f = new Field(4, 6, 0, false);
		put(f, 0, 5, Block.BLOCK_COLOR_RED);
		put(f, 1, 5, Block.BLOCK_COLOR_RED);
		// A valid item (1) and an out-of-range item id (MAX_ITEM + 1).
		f.getBlock(0, 5).item = Block.MAX_ITEM;          // counted
		f.getBlock(1, 5).item = Block.MAX_ITEM + 1;      // rejected by <= MAX_ITEM
		f.setLineFlag(5, true);

		boolean[] result = f.getItemClears();
		assertTrue(result[Block.MAX_ITEM], "in-range item id recorded");
		assertEquals(Block.MAX_ITEM + 1, result.length,
				"result array sized to MAX_ITEM + 1; id above max cannot be indexed");
	}

	// ══════════════════════════════════════════════════════════════════════
	// checkLineColor — L1666: cover lineColorsCleared != null (the FALSE arm).
	// Calling twice with flag=true means the second call finds it non-null.
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void checkLineColorReusesExistingLineColorsClearedList() {
		Field f = new Field(6, 4, 0, false);
		// A horizontal run of 4 same-color blocks so checkLineColor finds a clear.
		for (int x = 0; x < 4; x++) {
			put(f, x, 3, Block.BLOCK_COLOR_RED);
		}
		int first = f.checkLineColor(4, true, false, false);
		assertTrue(first >= 4, "first pass detects the colour line");
		assertTrue(f.lineColorsCleared != null, "list allocated on first pass");

		// Second pass: lineColorsCleared is already non-null, so L1666's
		// `if (lineColorsCleared == null)` takes the FALSE branch.
		int second = f.checkLineColor(4, true, false, false);
		assertTrue(second >= 4, "second pass still detects the colour line");
	}

	// ══════════════════════════════════════════════════════════════════════
	// checkBlockLinkSub — L2048: cover the TEMP_MARK-already-set FALSE arm.
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void checkBlockLinkSkipsBlocksAlreadyTempMarked() {
		Field f = new Field(4, 6, 0, false);
		put(f, 1, 5, Block.BLOCK_COLOR_RED);
		Block b = f.getBlock(1, 5);
		// Pre-mark it so the (!getAttribute(TEMP_MARK)) sub-condition is false.
		b.setAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK, true);

		// checkBlockLink sets TEMP_MARK across the reachable set, but the
		// already-marked block short-circuits inside checkBlockLinkSub.
		f.checkBlockLink(1, 5);
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK),
				"already-marked block remains marked");
	}

	// ══════════════════════════════════════════════════════════════════════
	// setBlockLinkByColorSub — L2116/L2117: cover the GARBAGE and
	// non-normal-block FALSE arms (a garbage block is never colour-linked).
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void setBlockLinkByColorIgnoresGarbageBlocks() {
		Field f = new Field(4, 6, 0, false);
		put(f, 0, 5, Block.BLOCK_COLOR_RED);
		put(f, 1, 5, Block.BLOCK_COLOR_RED);
		// Mark (0,5) as garbage so !GARBAGE is false at the start cell.
		f.getBlock(0, 5).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);

		f.setBlockLinkByColor(0, 5);
		// The garbage start block is never marked nor connected.
		assertFalse(f.getBlock(0, 5).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK),
				"garbage block is skipped, never temp-marked");
		assertFalse(f.getBlock(0, 5).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT),
				"garbage block gets no connection flags");
	}

	@Test
	void setBlockLinkByColorIgnoresNonNormalBlocks() {
		Field f = new Field(4, 6, 0, false);
		// A gem block (color 9) is non-normal -> isNormalBlock() is false.
		put(f, 0, 5, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockLinkByColor(0, 5);
		assertFalse(f.getBlock(0, 5).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK),
				"non-normal (gem) block is skipped");
	}

	// ══════════════════════════════════════════════════════════════════════
	// doCascadeGravity — L1899: cover the ANTIGRAVITY block being skipped
	// (the !getAttribute(ANTIGRAVITY) sub-condition takes its FALSE arm).
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void doCascadeGravityLeavesAntigravityBlockFloating() {
		Field f = new Field(4, 8, 0, false);
		// A floating block high up, marked ANTIGRAVITY: it must not fall.
		put(f, 1, 2, Block.BLOCK_COLOR_RED);
		f.getBlock(1, 2).setAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY, true);

		boolean moved = f.doCascadeGravity();
		assertFalse(moved, "antigravity block does not trigger a cascade fall");
		assertFalse(f.getBlockEmpty(1, 2), "antigravity block stays at its original row");
	}

	@Test
	void doCascadeSlowLeavesAntigravityBlockFloating() {
		Field f = new Field(4, 8, 0, false);
		put(f, 2, 2, Block.BLOCK_COLOR_BLUE);
		f.getBlock(2, 2).setAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY, true);

		boolean moved = f.doCascadeSlow();
		assertFalse(moved, "antigravity block does not trigger a slow cascade fall");
		assertFalse(f.getBlockEmpty(2, 2), "antigravity block stays put under slow cascade");
	}

	// ══════════════════════════════════════════════════════════════════════
	// doCascadeGravity / doCascadeSlow — a normal floating block DOES fall,
	// driving the bBelow-empty TRUE path inside the inner mark/move loops
	// (L1930-1931 / L2002-2003) and the wall-attribute paths (L1912/L1985).
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void doCascadeGravityDropsFloatingBlockOneStep() {
		Field f = new Field(4, 6, 0, false);
		put(f, 1, 1, Block.BLOCK_COLOR_RED); // floating near the top
		assertTrue(f.getBlockEmpty(1, 2), "cell below initially empty");

		boolean moved = f.doCascadeGravity();
		assertTrue(moved, "a floating block produces a cascade step");
		// doCascadeGravity advances the block exactly one row per call.
		assertTrue(f.getBlockEmpty(1, 1), "original cell is now empty");
		assertFalse(f.getBlockEmpty(1, 2), "block descended one row");
	}

	@Test
	void doCascadeSlowDropsFloatingBlockOneStep() {
		Field f = new Field(4, 6, 0, false);
		put(f, 2, 1, Block.BLOCK_COLOR_GREEN);

		boolean moved = f.doCascadeSlow();
		assertTrue(moved, "slow cascade reports movement");
		assertTrue(f.getBlockEmpty(2, 1), "block left its starting cell");
		assertFalse(f.getBlockEmpty(2, 2), "block descended exactly one row in slow mode");
	}

	// ══════════════════════════════════════════════════════════════════════
	// canCascade — L2577 ANTIGRAVITY skip arm, plus the no-fall result.
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void canCascadeFalseWhenOnlyAntigravityBlockFloats() {
		Field f = new Field(4, 6, 0, false);
		put(f, 1, 2, Block.BLOCK_COLOR_RED);
		f.getBlock(1, 2).setAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY, true);
		assertFalse(f.canCascade(), "antigravity-only field cannot cascade");
	}

	@Test
	void canCascadeTrueWhenFloatingBlockExists() {
		Field f = new Field(4, 6, 0, false);
		put(f, 1, 2, Block.BLOCK_COLOR_RED);
		assertTrue(f.canCascade(), "a free floating block can cascade");
	}

	// ══════════════════════════════════════════════════════════════════════
	// freeFall — L2963 while(!getBlockEmpty(x,y1) && y1 >= -hidden_height):
	// cover the y1 >= -hidden_height FALSE arm by completely filling a column
	// (visible + hidden rows) so the scan walks off the top of the field.
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void freeFallHandlesFullyFilledColumn() {
		int hidden = 2;
		Field f = new Field(3, 4, hidden, false);
		// Fill column 0 from the top hidden row down to the floor (no gaps),
		// so y1 decrements past -hidden_height and the loop guard goes false.
		for (int y = -hidden; y < f.getHeight(); y++) {
			put(f, 0, y, Block.BLOCK_COLOR_RED);
		}
		// Column 1 has a gap that freeFall should collapse, ensuring a true result.
		put(f, 1, 1, Block.BLOCK_COLOR_BLUE); // floating

		boolean result = f.freeFall();
		assertTrue(result, "the floating block in column 1 falls, so freeFall reports change");
		// Fully packed column 0 stays fully packed.
		for (int y = -hidden; y < f.getHeight(); y++) {
			assertFalse(f.getBlockEmpty(0, y), "packed column remains packed at row " + y);
		}
		// Column 1 block ended on the floor.
		assertFalse(f.getBlockEmpty(1, f.getHeight() - 1), "column-1 block reached the floor");
	}

	@Test
	void freeFallReturnsFalseWhenNothingToMove() {
		Field f = new Field(3, 4, 0, false);
		// Solid floor row, nothing floats.
		for (int x = 0; x < 3; x++) {
			put(f, x, 3, Block.BLOCK_COLOR_RED);
		}
		assertFalse(f.freeFall(), "settled field does not move");
	}

	// ══════════════════════════════════════════════════════════════════════
	// garbageDrop — L2503 avoidColumn guard: cover the out-of-range arm
	// (avoidColumn < 0) in the high-drop (> half width) path.
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void garbageDropHighDropWithNegativeAvoidColumn() {
		Field f = new Field(10, 4, 0, false);
		// drop=9 > width/2 takes the "fill all then remove" branch; avoidColumn=-1
		// makes (avoidColumn >= 0 && avoidColumn < actualWidth) take its FALSE arm
		// so no column is exempted.
		f.garbageDrop(createEngine(), 9, false, 0, 0, -1, Block.BLOCK_COLOR_GRAY);
		int filled = 0;
		for (int x = 0; x < 10; x++) {
			if (!f.getBlockEmpty(x, 0)) {
				filled++;
			}
		}
		assertEquals(9, filled, "high-drop with no avoid column fills exactly 9 cells");
	}

	@Test
	void garbageDropHighDropExactlyHalfPlusOne() {
		Field f = new Field(10, 4, 0, false);
		// drop=6 (> 5 == width>>1) high path, valid avoid column 4.
		f.garbageDrop(createEngine(), 6, false, 0, 0, 4, Block.BLOCK_COLOR_GRAY);
		assertTrue(f.getBlockEmpty(4, 0), "the avoided column stays empty");
		int filled = 0;
		for (int x = 0; x < 10; x++) {
			if (!f.getBlockEmpty(x, 0)) {
				filled++;
			}
		}
		assertEquals(6, filled, "high-drop fills exactly 6 cells");
	}

	// ══════════════════════════════════════════════════════════════════════
	// getSecretGrade — L1303 for(i = height-1; i > 0; i--): cover the loop
	// running to completion (i > 0 becoming false) by building a full
	// "secret grade" staircase that never trips an early break.
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getSecretGradeFullStaircaseExhaustsLoop() {
		// Use a small field so we can fill the whole staircase deterministically.
		int w = 6;
		int h = 6;
		Field f = new Field(w, h, 0, false);
		// For each row i (height-1 .. 1) the algorithm computes a single hole
		// column holeLoc and requires every other column filled, plus the same
		// hole column filled on row i-1. Fill every cell, then carve the holes.
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				put(f, x, y, Block.BLOCK_COLOR_RED);
			}
		}
		for (int i = h - 1; i > 0; i--) {
			int holeLoc = -Math.abs(i - (h / 2)) + (h / 2) - 1;
			if (holeLoc >= 0 && holeLoc < w) {
				f.setBlockColor(holeLoc, i, Block.BLOCK_COLOR_NONE);
			}
		}
		int grade = f.getSecretGrade();
		// Every row 5..1 satisfies rowCheck (no early break), so the loop runs to
		// completion (i > 0 becomes false) and all 5 rows are counted.
		assertEquals(h - 1, grade, "full staircase counts every scanned row");
	}

	// ══════════════════════════════════════════════════════════════════════
	// checkForSquares — L1446/L1507: cover the rootBlk-is-already-a-square
	// FALSE arm (a field already containing a gold square block is rejected
	// as the root of a new square).
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void checkForSquaresRejectsExistingGoldSquareRoot() {
		Field f = new Field(5, 5, 0, false);
		// Make (0,0) already a gold-square block so rootBlk.isGoldSquareBlock()
		// is true -> the L1446 guard takes its FALSE arm and the 4x4 scan is
		// skipped for that root.
		put(f, 0, 0, Block.BLOCK_COLOR_SQUARE_GOLD_1);
		int[] squares = f.checkForSquares();
		assertEquals(0, squares[0], "no new gold square formed from an existing square root");
		assertEquals(0, squares[1], "no silver square either");
	}

	@Test
	void checkForSquaresFormsGoldSquareFromMonochromeBlock() {
		Field f = new Field(5, 5, 0, false);
		// A solid 4x4 monochrome region (no connections, no garbage) -> one gold square.
		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 4; x++) {
				put(f, x, y, Block.BLOCK_COLOR_RED);
			}
		}
		int[] squares = f.checkForSquares();
		assertEquals(1, squares[0], "monochrome 4x4 forms exactly one gold square");
		assertTrue(f.getBlock(0, 0).isGoldSquareBlock(), "top-left became a gold square block");
	}
}
