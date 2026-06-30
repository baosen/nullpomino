package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Coverage-boosting tests for {@link SpeedMania2Mode} targeting the
 * previously-uncovered lines: the {@code onSetting} menu branches
 * (lvstopse / showsectiontime / big / torikan-wrap / gradedisp toggles,
 * BUTTON_F best-section toggle, BUTTON_A decide), the {@code renderLast}
 * ranking / best-section / in-game (grade, roll, regret, medals, section
 * time) branches, {@code onMove} lvupflag clearing, the {@code calcScore}
 * CO-medal / torikan / regret paths, and {@code renderResult} /
 * {@code onResult} page navigation.
 */
class SpeedMania2ModeCoverageBoostTest {

	// -----------------------------------------------------------------------
	// onSetting menu branches (lines 403-441)
	// -----------------------------------------------------------------------

	@Test
	void onSettingCursor1TogglesLvstopse() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 1);
		boolean before = readBoolean(mode, "lvstopse");

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, readBoolean(mode, "lvstopse"));
	}

	@Test
	void onSettingCursor2TogglesShowSectionTime() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 2);
		boolean before = readBoolean(mode, "showsectiontime");

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(!before, readBoolean(mode, "showsectiontime"));
	}

	@Test
	void onSettingCursor3TogglesBig() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 3);
		boolean before = readBoolean(mode, "big");

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, readBoolean(mode, "big"));
	}

	@Test
	void onSettingCursor4AdjustsTorikan() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 4);
		setInt(mode, "torikan", 1000);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		// torikan += 60 * change (change = +1)
		assertEquals(1060, readInt(mode, "torikan"));
	}

	@Test
	void onSettingCursor4TorikanWrapsBelowZero() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 4);
		setInt(mode, "torikan", 0);

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		// 0 - 60 = -60 < 0 -> wraps to 72000
		assertEquals(72000, readInt(mode, "torikan"));
	}

	@Test
	void onSettingCursor4TorikanWrapsAboveMax() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 4);
		setInt(mode, "torikan", 72000);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		// 72000 + 60 > 72000 -> wraps to 0
		assertEquals(0, readInt(mode, "torikan"));
	}

	@Test
	void onSettingCursor5TogglesGradedisp() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 5);
		boolean before = readBoolean(mode, "gradedisp");

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(!before, readBoolean(mode, "gradedisp"));
	}

	@Test
	void onSettingButtonFTogglesBestSectionTime() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10); // >= 5 so the F branch fires
		boolean before = readBoolean(mode, "isShowBestSectionTime");

		pressKey(engine, Controller.BUTTON_F);
		mode.onSetting(engine, 0);

		assertEquals(!before, readBoolean(mode, "isShowBestSectionTime"));
	}

	@Test
	void onSettingButtonADecidesAndReturnsFalse() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10); // >= 5 so the A branch fires
		setInt(mode, "sectionscomp", 7);

		pressKey(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);

		assertFalse(result, "BUTTON_A confirm should return false");
		assertEquals(0, readInt(mode, "sectionscomp"),
				"confirm resets sectionscomp");
	}

	@Test
	void onSettingButtonBQuits() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 0);

		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		setInt(mode, "menuTime", 59);

		boolean result = mode.onSetting(engine, 0);

		assertFalse(result, "replay path returns false once menuTime reaches 60");
		assertEquals(-1, readInt(mode, "menuCursor"));
	}

	// -----------------------------------------------------------------------
	// renderLast branches (lines 509-639)
	// -----------------------------------------------------------------------

	@Test
	void renderLastRankingScreen() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;
		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);
		engine.ai = null;
		setBoolean(mode, "isShowBestSectionTime", false);

		// Exercise rollclear colour branches in the ranking table.
		int[] rollclear = (int[]) readField(mode, "rankingRollclear");
		rollclear[0] = 1;
		rollclear[1] = 2;

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastBestSectionTimeScreen() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;
		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);
		engine.ai = null;
		setBoolean(mode, "isShowBestSectionTime", true); // best-section branch

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastInGameWithGradeRollRegretAndMedals() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE; // not SETTING/RESULT -> in-game branch

		setBoolean(mode, "gradedisp", true);
		setInt(mode, "grade", 3);
		setInt(mode, "gradeflash", 4); // gradeflash > 0 && %4 == 0
		setInt(mode, "lastscore", 500);
		setInt(mode, "scgettime", 60); // lastscore != 0 && scgettime > 0
		engine.statistics.score = 1234;
		engine.statistics.level = 250;

		// Roll-time display branch.
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 100);

		// REGRET display branch.
		setInt(mode, "regretdispframe", 4);

		// Medals shown.
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 3);
		setInt(mode, "medalCO", 1);

		// Section time display branch.
		setBoolean(mode, "showsectiontime", true);
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[2] = 1200;
		setInt(mode, "sectionavgtime", 1200);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastInGameNegativeLevelClampsToZero() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setBoolean(mode, "gradedisp", true);
		setInt(mode, "scgettime", 0); // lastscore==0 || scgettime<=0 branch
		engine.statistics.level = -5; // clamped to 0
		engine.speed.gravity = -1; // 20G speed branch -> speed = 40

		mode.renderLast(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onMove lvupflag clearing (line 657)
	// -----------------------------------------------------------------------

	@Test
	void onMoveClearsLvupflagWhenStatcPositive() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 1; // > 0
		setBoolean(mode, "lvupflag", true);

		mode.onMove(engine, 0);

		// version is CURRENT_VERSION (2) >= 1 -> lvupflag = false
		assertFalse(readBoolean(mode, "lvupflag"));
	}

	// -----------------------------------------------------------------------
	// calcScore CO-medal branches (lines 765-780)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreBigComboAwardsCoMedal() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		// Keep field non-empty so the AC-medal/bravo branch does not interfere.
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		setBoolean(mode, "big", true);
		engine.combo = 4; // big: combo>=2/3/4 -> medalCO escalates to 3

		mode.calcScore(engine, 0, 1);

		assertEquals(1, readInt(mode, "medalCO"),
				"big-mode first combo medal is CO level 1");
	}

	@Test
	void calcScoreNonBigComboAwardsCoMedal() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
		setBoolean(mode, "big", false);
		engine.combo = 4; // non-big: combo>=4 -> medalCO = 1

		mode.calcScore(engine, 0, 1);

		assertEquals(1, readInt(mode, "medalCO"));
	}

	// -----------------------------------------------------------------------
	// calcScore torikan / regret / next-section paths (819-858, 892-901)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreSectionAdvanceWithRegret() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 197; // +4 (triple) -> 201 >= nextseclv 200
		// Slow section time so it exceeds tableTimeRegret -> REGRET path (892-893).
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[1] = 5000;

		mode.calcScore(engine, 0, 3);

		assertEquals(180, readInt(mode, "regretdispframe"),
				"slow section time triggers REGRET display");
		assertEquals(300, readInt(mode, "nextseclv"),
				"nextseclv advances by 100");
	}

	@Test
	void calcScoreSectionAdvanceWithGradeRise() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 197;
		// Fast section time (well under regret) -> grade rise branch.
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[1] = 100;
		setInt(mode, "grade", 0);

		mode.calcScore(engine, 0, 3);

		assertEquals(1, readInt(mode, "grade"), "fast section raises grade");
		assertEquals(180, readInt(mode, "gradeflash"));
	}

	@Test
	void calcScoreTorikanKnockoutAtLevel500() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		setInt(mode, "nextseclv", 500);
		setInt(mode, "torikan", 1000);
		engine.statistics.level = 497; // +4 (triple) -> 501 >= 500
		engine.statistics.time = 5000; // > torikan -> knockout
		// Section time exceeds regret to exercise the regret branch inside KO too.
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[4] = 5000;

		mode.calcScore(engine, 0, 3);

		assertEquals(500, engine.statistics.level, "level pinned to 500 on KO");
		assertEquals(1, engine.ending, "torikan knockout sets ending = 1");
		assertFalse(engine.staffrollEnable, "staff roll disabled on KO");
	}

	@Test
	void calcScoreLevelStopSeWhenStoppedBeforeSection() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		setInt(mode, "nextseclv", 200);
		setBoolean(mode, "lvstopse", true);
		// level becomes nextseclv-1 == 199 and below nextseclv -> levelstop branch (901).
		engine.statistics.level = 198; // single -> +1 -> 199

		mode.calcScore(engine, 0, 1);

		assertEquals(199, engine.statistics.level);
	}

	// -----------------------------------------------------------------------
	// renderResult / onResult (lines 1009-1056)
	// -----------------------------------------------------------------------

	@Test
	void renderResultPage0WithSecretGrade() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0; // page 0
		setInt(mode, "grade", 5);
		setInt(mode, "rollclear", 2); // orange grade colour branch
		setInt(mode, "secretGrade", 10); // > 4 -> S.GRADE line

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage1SectionTimes() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 1; // page 1
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 1500;
		sectiontime[1] = 1600;
		setInt(mode, "sectionavgtime", 1550); // average line

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage2Medals() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 2; // page 2
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 3);
		setInt(mode, "medalCO", 1);

		mode.renderResult(engine, 0);
	}

	@Test
	void onResultPagesDownWrapsAndPlaysChange() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 2;

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onResult(engine, 0);

		assertEquals(0, engine.statc[1], "DOWN past page 2 wraps to 0");
	}

	@Test
	void onResultPagesUpWraps() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;

		pressKey(engine, Controller.BUTTON_UP);
		mode.onResult(engine, 0);

		assertEquals(2, engine.statc[1], "UP below page 0 wraps to 2");
	}

	@Test
	void onResultButtonFTogglesBestSectionTime() throws Exception {
		SpeedMania2Mode mode = new SpeedMania2Mode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		boolean before = readBoolean(mode, "isShowBestSectionTime");

		pressKey(engine, Controller.BUTTON_F);
		mode.onResult(engine, 0);

		assertEquals(!before, readBoolean(mode, "isShowBestSectionTime"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(SpeedMania2Mode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
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
}
