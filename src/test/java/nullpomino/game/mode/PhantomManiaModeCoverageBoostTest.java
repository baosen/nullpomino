package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.menu.BooleanMenuItem;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage-boosting tests for {@link PhantomManiaMode} that exercise the
 * branches not reached by the existing helper/logic tests: onSetting F/B
 * button paths, renderLast (best-section-time view, in-game HUD with medals,
 * roll-time and section-time displays), onMove recovery-flag medal logic,
 * calcScore big/normal medal tiers and the LV300/500/800 torikan + regular
 * section-advance branches, onLast roll-time boost and ending transition,
 * renderResult all three pages, onResult page-flip navigation, and
 * saveReplay's ranking-save path.
 */
class PhantomManiaModeCoverageBoostTest {

	// -----------------------------------------------------------------------
	// onSetting: F button (flip view) and B button (quit)
	// -----------------------------------------------------------------------

	@Test
	void onSettingFButtonFlipsBestSectionTimeView() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuTime", 10);
		setBoolean(mode, "isShowBestSectionTime", false);

		pressKey(engine, Controller.BUTTON_F);
		boolean result = mode.onSetting(engine, 0);

		assertTrue(result, "onSetting keeps returning true while in menu");
		assertTrue(readBoolean(mode, "isShowBestSectionTime"),
				"F should flip the view flag");
	}

	@Test
	void onSettingBButtonSetsQuitFlag() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuTime", 10);

		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingAButtonStartsGame() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuTime", 10);

		pressKey(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);

		assertFalse(result, "A confirms and exits the setting screen");
		assertFalse(readBoolean(mode, "isShowBestSectionTime"));
	}

	// -----------------------------------------------------------------------
	// renderLast: best-section-time view (lines 464-485)
	// -----------------------------------------------------------------------

	@Test
	void renderLastBestSectionTimeView() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		engine.stat = GameEngine.Status.SETTING;
		setIntMenuValue(mode, "startlevel", 0);
		setBoolMenuValue(mode, "big", false);
		setBoolean(mode, "isShowBestSectionTime", true);

		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");
		isNew[0] = true;

		mode.renderLast(engine, 0);
		// Executes the SECTION TIME records rendering path without throwing.
	}

	@Test
	void renderLastLeaderboardView() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		engine.stat = GameEngine.Status.SETTING;
		setIntMenuValue(mode, "startlevel", 0);
		setBoolMenuValue(mode, "big", false);
		setBoolean(mode, "isShowBestSectionTime", false);

		int[] rollclear = (int[]) readField(mode, "rankingRollclear");
		rollclear[0] = 1;
		rollclear[1] = 2;
		setInt(mode, "rankingRank", 0);

		mode.renderLast(engine, 0);
	}

	// -----------------------------------------------------------------------
	// renderLast: in-game HUD else branch (lines 488-554)
	// -----------------------------------------------------------------------

	@Test
	void renderLastInGameHudWithMedalsRollTimeAndSectionTime() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE; // not SETTING / RESULT -> else branch

		engine.statistics.level = 150;
		engine.statistics.score = 1234;
		engine.statistics.time = 600;
		engine.speed.gravity = -1; // hits speed = 40 path
		engine.gameActive = true;
		engine.ending = 2;          // roll-time display

		setInt(mode, "lastscore", 50);
		setInt(mode, "scgettime", 60); // hits the "(+score)" string path
		setInt(mode, "gradeflash", 4); // gradeflash > 0 && %4==0
		setInt(mode, "grade", 1);
		setInt(mode, "nextseclv", 200);
		setInt(mode, "rolltime", 100);

		// all medals visible
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 3);
		setInt(mode, "medalRE", 1);
		setInt(mode, "medalRO", 2);
		setInt(mode, "medalCO", 3);

		// section-time display
		setBoolMenuValue(mode, "showsectiontime", true);
		setInt(mode, "sectionavgtime", 500);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 300;
		st[1] = 350; // index 1 == section (150/100) and ending!=0 path covered too

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastInGameHudNegativeLevelAndPositiveSpeed() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;

		engine.statistics.level = -5; // tempLevel clamps to 0
		engine.speed.gravity = 256;   // positive -> speed = gravity/128
		engine.gameActive = false;
		engine.ending = 0;

		setInt(mode, "lastscore", 0); // hits the plain-score string path
		setInt(mode, "grade", 0);

		// section time with current section active (ending == 0 -> "b" separator)
		setBoolMenuValue(mode, "showsectiontime", true);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 120; // section 0 active

		mode.renderLast(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onMove: recovery-flag medal logic (lines 572-593)
	// -----------------------------------------------------------------------

	@Test
	void onMoveSetsRecoveryFlagWhenFieldIsCrowded() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		engine.timerActive = true;
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "lvupflag", false);
		setBoolean(mode, "recoveryFlag", false);
		setInt(mode, "medalRE", 0);

		// Fill the field so getHowManyBlocks() >= 150
		engine.createFieldIfNeeded();
		fillField(engine);

		mode.onMove(engine, 0);

		assertTrue(readBoolean(mode, "recoveryFlag"),
				">=150 blocks should arm the recovery flag");
	}

	@Test
	void onMoveAwardsReMedalWhenFieldRecovers() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		engine.timerActive = true;
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "lvupflag", false);
		setBoolean(mode, "recoveryFlag", true);
		setInt(mode, "medalRE", 0);

		engine.createFieldIfNeeded(); // empty field -> blocks <= 70

		mode.onMove(engine, 0);

		assertFalse(readBoolean(mode, "recoveryFlag"));
		assertEquals(1, readInt(mode, "medalRE"),
				"recovering to <=70 blocks should award the RE medal");
	}

	@Test
	void onMoveClearsLvupFlagAndStartsRoll() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// statc[0] > 0 with ending 0 clears lvupflag (lines 588-590)
		engine.ending = 0;
		engine.statc[0] = 1;
		setBoolean(mode, "lvupflag", true);
		mode.onMove(engine, 0);
		assertFalse(readBoolean(mode, "lvupflag"));

		// ending == 2 starts the roll (lines 592-593)
		engine.ending = 2;
		setBoolean(mode, "rollstarted", false);
		mode.onMove(engine, 0);
		assertTrue(readBoolean(mode, "rollstarted"));
	}

	// -----------------------------------------------------------------------
	// calcScore: big-mode SK/CO medals (lines 654-695)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreBigModeAwardsSkAndCoMedals() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		// keep field non-empty to avoid all-clear path stealing the medal SE
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		engine.ending = 0;
		engine.statistics.level = 100;
		engine.statistics.totalFour = 1; // big-mode SK trigger
		engine.combo = 2;                 // big-mode CO trigger (medalCO 0 -> 1)
		setBoolMenuValue(mode, "big", true);
		setInt(mode, "nextseclv", 999); // avoid section-advance reset
		setInt(mode, "medalSK", 0);
		setInt(mode, "medalCO", 0);

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
		assertEquals(1, readInt(mode, "medalCO"));
	}

	@Test
	void calcScoreNormalModeAwardsCoMedal() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		engine.ending = 0;
		engine.statistics.level = 100;
		engine.statistics.totalFour = 5; // normal-mode SK trigger
		engine.combo = 4;                 // normal-mode CO trigger (medalCO 0 -> 1)
		setBoolMenuValue(mode, "big", false);
		setInt(mode, "nextseclv", 999);
		setInt(mode, "medalSK", 0);
		setInt(mode, "medalCO", 0);

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
		assertEquals(1, readInt(mode, "medalCO"));
	}

	// -----------------------------------------------------------------------
	// calcScore: torikan branches (lines 726-799) and regular section advance
	// -----------------------------------------------------------------------

	@Test
	void calcScoreLv300TorikanEndsGame() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		engine.ending = 0;
		engine.timerActive = true;
		engine.statistics.level = 299;
		engine.statistics.time = 9000; // > LV300TORIKAN (8880)
		setInt(mode, "nextseclv", 300);

		mode.calcScore(engine, 0, 1); // level -> 300

		assertEquals(300, engine.statistics.level);
		assertEquals(2, engine.ending);
		assertFalse(engine.timerActive);
	}

	@Test
	void calcScoreLv500TorikanEndsGame() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		engine.ending = 0;
		engine.timerActive = true;
		engine.statistics.level = 499;
		engine.statistics.time = 14000; // > LV500TORIKAN (13080)
		setInt(mode, "nextseclv", 500);

		mode.calcScore(engine, 0, 1); // level -> 500

		assertEquals(500, engine.statistics.level);
		assertEquals(2, engine.ending);
	}

	@Test
	void calcScoreLv800TorikanEndsGame() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		engine.ending = 0;
		engine.timerActive = true;
		engine.statistics.level = 799;
		engine.statistics.time = 20000; // > LV800TORIKAN (19380)
		setInt(mode, "nextseclv", 800);

		mode.calcScore(engine, 0, 1); // level -> 800

		assertEquals(800, engine.statistics.level);
		assertEquals(2, engine.ending);
	}

	@Test
	void calcScoreRegularSectionAdvanceUpdatesGradeAndBgm() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		engine.ending = 0;
		engine.timerActive = true;
		engine.statistics.level = 299;
		engine.statistics.time = 100; // under the torikan -> regular advance
		setIntMenuValue(mode, "startlevel", 0); // enables grade update loop
		setInt(mode, "nextseclv", 300); // hits the roMedalCheck (nextseclv == 300) path
		setInt(mode, "sectionfourline", 0); // < 2 -> gmfourline = false
		setBoolean(mode, "gmfourline", true);

		mode.calcScore(engine, 0, 1); // level -> 300 >= nextseclv

		assertEquals(400, readInt(mode, "nextseclv"),
				"section advance bumps nextseclv by 100");
		assertFalse(readBoolean(mode, "gmfourline"),
				"sectionfourline < 2 clears gmfourline");
	}

	@Test
	void calcScoreLevelStopSeBranch() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));

		engine.ending = 0;
		engine.statistics.level = 98; // -> 99 == nextseclv - 1
		setInt(mode, "nextseclv", 100);
		setBoolMenuValue(mode, "lvstopse", true);

		mode.calcScore(engine, 0, 1); // level becomes 99 == nextseclv-1

		assertEquals(99, engine.statistics.level,
				"line-clear at nextseclv-1 with lvstopse hits the levelstop branch");
	}

	// -----------------------------------------------------------------------
	// onLast: roll-time F boost and ending transition (lines 865-885)
	// -----------------------------------------------------------------------

	@Test
	void onLastRollTimeBoostedByFButton() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		engine.statistics.level = 500; // < 999 enables the +5 boost
		setInt(mode, "version", 1);
		setInt(mode, "rolltime", 0);

		pressPress(engine, Controller.BUTTON_F);
		mode.onLast(engine, 0);

		assertEquals(5, readInt(mode, "rolltime"),
				"F during roll with version>=1 and level<999 adds 5");
	}

	@Test
	void onLastRollTimeEndsExcellentWhenLimitReached() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		engine.statistics.level = 999;
		setInt(mode, "version", 1);
		setInt(mode, "rolltime", 1981); // +1 -> 1982 == ROLLTIMELIMIT

		mode.onLast(engine, 0);

		assertEquals(2, readInt(mode, "rollclear"),
				"reaching roll limit at level 999 sets rollclear=2");
		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	// -----------------------------------------------------------------------
	// renderResult: all three pages (lines 903-945) including secret grade
	// -----------------------------------------------------------------------

	@Test
	void renderResultPage0WithSecretGrade() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;
		setInt(mode, "grade", 6);
		setInt(mode, "rollclear", 2);
		setInt(mode, "secretGrade", 18); // > 4 -> S. GRADE line
		setInt(mode, "rankingRank", 0);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage1SectionTimes() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 1;
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 300;
		st[1] = 350;
		setInt(mode, "sectionavgtime", 325);
		boolean[] isNew = (boolean[]) readField(mode, "sectionIsNewRecord");
		isNew[0] = true;

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage2Medals() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 2;
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 3);
		setInt(mode, "medalRE", 1);
		setInt(mode, "medalRO", 2);
		setInt(mode, "medalCO", 3);

		mode.renderResult(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onResult: page-flip navigation (lines 954-968)
	// -----------------------------------------------------------------------

	@Test
	void onResultUpKeyWrapsToLastPage() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;

		pressKey(engine, Controller.BUTTON_UP);
		mode.onResult(engine, 0);

		assertEquals(2, engine.statc[1], "UP from page 0 wraps to page 2");
	}

	@Test
	void onResultDownKeyWrapsToFirstPage() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 2;

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onResult(engine, 0);

		assertEquals(0, engine.statc[1], "DOWN from page 2 wraps to page 0");
	}

	@Test
	void onResultFKeyFlipsView() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "isShowBestSectionTime", false);

		pressKey(engine, Controller.BUTTON_F);
		mode.onResult(engine, 0);

		assertTrue(readBoolean(mode, "isShowBestSectionTime"));
	}

	// -----------------------------------------------------------------------
	// saveReplay: ranking-save path (lines 981-988)
	// -----------------------------------------------------------------------

	@Test
	void saveReplaySavesRankingWhenQualified() throws Exception {
		PhantomManiaMode mode = new PhantomManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		setIntMenuValue(mode, "startlevel", 0);
		setBoolMenuValue(mode, "big", false);

		// Force a qualifying entry: grade/level/time good enough to rank.
		setInt(mode, "grade", 6);
		setInt(mode, "rollclear", 2);
		engine.statistics.level = 999;
		engine.statistics.time = 1000;
		setInt(mode, "medalST", 3); // also triggers updateBestSectionTime + save

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(1, prop.getProperty("phantommania.version", -1));
		// rankingRank should have been resolved (>=0 because we qualify).
		assertTrue(readInt(mode, "rankingRank") >= 0,
				"qualifying run should be placed on the leaderboard");
	}

	// =======================================================================
	// Helpers
	// =======================================================================

	private static GameEngine freshEngine(PhantomManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void fillField(GameEngine engine) {
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				engine.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
			}
		}
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void pressPress(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 3; // isPress true, isPush false
	}

	private static int readInt(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(Object mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(Object mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void setIntMenuValue(Object mode, String menuFieldName, int value) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		((IntegerMenuItem) f.get(mode)).value = value;
	}

	private static void setBoolMenuValue(Object mode, String menuFieldName, boolean value) throws Exception {
		Field f = findField(mode.getClass(), menuFieldName);
		f.setAccessible(true);
		((BooleanMenuItem) f.get(mode)).value = value;
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
