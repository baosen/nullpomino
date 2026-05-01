package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class SpeedParamTest {

	@Test
	void defaultConstructorAppliesResetValues() {
		SpeedParam s = new SpeedParam();

		assertEquals(4, s.gravity);
		assertEquals(256, s.denominator);
		assertEquals(24, s.are);
		assertEquals(24, s.areLine);
		assertEquals(40, s.lineDelay);
		assertEquals(30, s.lockDelay);
		assertEquals(14, s.das);
	}

	@Test
	void resetRestoresDefaultsAfterMutation() {
		SpeedParam s = new SpeedParam();
		s.gravity = 99;
		s.denominator = 1;
		s.are = 0;
		s.areLine = 0;
		s.lineDelay = 0;
		s.lockDelay = 0;
		s.das = 0;

		s.reset();

		assertEquals(4, s.gravity);
		assertEquals(256, s.denominator);
		assertEquals(24, s.are);
		assertEquals(24, s.areLine);
		assertEquals(40, s.lineDelay);
		assertEquals(30, s.lockDelay);
		assertEquals(14, s.das);
	}

	@Test
	void copyConstructorProducesIndependentInstanceWithSameValues() {
		SpeedParam src = new SpeedParam();
		src.gravity = 1024;
		src.denominator = 60;
		src.are = 5;
		src.areLine = 6;
		src.lineDelay = 7;
		src.lockDelay = 8;
		src.das = 9;

		SpeedParam dst = new SpeedParam(src);

		assertNotSame(src, dst);
		assertEquals(1024, dst.gravity);
		assertEquals(60, dst.denominator);
		assertEquals(5, dst.are);
		assertEquals(6, dst.areLine);
		assertEquals(7, dst.lineDelay);
		assertEquals(8, dst.lockDelay);
		assertEquals(9, dst.das);

		src.gravity = 0;
		assertEquals(1024, dst.gravity);
	}

	@Test
	void copyOverwritesEveryField() {
		SpeedParam src = new SpeedParam();
		src.gravity = 1;
		src.denominator = 2;
		src.are = 3;
		src.areLine = 4;
		src.lineDelay = 5;
		src.lockDelay = 6;
		src.das = 7;

		SpeedParam dst = new SpeedParam();
		dst.copy(src);

		assertEquals(1, dst.gravity);
		assertEquals(2, dst.denominator);
		assertEquals(3, dst.are);
		assertEquals(4, dst.areLine);
		assertEquals(5, dst.lineDelay);
		assertEquals(6, dst.lockDelay);
		assertEquals(7, dst.das);
	}
}
