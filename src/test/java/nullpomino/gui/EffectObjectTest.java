package nullpomino.gui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class EffectObjectTest {

	@Test
	void defaultConstructorZeroesEveryField() {
		EffectObject e = new EffectObject();

		assertEquals(0, e.effect);
		assertEquals(0, e.x);
		assertEquals(0, e.y);
		assertEquals(0, e.param);
		assertEquals(0, e.anim);
	}

	@Test
	void parameterisedConstructorAssignsFieldsAndZeroesAnim() {
		EffectObject e = new EffectObject(3, 10, 20, 7);

		assertEquals(3, e.effect);
		assertEquals(10, e.x);
		assertEquals(20, e.y);
		assertEquals(7, e.param);
		assertEquals(0, e.anim);
	}

	@Test
	void copyConstructorReplicatesEveryFieldAndIsIndependent() {
		EffectObject src = new EffectObject(1, 2, 3, 4);
		src.anim = 9;

		EffectObject copy = new EffectObject(src);

		assertNotSame(src, copy);
		assertEquals(1, copy.effect);
		assertEquals(2, copy.x);
		assertEquals(3, copy.y);
		assertEquals(4, copy.param);
		assertEquals(9, copy.anim);

		src.effect = 99;
		assertEquals(1, copy.effect);
	}
}
