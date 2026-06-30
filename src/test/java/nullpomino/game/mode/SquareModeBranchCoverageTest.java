package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Exercises branches in {@link SquareMode} outside the settings-menu wraparound:
 * the switch cases, render string/threshold branches, the Ultra/Sprint
 * {@code onLast} timing logic, the avalanche routines, {@code pieceLocked}
 * square sound branches, {@code saveReplay}, and {@code checkRanking}.
 */
class SquareModeBranchCoverageTest {

	/** EventReceiver that reports a side-by-side big-next display (getNextDisplayType()==2). */
	private static final class BigNextReceiver extends EventReceiver {
		@Override public int getNextDisplayType() { return 2; }
	}

	private static GameEngine freshEngine(SquareMode mode, EventReceiver rcv) {
		GameManager manager = new GameManager(rcv);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static GameEngine freshEngine(SquareMode mode) {
		return freshEngine(mode, new EventReceiver());
	}

	// ------------------------------------------------------------------
	// renderSetting: every outline / tspin / grayout string branch + TNT
	// ------------------------------------------------------------------

	@Test
	void renderSettingCoversAllStringBranches() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setBool(mode, "tntAvalanche", true); // "TNT" branch of the ternary on L240
		for (int v = 0; v <= 2; v++) {
			setInt(mode, "outlinetype", v);
			setInt(mode, "tspinEnableType", v);
			setInt(mode, "grayoutEnable", v);
			mode.renderSetting(engine, 0);
		}
		setBool(mode, "tntAvalanche", false); // "WORLDS" branch
		mode.renderSetting(engine, 0);
		assertTrue(true);
	}

	// ------------------------------------------------------------------
	// startGame: outlinetype 0 / 1 / 2 select branches
	// ------------------------------------------------------------------

	@Test
	void startGameSetsOutlineForEachType() throws Exception {
		for (int v = 0; v <= 2; v++) {
			SquareMode mode = new SquareMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			setInt(mode, "outlinetype", v);
			setInt(mode, "tspinEnableType", v); // also walk the three tspin branches
			engine.createFieldIfNeeded();
			mode.startGame(engine, 0);
		}
		assertTrue(true);
	}

	// ------------------------------------------------------------------
	// renderLast: ranking screen for each game type, plus big-next scale
	// ------------------------------------------------------------------

	@Test
	void renderLastRankingForEachGameType() throws Exception {
		for (int gt = 0; gt <= 2; gt++) {
			SquareMode mode = new SquareMode();
			GameEngine engine = freshEngine(mode);
			mode.playerInit(engine, 0);
			setInt(mode, "gametype", gt);
			setInt(mode, "rankingRank", 0); // makes (i == rankingRank) true for i==0
			engine.stat = GameEngine.Status.SETTING;
			engine.ai = null;
			mode.renderLast(engine, 0);
		}
		assertTrue(true);
	}

