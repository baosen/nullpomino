package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link Avalanche1PDummyMode}. Covers registry
 * surface, playerInit, calcScore scoring with chain multiplier and level
 * progression, setSpeed, addBonus (zenkeshi/maxchain), onLast, onGameOver,
 * onReady/readyInit, and startGame.
 */
class Avalanche1PDummyModeGameLogicTest {

	/** Minimal concrete subclass of the abstract Avalanche1PDummyMode. */
	private static class ConcreteAvalanche1PDummy extends Avalanche1PDummyMode {
	}

	// -----------------------------------------------------------------------
	// Registry surface
	// -----------------------------------------------------------------------

	@Test
	void getNameReturnsAvalancheDummy() {
		assertEquals("AVALANCHE DUMMY", new ConcreteAvalanche1PDummy().getName());
	}

	@Test
	void getPlayersReturnsOne() {
		assertEquals(1, new ConcreteAvalanche1PDummy().getPlayers());
	}

	@Test
	void getGameStyleIsAvalanche() {
		assertEquals(GameEngine.GAMESTYLE_AVALANCHE, new ConcreteAvalanche1PDummy().getGameStyle());
	}

	// -----------------------------------------------------------------------
	// Constructor defaults
	// -----------------------------------------------------------------------

	@Test
	void constructorSetsDefaults() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();

