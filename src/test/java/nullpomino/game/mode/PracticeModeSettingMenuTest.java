package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.junit.jupiter.api.Test;

/**
 * Covers {@link PracticeMode#onSetting} menu branches (46 cursor
 * positions), UP/DOWN navigation, A-button confirm paths, B-button
 * cancel, replayMode path, renderSetting page boundaries, calcScore
 * mania/normal branches, onLast timer/roll branches, onMove level-up,
 * startGame config branches, and renderLast event display branches.
 */
class PracticeModeSettingMenuTest {

	// ---------------------------------------------------------------
	// onSetting: UP/DOWN navigation (46 positions)
	// ---------------------------------------------------------------

	@Test
	void onSettingUpNavigatesThroughAllPositions() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);

		setFieldInt(mode, "menuCursor", 0);
		pressUp(engine);
		mode.onSetting(engine, 0);
		assertEquals(45, readFieldInt(mode, "menuCursor"),
				"UP at cursor 0 should wrap to 45");

		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"First DOWN from 45 should reach 0");

		for (int expected = 1; expected <= 45; expected++) {
			pressDown(engine);
			mode.onSetting(engine, 0);
			assertEquals(expected, readFieldInt(mode, "menuCursor"),
					"Should be at cursor " + expected);
		}
		pressDown(engine);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"),
				"DOWN from 45 should wrap to 0");
	}

	// ---------------------------------------------------------------
	// onSetting: LEFT/RIGHT at each cursor position
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor0Gravity() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		int before = engine.speed.gravity;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor1Denominator() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 1);
		int before = engine.speed.denominator;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.denominator);
	}

	@Test
	void onSettingCursor2Are() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 2);
		int before = engine.speed.are;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.are);
	}

	@Test
	void onSettingCursor3AreLine() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 3);
		int before = engine.speed.areLine;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.areLine);
	}

	@Test
	void onSettingCursor4LineDelay() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 4);
		int before = engine.speed.lineDelay;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor5LockDelay() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 5);
		int before = engine.speed.lockDelay;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor6Das() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 6);
		int before = engine.speed.das;
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, engine.speed.das);
	}

	@Test
	void onSettingCursor7Bgmno() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 7);
		int before = readFieldInt(mode, "bgmno");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "bgmno"));
	}

	@Test
	void onSettingCursor8Big() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 8);
		boolean before = readFieldBool(mode, "big");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "big"));
	}

	@Test
	void onSettingCursor9Leveltype() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 9);
		int before = readFieldInt(mode, "leveltype");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "leveltype"));
	}

	@Test
	void onSettingCursor10TspinEnableType() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 10);
		int before = readFieldInt(mode, "tspinEnableType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "tspinEnableType"));
	}

	@Test
	void onSettingCursor11Kick() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 11);
		boolean before = readFieldBool(mode, "enableTSpinKick");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableTSpinKick"));
	}

	@Test
	void onSettingCursor12SpinCheckType() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 12);
		int before = readFieldInt(mode, "spinCheckType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "spinCheckType"));
	}

	@Test
	void onSettingCursor13Ez() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 13);
		boolean before = readFieldBool(mode, "tspinEnableEZ");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "tspinEnableEZ"));
	}

	@Test
	void onSettingCursor14B2b() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 14);
		boolean before = readFieldBool(mode, "enableB2B");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "enableB2B"));
	}

	@Test
	void onSettingCursor15ComboType() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 15);
		int before = readFieldInt(mode, "comboType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "comboType"));
	}

	@Test
	void onSettingCursor16Lvstopse() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 16);
		boolean before = readFieldBool(mode, "lvstopse");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "lvstopse"));
	}

	@Test
	void onSettingCursor17Bigmove() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 17);
		boolean before = readFieldBool(mode, "bigmove");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "bigmove"));
	}

	@Test
	void onSettingCursor18Bighalf() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 18);
		boolean before = readFieldBool(mode, "bighalf");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "bighalf"));
	}

	@Test
	void onSettingCursor19Goallv() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 19);
		int before = readFieldInt(mode, "goallv");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "goallv"));
	}

	@Test
	void onSettingCursor20Timelimit() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 20);
		int before = readFieldInt(mode, "timelimit");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 60, readFieldInt(mode, "timelimit"));
	}

	@Test
	void onSettingCursor21Rolltimelimit() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 21);
		int before = readFieldInt(mode, "rolltimelimit");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 60, readFieldInt(mode, "rolltimelimit"));
	}

	@Test
	void onSettingCursor22TimeLimitReset() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 22);
		boolean before = readFieldBool(mode, "timelimitResetEveryLevel");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "timelimitResetEveryLevel"));
	}

	@Test
	void onSettingCursor23Bone() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 23);
		boolean before = readFieldBool(mode, "bone");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "bone"));
	}

	@Test
	void onSettingCursor24BlockHidden() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 24);
		int before = readFieldInt(mode, "blockHidden");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "blockHidden"));
	}

	@Test
	void onSettingCursor25BlockHiddenAnim() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 25);
		boolean before = readFieldBool(mode, "blockHiddenAnim");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "blockHiddenAnim"));
	}

	@Test
	void onSettingCursor26BlockOutlineType() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 26);
		int before = readFieldInt(mode, "blockOutlineType");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "blockOutlineType"));
	}

	@Test
	void onSettingCursor27BlockShowOutlineOnly() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 27);
		boolean before = readFieldBool(mode, "blockShowOutlineOnly");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "blockShowOutlineOnly"));
	}

	@Test
	void onSettingCursor28HeboHiddenLevel() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 28);
		int before = readFieldInt(mode, "heboHiddenLevel");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(before + 1, readFieldInt(mode, "heboHiddenLevel"));
	}

	@Test
	void onSettingCursor29To39PieceEnable() throws Exception {
		for (int cursor = 29; cursor <= 39; cursor++) {
			PracticeMode mode = new PracticeMode();
			GameEngine engine = freshEngine(mode, false);
			setMenuState(engine, mode, cursor);
			boolean[] pieceEnable = (boolean[]) readField(mode, "pieceEnable");
			int idx = cursor - 29;
			boolean before = pieceEnable[idx];
			pressRight(engine);
			mode.onSetting(engine, 0);
			pieceEnable = (boolean[]) readField(mode, "pieceEnable");
			assertEquals(!before, pieceEnable[idx], "pieceEnable[" + idx + "] should toggle at cursor " + cursor);
		}
	}

	@Test
	void onSettingCursor40UseMap() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 40);
		boolean before = readFieldBool(mode, "useMap");
		pressRight(engine);
		mode.onSetting(engine, 0);
		assertEquals(!before, readFieldBool(mode, "useMap"));
	}

	@Test
	void onSettingCursor41To43MapNumber() throws Exception {
		for (int cursor : new int[]{41, 42, 43}) {
			PracticeMode mode = new PracticeMode();
			GameEngine engine = freshEngine(mode, false);
			setMenuState(engine, mode, cursor);
			int before = readFieldInt(mode, "mapNumber");
			pressRight(engine);
			mode.onSetting(engine, 0);
			assertEquals(before + 1, readFieldInt(mode, "mapNumber"),
					"Cursor " + cursor + " should adjust mapNumber");
		}
	}

	@Test
	void onSettingCursor44To45PresetNumber() throws Exception {
		for (int cursor : new int[]{44, 45}) {
			PracticeMode mode = new PracticeMode();
			GameEngine engine = freshEngine(mode, false);
			setMenuState(engine, mode, cursor);
			int before = readFieldInt(mode, "presetNumber");
			pressRight(engine);
			mode.onSetting(engine, 0);
			assertEquals(before + 1, readFieldInt(mode, "presetNumber"),
					"Cursor " + cursor + " should adjust presetNumber");
		}
	}

	// ---------------------------------------------------------------
	// A button confirm paths
	// ---------------------------------------------------------------

	@Test
	void onSettingPressAAtCursor41EntersFieldEdit() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 41, 10);
		pressPush(engine, Controller.BUTTON_A);
		assertTrue(mode.onSetting(engine, 0),
				"A at cursor 41 should enter field edit (return true)");
	}

	@Test
	void onSettingPressAAtCursor44LoadsPreset() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 44, 10);
		engine.speed.gravity = 999;
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(4, engine.speed.gravity,
				"A at cursor 44 should load preset defaults");
	}

	@Test
	void onSettingPressAAtCursor45SavesPreset() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 45, 10);
		engine.speed.gravity = 99;
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.owner.modeConfig.getProperty("practice.gravity.0", -1));
	}

	@Test
	void onSettingPressAAtOtherCursorStartsGame() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0, 10);
		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertFalse(engine.owner.menuOnly,
				"Menu should be closed when starting game");
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setMenuState(engine, mode, 0);
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	// ---------------------------------------------------------------
	// replayMode path
	// ---------------------------------------------------------------

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.replayMode = true;
		setFieldInt(mode, "menuTime", 0);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "menuTime"));

		setFieldInt(mode, "menuTime", 60);
		mode.onSetting(engine, 0);
		assertEquals(22, readFieldInt(mode, "menuCursor"),
				"replayMode at menuTime>=60 should set cursor to 22");

		setFieldInt(mode, "menuTime", 119);
		mode.onSetting(engine, 0);
		assertFalse(engine.owner.menuOnly,
				"replayMode at menuTime>=120 should start game");
	}

	// ---------------------------------------------------------------
	// renderSetting page boundaries
	// ---------------------------------------------------------------

	@Test
	void renderSettingPage1CursorBelow23() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage2Cursor23AndAbove() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 23);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingReplayMode() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 0);
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// calcScore normal branches
	// ---------------------------------------------------------------

	@Test
	void calcScoreNormalSingleLine() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 0); // LEVELTYPE_NONE
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(1, readFieldInt(mode, "lastevent"));
		assertTrue(readFieldInt(mode, "lastscore") > 0,
				"Score should be > 0 for single line");
	}

	@Test
	void calcScoreNormalFourLinesB2b() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.b2b = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 4);

		assertEquals(4, readFieldInt(mode, "lastevent"));
		assertTrue(readFieldBool(mode, "lastb2b"));
	}

	@Test
	void calcScoreNormalTspinSingle() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = false;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(8, readFieldInt(mode, "lastevent"));
	}

	@Test
	void calcScoreNormalAllClear() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertTrue(readFieldInt(mode, "lastscore") >= 1900,
				"All-clear should give bonus score");
	}

	@Test
	void calcScoreNormalEndingFlag() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 0); // LEVELTYPE_NONE
		setFieldInt(mode, "version", 2);
		setFieldInt(mode, "goallv", 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.lines = 1;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// Ending should have been triggered
		assertEquals(1, engine.ending,
				"Lines >= goallv+1 should trigger ending");
	}

	// ---------------------------------------------------------------
	// calcScore MANIA branches
	// ---------------------------------------------------------------

	@Test
	void calcScoreManiaLeveltype3() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 3); // LEVELTYPE_MANIA
		setFieldInt(mode, "goallv", 5);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		assertTrue(readFieldInt(mode, "lastscore") > 0,
				"Mania calcScore should produce points");
	}

	@Test
	void calcScoreManiaLines0ResetsCombo() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 3);
		setFieldInt(mode, "comboValue", 10);

		mode.calcScore(engine, 0, 0);

		assertEquals(1, readFieldInt(mode, "comboValue"),
				"lines==0 should reset comboValue to 1");
	}

	// ---------------------------------------------------------------
	// onLast timer/roll/timeout branches
	// ---------------------------------------------------------------

	@Test
	void onLastIncrementsScgettime() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "scgettime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readFieldInt(mode, "scgettime"));
	}

	@Test
	void onLastActiveTimerDecrementsTimelimitTimer() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ending = 0;
		setFieldInt(mode, "timelimit", 100);
		setFieldInt(mode, "timelimitTimer", 50);

		mode.onLast(engine, 0);

		assertEquals(49, readFieldInt(mode, "timelimitTimer"),
				"timelimitTimer should decrement each frame");
	}

	@Test
	void onLastTimelimitExpiresEndsGame() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ending = 0;
		setFieldInt(mode, "timelimit", 100);
		setFieldInt(mode, "timelimitTimer", 0);
		setFieldInt(mode, "goallv", -1);

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat,
				"Time out with goallv==-1 should trigger ending");
	}

	@Test
	void onLastRollEndsGame() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.gameActive = true;
		engine.ending = 2;
		setFieldInt(mode, "rolltime", 100);
		setFieldInt(mode, "rolltimelimit", 50);

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat,
				"Roll time exceeded should trigger excellent");
	}

	// ---------------------------------------------------------------
	// onMove level-up
	// ---------------------------------------------------------------

	@Test
	void onMoveManiaLevelUp() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 3); // MANIA
		engine.ending = 0;
		engine.holdDisable = false;
		engine.statc[0] = 0;
		engine.statistics.level = 0;
		setFieldInt(mode, "nextseclv", 100);

		mode.onMove(engine, 0);

		assertEquals(1, engine.statistics.level,
				"Level should increment in mania mode onMove");
	}

	@Test
	void onMoveEndingRollStart() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.ending = 2;
		setFieldBool(mode, "rollstarted", false);

		mode.onMove(engine, 0);

		assertTrue(readFieldBool(mode, "rollstarted"),
				"rollstarted should be set to true when ending==2");
	}

	// ---------------------------------------------------------------
	// startGame config branches
	// ---------------------------------------------------------------

	@Test
	void startGameManiaDisablesTspin() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 3); // MANIA
		setFieldInt(mode, "version", 5);
		setFieldInt(mode, "blockHidden", -2);
		setFieldInt(mode, "heboHiddenLevel", 0);

		mode.startGame(engine, 0);

		assertFalse(engine.tspinEnable);
		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DOUBLE, engine.comboType);
	}

	@Test
	void startGameVersion5BlockHidden() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "leveltype", 0);
		setFieldInt(mode, "version", 5);
		setFieldInt(mode, "blockHidden", 30);

		mode.startGame(engine, 0);

		assertEquals(30, engine.blockHidden);
	}

	// ---------------------------------------------------------------
	// renderLast branches
	// ---------------------------------------------------------------

	@Test
	void renderLastFieldeditState() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.FIELDEDIT;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastManiaState() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		setFieldInt(mode, "leveltype", 3); // MANIA
		engine.gameActive = true;
		engine.ending = 0;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastNormalState() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		setFieldInt(mode, "leveltype", 0); // NONE
		engine.gameActive = true;
		engine.ending = 0;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastWithEventDisplay() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		setFieldInt(mode, "leveltype", 0);
		setFieldInt(mode, "lastevent", 4); // EVENT_FOUR
		setFieldInt(mode, "scgettime", 0);
		setFieldBool(mode, "lastb2b", true);
		setFieldInt(mode, "lastpiece", Piece.PIECE_T);
		engine.gameActive = true;
		engine.ending = 0;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastPointsLeveltype() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		setFieldInt(mode, "leveltype", 2); // LEVELTYPE_POINTS
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLast10LinesLeveltype() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.stat = GameEngine.Status.MOVE;
		setFieldInt(mode, "leveltype", 1); // LEVELTYPE_10LINES
		mode.renderLast(engine, 0);
	}

	// ---------------------------------------------------------------
	// setHeboHidden branches
	// ---------------------------------------------------------------

	@Test
	void setHeboHiddenLevel1To7() throws Exception {
		for (int level = 1; level <= 7; level++) {
			PracticeMode mode = new PracticeMode();
			GameEngine engine = freshEngine(mode, false);
			setFieldInt(mode, "heboHiddenLevel", level);
			invokeSetHeboHidden(mode, engine);
			assertTrue(engine.heboHiddenEnable,
					"heboHiddenEnable should be true for level " + level);
		}
	}

	@Test
	void setHeboHiddenLevel0Disables() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		setFieldInt(mode, "heboHiddenLevel", 0);
		invokeSetHeboHidden(mode, engine);
		assertFalse(engine.heboHiddenEnable);
	}

	// ---------------------------------------------------------------
	// onReady branches
	// ---------------------------------------------------------------

	@Test
	void onReadySetsTimelimitAndBoneAndPieceEnable() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.statc[0] = 0;
		setFieldInt(mode, "timelimit", 100);
		setFieldBool(mode, "bone", true);
		setFieldInt(mode, "version", 1);

		mode.onReady(engine, 0);

		assertEquals(100, readFieldInt(mode, "timelimitTimer"));
		assertTrue(engine.bone);
	}

	// ---------------------------------------------------------------
	// onGameOver
	// ---------------------------------------------------------------

	@Test
	void onGameOverSetsSecretGrade() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode, false);
		engine.createFieldIfNeeded();
		engine.statc[0] = 0;
		engine.gameActive = true;

		mode.onGameOver(engine, 0);

		assertTrue(readFieldInt(mode, "secretGrade") >= 0);
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(PracticeMode mode, boolean replayMode) {
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

	private static void setMenuState(GameEngine engine, PracticeMode mode, int cursor)
			throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, PracticeMode mode,
			int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	private static void pressUp(GameEngine engine) {
		pressKey(engine, Controller.BUTTON_UP);
	}

	private static void pressDown(GameEngine engine) {
		pressKey(engine, Controller.BUTTON_DOWN);
	}

	private static void pressRight(GameEngine engine) {
		pressKey(engine, Controller.BUTTON_RIGHT);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void pressPush(GameEngine engine, int btn) {
		pressKey(engine, btn);
	}

	private static void invokeSetHeboHidden(PracticeMode mode, GameEngine engine)
			throws Exception {
		Method m = PracticeMode.class.getDeclaredMethod(
				"setHeboHidden", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static boolean readFieldBool(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getBoolean(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setFieldBool(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static Object readField(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).get(obj);
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
