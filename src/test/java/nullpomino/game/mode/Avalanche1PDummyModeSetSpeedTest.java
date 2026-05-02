package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link Avalanche1PDummyMode#setSpeed}'s gravity / denominator
 * math, exercised through {@link AvalancheFeverMode} (the concrete
 * subclass that doesn't override setSpeed).
 *
 * <p>The level &lt;= 40 branch uses {@code gravity = 1} and
 * {@code denominator = max(43 - level - ((level % 10) * 2), 2)}.
 * The level &gt; 40 branch fixes denominator at 60 and steps through
 * {@code tableSpeedValue} using {@code speedIndex} (incremented past
 * each entry of {@code tableSpeedChangeLevel}). The default tables
 * are {80, 90, 96, 97, Integer.MAX_VALUE} for change-levels and
 * {30, 45, 120, 480, -1} for values.
 */
class Avalanche1PDummyModeSetSpeedTest {

	@Test
	void levelZeroSetsGravityToOneAndDenominatorToFortyThree() throws Exception {
		// level=0 -> max(43 - 0 - 0, 2) = 43.
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "level", 0);
		setInt(mode, "speedIndex", 0);

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity, "level <= 40 -> gravity = 1");
		assertEquals(43, engine.speed.denominator,
				"level=0 -> denominator = 43 (slowest start)");
	}

	@Test
	void modulusPenaltyAcceleratesFallWithinADecade() throws Exception {
		// The penalty is (level % 10) * 2. Within a decade, denominator
		// shrinks faster than 1 per level. Pin the per-level slope:
		// level=11 -> 43-11-2 = 30; level=19 -> 43-19-18 = 6.
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "level", 11);
		mode.setSpeed(engine);
		assertEquals(30, engine.speed.denominator,
				"level=11 -> 43-11-2 = 30");

		setInt(mode, "level", 19);
		mode.setSpeed(engine);
		assertEquals(6, engine.speed.denominator,
				"level=19 -> 43-19-18 = 6");
	}

	@Test
	void modulusPenaltyResetsAtEachDecadeBoundary() throws Exception {
		// At a multiple of 10, level % 10 == 0 so the penalty resets.
		// level=10 -> 43-10-0 = 33; level=20 -> 43-20-0 = 23.
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "level", 10);
		mode.setSpeed(engine);
		assertEquals(33, engine.speed.denominator,
				"decade boundary -> modulus penalty drops to 0");

		setInt(mode, "level", 20);
		mode.setSpeed(engine);
		assertEquals(23, engine.speed.denominator);
	}

	@Test
	void modulusPenaltyClampsDenominatorAtFloorOfTwoNearDecadeEnd() throws Exception {
		// level=29 -> 43-29-18 = -4 -> max(-4, 2) = 2.
		// The clamp also kicks in at level=39 -> 43-39-18 = -14 -> 2.
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "level", 29);
		mode.setSpeed(engine);
		assertEquals(2, engine.speed.denominator,
				"level=29 -> floor at 2 (max(-4, 2))");

		setInt(mode, "level", 39);
		mode.setSpeed(engine);
		assertEquals(2, engine.speed.denominator,
				"level=39 -> floor at 2 (max(-14, 2))");
	}

	@Test
	void levelFortyOneTransitionsToTableLookupBranch() throws Exception {
		// level > 40 -> denominator = 60, gravity steps through
		// tableSpeedValue. With speedIndex=0 and level=41 < 80
		// (tableSpeedChangeLevel[0]), the while loop doesn't advance
		// speedIndex. So gravity = tableSpeedValue[0] = 30.
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "level", 41);
		setInt(mode, "speedIndex", 0);

		mode.setSpeed(engine);

		assertEquals(60, engine.speed.denominator,
				"level > 40 fixes denominator at 60");
		assertEquals(30, engine.speed.gravity,
				"speedIndex stays at 0 -> gravity = tableSpeedValue[0]");
	}

	@Test
	void speedIndexAdvancesPastEachTableSpeedChangeLevelEntry() throws Exception {
		// At level=80, while loop advances speedIndex past entry 0
		// (changeLevel=80) -> speedIndex=1 -> gravity = 45.
		// At level=90, advances to 2 -> gravity = 120.
		// At level=96, advances to 3 -> gravity = 480.
		// At level=97, advances to 4 -> gravity = -1 (instant fall).
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "speedIndex", 0);
		setInt(mode, "level", 80);
		mode.setSpeed(engine);
		assertEquals(45, engine.speed.gravity,
				"level 80 advances speedIndex to 1 -> gravity 45");

		setInt(mode, "speedIndex", 0);
		setInt(mode, "level", 90);
		mode.setSpeed(engine);
		assertEquals(120, engine.speed.gravity,
				"level 90 advances speedIndex to 2 -> gravity 120");

		setInt(mode, "speedIndex", 0);
		setInt(mode, "level", 96);
		mode.setSpeed(engine);
		assertEquals(480, engine.speed.gravity,
				"level 96 advances speedIndex to 3 -> gravity 480");

		setInt(mode, "speedIndex", 0);
		setInt(mode, "level", 97);
		mode.setSpeed(engine);
		assertEquals(-1, engine.speed.gravity,
				"level 97 advances speedIndex to 4 -> gravity -1 (instant)");
	}

	@Test
	void speedIndexIsMonotonicAcrossSetSpeedCalls() throws Exception {
		// speedIndex is a member field — it persists between setSpeed
		// calls and only ever advances. Pin that contract so that a
		// subclass can rely on no-rewind during a level run.
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "speedIndex", 0);
		setInt(mode, "level", 80);
		mode.setSpeed(engine);
		// speedIndex now 1.

		// Drop level back below 80 — speedIndex stays at 1, the while
		// loop doesn't fire, and gravity stays at tableSpeedValue[1]=45.
		setInt(mode, "level", 70);
		mode.setSpeed(engine);
		assertEquals(45, engine.speed.gravity,
				"speedIndex doesn't rewind even when level drops back");
	}

	private static GameEngine freshEngine(AvalancheFeverMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void setInt(Object instance, String name, int value) throws Exception {
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
