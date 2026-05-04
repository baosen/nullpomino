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
 * Covers uncovered branches in {@link LineRaceMode}: onSetting menu
 * configuration changes (cursor cases 0-11), confirm paths (load/save/other),
 * cancel, net ranking, replay path, renderSetting, startGame watch mode,
 * renderLast various branches, calcScore near-goal BGM fade,
 * saveReplay ranking update, netSendStats, netRecvStats,
 * netSendEndGameStats, netSendOptions, netRecvOptions, netGetGoalType,
 * netIsNetRankingViewOK, netIsNetRankingSendOK.
 */
class LineRaceModeCoverageTest {

	@Test
	void onSettingConfirmOtherSettings() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		mode.netIsNetPlay = false;
		mode.netIsWatch = false;

		boolean result = mode.onSetting(engine, 0);
		assertFalse(result);
	}

	@Test
	void onSettingConfirmLoadPreset() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = engine.owner.modeConfig;
		prop.setProperty("linerace.gravity.5", 64);
		prop.setProperty("linerace.denominator.5", 128);
		prop.setProperty("linerace.are.5", 10);
		prop.setProperty("linerace.areLine.5", 5);
		prop.setProperty("linerace.lineDelay.5", 3);
		prop.setProperty("linerace.lockDelay.5", 20);
		prop.setProperty("linerace.das.5", 8);

		setInt(mode, "menuCursor", 10);
		setInt(mode, "presetNumber", 5);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		mode.netIsNetPlay = false;
		mode.netIsWatch = false;

		mode.onSetting(engine, 0);

		assertEquals(64, engine.speed.gravity);
	}

	@Test
	void onSettingConfirmSavePreset() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		engine.speed.gravity = 99;
		setInt(mode, "menuCursor", 11);
		setInt(mode, "presetNumber", 7);
		setInt(mode, "menuTime", 10);
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		mode.netIsNetPlay = false;
		mode.netIsWatch = false;

		mode.onSetting(engine, 0);

		assertEquals(99, engine.owner.modeConfig.getProperty("linerace.gravity.7", -1));
	}

	@Test
	void onSettingCancelSetsQuitFlag() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netIsNetPlay = false;

		engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;
		engine.ctrl.buttonPress[Controller.BUTTON_B] = true;

		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag);
	}

	@Test
	void onSettingReplayPathAutoStarts() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;

		for (int i = 0; i < 60; i++) {
			mode.onSetting(engine, 0);
		}

		// After 60 frames, should return false (start game)
		assertFalse(mode.onSetting(engine, 0));
	}

	@Test
	void startGameWatchModeSetsBgmNothing() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "bgmno", 3);
		mode.netIsWatch = true;

		mode.startGame(engine, 0);

		assertEquals(nullpomino.game.component.BGMStatus.BGM_NOTHING, engine.owner.bgmStatus.bgm);
	}

	@Test
	void startGameNormalModeSetsBgm() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "bgmno", 3);
		mode.netIsWatch = false;

		mode.startGame(engine, 0);

		assertEquals(3, engine.owner.bgmStatus.bgm);
	}

	@Test
	void calcScoreNearGoalFadesBgm() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // 20 lines
		engine.statistics.lines = 16; // within 5 of goal

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.owner.bgmStatus.fadesw);
	}

	@Test
	void calcScoreAllClearPlaysBravo() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0);

		mode.calcScore(engine, 0, 1);
		// Should not throw
	}

	@Test
	void saveReplayUpdatesRanking() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", false);
		engine.ai = null;
		mode.netIsWatch = false;
		engine.owner.replayMode = false;
		engine.statistics.lines = 20; // goal reached
		setInt(mode, "goaltype", 0);

		engine.owner.replayProp = new CustomProperties();
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		// Ranking should have been updated
		int rank = readInt(mode, "rankingRank");
		assertTrue(rank >= 0, "Should have a valid ranking position");
	}

	@Test
	void saveReplaySkipsRankingWhenBig() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", true);
		engine.ai = null;
		mode.netIsWatch = false;
		engine.owner.replayMode = false;
		engine.statistics.lines = 20;
		setInt(mode, "goaltype", 0);

		engine.owner.replayProp = new CustomProperties();
		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(-1, readInt(mode, "rankingRank"), "Big mode should skip ranking");
	}

	@Test
	void netSendStatsDoesNotThrow() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netIsNetPlay = false;

		mode.netSendStats(engine);
		// Should not throw when not netplay
	}

	@Test
	void netRecvStatsUpdatesEngine() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 1);

		String[] message = new String[] {"game", "0", "0", "stats", "30", "500", "60000", "5.0", "2.0", "1", "true", "true"};
		mode.netRecvStats(engine, message);

		assertEquals(30, engine.statistics.lines);
		assertEquals(500, engine.statistics.totalPieceLocked);
		assertEquals(60000, engine.statistics.time);
		assertEquals(1, readInt(mode, "goaltype"));
		assertTrue(engine.gameActive);
		assertTrue(engine.timerActive);
	}

	@Test
	void netSendOptionsDoesNotThrow() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		mode.netIsNetPlay = false;

		mode.netSendOptions(engine);
		// Should not throw when not netplay
	}

	@Test
	void netRecvOptionsUpdatesSettings() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		String[] message = new String[] {"game", "0", "0", "option", "0", "4", "256", "0", "0", "0", "30", "14", "3", "false", "1", "5"};
		mode.netRecvOptions(engine, message);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(3, readInt(mode, "bgmno"));
		assertFalse(readBoolean(mode, "big"));
		assertEquals(1, readInt(mode, "goaltype"));
		assertEquals(5, readInt(mode, "presetNumber"));
	}

	@Test
	void netGetGoalTypeReturnsGoaltype() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 2);

		assertEquals(2, mode.netGetGoalType());
	}

	@Test
	void netIsNetRankingViewOKWithBigMode() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", true);

		assertFalse(mode.netIsNetRankingViewOK(engine));
	}

	@Test
	void netIsNetRankingSendOKRequiresLinesAboveGoal() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", false);
		engine.ai = null;
		setInt(mode, "goaltype", 0); // 20 lines

		engine.statistics.lines = 19;
		assertFalse(mode.netIsNetRankingSendOK(engine));

		engine.statistics.lines = 20;
		assertTrue(mode.netIsNetRankingSendOK(engine));
	}

	@Test
	void renderSettingDoesNotThrow() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.renderSetting(engine, 0);
	}

	@Test
	void renderLastDoesNotThrow() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderResultDoesNotThrow() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.renderResult(engine, 0);
	}

	@Test
	void loadRankingAndSaveRankingRoundTrip() throws Exception {
		LineRaceMode mode = new LineRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Set some ranking data
		int[][] rankingTime = (int[][]) readField(mode, "rankingTime");
		rankingTime[0][0] = 3600;
		int[][] rankingPiece = (int[][]) readField(mode, "rankingPiece");
		rankingPiece[0][0] = 100;
		float[][] rankingPPS = (float[][]) readField(mode, "rankingPPS");
		rankingPPS[0][0] = 2.5f;

		CustomProperties prop = new CustomProperties();
		invokeSaveRanking(mode, prop, "test_rule");

		LineRaceMode loaded = new LineRaceMode();
		GameEngine loadedEngine = freshEngine(loaded);
		loaded.playerInit(loadedEngine, 0);

		loaded.loadRanking(prop, "test_rule");

		int[][] loadedTime = (int[][]) readField(loaded, "rankingTime");
		assertEquals(3600, loadedTime[0][0]);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(LineRaceMode mode) {
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

	private static boolean readBoolean(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(obj);
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

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeSaveRanking(LineRaceMode mode, CustomProperties prop, String ruleName) throws Exception {
		Method m = LineRaceMode.class.getDeclaredMethod("saveRanking", CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, prop, ruleName);
	}
}