package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Targets the remaining uncovered conditional outcomes in
 * {@link DigChallengeMode} that the wraparound and existing suites miss:
 * the secondary outcomes of the compound onSetting confirm/cancel guards,
 * renderLast score / event display when the new-score window has expired,
 * the meter-color thresholds at intermediate meter values, calcScore B2B
 * T-Spin-triple-with-all-spin and zero-combo index paths, getGarbageMaxTime
 * level clamp, the addGarbage level-cap loop guard, saveReplay's empty
 * player-name and non-zero-startlevel paths, and the side-big ranking
 * display with a highlighted current rank.
 */
class DigChallengeModeBranchCoverageTest {

	// ---------------------------------------------------------------
	// onSetting compound-guard secondary outcomes
	// ---------------------------------------------------------------

	@Test
	void onSettingAPushedButMenuTimeTooLowDoesNotStart() throws Exception {
		// L289: isPush(A) true but menuTime < 5 -> game must NOT start.
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0); // below the >=5 threshold
		pressKey(engine, Controller.BUTTON_A);

		boolean result = mode.onSetting(engine, 0);

		assertTrue(result, "A with menuTime<5 should keep the settings screen open");
	}

	@Test
	void onSettingBPushedInNetPlayDoesNotQuit() throws Exception {
		// L303: isPush(B) true but netIsNetPlay true -> quitflag must stay false.
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBool(mode, "netIsNetPlay", true);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_B);

		mode.onSetting(engine, 0);

		assertFalse(engine.quitflag, "B in net play should not set quitflag");
	}

	@Test
	void onSettingDPushedOfflineDoesNotEnterNetRanking() throws Exception {
		// L308: isPush(D) true but netIsNetPlay false -> net ranking not entered.
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setBool(mode, "netIsNetPlay", false);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 10);
		pressKey(engine, Controller.BUTTON_D);

		mode.onSetting(engine, 0);

		assertFalse(readBool(mode, "netIsNetRankingDisplayMode"),
				"D offline should not enter net ranking screen");
	}

	// ---------------------------------------------------------------
	// renderLast: new-score window expired (scgettime >= 120)
	// ---------------------------------------------------------------

	@Test
	void renderLastScoreWindowExpiredShowsPlainScore() throws Exception {
		// L425: lastscore != 0 but scgettime >= 120 -> plain score branch.
		// L446: lastevent != NONE but scgettime >= 120 -> event panel skipped.
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.menuOnly = false;
		engine.stat = GameEngine.Status.MOVE;
		setInt(mode, "lastscore", 50);
		setInt(mode, "lastevent", 4);
		setInt(mode, "scgettime", 120); // window expired

		mode.renderLast(engine, 0);
	}

	// ---------------------------------------------------------------
	// updateMeter: intermediate meter values exercise each color's
	// false outcome (lines 598/599/600).
	// ---------------------------------------------------------------

	@Test
	void updateMeterColorThresholds() throws Exception {
		// limitTime is 180 (level 0, version>=2). meterMax forced to 100.
		// meterValue = remainTime * 100 / 180, remainTime = 180 - garbageTimer.
		// garbageTimer 72 -> meterValue 60 (> max/2): stays GREEN.
		assertEquals(GameEngine.METER_COLOR_GREEN, meterColorFor(72),
				"meterValue 60 > 50 keeps GREEN");
		// garbageTimer 108 -> meterValue 40 (<=50, >33): YELLOW only.
		assertEquals(GameEngine.METER_COLOR_YELLOW, meterColorFor(108),
				"meterValue 40 is YELLOW only");
		// garbageTimer 126 -> meterValue 30 (<=33, >25): ORANGE.
		assertEquals(GameEngine.METER_COLOR_ORANGE, meterColorFor(126),
				"meterValue 30 is ORANGE");
		// garbageTimer 144 -> meterValue 20 (<=25): RED.
		assertEquals(GameEngine.METER_COLOR_RED, meterColorFor(144),
				"meterValue 20 is RED");
	}

	/** Builds an engine with a fixed meterMax of 100, runs updateMeter, returns the chosen color. */
	private static int meterColorFor(int garbageTimer) throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameManager manager = new GameManager(new MeterReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);
		setInt(mode, "version", 2);
		setInt(mode, "goaltype", 0);
		engine.statistics.level = 0;
		setInt(mode, "garbageTimer", garbageTimer);

		Method m = DigChallengeMode.class.getDeclaredMethod("updateMeter", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
		return engine.meterColor;
	}

	// ---------------------------------------------------------------
	// calcScore: B2B T-Spin triple WITH all-spin bonus (L675 else),
	// and zero-combo index clamp (L688).
	// ---------------------------------------------------------------

	@Test
	void calcScoreB2bTSpinTripleAllSpinUsesFlatBonus() throws Exception {
		// L675: lastevent==TSPIN_TRIPLE true but useAllSpinBonus true -> +1 (else).
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRowExceptOne(engine);
		engine.tspin = true;
		engine.b2b = true;
		engine.useAllSpinBonus = true;

		mode.calcScore(engine, 0, 3);

		assertEquals(9, readInt(mode, "lastevent"), "EVENT_TSPIN_TRIPLE");
		// T-Spin triple (6) + flat B2B bonus (1) = 7 (not the +2 path).
		assertEquals(7, readInt(mode, "lastscore"));
	}

	@Test
	void calcScoreZeroComboClampsIndexToZero() throws Exception {
		// L688: cmbindex = combo - 1 < 0 -> clamped to 0.
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRowExceptOne(engine);
		engine.comboType = GameEngine.COMBO_TYPE_NORMAL;
		engine.combo = 0; // cmbindex = -1 -> clamp to 0

		mode.calcScore(engine, 0, 1);

		assertEquals(0, readInt(mode, "lastcombo"), "lastcombo records engine.combo (0)");
	}

	@Test
	void calcScoreRealtimeSkipsNormalGarbageBonus() throws Exception {
		// L711: goaltype != NORMAL -> '&& version>=2' short-circuits false.
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		fillBottomRowExceptOne(engine);
		engine.statistics.score = 0;
		setInt(mode, "goaltype", 1); // REALTIME
		setInt(mode, "version", 2);
		setInt(mode, "garbagePending", 3);

		mode.calcScore(engine, 0, 2);

		// Double (1) only; no all-clear (field not empty), no normal garbage offset.
		assertEquals(1, engine.statistics.score, "realtime: just the double point");
		assertEquals(3, readInt(mode, "garbagePending"), "realtime leaves pending untouched");
	}

	// ---------------------------------------------------------------
	// getGarbageMaxTime: level above table length is clamped (L734).
	// ---------------------------------------------------------------

	@Test
	void getGarbageMaxTimeClampsHighLevel() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		setInt(mode, "version", 2);
		setInt(mode, "goaltype", 1); // realtime table, last entry 20
		Method m = DigChallengeMode.class.getDeclaredMethod("getGarbageMaxTime", int.class);
		m.setAccessible(true);

		int clamped = (int) m.invoke(mode, 99);
		assertEquals(20, clamped, "level beyond table length clamps to last entry");
	}

	// ---------------------------------------------------------------
	// addGarbage: level already at cap (19) -> the '&& level<19' guard
	// stops the level-up loop (L796 false-on-second-condition).
	// ---------------------------------------------------------------

	@Test
	void addGarbageAtMaxLevelDoesNotLevelUp() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.statistics.level = 19; // already capped
		setInt(mode, "garbageTotal", 1000);
		setInt(mode, "garbageNextLevelLines", 10);

		Method m = DigChallengeMode.class.getDeclaredMethod("addGarbage", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);

		assertEquals(19, engine.statistics.level, "level stays capped at 19");
	}

	// ---------------------------------------------------------------
	// saveReplay: empty player name (L845 length()==0) and non-zero
	// startlevel (L850 ranking skipped).
	// ---------------------------------------------------------------

	@Test
	void saveReplayEmptyNameNonZeroLevelSkipsRanking() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setStr(mode, "netPlayerName", ""); // L845: not null but length 0
		setInt(mode, "startlevel", 5);     // L850: startlevel != 0 -> no ranking
		setInt(mode, "rankingRank", -1);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals("", prop.getProperty("0.net.netPlayerName", ""),
				"empty name must not be written");
		assertEquals(-1, readInt(mode, "rankingRank"),
				"non-zero startlevel leaves ranking untouched");
	}

	// ---------------------------------------------------------------
	// renderLast: ranking display with side-big preview (scale 0.5,
	// topY 6) and a highlighted current rank (i == rankingRank).
	// Covers L411/L412 true and L417-419 highlight true.
	// ---------------------------------------------------------------

	@Test
	void renderLastRankingDisplaySideBigHighlighted() throws Exception {
		DigChallengeMode mode = new DigChallengeMode();
		GameManager manager = new GameManager(new SideBigReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		GameEngine engine = manager.engine[0];
		mode.playerInit(engine, 0);

		engine.owner.menuOnly = false;
		engine.owner.replayMode = false;
		engine.ai = null;
		engine.stat = GameEngine.Status.SETTING;
		setInt(mode, "startlevel", 0);
		setInt(mode, "rankingRank", 3); // highlight row 3

		mode.renderLast(engine, 0);
	}

	@Test
	void netIsNetRankingViewOkNonZeroLevelIsFalse() throws Exception {
		// L1068: startlevel != 0 -> false.
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "startlevel", 7);
		engine.ai = null;

		Method m = DigChallengeMode.class.getDeclaredMethod("netIsNetRankingViewOK", GameEngine.class);
		m.setAccessible(true);

		assertFalse((boolean) m.invoke(mode, engine), "non-zero startlevel => not OK");
	}

	@Test
	void onSettingChangeDifferentFromZeroPlaysChangeSe() throws Exception {
		// Sanity: a LEFT press at cursor 0 yields change != 0 and updates goaltype.
		DigChallengeMode mode = new DigChallengeMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.owner.replayMode = false;
		setInt(mode, "goaltype", 1);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "menuTime", 0);
		pressKey(engine, Controller.BUTTON_LEFT);

		mode.onSetting(engine, 0);

		assertNotEquals(1, readInt(mode, "goaltype"), "LEFT at cursor 0 changes goaltype");
	}

	// ---------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------

	private static GameEngine freshEngine(DigChallengeMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.modeConfig = new CustomProperties();
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.modeConfig = new CustomProperties();
		return manager.engine[0];
	}

	private static void fillBottomRowExceptOne(GameEngine engine) {
		int h = engine.field.getHeight();
		int w = engine.field.getWidth();
		for (int x = 1; x < w; x++) {
			engine.field.setBlock(x, h - 1, new Block(Block.BLOCK_COLOR_GRAY));
		}
	}

	private static void pressKey(GameEngine e, int btn) {
		e.ctrl.reset();
		e.ctrl.buttonPress[btn] = true;
		e.ctrl.buttonTime[btn] = 1;
	}

	private static int readInt(DigChallengeMode mode, String name) throws Exception {
		return findField(mode.getClass(), name).getInt(mode);
	}

	private static boolean readBool(DigChallengeMode mode, String name) throws Exception {
		return findField(mode.getClass(), name).getBoolean(mode);
	}

	private static void setInt(DigChallengeMode mode, String name, int value) throws Exception {
		findField(mode.getClass(), name).setInt(mode, value);
	}

	private static void setBool(DigChallengeMode mode, String name, boolean value) throws Exception {
		findField(mode.getClass(), name).setBoolean(mode, value);
	}

	private static void setStr(DigChallengeMode mode, String name, String value) throws Exception {
		findField(mode.getClass(), name).set(mode, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	/** Receiver with a positive, fixed meter maximum so meter colors are testable. */
	private static final class MeterReceiver extends EventReceiver {
		@Override
		public int getMeterMax(GameEngine engine) {
			return 100;
		}
	}

	/** Receiver reporting the side-big next-preview layout (getNextDisplayType() == 2). */
	private static final class SideBigReceiver extends EventReceiver {
		@Override
		public int getNextDisplayType() {
			return 2;
		}
	}
}
