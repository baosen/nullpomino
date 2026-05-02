package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetVSLineRaceMode} logic that does not require a live
 * netplay session: registry-facing constants, the private
 * {@code getNowPlayerPlace} ranking method, and the private
 * {@code updateMeter} progress-bar method.
 */
class NetVSLineRaceModeLogicTest {

	private NetVSLineRaceMode mode;
	private GameManager manager;

	@BeforeEach
	void setUp() {
		mode = new NetVSLineRaceMode();
		manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		// Initialise all engines so their statistics objects exist.
		for (int i = 0; i < manager.engine.length; i++) {
			manager.engine[i].init();
		}
	}

	// ========================
	// Registry constants
	// ========================

	@Test
	void getNameReturnsNetVSLineRace() {
		assertEquals("NET-VS-LINE RACE", mode.getName());
	}

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(mode.isVSMode());
	}

	@Test
	void isNetplayModeReturnsTrue() {
		assertTrue(mode.isNetplayMode());
	}

	@Test
	void getPlayersReturnsSix() {
		assertEquals(6, mode.getPlayers());
	}

	@Test
	void goalLinesInitialisedToFortyByModeInit() throws Exception {
		assertEquals(40, getInt(mode, "goalLines"));
	}

	// ========================
	// getNowPlayerPlace (private, tested via reflection)
	// ========================

	@Test
	void getNowPlayerPlaceReturnsMinusOneForNonExistentPlayer() throws Exception {
		setPlayerExists(0, false);
		assertEquals(-1, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceReturnsMinusOneForDeadPlayer() throws Exception {
		setPlayerExists(0, true);
		setPlayerDead(0, true);
		assertEquals(-1, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceReturnsZeroForOnlyAlivePlayer() throws Exception {
		setPlayerExists(0, true);
		manager.engine[0].statistics.lines = 15;
		assertEquals(0, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceReturnsZeroWhenLeadingByLines() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		manager.engine[0].statistics.lines = 30;
		manager.engine[1].statistics.lines = 10;
		assertEquals(0, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceReturnsOneWhenTrailingByLines() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		manager.engine[0].statistics.lines = 5;
		manager.engine[1].statistics.lines = 25;
		assertEquals(1, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceTiebreakerByPps() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		manager.engine[0].statistics.lines = 20;
		manager.engine[0].statistics.pps = 0.5f;
		manager.engine[1].statistics.lines = 20;
		manager.engine[1].statistics.pps = 1.0f;
		// Same lines but lower pps => rank behind
		assertEquals(1, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceTiebreakerByLpm() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		manager.engine[0].statistics.lines = 20;
		manager.engine[0].statistics.pps = 0.5f;
		manager.engine[0].statistics.lpm = 5.0f;
		manager.engine[1].statistics.lines = 20;
		manager.engine[1].statistics.pps = 0.5f;
		manager.engine[1].statistics.lpm = 10.0f;
		// Same lines and pps but lower lpm => rank behind
		assertEquals(1, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceTiesAreNotCountedAgainstEqualPlayer() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		manager.engine[0].statistics.lines = 20;
		manager.engine[0].statistics.pps = 1.0f;
		manager.engine[0].statistics.lpm = 10.0f;
		manager.engine[1].statistics.lines = 20;
		manager.engine[1].statistics.pps = 1.0f;
		manager.engine[1].statistics.lpm = 10.0f;
		// Everything equal => both rank 0
		assertEquals(0, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceMultiplePlayers() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		setPlayerExists(2, true);
		manager.engine[0].statistics.lines = 35;
		manager.engine[1].statistics.lines = 20;
		manager.engine[2].statistics.lines = 10;
		assertEquals(0, callGetNowPlayerPlace(manager.engine[0], 0));
		assertEquals(1, callGetNowPlayerPlace(manager.engine[1], 1));
		assertEquals(2, callGetNowPlayerPlace(manager.engine[2], 2));
	}

	@Test
	void getNowPlayerPlaceSkipsDeadAndNonExistentPlayers() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		setPlayerDead(1, true);
		setPlayerExists(2, true);
		manager.engine[0].statistics.lines = 5;
		manager.engine[2].statistics.lines = 25;
		// Player 1 is dead, so only 0 and 2 compete; player 0 trails player 2
		assertEquals(1, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	@Test
	void getNowPlayerPlaceCapsLinesAtGoalLines() throws Exception {
		setPlayerExists(0, true);
		setPlayerExists(1, true);
		manager.engine[0].statistics.lines = 100; // capped to goalLines (40)
		manager.engine[1].statistics.lines = 50;  // capped to goalLines (40)
		// Both capped at 40 => equal lines, equal pps(0), equal lpm(0) => tie => rank 0
		assertEquals(0, callGetNowPlayerPlace(manager.engine[0], 0));
	}

	// ========================
	// updateMeter (private, tested via reflection)
	// ========================

	private void enableMeter() throws Exception {
		manager.engine[0].fieldHeight = 20;
		Field sf = EventReceiver.class.getDeclaredField("showmeter");
		sf.setAccessible(true);
		sf.set(manager.receiver, true);
	}

	@Test
	void updateMeterSetsGreenWhenRemainLinesAbove30() throws Exception {
		enableMeter();
		manager.engine[0].statistics.lines = 0; // 40 remain
		callUpdateMeter(manager.engine[0]);
		assertEquals(GameEngine.METER_COLOR_GREEN, manager.engine[0].meterColor);
	}

	@Test
	void updateMeterSetsYellowAtExactly30Remaining() throws Exception {
		enableMeter();
		manager.engine[0].statistics.lines = 10; // 30 remain
		callUpdateMeter(manager.engine[0]);
		assertEquals(GameEngine.METER_COLOR_YELLOW, manager.engine[0].meterColor);
	}

	@Test
	void updateMeterSetsOrangeAtExactly20Remaining() throws Exception {
		enableMeter();
		manager.engine[0].statistics.lines = 20; // 20 remain
		callUpdateMeter(manager.engine[0]);
		assertEquals(GameEngine.METER_COLOR_ORANGE, manager.engine[0].meterColor);
	}

	@Test
	void updateMeterSetsRedAtExactly10Remaining() throws Exception {
		enableMeter();
		manager.engine[0].statistics.lines = 30; // 10 remain
		callUpdateMeter(manager.engine[0]);
		assertEquals(GameEngine.METER_COLOR_RED, manager.engine[0].meterColor);
	}

	@Test
	void updateMeterSetsRedAtExactly0Remaining() throws Exception {
		enableMeter();
		manager.engine[0].statistics.lines = 40; // 0 remain
		callUpdateMeter(manager.engine[0]);
		assertEquals(GameEngine.METER_COLOR_RED, manager.engine[0].meterColor);
		assertEquals(0, manager.engine[0].meterValue);
	}

	@Test
	void updateMeterCalculatesCorrectMeterValue() throws Exception {
		enableMeter();
		// meterMax = fieldHeight * 16 = 20 * 16 = 320
		// meterValue = (remainLines * 320) / goalLines = (20 * 320) / 40 = 160
		manager.engine[0].statistics.lines = 20;
		callUpdateMeter(manager.engine[0]);
		assertEquals(160, manager.engine[0].meterValue);
	}

	@Test
	void updateMeterFullWhenNoLinesCleared() throws Exception {
		enableMeter();
		manager.engine[0].statistics.lines = 0;
		callUpdateMeter(manager.engine[0]);
		int expectedMax = manager.engine[0].fieldHeight * 16;
		assertEquals(expectedMax, manager.engine[0].meterValue);
	}

	@Test
	void updateMeterDoesNothingWhenGoalLinesIsZero() throws Exception {
		setInt(mode, "goalLines", 0);
		enableMeter();
		manager.engine[0].statistics.lines = 10;
		manager.engine[0].meterValue = -999;
		manager.engine[0].meterColor = -999;
		callUpdateMeter(manager.engine[0]);
		assertEquals(-999, manager.engine[0].meterValue);
		assertEquals(-999, manager.engine[0].meterColor);
	}

	// ========================
	// Reflection helpers
	// ========================

	private void setPlayerExists(int playerID, boolean exists) throws Exception {
		boolean[] arr = (boolean[]) getField(mode, "netvsPlayerExist");
		arr[playerID] = exists;
	}

	private void setPlayerDead(int playerID, boolean dead) throws Exception {
		boolean[] arr = (boolean[]) getField(mode, "netvsPlayerDead");
		arr[playerID] = dead;
	}

	private static int getInt(Object instance, String name) throws Exception {
		Field f = resolveField(instance.getClass(), name);
		f.setAccessible(true);
		return f.getInt(instance);
	}

	private static void setInt(Object instance, String name, int value) throws Exception {
		Field f = resolveField(instance.getClass(), name);
		f.setAccessible(true);
		f.setInt(instance, value);
	}

	private static Object getField(Object instance, String name) throws Exception {
		Field f = resolveField(instance.getClass(), name);
		f.setAccessible(true);
		return f.get(instance);
	}

	private int callGetNowPlayerPlace(GameEngine engine, int playerID) throws Exception {
		Method m = NetVSLineRaceMode.class.getDeclaredMethod("getNowPlayerPlace", GameEngine.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, engine, playerID);
	}

	private void callUpdateMeter(GameEngine engine) throws Exception {
		Method m = NetVSLineRaceMode.class.getDeclaredMethod("updateMeter", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	/**
	 * Walk the class hierarchy to find a declared field, supporting fields
	 * defined in parent classes (e.g. netvsPlayerExist on NetDummyVSMode).
	 */
	private static Field resolveField(Class<?> clazz, String name) throws NoSuchFieldException {
		Class<?> c = clazz;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
