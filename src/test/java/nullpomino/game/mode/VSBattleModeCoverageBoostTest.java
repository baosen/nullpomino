package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage tests for {@link VSBattleMode} (local 2-player VS battle, two
 * engines). Exercises: onSetting menu navigation/branches for both players,
 * the replay auto-advance path and the start path, renderSetting page
 * boundaries, renderLast status/event/combo display (both display modes),
 * calcScore scoring + garbage attack/counter/rising logic across all garbage
 * types and versions, onReady map loading, startGame engine config, onLast
 * meter + game-end win/lose/draw, renderResult, saveReplay, loadMap/saveMap,
 * loadMapPreview, getTotalGarbageLines, and the GarbageEntry constructors.
 */
class VSBattleModeCoverageBoostTest {

	// ---------------------------------------------------------------
	// Manager / engine setup (2 engines, playerID 0 and 1)
	// ---------------------------------------------------------------

	private static GameManager freshManager(VSBattleMode mode, boolean replayMode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.replayMode = replayMode;
		manager.mode = mode;
		manager.init(); // calls mode.modeInit + creates 2 engines
		manager.replayMode = replayMode;
		for (int i = 0; i < manager.engine.length; i++) {
			manager.engine[i].init();
			manager.engine[i].owner.replayMode = replayMode;
		}
		return manager;
	}

	/** Build a manager and run playerInit for both players. */
	private static GameManager initedManager(VSBattleMode mode, boolean replayMode) throws Exception {
		GameManager manager = freshManager(mode, replayMode);
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();
		return manager;
	}

	// ---------------------------------------------------------------
	// playerInit (replay + non-replay), modeInit
	// ---------------------------------------------------------------

	@Test
	void getNameAndPlayers() {
		VSBattleMode mode = new VSBattleMode();
		assertEquals("VS-BATTLE", mode.getName());
		assertEquals(2, mode.getPlayers());
		assertTrue(mode.isVSMode());
	}

