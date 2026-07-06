// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetChatMessage;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link RoomMirror}: stale sequence numbers, frames
 * for unknown rooms/players, snapshot frames of every kind (including
 * too-short ones), chat history trimming, and promotion of cached blobs.
 */
class RoomMirrorBranchGapTest {

    private RoomMirror mirror;

    @BeforeEach
    void setUp() {
        mirror = new RoomMirror();
    }

    // ---------------------------------------------------------------- helpers

    private static String playerBlob(int uid, String name, int roomID) {
        NetPlayerInfo p = new NetPlayerInfo();
        p.uid = uid;
        p.strName = name;
        p.roomID = roomID;
        return p.exportString();
    }

    private static String roomBlob(int roomID, String name) {
        NetRoomInfo r = new NetRoomInfo();
        r.roomID = roomID;
        r.strName = name;
        return r.exportString();
    }

    private static String chatLine(String kind, int uid, String name, String msg) {
        return kind + "\t" + uid + "\t" + NetUtil.urlEncode(name) + "\t"
                + GeneralUtil.exportCalendarString() + "\t" + NetUtil.urlEncode(msg);
    }

    private static String compressedRuleBlob() {
        CustomProperties prop = new CustomProperties();
        new RuleOptions().writeProperty(prop, 0);
        return NetUtil.compressString(prop.encode("rule"));
    }

    private void createRoom(int roomID) {
        mirror.applyBroadcast(1, -1, "roomcreate\t" + roomBlob(roomID, "Room" + roomID));
    }

    private static RoomProtocol.AuthRoom authRoom(long seq, int roomId, int[] queueUids, int[] deadUids) {
        return new RoomProtocol.AuthRoom(seq, roomId, false, 0, 0, false, false, -1,
                new int[0], new int[0], deadUids, queueUids);
    }

    // ---------------------------------------------------------------- sequence numbers

    @Test
    void staleBroadcastSeqDoesNotLowerHighWater() {
        mirror.applyBroadcast(5, -1, "unknownverb");
        assertEquals(5, mirror.getSeq());
        mirror.applyBroadcast(3, -1, "unknownverb");
        assertEquals(5, mirror.getSeq());
    }

    @Test
    void staleAuthGlobalSeqDoesNotLowerHighWater() {
        mirror.applyAuthGlobal(RoomProtocol.parseAuthGlobal(
                RoomProtocol.buildAuthGlobal(10, 4).split("\t", -1)));
        assertEquals(10, mirror.getSeq());
        assertEquals(4, mirror.getNextUid());

        mirror.applyAuthGlobal(RoomProtocol.parseAuthGlobal(
                RoomProtocol.buildAuthGlobal(2, 7).split("\t", -1)));
        assertEquals(10, mirror.getSeq());
        assertEquals(7, mirror.getNextUid(), "counter still overwritten wholesale");
    }

    @Test
    void staleAuthRoomSeqDoesNotLowerHighWater() {
        createRoom(0);
        mirror.applyAuthRoom(authRoom(50, 0, new int[0], new int[0]));
        assertEquals(50, mirror.getSeq());
        mirror.applyAuthRoom(authRoom(1, 0, new int[0], new int[0]));
        assertEquals(50, mirror.getSeq());
    }

    // ---------------------------------------------------------------- auth-room targeting

    @Test
    void authRoomForMissingOrMismatchedRoomIsIgnored() {
        // No room at all
        mirror.applyAuthRoom(authRoom(1, 0, new int[0], new int[0]));
        assertNull(mirror.getRoom());

        // Room exists but under a different id
        createRoom(0);
        RoomProtocol.AuthRoom wrongRoom = new RoomProtocol.AuthRoom(2, 9, true, 3, 1,
                false, false, -1, new int[0], new int[0], new int[0], new int[0]);
        mirror.applyAuthRoom(wrongRoom);
        assertFalse(mirror.getRoom().playing, "mismatched authr must not touch the room");
    }

    @Test
    void authRoomSkipsUnknownUidsInQueueAndDeadLists() {
        createRoom(0);
        mirror.applyBroadcast(2, -1, "playernew\t" + playerBlob(1, "Bob", 0));

        mirror.applyAuthRoom(authRoom(3, 0, new int[] { 1, 99 }, new int[] { 1, 99 }));
        assertEquals(1, mirror.getRoom().playerQueue.size(), "unknown uid 99 dropped");
        assertEquals(1, mirror.getRoom().playerSeatDead.size(), "unknown uid 99 dropped");
    }

    // ---------------------------------------------------------------- chat broadcasts

    @Test
    void chatForUnknownScopeIsDropped() {
        mirror.applyBroadcast(1, 0, chatLine("chat", 0, "Alice", "hello?"));
        assertNull(mirror.getRoom());
    }

    @Test
    void roomChatHistoryIsTrimmed() {
        createRoom(0);
        for (int i = 0; i < RoomAuthority.MAX_ROOMCHAT_HISTORY + 2; i++) {
            mirror.applyBroadcast(2 + i, 0, chatLine("chat", 0, "Alice", "msg" + i));
        }
        assertEquals(RoomAuthority.MAX_ROOMCHAT_HISTORY, mirror.getRoom().chatList.size());
        assertEquals("msg2", mirror.getRoom().chatList.getFirst().strMessage);
    }

