// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.gui.net.NetLobbyFrame;
import nullpomino.gui.net.NetLobbyListener;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Full-stack three-peer integration: real RoomSessions over real sockets,
 * each fronted by the NetRoomPlayerClient seam and a NetLobbyFrame with its
 * own pump loop - the exact wiring the SDL client uses. Covers session
 * formation, multi-room isolation, a full rated round, mid-game arbiter
 * migration with a follow-up rematch, and queue promotion on leave.
 */
class RoomClientIntegrationTest {

    /** One peer: session + seam + lobby + pump thread + recorded events */
    private final class Peer {
        final RoomSession session;
        final NetLobbyFrame nl;
        final BlockingQueue<String> messages = new LinkedBlockingQueue<String>();
        volatile boolean pumping = true;

        Peer(RoomSession session, String name) {
            this.session = session;
            nl = new NetLobbyFrame();
            nl.init();
            nl.addListener(new NetLobbyListener() {
                public void netlobbyOnInit(NetLobbyFrame lobby) {}
                public void netlobbyOnLoginOK(NetLobbyFrame lobby, NetPlayerClient client) {}
                public void netlobbyOnRoomJoin(NetLobbyFrame lobby, NetPlayerClient client, NetRoomInfo roomInfo) {}
                public void netlobbyOnRoomLeave(NetLobbyFrame lobby, NetPlayerClient client) {}
                public void netlobbyOnDisconnect(NetLobbyFrame lobby, NetPlayerClient client, Throwable ex) {}
                public void netlobbyOnMessage(NetLobbyFrame lobby, NetPlayerClient client, String[] message) throws IOException {
                    messages.add(String.join("\t", message));
                }
                public void netlobbyOnExit(NetLobbyFrame lobby) {}
            });
            nl.connectToRoom(name, "", session);
            Thread pump = new Thread(() -> {
                while (pumping) {
                    nl.pump();
                    try { Thread.sleep(10); } catch (InterruptedException e) { return; }
                }
            }, "TestPump-" + name);
            pump.setDaemon(true);
            pump.start();
        }

        void send(String line) {
            nl.netPlayerClient.send(line + "\n");
        }

        String await(String prefix) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 12000;
            while (System.currentTimeMillis() < deadline) {
                String line = messages.poll(200, TimeUnit.MILLISECONDS);
                if (line != null && line.startsWith(prefix)) return line;
            }
            fail("No message starting with '" + prefix.replace('\t', '|') + "'");
            return null;
        }

        boolean silentOn(String prefix, long waitMs) throws InterruptedException {
            long deadline = System.currentTimeMillis() + waitMs;
            while (System.currentTimeMillis() < deadline) {
                String line = messages.poll(50, TimeUnit.MILLISECONDS);
                if (line != null && line.startsWith(prefix)) return false;
            }
            return true;
        }

