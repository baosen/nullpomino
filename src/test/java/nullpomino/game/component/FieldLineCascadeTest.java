package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.randomizer.Randomizer;
import nullpomino.game.wallkick.Wallkick;

/**
 * Tests for Field line-manipulation, block-drop, hover-block, and
 * terrain-analysis methods not covered by other Field test classes.
 *
 * <p>Methods tested:
 * <ul>
 *   <li>{@link Field#downFloatingBlocksSingleLine()}</li>
 *   <li>{@link Field#getLines()}</li>
 *   <li>{@link Field#stringToRow(String)}</li>
 *   <li>{@link Field#garbageDrop(GameEngine, int, boolean)}</li>
 *   <li>{@link Field#addRandomHoverBlocks(GameEngine, int, int[], int, boolean)}</li>
 *   <li>{@link Field#getHowManyLidAboveHoles()}</li>
 *   <li>{@link Field#getTotalValleyDepth()}</li>
 *   <li>{@link Field#getTotalValleyNeedIPiece()}</li>
 * </ul>
 */
class FieldLineCascadeTest {

	// ─── helpers ──────────────────────────────────────────────

	/** Create a field with no hidden height for predictable row indices. */
	private static Field field(int width, int height) {
		return new Field(width, height, 0, false);
	}

	private static Field newField() {
		return new Field(10, 20, 3, false);
	}

