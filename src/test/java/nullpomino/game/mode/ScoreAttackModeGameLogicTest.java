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
 * Covers game-logic methods in {@link ScoreAttackMode}: getName,
 * playerInit, loadSetting/saveSetting round-trip, ranking arrays,
 * updateRanking, calcScore (6x multiplier formula, level-up, ending,
 * time bonus), onLast (scgettime, section time, ending roll),
 * onMove (level increment), startGame, levelUp, and afterHardDropFall.
 */
class ScoreAttackModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("SCORE ATTACK", new ScoreAttackMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "gravityindex"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(25, engine.speed.are);
		assertEquals(25, engine.speed.areLine);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(15, engine.speed.das);
		// playerInit calls loadSetting(owner.modeConfig) which defaults showsectiontime to false
		assertFalse(readBoolean(mode, "showsectiontime"));
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 1);
		setBoolean(mode, "alwaysghost", true);
		setBoolean(mode, "always20g", true);
		setBoolean(mode, "showsectiontime", true);
		setBoolean(mode, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		ScoreAttackMode dest = new ScoreAttackMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);

		assertEquals(1, readInt(dest, "startlevel"));
		assertTrue(readBoolean(dest, "alwaysghost"));
		assertTrue(readBoolean(dest, "always20g"));
		assertTrue(readBoolean(dest, "showsectiontime"));
		assertTrue(readBoolean(dest, "big"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		assertNotNull(rankingScore);
		assertEquals(10, rankingScore.length);
		assertEquals(10, rankingLevel.length);
		assertEquals(10, rankingTime.length);
	}

	@Test
	void updateRankingInsertsFirstEntry() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 50000, 300, 3600);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		assertEquals(50000, rankingScore[0]);
	}

	@Test
	void checkRankingReturnsMinusOneForUnranked() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		for (int i = 0; i < 10; i++) rankingScore[i] = 999999;

		int rank = invokeCheckRanking(mode, 100, 0, 0);
		assertEquals(-1, rank);
	}

	@Test
	void startGameSetsLevelAndBgm() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 1);

		mode.startGame(engine, 0);

		assertEquals(100, engine.statistics.level);
		assertEquals(200, readInt(mode, "nextseclv"));
		assertEquals(0, readInt(mode, "bgmlv"));
	}

	@Test
	void calcScoreWithNoLinesResetsCombo() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);

		assertEquals(1, readInt(mode, "comboValue"));
	}

	@Test
	void calcScoreSingleLineUses6xMultiplier() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "comboValue", 1);
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// comboValue = 1 + 2*1 - 2 = 1
		// levelb = 0, level = 1
		// bravo = 4 (field empty)
		// speedBonus = getLockDelay(30) - statc[0](0) = 30
		// lastscore = 6 * (((0+1)/4 + 0 + 0 + 0) * 1 * 1 * 4 + (1/2) + (30*7))
		// = 6 * (0*4 + 0 + 210) = 6 * 210 = 1260
		assertEquals(1260, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreFourLinesWithEmptyFieldGivesScore() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "comboValue", 1);
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 4);

		// comboValue = 1 + 2*4 - 2 = 7
		// levelb=100, level=104
		// bravo=4 (empty field)
		// speedBonus = 30-0 = 30
		// lastscore = 6 * (((100+4)/4) * 4 * 7 * 4 + (104/2) + (30*7))
		// = 6 * (26*4*7*4 + 52 + 210) = 6 * (2912+52+210) = 6*3174 = 19044
		assertEquals(7, readInt(mode, "comboValue"));
		assertTrue(engine.statistics.score > 0);
	}

	@Test
	void calcScoreLevel300EndingWithTimeBonus() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 296;
		engine.timerActive = true;
		engine.statistics.time = 3600; // 60 seconds

		mode.calcScore(engine, 0, 4);

		assertEquals(300, engine.statistics.level);
		assertEquals(2, engine.ending);
		assertFalse(engine.timerActive);
	}

	@Test
	void onMoveIncrementsLevel() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		engine.statistics.level = 50;

		mode.onMove(engine, 0);

		assertTrue(engine.statistics.level > 50);
	}

	@Test
	void onMoveLevelCapsAt299() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		engine.statistics.level = 299;

		mode.onMove(engine, 0);

		assertEquals(299, engine.statistics.level, "Level should cap at 299");
	}

	@Test
	void onLastDecrementsScgettime() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 50);

		mode.onLast(engine, 0);

		assertEquals(49, readInt(mode, "scgettime"));
	}

	@Test
	void onLastIncrementsSectionTime() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 50;

		mode.onLast(engine, 0);

		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		assertEquals(1, sectiontime[0]);
	}

	@Test
	void onLastEndingRollCountsTime() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
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
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 1955); // ROLLTIMELIMIT - 1

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	@Test
	void afterHardDropFallUpdatesHarddropBonus() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);

		mode.afterHardDropFall(engine, 0, 5);

		assertEquals(10, readInt(mode, "harddropBonus"));
	}

	@Test
	void onGameOverSetsSecretGrade() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		engine.createFieldIfNeeded();

		mode.onGameOver(engine, 0);

		assertEquals(0, readInt(mode, "secretGrade"));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(ScoreAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(ScoreAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ScoreAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(ScoreAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(ScoreAttackMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ScoreAttackMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadSetting(ScoreAttackMode mode, CustomProperties prop) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(ScoreAttackMode mode, CustomProperties prop) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(ScoreAttackMode mode, int sc, int lv, int time) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, sc, lv, time);
	}

	private static int invokeCheckRanking(ScoreAttackMode mode, int sc, int lv, int time) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("checkRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, lv, time);
	}
}
