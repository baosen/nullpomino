package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.LinkedList;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.play.GameEngine;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Extended tests for {@link NetServer} covering additional testable
 * helper methods: getRatedRule, deleteRoom, gameStartIfPossible,
 * autoStartTimerCheck, getSPRanking/setSPRanking, and rating math
 * edge cases.
 */
class NetServerExtendedTest {

    private NetServer server;

    // Method handles
    private MethodHandle getRatedRule;
    private MethodHandle deleteRoom;
    private MethodHandle gameStartIfPossible;
    private MethodHandle autoStartTimerCheck;
    private MethodHandle getSPRankingAllRules;
    private MethodHandle updateSPDailyRanking;
    private MethodHandle writeServerStatusFile;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() throws Exception {
        setStaticField("propServer", new CustomProperties());

        server = new NetServer(9999);

        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());

        getRatedRule = lookup.findVirtual(NetServer.class, "getRatedRule",
                MethodType.methodType(RuleOptions.class, int.class, String.class));
        deleteRoom = lookup.findVirtual(NetServer.class, "deleteRoom",
                MethodType.methodType(boolean.class, NetRoomInfo.class));
        gameStartIfPossible = lookup.findVirtual(NetServer.class, "gameStartIfPossible",
                MethodType.methodType(boolean.class, NetRoomInfo.class));
        autoStartTimerCheck = lookup.findVirtual(NetServer.class, "autoStartTimerCheck",
                MethodType.methodType(void.class, NetRoomInfo.class));
        getSPRankingAllRules = lookup.findStatic(NetServer.class, "getSPRankingAllRules",
                MethodType.methodType(NetSPRanking.class, String.class, int.class, boolean.class));
        updateSPDailyRanking = lookup.findStatic(NetServer.class, "updateSPDailyRanking",
                MethodType.methodType(boolean.class));
        writeServerStatusFile = lookup.findVirtual(NetServer.class, "writeServerStatusFile",
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

        // Init ranking lists
        Field f = NetServer.class.getDeclaredField("mpRankingList");
        f.setAccessible(true);
        LinkedList<NetPlayerInfo>[] list = new LinkedList[GameEngine.MAX_GAMESTYLE];
        for (int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
            list[i] = new LinkedList<NetPlayerInfo>();
        }
        f.set(null, list);
    }

    // ------------------------------------------------------------------
    // Helpers: reflect into private static fields
    // ------------------------------------------------------------------

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
    private static LinkedList<NetPlayerInfo> getMpRankingList(int style) throws Exception {
        Field f = NetServer.class.getDeclaredField("mpRankingList");
        f.setAccessible(true);
        LinkedList<NetPlayerInfo>[] arr = (LinkedList<NetPlayerInfo>[]) f.get(null);
        return arr[style];
    }

    // ------------------------------------------------------------------
    // getRatedRule
    // ------------------------------------------------------------------

    @Test
    void getRatedRuleReturnsNullForEmptyList() throws Throwable {
        RuleOptions rule = (RuleOptions) getRatedRule.invokeExact(server, 0, "nonexistent");
        assertNull(rule, "Non-existent rule should return null");
    }

    @Test
    void getRatedRuleReturnsNullForNullName() throws Throwable {
        RuleOptions rule = (RuleOptions) getRatedRule.invokeExact(server, 0, (String) null);
        assertNull(rule, "Null name should return null");
    }

    // ------------------------------------------------------------------
    // deleteRoom
    // ------------------------------------------------------------------

    @Test
    void deleteRoomReturnsFalseForNull() throws Throwable {
        boolean result = (boolean) deleteRoom.invokeExact(server, (NetRoomInfo) null);
        assertFalse(result, "Null room should return false");
    }

