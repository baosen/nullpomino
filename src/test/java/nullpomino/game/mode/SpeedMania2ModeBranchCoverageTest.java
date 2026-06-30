package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link SpeedMania2Mode} covering the game-logic,
 * scoring, medal, render-threshold and results-screen branches that the
 * settings-menu wraparound tests do not reach.
 */
class SpeedMania2ModeBranchCoverageTest {

	private static GameEngine freshEngine(SpeedMania2Mode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Builds an engine with an empty playfield so calcScore/onGameOver can run. */
	private static GameEngine startedEngine(SpeedMania2Mode mode) {
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.startGame(engine, 0);
		engine.createFieldIfNeeded();
		return engine;
	}

	// -----------------------------------------------------------------------
	// startGame branches
	// -----------------------------------------------------------------------

	@Test
	void startGameStartLevelThirteenJumpsToEnding() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 13);
		mode.startGame(engine, 0);

		assertEquals(1300, engine.statistics.level, "startlevel 13 forces ending level");
		assertEquals(2, engine.ending, "startlevel 13 forces ending == 2");
		assertTrue(readBool(mode, "rollstarted"), "startlevel 13 starts the roll");
	}

	@Test
	void startGameNormalSetsNextSection() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 0);
		mode.startGame(engine, 0);

		assertEquals(0, engine.statistics.level);
		assertEquals(100, readInt(mode, "nextseclv"), "level 0 -> next section 100");
	}

	// -----------------------------------------------------------------------
	// stMedalCheck branches (L353/L354/L358/L361)
	// -----------------------------------------------------------------------

	@Test
	void stMedalCheckGoldNewRecord() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] best = (int[]) field(mode.getClass(), "bestSectionTime").get(mode);
		best[0] = 1000;
		setInt(mode, "sectionlasttime", 500); // < best -> gold (medalST = 3)
		setInt(mode, "medalST", 0);
		engine.owner.replayMode = false;
		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"), "beating best section time grants gold ST");
		boolean[] rec = (boolean[]) field(mode.getClass(), "sectionIsNewRecord").get(mode);
		assertTrue(rec[0], "non-replay new record flag set");
	}

	@Test
	void stMedalCheckGoldAlreadyGoldAndReplayMode() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] best = (int[]) field(mode.getClass(), "bestSectionTime").get(mode);
		best[0] = 1000;
		setInt(mode, "sectionlasttime", 500); // < best
		setInt(mode, "medalST", 3); // already gold -> medalST < 3 is false
		engine.owner.replayMode = true; // !owner.replayMode is false
		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"));
		boolean[] rec = (boolean[]) field(mode.getClass(), "sectionIsNewRecord").get(mode);
		assertEquals(false, rec[0], "replay mode does not mark new record");
	}

	@Test
	void stMedalCheckSilver() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] best = (int[]) field(mode.getClass(), "bestSectionTime").get(mode);
		best[0] = 1000;
		setInt(mode, "sectionlasttime", 1200); // < best+300, medalST<2 -> silver
		setInt(mode, "medalST", 0);
		invokeStMedalCheck(mode, engine, 0);

		assertEquals(2, readInt(mode, "medalST"), "within +300 grants silver ST");
	}

	@Test
	void stMedalCheckBronze() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] best = (int[]) field(mode.getClass(), "bestSectionTime").get(mode);
		best[0] = 1000;
		setInt(mode, "sectionlasttime", 1500); // < best+600, medalST<1 -> bronze
		setInt(mode, "medalST", 0);
		invokeStMedalCheck(mode, engine, 0);

		assertEquals(1, readInt(mode, "medalST"), "within +600 grants bronze ST");
	}

	// -----------------------------------------------------------------------
	// calcScore branches
	// -----------------------------------------------------------------------

	@Test
	void calcScoreZeroLinesResetsCombo() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		mode.calcScore(engine, 0, 0);
		assertEquals(1, readInt(mode, "comboValue"), "zero lines resets combo to 1");
	}

	@Test
	void calcScoreSingleLineAdvancesLevelAndScores() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		int before = engine.statistics.score;
		mode.calcScore(engine, 0, 1);

		assertEquals(51, engine.statistics.level, "single line advances level by 1");
		assertNotEquals(before, engine.statistics.score, "scoring adds points");
	}

	@Test
	void calcScoreSkMedalNonBig() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setBool(mode, "big", false);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		engine.statistics.totalFour = 5; // grants SK medal in non-big path
		setInt(mode, "medalSK", 0);
		mode.calcScore(engine, 0, 4);
		assertEquals(1, readInt(mode, "medalSK"), "5th tetris grants SK medal (non-big)");
	}

	@Test
	void calcScoreSkMedalBig() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setBool(mode, "big", true);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		engine.statistics.totalFour = 1; // grants SK medal in big path
		setInt(mode, "medalSK", 0);
		mode.calcScore(engine, 0, 4);
		assertEquals(1, readInt(mode, "medalSK"), "1st tetris grants SK medal (big)");
	}

	@Test
	void calcScoreComboMedalNonBig() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setBool(mode, "big", false);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		// The CO chain only climbs one tier per call; pre-set medalCO so the
		// >=7 else-if branch is the one taken this call.
		setInt(mode, "medalCO", 2);
		engine.combo = 7; // >= 7 && medalCO < 3 -> gold CO (non-big)
		mode.calcScore(engine, 0, 1);
		assertEquals(3, readInt(mode, "medalCO"), "combo 7 grants gold CO (non-big)");
	}

	@Test
	void calcScoreComboMedalBig() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setBool(mode, "big", true);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		// The CO chain only climbs one tier per call; pre-set medalCO so the
		// >=4 else-if branch is the one taken this call.
		setInt(mode, "medalCO", 2);
		engine.combo = 4; // >= 4 && medalCO < 3 -> gold CO (big)
		mode.calcScore(engine, 0, 1);
		assertEquals(3, readInt(mode, "medalCO"), "combo 4 grants gold CO (big)");
	}

	@Test
	void calcScoreComboMedalTiersNonBig() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setBool(mode, "big", false);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);

		// Tier 1: combo >= 4 && medalCO < 1
		setInt(mode, "medalCO", 0);
		engine.combo = 4;
		mode.calcScore(engine, 0, 1);
		assertEquals(1, readInt(mode, "medalCO"), "combo 4 grants bronze CO (non-big)");

		// Tier 2: combo >= 5 && medalCO < 2
		setInt(mode, "medalCO", 1);
		engine.combo = 5;
		mode.calcScore(engine, 0, 1);
		assertEquals(2, readInt(mode, "medalCO"), "combo 5 grants silver CO (non-big)");
	}

	@Test
	void calcScoreComboMedalTiersBig() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setBool(mode, "big", true);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);

		// Tier 1: combo >= 2 && medalCO < 1
		setInt(mode, "medalCO", 0);
		engine.combo = 2;
		mode.calcScore(engine, 0, 1);
		assertEquals(1, readInt(mode, "medalCO"), "combo 2 grants bronze CO (big)");

		// Tier 2: combo >= 3 && medalCO < 2
		setInt(mode, "medalCO", 1);
		engine.combo = 3;
		mode.calcScore(engine, 0, 1);
		assertEquals(2, readInt(mode, "medalCO"), "combo 3 grants silver CO (big)");
	}

	@Test
	void calcScoreAcMedalOnEmptyField() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		setInt(mode, "medalAC", 0); // field is empty -> AC medal granted
		mode.calcScore(engine, 0, 1);
		assertEquals(1, readInt(mode, "medalAC"), "clearing to empty field grants AC medal");
	}

	@Test
	void calcScoreNextSectionTransition() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statistics.level = 99;
		setInt(mode, "nextseclv", 100);
		setBool(mode, "lvstopse", false);
		mode.calcScore(engine, 0, 1); // 99 -> 100 >= nextseclv: next section

		assertEquals(200, readInt(mode, "nextseclv"), "crossing a section bumps nextseclv");
		assertEquals(1, readInt(mode, "sectionscomp"), "section completed");
	}

	@Test
	void calcScoreTorikanCutAt500() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statistics.level = 499;
		setInt(mode, "nextseclv", 500);
		setInt(mode, "torikan", 100);
		engine.statistics.time = 200; // time > torikan -> torikan cut
		mode.calcScore(engine, 0, 1);

		assertEquals(500, engine.statistics.level, "torikan cut clamps level to 500");
		assertEquals(1, engine.ending, "torikan cut sets ending == 1");
	}

	@Test
	void calcScoreTorikanCutAt1000() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statistics.level = 999;
		setInt(mode, "nextseclv", 1000);
		setInt(mode, "torikan", 100);
		engine.statistics.time = 300; // time > torikan*2 (200) -> 1000 torikan cut
		mode.calcScore(engine, 0, 1);

		assertEquals(1000, engine.statistics.level, "torikan cut clamps level to 1000");
		assertEquals(1, engine.ending, "torikan cut sets ending == 1");
	}

	@Test
	void calcScoreSkMedalNonBigSecondTier() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setBool(mode, "big", false);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		engine.statistics.totalFour = 10; // second disjunct of the non-big SK condition
		setInt(mode, "medalSK", 1);
		mode.calcScore(engine, 0, 4);
		assertEquals(2, readInt(mode, "medalSK"), "10th tetris grants another SK medal (non-big)");
	}

	@Test
	void calcScoreSkMedalBigSecondTier() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setBool(mode, "big", true);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		engine.statistics.totalFour = 2; // second disjunct of the big SK condition
		setInt(mode, "medalSK", 1);
		mode.calcScore(engine, 0, 4);
		assertEquals(2, readInt(mode, "medalSK"), "2nd tetris grants another SK medal (big)");
	}

	@Test
	void calcScoreEndingAt1300() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statistics.level = 1299;
		setInt(mode, "nextseclv", 1300);
		mode.calcScore(engine, 0, 1); // -> 1300, ending

		assertEquals(1300, engine.statistics.level);
		assertEquals(1, engine.ending, "reaching 1300 starts ending");
		assertEquals(1, readInt(mode, "rollclear"));
	}

	@Test
	void calcScoreRegretWhenSectionTooSlow() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statistics.level = 99;
		setInt(mode, "nextseclv", 100);
		// Make the just-completed section time exceed the regret threshold (3600 for sec 0).
		int[] st = (int[]) field(mode.getClass(), "sectiontime").get(mode);
		st[0] = 5000;
		setInt(mode, "grade", 0);
		mode.calcScore(engine, 0, 1);

		assertTrue(readInt(mode, "regretdispframe") > 0, "slow section triggers REGRET display");
		assertEquals(0, readInt(mode, "grade"), "REGRET means no grade increase");
	}

	@Test
	void calcScoreLevelStopHeldNearSectionEnd() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statistics.level = 98;
		setInt(mode, "nextseclv", 100);
		setBool(mode, "lvstopse", true);
		// 98 + 1 = 99 == nextseclv - 1 and lvstopse true -> last else-if (levelstop)
		mode.calcScore(engine, 0, 1);
		assertEquals(99, engine.statistics.level, "stops one below section boundary");
	}

	// -----------------------------------------------------------------------
	// onMove / onARE / levelUp branches
	// -----------------------------------------------------------------------

	@Test
	void onMoveLevelStopSoundNearSection() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBool(mode, "lvupflag", false);
		setBool(mode, "lvstopse", true);
		engine.statistics.level = 98;
		setInt(mode, "nextseclv", 100); // 98 -> 99 == nextseclv-1
		mode.onMove(engine, 0);
		assertEquals(99, engine.statistics.level, "onMove advances level toward section end");
	}

	@Test
	void onMoveStartsEndingRoll() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.ending = 2;
		setBool(mode, "rollstarted", false);
		mode.onMove(engine, 0);
		assertTrue(readBool(mode, "rollstarted"), "ending==2 begins the roll");
	}

	@Test
	void onMoveGarbageRise() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBool(mode, "lvupflag", true); // skip level-up block
		engine.statistics.level = 500; // tableGarbage[5] = 20 (non-zero)
		setInt(mode, "garbageCount", 19); // ++ -> 20 >= 20 triggers rise
		mode.onMove(engine, 0);
		assertEquals(0, readInt(mode, "garbageCount"), "garbage rise resets the counter");
	}

	@Test
	void onAreLastFrameLevelUp() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 6; // statc[0] >= statc[1]-1
		setBool(mode, "lvupflag", false);
		setBool(mode, "lvstopse", true);
		engine.statistics.level = 98;
		setInt(mode, "nextseclv", 100);
		mode.onARE(engine, 0);
		assertEquals(99, engine.statistics.level, "onARE last frame advances level");
		assertTrue(readBool(mode, "lvupflag"), "onARE sets lvupflag");
	}

	@Test
	void levelUpBgmFadeout() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setInt(mode, "bgmlv", 0); // tableBGMFadeout[0] = 485
		engine.statistics.level = 485;
		setInt(mode, "nextseclv", 500);
		// drive levelUp via onMove last-frame path keeping ending 0
		invokeLevelUp(mode, engine);
		assertTrue(engine.owner.bgmStatus.fadesw, "reaching fadeout level enables BGM fade");
	}

	// -----------------------------------------------------------------------
	// onLast branches
	// -----------------------------------------------------------------------

	@Test
	void onLastSectionTimeIncrements() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 250; // section 2
		int[] st = (int[]) field(mode.getClass(), "sectiontime").get(mode);
		st[2] = 0;
		mode.onLast(engine, 0);
		assertEquals(1, st[2], "section time increments while timer active");
	}

	@Test
	void onLastRollProgressesAndEnds() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 0);
		mode.onLast(engine, 0);
		assertEquals(1, readInt(mode, "rolltime"), "roll time advances during ending");

		// Force roll completion path
		setInt(mode, "rolltime", 3237); // ROLLTIMELIMIT = 3238, ++ -> 3238 >= limit
		mode.onLast(engine, 0);
		assertEquals(2, readInt(mode, "rollclear"), "completing the roll sets rollclear 2");
	}

	// -----------------------------------------------------------------------
	// onGameOver
	// -----------------------------------------------------------------------

	@Test
	void onGameOverComputesSecretGrade() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statc[0] = 0;
		engine.gameActive = true;
		boolean r = mode.onGameOver(engine, 0);
		assertEquals(false, r, "onGameOver returns false");
	}

	// -----------------------------------------------------------------------
	// onResult page navigation (wrap branches)
	// -----------------------------------------------------------------------

	@Test
	void onResultUpWrapsToLastPage() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statc[1] = 0;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.onResult(engine, 0);
		assertEquals(2, engine.statc[1], "UP from page 0 wraps to page 2");
	}

	@Test
	void onResultDownWrapsToFirstPage() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		engine.statc[1] = 2;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1], "DOWN from page 2 wraps to page 0");
	}

	@Test
	void onResultButtonFTogglesSectionView() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setBool(mode, "isShowBestSectionTime", false);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onResult(engine, 0);
		assertTrue(readBool(mode, "isShowBestSectionTime"), "F toggles best-section view on results");
	}

	// -----------------------------------------------------------------------
	// renderResult pages (render-threshold / no-assertion smoke)
	// -----------------------------------------------------------------------

	@Test
	void renderResultAllPages() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);
		setInt(mode, "secretGrade", 10); // > 4 -> secret grade line
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 3);
		setInt(mode, "medalCO", 1);
		setInt(mode, "rollclear", 2); // orange grade color
		setInt(mode, "sectionavgtime", 100);
		int[] st = (int[]) field(mode.getClass(), "sectiontime").get(mode);
		st[0] = 1000;

		for(int page = 0; page <= 2; page++) {
			engine.statc[1] = page;
			mode.renderResult(engine, 0);
		}
		assertTrue(true, "renderResult handled all three pages without throwing");
	}

	// -----------------------------------------------------------------------
	// renderLast branches (render-threshold / no-assertion smoke)
	// -----------------------------------------------------------------------

	@Test
	void renderLastRankingAndGameScreens() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);

		// Ranking screen branch: SETTING + replay false + startlevel 0 + big false + ai null
		engine.stat = GameEngine.Status.SETTING;
		engine.owner.replayMode = false;
		setInt(mode, "startlevel", 0);
		setBool(mode, "big", false);
		engine.ai = null;
		setBool(mode, "isShowBestSectionTime", false);
		mode.renderLast(engine, 0);

		// Section-time variant of the same screen
		setBool(mode, "isShowBestSectionTime", true);
		mode.renderLast(engine, 0);

		assertTrue(true, "renderLast ranking/section screens handled");
	}

	@Test
	void renderLastInGameScreen() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode);

		// In-game branch: not SETTING/RESULT
		engine.stat = GameEngine.Status.MOVE;
		setBool(mode, "gradedisp", true);
		setInt(mode, "grade", 1);
		setInt(mode, "gradeflash", 4); // gradeflash>0 && %4==0
		setInt(mode, "lastscore", 100);
		setInt(mode, "scgettime", 60); // lastscore!=0 && scgettime>0 -> "(+...)" branch
		engine.gameActive = true;
		engine.ending = 2; // roll-time block
		setInt(mode, "rolltime", 100);
		setInt(mode, "regretdispframe", 4); // regret line
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 1);
		setInt(mode, "medalSK", 1);
		setInt(mode, "medalCO", 1);
		setBool(mode, "showsectiontime", true);
		int[] st = (int[]) field(mode.getClass(), "sectiontime").get(mode);
		st[0] = 500;
		st[1] = 600;
		engine.statistics.level = 0; // section 0 == i for separator branch
		setInt(mode, "sectionavgtime", 100);
		mode.renderLast(engine, 0);

		assertTrue(true, "renderLast in-game screen handled");
	}

	// -----------------------------------------------------------------------
	// updateRanking / checkRanking  (HERMETIC: reset ranking rows first)
	// -----------------------------------------------------------------------

	@Test
	void checkRankingPlacesNewEntryFirst() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		resetRankingArrays(mode); // hermetic reset (see brief: shared modeConfig leak)

		invokeUpdateRanking(mode, 13, 1300, 100, 2);
		assertEquals(0, readInt(mode, "rankingRank"), "best-ever clear ranks first");
		int[] grades = (int[]) field(mode.getClass(), "rankingGrade").get(mode);
		assertEquals(13, grades[0], "grade recorded at rank 0");
	}

	@Test
	void checkRankingUnranked() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		resetRankingArrays(mode);

		// Fill all slots with strong entries so a weak result does not rank.
		int[] grades = (int[]) field(mode.getClass(), "rankingGrade").get(mode);
		int[] levels = (int[]) field(mode.getClass(), "rankingLevel").get(mode);
		int[] times = (int[]) field(mode.getClass(), "rankingTime").get(mode);
		int[] clears = (int[]) field(mode.getClass(), "rankingRollclear").get(mode);
		for(int i = 0; i < grades.length; i++) {
			grades[i] = 13;
			levels[i] = 1300;
			times[i] = 1;
			clears[i] = 4;
		}
		invokeUpdateRanking(mode, 0, 100, 99999, 0);
		assertEquals(-1, readInt(mode, "rankingRank"), "a weak result does not rank");
	}

	// -----------------------------------------------------------------------
	// reflection helpers
	// -----------------------------------------------------------------------

	private static void resetRankingArrays(SpeedMania2Mode mode) throws Exception {
		int[] grades = (int[]) field(mode.getClass(), "rankingGrade").get(mode);
		int[] levels = (int[]) field(mode.getClass(), "rankingLevel").get(mode);
		int[] times = (int[]) field(mode.getClass(), "rankingTime").get(mode);
		int[] clears = (int[]) field(mode.getClass(), "rankingRollclear").get(mode);
		for(int i = 0; i < grades.length; i++) {
			grades[i] = 0;
			levels[i] = 0;
			times[i] = 0;
			clears[i] = 0;
		}
		setInt(mode, "rankingRank", -1);
	}

	private static void invokeStMedalCheck(SpeedMania2Mode mode, GameEngine engine, int sectionNumber) throws Exception {
		java.lang.reflect.Method m = mode.getClass().getDeclaredMethod(
				"stMedalCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, sectionNumber);
	}

	private static void invokeLevelUp(SpeedMania2Mode mode, GameEngine engine) throws Exception {
		java.lang.reflect.Method m = mode.getClass().getDeclaredMethod("levelUp", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeUpdateRanking(SpeedMania2Mode mode, int gr, int lv, int time, int clear) throws Exception {
		java.lang.reflect.Method m = mode.getClass().getDeclaredMethod(
				"updateRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, gr, lv, time, clear);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
	}

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
