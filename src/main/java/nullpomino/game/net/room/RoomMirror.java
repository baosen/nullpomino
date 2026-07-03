// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetChatMessage;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.util.CustomProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Passive replica of the authority state, maintained on EVERY peer (arbiter
 * included) from the broadcast stream, the auth-extras frames, and the cache
 * frames. When the arbiter disappears, {@link #promote()} turns the replica
 * into live objects a fresh {@link RoomAuthority} can adopt losslessly.
 *
 * <p>All methods are dispatcher-confined. Application is idempotent and
 * order-tolerant within one seq: auth frames overwrite wholesale.
 */
public class RoomMirror {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(RoomMirror.class);

	/** Players by uid (identity preserved across updates) */
	private final LinkedHashMap<Integer, NetPlayerInfo> players = new LinkedHashMap<Integer, NetPlayerInfo>();

	/** Rooms (identity preserved across updates) */
	private final LinkedList<NetRoomInfo> rooms = new LinkedList<NetRoomInfo>();

	/** Compressed rule blob per uid (checksum, data) */
	private final Map<Integer, String[]> ruleBlobs = new LinkedHashMap<Integer, String[]>();

	/** Compressed room rule per roomID (rule-locked rooms) */
	private final Map<Integer, String> roomRuleBlobs = new LinkedHashMap<Integer, String>();

	/** Compressed map list per roomID (map rooms) */
	private final Map<Integer, String> mapBlobs = new LinkedHashMap<Integer, String>();

	/** Lobby chat history */
	private final LinkedList<NetChatMessage> lobbyChatList = new LinkedList<NetChatMessage>();

	/** Highest broadcast seq seen */
	private long seq = 0;

	/** Global authority counters (from authg frames) */
	private int nextUid = 0;
	private int nextRoomId = 0;

	// ================================================================ application

	/**
	 * Apply one authoritative broadcast line.
	 * @param seq Frame sequence number
	 * @param scope roomID scope of the line (SCOPE_GLOBAL for global lines)
	 * @param payload Verbatim server-to-client line
	 */
	public void applyBroadcast(long seq, int scope, String payload) {
		if(seq > this.seq) this.seq = seq;
		String[] m = payload.split("\t", -1);

		if(m[0].equals("playernew") || m[0].equals("playerupdate")) {
			upsertPlayer(m[1]);
		} else if(m[0].equals("playerlogout")) {
			NetPlayerInfo gone = new NetPlayerInfo(m[1]);
			players.remove(gone.uid);
			ruleBlobs.remove(gone.uid);
		} else if(m[0].equals("roomcreate") || m[0].equals("roomupdate")) {
			upsertRoom(m[1]);
		} else if(m[0].equals("roomdelete")) {
			NetRoomInfo gone = new NetRoomInfo(m[1]);
			NetRoomInfo existing = getRoom(gone.roomID);
			if(existing != null) rooms.remove(existing);
			roomRuleBlobs.remove(gone.roomID);
			mapBlobs.remove(gone.roomID);
		} else if(m[0].equals("chat")) {
			NetRoomInfo room = getRoom(scope);
			if(room != null) {
				room.chatList.add(chatFromBroadcast(m, scope));
				while(room.chatList.size() > RoomAuthority.MAX_ROOMCHAT_HISTORY) room.chatList.removeFirst();
			}
		} else if(m[0].equals("lobbychat")) {
			lobbyChatList.add(chatFromBroadcast(m, -1));
			while(lobbyChatList.size() > RoomAuthority.MAX_LOBBYCHAT_HISTORY) lobbyChatList.removeFirst();
		}
		// start/dead/finish/changestatus/... need no mirror action: the auth
		// frames that follow every mutation carry the resulting state
	}

	/** Apply a global auth-extras frame */
	public void applyAuthGlobal(RoomProtocol.AuthGlobal g) {
		if(g.seq > this.seq) this.seq = g.seq;
		this.nextUid = g.nextUid;
		this.nextRoomId = g.nextRoomId;
	}

	/** Apply a per-room auth-extras frame (wholesale overwrite) */
	public void applyAuthRoom(RoomProtocol.AuthRoom a) {
		if(a.seq > this.seq) this.seq = a.seq;
		NetRoomInfo room = getRoom(a.roomId);
		if(room == null) {
			log.debug("authr for unknown room {}", a.roomId);
			return;
		}

		room.playing = a.playing;
		room.startPlayers = a.startPlayers;
		room.deadCount = a.deadCount;
		room.autoStartActive = a.autoStartActive;
		room.isSomeoneCancelled = a.isSomeoneCancelled;
		room.mapPrevious = a.mapPrevious;

		room.playerSeat.clear();
		for(int uid: a.seatUids) room.playerSeat.add(players.get(uid));
		room.playerSeatNowPlaying.clear();
		for(int uid: a.nowPlayingUids) room.playerSeatNowPlaying.add(players.get(uid));
		room.playerSeatDead.clear();
		for(int uid: a.deadUids) {
			NetPlayerInfo p = players.get(uid);
			if(p != null) room.playerSeatDead.add(p);
		}
		room.playerQueue.clear();
		for(int uid: a.queueUids) {
			NetPlayerInfo p = players.get(uid);
			if(p != null) room.playerQueue.add(p);
		}
	}

	/**
	 * Apply one snapshot/cache frame ({@code room\tsnap\t...}). Valid both
	 * during the join handshake and live (rule/map dissemination).
	 */
	public void applySnapshot(String[] parts) {
		if(parts.length < 3) return;
		String kind = parts[2];

		if(kind.equals("player") && parts.length > 3) {
			upsertPlayer(parts[3]);
		} else if(kind.equals("room") && parts.length > 4) {
			upsertRoom(parts[4]);
		} else if(kind.equals("rule") && parts.length > 5) {
			cacheRule(Integer.parseInt(parts[3]), parts[4], parts[5]);
		} else if(kind.equals("roomrule") && parts.length > 4) {
			roomRuleBlobs.put(Integer.parseInt(parts[3]), parts[4]);
		} else if(kind.equals("map") && parts.length > 4) {
			mapBlobs.put(Integer.parseInt(parts[3]), parts[4]);
		} else if(kind.equals("chat") && parts.length > 4) {
			NetRoomInfo room = getRoom(Integer.parseInt(parts[3]));
			if(room != null) {
				NetChatMessage chat = new NetChatMessage();
				chat.importString(parts[4]);
				room.chatList.add(chat);
			}
		} else if(kind.equals("lobbychat") && parts.length > 3) {
			NetChatMessage chat = new NetChatMessage();
			chat.importString(parts[3]);
			lobbyChatList.add(chat);
		}
	}

	public void cacheRule(int uid, String checksum, String compressedData) {
		ruleBlobs.put(uid, new String[] { checksum, compressedData });
	}

	public void cacheRoomRule(int roomId, String compressedData) {
		roomRuleBlobs.put(roomId, compressedData);
	}

	public void cacheMap(int roomId, String compressedData) {
		mapBlobs.put(roomId, compressedData);
	}

	// ================================================================ promotion

	/**
	 * Prepare the replica for adoption by a successor {@link RoomAuthority}:
	 * rebuild each room's playerList (uid order - deterministic on every
	 * peer), and restore the objects that travel only as compressed caches
	 * (player rules, room rules, map lists).
	 */
	public void promote() {
		for(NetRoomInfo room: rooms) {
			room.playerList.clear();
			ArrayList<NetPlayerInfo> inRoom = new ArrayList<NetPlayerInfo>();
			for(NetPlayerInfo p: players.values()) {
				if(p.roomID == room.roomID) inRoom.add(p);
			}
			Collections.sort(inRoom, new Comparator<NetPlayerInfo>() {
				public int compare(NetPlayerInfo a, NetPlayerInfo b) {
					return Integer.compare(a.uid, b.uid);
				}
			});
			room.playerList.addAll(inRoom);
			room.updatePlayerCount();

			String roomRule = roomRuleBlobs.get(room.roomID);
			if(roomRule != null) room.ruleOpt = decompressRule(roomRule);

			String maps = mapBlobs.get(room.roomID);
			if(maps != null) {
				room.mapList.clear();
				String[] strMaps = NetUtil.decompressString(maps).split("\t");
				for(String strMap: strMaps) room.mapList.add(strMap);
			}
		}

		for(Map.Entry<Integer, String[]> entry: ruleBlobs.entrySet()) {
			NetPlayerInfo p = players.get(entry.getKey());
			if(p != null) p.ruleOpt = decompressRule(entry.getValue()[1]);
		}
	}

	private static RuleOptions decompressRule(String compressedData) {
		CustomProperties prop = new CustomProperties();
		prop.decode(NetUtil.decompressString(compressedData));
		RuleOptions rule = new RuleOptions();
		rule.readProperty(prop, 0);
		return rule;
	}

	// ================================================================ queries

	public NetRoomInfo getRoom(int roomID) {
		if(roomID < 0) return null;
		for(NetRoomInfo room: rooms) {
			if(room.roomID == roomID) return room;
		}
		return null;
	}

	public NetPlayerInfo getPlayer(int uid) {
		return players.get(uid);
	}

	public Map<Integer, NetPlayerInfo> getPlayers() {
		return players;
	}

	public LinkedList<NetRoomInfo> getRooms() {
		return rooms;
	}

	public Map<Integer, String[]> getRuleBlobs() {
		return ruleBlobs;
	}

	public Map<Integer, String> getRoomRuleBlobs() {
		return roomRuleBlobs;
	}

	public Map<Integer, String> getMapBlobs() {
		return mapBlobs;
	}

	public LinkedList<NetChatMessage> getLobbyChatList() {
		return lobbyChatList;
	}

	public long getSeq() {
		return seq;
	}

	public int getNextUid() {
		return nextUid;
	}

	public int getNextRoomId() {
		return nextRoomId;
	}

	// ================================================================ internals

	private void upsertPlayer(String blob) {
		NetPlayerInfo incoming = new NetPlayerInfo(blob);
		NetPlayerInfo existing = players.get(incoming.uid);
		if(existing != null) {
			existing.importString(blob);
		} else {
			players.put(incoming.uid, incoming);
		}
	}

	private void upsertRoom(String blob) {
		NetRoomInfo incoming = new NetRoomInfo(blob);
		NetRoomInfo existing = getRoom(incoming.roomID);
		if(existing != null) {
			existing.importString(blob);
		} else {
			rooms.add(incoming);
		}
	}

	private static NetChatMessage chatFromBroadcast(String[] m, int roomID) {
		// chat/lobbychat\t[uid]\t[nameEnc]\t[calendar]\t[msgEnc]
		NetChatMessage chat = new NetChatMessage(NetUtil.urlDecode(m[4]));
		chat.uid = Integer.parseInt(m[1]);
		chat.strUserName = NetUtil.urlDecode(m[2]);
		chat.timestamp = nullpomino.util.GeneralUtil.importCalendarString(m[3]);
		chat.roomID = roomID;
		return chat;
	}
}
