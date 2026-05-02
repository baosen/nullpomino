package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link MarathonMode}'s public {@code setSpeed} table walk —
 * the canonical slow-then-jump-then-sentinel layout shared with
 * Technician/MarathonPlus's first 20 entries: gravity 1 across levels
 * 0-12 (with denominator descending 63 -> 1), an abrupt jump to
 * 465/731/1280/1707 at indices 13-16, and the gravity=-1 instant-fall
 * sentinel at indices 17-19. Levels are clamped into [0, 19].
 */
class MarathonModeSetSpeedTest {

	@Test
	void negativeLevelClampsToFirstEntry() {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = -7;

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);
	}

	@Test
	void slowGravityZoneCoversLevelsZeroThroughTwelve() {
		// gravity stays at 1; denominator descends 63 -> 1.
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 12;

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity,
				"index 12 still slow-gravity zone");
		assertEquals(1, engine.speed.denominator,
				"index 12 -> denominator 1 (1G effective)");
	}

	@Test
	void levelThirteenJumpsGravity() {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 13;

		mode.setSpeed(engine);

		assertEquals(465, engine.speed.gravity,
				"index 13 jumps to 465");
		assertEquals(256, engine.speed.denominator);
	}

	@Test
	void levelSixteenIsLastFiniteGravityEntry() {
		// Index 16 -> gravity 1707 (last finite entry before -1).
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 16;

		mode.setSpeed(engine);

		assertEquals(1707, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
	}

	@Test
	void levelSeventeenHitsMinusOneSentinel() {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 17;

		mode.setSpeed(engine);

		assertEquals(-1, engine.speed.gravity,
				"index 17 -> -1 sentinel (instant fall)");
	}

	@Test
	void outOfRangeLevelClampsToLastIndex() {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 50;

		mode.setSpeed(engine);

		assertEquals(-1, engine.speed.gravity,
				"out-of-range level clamps to last entry -> -1");
		assertEquals(256, engine.speed.denominator);
	}

	private static GameEngine freshEngine(MarathonMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}
}
