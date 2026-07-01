package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

/**
 * Drives {@link AvalancheVSFeverMode#onSetting} across the speed-parameter
 * cursors (0-6) past both bounds so their wraparound branches fire, plus the
 * BUTTON_E (x100) / BUTTON_F (x1000) multipliers on gravity/denominator.
 */
class AvalancheVSFeverModeSettingWraparoundTest {

	private static GameEngine freshEngine(AvalancheVSFeverMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void change(AvalancheVSFeverMode mode, GameEngine engine, int cursor, int dir, int mult) throws Exception {
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
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
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

		// cursors 2/3/4/6 = are/areLine/lineDelay/das, all clamped 0..99
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

		// cursor 5 = lockDelay, clamped 0..999 with an x10 fast-adjust
		setSpeed(engine, "lockDelay", 0);
		change(mode, engine, 5, Controller.BUTTON_LEFT, -1);
		assertEquals(999, getSpeed(engine, "lockDelay"));
		setSpeed(engine, "lockDelay", 999);
		change(mode, engine, 5, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, getSpeed(engine, "lockDelay"));
		setSpeed(engine, "lockDelay", 500);
		change(mode, engine, 5, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(490, getSpeed(engine, "lockDelay"));
	}

	@Test
	void gameplayAndDisplayCursorsWrapAtBothBounds() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// cursor 7/8 -> engine.cascadeDelay (0-20) / cascadeClearDelay (0-99) scalars
		engine.cascadeDelay = 0;
		change(mode, engine, 7, Controller.BUTTON_LEFT, -1);
		assertEquals(20, engine.cascadeDelay);
		engine.cascadeDelay = 20;
		change(mode, engine, 7, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.cascadeDelay);
		engine.cascadeClearDelay = 0;
		change(mode, engine, 8, Controller.BUTTON_LEFT, -1);
		assertEquals(99, engine.cascadeClearDelay);
		engine.cascadeClearDelay = 99;
		change(mode, engine, 8, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, engine.cascadeClearDelay);

		wrapArray(mode, engine, 9, "zenKeshiType", 0, 2, -1);
		wrapArray(mode, engine, 10, "maxAttack", 0, 99, -1);
		// cursor 10 also has an x10 fast-adjust
		setIntArray0(mode, "maxAttack", 50);
		change(mode, engine, 10, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(40, readIntArray0(mode, "maxAttack"));
		wrapArray(mode, engine, 11, "numColors", 3, 5, -1);
		wrapArray(mode, engine, 12, "rensaShibari", 1, 20, -1);
		wrapArray(mode, engine, 15, "ojamaHard", 0, 9, -1);
		wrapArray(mode, engine, 21, "outlineType", 0, 2, -1);
		wrapArray(mode, engine, 22, "chainDisplayType", 0, 4, -1);
		wrapArray(mode, engine, 28, "presetNumber", 0, 99, -1);

		// cursor 13 ojamaRate (10-1000, plain change is x10)
		setIntArray0(mode, "ojamaRate", 10);
		change(mode, engine, 13, Controller.BUTTON_LEFT, -1);
		assertEquals(1000, readIntArray0(mode, "ojamaRate"));
		setIntArray0(mode, "ojamaRate", 1000);
		change(mode, engine, 13, Controller.BUTTON_RIGHT, -1);
		assertEquals(10, readIntArray0(mode, "ojamaRate"));

		// cursor 14 hurryupSeconds (0-300)
		wrapArray(mode, engine, 14, "hurryupSeconds", 0, 300, -1);
		// cursor 18 ojamaHandicap (0-9999, change*m)
		wrapArray(mode, engine, 18, "ojamaHandicap", 0, 9999, -1);

		// cursor 25 bgmno (scalar)
		setInt(mode, "bgmno", 0);
		change(mode, engine, 25, Controller.BUTTON_LEFT, -1);
		assertEquals(BGMStatus.BGM_COUNT - 1, readInt(mode, "bgmno"));
		setInt(mode, "bgmno", BGMStatus.BGM_COUNT - 1);
		change(mode, engine, 25, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "bgmno"));

		// toggle switch arms (16/17/23/24/26/27) — exercised, no exact assertion needed
		for(int c : new int[] {16, 17, 23, 24, 26, 27}) {
			change(mode, engine, c, Controller.BUTTON_RIGHT, -1);
		}
	}

	/** Drives an int[] cursor to its lower bound (LEFT wrap to max) then upper (RIGHT wrap to min). */
	private static void wrapArray(AvalancheVSFeverMode mode, GameEngine engine, int cursor,
			String fieldName, int min, int max, int mult) throws Exception {
		setIntArray0(mode, fieldName, min);
		change(mode, engine, cursor, Controller.BUTTON_LEFT, mult);
		assertEquals(max, readIntArray0(mode, fieldName), fieldName + " underflow");
		setIntArray0(mode, fieldName, max);
		change(mode, engine, cursor, Controller.BUTTON_RIGHT, mult);
		assertEquals(min, readIntArray0(mode, fieldName), fieldName + " overflow");
	}

	private static void setIntArray0(Object o, String n, int v) throws Exception {
		((int[]) field(o.getClass(), n).get(o))[0] = v;
	}
	private static int readIntArray0(Object o, String n) throws Exception {
		return ((int[]) field(o.getClass(), n).get(o))[0];
	}
	private static int readInt(Object o, String n) throws Exception { return field(o.getClass(), n).getInt(o); }
	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void setSpeed(GameEngine e, String n, int v) throws Exception {
		e.speed.getClass().getField(n).setInt(e.speed, v);
	}
	private static int getSpeed(GameEngine e, String n) throws Exception {
		return e.speed.getClass().getField(n).getInt(e.speed);
	}
	private static void setInt(Object o, String n, int v) throws Exception {
		Class<?> c = o.getClass();
		while (c != null) {
			try { Field f = c.getDeclaredField(n); f.setAccessible(true); f.setInt(o, v); return; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(n);
	}
}
