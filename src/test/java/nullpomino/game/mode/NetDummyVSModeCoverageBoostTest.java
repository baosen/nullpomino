package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.RuleOptions;
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
 * Coverage-boosting tests for {@link NetDummyVSMode}. Drives the netplay
 * lifecycle and message handlers that remained uncovered by the existing
 * suites: netUpdatePlayerExist player-loop + team coloring, netOnJoin
 * newcomer path, netPlayerInit, netDrawPlayerName, netSendPieceMovement,
 * netvsApplyRoomSettings (parameterless + tspin off branch),
 * netvsStartPractice map loading, netvsRecvEndGameStats, onSetting random
 * map preview, renderSetting ready/cancel + onReady map loading, onMove
 * auto-lock, renderLast 2-player layout + auto-start timer, onGameOver
 * null-field branch, renderGameOver/renderExcellent/renderResult placement
 * branches, and the netlobbyOnMessage handlers (playerupdate SE, changestatus
 * newcomer, playerleave, start, finish team/normal win, dead forced-death,
 * game piece/next).
 *
 * <p>Idiom (matches NetDummyVSModeDeepCoverageTest): attach a disconnected
 * {@link NetLobbyFrame} + {@link NetPlayerClient} ({@code send()} is a safe
 * no-op while disconnected), populate the player list, and set
 * {@code netCurrentRoomInfo} via reflection.
 */
class NetDummyVSModeCoverageBoostTest {

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

