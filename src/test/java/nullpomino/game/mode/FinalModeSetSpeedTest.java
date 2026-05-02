package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link FinalMode}'s private {@code setSpeed} via reflection.
 * FINAL pegs gravity at -1 and reads ARE / ARELine / LineDelay /
 * LockDelay / DAS from 10-entry tables indexed by level/100, with
 * ARE and ARELine sharing the same tableARE source. The lockDelay
 * has a version-dependent +1 fudge based on whether move-and-rotate
 * lockreset is configured.
 */
class FinalModeSetSpeedTest {

	@Test
	void levelZeroLandsAtFirstBucketAndPegsGravityAtMinusOne() throws Exception {
		// section 0: ARE=1, ARELine=1, LineDelay=0, LockDelay=9, DAS=4.
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 3);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"FINAL always pegs gravity at -1");
		assertEquals(1, engine.speed.are);
		assertEquals(1, engine.speed.areLine,
				"ARE and ARELine come from the same tableARE entry");
		assertEquals(0, engine.speed.lineDelay);
		assertEquals(4, engine.speed.das);
	}

	@Test
	void areAndAreLineAlwaysReadFromTheSameTable() throws Exception {
		// Across multiple sections, are == areLine.
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 3);

		for(int section = 0; section < 10; section++) {
			engine.statistics.level = section * 100;
			invokeSetSpeed(mode, engine);
			assertEquals(engine.speed.are, engine.speed.areLine,
					"section " + section + ": ARE and ARELine must match");
		}
	}

	@Test
	void levelNineHundredLandsAtFinalBucket() throws Exception {
		// section 9 (last): ARE=0, LineDelay=0, LockDelay=3, DAS=0.
		// version 3, default ruleopt -> the +1 fudge fires (move/rotate
		// reset both off in default ruleopt), so lockDelay = 4.
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 3);
		engine.ruleopt.lockresetMove = false;
		engine.ruleopt.lockresetRotate = false;
		engine.statistics.level = 900;

		invokeSetSpeed(mode, engine);

		assertEquals(0, engine.speed.are);
		assertEquals(0, engine.speed.areLine);
		assertEquals(0, engine.speed.lineDelay);
		assertEquals(0, engine.speed.das);
		assertEquals(4, engine.speed.lockDelay,
				"v3 + no-lockreset -> base 3 + 1 = 4");
	}

	@Test
	void levelPastNineHundredClampsToFinalBucket() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 3);
		engine.ruleopt.lockresetMove = false;
		engine.ruleopt.lockresetRotate = false;
		engine.statistics.level = 9999;

		invokeSetSpeed(mode, engine);

		assertEquals(0, engine.speed.das,
				"level past 900 clamps to section 9 -> DAS 0");
	}

	@Test
	void v3PlusLockResetMoveOrRotateDisablesTheLockDelayBumpFudge() throws Exception {
		// v3+ branch: +1 lockDelay fudge fires only when BOTH lockreset
		// flags are off. Setting either one to true skips the bump.
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 3);

		// section 0 -> base lockDelay = 9.
		engine.statistics.level = 0;
		engine.ruleopt.lockresetMove = true;
		engine.ruleopt.lockresetRotate = false;
		invokeSetSpeed(mode, engine);
		assertEquals(9, engine.speed.lockDelay,
				"v3 + move-lockreset on -> no fudge, lockDelay = 9");

		engine.ruleopt.lockresetMove = false;
		engine.ruleopt.lockresetRotate = true;
		invokeSetSpeed(mode, engine);
		assertEquals(9, engine.speed.lockDelay,
				"v3 + rotate-lockreset on -> no fudge, lockDelay = 9");

		engine.ruleopt.lockresetMove = false;
		engine.ruleopt.lockresetRotate = false;
		invokeSetSpeed(mode, engine);
		assertEquals(10, engine.speed.lockDelay,
				"v3 + both lockreset off -> +1 fudge, lockDelay = 10");
	}

	@Test
	void preV3LegacyBranchInvertsTheLockDelayBumpCondition() throws Exception {
		// Legacy (version < 3) branch: +1 lockDelay fudge fires when
		// EITHER lockreset flag is on (the inverse of the v3+ rule).
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "version", 0);

		engine.statistics.level = 0;
		engine.ruleopt.lockresetMove = true;
		engine.ruleopt.lockresetRotate = false;
		invokeSetSpeed(mode, engine);
		assertEquals(10, engine.speed.lockDelay,
				"legacy + move-lockreset -> +1 fudge, lockDelay = 10");

		engine.ruleopt.lockresetMove = false;
		engine.ruleopt.lockresetRotate = false;
		invokeSetSpeed(mode, engine);
		assertEquals(9, engine.speed.lockDelay,
				"legacy + both off -> no fudge, lockDelay = 9");
	}

	private static GameEngine freshEngine(FinalMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(FinalMode mode, GameEngine engine)
			throws Exception {
		Method m = FinalMode.class.getDeclaredMethod(
				"setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void setInt(Object instance, String name, int value)
			throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
