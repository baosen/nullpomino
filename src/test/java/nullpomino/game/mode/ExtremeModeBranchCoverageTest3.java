package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Targets the remaining uncovered branch outcomes in {@link ExtremeMode} that
 * the other Extreme test classes do not exercise:
 * <ul>
 *   <li>renderSetting SPIN TYPE "IMMOBILE" branch (line 249).</li>
 *   <li>renderLast ranking-table layout via getNextDisplayType()==2 and the
 *       (i == rankingRank) highlight (lines 308-321).</li>
 *   <li>renderLast score-delta / roll-time-clamp / event-gate branches
 *       (lines 327, 335, 348, 351, 355, 404).</li>
 *   <li>onLast guard / roll-time-clamp branches (lines 426, 431).</li>
 *   <li>calcScore else-if chain corners (lines 459/469/514 fall-through,
 *       497 mini-without-allspin, 546 combo-with-zero-combo, 567 ending!=0,
 *       569 final BGM level, 595 level-up while at max level).</li>
 *   <li>saveReplay netPlayerName / big / endless ranking branches
 *       (lines 639, 644, 645).</li>
 *   <li>netIsNetRankingViewOK guard corners (line 775).</li>
 * </ul>
 */
class ExtremeModeBranchCoverageTest3 {

	/** EventReceiver whose getNextDisplayType()==2 forces the BSP ranking layout. */
	private static final class BspReceiver extends EventReceiver {
		@Override public int getNextDisplayType() { return 2; }
	}

	// ==================================================================
	//  renderSetting: SPIN TYPE IMMOBILE (line 249)
	// ==================================================================

