// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import java.util.ArrayList;
import java.util.List;

import nullpomino.game.net.LoungeService;
import nullpomino.game.net.NetLanDiscovery;

/**
 * Browser {@link LoungeService} over a public MQTT-over-WebSocket
 * broker: room announces, lounge chat and presence ride broker topics
 * with the same payloads, cadence and TTL as the UDP LAN lounge.
 */
public class MqttLoungeService implements LoungeService {
	@Override
	public boolean open() {
		// TODO(webrtc): connect the bonded MQTT channel, subscribe lounge topics
		return false;
	}

	@Override
	public void close() {
	}

	@Override
	public void setChatConsumer(NetLanDiscovery.ChatConsumer consumer) {
	}

	@Override
	public void sendChat(String playerName, String message) {
	}

	@Override
	public void setPresence(String playerName, String instanceId) {
	}

	@Override
	public List<NetLanDiscovery.Announce> snapshotRooms() {
		return new ArrayList<NetLanDiscovery.Announce>();
	}

	@Override
	public List<NetLanDiscovery.Presence> snapshotPresence() {
		return new ArrayList<NetLanDiscovery.Presence>();
	}
}
