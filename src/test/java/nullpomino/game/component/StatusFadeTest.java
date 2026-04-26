package nullpomino.game.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class StatusFadeTest {

	@Test
	void bgmResetCopyAndFadeBehavior() {
		BGMStatus status = new BGMStatus();
		assertEquals(BGMStatus.BGM_NOTHING, status.bgm);
		assertEquals(1f, status.volume);
		assertFalse(status.fadesw);

		status.bgm = BGMStatus.BGM_NORMAL1;
		status.volume = 0.5f;
		status.fadesw = true;
		BGMStatus copy = new BGMStatus(status);
		assertEquals(status.bgm, copy.bgm);
		assertEquals(status.volume, copy.volume);
		assertEquals(status.fadesw, copy.fadesw);

		status.fadeUpdate();
		assertEquals(0.495f, status.volume, 0.0001f);

		status.volume = -0.1f;
		status.fadeUpdate();
		assertEquals(0f, status.volume);

		status.fadesw = false;
		status.volume = 0.25f;
		status.fadeUpdate();
		assertEquals(1f, status.volume);
	}

	@Test
	void backgroundFadeSwitchesImageThenCompletes() {
		BackgroundStatus status = new BackgroundStatus();
		status.bg = 1;
		status.fadebg = 2;
		status.fadesw = true;
		status.fadecount = 100;

		status.fadeUpdate();

		assertEquals(2, status.bg);
		assertTrue(status.fadestat);
		assertEquals(0, status.fadecount);
		assertTrue(status.fadesw);

		status.fadecount = 100;
		status.fadeUpdate();

		assertEquals(2, status.bg);
		assertFalse(status.fadestat);
		assertFalse(status.fadesw);
		assertEquals(0, status.fadecount);
	}

	@Test
	void backgroundFadeDoesNothingWhenDisabledAndCountsByTenWhenActive() {
		BackgroundStatus status = new BackgroundStatus();
		status.fadeUpdate();
		assertEquals(0, status.fadecount);

		status.fadesw = true;
		status.fadeUpdate();
		assertEquals(10, status.fadecount);

		BackgroundStatus copy = new BackgroundStatus(status);
		assertEquals(status.bg, copy.bg);
		assertEquals(status.fadebg, copy.fadebg);
		assertEquals(status.fadecount, copy.fadecount);
		assertEquals(status.fadesw, copy.fadesw);
		assertEquals(status.fadestat, copy.fadestat);
	}
}
