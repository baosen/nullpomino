package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link ScoreAttackMode}'s private {@code setSpeed} via
 * reflection. always20g pegs gravity at -1; otherwise the level walks
 * tableGravityChangeLevel to advance gravityindex past every threshold
 * and reads gravity from tableGravityValue. The 32-entry tables ramp
 * from gravity 4 at level 8 up to a -1 sentinel at level 300+.
 */
class ScoreAttackModeSetSpeedTest {

	@Test
	void always20gPegsGravityAtMinusOneIgnoringLevel() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", true);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"always20g -> gravity = -1 instant fall");
	}

	@Test
	void levelZeroLeavesGravityIndexAtZero() throws Exception {
		// level 0 < tableGravityChangeLevel[0] = 8 -> gravityindex stays 0
		// -> gravity = tableGravityValue[0] = 4.
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(4, engine.speed.gravity);
	}

	@Test
	void levelEightAdvancesPastFirstThreshold() throws Exception {
		// level 8 == threshold[0] -> gravityindex 1 -> gravity 5.
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 8;

		invokeSetSpeed(mode, engine);

		assertEquals(5, engine.speed.gravity);
	}

	@Test
	void preShiftReplaysStillSlowDownAtLevel100And200() throws Exception {
		// Replays recorded before SLOWDOWN_SHIFT_VERSION (version 0 = pre-item,
		// version 1 = the released item build) keep the original 100/200 timing so
		// their recorded inputs stay in sync. gravityChangeLevel maps the table's
		// 101/201 entries back to 100/200 -> level 100 advances to gravityindex 10
		// (tableGravityValue[10] = 4) and level 200 to gravityindex 21 (=16).
		for(int version : new int[] {0, 1}) {
			ScoreAttackMode mode = new ScoreAttackMode();
			GameEngine engine = freshEngine(mode);
			setBoolean(mode, "always20g", false);
			setInt(mode, "version", version);
			setInt(mode, "gravityindex", 0);

			engine.statistics.level = 100;
			invokeSetSpeed(mode, engine);
			assertEquals(4, engine.speed.gravity,
					"version " + version + " -> slow-down still at level 100");
			engine.statistics.level = 200;
			invokeSetSpeed(mode, engine);
			assertEquals(16, engine.speed.gravity,
					"version " + version + " -> slow-down still at level 200");
		}
	}

	@Test
	void currentVersionSlowsDownAtLevel101And201NotTheHundreds() throws Exception {
		// As of the item version the section slow-down (gravity reset) is deferred
		// one level so it coincides with the scripted item entering the field.
		// tableGravityValue: [9]=64, [10]=4 (section 1), [20]=144, [21]=16 (section 2).
		// playerInit on a non-replay engine sets version = CURRENT_VERSION.
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);

		engine.statistics.level = 100;
		invokeSetSpeed(mode, engine);
		assertEquals(64, engine.speed.gravity, "no slow-down yet at level 100");
		engine.statistics.level = 101;
		invokeSetSpeed(mode, engine);
		assertEquals(4, engine.speed.gravity, "slow-down at level 101");
		engine.statistics.level = 200;
		invokeSetSpeed(mode, engine);
		assertEquals(144, engine.speed.gravity, "no slow-down yet at level 200");
		engine.statistics.level = 201;
		invokeSetSpeed(mode, engine);
		assertEquals(16, engine.speed.gravity, "slow-down at level 201");
	}

	@Test
	void gravityIndexIsMonotonic() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);

		engine.statistics.level = 101;
		invokeSetSpeed(mode, engine);
		// gravityindex now 10 (section-1 slow-down at level 101).

		engine.statistics.level = 0;
		invokeSetSpeed(mode, engine);
		assertEquals(4, engine.speed.gravity,
				"gravityindex doesn't rewind -> gravity stays at table[10]=4");
	}

	@Test
	void levelThreeHundredAdvancesToFinalSentinelEntry() throws Exception {
		// Level 300 advances past every threshold up to 300 ->
		// gravityindex 31 -> gravity = -1.
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "always20g", false);
		setInt(mode, "gravityindex", 0);
		engine.statistics.level = 300;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"level 300 advances to final gravityindex -> instant fall");
	}

	private static GameEngine freshEngine(ScoreAttackMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(ScoreAttackMode mode, GameEngine engine)
			throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod(
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
