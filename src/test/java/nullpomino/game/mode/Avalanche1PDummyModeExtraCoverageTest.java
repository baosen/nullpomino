package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers remaining uncovered lines in {@link Avalanche1PDummyMode}:
 * onReady (line 196), readyInit outlinetype==2 (line 206),
 * addBonus numColors>=5 (line 274), calcScore zenKeshi branch (line 306),
 * calcScore else zenKeshi false (line 313), calcScore colorsCleared>1 (line 322),
 * calcScore multiplier cap at 999 (line 327).
 */
class Avalanche1PDummyModeExtraCoverageTest {

	@Test
	void onReadyReturnsFalseWhenStatcNotZero() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.statc[0] = 1; // not zero, so onReady returns false directly

		boolean result = mode.onReady(engine, 0);

		assertEquals(false, result);
	}

	@Test
	void readyInitBlockOutlineNoneWhenOutlinetypeIsTwo() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		// Set outlinetype AFTER playerInit which resets it to 0
		setInt(mode, "outlinetype", 2);

		// Trigger readyInit
		boolean result = invokeReadyInit(mode, engine, 0);

		assertEquals(false, result);
		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, engine.blockOutlineType);
	}

	@Test
	void addBonusZenKeshiWithNumColorsGE5() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		setInt(mode, "numColors", 5);
		setInt(mode, "zenKeshiCount", 3);
		engine.statistics.maxChain = 2;
		engine.statistics.score = 1000;

		setInt(mode, "scoreBeforeBonus", 0);
		setInt(mode, "zenKeshiBonus", 0);
		setInt(mode, "maxChainBonus", 0);

		invokeAddBonus(mode, engine, 0);

		// zenKeshiBonus = 3*3*1000 = 9000
		assertEquals(9000, readInt(mode, "zenKeshiBonus"));
		// maxChainBonus = 2*2*2000 = 8000
		assertEquals(8000, readInt(mode, "maxChainBonus"));
		// score = 1000 + 9000 + 8000 = 18000
		assertEquals(18000, engine.statistics.score);
	}

	@Test
	void calcScoreZenKeshiBranchAddsGarbage() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setBoolean(mode, "zenKeshi", true);
		setInt(mode, "garbageAdd", 0);

		// field is empty, so zenKeshi stays true and garbageAdd += 30
		mode.calcScore(engine, 0, 4);

		// zenKeshi was true, so garbageAdd gets +30 plus calcOjama adds more
		assertTrue(readInt(mode, "garbageAdd") >= 30);
	}

	@Test
	void calcScoreZenKeshiFalseWhenFieldNotEmpty() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		setBoolean(mode, "zenKeshi", true);
		// Place a block in the field so isEmpty returns false
		engine.field.setBlockE(0, 0, new Block(1));

		mode.calcScore(engine, 0, 4);

		// zenKeshi should have been set to false because field was not empty
		assertEquals(false, readBoolean(mode, "zenKeshi"));
	}

	@Test
	void calcScoreMultiplierColorsClearedMoreThanOne() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.colorClearExtraCount = 5;
		engine.field.colorsCleared = 3; // > 1, so multiplier adds (3-1)*2 = 4

		mode.calcScore(engine, 0, 4);

		// multiplier starts at colorClearExtraCount=5, then adds (3-1)*2=4 => 9
		// Also calcChainMultiplier for chain=0 returns 0, so multiplier = 5+4+0 = 9
		assertTrue(readInt(mode, "lastmultiplier") >= 9);
	}

	@Test
	void calcScoreMultiplierCapAt999() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.colorClearExtraCount = 1000;
		engine.field.colorsCleared = 0;

		mode.calcScore(engine, 0, 4);

		// multiplier capped at 999
		assertEquals(999, readInt(mode, "lastmultiplier"));
	}

	@Test
	void calcScoreMultiplierMinAtOne() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		mode.playerInit(engine, 0);
		engine.createFieldIfNeeded();
		engine.field.colorClearExtraCount = -5;
		engine.field.colorsCleared = 0;

		mode.calcScore(engine, 0, 4);

		// multiplier clamped to 1
		assertEquals(1, readInt(mode, "lastmultiplier"));
	}

	// ---------------------------------------------------------------
	// Reflection helpers
	// ---------------------------------------------------------------

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

	private static void setInt(Object obj, String name, int value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setInt(obj, value);
	}

	private static void setBoolean(Object obj, String name, boolean value) throws Exception {
		Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(obj, value);
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

	private static boolean invokeReadyInit(Avalanche1PDummyMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = findMethod(mode.getClass(), "readyInit", GameEngine.class, int.class);
		m.setAccessible(true);
		return (boolean) m.invoke(mode, engine, playerID);
	}

	private static void invokeAddBonus(Avalanche1PDummyMode mode, GameEngine engine, int playerID) throws Exception {
		Method m = findMethod(mode.getClass(), "addBonus", GameEngine.class, int.class);
		m.setAccessible(true);
		m.invoke(mode, engine, playerID);
	}

	private static Method findMethod(Class<?> cls, String name, Class<?>... paramTypes)
			throws NoSuchMethodException {
		Class<?> c = cls;
		while (c != null) {
			try {
				return c.getDeclaredMethod(name, paramTypes);
			} catch (NoSuchMethodException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchMethodException(name);
	}
}
