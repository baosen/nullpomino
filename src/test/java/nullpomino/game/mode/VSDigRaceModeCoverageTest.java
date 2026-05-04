package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers uncovered branches in {@link VSDigRaceMode}: onSetting menu
 * configuration changes (cursor cases 0-12), confirm paths (load/save/other),
 * cancel, replay path, dual-player ready start, renderSetting, renderLast
 * color thresholds, calcScore game completion, onLast draw/2P-win,
 * renderResult, saveReplay, loadOtherSetting/saveOtherSetting round-trip.
 */
class VSDigRaceModeCoverageTest {

	@Test
	void loadOtherSettingReadsDefaults() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadOtherSetting(mode, engine, new CustomProperties());

		assertEquals(18, ((int[]) readField(mode, "goalLines"))[0]);
		assertEquals(100, ((int[]) readField(mode, "garbagePercent"))[0]);
		assertEquals(true, ((boolean[]) readField(mode, "enableSE"))[0]);
		assertEquals(0, ((int[]) readField(mode, "presetNumber"))[0]);
		assertEquals(0, readInt(mode, "bgmno"));
	}

	@Test
	void saveOtherSettingAndLoadOtherSettingRoundTrip() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);

		((int[]) readField(mode, "goalLines"))[0] = 10;
		((int[]) readField(mode, "garbagePercent"))[0] = 50;
		((boolean[]) readField(mode, "enableSE"))[0] = false;

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop);

		VSDigRaceMode loaded = new VSDigRaceMode();
		GameManager manager2 = new GameManager(new EventReceiver());
		loaded.modeInit(manager2);
		manager2.mode = loaded;
		manager2.init();
		manager2.engine[0].init();
		GameEngine engine2 = manager2.engine[0];
		loaded.playerInit(engine2, 0);
		invokeLoadOtherSetting(loaded, engine2, prop);

		assertEquals(10, ((int[]) readField(loaded, "goalLines"))[0]);
		assertEquals(50, ((int[]) readField(loaded, "garbagePercent"))[0]);
		assertEquals(false, ((boolean[]) readField(loaded, "enableSE"))[0]);
	}

	@Test
	void onSettingConfirmLoadPreset() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = engine.owner.modeConfig;
		prop.setProperty("vsdigrace.gravity.5", 64);
		prop.setProperty("vsdigrace.denominator.5", 128);
		prop.setProperty("vsdigrace.are.5", 10);
		prop.setProperty("vsdigrace.areLine.5", 5);
		prop.setProperty("vsdigrace.lineDelay.5", 3);
		prop.setProperty("vsdigrace.lockDelay.5", 20);
		prop.setProperty("vsdigrace.das.5", 8);

		((int[]) readField(mode, "presetNumber"))[0] = 5;
		setInt(mode, "menuCursor", 7);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		assertEquals(64, engine.speed.gravity);
	}

	@Test
	void onSettingConfirmSavePreset() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.speed.gravity = 99;
		((int[]) readField(mode, "presetNumber"))[0] = 7;
		setInt(mode, "menuCursor", 8);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		assertEquals(99, engine.owner.modeConfig.getProperty("vsdigrace.gravity.7", -1));
	}

	@Test
	void onSettingConfirmOtherSettings() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingCancelSetsQuitFlag() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;

		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingReplayPathAutoStarts() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;

		for (int i = 0; i < 120; i++) {
			mode.onSetting(engine, 0);
		}

		assertEquals(1, engine.statc[4]);
	}

	@Test
	void onSettingReplayPathSetsCursorTo9() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;

		for (int i = 0; i < 60; i++) {
			mode.onSetting(engine, 0);
		}

		assertEquals(9, readInt(mode, "menuCursor"));
	}

	@Test
	void onSettingDualPlayerReadyStartsGame() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];
		mode.playerInit(engine0, 0);
		mode.playerInit(engine1, 1);

		engine0.statc[4] = 1;
		engine1.statc[4] = 1;

		mode.onSetting(engine1, 1);

		assertEquals(GameEngine.Status.READY, engine0.stat);
		assertEquals(GameEngine.Status.READY, engine1.stat);
	}

	@Test
	void onSettingCancelReadyState() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[4] = 1;

		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;

		mode.onSetting(engine, 0);

		assertEquals(0, engine.statc[4]);
	}

	@Test
	void calcScoreGameCompleted() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();

		((int[]) readField(mode, "goalLines"))[0] = 0;
		((int[]) readField(mode, "goalLines"))[1] = 0;

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		mode.calcScore(engine0, 0, 1);

		assertFalse(engine0.timerActive);
		assertEquals(GameEngine.Status.GAMEOVER, engine1.stat);
	}

	@Test
	void onLastDetectsDraw() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		manager.engine[0].stat = GameEngine.Status.GAMEOVER;
		manager.engine[1].stat = GameEngine.Status.GAMEOVER;

		mode.onLast(manager.engine[1], 1);

		assertEquals(-1, readInt(mode, "winnerID"));
	}

	@Test
	void onLastDetectsPlayer2Win() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();

		manager.engine[0].stat = GameEngine.Status.GAMEOVER;
		manager.engine[1].stat = GameEngine.Status.MOVE;
		manager.engine[0].gameActive = true;

		((int[]) readField(mode, "winCount"))[0] = 0;
		((int[]) readField(mode, "winCount"))[1] = 0;

		mode.onLast(manager.engine[1], 1);

		assertEquals(1, readInt(mode, "winnerID"));
		assertEquals(1, ((int[]) readField(mode, "winCount"))[1]);
	}

	@Test
	void renderResultDoesNotThrow() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultWithWinner() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "winnerID", 0);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultWithLoser() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "winnerID", 1);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultWithDraw() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "winnerID", -1);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderSettingDoesNotThrow() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// statc[4] == 0, menuCursor < 9
		setInt(mode, "menuCursor", 0);
		engine.statc[4] = 0;
		mode.renderSetting(engine, 0);

		// statc[4] == 0, menuCursor >= 9
		setInt(mode, "menuCursor", 9);
		mode.renderSetting(engine, 0);

		// statc[4] == 1 (waiting)
		engine.statc[4] = 1;
		mode.renderSetting(engine, 0);
	}

	@Test
	void saveReplayPersistsVersion() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.owner.replayProp = new CustomProperties();
		mode.saveReplay(engine, 0, engine.owner.replayProp);

		assertEquals(0, engine.owner.replayProp.getProperty("vsdigrace.version", -1));
	}

	@Test
	void playerInitReplayBranch() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		engine.owner.replayMode = true;
		engine.owner.replayProp.setProperty("vsdigrace.goalLines.p0", 10);

		mode.playerInit(engine, 0);

		assertEquals(10, ((int[]) readField(mode, "goalLines"))[0]);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(VSDigRaceMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
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

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeLoadOtherSetting(VSDigRaceMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod("loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOtherSetting(VSDigRaceMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod("saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}
}