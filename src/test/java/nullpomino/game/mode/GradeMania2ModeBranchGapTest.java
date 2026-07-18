package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import nullpomino.game.ai.DummyAI;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.menu.OnOffMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.junit.jupiter.api.Test;

/**
 * Residual branch-gap tests for {@link GradeMania2Mode}, driven by the
 * jacoco uncovered-branch dump. Covers: setAverageSectionTime negative-index
 * guard, onSetting F-toggle back to ranking view, startGame negative level,
 * renderLast RESULT/setting gate arms + ranking row decorations + gameplay
 * HUD arms (grade flash, score delta, negative level, 20G meter, roll timer,
 * medals, section-time display variants), onMove hold-disable / levelstop /
 * v1 RE-medal / lvupflag / grade-decay arms, onARE arms, levelUp ghost/BGM/RE
 * arms, calcScore grade-point clamps, SK/AC/CO medal arms, m-roll veto arms,
 * BGM table end, RO at 300, levelstop-off, speed-bonus clamp, onLast section
 * guards, onGameOver arms, renderResult roll-clear colors and page arms, and
 * saveReplay gating arms. A recording EventReceiver makes render output
 * observable.
 */
class GradeMania2ModeBranchGapTest {

	// =======================================================================
	// L291: setAverageSectionTime skips negative section indices
	// =======================================================================

