package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers the replay-mode menu auto-advance in {@link AvalancheMode#onSetting}:
 * once {@code menuTime} reaches 60 the cursor jumps to entry 9 (line 226). The
 * sibling {@code menuTime >= 120 -> return false} arm (line 228) is dead — it is
 * an {@code else if} reached only when {@code menuTime < 60}, where
 * {@code menuTime >= 120} can never hold — so it is excluded, not tested.
 */
class AvalancheModeReplayMenuTest {

	private static Field field(Class<?> c, String name) throws NoSuchFieldException {
		for (Class<?> k = c; k != null; k = k.getSuperclass()) {
			try { return k.getDeclaredField(name); } catch (NoSuchFieldException e) { /* up */ }
		}
		throw new NoSuchFieldException(name);
	}

	private static void setInt(Object o, String name, int v) throws Exception {
		Field f = field(o.getClass(), name); f.setAccessible(true); f.setInt(o, v);
	}

	private static int getInt(Object o, String name) throws Exception {
		Field f = field(o.getClass(), name); f.setAccessible(true); return f.getInt(o);
	}

	@Test
	void replayMenuJumpsCursorAfterDelay() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		mode.playerInit(manager.engine[0], 0);
		GameEngine engine = manager.engine[0];
		engine.owner.replayMode = true;     // take the else (replay) branch
		setInt(mode, "menuTime", 60);       // >= 60 -> cursor jumps to 9

		mode.onSetting(engine, 0);

		assertEquals(9, getInt(mode, "menuCursor"), "replay menu jumps cursor to 9 at menuTime>=60");
	}

	@Test
	void replayMenuEarlyFrameEvaluatesElseIf() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		mode.playerInit(manager.engine[0], 0);
		GameEngine engine = manager.engine[0];
		engine.owner.replayMode = true;
		setInt(mode, "menuTime", 10);       // < 60 -> evaluates the else-if (line 227), stays -1

		mode.onSetting(engine, 0);

		assertEquals(-1, getInt(mode, "menuCursor"), "early replay frame leaves cursor at -1");
	}
}
