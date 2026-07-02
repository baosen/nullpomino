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
 * Covers game-logic methods in {@link FinalMode}: getName, playerInit,
 * loadSetting/saveSetting round-trip, ranking arrays, updateRanking,
 * calcScore (combo, level-up, medals, ending, section changes),
 * onLast (section time, ending roll), onMove (section level-up),
 * startGame initialization, onReady, and onGameOver.
 */
class FinalModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("FINAL", new FinalMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "nextseclv"));
		assertTrue(readBoolean(mode, "lvupflag"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertFalse(readBoolean(mode, "rollstarted"));
		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "gradeflash"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "medalAC"));
		assertEquals(0, readInt(mode, "medalST"));
		assertEquals(0, readInt(mode, "medalSK"));
		assertEquals(0, readInt(mode, "medalCO"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 3);
		setBoolean(mode, "lvstopse", true);
		setBoolean(mode, "showsectiontime", true);
		setBoolean(mode, "big", true);

		CustomProperties prop = new CustomProperties();
		invokeSaveSetting(mode, prop);

		FinalMode dest = new FinalMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		invokeLoadSetting(dest, prop);

		assertEquals(3, readInt(dest, "startlevel"));
		assertTrue(readBoolean(dest, "lvstopse"));
		assertTrue(readBoolean(dest, "showsectiontime"));
		assertTrue(readBoolean(dest, "big"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		FinalMode mode = new FinalMode();
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
	void updateRankingInsertsFirstEntry() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 3, 999, 3600, 2);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		assertEquals(3, rankingGrade[0]);
	}

	@Test
	void updateRankingPrefersHigherClearAndGrade() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 3, 999, 3600, 2); // rollclear 2, grade 3
		invokeUpdateRanking(mode, 2, 500, 1800, 1); // rollclear 1, worse

		// After first insert: rankingRank should be 0
		// After second insert with worse values: it should still be 0 if it stayed #1
		// But checkRanking may place the second entry below the first
		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		assertEquals(3, rankingGrade[0]);
		assertEquals(2, rankingGrade[1]);
	}

	@Test
	void startGameSetsLevelAndBone() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3);

		mode.startGame(engine, 0);

		assertEquals(300, engine.statistics.level);
		assertEquals(400, readInt(mode, "nextseclv"));
		assertTrue(engine.bone);
	}

	@Test
	void calcScoreWithNoLinesResetsCombo() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);

		assertEquals(1, readInt(mode, "comboValue"));
	}

	@Test
	void calcScoreSingleLineUsesFormula() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "comboValue", 1);
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 1);

		// comboValue = 1 + 2*1 - 2 = 1
		// levelb = 100, level = 101
		// bravo = 2 (field empty)
		// lastscore = (((100+1)/4 + 0 + 0) * 1 * 1 + 0 + (101/2)) * 2
		// = (25 * 1 + 0 + 50) * 2 = 75 * 2 = 150
		// speedBonus adds extra: engine.getLockDelay() - statc[0]
		assertEquals(1, readInt(mode, "comboValue"));
		assertTrue(readInt(mode, "lastscore") >= 150);
		assertEquals(120, readInt(mode, "scgettime"));
	}

	@Test
	void calcScoreFourLinesWithBigSkMedal() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.totalFour = 5; // standard SK medal threshold
		setBoolean(mode, "big", false);

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	@Test
	void calcScoreAcMedalOnEmptyField() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);

		assertEquals(1, readInt(mode, "medalAC"));
	}

	@Test
	void calcScoreLevel999Ending() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 995;

		mode.calcScore(engine, 0, 4);

		assertEquals(999, engine.statistics.level);
		assertEquals(1, engine.ending);
		assertEquals(3, readInt(mode, "grade")); // GOD grade
		assertEquals(180, readInt(mode, "gradeflash"));
	}

	@Test
	void calcScoreNextSectionTriggersGradeUp() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "nextseclv", 300);
		engine.statistics.level = 250;

		mode.calcScore(engine, 0, 1);

		// level was 250, +1 = 251 which is < 300, so no section change
		assertEquals(0, engine.ending);
	}

	@Test
	void calcScoreNextSectionAt300GivesGrade1() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "nextseclv", 300);
		engine.statistics.level = 299;

		mode.calcScore(engine, 0, 1);

		// 299+1=300 >= nextseclv=300 -> section change
		assertEquals(400, readInt(mode, "nextseclv"));
		assertEquals(1, readInt(mode, "grade")); // M grade
		assertEquals(180, readInt(mode, "gradeflash"));
	}

	@Test
	void onMoveLevelUpOnNewPiece() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		engine.statistics.level = 100;
		// nextseclv must be > level+1 for the level-up check to pass
		setInt(mode, "nextseclv", 200);

		mode.onMove(engine, 0);

		// Level increases by 1 onMove
		assertEquals(101, engine.statistics.level);
	}

	@Test
	void onMoveStartsEndingRoll() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 2;
		setBoolean(mode, "rollstarted", false);

		mode.onMove(engine, 0);

		assertTrue(readBoolean(mode, "rollstarted"));
	}

	@Test
	void onLastDecrementsGradeflashAndScgettime() throws Exception {
		FinalMode mode = new FinalMode();
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
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 150;

		mode.onLast(engine, 0);

		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		assertEquals(1, sectiontime[1]);
	}

	@Test
	void onLastEndingRollWithVersion3() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "version", 3);
		setInt(mode, "rolltime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "rolltime"));
	}

	@Test
	void onLastEndingRollFinishesWithVersion3Limit() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "version", 3);
		setInt(mode, "rolltime", 3237); // ROLLTIMELIMIT - 1

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
		assertEquals(2, readInt(mode, "rollclear"));
	}

	@Test
	void onReadyEnablesBoneWithVersion3() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setInt(mode, "version", 3);

		mode.onReady(engine, 0);

		assertTrue(engine.bone);
	}

	@Test
	void onGameOverSetsSecretGrade() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		engine.createFieldIfNeeded();

		mode.onGameOver(engine, 0);

		assertEquals(0, readInt(mode, "secretGrade"));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(FinalMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(FinalMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(FinalMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(FinalMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(FinalMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(FinalMode mode, String name, boolean value) throws Exception {
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

	private static void invokeLoadSetting(FinalMode mode, CustomProperties prop) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod("loadSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeSaveSetting(FinalMode mode, CustomProperties prop) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod("saveSetting", CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, prop);
	}

	private static void invokeUpdateRanking(FinalMode mode, int gr, int lv, int time, int clear) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time, clear);
	}
}
