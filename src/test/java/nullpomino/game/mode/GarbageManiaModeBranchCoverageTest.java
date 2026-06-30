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
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch coverage for {@link GarbageManiaMode} concentrating on the
 * "false outcome" / boundary branches the existing suite leaves untaken:
 * helper guards, the {@code onMove}/{@code onARE} level-stop arms,
 * {@code calcScore} section / garbage-wrap arms, {@code onLast} roll arms,
 * the results navigation wraps, {@code saveReplay} gating, and a number of
 * {@code renderLast} threshold branches (render-only, no assertions).
 */
class GarbageManiaModeBranchCoverageTest {

	/** Receiver that reports the alternate "next display type" so the
	 *  {@code getNextDisplayType() == 2} ternary arms in renderLast fire. */
	private static final class WideNextReceiver extends EventReceiver {
		@Override public int getNextDisplayType() { return 2; }
	}

	// ------------------------------------------------------------------
	// Helper-method guard branches
	// ------------------------------------------------------------------

	@Test
	void setAverageSectionTimeSkipsOutOfRangeSectionIndices() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);

		// startlevel=5, sectionscomp=8 -> i runs 5..12; indices 10..12 are out of
		// range (sectiontime.length == 10) so the (i < length) guard goes false.
		int[] st = (int[]) readObj(mode, "sectiontime");
		for(int i = 0; i < st.length; i++) st[i] = 60; // 10 each in-range section
		setInt(mode, "startlevel", 5);
		setInt(mode, "sectionscomp", 8);

		invoke(mode, "setAverageSectionTime");

		// Only indices 5..9 contribute (5 sections * 60 = 300), divided by 8.
		assertEquals(300 / 8, readInt(mode, "sectionavgtime"));
	}

	@Test
	void stNewRecordCheckSuppressedInReplayMode() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.owner.replayMode = true; // !owner.replayMode -> false

		int[] st = (int[]) readObj(mode, "sectiontime");
		int[] best = (int[]) readObj(mode, "bestSectionTime");
		st[0] = 10; best[0] = 9999; // section time beats best, but replay suppresses

		invoke(mode, "stNewRecordCheck", new Class<?>[]{int.class}, 0);

		boolean[] rec = (boolean[]) readObj(mode, "sectionIsNewRecord");
		assertFalse(rec[0]);
		assertFalse(readBool(mode, "sectionAnyNewRecord"));
	}

	// ------------------------------------------------------------------
	// startGame negative-level branch
	// ------------------------------------------------------------------

	@Test
	void startGameNegativeLevelForcesNextSection100() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "startlevel", -1); // level = -100 < 0

		mode.startGame(e, 0);

		assertEquals(-100, e.statistics.level);
		assertEquals(100, readInt(mode, "nextseclv"));
	}

	// ------------------------------------------------------------------
	// onMove: false branch of (level < nextseclv-1) + levelstop arm
	// ------------------------------------------------------------------

	@Test
	void onMoveAtSectionEdgeDoesNotAdvanceLevel() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.statc[0] = 0; e.holdDisable = false;
		setBool(mode, "lvupflag", false);
		// level already at the section edge: nextseclv-1 -> the < guard is false.
		setInt(mode, "nextseclv", 200);
		e.statistics.level = 199;

		mode.onMove(e, 0);

		assertEquals(199, e.statistics.level); // not advanced
	}

	@Test
	void onMoveLevelStopSEFiresAtSectionEdge() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.statc[0] = 0; e.holdDisable = false;
		setBool(mode, "lvupflag", false);
		setBool(mode, "lvstopse", true);
		setInt(mode, "nextseclv", 200);
		e.statistics.level = 198; // ++ -> 199 == nextseclv-1, lvstopse true

		mode.onMove(e, 0);

		assertEquals(199, e.statistics.level);
	}

	@Test
	void onMoveVersion1WithHoldDisableDoesNotClearLvupflag() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.statc[0] = 1; e.holdDisable = true;
		setInt(mode, "version", 1); // version<2 AND holdDisable -> (v>=2 || !holdDisable) false
		setBool(mode, "lvupflag", true);

		mode.onMove(e, 0);

		assertTrue(readBool(mode, "lvupflag")); // unchanged
	}

	// ------------------------------------------------------------------
	// onARE: false branch of (level < nextseclv-1) + levelstop arm
	// ------------------------------------------------------------------

	@Test
	void onAREAtSectionEdgeDoesNotAdvanceLevel() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.statc[0] = 5; e.statc[1] = 6;
		setBool(mode, "lvupflag", false);
		setInt(mode, "nextseclv", 300);
		e.statistics.level = 299; // == nextseclv-1 -> < guard false

		mode.onARE(e, 0);

		assertEquals(299, e.statistics.level);
		assertTrue(readBool(mode, "lvupflag"));
	}

	@Test
	void onARELevelStopSEFiresAtSectionEdge() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.statc[0] = 5; e.statc[1] = 6;
		setBool(mode, "lvupflag", false);
		setBool(mode, "lvstopse", true);
		setInt(mode, "nextseclv", 300);
		e.statistics.level = 298; // ++ -> 299 == nextseclv-1, lvstopse true

		mode.onARE(e, 0);

		assertEquals(299, e.statistics.level);
	}

	// ------------------------------------------------------------------
	// levelUp branches reached via onARE
	// ------------------------------------------------------------------

	@Test
	void levelUpKeepsGhostWhenAlwaysghost() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.statc[0] = 5; e.statc[1] = 6;
		setBool(mode, "lvupflag", false);
		setBool(mode, "alwaysghost", true); // (level>=100) && !alwaysghost -> false
		setInt(mode, "nextseclv", 999);
		e.statistics.level = 150; // >= 100
		e.ghost = true;

		mode.onARE(e, 0);

		assertTrue(e.ghost); // ghost not turned off
	}

	@Test
	void levelUpTriggersBgmFadeout() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.statc[0] = 5; e.statc[1] = 6;
		setBool(mode, "lvupflag", false);
		setInt(mode, "bgmlv", 0); // tableBGMFadeout[0] == 495
		setInt(mode, "nextseclv", 999);
		e.statistics.level = 495; // >= 495 -> fadeout

		mode.onARE(e, 0);

		assertTrue(e.owner.bgmStatus.fadesw);
	}

	// ------------------------------------------------------------------
	// calcScore branches
	// ------------------------------------------------------------------

	@Test
	void calcScoreBigButOldVersionUsesNonBigGarbage() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.createFieldIfNeeded();
		setBool(mode, "big", true);
		setInt(mode, "version", 1); // (big && version>=3) -> false, non-big branch
		setInt(mode, "garbageCount", 12); // -> >= 13 - level/100 (13) after ++

		mode.calcScore(e, 0, 0);

		assertTrue(readInt(mode, "garbageTotal") > 0);
	}

	@Test
	void calcScoreGarbagePosWrapsNonBig() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.createFieldIfNeeded();
		setBool(mode, "big", false);
		setInt(mode, "garbageCount", 12);
		// tableGarbagePattern.length == 24 -> last index 23; set to 23 so ++ -> 24 wraps to 0.
		setInt(mode, "garbagePos", 23);

		mode.calcScore(e, 0, 0);

		assertEquals(0, readInt(mode, "garbagePos"));
	}

	@Test
	void calcScoreGarbagePosWrapsBig() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.createFieldIfNeeded();
		setBool(mode, "big", true);
		setInt(mode, "version", 3);
		setInt(mode, "garbageCount", 12);
		// tableGarbagePatternBig.length == 24 -> last index 23.
		setInt(mode, "garbagePos", 23);

		mode.calcScore(e, 0, 0);

		assertEquals(0, readInt(mode, "garbagePos"));
	}

	@Test
	void calcScoreLinesDuringEndingSkipsScoring() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 2; // (lines>=1) && (ending==0) -> false
		e.createFieldIfNeeded();
		e.statistics.score = 0;
		e.statistics.level = 100;

		mode.calcScore(e, 0, 2);

		assertEquals(0, e.statistics.score); // scoring skipped
		assertEquals(100, e.statistics.level); // level not advanced
	}

	@Test
	void calcScoreSectionUpSwitchesBgm() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		setInt(mode, "bgmlv", 0); // tableBGMChange[0] == 500
		setInt(mode, "nextseclv", 500);
		e.statistics.level = 499; // +1 -> 500 >= nextseclv and >= tableBGMChange[0]

		mode.calcScore(e, 0, 1);

		assertEquals(1, readInt(mode, "bgmlv"));
		assertEquals(1, e.owner.bgmStatus.bgm);
	}

	@Test
	void calcScoreClampsNextSectionAt999() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		setInt(mode, "nextseclv", 900);
		e.statistics.level = 899; // +1 -> 900 >= nextseclv; nextseclv += 100 -> 1000 -> clamp 999

		mode.calcScore(e, 0, 1);

		assertEquals(999, readInt(mode, "nextseclv"));
	}

	@Test
	void calcScoreLevelStopSEViaElseIfArm() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		setBool(mode, "lvstopse", true);
		setInt(mode, "nextseclv", 200);
		e.statistics.level = 198; // +1 -> 199 == nextseclv-1 (else-if arm), lvstopse true

		mode.calcScore(e, 0, 1);

		assertEquals(199, e.statistics.level);
	}

	@Test
	void calcScoreManualLockBonusAndNonEmptyFieldNoBravo() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		e.manualLock = true;          // L741 true branch
		// Make the field non-empty so isEmpty() is false (no bravo).
		Block blk = new Block();
		blk.color = Block.BLOCK_COLOR_RED;
		e.field.setBlock(0, e.field.getHeight() - 1, blk);
		e.statistics.level = 100;

		mode.calcScore(e, 0, 1);

		assertFalse(e.field.isEmpty());
		assertTrue(e.statistics.score > 0);
	}

	@Test
	void calcScoreNegativeSpeedBonusClampedToZero() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.ending = 0; e.nowPieceObject = new Piece(Piece.PIECE_T); e.createFieldIfNeeded();
		// speedBonus = getLockDelay() - statc[0]; lockDelay default 31 -> statc[0] bigger -> negative.
		e.statc[0] = 1000;
		e.statistics.level = 100;

		mode.calcScore(e, 0, 1);

		assertTrue(e.statistics.score > 0);
	}

	// ------------------------------------------------------------------
	// onLast branches
	// ------------------------------------------------------------------

	@Test
	void onLastNoSectionTimeWhenTimerInactive() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.timerActive = false; // (timerActive && ending==0) -> false
		e.ending = 0;
		int[] st = (int[]) readObj(mode, "sectiontime");
		st[0] = 0;

		mode.onLast(e, 0);

		assertEquals(0, st[0]); // not incremented
	}

	@Test
	void onLastSectionIndexOutOfRangeIsIgnored() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.timerActive = true; e.ending = 0;
		e.statistics.level = -100; // section = -1 -> (section>=0) false

		mode.onLast(e, 0); // must not throw / index out of bounds

		int[] st = (int[]) readObj(mode, "sectiontime");
		assertEquals(0, st[0]);
	}

	@Test
	void onLastEndingButGameInactiveSkipsRoll() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.gameActive = false; // (gameActive && ending==2) -> false
		e.ending = 2;
		setInt(mode, "rolltime", 0);

		mode.onLast(e, 0);

		assertEquals(0, readInt(mode, "rolltime")); // roll not advanced
	}

	@Test
	void onLastEndingVersionZeroAdvancesRollByOne() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.gameActive = true; e.ending = 2;
		setInt(mode, "version", 0); // version<1 -> else arm (+1)
		setInt(mode, "rolltime", 0);
		e.ctrl.reset();
		e.ctrl.buttonPress[Controller.BUTTON_F] = true;
		e.ctrl.buttonTime[Controller.BUTTON_F] = 1; // pressed but version<1 ignores

		mode.onLast(e, 0);

		assertEquals(1, readInt(mode, "rolltime"));
	}

	// ------------------------------------------------------------------
	// onGameOver false branch
	// ------------------------------------------------------------------

	@Test
	void onGameOverNonZeroStatcDoesNotComputeSecretGrade() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statc[0] = 1; // (statc[0]==0) -> false
		e.createFieldIfNeeded();
		setInt(mode, "secretGrade", 7);

		mode.onGameOver(e, 0);

		assertEquals(7, readInt(mode, "secretGrade")); // unchanged
	}

	// ------------------------------------------------------------------
	// renderResult / onResult navigation
	// ------------------------------------------------------------------

	@Test
	void renderResultPage1WithoutAverage() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.RESULT; e.statc[1] = 1;
		setInt(mode, "sectionavgtime", 0); // (sectionavgtime>0) -> false
		mode.renderResult(e, 0); // render-only
		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	@Test
	void onResultUpWrapsToLastPage() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statc[1] = 0; // -- -> -1 -> wrap to 2
		e.ctrl.reset();
		e.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		e.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.onResult(e, 0);
		assertEquals(2, e.statc[1]);
	}

	@Test
	void onResultDownWrapsToFirstPage() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.statc[1] = 2; // ++ -> 3 -> wrap to 0
		e.ctrl.reset();
		e.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		e.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.onResult(e, 0);
		assertEquals(0, e.statc[1]);
	}

	// ------------------------------------------------------------------
	// saveReplay gating branches
	// ------------------------------------------------------------------

	@Test
	void saveReplaySavesRankingWhenRanked() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		// Reset ranking row so the result is hermetic regardless of prior runs.
		resetRanking(mode);
		setInt(mode, "startlevel", 0);
		setBool(mode, "big", false);
		setBool(mode, "always20g", false);
		e.ai = null;
		e.statistics.level = 999;   // beats the cleared (zeroed) ranking -> rank 0
		e.statistics.time = 1000;
		CustomProperties prop = new CustomProperties();
		e.owner.replayProp = prop;
		e.owner.modeConfig = new CustomProperties();

		mode.saveReplay(e, 0, prop);

		assertEquals(0, readInt(mode, "rankingRank"));
		// (rankingRank != -1) true -> saveRanking ran and wrote the level row.
		assertEquals(999, e.owner.modeConfig.getProperty(
				"garbagemania.ranking." + e.ruleopt.strRuleName + ".level.0", -1));
	}

	@Test
	void saveReplaySkipsRankingWhenBig() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		setInt(mode, "startlevel", 0);
		setBool(mode, "big", true); // big -> outer condition false, ranking not touched
		setBool(mode, "always20g", false);
		e.ai = null;
		e.statistics.level = 999;
		setInt(mode, "rankingRank", -1);
		CustomProperties prop = new CustomProperties();
		e.owner.replayProp = prop;

		mode.saveReplay(e, 0, prop);

		assertEquals(-1, readInt(mode, "rankingRank")); // unchanged -> updateRanking skipped
	}

	@Test
	void saveReplaySavesBestSectionTimeOnNewRecord() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		resetRanking(mode);
		setInt(mode, "startlevel", 0);
		setBool(mode, "big", false);
		setBool(mode, "always20g", false);
		e.ai = null;
		e.statistics.level = 0; // loses to cleared ranking -> rank -1
		e.statistics.time = 0;
		setBool(mode, "sectionAnyNewRecord", true); // L891 true; L893 second arm true
		boolean[] rec = (boolean[]) readObj(mode, "sectionIsNewRecord");
		int[] st = (int[]) readObj(mode, "sectiontime");
		rec[0] = true; st[0] = 123;
		CustomProperties prop = new CustomProperties();
		e.owner.replayProp = prop;
		e.owner.modeConfig = new CustomProperties();

		mode.saveReplay(e, 0, prop);

		assertEquals(-1, readInt(mode, "rankingRank"));
		int[] best = (int[]) readObj(mode, "bestSectionTime");
		assertEquals(123, best[0]); // updateBestSectionTime ran
	}

	// ------------------------------------------------------------------
	// renderLast threshold branches (render-only; no behavioural assertion)
	// ------------------------------------------------------------------

	@Test
	void renderLastRankingHighlightAndNegativeLevel() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		// SETTING + default flags -> ranking screen; rankingRank == 0 lights row 0.
		e.stat = GameEngine.Status.SETTING;
		setBool(mode, "isShowBestSectionTime", false);
		setInt(mode, "startlevel", 0);
		setBool(mode, "big", false);
		setBool(mode, "always20g", false);
		setInt(mode, "rankingRank", 0); // (i == rankingRank) true for i==0
		mode.renderLast(e, 0);

		// Then the in-game branch with a negative level + negative gravity.
		e.stat = GameEngine.Status.MOVE;
		e.statistics.level = -10; // tempLevel < 0 -> 0
		e.speed.gravity = -1;     // gravity < 0 -> speed = 40
		mode.renderLast(e, 0);
		assertEquals(-10, e.statistics.level); // render does not mutate level
	}

	@Test
	void renderLastRollTimeClampAndColorBand() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngine(mode);
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE; e.gameActive = true; e.ending = 2;
		// rolltime in (ROLLTIMELIMIT-600, ROLLTIMELIMIT): remaining in (0,600) -> highlighted.
		setInt(mode, "rolltime", 2024 - 100); // remaining 100, in (0, 600)
		mode.renderLast(e, 0);

		// Now overshoot the limit so remaining < 0 clamps to 0.
		setInt(mode, "rolltime", 2024 + 50);
		mode.renderLast(e, 0);
		assertTrue(e.gameActive);
	}

	@Test
	void renderLastSectionTimeWideNextDisplay() throws Exception {
		GarbageManiaMode mode = new GarbageManiaMode();
		GameEngine e = freshEngineWithReceiver(mode, new WideNextReceiver());
		mode.playerInit(e, 0);
		e.stat = GameEngine.Status.MOVE;
		e.ending = 0;
		setBool(mode, "showsectiontime", true);
		int[] st = (int[]) readObj(mode, "sectiontime");
		st[0] = 100; // section 0 has time; with level 0 -> i==section && ending==0 -> "b"
		st[1] = 0;   // sectiontime[i] > 0 false for i==1
		setInt(mode, "sectionavgtime", 0); // (sectionavgtime>0) false
		e.statistics.level = 0; // section = 0
		mode.renderLast(e, 0);
		assertEquals(100, st[0]);
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	private static GameEngine freshEngine(GarbageManiaMode mode) {
		return freshEngineWithReceiver(mode, new EventReceiver());
	}

	private static GameEngine freshEngineWithReceiver(GarbageManiaMode mode, EventReceiver receiver) {
		GameManager m = new GameManager(receiver);
		m.mode = mode;
		m.init();
		m.engine[0].init();
		m.engine[0].ruleopt.fieldWidth = 10;
		m.engine[0].ruleopt.fieldHeight = 20;
		m.engine[0].owner.replayMode = false;
		return m.engine[0];
	}

	private static void resetRanking(GarbageManiaMode mode) throws Exception {
		int[] lv = (int[]) readObj(mode, "rankingLevel");
		int[] tm = (int[]) readObj(mode, "rankingTime");
		for(int i = 0; i < lv.length; i++) { lv[i] = 0; tm[i] = 0; }
	}

	private static void invoke(Object o, String name) throws Exception {
		java.lang.reflect.Method m = method(o.getClass(), name, new Class<?>[0]);
		m.invoke(o);
	}

	private static void invoke(Object o, String name, Class<?>[] sig, Object... args) throws Exception {
		java.lang.reflect.Method m = method(o.getClass(), name, sig);
		m.invoke(o, args);
	}

	private static java.lang.reflect.Method method(Class<?> cls, String name, Class<?>[] sig) throws NoSuchMethodException {
		Class<?> c = cls;
		while (c != null) {
			try {
				java.lang.reflect.Method m = c.getDeclaredMethod(name, sig);
				m.setAccessible(true);
				return m;
			} catch (NoSuchMethodException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchMethodException(name);
	}

	private static void setInt(Object o, String n, int v) throws Exception { field(o.getClass(), n).setInt(o, v); }
	private static void setBool(Object o, String n, boolean v) throws Exception { field(o.getClass(), n).setBoolean(o, v); }
	private static int readInt(Object o, String n) throws Exception { return field(o.getClass(), n).getInt(o); }
	private static boolean readBool(Object o, String n) throws Exception { return field(o.getClass(), n).getBoolean(o); }
	private static Object readObj(Object o, String n) throws Exception { return field(o.getClass(), n).get(o); }

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
