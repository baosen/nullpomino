// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Arbiter migration and failure handling: lowest-uid survivor promotes and
 * the game continues, a mid-game arbiter death produces its own dead line,
 * and split-mesh members get arbitrated away.
 */
class MeshMigrationTest {

    private final List<MeshSession> sessions = new ArrayList<MeshSession>();

    @AfterEach
    void tearDown() {
        for (MeshSession s : sessions) s.shutdown();
    }

    private static MeshConfig testConfig() {
        MeshConfig config = new MeshConfig();
        config.listenPort = 0;
        config.lanAnnounce = false;
        return config;
    }

    private static final class Client implements MeshEndpoint.LineListener {
        final BlockingQueue<String> lines = new LinkedBlockingQueue<String>();
        public void onLine(String line) { lines.add(line); }

        String await(String prefix) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 12000;
            while (System.currentTimeMillis() < deadline) {
                String line = lines.poll(200, TimeUnit.MILLISECONDS);
                if (line != null && line.startsWith(prefix)) return line;
            }
            fail("No line starting with '" + prefix.replace('\t', '|') + "' arrived");
            return null;
        }
    }

    private MeshSession create(String name) throws Exception {
        MeshSession s = MeshSession.create(name, testConfig(), null);
        sessions.add(s);
        return s;
    }

    private MeshSession join(MeshSession target, String name) throws Exception {
        MeshSession s = MeshSession.join("127.0.0.1", target.getListenPort(), name, testConfig(), null);
        sessions.add(s);
        awaitState(s, MeshSession.State.READY);
        return s;
    }

    private static void awaitState(MeshSession s, MeshSession.State expected) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 12000;
        while (s.getState() != expected && System.currentTimeMillis() < deadline) Thread.sleep(20);
        assertEquals(expected, s.getState());
    }

    private static void awaitArbiter(MeshSession s) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 12000;
        while (!s.isArbiter() && System.currentTimeMillis() < deadline) Thread.sleep(20);
        assertTrue(s.isArbiter(), "Expected promotion to arbiter");
    }

    private Client login(MeshSession session, String name) throws Exception {
        Client client = new Client();
        session.setLineListener(client);
        session.clientReady();
        client.await("welcome\t");
        session.sendLine("login\t1.0\t" + NetUtil.urlEncode(name) + "\t\t");
        client.await("loginsuccess\t");
        return client;
    }

    private static String roomCreateLine(String name, int maxPlayers) {
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = maxPlayers;
        template.strMode = "NET-VS-BATTLE";
        return "roomcreate\t" + NetUtil.urlEncode(name) + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE");
    }

    @Test
    void survivorPromotesAndTheRoundContinues() throws Exception {
        MeshSession a = create("Alice");
        login(a, "Alice");
        MeshSession b = join(a, "Bob");
        Client bobClient = login(b, "Bob");
        MeshSession c = join(a, "Carol");
        Client carolClient = login(c, "Carol");

        // Bob hosts the room; Alice (the arbiter) stays in the lobby
        b.sendLine(roomCreateLine("Round", 2));
        bobClient.await("roomcreatesuccess\t");
        c.sendLine("roomjoin\t0\tfalse");
        carolClient.await("roomjoinsuccess\t");
        b.sendLine("ready\ttrue");
        c.sendLine("ready\ttrue");
        String startB = bobClient.await("start\t");
        String startC = carolClient.await("start\t");
        assertEquals(startB, startC);

        // The arbiter dies mid-round without a goodbye
        a.killAbruptly();
        awaitArbiter(b);   // Bob (uid 1) is the lowest survivor

        // Both survivors observe Alice's logout
        bobClient.await("playerlogout\t");
        carolClient.await("playerlogout\t");

        // The round continues under the new arbiter: Carol dies, Bob wins
        c.sendLine("dead\t1");
        String dead = carolClient.await("dead\t2\t");
        assertTrue(dead.contains("\t1\t" + NetUtil.urlEncode("Bob")), "KO credited to Bob: " + dead);
        String finish = carolClient.await("finish\t");
        assertTrue(finish.startsWith("finish\t1\t0\t" + NetUtil.urlEncode("Bob")), finish);
        bobClient.await("finish\t");
    }

    @Test
    void arbiterDeathMidGameCountsAsItsDeath() throws Exception {
        MeshSession a = create("Alice");
        Client aliceClient = login(a, "Alice");
        a.sendLine(roomCreateLine("Duel", 2));
        aliceClient.await("roomcreatesuccess\t");

        MeshSession b = join(a, "Bob");
        Client bobClient = login(b, "Bob");
        b.sendLine("roomjoin\t0\tfalse");
        bobClient.await("roomjoinsuccess\t");
        a.sendLine("ready\ttrue");
        b.sendLine("ready\ttrue");
        bobClient.await("start\t");

        a.killAbruptly();
        awaitArbiter(b);

        // The departed arbiter was playing: the successor emits its death and Bob wins
        String dead = bobClient.await("dead\t0\t");
        assertTrue(dead.contains("\t0\t2\t-1\t"), "Alice seat 0, place 2, no KO credit: " + dead);
        String finish = bobClient.await("finish\t");
        assertTrue(finish.startsWith("finish\t1\t1\t" + NetUtil.urlEncode("Bob")), finish);
    }

    @Test
    void splitMeshMemberGetsKicked() throws Exception {
        MeshSession a = create("Alice");
        Client aliceClient = login(a, "Alice");
        MeshSession b = join(a, "Bob");
        login(b, "Bob");
        MeshSession c = join(a, "Carol");
        login(c, "Carol");

        final BlockingQueue<String> carolClosed = new LinkedBlockingQueue<String>();
        c.setClosedListener(reason -> carolClosed.add(reason));

        // Only the Bob-Carol link dies; both still reach the arbiter
        b.severLinkForTest(2);

        // Tie between reports: the higher uid (Carol) is kicked
        assertEquals("KICKED", carolClosed.poll(12, TimeUnit.SECONDS));
        aliceClient.await("playerlogout\t");
        assertEquals(MeshSession.State.READY, b.getState());
        assertTrue(b.isOpen());
    }
}
