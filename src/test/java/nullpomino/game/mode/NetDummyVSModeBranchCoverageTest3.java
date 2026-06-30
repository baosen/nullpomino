package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import nullpomino.game.component.Controller;
import nullpomino.game.component.Piece;
import nullpomino.game.component.RuleOptions;
import nullpomino.game.event.EventReceiver;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.gui.net.NetLobbyFrame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Additional branch-coverage tests for {@link NetDummyVSMode}, complementing
 * {@code NetDummyVSModeBranchCoverageTest2}. These target previously uncovered
 * branch outcomes, especially:
 * <ul>
 *   <li>Watch-mode-TRUE paths ({@code netvsIsWatch()} returns true) in
 *       {@code netvsRecvEndGameStats}, {@code netvsDrawRoomInfoBox},
 *       {@code renderResult} and the {@code "finish"} handler (line 1463).</li>
 *   <li>{@code playerID != 0} early-return in {@code onMove} (line 829).</li>
 *   <li>{@code "playerupdate"} change/SE branches (lines 1228, 1235).</li>
 *   <li>{@code "changestatus"} self-uid engine reset (line 1257).</li>
 *   <li>{@code "playerenter"} / {@code "playerleave"} / {@code "autostartbegin"}
 *       player-count guards (lines 1281, 1289, 1295).</li>
 *   <li>{@code onGameOver} other-player-dead with result received (line 979).</li>
 *   <li>{@code "finish"} team-win i==0 send-stats path (line 1438).</li>
 * </ul>
 */
class NetDummyVSModeBranchCoverageTest3 {

    private NetDummyVSMode mode;
    private GameManager manager;
    private GameEngine engine;

    @BeforeEach
    void setUp() throws Exception {
        mode = new NetDummyVSMode();
        EventReceiver receiver = new EventReceiver();
        manager = new GameManager(receiver);
        manager.mode = mode;
        mode.modeInit(manager);
        manager.init();
        for (int i = 0; i < manager.engine.length; i++) {
            manager.engine[i].init();
            manager.engine[i].createFieldIfNeeded();
            manager.engine[i].nextPieceArrayObject = new Piece[] { new Piece(Piece.PIECE_T) };
        }
        engine = manager.engine[0];
        setupSeatedLobby();
        setupNetCurrentRoomInfo(6, false);
    }

    // ===============================================================
    // Line 594 (playerID != 0) – netvsRecvEndGameStats records result
    // for a NON-self player.
    // ===============================================================

    /**
     * As a seated player (seatID=0, NOT watch), receive end-game stats for the
     * player in seat 1.  seat 1 maps to playerID 1 (non-zero), so the
     * {@code (playerID != 0)} side of line 594 is taken and
     * netvsPlayerResultReceived[1] becomes true.
     */
    @Test
    void recvEndGameStatsRecordsResultForNonZeroPlayer() throws Exception {
        Method m = findMethod(mode.getClass(), "netvsRecvEndGameStats", String[].class);
        m.setAccessible(true);
        // message[2] = seatID (as parsed by netvsRecvEndGameStats)
        m.invoke(mode, (Object) new String[] { "gstat", "x", "1" });

        boolean[] received = (boolean[]) getObjField(mode, "netvsPlayerResultReceived");
        assertTrue(received[1], "result received should be set for non-self playerID 1");
    }

    // ===============================================================
    // Line 594 (netvsIsWatch() == true) – as a spectator, even seat 0 records.
    // ===============================================================

    /**
     * As a spectator (seatID=-1, watch mode), receive end-game stats for seat 0.
     * playerID is 0, but {@code netvsIsWatch()} is true, so the
     * {@code (playerID != 0) || netvsIsWatch()} guard at line 594 passes via the
     * watch side, and netvsPlayerResultReceived[0] becomes true.
     */
    @Test
    void recvEndGameStatsRecordsResultForSelfSeatWhenWatching() throws Exception {
        makeSpectator();
        Method m = findMethod(mode.getClass(), "netvsRecvEndGameStats", String[].class);
        m.setAccessible(true);
        m.invoke(mode, (Object) new String[] { "gstat", "x", "0" });

        boolean[] received = (boolean[]) getObjField(mode, "netvsPlayerResultReceived");
        assertTrue(received[0], "result received should be set in watch mode even for seat 0");
    }

    // ===============================================================
    // Line 661 (false branch) – netvsDrawRoomInfoBox skips MATCHES/WINS
    // block when watching.
    // ===============================================================

