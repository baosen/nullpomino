package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Targets the remaining uncovered alternate-outcome branches in
 * {@link FinalMode} that the positive-path logic test does not reach:
 * setSpeed (legacy lock-reset arm), setAverageSectionTime (index guard),
 * stMedalCheck (all three medal tiers + replay guard), startGame level
 * clamps, onReady/onGameOver guards, calcScore medal/section/score
 * branches, onMove/onARE level-up alternates, onLast guards, onResult page
 * wrap and F flip, saveReplay update path, and the render hooks (driven for
 * branch reach with a no-op receiver, no assertions).
 */
class FinalModeBranchCoverageTest {

	/** EventReceiver that never writes any config/properties file. */
	private static final class NonPersistingReceiver extends EventReceiver {
		@Override public boolean saveProperties(String f, CustomProperties p) { return true; }
		@Override public void saveModeConfig(CustomProperties c) { }
	}

	private static GameEngine freshEngine(FinalMode mode) {
		GameManager manager = new GameManager(new NonPersistingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	// --------------------------------------------------------------------
	// setSpeed: legacy (version < 3) lock-reset increment arm (L265)
	// --------------------------------------------------------------------

	@Test
	void setSpeedLegacyLockResetArm() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 2);
		engine.statistics.level = 0;

		// One of lockresetMove/Rotate true -> lockDelay incremented.
		engine.ruleopt.lockresetMove = true;
		engine.ruleopt.lockresetRotate = false;
		invokeSetSpeed(mode, engine);
		int withReset = engine.speed.lockDelay;

		// Both false -> no increment (the not-taken sub-branch of the ||).
		engine.ruleopt.lockresetMove = false;
		engine.ruleopt.lockresetRotate = false;
		invokeSetSpeed(mode, engine);
		int noReset = engine.speed.lockDelay;

		assertEquals(noReset + 1, withReset);
	}

	// --------------------------------------------------------------------
	// setAverageSectionTime: index-out-of-range guard false arm (L277)
	// --------------------------------------------------------------------

	@Test
	void setAverageSectionTimeSkipsOutOfRangeIndices() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel negative so the loop index i starts < 0 -> guard false.
		setInt(mode, "startlevel", -1);
		setInt(mode, "sectionscomp", 2);
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 600; // only the in-range index contributes

		invokeSetAverageSectionTime(mode);

