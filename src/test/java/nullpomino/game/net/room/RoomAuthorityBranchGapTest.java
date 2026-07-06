// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.zip.Adler32;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link RoomAuthority}: membership edge cases, the
 * control dispatcher tail, rule blobs, chat history caps, room create
 * variants (single rooms, clamps, rule-lock, mode fallback, map-less map
 * rooms) and join/team/name/status guards.
 */
class RoomAuthorityBranchGapTest {

    private static final class RecordingSink implements RoomAuthority.Sink {
        final List<String> broadcasts = new ArrayList<String>();   // scope + "|" + line
        final List<String> directs = new ArrayList<String>();      // uid + "|" + line
        final List<String> ruleCaches = new ArrayList<String>();
        final List<String> roomRuleCaches = new ArrayList<String>();
        final List<String> mapCaches = new ArrayList<String>();
        int authUpdates = 0;

        public void broadcast(int scope, String line, int exceptUid) { broadcasts.add(scope + "|" + line); }
        public void direct(int uid, String line) { directs.add(uid + "|" + line); }
        public void ruleCache(int uid, String checksum, String data) { ruleCaches.add(uid + "|" + checksum); }
        public void roomRuleCache(int roomId, String data) { roomRuleCaches.add(roomId + "|" + data); }
        public void mapCache(int roomId, String data) { mapCaches.add(roomId + "|" + data); }
        public void authUpdate() { authUpdates++; }

        String lastBroadcastStarting(String prefix) {
            for (int i = broadcasts.size() - 1; i >= 0; i--) {
                String line = broadcasts.get(i).split("\\|", 2)[1];
                if (line.startsWith(prefix)) return line;
            }
            return null;
        }
        String lastDirectFor(int uid, String prefix) {
            for (int i = directs.size() - 1; i >= 0; i--) {
                String[] f = directs.get(i).split("\\|", 2);
                if (Integer.parseInt(f[0]) == uid && f[1].startsWith(prefix)) return f[1];
            }
            return null;
        }
        void clear() { broadcasts.clear(); directs.clear(); }
    }

    private RecordingSink sink;
    private RoomAuthority auth;

    @BeforeEach
    void setUp() {
        sink = new RecordingSink();
        auth = new RoomAuthority(sink, new Random(42));
    }

    private void admit(int expectedUid, String name) {
        int uid = auth.reserveUid();
        assertEquals(expectedUid, uid);
        auth.admitMember(uid, name, "127.0.0.1");
        auth.getPlayer(uid).ruleOpt = new RuleOptions();
    }

    private void control(int uid, String line) {
        auth.handleControl(uid, line.split("\t", -1));
    }

