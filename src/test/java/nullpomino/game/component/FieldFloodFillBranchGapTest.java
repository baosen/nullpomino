package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for Field's flood-fill helpers: the recursive
 * {@code checkBlockLinkSub} and {@code setBlockLinkByColorSub} must tolerate
 * coordinates outside the field (null block), which happens when a border
 * block carries a connection attribute pointing off the field, or when the
 * public entry points are called with out-of-range coordinates.
 */
class FieldFloodFillBranchGapTest {

	private static Field field() {
		return new Field(10, 10, 0, false);
	}

	/**
	 * A corner block whose connection flags point off the field: the
	 * flood-fill recursion follows them to out-of-range coordinates and must
	 * treat the null block as a dead end instead of crashing.
	 */
	@Test
	void checkBlockLinkFollowsConnectionsOffTheField() {
		Field f = field();
		f.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED, 0,
				Block.BLOCK_ATTRIBUTE_CONNECT_LEFT | Block.BLOCK_ATTRIBUTE_CONNECT_UP));

		f.checkBlockLink(0, 0);

		assertTrue(f.getBlock(0, 0).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK),
				"root block gets marked despite dangling connections");
	}

	/** The public entry point must be a no-op for out-of-range coordinates. */
	@Test
	void setBlockLinkByColorToleratesOutOfRangeCoordinates() {
		Field f = field();
		f.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED, 0, 0));

		f.setBlockLinkByColor(-1, 0);
		f.setBlockLinkByColor(0, -1);
		f.setBlockLinkByColor(10, 0);

		assertFalse(f.getBlock(0, 0).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK),
				"nothing gets marked from an out-of-range start");
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(0, 0));
	}

	/** Same tolerance for checkBlockLink's public entry point. */
	@Test
	void checkBlockLinkToleratesOutOfRangeCoordinates() {
		Field f = field();
		f.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED, 0, 0));

		f.checkBlockLink(-1, 5);

		assertFalse(f.getBlock(0, 0).getAttribute(Block.BLOCK_ATTRIBUTE_TEMP_MARK));
	}
}