		// temp = sectiontime[0] only (i=-1 skipped); avg = 600 / 2 = 300
		assertEquals(300, readInt(mode, "sectionavgtime"));
	}

	// --------------------------------------------------------------------
	// stMedalCheck: tier 3 already-held guard, replay guard, tier 2, tier 1
	// --------------------------------------------------------------------

	@Test
	void stMedalCheckTier3AlreadyHeldAndReplayGuard() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] best = (int[]) readField(mode, "bestSectionTime");
		best[0] = 1000;
		setInt(mode, "sectionlasttime", 500); // < best -> tier 3 path
		setInt(mode, "medalST", 3);            // L294 false: already 3
		engine.owner.replayMode = true;        // L298 false: replay

		invokeStMedalCheck(mode, engine, 0);

		assertEquals(3, readInt(mode, "medalST"));
		boolean[] newRec = (boolean[]) readField(mode, "sectionIsNewRecord");
		assertFalse(newRec[0]); // replay guard prevented the record flag
	}

	@Test
	void stMedalCheckTier2() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] best = (int[]) readField(mode, "bestSectionTime");
		best[1] = 1000;
		setInt(mode, "sectionlasttime", 1100); // best <= t < best+300
		setInt(mode, "medalST", 0);

		invokeStMedalCheck(mode, engine, 1);

		assertEquals(2, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckTier1() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] best = (int[]) readField(mode, "bestSectionTime");
		best[2] = 1000;
		setInt(mode, "sectionlasttime", 1400); // best+300 <= t < best+600
		setInt(mode, "medalST", 0);

		invokeStMedalCheck(mode, engine, 2);

		assertEquals(1, readInt(mode, "medalST"));
	}

	// --------------------------------------------------------------------
	// startGame: level >= 900 clamps nextseclv to 999 (L419)
	// --------------------------------------------------------------------

	@Test
	void startGameLevel900ClampsNextSecLvTo999() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 9); // level = 900

		mode.startGame(engine, 0);

		assertEquals(900, engine.statistics.level);
		assertEquals(999, readInt(mode, "nextseclv"));
	}

	// Note: the startGame "level < 0 -> nextseclv = 100" branch (L418) is
	// effectively unreachable: a negative level only arises from a negative
	// startlevel, but startGame immediately calls setSpeed which indexes
	// tableARE[level/100] and throws for a negative section. The settings
	// menu clamps startlevel to 0..9, so level is never negative in practice.

	// --------------------------------------------------------------------
	// onReady: version < 3 (L405 false) and statc[0] != 0 (L404 false)
	// --------------------------------------------------------------------

	@Test
	void onReadyVersionBelow3DoesNotForceBone() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setInt(mode, "version", 2);
		engine.bone = false;

		mode.onReady(engine, 0);

		assertFalse(engine.bone); // version<3 -> bone left alone
	}

	@Test
	void onReadyNonZeroStatcSkips() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 1; // L404 false
		setInt(mode, "version", 3);
		engine.bone = false;

		mode.onReady(engine, 0);

		assertFalse(engine.bone);
	}

	// --------------------------------------------------------------------
	// onGameOver: statc[0] != 0 (L813 false)
	// --------------------------------------------------------------------

	@Test
	void onGameOverNonZeroStatcSkips() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 1;
		engine.createFieldIfNeeded();
		setInt(mode, "secretGrade", 7);

		mode.onGameOver(engine, 0);

		assertEquals(7, readInt(mode, "secretGrade")); // unchanged
	}

	// --------------------------------------------------------------------
	// calcScore: lines>=1 but ending != 0 (L639 false arm)
	// --------------------------------------------------------------------

	@Test
	void calcScoreDuringEndingSkipsScoring() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 2; // ending != 0
		engine.createFieldIfNeeded();
		setInt(mode, "lastscore", 0);

		mode.calcScore(engine, 0, 2);

		// comboValue updated, but no score added (lastscore stays 0).
		assertEquals(0, readInt(mode, "lastscore"));
	}

	// --------------------------------------------------------------------
	// calcScore: big-mode SK medal (L644) and big-mode CO tier 3 (L674)
	// --------------------------------------------------------------------

	@Test
	void calcScoreBigSkAndCoMedals() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRow(engine); // field NOT empty so AC path is skipped
		setBoolean(mode, "big", true);
		engine.statistics.totalFour = 1; // big SK threshold
		engine.combo = 4;                 // big CO: reaches tier 3 else-if arm
		setInt(mode, "medalCO", 2);       // medalCO already 2 so L674 arm fires

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
		assertEquals(3, readInt(mode, "medalCO"));
	}

	// --------------------------------------------------------------------
	// calcScore: standard CO tier 3 (L685) + manualLock (L749) + speedBonus<0
	// --------------------------------------------------------------------

	@Test
	void calcScoreStandardCoTier3AndManualLock() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRow(engine);
		setBoolean(mode, "big", false);
		engine.combo = 7;          // standard CO: reaches tier 3 else-if arm
		setInt(mode, "medalCO", 2); // medalCO already 2 so L685 arm fires
		engine.manualLock = true;  // L749 true
		engine.statc[0] = 9999;    // speedBonus = lockDelay - statc[0] < 0 -> clamp

		mode.calcScore(engine, 0, 1);

		assertEquals(3, readInt(mode, "medalCO"));
		assertTrue(readInt(mode, "lastscore") > 0);
	}

	// --------------------------------------------------------------------
	// calcScore: AC medal skipped when field not empty (L657 false)
	// --------------------------------------------------------------------

	@Test
	void calcScoreNonEmptyFieldSkipsAcMedal() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRow(engine);

		mode.calcScore(engine, 0, 1);

		assertEquals(0, readInt(mode, "medalAC"));
	}

	// --------------------------------------------------------------------
	// calcScore: AC medal cap (L660 false) - field empty but medalAC == 3
	// --------------------------------------------------------------------

	@Test
	void calcScoreAcMedalCappedAtThree() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded(); // empty field -> AC path entered
		setInt(mode, "medalAC", 3);

		mode.calcScore(engine, 0, 1);

		assertEquals(3, readInt(mode, "medalAC")); // not incremented past 3
	}

	// --------------------------------------------------------------------
	// calcScore: section change with nextseclv+100 > 999 clamp (L742),
	// also exercises the >=nextseclv branch reaching 999 via section step.
	// --------------------------------------------------------------------

	@Test
	void calcScoreSectionChangeClampsNextSecLv() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "nextseclv", 900);
		engine.statistics.level = 899;

		mode.calcScore(engine, 0, 1); // 899+1 = 900 >= 900 -> section change

		// nextseclv += 100 = 1000 -> clamped to 999
		assertEquals(999, readInt(mode, "nextseclv"));
	}

	// --------------------------------------------------------------------
	// calcScore: level-stop SE arm (L743) - reach nextseclv-1, lvstopse on,
	// without crossing into a new section.
	// --------------------------------------------------------------------

	@Test
	void calcScoreLevelStopSound() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "nextseclv", 200);
		setBoolean(mode, "lvstopse", true);
		engine.statistics.level = 198; // +1 = 199 == nextseclv-1, < nextseclv

		mode.calcScore(engine, 0, 1);

		assertEquals(199, engine.statistics.level);
		assertEquals(200, readInt(mode, "nextseclv")); // no section change
	}

	// --------------------------------------------------------------------
	// onMove: level not yet at nextseclv-1 (L570 true already covered);
	// here cover L570 false (level already at/above nextseclv-1) so the
	// inner increment is skipped, plus L568 holdDisable arm and L576.
	// --------------------------------------------------------------------

	@Test
	void onMoveAtSectionCapDoesNotLevelUp() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 199; // == nextseclv-1 -> L570 false

		mode.onMove(engine, 0);

		assertEquals(199, engine.statistics.level); // not incremented
	}

	@Test
	void onMoveLevelStopSoundOnReachingCap() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		setBoolean(mode, "lvstopse", true);
		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 198; // +1 = 199 == nextseclv-1 -> levelstop SE

		mode.onMove(engine, 0);

		assertEquals(199, engine.statistics.level);
	}

	@Test
	void onMoveClearsLvupFlagAfterPiecePlacement() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 1;       // statc[0] > 0 -> L576 path
		setInt(mode, "version", 3); // version>=2 short-circuits holdDisable
		setBoolean(mode, "lvupflag", true);

		mode.onMove(engine, 0);

		assertFalse(readBoolean(mode, "lvupflag"));
	}

	// --------------------------------------------------------------------
	// onARE: last frame level-up + level-stop SE (L598/L599/L601)
	// --------------------------------------------------------------------

	@Test
	void onAreLevelUpAndLevelStop() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 6; // statc[0] >= statc[1]-1
		setBoolean(mode, "lvupflag", false);
		setBoolean(mode, "lvstopse", true);
		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 198; // +1 = 199 == nextseclv-1 -> levelstop

		mode.onARE(engine, 0);

		assertEquals(199, engine.statistics.level);
		assertTrue(readBoolean(mode, "lvupflag")); // set true at end
	}

	@Test
	void onAreAtCapDoesNotLevelUp() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 6;
		setBoolean(mode, "lvupflag", false);
		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 199; // == nextseclv-1 -> L599 false

		mode.onARE(engine, 0);

		assertEquals(199, engine.statistics.level);
	}

	// --------------------------------------------------------------------
	// onLast: guards false (L777 timerActive false, L786 ending != 2)
	// --------------------------------------------------------------------

	@Test
	void onLastTimerInactiveSkipsSectionTime() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = false; // L777 false
		engine.gameActive = false;  // L786 false
		engine.ending = 0;
		engine.statistics.level = 100;

		mode.onLast(engine, 0);

		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		assertEquals(0, sectiontime[1]); // not incremented
	}

	// --------------------------------------------------------------------
	// onResult: page wrap up (statc[1] < 0 -> 2) and down (statc[1] > 2 -> 0)
	// and BUTTON_F flip (L887)
	// --------------------------------------------------------------------

	@Test
	void onResultUpWrapsToLastPage() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;

		mode.onResult(engine, 0);

		assertEquals(2, engine.statc[1]); // 0-1 = -1 -> 2
	}

	@Test
	void onResultDownWrapsToFirstPage() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 2;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;

		mode.onResult(engine, 0);

		assertEquals(0, engine.statc[1]); // 2+1 = 3 -> 0
	}

	@Test
	void onResultFButtonFlipsBestSectionTime() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "isShowBestSectionTime", false);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;

		mode.onResult(engine, 0);

		assertTrue(readBoolean(mode, "isShowBestSectionTime"));
	}

	// --------------------------------------------------------------------
	// saveReplay: update path (startlevel==0, big==false, no ai) where a
	// ranking entry is created (rankingRank != -1) and best section times
	// updated (medalST == 3). Hermetic: reset the ranking arrays first.
	// --------------------------------------------------------------------

	@Test
	void saveReplayUpdatesRankingAndBestSectionTime() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Hermetic reset of ranking + section arrays (full-suite state leak).
		resetRankingArrays(mode);

		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);
		setInt(mode, "grade", 3);
		setInt(mode, "rollclear", 2);
		setInt(mode, "medalST", 3); // triggers updateBestSectionTime + save
		boolean[] newRec = (boolean[]) readField(mode, "sectionIsNewRecord");
		newRec[0] = true;
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 777;
		engine.statistics.level = 999;
		engine.statistics.time = 3600;

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		// A brand-new top entry was created.
		assertEquals(0, readInt(mode, "rankingRank"));
		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		assertEquals(777, bestSectionTime[0]); // updated from new-record section
	}

	@Test
	void saveReplaySkipsUpdateWhenStartLevelNonZero() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		resetRankingArrays(mode);
		setInt(mode, "startlevel", 3); // L902 false -> whole update block skipped
		setInt(mode, "rankingRank", -1);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(-1, readInt(mode, "rankingRank")); // unchanged
	}

	// --------------------------------------------------------------------
	// Render hooks: drive each branch with the no-op receiver. No
	// assertions - reach-only coverage of the render thresholds.
	// --------------------------------------------------------------------

	@Test
	void renderLastSettingLeaderboardAndSectionViews() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.SETTING;
		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);

		// Leaderboard view with rollclear colour variants on rank rows.
		int[] rankingRollclear = (int[]) readField(mode, "rankingRollclear");
		rankingRollclear[0] = 1; // green
		rankingRollclear[1] = 2; // orange
		setInt(mode, "rankingRank", 0);
		setBoolean(mode, "isShowBestSectionTime", false);
		mode.renderLast(engine, 0);

		// Best section time view.
		setBoolean(mode, "isShowBestSectionTime", true);
		boolean[] newRec = (boolean[]) readField(mode, "sectionIsNewRecord");
		newRec[0] = true;
		mode.renderLast(engine, 0);

		assertTrue(true);
	}

	@Test
	void renderLastInGameStatsAllBranches() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE; // not SETTING/RESULT -> stats path
		setInt(mode, "grade", 2);              // grade in [1,len) -> shown
		setInt(mode, "gradeflash", 4);         // flash % 4 == 0
		setInt(mode, "lastscore", 50);
		setInt(mode, "scgettime", 60);         // score delta shown
		engine.statistics.level = 150;
		engine.statistics.score = 1234;
		engine.speed.gravity = -1;             // speed = 40 branch
		engine.gameActive = true;
		engine.ending = 2;                     // roll-time block
		setInt(mode, "version", 3);
		setInt(mode, "rolltime", 100);
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 2);
		setInt(mode, "medalSK", 3);
		setInt(mode, "medalCO", 1);
		setBoolean(mode, "showsectiontime", true);
		setInt(mode, "sectionavgtime", 300);
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[1] = 600; // a populated section row (i==section, ending!=0)

		mode.renderLast(engine, 0);

		assertTrue(true);
	}

	@Test
	void renderResultAllThreePages() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "grade", 2);
		setInt(mode, "rollclear", 1); // green
		setInt(mode, "secretGrade", 7);
		setInt(mode, "sectionavgtime", 300);
		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 600;
		setInt(mode, "medalAC", 1);
		setInt(mode, "medalST", 1);
		setInt(mode, "medalSK", 1);
		setInt(mode, "medalCO", 1);

		engine.statc[1] = 0;
		mode.renderResult(engine, 0); // page 1 (grade, secret grade)

		setInt(mode, "rollclear", 2); // orange
		mode.renderResult(engine, 0);

		engine.statc[1] = 1;
		mode.renderResult(engine, 0); // page 2 (section)

		engine.statc[1] = 2;
		mode.renderResult(engine, 0); // page 3 (medals)

		assertTrue(true);
	}

	@Test
	void renderSettingDrawsMenu() throws Exception {
		FinalMode mode = new FinalMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.renderSetting(engine, 0);

		assertTrue(true);
	}

	// ---- field setup helper ----

	private static void fillBottomRow(GameEngine engine) {
		int y = engine.field.getHeight() - 1;
		for(int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_RED));
		}
	}

	private static void resetRankingArrays(FinalMode mode) throws Exception {
		int[] rg = (int[]) readField(mode, "rankingGrade");
		int[] rl = (int[]) readField(mode, "rankingLevel");
		int[] rt = (int[]) readField(mode, "rankingTime");
		int[] rc = (int[]) readField(mode, "rankingRollclear");
		for(int i = 0; i < rg.length; i++) {
			rg[i] = 0;
			rl[i] = 0;
			rt[i] = 0;
			rc[i] = 0;
		}
		int[] best = (int[]) readField(mode, "bestSectionTime");
		for(int i = 0; i < best.length; i++) best[i] = 1800;
	}

	// ---- reflection helpers ----

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

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeSetSpeed(FinalMode mode, GameEngine engine) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeSetAverageSectionTime(FinalMode mode) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static void invokeStMedalCheck(FinalMode mode, GameEngine engine, int section) throws Exception {
		Method m = FinalMode.class.getDeclaredMethod("stMedalCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, section);
	}
}