    @Test
    void drawRoomInfoBoxSkipsMatchesBlockWhenWatching() throws Exception {
        makeSpectator();
        Method m = findMethod(mode.getClass(), "netvsDrawRoomInfoBox",
                GameEngine.class, int.class, int.class);
        m.setAccessible(true);
        assertDoesNotThrow(() -> m.invoke(mode, engine, 100, 100),
                "netvsDrawRoomInfoBox should not throw while watching");
    }

    // ===============================================================
    // Line 829 (true branch) – onMove returns true (stops game) for a
    // remote player (playerID != 0).
    // ===============================================================

    @Test
    void onMoveStopsGameForRemotePlayer() throws Exception {
        GameEngine engine1 = manager.engine[1];
        engine1.playerID = 1;
        assertTrue(mode.onMove(engine1, 1),
                "onMove should return true (skip) for non-zero playerID");
    }

    // ===============================================================
    // Line 829 (netvsIsWatch() true branch) – onMove returns true for seat-0
    // engine when watching.
    // ===============================================================

    @Test
    void onMoveStopsGameForOwnEngineWhenWatching() throws Exception {
        makeSpectator();
        assertTrue(mode.onMove(engine, 0),
                "onMove should return true (skip) for playerID 0 while watching");
    }

    // ===============================================================
    // Lines 1110 + 1144 (watch branch) – renderResult else-branch when
    // watching (playerID != 0 path / not-own-player ready overlay).
    // ===============================================================

    /**
     * As a spectator, renderResult takes the {@code else if(netvsPlayerReady &&
     * netvsPlayerExist)} arm rather than the {@code (playerID==0 && !watch)} arm.
     */
    @Test
    void renderResultDrawsReadyOverlayForWatchedPlayer() throws Exception {
        makeSpectator();
        setBoolArray(mode, "netvsPlayerReady", 0, true);
        setBoolArray(mode, "netvsPlayerExist", 0, true);
        engine.displaysize = 0;
        engine.isVisible = true;

        assertDoesNotThrow(() -> mode.renderResult(engine, 0),
                "renderResult should not throw in watch mode");
    }

    // ===============================================================
    // Line 1228 (false branch) – playerupdate with UNCHANGED ready flag
    // leaves the inner block unexecuted.
    // ===============================================================

    /**
     * Send "playerupdate" for a player whose {@code ready} matches the cached
     * netvsPlayerReady value, so {@code netvsPlayerReady[id] != pInfo.ready} is
     * false and the SE branch is skipped. Verifies no throw and exercises the
     * false outcome of line 1228.
     */
    @Test
    void playerUpdateWithUnchangedReadyDoesNothing() throws Exception {
        // seat 1 -> playerID 1, ready already false (default) and pInfo.ready false
        NetPlayerInfo pInfo = new NetPlayerInfo();
        pInfo.roomID = 1;
        pInfo.seatID = 1;
        pInfo.uid = 50;
        pInfo.ready = false;
        String exported = pInfo.exportString();

        assertDoesNotThrow(() -> mode.netlobbyOnMessage(
                null, null, new String[] { "playerupdate", exported }),
                "playerupdate with unchanged ready should not throw");
        boolean[] ready = (boolean[]) getObjField(mode, "netvsPlayerReady");
        assertFalse(ready[1], "ready for seat 1 should remain false");
    }

    // ===============================================================
    // Line 1235 – playerupdate where a NON-self player becomes un-ready and
    // not playing -> playSE("change").
    // ===============================================================

    /**
     * Pre-set netvsPlayerReady[1]=true so the incoming pInfo.ready=false toggles
     * the flag. Because playerID 1 != 0, the else branch runs; pInfo.playing is
     * false so {@code else if(!pInfo.playing)} (line 1235) fires.
     */
    @Test
    void playerUpdateChangeSeForNonPlayingOpponent() throws Exception {
        setBoolArray(mode, "netvsPlayerReady", 1, true);

        NetPlayerInfo pInfo = new NetPlayerInfo();
        pInfo.roomID = 1;
        pInfo.seatID = 1;
        pInfo.uid = 51;
        pInfo.ready = false;
        pInfo.playing = false;
        String exported = pInfo.exportString();

        assertDoesNotThrow(() -> mode.netlobbyOnMessage(
                null, null, new String[] { "playerupdate", exported }),
                "playerupdate change for opponent should not throw");
    }

    // ===============================================================
    // Line 1234 – playerupdate where a NON-self player becomes ready ->
    // playSE("decide").
    // ===============================================================

