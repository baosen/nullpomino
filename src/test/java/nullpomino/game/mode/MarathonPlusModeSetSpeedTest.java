package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link MarathonPlusMode}'s private {@code setSpeed}. The same
 * shape as the classic Technician/Grade tables (clamp level to
 * [0, tableGravity.length-1=20] and read parallel gravity/denominator
 * tables) but with a 21st bonus-level entry tacked on at index 20:
 * gravity 1, denominator 4, so the bonus stage falls slowly. Line
 * delay is always pinned to 12.
 */
class MarathonPlusModeSetSpeedTest {

	@Test
	void negativeLevelClampsToFirstEntry() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = -3;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity,
				"negative level clamps to index 0 -> gravity 1");
		assertEquals(63, engine.speed.denominator,
				"negative level clamps to index 0 -> denominator 63");
	}

	@Test
	void slowGravityZoneCoversLevelsZeroThroughTwelve() throws Exception {
		// Levels 0-12 share gravity=1; denominators descend 63 -> 1.
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 6;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(12, engine.speed.denominator,
				"tableDenominator[6]=12");
	}

	@Test
	void levelThirteenJumpsToHighGravityZone() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 13;

		invokeSetSpeed(mode, engine);

		assertEquals(465, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
	}

	@Test
	void levelSeventeenHitsMinusOneSentinel() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 17;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"index 17 -> gravity -1 (instant fall)");
	}

	@Test
	void bonusLevelIndexTwentyHasSlowGravityOneOverFour() throws Exception {
		// Index 20 is the marathon-plus bonus level — gravity drops
		// back to 1 with denominator 4 (slow fall again).
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 20;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity,
				"bonus level: gravity drops back to 1");
		assertEquals(4, engine.speed.denominator,
				"bonus level: denominator 4 -> 1/4 fall speed");
	}

	@Test
	void outOfRangeLevelClampsToBonusEntry() throws Exception {
		// Level 99 clamps to last index = 20 (bonus level entry).
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 99;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity,
				"out-of-range level clamps to index 20 -> gravity 1");
		assertEquals(4, engine.speed.denominator);
	}

	@Test
	void lineDelayIsAlwaysTwelveRegardlessOfLevel() throws Exception {
		// lineDelay is unconditional: 12 frames regardless of level.
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);

		engine.statistics.level = 0;
		invokeSetSpeed(mode, engine);
		assertEquals(12, engine.speed.lineDelay);

		engine.statistics.level = 17;
		invokeSetSpeed(mode, engine);
		assertEquals(12, engine.speed.lineDelay,
				"lineDelay unaffected by gravity-table jump");

		engine.statistics.level = 20;
		invokeSetSpeed(mode, engine);
		assertEquals(12, engine.speed.lineDelay,
				"lineDelay unaffected by bonus level");
	}

	private static GameEngine freshEngine(MarathonPlusMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(MarathonPlusMode mode, GameEngine engine)
			throws Exception {
		Method m = MarathonPlusMode.class.getDeclaredMethod(
				"setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