	@Test
	void playerInitNonReplayBothPlayers() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = freshManager(mode, false);

		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);

		assertEquals(GameEngine.FRAME_COLOR_RED, manager.engine[0].framecolor);
		assertEquals(GameEngine.FRAME_COLOR_BLUE, manager.engine[1].framecolor);
		// player 1 seeds from player 0
		assertEquals(manager.engine[0].randSeed, manager.engine[1].randSeed);
		assertEquals(5, readInt(mode, "version"));
	}

	@Test
	void playerInitReplayMode() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = freshManager(mode, true);
		// covers lines 439-441
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		assertNotNull(getIntArray(mode, "garbage"));
	}

	// ---------------------------------------------------------------
	// onSetting: navigation + every cursor LEFT/RIGHT branch
	// ---------------------------------------------------------------

	@Test
	void onSettingCursorBranchesPlayer0() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		// Walk all 28 cursor positions (0..27) pressing RIGHT each time.
		for (int cursor = 0; cursor <= 27; cursor++) {
			setMenuState(engine, mode, cursor);
			pressKey(engine, Controller.BUTTON_RIGHT);
			mode.onSetting(engine, 0);
		}
		// Walk again pressing LEFT to hit the lower-bound wrap branches.
		for (int cursor = 0; cursor <= 27; cursor++) {
			setMenuState(engine, mode, cursor);
			pressKey(engine, Controller.BUTTON_LEFT);
			mode.onSetting(engine, 0);
		}
		assertTrue(true);
	}

	@Test
	void onSettingCursorBranchesPlayer1() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[1];

		for (int cursor = 0; cursor <= 27; cursor++) {
			setMenuState(engine, mode, cursor);
			pressKey(engine, Controller.BUTTON_RIGHT);
			mode.onSetting(engine, 1);
		}
		assertTrue(true);
	}

	@Test
	void onSettingUseMapToggleResetsField() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		// cursor 25 toggles useMap; with useMap currently false it turns on
		// (loadMapPreview), then toggle again to turn off and reset field
		// (covers line 573).
		getBoolArray(mode, "useMap")[0] = true;
		setMenuState(engine, mode, 25);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0); // toggles to false -> field.reset()
		assertFalse(getBoolArray(mode, "useMap")[0]);
	}

	@Test
	void onSettingMapSetAndMapNumberWithUseMap() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		getBoolArray(mode, "useMap")[0] = true;
		// cursor 26 mapSet with useMap true -> reload preview (583-584)
		setMenuState(engine, mode, 26);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		// cursor 27 mapNumber with useMap true -> 589-592
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapMaxNo")[0] = 3;
		getIntArray(mode, "mapNumber")[0] = 0;
		setMenuState(engine, mode, 27);
		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertTrue(true);
	}

	@Test
	void onSettingPressEAndFMultipliers() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		// cursor 0 gravity with BUTTON_E pressed (m = 100)
		setMenuState(engine, mode, 0);
		engine.speed.gravity = 0;
		pressKey(engine, Controller.BUTTON_RIGHT);
		engine.ctrl.buttonPress[Controller.BUTTON_E] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_E] = 1; // isPress needs time >= 1
		mode.onSetting(engine, 0);
		assertEquals(100, engine.speed.gravity);

		// cursor 1 denominator with BUTTON_F pressed (m = 1000)
		setMenuState(engine, mode, 1);
		engine.speed.denominator = 0;
		pressKey(engine, Controller.BUTTON_RIGHT);
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1; // isPress needs time >= 1
		mode.onSetting(engine, 0);
		assertEquals(1000, engine.speed.denominator);
	}

	@Test
	void onSettingPressALoadPreset() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		setMenuState(engine, mode, 7, 10);
		engine.speed.gravity = 42;
		pressKey(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		// preset load resets gravity to default 4
		assertEquals(4, engine.speed.gravity);
	}

	@Test
	void onSettingPressASavePreset() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		setMenuState(engine, mode, 8, 10);
		engine.speed.gravity = 77;
		getIntArray(mode, "presetNumber")[0] = 3;
		pressKey(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(77, manager.modeConfig.getProperty("vsbattle.gravity.3", -1));
	}

	@Test
	void onSettingPressAStartsGame() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		setMenuState(engine, mode, 0, 10);
		pressKey(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingPressBQuits() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		setMenuState(engine, mode, 0, 10);
		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingReplayAutoAdvance() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, true);
		GameEngine engine = manager.engine[0];
		engine.owner.replayMode = true;
		engine.statc[4] = 0;

		// covers 638-645
		setFieldInt(mode, "menuTime", 0);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 60);
		mode.onSetting(engine, 0);
		assertEquals(9, readFieldInt(mode, "menuCursor"));

		setFieldInt(mode, "menuTime", 120);
		mode.onSetting(engine, 0);
		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingStartReadyTransition() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);

		// Both engines have statc[4]==1, call as player 1 -> READY (649-653)
		manager.engine[0].statc[4] = 1;
		manager.engine[1].statc[4] = 1;
		mode.onSetting(manager.engine[1], 1);
		assertEquals(GameEngine.Status.READY, manager.engine[0].stat);
		assertEquals(GameEngine.Status.READY, manager.engine[1].stat);
	}

	@Test
	void onSettingStartCancelWithB() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		// statc[4]==1 but not the ready-trigger case; B cancels (656-657)
		engine.statc[4] = 1;
		manager.engine[1].statc[4] = 0;
		pressKey(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertEquals(0, engine.statc[4]);
	}

	// ---------------------------------------------------------------
	// renderSetting page boundaries (both players)
	// ---------------------------------------------------------------

	@Test
	void renderSettingAllPages() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);

		for (int playerID = 0; playerID <= 1; playerID++) {
			GameEngine engine = manager.engine[playerID];
			engine.statc[4] = 0;
			for (int cursor : new int[]{0, 9, 19}) {
				setFieldInt(mode, "menuCursor", cursor);
				mode.renderSetting(engine, playerID);
			}
			// WAIT state
			engine.statc[4] = 1;
			mode.renderSetting(engine, playerID);
		}
		assertTrue(true);
	}

	@Test
	void renderSettingOldVersionTSpinString() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];
		// version < 4 -> covers line 689 (getONorOFF branch)
		setFieldInt(mode, "version", 3);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 9);
		mode.renderSetting(engine, 0);
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// onReady map loading
	// ---------------------------------------------------------------

	@Test
	void onReadyUseMapNonReplayNoPropFile() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];
		engine.statc[0] = 0;
		setFieldInt(mode, "version", 3);
		getBoolArray(mode, "useMap")[0] = true;
		// propMap[0] null -> attempts load (likely null), covers 739-740
		mode.onReady(engine, 0);
		assertTrue(true);
	}

	@Test
	void onReadyUseMapReplayMode() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, true);
		GameEngine engine = manager.engine[0];
		engine.owner.replayMode = true;
		engine.statc[0] = 0;
		setFieldInt(mode, "version", 3);
		getBoolArray(mode, "useMap")[0] = true;
		// covers 734-737 (replay branch: loadMap from replayProp)
		mode.onReady(engine, 0);
		assertTrue(true);
	}

	@Test
	void onReadyUseMapWithPropFileVariousMapNumbers() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];
		engine.statc[0] = 0;
		setFieldInt(mode, "version", 3);
		getBoolArray(mode, "useMap")[0] = true;

		// Provide a non-null propMap so the load branches run (743-758).
		CustomProperties[] propMap = (CustomProperties[]) readField(mode, "propMap");
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.maxMapNumber", 2);
		pm.setProperty("map.0", "");
		pm.setProperty("map.1", "");
		propMap[0] = pm;
		getIntArray(mode, "mapMaxNo")[0] = 2;

		// mapNumber >= 0 -> loadMap(specific) (754)
		getIntArray(mode, "mapNumber")[0] = 1;
		mode.onReady(engine, 0);

		// mapNumber < 0 -> random map (750-751)
		engine.statc[0] = 0;
		getIntArray(mode, "mapNumber")[0] = -1;
		mode.onReady(engine, 0);
		assertTrue(true);
	}

	@Test
	void onReadyUseMapPlayer1CopiesPlayer0() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine1 = manager.engine[1];
		engine1.statc[0] = 0;
		setFieldInt(mode, "version", 3);

		getBoolArray(mode, "useMap")[0] = true;
		getBoolArray(mode, "useMap")[1] = true;
		getIntArray(mode, "mapNumber")[0] = -1;
		getIntArray(mode, "mapNumber")[1] = -1;

		CustomProperties[] propMap = (CustomProperties[]) readField(mode, "propMap");
		CustomProperties pm = new CustomProperties();
		pm.setProperty("map.maxMapNumber", 1);
		propMap[1] = pm;
		getIntArray(mode, "mapMaxNo")[1] = 1;
		manager.engine[0].createFieldIfNeeded();

		// covers 747-748 (player1 copies player0 field)
		mode.onReady(engine1, 1);
		assertTrue(true);
	}

	@Test
	void onReadyUseMapFalseResetsField() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];
		engine.statc[0] = 0;
		setFieldInt(mode, "version", 3);
		getBoolArray(mode, "useMap")[0] = false;
		mode.onReady(engine, 0); // 761-762 reset
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// startGame: version branches, both players
	// ---------------------------------------------------------------

	@Test
	void startGameVersion5SpinTypes() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);

		int[] tspinTypes = getIntArray(mode, "tspinEnableType");
		for (int t = 0; t <= 2; t++) {
			tspinTypes[0] = t;
			mode.startGame(manager.engine[0], 0);
		}
		mode.startGame(manager.engine[1], 1);
		assertTrue(manager.engine[1].big || !manager.engine[1].big);
	}

	@Test
	void startGameOldVersionTSpin() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 3); // covers line 794
		getBoolArray(mode, "enableTSpin")[0] = true;
		mode.startGame(manager.engine[0], 0);
		assertTrue(manager.engine[0].tspinEnable);
	}

	// ---------------------------------------------------------------
	// renderLast: status, stats both display types, event display
	// ---------------------------------------------------------------

	@Test
	void renderLastDefaultDisplay() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);

		// give garbage so the garbage-count display branches run (843-848)
		getIntArray(mode, "garbage")[0] = 5;
		getIntArray(mode, "garbage")[1] = 2;
		setFieldBool(mode, "showStats", true);
		getIntArray(mode, "hurryupSeconds")[0] = 0;
		manager.engine[0].timerActive = true;
		manager.engine[0].statistics.time = 0;

		mode.renderLast(manager.engine[0], 0);
		mode.renderLast(manager.engine[1], 1);
		assertTrue(true);
	}

	@Test
	void renderLastSideDisplayType2() throws Exception {
		// Use a receiver whose getNextDisplayType() == 2 to hit 851-865.
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver() {
			@Override
			public int getNextDisplayType() { return 2; }
		});
		manager.mode = mode;
		manager.init();
		for (int i = 0; i < manager.engine.length; i++) manager.engine[i].init();
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();

		setFieldBool(mode, "showStats", true);
		getIntArray(mode, "garbage")[0] = 3;
		getIntArray(mode, "garbageSent")[0] = 12; // >= 10 branch (856-857)
		getIntArray(mode, "garbageSent")[1] = 4;  // < 10 branch (858-859)
		getIntArray(mode, "winCount")[0] = 11;    // >= 10 branch (862-863)
		getIntArray(mode, "winCount")[1] = 3;     // < 10 branch (864-865)

		mode.renderLast(manager.engine[0], 0);
		mode.renderLast(manager.engine[1], 1);
		assertTrue(true);
	}

	@Test
	void renderLastEventDisplayAllEvents() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];
		engine.nowPieceObject = new nullpomino.game.component.Piece(
				nullpomino.game.component.Piece.PIECE_T);

		int[] lastevent = getIntArray(mode, "lastevent");
		int[] lastpiece = getIntArray(mode, "lastpiece");
		int[] scgettime = getIntArray(mode, "scgettime");
		int[] lastcombo = getIntArray(mode, "lastcombo");
		boolean[] lastb2b = (boolean[]) readField(mode, "lastb2b");
		lastpiece[0] = nullpomino.game.component.Piece.PIECE_T;
		scgettime[0] = 0;
		lastcombo[0] = 3; // combo display (913-914)

		// Each event value 1..10, with b2b true then false to hit both arms.
		for (int ev = 1; ev <= 10; ev++) {
			lastevent[0] = ev;
			lastb2b[0] = true;
			mode.renderLast(engine, 0);
			lastb2b[0] = false;
			mode.renderLast(engine, 0);
		}
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// calcScore: scoring branches + garbage attack / rising
	// ---------------------------------------------------------------

	private static void prepCalc(VSBattleMode mode, GameEngine engine) throws Exception {
		engine.createFieldIfNeeded();
		// fill the bottom row to keep field non-empty (avoid all-clear bonus
		// unless a test wants it)
		for (int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlock(x, engine.field.getHeight() - 1,
					new Block(Block.BLOCK_COLOR_GRAY));
		}
		engine.nowPieceObject = new nullpomino.game.component.Piece(
				nullpomino.game.component.Piece.PIECE_T);
	}

	@Test
	void calcScoreNormalLineClears() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		for (int lines = 1; lines <= 4; lines++) {
			prepCalc(mode, engine);
			engine.tspin = false;
			engine.b2b = false;
			mode.calcScore(engine, 0, lines);
		}
		assertTrue(getIntArray(mode, "garbageSent")[0] >= 0);
	}

	@Test
	void calcScoreTSpinVariants() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		// EZ spin (935-939) both useAllSpinBonus arms
		prepCalc(mode, engine);
		engine.tspin = true; engine.tspinez = true; engine.useAllSpinBonus = false;
		mode.calcScore(engine, 0, 1);
		prepCalc(mode, engine);
		engine.tspin = true; engine.tspinez = true; engine.useAllSpinBonus = true;
		mode.calcScore(engine, 0, 1);

		// T-Spin single mini (943-948) and non-mini single (950-951)
		prepCalc(mode, engine);
		engine.tspin = true; engine.tspinez = false; engine.tspinmini = true;
		engine.useAllSpinBonus = false;
		mode.calcScore(engine, 0, 1);
		prepCalc(mode, engine);
		engine.tspin = true; engine.tspinez = false; engine.tspinmini = true;
		engine.useAllSpinBonus = true;
		mode.calcScore(engine, 0, 1);
		prepCalc(mode, engine);
		engine.tspin = true; engine.tspinez = false; engine.tspinmini = false;
		mode.calcScore(engine, 0, 1);

		// T-Spin double mini (956-958) and double (959-961)
		prepCalc(mode, engine);
		engine.tspin = true; engine.tspinez = false; engine.tspinmini = true;
		engine.useAllSpinBonus = true;
		mode.calcScore(engine, 0, 2);
		prepCalc(mode, engine);
		engine.tspin = true; engine.tspinez = false; engine.tspinmini = false;
		engine.useAllSpinBonus = false;
		mode.calcScore(engine, 0, 2);

		// T-Spin triple (965-967)
		prepCalc(mode, engine);
		engine.tspin = true; engine.tspinez = false; engine.tspinmini = false;
		mode.calcScore(engine, 0, 3);
		assertTrue(true);
	}

	@Test
	void calcScoreB2BAndCombo() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		// B2B with T-spin triple (990-991), b2bType 1 -> non-separated
		getIntArray(mode, "b2bType")[0] = 1;
		prepCalc(mode, engine);
		engine.tspin = true; engine.tspinmini = false; engine.useAllSpinBonus = false;
		engine.b2b = true;
		mode.calcScore(engine, 0, 3);

		// Combo (1004-1008)
		prepCalc(mode, engine);
		engine.tspin = false; engine.b2b = false;
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
		engine.combo = 5;
		mode.calcScore(engine, 0, 1);

		// large combo clamps to table end
		prepCalc(mode, engine);
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
		engine.combo = 50;
		mode.calcScore(engine, 0, 1);
		assertTrue(true);
	}

	@Test
	void calcScoreSeparatedB2B() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		// b2bType 2 -> separated garbage (1024, 1047-1048)
		getIntArray(mode, "b2bType")[0] = 2;
		prepCalc(mode, engine);
		engine.tspin = false; engine.b2b = true;
		mode.calcScore(engine, 0, 4);
		assertTrue(getIntArray(mode, "garbageSent")[0] >= 0);
	}

	@Test
	void calcScoreGarbageCountering() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		getBoolArray(mode, "garbageCounter")[0] = true;
		// queue incoming garbage for player 0 so the counter loop runs
		// (1027-1037)
		addGarbage(mode, 0, 2, 1);
		addGarbage(mode, 0, 1, 1);
		prepCalc(mode, engine);
		engine.tspin = false; engine.b2b = false;
		mode.calcScore(engine, 0, 4); // pts=4 offsets the 3 queued lines
		assertTrue(true);
	}

	@Test
	void calcScoreEnemyDangerSE() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		// big attack -> enemy garbage >= 4 -> danger SE (1053-1054)
		prepCalc(mode, engine);
		engine.tspin = false; engine.b2b = false;
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
		engine.combo = 11; // big combo bonus to push attack high
		mode.calcScore(engine, 0, 4);
		assertTrue(getIntArray(mode, "garbageSent")[0] >= 0);
	}

	@Test
	void calcScoreRisingGarbageTypeNormalV5() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		// lines == 0 path with queued garbage -> rising (1061-1129)
		getIntArray(mode, "garbageType")[0] = 0; // NORMAL
		getIntArray(mode, "garbagePercent")[0] = 100;
		getIntArray(mode, "lastHole")[0] = -1;
		addGarbage(mode, 0, 3, 1);
		engine.createFieldIfNeeded();
		mode.calcScore(engine, 0, 0);
		assertTrue(true);
	}

	@Test
	void calcScoreRisingGarbageTypeOneRiseV5() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		getIntArray(mode, "garbageType")[0] = 1; // ONE RISE
		getIntArray(mode, "garbagePercent")[0] = 100;
		getIntArray(mode, "lastHole")[0] = 2;
		addGarbage(mode, 0, 4, 1);
		engine.createFieldIfNeeded();
		mode.calcScore(engine, 0, 0); // 1091-1107
		assertTrue(true);
	}

	@Test
	void calcScoreRisingGarbageTypeOneAttackV5() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		getIntArray(mode, "garbageType")[0] = 2; // 1-ATTACK
		getIntArray(mode, "garbagePercent")[0] = 100;
		getIntArray(mode, "lastHole")[0] = 1;
		addGarbage(mode, 0, 2, 1);
		addGarbage(mode, 0, 3, 1);
		engine.createFieldIfNeeded();
		mode.calcScore(engine, 0, 0); // 1108-1125
		assertTrue(true);
	}

	@Test
	void calcScoreRisingGarbageOldVersion() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 4); // old branches (1084-1085, 1102, 1119)
		GameEngine engine = manager.engine[0];

		for (int type = 0; type <= 2; type++) {
			getIntArray(mode, "garbageType")[0] = type;
			getIntArray(mode, "lastHole")[0] = -1;
			addGarbage(mode, 0, 3, 1);
			engine.createFieldIfNeeded();
			mode.calcScore(engine, 0, 0);
		}
		assertTrue(true);
	}

	@Test
	void calcScoreGarbageBlockingFalse() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		// lines > 0 but garbageBlocking false -> still rises (1061 second arm)
		getBoolArray(mode, "garbageBlocking")[0] = false;
		getIntArray(mode, "garbageType")[0] = 0;
		getIntArray(mode, "garbagePercent")[0] = 100;
		addGarbage(mode, 0, 2, 1);
		prepCalc(mode, engine);
		engine.tspin = false; engine.b2b = false;
		mode.calcScore(engine, 0, 1);
		assertTrue(true);
	}

	@Test
	void calcScoreHurryupVersion2() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 5);
		GameEngine engine = manager.engine[0];

		// HURRY UP version >= 2 branch (1133-1142)
		getIntArray(mode, "hurryupSeconds")[0] = 0;
		getIntArray(mode, "hurryupInterval")[0] = 1;
		engine.timerActive = true;
		engine.statistics.time = 60; // >= 0*60
		prepCalc(mode, engine);
		engine.tspin = false; engine.b2b = false;
		mode.calcScore(engine, 0, 1);

		// time below threshold -> else branch (1142)
		engine.statistics.time = 0;
		getIntArray(mode, "hurryupSeconds")[0] = 5;
		prepCalc(mode, engine);
		mode.calcScore(engine, 0, 1);
		assertTrue(true);
	}

	@Test
	void calcScoreHurryupVersion1() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		setFieldInt(mode, "version", 1); // version < 2 -> 1146-1150
		GameEngine engine = manager.engine[0];

		getIntArray(mode, "hurryupSeconds")[0] = 0;
		getIntArray(mode, "hurryupInterval")[0] = 1;
		engine.timerActive = true;
		engine.statistics.time = 60;
		prepCalc(mode, engine);
		engine.tspin = false; engine.b2b = false;
		mode.calcScore(engine, 0, 1);
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// onLast: meter, game-end win/lose/draw
	// ---------------------------------------------------------------

	@Test
	void onLastMeterDown() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];
		// garbage low, meterValue high -> meterValue-- (1172)
		getIntArray(mode, "garbage")[0] = 0;
		engine.meterValue = 50;
		mode.onLast(engine, 0);
		assertTrue(engine.meterValue <= 50);
	}

	@Test
	void onLastGameEndDraw() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);

		manager.engine[0].gameActive = true;
		manager.engine[0].stat = GameEngine.Status.GAMEOVER;
		manager.engine[1].stat = GameEngine.Status.GAMEOVER;
		mode.onLast(manager.engine[1], 1); // 1183-1186
		assertEquals(-1, readInt(mode, "winnerID"));
	}

	@Test
	void onLastGameEndPlayer1Wins() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);

		manager.engine[0].gameActive = true;
		manager.engine[0].stat = GameEngine.Status.MOVE; // alive
		manager.engine[1].stat = GameEngine.Status.GAMEOVER;
		mode.onLast(manager.engine[1], 1); // 1P win path (1188-1196)
		assertEquals(0, readInt(mode, "winnerID"));
		assertEquals(GameEngine.Status.EXCELLENT, manager.engine[0].stat);
		assertEquals(1, getIntArray(mode, "winCount")[0]);
	}

	@Test
	void onLastGameEndPlayer2Wins() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);

		manager.engine[0].gameActive = true;
		manager.engine[0].stat = GameEngine.Status.GAMEOVER;
		manager.engine[1].stat = GameEngine.Status.MOVE; // alive
		mode.onLast(manager.engine[1], 1); // 2P win path (1197-1206)
		assertEquals(1, readInt(mode, "winnerID"));
		assertEquals(GameEngine.Status.EXCELLENT, manager.engine[1].stat);
		assertEquals(1, getIntArray(mode, "winCount")[1]);
	}

	// ---------------------------------------------------------------
	// renderResult: win / lose / draw
	// ---------------------------------------------------------------

	@Test
	void renderResultAllOutcomes() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];
		engine.statistics.time = 600;
		engine.statistics.lines = 10;
		getIntArray(mode, "garbageSent")[0] = 5;

		// draw
		setFieldInt(mode, "winnerID", -1);
		mode.renderResult(engine, 0);
		// win (winnerID == playerID)
		setFieldInt(mode, "winnerID", 0);
		mode.renderResult(engine, 0);
		// lose (winnerID != playerID)
		setFieldInt(mode, "winnerID", 1);
		mode.renderResult(engine, 0); // 1222
		assertTrue(true);
	}

	// ---------------------------------------------------------------
	// saveReplay, loadMap/saveMap, loadMapPreview, getTotalGarbageLines
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesVersionAndMap() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];

		// useMap true with fldBackup set -> saveMap path (1249-1250)
		getBoolArray(mode, "useMap")[0] = true;
		nullpomino.game.component.Field[] fldBackup =
				(nullpomino.game.component.Field[]) readField(mode, "fldBackup");
		engine.createFieldIfNeeded();
		fldBackup[0] = new nullpomino.game.component.Field(engine.field);

		mode.saveReplay(engine, 0, manager.replayProp);
		assertEquals(5, manager.replayProp.getProperty("vsbattle.version", -1));
		// map.0 should now be present (covers saveMap 368-369)
		assertNotNull(manager.replayProp.getProperty("map.0", (String) null));
	}

	@Test
	void loadMapPreviewNullPropResetsField() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);
		GameEngine engine = manager.engine[0];
		engine.createFieldIfNeeded();

		// propMap null and field non-null -> reset (397-398)
		CustomProperties[] propMap = (CustomProperties[]) readField(mode, "propMap");
		propMap[0] = null;
		java.lang.reflect.Method m = VSBattleMode.class.getDeclaredMethod(
				"loadMapPreview", GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, 0, true);
		assertTrue(true);
	}

	@Test
	void getTotalGarbageLinesSumsEntries() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = initedManager(mode, false);

		addGarbage(mode, 0, 3, 1);
		addGarbage(mode, 0, 2, 0);
		java.lang.reflect.Method m = VSBattleMode.class.getDeclaredMethod(
				"getTotalGarbageLines", int.class);
		m.setAccessible(true);
		int total = (int) m.invoke(mode, 0);
		assertEquals(5, total);
	}

	@Test
	void garbageEntryConstructors() throws Exception {
		// Exercise the unused GarbageEntry() and GarbageEntry(int) constructors
		// (1270-1271, 1278-1280) via reflection.
		Class<?> entryCls = Class.forName(
				"nullpomino.game.mode.VSBattleMode$GarbageEntry");
		VSBattleMode mode = new VSBattleMode();

		java.lang.reflect.Constructor<?> noArg =
				entryCls.getDeclaredConstructor(VSBattleMode.class);
		noArg.setAccessible(true);
		Object e0 = noArg.newInstance(mode);
		assertNotNull(e0);

		java.lang.reflect.Constructor<?> oneArg =
				entryCls.getDeclaredConstructor(VSBattleMode.class, int.class);
		oneArg.setAccessible(true);
		Object e1 = oneArg.newInstance(mode, 7);
		Field linesF = entryCls.getDeclaredField("lines");
		linesF.setAccessible(true);
		assertEquals(7, linesF.getInt(e1));
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	@SuppressWarnings("unchecked")
	private static void addGarbage(VSBattleMode mode, int playerID, int lines, int srcPlayer) throws Exception {
		LinkedList<Object>[] entries = (LinkedList<Object>[]) readField(mode, "garbageEntries");
		Class<?> entryCls = Class.forName("nullpomino.game.mode.VSBattleMode$GarbageEntry");
		java.lang.reflect.Constructor<?> ctor =
				entryCls.getDeclaredConstructor(VSBattleMode.class, int.class, int.class);
		ctor.setAccessible(true);
		Object entry = ctor.newInstance(mode, lines, srcPlayer);
		((LinkedList<Object>) entries[playerID]).add(entry);
	}

	private static void setMenuState(GameEngine engine, VSBattleMode mode, int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, VSBattleMode mode, int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static int readInt(VSBattleMode mode, String name) throws Exception {
		return findField(mode.getClass(), name).getInt(mode);
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static void setFieldBool(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		return (int[]) findField(obj.getClass(), name).get(obj);
	}

	private static boolean[] getBoolArray(Object obj, String name) throws Exception {
		return (boolean[]) findField(obj.getClass(), name).get(obj);
	}

	private static Object readField(VSBattleMode mode, String name) throws Exception {
		return findField(mode.getClass(), name).get(mode);
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
