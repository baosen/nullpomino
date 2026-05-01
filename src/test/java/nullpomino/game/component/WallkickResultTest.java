package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WallkickResultTest {

	@Test
	void defaultConstructorZeroesEveryField() {
		WallkickResult r = new WallkickResult();

		assertEquals(0, r.offsetX);
		assertEquals(0, r.offsetY);
		assertEquals(0, r.direction);
	}

	@Test
	void parameterisedConstructorAssignsFields() {
		WallkickResult r = new WallkickResult(-1, 2, 3);

		assertEquals(-1, r.offsetX);
		assertEquals(2, r.offsetY);
		assertEquals(3, r.direction);
	}

	@Test
	void resetClearsMutatedFields() {
		WallkickResult r = new WallkickResult(5, -5, 2);

		r.reset();

		assertEquals(0, r.offsetX);
		assertEquals(0, r.offsetY);
		assertEquals(0, r.direction);
	}

	@Test
	void copyConstructorProducesIndependentInstanceWithSameValues() {
		WallkickResult src = new WallkickResult(7, -8, 1);

		WallkickResult dst = new WallkickResult(src);

		assertNotSame(src, dst);
		assertEquals(7, dst.offsetX);
		assertEquals(-8, dst.offsetY);
		assertEquals(1, dst.direction);

		src.offsetX = 0;
		assertEquals(7, dst.offsetX);
	}

	@Test
	void copyOverwritesEveryField() {
		WallkickResult src = new WallkickResult(4, 5, 6);
		WallkickResult dst = new WallkickResult();

		dst.copy(src);

		assertEquals(4, dst.offsetX);
		assertEquals(5, dst.offsetY);
		assertEquals(6, dst.direction);
	}

	@Test
	void isUpwardOnlyTrueForNegativeOffsetY() {
		assertTrue(new WallkickResult(0, -1, 0).isUpward());
		assertTrue(new WallkickResult(3, -10, 2).isUpward());
		assertFalse(new WallkickResult(0, 0, 0).isUpward());
		assertFalse(new WallkickResult(0, 1, 0).isUpward());
	}
}
