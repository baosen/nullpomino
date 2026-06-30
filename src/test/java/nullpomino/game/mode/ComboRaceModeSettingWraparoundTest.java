package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link ComboRaceMode#onSetting} across the numeric configuration
 * cursors, pushing each value past both its lower and upper bound so the
 * wraparound branches (value &lt; min and value &gt; max) all fire. The
 * existing {@code ComboRaceModeCoverageBoostTest} only exercises one direction
 * for several cursors; this class fills in the opposite-direction wrap
 * branches (goaltype RIGHT-wrap L295, bgmno RIGHT-wrap L364, the spawnAboveField
 * toggle from both starting states L324) plus the BUTTON_E (x100) / BUTTON_F
 * (x1000) speed multipliers, and the renderSetting "BELOW" spawn label (L436).
 */
class ComboRaceModeSettingWraparoundTest {

	private static GameEngine freshEngine(ComboRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Sets menuCursor, injects a single LEFT/RIGHT (and optional E/F) press, then runs onSetting. */
	private static void change(ComboRaceMode mode, GameEngine engine, int cursor, int dirButton, int multButton)
			throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		// reset() clears both buttonPress and buttonTime; clearButtonState() would
		// leave stale buttonTime entries so a prior LEFT keeps overriding RIGHT.
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		if (multButton >= 0) {
			engine.ctrl.buttonPress[multButton] = true;
			engine.ctrl.buttonTime[multButton] = 1;
		}
		mode.onSetting(engine, 0);
	}

	// -----------------------------------------------------------------------
	// case 0: goaltype - both wrap directions (L294 already covered; L295 here)
	// -----------------------------------------------------------------------

	@Test
	void goaltypeWrapsAtBothBounds() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow: 0 - 1 < 0 -> GOAL_TABLE.length-1 == 3
		setInt(mode, "goaltype", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT, -1);
		assertEquals(3, readInt(mode, "goaltype"));

