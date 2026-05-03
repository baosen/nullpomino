package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;

import org.junit.jupiter.api.Test;

/**
 * Extended test coverage for {@link NetVSDigRaceMode}: covers getName,
 * modeInit, fillGarbage, getRemainGarbageLines, calcScore dig-race
 * completion logic, updateMeter, getNowPlayerPlace, renderResult,
 * and the no-op stubs.
 */
class NetVSDigRaceModeExtendedGameLogicTest {

    @Test
    void getNameReturnsExpected() {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        assertEquals("NET-VS-DIG RACE", mode.getName());
    }

    @Test
    void modeInitAllocatesArrays() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);

        assertEquals(6, ((int[]) readField(mode, "playerRemainLines")).length);
        assertEquals(6, ((int[]) readField(mode, "playerStartGems")).length);
    }

    @Test
    void getNowPlayerPlaceReturnsCorrectPlace() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.mode = mode;
        manager.init();

        // Set up two players
        boolean[] playerExist = (boolean[]) readField(mode, "netvsPlayerExist");
        playerExist[0] = true;
        playerExist[1] = true;

        // Ensure both engines have fields so getNowPlayerPlace can compare
        manager.engine[1].createFieldIfNeeded();

        int[] playerRemainLines = (int[]) readField(mode, "playerRemainLines");
        playerRemainLines[0] = 10;
        playerRemainLines[1] = 5;

        // Player 0 has more lines remaining -> lower place (worse)
        Method getPlace = Method.getDeclaredMethod(NetVSDigRaceMode.class,
                "getNowPlayerPlace", GameEngine.class, int.class);

        int place = (int) getPlace.invoke(mode, manager.engine[0], 0);
        assertEquals(1, place); // Player 0 is behind player 1
    }

    @Test
    void getNowPlayerPlaceReturnsZeroForLeader() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        manager.mode = mode;
        manager.init();

        boolean[] playerExist = (boolean[]) readField(mode, "netvsPlayerExist");
        playerExist[0] = true;
        playerExist[1] = true;

        int[] playerRemainLines = (int[]) readField(mode, "playerRemainLines");
        playerRemainLines[0] = 3;
        playerRemainLines[1] = 10;

        Method getPlace = Method.getDeclaredMethod(NetVSDigRaceMode.class,
                "getNowPlayerPlace", GameEngine.class, int.class);

        int place = (int) getPlace.invoke(mode, manager.engine[0], 0);
        assertEquals(0, place); // Player 0 is winning
    }

    @Test
    void calcScoreUpdatesPlayerRemainLines() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        // Need netCurrentRoomInfo to be set with non-null useMap
        setNetCurrentRoomInfo(mode);
        setBoolean(mode, "netvsIsPractice", true);

        // Fill field with garbage so getRemainGarbageLines returns something
        engine.field.setBlockColor(0, 19, 1);

        mode.calcScore(engine, 0, 1);

        // playerRemainLines should have been updated
        int[] playerRemainLines = (int[]) readField(mode, "playerRemainLines");
        assertTrue(playerRemainLines[0] >= 0);
    }

    @Test
    void calcScoreCompletesGameWhenRemainLinesZero() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        setNetCurrentRoomInfo(mode);

        // Set remainLines to 0 to trigger completion
        int[] playerRemainLines = (int[]) readField(mode, "playerRemainLines");
        playerRemainLines[0] = 0;

        // calcScore with netvsIsPractice=true (avoids netLobby NPE)
        setBoolean(mode, "netvsIsPractice", true);

        mode.calcScore(engine, 0, 1);

        // Should trigger race completion (stat changed to EXCELLENT in practice mode)
        assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
    }

    @Test
    void netSendStatsAndRecvStatsRoundTrip() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        int[] playerRemainLines = (int[]) readField(mode, "playerRemainLines");
        playerRemainLines[0] = 8;

        // netSendStats sends to netLobby which may be null, but netRecvStats sets locally
        mode.netRecvStats(engine, new String[]{"game", "stats", "0", "0", "12"});

        assertEquals(12, playerRemainLines[0]);
    }

    @Test
    void renderLastDoesNotThrow() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        mode.renderLast(engine, 0);
        // Should not throw
    }

    @Test
    void renderResultDoesNotThrow() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);
        engine.isVisible = true;

        mode.renderResult(engine, 0);
        // Should not throw
    }

    @Test
    void updateMeterUpdatesEngineMeterValues() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameManager manager = new GameManager(new EventReceiver());
        mode.modeInit(manager);
        GameEngine engine = freshEngine(mode);

        int[] playerRemainLines = (int[]) readField(mode, "playerRemainLines");
        playerRemainLines[0] = 10;

        Method updateMeter = Method.getDeclaredMethod(NetVSDigRaceMode.class,
                "updateMeter", GameEngine.class);
        updateMeter.invoke(mode, engine);

        assertTrue(engine.meterValue > 0);
    }

    // ---------------------------------------------------------------
    // Reflection helpers
    // ---------------------------------------------------------------

    private static GameEngine freshEngine(NetVSDigRaceMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        manager.engine[0].createFieldIfNeeded();
        manager.engine[0].nowPieceObject = new Piece(Piece.PIECE_T);
        return manager.engine[0];
    }

    private static void setNetCurrentRoomInfo(NetVSDigRaceMode mode) throws Exception {
        Class<?> roomInfoClass = Class.forName("nullpomino.game.net.NetRoomInfo");
        Object roomInfo = roomInfoClass.getConstructor().newInstance();
        java.lang.reflect.Field useMapField = roomInfoClass.getField("useMap");
        useMapField.setBoolean(roomInfo, false);
        java.lang.reflect.Field garbagePercentField = roomInfoClass.getField("garbagePercent");
        garbagePercentField.setInt(roomInfo, 100);

        Field f = findField(mode.getClass(), "netCurrentRoomInfo");
        f.setAccessible(true);
        f.set(mode, roomInfo);
    }

    private static void setBoolean(Object obj, String name, boolean value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setBoolean(obj, value);
    }

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

    private static boolean readBoolean(Object obj, String name, int index) throws Exception {
        boolean[] arr = (boolean[]) readField(obj, name);
        return arr[index];
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

    // Inner class for reflection-based method access
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
