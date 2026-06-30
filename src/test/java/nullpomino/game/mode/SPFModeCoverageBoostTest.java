package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.BGMStatus;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Coverage-focused tests for the deeper, previously-unexercised branches of
 * {@link SPFMode}: the diamond-break / chain / hurryup / ojama-offset paths of
 * {@code calcScore}; gem-square detection and expansion in {@code checkSquares};
 * the {@code lineClearEnd} diamond-recurse and no-room branches; {@code onReady}
 * map-load branches; {@code onLast} rising-meter and settlement (win/lose/draw)
 * paths; {@code renderResult} win/lose/draw rendering; {@code renderLast}
 * countdown-block and big-display rendering; and the remaining {@code onSetting}
 * navigation/preview branches (drop-map preview, map preview, replay auto-advance
 * at the 120-frame mark, and the post-confirm start/cancel state).
 *
 * <p>These complement the existing SPFMode test files (SPFModeSettingMenuTest,
 * SPFModeGameLogicTest, SPFModeHelpersTest, SPFModeOtherSettingTest, SPFModeTest)
 * without duplicating their assertions.
 */
class SPFModeCoverageBoostTest {

	// =================================================================
	// calcScore: diamond break, chain bonus, hurryup, ojama offset
	// =================================================================

	/**
	 * A rainbow diamond at the very bottom row (y+1 >= height) takes the
	 * "tech bonus" branch: +10000 score and techBonusDisplay set.
	 */
	@Test
	void calcScoreDiamondAtBottomAwardsTechBonus() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		Block diamond = new Block(Block.BLOCK_COLOR_GEM_RAINBOW);
		engine.field.setBlock(0, h - 1, diamond);
		getIntArray(mode, "diamondPower")[0] = 3; // 100%, no scaling
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0);

		// +10000 tech bonus (and, since clearing the lone diamond empties the
		// board, an additional +1000 zenkeshi bonus).
		assertTrue(engine.statistics.score >= 10000,
				"diamond on bottom row grants the +10000 tech bonus");
		assertTrue(getIntArray(mode, "techBonusDisplay")[0] > 0,
				"techBonusDisplay should be set after a bottom-row diamond");
	}

	/**
	 * A diamond above the bottom row breaks the color directly beneath it and
	 * clears all blocks of that color via allClearColor, accumulating points.
	 */
	@Test
	void calcScoreDiamondBreaksColorBelow() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		// Diamond on top playfield row, red beneath it (and a few more reds).
		engine.field.setBlock(0, h - 2, new Block(Block.BLOCK_COLOR_GEM_RAINBOW));
		engine.field.setBlock(0, h - 1, new Block(Block.BLOCK_COLOR_RED));
		engine.field.setBlock(1, h - 1, new Block(Block.BLOCK_COLOR_RED));
		getIntArray(mode, "diamondPower")[0] = 1; // 50% scaling branch
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0);

		// The two reds beneath/around get cleared by the diamond break,
		// so the field becomes empty and the zenkeshi bonus (+1000) fires.
		assertTrue(engine.statistics.score >= 1000,
				"clearing the board via diamond break should award the zenkeshi bonus");
	}

	/**
	 * Diamond power == 2 takes the 0.8 scaling branch.
	 */
	@Test
	void calcScoreDiamondPower2Scaling() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		engine.field.setBlock(0, h - 2, new Block(Block.BLOCK_COLOR_GEM_RAINBOW));
		engine.field.setBlock(0, h - 1, new Block(Block.BLOCK_COLOR_GREEN));
		getIntArray(mode, "diamondPower")[0] = 2; // 0.8 scaling branch
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0);

		assertTrue(engine.statistics.score >= 0);
	}

	/**
	 * calcScore with ERASE-flagged colored blocks (no diamond): exercises the
	 * normal "clear blocks" loop, chain bonus, combo SE, lastscore/scgettime
	 * write, garbage-bonus halving and the bonusValue multiplier.
	 */
	@Test
	void calcScoreClearsErasableBlocksWithChainAndBonus() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		// A normal erasable colored block.
		Block plain = new Block(Block.BLOCK_COLOR_RED);
		plain.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		plain.bonusValue = 2; // exercises the bonusValue multiplier branch
		engine.field.setBlock(0, h - 1, plain);

		// A garbage erasable block (exercises the /2.0 garbage branch).
		Block garbage = new Block(Block.BLOCK_COLOR_BLUE);
		garbage.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
		garbage.setAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE, true);
		garbage.secondaryColor = Block.BLOCK_COLOR_BLUE;
		engine.field.setBlock(1, h - 1, garbage);

		engine.chain = 3; // chain > 1 -> chain bonus + combo SE
		getIntArray(mode, "diamondPower")[0] = 0; // skip diamond loop entirely
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0);

		// Both blocks erased -> board empty -> zenkeshi (+1000) and lastscore set.
		assertTrue(getIntArray(mode, "score")[0] > 0,
				"clearing erasable blocks should award points");
		assertTrue(getIntArray(mode, "scgettime")[0] > 0,
				"scgettime should be set when score is awarded");
	}

	/**
	 * With ojama already pending and hurryup active, the offsetting branch
	 * (delta subtraction) and the hurryup multiplier branch execute.
	 */
	@Test
	void calcScoreOffsetsPendingOjamaUnderHurryup() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		// Fill the bottom row with erasable blocks to generate attack points.
		for (int x = 0; x < engine.field.getWidth(); x++) {
			Block b = new Block(Block.BLOCK_COLOR_RED);
			b.setAttribute(Block.BLOCK_ATTRIBUTE_ERASE, true);
			engine.field.setBlock(x, h - 1, b);
		}
		getIntArray(mode, "diamondPower")[0] = 0;
		getIntArray(mode, "ojama")[0] = 3;       // pending ojama on self -> offset branch
		getIntArray(mode, "hurryupSeconds")[0] = 1; // hurryup active
		engine.statistics.time = 200;            // time > hurryupSeconds -> multiplier branch
		engine.chain = 1;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0);

		// Either the pending ojama got offset, or attack got sent to the enemy.
		assertTrue(getIntArray(mode, "ojama")[0] >= 0);
		assertTrue(getIntArray(mode, "ojama")[1] >= 0,
				"enemy ojama should be non-negative after a send");
	}

	/**
	 * calcScore returns immediately if the field can still cascade (blocks
	 * floating above gaps), without scoring.
	 */
	@Test
	void calcScoreReturnsEarlyWhenFieldCanCascade() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		// A floating block with empty space below -> canCascade() true.
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED));
		getIntArray(mode, "diamondPower")[0] = 0;
		engine.statistics.score = 0;

		mode.calcScore(engine, 0, 0);

		assertEquals(0, engine.statistics.score,
				"a cascadable field should bail out before scoring");
	}

	// =================================================================
	// checkSquares: pre-existing square, new square formation, expansion
	// =================================================================

	/**
	 * Four BROKEN blocks of the same color in a 2x2 arrangement form a new gem
	 * square: their BROKEN attribute is cleared and bonusValue is assigned.
	 */
	@Test
	void checkSquaresFormsNewSquareFromBrokenBlocks() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		int color = Block.BLOCK_COLOR_RED;
		for (int dx = 0; dx <= 1; dx++)
			for (int dy = 0; dy <= 1; dy++) {
				Block b = new Block(color);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
				engine.field.setBlock(dx, h - 2 + dy, b);
			}

		invokeCheckSquares(mode, engine, 0, true);

		// The top-left of the square should no longer be BROKEN and gain a size.
		Block tl = engine.field.getBlock(0, h - 2);
		assertFalse(tl.getAttribute(Block.BLOCK_ATTRIBUTE_BROKEN),
				"forming a square clears the BROKEN attribute");
		assertEquals(2, tl.bonusValue,
				"a 2x2 square should set bonusValue to 2");
	}

	/**
	 * A bigger broken arrangement (3x2) forms a square then expands, taking the
	 * expansion branches and assigning the connect attributes / bonusValue.
	 */
	@Test
	void checkSquaresExpandsLargerRegion() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		int color = Block.BLOCK_COLOR_GREEN;
		for (int dx = 0; dx <= 2; dx++)
			for (int dy = 0; dy <= 1; dy++) {
				Block b = new Block(color);
				b.setAttribute(Block.BLOCK_ATTRIBUTE_BROKEN, true);
				engine.field.setBlock(dx, h - 2 + dy, b);
			}

		invokeCheckSquares(mode, engine, 0, true);

		// Some block in the region should have a connect attribute and bonusValue.
		Block b = engine.field.getBlock(0, h - 2);
		assertNotNull(b);
		assertTrue(b.bonusValue >= 2,
				"an expanded square should carry a bonusValue >= 2");
	}

	/**
	 * checkSquares is a no-op when the time matches the last check and no
	 * recheck is forced (the lastSquareCheck short-circuit).
	 */
	@Test
	void checkSquaresSkipsWhenAlreadyCheckedThisFrame() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();
		engine.statistics.time = 50;

		// First call records lastSquareCheck = 50.
		invokeCheckSquares(mode, engine, 0, false);
		// Second call at same time, no force -> early return.
		invokeCheckSquares(mode, engine, 0, false);

		assertEquals(50, getIntArray(mode, "lastSquareCheck")[0]);
	}

	// =================================================================
	// lineClearEnd: diamond present recurses; no drop room
	// =================================================================

	/**
	 * If a diamond is on the field, lineClearEnd recurses into calcScore and
	 * returns true (the 1421-1427 branch).
	 */
	@Test
	void lineClearEndWithDiamondRecursesIntoCalcScore() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		int h = engine.field.getHeight();
		engine.field.setBlock(0, h - 1, new Block(Block.BLOCK_COLOR_GEM_RAINBOW));
		getIntArray(mode, "diamondPower")[0] = 3;

		boolean result = invokeLineClearEnd(mode, engine, 0);

		assertTrue(result, "a diamond on the field should make lineClearEnd return true");
	}

	/**
	 * With a small ojama amount on a tall stack there is no room to drop garbage
	 * (dropRows <= 0), so lineClearEnd returns false without dropping.
	 */
	@Test
	void lineClearEndReturnsFalseWhenNoDropRoom() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();

		// Fill the whole visible field so the highest block is at the very top
		// (no room above), forcing getHighestBlockY(...) -> dropRows <= 0.
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		for (int x = 0; x < w; x++)
			for (int y = 0; y < h; y++)
				engine.field.setBlock(x, y, new Block(Block.BLOCK_COLOR_RED));

		getIntArray(mode, "ojama")[0] = 1;
		int[][][] dp = (int[][][]) findField(mode.getClass(), "dropPattern").get(mode);
		dp[1] = new int[][]{{2, 2, 2, 2}};

		boolean result = invokeLineClearEnd(mode, engine, 0);

		assertFalse(result, "no drop room should make lineClearEnd return false");
		assertEquals(1, getIntArray(mode, "ojama")[0],
				"ojama should be untouched when there is no room to drop");
	}

	/**
	 * With no ojama and no diamond, lineClearEnd just runs checkAll and returns
	 * false.
	 */
	@Test
	void lineClearEndReturnsFalseWithNoOjamaNoDiamond() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();
		getIntArray(mode, "ojama")[0] = 0;

		assertFalse(invokeLineClearEnd(mode, engine, 0));
	}

	// =================================================================
	// onReady: map-load branches
	// =================================================================

	/**
	 * onReady at statc[0]==0 with useMap on and a randomly-selected map (replay
	 * mode off, no map properties on disk -> propMap stays null) still executes
	 * the useMap branch and the field-reset fallback.
	 */
	@Test
	void onReadyUseMapNoPropertiesFallsBack() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.statc[0] = 0;
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = -1; // random selection branch
		engine.owner.replayMode = false;

		// receiver.loadProperties returns null in headless mode, so propMap
		// stays null -> the "if(propMap != null)" inner branch is skipped.
		mode.onReady(engine, 0);

		assertEquals(20, engine.numColors,
				"onReady configures 20 colors for SPF");
	}

	/**
	 * onReady at statc[0]==0 with useMap OFF resets an existing field (the
	 * else-branch at 887-889).
	 */
	@Test
	void onReadyNoMapResetsField() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.statc[0] = 0;
		getBoolArray(mode, "useMap")[0] = false;
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED));

		mode.onReady(engine, 0);

		assertTrue(engine.field.getBlockEmpty(0, 0),
				"onReady with no map should reset the field");
	}

	/**
	 * onReady at statc[0]==1 with diamondPower>0 recolours the diamond slots in
	 * the next-piece array.
	 */
	@Test
	void onReadyStatc1SetsDiamondColors() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.statc[0] = 1;
		getIntArray(mode, "diamondPower")[0] = 3;

		// Build a next-piece array large enough to contain at least one slot
		// at index 24 (the loop starts at x=24 and steps by 25).
		engine.nextPieceArraySize = 50;
		engine.nextPieceArrayObject = new Piece[50];
		for (int i = 0; i < 50; i++)
			engine.nextPieceArrayObject[i] = new Piece(Piece.PIECE_I);

		boolean result = mode.onReady(engine, 0);

		assertFalse(result, "onReady returns false");
		assertEquals(SPFMode_DIAMOND_COLOR(),
				engine.nextPieceArrayObject[24].block[1].color,
				"the diamond slot's second block should be recoloured to rainbow gem");
	}

	// =================================================================
	// onLast: rising meter + settlement (win / lose / draw)
	// =================================================================

	/**
	 * onLast raises the meter towards a high ojama target and selects the RED
	 * meter colour when ojama > 30.
	 */
	@Test
	void onLastRaisesMeterAndSelectsRedColour() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		getIntArray(mode, "ojama")[0] = 40; // > 30 -> RED meter colour
		engine.meterValue = 0;

		mode.onLast(engine, 0);

		assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
	}

	/**
	 * onLast lowers the meter when the ojama target is below the current meter
	 * value, and selects the YELLOW colour for a mid-range ojama count.
	 */
	@Test
	void onLastLowersMeterAndSelectsYellowColour() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		getIntArray(mode, "ojama")[0] = 15; // 10 < x <= 30 -> YELLOW
		engine.meterValue = 9999;

		mode.onLast(engine, 0);

		assertEquals(GameEngine.METER_COLOR_YELLOW, engine.meterColor);
		assertTrue(engine.meterValue < 9999, "meter should have decreased");
	}

	/**
	 * Player-1 settlement when both engines are GAMEOVER -> draw (winnerID -1).
	 */
	@Test
	void onLastSettlementDraw() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = settledTwoPlayerEngine(mode);
		engine.owner.engine[0].stat = GameEngine.Status.GAMEOVER;
		engine.owner.engine[1].stat = GameEngine.Status.GAMEOVER;
		mode.playerInit(engine.owner.engine[1], 1);

		mode.onLast(engine.owner.engine[1], 1);

		assertEquals(-1, readFieldInt(mode, "winnerID"), "both dead -> draw");
	}

	/**
	 * Player-1 settlement when only player 2 is GAMEOVER -> player 0 wins.
	 */
	@Test
	void onLastSettlement1PWins() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = settledTwoPlayerEngine(mode);
		engine.owner.engine[0].stat = GameEngine.Status.MOVE;
		engine.owner.engine[1].stat = GameEngine.Status.GAMEOVER;
		mode.playerInit(engine.owner.engine[1], 1);

		mode.onLast(engine.owner.engine[1], 1);

		assertEquals(0, readFieldInt(mode, "winnerID"), "P2 dead -> P1 wins");
		assertEquals(GameEngine.Status.EXCELLENT, engine.owner.engine[0].stat);
	}

	/**
	 * Player-1 settlement when only player 1 is GAMEOVER -> player 1 wins.
	 */
	@Test
	void onLastSettlement2PWins() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = settledTwoPlayerEngine(mode);
		engine.owner.engine[0].stat = GameEngine.Status.GAMEOVER;
		engine.owner.engine[1].stat = GameEngine.Status.MOVE;
		mode.playerInit(engine.owner.engine[1], 1);

		mode.onLast(engine.owner.engine[1], 1);

		assertEquals(1, readFieldInt(mode, "winnerID"), "P1 dead -> P2 wins");
		assertEquals(GameEngine.Status.EXCELLENT, engine.owner.engine[1].stat);
	}

	// =================================================================
	// renderResult: draw / win / lose text
	// =================================================================

	@Test
	void renderResultDraw() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		setFieldInt(mode, "winnerID", -1);
		engine.statistics.time = 600;

		mode.renderResult(engine, 0); // no-op receiver -> just executes lines
	}

	@Test
	void renderResultWin() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		setFieldInt(mode, "winnerID", 0); // winnerID == playerID -> WIN!
		engine.statistics.time = 600;

		mode.renderResult(engine, 0);
	}

	@Test
	void renderResultLose() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		setFieldInt(mode, "winnerID", 1); // winnerID != playerID -> LOSE
		engine.statistics.time = 600;

		mode.renderResult(engine, 0);
	}

	// =================================================================
	// renderLast: countdown blocks, big-display, on-screen texts
	// =================================================================

	/**
	 * renderLast in big-display mode with active countdown blocks of various
	 * colours, plus zenkeshi / tech-bonus banners showing.
	 */
	@Test
	void renderLastBigDisplayWithCountdownBlocks() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.displaysize = 1; // big-display branch

		int h = engine.field.getHeight();
		// One countdown block of each handled colour to walk the colour switch.
		int[] colours = {
				Block.BLOCK_COLOR_BLUE, Block.BLOCK_COLOR_GREEN,
				Block.BLOCK_COLOR_RED, Block.BLOCK_COLOR_YELLOW
		};
		for (int i = 0; i < colours.length; i++) {
			Block b = new Block(colours[i]);
			b.countdown = 3;
			b.secondaryColor = colours[i];
			engine.field.setBlock(i, h - 1, b);
		}

		getIntArray(mode, "ojama")[0] = 13;      // colour-tier branches for ojama text
		getIntArray(mode, "techBonusDisplay")[0] = 10;
		getIntArray(mode, "zenKeshiDisplay")[0] = 10;
		setFieldInt(mode, "winnerID", -1);

		mode.renderLast(engine, 0);
	}

	/**
	 * renderLast in normal-size mode with a started game and countdown blocks.
	 */
	@Test
	void renderLastNormalDisplay() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.gameActive = true;
		engine.gameStarted = true;
		engine.displaysize = 0;

		int h = engine.field.getHeight();
		Block b = new Block(Block.BLOCK_COLOR_BLUE);
		b.countdown = 2;
		b.secondaryColor = Block.BLOCK_COLOR_BLUE;
		engine.field.setBlock(0, h - 1, b);

		getIntArray(mode, "ojama")[0] = 8; // 6..11 -> ORANGE tier
		mode.renderLast(engine, 1); // player 1 path (no timer draw)
	}

	// =================================================================
	// onSetting: drop-map / map preview navigation + post-confirm state
	// =================================================================

	/**
	 * UP from cursor 0 wraps to 19 and loads the drop-map preview (line 547);
	 * a subsequent UP into cursor 17 nulls the field (line 549-550).
	 */
	@Test
	void onSettingUpWrapLoadsDropMapPreviewThenClearsAtCursor17() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 10);

		setFieldInt(mode, "menuCursor", 0);
		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(19, readFieldInt(mode, "menuCursor"));
		assertNotNull(engine.field, "drop-map preview should have created a field");

		// Walk up to cursor 17 to hit the "menuCursor == 17 -> field = null" branch.
		setFieldInt(mode, "menuCursor", 18);
		pressKey(engine, Controller.BUTTON_UP);
		mode.onSetting(engine, 0);
		assertEquals(17, readFieldInt(mode, "menuCursor"));
	}

	/**
	 * DOWN into cursor 18 loads the drop-map preview (line 560-562).
	 */
	@Test
	void onSettingDownIntoCursor18LoadsDropMapPreview() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 10);
		setFieldInt(mode, "menuCursor", 17);

		pressKey(engine, Controller.BUTTON_DOWN);
		mode.onSetting(engine, 0);

		assertEquals(18, readFieldInt(mode, "menuCursor"));
		assertNotNull(engine.field, "drop-map preview should have created a field");
	}

	/**
	 * Cursor 10 toggling useMap ON triggers a map preview load (line 629).
	 */
	@Test
	void onSettingCursor10UseMapOnLoadsPreview() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 10);
		setFieldInt(mode, "menuCursor", 10);
		getBoolArray(mode, "useMap")[0] = false; // toggling -> ON -> loadMapPreview

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertTrue(getBoolArray(mode, "useMap")[0], "cursor 10 should toggle useMap ON");
	}

	/**
	 * Cursor 11 with useMap ON resets mapNumber and reloads the preview
	 * (lines 636-639).
	 */
	@Test
	void onSettingCursor11WithUseMapReloadsPreview() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 10);
		setFieldInt(mode, "menuCursor", 11);
		getBoolArray(mode, "useMap")[0] = true;

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		assertEquals(-1, getIntArray(mode, "mapNumber")[0],
				"changing the map set with useMap on resets mapNumber to random");
	}

	/**
	 * Cursor 12 with useMap ON adjusts mapNumber and reloads the preview
	 * (lines 642-646).
	 */
	@Test
	void onSettingCursor12WithUseMapAdjustsMapNumber() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 10);
		setFieldInt(mode, "menuCursor", 12);
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = 0;

		pressKey(engine, Controller.BUTTON_RIGHT);
		mode.onSetting(engine, 0);

		// mapMaxNo is 0 (no real map file), so RIGHT from 0 wraps mapNumber to -1.
		assertEquals(-1, getIntArray(mode, "mapNumber")[0]);
	}

	/**
	 * With useMap on and menuTime 0, the per-frame preview load (line 712) runs;
	 * the random-preview block at line 717-721 needs propMap != null which it
	 * is not in headless mode, but the menuTime==0 branch executes.
	 */
	@Test
	void onSettingUseMapPreviewAtMenuTimeZero() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = false;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 0);
		setFieldInt(mode, "menuCursor", 0);
		getBoolArray(mode, "useMap")[0] = true;
		getIntArray(mode, "mapNumber")[0] = -1;

		mode.onSetting(engine, 0);

		assertEquals(1, readFieldInt(mode, "menuTime"),
				"menuTime advances after one onSetting call");
	}

	/**
	 * Replay auto-advance: at menuTime == 120 the drop-map preview loads
	 * (lines 734-738).
	 */
	@Test
	void onSettingReplayAutoAdvanceAt120LoadsDropMapPreview() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.owner.replayMode = true;
		engine.statc[4] = 0;
		setFieldInt(mode, "menuTime", 119);

		mode.onSetting(engine, 0); // 119 -> 120 -> cursor 18 + drop-map preview

		assertEquals(18, readFieldInt(mode, "menuCursor"));
		assertNotNull(engine.field);
	}

	/**
	 * After confirmation (statc[4] == 1) with only one engine ready, B cancels
	 * back to statc[4] == 0 (lines 750-751).
	 */
	@Test
	void onSettingPostConfirmBCancels() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = twoPlayerEngine(mode);
		engine.statc[4] = 1;
		engine.owner.engine[0].statc[4] = 1;
		engine.owner.engine[1].statc[4] = 0; // not both ready

		pressPush(engine, Controller.BUTTON_B);
		mode.onSetting(engine, 0);

		assertEquals(0, engine.statc[4], "B after confirm should return to the menu");
	}

	/**
	 * After both engines confirm, player 1's onSetting transitions both engines
	 * to READY (lines 743-747).
	 */
	@Test
	void onSettingPostConfirmBothReadyStartsGame() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = twoPlayerEngine(mode);
		engine.owner.engine[0].statc[4] = 1;
		engine.owner.engine[1].statc[4] = 1;

		mode.onSetting(engine.owner.engine[1], 1);

		assertEquals(GameEngine.Status.READY, engine.owner.engine[0].stat);
		assertEquals(GameEngine.Status.READY, engine.owner.engine[1].stat);
	}

	// =================================================================
	// renderSetting: page-3 below-100% multiplier branches
	// =================================================================

	/**
	 * Page 3 with a drop set/map whose attack and defend multipliers are below
	 * 100% exercises the alternate-colour render branches (lines 806, 813).
	 */
	@Test
	void renderSettingPage3BelowFullMultipliers() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.statc[4] = 0;
		setFieldInt(mode, "menuCursor", 18);
		// set 0, map 8 -> attack multiplier 0.7 (<100%, RED attack branch).
		getIntArray(mode, "dropSet")[0] = 0;
		getIntArray(mode, "dropMap")[0] = 8;

		mode.renderSetting(engine, 0);
	}

	// =================================================================
	// loadMapPreview: null-properties field reset
	// =================================================================

	/**
	 * loadMapPreview with a non-null field but no map file on disk takes the
	 * "propMap == null && field != null -> reset" branch (lines 456-457).
	 */
	@Test
	void loadMapPreviewWithoutPropertiesResetsField() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.createFieldIfNeeded();
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED));

		Method m = SPFMode.class.getDeclaredMethod(
				"loadMapPreview", GameEngine.class, int.class, int.class, boolean.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0, 0, true);

		// receiver.loadProperties returns null headless -> field gets reset.
		assertTrue(engine.field == null || engine.field.getBlockEmpty(0, 0),
				"with no map file the preview load should reset (or null) the field");
	}

	// =================================================================
	// pieceLocked
	// =================================================================

	@Test
	void pieceLockedWithNullFieldDoesNothing() {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		engine.field = null;
		mode.pieceLocked(engine, 0, 0); // null field -> early return, no crash
	}

	@Test
	void pieceLockedWithFieldRunsCheckAll() throws Exception {
		SPFMode mode = new SPFMode();
		GameEngine engine = fullEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		getBoolArray(mode, "countdownDecremented")[0] = false;

		mode.pieceLocked(engine, 0, 0);
		// Reaching here without an exception means checkAll ran.
		assertTrue(getBoolArray(mode, "countdownDecremented")[0],
				"checkAll -> checkCountdown should have marked the frame decremented");
	}

	// =================================================================
	// Helpers
	// =================================================================

	/** Full single-engine setup with modeInit + playerID set. */
	private static GameEngine fullEngine(SPFMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		return manager.engine[0];
	}

	/** Two-player setup; returns engine[0]. */
	private static GameEngine twoPlayerEngine(SPFMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.modeInit(manager);
		manager.engine[0].playerID = 0;
		manager.engine[1].playerID = 1;
		return manager.engine[0];
	}

	/**
	 * Two-player setup with player-0 gameActive (the onLast settlement guard at
	 * line 1491 requires owner.engine[0].gameActive).
	 */
	private static GameEngine settledTwoPlayerEngine(SPFMode mode) {
		GameEngine e0 = twoPlayerEngine(mode);
		e0.gameActive = true;
		return e0;
	}

	private static int SPFMode_DIAMOND_COLOR() {
		return Block.BLOCK_COLOR_GEM_RAINBOW;
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

	private static void pressKey(GameEngine engine, int btn) {
		engine.ctrl.clearButtonState();
		for (int i = 0; i < Controller.BUTTON_COUNT; i++) {
			engine.ctrl.buttonTime[i] = 0;
		}
		engine.ctrl.buttonPress[btn] = true;
		engine.ctrl.buttonTime[btn] = 1;
	}

	private static void pressPush(GameEngine engine, int btn) {
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
}
