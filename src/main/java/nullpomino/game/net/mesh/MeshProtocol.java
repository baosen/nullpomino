// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.mesh;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import nullpomino.game.net.NetUtil;

/**
 * Frame formats for the P2P mesh netplay protocol.
 *
 * <p>Every line on a mesh link is newline-terminated, tab-delimited UTF-8.
 * Lines starting with {@code mesh\t} are mesh-internal control frames (this
 * class owns all their builders and parsers); {@code game\t...} and
 * {@code gstat\t...} lines pass through pre-stamped with the sender's
 * uid/seat and are NEVER re-tokenized (the {@code game\tattack} line has a
 * historic empty field that re-joining would destroy). Wrapped payloads
 * (control/broadcast/direct) are therefore extracted by substring, not by
 * split-and-join.
 *
 * <p>Builders return lines WITHOUT the trailing newline; the link layer
 * appends it on send.
 */
public final class MeshProtocol {
	/** Default TCP listen port for mesh peer links (9200=old server, 9201=discovery UDP) */
	public static final int DEFAULT_PORT = 9202;

	/** Interval between liveness pings on an idle link (ms) */
	public static final int PING_INTERVAL = 5000;

	/** A link with no inbound traffic for this long is dead (ms) */
	public static final int LINK_TIMEOUT = 30000;

	/** Give up on a join (handshake + peer dials) after this long (ms) */
	public static final int JOIN_TIMEOUT = 10000;

	/** Wait this long for an arbiter-migration claim before skipping the candidate (ms) */
	public static final int CLAIM_TIMEOUT = 5000;

	/** Aggregation window for peerdown reports before kicking (ms) */
	public static final int PEERDOWN_WINDOW = 2000;

	/** A link whose outbound queue exceeds this many lines is declared dead */
	public static final int WRITE_QUEUE_MAX = 4096;

	/** Scope value on a broadcast frame meaning "not room-scoped" */
	public static final int SCOPE_GLOBAL = -1;

	// Frame prefixes (first two tab fields)
	public static final String PREFIX = "mesh\t";
	private static final String CONTROL_PREFIX = "mesh\tc\t";
	private static final String DIRECT_PREFIX = "mesh\td\t";
	private static final String BROADCAST_PREFIX = "mesh\tb\t";

	// Complete frames with no arguments
	public static final String LINE_PING = "mesh\tping";
	public static final String LINE_PONG = "mesh\tpong";
	public static final String LINE_MESHOK = "mesh\tmeshok";
	public static final String LINE_BYE = "mesh\tbye";
	public static final String LINE_SNAPEND = "mesh\tsnapend";

	// Deny reasons
	public static final String DENY_DIFFERENT_VERSION = "DIFFERENT_VERSION";
	public static final String DENY_DIFFERENT_BUILD = "DIFFERENT_BUILD";
	public static final String DENY_BAD_TOKEN = "BAD_TOKEN";
	public static final String DENY_DUPLICATE = "DUPLICATE";
	public static final String DENY_SHUTDOWN = "SHUTDOWN";

	private MeshProtocol() {}

	/** @return true if the line is a mesh-internal control frame */
	public static boolean isMeshFrame(String line) {
		return line.startsWith(PREFIX);
	}

	/** @return 32 zero-padded hex chars identifying a session */
	public static String generateToken(Random rand) {
		return String.format("%016x%016x", rand.nextLong(), rand.nextLong());
	}

	// ------------------------------------------------------------------ handshake

	/** One member in the welcome roster */
	public static final class RosterEntry {
		public final int uid;
		public final String host;
		public final int listenPort;
		public final String name;

		public RosterEntry(int uid, String host, int listenPort, String name) {
			this.uid = uid;
			this.host = host;
			this.listenPort = listenPort;
			this.name = name;
		}

		String export() {
			return uid + ";" + host + ";" + listenPort + ";" + NetUtil.urlEncode(name);
		}

		static RosterEntry importString(String str) {
			String[] f = str.split(";");
			return new RosterEntry(Integer.parseInt(f[0]), f[1], Integer.parseInt(f[2]),
				NetUtil.urlDecode(f[3]));
		}
	}

