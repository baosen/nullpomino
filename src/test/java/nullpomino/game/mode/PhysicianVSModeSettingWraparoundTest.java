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
 * Drives {@link PhysicianVSMode#onSetting} across every numeric configuration
 * cursor (0-16) past both bounds so the wraparound branches all fire, plus the
 * BUTTON_E (x100) / BUTTON_F (x1000) multipliers and the hoverBlocks fast-adjust.
 */
class PhysicianVSModeSettingWraparoundTest {

	private static GameEngine freshEngine(PhysicianVSMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		manager.engine[0].createFieldIfNeeded();
		return manager.engine[0];
	}

	private static void change(PhysicianVSMode mode, GameEngine engine, int cursor, int dir, int mult) throws Exception {
		setInt(mode, "menuCursor", cursor);
		engine.statc[4] = 0;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dir] = true;
		engine.ctrl.buttonTime[dir] = 1;
		if(mult >= 0) {
			engine.ctrl.buttonPress[mult] = true;
			engine.ctrl.buttonTime[mult] = 1;
		}
		mode.onSetting(engine, 0);
	}

	@Test
	void speedCursorsWrapAtBothBoundsWithMultipliers() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.speed.gravity = -1;
		change(mode, engine, 0, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(99999, engine.speed.gravity);
		engine.speed.gravity = 99999;
		change(mode, engine, 0, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		assertEquals(-1, engine.speed.gravity);

		engine.speed.denominator = -1;
		change(mode, engine, 1, Controller.BUTTON_LEFT, -1);
		assertEquals(99999, engine.speed.denominator);
		engine.speed.denominator = 99999;
		change(mode, engine, 1, Controller.BUTTON_RIGHT, -1);
		assertEquals(-1, engine.speed.denominator);

		int[] cursors = {2, 3, 4, 5, 6};
		String[] fields = {"are", "areLine", "lineDelay", "lockDelay", "das"};
		for(int i = 0; i < cursors.length; i++) {
			setSpeed(engine, fields[i], 0);
			change(mode, engine, cursors[i], Controller.BUTTON_LEFT, -1);
			assertEquals(99, getSpeed(engine, fields[i]));
			setSpeed(engine, fields[i], 99);
			change(mode, engine, cursors[i], Controller.BUTTON_RIGHT, -1);
			assertEquals(0, getSpeed(engine, fields[i]));
		}
	}

	@Test
	void presetSpeedHoverBgmAndMapCursorsWrap() throws Exception {
		PhysicianVSMode mode = new PhysicianVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 7/8 presetNumber
		setIntArray0(mode, "presetNumber", 0);
		change(mode, engine, 7, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readIntArray0(mode, "presetNumber"));
		setIntArray0(mode, "presetNumber", 99);
		change(mode, engine, 8, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readIntArray0(mode, "presetNumber"));

		// case 9 speed (0..2)
		setIntArray0(mode, "speed", 0);
		change(mode, engine, 9, Controller.BUTTON_LEFT, -1);
		assertEquals(2, readIntArray0(mode, "speed"));
		setIntArray0(mode, "speed", 2);
		change(mode, engine, 9, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readIntArray0(mode, "speed"));

		// case 10 hoverBlocks (1..99) with the x10 fast-adjust
		setIntArray0(mode, "hoverBlocks", 1);
		change(mode, engine, 10, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readIntArray0(mode, "hoverBlocks"));
		setIntArray0(mode, "hoverBlocks", 99);
		change(mode, engine, 10, Controller.BUTTON_RIGHT, -1);
		assertEquals(1, readIntArray0(mode, "hoverBlocks"));
		setIntArray0(mode, "hoverBlocks", 50);
		change(mode, engine, 10, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(40, readIntArray0(mode, "hoverBlocks"));

		// case 11/12 toggles (switch arms)
		change(mode, engine, 11, Controller.BUTTON_RIGHT, -1);
		change(mode, engine, 12, Controller.BUTTON_RIGHT, -1);

		// case 13 bgmno
		setInt(mode, "bgmno", 0);
		change(mode, engine, 13, Controller.BUTTON_LEFT, -1);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));
		setInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
		change(mode, engine, 13, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "bgmno"));

		// case 15 mapSet (0..99) with useMap off so no preview load
		setBoolArray0(mode, "useMap", false);
		setIntArray0(mode, "mapSet", 0);
		change(mode, engine, 15, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readIntArray0(mode, "mapSet"));
		setIntArray0(mode, "mapSet", 99);
		change(mode, engine, 15, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readIntArray0(mode, "mapSet"));

		// case 16 mapNumber with useMap on exercises the switch arm + loadMapPreview
		// (loadMapPreview reloads mapMaxNo, so the exact wrapped value is not asserted).
		setBoolArray0(mode, "useMap", true);
		change(mode, engine, 16, Controller.BUTTON_LEFT, -1);
		change(mode, engine, 16, Controller.BUTTON_RIGHT, -1);
	}

	// --- helpers ---
	private static void setSpeed(GameEngine e, String n, int v) throws Exception {
		e.speed.getClass().getField(n).setInt(e.speed, v);
	}
	private static int getSpeed(GameEngine e, String n) throws Exception {
		return e.speed.getClass().getField(n).getInt(e.speed);
	}
	private static void setInt(Object o, String n, int v) throws Exception { field(o.getClass(), n).setInt(o, v); }
	private static int readInt(Object o, String n) throws Exception { return field(o.getClass(), n).getInt(o); }
	private static void setIntArray0(Object o, String n, int v) throws Exception { ((int[]) field(o.getClass(), n).get(o))[0] = v; }
	private static int readIntArray0(Object o, String n) throws Exception { return ((int[]) field(o.getClass(), n).get(o))[0]; }
	private static void setBoolArray0(Object o, String n, boolean v) throws Exception { ((boolean[]) field(o.getClass(), n).get(o))[0] = v; }

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
