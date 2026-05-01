package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BGMStatusTest {

	@Test
	void defaultConstructorAppliesResetValues() {
		BGMStatus s = new BGMStatus();

		assertEquals(BGMStatus.BGM_NOTHING, s.bgm);
		assertEquals(1f, s.volume);
		assertFalse(s.fadesw);
	}

	@Test
	void resetRestoresDefaultsAfterMutation() {
		BGMStatus s = new BGMStatus();
		s.bgm = BGMStatus.BGM_SPECIAL4;
		s.volume = 0.25f;
		s.fadesw = true;

		s.reset();

		assertEquals(BGMStatus.BGM_NOTHING, s.bgm);
		assertEquals(1f, s.volume);
		assertFalse(s.fadesw);
	}

	@Test
	void copyConstructorProducesIndependentInstanceWithSameValues() {
		BGMStatus src = new BGMStatus();
		src.bgm = BGMStatus.BGM_PUZZLE2;
		src.volume = 0.5f;
		src.fadesw = true;

		BGMStatus dst = new BGMStatus(src);

		assertNotSame(src, dst);
		assertEquals(BGMStatus.BGM_PUZZLE2, dst.bgm);
		assertEquals(0.5f, dst.volume);
		assertTrue(dst.fadesw);

		src.bgm = BGMStatus.BGM_NOTHING;
		assertEquals(BGMStatus.BGM_PUZZLE2, dst.bgm);
	}

	@Test
	void copyOverwritesEveryField() {
		BGMStatus src = new BGMStatus();
		src.bgm = BGMStatus.BGM_ENDING1;
		src.volume = 0.75f;
		src.fadesw = true;

		BGMStatus dst = new BGMStatus();
		dst.copy(src);

		assertEquals(BGMStatus.BGM_ENDING1, dst.bgm);
		assertEquals(0.75f, dst.volume);
		assertTrue(dst.fadesw);
	}

	@Test
	void fadeUpdateClampsVolumeToOneWhenFadeDisabled() {
		BGMStatus s = new BGMStatus();
		s.fadesw = false;
		s.volume = 0.4f;

		s.fadeUpdate();

		assertEquals(1f, s.volume);
	}

	@Test
	void fadeUpdateLeavesVolumeAtOneWhenFadeDisabled() {
		BGMStatus s = new BGMStatus();
		s.fadesw = false;
		s.volume = 1f;

		s.fadeUpdate();

		assertEquals(1f, s.volume);
	}

	@Test
	void fadeUpdateDecrementsVolumeByPointFiveThousandthsWhenFadingOut() {
		BGMStatus s = new BGMStatus();
		s.fadesw = true;
		s.volume = 0.5f;

		s.fadeUpdate();

		assertEquals(0.5f - 0.005f, s.volume, 1e-6f);
	}

	@Test
	void fadeUpdateClampsNegativeVolumeToZero() {
		BGMStatus s = new BGMStatus();
		s.fadesw = true;
		s.volume = -0.1f;

		s.fadeUpdate();

		assertEquals(0f, s.volume);
	}

	@Test
	void fadeUpdateLeavesZeroVolumeUnchangedWhileFading() {
		BGMStatus s = new BGMStatus();
		s.fadesw = true;
		s.volume = 0f;

		s.fadeUpdate();

		assertEquals(0f, s.volume);
		assertTrue(s.fadesw);
	}

	@Test
	void bgmConstantsCoverAllSlots() {
		assertEquals(-1, BGMStatus.BGM_NOTHING);
		assertEquals(0, BGMStatus.BGM_NORMAL1);
		assertEquals(15, BGMStatus.BGM_SPECIAL4);
		assertEquals(16, BGMStatus.BGM_COUNT);
	}
}