    @Test
    void deleteRoomReturnsFalseForNonEmptyRoom() throws Throwable {
        // Create a room with a player
        NetRoomInfo room = new NetRoomInfo();
        room.roomID = 1;
        room.playerList.add(new NetPlayerInfo());

        boolean result = (boolean) deleteRoom.invokeExact(server, room);
        assertFalse(result, "Non-empty room should not be deleted");
    }

    @Test
    void deleteRoomReturnsTrueForEmptyRoom() throws Throwable {
        NetRoomInfo room = new NetRoomInfo();
        room.roomID = 2;
        room.strName = "Empty Room";

        // Ensure room is in the list via field access
        Field roomListField = NetServer.class.getDeclaredField("roomInfoList");
        roomListField.setAccessible(true);
        @SuppressWarnings("unchecked")
        LinkedList<NetRoomInfo> roomList = (LinkedList<NetRoomInfo>) roomListField.get(server);
        roomList.add(room);

        boolean result = (boolean) deleteRoom.invokeExact(server, room);
        assertTrue(result, "Empty room should be deleted");
    }

    // ------------------------------------------------------------------
    // gameStartIfPossible
    // ------------------------------------------------------------------

    @Test
    void gameStartIfPossibleReturnsFalseForNullRoom() throws Throwable {
        boolean result = (boolean) gameStartIfPossible.invokeExact(server, (NetRoomInfo) null);
        assertFalse(result, "Null room should not start game");
    }

    @Test
    void gameStartIfPossibleReturnsFalseForSinglePlayer() throws Throwable {
        NetRoomInfo room = new NetRoomInfo();
        room.singleplayer = true;
        // gameStartIfPossible requires >=2 players ready, which doesn't apply to singleplayer

        boolean result = (boolean) gameStartIfPossible.invokeExact(server, room);
        assertFalse(result, "Single-player room should not start via gameStartIfPossible");
    }

    @Test
    void gameStartIfPossibleReturnsFalseWhenNotAllReady() throws Throwable {
        NetRoomInfo room = new NetRoomInfo();
        room.singleplayer = false;
        NetPlayerInfo p1 = new NetPlayerInfo();
        p1.ready = true;
        NetPlayerInfo p2 = new NetPlayerInfo();
        p2.ready = false;
        room.playerSeat = new LinkedList<NetPlayerInfo>();
        room.playerSeat.add(p1);
        room.playerSeat.add(p2);

        // getNumberOfPlayerSeated returns count of non-null entries
        // getHowManyPlayersReady returns count of ready non-null entries
        boolean result = (boolean) gameStartIfPossible.invokeExact(server, room);
        assertFalse(result, "Not all ready should not start");
    }

    // ------------------------------------------------------------------
    // autoStartTimerCheck
    // ------------------------------------------------------------------

    @Test
    void autoStartTimerCheckDoesNothingForDisabled() throws Throwable {
        NetRoomInfo room = new NetRoomInfo();
        room.autoStartSeconds = 0;

        autoStartTimerCheck.invokeExact(server, room);
        // No exception expected; autoStartActive remains false
        assertFalse(room.autoStartActive);
    }

    @Test
    void autoStartTimerCheckStopsWhenNotEnoughReady() throws Throwable {
        NetRoomInfo room = new NetRoomInfo();
        room.autoStartSeconds = 10;
        room.autoStartActive = true;
        NetPlayerInfo p1 = new NetPlayerInfo();
        p1.ready = false;
        room.playerSeat = new LinkedList<NetPlayerInfo>();
        room.playerSeat.add(p1);

        autoStartTimerCheck.invokeExact(server, room);
        // Should stop because not enough ready players
        assertFalse(room.autoStartActive);
    }

    @Test
    void autoStartTimerCheckTurnsOffReadyForSinglePlayer() throws Throwable {
        NetRoomInfo room = new NetRoomInfo();
        room.autoStartSeconds = 10;
        NetPlayerInfo p1 = new NetPlayerInfo();
        p1.ready = true;
        room.playerSeat = new LinkedList<NetPlayerInfo>();
        room.playerSeat.add(p1);
        room.playerSeat.add(null);

        autoStartTimerCheck.invokeExact(server, room);
        // getNumberOfPlayerSeated returns 1 -> autoStartActive stays false, ready turned off
        assertFalse(p1.ready, "Single player ready should be turned off");
    }

