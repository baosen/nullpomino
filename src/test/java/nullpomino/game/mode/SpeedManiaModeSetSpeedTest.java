package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link SpeedManiaMode}'s private {@code setSpeed} table-lookup
 * math via reflection. SPEED MANIA pegs gravity at -1 (instant fall)
 * and reads ARE / ARELine / LineDelay / LockDelay / DAS from five
 * 6-entry tables, picking the bucket from {@code level / 100} (clamped
 * to the last bucket once level reaches 500+).
 */
class SpeedManiaModeSetSpeedTest {

	@Test
	void levelZeroLandsAtFirstBucketAndPegsGravityAtMinusOne() throws Exception {
		// section 0: ARE=15, ARELine=11, LineDelay=12, LockDelay=31, DAS=11.
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"SPEED MANIA always pegs gravity at -1");
		assertEquals(15, engine.speed.are);
		assertEquals(11, engine.speed.areLine);
		assertEquals(12, engine.speed.lineDelay);
		assertEquals(31, engine.speed.lockDelay);
		assertEquals(11, engine.speed.das);
	}

	@Test
	void levelOneHundredLandsAtSecondBucket() throws Exception {
		// section 1: ARE=11, ARELine=5, LineDelay=6, LockDelay=27, DAS=11.
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 100;

		invokeSetSpeed(mode, engine);

		assertEquals(11, engine.speed.are);
		assertEquals(5, engine.speed.areLine);
		assertEquals(6, engine.speed.lineDelay);
		assertEquals(27, engine.speed.lockDelay);
		assertEquals(11, engine.speed.das);
	}

	@Test
	void levelFiveHundredLandsAtFinalBucket() throws Exception {
		// section 5 (last): ARE=3, ARELine=3, LineDelay=4, LockDelay=16, DAS=7.
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 500;

		invokeSetSpeed(mode, engine);

		assertEquals(3, engine.speed.are);
		assertEquals(3, engine.speed.areLine);
		assertEquals(4, engine.speed.lineDelay);
		assertEquals(16, engine.speed.lockDelay);
		assertEquals(7, engine.speed.das);
	}

	@Test
	void levelPastTheLastBucketClampsToFinalEntry() throws Exception {
		// Level 9999 / 100 = 99 -> clamped to section 5.
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 9999;

		invokeSetSpeed(mode, engine);

		assertEquals(3, engine.speed.are,
				"level past 500 clamps to section 5 (final bucket)");
		assertEquals(7, engine.speed.das);
	}

	@Test
	void midSectionLevelsFallIntoTheCorrectBucket() throws Exception {
		// Level 199 -> section 1 (199/100 = 1).
		// Level 250 -> section 2 (250/100 = 2): ARE=11, ARELine=5,
		// LineDelay=6, LockDelay=23, DAS=10.
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);

		engine.statistics.level = 199;
		invokeSetSpeed(mode, engine);
		assertEquals(11, engine.speed.are, "level 199 -> section 1");

		engine.statistics.level = 250;
		invokeSetSpeed(mode, engine);
		assertEquals(11, engine.speed.are, "level 250 -> section 2");
		assertEquals(23, engine.speed.lockDelay,
				"level 250 -> section 2 LockDelay = 23");
	}

	private static GameEngine freshEngine(SpeedManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(SpeedManiaMode mode, GameEngine engine)
			throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod(
				"setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
