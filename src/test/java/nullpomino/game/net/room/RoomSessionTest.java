// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.zip.Adler32;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.util.CustomProperties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Wire-level tests for {@link RoomSession}: real sockets on ephemeral ports,
 * session formation, login synthesis, control routing through the arbiter,
 * round lifecycle over the room session, and room-scoped game fan-out.
 */
class RoomSessionTest {

    private final List<RoomSession> sessions = new ArrayList<RoomSession>();

    @AfterEach
    void tearDown() {
        for (RoomSession s : sessions) s.shutdown();
    }

    private static RoomConfig testConfig() {
        RoomConfig config = new RoomConfig();
        config.listenPort = 0;        // ephemeral
        config.lanAnnounce = false;   // no UDP broadcast noise in CI
        return config;
    }

    /** Line sink + convenience matcher */
    private static final class Client implements RoomEndpoint.LineListener {
        final BlockingQueue<String> lines = new LinkedBlockingQueue<String>();
        public void onLine(String line) { lines.add(line); }

        /** Drain until a line with this prefix arrives (or fail) */
        String await(String prefix) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 8000;
            while (System.currentTimeMillis() < deadline) {
                String line = lines.poll(200, TimeUnit.MILLISECONDS);
                if (line != null && line.startsWith(prefix)) return line;
            }
            fail("No line starting with '" + prefix.replace('\t', '|') + "' arrived");
            return null;
        }