	/** Wire a disconnected lobby + client; "me" sits at the given seat. */
	NetLobbyFrame setupLobby(int mySeatID, int myRoomID) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		NetPlayerClient client = new NetPlayerClient();
		NetPlayerInfo myInfo = new NetPlayerInfo();
		myInfo.seatID = mySeatID;
		myInfo.uid = 1;
		myInfo.roomID = myRoomID;
		myInfo.strName = "me";
		client.getPlayerInfoList().add(myInfo);
		setField(client, "playerUID", 1);
		setField(client, "playerName", "me");
		setField(lobby, "netPlayerClient", client);
		// netvsSetLockedRule reverts to ruleOptPlayer when the room is not
		// rule-locked; supply a valid one so the revert path doesn't NPE.
		RuleOptions ruleOptPlayer = new RuleOptions();
		ruleOptPlayer.strRandomizer = "";
		ruleOptPlayer.strWallkick = "";
		setField(lobby, "ruleOptPlayer", ruleOptPlayer);
		setField(mode, "netLobby", lobby);
		return lobby;
	}

	NetRoomInfo setupRoom(int roomID, int maxPlayers) throws Exception {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.roomID = roomID;
		roomInfo.ruleName = "test_rule";
		roomInfo.ruleLock = false;
		roomInfo.rated = false;
		roomInfo.maxPlayers = maxPlayers;
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
		return roomInfo;
	}

	/** Add another player into the same room as "me". */
	NetPlayerInfo addPlayer(NetLobbyFrame lobby, int uid, int seatID, int roomID,
			String name, String team, boolean ready, boolean playing) {
		NetPlayerInfo p = new NetPlayerInfo();
		p.uid = uid;
		p.seatID = seatID;
		p.roomID = roomID;
		p.strName = name;
		p.strTeam = team;
		p.ready = ready;
		p.playing = playing;
		p.winCountNow = 1;
		p.playCountNow = 2;
		lobby.netPlayerClient.getPlayerInfoList().add(p);
		return p;
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

	private void pressButton(int btn) {
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	// ================================================================
	//  netUpdatePlayerExist (lines 297-324): player loop + team coloring
	// ================================================================

	@Test
	void netUpdatePlayerExistPopulatesPlayersAndTeams() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		// me is at seat 0, no team
		lobby.netPlayerClient.getPlayerInfoList().get(0).strTeam = "Alpha";
		// A spectator (seatID -1) so netNumSpectators increments
		addPlayer(lobby, 2, -1, 1, "spec", "", false, false);
		// Two more seated players on teams (one shared with me, one new)
		addPlayer(lobby, 3, 1, 1, "p2", "Alpha", true, true);
		addPlayer(lobby, 4, 2, 1, "p3", "Bravo", false, true);

		mode.netUpdatePlayerExist();

		assertEquals(3, mode.netvsNumPlayers, "3 seated players counted");
		assertEquals(1, mode.netNumSpectators, "1 spectator counted");
		assertEquals(0, mode.netvsMySeatID);
		// Team colors assigned (>0) for players with a team
		int[] teamColor = (int[]) getField(mode, "netvsPlayerTeamColor");
		boolean anyColored = false;
		for (int c : teamColor) if (c > 0) anyColored = true;
		assertTrue(anyColored, "at least one player team-colored");
	}

	// ================================================================
	//  netOnJoin (343-349) + newcomer path
	// ================================================================

	@Test
	void netOnJoinNewcomerSetsNumNowPlayers() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 6);
		room.playing = true; // newcomer joining a game in progress
		addPlayer(lobby, 3, 1, 1, "p2", "", false, true);

		// netOnJoin re-reads room; pass the same room object.
		invokeNetOnJoin(lobby, lobby.netPlayerClient, room);

		assertTrue(mode.netIsNetPlay);
		// netvsIsNewcomer == room.playing == true -> numNowPlayers = numPlayers
		assertEquals(mode.netvsNumPlayers, getInt(mode, "netvsNumNowPlayers"));
	}

	@Test
	void netOnJoinNonNewcomer() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 6);
		room.playing = false;
		addPlayer(lobby, 3, 1, 1, "p2", "", false, true);

		invokeNetOnJoin(lobby, lobby.netPlayerClient, room);

		assertTrue(mode.netIsNetPlay);
	}

	private void invokeNetOnJoin(NetLobbyFrame lobby, NetPlayerClient client, NetRoomInfo room)
			throws Exception {
		java.lang.reflect.Method m = NetDummyVSMode.class.getDeclaredMethod("netOnJoin",
				NetLobbyFrame.class, NetPlayerClient.class, NetRoomInfo.class);
		m.setAccessible(true);
		m.invoke(mode, lobby, client, room);
	}

	// ================================================================
	//  netPlayerInit (357-366) via playerInit
	// ================================================================

	@Test
	void playerInitSetsFieldDimensions() throws Exception {
		mode.playerInit(engine, 0);
		assertEquals(10, engine.fieldWidth);
		assertEquals(20, engine.fieldHeight);
		assertFalse(engine.gameoverAll);
		assertTrue(engine.allowTextRenderByReceiver);
	}

	// ================================================================
	//  netDrawPlayerName (372-391)
	// ================================================================

	@Test
	void netDrawPlayerNameNormalAndSmallAndLong() throws Exception {
		setupLobby(0, 1);
		setupRoom(1, 6);
		String[] names = (String[]) getField(mode, "netvsPlayerName");
		names[0] = "AVeryLongPlayerNameThatExceedsLimits";
		int[] teamColor = (int[]) getField(mode, "netvsPlayerTeamColor");
		teamColor[0] = 2;

		// playerID 0, normal display -> truncated to 14
		engine.playerID = 0;
		engine.displaysize = 0;
		invokeNetDrawPlayerName(engine);

		// displaysize == -1 -> truncated to 7
		engine.displaysize = -1;
		invokeNetDrawPlayerName(engine);

		// playerID != 0, normal display -> else branch
		engine.playerID = 1;
		engine.displaysize = 0;
		names[1] = "p2";
		teamColor[1] = -5; // forces clamp to 0
		invokeNetDrawPlayerName(engine);
	}

	private void invokeNetDrawPlayerName(GameEngine eng) throws Exception {
		java.lang.reflect.Method m = NetDummyVSMode.class.getDeclaredMethod(
				"netDrawPlayerName", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, eng);
	}

	// ================================================================
	//  netSendPieceMovement (421-424)
	// ================================================================

	@Test
	void netSendPieceMovementMainPlayerSends() throws Exception {
		setupLobby(0, 1);
		mode.netvsIsPractice = false;
		mode.netIsWatch = false;
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.updateConnectData();
		java.lang.reflect.Method m = NetDummyVSMode.class.getDeclaredMethod(
				"netSendPieceMovement", GameEngine.class, boolean.class);
		m.setAccessible(true);
		// First call with forceSend -> returns true via super
		boolean result = (boolean) m.invoke(mode, engine, true);
		assertTrue(result, "main-player send returns super's true");
	}

	@Test
	void netSendPieceMovementPracticeReturnsFalse() throws Exception {
		setupLobby(0, 1);
		mode.netvsIsPractice = true;
		engine.playerID = 0;
		java.lang.reflect.Method m = NetDummyVSMode.class.getDeclaredMethod(
				"netSendPieceMovement", GameEngine.class, boolean.class);
		m.setAccessible(true);
		boolean result = (boolean) m.invoke(mode, engine, true);
		assertFalse(result, "practice mode short-circuits to false");
	}

	// ================================================================
	//  netvsApplyRoomSettings() parameterless (495-498) + tspin off (518-519)
	// ================================================================

	@Test
	void netvsApplyRoomSettingsAllEnginesTspinOff() throws Exception {
		initializeAllEngines();
		NetRoomInfo room = setupRoom(1, 6);
		room.tspinEnableType = 0;
		java.lang.reflect.Method m = NetDummyVSMode.class.getDeclaredMethod(
				"netvsApplyRoomSettings");
		m.setAccessible(true);
		m.invoke(mode);
		assertFalse(engine.tspinEnable, "tspinEnableType 0 disables tspin");
		assertFalse(engine.useAllSpinBonus);
	}

	// ================================================================
	//  netvsStartPractice map loading (566-582)
	// ================================================================

	@Test
	void netvsStartPracticeLoadsMap() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 6);
		room.useMap = true;
		lobby.mapList.add("0g0\n"); // a minimal field string
		lobby.mapList.add("0g0\n");

		mode.netvsStartPractice(engine);

		assertTrue(mode.netvsIsPractice);
		assertEquals(GameEngine.Status.READY, engine.stat);
		assertTrue(getInt(mode, "netvsMapPreviousPracticeMap") >= 0,
				"a map index was selected");
	}

	// ================================================================
	//  netvsRecvEndGameStats (590-595)
	// ================================================================

	@Test
	void netvsRecvEndGameStatsForOpponent() throws Exception {
		setupLobby(0, 1);
		mode.netvsMySeatID = 0;
		// seatID 1 -> playerID != 0 -> result received flag set
		String[] message = {"gstat", "x", "1"};
		java.lang.reflect.Method m = NetDummyVSMode.class.getDeclaredMethod(
				"netvsRecvEndGameStats", String[].class);
		m.setAccessible(true);
		m.invoke(mode, (Object) message);
		int pid = mode.netvsGetPlayerIDbySeatID(1);
		boolean[] received = (boolean[]) getField(mode, "netvsPlayerResultReceived");
		assertTrue(received[pid]);
	}

	// ================================================================
	//  onSetting random map preview (708-722)
	// ================================================================

	@Test
	void onSettingRandomMapPreviewWithExistingPlayer() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 6);
		room.useMap = true;
		lobby.mapList.add("0g0\n");
		lobby.mapList.add("0g0\n");
		mode.netvsPlayerExist[0] = true;
		mode.menuTime = 0; // menuTime % 30 == 0 triggers preview
		engine.statc[5] = 0;

		assertTrue(mode.onSetting(engine, 0));
		assertNotNull(engine.field);
	}

	@Test
	void onSettingRandomMapPreviewResetsFieldForNonExistingPlayer() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 6);
		room.useMap = true;
		lobby.mapList.add("0g0\n");
		// playerID 1 doesn't exist -> field reset branch
		mode.netvsPlayerExist[1] = false;
		engine.field.setBlock(0, 0,
				new nullpomino.game.component.Block(
						nullpomino.game.component.Block.BLOCK_COLOR_RED));
		assertTrue(mode.onSetting(engine, 1));
	}

	// ================================================================
	//  renderSetting ready/cancel + small display (740-769)
	// ================================================================

	@Test
	void renderSettingReadyOkSmallDisplay() throws Exception {
		setupLobby(0, 1);
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.displaysize = -1; // small display -> line 745
		engine.playerID = 0;
		mode.netvsPlayerReady[0] = true;
		mode.netvsPlayerExist[0] = true;
		mode.netIsWatch = false;
		mode.menuTime = 5;
		assertDoesNotThrow(() -> mode.renderSetting(engine, 0));
	}

	@Test
	void renderSettingCancelPromptWhenReady() throws Exception {
		setupLobby(0, 1);
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.playerID = 0;
		mode.netIsWatch = false;
		mode.netvsNumPlayers = 3;
		mode.netvsPlayerReady[0] = true; // -> "B ... CANCEL" branch (755-758)
		mode.menuTime = 5;
		assertDoesNotThrow(() -> mode.renderSetting(engine, 0));
	}

	@Test
	void renderSettingReadyPromptWhenNotReady() throws Exception {
		setupLobby(0, 1);
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.playerID = 0;
		mode.netIsWatch = false;
		mode.netvsNumPlayers = 3;
		mode.netvsPlayerReady[0] = false; // -> "A ... READY" branch (749-753)
		mode.menuTime = 5;
		assertDoesNotThrow(() -> mode.renderSetting(engine, 0));
	}

	// ================================================================
	//  onReady map loading (777-796)
	// ================================================================

	@Test
	void onReadyLoadsMapMainPlayer() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 6);
		room.useMap = true;
		lobby.mapList.add("0g0\n");
		setField(mode, "netvsMapNo", 0);
		mode.netvsIsPractice = false;
		mode.netIsWatch = false;
		engine.playerID = 0;
		engine.statc[0] = 0;
		assertFalse(mode.onReady(engine, 0));
		assertNotNull(engine.field);
	}

	@Test
	void onReadyPracticeExitAllowedAfterDelay() throws Exception {
		setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 6);
		room.useMap = false;
		mode.netvsIsPractice = true;
		engine.statc[0] = 10; // >= 10 -> practice exit allowed
		assertFalse(mode.onReady(engine, 0));
		assertTrue(getBool(mode, "netvsIsPracticeExitAllowed"));
	}

	@Test
	void onReadyMapLoadRuleLockSkin() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 6);
		room.useMap = true;
		room.ruleLock = true;
		RuleOptions ruleOptLock = new RuleOptions();
		ruleOptLock.skin = 1;
		setField(lobby, "ruleOptLock", ruleOptLock);
		lobby.mapList.add("0g0\n");
		setField(mode, "netvsMapNo", 0);
		mode.netvsIsPractice = false;
		// playerID 1, not watch -> hits ruleLock skin branch (784-785)
		engine.playerID = 1;
		engine.statc[0] = 0;
		assertFalse(mode.onReady(engine, 1));
	}

	// ================================================================
	//  onMove auto-lock (842-846)
	// ================================================================

	@Test
	void onMoveAutoLockForcesPieceDown() throws Exception {
		setupLobby(0, 1);
		ensureEngineHasNextPieces();
		mode.netvsIsPractice = false;
		engine.playerID = 0;
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceObject.updateConnectData();
		engine.nowPieceBottomY = 18;
		setField(mode, "netvsPieceMoveTimerMax", 1); // expire immediately
		setField(mode, "netvsPieceMoveTimer", 0);

		assertFalse(mode.onMove(engine, 0));
		// After auto-lock, piece Y snapped to bottom, timer reset
		assertEquals(18, engine.nowPieceY);
		assertEquals(0, getInt(mode, "netvsPieceMoveTimer"));
	}

	// ================================================================
	//  renderLast 2-player layout + auto-start timer (905-934)
	// ================================================================

	@Test
	void renderLastTwoPlayerLayoutAndTimer() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 2); // maxPlayers == 2 -> lines 911/913
		lobby.netPlayerClient.getRoomInfoList().add(room);
		engine.playerID = 5; // getPlayers()-1 -> draws room info box
		assertDoesNotThrow(() -> mode.renderLast(engine, 5));

		// player 0 + auto-start timer active -> line 929
		mode.netvsAutoStartTimerActive = true;
		mode.netvsIsGameActive = false;
		mode.netvsPlayTimerActive = true;
		assertDoesNotThrow(() -> mode.renderLast(engine, 0));
	}

	@Test
	void renderLastPracticeTimerShown() throws Exception {
		setupLobby(0, 1);
		setupRoom(1, 6);
		engine.playerID = 0;
		mode.netvsIsPractice = true; // -> practice time draw (922-924)
		assertDoesNotThrow(() -> mode.renderLast(engine, 0));
	}

	// ================================================================
	//  onGameOver null-field branch (974-977)
	// ================================================================

	@Test
	void onGameOverPlayerDeadNullField() throws Exception {
		mode.netvsPlayerDead[0] = true;
		engine.field = null; // -> SETTING branch (975-977)
		assertTrue(mode.onGameOver(engine, 0));
		assertEquals(GameEngine.Status.SETTING, engine.stat);
	}

	// ================================================================
	//  renderGameOver placement branches (999-1034)
	// ================================================================

	@Test
	void renderGameOverPlacementsNormalDisplay() throws Exception {
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.displaysize = 0;
		mode.netvsNumNowPlayers = 4;
		mode.netvsIsGameActive = true;
		mode.netvsPlayerReady[0] = false;
		for (int place = 2; place <= 6; place++) {
			mode.netvsPlayerPlace[0] = place;
			final int p = place;
			assertDoesNotThrow(() -> mode.renderGameOver(engine, 0),
					"place " + p + " normal");
		}
	}

	@Test
	void renderGameOverLoseTwoPlayers() throws Exception {
		setupRoom(1, 2); // maxPlayers == 2 -> LOSE branch (1002-1003)
		engine.isVisible = true;
		engine.displaysize = 0;
		mode.netvsNumNowPlayers = 2;
		mode.netvsIsGameActive = true;
		mode.netvsPlayerReady[0] = false;
		assertDoesNotThrow(() -> mode.renderGameOver(engine, 0));
	}

	@Test
	void renderGameOverPlacementsSmallDisplay() throws Exception {
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.displaysize = -1;
		mode.netvsNumNowPlayers = 4;
		mode.netvsIsGameActive = true;
		mode.netvsPlayerReady[0] = false;
		for (int place = 2; place <= 6; place++) {
			mode.netvsPlayerPlace[0] = place;
			final int p = place;
			assertDoesNotThrow(() -> mode.renderGameOver(engine, 0),
					"place " + p + " small");
		}
	}

	@Test
	void renderGameOverReadyOkSmall() throws Exception {
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.displaysize = -1;
		mode.netvsPlayerReady[0] = true; // OK branch (1018-1019)
		mode.netvsIsGameActive = false;
		assertDoesNotThrow(() -> mode.renderGameOver(engine, 0));
	}

	// ================================================================
	//  onExcellent force-result (1053-1055) + statc increment
	// ================================================================

	@Test
	void onExcellentForceResultWithButtonA() throws Exception {
		// playerID 1 (not 0) so result-received isn't auto-set, and game still
		// active so the jumped statc[0] is NOT immediately reset to RESULT.
		engine.isVisible = true;
		engine.playerID = 1;
		mode.netvsIsGameActive = true;
		engine.statc[0] = 120; // >= 120 + button A -> force jump (1053-1055)
		pressButton(Controller.BUTTON_A);
		assertTrue(mode.onExcellent(engine, 1));
		assertTrue(engine.statc[0] >= engine.field.getHeight() + 1 + 180,
				"statc jumped to end + buffer");
	}

	@Test
	void onExcellentFinishesToResult() throws Exception {
		engine.isVisible = true;
		mode.netvsIsGameActive = false;
		mode.netvsPlayerResultReceived[0] = true;
		engine.statc[0] = engine.field.getHeight() + 1 + 180; // -> result transition
		assertTrue(mode.onExcellent(engine, 0));
		assertEquals(GameEngine.Status.RESULT, engine.stat);
	}

	// ================================================================
	//  renderExcellent branches (1074-1099)
	// ================================================================

	@Test
	void renderExcellentWinTwoPlayerNormal() throws Exception {
		setupRoom(1, 2);
		engine.isVisible = true;
		engine.displaysize = 0;
		engine.playerID = 1;
		mode.netvsNumNowPlayers = 2; // -> WIN! branch (1085-1086)
		mode.netvsIsPractice = false;
		mode.netvsPlayerReady[1] = false;
		mode.netvsIsGameActive = false;
		assertDoesNotThrow(() -> mode.renderExcellent(engine, 1));
	}

	@Test
	void renderExcellentFirstPlaceNormal() throws Exception {
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.displaysize = 0;
		engine.playerID = 1;
		mode.netvsNumNowPlayers = 5; // else -> 1ST PLACE! (1088)
		mode.netvsIsPractice = false;
		mode.netvsPlayerReady[1] = false;
		mode.netvsIsGameActive = false;
		assertDoesNotThrow(() -> mode.renderExcellent(engine, 1));
	}

	@Test
	void renderExcellentWinSmallDisplay() throws Exception {
		setupRoom(1, 2);
		engine.isVisible = true;
		engine.displaysize = -1;
		engine.playerID = 1;
		mode.netvsNumNowPlayers = 2; // small -> WIN! (1095-1096)
		mode.netvsIsPractice = false;
		mode.netvsPlayerReady[1] = false;
		mode.netvsIsGameActive = false;
		assertDoesNotThrow(() -> mode.renderExcellent(engine, 1));
	}

	@Test
	void renderExcellentFirstPlaceSmallDisplay() throws Exception {
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.displaysize = -1;
		engine.playerID = 1;
		mode.netvsNumNowPlayers = 5; // small else -> 1ST PLACE!
		mode.netvsIsPractice = false;
		mode.netvsPlayerReady[1] = false;
		mode.netvsIsGameActive = false;
		assertDoesNotThrow(() -> mode.renderExcellent(engine, 1));
	}

	// ================================================================
	//  renderResult placement branches (1144-1194)
	// ================================================================

	@Test
	void renderResultAllPlacementsNormal() throws Exception {
		setupLobby(0, 1);
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.displaysize = 0;
		engine.playerID = 0;
		mode.netIsWatch = false;
		mode.netvsIsPractice = false;
		mode.netvsNumNowPlayers = 5;
		for (int place = 1; place <= 6; place++) {
			mode.netvsPlayerPlace[0] = place;
			final int p = place;
			assertDoesNotThrow(() -> mode.renderResult(engine, 0),
					"place " + p);
		}
	}

	@Test
	void renderResultWinLoseTwoPlayers() throws Exception {
		setupLobby(0, 1);
		setupRoom(1, 2);
		engine.isVisible = true;
		engine.displaysize = 0;
		engine.playerID = 0;
		mode.netIsWatch = false;
		mode.netvsIsPractice = false;
		mode.netvsNumNowPlayers = 2; // WIN!/LOSE branches (1148/1154)
		mode.netvsPlayerPlace[0] = 1;
		assertDoesNotThrow(() -> mode.renderResult(engine, 0));
		mode.netvsPlayerPlace[0] = 2;
		assertDoesNotThrow(() -> mode.renderResult(engine, 0));
	}

	@Test
	void renderResultPracticeRetryLabel() throws Exception {
		setupLobby(0, 1);
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.playerID = 0;
		mode.netIsWatch = false;
		mode.netvsIsPractice = true; // -> "RETRY" label (1184) + PRACTICE header
		assertDoesNotThrow(() -> mode.renderResult(engine, 0));
	}

	@Test
	void renderResultWatchPlayerReadyOk() throws Exception {
		setupLobby(-1, 1);
		setupRoom(1, 6);
		engine.isVisible = true;
		engine.displaysize = -1; // small -> bottom OK branch (1193-1194)
		engine.playerID = 0;
		mode.netIsWatch = true;
		mode.netvsIsPractice = false;
		mode.netvsPlayerReady[0] = true;
		mode.netvsPlayerExist[0] = true;
		mode.netvsPlayerPlace[0] = 3;
		assertDoesNotThrow(() -> mode.renderResult(engine, 0));
	}

	// ================================================================
	//  netlobbyOnMessage: playerupdate ready-SE (1234-1235)
	// ================================================================

	@Test
	void netlobbyOnMessagePlayerupdateOpponentReadySE() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		mode.netvsPlayerReady = new boolean[]{false, false, false, false, false, false};
		// An opponent at seat 1 turns ready -> playSE("decide") path (1234)
		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.strName = "opp";
		pInfo.roomID = 1;
		pInfo.uid = 9;
		pInfo.seatID = 1;
		pInfo.ready = true;
		pInfo.playing = false;
		String[] message = {"playerupdate", pInfo.exportString()};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
	}

	@Test
	void netlobbyOnMessagePlayerupdateOpponentCancelSE() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		mode.netvsPlayerReady = new boolean[]{false, true, false, false, false, false};
		// Opponent at seat 1 cancels (ready=false, playing=false) -> playSE("change") (1235)
		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.strName = "opp";
		pInfo.roomID = 1;
		pInfo.uid = 9;
		pInfo.seatID = 1;
		pInfo.ready = false;
		pInfo.playing = false;
		String[] message = {"playerupdate", pInfo.exportString()};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
	}

	// ================================================================
	//  netlobbyOnMessage: changestatus newcomer (1257-1276)
	// ================================================================

	@Test
	void netlobbyOnMessageChangestatusNewcomerDuringGame() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		initializeAllEngines();
		mode.netvsIsGameActive = true; // -> netvsIsNewcomer = true (1260)
		mode.netIsWatch = false;
		// uid "1" matches the local player's UID
		String[] message = {"changestatus", "join", "1"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertTrue(getBool(mode, "netvsIsNewcomer"));
	}

	// ================================================================
	//  netlobbyOnMessage: playerleave (1287-1291)
	// ================================================================

	@Test
	void netlobbyOnMessagePlayerleaveCancelsAutoStart() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		mode.netvsAutoStartTimerActive = true;
		mode.netvsNumPlayers = 0; // < 2 after leave -> cancels auto-start
		String[] message = {"playerleave", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertFalse(mode.netvsAutoStartTimerActive);
	}

	// ================================================================
	//  netlobbyOnMessage: start handler (1306-1377)
	// ================================================================

	@Test
	void netlobbyOnMessageStartTwoPlayerRuleLock() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 2);
		room.ruleLock = true;
		initializeAllEngines();
		mode.netvsNumPlayers = 2;
		// Two seated players exist in the room
		addPlayer(lobby, 3, 1, 1, "p2", "", false, true);
		// start: randseed(hex), numNowPlayers, mapNo
		String[] message = {"start", "abc", "2", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertTrue(mode.netvsIsGameActive);
	}

	@Test
	void netlobbyOnMessageStartManyPlayers() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		NetRoomInfo room = setupRoom(1, 6);
		room.ruleLock = false;
		initializeAllEngines();
		mode.netvsNumPlayers = 3;
		addPlayer(lobby, 3, 1, 1, "p2", "", false, true);
		addPlayer(lobby, 4, 2, 1, "p3", "", false, true);
		String[] message = {"start", "1", "3", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertTrue(mode.netvsIsGameActive);
	}

	// ================================================================
	//  netlobbyOnMessage: dead forced-death (1383-1401)
	// ================================================================

	@Test
	void netlobbyOnMessageDeadSelfForcedDeath() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		ensureEngineHasNextPieces();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		mode.netvsPlayerDead = new boolean[6];
		mode.netvsIsDeadPending = false; // -> forced death send block (1392-1396)
		// dead: seatID at index 3, place at index 4. seatID 0 == my seat
		String[] message = {"dead", "x", "x", "0", "1"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertTrue(mode.netvsPlayerDead[0]);
	}

	// ================================================================
	//  netlobbyOnMessage: finish team win + normal win (1409-1467)
	// ================================================================

	@Test
	void netlobbyOnMessageFinishTeamWin() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		initializeAllEngines();
		mode.netvsPlayerExist = new boolean[]{true, true, false, false, false, false};
		mode.netvsPlayerDead = new boolean[]{false, false, false, false, false, false};
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		// finish: winnerSeat(2), ?, teamWin(4)
		String[] message = {"finish", "x", "0", "x", "true"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertFalse(mode.netvsIsGameActive);
		assertTrue(mode.netvsIsGameFinished);
	}

	@Test
	void netlobbyOnMessageFinishNormalWin() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		initializeAllEngines();
		mode.netvsPlayerExist = new boolean[]{true, true, false, false, false, false};
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		// finish with a winning seat 0 and teamWin=false (1443-1459)
		String[] message = {"finish", "x", "0", "x", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertFalse(mode.netvsIsGameActive);
	}

	@Test
	void netlobbyOnMessageFinishStopsPractice() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		initializeAllEngines();
		mode.netvsIsPractice = true; // -> practice-stop block (1416-1423)
		mode.netvsPlayerExist = new boolean[]{true, false, false, false, false, false};
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		String[] message = {"finish", "x", "-1", "x", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertFalse(mode.netvsIsPractice);
	}

	// ================================================================
	//  netlobbyOnMessage: game piece + next (1470-1506)
	// ================================================================

	@Test
	void netlobbyOnMessageGamePieceWatchStartsTimer() throws Exception {
		NetLobbyFrame lobby = setupLobby(-1, 1); // spectator -> netvsIsWatch true
		setupRoom(1, 6);
		initializeAllEngines();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		mode.netIsWatch = true;
		mode.netvsIsNewcomer = false;
		mode.netvsPlayTimerActive = false;
		mode.netvsIsGameFinished = false;
		// game piece for seat 0
		String[] message = {"game", "x", "0", "piece",
				String.valueOf(Piece.PIECE_I), "3", "15", "0", "5", "0", "0", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertTrue(mode.netvsPlayTimerActive, "watch piece message starts play timer");
	}

	@Test
	void netlobbyOnMessageGamePieceForceStart() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		initializeAllEngines();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		mode.netIsWatch = false;
		mode.netvsPlayTimerActive = true;
		mode.netvsIsPractice = false;
		// engine for seat 0 (playerID 0) is READY with statc[0] < goEnd -> force start (1501).
		// Use a null piece (id -1) so netRecvPieceMovement leaves stat at READY.
		manager.engine[0].stat = GameEngine.Status.READY;
		manager.engine[0].statc[0] = 0;
		manager.engine[0].goEnd = 10;
		String[] message = {"game", "x", "0", "piece",
				"-1", "3", "15", "0", "5", "0", "0", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertEquals(10, manager.engine[0].statc[0]);
	}

	@Test
	void netlobbyOnMessageGameNext() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		initializeAllEngines();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		// game next for seat 0: maxNext, holdDisable, then hold;dir;color
		String[] message = {"game", "x", "0", "next", "1", "false",
				Piece.PIECE_NONE + ";0;0", Piece.PIECE_T + ";0;0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
	}

	@Test
	void netlobbyOnMessageGameFieldCreatesFieldWhenNull() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		initializeAllEngines();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		// Null out the target engine's field -> createFieldIfNeeded branch (1475-1477)
		manager.engine[0].field = null;
		String[] message = {"game", "x", "0", "field", "0", "20", "", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
		assertNotNull(manager.engine[0].field);
	}

	@Test
	void netlobbyOnMessageGameStats() throws Exception {
		NetLobbyFrame lobby = setupLobby(0, 1);
		setupRoom(1, 6);
		initializeAllEngines();
		mode.netvsPlayerSeatID = new int[]{0, 1, 2, 3, 4, 5};
		String[] message = {"game", "x", "0", "stats", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(lobby, lobby.netPlayerClient, message));
	}

	// ================================================================
	//  Reflection helpers
	// ================================================================

	private static Object getField(Object obj, String name) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static int getInt(Object obj, String name) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static boolean getBool(Object obj, String name) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static void setField(Object obj, String name, int value) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}
}
