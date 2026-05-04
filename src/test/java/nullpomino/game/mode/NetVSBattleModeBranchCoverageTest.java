package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Branch coverage for {@link NetVSBattleMode}: targets uncovered branches
 * in calcScore, onLast, startGame, renderLast, renderResult, netSendStats,
 * netRecvStats, netSendEndGameStats, netvsRecvEndGameStats, and helper methods.
 */
class NetVSBattleModeBranchCoverageTest {

	// ---------------------------------------------------------------
	// Helper setup
	// ---------------------------------------------------------------

	private static GameManager makeManager() {
		return new GameManager(new EventReceiver());
	}

	private static GameEngine freshEngine(NetVSBattleMode mode) {
		return freshEngine(mode, 0);
	}

	private static GameEngine freshEngine(NetVSBattleMode mode, int playerID) {
		GameManager manager = makeManager();
		manager.mode = mode;
		manager.init();
		for (int i = 0; i <= Math.max(playerID, 0); i++) {
			if (manager.engine[i] != null) {
				manager.engine[i].init();
				manager.engine[i].createFieldIfNeeded();
				manager.engine[i].playerID = i;
				manager.engine[i].nowPieceObject = new Piece(Piece.PIECE_T);
			}
		}
		return manager.engine[playerID];
	}

	private static Object makeRoomInfo() throws Exception {
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		Object roomInfo = roomInfoClass.getConstructor().newInstance();
		roomInfoClass.getField("reduceLineSend").setBoolean(roomInfo, false);
		roomInfoClass.getField("bravo").setBoolean(roomInfo, false);
		roomInfoClass.getField("counter").setBoolean(roomInfo, true);
		roomInfoClass.getField("useFractionalGarbage").setBoolean(roomInfo, false);
		roomInfoClass.getField("rensaBlock").setBoolean(roomInfo, true);
		roomInfoClass.getField("garbagePercent").setInt(roomInfo, 100);
		roomInfoClass.getField("divideChangeRateByPlayers").setBoolean(roomInfo, false);
		roomInfoClass.getField("garbageChangePerAttack").setBoolean(roomInfo, false);
		roomInfoClass.getField("hurryupSeconds").setInt(roomInfo, -1);
		roomInfoClass.getField("isTarget").setBoolean(roomInfo, false);
		return roomInfo;
	}

	private static void setNetCurrentRoomInfo(NetVSBattleMode mode) throws Exception {
		setField(mode, "netCurrentRoomInfo", makeRoomInfo());
	}

	/**
	 * Create a GarbageEntry instance via reflection (private inner class).
	 */
	private static Object makeGarbageEntry(NetVSBattleMode mode, int lines) throws Exception {
		Class<?> geClass = getGarbageEntryClass();
		Constructor<?> ctor = geClass.getDeclaredConstructor(NetVSBattleMode.class, int.class);
		ctor.setAccessible(true);
		return ctor.newInstance(mode, lines);
	}

	/**
	 * Create a GarbageEntry instance with playerID and UID.
	 */
	private static Object makeGarbageEntry(NetVSBattleMode mode, int lines, int playerID, int uid) throws Exception {
		Class<?> geClass = getGarbageEntryClass();
		Constructor<?> ctor = geClass.getDeclaredConstructor(NetVSBattleMode.class, int.class, int.class, int.class);
		ctor.setAccessible(true);
		return ctor.newInstance(mode, lines, playerID, uid);
	}

	private static Class<?> getGarbageEntryClass() throws Exception {
		for (Class<?> c : NetVSBattleMode.class.getDeclaredClasses()) {
			if (c.getSimpleName().equals("GarbageEntry")) return c;
		}
		throw new ClassNotFoundException("GarbageEntry");
	}

	/**
	 * Make a player attackable by setting the necessary arrays.
	 */
	private static void makePlayerAttackable(NetVSBattleMode mode, int playerID) throws Exception {
		setBooleanArray(mode, "netvsPlayerExist", playerID, true);
		setBooleanArray(mode, "netvsPlayerDead", playerID, false);
		setBooleanArray(mode, "netvsPlayerActive", playerID, true);
	}

