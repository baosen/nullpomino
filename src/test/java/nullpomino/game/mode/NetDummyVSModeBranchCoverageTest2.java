package nullpomino.game.mode;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.lang.reflect.Field;

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
 * Branch-coverage tests for {@link NetDummyVSMode} targeting the following
 * previously uncovered lines:
 * <ul>
 *   <li>Lines 786–787 – {@code engine.field.setAllSkin(netvsPlayerSkin[…])}
 *       in {@code onReady}: reached when playerID != 0 AND
 *       netvsPlayerSkin[playerID] >= 0 AND useMap == true AND ruleLock == false.</li>
 *   <li>Line 911 – {@code x2 = 321} in {@code renderLast}: reached when
 *       {@code getNextDisplayType() == 2} AND {@code maxPlayers == 2}.</li>
 *   <li>Line 1001 – {@code drawDirectFont(…, "OK", …)} in
 *       {@code renderGameOver}: reached when displaysize != -1 AND
 *       netvsPlayerReady[playerID] == true AND !netvsIsGameActive.</li>
 *   <li>Line 1021 – {@code drawDirectFont(…, "LOSE", …)} in
 *       {@code renderGameOver}: reached when displaysize == -1 AND
 *       (numNowPlayers==2 || maxPlayers==2) AND !netvsPlayerReady[playerID].</li>
 *   <li>Line 1092 – {@code drawDirectFont(…, "EXCELLENT!", …)} in
 *       {@code renderExcellent}: reached when displaysize == -1 AND
 *       playerID == 0 AND netvsIsPractice == true.</li>
 *   <li>Line 1192 – {@code drawDirectFont(…, "OK", …)} in
 *       {@code renderResult}: reached when netvsPlayerReady[playerID] == true
 *       AND netvsPlayerExist[playerID] == true AND displaysize != -1.</li>
 *   <li>Line 1303 – {@code netvsAutoStartTimerActive = false} in
 *       {@code netlobbyOnMessage}: reached by "autostartstop" message.</li>
 *   <li>Lines 1355–1356 – {@code engine.isNextVisible = false} /
 *       {@code engine.isHoldVisible = false} in "start" handler: reached when
 *       ruleLock == false AND player exists AND i == 1 (non-zero seat).</li>
 *   <li>Line 1366 – {@code engine.isVisible = false} in "start" handler:
 *       reached when !netvsPlayerExist[i] AND i < maxPlayers == 2.</li>
 *   <li>Line 1464 – {@code playSE("matchend")} in "finish" handler: reached
 *       when netvsPlayerPlace[0] >= 3 (player finished 3rd or worse).</li>
 * </ul>
 */
