package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link PhysicianMode#setSpeed}: gravity scales with the
 * BASE_SPEEDS table indexed by the speed setting and ramps with the
 * total pieces locked (every 10 pieces bumps the multiplier by one),
 * while denominator stays fixed at 3600. The base speeds are
 * {10, 20, 25} for the LOW / MED / HI presets.
 */
class PhysicianModeSetSpeedTest {

	@Test
	void setSpeedLowSpeedZeroPiecesProducesGravityOneHundred() throws Exception {
		// speed=0 (LOW), totalPieceLocked=0 -> gravity = 10 * (10+0) = 100.
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "speed", 0);
		engine.statistics.totalPieceLocked = 0;

		mode.setSpeed(engine);

		assertEquals(100, engine.speed.gravity,
				"speed=LOW (0), 0 pieces -> gravity = BASE_SPEEDS[0]*10 = 100");
		assertEquals(3600, engine.speed.denominator,
				"denominator is fixed at 3600 in PHYSICIAN mode");
	}

	@Test
	void setSpeedMediumSpeedOnePresetMultiplierIsTwenty() throws Exception {
		// speed=1 (MED) -> BASE_SPEEDS[1] = 20.
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "speed", 1);
		engine.statistics.totalPieceLocked = 0;

		mode.setSpeed(engine);

		assertEquals(200, engine.speed.gravity,
				"speed=MED (1), 0 pieces -> gravity = 20*10 = 200");
	}

	@Test
	void setSpeedHighSpeedTwoPresetMultiplierIsTwentyFive() throws Exception {
		// speed=2 (HI) -> BASE_SPEEDS[2] = 25.
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "speed", 2);
		engine.statistics.totalPieceLocked = 0;

		mode.setSpeed(engine);

		assertEquals(250, engine.speed.gravity,
				"speed=HI (2), 0 pieces -> gravity = 25*10 = 250");
	}

	@Test
	void totalPieceLockedRampsTheMultiplierEveryTenPieces() throws Exception {
		// gravity = base * (10 + tpl/10). tpl is integer-divided by 10
		// so the multiplier increments at every 10th piece.
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "speed", 0); // base = 10

		engine.statistics.totalPieceLocked = 9;
		mode.setSpeed(engine);
		assertEquals(100, engine.speed.gravity,
				"tpl=9 -> 9/10=0 -> still multiplier 10 -> gravity 100");

		engine.statistics.totalPieceLocked = 10;
		mode.setSpeed(engine);
		assertEquals(110, engine.speed.gravity,
				"tpl=10 -> 10/10=1 -> multiplier 11 -> gravity 110");

		engine.statistics.totalPieceLocked = 19;
		mode.setSpeed(engine);
		assertEquals(110, engine.speed.gravity,
				"tpl=19 -> 19/10=1 -> still multiplier 11");

		engine.statistics.totalPieceLocked = 100;
		mode.setSpeed(engine);
		assertEquals(200, engine.speed.gravity,
				"tpl=100 -> 100/10=10 -> multiplier 20 -> gravity 200");
	}

	@Test
	void rampScalesWithThePresetSoHiSpeedRampsFasterThanLow() throws Exception {
		// At tpl=100, multiplier is 20. So:
		//   speed=0 (base 10) -> gravity = 10*20 = 200
		//   speed=1 (base 20) -> gravity = 20*20 = 400
		//   speed=2 (base 25) -> gravity = 25*20 = 500
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.totalPieceLocked = 100;

		setInt(mode, "speed", 0);
		mode.setSpeed(engine);
		assertEquals(200, engine.speed.gravity);

		setInt(mode, "speed", 1);
		mode.setSpeed(engine);
		assertEquals(400, engine.speed.gravity);

		setInt(mode, "speed", 2);
		mode.setSpeed(engine);
		assertEquals(500, engine.speed.gravity);
	}

	@Test
	void denominatorStaysAtThirtySixHundredAcrossEveryConfiguration() throws Exception {
		// Pin that denominator is independent of speed and tpl.
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		engine.speed.denominator = 1; // intentionally spoiled

		setInt(mode, "speed", 2);
		engine.statistics.totalPieceLocked = 999;
		mode.setSpeed(engine);

		assertEquals(3600, engine.speed.denominator,
				"denominator is constant at 3600 regardless of speed/tpl");
	}

	private static GameEngine freshEngine(PhysicianMode mode) {
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
