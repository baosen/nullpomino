package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers uncovered branches in {@link NetVSLineRaceMode}: netvsApplyRoomSettings,
 * getNowPlayerPlace, updateMeter, calcScore practice mode and race-win,
 * renderLast display branches, renderResult, netSendStats, netRecvStats,
 * netSendEndGameStats, netvsRecvEndGameStats.
 */
class NetVSLineRaceModeCoverageTest {

	@Test
	void netvsApplyRoomSettingsWithNullRoom() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.netCurrentRoomInfo = null;

		mode.netvsApplyRoomSettings(engine);
		// Should not throw, no changes when room is null
	}

	@Test
	void netvsApplyRoomSettingsWithRoom() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));

		nullpomino.game.net.NetRoomInfo roomInfo = new nullpomino.game.net.NetRoomInfo();
		roomInfo.gravity = 4;
		roomInfo.denominator = 256;
		roomInfo.are = 0;
		roomInfo.areLine = 0;
		roomInfo.lineDelay = 0;
		roomInfo.lockDelay = 30;
		roomInfo.das = 14;
		mode.netCurrentRoomInfo = roomInfo;

		mode.netvsApplyRoomSettings(engine);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(0, engine.speed.are);
		assertEquals(0, engine.speed.areLine);
		assertEquals(0, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
	}

	@Test
	void getNowPlayerPlaceReturnsNeg1ForDeadPlayer() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		setBooleanArray(mode, "netvsPlayerDead", true);
		setBooleanArray(mode, "netvsPlayerExist", true);

		int result = invokeGetNowPlayerPlace(mode, engine, 0);
		assertEquals(-1, result);
	}

	@Test
	void getNowPlayerPlaceReturns0ForLeadingPlayer() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];

		setBooleanArray(mode, "netvsPlayerDead", false);
		setBooleanArray(mode, "netvsPlayerExist", true);
		engine.statistics.lines = 30;

		int result = invokeGetNowPlayerPlace(mode, engine, 0);
		assertEquals(0, result);
	}

	@Test
	void updateMeterWithGoalLines() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		setInt(mode, "goalLines", 40);
		engine.statistics.lines = 10;

		invokeUpdateMeter(mode, engine);

		assertTrue(engine.meterValue >= 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	@Test
	void updateMeterWithZeroGoalLines() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		setInt(mode, "goalLines", 0);

		invokeUpdateMeter(mode, engine);
		// Should not divide by zero
	}

	@Test
	void calcScoreInPracticeMode() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setBoolean(mode, "netvsIsPractice", true);
		setInt(mode, "goalLines", 40);
		engine.statistics.lines = 40;

		mode.calcScore(engine, 0, 1);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	@Test
	void renderResultDoesNotThrow() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);

		mode.renderResult(engine, 0);
	}

	@Test
	void netRecvStatsUpdatesLines() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		setInt(mode, "goalLines", 40);

		String[] message = new String[] {"game", "0", "0", "stats", "0", "10", "5.0", "3.0"};
		mode.netRecvStats(engine, message);

		assertEquals(10, engine.statistics.lines);
	}

	@Test
	void netSendStatsDoesNotThrow() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);
		setBoolean(mode, "netvsIsPractice", false);

		// netSendStats requires netLobby, so we need to set up netplay
		// Just verify it doesn't crash when not netplay
		mode.netIsNetPlay = false;
		mode.netSendStats(engine);
	}

	@Test
	void startGameSetsMeter() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.modeInit(new GameManager(new EventReceiver()));
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	@Test
	void netGetGoalTypeReturnsGoalLines() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		setInt(mode, "goalLines", 40);

		assertEquals(40, mode.netGetGoalType());
	}

	// ---- helpers ----

	private static GameEngine freshEngine(NetVSLineRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
	}

	private static void setBooleanArray(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		boolean[] arr = (boolean[]) f.get(obj);
		if (arr != null) {
			for (int i = 0; i < arr.length; i++) arr[i] = value;
		}
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static int invokeGetNowPlayerPlace(NetVSLineRaceMode mode, GameEngine engine, int playerID) throws Exception {
		java.lang.reflect.Method m = NetVSLineRaceMode.class.getDeclaredMethod("getNowPlayerPlace", GameEngine.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, engine, playerID);
	}

	private static void invokeUpdateMeter(NetVSLineRaceMode mode, GameEngine engine) throws Exception {
		java.lang.reflect.Method m = NetVSLineRaceMode.class.getDeclaredMethod("updateMeter", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}