// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for every {@link MeshProtocol} frame format.
 */
class MeshProtocolTest {

    private static String[] split(String line) {
        return line.split("\t", -1);
    }

    @Test
    void helloJoinRoundTrip() {
        String line = MeshProtocol.buildHelloJoin(7.5f, true, 9202, "Player Ø ne",
                new int[]{1622, 1500}, new int[]{12, 0}, new int[]{7, 0});
        MeshProtocol.Hello h = MeshProtocol.parseHello(split(line));

        assertNotNull(h);
        assertTrue(h.joinVariant);
        assertEquals(7.5f, h.verMajor);
        assertTrue(h.devBuild);
        assertEquals(9202, h.listenPort);
        assertEquals("Player Ø ne", h.name);
        assertArrayEquals(new int[]{1622, 1500}, h.ratings);
        assertArrayEquals(new int[]{12, 0}, h.playCounts);
        assertArrayEquals(new int[]{7, 0}, h.winCounts);
    }

    @Test
    void helloPeerRoundTrip() {
        String line = MeshProtocol.buildHelloPeer(7.5f, false, "aabbccdd", 3, 9300);
        MeshProtocol.Hello h = MeshProtocol.parseHello(split(line));

        assertNotNull(h);
        assertFalse(h.joinVariant);
        assertEquals("aabbccdd", h.token);
        assertEquals(3, h.uid);
        assertEquals(9300, h.listenPort);
    }

    @Test
    void welcomeRoundTripWithRoster() {
        List<MeshProtocol.RosterEntry> roster = new ArrayList<MeshProtocol.RosterEntry>();
        roster.add(new MeshProtocol.RosterEntry(0, "192.168.1.10", 9202, "Alice"));
        roster.add(new MeshProtocol.RosterEntry(2, "192.168.1.30", 9204, "Bob B"));

        String line = MeshProtocol.buildWelcome("tok", 5, "Carol", 0, 42, roster);
        MeshProtocol.Welcome w = MeshProtocol.parseWelcome(split(line));

        assertNotNull(w);
        assertEquals("tok", w.token);
        assertEquals(5, w.uid);
        assertEquals("Carol", w.name);
        assertEquals(0, w.arbiterUid);
        assertEquals(42, w.lastSeq);
        assertEquals(2, w.roster.size());
        assertEquals("192.168.1.30", w.roster.get(1).host);
        assertEquals("Bob B", w.roster.get(1).name);
    }

    @Test
    void welcomeWithEmptyRoster() {
        String line = MeshProtocol.buildWelcome("tok", 1, "Solo", 0, 0,
                new ArrayList<MeshProtocol.RosterEntry>());
        MeshProtocol.Welcome w = MeshProtocol.parseWelcome(split(line));

        assertNotNull(w);
        assertTrue(w.roster.isEmpty());
    }

    @Test
    void controlWrapPreservesTabsVerbatim() {
        String inner = "roomcreate\tMy Room\tblob;with;semis\tNET-VS-BATTLE";
        String wrapped = MeshProtocol.wrapControl(inner);

        assertEquals(inner, MeshProtocol.unwrapControl(wrapped));
        assertNull(MeshProtocol.unwrapControl("mesh\tb\t1\t-1\tfoo"));
    }

    @Test
    void broadcastWrapPreservesEmptyFields() {
        // The attack line's historic empty field (index 10 after uid/seat stamping)
        // must survive wrap/unwrap byte-for-byte.
        String attack = "game\t3\t1\tattack\t1\t0\t0\t0\t0\t0\t\tTSPIN\ttrue\t2\t0\tI\t2";
        String wrapped = MeshProtocol.wrapBroadcast(99, 4, attack);
        MeshProtocol.Broadcast b = MeshProtocol.parseBroadcast(wrapped);

        assertNotNull(b);
        assertEquals(99, b.seq);
        assertEquals(4, b.scope);
        assertEquals(attack, b.payload);
    }

