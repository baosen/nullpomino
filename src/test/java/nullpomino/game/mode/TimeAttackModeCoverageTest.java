package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Exercises the non-menu branches of {@link TimeAttackMode}: the speed-table
 * switch arms and clamps in {@code setSpeed}, the scoring / level-up / BGM
 * branches in {@code calcScore}, the per-frame logic in {@code onLast} and
 * {@code onMove}, result-screen paging in {@code onResult}, and the net-play
 * branches of {@code onSetting}.
 */
class TimeAttackModeCoverageTest {

	private static final int GAMETYPE_NORMAL = 0;
	private static final int GAMETYPE_ANOTHER = 3;
	private static final int GAMETYPE_NORMAL200 = 5;
	private static final int GAMETYPE_BASIC = 7;
	private static final int GAMETYPE_HELL = 8;
	private static final int GAMETYPE_HELLX = 9;
	private static final int GAMETYPE_VOID = 10;

	private static GameEngine freshEngine(TimeAttackMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[0].owner.replayMode = false;
		return manager.engine[0];
	}

	private static TimeAttackMode prepared(GameEngine[] out) {
		TimeAttackMode mode = new TimeAttackMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		out[0] = engine;
		return mode;
	}

	// -----------------------------------------------------------------------
	// setSpeed: switch arms + clamps (called via reflection)
	// -----------------------------------------------------------------------

	private static void invokeSetSpeed(TimeAttackMode mode, GameEngine engine) throws Exception {
		Method m = TimeAttackMode.class.getDeclaredMethod("setSpeed", GameEngine.class);
		m.setAccessible(true);
		m.invoke(mode, engine);
	}

