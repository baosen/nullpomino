package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import nullpomino.util.CustomProperties;

class FieldDataTest {

	@Test
	void copyConstructorClonesGridIndependently() {
		Field src = new Field(10, 20, 3);
		src.setBlockColor(3, 5, Block.BLOCK_COLOR_BLUE);

		Field copy = new Field(src);

		assertNotSame(src, copy);
		assertEquals(Block.BLOCK_COLOR_BLUE, copy.getBlockColor(3, 5));
		src.setBlockColor(3, 5, Block.BLOCK_COLOR_RED);
		assertEquals(Block.BLOCK_COLOR_BLUE, copy.getBlockColor(3, 5));
	}

	@Test
	void copyMethodReplicatesDimensionsAndState() {
		Field src = new Field(8, 22, 4, true);
		src.colorClearExtraCount = 7;
		src.colorsCleared = 3;
		src.gemsCleared = 2;
		src.garbageCleared = 5;
		src.setBlockColor(2, 21, Block.BLOCK_COLOR_GREEN);

		Field dst = new Field();
		dst.copy(src);

		assertEquals(8, dst.getWidth());
		assertEquals(22, dst.getHeight());
		assertEquals(4, dst.getHiddenHeight());
		assertTrue(dst.ceiling);
		assertEquals(7, dst.colorClearExtraCount);
		assertEquals(3, dst.colorsCleared);
		assertEquals(2, dst.gemsCleared);
		assertEquals(5, dst.garbageCleared);
		assertEquals(Block.BLOCK_COLOR_GREEN, dst.getBlockColor(2, 21));
	}

	@Test
	void writeAndReadPropertyRoundTripVisibleGridContents() {
		Field src = new Field(10, 20, 3);
		src.setBlockColor(0, 19, Block.BLOCK_COLOR_RED);
		src.setBlockColor(9, 19, Block.BLOCK_COLOR_BLUE);
		src.setBlockColor(5, 0, Block.BLOCK_COLOR_GRAY);
		CustomProperties prop = new CustomProperties();

		src.writeProperty(prop, 7);
		Field dst = new Field(10, 20, 3);
		dst.readProperty(prop, 7);

		assertEquals(Block.BLOCK_COLOR_RED, dst.getBlockColor(0, 19));
		assertEquals(Block.BLOCK_COLOR_BLUE, dst.getBlockColor(9, 19));
		assertEquals(Block.BLOCK_COLOR_GRAY, dst.getBlockColor(5, 0));
	}

	@Test
	void readPropertyTreatsMalformedColorEntriesAsEmpty() {
		Field dst = new Field(10, 20, 3);
		CustomProperties prop = new CustomProperties();
		prop.setProperty("0.field.map.19", "not-a-number,2,3,4,5,6,7,8,9,10");

		dst.readProperty(prop, 0);

		assertEquals(Block.BLOCK_COLOR_NONE, dst.getBlockColor(0, 19));
		assertEquals(2, dst.getBlockColor(1, 19));
	}

	@Test
	void getCoordAttributeRoutesThroughCeilingWallNormalHiddenAndVanish() {
		Field withCeiling = new Field(10, 20, 3, true);
		assertEquals(Field.COORD_WALL, withCeiling.getCoordAttribute(0, -1));

		Field plain = new Field(10, 20, 3);
		assertEquals(Field.COORD_WALL, plain.getCoordAttribute(-1, 0));
		assertEquals(Field.COORD_WALL, plain.getCoordAttribute(plain.getWidth(), 0));
		assertEquals(Field.COORD_WALL, plain.getCoordAttribute(0, plain.getHeight()));
		assertEquals(Field.COORD_NORMAL, plain.getCoordAttribute(0, 0));
		assertEquals(Field.COORD_HIDDEN, plain.getCoordAttribute(0, -1));
		assertEquals(Field.COORD_VANISH, plain.getCoordAttribute(0, -plain.getHiddenHeight() - 1));
	}

	@Test
	void getRowAndGetBlockReturnNullForOutOfRangeYAndThrowingVariantsThrow() {
		Field f = new Field(10, 20, 3);

		assertNotNull(f.getRow(0));
		assertNotNull(f.getRow(-1));
		assertNull(f.getRow(f.getHeight()));
		assertNotNull(f.getBlock(0, 0));
		assertNull(f.getBlock(-1, 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.getBlockE(-1, 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.getRowE(f.getHeight()));
	}

	@Test
	void setBlockReturnsFalseForOutOfRangeOrNullCoordinates() {
		Field f = new Field(10, 20, 3);
		Block b = new Block(Block.BLOCK_COLOR_RED);

		assertTrue(f.setBlock(0, 0, b));
		assertFalse(f.setBlock(-1, 0, b));
		assertFalse(f.setBlock(0, f.getHeight(), b));
		// NPE branch — pass a null Block; getBlockE returns a valid block
		// but copy(null) inside setBlockE throws NullPointerException, which
		// the public setBlock catches as a quiet failure for avalanche modes.
		assertFalse(f.setBlock(0, 0, null));
	}

	@Test
	void getBlockColorWithGemSameMapsGemColorsToNormalEquivalents() {
		Field f = new Field(10, 20, 3);
		f.setBlockColor(2, 5, Block.BLOCK_COLOR_GEM_RED);

		assertEquals(Block.BLOCK_COLOR_GEM_RED, f.getBlockColor(2, 5));
		assertEquals(Block.BLOCK_COLOR_RED, f.getBlockColor(2, 5, true));
		assertEquals(Block.BLOCK_COLOR_GEM_RED, f.getBlockColor(2, 5, false));
	}

	@Test
	void setBlockColorReturnsFalseForOutOfRangeAndTrueForInRange() {
		Field f = new Field(10, 20, 3);

		assertTrue(f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED));
		assertFalse(f.setBlockColor(-1, 0, Block.BLOCK_COLOR_RED));
	}

	@Test
	void getBlockColorEAndSetBlockColorEThrowOnOutOfRange() {
		Field f = new Field(10, 20, 3);

		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> f.getBlockColorE(-1, 0));
		assertThrows(ArrayIndexOutOfBoundsException.class,
				() -> f.setBlockColorE(-1, 0, Block.BLOCK_COLOR_RED));
	}

	@Test
	void setLineFlagAndGetLineFlagRoundTrip() {
		Field f = new Field(10, 20, 3);

		assertFalse(f.getLineFlag(5));
		assertTrue(f.setLineFlag(5, true));
		assertTrue(f.getLineFlag(5));
		assertFalse(f.setLineFlag(-9999, true));
		assertFalse(f.getLineFlag(-9999));
	}

	@Test
	void getBlockEmptyReportsTrueForFreshGridAndFalseAfterSet() {
		Field f = new Field(10, 20, 3);

		assertTrue(f.getBlockEmpty(0, 0));
		f.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
		assertFalse(f.getBlockEmpty(0, 0));
		assertTrue(f.getBlockEmpty(-1, 0));

		assertTrue(f.getBlockEmptyF(5, 5));
		f.setBlockColor(5, 5, Block.BLOCK_COLOR_BLUE);
		assertFalse(f.getBlockEmptyF(5, 5));

		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.getBlockEmptyE(-1, 0));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.getLineFlagE(-9999));
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> f.setLineFlagE(-9999, true));
	}
}
