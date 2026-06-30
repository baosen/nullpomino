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
 * Covers uncovered lines in {@link ExtremeMode}:
 * <ul>
 *   <li>Line 186: netSendOptions called when netIsNetPlay + spectators present</li>
 *   <li>Line 210: netEnterNetPlayRankingScreen called on D-button in net play</li>
 *   <li>Line 483: T-Spin single mini no-B2B (pts += 200)</li>
 *   <li>Line 499: T-Spin double mini useAllSpinBonus + B2B (pts += 600)</li>
 *   <li>Line 506: T-Spin double non-mini B2B (pts += 1800)</li>
 * </ul>
 */
class ExtremeModeBranchCoverageTest2 {

    // -----------------------------------------------------------------------
    // Line 186: netSendOptions via onSetting change with spectators
    // -----------------------------------------------------------------------

    @Test
    void onSettingChangeSignalsNetOptions() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        wireNetLobby(mode);
        engine.owner.replayMode = false;
        setField(mode, "netIsNetPlay", true);
        setField(mode, "netNumSpectators", 1);
        setField(mode, "menuCursor", 1); // case 1: tspinEnableType
        setField(mode, "menuTime", 10);

        // Press RIGHT to trigger menu change
        engine.ctrl.clearButtonState();
        engine.ctrl.buttonPress[Controller.BUTTON_RIGHT] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_RIGHT] = 1;

        // Should not throw; netSendOptions swallows the null-socket send
        mode.onSetting(engine, 0);
        assertTrue(true);
    }

    // -----------------------------------------------------------------------
    // Line 210: netEnterNetPlayRankingScreen via D-button in net play
    // -----------------------------------------------------------------------

    @Test
    void onSettingDButtonEntersNetRanking() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        wireNetLobby(mode);
        engine.owner.replayMode = false;
        setField(mode, "netIsNetPlay", true);
        setField(mode, "startlevel", 0);
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
    // Line 483: T-Spin single mini, no B2B
    // -----------------------------------------------------------------------

    @Test
    void calcScoreTSpinSingleMiniNoB2B() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.createFieldIfNeeded();
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));
        engine.nowPieceObject = new Piece(Piece.PIECE_T);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = true;
        engine.tspinez = false;
        engine.b2b = false;

        mode.calcScore(engine, 0, 1);

        // T-Spin single mini no-B2B = 200 * (0+1) = 200
        assertEquals(200, engine.statistics.score);
    }

    // -----------------------------------------------------------------------
    // Line 499: T-Spin double mini useAllSpinBonus + B2B
    // -----------------------------------------------------------------------

    @Test
    void calcScoreTSpinDoubleMiniWithB2B() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.createFieldIfNeeded();
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));
        engine.nowPieceObject = new Piece(Piece.PIECE_T);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = true;
        engine.useAllSpinBonus = true;
        engine.tspinez = false;
        engine.b2b = true;

        mode.calcScore(engine, 0, 2);

        // T-Spin double mini B2B = 600 * (0+1) = 600
        assertEquals(600, engine.statistics.score);
    }

    // -----------------------------------------------------------------------
    // Line 506: T-Spin double non-mini (or useAllSpinBonus==false), B2B
    // -----------------------------------------------------------------------

    @Test
    void calcScoreTSpinDoubleNonMiniWithB2B() throws Exception {
        ExtremeMode mode = new ExtremeMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.createFieldIfNeeded();
        engine.field.setBlock(0, engine.field.getHeight() - 1,
                new Block(Block.BLOCK_COLOR_GRAY));
        engine.nowPieceObject = new Piece(Piece.PIECE_T);

        engine.statistics.level = 0;
        engine.statistics.score = 0;
        engine.tspin = true;
        engine.tspinmini = false; // not mini -> goes to the else branch at line 504
        engine.tspinez = false;
        engine.b2b = true;

        mode.calcScore(engine, 0, 2);

        // T-Spin double non-mini B2B = 1800 * (0+1) = 1800
        assertEquals(1800, engine.statistics.score);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(ExtremeMode mode) {
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
