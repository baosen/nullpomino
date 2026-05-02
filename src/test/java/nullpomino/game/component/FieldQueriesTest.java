package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

/**
 * Pins Field's terrain-query and bulk-mutation contracts. Modes lean on
 * these helpers (block counts, hole/valley metrics, garbage placement,
 * hurry-up floor) for scoring and for AI-style heuristics, so they need
 * to keep their shape across refactors of the grid internals.
 */
class FieldQueriesTest {

	private static Field newField() {
		return new Field(10, 20, 3, false);
	}

	@Test
	void getHowManyBlocksCountsOnlyNonEmptyAndSkipsFlaggedRows() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 18, Block.BLOCK_COLOR_BLUE);

		assertEquals(3, f.getHowManyBlocks());

		f.setLineFlag(19, true);
		assertEquals(1, f.getHowManyBlocks(),
				"blocks on flagged rows must not be counted");
	}

	@Test
	void getHowManyBlocksFromLeftStopsAtFirstGap() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 19, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_BLUE);

		assertEquals(3, f.getHowManyBlocksFromLeft());
	}

	@Test
	void getHowManyBlocksFromRightStopsAtFirstGap() {
		Field f = newField();
		f.setBlockColor(9, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(8, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 19, Block.BLOCK_COLOR_BLUE);

		assertEquals(2, f.getHowManyBlocksFromRight());
	}

	@Test
	void getHighestBlockYReturnsTopmostFilledRowOrFieldHeightIfEmpty() {
		Field f = newField();
		assertEquals(f.getHeight(), f.getHighestBlockY());

		f.setBlockColor(0, 15, Block.BLOCK_COLOR_RED);
		assertEquals(15, f.getHighestBlockY());

		f.setBlockColor(5, 8, Block.BLOCK_COLOR_BLUE);
		assertEquals(8, f.getHighestBlockY());
	}

	@Test
	void getHighestBlockYByColumnReturnsTopmostBlockInThatColumnOnly() {
		Field f = newField();
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 17, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 12, Block.BLOCK_COLOR_BLUE);

		assertEquals(17, f.getHighestBlockY(3));
		assertEquals(12, f.getHighestBlockY(7));
		assertEquals(f.getHeight(), f.getHighestBlockY(0));
	}

	@Test
	void isHoleBelowReportsTrueOnlyWhenNonEmptyAboveEmpty() {
		Field f = newField();
		f.setBlockColor(2, 5, Block.BLOCK_COLOR_RED);

		assertTrue(f.isHoleBelow(2, 5));
		assertFalse(f.isHoleBelow(2, 4));
		assertFalse(f.isHoleBelow(3, 5));
	}

	@Test
	void getHowManyHolesCountsEmptyCellsCoveredByABlockAbove() {
		Field f = newField();
		// Column 0: filled at y=17 then empty at y=18, y=19 → 2 holes
		f.setBlockColor(0, 17, Block.BLOCK_COLOR_RED);
		// Column 2: filled at y=17 and y=18, empty at y=19 → 1 hole
		f.setBlockColor(2, 17, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 18, Block.BLOCK_COLOR_RED);

		assertEquals(3, f.getHowManyHoles());
	}

	@Test
	void getHowManyLidAboveHolesCountsBlocksStackedOverGaps() {
		Field f = newField();
		// Stack two blocks above a hole, then one block sealing the bottom
		f.setBlockColor(0, 16, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 17, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_BLUE);

		assertTrue(f.getHowManyLidAboveHoles() >= 2,
				"two blocks should be counted as lid above the y=18 hole");
	}

	@Test
	void getValleyDepthMeasuresWellBetweenAdjacentColumnWalls() {
		Field f = newField();
		for (int y = 17; y <= 19; y++) {
			f.setBlockColor(4, y, Block.BLOCK_COLOR_RED);
			f.setBlockColor(6, y, Block.BLOCK_COLOR_RED);
		}

		assertEquals(3, f.getValleyDepth(5));
		assertEquals(3, f.getTotalValleyDepth(),
				"only the column-5 valley contributes (depth >= 2)");
		assertEquals(1, f.getTotalValleyNeedIPiece(),
				"depth-3 valley counts as needing an I-piece");
	}

	@Test
	void getValleyDepthTreatsEdgeColumnsAsBoundedByImplicitWall() {
		Field f = newField();
		// Wall on x=1 only — col 0 is bounded by the field edge on the left
		for (int y = 17; y <= 19; y++) {
			f.setBlockColor(1, y, Block.BLOCK_COLOR_RED);
		}

		assertEquals(3, f.getValleyDepth(0));
	}

	@Test
	void toStringRendersOneRowPerLineAndMapsGemColorsToPlus() {
		Field f = new Field(4, 3, 1);
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 2, Block.BLOCK_COLOR_GEM_ORANGE);

		String s = f.toString();

		assertTrue(s.contains("Field"), "should include class header");
		// y=0 row: "  0:2000\n" — RED is color 2 at x=0
		assertTrue(s.contains("  0:2000"), "RED at (0,0) must show as digit 2");
		// GEM_ORANGE is color 10 → '+'
		assertTrue(s.contains("00+0"), "gem color (>=10) must show as '+'");
	}

	@Test
	void pushUpWithExplicitLinesShiftsRowsUpwardAndClearsBottom() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);

		f.pushUp(3);

		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 16));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
	}

	@Test
	void pushDownWithExplicitLinesShiftsRowsDownwardAndClearsTop() {
		Field f = newField();
		f.setBlockColor(0, -3, Block.BLOCK_COLOR_BLUE);

		f.pushDown(2);

		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, -1));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, -3));
	}

	@Test
	void cutLineRemovesTargetRowAndShiftsHigherRowsDown() {
		Field f = newField();
		f.setBlockColor(0, 17, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_BLUE);
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GREEN);

		f.cutLine(18, 1);

		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 18),
				"row 17 must drop into row 18 once row 18 is cut");
		assertEquals(Block.BLOCK_COLOR_GREEN, f.getBlockColor(0, 19),
				"bottom row is preserved by cutLine");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 17));
	}

	@Test
	void addSingleHoleGarbageWithoutAttributesSetsConnectionAndGarbageFlags() {
		Field f = newField();

		f.addSingleHoleGarbage(3, Block.BLOCK_COLOR_GRAY, 0, 2);

		for (int y = 18; y <= 19; y++) {
			assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(3, y),
					"hole column 3 must remain empty (y=" + y + ")");
			Block left = f.getBlock(0, y);
			assertNotNull(left);
			assertEquals(Block.BLOCK_COLOR_GRAY, left.color);
			assertTrue(left.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
			assertTrue(left.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		}
		// neighboring blocks across the hole get connection bits per side
		assertTrue(f.getBlock(2, 19).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT));
		assertFalse(f.getBlock(2, 19).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT),
				"block immediately left of the hole has no right neighbour");
	}

	@Test
	void addSingleHoleGarbageWithExplicitAttributesUsesThemVerbatim() {
		Field f = newField();

		f.addSingleHoleGarbage(2, Block.BLOCK_COLOR_RED, 1,
				Block.BLOCK_ATTRIBUTE_VISIBLE, 1);

		Block b = f.getBlock(0, 19);
		assertNotNull(b);
		assertEquals(Block.BLOCK_COLOR_RED, b.color);
		assertEquals(1, b.skin);
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE),
				"explicit attribute argument must replace, not OR-in, garbage flag");
	}

	@Test
	void addBottomCopyGarbageMirrorsBottomRowShape() {
		Field f = newField();
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(8, 19, Block.BLOCK_COLOR_RED);

		f.addBottomCopyGarbage(Block.BLOCK_COLOR_GRAY, 0, 0, 1);

		assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(1, 19));
		assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(4, 19));
		assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(8, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		// original bottom shifted up by one
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(1, 18));
	}

	@Test
	void addHurryupFloorAddsWallRowsAtBottomAndShrinksEffectiveHeight() {
		Field f = newField();
		int beforeHeight = f.getHeightWithoutHurryupFloor();

		f.addHurryupFloor(2, 0);

		assertEquals(2, f.getHurryupFloorLines());
		assertEquals(beforeHeight - 2, f.getHeightWithoutHurryupFloor());

		Block b = f.getBlock(0, 19);
		assertNotNull(b);
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_WALL));
		assertEquals(Block.BLOCK_COLOR_GRAY, b.color);
	}

	@Test
	void setAllAttributeFlipsAttributeOnEveryBlockIncludingEmptyCells() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);

		f.setAllAttribute(Block.BLOCK_ATTRIBUTE_BONE, true);

		assertTrue(f.getBlock(0, 19).getAttribute(Block.BLOCK_ATTRIBUTE_BONE));
		assertTrue(f.getBlock(5, 5).getAttribute(Block.BLOCK_ATTRIBUTE_BONE));

		f.setAllAttribute(Block.BLOCK_ATTRIBUTE_BONE, false);
		assertFalse(f.getBlock(0, 19).getAttribute(Block.BLOCK_ATTRIBUTE_BONE));
	}

	@Test
	void setAllSkinUpdatesSkinAcrossEveryCellIncludingHiddenRows() {
		Field f = newField();

		f.setAllSkin(7);

		assertEquals(7, f.getBlock(0, 0).skin);
		assertEquals(7, f.getBlock(9, 19).skin);
		assertEquals(7, f.getBlock(0, -1).skin);
	}

	@Test
	void getHowManyGemsCountsOnlyGemColors() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_GEM_BLUE);

		assertEquals(2, f.getHowManyGems());
	}

	@Test
	void getHowManyGemClearsCountsGemsOnlyOnFlaggedRows() {
		Field f = newField();
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_GEM_RED);
		f.setLineFlag(19, true);

		assertEquals(1, f.getHowManyGemClears());
	}

	@Test
	void getItemClearsRecordsItemsOnlyOnFlaggedRows() {
		Field f = newField();
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.item = Block.BLOCK_ITEM_RANDOM;
		f.setBlock(2, 19, b);

		boolean[] notFlagged = f.getItemClears();
		assertFalse(notFlagged[Block.BLOCK_ITEM_RANDOM],
				"items must not register until the row is flagged");

		f.setLineFlag(19, true);
		boolean[] flagged = f.getItemClears();
		assertTrue(flagged[Block.BLOCK_ITEM_RANDOM]);
		assertFalse(flagged[Block.BLOCK_ITEM_NONE]);
	}

	@Test
	void getTSlotLineClearReturnsTwoForTSpinDoubleSetup() {
		Field f = newField();
		// Three filled corners at (4,10), (6,10), (4,12) — the 4th corner stays open
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill rows 11 and 12 outside the T's column band so the side cells are full
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 11, Block.BLOCK_COLOR_BLUE);
				f.setBlockColor(x, 12, Block.BLOCK_COLOR_BLUE);
			}
		}

		assertTrue(f.isTSlot(4, 10, false));
		assertEquals(2, f.getTSlotLineClear(4, 10, false));
	}

	@Test
	void getTSlotLineClearAllSumsAcrossField() {
		Field f = newField();
		assertEquals(0, f.getTSlotLineClearAll(false));
		assertEquals(0, f.getTSlotLineClearAll(false, 2));
	}

	@Test
	void getTSlotLineClearAllReturnsTwoForFieldWithOneTSlotAndBothSideRowsFilled() {
		Field f = newField();
		// Three corners at (4,10),(6,10),(4,12) — classic T-spin double setup
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill rows 11 and 12 outside the T-span (columns 0-3 and 7-9)
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 11, Block.BLOCK_COLOR_BLUE);
				f.setBlockColor(x, 12, Block.BLOCK_COLOR_BLUE);
			}
		}

		int total = f.getTSlotLineClearAll(false);
		assertEquals(2, total, "one T-spin double contributes 2 to the total");
	}

	@Test
	void getTSlotLineClearWithGapInSideRowReducesCountToOne() {
		// T-slot at (4,10) with 3 corners.  Row 11 sides fully filled, but row 12
		// has a gap at column 0 — getBlockEmptyF(0, 12) returns true and sets
		// lineflag[1]=false (the "gap in side row" branch).  Result = 1.
		Field f = newField();
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		// Fill row 11 sides completely
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 11, Block.BLOCK_COLOR_BLUE);
			}
		}
		// Fill row 12 sides but leave column 0 empty → gap triggers lineflag[1]=false
		for (int x = 1; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 12, Block.BLOCK_COLOR_BLUE);
			}
		}

		assertEquals(1, f.getTSlotLineClear(4, 10, false),
				"gap in the side of row 12 must reduce the clear count to 1");
	}

	@Test
	void getTSlotLineClearAllWithMinimumFiltersOutSlotsBelow() {
		// Same double-clear setup as above; minimum=2 passes the guard so
		// 'result += temp' (the minimum-met branch) is exercised.
		Field f = newField();
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		for (int x = 0; x < f.getWidth(); x++) {
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 11, Block.BLOCK_COLOR_BLUE);
				f.setBlockColor(x, 12, Block.BLOCK_COLOR_BLUE);
			}
		}

		assertEquals(2, f.getTSlotLineClearAll(false, 2),
				"slot with 2 clear lines meets minimum=2 and must be counted");
		assertEquals(0, f.getTSlotLineClearAll(false, 3),
				"slot with 2 lines does not meet minimum=3 and must not be counted");
	}

	@Test
	void getSecretGradeIgnoresRowsWithGapsOutsideHoleColumn() {
		// height=20 → at i=19, holeLoc=0
		// Row 19 has holeLoc=0 empty, block above (0,18) filled, but column 5 also empty
		// → rowCheck becomes false → row doesn't count
		Field f = newField();
		for (int x = 1; x < f.getWidth(); x++) {
			if (x != 5) {
				f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
			}
		}
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);  // above-hole block

		assertEquals(0, f.getSecretGrade(), "row with extra gap must not count");
	}

	@Test
	void getLastLinesAsTGMAttackReplacesLastCommitWithEmptyBlocks() {
		Field f = newField();
		f.lastLinesCleared = new ArrayList<Block[]>();
		Block[] row = new Block[f.getWidth()];
		for (int i = 0; i < row.length; i++) {
			Block b = new Block(Block.BLOCK_COLOR_RED);
			if (i == 0) b.setAttribute(Block.BLOCK_ATTRIBUTE_LAST_COMMIT, true);
			row[i] = b;
		}
		f.lastLinesCleared.add(row);

		ArrayList<Block[]> attack = f.getLastLinesAsTGMAttack();

		assertEquals(1, attack.size());
		assertTrue(attack.get(0)[0].isEmpty(),
				"LAST_COMMIT cells become empty in TGM attack form");
		assertEquals(Block.BLOCK_COLOR_RED, attack.get(0)[1].color);
	}

	@Test
	void getSecretGradeCountsRowsThatMatchMirroredHolePattern() {
		// height=20 → at i=19, holeLoc = -|19-10| + 10 - 1 = 0
		// row 19 needs (0,19) empty, (0,18) filled, every other column in row 19 filled
		Field f = newField();
		for (int x = 1; x < f.getWidth(); x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);

		assertEquals(1, f.getSecretGrade());
	}

	@Test
	void canCascadeIsFalseWhenIsolatedBlockSitsOnFloor() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);

		assertFalse(f.canCascade());
	}

	@Test
	void canCascadeIsTrueWhenIsolatedBlockSitsOverEmptySpace() {
		Field f = newField();
		f.setBlockColor(0, 5, Block.BLOCK_COLOR_RED);

		assertTrue(f.canCascade());
	}

	@Test
	void setBlockLinkByColorMarksAdjacentSameColorBlocksAsConnected() {
		Field f = newField();
		f.setBlockColor(3, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 19, Block.BLOCK_COLOR_BLUE);

		f.setBlockLinkByColor();

		assertTrue(f.getBlock(3, 19).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP),
				"red below should connect up to the red above");
		assertFalse(f.getBlock(3, 19).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT),
				"red must not connect to the adjacent blue");
	}

	@Test
	void setBlockLinkByColorConnectsHorizontalSameColorNeighbors() {
		// Covers Field lines 2133-2134 (CONNECT_LEFT) and 2139-2140 (CONNECT_RIGHT)
		// in setBlockLinkByColorSub — the horizontal-neighbour branches that are only
		// reached when two same-color blocks share a row.
		Field f = newField();
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 19, Block.BLOCK_COLOR_RED);

		f.setBlockLinkByColor(3, 19);

		assertTrue(f.getBlock(3, 19).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT),
				"left block must connect right to its same-color neighbour");
		assertTrue(f.getBlock(4, 19).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT),
				"right block must connect left back to the originating cell");
	}

	@Test
	void setBlockLinkBrokenWalksConnectedNormalBlocks() {
		Field f = newField();
		f.setBlockColor(3, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);
		f.getBlock(3, 18).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);
		f.getBlock(3, 19).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);

		f.setBlockLinkBroken(3, 18);

		assertTrue(f.getBlock(3, 18).getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
		assertTrue(f.getBlock(3, 19).getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN),
				"connected neighbour must also be marked broken");
	}

	@Test
	void checkBlockLinkSetsTempMarkOnConnectedComponent() {
		Field f = newField();
		f.setBlockColor(3, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.getBlock(3, 18).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);
		f.getBlock(3, 19).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);

		f.checkBlockLink(3, 18);

		assertTrue(f.getBlock(3, 18).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK));
		assertTrue(f.getBlock(3, 19).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK));
		assertFalse(f.getBlock(5, 19).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK),
				"unconnected blocks of the same color must not be marked");
	}
}
