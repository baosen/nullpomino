package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link RetroMasteryMode}'s private {@code setSpeed}. The 31-
 * entry tables ramp lockDelay from 60 down to 6, denominator from 48
 * down to 1, and gravity stays at 1 through level 11 then jumps
 * irregularly (2, 1, 2, 1, 2, 1, 4×4, 8×8) before the level-30 cap.
 * Line delay is a clean two-bucket split: 25 frames below level 10,
 * 20 frames at level 10+.
 */
class RetroMasteryModeSetSpeedTest {

	@Test
	void negativeLevelClampsToFirstEntry() throws Exception {
		// lv < 0 -> 0 -> gravity 1, denominator 48, lockDelay 60.
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = -2;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(48, engine.speed.denominator);
		assertEquals(60, engine.speed.lockDelay,
				"index 0 -> longest lock delay (60 frames)");
	}

	@Test
	void levelFiveReadsMidSlowZone() throws Exception {
		// Index 5 -> gravity 1, denominator 18, lockDelay 30.
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 5;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(18, engine.speed.denominator);
		assertEquals(30, engine.speed.lockDelay);
	}

	@Test
	void levelTwelveJumpsGravityToTwo() throws Exception {
		// Index 12 is the first non-1 gravity entry: gravity 2,
		// denominator 11, lockDelay 17.
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 12;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.gravity,
				"index 12 -> gravity table jumps to 2 for the first time");
		assertEquals(11, engine.speed.denominator);
		assertEquals(17, engine.speed.lockDelay);
	}

	@Test
	void levelEighteenHitsGravityFourBlock() throws Exception {
		// Indices 18-21 share gravity 4.
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 18;

		invokeSetSpeed(mode, engine);

		assertEquals(4, engine.speed.gravity);
		assertEquals(11, engine.speed.denominator,
				"tableDenominator[18]=11");
		assertEquals(11, engine.speed.lockDelay,
				"tableLockDelay[18]=11");
	}

	@Test
	void levelTwentyTwoHitsGravityEightBlock() throws Exception {
		// Indices 22-29 share gravity 8 (the fastest pre-cap entry).
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 22;

		invokeSetSpeed(mode, engine);

		assertEquals(8, engine.speed.gravity);
		assertEquals(15, engine.speed.denominator);
		assertEquals(8, engine.speed.lockDelay);
	}

	@Test
	void levelThirtyHitsLastEntryWithGravityOneOverOne() throws Exception {
		// Index 30 (the cap): gravity 1 / denominator 1 = 1G,
		// lockDelay 6 (shortest).
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 30;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(1, engine.speed.denominator,
				"index 30 -> denominator 1 (effective 1G fall)");
		assertEquals(6, engine.speed.lockDelay,
				"index 30 -> shortest lockDelay (6 frames)");
	}

	@Test
	void outOfRangeLevelClampsToIndexThirty() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 100;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity,
				"out-of-range -> clamp to index 30");
		assertEquals(1, engine.speed.denominator);
		assertEquals(6, engine.speed.lockDelay);
	}

	@Test
	void lineDelayIsTwentyFiveBelowLevelTen() throws Exception {
		// lineDelay = lv >= 10 ? 20 : 25.
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 9;

		invokeSetSpeed(mode, engine);

		assertEquals(25, engine.speed.lineDelay,
				"level 9 -> lineDelay 25 (slow zone)");
	}

	@Test
	void lineDelayDropsToTwentyAtLevelTen() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 10;

		invokeSetSpeed(mode, engine);

		assertEquals(20, engine.speed.lineDelay,
				"level 10 -> lineDelay drops to 20 (fast zone)");
	}

	private static GameEngine freshEngine(RetroMasteryMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(RetroMasteryMode mode, GameEngine engine)
			throws Exception {
		Method m = RetroMasteryMode.class.getDeclaredMethod(
				"setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
