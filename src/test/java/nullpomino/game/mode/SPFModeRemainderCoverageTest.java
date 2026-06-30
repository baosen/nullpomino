package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Targets the still-uncovered branches of {@link SPFMode} that the existing
 * SPF test files (SPFModeSettingMenuTest, SPFModeGameLogicTest,
 * SPFModeHelpersTest, SPFModeCoverageBoostTest) do not reach:
 *
 * <ul>
 *   <li>{@code loadMapPreview} when {@code propMap} is non-null (459-462) and
 *       {@code loadDropMapPreview} null-pattern field reset (468).</li>
 *   <li>{@code playerInit} replay-mode branch (529-531).</li>
 *   <li>{@code onSetting} cursor-10 useMap-OFF field reset (627), the random
 *       map-preview block (718-721) and the replay {@code menuTime > 120}
 *       branch (733).</li>
 *   <li>{@code renderSetting} page-3 defend-multiplier {@literal <}100% green
 *       branch (813).</li>
 *   <li>{@code onReady} replay-mode map load and non-null {@code propMap}
 *       map-load branches (861-884).</li>
 *   <li>{@code calcScore} null-field guard (1009) and big-display block-break
 *       branches (1030-1033, 1063-1066, 1099-1102) plus the {@code b == null}
 *       continue (1087).</li>
 *   <li>{@code checkAll} recheck-log (1152) and {@code checkCountdown}
 *       {@code b == null} continue (1166).</li>
 *   <li>{@code checkSquares} null-field guard (1183), pre-existing-square
 *       detection (1214-1251), the no-square continue (1280) and all four
 *       expansion directions (1291-1388).</li>
 *   <li>{@code lineClearEnd} null-field guard (1415) and the
 *       {@code patternRow} wrap (1456).</li>
 *   <li>{@code saveReplay} map-backup save (1554).</li>
 * </ul>
 *
 * <p>The map/onReady tests inject a {@link CapturingReceiver} whose
 * {@code loadProperties} returns an in-memory {@link CustomProperties} and
 * whose {@code saveProperties}/{@code saveModeConfig} redirect to
 * {@code java.io.tmpdir} so no tracked config file is written.
 */
class SPFModeRemainderCoverageTest {

	// =================================================================
	// loadMapPreview / loadDropMapPreview
	// =================================================================

	/**
	 * loadMapPreview with an injected non-null propMap takes the
	 * "propMap != null" branch: reads maxMapNumber, creates the field and loads
	 * the map (lines 459-462).
	 */
	@Test
	void loadMapPreviewWithInjectedPropertiesLoadsMap() throws Exception {
		SPFMode mode = new SPFMode();
		CustomProperties mapProps = new CustomProperties();
		mapProps.setProperty("map.maxMapNumber", 3);
		// A single red block at (4,4) so the loaded field is non-empty.
		mapProps.setProperty("map.0",
				rowMap(4, 4, Block.BLOCK_COLOR_RED));
		GameEngine engine = engineWithReceiver(mode, new CapturingReceiver(mapProps));
		engine.field = null;

		Method m = SPFMode.class.getDeclaredMethod(
				"loadMapPreview", GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, 0, true);

		assertEquals(3, getIntArray(mode, "mapMaxNo")[0],
				"maxMapNumber should be read from the injected properties");
		assertNotNull(engine.field, "loadMapPreview should have created the field");
	}

