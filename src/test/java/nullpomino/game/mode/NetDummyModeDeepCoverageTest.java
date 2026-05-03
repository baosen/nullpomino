package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.util.Calendar;
import java.util.LinkedList;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetSPRecord;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Deep coverage for untested branches in {@link NetDummyMode}.
 * Covers netplay lifecycle hooks, field/piece/next networking,
 * watch mode branches, message handling, drawing, and ranking.
 */
class NetDummyModeDeepCoverageTest {

	private NetDummyMode mode;
	private GameManager manager;
	private GameEngine engine;
	private EventReceiver receiver;

	@BeforeEach
	void setUp() throws Exception {
		mode = new NetDummyMode();
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
		// Initialize ranking array needed by netplay methods
		mode.netRankingRank = new int[2];
		mode.netRankingRank[0] = -1;
		mode.netRankingRank[1] = -1;
	}

	/**
	 * Populate the engine's next-piece queue so that netSendNextAndHold
	 * (called by onMove, onARE, onEndingStart, onExcellent during netplay)
	 * does not NPE when iterating next pieces.
	 */
	private void ensureEngineHasNextPieces() {
		int n = engine.ruleopt.nextDisplay;
		if (n <= 0) n = 3;
		engine.nextPieceArrayObject = new Piece[n];
		for (int i = 0; i < n; i++) {
			engine.nextPieceArrayObject[i] = new Piece(Piece.PIECE_T);
		}
		engine.nextPieceCount = 0;
	}

	// ================================================================
	//  Helpers for setting up minimal net state
	// ================================================================

