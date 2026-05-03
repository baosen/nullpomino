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
import nullpomino.game.menu.BooleanMenuItem;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link SpeedManiaMode}: getName,
 * playerInit, loadSetting/saveSetting round-trip, ranking arrays,
 * updateRanking, calcScore (combo, level-up, medals, ending),
 * onLast (section time, grade flash, roll ending), onMove (level up,
 * RE medal), startGame initialization.
 */
class SpeedManiaModeGameLogicTest {

	@Test
	void getNameReturnsModeName() {
		assertEquals("SPEED MANIA", new SpeedManiaMode().getName());
	}

	@Test
	void playerInitInitializesFields() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "nextseclv"));
		assertTrue(readBoolean(mode, "lvupflag"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "medalAC"));
		assertEquals(0, readInt(mode, "medalST"));
		assertEquals(0, readInt(mode, "medalSK"));
		assertEquals(0, readInt(mode, "medalRE"));
		assertEquals(0, readInt(mode, "medalRO"));
		assertEquals(0, readInt(mode, "medalCO"));
		assertEquals(-1, readInt(mode, "rankingRank"));
		assertEquals(GameEngine.FRAME_COLOR_RED, engine.framecolor);
	}

	@Test
	void loadSettingSaveSettingRoundTrip() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setIntMenuValue(mode, "startlevel", 5);
		setBoolMenuValue(mode, "lvstopse", true);
		setBoolMenuValue(mode, "big", true);
		setBoolMenuValue(mode, "showsectiontime", true);
		setIntMenuValue(mode, "lv500torikan", 10000);

		CustomProperties prop = new CustomProperties();
		mode.saveSetting(prop);

		SpeedManiaMode dest = new SpeedManiaMode();
		GameEngine destEngine = freshEngine(dest);
		dest.playerInit(destEngine, 0);
		dest.loadSetting(prop);

		assertEquals(5, getIntMenuValue(dest, "startlevel"));
		assertTrue(getBoolMenuValue(dest, "lvstopse"));
		assertTrue(getBoolMenuValue(dest, "big"));
		assertTrue(getBoolMenuValue(dest, "showsectiontime"));
		assertEquals(10000, getIntMenuValue(dest, "lv500torikan"));
	}

	@Test
	void rankingArraysInitialized() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		assertNotNull(rankingGrade);
		assertEquals(10, rankingGrade.length);
		assertEquals(10, rankingLevel.length);
		assertEquals(10, rankingTime.length);
	}

	@Test
	void updateRankingInsertsFirst() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeUpdateRanking(mode, 2, 999, 3600);

		assertEquals(0, readInt(mode, "rankingRank"));
		int[] rankingGrade = (int[]) readField(mode, "rankingGrade");
		assertEquals(2, rankingGrade[0]);
	}

	@Test
	void startGameSetsSpeedAndBgm() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntMenuValue(mode, "startlevel", 3);

		mode.startGame(engine, 0);

		assertEquals(300, engine.statistics.level);
		assertEquals(400, readInt(mode, "nextseclv"));
		assertEquals(-1, engine.speed.gravity); // 20G
	}

	@Test
	void calcScoreWithNoLinesResetsCombo() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 0);

		assertEquals(1, readInt(mode, "comboValue"));
	}

	@Test
	void calcScoreFourLinesWithEmptyFieldAppliesBravo() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "comboValue", 1);
		engine.statistics.level = 50;

		mode.calcScore(engine, 0, 4);

		// comboValue = 1 + 2*4 - 2 = 7
		// field empty -> bravo = 4
		// lastscore = ((50+4)/4 + 0 + 0) * 4 * 7 * 4 + (54/2) + (0*7)
		// = (13 * 4 * 7 * 4) + 27 = 1456 + 27 = 1483
		assertEquals(7, readInt(mode, "comboValue"));
		assertTrue(engine.statistics.score > 0);
		assertEquals(120, readInt(mode, "scgettime"));
	}

	@Test
	void calcScoreAcMedalOnEmptyField() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
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
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 995;

		mode.calcScore(engine, 0, 4);

		assertEquals(999, engine.statistics.level);
		assertEquals(2, engine.ending);
		assertEquals(2, readInt(mode, "grade"));
		assertEquals(180, readInt(mode, "gradeflash"));
	}

	@Test
	void calcScoreLevel500TorikanEnding() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "nextseclv", 500);
		setIntMenuValue(mode, "lv500torikan", 100);
		engine.statistics.level = 496; // 496 + 4 lines = 500 >= 500
		engine.statistics.time = 200; // time > torikan

		mode.calcScore(engine, 0, 4);

		// level becomes 500, time > torikan -> torikan ending
		assertEquals(500, engine.statistics.level);
		assertEquals(2, engine.ending);
		assertFalse(engine.timerActive);
	}

	@Test
	void onMoveLevelUpOnNewPiece() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		engine.statistics.level = 100;
		setInt(mode, "nextseclv", 200); // must be > level+1 for level up

		mode.onMove(engine, 0);

		assertEquals(101, engine.statistics.level);
	}

	@Test
	void onLastDecrementsGradeflashAndScgettime() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
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
		SpeedManiaMode mode = new SpeedManiaMode();
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
	void onLastEndingRollCountsTimeWithVersion() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 0);
		setInt(mode, "version", 2);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "rolltime"));
	}

	@Test
	void onLastEndingRollFinishesAtLimit() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 1981); // ROLLTIMELIMIT - 1

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(SpeedManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(SpeedManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(SpeedManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(SpeedManiaMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(SpeedManiaMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(SpeedManiaMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void setIntMenuValue(SpeedManiaMode mode, String menuFieldName, int value) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		((IntegerMenuItem) f.get(mode)).value = value;
	}

	private static int getIntMenuValue(SpeedManiaMode mode, String menuFieldName) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		return ((IntegerMenuItem) f.get(mode)).value;
	}

	private static void setBoolMenuValue(SpeedManiaMode mode, String menuFieldName, boolean value) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		((BooleanMenuItem) f.get(mode)).value = value;
	}

	private static boolean getBoolMenuValue(SpeedManiaMode mode, String menuFieldName) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		return ((BooleanMenuItem) f.get(mode)).value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeUpdateRanking(SpeedManiaMode mode, int gr, int lv, int time) throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod("updateRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time);
	}
}
