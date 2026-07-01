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
 * Covers {@link PhysicianMode#onSetting} configuration branches: the hoverBlocks
 * wraparound (both bounds) with the x10 fast-adjust (BUTTON_E/BUTTON_F), the
 * speed cursor wraparound (the previously-unexercised switch case 1), and the
 * BUTTON_A menuTime guard.
 */
class PhysicianModeSettingBranchTest {

	private static GameEngine freshEngine(PhysicianMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void change(PhysicianMode mode, GameEngine engine, int cursor, int dir, int mult) throws Exception {
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", 0);
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
	void hoverBlocksWrapsAtBothBoundsWithFastAdjust() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "hoverBlocks", 1);
		change(mode, engine, 0, Controller.BUTTON_LEFT, -1);
		assertEquals(99, readInt(mode, "hoverBlocks"));

		setInt(mode, "hoverBlocks", 99);
		change(mode, engine, 0, Controller.BUTTON_RIGHT, -1);
		assertEquals(1, readInt(mode, "hoverBlocks"));

		// BUTTON_E -> m=100 (>=10) -> step of 10
		setInt(mode, "hoverBlocks", 50);
		change(mode, engine, 0, Controller.BUTTON_LEFT, Controller.BUTTON_E);
		assertEquals(40, readInt(mode, "hoverBlocks"));

		// BUTTON_F -> m=1000 (>=10) -> step of 10
		setInt(mode, "hoverBlocks", 50);
		change(mode, engine, 0, Controller.BUTTON_RIGHT, Controller.BUTTON_F);
		assertEquals(60, readInt(mode, "hoverBlocks"));
	}

	@Test
	void speedCursorWrapsAtBothBounds() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "speed", 0);
		change(mode, engine, 1, Controller.BUTTON_LEFT, -1);
		assertEquals(2, readInt(mode, "speed"));

		setInt(mode, "speed", 2);
		change(mode, engine, 1, Controller.BUTTON_RIGHT, -1);
		assertEquals(0, readInt(mode, "speed"));
	}

	@Test
	void confirmIgnoredWhenMenuTimeTooLow() throws Exception {
		PhysicianMode mode = new PhysicianMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// BUTTON_A with menuTime < 5 -> the (menuTime >= 5) guard is false, keeps running
		setInt(mode, "menuTime", 0);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		assertTrue(mode.onSetting(engine, 0));
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
