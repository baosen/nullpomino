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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers uncovered lines in {@link ComboRaceMode}:
 * <ul>
 *   <li>Line 315: comboColumn-- inside while-loop in case 3 of onSetting</li>
 *   <li>Line 409: netEnterNetPlayRankingScreen on D-button in net play</li>
 * </ul>
 */
class ComboRaceModeBranchCoverageTest2 {

    // -----------------------------------------------------------------------
    // Line 315: comboColumn-- when comboColumn + comboWidth - 1 > 10
    // -----------------------------------------------------------------------

    @Test
    void onSettingCase3ComboColumnDecrement() throws Exception {
        ComboRaceMode mode = new ComboRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        engine.owner.replayMode = false;
        // cursor 3 = comboWidth; set comboColumn high so that after comboWidth
        // increases, the while-condition comboColumn + comboWidth - 1 > 10 fires
        setField(mode, "menuCursor", 3);
        setField(mode, "comboColumn", 10);
        setField(mode, "comboWidth", 1); // will become 2 after RIGHT press
        setField(mode, "menuTime", 10);

        // Press RIGHT to increment comboWidth from 1 to 2
        engine.ctrl.clearButtonState();
        engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;

        mode.onSetting(engine, 0);

        // After: comboWidth==2, while loop fired comboColumn-- at least once
        // comboColumn + comboWidth - 1 should be <= 10 afterward
        int col = readInt(mode, "comboColumn");
        int width = readInt(mode, "comboWidth");
        assertTrue(col + width - 1 <= 10,
                "comboColumn should have been decremented so col+width-1 <= 10");
    }

    // -----------------------------------------------------------------------
    // Line 409: netEnterNetPlayRankingScreen via D-button in net play
    // -----------------------------------------------------------------------

    @Test
    void onSettingDButtonEntersNetRanking() throws Exception {
        ComboRaceMode mode = new ComboRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        wireNetLobby(mode);
        engine.owner.replayMode = false;
        setField(mode, "netIsNetPlay", true);
        setField(mode, "big", false);      // netIsNetRankingViewOK: !big && ai==null
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

    private static GameEngine freshEngine(ComboRaceMode mode) {
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

    private static int readInt(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        return f.getInt(obj);
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
