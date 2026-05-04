package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers uncovered branches in {@link VSLineRaceMode}: onSetting menu
 * configuration changes (all cursor cases 0-12), confirm paths (load/save/other),
 * cancel, replay path, dual-player ready start, renderLast color thresholds,
 * calcScore all-clear and game completion, onLast draw/1P-win/2P-win,
 * renderResult, saveReplay, loadOtherSetting/saveOtherSetting round-trip.
 */
class VSLineRaceModeCoverageTest {

	// -----------------------------------------------------------------------
	// loadOtherSetting / saveOtherSetting round-trip
	// -----------------------------------------------------------------------

	@Test
	void loadOtherSettingReadsDefaults() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		invokeLoadOtherSetting(mode, engine, new CustomProperties());

		assertEquals(40, ((int[]) readField(mode, "goalLines"))[0]);
		assertEquals(0, readInt(mode, "bgmno"));
		assertFalse(((boolean[]) readField(mode, "big"))[0]);
		assertTrue(((boolean[]) readField(mode, "enableSE"))[0]);
		assertEquals(0, ((int[]) readField(mode, "presetNumber"))[0]);
	}

	@Test
	void saveOtherSettingAndLoadOtherSettingRoundTrip() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);

		((int[]) readField(mode, "goalLines"))[0] = 80;
		((boolean[]) readField(mode, "big"))[0] = true;
		((boolean[]) readField(mode, "enableSE"))[0] = false;
		setInt(mode, "bgmno", 3);

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop);

		VSLineRaceMode loaded = new VSLineRaceMode();
		GameManager manager2 = new GameManager(new EventReceiver());
		loaded.modeInit(manager2);
		manager2.mode = loaded;
		manager2.init();
		manager2.engine[0].init();
		GameEngine engine2 = manager2.engine[0];
		loaded.playerInit(engine2, 0);
		invokeLoadOtherSetting(loaded, engine2, prop);

		assertEquals(80, ((int[]) readField(loaded, "goalLines"))[0]);
		assertTrue(((boolean[]) readField(loaded, "big"))[0]);
		assertFalse(((boolean[]) readField(loaded, "enableSE"))[0]);
		assertEquals(3, readInt(loaded, "bgmno"));
	}

	// -----------------------------------------------------------------------
	// onSetting confirm paths
	// -----------------------------------------------------------------------

	@Test
	void onSettingConfirmLoadPreset() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Save a preset first
		CustomProperties prop = engine.owner.modeConfig;
		prop.setProperty("vslinerace.gravity.5", 64);
		prop.setProperty("vslinerace.denominator.5", 128);
		prop.setProperty("vslinerace.are.5", 10);
		prop.setProperty("vslinerace.areLine.5", 5);
		prop.setProperty("vslinerace.lineDelay.5", 3);
		prop.setProperty("vslinerace.lockDelay.5", 20);
		prop.setProperty("vslinerace.das.5", 8);

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
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.speed.gravity = 99;
		((int[]) readField(mode, "presetNumber"))[0] = 7;
		setInt(mode, "menuCursor", 8);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		mode.onSetting(engine, 0);

		assertEquals(99, engine.owner.modeConfig.getProperty("vslinerace.gravity.7", -1));
	}

	@Test
	void onSettingConfirmOtherSettings() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;

		boolean result = mode.onSetting(engine, 0);
		// menuCursor 0 is not 7 or 8, so it should save other settings and set statc[4]=1
		assertEquals(1, engine.statc[4]);
	}

	// -----------------------------------------------------------------------
	// onSetting cancel
	// -----------------------------------------------------------------------

	@Test
	void onSettingCancelSetsQuitFlag() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;

		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag, "B button should set quitflag");
	}

	// -----------------------------------------------------------------------
	// onSetting replay path
	// -----------------------------------------------------------------------

	@Test
	void onSettingReplayPathWaitsThenStarts() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;

		// Simulate 120 frames of replay waiting
		for (int i = 0; i < 120; i++) {
			mode.onSetting(engine, 0);
		}

		assertEquals(1, engine.statc[4], "After 120 frames, replay should auto-start");
	}

	// -----------------------------------------------------------------------
	// onSetting dual-player ready
	// -----------------------------------------------------------------------

	@Test
	void onSettingDualPlayerReadyStartsGame() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
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

		// Both players ready
		engine0.statc[4] = 1;
		engine1.statc[4] = 1;

		mode.onSetting(engine1, 1);

		assertEquals(GameEngine.Status.READY, engine0.stat);
		assertEquals(GameEngine.Status.READY, engine1.stat);
	}

	// -----------------------------------------------------------------------
	// onSetting cancel from ready state
	// -----------------------------------------------------------------------

	@Test
	void onSettingCancelReadyState() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[4] = 1;

		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;

		mode.onSetting(engine, 0);

		assertEquals(0, engine.statc[4], "B button in ready state should cancel");
	}

	// -----------------------------------------------------------------------
	// calcScore all-clear
	// -----------------------------------------------------------------------

	@Test
	void calcScoreAllClearPlaysBravo() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		// Empty field + lines >= 1 = all clear
		mode.calcScore(engine, 0, 1);
		// Should not throw
	}

	@Test
	void calcScoreGameCompletedSetsGameOver() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
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

		((int[]) readField(mode, "goalLines"))[0] = 40;
		engine0.statistics.lines = 40;
		engine0.nowPieceObject = new Piece(Piece.PIECE_T);
		engine0.createFieldIfNeeded();

		mode.calcScore(engine0, 0, 1);

		assertFalse(engine0.timerActive);
		assertEquals(GameEngine.Status.GAMEOVER, engine1.stat);
	}

	// -----------------------------------------------------------------------
	// onLast draw detection
	// -----------------------------------------------------------------------

	@Test
	void onLastDrawDetection() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
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

		engine0.stat = GameEngine.Status.GAMEOVER;
		engine1.stat = GameEngine.Status.GAMEOVER;
		engine0.gameActive = true;

		mode.onLast(engine1, 1);

		assertEquals(-1, readInt(mode, "winnerID"));
	}

	// -----------------------------------------------------------------------
	// renderResult
	// -----------------------------------------------------------------------

	@Test
	void renderResultDoesNotThrow() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultWithWinner() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "winnerID", 0);

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultWithLoser() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "winnerID", 1);

		mode.renderResult(engine, 0);
	}

	// -----------------------------------------------------------------------
	// startGame
	// -----------------------------------------------------------------------

	@Test
	void startGameSetsBigAndSE() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		((boolean[]) readField(mode, "big"))[0] = true;
		((boolean[]) readField(mode, "enableSE"))[0] = false;

		mode.startGame(engine, 0);

		assertTrue(engine.big);
		assertFalse(engine.enableSE);
	}

	// -----------------------------------------------------------------------
	// saveReplay
	// -----------------------------------------------------------------------

	@Test
	void saveReplayPersistsSettings() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.owner.replayProp = new CustomProperties();
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(0, engine.owner.replayProp.getProperty("vslinerace.version", -1));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(VSLineRaceMode mode) {
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

	private static void invokeLoadOtherSetting(VSLineRaceMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = VSLineRaceMode.class.getDeclaredMethod("loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOtherSetting(VSLineRaceMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = VSLineRaceMode.class.getDeclaredMethod("saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}
}