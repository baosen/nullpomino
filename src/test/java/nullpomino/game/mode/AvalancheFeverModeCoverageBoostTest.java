package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Additional coverage for {@link AvalancheFeverMode} targeting the previously
 * uncovered lines: the {@code xyzzy == 573} preview menu in {@code onSetting}
 * (map-set / subset / chain adjustment and clamping), the xyzzy easter-egg
 * key sequence (UP/DOWN/LEFT/RIGHT and the A/B unlock branches), the replay
 * {@code playerInit} path, {@code renderMove}, {@code renderSetting} FAST line,
 * {@code renderLast} fever-timer / big-display / chain-colour branches,
 * {@code drawXorTimer} big-display branch, {@code onLast} timeLimitAddDisplay
 * decrement, {@code lineClearEnd} garbage / fever-chain clamp / cool / regret
 * branches, and {@code renderResult} with a valid ranking rank.
 *
 * Uses a save-redirecting EventReceiver so no tracked config file is touched.
 */
class AvalancheFeverModeCoverageBoostTest {

	/** EventReceiver that delegates map loads to the real file system but
	 *  redirects every write into java.io.tmpdir so no tracked file changes. */
	private static class SaveRedirectReceiver extends EventReceiver {
		@Override
		public void saveModeConfig(CustomProperties modeConfig) {
			saveProperties(tmp("mode.cfg"), modeConfig);
		}

		@Override
		public boolean saveProperties(String filename, CustomProperties prop) {
			return super.saveProperties(tmp(new File(filename).getName()), prop);
		}

		private static String tmp(String name) {
			return new File(System.getProperty("java.io.tmpdir"),
					"npmtest_" + name).getAbsolutePath();
		}
	}

	// -----------------------------------------------------------------------
	// playerInit replay path (line 149)
	// -----------------------------------------------------------------------

	@Test
	void playerInitReplayModeLoadsReplayProp() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		engine.owner.replayMode = true;
		engine.owner.replayProp = new CustomProperties();
		engine.owner.replayProp.setProperty("avalanchefever.gametype", 2);

		mode.playerInit(engine, 0);

