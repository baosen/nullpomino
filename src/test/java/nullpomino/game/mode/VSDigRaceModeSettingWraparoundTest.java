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
 * Drives {@link VSDigRaceMode#onSetting} across every numeric configuration
 * cursor past both bounds so the wraparound branches all fire, plus the
 * BUTTON_E (x100) / BUTTON_F (x1000) speed multipliers.
 */
class VSDigRaceModeSettingWraparoundTest {

	private static GameEngine freshEngine(VSDigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void change(VSDigRaceMode mode, GameEngine engine, int cursor, int dir, int mult) throws Exception {
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
		VSDigRaceMode mode = new VSDigRaceMode();
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
	void presetGoalGarbageBgmAndToggleCursorsWrap() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// case 7/8 presetNumber[0]
		setIntArray0(mode, "presetNumber", 0);
		change(mode, engine, 7, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readIntArray0(mode, "presetNumber"));
		setIntArray0(mode, "presetNumber", 99);
		change(mode, engine, 8, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readIntArray0(mode, "presetNumber"));

		// case 9 goalLines[0] (min 1, max 18)
		setIntArray0(mode, "goalLines", 1);
		change(mode, engine, 9, Controller.BUTTON_LEFT, -1);
		assertEquals(18, readIntArray0(mode, "goalLines"));
		setIntArray0(mode, "goalLines", 18);
		change(mode, engine, 9, Controller.BUTTON_RIGHT, -1);
		assertEquals(1, readIntArray0(mode, "goalLines"));

		// case 10 garbagePercent[0] (0..100)
		setIntArray0(mode, "garbagePercent", 0);
		change(mode, engine, 10, Controller.BUTTON_LEFT, -1);
		assertEquals(100, readIntArray0(mode, "garbagePercent"));
		setIntArray0(mode, "garbagePercent", 100);
		change(mode, engine, 10, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readIntArray0(mode, "garbagePercent"));

		// case 11 enableSE toggle (switch arm)
		change(mode, engine, 11, Controller.BUTTON_RIGHT, -1);

		// case 12 bgmno
		setInt(mode, "bgmno", 0);
		change(mode, engine, 12, Controller.BUTTON_LEFT, -1);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));
		setInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
		change(mode, engine, 12, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "bgmno"));
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

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
