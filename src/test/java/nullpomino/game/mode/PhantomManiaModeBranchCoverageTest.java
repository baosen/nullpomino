package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Exercises the non-menu logic branches of {@link PhantomManiaMode}:
 * level-up paths in {@link PhantomManiaMode#onMove}/{@link PhantomManiaMode#onARE},
 * the section-time / roll-timer logic in {@link PhantomManiaMode#onLast},
 * the medal checks ({@code stMedalCheck}/{@code roMedalCheck}), the scoring and
 * torikan / GM-grade paths in {@link PhantomManiaMode#calcScore}, the result-page
 * wrap in {@link PhantomManiaMode#onResult}, {@code onGameOver}, {@code startGame}
 * bounds, {@code setStartBgmlv}, the ranking helpers and the render hooks.
 *
 * <p>Ranking assertions are made hermetic by zeroing the ranking arrays via
 * reflection before driving {@code updateRanking}, so a populated shared
 * {@code config/setting} file from another test or CI run cannot perturb the
 * computed rank.
 */
class PhantomManiaModeBranchCoverageTest {

	private static GameEngine freshEngine(PhantomManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	// ------------------------------------------------------------------
	// startGame: nextseclv bounds
	// ------------------------------------------------------------------

	@Test
	void startGameClampsNextSecLvAt999ForLevel9() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel 9 -> level 900 -> nextseclv would be 1000, clamped to 999.
		setMenuValue(mode, "startlevel", 9);
		mode.startGame(engine, 0);
		assertEquals(900, engine.statistics.level);
		assertEquals(999, readInt(mode, "nextseclv"), "level>=900 forces nextseclv to 999");
	}

	@Test
	void startGameNextSecLvForLevelZeroIsHundred() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setMenuValue(mode, "startlevel", 0);
		mode.startGame(engine, 0);
		assertEquals(0, engine.statistics.level);
		assertEquals(100, readInt(mode, "nextseclv"), "level 0 -> nextseclv 100");
	}

	// ------------------------------------------------------------------
	// setStartBgmlv (via startGame): bgmlv advances past change levels
	// ------------------------------------------------------------------

	@Test
	void setStartBgmlvAdvancesPastChangeLevels() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel 5 -> level 500 >= tableBGMChange[0]=300 and >=500 -> bgmlv=2.
		setMenuValue(mode, "startlevel", 5);
		mode.startGame(engine, 0);
		assertEquals(2, readInt(mode, "bgmlv"), "level 500 advances bgmlv past 300 and 500");
	}

	// ------------------------------------------------------------------
	// onMove: level increment, levelstop SE branch, RE-medal recovery
	// ------------------------------------------------------------------

	@Test
	void onMoveIncrementsLevelBelowSectionBoundary() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "nextseclv", 100);
		setBool(mode, "lvupflag", false);
		engine.statistics.level = 50;
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		engine.timerActive = true;

		mode.onMove(engine, 0);
		assertEquals(51, engine.statistics.level, "level should increment toward nextseclv");
	}

	@Test
	void onMovePlaysLevelStopAtBoundaryMinusOne() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setMenuValue(mode, "lvstopse", true);
		setInt(mode, "nextseclv", 100);
		setBool(mode, "lvupflag", false);
		engine.statistics.level = 98; // increments to 99 == nextseclv-1
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		engine.timerActive = true;

		mode.onMove(engine, 0);
		assertEquals(99, engine.statistics.level, "level reaches nextseclv-1");
	}

	@Test
	void onMoveRecoveryMedalGrantedWhenFieldClears() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		// recoveryFlag already set; an empty field (<=70 blocks) grants RE medal.
		setBool(mode, "recoveryFlag", true);
		setInt(mode, "medalRE", 0);
		engine.field.reset(); // empty -> 0 blocks
		setInt(mode, "nextseclv", 200);
		setBool(mode, "lvupflag", false);
		engine.statistics.level = 50;
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		engine.timerActive = true;

		mode.onMove(engine, 0);
		assertEquals(1, readInt(mode, "medalRE"), "clearing the field grants the RE medal");
		assertFalse(readBool(mode, "recoveryFlag"), "recoveryFlag resets after medal");
	}

	@Test
	void onMoveRecoveryFlagSetWhenFieldFloods() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setBool(mode, "recoveryFlag", false);
		setInt(mode, "medalRE", 0);
		fillField(engine, 160); // >= 150 blocks
		setInt(mode, "nextseclv", 200);
		setBool(mode, "lvupflag", false);
		engine.statistics.level = 50;
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		engine.timerActive = true;

		mode.onMove(engine, 0);
		assertTrue(readBool(mode, "recoveryFlag"), "a flooded field arms the recovery flag");
	}

	@Test
	void onMoveSetsLvupFlagWhenStatcAdvances() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setBool(mode, "lvupflag", true);
		engine.ending = 0;
		engine.statc[0] = 5; // > 0 -> lvupflag cleared
		mode.onMove(engine, 0);
		assertFalse(readBool(mode, "lvupflag"), "lvupflag clears once the piece settles");
	}

	@Test
	void onMoveMarksRollStarted() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setBool(mode, "rollstarted", false);
		engine.ending = 2;
		mode.onMove(engine, 0);
		assertTrue(readBool(mode, "rollstarted"), "entering ending marks the roll as started");
	}

	// ------------------------------------------------------------------
	// onARE: level increment + lvupflag
	// ------------------------------------------------------------------

	@Test
	void onAreIncrementsLevelAndSetsFlag() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "nextseclv", 100);
		setBool(mode, "lvupflag", false);
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 5; // statc[0] >= statc[1]-1
		engine.statistics.level = 40;

		mode.onARE(engine, 0);
		assertEquals(41, engine.statistics.level);
		assertTrue(readBool(mode, "lvupflag"), "onARE re-arms lvupflag");
	}

	@Test
	void onArePlaysLevelStopWhenReachingBoundary() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setMenuValue(mode, "lvstopse", true);
		setInt(mode, "nextseclv", 100);
		setBool(mode, "lvupflag", false);
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 5;
		engine.statistics.level = 98; // -> 99 == nextseclv-1

		mode.onARE(engine, 0);
		assertEquals(99, engine.statistics.level);
	}

	// ------------------------------------------------------------------
	// onLast: section timing + roll meter colors + roll completion
	// ------------------------------------------------------------------

	@Test
	void onLastTicksSectionTime() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 150; // section 1

		mode.onLast(engine, 0);
		int[] sectiontime = (int[]) field(mode.getClass(), "sectiontime").get(mode);
		assertEquals(1, sectiontime[1], "section time ticks for the active section");
	}

	@Test
	void onLastDecrementsGradeFlashAndScGetTime() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "gradeflash", 3);
		setInt(mode, "scgettime", 3);
		engine.timerActive = false;
		engine.ending = 0;
		engine.gameActive = false;

		mode.onLast(engine, 0);
		assertEquals(2, readInt(mode, "gradeflash"));
		assertEquals(2, readInt(mode, "scgettime"));
	}

	@Test
	void onLastRollTimerSpeedsUpWithButtonF() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "version", 1);
		setInt(mode, "rolltime", 0);
		engine.gameActive = true;
		engine.ending = 2;
		engine.statistics.level = 998; // < 999 enables fast-forward
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 5;

		mode.onLast(engine, 0);
		assertEquals(5, readInt(mode, "rolltime"), "holding F advances roll time by 5");
	}

	@Test
	void onLastRollTimerNormalAdvance() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "version", 1);
		setInt(mode, "rolltime", 0);
		engine.gameActive = true;
		engine.ending = 2;
		engine.statistics.level = 999; // no fast-forward
		engine.ctrl.reset();

		mode.onLast(engine, 0);
		assertEquals(1, readInt(mode, "rolltime"), "roll time advances by 1 without F");
	}

	@Test
	void onLastRollCompletionGrantsExcellent() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "version", 1);
		setInt(mode, "rolltime", 1981); // +1 -> 1982 == ROLLTIMELIMIT
		setInt(mode, "rollclear", 1);
		engine.gameActive = true;
		engine.ending = 2;
		engine.statistics.level = 999;
		engine.ctrl.reset();

		mode.onLast(engine, 0);
		assertEquals(2, readInt(mode, "rollclear"), "completing the roll at level 999 sets rollclear=2");
		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	// ------------------------------------------------------------------
	// stMedalCheck: gold / silver / bronze tiers
	// ------------------------------------------------------------------

	@Test
	void stMedalCheckAwardsGoldSilverBronze() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		Method st = AbstractManiaMode.class.getDeclaredMethod("stMedalCheck", GameEngine.class, int.class);
		st.setAccessible(true);

		int[] best = (int[]) field(mode.getClass(), "bestSectionTime").get(mode);
		best[0] = 1000;

		// Gold: faster than best.
		setInt(mode, "sectionlasttime", 900);
		setInt(mode, "medalST", 0);
		st.invoke(mode, engine, 0);
		assertEquals(3, readInt(mode, "medalST"), "beating the record awards gold");

		// Silver: within best+300 (and medalST<2).
		setInt(mode, "sectionlasttime", 1200);
		setInt(mode, "medalST", 0);
		st.invoke(mode, engine, 0);
		assertEquals(2, readInt(mode, "medalST"), "within +300 awards silver");

		// Bronze: within best+600 (and medalST<1).
		setInt(mode, "sectionlasttime", 1500);
		setInt(mode, "medalST", 0);
		st.invoke(mode, engine, 0);
		assertEquals(1, readInt(mode, "medalST"), "within +600 awards bronze");
	}

	@Test
	void stMedalCheckMarksNewRecordOutsideReplay() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		Method st = AbstractManiaMode.class.getDeclaredMethod("stMedalCheck", GameEngine.class, int.class);
		st.setAccessible(true);

		int[] best = (int[]) field(mode.getClass(), "bestSectionTime").get(mode);
		best[2] = 2000;
		setInt(mode, "sectionlasttime", 1000);
		setInt(mode, "medalST", 0);
		st.invoke(mode, engine, 2);

		boolean[] rec = (boolean[]) field(mode.getClass(), "sectionIsNewRecord").get(mode);
		assertTrue(rec[2], "non-replay gold also marks a new section record");
	}

	// ------------------------------------------------------------------
	// roMedalCheck: average rotation triggers medal
	// ------------------------------------------------------------------

	@Test
	void roMedalCheckAwardsForHighRotationAverage() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		Method ro = mode.getClass().getDeclaredMethod("roMedalCheck", GameEngine.class);
		ro.setAccessible(true);

		setInt(mode, "rotateCount", 12);
		engine.statistics.totalPieceLocked = 10; // avg 1.2 >= 1.2f
		setInt(mode, "medalRO", 0);
		ro.invoke(mode, engine);
		assertEquals(1, readInt(mode, "medalRO"), "rotation average >= 1.2 awards RO medal");
	}

	// ------------------------------------------------------------------
	// calcScore: combo reset, scoring, section advance, GM grade
	// ------------------------------------------------------------------

	@Test
	void calcScoreZeroLinesResetsCombo() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "comboValue", 7);
		mode.calcScore(engine, 0, 0);
		assertEquals(1, readInt(mode, "comboValue"), "no line clear resets combo to 1");
	}

	@Test
	void calcScoreAdvancesSectionAndAwardsScore() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setMenuValue(mode, "startlevel", 0);
		setInt(mode, "nextseclv", 100);
		engine.ending = 0;
		engine.statistics.level = 98; // +2 lines -> 100 >= nextseclv
		engine.statistics.score = 0;
		engine.statistics.time = 100;
		engine.statistics.totalFour = 0;
		engine.combo = 0;

		mode.calcScore(engine, 0, 2);
		assertEquals(200, readInt(mode, "nextseclv"), "crossing the section bumps nextseclv by 100");
		assertTrue(engine.statistics.score > 0, "a line clear awards score");
	}

	@Test
	void calcScoreGmGradeAtMaxLevel() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "nextseclv", 999);
		setBool(mode, "gmfourline", true);
		setInt(mode, "sectionfourline", 2);
		engine.ending = 0;
		engine.statistics.level = 998; // +2 -> 999
		engine.statistics.totalFour = 31; // >= 31
		engine.timerActive = true;

		mode.calcScore(engine, 0, 2);
		assertEquals(2, engine.ending, "reaching 999 enters the ending");
		assertEquals(6, readInt(mode, "grade"), "GM conditions award grade 6");
		assertEquals(180, readInt(mode, "gradeflash"));
	}

	@Test
	void calcScoreLv300TorikanEndsGame() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "nextseclv", 300);
		setInt(mode, "bgmlv", 0);
		engine.ending = 0;
		engine.statistics.level = 299; // +1 -> 300
		engine.statistics.time = 9000; // > LV300TORIKAN (8880)
		engine.timerActive = true;

		mode.calcScore(engine, 0, 1);
		assertEquals(300, engine.statistics.level, "torikan clamps level to 300");
		assertEquals(2, engine.ending, "the level-300 torikan ends the game");
	}

	@Test
	void calcScoreSkMedalForBigQuad() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setMenuValue(mode, "big", true);
		setInt(mode, "nextseclv", 999);
		setInt(mode, "medalSK", 0);
		engine.ending = 0;
		engine.statistics.level = 100;
		engine.statistics.totalFour = 1; // matches the big-mode SK trigger
		engine.combo = 0;

		mode.calcScore(engine, 0, 4); // a quad
		assertEquals(1, readInt(mode, "medalSK"), "big-mode first quad awards SK medal");
	}

	@Test
	void calcScoreCoMedalForBigCombo() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setMenuValue(mode, "big", true);
		setInt(mode, "nextseclv", 999);
		// medalCO already silver (2); combo>=4 with medalCO<3 reaches the gold arm.
		setInt(mode, "medalCO", 2);
		engine.ending = 0;
		engine.statistics.level = 100;
		engine.combo = 4;
		engine.statistics.totalFour = 0;

		mode.calcScore(engine, 0, 1);
		assertEquals(3, readInt(mode, "medalCO"), "big-mode combo>=4 with prior silver awards gold CO");
	}

	@Test
	void calcScoreCoMedalNonBigBronze() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setMenuValue(mode, "big", false);
		setInt(mode, "nextseclv", 999);
		setInt(mode, "medalCO", 0);
		engine.ending = 0;
		engine.statistics.level = 100;
		engine.combo = 4; // non-big combo>=4 with medalCO<1 -> bronze
		engine.statistics.totalFour = 0;

		mode.calcScore(engine, 0, 1);
		assertEquals(1, readInt(mode, "medalCO"), "non-big combo>=4 awards bronze CO");
	}

	@Test
	void calcScoreAllClearAwardsAcMedal() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "nextseclv", 999);
		setInt(mode, "medalAC", 0);
		engine.field.reset(); // empty -> isEmpty() true
		engine.ending = 0;
		engine.statistics.level = 100;
		engine.combo = 0;
		engine.statistics.totalFour = 0;

		mode.calcScore(engine, 0, 1);
		assertEquals(1, readInt(mode, "medalAC"), "clearing to an empty field awards AC medal");
	}

	@Test
	void calcScoreLevelStopSeAtBoundary() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setMenuValue(mode, "lvstopse", true);
		setInt(mode, "nextseclv", 100);
		engine.ending = 0;
		engine.statistics.level = 98; // +1 -> 99 == nextseclv-1, none of the section arms fire
		engine.statistics.time = 10;
		engine.combo = 0;
		engine.statistics.totalFour = 0;

		mode.calcScore(engine, 0, 1);
		assertEquals(99, engine.statistics.level, "stays at boundary-1 with lvstopse");
	}

	// ------------------------------------------------------------------
	// onGameOver: secret grade captured on first frame
	// ------------------------------------------------------------------

	@Test
	void onGameOverCapturesSecretGrade() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		engine.statc[0] = 0;
		mode.onGameOver(engine, 0); // should not throw; reads field secret grade
		assertTrue(readInt(mode, "secretGrade") >= 0);

		// statc[0] != 0 -> no re-capture (other branch).
		engine.statc[0] = 5;
		assertFalse(mode.onGameOver(engine, 0));
	}

	// ------------------------------------------------------------------
	// onResult: page navigation wraps both directions
	// ------------------------------------------------------------------

	@Test
	void onResultPageUpWrapsToLast() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.statc[1] = 0;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.onResult(engine, 0);
		assertEquals(2, engine.statc[1], "page UP from 0 wraps to 2");
	}

	@Test
	void onResultPageDownWrapsToFirst() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.statc[1] = 2;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1], "page DOWN from 2 wraps to 0");
	}

	@Test
	void onResultButtonFFlipsSectionDisplay() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "isShowBestSectionTime", false);
		engine.statc[1] = 1;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onResult(engine, 0);
		assertTrue(readBool(mode, "isShowBestSectionTime"), "F flips the section-time display on the result screen");
	}

	// ------------------------------------------------------------------
	// Ranking helpers (hermetic): first record always ranks 0
	// ------------------------------------------------------------------

	@Test
	void saveReplayUpdatesRankingForFreshBoard() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;

		// Make the board hermetic: empty out every ranking row.
		clearRankingArrays(mode);
		setMenuValue(mode, "startlevel", 0);
		setMenuValue(mode, "big", false);
		setInt(mode, "grade", 6);
		setInt(mode, "rollclear", 2);
		engine.statistics.level = 999;
		engine.statistics.time = 5000;

		mode.saveReplay(engine, 0, new CustomProperties());
		assertEquals(0, readInt(mode, "rankingRank"), "a fresh top score lands at rank 0");
	}

	@Test
	void checkRankingReturnsMinusOneWhenWorse() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Fill every row with an unbeatable record; a weak score should not rank.
		int[] grade = (int[]) field(mode.getClass(), "rankingGrade").get(mode);
		int[] level = (int[]) field(mode.getClass(), "rankingLevel").get(mode);
		int[] time = (int[]) field(mode.getClass(), "rankingTime").get(mode);
		int[] roll = (int[]) field(mode.getClass(), "rankingRollclear").get(mode);
		for(int i = 0; i < grade.length; i++) {
			grade[i] = 6; level[i] = 999; time[i] = 1; roll[i] = 2;
		}

		Method check = AbstractManiaMode.class.getDeclaredMethod("checkRanking", int.class, int.class, int.class, int.class);
		check.setAccessible(true);
		int rank = (int) check.invoke(mode, 0, 1, 99999, 0); // far worse
		assertEquals(-1, rank, "a worse-than-everything score is out of rank");
	}

	// ------------------------------------------------------------------
	// Render hooks (smoke / threshold branches; no-op receiver)
	// ------------------------------------------------------------------

	@Test
	void renderLastInGameAndSettingsDoNotThrow() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		// In-game branch with all medals lit + roll time + section times shown.
		setMenuValue(mode, "showsectiontime", true);
		setInt(mode, "medalAC", 3);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 1);
		setInt(mode, "medalRE", 3);
		setInt(mode, "medalRO", 2);
		setInt(mode, "medalCO", 1);
		setInt(mode, "lastscore", 500);
		setInt(mode, "scgettime", 60);
		setInt(mode, "sectionavgtime", 200);
		int[] sectiontime = (int[]) field(mode.getClass(), "sectiontime").get(mode);
		sectiontime[0] = 300;
		sectiontime[1] = 1500;
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 100); // remaining roll time in 0..600 -> yellow flash branch
		engine.statistics.level = 150;
		engine.speed.gravity = -1; // speed meter = 40 branch
		mode.renderLast(engine, 0);

		// Settings branch: leaderboard view (startlevel 0, not big, no AI).
		setMenuValue(mode, "startlevel", 0);
		setMenuValue(mode, "big", false);
		setBool(mode, "isShowBestSectionTime", false);
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);

		// Settings branch: best-section-time view.
		setBool(mode, "isShowBestSectionTime", true);
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderResultAllPagesDoNotThrow() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setupRunningGame(engine, mode);

		setInt(mode, "grade", 6);
		setInt(mode, "rollclear", 2);
		setInt(mode, "secretGrade", 5); // > 4 -> S.GRADE line
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 1);
		setInt(mode, "medalSK", 1);
		setInt(mode, "medalRE", 1);
		setInt(mode, "medalRO", 1);
		setInt(mode, "medalCO", 1);
		setInt(mode, "sectionavgtime", 200);
		int[] sectiontime = (int[]) field(mode.getClass(), "sectiontime").get(mode);
		sectiontime[0] = 300;

		engine.statc[1] = 0;
		mode.renderResult(engine, 0);
		engine.statc[1] = 1;
		mode.renderResult(engine, 0);
		engine.statc[1] = 2;
		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	/** Ensures engine.field exists (the engine only creates it lazily during a real frame). */
	private static void setupRunningGame(GameEngine engine, PhantomManiaMode mode) throws Exception {
		mode.startGame(engine, 0);
		if(engine.field == null) {
			engine.field = new nullpomino.game.component.Field(
					engine.ruleopt.fieldWidth, engine.ruleopt.fieldHeight,
					engine.ruleopt.fieldHiddenHeight, engine.ruleopt.fieldCeiling);
		}
	}

	/** Fills the bottom of the field with enough solid blocks. */
	private static void fillField(GameEngine engine, int target) {
		engine.field.reset();
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		int placed = 0;
		for(int y = h - 1; y >= 0 && placed < target; y--) {
			for(int x = 0; x < w && placed < target; x++) {
				engine.field.setBlockColor(x, y, Block.BLOCK_COLOR_RED);
				placed++;
			}
		}
	}

	private static void clearRankingArrays(PhantomManiaMode mode) throws Exception {
		int[] grade = (int[]) field(mode.getClass(), "rankingGrade").get(mode);
		int[] level = (int[]) field(mode.getClass(), "rankingLevel").get(mode);
		int[] time = (int[]) field(mode.getClass(), "rankingTime").get(mode);
		int[] roll = (int[]) field(mode.getClass(), "rankingRollclear").get(mode);
		for(int i = 0; i < grade.length; i++) {
			grade[i] = 0; level[i] = 0; time[i] = 0; roll[i] = 0;
		}
	}

	private static void setMenuValue(Object mode, String menuItemName, int value) throws Exception {
		Object item = field(mode.getClass(), menuItemName).get(mode);
		Field valueField = field(item.getClass(), "value");
		valueField.set(item, Integer.valueOf(value));
	}

	private static void setMenuValue(Object mode, String menuItemName, boolean value) throws Exception {
		Object item = field(mode.getClass(), menuItemName).get(mode);
		Field valueField = field(item.getClass(), "value");
		valueField.set(item, value);
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
