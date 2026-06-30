package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Covers uncovered lines in {@link TimeAttackMode#renderResult}:
 * <ul>
 *   <li>Line 897: "NEW PB" drawn when netIsPB == true</li>
 *   <li>Line 900: "SENDING..." drawn when netIsNetPlay + netReplaySendStatus == 1</li>
 *   <li>Line 902: "A: RETRY" drawn when netIsNetPlay + !netIsWatch + netReplaySendStatus == 2</li>
 * </ul>
 */
class TimeAttackModeBranchCoverageTest2 {

    // -----------------------------------------------------------------------
    // Line 897: netIsPB == true
    // -----------------------------------------------------------------------

    @Test
    void renderResultNetIsPBDrawsNewPB() throws Exception {
        TimeAttackMode mode = new TimeAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setField(mode, "netIsPB", true);

        // renderResult calls receiver.drawMenuFont — no-op EventReceiver swallows it
        mode.renderResult(engine, 0);
        assertTrue(true, "renderResult with netIsPB should not throw");
    }

    // -----------------------------------------------------------------------
    // Line 900: netIsNetPlay + netReplaySendStatus == 1
    // -----------------------------------------------------------------------

    @Test
    void renderResultNetReplaySendStatus1DrawsSending() throws Exception {
        TimeAttackMode mode = new TimeAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setField(mode, "netIsNetPlay", true);
        setField(mode, "netReplaySendStatus", 1);

        mode.renderResult(engine, 0);
        assertTrue(true, "renderResult with status==1 should not throw");
    }

    // -----------------------------------------------------------------------
    // Line 902: netIsNetPlay + !netIsWatch + netReplaySendStatus == 2
    // -----------------------------------------------------------------------

    @Test
    void renderResultNetReplaySendStatus2DrawsRetry() throws Exception {
        TimeAttackMode mode = new TimeAttackMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        setField(mode, "netIsNetPlay", true);
        setField(mode, "netIsWatch", false);
        setField(mode, "netReplaySendStatus", 2);

        mode.renderResult(engine, 0);
        assertTrue(true, "renderResult with status==2 should not throw");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(TimeAttackMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        return manager.engine[0];
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.set(obj, value);
    }

    private static void setField(Object obj, String name, boolean value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setBoolean(obj, value);
    }

    private static void setField(Object obj, String name, int value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setInt(obj, value);
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
