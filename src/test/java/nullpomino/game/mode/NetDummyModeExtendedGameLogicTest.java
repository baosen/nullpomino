package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link NetDummyMode}: covers getName,
 * modeInit allocation and flag initialization, playerInit command,
 * netPlayerInit state reset, onMove field-sending thresholds,
 * pieceLocked and onLineClear triggers, no-op stubs, and loadRanking.
 */
class NetDummyModeExtendedGameLogicTest {

    @Test
    void getNameReturnsExpected() {
        NetDummyMode mode = new NetDummyMode();
        assertEquals("NET-DUMMY", mode.getName());
    }

    @Test
    void modeInitSetsOwnerAndAllocatesRankings() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);

        assertNotNull(readField(mode, "owner"));
        assertFalse(readBoolean(mode, "netIsNetPlay"));
        assertFalse(readBoolean(mode, "netIsWatch"));
        assertEquals(0, readInt(mode, "netNumSpectators"));

        // Ranking arrays
        assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingPlace")).length);
        assertEquals(2, ((java.util.LinkedList[]) readField(mode, "netRankingScore")).length);
    }

    @Test
    void playerInitStopsEngineAndHides() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        engine.stat = GameEngine.Status.MOVE;
        engine.isVisible = true;

        mode.playerInit(engine, 0);

        assertEquals(GameEngine.Status.NOTHING, engine.stat);
        assertFalse(engine.isVisible);
    }

    @Test
    void netPlayerInitResetsState() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        Method netPlayerInit = Method.getDeclaredMethod(NetDummyMode.class,
                "netPlayerInit", GameEngine.class, int.class);
        netPlayerInit.invoke(mode, engine, 0);

        assertEquals(-1, readInt(mode, "netPrevPieceID"));
        assertEquals(0, readInt(mode, "netPrevPieceX"));
        assertEquals(0, readInt(mode, "netPrevPieceY"));
        assertEquals(0, readInt(mode, "netPrevPieceDir"));
        assertEquals(0, readInt(mode, "netPlayerSkin"));
        assertEquals(0, readInt(mode, "netReplaySendStatus"));

        int[] netRankingRank = (int[]) readField(mode, "netRankingRank");
        assertEquals(-1, netRankingRank[0]);
        assertEquals(-1, netRankingRank[1]);

        assertFalse(readBoolean(mode, "netIsPB"));
    }

    @Test
    void onMoveDoesNotCrashWhenNotNetplay() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        // When netIsNetPlay is false, onMove should return false
        boolean result = mode.onMove(engine, 0);
        assertFalse(result);
    }

    @Test
    void onMoveReturnsTrueWhenWatchMode() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        setBoolean(mode, "netIsWatch", true);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        boolean result = mode.onMove(engine, 0);
        assertTrue(result);
    }

    @Test
    void pieceLockedDoesNotThrowWhenNotNetplay() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.pieceLocked(engine, 0, 0);
        // Should not throw
    }

    @Test
    void onLineClearDoesNotThrowWhenNotNetplay() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        boolean result = mode.onLineClear(engine, 0);
        assertFalse(result);
    }

    @Test
    void onAREDoesNotThrowWhenNotNetplay() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        boolean result = mode.onARE(engine, 0);
        assertFalse(result);
    }

    @Test
    void onEndingStartDoesNotThrow() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.onEndingStart(engine, 0);
        // Should not throw
    }

    @Test
    void onExcellentDoesNotThrowWhenNotNetplay() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.onExcellent(engine, 0);
        // Should not throw
    }

    @Test
    void onGameOverDoesNotThrowWhenNotNetplay() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.onGameOver(engine, 0);
        // Should not throw
    }

    @Test
    void noOpStubsDoNotThrow() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.netSendStats(engine);
        mode.netRecvStats(engine, new String[]{});
        mode.netSendEndGameStats(engine);
        mode.netSendOptions(engine);
        mode.netRecvOptions(engine, new String[]{});

        assertEquals(0, mode.netGetGoalType());
        assertFalse(mode.netIsNetRankingViewOK(engine));
    }

    @Test
    void loadRankingDoesNotThrow() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        CustomProperties prop = new CustomProperties();
        Method loadRanking = Method.getDeclaredMethod(NetDummyMode.class,
                "loadRanking", nullpomino.util.CustomProperties.class, String.class);
        loadRanking.invoke(mode, prop, "test");
        // Should not throw
    }

    @Test
    void renderLastDoesNotThrow() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.renderLast(engine, 0);
        // Should not throw
    }

    @Test
    void netplayInitHandlesNullLobby() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        mode.netplayInit(null);
        // Should not throw
    }

    @Test
    void netplayUnloadHandlesNullLobby() throws Exception {
        NetDummyMode mode = new NetDummyMode();
        mode.netplayUnload(null);
        // Should not throw
    }

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static Object readField(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.get(obj);
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

    private static class Method {
        java.lang.reflect.Method inner;

        Method(java.lang.reflect.Method m) {
            this.inner = m;
        }

        void invoke(Object obj, Object... args) throws Exception {
            inner.invoke(obj, args);
        }

        static Method getDeclaredMethod(Class<?> cls, String name, Class<?>... paramTypes)
                throws NoSuchMethodException {
            java.lang.reflect.Method m = cls.getDeclaredMethod(name, paramTypes);
            m.setAccessible(true);
            return new Method(m);
        }
    }
}
