package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link PhantomManiaMode}'s private {@code setSpeed} via
 * reflection. PHANTOM MANIA shares the SPEED MANIA shape: gravity
 * pegged at -1, ARE / ARELine / LineDelay / LockDelay / DAS read from
 * five 6-entry tables indexed by level/100, clamped to the last
 * bucket past 500.
 *
 * <p>The default tables match SPEED MANIA exactly (so the byte shape
 * is parallel between the two modes); pin both the values and the
 * clamp here.
 */
class PhantomManiaModeSetSpeedTest {

	@Test
	void levelZeroLandsAtFirstBucketAndPegsGravityAtMinusOne() throws Exception {
		// section 0: ARE=15, ARELine=11, LineDelay=12, LockDelay=31, DAS=11.
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"PHANTOM MANIA always pegs gravity at -1");
		assertEquals(15, engine.speed.are);
		assertEquals(11, engine.speed.areLine);
		assertEquals(12, engine.speed.lineDelay);
		assertEquals(31, engine.speed.lockDelay);
		assertEquals(11, engine.speed.das);
	}

	@Test
	void levelOneHundredLandsAtSecondBucket() throws Exception {
		// section 1: ARE=11, ARELine=5, LineDelay=6, LockDelay=27, DAS=11.
		PhantomManiaMode mode = new PhantomManiaMode();
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
		PhantomManiaMode mode = new PhantomManiaMode();
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
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 9999;

		invokeSetSpeed(mode, engine);

		assertEquals(3, engine.speed.are,
				"level past 500 clamps to section 5 (final bucket)");
		assertEquals(7, engine.speed.das);
	}

	private static GameEngine freshEngine(PhantomManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(PhantomManiaMode mode, GameEngine engine)
			throws Exception {
		Method m = PhantomManiaMode.class.getDeclaredMethod(
				"setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
