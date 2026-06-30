package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Additional coverage for {@link GradeMania3Mode} targeting branches not hit
 * by the existing GradeMania3 test suite: the replay-mode exam path in
 * playerInit, onSetting cursor-1 with M-ROLL level and exam-chance confirm,
 * renderSetting COOL color, renderLast ranking/exam/section/medal/roll-time
 * branches, renderReady, onMove level-up and ending-start, levelUp BGM
 * fadeout/change, calcScore SK/CO medals, the level-999 ending, lv500 torikan,
 * next-section COOL bonus, onLast roll-end grade-up, renderResult pass/fail
 * pages, onResult navigation, saveReplay ranking update, setPromotionalGrade,
 * and updateBestSectionTime.
 */
class GradeMania3ModeCoverageBoostTest {

	// -----------------------------------------------------------------------
	// playerInit: replay-mode exam path (lines 369-387)
	// -----------------------------------------------------------------------

	@Test
	void playerInitReplayModeWithPromotionalExam() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		engine.owner.replayMode = true;
		CustomProperties replay = engine.owner.replayProp;
		replay.setProperty("grademania3.enableexam", true);
		replay.setProperty("grademania3.exam", 20);
		replay.setProperty("grademania3.demopoint", 0);
		replay.setProperty("grademania3.demotionExamGrade", 0);

		mode.playerInit(engine, 0);

