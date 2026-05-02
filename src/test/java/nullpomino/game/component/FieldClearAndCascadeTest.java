package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

import nullpomino.game.play.GameEngine;

/**
 * Pins Field's color-clear, square-detection, and cascade contracts.
 * Avalanche, color-clear, and square modes all build on these helpers,
 * so the recursive flood-fill and cascade-fall behavior needs to stay
 * intact across refactors of the Field grid internals.
 */
class FieldClearAndCascadeTest {

	private static Field newField() {
		return new Field(10, 20, 3, false);
	}

	@Test
	void checkForSquaresFindsFourByFourMonoColorBlockAsGoldSquare() {
		Field f = newField();
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
			}
		}

		int[] squares = f.checkForSquares();

		assertArrayEquals(new int[] {1, 0}, squares);
		assertTrue(f.getBlock(0, 0).isGoldSquareBlock(),
				"upper-left of detected square turns into gold");
		assertTrue(f.getBlock(3, 3).isGoldSquareBlock());
	}

	@Test
	void checkForSquaresFindsFourByFourMultiColorBlockAsSilverSquare() {
		Field f = newField();
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				int color = ((x + y) % 2 == 0) ? Block.BLOCK_COLOR_RED : Block.BLOCK_COLOR_BLUE;
				f.setBlockColor(x, y, color);
			}
		}

		int[] squares = f.checkForSquares();

		assertArrayEquals(new int[] {0, 1}, squares);
		assertTrue(f.getBlock(0, 0).isSilverSquareBlock());
	}

	@Test
	void checkForSquaresIgnoresAreasContainingGarbageBlocks() {
		Field f = newField();
		for (int x = 0; x < 4; x++) {
			for (int y = 0; y < 4; y++) {
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
			}
		}
		f.getBlock(2, 2).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);

		int[] squares = f.checkForSquares();

		assertArrayEquals(new int[] {0, 0}, squares);
	}

	@Test
	void getHowManySquareClearsCountsGoldAndSilverStripsInFlaggedRows() {
		Field f = newField();
		for (int x = 0; x < 4; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_SQUARE_GOLD_1);
		}
		for (int x = 4; x < 8; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_SQUARE_SILVER_1);
		}
		f.setLineFlag(19, true);

		int[] clears = f.getHowManySquareClears();

		assertEquals(1, clears[0], "4 gold cells / 4 = 1 gold strip");
		assertEquals(1, clears[1], "4 silver cells / 4 = 1 silver strip");
	}

	@Test
	void getHowManySquareClearsIgnoresGarbageEvenIfBlockTypeIsSquare() {
		Field f = newField();
		for (int x = 0; x < 4; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_SQUARE_GOLD_1);
			f.getBlock(x, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		}
		f.setLineFlag(19, true);

		int[] clears = f.getHowManySquareClears();
		assertArrayEquals(new int[] {0, 0}, clears);
	}

	@Test
	void clearColorAtCoordRecursivelyClearsConnectedSameColorBlocks() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		// A separated RED that should survive
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);

		int cleared = f.clearColor(0, 19, false, false, false, false);

		assertEquals(5, cleared);
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(2, 19));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 19),
				"isolated block of the same color must remain");
	}

	@Test
	void clearColorAtCoordReturnsZeroForGarbageOrEmptyOrInvalid() {
		Field f = newField();
		assertEquals(0, f.clearColor(0, 19, false, false, false, false),
				"empty cell yields zero clears");
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.getBlock(0, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		assertEquals(0, f.clearColor(0, 19, false, false, false, false),
				"garbage cell does not start a flood-fill");
	}

	@Test
	void clearColorWithSizeOnlyClearsClustersAtLeastThatBig() {
		Field f = newField();
		// 4-block cluster of RED
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 18, Block.BLOCK_COLOR_RED);
		// Singleton BLUE — too small to clear
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_BLUE);

		int cleared = f.clearColor(4, false, false);

		assertEquals(4, cleared);
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(5, 19));
	}

	@Test
	void allClearColorClearsEveryCellOfTargetColorIgnoringConnectivity() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 5, Block.BLOCK_COLOR_BLUE);

		int cleared = f.allClearColor(Block.BLOCK_COLOR_RED, false, false);

		assertEquals(2, cleared);
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 10));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(7, 5));
	}

	@Test
	void allClearColorReturnsZeroForNegativeTargetColor() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);

		assertEquals(0, f.allClearColor(Block.BLOCK_COLOR_INVALID, false, false));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19),
				"invalid target color must leave the field intact");
	}

	@Test
	void checkColorCountsMatchingClustersWithoutMutatingOriginalField() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);

		// size=4: cluster too small; size=3: matches
		assertEquals(0, f.checkColor(4, false, false, false, false));
		assertEquals(3, f.checkColor(3, false, false, false, false));

		// Original field unaffected when flag=false
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19));
	}

	@Test
	void checkColorWithFlagSetsEraseAttributeOnQualifyingBlocks() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 18, Block.BLOCK_COLOR_RED);

		int total = f.checkColor(3, true, false, false, false);
		assertEquals(3, total);
		assertTrue(f.getBlock(0, 19).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
				"flag=true marks cleared blocks with ERASE attribute");
	}

	@Test
	void checkLineColorCountsHorizontalAndVerticalRuns() {
		Field f = newField();
		// 4-cell horizontal run
		for (int x = 0; x < 4; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		int total = f.checkLineColor(4, false, false, false);

		assertEquals(4, total,
				"4-cell horizontal run yields exactly 4 — only the leftmost cell starts a run >= 4");
	}

	@Test
	void checkLineColorReturnsZeroForSizeBelowOne() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);

		assertEquals(0, f.checkLineColor(0, false, false, false));
	}

	@Test
	void clearLineColorErasesBlocksMarkedWithEraseAttribute() {
		Field f = newField();
		for (int x = 0; x < 4; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}

		int marked = f.checkLineColor(4, true, false, false);
		int cleared = f.clearLineColor(4, false, false);

		assertEquals(4, marked);
		assertEquals(4, cleared);
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
	}

	@Test
	void gemClearColorClearsClustersThatContainAtLeastOneGemBlock() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);

		int cleared = f.gemClearColor(4, false, false);

		assertEquals(4, cleared);
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(3, 19));
	}

	@Test
	void gemClearColorTwoArgVariantSkipsHiddenRows() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);

		int cleared = f.gemClearColor(2, false);

		assertEquals(2, cleared);
	}

	@Test
	void gemColorCheckReportsTotalAndOptionallyMarksErase() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);

		int total = f.gemColorCheck(3, false, false, false);
		assertEquals(3, total);
		assertEquals(Block.BLOCK_COLOR_GEM_RED, f.getBlockColor(0, 19),
				"flag=false leaves the field unmodified");

		int totalWithFlag = f.gemColorCheck(3, true, false, false);
		assertEquals(3, totalWithFlag);
		assertTrue(f.getBlock(0, 19).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE));
	}

	@Test
	void doCascadeGravityNoArgMovesIsolatedBlockOneStepDown() {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);

		boolean changed = f.doCascadeGravity();

		assertTrue(changed);
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 10));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 11));
	}

	@Test
	void doCascadeGravityReturnsFalseWhenAllBlocksRest() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);

		assertFalse(f.doCascadeGravity());
	}

	@Test
	void doCascadeGravityWithEnumDispatchesToFastCascade() {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);

		assertTrue(f.doCascadeGravity(GameEngine.LineGravity.CASCADE));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 11));
	}

	@Test
	void doCascadeGravityWithSlowEnumDispatchesToSlowCascade() {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);

		assertTrue(f.doCascadeGravity(GameEngine.LineGravity.CASCADE_SLOW));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 11));
	}

	@Test
	void doCascadeSlowMatchesFastResultForIsolatedBlock() {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);

		boolean changed = f.doCascadeSlow();

		assertTrue(changed);
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 11));
	}

	@Test
	void addHoverBlockSetsAntigravityAndOtherFlagsOnExistingBlock() {
		Field f = newField();

		boolean ok = f.addHoverBlock(5, 5, Block.BLOCK_COLOR_RED);

		assertTrue(ok);
		Block b = f.getBlock(5, 5);
		assertEquals(Block.BLOCK_COLOR_RED, b.color);
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_ANTIGRAVITY));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
		assertFalse(b.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
	}

	@Test
	void addHoverBlockReturnsFalseForOutOfRangeCoord() {
		Field f = newField();

		assertFalse(f.addHoverBlock(99, 0, Block.BLOCK_COLOR_RED));
	}

	@Test
	void freeFallDropsBlocksToBottomInColumnOrder() {
		Field f = newField();
		f.setBlockColor(3, 5, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 10, Block.BLOCK_COLOR_BLUE);

		boolean changed = f.freeFall();

		assertTrue(changed);
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(3, 19),
				"the lower block hits the floor first");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(3, 18),
				"upper block stacks just above the lower");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(3, 5));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(3, 10));
	}

	@Test
	void freeFallReturnsFalseWhenNothingMoves() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);

		assertFalse(f.freeFall());
	}

	@Test
	void garbageDropPlaceFillsEmptyCellWithGarbageBlockOfDefaultColor() {
		Field f = newField();

		boolean placed = f.garbageDropPlace(0, 0, false, 1);

		assertTrue(placed);
		Block b = f.getBlock(0, 0);
		assertEquals(Block.BLOCK_COLOR_GRAY, b.color);
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE));
		assertTrue(b.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
		assertEquals(1, b.hard);
	}

	@Test
	void garbageDropPlaceWithExplicitColorUsesIt() {
		Field f = newField();

		boolean placed = f.garbageDropPlace(0, 0, false, 0, Block.BLOCK_COLOR_BLUE);

		assertTrue(placed);
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(0, 0));
	}

	@Test
	void garbageDropPlaceReturnsFalseForOutOfRangeCoord() {
		Field f = newField();

		assertFalse(f.garbageDropPlace(99, 0, false, 0));
	}

	@Test
	void shuffleColorsRemapsBlockColorsToOneOfThePaletteEntries() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);

		int[] palette = {Block.BLOCK_COLOR_GREEN, Block.BLOCK_COLOR_PURPLE};
		f.shuffleColors(palette, 2, new Random(0));

		int after = f.getBlockColor(0, 19);
		assertTrue(
				after == Block.BLOCK_COLOR_GREEN || after == Block.BLOCK_COLOR_PURPLE,
				"after shuffleColors with a 2-entry palette, RED must remap to one of the entries; got " + after);
	}

	@Test
	void clearLineColorClearsConnectDownAttributeOnAdjacentBlock() {
		Field f = newField();
		// Row 18: erased block connected downward; row 19: adjacent block below
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		Block top = f.getBlock(5, 18);
		Block bot = f.getBlock(5, 19);
		top.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		top.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);
		bot.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);

		f.clearLineColor(1, false, false);

		assertFalse(bot.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP),
				"CONNECT_UP on block below erased+CONNECT_DOWN block must be cleared");
		assertTrue(bot.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN),
				"BROKEN must be set on block below erased+CONNECT_DOWN block");
	}

	@Test
	void clearLineColorClearsConnectUpAttributeOnAdjacentBlock() {
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_RED);
		Block bot = f.getBlock(5, 19);
		Block top = f.getBlock(5, 18);
		bot.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		bot.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);
		top.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);

		f.clearLineColor(1, false, false);

		assertFalse(top.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN),
				"CONNECT_DOWN on block above erased+CONNECT_UP block must be cleared");
		assertTrue(top.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN),
				"BROKEN must be set on block above erased+CONNECT_UP block");
	}

	@Test
	void clearLineColorClearsConnectLeftAttributeOnAdjacentBlock() {
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 19, Block.BLOCK_COLOR_RED);
		Block right = f.getBlock(5, 19);
		Block left  = f.getBlock(4, 19);
		right.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		right.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);
		left.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);

		f.clearLineColor(1, false, false);

		assertFalse(left.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT),
				"CONNECT_RIGHT on block left of erased+CONNECT_LEFT block must be cleared");
		assertTrue(left.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
	}

	@Test
	void clearLineColorClearsConnectRightAttributeOnAdjacentBlock() {
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 19, Block.BLOCK_COLOR_RED);
		Block left  = f.getBlock(5, 19);
		Block right = f.getBlock(6, 19);
		left.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		left.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
		right.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);

		f.clearLineColor(1, false, false);

		assertFalse(right.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT),
				"CONNECT_LEFT on block right of erased+CONNECT_RIGHT block must be cleared");
		assertTrue(right.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
	}

	@Test
	void checkLineColorDecrementsHardCounterInsteadOfMarkingEraseForHardBlock() {
		Field f = newField();
		// 4-cell horizontal run with one hard block (hard=1)
		for (int x = 0; x < 4; x++) {
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		}
		Block hard = f.getBlock(2, 19);
		hard.hard = 1;

		// flag=true triggers the erase/hard-counter path
		int total = f.checkLineColor(4, true, false, false);

		assertEquals(4, total);
		// Hard block: counter decremented, ERASE not set
		assertEquals(0, hard.hard, "hard counter must be decremented");
		assertFalse(hard.getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
				"hard block must not receive ERASE when hard > 0");
		// Normal block: ERASE is set
		assertTrue(f.getBlock(0, 19).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE));
	}

	@Test
	void checkLineColorIncrementsGemsClearedForGemBlocksInRun() {
		Field f = newField();
		// 4-cell run mixing one gem and three normals
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(2, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(3, 19, Block.BLOCK_COLOR_RED);

		// gemSame=true so the gem counts as RED
		int total = f.checkLineColor(4, true, false, true);

		assertEquals(4, total);
		assertEquals(1, f.gemsCleared, "gemsCleared must be incremented for the gem block");
	}

	@Test
	void allClearColorWithGemSameTrueConvertsGemTargetToNormalColor() {
		// Covers Field line 1863: targetColor = Block.gemToNormalColor(targetColor)
		// GEM_RED (9) normalises to RED (2), so all RED blocks are cleared.
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 5, Block.BLOCK_COLOR_BLUE);

		int cleared = f.allClearColor(Block.BLOCK_COLOR_GEM_RED, false, true);

		assertEquals(2, cleared, "GEM_RED target with gemSame=true must clear all RED blocks");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 10));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(7, 5), "blue unaffected");
	}

	@Test
	void allClearColorWithFlagTrueMarksBlocksWithEraseAttributeRatherThanRemoving() {
		// Covers Field line 1871: getBlock(x, y).setAttribute(BLOCK_ATTRIBUTE_ERASE, true)
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);

		int marked = f.allClearColor(Block.BLOCK_COLOR_RED, true, false);

		assertEquals(2, marked);
		// flag=true: ERASE attribute set, color kept
		assertTrue(f.getBlock(0, 19).getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
				"flag=true must set ERASE on each matching block");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 19),
				"flag=true must not clear the block color");
	}

	@Test
	void doCascadeGravityClearsConnectionFlagsOnIgnoreBlocklinkBlock() {
		// Covers Field lines 1937-1940: the IGNORE_BLOCKLINK branch that disconnects
		// a falling block from its neighbours.
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		Block blk = f.getBlock(5, 10);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_IGNORE_BLOCKLINK, true);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);

		boolean changed = f.doCascadeGravity();

		assertTrue(changed);
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 11), "block dropped one row");
		assertFalse(f.getBlock(5, 11).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT),
				"IGNORE_BLOCKLINK: CONNECT_LEFT cleared on fall");
		assertFalse(f.getBlock(5, 11).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT),
				"IGNORE_BLOCKLINK: CONNECT_RIGHT cleared on fall");
	}

	@Test
	void doCascadeSlowClearsConnectionFlagsOnIgnoreBlocklinkBlock() {
		// Covers Field lines 2009-2012: the same IGNORE_BLOCKLINK branch in the slow
		// cascade variant.
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		Block blk = f.getBlock(5, 10);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_IGNORE_BLOCKLINK, true);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);
		blk.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);

		boolean changed = f.doCascadeSlow();

		assertTrue(changed);
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 11), "block dropped one row");
		assertFalse(f.getBlock(5, 11).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP),
				"IGNORE_BLOCKLINK: CONNECT_UP cleared on slow-cascade fall");
		assertFalse(f.getBlock(5, 11).getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN),
				"IGNORE_BLOCKLINK: CONNECT_DOWN cleared on slow-cascade fall");
	}

	@Test
	void doCascadeSlowReturnsFalseWhenBlockCannotFall() {
		// Covers Field line 1988: fall = false when a block in the linked group is
		// blocked (wall directly below).
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);  // already on the floor

		assertFalse(f.doCascadeSlow(), "block on the floor must not trigger a fall");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 19), "block must stay put");
	}

	@Test
	void clearColorIgnoresHiddenRowBlockWhenIgnoreHiddenIsTrue() {
		// Covers Field line 1817: return 0 when ignoreHidden=true and y < 0 in the
		// private recursive clearColor.
		Field f = newField();   // hidden_height = 3, so y=-1 is valid hidden row
		f.setBlockColor(5, -1, Block.BLOCK_COLOR_RED);

		int cleared = f.clearColor(5, -1, false, false, false, true);

		assertEquals(0, cleared, "ignoreHidden=true must skip hidden-area cells");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, -1), "hidden block left untouched");
	}

	@Test
	void clearColorDecrementsHardCounterInsteadOfErasingNonFlaggedHardBlock() {
		// Covers Field line 1842: b.hard-- when flag=false and the block is hard.
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		Block b = f.getBlock(5, 19);
		b.hard = 1;

		int cleared = f.clearColor(5, 19, false, false, false, false);

		assertEquals(1, cleared, "hard block contributes to the cluster size");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 19), "hard block color unchanged");
		assertEquals(0, b.hard, "hard counter decremented from 1 to 0");
	}

	@Test
	void clearColorWithGarbageClearClearsAdjacentGarbageBlock() {
		// Covers Field lines 1825, 1835: when garbageClear=true and a garbage block
		// is adjacent to the same-color cluster, its color is set to NONE even though
		// it doesn't count toward the cluster total.
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 19, Block.BLOCK_COLOR_GRAY);
		f.getBlock(6, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);

		int cleared = f.clearColor(5, 19, false, true, false, false);

		assertEquals(1, cleared, "garbage block does not count toward cleared total");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 19), "same-color block cleared");
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(6, 19), "adjacent garbage block cleared");
	}

	@Test
	void clearColorWithGarbageClearAndFlagSetMarksAdjacentGarbageWithErase() {
		// Covers Field lines 1827-1830: flag=true path for adjacent garbage blocks.
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 19, Block.BLOCK_COLOR_GRAY);
		Block garbage = f.getBlock(6, 19);
		garbage.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);

		f.clearColor(5, 19, true, true, false, false);

		assertTrue(garbage.getAttribute(Block.BLOCK_ATTRIBUTE_ERASE),
				"flag=true + garbageClear: adjacent garbage block marked ERASE");
	}
}
