// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import nullpomino.game.net.NetLanDiscovery;
import nullpomino.game.net.room.RoomBeacon;
import nullpomino.game.net.web.WebSignaling;

import org.teavm.jso.browser.Window;

/**
 * Browser {@link RoomBeacon}: publishes the room announce to the
 * session's broker topic on the LAN beacon cadence, wrapped with this
 * peer's signaling clientId so joiners know whom to dial. A null
 * announce skips the cycle (arbiter-only gating, unchanged).
 */
public class MqttRoomBeacon implements RoomBeacon {
	private final WebNetHub hub;
	private int intervalId = -1;

	public MqttRoomBeacon() {
		this(WebNetHub.get());
	}

	MqttRoomBeacon(WebNetHub hub) {
		this.hub = hub;
	}

	@Override
	public void start(Supplier supplier) {
		if(intervalId != -1) return;
		intervalId = Window.setInterval(() -> {
			NetLanDiscovery.Announce announce = supplier.get();
			if(announce == null) return;
			hub.connectedChannel().publish(
				WebSignaling.topicRooms(hub.topicRoot, announce.sessionId),
				WebSignaling.wrapAnnounce(hub.clientId, NetLanDiscovery.encodeRoomAnnounce(announce)));
		}, NetLanDiscovery.ANNOUNCE_INTERVAL_MS);
	}

	@Override
	public void stop() {
		if(intervalId != -1) {
			Window.clearInterval(intervalId);
			intervalId = -1;
		}
	}
}
