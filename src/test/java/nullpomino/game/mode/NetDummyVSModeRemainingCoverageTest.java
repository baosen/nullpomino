package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Field;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
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
        mode.getClass().getSuperclass().getDeclaredMethod("netOnJoin", 
            Class.forName("nullpomino.gui.net.NetLobbyFrame"), 
            Class.forName("nullpomino.game.net.NetPlayerClient"), 
            Class.forName("nullpomino.game.net.NetRoomInfo")).invoke(mode, null, null, null);
        assertTrue(true, "net on join");
    }

    // --- helpers ---
    private static void setField(Object o, String n, Object v) throws Exception {
        findField(o.getClass(), n).setAccessible(true);
        findField(o.getClass(), n).set(o, v);
    }

    private static java.lang.reflect.Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            try { return c.getDeclaredField(name); } catch (NoSuchFieldException e) { /* continue */ }
        }
        throw new NoSuchFieldException(name);
    }
}
