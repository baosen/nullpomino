package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BackgroundStatusTest {

	@Test
	void defaultConstructorAppliesResetValues() {
		BackgroundStatus s = new BackgroundStatus();

		assertEquals(0, s.bg);
		assertFalse(s.fadesw);
		assertFalse(s.fadestat);
		assertEquals(0, s.fadecount);
		assertEquals(0, s.fadebg);
	}

	@Test
	void resetRestoresDefaultsAfterMutation() {
		BackgroundStatus s = new BackgroundStatus();
		s.bg = 7;
		s.fadesw = true;
		s.fadestat = true;
		s.fadecount = 50;
		s.fadebg = 9;

		s.reset();

		assertEquals(0, s.bg);
		assertFalse(s.fadesw);
		assertFalse(s.fadestat);
		assertEquals(0, s.fadecount);
		assertEquals(0, s.fadebg);
	}

	@Test
	void copyConstructorProducesIndependentInstanceWithSameValues() {
		BackgroundStatus src = new BackgroundStatus();
		src.bg = 3;
		src.fadesw = true;
		src.fadestat = true;
		src.fadecount = 30;
		src.fadebg = 4;

		BackgroundStatus dst = new BackgroundStatus(src);

		assertNotSame(src, dst);
		assertEquals(3, dst.bg);
		assertTrue(dst.fadesw);
		assertTrue(dst.fadestat);
		assertEquals(30, dst.fadecount);
		assertEquals(4, dst.fadebg);

		src.bg = 0;
		assertEquals(3, dst.bg);
	}

	@Test
	void copyOverwritesEveryField() {
		BackgroundStatus src = new BackgroundStatus();
		src.bg = 5;
		src.fadesw = true;
		src.fadestat = true;
		src.fadecount = 10;
		src.fadebg = 6;

		BackgroundStatus dst = new BackgroundStatus();
		dst.copy(src);

		assertEquals(5, dst.bg);
		assertTrue(dst.fadesw);
		assertTrue(dst.fadestat);
		assertEquals(10, dst.fadecount);
		assertEquals(6, dst.fadebg);
	}

	@Test
	void fadeUpdateIsNoOpWhenFadeDisabled() {
		BackgroundStatus s = new BackgroundStatus();
		s.fadesw = false;
		s.bg = 2;
		s.fadebg = 8;
		s.fadecount = 42;
		s.fadestat = true;

		s.fadeUpdate();

		assertEquals(2, s.bg);
		assertEquals(8, s.fadebg);
		assertEquals(42, s.fadecount);
		assertTrue(s.fadestat);
		assertFalse(s.fadesw);
	}

	@Test
	void fadeUpdateAdvancesCounterByTenWhileBelowHundred() {
		BackgroundStatus s = new BackgroundStatus();
		s.fadesw = true;
		s.fadecount = 0;
		s.bg = 1;
		s.fadebg = 7;

		s.fadeUpdate();

		assertEquals(10, s.fadecount);
		assertEquals(1, s.bg);
		assertFalse(s.fadestat);
		assertTrue(s.fadesw);
	}

	@Test
	void fadeUpdateSwitchesToFadeInOnceCounterReachesHundred() {
		BackgroundStatus s = new BackgroundStatus();
		s.fadesw = true;
		s.fadecount = 100;
		s.fadestat = false;
		s.bg = 1;
		s.fadebg = 7;

		s.fadeUpdate();

		assertEquals(7, s.bg);
		assertTrue(s.fadestat);
		assertEquals(0, s.fadecount);
		assertTrue(s.fadesw);
	}

	@Test
	void fadeUpdateClearsFadeStateWhenFadeInCompletes() {
		BackgroundStatus s = new BackgroundStatus();
		s.fadesw = true;
		s.fadecount = 100;
		s.fadestat = true;
		s.bg = 7;
		s.fadebg = 7;

		s.fadeUpdate();

		assertFalse(s.fadesw);
		assertFalse(s.fadestat);
		assertEquals(0, s.fadecount);
		assertEquals(7, s.bg);
	}
}
