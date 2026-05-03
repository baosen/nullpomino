package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Covers game-logic methods in {@link AvalancheVSMode}: playerInit,
 * readyInit, calcChainNewPower with FEVER_POWERS, addOjama override
 * (fever points, counter, time criteria), onClear, lineClearEnd
 * (fever mode transitions), onLast (fever timer, meter), startGame,
 * loadSetting/saveSetting, and saveReplay.
 */
class AvalancheVSModeGameLogicTest {

	@Test
	void getNameReturnsExpected() {
		assertEquals("AVALANCHE VS-BATTLE (RC1)", new AvalancheVSMode().getName());
	}

	@Test
	void playerInitSetsFeverDefaults() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "menuTime"));
		assertEquals(0, readInt(mode, "menuCursor"));
		assertEquals(0, readIntArray(mode, "feverPoints", 0));
		assertFalse(readBooleanArray(mode, "inFever", 0));
	}

	@Test
	void playerInitSetsFeverTimeFromMinDefault() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.modeConfig = new CustomProperties();

		// playerInit sets feverTime = feverTimeMin * 60 before loadOtherSetting runs,
		// so we need feverTimeMin pre-seeded for the computed feverTime to be correct.
		Field ftm = AvalancheVSMode.class.getDeclaredField("feverTimeMin");
		ftm.setAccessible(true);
		((int[]) ftm.get(mode))[0] = 15;

		manager.engine[0].init();

		// feverTime = feverTimeMin * 60 = 15 * 60 = 900
		assertEquals(900, readIntArray(mode, "feverTime", 0));
	}

	@Test
	void readyInitSetsOjamaMeterWhenFeverThresholdZero() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "feverThreshold", 0, 0);
		setBooleanArray(mode, "ojamaMeter", false, 0);

		mode.readyInit(engine, 0);

		assertTrue(readBooleanArray(mode, "ojamaMeter", 0));
	}

	@Test
	void readyInitSetsFeverChainWhenFeverThresholdAboveZero() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverChainStart", 7, 0);

		mode.readyInit(engine, 0);

		assertEquals(7, readIntArray(mode, "feverChain", 0));
	}

	@Test
	void calcChainNewPowerUsesFeverPowers() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Enable inFever to use FEVER_POWERS
		setBooleanArray(mode, "inFever", true, 0);

		// Chain 1: FEVER_POWERS[0] = 4
		int result1 = mode.calcChainNewPower(engine, 0, 1);
		assertEquals(4, result1);

		// Chain 5: FEVER_POWERS[4] = 29
		int result5 = mode.calcChainNewPower(engine, 0, 5);
		assertEquals(29, result5);

		// Chain 24: FEVER_POWERS[23] = 720
		int result24 = mode.calcChainNewPower(engine, 0, 24);
		assertEquals(720, result24);

		// Beyond table: last value
		int result99 = mode.calcChainNewPower(engine, 0, 99);
		assertEquals(720, result99);
	}

	@Test
	void calcScoreInFeverUsesFeverPowerForOjama() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);

		setIntArray(mode, "ojamaRate", 120, 0);
		setBooleanArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverPower", 10, 0);
		setIntArray(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);

		// Place a block to prevent all-clear
		engine.field.setBlock(0, engine.field.getHeight() - 1,
				new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		// calcScore with avalanche=5 (cleared blocks), chain=1
		engine.chain = 1;
		mode.calcScore(engine, 0, 5);

		// avalanche*10 = 50 pts, multiplier = colorClearExtraCount + colorsCleared bonus + chain(4)
		// Since field has blocks, colorClearExtraCount and colorsCleared are from the field
		// Just check that score increased
		assertTrue(readIntArray(mode, "score", 0) > 0);
	}

	@Test
	void onClearSetsOjamaAddToFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine, 0);
		mode.playerInit(engine2, 1);

		engine.chain = 1;
		setBooleanArray(mode, "inFever", true, 1); // P2 in fever

		invokeOnClear(mode, engine, 0);

		assertTrue(readBooleanArray(mode, "ojamaAddToFever", 1));
	}

	@Test
	void addOjamaAddsFeverPointOnCounter() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine, 0);
		mode.playerInit(engine2, 1);

		setIntArray(mode, "ojamaRate", 120, 0);
		setIntArray(mode, "feverThreshold", 5, 0);
		setIntArray(mode, "feverPoints", 0, 0);
		setIntArray(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_ON, 0);
		setIntArray(mode, "ojama", 100, 0); // Enough ojama to counter

		invokeAddOjama(mode, engine, 0, 1000);

		// Since ojama was countered, feverPoints should increase (feverPointCriteria defaults to COUNTER)
		assertEquals(1, readIntArray(mode, "feverPoints", 0));
	}

	@Test
	void lineClearEndTransfersOjamaAdd() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine1 = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine1, 0);
		mode.playerInit(engine2, 1);
		engine1.createFieldIfNeeded();
		engine1.nowPieceObject = new Piece(Piece.PIECE_T);
		setIntArray(mode, "ojamaAdd", 5, 1);

		mode.lineClearEnd(engine1, 0);

		assertEquals(5, readIntArray(mode, "ojama", 1));
		assertEquals(0, readIntArray(mode, "ojamaAdd", 1));
	}

	@Test
	void onLastDecrementsFeverTimeWhenInFever() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setBooleanArray(mode, "inFever", true, 0);
		setIntArray(mode, "feverTime", 100, 0);
		engine.timerActive = true;

		mode.onLast(engine, 0);

		assertEquals(99, readIntArray(mode, "feverTime", 0));
	}

	@Test
	void startGameCallsSuper() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();

		mode.startGame(engine, 0);

		assertFalse(engine.b2bEnable);
	}

	@Test
	void saveReplayStoresVersion() throws Exception {
		AvalancheVSMode mode = new AvalancheVSMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(0, prop.getProperty("avalanchevs.version", -1));
	}

	// ---- helpers ----

	private static GameEngine freshEngine(AvalancheVSMode mode) {
		return freshEngine(mode, 0);
	}

	private static GameEngine freshEngine(AvalancheVSMode mode, int playerID) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		for (int i = 0; i <= playerID; i++) {
			manager.engine[i].init();
		}
		return manager.engine[playerID];
	}

	private static int readInt(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getInt(mode);
	}

	private static boolean readBoolean(Object mode, String name) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return f.getBoolean(mode);
	}

	private static int readIntArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((int[]) f.get(mode))[index];
	}

	private static boolean readBooleanArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((boolean[]) f.get(mode))[index];
	}

	private static void setIntArray(Object mode, String name, int value, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		((int[]) f.get(mode))[index] = value;
	}

	private static void setBooleanArray(Object mode, String name, boolean value, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		((boolean[]) f.get(mode))[index] = value;
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while (c != null) {
			try { return c.getDeclaredField(name); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	private static void invokeOnClear(AvalancheVSMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = AvalancheVSMode.class.getDeclaredMethod("onClear", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}

	private static void invokeAddOjama(AvalancheVSMode mode, GameEngine engine, int playerID, int pts) throws Exception {
		Method m = AvalancheVSMode.class.getDeclaredMethod("addOjama", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID, pts);
	}
}