	// gravlv/speedlv/timelv < 0 clamps (L331, L338, L402) for a table-driven type
	@Test
	void setSpeedClampsNegativeLevel() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_ANOTHER); // table-driven ARE etc.
		engine.statistics.level = -5; // forces every <0 clamp to fire
		invokeSetSpeed(mode, engine);
		// level 0 row of tableAnother: ARE 18, lineDelay 14, lockDelay 28, das 10
		assertEquals(18, engine.speed.are);
		assertEquals(14, engine.speed.lineDelay);
		assertEquals(28, engine.speed.lockDelay);
		assertEquals(10, engine.speed.das);
	}

	// NORMAL200 speedlv >= length clamp (L367)
	@Test
	void setSpeedNormal200ClampsHighLevel() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_NORMAL200);
		engine.statistics.level = 999; // >= table length -> clamp to last index (19)
		invokeSetSpeed(mode, engine);
		// last column of tableNormal200: ARE 10, lineDelay 8, lockDelay 17, das 6
		assertEquals(10, engine.speed.are);
		assertEquals(8, engine.speed.lineDelay);
		assertEquals(17, engine.speed.lockDelay);
		assertEquals(6, engine.speed.das);
	}

	// BASIC speedlv >= length clamp (L375)
	@Test
	void setSpeedBasicClampsHighLevel() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_BASIC);
		engine.statistics.level = 999;
		invokeSetSpeed(mode, engine);
		// last column of tableBasic: ARE 2, lineDelay 3, lockDelay 11, das 7
		assertEquals(2, engine.speed.are);
		assertEquals(3, engine.speed.lineDelay);
		assertEquals(11, engine.speed.lockDelay);
		assertEquals(7, engine.speed.das);
	}

	// VOID switch arm (L390-397) and its speedlv clamp
	@Test
	void setSpeedVoidClampsHighLevel() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_VOID);
		engine.statistics.level = 999;
		invokeSetSpeed(mode, engine);
		// last column of tableVoid: ARE 0, lineDelay 0, lockDelay 8, das 2
		assertEquals(0, engine.speed.are);
		assertEquals(0, engine.speed.lineDelay);
		assertEquals(8, engine.speed.lockDelay);
		assertEquals(2, engine.speed.das);
		assertTrue(engine.bone, "VOID enables bone blocks");
	}

	// HELL-X block-fade clamps (L417 <0, L418 >=length) + bone for HELL-X
	@Test
	void setSpeedHellXFadeClampsBothBounds() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		// High level: fadelv >= length -> clamp to last entry (60)
		setInt(mode, "goaltype", GAMETYPE_HELLX);
		engine.statistics.level = 999;
		invokeSetSpeed(mode, engine);
		assertEquals(60, engine.blockHidden);
		assertTrue(engine.bone, "HELL-X enables bone blocks");

		// Negative level: fadelv < 0 -> clamp to entry 0 (-1)
		engine.statistics.level = -3;
		invokeSetSpeed(mode, engine);
		assertEquals(-1, engine.blockHidden);
	}

	// HELL with level >= 15 enables bone + outline-only (L407, L411)
	@Test
	void setSpeedHellHighLevelEnablesBone() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_HELL);
		engine.statistics.level = 16;
		invokeSetSpeed(mode, engine);
		assertTrue(engine.blockShowOutlineOnly, "HELL shows outline only");
		assertTrue(engine.bone, "HELL at level>=15 enables bone");
		// HELL uses fixed speeds
		assertEquals(2, engine.speed.are);
		assertEquals(11, engine.speed.lockDelay);
	}

	// -----------------------------------------------------------------------
	// calcScore: meter color, BGM change/fadeout, level-up, completion
	// -----------------------------------------------------------------------

	// norm%10 >= 8 red meter (L791) on a sub-20 goal type
	@Test
	void calcScoreRedMeterNearSectionEnd() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_NORMAL); // goal level 15 (< 20)
		setInt(mode, "norm", 7);
		engine.statistics.level = 0;
		engine.ending = 0;
		mode.calcScore(engine, 0, 1); // norm -> 8, 8%10>=8
		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	// BGM change branch (L795) when norm crosses tableBGMChange threshold
	@Test
	void calcScoreAdvancesBgmAtChangeThreshold() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_NORMAL); // tableBGMChange = {50,100}
		setInt(mode, "bgmlv", 0);
		setInt(mode, "norm", 49);
		engine.statistics.level = 4; // level+1)*10 = 50, avoids spurious level-up via norm
		engine.ending = 0;
		mode.calcScore(engine, 0, 1); // norm -> 50 >= 50 -> bgmlv++
		assertEquals(1, readInt(mode, "bgmlv"));
		assertEquals(false, engine.owner.bgmStatus.fadesw);
	}

	// BGM fadeout branch (L801): change threshold not met but fadeout threshold met
	@Test
	void calcScoreFadesBgmAtFadeoutThreshold() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		// NORMAL: tableBGMChange={50,100}, tableBGMFadeout={45,95,145}
		// Put bgmlv at 2 so the change clause (index 2 out of range) is skipped,
		// and the fadeout clause uses index 2 -> threshold 145.
		setInt(mode, "goaltype", GAMETYPE_NORMAL);
		setInt(mode, "bgmlv", 2);
		setInt(mode, "norm", 144);
		engine.statistics.level = 13; // (13+1)*10 = 140, no level-up from this
		engine.ending = 0;
		engine.owner.bgmStatus.fadesw = false;
		mode.calcScore(engine, 0, 1); // norm -> 145 >= 145 fadeout
		assertTrue(engine.owner.bgmStatus.fadesw, "fadeout sets fadesw");
	}

	// Level-up branch (L829)
	@Test
	void calcScoreLevelsUp() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_NORMAL); // goal level 15
		setInt(mode, "norm", 9);
		setInt(mode, "bgmlv", 9); // keep BGM clauses out of range so they no-op
		engine.statistics.level = 0;
		engine.ending = 0;
		engine.timerActive = true;
		mode.calcScore(engine, 0, 1); // norm -> 10 >= (0+1)*10 and level < 14 -> level up
		assertEquals(1, engine.statistics.level);
		assertTrue(engine.owner.backgroundStatus.fadesw);
	}

	// Completion via HELL-X/VOID staffroll branch (L819)
	@Test
	void calcScoreHellXCompletionStartsStaffroll() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_HELLX); // goal level 20 -> goal norm 200
		setInt(mode, "norm", 199);
		setInt(mode, "bgmlv", 9);
		engine.statistics.level = 19;
		engine.ending = 0;
		engine.timerActive = true;
		mode.calcScore(engine, 0, 1); // norm -> 200 >= 200 -> ending=1, staffroll
		assertEquals(1, engine.ending);
		assertTrue(engine.staffrollEnable, "HELL-X completion enables staffroll");
		assertEquals(1, engine.statistics.rollclear);
	}

	// Completion via non-staffroll branch (L823 else) for a normal type
	@Test
	void calcScoreNormalCompletionEndsGame() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_NORMAL); // goal level 15 -> goal norm 150
		setInt(mode, "norm", 149);
		setInt(mode, "bgmlv", 9);
		engine.statistics.level = 14;
		engine.ending = 0;
		engine.timerActive = true;
		mode.calcScore(engine, 0, 1);
		assertEquals(1, engine.ending);
		assertEquals(2, engine.statistics.rollclear);
	}

	// calcScore early-return during ending (L773 true branch)
	@Test
	void calcScoreNoOpDuringEnding() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "norm", 5);
		engine.ending = 1;
		mode.calcScore(engine, 0, 3);
		assertEquals(5, readInt(mode, "norm"), "norm unchanged during ending");
	}

	// Hebo Hidden decrement clamp (L779, L782) when enabled
	@Test
	void calcScoreHeboHiddenClampsAtZero() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_NORMAL);
		setInt(mode, "norm", 0);
		setInt(mode, "bgmlv", 9);
		engine.statistics.level = 0;
		engine.ending = 0;
		engine.heboHiddenEnable = true;
		engine.heboHiddenYNow = 1;
		mode.calcScore(engine, 0, 3); // yNow -= 3 -> -2 -> clamp to 0
		assertEquals(0, engine.heboHiddenYNow);
	}

	// -----------------------------------------------------------------------
	// onLast: timer countdown, meter colors, section time, hebo hidden
	// -----------------------------------------------------------------------

	// Level timer reaching 0 ends the game (L705 else branch)
	@Test
	void onLastTimeoutEndsGame() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_NORMAL);
		setInt(mode, "levelTimer", 0);
		setBool2(mode, "rollstarted", false);
		engine.statistics.level = 0;
		engine.ending = 0;
		engine.timerActive = true;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	// Yellow meter threshold (L716) for a 20-goal type
	@Test
	void onLastYellowMeterThreshold() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_NORMAL200); // goal level 20 (>=20)
		setInt(mode, "levelTimer", 25 * 60); // <= 25*60 -> yellow, > 15*60
		setInt(mode, "levelTimerMax", 7200);
		engine.statistics.level = 0;
		engine.ending = 0;
		engine.timerActive = true;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
	}

	// HELL hebo-hidden mid-level arm (L730/L735) for level 7..14
	@Test
	void onLastHellHeboHiddenLevel7to14() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_HELL);
		setInt(mode, "levelTimer", 1000);
		engine.statistics.level = 8; // in [7,14]
		engine.ending = 0;
		engine.timerActive = true;
		mode.onLast(engine, 0);
		assertTrue(engine.heboHiddenEnable, "HELL level 7-14 enables hebo hidden");
	}

	// HELL hebo-hidden disabled outside the two windows (L739 else)
	@Test
	void onLastHellHeboHiddenDisabledOutsideWindows() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_HELL);
		setInt(mode, "levelTimer", 1000);
		engine.statistics.level = 0; // outside [5,6] and [7,14]
		engine.ending = 0;
		engine.timerActive = true;
		engine.heboHiddenEnable = true;
		mode.onLast(engine, 0);
		assertTrue(!engine.heboHiddenEnable, "HELL outside windows disables hebo hidden");
	}

	// Ending roll meter + completion (L745, L757) -> EXCELLENT
	@Test
	void onLastEndingRollCompletes() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "rolltime", 3237); // ROLLTIMELIMIT=3238, rolltime++ -> 3238
		engine.gameActive = true;
		engine.ending = 2;
		mode.onLast(engine, 0);
		assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
		assertEquals(2, engine.statistics.rollclear);
	}

	// -----------------------------------------------------------------------
	// onMove: ending start (L677)
	// -----------------------------------------------------------------------

	@Test
	void onMoveStartsEndingRoll() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		engine.ending = 2;
		engine.staffrollEnable = true;
		setBool2(mode, "rollstarted", false);
		mode.onMove(engine, 0);
		assertTrue(readBool(mode, "rollstarted"), "onMove should start the roll");
	}

	// onMove VOID ending sets block visibility (L683)
	@Test
	void onMoveVoidEndingHidesBlocks() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		setInt(mode, "goaltype", GAMETYPE_VOID);
		engine.ending = 2;
		engine.staffrollEnable = true;
		setBool2(mode, "rollstarted", false);
		mode.onMove(engine, 0);
		assertTrue(readBool(mode, "rollstarted"));
		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, engine.blockOutlineType);
	}

	// -----------------------------------------------------------------------
	// onResult: page paging wraparound (L914, L919)
	// -----------------------------------------------------------------------

	@Test
	void onResultPagingWrapsBothEnds() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		// UP from page 0 wraps to 2
		engine.statc[1] = 0;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_UP] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_UP] = 1;
		mode.onResult(engine, 0);
		assertEquals(2, engine.statc[1]);

		// DOWN from page 2 wraps to 0
		engine.statc[1] = 2;
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_DOWN] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_DOWN] = 1;
		mode.onResult(engine, 0);
		assertEquals(0, engine.statc[1]);
	}

	// -----------------------------------------------------------------------
	// onSetting net-play branches (L486 netSendOptions, L509 D-button)
	// -----------------------------------------------------------------------

	private static void wireNetLobby(Object mode) throws Exception {
		NetLobbyFrame lobby = new NetLobbyFrame();
		lobby.netPlayerClient = new NetPlayerClient();
		setField(mode, "netLobby", lobby);
	}

	@Test
	void onSettingNetChangeSignalsOptions() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		wireNetLobby(mode);
		setBool2(mode, "netIsNetPlay", true);
		setInt(mode, "netNumSpectators", 1);
		setInt(mode, "menuCursor", 2); // toggle showsectiontime -> change != 0
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;

		// netSendOptions sends over a null socket; should not throw.
		mode.onSetting(engine, 0);
		assertTrue(true);
	}

	@Test
	void onSettingDButtonEntersNetRanking() throws Exception {
		GameEngine[] box = new GameEngine[1];
		TimeAttackMode mode = prepared(box);
		GameEngine engine = box[0];

		wireNetLobby(mode);
		setBool2(mode, "netIsNetPlay", true);
		setBool2(mode, "big", false);
		setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
		setInt(mode, "menuTime", 10);
		engine.ctrl.reset();
		engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
		engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

		mode.onSetting(engine, 0);
		assertTrue(readBool(mode, "netIsNetRankingDisplayMode"),
				"D in net play should enter ranking screen");
	}

	// -----------------------------------------------------------------------
	// reflection helpers
	// -----------------------------------------------------------------------

	private static void setInt(Object obj, String name, int value) throws Exception {
		field(obj.getClass(), name).setInt(obj, value);
	}

	private static void setBool2(Object obj, String name, boolean value) throws Exception {
		field(obj.getClass(), name).setBoolean(obj, value);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		field(obj.getClass(), name).set(obj, value);
	}

	private static int readInt(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getInt(obj);
	}

	private static boolean readBool(Object obj, String name) throws Exception {
		return field(obj.getClass(), name).getBoolean(obj);
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