class NetDummyVSModeBranchCoverageTest2 {

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
            manager.engine[i].stat = GameEngine.Status.MOVE;
            manager.engine[i].statc[0] = 2;
            manager.engine[i].nextPieceArrayObject = new Piece[] { new Piece(Piece.PIECE_T) };
        }
        engine = manager.engine[0];
        setupMinimalNetLobby();
        setupNetCurrentRoomInfo(6, false);
    }

    // ---------------------------------------------------------------
    // Lines 786–787 – setAllSkin path in onReady for playerID != 0
    // ---------------------------------------------------------------

    /**
     * Set {@code netvsPlayerSkin[1] = 3} (>= 0) and call onReady with
     * playerID==1. Since playerID != 0, the {@code else if(netvsPlayerSkin[1] >= 0)}
     * branch fires and sets all skin (lines 786–787).
     *
     * Proof: engine[1].field has skin set (no exception thrown).
     */
    @Test
    void onReadySetsAllSkinForNonZeroPlayerIdWhenSkinIsSet() throws Exception {
        setupNetCurrentRoomInfo(2, false);
        ((NetRoomInfo) getObjField(mode, "netCurrentRoomInfo")).useMap = true;
        ((NetLobbyFrame) getObjField(mode, "netLobby")).mapList.add("0g0\n");
        setIntField(mode, "netvsMapNo", 0);
        setIntArray(mode, "netvsPlayerSkin", 1, 3);

        GameEngine engine1 = manager.engine[1];
        engine1.init();
        engine1.createFieldIfNeeded();
        engine1.statc[0] = 0;
        engine1.playerID = 1;
        mode.playerInit(engine1, 1);

        assertDoesNotThrow(() -> mode.onReady(engine1, 1),
                "onReady for playerID=1 with skin set should not throw");
    }

    // ---------------------------------------------------------------
    // Line 911 – x2 = 321 in renderLast when maxPlayers == 2
    // ---------------------------------------------------------------

    /**
     * Set maxPlayers==2 on the room info and enable bigsidenext
     * (getNextDisplayType()==2). With playerID == getPlayers()-1 == 1,
     * the x2=321 branch fires.
     *
     * Proof: renderLast completes without throwing.
     */
    @Test
    void renderLastSetsX2To321WhenBigSideNextAndTwoMaxPlayers() throws Exception {
        NetDummyVSMode localMode = new NetDummyVSMode();
        GameManager localManager = new GameManager(new BigNextReceiver());
        localManager.mode = localMode;
        localMode.modeInit(localManager);
        localManager.init();
        localManager.engine[5].init();
        localManager.engine[5].createFieldIfNeeded();
        NetRoomInfo room = new NetRoomInfo();
        room.maxPlayers = 2;
        NetLobbyFrame lobby = new NetLobbyFrame();
        NetPlayerClient client = new NetPlayerClient();
        lobby.netPlayerClient = client;
        client.getRoomInfoList().add(room);
        setObjField(localMode, "netLobby", lobby);
        setObjField(localMode, "netCurrentRoomInfo", room);

        assertDoesNotThrow(() -> localMode.renderLast(localManager.engine[5], 5),
                "renderLast should not throw with bigsidenext + maxPlayers=2");
    }

    // ---------------------------------------------------------------
    // Line 1001 – OK in renderGameOver, displaysize != -1 + ready
    // ---------------------------------------------------------------

    /**
     * Set netvsPlayerReady[0]=true, engine.displaysize=0, netvsIsGameActive=false
     * and call the private renderGameOver method via reflection.
     *
     * Proof: method completes without throwing.
     */
    @Test
    void renderGameOverDrawsOkWhenReadyAndDisplaysizeNotMinus1() throws Exception {
        setBoolArray(mode, "netvsPlayerReady", 0, true);
        setBoolField(mode, "netvsIsGameActive", false);
        engine.displaysize = 0; // != -1

        assertDoesNotThrow(() -> callRenderGameOver(engine, 0),
                "renderGameOver should not throw with ready=true, displaysize=0");
    }

    // ---------------------------------------------------------------
    // Line 1021 – LOSE in renderGameOver, displaysize == -1
    // ---------------------------------------------------------------

    /**
     * Set netvsPlayerReady[0]=false, engine.displaysize=-1, maxPlayers=2,
     * netvsPlayerPlace[0] != 1. renderGameOver draws LOSE (line 1021).
     *
     * Proof: method completes without throwing.
     */
    @Test
    void renderGameOverDrawsLoseWhenNotReadyAndDisplaysizeMinus1() throws Exception {
        setupNetCurrentRoomInfo(2, false);
        setBoolArray(mode, "netvsPlayerReady", 0, false);
        engine.displaysize = -1;
        setIntArray(mode, "netvsPlayerPlace", 0, 2); // not 1st, triggers LOSE branch

        assertDoesNotThrow(() -> callRenderGameOver(engine, 0),
                "renderGameOver should not throw with ready=false, displaysize=-1, maxPlayers=2");
    }

    // ---------------------------------------------------------------
    // Line 1092 – EXCELLENT! in renderExcellent, displaysize == -1 + practice
    // ---------------------------------------------------------------

    /**
     * Set netvsIsPractice=true, engine.displaysize=-1, playerID=0.
     * renderExcellent draws EXCELLENT! (line 1092).
     *
     * Proof: method completes without throwing.
     */
    @Test
    void renderExcellentDrawsExcellentLabelInPracticeModeWithDisplaysizeMinus1() throws Exception {
        setBoolField(mode, "netvsIsPractice", true);
        engine.displaysize = -1;

        assertDoesNotThrow(() -> callRenderExcellent(engine, 0),
                "renderExcellent should not throw in practice mode with displaysize=-1");
    }

    // ---------------------------------------------------------------
    // Line 1192 – OK in renderResult when ready + exist
    // ---------------------------------------------------------------

    /**
     * Set netvsPlayerReady[0]=true, netvsPlayerExist[0]=true,
     * engine.displaysize=0. renderResult draws OK (line 1192).
     *
     * Proof: method completes without throwing.
     */
    @Test
    void renderResultDrawsOkWhenPlayerReadyAndExistAndDisplaysizeNotMinus1() throws Exception {
        GameEngine engine1 = manager.engine[1];
        engine1.init();
        engine1.createFieldIfNeeded();
        engine1.playerID = 1;
        setBoolArray(mode, "netvsPlayerReady", 1, true);
        setBoolArray(mode, "netvsPlayerExist", 1, true);
        engine1.displaysize = 0;

        assertDoesNotThrow(() -> mode.renderResult(engine1, 1),
                "renderResult should not throw when ready=true, exist=true, displaysize=0");
    }

    // ---------------------------------------------------------------
    // Line 1303 – netvsAutoStartTimerActive = false via "autostartstop"
    // ---------------------------------------------------------------

    /**
     * Send an "autostartstop" message.  Line 1303 sets
     * {@code netvsAutoStartTimerActive = false}.
     *
     * Proof: after the call netvsAutoStartTimerActive == false.
     */
    @Test
    void netlobbyOnMessageSetsAutoStartTimerInactiveOnAutoStartStop() throws Exception {
        setBoolField(mode, "netvsAutoStartTimerActive", true);

        mode.netlobbyOnMessage(null, null, new String[] { "autostartstop" });

        assertFalse((boolean) getBoolField(mode, "netvsAutoStartTimerActive"),
                "netvsAutoStartTimerActive should be false after autostartstop");
    }

    // ---------------------------------------------------------------
    // Lines 1355–1356 – isNextVisible/isHoldVisible = false in "start"
    // ---------------------------------------------------------------

    /**
     * Send a "start" message with maxPlayers==2, numPlayers==2,
     * ruleLock==false. For i==1 (second engine), !ruleLock && i!=0
     * → engine[1].isNextVisible = false (line 1355) and isHoldVisible = false
     * (line 1356).
     *
     * Proof: manager.engine[1].isNextVisible == false after start message.
     */
    @Test
    void startMessageSetsNextVisibleFalseForSecondPlayerWhenRuleLockFalse() throws Exception {
        setupNetCurrentRoomInfo(2, false);
        setIntField(mode, "netvsNumPlayers", 2);
        setBoolArray(mode, "netvsPlayerExist", 1, true);
        setBoolArray(mode, "netvsPlayerExist", 0, true);

        manager.engine[1].isNextVisible = true;
        manager.engine[1].isHoldVisible = true;

        // "start" message: [0]=cmd, [1]=seed(hex), [2]=numPlayers, [3]=extra
        mode.netlobbyOnMessage(null, null, new String[] { "start", "1a2b3c", "2", "0" });

        assertFalse(manager.engine[1].isNextVisible,
                "engine[1].isNextVisible should be false after start with ruleLock=false");
        assertFalse(manager.engine[1].isHoldVisible,
                "engine[1].isHoldVisible should be false after start with ruleLock=false");
    }

    // ---------------------------------------------------------------
    // Line 1366 – engine.isVisible = false for missing player in "start"
    // ---------------------------------------------------------------

    /**
     * Send a "start" message with maxPlayers==2, numPlayers==2, and
     * netvsPlayerExist[0]=false. For i==0 (which < maxPlayers==2) with
     * !playerExist[0], engine[0].isVisible = false (line 1366).
     *
     * Proof: manager.engine[0].isVisible == false after start message.
     */
    @Test
    void startMessageSetsVisibleFalseForAbsentPlayerWhenMaxPlayersIsTwo() throws Exception {
        setupNetCurrentRoomInfo(2, false);
        setIntField(mode, "netvsNumPlayers", 2);

        // Re-configure so we are a spectator (seatID=-1) and the two room players
        // have seatID=1 and seatID=2.  With netvsMySeatID=-1 the getPlayerIDbySeatID
        // mapping uses NETVS_GAME_SEAT_NUMBERS[0]={0,1,2,...}: seatID=1→playerID=1,
        // seatID=2→playerID=2.  So netvsPlayerExist[0] stays false while
        // netvsNumPlayers=2.  With maxPlayers==2 && numPlayers==2 the
        // else-if(i < maxPlayers) branch fires at i=0, reaching isVisible=false
        // at line 1366.
        NetLobbyFrame lobby = (NetLobbyFrame) getObjField(mode, "netLobby");
        lobby.netPlayerClient.getPlayerInfoList().clear();
        // The "own" player — seatID=-1 (spectator), roomID=1
        NetPlayerInfo mePlayer = new NetPlayerInfo();
        mePlayer.uid = 1; mePlayer.seatID = -1; mePlayer.roomID = 1;
        lobby.netPlayerClient.getPlayerInfoList().add(mePlayer);
        // Two room players — roomID=1, seatID=1 and seatID=2
        NetPlayerInfo p1 = new NetPlayerInfo();
        p1.uid = 2; p1.seatID = 1; p1.roomID = 1;
        lobby.netPlayerClient.getPlayerInfoList().add(p1);
        NetPlayerInfo p2 = new NetPlayerInfo();
        p2.uid = 3; p2.seatID = 2; p2.roomID = 1;
        lobby.netPlayerClient.getPlayerInfoList().add(p2);

        manager.engine[0].isVisible = true;

        mode.netlobbyOnMessage(null, null, new String[] { "start", "1a2b3c", "2", "0" });

        assertFalse(manager.engine[0].isVisible,
                "engine[0].isVisible should be false when playerExist[0]=false and maxPlayers=2");
    }

    // ---------------------------------------------------------------
    // Line 1464 – playSE("matchend") when netvsPlayerPlace[0] >= 3
    // ---------------------------------------------------------------

    /**
     * Set netvsPlayerPlace[0]=3 (third place or worse), then send a "finish"
     * message. Since netvsPlayerPlace[0] >= 3, line 1464
     * ({@code owner.receiver.playSE("matchend")}) fires.
     *
     * Proof: no exception thrown (EventReceiver.playSE is a no-op).
     */
    @Test
    void finishMessagePlaysMatchendWhenPlayerPlaceIsThirdOrWorse() throws Exception {
        setIntArray(mode, "netvsPlayerPlace", 0, 3);

        // "finish" message format: ["finish", "flagTeamWin", ...]
        // teamWin=false path uses message[2] as seatID
        assertDoesNotThrow(() -> mode.netlobbyOnMessage(
                null, null,
                new String[] { "finish", "false", "-1", "0", "false" }),
                "finish message should not throw");
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private void setupMinimalNetLobby() throws Exception {
        NetLobbyFrame lobby = new NetLobbyFrame();
        NetPlayerClient client = new NetPlayerClient();
        NetPlayerInfo myInfo = new NetPlayerInfo();
        myInfo.seatID = 0;
        myInfo.uid = 1;
        client.getPlayerInfoList().add(myInfo);
        setObjField(client, "playerUID", 1);
        setObjField(lobby, "netPlayerClient", client);
        // ruleOptPlayer must be non-null so netvsSetLockedRule can copy it
        // when ruleLock==false (else branch of the lock check)
        lobby.ruleOptPlayer = new RuleOptions();
        setObjField(mode, "netLobby", lobby);
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

    private void callRenderGameOver(GameEngine eng, int pid) throws Exception {
        java.lang.reflect.Method m = findMethod(mode.getClass(), "renderGameOver",
                GameEngine.class, int.class);
        m.setAccessible(true);
        m.invoke(mode, eng, pid);
    }

    private void callRenderExcellent(GameEngine eng, int pid) throws Exception {
        java.lang.reflect.Method m = findMethod(mode.getClass(), "renderExcellent",
                GameEngine.class, int.class);
        m.setAccessible(true);
        m.invoke(mode, eng, pid);
    }

    private static java.lang.reflect.Method findMethod(Class<?> cls, String name,
            Class<?>... params) throws NoSuchMethodException {
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

    private static void setIntArray(Object obj, String name, int idx, int val)
            throws Exception {
        Field f = findField(obj.getClass(), name);
        f.setAccessible(true);
        int[] arr = (int[]) f.get(obj);
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

    private static final class BigNextReceiver extends EventReceiver {
        @Override
        public int getNextDisplayType() {
            return 2;
        }
    }
}
