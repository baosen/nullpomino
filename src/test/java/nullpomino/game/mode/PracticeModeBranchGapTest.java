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
 * Targets the remaining uncovered branch tail of {@link PracticeMode}:
 * onSetting toggle directions + switch default, renderSetting cursor
 * highlights, onReady/startGame version and leveltype variants, renderLast
 * time-color and roll-time edges, onLast timer edges, onMove/onARE mania
 * level-up edges, calcScore (mania + normal) scoring edges, setMeter color
 * thresholds and clamps, soft/hard drop leveltype variants, and saveReplay
 * without a field backup.
 */
class PracticeModeBranchGapTest {

	/** Never writes config/map/** or mode config; gives the meter a real size. */
	private static final class GapReceiver extends EventReceiver {
		@Override public boolean saveProperties(String filename, CustomProperties prop) { return true; }
		@Override public void saveModeConfig(CustomProperties prop) { }
		@Override public int getMeterMax(GameEngine engine) { return 100; }
	}

	private static GameEngine freshEngine(PracticeMode mode) {
		GameManager manager = new GameManager(new GapReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** playerInit + reset the fields that the shared config file may dirty. */
	private static GameEngine freshInitedEngine(PracticeMode mode) throws Exception {
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 0);
		setInt(mode, "goallv", -1);
		setInt(mode, "timelimit", 0);
		setInt(mode, "rolltimelimit", 0);
		setInt(mode, "heboHiddenLevel", 0);
		setBoolean(mode, "useMap", false);
		setBoolean(mode, "timelimitResetEveryLevel", false);
		setBoolean(mode, "lvstopse", true);
		return engine;
	}

	private static void press(GameEngine engine, int... buttons) {
		engine.ctrl.reset();
		for (int b : buttons) {
			engine.ctrl.buttonPress[b] = true;
			engine.ctrl.buttonTime[b] = 1;
		}
	}

	// =====================================================================
	// onSetting: toggle both directions of every boolean menu option
	// (L429/443/451/454/462/465/468/486/489/497/505/513-546)
	// =====================================================================

	@Test
	void onSettingTogglesEveryBooleanOptionBothWays() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);

