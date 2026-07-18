package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link SpeedMania2Mode}: the remaining uncovered
 * operand outcomes in the settings switch, renderLast display variants,
 * onMove/onARE hold+version guards, torikan operand falsifiers, grade
 * ceiling clamps and the saveReplay ranking guard.
 */
class SpeedMania2ModeBranchGapTest {

	/** Hermetic receiver: no mode.cfg disk I/O, adjustable next-display type. */
	private static class TestReceiver extends EventReceiver {
		int nextDisplayType;
		int savedCount;
		@Override public int getNextDisplayType() { return nextDisplayType; }
		@Override public CustomProperties loadModeConfig() { return new CustomProperties(); }
		@Override public void saveModeConfig(CustomProperties prop) { savedCount++; }
	}

	private static GameEngine freshEngine(SpeedMania2Mode mode, EventReceiver receiver) {
		GameManager manager = new GameManager(receiver);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static GameEngine startedEngine(SpeedMania2Mode mode, EventReceiver receiver) {
		GameEngine engine = freshEngine(mode, receiver);
		mode.playerInit(engine, 0);
		mode.startGame(engine, 0);
		engine.createFieldIfNeeded();
		return engine;
	}

	private static void push(GameEngine engine, int button) {
		engine.ctrl.reset();
		engine.ctrl.buttonPress[button] = true;
		engine.ctrl.buttonTime[button] = 1;
	}

	// -----------------------------------------------------------------------
	// setAverageSectionTime out-of-range loop indices (defensive guard)
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeSkipsOutOfRangeIndices() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode, new TestReceiver());
		mode.playerInit(engine, 0);

		// i starts below 0: only sectiontime[0] is summed
		mode.sectiontime[0] = 600;
		setInt(mode, "startlevel", -1);
		mode.sectionscomp = 2;
		invokeSetAverageSectionTime(mode);
		assertEquals(300, mode.sectionavgtime, "index -1 skipped, only [0] counted");

