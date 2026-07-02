package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link GradeManiaMode}. Covers registry surface,
 * playerInit, settings round-trip, ranking persistence, calcScore scoring
 * formula, onMove/onLast game logic, startGame initialization, and the
 * grade/section-time sub-system.
 */
class GradeManiaModeGameLogicTest {

	// -----------------------------------------------------------------------
	// Registry surface
	// -----------------------------------------------------------------------

	@Test
	void getNameReturnsGradeMania() {
		assertEquals("GRADE MANIA", new GradeManiaMode().getName());
	}

	@Test
	void getPlayersReturnsOne() {
		assertEquals(1, new GradeManiaMode().getPlayers());
	}

	@Test
	void getGameStyleIsTetromino() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new GradeManiaMode().getGameStyle());
	}

	// -----------------------------------------------------------------------
	// playerInit
	// -----------------------------------------------------------------------

	@Test
	void playerInitResetsGradeStateAndEngineSettings() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "lastGradeTime"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(false, readBoolean(mode, "gm300"));
		assertEquals(false, readBoolean(mode, "gm500"));
		assertEquals(0, readInt(mode, "secretGrade"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		assertFalse(engine.tspinEnable);
		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DOUBLE, engine.comboType);
		assertEquals(25, engine.speed.are);
		assertEquals(25, engine.speed.areLine);
		assertEquals(41, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(15, engine.speed.das);

		assertNotNull(readField(mode, "rankingGrade"));
		assertNotNull(readField(mode, "rankingLevel"));
		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "bestSectionTime"));
		assertEquals(10, ((int[]) readField(mode, "rankingGrade")).length);
	}

	// -----------------------------------------------------------------------
	// Settings round-trip
	// -----------------------------------------------------------------------

	@Test
	void saveSettingAndLoadSettingRoundTrip() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Set startlevel to 5
		setStartlevelValue(mode, 5);

		CustomProperties prop = new CustomProperties();
		mode.saveSetting(prop);

		// Verify saved values
		assertEquals(5, prop.getProperty("grademania.startlevel", -1));
		assertEquals(false, prop.getProperty("grademania.big", true));

		// Create a fresh mode and load
		GradeManiaMode loaded = new GradeManiaMode();
		GameEngine loadedEngine = freshEngine(loaded);
		loaded.playerInit(loadedEngine, 0);
		loaded.loadSetting(prop);

		assertEquals(5, ((IntegerMenuItem) readField(loaded, "startlevel")).value);
	}

	@Test
	void loadSettingUsesDefaultsWhenEmpty() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		mode.loadSetting(prop);

		assertEquals(0, ((IntegerMenuItem) readField(mode, "startlevel")).value);
	}

	// -----------------------------------------------------------------------
	// Ranking persistence
	// -----------------------------------------------------------------------

	@Test
	void rankingArraysInitialiseToZero() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] grades = (int[]) readField(mode, "rankingGrade");
		int[] levels = (int[]) readField(mode, "rankingLevel");
		int[] times = (int[]) readField(mode, "rankingTime");
		for (int i = 0; i < 10; i++) {
			assertEquals(0, grades[i]);
			assertEquals(0, levels[i]);
			assertEquals(0, times[i]);
		}
	}

	@Test
	void loadRankingReadsDefaultsWhenEmpty() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadRanking(mode, new CustomProperties(), "TestRule");

		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		for (int i = 0; i < 10; i++) {
			assertEquals(5400, bestSectionTime[i]);
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTrip() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] grades = (int[]) readField(mode, "rankingGrade");
		int[] levels = (int[]) readField(mode, "rankingLevel");
		int[] times = (int[]) readField(mode, "rankingTime");
		int[] best = (int[]) readField(mode, "bestSectionTime");

		for (int i = 0; i < 10; i++) {
			grades[i] = 18 - i;
			levels[i] = 999 - i * 10;
			times[i] = 100000 + i * 1000;
			best[i] = 5000 + i * 50;
		}

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		GradeManiaMode loaded = new GradeManiaMode();
		GameEngine le = freshEngine(loaded);
		loaded.playerInit(le, 0);
		invokeLoadRanking(loaded, prop, "Standard");

		assertArrayEquals(grades, (int[]) readField(loaded, "rankingGrade"));
		assertArrayEquals(levels, (int[]) readField(loaded, "rankingLevel"));
		assertArrayEquals(times, (int[]) readField(loaded, "rankingTime"));
		assertArrayEquals(best, (int[]) readField(loaded, "bestSectionTime"));
	}

	// -----------------------------------------------------------------------
	// startGame
	// -----------------------------------------------------------------------

	@Test
	void startGameSetsLevelFromStartlevel() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setStartlevelValue(mode, 3);
		mode.startGame(engine, 0);

		assertEquals(300, engine.statistics.level);
		assertEquals(400, readInt(mode, "nextseclv"));
	}

	@Test
	void startGameWithLevelZeroSetsNextSecLevelTo100() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setStartlevelValue(mode, 0);
		mode.startGame(engine, 0);

		assertEquals(0, engine.statistics.level);
		assertEquals(100, readInt(mode, "nextseclv"));
	}

	// -----------------------------------------------------------------------
	// calcScore formula
	// -----------------------------------------------------------------------

	@Test
	void calcScoreWithZeroLinesResetsComboValue() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "comboValue", 5);
		mode.calcScore(engine, 0, 0);
		assertEquals(1, readInt(mode, "comboValue"));
	}

	@Test
	void calcScoreWithSingleLineComputesCorrectScore() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 100;
		engine.softdropFall = 0;
		engine.harddropFall = 0;
		engine.manualLock = false;
		setInt(mode, "comboValue", 1);

		engine.statistics.score = 0;
		mode.calcScore(engine, 0, 1);

		// comboValue is auto-updated: 1 + (2*1) - 2 = 1
		// bravo = 4 because field is empty at start
		// Formula: lastscore = (((level + lines) / 4) + softdrop + harddrop + manuallock) * lines * comboValue * bravo
		// = ((100+1)/4 + 0 + 0 + 0) * 1 * 1 * 4 = 25 * 4 = 100
		assertEquals(100, engine.statistics.score);
		assertEquals(100, readInt(mode, "lastscore"));
		assertEquals(101, engine.statistics.level);
	}

	@Test
	void calcScoreWithFourLinesAndBravoAppliesBravoMultiplier() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		// Make field empty for bravo
		engine.statistics.level = 200;
		engine.softdropFall = 0;
		engine.harddropFall = 0;
		engine.manualLock = false;
		setInt(mode, "comboValue", 1);

		engine.statistics.score = 0;
		mode.calcScore(engine, 0, 4);

		// comboValue is auto-updated: 1 + (2*4) - 2 = 7
		// bravo = 4 (field empty)
		// Formula: ((200+4)/4 + 0 + 0 + 0) * 4 * 7 * 4 = 51 * 112 = 5712
		assertEquals(5712, engine.statistics.score);
	}

	@Test
	void calcScoreWithManualLockBonus() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 50;
		engine.softdropFall = 3;
		engine.harddropFall = 5;
		engine.manualLock = true;
		setInt(mode, "comboValue", 2);

		engine.statistics.score = 0;
		mode.calcScore(engine, 0, 2);

		// comboValue is auto-updated: 2 + (2*2) - 2 = 4
		// bravo = 4 (field empty)
		// lastscore = ((50+2)/4 + 3 + 5 + 1) * 2 * 4 * 4 = 22 * 32 = 704
		assertEquals(704, engine.statistics.score);
	}

	@Test
	void calcScoreTriggersGradeUp() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		// Set grade to 0, need 400 pts for next grade (tableGradeScore[0]=400)
		engine.statistics.level = 1000;
		engine.statistics.score = 380;
		engine.softdropFall = 0;
		engine.harddropFall = 0;
		engine.manualLock = false;
		setInt(mode, "comboValue", 1);

		mode.calcScore(engine, 0, 1);

		// Score should have gone up and grade should have increased
		assertTrue(readInt(mode, "grade") > 0);
		assertTrue(readInt(mode, "gradeflash") > 0);
	}

	@Test
	void calcScoreTriggersEndingAtLevel999() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 998;
		engine.statistics.score = 999999;
		engine.softdropFall = 0;
		engine.harddropFall = 0;
		engine.manualLock = false;
		setInt(mode, "comboValue", 1);
		// Set gm300 and gm500 to true for GM ending
		setBoolean(mode, "gm300", true);
		setBoolean(mode, "gm500", true);
		setInt(mode, "grade", 17);

		mode.calcScore(engine, 0, 1);

		assertEquals(999, engine.statistics.level);
		assertEquals(2, engine.ending); // Roll ending
	}

	// -----------------------------------------------------------------------
	// onMove
	// -----------------------------------------------------------------------

	@Test
	void onMoveLevelUpDuringGame() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 200);
		setBoolean(mode, "lvupflag", false);
		engine.ending = 0;
		engine.holdDisable = false;

		mode.onMove(engine, 0);

		// Level should increase since level < nextseclv - 1
		assertTrue(engine.statistics.level > 50);
	}

	@Test
	void onMoveDoesNotLevelWhenLvupflagIsTrue() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 200);
		setBoolean(mode, "lvupflag", true);
		engine.ending = 0;
		engine.holdDisable = false;

		mode.onMove(engine, 0);

		assertEquals(50, engine.statistics.level);
	}

	// -----------------------------------------------------------------------
	// onLast
	// -----------------------------------------------------------------------

	@Test
	void onLastDecrementsGradeFlash() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "gradeflash", 10);
		mode.onLast(engine, 0);
		assertEquals(9, readInt(mode, "gradeflash"));
	}

	@Test
	void onLastDecrementsScgettime() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "scgettime", 5);
		mode.onLast(engine, 0);
		assertEquals(4, readInt(mode, "scgettime"));
	}

	@Test
	void onLastIncrementsSectionTimeDuringActivePlay() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 150;

		int[] st = (int[]) readField(mode, "sectiontime");
		st[1] = 0;

		mode.onLast(engine, 0);

		assertEquals(1, st[1]);
	}

	@Test
	void onLastDoesNotIncrementSectionTimeDuringEnding() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.timerActive = true;
		engine.ending = 2;

		int[] st = (int[]) readField(mode, "sectiontime");
		int before = st[1];
		mode.onLast(engine, 0);
		assertEquals(before, st[1]);
	}

	@Test
	void onLastHandlesRollTimer() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 0);

		mode.onLast(engine, 0);
		assertEquals(1, readInt(mode, "rolltime"));
	}

	// -----------------------------------------------------------------------
	// setSpeed / levelUp
	// -----------------------------------------------------------------------

	@Test
	void setSpeedWithout20gUsesGravityTable() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.statistics.level = 30;
		invokeSetSpeed(mode, engine);

		// Level 30 >= tableGravityChangeLevel[0]=30, so gravityindex advances to 1: value=6
		assertEquals(6, engine.speed.gravity);
	}

	@Test
	void setSpeedWithAlways20gSetsNegativeGravity() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBooleanMenuItem(mode, "always20g", true);
		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity);
	}

	// -----------------------------------------------------------------------
	// setAverageSectionTime
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeComputesAverageWhenSectionsCompleted() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setStartlevelValue(mode, 0);
		setInt(mode, "sectionscomp", 3);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 300;
		st[1] = 600;
		st[2] = 900;

		invokeSetAverageSectionTime(mode);

		assertEquals(600, readInt(mode, "sectionavgtime"));
	}

	@Test
	void setAverageSectionTimeReturnsZeroWhenNoSections() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionscomp", 0);
		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// stNewRecordCheck
	// -----------------------------------------------------------------------

	@Test
	void stNewRecordCheckFlagsWhenSectionBeatsBest() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		st[3] = 2500;
		bst[3] = 3000;

		invokeStNewRecordCheck(mode, 3);

		assertTrue(((boolean[]) readField(mode, "sectionIsNewRecord"))[3]);
		assertTrue(readBoolean(mode, "sectionAnyNewRecord"));
	}

	@Test
	void stNewRecordCheckDoesNotFlagWhenSectionEqualsOrExceedsBest() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		st[0] = 3000;
		bst[0] = 3000;

		invokeStNewRecordCheck(mode, 0);

		assertFalse(((boolean[]) readField(mode, "sectionIsNewRecord"))[0]);
	}

	// -----------------------------------------------------------------------
	// onGameOver
	// -----------------------------------------------------------------------

	@Test
	void onGameOverSetsSecretGrade() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		mode.onGameOver(engine, 0);
		assertTrue(readInt(mode, "secretGrade") >= 0);
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GradeManiaMode mode) {
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

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
	}

	private static void setStartlevelValue(GradeManiaMode mode, int value) throws Exception {
		Field f = findField(mode.getClass(), "startlevel");
		f.setAccessible(true);
		IntegerMenuItem item = (IntegerMenuItem) f.get(mode);
		Field vf = findField(item.getClass(), "value");
		vf.setAccessible(true);
		vf.set(item, value);
	}

	private static void setBooleanMenuItem(GradeManiaMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		Object item = f.get(mode);
		Field vf = findField(item.getClass(), "value");
		vf.setAccessible(true);
		vf.set(item, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeLoadRanking(GradeManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod("loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(GradeManiaMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod("saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSetSpeed(GradeManiaMode mode, GameEngine engine) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeSetAverageSectionTime(GradeManiaMode mode) throws Exception {
		Method m = GradeManiaMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeStNewRecordCheck(GradeManiaMode mode, int sectionNumber) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("stNewRecordCheck", int.class);
		m.setAccessible(true);
		m.invoke(mode, sectionNumber);
	}
}
