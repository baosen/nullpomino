// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Desktop {@link LoungeService} over UDP LAN broadcast: a thin lifecycle
 * wrapper around {@link NetLanDiscovery}'s Listener (rooms/chat/presence
 * in) and Announcer (presence out).
 */
public class LanLoungeService implements LoungeService {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(LanLoungeService.class);

	private final Random rand = new Random();

	private NetLanDiscovery.Listener listener;
	private NetLanDiscovery.Announcer presenceAnnouncer;
	private volatile NetLanDiscovery.ChatConsumer chatConsumer;
	private volatile String presenceName = "";
	private volatile String presenceInstanceId = "";

	@Override
	public synchronized boolean open() {
		if(listener != null) return true;
		try {
			listener = new NetLanDiscovery.Listener();
			NetLanDiscovery.ChatConsumer consumer = chatConsumer;
			if(consumer != null) listener.setChatConsumer(consumer);
			listener.start();
		} catch (SocketException e) {
			log.info("LAN lounge listener could not bind", e);
			listener = null;
			return false;
		}

		presenceAnnouncer = new NetLanDiscovery.Announcer(() -> {
			String name = presenceName;
			String id = presenceInstanceId;
			return (name.length() == 0 || id.length() == 0)
				? null : NetLanDiscovery.encodePresence(name, id);
		});
		presenceAnnouncer.start();
		return true;
	}

	@Override
	public synchronized void close() {
		if(listener != null) {
			listener.shutdown();
			listener = null;
		}
		if(presenceAnnouncer != null) {
			presenceAnnouncer.shutdown();
			presenceAnnouncer = null;
		}
	}

	@Override
	public void setChatConsumer(NetLanDiscovery.ChatConsumer consumer) {
		chatConsumer = consumer;
		synchronized(this) {
			if(listener != null) listener.setChatConsumer(consumer);
		}
	}

	@Override
	public void sendChat(String playerName, String message) {
		String msgId = Long.toHexString(rand.nextLong());
		// Pre-register the id so the looped-back broadcast copy dedupes away
		synchronized(this) {
			if(listener != null) listener.markSeen(msgId);
		}
		NetLanDiscovery.broadcastPacket(NetLanDiscovery.encodeChat(playerName, msgId, message));
	}

	@Override
	public void setPresence(String playerName, String instanceId) {
		presenceName = playerName;
		presenceInstanceId = instanceId;
	}

	@Override
	public synchronized List<NetLanDiscovery.Announce> snapshotRooms() {
		if(listener == null) return new ArrayList<NetLanDiscovery.Announce>();
		List<NetLanDiscovery.Announce> rooms = new ArrayList<NetLanDiscovery.Announce>();
		for(NetLanDiscovery.Announce a: listener.snapshot()) {
			if(a.room) rooms.add(a);
		}
		return NetLanDiscovery.dedupeBySession(rooms);
	}

	@Override
	public synchronized List<NetLanDiscovery.Presence> snapshotPresence() {
		if(listener == null) return new ArrayList<NetLanDiscovery.Presence>();
		return listener.snapshotPresence();
	}
}
