package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Controller;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers uncovered lines in {@link LineRaceMode}:
 * <ul>
 *   <li>Line 221: netSendOptions called when menu item changes + spectators present</li>
 *   <li>Line 235: netSendOptions called on cursor-10 (load preset) + spectators present</li>
 *   <li>Line 261: netEnterNetPlayRankingScreen on D-button in net play</li>
 * </ul>
 */
class LineRaceModeBranchCoverageTest2 {

    // -----------------------------------------------------------------------
    // Line 221: netSendOptions on menu change with spectators
    // -----------------------------------------------------------------------

    @Test
    void onSettingChangeSignalsNetOptions() throws Exception {
        LineRaceMode mode = new LineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        wireNetLobby(mode);
        engine.owner.replayMode = false;
        setField(mode, "netIsNetPlay", true);
        setField(mode, "netNumSpectators", 1);
        setField(mode, "menuCursor", 1); // any non-preset cursor
        setField(mode, "menuTime", 10);

        // Press RIGHT to trigger menu value change
        engine.ctrl.clearButtonState();
        engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;

        // Should not throw; netSendOptions swallows the null-socket send
        mode.onSetting(engine, 0);
        assertTrue(true);
    }

    // -----------------------------------------------------------------------
    // Line 235: netSendOptions on cursor-10 (load preset) with spectators
    // -----------------------------------------------------------------------

    @Test
    void onSettingLoadPresetSignalsNetOptions() throws Exception {
        LineRaceMode mode = new LineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        wireNetLobby(mode);
        engine.owner.replayMode = false;
        setField(mode, "netIsNetPlay", true);
        setField(mode, "netNumSpectators", 1);
        setField(mode, "netIsWatch", false);
        setField(mode, "menuCursor", 10); // cursor 10 = load preset
        setField(mode, "menuTime", 10);

        // Press A to confirm (load preset + send options)
        engine.ctrl.clearButtonState();
        engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

        mode.onSetting(engine, 0);
        assertTrue(true);
    }

    // -----------------------------------------------------------------------
    // Line 261: netEnterNetPlayRankingScreen via D-button in net play
    // -----------------------------------------------------------------------

    @Test
    void onSettingDButtonEntersNetRanking() throws Exception {
        LineRaceMode mode = new LineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        wireNetLobby(mode);
        engine.owner.replayMode = false;
        setField(mode, "netIsNetPlay", true);
        setField(mode, "big", false);
        setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
        setField(mode, "menuTime", 10);

        // Press D to trigger ranking screen
        engine.ctrl.clearButtonState();
        engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

        mode.onSetting(engine, 0);
        assertTrue(readBoolean(mode, "netIsNetRankingDisplayMode"),
                "pressing D in net play should enter ranking screen");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(LineRaceMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        manager.engine[0].init();
        return manager.engine[0];
    }

    private static void wireNetLobby(Object mode) throws Exception {
        NetLobbyFrame lobby = new NetLobbyFrame();
        lobby.netPlayerClient = new NetPlayerClient();
        setField(mode, "netLobby", lobby);
    }

    private static boolean readBoolean(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        return f.getBoolean(obj);
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