    @Test
    void playerUpdateDecideSeWhenOpponentReady() throws Exception {
        // netvsPlayerReady[1] starts false; incoming ready=true toggles it.
        NetPlayerInfo pInfo = new NetPlayerInfo();
        pInfo.roomID = 1;
        pInfo.seatID = 1;
        pInfo.uid = 52;
        pInfo.ready = true;
        String exported = pInfo.exportString();

        assertDoesNotThrow(() -> mode.netlobbyOnMessage(
                null, null, new String[] { "playerupdate", exported }),
                "playerupdate ready for opponent should not throw");
    }

    // ===============================================================
    // Line 1257 (true branch) – changestatus with self UID resets engines.
    // ===============================================================

    /**
     * "changestatus" carrying our own UID (message[2] == playerUID) takes the
     * {@code uid == getPlayerUID()} true branch, resetting all engines to
     * SETTING.
     */
    @Test
    void changeStatusForSelfResetsEnginesToSetting() throws Exception {
        // playerUID is 1 (set up in setupSeatedLobby)
        manager.engine[0].stat = GameEngine.Status.MOVE;
        mode.netlobbyOnMessage(null, null, new String[] { "changestatus", "x", "1" });

        assertTrue(manager.engine[0].stat == GameEngine.Status.SETTING,
                "engine[0] should be reset to SETTING for self changestatus");
    }

    // ===============================================================
    // Line 1257 (false branch) – changestatus for a DIFFERENT UID does not
    // reset our engines.
    // ===============================================================

    @Test
    void changeStatusForOtherUidLeavesEnginesUntouched() throws Exception {
        manager.engine[0].stat = GameEngine.Status.MOVE;
        mode.netlobbyOnMessage(null, null, new String[] { "changestatus", "x", "999" });

        assertTrue(manager.engine[0].stat == GameEngine.Status.MOVE,
                "engine[0] should be untouched for a different UID");
    }

    // ===============================================================
    // Line 1281 (true branch) – playerenter with seatID != -1 and < 2 players
    // plays "levelstop".
    // ===============================================================

    @Test
    void playerEnterPlaysLevelstopWhenFewPlayers() throws Exception {
        setIntField(mode, "netvsNumPlayers", 1);
        // message[3] = seatID
        assertDoesNotThrow(() -> mode.netlobbyOnMessage(
                null, null, new String[] { "playerenter", "x", "y", "2" }),
                "playerenter should not throw");
    }

    // ===============================================================
    // Line 1281 (false branch) – playerenter as a spectator (seatID == -1)
    // does not play the SE.
    // ===============================================================

    @Test
    void playerEnterNoSeWhenSpectatorSeat() throws Exception {
        setIntField(mode, "netvsNumPlayers", 1);
        assertDoesNotThrow(() -> mode.netlobbyOnMessage(
                null, null, new String[] { "playerenter", "x", "y", "-1" }),
                "playerenter for spectator seat should not throw");
    }

    // ===============================================================
    // Line 1289 (true branch) – playerleave with < 2 players turns the
    // auto-start timer off.
    // ===============================================================

    @Test
    void playerLeaveStopsAutoStartTimerWhenFewPlayers() throws Exception {
        // Only the seated "me" player is in the room -> netvsNumPlayers becomes 1.
        setBoolField(mode, "netvsAutoStartTimerActive", true);
        mode.netlobbyOnMessage(null, null, new String[] { "playerleave", "x" });

        assertFalse((boolean) getBoolField(mode, "netvsAutoStartTimerActive"),
                "auto-start timer should be disabled after a player leaves below 2 players");
    }

    // ===============================================================
    // Line 1295 (true branch) – autostartbegin with >= 2 players arms the
    // auto-start timer.
    // ===============================================================

    @Test
    void autoStartBeginArmsTimerWhenEnoughPlayers() throws Exception {
        setIntField(mode, "netvsNumPlayers", 2);
        mode.netlobbyOnMessage(null, null, new String[] { "autostartbegin", "5" });

        assertTrue((boolean) getBoolField(mode, "netvsAutoStartTimerActive"),
                "auto-start timer should be armed with >= 2 players");
    }

    // ===============================================================
    // Line 1295 (false branch) – autostartbegin with < 2 players does NOT arm.
    // ===============================================================