    private void createRoom(int creatorUid, String name, NetRoomInfo template) {
        control(creatorUid, "roomcreate\t" + NetUtil.urlEncode(name) + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE"));
    }

    private NetRoomInfo vsTemplate(int maxPlayers) {
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = maxPlayers;
        template.strMode = "NET-VS-BATTLE";
        return template;
    }

    // ---------------------------------------------------------------- membership

    @Test
    void memberGoneForUnknownUidIsIgnored() {
        admit(0, "Alice");
        sink.clear();
        auth.onMemberGone(99, true);      // never admitted
        assertTrue(sink.broadcasts.isEmpty());
        assertNotNull(auth.getPlayer(0));
    }

    @Test
    void soleMemberGoneDeletesTheRoom() {
        admit(0, "Alice");
        createRoom(0, "Doomed", vsTemplate(2));
        sink.clear();

        auth.onMemberGone(0, true);

        assertNotNull(sink.lastBroadcastStarting("roomdelete\t"));
        assertNotNull(sink.lastBroadcastStarting("playerlogout\t"));
        assertNull(auth.getRoom());
    }

    // ---------------------------------------------------------------- dispatcher tail

    @Test
    void reset1pAndUnknownCommandsFallThroughSilently() {
        admit(0, "Alice");
        sink.clear();
        control(0, "reset1p");            // lobby: roomForId(-1) == null
        control(0, "nosuchcommand\targ"); // falls off the dispatch chain
        assertTrue(sink.broadcasts.isEmpty());
        assertTrue(sink.directs.isEmpty());
    }

    // ---------------------------------------------------------------- rules

    @Test
    void ruleDataWithValidChecksumIsStoredAndCached() {
        admit(0, "Alice");
        RuleOptions rule = new RuleOptions();
        CustomProperties prop = new CustomProperties();
        rule.writeProperty(prop, 0);
        String data = NetUtil.compressString(prop.encode("RuleData test"));
        Adler32 checksumObj = new Adler32();
        checksumObj.update(NetUtil.stringToBytes(data));

        sink.clear();
        control(0, "ruledata\t" + checksumObj.getValue() + "\t" + data);

        assertEquals("ruledatasuccess", sink.lastDirectFor(0, "ruledatasuccess"));
        assertTrue(auth.getRuleBlobs().containsKey(0));
        assertEquals(1, sink.ruleCaches.size());
    }

    @Test
    void ruleDataWithBadChecksumFails() {
        admit(0, "Alice");
        String data = NetUtil.compressString("garbage payload");

        sink.clear();
        control(0, "ruledata\t0\t" + data);   // Adler32 of a non-empty string is never 0

        assertNotNull(sink.lastDirectFor(0, "ruledatafail\t"));
        assertFalse(auth.getRuleBlobs().containsKey(0));
        assertTrue(sink.ruleCaches.isEmpty());
    }

    @Test
    void ruleGetCoversNullRuleExistingRuleAndUnknownUid() {
        // Alice admitted raw: her ruleOpt stays null until someone asks
        int uid = auth.reserveUid();
        auth.admitMember(uid, "Alice", "127.0.0.1");
        admit(1, "Bob");   // Bob has a ruleOpt

        sink.clear();
        control(1, "ruleget\t0");
        assertNotNull(sink.lastDirectFor(1, "rulegetsuccess\t0\t"));
        assertNotNull(auth.getPlayer(0).ruleOpt, "ruleget must materialize a default rule");

        control(1, "ruleget\t1");
        assertNotNull(sink.lastDirectFor(1, "rulegetsuccess\t1\t"));

        control(1, "ruleget\t99");
        assertEquals("rulegetfail\t99", sink.lastDirectFor(1, "rulegetfail"));
    }

    // ---------------------------------------------------------------- chat

    @Test
    void lobbyChatHistoryIsCapped() {
        admit(0, "Alice");
        for (int i = 0; i < RoomAuthority.MAX_LOBBYCHAT_HISTORY + 2; i++) {
            control(0, "lobbychat\t" + NetUtil.urlEncode("msg" + i));
        }
        assertEquals(RoomAuthority.MAX_LOBBYCHAT_HISTORY, auth.getLobbyChatList().size());
        assertEquals("msg2", auth.getLobbyChatList().getFirst().strMessage);
    }

    @Test
    void roomChatFromLobbyIsIgnored() {
        admit(0, "Alice");
        sink.clear();
        control(0, "chat\t" + NetUtil.urlEncode("hello"));
        assertTrue(sink.broadcasts.isEmpty());
    }

    @Test
    void roomChatHistoryIsCapped() {
        admit(0, "Alice");
        createRoom(0, "Chatty", vsTemplate(2));
        for (int i = 0; i < RoomAuthority.MAX_ROOMCHAT_HISTORY + 2; i++) {
            control(0, "chat\t" + NetUtil.urlEncode("msg" + i));
        }
        assertEquals(RoomAuthority.MAX_ROOMCHAT_HISTORY, auth.getRoom().chatList.size());
        assertEquals("msg2", auth.getRoom().chatList.getFirst().strMessage);
    }

    // ---------------------------------------------------------------- single room create

    @Test
    void singleRoomCreateWithEmptyNameUsesDefaultTitle() {
        admit(0, "Alice");
        sink.clear();
        control(0, "singleroomcreate\t\t" + NetUtil.urlEncode("NET-SINGLE"));

        assertEquals("roomcreatesuccess\t0\t0\t-1", sink.lastDirectFor(0, "roomcreatesuccess"));
        assertNotNull(auth.getRoom());
        assertTrue(auth.getRoom().singleplayer);
        assertEquals("Single (Alice)", auth.getRoom().strName);
        assertEquals(1, auth.getRoom().maxPlayers);
        assertEquals(0, auth.getPlayer(0).seatID);
    }

    @Test
    void singleRoomCreateKeepsProvidedName() {
        admit(0, "Alice");
        control(0, "singleroomcreate\t" + NetUtil.urlEncode("My Single") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        assertEquals("My Single", auth.getRoom().strName);
    }

    @Test
    void singleRoomCreateIgnoredWhenSenderAlreadyInRoom() {
        admit(0, "Alice");
        control(0, "singleroomcreate\t" + NetUtil.urlEncode("First") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        sink.clear();
        control(0, "singleroomcreate\t" + NetUtil.urlEncode("Second") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        assertNull(sink.lastDirectFor(0, "roomcreatesuccess"));
        assertEquals("First", auth.getRoom().strName);
    }

    @Test
    void singleRoomCreateIgnoredWhenSessionAlreadyHasRoom() {
        admit(0, "Alice");
        admit(1, "Bob");
        control(0, "singleroomcreate\t" + NetUtil.urlEncode("First") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        sink.clear();
        control(1, "singleroomcreate\t" + NetUtil.urlEncode("Second") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        assertNull(sink.lastDirectFor(1, "roomcreatesuccess"));
        assertEquals(-1, auth.getPlayer(1).roomID);
    }

    // ---------------------------------------------------------------- room create variants

    @Test
    void roomCreateIgnoredWhenSenderAlreadyInRoom() {
        admit(0, "Alice");
        createRoom(0, "First", vsTemplate(2));
        sink.clear();
        createRoom(0, "Second", vsTemplate(2));
        assertNull(sink.lastDirectFor(0, "roomcreatesuccess"));
        assertEquals("First", auth.getRoom().strName);
    }

    @Test
    void roomCreateDefaultsEmptyNameAndClampsMaxPlayersLow() {
        admit(0, "Alice");
        createRoom(0, "", vsTemplate(0));
        assertEquals("No Title", auth.getRoom().strName);
        assertEquals(1, auth.getRoom().maxPlayers);
    }

    @Test
    void roomCreateClampsMaxPlayersHigh() {
        admit(0, "Alice");
        createRoom(0, "Big", vsTemplate(99));
        assertEquals(6, auth.getRoom().maxPlayers);
    }

    @Test
    void roomCreateWithRuleLockCachesAndSendsRule() {
        admit(0, "Alice");
        admit(1, "Bob");
        NetRoomInfo template = vsTemplate(2);
        template.ruleLock = true;
        createRoom(0, "Locked", template);

        assertTrue(auth.getRoom().ruleLock);
        assertEquals(1, sink.roomRuleCaches.size());
        assertNotNull(sink.lastDirectFor(0, "rulelock\t"));

        // Joiners are sent the locked rule too
        sink.clear();
        control(1, "roomjoin\t0\tfalse");
        assertNotNull(sink.lastDirectFor(1, "rulelock\t"));
    }

    @Test
    void roomCreateFallsBackToModeFromMessage() {
        admit(0, "Alice");
        NetRoomInfo template = vsTemplate(2);
        template.strMode = "";
        control(0, "roomcreate\t" + NetUtil.urlEncode("Modeless") + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE"));
        assertEquals("NET-VS-BATTLE", auth.getRoom().strMode);
    }

    @Test
    void useMapRoomWithoutMapPayloadHasNoMapsToServe() {
        admit(0, "Alice");
        admit(1, "Bob");
        NetRoomInfo template = vsTemplate(2);
        template.useMap = true;
        createRoom(0, "MapLess", template);   // 4-part message: no message[4] map data

        assertTrue(auth.getRoom().useMap);
        assertTrue(auth.getRoom().mapList.isEmpty());
        assertTrue(sink.mapCaches.isEmpty());

        // Joiner gets no "map" line either
        sink.clear();
        control(1, "roomjoin\t0\tfalse");
        assertNull(sink.lastDirectFor(1, "map\t"));
        assertNotNull(sink.lastDirectFor(1, "roomjoinsuccess\t"));
    }

    // ---------------------------------------------------------------- join variants

    @Test
    void roomJoinMinusOneFromLobbyIsSafe() {
        admit(0, "Alice");
        sink.clear();
        control(0, "roomjoin\t-1\tfalse");
        assertEquals("roomjoinsuccess\t-1\t-1\t-1", sink.lastDirectFor(0, "roomjoinsuccess"));
    }

    @Test
    void roomJoinToUnknownRoomFails() {
        admit(0, "Alice");
        sink.clear();
        control(0, "roomjoin\t5\tfalse");
        assertEquals("roomjoinfail", sink.lastDirectFor(0, "roomjoinfail"));
        assertEquals(-1, auth.getPlayer(0).roomID);
    }

    @Test
    void rejoiningTheSameRoomLeavesThenReenters() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Loop", vsTemplate(2));
        control(1, "roomjoin\t0\tfalse");

        sink.clear();
        control(1, "roomjoin\t0\tfalse");   // prevRoom == newRoom

        assertNotNull(sink.lastBroadcastStarting("playerleave\t1\t"));
        assertNotNull(sink.lastBroadcastStarting("playerenter\t1\t"));
        assertEquals(0, auth.getPlayer(1).roomID);
        assertNotNull(auth.getRoom());
    }

    @Test
    void joiningSingleplayerRoomAsPlayerTakesNoSeat() {
        admit(0, "Alice");
        admit(1, "Bob");
        control(0, "singleroomcreate\t" + NetUtil.urlEncode("Solo") + "\t" + NetUtil.urlEncode("NET-SINGLE"));

        sink.clear();
        control(1, "roomjoin\t0\tfalse");   // watch=false, but room is singleplayer
        assertEquals("roomjoinsuccess\t0\t-1\t-1", sink.lastDirectFor(1, "roomjoinsuccess"));
        assertEquals(-1, auth.getPlayer(1).seatID);
    }

    // ---------------------------------------------------------------- team / name / status guards

    @Test
    void changeTeamCoversNoOpChangeAndPlayingGuard() {
        admit(0, "Alice");
        createRoom(0, "Teams", vsTemplate(2));

        sink.clear();
        control(0, "changeteam");   // no argument -> "" == current team, no-op
        assertNull(sink.lastBroadcastStarting("changeteam\t"));

        control(0, "changeteam\t" + NetUtil.urlEncode("A"));
        assertEquals("A", auth.getPlayer(0).strTeam);
        assertNotNull(sink.lastBroadcastStarting("changeteam\t0\t"));

        sink.clear();
        control(0, "changeteam\t" + NetUtil.urlEncode("A"));   // same team again
        assertNull(sink.lastBroadcastStarting("changeteam\t"));

        auth.getPlayer(0).playing = true;
        control(0, "changeteam\t" + NetUtil.urlEncode("B"));
        assertEquals("A", auth.getPlayer(0).strTeam, "Mid-game team changes are refused");
        auth.getPlayer(0).playing = false;
    }

    @Test
    void changeNameWithoutArgumentFailsEmpty() {
        admit(0, "Alice");
        sink.clear();
        control(0, "changename");
        assertEquals("changenamefail\tEMPTY", sink.lastDirectFor(0, "changenamefail"));
    }

    @Test
    void changeStatusGuardsLobbyPlayingAndSingleplayer() {
        admit(0, "Alice");
        sink.clear();
        control(0, "changestatus\ttrue");   // lobby: roomID == -1
        assertTrue(sink.broadcasts.isEmpty());

        control(0, "singleroomcreate\t" + NetUtil.urlEncode("Solo") + "\t" + NetUtil.urlEncode("NET-SINGLE"));
        sink.clear();
        control(0, "changestatus\ttrue");   // singleplayer rooms have no spectators
        assertTrue(sink.broadcasts.isEmpty());
        assertEquals(0, auth.getPlayer(0).seatID);

        auth.getPlayer(0).playing = true;
        control(0, "changestatus\ttrue");   // playing guard fires first
        assertTrue(sink.broadcasts.isEmpty());
        auth.getPlayer(0).playing = false;
    }

    @Test
    void changeStatusToSpectatorFreesTheSeat() {
        admit(0, "Alice");
        admit(1, "Bob");
        createRoom(0, "Spec", vsTemplate(2));
        control(1, "roomjoin\t0\tfalse");

        sink.clear();
        control(1, "changestatus\ttrue");

        assertNotNull(sink.lastBroadcastStarting("changestatus\twatchonly\t1\t"));
        assertEquals(-1, auth.getPlayer(1).seatID);
        assertFalse(auth.getPlayer(1).ready);
    }

    @Test
    void changeStatusJoinsQueueWhenRoomIsFull() {
        admit(0, "Alice");
        admit(1, "Bob");
        admit(2, "Carol");
        createRoom(0, "Full", vsTemplate(2));
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\ttrue");    // Carol watches first

        sink.clear();
        control(2, "changestatus\tfalse");  // both seats taken -> queue

        assertNotNull(sink.lastBroadcastStarting("changestatus\tjoinqueue\t2\t"));
        assertEquals(-1, auth.getPlayer(2).seatID);
        assertEquals(0, auth.getPlayer(2).queueID);
    }
}
