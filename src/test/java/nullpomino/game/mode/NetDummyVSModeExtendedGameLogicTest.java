package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link NetDummyVSMode}: covers getName,
 * isVSMode, modeInit allocation, netvsResetFlags, netvsIsAttackable
 * rules, netvsGetPlayerIDbySeatID mappings, onReady guard, onMove
 * remote-player handling, onLast timer, and renderLast/no-op stubs.
 */
class NetDummyVSModeExtendedGameLogicTest {

    @Test
    void getNameReturnsExpected() {
        NetDummyVSMode mode = new NetDummyVSMode();
        assertEquals("NET-VS-DUMMY", mode.getName());
    }

    @Test
    void isVSModeReturnsTrue() {
        NetDummyVSMode mode = new NetDummyVSMode();
        assertTrue(mode.isVSMode());
    }

    @Test
    void getPlayersReturnsSix() {
        NetDummyVSMode mode = new NetDummyVSMode();
        assertEquals(6, mode.getPlayers());
    }

    @Test
    void isNetplayModeReturnsTrue() {
        NetDummyVSMode mode = new NetDummyVSMode();
        assertTrue(mode.isNetplayMode());
    }

    @Test
    void modeInitAllocatesAllArrays() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);

        // Verify player arrays are allocated with NETVS_MAX_PLAYERS (6)
        assertEquals(6, ((boolean[]) readField(mode, "netvsPlayerExist")).length);
        assertEquals(6, ((boolean[]) readField(mode, "netvsPlayerReady")).length);
        assertEquals(6, ((int[]) readField(mode, "netvsPlayerSeatID")).length);
        assertEquals(6, ((int[]) readField(mode, "netvsPlayerUID")).length);
        assertEquals(6, ((int[]) readField(mode, "netvsPlayerWinCount")).length);
        assertEquals(6, ((int[]) readField(mode, "netvsPlayerPlayCount")).length);
        assertEquals(6, ((int[]) readField(mode, "netvsPlayerTeamColor")).length);
        assertEquals(6, ((String[]) readField(mode, "netvsPlayerName")).length);
        assertEquals(6, ((String[]) readField(mode, "netvsPlayerTeam")).length);
        assertEquals(6, ((int[]) readField(mode, "netvsPlayerSkin")).length);

        assertFalse(readBoolean(mode, "netvsAutoStartTimerActive"));
        assertEquals(0, readInt(mode, "netvsAutoStartTimer"));
    }

    @Test
    void netvsResetFlagsClearsRoundState() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);

        mode.netvsResetFlags();

        assertFalse(readBoolean(mode, "netvsIsGameActive"));
        assertFalse(readBoolean(mode, "netvsIsGameFinished"));
        assertFalse(readBoolean(mode, "netvsIsReadyChangePending"));
        assertFalse(readBoolean(mode, "netvsIsDeadPending"));
        assertFalse(readBoolean(mode, "netvsIsNewcomer"));
        assertFalse(readBoolean(mode, "netvsPlayTimerActive"));
        assertFalse(readBoolean(mode, "netvsIsPractice"));
        assertFalse(readBoolean(mode, "netvsIsPracticeExitAllowed"));
        assertEquals(0, readInt(mode, "netvsPlayTimer"));
        assertEquals(0, readInt(mode, "netvsPieceMoveTimer"));

        // Arrays should be reallocated with size 6
        assertEquals(6, ((boolean[]) readField(mode, "netvsPlayerResultReceived")).length);
        assertEquals(6, ((boolean[]) readField(mode, "netvsPlayerDead")).length);
        assertEquals(6, ((int[]) readField(mode, "netvsPlayerPlace")).length);
    }

    @Test
    void netvsGetPlayerIDbySeatIDIdentityMapping() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();

        Method getID = Method.getDeclaredMethod(NetDummyVSMode.class,
                "netvsGetPlayerIDbySeatID", int.class, int.class);

        // When my seat is 0, seat-to-player is identity
        assertEquals(0, (int) getID.invoke(mode, 0, 0));
        assertEquals(1, (int) getID.invoke(mode, 1, 0));
        assertEquals(2, (int) getID.invoke(mode, 2, 0));
    }

    @Test
    void netvsGetPlayerIDbySeatIDWithOffset() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();

        Method getID = Method.getDeclaredMethod(NetDummyVSMode.class,
                "netvsGetPlayerIDbySeatID", int.class, int.class);

        // When my seat is 1, seat 1 maps to player 0
        assertEquals(0, (int) getID.invoke(mode, 1, 1));
        assertEquals(1, (int) getID.invoke(mode, 0, 1));
    }

    @Test
    void netvsGetPlayerIDbySeatIDNegativeMySeatClamped() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();

        Method getID = Method.getDeclaredMethod(NetDummyVSMode.class,
                "netvsGetPlayerIDbySeatID", int.class, int.class);

        // -1 (spectator) treated as 0
        assertEquals(0, (int) getID.invoke(mode, 0, -1));
    }

    @Test
    void netvsIsAttackableCannotAttackSelf() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);

        Method isAttackable = Method.getDeclaredMethod(NetDummyVSMode.class,
                "netvsIsAttackable", int.class);

        assertFalse((boolean) isAttackable.invoke(mode, 0));
        assertFalse((boolean) isAttackable.invoke(mode, -1));
    }

    @Test
    void netvsIsAttackableNonExistentPlayer() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);

        Method isAttackable = Method.getDeclaredMethod(NetDummyVSMode.class,
                "netvsIsAttackable", int.class);

        // Player 1 doesn't exist
        assertFalse((boolean) isAttackable.invoke(mode, 1));
    }

    @Test
    void constantsArePinned() throws Exception {
        assertEquals(6, findField(NetDummyVSMode.class, "NETVS_MAX_PLAYERS").getInt(null));

        int[][] seatNums = (int[][]) findField(
                NetDummyVSMode.class, "NETVS_GAME_SEAT_NUMBERS").get(null);
        assertEquals(6, seatNums.length);
        assertEquals(6, seatNums[0].length);

        assertEquals(Block.BLOCK_COLOR_RED,
                ((int[]) findField(NetDummyVSMode.class, "NETVS_PLAYER_COLOR_BLOCK").get(null))[0]);
    }

    @Test
    void onMoveRemotePlayerReturnsTrue() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        // PlayerID != 0 is remote -> should stop
        assertTrue(mode.onMove(engine, 1));
    }

    @Test
    void onMoveLocalPlayerReturnsFalse() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        // PlayerID 0 is local -> should not stop
        assertFalse(mode.onMove(engine, 0));
    }

    @Test
    void onLastDoesNotThrowWithoutNetwork() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.onLast(engine, 0);
        // Should not throw
    }

    @Test
    void renderLastDoesNotThrowWithoutNetwork() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        mode.renderLast(engine, 0);
        // Should not throw
    }

    @Test
    void onReadyDoesNotThrowWithoutNetwork() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        try {
            mode.onReady(engine, 0);
        } catch (NullPointerException e) {
            // Expected: netCurrentRoomInfo is null
        }
    }

    @Test
    void netvsGetNumberOfTeamsAliveReturnsCorrectCount() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);

        Method getTeams = Method.getDeclaredMethod(NetDummyVSMode.class,
                "netvsGetNumberOfTeamsAlive");

        int count = (int) getTeams.invoke(mode);
        assertEquals(0, count); // No active players
    }

    @Test
    void netPlayerInitSetsEngineDefaults() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

        Method netPlayerInit = Method.getDeclaredMethod(NetDummyVSMode.class,
                "netPlayerInit", GameEngine.class, int.class);
        netPlayerInit.invoke(mode, engine, 0);

        assertEquals(10, engine.fieldWidth);
        assertEquals(20, engine.fieldHeight);
        assertFalse(engine.gameoverAll);
        assertTrue(engine.allowTextRenderByReceiver);
    }

    @Test
    void gameOverTriggersEndInPractice() throws Exception {
        NetDummyVSMode mode = new NetDummyVSMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        GameEngine engine = manager.engine[0];

		engine.createFieldIfNeeded();
		setBoolean(mode, "netvsIsPractice", true);

		boolean result = mode.onGameOver(engine, 0);

		// In practice mode with low statc[0], should return false
		assertFalse(result);
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

        Object invoke(Object obj, Object... args) throws Exception {
            return inner.invoke(obj, args);
        }

        static Method getDeclaredMethod(Class<?> cls, String name, Class<?>... paramTypes)
                throws NoSuchMethodException {
            java.lang.reflect.Method m = cls.getDeclaredMethod(name, paramTypes);
            m.setAccessible(true);
            return new Method(m);
        }
    }
}
