package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Netplay-specific branches in {@link NetDummyMode},
 * {@link NetDummyVSMode}, and the three concrete net VS modes
 * ({@link NetVSBattleMode}, {@link NetVSDigRaceMode},
 * {@link NetVSLineRaceMode}).
 */
class NetModeBranchesTest {

	// ---- NetDummyMode ----

	@Test
	void netDummyModeSurface() {
		NetDummyMode mode = new NetDummyMode();
		assertEquals("NET-DUMMY", mode.getName());
		assertEquals(1, mode.getPlayers());
		assertFalse(mode.isNetplayMode());
		assertFalse(mode.isVSMode());
	}

	@Test
	void netDummyModeRankingDisplayModeSmoke() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameEngine engine = createEngine(mode);

		// Enable ranking display mode
		setField(mode, "netIsNetRankingDisplayMode", true);
		setField(mode, "netRankingNoDataFlag", new boolean[] { false, false });
		setField(mode, "netRankingReady", new boolean[] { true, true });

		// Populate ranking data for the controller-based branch
		LinkedList<Integer>[] netRankingPlace = makeListArray(0);
		setField(mode, "netRankingPlace", netRankingPlace);
		LinkedList<String>[] netRankingName = makeStringArray("TestPlayer");
		setField(mode, "netRankingName", netRankingName);
		LinkedList<Integer>[] netRankingScore = makeListArray(1000);
		setField(mode, "netRankingScore", netRankingScore);
		LinkedList<Integer>[] netRankingLines = makeListArray(10);
		setField(mode, "netRankingLines", netRankingLines);
		LinkedList<Integer>[] netRankingTime = makeListArray(500);
		setField(mode, "netRankingTime", netRankingTime);
		LinkedList<Float>[] netRankingPPS = makeFloatArray(1.0f);
		setField(mode, "netRankingPPS", netRankingPPS);

		// Set ranking type to GENERIC_SCORE for the rendering branch
		setField(mode, "netRankingType", 0);
		setField(mode, "netRankingCursor", new int[] { 0, 0 });
		setField(mode, "netRankingMyRank", new int[] { -1, -1 });

		// Call netOnUpdateNetPlayRanking via onSetting
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		mode.onSetting(engine, 0);
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 0;
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = false;

		// Try LEFT/RIGHT to switch ranking view
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = true;
		mode.onSetting(engine, 0);
		engine.ctrl.buttonTime[Controller.BUTTON_LEFT] = 0;
		engine.ctrl.buttonPress[Controller.BUTTON_LEFT] = false;

		// Try B to exit ranking display
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
		mode.onSetting(engine, 0);
		engine.ctrl.buttonTime[Controller.BUTTON_B] = 0;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = false;

