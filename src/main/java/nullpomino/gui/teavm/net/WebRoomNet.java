// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import nullpomino.game.net.NetLanDiscovery;
import nullpomino.game.net.room.RoomBeacon;
import nullpomino.game.net.room.RoomDispatcher;
import nullpomino.game.net.room.RoomEventSink;
import nullpomino.game.net.room.RoomNet;
import nullpomino.game.net.room.RoomTransport;

/**
 * Browser {@link RoomNet}: WebRTC DataChannel mesh links, a trampoline
 * dispatcher on the JS event loop, and an MQTT room beacon.
 */
public class WebRoomNet implements RoomNet {
	@Override
	public RoomTransport createTransport(RoomEventSink sink) {
		return new RtcRoomTransport(sink);
	}

	@Override
	public RoomDispatcher createDispatcher() {
		return new WebRoomDispatcher();
	}

	@Override
	public RoomBeacon createBeacon() {
		// TODO(webrtc): announce over the MQTT lounge topics
		return new RoomBeacon() {
			@Override public void start(Supplier supplier) {}
			@Override public void stop() {}
		};
	}
}