	@Test
	void setAverageSectionTimeSkipsNegativeSections() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);

		// startlevel = -1 (reflection only; UI clamps to >= 0), sectionscomp = 2
		// -> loop i = -1, 0. i = -1 fails (i >= 0) so only sectiontime[0] counts.
		startlevel(mode).value = -1;
		mode.sectionscomp = 2;
		mode.sectiontime[0] = 1200;

		Method m = GradeMania2Mode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);

		assertEquals(600, mode.sectionavgtime);
	}

	// =======================================================================
	// L353: onSetting F toggles best-section-time view back OFF
	// =======================================================================

	@Test
	void onSettingFTogglesSectionTimeViewOff() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		mode.isShowBestSectionTime = true;
		mode.menuTime = 5;

		e.ctrl.reset();
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1;

		mode.onSetting(e, 0);

		assertFalse(mode.isShowBestSectionTime, "F must toggle back to the ranking view");
	}

	// =======================================================================
	// L392: startGame with negative level forces nextseclv = 100
	// =======================================================================

	@Test
	void startGameNegativeLevelForcesNextSection100() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		startlevel(mode).value = -1; // level = min(999, -100) = -100 < 0

		// setSpeed cannot map a negative section (tableDAS[-1]) so startGame
		// aborts after the nextseclv fixups; the branch outcome is still
		// observable on the mode/engine state.
		assertThrows(ArrayIndexOutOfBoundsException.class, () -> mode.startGame(e, 0));

		assertEquals(-100, e.statistics.level);
		assertEquals(100, mode.nextseclv);
	}

	// =======================================================================
	// L434/435: renderLast outer gate arms
	// =======================================================================

	@Test
	void renderLastResultScreenShowsRankingWithDecorations() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);

		// L444/445 roll-clear colors, L448 grade-range guards, L450/451 rank row.
		mode.rankingRollclear[0] = 1;
		mode.rankingRollclear[1] = 3;
		mode.rankingRollclear[2] = 2;
		mode.rankingRollclear[3] = 4;
		mode.rankingGrade[4] = -1;
		mode.rankingGrade[5] = 99;
		mode.rankingLevel[2] = 777;
		mode.rankingTime[2] = 300;
		mode.rankingRank = 2;

		e.stat = GameEngine.Status.RESULT; // (RESULT && !replayMode) arm of L434
		e.owner.replayMode = false;
		mode.renderLast(e, 0);

		assertTrue(rec.scoreHas("GRADE LEVEL TIME", EventReceiver.COLOR_BLUE));
		assertTrue(rec.scoreHas("9", EventReceiver.COLOR_GREEN), "rollclear 1 row is green");
		assertTrue(rec.scoreHas("9", EventReceiver.COLOR_ORANGE), "rollclear 2 row is orange");
		assertTrue(rec.scoreHas("777", EventReceiver.COLOR_RED), "current-rank level is highlighted");
		assertTrue(rec.scoreHas(GeneralUtil.getTime(300), EventReceiver.COLOR_RED), "current-rank time is highlighted");
		// grade names out of [0, 20) are skipped (rows i=4 at y=7, i=5 at y=8)
		assertFalse(rec.scoreAt(3, 7), "negative grade must not draw a grade name");
		assertFalse(rec.scoreAt(3, 8), "grade >= table length must not draw a grade name");
	}

	@Test
	void renderLastResultInReplayModeShowsGameplayHud() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);

		e.stat = GameEngine.Status.RESULT;
		e.owner.replayMode = true; // (RESULT && !replayMode) fails -> HUD branch
		mode.renderLast(e, 0);

		assertFalse(rec.scoreHasText("GRADE LEVEL TIME"));
		assertTrue(rec.scoreHas("GRADE", EventReceiver.COLOR_BLUE));
	}

	@Test
	void renderLastSettingHidesRankingWhenAnyGateConditionFails() throws Exception {
		for (int variant = 0; variant < 5; variant++) {
			GradeMania2Mode mode = new GradeMania2Mode();
			Rec rec = new Rec();
			GameEngine e = fresh(mode, rec);
			mode.playerInit(e, 0);
			e.stat = GameEngine.Status.SETTING;

			switch (variant) {
			case 0: e.owner.replayMode = true; break;
			case 1: startlevel(mode).value = 1; break;
			case 2: onOff(mode, "big").value = true; break;
			case 3: onOff(mode, "always20g").value = true; break;
			case 4: e.ai = new DummyAI(); break;
			}

			mode.renderLast(e, 0);

			assertTrue(rec.scoreHas("GRADE MANIA 2", EventReceiver.COLOR_CYAN), "variant " + variant);
			assertFalse(rec.scoreHasText("GRADE LEVEL TIME"),
					"variant " + variant + " must suppress the ranking table");
			assertFalse(rec.scoreHasText("F:VIEW SECTION TIME"), "variant " + variant);
		}
	}

	// =======================================================================
	// L438/439: side-next (type 2) layout for the ranking table
	// =======================================================================

	@Test
	void renderLastRankingUsesHalfScaleOnType2Display() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		rec.nextType = 2;
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.SETTING;

		mode.renderLast(e, 0);

		assertTrue(rec.score.stream().anyMatch(
				d -> d.s.equals("GRADE LEVEL TIME") && d.y == 4 && d.scale == 0.5f),
				"type-2 display puts the half-scale header at topY-1 = 4");
	}

	// =======================================================================
	// L483: gameplay grade display range guards
	// =======================================================================

	@Test
	void renderLastSkipsGradeNameOutsideTableRange() throws Exception {
		for (int g : new int[] {-1, 20}) {
			GradeMania2Mode mode = new GradeMania2Mode();
			Rec rec = new Rec();
			GameEngine e = fresh(mode, rec);
			mode.playerInit(e, 0);
			e.stat = GameEngine.Status.MOVE;
			mode.grade = g;

			mode.renderLast(e, 0);

			assertTrue(rec.scoreHas("GRADE", EventReceiver.COLOR_BLUE));
			assertFalse(rec.scoreAt(0, 3), "grade " + g + " must not draw a grade name");
		}
	}

	// =======================================================================
	// L484: grade flash arms (gradeflash > 0, % 4)
	// =======================================================================

	@Test
	void renderLastGradeFlashHighlightsOnMultipleOfFour() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		mode.grade = 19;

		mode.gradeflash = 4; // > 0 and % 4 == 0 -> highlighted (red)
		mode.renderLast(e, 0);
		assertTrue(rec.scoreHas("GM", EventReceiver.COLOR_RED));

		rec.score.clear();
		mode.gradeflash = 3; // > 0 but % 4 != 0 -> plain white
		mode.renderLast(e, 0);
		assertTrue(rec.scoreHas("GM", EventReceiver.COLOR_WHITE));
	}

	// =======================================================================
	// L489: score delta string arms
	// =======================================================================

	@Test
	void renderLastShowsScoreDeltaOnlyWhileScgettimeRuns() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.statistics.score = 555;
		mode.lastscore = 100;

		mode.scgettime = 120; // both OR operands false -> "+100" suffix shown
		mode.renderLast(e, 0);
		assertTrue(rec.score.stream().anyMatch(d -> d.s.contains("(+100)")));

		rec.score.clear();
		mode.scgettime = 0; // second operand true -> plain score
		mode.renderLast(e, 0);
		assertTrue(rec.scoreHasText("555"));
		assertFalse(rec.score.stream().anyMatch(d -> d.s.contains("(+")));
	}

	// =======================================================================
	// L499/504: negative level clamp and 20G speed meter
	// =======================================================================

	@Test
	void renderLastClampsNegativeLevelAndPegsSpeedMeterAt20G() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.statistics.level = -5;
		e.speed.gravity = -1;

		mode.renderLast(e, 0);

		assertTrue(rec.scoreHasText("  0"), "negative level renders as 0");
		assertEquals(40, rec.speedMeter, "20G pegs the speed meter at 40");
	}

	// =======================================================================
	// L514/516/518: roll countdown display arms
	// =======================================================================

	@Test
	void renderLastRollCountdownArms() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.gameActive = true;
		e.ending = 2;

		mode.rolltime = 4000; // over the limit -> clamped to 0, not highlighted
		mode.renderLast(e, 0);
		assertTrue(rec.scoreHasText("ROLL TIME"));
		assertTrue(rec.score.stream().anyMatch(
				d -> d.y == 18 && d.s.equals(GeneralUtil.getTime(0)) && d.color == EventReceiver.COLOR_WHITE));

		rec.score.clear();
		mode.rolltime = 3694 - 300; // 300 frames left -> highlighted red
		mode.renderLast(e, 0);
		assertTrue(rec.score.stream().anyMatch(
				d -> d.y == 18 && d.s.equals(GeneralUtil.getTime(300)) && d.color == EventReceiver.COLOR_RED));

		rec.score.clear();
		mode.rolltime = 0; // 3694 left (>= 600) -> plain white
		mode.renderLast(e, 0);
		assertTrue(rec.score.stream().anyMatch(
				d -> d.y == 18 && d.s.equals(GeneralUtil.getTime(3694)) && d.color == EventReceiver.COLOR_WHITE));
	}

	// =======================================================================
	// L522-527: medal row rendering
	// =======================================================================

	@Test
	void renderLastDrawsEveryEarnedMedal() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		mode.medalAC = 1;
		mode.medalST = 2;
		mode.medalSK = 3;
		set(mode, "medalRE", 1);
		set(mode, "medalRO", 2);
		mode.medalCO = 3;

		mode.renderLast(e, 0);

		assertTrue(rec.scoreHas("AC", EventReceiver.COLOR_RED));
		assertTrue(rec.scoreHas("ST", EventReceiver.COLOR_WHITE));
		assertTrue(rec.scoreHas("RE", EventReceiver.COLOR_RED));
		assertTrue(rec.scoreHas("CO", EventReceiver.COLOR_YELLOW));
		// SK medal and RO medal both render the text "SK"
		assertEquals(2, rec.score.stream().filter(d -> d.s.equals("SK")).count());
	}

	// =======================================================================
	// L532/533/534/544: section time display arms
	// =======================================================================

	@Test
	void renderLastSkipsSectionTimesWhenArrayIsNull() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		onOff(mode, "showsectiontime").value = true;
		mode.sectiontime = null;

		mode.renderLast(e, 0);

		assertFalse(rec.scoreHasText("SECTION TIME"));
	}

	@Test
	void renderLastSectionTimesUseNarrowColumnOnType2Display() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		rec.nextType = 2;
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		onOff(mode, "showsectiontime").value = true;
		mode.sectiontime[0] = 120;
		mode.sectionavgtime = 90;

		mode.renderLast(e, 0);

		assertTrue(rec.score.stream().anyMatch(d -> d.s.equals("SECTION TIME") && d.x == 8),
				"type-2 display moves the section header to x = 8");
		assertTrue(rec.score.stream().anyMatch(d -> d.s.equals("AVERAGE") && d.x == 9));
	}

	@Test
	void renderLastSectionSeparatorArms() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		onOff(mode, "showsectiontime").value = true;
		mode.sectiontime[0] = 120;
		mode.sectiontime[1] = 100;
		e.statistics.level = 0; // current section = 0

		e.ending = 0;
		mode.renderLast(e, 0);
		assertTrue(rec.scoreHasText("  0b" + GeneralUtil.getTime(120)), "current section uses 'b'");
		assertTrue(rec.scoreHasText("100 " + GeneralUtil.getTime(100)), "other sections use a space");

		rec.score.clear();
		e.ending = 2; // ending != 0 -> even the current section uses a space
		mode.renderLast(e, 0);
		assertTrue(rec.scoreHasText("  0 " + GeneralUtil.getTime(120)));
	}

	// =======================================================================
	// L567: onMove hold-disable arm blocks the natural level-up
	// =======================================================================

	@Test
	void onMoveHoldDisableBlocksLevelUp() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.statc[0] = 0;
		e.holdDisable = true;
		mode.lvupflag = false;
		mode.nextseclv = 100;
		e.statistics.level = 0;

		mode.onMove(e, 0);

		assertEquals(0, e.statistics.level, "hold-disabled frame must not raise the level");
	}

	// =======================================================================
	// L571: onMove levelstop arms at nextseclv-1
	// =======================================================================

	@Test
	void onMoveLevelStopArms() throws Exception {
		for (boolean se : new boolean[] {true, false}) {
			GradeMania2Mode mode = new GradeMania2Mode();
			GameEngine e = fresh(mode);
			mode.playerInit(e, 0);
			e.ending = 0;
			e.statc[0] = 0;
			e.holdDisable = false;
			mode.lvupflag = false;
			mode.nextseclv = 100;
			onOff(mode, "lvstopse").value = se;
			e.statistics.level = 98;

			mode.onMove(e, 0);

			assertEquals(99, e.statistics.level, "lvstopse=" + se);
			assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);

			// level != nextseclv-1 after the raise (false arm of L571)
			e.statistics.level = 0;
			mode.onMove(e, 0);
			assertEquals(1, e.statistics.level);
		}
	}

	// =======================================================================
	// L581/585/589: onMove v1 RE-medal arms
	// =======================================================================

	@Test
	void onMoveV1ReMedalSkippedWhenTimerInactive() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = v1MedalEngine(mode, 16); // 160 blocks
		e.timerActive = false;

		mode.onMove(e, 0);

		assertFalse(getBool(mode, "recoveryFlag"),
				"timer-inactive frame must not arm the recovery flag");
	}

	@Test
	void onMoveV1ReMedalSkippedWhenAlreadyMaxed() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = v1MedalEngine(mode, 16);
		e.timerActive = true;
		set(mode, "medalRE", 3);

		mode.onMove(e, 0);

		assertEquals(3, getInt(mode, "medalRE"));
		assertFalse(getBool(mode, "recoveryFlag"));
	}

	@Test
	void onMoveV1ReMedalNeedsEnoughBlocksToArm() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = v1MedalEngine(mode, 5); // 50 blocks < 150
		e.timerActive = true;

		mode.onMove(e, 0);

		assertFalse(getBool(mode, "recoveryFlag"));
	}

	@Test
	void onMoveV1ReMedalNotAwardedAbove70Blocks() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = v1MedalEngine(mode, 16); // 160 blocks > 70
		e.timerActive = true;
		set(mode, "recoveryFlag", true);

		mode.onMove(e, 0);

		assertTrue(getBool(mode, "recoveryFlag"), "recovery stays armed above 70 blocks");
		assertEquals(0, getInt(mode, "medalRE"));
	}

	private static GameEngine v1MedalEngine(GradeMania2Mode mode, int rows) throws Exception {
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.statc[0] = 0;
		e.holdDisable = false;
		mode.lvupflag = false;
		mode.version = 1;
		e.createFieldIfNeeded();
		fillBlocks(e, rows);
		return e;
	}

	// =======================================================================
	// L598: onMove lvupflag reset arms for version 0
	// =======================================================================

	@Test
	void onMoveVersion0ResetsLvupflagOnlyWhileHoldEnabled() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		mode.version = 0;
		e.ending = 0;
		e.statc[0] = 1;

		e.holdDisable = false;
		mode.lvupflag = true;
		mode.onMove(e, 0);
		assertFalse(mode.lvupflag, "v0 + hold enabled resets the pending flag");

		e.holdDisable = true;
		mode.lvupflag = true;
		mode.onMove(e, 0);
		assertTrue(mode.lvupflag, "v0 + hold disabled keeps the pending flag");
	}

	// =======================================================================
	// L603/607: grade decay arms
	// =======================================================================

	@Test
	void onMoveGradeDecayPausedDuringCombo() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = decayEngine(mode);
		e.combo = 1;

		mode.onMove(e, 0);

		assertEquals(0, getInt(mode, "gradeDecay"), "combo in progress must pause decay");
	}

	@Test
	void onMoveGradeDecayPausedNearLock() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = decayEngine(mode);
		e.combo = 0;
		e.lockDelayNow = 29; // not < getLockDelay()-1 = 29

		mode.onMove(e, 0);

		assertEquals(0, getInt(mode, "gradeDecay"), "piece about to lock must pause decay");
	}

	@Test
	void onMoveGradeDecayClampsInternalGradeIndex() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = decayEngine(mode);
		e.combo = 0;
		set(mode, "gradeInternal", 40); // beyond decay table -> clamped to last (rate 10)
		set(mode, "gradeDecay", 9);

		mode.onMove(e, 0);

		assertEquals(0, getInt(mode, "gradeDecay"), "decay counter resets at the clamped rate");
		assertEquals(0, getInt(mode, "gradePoint"), "one grade point decays away");
	}

	private static GameEngine decayEngine(GradeMania2Mode mode) throws Exception {
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.statc[0] = 0;
		mode.lvupflag = true; // skip the new-piece block
		e.timerActive = true;
		e.lockDelayNow = 0;
		e.speed.lockDelay = 30;
		set(mode, "gradePoint", 1);
		return e;
	}

	// =======================================================================
	// L616: onMove roll already started
	// =======================================================================

	@Test
	void onMoveEndingIgnoredOnceRollStarted() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.ending = 2;
		mode.rollstarted = true;
		e.blockHidden = 123;
		e.owner.bgmStatus.bgm = 7;

		mode.onMove(e, 0);

		assertEquals(123, e.blockHidden, "roll setup must not run twice");
		assertEquals(7, e.owner.bgmStatus.bgm);
	}

	// =======================================================================
	// L641/642/644: onARE arms
	// =======================================================================

	@Test
	void onAreSkipsLevelUpDuringEnding() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.ending = 2;
		e.statc[0] = 5;
		e.statc[1] = 2;
		mode.lvupflag = false;
		mode.nextseclv = 100;
		e.statistics.level = 50;

		mode.onARE(e, 0);

		assertEquals(50, e.statistics.level);
		assertFalse(mode.lvupflag, "ending frames must not mark a pending level-up");
	}

	@Test
	void onAreAtSectionStopKeepsLevelButMarksPending() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.statc[0] = 1;
		e.statc[1] = 1;
		mode.lvupflag = false;
		mode.nextseclv = 100;
		e.statistics.level = 99; // already at nextseclv-1: no raise

		mode.onARE(e, 0);

		assertEquals(99, e.statistics.level);
		assertTrue(mode.lvupflag);
	}

	@Test
	void onAreLevelStopArms() throws Exception {
		for (boolean se : new boolean[] {true, false}) {
			GradeMania2Mode mode = new GradeMania2Mode();
			GameEngine e = fresh(mode);
			mode.playerInit(e, 0);
			e.ending = 0;
			e.statc[0] = 1;
			e.statc[1] = 1;
			mode.lvupflag = false;
			mode.nextseclv = 100;
			onOff(mode, "lvstopse").value = se;
			e.statistics.level = 98;

			mode.onARE(e, 0);

			assertEquals(99, e.statistics.level, "lvstopse=" + se);
			assertTrue(mode.lvupflag);
		}
	}

	// =======================================================================
	// L668/671/679: levelUp ghost / BGM-fade / RE arms
	// =======================================================================

	@Test
	void levelUpKeepsGhostWithAlwaysGhost() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		onOff(mode, "alwaysghost").value = true;
		e.statistics.level = 150;
		e.ghost = true;
		e.timerActive = false;

		levelUp(mode, e);

		assertTrue(e.ghost, "FULL GHOST must survive level 100+");
	}

	@Test
	void levelUpSkipsFadeoutOnLastBgmSlot() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		mode.bgmlv = 3; // tableBGMFadeout[3] == -1
		e.statistics.level = 990;
		e.timerActive = false;
		e.owner.bgmStatus.fadesw = false;

		levelUp(mode, e);

		assertFalse(e.owner.bgmStatus.fadesw, "no fadeout past the last BGM slot");
	}

	@Test
	void levelUpV2ReMedalSkippedWhenAlreadyMaxed() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0); // version 3 >= 2
		e.timerActive = true;
		set(mode, "medalRE", 3);
		e.createFieldIfNeeded();
		fillBlocks(e, 16);

		levelUp(mode, e);

		assertEquals(3, getInt(mode, "medalRE"));
		assertFalse(getBool(mode, "recoveryFlag"), "maxed RE medal must not re-arm recovery");
	}

	private static void levelUp(GradeMania2Mode mode, GameEngine e) throws Exception {
		Method m = GradeMania2Mode.class.getDeclaredMethod("levelUp", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, e);
	}

	// =======================================================================
	// L716: m-roll line counting requires ending == 2
	// =======================================================================

	@Test
	void calcScoreMrollLinesOnlyCountDuringRoll() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode);
		mode.mrollFlag = true;

		e.ending = 1; // not the roll -> not counted
		mode.calcScore(e, 0, 1);
		assertEquals(0, getInt(mode, "mrollLines"));

		e.ending = 2; // roll -> counted
		mode.calcScore(e, 0, 2);
		assertEquals(2, getInt(mode, "mrollLines"));
	}

	// =======================================================================
	// L725/730: grade point index clamps
	// =======================================================================

	@Test
	void calcScoreClampsInternalGradeIndexForPoints() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode);
		set(mode, "gradeInternal", 15); // > 10 -> clamped, single = 2 points

		mode.calcScore(e, 0, 1);

		assertEquals(2, getInt(mode, "gradePoint"));
	}

	@Test
	void calcScoreClampsComboBonusIndex() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode);
		e.combo = 20; // index 19 -> clamped to last single bonus (2.0)

		mode.calcScore(e, 0, 1);

		assertEquals(20, getInt(mode, "gradePoint"), "10 base * 2.0 clamped combo bonus");
	}

	// =======================================================================
	// L751: internal grade rollover below the promotion threshold
	// =======================================================================

	@Test
	void calcScoreGradePointRolloverWithoutPromotion() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode);
		mode.grade = 1; // needs gradeInternal >= 2 to promote
		set(mode, "gradePoint", 95);

		mode.calcScore(e, 0, 1); // +10 -> 105 >= 100 rollover

		assertEquals(0, getInt(mode, "gradePoint"));
		assertEquals(1, getInt(mode, "gradeInternal"));
		assertEquals(1, mode.grade, "grade must not advance before the threshold");
	}

	// =======================================================================
	// L767/772: SK medal totalFour arms
	// =======================================================================

	@Test
	void calcScoreSkMedalBigModeArms() throws Exception {
		int[][] cases = { {2, 1}, {4, 1}, {3, 0} }; // {totalFour, expected medal}
		for (int[] c : cases) {
			GradeMania2Mode mode = new GradeMania2Mode();
			GameEngine e = scoreEngine(mode);
			onOff(mode, "big").value = true;
			e.statistics.totalFour = c[0];

			mode.calcScore(e, 0, 4);

			assertEquals(c[1], mode.medalSK, "totalFour=" + c[0]);
		}
	}

	@Test
	void calcScoreSkMedalNormalModeArms() throws Exception {
		int[][] cases = { {20, 1}, {35, 1}, {25, 0} };
		for (int[] c : cases) {
			GradeMania2Mode mode = new GradeMania2Mode();
			GameEngine e = scoreEngine(mode);
			e.statistics.totalFour = c[0];

			mode.calcScore(e, 0, 4);

			assertEquals(c[1], mode.medalSK, "totalFour=" + c[0]);
		}
	}

	// =======================================================================
	// L783: AC medal already maxed
	// =======================================================================

	@Test
	void calcScoreAcMedalDoesNotExceedMax() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode); // empty field -> bravo
		mode.medalAC = 3;

		mode.calcScore(e, 0, 1);

		assertEquals(3, mode.medalAC);
	}

	// =======================================================================
	// L797/808: CO medal already maxed
	// =======================================================================

	@Test
	void calcScoreCoMedalAlreadyMaxed() throws Exception {
		// big mode, combo 4
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode);
		onOff(mode, "big").value = true;
		mode.medalCO = 3;
		e.combo = 4;
		mode.calcScore(e, 0, 1);
		assertEquals(3, mode.medalCO);

		// normal mode, combo 7
		mode = new GradeMania2Mode();
		e = scoreEngine(mode);
		mode.medalCO = 3;
		e.combo = 7;
		mode.calcScore(e, 0, 1);
		assertEquals(3, mode.medalCO);
	}

	// =======================================================================
	// L844: m-roll veto arms at level 999
	// =======================================================================

	@Test
	void mrollDeniedWhenSectionTimeCriterionFailed() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = ending999Engine(mode);
		set(mode, "mrollSectiontime", false);

		mode.calcScore(e, 0, 1);

		assertEquals(999, e.statistics.level);
		assertEquals(1, e.ending);
		assertFalse(mode.mrollFlag, "failed section-time criterion must veto the M-roll");
	}

	@Test
	void mrollDeniedWhenFourlineCriterionFailed() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = ending999Engine(mode);
		set(mode, "mrollFourline", false);

		mode.calcScore(e, 0, 1);

		assertEquals(1, e.ending);
		assertFalse(mode.mrollFlag, "failed tetris criterion must veto the M-roll");
	}

	@Test
	void mrollDeniedWhenTooSlow() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = ending999Engine(mode);
		e.statistics.time = 40000; // > M_ROLL_TIME_REQUIRE (31500)

		mode.calcScore(e, 0, 1);

		assertEquals(1, e.ending);
		assertFalse(mode.mrollFlag, "slow clear must veto the M-roll");
	}

	private static GameEngine ending999Engine(GradeMania2Mode mode) throws Exception {
		GameEngine e = scoreEngine(mode);
		mode.nextseclv = 999;
		e.statistics.level = 998;
		e.statistics.time = 100;
		mode.grade = 17;
		// keep mrollCheck from flipping the section-time criterion
		mode.sectiontime[8] = 1000;
		mode.sectiontime[9] = 1000;
		return e;
	}

	// =======================================================================
	// L856/878: last BGM slot at a section cross + nextseclv clamp
	// =======================================================================

	@Test
	void calcScoreSectionCrossOnLastBgmSlotClampsNextSection() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode);
		mode.bgmlv = 3; // tableBGMChange[3] == -1
		mode.nextseclv = 900;
		e.statistics.level = 899;
		e.owner.bgmStatus.bgm = 3;

		mode.calcScore(e, 0, 1);

		assertEquals(3, mode.bgmlv, "no BGM slot past the table end");
		assertEquals(3, e.owner.bgmStatus.bgm);
		assertEquals(999, mode.nextseclv, "nextseclv 1000 clamps to 999");
	}

	// =======================================================================
	// L874: RO medal check at the 300 boundary
	// =======================================================================

	@Test
	void calcScoreRoMedalAwardedAtSection300Boundary() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode);
		mode.nextseclv = 300;
		e.statistics.level = 299;
		e.nowPieceRotateCount = 4;
		e.statistics.totalPieceLocked = 1; // average 4.0 >= 1.2

		mode.calcScore(e, 0, 1);

		assertEquals(1, getInt(mode, "medalRO"));
		assertEquals(400, mode.nextseclv);
	}

	// =======================================================================
	// L879: level stop reached with the SE disabled
	// =======================================================================

	@Test
	void calcScoreLevelStopWithSeDisabled() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode);
		e.statistics.level = 97;

		mode.calcScore(e, 0, 2); // 99 == nextseclv-1, lvstopse off

		assertEquals(99, e.statistics.level);
		assertEquals(100, mode.nextseclv, "section must not advance at the stop level");
	}

	// =======================================================================
	// L893: negative speed bonus clamps to zero
	// =======================================================================

	@Test
	void calcScoreNegativeSpeedBonusClampsToZero() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = scoreEngine(mode);
		e.statistics.level = 10;
		e.speed.lockDelay = 30;
		e.statc[0] = 100; // lockDelay - statc[0] = -70 -> clamped to 0

		mode.calcScore(e, 0, 1);

		// ((10+1)/4) * 1 line * combo 1 * bravo 4 + (11/2) + 0 speed bonus = 13
		assertEquals(13, mode.lastscore);
		assertEquals(13, e.statistics.score);
	}

	private static GameEngine scoreEngine(GradeMania2Mode mode) throws Exception {
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.ending = 0;
		e.createFieldIfNeeded();
		mode.nextseclv = 100;
		e.statistics.level = 0;
		return e;
	}

	// =======================================================================
	// L918/921: onLast section time guards
	// =======================================================================

	@Test
	void onLastStopsSectionTimerDuringEnding() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.timerActive = true;
		e.ending = 2;
		e.gameActive = false;
		mode.sectiontime[0] = 5;

		mode.onLast(e, 0);

		assertEquals(5, mode.sectiontime[0], "section timer must freeze once the game ends");
		assertEquals(0, mode.rolltime, "roll clock only runs while the game is active");
	}

	@Test
	void onLastIgnoresOutOfRangeSections() throws Exception {
		for (int level : new int[] {-100, 1000}) {
			GradeMania2Mode mode = new GradeMania2Mode();
			GameEngine e = fresh(mode);
			mode.playerInit(e, 0);
			e.timerActive = true;
			e.ending = 0;
			e.statistics.level = level;

			mode.onLast(e, 0);

			for (int i = 0; i < mode.sectiontime.length; i++) {
				assertEquals(0, mode.sectiontime[i], "level " + level + " section " + i);
			}
		}
	}

	// =======================================================================
	// L967: onGameOver M-grade arms
	// =======================================================================

	@Test
	void onGameOverNoMGradeOutsideRoll() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		mode.mrollFlag = true;
		mode.grade = 17;
		e.ending = 1; // not the vanish roll
		e.statc[0] = 0;
		e.createFieldIfNeeded();

		mode.onGameOver(e, 0);

		assertEquals(17, mode.grade, "M grade needs the roll (ending == 2)");
	}

	@Test
	void onGameOverNoMGradeAfterFirstFrame() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		mode.mrollFlag = true;
		mode.grade = 17;
		e.ending = 2;
		e.statc[0] = 1; // not the first game-over frame
		e.blockOutlineType = GameEngine.BLOCK_OUTLINE_NONE;

		mode.onGameOver(e, 0);

		assertEquals(17, mode.grade);
		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, e.blockOutlineType,
				"outline reset only happens on frame 0");
	}

	// =======================================================================
	// L993/994: renderResult roll-clear grade colors
	// =======================================================================

	@Test
	void renderResultGradeColorFollowsRollClear() throws Exception {
		int[][] cases = {
			{3, EventReceiver.COLOR_GREEN},
			{2, EventReceiver.COLOR_ORANGE},
			{4, EventReceiver.COLOR_ORANGE},
		};
		for (int[] c : cases) {
			GradeMania2Mode mode = new GradeMania2Mode();
			Rec rec = new Rec();
			GameEngine e = fresh(mode, rec);
			mode.playerInit(e, 0);
			e.statc[1] = 0;
			mode.rollclear = c[0];

			mode.renderResult(e, 0);

			assertTrue(rec.menuHas(String.format("%10s", "9"), c[1]), "rollclear=" + c[0]);
		}
	}

	// =======================================================================
	// L1015/1019: renderResult page arms
	// =======================================================================

	@Test
	void renderResultPage1ShowsAverageWhenPresent() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.statc[1] = 1;
		mode.sectionavgtime = 1234;

		mode.renderResult(e, 0);

		assertTrue(rec.menuHas("AVERAGE", EventReceiver.COLOR_BLUE));
		assertTrue(rec.menu.stream().anyMatch(d -> d.s.equals(GeneralUtil.getTime(1234))));
	}

	@Test
	void renderResultUnknownPageDrawsOnlyHeader() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		Rec rec = new Rec();
		GameEngine e = fresh(mode, rec);
		mode.playerInit(e, 0);
		e.statc[1] = 3;

		mode.renderResult(e, 0);

		assertEquals(1, rec.menu.size());
		assertEquals("kn PAGE4/3", rec.menu.get(0).s);
	}

	// =======================================================================
	// L1044/1046/1048: saveReplay ranking gate arms
	// =======================================================================

	@Test
	void saveReplaySkipsRankingWhenAnyGateConditionFails() throws Exception {
		for (int variant = 0; variant < 5; variant++) {
			GradeMania2Mode mode = new GradeMania2Mode();
			GameEngine e = fresh(mode);
			mode.playerInit(e, 0);
			e.owner.replayProp = new CustomProperties();
			mode.grade = 5; // would rank first if updateRanking ran

			switch (variant) {
			case 0: e.owner.replayMode = true; break;
			case 1: startlevel(mode).value = 1; break;
			case 2: onOff(mode, "always20g").value = true; break;
			case 3: onOff(mode, "big").value = true; break;
			case 4: e.ai = new DummyAI(); break;
			}

			mode.saveReplay(e, 0, e.owner.replayProp);

			assertEquals(-1, mode.rankingRank, "variant " + variant + " must not rank");
			assertEquals(0, mode.rankingGrade[0], "variant " + variant + " must not write the table");
		}
	}

	@Test
	void saveReplayStMedalGoldUpdatesBestSectionTimes() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.owner.replayProp = new CustomProperties();
		mode.medalST = 3;
		mode.sectionIsNewRecord[0] = true;
		mode.sectiontime[0] = 100;

		mode.saveReplay(e, 0, e.owner.replayProp);

		assertEquals(100, mode.bestSectionTime[0]);
		assertEquals(100, e.owner.modeConfig.getProperty(
				"grademania2.bestSectionTime." + e.ruleopt.strRuleName + ".0", -1),
				"gold ST medal must persist the record");
	}

	@Test
	void saveReplayUnrankedWithoutGoldStSkipsSave() throws Exception {
		GradeMania2Mode mode = new GradeMania2Mode();
		GameEngine e = fresh(mode);
		mode.playerInit(e, 0);
		e.owner.replayProp = new CustomProperties();
		// all-zero result ties the empty table -> unranked, and medalST != 3

		mode.saveReplay(e, 0, e.owner.replayProp);

		assertEquals(-1, mode.rankingRank);
		assertEquals(-7777, e.owner.modeConfig.getProperty(
				"grademania2.ranking." + e.ruleopt.strRuleName + ".grade.0", -7777),
				"unranked game without gold ST must not persist anything");
	}

	// =======================================================================
	// Helpers
	// =======================================================================

	/** EventReceiver that records every core draw call. */
	private static final class Rec extends EventReceiver {
		static final class Draw {
			final int x, y, color;
			final float scale;
			final String s;
			Draw(int x, int y, String s, int color, float scale) {
				this.x = x; this.y = y; this.s = s; this.color = color; this.scale = scale;
			}
		}
		final List<Draw> score = new ArrayList<>();
		final List<Draw> menu = new ArrayList<>();
		int nextType = 0;
		int speedMeter = -1;

		@Override
		public void drawScoreFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
			score.add(new Draw(x, y, str, color, scale));
		}
		@Override
		public void drawMenuFont(GameEngine engine, int playerID, int x, int y, String str, int color, float scale) {
			menu.add(new Draw(x, y, str, color, scale));
		}
		@Override
		public int getNextDisplayType() {
			return nextType;
		}
		@Override
		public void drawSpeedMeter(GameEngine engine, int playerID, int x, int y, int s) {
			speedMeter = s;
		}

		boolean scoreHas(String s, int color) {
			return score.stream().anyMatch(d -> d.s.equals(s) && d.color == color);
		}
		boolean scoreHasText(String s) {
			return score.stream().anyMatch(d -> d.s.equals(s));
		}
		boolean scoreAt(int x, int y) {
			return score.stream().anyMatch(d -> d.x == x && d.y == y);
		}
		boolean menuHas(String s, int color) {
			return menu.stream().anyMatch(d -> d.s.equals(s) && d.color == color);
		}
	}

	private static GameEngine fresh(GradeMania2Mode mode) {
		return fresh(mode, new Rec());
	}

	private static GameEngine fresh(GradeMania2Mode mode, EventReceiver receiver) {
		GameManager m = new GameManager(receiver);
		m.mode = mode;
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		m.engine[0].owner.replayMode = false;
		return m.engine[0];
	}

	private static void fillBlocks(GameEngine e, int rows) {
		int w = e.field.getWidth();
		int h = e.field.getHeight();
		for (int y = h - 1; y > h - 1 - rows && y >= 0; y--) {
			for (int x = 0; x < w; x++) {
				e.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_RED));
			}
		}
	}

	private static IntegerMenuItem startlevel(GradeMania2Mode mode) throws Exception {
		return (IntegerMenuItem) priv(mode, "startlevel");
	}

	private static OnOffMenuItem onOff(GradeMania2Mode mode, String name) throws Exception {
		return (OnOffMenuItem) priv(mode, name);
	}

	private static Object priv(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.get(o);
	}

	private static int getInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static boolean getBool(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getBoolean(o);
	}

	private static void set(Object o, String n, Object v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.set(o, v);
	}

	private static Field findField(Class<?> cls, String n) throws NoSuchFieldException {
		for (Class<?> c = cls; c != null; c = c.getSuperclass())
			try { return c.getDeclaredField(n); } catch (NoSuchFieldException ex) { }
		throw new NoSuchFieldException(n);
	}
}