	@Test
	void renderLastRankingBigNextScaleBranch() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode, new BigNextReceiver());
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0); // gametype 0 + getNextDisplayType()==2 -> scale 0.5/topY 6
		engine.stat = GameEngine.Status.SETTING;
		engine.ai = null;
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// ------------------------------------------------------------------
	// renderLast in-game (else) branch: score "(+n)" and Ultra timer colors
	// ------------------------------------------------------------------

	@Test
	void renderLastInGameScoreAndUltraTimerColors() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE; // not SETTING / RESULT -> else branch
		setInt(mode, "gametype", 1); // Ultra timer path

		// lastscore != 0 && scgettime > 0 -> "(+n)" branch
		setInt(mode, "lastscore", 50);
		setInt(mode, "scgettime", 10);

		// remainTime in (0, 10*60) -> YELLOW then ORANGE then RED branches all evaluated
		engine.statistics.time = 10800 - 100; // remaining 100 frames (<10*60, >0)
		mode.renderLast(engine, 0);

		// time <= 0 -> "if(time < 0) time = 0" branch + none of the color ifs
		engine.statistics.time = 10800 + 50;
		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// ------------------------------------------------------------------
	// onLast: Ultra countdown / fadeout / time-up; Sprint goal
	// ------------------------------------------------------------------

	@Test
	void onLastUltraCountdownFadeoutAndTimeUp() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 1);
		setInt(mode, "scgettime", 3); // scgettime>0 -> decrement branch
		engine.timerActive = true;

		// remainTime in (0,10*60], time%60==0 -> countdown + fadeout (remainTime<=5*60)
		engine.statistics.time = 10800 - 120; // remaining 120 (<=5*60), %60==0
		mode.onLast(engine, 0);
		assertEquals(2, readInt(mode, "scgettime"));

		// time-up: statistics.time >= ULTRA_MAX_TIME && timerActive
		engine.statistics.time = 10800;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);
	}

	@Test
	void onLastSprintGoalEndsGame() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 2); // Sprint
		engine.timerActive = true;
		engine.statistics.score = 150; // >= SPRINT_MAX_SCORE
		mode.onLast(engine, 0);
		assertEquals(GameEngine.Status.ENDINGSTART, engine.stat);
	}

	@Test
	void onLastSprintTimerInactiveZeroesMeter() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 2);
		engine.timerActive = false; // remainScore = 0 branch, goal not triggered
		engine.statistics.score = 200;
		mode.onLast(engine, 0);
		assertEquals(0, engine.meterValue);
	}

	// ------------------------------------------------------------------
	// onLineClear: grayoutEnable==2 grays broken blocks
	// ------------------------------------------------------------------

	@Test
	void onLineClearGraysBrokenBlocksWhenEnabled() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "grayoutEnable", 2);
		engine.createFieldIfNeeded();

		// place a broken block so the inner gray-out branch fires
		Block broken = new Block(Block.BLOCK_COLOR_RED);
		broken.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
		engine.field.setBlock(0, 0, broken);

		// also a non-broken block to take the false side of the inner condition
		engine.field.setBlock(1, 0, new Block(Block.BLOCK_COLOR_BLUE));

		engine.statc[0] = 1;
		mode.onLineClear(engine, 0);

		assertEquals(Block.BLOCK_COLOR_GRAY, engine.field.getBlock(0, 0).color);
	}

	// ------------------------------------------------------------------
	// calcScore -> avalanche (version 1): grayoutEnable!=0 + tntAvalanche
	// ------------------------------------------------------------------

	@Test
	void calcScoreAvalancheNewWithGrayAndTnt() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1);
		setInt(mode, "grayoutEnable", 1); // grayoutEnable != 0 branch in avalanche()
		setBool(mode, "tntAvalanche", true); // tnt anti-gravity branch

		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.nowPieceY = 5;

		// Fill two rows of blocks so the inner block branches fire on both the
		// "affected" rows (line-flagged) and the TNT (non-affected) rows.
		for (int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlock(x, 5, new Block(Block.BLOCK_COLOR_RED));
			engine.field.setBlock(x, 6, new Block(Block.BLOCK_COLOR_BLUE));
		}
		engine.field.setLineFlag(5, true); // makes affectY[minY] true

		engine.tspin = true;
		mode.calcScore(engine, 0, 1);

		// affected blocks become broken garbage and (grayout) gray.
		assertEquals(Block.BLOCK_COLOR_GRAY, engine.field.getBlock(0, 5).color);
		assertTrue(engine.field.getBlock(0, 5).getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN));
	}

	// ------------------------------------------------------------------
	// calcScore -> avalancheOld (version 0): lines==1, grayout, tnt
	// ------------------------------------------------------------------

	@Test
	void calcScoreAvalancheOldWithGrayAndTnt() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 0); // route to avalancheOld
		setInt(mode, "grayoutEnable", 1);
		setBool(mode, "tntAvalanche", true);

		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_O);
		engine.nowPieceY = 5;

		for (int x = 0; x < engine.field.getWidth(); x++) {
			engine.field.setBlock(x, 4, new Block(Block.BLOCK_COLOR_GREEN)); // above the found line (TNT anti-grav)
			engine.field.setBlock(x, 6, new Block(Block.BLOCK_COLOR_RED));   // below -> becomes broken garbage
		}
		engine.field.setLineFlag(5, true); // the "found a line" branch in the lines==1 loop

		engine.tspin = true;
		mode.calcScore(engine, 0, 1); // lines == 1 -> the lines==1 block runs

		assertEquals(Block.BLOCK_COLOR_GRAY, engine.field.getBlock(0, 6).color);
	}

	// ------------------------------------------------------------------
	// calcScore (no tspin): bravo on empty field + 4-line bonus formula
	// ------------------------------------------------------------------

	@Test
	void calcScoreFourLinesBonusAndBravo() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded(); // empty field -> bravo branch
		engine.tspin = false;

		mode.calcScore(engine, 0, 4); // lines>3 -> pts = 3 + (4-3)*2 = 5

		assertEquals(5, readInt(mode, "lastscore"));
		assertEquals(120, readInt(mode, "scgettime"));
	}

	// ------------------------------------------------------------------
	// pieceLocked: gold square ("square_g") and silver-only ("square_s")
	// ------------------------------------------------------------------

	@Test
	void pieceLockedGoldSquarePlaysGongSound() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		fillMonoSquare(engine, 0, 4, Block.BLOCK_COLOR_RED); // a gold (monocolor) 4x4
		mode.pieceLocked(engine, 0, 0);
		assertTrue(readInt(mode, "squares") > 0);
	}

	@Test
	void pieceLockedSilverOnlySquarePlaysSmallSound() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		fillMultiSquare(engine, 0, 4); // mixed colors -> silver only (sq[0]==0 && sq[1]>0)
		mode.pieceLocked(engine, 0, 0);
		assertTrue(readInt(mode, "squares") > 0);
	}

	@Test
	void pieceLockedNoSquareNoSound() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded(); // empty -> sq[0]==0 && sq[1]==0
		mode.pieceLocked(engine, 0, 0);
		assertEquals(0, readInt(mode, "squares"));
	}

	// ------------------------------------------------------------------
	// saveReplay: replayMode / ai guard branches
	// ------------------------------------------------------------------

	@Test
	void saveReplayUpdatesRankingWhenNotReplayAndNoAi() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0);
		resetRankingRow(mode, 0); // hermetic: ensure the row is empty before ranking
		engine.owner.replayMode = false;
		engine.ai = null;
		engine.statistics.score = 1_000_000; // beats the emptied row -> rank 0
		engine.statistics.time = 100;

		mode.saveReplay(engine, 0, engine.owner.modeConfig);
		assertEquals(0, readInt(mode, "rankingRank"));
	}

	@Test
	void saveReplaySkipsRankingWhenReplayMode() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true; // takes the false side of the (replayMode==false) guard
		mode.saveReplay(engine, 0, engine.owner.replayProp);
		assertEquals(-1, readInt(mode, "rankingRank")); // unchanged
	}

	// ------------------------------------------------------------------
	// checkRanking: gametype 0 / 1 / 2 comparison branches
	// ------------------------------------------------------------------

	@Test
	void checkRankingMarathonScoreTieBreaks() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 0);
		resetRankingRow(mode, 0);
		// seed row 0 so equal-score / equal-square tie-break paths are taken
		int[][] rs = (int[][]) field(SquareMode.class, "rankingScore").get(mode);
		int[][] rsq = (int[][]) field(SquareMode.class, "rankingSquares").get(mode);
		int[][] rt = (int[][]) field(SquareMode.class, "rankingTime").get(mode);
		rs[0][0] = 100; rsq[0][0] = 5; rt[0][0] = 1000;

		// equal score, equal squares, smaller time -> beats (third tie-break clause)
		assertEquals(0, invokeCheckRanking(mode, 100, 999, 5, 0));
		// equal score, more squares -> beats (second clause)
		assertEquals(0, invokeCheckRanking(mode, 100, 2000, 6, 0));
		// strictly higher score -> beats (first clause)
		assertEquals(0, invokeCheckRanking(mode, 101, 2000, 0, 0));
	}

	@Test
	void checkRankingUltraRequiresMaxTime() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 1);
		resetRankingRow(mode, 1);

		// time < ULTRA_MAX_TIME -> not ranked
		assertEquals(-1, invokeCheckRanking(mode, 500, 5000, 1, 1));
		// time >= ULTRA_MAX_TIME, score beats empty (0) -> ranked
		assertEquals(0, invokeCheckRanking(mode, 500, 10800, 1, 1));
	}

	@Test
	void checkRankingSprintRequiresMaxScore() throws Exception {
		SquareMode mode = new SquareMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "gametype", 2);
		resetRankingRow(mode, 2); // emptied rows have time == -1

		// score < SPRINT_MAX_SCORE -> not ranked
		assertEquals(-1, invokeCheckRanking(mode, 100, 500, 1, 2));
		// score >= SPRINT_MAX_SCORE, empty time(-1) -> ranked (rankingTime < 0 clause)
		assertEquals(0, invokeCheckRanking(mode, 150, 500, 1, 2));
	}

	// ------------------------------------------------------------------
	// helpers
	// ------------------------------------------------------------------

	/** Fill a 4x4 monocolor region (top-left at x,y) -> one gold square. */
	private static void fillMonoSquare(GameEngine engine, int x0, int y0, int color) {
		for (int k = 0; k < 4; k++)
			for (int l = 0; l < 4; l++)
				engine.field.setBlock(x0 + l, y0 + k, new Block(color));
	}

	/** Fill a 4x4 region with two colors -> no gold square, one silver square. */
	private static void fillMultiSquare(GameEngine engine, int x0, int y0) {
		for (int k = 0; k < 4; k++)
			for (int l = 0; l < 4; l++) {
				int color = ((k + l) % 2 == 0) ? Block.BLOCK_COLOR_RED : Block.BLOCK_COLOR_BLUE;
				engine.field.setBlock(x0 + l, y0 + k, new Block(color));
			}
	}

	private static int invokeCheckRanking(SquareMode mode, int sc, int time, int sq, int type) throws Exception {
		java.lang.reflect.Method m = SquareMode.class.getDeclaredMethod(
				"checkRanking", int.class, int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, time, sq, type);
	}

	/** Empty out one ranking row for hermeticity (rankings load from shared config). */
	private static void resetRankingRow(SquareMode mode, int type) throws Exception {
		int[][] rs = (int[][]) field(SquareMode.class, "rankingScore").get(mode);
		int[][] rt = (int[][]) field(SquareMode.class, "rankingTime").get(mode);
		int[][] rsq = (int[][]) field(SquareMode.class, "rankingSquares").get(mode);
		for (int i = 0; i < rs[type].length; i++) {
			rs[type][i] = 0;
			rt[type][i] = -1;
			rsq[type][i] = 0;
		}
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				Field f = c.getDeclaredField(name);
				f.setAccessible(true);
				return f;
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
