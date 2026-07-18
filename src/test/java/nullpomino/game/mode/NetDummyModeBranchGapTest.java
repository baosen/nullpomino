package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.component.RuleOptions;
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
 * Branch-gap tests for {@link NetDummyMode}: exercises the residual uncovered
 * branches (spectator-count vs force-send short circuits, watch-mode guards,
 * ranking cursor/render side conditions, rule-lock join, and piece delta detection).
 */
class NetDummyModeBranchGapTest {

	/** Records every terminal draw call so "nothing drawn" is observable. */
	static class CountingReceiver extends EventReceiver {
		final List<String> drawn = new ArrayList<>();
		@Override public void drawMenuFont(GameEngine e, int p, int x, int y, String s, int color, float scale) { drawn.add("menu:" + s); }
		@Override public void drawTTFMenuFont(GameEngine e, int p, int x, int y, String s, int color) { drawn.add("ttfmenu:" + s); }
		@Override public void drawScoreFont(GameEngine e, int p, int x, int y, String s, int color, float scale) { drawn.add("score:" + s); }
		@Override public void drawDirectFont(GameEngine e, int p, int x, int y, String s, int color, float scale) { drawn.add("direct:" + s); }
	}

	/** Offline client stub: records sent messages, lets tests fake connection state. */
	static class StubClient extends NetPlayerClient {
		final List<String> sent = new ArrayList<>();
		boolean connected = false;
		@Override public boolean send(String msg) { sent.add(msg); return true; }
		@Override public boolean isConnected() { return connected; }
		void setUID(int uid) { playerUID = uid; }
		void setCounts(int observers, int players) { observerCount = observers; playerCount = players; }
		void addRoom(NetRoomInfo r) { roomInfoList.add(r); }
	}

	/** Lets tests enable the net-ranking prompt branch. */
	static class TestMode extends NetDummyMode {
		boolean rankingViewOK = false;
		@Override protected boolean netIsNetRankingViewOK(GameEngine engine) { return rankingViewOK; }
	}

	private TestMode mode;
	private GameManager manager;
	private GameEngine engine;
	private CountingReceiver receiver;
	private NetLobbyFrame lobby;
	private StubClient client;

	@BeforeEach
	void setUp() {
		mode = new TestMode();
		receiver = new CountingReceiver();
		manager = new GameManager(receiver);
		manager.mode = mode;
		mode.modeInit(manager);
		manager.init();
		engine = manager.engine[0];
		engine.init();
		engine.ruleopt.fieldWidth = 10;
		engine.ruleopt.fieldHeight = 20;
		engine.ruleopt.fieldHiddenHeight = 4;
		engine.createFieldIfNeeded();
		engine.stat = GameEngine.Status.MOVE;
		engine.statc[0] = 2;
		mode.netRankingRank = new int[] { -1, -1 };

		lobby = new NetLobbyFrame();
		client = new StubClient();
		lobby.netPlayerClient = client;
		mode.netLobby = lobby;
	}

	// ---------------- helpers ----------------

	private void netplay(boolean watch) {
		mode.netIsNetPlay = true;
		mode.netIsWatch = watch;
	}