	void setupMinimalNetLobby() throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		NetPlayerClient client = new NetPlayerClient();
		setField(lobby, "netPlayerClient", client);
		setField(mode, "netLobby", lobby);
	}

	void setupNetCurrentRoomInfo() throws Exception {
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.roomID = 1;
		roomInfo.ruleName = "test_rule";
		roomInfo.rated = false;
		setField(mode, "netCurrentRoomInfo", roomInfo);
	}

	// ================================================================
	//  netplayInit
	// ================================================================

	@Test
	void netplayInitWithLobbySetsNetDummyMode() throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		NetPlayerClient client = new NetPlayerClient();
		setField(lobby, "netPlayerClient", client);
		mode.netplayInit(lobby);
		assertNotNull(getField(mode, "netLobby"));
	}

	// ================================================================
	//  onMove / pieceLocked / onLineClear / onARE with netplay
	// ================================================================

	@Test
	void onMoveWithNetplaySendsFieldAndStats() throws Exception {
		setupMinimalNetLobby();
		ensureEngineHasNextPieces();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceX = 4;
		engine.nowPieceY = 16;
		engine.nowPieceObject.direction = Piece.DIRECTION_DOWN;
		mode.netPrevPieceID = Piece.PIECE_I;
		mode.netPrevPieceX = 3;
		mode.netPrevPieceY = 15;
		mode.netPrevPieceDir = Piece.DIRECTION_UP;
		assertFalse(mode.onMove(engine, 0));
	}

	@Test
	void onMoveWatchModeReturnsTrue() throws Exception {
		mode.netIsWatch = true;
		assertTrue(mode.onMove(engine, 0));
	}

	@Test
	void pieceLockedWithNetplay() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.ending = 0;
		assertDoesNotThrow(() -> mode.pieceLocked(engine, 0, 1));
	}

	@Test
	void onLineClearWithNetplay() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.statc[0] = 1;
		engine.ending = 0;
		assertFalse(mode.onLineClear(engine, 0));
	}

	@Test
	void onAREWithNetplay() throws Exception {
		setupMinimalNetLobby();
		ensureEngineHasNextPieces();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.statc[0] = 0;
		engine.ending = 0;
		assertFalse(mode.onARE(engine, 0));
	}

	// ================================================================
	//  onEndingStart / onExcellent with netplay
	// ================================================================

	@Test
	void onEndingStartWithNetplay() throws Exception {
		setupMinimalNetLobby();
		ensureEngineHasNextPieces();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		mode.menuCursor = 0;
		assertFalse(mode.onEndingStart(engine, 0));
	}

	@Test
	void onExcellentWithNetplay() throws Exception {
		setupMinimalNetLobby();
		ensureEngineHasNextPieces();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.statc[0] = 0;
		assertFalse(mode.onExcellent(engine, 0));
	}

	// ================================================================
	//  onGameOver — all branches
	// ================================================================

	@Test
	void onGameOverNetplayNotWatchSendsDead() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		engine.statc[0] = 0;
		engine.field = new Field(10, 20, 4);
		assertFalse(mode.onGameOver(engine, 0));
	}

	@Test
	void onGameOverNetplayNotWatchResultsScreen() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		engine.statc[0] = engine.field.getHeight() + 1 + 180;
		assertFalse(mode.onGameOver(engine, 0));
	}

	@Test
	void onGameOverWatchModeWaiting() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = true;
		engine.statc[0] = 0;
		engine.field = new Field(10, 20, 4);
		assertFalse(mode.onGameOver(engine, 0));
	}

	@Test
	void onGameOverWatchModeToResult() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = true;
		engine.statc[0] = engine.field.getHeight() + 1 + 180;
		assertTrue(mode.onGameOver(engine, 0));
		assertEquals(GameEngine.Status.RESULT, engine.stat);
	}

	// ================================================================
	//  onResult — netplay replay send and retry
	// ================================================================

	@Test
	void onResultWatchModeSetsReplaySent() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = true;
		engine.allowTextRenderByReceiver = true;
		assertTrue(mode.onResult(engine, 0));
		assertEquals(2, mode.netReplaySendStatus);
	}

	@Test
	void onResultReplayModeSetsReplaySent() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		manager.replayMode = true;
		engine.allowTextRenderByReceiver = true;
		assertTrue(mode.onResult(engine, 0));
		assertEquals(2, mode.netReplaySendStatus);
	}

	@Test
	void onResultSendsReplayWhenStatusZero() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		manager.replayMode = false;
		engine.allowTextRenderByReceiver = true;
		mode.netReplaySendStatus = 0;
		assertTrue(mode.onResult(engine, 0));
	}

	// ================================================================
	//  netplayOnRetryKey
	// ================================================================

	@Test
	void netplayOnRetryKeySendsReset() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		assertDoesNotThrow(() -> mode.netplayOnRetryKey(engine, 0));
	}

	@Test
	void netplayOnRetryKeyWatchModeSkips() throws Exception {
		mode.netIsNetPlay = true;
		mode.netIsWatch = true;
		assertDoesNotThrow(() -> mode.netplayOnRetryKey(engine, 0));
	}

	// ================================================================
	//  netSendField / netRecvField
	// ================================================================

	@Test
	void netSendFieldWithoutAttributes() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netAlwaysSendFieldAttributes = false;
		engine.field.setBlock(0, 19, new Block(Block.BLOCK_COLOR_RED, 0,
				Block.BLOCK_ATTRIBUTE_VISIBLE));
		callNetSendField(engine);
	}

	@Test
	void netSendFieldWithAttributes() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netAlwaysSendFieldAttributes = true;
		engine.field.setBlock(0, 19, new Block(Block.BLOCK_COLOR_RED, 0,
				Block.BLOCK_ATTRIBUTE_VISIBLE));
		callNetSendField(engine);
	}

	@Test
	void netRecvFieldWithFieldAttr() throws Exception {
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "fieldattr", "0", "......", "false"};
		callNetRecvField(engine, message);
	}

	@Test
	void netRecvFieldWithFieldAttrDecompressed() throws Exception {
		engine.field = new Field(10, 20, 4);
		// Use empty uncompressed data so field.attrStringToField is called with ""
		String[] message = new String[] {"game", "0", "0", "fieldattr", "0", "", "false"};
		callNetRecvField(engine, message);
	}

	@Test
	void netRecvFieldWithoutAttributes() throws Exception {
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "field", "0", "20", "", "false"};
		callNetRecvField(engine, message);
	}

	@Test
	void netRecvFieldWithoutAttributesShortMessage() throws Exception {
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "field", "0"};
		callNetRecvField(engine, message);
	}

	@Test
	void netRecvFieldWithFieldAttrShortMessage() throws Exception {
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "fieldattr"};
		callNetRecvField(engine, message);
	}

	@Test
	void netRecvFieldWithoutAttributesEmptyField() throws Exception {
		// Short message without field data triggers field.reset()
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "field", "0", "20"};
		callNetRecvField(engine, message);
	}

	// ================================================================
	//  netSendPieceMovement / netRecvPieceMovement
	// ================================================================

	@Test
	void netSendPieceMovementWithNullPiece() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.nowPieceObject = null;
		mode.netPrevPieceID = Piece.PIECE_I;
		assertTrue(callNetSendPieceMovement(engine, true));
	}

	@Test
	void netSendPieceMovementWithManualLock() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.nowPieceX = 3;
		engine.nowPieceY = 15;
		engine.nowPieceObject.direction = Piece.DIRECTION_UP;
		engine.manualLock = true;
		mode.netPrevPieceID = Piece.PIECE_I;
		mode.netPrevPieceX = 3;
		mode.netPrevPieceY = 15;
		mode.netPrevPieceDir = Piece.DIRECTION_UP;
		assertTrue(callNetSendPieceMovement(engine, false));
	}

	@Test
	void netSendPieceMovementWithDifferentPiece() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceX = 4;
		engine.nowPieceY = 16;
		engine.nowPieceObject.direction = Piece.DIRECTION_DOWN;
		mode.netPrevPieceID = Piece.PIECE_I;
		mode.netPrevPieceX = 3;
		mode.netPrevPieceY = 15;
		mode.netPrevPieceDir = Piece.DIRECTION_UP;
		assertTrue(callNetSendPieceMovement(engine, false));
	}

	@Test
	void netSendPieceMovementNoChange() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.nowPieceObject = new Piece(Piece.PIECE_I);
		engine.nowPieceX = 3;
		engine.nowPieceY = 15;
		engine.nowPieceObject.direction = Piece.DIRECTION_UP;
		mode.netPrevPieceID = Piece.PIECE_I;
		mode.netPrevPieceX = 3;
		mode.netPrevPieceY = 15;
		mode.netPrevPieceDir = Piece.DIRECTION_UP;
		assertFalse(callNetSendPieceMovement(engine, false));
	}

	@Test
	void netRecvPieceMovementWithValidPiece() throws Exception {
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "piece",
				String.valueOf(Piece.PIECE_I), "3", "15", "0", "5", "0", "0", "false"};
		callNetRecvPieceMovement(engine, message);
		assertNotNull(engine.nowPieceObject);
	}

	@Test
	void netRecvPieceMovementWithBigPiece() throws Exception {
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "piece",
				String.valueOf(Piece.PIECE_I), "3", "15", "0", "5", "0", "0", "true"};
		callNetRecvPieceMovement(engine, message);
		assertTrue(engine.nowPieceObject.big);
	}

	@Test
	void netRecvPieceMovementWithNullPiece() throws Exception {
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "piece",
				String.valueOf(Piece.PIECE_NONE), "0", "0", "0", "0", "0", "0", "false"};
		callNetRecvPieceMovement(engine, message);
		assertNull(engine.nowPieceObject);
	}

	// ================================================================
	//  netSendNextAndHold / netRecvNextAndHold
	// ================================================================

	@Test
	void netSendNextAndHoldSendsMessage() throws Exception {
		setupMinimalNetLobby();
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		mode.netForceSendMovements = true;
		engine.holdPieceObject = new Piece(Piece.PIECE_O);
		engine.ruleopt.nextDisplay = 3;
		engine.nextPieceCount = 0;
		ensureEngineHasNextPieces();
		callNetSendNextAndHold(engine);
	}

	@Test
	void netRecvNextAndHoldWithData() throws Exception {
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "next", "3", "false",
				Piece.PIECE_O + ";0;0",
				Piece.PIECE_I + ";1;1",
				Piece.PIECE_T + ";2;2",
				Piece.PIECE_S + ";3;3"};
		callNetRecvNextAndHold(engine, message);
		assertTrue(engine.isNextVisible);
		assertTrue(engine.isHoldVisible);
	}

	@Test
	void netRecvNextAndHoldWithNoHold() throws Exception {
		engine.field = new Field(10, 20, 4);
		String[] message = new String[] {"game", "0", "0", "next", "2", "false",
				String.valueOf(Piece.PIECE_NONE) + ";0;0",
				Piece.PIECE_I + ";1;1",
				Piece.PIECE_T + ";2;2"};
		callNetRecvNextAndHold(engine, message);
		assertNull(engine.holdPieceObject);
	}

	// ================================================================
	//  netlobbyOnMessage — various message types
	// ================================================================

	@Test
	void netlobbyOnMessagePlayerupdate() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		String[] message = new String[] {"playerupdate", "0\t0\t0\t0\t0\tname\tteam\t0\t0\t0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageStartInWatchMode() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		String[] message = new String[] {"start"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageDeadInWatchMode() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		engine.stat = GameEngine.Status.MOVE;
		String[] message = new String[] {"dead"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageSpsendng() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = false;
		mode.owner = manager;
		mode.netReplaySendStatus = 0;
		String[] message = new String[] {"spsendng"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageSpsendok() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = false;
		mode.owner = manager;
		String[] message = new String[] {"spsendok", "1", "true", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		assertEquals(2, mode.netReplaySendStatus);
		assertTrue(mode.netIsPB);
	}

	@Test
	void netlobbyOnMessageReset1pWatchMode() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		String[] message = new String[] {"reset1p"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameCursor() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		engine.stat = GameEngine.Status.SETTING;
		String[] message = new String[] {"game", "0", "0", "cursor", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameField() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		String[] message = new String[] {"game", "0", "0", "field", "0", "20", "", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameFieldAttr() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		String[] message = new String[] {"game", "0", "0", "fieldattr", "0", "", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameStats() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		String[] message = new String[] {"game", "0", "0", "stats", "0"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGamePiece() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		String[] message = new String[] {"game", "0", "0", "piece",
				String.valueOf(Piece.PIECE_I), "3", "15", "0", "5", "0", "0", "false"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameNext() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		String[] message = new String[] {"game", "0", "0", "next",
				"1", "false", Piece.PIECE_O + ";0;0", Piece.PIECE_I + ";1;1"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameEnding() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		engine.staffrollEnable = false;
		String[] message = new String[] {"game", "0", "0", "ending"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		assertEquals(1, engine.ending);
	}

	@Test
	void netlobbyOnMessageGameExcellent() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		String[] message = new String[] {"game", "0", "0", "excellent"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	@Test
	void netlobbyOnMessageGameRetry() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		engine.ending = 0;
		String[] message = new String[] {"game", "0", "0", "retry"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void netlobbyOnMessageGameResultsscreen() throws Exception {
		setupMinimalNetLobby();
		mode.netIsWatch = true;
		mode.owner = manager;
		String[] message = new String[] {"game", "0", "0", "resultsscreen"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		assertEquals(GameEngine.Status.RESULT, engine.stat);
	}

	// ================================================================
	//  netDrawAllPlayersCount
	// ================================================================

	@Test
	void netDrawAllPlayersCountWithNullLobby() throws Exception {
		mode.netLobby = null;
		callNetDrawAllPlayersCount(engine);
	}

	// ================================================================
	//  netDrawGameRate — all color branches
	// ================================================================

	@Test
	void netDrawGameRateNotNetplay() throws Exception {
		mode.netIsNetPlay = false;
		callNetDrawGameRate(engine);
	}

	@Test
	void netDrawGameRateWatchMode() throws Exception {
		mode.netIsNetPlay = true;
		mode.netIsWatch = true;
		callNetDrawGameRate(engine);
	}

	@Test
	void netDrawGameRateNotStarted() throws Exception {
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		engine.gameStarted = false;
		callNetDrawGameRate(engine);
	}

	@Test
	void netDrawGameRateWithEndTime() throws Exception {
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		engine.gameStarted = true;
		engine.startTime = System.nanoTime() - 1_000_000_000L;
		engine.endTime = System.nanoTime();
		engine.statistics.gamerate = 0.95f;
		callNetDrawGameRate(engine);
	}

	@Test
	void netDrawGameRateRunning() throws Exception {
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		engine.gameStarted = true;
		engine.startTime = System.nanoTime() - 1_000_000_000L;
		engine.endTime = 0;
		engine.replayTimer = 60_000_000;
		callNetDrawGameRate(engine);
	}

	// ================================================================
	//  netDrawSpectatorsCount
	// ================================================================

	@Test
	void netDrawSpectatorsCountNotNetplay() throws Exception {
		mode.netIsNetPlay = false;
		callNetDrawSpectatorsCount(engine, 0, 0);
	}

	@Test
	void netDrawSpectatorsCountWatchMode() throws Exception {
		mode.netIsNetPlay = true;
		mode.netIsWatch = true;
		engine.stat = GameEngine.Status.SETTING;
		callNetDrawSpectatorsCount(engine, 0, 0);
	}

	@Test
	void netDrawSpectatorsCountNotWatch() throws Exception {
		mode.netIsNetPlay = true;
		mode.netIsWatch = false;
		engine.stat = GameEngine.Status.SETTING;
		callNetDrawSpectatorsCount(engine, 0, 0);
	}

	// ================================================================
	//  netDrawPlayerName
	// ================================================================

	@Test
	void netDrawPlayerNameWithNullName() throws Exception {
		mode.netPlayerName = null;
		callNetDrawPlayerName(engine);
	}

	@Test
	void netDrawPlayerNameWithEmptyName() throws Exception {
		mode.netPlayerName = "";
		callNetDrawPlayerName(engine);
	}

	@Test
	void netDrawPlayerNameWithName() throws Exception {
		mode.netPlayerName = "TestPlayer";
		callNetDrawPlayerName(engine);
	}

	// ================================================================
	//  netOnJoin
	// ================================================================

	@Test
	void netOnJoinSetsWatchFlag() throws Exception {
		setupMinimalNetLobby();
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.roomID = 1;
		roomInfo.ruleLock = false;

		NetPlayerInfo myInfo = new NetPlayerInfo();
		myInfo.seatID = -1; // spectator
		myInfo.uid = 1;
		NetPlayerClient client = (NetPlayerClient) getField(getField(mode, "netLobby"), "netPlayerClient");
		client.getPlayerInfoList().add(myInfo);
		setField(client, "playerUID", 1);

		callNetOnJoin((NetLobbyFrame) getField(mode, "netLobby"), client, roomInfo);
		assertTrue(mode.netIsWatch);
	}

	@Test
	void netOnJoinSetsPlayerFlag() throws Exception {
		setupMinimalNetLobby();
		NetRoomInfo roomInfo = new NetRoomInfo();
		roomInfo.roomID = 1;
		roomInfo.ruleLock = false;

		NetPlayerInfo myInfo = new NetPlayerInfo();
		myInfo.seatID = 0;
		myInfo.uid = 1;
		NetPlayerClient client = (NetPlayerClient) getField(getField(mode, "netLobby"), "netPlayerClient");
		client.getPlayerInfoList().add(myInfo);
		setField(client, "playerUID", 1);

		callNetOnJoin((NetLobbyFrame) getField(mode, "netLobby"), client, roomInfo);
		assertFalse(mode.netIsWatch);
	}

	// ================================================================
	//  netRecvNetPlayRanking — various ranking types
	// ================================================================

	@Test
	void netRecvNetPlayRankingGenericScore() throws Exception {
		String[] message = new String[] {"spranking", "", "", "", "false",
				String.valueOf(NetSPRecord.RANKINGTYPE_GENERIC_SCORE), "1",
				"0," + nullpomino.game.net.NetUtil.urlEncode("TestPlayer") + ",2024/01/01 12:00:00,1.0,1000,10,500"};
		callNetRecvNetPlayRanking(engine, message);
	}

	@Test
	void netRecvNetPlayRankingGenericTime() throws Exception {
		String[] message = new String[] {"spranking", "", "", "", "false",
				String.valueOf(NetSPRecord.RANKINGTYPE_GENERIC_TIME), "1",
				"0," + nullpomino.game.net.NetUtil.urlEncode("TestPlayer") + ",2024/01/01 12:00:00,1.0,500,100,2.0"};
		callNetRecvNetPlayRanking(engine, message);
	}

	@Test
	void netRecvNetPlayRankingScoreRace() throws Exception {
		String[] message = new String[] {"spranking", "", "", "", "false",
				String.valueOf(NetSPRecord.RANKINGTYPE_SCORERACE), "1",
				"0," + nullpomino.game.net.NetUtil.urlEncode("TestPlayer") + ",2024/01/01 12:00:00,1.0,500,10,5.0"};
		callNetRecvNetPlayRanking(engine, message);
	}

	@Test
	void netRecvNetPlayRankingDigRace() throws Exception {
		String[] message = new String[] {"spranking", "", "", "", "false",
				String.valueOf(NetSPRecord.RANKINGTYPE_DIGRACE), "1",
				"0," + nullpomino.game.net.NetUtil.urlEncode("TestPlayer") + ",2024/01/01 12:00:00,1.0,500,10,100"};
		callNetRecvNetPlayRanking(engine, message);
	}

	@Test
	void netRecvNetPlayRankingUltra() throws Exception {
		String[] message = new String[] {"spranking", "", "", "", "false",
				String.valueOf(NetSPRecord.RANKINGTYPE_ULTRA), "1",
				"0," + nullpomino.game.net.NetUtil.urlEncode("TestPlayer") + ",2024/01/01 12:00:00,1.0,1000,10,100"};
		callNetRecvNetPlayRanking(engine, message);
	}

	@Test
	void netRecvNetPlayRankingComboRace() throws Exception {
		String[] message = new String[] {"spranking", "", "", "", "false",
				String.valueOf(NetSPRecord.RANKINGTYPE_COMBORACE), "1",
				"0," + nullpomino.game.net.NetUtil.urlEncode("TestPlayer") + ",2024/01/01 12:00:00,1.0,5,500,2.0"};
		callNetRecvNetPlayRanking(engine, message);
	}

	@Test
	void netRecvNetPlayRankingDigChallenge() throws Exception {
		String[] message = new String[] {"spranking", "", "", "", "false",
				String.valueOf(NetSPRecord.RANKINGTYPE_DIGCHALLENGE), "1",
				"0," + nullpomino.game.net.NetUtil.urlEncode("TestPlayer") + ",2024/01/01 12:00:00,1.0,1000,10,500"};
		callNetRecvNetPlayRanking(engine, message);
	}

	@Test
	void netRecvNetPlayRankingTimeAttack() throws Exception {
		String[] message = new String[] {"spranking", "", "", "", "false",
				String.valueOf(NetSPRecord.RANKINGTYPE_TIMEATTACK), "1",
				"0," + nullpomino.game.net.NetUtil.urlEncode("TestPlayer") + ",2024/01/01 12:00:00,1.0,10,500,2.0,0"};
		callNetRecvNetPlayRanking(engine, message);
	}

	@Test
	void netRecvNetPlayRankingNoData() throws Exception {
		String[] message = new String[] {"spranking", "", "", "", "true"};
		callNetRecvNetPlayRanking(engine, message);
	}

	// ================================================================
	//  netOnRenderNetPlayRanking — all ranking type branches
	// ================================================================

	@Test
	void netOnRenderNetPlayRankingGenericScore() throws Exception {
		setupRankingDisplay(NetSPRecord.RANKINGTYPE_GENERIC_SCORE);
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	@Test
	void netOnRenderNetPlayRankingGenericTime() throws Exception {
		setupRankingDisplay(NetSPRecord.RANKINGTYPE_GENERIC_TIME);
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	@Test
	void netOnRenderNetPlayRankingScoreRace() throws Exception {
		setupRankingDisplay(NetSPRecord.RANKINGTYPE_SCORERACE);
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	@Test
	void netOnRenderNetPlayRankingDigRace() throws Exception {
		setupRankingDisplay(NetSPRecord.RANKINGTYPE_DIGRACE);
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	@Test
	void netOnRenderNetPlayRankingUltra() throws Exception {
		setupRankingDisplay(NetSPRecord.RANKINGTYPE_ULTRA);
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	@Test
	void netOnRenderNetPlayRankingComboRace() throws Exception {
		setupRankingDisplay(NetSPRecord.RANKINGTYPE_COMBORACE);
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	@Test
	void netOnRenderNetPlayRankingDigChallenge() throws Exception {
		setupRankingDisplay(NetSPRecord.RANKINGTYPE_DIGCHALLENGE);
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	@Test
	void netOnRenderNetPlayRankingTimeAttack() throws Exception {
		setupRankingDisplay(NetSPRecord.RANKINGTYPE_TIMEATTACK);
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	@Test
	void netOnRenderNetPlayRankingNoData() throws Exception {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { true, true };
		mode.netRankingReady = new boolean[] { false, false };
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	@Test
	void netOnRenderNetPlayRankingLoading() throws Exception {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingReady = new boolean[] { false, false };
		mode.netRankingPlace = new LinkedList[2];
		callNetOnRenderNetPlayRanking(engine, 0, receiver);
	}

	// ================================================================
	//  netSendReplay
	// ================================================================

	@Test
	void netSendReplayNotSendOK() throws Exception {
		mode.netReplaySendStatus = 0;
		callNetSendReplay(engine);
		assertEquals(2, mode.netReplaySendStatus);
	}

	// ================================================================
	//  netOnUpdateNetPlayRanking — all controller inputs
	// ================================================================

	@Test
	void netOnUpdateNetPlayRankingUpDown() throws Exception {
		enableRankingUpdate();
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		callNetOnUpdateNetPlayRanking(engine, 0);
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 0;
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = false;

		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		callNetOnUpdateNetPlayRanking(engine, 0);
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 0;
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = false;
	}

	@Test
	void netOnUpdateNetPlayRankingDownload() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		enableRankingUpdate();
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		callNetOnUpdateNetPlayRanking(engine, 0);
	}

	@Test
	void netOnUpdateNetPlayRankingLeftRight() throws Exception {
		enableRankingUpdate();
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		callNetOnUpdateNetPlayRanking(engine, 0);

		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		callNetOnUpdateNetPlayRanking(engine, 0);
	}

	@Test
	void netOnUpdateNetPlayRankingExit() throws Exception {
		enableRankingUpdate();
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		callNetOnUpdateNetPlayRanking(engine, 0);
		assertFalse(mode.netIsNetRankingDisplayMode);
	}

	// ================================================================
	//  enterNetPlayRankingScreen
	// ================================================================

	@Test
	void netEnterNetPlayRankingScreen() throws Exception {
		setupMinimalNetLobby();
		setupNetCurrentRoomInfo();
		mode.netCurrentRoomInfo.rated = true;
		mode.netRankingPlace = new LinkedList[2];
		mode.netRankingPlace[0] = new LinkedList<>();
		mode.netRankingPlace[1] = new LinkedList<>();
		callNetEnterNetPlayRankingScreen(engine, 0, 0);
		assertTrue(mode.netIsNetRankingDisplayMode);
	}

	// ================================================================
	//  Delegate callers for protected methods
	// ================================================================

	// These are in the same package so protected methods are accessible.
	// We provide explicit caller wrappers for clarity and to avoid
	// reflection issues with primitive types.

	private void callNetSendField(GameEngine e) {
		mode.netSendField(e);
	}

	private void callNetRecvField(GameEngine e, String[] msg) {
		mode.netRecvField(e, msg);
	}

	private boolean callNetSendPieceMovement(GameEngine e, boolean force) {
		return mode.netSendPieceMovement(e, force);
	}

	private void callNetRecvPieceMovement(GameEngine e, String[] msg) {
		mode.netRecvPieceMovement(e, msg);
	}

	private void callNetSendNextAndHold(GameEngine e) {
		mode.netSendNextAndHold(e);
	}

	private void callNetRecvNextAndHold(GameEngine e, String[] msg) {
		mode.netRecvNextAndHold(e, msg);
	}

	private void callNetDrawAllPlayersCount(GameEngine e) {
		mode.netDrawAllPlayersCount(e);
	}

	private void callNetDrawGameRate(GameEngine e) {
		mode.netDrawGameRate(e);
	}

	private void callNetDrawSpectatorsCount(GameEngine e, int x, int y) {
		mode.netDrawSpectatorsCount(e, x, y);
	}

	private void callNetDrawPlayerName(GameEngine e) {
		mode.netDrawPlayerName(e);
	}

	private void callNetOnJoin(NetLobbyFrame l, NetPlayerClient c, NetRoomInfo r) {
		mode.netOnJoin(l, c, r);
	}

	private void callNetRecvNetPlayRanking(GameEngine e, String[] msg) {
		mode.netRecvNetPlayRanking(e, msg);
	}

	private void callNetOnRenderNetPlayRanking(GameEngine e, int pid, EventReceiver r) {
		mode.netOnRenderNetPlayRanking(e, pid, r);
	}

	private void callNetSendReplay(GameEngine e) {
		mode.netSendReplay(e);
	}

	private void callNetOnUpdateNetPlayRanking(GameEngine e, int gt) {
		mode.netOnUpdateNetPlayRanking(e, gt);
	}

	private void callNetEnterNetPlayRankingScreen(GameEngine e, int pid, int gt) {
		mode.netEnterNetPlayRankingScreen(e, pid, gt);
	}

	// ================================================================
	//  Setup helpers for ranking data
	// ================================================================

	private void setupRankingDisplay(int rankingType) throws Exception {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingReady = new boolean[] { true, true };
		mode.netRankingType = rankingType;
		mode.netRankingCursor = new int[] { 0, 0 };
		mode.netRankingMyRank = new int[] { 0, 0 };

		setField(mode, "netRankingPlace", makeListArray(0));
		setField(mode, "netRankingName", makeStringArray("TestPlayer"));
		setField(mode, "netRankingScore", makeListArray(1000));
		setField(mode, "netRankingLines", makeListArray(10));
		setField(mode, "netRankingTime", makeListArray(500));
		setField(mode, "netRankingPPS", makeFloatArray(1.0f));
		setField(mode, "netRankingPiece", makeListArray(100));
		setField(mode, "netRankingSPL", makeDoubleArray(5.0));
		setField(mode, "netRankingRollclear", makeListArray(0));
		setField(mode, "netRankingDate", makeCalendarArray());
		setField(mode, "netRankingGamerate", makeFloatArray(1.0f));
	}

	private void enableRankingUpdate() throws Exception {
		setField(mode, "netRankingPlace", makeListArray(0));
		setField(mode, "netRankingName", makeStringArray("TestPlayer"));
		setField(mode, "netRankingScore", makeListArray(1000));
		setField(mode, "netRankingLines", makeListArray(10));
		setField(mode, "netRankingTime", makeListArray(500));
		setField(mode, "netRankingPPS", makeFloatArray(1.0f));

		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingReady = new boolean[] { true, true };
		mode.netRankingCursor = new int[] { 0, 0 };
		mode.netRankingMyRank = new int[] { -1, -1 };
		mode.netRankingType = NetSPRecord.RANKINGTYPE_GENERIC_SCORE;
		mode.netRankingView = 0;
		mode.owner = manager;
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<Integer>[] makeListArray(int value) {
		LinkedList<Integer>[] arr = new LinkedList[2];
		arr[0] = new LinkedList<>();
		arr[0].add(value);
		arr[1] = new LinkedList<>();
		return arr;
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<String>[] makeStringArray(String value) {
		LinkedList<String>[] arr = new LinkedList[2];
		arr[0] = new LinkedList<>();
		arr[0].add(value);
		arr[1] = new LinkedList<>();
		return arr;
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<Float>[] makeFloatArray(float value) {
		LinkedList<Float>[] arr = new LinkedList[2];
		arr[0] = new LinkedList<>();
		arr[0].add(value);
		arr[1] = new LinkedList<>();
		return arr;
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<Double>[] makeDoubleArray(double value) {
		LinkedList<Double>[] arr = new LinkedList[2];
		arr[0] = new LinkedList<>();
		arr[0].add(value);
		arr[1] = new LinkedList<>();
		return arr;
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<Calendar>[] makeCalendarArray() {
		LinkedList<Calendar>[] arr = new LinkedList[2];
		arr[0] = new LinkedList<>();
		arr[0].add(Calendar.getInstance());
		arr[1] = new LinkedList<>();
		return arr;
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
}
