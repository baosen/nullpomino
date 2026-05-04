package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

import org.junit.jupiter.api.Test;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

/**
 * Covers remaining branches in {@link Field} not yet exercised by existing
 * test classes. Targets null-defensive guards, boolean short-circuit branches,
 * flagged-row skips, boundary conditions, and exception catch paths.
 */
class FieldBranchCoverageTest {

	// ── helpers ──────────────────────────────────────────────────────────

	private static Field newField() {
		return new Field(10, 20, 3, false);
	}

	private static GameEngine createEngine() {
		GameManager gm = new GameManager(new EventReceiver());
		GameEngine engine = new GameEngine(gm, 0);
		engine.random = new Random(42);
		return engine;
	}

	/** Null out a single cell in block_field via reflection. */
	private static void nullOutFieldBlock(Field f, int x, int y) throws Exception {
		java.lang.reflect.Field bf = nullpomino.game.component.Field.class.getDeclaredField("block_field");
		bf.setAccessible(true);
		Block[][] blocks = (Block[][]) bf.get(f);
		blocks[y][x] = null;
	}

	// ══════════════════════════════════════════════════════════════════════
	// readProperty — lines 231, 236
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void readPropertyWithMoreEntriesThanWidth() {
		// Line 231: loop condition j < mapArray.length && j < width
		// When mapArray.length > width, the second sub-condition j < width
		// terminates the loop — its "false" branch is exercised.
		Field f = new Field(4, 3, 1, false);
		CustomProperties props = new CustomProperties();
		props.setProperty("0.field.map.1", "2,0,7,0,3,5,1");
		f.readProperty(props, 0);
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 1));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(1, 1));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(2, 1));
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(3, 1));
	}

	// ══════════════════════════════════════════════════════════════════════
	// isEmpty — line 753
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void isEmptyReturnsTrueWhenNonEmptyRowsAreFlagged() {
		// Line 753: if(getLineFlag(i) == false) — when true (flagged),
		// blocks in that row are not counted.
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		assertFalse(f.isEmpty());
		f.setLineFlag(19, true);
		assertTrue(f.isEmpty());
	}

	// ══════════════════════════════════════════════════════════════════════
	// clearLine — lines 650, 660 (null block in connect checks)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void clearLineSkipsNullBlockInConnectUp() throws Exception {
		// Line 650: if(blk != null && blk.getAttribute(CONNECT_UP))
		Field f = newField();
		for (int x = 0; x < 10; x++) f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		f.setLineFlag(19, true);
		nullOutFieldBlock(f, 5, 18);
		assertEquals(1, f.clearLine());
	}

	@Test
	void clearLineSkipsNullBlockInConnectDown() throws Exception {
		// Line 660: if(blk != null && blk.getAttribute(CONNECT_DOWN))
		Field f = newField();
		for (int x = 0; x < 10; x++) f.setBlockColor(x, 18, Block.BLOCK_COLOR_RED);
		f.setLineFlag(18, true);
		nullOutFieldBlock(f, 5, 19);
		assertEquals(1, f.clearLine());
	}

	// ══════════════════════════════════════════════════════════════════════
	// copyRow — line 731 (null block -> new Block())
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void copyRowHandlesNullBlock() throws Exception {
		// Line 731: block == null ? new Block() : block
		Field f = newField();
		nullOutFieldBlock(f, 5, 10);
		f.pushUp(1); // calls copyRow internally
		// No NPE expected
	}

	// ══════════════════════════════════════════════════════════════════════
	// getTSlotLineClearAll(big, minimum) — line 896
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getTSlotLineClearAllWithMinimumSkipsFlaggedRows() {
		// Line 896: if(getLineFlag(i) == false) — when a row is flagged, skip
		Field f = newField();
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 12, Block.BLOCK_COLOR_RED);
		for (int x = 0; x < f.getWidth(); x++)
			if (x < 4 || x >= 7) {
				f.setBlockColor(x, 11, Block.BLOCK_COLOR_BLUE);
				f.setBlockColor(x, 12, Block.BLOCK_COLOR_BLUE);
			}
		f.setLineFlag(10, true);
		assertEquals(0, f.getTSlotLineClearAll(false, 2));
	}

	// ══════════════════════════════════════════════════════════════════════
	// getHowManyBlocksFromRight — line 958
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getHowManyBlocksFromRightSkipsFlaggedRows() {
		// Line 958: if(getLineFlag(i) == false) — when true, skip
		Field f = newField();
		f.setBlockColor(9, 19, Block.BLOCK_COLOR_RED);
		f.setLineFlag(19, true);
		assertEquals(0, f.getHowManyBlocksFromRight());
	}

	// ══════════════════════════════════════════════════════════════════════
	// getHowManyHoles — line 1026
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getHowManyHolesSkipsFlaggedRows() {
		// Line 1026: if(getLineFlag(i) == false)
		Field f = newField();
		f.setBlockColor(0, 17, Block.BLOCK_COLOR_RED);
		f.setLineFlag(17, true);
		assertEquals(0, f.getHowManyHoles());
	}

	@Test
	void getHowManyHolesResetsSameholeAfterNonHole() {
		// Line 1029: else if(samehole && getBlockEmpty(j, i))
		Field f = newField();
		// Column 0: block at 16, then empty at 17,18,19
		// At i=16: isHoleBelow=true -> samehole=true
		// At i=17: isHoleBelow=false -> samehole=true && empty -> hole++, samehole=false
		// At i=18: isHoleBelow=false -> samehole=false
		// At i=19: isHoleBelow=true (OOB) -> samehole=true
		f.setBlockColor(0, 16, Block.BLOCK_COLOR_RED);
		assertEquals(3, f.getHowManyHoles());
	}

	// ══════════════════════════════════════════════════════════════════════
	// getHowManyLidAboveHoles — line 1052
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getHowManyLidAboveHolesSkipsFlaggedRows() {
		// Line 1052: if(getLineFlag(i) == false)
		Field f = newField();
		f.setBlockColor(0, 9, Block.BLOCK_COLOR_RED);
		f.setBlockColor(0, 7, Block.BLOCK_COLOR_RED);
		f.setLineFlag(9, true);
		assertEquals(1, f.getHowManyLidAboveHoles());
	}

	// ══════════════════════════════════════════════════════════════════════
	// getValleyDepth — lines 1109, 1110
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getValleyDepthSkipsFlaggedRows() {
		// Line 1109: if(getLineFlag(i) == false)
		Field f = newField();
		for (int y = 17; y <= 19; y++) {
			f.setBlockColor(4, y, Block.BLOCK_COLOR_RED);
			f.setBlockColor(6, y, Block.BLOCK_COLOR_RED);
		}
		f.setLineFlag(18, true);
		assertEquals(2, f.getValleyDepth(5));
	}

	@Test
	void getValleyDepthLeftEdge() {
		// Line 1110: (x <= 0) short-circuits left-wall check
		Field f = newField();
		for (int y = 17; y <= 19; y++)
			f.setBlockColor(1, y, Block.BLOCK_COLOR_RED);
		assertEquals(3, f.getValleyDepth(0));
	}

	@Test
	void getValleyDepthRightEdge() {
		// Line 1110: (x >= width - 1) short-circuits right-wall check
		Field f = newField();
		for (int y = 17; y <= 19; y++)
			f.setBlockColor(8, y, Block.BLOCK_COLOR_RED);
		assertEquals(3, f.getValleyDepth(9));
	}

	// ══════════════════════════════════════════════════════════════════════
	// cutLine — line 1168 (null block)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void cutLineHandlesNullBlock() throws Exception {
		// Line 1168: if(blk == null) blk = new Block()
		Field f = newField();
		f.setBlockColor(0, 10, Block.BLOCK_COLOR_RED);
		nullOutFieldBlock(f, 5, 6);
		f.cutLine(8, 1);
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(0, 11));
	}

	// ══════════════════════════════════════════════════════════════════════
	// addSingleHoleGarbage — line 1258 (null block)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void addSingleHoleGarbageSkipsNullBlock() throws Exception {
		// Line 1258: if(blk != null) — null block skipped
		Field f = newField();
		nullOutFieldBlock(f, 0, 19);
		f.addSingleHoleGarbage(3, Block.BLOCK_COLOR_GRAY, 0, Block.BLOCK_ATTRIBUTE_VISIBLE, 1);
		// No NPE expected
	}

	// ══════════════════════════════════════════════════════════════════════
	// getSecretGrade — line 1303
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getSecretGradeBreaksWhenNoBlockBelowHole() {
		// Line 1305: getBlockEmpty(holeLoc, i) && !getBlockEmpty(holeLoc, i-1)
		// When both cells are empty, the else branch fires and breaks.
		Field f = newField();
		f.setBlockColor(1, 19, Block.BLOCK_COLOR_RED);
		assertEquals(0, f.getSecretGrade());
	}

	// ══════════════════════════════════════════════════════════════════════
	// setAllAttribute — line 1336 (null block)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void setAllAttributeSkipsNullBlock() throws Exception {
		Field f = newField();
		nullOutFieldBlock(f, 5, 10);
		f.setAllAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE, true);
		assertTrue(f.getBlock(0, 0).getAttribute(Block.BLOCK_ATTRIBUTE_VISIBLE));
	}

	// ══════════════════════════════════════════════════════════════════════
	// setAllSkin — line 1352 (null block)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void setAllSkinSkipsNullBlock() throws Exception {
		Field f = newField();
		nullOutFieldBlock(f, 5, 10);
		f.setAllSkin(42);
		assertEquals(42, f.getBlock(0, 0).skin);
	}

	// ══════════════════════════════════════════════════════════════════════
	// getHowManyGems — line 1369 (null block)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getHowManyGemsSkipsNullBlock() throws Exception {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		nullOutFieldBlock(f, 5, 10);
		assertEquals(1, f.getHowManyGems());
	}

	// ══════════════════════════════════════════════════════════════════════
	// getHowManyGemClears — line 1389 (null block)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getHowManyGemClearsSkipsNullBlock() throws Exception {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_GEM_RED);
		f.setLineFlag(19, true);
		nullOutFieldBlock(f, 5, 19);
		assertEquals(1, f.getHowManyGemClears());
	}

	// ══════════════════════════════════════════════════════════════════════
	// getItemClears — line 1412 (null block)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getItemClearsSkipsNullBlock() throws Exception {
		Field f = newField();
		f.getBlock(0, 19).item = 1;
		f.setLineFlag(19, true);
		nullOutFieldBlock(f, 5, 19);
		boolean[] result = f.getItemClears();
		assertTrue(result[1]);
	}

	// ══════════════════════════════════════════════════════════════════════
	// checkForSquares (gold) — lines 1441, 1446, 1458-1464
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void checkForSquaresGoldSkipsEmptyRoot() {
		// Line 1441: !(rootBlk == null || rootBlk.isEmpty())
		assertArrayEquals(new int[]{0, 0}, newField().checkForSquares());
	}

	@Test
	void checkForSquaresGoldSkipsAlreadySquareRoot() {
		// Line 1446: !(rootBlk.isGoldSquareBlock() || rootBlk.isSilverSquareBlock())
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
		f.checkForSquares(); // converts to gold squares
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	@Test
	void checkForSquaresGoldBreaksOnConnectLeft() {
		// Line 1461: l == 0 && CONNECT_LEFT
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
		f.getBlock(0, 0).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	@Test
	void checkForSquaresGoldBreaksOnConnectRight() {
		// Line 1462: l == 3 && CONNECT_RIGHT
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
		f.getBlock(3, 0).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	@Test
	void checkForSquaresGoldBreaksOnConnectUp() {
		// Line 1463: k == 0 && CONNECT_UP
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
		f.getBlock(0, 0).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	@Test
	void checkForSquaresGoldBreaksOnConnectDown() {
		// Line 1464: k == 3 && CONNECT_DOWN
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
		f.getBlock(0, 3).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	// ══════════════════════════════════════════════════════════════════════
	// checkForSquares (silver) — lines 1507, 1514-1520
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void checkForSquaresSilverSkipsAlreadySquareRoot() {
		// Line 1507: !(rootBlk.isGoldSquareBlock() || rootBlk.isSilverSquareBlock())
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, (x + y) % 2 == 0
						? Block.BLOCK_COLOR_RED : Block.BLOCK_COLOR_BLUE);
		f.checkForSquares(); // converts to silver
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	@Test
	void checkForSquaresSilverBreaksOnConnectLeft() {
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, (x + y) % 2 == 0
						? Block.BLOCK_COLOR_RED : Block.BLOCK_COLOR_BLUE);
		f.getBlock(0, 0).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	@Test
	void checkForSquaresSilverBreaksOnConnectRight() {
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, (x + y) % 2 == 0
						? Block.BLOCK_COLOR_RED : Block.BLOCK_COLOR_BLUE);
		f.getBlock(3, 0).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	@Test
	void checkForSquaresSilverBreaksOnConnectUp() {
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, (x + y) % 2 == 0
						? Block.BLOCK_COLOR_RED : Block.BLOCK_COLOR_BLUE);
		f.getBlock(0, 0).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	@Test
	void checkForSquaresSilverBreaksOnConnectDown() {
		Field f = newField();
		for (int x = 0; x < 4; x++)
			for (int y = 0; y < 4; y++)
				f.setBlockColor(x, y, (x + y) % 2 == 0
						? Block.BLOCK_COLOR_RED : Block.BLOCK_COLOR_BLUE);
		f.getBlock(0, 3).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);
		assertArrayEquals(new int[]{0, 0}, f.checkForSquares());
	}

	// ══════════════════════════════════════════════════════════════════════
	// getHowManySquareClears — line 1573 (null block)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void getHowManySquareClearsSkipsNullBlock() throws Exception {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_SQUARE_GOLD_1);
		f.setLineFlag(19, true);
		nullOutFieldBlock(f, 5, 19);
		assertArrayEquals(new int[]{0, 0}, f.getHowManySquareClears());
	}

	// ══════════════════════════════════════════════════════════════════════
	// clearLineColor — lines 1613, 1622, 1631, 1640 (null bAdj)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void clearLineColorHandlesNullConnectDownAdjacent() throws Exception {
		Field f = newField();
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_RED);
		f.getBlock(5, 18).setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		f.getBlock(5, 18).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, true);
		nullOutFieldBlock(f, 5, 19);
		assertEquals(1, f.clearLineColor(1, false, false));
	}

	@Test
	void clearLineColorHandlesNullConnectUpAdjacent() throws Exception {
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.getBlock(5, 19).setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		f.getBlock(5, 19).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);
		nullOutFieldBlock(f, 5, 18);
		assertEquals(1, f.clearLineColor(1, false, false));
	}

	@Test
	void clearLineColorHandlesNullConnectLeftAdjacent() throws Exception {
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.getBlock(5, 19).setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		f.getBlock(5, 19).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);
		nullOutFieldBlock(f, 4, 19);
		assertEquals(1, f.clearLineColor(1, false, false));
	}

	@Test
	void clearLineColorHandlesNullConnectRightAdjacent() throws Exception {
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.getBlock(5, 19).setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		f.getBlock(5, 19).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
		nullOutFieldBlock(f, 6, 19);
		assertEquals(1, f.clearLineColor(1, false, false));
	}

	// ══════════════════════════════════════════════════════════════════════
	// checkLineColor — line 1666 (lineColorsCleared init)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void checkLineColorInitsLineColorsClearedWhenFlagTrue() {
		Field f = newField();
		f.lineColorsCleared = null;
		f.checkLineColor(1, true, false, false);
		assertNotNull(f.lineColorsCleared);
	}

	@Test
	void checkLineColorDoesNotInitWhenFlagFalse() {
		Field f = newField();
		f.lineColorsCleared = null;
		f.checkLineColor(1, false, false, false);
		assertNull(f.lineColorsCleared);
	}

	// ══════════════════════════════════════════════════════════════════════
	// clearColor (private) — line 1825 (garbage with wall attribute)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void clearColorSkipsGarbageBlockWithWallAttribute() {
		// Line 1825: garbageClear && b.getAttribute(GARBAGE) && !b.getAttribute(WALL)
		// When WALL is set, the garbage branch is skipped.
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 19, Block.BLOCK_COLOR_GRAY);
		f.getBlock(6, 19).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		f.getBlock(6, 19).setAttribute(Block.BLOCK_ATTRIBUTE_WALL, true);
		int cleared = f.clearColor(5, 19, false, true, false, false);
		assertEquals(1, cleared);
		assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlockColor(6, 19));
	}

	// ══════════════════════════════════════════════════════════════════════
	// doCascadeGravity — lines 1899, 1907-1908, 1912-1913, 1929-1931
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void doCascadeGravityHandlesNullBlock() throws Exception {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		nullOutFieldBlock(f, 0, 0);
		assertTrue(f.doCascadeGravity());
	}

	@Test
	void doCascadeGravityBlockedByOccupiedBelow() {
		Field f = newField();
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		assertFalse(f.doCascadeGravity());
	}

	@Test
	void doCascadeGravityFallsCorrectly() {
		Field f = newField();
		f.setBlockColor(3, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(7, 5, Block.BLOCK_COLOR_BLUE);
		assertTrue(f.doCascadeGravity());
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(3, 11));
		assertEquals(Block.BLOCK_COLOR_BLUE, f.getBlockColor(7, 6));
	}

	// ══════════════════════════════════════════════════════════════════════
	// doCascadeSlow — lines 1972, 1980-1981, 1985-1986, 2001-2003
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void doCascadeSlowHandlesNullBlock() throws Exception {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		nullOutFieldBlock(f, 0, 0);
		assertTrue(f.doCascadeSlow());
	}

	@Test
	void doCascadeSlowBlockedByWall() {
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		assertFalse(f.doCascadeSlow());
	}

	@Test
	void doCascadeSlowFallsCorrectly() {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		assertTrue(f.doCascadeSlow());
		assertEquals(Block.BLOCK_COLOR_NONE, f.getBlockColor(5, 10));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(5, 11));
	}

	// ══════════════════════════════════════════════════════════════════════
	// checkBlockLinkSub — lines 2048, 2054-2055
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void checkBlockLinkSubSkipsNullBlock() throws Exception {
		nullOutFieldBlock(newField(), 5, 10);
		newField().checkBlockLink(5, 10);
		// No NPE expected
	}

	@Test
	void checkBlockLinkSubWalksHorizontalConnections() {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(4, 10, Block.BLOCK_COLOR_RED);
		f.setBlockColor(6, 10, Block.BLOCK_COLOR_RED);
		f.getBlock(5, 10).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, true);
		f.getBlock(5, 10).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, true);
		f.checkBlockLink(5, 10);
		assertTrue(f.getBlock(4, 10).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK));
		assertTrue(f.getBlock(6, 10).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK));
	}

	// ══════════════════════════════════════════════════════════════════════
	// setBlockLinkBrokenSub — line 2078
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void setBlockLinkBrokenSubSkipsNullBlock() throws Exception {
		nullOutFieldBlock(newField(), 5, 10);
		newField().setBlockLinkBroken(5, 10);
	}

	@Test
	void setBlockLinkBrokenSubSkipsNonNormalBlock() {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_GEM_RED);
		f.getBlock(5, 10).setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, true);
		f.setBlockLinkBroken(5, 10);
		assertFalse(f.getBlock(5, 10).getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
	}

	// ══════════════════════════════════════════════════════════════════════
	// setBlockLinkByColorSub — lines 2116-2117
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void setBlockLinkByColorSubSkipsNullBlock() throws Exception {
		nullOutFieldBlock(newField(), 5, 10);
		newField().setBlockLinkByColor(5, 10);
	}

	@Test
	void setBlockLinkByColorSubSkipsGarbageBlock() {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		f.getBlock(5, 10).setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		f.setBlockLinkByColor(5, 10);
		assertFalse(f.getBlock(5, 10).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK));
	}

	@Test
	void setBlockLinkByColorSubSkipsEmptyBlock() {
		Field f = newField();
		f.setBlockLinkByColor(5, 10);
		assertFalse(f.getBlock(5, 10).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK));
	}

	// ══════════════════════════════════════════════════════════════════════
	// stringToField — line 2287 (exception catch)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void stringToFieldCatchesException() {
		Field f = newField();
		f.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		assertDoesNotThrow(() -> f.stringToField("zzz"));
	}

	// ══════════════════════════════════════════════════════════════════════
	// attrStringToRow — lines 2354, 2356 (length checks)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void attrStringToRowWithColorOnly() {
		// Line 2356: strSubArray.length > 1 — when length=1, attr stays 0,
		// but setAttribute(VISIBLE|OUTLINE) forces bits 0 and 1 on.
		Field f = new Field(2, 2, 0, false);
		Block[] row = f.attrStringToRow("2;", 0);
		assertEquals(Block.BLOCK_COLOR_RED, row[0].color);
		assertEquals(3, row[0].attribute);
	}

	@Test
	void attrStringToRowWithNonHexInput() {
		Field f = new Field(2, 2, 0, false);
		Block[] row = f.attrStringToRow("zz/zz;", 0);
		assertEquals(Block.BLOCK_COLOR_NONE, row[0].color);
	}

	// ══════════════════════════════════════════════════════════════════════
	// checkColor — line 2456 (color range check)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void checkColorRecordsInRangeColors() {
		Field f = newField();
		for (int x = 0; x < 4; x++)
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_RED);
		f.checkColor(4, true, false, false, false);
		assertEquals(1, f.colorsCleared);
	}

	@Test
	void checkColorSkipsGrayOutOfRange() {
		Field f = newField();
		for (int x = 0; x < 4; x++)
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_GRAY);
		f.checkColor(4, true, false, false, false);
		assertEquals(0, f.colorsCleared);
	}

	@Test
	void checkColorSkipsGemRedOutOfRange() {
		// GEM_RED = 9 is outside [2,8] → not recorded as a cleared color
		Field f = newField();
		for (int x = 0; x < 4; x++)
			f.setBlockColor(x, 19, Block.BLOCK_COLOR_GEM_RED);
		f.checkColor(4, true, false, false, false);
		assertEquals(0, f.colorsCleared);
	}

	// ══════════════════════════════════════════════════════════════════════
	// garbageDrop — lines 2503, 2512, 2524
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void garbageDropAvoidColumnHighDrop() {
		// High drop path (> half width): fill all then remove
		Field f = new Field(10, 4, 0, false);
		f.garbageDrop(createEngine(), 9, false, 0, 0, 3, Block.BLOCK_COLOR_GRAY);
		assertTrue(f.getBlockEmpty(3, 0));
	}

	@Test
	void garbageDropLowDropPath() {
		// Low drop path (<= half width): start empty then fill
		Field f = new Field(10, 4, 0, false);
		f.garbageDrop(createEngine(), 3, false, 0, 0, 7, Block.BLOCK_COLOR_GRAY);
		int filled = 0;
		for (int x = 0; x < 10; x++)
			if (!f.getBlockEmpty(x, 0)) filled++;
		assertEquals(3, filled);
	}

	@Test
	void garbageDropBigMode() {
		Field f = new Field(10, 6, 0, false);
		f.garbageDrop(createEngine(), 12, true);
		assertFalse(f.getBlockEmpty(0, 0));
		assertFalse(f.getBlockEmpty(0, 1));
	}

	// ══════════════════════════════════════════════════════════════════════
	// canCascade — lines 2577, 2585-2586, 2590-2591
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void canCascadeHandlesNullBlock() throws Exception {
		Field f = newField();
		f.setBlockColor(5, 10, Block.BLOCK_COLOR_RED);
		nullOutFieldBlock(f, 0, 0);
		assertTrue(f.canCascade());
	}

	@Test
	void canCascadeBlockedByWall() {
		Field f = newField();
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		assertFalse(f.canCascade());
	}

	@Test
	void canCascadeBlockedByBlockBelow() {
		Field f = newField();
		f.setBlockColor(5, 18, Block.BLOCK_COLOR_RED);
		f.setBlockColor(5, 19, Block.BLOCK_COLOR_RED);
		assertFalse(f.canCascade());
	}

	// ══════════════════════════════════════════════════════════════════════
	// addRandomHoverBlocks — lines 2672, 2686, 2695, 2702, 2792, 2821, 2850, 2865
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void addRandomHoverBlocksReturnsEarlyWhenAvoidLinesFalse() {
		// Line 2672: if (!avoidLines || colors.length == 1) return;
		Field f = new Field(4, 4, 0, false);
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};
		f.addRandomHoverBlocks(createEngine(), 8, colors, 0, false, false);
		int count = 0;
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(8, count);
	}

	@Test
	void addRandomHoverBlocksReturnsEarlyWhenOneColor() {
		// Line 2672: colors.length == 1
		Field f = new Field(4, 4, 0, false);
		int[] colors = {Block.BLOCK_COLOR_RED};
		f.addRandomHoverBlocks(createEngine(), 8, colors, 0, true, false);
		int count = 0;
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(8, count);
	}

	@Test
	void addRandomHoverBlocksExcessRemoval() throws Exception {
		Field f = new Field(4, 4, 0, false);
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_GRAY);
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_GREEN};
		f.addRandomHoverBlocks(createEngine(), 6, colors, 0, true, false);
		int count = 0;
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(7, count);
	}

	@Test
	void addRandomHoverBlocksFlashMode() {
		Field f = new Field(4, 4, 0, false);
		int[] colors = {Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_BLUE};
		f.addRandomHoverBlocks(createEngine(), 8, colors, 0, true, true);
		int count = 0;
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(8, count);
	}

	@Test
	void addRandomHoverBlocksFlashModeColorOutOfRange() {
		// Line 2850: colors[i] >= 2 && colors[i] <= 8
		Field f = new Field(4, 4, 0, false);
		int[] colors = {Block.BLOCK_COLOR_GRAY, Block.BLOCK_COLOR_RED};
		f.addRandomHoverBlocks(createEngine(), 8, colors, 0, true, true);
		int count = 0;
		for (int y = 0; y < 4; y++)
			for (int x = 0; x < 4; x++)
				if (f.getBlockColor(x, y) != Block.BLOCK_COLOR_NONE) count++;
		assertEquals(8, count);
	}

	// ══════════════════════════════════════════════════════════════════════
	// freeFall — line 2963 (while loop condition)
	// ══════════════════════════════════════════════════════════════════════

	@Test
	void freeFallFullColumnDoesNothing() {
		// Line 2963: while (!getBlockEmpty(x, y1) && y1 >= (-1 * hidden_height))
		Field f = newField();
		for (int y = -3; y < 20; y++)
			f.setBlockColor(5, y, Block.BLOCK_COLOR_RED);
		assertFalse(f.freeFall());
	}
}