		assertEquals(15, readInt(mode, "blocksPerLevel"));
		assertEquals(99, readInt(mode, "maxLevel"));
		assertEquals(120, readInt(mode, "ojamaRate"));
	}

	// -----------------------------------------------------------------------
	// playerInit
	// -----------------------------------------------------------------------

	@Test
	void playerInitResetsScoreStateAndEngineSettings() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "lastscore"));
		assertEquals(0, readInt(mode, "lastmultiplier"));
		assertEquals(0, readInt(mode, "scgettime"));
		assertEquals(0, readInt(mode, "speedIndex"));
		assertEquals(false, readBoolean(mode, "zenKeshi"));
		assertEquals(0, readInt(mode, "garbageSent"));
		assertEquals(0, readInt(mode, "garbageAdd"));
		assertEquals(0, readInt(mode, "blocksCleared"));
		assertEquals(5, readInt(mode, "level"));
		assertEquals(15, readInt(mode, "toNextLevel"));

		assertEquals(GameEngine.FRAME_COLOR_PURPLE, engine.framecolor);
		assertEquals(GameEngine.ClearType.COLOR, engine.clearMode);
		assertTrue(engine.garbageColorClear);
		assertTrue(engine.ignoreHidden);
		assertFalse(engine.connectBlocks);
		assertTrue(engine.dominoQuickTurn);
	}

	// -----------------------------------------------------------------------
	// calcScore
	// -----------------------------------------------------------------------

	@Test
	void calcScoreWithAvalancheUpdatesScoreAndLevel() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.chain = 0;

		mode.calcScore(engine, 0, 3);

		// avalanche > 0 -> score is computed
		// calcPts: 3*10 = 30
		// multiplier: colorClearExtraCount(=0) + chainMultiplier(chain=0 -> 0) = 0 -> clamped to 1
		// score = 30 * 1 = 30
		assertEquals(30, engine.statistics.score);
		assertEquals(30, readInt(mode, "lastscore"));
		assertEquals(1, readInt(mode, "lastmultiplier"));
		assertTrue(readInt(mode, "scgettime") > 0);

		// blocksCleared += 3
		assertEquals(3, readInt(mode, "blocksCleared"));
	}

	@Test
	void calcScoreWithEmptyFieldTriggersZenkeshi() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.chain = 0;

		mode.calcScore(engine, 0, 4);

		// Field starts empty -> zenKeshi becomes true, zenKeshiCount increments
		assertTrue(readBoolean(mode, "zenKeshi"));
		assertEquals(1, readInt(mode, "zenKeshiCount"));
	}

	@Test
	void calcScoreIncrementsLevelWhenToNextLevelReached() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.chain = 0;

		setInt(mode, "level", 5);
		setInt(mode, "toNextLevel", 2);

		mode.calcScore(engine, 0, 3); // clears 3 blocks

		assertEquals(6, readInt(mode, "level"));
		assertEquals(15, readInt(mode, "toNextLevel")); // reset to blocksPerLevel
	}

	@Test
	void calcScoreWithNoAvalancheDoesNothing() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		int scoreBefore = engine.statistics.score;
		mode.calcScore(engine, 0, 0);

		assertEquals(scoreBefore, engine.statistics.score);
	}

	// -----------------------------------------------------------------------
	// calcChainMultiplier
	// -----------------------------------------------------------------------

	@Test
	void calcChainMultiplierReturnsCorrectValues() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();

		Method m = Avalanche1PDummyMode.class.getDeclaredMethod("calcChainMultiplier", int.class);
		m.setAccessible(true);

		assertEquals(0, m.invoke(mode, 1));
		assertEquals(8, m.invoke(mode, 2));
		assertEquals(16, m.invoke(mode, 3));
		assertEquals(32, m.invoke(mode, 4));
		assertEquals(64, m.invoke(mode, 5));
	}

	// -----------------------------------------------------------------------
	// calcPts
	// -----------------------------------------------------------------------

	@Test
	void calcPtsReturnsAvalancheTimes10() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();

		Method m = Avalanche1PDummyMode.class.getDeclaredMethod("calcPts", int.class);
		m.setAccessible(true);

		assertEquals(0, m.invoke(mode, 0));
		assertEquals(10, m.invoke(mode, 1));
		assertEquals(50, m.invoke(mode, 5));
	}

	// -----------------------------------------------------------------------
	// calcOjama
	// -----------------------------------------------------------------------

	@Test
	void calcOjamaUsesRate() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		setInt(mode, "ojamaRate", 120);

		Method m = Avalanche1PDummyMode.class.getDeclaredMethod(
				"calcOjama", int.class, int.class, int.class, int.class);
		m.setAccessible(true);

		// score=240, rate=120 -> ceil(240/120) = 2
		assertEquals(2, m.invoke(mode, 240, 0, 0, 0));
		// score=100, rate=120 -> ceil(100/120) = 1
		assertEquals(1, m.invoke(mode, 100, 0, 0, 0));
	}

	// -----------------------------------------------------------------------
	// setSpeed
	// -----------------------------------------------------------------------

	@Test
	void setSpeedAtLowLevelUsesFormula() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "level", 10);
		mode.setSpeed(engine);

		assertEquals(1, engine.speed.gravity);
		// denominator = max(43 - 10 - (10%10)*2, 2) = max(33, 2) = 33
		assertEquals(33, engine.speed.denominator);
	}

	@Test
	void setSpeedAtHighLevelUsesTable() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "level", 80);
		mode.setSpeed(engine);

		assertEquals(60, engine.speed.denominator);
		// level 80 -> speedIndex advances past tableSpeedChangeLevel[0]=80 to 1
		// tableSpeedValue[1] = 45
		assertEquals(45, engine.speed.gravity);
	}

	@Test
	void setSpeedAtVeryHighLevelSets20g() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "level", 99);
		setInt(mode, "speedIndex", 3); // past all table entries
		mode.setSpeed(engine);

		assertEquals(-1, engine.speed.gravity);
	}

	// -----------------------------------------------------------------------
	// addBonus (onGameOver)
	// -----------------------------------------------------------------------

	@Test
	void addBonusComputesZenkeshiAndMaxChainBonuses() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "zenKeshiCount", 5);
		engine.statistics.maxChain = 3;
		engine.statistics.score = 1000;

		invokeAddBonus(mode, engine, 0);

		// numColors defaults to 0 (since not set in playerInit), so else branch:
		// zenKeshiBonus = zenKeshiCount*(zenKeshiCount+3)*250 = 5*8*250 = 10000
		// maxChainBonus = maxChain*maxChain*2000 = 9*2000 = 18000
		// total bonus = 10000 + 18000 = 28000
		// final score = 1000 + 28000 = 29000
		assertEquals(1000, readInt(mode, "scoreBeforeBonus"));
		assertEquals(10000, readInt(mode, "zenKeshiBonus"));
		assertEquals(18000, readInt(mode, "maxChainBonus"));
		assertEquals(29000, engine.statistics.score);
	}

	@Test
	void addBonusWith4ColorsUsesDifferentFormula() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "numColors", 4);
		setInt(mode, "zenKeshiCount", 5);
		engine.statistics.maxChain = 3;

		invokeAddBonus(mode, engine, 0);

		// zenKeshi bonus = 5*6*500 = 15000
		assertEquals(15000, readInt(mode, "zenKeshiBonus"));
	}

	@Test
	void addBonusWith3ColorsUsesAnotherFormula() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "numColors", 3);
		setInt(mode, "zenKeshiCount", 5);
		engine.statistics.maxChain = 3;

		invokeAddBonus(mode, engine, 0);

		// zenKeshi bonus = 5*8*250 = 10000
		assertEquals(10000, readInt(mode, "zenKeshiBonus"));
	}

	// -----------------------------------------------------------------------
	// onLast
	// -----------------------------------------------------------------------

	@Test
	void onLastDecrementsScgettimeAndChainDisplay() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "scgettime", 10);
		setInt(mode, "chainDisplay", 20);

		mode.onLast(engine, 0);

		assertEquals(9, readInt(mode, "scgettime"));
		assertEquals(19, readInt(mode, "chainDisplay"));
	}

	// -----------------------------------------------------------------------
	// onGameOver
	// -----------------------------------------------------------------------

	@Test
	void onGameOverCallsAddBonusOnce() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		engine.statistics.score = 500;
		setInt(mode, "zenKeshiCount", 2);
		engine.statistics.maxChain = 1;

		mode.onGameOver(engine, 0);

		// Bonus should have been applied
		assertTrue(engine.statistics.score > 500);
	}

	// -----------------------------------------------------------------------
	// onReady / readyInit
	// -----------------------------------------------------------------------

	@Test
	void readyInitSetsNumColorsAndOutline() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		setInt(mode, "numColors", 5);
		setInt(mode, "outlinetype", 1);

		invokeReadyInit(mode, engine, 0);

		assertEquals(5, engine.numColors);
		assertEquals(GameEngine.BLOCK_OUTLINE_SAMECOLOR, engine.blockOutlineType);
	}

	@Test
	void readyInitSetsLevelBasedOnNumColors() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "numColors", 3);
		invokeReadyInit(mode, engine, 0);

		assertEquals(1, readInt(mode, "level"));

		setInt(mode, "numColors", 4);
		invokeReadyInit(mode, engine, 0);

		assertEquals(5, readInt(mode, "level"));

		setInt(mode, "numColors", 5);
		invokeReadyInit(mode, engine, 0);

		assertEquals(10, readInt(mode, "level"));
	}

	// -----------------------------------------------------------------------
	// startGame
	// -----------------------------------------------------------------------

	@Test
	void startGameSetsEngineParameters() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertEquals(30, engine.speed.are);
		assertEquals(30, engine.speed.areLine);
		assertEquals(10, engine.speed.das);
		assertEquals(60, engine.speed.lockDelay);
	}

	// -----------------------------------------------------------------------
	// afterSoftDropFall / afterHardDropFall
	// -----------------------------------------------------------------------

	@Test
	void afterHardDropFallIncrementsScore() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		engine.statistics.score = 0;
		mode.afterHardDropFall(engine, 0, 7);

		assertEquals(7, engine.statistics.score);
	}

	@Test
	void afterSoftDropFallIncrementsScore() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		engine.statistics.score = 0;
		mode.afterSoftDropFall(engine, 0, 3);

		assertEquals(3, engine.statistics.score);
	}

	// -----------------------------------------------------------------------
	// lineClearEnd
	// -----------------------------------------------------------------------

	@Test
	void lineClearEndSendsGarbageAndResets() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "garbageAdd", 50);

		mode.lineClearEnd(engine, 0);

		assertEquals(50, readInt(mode, "garbageSent"));
		assertEquals(0, readInt(mode, "garbageAdd"));
	}

	// -----------------------------------------------------------------------
	// onARE
	// -----------------------------------------------------------------------

	@Test
	void onAREAdjustsDasCount() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		engine.dasCount = 5;
		mode.onARE(engine, 0);

		// dasCount < DAS(10), so dasCount = DAS = 10
		assertEquals(10, engine.dasCount);
	}

	@Test
	void onAREDoesNotChangeDasWhenAtMax() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		engine.dasCount = 15; // > DAS(10)
		mode.onARE(engine, 0);

		assertEquals(15, engine.dasCount);
	}

	// -----------------------------------------------------------------------
	// onClear hook
	// -----------------------------------------------------------------------

	@Test
	void onClearSetsChainDisplay() throws Exception {
		ConcreteAvalanche1PDummy mode = new ConcreteAvalanche1PDummy();
		GameEngine engine = freshEngine(mode);

		setInt(mode, "chainDisplay", 0);

		Method m = Avalanche1PDummyMode.class.getDeclaredMethod(
				"onClear", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, 0);

		assertEquals(60, readInt(mode, "chainDisplay"));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(Avalanche1PDummyMode mode) {
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

	private static void invokeAddBonus(Avalanche1PDummyMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = Avalanche1PDummyMode.class.getDeclaredMethod("addBonus", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}

	private static void invokeReadyInit(Avalanche1PDummyMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = Avalanche1PDummyMode.class.getDeclaredMethod("readyInit", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}
}