    // ------------------------------------------------------------------
    // getSPRankingAllRules
    // ------------------------------------------------------------------

    @Test
    void getSPRankingAllRulesReturnsNullForEmptyLists() throws Throwable {
        // Init spRankingListAlltime
        Field f = NetServer.class.getDeclaredField("spRankingListAlltime");
        f.setAccessible(true);
        f.set(null, new LinkedList<NetSPRanking>());

        NetSPRanking result = (NetSPRanking) getSPRankingAllRules.invokeExact("testmode", 0, false);
        assertNull(result, "Empty list should return null");
    }

    // ------------------------------------------------------------------
    // updateSPDailyRanking
    // ------------------------------------------------------------------

	@Test
	void updateSPDailyRankingFirstTimeReturnsFalse() throws Throwable {
		// spDailyLastUpdate is null initially -> returns false (no wipe)
		// because the code returns false when oldLastUpdate == null
		Field f = NetServer.class.getDeclaredField("spDailyLastUpdate");
		f.setAccessible(true);
		f.set(null, null);

		// Also init spRankingListDaily
		Field dailyField = NetServer.class.getDeclaredField("spRankingListDaily");
		dailyField.setAccessible(true);
		dailyField.set(null, new LinkedList<NetSPRanking>());

		boolean result = (boolean) updateSPDailyRanking.invokeExact();
		assertFalse(result, "First update should return false (no wipe needed)");
	}

    // ------------------------------------------------------------------
    // rankDelta corner cases
    // ------------------------------------------------------------------

    @Test
    void rankDeltaProvisionalPlayerLosesPoints() throws Throwable {
        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
        MethodHandle rankDelta = lookup.findVirtual(NetServer.class, "rankDelta",
                MethodType.methodType(double.class, int.class, double.class, double.class, double.class));

        // Provisional player (0 games) loses to equal-rated opponent
        double delta = (double) rankDelta.invokeExact(server, 0, 1500.0, 1500.0, 0.0);
        // maxDelta(0) = NORMAL_MAX_DIFF + 400/3 = 16 + 133 = 149
        // expectedScore(1500,1500) = 0.5
        // delta = 149 * (0 - 0.5) = -74.5
        assertTrue(delta < 0, "Losing provisional player should lose points");
    }

    @Test
    void rankDeltaProvisionalAgainstHigherRated() throws Throwable {
        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
        MethodHandle rankDelta = lookup.findVirtual(NetServer.class, "rankDelta",
                MethodType.methodType(double.class, int.class, double.class, double.class, double.class));

        // Provisional player (5 games) beats a much higher-rated opponent
        double delta = (double) rankDelta.invokeExact(server, 5, 1000.0, 2000.0, 1.0);
        assertTrue(delta > 0, "Beating higher-rated opponent should gain points");
    }

    // ------------------------------------------------------------------
    // writeServerStatusFile (disabled by config)
    // ------------------------------------------------------------------

    @Test
    void writeServerStatusFileDisabledDoesNotThrow() throws Throwable {
        // Default propServer has writestatusfile = false, so this is a no-op
        writeServerStatusFile.invokeExact(server);
        // No exception expected
    }

    // ------------------------------------------------------------------
    // mpRankingUpdate edge cases
    // ------------------------------------------------------------------

    @Test
    void mpRankingUpdateInsertsNewPlayer() throws Throwable {
        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
        MethodHandle mpRankingUpdate = lookup.findStatic(NetServer.class, "mpRankingUpdate",
                MethodType.methodType(int.class, int.class, NetPlayerInfo.class));

        NetPlayerInfo p = new NetPlayerInfo();
        p.strName = "NewPlayer";
        p.rating[0] = 1500;

        int place = (int) mpRankingUpdate.invokeExact(0, p);

        assertEquals(0, place, "New player should be placed at 0");
        assertEquals(1, getMpRankingList(0).size(), "Ranking list should have one entry");
    }

