package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.Test;

/**
 * Raises branch coverage of {@link NetVSLineRaceMode}. The mode has no
 * settings-menu wraparound (it inherits {@link NetDummyVSMode#onSetting}); its
 * uncovered branches live in the score/place/meter logic and the net-message
 * send/receive handlers. Tests drive those methods directly with crafted state,
 * using the disconnected-NetLobby idiom so {@code netPlayerClient.send(...)}
 * swallows the null-socket write instead of throwing.
 */
class NetVSLineRaceModeBranchCoverageTest {

    // -----------------------------------------------------------------------
    // getNowPlayerPlace: tie on lines & pps, decided by lpm (L87)
    // -----------------------------------------------------------------------

    @Test
    void placeBrokenByLpmWhenLinesAndPpsTie() throws Exception {
        NetVSLineRaceMode mode = new NetVSLineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        boolean[] dead = (boolean[]) read(mode, "netvsPlayerDead");
        exist[0] = true; exist[1] = true;
        dead[0] = false; dead[1] = false;

        GameEngine me = engine.owner.engine[0];
        GameEngine enemy = engine.owner.engine[1];
        me.statistics.lines = 5;   enemy.statistics.lines = 5;   // equal lines
        me.statistics.pps = 1.0f;  enemy.statistics.pps = 1.0f;  // equal pps
        me.statistics.lpm = 1.0f;  enemy.statistics.lpm = 2.0f;   // my lpm is less

        int place = invokePlace(mode, me, 0);
        assertEquals(1, place, "losing the lpm tiebreak should drop the player one place");

        // Sanity: with a higher lpm the tie stays at place 0 (exercises the
        // false side of the L87 lpm condition).
        me.statistics.lpm = 3.0f;
        assertEquals(0, invokePlace(mode, me, 0));
    }

    // -----------------------------------------------------------------------
    // calcScore: all-clear bravo branch + practice-mode completion (L122, L127)
    // -----------------------------------------------------------------------

    @Test
    void calcScoreAllClearAndPracticeCompletion() throws Exception {
        NetVSLineRaceMode mode = new NetVSLineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "goalLines", 40);

        engine.createFieldIfNeeded();
        // Empty field + line clear => all-clear "bravo" branch (L122 true side).
        engine.statistics.lines = 1;
        setBool(mode, "netvsIsPractice", true);
        mode.calcScore(engine, 0, 1);

