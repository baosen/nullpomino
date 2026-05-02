package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheMode#setSpeed}'s gravity / denominator math.
 *
 * <p>setSpeed always pegs gravity at 1, then picks the denominator
 * based on gametype:
 * <ul>
 *   <li>gametype == 0 (Endless): denominator = max(41 - level, 2),
 *       so blocks fall faster as level climbs but the floor at 2
 *       prevents an infinitely-fast fall once level reaches 39.</li>
 *   <li>gametype != 0: denominator is fixed at 40 — Sprint and
 *       similar fixed-pace modes use a constant fall speed.</li>
 * </ul>
 */
class AvalancheModeSetSpeedTest {

	@Test
	void setSpeedAlwaysPegsGravityAtOne() {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.speed.gravity = 99999;

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity,
				"AvalancheMode.setSpeed always pegs gravity at 1");
	}

	@Test
	void setSpeedGametypeZeroLevelZeroSetsDenominatorAtForty() throws Exception {
		// gametype=0, level=0 -> max(41-0, 2) = 41.
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		// playerInit ran via freshEngine and may have loaded settings
		// from modeConfig — re-set after.
		setInt(mode, "gametype", 0);
		setInt(mode, "level", 0);

		mode.setSpeed(engine);

		assertEquals(41, engine.speed.denominator,
				"gametype=0 level=0 -> denominator = 41 (slowest endless rate)");
	}

	@Test
	void setSpeedGametypeZeroAtMaxLevelClampsDenominatorAtTwo() throws Exception {
		// max(41-level, 2): once level >= 39 the floor at 2 kicks in
		// and the denominator stays at 2 regardless of higher levels.
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		setInt(mode, "level", 39);

		mode.setSpeed(engine);
		assertEquals(2, engine.speed.denominator,
				"level 39 -> denominator clamps at 2");

		setInt(mode, "level", 99);
		mode.setSpeed(engine);
		assertEquals(2, engine.speed.denominator,
				"level past 39 stays clamped at 2");
	}

	@Test
	void setSpeedGametypeZeroLinearFallBetweenZeroAndThirtyNine() throws Exception {
		// In the linear range, denominator = 41 - level.
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);

		setInt(mode, "level", 1);
		mode.setSpeed(engine);
		assertEquals(40, engine.speed.denominator);

		setInt(mode, "level", 10);
		mode.setSpeed(engine);
		assertEquals(31, engine.speed.denominator);

		setInt(mode, "level", 38);
		mode.setSpeed(engine);
		assertEquals(3, engine.speed.denominator);
	}

	@Test
	void setSpeedNonEndlessGametypeFixesDenominatorAtForty() throws Exception {
		// gametype != 0 -> denominator constant at 40 regardless of level.
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 1);

		setInt(mode, "level", 0);
		mode.setSpeed(engine);
		assertEquals(40, engine.speed.denominator);

		setInt(mode, "level", 99);
		mode.setSpeed(engine);
		assertEquals(40, engine.speed.denominator,
				"non-endless gametype ignores level for denominator");
	}

	private static GameEngine freshEngine(AvalancheMode mode) {
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
