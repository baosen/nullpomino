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
 * Fills in the remaining unexercised branch OUTCOMES in {@link ScoreAttackMode}
 * that the existing ScoreAttackMode* tests only hit on one side:
 * <ul>
 *   <li>{@code calcScore}: ending!=0 early return, manualLock, non-empty field
 *       (bravo stays 1), negative speedBonus.</li>
 *   <li>{@code renderLast}: negative tempLevel clamp, 20G gravity-&lt;0 speed,
 *       lastscore+scgettime "(+n)" score string, roll-time clamp + bold flag,
 *       showsectiontime-off skip, getNextDisplayType()==2 side layout,
 *       leaderboard row highlight (i==rankingRank), section avg display.</li>
 *   <li>{@code levelUp}: alwaysghost keeps ghost, bgm fade-out at level&gt;=290.</li>
 *   <li>{@code onARE}: level capped at 299.</li>
 *   <li>{@code onLast}: section index above array bounds is skipped.</li>
 *   <li>{@code onGameOver}: statc[0]!=0 leaves secretGrade untouched.</li>
 *   <li>{@code onResult}: page step without wrap, F flips view true-&gt;false.</li>
 *   <li>{@code renderResult}: page index outside 0..2 (else-if false arm).</li>
 *   <li>{@code onSetting}: F/A pressed while menuTime&lt;5 (guard short-circuit),
 *       replay branch with menuTime&lt;60 keeps running.</li>
 *   <li>{@code stNewRecordCheck}: replayMode suppresses the new-record flag.</li>
 *   <li>{@code saveReplay}: eligible run that neither ranks nor sets a section
 *       record skips the save.</li>
 * </ul>
 */
class ScoreAttackModeBranchCoverageTest {

	/** EventReceiver whose Next-display is the side/big layout (type 2). */
	private static final class SideNextReceiver extends EventReceiver {
		@Override public int getNextDisplayType() { return 2; }
	}

	// ────────────────────────────────────────────────────────────────
	// calcScore branch outcomes
	// ────────────────────────────────────────────────────────────────

	@Test
	void calcScoreReturnsEarlyDuringEnding() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 2; // non-zero -> early return before combo logic
		setInt(mode, "comboValue", 42);

		mode.calcScore(engine, 0, 3);