		// Overflow: 3 + 1 > 3 -> 0  (L295)
		setInt(mode, "goaltype", 3);
		change(mode, engine, 0, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "goaltype"));
	}

	// -----------------------------------------------------------------------
	// case 1: shapetype - both wrap directions
	// -----------------------------------------------------------------------

	@Test
	void shapetypeWrapsAtBothBounds() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "shapetype", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT, -1);
		assertEquals(8, readInt(mode, "shapetype"));

		setInt(mode, "shapetype", 8);
		change(mode, engine, 1, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "shapetype"));
	}

	// -----------------------------------------------------------------------
	// case 4: ceilingAdjust - both wrap directions
	// -----------------------------------------------------------------------

	@Test
	void ceilingAdjustWrapsAtBothBounds() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "ceilingAdjust", 10);
		change(mode, engine, 4, Controller.BUTTON_RIGHT, -1);
		assertEquals(-10, readInt(mode, "ceilingAdjust"));

		setInt(mode, "ceilingAdjust", -10);
		change(mode, engine, 4, Controller.BUTTON_LEFT, -1);
		assertEquals(10, readInt(mode, "ceilingAdjust"));
	}

	// -----------------------------------------------------------------------
	// case 5: spawnAboveField toggle - both starting states (L324)
	// -----------------------------------------------------------------------

	@Test
	void spawnAboveFieldTogglesFromBothStates() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "spawnAboveField", true);
		change(mode, engine, 5, Controller.BUTTON_RIGHT, -1);
		assertEquals(false, readBool(mode, "spawnAboveField"));

		setBool(mode, "spawnAboveField", false);
		change(mode, engine, 5, Controller.BUTTON_LEFT, -1);
		assertEquals(true, readBool(mode, "spawnAboveField"));
	}

	// -----------------------------------------------------------------------
	// case 6: gravity with x100 / x1000 multipliers at both bounds
	// -----------------------------------------------------------------------

	@Test
	void gravityWrapsAtBothBoundsWithMultipliers() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow with x100 (BUTTON_E): -1 - 100 < -1 -> 99999
		engine.speed.gravity = -1;
		change(mode, engine, 6, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(99999, engine.speed.gravity);

		// Overflow with x1000 (BUTTON_F): 99999 + 1000 > 99999 -> -1
		engine.speed.gravity = 99999;
		change(mode, engine, 6, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		assertEquals(-1, engine.speed.gravity);
	}

	// -----------------------------------------------------------------------
	// case 7: denominator with x1000 multiplier at both bounds
	// -----------------------------------------------------------------------

	@Test
	void denominatorWrapsAtBothBoundsWithMultiplier() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.speed.denominator = -1;
		change(mode, engine, 7, Controller.BUTTON_LEFT, Controller.BUTTON_F);
		assertEquals(99999, engine.speed.denominator);

		engine.speed.denominator = 99999;
		change(mode, engine, 7, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		assertEquals(-1, engine.speed.denominator);
	}

	// -----------------------------------------------------------------------
	// cases 8-12: are / areLine / lineDelay / lockDelay / das wrap both ways
	// -----------------------------------------------------------------------

	@Test
	void speedFieldsWrapAtBothBounds() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 8: are
		engine.speed.are = 0;
		change(mode, engine, 8, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.are);
		engine.speed.are = 99;
		change(mode, engine, 8, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.are);

		// case 9: areLine
		engine.speed.areLine = 0;
		change(mode, engine, 9, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.areLine);
		engine.speed.areLine = 99;
		change(mode, engine, 9, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.areLine);

		// case 10: lineDelay
		engine.speed.lineDelay = 0;
		change(mode, engine, 10, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.lineDelay);
		engine.speed.lineDelay = 99;
		change(mode, engine, 10, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.lineDelay);

		// case 11: lockDelay
		engine.speed.lockDelay = 0;
		change(mode, engine, 11, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.lockDelay);
		engine.speed.lockDelay = 99;
		change(mode, engine, 11, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.lockDelay);

		// case 12: das
		engine.speed.das = 0;
		change(mode, engine, 12, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.speed.das);
		engine.speed.das = 99;
		change(mode, engine, 12, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.speed.das);
	}

	// -----------------------------------------------------------------------
	// case 13: bgmno - both wrap directions (L363 covered elsewhere; L364 here)
	// -----------------------------------------------------------------------

	@Test
	void bgmnoWrapsAtBothBounds() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Underflow: 0 - 1 < 0 -> BGM_COUNT - 1
		setInt(mode, "bgmno", 0);
		change(mode, engine, 13, Controller.BUTTON_LEFT, -1);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));

		// Overflow: (BGM_COUNT-1) + 1 > BGM_COUNT-1 -> 0  (L364)
		setInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
		change(mode, engine, 13, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "bgmno"));
	}

	// -----------------------------------------------------------------------
	// cases 14 & 15: presetNumber - both wrap directions on both cursors
	// -----------------------------------------------------------------------

	@Test
	void presetNumberWrapsAtBothBoundsOnBothCursors() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// cursor 14 underflow
		setInt(mode, "presetNumber", 0);
		change(mode, engine, 14, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readInt(mode, "presetNumber"));

		// cursor 15 overflow
		setInt(mode, "presetNumber", 99);
		change(mode, engine, 15, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "presetNumber"));
	}

	// -----------------------------------------------------------------------
	// renderSetting: spawnAboveField "BELOW" label branch (L436)
	// -----------------------------------------------------------------------

	@Test
	void renderSettingShowsBelowSpawnLabel() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "menuCursor", 0); // < 6 -> first page (renders strSpawn)
		setBool(mode, "spawnAboveField", false); // -> "BELOW" ternary branch
		mode.renderSetting(engine, 0);

		// And the "ABOVE" branch for completeness.
		setBool(mode, "spawnAboveField", true);
		mode.renderSetting(engine, 0);
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
