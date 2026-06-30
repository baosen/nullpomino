package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers the remaining uncovered line in {@link AvalancheVSDigRaceMode}:
 *
 * <ul>
 *   <li>Line 500 – {@code lineClearEnd}: when {@code dangerColumnDouble[playerID]}
 *       is {@code true} and column 3 row 0 is occupied the game-over branch is
 *       reached via the second condition of the {@code if}.</li>
 * </ul>
 */
class AvalancheVSDigRaceModeTailCoverage2Test {

    /**
     * When {@code dangerColumnDouble[0]} is {@code true} and column 3, row 0
     * contains a block the condition at line 499–500 is satisfied and the engine
     * transitions to GAMEOVER.  Column 2, row 0 is deliberately kept empty so
     * only the second operand (line 500) triggers the result.
     */
    @Test
    void lineClearEndGameOverWhenDangerColumnDoubleAndCol3Blocked() throws Exception {
        AvalancheVSDigRaceMode mode = new AvalancheVSDigRaceMode();
        GameManager manager = twoEngineManager(mode);
        GameEngine engine = manager.engine[0];
        engine.createFieldIfNeeded();

        // No garbage to drop and not cleared, so we reach the game-over check
        setIntArray(mode, "ojama", 0, 0);
        setIntArray(mode, "ojamaAdd", 0, 1);
        setBoolArray(mode, "ojamaDrop", false, 0);
        setBoolArray(mode, "cleared", false, 0);

        // Enable double-column danger check
        setBoolArray(mode, "dangerColumnDouble", true, 0);

        // col 2 empty, col 3 blocked → line 500 fires the GAMEOVER
        engine.field.setBlock(3, 0, new Block(Block.BLOCK_COLOR_GRAY));

        mode.lineClearEnd(engine, 0);

        assertEquals(GameEngine.Status.GAMEOVER, engine.stat,
                "dangerColumnDouble + col3 blocked should trigger GAMEOVER (line 500)");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    static class RedirectingReceiver extends EventReceiver {
        @Override
        public void saveModeConfig(CustomProperties modeConfig) {
            try {
                modeConfig.storeToFile(
                        System.getProperty("java.io.tmpdir") + "/avsvsdig2-test-mode.cfg",
                        "test");
            } catch (Exception ignored) {
            }
        }

        @Override
        public boolean saveProperties(String filename, CustomProperties prop) {
            try {
                prop.storeToFile(
                        System.getProperty("java.io.tmpdir") + "/avsvsdig2-test-redirect.cfg",
                        "test");
            } catch (Exception ignored) {
            }
            return true;
        }
    }

    private static GameManager twoEngineManager(AvalancheVSDigRaceMode mode) throws Exception {
        GameManager manager = new GameManager(new RedirectingReceiver());
        manager.replayMode = false;
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[1].init();
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);
        mode.playerInit(manager.engine[1], 1);
        manager.engine[0].owner.replayMode = false;
        manager.engine[1].owner.replayMode = false;
        return manager;
    }

    private static void setIntArray(Object obj, String name, int value, int index)
            throws Exception {
        ((int[]) findField(obj.getClass(), name).get(obj))[index] = value;
    }

    private static void setBoolArray(Object obj, String name, boolean value, int index)
            throws Exception {
        ((boolean[]) findField(obj.getClass(), name).get(obj))[index] = value;
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                Field f = c.getDeclaredField(name);
                f.setAccessible(true);
                return f;
            } catch (NoSuchFieldException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