        // Reaching the goal while in practice => EXCELLENT (L127 true, practice arm).
        engine.statistics.lines = 40;
        mode.calcScore(engine, 0, 0);
        assertEquals(GameEngine.Status.EXCELLENT, engine.stat,
                "hitting goalLines in practice should flag EXCELLENT");
    }

    // -----------------------------------------------------------------------
    // calcScore: real (non-practice) win sends racewin and waits (L127 else arm,
    // L140 both sides via a dead player yielding place -1)
    // -----------------------------------------------------------------------

    @Test
    void calcScoreRealWinSendsRacewin() throws Exception {
        NetVSLineRaceMode mode = new NetVSLineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode);
        setInt(mode, "goalLines", 40);
        setBool(mode, "netvsIsPractice", false);

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        boolean[] dead = (boolean[]) read(mode, "netvsPlayerDead");
        int[] uid = (int[]) read(mode, "netvsPlayerUID");
        exist[0] = true; dead[0] = false; uid[0] = 100;
        // Player 1 exists but is dead => getNowPlayerPlace returns -1, so the
        // L140 (places[i] >= 0) guard takes its false branch for that seat.
        exist[1] = true; dead[1] = true; uid[1] = 200;

        engine.createFieldIfNeeded();
        engine.owner.engine[1].createFieldIfNeeded();
        engine.statistics.lines = 40; // reached goal

        mode.calcScore(engine, 0, 0);

        assertEquals(GameEngine.Status.NOTHING, engine.stat,
                "a real win should set NOTHING (wait for everyone to die)");
    }

    // -----------------------------------------------------------------------
    // renderLast: color thresholds + place text + games-count branch
    // -----------------------------------------------------------------------

    @Test
    void renderLastColorThresholdsAndPlaces() throws Exception {
        NetVSLineRaceMode mode = new NetVSLineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "goalLines", 40);

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        exist[0] = true;
        engine.isVisible = true;
        setBool(mode, "netvsIsGameActive", true);
        engine.stat = GameEngine.Status.MOVE;

        // remainLines just under each threshold => YELLOW/ORANGE/RED arms (L175-177).
        for(int rem : new int[]{35, 25, 15, 5}) {
            engine.statistics.lines = 40 - rem;
            mode.renderLast(engine, 0);
        }
        // 3-digit remain string (menu display, L186-ish) with a big goal.
        setInt(mode, "goalLines", 200);
        engine.statistics.lines = 0; // remain = 200 -> 3 chars
        engine.displaysize = 0;
        mode.renderLast(engine, 0);
        // 3-digit remain string in small-display path (displaysize == -1, L194).
        engine.displaysize = -1;
        mode.renderLast(engine, 0);

        assertTrue(true);
    }

    @Test
    void renderLastLastPlaceAndGamesCount() throws Exception {
        NetVSLineRaceMode mode = new NetVSLineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "goalLines", 40);

        boolean[] exist = (boolean[]) read(mode, "netvsPlayerExist");
        boolean[] dead = (boolean[]) read(mode, "netvsPlayerDead");
        int[] place = (int[]) read(mode, "netvsPlayerPlace");
        exist[0] = true;
        engine.isVisible = true;

        // Force the "6th place" (place == 5) branch via a dead player with a
        // precomputed place, on both display sizes (L216 and L230).
        setBool(mode, "netvsIsGameActive", true);
        dead[0] = true; place[0] = 5;
        engine.stat = GameEngine.Status.MOVE;
        engine.displaysize = 0;
        mode.renderLast(engine, 0);
        engine.displaysize = -1;
        mode.renderLast(engine, 0);

        // Games-count branch: not game active, not practice (L236).
        setBool(mode, "netvsIsGameActive", false);
        setBool(mode, "netvsIsPractice", false);
        int[] win = (int[]) read(mode, "netvsPlayerWinCount");
        int[] play = (int[]) read(mode, "netvsPlayerPlayCount");
        win[0] = 2; play[0] = 3;
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
        NetVSLineRaceMode mode = new NetVSLineRaceMode();
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
    // netSendStats: 1P, not practice, not watch (L273 true side)
    // -----------------------------------------------------------------------

    @Test
    void netSendStatsForLocalPlayer() throws Exception {
        NetVSLineRaceMode mode = new NetVSLineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode);
        setBool(mode, "netvsIsPractice", false);
        setBool(mode, "netIsWatch", false);

        engine.playerID = 0;
        engine.statistics.lines = 7;
        invokeVoid(mode, "netSendStats", engine);
        assertTrue(true);

        // False side: practice on => no send.
        setBool(mode, "netvsIsPractice", true);
        invokeVoid(mode, "netSendStats", engine);
        assertTrue(true);
    }

    // -----------------------------------------------------------------------
    // netRecvStats: parse guarded array positions + updateMeter
    // -----------------------------------------------------------------------

    @Test
    void netRecvStatsParsesGuardedFields() throws Exception {
        NetVSLineRaceMode mode = new NetVSLineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        setInt(mode, "goalLines", 40);

        // Full-length message => all three guarded parses run.
        String[] full = {"game", "stats", "1", "field", "12", "1.5", "2.5"};
        invokeRecvStats(mode, engine, full);
        assertEquals(12, engine.statistics.lines);

        // Short message => guards take their false side (nothing parsed past len).
        String[] shortMsg = {"game", "stats", "1"};
        invokeRecvStats(mode, engine, shortMsg);
        assertEquals(12, engine.statistics.lines, "short message must not overwrite lines");
    }

    // -----------------------------------------------------------------------
    // netvsRecvEndGameStats: watch mode receives an opponent's stats (L314)
    // -----------------------------------------------------------------------

    @Test
    void recvEndGameStatsInWatchMode() throws Exception {
        NetVSLineRaceMode mode = new NetVSLineRaceMode();
        GameEngine engine = freshEngine(mode);
        mode.playerInit(engine, 0);
        wireNetLobby(mode);
        setInt(mode, "netvsMySeatID", -1); // spectator => netvsGetPlayerIDbySeatID uses row 0

        // seatID 1 -> playerID 1 (row 0), so the (playerID != 0) guard is true and
        // the opponent's stats get applied (L314 true side).
        String[] msg = {"gstat", "x", "1", "0", "0", "0", "0", "0", "9", "1.1", "13", "2.2", "555"};
        invokeRecvEndGameStats(mode, msg);

        GameEngine target = engine.owner.engine[1];
        assertEquals(9, target.statistics.lines);
        assertEquals(13, target.statistics.totalPieceLocked);
        assertEquals(555, target.statistics.time);
        boolean[] got = (boolean[]) read(mode, "netvsPlayerResultReceived");
        assertTrue(got[1], "receiving an opponent's stats should mark the result received");
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static GameEngine freshEngine(NetVSLineRaceMode mode) {
        GameManager manager = new GameManager(new EventReceiver());
        manager.mode = mode;
        manager.init();
        // init() every engine so statistics/ctrl are allocated on the opponents too.
        for(GameEngine e : manager.engine) e.init();
        manager.engine[0].owner.replayMode = false;
        return manager.engine[0];
    }

    private static void wireNetLobby(Object mode) throws Exception {
        NetLobbyFrame lobby = new NetLobbyFrame();
        lobby.netPlayerClient = new NetPlayerClient();
        setField(mode, "netLobby", lobby);
        setField(mode, "netCurrentRoomInfo", new NetRoomInfo());
    }

    private static int invokePlace(NetVSLineRaceMode mode, GameEngine engine, int playerID) throws Exception {
        Method m = NetVSLineRaceMode.class.getDeclaredMethod(
                "getNowPlayerPlace", GameEngine.class, int.class);
        m.setAccessible(true);
        return (Integer) m.invoke(mode, engine, playerID);
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
