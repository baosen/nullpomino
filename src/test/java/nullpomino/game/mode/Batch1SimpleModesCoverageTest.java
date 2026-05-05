package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage tests for simpler modes: VSLineRaceMode, VSDigRaceMode,
 * SquareMode, ScoreAttackMode, GradeManiaMode.
 *
 * Targets uncovered branches in onSetting, renderSetting, renderLast,
 * calcScore, renderResult, saveReplay, startGame, and helper methods.
 */
class Batch1SimpleModesCoverageTest {

	// =====================================================================
	// VSLineRaceMode
	// =====================================================================

	@Test
	void vsLineRaceModeInitAndGameFlow() throws Exception {
		VSLineRaceMode mode = new VSLineRaceMode();
		GameManager manager = twoPlayerManager(mode);

		assertEquals("VS-LINE RACE", mode.getName());
		assertTrue(mode.isVSMode());
		assertEquals(2, mode.getPlayers());

		// playerInit
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		assertEquals(2, manager.engine[0].framecolor); // FRAME_COLOR_RED
		assertEquals(0, manager.engine[1].framecolor); // FRAME_COLOR_BLUE

		// loadPreset / savePreset round trip
		CustomProperties prop = new CustomProperties();
		invoke(mode, "savePreset", manager.engine[0], prop, 5);
		VSLineRaceMode mode2 = new VSLineRaceMode();
		GameEngine e2 = freshEngine(mode2);
		invoke(mode2, "loadPreset", e2, prop, 5);
		assertEquals(4, e2.speed.gravity);

		// startGame
		set(mode, "big", new boolean[]{true, false});
		mode.startGame(manager.engine[0], 0);
		assertTrue(manager.engine[0].big);

		// calcScore
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
		mode.calcScore(manager.engine[0], 0, 1);
		assertEquals(GameEngine.METER_COLOR_GREEN, manager.engine[0].meterColor);

		// calcScore reaching goal
		set(mode, "goalLines", new int[]{5, 5});
		manager.engine[0].statistics.lines = 5;
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
		mode.calcScore(manager.engine[0], 0, 1);
		assertFalse(manager.engine[0].timerActive);

		// onLast - draw
		manager.engine[0].gameActive = true;
		manager.engine[0].stat = GameEngine.Status.GAMEOVER;
		manager.engine[1].stat = GameEngine.Status.GAMEOVER;
		mode.onLast(manager.engine[1], 1);
		assertEquals(-1, getInt(mode, "winnerID"));

		// onLast - 1P win
		manager.engine[0].gameActive = true;
		manager.engine[0].stat = GameEngine.Status.READY;
		manager.engine[1].stat = GameEngine.Status.GAMEOVER;
		mode.onLast(manager.engine[1], 1);
		assertEquals(0, getInt(mode, "winnerID"));

		// onLast - 2P win
		manager.engine[0].gameActive = true;
		manager.engine[0].stat = GameEngine.Status.GAMEOVER;
		manager.engine[1].stat = GameEngine.Status.READY;
		mode.onLast(manager.engine[1], 1);
		assertEquals(1, getInt(mode, "winnerID"));

		// renderSetting, renderLast, renderResult (no-op smoke tests)
		mode.renderSetting(manager.engine[0], 0);
		mode.renderLast(manager.engine[0], 0);
		mode.renderResult(manager.engine[0], 0);

		// saveReplay
		CustomProperties rp = new CustomProperties();
		mode.saveReplay(manager.engine[0], 0, rp);
	}

	// =====================================================================
	// VSDigRaceMode
	// =====================================================================

	@Test
	void vsDigRaceModeInitAndGameFlow() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = twoPlayerManager(mode);

		assertEquals("VS-DIG RACE", mode.getName());
		assertTrue(mode.isVSMode());
		assertEquals(2, mode.getPlayers());

		// modeInit + playerInit
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		assertEquals(2, manager.engine[0].framecolor);

