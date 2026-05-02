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
 * Covers game-logic methods in {@link MarathonPlusMode}: startGame
 * engine configuration, calcScore (level-based scoring with T-Spin,
 * B2B, combo, all-clear, BGM changes, meter), onLast (bonus level
 * timer), bonusLevelProc (outline visibility), setSpeed, setStartBgmlv,
 * and the bonus-level unlock path in onCustom / onEndingStart.
 */
class MarathonPlusModeGameLogicTest {

	@Test
	void startGameSetsLevelAndLinesForStartlevelOne() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 1);
		engine.readyDone = false;

		mode.startGame(engine, 0);

		assertEquals(1, engine.statistics.level);
		assertEquals(10, engine.statistics.lines,
				"lines = startlevel * 10 for startlevel < 20");
		assertEquals(1, engine.statistics.levelDispAdd);
	}

	@Test
	void startGameWithStartlevel20DoesNotSetLines() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 20);
		engine.readyDone = false;

		mode.startGame(engine, 0);

		assertEquals(20, engine.statistics.level);
		// Lines should be 0 for bonus game (since startlevel >= 20)
		assertEquals(0, engine.statistics.lines);
	}

	@Test
	void startGameSetsSpeedAndBgm() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 0);
		engine.readyDone = false;

		mode.startGame(engine, 0);

		// Level 0 speed: gravity=1, denominator=63
		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);
		assertEquals(12, engine.speed.lineDelay);
	}

	private void placeOneBlock(GameEngine engine) {
		// Prevent all-clear bonus from triggering
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
	}

	@Test
	void calcScoreSingleLineNoTSpin() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(100, engine.statistics.score,
				"1 line no T-Spin at level 0 = 100");
	}

	@Test
	void calcScoreFourLinesWithB2b() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.b2b = true;

		mode.calcScore(engine, 0, 4);

		assertEquals(1200, engine.statistics.score,
				"4 lines with B2B at level 0 = 1200");
	}

	@Test
	void calcScoreFourLinesNoB2b() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.b2b = false;

		mode.calcScore(engine, 0, 4);

		assertEquals(800, engine.statistics.score,
				"4 lines without B2B at level 0 = 800");
	}

	@Test
	void calcScoreTSpinDouble() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = true;

		mode.calcScore(engine, 0, 2);

		assertEquals(1200, engine.statistics.score,
				"T-Spin double at level 0 = 1200");
	}

	@Test
	void calcScoreTSpinTripleWithB2b() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.b2b = true;

		mode.calcScore(engine, 0, 3);

		assertEquals(2400, engine.statistics.score,
				"T-Spin triple B2B at level 0 = 2400");
	}

	@Test
	void calcScoreAllClearBonus() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(100 + 1800, engine.statistics.score,
				"1 line + all clear at level 0 = 1900");
	}

	@Test
	void calcScoreWithCombo() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "enableCombo", true);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.combo = 3;

		mode.calcScore(engine, 0, 1);

		assertEquals(100 + 100, engine.statistics.score,
				"1 line + combo 3 at level 0 = 100 + 2*50 = 200");
	}

	@Test
	void calcScoreTSpinZeroMini() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinmini = true;

		mode.calcScore(engine, 0, 0);

		assertEquals(100, engine.statistics.score,
				"T-Spin zero mini at level 0 = 100");
	}

	@Test
	void calcScoreTSpinZero() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinmini = false;

		mode.calcScore(engine, 0, 0);

		assertEquals(400, engine.statistics.score,
				"T-Spin zero at level 0 = 400");
	}

	@Test
	void calcScoreLevelUpTriggersAtBoundary() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.lines = 10; // GameEngine increments lines before calcScore
		setInt(mode, "startlevel", 0);

		mode.calcScore(engine, 0, 10);

		assertEquals(1, engine.statistics.level, "10 lines should trigger level up to 1");
	}

	@Test
	void calcScoreLevelUpToBonusLevelEndsGame() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 19;
		engine.statistics.lines = 200; // GameEngine increments lines before calcScore, needs >= (19+1)*10
		setInt(mode, "startlevel", 0);

		mode.calcScore(engine, 0, 10);

		assertEquals(20, engine.statistics.level, "Level should be 20 (bonus)");
		assertEquals(1, engine.ending, "Ending should be triggered at bonus level");
		assertFalse(engine.timerActive, "Timer should stop at bonus level");
	}

	@Test
	void calcScoreBonusLevelTracksLinesAndPieces() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 20; // already in bonus
		engine.statistics.lines = 200;
		engine.statistics.score = 0;
		setInt(mode, "bonusLines", 4); // set high enough to prevent flash reset (bonusPieceCount > bonusLines/4)

		mode.calcScore(engine, 0, 1);

		assertEquals(5, readInt(mode, "bonusLines"),
				"Bonus lines should increment");
		assertEquals(1, readInt(mode, "bonusPieceCount"),
				"Bonus piece count should increment");
	}

	@Test
	void calcScoreBonusFlashTriggersAtThreshold() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 20;
		setInt(mode, "bonusLines", 3);
		setInt(mode, "bonusPieceCount", 1);

		// bonusPieceCount(1) > bonusLines(3)/4 = 0 -> flash
		mode.calcScore(engine, 0, 1);

		assertEquals(30, readInt(mode, "bonusFlashNow"),
				"bonusFlashNow should be set to 30");
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);
		engine.statistics.level = 0;

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	@Test
	void onLastInBonusLevelIncrementsBonusTime() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.level = 20;
		engine.gameActive = true;
		engine.timerActive = true;
		engine.createFieldIfNeeded();
		setInt(mode, "bonusTime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "bonusTime"),
				"bonusTime should increment when in bonus level");
	}

	@Test
	void bonusLevelProcWithFlashShowsOutline() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "bonusFlashNow", 10);

		invokeBonusLevelProc(mode, engine);

		assertEquals(GameEngine.BLOCK_OUTLINE_NORMAL, engine.blockOutlineType,
				"Outline should be normal during flash");
	}

	@Test
	void bonusLevelProcWithoutFlashHidesBlocks() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "bonusFlashNow", 0);

		invokeBonusLevelProc(mode, engine);

		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, engine.blockOutlineType,
				"Outline should be none outside flash");
	}

	@Test
	void setSpeedForLevelZero() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(1, engine.speed.gravity);
		assertEquals(63, engine.speed.denominator);
	}

	@Test
	void setSpeedForLevelTwenty() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 20;

		invokeSetSpeed(mode, engine);

		// Bonus tier: table index 20 = gravity 1, denominator 4
		assertEquals(1, engine.speed.gravity);
		assertEquals(4, engine.speed.denominator);
	}

	@Test
	void setSpeedClampsAboveTable() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		engine.statistics.level = 100;

		invokeSetSpeed(mode, engine);

		// Last table entry: index 20
		assertEquals(1, engine.speed.gravity);
		assertEquals(4, engine.speed.denominator);
	}

	@Test
	void setStartBgmlvForStartlevelBelow20() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 0);
		engine.statistics.lines = 0;

		invokeSetStartBgmlv(mode, engine);

		assertEquals(0, readInt(mode, "bgmlv"),
				"bgmlv should start at 0 for startlevel 0");
	}

	@Test
	void setStartBgmlvForStartlevel20() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "startlevel", 20);

		invokeSetStartBgmlv(mode, engine);

		assertEquals(4, readInt(mode, "bgmlv"),
				"bgmlv should be 4 for startlevel >= 20");
	}

	@Test
	void onEndingStartTransitionsToCustom() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		boolean result = mode.onEndingStart(engine, 0);

		assertTrue(result, "onEndingStart should return true");
		assertEquals(GameEngine.Status.CUSTOM, engine.stat);
	}

	@Test
	void loadCoreSettingsRoundTrip() throws Exception {
		MarathonPlusMode source = new MarathonPlusMode();
		setInt(source, "startlevel", 7);
		setBoolean(source, "enableB2B", false);
		setBoolean(source, "enableCombo", false);
		setBoolean(source, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveCoreSettings(source, prop);

		assertEquals(7, prop.getProperty("marathonplus.startlevel", -1));
		assertEquals(false, prop.getProperty("marathonplus.enableB2B", true));
		assertEquals(false, prop.getProperty("marathonplus.enableCombo", true));

		MarathonPlusMode dest = new MarathonPlusMode();
		invokeLoadCoreSettings(dest, prop);

		assertEquals(7, readInt(dest, "startlevel"));
		assertFalse(readBoolean(dest, "enableB2B"));
		assertFalse(readBoolean(dest, "enableCombo"));
		assertTrue(readBoolean(dest, "big"));
	}

	@Test
	void updateRankingInsertsScore() throws Exception {
		MarathonPlusMode mode = new MarathonPlusMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 50000, 200, 3600, 0);

		assertEquals(0, readInt(mode, "rankingRank"), "First entry should rank #1");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(MarathonPlusMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(MarathonPlusMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(MarathonPlusMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(MarathonPlusMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(MarathonPlusMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(MarathonPlusMode mode, String name, boolean value) throws Exception {
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

	private static void invokeBonusLevelProc(MarathonPlusMode mode, GameEngine engine) throws Exception {
		Method m = MarathonPlusMode.class.getDeclaredMethod("bonusLevelProc", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeSetSpeed(MarathonPlusMode mode, GameEngine engine) throws Exception {
		Method m = MarathonPlusMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeSetStartBgmlv(MarathonPlusMode mode, GameEngine engine) throws Exception {
		Method m = MarathonPlusMode.class.getDeclaredMethod("setStartBgmlv", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeSaveCoreSettings(MarathonPlusMode mode, CustomProperties prop) throws Exception {
		Method m = AbstractMarathonMode.class.getDeclaredMethod("saveCoreSettings", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeLoadCoreSettings(MarathonPlusMode mode, CustomProperties prop) throws Exception {
		Method m = AbstractMarathonMode.class.getDeclaredMethod("loadCoreSettings", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(MarathonPlusMode mode, int sc, int li, int time, int type) throws Exception {
		Method m = AbstractMarathonMode.class.getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, li, time, type);
	}
}