		assertEquals(20, readInt(mode, "promotionalExam"));
		assertTrue(readBoolean(mode, "promotionFlag"));
		assertEquals(100, readInt(mode, "readyframe"));
		assertEquals(600, readInt(mode, "passframe"));
	}

	@Test
	void playerInitReplayModeWithDemotionPoints() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		engine.owner.replayMode = true;
		CustomProperties replay = engine.owner.replayProp;
		replay.setProperty("grademania3.enableexam", true);
		replay.setProperty("grademania3.exam", 0);
		replay.setProperty("grademania3.demopoint", 30);
		replay.setProperty("grademania3.demotionExamGrade", 15);

		mode.playerInit(engine, 0);

		assertTrue(readBoolean(mode, "demotionFlag"));
		assertEquals(600, readInt(mode, "passframe"));
		assertEquals(30, readInt(mode, "demotionPoints"));
	}

	// -----------------------------------------------------------------------
	// onSetting cursor 1 with M-ROLL level (line 627) + section time toggle
	// (lines 674-675)
	// -----------------------------------------------------------------------

	@Test
	void onSettingCursor1WithMRollLevelClampsSpeed() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		setFieldInt(mode, "startlevel", 11);
		setFieldInt(mode, "internalStartLevel", 500);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(1200, readInt(mode, "internalStartLevel"));
	}

	@Test
	void onSettingFButtonTogglesBestSectionTime() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		boolean before = readBoolean(mode, "isShowBestSectionTime");

		pressKey(engine, Controller.BUTTON_F);
		mode.onSetting(engine, 0);

		assertEquals(!before, readBoolean(mode, "isShowBestSectionTime"));
	}

	// -----------------------------------------------------------------------
	// onSetting A button with exam chance -> promotion (lines 690-709)
	// -----------------------------------------------------------------------

	@Test
	void onSettingPressATriggersPromotionalExam() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);

		// Enable exam path: not 20g, not big, exam on
		setFieldBool(mode, "always20g", false);
		setFieldBool(mode, "big", false);
		setFieldBool(mode, "enableexam", true);

		// Build a grade history that yields a high promotional grade so that
		// promotionalExam > qualifiedGrade likely triggers the promotion flag.
		int[] history = (int[]) readField(mode, "gradeHistory");
		for (int i = 0; i < history.length; i++) history[i] = 20;
		setFieldInt(mode, "qualifiedGrade", 0);
		setFieldInt(mode, "demotionPoints", 40);

		// Retry until the random EXAM_CHANCE gate opens (1/3 each call).
		boolean entered = false;
		for (int attempt = 0; attempt < 200 && !entered; attempt++) {
			GradeMania3Mode m = new GradeMania3Mode();
			GameEngine e = freshEngine(m, false);
			setMenuState(e, m, 0, 10);
			setFieldBool(m, "always20g", false);
			setFieldBool(m, "big", false);
			setFieldBool(m, "enableexam", true);
			int[] h = (int[]) readField(m, "gradeHistory");
			for (int i = 0; i < h.length; i++) h[i] = 20;
			setFieldInt(m, "qualifiedGrade", 0);
			setFieldInt(m, "demotionPoints", 40);

			pressKey(e, Controller.BUTTON_A);
			boolean result = m.onSetting(e, 0);
			assertFalse(result, "A should confirm and return false");
			if (readBoolean(m, "promotionFlag")) {
				entered = true;
				assertEquals(100, readInt(m, "readyframe"));
				assertEquals(600, readInt(m, "passframe"));
			}
		}
		assertTrue(entered, "Promotion flag should have been set at least once");
	}

	// -----------------------------------------------------------------------
	// renderSetting COOL color path (line 740)
	// -----------------------------------------------------------------------

	@Test
	void renderSettingCoolColorAndRollLevels() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);

		setFieldInt(mode, "stcolor", 2);          // COOL
		setFieldInt(mode, "startlevel", 11);       // M-ROLL
		setFieldInt(mode, "internalStartLevel", 1200); // MAX
		setFieldInt(mode, "lv500torikan", 0);      // NONE

		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingNewRecordColorAndRollLevel() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);

		setFieldInt(mode, "stcolor", 1);          // NEWRECORD
		setFieldInt(mode, "startlevel", 10);       // ROLL
		setFieldInt(mode, "internalStartLevel", 500);
		setFieldInt(mode, "lv500torikan", 25200);

		mode.renderSetting(engine, 0);
	}

	// -----------------------------------------------------------------------
	// renderLast: ranking + exam display (lines 833-837)
	// -----------------------------------------------------------------------

	@Test
	void renderLastRankingWithExamQualifiedGrade() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.SETTING;
		setFieldInt(mode, "startlevel", 0);
		setFieldBool(mode, "big", false);
		setFieldBool(mode, "always20g", false);
		setFieldBool(mode, "enableexam", true);
		setFieldBool(mode, "isShowBestSectionTime", false);
		setFieldInt(mode, "qualifiedGrade", 12);

		// Mark some rankings with rollclear values to hit color branches.
		int[][] rollclear = (int[][]) readField(mode, "rankingRollclear");
		rollclear[0][1] = 1;
		rollclear[1][1] = 2;
		rollclear[2][1] = 3;
		rollclear[3][1] = 4;

		mode.renderLast(engine, 0);
	}

	// -----------------------------------------------------------------------
	// renderLast: best section time view (lines 840-863)
	// -----------------------------------------------------------------------

	@Test
	void renderLastBestSectionTimeView() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.SETTING;
		setFieldInt(mode, "startlevel", 0);
		setFieldBool(mode, "big", false);
		setFieldBool(mode, "always20g", false);
		setFieldBool(mode, "isShowBestSectionTime", true);

		mode.renderLast(engine, 0);
	}

	// -----------------------------------------------------------------------
	// renderLast: in-game promotion + gradedisp + score (lines 868-915)
	// -----------------------------------------------------------------------

	@Test
	void renderLastInGamePromotionGradeAndScore() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.MOVE;
		setFieldBool(mode, "promotionFlag", true);
		setFieldBool(mode, "gradedisp", true);
		setFieldBool(mode, "enableexam", true);
		setFieldInt(mode, "grade", 32);
		setFieldInt(mode, "qualifiedGrade", 10);
		setFieldInt(mode, "promotionalExam", 25);
		setFieldInt(mode, "gradeflash", 4);
		engine.statistics.level = 200;

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastInGameGradeDispWithLastScore() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.MOVE;
		setFieldBool(mode, "promotionFlag", false);
		setFieldBool(mode, "gradedisp", true);
		setFieldInt(mode, "lastscore", 500);
		setFieldInt(mode, "scgettime", 30);
		engine.statistics.score = 1000;
		engine.statistics.level = 100;

		mode.renderLast(engine, 0);
	}

	// -----------------------------------------------------------------------
	// renderLast: roll time + medals + section time (lines 912-967)
	// -----------------------------------------------------------------------

	@Test
	void renderLastRollTimeMedalsAndSectionTime() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 2;
		setFieldInt(mode, "rolltime", 100);

		// Medals
		setFieldInt(mode, "medalAC", 1);
		setFieldInt(mode, "medalST", 2);
		setFieldInt(mode, "medalSK", 3);
		setFieldInt(mode, "medalCO", 1);

		// Section time display with COOL color coding
		setFieldBool(mode, "showsectiontime", true);
		setFieldInt(mode, "stcolor", 2);
		setFieldInt(mode, "sectionavgtime", 3000);
		engine.statistics.level = 150;
		int[] st = (int[]) readField(mode, "sectiontime");
		for (int i = 0; i < st.length; i++) st[i] = 2000 + i * 100;
		boolean[] regret = (boolean[]) readField(mode, "regretsection");
		boolean[] cool = (boolean[]) readField(mode, "coolsection");
		regret[0] = true;
		cool[2] = true;

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastSectionTimeNewRecordColor() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.MOVE;
		engine.ending = 0;
		setFieldBool(mode, "showsectiontime", true);
		setFieldInt(mode, "stcolor", 1);
		engine.statistics.level = 250;
		int[] st = (int[]) readField(mode, "sectiontime");
		for (int i = 0; i < st.length; i++) st[i] = 1500;
		boolean[] newRec = (boolean[]) readField(mode, "sectionIsNewRecord");
		newRec[0] = true;

		mode.renderLast(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onReady: A/B press resets readyframe (line 984), demotion (991-992)
	// -----------------------------------------------------------------------

	@Test
	void onReadyPromotionAButtonResetsReadyframe() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", true);
		setFieldInt(mode, "readyframe", 50);

		pressKey(engine, Controller.BUTTON_A);
		boolean result = mode.onReady(engine, 0);

		assertEquals(0, readInt(mode, "readyframe"));
		assertFalse(result, "readyframe==0 should not hold ready");
	}

	@Test
	void onReadyDemotionFlagSetsGrayFrame() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", false);
		setFieldBool(mode, "demotionFlag", true);

		boolean result = mode.onReady(engine, 0);

		assertEquals(GameEngine.FRAME_COLOR_GRAY, engine.framecolor);
		assertFalse(result);
	}

	// -----------------------------------------------------------------------
	// renderReady: promotion display (lines 1004-1006)
	// -----------------------------------------------------------------------

	@Test
	void renderReadyShowsPromotionExam() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", true);
		setFieldInt(mode, "readyframe", 80);
		setFieldInt(mode, "promotionalExam", 25);

		mode.renderReady(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onMove: level-up new piece + ending start (lines 1048-1059)
	// -----------------------------------------------------------------------

	@Test
	void onMoveStartsEndingWhenEndingTwo() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		engine.ending = 2;
		setFieldBool(mode, "rollstarted", false);
		setFieldBool(mode, "mrollFlag", true);

		mode.onMove(engine, 0);

		assertTrue(readBoolean(mode, "rollstarted"));
		assertTrue(engine.blockShowOutlineOnly);
	}

	@Test
	void onMoveStartsEndingNonMRoll() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		engine.ending = 2;
		setFieldBool(mode, "rollstarted", false);
		setFieldBool(mode, "mrollFlag", false);

		mode.onMove(engine, 0);

		assertTrue(readBoolean(mode, "rollstarted"));
		assertEquals(300, engine.blockHidden);
		assertTrue(engine.blockHiddenAnim);
	}

	// -----------------------------------------------------------------------
	// levelUp: BGM fadeout + BGM change (lines 1109, 1114-1116)
	// -----------------------------------------------------------------------

	@Test
	void levelUpTriggersBgmFadeoutAndChange() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		// bgmlv=0 -> tableBGMFadeout[0]=485, tableBGMChange[0]=500
		setFieldInt(mode, "bgmlv", 0);
		setFieldInt(mode, "internalLevel", 500);
		engine.statistics.level = 500;

		invokeLevelUp(mode, engine);

		// internalLevel 500 >= tableBGMChange[0]=500 -> bgmlv increments
		assertEquals(1, readInt(mode, "bgmlv"));
		assertEquals(1, engine.owner.bgmStatus.bgm);
	}

	// -----------------------------------------------------------------------
	// calcScore: SK medal non-big (lines 1175-1176)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreSkMedalNonBig() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		engine.ending = 0;
		engine.statistics.level = 0;
		engine.statistics.totalFour = 10;
		setFieldBool(mode, "big", false);
		setFieldInt(mode, "gradeBasicPoint", 100);
		// fill field so not all-clear (keeps focus on SK branch deterministic)
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	// -----------------------------------------------------------------------
	// calcScore: CO medal non-big & big (lines 1194-1212)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreCoMedalNonBigCombo() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		engine.ending = 0;
		engine.statistics.level = 0;
		setFieldBool(mode, "big", false);
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		// The CO medal branches are else-if chained, so each tier is reached on
		// a separate call: combo>=4 -> 1, then combo>=5 -> 2, then combo>=7 -> 3.
		engine.combo = 4;
		mode.calcScore(engine, 0, 1);
		assertEquals(1, readInt(mode, "medalCO"));

		engine.combo = 5;
		mode.calcScore(engine, 0, 1);
		assertEquals(2, readInt(mode, "medalCO"));

		engine.combo = 7;
		mode.calcScore(engine, 0, 1);
		assertEquals(3, readInt(mode, "medalCO"));
	}

	@Test
	void calcScoreCoMedalBigCombo() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		engine.ending = 0;
		engine.statistics.level = 0;
		setFieldBool(mode, "big", true);
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		// Big-mode CO medal tiers: combo>=2 -> 1, combo>=3 -> 2, combo>=4 -> 3.
		engine.combo = 2;
		mode.calcScore(engine, 0, 1);
		assertEquals(1, readInt(mode, "medalCO"));

		engine.combo = 3;
		mode.calcScore(engine, 0, 1);
		assertEquals(2, readInt(mode, "medalCO"));

		engine.combo = 4;
		mode.calcScore(engine, 0, 1);
		assertEquals(3, readInt(mode, "medalCO"));
	}

	// -----------------------------------------------------------------------
	// calcScore: level 999 ending (lines 1230-1252)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreReaches999AndEntersEnding() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		engine.ending = 0;
		engine.statistics.level = 998;
		setFieldInt(mode, "nextseclv", 999);
		setFieldInt(mode, "grade", 30);
		setFieldInt(mode, "coolcount", 9);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[9] = 2000;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		assertEquals(999, engine.statistics.level);
		assertEquals(1, engine.ending);
		assertEquals(1, readInt(mode, "rollclear"));
		assertTrue(readBoolean(mode, "mrollFlag"),
				"mroll should trigger for grade>=24 & coolcount>=9 in version>=2");
	}

	// -----------------------------------------------------------------------
	// calcScore: lv500 torikan (lines 1257-1273)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreLv500Torikan() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		engine.ending = 0;
		engine.statistics.level = 499;
		engine.statistics.time = 30000;
		setFieldInt(mode, "nextseclv", 500);
		setFieldInt(mode, "lv500torikan", 25200);
		setFieldBool(mode, "promotionFlag", false);
		setFieldBool(mode, "demotionFlag", false);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[4] = 2000;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		assertEquals(999, engine.statistics.level);
		assertEquals(1, engine.ending);
		assertFalse(engine.staffrollEnable);
	}

	// -----------------------------------------------------------------------
	// calcScore: next-section with COOL bonus (lines 1285-1325)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreNextSectionWithCool() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		engine.ending = 0;
		engine.statistics.level = 99;
		setFieldInt(mode, "nextseclv", 100);
		setFieldInt(mode, "internalLevel", 99);
		setFieldBool(mode, "cool", true);
		setFieldBool(mode, "gradedisp", true);
		setFieldInt(mode, "grade", 5);
		setFieldInt(mode, "coolcount", 0);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 2000;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		assertTrue(readInt(mode, "coolcount") >= 1, "coolcount should increment");
		assertTrue(readBoolean(mode, "previouscool"));
		assertEquals(200, readInt(mode, "nextseclv"));
	}

	@Test
	void calcScoreNextSectionWithoutCool() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		engine.ending = 0;
		engine.statistics.level = 99;
		setFieldInt(mode, "nextseclv", 100);
		setFieldInt(mode, "internalLevel", 99);
		setFieldBool(mode, "cool", false);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 2000;
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.calcScore(engine, 0, 1);

		assertFalse(readBoolean(mode, "previouscool"));
		assertEquals(200, readInt(mode, "nextseclv"));
	}

	// -----------------------------------------------------------------------
	// onLast: roll end grade-up loop (lines 1430-1439)
	// -----------------------------------------------------------------------

	@Test
	void onLastRollEndAddsGradeFromRollPoints() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		engine.gameActive = true;
		engine.ending = 2;
		setFieldInt(mode, "rolltime", 3237); // becomes 3238 = ROLLTIMELIMIT
		setFieldBool(mode, "mrollFlag", true);
		setFieldFloat(mode, "rollPoints", 0.5f);
		setFieldBool(mode, "gradedisp", true);
		setFieldInt(mode, "grade", 10);

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
		assertEquals(2, readInt(mode, "rollclear"));
		assertTrue(readInt(mode, "grade") > 10, "grade should rise from roll points");
	}

	// -----------------------------------------------------------------------
	// renderResult: promotion pass/fail (lines 1502-1505)
	// -----------------------------------------------------------------------

	@Test
	void renderResultPromotionFail() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", true);
		setFieldInt(mode, "passframe", 400); // < 420
		setFieldInt(mode, "grade", 10);
		setFieldInt(mode, "promotionalExam", 25); // grade < exam => FAIL

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPromotionPass() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", true);
		setFieldInt(mode, "passframe", 400);
		setFieldInt(mode, "grade", 30);
		setFieldInt(mode, "promotionalExam", 25); // grade >= exam => PASS

		mode.renderResult(engine, 0);
	}

	// -----------------------------------------------------------------------
	// renderResult: demotion pass/fail (lines 1513-1516)
	// -----------------------------------------------------------------------

	@Test
	void renderResultDemotionFail() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", false);
		setFieldBool(mode, "demotionFlag", true);
		setFieldInt(mode, "passframe", 400);
		setFieldInt(mode, "grade", 5);
		setFieldInt(mode, "demotionExamGrade", 15); // grade < => FAIL

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultDemotionPass() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", false);
		setFieldBool(mode, "demotionFlag", true);
		setFieldInt(mode, "passframe", 400);
		setFieldInt(mode, "grade", 20);
		setFieldInt(mode, "demotionExamGrade", 15); // grade >= => PASS

		mode.renderResult(engine, 0);
	}

	// -----------------------------------------------------------------------
	// renderResult: page 0 secret grade (lines 1538-1539) & page 1 colors
	// (lines 1546-1559)
	// -----------------------------------------------------------------------

	@Test
	void renderResultPage0WithSecretGrade() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		setFieldInt(mode, "passframe", 0);
		engine.statc[1] = 0;
		setFieldInt(mode, "secretGrade", 6);
		setFieldInt(mode, "rollclear", 1);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage1SectionColors() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		setFieldInt(mode, "passframe", 0);
		engine.statc[1] = 1;
		setFieldInt(mode, "stcolor", 2);
		setFieldInt(mode, "sectionavgtime", 3000);
		int[] st = (int[]) readField(mode, "sectiontime");
		for (int i = 0; i < st.length; i++) st[i] = 2000;
		boolean[] regret = (boolean[]) readField(mode, "regretsection");
		boolean[] cool = (boolean[]) readField(mode, "coolsection");
		regret[0] = true;
		cool[1] = true;

		mode.renderResult(engine, 0);
	}

	// -----------------------------------------------------------------------
	// renderResult: page 2 roll points (lines 1569-1571)
	// -----------------------------------------------------------------------

	@Test
	void renderResultPage2WithRollPoints() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		setFieldInt(mode, "passframe", 0);
		engine.statc[1] = 2;
		setFieldInt(mode, "medalAC", 1);
		setFieldInt(mode, "medalST", 1);
		setFieldInt(mode, "medalSK", 1);
		setFieldInt(mode, "medalCO", 1);
		setFieldFloat(mode, "rollPointsTotal", 5.5f);

		mode.renderResult(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onResult: pass-frame paths + page switching (lines 1586-1633)
	// -----------------------------------------------------------------------

	@Test
	void onResultPromotionPassframeExcellent() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", true);
		setFieldInt(mode, "passframe", 420);
		setFieldInt(mode, "grade", 30);
		setFieldInt(mode, "promotionalExam", 25);

		boolean result = mode.onResult(engine, 0);

		assertTrue(result, "onResult holds while passframe>0");
		assertFalse(engine.allowTextRenderByReceiver);
		assertEquals(419, readInt(mode, "passframe"));
	}

	@Test
	void onResultDemotionPassframeGradeup() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", false);
		setFieldBool(mode, "demotionFlag", true);
		setFieldInt(mode, "passframe", 420);
		setFieldInt(mode, "grade", 20);
		setFieldInt(mode, "qualifiedGrade", 15);

		boolean result = mode.onResult(engine, 0);

		assertTrue(result);
		assertEquals(419, readInt(mode, "passframe"));
	}

	@Test
	void onResultPassframeButtonPressClamps() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldBool(mode, "promotionFlag", true);
		setFieldInt(mode, "passframe", 500);
		setFieldInt(mode, "grade", 10);
		setFieldInt(mode, "promotionalExam", 25);

		pressKey(engine, Controller.BUTTON_A);
		mode.onResult(engine, 0);

		// passframe>420 path clamps to 420, then decrements to 419
		assertEquals(419, readInt(mode, "passframe"));
	}

	@Test
	void onResultPageSwitchingDownUp() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "passframe", 0);
		engine.statc[1] = 0;

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onResult(engine, 0);
		assertEquals(1, engine.statc[1]);

		pressKey(engine, Controller.BUTTON_UP);
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1]);

		// UP wraps from 0 to 2
		pressKey(engine, Controller.BUTTON_UP);
		mode.onResult(engine, 0);
		assertEquals(2, engine.statc[1]);
	}

	@Test
	void onResultFButtonTogglesSectionTime() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "passframe", 0);
		boolean before = readBoolean(mode, "isShowBestSectionTime");

		pressKey(engine, Controller.BUTTON_F);
		mode.onResult(engine, 0);

		assertEquals(!before, readBoolean(mode, "isShowBestSectionTime"));
	}

	// -----------------------------------------------------------------------
	// saveReplay: ranking update path (lines 1658-1672)
	// -----------------------------------------------------------------------

	@Test
	void saveReplayUpdatesRankingNonExam() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		setFieldInt(mode, "startlevel", 0);
		setFieldBool(mode, "always20g", false);
		setFieldBool(mode, "big", false);
		setFieldBool(mode, "enableexam", false);
		setFieldInt(mode, "grade", 20);
		setFieldInt(mode, "medalST", 3);
		engine.statistics.level = 999;

		mode.saveReplay(engine, 0, engine.owner.replayProp);

		// Ranking update should have run (rank >= 0 since beats empty table).
		assertTrue(readInt(mode, "rankingRank") >= 0);
		assertEquals(20, engine.owner.replayProp.getProperty("result.grade.number", -1));
	}

	@Test
	void saveReplayExamUpdatesGradeHistory() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		setFieldInt(mode, "startlevel", 0);
		setFieldBool(mode, "always20g", false);
		setFieldBool(mode, "big", false);
		setFieldBool(mode, "enableexam", true);
		setFieldBool(mode, "promotionFlag", true);
		setFieldInt(mode, "grade", 33); // >=32 with qualifiedGrade<32 -> clamps to 31
		setFieldInt(mode, "qualifiedGrade", 10);
		engine.statistics.level = 999;

		mode.saveReplay(engine, 0, engine.owner.replayProp);

		// During an exam, rankingRank is forced to -1.
		assertEquals(-1, readInt(mode, "rankingRank"));
		int[] history = (int[]) readField(mode, "gradeHistory");
		assertEquals(33, history[0], "grade history records raw grade");
	}

	// -----------------------------------------------------------------------
	// setPromotionalGrade (lines 1812-1832)
	// -----------------------------------------------------------------------

	@Test
	void setPromotionalGradeReturnsZeroWhenHistoryHasUnset() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		int[] history = (int[]) readField(mode, "gradeHistory");
		for (int i = 0; i < history.length; i++) history[i] = -1; // all unset

		invokeSetPromotionalGrade(mode);

		assertEquals(0, readInt(mode, "promotionalExam"));
	}

	@Test
	void setPromotionalGradeComputesHighGrade() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		int[] history = (int[]) readField(mode, "gradeHistory");
		// More than 3 entries >= 18 should yield a promotional grade.
		for (int i = 0; i < history.length; i++) history[i] = 18;
		setFieldInt(mode, "qualifiedGrade", 10);

		invokeSetPromotionalGrade(mode);

		assertTrue(readInt(mode, "promotionalExam") > 0,
				"promotional exam grade should be set");
	}

	@Test
	void setPromotionalGradeClampsGmWhenNotQualified() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		int[] history = (int[]) readField(mode, "gradeHistory");
		// All GM (32) -> promotionalExam would be 32 but clamps to 31 when
		// qualifiedGrade < 31.
		for (int i = 0; i < history.length; i++) history[i] = 32;
		setFieldInt(mode, "qualifiedGrade", 10);

		invokeSetPromotionalGrade(mode);

		assertEquals(31, readInt(mode, "promotionalExam"));
	}

	// -----------------------------------------------------------------------
	// updateBestSectionTime (lines 1838-1844)
	// -----------------------------------------------------------------------

	@Test
	void updateBestSectionTimeWritesNewRecords() throws Exception {
		GradeMania3Mode mode = new GradeMania3Mode();
		GameEngine engine = freshEngine(mode, false);
		mode.playerInit(engine, 0);

		setFieldBool(mode, "enableexam", false);
		boolean[] newRec = (boolean[]) readField(mode, "sectionIsNewRecord");
		int[] st = (int[]) readField(mode, "sectiontime");
		newRec[2] = true;
		st[2] = 1234;

		invokeUpdateBestSectionTime(mode);

		int[][] best = (int[][]) readField(mode, "bestSectionTime");
		assertEquals(1234, best[2][0]);
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(GradeMania3Mode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		mode.modeInit(manager);
		return manager.engine[0];
	}

	private static void setMenuState(GameEngine engine, GradeMania3Mode mode, int cursor)
			throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, GradeMania3Mode mode, int cursor, int menuTime)
			throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++)
			engine.ctrl.buttonTime[i] = 0;
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static int readInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static Object readField(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).get(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setFieldFloat(Object obj, String name, float value) throws Exception {
		findField(obj.getClass(), name).setFloat(obj, value);
	}

	private static void setFieldBool(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
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

	private static void invokeLevelUp(GradeMania3Mode mode, GameEngine engine) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("levelUp", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeSetPromotionalGrade(GradeMania3Mode mode) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("setPromotionalGrade");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeUpdateBestSectionTime(GradeMania3Mode mode) throws Exception {
		Method m = GradeMania3Mode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
