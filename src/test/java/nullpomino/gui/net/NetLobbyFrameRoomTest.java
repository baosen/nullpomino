// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.net;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.game.net.room.RoomEndpoint;

import org.junit.jupiter.api.Test;

/**
 * The room ordering acceptance spec: NetLobbyFrame over the
 * NetRoomPlayerClient seam must reach LOBBY on the standard login sequence
 * and INROOM on the standard join sequence - and must NOT reach INROOM when
 * the sequence is delivered out of order (this pins the ordering contract
 * the room core has to honor).
 */
class NetLobbyFrameRoomTest {

    private static final class FakeRoomEndpoint implements RoomEndpoint {
        final List<String> sent = new ArrayList<String>();
        LineListener lineListener;
        ClosedListener closedListener;
        boolean open = true;

        public void setLineListener(LineListener listener) { lineListener = listener; }
        public void setClosedListener(ClosedListener listener) { closedListener = listener; }
        public void sendLine(String line) { sent.add(line.endsWith("\n") ? line.substring(0, line.length() - 1) : line); }
        public void clientReady() {}
        public boolean isOpen() { return open; }
        public int getListenPort() { return 9202; }
        public String getDisplayHost() { return "192.168.1.10"; }
        public String getSessionId() { return "deadbeef"; }
        public void shutdown() { open = false; }

        void emit(String line) { lineListener.onLine(line); }
        boolean sentStartingWith(String prefix) {
            for (String s : sent) if (s.startsWith(prefix)) return true;
            return false;
        }
    }

    private static final class RecordingListener implements NetLobbyListener {
        boolean loginOK, roomJoin, roomLeave, disconnected;
        NetRoomInfo joinedRoom;

        public void netlobbyOnInit(NetLobbyFrame lobby) {}
        public void netlobbyOnLoginOK(NetLobbyFrame lobby, NetPlayerClient client) { loginOK = true; }
        public void netlobbyOnRoomJoin(NetLobbyFrame lobby, NetPlayerClient client, NetRoomInfo roomInfo) {
            roomJoin = true;
            joinedRoom = roomInfo;
        }
        public void netlobbyOnRoomLeave(NetLobbyFrame lobby, NetPlayerClient client) { roomLeave = true; }
        public void netlobbyOnDisconnect(NetLobbyFrame lobby, NetPlayerClient client, Throwable ex) { disconnected = true; }
        public void netlobbyOnMessage(NetLobbyFrame lobby, NetPlayerClient client, String[] message) throws IOException {}
        public void netlobbyOnExit(NetLobbyFrame lobby) {}
    }

    private NetLobbyFrame nl;
    private FakeRoomEndpoint endpoint;
    private RecordingListener events;

    private void connect() {
        nl = new NetLobbyFrame();
        nl.init();
        events = new RecordingListener();
        nl.addListener(events);
        endpoint = new FakeRoomEndpoint();
        nl.connectToRoom("Alice", "", endpoint);
    }

    private static String selfBlob(int roomID, int seatID) {
        NetPlayerInfo self = new NetPlayerInfo();
        self.uid = 0;
        self.strName = "Alice";
        self.roomID = roomID;
        self.seatID = seatID;
        self.connected = true;
        return self.exportString();
    }

    private static String roomBlob() {
        NetRoomInfo room = new NetRoomInfo();
        room.roomID = 0;
        room.strName = "Round";
        room.maxPlayers = 2;
        room.strMode = "NET-VS-BATTLE";
        return room.exportString();
    }

    /** The standard synthesized login sequence up to LOBBY */
    private void driveLogin() {
        endpoint.emit("welcome\t1.0\t1\t0\t0\t1.0.0\t5000\tfalse");
        endpoint.emit("loginsuccess\t" + NetUtil.urlEncode("Alice") + "\t0");
        endpoint.emit("playerlist\t1\t" + selfBlob(-1, -1));
        endpoint.emit("roomlist\t0");
        nl.pump();
        endpoint.emit("ruledatasuccess");
        nl.pump();
    }

    @Test
    void loginSequenceReachesLobby() {
        connect();
        assertTrue(nl.isRoomSession());

        driveLogin();

        assertEquals(NetLobbyFrame.LOBBYMODE_LOBBY, nl.lobbyMode);
        assertTrue(events.loginOK, "netlobbyOnLoginOK must fire on ruledatasuccess");
        assertTrue(endpoint.sentStartingWith("login\t"), "The seam self-drives the login line");
        assertTrue(endpoint.sentStartingWith("ruledata\t"), "loginsuccess triggers the rule upload");
    }

    @Test
    void joinSequenceInServerOrderReachesInRoom() {
        connect();
        driveLogin();

        // roomupdate -> playerupdate(self) -> roomjoinsuccess LAST
        endpoint.emit("roomcreate\t" + roomBlob());
        endpoint.emit("playerupdate\t" + selfBlob(0, 0));
        endpoint.emit("roomjoinsuccess\t0\t0\t-1");
        nl.pump();

        assertEquals(NetLobbyFrame.LOBBYMODE_INROOM, nl.lobbyMode);
        assertTrue(events.roomJoin);
        assertEquals("NET-VS-BATTLE", events.joinedRoom.strMode, "The mode class comes from the room blob");
    }

    @Test
    void roomJoinSuccessBeforeRoomInfoDoesNotEnterRoom() {
        connect();
        driveLogin();

        // Out of order: roomjoinsuccess arrives before the room exists in the mirror.
        // This is exactly what the room core must never do.
        endpoint.emit("roomjoinsuccess\t0\t0\t-1");
        nl.pump();

        assertNotEquals(NetLobbyFrame.LOBBYMODE_INROOM, nl.lobbyMode);
        assertFalse(events.roomJoin);
    }

    @Test
    void returnToLobbyFiresRoomLeave() {
        connect();
        driveLogin();
        endpoint.emit("roomcreate\t" + roomBlob());
        endpoint.emit("playerupdate\t" + selfBlob(0, 0));
        endpoint.emit("roomjoinsuccess\t0\t0\t-1");
        nl.pump();

        endpoint.emit("playerupdate\t" + selfBlob(-1, -1));
        endpoint.emit("roomjoinsuccess\t-1\t-1\t-1");
        nl.pump();

        assertEquals(NetLobbyFrame.LOBBYMODE_LOBBY, nl.lobbyMode);
        assertTrue(events.roomLeave);
    }

    @Test
    void meshCloseDispatchesDisconnect() {
        connect();
        driveLogin();

        endpoint.closedListener.onClosed("ARBITER_LOST");
        nl.pump();

        assertTrue(events.disconnected);
        assertEquals(NetLobbyFrame.LOBBYMODE_DISCONNECTED, nl.lobbyMode);
    }

    @Test
    void shutdownIsSeamSafe() {
        connect();
        driveLogin();

        nl.shutdown();   // must not throw on the never-started seam thread

        assertTrue(endpoint.sentStartingWith("disconnect"), "Graceful leave goes through the seam");
    }
}
