package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage-boost tests for {@link ComboRaceMode}. Exercises the previously
 * uncovered paths: the onSetting menu block (every cursor 0-15 with
 * LEFT/RIGHT plus A/B confirm/cancel, load/save/start presets, net ranking
 * display branch), renderSetting both pages plus net-ranking page,
 * renderLast in-game with every lastevent/combo branch, calcScore deeper
 * branches (tspin mini, double/triple, endless meter, fade switches,
 * remaining-stack drop), startGame version/watch branches, fillStack
 * else-branch, onReady spectator field send, renderResult net flags,
 * saveReplay name + ranking save, the net send/recv stat/option pipeline,
 * the replay playerInit path, netGetGoalType and netIsNetRankingViewOK.
 */
class ComboRaceModeCoverageBoostTest {

	// -----------------------------------------------------------------------
	// playerInit replay path (219-224)
	// -----------------------------------------------------------------------

	@Test
	void playerInitReplayPathLoadsVersionAndPreset() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		engine.owner.replayMode = true;
		engine.owner.replayProp.setProperty("comborace.version", 0);
		engine.owner.replayProp.setProperty("0.net.netPlayerName", "REPLAYER");

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "version"));
		assertEquals(0, readInt(mode, "presetNumber"));
	}

	// -----------------------------------------------------------------------
	// onSetting: each cursor LEFT/RIGHT (298-389 menu block)
	// -----------------------------------------------------------------------

	@Test
	void onSettingCursor0Goaltype() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		int before = readInt(mode, "goaltype");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertNotEquals(before, readInt(mode, "goaltype"));
	}

	@Test
	void onSettingCursor0GoaltypeWrapsBelowZero() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		setInt(mode, "goaltype", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(3, readInt(mode, "goaltype"), "goaltype wraps to GOAL_TABLE.length-1");
	}

	@Test
	void onSettingCursor1Shapetype() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 1);
		setInt(mode, "shapetype", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(8, readInt(mode, "shapetype"), "shapetype wraps to SHAPETYPE_MAX-1");
	}

	@Test
	void onSettingCursor1ShapetypeWrapsAtTop() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 1);
		setInt(mode, "shapetype", 8);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "shapetype"));
	}

	@Test
	void onSettingCursor2ComboColumnWrapsAndShrinksWidth() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 2);
		setInt(mode, "comboColumn", 1);
		setInt(mode, "comboWidth", 4);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		// comboColumn wraps from 1 to 10, then the while-loop shrinks comboWidth
		assertEquals(10, readInt(mode, "comboColumn"));
		assertEquals(1, readInt(mode, "comboWidth"));
	}

	@Test
	void onSettingCursor2ComboColumnWrapsAtTop() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 2);
		setInt(mode, "comboColumn", 10);
		setInt(mode, "comboWidth", 1);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(1, readInt(mode, "comboColumn"));
	}

	@Test
	void onSettingCursor3ComboWidthWrapsAndShrinksColumn() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 3);
		setInt(mode, "comboColumn", 10);
		setInt(mode, "comboWidth", 10);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		// comboWidth wraps to 1, then column shrink loop runs
		assertEquals(1, readInt(mode, "comboWidth"));
	}

	@Test
	void onSettingCursor3ComboWidthWrapsBelowOne() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 3);
		setInt(mode, "comboColumn", 1);
		setInt(mode, "comboWidth", 1);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(10, readInt(mode, "comboWidth"));
	}

	@Test
	void onSettingCursor4CeilingAdjustWraps() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 4);
		setInt(mode, "ceilingAdjust", 10);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(-10, readInt(mode, "ceilingAdjust"));

		setInt(mode, "ceilingAdjust", -10);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(10, readInt(mode, "ceilingAdjust"));
	}

	@Test
	void onSettingCursor5SpawnAboveFieldToggles() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 5);
		boolean before = readBoolean(mode, "spawnAboveField");
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!before, readBoolean(mode, "spawnAboveField"));
	}

	@Test
	void onSettingCursor6GravityWithMultiplier() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 6);
		engine.speed.gravity = 0;
		pressKey(engine, Controller.BUTTON_RIGHT);
		// E button -> multiplier 100
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 5;
		mode.onSetting(engine, 0);
		assertEquals(100, engine.speed.gravity);
	}

	@Test
	void onSettingCursor6GravityWrapHigh() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 6);
		engine.speed.gravity = 99999;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(-1, engine.speed.gravity);
	}

	@Test
	void onSettingCursor6GravityWrapLow() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 6);
		engine.speed.gravity = -1;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99999, engine.speed.gravity);
	}

	@Test
	void onSettingCursor7DenominatorWithBigMultiplier() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 7);
		engine.speed.denominator = 0;
		pressKey(engine, Controller.BUTTON_RIGHT);
		// F button -> multiplier 1000
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 5;
		mode.onSetting(engine, 0);
		assertEquals(1000, engine.speed.denominator);
	}

	@Test
	void onSettingCursor7DenominatorWrap() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 7);
		engine.speed.denominator = 99999;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(-1, engine.speed.denominator);

		engine.speed.denominator = -1;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99999, engine.speed.denominator);
	}

	@Test
	void onSettingCursor8Are() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 8);
		engine.speed.are = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.are);

		engine.speed.are = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.are);
	}

	@Test
	void onSettingCursor9AreLine() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 9);
		engine.speed.areLine = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.areLine);

		engine.speed.areLine = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.areLine);
	}

	@Test
	void onSettingCursor10LineDelay() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 10);
		engine.speed.lineDelay = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.lineDelay);

		engine.speed.lineDelay = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.lineDelay);
	}

	@Test
	void onSettingCursor11LockDelay() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 11);
		engine.speed.lockDelay = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.lockDelay);

		engine.speed.lockDelay = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.lockDelay);
	}

	@Test
	void onSettingCursor12Das() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 12);
		engine.speed.das = 0;
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, engine.speed.das);

		engine.speed.das = 99;
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.speed.das);
	}

	@Test
	void onSettingCursor13Bgmno() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 13);
		setInt(mode, "bgmno", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertTrue(readInt(mode, "bgmno") > 0, "bgmno wraps to BGM_COUNT-1");
	}

	@Test
	void onSettingCursor14PresetNumberWraps() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 14);
		setInt(mode, "presetNumber", 0);
		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(99, readInt(mode, "presetNumber"));

		setInt(mode, "presetNumber", 99);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, readInt(mode, "presetNumber"));
	}

	@Test
	void onSettingCursor15PresetNumber() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 15);
		setInt(mode, "presetNumber", 5);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(6, readInt(mode, "presetNumber"));
	}

	// -----------------------------------------------------------------------
	// onSetting: A confirm / B cancel (379-409)
	// -----------------------------------------------------------------------

	@Test
	void onSettingPressAAtCursor14LoadsPreset() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 14);
		setInt(mode, "menuTime", 10);
		setInt(mode, "presetNumber", 0);
		// Seed a known preset value so the load is deterministic regardless of
		// any persisted modeConfig state.
		engine.owner.modeConfig.setProperty("comborace.gravity.0", 555);
		engine.speed.gravity = 12345;

		pressKey(engine, Controller.BUTTON_A);
		boolean cont = mode.onSetting(engine, 0);

		assertTrue(cont, "Load preset stays in setting screen");
		assertEquals(555, engine.speed.gravity, "Loaded gravity from preset 0");
	}

	@Test
	void onSettingPressAAtCursor15SavesPreset() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 15);
		setInt(mode, "menuTime", 10);
		setInt(mode, "presetNumber", 0);
		engine.speed.gravity = 77;

		pressKey(engine, Controller.BUTTON_A);
		boolean cont = mode.onSetting(engine, 0);

		assertTrue(cont);
		assertEquals(77, engine.owner.modeConfig.getProperty("comborace.gravity.0", -1));
	}

	@Test
	void onSettingPressAAtOtherCursorStartsGame() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		setInt(mode, "menuTime", 10);
		setInt(mode, "presetNumber", 3);

		pressKey(engine, Controller.BUTTON_A);
		boolean cont = mode.onSetting(engine, 0);

		assertFalse(cont, "Confirm at a normal cursor starts the game");
		assertEquals(3, engine.owner.modeConfig.getProperty("comborace.presetNumber", -1));
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		setInt(mode, "menuTime", 10);

		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingNetRankingDisplayModeBranch() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = settingEngine(mode, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);
		setObject(mode, "netRankingNoDataFlag", new boolean[]{true, true});
		setInt(mode, "netRankingView", 0);

		// No buttons pressed: returns through the net-ranking update branch.
		boolean cont = mode.onSetting(engine, 0);
		assertTrue(cont);
	}

	@Test
	void onSettingReplayModeAutoAdvances() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		setInt(mode, "menuTime", 0);

		boolean cont = mode.onSetting(engine, 0);
		assertTrue(cont);
		assertEquals(-1, readMenuCursor(mode));

		setInt(mode, "menuTime", 59);
		boolean cont2 = mode.onSetting(engine, 0);
		assertFalse(cont2, "After 60 frames replay setting ends");
	}

	// -----------------------------------------------------------------------
	// renderSetting (434, 448-459)
	// -----------------------------------------------------------------------

	@Test
	void renderSettingFirstPage() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenuCursor(mode, 0);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingFirstPageEndlessGoal() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenuCursor(mode, 0);
		setInt(mode, "goaltype", 3); // ENDLESS -> "ENDLESS" label
		setInt(mode, "comboWidth", 5); // non-4 width color branch
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingSecondPage() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setMenuCursor(mode, 6);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingNetRankingPage() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetRankingDisplayMode", true);
		setObject(mode, "netRankingNoDataFlag", new boolean[]{true, true});
		setInt(mode, "netRankingView", 0);
		mode.renderSetting(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onReady spectator field send (478)
	// -----------------------------------------------------------------------

	@Test
	void onReadyWithSpectatorsSendsField() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setInt(mode, "netNumSpectators", 1);
		attachNetLobby(mode);

		boolean result = mode.onReady(engine, 0);
		assertFalse(result);
	}

	@Test
	void onReadyEndlessSetsZeroMeter() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[0] = 0;
		setInt(mode, "goaltype", 3); // ENDLESS

		mode.onReady(engine, 0);
		assertEquals(0, engine.meterValue);
	}

	// -----------------------------------------------------------------------
	// startGame version / watch branches (491, 494)
	// -----------------------------------------------------------------------

	@Test
	void startGameLegacyVersionSetsBig() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 0); // <= 0 -> engine.big = big
		setBoolean(mode, "big", true);

		mode.startGame(engine, 0);
		assertTrue(engine.big);
	}

	@Test
	void startGameWatchMutesBgm() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsWatch", true);

		mode.startGame(engine, 0);
		assertEquals(nullpomino.game.component.BGMStatus.BGM_NOTHING,
				engine.owner.bgmStatus.bgm);
	}

	// -----------------------------------------------------------------------
	// fillStack else-branch (522-523): goal fits under ceiling
	// -----------------------------------------------------------------------

	@Test
	void fillStackGoalShorterThanCeilingSetsZeroRemain() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		// goaltype 0 -> goal 20. With a large positive ceilingAdjust the goal
		// becomes <= h + ceilingAdjust, hitting the else branch.
		setInt(mode, "ceilingAdjust", 10);
		invokeFillStack(mode, engine, 0);

		assertEquals(0, readInt(mode, "remainStack"));
	}

	// -----------------------------------------------------------------------
	// renderLast in-game (557, 558, 573-632)
	// -----------------------------------------------------------------------

	@Test
	void renderLastSettingStateEndlessRanking() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 3); // ENDLESS -> "(ENDLESS GAME)" line 558
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastInGameWithAllEvents() throws Exception {
		int[] events = {1, 2, 3, 4, 5, 6, 7, 8, 9};
		for (int ev : events) {
			for (boolean b2b : new boolean[]{false, true}) {
				ComboRaceMode mode = new ComboRaceMode();
				GameEngine engine = freshEngine(mode);
				mode.playerInit(engine, 0);
				engine.nowPieceObject = new Piece(Piece.PIECE_T);
				engine.stat = GameEngine.Status.MOVE;
				setInt(mode, "goaltype", 0); // finite -> "(20 LINES GAME)"
				setInt(mode, "lastevent", ev);
				setInt(mode, "scgettime", 0);
				setBoolean(mode, "lastb2b", b2b);
				setInt(mode, "lastcombo", 5); // >= 2 -> combo line 628
				mode.renderLast(engine, 0);
			}
		}
	}

	@Test
	void renderLastInGameActiveComboBranch() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastevent", 0); // EVENT_NONE -> skip event switch
		engine.combo = 3;
		engine.gameActive = true;
		setInt(mode, "lastcombo", 4);
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastInGameMaxComboBranch() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastevent", 0);
		engine.combo = 0;
		engine.gameActive = false;
		engine.statistics.maxCombo = 5; // >= 2 -> max combo line 632
		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastMenuOnlyReturnsEarly() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = true;
		mode.renderLast(engine, 0); // returns immediately
	}

	// -----------------------------------------------------------------------
	// calcScore deeper branches (661, 669, 682, 684, 705-723, 736-741)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreTSpinSingleMini() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;
		engine.tspinmini = true; // -> EVENT_TSPIN_SINGLE_MINI (661)

		mode.calcScore(engine, 0, 1);
		assertEquals(5, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreTSpinDoubleMini() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = true; // -> EVENT_TSPIN_DOUBLE_MINI (669)

		mode.calcScore(engine, 0, 2);
		assertEquals(9, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreDoubleAndTriple() throws Exception {
		ComboRaceMode m1 = new ComboRaceMode();
		GameEngine e1 = freshEngine(m1);
		m1.playerInit(e1, 0);
		e1.nowPieceObject = new Piece(Piece.PIECE_T);
		e1.createFieldIfNeeded();
		m1.calcScore(e1, 0, 2); // EVENT_DOUBLE (682)
		assertEquals(2, readInt(m1, "lastevent"));

		ComboRaceMode m2 = new ComboRaceMode();
		GameEngine e2 = freshEngine(m2);
		m2.playerInit(e2, 0);
		e2.nowPieceObject = new Piece(Piece.PIECE_T);
		e2.createFieldIfNeeded();
		m2.calcScore(e2, 0, 3); // EVENT_TRIPLE (684)
		assertEquals(3, readInt(m2, "lastevent"));
	}

	@Test
	void calcScoreEndlessRemainStackAndMeter() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 3); // ENDLESS -> remainStack=MAX_VALUE (705), endless meter (716-722)
		setInt(mode, "ceilingAdjust", -2);
		engine.statistics.maxCombo = 25; // drives meter color index
		// Enable the meter so getMeterMax() is non-zero (avoids /0 in 716-722).
		enableMeter(engine);

		mode.calcScore(engine, 0, 2); // 2 lines -> runs remainStack loop
		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void calcScoreFiniteGoalFadeSwitches() throws Exception {
		// lines just under goal-5 boundary -> bgm fade switch (736)
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // goal 20
		engine.statistics.lines = 16; // >= goal-5 (15) but < goal

		mode.calcScore(engine, 0, 1);
		assertTrue(engine.owner.bgmStatus.fadesw);
	}

	@Test
	void calcScoreFiniteGoalSectionFade() throws Exception {
		// lines crossing nextseclines (10) -> background fade switch (737-741)
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 2); // goal 100, so goal-5 not yet reached
		setInt(mode, "nextseclines", 10);
		engine.statistics.lines = 12; // >= nextseclines

		mode.calcScore(engine, 0, 1);
		assertTrue(engine.owner.backgroundStatus.fadesw);
		assertEquals(20, readInt(mode, "nextseclines"));
	}

	@Test
	void calcScoreFiniteGoalMeterColors() throws Exception {
		// remainLines low -> red meter color branch (727-729)
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 1); // goal 40
		engine.statistics.lines = 35; // remainLines = 5 -> red

		mode.calcScore(engine, 0, 1);
		assertTrue(engine.meterValue >= 0);
	}

	// -----------------------------------------------------------------------
	// renderResult net flags (776, 780, 782)
	// -----------------------------------------------------------------------

	@Test
	void renderResultWithPBFlag() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsPB", true);
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultSendingStatus() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetPlay", true);
		setInt(mode, "netReplaySendStatus", 1); // SENDING...
		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultRetryStatus() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsNetPlay", true);
		setBoolean(mode, "netIsWatch", false);
		setInt(mode, "netReplaySendStatus", 2); // A: RETRY
		mode.renderResult(engine, 0);
	}

	// -----------------------------------------------------------------------
	// saveReplay name + ranking save (795-806)
	// -----------------------------------------------------------------------

	@Test
	void saveReplaySavesNameAndRanking() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setObject(mode, "netPlayerName", "TESTER");
		setBoolean(mode, "big", false);
		engine.statistics.maxCombo = 30;
		engine.statistics.time = 1234;
		engine.ending = 1; // -> time recorded

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals("TESTER", prop.getProperty("0.net.netPlayerName", ""));
		assertEquals(1, engine.owner.replayProp.getProperty("comborace.version", -1));
		assertTrue(readInt(mode, "rankingRank") >= 0, "Ranking entry should be inserted");
	}

	// -----------------------------------------------------------------------
	// net send/recv pipeline (868-957)
	// -----------------------------------------------------------------------

	@Test
	void netSendStatsBuildsMessage() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		invokeNet(mode, "netSendStats", engine);
	}

	@Test
	void netSendStatsWithBackgroundFade() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		engine.owner.backgroundStatus.fadesw = true;
		engine.owner.backgroundStatus.fadebg = 2;
		invokeNet(mode, "netSendStats", engine);
	}

	@Test
	void netRecvStatsParsesMessage() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		String[] msg = new String[22];
		for (int i = 0; i < msg.length; i++) msg[i] = "0";
		msg[7] = "1.5"; msg[8] = "2.5";           // lpm, pps (float)
		msg[10] = "true"; msg[11] = "true";        // gameActive, timerActive
		msg[17] = "false";                          // lastb2b
		msg[9] = "2";                               // goaltype

		invokeNetRecv(mode, "netRecvStats", engine, msg);
		assertEquals(2, readInt(mode, "goaltype"));
	}

	@Test
	void netSendEndGameStatsBuildsMessage() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		engine.statistics.maxCombo = 10;
		invokeNet(mode, "netSendEndGameStats", engine);
	}

	@Test
	void netSendOptionsBuildsMessage() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		attachNetLobby(mode);
		invokeNet(mode, "netSendOptions", engine);
	}

	@Test
	void netRecvOptionsParsesMessage() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		String[] msg = new String[19];
		for (int i = 0; i < msg.length; i++) msg[i] = "0";
		msg[4] = "5";   // gravity
		msg[12] = "2";  // goaltype
		msg[18] = "true"; // spawnAboveField

		invokeNetRecv(mode, "netRecvOptions", engine, msg);
		assertEquals(5, engine.speed.gravity);
		assertEquals(2, readInt(mode, "goaltype"));
		assertTrue(readBoolean(mode, "spawnAboveField"));
	}

	// -----------------------------------------------------------------------
	// netGetGoalType (964) / netIsNetRankingViewOK (972)
	// -----------------------------------------------------------------------

	@Test
	void netGetGoalTypeReturnsGoaltype() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 2);

		Method m = ComboRaceMode.class.getDeclaredMethod("netGetGoalType");
		m.setAccessible(true);
		assertEquals(2, (int) m.invoke(mode));
	}

	@Test
	void netIsNetRankingViewOKReflectsBigAndAi() throws Exception {
		ComboRaceMode mode = new ComboRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", false);
		engine.ai = null;

		Method m = ComboRaceMode.class.getDeclaredMethod(
				"netIsNetRankingViewOK", GameEngine.class);
		m.setAccessible(true);
		assertTrue((boolean) m.invoke(mode, engine));

		setBoolean(mode, "big", true);
		assertFalse((boolean) m.invoke(mode, engine));
	}

	// =======================================================================
	// Helpers
	// =======================================================================

	private static GameEngine freshEngine(ComboRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	/** Engine wired for the live (non-replay) onSetting menu at a given cursor. */
	private static GameEngine settingEngine(ComboRaceMode mode, int cursor) throws Exception {
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setMenuCursor(mode, cursor);
		setInt(mode, "menuTime", 10);
		return engine;
	}

	/** Turns on the HUD meter so EventReceiver.getMeterMax returns non-zero. */
	private static void enableMeter(GameEngine engine) throws Exception {
		Field f = findField(engine.owner.receiver.getClass(), "showmeter");
		f.setAccessible(true);
		f.setBoolean(engine.owner.receiver, true);
	}

	private static void attachNetLobby(ComboRaceMode mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setObject(mode, "netLobby", lobby);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static int readMenuCursor(ComboRaceMode mode) throws Exception {
		Field f = findField(mode.getClass(), "menuCursor");
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static void setMenuCursor(ComboRaceMode mode, int value) throws Exception {
		Field f = findField(mode.getClass(), "menuCursor");
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static int readInt(ComboRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ComboRaceMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static void setInt(ComboRaceMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ComboRaceMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static void setObject(ComboRaceMode mode, String name, Object value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.set(mode, value);
	}

	private static void invokeFillStack(ComboRaceMode mode, GameEngine engine, int height) throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod("fillStack", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, height);
	}

	private static void invokeNet(ComboRaceMode mode, String method, GameEngine engine) throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod(method, GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeNetRecv(ComboRaceMode mode, String method, GameEngine engine, String[] msg)
			throws Exception {
		Method m = ComboRaceMode.class.getDeclaredMethod(method, GameEngine.class, String[].class);
		m.setAccessible(true);
		m.invoke(mode, engine, (Object) msg);
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
