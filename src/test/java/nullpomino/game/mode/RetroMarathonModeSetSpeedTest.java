package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link RetroMarathonMode}'s private {@code setSpeed} via
 * reflection. Two branches: GAMETYPE_ARRANGE reads gravity AND
 * denominator from level-indexed tables, including a -1 instant-fall
 * sentinel at level 39; other game types peg gravity at 1 and read
 * only denominator from a 30-entry table.
 *
 * <p>Both branches clamp the level to [0, len-1] before lookup. The
 * tables ramp the fall rate in a quintessentially Retro Marathon
 * shape — denominator drops by 5 per level early on, then more
 * gradually.
 */
class RetroMarathonModeSetSpeedTest {

	@Test
	void typeAGametypeWithLevelZeroSetsGravityOneAndDenominatorFortyEight() throws Exception {
		// gametype=A (0): gravity=1, denominator=tableDenominator[0]=48.
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity,
				"non-arrange gametype pegs gravity at 1");
		assertEquals(48, engine.speed.denominator,
				"level 0 -> tableDenominator[0] = 48 (slowest start)");
	}

	@Test
	void typeAGametypeWithLevelTwentyNineSetsDenominatorOne() throws Exception {
		// Level 29 (last in-range) -> tableDenominator[29] = 1.
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		engine.statistics.level = 29;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(1, engine.speed.denominator,
				"level 29 -> denominator 1 (fastest in non-arrange)");
	}

	@Test
	void typeAGametypeClampsLevelToTableLengthMinusOne() throws Exception {
		// Level past 29 clamps to 29.
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		engine.statistics.level = 99;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.denominator,
				"level past last entry clamps to denominator 1");
	}

	@Test
	void typeAGametypeWithNegativeLevelClampsToZero() throws Exception {
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		engine.statistics.level = -5;

		invokeSetSpeed(mode, engine);

		assertEquals(48, engine.speed.denominator,
				"negative level clamps to 0 -> first table entry");
	}

	@Test
	void arrangeGametypeReadsGravityFromArrangeTable() throws Exception {
		// gametype=ARRANGE (2): gravity from tableGravityArrange,
		// denominator from tableDenominatorArrange.
		// Level 0 -> gravity=1, denominator=48.
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 2);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(48, engine.speed.denominator);
	}

	@Test
	void arrangeGametypeAtLevelTwentySixGravityRampsToFive() throws Exception {
		// tableGravityArrange[26] = 5, tableDenominatorArrange[26] = 4.
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 2);
		engine.statistics.level = 26;

		invokeSetSpeed(mode, engine);

		assertEquals(5, engine.speed.gravity,
				"level 26 in arrange -> gravity 5");
		assertEquals(4, engine.speed.denominator,
				"level 26 in arrange -> denominator 4");
	}

	@Test
	void arrangeGametypeAtLevelThirtyNineHitsInstantFallSentinel() throws Exception {
		// tableGravityArrange[39] = -1, tableDenominatorArrange[39] = 1.
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 2);
		engine.statistics.level = 39;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity,
				"level 39 in arrange hits the instant-fall sentinel");
		assertEquals(1, engine.speed.denominator);
	}

	@Test
	void arrangeGametypePastLastEntryClampsToFinalSentinel() throws Exception {
		// Level 999 in arrange clamps to 39 -> -1 / 1.
		RetroMarathonMode mode = new RetroMarathonMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 2);
		engine.statistics.level = 999;

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity);
		assertEquals(1, engine.speed.denominator);
	}

	private static GameEngine freshEngine(RetroMarathonMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(RetroMarathonMode mode, GameEngine engine)
			throws Exception {
		Method m = RetroMarathonMode.class.getDeclaredMethod(
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
