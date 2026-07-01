package nullpomino.game.play;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import nullpomino.game.event.EventReceiver;

/**
 * Covers remaining uncovered lines and branches in GameManager.
 *
 * <p>The commit-hash / .git-ref branches are covered by
 * {@link GameManagerCommitHashEdgeTest} against a throwaway {@code @TempDir}
 * (via {@link GameManager#resolveCommitHash(java.nio.file.Path)} /
 * {@link GameManager#readRef(java.nio.file.Path, String)}), so they are no longer
 * duplicated here — the previous versions wrote to the real {@code .git} and
 * could corrupt the repository when run outside Bazel's sandbox.
 */
class GameManagerExtraTest {

	/**
	 * Test init() with modeConfig == null path.
	 */
	@Test
	void initSetsModeConfigWhenReceiverReturnsNull() {
		EventReceiver receiver = new EventReceiver() {
			@Override
			public nullpomino.util.CustomProperties loadModeConfig() {
				return null;
			}
		};
		GameManager gm = new GameManager(receiver);
		gm.init();

		assertNotNull(gm.modeConfig);
	}

	/**
	 * Test getQuitFlag with null engine slot.
	 */
	@Test
	void getQuitFlagWithNullEngineSlot() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0] = null;

		assertFalse(gm.getQuitFlag());
	}

	/**
	 * Test isGameActive with null engine slot.
	 */
	@Test
	void isGameActiveWithNullEngineSlot() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.init();
		gm.engine[0] = null;

		assertFalse(gm.isGameActive());
	}

	/**
	 * Test that init properly handles re-init path.
	 */
	@Test
	void initReinitializesEngines() {
		GameManager gm = new GameManager(new EventReceiver());
		gm.replayProp = new nullpomino.util.CustomProperties();
		gm.init();

		assertFalse(gm.replayMode);
		assertEquals(1, gm.engine.length);
	}
}
