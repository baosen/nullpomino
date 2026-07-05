// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.gui.teavm.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import nullpomino.game.net.LoungeService;
import nullpomino.game.net.NetLanDiscovery;
import nullpomino.game.net.web.WebSignaling;

import org.teavm.jso.browser.Window;

/**
 * Browser {@link LoungeService} over the bonded MQTT channel: room
 * announces, lounge chat and presence ride broker topics with the same
 * payload codec, beacon cadence and TTL semantics as the UDP LAN lounge
 * ({@link NetLanDiscovery}). Single JS thread, so plain HashMaps.
 */
public class MqttLoungeService implements LoungeService {
	/** Remember this many chat msgIds for duplicate suppression */
	private static final int CHAT_SEEN_CAP = 256;

	private final WebNetHub hub;

	private boolean open = false;
	private MqttBondedChannel.MessageHandler handler;
	private NetLanDiscovery.ChatConsumer chatConsumer;

	/** Discovered rooms by hostPort (clientId:1), TTL-pruned on snapshot */
	private final Map<String, NetLanDiscovery.Announce> rooms = new HashMap<String, NetLanDiscovery.Announce>();

	/** Lounge visitors by instanceId, TTL-pruned on snapshot */
	private final Map<String, NetLanDiscovery.Presence> presence = new HashMap<String, NetLanDiscovery.Presence>();

	/** msgIds already delivered (bonded brokers duplicate every message) */
	private final LinkedHashSet<String> chatSeen = new LinkedHashSet<String>();

	/** Presence beacon while open */
	private String presenceName = "";
	private String presenceInstanceId = "";
	private int presenceIntervalId = -1;

	public MqttLoungeService() {
		this(WebNetHub.get());
	}

	MqttLoungeService(WebNetHub hub) {
		this.hub = hub;
	}

	@Override
	public boolean open() {
		if(open) return true;
		open = true;

		handler = (topic, payload) -> {
			if(open) onMessage(topic, payload);
		};
		hub.connectedChannel().addHandler(handler);
		hub.channel.subscribe(WebSignaling.loungeFilter(hub.topicRoot));

		presenceIntervalId = Window.setInterval(() -> {
			String name = presenceName;
			if((name.length() == 0) || (presenceInstanceId.length() == 0)) return;
			hub.channel.publish(WebSignaling.topicPresence(hub.topicRoot),
				NetLanDiscovery.encodePresence(name, presenceInstanceId));
		}, NetLanDiscovery.ANNOUNCE_INTERVAL_MS);
		return true;
	}

	@Override
	public void close() {
		if(!open) return;
		open = false;
		if(handler != null) {
			hub.channel.removeHandler(handler);
			handler = null;
		}
		if(presenceIntervalId != -1) {
			Window.clearInterval(presenceIntervalId);
			presenceIntervalId = -1;
		}
		rooms.clear();
		presence.clear();
	}

	// ---------------------------------------------------------------- inbound

	private void onMessage(String topic, byte[] payload) {
		if(topic.startsWith(hub.topicRoot + "/lounge/rooms/")) {
			WebSignaling.WrappedAnnounce wrapped = WebSignaling.unwrapAnnounce(payload);
			if(wrapped == null) return;
			NetLanDiscovery.Announce announce = NetLanDiscovery.decodeAnnounce(
				wrapped.packet, wrapped.packet.length, wrapped.arbiterClientId);
			if((announce != null) && announce.room) {
				rooms.put(announce.hostPort(), announce);
			}
		} else if(topic.equals(WebSignaling.topicChat(hub.topicRoot))) {
			NetLanDiscovery.ChatLine chat = NetLanDiscovery.decodeChat(payload, payload.length);
			if((chat != null) && (chatConsumer != null) && markSeen(chat.msgId)) {
				chatConsumer.onChat(chat.playerName, chat.message);
			}
		} else if(topic.equals(WebSignaling.topicPresence(hub.topicRoot))) {
			NetLanDiscovery.Presence visitor = NetLanDiscovery.decodePresence(payload, payload.length);
			if(visitor != null) presence.put(visitor.instanceId, visitor);
		}
	}

	/** @return true if the id was new (bonded brokers + local echo dedupe) */
	private boolean markSeen(String msgId) {
		if(!chatSeen.add(msgId)) return false;
		while(chatSeen.size() > CHAT_SEEN_CAP) {
			chatSeen.remove(chatSeen.iterator().next());
		}
		return true;
	}

	// ---------------------------------------------------------------- outbound

	@Override
	public void setChatConsumer(NetLanDiscovery.ChatConsumer consumer) {
		chatConsumer = consumer;
	}

	@Override
	public void sendChat(String playerName, String message) {
		String msgId = WebSignaling.generateId(hub.rand);
		// Pre-register the id so our own copies (one per bonded broker)
		// dedupe away after the UI's local echo
		markSeen(msgId);
		hub.connectedChannel().publish(WebSignaling.topicChat(hub.topicRoot),
			NetLanDiscovery.encodeChat(playerName, msgId, message));
	}

	@Override
	public void setPresence(String playerName, String instanceId) {
		presenceName = playerName;
		presenceInstanceId = instanceId;
	}

	// ---------------------------------------------------------------- snapshots

	@Override
	public List<NetLanDiscovery.Announce> snapshotRooms() {
		long now = System.currentTimeMillis();
		List<NetLanDiscovery.Announce> result = new ArrayList<NetLanDiscovery.Announce>();
		List<String> expired = new ArrayList<String>();
		for(Map.Entry<String, NetLanDiscovery.Announce> e: rooms.entrySet()) {
			if(now - e.getValue().lastSeen <= NetLanDiscovery.ENTRY_TTL_MS) {
				result.add(e.getValue());
			} else {
				expired.add(e.getKey());
			}
		}
		for(String key: expired) rooms.remove(key);

		Collections.sort(result, new Comparator<NetLanDiscovery.Announce>() {
			public int compare(NetLanDiscovery.Announce a, NetLanDiscovery.Announce b) {
				return a.hostPort().compareTo(b.hostPort());
			}
		});
		return NetLanDiscovery.dedupeBySession(result);
	}

	@Override
	public List<NetLanDiscovery.Presence> snapshotPresence() {
		long now = System.currentTimeMillis();
		List<NetLanDiscovery.Presence> result = new ArrayList<NetLanDiscovery.Presence>();
		List<String> expired = new ArrayList<String>();
		for(Map.Entry<String, NetLanDiscovery.Presence> e: presence.entrySet()) {
			if(now - e.getValue().lastSeen <= NetLanDiscovery.ENTRY_TTL_MS) {
				result.add(e.getValue());
			} else {
				expired.add(e.getKey());
			}
		}
		for(String key: expired) presence.remove(key);

		Collections.sort(result, new Comparator<NetLanDiscovery.Presence>() {
			public int compare(NetLanDiscovery.Presence a, NetLanDiscovery.Presence b) {
				return a.playerName.compareTo(b.playerName);
			}
		});
		return result;
	}
}
