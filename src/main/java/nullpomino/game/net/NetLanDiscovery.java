// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import nullpomino.game.play.GameManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LAN discovery for player-hosted netplay servers.
 * A hosting client broadcasts a small UDP announce packet at a fixed interval;
 * clients on the server-select screen listen for these packets and show the
 * discovered hosts alongside the saved server list.
 *
 * <p>Packet format (tab-delimited, UTF-8):
 * {@code NullpoLAN\t[PROTOCOL_VERSION]\t[tcpPort]\t[urlEncode(playerName)]\t[versionMajor]}
 */
public class NetLanDiscovery {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(NetLanDiscovery.class);

	/** UDP port the announce packets are sent to */
	public static final int DISCOVERY_PORT = 9201;

	/** First field of every announce packet */
	public static final String MAGIC = "NullpoLAN";

	/** Announce packet format version (server announce) */
	public static final int PROTOCOL_VERSION = 1;

	/** Announce packet format version for mesh sessions (old clients reject it silently) */
	public static final int MESH_PROTOCOL_VERSION = 2;

	/** Type marker inside a v2 announce identifying a mesh session */
	public static final String MESH_TYPE = "M";

	/** Delay between announce broadcasts (ms) */
	public static final int ANNOUNCE_INTERVAL_MS = 1500;

	/** Discovered entries older than this are dropped (ms) */
	public static final int ENTRY_TTL_MS = 5000;

	private NetLanDiscovery() {}

	/**
	 * Encode an announce packet
	 * @param tcpPort Port the game server listens on
	 * @param playerName Name of the hosting player
	 * @return Packet payload
	 */
	public static byte[] encodeAnnounce(int tcpPort, String playerName) {
		return NetUtil.stringToBytes(
			MAGIC + "\t" + PROTOCOL_VERSION + "\t" + tcpPort + "\t" +
			NetUtil.urlEncode(playerName) + "\t" + GameManager.getVersionMajor());
	}

	/**
	 * Encode a v2 announce packet for a P2P mesh session
	 * @param tcpPort Port this peer's mesh transport listens on
	 * @param playerName Name of the announcing peer
	 * @param sessionId Session identifier shared by every peer of the session
	 * @param lobbyName Display name of the session
	 * @param players Current number of players in the session
	 * @return Packet payload
	 */
	public static byte[] encodeMeshAnnounce(int tcpPort, String playerName, String sessionId,
		String lobbyName, int players)
	{
		return NetUtil.stringToBytes(
			MAGIC + "\t" + MESH_PROTOCOL_VERSION + "\t" + tcpPort + "\t" +
			NetUtil.urlEncode(playerName) + "\t" + GameManager.getVersionMajor() + "\t" +
			MESH_TYPE + "\t" + sessionId + "\t" + NetUtil.urlEncode(lobbyName) + "\t" + players);
	}

