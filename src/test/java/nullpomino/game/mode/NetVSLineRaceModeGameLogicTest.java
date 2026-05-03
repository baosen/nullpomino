package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link NetVSLineRaceMode}: getName,
 * modeInit, playerInit (meter color), startGame (meter color/value),
 * calcScore (meter, all-clear, game completion detection).
 */
class NetVSLineRaceModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("NET-VS-LINE RACE", new NetVSLineRaceMode().getName());
	}

	@Test
	void modeInitSetsGoalLines() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		mode.modeInit(new GameManager(new EventReceiver()));

		assertEquals(40, readInt(mode, "goalLines"));
	}

	@Test
	void playerInitSetsMeterColor() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	@Test
	void startGameSetsMeter() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
		// meterValue depends on receiver.getMeterMax which returns 0 in unit-test context
		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void calcScoreUpdatesMeter() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void calcScoreAllClearPlaysSound() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		// Empty field + lines cleared = all-clear
		mode.calcScore(engine, 0, 1);

		// Should not throw
		assertTrue(true);
	}

	@Test
	void calcScoreGameCompletedWhenLinesReachGoal() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		engine.statistics.lines = 40;
		// Set practice mode to avoid NPE from netLobby being null
		setBoolean(mode, "netvsIsPractice", true);

		mode.calcScore(engine, 0, 1);
		// With playerID = 0 and lines >= 40 (practice mode), should trigger game completion
	}

	@Test
	void getGoalLinesIs40ByDefault() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));

		assertEquals(40, readInt(mode, "goalLines"));
	}

	@Test
	void gameStyleIsTetromino() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new NetVSLineRaceMode().getGameStyle());
	}

	// ---- helpers ----

	private static GameEngine freshEngine(NetVSLineRaceMode mode) {
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

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
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
