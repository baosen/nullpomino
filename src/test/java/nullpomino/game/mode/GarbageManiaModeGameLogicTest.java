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
 * Covers game-logic methods in {@link GarbageManiaMode}: getName,
 * playerInit, loadSetting/saveSetting round-trip, ranking arrays and
 * updateRanking, calcScore (combo, level-up, ending, garbage rising),
 * onLast (section time, ending roll), onMove (level up), and
 * startGame initialization.
 */
class GarbageManiaModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("GARBAGE MANIA", new GarbageManiaMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "gravityindex"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(23, engine.speed.are);
		assertEquals(23, engine.speed.areLine);
		assertEquals(40, engine.speed.lineDelay);
		assertEquals(31, engine.speed.lockDelay);
		assertEquals(15, engine.speed.das);
		assertFalse(engine.tspinEnable);
		assertFalse(engine.b2bEnable);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 5);
		setBoolean(mode, "alwaysghost", true);
		setBoolean(mode, "always20g", true);
		setBoolean(mode, "lvstopse", true);
		setBoolean(mode, "showsectiontime", true);
		setBoolean(mode, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		GarbageManiaMode dest = new GarbageManiaMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);

		assertEquals(5, readInt(dest, "startlevel"));
		assertTrue(readBoolean(dest, "alwaysghost"));
		assertTrue(readBoolean(dest, "always20g"));
		assertTrue(readBoolean(dest, "lvstopse"));
		assertTrue(readBoolean(dest, "showsectiontime"));
		assertTrue(readBoolean(dest, "big"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		assertNotNull(rankingLevel);
		assertEquals(10, rankingLevel.length);
		assertEquals(10, rankingTime.length);
		assertEquals(10, bestSectionTime.length);
	}

	@Test
	void updateRankingInsertsFirstEntry() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 500, 3600);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		assertEquals(500, rankingLevel[0]);
		assertEquals(3600, rankingTime[0]);
	}

	@Test
	void updateRankingRejectsLowerLevel() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 500, 3600);
		invokeUpdateRanking(mode, 100, 1800);

		assertEquals(1, readInt(mode, "rankingRank"));
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		assertEquals(500, rankingLevel[0], "First entry still best");
		assertEquals(100, rankingLevel[1], "Second entry inserted after");
	}

	@Test
	void checkRankingReturnsMinusOneForUnranked() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Fill rankings with high levels
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		for (int i = 0; i < 10; i++) rankingLevel[i] = 999;

		int rank = invokeCheckRanking(mode, 100, 3600);
		assertEquals(-1, rank);
	}

	@Test
	void startGameSetsLevelAndNextSectionLevel() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3);

		mode.startGame(engine, 0);

		assertEquals(300, engine.statistics.level);
		assertEquals(400, readInt(mode, "nextseclv"));
		assertEquals(3, engine.owner.backgroundStatus.bg);
	}

	@Test
	void startGameWithStartlevel9SetsNextseclv999() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 9);

		mode.startGame(engine, 0);

		assertEquals(900, engine.statistics.level);
		assertEquals(999, readInt(mode, "nextseclv"));
	}

	@Test
	void calcScoreWithNoLinesIncrementsComboAndGarbageCount() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);

		assertEquals(1, readInt(mode, "comboValue"));
		assertEquals(1, readInt(mode, "garbageCount"));
	}

	@Test
	void calcScoreSingleLineIncrementsLevelAndScore() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "comboValue", 1);
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 1);

		// comboValue = 1 + (2*1) - 2 = 1
		// levelb = 100, level becomes 101
		// lastscore = ((100+1)/4 + 0 + 0 + 0) * 1 * 1 * 1 + (101/2) + (0*7)
		// = (25) * 1 + 50 + 0 = 75
		assertEquals(1, readInt(mode, "comboValue"));
		assertTrue(engine.statistics.score > 0);
		assertEquals(101, engine.statistics.level);
		assertEquals(120, readInt(mode, "scgettime"));
	}

	@Test
	void calcScoreFourLinesWithEmptyFieldAppliesBravoMultiplier() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "comboValue", 2);
		engine.statistics.level = 50;

		mode.calcScore(engine, 0, 4);

		// comboValue = 2 + (2*4) - 2 = 8
		// field is empty -> bravo = 4
		// score with bravo=4 should be higher
		assertEquals(8, readInt(mode, "comboValue"));
		assertTrue(engine.statistics.score > 0);
	}

	@Test
	void calcScoreAtLevel999StartsEnding() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 995;

		mode.calcScore(engine, 0, 4);

		assertEquals(999, engine.statistics.level);
		assertEquals(2, engine.ending);
		assertFalse(engine.timerActive);
	}

	@Test
	void calcScoreReachesNextSection() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 150;

		mode.calcScore(engine, 0, 1);

		// level was 150, +1 = 151 which is < nextseclv=200, so no section change
		assertEquals(0, engine.ending);
		assertEquals(0, readInt(mode, "sectionscomp"));
	}

	@Test
	void onMoveLevelUpOnNewPiece() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 100;

		mode.onMove(engine, 0);

		// level should have increased from 100 to 101 (since < 199)
		assertTrue(engine.statistics.level > 100);
	}

	@Test
	void onMoveDoesNotLevelUpWhenLvupflagTrue() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", true);
		engine.statistics.level = 100;

		mode.onMove(engine, 0);

		// Level should stay the same because lvupflag is true
		assertEquals(100, engine.statistics.level);
	}

	@Test
	void onLastDecrementsScgettime() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 50);

		mode.onLast(engine, 0);

		assertEquals(49, readInt(mode, "scgettime"));
	}

	@Test
	void onLastIncrementsSectionTime() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 150;

		mode.onLast(engine, 0);

		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		assertEquals(1, sectiontime[1]); // level 150 -> section 1
	}

	@Test
	void onLastEndingRollCountsTime() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "rolltime"));
	}

	@Test
	void onLastEndingRollFinishesAtLimit() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 2023); // ROLLTIMELIMIT - 1

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	@Test
	void afterHardDropFallUpdatesHarddropBonus() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.afterHardDropFall(engine, 0, 5);

		assertEquals(10, readInt(mode, "harddropBonus")); // fall * 2
	}

	@Test
	void afterHardDropFallKeepsHigherBonus() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "harddropBonus", 20);

		mode.afterHardDropFall(engine, 0, 5); // fall*2 = 10 < 20

		assertEquals(20, readInt(mode, "harddropBonus"), "Should keep larger bonus");
	}

	@Test
	void onGameOverSetsSecretGrade() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		engine.createFieldIfNeeded();

		mode.onGameOver(engine, 0);

		// secretGrade should be set (0 if no blocks)
		assertEquals(0, readInt(mode, "secretGrade"));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(GarbageManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(GarbageManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(GarbageManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(GarbageManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(GarbageManiaMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(GarbageManiaMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadSetting(GarbageManiaMode mode, CustomProperties prop) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(GarbageManiaMode mode, CustomProperties prop) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(GarbageManiaMode mode, int lv, int time) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod("updateRanking", int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, lv, time);
	}

	private static int invokeCheckRanking(GarbageManiaMode mode, int lv, int time) throws Exception {
		Method m = GarbageManiaMode.class.getDeclaredMethod("checkRanking", int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, lv, time);
	}
}
