// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for {@link MeshMirror}: a mirror fed through the real
 * frame codecs must reconstruct the authority state so exactly that a
 * successor authority adopted from it continues the game correctly.
 */
class MeshMirrorTest {

    /** Sink that feeds a mirror through the actual wire-frame codecs */
    private final class LoopSink implements MeshAuthority.Sink {
        long seq = 0;
        final List<String> broadcastLines = new ArrayList<String>();

        public void broadcast(int scope, String line, int exceptUid) {
            seq++;
            // Round-trip through the broadcast frame codec
            MeshProtocol.Broadcast b = MeshProtocol.parseBroadcast(MeshProtocol.wrapBroadcast(seq, scope, line));
            mirror.applyBroadcast(b.seq, b.scope, b.payload);
            broadcastLines.add(line);
        }
        public void direct(int uid, String line) {}
        public void ruleCache(int uid, String checksum, String data) {
            String[] parts = MeshProtocol.buildSnapRule(uid, checksum, data).split("\t", -1);
            mirror.applySnapshot(parts);
        }
        public void roomRuleCache(int roomId, String data) {
            mirror.applySnapshot(MeshProtocol.buildSnapRoomRule(roomId, data).split("\t", -1));
        }
        public void mapCache(int roomId, String data) {
            mirror.applySnapshot(MeshProtocol.buildSnapMap(roomId, data).split("\t", -1));
        }
        public void authUpdate() {
            seq++;
            MeshProtocol.AuthGlobal g = MeshProtocol.parseAuthGlobal(
                    MeshProtocol.buildAuthGlobal(seq, auth.getNextUid(), auth.getNextRoomId()).split("\t", -1));
            mirror.applyAuthGlobal(g);
            for (NetRoomInfo room : auth.getRooms()) {
                MeshProtocol.AuthRoom a = MeshProtocol.parseAuthRoom(
                        MeshProtocol.buildAuthRoom(MeshAuthority.buildAuthRoom(seq, room)).split("\t", -1));
                mirror.applyAuthRoom(a);
            }
        }
    }

    private MeshMirror mirror;
    private MeshAuthority auth;
    private LoopSink loopSink;

    @BeforeEach
    void setUp() {
        mirror = new MeshMirror();
        loopSink = new LoopSink();
        auth = new MeshAuthority(loopSink, new Random(7));
    }

    private void admit(String name) {
        int uid = auth.reserveUid();
        auth.admitMember(uid, name, "127.0.0.1");
        auth.getPlayer(uid).ruleOpt = new RuleOptions();
    }

    private void control(int uid, String line) {
        auth.handleControl(uid, line.split("\t", -1));
    }

