package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in RetroManiaMode.
 */
class RetroManiaModeFinalCoverageTest {

	@Test
	void onReadyStatcZeroReturnsFalse() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		boolean result = mode.onReady(engine, 0);
		assertFalse(result);
	}

	@Test
	void onReadyStatcNonZeroReturnsFalse() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 1;

		boolean result = mode.onReady(engine, 0);
		assertFalse(result);
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 5);

		mode.onLast(engine, 0);

		assertEquals(6, readInt(mode, "scgettime"));
	}

	@Test
	void renderSettingDisplaysMenu() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.renderSetting(engine, 0);
		// No exception
	}

	@Test
	void renderResultDisplaysStats() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.RESULT;
		engine.statistics.score = 50000;
		engine.statistics.lines = 15;
		engine.statistics.time = 1234;

		mode.renderResult(engine, 0);
		// No exception
	}

	@Test
	void calcScoreZeroLines() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);
		// No exception
	}

	@Test
	void startGameSetsEngineFields() throws Exception {
		RetroManiaMode mode = new RetroManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(RetroManiaMode mode) {
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

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
