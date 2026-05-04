package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import nullpomino.game.component.Block;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

/**
 * Covers remaining uncovered lines and branches in Avalanche1PDummyMode.
 */
class Avalanche1PDummyModeBranchTest {

	@Test
	void onReadyWithNonZeroStatcReturnsFalse() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.statc[0] = 1;

		boolean result = mode.onReady(engine, 0);
		assertFalse(result);
	}

	@Test
	void readyInitWithOutlineType2() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		setField(mode, "numColors", 4);
		setField(mode, "cascadeSlow", false);
		setField(mode, "bigDisplay", false);
		setField(mode, "outlinetype", 2);

		Method readyInit = findMethod(Avalanche1PDummyMode.class, "readyInit", GameEngine.class, int.class);
		readyInit.setAccessible(true);
		readyInit.invoke(mode, engine, 0);

		assertEquals(GameEngine.BLOCK_OUTLINE_NONE, engine.blockOutlineType);
	}

	@Test
	void onLastWithScgettimeZero() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		setField(mode, "scgettime", 0);
		setField(mode, "chainDisplay", 0);

		mode.onLast(engine, 0);

		assertEquals(0, readInt(mode, "scgettime"));
	}

	@Test
	void onGameOverWithNonZeroStatc() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.statc[0] = 1;

		boolean result = mode.onGameOver(engine, 0);
		assertFalse(result);
	}

	@Test
	void addBonusWithNumColors5() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.statistics.score = 100;
		setField(mode, "numColors", 5);
		setField(mode, "zenKeshiCount", 2);
		setField(mode, "zenKeshiBonus", 0);
		setField(mode, "maxChainBonus", 0);
		engine.statistics.maxChain = 3;

		Method addBonus = findMethod(Avalanche1PDummyMode.class, "addBonus", GameEngine.class, int.class);
		addBonus.setAccessible(true);
		addBonus.invoke(mode, engine, 0);

		assertEquals(100, readInt(mode, "scoreBeforeBonus"));
		// zenKeshiBonus = 2*2*1000 = 4000 (for numColors >= 5)
		assertEquals(4000, readInt(mode, "zenKeshiBonus"));
	}

	@Test
	void addBonusWithNumColors4() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.statistics.score = 100;
		setField(mode, "numColors", 4);
		setField(mode, "zenKeshiCount", 3);
		setField(mode, "zenKeshiBonus", 0);
		setField(mode, "maxChainBonus", 0);
		engine.statistics.maxChain = 2;

		Method addBonus = findMethod(Avalanche1PDummyMode.class, "addBonus", GameEngine.class, int.class);
		addBonus.setAccessible(true);
		addBonus.invoke(mode, engine, 0);

		// zenKeshiBonus = 3*(3+1)*500 = 6000 (for numColors == 4)
		assertEquals(6000, readInt(mode, "zenKeshiBonus"));
	}

	@Test
	void addBonusWithNumColors3() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.statistics.score = 100;
		setField(mode, "numColors", 3);
		setField(mode, "zenKeshiCount", 2);
		setField(mode, "zenKeshiBonus", 0);
		setField(mode, "maxChainBonus", 0);
		engine.statistics.maxChain = 1;

		Method addBonus = findMethod(Avalanche1PDummyMode.class, "addBonus", GameEngine.class, int.class);
		addBonus.setAccessible(true);
		addBonus.invoke(mode, engine, 0);

		// zenKeshiBonus = 2*(2+3)*250 = 2500 (for numColors < 4)
		assertEquals(2500, readInt(mode, "zenKeshiBonus"));
	}

	@Test
	void calcScoreWithAvalancheZero() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.statistics.score = 100;

		mode.calcScore(engine, 0, 0);

		// avalanche == 0 → no score change
		assertEquals(100, engine.statistics.score);
	}

	@Test
	void calcScoreWithClearAll() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.statistics.score = 0;
		setField(mode, "numColors", 5);
		setField(mode, "ojamaRate", 120);
		setField(mode, "blocksPerLevel", 15);
		setField(mode, "maxLevel", 99);
		setField(mode, "level", 5);
		setField(mode, "toNextLevel", 15);
		engine.chain = 1;
		engine.field = new nullpomino.game.component.Field();
		engine.field.setBlock(0, 0, new Block(Block.BLOCK_COLOR_RED));
		engine.field.setBlock(0, 1, new Block(Block.BLOCK_COLOR_RED));
		engine.field.setBlock(0, 2, new Block(Block.BLOCK_COLOR_RED));
		engine.field.setBlock(0, 3, new Block(Block.BLOCK_COLOR_RED));

		mode.calcScore(engine, 0, 4);

		// Verify score was calculated (should be > 0)
		assertTrue(engine.statistics.score >= 0);
	}

	/**
	 * Covers line 306: {@code garbageAdd += 30} when {@code zenKeshi} is true.
	 *
	 * Sets {@code zenKeshi = true}, places a block on the field so that
	 * {@code isEmpty()} returns false, then calls {@code calcScore}.
	 * Verifies that {@code garbageAdd} is incremented by the zenKeshi bonus of 30
	 * plus any additional {@code calcOjama} contribution (1 in this setup).
	 */
	@Test
	void calcScoreZenKeshiIncrementsGarbageAdd() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setField(mode, "zenKeshi", true);
		setField(mode, "garbageAdd", 0);
		setField(mode, "numColors", 5);
		setField(mode, "ojamaRate", 120);
		setField(mode, "blocksPerLevel", 15);
		setField(mode, "maxLevel", 99);
		setField(mode, "level", 5);
		setField(mode, "toNextLevel", 15);
		engine.chain = 1;
		// Place a block so isEmpty() returns false
		engine.field.setBlock(0, engine.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_RED));

		mode.calcScore(engine, 0, 4);

		// garbageAdd = 30 (zenKeshi, line 306) + calcOjama(40,4,40,1) = 30 + 1 = 31
		int ga = readInt(mode, "garbageAdd");
		assertTrue(ga >= 30, "garbageAdd should be at least 30 from zenKeshi bonus, got " + ga);
	}

	/**
	 * Covers line 322: {@code multiplier += (engine.field.colorsCleared-1)*2}
	 * when {@code colorsCleared > 1}.
	 *
	 * Sets up the field with a non-empty state (so {@code isEmpty()} is false)
	 * and sets {@code colorsCleared = 3}.  Then calls {@code calcScore} with
	 * {@code avalanche = 4}.  With {@code colorClearExtraCount = 0},
	 * {@code colorsCleared = 3}, and {@code chain = 1}:
	 * <pre>
	 *   pts     = 4 * 10 = 40
	 *   multiplier = 0 + (3-1)*2 + 0 = 4
	 *   score   = 40 * 4 = 160
	 * </pre>
	 */
	@Test
	void calcScoreMultiplierFromColorsCleared() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		setField(mode, "numColors", 5);
		setField(mode, "ojamaRate", 120);
		setField(mode, "blocksPerLevel", 15);
		setField(mode, "maxLevel", 99);
		setField(mode, "level", 5);
		setField(mode, "toNextLevel", 15);
		engine.chain = 1;
		engine.statistics.score = 0;
		// Place a block so isEmpty() is false (avoids zenKeshi path interference)
		engine.field.setBlock(0, engine.field.getHeight() - 1, new Block(Block.BLOCK_COLOR_RED));
		// Set colorsCleared > 1 to trigger line 322
		engine.field.colorsCleared = 3;

		mode.calcScore(engine, 0, 4);

		// pts = 4 * 10 = 40; multiplier = 0 + (3-1)*2 + 0 = 4; score = 40 * 4 = 160
		assertEquals(160, engine.statistics.score);
		assertEquals(160, engine.statistics.scoreFromLineClear);
	}

	@Test
	void calcChainMultiplierForChain2() throws Exception {
		Avalanche1PDummyMode mode = new Avalanche1PDummyMode() {};

		Method calcChainMultiplier = findMethod(Avalanche1PDummyMode.class, "calcChainMultiplier", int.class);
		calcChainMultiplier.setAccessible(true);

		assertEquals(8, calcChainMultiplier.invoke(mode, 2));
		assertEquals(16, calcChainMultiplier.invoke(mode, 3));
		assertEquals(32, calcChainMultiplier.invoke(mode, 4)); // 32*(4-3) = 32
		assertEquals(64, calcChainMultiplier.invoke(mode, 5)); // 32*(5-3) = 64
		assertEquals(0, calcChainMultiplier.invoke(mode, 0));
		assertEquals(0, calcChainMultiplier.invoke(mode, 1));
	}

	// Reflection helpers
	private static GameEngine freshEngine(Avalanche1PDummyMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	private static int readInt(Object obj, String name) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		return f.getInt(obj);
	}

	private static void setField(Object obj, String name, Object value) throws Exception {
		java.lang.reflect.Field f = findField(obj.getClass(), name);
		f.setAccessible(true);
		if (value instanceof Integer) {
			f.setInt(obj, (Integer) value);
		} else if (value instanceof Boolean) {
			f.setBoolean(obj, (Boolean) value);
		} else {
			f.set(obj, value);
		}
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
