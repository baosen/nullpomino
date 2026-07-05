// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import nullpomino.game.net.room.RoomEventSink;
import nullpomino.game.net.room.RoomTransport;

/**
 * Browser {@link RoomTransport} over WebRTC DataChannels: "listening"
 * subscribes this peer's signaling inbox on the MQTT channel, and
 * "dialing" runs a non-trickle offer/answer exchange against the target
 * peer's inbox, where the host string is the target's signaling clientId
 * (ports are meaningless and ignored).
 */
public class RtcRoomTransport implements RoomTransport {
	private final RoomEventSink sink;

	public RtcRoomTransport(RoomEventSink sink) {
		this.sink = sink;
	}

	@Override
	public int startListening(int configuredPort) {
		// TODO(webrtc): subscribe the signaling inbox
		return 0;
	}

	@Override
	public void dial(String host, int port, int timeoutMs, DialCallback callback) {
		// TODO(webrtc): offer/answer over the signaling inbox, then DataChannel open
		callback.onDialFailed("WebRTC transport not implemented yet");
	}

	@Override
	public int getListenPort() {
		return 0;
	}

	@Override
	public String getDisplayAddress() {
		// TODO(webrtc): the local signaling clientId
		return "web";
	}

	@Override
	public void shutdown() {
		// TODO(webrtc): unsubscribe the inbox, close all peer connections
	}
}
