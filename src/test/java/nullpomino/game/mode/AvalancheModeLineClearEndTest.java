package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Pins {@link AvalancheMode#lineClearEnd}'s game-over check. After a
 * cleared chain finishes, the mode inspects the danger column(s) at
 * the top of the field: column 2 is always checked, and column 3 is
 * only checked when {@code dangerColumnDouble} is on. If either
 * danger column has a block at row 0, the mode forces the engine
 * into GAMEOVER status (with statc[1]=1 so the death animation
 * starts properly).
 */
class AvalancheModeLineClearEndTest {

	@Test
	void emptyDangerColumnsReturnFalseAndDontTriggerGameOver() {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		// Both column 2 row 0 and column 3 row 0 left empty.

		boolean result = mode.lineClearEnd(engine, 0);

		assertFalse(result, "lineClearEnd always returns false");
		assertEquals(GameEngine.Status.SETTING, engine.stat,
				"empty danger columns -> stat unchanged from initial SETTING");
	}

	@Test
	void nullFieldReturnsFalseWithoutThrowing() {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.field = null;

		boolean result = mode.lineClearEnd(engine, 0);

		assertFalse(result,
				"null field -> short-circuit, no NPE");
	}

	@Test
	void blockInColumnTwoRowZeroForcesGameOver() {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.field.setBlockColor(2, 0, Block.BLOCK_COLOR_RED);

		boolean result = mode.lineClearEnd(engine, 0);

		assertFalse(result);
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat,
				"block at (2, 0) -> GAMEOVER (avalanche danger column hit)");
		assertEquals(1, engine.statc[1],
				"statc[1] = 1 so the death animation starts");
	}

	@Test
	void blockInColumnThreeRowZeroAlsoTriggersGameOverWhenDangerColumnDoubleOn() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "dangerColumnDouble", true);
		engine.createFieldIfNeeded();
		// Column 2 stays empty; column 3 has a block.
		engine.field.setBlockColor(3, 0, Block.BLOCK_COLOR_BLUE);

		boolean result = mode.lineClearEnd(engine, 0);

		assertFalse(result);
		assertEquals(GameEngine.Status.GAMEOVER, engine.stat,
				"dangerColumnDouble + block at (3, 0) -> GAMEOVER");
	}

	@Test
	void blockInColumnThreeAloneSurvivesWhenDangerColumnDoubleOff() throws Exception {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		setBoolean(mode, "dangerColumnDouble", false);
		engine.createFieldIfNeeded();
		engine.field.setBlockColor(3, 0, Block.BLOCK_COLOR_BLUE);

		boolean result = mode.lineClearEnd(engine, 0);

		assertFalse(result);
		assertEquals(GameEngine.Status.SETTING, engine.stat,
				"dangerColumnDouble=false ignores column 3 -> stat unchanged");
	}

	@Test
	void gameOverPathClearsStatcZeroAndKeepsStatcOneAtOne() {
		AvalancheMode mode = new AvalancheMode();
		GameEngine engine = freshEngine(mode);
		engine.createFieldIfNeeded();
		engine.field.setBlockColor(2, 0, Block.BLOCK_COLOR_RED);
		// Spoil statc[0] so we can verify resetStatc cleared it.
		engine.statc[0] = 99;

		mode.lineClearEnd(engine, 0);

		assertEquals(0, engine.statc[0],
				"resetStatc cleared statc[0]");
		assertEquals(1, engine.statc[1],
				"statc[1]=1 forced after the reset");
	}

	private static GameEngine freshEngine(AvalancheMode mode) {
		GameManager gm = new GameManager(new EventReceiver());
		gm.mode = mode;
		gm.init();
		gm.engine[0].init();
		return gm.engine[0];
	}

	private static void setBoolean(Object instance, String name, boolean value)
			throws Exception {
		Field f = findField(instance.getClass(), name);
		f.setAccessible(true);
		f.setBoolean(instance, value);
	}

	private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
		Class<?> c = cls;
		while(c != null) {
			try {
				return c.getDeclaredField(name);
			} catch(NoSuchFieldException e) {
				c = c.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