    @Test
    void broadcastGlobalScope() {
        String wrapped = MeshProtocol.wrapBroadcast(7, MeshProtocol.SCOPE_GLOBAL, "playerupdate\tblob");
        MeshProtocol.Broadcast b = MeshProtocol.parseBroadcast(wrapped);

        assertNotNull(b);
        assertEquals(MeshProtocol.SCOPE_GLOBAL, b.scope);
        assertEquals("playerupdate\tblob", b.payload);
    }

    @Test
    void directWrapRoundTrip() {
        String inner = "roomjoinsuccess\t0\t1\t-1";
        assertEquals(inner, MeshProtocol.unwrapDirect(MeshProtocol.wrapDirect(inner)));
    }

    @Test
    void authGlobalRoundTrip() {
        MeshProtocol.AuthGlobal g = MeshProtocol.parseAuthGlobal(
                split(MeshProtocol.buildAuthGlobal(17, 6, 3)));

        assertNotNull(g);
        assertEquals(17, g.seq);
        assertEquals(6, g.nextUid);
        assertEquals(3, g.nextRoomId);
    }

    @Test
    void authRoomRoundTrip() {
        MeshProtocol.AuthRoom in = new MeshProtocol.AuthRoom(
                21, 2, true, 3, 1, false, true, 4,
                new int[]{0, -1, 2, 5}, new int[]{0, -1, 2}, new int[]{2}, new int[0]);

        MeshProtocol.AuthRoom out = MeshProtocol.parseAuthRoom(split(MeshProtocol.buildAuthRoom(in)));

        assertNotNull(out);
        assertEquals(21, out.seq);
        assertEquals(2, out.roomId);
        assertTrue(out.playing);
        assertEquals(3, out.startPlayers);
        assertEquals(1, out.deadCount);
        assertFalse(out.autoStartActive);
        assertTrue(out.isSomeoneCancelled);
        assertEquals(4, out.mapPrevious);
        assertArrayEquals(new int[]{0, -1, 2, 5}, out.seatUids);
        assertArrayEquals(new int[]{0, -1, 2}, out.nowPlayingUids);
        assertArrayEquals(new int[]{2}, out.deadUids);
        assertArrayEquals(new int[0], out.queueUids);
    }

    @Test
    void uidListRoundTripIncludingEmpty() {
        assertEquals("", MeshProtocol.joinUids(new int[0]));
        assertArrayEquals(new int[0], MeshProtocol.parseUids(""));
        assertArrayEquals(new int[]{-1, 0, 42}, MeshProtocol.parseUids(MeshProtocol.joinUids(new int[]{-1, 0, 42})));
    }

    @Test
    void tokenIsThirtyTwoHexChars() {
        String token = MeshProtocol.generateToken(new Random(1234));
        assertEquals(32, token.length());
        assertTrue(token.matches("[0-9a-f]{32}"));
    }

    @Test
    void malformedFramesParseToNull() {
        assertNull(MeshProtocol.parseHello(split("mesh\thello\tunknown\t1")));
        assertNull(MeshProtocol.parseHello(split("mesh\thello\tjoin\tnotafloat\ttrue\t9202\tx")));
        assertNull(MeshProtocol.parseWelcome(split("mesh\twelcome\ttok\t1\tx\t0\t0\t5\tonly;one;9202;entry")));
        assertNull(MeshProtocol.parseBroadcast("mesh\tb\tnotanumber\t-1\tx"));
        assertNull(MeshProtocol.parseAuthRoom(split("mesh\tauthr\t1\t2\ttrue")));
        assertNull(MeshProtocol.parsePeerAnnounce(split("mesh\tpeer\tnotanint\thost\t9\tx")));
    }

    @Test
    void meshFrameDetection() {
        assertTrue(MeshProtocol.isMeshFrame("mesh\tping"));
        assertFalse(MeshProtocol.isMeshFrame("game\t1\t0\tpiece\t1"));
        assertFalse(MeshProtocol.isMeshFrame("meshsomething\telse"));
    }
}
