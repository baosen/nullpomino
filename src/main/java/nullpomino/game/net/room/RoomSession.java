// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;

import nullpomino.game.net.NetLanDiscovery;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.game.play.GameManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One peer's participation in a P2P room: the single
 * dispatcher thread that owns all session state, the handshake state
 * machine, the login synthesis for the local client, routing (control to
 * the arbiter, game traffic directly to same-room peers, records queries
 * to the local files), and room-scope filtering of authoritative
 * broadcasts.
 *
 * <p>Created via {@link #create} (become the first arbiter) or
 * {@link #join} (dial an existing session). The local client attaches
 * through the {@link RoomEndpoint} face.
 */
public class RoomSession implements RoomEndpoint, RoomEventSink {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(RoomSession.class);

	public enum State { CONNECTING, JOINING, READY, MIGRATING, CLOSED }

	/** Session-level callbacks for the UI layer (called on the dispatcher thread) */
	public interface Listener {
		void onSessionState(State state, String detail);
		void onArbiterChanged(int arbiterUid, boolean localIsArbiter);
	}

	// ---------------------------------------------------------------- construction

	private final RoomConfig config;
	private final Listener listener;
	private final String selfName;
	private final RoomTransport transport;
	private final RoomRoster roster = new RoomRoster();
	private final RoomMirror mirror = new RoomMirror();
	private final RoomLocalRecords records;
	private final Random rand = new Random();

	private final LinkedBlockingQueue<RoomEvent> queue = new LinkedBlockingQueue<RoomEvent>();
	private final Thread dispatcherThread;
	private final Timer tickTimer = new Timer("RoomTick", true);

	/** Non-null iff this peer is the arbiter */
	private RoomAuthority authority;
	/** Broadcast sequence counter (arbiter only) */
	private long seq = 0;

	private volatile State state = State.CONNECTING;
	private String token = "";
	private int localUid = -1;
	private int arbiterUid = -1;
	private volatile RoomPeerLink arbiterLink;   // null when local peer is arbiter; set from the dialer thread
	private String displayHost = "?";

	/** Local stats identity (personal bests etc.), independent of the mirror */
	private final NetPlayerInfo selfLocal = new NetPlayerInfo();

	/** Links accepted/dialed but not yet uid-bound */
	private final Set<RoomPeerLink> pendingLinks = new HashSet<RoomPeerLink>();

	/** Joiner handshake bookkeeping */
	private int peerOksAwaited = 0;
	private boolean welcomeSeen = false;
	private long joinStartedAt;

	/** Local client attachment */
	private volatile LineListener lineListener;
	private volatile ClosedListener closedListener;
	private boolean closedFired = false;
	private boolean clientAttached = false;

	/** Per-room dedup + rating capture (cleared on each start line) */
	private final Map<Integer, Set<Integer>> deadSeen = new HashMap<Integer, Set<Integer>>();
	private final Set<Integer> finishSeen = new HashSet<Integer>();
	private final Map<Integer, List<int[]>> pendingRatings = new HashMap<Integer, List<int[]>>();     // scope -> [uid, rating]
	private final Map<Integer, List<String>> pendingRatingNames = new HashMap<Integer, List<String>>();

	/** LAN beacon */
	private NetLanDiscovery.Announcer announcer;

	/** What the beacon currently announces; null = nothing to announce yet
	 *  (no room created). Refreshed by the dispatcher, read by the announcer. */
	private volatile NetLanDiscovery.Announce beaconSnapshot;

	/** Volatile local state re-sent after a migration (at-most-once healing) */
	private String pendingDeadControl;
	private String pendingRacewinControl;
	private Boolean lastReadySent;

	/** Migration bookkeeping (non-arbiter waiting for a claim) */
	private int expectedClaimUid = -1;
	private long claimDeadline = 0;

	/** Peerdown aggregation (arbiter only) */
	private final Map<Integer, Integer> peerdownReports = new HashMap<Integer, Integer>();
	private long peerdownWindowEnd = 0;

	private RoomSession(String playerName, RoomConfig config, Listener listener) {
		this.selfName = playerName;
		this.config = config;
		this.listener = listener;
		this.records = new RoomLocalRecords();
		this.transport = new RoomTransport(this);
		selfLocal.strName = playerName;
		records.loadInto(selfLocal);
		dispatcherThread = new Thread(this::dispatchLoop, "RoomDispatcher");
		dispatcherThread.setDaemon(true);
	}

	/** Create a session and become the first arbiter. The listen port is bound when this returns. */
	public static RoomSession create(String playerName, RoomConfig config, Listener listener) throws IOException {
		RoomSession session = new RoomSession(playerName, config, listener);
		session.transport.startListening(config.listenPort);
		session.token = RoomProtocol.generateToken(session.rand);
		session.displayHost = getLanAddress();

		session.authority = new RoomAuthority(session.new AuthoritySink(), session.rand);
		session.localUid = session.authority.reserveUid();
		session.arbiterUid = session.localUid;
		session.roster.add(new RoomRoster.Entry(session.localUid, playerName, "127.0.0.1",
			session.transport.getListenPort(), null));
		session.authority.admitMember(session.localUid, playerName, "127.0.0.1",
			session.selfLocal.rating, session.selfLocal.playCount, session.selfLocal.winCount);

		session.setState(State.READY, "created");
		session.startAnnouncer();
		session.dispatcherThread.start();
		session.startTicks();
		log.info("Room session created, listening on {}", session.transport.getListenPort());
		return session;
	}

	/** Join an existing session via its arbiter. Progress is reported through the listener. */
	public static RoomSession join(String host, int port, String playerName, RoomConfig config,
		Listener listener) throws IOException
	{
		RoomSession session = new RoomSession(playerName, config, listener);
		session.transport.startListening(config.listenPort);
		session.displayHost = host;
		session.joinStartedAt = System.currentTimeMillis();
		session.dispatcherThread.start();
		session.startTicks();

		// Dial off-thread: transport.dial blocks
		Thread dialer = new Thread(() -> {
			try {
				RoomPeerLink link = session.transport.dial(host, port, config.joinTimeout);
				synchronized(session.pendingLinks) {
					session.pendingLinks.add(link);
				}
				session.arbiterLink = link;
				link.sendLine(RoomProtocol.buildHelloJoin(GameManager.getVersionMajor(),
					GameManager.isDevBuild(), session.transport.getListenPort(), playerName,
					session.selfLocal.rating, session.selfLocal.playCount, session.selfLocal.winCount));
			} catch (IOException e) {
				log.info("Room join dial failed", e);
				session.queue.add(RoomEvent.shutdown("JOIN_FAILED:" + e.getMessage()));
			}
		}, "RoomDialer");
		dialer.setDaemon(true);
		dialer.start();
		return session;
	}

	private void startTicks() {
		tickTimer.schedule(new TimerTask() {
			@Override public void run() {
				queue.add(RoomEvent.tick());
			}
		}, 1000, 1000);
	}

	/**
	 * Rebuild what the beacon announces from the mirrored room state. A
	 * session only announces once it holds a room (prevents joining a
	 * half-created session while the create-room form is still open).
	 */
	private void refreshBeaconSnapshot() {
		if(state == State.CLOSED) {
			beaconSnapshot = null;
			return;
		}
		// Only the arbiter announces: joiners dial the beacon's source address,
		// and only the arbiter admits joins (anyone else denies the handshake
		// with BAD_TOKEN). On arbiter loss the promoted survivor starts
		// announcing within one beacon cycle, inside the row TTL, so the room
		// stays discoverable.
		if(!isArbiter()) {
			beaconSnapshot = null;
			return;
		}
		NetRoomInfo room = mirror.getRooms().isEmpty() ? null : mirror.getRooms().getFirst();
		if(room == null) {
			beaconSnapshot = null;
			return;
		}
		// Names come from the mirror so /name renames reach the beacon too
		NetPlayerInfo self = mirror.getPlayer(localUid);
		String ownName = (self != null) ? self.strName : selfName;
		beaconSnapshot = new NetLanDiscovery.Announce("", transport.getListenPort(), ownName,
			String.valueOf(GameManager.getVersionMajor()), token, ownName, roster.size(),
			room.strName, room.rated, room.ruleLock ? room.ruleName : "", room.strMode,
			room.playing, room.playerSeatedCount, room.maxPlayers, room.spectatorCount);
	}

	/** Package-private: tests inspect the announce gating */
	NetLanDiscovery.Announce getBeaconSnapshotForTest() {
		return beaconSnapshot;
	}

	private void startAnnouncer() {
		if(!config.lanAnnounce || (announcer != null)) return;
		announcer = new NetLanDiscovery.Announcer(() -> {
			NetLanDiscovery.Announce a = beaconSnapshot;
			return (a == null) ? null : NetLanDiscovery.encodeRoomAnnounce(a);
		});
		announcer.start();
	}

	// ================================================================ RoomEndpoint (client seam)

	@Override
	public void setLineListener(LineListener listener) {
		this.lineListener = listener;
	}

	@Override
	public void setClosedListener(ClosedListener listener) {
		this.closedListener = listener;
	}

	@Override
	public void sendLine(String line) {
		if(state == State.CLOSED) return;
		String trimmed = line.endsWith("\n") ? line.substring(0, line.length() - 1) : line;
		if(trimmed.length() == 0) return;
		queue.add(RoomEvent.localSend(trimmed));
	}

	@Override
	public void clientReady() {
		queue.add(RoomEvent.localSend(" clientready"));
	}

	@Override
	public boolean isOpen() {
		return state != State.CLOSED;
	}

	@Override
	public int getListenPort() {
		return transport.getListenPort();
	}

	@Override
	public String getDisplayHost() {
		return displayHost;
	}

	@Override
	public String getSessionId() {
		return token;
	}

	@Override
	public void shutdown() {
		queue.add(RoomEvent.shutdown("LEFT"));
	}

	public boolean isArbiter() {
		return authority != null;
	}

	public int getLocalUid() {
		return localUid;
	}

	public State getState() {
		return state;
	}

	// ================================================================ dispatcher

	private void dispatchLoop() {
		try {
			while(state != State.CLOSED) {
				processOneEvent(queue.take());
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (Throwable e) {
			log.error("Room dispatcher died", e);
			doShutdown("dispatcher error: " + e);
		}
	}

	/** Package-private so tests can drive events deterministically */
	void processOneEvent(RoomEvent event) {
		try {
			dispatchEvent(event);
		} finally {
			refreshBeaconSnapshot();
		}
	}

	private void dispatchEvent(RoomEvent event) {
		switch(event.type) {
		case LINK_ACCEPTED:
			synchronized(pendingLinks) {
				pendingLinks.add(event.link);
			}
			break;
		case LINE:
			onLineEvent(event.link, event.line);
			break;
		case LINK_CLOSED:
			onLinkClosedEvent(event.link, event.reason);
			break;
		case LOCAL_SEND:
			onLocalSend(event.line);
			break;
		case TICK:
			onTick();
			break;
		case SHUTDOWN:
			doShutdown(event.reason);
			break;
		}
	}

	// RoomEventSink (transport threads -> queue)
	@Override
	public void onLinkAccepted(RoomPeerLink link) {
		queue.add(RoomEvent.linkAccepted(link));
	}

	@Override
	public void onLine(RoomPeerLink link, String line) {
		queue.add(RoomEvent.line(link, line));
	}

	@Override
	public void onLinkClosed(RoomPeerLink link, String reason) {
		queue.add(RoomEvent.linkClosed(link, reason));
	}

	// ================================================================ inbound lines

	private void onLineEvent(RoomPeerLink link, String line) {
		if(!RoomProtocol.isRoomFrame(line)) {
			// Pre-stamped game/gstat traffic from a peer
			if(line.startsWith("game\t") || line.startsWith("gstat\t")) {
				deliverPeerGameLine(line);
			} else {
				// A stale (non-room) client connected and sent a plain protocol line
				link.sendLine(RoomProtocol.buildDeny(RoomProtocol.DENY_DIFFERENT_VERSION,
					GameManager.getVersionString()));
				link.closeAfterFlush("not a room peer");
			}
			return;
		}

		String[] parts = line.split("\t", -1);
		String type = parts[1];

		if(type.equals("hello")) { onHello(link, parts); return; }
		if(type.equals("welcome")) { onWelcome(link, parts); return; }
		if(type.equals("snap")) { mirror.applySnapshot(parts); return; }
		if(type.equals("snapend")) { onSnapEnd(); return; }
		if(type.equals("peerok")) { onPeerOk(link, parts); return; }
		if(type.equals("peer")) { onPeerAnnounce(parts); return; }
		if(type.equals("roomok")) { onRoomOk(link); return; }
		if(type.equals("deny")) { onDeny(link, parts); return; }
		if(type.equals("bye")) { onBye(link); return; }
		if(type.equals("c")) { onControlFrame(link, line); return; }
		if(type.equals("b")) { onBroadcastFrame(line); return; }
		if(type.equals("d")) { onDirectFrame(line); return; }
		if(type.equals("authg")) {
			RoomProtocol.AuthGlobal g = RoomProtocol.parseAuthGlobal(parts);
			if(g != null) mirror.applyAuthGlobal(g);
			return;
		}
		if(type.equals("authr")) {
			RoomProtocol.AuthRoom a = RoomProtocol.parseAuthRoom(parts);
			if(a != null) mirror.applyAuthRoom(a);
			return;
		}
		if(type.equals("arbiter")) { onArbiterClaim(link, parts); return; }
		if(type.equals("peerdown")) { onPeerDownReport(parts); return; }
		if(type.equals("kick")) { onKick(parts); return; }
		if(line.equals(RoomProtocol.LINE_PING)) { link.sendLine(RoomProtocol.LINE_PONG); return; }
		if(line.equals(RoomProtocol.LINE_PONG)) { return; }

		log.debug("Unhandled room frame: {}", parts[1]);
	}

	private void onHello(RoomPeerLink link, String[] parts) {
		RoomProtocol.Hello hello = RoomProtocol.parseHello(parts);
		if(hello == null) {
			link.close("malformed hello");
			return;
		}

		// Version gate (both variants)
		if(hello.verMajor != GameManager.getVersionMajor()) {
			link.sendLine(RoomProtocol.buildDeny(RoomProtocol.DENY_DIFFERENT_VERSION,
				String.valueOf(GameManager.getVersionMajor())));
			link.closeAfterFlush("version mismatch");
			return;
		}
		if(hello.devBuild != GameManager.isDevBuild()) {
			link.sendLine(RoomProtocol.buildDeny(RoomProtocol.DENY_DIFFERENT_BUILD,
				GameManager.getVersionString()));
			link.closeAfterFlush("build mismatch");
			return;
		}

		if(hello.joinVariant) {
			// Only the arbiter admits joiners
			if(!isArbiter()) {
				link.sendLine(RoomProtocol.buildDeny(RoomProtocol.DENY_BAD_TOKEN, "not the arbiter"));
				link.closeAfterFlush("join at non-arbiter");
				return;
			}

			int uid = authority.reserveUid();
			// playernew reaches existing members; the joiner gets the snapshot instead
			authority.admitMember(uid, hello.name, link.getRemoteAddress(),
				hello.ratings, hello.playCounts, hello.winCounts);

			link.uid = uid;
			synchronized(pendingLinks) {
				pendingLinks.remove(link);
			}
			roster.add(new RoomRoster.Entry(uid, hello.name, link.getRemoteAddress(), hello.listenPort, link));

			// Welcome + full snapshot + snapend, all on this link in one step
			List<RoomProtocol.RosterEntry> entries = new ArrayList<RoomProtocol.RosterEntry>();
			for(RoomRoster.Entry e: roster.all().values()) {
				if(e.uid != uid) entries.add(new RoomProtocol.RosterEntry(e.uid, e.host, e.listenPort, e.name));
			}
			link.sendLine(RoomProtocol.buildWelcome(token, uid, hello.name, arbiterUid, seq, entries));
			sendSnapshot(link);
			link.sendLine(RoomProtocol.LINE_SNAPEND);

			// Announce to the other members; the joiner will dial them
			for(RoomRoster.Entry e: roster.linkedMembers()) {
				if(e.uid != uid) {
					e.link.sendLine(RoomProtocol.buildPeerAnnounce(uid, link.getRemoteAddress(),
						hello.listenPort, hello.name));
				}
			}
		} else {
			// hello peer: a welcomed joiner dialing an existing member
			if(!token.equals(hello.token)) {
				link.sendLine(RoomProtocol.buildDeny(RoomProtocol.DENY_BAD_TOKEN, ""));
				link.closeAfterFlush("bad token");
				return;
			}
			link.uid = hello.uid;
			synchronized(pendingLinks) {
				pendingLinks.remove(link);
			}
			RoomRoster.Entry entry = roster.get(hello.uid);
			if(entry == null) {
				// Announce may not have arrived yet; token proves membership
				entry = new RoomRoster.Entry(hello.uid, "?", link.getRemoteAddress(), hello.listenPort, link);
				roster.add(entry);
			} else {
				entry.link = link;
				entry.listenPort = hello.listenPort;
			}
			link.sendLine(RoomProtocol.buildPeerOk(localUid));
		}
	}

	/** Send the full state snapshot to a joiner's link */
	private void sendSnapshot(RoomPeerLink link) {
		for(NetRoomInfo room: authority.getRooms()) {
			link.sendLine(RoomProtocol.buildSnapRoom(room.roomID, room.exportString()));
		}
		for(NetPlayerInfo p: authority.getPlayers().values()) {
			link.sendLine(RoomProtocol.buildSnapPlayer(p.exportString()));
		}
		for(Map.Entry<Integer, String[]> e: authority.getRuleBlobs().entrySet()) {
			link.sendLine(RoomProtocol.buildSnapRule(e.getKey(), e.getValue()[0], e.getValue()[1]));
		}
		for(Map.Entry<Integer, String> e: mirror.getRoomRuleBlobs().entrySet()) {
			link.sendLine(RoomProtocol.buildSnapRoomRule(e.getKey(), e.getValue()));
		}
		for(Map.Entry<Integer, String> e: mirror.getMapBlobs().entrySet()) {
			link.sendLine(RoomProtocol.buildSnapMap(e.getKey(), e.getValue()));
		}
		for(NetRoomInfo room: authority.getRooms()) {
			for(nullpomino.game.net.NetChatMessage chat: room.chatList) {
				link.sendLine(RoomProtocol.buildSnapChat(room.roomID, chat.exportString()));
			}
		}
		for(nullpomino.game.net.NetChatMessage chat: authority.getLobbyChatList()) {
			link.sendLine(RoomProtocol.buildSnapLobbyChat(chat.exportString()));
		}
		link.sendLine(RoomProtocol.buildAuthGlobal(seq, authority.getNextUid(), authority.getNextRoomId()));
		for(NetRoomInfo room: authority.getRooms()) {
			link.sendLine(RoomProtocol.buildAuthRoom(RoomAuthority.buildAuthRoom(seq, room)));
		}
	}

	private void onWelcome(RoomPeerLink link, String[] parts) {
		RoomProtocol.Welcome welcome = RoomProtocol.parseWelcome(parts);
		if(welcome == null) {
			doShutdown("JOIN_FAILED:malformed welcome");
			return;
		}
		token = welcome.token;
		localUid = welcome.uid;
		arbiterUid = welcome.arbiterUid;
		welcomeSeen = true;
		setState(State.JOINING, "welcomed");

		link.uid = arbiterUid;
		synchronized(pendingLinks) {
			pendingLinks.remove(link);
		}
		roster.add(new RoomRoster.Entry(localUid, welcome.name, "127.0.0.1", transport.getListenPort(), null));
		roster.add(new RoomRoster.Entry(arbiterUid, "?", link.getRemoteAddress(), -1, link));
		for(RoomProtocol.RosterEntry e: welcome.roster) {
			if((e.uid != arbiterUid) && (e.uid != localUid)) {
				roster.add(new RoomRoster.Entry(e.uid, e.name, e.host, e.listenPort, null));
			} else if(e.uid == arbiterUid) {
				RoomRoster.Entry arb = roster.get(arbiterUid);
				arb.name = e.name;
				arb.listenPort = e.listenPort;
			}
		}
	}

	private void onSnapEnd() {
		// Snapshot applied; dial every member that isn't the arbiter or us
		final List<RoomRoster.Entry> toDial = new ArrayList<RoomRoster.Entry>();
		for(RoomRoster.Entry e: roster.all().values()) {
			if((e.uid != localUid) && (e.uid != arbiterUid) && (e.link == null)) {
				toDial.add(e);
			}
		}
		peerOksAwaited = toDial.size();

		if(peerOksAwaited == 0) {
			completeJoin();
			return;
		}

		Thread dialer = new Thread(() -> {
			for(RoomRoster.Entry e: toDial) {
				try {
					RoomPeerLink link = transport.dial(e.host, e.listenPort, config.joinTimeout);
					link.uid = e.uid;
					link.sendLine(RoomProtocol.buildHelloPeer(GameManager.getVersionMajor(),
						GameManager.isDevBuild(), token, localUid, transport.getListenPort()));
				} catch (IOException ex) {
					log.info("Room peer dial to uid {} failed", e.uid, ex);
					queue.add(RoomEvent.shutdown("JOIN_FAILED:peer dial " + e.uid));
					return;
				}
			}
		}, "RoomDialer");
		dialer.setDaemon(true);
		dialer.start();
	}

	private void onPeerOk(RoomPeerLink link, String[] parts) {
		int uid = Integer.parseInt(parts[2]);
		link.uid = uid;
		RoomRoster.Entry entry = roster.get(uid);
		if(entry != null) entry.link = link;

		if((state == State.JOINING) && (peerOksAwaited > 0)) {
			peerOksAwaited--;
			if(peerOksAwaited == 0) completeJoin();
		}
	}

	private void completeJoin() {
		if(arbiterLink != null) arbiterLink.sendLine(RoomProtocol.LINE_ROOMOK);
		setState(State.READY, "joined");
		startAnnouncer();
		if(clientAttached) synthesizeWelcome();
	}

	private void onPeerAnnounce(String[] parts) {
		RoomProtocol.RosterEntry e = RoomProtocol.parsePeerAnnounce(parts);
		if(e == null) return;
		RoomRoster.Entry existing = roster.get(e.uid);
		if(existing == null) {
			roster.add(new RoomRoster.Entry(e.uid, e.name, e.host, e.listenPort, null));
		} else {
			existing.name = e.name;
			existing.host = e.host;
			existing.listenPort = e.listenPort;
		}
	}

	private void onRoomOk(RoomPeerLink link) {
		RoomRoster.Entry entry = roster.getByLink(link);
		if(entry != null) entry.linksOk = true;
	}

	private void onDeny(RoomPeerLink link, String[] parts) {
		String reason = (parts.length > 2) ? parts[2] : "?";
		log.info("Room deny: {}", reason);
		if(link == arbiterLink) doShutdown("JOIN_FAILED:" + reason);
		else link.close("denied");
	}

	private void onBye(RoomPeerLink link) {
		link.close("bye");
		// LINK_CLOSED handles the rest
	}

	private void onControlFrame(RoomPeerLink link, String line) {
		if(!isArbiter()) return;
		String payload = RoomProtocol.unwrapControl(line);
		if((payload == null) || (link.uid < 0)) return;
		RoomRoster.Entry entry = roster.get(link.uid);
		// A half-joined member (no roomok yet) may not enter rooms
		if((entry != null) && !entry.linksOk && payload.startsWith("roomjoin\t")) return;
		authority.handleControl(link.uid, payload.split("\t", -1));
	}

	private void onBroadcastFrame(String line) {
		RoomProtocol.Broadcast b = RoomProtocol.parseBroadcast(line);
		if(b == null) return;
		mirror.applyBroadcast(b.seq, b.scope, b.payload);
		deliverBroadcastLocal(b.scope, b.payload);
	}

	private void onDirectFrame(String line) {
		String payload = RoomProtocol.unwrapDirect(line);
		if(payload != null) deliverToClient(payload);
	}

	// ================================================================ local sends (client -> mesh)

	private void onLocalSend(String line) {
		if(line.equals(" clientready")) {
			clientAttached = true;
			if(state == State.READY) synthesizeWelcome();
			return;
		}
		if(state != State.READY && state != State.MIGRATING) return;

		if(line.equals("ping")) {
			deliverToClient("pong");
			return;
		}
		if(line.startsWith("login\t")) {
			synthesizeLogin();
			return;
		}
		if(line.equals("disconnect")) {
			doShutdown("LEFT");
			return;
		}
		if(line.startsWith("game\t")) {
			fanOutGameLine("game", line.substring("game\t".length()), false);
			return;
		}
		if(line.startsWith("gstat\t")) {
			fanOutGameLine("gstat", line.substring("gstat\t".length()), true);
			return;
		}
		if(line.startsWith("mpranking\t")) {
			int style = Integer.parseInt(line.split("\t")[1]);
			deliverToClient(records.buildMPRankingReply(style, selfWithMirrorStats(style)));
			return;
		}
		if(line.startsWith("spsend\t")) {
			NetPlayerInfo self = mirror.getPlayer(localUid);
			NetRoomInfo room = (self == null) ? null : mirror.getRoom(self.roomID);
			if((room != null) && (self.seatID != -1)) {
				deliverToClient(records.registerSPRecord(room, selfLocal, line.split("\t", -1)));
			}
			return;
		}
		if(line.startsWith("spranking\t")) {
			deliverToClient(records.buildSPRankingReply(line.split("\t", -1), selfLocal));
			return;
		}

		// Track volatile state so it can be re-sent if the arbiter dies with
		// the control in flight (all idempotent at the authority)
		if(line.startsWith("dead")) pendingDeadControl = line;
		else if(line.startsWith("racewin\t")) pendingRacewinControl = line;
		else if(line.startsWith("ready\t")) lastReadySent = Boolean.valueOf(line.split("\t")[1]);

		// Everything else is a control message for the arbiter
		routeControl(line);
	}

	/** Local leaderboard replies show the mirror's current view of our stats */
	private NetPlayerInfo selfWithMirrorStats(int style) {
		NetPlayerInfo self = mirror.getPlayer(localUid);
		return (self != null) ? self : selfLocal;
	}

	/** Stamp uid/seat and fan out to same-room peers (gstat also loops back locally) */
	private void fanOutGameLine(String command, String rest, boolean loopback) {
		NetPlayerInfo self = mirror.getPlayer(localUid);
		if((self == null) || (self.roomID == -1) || (self.seatID == -1)) return;

		String stamped;
		if(command.equals("gstat")) {
			stamped = "gstat\t" + localUid + "\t" + self.seatID + "\t" + NetUtil.urlEncode(self.strName) + "\t" + rest;
		} else {
			stamped = "game\t" + localUid + "\t" + self.seatID + "\t" + rest;
		}

		for(RoomRoster.Entry e: roster.linkedMembers()) {
			NetPlayerInfo p = mirror.getPlayer(e.uid);
			if((p != null) && (p.roomID == self.roomID)) {
				e.link.sendLine(stamped);
			}
		}
		if(loopback) deliverToClient(stamped);
	}

	/** Deliver a pre-stamped game/gstat line from a peer if it's for our room */
	private void deliverPeerGameLine(String line) {
		String[] head = line.split("\t", 4);
		if(head.length < 3) return;
		int senderUid;
		try {
			senderUid = Integer.parseInt(head[1]);
		} catch (NumberFormatException e) {
			return;
		}
		NetPlayerInfo sender = mirror.getPlayer(senderUid);
		NetPlayerInfo self = mirror.getPlayer(localUid);
		if((sender == null) || (self == null)) return;
		if((self.roomID == -1) || (sender.roomID != self.roomID)) return;
		deliverToClient(line);
	}

	// ================================================================ login synthesis

	private void synthesizeWelcome() {
		deliverToClient("welcome\t" + GameManager.getVersionMajor() + "\t" + (roster.size()) + "\t0\t" +
			GameManager.getVersionMinor() + "\t" + GameManager.getVersionString() + "\t" +
			config.pingInterval + "\t" + GameManager.isDevBuild());
	}

	private void synthesizeLogin() {
		deliverToClient("loginsuccess\t" + NetUtil.urlEncode(selfName) + "\t" + localUid);

		StringBuilder playerList = new StringBuilder("playerlist\t").append(mirror.getPlayers().size());
		for(NetPlayerInfo p: mirror.getPlayers().values()) {
			playerList.append('\t').append(p.exportString());
		}
		deliverToClient(playerList.toString());

		StringBuilder roomList = new StringBuilder("roomlist\t").append(mirror.getRooms().size());
		for(NetRoomInfo room: mirror.getRooms()) {
			roomList.append('\t').append(room.exportString());
		}
		deliverToClient(roomList.toString());
		// The client's ruledata follows as a normal control; ruledatasuccess
		// comes back as a direct line and gates lobbyMode=LOBBY
	}

	// ================================================================ local delivery + scope filter

	private void deliverBroadcastLocal(int scope, String payload) {
		captureRatingFlow(scope, payload);

		if(scope != RoomProtocol.SCOPE_GLOBAL) {
			NetPlayerInfo self = mirror.getPlayer(localUid);
			int myRoom = (self == null) ? -1 : self.roomID;
			if(scope != myRoom) return;   // never leak another room's lines into the client

			// The round progressed: the in-flight controls were not lost
			if(payload.startsWith("dead\t" + localUid + "\t")) pendingDeadControl = null;
			else if(payload.startsWith("finish\t")) pendingRacewinControl = null;
			else if(payload.startsWith("start\t")) { pendingDeadControl = null; pendingRacewinControl = null; }

			// Dedup dead/finish around migration resyncs
			if(payload.startsWith("dead\t")) {
				int uid = Integer.parseInt(payload.split("\t")[1]);
				Set<Integer> seen = deadSeen.get(scope);
				if(seen == null) {
					seen = new HashSet<Integer>();
					deadSeen.put(scope, seen);
				}
				if(!seen.add(uid)) return;
			} else if(payload.startsWith("finish\t")) {
				if(!finishSeen.add(scope)) return;
			} else if(payload.startsWith("start\t")) {
				deadSeen.remove(scope);
				finishSeen.remove(scope);
			}
		}
		deliverToClient(payload);
	}

	/** Track rating lines per round and commit them to the local records at finish */
	private void captureRatingFlow(int scope, String payload) {
		if(scope == RoomProtocol.SCOPE_GLOBAL) return;

		if(payload.startsWith("start\t")) {
			pendingRatings.remove(scope);
			pendingRatingNames.remove(scope);
		} else if(payload.startsWith("rating\t")) {
			String[] m = payload.split("\t");
			List<int[]> list = pendingRatings.get(scope);
			List<String> names = pendingRatingNames.get(scope);
			if(list == null) {
				list = new ArrayList<int[]>();
				names = new ArrayList<String>();
				pendingRatings.put(scope, list);
				pendingRatingNames.put(scope, names);
			}
			list.add(new int[] { Integer.parseInt(m[1]), Integer.parseInt(m[4]) });
			names.add(NetUtil.urlDecode(m[3]));
		} else if(payload.startsWith("finish\t")) {
			List<int[]> list = pendingRatings.remove(scope);
			List<String> names = pendingRatingNames.remove(scope);
			if(list == null) return;
			int winnerUid = Integer.parseInt(payload.split("\t")[1]);
			NetRoomInfo room = mirror.getRoom(scope);
			int style = (room == null) ? 0 : room.style;

			for(int i = 0; i < list.size(); i++) {
				int uid = list.get(i)[0];
				int newRating = list.get(i)[1];
				boolean won = (uid == winnerUid);
				records.recordRatedResult(style, names.get(i), newRating, won);
				if(uid == localUid) {
					selfLocal.rating[style] = newRating;
					selfLocal.playCount[style]++;
					if(won) selfLocal.winCount[style]++;
					records.saveFrom(selfLocal);
				}
			}
		}
	}

	private void deliverToClient(String line) {
		LineListener l = lineListener;
		if(l != null) {
			try {
				l.onLine(line);
			} catch (Exception e) {
				log.error("Line listener failed on: {}", line, e);
			}
		}
	}

	// ================================================================ arbiter sink

	/** Routes RoomAuthority output onto the mesh + own mirror + own client */
	private final class AuthoritySink implements RoomAuthority.Sink {
		@Override
		public void broadcast(int scope, String line, int exceptUid) {
			seq++;
			String frame = RoomProtocol.wrapBroadcast(seq, scope, line);
			for(RoomRoster.Entry e: roster.linkedMembers()) {
				if(e.uid != exceptUid) e.link.sendLine(frame);
			}
			mirror.applyBroadcast(seq, scope, line);
			if(exceptUid != localUid) deliverBroadcastLocal(scope, line);
		}

		@Override
		public void direct(int uid, String line) {
			if(uid == localUid) {
				deliverToClient(line);
			} else {
				RoomRoster.Entry e = roster.get(uid);
				if((e != null) && (e.link != null)) e.link.sendLine(RoomProtocol.wrapDirect(line));
			}
		}

		@Override
		public void ruleCache(int uid, String checksum, String compressedData) {
			sendCacheFrame(RoomProtocol.buildSnapRule(uid, checksum, compressedData));
		}

		@Override
		public void roomRuleCache(int roomId, String compressedData) {
			sendCacheFrame(RoomProtocol.buildSnapRoomRule(roomId, compressedData));
		}

		@Override
		public void mapCache(int roomId, String compressedData) {
			sendCacheFrame(RoomProtocol.buildSnapMap(roomId, compressedData));
		}

		private void sendCacheFrame(String frame) {
			for(RoomRoster.Entry e: roster.linkedMembers()) {
				e.link.sendLine(frame);
			}
			mirror.applySnapshot(frame.split("\t", -1));
		}

		@Override
		public void authUpdate() {
			seq++;
			String authg = RoomProtocol.buildAuthGlobal(seq, authority.getNextUid(), authority.getNextRoomId());
			List<String> frames = new ArrayList<String>();
			frames.add(authg);
			for(NetRoomInfo room: authority.getRooms()) {
				frames.add(RoomProtocol.buildAuthRoom(RoomAuthority.buildAuthRoom(seq, room)));
			}
			for(RoomRoster.Entry e: roster.linkedMembers()) {
				for(String frame: frames) e.link.sendLine(frame);
			}
			mirror.applyAuthGlobal(RoomProtocol.parseAuthGlobal(authg.split("\t", -1)));
			for(int i = 1; i < frames.size(); i++) {
				mirror.applyAuthRoom(RoomProtocol.parseAuthRoom(frames.get(i).split("\t", -1)));
			}
		}
	}

	// ================================================================ link loss / liveness / shutdown

	private void onLinkClosedEvent(RoomPeerLink link, String reason) {
		synchronized(pendingLinks) {
			if(pendingLinks.remove(link)) return;   // unbound link died: nothing else to do
		}

		RoomRoster.Entry entry = roster.getByLink(link);
		if(entry == null) return;
		entry.link = null;

		if(isArbiter()) {
			// A member is gone: run the full logout sequence
			roster.remove(entry.uid);
			authority.onMemberGone(entry.uid, "bye".equals(reason));
		} else if(link == arbiterLink) {
			if(state != State.READY && state != State.MIGRATING) {
				// Lost the arbiter mid-join: abort, no migration participation
				doShutdown("JOIN_FAILED:arbiter lost during join");
				return;
			}
			arbiterLink = null;
			roster.remove(entry.uid);
			beginMigration();
		} else {
			// A direct link to another member died while the arbiter link is
			// fine: report it so the arbiter can arbitrate a split mesh
			if((arbiterLink != null) && (roster.get(entry.uid) != null)) {
				arbiterLink.sendLine(RoomProtocol.buildPeerDown(entry.uid));
			}
		}
	}

	// ================================================================ arbiter migration

	/** The arbiter is gone: deterministically pick the lowest surviving uid */
	private void beginMigration() {
		int successor = roster.lowestUid();
		if(successor == -1) {
			doShutdown("LAST_PEER");
			return;
		}

		if(successor == localUid) {
			promoteSelf();
		} else {
			setState(State.MIGRATING, "waiting for claim from uid " + successor);
			expectedClaimUid = successor;
			claimDeadline = System.currentTimeMillis() + config.claimTimeout;
		}
	}

	/** Become the arbiter: adopt the mirror, claim, resync, bury the old arbiter */
	private void promoteSelf() {
		int oldArbiterUid = arbiterUid;
		log.info("Promoting self (uid {}) to arbiter, replacing uid {}", localUid, oldArbiterUid);

		mirror.promote();
		authority = new RoomAuthority(new AuthoritySink(), rand);
		authority.adoptState(mirror.getPlayers(), mirror.getRooms(), mirror.getRuleBlobs());
		authority.restoreCounters(mirror.getNextUid(), mirror.getNextRoomId());
		seq = mirror.getSeq();
		arbiterUid = localUid;
		arbiterLink = null;
		expectedClaimUid = -1;
		claimDeadline = 0;

		for(RoomRoster.Entry e: roster.linkedMembers()) {
			e.link.sendLine(RoomProtocol.buildArbiterClaim(localUid, seq));
		}

		// Re-baseline everyone, then process the old arbiter's departure
		// through the normal path (mid-game: its dead line, maybe a finish)
		authority.resyncAll();
		authority.onMemberGone(oldArbiterUid, false);

		setState(State.READY, "promoted");
		if(listener != null) listener.onArbiterChanged(localUid, true);
		resendVolatileState();
	}

	/** A successor announced itself on our existing link to it */
	private void onArbiterClaim(RoomPeerLink link, String[] parts) {
		int claimUid;
		try {
			claimUid = Integer.parseInt(parts[2]);
		} catch (RuntimeException e) {
			return;
		}
		if(link.uid != claimUid) return;   // claims only count on the claimant's own link

		arbiterUid = claimUid;
		arbiterLink = link;
		expectedClaimUid = -1;
		claimDeadline = 0;
		RoomRoster.Entry entry = roster.get(claimUid);

		setState(State.READY, "arbiter is now uid " + claimUid);
		if(listener != null) listener.onArbiterChanged(claimUid, false);
		resendVolatileState();
	}

	/** Heal controls lost in flight to the dead arbiter (all idempotent at the authority) */
	private void resendVolatileState() {
		NetPlayerInfo self = mirror.getPlayer(localUid);
		if(pendingDeadControl != null) {
			routeControl(pendingDeadControl);
		}
		if(pendingRacewinControl != null) {
			routeControl(pendingRacewinControl);
		}
		if((lastReadySent != null) && (self != null) && (self.ready != lastReadySent.booleanValue())) {
			routeControl("ready\t" + lastReadySent);
		}
	}

	private void routeControl(String line) {
		if(isArbiter()) {
			authority.handleControl(localUid, line.split("\t", -1));
		} else if(arbiterLink != null) {
			arbiterLink.sendLine(RoomProtocol.wrapControl(line));
		}
	}

	// ================================================================ peerdown / kick

	/** Arbiter aggregates broken-link reports and kicks the worst-connected member */
	private void onPeerDownReport(String[] parts) {
		if(!isArbiter()) return;
		int uid;
		try {
			uid = Integer.parseInt(parts[2]);
		} catch (RuntimeException e) {
			return;
		}
		if(roster.get(uid) == null) return;   // already gone

		Integer count = peerdownReports.get(uid);
		peerdownReports.put(uid, (count == null) ? 1 : count + 1);
		if(peerdownWindowEnd == 0) {
			peerdownWindowEnd = System.currentTimeMillis() + RoomProtocol.PEERDOWN_WINDOW;
		}
	}

	private void resolvePeerDownWindow() {
		int victim = -1;
		int worst = 0;
		for(Map.Entry<Integer, Integer> e: peerdownReports.entrySet()) {
			if(roster.get(e.getKey()) == null) continue;
			// Most broken links wins; ties go to the higher uid
			if((e.getValue() > worst) || ((e.getValue() == worst) && (e.getKey() > victim))) {
				worst = e.getValue();
				victim = e.getKey();
			}
		}
		peerdownReports.clear();
		peerdownWindowEnd = 0;

		if(victim == -1) return;
		log.info("Kicking partially-connected member uid {}", victim);

		for(RoomRoster.Entry e: roster.linkedMembers()) {
			e.link.sendLine(RoomProtocol.buildKick(victim, "split links"));
		}
		RoomRoster.Entry entry = roster.remove(victim);
		if((entry != null) && (entry.link != null)) entry.link.closeAfterFlush("kicked");
		authority.onMemberGone(victim, false);
	}

	private void onKick(String[] parts) {
		int uid;
		try {
			uid = Integer.parseInt(parts[2]);
		} catch (RuntimeException e) {
			return;
		}
		if(uid == localUid) {
			doShutdown("KICKED");
			return;
		}
		RoomRoster.Entry entry = roster.remove(uid);
		if((entry != null) && (entry.link != null)) entry.link.close("kicked by arbiter");
	}

	private void onTick() {
		if(state == State.CLOSED) return;
		long now = System.currentTimeMillis();

		// Joiner handshake timeout
		if((state == State.CONNECTING || state == State.JOINING)
			&& (now - joinStartedAt > config.joinTimeout) && (joinStartedAt > 0)) {
			doShutdown("JOIN_FAILED:timeout");
			return;
		}

		// Migration claim never arrived: drop the candidate and recompute
		if((state == State.MIGRATING) && (claimDeadline > 0) && (now >= claimDeadline)) {
			RoomRoster.Entry candidate = roster.remove(expectedClaimUid);
			if((candidate != null) && (candidate.link != null)) candidate.link.close("claim timeout");
			beginMigration();
		}

		// Split-mesh arbitration window expired
		if(isArbiter() && (peerdownWindowEnd > 0) && (now >= peerdownWindowEnd)) {
			resolvePeerDownWindow();
		}

		for(RoomRoster.Entry e: roster.linkedMembers()) {
			long idle = now - e.link.lastInboundMillis;
			if(idle > config.linkTimeout) {
				e.link.close("timeout");
			} else if(idle > config.pingInterval) {
				e.link.sendLine(RoomProtocol.LINE_PING);
			}
		}
	}

	/** Package-private: tests sever one direct link (both TCP ends die) */
	void severLinkForTest(int uid) {
		RoomRoster.Entry entry = roster.get(uid);
		if((entry != null) && (entry.link != null)) entry.link.close("test sever");
	}

	/** Package-private: tests sever this peer without a graceful bye */
	void killAbruptly() {
		state = State.CLOSED;
		tickTimer.cancel();
		transport.shutdown();
		queue.add(RoomEvent.tick());   // wake the dispatcher so it observes CLOSED
	}

	private void doShutdown(String reason) {
		if(state == State.CLOSED) return;
		log.info("Mesh session closed: {}", reason);
		setState(State.CLOSED, reason);

		if(announcer != null) {
			announcer.shutdown();
			announcer = null;
		}
		for(RoomRoster.Entry e: roster.linkedMembers()) {
			e.link.sendLine(RoomProtocol.LINE_BYE);
			e.link.closeAfterFlush("bye sent");
		}
		tickTimer.cancel();
		transport.shutdown();

		if(!closedFired) {
			closedFired = true;
			ClosedListener l = closedListener;
			if(l != null) {
				try {
					l.onClosed(reason);
				} catch (Exception e) {
					log.error("Closed listener failed", e);
				}
			}
		}
	}

	private void setState(State newState, String detail) {
		state = newState;
		if(listener != null) {
			try {
				listener.onSessionState(newState, detail);
			} catch (Exception e) {
				log.error("Session listener failed", e);
			}
		}
	}

	// ================================================================ helpers

	/** @return The machine's LAN IPv4 address, or "?" when undeterminable */
	public static String getLanAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while(interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				if(!ni.isUp() || ni.isLoopback()) continue;
				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while(addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if((addr instanceof Inet4Address) && addr.isSiteLocalAddress()) {
						return addr.getHostAddress();
					}
				}
			}
			return InetAddress.getLocalHost().getHostAddress();
		} catch (Exception e) {
			return "?";
		}
	}
}
