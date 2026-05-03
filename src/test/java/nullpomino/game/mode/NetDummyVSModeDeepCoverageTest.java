package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedList;
import java.util.Random;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.Statistics;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Deep coverage for untested branches in {@link NetDummyVSMode}.
 * Covers VS-specific lifecycle hooks, game screen layout, locked rules,
 * room settings, practice mode, auto-start timer, game-over branches,
 * rendering, and message handling.
 */
class NetDummyVSModeDeepCoverageTest {

	private NetDummyVSMode mode;
	private GameManager manager;
	private GameEngine engine;
	private EventReceiver receiver;

	@BeforeEach
	void setUp() throws Exception {
		mode = new NetDummyVSMode();
		receiver = new EventReceiver();
		manager = new GameManager(receiver);
		manager.mode = mode;
		mode.modeInit(manager);
		manager.init();
		manager.engine[0].init();
		engine = manager.engine[0];
		engine.ruleopt.fieldWidth = 10;
		engine.ruleopt.fieldHeight = 20;
		engine.ruleopt.fieldHiddenHeight = 4;
		engine.createFieldIfNeeded();
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 2;
	}

	// ================================================================
	//  Helpers
	// ================================================================

	void setupMinimalNetLobby() throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		NetPlayerClient client = new NetPlayerClient();
		NetPlayerInfo myInfo = new NetPlayerInfo();
		myInfo.seatID = 0;
		myInfo.uid = 1;
		client.getPlayerInfoList().add(myInfo);
		setField(client, "playerUID", 1);
		setField(lobby, "netPlayerClient", client);
		setField(mode, "netLobby", lobby);
	}

	void setupNetCurrentRoomInfo() throws Exception {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.roomID = 1;
		roomInfo.ruleName = "test_rule";
		roomInfo.ruleLock = false;
		roomInfo.rated = false;
		roomInfo.maxPlayers = 6;
		roomInfo.gravity = 1;
		roomInfo.denominator = 256;
		roomInfo.are = 25;
		roomInfo.areLine = 25;
		roomInfo.lineDelay = 0;
		roomInfo.lockDelay = 30;
		roomInfo.das = 10;
		roomInfo.b2b = false;
		roomInfo.combo = false;
		roomInfo.tspinEnableType = 0;
		setField(mode, "netCurrentRoomInfo", roomInfo);
	}

	void ensureEngineHasNextPieces() {
		int n = Math.max(1, engine.ruleopt.nextDisplay);
		engine.nextPieceArrayObject = new Piece[n];
		for (int i = 0; i < n; i++) {
			engine.nextPieceArrayObject[i] = new Piece(Piece.PIECE_T);
		}
		engine.nextPieceCount = 0;
	}

	private void initializeAllEngines() throws Exception {
		for (int i = 0; i < 6; i++) {
			if (manager.engine[i] == null) {
				manager.engine[i] = new GameEngine(manager, i);
			}
			manager.engine[i].init();
			manager.engine[i].ruleopt.fieldWidth = 10;
			manager.engine[i].ruleopt.fieldHeight = 20;
			manager.engine[i].ruleopt.fieldHiddenHeight = 4;
			manager.engine[i].createFieldIfNeeded();
			manager.engine[i].stat = GameEngine.Status.SETTING;
			manager.engine[i].statistics = new Statistics();
		}
	}

	// ================================================================
	//  netvsIsAttackable
	// ================================================================

	@Test
	void netvsIsAttackableSelfReturnsFalse() {
		assertFalse(mode.netvsIsAttackable(0));
	}

	@Test
	void netvsIsAttackableNonExistentReturnsFalse() throws Exception {
		setField(mode, "netvsPlayerExist", new boolean[] { true, false });
		assertFalse(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableDeadReturnsFalse() throws Exception {
		setField(mode, "netvsPlayerExist", new boolean[] { true, true });
		setField(mode, "netvsPlayerDead", new boolean[] { false, true });
		assertFalse(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableNewcomerReturnsFalse() throws Exception {
		setField(mode, "netvsPlayerExist", new boolean[] { true, true });
		setField(mode, "netvsPlayerDead", new boolean[] { false, false });
		setField(mode, "netvsPlayerActive", new boolean[] { true, false });
		assertFalse(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableTeammateReturnsFalse() throws Exception {
		setField(mode, "netvsPlayerExist", new boolean[] { true, true });
		setField(mode, "netvsPlayerDead", new boolean[] { false, false });
		setField(mode, "netvsPlayerActive", new boolean[] { true, true });
		setField(mode, "netvsPlayerTeam", new String[] { "TeamA", "TeamA" });
		assertFalse(mode.netvsIsAttackable(1));
	}

	@Test
	void netvsIsAttackableValidReturnsTrue() throws Exception {
		setField(mode, "netvsPlayerExist", new boolean[] { true, true });
		setField(mode, "netvsPlayerDead", new boolean[] { false, false });
		setField(mode, "netvsPlayerActive", new boolean[] { true, true });
		setField(mode, "netvsPlayerTeam", new String[] { "", "" });
		assertTrue(mode.netvsIsAttackable(1));
	}

	// ================================================================
	//  netvsGetNumberOfTeamsAlive
	// ================================================================

	@Test
	void netvsGetNumberOfTeamsAliveNoTeams() throws Exception {
		setField(mode, "netvsPlayerExist", new boolean[] { true, true, false, false, false, false });
		setField(mode, "netvsPlayerDead", new boolean[] { false, false, false, false, false, false });
		setField(mode, "netvsPlayerTeam", new String[] { "", "", "", "", "", "" });
		manager.engine[0].gameActive = true;
		manager.engine[1] = new GameEngine(manager, 1);
		manager.engine[1].init();
		manager.engine[1].gameActive = true;
		assertEquals(2, mode.netvsGetNumberOfTeamsAlive());
	}

	@Test
	void netvsGetNumberOfTeamsAliveWithTeams() throws Exception {
		setField(mode, "netvsPlayerExist", new boolean[] { true, true, true, false, false, false });
		setField(mode, "netvsPlayerDead", new boolean[] { false, false, false, false, false, false });
		setField(mode, "netvsPlayerTeam", new String[] { "A", "B", "A", "", "", "" });
		manager.engine[0].gameActive = true;
		manager.engine[1] = new GameEngine(manager, 1);
		manager.engine[1].init();
		manager.engine[1].gameActive = true;
		manager.engine[2] = new GameEngine(manager, 2);
		manager.engine[2].init();
		manager.engine[2].gameActive = true;
		assertEquals(2, mode.netvsGetNumberOfTeamsAlive()); // Team A + Team B = 2 (A has 2 members, B has 1)
	}

	// ================================================================
	//  netvsSetLockedRule
	// ================================================================

	@Test
	void netvsSetLockedRuleNoRoomInfo() throws Exception {
		// When roomInfo is null and netLobby has ruleOptPlayer, it should work
		setupMinimalNetLobby();
		setupRuleOptPlayer();
		mode.netvsSetLockedRule();
	}

	private void setupRuleOptPlayer() throws Exception {
		NetLobbyFrame lobby = (NetLobbyFrame) getField(mode, "netLobby");
		nullpomino.game.component.RuleOptions ruleOptPlayer = new nullpomino.game.component.RuleOptions();
		ruleOptPlayer.strRandomizer = "BAG";
		ruleOptPlayer.strWallkick = "NULL";
		setField(lobby, "ruleOptPlayer", ruleOptPlayer);
	}

	@Test
	void netvsSetLockedRuleLockedWithLockOpt() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netCurrentRoomInfo.ruleLock = true;
		NetLobbyFrame lobby = (NetLobbyFrame) getField(mode, "netLobby");
		nullpomino.game.component.RuleOptions ruleOptLock = new nullpomino.game.component.RuleOptions();
		ruleOptLock.strRandomizer = "BAG";
		ruleOptLock.strWallkick = "NULL";
		setField(lobby, "ruleOptLock", ruleOptLock);
		mode.netvsSetLockedRule();
	}

	@Test
	void netvsSetLockedRuleUnlockedNotWatch() throws Exception {
		setupMinimalNetLobby();
		setupRuleOptPlayer();
		setupNetCurrentRoomInfo();
		mode.netCurrentRoomInfo.ruleLock = false;
		mode.netvsSetLockedRule();
	}

	// ================================================================
	//  netvsSetGameScreenLayout
	// ================================================================

	@Test
	void netvsSetGameScreenLayoutMainPlayer() throws Exception {
		engine.playerID = 0;
		mode.netIsWatch = false;
		mode.netvsPlayerSeatID[0] = 0;
		mode.netvsSetGameScreenLayout(engine);
		assertEquals(0, engine.displaysize);
		assertTrue(engine.enableSE);
	}

	@Test
	void netvsSetGameScreenLayoutOtherPlayers() throws Exception {
		engine.playerID = 1;
		setupNetCurrentRoomInfo();
		mode.netCurrentRoomInfo.maxPlayers = 6;
		mode.netvsPlayerSeatID[1] = 1;
		mode.netvsSetGameScreenLayout(engine);
		assertEquals(-1, engine.displaysize);
		assertFalse(engine.enableSE);
	}

	@Test
	void netvsSetGameScreenLayoutMaxPlayers2() throws Exception {
		engine.playerID = 1;
		setupNetCurrentRoomInfo();
		mode.netCurrentRoomInfo.maxPlayers = 2;
		mode.netvsPlayerSeatID[1] = 1;
		mode.netvsSetGameScreenLayout(engine);
		assertEquals(0, engine.displaysize);
	}

	@Test
	void netvsSetGameScreenLayoutExceedsMaxPlayers() throws Exception {
		engine.playerID = 5;
		setupNetCurrentRoomInfo();
		mode.netCurrentRoomInfo.maxPlayers = 2;
		mode.netvsPlayerSeatID[5] = 5;
		mode.netvsSetGameScreenLayout(engine);
		assertFalse(engine.isVisible);
	}

	// ================================================================
	//  netvsApplyRoomSettings
	// ================================================================

	@Test
	void netvsApplyRoomSettingsNullRoom() throws Exception {
		mode.netvsApplyRoomSettings(engine);
	}

	@Test
	void netvsApplyRoomSettingsWithRoomInfo() throws Exception {
		setupNetCurrentRoomInfo();
		mode.netCurrentRoomInfo.tspinEnableType = 1;
		mode.netvsApplyRoomSettings(engine);
		assertTrue(engine.tspinEnable);
		assertFalse(engine.useAllSpinBonus);
	}

	@Test
	void netvsApplyRoomSettingsAllSpin() throws Exception {
		setupNetCurrentRoomInfo();
		mode.netCurrentRoomInfo.tspinEnableType = 2;
		mode.netvsApplyRoomSettings(engine);
		assertTrue(engine.tspinEnable);
		assertTrue(engine.useAllSpinBonus);
	}

	// ================================================================
	//  netvsDrawRoomInfoBox
	// ================================================================

	@Test
	void netvsDrawRoomInfoBoxNormal() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsNumPlayers = 3;
		mode.netNumSpectators = 2;
		NetLobbyFrame lobby = (NetLobbyFrame) getField(mode, "netLobby");
		lobby.netPlayerClient.getRoomInfoList().add(mode.netCurrentRoomInfo);
		mode.netvsDrawRoomInfoBox(engine, 0, 0);
	}

	@Test
	void netvsDrawRoomInfoBoxWatchMode() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netIsWatch = true;
		NetLobbyFrame lobby = (NetLobbyFrame) getField(mode, "netLobby");
		lobby.netPlayerClient.getRoomInfoList().add(mode.netCurrentRoomInfo);
		mode.netvsDrawRoomInfoBox(engine, 0, 0);
	}

	// ================================================================
	//  netvsStartPractice
	// ================================================================

	@Test
	void netvsStartPracticeBasic() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netCurrentRoomInfo.useMap = false;
		mode.netvsStartPractice(engine);
		assertTrue(mode.netvsIsPractice);
		assertEquals(GameEngine.Status.READY, engine.stat);
	}

	// ================================================================
	//  netvsGetPlayerIDbySeatID
	// ================================================================

	@Test
	void netvsGetPlayerIDbySeatIDWithMySeat() throws Exception {
		mode.netvsMySeatID = 2;
		int result = mode.netvsGetPlayerIDbySeatID(3);
		assertEquals(NetDummyVSMode.NETVS_GAME_SEAT_NUMBERS[2][3], result);
	}

	@Test
	void netvsGetPlayerIDbySeatIDWithNegativeMySeat() throws Exception {
		mode.netvsMySeatID = -1;
		int result = mode.netvsGetPlayerIDbySeatID(3);
		assertEquals(NetDummyVSMode.NETVS_GAME_SEAT_NUMBERS[0][3], result);
	}

	// ================================================================
	//  onSetting / renderSetting
	// ================================================================

	@Test
	void onSettingNotInRoom() throws Exception {
		assertTrue(mode.onSetting(engine, 0));
	}

	@Test
	void onSettingReadyToggle() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsNumPlayers = 2;
		mode.menuTime = 5;
		mode.netvsPlayerReady[0] = false;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		assertTrue(mode.onSetting(engine, 0));
	}

	@Test
	void onSettingReadyCancel() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsNumPlayers = 2;
		mode.menuTime = 5;
		mode.netvsPlayerReady[0] = true;
		mode.netvsPlayerExist[0] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		assertTrue(mode.onSetting(engine, 0));
	}

	@Test
	void onSettingPracticeKey() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.menuTime = 5;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		assertTrue(mode.onSetting(engine, 0));
	}

	@Test
	void renderSettingVisiblePlayerReady() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		engine.isVisible = true;
		engine.playerID = 0;
		mode.netvsPlayerReady[0] = true;
		mode.netvsPlayerExist[0] = true;
		mode.netIsWatch = false;
		mode.menuTime = 5;
		assertDoesNotThrow(() -> mode.renderSetting(engine, 0));
	}

	@Test
	void renderSettingInvisibleSkips() throws Exception {
		engine.isVisible = false;
		assertDoesNotThrow(() -> mode.renderSetting(engine, 0));
	}

	@Test
	void renderSettingWatchModeSkipsActions() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		engine.isVisible = true;
		engine.playerID = 0;
		mode.netIsWatch = false;
		mode.netvsNumPlayers = 3;
		setField(mode, "netvsPlayerSeatID", new int[] {0, 1, 2, -1, -1, -1});
		assertDoesNotThrow(() -> mode.renderSetting(engine, 0));
	}

	// ================================================================
	//  onReady
	// ================================================================

	@Test
	void onReadyResetsFlags() throws Exception {
		assertFalse(mode.onReady(engine, 0));
	}

	// ================================================================
	//  startGame
	// ================================================================

	@Test
	void startGameSetsBgmForPractice() throws Exception {
		mode.netvsIsPractice = true;
		mode.startGame(engine, 0);
	}

	@Test
	void startGameSetsBgmForNormal() throws Exception {
		mode.netvsIsPractice = false;
		mode.startGame(engine, 0);
	}

	// ================================================================
	//  onMove VS override
	// ================================================================

	@Test
	void onMoveVsRemotePlayerStops() throws Exception {
		assertTrue(mode.onMove(engine, 1));
	}

	@Test
	void onMoveVsMainPlayerProceeds() throws Exception {
		setupMinimalNetLobby();
		ensureEngineHasNextPieces();
		mode.netvsIsPractice = true;
		assertFalse(mode.onMove(engine, 0));
	}

	// ================================================================
	//  onLast — auto-start timer and practice exit
	// ================================================================

	@Test
	void onLastPlayTimerIncrements() throws Exception {
		mode.netvsPlayTimerActive = true;
		mode.onLast(engine, 0);
		assertEquals(1, mode.netvsPlayTimer);
	}

	@Test
	void onLastAutoStartActiveCountsDown() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsNumPlayers = 2;
		mode.netvsAutoStartTimerActive = true;
		mode.netvsAutoStartTimer = 120;
		mode.onLast(engine, 0);
		assertEquals(119, mode.netvsAutoStartTimer);
	}

	@Test
	void onLastAutoStartExpires() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsNumPlayers = 2;
		mode.netvsAutoStartTimerActive = true;
		mode.netvsAutoStartTimer = 0;
		mode.onLast(engine, 0);
		assertFalse(mode.netvsAutoStartTimerActive);
	}

	@Test
	void onLastAutoStartSinglePlayerCancels() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsNumPlayers = 1;
		mode.netvsAutoStartTimerActive = true;
		mode.netvsAutoStartTimer = 120;
		mode.onLast(engine, 0);
		assertFalse(mode.netvsAutoStartTimerActive);
	}

	@Test
	void onLastPracticeExit() throws Exception {
		mode.netvsIsPractice = true;
		mode.netvsIsPracticeExitAllowed = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		mode.onLast(engine, 0);
		assertFalse(mode.netvsIsPractice);
	}

	// ================================================================
	//  renderLast — VS-specific rendering
	// ================================================================

	@Test
	void renderLastPlayerCount() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		engine.playerID = 5;
		mode.netLobby.netPlayerClient.getRoomInfoList().add(mode.netCurrentRoomInfo);
		assertDoesNotThrow(() -> mode.renderLast(engine, 5));
	}

	@Test
	void renderLastNoRoomInfo() throws Exception {
		setupMinimalNetLobby();
		engine.playerID = 5;
		// Set up minimal room info so netCurrentRoomInfo is not null
		NetRoomInfo ri = new NetRoomInfo();
		ri.maxPlayers = 6;
		ri.roomID = 1;
		setField(mode, "netCurrentRoomInfo", ri);
		mode.netLobby.netPlayerClient.getRoomInfoList().add(ri);
		assertDoesNotThrow(() -> mode.renderLast(engine, 5));
	}

	// ================================================================
	//  onGameOver — VS branches
	// ================================================================

	@Test
	void onGameOverVSPracticeMode() throws Exception {
		mode.netvsIsPractice = true;
		mode.netvsPlayerDead[0] = false;
		engine.statc[0] = 0;
		assertFalse(mode.onGameOver(engine, 0));
	}

	@Test
	void onGameOverVSPracticeResult() throws Exception {
		mode.netvsIsPractice = true;
		engine.statc[0] = engine.field.getHeight() + 1;
		assertTrue(mode.onGameOver(engine, 0));
		assertEquals(GameEngine.Status.RESULT, engine.stat);
	}

	@Test
	void onGameOverVSPlayerDied() throws Exception {
		setupMinimalNetLobby();
		ensureEngineHasNextPieces();
		mode.netvsPlayerDead[0] = false;
		mode.netvsIsDeadPending = false;
		mode.netIsWatch = false;
		assertTrue(mode.onGameOver(engine, 0));
	}

	@Test
	void onGameOverVSAlreadyDead() throws Exception {
		mode.netvsPlayerDead[0] = true;
		engine.field = new Field(10, 20, 4);
		engine.statc[0] = engine.field.getHeight() + 1;
		mode.netvsPlayerResultReceived[0] = false;
		assertTrue(mode.onGameOver(engine, 0));
	}

	@Test
	void onGameOverVSAlreadyDeadWaiting() throws Exception {
		mode.netvsPlayerDead[0] = true;
		engine.field = new Field(10, 20, 4);
		engine.statc[0] = 0;
		mode.netvsPlayerResultReceived[0] = false;
		assertFalse(mode.onGameOver(engine, 0));
	}

	// ================================================================
	//  renderGameOver
	// ================================================================

	@Test
	void renderGameOverPracticeSkips() throws Exception {
		mode.netvsIsPractice = true;
		engine.isVisible = true;
		assertDoesNotThrow(() -> mode.renderGameOver(engine, 0));
	}

	@Test
	void renderGameOverInvisibleSkips() throws Exception {
		engine.isVisible = false;
		assertDoesNotThrow(() -> mode.renderGameOver(engine, 0));
	}

	@Test
	void renderGameOverFirstPlace() throws Exception {
		setupNetCurrentRoomInfo();
		engine.isVisible = true;
		mode.netvsPlayerPlace[0] = 1;
		mode.netvsNumNowPlayers = 4;
		mode.netvsPlayerReady[0] = false;
		mode.netvsIsGameActive = true;
		assertDoesNotThrow(() -> mode.renderGameOver(engine, 0));
	}

	@Test
	void renderGameOverSmallDisplay() throws Exception {
		setupNetCurrentRoomInfo();
		engine.isVisible = true;
		engine.displaysize = -1;
		mode.netvsPlayerPlace[0] = 3;
		mode.netvsNumNowPlayers = 4;
		mode.netvsPlayerReady[0] = false;
		mode.netvsIsGameActive = true;
		assertDoesNotThrow(() -> mode.renderGameOver(engine, 0));
	}

	// ================================================================
	//  onExcellent / renderExcellent
	// ================================================================

	@Test
	void onExcellentVsNormal() throws Exception {
		engine.statc[0] = 0;
		assertTrue(mode.onExcellent(engine, 0));
	}

	@Test
	void renderExcellentPractice() throws Exception {
		engine.isVisible = true;
		mode.netvsIsPractice = true;
		mode.netIsWatch = false;
		engine.playerID = 0;
		assertDoesNotThrow(() -> mode.renderExcellent(engine, 0));
	}

	@Test
	void renderExcellentNormalDisplay() throws Exception {
		engine.isVisible = true;
		engine.playerID = 1;
		mode.netvsPlayerReady[1] = true;
		mode.netvsIsGameActive = false;
		assertDoesNotThrow(() -> mode.renderExcellent(engine, 1));
	}

	@Test
	void renderExcellentSmallDisplay() throws Exception {
		engine.isVisible = true;
		engine.displaysize = -1;
		mode.netvsPlayerReady[0] = true;
		mode.netvsIsGameActive = false;
		assertDoesNotThrow(() -> mode.renderExcellent(engine, 0));
	}

	// ================================================================
	//  onResult / renderResult
	// ================================================================

	@Test
	void onResultVsPlayerPressA() throws Exception {
		mode.netIsWatch = false;
		engine.playerID = 0;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		assertTrue(mode.onResult(engine, 0));
		assertEquals(GameEngine.Status.SETTING, engine.stat);
	}

	@Test
	void onResultVsPlayerPressF() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netIsWatch = false;
		engine.playerID = 0;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		assertTrue(mode.onResult(engine, 0));
	}

	@Test
	void renderResultFirstPlace() throws Exception {
		engine.isVisible = true;
		mode.netvsIsPractice = false;
		mode.netvsPlayerPlace[0] = 1;
		mode.netvsNumNowPlayers = 5;
		mode.netIsWatch = false;
		mode.netvsPlayerReady[0] = false;
		engine.playerID = 0;
		assertDoesNotThrow(() -> mode.renderResult(engine, 0));
	}

	@Test
	void renderResultPractice() throws Exception {
		engine.isVisible = true;
		mode.netvsIsPractice = true;
		mode.netvsPlayerPlace[0] = 0;
		engine.playerID = 0;
		assertDoesNotThrow(() -> mode.renderResult(engine, 0));
	}

	@Test
	void renderResultSmallDisplay() throws Exception {
		engine.isVisible = true;
		engine.displaysize = -1;
		mode.netvsIsPractice = false;
		mode.netvsPlayerPlace[0] = 3;
		engine.playerID = 0;
		assertDoesNotThrow(() -> mode.renderResult(engine, 0));
	}

	@Test
	void renderResultWatchModePlayerReady() throws Exception {
		engine.isVisible = true;
		mode.netIsWatch = true;
		mode.netvsPlayerReady[0] = true;
		mode.netvsPlayerExist[0] = true;
		mode.netvsIsPractice = false;
		mode.netvsPlayerPlace[0] = 4;
		engine.playerID = 0;
		assertDoesNotThrow(() -> mode.renderResult(engine, 0));
	}

	// ================================================================
	//  netlobbyOnMessage — VS message types
	// ================================================================

	@Test
	void netlobbyOnMessagePlayerupdateReadyChange() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		mode.netvsPlayerReady = new boolean[]{false, false, false, false, false, false};
		// Use exported NetPlayerInfo format
		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.strName = "name";
		pInfo.strTeam = "team";
		pInfo.roomID = 1;
		pInfo.uid = 1;
		pInfo.seatID = 0;
		pInfo.queueID = 0;
		pInfo.ready = true;
		pInfo.playing = true;
		pInfo.connected = true;
		String[] exported = pInfo.exportStringArray();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < exported.length; i++) {
			if (i > 0) sb.append(";");
			sb.append(exported[i] != null ? exported[i] : "");
		}
		String[] message = new String[] {"playerupdate", sb.toString()};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessagePlayerlogout() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		// Create a valid NetPlayerInfo and use its exported string format
		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.strName = "name";
		pInfo.strTeam = "team";
		pInfo.roomID = 1;
		pInfo.uid = 1;
		pInfo.seatID = 0;
		pInfo.queueID = 0;
		pInfo.ready = true;
		pInfo.playing = true;
		pInfo.connected = true;
		String[] exported = pInfo.exportStringArray();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < exported.length; i++) {
			if (i > 0) sb.append(";");
			sb.append(exported[i] != null ? exported[i] : "");
		}
		String[] message = new String[] {"playerlogout", sb.toString()};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageChangestatusOwnUID() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		initializeAllEngines();
		String[] message = new String[] {"changestatus", "watchonly", "1"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessagePlayerenter() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		String[] message = new String[] {"playerenter", "0", "0", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageAutostartbegin() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsNumPlayers = 2;
		String[] message = new String[] {"autostartbegin", "30"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		assertTrue(mode.netvsAutoStartTimerActive);
	}

	@Test
	void netlobbyOnMessageStart() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		initializeAllEngines();
		mode.netCurrentRoomInfo.maxPlayers = 6;
		mode.netCurrentRoomInfo.ruleLock = true;
		mode.netvsNumPlayers = 2;

		// Set up player info to match room
		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.seatID = 0;
		pInfo.uid = 1;
		mode.netLobby.netPlayerClient.getPlayerInfoList().add(pInfo);

		String[] message = new String[] {"start", "12345", "2", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageFinish() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		initializeAllEngines();

		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.seatID = 0;
		pInfo.uid = 1;
		mode.netLobby.netPlayerClient.getPlayerInfoList().add(pInfo);

		String[] message = new String[] {"finish", "0", "0", "0", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageFinishTeamWin() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		initializeAllEngines();
		mode.netvsPlayerExist = new boolean[]{true, true, false, false, false, false};
		mode.netvsPlayerDead = new boolean[]{false, false, false, false, false, false};
		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.seatID = 0;
		pInfo.uid = 1;
		mode.netLobby.netPlayerClient.getPlayerInfoList().add(pInfo);

		String[] message = new String[] {"finish", "0", "0", "0", "true"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageDeadVs() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		ensureEngineHasNextPieces();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		mode.netvsPlayerDead = new boolean[6];
		mode.netvsIsDeadPending = true; // prevents netSendField/NextAndHold
		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.seatID = 0;
		pInfo.uid = 1;
		mode.netLobby.netPlayerClient.getPlayerInfoList().add(pInfo);

		String[] message = new String[] {"dead", "0", "0", "0", "1"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGstat() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		String[] message = new String[] {"gstat", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameFieldVs() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		String[] message = new String[] {"game", "0", "0", "field", "0", "20", "", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameFieldAttrVs() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		String[] message = new String[] {"game", "0", "0", "fieldattr", "0", "", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGamePieceVs() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		mode.netvsPlayTimerActive = true;
		engine.stat = GameEngine.Status.READY;
		engine.statc[0] = 0;
		engine.goEnd = 10;
		String[] message = new String[] {"game", "0", "0", "piece",
				String.valueOf(Piece.PIECE_I), "3", "15", "0", "5", "0", "0", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageDisconnect() throws Exception {
		mode.netlobbyOnDisconnect(null, null, null);
		for (int i = 0; i < 6; i++) {
			assertEquals(GameEngine.Status.NOTHING, manager.engine[i].stat);
		}
	}

	// ================================================================
	//  Reflection helpers
	// ================================================================

	private Object invokeMethod(String name, Object... args) throws Exception {
		Class<?>[] argTypes = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) {
			argTypes[i] = args[i].getClass();
		}
		java.lang.reflect.Method m = findMethod(mode.getClass(), name, argTypes);
		m.setAccessible(true);
		return m.invoke(mode, args);
	}

	private static Object getField(Object obj, String name) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static java.lang.reflect.Method findMethod(Class<?> cls, String name, Class<?>... paramTypes)
			throws NoSuchMethodException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredMethod(name, paramTypes); }
			catch (NoSuchMethodException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchMethodException(name);
	}
}