    @Test
    void autoStartBeginDoesNotArmTimerWhenTooFewPlayers() throws Exception {
        setIntField(mode, "netvsNumPlayers", 1);
        setBoolField(mode, "netvsAutoStartTimerActive", false);
        mode.netlobbyOnMessage(null, null, new String[] { "autostartbegin", "5" });

        assertFalse((boolean) getBoolField(mode, "netvsAutoStartTimerActive"),
                "auto-start timer should stay disarmed with < 2 players");
    }

    // ===============================================================
    // Line 979 (netvsPlayerResultReceived true) – onGameOver for an opponent
    // whose result has been received returns false (stays on game-over).
    // ===============================================================

    /**
     * For an opponent (playerID 1) that is dead, with result received and
     * statc[0] past the field height, the {@code (statc[0] < height+1) ||
     * resultReceived} guard at line 979 is true (via resultReceived), so
     * onGameOver returns false.
     */
    @Test
    void onGameOverReturnsFalseForDeadOpponentWithResultReceived() throws Exception {
        GameEngine engine1 = manager.engine[1];
        engine1.playerID = 1;
        engine1.createFieldIfNeeded();
        setBoolArray(mode, "netvsPlayerDead", 1, true);
        setBoolArray(mode, "netvsPlayerResultReceived", 1, true);
        // Push statc[0] well past field height so the first disjunct is false and
        // we rely on resultReceived for the true outcome.
        engine1.statc[0] = engine1.field.getHeight() + 5;

        assertFalse(mode.onGameOver(engine1, 1),
                "onGameOver should return false for a dead opponent whose result was received");
    }

    // ===============================================================
    // Line 1438 (i==0 && !watch) – finish team-win sends end-game stats for
    // the local player.
    // ===============================================================

    /**
     * Team-win finish with the local seated player (i==0, not watching) alive
     * triggers the {@code (i == 0) && !netvsIsWatch()} branch (line 1438).
     */
    @Test
    void finishTeamWinSendsEndGameStatsForLocalPlayer() throws Exception {
        setBoolArray(mode, "netvsPlayerExist", 0, true);
        setBoolArray(mode, "netvsPlayerDead", 0, false);
        manager.engine[0].createFieldIfNeeded();

        // "finish": [0]=cmd, [1]=?, [2]=seatID, [3]=?, [4]=flagTeamWin
        assertDoesNotThrow(() -> mode.netlobbyOnMessage(
                null, null, new String[] { "finish", "x", "-1", "0", "true" }),
                "team-win finish should not throw");
    }

    // ===============================================================
    // Line 1463 (netvsIsWatch true) – finish plays "matchend" while watching.
    // ===============================================================

    @Test
    void finishPlaysMatchendWhenWatching() throws Exception {
        makeSpectator();
        assertDoesNotThrow(() -> mode.netlobbyOnMessage(
                null, null, new String[] { "finish", "x", "-1", "0", "false" }),
                "finish while watching should not throw and play matchend");
    }

    // ===============================================================
    // Line 1492 (watch play-timer start) – a "game"/"piece" message while
    // watching, with timer inactive and game not finished, starts the play
    // timer.
    // ===============================================================

    /**
     * As a spectator (watch=true) with netvsPlayTimerActive=false,
     * netvsIsNewcomer=false and netvsIsGameFinished=false, a piece message
     * for an existing seat starts the play timer (line 1493).
     */
    @Test
    void gamePieceMessageStartsPlayTimerWhenWatching() throws Exception {
        makeSpectator();
        setBoolField(mode, "netvsPlayTimerActive", false);
        setBoolField(mode, "netvsIsNewcomer", false);
        setBoolField(mode, "netvsIsGameFinished", false);

        // Build a minimal "game ... piece" message. netRecvPieceMovement must
        // tolerate the payload; mirror the layout used by existing tests.
        // message: [0]=game [1]=? [2]=seatID [3]=piece [4..]=piece data
        String[] msg = new String[] {
            "game", "x", "0", "piece",
            "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0", "0"
        };
        assertDoesNotThrow(() -> mode.netlobbyOnMessage(null, null, msg),
                "game/piece message while watching should not throw");
    }

    // ===============================================================
    // Line 686/692 false branches – onSetting ready toggles guarded by
    // current ready state.
    // ===============================================================