		// i runs past the last section: only sectiontime[12] is summed
		setInt(mode, "startlevel", 12);
		mode.sectiontime[12] = 400;
		invokeSetAverageSectionTime(mode);
		assertEquals(200, mode.sectionavgtime, "index 13 skipped, only [12] counted");
	}

	// -----------------------------------------------------------------------
	// onSetting: toggle cases both directions + switch default
	// -----------------------------------------------------------------------

	@Test
	void onSettingTogglesBothWaysAndIgnoresUnknownCursor() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode, new TestReceiver());
		mode.playerInit(engine, 0);
		push(engine, Controller.BUTTON_RIGHT);

		mode.menuCursor = 2;
		mode.onSetting(engine, 0);
		assertTrue(readBool(mode, "showsectiontime"), "cursor 2 toggles on");
		mode.onSetting(engine, 0);
		assertFalse(readBool(mode, "showsectiontime"), "cursor 2 toggles back off");

		mode.menuCursor = 3;
		mode.onSetting(engine, 0);
		assertTrue(readBool(mode, "big"), "cursor 3 toggles on");
		mode.onSetting(engine, 0);
		assertFalse(readBool(mode, "big"), "cursor 3 toggles back off");

		mode.menuCursor = 5;
		mode.onSetting(engine, 0);
		assertTrue(readBool(mode, "gradedisp"), "cursor 5 toggles on");
		mode.onSetting(engine, 0);
		assertFalse(readBool(mode, "gradedisp"), "cursor 5 toggles back off");

		// switch default: no setting changes
		int torikanBefore = readInt(mode, "torikan");
		mode.menuCursor = 17;
		mode.onSetting(engine, 0);
		assertEquals(torikanBefore, readInt(mode, "torikan"), "unknown cursor changes nothing");
		assertEquals(0, readInt(mode, "startlevel"), "unknown cursor changes nothing");
	}

	@Test
	void onSettingSectionTimeViewToggleAndDecide() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		TestReceiver receiver = new TestReceiver();
		GameEngine engine = freshEngine(mode, receiver);
		mode.playerInit(engine, 0);

		mode.menuTime = 10;
		push(engine, Controller.BUTTON_F);
		assertTrue(mode.onSetting(engine, 0), "F only flips the view, stays on menu");
		assertTrue(mode.isShowBestSectionTime, "F flips to best-section view");
		mode.onSetting(engine, 0);
		assertFalse(mode.isShowBestSectionTime, "second F flips back");

		mode.menuTime = 10;
		push(engine, Controller.BUTTON_A);
		assertFalse(mode.onSetting(engine, 0), "A leaves the settings screen");
		assertEquals(1, receiver.savedCount, "decide saves the mode config");
	}

	// -----------------------------------------------------------------------
	// renderSetting torikan NONE/time variants
	// -----------------------------------------------------------------------

	@Test
	void renderSettingTorikanNoneAndTime() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode, new TestReceiver());
		mode.playerInit(engine, 0);

		setInt(mode, "torikan", 0);
		mode.renderSetting(engine, 0);
		setInt(mode, "torikan", 8880);
		mode.renderSetting(engine, 0);
		assertEquals(8880, readInt(mode, "torikan"), "render leaves settings untouched");
	}

	// -----------------------------------------------------------------------
	// startGame negative level guard
	// -----------------------------------------------------------------------

	@Test
	void startGameNegativeStartLevelClampsNextSection() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode, new TestReceiver());
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", -1);

		// The guard fires (nextseclv forced to 100), but setSpeed right after
		// cannot handle a negative section index.
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> mode.startGame(engine, 0));
		assertEquals(100, readInt(mode, "nextseclv"), "negative level still clamps next section");
	}

	// -----------------------------------------------------------------------
	// renderLast: outer status condition + leaderboard guard falsifiers
	// -----------------------------------------------------------------------

	@Test
	void renderLastOnResultScreenBothReplayModes() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());

		engine.stat = GameEngine.Status.RESULT;
		engine.owner.replayMode = false;
		mode.renderLast(engine, 0); // RESULT + live play -> leaderboard branch

		engine.owner.replayMode = true;
		mode.renderLast(engine, 0); // RESULT + replay -> in-game HUD branch
		assertEquals(GameEngine.Status.RESULT, engine.stat);
	}

	@Test
	void renderLastLeaderboardGuardFalsifiers() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.stat = GameEngine.Status.SETTING;

		engine.owner.replayMode = true;
		mode.renderLast(engine, 0); // replayMode true

		engine.owner.replayMode = false;
		setInt(mode, "startlevel", 5);
		mode.renderLast(engine, 0); // startlevel != 0

		setInt(mode, "startlevel", 0);
		setBool(mode, "big", true);
		mode.renderLast(engine, 0); // big

		setBool(mode, "big", false);
		engine.ai = new DummyAI();
		mode.renderLast(engine, 0); // AI in use
		engine.ai = null;
		assertEquals(0, readInt(mode, "startlevel"));
	}

	@Test
	void renderLastSideNextDisplayAndRankHighlight() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		TestReceiver receiver = new TestReceiver();
		receiver.nextDisplayType = 2;
		GameEngine engine = startedEngine(mode, receiver);

		// Leaderboard with side-big preview scale and a highlighted rank row
		engine.stat = GameEngine.Status.SETTING;
		mode.rankingRank = 2;
		mode.isShowBestSectionTime = false;
		mode.renderLast(engine, 0);

		// In-game section-time column at side-big offsets
		engine.stat = GameEngine.Status.MOVE;
		setBool(mode, "showsectiontime", true);
		mode.sectiontime[0] = 500;
		mode.sectionavgtime = 120;
		engine.statistics.level = 0;
		mode.renderLast(engine, 0);
		assertEquals(2, mode.rankingRank);
	}

	@Test
	void renderLastInGameGradeAndScoreBranches() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.stat = GameEngine.Status.MOVE;
		setBool(mode, "gradedisp", true);

		// grade below range + fresh score + game active outside the roll
		setInt(mode, "grade", -1);
		setInt(mode, "lastscore", 5);
		setInt(mode, "scgettime", 0);
		engine.gameActive = true;
		engine.ending = 0;
		mode.renderLast(engine, 0);

		// grade above range + "(+n)" score suffix
		setInt(mode, "grade", 99);
		setInt(mode, "scgettime", 10);
		mode.renderLast(engine, 0);

		// valid grade with non-multiple-of-4 flash frame
		setInt(mode, "grade", 5);
		setInt(mode, "gradeflash", 3);
		mode.renderLast(engine, 0);

		// grade display off entirely
		setBool(mode, "gradedisp", false);
		mode.renderLast(engine, 0);
		assertEquals(5, readInt(mode, "grade"));
	}

	@Test
	void renderLastRollTimeBoundaryBranches() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 2;

		setInt(mode, "rolltime", 4000); // remaining < 0 -> clamped to 0, "time > 0" false
		mode.renderLast(engine, 0);
		setInt(mode, "rolltime", 0); // remaining 3238 -> "time < 600" false
		mode.renderLast(engine, 0);
		setInt(mode, "rolltime", 3000); // remaining 238 -> flashing range
		mode.renderLast(engine, 0);
		assertEquals(3000, readInt(mode, "rolltime"));
	}

	@Test
	void renderLastSectionSeparatorDuringEndingAndNullSectionTime() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.stat = GameEngine.Status.MOVE;
		setBool(mode, "showsectiontime", true);
		mode.sectiontime[0] = 500;
		engine.statistics.level = 0; // current section, but...
		engine.ending = 1; // ...no highlight separator during ending
		mode.sectionavgtime = 0; // average line suppressed
		mode.renderLast(engine, 0);

		mode.sectiontime = null; // display enabled but no data
		mode.renderLast(engine, 0);
		assertEquals(0, mode.sectionavgtime);
	}

	// -----------------------------------------------------------------------
	// onMove operand falsifiers
	// -----------------------------------------------------------------------

	@Test
	void onMoveHoldDisableSkipsLevelUpAndGarbage() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = true;
		setBool(mode, "lvupflag", false);
		engine.statistics.level = 50;
		mode.onMove(engine, 0);
		assertEquals(50, engine.statistics.level, "hold in progress defers the level-up");
	}

	@Test
	void onMoveLevelStopWithoutSound() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBool(mode, "lvupflag", false);
		setBool(mode, "lvstopse", false);
		engine.statistics.level = 98;
		setInt(mode, "nextseclv", 100);
		mode.onMove(engine, 0);
		assertEquals(99, engine.statistics.level, "level still advances with lvstopse off");
	}

	@Test
	void onMoveGarbageCounterBelowThreshold() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBool(mode, "lvupflag", true);
		engine.statistics.level = 500; // garbage interval 20
		setInt(mode, "garbageCount", 0);
		mode.onMove(engine, 0);
		assertEquals(1, readInt(mode, "garbageCount"), "counter rises but no garbage yet");
	}

	@Test
	void onMoveRollAlreadyStarted() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.ending = 2;
		setBool(mode, "rollstarted", true);
		engine.big = false;
		mode.onMove(engine, 0);
		assertFalse(engine.big, "already-started roll does not re-run roll setup");
	}

	@Test
	void onMoveVersionZeroHoldBranches() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		setInt(mode, "version", 0);
		engine.ending = 0;
		engine.statc[0] = 1;

		engine.holdDisable = false;
		setBool(mode, "lvupflag", true);
		mode.onMove(engine, 0);
		assertFalse(readBool(mode, "lvupflag"), "v0 + no hold clears the flag");

		engine.holdDisable = true;
		setBool(mode, "lvupflag", true);
		mode.onMove(engine, 0);
		assertTrue(readBool(mode, "lvupflag"), "v0 + hold keeps the flag");
	}

	// -----------------------------------------------------------------------
	// onARE operand falsifiers
	// -----------------------------------------------------------------------

	@Test
	void onAreOperandFalsifiers() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		setBool(mode, "lvupflag", false);
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);

		engine.ending = 2; // ending active
		engine.statc[0] = 5;
		engine.statc[1] = 6;
		mode.onARE(engine, 0);
		assertEquals(50, engine.statistics.level);

		engine.ending = 0; // not the last ARE frame
		engine.statc[0] = 0;
		engine.statc[1] = 10;
		mode.onARE(engine, 0);
		assertEquals(50, engine.statistics.level);
	}

	@Test
	void onAreAtSectionStopAndSilentStop() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 6;

		// already parked at nextseclv-1: no further increment
		setBool(mode, "lvupflag", false);
		engine.statistics.level = 99;
		setInt(mode, "nextseclv", 100);
		mode.onARE(engine, 0);
		assertEquals(99, engine.statistics.level, "level stop holds at section-1");
		assertTrue(readBool(mode, "lvupflag"));

		// advances into the stop silently with lvstopse off
		setBool(mode, "lvupflag", false);
		setBool(mode, "lvstopse", false);
		engine.statistics.level = 98;
		mode.onARE(engine, 0);
		assertEquals(99, engine.statistics.level);
	}

	// -----------------------------------------------------------------------
	// levelUp BGM fadeout table end
	// -----------------------------------------------------------------------

	@Test
	void levelUpNoFadeoutAtTableEnd() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.owner.bgmStatus.fadesw = false;

		setInt(mode, "bgmlv", 3); // fadeout table entry -1
		engine.statistics.level = 999;
		invokeLevelUp(mode, engine);
		assertFalse(engine.owner.bgmStatus.fadesw, "no fadeout past the table end");

		setInt(mode, "bgmlv", 0); // fadeout at 485, level below it
		engine.statistics.level = 100;
		setInt(mode, "nextseclv", 200);
		invokeLevelUp(mode, engine);
		assertFalse(engine.owner.bgmStatus.fadesw, "no fadeout below the trigger level");
	}

	// -----------------------------------------------------------------------
	// calcScore operand falsifiers
	// -----------------------------------------------------------------------

	@Test
	void calcScoreDuringEndingSkipsScoring() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.ending = 2;
		int before = engine.statistics.score;
		mode.calcScore(engine, 0, 1);
		assertEquals(before, engine.statistics.score, "roll line clears do not score");
	}

	@Test
	void calcScoreSkMedalOperandTails() throws Exception {
		SpeedMania2Mode big = new SpeedMania2Mode();
		GameEngine engineBig = startedEngine(big, new TestReceiver());
		setBool(big, "big", true);
		engineBig.statistics.level = 50;
		setInt(big, "nextseclv", 100);
		engineBig.statistics.totalFour = 4; // last disjunct of the big chain
		mode(engineBig, big, 4);
		assertEquals(1, readInt(big, "medalSK"), "4th big tetris grants SK");
		engineBig.statistics.totalFour = 3; // no disjunct matches
		mode(engineBig, big, 4);
		assertEquals(1, readInt(big, "medalSK"), "3rd big tetris grants nothing");

		SpeedMania2Mode small = new SpeedMania2Mode();
		GameEngine engineSmall = startedEngine(small, new TestReceiver());
		engineSmall.statistics.level = 50;
		setInt(small, "nextseclv", 100);
		engineSmall.statistics.totalFour = 17; // last disjunct of the non-big chain
		mode(engineSmall, small, 4);
		assertEquals(1, readInt(small, "medalSK"), "17th tetris grants SK");
		engineSmall.statistics.totalFour = 3;
		mode(engineSmall, small, 4);
		assertEquals(1, readInt(small, "medalSK"), "3rd tetris grants nothing");
	}

	/** Small alias so the SK test reads as "clear n lines on this mode". */
	private static void mode(GameEngine engine, SpeedMania2Mode mode, int lines) {
		mode.calcScore(engine, 0, lines);
	}

	@Test
	void calcScoreMedalsAlreadyGoldStaySaturated() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);

		setInt(mode, "medalAC", 3); // bravo on an empty field, but AC already gold
		mode.calcScore(engine, 0, 1);
		assertEquals(3, readInt(mode, "medalAC"));

		setBool(mode, "big", true); // big combo chain saturated
		setInt(mode, "medalCO", 3);
		engine.combo = 4;
		mode.calcScore(engine, 0, 1);
		assertEquals(3, readInt(mode, "medalCO"));

		setBool(mode, "big", false); // non-big combo chain saturated
		engine.combo = 7;
		mode.calcScore(engine, 0, 1);
		assertEquals(3, readInt(mode, "medalCO"));
	}

	@Test
	void calcScoreGradeClampAtCeiling() throws Exception {
		// 1300 ending path
		SpeedMania2Mode ending = new SpeedMania2Mode();
		GameEngine e1 = startedEngine(ending, new TestReceiver());
		setInt(ending, "grade", 13);
		e1.statistics.level = 1299;
		setInt(ending, "nextseclv", 1300);
		ending.calcScore(e1, 0, 1);
		assertEquals(13, readInt(ending, "grade"), "1300 ending clamps grade at S13");

		// torikan cut path
		SpeedMania2Mode cut = new SpeedMania2Mode();
		GameEngine e2 = startedEngine(cut, new TestReceiver());
		setInt(cut, "grade", 13);
		e2.statistics.level = 499;
		setInt(cut, "nextseclv", 500);
		setInt(cut, "torikan", 100);
		e2.statistics.time = 200;
		cut.calcScore(e2, 0, 1);
		assertEquals(13, readInt(cut, "grade"), "torikan cut clamps grade at S13");

		// ordinary section crossing path
		SpeedMania2Mode section = new SpeedMania2Mode();
		GameEngine e3 = startedEngine(section, new TestReceiver());
		setInt(section, "grade", 13);
		e3.statistics.level = 99;
		setInt(section, "nextseclv", 100);
		section.calcScore(e3, 0, 1);
		assertEquals(13, readInt(section, "grade"), "section crossing clamps grade at S13");
	}

	@Test
	void calcScoreTorikanOperandFalsifiers() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());

		// nextseclv 500 but level stays below 500
		engine.ending = 0;
		engine.statistics.level = 498;
		setInt(mode, "nextseclv", 500);
		setInt(mode, "torikan", 100);
		engine.statistics.time = 99999;
		mode.calcScore(engine, 0, 1);
		assertEquals(499, engine.statistics.level);
		assertEquals(0, engine.ending, "level below 500 is never torikan-cut");

		// level 500 reached but torikan disabled -> ordinary section transition
		engine.statistics.level = 499;
		setInt(mode, "nextseclv", 500);
		setInt(mode, "torikan", 0);
		mode.calcScore(engine, 0, 1);
		assertEquals(600, readInt(mode, "nextseclv"), "torikan 0 lets play continue");
		assertEquals(0, engine.ending);

		// level 500 reached within the time limit -> ordinary section transition
		engine.statistics.level = 499;
		setInt(mode, "nextseclv", 500);
		setInt(mode, "torikan", 50000);
		engine.statistics.time = 10;
		mode.calcScore(engine, 0, 1);
		assertEquals(600, readInt(mode, "nextseclv"), "fast play passes the 500 gate");

		// nextseclv 1000 but level stays below 1000
		engine.statistics.level = 997;
		setInt(mode, "nextseclv", 1000);
		setInt(mode, "torikan", 100);
		engine.statistics.time = 99999;
		mode.calcScore(engine, 0, 1);
		assertEquals(998, engine.statistics.level);
		assertEquals(0, engine.ending);

		// level 1000 reached but torikan disabled
		engine.statistics.level = 999;
		setInt(mode, "nextseclv", 1000);
		setInt(mode, "torikan", 0);
		mode.calcScore(engine, 0, 1);
		assertEquals(1100, readInt(mode, "nextseclv"));
		assertTrue(engine.bone, "passing 1000 enables bone blocks");

		// level 1000 reached within twice the time limit
		engine.statistics.level = 999;
		setInt(mode, "nextseclv", 1000);
		setInt(mode, "torikan", 100);
		engine.statistics.time = 150; // <= torikan * 2
		mode.calcScore(engine, 0, 1);
		assertEquals(1100, readInt(mode, "nextseclv"), "fast play passes the 1000 gate");
	}

	@Test
	void calcScoreSectionBgmTableEnd() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		setInt(mode, "bgmlv", 3); // BGM change table entry -1
		int bgmBefore = engine.owner.bgmStatus.bgm;
		engine.statistics.level = 99;
		setInt(mode, "nextseclv", 100);
		mode.calcScore(engine, 0, 1);
		assertEquals(200, readInt(mode, "nextseclv"));
		assertEquals(bgmBefore, engine.owner.bgmStatus.bgm, "no BGM switch past the table end");
	}

	@Test
	void calcScoreManualLockAndNegativeSpeedBonus() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		engine.manualLock = true;
		engine.statc[0] = 9999; // lock delay long exceeded -> speed bonus clamped to 0
		mode.calcScore(engine, 0, 1);
		assertTrue(engine.statistics.score > 0, "manual lock still scores");
		assertEquals(120, readInt(mode, "scgettime"));
	}

	// -----------------------------------------------------------------------
	// onReady bone-block gate
	// -----------------------------------------------------------------------

	@Test
	void onReadyBoneGateBranches() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode, new TestReceiver());
		mode.playerInit(engine, 0);

		engine.statc[0] = 0;
		setInt(mode, "startlevel", 11);
		engine.bone = false;
		mode.onReady(engine, 0);
		assertTrue(engine.bone, "start level 11+ begins with bone blocks");

		setInt(mode, "startlevel", 0);
		engine.bone = false;
		mode.onReady(engine, 0);
		assertFalse(engine.bone, "low start level has no bone blocks");

		engine.statc[0] = 1;
		setInt(mode, "startlevel", 11);
		mode.onReady(engine, 0);
		assertFalse(engine.bone, "only the first ready frame arms bone blocks");
	}

	// -----------------------------------------------------------------------
	// onLast operand falsifiers
	// -----------------------------------------------------------------------

	@Test
	void onLastSectionAndRollGuardFalsifiers() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());

		// timer running but ending active: no section time accumulation
		engine.timerActive = true;
		engine.ending = 2;
		engine.gameActive = false;
		engine.statistics.level = 0;
		mode.onLast(engine, 0);
		assertEquals(0, mode.sectiontime[0]);

		// section index out of range on both sides
		engine.ending = 0;
		engine.statistics.level = -100;
		mode.onLast(engine, 0);
		engine.statistics.level = 1300;
		mode.onLast(engine, 0);

		// game active but not in the roll: roll timer untouched
		engine.gameActive = true;
		engine.ending = 0;
		engine.timerActive = false;
		mode.onLast(engine, 0);
		assertEquals(0, readInt(mode, "rolltime"));
	}

	// -----------------------------------------------------------------------
	// onGameOver inactive-game guard
	// -----------------------------------------------------------------------

	@Test
	void onGameOverInactiveGameSkipsSecretGrade() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());
		engine.statc[0] = 0;
		engine.gameActive = false;
		assertFalse(mode.onGameOver(engine, 0));
		assertEquals(0, readInt(mode, "secretGrade"), "no secret grade without an active game");
	}

	// -----------------------------------------------------------------------
	// renderResult rollclear colors, off-range page, empty medals
	// -----------------------------------------------------------------------

	@Test
	void renderResultRollclearColorsAndOffRangePage() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = startedEngine(mode, new TestReceiver());

		engine.statc[1] = 0;
		for(int rollclear : new int[] {1, 3, 4}) {
			setInt(mode, "rollclear", rollclear);
			mode.renderResult(engine, 0);
		}

		engine.statc[1] = 2; // medal page with no medals at all
		mode.renderResult(engine, 0);

		engine.statc[1] = 3; // beyond the last page: header only
		mode.renderResult(engine, 0);
		assertEquals(3, engine.statc[1]);
	}

	// -----------------------------------------------------------------------
	// saveReplay ranking guard
	// -----------------------------------------------------------------------

	@Test
	void saveReplayGuardFalsifiersSkipRanking() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		TestReceiver receiver = new TestReceiver();
		GameEngine engine = startedEngine(mode, receiver);
		CustomProperties prop = new CustomProperties();

		engine.owner.replayMode = true;
		mode.saveReplay(engine, 0, prop);

		engine.owner.replayMode = false;
		setInt(mode, "startlevel", 5);
		mode.saveReplay(engine, 0, prop);

		setInt(mode, "startlevel", 0);
		setBool(mode, "big", true);
		mode.saveReplay(engine, 0, prop);

		setBool(mode, "big", false);
		engine.ai = new DummyAI();
		mode.saveReplay(engine, 0, prop);
		engine.ai = null;

		assertEquals(-1, mode.rankingRank, "no ranking update in any falsified case");
		assertEquals(0, receiver.savedCount, "nothing is persisted");
	}

	@Test
	void saveReplayUnrankedButGoldStStillSaves() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		TestReceiver receiver = new TestReceiver();
		GameEngine engine = startedEngine(mode, receiver);

		// Fill the table with unbeatable rows so this run stays unranked.
		for(int i = 0; i < mode.rankingGrade.length; i++) {
			mode.rankingGrade[i] = 13;
			mode.rankingLevel[i] = 1300;
			mode.rankingTime[i] = 1;
			mode.rankingRollclear[i] = 4;
		}
		mode.medalST = 3;
		mode.saveReplay(engine, 0, new CustomProperties());

		assertEquals(-1, mode.rankingRank, "weak result does not rank");
		assertEquals(1, receiver.savedCount, "gold ST alone still saves section records");
	}

	@Test
	void saveReplayRankedResultSaves() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		TestReceiver receiver = new TestReceiver();
		GameEngine engine = startedEngine(mode, receiver);
		engine.statistics.level = 500; // beats the empty table
		mode.saveReplay(engine, 0, new CustomProperties());

		assertEquals(0, mode.rankingRank, "result enters the empty leaderboard first");
		assertEquals(1, receiver.savedCount);
	}

	// -----------------------------------------------------------------------
	// reflection helpers
	// -----------------------------------------------------------------------

	private static void invokeSetAverageSectionTime(SpeedMania2Mode mode) throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeLevelUp(SpeedMania2Mode mode, GameEngine engine) throws Exception {
		Method m = SpeedMania2Mode.class.getDeclaredMethod("levelUp", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
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
