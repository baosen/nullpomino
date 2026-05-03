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
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link GradeMania3Mode}. Covers registry surface,
 * playerInit, settings round-trip, ranking persistence (dual-type for
 * normal/exam), calcScore scoring formula with grade point and medal
 * mechanics, onMove/onLast game logic, startGame, COOL/REGRET checks,
 * promotion/demotion exam, and roll point accumulation.
 */
class GradeMania3ModeGameLogicTest {

	// -----------------------------------------------------------------------
	// Registry surface
	// -----------------------------------------------------------------------

	@Test
	void getNameReturnsGradeMania3() {
		assertEquals("GRADE MANIA 3", new GradeMania3Mode().getName());
	}

	@Test
	void getPlayersReturnsOne() {
		assertEquals(1, new GradeMania3Mode().getPlayers());
	}

	@Test
	void getGameStyleIsTetromino() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new GradeMania3Mode().getGameStyle());
	}

	// -----------------------------------------------------------------------
	// playerInit
	// -----------------------------------------------------------------------

	@Test
	void playerInitResetsGradeStateAndEngineSettings() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "grade"));
		assertEquals(0, readInt(mode, "gradeBasicReal"));
		assertEquals(0, readInt(mode, "gradeBasicInternal"));
		assertEquals(0, readInt(mode, "gradeBasicPoint"));
		assertEquals(0, readInt(mode, "gradeBasicDecay"));
		assertEquals(0, readInt(mode, "internalLevel"));
		assertEquals(0, readInt(mode, "internalStartLevel"));
		assertEquals(0, readInt(mode, "harddropBonus"));
		assertEquals(0, readInt(mode, "comboValue"));
		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "rolltime"));
		assertEquals(0, readInt(mode, "rollclear"));
		assertEquals(false, readBoolean(mode, "rollstarted"));
		assertEquals(false, readBoolean(mode, "mrollFlag"));
		assertEquals(-1, readInt(mode, "rankingRank"));

		assertEquals(0, readInt(mode, "medalAC"));
		assertEquals(0, readInt(mode, "medalST"));
		assertEquals(0, readInt(mode, "medalSK"));
		assertEquals(0, readInt(mode, "medalCO"));

		assertEquals(false, readBoolean(mode, "cool"));
		assertEquals(0, readInt(mode, "coolcount"));
		assertEquals(false, readBoolean(mode, "previouscool"));
		assertEquals(false, readBoolean(mode, "coolchecked"));
		assertEquals(false, readBoolean(mode, "cooldisplayed"));

		assertEquals(0, readInt(mode, "promotionalExam"));
		assertEquals(0, readInt(mode, "qualifiedGrade"));
		assertEquals(0, readInt(mode, "demotionPoints"));
		assertEquals(false, readBoolean(mode, "promotionFlag"));
		assertEquals(false, readBoolean(mode, "demotionFlag"));

		assertFalse(engine.tspinEnable);
		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.FRAME_COLOR_BLUE, engine.framecolor);
		assertTrue(engine.bighalf);
		assertTrue(engine.bigmove);

		assertNotNull(readField(mode, "rankingGrade"));
		assertNotNull(readField(mode, "rankingLevel"));
		assertNotNull(readField(mode, "rankingTime"));
		assertNotNull(readField(mode, "rankingRollclear"));
		assertNotNull(readField(mode, "bestSectionTime"));
		assertNotNull(readField(mode, "gradeHistory"));
		assertNotNull(readField(mode, "coolsection"));
		assertNotNull(readField(mode, "regretsection"));

		int[][] rg = (int[][]) readField(mode, "rankingGrade");
		assertEquals(10, rg.length);
		assertEquals(2, rg[0].length);
	}

	// -----------------------------------------------------------------------
	// Settings persistence
	// -----------------------------------------------------------------------

	@Test
	void saveSettingAndLoadSettingRoundTrip() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 5);
		setBoolean(mode, "alwaysghost", true);
		setBoolean(mode, "lvstopse", false);
		setInt(mode, "lv500torikan", 30000);
		setInt(mode, "stcolor", 2);

		CustomProperties prop = new CustomProperties();
		mode.saveSetting(prop);

		assertEquals(5, prop.getProperty("grademania3.startlevel", -1));
		assertEquals(true, prop.getProperty("grademania3.alwaysghost", false));
		assertEquals(false, prop.getProperty("grademania3.lvstopse", true));
		assertEquals(30000, prop.getProperty("grademania3.lv500torikan", -1));
		assertEquals(2, prop.getProperty("grademania3.stcolor", -1));

		GradeMania3Mode loaded = new GradeMania3Mode();
		GameEngine le = freshEngine(loaded);
		loaded.playerInit(le, 0);
		loaded.loadSetting(prop);

		assertEquals(5, readInt(loaded, "startlevel"));
		assertEquals(true, readBoolean(loaded, "alwaysghost"));
		assertEquals(false, readBoolean(loaded, "lvstopse"));
		assertEquals(30000, readInt(loaded, "lv500torikan"));
		assertEquals(2, readInt(loaded, "stcolor"));
	}

	@Test
	void loadSettingUsesDefaultsWhenEmpty() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.loadSetting(new CustomProperties());

		assertEquals(0, readInt(mode, "startlevel"));
		assertEquals(false, readBoolean(mode, "alwaysghost"));
		assertEquals(false, readBoolean(mode, "always20g"));
		assertEquals(true, readBoolean(mode, "lvstopse"));
		assertEquals(25200, readInt(mode, "lv500torikan"));
	}

	// -----------------------------------------------------------------------
	// Ranking persistence (dual-type)
	// -----------------------------------------------------------------------

	@Test
	void rankingArraysInitialiseToZero() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] grades = (int[][]) readField(mode, "rankingGrade");
		int[][] levels = (int[][]) readField(mode, "rankingLevel");
		int[][] times = (int[][]) readField(mode, "rankingTime");
		int[][] rollclear = (int[][]) readField(mode, "rankingRollclear");
		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 2; j++) {
				assertEquals(0, grades[i][j]);
				assertEquals(0, levels[i][j]);
				assertEquals(0, times[i][j]);
				assertEquals(0, rollclear[i][j]);
			}
		}
	}

	@Test
	void saveRankingAndLoadRankingRoundTrip() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[][] grades = (int[][]) readField(mode, "rankingGrade");
		int[][] levels = (int[][]) readField(mode, "rankingLevel");
		int[][] times = (int[][]) readField(mode, "rankingTime");
		int[][] rollclear = (int[][]) readField(mode, "rankingRollclear");
		int[][] best = (int[][]) readField(mode, "bestSectionTime");

		for (int i = 0; i < 10; i++) {
			for (int j = 0; j < 2; j++) {
				grades[i][j] = 32 - i - j;
				levels[i][j] = 999 - i;
				times[i][j] = 30000 + i * 2000;
				rollclear[i][j] = (i + j) % 4;
				best[i][j] = 3500 + i * 150;
			}
		}
		int[] history = (int[]) readField(mode, "gradeHistory");
		for (int i = 0; i < history.length; i++) history[i] = 20 - i;
		setInt(mode, "qualifiedGrade", 12);
		setInt(mode, "demotionPoints", 5);

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "Standard");

		GradeMania3Mode loaded = new GradeMania3Mode();
		GameEngine le = freshEngine(loaded);
		loaded.playerInit(le, 0);
		invokeLoadRanking(loaded, prop, "Standard");

		int[][] loadedGrades = (int[][]) readField(loaded, "rankingGrade");
		int[][] loadedLevels = (int[][]) readField(loaded, "rankingLevel");
		int[][] loadedTimes = (int[][]) readField(loaded, "rankingTime");
		int[][] loadedRollclear = (int[][]) readField(loaded, "rankingRollclear");
		int[][] loadedBest = (int[][]) readField(loaded, "bestSectionTime");
		for (int i = 0; i < 10; i++) {
			assertArrayEquals(grades[i], loadedGrades[i]);
			assertArrayEquals(levels[i], loadedLevels[i]);
			assertArrayEquals(times[i], loadedTimes[i]);
			assertArrayEquals(rollclear[i], loadedRollclear[i]);
			assertArrayEquals(best[i], loadedBest[i]);
		}
		assertArrayEquals(history, (int[]) readField(loaded, "gradeHistory"));
		assertEquals(12, readInt(loaded, "qualifiedGrade"));
		assertEquals(5, readInt(loaded, "demotionPoints"));
	}

	// -----------------------------------------------------------------------
	// startGame
	// -----------------------------------------------------------------------

	@Test
	void startGameSetsLevelAndInternalLevel() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 3);
		setInt(mode, "internalStartLevel", 350);
		mode.startGame(engine, 0);

		assertEquals(300, engine.statistics.level);
		assertEquals(400, readInt(mode, "nextseclv"));
		assertEquals(350, readInt(mode, "internalLevel"));
	}

	@Test
	void startGameWithLevel10EntersRoll() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		setInt(mode, "startlevel", 10);
		mode.startGame(engine, 0);

		assertEquals(2, engine.ending);
		assertEquals(1, readInt(mode, "rollclear"));
	}

	@Test
	void startGameWithLevel11EntersMRoll() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		setInt(mode, "startlevel", 11);
		mode.startGame(engine, 0);

		assertEquals(2, engine.ending);
		assertTrue(readBoolean(mode, "mrollFlag"));
	}

	// -----------------------------------------------------------------------
	// calcScore formula
	// -----------------------------------------------------------------------

	@Test
	void calcScoreWithZeroLinesResetsComboValue() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "comboValue", 7);
		mode.calcScore(engine, 0, 0);

		assertEquals(1, readInt(mode, "comboValue"));
	}

	@Test
	void calcScoreWithSingleLineComputesGradePointsAndScore() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 100;
		engine.softdropFall = 0;
		engine.harddropFall = 0;
		engine.manualLock = false;
		setInt(mode, "comboValue", 1);
		engine.combo = 1;
		engine.ending = 0;

		mode.calcScore(engine, 0, 1);

		// Grade point: tableGradePoint[0][0]=10, combobonus=1.0, levelbonus=1+100/250=1
		// point = 10 * 1.0 * 1 = 10
		assertEquals(10, readInt(mode, "gradeBasicPoint"));

		// comboValue is auto-updated: 1 + (2*1) - 2 = 1
		// bravo = 2 (field empty in GM3)
		// level after levelUp = 101, section=1, tableLockDelay[1]=31
		// speedBonus = 31 - 0 = 31
		// Score: (((100+1)/4 + 0 + 0 + 0) * 1 * 1 + 31 + 101/2) * 2
		// = (25 + 31 + 50) * 2 = 106 * 2 = 212
		assertEquals(212, engine.statistics.score);
	}

	@Test
	void calcScoreBravoAppliesMultiplier2() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 100;
		engine.softdropFall = 0;
		engine.harddropFall = 0;
		engine.manualLock = false;
		setInt(mode, "comboValue", 1);
		engine.combo = 1;
		engine.ending = 0;

		// Field is empty -> bravo=2 instead of 1
		mode.calcScore(engine, 0, 1);

		// In GM3 bravo=2, so score = 106 * 2 = 212
		assertEquals(212, engine.statistics.score);
	}

	@Test
	void calcScoreThreeLinesBumpsToFourLevels() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 100;
		engine.combo = 1;
		engine.ending = 0;

		mode.calcScore(engine, 0, 3);

		// levelplus should be 4 for 3 lines
		assertEquals(104, engine.statistics.level);
	}

	@Test
	void calcScoreFourLinesBumpsToSixLevels() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 100;
		engine.combo = 1;
		engine.ending = 0;

		mode.calcScore(engine, 0, 4);

		// levelplus should be 6 for 4 lines
		assertEquals(106, engine.statistics.level);
	}

	@Test
	void calcScoreAddsRollPointsDuringEnding() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "comboValue", 1);
		engine.combo = 0;
		engine.ending = 2;
		setBoolean(mode, "mrollFlag", false);

		mode.calcScore(engine, 0, 4);

		// For mrollFlag==false, 4 lines = 0.26f points
		assertEquals(0.26f, (Float) readField(mode, "rollPoints"), 0.001f);
		assertEquals(0.26f, (Float) readField(mode, "rollPointsTotal"), 0.001f);
	}

	@Test
	void calcScoreMRollAddsMoreRollPoints() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "comboValue", 1);
		engine.combo = 0;
		engine.ending = 2;
		setBoolean(mode, "mrollFlag", true);

		mode.calcScore(engine, 0, 4);

		// For mrollFlag==true, 4 lines = 1.0f points which triggers grade up
		assertEquals(0.0f, (Float) readField(mode, "rollPoints"), 0.001f);
		assertEquals(1.0f, (Float) readField(mode, "rollPointsTotal"), 0.001f);
		assertTrue(readInt(mode, "grade") > 0);
	}

	// -----------------------------------------------------------------------
	// COOL check
	// -----------------------------------------------------------------------

	@Test
	void checkCoolDetectsCOOLWhenWithinTime() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 170; // section 1, 70% = level 170
		int[] st = (int[]) readField(mode, "sectiontime");
		st[1] = 2000; // Within tableTimeCool[1] = 3120

		invokeCheckCool(mode, engine);

		assertTrue(readBoolean(mode, "cool"));
		assertTrue(readBoolean(mode, "coolchecked"));
	}

	@Test
	void checkCoolMissesWhenOverTime() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 170; // section 1
		int[] st = (int[]) readField(mode, "sectiontime");
		st[1] = 5000; // Over tableTimeCool[1] = 3120

		invokeCheckCool(mode, engine);

		assertFalse(readBoolean(mode, "cool"));
	}

	// -----------------------------------------------------------------------
	// REGRET check
	// -----------------------------------------------------------------------

	@Test
	void checkRegretTriggersWhenSectionExceedsTimeLimit() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionlasttime", 5000); // Over tableTimeRegret[1] = 4500
		setInt(mode, "grade", 10);

		invokeCheckRegret(mode, engine, 100); // levelb=100 -> section 1

		// Grade should decrease and regret should display
		assertEquals(9, readInt(mode, "grade"));
		assertTrue(readInt(mode, "regretdispframe") > 0);
	}

	@Test
	void checkRegretDoesNotTriggerWhenWithinTime() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "sectionlasttime", 3000); // Under tableTimeRegret[1] = 4500
		setInt(mode, "grade", 10);

		invokeCheckRegret(mode, engine, 100);

		assertEquals(10, readInt(mode, "grade"));
	}

	// -----------------------------------------------------------------------
	// onMove
	// -----------------------------------------------------------------------

	@Test
	void onMoveLevelsUpDuringGame() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.statistics.level = 50;
		setInt(mode, "internalLevel", 50);
		setInt(mode, "nextseclv", 200);
		setBoolean(mode, "lvupflag", false);
		engine.ending = 0;
		engine.holdDisable = false;

		mode.onMove(engine, 0);

		assertTrue(engine.statistics.level > 50);
		assertTrue(readInt(mode, "internalLevel") > 50);
	}

	@Test
	void onMoveDecaysGradePoint() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.timerActive = true;
		setInt(mode, "gradeBasicPoint", 10);
		engine.combo = 0;
		engine.lockDelayNow = 0;

		mode.onMove(engine, 0);

		assertEquals(1, readInt(mode, "gradeBasicDecay"));
	}

	// -----------------------------------------------------------------------
	// onLast
	// -----------------------------------------------------------------------

	@Test
	void onLastDecrementsFlashAndScoreGetTime() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "gradeflash", 10);
		setInt(mode, "scgettime", 5);

		mode.onLast(engine, 0);

		assertEquals(9, readInt(mode, "gradeflash"));
		assertEquals(4, readInt(mode, "scgettime"));
	}

	@Test
	void onLastDecrementsCoolAndRegretDisplayFrames() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "cooldispframe", 10);
		setInt(mode, "regretdispframe", 15);

		mode.onLast(engine, 0);

		assertEquals(9, readInt(mode, "cooldispframe"));
		assertEquals(14, readInt(mode, "regretdispframe"));
	}

	@Test
	void onLastIncrementsSectionTime() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
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
	void onLastHandles15MinuteSpeedUpdate() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.statistics.time = 54000;

		mode.onLast(engine, 0);

		assertEquals(-1, engine.speed.gravity);
		assertEquals(2, engine.speed.are);
	}

	// -----------------------------------------------------------------------
	// afterHardDropFall
	// -----------------------------------------------------------------------

	@Test
	void afterHardDropFallUpdatesHarddropBonus() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.afterHardDropFall(engine, 0, 7);
		assertEquals(14, readInt(mode, "harddropBonus"));

		mode.afterHardDropFall(engine, 0, 3);
		assertEquals(14, readInt(mode, "harddropBonus"));
	}

	// -----------------------------------------------------------------------
	// onGameOver with exam logic
	// -----------------------------------------------------------------------

	@Test
	void onGameOverUpdatesDemotionPointsWithExamEnabled() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		setBoolean(mode, "enableexam", true);
		setInt(mode, "qualifiedGrade", 20);
		setInt(mode, "grade", 10); // qualifiedGrade - grade = 10 > 7, so demotion

		mode.onGameOver(engine, 0);

		assertEquals(3, readInt(mode, "demotionPoints")); // (20 - 10 - 7) = 3
	}

	@Test
	void onGameOverWithPromotionAndSufficientGradeUpdatesQualifiedGrade() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		setBoolean(mode, "enableexam", true);
		setBoolean(mode, "promotionFlag", true);
		setInt(mode, "promotionalExam", 22);
		setInt(mode, "grade", 25); // >= 22, so pass

		mode.onGameOver(engine, 0);

		assertEquals(22, readInt(mode, "qualifiedGrade"));
		assertEquals(0, readInt(mode, "demotionPoints"));
	}

	@Test
	void onGameOverWithDemotionAndInsufficientGradeUpdatesQualifiedGrade() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		setBoolean(mode, "enableexam", true);
		setBoolean(mode, "demotionFlag", true);
		setInt(mode, "demotionExamGrade", 15);
		setInt(mode, "grade", 10); // < 15, so fail

		mode.onGameOver(engine, 0);

		assertEquals(14, readInt(mode, "qualifiedGrade")); // 15 - 1
	}

	// -----------------------------------------------------------------------
	// setPromotionalGrade / updateGradeHistory
	// -----------------------------------------------------------------------

	@Test
	void updateGradeHistoryShiftsEntries() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] history = (int[]) readField(mode, "gradeHistory");
		history[0] = 5;
		history[1] = 10;

		invokeUpdateGradeHistory(mode, 20);

		assertEquals(20, history[0]);
		assertEquals(5, history[1]);
		assertEquals(10, history[2]);
	}

	// -----------------------------------------------------------------------
	// setSpeed
	// -----------------------------------------------------------------------

	@Test
	void setSpeedUsesGravityTable() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "internalLevel", 30);
		invokeSetSpeed(mode, engine);

		// Level 30 >= tableGravityChangeLevel[0]=30, so gravityindex advances to 1: value=6
		assertEquals(6, engine.speed.gravity);
	}

	@Test
	void setSpeedWith20gSetsNegativeGravity() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBoolean(mode, "always20g", true);
		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity);
	}

	// -----------------------------------------------------------------------
	// getGradeName
	// -----------------------------------------------------------------------

	@Test
	void getGradeNameReturnsCorrectName() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		Method m = GradeMania3Mode.class.getDeclaredMethod("getGradeName", int.class);
		m.setAccessible(true);

		assertEquals("9", m.invoke(mode, 0));
		assertEquals("S1", m.invoke(mode, 9));
		assertEquals("M1", m.invoke(mode, 18));
		assertEquals("GM", m.invoke(mode, 32));
		assertEquals("N/A", m.invoke(mode, 33));
		assertEquals("N/A", m.invoke(mode, -1));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GradeMania3Mode mode) {
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

	private static void invokeLoadRanking(GradeMania3Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("loadRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSaveRanking(GradeMania3Mode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}

	private static void invokeSetSpeed(GradeMania3Mode mode, GameEngine engine) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeCheckCool(GradeMania3Mode mode, GameEngine engine) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("checkCool", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeCheckRegret(GradeMania3Mode mode, GameEngine engine, int levelb) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("checkRegret", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, levelb);
	}

	private static void invokeUpdateGradeHistory(GradeMania3Mode mode, int gr) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("updateGradeHistory", int.class);
		m.setAccessible(true);
		m.invoke(mode, gr);
	}
}
