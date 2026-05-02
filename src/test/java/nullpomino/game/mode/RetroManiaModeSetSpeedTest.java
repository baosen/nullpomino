package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link RetroManiaMode}'s private {@code setSpeed} via
 * reflection. Gravity is fixed at 1; denominator comes from a
 * 4x16 table indexed by [gametype][level], with level clamped to
 * [0, 15] before lookup.
 *
 * <p>The four difficulty rows (EASY / NORMAL / HARD / HARDEST) are
 * progressively faster: row 0 starts at denominator 48 with a
 * shallow ramp, row 3 starts at 30 and reaches 1 by level 8.
 */
class RetroManiaModeSetSpeedTest {

	@Test
	void setSpeedAlwaysPegsGravityAtOne() throws Exception {
		// gravity=1 regardless of gametype/level.
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		engine.statistics.level = 0;
		engine.speed.gravity = 99999;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity,
				"RetroMania always pegs gravity at 1");
	}

	@Test
	void easyGametypeAtLevelZeroLandsAtForestEightDenominator() throws Exception {
		// row 0 (EASY), level 0 -> denominator 48.
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(48, engine.speed.denominator);
	}

	@Test
	void hardestGametypeAtLevelZeroStartsAtThirtyDenominator() throws Exception {
		// row 3 (HARDEST), level 0 -> denominator 30.
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 3);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(30, engine.speed.denominator,
				"HARDEST starts faster than EASY (30 vs 48 denominator)");
	}

	@Test
	void hardestGametypeAtLevelEightLandsAtDenominatorOne() throws Exception {
		// row 3, level 8 -> denominator 1 (full speed).
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 3);
		engine.statistics.level = 8;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.denominator,
				"HARDEST reaches denominator 1 by level 8");
	}

	@Test
	void easyGametypeAtLevelFifteenLandsAtDenominatorTwo() throws Exception {
		// row 0, level 15 (last in-range) -> denominator 2.
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		engine.statistics.level = 15;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.denominator,
				"EASY at level 15 -> denominator 2");
	}

	@Test
	void levelPastFifteenClampsToFinalEntry() throws Exception {
		// Level 99 in EASY clamps to 15 -> denominator 2.
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		engine.statistics.level = 99;

		invokeSetSpeed(mode, engine);

		assertEquals(2, engine.speed.denominator);
	}

	@Test
	void negativeLevelClampsToZero() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		engine.statistics.level = -10;

		invokeSetSpeed(mode, engine);

		assertEquals(48, engine.speed.denominator,
				"negative level clamps to 0 -> first table entry");
	}

	@Test
	void rampDifferencesAcrossDifficultyAtLevelFour() throws Exception {
		// Same level, four difficulties: 14, 12, 10, 8.
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 4;

		setInt(mode, "gametype", 0);
		invokeSetSpeed(mode, engine);
		assertEquals(14, engine.speed.denominator);

		setInt(mode, "gametype", 1);
		invokeSetSpeed(mode, engine);
		assertEquals(12, engine.speed.denominator);

		setInt(mode, "gametype", 2);
		invokeSetSpeed(mode, engine);
		assertEquals(10, engine.speed.denominator);

		setInt(mode, "gametype", 3);
		invokeSetSpeed(mode, engine);
		assertEquals(8, engine.speed.denominator);
	}

	private static GameEngine freshEngine(RetroManiaMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void invokeSetSpeed(RetroManiaMode mode, GameEngine engine)
			throws Exception {
		Method m = RetroManiaMode.class.getDeclaredMethod(
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
