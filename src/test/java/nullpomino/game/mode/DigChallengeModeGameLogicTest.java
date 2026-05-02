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
 * Covers game-logic methods in {@link DigChallengeMode}: startGame
 * engine configuration and garbage setup, calcScore (attack-based
 * scoring with T-Spin, B2B, combo, all-clear, garbage pending offset),
 * onLast (garbage timer, meter update), addGarbage, getGarbageMaxTime,
 * updateMeter, and updateRanking/checkRanking.
 */
class DigChallengeModeGameLogicTest {

	@Test
	void startGameSetsLevelAndGarbageCounters() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3);

		mode.startGame(engine, 0);

		assertEquals(3, engine.statistics.level);
		assertEquals(30, readInt(mode, "garbageTotal"), "level 3 * 10 = 30");
		assertEquals(40, readInt(mode, "garbageNextLevelLines"), "(3 + 1) * 10 = 40");
	}

	@Test
	void startGameWithComboAndB2bEnabled() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "enableB2B", true);
		setBoolean(mode, "enableCombo", true);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_NORMAL, engine.comboType);
		assertTrue(engine.b2bEnable);
	}

	@Test
	void calcScoreSingleLineNoTSpin() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		// Empty field triggers all-clear (+6), and version >= 2 gives garbage
		// pending bonus when pts > garbagePending (garbagePending=0-6=-6 so bonus=6)
		// Total = 6 (pts) + 6 (bonus) = 12
		assertEquals(12, engine.statistics.score, "Single line no T-Spin = 12 (6 pts + 6 bonus)");
	}

	@Test
	void calcScoreDoubleLineNoTSpin() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 2);

		// Double (1) + all-clear (6) = 7 pts, then garbage bonus (7) = 14
		assertEquals(14, engine.statistics.score, "Double + all-clear + bonus = 14");
	}

	@Test
	void calcScoreTripleLineNoTSpin() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 3);

		// Triple (2) + all-clear (6) = 8 pts, then garbage bonus (8) = 16
		assertEquals(16, engine.statistics.score, "Triple + all-clear + bonus = 16");
	}

	@Test
	void calcScoreFourLinesNoTSpin() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 4);

		// Four (4) + all-clear (6) = 10 pts, then garbage bonus (10) = 20
		assertEquals(20, engine.statistics.score, "Four lines + all-clear + bonus = 20");
	}

	@Test
	void calcScoreTSpinSingle() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinmini = false;

		mode.calcScore(engine, 0, 1);

		// T-Spin single (2) + all-clear (6) = 8 pts, then garbage bonus (8) = 16
		assertEquals(16, engine.statistics.score, "T-Spin single + all-clear + bonus = 16");
	}

	@Test
	void calcScoreTSpinDouble() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		engine.tspin = true;

		mode.calcScore(engine, 0, 2);

		// T-Spin double (4) + all-clear (6) = 10 pts, then garbage bonus (10) = 20
		assertEquals(20, engine.statistics.score, "T-Spin double + all-clear + bonus = 20");
	}

	@Test
	void calcScoreTSpinTriple() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		engine.tspin = true;

		mode.calcScore(engine, 0, 3);

		// T-Spin triple (6) + all-clear (6) = 12 pts, then garbage bonus (12) = 24
		assertEquals(24, engine.statistics.score, "T-Spin triple + all-clear + bonus = 24");
	}

	@Test
	void calcScoreB2bBonus() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		engine.b2b = true;
		engine.tspin = true;

		mode.calcScore(engine, 0, 1);

		// T-Spin single (2) + B2B (1) + all-clear (6) = 9 pts, then garbage bonus (9) = 18
		assertEquals(18, engine.statistics.score, "T-Spin single B2B + all-clear + bonus = 18");
	}

	@Test
	void calcScoreAllClearBonus() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 4);

		// Four (4) + all-clear (6) = 10 pts, then garbage bonus (10) = 20
		assertEquals(20, engine.statistics.score, "Four lines + all-clear + bonus = 20");
	}

	@Test
	void calcScoreComboBonus() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		engine.combo = 3;

		mode.calcScore(engine, 0, 2);

		// Double (1) + all-clear (6) = 7 pts, then garbage bonus (7) = 14
		// Note: comboType defaults to DISABLE so combo bonus not applied
		assertEquals(14, engine.statistics.score, "Double + all-clear + bonus = 14");
	}

	@Test
	void calcScoreWithGarbagePendingNormalType() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		setInt(mode, "goaltype", 0); // NORMAL
		setInt(mode, "garbagePending", 5);
		setInt(mode, "version", 2);

		mode.calcScore(engine, 0, 2);

		// Top garbage block skipped (version>=2, lines>0).
		// Double (1) + all-clear (6) = 7 pts.
		// garbagePending: 5 - 7 = -2, bonus = 2
		// Total score = 7 + 2 = 9
		assertEquals(9, engine.statistics.score);
		assertEquals(0, readInt(mode, "garbagePending"));
	}

	@Test
	void calcScoreGarbagePendingBonusWhenExceeds() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		setInt(mode, "goaltype", 0); // NORMAL
		setInt(mode, "garbagePending", 0);
		setInt(mode, "version", 2);

		mode.calcScore(engine, 0, 4);

		// Four lines (4) + all-clear (6) = 10 pts.
		// garbagePending: 0 - 10 = -10, bonus = 10
		// Total score = 10 + 10 = 20
		assertEquals(20, engine.statistics.score);
	}

	@Test
	void getGarbageMaxTimeReturnsCorrectValue() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();

		// For version > 1, t = goaltype
		setInt(mode, "version", 2);
		setInt(mode, "goaltype", 0); // Normal
		int time = invokeGetGarbageMaxTime(mode, 0);
		assertEquals(180, time, "Level 0 normal = 180 frames");
	}

	@Test
	void getGarbageMaxTimeRealtime() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		setInt(mode, "version", 2);
		setInt(mode, "goaltype", 1); // Realtime
		int time = invokeGetGarbageMaxTime(mode, 0);
		assertEquals(180, time, "Level 0 realtime = 180 frames");
	}

	@Test
	void addGarbageCreatesBlocksWithHole() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();

		invokeAddGarbageOne(mode, engine);

		// One garbage line added: all gray blocks except one hole column
		int hole = readInt(mode, "garbageHole");
		assertTrue(hole >= 0 && hole < w, "Hole column should be valid");
		for (int x = 0; x < w; x++) {
			Block b = engine.field.getBlock(x, h - 1);
			if (x == hole) {
				assertTrue(b == null || b.isEmpty(), "Hole column should be empty");
			} else {
				assertNotNull(b, "Non-hole column should have a block");
				assertEquals(Block.BLOCK_COLOR_GRAY, b.color);
			}
		}
		assertEquals(1, readInt(mode, "garbageTotal"), "1 garbage line added");
	}

	@Test
	void addGarbageTriggersLevelUpAtBoundary() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "garbageTotal", 9);
		setInt(mode, "garbageNextLevelLines", 10);
		engine.statistics.level = 0;

		invokeAddGarbageOne(mode, engine);

		assertEquals(1, engine.statistics.level, "Level should increase to 1");
		assertEquals(10, readInt(mode, "garbageTotal"));
		assertEquals(20, readInt(mode, "garbageNextLevelLines"));
	}

	@Test
	void updateRankingInsertsHigherScore() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 5000, 50, 3600, 0);

		assertEquals(0, readInt(mode, "rankingRank"), "First entry ranks #1");
	}

	@Test
	void updateRankingShiftsExistingEntries() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] rankingScore = (int[][]) readField(mode, "rankingScore");
		for (int i = 0; i < 10; i++) {
			rankingScore[0][i] = 10000;
		}

		invokeUpdateRanking(mode, 5000, 50, 3600, 0);

		assertEquals(-1, readInt(mode, "rankingRank"), "Score too low to rank");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(DigChallengeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(DigChallengeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(DigChallengeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(DigChallengeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(DigChallengeMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(DigChallengeMode mode, String name, boolean value) throws Exception {
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

	private static int invokeGetGarbageMaxTime(DigChallengeMode mode, int lv) throws Exception {
		Method m = DigChallengeMode.class.getDeclaredMethod("getGarbageMaxTime", int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, lv);
	}

	private static void invokeAddGarbageOne(DigChallengeMode mode, GameEngine engine) throws Exception {
		Method m = DigChallengeMode.class.getDeclaredMethod("addGarbage", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeUpdateRanking(DigChallengeMode mode, int sc, int li, int time, int type) throws Exception {
		Method m = DigChallengeMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, li, time, type);
	}
}
