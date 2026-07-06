// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import nullpomino.game.net.NetLanDiscovery;

import org.junit.jupiter.api.Test;

/**
 * Branch-gap tests for {@link LanRoomBeacon}: idempotent start/stop and the
 * null-announce "skip this cycle" gate of the payload supplier.
 */
class LanRoomBeaconBranchGapTest {

    @Test
    void stopBeforeStartIsANoOp() {
        LanRoomBeacon beacon = new LanRoomBeacon();
        beacon.stop(); // never started - nothing to shut down
    }

    @Test
    void startPollsSupplierAndSecondStartIsIgnored() throws Exception {
        // One beacon whose supplier gates the cycle off (null announce)...
        LanRoomBeacon gatedBeacon = new LanRoomBeacon();
        final CountDownLatch gatedPolled = new CountDownLatch(1);
        gatedBeacon.start(() -> {
            gatedPolled.countDown();
            return null;
        });

        // ...and one that actually announces a room
        LanRoomBeacon liveBeacon = new LanRoomBeacon();
        final CountDownLatch livePolled = new CountDownLatch(1);
        final NetLanDiscovery.Announce announce = new NetLanDiscovery.Announce(
                "127.0.0.1", 9202, "Gap", "7.5", "lrb-sess-1", "Lobby", 1);
        liveBeacon.start(() -> {
            livePolled.countDown();
            return announce;
        });

        try {
            assertTrue(gatedPolled.await(10, TimeUnit.SECONDS),
                    "Supplier should be polled even when it gates the announce off");
            assertTrue(livePolled.await(10, TimeUnit.SECONDS),
                    "Supplier should be polled for a real announce");

            // Second start while running: ignored, replacement supplier unused
            liveBeacon.start(() -> fail("Second start(supplier) must be a no-op"));
        } finally {
            gatedBeacon.stop();
            liveBeacon.stop();
        }
        liveBeacon.stop(); // stop after stop - the announcer is already gone
    }
}