		assertEquals(2, readInt(mode, "mapSet"),
				"Replay path should load mapSet from replayProp");
	}

	// -----------------------------------------------------------------------
	// onSetting: xyzzy easter-egg key navigation (lines 232,238,244,250)
	// -----------------------------------------------------------------------

	@Test
	void onSettingXyzzyKeySequenceAdvances() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// xyzzy starts 0; UP at 0 -> stays/sets 1 path; set xyzzy to 1 then UP -> 2
		setInt(mode, "xyzzy", 1);
		press(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(2, readInt(mode, "xyzzy"), "UP with xyzzy==1 should increment");

		// xyzzy==2, DOWN -> 3
		setInt(mode, "xyzzy", 2);
		press(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(3, readInt(mode, "xyzzy"), "DOWN with xyzzy==2 should increment");

		// xyzzy==4, LEFT -> 5
		setInt(mode, "xyzzy", 4);
		press(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(5, readInt(mode, "xyzzy"), "LEFT with xyzzy==4 should increment");

		// xyzzy==5, RIGHT -> 6
		setInt(mode, "xyzzy", 5);
		press(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(6, readInt(mode, "xyzzy"), "RIGHT with xyzzy==5 should increment");
	}

	@Test
	void onSettingXyzzyBButtonAdvancesThenUnlocksOnA() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// B with xyzzy==8 -> 9 (line 274,275)
		setInt(mode, "xyzzy", 8);
		press(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertEquals(9, readInt(mode, "xyzzy"), "B with xyzzy==8 should advance to 9");

		// A with xyzzy==9 -> unlocks to 573 (lines 260-263)
		setInt(mode, "xyzzy", 9);
		setInt(mode, "menuCursor", 0);
		press(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);
		assertEquals(573, readInt(mode, "xyzzy"),
				"A with xyzzy==9 should unlock preview mode");
	}

	@Test
	void onSettingBButtonQuitsWhenNotXyzzy8() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "xyzzy", 0);

		press(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertTrue(engine.quitflag, "B (xyzzy!=8) should set quitflag (line 278)");
	}

	// -----------------------------------------------------------------------
	// onSetting: xyzzy==573 preview menu config changes (lines 185-223)
	// -----------------------------------------------------------------------

	@Test
	void onSettingXyzzy573MapSetReloadsAndClamps() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "xyzzy", 573);
		setInt(mode, "mapSet", 0);
		setInt(mode, "menuCursor", 0);
		// drive previewChain/Subset out of range so clamps execute
		setInt(mode, "previewChain", 999);
		setInt(mode, "previewSubset", 999);

		press(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		// loadMapSetFever ran -> feverChainMin/Max populated from FeverEndless.map
		assertEquals(3, readInt(mode, "feverChainMin"));
		assertEquals(15, readInt(mode, "feverChainMax"));
		assertEquals(0, readInt(mode, "previewSubset"),
				"out-of-range previewSubset clamps to 0");
	}

	@Test
	void onSettingXyzzy573SubsetCursorWraps() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "xyzzy", 573);
		setInt(mode, "mapSet", 0);
		// load the map set so mapSubsets is populated
		invokeLoadMapSetFever(mode, engine, 0);
		setInt(mode, "menuCursor", 7);
		setInt(mode, "previewSubset", 0);

		// LEFT at subset 0 -> wraps to mapSubsets.length-1 (line 216,217)
		press(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertTrue(readInt(mode, "previewSubset") > 0,
				"previewSubset should wrap to last subset");
	}

	@Test
	void onSettingXyzzy573ChainCursorClamps() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "xyzzy", 573);
		setInt(mode, "mapSet", 0);
		invokeLoadMapSetFever(mode, engine, 0);
		setInt(mode, "feverChainMin", 3);
		setInt(mode, "feverChainMax", 15);
		setInt(mode, "menuCursor", 8);
		setInt(mode, "previewChain", 3);

		// LEFT at min -> wraps to max (line 221,222,223)
		press(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(15, readInt(mode, "previewChain"),
				"previewChain below min wraps to max");
	}

	@Test
	void onSettingXyzzy573PreviewAButtonLoadsFeverMap() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "xyzzy", 573);
		setInt(mode, "mapSet", 0);
		invokeLoadMapSetFever(mode, engine, 0);
		setInt(mode, "feverChainMin", 3);
		setInt(mode, "feverChainMax", 15);
		setInt(mode, "previewChain", 5);
		setInt(mode, "previewSubset", 0);
		setInt(mode, "menuCursor", 6); // > 5

		// A with xyzzy==573 && cursor>5 -> loadFeverMap (lines 257-259)
		press(engine, Controller.BUTTON_A);
		boolean result = mode.onSetting(engine, 0);
		assertTrue(result, "preview A should keep menu open");
	}

	@Test
	void onSettingXyzzy573OutlineColorsChainBigFastBranches() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "xyzzy", 573);
		invokeLoadMapSetFever(mode, engine, 0); // populate mapSubsets

		// cursor 1: outline wrap (lines 192-196)
		setInt(mode, "mapSet", 0);
		setInt(mode, "menuCursor", 1);
		setInt(mode, "outlinetype", 0);
		press(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(2, readInt(mode, "outlinetype"), "outline wraps 0->2 on LEFT");

		// cursor 2: numColors wrap (lines 198-200)
		setInt(mode, "menuCursor", 2);
		setInt(mode, "numColors", 3);
		press(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(5, readInt(mode, "numColors"), "numColors wraps 3->5 on LEFT");

		// cursor 3: chainDisplayType wrap (lines 203-205)
		setInt(mode, "menuCursor", 3);
		setInt(mode, "chainDisplayType", 0);
		press(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(2, readInt(mode, "chainDisplayType"));

		// cursor 4: bigDisplay toggle (line 208)
		setInt(mode, "menuCursor", 4);
		boolean bigBefore = readBool(mode, "bigDisplay");
		press(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(!bigBefore, readBool(mode, "bigDisplay"));

		// cursor 5: fastenable wrap (lines 211-213)
		setInt(mode, "menuCursor", 5);
		setInt(mode, "fastenable", 0);
		press(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(2, readInt(mode, "fastenable"));
	}

	@Test
	void onSettingMapSet4ForcesThreeColors() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		// default (xyzzy != 573) so loadMapSetFever is NOT triggered on cursor 0
		setInt(mode, "xyzzy", 0);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "mapSet", 3);
		setInt(mode, "numColors", 5);

		// RIGHT moves mapSet 3 -> 4, then numColors forced to 3 (line 226)
		press(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(4, readInt(mode, "mapSet"));
		assertEquals(3, readInt(mode, "numColors"), "mapSet 4 forces 3 colors");
	}

	@Test
	void onSettingMapSetWrapsBelowZero() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "xyzzy", 0);
		setInt(mode, "menuCursor", 0);
		setInt(mode, "mapSet", 0);

		press(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(AvalancheFeverMode.FEVER_MAPS.length - 1, readInt(mode, "mapSet"),
				"mapSet wraps below 0 to last (line 183)");
	}

	// -----------------------------------------------------------------------
	// renderMove (line 301)
	// -----------------------------------------------------------------------

	@Test
	void renderMoveDrawsTimerWhenStarted() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.gameStarted = true;
		engine.createFieldIfNeeded();

		mode.renderMove(engine, 0); // executes drawXorTimer (line 301)
	}

	// -----------------------------------------------------------------------
	// renderSetting FAST line in xyzzy==573 (lines 323,324)
	// -----------------------------------------------------------------------

	@Test
	void renderSettingFastLineWhenXyzzy573() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "xyzzy", 573);
		setInt(mode, "menuCursor", 0); // <= 5 -> page 1 with FAST line
		setInt(mode, "fastenable", 0);

		mode.renderSetting(engine, 0); // line 324 FAST line
	}

	// -----------------------------------------------------------------------
	// renderLast: fever-timer + big-display + chain colour branches
	// -----------------------------------------------------------------------

	@Test
	void renderLastDrawsTimerInPlay() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.gameStarted = true;
		engine.gameActive = true;
		engine.stat = GameEngine.Status.READY; // not MOVE/RESULT -> drawXorTimer (line 399)

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastBigDisplayTimerAndChainColors() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.gameStarted = true;
		engine.gameActive = true;
		engine.displaysize = 1; // big display -> lines 409, 442/443
		engine.stat = GameEngine.Status.READY;
		engine.chain = 4;
		setInt(mode, "chainDisplay", 30);
		setInt(mode, "chainDisplayType", 2);
		setInt(mode, "feverChainDisplay", 6); // chain==feverChainDisplay-2 -> ORANGE (line 420)
		setInt(mode, "timeLimit", 100); // < 360 -> RED timer text

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastChainColorGreenBranch() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.gameStarted = true;
		engine.gameActive = true;
		engine.stat = GameEngine.Status.READY;
		engine.chain = 8;
		setInt(mode, "chainDisplay", 30);
		setInt(mode, "chainDisplayType", 2);
		setInt(mode, "feverChainDisplay", 6); // chain >= feverChainDisplay -> GREEN (line 418)

		mode.renderLast(engine, 0);
	}

	// -----------------------------------------------------------------------
	// onLast: timeLimitAddDisplay decrement (lines 480,481)
	// -----------------------------------------------------------------------

	@Test
	void onLastDecrementsTimeLimitAddDisplay() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "timeLimitAddDisplay", 30);

		mode.onLast(engine, 0);

		assertEquals(29, readInt(mode, "timeLimitAddDisplay"),
				"timeLimitAddDisplay should decrement (line 481)");
	}

	// -----------------------------------------------------------------------
	// lineClearEnd: garbage drop + fever chain clamps + cool/regret SE
	// -----------------------------------------------------------------------

	@Test
	void lineClearEndDrainsGarbageAndClampsCool() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "mapSet", 0);
		invokeLoadMapSetFever(mode, engine, 0); // populate mapSubsets + propFeverMap
		engine.createFieldIfNeeded();
		engine.random.setSeed(1);

		setInt(mode, "garbageAdd", 5);   // -> lines 525,526
		setInt(mode, "garbageSent", 0);
		setBool(mode, "cleared", true);
		engine.chain = 10;               // high chain -> newFeverChain large
		setInt(mode, "feverChainMin", 3);
		setInt(mode, "feverChainMax", 15);
		setInt(mode, "feverChain", 5);   // newFeverChain > feverChain -> cool (line 545)
		setInt(mode, "timeLimit", 3600);

		mode.lineClearEnd(engine, 0);

		assertEquals(5, readInt(mode, "garbageSent"),
				"garbageAdd should be flushed into garbageSent");
		assertEquals(0, readInt(mode, "garbageAdd"));
		assertEquals(11, readInt(mode, "feverChain"),
				"feverChain rises toward chain+1 (clamped under max)");
	}

	@Test
	void lineClearEndClampsToMaxAndZenkeshiAddsTime() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "mapSet", 0);
		invokeLoadMapSetFever(mode, engine, 0);
		engine.createFieldIfNeeded();
		engine.random.setSeed(2);

		setBool(mode, "cleared", true);
		setBool(mode, "zenKeshi", true); // zenkeshi branch -> +180, newFeverChain+=2
		engine.chain = 20;               // very high -> newFeverChain > max -> clamp (line 543)
		setInt(mode, "feverChainMin", 3);
		setInt(mode, "feverChainMax", 15);
		setInt(mode, "feverChain", 5);
		setInt(mode, "timeLimit", 3600);

		mode.lineClearEnd(engine, 0);

		assertEquals(15, readInt(mode, "feverChain"),
				"newFeverChain clamps to feverChainMax (line 543)");
		assertTrue(readInt(mode, "timeLimit") > 3600,
				"zenkeshi + chain bonus should extend the time limit");
	}

	@Test
	void lineClearEndRegretWhenChainDrops() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "mapSet", 0);
		invokeLoadMapSetFever(mode, engine, 0);
		engine.createFieldIfNeeded();
		engine.random.setSeed(3);

		setBool(mode, "cleared", true);
		engine.chain = 1;                // newFeverChain = max(2, feverChain-2)
		setInt(mode, "feverChainMin", 3);
		setInt(mode, "feverChainMax", 15);
		setInt(mode, "feverChain", 10);  // -> newFeverChain=8 < feverChain -> regret (line 547)
		setInt(mode, "timeLimit", 3600);

		mode.lineClearEnd(engine, 0);

		assertEquals(8, readInt(mode, "feverChain"),
				"feverChain drops by 2 (max(chain+1, feverChain-2)) -> regret SE");
		assertTrue(readInt(mode, "feverChain") < 10);
	}

	@Test
	void lineClearEndClampsToMinWhenChainLow() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "mapSet", 0);
		invokeLoadMapSetFever(mode, engine, 0);
		engine.createFieldIfNeeded();
		engine.random.setSeed(4);

		setBool(mode, "cleared", true);
		engine.chain = 1;
		setInt(mode, "feverChainMin", 5); // force newFeverChain < min -> clamp (line 541)
		setInt(mode, "feverChainMax", 15);
		setInt(mode, "feverChain", 4);    // newFeverChain=max(2,2)=2 < min 5 -> 5
		setInt(mode, "timeLimit", 3600);

		mode.lineClearEnd(engine, 0);

		assertEquals(5, readInt(mode, "feverChain"),
				"newFeverChain clamps up to feverChainMin (line 541)");
	}

	// -----------------------------------------------------------------------
	// renderResult with a valid ranking rank (lines 611-614)
	// -----------------------------------------------------------------------

	@Test
	void renderResultWithRankingRank() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "rankingRank", 2); // != -1 -> RANK lines 611-614
		setInt(mode, "scoreBeforeBonus", 5000);
		setInt(mode, "zenKeshiCount", 3);
		setInt(mode, "zenKeshiBonus", 600);
		setInt(mode, "maxChainBonus", 400);
		engine.statistics.maxChain = 9;
		engine.statistics.score = 6000;

		mode.renderResult(engine, 0);
	}

	@Test
	void saveReplayUpdatesRankingAndSavesViaRedirect() throws Exception {
		AvalancheFeverMode mode = new AvalancheFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.statistics.score = 999999; // beats empty ranking -> rankingRank set
		engine.statistics.time = 1000;
		setInt(mode, "mapSet", 0);
		setInt(mode, "numColors", 4);

		// not replay, no ai -> updateRanking + saveRanking + saveModeConfig (redirected)
		mode.saveReplay(engine, 0, new CustomProperties());

		assertTrue(readInt(mode, "rankingRank") >= 0,
				"high score should rank and trigger save path");
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(AvalancheFeverMode mode) {
		GameManager manager = new GameManager(new SaveRedirectReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	/** Press a single button so both isPush and isMenuRepeatKey fire. */
	private static void press(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonPress[i] = false;
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void invokeLoadMapSetFever(AvalancheFeverMode mode,
			GameEngine engine, int id) throws Exception {
		java.lang.reflect.Method m = AvalancheFeverMode.class.getDeclaredMethod(
				"loadMapSetFever", GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, id, true);
	}

	private static int readInt(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getInt(o);
	}

	private static boolean readBool(Object o, String n) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		return f.getBoolean(o);
	}

	private static void setInt(Object o, String n, int v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setInt(o, v);
	}

	private static void setBool(Object o, String n, boolean v) throws Exception {
		Field f = findField(o.getClass(), n);
		f.setAccessible(true);
		f.setBoolean(o, v);
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
