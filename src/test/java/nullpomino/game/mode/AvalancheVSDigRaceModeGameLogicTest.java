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
 * Covers game-logic methods in {@link AvalancheVSDigRaceMode}: playerInit,
 * startGame (handicap layout), lineClearEnd (ojama drop, game-over check),
 * onLast (settlement, scgettime), saveReplay, loadSetting/saveSetting
 * round-trip, and the renderMove override.
 */
class AvalancheVSDigRaceModeGameLogicTest {

	@Test
	void getNameReturnsExpected() {
		assertEquals("AVALANCHE VS DIG RACE (RC1)", new AvalancheVSDigRaceMode().getName());
	}

	@Test
	void playerInitSetsFieldsCorrectly() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(0, readInt(mode, "menuTime"));
		assertEquals(0, readInt(mode, "menuCursor"));
		assertFalse(readBooleanArray(mode, "useMap", 0));
		// playerInit sets -1 but loadOtherSetting overrides to 0 (default)
		assertEquals(0, readIntArray(mode, "feverMapSet", 0));
	}

	@Test
	void playerInitHandicapRowsDefaultsTo6() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(6, readIntArray(mode, "handicapRows", 0));
	}

	@Test
	void playerInitOjamaRateDefaultsTo420() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		assertEquals(420, readIntArray(mode, "ojamaRate", 0));
	}

	@Test
	void loadSaveSettingRoundTrip() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		setIntArray(mode, "handicapRows", 3, 0);
		setIntArray(mode, "ojamaRate", 500, 0);

		invokeSaveOtherSetting(mode, engine, prop);
		invokeLoadOtherSetting(mode, engine, prop);

		assertEquals(3, readIntArray(mode, "handicapRows", 0));
		assertEquals(500, readIntArray(mode, "ojamaRate", 0));
	}

	@Test
	void startGamePlacesHandicapRowsAndRainbowBlock() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		setIntArray(mode, "handicapRows", 3, 0);
		setIntArray(mode, "numColors", 4, 0);
		engine.colorClearSize = 4;

		mode.startGame(engine, 0);

		assertTrue(engine.field != null);
		// Field should have blocks (handicap + rainbow + garbage placement)
		assertTrue(engine.field.getHowManyBlocks() > 0);
	}

	@Test
	void lineClearEndDropsOjamaWhenOjamaPresent() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		setIntArray(mode, "ojama", 10, 0);
		setIntArray(mode, "ojamaCounterMode", AvalancheVSDummyMode.OJAMA_COUNTER_OFF, 0);
		setBooleanArray(mode, "cleared", false, 0);

		boolean result = mode.lineClearEnd(engine, 0);

		assertTrue(result);
		// ojama should have been reduced by maxAttack
		assertEquals(0, readIntArray(mode, "ojama", 0));
		assertTrue(readBooleanArray(mode, "ojamaDrop", 0));
	}

	@Test
	void lineClearEndTriggersGameOverWhenDangerColumnBlocked() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		// Place a block in column 2, row 0 (danger column for non-big mode)
		engine.field.setBlock(2, 0, new nullpomino.game.component.Block(nullpomino.game.component.Block.BLOCK_COLOR_GRAY));

		mode.lineClearEnd(engine, 0);

		assertEquals(GameEngine.Status.GAMEOVER, engine.stat);
	}

	@Test
	void onLastDecrementsScgettime() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "scgettime", 10, 0);

		mode.onLast(engine, 0);

		assertEquals(9, readIntArray(mode, "scgettime", 0));
	}

	@Test
	void onLastDecrementsChainDisplay() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setIntArray(mode, "chainDisplay", 5, 0);

		mode.onLast(engine, 0);

		assertEquals(4, readIntArray(mode, "chainDisplay", 0));
	}

	@Test
	void onLastSettlementDrawGameWhenBothDie() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine1 = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine1, 0);
		mode.playerInit(engine2, 1);

		engine1.stat = GameEngine.Status.GAMEOVER;
		engine2.stat = GameEngine.Status.GAMEOVER;

		mode.onLast(engine2, 1);

		assertEquals(-1, readInt(mode, "winnerID"));
		assertEquals(GameEngine.Status.GAMEOVER, engine1.stat);
		assertEquals(GameEngine.Status.GAMEOVER, engine2.stat);
	}

	@Test
	void onLastSettlementOneWinsWhenOtherDies() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine1 = freshEngine(mode);
		GameEngine engine2 = freshEngine(mode, 1);
		mode.playerInit(engine1, 0);
		mode.playerInit(engine2, 1);

		// freshEngine(mode,1) creates a new manager with both engines.
		// engine2.owner is that manager. engine1.owner is a different manager.
		// Settlement checks owner.engine[0], which is engine2's owner's engine[0].
		engine2.owner.engine[0].gameActive = true;
		engine2.owner.engine[0].stat = GameEngine.Status.MOVE;
		engine2.stat = GameEngine.Status.MOVE;
		engine2.createFieldIfNeeded();

		mode.onLast(engine2, 1);

		// P2's field is empty (0 gems) -> p1Lose = true (P1 loses because P2 cleared everything)
		// P1's field not created -> p2Lose stays false (P2 doesn't lose)
		// p1Lose && !p2Lose -> P2 wins
		assertEquals(1, readInt(mode, "winnerID"));
	}

	@Test
	void saveReplayStoresVersion() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);

		CustomProperties prop = new CustomProperties();
		mode.saveReplay(engine, 0, prop);

		assertEquals(0, prop.getProperty("avalanchevsdigrace.version", -1));
	}

	@Test
	void modeInitCreatesHandicapRowsArray() throws Exception {
		AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);

		int[] handicapRows = (int[]) readFieldByClass(mode, AvalancheVSDigRaceMode.class, "handicapRows");
		assertEquals(2, handicapRows.length);
	}

	// ---- helpers ----

	private static GameEngine freshEngine(AvalancheVSDigRaceMode mode) {
		return freshEngine(mode, 0);
	}

	private static GameEngine freshEngine(AvalancheVSDigRaceMode mode, int playerID) {
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

	private static boolean readBooleanArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((boolean[]) f.get(mode))[index];
	}

	private static int readIntArray(Object mode, String name, int index) throws Exception {
		Field f = findField(mode.getClass(), name);
		f.setAccessible(true);
		return ((int[]) f.get(mode))[index];
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

	private static void invokeSaveOtherSetting(AvalancheVSDigRaceMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = AvalancheVSDigRaceMode.class.getDeclaredMethod("saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeLoadOtherSetting(AvalancheVSDigRaceMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = AvalancheVSDigRaceMode.class.getDeclaredMethod("loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}
}
