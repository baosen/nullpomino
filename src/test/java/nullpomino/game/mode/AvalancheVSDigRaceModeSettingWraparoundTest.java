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
 * Drives {@link AvalancheVSDigRaceMode#onSetting} across the configuration
 * cursors (0-28) past both bounds so the wraparound branches fire, plus the
 * BUTTON_E / BUTTON_F multipliers and lockDelay/ojamaRate fast-adjusts.
 */
class AvalancheVSDigRaceModeSettingWraparoundTest {

	private static GameEngine freshEngine(AvalancheVSDigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void change(AvalancheVSDigRaceMode mode, GameEngine engine, int cursor, int dir, int mult) throws Exception {
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
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
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

		int[] cursors = {2, 3, 4, 6};
		String[] fields = {"are", "areLine", "lineDelay", "das"};
		for(int i = 0; i < cursors.length; i++) {
			setSpeed(engine, fields[i], 0);
			change(mode, engine, cursors[i], Controller.BUTTON_LEFT, -1);
			assertEquals(99, getSpeed(engine, fields[i]));
			setSpeed(engine, fields[i], 99);
			change(mode, engine, cursors[i], Controller.BUTTON_RIGHT, -1);
			assertEquals(0, getSpeed(engine, fields[i]));
		}

		setSpeed(engine, "lockDelay", 0);
		change(mode, engine, 5, Controller.BUTTON_LEFT, -1);
		assertEquals(999, getSpeed(engine, "lockDelay"));
		setSpeed(engine, "lockDelay", 500);
		change(mode, engine, 5, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(490, getSpeed(engine, "lockDelay"));
	}

	@Test
	void gameplayAndDisplayCursorsWrapAtBothBounds() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.cascadeDelay = 0;
		change(mode, engine, 7, Controller.BUTTON_LEFT, -1);
		assertEquals(20, engine.cascadeDelay);
		engine.cascadeClearDelay = 0;
		change(mode, engine, 8, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.cascadeClearDelay);

		wrapArray(mode, engine, 9, "ojamaCounterMode", 0, 2);
		wrapArray(mode, engine, 10, "maxAttack", 0, 99);
		wrapArray(mode, engine, 11, "numColors", 3, 5);
		wrapArray(mode, engine, 12, "rensaShibari", 1, 20);

		setIntArray0(mode, "ojamaRate", 10);
		change(mode, engine, 13, Controller.BUTTON_LEFT, -1);
		assertEquals(1000, readIntArray0(mode, "ojamaRate"));
		setIntArray0(mode, "ojamaRate", 1000);
		change(mode, engine, 13, Controller.BUTTON_RIGHT, -1);
		assertEquals(10, readIntArray0(mode, "ojamaRate"));

		wrapArray(mode, engine, 14, "hurryupSeconds", 0, 300);
		wrapArray(mode, engine, 15, "ojamaHard", 0, 9);
		wrapArray(mode, engine, 18, "handicapRows", 0, 11);

		engine.colorClearSize = 2;
		change(mode, engine, 20, Controller.BUTTON_LEFT, -1);
		assertEquals(36, engine.colorClearSize);
		engine.colorClearSize = 36;
		change(mode, engine, 20, Controller.BUTTON_RIGHT, -1);
		assertEquals(2, engine.colorClearSize);

		wrapArray(mode, engine, 21, "outlineType", 0, 2);
		wrapArray(mode, engine, 22, "chainDisplayType", 0, 3);

		setInt(mode, "bgmno", 0);
		change(mode, engine, 24, Controller.BUTTON_LEFT, -1);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));

		wrapArray(mode, engine, 27, "presetNumber", 0, 99);

		// toggle switch arms (16/17/19/23/25/26)
		for(int c : new int[] {16, 17, 19, 23, 25, 26}) {
			change(mode, engine, c, Controller.BUTTON_RIGHT, -1);
		}
	}

	private static void wrapArray(AvalancheVSDigRaceMode mode, GameEngine engine, int cursor,
			String fieldName, int min, int max) throws Exception {
		setIntArray0(mode, fieldName, min);
		change(mode, engine, cursor, Controller.BUTTON_LEFT, -1);
		assertEquals(max, readIntArray0(mode, fieldName), fieldName + " underflow");
		setIntArray0(mode, fieldName, max);
		change(mode, engine, cursor, Controller.BUTTON_RIGHT, -1);
		assertEquals(min, readIntArray0(mode, fieldName), fieldName + " overflow");
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
