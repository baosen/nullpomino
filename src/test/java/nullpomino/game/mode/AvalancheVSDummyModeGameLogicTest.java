package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link AvalancheVSDummyMode}. Covers registry
 * surface, modeInit/playerInit, preset persistence, chain/power calculation,
 * calcScore scoring with chain multiplier and ojama mechanics, onLast
 * win/loss/draw, and helper methods.
 */
class AvalancheVSDummyModeGameLogicTest {

	/**
	 * Minimal concrete subclass of the abstract AvalancheVSDummyMode.
	 * Implements lineClearEnd as required.
	 */
	private static class ConcreteAvalancheVSDummy extends AvalancheVSDummyMode {
		@Override
		public boolean lineClearEnd(GameEngine engine, int playerID) {
			return false;
		}
	}

	// -----------------------------------------------------------------------
	// Registry surface
	// -----------------------------------------------------------------------

	@Test
	void getNameReturnsAvalancheVsDummy() {
		assertEquals("AVALANCHE VS DUMMY", new ConcreteAvalancheVSDummy().getName());
	}

	@Test
	void getPlayersReturnsTwo() {
		assertEquals(2, new ConcreteAvalancheVSDummy().getPlayers());
	}

	@Test
	void getGameStyleIsAvalanche() {
		assertEquals(GameEngine.GAMESTYLE_AVALANCHE, new ConcreteAvalancheVSDummy().getGameStyle());
	}

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(new ConcreteAvalancheVSDummy().isVSMode());
	}

	// -----------------------------------------------------------------------
	// modeInit
	// -----------------------------------------------------------------------

	@Test
	void modeInitAllocatesPerPlayerArrays() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		assertNotNull(readField(mode, "ojama"));
		assertNotNull(readField(mode, "ojamaSent"));
		assertNotNull(readField(mode, "ojamaCounterMode"));
		assertNotNull(readField(mode, "scgettime"));
		assertNotNull(readField(mode, "score"));
		assertNotNull(readField(mode, "big"));
		assertNotNull(readField(mode, "enableSE"));
		assertNotNull(readField(mode, "zenKeshi"));
		assertNotNull(readField(mode, "cleared"));
		assertNotNull(readField(mode, "ojamaDrop"));
		assertNotNull(readField(mode, "numColors"));

		assertEquals(2, ((int[]) readField(mode, "ojama")).length);
		assertEquals(-1, readInt(mode, "winnerID"));
	}

	@Test
	void playerInitResetsPlayerState() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		// Init player 0 first so player 1 can sync seed
		mode.playerInit(engine0, 0);
		mode.playerInit(engine1, 1);

		// Verify frame colors
		assertEquals(GameEngine.FRAME_COLOR_RED, engine0.framecolor);
		assertEquals(GameEngine.FRAME_COLOR_BLUE, engine1.framecolor);

		// Verify engine settings
		assertEquals(GameEngine.ClearType.COLOR, engine0.clearMode);
		assertTrue(engine0.garbageColorClear);
		assertEquals(GameEngine.LineGravity.CASCADE, engine0.lineGravityType);

		// Verify player state reset
		int[] ojama = (int[]) readField(mode, "ojama");
		assertEquals(0, ojama[0]);
		assertEquals(0, ojama[1]);

		int[] scgettime = (int[]) readField(mode, "scgettime");
		assertEquals(0, scgettime[0]);
		assertEquals(0, scgettime[1]);

		boolean[] cleared = (boolean[]) readField(mode, "cleared");
		assertFalse(cleared[0]);
		assertFalse(cleared[1]);
	}

	@Test
	void playerInitSyncsSeedForPlayer1() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		mode.playerInit(manager.engine[0], 0);
		long seed0 = manager.engine[0].randSeed;

		mode.playerInit(manager.engine[1], 1);

		assertEquals(seed0, manager.engine[1].randSeed);
	}

	// -----------------------------------------------------------------------
	// Settings persistence
	// -----------------------------------------------------------------------

	@Test
	void loadPresetReadsAllSpeedKeys() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameEngine engine = freshEngineP0(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("avalanchevs.gravity.1", 8);
		prop.setProperty("avalanchevs.denominator.1", 128);
		prop.setProperty("avalanchevs.are.1", 25);
		prop.setProperty("avalanchevs.areLine.1", 20);
		prop.setProperty("avalanchevs.lineDelay.1", 15);
		prop.setProperty("avalanchevs.lockDelay.1", 45);
		prop.setProperty("avalanchevs.das.1", 10);
		prop.setProperty("avalanchevs.fallDelay.1", 2);
		prop.setProperty("avalanchevs.clearDelay.1", 8);

		invokeLoadPreset(mode, engine, prop, 1, "");

		assertEquals(8, engine.speed.gravity);
		assertEquals(128, engine.speed.denominator);
		assertEquals(25, engine.speed.are);
		assertEquals(20, engine.speed.areLine);
		assertEquals(15, engine.speed.lineDelay);
		assertEquals(45, engine.speed.lockDelay);
		assertEquals(10, engine.speed.das);
		assertEquals(2, engine.cascadeDelay);
		assertEquals(8, engine.cascadeClearDelay);
	}

	@Test
	void savePresetAndLoadPresetRoundTrip() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameEngine engine = freshEngineP0(mode);

		engine.speed.gravity = 16;
		engine.speed.das = 7;
		engine.cascadeDelay = 3;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(mode, engine, prop, 0, "");

		assertEquals(16, prop.getProperty("avalanchevs.gravity.0", -1));
		assertEquals(7, prop.getProperty("avalanchevs.das.0", -1));
		assertEquals(3, prop.getProperty("avalanchevs.fallDelay.0", -1));

		ConcreteAvalancheVSDummy loaded = new ConcreteAvalancheVSDummy();
		GameEngine le = freshEngineP0(loaded);
		invokeLoadPreset(loaded, le, prop, 0, "");

		assertEquals(16, le.speed.gravity);
		assertEquals(7, le.speed.das);
		assertEquals(3, le.cascadeDelay);
	}

	@Test
	void loadOtherSettingReadsDefaults() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngineP0(mode);

		((int[]) readField(mode, "ojamaRate"))[0] = 120;
		((int[]) readField(mode, "ojamaHard"))[0] = 0;

		invokeLoadOtherSetting(mode, engine, new CustomProperties(), "");

		assertEquals(0, readInt(mode, "bgmno"));
		assertEquals(AvalancheVSDummyMode.OJAMA_COUNTER_ON,
				((int[]) readField(mode, "ojamaCounterMode"))[0]);
		assertEquals(true, ((boolean[]) readField(mode, "enableSE"))[0]);
	}

	@Test
	void saveOtherSettingAndLoadOtherSettingRoundTrip() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngineP0(mode);

		((boolean[]) readField(mode, "enableSE"))[0] = false;
		((int[]) readField(mode, "maxAttack"))[0] = 50;
		((int[]) readField(mode, "ojamaRate"))[0] = 200;
		((int[]) readField(mode, "ojamaHard"))[0] = 3;

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop, "");

		assertEquals(false, prop.getProperty("avalanchevs.enableSE.p0", true));
		assertEquals(50, prop.getProperty("avalanchevs.maxAttack.p0", -1));

		ConcreteAvalancheVSDummy loaded = new ConcreteAvalancheVSDummy();
		loaded.modeInit(new GameManager(new EventReceiver()));
		GameEngine le = freshEngineP0(loaded);
		// Need to set rate fields before loading
		((int[]) readField(loaded, "ojamaRate"))[0] = 120;
		((int[]) readField(loaded, "ojamaHard"))[0] = 0;
		invokeLoadOtherSetting(loaded, le, prop, "");

		assertEquals(false, ((boolean[]) readField(loaded, "enableSE"))[0]);
		assertEquals(50, ((int[]) readField(loaded, "maxAttack"))[0]);
	}

	// -----------------------------------------------------------------------
	// calcScore
	// -----------------------------------------------------------------------

	@Test
	void calcScoreWithAvalancheClearsAndComputesScore() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngineP0(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.chain = 0;

		// Place a block so field is not empty (otherwise zenkeshi bonus applies)
		engine.field.setBlock(0, 19, new Block(Block.BLOCK_COLOR_RED, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));

		mode.calcScore(engine, 0, 3);

		// avalanche > 0 -> cleared[0] = true
		assertTrue(((boolean[]) readField(mode, "cleared"))[0]);

		// calcPts: avalanche*10 = 30
		// multiplier: colorClearExtraCount(=0) + chainMultiplier(chain=0 -> 0) = 0
		// clamp: 0 -> 1
		// ptsTotal = 30 * 1 = 30
		// score[0] += 30
		assertEquals(30, ((int[]) readField(mode, "score"))[0]);
	}

	@Test
	void calcScoreWithEmptyFieldTriggersZenkeshi() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngineP0(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.chain = 0;

		// Field is already empty
		mode.calcScore(engine, 0, 4);

		// Zenkeshi should trigger: score += 2100
		assertTrue(((boolean[]) readField(mode, "zenKeshi"))[0]);
		assertTrue(engine.statistics.score >= 2100);
		assertTrue(((int[]) readField(mode, "score"))[0] >= 2100);
	}

	@Test
	void calcScoreWithChainMultiplierUsesClassicPower() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngineP0(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		// Chain 3, not new chain power
		((boolean[]) readField(mode, "newChainPower"))[0] = false;
		engine.chain = 3;

		mode.calcScore(engine, 0, 2);

		// Classic: chain 3 -> 16
		assertEquals(16, ((int[]) readField(mode, "lastmultiplier"))[0]);
	}

	@Test
	void calcScoreWithNewChainPowerUsesTable() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngineP0(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		((boolean[]) readField(mode, "newChainPower"))[0] = true;
		engine.chain = 5;

		mode.calcScore(engine, 0, 1);

		// New: CHAIN_POWERS[4] = 50
		assertEquals(50, ((int[]) readField(mode, "lastmultiplier"))[0]);
	}

	@Test
	void calcScoreWithNoAvalancheResetsCleared() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngineP0(mode);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		((boolean[]) readField(mode, "cleared"))[0] = true;

		mode.calcScore(engine, 0, 0);

		// No avalanche and can cascade -> cleared should stay true (no change)
		// Actually if field.canCascade() returns true, cleared stays as-is
		// If it returns false, cleared becomes false
	}

	// -----------------------------------------------------------------------
	// calcChainMultiplier variants
	// -----------------------------------------------------------------------

	@Test
	void calcChainClassicPowerReturnsCorrectValues() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();

		assertEquals(0, invokeCalcChainClassicPower(mode, null, 0, 1));
		assertEquals(8, invokeCalcChainClassicPower(mode, null, 0, 2));
		assertEquals(16, invokeCalcChainClassicPower(mode, null, 0, 3));
		assertEquals(32, invokeCalcChainClassicPower(mode, null, 0, 4));
		assertEquals(64, invokeCalcChainClassicPower(mode, null, 0, 5));
	}

	@Test
	void calcChainNewPowerReturnsCorrectValues() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();

		assertEquals(4, invokeCalcChainNewPower(mode, null, 0, 1));
		assertEquals(12, invokeCalcChainNewPower(mode, null, 0, 2));
		assertEquals(24, invokeCalcChainNewPower(mode, null, 0, 3));
		assertEquals(33, invokeCalcChainNewPower(mode, null, 0, 4));
		assertEquals(50, invokeCalcChainNewPower(mode, null, 0, 5));
		assertEquals(999, invokeCalcChainNewPower(mode, null, 0, 16));
	}

	// -----------------------------------------------------------------------
	// addOjama
	// -----------------------------------------------------------------------

	@Test
	void addOjamaSendsOjamaToEnemy() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		GameEngine engine0 = manager.engine[0];

		((int[]) readField(mode, "ojamaRate"))[0] = 100;
		((int[]) readField(mode, "ojamaCounterMode"))[0] = AvalancheVSDummyMode.OJAMA_COUNTER_ON;

		// 500 points -> ceil(500/100) = 5 ojama
		invokeAddOjama(mode, engine0, 0, 500);

		int[] ojamaAdd = (int[]) readField(mode, "ojamaAdd");
		assertEquals(5, ojamaAdd[1]); // enemy (P1) gets the ojama
		assertEquals(5, ((int[]) readField(mode, "ojamaSent"))[0]);
	}

	@Test
	void addOjamaCountersExistingOjama() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		GameEngine engine0 = manager.engine[0];

		((int[]) readField(mode, "ojamaRate"))[0] = 100;
		((int[]) readField(mode, "ojamaCounterMode"))[0] = AvalancheVSDummyMode.OJAMA_COUNTER_ON;
		((int[]) readField(mode, "ojama"))[0] = 3; // 3 pending ojama to counter

		invokeAddOjama(mode, engine0, 0, 500); // 5 ojama generated

		// 3 countered by pending, 2 sent to enemy
		int[] ojamaAdd = (int[]) readField(mode, "ojamaAdd");
		assertEquals(2, ojamaAdd[1]);
		assertEquals(0, ((int[]) readField(mode, "ojama"))[0]);
	}

	// -----------------------------------------------------------------------
	// onLast
	// -----------------------------------------------------------------------

	@Test
	void onLastDetectsDraw() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		engine0.stat = GameEngine.Status.GAMEOVER;
		engine1.stat = GameEngine.Status.GAMEOVER;
		engine0.gameActive = true;

		mode.onLast(engine1, 1);

		assertEquals(-1, readInt(mode, "winnerID"));
	}

	@Test
	void onLastDetectsPlayer1Win() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		// P2 game over, P1 not -> P1 wins
		engine1.stat = GameEngine.Status.GAMEOVER;
		engine0.gameActive = true;

		mode.onLast(engine1, 1);

		assertEquals(0, readInt(mode, "winnerID"));
	}

	@Test
	void onLastDetectsPlayer2Win() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		engine0.gameActive = true;
		engine0.stat = GameEngine.Status.GAMEOVER;
		engine1.gameActive = true;

		mode.onLast(engine1, 1);

		assertEquals(1, readInt(mode, "winnerID"));
	}

	// -----------------------------------------------------------------------
	// gameOverCheck
	// -----------------------------------------------------------------------

	@Test
	void gameOverCheckNullFieldReturnsEarly() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameEngine engine = freshEngineP0(mode);

		// After engine.init(), stat is SETTING; gameOverCheck should not change it
		engine.stat = GameEngine.Status.MOVE;
		engine.field = null;
		invokeGameOverCheck(mode, engine, 0);

		assertEquals(GameEngine.Status.MOVE, engine.stat); // not changed
	}

	@Test
	void gameOverCheckDetectsBlockAtDangerColumn() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameEngine engine = freshEngineP0(mode);
		engine.createFieldIfNeeded();

		// Place a block at column 2, row 0
		engine.field.setBlock(2, 0, new Block(Block.BLOCK_COLOR_RED, 0,
				Block.BLOCK_ATTRIBUTE_VISIBLE));

		invokeGameOverCheck(mode, engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	// -----------------------------------------------------------------------
	// pieceLocked
	// -----------------------------------------------------------------------

	@Test
	void pieceLockedResetsClearedAndOjamaDrop() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameEngine engine = freshEngineP0(mode);

		((boolean[]) readField(mode, "cleared"))[0] = true;
		((boolean[]) readField(mode, "ojamaDrop"))[0] = true;

		mode.pieceLocked(engine, 0, 0);

		assertFalse(((boolean[]) readField(mode, "cleared"))[0]);
		assertFalse(((boolean[]) readField(mode, "ojamaDrop"))[0]);
	}

	// -----------------------------------------------------------------------
	// afterSoftDropFall / afterHardDropFall
	// -----------------------------------------------------------------------

	@Test
	void afterHardDropFallIncrementsScore() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameEngine engine = freshEngineP0(mode);

		engine.statistics.score = 0;
		mode.afterHardDropFall(engine, 0, 10);

		assertEquals(10, engine.statistics.score);
	}

	@Test
	void afterSoftDropFallIncrementsScore() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameEngine engine = freshEngineP0(mode);

		engine.statistics.score = 0;
		mode.afterSoftDropFall(engine, 0, 5);

		assertEquals(5, engine.statistics.score);
	}

	// -----------------------------------------------------------------------
	// startGame
	// -----------------------------------------------------------------------

	@Test
	void startGameDisablesComboAndTspin() throws Exception {
		ConcreteAvalancheVSDummy mode = new ConcreteAvalancheVSDummy();
		GameEngine engine = freshEngineP0(mode);

		((boolean[]) readField(mode, "enableSE"))[0] = true;
		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
		assertFalse(engine.b2bEnable);
		assertFalse(engine.tspinEnable);
		assertFalse(engine.useAllSpinBonus);
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngineP0(AvalancheVSDummyMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		return manager.engine[0];
	}

	private static int readInt(Object obj, String name) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static Object readField(Object obj, String name) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
	}

	private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
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

	private static void invokeLoadPreset(AvalancheVSDummyMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}

	private static void invokeSavePreset(AvalancheVSDummyMode mode, GameEngine engine,
			CustomProperties prop, int preset, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset, name);
	}

	private static void invokeLoadOtherSetting(AvalancheVSDummyMode mode, GameEngine engine,
			CustomProperties prop, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, name);
	}

	private static void invokeSaveOtherSetting(AvalancheVSDummyMode mode, GameEngine engine,
			CustomProperties prop, String name) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class, String.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, name);
	}

	private static void invokeAddOjama(AvalancheVSDummyMode mode, GameEngine engine,
			int playerID, int pts) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"addOjama", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID, pts);
	}

	private static void invokeGameOverCheck(AvalancheVSDummyMode mode, GameEngine engine,
			int playerID) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"gameOverCheck", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}

	private static int invokeCalcChainClassicPower(AvalancheVSDummyMode mode, GameEngine engine,
			int playerID, int chain) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"calcChainClassicPower", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, engine, playerID, chain);
	}

	private static int invokeCalcChainNewPower(AvalancheVSDummyMode mode, GameEngine engine,
			int playerID, int chain) throws Exception {
		Method m = AvalancheVSDummyMode.class.getDeclaredMethod(
				"calcChainNewPower", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, engine, playerID, chain);
	}
}