        void awaitLobbyMode(int mode) throws InterruptedException {
            long deadline = System.currentTimeMillis() + 12000;
            while (nl.lobbyMode != mode && System.currentTimeMillis() < deadline) Thread.sleep(20);
            assertEquals(mode, nl.lobbyMode);
        }
    }

    private final List<Peer> peers = new ArrayList<Peer>();

    @AfterEach
    void tearDown() {
        for (Peer p : peers) {
            p.pumping = false;
            p.session.shutdown();
        }
    }

    private static RoomConfig testConfig() {
        RoomConfig config = new RoomConfig();
        config.listenPort = 0;
        config.lanAnnounce = false;
        return config;
    }

    private Peer createPeer(String name) throws Exception {
        Peer p = new Peer(RoomSession.create(name, testConfig(), null), name);
        peers.add(p);
        p.awaitLobbyMode(NetLobbyFrame.LOBBYMODE_LOBBY);
        return p;
    }

    private Peer joinPeer(Peer target, String name) throws Exception {
        Peer p = new Peer(RoomSession.join("127.0.0.1", target.session.getListenPort(),
                name, testConfig(), null), name);
        peers.add(p);
        p.awaitLobbyMode(NetLobbyFrame.LOBBYMODE_LOBBY);
        return p;
    }

    private static String roomCreateLine(String name, int maxPlayers, boolean rated) {
        NetRoomInfo template = new NetRoomInfo();
        template.maxPlayers = maxPlayers;
        template.strMode = "NET-VS-BATTLE";
        template.rated = rated;
        return "roomcreate\t" + NetUtil.urlEncode(name) + "\t"
                + NetUtil.urlEncode(template.exportString()) + "\t" + NetUtil.urlEncode("NET-VS-BATTLE");
    }

    @Test
    void threePeersFormASessionWithAgreeingRosters() throws Exception {
        Peer alice = createPeer("Alice");
        Peer bob = joinPeer(alice, "Bob");
        Peer carol = joinPeer(alice, "Carol");

        // Everyone ends up with all three players mirrored
        for (Peer p : new Peer[]{alice, bob, carol}) {
            long deadline = System.currentTimeMillis() + 12000;
            while (p.nl.netPlayerClient.getPlayerInfoList().size() < 3
                    && System.currentTimeMillis() < deadline) Thread.sleep(20);
            assertEquals(3, p.nl.netPlayerClient.getPlayerInfoList().size());
        }
        NetPlayerInfo carolSelf = carol.nl.netPlayerClient.getYourPlayerInfo();
        assertEquals(2, carolSelf.uid);
    }

    @Test
    void twoRoomsStayFullyIsolated() throws Exception {
        Peer alice = createPeer("Alice");
        Peer bob = joinPeer(alice, "Bob");
        Peer carol = joinPeer(alice, "Carol");

        alice.send(roomCreateLine("Room A", 2, false));
        alice.await("roomcreatesuccess\t");
        bob.send("roomjoin\t0\tfalse");
        bob.await("roomjoinsuccess\t0\t");

        carol.send(roomCreateLine("Room B", 2, false));
        carol.await("roomcreatesuccess\t");
        carol.messages.clear();

        // Room A plays a full round; Carol (room B) must observe none of it
        alice.send("ready\ttrue");
        bob.send("ready\ttrue");
        String startA = alice.await("start\t");
        String startB = bob.await("start\t");
        assertEquals(startA, startB, "Shared seed inside room A");

        alice.send("game\tpiece\t1\t2\t3\t0\t18\t1\t0\tfalse");
        bob.await("game\t0\t0\tpiece\t");
        bob.send("dead\t0");
        bob.await("finish\t");

        assertTrue(carol.silentOn("start\t", 300), "start must not leak across rooms");
        assertTrue(carol.silentOn("game\t", 200), "game traffic must not leak across rooms");
        assertTrue(carol.silentOn("dead\t", 200), "dead must not leak across rooms");
    }

    @Test
    void ratedRoundBroadcastsRatingsToTheRoom() throws Exception {
        Peer alice = createPeer("Alice");
        Peer bob = joinPeer(alice, "Bob");

        alice.send(roomCreateLine("Rated", 2, true));
        alice.await("roomcreatesuccess\t");
        bob.send("roomjoin\t0\tfalse");
        bob.await("roomjoinsuccess\t0\t");

        alice.send("ready\ttrue");
        bob.send("ready\ttrue");
        alice.await("start\t");
        bob.send("dead\t0");

        String rating = bob.await("rating\t");
        assertTrue(rating.startsWith("rating\t"), rating);
        bob.await("finish\t0\t0\t");
    }

    @Test
    void arbiterDeathMidGameMigratesAndAllowsARematch() throws Exception {
        Peer alice = createPeer("Alice");     // arbiter, stays in the lobby
        Peer bob = joinPeer(alice, "Bob");
        Peer carol = joinPeer(alice, "Carol");

        bob.send(roomCreateLine("Round", 2, false));
        bob.await("roomcreatesuccess\t");
        carol.send("roomjoin\t0\tfalse");
        carol.await("roomjoinsuccess\t0\t");
        bob.send("ready\ttrue");
        carol.send("ready\ttrue");
        bob.await("start\t");
        carol.await("start\t");

        alice.session.killAbruptly();
        alice.pumping = false;

        // Survivors stay in the room, see the logout, and finish the round
        bob.await("playerlogout\t");
        carol.await("playerlogout\t");
        assertEquals(NetLobbyFrame.LOBBYMODE_INROOM, bob.nl.lobbyMode);
        assertEquals(NetLobbyFrame.LOBBYMODE_INROOM, carol.nl.lobbyMode);

        carol.send("dead\t1");
        bob.await("finish\t1\t0\t");
        carol.await("finish\t1\t0\t");

        // Rematch under the new arbiter: everyone re-readies, a new round starts
        bob.send("ready\ttrue");
        carol.send("ready\ttrue");
        String start2B = bob.await("start\t");
        String start2C = carol.await("start\t");
        assertEquals(start2B, start2C, "The rematch seed is shared under the new arbiter");
    }

    @Test
    void renamePropagatesToEveryPeer() throws Exception {
        Peer alice = createPeer("Alice");
        Peer bob = joinPeer(alice, "Bob");

        bob.send("changename\t" + NetUtil.urlEncode("Bobby"));

        String renameAtAlice = alice.await("changename\t");
        assertTrue(renameAtAlice.endsWith("\t" + NetUtil.urlEncode("Bobby")), renameAtAlice);
        bob.await("changename\t");

        long deadline = System.currentTimeMillis() + 12000;
        NetPlayerInfo bobAtAlice = null;
        while (System.currentTimeMillis() < deadline) {
            bobAtAlice = alice.nl.netPlayerClient.getPlayerInfoByUID(1);
            if (bobAtAlice != null && "Bobby".equals(bobAtAlice.strName)) break;
            Thread.sleep(20);
        }
        assertNotNull(bobAtAlice);
        assertEquals("Bobby", bobAtAlice.strName, "The playerupdate carries the new name");
    }

    @Test
    void leavingASeatPromotesTheQueue() throws Exception {
        Peer alice = createPeer("Alice");
        Peer bob = joinPeer(alice, "Bob");
        Peer carol = joinPeer(alice, "Carol");

        alice.send(roomCreateLine("Two Seats", 2, false));
        alice.await("roomcreatesuccess\t");
        bob.send("roomjoin\t0\tfalse");
        bob.await("roomjoinsuccess\t0\t1\t");
        carol.send("roomjoin\t0\tfalse");
        carol.await("roomjoinsuccess\t0\t-1\t0");   // queued

        bob.send("roomjoin\t-1\tfalse");
        bob.await("roomjoinsuccess\t-1\t");

        // Carol is promoted into the freed seat
        String promo = carol.await("changestatus\tjoinseat\t2\t");
        assertTrue(promo.endsWith("\t1"), "Into freed seat 1: " + promo);
    }
}
