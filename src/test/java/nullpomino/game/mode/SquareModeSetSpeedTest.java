package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link SquareMode#setSpeed}'s gravity-table lookup. The
 * standard gametype (0) clamps the score to [0, 5000], advances
 * gravityindex past every entry of tableGravityChangeScore the
 * clamped speedlv reaches, then reads gravity from tableGravityValue.
 * The sprint gametype (!=0) pegs gravity at 1. denominator stays at
 * 60 in both cases.
 */
class SquareModeSetSpeedTest {

	@Test
	void standardGametypeWithZeroScoreUsesFirstTableEntry() throws Exception {
		// score=0 < tableGravityChangeScore[0]=150 -> gravityindex stays 0
		// -> gravity = tableGravityValue[0] = 1; denominator = 60.
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		setInt(mode, "gravityindex", 0);
		engine.statistics.score = 0;

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity,
				"score=0 -> gravityindex 0 -> gravity 1");
		assertEquals(60, engine.speed.denominator,
				"denominator is constant at 60");
	}

	@Test
	void scoreJustBelowMaxThresholdLandsAtFinalGravityValue() throws Exception {
		// score=4999 advances gravityindex past every threshold below
		// 5000 — gravityindex=12 -> gravity = tableGravityValue[12] = 300.
		// (score >= 5000 walks the loop off the end of the table — that
		// path is intentionally not exercised here.)
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		setInt(mode, "gravityindex", 0);
		engine.statistics.score = 4999;

		mode.setSpeed(engine);

		assertEquals(300, engine.speed.gravity,
				"score=4999 hits 12 thresholds -> gravityindex 12 -> gravity 300");
	}

	@Test
	void negativeScoreClampsAtZeroAndKeepsFirstTableEntry() throws Exception {
		// speedlv < 0 -> clamp to 0 -> first-entry gravity (1).
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		setInt(mode, "gravityindex", 0);
		engine.statistics.score = -100;

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity,
				"negative score clamps at 0 -> gravity stays at table[0]=1");
	}

	@Test
	void scoreAtFirstThresholdAdvancesGravityIndexByOne() throws Exception {
		// score=150 advances gravityindex past entry 0 -> gravityindex=1
		// -> gravity = tableGravityValue[1] = 2.
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		setInt(mode, "gravityindex", 0);
		engine.statistics.score = 150;

		mode.setSpeed(engine);

		assertEquals(2, engine.speed.gravity,
				"score=150 advances to gravityindex 1 -> gravity 2");
	}

	@Test
	void scoreAtMidThresholdLandsAtMatchingTableEntry() throws Exception {
		// score=1000 advances past 150, 300, 400, 500, 600, 700, 800,
		// 900, 1000 -> 9 thresholds -> gravityindex=9 -> gravity = 60.
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		setInt(mode, "gravityindex", 0);
		engine.statistics.score = 1000;

		mode.setSpeed(engine);

		assertEquals(60, engine.speed.gravity,
				"score=1000 hits 9 thresholds -> gravityindex 9 -> gravity 60");
	}

	@Test
	void gravityIndexIsMonotonicAcrossSetSpeedCalls() throws Exception {
		// gravityindex persists across calls — once advanced, dropping
		// the score back doesn't rewind it.
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 0);
		setInt(mode, "gravityindex", 0);

		engine.statistics.score = 1000;
		mode.setSpeed(engine);
		// gravityindex now 9.

		engine.statistics.score = 50;
		mode.setSpeed(engine);
		assertEquals(60, engine.speed.gravity,
				"gravityindex doesn't rewind even when score drops back");
	}

	@Test
	void sprintGametypeFixesGravityAtOneRegardlessOfScore() throws Exception {
		// gametype != 0 -> gravity = 1, denominator = 60.
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "gametype", 1);
		engine.statistics.score = 9999;

		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity,
				"gametype 1 ignores score and pegs gravity at 1");
		assertEquals(60, engine.speed.denominator);
	}


	private static GameEngine freshEngine(SquareMode mode) {
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
