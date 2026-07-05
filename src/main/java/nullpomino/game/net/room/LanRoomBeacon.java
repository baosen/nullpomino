// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import nullpomino.game.net.NetLanDiscovery;

/**
 * Desktop {@link RoomBeacon}: UDP LAN broadcast via
 * {@link NetLanDiscovery.Announcer}.
 */
public class LanRoomBeacon implements RoomBeacon {
	private NetLanDiscovery.Announcer announcer;

	@Override
	public synchronized void start(Supplier supplier) {
		if(announcer != null) return;
		announcer = new NetLanDiscovery.Announcer(() -> {
			NetLanDiscovery.Announce announce = supplier.get();
			return (announce == null) ? null : NetLanDiscovery.encodeRoomAnnounce(announce);
		});
		announcer.start();
	}

	@Override
	public synchronized void stop() {
		if(announcer != null) {
			announcer.shutdown();
			announcer = null;
		}
	}
}
