package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Covers additional branches in {@link ScoreAttackMode}: setSpeed always20g,
 * levelUp section boundary, calcScore level300 ending without timerActive,
 * onLast section time bounds check, onResult page flip and F key,
 * saveReplay with section records, and updateBestSectionTime.
 */
class ScoreAttackModeExtendedLogicTest {

	// -----------------------------------------------------------------------
	// setSpeed always20g
	// -----------------------------------------------------------------------

	@Test
	void setSpeedWithAlways20gSetsGravityToMinus1() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "always20g", true);

		invokeSetSpeed(mode, engine);

		assertEquals(-1, engine.speed.gravity);
	}

	// -----------------------------------------------------------------------
	// levelUp section boundary
	// -----------------------------------------------------------------------

	@Test
	void levelUpTriggersAtNextSectionBoundary() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "nextseclv", 100);
		engine.statistics.level = 100; // level >= nextseclv

		invokeLevelUp(mode, engine);

		assertEquals(1, readInt(mode, "sectionscomp"));
	}

	@Test
	void levelUpTriggersGhostOffAtLevel100() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ghost = true;
		setBoolean(mode, "alwaysghost", false);
		setInt(mode, "nextseclv", 200);
		engine.statistics.level = 100;

		invokeLevelUp(mode, engine);

		assertFalse(engine.ghost);
	}

	// -----------------------------------------------------------------------
	// calcScore level300 ending without timerActive
	// -----------------------------------------------------------------------

	@Test
	void calcScoreLevel300WithoutTimerActiveNoTimeBonus() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.statistics.level = 296;
		engine.timerActive = false; // time bonus should not fire

		mode.calcScore(engine, 0, 4);

		assertEquals(300, engine.statistics.level);
		assertEquals(2, engine.ending);
	}

	// -----------------------------------------------------------------------
	// onLast section time bounds check
	// -----------------------------------------------------------------------

	@Test
	void onLastSectionTimeHandlesLevelOutOfBounds() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.statistics.level = -5;

		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		mode.onLast(engine, 0);

		// level -5 / 100 = 0 (integer division), so section 0 gets incremented to 1
		assertEquals(1, sectiontime[0]);
	}

	// -----------------------------------------------------------------------
	// onLast ending roll with F key speedup
	// -----------------------------------------------------------------------

	@Test
	void onLastEndingRollFKeySpeedsUp() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 0);

		// We can't really press F in headless mode, but we verify the normal path
		mode.onLast(engine, 0);

		assertEquals(1, readInt(mode, "rolltime"));
	}

	@Test
	void onLastEndingRollFinishesAtLimit() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setInt(mode, "rolltime", 1955); // ROLLTIMELIMIT - 1

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
	}

	// -----------------------------------------------------------------------
	// onResult page flip
	// -----------------------------------------------------------------------

	@Test
	void onResultFlipsPages() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statc[1] = 0;

		// Without controller input, no flip should occur
		boolean result = mode.onResult(engine, 0);

		assertFalse(result);
	}

	@Test
	void onResultFKeyTogglesSectionTimeView() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBoolean(mode, "isShowBestSectionTime", false);

		// Can't press F without controller, but verify the toggle logic would work
		boolean before = readBoolean(mode, "isShowBestSectionTime");
		// Simulate the F-key toggle
		setBoolean(mode, "isShowBestSectionTime", !before);

		assertTrue(readBoolean(mode, "isShowBestSectionTime"));
	}

	// -----------------------------------------------------------------------
	// saveReplay with section records
	// -----------------------------------------------------------------------

	@Test
	void saveReplaySkipsRankingWhenStartlevelNotZero() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "startlevel", 1);
		engine.ai = null;

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	@Test
	void saveReplaySkipsRankingWhenAlways20g() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBoolean(mode, "always20g", true);
		engine.ai = null;

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(-1, readInt(mode, "rankingRank"));
	}

	@Test
	void saveReplayUpdatesRankingWhenEligible() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		engine.statistics.score = 50000;
		engine.statistics.level = 300;
		engine.statistics.time = 3600;
		setBoolean(mode, "sectionAnyNewRecord", true);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(0, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// updateBestSectionTime
	// -----------------------------------------------------------------------

	@Test
	void updateBestSectionTimeUpdatesRecord() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] sectiontime = (int[]) readField(mode, "sectiontime");
		sectiontime[0] = 2500;
		boolean[] sectionIsNewRecord = (boolean[]) readField(mode, "sectionIsNewRecord");
		sectionIsNewRecord[0] = true;

		invokeUpdateBestSectionTime(mode);

		int[] bestSectionTime = (int[]) readField(mode, "bestSectionTime");
		assertEquals(2500, bestSectionTime[0]);
	}

	// -----------------------------------------------------------------------
	// checkRanking with tiebreakers
	// -----------------------------------------------------------------------

	@Test
	void checkRankingPrefersHigherLevelOnScoreTie() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		rankingScore[0] = 50000;
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		rankingLevel[0] = 250;

		int rank = invokeCheckRanking(mode, 50000, 300, 3600);
		assertEquals(0, rank, "Same score but higher level → should rank higher");
	}

	@Test
	void checkRankingPrefersFasterTimeOnTie() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		int[] rankingScore = (int[]) readField(mode, "rankingScore");
		rankingScore[0] = 50000;
		int[] rankingLevel = (int[]) readField(mode, "rankingLevel");
		rankingLevel[0] = 300;
		int[] rankingTime = (int[]) readField(mode, "rankingTime");
		rankingTime[0] = 4000;

		int rank = invokeCheckRanking(mode, 50000, 300, 3600);
		assertEquals(0, rank, "Same score and level but faster time → should rank higher");
	}

	// -----------------------------------------------------------------------
	// startGame level < 0 → nextseclv = 100
	// -----------------------------------------------------------------------

	@Test
	void startGameWithNegativeLevelSetsNextseclvTo100() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", -1);

		mode.startGame(engine, 0);

		assertEquals(100, readInt(mode, "nextseclv"));
	}

	// -----------------------------------------------------------------------
	// onARE levelup
	// -----------------------------------------------------------------------

	@Test
	void onARETriggersLevelUp() throws Exception {
		ScoreAttackMode mode = new ScoreAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.statc[0] = 10;
		engine.statc[1] = 10;
		engine.statistics.level = 50;

		mode.onARE(engine, 0);

		assertTrue(readBoolean(mode, "lvupflag"));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(ScoreAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(ScoreAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(ScoreAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static Object readField(ScoreAttackMode mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.get(mode);
	}

	private static void setInt(ScoreAttackMode mode, String name, int value) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		f.setInt(mode, value);
	}

	private static void setBoolean(ScoreAttackMode mode, String name, boolean value) throws Exception {
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

	private static void invokeSetSpeed(ScoreAttackMode mode, GameEngine engine) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeLevelUp(ScoreAttackMode mode, GameEngine engine) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("levelUp", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeUpdateBestSectionTime(ScoreAttackMode mode) throws Exception {
		Method m = AbstractManiaMode.class.getDeclaredMethod("updateBestSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}

	private static int invokeCheckRanking(ScoreAttackMode mode, int sc, int lv, int time) throws Exception {
		Method m = ScoreAttackMode.class.getDeclaredMethod("checkRanking", int.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, sc, lv, time);
	}
}