		int[] toggleCursors = {8, 11, 13, 14, 16, 17, 18, 22, 23, 25, 27,
				29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40};
		boolean bigBefore = readBoolean(mode, "big");
		for (int cursor : toggleCursors) {
			for (int i = 0; i < 2; i++) {
				setInt(mode, "menuCursor", cursor);
				press(engine, Controller.BUTTON_RIGHT);
				assertTrue(mode.onSetting(engine, 0));
			}
		}
		assertEquals(bigBefore, readBoolean(mode, "big"),
				"double toggle must restore the original value");
	}

	// =====================================================================
	// onSetting: switch default (L387) via an out-of-range cursor
	// =====================================================================

	@Test
	void onSettingChangeWithOutOfRangeCursorHitsSwitchDefault() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);

		setInt(mode, "menuCursor", 99);
		// UP moves 99 -> 98 (no wrap since >= 0); LEFT produces change = -1.
		press(engine, Controller.BUTTON_UP, Controller.BUTTON_LEFT);
		assertTrue(mode.onSetting(engine, 0));
		assertEquals(98, readInt(mode, "menuCursor"));
	}

	// =====================================================================
	// onSetting: start game with useMap && empty field (L603 isEmpty()==true)
	// =====================================================================

	@Test
	void onSettingStartGameUseMapEmptyFieldEvaluatesIsEmpty() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);

		engine.createFieldIfNeeded(); // exists but empty
		setBoolean(mode, "useMap", true);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		press(engine, Controller.BUTTON_A);

		boolean ret = mode.onSetting(engine, 0);
		assertFalse(ret, "start game returns false");
		assertFalse(engine.owner.menuOnly);
	}

	// =====================================================================
	// renderSetting: every cursor highlight on both pages, MANIA+ goal string,
	// IMMOBILE spin type, 1-CELL big move, nonzero time limits and hebo level,
	// plus the menuCursor >= 46 fallthrough (L676-752, L718)
	// =====================================================================

	@Test
	void renderSettingAllCursorsWithNonDefaultValues() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);

		setInt(mode, "leveltype", 4);      // MANIA+ (L704 second operand)
		setInt(mode, "goallv", 2);
		setInt(mode, "spinCheckType", 1);  // IMMOBILE (L695)
		setBoolean(mode, "bigmove", false); // 1 CELL (L700)
		setInt(mode, "timelimit", 3600);   // L712
		setInt(mode, "rolltimelimit", 3600); // L714
		setInt(mode, "heboHiddenLevel", 3); // L734

		for (int cursor = 0; cursor <= 45; cursor++) {
			setInt(mode, "menuCursor", cursor);
			mode.renderSetting(engine, 0);
		}
		// L718 false side: neither page is drawn.
		setInt(mode, "menuCursor", 99);
		mode.renderSetting(engine, 0);
		assertEquals(99, readInt(mode, "menuCursor"));
	}

	// =====================================================================
	// onReady: statc[0] != 0 (L761) and version < 1 (L769)
	// =====================================================================

	@Test
	void onReadySkipsInitWhenStatcNonZero() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "timelimit", 3600);
		setInt(mode, "timelimitTimer", 0);
		engine.statc[0] = 1;

		assertFalse(mode.onReady(engine, 0));
		assertEquals(0, readInt(mode, "timelimitTimer"),
				"timelimitTimer untouched when statc[0] != 0");
	}

	@Test
	void onReadyVersion0SkipsPieceEnableSetup() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "version", 0);
		engine.statc[0] = 0;
		engine.nextPieceEnable[0] = true;
		boolean[] pieceEnable = (boolean[]) field(mode.getClass(), "pieceEnable").get(mode);
		pieceEnable[0] = false;

		assertFalse(mode.onReady(engine, 0));
		assertTrue(engine.nextPieceEnable[0],
				"version 0 must not copy pieceEnable into the engine");
	}

	// =====================================================================
	// startGame: leveltype == MANIA+ (L808 second operand false)
	// =====================================================================

	@Test
	void startGameManiaPlusUsesManiaEngineConfig() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 4);

		mode.startGame(engine, 0);

		assertFalse(engine.tspinEnable);
		assertEquals(GameEngine.COMBO_TYPE_DOUBLE, engine.comboType);
	}

	// =====================================================================
	// renderLast: mania path edges
	// =====================================================================

	@Test
	void renderLastManiaPlusNegativeLevelAndGravityStaleScore() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "leveltype", 4);        // L918 MANIA+ branch
		setInt(mode, "lastscore", 5);
		setInt(mode, "scgettime", 200);      // L924 scgettime >= 120
		engine.statistics.level = -1;        // L930 tempLevel < 0
		engine.speed.gravity = -1;           // L935 speed = 40
		setInt(mode, "timelimit", 60);
		setInt(mode, "timelimitTimer", -5);  // L944 time < 0
		engine.gameActive = false;           // L952 gameActive false

		mode.renderLast(engine, 0);
		assertEquals(-1, engine.statistics.level);
	}

	@Test
	void renderLastManiaTimeColorThresholds() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "leveltype", 3);

		// time in [1200,1800): L946 all-true, L947/948 first-cond false
		setInt(mode, "timelimit", 3600);
		setInt(mode, "timelimitTimer", 1500);
		mode.renderLast(engine, 0);

		// time >= 1800: L946 first-cond false
		setInt(mode, "timelimitTimer", 2000);
		mode.renderLast(engine, 0);

		// timelimit == 0 with 0 < time < 600: L946/947/948 last-cond false
		setInt(mode, "timelimit", 0);
		engine.statistics.time = 100;
		mode.renderLast(engine, 0);
		assertEquals(100, engine.statistics.time);
	}

	@Test
	void renderLastManiaRollTimeClampAndBlink() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "leveltype", 3);
		engine.gameActive = true;
		engine.ending = 2;

		// remainTime < 0 -> clamped to 0 (L954), remainTime > 0 false (L956)
		setInt(mode, "rolltimelimit", 100);
		setInt(mode, "rolltime", 200);
		mode.renderLast(engine, 0);

		// 0 < remainTime < 600 -> blink flag true (L956 second operand)
		setInt(mode, "rolltimelimit", 600);
		setInt(mode, "rolltime", 300);
		mode.renderLast(engine, 0);
		assertEquals(2, engine.ending);
	}

	// =====================================================================
	// renderLast: non-mania path edges
	// =====================================================================

	@Test
	void renderLastNormalStaleScoreAndStaleEvent() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastscore", 5);
		setInt(mode, "lastevent", 1);   // EVENT_SINGLE
		setInt(mode, "scgettime", 200); // L966 + L1022 scgettime >= 120

		mode.renderLast(engine, 0);
		assertEquals(200, readInt(mode, "scgettime"));
	}

	@Test
	void renderLastPointsStaleGoalSuffix() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "leveltype", 2);   // POINTS
		setInt(mode, "lastgoal", 3);
		setInt(mode, "scgettime", 200); // L973 scgettime >= 120
		engine.ending = 0;

		mode.renderLast(engine, 0);
		assertEquals(0, engine.ending);
	}

	@Test
	void renderLastNormalTimeColorThresholds() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.stat = GameEngine.Status.MOVE;

		// time < 0 -> clamped (L1004)
		setInt(mode, "timelimit", 3600);
		setInt(mode, "timelimitTimer", -5);
		mode.renderLast(engine, 0);

		// time in [600,1200): L1007 all-true
		setInt(mode, "timelimitTimer", 800);
		mode.renderLast(engine, 0);

		// time in (0,600): L1008 all-true
		setInt(mode, "timelimitTimer", 300);
		mode.renderLast(engine, 0);

		// time >= 1800: L1006 first-cond false
		setInt(mode, "timelimitTimer", 2000);
		mode.renderLast(engine, 0);

		// timelimit == 0 with 0 < time < 600: L1006/1007/1008 last-cond false
		setInt(mode, "timelimit", 0);
		engine.statistics.time = 300;
		mode.renderLast(engine, 0);
		assertEquals(300, engine.statistics.time);
	}

	@Test
	void renderLastNormalRollTimeClampAndBlink() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 2;

		// remainTime < 0 -> clamped to 0 (L1014), remainTime > 0 false (L1016)
		setInt(mode, "rolltimelimit", 100);
		setInt(mode, "rolltime", 200);
		mode.renderLast(engine, 0);

		// 0 < remainTime < 600 -> blink flag true (L1016 second operand)
		setInt(mode, "rolltimelimit", 600);
		setInt(mode, "rolltime", 300);
		mode.renderLast(engine, 0);
		assertEquals(2, engine.ending);
	}

	@Test
	void renderLastUnknownLastEventHitsSwitchDefault() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastevent", 99); // != EVENT_NONE, matches no case (L1025)
		setInt(mode, "scgettime", 10);

		mode.renderLast(engine, 0);
		assertEquals(99, readInt(mode, "lastevent"));
	}

	// =====================================================================
	// onLast: out-of-time check with timerActive false (L1103) and
	// BGM fadeout skipped when timelimitResetEveryLevel is true (L1117)
	// =====================================================================

	@Test
	void onLastOutOfTimeIgnoredWhenTimerInactive() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "timelimit", 60);
		setInt(mode, "timelimitTimer", 0);
		engine.gameActive = false;
		engine.timerActive = false;
		engine.ending = 0;

		mode.onLast(engine, 0);
		assertTrue(engine.stat != GameEngine.Status.GAMEOVER,
				"no game over while timer inactive");
	}

	@Test
	void onLastNoBgmFadeWhenTimeLimitResetsEveryLevel() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "timelimit", 60);
		setInt(mode, "timelimitTimer", 100);
		setBoolean(mode, "timelimitResetEveryLevel", true);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ending = 0;
		engine.owner.bgmStatus.fadesw = false;

		mode.onLast(engine, 0);
		assertFalse(engine.owner.bgmStatus.fadesw,
				"fadeout must be skipped when the limit resets every level");
	}

	// =====================================================================
	// onGameOver: statc[0] != 0 (L1131)
	// =====================================================================

	@Test
	void onGameOverSkipsSecretGradeAfterFirstFrame() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.gameActive = true;
		engine.statc[0] = 1;
		setInt(mode, "secretGrade", -123);

		assertFalse(mode.onGameOver(engine, 0));
		assertEquals(-123, readInt(mode, "secretGrade"),
				"secretGrade untouched when statc[0] != 0");
	}

	// =====================================================================
	// onMove: mania level-up guard edges (L1144/1146/1148/1155/1161)
	// =====================================================================

	@Test
	void onMoveManiaHoldDisableAndLvupflagBlockLevelUp() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		engine.ending = 0;
		engine.statc[0] = 0;

		engine.holdDisable = true; // third operand false (L1144)
		setBoolean(mode, "lvupflag", false);
		mode.onMove(engine, 0);
		assertEquals(0, engine.statistics.level);

		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", true); // fourth operand false (L1144)
		mode.onMove(engine, 0);
		assertEquals(0, engine.statistics.level);
	}

	@Test
	void onMoveManiaLevelStopAtSectionBoundary() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;

		// level == nextseclv - 1: no increment at all (L1146 false)
		setBoolean(mode, "lvupflag", false);
		engine.statistics.level = 99;
		setInt(mode, "nextseclv", 100);
		mode.onMove(engine, 0);
		assertEquals(99, engine.statistics.level);

		// level 98 -> 99 == nextseclv-1 with levelstop SE on (L1148 true,true)
		setBoolean(mode, "lvupflag", false);
		engine.statistics.level = 98;
		mode.onMove(engine, 0);
		assertEquals(99, engine.statistics.level);

		// same but levelstop SE off (L1148 true,false)
		setBoolean(mode, "lvupflag", false);
		setBoolean(mode, "lvstopse", false);
		engine.statistics.level = 98;
		mode.onMove(engine, 0);
		assertEquals(99, engine.statistics.level);
	}

	@Test
	void onMoveManiaLvupflagResetVersionCombos() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		engine.ending = 0;
		engine.statc[0] = 1;

		// version >= 1 short-circuits the hold check (L1155)
		setInt(mode, "version", 5);
		setBoolean(mode, "lvupflag", true);
		mode.onMove(engine, 0);
		assertFalse(readBoolean(mode, "lvupflag"));

		// version 0: holdDisable == false decides
		setInt(mode, "version", 0);
		setBoolean(mode, "lvupflag", true);
		engine.holdDisable = false;
		mode.onMove(engine, 0);
		assertFalse(readBoolean(mode, "lvupflag"));

		// version 0 + holdDisable: flag stays set
		setBoolean(mode, "lvupflag", true);
		engine.holdDisable = true;
		mode.onMove(engine, 0);
		assertTrue(readBoolean(mode, "lvupflag"));
	}

	@Test
	void onMoveRollAlreadyStartedSkipsRollSetup() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.ending = 2;
		setBoolean(mode, "rollstarted", true); // L1161 second operand false
		engine.blockHidden = -1;

		mode.onMove(engine, 0);
		assertEquals(-1, engine.blockHidden, "roll setup must not run twice");
	}

	// =====================================================================
	// onARE: mania level-up edges (L1184/1185/1186/1188)
	// =====================================================================

	@Test
	void onAREManiaPlusLevelUpOnLastFrame() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 4); // L1184 second operand
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 5;
		setBoolean(mode, "lvupflag", false);
		engine.statistics.level = 0;

		mode.onARE(engine, 0);
		assertEquals(1, engine.statistics.level);
		assertTrue(readBoolean(mode, "lvupflag"));
	}

	@Test
	void onAREManiaGuardEdges() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);

		// ending != 0 (L1185 first operand false)
		engine.ending = 1;
		engine.statc[0] = 5;
		engine.statc[1] = 5;
		setBoolean(mode, "lvupflag", false);
		mode.onARE(engine, 0);
		assertEquals(0, engine.statistics.level);

		// not the last frame (L1185 second operand false)
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.statc[1] = 10;
		mode.onARE(engine, 0);
		assertEquals(0, engine.statistics.level);

		// lvupflag already set (L1185 third operand false)
		engine.statc[0] = 9;
		setBoolean(mode, "lvupflag", true);
		mode.onARE(engine, 0);
		assertEquals(0, engine.statistics.level);
	}

	@Test
	void onAREManiaSectionBoundaryEdges() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		engine.ending = 0;
		engine.statc[0] = 5;
		engine.statc[1] = 5;
		setInt(mode, "nextseclv", 100);

		// level == nextseclv - 1: no increment (L1186 false)
		engine.statistics.level = 99;
		setBoolean(mode, "lvupflag", false);
		mode.onARE(engine, 0);
		assertEquals(99, engine.statistics.level);

		// 50 -> 51 != 99 (L1188 first operand false)
		engine.statistics.level = 50;
		setBoolean(mode, "lvupflag", false);
		mode.onARE(engine, 0);
		assertEquals(51, engine.statistics.level);

		// 98 -> 99 with SE on (L1188 true,true)
		engine.statistics.level = 98;
		setBoolean(mode, "lvupflag", false);
		mode.onARE(engine, 0);
		assertEquals(99, engine.statistics.level);

		// 98 -> 99 with SE off (L1188 true,false)
		setBoolean(mode, "lvstopse", false);
		engine.statistics.level = 98;
		setBoolean(mode, "lvupflag", false);
		mode.onARE(engine, 0);
		assertEquals(99, engine.statistics.level);
	}

	// =====================================================================
	// calcScore: hebo hidden edges (L1204/L1207)
	// =====================================================================

	@Test
	void calcScoreHeboHiddenZeroLinesAndUnderflowClamp() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);

		// hebo enabled but no lines (L1204 second operand false)
		engine.heboHiddenEnable = true;
		engine.heboHiddenYNow = 3;
		mode.calcScore(engine, 0, 0);
		assertEquals(3, engine.heboHiddenYNow);

		// underflow: 1 - 3 -> clamped to 0 (L1207)
		engine.heboHiddenYNow = 1;
		mode.calcScore(engine, 0, 3);
		assertEquals(0, engine.heboHiddenYNow);
	}

	// =====================================================================
	// calcScoreMania edges
	// =====================================================================

	@Test
	void calcScoreManiaDuringEndingOnlyUpdatesCombo() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		engine.createFieldIfNeeded();
		placeBlock(engine);
		engine.ending = 1; // L1229 second operand false

		mode.calcScore(engine, 0, 1);
		assertEquals(0, engine.statistics.level, "no level up during ending");
		assertEquals(0, engine.statistics.score);
	}

	@Test
	void calcScoreManiaPlusTripleAndQuadLevelBonus() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 4);
		engine.createFieldIfNeeded();
		placeBlock(engine);
		engine.ending = 0;

		// 3 lines -> +4 levels (L1237)
		engine.statistics.level = 0;
		mode.calcScore(engine, 0, 3);
		assertEquals(4, engine.statistics.level);

		// 4 lines -> +6 levels (L1238)
		engine.statistics.level = 0;
		setInt(mode, "nextseclv", 100);
		mode.calcScore(engine, 0, 4);
		assertEquals(6, engine.statistics.level);
	}

	@Test
	void calcScoreManiaSectionWithMaxBackgroundAndTimeReset() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		engine.createFieldIfNeeded();
		placeBlock(engine);
		engine.ending = 0;
		engine.owner.backgroundStatus.bg = 19; // L1260 false
		setBoolean(mode, "timelimitResetEveryLevel", true);
		setInt(mode, "timelimit", 3600); // L1270 (true,true)
		setInt(mode, "timelimitTimer", 5);
		engine.statistics.level = 98;
		setInt(mode, "nextseclv", 100);

		mode.calcScore(engine, 0, 2); // 98+2 = 100 >= nextseclv
		assertEquals(200, readInt(mode, "nextseclv"));
		assertEquals(3600, readInt(mode, "timelimitTimer"));
		assertFalse(engine.owner.backgroundStatus.fadesw,
				"no background fade at bg 19");
	}

	@Test
	void calcScoreManiaSectionWithoutTimeLimitSkipsReset() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		engine.createFieldIfNeeded();
		placeBlock(engine);
		engine.ending = 0;
		setBoolean(mode, "timelimitResetEveryLevel", true);
		setInt(mode, "timelimit", 0); // L1270 (true,false)
		setInt(mode, "timelimitTimer", 5);
		engine.statistics.level = 98;
		setInt(mode, "nextseclv", 100);

		mode.calcScore(engine, 0, 2);
		assertEquals(5, readInt(mode, "timelimitTimer"));
	}

	@Test
	void calcScoreManiaLevelStopElseIfEdges() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		engine.createFieldIfNeeded();
		placeBlock(engine);
		engine.ending = 0;
		setInt(mode, "nextseclv", 100);

		// 97 -> 98 != 99 (L1271 first operand false)
		engine.statistics.level = 97;
		mode.calcScore(engine, 0, 1);
		assertEquals(98, engine.statistics.level);

		// 98 -> 99 with SE off (L1271 true,false)
		setBoolean(mode, "lvstopse", false);
		engine.statistics.level = 98;
		mode.calcScore(engine, 0, 1);
		assertEquals(99, engine.statistics.level);

		// 98 -> 99 with SE on (L1271 true,true)
		setBoolean(mode, "lvstopse", true);
		engine.statistics.level = 98;
		mode.calcScore(engine, 0, 1);
		assertEquals(99, engine.statistics.level);
	}

	@Test
	void calcScoreManiaSlowLockClampsSpeedBonus() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		engine.createFieldIfNeeded();
		placeBlock(engine);
		engine.ending = 0;
		engine.statc[0] = 1000; // lock delay long since expired (L1287)
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);
		assertTrue(engine.statistics.score < 100,
				"speed bonus must be clamped to 0, not negative");
	}

	@Test
	void calcScoreManiaPlusManualLockNonEmptyFieldSlowLock() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 4);
		engine.createFieldIfNeeded();
		placeBlock(engine);       // L1300 non-empty (no bravo)
		engine.ending = 0;
		engine.manualLock = true; // L1297
		engine.statc[0] = 1000;   // L1306 speed bonus clamp
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);
		assertTrue(engine.statistics.score > 0);
	}

	// =====================================================================
	// calcScoreNormal edges
	// =====================================================================

	@Test
	void calcScoreNormalEzSpinZeroLinesFallsThroughAllTspinCases() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		engine.tspin = true;
		engine.tspinez = true; // L1330 second operand false, L1340 lines>0 false,
		                       // L1385 lines>=3 false

		mode.calcScore(engine, 0, 0);
		assertEquals(0, engine.statistics.score, "no points for EZ spin with 0 lines");
	}

	@Test
	void calcScoreNormalMiniDoubleWithoutAllSpinBonus() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = false; // L1368 second operand false
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);
		assertEquals(1200, engine.statistics.score,
				"mini double without all-spin bonus scores as full T-Spin double");
	}

	@Test
	void calcScoreNormalComboWithZeroLinesSkipsComboBonus() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		engine.combo = 2; // L1417 second operand false
		setInt(mode, "lastcombo", 0);

		mode.calcScore(engine, 0, 0);
		assertEquals(0, readInt(mode, "lastcombo"));
	}

	@Test
	void calcScoreNormalHugeComboClampsGoalTableIndex() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		engine.combo = 14; // cmbindex 13 >= table length 12 (L1439)
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);
		assertEquals(6, readInt(mode, "lastgoal"),
				"goal delta = 1 (single) + 5 (clamped combo table)");
	}

	@Test
	void calcScoreNormalGoalStaysPositive() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		setInt(mode, "goal", 1000); // L1442 false
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);
		assertEquals(999, readInt(mode, "goal"));
	}

	@Test
	void calcScoreNormal10LinesLevelUpWithGoalLevelAheadAndMaxBg() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 1); // 10LINES
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		engine.statistics.lines = 10; // L1447 second operand true
		engine.statistics.level = 0;
		setInt(mode, "goallv", 5);    // L1450 first operand false -> level up
		engine.owner.backgroundStatus.bg = 19; // L1457 false
		setBoolean(mode, "timelimitResetEveryLevel", true);
		setInt(mode, "timelimit", 3600); // L1466 (true,true)
		setInt(mode, "timelimitTimer", 7);

		mode.calcScore(engine, 0, 1);
		assertEquals(1, engine.statistics.level);
		assertEquals(3600, readInt(mode, "timelimitTimer"));
		assertFalse(engine.owner.backgroundStatus.fadesw);
	}

	@Test
	void calcScoreNormal10LinesLevelUpWithoutTimeLimit() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 1);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		engine.statistics.lines = 10;
		engine.statistics.level = 0;
		setInt(mode, "goallv", 5);
		setBoolean(mode, "timelimitResetEveryLevel", true);
		setInt(mode, "timelimit", 0); // L1466 (true,false)
		setInt(mode, "timelimitTimer", 7);

		mode.calcScore(engine, 0, 1);
		assertEquals(1, engine.statistics.level);
		assertEquals(7, readInt(mode, "timelimitTimer"));
	}

	@Test
	void calcScoreNormalPointsWithGoalRemainingDoesNotLevelUp() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 2); // POINTS
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		setInt(mode, "goal", 50); // stays > 0 after a single (L1447 last operand false)
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);
		assertEquals(0, engine.statistics.level);
		assertEquals(49, readInt(mode, "goal"));
	}

	@Test
	void calcScoreNormalLineGoalEndingVersionEdges() throws Exception {
		// version < 2: whole condition short-circuits (L1473 first operand)
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		setInt(mode, "version", 1);
		mode.calcScore(engine, 0, 1);
		assertEquals(0, engine.ending);

		// goal level far away: lines < goallv + 1 (L1473 third operand false)
		mode = new PracticeMode();
		engine = freshInitedEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		setInt(mode, "goallv", 100);
		engine.statistics.lines = 1;
		mode.calcScore(engine, 0, 1);
		assertEquals(0, engine.ending);

		// version 2 + endless goal: (goallv != -1) || (version <= 2) true via
		// the version clause (L1473 last operand true) -> instant ending
		mode = new PracticeMode();
		engine = freshInitedEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		placeBlock(engine);
		setInt(mode, "version", 2);
		setInt(mode, "goallv", -1);
		setInt(mode, "rolltimelimit", 0);
		engine.statistics.lines = 5;
		mode.calcScore(engine, 0, 1);
		assertEquals(1, engine.ending, "version <= 2 endless goal ends the game");
	}

	// =====================================================================
	// setMeter edges (invoked via reflection, meter max = 100)
	// =====================================================================

	@Test
	void setMeterRollWithPlentyOfTimeStaysGreen() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltimelimit", 7200); // remain 7200 > all thresholds
		setInt(mode, "rolltime", 0);

		invokeSetMeter(mode, engine);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
		assertEquals(100, engine.meterValue);
	}

	@Test
	void setMeterNegativeRollRemainderClampsToZero() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltimelimit", 100);
		setInt(mode, "rolltime", 200); // meterValue < 0 (L1542)

		invokeSetMeter(mode, engine);
		assertEquals(0, engine.meterValue);
	}

	@Test
	void setMeterOverfullTimeLimitClampsToMax() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "timelimit", 100);
		setInt(mode, "timelimitTimer", 1000); // meterValue > max (L1543)

		invokeSetMeter(mode, engine);
		assertEquals(100, engine.meterValue);
	}

	@Test
	void setMeter10LinesEarlyInSectionStaysGreen() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 1);
		engine.statistics.lines = 3; // below all color thresholds (L1519-21)

		invokeSetMeter(mode, engine);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	@Test
	void setMeterPointsFullGoalStaysGreen() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 2);
		setInt(mode, "goal", 5); // full meter (L1525-27 false)
		engine.statistics.level = 0;

		invokeSetMeter(mode, engine);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
		assertEquals(100, engine.meterValue);
	}

	@Test
	void setMeterLineGoalJustStartedStaysGreen() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 0);
		setInt(mode, "goallv", 99);
		engine.statistics.lines = 0; // meterValue 0 < max/10 (L1537-39)

		invokeSetMeter(mode, engine);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
		assertEquals(0, engine.meterValue);
	}

	@Test
	void setMeterUnknownLevelTypeAndEndlessNoneSkipMeter() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);

		// leveltype outside all cases: L1534 first operand false
		setInt(mode, "leveltype", 99);
		engine.meterValue = 42;
		invokeSetMeter(mode, engine);
		assertEquals(42, engine.meterValue, "no branch taken; only clamped");

		// NONE + endless: L1534 second operand false
		setInt(mode, "leveltype", 0);
		setInt(mode, "goallv", -1);
		invokeSetMeter(mode, engine);
		assertEquals(42, engine.meterValue);
	}

	// =====================================================================
	// afterSoftDropFall / afterHardDropFall leveltype variants
	// =====================================================================

	@Test
	void softDropGivesNoScoreInManiaTypes() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);

		setInt(mode, "leveltype", 3); // L1551 first operand false
		mode.afterSoftDropFall(engine, 0, 3);
		assertEquals(0, engine.statistics.score);

		setInt(mode, "leveltype", 4); // L1551 second operand false
		mode.afterSoftDropFall(engine, 0, 3);
		assertEquals(0, engine.statistics.score);
	}

	@Test
	void hardDropKeepsLargerExistingBonusInMania() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setInt(mode, "leveltype", 3);
		setInt(mode, "harddropBonus", 100);

		mode.afterHardDropFall(engine, 0, 1); // 2 > 100 false (L1563)
		assertEquals(100, readInt(mode, "harddropBonus"));
	}

	// =====================================================================
	// saveReplay: useMap without a field backup (L1589 second operand false)
	// =====================================================================

	@Test
	void saveReplayUseMapWithoutBackupSkipsMapWrite() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshInitedEngine(mode);
		setBoolean(mode, "useMap", true);
		field(mode.getClass(), "fldBackup").set(mode, null);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);
		assertEquals(null, prop.getProperty("0.field.width", null),
				"no map data written without a backup field");
	}

	// ---- helpers ----

	private static void placeBlock(GameEngine engine) {
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
	}

	private static void invokeSetMeter(PracticeMode mode, GameEngine engine) throws Exception {
		Method m = PracticeMode.class.getDeclaredMethod("setMeter", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
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