	public static String buildHelloJoin(float verMajor, boolean devBuild, int listenPort, String name) {
		return "mesh\thello\tjoin\t" + verMajor + "\t" + devBuild + "\t" + listenPort + "\t"
			+ NetUtil.urlEncode(name);
	}

	/** Parsed {@code hello} frame (join variant has uid/token unset) */
	public static final class Hello {
		public final boolean joinVariant;
		public final float verMajor;
		public final boolean devBuild;
		public final String token;   // peer variant only, else ""
		public final int uid;        // peer variant only, else -1
		public final int listenPort;
		public final String name;    // join variant only, else ""

		Hello(boolean joinVariant, float verMajor, boolean devBuild, String token, int uid,
			int listenPort, String name)
		{
			this.joinVariant = joinVariant;
			this.verMajor = verMajor;
			this.devBuild = devBuild;
			this.token = token;
			this.uid = uid;
			this.listenPort = listenPort;
			this.name = name;
		}
	}

	public static String buildHelloPeer(float verMajor, boolean devBuild, String token, int uid,
		int listenPort)
	{
		return "mesh\thello\tpeer\t" + verMajor + "\t" + devBuild + "\t" + token + "\t" + uid
			+ "\t" + listenPort;
	}

	/** @return parsed hello, or null if malformed */
	public static Hello parseHello(String[] parts) {
		try {
			if(parts.length >= 7 && "join".equals(parts[2])) {
				return new Hello(true, Float.parseFloat(parts[3]), Boolean.parseBoolean(parts[4]),
					"", -1, Integer.parseInt(parts[5]), NetUtil.urlDecode(parts[6]));
			}
			if(parts.length >= 8 && "peer".equals(parts[2])) {
				return new Hello(false, Float.parseFloat(parts[3]), Boolean.parseBoolean(parts[4]),
					parts[5], Integer.parseInt(parts[6]), Integer.parseInt(parts[7]), "");
			}
			return null;
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/** Parsed {@code welcome} frame */
	public static final class Welcome {
		public final String token;
		public final int uid;
		public final String name;
		public final int arbiterUid;
		public final long lastSeq;
		public final List<RosterEntry> roster;

		Welcome(String token, int uid, String name, int arbiterUid, long lastSeq,
			List<RosterEntry> roster)
		{
			this.token = token;
			this.uid = uid;
			this.name = name;
			this.arbiterUid = arbiterUid;
			this.lastSeq = lastSeq;
			this.roster = roster;
		}
	}

	public static String buildWelcome(String token, int uid, String name, int arbiterUid,
		long lastSeq, List<RosterEntry> roster)
	{
		StringBuilder sb = new StringBuilder("mesh\twelcome\t");
		sb.append(token).append('\t').append(uid).append('\t').append(NetUtil.urlEncode(name));
		sb.append('\t').append(arbiterUid).append('\t').append(lastSeq).append('\t').append(roster.size());
		for(RosterEntry entry: roster) sb.append('\t').append(entry.export());
		return sb.toString();
	}

	/** @return parsed welcome, or null if malformed */
	public static Welcome parseWelcome(String[] parts) {
		try {
			int count = Integer.parseInt(parts[7]);
			if(parts.length < 8 + count) return null;
			List<RosterEntry> roster = new ArrayList<RosterEntry>();
			for(int i = 0; i < count; i++) roster.add(RosterEntry.importString(parts[8 + i]));
			return new Welcome(parts[2], Integer.parseInt(parts[3]), NetUtil.urlDecode(parts[4]),
				Integer.parseInt(parts[5]), Long.parseLong(parts[6]), roster);
		} catch (RuntimeException e) {
			return null;
		}
	}

	public static String buildPeerOk(int uid) {
		return "mesh\tpeerok\t" + uid;
	}

	public static String buildDeny(String reason, String detail) {
		return "mesh\tdeny\t" + reason + "\t" + NetUtil.urlEncode(detail);
	}

	public static String buildPeerAnnounce(int uid, String host, int listenPort, String name) {
		return "mesh\tpeer\t" + uid + "\t" + host + "\t" + listenPort + "\t" + NetUtil.urlEncode(name);
	}

	/** @return parsed peer announce as a RosterEntry, or null if malformed */
	public static RosterEntry parsePeerAnnounce(String[] parts) {
		try {
			return new RosterEntry(Integer.parseInt(parts[2]), parts[3], Integer.parseInt(parts[4]),
				NetUtil.urlDecode(parts[5]));
		} catch (RuntimeException e) {
			return null;
		}
	}

	// ------------------------------------------------------------------ session traffic

	public static String wrapControl(String line) {
		return CONTROL_PREFIX + line;
	}

	/** @return the verbatim client-to-server line inside a control frame, or null */
	public static String unwrapControl(String rawLine) {
		if(!rawLine.startsWith(CONTROL_PREFIX)) return null;
		return rawLine.substring(CONTROL_PREFIX.length());
	}

	public static String wrapBroadcast(long seq, int scope, String line) {
		return BROADCAST_PREFIX + seq + "\t" + scope + "\t" + line;
	}

	/** Parsed broadcast frame; payload is the verbatim server-to-client line */
	public static final class Broadcast {
		public final long seq;
		public final int scope;
		public final String payload;

		Broadcast(long seq, int scope, String payload) {
			this.seq = seq;
			this.scope = scope;
			this.payload = payload;
		}
	}

	/** @return parsed broadcast (payload extracted by substring, tabs preserved), or null */
	public static Broadcast parseBroadcast(String rawLine) {
		if(!rawLine.startsWith(BROADCAST_PREFIX)) return null;
		int seqStart = BROADCAST_PREFIX.length();
		int seqEnd = rawLine.indexOf('\t', seqStart);
		if(seqEnd < 0) return null;
		int scopeEnd = rawLine.indexOf('\t', seqEnd + 1);
		if(scopeEnd < 0) return null;
		try {
			return new Broadcast(Long.parseLong(rawLine.substring(seqStart, seqEnd)),
				Integer.parseInt(rawLine.substring(seqEnd + 1, scopeEnd)),
				rawLine.substring(scopeEnd + 1));
		} catch (NumberFormatException e) {
			return null;
		}
	}

	public static String wrapDirect(String line) {
		return DIRECT_PREFIX + line;
	}

	/** @return the verbatim server-to-client line inside a direct frame, or null */
	public static String unwrapDirect(String rawLine) {
		if(!rawLine.startsWith(DIRECT_PREFIX)) return null;
		return rawLine.substring(DIRECT_PREFIX.length());
	}

	// ------------------------------------------------------------------ authority extras

	public static String buildAuthGlobal(long seq, int nextUid, int nextRoomId) {
		return "mesh\tauthg\t" + seq + "\t" + nextUid + "\t" + nextRoomId;
	}

	/** Parsed {@code authg} frame */
	public static final class AuthGlobal {
		public final long seq;
		public final int nextUid;
		public final int nextRoomId;

		AuthGlobal(long seq, int nextUid, int nextRoomId) {
			this.seq = seq;
			this.nextUid = nextUid;
			this.nextRoomId = nextRoomId;
		}
	}

	/** @return parsed authg, or null if malformed */
	public static AuthGlobal parseAuthGlobal(String[] parts) {
		try {
			return new AuthGlobal(Long.parseLong(parts[2]), Integer.parseInt(parts[3]),
				Integer.parseInt(parts[4]));
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** Per-room authority extras not derivable from the standard broadcasts */
	public static final class AuthRoom {
		public final long seq;
		public final int roomId;
		public final boolean playing;
		public final int startPlayers;
		public final int deadCount;
		public final boolean autoStartActive;
		public final boolean isSomeoneCancelled;
		public final int mapPrevious;
		public final int[] seatUids;        // seat slot order, -1 = empty slot
		public final int[] nowPlayingUids;  // playerSeatNowPlaying order, -1 = null entry
		public final int[] deadUids;        // playerSeatDead front-to-back
		public final int[] queueUids;       // playerQueue FIFO order

		public AuthRoom(long seq, int roomId, boolean playing, int startPlayers, int deadCount,
			boolean autoStartActive, boolean isSomeoneCancelled, int mapPrevious,
			int[] seatUids, int[] nowPlayingUids, int[] deadUids, int[] queueUids)
		{
			this.seq = seq;
			this.roomId = roomId;
			this.playing = playing;
			this.startPlayers = startPlayers;
			this.deadCount = deadCount;
			this.autoStartActive = autoStartActive;
			this.isSomeoneCancelled = isSomeoneCancelled;
			this.mapPrevious = mapPrevious;
			this.seatUids = seatUids;
			this.nowPlayingUids = nowPlayingUids;
			this.deadUids = deadUids;
			this.queueUids = queueUids;
		}
	}

	public static String buildAuthRoom(AuthRoom a) {
		return "mesh\tauthr\t" + a.seq + "\t" + a.roomId + "\t" + a.playing + "\t"
			+ a.startPlayers + "\t" + a.deadCount + "\t" + a.autoStartActive + "\t"
			+ a.isSomeoneCancelled + "\t" + a.mapPrevious + "\t" + joinUids(a.seatUids) + "\t"
			+ joinUids(a.nowPlayingUids) + "\t" + joinUids(a.deadUids) + "\t" + joinUids(a.queueUids);
	}

	/** @return parsed authr, or null if malformed */
	public static AuthRoom parseAuthRoom(String[] parts) {
		try {
			if(parts.length < 14) return null;
			return new AuthRoom(Long.parseLong(parts[2]), Integer.parseInt(parts[3]),
				Boolean.parseBoolean(parts[4]), Integer.parseInt(parts[5]), Integer.parseInt(parts[6]),
				Boolean.parseBoolean(parts[7]), Boolean.parseBoolean(parts[8]), Integer.parseInt(parts[9]),
				parseUids(parts[10]), parseUids(parts[11]), parseUids(parts[12]), parseUids(parts[13]));
		} catch (RuntimeException e) {
			return null;
		}
	}

	/** ";"-join a uid list; empty list = "" */
	public static String joinUids(int[] uids) {
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < uids.length; i++) {
			if(i > 0) sb.append(';');
			sb.append(uids[i]);
		}
		return sb.toString();
	}

	/** Parse a ";"-joined uid list; "" = empty */
	public static int[] parseUids(String str) {
		if(str.length() == 0) return new int[0];
		String[] f = str.split(";");
		int[] uids = new int[f.length];
		for(int i = 0; i < f.length; i++) uids[i] = Integer.parseInt(f[i]);
		return uids;
	}

	// ------------------------------------------------------------------ rule cache / liveness / failure

	public static String buildRuleCache(int uid, String checksum, String compressedData) {
		return "mesh\trule\t" + uid + "\t" + checksum + "\t" + compressedData;
	}

	public static String buildPeerDown(int uid) {
		return "mesh\tpeerdown\t" + uid;
	}

	public static String buildKick(int uid, String reason) {
		return "mesh\tkick\t" + uid + "\t" + NetUtil.urlEncode(reason);
	}

	public static String buildArbiterClaim(int uid, long lastSeq) {
		return "mesh\tarbiter\t" + uid + "\t" + lastSeq;
	}

	// ------------------------------------------------------------------ snapshot frames

	public static String buildSnapRoom(int roomId, String roomBlob) {
		return "mesh\tsnap\troom\t" + roomId + "\t" + roomBlob;
	}

	public static String buildSnapPlayer(String playerBlob) {
		return "mesh\tsnap\tplayer\t" + playerBlob;
	}

	public static String buildSnapRule(int uid, String checksum, String compressedData) {
		return "mesh\tsnap\trule\t" + uid + "\t" + checksum + "\t" + compressedData;
	}

	public static String buildSnapRoomRule(int roomId, String compressedData) {
		return "mesh\tsnap\troomrule\t" + roomId + "\t" + compressedData;
	}

	public static String buildSnapMap(int roomId, String compressedData) {
		return "mesh\tsnap\tmap\t" + roomId + "\t" + compressedData;
	}

	/** chatLine is a pre-encoded NetChatMessage export (no raw tabs) */
	public static String buildSnapChat(int roomId, String chatLine) {
		return "mesh\tsnap\tchat\t" + roomId + "\t" + chatLine;
	}

	public static String buildSnapLobbyChat(String chatLine) {
		return "mesh\tsnap\tlobbychat\t" + chatLine;
	}
}
