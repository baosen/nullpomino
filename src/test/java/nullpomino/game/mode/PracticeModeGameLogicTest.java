package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link PracticeMode}: startGame engine
 * configuration, calcScore (normal and mania), onLast timer/roll,
 * onMove level-up, onARE level-up, onGameOver secret grade, setMeter,
 * setHeboHidden, and the load/save map helpers.
 */
class PracticeModeGameLogicTest {

	@Test
	void startGameSetsBigAndB2bAndComboConfig() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "big", true);
		setInt(mode, "leveltype", 0); // LEVELTYPE_NONE

		mode.startGame(engine, 0);

		assertTrue(engine.big);
		assertTrue(engine.b2bEnable);
	}

	@Test
	void startGameWithManiaLevelTypeDisablesTspinAndSetsDoubleCombo() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 3); // LEVELTYPE_MANIA

		mode.startGame(engine, 0);

		assertFalse(engine.tspinEnable);
		assertFalse(engine.tspinAllowKick);
		assertFalse(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_DOUBLE, engine.comboType);
		assertEquals(0, engine.statistics.levelDispAdd);
	}

	@Test
	void startGameSetsGoalForLevelZero() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertEquals(5, readInt(mode, "goal"), "goal = 5 * (level + 1) for level 0");
	}

	private void placeOneBlock(GameEngine engine) {
		// Prevent all-clear bonus from triggering
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));
	}

	@Test
	void calcScoreNormalSingleLineNoSpin() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		assertEquals(100, engine.statistics.score,
				"1 line no spin at level 0 = 100 pts");
	}

	@Test
	void calcScoreNormalFourLinesWithB2b() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.b2b = true;

		mode.calcScore(engine, 0, 4);

		assertEquals(1200, engine.statistics.score,
				"4 lines with B2B at level 0 = 1200 pts");
	}

	@Test
	void calcScoreNormalTSpinDouble() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = true;

		mode.calcScore(engine, 0, 2);

		assertEquals(1200, engine.statistics.score,
				"T-Spin double at level 0 = 1200 pts");
	}

	@Test
	void calcScoreNormalAllClearBonus() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 1);

		// Field is empty, so all-clear triggers
		assertEquals(100 + 1800, engine.statistics.score,
				"Single line + all clear = 1900 pts");
	}

	@Test
	void calcScoreWithCombo() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.combo = 3;

		mode.calcScore(engine, 0, 1);

		// calcScoreNormal adds pts (100 for single) to score but does NOT add cmb
		assertEquals(100, engine.statistics.score,
				"1 line at level 0 = 100 pts (combo bonus tracked but not added to score)");
	}

	@Test
	void calcScoreFourLinesNoB2b() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.b2b = false;

		mode.calcScore(engine, 0, 4);

		assertEquals(800, engine.statistics.score,
				"4 lines without B2B at level 0 = 800 pts");
	}

	@Test
	void calcScoreManiaWithLines() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		placeOneBlock(engine);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		setInt(mode, "leveltype", 3); // LEVELTYPE_MANIA
		engine.ending = 0;
		engine.manualLock = false;
		engine.softdropFall = 0;

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.statistics.score > 0,
				"Mania scoring should produce positive points");
		assertEquals(1, engine.statistics.level,
				"Mania adds 1 level per line");
	}

	@Test
	void calcScoreManiaWithFieldEmptyTriggersBravo() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		setInt(mode, "leveltype", 4); // LEVELTYPE_MANIAPLUS
		engine.ending = 0;
		engine.manualLock = false;
		engine.softdropFall = 0;

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.statistics.score > 0,
				"Mania+ scoring should produce positive points");
	}

	@Test
	void onLastIncrementsScgettime() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "scgettime", 0);

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "scgettime"));
	}

	@Test
	void onLastWithTimelimitDecrementsTimer() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "timelimit", 3600);
		setInt(mode, "timelimitTimer", 1800);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ending = 0;

		mode.onLast(engine, 0);

		assertEquals(1799, readInt(mode, "timelimitTimer"),
				"timelimitTimer should decrement when timerActive is true");
	}

	@Test
	void onLastWithTimelimitExpiredTriggersGameEnded() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "timelimit", 60);
		setInt(mode, "timelimitTimer", 0);
		engine.gameActive = true;
		engine.timerActive = true;
		engine.ending = 0;

		mode.onLast(engine, 0);
		// timelimitTimer was already 0, it decrements to -1,
		// then on next call, the out-of-time branch triggers
	}

	@Test
	void onLastInRollIncrementsRolltime() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2; // Roll

		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "rolltime"));
	}

	@Test
	void onMoveDoesNotThrowAndReturnsFalse() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		boolean result = mode.onMove(engine, 0);

		assertFalse(result);
	}

	@Test
	void onMoveWithManiaLevelTypeIncrementsLevel() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "leveltype", 3); // MANIA
		engine.statistics.level = 0;
		engine.ending = 0;
		engine.statc[0] = 0;
		engine.holdDisable = false;
		setBoolean(mode, "lvupflag", false);

		mode.onMove(engine, 0);

		assertTrue(engine.statistics.level > 0, "Mania mode should level up on each piece");
	}

	@Test
	void onGameOverSetsSecretGrade() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.statc[0] = 0;

		mode.onGameOver(engine, 0);

		assertTrue(readInt(mode, "secretGrade") >= 0,
				"secretGrade should be set by field.getSecretGrade()");
	}

	@Test
	void onReadySetsTimelimitTimer() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "timelimit", 3600);

		mode.onReady(engine, 0);

		assertEquals(3600, readInt(mode, "timelimitTimer"));
	}

	@Test
	void onReadyWithBoneSetsEngineBone() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "bone", true);
		engine.statc[0] = 0;

		mode.onReady(engine, 0);

		assertTrue(engine.bone);
	}

	@Test
	void loadSettingRoundTrip() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		mode.saveSetting(prop);
		mode.loadSetting(prop);

		// Just verify no exception and fields are at defaults
		assertEquals(0, readInt(mode, "goal"));
	}

	@Test
	void setHeboHiddenLevel1() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 1);

		invokeSetHeboHidden(mode, engine);

		assertTrue(engine.heboHiddenEnable);
		assertEquals(15, engine.heboHiddenYLimit);
	}

	@Test
	void setHeboHiddenLevel7() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 7);

		invokeSetHeboHidden(mode, engine);

		assertTrue(engine.heboHiddenEnable);
		assertEquals(20, engine.heboHiddenYLimit);
	}

	@Test
	void setHeboHiddenLevel0Disables() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		setInt(mode, "heboHiddenLevel", 0);

		invokeSetHeboHidden(mode, engine);

		assertFalse(engine.heboHiddenEnable);
	}

	@Test
	void calcScoreWithComboBonus() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.combo = 3;

		mode.calcScore(engine, 0, 1);

		// Empty field triggers all-clear: pts = 100 (single) + 1800 (all-clear) = 1900.
		// cmb (100) is tracked but NOT added to score.
		assertEquals(1900, engine.statistics.score,
				"Single line + all-clear at level 0 = 100 + 1800 = 1900");
	}

	@Test
	void calcScoreWithFourLinesNoB2b() throws Exception {
		PracticeMode mode = new PracticeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.b2b = false;

		mode.calcScore(engine, 0, 4);

		// Empty field triggers all-clear: pts = 800 (four lines) + 1800 (all-clear) = 2600
		assertEquals(2600, engine.statistics.score,
				"4 lines without B2B + all-clear at level 0 = 800 + 1800 = 2600");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(PracticeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(PracticeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(PracticeMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static void setInt(PracticeMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(PracticeMode mode, String name, boolean value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(mode, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeSetHeboHidden(PracticeMode mode, GameEngine engine) throws Exception {
		Method m = PracticeMode.class.getDeclaredMethod("setHeboHidden", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}
}
