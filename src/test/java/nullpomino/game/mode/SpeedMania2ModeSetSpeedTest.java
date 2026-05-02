package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link SpeedMania2Mode}'s private {@code setSpeed} table-lookup
 * math via reflection. SPEED MANIA 2 follows the same pattern as
 * SPEED MANIA but with 14 buckets instead of 6 (one bucket per 100
 * levels up to level 1300, last entry at section 13). Gravity is
 * pegged at -1 and the bucket index is clamped at the last entry.
 */
class SpeedMania2ModeSetSpeedTest {

	@Test
	void levelZeroLandsAtFirstBucket() throws Exception {
		// section 0: ARE=8, ARELine=4, LineDelay=6, LockDelay=19, DAS=9.
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"SPEED MANIA 2 always pegs gravity at -1");
		assertEquals(8, engine.speed.are);
		assertEquals(4, engine.speed.areLine);
		assertEquals(6, engine.speed.lineDelay);
		assertEquals(19, engine.speed.lockDelay);
		assertEquals(9, engine.speed.das);
	}

	@Test
	void levelTwelveHundredLandsAtSecondToLastBucket() throws Exception {
		// section 12: ARE=2, ARELine=1, LineDelay=3, LockDelay=9, DAS=5.
		// (The last entry is the 'rollover' bucket with different shape.)
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 1200;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.are);
		assertEquals(1, engine.speed.areLine);
		assertEquals(3, engine.speed.lineDelay);
		assertEquals(9, engine.speed.lockDelay);
		assertEquals(5, engine.speed.das);
	}

	@Test
	void levelThirteenHundredLandsAtFinalRolloverBucket() throws Exception {
		// section 13 (final): ARE=2, ARELine=2, LineDelay=6, LockDelay=16,
		// DAS=5. This is the rollover bucket at the very end.
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 1300;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.are);
		assertEquals(2, engine.speed.areLine);
		assertEquals(6, engine.speed.lineDelay);
		assertEquals(16, engine.speed.lockDelay);
		assertEquals(5, engine.speed.das);
	}

	@Test
	void levelPastTheLastBucketClampsToFinalEntry() throws Exception {
		// Level 99999 / 100 = 999 -> clamped to section 13.
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 99999;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.are,
				"level past 1300 clamps to section 13");
		assertEquals(16, engine.speed.lockDelay);
	}

	@Test
	void levelThreeHundredLandsAtFourthBucket() throws Exception {
		// section 3: ARE=2, ARELine=2, LineDelay=4, LockDelay=16, DAS=7.
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 300;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.are);
		assertEquals(2, engine.speed.areLine);
		assertEquals(4, engine.speed.lineDelay);
		assertEquals(16, engine.speed.lockDelay);
		assertEquals(7, engine.speed.das);
	}

	private static GameEngine freshEngine(SpeedMania2Mode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(SpeedMania2Mode mode, GameEngine engine)
			throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod(
				"setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
