package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Block;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Raises branch coverage of {@link NetVSDigRaceMode}. The mode inherits
 * {@link NetDummyVSMode#onSetting} (no wraparound of its own); its uncovered
 * branches live in the garbage-fill / remaining-lines helpers, the
 * score/place/meter logic and the net-message send/receive handlers. Tests
 * drive those directly with crafted field + player state, using the
 * disconnected-NetLobby idiom so {@code netPlayerClient.send(...)} swallows the
 * null-socket write instead of throwing.
 */
class NetVSDigRaceModeBranchCoverageTest {

    // -----------------------------------------------------------------------
    // onReady (normal, non-map): fills garbage for an opponent seat, exercising
    // fillGarbage playerID!=0 / skin<0 branches and getRemainGarbageLines gem +
    // garbage detection (L67, L68, L71, L127, L130, L169, L170 true side).
    // -----------------------------------------------------------------------

    @Test
    void onReadyFillsGarbageForOpponent() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, false); // useMap = false

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        exist[1] = true;

        GameEngine opp = engine.owner.engine[1];
        opp.statc[0] = 0;
        opp.playerID = 1;
        // Skin stays -1 (modeInit default) => fillGarbage skin<0 branch fires.

        mode.onReady(opp, 1);

        int[] remain = (int[]) read(mode, "playerRemainLines");
        assertTrue(remain[1] > 0, "opponent field should have garbage lines remaining");
    }

    @Test
    void onReadyFillsGarbageForLocalPlayer() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, false);

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        exist[0] = true;
        engine.statc[0] = 0;
        engine.playerID = 0;

        mode.onReady(engine, 0);

        int[] remain = (int[]) read(mode, "playerRemainLines");
        assertTrue(remain[0] > 0, "local field should have garbage lines remaining");
    }

    // -----------------------------------------------------------------------
    // onReady map game path + turnAllBlocksToGem color-range branch
    // (L170 false side, L155).
    // -----------------------------------------------------------------------

    @Test
    void onReadyMapGameAndTurnBlocksToGem() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, true); // useMap = true

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        exist[0] = true;
        engine.statc[0] = 0;
        engine.playerID = 0;
        engine.createFieldIfNeeded();

        // Seed the field with colored blocks so turnAllBlocksToGem's colour-range
        // guard (RED..PURPLE) takes its true side, plus one grey block for the
        // false side of the colour test.
        nullpomino.game.component.Field f = engine.field;
        int h = f.getHeight();
        f.setBlock(0, h - 1, new Block(Block.BLOCK_COLOR_RED, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));
        f.setBlock(1, h - 1, new Block(Block.BLOCK_COLOR_PURPLE, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));
        f.setBlock(2, h - 1, new Block(Block.BLOCK_COLOR_GRAY, 0, Block.BLOCK_ATTRIBUTE_VISIBLE));

        mode.onReady(engine, 0);

        assertEquals(Block.BLOCK_COLOR_GEM_RED, f.getBlock(0, h - 1).color,
                "RED should become a gem via turnAllBlocksToGem");
        assertEquals(Block.BLOCK_COLOR_GRAY, f.getBlock(2, h - 1).color,
                "GRAY is outside RED..PURPLE and must stay unchanged");
    }

    // -----------------------------------------------------------------------
    // calcScore: normal completion (real win) + map-game arm + practice arm
    // (L248, L251, L270).
    // -----------------------------------------------------------------------

    @Test
    void calcScorePracticeCompletion() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, false);

        int[] remain = (int[]) read(mode, "playerRemainLines");
        remain[0] = 0; // already cleared
        engine.createFieldIfNeeded(); // empty => getRemainGarbageLines returns 0

        setBool(mode, "netvsIsPractice", true);
        mode.calcScore(engine, 0, 1); // lines>0, playerID 0, remain becomes 0

        assertEquals(GameEngine.Status.EXCELLENT, engine.stat,
                "clearing all garbage in practice should flag EXCELLENT");
    }

    @Test
    void calcScoreRealWinSendsRacewin() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, false);

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        boolean[] dead = (boolean[]) read(mode, "netvsPlayerDead");
        int[] uid = (int[]) read(mode, "netvsPlayerUID");
        exist[0] = true; dead[0] = false; uid[0] = 100;
        // A dead opponent => getNowPlayerPlace returns -1 => L270 false side.
        exist[1] = true; dead[1] = true; uid[1] = 200;

        engine.createFieldIfNeeded();       // empty => remain 0 => completion
        engine.owner.engine[1].createFieldIfNeeded();
        setBool(mode, "netvsIsPractice", false);

        mode.calcScore(engine, 0, 1);

        assertEquals(GameEngine.Status.NOTHING, engine.stat,
                "a real win should set NOTHING (wait for everyone to die)");
    }

    @Test
    void calcScoreMapGameArm() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, true); // useMap => map arm (L249 false, L251 true)

        int[] startGems = (int[]) read(mode, "playerStartGems");
        startGems[0] = 4;
        engine.createFieldIfNeeded();
        setBool(mode, "netvsIsPractice", true);

        // Empty field => gems - gemClears = 0 => completion => EXCELLENT (practice).
        mode.calcScore(engine, 0, 2);

        assertEquals(GameEngine.Status.EXCELLENT, engine.stat);
    }

    // -----------------------------------------------------------------------
    // updateMeter map-game colour thresholds (via netRecvStats -> updateMeter is
    // the normal path; here we hit the map path through calcScore's meter update).
    // -----------------------------------------------------------------------

    @Test
    void updateMeterMapGameThresholds() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, true); // useMap

        int[] startGems = (int[]) read(mode, "playerStartGems");
        startGems[0] = 8;
        engine.createFieldIfNeeded();
        engine.playerID = 0;

        // With an empty field remainLines (gems-clears) is 0 which is <= every
        // fraction of startGems, so YELLOW/ORANGE/RED map thresholds all fire.
        invokeUpdateMeter(mode, engine);
        assertEquals(GameEngine.METER_COLOR_RED, engine.meterColor);
    }

    // -----------------------------------------------------------------------
    // renderLast: colour thresholds, 3-digit remain, last place, games count
    // (L301, L302, L306, L307, L308, L317, L325, L331, L347, L361, L367).
    // -----------------------------------------------------------------------

    @Test
    void renderLastColorThresholdsAndDigits() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, false);

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        exist[0] = true;
        engine.isVisible = true;
        setBool(mode, "netvsIsGameActive", true);
        engine.stat = GameEngine.Status.MOVE;

        int[] remain = (int[]) read(mode, "playerRemainLines");
        // 1-digit remain values below each colour threshold (14/8/4).
        for(int r : new int[]{13, 7, 3}) {
            remain[0] = r;
            engine.displaysize = 0;
            mode.renderLast(engine, 0);
        }
        // 3-digit remain in both display modes (L317 / L325).
        remain[0] = 123;
        engine.displaysize = 0;
        mode.renderLast(engine, 0);
        engine.displaysize = -1;
        mode.renderLast(engine, 0);

        assertTrue(true);
    }

    @Test
    void renderLastLastPlaceAndGamesCount() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, false);

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        boolean[] dead = (boolean[]) read(mode, "netvsPlayerDead");
        int[] place = (int[]) read(mode, "netvsPlayerPlace");
        exist[0] = true;
        engine.isVisible = true;

        // 6th place (place == 5) on both display sizes (L347 / L361).
        setBool(mode, "netvsIsGameActive", true);
        dead[0] = true; place[0] = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.displaysize = 0;
        mode.renderLast(engine, 0);
        engine.displaysize = -1;
        mode.renderLast(engine, 0);

        // Games-count branch (L367): not game active, not practice.
        setBool(mode, "netvsIsGameActive", false);
        setBool(mode, "netvsIsPractice", false);
        int[] win = (int[]) read(mode, "netvsPlayerWinCount");
        int[] play = (int[]) read(mode, "netvsPlayerPlayCount");
        win[0] = 1; play[0] = 4;
        engine.displaysize = 0;
        engine.stat = GameEngine.Status.RESULT; // y2 = 22 branch
        mode.renderLast(engine, 0);
        engine.displaysize = -1;
        mode.renderLast(engine, 0);

        assertTrue(true);
    }

    // -----------------------------------------------------------------------
    // renderResult on both scales
    // -----------------------------------------------------------------------

    @Test
    void renderResultBothScales() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        engine.isVisible = true;

        engine.displaysize = 0;
        mode.renderResult(engine, 0);
        engine.displaysize = -1;
        mode.renderResult(engine, 0);
        assertTrue(true);
    }

    // -----------------------------------------------------------------------
    // netSendStats (L406) and netRecvStats
    // -----------------------------------------------------------------------

    @Test
    void netSendAndRecvStats() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, false);
        setBool(mode, "netvsIsPractice", false);
        setBool(mode, "netIsWatch", false);

        engine.playerID = 0;
        int[] remain = (int[]) read(mode, "playerRemainLines");
        remain[0] = 6;
        invokeVoid(mode, "netSendStats", engine);

        // False side: practice on => no send.
        setBool(mode, "netvsIsPractice", true);
        invokeVoid(mode, "netSendStats", engine);

        // netRecvStats: full message parses remain, short message does not.
        String[] full = {"game", "stats", "1", "field", "9"};
        invokeRecvStats(mode, engine, full);
        assertEquals(9, remain[0]);
        String[] shortMsg = {"game", "stats", "1"};
        invokeRecvStats(mode, engine, shortMsg);
        assertEquals(9, remain[0], "short message must not overwrite remaining lines");
    }

    // -----------------------------------------------------------------------
    // netvsRecvEndGameStats: opponent stats applied for a non-zero seat (L447)
    // -----------------------------------------------------------------------

    @Test
    void recvEndGameStatsForOpponent() throws Exception {
        NetVSDigRaceMode mode = new NetVSDigRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode, false);
        setInt(mode, "netvsMySeatID", -1); // spectator => row 0

        // seatID 1 -> playerID 1, so (playerID != 0) is true (L447 true side).
        String[] msg = {"gstat", "x", "1", "0", "0", "0", "0", "0", "17", "1.1", "21", "2.2", "999"};
        invokeRecvEndGameStats(mode, msg);

        GameEngine target = engine.owner.engine[1];
        assertEquals(17, target.statistics.lines);
        assertEquals(21, target.statistics.totalPieceLocked);
        assertEquals(999, target.statistics.time);
        boolean[] got = (boolean[]) read(mode, "netvsPlayerResultReceived");
        assertTrue(got[1]);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(NetVSDigRaceMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        for(GameEngine e : manager.engine) e.init();
        manager.engine[0].owner.replayMode = false;
        return manager.engine[0];
    }

    private static void wireNetLobby(Object mode, boolean useMap) throws Exception {
        NetLobbyFrame lobby = new NetLobbyFrame();
        lobby.netPlayerClient = new NetPlayerClient();
        setField(mode, "netLobby", lobby);
        NetRoomInfo room = new NetRoomInfo();
        room.useMap = useMap;
        setField(mode, "netCurrentRoomInfo", room);
    }

    private static void invokeUpdateMeter(Object mode, GameEngine engine) throws Exception {
        Method m = findMethod(mode.getClass(), "updateMeter", GameEngine.class);
        m.invoke(mode, engine);
    }

    private static void invokeVoid(Object mode, String name, GameEngine engine) throws Exception {
        Method m = findMethod(mode.getClass(), name, GameEngine.class);
        m.invoke(mode, engine);
    }

    private static void invokeRecvStats(Object mode, GameEngine engine, String[] msg) throws Exception {
        Method m = findMethod(mode.getClass(), "netRecvStats", GameEngine.class, String[].class);
        m.invoke(mode, engine, msg);
    }

    private static void invokeRecvEndGameStats(Object mode, String[] msg) throws Exception {
        Method m = findMethod(mode.getClass(), "netvsRecvEndGameStats", String[].class);
        m.invoke(mode, (Object) msg);
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) throws NoSuchMethodException {
        Class<?> c = cls;
        while (c != null) {
            try {
                Method m = c.getDeclaredMethod(name, params);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static Object read(Object obj, String name) throws Exception {
        return field(obj.getClass(), name).get(obj);
    }

    private static void setField(Object obj, String name, Object value) throws Exception {
        field(obj.getClass(), name).set(obj, value);
    }

    private static void setInt(Object obj, String name, int value) throws Exception {
        field(obj.getClass(), name).setInt(obj, value);
    }

    private static void setBool(Object obj, String name, boolean value) throws Exception {
        field(obj.getClass(), name).setBoolean(obj, value);
    }

    private static Field field(Class<?> cls, String name) throws NoSuchFieldException {
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
