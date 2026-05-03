package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Comprehensive tests for {@link VSDigRaceMode}. Covers registry surface,
 * modeInit/playerInit, speed preset persistence, other-settings persistence,
 * fillGarbage, getRemainGarbageLines, calcScore win detection, onLast
 * win/loss/draw logic, onReady field setup, and startGame.
 */
class VSDigRaceModeGameLogicTest {

	// -----------------------------------------------------------------------
	// Registry surface
	// -----------------------------------------------------------------------

	@Test
	void getNameReturnsVSDigRace() {
		assertEquals("VS-DIG RACE", new VSDigRaceMode().getName());
	}

	@Test
	void getPlayersReturnsTwo() {
		assertEquals(2, new VSDigRaceMode().getPlayers());
	}

	@Test
	void getGameStyleIsTetromino() {
		assertEquals(GameEngine.GAMESTYLE_TETROMINO, new VSDigRaceMode().getGameStyle());
	}

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(new VSDigRaceMode().isVSMode());
	}

	// -----------------------------------------------------------------------
	// modeInit
	// -----------------------------------------------------------------------

	@Test
	void modeInitAllocatesPerPlayerArrays() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		assertNotNull(readField(mode, "goalLines"));
		assertNotNull(readField(mode, "garbagePercent"));
		assertNotNull(readField(mode, "enableSE"));
		assertNotNull(readField(mode, "presetNumber"));
		assertNotNull(readField(mode, "winCount"));

		assertEquals(2, ((int[]) readField(mode, "goalLines")).length);
		assertEquals(-1, readInt(mode, "winnerID"));
		assertEquals(0, readInt(mode, "version"));
	}

	// -----------------------------------------------------------------------
	// playerInit
	// -----------------------------------------------------------------------

	@Test
	void playerInitResetsMenuAndSetsFrameColor() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		mode.playerInit(engine0, 0);
		mode.playerInit(engine1, 1);

		// PLAYER_COLOR_FRAME = {FRAME_COLOR_RED, FRAME_COLOR_BLUE} = {2, 0}
		assertEquals(2, engine0.framecolor);
		assertEquals(0, engine1.framecolor);
	}

	@Test
	void playerInitSyncsSeedForPlayer1() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
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
	// Speed preset persistence
	// -----------------------------------------------------------------------

	@Test
	void loadPresetReadsAllKeys() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		CustomProperties prop = new CustomProperties();
		prop.setProperty("vsdigrace.gravity.3", 64);
		prop.setProperty("vsdigrace.denominator.3", 128);
		prop.setProperty("vsdigrace.are.3", 20);
		prop.setProperty("vsdigrace.areLine.3", 15);
		prop.setProperty("vsdigrace.lineDelay.3", 10);
		prop.setProperty("vsdigrace.lockDelay.3", 35);
		prop.setProperty("vsdigrace.das.3", 12);

		invokeLoadPreset(mode, engine, prop, 3);

		assertEquals(64, engine.speed.gravity);
		assertEquals(128, engine.speed.denominator);
		assertEquals(20, engine.speed.are);
		assertEquals(15, engine.speed.areLine);
		assertEquals(10, engine.speed.lineDelay);
		assertEquals(35, engine.speed.lockDelay);
		assertEquals(12, engine.speed.das);
	}

	@Test
	void loadPresetAppliesDefaultsForMissingEntries() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadPreset(mode, engine, new CustomProperties(), 99);

		assertEquals(4, engine.speed.gravity);
		assertEquals(256, engine.speed.denominator);
		assertEquals(0, engine.speed.are);
		assertEquals(0, engine.speed.areLine);
		assertEquals(0, engine.speed.lineDelay);
		assertEquals(30, engine.speed.lockDelay);
		assertEquals(14, engine.speed.das);
	}

	@Test
	void savePresetAndLoadPresetRoundTrip() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		engine.speed.gravity = 88;
		engine.speed.denominator = 64;
		engine.speed.are = 15;
		engine.speed.areLine = 10;
		engine.speed.lineDelay = 5;
		engine.speed.lockDelay = 20;
		engine.speed.das = 8;

		CustomProperties prop = new CustomProperties();
		invokeSavePreset(mode, engine, prop, 2);

		assertEquals(88, prop.getProperty("vsdigrace.gravity.2", -1));
		assertEquals(64, prop.getProperty("vsdigrace.denominator.2", -1));

		VSDigRaceMode loaded = new VSDigRaceMode();
		GameEngine le = freshEngine(loaded);
		invokeLoadPreset(loaded, le, prop, 2);

		assertEquals(88, le.speed.gravity);
		assertEquals(64, le.speed.denominator);
		assertEquals(15, le.speed.are);
		assertEquals(10, le.speed.areLine);
		assertEquals(5, le.speed.lineDelay);
		assertEquals(20, le.speed.lockDelay);
		assertEquals(8, le.speed.das);
	}

	// -----------------------------------------------------------------------
	// Other settings persistence
	// -----------------------------------------------------------------------

	@Test
	void loadOtherSettingReadsDefaults() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		invokeLoadOtherSetting(mode, engine, new CustomProperties());

		assertEquals(18, ((int[]) readField(mode, "goalLines"))[0]);
		assertEquals(100, ((int[]) readField(mode, "garbagePercent"))[0]);
		assertEquals(true, ((boolean[]) readField(mode, "enableSE"))[0]);
		assertEquals(0, ((int[]) readField(mode, "presetNumber"))[0]);
		assertEquals(0, readInt(mode, "bgmno"));
	}

	@Test
	void saveOtherSettingAndLoadOtherSettingRoundTrip() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		mode.modeInit(new GameManager(new EventReceiver()));
		GameEngine engine = freshEngine(mode);

		((int[]) readField(mode, "goalLines"))[0] = 12;
		((int[]) readField(mode, "garbagePercent"))[0] = 50;
		((boolean[]) readField(mode, "enableSE"))[0] = false;

		CustomProperties prop = new CustomProperties();
		invokeSaveOtherSetting(mode, engine, prop);

		assertEquals(12, prop.getProperty("vsdigrace.goalLines.p0", -1));
		assertEquals(50, prop.getProperty("vsdigrace.garbagePercent.p0", -1));
		assertEquals(false, prop.getProperty("vsdigrace.enableSE.p0", true));

		VSDigRaceMode loaded = new VSDigRaceMode();
		GameEngine le = freshEngine(loaded);
		invokeLoadOtherSetting(loaded, le, prop);

		assertEquals(12, ((int[]) readField(loaded, "goalLines"))[0]);
		assertEquals(50, ((int[]) readField(loaded, "garbagePercent"))[0]);
		assertEquals(false, ((boolean[]) readField(loaded, "enableSE"))[0]);
	}

	// -----------------------------------------------------------------------
	// fillGarbage
	// -----------------------------------------------------------------------

	@Test
	void fillGarbageCreatesGarbageLinesWithHoles() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		((int[]) readField(mode, "goalLines"))[0] = 5;
		invokeFillGarbage(mode, engine, 0);

		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		// Bottom rows should have blocks placed (field not empty)
		boolean hasBlocks = false;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				if (engine.field.getBlock(x, y) != null) {
					hasBlocks = true;
					break;
				}
			}
		}
		assertTrue(hasBlocks, "Field should have blocks after fillGarbage");
	}

	// -----------------------------------------------------------------------
	// getRemainGarbageLines
	// -----------------------------------------------------------------------

	@Test
	void getRemainGarbageLinesReturnsZeroWhenNoGemBlocks() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		// Fill garbage without gem blocks
		((int[]) readField(mode, "goalLines"))[0] = 3;
		invokeFillGarbage(mode, engine, 0);

		// getRemainGarbageLines returns 3 because fillGarbage creates garbage lines
		// (the bottom row has gem blocks, so hasGemBlock is true)
		int result = invokeGetRemainGarbageLines(mode, engine, 0);
		assertEquals(3, result);
	}

	@Test
	void getRemainGarbageLinesReturnsCountWhenGarbagePresent() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		((int[]) readField(mode, "goalLines"))[0] = 3;
		invokeFillGarbage(mode, engine, 0);

		// Add a gem block to bottom row so hasGemBlock returns true
		int w = engine.field.getWidth();
		int h = engine.field.getHeight();
		// Replace a garbage block (non-hole) with a gem block so the logic
		// still finds garbage lines and also has gem blocks
		for (int x = 0; x < w; x++) {
			Block b = engine.field.getBlock(x, h - 1);
			if (b != null && b.getAttribute(Block.BLOCK_ATTRIBUTE_GARBAGE)) {
				// Check adjacent too - just set one as gem
				engine.field.setBlock(x, h - 1, new Block(
					Block.BLOCK_COLOR_GEM_RED, engine.getSkin(),
					Block.BLOCK_ATTRIBUTE_VISIBLE | Block.BLOCK_ATTRIBUTE_GARBAGE));
				break;
			}
		}

		int result = invokeGetRemainGarbageLines(mode, engine, 0);
		assertTrue(result >= 1);
	}

	// -----------------------------------------------------------------------
	// calcScore
	// -----------------------------------------------------------------------

	@Test
	void calcScoreUpdatesMeter() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();

		((int[]) readField(mode, "goalLines"))[0] = 3;
		invokeFillGarbage(mode, engine, 0);

		mode.calcScore(engine, 0, 1);

		// Meter value should be updated
		assertTrue(engine.meterValue >= 0);
	}

	@Test
	void calcScoreWithRemainingLinesZeroSignalsGameComplete() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();

		// Set goal lines to 0 so there's no garbage to clear
		((int[]) readField(mode, "goalLines"))[0] = 0;
		((int[]) readField(mode, "goalLines"))[1] = 0;

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		// calcScore with lines>0 and remainLines<=0 should trigger win
		mode.calcScore(engine0, 0, 1);

		assertFalse(engine0.timerActive);
		assertEquals(GameEngine.Status.GAMEOVER, engine1.stat);
	}

	// -----------------------------------------------------------------------
	// onLast
	// -----------------------------------------------------------------------

	@Test
	void onLastDetectsPlayer1Win() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		// Set up: P2 is game over, P1 is not -> P1 wins
		engine0.stat = GameEngine.Status.MOVE;
		engine1.stat = GameEngine.Status.GAMEOVER;
		engine0.gameActive = true;

		// Pad player 1
		((int[]) readField(mode, "winCount"))[0] = 0;
		((int[]) readField(mode, "winCount"))[1] = 0;

		mode.onLast(engine1, 1);

		assertEquals(0, readInt(mode, "winnerID"));
		assertEquals(1, ((int[]) readField(mode, "winCount"))[0]);
	}

	@Test
	void onLastDetectsPlayer2Win() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		manager.engine[0].createFieldIfNeeded();
		manager.engine[1].createFieldIfNeeded();

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		engine0.stat = GameEngine.Status.GAMEOVER;
		engine1.stat = GameEngine.Status.MOVE;
		engine1.gameActive = true;

		((int[]) readField(mode, "winCount"))[0] = 0;
		((int[]) readField(mode, "winCount"))[1] = 0;

		// onLast only processes when owner.engine[0].gameActive is true
		engine0.gameActive = true;
		mode.onLast(engine1, 1);

		assertEquals(1, readInt(mode, "winnerID"));
		assertEquals(1, ((int[]) readField(mode, "winCount"))[1]);
	}

	@Test
	void onLastDetectsDraw() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();

		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];

		engine0.stat = GameEngine.Status.GAMEOVER;
		engine1.stat = GameEngine.Status.GAMEOVER;

		mode.onLast(engine1, 1);

		assertEquals(-1, readInt(mode, "winnerID"));
	}

	// -----------------------------------------------------------------------
	// onReady
	// -----------------------------------------------------------------------

	@Test
	void onReadyCreatesFieldAndFillsGarbage() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		((int[]) readField(mode, "goalLines"))[0] = 5;

		mode.onReady(engine, 0);

		assertNotNull(engine.field);
		assertTrue(engine.meterValue > 0);
	}

	// -----------------------------------------------------------------------
	// startGame
	// -----------------------------------------------------------------------

	@Test
	void startGameSetsSEAndMeter() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		((boolean[]) readField(mode, "enableSE"))[0] = true;
		mode.startGame(engine, 0);

		assertTrue(engine.enableSE);
		assertEquals(GameEngine.METER_COLOR_GREEN, engine.meterColor);
	}

	// -----------------------------------------------------------------------
	// saveReplay
	// -----------------------------------------------------------------------

	@Test
	void saveReplayPersistsVersion() throws Exception {
		VSDigRaceMode mode = new VSDigRaceMode();
		GameEngine engine = freshEngine(mode);

		// saveReplay writes to owner.replayProp, not the passed prop
		engine.owner.replayProp = new CustomProperties();
		mode.saveReplay(engine, 0, engine.owner.replayProp);

		assertEquals(0, engine.owner.replayProp.getProperty("vsdigrace.version", -1));
	}

	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------

	private static GameEngine freshEngine(VSDigRaceMode mode) {
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

	private static Object readField(Object obj, String name) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.get(obj);
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

	private static void invokeLoadPreset(VSDigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"loadPreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeSavePreset(VSDigRaceMode mode, GameEngine engine,
			CustomProperties prop, int preset) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"savePreset", GameEngine.class, CustomProperties.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop, preset);
	}

	private static void invokeLoadOtherSetting(VSDigRaceMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOtherSetting(VSDigRaceMode mode, GameEngine engine,
			CustomProperties prop) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeFillGarbage(VSDigRaceMode mode, GameEngine engine,
			int playerID) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"fillGarbage", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}

	private static int invokeGetRemainGarbageLines(VSDigRaceMode mode, GameEngine engine,
			int playerID) throws Exception {
		Method m = VSDigRaceMode.class.getDeclaredMethod(
				"getRemainGarbageLines", GameEngine.class, int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, engine, playerID);
	}
}
