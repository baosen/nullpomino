package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GradeMania3Mode}'s private {@code setSpeed} via
 * reflection. Two layered branches:
 * <ol>
 *   <li>Gravity: always20g OR time >= 54000 (15-minute mark) pegs at
 *       -1; otherwise the walker advances gravityindex past every
 *       tableGravityChangeLevel entry the internalLevel reaches.</li>
 *   <li>Delays: time >= 54000 forces a fixed delay set
 *       (ARE=2, ARELine=1, LineDelay=3, LockDelay=13, DAS=5);
 *       otherwise the section index = internalLevel/100 (clamped to
 *       the last entry) reads delays from 13-entry tables.</li>
 * </ol>
 */
class GradeMania3ModeSetSpeedTest {

	@Test
	void always20gPegsGravityAtMinusOne() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", true);
		setInt(mode, "internalLevel", 0);
		engine.statistics.time = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"always20g -> gravity = -1 instant fall");
	}

	@Test
	void timePastFiftyFourThousandPegsGravityAtMinusOneAndUsesFixedDelays() throws Exception {
		// time >= 54000 (15 min) forces gravity=-1 AND the fixed
		// late-game delay set.
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "internalLevel", 100);
		engine.statistics.time = 54000;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"time >= 54000 -> gravity = -1");
		assertEquals(2, engine.speed.are,
				"time >= 54000 -> fixed ARE = 2");
		assertEquals(1, engine.speed.areLine);
		assertEquals(3, engine.speed.lineDelay);
		assertEquals(13, engine.speed.lockDelay);
		assertEquals(5, engine.speed.das);
	}

	@Test
	void normalBranchWithLevelZeroReadsFirstTableEntries() throws Exception {
		// internalLevel < 30, time < 54000 -> gravityindex 0,
		// section 0: ARE=23, ARELine=23, LineDelay=40, LockDelay=31,
		// DAS=15.
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "internalLevel", 0);
		setInt(mode, "gravityindex", 0);
		engine.statistics.time = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(4, engine.speed.gravity,
				"level 0 -> gravity table[0] = 4");
		assertEquals(23, engine.speed.are);
		assertEquals(23, engine.speed.areLine);
		assertEquals(40, engine.speed.lineDelay);
		assertEquals(31, engine.speed.lockDelay);
		assertEquals(15, engine.speed.das);
	}

	@Test
	void normalBranchSectionFiveReadsFifthBucket() throws Exception {
		// internalLevel 500 / 100 = section 5: ARE=23, ARELine=23,
		// LineDelay=25, LockDelay=31, DAS=9.
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "internalLevel", 500);
		setInt(mode, "gravityindex", 0);
		engine.statistics.time = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(23, engine.speed.are);
		assertEquals(23, engine.speed.areLine);
		assertEquals(25, engine.speed.lineDelay);
		assertEquals(31, engine.speed.lockDelay);
		assertEquals(9, engine.speed.das);
	}

	@Test
	void normalBranchSectionTwelveLandsAtFinalDelaysBucket() throws Exception {
		// internalLevel 1200 -> section 12 (last): ARE=2, ARELine=2,
		// LineDelay=6, LockDelay=16, DAS=7.
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "internalLevel", 1200);
		setInt(mode, "gravityindex", 0);
		engine.statistics.time = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.are);
		assertEquals(2, engine.speed.areLine);
		assertEquals(6, engine.speed.lineDelay);
		assertEquals(16, engine.speed.lockDelay);
		assertEquals(7, engine.speed.das);
	}

	@Test
	void normalBranchPastTwelveHundredClampsDelaysToFinalEntry() throws Exception {
		// internalLevel past last threshold clamps section to 12.
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "internalLevel", 9999);
		setInt(mode, "gravityindex", 0);
		engine.statistics.time = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.are,
				"internalLevel past 1200 clamps to section 12 -> ARE=2");
		assertEquals(7, engine.speed.das);
	}

	@Test
	void timeBranchOverridesNormalDelaysEvenForLowLevel() throws Exception {
		// time >= 54000 forces fixed delays even when internalLevel=0
		// (a far slower section by table) — the time gate dominates.
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "internalLevel", 0);
		engine.statistics.time = 54000;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.are);
		assertEquals(13, engine.speed.lockDelay,
				"time gate overrides section 0's slow LockDelay 31");
	}

	private static GameEngine freshEngine(GradeMania3Mode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(GradeMania3Mode mode, GameEngine engine)
			throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod(
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

	private static void setBoolean(Object instance, String name, boolean value)
			throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(instance, value);
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
