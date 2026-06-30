package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;

import nullpomino.game.component.Block;
import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.Test;

/**
 * Covers uncovered lines in {@link TechnicianMode}:
 * <ul>
 *   <li>Line 257: netEnterNetPlayRankingScreen on D-button in net play</li>
 *   <li>Line 676: T-Spin double non-mini no-B2B (pts += 1200)</li>
 * </ul>
 */
class TechnicianModeBranchCoverageTest2 {

    // -----------------------------------------------------------------------
    // Line 257: netEnterNetPlayRankingScreen via D-button in net play
    // -----------------------------------------------------------------------

    @Test
    void onSettingDButtonEntersNetRanking() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        wireNetLobby(mode);
        engine.owner.replayMode = false;
        setField(mode, "netIsNetPlay", true);
        setField(mode, "startlevel", 0);   // netIsNetRankingViewOK requires startlevel==0
        setField(mode, "big", false);       // and big==false
        setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
        setField(mode, "menuTime", 10);

        // Press D to trigger ranking screen
        engine.ctrl.clearButtonState();
        engine.ctrl.buttonPress[Controller.BUTTON_D] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_D] = 1;

        mode.onSetting(engine, 0);
        assertTrue(readBoolean(mode, "netIsNetRankingDisplayMode"),
                "pressing D in net play with startlevel==0 should enter ranking screen");
    }

    // -----------------------------------------------------------------------
    // Line 676: T-Spin double non-mini, no B2B (pts += 1200)
    // -----------------------------------------------------------------------

    @Test
    void calcScoreTSpinDoubleNonMiniNoB2B() throws Exception {
        TechnicianMode mode = new TechnicianMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.createFieldIfNeeded();
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));
        engine.nowPieceObject = new Piece(Piece.PIECE_T);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = false; // not mini -> else branch at line 673
        engine.tspinez = false;
        engine.b2b = false;      // no B2B -> line 676
        // Set goal > expected reduction so no level-up fires (which would add a
        // time bonus and inflate the score)
        setField(mode, "goal", 9999);

        mode.calcScore(engine, 0, 2);

        // T-Spin double non-mini no-B2B = 1200 * (0+1) = 1200
        assertEquals(1200, engine.statistics.score);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(TechnicianMode mode) {
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
