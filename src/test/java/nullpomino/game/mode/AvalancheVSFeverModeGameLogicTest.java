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
 * Covers game-logic methods in {@link AvalancheVSFeverMode}: playerInit,
 * readyInit, startGame, calcChainNewPower with FEVER_POWERS, addOjama
 * override (handicap counter), onClear, lineClearEnd (fever chain,
 * zenkeshi, ojama drop), onLast, loadSetting/saveSetting,
 * getChainColor, and saveReplay.
 */
class AvalancheVSFeverModeGameLogicTest {

	@Test
	void getNameReturnsExpected() {
		assertEquals("AVALANCHE VS FEVER MARATHON (RC1)",
				new AvalancheVSFeverMode().getName());
	}

	@Test
	void playerInitSetsOjamaCounterToFever() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// playerInit sets feer then loadOtherSetting resets to default (OJAMA_COUNTER_ON)
		assertEquals(AvalancheVSDummyMode.OJAMA_COUNTER_ON,
				readIntArray(mode, "ojamaCounterMode", 0));
		assertEquals(0, readIntArray(mode, "ojama", 0));
	}

	@Test
	void playerInitDefaultOjamaHandicapIs270() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(270, readIntArray(mode, "ojamaHandicap", 0));
	}

	@Test
	void playerInitDefaultOjamaRateIs120() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(120, readIntArray(mode, "ojamaRate", 0));
	}

	@Test
	void readyInitSetsHandicapLeftAndFeverChain() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "ojamaHandicap", 100, 0);
		setIntArray(mode, "feverChainStart", 5, 0);

		mode.readyInit(engine, 0);

		assertEquals(100, readIntArray(mode, "ojamaHandicapLeft", 0));
		assertEquals(5, readIntArray(mode, "feverChain", 0));
	}

	@Test
	void startGameLoadsFeverMap() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "feverChain", 5, 0);

		mode.startGame(engine, 0);

		// startGame calls loadFeverMap which creates field
		assertTrue(engine.field != null);
	}

	@Test
	void calcChainNewPowerUsesFEVER_POWERS() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		// Chain 1 -> FEVER_POWERS[0] = 4
		assertEquals(4, mode.calcChainNewPower(engine, 0, 1));
		// Chain 5 -> FEVER_POWERS[4] = 29
		assertEquals(29, mode.calcChainNewPower(engine, 0, 5));
		// Beyond table -> last
		assertEquals(720, mode.calcChainNewPower(engine, 0, 100));
	}

	@Test
	void addOjamaConsumesHandicapFirst() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine, 0);
		mode.playerInit(engine2, 1);
		setIntArray(mode, "ojamaRate", 120, 0);
		setIntArray(mode, "ojama", 0, 0);
		setIntArray(mode, "ojamaHandicapLeft", 50, 0);

		// Add large pts - will go through handicap first
		invokeAddOjama(mode, engine, 0, 60000);

		// pts=60000, rate=120, ceil(60000/120)=500
		// handicap 50 consumed, remainder 450 goes to enemy
		assertEquals(0, readIntArray(mode, "ojamaHandicapLeft", 0));
		assertEquals(450, readIntArray(mode, "ojamaAdd", 1));
	}

	@Test
	void addOjamaWithRateHandicapAndCounter() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine, 0);
		mode.playerInit(engine2, 1);
		setIntArray(mode, "ojamaRate", 120, 0);
		setIntArray(mode, "ojama", 100, 0); // counter
		setIntArray(mode, "ojamaHandicapLeft", 200, 0);

		// pts = 60000 -> ceil(60000/120) = 500
		// counter: ojama 100 consumed, 400 left
		// handicap: 200 consumed, 200 left
		// 200 to enemy
		invokeAddOjama(mode, engine, 0, 60000);

		assertEquals(0, readIntArray(mode, "ojama", 0));
		assertEquals(0, readIntArray(mode, "ojamaHandicapLeft", 0));
		assertEquals(200, readIntArray(mode, "ojamaAdd", 1));
	}

	@Test
	void lineClearEndTransfersOjamaAdd() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine1 = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine1, 0);
		mode.playerInit(engine2, 1);
		engine1.createFieldIfNeeded();
		engine1.nowPieceObject = new Piece(Piece.PIECE_T);
		setIntArray(mode, "ojamaAdd", 30, 1);

		mode.lineClearEnd(engine1, 0);

		assertEquals(30, readIntArray(mode, "ojama", 1));
		assertEquals(0, readIntArray(mode, "ojamaAdd", 1));
	}

	@Test
	void lineClearEndUpdatesFeverChainOnClear() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setBooleanArray(mode, "cleared", true, 0);
		setIntArray(mode, "feverChain", 5, 0);
		engine.chain = 7;

		mode.lineClearEnd(engine, 0);

		// newFeverChain = max(7+1, 5-2) = max(8,3) = 8
		assertEquals(8, readIntArray(mode, "feverChain", 0));
	}

	@Test
	void onLastDecrementsStandardCounters() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "scgettime", 10, 0);
		setIntArray(mode, "zenKeshiDisplay", 5, 0);
		setIntArray(mode, "chainDisplay", 3, 0);

		mode.onLast(engine, 0);

		assertEquals(9, readIntArray(mode, "scgettime", 0));
		assertEquals(4, readIntArray(mode, "zenKeshiDisplay", 0));
		assertEquals(2, readIntArray(mode, "chainDisplay", 0));
	}

	@Test
	void getChainColorWithFeverSizeReturnsGreenWhenChainAtFeverChain() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "chainDisplayType",
				AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE, 0);
		setIntArray(mode, "feverChainDisplay", 5, 0);
		engine.chain = 5;

		int color = mode.getChainColor(engine, 0);

		assertEquals(EventReceiver.COLOR_GREEN, color);
	}

	@Test
	void getChainColorWithFeverSizeReturnsOrangeWhenTwoBelow() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "chainDisplayType",
				AvalancheVSFeverMode.CHAIN_DISPLAY_FEVERSIZE, 0);
		setIntArray(mode, "feverChainDisplay", 5, 0);
		engine.chain = 3;

		int color = mode.getChainColor(engine, 0);

		assertEquals(EventReceiver.COLOR_ORANGE, color);
	}

	@Test
	void onClearSetsFeverChainDisplay() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "feverChain", 7, 0);

		invokeOnClear(mode, engine, 0);

		assertEquals(7, readIntArray(mode, "feverChainDisplay", 0));
	}

	@Test
	void loadSaveSettingRoundTrip() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		setIntArray(mode, "ojamaHandicap", 500, 0);
		setIntArray(mode, "feverChainStart", 7, 0);

		invokeSaveOtherSetting(mode, engine, prop);
		invokeLoadOtherSetting(mode, engine, prop);

		assertEquals(500, readIntArray(mode, "ojamaHandicap", 0));
		assertEquals(7, readIntArray(mode, "feverChainStart", 0));
	}

	@Test
	void saveReplayStoresVersion() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(1, prop.getProperty("avalanchevsfever.version", -1));
	}

	@Test
	void modeInitCreatesArrays() throws Exception {
		AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		int[] ojamaHandicapLeft = (int[]) readFieldByClass(mode, AvalancheVSFeverMode.class, "ojamaHandicapLeft");
		int[] feverChain = (int[]) readFieldByClass(mode, AvalancheVSFeverMode.class, "feverChain");
		int[] ojamaHandicap = (int[]) readFieldByClass(mode, AvalancheVSFeverMode.class, "ojamaHandicap");
		assertEquals(2, ojamaHandicapLeft.length);
		assertEquals(2, feverChain.length);
		assertEquals(2, ojamaHandicap.length);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(AvalancheVSFeverMode mode) {
		return freshEngine(mode, 0);
	}

	private static GameEngine freshEngine(AvalancheVSFeverMode mode, int playerID) {
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

	private static Object readFieldByClass(Object mode, Class<?> cls, String name) throws Exception {
		Field f = cls.getDeclaredField(name);
		f.setAccessible(true);
		return f.get(mode);
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

	private static void invokeSaveOtherSetting(AvalancheVSFeverMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = AvalancheVSFeverMode.class.getDeclaredMethod("saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeLoadOtherSetting(AvalancheVSFeverMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = AvalancheVSFeverMode.class.getDeclaredMethod("loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeAddOjama(AvalancheVSFeverMode mode, GameEngine engine, int playerID, int pts) throws Exception {
		Method m = AvalancheVSFeverMode.class.getDeclaredMethod("addOjama", GameEngine.class, int.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID, pts);
	}

	private static void invokeOnClear(AvalancheVSFeverMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = AvalancheVSFeverMode.class.getDeclaredMethod("onClear", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}
}
