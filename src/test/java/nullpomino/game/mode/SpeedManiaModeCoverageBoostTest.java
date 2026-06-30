package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.menu.BooleanMenuItem;
import nullpomino.game.menu.IntegerMenuItem;

import org.junit.jupiter.api.Test;

/**
 * Coverage-boosting tests for {@link SpeedManiaMode}, targeting lines not
 * exercised by the existing helper / game-logic tests: onSetting F/B button
 * branches, renderLast (ranking, section-time view, in-game medal/section
 * display), levelUp v3 RE-medal recovery, calcScore SK/CO medal tiers,
 * BGM/grade transitions in the next-section branch, onLast roll fast-forward,
 * renderResult all three pages, and onResult F-toggle.
 */
class SpeedManiaModeCoverageBoostTest {

	// -----------------------------------------------------------------------
	// onSetting: F toggle (363-364) and B cancel (379)
	// -----------------------------------------------------------------------

	@Test
	void onSettingFButtonTogglesSectionTimeView() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuTime", 10);
		setBoolean(mode, "isShowBestSectionTime", false);

		pressKey(engine, Controller.BUTTON_F);
		mode.onSetting(engine, 0);

		assertTrue(readBoolean(mode, "isShowBestSectionTime"));
	}

	@Test
	void onSettingBButtonSetsQuitFlag() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuTime", 10);

		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingAButtonConfirmsAndReturnsFalse() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuTime", 10);
		setBoolean(mode, "isShowBestSectionTime", true);
		setInt(mode, "sectionscomp", 5);

		pressKey(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);

		assertFalse(result);
		assertFalse(readBoolean(mode, "isShowBestSectionTime"));
		assertEquals(0, readInt(mode, "sectionscomp"));
	}

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		setInt(mode, "menuTime", 59);

		boolean result = mode.onSetting(engine, 0);

		assertFalse(result);
		assertEquals(-1, readInt(mode, "menuCursor"));
	}

	// -----------------------------------------------------------------------
	// renderLast (440-538)
	// -----------------------------------------------------------------------

	@Test
	void renderLastRankingTableInSettingState() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;
		setIntMenuValue(mode, "startlevel", 0);
		setBoolMenuValue(mode, "big", false);
		engine.ai = null;
		setBoolean(mode, "isShowBestSectionTime", false);

		mode.renderLast(engine, 0); // ranking table branch
	}

	@Test
	void renderLastSectionTimeViewInSettingState() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;
		setIntMenuValue(mode, "startlevel", 0);
		setBoolMenuValue(mode, "big", false);
		engine.ai = null;
		setBoolean(mode, "isShowBestSectionTime", true);
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		for (int i = 0; i < bst.length; i++) bst[i] = 2520;
		boolean[] rec = (boolean[]) readField(mode, "sectionIsNewRecord");
		rec[0] = true;

		mode.renderLast(engine, 0); // SECTION TIME view branch (440-461)
	}

	@Test
	void renderLastInGameShowsScoreMedalsAndSectionTime() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;

		// grade display branch
		setInt(mode, "grade", 1);
		setInt(mode, "gradeflash", 4);
		// score with delta display
		setInt(mode, "lastscore", 500);
		setInt(mode, "scgettime", 60);
		engine.statistics.score = 1234;
		engine.statistics.level = 150;
		engine.statistics.time = 3600;
		// roll-rest branch
		engine.gameActive = true;
		engine.ending = 2;
		// all medals shown
		setInt(mode, "medalAC", 3);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 1);
		setInt(mode, "medalRE", 3);
		setInt(mode, "medalRO", 2);
		setInt(mode, "medalCO", 1);
		// section time display enabled
		setBoolMenuValue(mode, "showsectiontime", true);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 300;
		st[1] = 360;
		setInt(mode, "sectionavgtime", 330);

		mode.renderLast(engine, 0); // lines 463-540
	}

	@Test
	void renderLastInGameWithNegativeLevel() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statistics.level = -5; // tempLevel < 0 branch (line 483)
		setInt(mode, "grade", 0); // no grade display
		setInt(mode, "lastscore", 0); // plain score branch
		engine.gameActive = false;

		mode.renderLast(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onMove ending roll start (581)
	// -----------------------------------------------------------------------

	@Test
	void onMoveStartsRollWhenEnding() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 2;
		setBoolean(mode, "rollstarted", false);

		mode.onMove(engine, 0);

		assertTrue(readBoolean(mode, "rollstarted"));
	}

	// -----------------------------------------------------------------------
	// levelUp: v3 RE medal recovery (624-636 incl. 632-635)
	// -----------------------------------------------------------------------

	@Test
	void levelUpAwardsReMedalOnRecovery() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.timerActive = true;
		engine.statistics.level = 50;
		setInt(mode, "version", 3);
		setInt(mode, "medalRE", 0);
		setInt(mode, "nextseclv", 100);
		// field nearly empty (blocks <= 70), recoveryFlag already true
		setBoolean(mode, "recoveryFlag", true);

		invokeLevelUp(mode, engine);

		assertEquals(1, readInt(mode, "medalRE"));
		assertFalse(readBoolean(mode, "recoveryFlag"));
	}

	@Test
	void levelUpSetsRecoveryFlagWhenFieldFull() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.timerActive = true;
		engine.statistics.level = 50;
		setInt(mode, "version", 3);
		setInt(mode, "medalRE", 0);
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "recoveryFlag", false);
		// fill > 150 blocks
		fillFieldBlocks(engine, 160);

		invokeLevelUp(mode, engine);

		assertTrue(readBoolean(mode, "recoveryFlag"));
	}

	// -----------------------------------------------------------------------
	// calcScore SK medal (non-big), CO medal tiers, BGM/grade/levelstop
	// (670-671, 689-704, 768-770, 786-787, 793-794)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreSkMedalNonBigOnFifthFour() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5); // not empty
		setBoolMenuValue(mode, "big", false);
		engine.statistics.totalFour = 5; // triggers SK medal (670-671)
		setInt(mode, "medalSK", 0);
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	@Test
	void calcScoreCoMedalNonBigTiers() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5);
		setBoolMenuValue(mode, "big", false);
		engine.combo = 7; // highest non-big tier (703-704)
		setInt(mode, "medalCO", 0);
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 1);

		assertTrue(readInt(mode, "medalCO") >= 1);
	}

	@Test
	void calcScoreCoMedalBigTiers() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5);
		setBoolMenuValue(mode, "big", true);
		engine.big = true;
		engine.combo = 4; // big tier (694-697)
		setInt(mode, "medalCO", 0);
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 1);

		assertTrue(readInt(mode, "medalCO") >= 1);
	}

	@Test
	void calcScoreNextSectionBgmGradeAndLevelStop() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5);
		setBoolMenuValue(mode, "big", false);
		setBoolMenuValue(mode, "lvstopse", true);
		// Cross into section 500 to hit grade rise (786-787) and BGM switch.
		// tableBGMChange = {300, 500, -1}; set bgmlv so level>=500 triggers
		// the BGM switch block (768-770).
		setInt(mode, "bgmlv", 1);
		setInt(mode, "nextseclv", 500);
		engine.statistics.level = 497; // 497 + 4 = 501 >= nextseclv(500)

		mode.calcScore(engine, 0, 4);

		// crossed section: grade became 1, nextseclv advanced past 500
		assertEquals(1, readInt(mode, "grade"));
		assertTrue(readInt(mode, "nextseclv") > 500);
	}

	@Test
	void calcScoreLevelStopSoundNearSectionEnd() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5);
		setBoolMenuValue(mode, "lvstopse", true);
		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 198; // 198 + 1 = 199 == nextseclv-1 (793-794)

		mode.calcScore(engine, 0, 1);

		assertEquals(199, engine.statistics.level);
	}

	// -----------------------------------------------------------------------
	// onLast roll fast-forward with F held (836-837)
	// -----------------------------------------------------------------------

	@Test
	void onLastRollFastForwardWithFButton() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "version", 1);
		setInt(mode, "rolltime", 0);
		pressKey(engine, Controller.BUTTON_F);

		mode.onLast(engine, 0);

		assertEquals(5, readInt(mode, "rolltime"));
	}

	// -----------------------------------------------------------------------
	// renderResult: all three pages (874-914 incl. 878-901)
	// -----------------------------------------------------------------------

	@Test
	void renderResultPage0WithGradeAndSecretGrade() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;
		setInt(mode, "grade", 2); // grade display (877-880)
		setInt(mode, "secretGrade", 10); // secret grade > 4 (886-888)
		setInt(mode, "rankingRank", 0);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage1SectionTimesAndAverage() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 1; // SECTION page (890-901)
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 300;
		st[1] = 360;
		boolean[] rec = (boolean[]) readField(mode, "sectionIsNewRecord");
		rec[0] = true;
		setInt(mode, "sectionavgtime", 330); // average branch (899-901)

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultPage2Medals() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 2; // MEDAL page (903-913)
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 3);
		setInt(mode, "medalRE", 1);
		setInt(mode, "medalRO", 2);
		setInt(mode, "medalCO", 3);

		mode.renderResult(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onResult F-toggle (934-936)
	// -----------------------------------------------------------------------

	@Test
	void onResultFButtonTogglesSectionTimeView() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "isShowBestSectionTime", false);
		pressKey(engine, Controller.BUTTON_F);

		mode.onResult(engine, 0);

		assertTrue(readBoolean(mode, "isShowBestSectionTime"));
	}

	@Test
	void onResultUpDownChangesPage() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;

		pressKey(engine, Controller.BUTTON_UP);
		mode.onResult(engine, 0);
		assertEquals(2, engine.statc[1]); // wraps to 2

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1]); // wraps back to 0
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(SpeedManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void fillFieldBlocks(GameEngine engine, int count) {
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		int placed = 0;
		for (int y = h - 1; y >= 0 && placed < count; y--) {
			for (int x = 0; x < w && placed < count; x++) {
				engine.field.setBlock(x, y,
						new nullpomino.game.component.Block(
								nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
				placed++;
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

	private static void invokeLevelUp(SpeedManiaMode mode, GameEngine engine) throws Exception {
		Method m = SpeedManiaMode.class.getDeclaredMethod("levelUp", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
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

	private static void setBoolMenuValue(SpeedManiaMode mode, String menuFieldName, boolean value) throws Exception {
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
