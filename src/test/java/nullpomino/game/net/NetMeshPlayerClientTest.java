// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import nullpomino.game.net.mesh.MeshEndpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link NetMeshPlayerClient} seam over a fake
 * {@link MeshEndpoint}: login self-drive, mirror population, send routing,
 * connection semantics, and one-shot disconnect fanout.
 */
class NetMeshPlayerClientTest {

    /** Records sendLine calls and lets tests emit synthesized lines */
    private static final class FakeMeshEndpoint implements MeshEndpoint {
        final List<String> sent = new ArrayList<String>();
        LineListener lineListener;
        ClosedListener closedListener;
        boolean open = true;
        boolean clientReadyCalled = false;

        public void setLineListener(LineListener listener) { lineListener = listener; }
        public void setClosedListener(ClosedListener listener) { closedListener = listener; }
        public void sendLine(String line) { sent.add(line); }
        public void clientReady() { clientReadyCalled = true; }
        public boolean isOpen() { return open; }
        public int getListenPort() { return 9202; }
        public String getDisplayHost() { return "192.168.1.10"; }
        public String getSessionId() { return "deadbeef"; }
        public void shutdown() { open = false; }

        void emit(String line) { lineListener.onLine(line); }
        void close(String reason) { closedListener.onClosed(reason); }
    }

    private FakeMeshEndpoint mesh;
    private NetMeshPlayerClient client;

    @BeforeEach
    void setUp() {
        mesh = new FakeMeshEndpoint();
        client = new NetMeshPlayerClient(mesh, "Alice", "TeamA");
    }

    @Test
    void connectAttachesAndWelcomeSelfDrivesLogin() {
        client.connect();
        assertTrue(mesh.clientReadyCalled);
        assertTrue(client.isConnected());

        mesh.emit("welcome\t1.0\t1\t0\t0\t1.0.0\t5000\tfalse");

        assertEquals(1, mesh.sent.size());
        String login = mesh.sent.get(0);
        assertTrue(login.startsWith("login\t"), login);
        assertTrue(login.contains("\t" + NetUtil.urlEncode("Alice") + "\t"), login);
        assertTrue(login.contains("\t" + NetUtil.urlEncode("TeamA") + "\t"), login);
    }

    @Test
    void loginSuccessAndListsPopulateTheInheritedMirrors() {
        client.connect();
        mesh.emit("loginsuccess\t" + NetUtil.urlEncode("Alice") + "\t0");
        assertEquals(0, client.getPlayerUID());

        NetPlayerInfo alice = new NetPlayerInfo();
        alice.uid = 0;
        alice.strName = "Alice";
        NetPlayerInfo bob = new NetPlayerInfo();
        bob.uid = 1;
        bob.strName = "Bob";
        mesh.emit("playerlist\t2\t" + alice.exportString() + "\t" + bob.exportString());

        assertEquals(2, client.getPlayerInfoList().size());
        assertEquals("Alice", client.getYourPlayerInfo().strName);

        NetRoomInfo room = new NetRoomInfo();
        room.roomID = 0;
        room.strName = "Room";
        mesh.emit("roomlist\t1\t" + room.exportString());
        assertEquals(1, client.getRoomInfoList().size());
    }

    @Test
    void sendRoutesToMeshAndFailsAfterClose() {
        client.connect();
        assertTrue(client.send("ready\ttrue\n"));
        assertEquals("ready\ttrue\n", mesh.sent.get(0));

        mesh.close("LEFT");
        assertFalse(client.send("ready\tfalse\n"));
        assertEquals(1, mesh.sent.size());
    }

    @Test
    void isConnectedTracksBothFlagAndMeshState() {
        assertFalse(client.isConnected());
        client.connect();
        assertTrue(client.isConnected());
        mesh.open = false;
        assertFalse(client.isConnected());
    }

    @Test
    void disconnectFanoutFiresExactlyOnce() {
        final AtomicInteger disconnects = new AtomicInteger();
        client.addListener(new NetMessageListener() {
            public void netOnMessage(NetBaseClient c, String[] message) {}
            public void netOnDisconnect(NetBaseClient c, Throwable reason) { disconnects.incrementAndGet(); }
        });
        client.connect();

        mesh.close("ARBITER_LOST");
        mesh.close("second close");

        assertEquals(1, disconnects.get());
        assertFalse(client.isConnected());
    }

    @Test
    void threadEntryPointsAreInert() {
        client.connect();
        ((Runnable) client).run();  // deliberately synchronous: must not open sockets or throw
        client.startPingTask(123);  // mesh owns keepalive: no timer, no ping lines
        assertTrue(mesh.sent.isEmpty());
    }
}
