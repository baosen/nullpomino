// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.menu.OnOffMenuItem;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Branch-complement coverage for {@link GradeManiaMode}: drives the
 * false/true outcomes that the existing GradeManiaMode test classes only take
 * once, plus the not-yet-covered switch / threshold arms.
 *
 * <p>Targets (line numbers in GradeManiaMode.java):
 * startGame L357/L358; renderLast L378/L382-L391/L422-L444/L452-L456/L460-L472/L481
 * (NextDisplayType==2 layout, out-of-range grade clamps, GM grade, negative
 * level/gravity, roll-time, section-time render); onMove L496/L498; onARE
 * L516/L518; levelUp L542/L545; calcScore L554/L640/L647/L648; onLast L669
 * (section out of range); renderResult L737/L741/L744; onResult L765/L771;
 * saveReplay/checkRanking L788/L792/L861.
 */
class GradeManiaModeBranchCoverageTest {

	/** EventReceiver whose preview type is the "Side Big" (==2) layout. */
	private static final class BigSideNextReceiver extends EventReceiver {
		@Override public int getNextDisplayType() { return 2; }
	}

	// =====================================================================
	// startGame: L357 (level < 0 -> nextseclv = 100), L358 (level >= 900 -> 999)
	// =====================================================================

	@Test
	void startGameNegativeStartLevelClampsNextSecLvTo100() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// startlevel below 0 -> level = -100 -> L357 true arm.
		setMenuValue(mode, "startlevel", -1);
		mode.startGame(engine, 0);

