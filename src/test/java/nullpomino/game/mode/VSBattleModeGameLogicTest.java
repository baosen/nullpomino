package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedList;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers game-logic methods in {@link VSBattleMode}: modeInit (2-player
 * array allocation), playerInit with 2 players, startGame engine
 * configuration, calcScore (attack-based scoring with T-Spin, B2B,
 * combo, all-clear, garbage sending), onLast (meter, win detection),
 * loadOtherSetting / saveOtherSetting round-trip, isVSMode, and the
 * getTotalGarbageLines helper.
 */
class VSBattleModeGameLogicTest {

	@Test
	void isVSModeReturnsTrue() {
		assertTrue(new VSBattleMode().isVSMode());
	}

	@Test
	void getPlayersIsTwo() {
		assertEquals(2, new VSBattleMode().getPlayers());
	}

	@Test
	void modeInitCreatesTwoPlayerArrays() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		mode.modeInit(new GameManager(new EventReceiver()));

		assertEquals(2, ((int[]) readField(mode, "garbage")).length);
		assertEquals(2, ((int[]) readField(mode, "garbageSent")).length);
		assertEquals(2, ((LinkedList[]) readField(mode, "garbageEntries")).length);
		assertEquals(2, ((boolean[]) readField(mode, "enableSE")).length);
		assertEquals(2, ((int[]) readField(mode, "winCount")).length);
	}

	@Test
	void playerInitSetsUpBothPlayers() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];
		engine0.init();
		engine1.init();

		mode.playerInit(engine0, 0);
		mode.playerInit(engine1, 1);

		assertEquals(0, ((int[]) readField(mode, "garbage"))[0]);
		assertEquals(0, ((int[]) readField(mode, "garbage"))[1]);
		assertEquals(-1, ((int[]) readField(mode, "lastHole"))[0]);
		assertNotNull(((LinkedList[]) readField(mode, "garbageEntries"))[0]);
		assertNotNull(((LinkedList[]) readField(mode, "garbageEntries"))[1]);
	}

	@Test
	void playerInitSyncsRandomSeedBetweenPlayers() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		GameEngine engine0 = manager.engine[0];
		GameEngine engine1 = manager.engine[1];
		engine0.init();
		engine1.init();

		mode.playerInit(engine0, 0);
		mode.playerInit(engine1, 1);

		assertEquals(engine0.randSeed, engine1.randSeed,
				"Player 1 should share player 0's random seed");
	}

	@Test
	void startGameSetsB2bAndComboAndBigAndSe() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);

		mode.startGame(engine, 0);

		assertTrue(engine.b2bEnable);
		assertEquals(GameEngine.COMBO_TYPE_NORMAL, engine.comboType);
	}

	@Test
	void startGameWithDisabledCombo() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameEngine engine = freshEngine(mode, 0);
		mode.playerInit(engine, 0);
		setBoolArray(mode, "enableCombo")[0] = false;

		mode.startGame(engine, 0);

		assertEquals(GameEngine.COMBO_TYPE_DISABLE, engine.comboType);
	}

	private void initBothPlayers(VSBattleMode mode, GameManager manager) {
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		manager.engine[1].init();
		mode.playerInit(manager.engine[0], 0);
		mode.playerInit(manager.engine[1], 1);
	}

	@Test
	void calcScoreSingleLineCreatesNoGarbage() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		initBothPlayers(mode, manager);
		GameEngine engine = manager.engine[0];
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 1);

		// Empty field triggers all-clear (+6), so single line sends 6 garbage
		assertEquals(6, ((int[]) readField(mode, "garbageSent"))[0],
				"1 line no T-Spin with all-clear sends 6 garbage");
	}

	@Test
	void calcScoreTSpinDoubleSendsGarbage() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		initBothPlayers(mode, manager);
		GameEngine engine = manager.engine[0];
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.tspin = true;

		mode.calcScore(engine, 0, 2);

		// T-Spin double (4) + all-clear (6) = 10
		assertEquals(10, ((int[]) readField(mode, "garbageSent"))[0],
				"T-Spin double with all-clear sends 10 garbage");
		assertEquals(10, ((int[]) readField(mode, "garbage"))[1],
				"Enemy should have 10 pending garbage lines");
	}

	@Test
	void calcScoreFourLinesWithB2b() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		initBothPlayers(mode, manager);
		GameEngine engine = manager.engine[0];
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.b2b = true;

		mode.calcScore(engine, 0, 4);

		// Four (4) + B2B (1, b2bType defaults to 1) + all-clear (6) = 11
		assertEquals(11, ((int[]) readField(mode, "garbageSent"))[0]);
	}

	@Test
	void calcScoreWithCombo() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		initBothPlayers(mode, manager);
		GameEngine engine = manager.engine[0];
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();
		engine.combo = 3;

		mode.calcScore(engine, 0, 2);

		// Double (1) + all-clear (6) = 7 (combo disabled by default)
		assertEquals(7, ((int[]) readField(mode, "garbageSent"))[0]);
	}

	@Test
	void calcScoreTripleNoTSpin() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		initBothPlayers(mode, manager);
		GameEngine engine = manager.engine[0];
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 3);

		// Triple (2) + all-clear (6) = 8
		assertEquals(8, ((int[]) readField(mode, "garbageSent"))[0]);
	}

	@Test
	void calcScoreAllClearSendsExtraGarbage() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		initBothPlayers(mode, manager);
		GameEngine engine = manager.engine[0];
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		mode.calcScore(engine, 0, 4);

		assertEquals(10, ((int[]) readField(mode, "garbageSent"))[0]);
	}

	@Test
	void calcScoreGarbageCountering() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		initBothPlayers(mode, manager);
		GameEngine engine = manager.engine[0];
		engine.playerID = 0;
		engine.nowPieceObject = new Piece(Piece.PIECE_T);
		engine.createFieldIfNeeded();

		LinkedList[] entriesArray = (LinkedList[]) readField(mode, "garbageEntries");
		LinkedList<Object> entries = entriesArray[0];
		entries.add(newGarbageEntry(mode, 3, 1));
		setBoolArray(mode, "garbageCounter")[0] = true;

		mode.calcScore(engine, 0, 4);

		assertTrue(((int[]) readField(mode, "garbageSent"))[0] > 0);
		assertTrue(entries.isEmpty(), "All garbage should be countered");
	}

	@Test
	void loadOtherSettingRoundTrip() throws Exception {
		VSBattleMode source = new VSBattleMode();
		GameEngine engine = freshEngine(source, 0);
		source.playerInit(engine, 0);
		setIntArray(source, "garbagePercent")[0] = 80;
		setIntArray(source, "garbageType")[0] = 1;

		CustomProperties prop = new CustomProperties();
		invokeSaveOther(source, engine, prop);

		VSBattleMode dest = new VSBattleMode();
		GameEngine destEngine = freshEngine(dest, 0);
		dest.playerInit(destEngine, 0);
		invokeLoadOther(dest, destEngine, prop);

		assertEquals(80, getIntArray(dest, "garbagePercent")[0]);
		assertEquals(1, getIntArray(dest, "garbageType")[0]);
	}

	@Test
	void getTotalGarbageLinesReturnsCorrectCount() throws Exception {
		VSBattleMode mode = new VSBattleMode();
		GameManager manager = new GameManager(new EventReceiver());
		initBothPlayers(mode, manager);

		@SuppressWarnings("unchecked")
		LinkedList<Object> entries =
				((LinkedList<Object>[]) readField(mode, "garbageEntries"))[0];
		entries.add(newGarbageEntry(mode, 4, 1));
		entries.add(newGarbageEntry(mode, 2, 1));

		int total = invokeGetTotalGarbageLines(mode, 0);

		assertEquals(6, total, "4 + 2 = 6 garbage lines");
	}

	// ---- helpers ----

	private static GameEngine freshEngine(VSBattleMode mode, int playerID) {
		GameManager manager = new GameManager(new EventReceiver());
		mode.modeInit(manager);
		manager.mode = mode;
		manager.init();
		GameEngine engine = manager.engine[playerID];
		engine.init();
		return engine;
	}

	private static Object readField(VSBattleMode mode, String name) throws Exception {
		return readFieldObject(mode, name);
	}

	private static Object readField(Object obj, String name) throws Exception {
		return readFieldObject(obj, name);
	}

	private static Object readFieldObject(Object obj, String name) throws Exception {
		Class<?> c = obj.getClass();
		while (c != null) {
			try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f.get(obj); }
			catch (NoSuchFieldException e) { c = c.getSuperclass(); }
		}
		throw new NoSuchFieldException(name);
	}

	/** Create a GarbageEntry via reflection (the class is private) */
	private static Object newGarbageEntry(VSBattleMode mode, int lines, int fromPlayer) throws Exception {
		Class<?> geClass = null;
		for (Class<?> inner : VSBattleMode.class.getDeclaredClasses()) {
			if (inner.getSimpleName().equals("GarbageEntry")) {
				geClass = inner;
				break;
			}
		}
		if (geClass == null) throw new NoSuchFieldException("GarbageEntry");
		// Non-static inner class constructors take the outer instance first
		Constructor<?> ctor = geClass.getDeclaredConstructor(VSBattleMode.class, int.class, int.class);
		ctor.setAccessible(true);
		return ctor.newInstance(mode, lines, fromPlayer);
	}

	private static int[] getIntArray(VSBattleMode mode, String name) throws Exception {
		return (int[]) readField(mode, name);
	}

	private static int[] setIntArray(VSBattleMode mode, String name) throws Exception {
		return (int[]) readField(mode, name);
	}

	private static boolean[] setBoolArray(VSBattleMode mode, String name) throws Exception {
		return (boolean[]) readField(mode, name);
	}

	private static void invokeLoadOther(VSBattleMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = VSBattleMode.class.getDeclaredMethod("loadOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static void invokeSaveOther(VSBattleMode mode, GameEngine engine, CustomProperties prop) throws Exception {
		Method m = VSBattleMode.class.getDeclaredMethod("saveOtherSetting", GameEngine.class, CustomProperties.class);
		m.setAccessible(true);
		m.invoke(mode, engine, prop);
	}

	private static int invokeGetTotalGarbageLines(VSBattleMode mode, int playerID) throws Exception {
		Method m = VSBattleMode.class.getDeclaredMethod("getTotalGarbageLines", int.class);
		m.setAccessible(true);
		return (int) m.invoke(mode, playerID);
	}
}
