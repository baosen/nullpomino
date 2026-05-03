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
 * Covers game-logic methods in {@link PreviewMode}: getName, playerInit,
 * and onGameOver.
 */
class PreviewModeGameLogicTest {

	@Test
	void getNameReturnsPreview() {
		assertEquals("PREVIEW", new PreviewMode().getName());
	}

	@Test
	void playerInitSilencesReceiverAndSkipsReadyGo() {
		PreviewMode mode = new PreviewMode();
		GameEngine engine = freshEngine(mode);

		engine.allowTextRenderByReceiver = true;
		engine.readyStart = 99;
		engine.readyEnd = 99;
		engine.goStart = 99;
		engine.goEnd = 99;

		mode.playerInit(engine, 0);

		assertFalse(engine.allowTextRenderByReceiver);
		assertEquals(-1, engine.readyStart);
		assertEquals(-1, engine.readyEnd);
		assertEquals(-1, engine.goStart);
		assertEquals(10, engine.goEnd);
	}

	@Test
	void onGameOverGivesALife() {
		PreviewMode mode = new PreviewMode();
		GameEngine engine = freshEngine(mode);
		engine.lives = 0;

		mode.onGameOver(engine, 0);

		assertEquals(1, engine.lives);
	}

	@Test
	void gameStyleIsTetromino() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new PreviewMode().getGameStyle());
	}

	@Test
	void getPlayersReturnsOne() {
		assertEquals(1, new PreviewMode().getPlayers());
	}

	@Test
	void isVSModeReturnsFalse() {
		assertFalse(new PreviewMode().isVSMode());
	}

	// ---- helpers ----

	private static GameEngine freshEngine(PreviewMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static Object readField(Object obj, String name) throws Exception {
		Class<?> c = obj.getClass();
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(obj); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