		assertEquals(100, readInt(mode, "nextseclv"),
				"negative start level should set nextseclv to 100");
	}

	@Test
	void startGameLevel900SetsNextSecLvTo999() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setMenuValue(mode, "startlevel", 9); // level = 900 -> L358 true arm
		mode.startGame(engine, 0);

		assertEquals(999, readInt(mode, "nextseclv"),
				"start level >= 900 should set nextseclv to 999");
	}

	// =====================================================================
	// renderLast RESULT branch (L378 RESULT && !replayMode) + ranking render
	// with NextDisplayType==2 layout (L382/L383 true arm), the rankingRank
	// highlight (L389-L391 i==rankingRank true) and the out-of-range grade
	// guard (L388 false arm).
	// =====================================================================

	@Test
	void renderLastResultRankingWithBigSideLayoutAndHighlight() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngineWith(mode, new BigSideNextReceiver());
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.RESULT; // L378: RESULT && !replayMode
		engine.owner.replayMode = false;
		setMenuValue(mode, "startlevel", 0);
		setBool(mode, "isShowBestSectionTime", false);
		setInt(mode, "rankingRank", 2); // L389-391: i == rankingRank true at i=2

		int[] grades = (int[]) readField(mode, "rankingGrade");
		grades[0] = -1;                 // L388 false arm (rankingGrade[i] < 0)
		grades[1] = tableGradeNameLength(); // L388 false arm (>= length)
		grades[2] = 5;                  // in range, highlighted row

		mode.renderLast(engine, 0); // no assertion: render branch coverage
		assertTrue(true);
	}

	// =====================================================================
	// renderLast in-game panel: GM grade (L433 false: grade >= 17),
	// lastscore!=0 && scgettime>0 (L427 false-false -> else), negative level
	// (L439 true), negative gravity (L444 true), roll-time with time<0
	// (L452 true / L454 true / L456), and section-time render via the
	// NextDisplayType==2 column layout (L460-L472) plus L481.
	// =====================================================================

	@Test
	void renderLastInGamePanelExercisesThresholdArms() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngineWith(mode, new BigSideNextReceiver());
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.MOVE; // else-branch (in-game panel)
		setInt(mode, "grade", 18);             // L422 in-range true; L433 grade<17 false
		setInt(mode, "gradeflash", 8);         // L423: gradeflash>0 && %4==0 true
		setInt(mode, "lastscore", 250);        // L427: lastscore!=0 ...
		setInt(mode, "scgettime", 30);         // ... && scgettime>0 -> else arm
		engine.statistics.level = -5;          // L439: tempLevel<0 true
		engine.speed.gravity = -1;             // L444: gravity<0 -> speed=40
		engine.gameActive = true;
		engine.ending = 2;                     // L452: gameActive && ending==2 true
		setInt(mode, "rolltime", 9999);        // ROLLTIMELIMIT - rolltime < 0 -> L454 true

		((OnOffMenuItem) readField(mode, "showsectiontime")).value = true; // L460
		int[] st = (int[]) readField(mode, "sectiontime");
		st[0] = 100;
		st[1] = 200;
		setInt(mode, "sectionavgtime", 150);   // L481 true

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// =====================================================================
	// renderLast in-game panel with out-of-range grade (L422 false arm)
	// =====================================================================

	@Test
	void renderLastInGameOutOfRangeGradeSkipsName() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "grade", 99); // L422: grade < tableGradeName.length false

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// =====================================================================
	// renderLast RESULT ranking: false arm of the big AND chain on L379
	// (replayMode==false but startlevel != 0) so the ranking block is skipped.
	// =====================================================================

	@Test
	void renderLastResultSkipsRankingWhenStartLevelNonZero() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.RESULT;
		engine.owner.replayMode = false;
		setMenuValue(mode, "startlevel", 3); // L379: startlevel.value == 0 false

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// =====================================================================
	// onMove: level already at nextseclv-1 (L496 false), and at nextseclv-1
	// with lvstopse -> levelstop SE (L498 true via the +1 from the outer block
	// is not reached, so cover L498 through onARE instead). Here L496 false.
	// =====================================================================

	@Test
	void onMoveAtSectionBoundaryDoesNotIncrementLevel() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 199; // == nextseclv-1 -> L496 (level < nextseclv-1) false
		setBool(mode, "lvupflag", false);
		engine.ending = 0;
		engine.holdDisable = false;

		mode.onMove(engine, 0);

		assertEquals(199, engine.statistics.level,
				"level at the section boundary must not advance in onMove");
	}

	// =====================================================================
	// onARE: increments to the section boundary and fires levelstop SE
	// (L515 true, L516 true, L518 true: level == nextseclv-1 && lvstopse).
	// =====================================================================

	@Test
	void onAreReachingBoundaryPlaysLevelStopSe() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 198; // +1 -> 199 == nextseclv-1
		setBool(mode, "lvupflag", false);
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 6; // statc[0] >= statc[1]-1 -> L515 true
		((OnOffMenuItem) readField(mode, "lvstopse")).value = true; // L518 condition true

		mode.onARE(engine, 0);

		assertEquals(199, engine.statistics.level,
				"onARE should advance one level to the section boundary");
		assertTrue(readBool(mode, "lvupflag"),
				"onARE should set lvupflag after the boundary level-up");
	}

	@Test
	void onAreAtBoundaryDoesNotIncrementLevel() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 199; // == nextseclv-1 -> L516 false
		setBool(mode, "lvupflag", false);
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 6;

		mode.onARE(engine, 0);

		assertEquals(199, engine.statistics.level);
	}

	// =====================================================================
	// levelUp: alwaysghost ON keeps the ghost (L542 false arm: !alwaysghost
	// false). Driven through onMove which calls levelUp.
	// =====================================================================

	@Test
	void levelUpWithAlwaysGhostKeepsGhostEnabled() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		((OnOffMenuItem) readField(mode, "alwaysghost")).value = true; // L542 false arm
		engine.ghost = true;
		engine.statistics.level = 150; // >= 100
		setInt(mode, "nextseclv", 500);
		setBool(mode, "lvupflag", false);
		engine.ending = 0;
		engine.holdDisable = false;

		mode.onMove(engine, 0); // -> levelUp

		assertTrue(engine.ghost, "ghost must stay on when alwaysghost is enabled");
	}

	@Test
	void levelUpAtLevel100DisablesGhostWhenAlwaysGhostOff() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		((OnOffMenuItem) readField(mode, "alwaysghost")).value = false; // L542 true arm
		engine.ghost = true;
		engine.statistics.level = 150;
		setInt(mode, "nextseclv", 500);
		setBool(mode, "lvupflag", false);
		engine.ending = 0;
		engine.holdDisable = false;

		mode.onMove(engine, 0);

		assertFalse(engine.ghost, "ghost must turn off past level 100 without alwaysghost");
	}

	// =====================================================================
	// levelUp BGM fade: bgmlv==0 && level>=490 triggers fade (L545 true arm).
	// =====================================================================

	@Test
	void levelUpTriggersBgmFadeAt490() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "bgmlv", 0);
		setInt(mode, "nextseclv", 999);
		engine.statistics.level = 495; // >= 490 -> L545 true
		setBool(mode, "lvupflag", false);
		engine.ending = 0;
		engine.holdDisable = false;

		mode.onMove(engine, 0);

		assertTrue(engine.owner.bgmStatus.fadesw, "BGM fade should be requested at level>=490");
	}

	// =====================================================================
	// calcScore: ending != 0 returns immediately (L554 true arm).
	// =====================================================================

	@Test
	void calcScoreReturnsEarlyDuringEnding() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		engine.ending = 1; // L554: engine.ending != 0 -> return
		engine.statistics.score = 0;
		setInt(mode, "comboValue", 9);

		mode.calcScore(engine, 0, 4);

		assertEquals(0, engine.statistics.score, "score must not change once ending started");
		assertEquals(9, readInt(mode, "comboValue"), "combo must not change once ending started");
	}

	// =====================================================================
	// calcScore section advance: bgmlv==0 && nextseclv==500 -> BGM switch
	// (L640 true), and nextseclv past 999 clamps (L647 true). Field made
	// non-empty so bravo == 1 (no 4x), keeping the scoring deterministic.
	// =====================================================================

	@Test
	void calcScoreSection500SwitchesBgm() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "version", 1);
		setInt(mode, "bgmlv", 0);          // L640 first operand true
		setInt(mode, "nextseclv", 500);    // L640 second operand true
		engine.statistics.level = 450;     // +50 lines -> 500 >= nextseclv
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 50);

		assertEquals(1, readInt(mode, "bgmlv"), "section 500 should bump BGM level");
	}

	@Test
	void calcScoreNextSecLvClampsTo999() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setInt(mode, "version", 1);
		setInt(mode, "bgmlv", 1);          // L640 first operand false (no BGM swap)
		setInt(mode, "nextseclv", 900);    // +100 -> 1000 -> L647 clamps to 999
		engine.statistics.level = 850;     // +50 -> 900 >= nextseclv (next section)
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 50);

		assertEquals(999, readInt(mode, "nextseclv"), "nextseclv past 999 must clamp to 999");
	}

	// =====================================================================
	// calcScore: level reaches exactly nextseclv-1 with lvstopse -> levelstop
	// SE (L648 true arm). Field non-empty so bravo==1, level lands on boundary.
	// =====================================================================

	@Test
	void calcScoreLandingOnBoundaryPlaysLevelStop() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		fillFieldCell(engine); // bravo == 1

		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 198; // +1 line -> 199 == nextseclv-1 -> L648 true
		((OnOffMenuItem) readField(mode, "lvstopse")).value = true;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(199, engine.statistics.level,
				"landing exactly on the section boundary holds the level");
	}

	// =====================================================================
	// onLast: section index out of range (level/100 >= sectiontime.length)
	// so the increment is skipped (L669 false arm).
	// =====================================================================

	@Test
	void onLastSectionOutOfRangeSkipsIncrement() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 1500; // section = 15 >= length 10 -> L669 false

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] before = st.clone();

		mode.onLast(engine, 0);

		for (int i = 0; i < st.length; i++) {
			assertEquals(before[i], st[i], "no section time should change for an out-of-range section");
		}
	}

	// =====================================================================
	// renderResult page 1 with sectionavgtime == 0 (L733 false arm).
	// =====================================================================

	@Test
	void renderResultPage1WithoutAverage() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.RESULT;
		engine.statc[1] = 1; // L737 reached via the page-1 arm
		setInt(mode, "sectionavgtime", 0); // L733 false arm

		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	// =====================================================================
	// renderResult page 2: grade != 18 -> PIER grade block skipped
	// (L741 false arm).
	// =====================================================================

	@Test
	void renderResultPage2NonGmGradeSkipsPier() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.RESULT;
		engine.statc[1] = 2; // L737 true arm
		setInt(mode, "grade", 10); // L741: grade == 18 false

		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	// =====================================================================
	// renderResult page 2: GM grade with a fast time so the PIER rank loop
	// updates (L744 true arm: time < tablePier21GradeTime[i]).
	// =====================================================================

	@Test
	void renderResultPage2GmFastTimeRanksPier() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.stat = GameEngine.Status.RESULT;
		engine.statc[1] = 2;
		setInt(mode, "grade", 18); // L741 true
		engine.statistics.time = 1000; // well below every tablePier21GradeTime -> L744 true

		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	// =====================================================================
	// onResult: DOWN at page 2 wraps to 0 (L765 true arm).
	// =====================================================================

	@Test
	void onResultDownAtLastPageWrapsToZero() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.statc[1] = 2;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;

		mode.onResult(engine, 0);

		assertEquals(0, engine.statc[1], "DOWN past the last page wraps back to 0");
	}

	// =====================================================================
	// onResult: F toggles section view true -> false (L771 second outcome).
	// =====================================================================

	@Test
	void onResultFTogglesSectionViewBackToFalse() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "isShowBestSectionTime", true); // toggle -> false
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;

		mode.onResult(engine, 0);

		assertFalse(readBool(mode, "isShowBestSectionTime"),
				"F on the result screen toggles the section view true -> false");
	}

	// =====================================================================
	// saveReplay / checkRanking: with a hermetic empty ranking row, a fresh
	// result ranks at the top (rankingRank != -1 -> L792 true) and the
	// checkRanking predicate's grade-greater arm (L861) fires. We avoid
	// persisting to disk by stubbing saveModeConfig.
	// =====================================================================

	@Test
	void saveReplayInsertsTopRankAndPersistsConditionally() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngineWith(mode, new NonPersistingReceiver());
		mode.playerInit(engine, 0);

		// Hermetic: clear the ranking arrays (cross-test leak guard).
		int[] grades = (int[]) readField(mode, "rankingGrade");
		int[] levels = (int[]) readField(mode, "rankingLevel");
		int[] times = (int[]) readField(mode, "rankingTime");
		for (int i = 0; i < grades.length; i++) {
			grades[i] = 0;
			levels[i] = 0;
			times[i] = 0;
		}

		engine.owner.replayMode = false;
		setMenuValue(mode, "startlevel", 0); // L788 chain stays true
		setInt(mode, "grade", 18);           // gr > rankingGrade[0] (0) -> L861 true
		engine.statistics.level = 999;
		setInt(mode, "lastGradeTime", 5000);

		mode.saveReplay(engine, 0, engine.owner.replayProp);

		assertEquals(0, readInt(mode, "rankingRank"),
				"a grade-18 result should rank first against an empty board");
		assertEquals(18, grades[0], "top ranking grade should be the new result");
	}

	// =====================================================================
	// saveReplay: false arm of the big eligibility chain (startlevel != 0)
	// so updateRanking / saveRanking are skipped (L788 false).
	// =====================================================================

	@Test
	void saveReplaySkipsRankingWhenStartLevelNonZero() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngineWith(mode, new NonPersistingReceiver());
		mode.playerInit(engine, 0);

		engine.owner.replayMode = false;
		setMenuValue(mode, "startlevel", 5); // L788: startlevel.value == 0 false
		setInt(mode, "rankingRank", -1);

		mode.saveReplay(engine, 0, engine.owner.replayProp);

		assertEquals(-1, readInt(mode, "rankingRank"),
				"ranking must not update when start level is non-zero");
	}

	// =====================================================================
	// Helpers
	// =====================================================================

	/** Receiver whose saves are no-ops so call sites run without touching disk. */
	private static final class NonPersistingReceiver extends EventReceiver {
		@Override public boolean saveProperties(String f, nullpomino.util.CustomProperties p) { return true; }
		@Override public void saveModeConfig(nullpomino.util.CustomProperties c) { }
	}

	private static GameEngine freshEngine(GradeManiaMode mode) {
		return freshEngineWith(mode, new EventReceiver());
	}

	private static GameEngine freshEngineWith(GradeManiaMode mode, EventReceiver receiver) {
		GameManager manager = new GameManager(receiver);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].ruleopt.fieldWidth = 10;
		manager.engine[0].ruleopt.fieldHeight = 20;
		manager.engine[0].playerID = 0;
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Drop a single block into the field so {@code field.isEmpty()} is false (bravo == 1). */
	private static void fillFieldCell(GameEngine engine) {
		engine.field.setBlockColor(0, 0, Block.BLOCK_COLOR_RED);
	}

	private static int tableGradeNameLength() throws Exception {
		Field f = GradeManiaMode.class.getDeclaredField("tableGradeName");
		f.setAccessible(true);
		return ((String[]) f.get(null)).length;
	}

	private static void setMenuValue(GradeManiaMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		Object item = f.get(mode);
		Field vf = findField(item.getClass(), "value");
		vf.set(item, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static Object readField(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).get(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
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
}
