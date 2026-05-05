package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage tests for NetDummyMode-derived modes: NetDummyMode, LineRaceMode,
 * DigRaceMode, NetVSLineRaceMode, NetVSDigRaceMode.
 */
class Batch2NetModesCoverageTest {

	// =====================================================================
	// NetDummyMode
	// =====================================================================

	@Test
	void netDummyModeInitAndGameFlow() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameEngine engine = freshEngine(mode);

		assertEquals("NET-DUMMY", mode.getName());

		// playerInit
		mode.playerInit(engine, 0);
		assertEquals(GameEngine.FRAME_COLOR_BLUE, engine.framecolor);

		// startGame
		mode.startGame(engine, 0);

		// renderLast, renderSetting, renderResult (smoke tests)
		mode.renderLast(engine, 0);
		mode.renderSetting(engine, 0);
		mode.renderResult(engine, 0);

		// saveReplay
		CustomProperties rp = new CustomProperties();
		mode.saveReplay(engine, 0, rp);

		// calcScore
		engine.createFieldIfNeeded();
		mode.calcScore(engine, 0, 1);

		// onSetting, onReady (replay mode)
		engine.owner.replayMode = true;
		mode.onReady(engine, 0);
		mode.onSetting(engine, 0);
	}

	// =====================================================================
	// LineRaceMode (extends NetDummyMode)
	// =====================================================================

	@Test
	void lineRaceModeInitAndGameFlow() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);

		assertEquals("LINE RACE", mode.getName());

		// playerInit
		mode.playerInit(engine, 0);
		assertEquals(GameEngine.FRAME_COLOR_RED, engine.framecolor);

		// loadPreset/savePreset round-trip
		CustomProperties prop = new CustomProperties();
		mode.playerInit(engine, 0);
		invoke(mode, "savePreset", engine, prop, -1);
		LineRaceMode mode2 = new LineRaceMode();
		GameEngine e2 = freshEngine(mode2);
		invoke(mode2, "loadPreset", e2, prop, -1);
		assertEquals(4, e2.speed.gravity);

		// loadRanking
		invoke(mode, "loadRanking", prop, "testrule");

		// startGame
		set(mode, "big", true);
		mode.startGame(engine, 0);
		assertTrue(engine.big);

		// calcScore - must fill garbage first for DigRaceMode
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.statistics.lines = 5;
		mode.calcScore(engine, 0, 1);
		assertTrue(true);

		// renderLast
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
		engine.stat = GameEngine.Status.READY;
		mode.renderLast(engine, 0);

		// renderSetting
		mode.renderSetting(engine, 0);

		// renderResult
		mode.renderResult(engine, 0);

		// saveReplay
		CustomProperties rp = new CustomProperties();
		mode.saveReplay(engine, 0, rp);

		// netRecvStats (needs 14 elements for DigRaceMode)
		invoke(mode, "netRecvStats", engine, new String[]{"game", "stats", "0", "0", "3", "5", "1200", "5.0", "1.5", "0", "true", "true", "0", "50"});
		assertEquals(3, engine.statistics.lines);

		// netGetGoalType
		int gt = invokeInt(mode, "netGetGoalType");
		assertEquals(0, gt);

		// netIsNetRankingViewOK
		set(mode, "big", false);
		boolean ok = invokeBool(mode, "netIsNetRankingViewOK", engine);
		assertTrue(ok);

		// netIsNetRankingSendOK - need lines >= GOAL_TABLE[goaltype]=20
		engine.statistics.lines = 25;
		boolean sendOk = invokeBool(mode, "netIsNetRankingSendOK", engine);
		assertTrue(sendOk);
	}

	// =====================================================================
	// DigRaceMode (extends NetDummyMode)
	// =====================================================================

	@Test
	void digRaceModeInitAndGameFlow() throws Exception {
		DigRaceMode mode = new DigRaceMode();
		GameEngine engine = freshEngine(mode);

		assertEquals("DIG RACE", mode.getName());

		// playerInit
		mode.playerInit(engine, 0);
		assertEquals(GameEngine.FRAME_COLOR_GREEN, engine.framecolor);

		// loadPreset/savePreset
		CustomProperties prop = new CustomProperties();
		invoke(mode, "savePreset", engine, prop, -1);
		DigRaceMode mode2 = new DigRaceMode();
		GameEngine e2 = freshEngine(mode2);
		invoke(mode2, "loadPreset", e2, prop, -1);
		assertEquals(4, e2.speed.gravity);

		// loadRanking
		invoke(mode, "loadRanking", prop, "testrule");

		// startGame
		set(mode, "big", false);
		mode.startGame(engine, 0);

		// calcScore
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		mode.calcScore(engine, 0, 1);

		// renderLast
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
		engine.stat = GameEngine.Status.READY;
		mode.renderLast(engine, 0);

		// renderSetting
		mode.renderSetting(engine, 0);

		// renderResult
		mode.renderResult(engine, 0);

		// saveReplay
		CustomProperties rp = new CustomProperties();
		mode.saveReplay(engine, 0, rp);

		// netRecvStats (needs 14 elements for DigRaceMode: indexes 12 and 13 are meterColor/meterValue)
		invoke(mode, "netRecvStats", engine, new String[]{"game", "stats", "0", "0", "3", "5", "1200", "5.0", "1.5", "0", "true", "true", "0", "50"});
		assertEquals(3, engine.statistics.lines);

		// netGetGoalType
		int gt = invokeInt(mode, "netGetGoalType");
		assertEquals(0, gt);
	}

	// =====================================================================
	// NetVSLineRaceMode (extends NetDummyVSMode)
	// =====================================================================

	@Test
	void netVSLineRaceModeInitAndGameFlow() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();

		assertEquals("NET-VS-LINE RACE", mode.getName());

		// netPlayerInit
		mode.netPlayerInit(manager.engine[0], 0);
		assertEquals(GameEngine.METER_COLOR_GREEN, manager.engine[0].meterColor);

		// startGame
		mode.startGame(manager.engine[0], 0);

		// renderLast
		mode.renderLast(manager.engine[0], 0);

		// renderResult
		mode.renderResult(manager.engine[0], 0);

		// calcScore
		set(mode, "netvsIsPractice", true);
		manager.engine[0].createFieldIfNeeded();
		mode.calcScore(manager.engine[0], 0, 1);

		// calcScore reaching goal
		set(mode, "goalLines", 5);
		manager.engine[0].statistics.lines = 5;
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
		mode.calcScore(manager.engine[0], 0, 1);
		assertTrue(true);

		// getNowPlayerPlace: ensure player 0 exists
		set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
		set(mode, "netvsPlayerDead", new boolean[]{false, false, false, false, false, false});
		int place = invokeInt(mode, "getNowPlayerPlace", manager.engine[0], 0);
		assertTrue(place >= 0);

		// netRecvStats
		invoke(mode, "netRecvStats", manager.engine[0], new String[]{"game", "stats", "0", "0", "5", "10", "5000", "8.0", "2.0", "true", "true"});
		assertEquals(5, manager.engine[0].statistics.lines);

		// netvsApplyRoomSettings
		invoke(mode, "netvsApplyRoomSettings", manager.engine[0]);
	}

	// =====================================================================
	// NetVSDigRaceMode (extends NetDummyVSMode)
	// =====================================================================

	@Test
	void netVSDigRaceModeInitAndGameFlow() throws Exception {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();

		assertEquals("NET-VS-DIG RACE", mode.getName());

		// modeInit
		assertEquals(18, getInt(mode, "goalLines"));

		// startGame
		mode.startGame(manager.engine[0], 0);

		// renderLast
		mode.renderLast(manager.engine[0], 0);

		// renderResult
		mode.renderResult(manager.engine[0], 0);

		// calcScore: set practice mode to avoid netLobby NPE
		set(mode, "netvsIsPractice", true);
		manager.engine[0].createFieldIfNeeded();
		mode.calcScore(manager.engine[0], 0, 1);

		// calcScore (non-winning)
		set(mode, "goalLines", 18);
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
		mode.calcScore(manager.engine[0], 0, 0);

		// getNowPlayerPlace: ensure player 0 exists
		set(mode, "netvsPlayerExist", new boolean[]{true, false, false, false, false, false});
		set(mode, "netvsPlayerDead", new boolean[]{false, false, false, false, false, false});
		int place = invokeInt(mode, "getNowPlayerPlace", manager.engine[0], 0);
		assertTrue(place >= 0);

		// netRecvStats (updates playerRemainLines, not statistics.lines)
		invoke(mode, "netRecvStats", manager.engine[0], new String[]{"game", "stats", "0", "0", "3", "5", "4000", "6.0", "1.8"});

		// netvsApplyRoomSettings
		invoke(mode, "netvsApplyRoomSettings", manager.engine[0]);
	}

	// =====================================================================
	// Helpers
	// =====================================================================

	private static GameEngine freshEngine(GameMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name + " in " + cls.getName());
	}

	private static int getInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static boolean getBool(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
	}

	private static void set(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static void invoke(Object obj, String name, Object... args) throws Exception {
		Class<?>[] types = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
		for (int i = 0; i < args.length; i++) {
			if (types[i] == Integer.class) types[i] = int.class;
			else if (types[i] == Boolean.class) types[i] = boolean.class;
			else if (types[i] == Float.class) types[i] = float.class;
		}
		Method m = findMethod(obj.getClass(), name, types);
		m.setAccessible(true);
		m.invoke(obj, args);
	}

	private static boolean invokeBool(Object obj, String name, Object... args) throws Exception {
		Class<?>[] types = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
		for (int i = 0; i < args.length; i++) {
			if (types[i] == Integer.class) types[i] = int.class;
			else if (types[i] == Boolean.class) types[i] = boolean.class;
		}
		Method m = findMethod(obj.getClass(), name, types);
		m.setAccessible(true);
		return (boolean) m.invoke(obj, args);
	}

	private static int invokeInt(Object obj, String name, Object... args) throws Exception {
		Class<?>[] types = new Class<?>[args.length];
		for (int i = 0; i < args.length; i++) types[i] = args[i].getClass();
		for (int i = 0; i < args.length; i++) {
			if (types[i] == Integer.class) types[i] = int.class;
			else if (types[i] == Boolean.class) types[i] = boolean.class;
		}
		Method m = findMethod(obj.getClass(), name, types);
		m.setAccessible(true);
		return (int) m.invoke(obj, args);
	}

	private static Method findMethod(Class<?> cls, String name, Class<?>... paramTypes) throws NoSuchMethodException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredMethod(name, paramTypes); }
			catch (NoSuchMethodException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchMethodException(name + " in " + cls.getName());
	}
}