        boolean noneStartingWith(String prefix, long waitMs) throws InterruptedException {
            long deadline = System.currentTimeMillis() + waitMs;
            while (System.currentTimeMillis() < deadline) {
                String line = lines.poll(50, TimeUnit.MILLISECONDS);
                if (line != null && line.startsWith(prefix)) return false;
            }
            return true;
        }
    }

    private RoomSession create(String name) throws Exception {
        RoomSession s = RoomSession.create(name, testConfig(), null, new LanRoomNet());
        sessions.add(s);
        return s;
    }

    private RoomSession join(RoomSession target, String name) throws Exception {
        RoomSession s = RoomSession.join("127.0.0.1", target.getListenPort(), name, testConfig(), null, new LanRoomNet());
        sessions.add(s);
        long deadline = System.currentTimeMillis() + 8000;
        while (s.getState() != RoomSession.State.READY && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        assertEquals(RoomSession.State.READY, s.getState(), "Join should reach READY");
        return s;
    }

    /** Attach a client and drive the standard login sequence */
    private Client login(RoomSession session, String name) throws Exception {
        Client client = new Client();
        session.setLineListener(client);
        session.clientReady();
        client.await("welcome\t");
        session.sendLine("login\t1.0\t" + NetUtil.urlEncode(name) + "\t\t\n");
        client.await("loginsuccess\t");
        return client;
    }

    private static String ruleDataLine() {
        CustomProperties prop = new CustomProperties();
        new RuleOptions().writeProperty(prop, 0);
        String compressed = NetUtil.compressString(prop.encode("RuleData"));
        Adler32 checksum = new Adler32();
        checksum.update(NetUtil.stringToBytes(compressed));
        return "ruledata\t" + checksum.getValue() + "\t" + compressed;
    }

    private static String roomCreateLine(String name, int maxPlayers) {
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = maxPlayers;
        template.strMode = "NET-VS-BATTLE";
        return "roomcreate\t" + NetUtil.urlEncode(name) + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE");
    }

    @Test
    void loginSynthesisFollowsServerOrder() throws Exception {
        RoomSession a = create("Alice");
        Client client = new Client();
        a.setLineListener(client);
        a.clientReady();

        client.await("welcome\t");
        a.sendLine("login\t1.0\t" + NetUtil.urlEncode("Alice") + "\t\t");

        String loginsuccess = client.await("loginsuccess\t");
        assertEquals("loginsuccess\t" + NetUtil.urlEncode("Alice") + "\t0", loginsuccess);
        String playerlist = client.await("playerlist\t");
        assertTrue(playerlist.startsWith("playerlist\t1\t"), "Roster includes self");
        client.await("roomlist\t");

        a.sendLine(ruleDataLine());
        client.await("ruledatasuccess");
    }

    @Test
    void joinerSeesRosterAndRoom() throws Exception {
        RoomSession a = create("Alice");
        Client aliceClient = login(a, "Alice");
        a.sendLine(roomCreateLine("Early Room", 4));
        aliceClient.await("roomcreatesuccess\t");

        RoomSession b = join(a, "Bob");
        assertFalse(b.isArbiter());
        Client bobClient = login(b, "Bob");

        // Bob's snapshot-driven login shows both players and the existing room
        String playerlist = bobClient.await("playerlist\t");
        assertTrue(playerlist.startsWith("playerlist\t2\t"), playerlist.substring(0, 20));
        String roomlist = bobClient.await("roomlist\t");
        assertTrue(roomlist.startsWith("roomlist\t1\t"), "Pre-existing room in the list");

        // Bob joins through the arbiter and lands in seat 1
        b.sendLine("roomjoin\t0\tfalse");
        assertEquals("roomjoinsuccess\t0\t1\t-1", bobClient.await("roomjoinsuccess\t"));
        aliceClient.await("playerenter\t1\t");
    }

    @Test
    void roundLifecycleSharesSeedAndOrdersDeadBeforeFinish() throws Exception {
        RoomSession a = create("Alice");
        Client aliceClient = login(a, "Alice");
        a.sendLine(roomCreateLine("Round", 2));
        aliceClient.await("roomcreatesuccess\t");

        RoomSession b = join(a, "Bob");
        Client bobClient = login(b, "Bob");
        b.sendLine("roomjoin\t0\tfalse");
        bobClient.await("roomjoinsuccess\t");

        a.sendLine("ready\ttrue");
        b.sendLine("ready\ttrue");

        String startA = aliceClient.await("start\t");
        String startB = bobClient.await("start\t");
        assertEquals(startA, startB, "Identical shared seed on every peer");

        b.sendLine("dead\t0");
        String deadB = bobClient.await("dead\t");
        assertTrue(deadB.startsWith("dead\t1\t"), deadB);
        String finishB = bobClient.await("finish\t");
        assertTrue(finishB.startsWith("finish\t0\t0\t"), "Alice wins: " + finishB);
        aliceClient.await("finish\t");
    }

    @Test
    void gameTrafficIsStampedAndRoomScoped() throws Exception {
        RoomSession a = create("Alice");
        Client aliceClient = login(a, "Alice");
        a.sendLine(roomCreateLine("Room", 2));
        aliceClient.await("roomcreatesuccess\t");

        RoomSession b = join(a, "Bob");
        Client bobClient = login(b, "Bob");
        b.sendLine("roomjoin\t0\tfalse");
        bobClient.await("roomjoinsuccess\t");

        RoomSession c = join(a, "Carol");   // stays in the lobby
        Client carolClient = login(c, "Carol");

        a.sendLine("game\tpiece\t1\t2\t3\t0\t18\t1\t0\tfalse");
        String game = bobClient.await("game\t");
        assertEquals("game\t0\t0\tpiece\t1\t2\t3\t0\t18\t1\t0\tfalse", game);
        assertTrue(carolClient.noneStartingWith("game\t", 500),
                "A lobby bystander must not receive room 0's game lines");
    }

    @Test
    void beaconAnnouncesOnlyOnceARoomExists() throws Exception {
        RoomSession a = create("Alice");
        Client client = login(a, "Alice");

        // No room yet: nothing to announce (the create-room form may be open)
        assertNull(a.getBeaconSnapshotForTest());

        a.sendLine(roomCreateLine("Beacon Room", 4));
        client.await("roomcreatesuccess\t");

        long deadline = System.currentTimeMillis() + 5000;
        while (a.getBeaconSnapshotForTest() == null && System.currentTimeMillis() < deadline) {
            Thread.sleep(20);
        }
        nullpomino.game.net.NetLanDiscovery.Announce beacon = a.getBeaconSnapshotForTest();
        assertNotNull(beacon);
        assertEquals("Beacon Room", beacon.roomName);
        assertEquals("NET-VS-BATTLE", beacon.mode);
        assertEquals(1, beacon.seated);
        assertEquals(4, beacon.maxPlayers);
        assertEquals(a.getSessionId(), beacon.sessionId);
        assertFalse(beacon.playing);
    }

    @Test
    void staleClientGetsDeniedGracefully() throws Exception {
        RoomSession a = create("Alice");
        try (java.net.Socket stale = new java.net.Socket("127.0.0.1", a.getListenPort())) {
            stale.getOutputStream().write(NetUtil.stringToBytes("login\t1.0\tOldTimer\t\t\n"));
            stale.getOutputStream().flush();
            stale.setSoTimeout(5000);
            byte[] buf = new byte[512];
            int len = stale.getInputStream().read(buf);
            assertTrue(len > 0);
            String reply = new String(buf, 0, len, java.nio.charset.StandardCharsets.UTF_8);
            assertTrue(reply.startsWith("room\tdeny\t"), reply);
        }
    }

    @Test
    void leaveNotifiesPeersAndClosesOnce() throws Exception {
        RoomSession a = create("Alice");
        Client aliceClient = login(a, "Alice");
        RoomSession b = join(a, "Bob");
        login(b, "Bob");

        final BlockingQueue<String> closedReasons = new LinkedBlockingQueue<String>();
        b.setClosedListener(reason -> closedReasons.add(reason));
        b.sendLine("disconnect");

        assertEquals("LEFT", closedReasons.poll(5, TimeUnit.SECONDS));
        assertFalse(b.isOpen());
        // The arbiter sees Bob leave (playerlogout broadcast reaches Alice's client)
        aliceClient.await("playerlogout\t");
    }
}