    @Test
    void mpRankingUpdateReplacesExistingPlayer() throws Throwable {
        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
        MethodHandle mpRankingUpdate = lookup.findStatic(NetServer.class, "mpRankingUpdate",
                MethodType.methodType(int.class, int.class, NetPlayerInfo.class));

		// Add player once
		NetPlayerInfo p = new NetPlayerInfo();
		p.strName = "Player";
		p.rating[0] = 1500;
		int unused = (int) mpRankingUpdate.invokeExact(0, p);

		// Update with higher rating
		NetPlayerInfo p2 = new NetPlayerInfo();
		p2.strName = "Player";
		p2.rating[0] = 1600;
		int place = (int) mpRankingUpdate.invokeExact(0, p2);

		assertEquals(0, place, "Updated player should be at top");
		assertEquals(1, getMpRankingList(0).size(), "Should still have one entry");
    }

    @Test
    void mpRankingUpdateRespectsMaxSize() throws Throwable {
        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
        MethodHandle mpRankingUpdate = lookup.findStatic(NetServer.class, "mpRankingUpdate",
                MethodType.methodType(int.class, int.class, NetPlayerInfo.class));

        // Set maxMPRanking to 2
        setStaticInt("maxMPRanking", 2);

		// Add 3 players; only 2 should remain
		NetPlayerInfo p1 = new NetPlayerInfo();
		p1.strName = "P1";
		p1.rating[0] = 1500;
		int unused1 = (int) mpRankingUpdate.invokeExact(0, p1);

		NetPlayerInfo p2 = new NetPlayerInfo();
		p2.strName = "P2";
		p2.rating[0] = 1400;
		int unused2 = (int) mpRankingUpdate.invokeExact(0, p2);

		NetPlayerInfo p3 = new NetPlayerInfo();
		p3.strName = "P3";
		p3.rating[0] = 1300;
		int unused3 = (int) mpRankingUpdate.invokeExact(0, p3);

        assertEquals(2, getMpRankingList(0).size(), "Max size should be enforced");
    }

    // ------------------------------------------------------------------
    // getHostAddress / getHostName / getHostFull (without SocketChannel)
    // ------------------------------------------------------------------

    @Test
    void getHostAddressWithNullReturnsEmpty() throws Throwable {
        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
        MethodHandle getHostAddress = lookup.findStatic(NetServer.class, "getHostAddress",
                MethodType.methodType(String.class, java.nio.channels.SocketChannel.class));

        String result = (String) getHostAddress.invokeExact((java.nio.channels.SocketChannel) null);
        assertEquals("", result, "Null SocketChannel should return empty string");
    }

    @Test
    void getHostNameWithNullReturnsEmpty() throws Throwable {
        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
        MethodHandle getHostName = lookup.findStatic(NetServer.class, "getHostName",
                MethodType.methodType(String.class, java.nio.channels.SocketChannel.class));

        String result = (String) getHostName.invokeExact((java.nio.channels.SocketChannel) null);
        assertEquals("", result, "Null SocketChannel should return empty string");
    }

    @Test
    void getHostFullWithNullReturnsEmpty() throws Throwable {
        MethodHandles.Lookup lookup =
                MethodHandles.privateLookupIn(NetServer.class, MethodHandles.lookup());
        MethodHandle getHostFull = lookup.findStatic(NetServer.class, "getHostFull",
                MethodType.methodType(String.class, java.nio.channels.SocketChannel.class));

        String result = (String) getHostFull.invokeExact((java.nio.channels.SocketChannel) null);
        assertEquals("", result, "Null SocketChannel should return empty string");
    }
}