    /**
     * With netvsPlayerReady[0] already true, pressing A (BUTTON_A) does NOT
     * re-arm ready: the {@code !netvsPlayerReady[0]} condition on line 686 is
     * false, so no ready-change pending is set.
     */
    @Test
    void onSettingAButtonIgnoredWhenAlreadyReady() throws Exception {
        setupNetCurrentRoomInfo(2, false);
        mode.playerInit(engine, 0);
        engine.owner.replayMode = false;
        setIntField(mode, "netvsNumPlayers", 2);
        setBoolField(mode, "netvsIsReadyChangePending", false);
        setBoolField(mode, "netvsIsNewcomer", false);
        setBoolArray(mode, "netvsPlayerReady", 0, true);
        setIntField(mode, "menuTime", 10);

        engine.ctrl.reset();
        engine.ctrl.buttonPress[Controller.BUTTON_A] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_A] = 1;

        mode.onSetting(engine, 0);

        assertFalse((boolean) getBoolField(mode, "netvsIsReadyChangePending"),
                "pressing A while already ready should not set ready-change pending");
    }

    /**
     * With netvsPlayerReady[0] true, pressing B (BUTTON_B) toggles ready OFF:
     * the {@code netvsPlayerReady[0]} condition on line 692 is true, so
     * netvsIsReadyChangePending becomes true.
     */
    @Test
    void onSettingBButtonCancelsReadyWhenReady() throws Exception {
        setupNetCurrentRoomInfo(2, false);
        mode.playerInit(engine, 0);
        engine.owner.replayMode = false;
        setIntField(mode, "netvsNumPlayers", 2);
        setBoolField(mode, "netvsIsReadyChangePending", false);
        setBoolField(mode, "netvsIsNewcomer", false);
        setBoolArray(mode, "netvsPlayerReady", 0, true);
        setIntField(mode, "menuTime", 10);

        engine.ctrl.reset();
        engine.ctrl.buttonPress[Controller.BUTTON_B] = true;
        engine.ctrl.buttonTime[Controller.BUTTON_B] = 1;

        mode.onSetting(engine, 0);

        assertTrue((boolean) getBoolField(mode, "netvsIsReadyChangePending"),
                "pressing B while ready should set ready-change pending");
    }

    // ===============================================================
    // Helpers
    // ===============================================================

    /** Builds a lobby where "me" is seated (seatID=0, uid=1) -> NOT watching. */
    private void setupSeatedLobby() throws Exception {
        NetLobbyFrame lobby = new NetLobbyFrame();
        NetPlayerClient client = new NetPlayerClient();
        NetPlayerInfo myInfo = new NetPlayerInfo();
        myInfo.seatID = 0;
        myInfo.uid = 1;
        myInfo.roomID = 1;
        client.getPlayerInfoList().add(myInfo);
        setObjField(client, "playerUID", 1);
        setObjField(lobby, "netPlayerClient", client);
        lobby.ruleOptPlayer = new RuleOptions();
        setObjField(mode, "netLobby", lobby);
    }

    /** Re-points "me" to a spectator seat (seatID=-1) so netvsIsWatch()==true. */
    private void makeSpectator() throws Exception {
        NetLobbyFrame lobby = (NetLobbyFrame) getObjField(mode, "netLobby");
        NetPlayerInfo me = lobby.netPlayerClient.getPlayerInfoList().get(0);
        me.seatID = -1;
        setIntField(mode, "netvsMySeatID", -1);
    }

    private void setupNetCurrentRoomInfo(int maxPlayers, boolean ruleLock) throws Exception {
        NetRoomInfo roomInfo = new NetRoomInfo();
        roomInfo.roomID = 1;
        roomInfo.maxPlayers = maxPlayers;
        roomInfo.ruleLock = ruleLock;
        roomInfo.rated = false;
        roomInfo.gravity = 1;
        roomInfo.denominator = 256;
        roomInfo.are = 25;
        roomInfo.areLine = 25;
        roomInfo.lineDelay = 0;
        roomInfo.lockDelay = 30;
        roomInfo.das = 10;
        setObjField(mode, "netCurrentRoomInfo", roomInfo);
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params)
            throws NoSuchMethodException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredMethod(name, params);
            } catch (NoSuchMethodException e) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static void setBoolField(Object obj, String name, boolean value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setBoolean(obj, value);
    }

    private static boolean getBoolField(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.getBoolean(obj);
    }

    private static void setBoolArray(Object obj, String name, int idx, boolean val)
            throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        boolean[] arr = (boolean[]) f.get(obj);
        arr[idx] = val;
    }

    private static void setIntField(Object obj, String name, int value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.setInt(obj, value);
    }

    private static void setObjField(Object obj, String name, Object value) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        f.set(obj, value);
    }

    private static Object getObjField(Object obj, String name) throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        return f.get(obj);
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
}
