package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertFalse;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers the remaining uncovered line in {@link AvalancheVSDummyMode}:
 *
 * <ul>
 *   <li>Line 491 – {@code onReady}: when {@code engine.statc[0] != 0} the method
 *       returns {@code false} without calling {@code readyInit}.</li>
 * </ul>
 */
class AvalancheVSDummyModeTailCoverage2Test {

    /**
     * When {@code statc[0] != 0} the {@code onReady} guard is false and the
     * method takes the {@code return false} branch at line 491.
     * {@link AvalancheVSFeverMode} inherits {@code onReady} from
     * {@link AvalancheVSDummyMode} (it does not override it), so this test
     * exercises the line in its declaring class.
     */
    @Test
    void onReadyReturnsFalseWhenStatc0NotZero() throws Exception {
        // AvalancheVSDummyMode is abstract; AvalancheVSFeverMode does NOT override
        // onReady, so it inherits the method from AvalancheVSDummyMode and covers it.
        AvalancheVSFeverMode mode = new AvalancheVSFeverMode();
        GameManager manager = new GameManager(new RedirectingReceiver());
        manager.replayMode = false;
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[1].init();
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);
        mode.playerInit(manager.engine[1], 1);

        GameEngine engine = manager.engine[0];
        // Any value other than 0 makes the guard fail → line 491 executes
        engine.statc[0] = 1;

        boolean result = mode.onReady(engine, 0);

        assertFalse(result, "onReady should return false when statc[0] != 0 (line 491)");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    static class RedirectingReceiver extends EventReceiver {
        @Override
        public void saveModeConfig(CustomProperties modeConfig) {
            try {
                modeConfig.storeToFile(
                        System.getProperty("java.io.tmpdir") + "/avsvsdummy2-test-mode.cfg",
                        "test");
            } catch (Exception ignored) {
            }
        }

        @Override
        public boolean saveProperties(String filename, CustomProperties prop) {
            try {
                prop.storeToFile(
                        System.getProperty("java.io.tmpdir") + "/avsvsdummy2-test-redirect.cfg",
                        "test");
            } catch (Exception ignored) {
            }
            return true;
        }
    }
}
