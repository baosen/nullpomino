package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.OnOffMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link GradeMania2Mode}'s private {@code setSpeed} via
 * reflection. Mirrors GradeMania3 with two key differences: the
 * delay tables have 10 entries (not 13), and DAS reads from the
 * delay table even on the time-gate branch (the time-gate only
 * overrides ARE/ARELine/LineDelay/LockDelay).
 *
 * <p>Branches: always20g.value OR time >= 54000 pegs gravity at -1.
 * For delays, time >= 54000 forces fixed (3, 6, 6, 19) for ARE/
 * ARELine/LineDelay/LockDelay; otherwise section = level/100
 * (clamped to 9) reads from the 10-entry tables. DAS always comes
 * from tableDAS regardless of the time gate.
 */
class GradeMania2ModeSetSpeedTest {

	@Test
	void always20gMenuItemPegsGravityAtMinusOne() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, true);
		engine.statistics.level = 0;
		engine.statistics.time = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"always20g.value=true -> gravity = -1");
	}

	@Test
	void timePastFiftyFourThousandPegsGravityAndForcesFixedDelays() throws Exception {
		// time >= 54000 -> gravity=-1 + fixed (3, 6, 6, 19) delays.
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, false);
		engine.statistics.level = 100;
		engine.statistics.time = 54000;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity);
		assertEquals(3, engine.speed.are);
		assertEquals(6, engine.speed.areLine);
		assertEquals(6, engine.speed.lineDelay);
		assertEquals(19, engine.speed.lockDelay);
	}

	@Test
	void timeBranchStillReadsDasFromTheNormalTable() throws Exception {
		// DAS always reads from tableDAS regardless of the time gate.
		// section 1 (level 100) -> tableDAS[1] = 15.
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, false);
		engine.statistics.level = 100;
		engine.statistics.time = 54000;

		invokeSetSpeed(mode, engine);

		assertEquals(15, engine.speed.das,
				"DAS reads from tableDAS even on the time-gate branch");
	}

	@Test
	void normalBranchAtLevelZeroReadsFirstTableEntries() throws Exception {
		// section 0: ARE=23, ARELine=23, LineDelay=40, LockDelay=31, DAS=15.
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 0;
		engine.statistics.time = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(4, engine.speed.gravity,
				"level 0 -> gravityindex 0 -> gravity 4");
		assertEquals(23, engine.speed.are);
		assertEquals(23, engine.speed.areLine);
		assertEquals(40, engine.speed.lineDelay);
		assertEquals(31, engine.speed.lockDelay);
		assertEquals(15, engine.speed.das);
	}

	@Test
	void normalBranchAtLevelNineHundredLandsAtFinalDelaysBucket() throws Exception {
		// section 9 (last): ARE=10, ARELine=4, LineDelay=6, LockDelay=18,
		// DAS=7.
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 900;
		engine.statistics.time = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(10, engine.speed.are);
		assertEquals(4, engine.speed.areLine);
		assertEquals(6, engine.speed.lineDelay);
		assertEquals(18, engine.speed.lockDelay);
		assertEquals(7, engine.speed.das);
	}

	@Test
	void normalBranchPastNineHundredClampsDelaysToFinalEntry() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine engine = freshEngine(mode);
		setAlways20gValue(mode, false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 9999;
		engine.statistics.time = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(10, engine.speed.are,
				"level past 900 clamps to section 9 -> ARE=10");
		assertEquals(7, engine.speed.das);
	}

	private static GameEngine freshEngine(GradeMania2Mode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void setAlways20gValue(GradeMania2Mode mode, boolean value)
			throws Exception {
		Field f = findField(mode.getClass(), "always20g");
		f.setAccessible(true);
		OnOffMenuItem item = (OnOffMenuItem) f.get(mode);
		item.value = value;
	}

	private static void invokeSetSpeed(GradeMania2Mode mode, GameEngine engine)
			throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod(
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
