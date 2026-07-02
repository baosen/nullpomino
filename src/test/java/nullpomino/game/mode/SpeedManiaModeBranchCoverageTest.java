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
import nullpomino.game.menu.BooleanMenuItem;
import nullpomino.game.menu.IntegerMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link SpeedManiaMode} hitting the
 * still-uncovered conditional outcomes (the "other arm") across most
 * lifecycle hooks and helpers: section-time average overflow guard,
 * stMedalCheck downgrade/silver/replay branches, renderLast display-type-2
 * and threshold branches, onMove/onARE level-stop and old-version RE medal,
 * levelUp BGM fadeout, calcScore SK/medalAC-cap/torikan-BGM/roMedal/clamp
 * branches, onLast section-index guard and slow-roll branch, onGameOver
 * non-first-frame guard, and saveReplay ranking branches.
 */
class SpeedManiaModeBranchCoverageTest {

	/** Receiver that reports the wide ("BIG SIDE PANEL", type 2) next display
	 *  and never writes to disk, so display-type-2 layout branches run. */
	private static final class WideNonPersistingReceiver extends EventReceiver {
		@Override public int getNextDisplayType() { return 2; }
		@Override public boolean saveProperties(String f, CustomProperties p) { return true; }
		@Override public void saveModeConfig(CustomProperties c) { }
	}

	// =======================================================================
	// setAverageSectionTime: i >= sectiontime.length guard (line 293 false arm)
	// =======================================================================

	@Test
	void setAverageSectionTimeSkipsOutOfRangeSections() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setIntMenuValue(mode, "startlevel", 0);
		// sectionscomp = 12 -> loop i = 0..11; i = 10, 11 fall outside
		// sectiontime (length 10) and exercise the (i < length) false arm.
		setInt(mode, "sectionscomp", 12);
		int[] st = (int[]) readField(mode, "sectiontime");
		for (int i = 0; i < st.length; i++) st[i] = 120;

		invoke(mode, "setAverageSectionTime");

