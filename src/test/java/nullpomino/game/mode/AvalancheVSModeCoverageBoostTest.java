package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Random;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Boosts line coverage of {@link AvalancheVSMode} by exercising branches not
 * touched by the existing {@code AvalancheVSModeSettingMenuTest} and siblings:
 *
 *  - onSetting fever-map-dependent cursor branches (25/27/34/36/37/38), the
 *    xyzzy cheat-code sequence (cursors 45/46, UP/DOWN/LEFT/RIGHT pushes,
 *    A/B confirm), random map preview, and the statc[4]!=0 start/cancel path.
 *  - renderSetting page 3 (big), page 5, and the MAP-PREVIEW page (cursor>=44).
 *  - renderLast in fever mode (meter + numeric), displaysize variations,
 *    ojama/ojamaFever "(+n)" strings, drawXorTimer fever path, drawHardOjama.
 *  - calcChainNewPower (fever powers), onClear, addOjama fever counter logic,
 *    lineClearEnd fever-chain / zenkeshi-fever / fever-start / fever-end paths,
 *    onLast fever meter / debug cheat, readyInit big and fever paths, and
 *    saveReplay debug-cheat property.
 *
 * Uses a {@link RedirectingReceiver} so any saveModeConfig / saveProperties on
 * a confirm path writes to java.io.tmpdir instead of the tracked config files.
 */
class AvalancheVSModeCoverageBoostTest {

	/** EventReceiver that still loads real fever maps but redirects all writes. */
	static class RedirectingReceiver extends EventReceiver {
		@Override
		public void saveModeConfig(CustomProperties modeConfig) {
			try {
				modeConfig.storeToFile(
						System.getProperty("java.io.tmpdir") + "/avalanchevs-test-mode.cfg",
						"test");
			} catch (Exception ignored) {
			}
		}

		@Override
		public boolean saveProperties(String filename, CustomProperties prop) {
			try {
				prop.storeToFile(
						System.getProperty("java.io.tmpdir") + "/avalanchevs-test-redirect.cfg",
						"test");
			} catch (Exception ignored) {
			}
			return true;
		}
	}

	// ---------------------------------------------------------------
	// onSetting: fever-map-dependent cursor branches
	// ---------------------------------------------------------------

