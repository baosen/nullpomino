// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

/**
 * Round-trip tests for every {@link RoomProtocol} frame format.
 */
class RoomProtocolTest {

    private static String[] split(String line) {
        return line.split("\t", -1);
    }

    @Test
    void helloJoinRoundTrip() {
        String line = RoomProtocol.buildHelloJoin(7.5f, true, 9202, "Player Ø ne",
                new int[]{1622, 1500}, new int[]{12, 0}, new int[]{7, 0});
        RoomProtocol.Hello h = RoomProtocol.parseHello(split(line));

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
        String line = RoomProtocol.buildHelloPeer(7.5f, false, "aabbccdd", 3, 9300);
        RoomProtocol.Hello h = RoomProtocol.parseHello(split(line));

        assertNotNull(h);
        assertFalse(h.joinVariant);
        assertEquals("aabbccdd", h.token);
        assertEquals(3, h.uid);
        assertEquals(9300, h.listenPort);
    }

    @Test
    void welcomeRoundTripWithRoster() {
        List<RoomProtocol.RosterEntry> roster = new ArrayList<RoomProtocol.RosterEntry>();
        roster.add(new RoomProtocol.RosterEntry(0, "192.168.1.10", 9202, "Alice"));
        roster.add(new RoomProtocol.RosterEntry(2, "192.168.1.30", 9204, "Bob B"));

        String line = RoomProtocol.buildWelcome("tok", 5, "Carol", 0, 42, roster);
        RoomProtocol.Welcome w = RoomProtocol.parseWelcome(split(line));

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
        String line = RoomProtocol.buildWelcome("tok", 1, "Solo", 0, 0,
                new ArrayList<RoomProtocol.RosterEntry>());
        RoomProtocol.Welcome w = RoomProtocol.parseWelcome(split(line));

        assertNotNull(w);
        assertTrue(w.roster.isEmpty());
    }

    @Test
    void controlWrapPreservesTabsVerbatim() {
        String inner = "roomcreate\tMy Room\tblob;with;semis\tNET-VS-BATTLE";
        String wrapped = RoomProtocol.wrapControl(inner);

        assertEquals(inner, RoomProtocol.unwrapControl(wrapped));
        assertNull(RoomProtocol.unwrapControl("room\tb\t1\t-1\tfoo"));
    }

    @Test
    void broadcastWrapPreservesEmptyFields() {
        // The attack line's historic empty field (index 10 after uid/seat stamping)
        // must survive wrap/unwrap byte-for-byte.
        String attack = "game\t3\t1\tattack\t1\t0\t0\t0\t0\t0\t\tTSPIN\ttrue\t2\t0\tI\t2";
        String wrapped = RoomProtocol.wrapBroadcast(99, 4, attack);
        RoomProtocol.Broadcast b = RoomProtocol.parseBroadcast(wrapped);

        assertNotNull(b);
        assertEquals(99, b.seq);
        assertEquals(4, b.scope);
        assertEquals(attack, b.payload);
    }

    @Test
    void broadcastGlobalScope() {
        String wrapped = RoomProtocol.wrapBroadcast(7, RoomProtocol.SCOPE_GLOBAL, "playerupdate\tblob");
        RoomProtocol.Broadcast b = RoomProtocol.parseBroadcast(wrapped);

        assertNotNull(b);
        assertEquals(RoomProtocol.SCOPE_GLOBAL, b.scope);
        assertEquals("playerupdate\tblob", b.payload);
    }

    @Test
    void directWrapRoundTrip() {
        String inner = "roomjoinsuccess\t0\t1\t-1";
        assertEquals(inner, RoomProtocol.unwrapDirect(RoomProtocol.wrapDirect(inner)));
    }

    @Test
    void authGlobalRoundTrip() {
        RoomProtocol.AuthGlobal g = RoomProtocol.parseAuthGlobal(
                split(RoomProtocol.buildAuthGlobal(17, 6)));

        assertNotNull(g);
        assertEquals(17, g.seq);
        assertEquals(6, g.nextUid);
    }

    @Test
    void authRoomRoundTrip() {
        RoomProtocol.AuthRoom in = new RoomProtocol.AuthRoom(
                21, 2, true, 3, 1, false, true, 4,
                new int[]{0, -1, 2, 5}, new int[]{0, -1, 2}, new int[]{2}, new int[0]);

        RoomProtocol.AuthRoom out = RoomProtocol.parseAuthRoom(split(RoomProtocol.buildAuthRoom(in)));

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
        assertEquals("", RoomProtocol.joinUids(new int[0]));
        assertArrayEquals(new int[0], RoomProtocol.parseUids(""));
        assertArrayEquals(new int[]{-1, 0, 42}, RoomProtocol.parseUids(RoomProtocol.joinUids(new int[]{-1, 0, 42})));
    }

    @Test
    void tokenIsThirtyTwoHexChars() {
        String token = RoomProtocol.generateToken(new Random(1234));
        assertEquals(32, token.length());
        assertTrue(token.matches("[0-9a-f]{32}"));
    }

    @Test
    void malformedFramesParseToNull() {
        assertNull(RoomProtocol.parseHello(split("room\thello\tunknown\t1")));
        assertNull(RoomProtocol.parseHello(split("room\thello\tjoin\tnotafloat\ttrue\t9202\tx")));
        assertNull(RoomProtocol.parseWelcome(split("room\twelcome\ttok\t1\tx\t0\t0\t5\tonly;one;9202;entry")));
        assertNull(RoomProtocol.parseBroadcast("room\tb\tnotanumber\t-1\tx"));
        assertNull(RoomProtocol.parseAuthRoom(split("room\tauthr\t1\t2\ttrue")));
        assertNull(RoomProtocol.parsePeerAnnounce(split("room\tpeer\tnotanint\thost\t9\tx")));
    }

    @Test
    void roomFrameDetection() {
        assertTrue(RoomProtocol.isRoomFrame("room\tping"));
        assertFalse(RoomProtocol.isRoomFrame("game\t1\t0\tpiece\t1"));
        assertFalse(RoomProtocol.isRoomFrame("roomsomething\telse"));
    }
}
