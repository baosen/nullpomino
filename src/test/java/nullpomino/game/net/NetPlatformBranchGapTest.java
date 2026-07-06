// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;

import nullpomino.game.net.room.RoomBeacon;
import nullpomino.game.net.room.RoomDispatcher;
import nullpomino.game.net.room.RoomEventSink;
import nullpomino.game.net.room.RoomNet;
import nullpomino.game.net.room.RoomTransport;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetPlatform}: the accessors throw before install and
 * return the installed instances afterwards. The static holder is reset
 * to its pristine (uninstalled) state before and after, so the test is
 * order-independent inside the big suite.
 */
class NetPlatformBranchGapTest {

    private static final class StubRoomNet implements RoomNet {
        public RoomTransport createTransport(RoomEventSink sink) { return null; }
        public RoomDispatcher createDispatcher() { return null; }
        public RoomBeacon createBeacon() { return null; }
    }

    private static final class StubLounge implements LoungeService {
        public boolean open() { return false; }
        public void close() {}
        public void setChatConsumer(NetLanDiscovery.ChatConsumer consumer) {}
        public void sendChat(String playerName, String message) {}
        public void setPresence(String playerName, String instanceId) {}
        public List<NetLanDiscovery.Announce> snapshotRooms() { return Collections.emptyList(); }
        public List<NetLanDiscovery.Presence> snapshotPresence() { return Collections.emptyList(); }
    }

    @Test
    void accessorsThrowBeforeInstallAndReturnInstancesAfter() {
        NetPlatform.install(null, null);
        try {
            assertThrows(IllegalStateException.class, NetPlatform::roomNet);
            assertThrows(IllegalStateException.class, NetPlatform::lounge);

            RoomNet net = new StubRoomNet();
            LoungeService lounge = new StubLounge();
            NetPlatform.install(net, lounge);
            assertSame(net, NetPlatform.roomNet());
            assertSame(lounge, NetPlatform.lounge());
        } finally {
            NetPlatform.install(null, null);
        }
    }
}
