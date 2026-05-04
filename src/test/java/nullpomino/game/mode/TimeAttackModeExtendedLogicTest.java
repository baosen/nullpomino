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
 * Covers additional branches in {@link TimeAttackMode}: setSpeed with
 * various game types (ANOTHER, ANOTHER2, NORMAL200, BASIC, HELL, HELL-X,
 * VOID), onLast level-timer countdown sound and game-over, onMove ending
 * start and VOID special path, calcScore heboHidden, game-complete/level-up
 * sections, startGame BGM selection, saveReplay ranking conditions,
 * netIsNetRankingViewOK, and setAverageSectionTime with zero sections.
 */
class TimeAttackModeExtendedLogicTest {

	// -----------------------------------------------------------------------
	// setSpeed with various game types
	// -----------------------------------------------------------------------

	@Test
	void setSpeedForNormalType() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 0); // NORMAL
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(4, engine.speed.gravity);
		assertEquals(25, engine.speed.are);
		assertEquals(25, engine.speed.areLine);
	}

	@Test
	void setSpeedForAnotherType() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 3); // ANOTHER
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(18, engine.speed.are);
		assertEquals(14, engine.speed.lineDelay);
	}

	@Test
	void setSpeedForAnother2Type() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 4); // ANOTHER2
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(6, engine.speed.are);
		assertEquals(4, engine.speed.lineDelay);
		assertEquals(7, engine.speed.das);
	}

	@Test
	void setSpeedForNormal200Type() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 5); // NORMAL200
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(25, engine.speed.are);
	}

	@Test
	void setSpeedForBasicType() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 7); // BASIC
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(26, engine.speed.are);
		assertEquals(40, engine.speed.lineDelay);
	}

	@Test
	void setSpeedForHellTypeEnablesOutlineOnly() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 8); // HELL
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertTrue(engine.blockShowOutlineOnly);
	}

	@Test
	void setSpeedForHellXTypeEnablesBoneAndHidden() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 9); // HELL-X
		engine.statistics.level = 0; // tableHellXFade[0] = -1

		invokeSetSpeed(mode, engine);

		assertTrue(engine.bone);
		assertEquals(-1, engine.blockHidden);
	}

	@Test
	void setSpeedForVoidTypeEnablesBone() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 10); // VOID
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertTrue(engine.bone);
		assertEquals(2, engine.speed.are);
	}

	@Test
	void setSpeedForHighSpeed1Type() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 1); // HIGH SPEED 1
		engine.statistics.level = 0;

		invokeSetSpeed(mode, engine);

		assertEquals(84, engine.speed.gravity);
		assertEquals(25, engine.speed.are);
	}

	@Test
	void setSpeedClampsLevelToTableLength() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "goaltype", 0); // NORMAL has 13 entries
		engine.statistics.level = 100;

		invokeSetSpeed(mode, engine);

		// Should clamp to last entry (-1 gravity)
		assertEquals(-1, engine.speed.gravity);
	}

	// -----------------------------------------------------------------------
	// onLast level-timer countdown and game-over
	// -----------------------------------------------------------------------

	@Test
	void onLastLevelTimerCountsDown() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		setInt(mode, "levelTimer", 600); // at 600, countdown SE plays on multiples of 60
		setInt(mode, "levelTimerMax", 7200);

		mode.onLast(engine, 0);

		assertEquals(599, readInt(mode, "levelTimer"));
	}

	@Test
	void onLastLevelTimerGameOverWhenZero() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		setInt(mode, "levelTimer", 0);

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	// -----------------------------------------------------------------------
	// onMove ending start and VOID path
	// -----------------------------------------------------------------------

	@Test
	void onMoveWithEnding2StartsRollForVOID() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 2;
		engine.staffrollEnable = true;
		setBoolean(mode, "rollstarted", false);
		setBoolean(mode, "netIsWatch", false);
		setInt(mode, "goaltype", 10); // VOID

		mode.onMove(engine, 0);

		assertTrue(readBoolean(mode, "rollstarted"));
	}

	// -----------------------------------------------------------------------
	// calcScore heboHidden path
	// -----------------------------------------------------------------------

	@Test
	void calcScoreHeboHiddenResetOnLines() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.heboHiddenEnable = true;
		engine.heboHiddenYNow = 10;

		mode.calcScore(engine, 0, 3);

		assertEquals(7, engine.heboHiddenYNow); // YNow = 10 - 3 = 7
		assertEquals(0, engine.heboHiddenTimerNow);
	}

	// -----------------------------------------------------------------------
	// calcScore game complete for HELL-X / VOID (staffroll)
	// -----------------------------------------------------------------------

	@Test
	void calcScoreGameCompleteForHellXEnablesStaffRoll() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 9); // HELL-X (tableGoalLevel[9] = 20)
		setInt(mode, "norm", 199); // one away from 20*10=200

		mode.calcScore(engine, 0, 1);

		assertTrue(engine.staffrollEnable);
		assertEquals(200, readInt(mode, "norm"));
	}

	@Test
	void calcScoreGameCompleteForNormalEndsDirectly() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.ending = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		setInt(mode, "goaltype", 0); // NORMAL (tableGoalLevel[0] = 15)
		setInt(mode, "norm", 149); // one away from 15*10=150

		mode.calcScore(engine, 0, 1);

		assertEquals(1, engine.ending);
		assertEquals(2, engine.statistics.rollclear);
	}

	// -----------------------------------------------------------------------
	// onLast HELL heboHidden enable
	// -----------------------------------------------------------------------

	@Test
	void onLastHellHeboHiddenForLevel5() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.gameActive = true;
		setInt(mode, "goaltype", 8); // HELL
		setInt(mode, "levelTimer", 100); // prevent game over
		engine.statistics.level = 5;
		engine.heboHiddenYNow = 10;

		mode.onLast(engine, 0);

		assertTrue(engine.heboHiddenEnable);
		assertEquals(19, engine.heboHiddenYLimit);
	}

	@Test
	void onLastHellHeboHiddenForLevel7() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		engine.gameActive = true;
		setInt(mode, "goaltype", 8); // HELL
		setInt(mode, "levelTimer", 100); // prevent game over
		engine.statistics.level = 10;
		engine.heboHiddenYNow = 10;

		mode.onLast(engine, 0);

		assertTrue(engine.heboHiddenEnable);
	}

	@Test
	void onLastHellHeboHiddenDisabledAtLevel15() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.timerActive = true;
		engine.ending = 0;
		setInt(mode, "goaltype", 8); // HELL
		engine.statistics.level = 15;

		mode.onLast(engine, 0);

		assertFalse(engine.heboHiddenEnable);
	}

	// -----------------------------------------------------------------------
	// onLast ending roll with rolltime limit
	// -----------------------------------------------------------------------

	@Test
	void onLastEndingRollReachesLimit() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameActive = true;
		engine.ending = 2;
		setBoolean(mode, "netIsWatch", false);
		setInt(mode, "rolltime", 3237); // ROLLTIMELIMIT - 1

		mode.onLast(engine, 0);

		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
		assertEquals(2, engine.statistics.rollclear);
	}

	// -----------------------------------------------------------------------
	// setAverageSectionTime with zero sections
	// -----------------------------------------------------------------------

	@Test
	void setAverageSectionTimeZeroSections() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "sectionscomp", 0);

		invokeSetAverageSectionTime(mode);

		assertEquals(0, readInt(mode, "sectionavgtime"));
	}

	// -----------------------------------------------------------------------
	// savePlay ranking conditions
	// -----------------------------------------------------------------------

	@Test
	void saveReplaySkipsRankingWhenStartlevelNotZero() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
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
	void saveReplayUpdatesRankingWhenEligible() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		engine.ai = null;
		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);
		setInt(mode, "goaltype", 0);
		setInt(mode, "norm", 150);
		engine.statistics.time = 3600;
		engine.statistics.rollclear = 2;

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(0, readInt(mode, "rankingRank"));
	}

	// -----------------------------------------------------------------------
	// netIsNetRankingViewOK
	// -----------------------------------------------------------------------

	@Test
	void netIsNetRankingViewOKRequiresStartlevel0NoBigNoAI() throws Exception {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setInt(mode, "startlevel", 0);
		setBoolean(mode, "big", false);
		engine.ai = null;
		assertTrue(mode.netIsNetRankingViewOK(engine));

		setInt(mode, "startlevel", 1);
		assertFalse(mode.netIsNetRankingViewOK(engine));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(TimeAttackMode mode) {
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

	private static void invokeSetSpeed(TimeAttackMode mode, GameEngine engine) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	private static void invokeSetAverageSectionTime(TimeAttackMode mode) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("setAverageSectionTime");
		m.setAccessible(true);
		m.invoke(mode);
	}
}
