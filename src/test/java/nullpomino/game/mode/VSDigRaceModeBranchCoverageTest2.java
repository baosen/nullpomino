package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link VSDigRaceMode} targeting the following
 * uncovered lines:
 * <ul>
 *   <li>Lines 471–472 – {@code strLines.length() == 3} branch in
 *       {@code renderLast}: DEAD CODE — {@code getRemainGarbageLines}
 *       is capped by {@code goalLines[playerID]} which has a maximum
 *       of 18, so {@code remainLines} can never reach 100.  These lines
 *       are unreachable; we document and skip them.</li>
 *   <li>Line 513 – wide-position win-count draw in {@code renderLast}:
 *       reached when {@code getNextDisplayType() == 2}
 *       AND {@code !owner.replayMode}
 *       AND {@code winCount[playerID] >= 10}.</li>
 * </ul>
 */
class VSDigRaceModeBranchCoverageTest2 {

    // Lines 471-472: DEAD CODE – remainLines max is 18 (goalLines max), so
    // strLines.length() can never equal 3. Skip.

    // ---------------------------------------------------------------
    // Line 513 – wide x-offset when winCount >= 10
    // ---------------------------------------------------------------

    /**
     * Set {@code winCount[0] = 10} and enable big-sidenext so that
     * {@code getNextDisplayType() == 2}.
     *
     * Proof: renderLast executes the wide-offset draw path (line 513) without
     * throwing.
     */
    @Test
    void renderLastUsesWideOffsetWhenWinCountIsTenOrMore() throws Exception {
        VSDigRaceMode mode = new VSDigRaceMode();
        GameEngine engine = buildEngine(mode);

        // Enable bigsidenext layout so getNextDisplayType() == 2
        setReceiverBoolField(engine, "sidenext", true);
        setReceiverBoolField(engine, "bigsidenext", true);

        // Set winCount[0] = 10 to enter the wide-offset branch (line 513)
        int[] winCount = getIntArray(mode, "winCount");
        winCount[0] = 10;

        assertDoesNotThrow(() -> mode.renderLast(engine, 0),
                "renderLast should not throw when winCount=10 and NextDisplayType=2");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static GameEngine buildEngine(VSDigRaceMode mode) throws Exception {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);
        manager.engine[0].createFieldIfNeeded();
        return manager.engine[0];
    }

    private static int[] getIntArray(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return (int[]) f.get(obj);
    }

    private static void setReceiverBoolField(GameEngine engine, String name, boolean value)
            throws Exception {
        Field f = findField(engine.owner.receiver.getClass(), name);
        f.setAccessible(true);
        f.setBoolean(engine.owner.receiver, value);
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
}
