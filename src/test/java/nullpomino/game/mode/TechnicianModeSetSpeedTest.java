package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link TechnicianMode}'s private {@code setSpeed}. Looks up
 * gravity and denominator in two parallel 20-entry tables, indexed by
 * {@code engine.statistics.level} clamped into [0, length-1]. The
 * gravity table climbs slowly through level 12 (gravity 1) then jumps
 * to 465 → 731 → 1280 → 1707 before the {@code -1} sentinel saturates
 * at index 17+; the denominator table starts at 63, descends to 1, then
 * pegs at 256 from index 13 onward.
 */
class TechnicianModeSetSpeedTest {

	@Test
	void negativeLevelClampsToFirstEntry() throws Exception {
		// lv < 0 -> clamp to 0 -> tableGravity[0]=1, tableDenominator[0]=63.
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = -5;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity,
				"negative level clamps to index 0 -> gravity 1");
		assertEquals(63, engine.speed.denominator,
				"negative level clamps to index 0 -> denominator 63");
	}

	@Test
	void levelZeroReadsFirstTableEntry() throws Exception {
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);
	}

	@Test
	void levelTwelveIsLastSlowGravityEntry() throws Exception {
		// Index 12 is the boundary — gravity is still 1 but denominator
		// has dropped to 1 (so effective fall speed equals 1/1 = 1G).
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 12;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity,
				"index 12 still in slow-gravity zone -> gravity 1");
		assertEquals(1, engine.speed.denominator,
				"index 12 -> denominator hits 1 (effective 1G fall)");
	}

	@Test
	void levelThirteenJumpsToHighGravityZone() throws Exception {
		// Index 13 is where the table jumps: gravity 465, denominator 256.
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 13;

		invokeSetSpeed(mode, engine);

		assertEquals(465, engine.speed.gravity,
				"index 13 -> gravity table jumps to 465");
		assertEquals(256, engine.speed.denominator,
				"index 13 -> denominator pegs at 256");
	}

	@Test
	void levelSeventeenHitsMinusOneSentinel() throws Exception {
		// Index 17 -> gravity = -1 (instant fall).
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 17;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"index 17 -> gravity = -1 sentinel (instant fall)");
		assertEquals(256, engine.speed.denominator);
	}

	@Test
	void levelAtOrAboveTableLengthClampsToLastEntry() throws Exception {
		// lv 19 is the last index; lv 50 clamps to 19 -> same values.
		TechnicianMode mode = new TechnicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 50;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"out-of-range level clamps to last index -> gravity -1");
		assertEquals(256, engine.speed.denominator,
				"out-of-range level clamps to last index -> denominator 256");
	}

	private static GameEngine freshEngine(TechnicianMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(TechnicianMode mode, GameEngine engine)
			throws Exception {
		Method m = TechnicianMode.class.getDeclaredMethod(
				"setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
