package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
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
 * Coverage-boost tests for {@link PracticeMode}. Targets the configuration
 * menu A-button decide paths (field edit / map load+save / preset load+save /
 * start game), renderSetting page branches, renderLast (NONE / MANIA / field
 * edit / all line-clear events), onReady map-backup paths, startGame T-spin
 * branches, onLast timeout/countdown, onMove/onARE MANIA level-up, calcScore
 * (mania + normal scoring branches), setMeter meter-color branches,
 * afterHardDropFall, renderResult secret grade, and saveReplay.
 */
class PracticeModeCoverageBoostTest {

	// ---------------------------------------------------------------
	// playerInit
	// ---------------------------------------------------------------

	@Test
	void playerInitNormalMode() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		assertEquals(GameEngine.FRAME_COLOR_YELLOW, engine.framecolor);
		assertEquals(GameEngine.FRAME_COLOR_YELLOW, engine.framecolor);
	}

	@Test
	void playerInitReplayMode() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = true;
		engine.owner.replayProp = new CustomProperties();
		mode.playerInit(engine, 0);
		// replay branch (lines 250-253) read version/loadPreset from replayProp
		assertEquals(0, readInt(mode, "presetNumber"));
		assertEquals(0, readInt(mode, "mapNumber"));
	}

	// ---------------------------------------------------------------
	// onSetting: A-button decide paths (lines 567-609)
	// ---------------------------------------------------------------

	@Test
	void onSettingDecideFieldEditCursor41() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenu(engine, mode, 41, 10);
		pressKey(engine, Controller.BUTTON_A);
		boolean ret = mode.onSetting(engine, 0);
		assertTrue(ret, "field edit path returns true");
		assertEquals(GameEngine.Status.FIELDEDIT, engine.stat);
	}

	@Test
	void onSettingDecideMapLoadCursor42() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenu(engine, mode, 42, 10);
		setInt(mode, "mapNumber", 0);
		pressKey(engine, Controller.BUTTON_A);
		// loadProperties returns null for a missing map -> safe branch
		mode.onSetting(engine, 0);
		assertNotNull(engine.field);
	}

	@Test
	void onSettingDecideMapSaveCursor43() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setMenu(engine, mode, 43, 10);
		setInt(mode, "mapNumber", 97);
		File f = new File("config/map/practice/97.map");
		try {
			pressKey(engine, Controller.BUTTON_A);
			mode.onSetting(engine, 0);
			// file is written by the save path; just verify no crash
		} finally {
			f.delete();
		}
	}

	@Test
	void onSettingDecidePresetLoadCursor44() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenu(engine, mode, 44, 10);
		engine.speed.gravity = 12345;
		pressKey(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		// loadPreset always overwrites gravity from the (preset) properties,
		// so it can no longer be the sentinel value.
		assertTrue(engine.speed.gravity != 12345,
				"loadPreset should overwrite gravity");
	}

	@Test
	void onSettingDecidePresetSaveCursor45() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenu(engine, mode, 45, 10);
		setInt(mode, "presetNumber", 0);
		engine.speed.gravity = 77;
		pressKey(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(77, engine.owner.modeConfig.getProperty("practice.gravity.0", -1));
	}

	@Test
	void onSettingDecideStartGameNoMap() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenu(engine, mode, 0, 10);
		pressKey(engine, Controller.BUTTON_A);
		boolean ret = mode.onSetting(engine, 0);
		assertFalse(ret, "start game returns false");
		assertFalse(engine.owner.menuOnly);
	}

	@Test
	void onSettingDecideStartGameUseMapNoFile() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenu(engine, mode, 0, 10);
		setBoolean(mode, "useMap", true);
		setInt(mode, "mapNumber", 98); // no such file -> useMap reset to false
		// no field set so isEmpty/null path taken
		pressKey(engine, Controller.BUTTON_A);
		boolean ret = mode.onSetting(engine, 0);
		assertFalse(ret);
		assertFalse(readBoolean(mode, "useMap"), "missing map file disables useMap");
	}

	@Test
	void onSettingCancelQuits() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenu(engine, mode, 0, 10);
		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingChangeGravityFast() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenu(engine, mode, 0, 10);
		engine.speed.gravity = 0;
		// RIGHT + E button => m = 100
		pressKey(engine, Controller.BUTTON_RIGHT);
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 5;
		mode.onSetting(engine, 0);
		assertEquals(100, engine.speed.gravity);
	}

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = true;
		setInt(mode, "menuTime", 0);

		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "menuCursor"));

		setInt(mode, "menuTime", 60);
		mode.onSetting(engine, 0);
		assertEquals(22, readInt(mode, "menuCursor"));

		setInt(mode, "menuTime", 120);
		boolean ret = mode.onSetting(engine, 0);
		assertFalse(ret);
	}

	// ---------------------------------------------------------------
	// renderSetting branches
	// ---------------------------------------------------------------

	@Test
	void renderSettingPage1OldVersion() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 3); // < 4 -> SPIN BONUS uses enableTSpin (line 676)
		setInt(mode, "menuCursor", 0);
		setInt(mode, "goallv", 5);
		setInt(mode, "leveltype", 3); // MANIA -> goal level "LVxxx" (689-690)
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage1GoalLevelNone() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 5);
		setInt(mode, "menuCursor", 19);
		setInt(mode, "goallv", 3);
		setInt(mode, "leveltype", 0); // NONE -> "x LINES" (691-692)
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage1GoalLevelPoints() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "menuCursor", 19);
		setInt(mode, "goallv", 3);
		setInt(mode, "leveltype", 2); // POINTS -> "LVx" else-branch (693-694)
		setInt(mode, "tspinEnableType", 0);
		mode.renderSetting(engine, 0);
		setInt(mode, "tspinEnableType", 2);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage2() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "menuCursor", 24);
		setInt(mode, "blockHidden", -2);
		setInt(mode, "heboHiddenLevel", 3);
		mode.renderSetting(engine, 0);
		setInt(mode, "blockHidden", 60);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingReplayMode() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		setInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
		setInt(mode, "menuCursor", 30);
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// onReady map backup paths (763-770)
	// ---------------------------------------------------------------

	@Test
	void onReadyUseMapReplayMode() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "useMap", true);
		engine.owner.replayMode = true;
		engine.owner.replayProp = new CustomProperties();
		engine.statc[0] = 0;
		mode.onReady(engine, 0);
		assertNotNull(engine.field);
	}

	@Test
	void onReadyUseMapBackup() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "useMap", true);
		engine.owner.replayMode = false;
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		mode.onReady(engine, 0);
		assertNotNull(readField(mode, "fldBackup"));
	}

	@Test
	void onReadyNoMapResetsField() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "useMap", false);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		mode.onReady(engine, 0);
		assertTrue(engine.field.isEmpty());
	}

	// ---------------------------------------------------------------
	// startGame T-spin branches (801, 805-806, 809)
	// ---------------------------------------------------------------

	@Test
	void startGameTspinOff() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 5);
		setInt(mode, "tspinEnableType", 0);
		setInt(mode, "leveltype", 0);
		mode.startGame(engine, 0);
		assertFalse(engine.tspinEnable);
	}

	@Test
	void startGameTspinAllSpin() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 5);
		setInt(mode, "tspinEnableType", 2);
		setInt(mode, "leveltype", 0);
		mode.startGame(engine, 0);
		assertTrue(engine.tspinEnable);
		assertTrue(engine.useAllSpinBonus);
	}

	@Test
	void startGameTspinOldVersion() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 3); // < 4 -> enableTSpin path (809)
		setBoolean(mode, "enableTSpin", true);
		setInt(mode, "leveltype", 0);
		mode.startGame(engine, 0);
		assertTrue(engine.tspinEnable);
	}

	@Test
	void startGameManiaType() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 3); // MANIA
		mode.startGame(engine, 0);
		assertFalse(engine.tspinEnable);
		assertEquals(GameEngine.COMBO_TYPE_DOUBLE, engine.comboType);
	}

	// ---------------------------------------------------------------
	// renderLast branches
	// ---------------------------------------------------------------

	@Test
	void renderLastFieldEdit() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.FIELDEDIT;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastManiaWithRollTime() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "leveltype", 3); // MANIA
		setInt(mode, "lastscore", 50);
		setInt(mode, "scgettime", 5);
		setInt(mode, "timelimit", 3600);
		setInt(mode, "timelimitTimer", 5 * 60); // < 10*60 -> RED font
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltimelimit", 3600);
		setInt(mode, "rolltime", 60);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastPointsWithGoal() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "leveltype", 2); // POINTS
		setInt(mode, "lastscore", 10);
		setInt(mode, "lastgoal", 3);
		setInt(mode, "scgettime", 5);
		engine.ending = 0;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLast10Lines() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "leveltype", 1); // 10LINES
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastNoneWithRollTimeAndAllEvents() throws Exception {
		// NONE leveltype with roll-time and every line-clear event drawn
		int[] events = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
		for (int ev : events) {
			for (boolean b2b : new boolean[]{false, true}) {
				PracticeMode mode = new PracticeMode();
				GameEngine engine = freshEngine(mode);
				mode.playerInit(engine, 0);
				engine.stat = GameEngine.Status.MOVE;
				setInt(mode, "leveltype", 0); // NONE
				setInt(mode, "lastscore", 100);
				setInt(mode, "scgettime", 5);
				setInt(mode, "lastevent", ev);
				setBoolean(mode, "lastb2b", b2b);
				setInt(mode, "lastcombo", 3);
				setInt(mode, "lastpiece", Piece.PIECE_T);
				setInt(mode, "timelimit", 3600);
				setInt(mode, "timelimitTimer", 25 * 60); // < 30*60 yellow
				engine.gameActive = true;
				engine.ending = 2;
				setInt(mode, "rolltimelimit", 3600);
				setInt(mode, "rolltime", 60);
				mode.renderLast(engine, 0);
			}
		}
	}

	@Test
	void renderLastNoneLevelTypeDisplay() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "leveltype", 1); // 10LINES -> level shown (line 970-972)
		engine.statistics.level = 2;
		mode.renderLast(engine, 0);
	}

	// ---------------------------------------------------------------
	// onLast branches (1089, 1094)
	// ---------------------------------------------------------------

	@Test
	void onLastTimeoutGameOver() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "timelimit", 3600);
		setInt(mode, "timelimitTimer", 0);
		setInt(mode, "goallv", 5); // != -1 -> GAMEOVER (line 1089)
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ending = 0;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	@Test
	void onLastTimeoutEndingStart() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "timelimit", 3600);
		setInt(mode, "timelimitTimer", 0);
		setInt(mode, "goallv", -1); // -1 -> ENDINGSTART
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ending = 0;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);
	}

	@Test
	void onLastCountdownSE() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "timelimit", 3600);
		setInt(mode, "timelimitTimer", 600); // <=10*60 and %60==0 -> countdown (1094)
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ending = 0;
		mode.onLast(engine, 0);
		// timelimitTimer was decremented before the countdown check
		assertTrue(readInt(mode, "timelimitTimer") >= 0);
	}

	@Test
	void onLastRollEnd() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 100);
		setInt(mode, "rolltimelimit", 100);
		mode.onLast(engine, 0);
		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	// ---------------------------------------------------------------
	// onMove / onARE MANIA branches (1137, 1146-1188)
	// ---------------------------------------------------------------

	@Test
	void onMoveManiaLevelUp() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 3); // MANIA
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "lvstopse", true);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		engine.statistics.level = 0;
		mode.onMove(engine, 0);
		assertEquals(1, engine.statistics.level);
	}

	@Test
	void onMoveManiaLvupflagReset() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 3); // MANIA
		setBoolean(mode, "lvupflag", true);
		setInt(mode, "version", 5);
		engine.ending = 0;
		engine.statc[0] = 5; // > 0 -> reset lvupflag (line 1137)
		mode.onMove(engine, 0);
		assertFalse(readBoolean(mode, "lvupflag"));
	}

	@Test
	void onMoveEndingStartMania() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 3); // MANIA -> blockHidden=300, outline NONE (1146-1151)
		setBoolean(mode, "rollstarted", false);
		engine.ending = 2;
		mode.onMove(engine, 0);
		assertTrue(readBoolean(mode, "rollstarted"));
		assertEquals(300, engine.blockHidden);
	}

	@Test
	void onMoveEndingStartManiaPlus() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 4); // MANIAPLUS (no outline change)
		setBoolean(mode, "rollstarted", false);
		engine.ending = 2;
		mode.onMove(engine, 0);
		assertTrue(readBoolean(mode, "rollstarted"));
	}

	@Test
	void onAREManiaLevelUp() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 3); // MANIA
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "lvstopse", true);
		setBoolean(mode, "lvupflag", false);
		engine.ending = 0;
		engine.statc[0] = 10;
		engine.statc[1] = 10; // statc[0] >= statc[1]-1
		engine.statistics.level = 98; // -> becomes 99 == nextseclv-1, plays levelstop
		mode.onARE(engine, 0);
		assertEquals(99, engine.statistics.level);
		assertTrue(readBoolean(mode, "lvupflag"));
	}

	// ---------------------------------------------------------------
	// calcScore MANIA branches (1225-1264)
	// ---------------------------------------------------------------

	@Test
	void calcScoreManiaEndingNoRoll() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "leveltype", 3); // MANIA
		setInt(mode, "goallv", 0); // ending at level 100
		setInt(mode, "rolltimelimit", 0); // gameEnded path (1228-1230)
		engine.ending = 0;
		engine.statistics.level = 99;
		mode.calcScore(engine, 0, 4); // levelplus 6 -> >= 100
		assertEquals(1, engine.ending);
		assertEquals(100, engine.statistics.level);
	}

	@Test
	void calcScoreManiaEndingWithRoll() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "leveltype", 3); // MANIA
		setInt(mode, "goallv", 0);
		setInt(mode, "rolltimelimit", 3600); // staffroll path (1232-1234)
		engine.ending = 0;
		engine.statistics.level = 99;
		mode.calcScore(engine, 0, 4);
		assertEquals(1, engine.ending);
		assertTrue(engine.staffrollEnable);
	}

	@Test
	void calcScoreManiaNextSection() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "leveltype", 3); // MANIA
		setInt(mode, "goallv", -1); // endless
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "timelimitResetEveryLevel", true);
		setInt(mode, "timelimit", 3600);
		engine.ending = 0;
		engine.statistics.level = 99;
		mode.calcScore(engine, 0, 1); // level 100 >= nextseclv (1236-1251)
		assertEquals(200, readInt(mode, "nextseclv"));
	}

	@Test
	void calcScoreManiaLevelStopSE() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "leveltype", 3); // MANIA
		setInt(mode, "goallv", -1);
		setInt(mode, "nextseclv", 100);
		setBoolean(mode, "lvstopse", true);
		engine.ending = 0;
		engine.statistics.level = 98;
		mode.calcScore(engine, 0, 1); // level 99 == nextseclv-1 (1252-1253)
		assertEquals(99, engine.statistics.level);
	}

	@Test
	void calcScoreManiaBravo() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded(); // empty field -> bravo (1262-1264)
		setInt(mode, "leveltype", 3); // MANIA
		setInt(mode, "goallv", -1);
		setInt(mode, "nextseclv", 100);
		engine.ending = 0;
		engine.statistics.level = 5;
		engine.manualLock = true;
		mode.calcScore(engine, 0, 2);
		assertTrue(engine.statistics.score > 0);
	}

	// ---------------------------------------------------------------
	// calcScore NORMAL branches (1312-1383, 1415, 1431-1472)
	// ---------------------------------------------------------------

	@Test
	void calcScoreTspinZeroMini() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "leveltype", 0); // NONE
		engine.ending = 0;
		engine.tspin = true;
		engine.tspinmini = true;
		engine.tspinez = false;
		mode.calcScore(engine, 0, 0); // 1312-1314, scoreFromOtherBonus (1415)
		assertEquals(5, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTspinZero() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "leveltype", 0);
		engine.ending = 0;
		engine.tspin = true;
		engine.tspinmini = false;
		engine.tspinez = false;
		mode.calcScore(engine, 0, 0); // 1316-1317
		assertEquals(6, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTspinEz() throws Exception {
		for (boolean b2b : new boolean[]{false, true}) {
			PracticeMode mode = new PracticeMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			engine.createFieldIfNeeded();
			engine.field.setBlock(0, engine.field.getHeight() - 1,
					new Block(Block.BLOCK_COLOR_GRAY));
			engine.nowPieceObject = new Piece(Piece.PIECE_T);
			setInt(mode, "leveltype", 0);
			engine.ending = 0;
			engine.tspin = true;
			engine.tspinez = true;
			engine.b2b = b2b;
			mode.calcScore(engine, 0, 1); // 1321-1327
			assertEquals(12, readInt(mode, "lastevent"));
		}
	}

	@Test
	void calcScoreTspinSingleAndMini() throws Exception {
		for (boolean mini : new boolean[]{false, true}) {
			for (boolean b2b : new boolean[]{false, true}) {
				PracticeMode mode = new PracticeMode();
				GameEngine engine = freshEngine(mode);
				mode.playerInit(engine, 0);
				engine.createFieldIfNeeded();
				engine.field.setBlock(0, engine.field.getHeight() - 1,
						new Block(Block.BLOCK_COLOR_GRAY));
				engine.nowPieceObject = new Piece(Piece.PIECE_T);
				setInt(mode, "leveltype", 0);
				engine.ending = 0;
				engine.tspin = true;
				engine.tspinmini = mini;
				engine.tspinez = false;
				engine.b2b = b2b;
				mode.calcScore(engine, 0, 1); // 1330-1345
				int ev = readInt(mode, "lastevent");
				assertTrue(ev == 7 || ev == 8);
			}
		}
	}

	@Test
	void calcScoreTspinDoubleAndMini() throws Exception {
		for (boolean miniAll : new boolean[]{false, true}) {
			for (boolean b2b : new boolean[]{false, true}) {
				PracticeMode mode = new PracticeMode();
				GameEngine engine = freshEngine(mode);
				mode.playerInit(engine, 0);
				engine.createFieldIfNeeded();
				engine.field.setBlock(0, engine.field.getHeight() - 1,
						new Block(Block.BLOCK_COLOR_GRAY));
				engine.nowPieceObject = new Piece(Piece.PIECE_T);
				setInt(mode, "leveltype", 0);
				engine.ending = 0;
				engine.tspin = true;
				engine.tspinmini = miniAll;
				engine.useAllSpinBonus = miniAll;
				engine.tspinez = false;
				engine.b2b = b2b;
				mode.calcScore(engine, 0, 2); // 1348-1362
				int ev = readInt(mode, "lastevent");
				assertTrue(ev == 9 || ev == 10);
			}
		}
	}

	@Test
	void calcScoreTspinTriple() throws Exception {
		for (boolean b2b : new boolean[]{false, true}) {
			PracticeMode mode = new PracticeMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			engine.createFieldIfNeeded();
			engine.field.setBlock(0, engine.field.getHeight() - 1,
					new Block(Block.BLOCK_COLOR_GRAY));
			engine.nowPieceObject = new Piece(Piece.PIECE_T);
			setInt(mode, "leveltype", 0);
			engine.ending = 0;
			engine.tspin = true;
			engine.tspinmini = false;
			engine.tspinez = false;
			engine.b2b = b2b;
			mode.calcScore(engine, 0, 3); // 1366-1372
			assertEquals(11, readInt(mode, "lastevent"));
		}
	}

	@Test
	void calcScoreNormalSingleDoubleTriple() throws Exception {
		int[][] cases = {{1, 1}, {2, 2}, {3, 3}};
		for (int[] c : cases) {
			PracticeMode mode = new PracticeMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			engine.createFieldIfNeeded();
			engine.field.setBlock(0, engine.field.getHeight() - 1,
					new Block(Block.BLOCK_COLOR_GRAY));
			engine.nowPieceObject = new Piece(Piece.PIECE_T);
			setInt(mode, "leveltype", 0);
			engine.ending = 0;
			engine.tspin = false;
			engine.combo = 3; // combo bonus (1398-1400)
			mode.calcScore(engine, 0, c[0]);
			assertEquals(c[1], readInt(mode, "lastevent"));
		}
	}

	@Test
	void calcScoreNormalFourB2B() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "leveltype", 0);
		engine.ending = 0;
		engine.tspin = false;
		engine.b2b = true;
		mode.calcScore(engine, 0, 4); // 1386-1391
		assertEquals(4, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreNormalAllClear() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded(); // empty -> all clear (1404-1406)
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "leveltype", 2); // POINTS
		engine.ending = 0;
		engine.tspin = false;
		mode.calcScore(engine, 0, 1);
		assertTrue(engine.statistics.score > 0);
	}

	@Test
	void calcScore10LinesLevelUp() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "leveltype", 1); // 10LINES
		setInt(mode, "goallv", -1); // not goal yet -> level up (1435-1449)
		setBoolean(mode, "timelimitResetEveryLevel", true);
		setInt(mode, "timelimit", 3600);
		engine.ending = 0;
		engine.tspin = false;
		engine.statistics.level = 0;
		engine.statistics.lines = 10; // >= (level+1)*10
		mode.calcScore(engine, 0, 1);
		assertEquals(1, engine.statistics.level);
	}

	@Test
	void calcScorePointsEndingNoRoll() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "leveltype", 2); // POINTS
		setInt(mode, "goal", 1); // pts will drive goal <= 0
		setInt(mode, "goallv", 0); // level >= goallv -> ending (1431-1433, 1459-1465)
		setInt(mode, "rolltimelimit", 0);
		engine.ending = 0;
		engine.tspin = false;
		engine.statistics.level = 0;
		mode.calcScore(engine, 0, 1);
		assertEquals(1, engine.ending);
	}

	@Test
	void calcScoreNoneEndingWithRoll() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "leveltype", 0); // NONE
		setInt(mode, "version", 5);
		setInt(mode, "goallv", 0); // lines >= goallv+1 (1) -> ending (1454-1455)
		setInt(mode, "rolltimelimit", 3600); // ending==2 staffroll (1466-1470)
		engine.ending = 0;
		engine.tspin = false;
		engine.statistics.lines = 1;
		mode.calcScore(engine, 0, 1);
		assertEquals(2, engine.ending);
		assertTrue(engine.staffrollEnable);
	}

	@Test
	void calcScoreHeboHiddenDecrease() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setInt(mode, "leveltype", 0);
		engine.ending = 0;
		engine.tspin = false;
		engine.heboHiddenEnable = true;
		engine.heboHiddenYNow = 5;
		mode.calcScore(engine, 0, 2); // 1185-1188
		assertEquals(3, engine.heboHiddenYNow);
	}

	// ---------------------------------------------------------------
	// setMeter branches (via onLast / startGame) (1483-1521)
	// ---------------------------------------------------------------

	@Test
	void setMeterRollTime() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltimelimit", 3600);
		setInt(mode, "rolltime", 3600 - 5 * 60); // remain 5*60 -> RED (1489)
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void setMeterTimeLimit() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "timelimit", 3600);
		setInt(mode, "timelimitTimer", 5 * 60); // RED (1496)
		engine.gameActive = false;
		engine.ending = 0;
		engine.timerActive = false;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void setMeter10Lines() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 1); // 10LINES (1497-1502)
		engine.statistics.lines = 8; // %10==8 -> RED
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void setMeterPoints() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 2); // POINTS (1503-1508)
		setInt(mode, "goal", 1);
		engine.statistics.level = 0;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void setMeterMania() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 3); // MANIA (1509-1514)
		setInt(mode, "nextseclv", 100);
		engine.statistics.level = 99; // == nextseclv-1 -> RED
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void setMeterNoneWithGoal() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 0); // NONE (1515-1520)
		setInt(mode, "goallv", 9);
		engine.statistics.lines = 10;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	// ---------------------------------------------------------------
	// afterSoftDropFall / afterHardDropFall (1531-1548)
	// ---------------------------------------------------------------

	@Test
	void afterHardDropFallMania() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 3); // MANIA (1543-1544)
		mode.afterHardDropFall(engine, 0, 5);
		assertEquals(10, readInt(mode, "harddropBonus"));
	}

	@Test
	void afterHardDropFallNormal() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 0); // NONE
		engine.statistics.score = 0;
		mode.afterHardDropFall(engine, 0, 5);
		assertEquals(10, engine.statistics.score);
	}

	@Test
	void afterSoftDropFallNormal() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 0);
		engine.statistics.score = 0;
		mode.afterSoftDropFall(engine, 0, 3);
		assertEquals(3, engine.statistics.score);
	}

	// ---------------------------------------------------------------
	// renderResult (1556-1561)
	// ---------------------------------------------------------------

	@Test
	void renderResultWithSecretGrade() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "secretGrade", 5); // > 0 -> S.GRADE drawn (1558-1560)
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultNoSecretGrade() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "secretGrade", 0);
		mode.renderResult(engine, 0);
	}

	// ---------------------------------------------------------------
	// saveReplay (1568-1573)
	// ---------------------------------------------------------------

	@Test
	void saveReplayWithMapBackup() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "useMap", true);
		engine.createFieldIfNeeded();
		// give fldBackup a field so the saveMap branch (1571) executes
		Field f = findField(mode.getClass(), "fldBackup");
		f.set(mode, new nullpomino.game.component.Field(engine.field));
		mode.saveReplay(engine, 0, engine.owner.replayProp);
		assertEquals(5, engine.owner.replayProp.getProperty("practice.version", -1));
	}

	@Test
	void saveReplayNoMap() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "useMap", false);
		mode.saveReplay(engine, 0, engine.owner.replayProp);
		assertEquals(5, engine.owner.replayProp.getProperty("practice.version", -1));
	}

	// ---------------------------------------------------------------
	// onGameOver
	// ---------------------------------------------------------------

	@Test
	void onGameOverReadsSecretGrade() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		engine.gameActive = true;
		boolean ret = mode.onGameOver(engine, 0);
		assertFalse(ret);
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(PracticeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static void setMenu(GameEngine engine, PracticeMode mode, int cursor, int menuTime)
			throws Exception {
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", cursor);
		setInt(mode, "menuTime", menuTime);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static int readInt(PracticeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		return f.getInt(mode);
	}

	private static boolean readBoolean(PracticeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		return f.getBoolean(mode);
	}

	private static Object readField(PracticeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		return f.get(mode);
	}

	private static void setInt(PracticeMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setInt(mode, value);
	}

	private static void setBoolean(PracticeMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setBoolean(mode, value);
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