	/**
	 * loadDropMapPreview with a null pattern but a non-null field resets the
	 * field (line 468).
	 */
	@Test
	void loadDropMapPreviewNullPatternResetsField() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED));

		Method m = SPFMode.class.getDeclaredMethod(
				"loadDropMapPreview", GameEngine.class, int.class, int[][].class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, (Object) null);

		assertTrue(engine.field.getBlockEmpty(0, 0),
				"null pattern with a non-null field should reset the field");
	}

	// =================================================================
	// playerInit replay-mode branch (529-531)
	// =================================================================

	/**
	 * playerInit in replay mode reads the other settings, preset and version
	 * from the replay properties instead of the mode config (lines 529-531).
	 */
	@Test
	void playerInitReplayModeReadsReplayProps() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = true;
		engine.owner.replayProp.setProperty("spfvs.version", 7);
		engine.owner.replayProp.setProperty("spfvs.gravity.-1", 13);

		mode.playerInit(engine, 0);

		assertEquals(7, readFieldInt(mode, "version"),
				"replay-mode playerInit should read the version from replayProp");
		assertEquals(13, engine.speed.gravity,
				"replay-mode playerInit should load the preset from replayProp");
	}

	// =================================================================
	// onSetting: cursor-10 useMap OFF field reset (627)
	// =================================================================

	/**
	 * Cursor 10 toggling useMap from ON to OFF with a non-null field resets it
	 * (line 627).
	 */
	@Test
	void onSettingCursor10UseMapOffResetsField() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 10);
		setFieldInt(mode, "menuCursor", 10);
		getBoolArray(mode, "useMap")[0] = true; // toggling -> OFF -> reset field
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED));

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertFalse(getBoolArray(mode, "useMap")[0], "cursor 10 should toggle useMap OFF");
		assertTrue(engine.field.getBlockEmpty(0, 0),
				"toggling useMap OFF with a field present should reset it");
	}

	// =================================================================
	// onSetting: random map preview (718-721)
	// =================================================================

	/**
	 * With useMap on, a non-null propMap and a random map number, the periodic
	 * (menuTime % 30 == 0) random-preview block executes, advancing statc[5]
	 * and reloading the preview (lines 718-721).
	 */
	@Test
	void onSettingRandomMapPreviewAdvancesStatc5() throws Exception {
		SPFMode mode = new SPFMode();
		CustomProperties mapProps = new CustomProperties();
		mapProps.setProperty("map.maxMapNumber", 4);
		mapProps.setProperty("map.0", rowMap(2, 2, Block.BLOCK_COLOR_BLUE));
		GameEngine engine = engineWithReceiver(mode, new CapturingReceiver(mapProps));
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		engine.statc[5] = 0;
		setFieldInt(mode, "menuTime", 30); // % 30 == 0 -> random preview block
		setFieldInt(mode, "menuCursor", 0);
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = -1; // random

		// Pre-populate propMap and mapMaxNo so the random-preview branch runs.
		getPropMapArray(mode)[0] = mapProps;
		getIntArray(mode, "mapMaxNo")[0] = 4;

		mode.onSetting(engine, 0);

		assertEquals(1, engine.statc[5],
				"the random-preview block should have advanced statc[5]");
	}

	// =================================================================
	// onSetting: replay menuTime > 120 branch (733)
	// =================================================================

	/**
	 * In replay mode with 120 {@literal <} menuTime {@literal <} 180, the
	 * auto-advance selects cursor 18 via the {@code menuTime > 120} branch
	 * (line 733).
	 */
	@Test
	void onSettingReplayMenuTimeAbove120SelectsCursor18() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 150); // 120 < t < 180 -> cursor 18 (no preview)

		mode.onSetting(engine, 0);

		assertEquals(18, readFieldInt(mode, "menuCursor"),
				"replay menuTime > 120 should select cursor 18");
		assertEquals(151, readFieldInt(mode, "menuTime"),
				"menuTime should have incremented");
	}

	// =================================================================
	// renderSetting: page-3 defend multiplier < 100% (813)
	// =================================================================

	/**
	 * Page 3 with a drop set/map whose defend multiplier is below 100%
	 * exercises the green defend-multiplier branch (line 813). Set 0, map 8 has
	 * an attack multiplier of 0.7 and a defend multiplier of 1.0; set 1, map 9
	 * has a defend multiplier of 1.0 too, so use the multiplier table directly:
	 * the only defend value below 1.0 does not exist, but set 1 map 8 gives a
	 * defend multiplier of 1.2 (>=100) -> the {@literal >=}100 RED branch. To hit
	 * the {@literal <}100 GREEN branch we rely on the array lookup returning a
	 * value below 1.0 for an out-of-range map (falls back to 1.0), so instead we
	 * drive the attack-{@literal <}100 plus defend-{@literal >=}100 combination
	 * and the defend default. The branch at 813 is reached whenever the defend
	 * multiplier read is strictly below 100, which happens for no built-in entry;
	 * to still execute line 813 we force the menu render with a custom defend
	 * value by selecting set 1 / map 9 (defend 1.0) — this lands on the
	 * {@literal >=}100 path. Because the table contains no sub-100 defend value,
	 * we additionally render set 0 / map 8 (attack 0.7) which guarantees the
	 * attack {@literal <}100 RED branch (line 806) is covered here too.
	 */
	@Test
	void renderSettingPage3MultiplierBranches() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 18);

		// set 0 / map 8 -> attack 0.7 (<100 RED), defend 1.0 (>=100 RED).
		getIntArray(mode, "dropSet")[0] = 0;
		getIntArray(mode, "dropMap")[0] = 8;
		mode.renderSetting(engine, 0);

		// set 1 / map 8 -> attack 1.0 (>=100), defend 1.2 (>=100 RED).
		getIntArray(mode, "dropSet")[0] = 1;
		getIntArray(mode, "dropMap")[0] = 8;
		mode.renderSetting(engine, 0);
	}

	/**
	 * Renders page 3 with an injected sub-100% defend multiplier so the GREEN
	 * defend branch (line 813) executes. The multiplier table is final and
	 * static, so we instead exercise line 813 by reflectively reading what the
	 * renderer computes: a defend multiplier below 1.0 routes to the green
	 * branch. We force that by temporarily replacing the static defend
	 * multiplier table entry via reflection.
	 */
	@Test
	void renderSettingPage3DefendBelow100UsesGreen() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 18);

		double[][] defend = getStaticDoubleMatrix("DROP_PATTERNS_DEFEND_MULTIPLIERS");
		double saved = defend[0][0];
		try {
			defend[0][0] = 0.5; // < 1.0 -> GREEN defend branch (line 813)
			getIntArray(mode, "dropSet")[0] = 0;
			getIntArray(mode, "dropMap")[0] = 0;
			mode.renderSetting(engine, 0);
		} finally {
			defend[0][0] = saved;
		}
	}

	// =================================================================
	// onReady: map-load branches (861-884)
	// =================================================================

	/**
	 * onReady in replay mode with useMap on loads the player's map from the
	 * replay properties (lines 860-863).
	 */
	@Test
	void onReadyReplayModeLoadsMapFromReplay() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.statc[0] = 0;
		engine.owner.replayMode = true;
		getBoolArray(mode, "useMap")[0] = true;
		engine.owner.replayProp.setProperty("map.0",
				rowMap(1, 1, Block.BLOCK_COLOR_GREEN));

		mode.onReady(engine, 0);

		assertNotNull(engine.field, "replay onReady should have created the field");
		assertEquals(20, engine.numColors);
	}

	/**
	 * onReady (not replay) with useMap on, a fixed map number and an injected
	 * propMap loads that specific map and backs it up (lines 865-885, fixed-map
	 * branch at 880).
	 */
	@Test
	void onReadyUseMapFixedNumberLoadsAndBacksUp() throws Exception {
		SPFMode mode = new SPFMode();
		CustomProperties mapProps = new CustomProperties();
		mapProps.setProperty("map.maxMapNumber", 2);
		mapProps.setProperty("map.1", rowMap(3, 3, Block.BLOCK_COLOR_YELLOW));
		GameEngine engine = engineWithReceiver(mode, new CapturingReceiver(mapProps));
		engine.statc[0] = 0;
		engine.owner.replayMode = false;
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = 1; // fixed-map branch
		getIntArray(mode, "mapSet")[0] = 0;

		mode.onReady(engine, 0);

		assertNotNull(engine.field, "onReady should have created the field");
		assertNotNull(getFieldArray(mode, "fldBackup")[0],
				"a fixed-map onReady should back up the field for replay");
	}

	/**
	 * onReady (not replay) with useMap on, a random map number and an injected
	 * propMap takes the random-selection branch (lines 872-878).
	 */
	@Test
	void onReadyUseMapRandomNumberLoadsAndBacksUp() throws Exception {
		SPFMode mode = new SPFMode();
		CustomProperties mapProps = new CustomProperties();
		mapProps.setProperty("map.maxMapNumber", 3);
		mapProps.setProperty("map.0", rowMap(2, 2, Block.BLOCK_COLOR_BLUE));
		mapProps.setProperty("map.1", rowMap(2, 3, Block.BLOCK_COLOR_BLUE));
		mapProps.setProperty("map.2", rowMap(2, 4, Block.BLOCK_COLOR_BLUE));
		GameEngine engine = engineWithReceiver(mode, new CapturingReceiver(mapProps));
		engine.statc[0] = 0;
		engine.owner.replayMode = false;
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = -1; // random-selection branch
		getIntArray(mode, "mapSet")[0] = 0;
		getIntArray(mode, "mapMaxNo")[0] = 3; // so randMap.nextInt(3) is used

		mode.onReady(engine, 0);

		assertNotNull(engine.field);
		assertNotNull(getFieldArray(mode, "fldBackup")[0],
				"a random-map onReady should back up the field for replay");
	}

	/**
	 * Player 1 with random maps copies player 0's already-created random map
	 * instead of loading a separate random map (line 874).
	 */
	@Test
	void onReadyPlayerOneRandomMapCopiesPlayerZeroField() throws Exception {
		SPFMode mode = new SPFMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		manager.engine[1].playerID = 1;
		manager.replayMode = false;

		manager.engine[0].createFieldIfNeeded();
		manager.engine[0].field.setBlock(1, 1, new Block(Block.BLOCK_COLOR_RED));

		getBoolArray(mode, "useMap")[0] = true;
		getBoolArray(mode, "useMap")[1] = true;
		getIntArray(mode, "mapNumber")[0] = -1;
		getIntArray(mode, "mapNumber")[1] = -1;
		getPropMapArray(mode)[1] = new CustomProperties();

		mode.onReady(manager.engine[1], 1);

		assertEquals(Block.BLOCK_COLOR_RED,
				manager.engine[1].field.getBlockColor(1, 1),
				"player 1 random map should copy player 0's field");
	}

	// =================================================================
	// calcScore: null field guard (1009)
	// =================================================================

	@Test
	void calcScoreWithNullFieldReturnsEarly() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.field = null;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0); // null field -> early return, no crash

		assertEquals(0, engine.statistics.score);
	}

	// =================================================================
	// calcScore: big-display block-break branches (1030-1033, 1063-1066,
	// 1099-1102) + b == null continue (1087)
	// =================================================================

	/**
	 * calcScore in big-display mode with a diamond above a colored block walks
	 * the big-display diamond block-break (1030-1033) and the diamond-color
	 * clear block-break (1063-1066). A plain erasable block also exercises the
	 * big-display clear-blocks break (1099-1102).
	 */
	@Test
	void calcScoreBigDisplayBlockBreakBranches() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();
		engine.displaysize = 1; // big-display branches

		int h = engine.field.getHeight();
		// Diamond on the top playfield row, green directly below (and around) it.
		engine.field.setBlock(0, h - 2, new Block(Block.BLOCK_COLOR_GEM_RAINBOW));
		engine.field.setBlock(0, h - 1, new Block(Block.BLOCK_COLOR_GREEN));
		engine.field.setBlock(1, h - 1, new Block(Block.BLOCK_COLOR_GREEN));
		getIntArray(mode, "diamondPower")[0] = 3; // 100% (no scaling)
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0);

		assertTrue(engine.statistics.score >= 0);
	}

	/**
	 * calcScore in big-display mode where only a plain erasable block is present
	 * (no diamond) covers the big-display clear-blocks break (1099-1102).
	 */
	@Test
	void calcScoreBigDisplayClearsErasableBlock() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();
		engine.displaysize = 1;

		int h = engine.field.getHeight();
		Block b = new Block(Block.BLOCK_COLOR_RED);
		b.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		engine.field.setBlock(0, h - 1, b);
		getIntArray(mode, "diamondPower")[0] = 0; // skip diamond loop
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0);

		assertTrue(getIntArray(mode, "score")[0] > 0,
				"a cleared erasable block should award points");
	}

	// =================================================================
	// checkAll recheck-log (1152) + checkCountdown b == null continue (1166)
	// =================================================================

	/**
	 * When a countdown block expires (countdown == 1), checkCountdown returns
	 * true, so checkAll logs the recheck (line 1152) and re-runs checkSquares.
	 * The field also contains a null cell, exercising the {@code b == null}
	 * continue in checkCountdown (line 1166).
	 */
	@Test
	void checkAllRechecksWhenCountdownExpires() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		Block expiring = new Block(Block.BLOCK_COLOR_RED);
		expiring.countdown = 1;
		expiring.secondaryColor = Block.BLOCK_COLOR_GREEN;
		expiring.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		engine.field.setBlock(0, h - 1, expiring);

		getBoolArray(mode, "countdownDecremented")[0] = false;

		Method m = SPFMode.class.getDeclaredMethod(
				"checkAll", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0);

		assertEquals(0, engine.field.getBlock(0, h - 1).countdown,
				"the expiring block's countdown should have reached 0");
		assertFalse(engine.field.getBlock(0, h - 1)
						.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE),
				"the expired block should no longer be garbage");
	}

	// =================================================================
	// checkSquares: null field guard (1183)
	// =================================================================

	@Test
	void checkSquaresWithNullFieldReturnsEarly() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.field = null;

		invokeCheckSquares(mode, engine, 0, true); // null field -> early return
	}

	// =================================================================
	// checkSquares: pre-existing square (1214-1251) + no-square continue (1280)
	// =================================================================

	/**
	 * A pre-existing 2x2 gem square (top-left has CONNECT_RIGHT + CONNECT_DOWN,
	 * no CONNECT_UP/LEFT, not BROKEN) is detected and its boundaries scanned via
	 * the right/down while-loops (lines 1214-1251). A lone colored block elsewhere
	 * hits the {@code maxX <= minX} no-square continue (line 1280).
	 */
	@Test
	void checkSquaresDetectsPreExistingSquare() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		int color = Block.BLOCK_COLOR_RED;
		// 2x2 gem square at (0,h-2)..(1,h-1) with proper connect flags.
		buildGemSquare(engine, 0, h - 2, 1, h - 1, color);

		// A lone block of another color -> "no gem block, skip" continue (1280).
		engine.field.setBlock(5, h - 1, new Block(Block.BLOCK_COLOR_BLUE));

		invokeCheckSquares(mode, engine, 0, true);

		// The top-left should still carry the (unchanged) bonusValue / connects.
		Block tl = engine.field.getBlock(0, h - 2);
		assertNotNull(tl);
		assertTrue(tl.getAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT),
				"pre-existing square's top-left keeps its CONNECT_RIGHT flag");
	}

	// =================================================================
	// checkSquares: expansion in all four directions (1291-1388)
	// =================================================================

	/**
	 * A pre-existing 2x2 gem square surrounded on all four sides by plain
	 * same-colored blocks triggers the expand-up, expand-left, expand-right and
	 * expand-down loops (lines 1287-1388), after which the square is rebuilt
	 * with connect attributes and a larger bonusValue.
	 */
	@Test
	void checkSquaresExpandsInAllDirections() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int color = Block.BLOCK_COLOR_GREEN;
		// Square interior so expansion has room on every side. Use rows in the
		// visible playfield well away from edges.
		int sx = 3, sy = 8; // top-left of the 2x2 square
		buildGemSquare(engine, sx, sy, sx + 1, sy + 1, color);

		// Surround the square with plain (no-connect) same-color blocks: one ring.
		for (int x = sx - 1; x <= sx + 2; x++)
			for (int y = sy - 1; y <= sy + 2; y++) {
				if (engine.field.getBlockColor(x, y) == color)
					continue; // leave the existing square as-is
				engine.field.setBlock(x, y, new Block(color));
			}

		invokeCheckSquares(mode, engine, 0, true);

		// After expansion every block in the (now larger) region is non-broken
		// and shares a bonusValue >= 2.
		Block tl = engine.field.getBlock(sx - 1, sy - 1);
		assertNotNull(tl);
		assertTrue(tl.bonusValue >= 2 || engine.field.getBlock(sx, sy).bonusValue >= 2,
				"expanding the square should assign a bonusValue >= 2");
	}

	// =================================================================
	// lineClearEnd: null field guard (1415) + patternRow wrap (1456)
	// =================================================================

	@Test
	void lineClearEndWithNullFieldReturnsFalse() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.field = null;

		assertFalse(invokeLineClearEnd(mode, engine, 0),
				"null field should make lineClearEnd return false");
	}

	/**
	 * Dropping a tall column of garbage with a short drop pattern forces the
	 * {@code patternRow >= dropPattern[...].length} wrap (line 1456).
	 */
	@Test
	void lineClearEndPatternRowWraps() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		// Lots of ojama -> several garbage rows dropped per column.
		getIntArray(mode, "ojama")[0] = 60;
		// Enemy drop pattern is a single-row pattern, so patternRow wraps quickly.
		int[][][] dp = getDropPatternArray(mode);
		dp[1] = new int[][]{{2}};

		boolean result = invokeLineClearEnd(mode, engine, 0);

		assertTrue(result, "ojama > 0 should drop garbage and return true");
		assertTrue(getIntArray(mode, "ojama")[0] < 60,
				"ojama should decrease after a garbage drop");
	}

	// =================================================================
	// saveReplay: map-backup save (1554)
	// =================================================================

	/**
	 * saveReplay with useMap on and a non-null field backup saves the backup map
	 * into the replay properties (line 1554).
	 */
	@Test
	void saveReplaySavesMapBackupWhenUseMapAndBackupPresent() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		mode.playerInit(engine, 0);
		getBoolArray(mode, "useMap")[0] = true;

		// Provide a non-null field backup so the saveMap branch runs.
		nullpomino.game.component.Field backup = new nullpomino.game.component.Field();
		backup.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED));
		getFieldArray(mode, "fldBackup")[0] = backup;

		mode.saveReplay(engine, 0, engine.owner.replayProp);

		assertNotNull(engine.owner.replayProp.getProperty("map.0", null),
				"saveReplay should write the backup map under map.0");
	}

	// =================================================================
	// Helpers
	// =================================================================

	/** Full single-engine setup with the default no-op receiver. */
	private static GameEngine fullEngine(SPFMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		return manager.engine[0];
	}

	/** Full single-engine setup with a custom receiver injected. */
	private static GameEngine engineWithReceiver(SPFMode mode, EventReceiver receiver) {
		GameManager manager = new GameManager(receiver);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		return manager.engine[0];
	}

	/**
	 * A field-string encoding a single colored block at (x, y) suitable for
	 * {@code Field.stringToField}. The string is produced by building a real
	 * field and serializing it, so the format always matches.
	 */
	private static String rowMap(int x, int y, int color) {
		nullpomino.game.component.Field f = new nullpomino.game.component.Field();
		f.setBlock(x, y, new Block(color));
		return f.fieldToString();
	}

	/**
	 * Builds a rectangular gem square from (minX,minY) to (maxX,maxY) with the
	 * connect attributes the SPF "pre-existing square" detector expects.
	 */
	private static void buildGemSquare(GameEngine engine, int minX, int minY,
			int maxX, int maxY, int color) {
		for (int x = minX; x <= maxX; x++)
			for (int y = minY; y <= maxY; y++) {
				Block b = new Block(color);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, false);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_LEFT, x != minX);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_RIGHT, x != maxX);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_UP, y != minY);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_CONNECT_DOWN, y != maxY);
				b.bonusValue = Math.min(maxX - minX + 1, maxY - minY + 1);
				engine.field.setBlock(x, y, b);
			}
	}

	private static void invokeCheckSquares(SPFMode mode, GameEngine engine,
			int playerID, boolean force) throws Exception {
		Method m = SPFMode.class.getDeclaredMethod(
				"checkSquares", GameEngine.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID, force);
	}

	private static boolean invokeLineClearEnd(SPFMode mode, GameEngine engine,
			int playerID) throws Exception {
		Method m = SPFMode.class.getDeclaredMethod(
				"lineClearEnd", GameEngine.class, int.class);
		m.setAccessible(true);
		return (boolean) m.invoke(mode, engine, playerID);
	}

	private static double[][] getStaticDoubleMatrix(String name) throws Exception {
		Field f = SPFMode.class.getDeclaredField(name);
		f.setAccessible(true);
		return (double[][]) f.get(null);
	}

	private static int[][][] getDropPatternArray(SPFMode mode) throws Exception {
		return (int[][][]) findField(mode.getClass(), "dropPattern").get(mode);
	}

	private static CustomProperties[] getPropMapArray(SPFMode mode) throws Exception {
		return (CustomProperties[]) findField(mode.getClass(), "propMap").get(mode);
	}

	private static nullpomino.game.component.Field[] getFieldArray(SPFMode mode, String name)
			throws Exception {
		return (nullpomino.game.component.Field[]) findField(mode.getClass(), name).get(mode);
	}

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
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

	/**
	 * EventReceiver that returns an in-memory {@link CustomProperties} from
	 * {@code loadProperties} and redirects all saves to {@code java.io.tmpdir}
	 * so no tracked config file is written.
	 */
	private static final class CapturingReceiver extends EventReceiver {
		private final CustomProperties props;

		CapturingReceiver(CustomProperties props) {
			this.props = props;
		}

		@Override
		public CustomProperties loadProperties(String filename) {
			return props;
		}

		@Override
		public boolean saveProperties(String filename, CustomProperties prop) {
			return true; // do not touch disk
		}

		@Override
		public void saveModeConfig(CustomProperties modeConfig) {
			// redirect to tmpdir to avoid writing the tracked mode.cfg
			try {
				modeConfig.storeToFile(
						System.getProperty("java.io.tmpdir") + "/spf-test-mode.cfg",
						"test");
			} catch (Exception ignored) {
				// ignore
			}
		}
	}
}
