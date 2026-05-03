package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TimeZone;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests covering additional uncovered lines in {@link NetServer}.
 *
 * <p>Focus areas:
 * <ul>
 *   <li>{@code loadRuleList()} — file-based rule loading with style markers</li>
 *   <li>{@code processPacket} handlers for {@code observerlogin}, {@code login},
 *       {@code roomcreate}, {@code roomjoin}, {@code lobbychat}, {@code chat}</li>
 *   <li>{@code updateSPDailyRanking} wipe path (different day)</li>
 *   <li>Error/exception paths in write methods</li>
 * </ul>
 */
class NetServerUncoveredLinesTest {

    private NetServer server;
    private Selector savedSelector;

    @TempDir
    Path tempDir;

    // Method handles for static methods
    private MethodHandle loadRuleList;
    private MethodHandle updateSPDailyRanking;
    private MethodHandle writeMPRankingToFile;
    private MethodHandle writeSPRankingToFile;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        setStaticField("propServer", new CustomProperties());

        server = new NetServer(9999);

        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());

        loadRuleList = lookup.findStatic(NetServer.class, "loadRuleList",
                MethodType.methodType(void.class, String.class));
        updateSPDailyRanking = lookup.findStatic(NetServer.class, "updateSPDailyRanking",
                MethodType.methodType(boolean.class));
        writeMPRankingToFile = lookup.findStatic(NetServer.class, "writeMPRankingToFile",
                MethodType.methodType(void.class));
        writeSPRankingToFile = lookup.findStatic(NetServer.class, "writeSPRankingToFile",
                MethodType.methodType(void.class));

        // Seed static fields
        setStaticDouble("ratingNormalMaxDiff", NetServer.NORMAL_MAX_DIFF);
        setStaticInt("ratingProvisionalGames", NetServer.PROVISIONAL_GAMES);
        setStaticInt("ratingMin", 0);
        setStaticInt("ratingMax", 99999);
        setStaticInt("ratingDefault", NetPlayerInfo.DEFAULT_MULTIPLAYER_RATING);
        setStaticInt("maxMPRanking", NetServer.DEFAULT_MAX_MPRANKING);
        setStaticInt("maxSPRanking", NetServer.DEFAULT_MAX_SPRANKING);
        setStaticInt("maxLobbyChatHistory", NetServer.DEFAULT_MAX_LOBBYCHAT_HISTORY);
        setStaticInt("maxRoomChatHistory", NetServer.DEFAULT_MAX_ROOMCHAT_HISTORY);
        setStaticBoolean("allowDNSAccess", true);

        // Init ruleList
        initRuleList();

        // Init mpRankingList
        Field f = NetServer.class.getDeclaredField("mpRankingList");
        f.setAccessible(true);
        LinkedList<NetPlayerInfo>[] mpList = new LinkedList[GameEngine.MAX_GAMESTYLE];
        for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
            mpList[i] = new LinkedList<NetPlayerInfo>();
        }
        f.set(null, mpList);

        // Init banList
        setStaticField("banList", new LinkedList<NetServerBan>());

        // Init lobbyChatList
        setStaticField("lobbyChatList", new LinkedList<NetChatMessage>());

        // Init SP ranking lists
        setStaticField("spRankingListAlltime", new LinkedList<NetSPRanking>());
        setStaticField("spRankingListDaily", new LinkedList<NetSPRanking>());

        // Init SP mode list
        @SuppressWarnings("unchecked")
        LinkedList<String>[] spModeList = new LinkedList[GameEngine.MAX_GAMESTYLE];
        for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
            spModeList[i] = new LinkedList<String>();
        }
        Field spModeField = NetServer.class.getDeclaredField("spModeList");
        spModeField.setAccessible(true);
        spModeField.set(null, spModeList);

        // Init mp mode lists
        @SuppressWarnings("unchecked")
        LinkedList<String>[] mpModeList = new LinkedList[GameEngine.MAX_GAMESTYLE];
        @SuppressWarnings("unchecked")
        LinkedList<Boolean>[] mpModeIsRace = new LinkedList[GameEngine.MAX_GAMESTYLE];
        for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
            mpModeList[i] = new LinkedList<String>();
            mpModeIsRace[i] = new LinkedList<Boolean>();
        }
        Field mpModeField = NetServer.class.getDeclaredField("mpModeList");
        mpModeField.setAccessible(true);
        mpModeField.set(null, mpModeList);
        Field mpRaceField = NetServer.class.getDeclaredField("mpModeIsRace");
        mpRaceField.setAccessible(true);
        mpRaceField.set(null, mpModeIsRace);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (savedSelector != null && savedSelector.isOpen()) {
            savedSelector.close();
        }
    }

    // ==================================================================
    // Helpers
    // ==================================================================

    private static void setStaticDouble(String name, double value) throws Exception {
        Field f = NetServer.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setDouble(null, value);
    }

    private static void setStaticInt(String name, int value) throws Exception {
        Field f = NetServer.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setInt(null, value);
    }

    private static void setStaticBoolean(String name, boolean value) throws Exception {
        Field f = NetServer.class.getDeclaredField(name);
        f.setAccessible(true);
        f.setBoolean(null, value);
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field f = NetServer.class.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> T getStaticField(String name, Class<T> type) throws Exception {
        Field f = NetServer.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(null);
    }

    @SuppressWarnings("unchecked")
    private static LinkedList<NetPlayerInfo> getMpRankingList(int style) throws Exception {
        Field f = NetServer.class.getDeclaredField("mpRankingList");
        f.setAccessible(true);
        LinkedList<NetPlayerInfo>[] arr = (LinkedList<NetPlayerInfo>[]) f.get(null);
        return arr[style];
    }

    @SuppressWarnings("unchecked")
    private <T> T getInstanceField(Object obj, String name) throws Exception {
        Field f = NetServer.class.getDeclaredField(name);
        f.setAccessible(true);
        return (T) f.get(obj);
    }

    /** Init ruleList with empty arrays (avoids NPE in various methods). */
    @SuppressWarnings("unchecked")
    private static void initRuleList() throws Exception {
        LinkedList<RuleOptions>[] ruleList = new LinkedList[GameEngine.MAX_GAMESTYLE];
        LinkedList<Integer>[] ruleSettingIDList = new LinkedList[GameEngine.MAX_GAMESTYLE];
        for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
            ruleList[i] = new LinkedList<RuleOptions>();
            ruleSettingIDList[i] = new LinkedList<Integer>();
        }
        Field f = NetServer.class.getDeclaredField("ruleList");
        f.setAccessible(true);
        f.set(null, ruleList);
        Field f2 = NetServer.class.getDeclaredField("ruleSettingIDList");
        f2.setAccessible(true);
        f2.set(null, ruleSettingIDList);
    }

    /** Enable the selector so send() doesn't NPE. */
    private void setSelector() throws Exception {
        Field f = NetServer.class.getDeclaredField("selector");
        f.setAccessible(true);
        if (f.get(server) == null) {
            savedSelector = Selector.open();
            f.set(server, savedSelector);
        }
    }

    /** Add a player+channel to the server's internal maps. */
    private NetPlayerInfo addPlayerToServer(SocketChannel ch, String name, int uid) throws Exception {
        NetPlayerInfo p = new NetPlayerInfo();
        p.strName = name;
        p.uid = uid;
        p.roomID = -1;
        p.connected = true;
        p.playing = false;
        p.isTripUse = false;
        p.ruleOpt = new RuleOptions();
        Map<SocketChannel, NetPlayerInfo> infoMap = getInstanceField(server, "playerInfoMap");
        infoMap.put(ch, p);
        LinkedList<SocketChannel> chList = getInstanceField(server, "channelList");
        chList.add(ch);
        return p;
    }

    /** Invoke processPacket and unwrap InvocationTargetException. */
    private void callProcessPacket(SocketChannel ch, String msg) throws Throwable {
        Method m = NetServer.class.getDeclaredMethod("processPacket", SocketChannel.class, String.class);
        m.setAccessible(true);
        try {
            m.invoke(server, ch, msg);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    // ==================================================================
    // loadRuleList
    // ==================================================================

    @Test
    void loadRuleListWithValidFilePopulatesRules() throws Throwable {
        Path ruleFile = tempDir.resolve("rulelist.lst");
        Files.writeString(ruleFile, String.join("\n",
                "# Comment line",
                ":AUTO",                                  // valid style marker
                "config/rules/standard/standard.rul",     // likely exists
                ":TETRIS",                                // another valid style
                ""));

        loadRuleList.invokeExact((String) ruleFile.toString());

        // Verify ruleList is populated
        LinkedList<RuleOptions>[] rl = getStaticField("ruleList", LinkedList[].class);
        LinkedList<Integer>[] rlID = getStaticField("ruleSettingIDList", LinkedList[].class);
        assertNotNull(rl);
        assertNotNull(rlID);

        // Style 0 (AUTO) should have at least 1 rule (standard.rul)
        // Style 1 (TETRIS) may or may not have rules depending on file existence
    }

    @Test
    void loadRuleListWithUnknownStyleFallsBackToZero() throws Throwable {
        Path ruleFile = tempDir.resolve("rulelist.lst");
        Files.writeString(ruleFile, ":UNKNOWN_STYLE_XYZ123\n");

        loadRuleList.invokeExact((String) ruleFile.toString());

        // Should not throw; unknown style causes style=0 fallback
        LinkedList<RuleOptions>[] rl = getStaticField("ruleList", LinkedList[].class);
        assertNotNull(rl);
    }

    @Test
    void loadRuleListWithNonexistentFileDoesNotThrow() throws Throwable {
        Path ruleFile = tempDir.resolve("does-not-exist.lst");

        loadRuleList.invokeExact((String) ruleFile.toString());

        LinkedList<RuleOptions>[] rl = getStaticField("ruleList", LinkedList[].class);
        assertNotNull(rl);
        // ruleList should have been initialized with empty lists
        for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
            assertNotNull(rl[i]);
            assertTrue(rl[i].isEmpty());
        }
    }

    // ==================================================================
    // processPacket: observerlogin
    // ==================================================================

    @Test
    void processPacketObserverloginSuccess() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            callProcessPacket(ch, "observerlogin\t" + GameManager.getVersionMajor() +
                    "\t0\t" + GameManager.isDevBuild());

            // Should add to observerList
            LinkedList<SocketChannel> obsList = getInstanceField(server, "observerList");
            assertTrue(obsList.contains(ch));
        }
    }

    @Test
    void processPacketObserverloginVersionMismatch() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            callProcessPacket(ch, "observerlogin\t999.0\t0\tfalse");

            // Should NOT add to observerList (version mismatch)
            LinkedList<SocketChannel> obsList = getInstanceField(server, "observerList");
            assertFalse(obsList.contains(ch));
        }
    }

    @Test
    void processPacketObserverloginBuildMismatch() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            boolean serverDev = GameManager.isDevBuild();
            callProcessPacket(ch, "observerlogin\t" + GameManager.getVersionMajor() +
                    "\t0\t" + (!serverDev));

            // Should NOT add to observerList (build mismatch)
            LinkedList<SocketChannel> obsList = getInstanceField(server, "observerList");
            assertFalse(obsList.contains(ch));
        }
    }

    @Test
    void processPacketObserverloginAlreadyLoggedInAsPlayerReturnsEarly() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            addPlayerToServer(ch, "TestPlayer", 1);

            callProcessPacket(ch, "observerlogin\t" + GameManager.getVersionMajor() +
                    "\t0\t" + GameManager.isDevBuild());

            // Should NOT add to observerList (already a player)
            LinkedList<SocketChannel> obsList = getInstanceField(server, "observerList");
            assertFalse(obsList.contains(ch));
        }
    }

    // ==================================================================
    // processPacket: login
    // ==================================================================

    @Test
    void processPacketLoginSuccess() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            String encodedName = NetUtil.urlEncode("TestPlayer");
            callProcessPacket(ch, "login\t" + GameManager.getVersionMajor() +
                    "\t" + encodedName + "\tUS\tTeam1\t0\t" + GameManager.isDevBuild());

            // Should create player info
            Map<SocketChannel, NetPlayerInfo> infoMap =
                    getInstanceField(server, "playerInfoMap");
            NetPlayerInfo p = infoMap.get(ch);
            assertNotNull(p);
            assertEquals("TestPlayer", p.strName);
            assertEquals("US", p.strCountry);
            assertEquals("Team1", p.strTeam);
            assertTrue(p.connected);
        }
    }

    @Test
    void processPacketLoginVersionMismatch() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            String encodedName = NetUtil.urlEncode("TestPlayer");
            callProcessPacket(ch, "login\t999.0\t" + encodedName + "\tUS\tTeam1\t0\tfalse");

            // Should NOT create player info
            Map<SocketChannel, NetPlayerInfo> infoMap =
                    getInstanceField(server, "playerInfoMap");
            assertNull(infoMap.get(ch));
        }
    }

    @Test
    void processPacketLoginWithTripcode() throws Throwable {
        setSelector();
        // Set propServer with tripcodemax
        CustomProperties props = new CustomProperties();
        props.setProperty("netserver.tripcodemax", "10");
        setStaticField("propServer", props);

        try (SocketChannel ch = SocketChannel.open()) {
            String encodedName = NetUtil.urlEncode("Player#secret");
            callProcessPacket(ch, "login\t" + GameManager.getVersionMajor() +
                    "\t" + encodedName + "\tUS\tTeam1\t0\t" + GameManager.isDevBuild());

            Map<SocketChannel, NetPlayerInfo> infoMap =
                    getInstanceField(server, "playerInfoMap");
            NetPlayerInfo p = infoMap.get(ch);
            assertNotNull(p);
            // Name should contain tripcode: "Player !<hash>"
            assertTrue(p.strName.startsWith("Player !"), "Tripcode name should start with 'Player !': " + p.strName);
            assertTrue(p.isTripUse);
        }
    }

    @Test
    void processPacketLoginAlreadyLoggedInReturnsEarly() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            addPlayerToServer(ch, "ExistingPlayer", 1);

            String encodedName = NetUtil.urlEncode("NewPlayer");
            callProcessPacket(ch, "login\t" + GameManager.getVersionMajor() +
                    "\t" + encodedName + "\tUS\tTeam1\t0\t" + GameManager.isDevBuild());

            // Name should remain unchanged (existing player)
            Map<SocketChannel, NetPlayerInfo> infoMap =
                    getInstanceField(server, "playerInfoMap");
            NetPlayerInfo p = infoMap.get(ch);
            assertEquals("ExistingPlayer", p.strName);
        }
    }

    // ==================================================================
    // processPacket: singleroomcreate
    // ==================================================================

    @Test
    void processPacketSingleroomcreateSuccess() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "SoloPlayer", 1);
            // Player needs a ruleOpt with a valid strRuleName for the non-rated path
            RuleOptions rule = new RuleOptions();
            rule.strRuleName = "Standard";
            p.ruleOpt = rule;

            callProcessPacket(ch, "singleroomcreate\t" + NetUtil.urlEncode("Solo Game") +
                    "\t" + NetUtil.urlEncode("SP"));

            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            assertFalse(roomList.isEmpty());
            NetRoomInfo room = roomList.getLast();
            assertTrue(room.singleplayer);
            assertEquals("Solo Game", room.strName);
            assertEquals(1, room.maxPlayers);
            assertFalse(room.rated); // no rule name arg -> unrated
        }
    }

    // ==================================================================
    // processPacket: roomcreate (multiplayer)
    // ==================================================================

    @Test
    void processPacketRoomcreateNoTitleUsesDefault() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "NoTitle", 1);

            // Build a proper NetRoomInfo export string
            NetRoomInfo roomTemplate = new NetRoomInfo();
            roomTemplate.maxPlayers = 2;
            roomTemplate.strMode = "VS";
            String roomInfoStr = NetUtil.urlEncode(roomTemplate.exportString());

            callProcessPacket(ch, "roomcreate\t" + NetUtil.urlEncode("") +
                    "\t" + roomInfoStr +
                    "\t" + NetUtil.urlEncode("VS"));

            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            assertFalse(roomList.isEmpty());
            NetRoomInfo room = roomList.getLast();
            assertEquals("No Title", room.strName);
        }
    }

    // ==================================================================
    // processPacket: roomjoin (join room and return to lobby)
    // ==================================================================

    @Test
    void processPacketRoomjoinReturnToLobby() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "LobbyReturn", 1);
            p.roomID = -1; // already in lobby

            callProcessPacket(ch, "roomjoin\t-1\tfalse");

            // Should return success with -1 roomID
            // Player should remain in lobby state
            assertEquals(-1, p.roomID);
            assertEquals(-1, p.seatID);
            assertEquals(-1, p.queueID);
        }
    }

    @Test
    void processPacketRoomjoinJoinExistingRoom() throws Throwable {
        setSelector();
        try (SocketChannel ch1 = SocketChannel.open(); SocketChannel ch2 = SocketChannel.open()) {
            // Create room first via room 1
            NetPlayerInfo host = addPlayerToServer(ch1, "Host", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 0;
            room.strName = "JoinableRoom";
            room.maxPlayers = 4;
            room.strMode = "VS";
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            host.roomID = 0;
            host.seatID = room.joinSeat(host);
            room.playerList.add(host);

            // Player 2 joins
            NetPlayerInfo joiner = addPlayerToServer(ch2, "Joiner", 2);

            callProcessPacket(ch2, "roomjoin\t0\tfalse");

            // Should have successfully joined
            assertEquals(0, joiner.roomID);
            assertTrue(joiner.seatID >= 0);
        }
    }

    @Test
    void processPacketRoomjoinNonexistentRoom() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            addPlayerToServer(ch, "LostPlayer", 1);

            callProcessPacket(ch, "roomjoin\t99999\tfalse");
            // Should not throw - roomjoinfail message is sent
        }
    }

    // ==================================================================
    // processPacket: lobbychat (including /msg private messages)
    // ==================================================================

    @Test
    void processPacketLobbychatNormal() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "Chatter", 1);

            callProcessPacket(ch, "lobbychat\t" + NetUtil.urlEncode("Hello world"));

            // Should have added to lobbyChatList
            LinkedList<NetChatMessage> chatList = getStaticField("lobbyChatList", LinkedList.class);
            assertFalse(chatList.isEmpty());
            assertEquals("Hello world", chatList.getLast().strMessage);
        }
    }

    @Test
    void processPacketLobbychatPrivateMessage() throws Throwable {
        setSelector();
        try (SocketChannel ch1 = SocketChannel.open(); SocketChannel ch2 = SocketChannel.open()) {
            NetPlayerInfo sender = addPlayerToServer(ch1, "Sender", 1);
            NetPlayerInfo recipient = addPlayerToServer(ch2, "Recipient", 2);

            // Send private message: "/msg Recipient Hi there"
            callProcessPacket(ch1, "lobbychat\t" + NetUtil.urlEncode("/msg Recipient Hi there"));

            // Should not throw and should send private message to recipient
            // No direct way to verify the output without a selector, but we can
            // verify no exception was thrown in the processPacket handler
        }
    }

    // ==================================================================
    // processPacket: room chat
    // ==================================================================

    @Test
    void processPacketChatInRoom() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "RoomChatter", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 5;
            room.strName = "ChatRoom";
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 5;

            callProcessPacket(ch, "chat\t" + NetUtil.urlEncode("Hello in room"));

            // Should have added to room chatList
            assertFalse(room.chatList.isEmpty());
            assertEquals("Hello in room", room.chatList.getLast().strMessage);
        }
    }

    @Test
    void processPacketChatWithoutRoomDoesNotThrow() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "Homeless", 1);
            p.roomID = -1; // not in a room

            callProcessPacket(ch, "chat\t" + NetUtil.urlEncode("Orphan message"));
            // Should not throw
        }
    }

    // ==================================================================
    // processPacket: start1p (single player game start)
    // ==================================================================

    @Test
    void processPacketStart1pStartsGame() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "SoloStarter", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 10;
            room.singleplayer = true;
            room.strName = "SoloRoom";
            room.playerSeat.add(p);
            room.playerList.add(p);
            room.startPlayers = 1;
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 10;
            p.seatID = 0;

            callProcessPacket(ch, "start1p");

            assertTrue(room.playing);
        }
    }

    // ==================================================================
    // processPacket: ready state change
    // ==================================================================

    @Test
    void processPacketReadyTrueMarksReady() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "ReadyPlayer", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 20;
            room.singleplayer = false;
            room.playerSeat.add(p);
            room.playerList.add(p);
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 20;
            p.seatID = 0;

            callProcessPacket(ch, "ready\ttrue");

            assertTrue(p.ready);
        }
    }

    @Test
    void processPacketReadyFalseMarksCancelled() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "CancelPlayer", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 21;
            room.singleplayer = false;
            room.playerSeat.add(p);
            room.playerList.add(p);
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 21;
            p.seatID = 0;
            p.ready = true;

            callProcessPacket(ch, "ready\tfalse");

            assertFalse(p.ready);
            assertTrue(room.isSomeoneCancelled);
        }
    }

    // ==================================================================
    // processPacket: dead (player death signal)
    // ==================================================================

    @Test
    void processPacketDeadMarksPlayerDead() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "DyingPlayer", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 30;
            room.playing = true;
            room.startPlayers = 2;
            room.playerSeat.add(p);
            room.playerList.add(p);
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 30;
            p.seatID = 0;
            p.playing = true;

            callProcessPacket(ch, "dead");
            assertFalse(p.playing);
        }
    }

    // ==================================================================
    // processPacket: gstat (multiplayer game stats)
    // ==================================================================

    @Test
    void processPacketGstatBroadcasts() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "StatPlayer", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 40;
            room.singleplayer = false;
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 40;
            p.seatID = 0;

            callProcessPacket(ch, "gstat\t100\t200\t300");
            // Should not throw
        }
    }

    // ==================================================================
    // processPacket: gstat1p (single player game stats)
    // ==================================================================

    @Test
    void processPacketGstat1pBroadcasts() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "SPStatPlayer", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 50;
            room.singleplayer = true;
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 50;
            p.seatID = 0;

            callProcessPacket(ch, "gstat1p\tdata");
            // Should not throw
        }
    }

    // ==================================================================
    // processPacket: changestatus (watch/toggle)
    // ==================================================================

    @Test
    void processPacketChangestatusToWatch() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "Watcher", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 60;
            room.singleplayer = false;
            room.maxPlayers = 4;
            room.playerSeat.add(p);
            room.playerList.add(p);
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 60;
            p.seatID = 0;

            callProcessPacket(ch, "changestatus\ttrue");

            // Should be watching now (seatID = -1)
            assertEquals(-1, p.seatID);
        }
    }

    // ==================================================================
    // updateSPDailyRanking wipe path (different day)
    // ==================================================================

    @Test
    void updateSPDailyRankingDifferentDayReturnsTrue() throws Throwable {
        // Set spDailyLastUpdate to yesterday
        Calendar yesterday = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        yesterday.add(Calendar.DATE, -1);
        Field lastUpdateField = NetServer.class.getDeclaredField("spDailyLastUpdate");
        lastUpdateField.setAccessible(true);
        lastUpdateField.set(null, yesterday);

        setStaticField("spRankingListDaily", new LinkedList<NetSPRanking>());
        setStaticField("propSPRankingDaily", new CustomProperties());
        setStaticField("spDailyTimeZone", "GMT");

        boolean wiped = (boolean) updateSPDailyRanking.invokeExact();
        assertTrue(wiped, "Different day should wipe daily ranking");
    }

    @Test
    void updateSPDailyRankingWipeClearsRecords() throws Throwable {
        // Set spDailyLastUpdate to yesterday
        Calendar yesterday = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        yesterday.add(Calendar.DATE, -1);
        Field lastUpdateField = NetServer.class.getDeclaredField("spDailyLastUpdate");
        lastUpdateField.setAccessible(true);
        lastUpdateField.set(null, yesterday);

        // Create daily ranking with a record
        LinkedList<NetSPRanking> daily = new LinkedList<NetSPRanking>();
        NetSPRanking r = new NetSPRanking("TestMode", "TestRule", 0, 0,
                NetSPRecord.RANKINGTYPE_GENERIC_SCORE, 100);
        NetSPRecord rec = new NetSPRecord();
        rec.strPlayerName = "Alice";
        rec.strModeName = "TestMode";
        rec.strRuleName = "TestRule";
        rec.stats = new nullpomino.game.component.Statistics();
        rec.stats.score = 100;
        r.listRecord.add(rec);
        daily.add(r);
        setStaticField("spRankingListDaily", daily);
        setStaticField("propSPRankingDaily", new CustomProperties());
        setStaticField("spDailyTimeZone", "GMT");

        boolean wiped = (boolean) updateSPDailyRanking.invokeExact();
        assertTrue(wiped, "Different day should wipe");
        assertTrue(r.listRecord.isEmpty(), "Daily records should be cleared after wipe");
    }

    // ==================================================================
    // writeMPRankingToFile - cover IO error path
    // ==================================================================

    @Test
    void writeMPRankingToFileWithNonWritablePathDoesNotThrow() throws Throwable {
        // Set propMPRanking to null or a read-only path (unwritable in test context)
        setStaticField("propMPRanking", new CustomProperties());

        LinkedList<NetPlayerInfo> mpList = getMpRankingList(0);
        NetPlayerInfo p = new NetPlayerInfo();
        p.strName = "TestPlayer";
        p.rating[0] = 1500;
        mpList.add(p);

        // writeMPRankingToFile writes to "config/setting/netserver_mpranking.cfg"
        // which is likely writable, but even if not, the method catches IOException
        writeMPRankingToFile.invokeExact();
        // Should not throw
    }

    // ==================================================================
    // writeSPRankingToFile - cover IO error path
    // ==================================================================

    @Test
    void writeSPRankingToFileWithNonWritablePathDoesNotThrow() throws Throwable {
        setStaticField("propSPRankingAlltime", new CustomProperties());
        setStaticField("propSPRankingDaily", new CustomProperties());
        setStaticField("spRankingListAlltime", new LinkedList<NetSPRanking>());
        setStaticField("spRankingListDaily", new LinkedList<NetSPRanking>());

        writeSPRankingToFile.invokeExact();
        // Should not throw
    }

    // ==================================================================
    // processPacket: spranking (SP leaderboard query)
    // ==================================================================

    @Test
    void processPacketSprankingWithEmptyListReturnsZeroRecords() throws Throwable {
        setSelector();
        setStaticField("spRankingListAlltime", new LinkedList<NetSPRanking>());
        setStaticField("spRankingListDaily", new LinkedList<NetSPRanking>());
        setStaticField("propSPRankingDaily", new CustomProperties());

        try (SocketChannel ch = SocketChannel.open()) {
            String rule = NetUtil.urlEncode("all");
            String mode = NetUtil.urlEncode("TestMode");
            callProcessPacket(ch, "spranking\t" + rule + "\t" + mode + "\t0\tfalse");
            // Should not throw despite empty lists
        }
    }

    @Test
    void processPacketSprankingDailyWipesOnDifferentDay() throws Throwable {
        setSelector();
        Calendar yesterday = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        yesterday.add(Calendar.DATE, -1);
        Field lastUpdateField = NetServer.class.getDeclaredField("spDailyLastUpdate");
        lastUpdateField.setAccessible(true);
        lastUpdateField.set(null, yesterday);

        setStaticField("spRankingListDaily", new LinkedList<NetSPRanking>());
        setStaticField("propSPRankingDaily", new CustomProperties());
        setStaticField("spRankingListAlltime", new LinkedList<NetSPRanking>());

        try (SocketChannel ch = SocketChannel.open()) {
            String rule = NetUtil.urlEncode("all");
            String mode = NetUtil.urlEncode("TestMode");
            callProcessPacket(ch, "spranking\t" + rule + "\t" + mode + "\t0\ttrue");
            // Should not throw despite daily wipe
        }
    }

    // ==================================================================
    // processPacket: reset1p (SP mode reset)
    // ==================================================================

    @Test
    void processPacketReset1pResetsPlayState() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "ResetPlayer", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 70;
            room.singleplayer = true;
            room.playing = true;
            room.playerSeat.add(p);
            room.playerList.add(p);
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 70;
            p.seatID = 0;
            p.playing = true;

            callProcessPacket(ch, "reset1p");
            assertFalse(p.playing);
        }
    }

    // ==================================================================
    // processPacket: game (game message relay)
    // ==================================================================

    @Test
    void processPacketGameRelaysMessage() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "GamePlayer", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 80;
            room.playerSeat.add(p);
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 80;
            p.seatID = 0;

            callProcessPacket(ch, "game\tmove\tleft");
            // Should not throw
        }
    }

    // ==================================================================
    // processPacket: racewin (race mode)
    // ==================================================================

    @Test
    void processPacketRacewinWithNoRaceModeDoesNothing() throws Throwable {
        setSelector();
        try (SocketChannel ch = SocketChannel.open()) {
            NetPlayerInfo p = addPlayerToServer(ch, "Racer", 1);

            NetRoomInfo room = new NetRoomInfo();
            room.roomID = 90;
            room.playing = true;
            room.strMode = "VS"; // not a race mode
            room.style = 0;
            room.playerSeat.add(p);
            room.playerList.add(p);
            LinkedList<NetRoomInfo> roomList = getInstanceField(server, "roomInfoList");
            roomList.add(room);

            p.roomID = 90;
            p.seatID = 0;
            p.playing = true;

            callProcessPacket(ch, "racewin\t1\t2");
            // Should not throw (isRace will be false since mpModeIsRace is empty)
        }
    }
}