		// loadPreset / savePreset
		CustomProperties prop = new CustomProperties();
		invoke(mode, "savePreset", manager.engine[0], prop, 0);
		VSDigRaceMode mode2 = new VSDigRaceMode();
		GameEngine e2 = freshEngine(mode2);
		invoke(mode2, "loadPreset", e2, prop, 0);
		assertEquals(4, e2.speed.gravity);

		// loadOtherSetting / saveOtherSetting
		invoke(mode, "saveOtherSetting", manager.engine[0], prop);
		invoke(mode2, "loadOtherSetting", e2, prop);

		// onReady with fillGarbage
		manager.engine[0].createFieldIfNeeded();
		boolean ret = mode.onReady(manager.engine[0], 0);
		assertFalse(ret);

		// startGame
		mode.startGame(manager.engine[0], 0);

		// calcScore
		mode.calcScore(manager.engine[0], 0, 0);
		assertTrue(manager.engine[0].meterValue >= 0);

		// calcScore win
		set(mode, "goalLines", new int[]{0, 0});
		manager.engine[0].createFieldIfNeeded();
		mode.calcScore(manager.engine[0], 0, 1);
		assertFalse(manager.engine[0].timerActive);
		assertEquals(GameEngine.Status.GAMEOVER, manager.engine[1].stat);

		// onLast
		mode.onLast(manager.engine[1], 1);

		// renderSetting (two branches: cursor < 9 and >= 9)
		set(mode, "menuCursor", 0);
		manager.engine[0].statc[4] = 0;
		mode.renderSetting(manager.engine[0], 0);
		set(mode, "menuCursor", 9);
		mode.renderSetting(manager.engine[0], 0);
		manager.engine[0].statc[4] = 1;
		mode.renderSetting(manager.engine[0], 0);

		// renderLast
		manager.engine[0].createFieldIfNeeded();
		mode.renderLast(manager.engine[0], 0);

		// renderResult
		mode.renderResult(manager.engine[0], 0);