	private void press(int btn) {
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private void nextPieces() {
		int n = engine.ruleopt.nextDisplay;
		if (n <= 0) n = 3;
		engine.nextPieceArrayObject = new Piece[n];
		for (int i = 0; i < n; i++) engine.nextPieceArrayObject[i] = new Piece(Piece.PIECE_T);
		engine.nextPieceCount = 0;
	}

	private void movingPiece() {
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceX = 4;
		engine.nowPieceY = 10;
		engine.nowPieceObject.direction = 0;
		mode.netPrevPieceID = Piece.PIECE_I;
		mode.netPrevPieceX = 3;
		mode.netPrevPieceY = 9;
		mode.netPrevPieceDir = 0;
	}

	private String sentJoined() {
		return String.join("", client.sent);
	}

	/** Populate every ranking list on side {@code d} with {@code n} rows and enter display mode. */
	private void setupRanking(int type, int n, int d) {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingType = type;
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingReady = new boolean[2];
		mode.netRankingReady[d] = true;
		mode.netRankingCursor = new int[] { 0, 0 };
		mode.netRankingMyRank = new int[] { -1, -1 };
		mode.netRankingView = d;
		mode.netRankingPlace[d] = new LinkedList<>();
		mode.netRankingName[d] = new LinkedList<>();
		mode.netRankingDate[d] = new LinkedList<>();
		mode.netRankingGamerate[d] = new LinkedList<>();
		mode.netRankingTime[d] = new LinkedList<>();
		mode.netRankingScore[d] = new LinkedList<>();
		mode.netRankingPiece[d] = new LinkedList<>();
		mode.netRankingPPS[d] = new LinkedList<>();
		mode.netRankingLines[d] = new LinkedList<>();
		mode.netRankingSPL[d] = new LinkedList<>();
		mode.netRankingRollclear[d] = new LinkedList<>();
		for (int i = 0; i < n; i++) {
			mode.netRankingPlace[d].add(i);
			mode.netRankingName[d].add("P" + i);
			mode.netRankingDate[d].add(Calendar.getInstance());
			mode.netRankingGamerate[d].add(1.0f);
			mode.netRankingTime[d].add(600 + i);
			mode.netRankingScore[d].add(1000 + i);
			mode.netRankingPiece[d].add(50 + i);
			mode.netRankingPPS[d].add(1.5f);
			mode.netRankingLines[d].add(10 + i);
			mode.netRankingSPL[d].add(2.0);
			mode.netRankingRollclear[d].add(i % 3);
		}
	}

	// ---------------- netplayInit (line 160) ----------------

	@Test
	void netplayInitJoinsExistingRoom() {
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 5;
		NetPlayerInfo me = new NetPlayerInfo();
		me.uid = 1;
		me.roomID = 5;
		me.seatID = 0;
		client.getPlayerInfoList().add(me);
		client.setUID(1);
		client.addRoom(room);

		mode.netplayInit(lobby);

		assertTrue(mode.netIsNetPlay);
		assertFalse(mode.netIsWatch);
		assertSame(room, mode.netCurrentRoomInfo);
	}

	// ---------------- onMove (lines 249/256/259) ----------------

	@Test
	void onMoveSpectatorsTriggerFieldPieceAndNextSend() {
		netplay(false);
		mode.netNumSpectators = 1;
		mode.netForceSendMovements = false;
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		nextPieces();
		movingPiece();

		assertFalse(mode.onMove(engine, 0));
		String all = sentJoined();
		assertTrue(all.contains("game\tfield"));
		assertTrue(all.contains("game\tpiece"));
		assertTrue(all.contains("game\tnext"));
	}

	@Test
	void onMoveWatchModeSendsNothingAndStopsGame() {
		netplay(true);
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		movingPiece();

		assertTrue(mode.onMove(engine, 0));
		assertEquals(0, client.sent.size());
	}

	@Test
	void onMoveNoAudienceSendsNothing() {
		netplay(false);
		mode.netNumSpectators = 0;
		mode.netForceSendMovements = false;
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		movingPiece();

		assertFalse(mode.onMove(engine, 0));
		assertEquals(0, client.sent.size());
	}

	@Test
	void onMoveNullPieceSkipsPieceSend() {
		netplay(false);
		mode.netForceSendMovements = true;
		engine.ending = 0;
		engine.statc[0] = 1; // skip the field/stats block
		engine.nowPieceObject = null;
		mode.netPrevPieceID = Piece.PIECE_NONE;

		assertFalse(mode.onMove(engine, 0));
		assertEquals(0, client.sent.size());
	}

	@Test
	void onMoveUnchangedPieceSkipsNextSend() {
		netplay(false);
		mode.netForceSendMovements = true;
		engine.ending = 0;
		engine.statc[0] = 1;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceX = 4;
		engine.nowPieceY = 10;
		engine.nowPieceObject.direction = 0;
		mode.netPrevPieceID = Piece.PIECE_T;
		mode.netPrevPieceX = 4;
		mode.netPrevPieceY = 10;
		mode.netPrevPieceDir = 0;

		assertFalse(mode.onMove(engine, 0));
		assertFalse(sentJoined().contains("game\tnext"));
		assertFalse(sentJoined().contains("game\tpiece"));
	}

	// ---------------- pieceLocked (line 277) ----------------

	@Test
	void pieceLockedDuringEndingSendsNothing() {
		netplay(false);
		mode.netForceSendMovements = true;
		engine.ending = 1;
		mode.pieceLocked(engine, 0, 1);
		assertEquals(0, client.sent.size());
	}

	@Test
	void pieceLockedWatchModeSendsNothing() {
		netplay(true);
		mode.netForceSendMovements = true;
		engine.ending = 0;
		mode.pieceLocked(engine, 0, 1);
		assertEquals(0, client.sent.size());
	}

	@Test
	void pieceLockedSpectatorsTriggerSend() {
		netplay(false);
		mode.netNumSpectators = 2;
		mode.netForceSendMovements = false;
		engine.ending = 0;
		mode.pieceLocked(engine, 0, 1);
		assertTrue(sentJoined().contains("game\tfield"));
	}

	@Test
	void pieceLockedNoAudienceSendsNothing() {
		netplay(false);
		mode.netNumSpectators = 0;
		mode.netForceSendMovements = false;
		engine.ending = 0;
		mode.pieceLocked(engine, 0, 1);
		assertEquals(0, client.sent.size());
	}

	// ---------------- onLineClear (line 289) ----------------

	@Test
	void onLineClearBranchMatrix() {
		engine.statc[0] = 1;

		// ending != 0
		netplay(false);
		mode.netForceSendMovements = true;
		engine.ending = 1;
		assertFalse(mode.onLineClear(engine, 0));
		assertEquals(0, client.sent.size());

		// watch mode
		engine.ending = 0;
		netplay(true);
		assertFalse(mode.onLineClear(engine, 0));
		assertEquals(0, client.sent.size());

		// no audience
		netplay(false);
		mode.netForceSendMovements = false;
		mode.netNumSpectators = 0;
		assertFalse(mode.onLineClear(engine, 0));
		assertEquals(0, client.sent.size());

		// spectators > 0 short-circuits force flag
		mode.netNumSpectators = 1;
		assertFalse(mode.onLineClear(engine, 0));
		assertTrue(sentJoined().contains("game\tfield"));
	}

	// ---------------- onARE (line 302) ----------------

	@Test
	void onAREBranchMatrix() {
		engine.statc[0] = 0;
		nextPieces();

		netplay(false);
		mode.netForceSendMovements = true;
		engine.ending = 1;
		assertFalse(mode.onARE(engine, 0));
		assertEquals(0, client.sent.size());

		engine.ending = 0;
		netplay(true);
		assertFalse(mode.onARE(engine, 0));
		assertEquals(0, client.sent.size());

		netplay(false);
		mode.netForceSendMovements = false;
		mode.netNumSpectators = 0;
		assertFalse(mode.onARE(engine, 0));
		assertEquals(0, client.sent.size());

		mode.netNumSpectators = 1;
		assertFalse(mode.onARE(engine, 0));
		String all = sentJoined();
		assertTrue(all.contains("game\tfield"));
		assertTrue(all.contains("game\tnext"));
	}

	// ---------------- onEndingStart (lines 315/317) ----------------

	@Test
	void onEndingStartCursorNonZeroSendsNothing() {
		netplay(false);
		mode.netForceSendMovements = true;
		mode.menuCursor = 1;
		nextPieces();
		assertFalse(mode.onEndingStart(engine, 0));
		assertEquals(0, client.sent.size());
	}

	@Test
	void onEndingStartBranchMatrix() {
		mode.menuCursor = 0;
		nextPieces();

		netplay(true);
		mode.netForceSendMovements = true;
		assertFalse(mode.onEndingStart(engine, 0));
		assertEquals(0, client.sent.size());

		netplay(false);
		mode.netForceSendMovements = false;
		mode.netNumSpectators = 0;
		assertFalse(mode.onEndingStart(engine, 0));
		assertEquals(0, client.sent.size());

		mode.netNumSpectators = 1;
		assertFalse(mode.onEndingStart(engine, 0));
		assertTrue(sentJoined().contains("game\tending"));
	}

	// ---------------- onExcellent (line 334) ----------------

	@Test
	void onExcellentBranchMatrix() {
		engine.statc[0] = 0;
		nextPieces();

		netplay(true);
		mode.netForceSendMovements = true;
		assertFalse(mode.onExcellent(engine, 0));
		assertEquals(0, client.sent.size());

		netplay(false);
		mode.netForceSendMovements = false;
		mode.netNumSpectators = 0;
		assertFalse(mode.onExcellent(engine, 0));
		assertEquals(0, client.sent.size());

		mode.netNumSpectators = 1;
		assertFalse(mode.onExcellent(engine, 0));
		assertTrue(sentJoined().contains("game\texcellent"));
	}

	// ---------------- onGameOver (lines 354/361) ----------------

	@Test
	void onGameOverSpectatorsSendEverything() {
		netplay(false);
		mode.netNumSpectators = 1;
		mode.netForceSendMovements = false;
		engine.statc[0] = 0;
		nextPieces();

		assertFalse(mode.onGameOver(engine, 0));
		String all = sentJoined();
		assertTrue(all.contains("game\tfield"));
		assertTrue(all.contains("dead\t-1"));
	}

	@Test
	void onGameOverMidAnimationSendsNothing() {
		netplay(false);
		mode.netForceSendMovements = true;
		engine.statc[0] = 1; // > 0 but below field height + 181 threshold

		assertFalse(mode.onGameOver(engine, 0));
		assertEquals(0, client.sent.size());
	}

	// ---------------- onResult (lines 398/400) ----------------

	@Test
	void onResultRetryWithSpectatorsSendsRetry() {
		netplay(false);
		mode.netNumSpectators = 1;
		mode.netForceSendMovements = false;
		mode.netReplaySendStatus = 2;
		press(Controller.BUTTON_A);

		assertTrue(mode.onResult(engine, 0));
		assertTrue(sentJoined().contains("game\tretry"));
	}

	@Test
	void onResultRetryNoAudienceSkipsRetryMessage() {
		netplay(false);
		mode.netNumSpectators = 0;
		mode.netForceSendMovements = false;
		mode.netReplaySendStatus = 2;
		press(Controller.BUTTON_A);

		assertTrue(mode.onResult(engine, 0));
		assertFalse(sentJoined().contains("game\tretry"));
	}

	@Test
	void onResultAPushInWatchModeDoesNotRetry() {
		netplay(true);
		press(Controller.BUTTON_A);

		assertTrue(mode.onResult(engine, 0));
		assertEquals(2, mode.netReplaySendStatus);
		assertEquals(0, client.sent.size());
	}

	@Test
	void onResultAPushWhileStillSendingDoesNotRetry() {
		netplay(false);
		mode.netReplaySendStatus = 1; // sending in progress
		press(Controller.BUTTON_A);

		assertTrue(mode.onResult(engine, 0));
		assertEquals(1, mode.netReplaySendStatus);
		assertEquals(0, client.sent.size());
	}

	// ---------------- renderLast + netDrawAllPlayersCount (418/690/691) ----------------

	@Test
	void renderLastLastPlayerDrawsObserverCountGreen() {
		client.connected = true;
		client.setCounts(1, 1); // observers > 0 -> green
		mode.renderLast(engine, 0);
		assertEquals(1, receiver.drawn.size());
		assertTrue(receiver.drawn.get(0).startsWith("direct:"));
		assertTrue(receiver.drawn.get(0).contains("1/1"));
	}

	@Test
	void renderLastLastPlayerDrawsPlayerCountRed() {
		client.connected = true;
		client.setCounts(0, 2); // players > 1 -> red
		mode.renderLast(engine, 0);
		assertEquals(1, receiver.drawn.size());
		assertTrue(receiver.drawn.get(0).contains("0/2"));
	}

	@Test
	void renderLastNonLastPlayerDrawsNothing() {
		client.connected = true;
		client.setCounts(1, 2);
		mode.renderLast(engine, 3);
		assertEquals(0, receiver.drawn.size());
	}

	// ---------------- updateCursor (line 432) ----------------

	@Test
	void updateCursorForceSendSignalsSpectators() {
		netplay(false);
		mode.netNumSpectators = 0;
		mode.netForceSendMovements = true;
		mode.menuCursor = 2;
		press(Controller.BUTTON_UP);

		assertEquals(0, mode.updateCursor(engine, 5, 0));
		assertEquals(1, mode.menuCursor);
		assertTrue(sentJoined().contains("game\tcursor\t1"));
	}

	@Test
	void updateCursorSpectatorCountSignalsSpectators() {
		netplay(false);
		mode.netNumSpectators = 1;
		mode.netForceSendMovements = false;
		mode.menuCursor = 2;
		press(Controller.BUTTON_DOWN);

		assertEquals(0, mode.updateCursor(engine, 5, 0));
		assertEquals(3, mode.menuCursor);
		assertTrue(sentJoined().contains("game\tcursor\t3"));
	}

	@Test
	void updateCursorNoAudienceMovesSilently() {
		netplay(false);
		mode.netNumSpectators = 0;
		mode.netForceSendMovements = false;
		mode.menuCursor = 2;
		press(Controller.BUTTON_UP);

		assertEquals(0, mode.updateCursor(engine, 5, 0));
		assertEquals(1, mode.menuCursor);
		assertEquals(0, client.sent.size());
	}

	// ---------------- netlobbyOnMessage "dead" (line 516) ----------------

	@Test
	void deadMessageWatchKeepsResultStat() throws Exception {
		netplay(true);
		engine.stat = GameEngine.Status.RESULT;
		mode.netlobbyOnMessage(null, null, new String[] { "dead" });
		assertEquals(GameEngine.Status.RESULT, engine.stat);
	}

	@Test
	void deadMessageWatchKeepsGameoverStat() throws Exception {
		netplay(true);
		engine.stat = GameEngine.Status.GAMEOVER;
		mode.netlobbyOnMessage(null, null, new String[] { "dead" });
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	// ---------------- netOnJoin (lines 640/642) ----------------

	private void seatMyself(int seatID) {
		NetPlayerInfo me = new NetPlayerInfo();
		me.uid = 1;
		me.roomID = 5;
		me.seatID = seatID;
		client.getPlayerInfoList().add(me);
		client.setUID(1);
	}

	@Test
	void netOnJoinNullRoomStillEntersNetplay() {
		seatMyself(0);
		mode.netOnJoin(lobby, client, null);
		assertTrue(mode.netIsNetPlay);
		assertFalse(mode.netIsWatch);
	}

	@Test
	void netOnJoinRuleLockCopiesLockedRule() {
		seatMyself(0);
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 5;
		room.ruleLock = true;
		lobby.ruleOptLock = new RuleOptions();
		lobby.ruleOptLock.strRuleName = "LOCKED";
		lobby.ruleOptLock.strRandomizer = "nullpomino.game.randomizer.BagRandomizer";
		lobby.ruleOptLock.strWallkick = "nullpomino.game.wallkick.StandardWallkick";

		mode.netOnJoin(lobby, client, room);

		// The ruleLock branch copies the locked rule name onto the engine.
		// (randomizer/wallkick are resolved by class name via ClassFactory,
		// whose impls aren't a build dep of this test - the copy branch still runs.)
		assertEquals("LOCKED", engine.ruleopt.strRuleName);
	}

	@Test
	void netOnJoinRuleLockWithoutLockedRuleKeepsOwnRule() {
		seatMyself(0);
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 5;
		room.ruleLock = true;
		lobby.ruleOptLock = null;
		String before = engine.ruleopt.strRuleName;

		mode.netOnJoin(lobby, client, room);

		assertEquals(before, engine.ruleopt.strRuleName);
	}

	// ---------------- netUpdatePlayerExist (line 674) ----------------

	@Test
	void netUpdatePlayerExistCountsSeatsAndSpectators() {
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 3;
		mode.netCurrentRoomInfo = room;

		NetPlayerInfo host = new NetPlayerInfo();
		host.uid = 1; host.roomID = 3; host.seatID = 0; host.strName = "HOST";
		NetPlayerInfo spectator = new NetPlayerInfo();
		spectator.uid = 2; spectator.roomID = 3; spectator.seatID = -1;
		NetPlayerInfo secondSeat = new NetPlayerInfo();
		secondSeat.uid = 3; secondSeat.roomID = 3; secondSeat.seatID = 1; // neither host nor spectator
		client.getPlayerInfoList().add(host);
		client.getPlayerInfoList().add(spectator);
		client.getPlayerInfoList().add(secondSeat);
		client.setUID(1);

		mode.netUpdatePlayerExist();

		assertEquals("HOST", mode.netPlayerName);
		assertEquals(1, mode.netNumSpectators);
	}

	// ---------------- netDrawGameRate (line 703) ----------------

	@Test
	void netDrawGameRateZeroStartTimeDrawsNothing() {
		netplay(false);
		engine.gameStarted = true;
		engine.startTime = 0;
		mode.netDrawGameRate(engine);
		assertEquals(0, receiver.drawn.size());
	}

	// ---------------- netDrawSpectatorsCount (lines 735/737) ----------------

	@Test
	void netDrawSpectatorsCountShowsRankingPrompt() {
		netplay(false);
		mode.rankingViewOK = true;
		engine.stat = GameEngine.Status.SETTING;
		mode.netDrawSpectatorsCount(engine, 0, 0); // y2 = 2, no clamp
		assertTrue(receiver.drawn.stream().anyMatch(s -> s.startsWith("score:D(")));
	}

	@Test
	void netDrawSpectatorsCountClampsPromptRow() {
		netplay(false);
		mode.rankingViewOK = true;
		engine.stat = GameEngine.Status.SETTING;
		mode.netDrawSpectatorsCount(engine, 0, 25); // y2 = 27 -> clamped to 24
		assertTrue(receiver.drawn.stream().anyMatch(s -> s.startsWith("score:D(")));
	}

	// ---------------- netDrawResultStatus (line 783) ----------------

	@Test
	void netDrawResultStatusWatchModeDrawsNothing() {
		netplay(true);
		mode.netReplaySendStatus = 2;
		mode.netDrawResultStatus(engine, 0, 10);
		assertEquals(0, receiver.drawn.size());
	}

	@Test
	void netDrawResultStatusSentShowsRetryPrompt() {
		mode.receiver = receiver; // playerInit normally wires this
		netplay(false);
		mode.netReplaySendStatus = 2;
		mode.netDrawResultStatus(engine, 0, 10);
		assertTrue(receiver.drawn.contains("menu:A: RETRY"));
	}

	@Test
	void netDrawResultStatusBeforeSendDrawsNothing() {
		netplay(false);
		mode.netReplaySendStatus = 0;
		mode.netDrawResultStatus(engine, 0, 10);
		assertEquals(0, receiver.drawn.size());
	}

	// ---------------- netSendPieceMovement (lines 796/803) ----------------

	@Test
	void netSendPieceMovementManualLockWithNoPreviousPiece() {
		engine.nowPieceObject = null;
		engine.manualLock = true;
		mode.netPrevPieceID = Piece.PIECE_NONE;

		assertTrue(mode.netSendPieceMovement(engine, false));
		assertEquals(Piece.PIECE_NONE, mode.netPrevPieceID);
		assertTrue(sentJoined().contains("game\tpiece"));
	}

	@Test
	void netSendPieceMovementDetectsEachDelta() {
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.nowPieceX = 4;
		engine.nowPieceY = 10;
		engine.nowPieceObject.direction = 0;
		mode.netPrevPieceID = Piece.PIECE_T;
		mode.netPrevPieceX = 4;
		mode.netPrevPieceY = 10;
		mode.netPrevPieceDir = 0;

		// baseline: nothing changed, no force
		assertFalse(mode.netSendPieceMovement(engine, false));

		// X delta only (id equal)
		engine.nowPieceX = 5;
		assertTrue(mode.netSendPieceMovement(engine, false));
		assertEquals(5, mode.netPrevPieceX);

		// Y delta only (id and X equal)
		engine.nowPieceY = 11;
		assertTrue(mode.netSendPieceMovement(engine, false));
		assertEquals(11, mode.netPrevPieceY);

		// direction delta only
		engine.nowPieceObject.direction = 1;
		assertTrue(mode.netSendPieceMovement(engine, false));
		assertEquals(1, mode.netPrevPieceDir);

		// nothing changed but forceSend requested
		assertTrue(mode.netSendPieceMovement(engine, true));
	}

	// ---------------- netRecvPieceMovement (line 850) ----------------

	@Test
	void netRecvPieceMovementKeepsTerminalStats() {
		String[] msg = { "game", "0", "0", "piece", String.valueOf(Piece.PIECE_I),
				"3", "15", "0", "5", "0", "0", "false" };
		GameEngine.Status[] terminal = {
				GameEngine.Status.EXCELLENT, GameEngine.Status.GAMEOVER, GameEngine.Status.RESULT };
		for (GameEngine.Status stat : terminal) {
			engine.stat = stat;
			engine.gameActive = false;
			mode.netRecvPieceMovement(engine, msg);
			assertEquals(stat, engine.stat);
			assertFalse(engine.gameActive);
		}
	}

	// ---------------- netRecvNextAndHold (line 1017) ----------------

	@Test
	void netRecvNextAndHoldReallocatesShortArray() {
		engine.nextPieceArrayObject = new Piece[1];
		String[] msg = { "game", "0", "0", "next", "2", "false", "1;0;1", "2;1;2", "3;2;3" };
		mode.netRecvNextAndHold(engine, msg);
		assertEquals(2, engine.nextPieceArrayObject.length);
	}

	@Test
	void netRecvNextAndHoldReusesBigEnoughArray() {
		Piece[] existing = new Piece[5];
		engine.nextPieceArrayObject = existing;
		String[] msg = { "game", "0", "0", "next", "2", "false", "1;0;1", "2;1;2", "3;2;3" };
		mode.netRecvNextAndHold(engine, msg);
		assertSame(existing, engine.nextPieceArrayObject);
		assertEquals(5, engine.nextPieceArrayObject.length);
	}

	// ---------------- netOnUpdateNetPlayRanking (1039-1059) ----------------

	@Test
	void updateRankingInactiveIgnoresInput() {
		mode.netIsNetRankingDisplayMode = false;
		mode.netRankingCursor[0] = 0;
		press(Controller.BUTTON_UP);
		mode.netOnUpdateNetPlayRanking(engine, 0);
		assertEquals(0, mode.netRankingCursor[0]);
	}

	@Test
	void updateRankingNullPlaceArrayStillAllowsExit() {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingReady = new boolean[] { true, true };
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingPlace = null;
		manager.menuOnly = true;
		press(Controller.BUTTON_B);
		mode.netOnUpdateNetPlayRanking(engine, 0);
		assertFalse(mode.netIsNetRankingDisplayMode);
		assertFalse(manager.menuOnly);
	}

	@Test
	void updateRankingNullSidePlaceStillTogglesView() {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingReady = new boolean[] { true, true };
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingPlace[0] = null;
		mode.netRankingView = 0;
		press(Controller.BUTTON_LEFT);
		mode.netOnUpdateNetPlayRanking(engine, 0);
		assertEquals(1, mode.netRankingView);
	}

	@Test
	void updateRankingCursorUpWithoutWrap() {
		setupRanking(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 2, 0);
		mode.netRankingCursor[0] = 1;
		press(Controller.BUTTON_UP);
		mode.netOnUpdateNetPlayRanking(engine, 0);
		assertEquals(0, mode.netRankingCursor[0]);
	}

	@Test
	void updateRankingCursorDownWithoutWrap() {
		setupRanking(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 2, 0);
		mode.netRankingCursor[0] = 0;
		press(Controller.BUTTON_DOWN);
		mode.netOnUpdateNetPlayRanking(engine, 0);
		assertEquals(1, mode.netRankingCursor[0]);
	}

	@Test
	void updateRankingDownloadFromDailyView() {
		setupRanking(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 1, 1); // daily side
		NetRoomInfo room = new NetRoomInfo();
		room.rated = false;
		room.ruleName = "R";
		mode.netCurrentRoomInfo = room;
		manager.menuOnly = true;
		press(Controller.BUTTON_A);

		mode.netOnUpdateNetPlayRanking(engine, 0);

		assertTrue(sentJoined().startsWith("spdownload"));
		assertFalse(mode.netIsNetRankingDisplayMode);
		assertFalse(manager.menuOnly);
	}

	// ---------------- netOnRenderNetPlayRanking (1089-1223) ----------------

	@Test
	void renderRankingInactiveDrawsNothing() {
		mode.netIsNetRankingDisplayMode = false;
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		assertEquals(0, receiver.drawn.size());
	}

	@Test
	void renderRankingSecondRowNotSelectedForAllTypes() {
		int[] types = {
				NetSPRecord.RANKINGTYPE_GENERIC_SCORE, NetSPRecord.RANKINGTYPE_GENERIC_TIME,
				NetSPRecord.RANKINGTYPE_SCORERACE, NetSPRecord.RANKINGTYPE_DIGRACE,
				NetSPRecord.RANKINGTYPE_ULTRA, NetSPRecord.RANKINGTYPE_COMBORACE,
				NetSPRecord.RANKINGTYPE_DIGCHALLENGE, NetSPRecord.RANKINGTYPE_TIMEATTACK };
		for (int type : types) {
			receiver.drawn.clear();
			setupRanking(type, 2, 0); // cursor stays on row 0, so row 1 renders unselected
			mode.netOnRenderNetPlayRanking(engine, 0, receiver);
			assertTrue(receiver.drawn.contains("menu:b"), "cursor marker for type " + type);
			assertTrue(receiver.drawn.contains("ttfmenu:P1"), "second row name for type " + type);
		}
	}

	@Test
	void renderRankingDailyViewShowsDailyTitle() {
		setupRanking(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 2, 1);
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		assertTrue(receiver.drawn.stream().anyMatch(s -> s.startsWith("menu:DAILY RANKING (")));
		assertTrue(receiver.drawn.stream().anyMatch(s -> s.contains("LEFT/RIGHT:ALL-TIME")));
	}

	@Test
	void renderRankingFullPageDoesNotClampEndIndex() {
		setupRanking(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 20, 0);
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		// all 20 rows rendered: 20 TTF name cells
		assertEquals(20, receiver.drawn.stream().filter(s -> s.startsWith("ttfmenu:P")).count());
	}

	@Test
	void renderRankingUnknownTypeSkipsHeaderAndRowBody() {
		setupRanking(999, 1, 0);
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		// rank number is drawn, but no type-specific columns
		assertTrue(receiver.drawn.contains("menu:  1"));
		assertFalse(receiver.drawn.contains("ttfmenu:P0"));
	}

	@Test
	void renderRankingNegativeCursorSkipsDateLine() {
		setupRanking(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 1, 0);
		mode.netRankingCursor[0] = -1;
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		assertFalse(receiver.drawn.stream().anyMatch(s -> s.startsWith("menu:DATE:")));
	}

	@Test
	void renderRankingEmptyDateListSkipsDateLine() {
		setupRanking(NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 1, 0);
		mode.netRankingDate[0].clear();
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		assertFalse(receiver.drawn.stream().anyMatch(s -> s.startsWith("menu:DATE:")));
	}

	@Test
	void renderRankingNoDataDailyView() {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { false, true };
		mode.netRankingReady = new boolean[] { false, false };
		mode.netRankingView = 1;
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		assertTrue(receiver.drawn.contains("menu:NO DATA"));
		assertTrue(receiver.drawn.contains("menu:DAILY RANKING"));
	}

	@Test
	void renderRankingLoadingWhenSidePlaceNull() {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingReady = new boolean[] { true, false }; // ready but data slot missing
		mode.netRankingPlace[0] = null;
		mode.netRankingView = 0;
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		assertTrue(receiver.drawn.contains("menu:LOADING..."));
	}

	@Test
	void renderRankingLoadingWhenPlaceArrayNull() {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingReady = new boolean[] { false, false };
		mode.netRankingPlace = null;
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		assertTrue(receiver.drawn.contains("menu:LOADING..."));
	}

	@Test
	void renderRankingSilentWhenNotReadyButDataPresent() {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingReady = new boolean[] { false, false };
		mode.netRankingPlace[0] = new LinkedList<>();
		mode.netRankingView = 0;
		mode.netOnRenderNetPlayRanking(engine, 0, receiver);
		assertEquals(0, receiver.drawn.size());
	}

	// ---------------- netEnterNetPlayRankingScreen (line 1234) ----------------

	@Test
	void enterRankingScreenWithNullPlaceArray() {
		mode.netRankingPlace = null;
		NetRoomInfo room = new NetRoomInfo();
		room.rated = false;
		mode.netCurrentRoomInfo = room;

		mode.netEnterNetPlayRankingScreen(engine, 0, 0);

		assertTrue(mode.netIsNetRankingDisplayMode);
		assertTrue(manager.menuOnly);
		assertEquals(2, client.sent.size());
		assertTrue(client.sent.get(0).startsWith("spranking"));
		assertTrue(client.sent.get(1).startsWith("spranking"));
	}

	// ---------------- netRecvNetPlayRanking (line 1338) ----------------

	@Test
	void recvRankingShortMessageIgnored() {
		String[] msg = { "spranking", "", "", "" }; // length 4: neither branch
		mode.netRecvNetPlayRanking(engine, msg);
		assertFalse(mode.netRankingNoDataFlag[0]);
		assertFalse(mode.netRankingNoDataFlag[1]);
		assertFalse(mode.netRankingReady[0]);
		assertFalse(mode.netRankingReady[1]);
	}
}
