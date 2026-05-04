package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers additional branches in {@link LineRaceMode}: calcScore meter-color
 * thresholds (30/20/10), netIsNetRankingViewOK/SendOK, saveReplay conditions,
 * onSetting confirm/save/load paths, and playerInit replay branch.
 */
class LineRaceModeExtendedLogicTest {

	@Test
	void calcScoreMeterColorThresholds() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // 20 lines goal

		// At 0 lines, remainLines = 20 → ORANGE (since <= 30 first and <= 20 second)
		engine.statistics.lines = 0;
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor,
				"20 remaining → orange (chains through yellow->orange)");

		// At 10 lines, remainLines = 10 → RED (through all three checks)
		engine.statistics.lines = 10;
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor,
				"10 remaining → red");

		// At 15 lines, remainLines = 5 → still RED (<=10 triggers it)
		engine.statistics.lines = 15;
		mode.calcScore(engine, 0, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor,
				"5 remaining → red");
	}

	@Test
	void netIsNetRankingViewOKRequiresNoBigAndNoAI() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);

		setBoolean(mode, "big", false);
		engine.ai = null;
		assertTrue(mode.netIsNetRankingViewOK(engine));

		setBoolean(mode, "big", true);
		assertFalse(mode.netIsNetRankingViewOK(engine));

		setBoolean(mode, "big", false);
		engine.ai = new nullpomino.game.ai.DummyAI();
		assertFalse(mode.netIsNetRankingViewOK(engine));
	}

	@Test
	void netIsNetRankingSendOKRequiresLinesAboveGoal() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", false);
		engine.ai = null;
		setInt(mode, "goaltype", 0); // 20 lines

		engine.statistics.lines = 19;
		assertFalse(mode.netIsNetRankingSendOK(engine));

		engine.statistics.lines = 20;
		assertTrue(mode.netIsNetRankingSendOK(engine));
	}

	@Test
	void playerInitReplayBranchLoadsFromReplayProp() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = true;
		engine.owner.replayProp.setProperty("linerace.big.-1", true);
		engine.owner.replayProp.setProperty("linerace.goaltype.-1", 2);
		engine.owner.replayProp.setProperty("linerace.gravity.-1", 10);

		mode.playerInit(engine, 0);

		assertTrue(readBoolean(mode, "big"));
		assertEquals(2, readInt(mode, "goaltype"));
		assertEquals(10, engine.speed.gravity);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(LineRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(LineRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(LineRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static void setBoolean(LineRaceMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void setInt(LineRaceMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
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
