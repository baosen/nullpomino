// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link GradeMania3Mode#onSetting} across the numeric configuration
 * cursors that wrap or clamp at a boundary, pushing each value past both its
 * lower and upper limit so the otherwise-unexercised wraparound branches all
 * fire. The existing {@code GradeMania3ModeSettingMenuTest} only covers the
 * plain +1 increment of each cursor (the value stays in range), leaving every
 * {@code if(x < min) x = ...} / {@code if(x > max) x = ...} arm uncovered.
 *
 * <p>Covered branches (line numbers in GradeMania3Mode.java):
 * <ul>
 *   <li>case 0 startlevel: L611 (&lt;0-&gt;11), L612 (&gt;11-&gt;0), L621 (internal clamp to min)</li>
 *   <li>case 1 internalStartLevel: L633 (&lt;min-&gt;max), L634 (&gt;max-&gt;min)</li>
 *   <li>case 7 lv500torikan: L653 (BUTTON_E hour step), L655 (&lt;0-&gt;72000), L656 (&gt;72000-&gt;0)</li>
 *   <li>case 8 stcolor: L660 (&lt;0-&gt;2), L661 (&gt;2-&gt;0)</li>
 * </ul>
 */
class GradeMania3ModeSettingWraparoundTest {

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

	/** Sets menuCursor, injects a single LEFT/RIGHT (and optional E/F) press, then runs onSetting. */
	private static void change(GradeMania3Mode mode, GameEngine engine, int cursor, int dirButton, int multButton)
			throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
		// reset() clears both buttonPress and buttonTime; clearButtonState()
		// would leave stale buttonTime so a prior LEFT keeps overriding RIGHT.
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dirButton] = true;
		engine.ctrl.buttonTime[dirButton] = 1;
		if (multButton >= 0) {
			engine.ctrl.buttonPress[multButton] = true;
			engine.ctrl.buttonTime[multButton] = 1;
		}
		mode.onSetting(engine, 0);
	}

	// -----------------------------------------------------------------
	// case 0: startlevel (with internal-speed clamp)
	// -----------------------------------------------------------------

	@Test
	void startlevelWrapsBelowZeroToEleven() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT, -1); // 0 - 1 = -1 -> 11
		assertEquals(11, readInt(mode, "startlevel"),
				"startlevel below 0 should wrap to 11 (M-ROLL)");
		// startlevel == 11 branch sets internalStartLevel = 1200
		assertEquals(1200, readInt(mode, "internalStartLevel"));
	}

	@Test
	void startlevelWrapsAboveElevenToZero() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 11);
		change(mode, engine, 0, Controller.BUTTON_RIGHT, -1); // 11 + 1 = 12 -> 0
		assertEquals(0, readInt(mode, "startlevel"),
				"startlevel above 11 should wrap to 0");
	}

	@Test
	void startlevelClampsInternalSpeedUpToMin() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel 5 -> 6 (change=+1): minSpeed = min(6,9)*100 = 600.
		// internalStartLevel starts at 0; L617 bumps it by +100 to 100,
		// which is still < 600, so L621 clamps it up to minSpeed = 600.
		setInt(mode, "startlevel", 5);
		setInt(mode, "internalStartLevel", 0);
		change(mode, engine, 0, Controller.BUTTON_RIGHT, -1);
		assertEquals(6, readInt(mode, "startlevel"));
		assertEquals(600, readInt(mode, "internalStartLevel"),
				"internalStartLevel below minSpeed should clamp up to minSpeed");
	}

	// -----------------------------------------------------------------
	// case 1: internalStartLevel speed wrap (min<->max)
	// -----------------------------------------------------------------

	@Test
	void internalStartLevelWrapsBelowMinToMax() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel 5: minSpeed = 500, maxSpeed = min(10,12)*100 = 1000.
		// internalStartLevel 400, change=-1 -> 300, which is < 500 ->
		// L633 sets it to maxSpeed = 1000. (1000 > 1000 is false, no L634.)
		setInt(mode, "startlevel", 5);
		setInt(mode, "internalStartLevel", 400);
		change(mode, engine, 1, Controller.BUTTON_LEFT, -1);
		assertEquals(1000, readInt(mode, "internalStartLevel"),
				"internalStartLevel below minSpeed should wrap to maxSpeed");
	}

	@Test
	void internalStartLevelWrapsAboveMaxToMin() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel 5: minSpeed = 500, maxSpeed = 1000.
		// internalStartLevel 1000, change=+1 -> 1100, which is > 1000 ->
		// L634 sets it to minSpeed = 500.
		setInt(mode, "startlevel", 5);
		setInt(mode, "internalStartLevel", 1000);
		change(mode, engine, 1, Controller.BUTTON_RIGHT, -1);
		assertEquals(500, readInt(mode, "internalStartLevel"),
				"internalStartLevel above maxSpeed should wrap to minSpeed");
	}

	// -----------------------------------------------------------------
	// case 7: lv500torikan (60-frame step, BUTTON_E = 3600-frame hour step)
	// -----------------------------------------------------------------

	@Test
	void lv500torikanWrapsBelowZeroToMax() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "lv500torikan", 0);
		change(mode, engine, 7, Controller.BUTTON_LEFT, -1); // 0 - 60 = -60 -> 72000
		assertEquals(72000, readInt(mode, "lv500torikan"),
				"lv500torikan below 0 should wrap to 72000");
	}

	@Test
	void lv500torikanWrapsAboveMaxToZeroWithHourStep() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// BUTTON_E held: change step becomes 3600 frames (L653).
		// 72000 + 3600 = 75600 > 72000 -> wraps to 0 (L656).
		setInt(mode, "lv500torikan", 72000);
		change(mode, engine, 7, Controller.BUTTON_RIGHT, Controller.BUTTON_E);
		assertEquals(0, readInt(mode, "lv500torikan"),
				"lv500torikan above 72000 (E-step) should wrap to 0");
	}

	// -----------------------------------------------------------------
	// case 8: stcolor (3-state cycle)
	// -----------------------------------------------------------------

	@Test
	void stcolorWrapsBelowZeroToTwo() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "stcolor", 0);
		change(mode, engine, 8, Controller.BUTTON_LEFT, -1); // 0 - 1 = -1 -> 2
		assertEquals(2, readInt(mode, "stcolor"),
				"stcolor below 0 should wrap to 2");
	}

	@Test
	void stcolorWrapsAboveTwoToZero() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "stcolor", 2);
		change(mode, engine, 8, Controller.BUTTON_RIGHT, -1); // 2 + 1 = 3 -> 0
		assertEquals(0, readInt(mode, "stcolor"),
				"stcolor above 2 should wrap to 0");
	}

	// --- reflection helpers ---

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
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
