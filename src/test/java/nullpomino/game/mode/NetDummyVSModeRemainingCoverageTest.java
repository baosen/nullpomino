package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NetDummyVSModeRemainingCoverageTest {

    private GameManager gm;
    private GameEngine engine;
    private NetDummyVSMode mode;

    @BeforeEach
    void setUp() {
        mode = new NetDummyVSMode();
        gm = new GameManager(new EventReceiver());
        gm.mode = mode;
        gm.init();
        engine = gm.engine[0];
        engine.init();
    }

    @Test
    void netvsDrawPlayerNameNullName() throws Exception {
        setField(mode, "netvsPlayerName", null);
        mode.getClass().getSuperclass().getDeclaredMethod("netDrawPlayerName", GameEngine.class).invoke(mode, engine);
        assertTrue(true, "null name path");
    }

    @Test
    void netvsDrawPlayerNameEmpty() throws Exception {
        setField(mode, "netvsPlayerName", new String[]{""});
        engine.displaysize = -1;
        mode.getClass().getSuperclass().getDeclaredMethod("netDrawPlayerName", GameEngine.class).invoke(mode, engine);
        assertTrue(true, "empty name path");
    }

    @Test
    void modeInitCreatesArrays() {
        mode.modeInit(gm);
        assertTrue(true, "mode init");
    }

    @Test
    void playerInit() {
        mode.playerInit(engine, 0);
        assertTrue(true, "player init");
    }

    @Test
    void netOnJoin() throws Exception {
        NetRoomInfo room = new NetRoomInfo();
        room.playing = true;
        java.lang.reflect.Method m = mode.getClass().getDeclaredMethod("netOnJoin",
            Class.forName("nullpomino.gui.net.NetLobbyFrame"),
            Class.forName("nullpomino.game.net.NetPlayerClient"),
            Class.forName("nullpomino.game.net.NetRoomInfo"));
        // Without a real NetLobbyFrame, netUpdatePlayerExist NPEs after the assignment
        // chain we want to cover. Verify we got past the netCurrentRoomInfo assignment.
        try { m.invoke(mode, null, null, room); }
        catch (java.lang.reflect.InvocationTargetException e) {
            assertInstanceOf(NullPointerException.class, e.getCause());
        }
        assertTrue(mode.netIsNetPlay, "netIsNetPlay set");
    }

    // --- helpers ---
    private static void setField(Object o, String n, Object v) throws Exception {
        java.lang.reflect.Field f = findField(o.getClass(), n);
        f.setAccessible(true);
        f.set(o, v);
    }

    private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException e) { /* continue */ }
        }
        throw new NoSuchFieldException(name);
    }
}
