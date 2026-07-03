// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for the v2 (room session) LAN announce format in {@link NetLanDiscovery}:
 * round trip, coexistence with v1, rejection by old clients, and session dedupe.
 */
class NetLanDiscoveryRoomTest {

    @Test
    void roomAnnounceRoundTrip() {
        byte[] data = NetLanDiscovery.encodeRoomAnnounce(
                9202, "Hoster", "0123456789abcdef0123456789abcdef", "Friday Lobby", 3);

        NetLanDiscovery.Announce a = NetLanDiscovery.decodeAnnounce(data, data.length, "192.168.1.5");

        assertNotNull(a);
        assertTrue(a.room);
        assertEquals("192.168.1.5", a.address);
        assertEquals(9202, a.port);
        assertEquals("Hoster", a.playerName);
        assertEquals("0123456789abcdef0123456789abcdef", a.sessionId);
        assertEquals("Friday Lobby", a.lobbyName);
        assertEquals(3, a.players);
    }

    @Test
    void legacyServerAnnounceDecodesAsNonRoom() {
        byte[] data = "NullpoLAN\t1\t9200\tServer+Guy\t7.5".getBytes(StandardCharsets.UTF_8);

        NetLanDiscovery.Announce a = NetLanDiscovery.decodeAnnounce(data, data.length, "10.0.0.2");

        assertNotNull(a);
        assertFalse(a.room);
        assertEquals("", a.sessionId);
        assertEquals(0, a.players);
    }

    @Test
    void oldClientRejectionIsPinnedByVersionField() {
        // Old clients only accept parts[1] == "1"; a v2 packet must carry "2" there
        // so that stale builds silently drop mesh announces.
        byte[] data = NetLanDiscovery.encodeRoomAnnounce(9202, "Hoster", "abc123", "Lobby", 1);
        String[] parts = new String(data, StandardCharsets.UTF_8).split("\t");
        assertEquals("2", parts[1]);
    }

    @Test
    void malformedRoomAnnouncesAreRejected() {
        byte[] wrongType = "NullpoLAN\t2\t9202\tname\t7.5\tX\tabc\tlobby\t2".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(wrongType, wrongType.length, "1.2.3.4"));

        byte[] emptySession = "NullpoLAN\t2\t9202\tname\t7.5\tM\t\tlobby\t2".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(emptySession, emptySession.length, "1.2.3.4"));

        byte[] truncated = "NullpoLAN\t2\t9202\tname\t7.5\tM\tabc".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(truncated, truncated.length, "1.2.3.4"));

        byte[] badPlayers = "NullpoLAN\t2\t9202\tname\t7.5\tM\tabc\tlobby\tmany".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(badPlayers, badPlayers.length, "1.2.3.4"));

        byte[] unknownVersion = "NullpoLAN\t3\t9202\tname\t7.5\tM\tabc\tlobby\t2".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(unknownVersion, unknownVersion.length, "1.2.3.4"));
    }

    @Test
    void dedupeCollapsesSameSessionToLowestHostPort() {
        List<NetLanDiscovery.Announce> in = new ArrayList<NetLanDiscovery.Announce>();
        in.add(room("192.168.1.20", 9202, "B", "sessA", 3));
        in.add(room("192.168.1.10", 9202, "A", "sessA", 3));
        in.add(room("192.168.1.30", 9202, "C", "sessB", 2));
        in.add(new NetLanDiscovery.Announce("192.168.1.40", 9200, "Server", "7.5"));

        List<NetLanDiscovery.Announce> out = NetLanDiscovery.dedupeBySession(in);

        assertEquals(3, out.size());
        assertEquals("192.168.1.10:9202", out.get(0).hostPort());
        assertEquals("sessA", out.get(0).sessionId);
        assertEquals("sessB", out.get(1).sessionId);
        assertFalse(out.get(2).room);
    }

    private static NetLanDiscovery.Announce room(String addr, int port, String name,
            String sessionId, int players) {
        return new NetLanDiscovery.Announce(addr, port, name, "7.5", sessionId, "Lobby", players);
    }
}