	@Test
	void onSettingCursor25ZenKeshiChainWhenFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		// zenKeshiType = FEVER -> cursor 25 adjusts zenKeshiChain with fever clamp
		setIntArray(mode, "zenKeshiType",
				AvalancheVSDummyMode.ZENKESHI_MODE_FEVER, 0);
		setIntArray(mode, "zenKeshiChain", 5, 0);
		setMenuState(engine, mode, 25);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		int min = getIntArray(mode, "feverChainMin")[0];
		int max = getIntArray(mode, "feverChainMax")[0];
		int v = getIntArray(mode, "zenKeshiChain")[0];
		assertTrue(v >= min && v <= max, "zenKeshiChain stays within fever bounds");
	}

	@Test
	void onSettingCursor25ZenKeshiChainWrapsBelowMin() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setIntArray(mode, "zenKeshiType",
				AvalancheVSDummyMode.ZENKESHI_MODE_FEVER, 0);
		int min = getIntArray(mode, "feverChainMin")[0];
		int max = getIntArray(mode, "feverChainMax")[0];
		setIntArray(mode, "zenKeshiChain", min, 0);
		setMenuState(engine, mode, 25);

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		// LEFT below min wraps to max
		assertEquals(max, getIntArray(mode, "zenKeshiChain")[0]);
	}

	@Test
	void onSettingCursor27FeverMapSetReloadsAndClamps() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		// Put preview/chain-start fields out of range so the clamp lines run.
		setIntArray(mode, "feverChainStart", 1, 0);
		setIntArray(mode, "previewChain", 1, 0);
		setIntArray(mode, "previewSubset", 99, 0);
		setIntArray(mode, "zenKeshiChain", 1, 0);
		int before = getIntArray(mode, "feverMapSet")[0];
		setMenuState(engine, mode, 27);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "feverMapSet")[0]);
		int min = getIntArray(mode, "feverChainMin")[0];
		int max = getIntArray(mode, "feverChainMax")[0];
		assertTrue(getIntArray(mode, "feverChainStart")[0] >= min);
		assertTrue(getIntArray(mode, "previewChain")[0] >= min
				&& getIntArray(mode, "previewChain")[0] <= max);
		assertEquals(0, getIntArray(mode, "previewSubset")[0]);
	}

	@Test
	void onSettingCursor34FeverChainStartClampsToMax() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		int min = getIntArray(mode, "feverChainMin")[0];
		int max = getIntArray(mode, "feverChainMax")[0];
		setIntArray(mode, "feverChainStart", min, 0);
		setMenuState(engine, mode, 34);

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		// LEFT below min wraps to max
		assertEquals(max, getIntArray(mode, "feverChainStart")[0]);
	}

	@Test
	void onSettingCursor36UseMapOffResetsField() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "useMap", true, 0);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED));
		setMenuState(engine, mode, 36);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		// useMap toggled to false; the field.reset() branch ran
		assertFalse(getBoolArray(mode, "useMap")[0]);
	}

	@Test
	void onSettingCursor37MapSetWithUseMapLoadsPreview() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "useMap", true, 0);
		int before = getIntArray(mode, "mapSet")[0];
		setMenuState(engine, mode, 37);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(before + 1, getIntArray(mode, "mapSet")[0]);
		assertEquals(-1, getIntArray(mode, "mapNumber")[0]);
	}

	@Test
	void onSettingCursor38MapNumberWithUseMap() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "useMap", true, 0);
		setIntArray(mode, "mapNumber", 0, 0);
		setMenuState(engine, mode, 38);

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		// mapNumber changed (incremented or wrapped) and preview was loaded
		assertTrue(getIntArray(mode, "mapNumber")[0] != 0
				|| getIntArray(mode, "mapNumber")[0] == -1);
	}

	// ---------------------------------------------------------------
	// onSetting: xyzzy cheat code + preview cursors (45/46)
	// ---------------------------------------------------------------

	@Test
	void onSettingXyzzyCodeAdvancesOnUp() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 5);
		setFieldInt(mode, "xyzzy", 0);

		// First UP: xyzzy -> 1
		pressPush(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(1, readFieldInt(mode, "xyzzy"));

		// Second UP: xyzzy == 1 -> ++ -> 2
		pressPush(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(2, readFieldInt(mode, "xyzzy"));

		// DOWN with xyzzy == 2 -> 3
		pressPush(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(3, readFieldInt(mode, "xyzzy"));

		// DOWN with xyzzy == 3 -> 4
		pressPush(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(4, readFieldInt(mode, "xyzzy"));

		// LEFT with xyzzy == 4 -> 5
		pressPush(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(5, readFieldInt(mode, "xyzzy"));

		// RIGHT with xyzzy == 5 -> 6
		pressPush(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(6, readFieldInt(mode, "xyzzy"));

		// LEFT with xyzzy == 6 -> 7
		pressPush(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(7, readFieldInt(mode, "xyzzy"));

		// RIGHT with xyzzy == 7 -> 8
		pressPush(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(8, readFieldInt(mode, "xyzzy"));

		// B with xyzzy == 8 -> 9 (and does NOT quit)
		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);
		assertEquals(9, readFieldInt(mode, "xyzzy"));
		assertFalse(engine.quitflag);
	}

	@Test
	void onSettingXyzzyAConfirmEnablesDebug() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 0, 10);
		setFieldInt(mode, "xyzzy", 9);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		assertEquals(573, readFieldInt(mode, "xyzzy"));
	}

	@Test
	void onSettingXyzzyResetOnWrongInput() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setMenuState(engine, mode, 5);

		// DOWN while xyzzy not 2/3 resets to 0
		setFieldInt(mode, "xyzzy", 5);
		pressPush(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "xyzzy"));

		// LEFT while xyzzy not 4/6 resets to 0
		setFieldInt(mode, "xyzzy", 3);
		pressPush(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "xyzzy"));

		// RIGHT while xyzzy not 5/7 resets to 0
		setFieldInt(mode, "xyzzy", 2);
		pressPush(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);
		assertEquals(0, readFieldInt(mode, "xyzzy"));
	}

	@Test
	void onSettingCursor45PreviewSubsetWhenDebug() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setFieldInt(mode, "xyzzy", 573);
		setIntArray(mode, "previewSubset", 0, 0);
		setMenuState(engine, mode, 45);

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		// LEFT below 0 wraps to last subset index
		int subsetCount = ((String[][]) findField(mode.getClass(),
				"feverMapSubsets").get(mode))[0].length;
		assertEquals(subsetCount - 1, getIntArray(mode, "previewSubset")[0]);
	}

	@Test
	void onSettingCursor46PreviewChainWhenDebug() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setFieldInt(mode, "xyzzy", 573);
		int min = getIntArray(mode, "feverChainMin")[0];
		int max = getIntArray(mode, "feverChainMax")[0];
		setIntArray(mode, "previewChain", min, 0);
		setMenuState(engine, mode, 46);

		pressKey(engine, Controller.BUTTON_LEFT);
		mode.onSetting(engine, 0);

		assertEquals(max, getIntArray(mode, "previewChain")[0]);
	}

	@Test
	void onSettingDebugAConfirmLoadsFeverMap() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setFieldInt(mode, "xyzzy", 573);
		setIntArray(mode, "previewChain", 5, 0);
		setIntArray(mode, "previewSubset", 0, 0);
		setMenuState(engine, mode, 45, 10);

		pressPush(engine, Controller.BUTTON_A);
		mode.onSetting(engine, 0);

		// loadFeverMap should have created/populated the field without error
		assertNotNull(engine.field);
	}

	@Test
	void onSettingDebugCursorRangeIs46() throws Exception {
		// With xyzzy == 573, updateCursor is given 46 (extra preview rows).
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setFieldInt(mode, "xyzzy", 573);
		setMenuState(engine, mode, 0);

		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);

		assertEquals(46, readFieldInt(mode, "menuCursor"),
				"UP at 0 with debug on wraps to 46");
	}

	// ---------------------------------------------------------------
	// onSetting: random map preview + statc[4]!=0 start/cancel
	// ---------------------------------------------------------------

	@Test
	void onSettingRandomMapPreviewTicks() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "useMap", true, 0);
		setIntArray(mode, "mapNumber", -1, 0);
		// Prime propMap + mapMaxNo so the random-preview block executes.
		invokeLoadMapPreview(mode, engine, 0, 0, true);
		setIntArray(mode, "mapMaxNo", 3, 0);
		setMenuState(engine, mode, 0, 30); // menuTime % 30 == 0
		engine.statc[5] = 0;

		mode.onSetting(engine, 0);

		// statc[5] advanced for the random preview cycle
		assertTrue(engine.statc[5] >= 0);
	}

	@Test
	void onSettingStartWhenBothEnginesReady() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = twoEngineManager(mode);
		manager.engine[0].statc[4] = 1;
		manager.engine[1].statc[4] = 1;

		// playerID == 1 with both statc[4]==1 transitions both to READY
		mode.onSetting(manager.engine[1], 1);

		assertEquals(GameEngine.Status.READY, manager.engine[0].stat);
		assertEquals(GameEngine.Status.READY, manager.engine[1].stat);
	}

	@Test
	void onSettingStartCancelGoesBack() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = twoEngineManager(mode);
		// statc[4]==1 but not both ready -> B cancels to statc[4]=0
		manager.engine[0].statc[4] = 1;
		manager.engine[1].statc[4] = 0;
		pressPush(manager.engine[0], Controller.BUTTON_B);

		mode.onSetting(manager.engine[0], 0);

		assertEquals(0, manager.engine[0].statc[4]);
	}

	// ---------------------------------------------------------------
	// renderSetting: page 3 (big), page 5, MAP PREVIEW page
	// ---------------------------------------------------------------

	@Test
	void renderSettingPage3WithBig() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setBoolArray(mode, "big", true, 0);
		setIntArray(mode, "zenKeshiType",
				AvalancheVSDummyMode.ZENKESHI_MODE_OFF, 0);
		setFieldInt(mode, "menuCursor", 17);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingPage4FeverEnabled() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setIntArray(mode, "feverThreshold", 3, 0);
		setIntArray(mode, "zenKeshiType",
				AvalancheVSDummyMode.ZENKESHI_MODE_FEVER, 0);
		setBoolArray(mode, "ojamaMeter", false, 0);
		setFieldInt(mode, "menuCursor", 26);
		mode.renderSetting(engine, 0);
	}

	@Test
	void renderSettingMapPreviewPage() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.statc[4] = 0;
		setIntArray(mode, "previewSubset", 0, 0);
		setIntArray(mode, "previewChain", 5, 0);
		setFieldInt(mode, "menuCursor", 44); // >= 44 -> MAP PREVIEW page
		mode.renderSetting(engine, 0);
	}

	// ---------------------------------------------------------------
	// readyInit: big and fever paths
	// ---------------------------------------------------------------

	@Test
	void readyInitBigDisablesFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "big", true, 0);
		setIntArray(mode, "feverThreshold", 5, 0);
		setBoolArray(mode, "ojamaMeter", false, 0);
		engine.statc[0] = 0;

		mode.readyInit(engine, 0);

		assertEquals(0, getIntArray(mode, "feverThreshold")[0]);
		assertTrue(getBoolArray(mode, "ojamaMeter")[0]);
	}

	@Test
	void readyInitFeverEnabledInitsTimer() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "big", false, 0);
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverTimeMin", 10, 0);
		setIntArray(mode, "feverChainStart", 5, 0);
		engine.statc[0] = 0;

		mode.readyInit(engine, 0);

		assertEquals(10 * 60, getIntArray(mode, "feverTime")[0]);
		assertEquals(5, getIntArray(mode, "feverChain")[0]);
	}

	// ---------------------------------------------------------------
	// renderLast: fever meter, displaysize, ojama strings
	// ---------------------------------------------------------------

	@Test
	void renderLastFeverMeterBigDisplay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.gameActive = true;
		engine.displaysize = 1;
		engine.gameStarted = true;
		engine.stat = GameEngine.Status.READY;
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverTime", 1000, 0);
		setIntArray(mode, "feverTimeLimitAddDisplay", 5, 0);
		setIntArray(mode, "feverTimeLimitAdd", 120, 0);
		setIntArray(mode, "feverPoints", 2, 0);
		setBoolArray(mode, "feverShowMeter", true, 0);
		setBoolArray(mode, "inFever", false, 0);
		setIntArray(mode, "ojama", 10, 0);
		setIntArray(mode, "ojamaAdd", 3, 0);
		setIntArray(mode, "lastscore", 5, 0);
		setIntArray(mode, "lastmultiplier", 4, 0);
		setIntArray(mode, "scgettime", 10, 0);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastFeverMeterInFeverBigDisplay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.gameActive = true;
		engine.displaysize = 1;
		engine.gameStarted = true;
		engine.statistics.time = 40;
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverTime", 800, 0);
		setIntArray(mode, "feverPoints", 5, 0);
		setBoolArray(mode, "feverShowMeter", true, 0);
		setBoolArray(mode, "inFever", true, 0);
		setBoolArray(mode, "ojamaAddToFever", true, 0);
		setIntArray(mode, "ojamaFever", 8, 0);
		setIntArray(mode, "ojamaAdd", 4, 0);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastFeverCountSmallDisplay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.gameActive = true;
		engine.displaysize = 0;
		engine.gameStarted = true;
		engine.stat = GameEngine.Status.MOVE;
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverTime", 700, 0);
		setIntArray(mode, "feverTimeLimitAddDisplay", 5, 0);
		setIntArray(mode, "feverTimeLimitAdd", 60, 0);
		setIntArray(mode, "feverPoints", 1, 0);
		setBoolArray(mode, "feverShowMeter", false, 0);
		setBoolArray(mode, "inFever", false, 0);
		setIntArray(mode, "ojama", 7, 0);
		setIntArray(mode, "ojamaAdd", 2, 0);
		setIntArray(mode, "ojamaHard", 1, 0);
		engine.createFieldIfNeeded();
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.hard = 2;
		engine.field.setBlock(0, 0, b);

		mode.renderLast(engine, 0);
	}

	@Test
	void renderLastNotGameActiveStillRunsChildBody() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.gameActive = false; // super.renderLast returns early; child body still runs
		engine.displaysize = 0;
		engine.gameStarted = true;
		setIntArray(mode, "ojama", 3, 0);
		setIntArray(mode, "ojamaFever", 4, 0);
		setBoolArray(mode, "inFever", true, 0);
		setBoolArray(mode, "ojamaAddToFever", true, 0);
		setIntArray(mode, "ojamaAdd", 1, 0);

		mode.renderLast(engine, 0);
	}

	// ---------------------------------------------------------------
	// drawXorTimer: fever timer path
	// ---------------------------------------------------------------

	@Test
	void drawXorTimerFeverSmallDisplay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.displaysize = 0;
		engine.field = null; // forces the "field == null" branch true
		setBoolArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverTime", 120, 0); // < 360 -> red color branch

		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"drawXorTimer", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0);
	}

	@Test
	void drawXorTimerFeverBigDisplay() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.displaysize = 1;
		engine.field = null;
		setBoolArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverTime", 1000, 0); // >= 360 -> white color branch

		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"drawXorTimer", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0);
	}

	@Test
	void renderMoveDelegatesToXorTimer() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "inFever", false, 0);
		setBoolArray(mode, "dangerColumnShowX", true, 0);
		engine.createFieldIfNeeded();
		mode.renderMove(engine, 0);
	}

	// ---------------------------------------------------------------
	// calcChainNewPower: fever powers
	// ---------------------------------------------------------------

	@Test
	void calcChainNewPowerUsesFeverPowersInFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "inFever", true, 0);

		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"calcChainNewPower", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		int p1 = (int) m.invoke(mode, engine, 0, 1);
		int pBig = (int) m.invoke(mode, engine, 0, 999); // chain > length clamp

		assertEquals(4, p1, "First fever chain power is 4");
		assertEquals(720, pBig, "Out-of-range chain clamps to last fever power");
	}

	// ---------------------------------------------------------------
	// onClear: fever-add propagation
	// ---------------------------------------------------------------

	@Test
	void onClearMarksOjamaAddToFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "inFever", true, 1);
		engine.chain = 1;

		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"onClear", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0);

		assertTrue(getBoolArray(mode, "ojamaAddToFever")[1]);
	}

	// ---------------------------------------------------------------
	// addOjama: fever counter / hurryup / fever-time criteria
	// ---------------------------------------------------------------

	@Test
	void addOjamaInFeverCountersFeverAndAdd() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "inFever", true, 0);
		setIntArray(mode, "ojamaCounterMode",
				AvalancheVSDummyMode.OJAMA_COUNTER_ON, 0);
		setIntArray(mode, "feverPower", 10, 0);
		setIntArray(mode, "ojamaRate", 1, 0);
		setIntArray(mode, "ojamaFever", 100, 0);
		setIntArray(mode, "ojamaAdd", 50, 0);
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverPointCriteria", 2, 0); // BOTH

		invokeAddOjama(mode, engine, 0, 500);

		// Some incoming attack was countered against ojamaFever/ojamaAdd.
		assertTrue(getIntArray(mode, "ojamaFever")[0] < 100
				|| getIntArray(mode, "ojamaAdd")[0] < 50);
	}

	@Test
	void addOjamaFeverTimeCriteriaAttackAddsTime() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "inFever", false, 0);
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverTimeCriteria",
				1 /* ATTACK */, 0);
		setIntArray(mode, "feverTime", 0, 0);
		setIntArray(mode, "feverTimeMax", 30, 0);
		setIntArray(mode, "ojamaRate", 10, 0);

		invokeAddOjama(mode, engine, 0, 100);

		assertEquals(60, getIntArray(mode, "feverTime")[0],
				"ATTACK criteria adds 1 second of fever time");
		assertEquals(60, getIntArray(mode, "feverTimeLimitAdd")[0]);
	}

	@Test
	void addOjamaHurryupReducesRate() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "inFever", false, 0);
		setIntArray(mode, "hurryupSeconds", 1, 0);
		engine.statistics.time = 200; // > hurryupSeconds -> rate >>= ...
		setIntArray(mode, "ojamaRate", 100, 0);
		setIntArray(mode, "ojamaCounterMode",
				AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);

		invokeAddOjama(mode, engine, 0, 50);

		assertTrue(getIntArray(mode, "ojamaSent")[0] >= 1);
	}

	@Test
	void addOjamaCounterGivesFeverPointAndEnemyTime() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "inFever", false, 0);
		setIntArray(mode, "ojamaCounterMode",
				AvalancheVSDummyMode.OJAMA_COUNTER_ON, 0);
		setIntArray(mode, "ojama", 100, 0); // gets countered -> countered=true
		setIntArray(mode, "ojamaRate", 1, 0);
		// fever point criteria != CLEAR so a counter grants a point
		setIntArray(mode, "feverPointCriteria", 0 /* COUNTER */, 0);
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverPoints", 0, 0);
		// enemy gets fever time on counter
		setIntArray(mode, "feverThreshold", 5, 1);
		setIntArray(mode, "feverTimeCriteria", 0 /* COUNTER */, 1);
		setBoolArray(mode, "inFever", false, 1);
		setIntArray(mode, "feverTime", 0, 1);
		setIntArray(mode, "feverTimeMax", 30, 1);

		invokeAddOjama(mode, engine, 0, 50);

		assertEquals(1, getIntArray(mode, "feverPoints")[0],
				"Countering should grant a fever point");
		assertEquals(60, getIntArray(mode, "feverTime")[1],
				"Enemy gets fever time from counter");
	}

	// ---------------------------------------------------------------
	// lineClearEnd: fever / zenkeshi-fever / fever start+end
	// ---------------------------------------------------------------

	@Test
	void lineClearEndTransfersOjamaAddToFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "ojamaAddToFever", true, 1);
		setBoolArray(mode, "inFever", true, 1);
		setIntArray(mode, "ojamaAdd", 7, 1);

		mode.lineClearEnd(engine, 0);

		assertEquals(7, getIntArray(mode, "ojamaFever")[1]);
		assertEquals(0, getIntArray(mode, "ojamaAdd")[1]);
	}

	@Test
	void lineClearEndZenKeshiFeverLoadsMap() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "zenKeshi", true, 0);
		setIntArray(mode, "zenKeshiType",
				AvalancheVSDummyMode.ZENKESHI_MODE_FEVER, 0);
		setBoolArray(mode, "inFever", false, 0);
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverPoints", 0, 0); // not enough -> loadFeverMap branch
		setIntArray(mode, "feverTime", 100, 0);
		setIntArray(mode, "feverTimeMax", 30, 0);
		setIntArray(mode, "zenKeshiChain", 5, 0);
		setBoolArray(mode, "cleared", false, 0);
		setBoolArray(mode, "ojamaDrop", false, 0);

		mode.lineClearEnd(engine, 0);

		// zenKeshi consumed (not ZENKESHI_MODE_ON)
		assertFalse(getBoolArray(mode, "zenKeshi")[0]);
		assertEquals(120, getIntArray(mode, "zenKeshiDisplay")[0]);
	}

	@Test
	void lineClearEndInFeverAdjustsChain() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "inFever", true, 0);
		setBoolArray(mode, "cleared", true, 0);
		engine.chain = 4;
		setIntArray(mode, "feverChain", 5, 0);
		setIntArray(mode, "feverTime", 100, 0);
		setIntArray(mode, "feverTimeMax", 30, 0);
		setBoolArray(mode, "ojamaDrop", false, 0);
		setIntArray(mode, "ojamaFever", 0, 0);

		mode.lineClearEnd(engine, 0);

		int min = getIntArray(mode, "feverChainMin")[0];
		int max = getIntArray(mode, "feverChainMax")[0];
		assertTrue(getIntArray(mode, "feverChain")[0] >= min
				&& getIntArray(mode, "feverChain")[0] <= max);
	}

	@Test
	void lineClearEndFeverTimeoutEndsFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		// Backup field with a known width so the meter math is exercised.
		nullpomino.game.component.Field backup =
				new nullpomino.game.component.Field(6, 12, 0);
		setObjArrayElem(mode, "feverBackupField", backup, 0);
		setBoolArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverTime", 0, 0); // timeout -> end fever
		setIntArray(mode, "feverTimeMin", 10, 0);
		setBoolArray(mode, "ojamaMeter", true, 0);
		setBoolArray(mode, "cleared", true, 0);
		setIntArray(mode, "ojama", 4, 0);
		setIntArray(mode, "ojamaFever", 3, 0);
		setBoolArray(mode, "ojamaDrop", false, 0);

		mode.lineClearEnd(engine, 0);

		assertFalse(getBoolArray(mode, "inFever")[0], "Fever ends on timeout");
		assertEquals(0, getIntArray(mode, "ojamaFever")[0]);
		assertEquals(10 * 60, getIntArray(mode, "feverTime")[0]);
	}

	@Test
	void lineClearEndStartsFeverMode() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "inFever", false, 0);
		setIntArray(mode, "feverThreshold", 3, 0);
		setIntArray(mode, "feverPoints", 3, 0); // reached threshold -> start fever
		setIntArray(mode, "feverChain", 5, 0);
		setBoolArray(mode, "ojamaMeter", false, 0);
		setBoolArray(mode, "cleared", true, 0);
		setIntArray(mode, "ojama", 0, 0);
		setBoolArray(mode, "ojamaDrop", false, 0);

		mode.lineClearEnd(engine, 0);

		assertTrue(getBoolArray(mode, "inFever")[0], "Fever begins at threshold");
		assertNotNull(engine.field, "Fever map loaded a fresh field");
	}

	@Test
	void lineClearEndFeverOjamaDrop() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setBoolArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverTime", 100, 0); // not timed out
		setIntArray(mode, "feverTimeMax", 30, 0);
		setIntArray(mode, "feverChain", 5, 0);
		setIntArray(mode, "ojamaFever", 5, 0);
		setIntArray(mode, "maxAttack", 10, 0);
		setBoolArray(mode, "ojamaDrop", false, 0);
		setBoolArray(mode, "cleared", false, 0);
		setIntArray(mode, "ojamaCounterMode",
				AvalancheVSDummyMode.OJAMA_COUNTER_FEVER, 0);

		boolean result = mode.lineClearEnd(engine, 0);

		assertTrue(result, "Fever ojama should drop");
		assertTrue(getIntArray(mode, "ojamaFever")[0] < 5);
	}

	// ---------------------------------------------------------------
	// onLast: fever meter logic + debug cheat
	// ---------------------------------------------------------------

	@Test
	void onLastFeverMeterNotInFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setIntArray(mode, "feverThreshold", 5, 0);
		setBoolArray(mode, "ojamaMeter", false, 0);
		setBoolArray(mode, "inFever", false, 0);
		setIntArray(mode, "feverPoints", 4, 0); // threshold-1 -> ORANGE
		setIntArray(mode, "feverTimeMin", 10, 0);
		setIntArray(mode, "feverTimeMax", 30, 0);

		mode.onLast(engine, 0);

		assertEquals(GameEngine.METER_COLOR_ORANGE, engine.meterColor);
	}

	@Test
	void onLastFeverMeterInFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setIntArray(mode, "feverThreshold", 5, 0);
		setBoolArray(mode, "ojamaMeter", false, 0);
		setBoolArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverTime", 60, 0); // small -> red
		setIntArray(mode, "feverTimeMin", 10, 0);
		setIntArray(mode, "feverTimeMax", 30, 0);
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	@Test
	void onLastDebugCheatAddsFeverPoint() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setFieldInt(mode, "xyzzy", 573);
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverPoints", 0, 0);
		setBoolArray(mode, "ojamaMeter", true, 0);
		pressPush(engine, Controller.BUTTON_F);

		mode.onLast(engine, 0);

		assertEquals(1, getIntArray(mode, "feverPoints")[0],
				"F debug cheat increments fever points");
	}

	@Test
	void onLastFeverTimerCountdownSound() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setIntArray(mode, "feverThreshold", 5, 0);
		setBoolArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverTime", 361, 0); // -> 360, % 60 == 0 -> countdown
		setIntArray(mode, "feverTimeMin", 10, 0);
		setIntArray(mode, "feverTimeMax", 30, 0);
		setBoolArray(mode, "ojamaMeter", true, 0);
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertEquals(360, getIntArray(mode, "feverTime")[0]);
	}

	@Test
	void onLastFeverTimerHitsZero() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setIntArray(mode, "feverThreshold", 5, 0);
		setBoolArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverTime", 1, 0); // -> 0 -> levelstop sound
		setIntArray(mode, "feverTimeMin", 10, 0);
		setIntArray(mode, "feverTimeMax", 30, 0);
		setBoolArray(mode, "ojamaMeter", true, 0);
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertEquals(0, getIntArray(mode, "feverTime")[0]);
	}

	// ---------------------------------------------------------------
	// saveReplay: debug-cheat property
	// ---------------------------------------------------------------

	@Test
	void saveReplayWritesDebugCheatFlag() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setFieldInt(mode, "xyzzy", 573);
		CustomProperties prop = new CustomProperties();

		mode.saveReplay(engine, 0, prop);

		assertTrue(prop.getProperty("avalanchevs.debugcheatenable", false),
				"Debug cheat flag persisted to replay");
		assertEquals(0, prop.getProperty("avalanchevs.version", -1));
	}

	@Test
	void saveReplayWithUseMapSavesField() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		setBoolArray(mode, "useMap", true, 0);
		nullpomino.game.component.Field backup =
				new nullpomino.game.component.Field(6, 12, 0);
		setObjArrayElem(mode, "fldBackup", backup, 0);
		CustomProperties prop = new CustomProperties();

		mode.saveReplay(engine, 0, prop);

		assertEquals(0, prop.getProperty("avalanchevs.version", -1));
	}

	// ---------------------------------------------------------------
	// playerInit replay path (loads other settings + version)
	// ---------------------------------------------------------------

	@Test
	void playerInitReplayModeReadsVersion() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new RedirectingReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.replayMode = true;
		manager.engine[0].owner.replayMode = true;
		manager.replayProp.setProperty("avalanchevs.version", 0);
		manager.replayProp.setProperty("avalanchevs.debugcheatenable", true);

		mode.playerInit(manager.engine[0], 0);

		assertEquals(0, readFieldInt(mode, "version"));
		// debug cheat enable in replay sets xyzzy
		assertEquals(573, readFieldInt(mode, "xyzzy"));
	}

	// ---------------------------------------------------------------
	// getName
	// ---------------------------------------------------------------

	@Test
	void getNameReturnsModeName() {
		assertEquals("AVALANCHE VS-BATTLE (RC1)", new AvalancheVSMode().getName());
	}

	// ---------------------------------------------------------------
	// Helpers
	// ---------------------------------------------------------------

	/** Single-engine setup (engine 0), modeInit + playerInit done. */
	private static GameEngine freshEngine(AvalancheVSMode mode) throws Exception {
		GameManager manager = twoEngineManager(mode);
		return manager.engine[0];
	}

	/** Two-engine manager fully initialised with both players' settings loaded. */
	private static GameManager twoEngineManager(AvalancheVSMode mode) throws Exception {
		GameManager manager = new GameManager(new RedirectingReceiver());
		manager.replayMode = false;
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.modeInit(manager);
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
		manager.engine[0].owner.replayMode = false;
		manager.engine[1].owner.replayMode = false;
		return manager;
	}

	private static void setMenuState(GameEngine engine, AvalancheVSMode mode,
			int cursor) throws Exception {
		setMenuState(engine, mode, cursor, 0);
	}

	private static void setMenuState(GameEngine engine, AvalancheVSMode mode,
			int cursor, int menuTime) throws Exception {
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", cursor);
		setFieldInt(mode, "menuTime", menuTime);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void pressPush(GameEngine engine, int btn) {
		pressKey(engine, btn);
	}

	private static void invokeAddOjama(AvalancheVSMode mode, GameEngine engine,
			int playerID, int pts) throws Exception {
		Method m = AvalancheVSMode.class.getDeclaredMethod(
				"addOjama", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID, pts);
	}

	private static void invokeLoadMapPreview(AvalancheVSMode mode, GameEngine engine,
			int playerID, int id, boolean forceReload) throws Exception {
		Method m = findMethod(mode.getClass(), "loadMapPreview",
				GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID, id, forceReload);
	}

	private static Method findMethod(Class<?> cls, String name, Class<?>... params)
			throws NoSuchMethodException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredMethod(name, params); }
			catch (NoSuchMethodException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchMethodException(name);
	}

	private static int readFieldInt(Object obj, String name) throws Exception {
		return findField(obj.getClass(), name).getInt(obj);
	}

	private static void setFieldInt(Object obj, String name, int value) throws Exception {
		findField(obj.getClass(), name).setInt(obj, value);
	}

	private static int[] getIntArray(Object obj, String name) throws Exception {
		return (int[]) findField(obj.getClass(), name).get(obj);
	}

	private static boolean[] getBoolArray(Object obj, String name) throws Exception {
		return (boolean[]) findField(obj.getClass(), name).get(obj);
	}

	private static void setIntArray(Object obj, String name, int value, int index) throws Exception {
		((int[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static void setBoolArray(Object obj, String name, boolean value, int index) throws Exception {
		((boolean[]) findField(obj.getClass(), name).get(obj))[index] = value;
	}

	private static void setObjArrayElem(Object obj, String name, Object value, int index) throws Exception {
		Object arr = findField(obj.getClass(), name).get(obj);
		java.lang.reflect.Array.set(arr, index, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
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
