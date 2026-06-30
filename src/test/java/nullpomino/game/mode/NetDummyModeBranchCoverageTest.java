package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Calendar;
import java.util.LinkedList;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetSPRecord;
import nullpomino.game.net.NetUtil;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link NetDummyMode} targeting the negative/false
 * arms of multi-condition guards and the less-travelled message-handler and
 * render branches that the existing NetDummyMode* suites leave uncovered.
 *
 * <p>The net client is a disconnected {@link NetPlayerClient}; its
 * {@code send()} swallows the null-socket error so the call-site branch still
 * executes.</p>
 */
class NetDummyModeBranchCoverageTest {

	/** EventReceiver whose skin is sticky, so netSendField takes the attr path. */
	private static final class StickyReceiver extends EventReceiver {
		@Override public boolean isStickySkin(int skin) { return true; }
	}

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
		mode.netRankingRank = new int[2];
		mode.netRankingRank[0] = -1;
		mode.netRankingRank[1] = -1;
	}

	private void setupMinimalNetLobby() throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		NetPlayerClient client = new NetPlayerClient();
		setField(lobby, "netPlayerClient", client);
		setField(mode, "netLobby", lobby);
	}

	private NetPlayerClient lobbyClient() throws Exception {
		NetLobbyFrame lobby = (NetLobbyFrame) getField(mode, "netLobby");
		return (NetPlayerClient) getField(lobby, "netPlayerClient");
	}

	// ================================================================
	//  netUpdatePlayerExist — both seatID inner branches (L668, L671, L673)
	// ================================================================

	@Test
	void netUpdatePlayerExistCountsSpectatorAndPlayerName() throws Exception {
		setupMinimalNetLobby();
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 5;
		setField(mode, "netCurrentRoomInfo", room);

		NetPlayerClient client = lobbyClient();
		// "me" so getYourPlayerInfo() is non-null and shares roomID 5
		NetPlayerInfo me = new NetPlayerInfo();
		me.uid = 1; me.roomID = 5; me.seatID = 0; me.strName = "HOST";
		// a spectator in the same room
		NetPlayerInfo spec = new NetPlayerInfo();
		spec.uid = 2; spec.roomID = 5; spec.seatID = -1; spec.strName = "WATCHER";
		client.getPlayerInfoList().add(me);
		client.getPlayerInfoList().add(spec);
		setField(client, "playerUID", 1);

		mode.netUpdatePlayerExist();

		assertEquals("HOST", getField(mode, "netPlayerName"),
				"seatID==0 player should populate netPlayerName");
		assertEquals(1, ((Integer) getField(mode, "netNumSpectators")).intValue(),
				"seatID==-1 player should increment netNumSpectators");
	}

	@Test
	void netUpdatePlayerExistRoomIdMinusOneSkipsLoop() throws Exception {
		setupMinimalNetLobby();
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = -1; // guard at L668 short-circuits
		setField(mode, "netCurrentRoomInfo", room);

		mode.netUpdatePlayerExist();

		assertEquals(0, ((Integer) getField(mode, "netNumSpectators")).intValue());
		assertEquals("", getField(mode, "netPlayerName"));
	}

	// ================================================================
	//  netOnJoin — ruleLock true branch (L639, L641)
	// ================================================================

	@Test
	void netOnJoinWithRuleLockCopiesRule() throws Exception {
		setupMinimalNetLobby();
		NetLobbyFrame lobby = (NetLobbyFrame) getField(mode, "netLobby");

		RuleOptions locked = new RuleOptions();
		locked.strRuleName = "LOCKED_RULE";
		locked.strRandomizer = "";
		locked.strWallkick = "";
		setField(lobby, "ruleOptLock", locked);

		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 7;
		room.ruleLock = true;

		// our own info as a player so netIsWatch resolves to false
		NetPlayerInfo me = new NetPlayerInfo();
		me.uid = 1; me.roomID = 7; me.seatID = 0;
		NetPlayerClient client = lobbyClient();
		client.getPlayerInfoList().add(me);
		setField(client, "playerUID", 1);

		mode.netOnJoin(lobby, client, room);

		assertEquals(room, getField(mode, "netCurrentRoomInfo"));
		assertEquals("LOCKED_RULE", engine.ruleopt.strRuleName,
				"ruleLock path should copy the locked rule into the engine");
		assertFalse((Boolean) getField(mode, "netIsWatch"));
	}

	// ================================================================
	//  netlobbyOnMessage playerlogout — both arms of L494
	// ================================================================

	@Test
	void playerlogoutSameRoomTriggersUpdate() throws Exception {
		setupMinimalNetLobby();
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 3;
		setField(mode, "netCurrentRoomInfo", room);

		// message[1] is parsed into a NetPlayerInfo; roomID is field index 64.
		NetPlayerInfo logoutInfo = new NetPlayerInfo();
		logoutInfo.roomID = 3;
		String[] message = {"playerlogout", logoutInfo.exportString()};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	@Test
	void playerlogoutDifferentRoomDoesNotMatch() throws Exception {
		setupMinimalNetLobby();
		NetRoomInfo room = new NetRoomInfo();
		room.roomID = 3;
		setField(mode, "netCurrentRoomInfo", room);

		NetPlayerInfo logoutInfo = new NetPlayerInfo();
		logoutInfo.roomID = 999; // different room -> false arm of L494
		String[] message = {"playerlogout", logoutInfo.exportString()};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
	}

	// ================================================================
	//  netlobbyOnMessage dead — L515 false arm (already GAMEOVER)
	// ================================================================

	@Test
	void deadInWatchModeAlreadyGameOverSkipsStatusChange() throws Exception {
		setupMinimalNetLobby();
		setField(mode, "netIsWatch", true);
		mode.owner = manager;
		engine.stat = GameEngine.Status.GAMEOVER; // L515 condition false
		String[] message = {"dead"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	// ================================================================
	//  netlobbyOnMessage game cursor — L553 false arm (stat != SETTING)
	// ================================================================

	@Test
	void gameCursorWhenNotSettingDoesNotChangeCursor() throws Exception {
		setupMinimalNetLobby();
		setField(mode, "netIsWatch", true);
		mode.owner = manager;
		engine.stat = GameEngine.Status.MOVE; // not SETTING
		mode.menuCursor = 4;
		String[] message = {"game", "0", "0", "cursor", "9"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		assertEquals(4, mode.menuCursor, "cursor must be unchanged when not in SETTING");
	}

	// ================================================================
	//  netlobbyOnMessage ending — L581 false arm (staffrollEnable true)
	// ================================================================

	@Test
	void gameEndingWithStaffrollEnabledDoesNotEndGame() throws Exception {
		setupMinimalNetLobby();
		setField(mode, "netIsWatch", true);
		mode.owner = manager;
		engine.staffrollEnable = true; // L581 -> gameEnded() skipped
		engine.ending = 0;
		String[] message = {"game", "0", "0", "ending"};
		assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, message));
		assertEquals(1, engine.ending);
		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);
	}

	// ================================================================
	//  netRecvPieceMovement — short message (L806 false) and
	//  stat in EXCELLENT/GAMEOVER/RESULT (L820 false arm)
	// ================================================================

	@Test
	void netRecvPieceMovementShortMessageNoBig() throws Exception {
		engine.field = new Field(10, 20, 4);
		engine.stat = GameEngine.Status.MOVE;
		// exactly 11 elements -> message.length > 11 is false -> pieceBig=false
		String[] message = {"game", "0", "0", "piece",
				String.valueOf(Piece.PIECE_I), "3", "15", "0", "5", "0", "0"};
		mode.netRecvPieceMovement(engine, message);
		assertNotNull(engine.nowPieceObject);
		assertFalse(engine.nowPieceObject.big);
	}

	@Test
	void netRecvPieceMovementResultStatLeavesStatUnchanged() throws Exception {
		engine.field = new Field(10, 20, 4);
		engine.stat = GameEngine.Status.RESULT; // L820 guard false -> no activation
		String[] message = {"game", "0", "0", "piece",
				String.valueOf(Piece.PIECE_I), "3", "15", "0", "5", "0", "0", "false"};
		mode.netRecvPieceMovement(engine, message);
		assertNotNull(engine.nowPieceObject);
		assertEquals(GameEngine.Status.RESULT, engine.stat,
				"piece movement should not reactivate the game when in RESULT");
	}

	// ================================================================
	//  netSendField — sticky skin true arm (L840)
	// ================================================================

	@Test
	void netSendFieldStickySkinSendsAttributes() throws Exception {
		// Rebuild the manager with a sticky receiver so isStickySkin returns true.
		NetDummyMode m = new NetDummyMode();
		GameManager mgr = new GameManager(new StickyReceiver());
		mgr.mode = m;
		m.modeInit(mgr);
		mgr.init();
		mgr.engine[0].init();
		GameEngine e = mgr.engine[0];
		e.createFieldIfNeeded();

		NetLobbyFrame lobby = new NetLobbyFrame();
		setField(lobby, "netPlayerClient", new NetPlayerClient());
		setField(m, "netLobby", lobby);

		assertDoesNotThrow(() -> m.netSendField(e));
	}

	// ================================================================
	//  netRecvField — fieldattr with stat==SETTING (L893 true)
	//  and short message (L896 false)
	// ================================================================

	@Test
	void netRecvFieldAttrFromSettingSwitchesToMove() throws Exception {
		engine.field = new Field(10, 20, 4);
		engine.stat = GameEngine.Status.SETTING;
		// length 6 -> message.length > 6 is false -> skip field data parse
		String[] message = {"game", "0", "0", "fieldattr", "0", "false"};
		mode.netRecvField(engine, message);
		assertEquals(GameEngine.Status.MOVE, engine.stat);
	}

	@Test
	void netRecvFieldNoAttrFromSettingSwitchesToMove() throws Exception {
		engine.field = new Field(10, 20, 4);
		engine.stat = GameEngine.Status.SETTING;
		// length 6 -> > 5 true, > 7 false -> field.reset() branch + stat switch
		String[] message = {"game", "0", "0", "field", "0", "20"};
		mode.netRecvField(engine, message);
		assertEquals(GameEngine.Status.MOVE, engine.stat);
	}

	// ================================================================
	//  netRecvNextAndHold — i+6 >= message.length (L970 false) and
	//  reuse of existing nextPieceArrayObject (L987 false)
	// ================================================================

	@Test
	void netRecvNextAndHoldReusesExistingNextArray() throws Exception {
		engine.field = new Field(10, 20, 4);
		// Pre-existing array big enough -> L987 condition false (reuse).
		engine.nextPieceArrayObject = new Piece[5];
		// maxNext=2 but only provide hold + 1 next -> the loop's later i values
		// hit "i + 6 < message.length" == false (L970).
		String[] message = {"game", "0", "0", "next", "2", "false",
				Piece.PIECE_O + ";0;0",
				Piece.PIECE_I + ";1;1"};
		mode.netRecvNextAndHold(engine, message);
		assertTrue(engine.isNextVisible);
		assertEquals(5, engine.nextPieceArrayObject.length,
				"existing array of sufficient length should be reused");
	}

	// ================================================================
	//  netOnUpdateNetPlayRanking — cursor wrap branches (L1016, L1022)
	// ================================================================

	@Test
	void rankingCursorUpWrapsToEnd() throws Exception {
		enableRankingUpdate(3);
		mode.netRankingCursor = new int[] { 0, 0 }; // up from 0 -> wrap
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.netOnUpdateNetPlayRanking(engine, 0);
		assertEquals(2, mode.netRankingCursor[0],
				"up at top should wrap to size-1");
	}

	@Test
	void rankingCursorDownWrapsToZero() throws Exception {
		enableRankingUpdate(3);
		mode.netRankingCursor = new int[] { 2, 0 }; // down from last -> wrap to 0
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.netOnUpdateNetPlayRanking(engine, 0);
		assertEquals(0, mode.netRankingCursor[0],
				"down past the last entry should wrap to 0");
	}

	@Test
	void rankingViewTogglesToDailyOnRight() throws Exception {
		enableRankingUpdate(2);
		mode.netRankingView = 0;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;
		mode.netOnUpdateNetPlayRanking(engine, 0);
		assertEquals(1, mode.netRankingView, "right should switch view to daily");
	}

	// ================================================================
	//  netDrawGameRate — color thresholds < 0.9 and < 0.8 (L716, L717)
	// ================================================================

	@Test
	void netDrawGameRateLowRateRunning() throws Exception {
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		mode.owner = manager;
		engine.gameStarted = true;
		// Force a very low gamerate: replayTimer small relative to elapsed time.
		engine.startTime = System.nanoTime() - 1_000_000_000L;
		engine.endTime = 0;
		engine.replayTimer = 1; // -> gamerate well below 0.8
		assertDoesNotThrow(() -> mode.netDrawGameRate(engine));
	}

	@Test
	void netDrawGameRateFromEndTimeLowRate() throws Exception {
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		mode.owner = manager;
		engine.gameStarted = true;
		engine.startTime = System.nanoTime() - 1_000_000_000L;
		engine.endTime = System.nanoTime();
		engine.statistics.gamerate = 0.85f; // < 0.9 but not < 0.8 (hits orange only)
		assertDoesNotThrow(() -> mode.netDrawGameRate(engine));
	}

	// ================================================================
	//  netRecvNetPlayRanking — daily (d=1) and no-data daily branches
	// ================================================================

	@Test
	void netRecvNetPlayRankingDailyGenericScore() throws Exception {
		String[] message = {"spranking", "", "", "", "true", // isDaily -> d=1
				String.valueOf(NetSPRecord.RANKINGTYPE_GENERIC_SCORE), "1",
				"0," + NetUtil.urlEncode("DailyPlayer") + ",2024/01/01 12:00:00,1.0,1000,10,500"};
		mode.netRecvNetPlayRanking(engine, message);
		assertTrue(mode.netRankingReady[1], "daily ranking row should be ready");
		assertFalse(mode.netRankingReady[0], "all-time row should be untouched");
	}

	@Test
	void netRecvNetPlayRankingDailyNoData() throws Exception {
		String[] message = {"spranking", "", "", "", "true"}; // length 5 -> > 4 only
		mode.netRecvNetPlayRanking(engine, message);
		assertTrue(mode.netRankingNoDataFlag[1], "daily no-data flag should be set");
	}

	// ================================================================
	//  netOnRenderNetPlayRanking — non-selected row + TimeAttack rollclear
	//  colors + null-date branch (L1096 false, L1144/L1145, L1158 false)
	// ================================================================

	@Test
	void renderRankingTimeAttackMultiRowRollclearColors() throws Exception {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingReady = new boolean[] { true, true };
		mode.netRankingType = NetSPRecord.RANKINGTYPE_TIMEATTACK;
		mode.netRankingView = 0;
		mode.netRankingCursor = new int[] { 0, 0 }; // row 1,2 -> i != cursor
		mode.netRankingMyRank = new int[] { -1, -1 };

		setField(mode, "netRankingPlace", listArr(0, 1, 2));
		setField(mode, "netRankingName", strArr("A", "B", "C"));
		setField(mode, "netRankingLines", listArr(10, 20, 30));
		setField(mode, "netRankingTime", listArr(500, 600, 700));
		setField(mode, "netRankingPPS", floatArr(1.0f, 2.0f, 3.0f));
		setField(mode, "netRankingRollclear", listArr(0, 1, 2)); // none/green/orange
		// date list has a null entry to hit the calendar==null branch (L1158)
		setField(mode, "netRankingDate", calArrWithNull());
		setField(mode, "netRankingGamerate", floatArr(0.0f, 1.0f, 1.0f)); // gamerate 0 -> UNKNOWN

		assertDoesNotThrow(() -> mode.netOnRenderNetPlayRanking(engine, 0, receiver));
	}

	// ================================================================
	//  onResult retry — A pressed, no spectators (L399 false arm)
	// ================================================================

	@Test
	void onResultRetryWithoutSpectatorsResets() throws Exception {
		setupMinimalNetLobby();
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		mode.owner = manager;
		manager.replayMode = false;
		mode.netReplaySendStatus = 2; // already sent -> retry allowed
		setField(mode, "netNumSpectators", 0);
		setField(mode, "netForceSendMovements", false); // L399 -> false arm
		engine.allowTextRenderByReceiver = true;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		assertTrue(mode.onResult(engine, 0));
	}

	@Test
	void onResultRetryWithSpectatorsSendsRetry() throws Exception {
		setupMinimalNetLobby();
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		mode.owner = manager;
		manager.replayMode = false;
		mode.netReplaySendStatus = 2;
		setField(mode, "netNumSpectators", 2); // L399 -> true arm
		engine.allowTextRenderByReceiver = true;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		assertTrue(mode.onResult(engine, 0));
	}

	// ================================================================
	//  updateCursor — net cursor signal (L431) with spectators
	// ================================================================

	@Test
	void updateCursorSignalsCursorChangeInNetplay() throws Exception {
		setupMinimalNetLobby();
		setField(mode, "netIsNetPlay", true);
		setField(mode, "netIsWatch", false);
		setField(mode, "netForceSendMovements", true);
		mode.owner = manager;
		engine.stat = GameEngine.Status.SETTING;
		// DOWN repeat key drives super.updateCursor AND the net signal branch.
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		assertDoesNotThrow(() -> mode.updateCursor(engine, 5, 0));
	}

	@Test
	void updateCursorWatchModeReturnsZero() throws Exception {
		setField(mode, "netIsWatch", true);
		assertEquals(0, mode.updateCursor(engine, 5, 0));
	}

	// ================================================================
	//  Ranking-update helpers
	// ================================================================

	private void enableRankingUpdate(int rows) throws Exception {
		mode.netIsNetRankingDisplayMode = true;
		mode.netRankingNoDataFlag = new boolean[] { false, false };
		mode.netRankingReady = new boolean[] { true, true };
		mode.netRankingType = NetSPRecord.RANKINGTYPE_GENERIC_SCORE;
		mode.netRankingView = 0;
		mode.netRankingMyRank = new int[] { -1, -1 };
		mode.owner = manager;

		Integer[] places = new Integer[rows];
		for (int i = 0; i < rows; i++) places[i] = i;
		setField(mode, "netRankingPlace", listArr(places));
		String[] names = new String[rows];
		for (int i = 0; i < rows; i++) names[i] = "P" + i;
		setField(mode, "netRankingName", strArr(names));
	}

	// ================================================================
	//  Array builders (index 0 = all-time row populated)
	// ================================================================

	@SuppressWarnings("unchecked")
	private static LinkedList<Integer>[] listArr(Integer... values) {
		LinkedList<Integer>[] arr = new LinkedList[2];
		arr[0] = new LinkedList<>();
		for (Integer v : values) arr[0].add(v);
		arr[1] = new LinkedList<>();
		return arr;
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<String>[] strArr(String... values) {
		LinkedList<String>[] arr = new LinkedList[2];
		arr[0] = new LinkedList<>();
		for (String v : values) arr[0].add(v);
		arr[1] = new LinkedList<>();
		return arr;
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<Float>[] floatArr(Float... values) {
		LinkedList<Float>[] arr = new LinkedList[2];
		arr[0] = new LinkedList<>();
		for (Float v : values) arr[0].add(v);
		arr[1] = new LinkedList<>();
		return arr;
	}

	@SuppressWarnings("unchecked")
	private static LinkedList<Calendar>[] calArrWithNull() {
		LinkedList<Calendar>[] arr = new LinkedList[2];
		arr[0] = new LinkedList<>();
		arr[0].add(null); // calendar == null branch
		arr[0].add(Calendar.getInstance());
		arr[0].add(Calendar.getInstance());
		arr[1] = new LinkedList<>();
		return arr;
	}

	// ================================================================
	//  Reflection helpers
	// ================================================================

	private static Object getField(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).get(obj);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		findField(obj.getClass(), name).set(obj, value);
	}

	private static void setField(Object obj, String name, boolean value) throws Exception {
		findField(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				java.lang.reflect.Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
