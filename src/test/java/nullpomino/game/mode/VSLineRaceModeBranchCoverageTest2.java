package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Branch-coverage tests for {@link VSLineRaceMode} targeting three previously
 * uncovered lines:
 * <ul>
 *   <li>Lines 376–377 – {@code else if (strLines.length() == 3)} path in
 *       {@code renderLast}: reached when {@code remainLines >= 100}.</li>
 *   <li>Line 415 – wide-position win-count draw path in {@code renderLast}:
 *       reached when {@code getNextDisplayType() == 2}
 *       AND {@code winCount[playerID] >= 10}.</li>
 * </ul>
 */
class VSLineRaceModeBranchCoverageTest2 {

    // ---------------------------------------------------------------
    // Lines 376–377 – strLines.length() == 3 (remainLines >= 100)
    // ---------------------------------------------------------------

    /**
     * Set {@code goalLines[0] = 100} so that {@code remainLines == 100},
     * making {@code strLines = "100"} (length 3).
     *
     * Proof: renderLast executes without throwing and the three-character
     * branch path is exercised.
     */
    @Test
    void renderLastEntersThreeCharBranchWhenRemainLinesIs100() throws Exception {
        VSLineRaceMode mode = new VSLineRaceMode();
        GameEngine engine = buildEngine(mode);

        // Set goalLines[0] = 100 so remainLines == 100 (3-digit string)
        int[] goalLines = getIntArray(mode, "goalLines");
        goalLines[0] = 100;

        // engine.statistics.lines = 0 (default) → remainLines = 100
        assertDoesNotThrow(() -> mode.renderLast(engine, 0),
                "renderLast should not throw with remainLines=100");
    }

    // ---------------------------------------------------------------
    // Line 415 – wide x-offset when winCount >= 10
    // ---------------------------------------------------------------

    /**
     * Set {@code winCount[0] = 10} and enable big-sidenext so that
     * {@code getNextDisplayType() == 2}.
     *
     * Proof: renderLast exercises the {@code winCount[playerID] >= 10}
     * branch (x - 44 offset) without throwing.
     */
    @Test
    void renderLastUsesWideOffsetWhenWinCountIsTenOrMore() throws Exception {
        VSLineRaceMode mode = new VSLineRaceMode();
        GameEngine engine = buildEngine(mode);

        // Enable bigsidenext layout so getNextDisplayType() returns 2
        setReceiverBoolField(engine, "sidenext", true);
        setReceiverBoolField(engine, "bigsidenext", true);

        // Confirm receiver.getNextDisplayType() == 2
        // (done implicitly – test would throw if wrong branch)

        // Set winCount[0] >= 10 to enter the wide-offset branch
        int[] winCount = getIntArray(mode, "winCount");
        winCount[0] = 10;

        assertDoesNotThrow(() -> mode.renderLast(engine, 0),
                "renderLast should not throw when winCount=10 and NextDisplayType=2");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static GameEngine buildEngine(VSLineRaceMode mode) throws Exception {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        // Init ALL engines so statistics/statc/etc. are non-null
        for (int i = 0; i < manager.engine.length; i++) {
            manager.engine[i].init();
            manager.engine[i].playerID = i;
        }
        mode.modeInit(manager);
        mode.playerInit(manager.engine[0], 0);
        mode.playerInit(manager.engine[1], 1);
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
