// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetLanDiscovery}: announce packet codec and the
 * listener's collect/expire behavior.
 */
class NetLanDiscoveryTest {

    @Test
    void announceRoundTripPreservesPortAndName() {
        byte[] data = NetLanDiscovery.encodeAnnounce(9200, "Player One\tテスト");

        NetLanDiscovery.Announce announce =
                NetLanDiscovery.decodeAnnounce(data, data.length, "192.168.1.10");

        assertNotNull(announce);
        assertEquals("192.168.1.10", announce.address);
        assertEquals(9200, announce.port);
        assertEquals("Player One\tテスト", announce.playerName);
        assertEquals("192.168.1.10:9200", announce.hostPort());
    }

    @Test
    void decodeRejectsMalformedPackets() {
        byte[] wrongMagic = "NotNullpo\t1\t9200\tname\t7.5".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(wrongMagic, wrongMagic.length, "1.2.3.4"));

        byte[] wrongVersion = "NullpoLAN\t999\t9200\tname\t7.5".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(wrongVersion, wrongVersion.length, "1.2.3.4"));

        byte[] badPort = "NullpoLAN\t1\tnotaport\tname\t7.5".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(badPort, badPort.length, "1.2.3.4"));

        byte[] portOutOfRange = "NullpoLAN\t1\t99999\tname\t7.5".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(portOutOfRange, portOutOfRange.length, "1.2.3.4"));

        byte[] truncated = "NullpoLAN\t1\t9200".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeAnnounce(truncated, truncated.length, "1.2.3.4"));

        assertNull(NetLanDiscovery.decodeAnnounce(null, 0, "1.2.3.4"));
        assertNull(NetLanDiscovery.decodeAnnounce(new byte[8], 0, "1.2.3.4"));
        assertNull(NetLanDiscovery.decodeAnnounce(new byte[8], 16, "1.2.3.4"));
    }

    @Test
    void listenerCollectsUnicastAnnounces() throws Exception {
        NetLanDiscovery.Listener listener = new NetLanDiscovery.Listener(0, NetLanDiscovery.ENTRY_TTL_MS);
        listener.start();
        try {
            sendAnnounce(listener.getLocalPort(), 9200, "Hoster");

            List<NetLanDiscovery.Announce> snapshot = awaitSnapshot(listener, 1);
            assertEquals(1, snapshot.size());
            assertEquals(9200, snapshot.get(0).port);
            assertEquals("Hoster", snapshot.get(0).playerName);
        } finally {
            listener.shutdown();
            listener.join(5000);
            assertFalse(listener.isAlive(), "Listener thread should exit after shutdown");
        }
    }

    @Test
    void listenerExpiresEntriesAfterTtl() throws Exception {
        NetLanDiscovery.Listener listener = new NetLanDiscovery.Listener(0, 100);
        listener.start();
        try {
            sendAnnounce(listener.getLocalPort(), 9200, "Hoster");
            assertEquals(1, awaitSnapshot(listener, 1).size());

            Thread.sleep(300);
            assertTrue(listener.snapshot().isEmpty(), "Entry should expire after the TTL");
        } finally {
            listener.shutdown();
            listener.join(5000);
        }
    }

    private static void sendAnnounce(int listenerPort, int tcpPort, String name) throws Exception {
        byte[] data = NetLanDiscovery.encodeAnnounce(tcpPort, name);
        try (DatagramSocket sender = new DatagramSocket()) {
            sender.send(new DatagramPacket(data, data.length,
                    InetAddress.getByName("127.0.0.1"), listenerPort));
        }
    }

    /** Poll until the listener has received at least {@code count} entries (or time out). */
    private static List<NetLanDiscovery.Announce> awaitSnapshot(
            NetLanDiscovery.Listener listener, int count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        List<NetLanDiscovery.Announce> snapshot = listener.snapshot();
        while (snapshot.size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
            snapshot = listener.snapshot();
        }
        return snapshot;
    }
}