	/**
	 * Decode an announce packet (v1 server announce or v2 mesh announce)
	 * @param data Packet payload
	 * @param len Payload length
	 * @param sourceAddr Address the packet was received from
	 * @return Decoded announce, or null if the packet is not a valid announce
	 */
	public static Announce decodeAnnounce(byte[] data, int len, String sourceAddr) {
		if((data == null) || (len <= 0) || (len > data.length)) return null;

		String[] parts = new String(data, 0, len, StandardCharsets.UTF_8).split("\t");
		if(parts.length < 5) return null;
		if(!MAGIC.equals(parts[0])) return null;

		try {
			int version = Integer.parseInt(parts[1]);
			int port = Integer.parseInt(parts[2]);
			if((port < 1) || (port > 65535)) return null;
			String name = NetUtil.urlDecode(parts[3]);

			if(version == PROTOCOL_VERSION) {
				return new Announce(sourceAddr, port, name, parts[4]);
			}
			if(version == MESH_PROTOCOL_VERSION) {
				if((parts.length < 9) || !MESH_TYPE.equals(parts[5]) || (parts[6].length() == 0)) return null;
				return new Announce(sourceAddr, port, name, parts[4],
					parts[6], NetUtil.urlDecode(parts[7]), Integer.parseInt(parts[8]));
			}
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Collapse mesh announces of the same session (every peer announces) to one entry each.
	 * The entry with the lexicographically lowest host:port wins, so the pick is stable
	 * across refreshes. Non-mesh announces pass through untouched.
	 * @param announces Announces (typically a listener snapshot, already hostPort-sorted)
	 * @return Deduplicated list, original order preserved
	 */
	public static List<Announce> dedupeBySession(List<Announce> announces) {
		List<Announce> result = new ArrayList<Announce>();
		Set<String> seenSessions = new LinkedHashSet<String>();

		for(Announce a: announces) {
			if(!a.mesh) {
				result.add(a);
			} else if(!seenSessions.contains(a.sessionId)) {
				Announce best = a;
				for(Announce b: announces) {
					if(b.mesh && b.sessionId.equals(a.sessionId)
						&& (b.hostPort().compareTo(best.hostPort()) < 0)) best = b;
				}
				result.add(best);
				seenSessions.add(a.sessionId);
			}
		}
		return result;
	}

	/**
	 * One discovered host. Immutable; the listener replaces the whole entry
	 * on every received announce.
	 */
	public static final class Announce {
		/** Address the announce came from */
		public final String address;

		/** Port the game server listens on */
		public final int port;

		/** Name of the hosting player */
		public final String playerName;

		/** Game version of the host */
		public final String version;

		/** Time the announce was received (System.currentTimeMillis()) */
		public final long lastSeen;

		/** true if this announce is a P2P mesh session (v2), false for a server (v1) */
		public final boolean mesh;

		/** Session identifier shared by all peers of a mesh session, "" for servers */
		public final String sessionId;

		/** Display name of the mesh session, "" for servers */
		public final String lobbyName;

		/** Number of players in the mesh session, 0 for servers */
		public final int players;

		public Announce(String address, int port, String playerName, String version) {
			this(address, port, playerName, version, "", "", 0);
			// v1 server announce - the mesh fields stay empty
		}

		public Announce(String address, int port, String playerName, String version,
			String sessionId, String lobbyName, int players)
		{
			this.address = address;
			this.port = port;
			this.playerName = playerName;
			this.version = version;
			this.lastSeen = System.currentTimeMillis();
			this.mesh = (sessionId != null) && (sessionId.length() > 0);
			this.sessionId = (sessionId == null) ? "" : sessionId;
			this.lobbyName = (lobbyName == null) ? "" : lobbyName;
			this.players = players;
		}

		/** @return "address:port" as accepted by the server-select connect logic */
		public String hostPort() {
			return address + ":" + port;
		}
	}

	/**
	 * Daemon thread that broadcasts announce packets for a hosted server
	 * until {@link #shutdown()} is called.
	 */
	public static final class Announcer extends Thread {
		private final int tcpPort;
		private final String playerName;
		private volatile boolean shutdownRequested = false;
		private volatile DatagramSocket socket;

		public Announcer(int tcpPort, String playerName) {
			super("LanAnnouncer");
			setDaemon(true);
			this.tcpPort = tcpPort;
			this.playerName = playerName;
		}

		@Override
		public void run() {
			try {
				socket = new DatagramSocket();
				socket.setBroadcast(true);
				byte[] data = encodeAnnounce(tcpPort, playerName);

				while(!shutdownRequested) {
					for(InetAddress target: broadcastTargets()) {
						try {
							socket.send(new DatagramPacket(data, data.length, target, DISCOVERY_PORT));
						} catch (IOException e) {
							log.debug("Failed to send announce to {}", target, e);
						}
					}
					Thread.sleep(ANNOUNCE_INTERVAL_MS);
				}
			} catch (InterruptedException e) {
				// shutdown() interrupts the sleep
			} catch (IOException e) {
				if(!shutdownRequested) log.warn("LAN announcer stopped by exception", e);
			} finally {
				DatagramSocket s = socket;
				if(s != null) s.close();
			}
		}

		/** Stop announcing. Safe to call from any thread. */
		public void shutdown() {
			shutdownRequested = true;
			DatagramSocket s = socket;
			if(s != null) s.close();
			interrupt();
		}

		/** @return Broadcast addresses of all usable interfaces, plus the limited broadcast address */
		private static Set<InetAddress> broadcastTargets() {
			Set<InetAddress> targets = new LinkedHashSet<InetAddress>();
			try {
				Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
				while(interfaces.hasMoreElements()) {
					NetworkInterface ni = interfaces.nextElement();
					if(!ni.isUp() || ni.isLoopback()) continue;
					for(InterfaceAddress ia: ni.getInterfaceAddresses()) {
						if(ia.getBroadcast() != null) targets.add(ia.getBroadcast());
					}
				}
			} catch (SocketException e) {
				log.debug("Failed to enumerate network interfaces", e);
			}
			try {
				targets.add(InetAddress.getByName("255.255.255.255"));
			} catch (IOException e) {
				log.debug("Failed to resolve limited broadcast address", e);
			}
			return targets;
		}
	}

	/**
	 * Daemon thread that collects announce packets. The UI thread reads
	 * discovered hosts via {@link #snapshot()}; entries expire after the TTL.
	 */
	public static final class Listener extends Thread {
		private final int ttlMs;
		private volatile boolean shutdownRequested = false;
		private final DatagramSocket socket;
		private final ConcurrentHashMap<String, Announce> entries = new ConcurrentHashMap<String, Announce>();

		public Listener() throws SocketException {
			this(DISCOVERY_PORT, ENTRY_TTL_MS);
		}

		/**
		 * @param udpPort UDP port to listen on (0 for ephemeral, used by tests)
		 * @param ttlMs Entry time-to-live in milliseconds
		 * @throws SocketException When the port can't be bound
		 */
		public Listener(int udpPort, int ttlMs) throws SocketException {
			super("LanListener");
			setDaemon(true);
			this.ttlMs = ttlMs;
			// SO_REUSEADDR before bind, so several clients on one machine can listen at once
			socket = new DatagramSocket(null);
			socket.setReuseAddress(true);
			socket.bind(new InetSocketAddress(udpPort));
			socket.setSoTimeout(500);
		}

		/** @return The UDP port this listener is bound to */
		public int getLocalPort() {
			return socket.getLocalPort();
		}

		@Override
		public void run() {
			byte[] buf = new byte[512];

			while(!shutdownRequested) {
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				try {
					socket.receive(packet);
				} catch (SocketTimeoutException e) {
					continue;
				} catch (IOException e) {
					if(!shutdownRequested) log.debug("LAN listener stopped by exception", e);
					break;
				}

				Announce announce = decodeAnnounce(packet.getData(), packet.getLength(),
					packet.getAddress().getHostAddress());
				if(announce != null) entries.put(announce.hostPort(), announce);
			}

			socket.close();
		}

		/** Stop listening. Safe to call from any thread. */
		public void shutdown() {
			shutdownRequested = true;
			socket.close();
		}

		/** @return All live (non-expired) discovered hosts, sorted by host:port */
		public List<Announce> snapshot() {
			long now = System.currentTimeMillis();
			List<Announce> result = new ArrayList<Announce>();

			for(Announce announce: entries.values()) {
				if(now - announce.lastSeen <= ttlMs) {
					result.add(announce);
				} else {
					entries.remove(announce.hostPort(), announce);
				}
			}

			Collections.sort(result, new Comparator<Announce>() {
				public int compare(Announce a, Announce b) {
					return a.hostPort().compareTo(b.hostPort());
				}
			});
			return result;
		}
	}
}
