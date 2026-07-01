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
 * Drives {@link AvalancheVSBombBattleMode#onSetting} across the configuration
 * cursors (0-33) past both bounds so the wraparound branches fire, plus the
 * BUTTON_E (x100) / BUTTON_F (x1000) multipliers and hoverBlocks/lockDelay/
 * ojamaRate fast-adjusts.
 */
class AvalancheVSBombBattleModeSettingWraparoundTest {

	private static GameEngine freshEngine(AvalancheVSBombBattleMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		manager.engine[0].createFieldIfNeeded();
		return manager.engine[0];
	}

	private static void change(AvalancheVSBombBattleMode mode, GameEngine engine, int cursor, int dir, int mult) throws Exception {
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
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
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
		AvalancheVSBombBattleMode mode = new AvalancheVSBombBattleMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// cursor 7/8 -> engine scalars
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

		// cursor 13 -> engine.colorClearSize (2-36)
		engine.colorClearSize = 2;
		change(mode, engine, 13, Controller.BUTTON_LEFT, -1);
		assertEquals(36, engine.colorClearSize);
		engine.colorClearSize = 36;
		change(mode, engine, 13, Controller.BUTTON_RIGHT, -1);
		assertEquals(2, engine.colorClearSize);

		// cursor 14 ojamaRate (10-1000, plain change is x10)
		setIntArray0(mode, "ojamaRate", 10);
		change(mode, engine, 14, Controller.BUTTON_LEFT, -1);
		assertEquals(1000, readIntArray0(mode, "ojamaRate"));
		setIntArray0(mode, "ojamaRate", 1000);
		change(mode, engine, 14, Controller.BUTTON_RIGHT, -1);
		assertEquals(10, readIntArray0(mode, "ojamaRate"));

		wrapArray(mode, engine, 15, "hurryupSeconds", 0, 300);
		wrapArray(mode, engine, 16, "ojamaHard", 0, 9);
		wrapArray(mode, engine, 19, "ojamaCountdown", 0, 9);
		wrapArray(mode, engine, 20, "zenKeshiType", 0, 2);
		wrapArray(mode, engine, 22, "outlineType", 0, 2);
		wrapArray(mode, engine, 23, "chainDisplayType", 0, 3);

		// cursor 21 feverMapSet wraps against FEVER_MAPS.length; exercise both if-arms
		setIntArray0(mode, "feverMapSet", 0);
		change(mode, engine, 21, Controller.BUTTON_LEFT, -1); // <0 -> length-1
		setIntArray0(mode, "feverMapSet", 999);
		change(mode, engine, 21, Controller.BUTTON_RIGHT, -1); // >=length -> 0
		assertEquals(0, readIntArray0(mode, "feverMapSet"));

		// cursor 27 mapSet (0-99) with useMap off so no preview load
		setBoolArray0(mode, "useMap", false);
		setIntArray0(mode, "mapSet", 0);
		change(mode, engine, 27, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readIntArray0(mode, "mapSet"));
		setIntArray0(mode, "mapSet", 99);
		change(mode, engine, 27, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readIntArray0(mode, "mapSet"));

		// cursor 30 bgmno
		setInt(mode, "bgmno", 0);
		change(mode, engine, 30, Controller.BUTTON_LEFT, -1);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));

		// cursor 32/33 presetNumber
		wrapArray(mode, engine, 32, "presetNumber", 0, 99);

		// toggle / map switch arms (17/18/24/25/26/28/29/31) — exercise, no exact assert
		for(int c : new int[] {17, 18, 24, 25, 26, 28, 29, 31}) {
			change(mode, engine, c, Controller.BUTTON_RIGHT, -1);
		}
	}

	private static void wrapArray(AvalancheVSBombBattleMode mode, GameEngine engine, int cursor,
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
