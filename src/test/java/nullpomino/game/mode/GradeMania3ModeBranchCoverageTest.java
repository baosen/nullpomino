// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Targets specific uncovered branch arms in {@link GradeMania3Mode} that the
 * other GM3 suites miss because they only ever drive the opposite outcome:
 * <ul>
 *   <li>startGame L771: negative start level forces nextseclv = 100.</li>
 *   <li>levelUp L1102: alwaysghost = true keeps the ghost ON at level &ge; 100.</li>
 *   <li>levelUp L1106/L1108: COOL adds +100 to the BGM-fadeout level test.</li>
 *   <li>onLast L1416/1417/1418: roll meter stays GREEN early in the roll (the
 *       remainRollTime-above-threshold arms, opposite of the roll-end tests).</li>
 *   <li>onLast L1437: the roll-end grade clamp {@code grade &gt; 32 -&gt; 32}.</li>
 *   <li>onMove L1019: level already at nextseclv-1 skips the per-piece bump.</li>
 * </ul>
 */
class GradeMania3ModeBranchCoverageTest {

	private static final int ROLLTIMELIMIT = 3238;

	private static GameEngine freshEngine(GradeMania3Mode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	// -----------------------------------------------------------------
	// startGame L771: engine.statistics.level < 0 -> nextseclv = 100
	// -----------------------------------------------------------------

	@Test
	void startGameNegativeStartLevelSetsNextSecLvTo100() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel * 100 = -100 -> Math.min(999,-100) = -100 < 0 -> nextseclv = 100.
		setInt(mode, "startlevel", -1);
		mode.startGame(engine, 0);

		assertEquals(-100, engine.statistics.level);
		assertEquals(100, readInt(mode, "nextseclv"),
				"negative start level should reset nextseclv to 100");
	}

	// -----------------------------------------------------------------
	// levelUp L1102: alwaysghost true keeps ghost ON at level >= 100
	// -----------------------------------------------------------------

	@Test
	void levelUpKeepsGhostWhenAlwaysGhost() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ghost = true;
		engine.statistics.level = 150;
		setBool(mode, "alwaysghost", true);
		setInt(mode, "bgmlv", 0);
		setInt(mode, "internalLevel", 150);

		invokeLevelUp(mode, engine);

		assertTrue(engine.ghost,
				"alwaysghost should keep the ghost ON even past level 100");
	}

	@Test
	void levelUpDisablesGhostWhenLevelHighAndNotAlwaysGhost() throws Exception {
		// Companion to the above so both arms of (level>=100 && !alwaysghost) are clear.
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ghost = true;
		engine.statistics.level = 150;
		setBool(mode, "alwaysghost", false);
		setInt(mode, "bgmlv", 0);
		setInt(mode, "internalLevel", 150);

		invokeLevelUp(mode, engine);

		assertEquals(false, engine.ghost,
				"without alwaysghost the ghost should turn OFF past level 100");
	}

	// -----------------------------------------------------------------
	// levelUp L1106/L1108: COOL bumps the fadeout-test level by +100
	// -----------------------------------------------------------------

	@Test
	void levelUpCoolTriggersBgmFadeout() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// bgmlv 0 -> tableBGMFadeout[0] = 485. internalLevel 400 alone is below it,
		// but cool adds +100 -> tempLevel 500 >= 485, so fadeout switches on.
		setInt(mode, "bgmlv", 0);
		setInt(mode, "internalLevel", 400);
		setBool(mode, "cool", true);
		engine.statistics.level = 80;
		engine.owner.bgmStatus.fadesw = false;

		invokeLevelUp(mode, engine);

		assertTrue(engine.owner.bgmStatus.fadesw,
				"COOL +100 should push internalLevel over the BGM fadeout threshold");
	}

	// -----------------------------------------------------------------
	// onLast L1416/1417/1418: roll meter colour stays GREEN early in roll
	// -----------------------------------------------------------------

	@Test
	void onLastRollMeterGreenWhenPlentyOfTimeLeft() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 0); // remainRollTime = ROLLTIMELIMIT, far above every threshold

		mode.onLast(engine, 0);

		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor,
				"early in the roll the meter should remain GREEN");
		assertEquals(1, readInt(mode, "rolltime"));
	}

	// -----------------------------------------------------------------
	// onLast L1437: roll-end grade clamp grade > 32 -> 32
	// -----------------------------------------------------------------

	@Test
	void onLastRollEndClampsGradeToThirtyTwo() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", ROLLTIMELIMIT - 1); // becomes ROLLTIMELIMIT this frame
		setBool(mode, "mrollFlag", true);
		// rollPoints 5.0 + mroll bonus 1.6 = 6.6 -> while loop bumps grade well past 32.
		setFloat(mode, "rollPoints", 5.0f);
		setInt(mode, "grade", 30);

		mode.onLast(engine, 0);

		assertEquals(32, readInt(mode, "grade"),
				"roll-end grade gain should clamp at 32 (GM)");
	}

	// -----------------------------------------------------------------
	// onMove L1019: level already at nextseclv-1 -> no per-piece level bump
	// -----------------------------------------------------------------

	@Test
	void onMoveAtSectionEdgeDoesNotBumpLevel() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBool(mode, "lvupflag", false);
		setInt(mode, "nextseclv", 100);
		engine.statistics.level = 99; // == nextseclv - 1, so the (level < nextseclv-1) guard is false

		mode.onMove(engine, 0);

		assertEquals(99, engine.statistics.level,
				"at the section edge the per-piece level bump must be suppressed");
	}

	// --- reflection helpers ---

	private static void invokeLevelUp(GradeMania3Mode mode, GameEngine engine) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("levelUp", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setFloat(Object obj, String name, float value) throws Exception {
		field(obj.getClass(), name).setFloat(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