		// sum of 10 in-range sections (1200) / 12 completed = 100
		assertEquals(100, readInt(mode, "sectionavgtime"));
	}

	// =======================================================================
	// stMedalCheck: gold record but medalST already 3 (310 false) + replay (314 false)
	// =======================================================================

	@Test
	void stMedalCheckGoldRecordKeepsExistingGoldAndSkipsRecordFlagInReplay() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true; // line 314 false arm

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		setInt(mode, "sectionlasttime", 2000); // < best -> enters first if (line 309 true)
		setInt(mode, "medalST", 3); // line 310 false arm (medalST already 3)

		invoke(mode, "stMedalCheck", engine, 0);

		assertEquals(3, readInt(mode, "medalST"));
		boolean[] rec = (boolean[]) readField(mode, "sectionIsNewRecord");
		assertFalse(rec[0], "replay mode must not flag a new section record");
	}

	// =======================================================================
	// stMedalCheck: silver/bronze conditions present but medal already high
	// (line 317 / 320 short-circuit: first cond true, medalST guard false)
	// =======================================================================

	@Test
	void stMedalCheckSilverConditionButMedalAlreadySilverDoesNotChange() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		// best+300 = 3300; lasttime 3100 is within silver window (line 317 first cond true)
		setInt(mode, "sectionlasttime", 3100);
		setInt(mode, "medalST", 2); // medalST < 2 is false -> no change

		invoke(mode, "stMedalCheck", engine, 0);

		assertEquals(2, readInt(mode, "medalST"));
	}

	@Test
	void stMedalCheckBronzeConditionButMedalAlreadyBronzeDoesNotChange() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] bst = (int[]) readField(mode, "bestSectionTime");
		bst[0] = 3000;
		// best+600 = 3600; lasttime 3500 is within bronze window only
		// (>= best+300 so silver fails), and medalST(1) < 1 is false (line 320).
		setInt(mode, "sectionlasttime", 3500);
		setInt(mode, "medalST", 1);

		invoke(mode, "stMedalCheck", engine, 0);

		assertEquals(1, readInt(mode, "medalST"));
	}

	// =======================================================================
	// renderLast: RESULT state in non-replay + display-type-2 ranking table
	// (line 422 RESULT arm, 426/427/432-434 wide layout + rankingRank highlight)
	// =======================================================================

	@Test
	void renderLastResultStateWideDisplayHighlightsRankingRow() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngineWide(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.RESULT; // line 422 RESULT && !replay arm
		setIntMenuValue(mode, "startlevel", 0);
		setBoolMenuValue(mode, "big", false);
		engine.ai = null;
		setBoolean(mode, "isShowBestSectionTime", false);
		setInt(mode, "rankingRank", 2); // i == rankingRank highlight (432-434)

		mode.renderLast(engine, 0);
	}

	// =======================================================================
	// renderLast: condition-array false arms (line 423) - ai != null suppresses
	// =======================================================================

	@Test
	void renderLastSettingStateWithAiSkipsRankingTable() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;
		setIntMenuValue(mode, "startlevel", 0);
		setBoolMenuValue(mode, "big", false);
		// engine.ai != null -> line 423 fourth condition false arm.
		engine.ai = new nullpomino.game.ai.DummyAI();

		mode.renderLast(engine, 0);
	}

	// =======================================================================
	// renderLast in-game: grade flash %4 != 0, positive gravity speed,
	// roll time negative clamp, plain section-time disabled, gradeflash off
	// (lines 467, 488, 500, 514 false, 536 false)
	// =======================================================================

	@Test
	void renderLastInGameGradeFlashOffPositiveGravityRollNegativeClamp() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;

		setInt(mode, "grade", 2);       // grade in range (line 465 true)
		setInt(mode, "gradeflash", 3);  // > 0 but %4 != 0 (line 467 second cond false)
		setInt(mode, "lastscore", 100);
		setInt(mode, "scgettime", 0);   // (lastscore!=0) but scgettime<=0 -> plain score (473)
		engine.statistics.level = 100;
		engine.speed.gravity = 256;     // >= 0 -> line 488 false arm (speed = gravity/128)
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 5000); // > ROLLTIMELIMIT -> time < 0 clamp (line 500 true)
		setBoolMenuValue(mode, "showsectiontime", false); // line 514 false arm

		mode.renderLast(engine, 0);
	}

	// =======================================================================
	// renderLast in-game: wide display section time, current-section separator,
	// no average row (lines 515/516 type-2, 527 true, 536 false)
	// =======================================================================

	@Test
	void renderLastInGameWideSectionTimeCurrentSectionSeparator() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngineWide(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statistics.level = 150; // section = 1
		engine.ending = 0;             // line 527 (i == section) && ending == 0 true
		setBoolMenuValue(mode, "showsectiontime", true);
		int[] st = (int[]) readField(mode, "sectiontime");
		st[1] = 360; // only the current section has time
		setInt(mode, "sectionavgtime", 0); // line 536 false arm

		mode.renderLast(engine, 0);
	}

	// =======================================================================
	// onMove: level == nextseclv-1 with levelstop SE (552 false, 554 true),
	// old-version RE medal recovery (559 version<=2, 563/567)
	// =======================================================================

	@Test
	void onMoveAtSectionEndPlaysLevelStopAndOldVersionReMedal() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		engine.timerActive = true;
		engine.statistics.level = 99;   // == nextseclv - 1 -> line 552 false arm
		setInt(mode, "nextseclv", 100);
		setBoolMenuValue(mode, "lvstopse", true); // line 554 true arm (level==nextseclv-1)
		setInt(mode, "version", 2);     // line 559 version <= 2 true arm
		setInt(mode, "medalRE", 0);
		setBoolean(mode, "recoveryFlag", false);
		fillFieldBlocks(engine, 160);   // blocks >= 150 -> recoveryFlag set (line 563 true)

		mode.onMove(engine, 0);

		assertEquals(99, engine.statistics.level); // not incremented (already at section end)
		assertTrue(readBoolean(mode, "recoveryFlag"));
	}

	@Test
	void onMoveOldVersionReMedalAwardedOnRecovery() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);
		engine.timerActive = true;
		engine.statistics.level = 50;
		setInt(mode, "nextseclv", 100);
		setInt(mode, "version", 1);     // old version path
		setInt(mode, "medalRE", 0);
		setBoolean(mode, "recoveryFlag", true); // field empty <= 70 -> line 567 true
		// field left empty (0 blocks <= 70)

		mode.onMove(engine, 0);

		assertEquals(1, readInt(mode, "medalRE"));
		assertFalse(readBoolean(mode, "recoveryFlag"));
	}

	// =======================================================================
	// onARE: level < nextseclv-1 increments, level==nextseclv-1 levelstop SE
	// (lines 593 true, 594 true increment, 596 true)
	// =======================================================================

	@Test
	void onAreLevelUpAndLevelStopSound() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 5; // statc[0] >= statc[1]-1 -> line 593 true
		setBoolean(mode, "lvupflag", false);
		engine.statistics.level = 98; // 98 < 99 -> increments to 99 == nextseclv-1 (596)
		setInt(mode, "nextseclv", 100);
		setBoolMenuValue(mode, "lvstopse", true);

		mode.onARE(engine, 0);

		assertEquals(99, engine.statistics.level);
		assertTrue(readBoolean(mode, "lvupflag"));
	}

	// =======================================================================
	// levelUp: BGM fadeout trigger (line 620 both conds true)
	// =======================================================================

	@Test
	void levelUpTriggersBgmFadeout() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "bgmlv", 0); // tableBGMFadeout[0] = 280
		setInt(mode, "version", 3);
		engine.timerActive = false; // skip RE medal block
		engine.statistics.level = 300; // >= 280 -> fadeout (line 620 true)
		setInt(mode, "nextseclv", 400);

		invoke(mode, "levelUp", engine);

		assertTrue(engine.owner.bgmStatus.fadesw);
	}

	// =======================================================================
	// calcScore: rotateTemp clamp (656 true), lines>=1 && ending!=0 (659 false),
	// big SK medal at totalFour==1 (664 true), medalAC cap (680 false),
	// manualLock (799 true), speedBonus clamp (805 true)
	// =======================================================================

	@Test
	void calcScoreRotateClampAndEndingActiveSkipsScoring() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.ending = 2; // lines>=1 but ending != 0 -> line 659 false arm
		engine.nowPieceRotateCount = 9; // > 4 -> clamp to 4 (line 656 true)
		setInt(mode, "rotateCount", 0);

		mode.calcScore(engine, 0, 2);

		assertEquals(4, readInt(mode, "rotateCount")); // clamped contribution
		assertEquals(0, engine.statistics.score);      // scoring block skipped
	}

	@Test
	void calcScoreBigSkMedalOnFirstFour() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5); // not empty
		engine.ending = 0;
		setBoolMenuValue(mode, "big", true);
		engine.big = true;
		engine.statistics.totalFour = 1; // line 664 first cond true -> SK medal
		setInt(mode, "medalSK", 0);
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	@Test
	void calcScoreNonBigSkMedalOnSeventeenthFour() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5);
		engine.ending = 0;
		setBoolMenuValue(mode, "big", false);
		engine.statistics.totalFour = 17; // line 669 third cond true
		setInt(mode, "medalSK", 0);
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 4);

		assertEquals(1, readInt(mode, "medalSK"));
	}

	@Test
	void calcScoreAcMedalCapNotIncrementedPastGold() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded(); // empty field -> bravo / AC path
		engine.ending = 0;
		setInt(mode, "medalAC", 3); // line 680 false arm (already at gold)
		engine.statistics.level = 100;

		mode.calcScore(engine, 0, 1);

		assertEquals(3, readInt(mode, "medalAC"));
	}

	@Test
	void calcScoreManualLockAndZeroSpeedBonus() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5);
		engine.ending = 0;
		engine.manualLock = true;   // line 799 true arm
		engine.statc[0] = 1000;     // lockDelay - statc[0] < 0 -> speedBonus clamp (805 true)
		engine.statistics.level = 100;
		setInt(mode, "nextseclv", 200);

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.statistics.score > 0);
	}

	// =======================================================================
	// calcScore: torikan ending BGM switch + roMedal at section 300/700
	// (lines 744 true, 782 true)
	// =======================================================================

	@Test
	void calcScoreTorikanEndingSwitchesBgm() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5);
		engine.ending = 0;
		setInt(mode, "nextseclv", 500);
		setInt(mode, "bgmlv", 1); // tableBGMChange[1] = 500 -> level>=500 switches BGM (744)
		setIntMenuValue(mode, "lv500torikan", 100);
		engine.statistics.level = 497; // 497 + 4 = 501 >= 500
		engine.statistics.time = 200;  // > torikan -> torikan ending

		mode.calcScore(engine, 0, 4);

		assertEquals(500, engine.statistics.level);
		assertEquals(2, engine.ending);
		assertEquals(2, readInt(mode, "bgmlv")); // bgmlv incremented by BGM switch
	}

	@Test
	void calcScoreNextSectionRoMedalAtSection300() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5);
		engine.ending = 0;
		setInt(mode, "nextseclv", 300); // line 782 (nextseclv == 300) true -> roMedalCheck
		// Make the RO average meet the threshold so the medal is awarded.
		setInt(mode, "rotateCount", 120);
		engine.statistics.totalPieceLocked = 100; // 1.2 >= 1.2
		setInt(mode, "medalRO", 0);
		engine.statistics.level = 297; // 297 + 4 = 301 >= 300

		mode.calcScore(engine, 0, 4);

		assertTrue(readInt(mode, "nextseclv") > 300);
		assertEquals(1, readInt(mode, "medalRO"));
	}

	// =======================================================================
	// calcScore: nextseclv > 999 clamp (line 792 true)
	// =======================================================================

	@Test
	void calcScoreNextSecClampedToNineNineNine() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.createFieldIfNeeded();
		fillFieldBlocks(engine, 5);
		engine.ending = 0;
		setInt(mode, "nextseclv", 900); // +100 = 1000 > 999 -> clamp (line 792 true)
		setInt(mode, "bgmlv", 2); // tableBGMChange[2] = -1 -> BGM switch skipped
		engine.statistics.level = 897; // 897 + 4 = 901 >= 900

		mode.calcScore(engine, 0, 4);

		assertEquals(999, readInt(mode, "nextseclv"));
	}

	// =======================================================================
	// onLast: timerActive false (826 false), section out-of-range guard (829 false),
	// slow roll without F / version 0 (835 true, 836 false arm)
	// =======================================================================

	@Test
	void onLastNoSectionIncrementWhenTimerInactive() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = false; // line 826 first cond false
		engine.ending = 0;
		engine.statistics.level = 100;

		mode.onLast(engine, 0);

		int[] st = (int[]) readField(mode, "sectiontime");
		assertEquals(0, st[1]); // not incremented
	}

	@Test
	void onLastSectionIndexOutOfRangeIsGuarded() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 1500; // section = 15 -> >= length (line 829 false arm)

		mode.onLast(engine, 0); // must not throw

		int[] st = (int[]) readField(mode, "sectiontime");
		for (int v : st) assertEquals(0, v);
	}

	@Test
	void onLastRollSlowAdvanceWhenVersionZero() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2; // line 835 true
		setInt(mode, "version", 0); // line 836 (version >= 1) false -> else rolltime += 1
		setInt(mode, "rolltime", 0);
		pressKey(engine, Controller.BUTTON_F); // F held but version 0 keeps slow advance

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "rolltime"));
	}

	// =======================================================================
	// onGameOver: non-first frame skips secret grade read (line 863 false arm)
	// =======================================================================

	@Test
	void onGameOverNonFirstFrameSkipsSecretGrade() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 1; // line 863 false arm
		setInt(mode, "secretGrade", 7);

		mode.onGameOver(engine, 0);

		assertEquals(7, readInt(mode, "secretGrade")); // unchanged
	}

	@Test
	void onGameOverFirstFrameReadsSecretGrade() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0; // line 863 true arm

		mode.onGameOver(engine, 0);

		assertEquals(engine.field.getSecretGrade(), readInt(mode, "secretGrade"));
	}

	// =======================================================================
	// saveReplay: ranking branches (951 conditions, 953 medalST==3, 955)
	// Hermetic: ranking row reset first so checkRanking/rank position is stable.
	// =======================================================================

	@Test
	void saveReplayUpdatesRankingAndBestSectionTimeOnGoldStMedal() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngineWide(mode); // no-op saveModeConfig
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setIntMenuValue(mode, "startlevel", 0);
		setBoolMenuValue(mode, "big", false);
		engine.ai = null;

		resetRanking(mode); // hermetic empty ranking row

		setInt(mode, "grade", 2);
		setInt(mode, "medalST", 3); // line 953 true -> updateBestSectionTime; line 955 true
		boolean[] rec = (boolean[]) readField(mode, "sectionIsNewRecord");
		rec[0] = true;
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 1500;
		engine.statistics.level = 999;
		engine.statistics.time = 3600;

		mode.saveReplay(engine, 0, owner(engine).replayProp);

		assertEquals(0, readInt(mode, "rankingRank")); // ranked first
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		assertEquals(1500, bst[0]); // best section time updated
	}

	@Test
	void saveReplaySkipsRankingWhenStartLevelNonZero() throws Exception {
		SpeedManiaMode mode = new SpeedManiaMode();
		GameEngine engine = freshEngineWide(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setIntMenuValue(mode, "startlevel", 3); // line 951 second cond false -> whole block skipped
		setBoolMenuValue(mode, "big", false);
		engine.ai = null;

		resetRanking(mode);

		mode.saveReplay(engine, 0, owner(engine).replayProp);

		assertEquals(-1, readInt(mode, "rankingRank")); // never ranked
	}

	// =======================================================================
	// Helpers
	// =======================================================================

	private static GameEngine freshEngine(SpeedManiaMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static GameEngine freshEngineWide(SpeedManiaMode mode) {
		GameManager manager = new GameManager(new WideNonPersistingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static nullpomino.game.play.GameManager owner(GameEngine engine) {
		return engine.owner;
	}

	private static void resetRanking(SpeedManiaMode mode) throws Exception {
		int[] g = (int[]) readField(mode, "rankingGrade");
		int[] l = (int[]) readField(mode, "rankingLevel");
		int[] t = (int[]) readField(mode, "rankingTime");
		for (int i = 0; i < g.length; i++) { g[i] = 0; l[i] = 0; t[i] = 0; }
		setInt(mode, "rankingRank", -1);
	}

	private static void fillFieldBlocks(GameEngine engine, int count) {
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		int placed = 0;
		for (int y = h - 1; y >= 0 && placed < count; y--) {
			for (int x = 0; x < w && placed < count; x++) {
				engine.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_GRAY));
				placed++;
			}
		}
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.reset();
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void invoke(SpeedManiaMode mode, String name, Object... args) throws Exception {
		for (Class<?> c = SpeedManiaMode.class; c != null; c = c.getSuperclass()) {
			for (Method m : c.getDeclaredMethods()) {
				if (m.getName().equals(name) && m.getParameterCount() == args.length) {
					m.setAccessible(true);
					m.invoke(mode, args);
					return;
				}
			}
		}
		throw new NoSuchMethodException(name);
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
