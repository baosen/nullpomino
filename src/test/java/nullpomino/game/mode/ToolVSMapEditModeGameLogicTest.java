package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link ToolVSMapEditMode}: getName, modeInit,
 * playerInit, and basic field editing operations.
 */
class ToolVSMapEditModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("TOOL-VS MAP EDIT", new ToolVSMapEditMode().getName());
	}

	@Test
	void modeInitInitializesFields() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		mode.modeInit(new GameManager(new EventReceiver()));

		// propMap is deliberately set to null by modeInit; it is loaded on demand via loadAllMaps
		assertEquals(null, readField(mode, "propMap"));
		assertNotNull(readField(mode, "listFields"));
		assertEquals(0, readInt(mode, "nowMapSetID"));
		assertEquals(0, readInt(mode, "nowMapID"));
	}

	@Test
	void playerInitCreatesFieldAndLoadsMaps() throws Exception {
		ToolVSMapEditMode mode = new ToolVSMapEditMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertNotNull(engine.field);
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
		assertNotNull(readField(mode, "listFields"));
	}

	@Test
	void gameStyleIsTetromino() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new ToolVSMapEditMode().getGameStyle());
	}

	@Test
	void getPlayersReturnsOne() {
		assertEquals(1, new ToolVSMapEditMode().getPlayers());
	}

	@Test
	void isVSModeReturnsFalse() {
		assertFalse(new ToolVSMapEditMode().isVSMode());
	}

	// ---- helpers ----

	private static GameEngine freshEngine(ToolVSMapEditMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