	private static GameEngine createEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		GameEngine engine = new GameEngine(gm, 0, new RuleOptions(), null, null);
		engine.random = new Random(42);
		return engine;
	}

	// ──────────────────────────────────────────────────────────
	// downFloatingBlocksSingleLine
	// ──────────────────────────────────────────────────────────

	@Test
	void downFloatingBlocksSingleLineReplacesFlaggedLineWithRowAbove() {
		Field f = field(6, 6);
		for (int x = 0; x < 6; x++) {
			f.setBlockColor(x, 5, Block.BLOCK_COLOR_RED);  // bottom, flagged
			f.setBlockColor(x, 4, Block.BLOCK_COLOR_BLUE); // above
		}
		f.setLineFlag(5, true);

		f.downFloatingBlocksSingleLine();

		for (int x = 0; x < 6; x++) {
			assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(x, 5),
					"flagged bottom row replaced by row above");
			assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(x, 4),
					"row above becomes empty after shift");
		}
	}

	@Test
	void downFloatingBlocksSingleLineDoesNothingWhenNoLineIsFlagged() {
		Field f = field(6, 6);
		f.setBlockColor(3, 5, Block.BLOCK_COLOR_RED);

		f.downFloatingBlocksSingleLine();

		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(3, 5));
	}

	@Test
	void downFloatingBlocksSingleLineOnlyActsOnBottommostFlaggedLine() {
		Field f = field(6, 6);
		for (int x = 0; x < 6; x++) {
			f.setBlockColor(x, 5, Block.BLOCK_COLOR_RED);   // bottom, flagged
			f.setBlockColor(x, 4, Block.BLOCK_COLOR_BLUE);  // middle, flagged
			f.setBlockColor(x, 3, Block.BLOCK_COLOR_GREEN); // top, not flagged
		}
		f.setLineFlag(4, true);
		f.setLineFlag(5, true);

		f.downFloatingBlocksSingleLine();

		// dropRowsAbove(5) copies row 4->5, row 3->4, row 2->3, etc., then clears row 0.
		for (int x = 0; x < 6; x++) {
			assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(x, 5),
					"bottom flagged row replaced by row 4");
			assertEquals(Block.BLOCK_COLOR_GREEN, f.getBlockColor(x, 4),
					"row 4 gets what was in row 3 after shift");
			assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(x, 3),
					"row 3 becomes empty after everything shifts down");
		}
	}

	@Test
	void downFloatingBlocksSingleLineWithMixedFlaggedAndSettledLines() {
		Field f = field(6, 6);
		for (int x = 0; x < 6; x++) {
			f.setBlockColor(x, 5, Block.BLOCK_COLOR_RED);   // bottom, flagged
			f.setBlockColor(x, 4, Block.BLOCK_COLOR_BLUE);  // settled
			f.setBlockColor(x, 3, Block.BLOCK_COLOR_GREEN); // settled
		}
		f.setLineFlag(5, true);

		f.downFloatingBlocksSingleLine();

		// dropRowsAbove(5) copies row 4->5, row 3->4, row 2->3, ..., then clears row 0
		for (int x = 0; x < 6; x++) {
			assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(x, 5),
					"flagged bottom row replaced by row 4 above");
		}
		// Row 3 now holds what row 2 held (which was empty)
		for (int x = 0; x < 6; x++) {
			assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(x, 3),
					"row 3 becomes empty after shift");
		}
	}

	// ──────────────────────────────────────────────────────────
	// getLines
	// ──────────────────────────────────────────────────────────

	@Test
	void getLinesReturnsZeroWhenNoLinesAreFlagged() {
		Field f = newField();
		assertEquals(0, f.getLines());
	}

	@Test
	void getLinesCountsSomeFlaggedLines() {
		Field f = field(6, 6);
		f.setLineFlag(1, true);
		f.setLineFlag(3, true);
		f.setLineFlag(5, true);
		assertEquals(3, f.getLines());
	}

	@Test
	void getLinesCountsAllVisibleRowsWhenAllAreFlagged() {
		Field f = field(4, 4);
		for (int y = 0; y < 4; y++) {
			f.setLineFlag(y, true);
		}
		assertEquals(4, f.getLines());
	}

	// ──────────────────────────────────────────────────────────
	// stringToRow (1-arg convenience)
	// ──────────────────────────────────────────────────────────

	@Test
	void stringToRowParsesCharacterEncodedColors() {
		Field f = field(6, 6);
		Block[] row = f.stringToRow("270");

		assertEquals(6, row.length);
		assertEquals(Block.BLOCK_COLOR_RED, row[0].color);   // '2'
		assertEquals(Block.BLOCK_COLOR_BLUE, row[1].color);  // '7'
		assertEquals(Block.BLOCK_COLOR_NONE, row[2].color);  // '0'
		// Remaining cells default to empty
		assertEquals(Block.BLOCK_COLOR_NONE, row[3].color);
		assertEquals(Block.BLOCK_COLOR_NONE, row[4].color);
		assertEquals(Block.BLOCK_COLOR_NONE, row[5].color);
	}

	@Test
	void stringToRowEmptyStringYieldsAllEmptyCells() {
		Field f = field(4, 4);
		Block[] row = f.stringToRow("");

		assertEquals(4, row.length);
		for (Block b : row) {
			assertEquals(Block.BLOCK_COLOR_NONE, b.color);
		}
	}

	@Test
	void stringToRowDecodesAllCommonBlockTypes() {
		Field f = field(10, 10);
		Block[] row = f.stringToRow("1259a");

		assertEquals(10, row.length);
		assertEquals(Block.BLOCK_COLOR_GRAY, row[0].color);       // '1'
		assertEquals(Block.BLOCK_COLOR_RED, row[1].color);        // '2'
		assertEquals(Block.BLOCK_COLOR_GREEN, row[2].color);      // '5'
		assertEquals(Block.BLOCK_COLOR_GEM_RED, row[3].color);    // '9'
		assertEquals(Block.BLOCK_COLOR_GEM_ORANGE, row[4].color); // 'a'
	}

	// ──────────────────────────────────────────────────────────
	// garbageDrop (full-row path avoids engine.random)
	// ──────────────────────────────────────────────────────────

	@Test
	void garbageDropFillsSingleFullRowWhenDropEqualsWidth() {
		Field f = new Field(10, 4, 0, false);
		GameEngine engine = createEngine();

		f.garbageDrop(engine, 10, false);

		for (int x = 0; x < 10; x++) {
			assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(x, 0),
					"garbage block uses GRAY color by default");
			assertTrue(f.getBlock(x, 0).getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE),
					"block is marked as garbage");
		}
	}

	@Test
	void garbageDropPlacesMultipleFullRowsWhenDropIsMultipleOfWidth() {
		Field f = new Field(10, 4, 0, false);
		GameEngine engine = createEngine();

		f.garbageDrop(engine, 20, false);

		// Two rows of garbage at y=0 and y=1
		for (int y = 0; y < 2; y++) {
			for (int x = 0; x < 10; x++) {
				assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(x, y));
			}
		}
		// Rows 2 and 3 untouched
		for (int x = 0; x < 10; x++) {
			assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(x, 2));
			assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(x, 3));
		}
	}

	@Test
	void garbageDropWithZeroDropDoesNothing() {
		Field f = new Field(10, 4, 0, false);
		GameEngine engine = createEngine();

		f.garbageDrop(engine, 0, false);

		for (int y = 0; y < 4; y++) {
			for (int x = 0; x < 10; x++) {
				assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(x, y));
			}
		}
	}

	// ──────────────────────────────────────────────────────────
	// addRandomHoverBlocks
	// ──────────────────────────────────────────────────────────

	@Test
	void addRandomHoverBlocksPlacesRequestedCountOfHoverBlocks() {
		Field f = new Field(10, 10, 0, false);
		GameEngine engine = createEngine();
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};

		f.addRandomHoverBlocks(engine, 10, colors, 0, false);

		int nonEmpty = 0;
		for (int y = 0; y < 10; y++) {
			for (int x = 0; x < 10; x++) {
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) {
					nonEmpty++;
					assertTrue(f.getBlock(x, y).getAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY),
							"each placed block has ANTIGRAVITY attribute");
				}
			}
		}
		assertEquals(10, nonEmpty,
				"exactly 10 hover blocks placed in a 10x10 field");
	}

	@Test
	void addRandomHoverBlocksWithSeededRandomIsDeterministic() {
		Field f1 = new Field(10, 10, 0, false);
		GameEngine e1 = createEngine();
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};
		f1.addRandomHoverBlocks(e1, 10, colors, 0, false);

		Field f2 = new Field(10, 10, 0, false);
		GameEngine e2 = createEngine();
		f2.addRandomHoverBlocks(e2, 10, colors, 0, false);

		for (int y = 0; y < 10; y++) {
			for (int x = 0; x < 10; x++) {
				assertEquals(f1.getBlockColor(x, y), f2.getBlockColor(x, y),
						"same seed yields same layout at (" + x + "," + y + ")");
			}
		}
	}

	@Test
	void addRandomHoverBlocksRespectsMinYOffset() {
		Field f = new Field(10, 10, 0, false);
		GameEngine engine = createEngine();
		int[] colors = {Block.BLOCK_COLOR_RED};

		// Place 5 hover blocks starting at y=5 (bottom half only)
		f.addRandomHoverBlocks(engine, 5, colors, 5, false);

		// Count blocks in bottom half (y>=5)
		int bottomHalf = 0;
		int topHalf = 0;
		for (int y = 0; y < 10; y++) {
			for (int x = 0; x < 10; x++) {
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) {
					if (y >= 5) bottomHalf++;
					else topHalf++;
				}
			}
		}
		assertEquals(5, bottomHalf,
				"all hover blocks placed at y >= minY");
		assertEquals(0, topHalf,
				"no hover blocks placed above minY");
	}

	// ──────────────────────────────────────────────────────────
	// getHowManyLidAboveHoles
	// ──────────────────────────────────────────────────────────

	@Test
	void getHowManyLidAboveHolesReturnsZeroForFlatSurface() {
		Field f = field(10, 10);
		for (int x = 0; x < 10; x++) {
			f.setBlockColor(x, 9, Block.BLOCK_COLOR_RED);
		}
		assertEquals(0, f.getHowManyLidAboveHoles());
	}

	@Test
	void getHowManyLidAboveHolesReturnsZeroForEmptyField() {
		Field f = field(10, 10);
		assertEquals(0, f.getHowManyLidAboveHoles());
	}

	@Test
	void getHowManyLidAboveHolesCountsBlocksAboveHolesInSingleColumn() {
		Field f = field(10, 10);
		// Column 0: block at row 9, empty row 8 (hole below row 9),
		//           blocks at rows 7 and 5 (lids above the hole)
		f.setBlockColor(0, 9, Block.BLOCK_COLOR_RED);  // bottom
		// row 8 empty = hole below row 9
		f.setBlockColor(0, 7, Block.BLOCK_COLOR_RED);  // lid 1
		// row 6 empty
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);  // lid 2

		assertEquals(2, f.getHowManyLidAboveHoles());
	}

	@Test
	void getHowManyLidAboveHolesAccumulatesAcrossColumns() {
		Field f = field(10, 10);
		// Column 0: block at row 9, empty row 8 (hole), block at row 7 (1 lid)
		f.setBlockColor(0, 9, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 7, Block.BLOCK_COLOR_RED);

		// Column 1: block at row 9, empty row 8 (hole), block at row 7, block at row 6 (2 lids)
		f.setBlockColor(1, 9, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 6, Block.BLOCK_COLOR_RED);

		assertEquals(3, f.getHowManyLidAboveHoles()); // 1 + 2
	}

	@Test
	void getHowManyLidAboveHolesIgnoresBlocksBelowOrAtSameRowAsHole() {
		Field f = field(10, 10);
		// Column 0: block at row 9, empty row 8 (hole) — block at row 9 is NOT a lid
		f.setBlockColor(0, 9, Block.BLOCK_COLOR_RED);
		// row 8 empty
		// No blocks above the hole
		assertEquals(0, f.getHowManyLidAboveHoles());
	}

	// ──────────────────────────────────────────────────────────
	// getTotalValleyDepth
	// ──────────────────────────────────────────────────────────

	@Test
	void getTotalValleyDepthReturnsZeroForFlatSurface() {
		Field f = field(10, 10);
		for (int x = 0; x < 10; x++) {
			f.setBlockColor(x, 9, Block.BLOCK_COLOR_RED);
		}
		assertEquals(0, f.getTotalValleyDepth());
	}

	@Test
	void getTotalValleyDepthReturnsZeroForEmptyField() {
		Field f = field(10, 10);
		assertEquals(0, f.getTotalValleyDepth());
	}

	@Test
	void getTotalValleyDepthCountsSingleValleyOfDepthExactlyTwo() {
		Field f = field(10, 10);
		// Bottom row full
		for (int x = 0; x < 10; x++) {
			f.setBlockColor(x, 9, Block.BLOCK_COLOR_RED);
		}
		// Walls at columns 4 and 6 raised to row 7 → valley in col 5, depth 2
		f.setBlockColor(4, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 7, Block.BLOCK_COLOR_RED);
		// Column 5 stays empty at rows 7 and 8 (depth = 2)

		assertEquals(2, f.getTotalValleyDepth());
	}

	@Test
	void getTotalValleyDepthAccumulatesMultipleValleys() {
		Field f = field(10, 10);
		// Bottom row full
		for (int x = 0; x < 10; x++) {
			f.setBlockColor(x, 9, Block.BLOCK_COLOR_RED);
		}
		// Valley at col 3, depth 2: walls at cols 2 and 4 raised
		f.setBlockColor(2, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		// Valley at col 6, depth 2: walls at cols 5 and 7 raised
		f.setBlockColor(5, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 7, Block.BLOCK_COLOR_RED);

		assertEquals(4, f.getTotalValleyDepth()); // 2 + 2
	}

	@Test
	void getTotalValleyDepthSkipsShallowValleysOfDepthLessThanTwo() {
		Field f = field(10, 10);
		// Bottom row full
		for (int x = 0; x < 10; x++) {
			f.setBlockColor(x, 9, Block.BLOCK_COLOR_RED);
		}
		// Valley at col 3, depth 1 (walls raised only one row above bottom)
		f.setBlockColor(2, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 8, Block.BLOCK_COLOR_RED);
		// Col 3 empty at row 8, but row 9 has a block → depth = 1, skipped
		// Valley at col 6, depth 2: walls at cols 5 and 7 raised two rows
		f.setBlockColor(5, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 7, Block.BLOCK_COLOR_RED);

		assertEquals(2, f.getTotalValleyDepth()); // only depth-2 valley at col 6 counted
	}

	// ──────────────────────────────────────────────────────────
	// getTotalValleyNeedIPiece
	// ──────────────────────────────────────────────────────────

	@Test
	void getTotalValleyNeedIPieceReturnsZeroWhenNoValleysAreDeepEnough() {
		Field f = field(10, 10);
		for (int x = 0; x < 10; x++) {
			f.setBlockColor(x, 9, Block.BLOCK_COLOR_RED);
		}
		assertEquals(0, f.getTotalValleyNeedIPiece());
	}

	@Test
	void getTotalValleyNeedIPieceCountsMultipleDeepValleys() {
		Field f = field(10, 10);
		// Bottom row full
		for (int x = 0; x < 10; x++) {
			f.setBlockColor(x, 9, Block.BLOCK_COLOR_RED);
		}
		// Valley at col 3, depth 3: walls at cols 2 and 4 raised to row 6
		f.setBlockColor(2, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 6, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 6, Block.BLOCK_COLOR_RED);

		// Valley at col 6, depth 3: walls at cols 5 and 7 raised to row 6
		f.setBlockColor(5, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 8, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 7, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 6, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 6, Block.BLOCK_COLOR_RED);

		assertEquals(2, f.getTotalValleyNeedIPiece()); // both valleys are >= depth 3
	}
}