    @Test
    void lobbyChatHistoryIsTrimmed() {
        for (int i = 0; i < RoomAuthority.MAX_LOBBYCHAT_HISTORY + 2; i++) {
            mirror.applyBroadcast(1 + i, -1, chatLine("lobbychat", 0, "Alice", "msg" + i));
        }
        assertEquals(RoomAuthority.MAX_LOBBYCHAT_HISTORY, mirror.getLobbyChatList().size());
        assertEquals("msg2", mirror.getLobbyChatList().getFirst().strMessage);
    }

    // ---------------------------------------------------------------- snapshot frames

    @Test
    void tooShortSnapshotIsIgnored() {
        mirror.applySnapshot(new String[] { "room", "snap" });
        assertNull(mirror.getRoom());
        assertTrue(mirror.getPlayers().isEmpty());
    }

    @Test
    void snapshotKindsWithMissingArgumentsAreIgnored() {
        createRoom(0);
        mirror.applySnapshot(new String[] { "room", "snap", "player" });
        mirror.applySnapshot(new String[] { "room", "snap", "room", "0" });
        mirror.applySnapshot(new String[] { "room", "snap", "rule", "0", "cs" });
        mirror.applySnapshot(new String[] { "room", "snap", "roomrule", "0" });
        mirror.applySnapshot(new String[] { "room", "snap", "map", "0" });
        mirror.applySnapshot(new String[] { "room", "snap", "chat", "0" });
        mirror.applySnapshot(new String[] { "room", "snap", "lobbychat" });
        mirror.applySnapshot(new String[] { "room", "snap", "bogus", "x", "y" });

        assertTrue(mirror.getPlayers().isEmpty());
        assertTrue(mirror.getRuleBlobs().isEmpty());
        assertNull(mirror.getRoomRuleBlob());
        assertNull(mirror.getMapBlob());
        assertTrue(mirror.getRoom().chatList.isEmpty());
        assertTrue(mirror.getLobbyChatList().isEmpty());
    }

    @Test
    void roomRuleAndMapSnapshotsAreCached() {
        String rule = compressedRuleBlob();
        mirror.applySnapshot(RoomProtocol.buildSnapRoomRule(0, rule).split("\t", -1));
        assertEquals(rule, mirror.getRoomRuleBlob());

        String map = NetUtil.compressString("map1\tmap2");
        mirror.applySnapshot(RoomProtocol.buildSnapMap(0, map).split("\t", -1));
        assertEquals(map, mirror.getMapBlob());
    }

    @Test
    void chatSnapshotsReplayHistory() {
        // Room chat snapshot before any room exists: dropped
        NetChatMessage chat = new NetChatMessage("first");
        mirror.applySnapshot(RoomProtocol.buildSnapChat(0, chat.exportString()).split("\t", -1));

        createRoom(0);
        mirror.applySnapshot(RoomProtocol.buildSnapChat(0, chat.exportString()).split("\t", -1));
        assertEquals(1, mirror.getRoom().chatList.size());
        assertEquals("first", mirror.getRoom().chatList.getFirst().strMessage);

        NetChatMessage lobby = new NetChatMessage("lounge");
        mirror.applySnapshot(RoomProtocol.buildSnapLobbyChat(lobby.exportString()).split("\t", -1));
        assertEquals(1, mirror.getLobbyChatList().size());
        assertEquals("lounge", mirror.getLobbyChatList().getFirst().strMessage);
    }

    // ---------------------------------------------------------------- promotion

    @Test
    void promoteWithoutRoomIsANoOp() {
        mirror.cacheRule(99, "cs", compressedRuleBlob());
        mirror.promote();
        assertNull(mirror.getRoom());
    }

    @Test
    void promoteRestoresRoomRuleAndMapsAndSkipsUnknownRuleOwners() {
        createRoom(0);
        mirror.applyBroadcast(2, -1, "playernew\t" + playerBlob(0, "Alice", 0));

        mirror.cacheRoomRule(compressedRuleBlob());
        mirror.cacheMap(NetUtil.compressString("mapA\tmapB"));
        mirror.cacheRule(0, "cs", compressedRuleBlob());
        mirror.cacheRule(42, "cs", compressedRuleBlob()); // no such player

        mirror.promote();

        NetRoomInfo room = mirror.getRoom();
        assertNotNull(room.ruleOpt, "room rule blob decompressed on promote");
        assertEquals(2, room.mapList.size());
        assertEquals("mapA", room.mapList.get(0));
        assertNotNull(mirror.getPlayer(0).ruleOpt, "player rule restored");
    }

    // ---------------------------------------------------------------- queries and upserts

    @Test
    void getRoomChecksIdAndPresence() {
        assertNull(mirror.getRoom(-1));
        assertNull(mirror.getRoom(0), "no room yet");
        createRoom(0);
        assertNull(mirror.getRoom(5), "wrong id");
        assertNotNull(mirror.getRoom(0));
        assertNull(mirror.getRoom(-1), "negative id even when a room exists");
    }

    @Test
    void roomCreateWithNewIdReplacesTheOldRoom() {
        createRoom(0);
        NetRoomInfo first = mirror.getRoom();
        mirror.applyBroadcast(2, -1, "roomcreate\t" + roomBlob(1, "Second"));
        assertNotSame(first, mirror.getRoom());
        assertEquals(1, mirror.getRoom().roomID);
        assertEquals("Second", mirror.getRoom().strName);
    }
}
