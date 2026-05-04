package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers additional branches in {@link MarathonMode}: startGame variations,
 * calcScore EZ-spin / double-mini / triple-no-b2b / BGM-change / pts-zero,
 * saveReplay ranking write conditions, and playerInit replay-branch edge
 * cases.
 */
class MarathonModeExtendedLogicTest {

	// -----------------------------------------------------------------------
	// startGame variations
	// -----------------------------------------------------------------------

	@Test
	void startGameWithAllSpinBonusEnablesUseAllSpinBonus() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "tspinEnableType", 2);
		setInt(mode, "version", 2);
		engine.readyDone = true; // normally false, but the method doesn't guard on it

		mode.startGame(engine, 0);

		assertTrue(engine.tspinEnable);
		assertTrue(engine.useAllSpinBonus);
		assertTrue(engine.tspinAllowKick);
	}

	@Test
	void startGameWithTSpinDisabled() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "tspinEnableType", 0);
		setInt(mode, "version", 2);

		mode.startGame(engine, 0);

		assertFalse(engine.tspinEnable);
	}

	@Test
	void startGameWithComboDisabled() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "enableCombo", false);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	@Test
	void startGameWithOldVersionUsesEnableTSpin() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1);
		setBoolean(mode, "enableTSpin", false);

		mode.startGame(engine, 0);

		assertFalse(engine.tspinEnable);
	}

	@Test
	void startGameWithNetIsWatchStopsBgm() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "netIsWatch", true);

		mode.startGame(engine, 0);

		assertEquals(BGMStatus.BGM_NOTHING, engine.owner.bgmStatus.bgm);
	}

	// -----------------------------------------------------------------------
	// calcScore EZ-spin / double-mini / triple / pts=0
	// -----------------------------------------------------------------------

	@Test
	void calcScoreTSpinEZWithB2B() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinez = true;
		engine.b2b = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// EZ spin B2B: 180 * (0+1) = 180
		assertEquals(12, readInt(mode, "lastevent")); // EVENT_TSPIN_EZ
		assertEquals(180, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinEZWithoutB2B() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinez = true;
		engine.b2b = false;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// EZ spin no B2B: 120 * (0+1) = 120
		assertEquals(12, readInt(mode, "lastevent")); // EVENT_TSPIN_EZ
		assertEquals(120, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinDoubleMini() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		// T-Spin double mini with useAllSpinBonus: 400 * (0+1) = 400
		assertEquals(9, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE_MINI
		assertEquals(400, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinDoubleMiniWithB2B() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = true;
		engine.b2b = true;
		engine.useAllSpinBonus = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		// T-Spin double mini B2B: 600 * (0+1) = 600
		assertEquals(9, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE_MINI
		assertEquals(600, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinDoubleMiniNoAllSpin() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.tspinmini = true;
		engine.useAllSpinBonus = false;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 2);

		// Without useAllSpinBonus, falls through to regular T-Spin double: 1200
		assertEquals(10, readInt(mode, "lastevent")); // EVENT_TSPIN_DOUBLE
		assertEquals(1200, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinTripleWithoutB2B() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.b2b = false;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 3);

		// T-Spin triple no B2B: 1600
		assertEquals(11, readInt(mode, "lastevent")); // EVENT_TSPIN_TRIPLE
		assertEquals(1600, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreTSpinTripleWithB2B() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.tspin = true;
		engine.b2b = true;
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 3);

		// T-Spin triple B2B: 2400
		assertEquals(11, readInt(mode, "lastevent")); // EVENT_TSPIN_TRIPLE
		assertEquals(2400, readInt(mode, "lastscore"));
	}

	@Test
	void calcScorePtsZeroDoesNotUpdateScoreVars() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.score = 999;
		setInt(mode, "lastscore", 888);
		setInt(mode, "scgettime", 50);

		// 0 lines and no T-Spin → pts stays 0
		mode.calcScore(engine, 0, 0);

		// lastscore and scgettime must remain unchanged
		assertEquals(888, readInt(mode, "lastscore"));
		assertEquals(50, readInt(mode, "scgettime"));
		assertEquals(999, engine.statistics.score);
	}

	@Test
	void calcScoreComboNotAppliedWhenLinesZero() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		engine.statistics.score = 500;
		engine.combo = 3;
		setBoolean(mode, "enableCombo", true);

		mode.calcScore(engine, 0, 0);

		// No lines → combo should not be added
		assertEquals(500, engine.statistics.score);
	}

	@Test
	void calcScoreBGMChangeTriggersAtThreshold() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		setInt(mode, "goaltype", 0); // 150 lines goal, tableBGMChange={50,100,150,200,-1}
		engine.statistics.lines = 50; // first BGM change at 50 lines

		mode.calcScore(engine, 0, 1);

		// bgmlv should have been incremented from 0 to 1, BGM should not be fading
		assertEquals(1, readInt(mode, "bgmlv"));
		assertFalse(engine.owner.bgmStatus.fadesw);
	}

	@Test
	void calcScoreBGMFadeoutBeforeChange() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		setInt(mode, "goaltype", 0);
		engine.statistics.lines = 46; // within 5 of tableBGMChange[0]=50

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.owner.bgmStatus.fadesw);
	}

	@Test
	void calcScoreDoesNotChangeBGMForEndlessGoal() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 0;
		setInt(mode, "goaltype", 2); // ENDLESS - tableGameClearLines[2] = -1
		engine.statistics.lines = 50;

		mode.calcScore(engine, 0, 1);

		// BGM change should still happen for endless mode (tableBGMChange is independent)
		assertEquals(1, readInt(mode, "bgmlv"));
	}

	@Test
	void calcScoreEndlessGoalStopsAtLevelUp() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
		engine.statistics.level = 18;
		engine.statistics.lines = 190;
		setInt(mode, "goaltype", 2); // ENDLESS

		mode.calcScore(engine, 0, 1);

		// With 190 lines and adding 1 more, we hit 191 which is >= (18+1)*10 = 190 → level up to 19
		assertEquals(19, engine.statistics.level);
	}

	// -----------------------------------------------------------------------
	// saveReplay ranking write conditions
	// -----------------------------------------------------------------------

	@Test
	void saveReplaySkipsRankingWhenBigEnabled() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBoolean(mode, "big", true);
		engine.ai = new nullpomino.game.ai.DummyAI();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.score = 50000;
		engine.statistics.lines = 200;
		engine.statistics.time = 3600;

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		// rankingRank should still be -1 because big=true
		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// netGetGoalType / netIsNetRankingViewOK
	// -----------------------------------------------------------------------

	@Test
	void netGetGoalTypeReturnsGoaltype() throws Exception {
		MarathonMode mode = new MarathonMode();
		setInt(mode, "goaltype", 1);

		assertEquals(1, invokeNetGetGoalType(mode));
	}

	@Test
	void netIsNetRankingViewOKRequiresStartlevelZeroAndNoBigAndNoAI() throws Exception {
		MarathonMode mode = new MarathonMode();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);
		engine.ai = null;
		assertTrue(mode.netIsNetRankingViewOK(engine));

		setInt(mode, "startlevel", 1);
		assertFalse(mode.netIsNetRankingViewOK(engine));

		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", true);
		assertFalse(mode.netIsNetRankingViewOK(engine));
	}

	// -----------------------------------------------------------------------
	// helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(MarathonMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(MarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(MarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(MarathonMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(MarathonMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(MarathonMode mode, String name, boolean value) throws Exception {
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

	private static int invokeNetGetGoalType(MarathonMode mode) throws Exception {
		Method m = MarathonMode.class.getDeclaredMethod("netGetGoalType");
		m.setAccessible(true);
		return (int) m.invoke(mode);
	}
}