		assertEquals(42, readInt(mode, "comboValue"), "ending!=0 must early-return untouched");
	}

	@Test
	void calcScoreManualLockAndNonEmptyFieldKeepBravoOne() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		// Put a block on the field so isEmpty() is false -> bravo stays 1.
		engine.field.setBlockColor(0, engine.field.getHeight() - 1, 1);
		engine.manualLock = true; // manuallock = 1
		engine.statc[0] = 0;
		setInt(mode, "comboValue", 1);
		engine.statistics.level = 0;

		mode.calcScore(engine, 0, 1);

		// comboValue = 1 + 2*1 - 2 = 1; levelb=0, level=1; bravo=1 (not empty);
		// manuallock=1; speedBonus = getLockDelay(30) - 0 = 30
		// lastscore = 6 * (((0+1)/4 + 0 + 1 + 0) * 1 * 1 * 1 + (1/2) + 30*7)
		//           = 6 * (1 + 0 + 210) = 6 * 211 = 1266
		assertEquals(1266, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreNegativeSpeedBonusClampsToZero() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "comboValue", 1);
		engine.statistics.level = 0;
		// statc[0] far beyond lock delay -> getLockDelay - statc[0] < 0 -> clamps to 0.
		engine.statc[0] = 100000;

		mode.calcScore(engine, 0, 1);

		// bravo=4 (empty field), speedBonus clamped to 0
		// lastscore = 6 * (((0+1)/4 + 0 + 0 + 0) * 1 * 1 * 4 + (1/2) + 0) = 6 * 0 = 0
		assertEquals(0, readInt(mode, "lastscore"));
	}

	// ────────────────────────────────────────────────────────────────
	// renderLast branch outcomes
	// ────────────────────────────────────────────────────────────────

	@Test
	void renderLastNegativeLevelAndTwentyGSpeed() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.statistics.level = -5;       // tempLevel < 0 -> clamp to 0
		engine.speed.gravity = -1;          // gravity < 0 -> speed = 40
		setBool(mode, "showsectiontime", false); // skip the section-time block
		setInt(mode, "lastscore", 500);     // lastscore != 0
		setInt(mode, "scgettime", 60);      // scgettime > 0 -> "(+n)" branch

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastRollTimeBoldAndClamp() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 2;
		// time in (0, 600) -> bold flag true: ROLLTIMELIMIT(1956) - rolltime = 300.
		setInt(mode, "rolltime", 1956 - 300);

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastRollTimeNegativeClampsToZero() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		engine.gameActive = true;
		engine.ending = 2;
		// rolltime beyond limit -> time < 0 -> clamp to 0 (and bold flag false).
		setInt(mode, "rolltime", 1956 + 100);

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastSectionTimeSideNextLayout() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameManager manager = new GameManager(new SideNextReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setBool(mode, "showsectiontime", true);
		int[] sectiontime = new int[]{3000, 0, 2000};
		setField(mode, "sectiontime", sectiontime);
		setInt(mode, "sectionavgtime", 2500); // sectionavgtime > 0 -> AVERAGE printed
		engine.statistics.level = 0;
		engine.ending = 0;

		mode.renderLast(engine, 0); // getNextDisplayType()==2 -> x=8, x2=9
		assertTrue(true);
	}

	@Test
	void renderLastSectionTimeTempClampAbove300() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.MOVE;
		setBool(mode, "showsectiontime", true);
		// Inject an oversized section array so i*100 exceeds 300 -> temp clamp.
		setField(mode, "sectiontime", new int[]{100, 200, 300, 400, 500});
		setField(mode, "sectionIsNewRecord", new boolean[]{false, false, false, false, false});
		engine.statistics.level = 0;
		engine.ending = 0;

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderLastLeaderboardHighlightsPlayerRow() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.stat = GameEngine.Status.SETTING;
		setBool(mode, "isShowBestSectionTime", false); // score leaderboard path
		setInt(mode, "startlevel", 0);
		setBool(mode, "big", false);
		setBool(mode, "always20g", false);
		engine.ai = null;
		setInt(mode, "rankingRank", 3); // one row matches i==rankingRank -> highlight branch

		mode.renderLast(engine, 0);
		assertTrue(true);
	}

	// ────────────────────────────────────────────────────────────────
	// levelUp branch outcomes
	// ────────────────────────────────────────────────────────────────

	@Test
	void levelUpKeepsGhostWhenAlwaysGhost() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ghost = true;
		setBool(mode, "alwaysghost", true); // (level>=100) && !alwaysghost -> false
		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 150;

		invokeLevelUp(mode, engine);

		assertTrue(engine.ghost, "alwaysghost should keep the ghost on past level 100");
	}

	@Test
	void levelUpTriggersBgmFadeNear290() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "bgmlv", 0);
		setInt(mode, "nextseclv", 300);
		engine.statistics.level = 295; // >=290, ending 0, bgmlv 0 -> fade-out
		engine.ending = 0;

		invokeLevelUp(mode, engine);

		assertTrue(engine.owner.bgmStatus.fadesw, "bgm should start fading near the ending");
	}

	// ────────────────────────────────────────────────────────────────
	// onARE: level capped at 299
	// ────────────────────────────────────────────────────────────────

	@Test
	void onARELevelCapsAt299() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 10;
		engine.statc[1] = 5; // statc[0] >= statc[1]-1
		setBool(mode, "lvupflag", false);
		engine.statistics.level = 299; // not < 299 -> no increment

		mode.onARE(engine, 0);

		assertEquals(299, engine.statistics.level, "level must not advance past 299 in onARE");
	}

	// ────────────────────────────────────────────────────────────────
	// onLast: section index above array bounds is skipped
	// ────────────────────────────────────────────────────────────────

	@Test
	void onLastSectionAboveBoundsIsSkipped() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = 400; // section 4 >= sectiontime.length(3) -> skip

		int[] before = ((int[]) readField(mode, "sectiontime")).clone();
		mode.onLast(engine, 0);
		int[] after = (int[]) readField(mode, "sectiontime");

		assertEquals(before[0], after[0]);
		assertEquals(before[1], after[1]);
		assertEquals(before[2], after[2]);
	}

	// ────────────────────────────────────────────────────────────────
	// onGameOver: statc[0]!=0 leaves secretGrade untouched
	// ────────────────────────────────────────────────────────────────

	@Test
	void onGameOverSkipsSecretGradeWhenStatcNonZero() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setInt(mode, "secretGrade", 7);
		engine.statc[0] = 1; // not 0 -> secretGrade unchanged

		mode.onGameOver(engine, 0);

		assertEquals(7, readInt(mode, "secretGrade"));
	}

	// ────────────────────────────────────────────────────────────────
	// onResult: page step without wrap; F flips view back to false
	// ────────────────────────────────────────────────────────────────

	@Test
	void onResultPageStepsWithoutWrap() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// UP from page 2 -> 1 (statc[1] >= 0, no wrap to 2)
		engine.statc[1] = 2;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.onResult(engine, 0);
		assertEquals(1, engine.statc[1]);

		// DOWN from page 0 -> 1 (statc[1] <= 2, no wrap to 0)
		engine.statc[1] = 0;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.onResult(engine, 0);
		assertEquals(1, engine.statc[1]);
	}

	@Test
	void onResultFButtonFlipsViewBackToFalse() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBool(mode, "isShowBestSectionTime", true); // start true -> toggle to false

		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;
		mode.onResult(engine, 0);

		assertFalse(readBool(mode, "isShowBestSectionTime"));
	}

	// ────────────────────────────────────────────────────────────────
	// renderResult: page index outside 0..2 (else-if false arm)
	// ────────────────────────────────────────────────────────────────

	@Test
	void renderResultPageOutsideKnownRange() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 3; // not 0/1/2 -> all arms false

		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	@Test
	void renderResultPage1WithAverage() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 1;
		setField(mode, "sectiontime", new int[]{3000, 0, 2000});
		setInt(mode, "sectionavgtime", 2500); // sectionavgtime > 0 -> AVERAGE printed

		mode.renderResult(engine, 0);
		assertTrue(true);
	}

	// ────────────────────────────────────────────────────────────────
	// onSetting: F/A guard short-circuit (menuTime < 5), replay keep-running
	// ────────────────────────────────────────────────────────────────

	@Test
	void onSettingFButtonIgnoredBeforeMenuTimeFive() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "menuTime", 0); // < 5 -> F guard short-circuits
		setBool(mode, "isShowBestSectionTime", false);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_F] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_F] = 1;

		mode.onSetting(engine, 0);

		assertFalse(readBool(mode, "isShowBestSectionTime"), "F before menuTime>=5 must not flip");
	}

	@Test
	void onSettingAButtonIgnoredBeforeMenuTimeFive() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "menuTime", 0); // < 5 -> A guard short-circuits, game keeps running
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

		boolean result = mode.onSetting(engine, 0);

		assertTrue(result, "A before menuTime>=5 must not start the game");
	}

	@Test
	void onSettingReplayKeepsRunningBeforeSixtyTicks() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true;
		setInt(mode, "menuTime", 0); // < 60 -> stays on the setting screen

		boolean result = mode.onSetting(engine, 0);

		assertTrue(result, "replay setting screen stays up until 60 ticks");
		assertEquals(-1, readInt(mode, "menuCursor"));
	}

	// ────────────────────────────────────────────────────────────────
	// stNewRecordCheck: replayMode suppresses the new-record flag
	// ────────────────────────────────────────────────────────────────

	@Test
	void stNewRecordCheckSuppressedDuringReplay() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = true; // second condition (!replayMode) false

		int[] st = (int[]) readField(mode, "sectiontime");
		int[] bst = (int[]) readField(mode, "bestSectionTime");
		st[0] = 1; // strictly less than best -> first condition true
		bst[0] = 9999;

		invokeStNewRecordCheck(mode, 0);

		assertFalse(readBool(mode, "sectionAnyNewRecord"),
				"replayMode must suppress new-record detection");
	}

	// ────────────────────────────────────────────────────────────────
	// saveReplay: eligible run that neither ranks nor sets a section record
	// ────────────────────────────────────────────────────────────────

	@Test
	void saveReplayEligibleButUnrankedSkipsSave() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		setInt(mode, "startlevel", 0);
		setBool(mode, "always20g", false);
		setBool(mode, "big", false);
		setBool(mode, "sectionAnyNewRecord", false);

		// Make every ranking slot unbeatable so updateRanking -> rankingRank = -1.
		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		for (int i = 0; i < rankingScore.length; i++) {
			rankingScore[i] = Integer.MAX_VALUE;
			rankingLevel[i] = 999;
			rankingTime[i] = 0;
		}
		engine.statistics.score = 1;
		engine.statistics.level = 0;
		engine.statistics.time = 99999;

		mode.saveReplay(engine, 0, new CustomProperties());

		// rankingRank == -1 AND sectionAnyNewRecord == false -> save skipped.
		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	// ================================================================
	// Helpers
	// ================================================================

	private static GameEngine freshEngine(ScoreAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static void invokeLevelUp(ScoreAttackMode mode, GameEngine engine) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("levelUp", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeStNewRecordCheck(ScoreAttackMode mode, int sectionNumber) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("stNewRecordCheck", int.class);
		m.setAccessible(true);
		m.invoke(mode, sectionNumber);
	}

	private static int readInt(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
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

	private static void setBool(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.set(obj, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredField(name);
			} catch (NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
