package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link ExtremeMode#setSpeed}'s level-indexed table lookups.
 * EXTREME pegs gravity at -1 (instant fall) and reads ARE / ARELine /
 * LineDelay / LockDelay / DAS from five 20-entry tables, clamping the
 * level into [0, 19] before lookup.
 */
class ExtremeModeSetSpeedTest {

	@Test
	void levelZeroLandsAtFirstTableEntryAndPegsGravityAtMinusOne() {
		// tableARE[0]=25, tableARELine[0]=25, tableLineDelay[0]=40,
		// tableLockDelay[0]=30, tableDAS[0]=16. Gravity is always -1.
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		mode.setSpeed(engine);

		assertEquals(-1, engine.speed.gravity,
				"EXTREME always pegs gravity at -1 (instant fall)");
		assertEquals(25, engine.speed.are);
		assertEquals(25, engine.speed.areLine);
		assertEquals(40, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(16, engine.speed.das);
	}

	@Test
	void midLevelSevenLandsAtMatchingTableEntry() {
		// Level 7 -> ARE=6, ARELine=2, LineDelay=2, LockDelay=16, DAS=6.
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 7;

		mode.setSpeed(engine);

		assertEquals(6, engine.speed.are);
		assertEquals(2, engine.speed.areLine);
		assertEquals(2, engine.speed.lineDelay);
		assertEquals(16, engine.speed.lockDelay);
		assertEquals(6, engine.speed.das);
	}

	@Test
	void levelNineteenLandsAtFinalTableEntry() {
		// Level 19 (last in-range) -> ARE=0, ARELine=0, LineDelay=0,
		// LockDelay=11, DAS=3.
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 19;

		mode.setSpeed(engine);

		assertEquals(0, engine.speed.are);
		assertEquals(0, engine.speed.areLine);
		assertEquals(0, engine.speed.lineDelay);
		assertEquals(11, engine.speed.lockDelay);
		assertEquals(3, engine.speed.das);
	}

	@Test
	void negativeLevelClampsToZero() {
		// Level < 0 reads as level 0.
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = -1;

		mode.setSpeed(engine);

		assertEquals(25, engine.speed.are,
				"negative level clamps to 0 -> first table entry");
	}

	@Test
	void levelPastLastEntryClampsToFinalEntry() {
		// Level >= 20 clamps to 19 -> final entry values.
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 50;

		mode.setSpeed(engine);

		assertEquals(0, engine.speed.are);
		assertEquals(11, engine.speed.lockDelay);
		assertEquals(3, engine.speed.das,
				"level 50 clamps to 19 -> tableDAS[19] = 3");
	}

	@Test
	void gravityRemainsMinusOneAtEveryLevel() {
		// gravity is set on every call regardless of level.
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		engine.speed.gravity = 99999; // intentionally spoiled

		for(int lv = 0; lv < 20; lv += 5) {
			engine.statistics.level = lv;
			engine.speed.gravity = 99999;
			mode.setSpeed(engine);
			assertEquals(-1, engine.speed.gravity,
					"gravity must be -1 at level " + lv);
		}
	}

	private static GameEngine freshEngine(ExtremeMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}
}
