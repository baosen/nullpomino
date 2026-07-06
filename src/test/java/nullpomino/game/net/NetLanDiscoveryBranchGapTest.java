// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link NetLanDiscovery}: malformed presence/chat
 * packets, null room-detail normalization, the announcer lifecycle, chat-id
 * eviction and the listener's non-shutdown exit paths.
 */
class NetLanDiscoveryBranchGapTest {

    // ----- decodePresence guard branches -----

    @Test
    void decodePresenceRejectsBadLengthAndHeader() {
        byte[] valid = NetLanDiscovery.encodePresence("Gap", "gap-inst");
        assertNotNull(NetLanDiscovery.decodePresence(valid, valid.length));

        assertNull(NetLanDiscovery.decodePresence(null, 4));
        assertNull(NetLanDiscovery.decodePresence(new byte[4], 0));
        assertNull(NetLanDiscovery.decodePresence(new byte[4], -1));
        assertNull(NetLanDiscovery.decodePresence(new byte[4], 8));

        byte[] wrongMagic = "NotNullpo\t2\t0\tname\t7.5\tP\tid".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodePresence(wrongMagic, wrongMagic.length));

        byte[] wrongVersion = "NullpoLAN\t1\t0\tname\t7.5\tP\tid".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodePresence(wrongVersion, wrongVersion.length));

        byte[] badVersion = "NullpoLAN\tv\t0\tname\t7.5\tP\tid".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodePresence(badVersion, badVersion.length));

        byte[] wrongType = "NullpoLAN\t2\t0\tname\t7.5\tX\tid".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodePresence(wrongType, wrongType.length));

        // Empty instanceId with a trailing pad field so split() keeps 7+ parts
        byte[] emptyId = "NullpoLAN\t2\t0\tname\t7.5\tP\t\tpad".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodePresence(emptyId, emptyId.length));
    }

    // ----- decodeChat guard branches -----

    @Test
    void decodeChatRejectsBadLengthAndHeader() {
        byte[] valid = NetLanDiscovery.encodeChat("Gap", "gap-id", "hi");
        assertNotNull(NetLanDiscovery.decodeChat(valid, valid.length));

        assertNull(NetLanDiscovery.decodeChat(null, 4));
        assertNull(NetLanDiscovery.decodeChat(new byte[4], 0));
        assertNull(NetLanDiscovery.decodeChat(new byte[4], -1));
        assertNull(NetLanDiscovery.decodeChat(new byte[4], 8));

        byte[] wrongMagic = "NotNullpo\t2\t0\tname\t7.5\tC\tid\tmsg".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeChat(wrongMagic, wrongMagic.length));

        byte[] wrongVersion = "NullpoLAN\t1\t0\tname\t7.5\tC\tid\tmsg".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeChat(wrongVersion, wrongVersion.length));

        byte[] badVersion = "NullpoLAN\tv\t0\tname\t7.5\tC\tid\tmsg".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeChat(badVersion, badVersion.length));

        byte[] wrongType = "NullpoLAN\t2\t0\tname\t7.5\tX\tid\tmsg".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeChat(wrongType, wrongType.length));

        byte[] emptyMsgId = "NullpoLAN\t2\t0\tname\t7.5\tC\t\tmsg".getBytes(StandardCharsets.UTF_8);
        assertNull(NetLanDiscovery.decodeChat(emptyMsgId, emptyMsgId.length));
    }

    // ----- Announce constructor null normalization -----

    @Test
    void announceConstructorNormalizesNullFields() {
        NetLanDiscovery.Announce a = new NetLanDiscovery.Announce("10.0.0.1", 9200, "Gap", "7.5",
                null, null, 0, null, false, null, null, false, 0, 0, 0);

        assertFalse(a.room, "null sessionId means not a room");
        assertEquals("", a.sessionId);
        assertEquals("", a.lobbyName);
        assertEquals("", a.roomName);
        assertEquals("", a.ruleName);
        assertEquals("", a.mode);
    }

    // ----- markSeen cap eviction -----

    @Test
    void markSeenEvictsOldestIdPastCap() throws Exception {
        NetLanDiscovery.Listener listener = new NetLanDiscovery.Listener(0, 1000);
        try {
            // 257 fresh ids: one over the 256 cap, so the oldest ("m0") is evicted
            for (int i = 0; i < 257; i++) {
                assertTrue(listener.markSeen("m" + i));
            }
            assertTrue(listener.markSeen("m0"), "Evicted id should be accepted as new again");
            assertFalse(listener.markSeen("m5"), "Retained id must still dedupe");
        } finally {
            listener.shutdown();
        }
    }

    // ----- listener: garbage packets and chat with no consumer -----

    @Test
    void listenerIgnoresGarbageAndDropsChatWithoutConsumer() throws Exception {
        NetLanDiscovery.Listener listener = new NetLanDiscovery.Listener(0, NetLanDiscovery.ENTRY_TTL_MS);
        listener.start();
        try (DatagramSocket sender = new DatagramSocket()) {
            InetAddress local = InetAddress.getByName("127.0.0.1");
            int port = listener.getLocalPort();

            // Garbage that decodes as neither announce, chat nor presence
            byte[] garbage = "hello there".getBytes(StandardCharsets.UTF_8);
            sender.send(new DatagramPacket(garbage, garbage.length, local, port));

            // A chat while no consumer is installed - silently dropped
            byte[] orphanChat = NetLanDiscovery.encodeChat("Gap", "gap-orphan", "lost");
            sender.send(new DatagramPacket(orphanChat, orphanChat.length, local, port));

            // An announce as an ordering barrier: once it shows up, the two
            // packets above (same socket, loopback FIFO) have been processed
            byte[] announce = NetLanDiscovery.encodeRoomAnnounce(new NetLanDiscovery.Announce(
                    "", 9202, "Gap", "7.5", "gap-barrier", "Lobby", 1));
            sender.send(new DatagramPacket(announce, announce.length, local, port));
            awaitSnapshotSize(listener, 1);

            BlockingQueue<String> received = new LinkedBlockingQueue<String>();
            listener.setChatConsumer((name, msg) -> received.add(name + "|" + msg));
            byte[] chat = NetLanDiscovery.encodeChat("Gap", "gap-live", "heard");
            sender.send(new DatagramPacket(chat, chat.length, local, port));

            assertEquals("Gap|heard", received.poll(5, TimeUnit.SECONDS));
            assertTrue(received.isEmpty(), "Consumer-less chat must not be replayed");
        } finally {
            listener.shutdown();
            listener.join(5000);
        }
    }

    // ----- listener: exit paths that don't go through shutdown() -----

    @Test
    void listenerExitsLoopWhenShutdownFlagSetWithoutClosingSocket() throws Exception {
        NetLanDiscovery.Listener listener = new NetLanDiscovery.Listener(0, NetLanDiscovery.ENTRY_TTL_MS);
        listener.start();
        try {
            // Set only the flag (no socket close): the next 500ms receive
            // timeout re-checks the while condition and exits normally
            Field flag = NetLanDiscovery.Listener.class.getDeclaredField("shutdownRequested");
            flag.setAccessible(true);
            flag.setBoolean(listener, true);

            listener.join(5000);
            assertFalse(listener.isAlive(), "Listener should exit via the loop condition");
        } finally {
            listener.shutdown();
        }
    }

    @Test
    void listenerBreaksWhenSocketDiesWithoutShutdown() throws Exception {
        NetLanDiscovery.Listener listener = new NetLanDiscovery.Listener(0, NetLanDiscovery.ENTRY_TTL_MS);
        listener.start();
        try {
            // Close the socket underneath the thread while shutdownRequested is
            // still false: receive() throws and the listener logs + breaks
            Field socketField = NetLanDiscovery.Listener.class.getDeclaredField("socket");
            socketField.setAccessible(true);
            ((DatagramSocket) socketField.get(listener)).close();

            listener.join(5000);
            assertFalse(listener.isAlive(), "Listener should stop after an unexpected socket error");
        } finally {
            listener.shutdown();
        }
    }

    // ----- announcer lifecycle -----

    @Test
    void announcerSkipsLoopWhenShutdownBeforeRun() {
        NetLanDiscovery.Announcer announcer = new NetLanDiscovery.Announcer(() -> null);
        announcer.shutdown(); // socket not created yet - the null branch of shutdown()
        Runnable sameThread = announcer; // deliberate same-thread run for determinism
        sameThread.run(); // opens the socket, sees the flag, closes
        assertFalse(announcer.isAlive());
    }

    @Test
    void announcerSkipsNullPayloadThenBroadcasts() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch broadcastCycle = new CountDownLatch(1);
        NetLanDiscovery.Announcer announcer = new NetLanDiscovery.Announcer(() -> {
            if (calls.incrementAndGet() == 1) return null; // "nothing to announce yet" cycle
            broadcastCycle.countDown();
            return NetLanDiscovery.encodePresence("Gap", "gap-announcer");
        });
        announcer.start();
        try {
            assertTrue(broadcastCycle.await(10, TimeUnit.SECONDS),
                    "Announcer should poll again after a null payload");
        } finally {
            announcer.shutdown();
            announcer.join(5000);
            assertFalse(announcer.isAlive(), "Announcer thread should exit after shutdown");
        }
    }

    // ----- broadcast helpers -----

    @Test
    void broadcastTargetsIncludeLimitedBroadcastAndPacketSendIsBestEffort() throws Exception {
        Set<InetAddress> targets = NetLanDiscovery.broadcastTargets();
        assertTrue(targets.contains(InetAddress.getByName("255.255.255.255")),
                "The limited broadcast address is always a target");

        // Best-effort one-shot broadcast: must not throw even if sends fail
        NetLanDiscovery.broadcastPacket(NetLanDiscovery.encodeChat("Gap", "gap-bcast", "ping"));
    }

    /** Poll until the listener has at least {@code count} entries (or time out). */
    private static List<NetLanDiscovery.Announce> awaitSnapshotSize(
            NetLanDiscovery.Listener listener, int count) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 5000;
        List<NetLanDiscovery.Announce> snapshot = listener.snapshot();
        while (snapshot.size() < count && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
            snapshot = listener.snapshot();
        }
        assertEquals(count, snapshot.size());
        return snapshot;
    }
}