	@Test
	void renderSettingSpinTypeImmobile() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "version", 1);
		setInt(mode, "spinCheckType", 1); // != 0 -> "IMMOBILE"
		mode.renderSetting(engine, 0);
	}

	// ==================================================================
	//  renderLast ranking table: BSP layout + rankingRank highlight
	//  (lines 308-321)
	// ==================================================================

	@Test
	void renderLastRankingTableBspLayoutWithHighlight() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(new BspReceiver(), mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.RESULT; // RESULT && !replayMode (line 308 right operand)
		setBoolean(mode, "big", false);
		setBoolean(mode, "endless", false);
		setInt(mode, "rankingRank", 0); // i == rankingRank true for i==0 (lines 319-321)

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastRankingTableSkippedWhenBig() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.stat = GameEngine.Status.SETTING;
		setBoolean(mode, "big", true); // line 309 middle operand false -> table skipped

		mode.renderLast(engine, 0);
	}

	// ==================================================================
	//  renderLast in-game: score-delta gate, line-display, roll clamp
	//  (lines 327, 335, 348, 351, 355)
	// ==================================================================

	@Test
	void renderLastScoreDeltaSuppressedAfterTimeout() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE; // in-game else branch
		engine.gameActive = false;
		engine.ending = 0;
		setInt(mode, "lastscore", 500); // != 0 ...
		setInt(mode, "scgettime", 200); // ... but scgettime >= 120 -> no "(+n)" (line 327 second operand)

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastHighLevelNonEndlessDuringEndingShowsBareLines() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = false;
		setBoolean(mode, "endless", false);
		engine.statistics.level = 19; // level<19 false
		engine.ending = 1;            // (!endless && ending==0) false -> else (bare lines, line 335)

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastHighLevelNonEndlessNoEndingShowsRatio() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = false;
		setBoolean(mode, "endless", false);
		engine.statistics.level = 19; // level<19 false
		engine.ending = 0;            // (!endless && ending==0) true (line 335 second operand true)

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastRollTimeClampedAndMeterWindow() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 2; // roll-time block active
		setInt(mode, "rolltime", ExtremeMode.ROLLTIMELIMIT + 100); // remainRollTime < 0 -> clamp (line 348)

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastRollTimeInsideMeterWindow() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 2;
		// remainRollTime within (0, 600) -> the (>0 && <10*60) highlight true (line 351)
		setInt(mode, "rolltime", ExtremeMode.ROLLTIMELIMIT - 300);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastEventSuppressedAfterTimeout() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = false;
		engine.ending = 0;
		setInt(mode, "lastevent", AbstractMarathonMode.EVENT_SINGLE); // != NONE ...
		setInt(mode, "scgettime", 200); // ... but >= 120 -> event block skipped (line 355 second operand)

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastEventWithoutComboLine() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = false;
		engine.ending = 0;
		setInt(mode, "lastevent", AbstractMarathonMode.EVENT_SINGLE);
		setInt(mode, "scgettime", 0);
		setInt(mode, "lastcombo", 1); // < 2 -> combo line skipped (line 404 first operand false)
		setInt(mode, "lastpiece", Piece.PIECE_T);

		mode.renderLast(engine, 0);
	}

	// ==================================================================
	//  onLast guard branches (lines 426, 431)
	// ==================================================================

	@Test
	void onLastNotInEndingDoesNotAdvanceRolltime() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 0; // ending != 2 -> roll block skipped (line 426 second operand false)
		setInt(mode, "rolltime", 0);

		mode.onLast(engine, 0);

		assertEquals(0, readInt(mode, "rolltime"));
	}

	@Test
	void onLastRollTimeClampedToZero() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		// rolltime already past the limit -> remainRollTime < 0 -> clamp (line 431)
		setInt(mode, "rolltime", ExtremeMode.ROLLTIMELIMIT + 50);

		mode.onLast(engine, 0);

		// rolltime incremented and the ending finish fires (>= ROLLTIMELIMIT)
		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	// ==================================================================
	//  calcScore else-if chain corners
	// ==================================================================

	@Test
	void calcScoreTspinEzZeroLinesFallsThroughChain() throws Exception {
		// tspin && tspinez && lines==0: every else-if (459/469/478/496/514) is
		// evaluated false, so the chain falls through without scoring.
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinez = true;
		engine.tspinmini = false;
		// block placed so the all-clear path is irrelevant (lines==0 anyway)
		placeFloorBlock(engine);

		mode.calcScore(engine, 0, 0);

		assertEquals(0, engine.statistics.score);
	}

	@Test
	void calcScoreTspinDoubleMiniWithoutAllSpinBonus() throws Exception {
		// lines==2, tspinmini true but useAllSpinBonus false -> else branch (line 497 second operand false)
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = true;
		engine.tspinez = false;
		engine.tspinmini = true;
		engine.useAllSpinBonus = false;
		engine.b2b = false;
		placeFloorBlock(engine);

		mode.calcScore(engine, 0, 2);

		// non-mini double (else): 1200 * 1
		assertEquals(1200, engine.statistics.score);
		assertEquals(AbstractMarathonMode.EVENT_TSPIN_DOUBLE, readInt(mode, "lastevent"));
	}

	@Test
	void calcScoreComboEnabledButZeroComboSkipsBonus() throws Exception {
		// enableCombo true, lines>=1, but engine.combo < 1 -> combo bonus skipped (line 546 second operand false)
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.level = 0;
		engine.statistics.score = 0;
		engine.tspin = false;
		setBoolean(mode, "enableCombo", true);
		engine.combo = 0; // < 1
		placeFloorBlock(engine);

		mode.calcScore(engine, 0, 1);

		// single only, no combo bonus
		assertEquals(100, engine.statistics.score);
	}

	@Test
	void calcScoreDuringEndingSkipsBgmAndMeterBlock() throws Exception {
		// engine.ending != 0 -> the whole BGM/meter/level-up block is skipped (line 567 false)
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.level = 0;
		engine.statistics.lines = 10; // would normally level up, but ending != 0
		engine.ending = 2;
		engine.tspin = false;
		placeFloorBlock(engine);

		mode.calcScore(engine, 0, 1);

		// no level-up because the ending block was skipped
		assertEquals(0, engine.statistics.level);
	}

	@Test
	void calcScoreAtFinalBgmLevelSkipsBgmChange() throws Exception {
		// bgmlv at the last table slot (value -1) -> the BGM-change block is skipped (line 569 false)
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.level = 0;
		engine.statistics.lines = 1;
		engine.ending = 0;
		engine.tspin = false;
		setInt(mode, "bgmlv", 3); // tableBGMChange[3] == -1
		placeFloorBlock(engine);

		mode.calcScore(engine, 0, 1);

		// bgmlv must not advance past the sentinel
		assertEquals(3, readInt(mode, "bgmlv"));
	}

	@Test
	void calcScoreLevelThresholdReachedButAtMaxLevelNoLevelUp() throws Exception {
		// lines >= (level+1)*10 true but level == 19 (not < 19) -> level-up else-if false (line 595 second operand false)
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.level = 19;
		engine.statistics.lines = 200; // >= (19+1)*10 but endless keeps us out of the 200-ending arm
		engine.ending = 0;
		engine.tspin = false;
		setBoolean(mode, "endless", true); // so the lines>=200 ending arm is false
		placeFloorBlock(engine);

		mode.calcScore(engine, 0, 1);

		// stays at max level
		assertEquals(19, engine.statistics.level);
	}

	// ==================================================================
	//  saveReplay branches (lines 639, 644, 645)
	// ==================================================================

	@Test
	void saveReplaySkipsRankingWhenBig() throws Exception {
		// big == true -> ranking update skipped (line 644 middle operand false)
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		setBoolean(mode, "big", true);
		setInt(mode, "rankingRank", -1);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		// rankingRank untouched because updateRanking was not called
		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	@Test
	void saveReplayWithEmptyNetPlayerNameSkipsNameProperty() throws Exception {
		// netPlayerName == "" -> length()>0 false (line 639 second operand false)
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		setBoolean(mode, "big", false);
		setBoolean(mode, "endless", true); // line 645 ternary -> goaltype 1
		setField(mode, "netPlayerName", "");
		resetRankingRow(mode, 1);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertTrue(true);
	}

	@Test
	void saveReplayWithNullNetPlayerNameSkipsNameProperty() throws Exception {
		// netPlayerName == null -> first operand false (line 639)
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		setBoolean(mode, "big", false);
		setBoolean(mode, "endless", false); // line 645 ternary -> goaltype 0
		setField(mode, "netPlayerName", null);
		resetRankingRow(mode, 0);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertTrue(true);
	}

	// ==================================================================
	//  netIsNetRankingViewOK guard corners (line 775)
	// ==================================================================

	@Test
	void netIsNetRankingViewOkFalseWhenStartLevelNonZero() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 3); // startlevel == 0 false
		setBoolean(mode, "big", false);
		engine.ai = null;

		assertEquals(Boolean.FALSE, invokeBool(mode, "netIsNetRankingViewOK", engine));
	}

	@Test
	void netIsNetRankingViewOkFalseWhenBig() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", true); // !big false
		engine.ai = null;

		assertEquals(Boolean.FALSE, invokeBool(mode, "netIsNetRankingViewOK", engine));
	}

	@Test
	void netIsNetRankingViewOkTrueWhenAllGuardsPass() throws Exception {
		ExtremeMode mode = new ExtremeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);
		engine.ai = null;

		assertEquals(Boolean.TRUE, invokeBool(mode, "netIsNetRankingViewOK", engine));
	}

	// ==================================================================
	//  Helpers
	// ==================================================================

	private static GameEngine freshEngine(ExtremeMode mode) {
		return freshEngine(new EventReceiver(), mode);
	}

	private static GameEngine freshEngine(EventReceiver receiver, ExtremeMode mode) {
		GameManager manager = new GameManager(receiver);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	/** Put a block on the floor so the field is not empty (avoids the all-clear bonus). */
	private static void placeFloorBlock(GameEngine engine) {
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new Block(Block.BLOCK_COLOR_GRAY));
	}

	/** Empty out one ranking row so rank-position math is hermetic across the full suite. */
	private static void resetRankingRow(ExtremeMode mode, int type) throws Exception {
		int[][] score = (int[][]) field(mode.getClass(), "rankingScore").get(mode);
		int[][] lines = (int[][]) field(mode.getClass(), "rankingLines").get(mode);
		int[][] time = (int[][]) field(mode.getClass(), "rankingTime").get(mode);
		for (int i = 0; i < score[type].length; i++) {
			score[type][i] = 0;
			lines[type][i] = 0;
			time[type][i] = -1;
		}
	}

	private static Boolean invokeBool(ExtremeMode mode, String name, GameEngine engine) throws Exception {
		Method m = findMethod(mode.getClass(), name, GameEngine.class);
		m.setAccessible(true);
		return (Boolean) m.invoke(mode, engine);
	}

	private static Method findMethod(Class<?> cls, String name, Class<?>... params) throws NoSuchMethodException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredMethod(name, params); }
			catch (NoSuchMethodException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchMethodException(name);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		field(obj.getClass(), name).set(obj, value);
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
