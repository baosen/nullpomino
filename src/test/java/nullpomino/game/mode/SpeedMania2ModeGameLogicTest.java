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
 * Covers game-logic methods in {@link SpeedMania2Mode}: getName,
 * playerInit, loadSetting/saveSetting round-trip, ranking arrays,
 * updateRanking, calcScore (combo, level-up, medals, torikan, ending),
 * onLast (section time, grade flash, ending roll), onMove (level up,
 * garbage), startGame initialization, and onReady.
 */
class SpeedMania2ModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("SPEED MANIA 2", new SpeedMania2Mode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "nextseclv"));
		assertTrue(readBoolean(mode, "lvupflag"));
		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertFalse(engine.tspinEnable);
		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.FRAME_COLOR_RED, engine.framecolor);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 3);
		setBoolean(mode, "lvstopse", true);
		setBoolean(mode, "showsectiontime", true);
		setBoolean(mode, "big", true);
		setBoolean(mode, "gradedisp", true);
		setInt(mode, "torikan", 5000);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop, "DEFAULT");

		SpeedMania2Mode dest = new SpeedMania2Mode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		setInt(dest, "version", 2);
		invokeLoadSetting(dest, prop, "DEFAULT");

		assertEquals(3, readInt(dest, "startlevel"));
		assertTrue(readBoolean(dest, "lvstopse"));
		assertTrue(readBoolean(dest, "showsectiontime"));
		assertTrue(readBoolean(dest, "big"));
		assertTrue(readBoolean(dest, "gradedisp"));
		assertEquals(5000, readInt(dest, "torikan"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		int[] rankingRollclear = (int[]) readField(mode, "rankingRollclear");
		assertNotNull(rankingGrade);
		assertEquals(10, rankingGrade.length);
		assertEquals(10, rankingLevel.length);
		assertEquals(10, rankingTime.length);
		assertEquals(10, rankingRollclear.length);
	}

	@Test
	void updateRankingInsertsFirst() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 3, 500, 3600, 1);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		assertEquals(3, rankingGrade[0]);
		assertEquals(500, rankingLevel[0]);
	}

	@Test
	void startGameSetsStartLevel() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 2);

		mode.startGame(engine, 0);

		assertEquals(200, engine.statistics.level);
		assertEquals(300, readInt(mode, "nextseclv"));
	}

	@Test
	void startGameLevel1300StartsEnding() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 13);

		mode.startGame(engine, 0);

		assertEquals(1300, engine.statistics.level);
		assertEquals(2, engine.ending);
		assertTrue(readBoolean(mode, "rollstarted"));
		assertTrue(engine.big);
	}

	@Test
	void calcScoreWithNoLinesResetsCombo() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);

		assertEquals(1, readInt(mode, "comboValue"));
	}

	@Test
	void calcScoreSingleLineUsesCorrectFormula() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "comboValue", 1);
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 1);

		// comboValue = 1 + 2*1 - 2 = 1
		// levelb = 100, levelplus = 1, level = 101
		// bravo = 1 (field not empty)
		// lastscore = (((100+1)/4 + 0 + 0) * 1 * 1 + 0 + (101/2)) * 1
		// = (25 * 1 + 0 + 50) * 1 = 75
		assertEquals(1, readInt(mode, "comboValue"));
		assertEquals(120, readInt(mode, "scgettime"));
		assertTrue(engine.statistics.score >= 75);
	}

	@Test
	void calcScoreFourLinesTriggersSKMedal() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.totalFour = 5;

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	@Test
	void calcScoreAcMedalOnEmptyField() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);

		// field is empty -> AC medal
		assertEquals(1, readInt(mode, "medalAC"));
	}

	@Test
	void calcScoreReaches1300StartsEnding() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 1295;

		mode.calcScore(engine, 0, 4);

		// levelplus=6, 1295+6=1301 >= 1300 -> ending
		assertEquals(1300, engine.statistics.level);
		assertEquals(1, engine.ending);
		assertFalse(engine.timerActive);
	}

	@Test
	void onMoveLevelUpOnNewPiece() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		engine.statistics.level = 100;

		mode.onMove(engine, 0);

		assertTrue(engine.statistics.level >= 100);
	}

	@Test
	void onMoveStartsEnding() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 2;
		setBoolean(mode, "rollstarted", false);

		mode.onMove(engine, 0);

		assertTrue(readBoolean(mode, "rollstarted"));
		assertTrue(engine.big);
	}

	@Test
	void onLastDecrementsGradeflashAndScgettime() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gradeflash", 50);
		setInt(mode, "scgettime", 30);

		mode.onLast(engine, 0);

		assertEquals(49, readInt(mode, "gradeflash"));
		assertEquals(29, readInt(mode, "scgettime"));
	}

	@Test
	void onLastIncrementsSectionTime() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 250;

		mode.onLast(engine, 0);

		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		assertEquals(1, sectiontime[2]); // level 250 -> section 2
	}

	@Test
	void onLastEndingRollCountsTime() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
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
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		engine.createFieldIfNeeded();
		setInt(mode, "rolltime", 3237); // ROLLTIMELIMIT - 1

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	@Test
	void onReadyWithStartLevel11EnablesBone() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setInt(mode, "startlevel", 11);

		mode.onReady(engine, 0);

		assertTrue(engine.bone);
	}

	@Test
	void onReadyWithLowStartLevelDoesNotEnableBone() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setInt(mode, "startlevel", 5);

		mode.onReady(engine, 0);

		assertFalse(engine.bone);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(SpeedMania2Mode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(SpeedMania2Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(SpeedMania2Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(SpeedMania2Mode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(SpeedMania2Mode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(SpeedMania2Mode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadSetting(SpeedMania2Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod("loadSetting", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveSetting(SpeedMania2Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod("saveSetting", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeUpdateRanking(SpeedMania2Mode mode, int gr, int lv, int time, int clear) throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time, clear);
	}
}
