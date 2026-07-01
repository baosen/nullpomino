package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link AvalancheFeverMode#onSetting} configuration wraparound branches:
 * outlinetype, numColors, chainDisplayType, mapSet (without the xyzzy cheat so no
 * fever-map reload), and fastenable (reachable only with the xyzzy cheat).
 */
class AvalancheFeverModeSettingWraparoundTest {

	private static GameEngine freshEngine(AvalancheFeverMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void change(AvalancheFeverMode mode, GameEngine engine, int cursor, int dir) throws Exception {
		setInt(mode, "menuCursor", cursor);
		engine.statc[4] = 0;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[dir] = true;
		engine.ctrl.buttonTime[dir] = 1;
		mode.onSetting(engine, 0);
	}

	@Test
	void outlineNumColorsChainDisplayWrapAtBothBounds() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "mapSet", 0); // keep mapSet != 4 so the post-switch numColors override is inert

		// cursor 1 outlinetype (0-2)
		setInt(mode, "outlinetype", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "outlinetype"));
		setInt(mode, "outlinetype", 2);
		change(mode, engine, 1, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "outlinetype"));

		// cursor 2 numColors (3-5)
		setInt(mode, "mapSet", 0);
		setInt(mode, "numColors", 3);
		change(mode, engine, 2, Controller.BUTTON_LEFT);
		assertEquals(5, readInt(mode, "numColors"));
		setInt(mode, "mapSet", 0);
		setInt(mode, "numColors", 5);
		change(mode, engine, 2, Controller.BUTTON_RIGHT);
		assertEquals(3, readInt(mode, "numColors"));

		// cursor 3 chainDisplayType (0-2)
		setInt(mode, "chainDisplayType", 0);
		change(mode, engine, 3, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "chainDisplayType"));
		setInt(mode, "chainDisplayType", 2);
		change(mode, engine, 3, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "chainDisplayType"));

		// cursor 4 bigDisplay toggle (switch arm)
		change(mode, engine, 4, Controller.BUTTON_RIGHT);
	}

	@Test
	void mapSetWrapsWithoutCheatAndFastenableWithCheat() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// cursor 0 mapSet wraps against FEVER_MAPS.length (xyzzy != 573 -> no map reload)
		setInt(mode, "xyzzy", 0);
		setInt(mode, "mapSet", 999);
		change(mode, engine, 0, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "mapSet"));
		setInt(mode, "mapSet", 0);
		change(mode, engine, 0, Controller.BUTTON_LEFT); // -> FEVER_MAPS.length-1 (exercise)

		// cursor 5 fastenable (0-2) is only reachable with the xyzzy==573 cheat
		setInt(mode, "xyzzy", 573);
		setInt(mode, "fastenable", 0);
		change(mode, engine, 5, Controller.BUTTON_LEFT);
		assertEquals(2, readInt(mode, "fastenable"));
		setInt(mode, "fastenable", 2);
		change(mode, engine, 5, Controller.BUTTON_RIGHT);
		assertEquals(0, readInt(mode, "fastenable"));
	}

	private static void setInt(Object o, String n, int v) throws Exception { field(o.getClass(), n).setInt(o, v); }
	private static int readInt(Object o, String n) throws Exception { return field(o.getClass(), n).getInt(o); }

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
