package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Additional branch coverage for {@link RetroMasteryMode}: the PRESSURE-aware
 * cursor navigation that skips the LEVEL row (lines 195/196/202/203), the
 * BIG toggle, the PRESSURE background branches in
 * {@link RetroMasteryMode#playerInit}, renderSetting cursor-highlight and the
 * non-PRESSURE LEVEL row, the level-up level clamp, and the meter color ladders
 * in {@link RetroMasteryMode#calcScore} for the PRESSURE and start-level paths.
 */
class RetroMasteryModeBranchCoverageTest4 {

	private static final int GAMETYPE_ENDLESS = 1;
	private static final int GAMETYPE_PRESSURE = 2;

	private static GameEngine fresh(RetroMasteryMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Inject a single menu key (UP/DOWN/LEFT/RIGHT) and run onSetting. */
	private static void press(RetroMasteryMode mode, GameEngine engine, int button) throws Exception {
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[button] = true;
		engine.ctrl.buttonTime[button] = 1;
		mode.onSetting(engine, 0);
	}

	// ---- PRESSURE cursor skips LEVEL row going UP (line 195) ----

	@Test
	void cursorUpSkipsLevelRowInPressure() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_PRESSURE);
		setInt(mode, "menuCursor", 2);
		press(mode, engine, Controller.BUTTON_UP); // 2 -> 1, then PRESSURE skip -> 0
		assertEquals(0, readInt(mode, "menuCursor"));
	}

	// ---- cursor UP wraparound below zero (line 196) ----

	@Test
	void cursorUpWrapsBelowZero() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_ENDLESS); // not PRESSURE -> no skip
		setInt(mode, "menuCursor", 0);
		press(mode, engine, Controller.BUTTON_UP); // 0 -> -1 -> wrap to 2
		assertEquals(2, readInt(mode, "menuCursor"));
	}

	// ---- PRESSURE cursor skips LEVEL row going DOWN (line 202) ----

	@Test
	void cursorDownSkipsLevelRowInPressure() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_PRESSURE);
		setInt(mode, "menuCursor", 0);
		press(mode, engine, Controller.BUTTON_DOWN); // 0 -> 1, then PRESSURE skip -> 2
		assertEquals(2, readInt(mode, "menuCursor"));
	}

	// ---- cursor DOWN wraparound above max (line 203) ----

	@Test
	void cursorDownWrapsAboveMax() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_ENDLESS);
		setInt(mode, "menuCursor", 2);
		press(mode, engine, Controller.BUTTON_DOWN); // 2 -> 3 -> wrap to 0
		assertEquals(0, readInt(mode, "menuCursor"));
	}

	// ---- settings value wraparound on the three cursors ----

	@Test
	void gametypeWrapsAtBothBounds() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gametype", 0);
		setInt(mode, "menuCursor", 0);
		press(mode, engine, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "gametype")); // GAMETYPE_MAX-1

		setInt(mode, "gametype", 2);
		setInt(mode, "menuCursor", 0);
		press(mode, engine, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "gametype"));
	}

	@Test
	void startlevelWrapsAtBothBounds() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_ENDLESS); // level row visible

		setInt(mode, "startlevel", 0);
		setInt(mode, "menuCursor", 1);
		press(mode, engine, Controller.BUTTON_LEFT);
		assertEquals(19, readInt(mode, "startlevel"));

		setInt(mode, "startlevel", 19);
		setInt(mode, "menuCursor", 1);
		press(mode, engine, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "startlevel"));
	}

	@Test
	void bigToggleCase2() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setBool(mode, "big", false);
		setInt(mode, "menuCursor", 2);
		press(mode, engine, Controller.BUTTON_RIGHT);
		assertTrue(readBool(mode, "big"));
	}

	// ---- playerInit PRESSURE background = 0 (line 164) ----

	@Test
	void playerInitPressureBackgroundIsZero() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		engine.owner.modeConfig.setProperty("retromastery.gametype", GAMETYPE_PRESSURE);
		engine.owner.modeConfig.setProperty("retromastery.startlevel", 10);
		mode.playerInit(engine, 0);
		assertEquals(0, engine.owner.backgroundStatus.bg); // PRESSURE -> 0, not startlevel
	}

	@Test
	void playerInitClampsBackgroundWhenStartlevelHigh() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		engine.owner.modeConfig.setProperty("retromastery.gametype", 0); // 200, not PRESSURE
		engine.owner.modeConfig.setProperty("retromastery.startlevel", 25);
		mode.playerInit(engine, 0);
		assertEquals(19, engine.owner.backgroundStatus.bg);
	}

	// ---- renderSetting: non-PRESSURE shows LEVEL row, cursor highlights ----

	@Test
	void renderSettingNonPressureShowsLevelRow() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0); // not PRESSURE -> level row drawn
		setInt(mode, "menuCursor", 1); // highlight level row (menuCursor == 1 true)
		mode.renderSetting(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderSettingPressureHidesLevelRowAndHighlightsBig() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_PRESSURE); // level row hidden (false branch)
		setInt(mode, "menuCursor", 2); // highlight BIG row (menuCursor == 2 true)
		mode.renderSetting(engine, 0);
		assertTrue(true);
	}

	// ---- calcScore level-up level clamp at high level (line 417 true side) ----

	@Test
	void calcScoreLevelUpClampsFadeBgAtHighLevel() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_ENDLESS);
		setInt(mode, "loons", 20);
		setInt(mode, "levellines", 10); // loons >= levellines -> level up
		engine.statistics.level = 30; // after ++ -> 31; lv>=19 clamp -> 19
		mode.calcScore(engine, 0, 0);
		assertEquals(19, engine.owner.backgroundStatus.fadebg);
	}

	// ---- calcScore PRESSURE meter ladder (line 431 yellow at togo == 3) ----

	@Test
	void calcScorePressureMeterYellowAtTogo3() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_PRESSURE);
		setInt(mode, "loons", 7);
		setInt(mode, "levellines", 10); // no level-up (7 < 10); togo = 3 -> YELLOW
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	// ---- calcScore start-level meter ladder (lines 435/436/437) ----

	@Test
	void calcScoreStartLevelMeterRedAtTogo5() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_ENDLESS);
		setInt(mode, "startlevel", 5);
		engine.statistics.level = 5; // level == startlevel && startlevel != 0
		setInt(mode, "loons", 16);
		setInt(mode, "levellines", 21); // togo = 5 -> RED
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void calcScoreStartLevelMeterOrangeAtTogo8() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_ENDLESS);
		setInt(mode, "startlevel", 5);
		engine.statistics.level = 5;
		setInt(mode, "loons", 16);
		setInt(mode, "levellines", 24); // togo = 8 -> ORANGE
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void calcScoreStartLevelMeterYellowAtTogo15() throws Exception {
		RetroMasteryMode mode = new RetroMasteryMode();
		GameEngine engine = fresh(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", GAMETYPE_ENDLESS);
		setInt(mode, "startlevel", 5);
		engine.statistics.level = 5;
		setInt(mode, "loons", 16);
		setInt(mode, "levellines", 31); // togo = 15 -> YELLOW
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	// --- reflection helpers ---

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
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
