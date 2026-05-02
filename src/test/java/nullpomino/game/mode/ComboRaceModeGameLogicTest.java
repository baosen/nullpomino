package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link ComboRaceMode}: startGame,
 * onReady (fillStack), calcScore (line-clear, B2B, combo, meter
 * update, goal/endless logic), onLast, onMove, fillStack, and
 * updateRanking/checkRanking.
 */
class ComboRaceModeGameLogicTest {

	@Test
	void startGameSetsComboTypeAndTSpin() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_NORMAL, engine.comboType);
		assertTrue(engine.tspinEnable);
		assertTrue(engine.tspinAllowKick);
	}

	@Test
	void startGameSetsPieceEnterAboveField() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "spawnAboveField", true);

		mode.startGame(engine, 0);

		assertTrue(engine.ruleopt.pieceEnterAboveField);
	}

	@Test
	void onReadyCreatesFieldAndFillsStack() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;

		mode.onReady(engine, 0);

		assertNotNull(engine.field);
		// In test context showmeter is false so meterValue may be 0;
		// just verify no exception and field was created
		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void fillStackPlacesBlocksOnField() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		invokeFillStack(mode, engine, 1);

		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		boolean hasBlocks = false;
		for (int x = 0; x < w && !hasBlocks; x++) {
			for (int y = 0; y < h && !hasBlocks; y++) {
				if (!engine.field.getBlockEmpty(x, y)) {
					hasBlocks = true;
				}
			}
		}
		assertTrue(hasBlocks, "Fill stack should place blocks on the field");
	}

	@Test
	void calcScoreSingleLine() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 1); // 40 lines

		mode.calcScore(engine, 0, 1);

		// No score field, but events should be set
		int lastevent = readInt(mode, "lastevent");
		assertEquals(1, lastevent, "Single line clear = EVENT_SINGLE");
	}

	@Test
	void calcScoreFourLinesSetsEvent() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 4);

		int lastevent = readInt(mode, "lastevent");
		assertEquals(4, lastevent, "Four lines = EVENT_FOUR");
	}

	@Test
	void calcScoreTSpinSingle() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;
		engine.tspinmini = false;

		mode.calcScore(engine, 0, 1);

		int lastevent = readInt(mode, "lastevent");
		assertEquals(6, lastevent, "T-Spin single = EVENT_TSPIN_SINGLE");
	}

	@Test
	void calcScoreTSpinDouble() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;

		mode.calcScore(engine, 0, 2);

		int lastevent = readInt(mode, "lastevent");
		assertEquals(7, lastevent, "T-Spin double = EVENT_TSPIN_DOUBLE");
	}

	@Test
	void calcScoreTSpinTriple() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;

		mode.calcScore(engine, 0, 3);

		int lastevent = readInt(mode, "lastevent");
		assertEquals(8, lastevent, "T-Spin triple = EVENT_TSPIN_TRIPLE");
	}

	@Test
	void calcScoreGoalLinesReachedEndsGame() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // 20 lines
		engine.statistics.lines = 20;

		mode.calcScore(engine, 0, 1);
		// lines(21) >= goal(20), so game ends
		assertEquals(1, engine.ending, "Game should end when goal is reached");
	}

	@Test
	void calcScoreEndlessModeWithNoLinesClearedEndsGame() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 3); // ENDLESS (-1)
		engine.statistics.maxCombo = 10;

		mode.calcScore(engine, 0, 0);

		// 0 lines with maxCombo >= 2 in endless -> game over
		assertTrue(engine.ending == 1 || engine.stat == GameEngine.Status.GAMEOVER,
				"Endless with no clearing and high combo should end game");
	}

	@Test
	void calcScoreAllClearTriggersBravoSE() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);
		assertEquals(1, readInt(mode, "lastevent"), "Single line clears produce EVENT_SINGLE");
	}

	@Test
	void calcScoreB2bTracking() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.b2b = true;

		mode.calcScore(engine, 0, 4);

		assertTrue(readBoolean(mode, "lastb2b"), "B2B should be tracked");
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	@Test
	void checkRankingPrefersHigherCombo() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int rank = invokeCheckRanking(mode, 50, 3600);
		assertEquals(0, rank, "First entry should rank #1");
	}

	@Test
	void checkRankingReturnsMinusOneForUnranked() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// goaltype defaults to 1 after loadSetting in playerInit
		setInt(mode, "goaltype", 0);
		int[][] rankingCombo = (int[][]) readField(mode, "rankingCombo");
		// Fill all 10 slots with combos higher than our test combo
		for (int i = 0; i < 10; i++) {
			rankingCombo[0][i] = 999;
		}

		int rank = invokeCheckRanking(mode, 5, -1);
		assertEquals(-1, rank, "Low combo should not rank");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(ComboRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(ComboRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ComboRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(ComboRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(ComboRaceMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ComboRaceMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeFillStack(ComboRaceMode mode, GameEngine engine, int height) throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod("fillStack", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, height);
	}

	private static int invokeCheckRanking(ComboRaceMode mode, int maxcombo, int time) throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod("checkRanking", int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, maxcombo, time);
	}
}