		// saveReplay
		CustomProperties rp = new CustomProperties();
		mode.saveReplay(manager.engine[0], 0, rp);
	}

	// =====================================================================
	// SquareMode
	// =====================================================================

	@Test
	void squareModeInitAndGameFlow() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);

		assertEquals("SQUARE", mode.getName());

		// playerInit
		mode.playerInit(engine, 0);
		assertEquals(GameEngine.FRAME_COLOR_PURPLE, engine.framecolor);

		// loadSetting / saveSetting round trip
		CustomProperties prop = new CustomProperties();
		mode.saveSetting(prop);
		SquareMode mode2 = new SquareMode();
		mode2.loadSetting(prop);
		assertEquals(0, getInt(mode2, "gametype"));

		// startGame
		mode.startGame(engine, 0);
		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);

		// onMove
		assertFalse(mode.onMove(engine, 0));
		assertEquals(GameEngine.LineGravity.NATIVE, engine.lineGravityType);

		// calcScore (no lines)
		mode.calcScore(engine, 0, 0);
		assertEquals(0, engine.statistics.score);

		// calcScore with lines
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.statistics.score = 0;
		mode.calcScore(engine, 0, 1);
		assertEquals(1, engine.statistics.score); // pts = 1

		// calcScore with T-Spin
		set(mode, "version", 0);
		engine.tspin = true;
		engine.createFieldIfNeeded();
		engine.field.setBlock(5, 5, new Block(Block.BLOCK_COLOR_RED, engine.getSkin(), Block.BLOCK_ATTRIBUTE_VISIBLE));
		mode.calcScore(engine, 0, 1);

		// onLast (gametype 0 - marathon)
		engine.statistics.time = 100;
		mode.onLast(engine, 0);

		// onLast (gametype 1 - ultra)
		set(mode, "gametype", 1);
		engine.statistics.time = 100;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);

		// onLast (gametype 2 - sprint)
		set(mode, "gametype", 2);
		engine.statistics.score = 0;
		mode.onLast(engine, 0);

		// pieceLocked
		int sqBefore = getInt(mode, "squares");
		mode.pieceLocked(engine, 0, 0);
		assertTrue(getInt(mode, "squares") >= sqBefore);

		// renderSetting
		set(mode, "gametype", 0);
		mode.renderSetting(engine, 0);

		// renderLast (SETTING state)
		engine.stat = GameEngine.Status.SETTING;
		set(mode, "gametype", 0);
		mode.renderLast(engine, 0);
		set(mode, "gametype", 1);
		mode.renderLast(engine, 0);
		set(mode, "gametype", 2);
		mode.renderLast(engine, 0);

		// renderLast (active game state)
		engine.stat = GameEngine.Status.READY;
		engine.statistics.score = 1000;
		set(mode, "gametype", 0);
		mode.renderLast(engine, 0);
		set(mode, "gametype", 1);
		mode.renderLast(engine, 0);
		set(mode, "gametype", 2);
		mode.renderLast(engine, 0);

		// renderResult
		mode.renderResult(engine, 0);

		// onLineClear
		mode.onLineClear(engine, 0);

		// saveReplay
		CustomProperties rp = new CustomProperties();
		mode.saveReplay(engine, 0, rp);
	}

	// =====================================================================
	// ScoreAttackMode
	// =====================================================================

	@Test
	void scoreAttackModeInitAndGameFlow() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);

		assertEquals("SCORE ATTACK", mode.getName());

		// playerInit
		mode.playerInit(engine, 0);
		assertFalse(engine.tspinEnable);
		assertEquals(0, getInt(mode, "startlevel"));

		// setSpeed with always20g
		set(mode, "always20g", true);
		invoke(mode, "setSpeed", engine);
		assertEquals(-1, engine.speed.gravity);

		// setSpeed normal
		set(mode, "always20g", false);
		engine.statistics.level = 0;
		invoke(mode, "setSpeed", engine);
		assertEquals(4, engine.speed.gravity);

		// loadSetting / saveSetting
		CustomProperties prop = new CustomProperties();
		mode.saveSetting(prop);
		ScoreAttackMode mode2 = new ScoreAttackMode();
		mode2.loadSetting(prop);

		// setAverageSectionTime
		set(mode, "sectionscomp", 1);
		set(mode, "startlevel", 0);
		set(mode, "sectiontime", new int[]{3000, 4000, 5000});
		invoke(mode, "setAverageSectionTime");
		assertEquals(3000, getInt(mode, "sectionavgtime"));

		// stNewRecordCheck
		set(mode, "bestSectionTime", new int[]{5000, 5000, 5000});
		invoke(mode, "stNewRecordCheck", 0);
		assertTrue(getBool(mode, "sectionAnyNewRecord"));

		// startGame
		mode.startGame(engine, 0);

		// calcScore (no lines)
		engine.createFieldIfNeeded();
		mode.calcScore(engine, 0, 0);
		assertEquals(0, engine.statistics.score);

		// calcScore with lines
		engine.statistics.level = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		mode.calcScore(engine, 0, 1);
		assertTrue(engine.statistics.score >= 1);

		// onLast
		engine.statistics.time = 100;
		mode.onLast(engine, 0);

		// onLast with big
		set(mode, "big", true);
		engine.statistics.time = 100;
		mode.onLast(engine, 0);

		// renderSetting
		mode.renderSetting(engine, 0);

		// renderLast (SETTING state)
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
		engine.stat = GameEngine.Status.READY;
		mode.renderLast(engine, 0);

		// renderResult
		mode.renderResult(engine, 0);

		// saveReplay
		CustomProperties rp = new CustomProperties();
		mode.saveReplay(engine, 0, rp);

		// saveReplay with section new record
		set(mode, "sectionAnyNewRecord", true);
		mode.saveReplay(engine, 0, rp);
	}

	// =====================================================================
	// GradeManiaMode
	// =====================================================================

	@Test
	void gradeManiaModeInitAndGameFlow() throws Exception {
		GradeManiaMode mode = new GradeManiaMode();
		GameEngine engine = freshEngine(mode);

		assertEquals("GRADE MANIA", mode.getName());

		// playerInit
		mode.playerInit(engine, 0);
		assertFalse(engine.tspinEnable);
		assertEquals(25, engine.speed.are); // are is set to 25 in playerInit

		// setSpeed with always20g
		setMenuValue(findField(GradeManiaMode.class, "always20g"), mode, true);
		invoke(mode, "setSpeed", engine);
		assertEquals(-1, engine.speed.gravity);

		// setSpeed normal
		setMenuValue(findField(GradeManiaMode.class, "always20g"), mode, false);
		engine.statistics.level = 0;
		invoke(mode, "setSpeed", engine);
		assertEquals(4, engine.speed.gravity);

		// setAverageSectionTime
		set(mode, "sectionscomp", 1);
		invoke(mode, "setAverageSectionTime");
		assertEquals(0, getInt(mode, "sectionavgtime"));

		// stNewRecordCheck
		set(mode, "bestSectionTime", new int[]{5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000, 5000});
		set(mode, "sectiontime", new int[]{3000, 0, 0, 0, 0, 0, 0, 0, 0, 0});
		invoke(mode, "stNewRecordCheck", 0);
		assertTrue(getBool(mode, "sectionAnyNewRecord"));

		// GradeManiaMode does not have setSecretGrade method
		// invoke(mode, "setSecretGrade", engine);

		// startGame
		mode.startGame(engine, 0);

		// calcScore (no lines)
		engine.createFieldIfNeeded();
		mode.calcScore(engine, 0, 0);

		// calcScore with lines
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		mode.calcScore(engine, 0, 1);

		// onLast
		engine.statistics.time = 100;
		mode.onLast(engine, 0);

		// renderSetting
		mode.renderSetting(engine, 0);

		// renderLast (SETTING state, then active)
		engine.stat = GameEngine.Status.SETTING;
		mode.renderLast(engine, 0);
		engine.stat = GameEngine.Status.READY;
		mode.renderLast(engine, 0);

		// renderResult
		mode.renderResult(engine, 0);

		// saveReplay
		CustomProperties rp = new CustomProperties();
		mode.saveReplay(engine, 0, rp);
	}

	// =====================================================================
	// Helpers
	// =====================================================================

	private static GameManager twoPlayerManager(GameMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		return manager;
	}

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
		throw new NoSuchFieldException(name);
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
		// Handle primitives
		for (int i = 0; i < args.length; i++) {
			if (types[i] == Integer.class) types[i] = int.class;
			else if (types[i] == Boolean.class) types[i] = boolean.class;
			else if (types[i] == Float.class) types[i] = float.class;
		}
		Method m = findMethod(obj.getClass(), name, types);
		m.setAccessible(true);
		m.invoke(obj, args);
	}

	private static Method findMethod(Class<?> cls, String name, Class<?>... paramTypes) throws NoSuchMethodException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredMethod(name, paramTypes); }
			catch (NoSuchMethodException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchMethodException(name + " in " + cls.getName());
	}

	private static void setMenuValue(Field f, Object mode, boolean val) throws Exception {
		f.setAccessible(true);
		Object menuItem = f.get(mode);
		// The value field is declared in AbstractMenuItem - traverse hierarchy
		Class<?> c = menuItem.getClass();
		Field valF = null;
		while (c != null && valF == null) {
			try { valF = c.getDeclaredField("value"); } catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		if (valF == null) throw new NoSuchFieldException("value not found in hierarchy of " + menuItem.getClass());
		valF.setAccessible(true);
		if (valF.getType() == boolean.class) {
			valF.setBoolean(menuItem, val);
		} else if (valF.getType() == int.class) {
			valF.setInt(menuItem, val ? 1 : 0);
		} else {
			valF.set(menuItem, val);
		}
	}
}