		// Render ranking display mode
		mode.renderSetting(engine, 0);
	}

	@Test
	void netDummyModeNoDataRankingBranch() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameEngine engine = createEngine(mode);

		setField(mode, "netIsNetRankingDisplayMode", true);
		setField(mode, "netRankingNoDataFlag", new boolean[] { true, true });
		setField(mode, "netRankingReady", new boolean[] { false, false });

		mode.renderSetting(engine, 0);
	}

	@Test
	void netDummyModeLoadingRankingBranch() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameEngine engine = createEngine(mode);

		setField(mode, "netIsNetRankingDisplayMode", true);
		setField(mode, "netRankingNoDataFlag", new boolean[] { false, false });
		setField(mode, "netRankingReady", new boolean[] { false, false });

		mode.renderSetting(engine, 0);
	}

	@Test
	void netDummyModeSendReplaySkipsWhenNotConnected() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		createEngine(mode);
		invokeMethod(mode, "netSendReplay", new Class<?>[] { GameEngine.class },
				new Object[] { null }); // engine can be null since we check netIsNetRankingSendOK first
	}

	@Test
	void netDummyModeGetGoalType() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		int goalType = (int) invokeMethod(mode, "netGetGoalType", new Class<?>[0], new Object[0]);
		assertEquals(0, goalType);
	}

	@Test
	void netDummyModeNetIsNetRankingViewOK() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameEngine engine = createEngine(mode);
		boolean ok = (boolean) invokeMethod(mode, "netIsNetRankingViewOK",
				new Class<?>[] { GameEngine.class }, new Object[] { engine });
		assertFalse(ok, "Default netIsNetRankingViewOK should return false");
	}

	@Test
	void netDummyModeNetIsNetRankingSendOK() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameEngine engine = createEngine(mode);
		boolean ok = (boolean) invokeMethod(mode, "netIsNetRankingSendOK",
				new Class<?>[] { GameEngine.class }, new Object[] { engine });
		assertFalse(ok, "Default netIsNetRankingSendOK should return false");
	}

	@Test
	void netDummyModeNetSendStatsNoThrow() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameEngine engine = createEngine(mode);
		invokeMethod(mode, "netSendStats", new Class<?>[] { GameEngine.class }, new Object[] { engine });
	}

	@Test
	void netDummyModeNetSendEndGameStatsNoThrow() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameEngine engine = createEngine(mode);
		invokeMethod(mode, "netSendEndGameStats", new Class<?>[] { GameEngine.class }, new Object[] { engine });
	}

	@Test
	void netDummyModeNetSendOptionsNoThrow() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		GameEngine engine = createEngine(mode);
		invokeMethod(mode, "netSendOptions", new Class<?>[] { GameEngine.class }, new Object[] { engine });
	}

	@Test
	void netDummyModeLoadRankingNoThrow() throws Exception {
		NetDummyMode mode = new NetDummyMode();
		createEngine(mode);
		invokeMethod(mode, "loadRanking",
				new Class<?>[] { CustomProperties.class, String.class },
				new Object[] { new CustomProperties(), "testRule" });
	}

	// ---- NetDummyVSMode ----

	@Test
	void netDummyVSModeSurface() {
		NetDummyVSMode mode = new NetDummyVSMode();
		assertEquals("NET-VS-DUMMY", mode.getName());
		assertEquals(6, mode.getPlayers());
		assertTrue(mode.isNetplayMode());
		assertTrue(mode.isVSMode());
	}

	@Test
	void netDummyVSModeRenderLastNoThrow() throws Exception {
		NetDummyVSMode mode = new NetDummyVSMode();
		GameEngine engine = createEngine(mode);
		engine.createFieldIfNeeded();
		mode.renderLast(engine, 0);
	}

	// ---- NetVSBattleMode ----

	@Test
	void netVSBattleModeSmoke() throws Exception {
		NetVSBattleMode mode = new NetVSBattleMode();
		assertEquals("NET-VS-BATTLE", mode.getName());
		assertTrue(mode.isVSMode());
		assertTrue(mode.isNetplayMode());

		GameEngine engine = createEngine(mode);
		engine.createFieldIfNeeded();
		mode.renderLast(engine, 0);
	}

	// ---- NetVSDigRaceMode ----

	@Test
	void netVSDigRaceModeSmoke() throws Exception {
		NetVSDigRaceMode mode = new NetVSDigRaceMode();
		assertEquals("NET-VS-DIG RACE", mode.getName());
		assertTrue(mode.isNetplayMode());

		GameEngine engine = createEngine(mode);
		engine.createFieldIfNeeded();
		mode.renderLast(engine, 0);
	}

	// ---- NetVSLineRaceMode ----

	@Test
	void netVSLineRaceModeSmoke() throws Exception {
		NetVSLineRaceMode mode = new NetVSLineRaceMode();
		assertEquals("NET-VS-LINE RACE", mode.getName());
		assertTrue(mode.isNetplayMode());

		GameEngine engine = createEngine(mode);
		engine.createFieldIfNeeded();
		mode.renderLast(engine, 0);
	}

	// ---- helpers ----

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

	private static GameEngine createEngine(GameMode mode) throws Exception {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.playerInit(manager.engine[0], 0);
		if (manager.engine[0].ruleopt != null) {
			if (manager.engine[0].ruleopt.fieldWidth <= 0) manager.engine[0].ruleopt.fieldWidth = 10;
			if (manager.engine[0].ruleopt.fieldHeight <= 0) manager.engine[0].ruleopt.fieldHeight = 20;
			if (manager.engine[0].ruleopt.fieldHiddenHeight < 0) manager.engine[0].ruleopt.fieldHiddenHeight = 4;
		}
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].stat = GameEngine.Status.SETTING;
		manager.engine[0].resetStatc();
		return manager.engine[0];
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static Object invokeMethod(Object obj, String name, Class<?>[] argTypes, Object[] args) throws Exception {
		java.lang.reflect.Method m = findMethod(obj.getClass(), name, argTypes);
		m.setAccessible(true);
		return m.invoke(obj, args);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
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