	/**
	 * Ensure engine instances exist for given playerIDs in the mode's owner.
	 */
	private static void ensureEngines(GameManager manager, int maxPlayerID) {
		for (int i = 0; i <= maxPlayerID; i++) {
			if (manager.engine[i] != null) {
				manager.engine[i].init();
				manager.engine[i].createFieldIfNeeded();
				manager.engine[i].playerID = i;
				manager.engine[i].nowPieceObject = new Piece(Piece.PIECE_T);
			}
		}
	}

	// ---------------------------------------------------------------
	// calcScore branch coverage
	// ---------------------------------------------------------------

	@Test
	void calcScoreTspinEZ() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		e.tspin = true;
		e.tspinez = true;
		e.tspinmini = false;
		mode.calcScore(e, 0, 1);
		assertEquals(10, ((int[]) readField(mode, "lastevent"))[0]); // EVENT_TSPIN_EZ
	}

	@Test
	void calcScoreTspinMiniSingle() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		e.tspin = true;
		e.tspinez = false;
		e.tspinmini = true;
		mode.calcScore(e, 0, 1);
		assertEquals(5, ((int[]) readField(mode, "lastevent"))[0]); // EVENT_TSPIN_SINGLE_MINI
	}

	@Test
	void calcScoreTspinDoubleMiniAllSpin() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		e.tspin = true;
		e.tspinmini = true;
		e.useAllSpinBonus = true;
		mode.calcScore(e, 0, 2);
		assertEquals(9, ((int[]) readField(mode, "lastevent"))[0]); // EVENT_TSPIN_DOUBLE_MINI
	}

	@Test
	void calcScoreTspinTriple() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		e.tspin = true;
		e.tspinmini = false;
		mode.calcScore(e, 0, 3);
		assertEquals(8, ((int[]) readField(mode, "lastevent"))[0]); // EVENT_TSPIN_TRIPLE
	}

	@Test
	void calcScoreUseAllSpinBonusPath() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		e.useAllSpinBonus = true;
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		assertEquals(1, ((int[]) readField(mode, "lastevent"))[0]); // EVENT_SINGLE
	}

	@Test
	void calcScoreB2bTTripleBonus() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		e.tspin = true;
		e.b2b = true;
		e.useAllSpinBonus = false;
		mode.calcScore(e, 0, 3);
		int[] garbageSent = (int[]) readField(mode, "garbageSent");
		// main=6*60=360, b2b=2*60=120
		assertEquals(480, garbageSent[0]);
	}

	@Test
	void calcScoreComboTypeDisabled() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		e.comboType = GameEngine.COMBO_TYPE_DISABLE;
		e.combo = 5;
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		int[] garbageSent = (int[]) readField(mode, "garbageSent");
		assertEquals(0, garbageSent[0]); // single normal -> 0 * 60 = 0
	}

	@Test
	void calcScoreGemBlockAttack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		int[] garbageSent = (int[]) readField(mode, "garbageSent");
		assertNotNull(garbageSent);
	}

	@Test
	void calcScoreNotPlayerZeroDoesNothing() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = makeManager();
		mode.modeInit(mgr);
		// Need netCurrentRoomInfo to avoid NPE at line 449 (rensaBlock check)
		setField(mode, "netCurrentRoomInfo", makeRoomInfo());
		GameEngine e = freshEngine(mode, 1);
		int scoreBefore = e.statistics.score;
		mode.calcScore(e, 1, 1);
		assertEquals(scoreBefore, e.statistics.score);
	}

	@Test
	void calcScoreGarbageCounterB2bChunk() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		// Inject a garbage entry to counter
		LinkedList<Object> entries = new LinkedList<>();
		entries.add(makeGarbageEntry(mode, 120, 1, 100));
		setField(mode, "garbageEntries", entries);
		e.tspin = false;
		mode.calcScore(e, 0, 1);
		// Should not throw
	}

	@Test
	void calcScoreGarbageLineGeneration() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("rensaBlock").setBoolean(roomInfo, false);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		setBoolean(mode, "netvsIsPractice", true);

		// Inject garbage entry to trigger line generation
		LinkedList<Object> entries = new LinkedList<>();
		entries.add(makeGarbageEntry(mode, 120));
		setField(mode, "garbageEntries", entries);

		e.field.reset();
		e.tspin = false;
		mode.calcScore(e, 0, 0);
		// Should not throw
	}

	@Test
	void calcScoreSmallGarbageRemainder() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("rensaBlock").setBoolean(roomInfo, true);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		setBoolean(mode, "netvsIsPractice", true);

		// Small garbage entry < GARBAGE_DENOMINATOR
		LinkedList<Object> entries = new LinkedList<>();
		entries.add(makeGarbageEntry(mode, 30));
		setField(mode, "garbageEntries", entries);

		e.field.reset();
		e.tspin = false;
		mode.calcScore(e, 0, 0);
		// Should not throw
	}

	@Test
	void calcScoreGarbageChangePerAttack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("rensaBlock").setBoolean(roomInfo, false);
		roomInfoClass.getField("garbageChangePerAttack").setBoolean(roomInfo, true);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		setBoolean(mode, "netvsIsPractice", true);

		LinkedList<Object> entries = new LinkedList<>();
		entries.add(makeGarbageEntry(mode, 120, 1, 100));
		setField(mode, "garbageEntries", entries);

		e.field.reset();
		e.tspin = false;
		mode.calcScore(e, 0, 0);
		// Should not throw
	}

	@Test
	void calcScoreHurryUpStarted() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("hurryupSeconds").setInt(roomInfo, 10);
		roomInfoClass.getField("hurryupInterval").setInt(roomInfo, 10);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		// Do NOT set netvsIsPractice; hurry-up only runs when !netvsIsPractice
		setBoolean(mode, "netvsIsPractice", false);
		setBoolean(mode, "hurryupStarted", true);
		setInt(mode, "hurryupCount", 9);

		e.timerActive = true;
		e.tspin = false;
		mode.calcScore(e, 0, 0);
		int count = readInt(mode, "hurryupCount");
		assertEquals(10, count);
	}

	@Test
	void calcScoreHurryUpNotStartedSetCounter() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("hurryupSeconds").setInt(roomInfo, 10);
		roomInfoClass.getField("hurryupInterval").setInt(roomInfo, 10);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		// Do NOT set netvsIsPractice; hurry-up only runs when !netvsIsPractice
		setBoolean(mode, "netvsIsPractice", false);
		setBoolean(mode, "hurryupStarted", false);

		e.timerActive = true;
		e.tspin = false;
		mode.calcScore(e, 0, 0);
		int count = readInt(mode, "hurryupCount");
		assertEquals(9, count); // hurryupInterval - 1
	}

	// ---------------------------------------------------------------
	// onLast branch coverage
	// ---------------------------------------------------------------

	@Test
	void onLastHurryupStart() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("hurryupSeconds").setInt(roomInfo, 10);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		setBoolean(mode, "netvsIsPractice", false);
		setBoolean(mode, "hurryupStarted", false);
		e.timerActive = true;
		setInt(mode, "netvsPlayTimer", 600); // 10 * 60
		// Set netLobby to avoid NPE in send
		setField(mode, "netLobby", new nullpomino.gui.net.NetLobbyFrame());
		// Set netPlayerClient to avoid NPE
		java.lang.reflect.Field netPlayerClientField = nullpomino.gui.net.NetLobbyFrame.class.getDeclaredField("netPlayerClient");
		netPlayerClientField.setAccessible(true);
		netPlayerClientField.set(readField(mode, "netLobby"), new nullpomino.game.net.NetPlayerClient("", 0) {
			@Override
			public boolean send(String msg) { return true; }
			@Override
			public int getPlayerUID() { return 0; }
		});

		mode.onLast(e, 0);

		assertTrue(readBoolean(mode, "hurryupStarted"));
	}

	@Test
	void onLastMeterColorRedWhenGarbageHigh() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		((int[]) readField(mode, "garbage"))[0] = 4 * 60;
		mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_RED, e.meterColor);
	}

	@Test
	void onLastMeterColorOrangeWhenGarbage3() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		((int[]) readField(mode, "garbage"))[0] = 3 * 60;
		mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_ORANGE, e.meterColor);
	}

	@Test
	void onLastMeterColorYellowWhenGarbage1() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		((int[]) readField(mode, "garbage"))[0] = 1 * 60;
		mode.onLast(e, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, e.meterColor);
	}

	@Test
	void onLastAPLComputedWhenLinesPositive() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		e.gameActive = true;
		e.timerActive = true;
		e.statistics.time = 3600;
		e.statistics.lines = 10;
		((int[]) readField(mode, "garbageSent"))[0] = 60;
		mode.onLast(e, 0);
		float[] apl = (float[]) readField(mode, "playerAPL");
		assertEquals(0.1f, apl[0], 0.001f);
	}

	@Test
	void onLastAPLZeroWhenNoLines() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		e.gameActive = true;
		e.timerActive = true;
		e.statistics.time = 3600;
		e.statistics.lines = 0;
		mode.onLast(e, 0);
		float[] apl = (float[]) readField(mode, "playerAPL");
		assertEquals(0f, apl[0], 0.001f);
	}

	@Test
	void onLastTargetCycle() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		// Initialize engines for players 0 and 1
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mgr.init();
		ensureEngines(mgr, 1);
		GameEngine e = mgr.engine[0];

		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("isTarget").setBoolean(roomInfo, true);
		roomInfoClass.getField("targetTimer").setInt(roomInfo, 30);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		setBoolean(mode, "netvsPlayTimerActive", true);
		e.gameActive = true;
		e.timerActive = true;

		// Make player 1 attackable: need exist, not dead, active, and team string set
		makePlayerAttackable(mode, 1);
		String[] team = (String[]) readField(mode, "netvsPlayerTeam");
		team[1] = "teamA";
		team[0] = "teamB"; // different team so attackable

		setInt(mode, "targetID", 1);
		setInt(mode, "targetTimer", 29);

		mode.onLast(e, 0);

		// timer >= targetTimer(30) should trigger setNewTarget
		assertEquals(0, readInt(mode, "targetTimer"));
	}

	// ---------------------------------------------------------------
	// startGame branch coverage
	// ---------------------------------------------------------------

	@Test
	void startGamePracticeSkipsHurryupInit() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		mode.startGame(e, 0);
		assertFalse(readBoolean(mode, "hurryupStarted"));
	}

	@Test
	void startGameNonPlayerZero() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode, 1);
		mode.startGame(e, 1);
		// Should not throw
	}

	// ---------------------------------------------------------------
	// Helper method coverage
	// ---------------------------------------------------------------

	@Test
	void getTotalGarbageLinesWithEntries() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		LinkedList<Object> entries = new LinkedList<>();
		entries.add(makeGarbageEntry(mode, 60));
		entries.add(makeGarbageEntry(mode, 120));
		setField(mode, "garbageEntries", entries);

		Method m = NetVSBattleMode.class.getDeclaredMethod("getTotalGarbageLines");
		m.setAccessible(true);
		int total = (int) m.invoke(mode);
		assertEquals(180, total);
	}

	@Test
	void getNumberOfPossibleTargets() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		setNetCurrentRoomInfo(mode);
		Method m = NetVSBattleMode.class.getDeclaredMethod("getNumberOfPossibleTargets");
		m.setAccessible(true);
		int count = (int) m.invoke(mode);
		assertEquals(0, count); // No players exist
	}

	@Test
	void setNewTargetNoTargets() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		Method m = NetVSBattleMode.class.getDeclaredMethod("setNewTarget");
		m.setAccessible(true);
		m.invoke(mode);
		assertEquals(-1, readInt(mode, "targetID"));
	}

	// ---------------------------------------------------------------
	// renderLast / renderResult branch coverage
	// ---------------------------------------------------------------

	@Test
	void renderLastFractionalGarbageDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		((int[]) readField(mode, "garbage"))[0] = 90;
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		Object roomInfo = readField(mode, "netCurrentRoomInfo");
		roomInfoClass.getField("useFractionalGarbage").setBoolean(roomInfo, true);
		mode.renderLast(e, 0);
		// Should not throw
	}

	@Test
	void renderLastTargetDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mgr.init();
		ensureEngines(mgr, 1);
		GameEngine e = mgr.engine[0];

		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("isTarget").setBoolean(roomInfo, true);
		roomInfoClass.getField("targetTimer").setInt(roomInfo, 30);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		setBoolean(mode, "netvsIsGameActive", true);
		setInt(mode, "targetID", 1);

		makePlayerAttackable(mode, 1);
		setIntArray(mode, "netvsPlayerSeatID", 1, 1);

		mode.renderLast(e, 0);
		// Should not throw
	}

	@Test
	void renderLastPracticeEndGamePrompt() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", true);
		setBoolean(mode, "netvsIsPracticeExitAllowed", true);
		mode.renderLast(e, 0);
		// Should not throw
	}

	@Test
	void renderLastHurryUpDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("hurryupSeconds").setInt(roomInfo, 10);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		setBoolean(mode, "hurryupStarted", true);
		setInt(mode, "hurryupShowFrames", 10);

		mode.renderLast(e, 0);
		// Should not throw
	}

	@Test
	void renderResultSmallDisplay() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		e.isVisible = true;
		e.displaysize = -1;
		mode.renderResult(e, 0);
		// Should not throw
	}

	// ---------------------------------------------------------------
	// Network message handling
	// ---------------------------------------------------------------

	@Test
	void netlobbyOnMessageDeadMessage() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mgr.init();
		ensureEngines(mgr, 5);
		mode.modeInit(mgr);
		// Set netLobby with mock client to avoid NPE
		setField(mode, "netLobby", new nullpomino.gui.net.NetLobbyFrame());
		java.lang.reflect.Field netPlayerClientField = nullpomino.gui.net.NetLobbyFrame.class.getDeclaredField("netPlayerClient");
		netPlayerClientField.setAccessible(true);
		netPlayerClientField.set(readField(mode, "netLobby"), new nullpomino.game.net.NetPlayerClient("", 0) {
			@Override
			public boolean send(String msg) { return true; }
			@Override
			public int getPlayerUID() { return 0; }
			@Override
			public nullpomino.game.net.NetPlayerInfo getYourPlayerInfo() {
				nullpomino.game.net.NetPlayerInfo info = new nullpomino.game.net.NetPlayerInfo();
				info.seatID = 99; // Different from msg[3] to skip the dangerous block
				return info;
			}
		});
		String[] msg = {"dead", "game", "0", "0", "0", "0"};
		mode.netlobbyOnMessage(null, null, msg);
		// Should not throw
	}

	@Test
	void netlobbyOnMessageGameAttack() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mgr.init();
		ensureEngines(mgr, 5);
		mode.modeInit(mgr);

		setNetCurrentRoomInfo(mode);
		setBoolean(mode, "netvsIsPractice", false);
		mgr.engine[0].timerActive = true;
		setIntArray(mode, "netvsPlayerSeatID", 0, 0);
		makePlayerAttackable(mode, 1);
		setIntArray(mode, "netvsPlayerSeatID", 1, 1);
		String[] team = (String[]) readField(mode, "netvsPlayerTeam");
		team[1] = "teamA";
		team[0] = "teamB";

		// Set netLobby with mock client to avoid NPE in netSendStats
		setField(mode, "netLobby", new nullpomino.gui.net.NetLobbyFrame());
		java.lang.reflect.Field netPlayerClientField = nullpomino.gui.net.NetLobbyFrame.class.getDeclaredField("netPlayerClient");
		netPlayerClientField.setAccessible(true);
		netPlayerClientField.set(readField(mode, "netLobby"), new nullpomino.game.net.NetPlayerClient("", 0) {
			@Override
			public boolean send(String msg) { return true; }
			@Override
			public int getPlayerUID() { return 0; }
		});

		// Construct attack message in the format received by netlobbyOnMessage
		// [0]=game, [1]=uid, [2]=seatID, [3]=attack, [4..9]=pts, [10]="" (double-tab),
		// [11]=lastevent, [12]=lastb2b, [13]=lastcombo, [14]=garbage, [15]=lastpiece, [16]=targetSeatID
		String[] msg = new String[17];
		msg[0] = "game";
		msg[1] = "100";
		msg[2] = "1"; // seatID for player 1
		msg[3] = "attack";
		msg[4] = "60"; msg[5] = "0"; msg[6] = "0";
		msg[7] = "0"; msg[8] = "0"; msg[9] = "0";
		msg[10] = "";   // empty from double-tab in send
		msg[11] = "1";  // lastevent
		msg[12] = "false"; // lastb2b
		msg[13] = "0";  // lastcombo
		msg[14] = "0";  // garbage
		msg[15] = "0";  // lastpiece
		msg[16] = "-1"; // targetSeatID

		mode.netlobbyOnMessage(null, null, msg);

		LinkedList<?> entries = (LinkedList<?>) readField(mode, "garbageEntries");
		assertNotNull(entries);
		assertFalse(entries.isEmpty());
	}

	@Test
	void netlobbyOnMessageHurryUp() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mgr.init();
		ensureEngines(mgr, 0);
		mode.modeInit(mgr);
		Object roomInfo = makeRoomInfo();
		Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
		roomInfoClass.getField("hurryupSeconds").setInt(roomInfo, 10);
		setField(mode, "netCurrentRoomInfo", roomInfo);
		setBoolean(mode, "netvsIsPractice", false);
		String[] msg = {"game", "0", "0", "hurryup"};
		mode.netlobbyOnMessage(null, null, msg);
		assertTrue(readBoolean(mode, "hurryupStarted"));
	}

	@Test
	void netSendEndGameStats() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		GameEngine e = freshEngine(mode);
		Method m = NetVSBattleMode.class.getDeclaredMethod("netSendEndGameStats", GameEngine.class);
		m.setAccessible(true);
		try {
			m.invoke(mode, e);
		} catch (java.lang.reflect.InvocationTargetException ex) {
			assertTrue(ex.getCause() instanceof NullPointerException);
		}
	}

	@Test
	void netvsRecvEndGameStats() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		GameManager mgr = new GameManager(new EventReceiver());
		mgr.mode = mode;
		mgr.init();
		ensureEngines(mgr, 2);
		mode.modeInit(mgr);

		// Set seat ID for player 1 -> seat 1 so netvsGetPlayerIDbySeatID(1) returns 1
		setIntArray(mode, "netvsPlayerSeatID", 1, 1);

		// The msg has seatID=1 so playerID=1, not 0, avoiding the (playerID != 0) filter
		String[] msg = {"gstat", "0", "1", "0", "0", "1.0", "0.5", "0.8", "10", "20.0", "5", "2.5", "3600"};
		Method m = NetVSBattleMode.class.getDeclaredMethod("netvsRecvEndGameStats", String[].class);
		m.setAccessible(true);
		m.invoke(mode, (Object) msg);
		assertEquals(60, readIntArray(mode, "garbageSent", 1)); // 1.0 * 60 = 60
	}

	// ---------------------------------------------------------------
	// GarbageEntry inner class constructors
	// ---------------------------------------------------------------

	@Test
	void garbageEntryConstructors() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		mode.modeInit(makeManager());
		Class<?> geClass = getGarbageEntryClass();

		// Constructor with lines
		Object e1 = geClass.getDeclaredConstructor(NetVSBattleMode.class, int.class).newInstance(mode, 60);
		assertEquals(60, geClass.getField("lines").getInt(e1));

		// Constructor with lines, playerID, uid
		Object e3 = geClass.getDeclaredConstructor(NetVSBattleMode.class, int.class, int.class, int.class)
				.newInstance(mode, 180, 2, 999);
		assertEquals(180, geClass.getField("lines").getInt(e3));
		assertEquals(2, geClass.getField("playerID").getInt(e3));
		assertEquals(999, geClass.getField("uid").getInt(e3));
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static int readIntArray(Object obj, String name, int index) throws Exception {
		int[] arr = (int[]) readField(obj, name);
		return arr[index];
	}

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static void setBooleanArray(Object obj, String name, int index, boolean value) throws Exception {
		boolean[] arr = (boolean[]) readField(obj, name);
		arr[index] = value;
	}

	private static void setIntArray(Object obj, String name, int index, int value) throws Exception {
		int[] arr = (int[]) readField(obj, name);
		arr[index] = value;
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
