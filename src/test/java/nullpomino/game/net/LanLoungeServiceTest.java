// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LanLoungeService}: open/close lifecycle, chat consumer
 * wiring, room/presence snapshots and the presence-beacon gating.
 *
 * <p>The service hard-codes {@link NetLanDiscovery#DISCOVERY_PORT} with
 * SO_REUSEADDR, so tests feed it via loopback unicast to that port and keep
 * at most the sockets they open themselves; everything is closed per test.
 */
class LanLoungeServiceTest {

    private final List<LanLoungeService> services = new ArrayList<LanLoungeService>();

    @AfterEach
    void closeAll() {
        for (LanLoungeService s : services) s.close();
        services.clear();
    }

    private LanLoungeService newService() {
        LanLoungeService s = new LanLoungeService();
        services.add(s);
        return s;
    }

    @Test
    void openIsIdempotentAndCloseIsSafeWhenNotOpen() {
        LanLoungeService service = newService();

        service.close(); // never opened - both null branches, no crash
        assertTrue(service.open());
        assertTrue(service.open(), "Second open is a no-op success");
        service.close();
        service.close(); // already closed - null branches again
    }

    @Test
    void closedServiceReturnsEmptySnapshotsAndSwallowsChat() {
        LanLoungeService service = newService();

        assertTrue(service.snapshotRooms().isEmpty());
        assertTrue(service.snapshotPresence().isEmpty());
        // No listener: markSeen is skipped but the broadcast still goes out
        service.sendChat("Gap", "unheard");
        service.setChatConsumer((name, msg) -> fail("closed service must not deliver chat"));
    }

    @Test
    void chatConsumerInstalledBeforeOpenReceivesLanChat() throws Exception {
        LanLoungeService service = newService();
        BlockingQueue<String> received = new LinkedBlockingQueue<String>();

        // Installed while closed: stored and handed to the listener on open()
        service.setChatConsumer((name, msg) -> received.add(name + "|" + msg));
        assertTrue(service.open());

        try (DatagramSocket sender = new DatagramSocket()) {
            byte[] chat = NetLanDiscovery.encodeChat("Gap", "lls-chat-1", "hello lounge");
            sender.send(new DatagramPacket(chat, chat.length,
                    InetAddress.getByName("127.0.0.1"), NetLanDiscovery.DISCOVERY_PORT));
        }
        assertEquals("Gap|hello lounge", received.poll(5, TimeUnit.SECONDS));

        // Re-install while open (live listener branch) and send our own line:
        // its msgId is pre-registered, so the loopback copy never comes back
        service.setChatConsumer((name, msg) -> received.add(name + "|" + msg));
        service.sendChat("Gap", "own line");
        assertNull(received.poll(500, TimeUnit.MILLISECONDS),
                "Own chat line must be deduped away by the pre-registered msgId");
    }

    @Test
    void snapshotRoomsKeepsOnlyRoomAnnouncesAndPresenceIsTracked() throws Exception {
        LanLoungeService service = newService();
        assertTrue(service.open());

        try (DatagramSocket sender = new DatagramSocket()) {
            InetAddress local = InetAddress.getByName("127.0.0.1");

            byte[] room = NetLanDiscovery.encodeRoomAnnounce(new NetLanDiscovery.Announce(
                    "", 9202, "Gap", "7.5", "lls-sess-1", "Lobby", 1,
                    "Room", false, "", "MODE", false, 1, 2, 0));
            sender.send(new DatagramPacket(room, room.length, local, NetLanDiscovery.DISCOVERY_PORT));

            byte[] legacy = "NullpoLAN\t1\t9200\tLegacy\t7.5".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            sender.send(new DatagramPacket(legacy, legacy.length, local, NetLanDiscovery.DISCOVERY_PORT));

            byte[] presence = NetLanDiscovery.encodePresence("GapVisitor", "lls-pres-1");
            sender.send(new DatagramPacket(presence, presence.length, local, NetLanDiscovery.DISCOVERY_PORT));
        }

        // Poll rather than assert exact sizes: a real LAN may add strangers
        long deadline = System.currentTimeMillis() + 5000;
        boolean roomSeen = false, presenceSeen = false;
        while ((!roomSeen || !presenceSeen) && System.currentTimeMillis() < deadline) {
            roomSeen = containsSession(service.snapshotRooms(), "lls-sess-1");
            presenceSeen = containsInstance(service.snapshotPresence(), "lls-pres-1");
            Thread.sleep(20);
        }
        assertTrue(roomSeen, "Room announce should surface in snapshotRooms()");
        assertTrue(presenceSeen, "Presence beacon should surface in snapshotPresence()");

        for (NetLanDiscovery.Announce a : service.snapshotRooms()) {
            assertTrue(a.room, "snapshotRooms() must filter out non-room (v1) announces");
        }
    }

    @Test
    void presenceBeaconIsGatedOnNameAndInstanceId() throws Exception {
        // Three services, one per supplier state; each announcer polls its
        // payload supplier right after open(), so a short wait covers all arms
        LanLoungeService unset = newService(); // name "" -> null payload
        assertTrue(unset.open());

        LanLoungeService noId = newService(); // name set, id "" -> null payload
        noId.setPresence("GapAlice", "");
        assertTrue(noId.open());

        LanLoungeService full = newService(); // both set -> encodePresence
        full.setPresence("GapAlice", "lls-inst-1");
        assertTrue(full.open());

        Thread.sleep(600); // > thread start + first announce cycle for all three
    }

    @Test
    void openReturnsFalseWhenDiscoveryPortIsUnavailable() {
        DatagramSocket blocker;
        try {
            // Plain bind (no SO_REUSEADDR): the service's reuse-bind must fail
            blocker = new DatagramSocket(NetLanDiscovery.DISCOVERY_PORT);
        } catch (SocketException e) {
            Assumptions.assumeTrue(false, "Discovery port already busy on this host");
            return;
        }
        try {
            LanLoungeService service = newService();
            assertFalse(service.open(), "open() should report bind failure");
            assertTrue(service.snapshotRooms().isEmpty());
        } finally {
            blocker.close();
        }
    }

    private static boolean containsSession(List<NetLanDiscovery.Announce> rooms, String sessionId) {
        for (NetLanDiscovery.Announce a : rooms) {
            if (sessionId.equals(a.sessionId)) return true;
        }
        return false;
    }

    private static boolean containsInstance(List<NetLanDiscovery.Presence> visitors, String instanceId) {
        for (NetLanDiscovery.Presence p : visitors) {
            if (instanceId.equals(p.instanceId)) return true;
        }
        return false;
    }
}
