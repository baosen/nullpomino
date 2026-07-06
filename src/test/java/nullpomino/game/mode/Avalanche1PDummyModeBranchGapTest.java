package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Closes the remaining branch gaps in {@link Avalanche1PDummyMode}:
 * <ul>
 * <li>L202 readyInit with bigDisplay enabled</li>
 * <li>L206 readyInit with an outline type outside 0-2 (no arm taken)</li>
 * <li>L333 calcScore level-up guard when already at max level</li>
 * </ul>
 */
class Avalanche1PDummyModeBranchGapTest {

	private static final class GapAvalancheMode extends Avalanche1PDummyMode {
	}

	private static GameEngine fresh(Avalanche1PDummyMode mode) {
		GameManager manager = new GameManager(new EventReceiver());
		manager.mode = mode;
		manager.init();
		manager.engine[0].init();
		return manager.engine[0];
	}

	@Test
	void readyInitBigDisplaySetsDoubleSizeField() {
		GapAvalancheMode mode = new GapAvalancheMode();
		GameEngine engine = fresh(mode);

		mode.bigDisplay = true;
		mode.readyInit(engine, 0);
		assertEquals(1, engine.displaysize);

		mode.bigDisplay = false;
		mode.readyInit(engine, 0);
		assertEquals(0, engine.displaysize);
	}

	@Test
	void readyInitUnknownOutlineTypeLeavesOutlineUntouched() {
		GapAvalancheMode mode = new GapAvalancheMode();
		GameEngine engine = fresh(mode);

		engine.blockOutlineType = GameEngine.BLOCK_OUTLINE_SAMECOLOR; // sentinel
		mode.outlinetype = 3; // no if/else-if arm matches
		mode.readyInit(engine, 0);

		assertEquals(GameEngine.BLOCK_OUTLINE_SAMECOLOR, engine.blockOutlineType);
	}

	@Test
	void calcScoreAtMaxLevelDoesNotLevelUp() {
		GapAvalancheMode mode = new GapAvalancheMode();
		GameEngine engine = fresh(mode);
		engine.createFieldIfNeeded();
		engine.chain = 1;

		mode.level = 99;
		mode.maxLevel = 99;
		mode.toNextLevel = 1;

		mode.calcScore(engine, 0, 3); // toNextLevel goes negative but level is capped

		assertEquals(99, mode.level, "level must not pass maxLevel");
		assertTrue(mode.toNextLevel <= 0, "counter must not be reset when level is capped");
	}
}