    private void createThreeSeatRoom() {
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = 3;
        template.strMode = "NET-VS-BATTLE";
        control(0, "roomcreate\t" + NetUtil.urlEncode("Round") + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE"));
        control(1, "roomjoin\t0\tfalse");
        control(2, "roomjoin\t0\tfalse");
    }

    @Test
    void mirrorTracksRosterRoomsAndCounters() {
        admit("Alice");
        admit("Bob");
        admit("Carol");
        createThreeSeatRoom();

        assertEquals(3, mirror.getPlayers().size());
        assertEquals("Bob", mirror.getPlayer(1).strName);
        assertEquals(1, mirror.getPlayer(1).seatID);
        assertNotNull(mirror.getRoom(0));
        assertEquals(3, mirror.getNextUid());
        assertEquals(1, mirror.getNextRoomId());
        assertEquals(loopSink.seq, mirror.getSeq());
    }

    @Test
    void authFramesReconstructMidGameStateExactly() {
        admit("Alice");
        admit("Bob");
        admit("Carol");
        createThreeSeatRoom();
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        control(2, "ready\ttrue");
        control(2, "dead\t0");    // Carol dies (place 3), game continues

        NetRoomInfo authRoom = auth.getRoomInfo(0);
        NetRoomInfo mirrorRoom = mirror.getRoom(0);

        // Compare the full non-derivable state via the frame builder
        assertEquals(MeshProtocol.buildAuthRoom(MeshAuthority.buildAuthRoom(0, authRoom)),
                MeshProtocol.buildAuthRoom(MeshAuthority.buildAuthRoom(0, mirrorRoom)));
        assertTrue(mirrorRoom.playing);
        assertEquals(3, mirrorRoom.startPlayers);
        assertEquals(1, mirrorRoom.deadCount);
        assertFalse(mirror.getPlayer(2).playing);
    }

    @Test
    void promotedMirrorContinuesTheGameCorrectly() {
        admit("Alice");
        admit("Bob");
        admit("Carol");
        createThreeSeatRoom();
        control(0, "ready\ttrue");
        control(1, "ready\ttrue");
        control(2, "ready\ttrue");
        control(2, "dead\t0");    // Carol out, place 3

        // Arbiter (Alice, uid 0) vanishes: promote the mirror into a successor authority
        mirror.promote();
        final List<String> successorLines = new ArrayList<String>();
        MeshAuthority successor = new MeshAuthority(new MeshAuthority.Sink() {
            public void broadcast(int scope, String line, int exceptUid) { successorLines.add(scope + "|" + line); }
            public void direct(int uid, String line) {}
            public void ruleCache(int uid, String checksum, String data) {}
            public void roomRuleCache(int roomId, String data) {}
            public void mapCache(int roomId, String data) {}
            public void authUpdate() {}
        }, new Random(8));
        successor.adoptState(mirror.getPlayers(), mirror.getRooms(), mirror.getRuleBlobs());
        successor.restoreCounters(mirror.getNextUid(), mirror.getNextRoomId());

        // The old arbiter's departure mid-game: dead line with the RIGHT place, then finish
        successor.onMemberGone(0, false);

        String deadLine = null, finishLine = null;
        for (String s : successorLines) {
            if (s.contains("|dead\t")) deadLine = s;
            if (s.contains("|finish\t")) finishLine = s;
        }
        assertNotNull(deadLine);
        // place = startPlayers(3) - deadCount(1) = 2, scope = room 0
        assertEquals("0|dead\t0\t" + NetUtil.urlEncode("Alice") + "\t0\t2\t-1\t", deadLine);
        assertNotNull(finishLine);
        assertTrue(finishLine.startsWith("0|finish\t1\t1\t" + NetUtil.urlEncode("Bob")),
                "Bob is the last one standing: " + finishLine);

        // Counters survive so future rooms/uids never collide
        assertEquals(3, successor.getNextUid());
        assertEquals(1, successor.getNextRoomId());
    }

    @Test
    void chatAndLogoutReplicate() {
        admit("Alice");
        admit("Bob");
        createThreeSeatRoom();   // Carol absent: only 2 join
        control(0, "chat\t" + NetUtil.urlEncode("gl hf"));
        control(0, "lobbychat\t" + NetUtil.urlEncode("hello lobby"));

        assertEquals(1, mirror.getRoom(0).chatList.size());
        assertEquals("gl hf", mirror.getRoom(0).chatList.getFirst().strMessage);
        assertEquals(1, mirror.getLobbyChatList().size());

        auth.onMemberGone(1, true);
        assertNull(mirror.getPlayer(1));
    }

    @Test
    void roomDeleteCleansMirror() {
        admit("Alice");
        admit("Bob");
        createThreeSeatRoom();
        control(0, "roomjoin\t-1\tfalse");
        control(1, "roomjoin\t-1\tfalse");

        assertNull(mirror.getRoom(0));
        assertTrue(mirror.getRooms().isEmpty());
    }

    @Test
    void promoteRestoresPlayerListDeterministically() {
        admit("Alice");
        admit("Bob");
        admit("Carol");
        createThreeSeatRoom();

        mirror.promote();
        NetRoomInfo room = mirror.getRoom(0);
        assertEquals(3, room.playerList.size());
        assertEquals(0, room.playerList.get(0).uid);
        assertEquals(1, room.playerList.get(1).uid);
        assertEquals(2, room.playerList.get(2).uid);
    }
}
