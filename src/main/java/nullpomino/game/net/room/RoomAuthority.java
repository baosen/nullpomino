// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net.room;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.zip.Adler32;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetChatMessage;
import nullpomino.game.net.NetMPModeRegistry;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The room/seat/game state machine for a room session - a faithful port of
 * NetServer's session logic (multi-room lobby, seats and FIFO queues, ready
 * and auto-start, shared-seed game start, death places, win detection, race
 * results, chat) over the same {@link NetRoomInfo}/{@link NetPlayerInfo}
 * classes, emitting byte-identical broadcast lines.
 *
 * <p>Runs ONLY on the current arbiter peer, ONLY on the dispatcher thread.
 * All output goes through the {@link Sink} so the class is unit-testable
 * without sockets. Room-scoped lines carry their roomID as scope; lines every
 * client needs (playerupdate/roomupdate/...) are {@link RoomProtocol#SCOPE_GLOBAL}.
 *
 * <p>Differences from NetServer, by design: no rated presets, no bans, no
 * observers, no admin, no lobby-chat private messages, and the same-IP
 * rating restriction is dropped (P2P play is LAN-oriented).
 */
public class RoomAuthority {
	/** Log */
	private static final Logger log = LoggerFactory.getLogger(RoomAuthority.class);

	/** Chat history caps (NetServer defaults) */
	public static final int MAX_LOBBYCHAT_HISTORY = 10;
	public static final int MAX_ROOMCHAT_HISTORY = 10;

	/** Where the authority's output goes. Implementations must preserve the
	 *  relative order of broadcast() and direct() lines per receiver. */
	public interface Sink {
		/**
		 * Authoritative broadcast to every member (including a local loopback
		 * on the arbiter itself).
		 * @param scope roomID the line belongs to, or SCOPE_GLOBAL
		 * @param line verbatim server-to-client line (no newline)
		 * @param exceptUid member to skip (-1 = nobody)
		 */
		void broadcast(int scope, String line, int exceptUid);

		/** Line for exactly one member (or the local client when uid is local) */
		void direct(int uid, String line);

		/** Disseminate a member's compressed rule blob for successor arbiters */
		void ruleCache(int uid, String checksum, String compressedData);

		/** Disseminate a rule-locked room's compressed rule (not part of the room blob) */
		void roomRuleCache(int roomId, String compressedData);

		/** Disseminate a map room's compressed map list (not part of the room blob) */
		void mapCache(int roomId, String compressedData);

		/** Authority state mutated - ship fresh auth frames to all members */
		void authUpdate();
	}

	private final Sink sink;

	/** RNG for seeds and map selection */
	private final Random rand;

	/** Members by uid, in admission order */
	private final LinkedHashMap<Integer, NetPlayerInfo> players = new LinkedHashMap<Integer, NetPlayerInfo>();

	/** All rooms */
	private final LinkedList<NetRoomInfo> roomInfoList = new LinkedList<NetRoomInfo>();

	/** Compressed rule blob per uid (checksum, data) - served to joiners and successors */
	private final Map<Integer, String[]> ruleBlobs = new LinkedHashMap<Integer, String[]>();

	/** Lobby chat history */
	private final LinkedList<NetChatMessage> lobbyChatList = new LinkedList<NetChatMessage>();

	/** Next room ID (roomCount analog) */
	private int nextRoomId = 0;

	/** Next member uid */
	private int nextUid = 0;

	public RoomAuthority(Sink sink, Random rand) {
		this.sink = sink;
		this.rand = rand;
	}

	// ================================================================ membership

	/** @return The uid the next admitted member will get */
	public int reserveUid() {
		return nextUid++;
	}

	/**
	 * Admit a member at session level (before their client logs in).
	 * Broadcasts {@code playernew}.
	 * @param uid uid from {@link #reserveUid()} (or the welcome frame)
	 * @param name Player name
	 * @param host Observed address (shown via the player blob)
	 * @return The created player record
	 */
	public NetPlayerInfo admitMember(int uid, String name, String host) {
		return admitMember(uid, name, host, new int[0], new int[0], new int[0]);
	}

	/**
	 * Admit with the member's self-reported persisted stats (from the hello
	 * frame); styles beyond the provided arrays fall back to the defaults.
	 */
	public NetPlayerInfo admitMember(int uid, String name, String host,
		int[] ratings, int[] playCounts, int[] winCounts)
	{
		NetPlayerInfo pInfo = new NetPlayerInfo();
		pInfo.uid = uid;
		pInfo.strName = name;
		pInfo.strHost = host;
		pInfo.connected = true;
		pInfo.roomID = -1;
		pInfo.seatID = -1;
		pInfo.queueID = -1;
		for(int i = 0; i < pInfo.rating.length; i++) {
			pInfo.rating[i] = (i < ratings.length) ? ratings[i] : RoomRating.RATING_DEFAULT;
			if(i < playCounts.length) pInfo.playCount[i] = playCounts[i];
			if(i < winCounts.length) pInfo.winCount[i] = winCounts[i];
		}
		players.put(uid, pInfo);

		broadcastPlayerInfoUpdate(pInfo, "playernew");
		sink.authUpdate();

		log.info("Room member admitted uid:{} name:{}", uid, name);
		return pInfo;
	}

	/**
	 * A member left (graceful bye or link loss). Ports NetServer's logout
	 * sequence: mid-game death first, then seat/queue/room cleanup, queue
	 * promotion, finish/start re-checks, then {@code playerlogout}.
	 */
	public void onMemberGone(int uid, boolean graceful) {
		NetPlayerInfo pInfo = players.remove(uid);
		ruleBlobs.remove(uid);
		if(pInfo == null) return;

		log.info("Room member gone uid:{} name:{} graceful:{}", uid, pInfo.strName, graceful);

		playerDead(pInfo);
		pInfo.connected = false;
		pInfo.ready = false;

		LinkedList<NetRoomInfo> deleteList = new LinkedList<NetRoomInfo>();
		for(NetRoomInfo roomInfo: roomInfoList) {
			if(roomInfo.playerList.contains(pInfo)) {
				roomInfo.playerList.remove(pInfo);
				roomInfo.playerQueue.remove(pInfo);
				roomInfo.exitSeat(pInfo);
				deleteList.add(roomInfo);
			}
		}
		for(NetRoomInfo roomInfo: deleteList) {
			if(!deleteRoom(roomInfo)) {
				joinAllQueuePlayers(roomInfo);

				if(!gameFinished(roomInfo)) {
					if(!gameStartIfPossible(roomInfo)) {
						autoStartTimerCheck(roomInfo);
						broadcastRoomInfoUpdate(roomInfo);
					}
				}
			}
		}

		broadcastPlayerInfoUpdate(pInfo, "playerlogout");
		pInfo.delete();
		sink.authUpdate();
	}

	// ================================================================ control dispatch

	/**
	 * Handle one verbatim client-to-server line from a member (the arbiter's
	 * own client included). {@code game}/{@code gstat} never arrive here -
	 * they fan out sender-side.
	 */
	public void handleControl(int fromUid, String[] message) {
		NetPlayerInfo pInfo = players.get(fromUid);
		if(pInfo == null) {
			log.debug("Control from unknown uid {}: {}", fromUid, message[0]);
			return;
		}

		if(message[0].equals("ruledata")) { onRuleData(pInfo, message); return; }
		if(message[0].equals("ruleget")) { onRuleGet(pInfo, message); return; }
		if(message[0].equals("lobbychat")) { onLobbyChat(pInfo, message); return; }
		if(message[0].equals("chat")) { onChat(pInfo, message); return; }
		if(message[0].equals("singleroomcreate")) { onSingleRoomCreate(pInfo, message); return; }
		if(message[0].equals("roomcreate")) { onRoomCreate(pInfo, message); return; }
		if(message[0].equals("roomjoin")) { onRoomJoin(pInfo, message); return; }
		if(message[0].equals("changeteam")) { onChangeTeam(pInfo, message); return; }
		if(message[0].equals("changename")) { onChangeName(pInfo, message); return; }
		if(message[0].equals("changestatus")) { onChangeStatus(pInfo, message); return; }
		if(message[0].equals("start1p")) { onStart1P(pInfo); return; }
		if(message[0].equals("ready")) { onReady(pInfo, message); return; }
		if(message[0].equals("autostart")) { onAutoStart(pInfo); return; }
		if(message[0].equals("dead")) { onDead(pInfo, message); return; }
		if(message[0].equals("racewin")) { onRaceWin(pInfo, message); return; }
		if(message[0].equals("reset1p")) { onReset1P(pInfo); return; }

		// Server-only commands (rated presets, rankings, sp*, admin...)
		log.debug("Ignored control command from uid {}: {}", fromUid, message[0]);
	}

	// ---------------------------------------------------------------- rules

	private void onRuleData(NetPlayerInfo pInfo, String[] message) {
		//ruledata\t[ADLER32CHECKSUM]\t[RULEDATA]
		String strData = message[2];

		Adler32 checksumObj = new Adler32();
		checksumObj.update(NetUtil.stringToBytes(strData));
		long sChecksum = checksumObj.getValue();
		long cChecksum = Long.parseLong(message[1]);

		if(sChecksum == cChecksum) {
			String strRuleData = NetUtil.decompressString(strData);

			CustomProperties prop = new CustomProperties();
			prop.decode(strRuleData);
			pInfo.ruleOpt = new RuleOptions();
			pInfo.ruleOpt.readProperty(prop, 0);

			ruleBlobs.put(pInfo.uid, new String[] { String.valueOf(sChecksum), strData });
			sink.ruleCache(pInfo.uid, String.valueOf(sChecksum), strData);
			sink.direct(pInfo.uid, "ruledatasuccess");
		} else {
			sink.direct(pInfo.uid, "ruledatafail\t" + sChecksum);
		}
	}

	private void onRuleGet(NetPlayerInfo pInfo, String[] message) {
		//ruleget\t[UID]
		int uid = Integer.parseInt(message[1]);
		NetPlayerInfo p = players.get(uid);

		if(p != null) {
			if(p.ruleOpt == null) p.ruleOpt = new RuleOptions();

			CustomProperties prop = new CustomProperties();
			p.ruleOpt.writeProperty(prop, 0);
			String strRuleTemp = prop.encode("RuleData " + p.strName);
			String strRuleData = NetUtil.compressString(strRuleTemp);

			Adler32 checksumObj = new Adler32();
			checksumObj.update(NetUtil.stringToBytes(strRuleData));
			long sChecksum = checksumObj.getValue();

			sink.direct(pInfo.uid, "rulegetsuccess\t" + uid + "\t" + sChecksum + "\t" + strRuleData);
		} else {
			sink.direct(pInfo.uid, "rulegetfail\t" + uid);
		}
	}

	// ---------------------------------------------------------------- chat

	private void onLobbyChat(NetPlayerInfo pInfo, String[] message) {
		//lobbychat\t[MESSAGE]  (no /msg private messages in P2P rooms)
		NetChatMessage chat = new NetChatMessage(NetUtil.urlDecode(message[1]), pInfo);
		chat.outputLog();
		lobbyChatList.add(chat);
		while(lobbyChatList.size() > MAX_LOBBYCHAT_HISTORY) lobbyChatList.removeFirst();

		sink.broadcast(RoomProtocol.SCOPE_GLOBAL, "lobbychat\t" + chat.uid + "\t" + NetUtil.urlEncode(chat.strUserName) + "\t" +
			GeneralUtil.exportCalendarString(chat.timestamp) + "\t" + NetUtil.urlEncode(chat.strMessage), -1);
	}

	private void onChat(NetPlayerInfo pInfo, String[] message) {
		//chat\t[MESSAGE]
		if(pInfo.roomID == -1) return;
		NetRoomInfo roomInfo = getRoomInfo(pInfo.roomID);
		if(roomInfo == null) return;

		NetChatMessage chat = new NetChatMessage(NetUtil.urlDecode(message[1]), pInfo, roomInfo);
		chat.outputLog();
		roomInfo.chatList.add(chat);
		while(roomInfo.chatList.size() > MAX_ROOMCHAT_HISTORY) roomInfo.chatList.removeFirst();

		sink.broadcast(pInfo.roomID, "chat\t" + chat.uid + "\t" + NetUtil.urlEncode(chat.strUserName) + "\t" +
			GeneralUtil.exportCalendarString(chat.timestamp) + "\t" + NetUtil.urlEncode(chat.strMessage), -1);
	}

	// ---------------------------------------------------------------- room create/join

	private void onSingleRoomCreate(NetPlayerInfo pInfo, String[] message) {
		//singleroomcreate\t[roomName]\t[mode]\t[rule]
		if(pInfo.roomID != -1) return;

		NetRoomInfo roomInfo = new NetRoomInfo();

		roomInfo.singleplayer = true;
		roomInfo.strMode = NetUtil.urlDecode(message[2]);

		roomInfo.strName = NetUtil.urlDecode(message[1]);
		if(roomInfo.strName.length() < 1) roomInfo.strName = "Single (" + pInfo.strName + ")";

		roomInfo.maxPlayers = 1;

		// No rated rules in P2P rooms: always play on the creator's own rule
		roomInfo.ruleName = pInfo.ruleOpt.strRuleName;
		roomInfo.ruleOpt = new RuleOptions(pInfo.ruleOpt);
		roomInfo.ruleLock = false;
		roomInfo.rated = false;

		roomInfo.roomID = nextRoomId;
		nextRoomId++;
		if(nextRoomId == -1) nextRoomId = 0;

		roomInfoList.add(roomInfo);

		pInfo.roomID = roomInfo.roomID;
		pInfo.resetPlayState();
		pInfo.playCountNow = 0;
		pInfo.winCountNow = 0;

		roomInfo.playerList.add(pInfo);
		pInfo.seatID = roomInfo.joinSeat(pInfo);

		broadcastPlayerInfoUpdate(pInfo);
		broadcastRoomInfoUpdate(roomInfo, "roomcreate");
		sink.direct(pInfo.uid, "roomcreatesuccess\t" + roomInfo.roomID + "\t0\t-1");
		sink.authUpdate();

		log.info("NewSingleRoom ID:{} Title:{}", roomInfo.roomID, roomInfo.strName);
	}

	private void onRoomCreate(NetPlayerInfo pInfo, String[] message) {
		//roomcreate\t[roomName]\t[roomInfoBlob]\t[mode]\t[mapData?]
		if(pInfo.roomID != -1) return;

		String strRoomInfo = NetUtil.urlDecode(message[2]);
		NetRoomInfo roomInfo = new NetRoomInfo(strRoomInfo);

		roomInfo.strName = NetUtil.urlDecode(message[1]);
		if(roomInfo.strName.length() < 1) roomInfo.strName = "No Title";

		if(roomInfo.maxPlayers < 1) roomInfo.maxPlayers = 1;
		if(roomInfo.maxPlayers > 6) roomInfo.maxPlayers = 6;

		if(roomInfo.ruleLock) {
			roomInfo.ruleName = pInfo.ruleOpt.strRuleName;
			roomInfo.ruleOpt = new RuleOptions(pInfo.ruleOpt);
		}

		if(roomInfo.strMode.length() <= 0) {
			roomInfo.strMode = NetUtil.urlDecode(message[3]);
		}

		// Set map
		if(roomInfo.useMap && (message.length > 4)) {
			String strDecompressed = NetUtil.decompressString(message[4]);
			String[] strMaps = strDecompressed.split("\t");

			for(String strMap: strMaps) {
				roomInfo.mapList.add(strMap);
			}

			if(roomInfo.mapList.isEmpty()) {
				log.debug("Room{}: No maps", roomInfo.roomID);
				roomInfo.useMap = false;
			} else {
				log.debug("Room{}: Received {} maps", roomInfo.roomID, roomInfo.mapList.size());
			}
		}

		roomInfo.roomID = nextRoomId;
		nextRoomId++;
		if(nextRoomId == -1) nextRoomId = 0;

		roomInfoList.add(roomInfo);

		pInfo.roomID = roomInfo.roomID;
		pInfo.resetPlayState();
		pInfo.playCountNow = 0;
		pInfo.winCountNow = 0;

		roomInfo.playerList.add(pInfo);
		pInfo.seatID = roomInfo.joinSeat(pInfo);

		// Send rule data if rule-lock is enabled; replicate it session-wide so a
		// successor arbiter can keep serving it (ruleOpt is not in the room blob)
		if(roomInfo.ruleLock) {
			String compressed = compressRule(roomInfo.ruleOpt);
			sink.roomRuleCache(roomInfo.roomID, compressed);
			sink.direct(pInfo.uid, "rulelock\t" + compressed);
		}

		// Replicate maps for the same reason
		if(roomInfo.useMap && !roomInfo.mapList.isEmpty()) {
			sink.mapCache(roomInfo.roomID, NetUtil.compressString(joinMaps(roomInfo)));
		}

		broadcastPlayerInfoUpdate(pInfo);
		broadcastRoomInfoUpdate(roomInfo, "roomcreate");
		sink.direct(pInfo.uid, "roomcreatesuccess\t" + roomInfo.roomID + "\t" + pInfo.seatID + "\t-1");
		sink.authUpdate();

		log.info("NewRoom ID:{} Title:{} RuleLock:{} Map:{} Mode:{}",
			roomInfo.roomID, roomInfo.strName, roomInfo.ruleLock, roomInfo.useMap, roomInfo.strMode);
	}

	private void onRoomJoin(NetPlayerInfo pInfo, String[] message) {
		//roomjoin\t[ROOMID]\t[WATCH]
		int roomID = Integer.parseInt(message[1]);
		boolean watch = Boolean.parseBoolean(message[2]);
		NetRoomInfo prevRoom = getRoomInfo(pInfo.roomID);
		NetRoomInfo newRoom = getRoomInfo(roomID);

		if(roomID < 0) {
			// Return to lobby
			if(prevRoom != null) {
				leavePreviousRoom(pInfo, prevRoom);
			}
			pInfo.roomID = -1;
			pInfo.seatID = -1;
			pInfo.queueID = -1;
			pInfo.resetPlayState();
			pInfo.playCountNow = 0;
			pInfo.winCountNow = 0;

			broadcastPlayerInfoUpdate(pInfo);
			sink.direct(pInfo.uid, "roomjoinsuccess\t-1\t-1\t-1");
			sink.authUpdate();
		} else if(newRoom != null) {
			// Enter a room
			if(prevRoom != null) {
				leavePreviousRoom(pInfo, prevRoom);
			}
			pInfo.roomID = newRoom.roomID;
			pInfo.resetPlayState();
			pInfo.playCountNow = 0;
			pInfo.winCountNow = 0;

			newRoom.playerList.add(pInfo);

			pInfo.seatID = -1;
			if(!watch && !newRoom.singleplayer) {
				pInfo.seatID = newRoom.joinSeat(pInfo);

				if(pInfo.seatID == -1) {
					pInfo.queueID = newRoom.joinQueue(pInfo);
				}
			}

			// Send rule data if rule-lock is enabled
			if(newRoom.ruleLock) {
				sink.direct(pInfo.uid, "rulelock\t" + compressRule(newRoom.ruleOpt));
			}

			// Map send
			if(newRoom.useMap && !newRoom.mapList.isEmpty()) {
				sink.direct(pInfo.uid, "map\t" + NetUtil.compressString(joinMaps(newRoom)));
			}

			sink.broadcast(newRoom.roomID,
				"playerenter\t" + pInfo.uid + "\t" + NetUtil.urlEncode(pInfo.strName) + "\t" + pInfo.seatID,
				pInfo.uid);
			broadcastRoomInfoUpdate(newRoom);
			broadcastPlayerInfoUpdate(pInfo);
			sink.direct(pInfo.uid, "roomjoinsuccess\t" + newRoom.roomID + "\t" + pInfo.seatID + "\t" + pInfo.queueID);

			// Send chat history
			for(NetChatMessage chat: newRoom.chatList) {
				sink.direct(pInfo.uid, "chath\t" + NetUtil.urlEncode(chat.strUserName) + "\t" +
					GeneralUtil.exportCalendarString(chat.timestamp) + "\t" + NetUtil.urlEncode(chat.strMessage));
			}
			sink.authUpdate();
		} else {
			// No such a room
			sink.direct(pInfo.uid, "roomjoinfail");
		}
	}

	/** The leave-previous-room block shared by both roomjoin paths (NetServer 2171-2223) */
	private void leavePreviousRoom(NetPlayerInfo pInfo, NetRoomInfo prevRoom) {
		int seatID = pInfo.seatID;
		sink.broadcast(prevRoom.roomID,
			"playerleave\t" + pInfo.uid + "\t" + NetUtil.urlEncode(pInfo.strName) + "\t" + seatID,
			pInfo.uid);
		playerDead(pInfo);
		pInfo.ready = false;
		prevRoom.exitSeat(pInfo);
		prevRoom.exitQueue(pInfo);
		prevRoom.playerList.remove(pInfo);
		if(!deleteRoom(prevRoom)) {
			joinAllQueuePlayers(prevRoom);

			if(!gameFinished(prevRoom)) {
				if(!gameStartIfPossible(prevRoom)) {
					autoStartTimerCheck(prevRoom);
					broadcastRoomInfoUpdate(prevRoom);
				}
			}
		}
	}

	// ---------------------------------------------------------------- status changes

	private void onChangeTeam(NetPlayerInfo pInfo, String[] message) {
		//changeteam\t[TEAM]
		if(pInfo.playing) return;

		String strTeam = "";
		if(message.length > 1) strTeam = NetUtil.urlDecode(message[1]);

		if(!strTeam.equals(pInfo.strTeam)) {
			pInfo.strTeam = strTeam;
			broadcastPlayerInfoUpdate(pInfo);

			sink.broadcast(pInfo.roomID,
				"changeteam\t" + pInfo.uid + "\t" + NetUtil.urlEncode(pInfo.strName) + "\t" + NetUtil.urlEncode(pInfo.strTeam),
				-1);
			sink.authUpdate();
		}
	}

	private void onChangeName(NetPlayerInfo pInfo, String[] message) {
		//changename\t[NEWNAME]
		if(pInfo.playing) {
			sink.direct(pInfo.uid, "changenamefail\tPLAYING");
			return;
		}
		String newName = (message.length > 1) ? NetUtil.urlDecode(message[1]).trim() : "";
		if(newName.length() == 0) {
			sink.direct(pInfo.uid, "changenamefail\tEMPTY");
			return;
		}
		if(newName.equals(pInfo.strName)) return;   // no-op

		for(NetPlayerInfo p: players.values()) {
			if((p != pInfo) && newName.equals(p.strName)) {
				sink.direct(pInfo.uid, "changenamefail\tDUPLICATE");
				return;
			}
		}

		String oldName = pInfo.strName;
		pInfo.strName = newName;
		// A rename is visible session-wide; the renamer sees its own success
		// through this broadcast too (no dedicated success opcode)
		sink.broadcast(RoomProtocol.SCOPE_GLOBAL,
			"changename\t" + pInfo.uid + "\t" + NetUtil.urlEncode(oldName) + "\t" + NetUtil.urlEncode(newName), -1);
		broadcastPlayerInfoUpdate(pInfo);
		log.info("Player renamed: {} -> {}", oldName, newName);
	}

	private void onChangeStatus(NetPlayerInfo pInfo, String[] message) {
		//changestatus\t[WATCH]
		if(pInfo.playing || (pInfo.roomID == -1)) return;
		NetRoomInfo roomInfo = getRoomInfo(pInfo.roomID);
		if((roomInfo == null) || roomInfo.singleplayer) return;

		boolean watch = Boolean.parseBoolean(message[1]);

		if(watch) {
			// Change to spectator
			int prevSeatID = pInfo.seatID;
			roomInfo.exitSeat(pInfo);
			roomInfo.exitQueue(pInfo);
			pInfo.ready = false;
			pInfo.seatID = -1;
			pInfo.queueID = -1;
			sink.broadcast(pInfo.roomID,
				"changestatus\twatchonly\t" + pInfo.uid + "\t" + NetUtil.urlEncode(pInfo.strName) + "\t" + prevSeatID,
				-1);

			joinAllQueuePlayers(roomInfo);	// Let the queue-player to join
		} else {
			// Change to player
			if(roomInfo.canJoinSeat()) {
				pInfo.seatID = roomInfo.joinSeat(pInfo);
				pInfo.queueID = -1;
				pInfo.ready = false;
				sink.broadcast(pInfo.roomID,
					"changestatus\tjoinseat\t" + pInfo.uid + "\t" + NetUtil.urlEncode(pInfo.strName) + "\t" + pInfo.seatID,
					-1);
			} else {
				pInfo.seatID = -1;
				pInfo.queueID = roomInfo.joinQueue(pInfo);
				pInfo.ready = false;
				sink.broadcast(pInfo.roomID,
					"changestatus\tjoinqueue\t" + pInfo.uid + "\t" + NetUtil.urlEncode(pInfo.strName) + "\t" + pInfo.queueID,
					-1);
			}
		}
		broadcastPlayerInfoUpdate(pInfo);
		if(!gameStartIfPossible(roomInfo)) {
			autoStartTimerCheck(roomInfo);
		}
		broadcastRoomInfoUpdate(roomInfo);
		sink.authUpdate();
	}

	// ---------------------------------------------------------------- game lifecycle

	private void onStart1P(NetPlayerInfo pInfo) {
		NetRoomInfo roomInfo = getRoomInfo(pInfo.roomID);
		if(roomInfo == null) return;
		int seat = roomInfo.getPlayerSeatNumber(pInfo);

		if((seat != -1) && (roomInfo.singleplayer)) {
			gameStart(roomInfo);
			sink.authUpdate();
		}
	}

	private void onReady(NetPlayerInfo pInfo, String[] message) {
		//ready\t[STATE]
		NetRoomInfo roomInfo = getRoomInfo(pInfo.roomID);
		if(roomInfo == null) return;
		int seat = roomInfo.getPlayerSeatNumber(pInfo);

		if((seat != -1) && (!roomInfo.singleplayer)) {
			pInfo.ready = Boolean.parseBoolean(message[1]);
			broadcastPlayerInfoUpdate(pInfo);

			if(!pInfo.ready) roomInfo.isSomeoneCancelled = true;

			// Start a game if possible
			if(!gameStartIfPossible(roomInfo)) {
				autoStartTimerCheck(roomInfo);
			}
			sink.authUpdate();
		}
	}

	private void onAutoStart(NetPlayerInfo pInfo) {
		NetRoomInfo roomInfo = getRoomInfo(pInfo.roomID);
		if(roomInfo == null) return;
		int seat = roomInfo.getPlayerSeatNumber(pInfo);

		if((seat != -1) && (roomInfo.autoStartActive) && (!roomInfo.singleplayer)) {
			if(roomInfo.autoStartTNET2) {
				// Move all non-ready players to spectators
				LinkedList<NetPlayerInfo> pList = new LinkedList<NetPlayerInfo>();
				pList.addAll(roomInfo.playerSeat);

				for(NetPlayerInfo p: pList) {
					if((p != null) && (!p.ready)) {
						int prevSeatID = p.seatID;
						roomInfo.exitSeat(p);
						roomInfo.exitQueue(p);
						p.ready = false;
						p.seatID = -1;
						p.queueID = -1;
						sink.broadcast(p.roomID,
							"changestatus\twatchonly\t" + p.uid + "\t" + NetUtil.urlEncode(p.strName) + "\t" + prevSeatID,
							-1);
					}
				}

				joinAllQueuePlayers(roomInfo);
			}

			gameStart(roomInfo);
			sink.authUpdate();
		}
	}

	private void onDead(NetPlayerInfo pInfo, String[] message) {
		if(message.length > 1) {
			int koUID = Integer.parseInt(message[1]);
			NetPlayerInfo koPlayerInfo = players.get(koUID);
			playerDead(pInfo, koPlayerInfo);
		} else {
			playerDead(pInfo);
		}
		sink.authUpdate();
	}

	private void onRaceWin(NetPlayerInfo pInfo, String[] message) {
		if((pInfo.roomID == -1) || (pInfo.seatID == -1)) return;
		NetRoomInfo roomInfo = getRoomInfo(pInfo.roomID);
		if(roomInfo == null) return;

		if(roomInfo.playing && isRaceMode(roomInfo.style, roomInfo.strMode)) {
			for(int i = message.length - 1; i > 1; i--) {
				int koUID = Integer.parseInt(message[i]);
				if(koUID != pInfo.uid) {
					NetPlayerInfo koPlayerInfo = players.get(koUID);

					if((koPlayerInfo != null) && (koPlayerInfo.roomID == roomInfo.roomID)) {
						playerDead(koPlayerInfo, pInfo);
					}
				}
			}
			sink.authUpdate();
		}
	}

	private void onReset1P(NetPlayerInfo pInfo) {
		NetRoomInfo roomInfo = getRoomInfo(pInfo.roomID);
		if(roomInfo == null) return;
		int seat = roomInfo.getPlayerSeatNumber(pInfo);

		if(seat != -1) {
			pInfo.resetPlayState();
			broadcastPlayerInfoUpdate(pInfo);
			gameFinished(roomInfo);
			sink.broadcast(roomInfo.roomID, "reset1p", pInfo.uid);
			sink.authUpdate();
		}
	}

	// ================================================================ ported private helpers (NetServer 3048-3436)

	private void autoStartTimerCheck(NetRoomInfo roomInfo) {
		if(roomInfo.autoStartSeconds <= 0) return;

		int minPlayers = (roomInfo.autoStartTNET2) ? 2 : 1;

		// Stop
		if((roomInfo.getNumberOfPlayerSeated() <= 1) ||
		   (roomInfo.isSomeoneCancelled && roomInfo.disableTimerAfterSomeoneCancelled) ||
		   (roomInfo.getHowManyPlayersReady() < minPlayers) || (roomInfo.getHowManyPlayersReady() < roomInfo.getNumberOfPlayerSeated() / 2))
		{
			if(roomInfo.autoStartActive == true) {
				sink.broadcast(roomInfo.roomID, "autostartstop", -1);
			}
			roomInfo.autoStartActive = false;
		}
		// Start
		else if((roomInfo.autoStartActive == false) &&
				(!roomInfo.isSomeoneCancelled || !roomInfo.disableTimerAfterSomeoneCancelled) &&
				(roomInfo.getHowManyPlayersReady() >= minPlayers) && (roomInfo.getHowManyPlayersReady() >= roomInfo.getNumberOfPlayerSeated() / 2))
		{
			sink.broadcast(roomInfo.roomID, "autostartbegin\t" + roomInfo.autoStartSeconds, -1);
			roomInfo.autoStartActive = true;
		}

		// Turn-off ready status if there is only 1 player
		if(roomInfo.getNumberOfPlayerSeated() == 1) {
			for(NetPlayerInfo p: roomInfo.playerSeat) {
				if((p != null) && (p.ready == true)) {
					p.ready = false;
					broadcastPlayerInfoUpdate(p);
				}
			}
		}
	}

	private boolean gameStartIfPossible(NetRoomInfo roomInfo) {
		if(roomInfo == null) return false;
		if((roomInfo.getHowManyPlayersReady() == roomInfo.getNumberOfPlayerSeated()) && (roomInfo.getNumberOfPlayerSeated() >= 2)) {
			gameStart(roomInfo);
			return true;
		}
		return false;
	}

	private void gameStart(NetRoomInfo roomInfo) {
		if(roomInfo == null) return;
		if(roomInfo.getNumberOfPlayerSeated() <= 0) return;
		if((roomInfo.getNumberOfPlayerSeated() <= 1) && (!roomInfo.singleplayer)) return;
		if(roomInfo.playing) return;

		roomInfo.gameStart();

		int mapNo = 0;
		int mapMax = roomInfo.mapList.size();
		if(roomInfo.useMap && (mapMax > 0)) {
			do {
				mapNo = rand.nextInt(mapMax);
			} while ((mapNo == roomInfo.mapPrevious) && (mapMax >= 2));

			roomInfo.mapPrevious = mapNo;
		}
		sink.broadcast(roomInfo.roomID,
			"start\t" + Long.toString(rand.nextLong(), 16) + "\t" + roomInfo.startPlayers + "\t" + mapNo, -1);

		for(NetPlayerInfo p: roomInfo.playerSeat) {
			if(p != null) {
				p.ready = false;
				p.playing = true;
				p.playCountNow++;

				// If ranked room
				if(isRatedGame(roomInfo)) {
					p.playCount[roomInfo.style]++;
					p.ratingBefore[roomInfo.style] = p.rating[roomInfo.style];
				}

				broadcastPlayerInfoUpdate(p);
			}
		}

		roomInfo.playing = true;
		roomInfo.autoStartActive = false;
		broadcastRoomInfoUpdate(roomInfo);
	}

	private boolean gameFinished(NetRoomInfo roomInfo) {
		int startPlayers = roomInfo.startPlayers;
		int nowPlaying = roomInfo.getHowManyPlayersPlaying();
		boolean isTeamWin = roomInfo.isTeamWin();

		if((roomInfo.playing) && ((nowPlaying < 1) || ((startPlayers >= 2) && (nowPlaying < 2)) || (isTeamWin))) {
			// Game finished
			NetPlayerInfo winner = roomInfo.getWinner();
			String msg = "finish\t";

			if(isTeamWin) {
				// Winner is a team
				String teamName = roomInfo.getWinnerTeam();
				if(teamName == null) teamName = "";
				msg += -1 + "\t" + -1 + "\t" + NetUtil.urlEncode(teamName) + "\t" + isTeamWin;

				for(NetPlayerInfo pInfo: roomInfo.playerSeat) {
					if((pInfo != null) && (pInfo.playing)) {
						pInfo.resetPlayState();
						pInfo.winCountNow++;
						broadcastPlayerInfoUpdate(pInfo);
						roomInfo.playerSeatDead.addFirst(pInfo);
					}
				}

			} else if((winner != null) && !roomInfo.singleplayer) {
				// Winner is a player
				roomInfo.playerSeatDead.addFirst(winner);

				// Rated game: pairwise ELO over the final placement order
				// (winner at the front of playerSeatDead), NetServer 3218-3251.
				// Persistence is per-peer, driven by these rating broadcasts.
				if(isRatedGame(roomInfo)) {
					winner.winCount[roomInfo.style]++;

					int style = roomInfo.style;
					int n = roomInfo.playerSeatDead.size();
					for(int w = 0; w < n - 1; w++) {
						for(int l = w + 1; l < n; l++) {
							NetPlayerInfo wp = roomInfo.playerSeatDead.get(w);
							NetPlayerInfo lp = roomInfo.playerSeatDead.get(l);

							wp.rating[style] += (int) (RoomRating.rankDelta(wp.playCount[style], wp.rating[style], lp.rating[style], 1) / (n-1));
							lp.rating[style] += (int) (RoomRating.rankDelta(lp.playCount[style], lp.rating[style], wp.rating[style], 0) / (n-1));

							if(wp.rating[style] < RoomRating.RATING_MIN) wp.rating[style] = RoomRating.RATING_MIN;
							if(lp.rating[style] < RoomRating.RATING_MIN) lp.rating[style] = RoomRating.RATING_MIN;
							if(wp.rating[style] > RoomRating.RATING_MAX) wp.rating[style] = RoomRating.RATING_MAX;
							if(lp.rating[style] > RoomRating.RATING_MAX) lp.rating[style] = RoomRating.RATING_MAX;
						}
					}

					for(int i = 0; i < n; i++) {
						NetPlayerInfo p = roomInfo.playerSeatDead.get(i);
						int change = p.rating[style] - p.ratingBefore[style];
						sink.broadcast(roomInfo.roomID,
							"rating\t" + p.uid + "\t" + p.seatID + "\t" + NetUtil.urlEncode(p.strName) + "\t" +
							p.rating[style] + "\t" + change, -1);
					}
				}

				msg += winner.uid + "\t" + winner.seatID + "\t" + NetUtil.urlEncode(winner.strName) + "\t" + isTeamWin;
				winner.resetPlayState();
				winner.winCountNow++;
				broadcastPlayerInfoUpdate(winner);
			} else {
				// No winner(s)
				msg += -1 + "\t" + -1 + "\t" + "" + "\t" + isTeamWin;
			}
			sink.broadcast(roomInfo.roomID, msg, -1);

			roomInfo.playing = false;
			roomInfo.autoStartActive = false;
			broadcastRoomInfoUpdate(roomInfo);

			return true;
		}

		return false;
	}

	private boolean deleteRoom(NetRoomInfo roomInfo) {
		if((roomInfo != null) && (roomInfo.playerList.isEmpty())) {
			log.info("RoomDelete ID:{} Title:{}", roomInfo.roomID, roomInfo.strName);
			broadcastRoomInfoUpdate(roomInfo, "roomdelete");
			roomInfoList.remove(roomInfo);
			roomInfo.delete();
			return true;
		}
		return false;
	}

	private int joinAllQueuePlayers(NetRoomInfo roomInfo) {
		int playerJoinedCount = 0;

		while(roomInfo.canJoinSeat() && !roomInfo.playerQueue.isEmpty()) {
			NetPlayerInfo pInfo = roomInfo.playerQueue.poll();
			pInfo.seatID = roomInfo.joinSeat(pInfo);
			pInfo.queueID = -1;
			pInfo.ready = false;
			sink.broadcast(pInfo.roomID,
				"changestatus\tjoinseat\t" + pInfo.uid + "\t" + NetUtil.urlEncode(pInfo.strName) + "\t" + pInfo.seatID,
				-1);
			broadcastPlayerInfoUpdate(pInfo);
			playerJoinedCount++;
		}

		if(playerJoinedCount > 0) broadcastRoomInfoUpdate(roomInfo);

		return playerJoinedCount;
	}

	private void playerDead(NetPlayerInfo pInfo) {
		playerDead(pInfo, null);
	}

	private void playerDead(NetPlayerInfo pInfo, NetPlayerInfo pKOInfo) {
		NetRoomInfo roomInfo = getRoomInfo(pInfo.roomID);

		if((roomInfo != null) && (pInfo.seatID != -1) && (pInfo.playing) && (roomInfo.playing)) {
			pInfo.resetPlayState();

			int place = roomInfo.startPlayers - roomInfo.deadCount;
			String msg = "dead\t" + pInfo.uid + "\t" + NetUtil.urlEncode(pInfo.strName) + "\t" + pInfo.seatID + "\t" + place + "\t";
			if(pKOInfo == null) {
				msg += -1 + "\t" + "";
			} else {
				msg += pKOInfo.uid + "\t" + NetUtil.urlEncode(pKOInfo.strName);
			}
			sink.broadcast(pInfo.roomID, msg, -1);

			roomInfo.deadCount++;
			roomInfo.playerSeatDead.addFirst(pInfo);
			gameFinished(roomInfo);

			broadcastPlayerInfoUpdate(pInfo);
		}
	}

	private void broadcastPlayerInfoUpdate(NetPlayerInfo pInfo) {
		broadcastPlayerInfoUpdate(pInfo, "playerupdate");
	}

	private void broadcastPlayerInfoUpdate(NetPlayerInfo pInfo, String command) {
		sink.broadcast(RoomProtocol.SCOPE_GLOBAL, command + "\t" + pInfo.exportString(), -1);
	}

	private void broadcastRoomInfoUpdate(NetRoomInfo roomInfo) {
		broadcastRoomInfoUpdate(roomInfo, "roomupdate");
	}

	private void broadcastRoomInfoUpdate(NetRoomInfo roomInfo, String command) {
		roomInfo.updatePlayerCount();
		sink.broadcast(RoomProtocol.SCOPE_GLOBAL, command + "\t" + roomInfo.exportString(), -1);
	}

	// ================================================================ queries (session layer + snapshots)

	public NetRoomInfo getRoomInfo(int roomID) {
		if(roomID < 0) return null;
		for(NetRoomInfo roomInfo: roomInfoList) {
			if(roomID == roomInfo.roomID) return roomInfo;
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
		return roomInfoList;
	}

	public Map<Integer, String[]> getRuleBlobs() {
		return ruleBlobs;
	}

	public LinkedList<NetChatMessage> getLobbyChatList() {
		return lobbyChatList;
	}

	public int getNextUid() {
		return nextUid;
	}

	public int getNextRoomId() {
		return nextRoomId;
	}

	/** Restore counters when a successor arbiter adopts mirrored state */
	public void restoreCounters(int nextUid, int nextRoomId) {
		this.nextUid = nextUid;
		this.nextRoomId = nextRoomId;
	}

	/** Adopt mirrored state wholesale (successor arbiter); counters via restoreCounters */
	public void adoptState(Map<Integer, NetPlayerInfo> players, LinkedList<NetRoomInfo> rooms,
		Map<Integer, String[]> rules)
	{
		this.players.clear();
		this.players.putAll(players);
		this.roomInfoList.clear();
		this.roomInfoList.addAll(rooms);
		this.ruleBlobs.clear();
		this.ruleBlobs.putAll(rules);
	}

	/**
	 * Re-baseline every peer after a migration: fresh roomupdate/playerupdate
	 * broadcasts plus auth frames heal any per-link divergence left behind by
	 * the old arbiter's independent write queues.
	 */
	public void resyncAll() {
		for(NetRoomInfo roomInfo: roomInfoList) {
			broadcastRoomInfoUpdate(roomInfo);
		}
		for(NetPlayerInfo pInfo: players.values()) {
			broadcastPlayerInfoUpdate(pInfo);
		}
		sink.authUpdate();
	}

	/** Build an AuthRoom snapshot of a room's non-derivable state */
	public static RoomProtocol.AuthRoom buildAuthRoom(long seq, NetRoomInfo roomInfo) {
		return new RoomProtocol.AuthRoom(seq, roomInfo.roomID, roomInfo.playing,
			roomInfo.startPlayers, roomInfo.deadCount, roomInfo.autoStartActive,
			roomInfo.isSomeoneCancelled, roomInfo.mapPrevious,
			uidsOf(roomInfo.playerSeat), uidsOf(roomInfo.playerSeatNowPlaying),
			uidsOf(roomInfo.playerSeatDead), uidsOf(roomInfo.playerQueue));
	}

	private static int[] uidsOf(java.util.List<NetPlayerInfo> list) {
		int[] uids = new int[list.size()];
		for(int i = 0; i < uids.length; i++) {
			NetPlayerInfo p = list.get(i);
			uids[i] = (p == null) ? -1 : p.uid;
		}
		return uids;
	}

	// ================================================================ small helpers

	/** Rated iff the room says so and it's not a team game. The server's same-IP
	 *  restriction is dropped: P2P play is LAN-oriented by design. */
	private static boolean isRatedGame(NetRoomInfo roomInfo) {
		return roomInfo.rated && !roomInfo.isTeamGame();
	}

	private static boolean isRaceMode(int style, String modeName) {
		for(NetMPModeRegistry.Entry entry: NetMPModeRegistry.forStyle(style)) {
			if(entry.name().equals(modeName)) return entry.isRace();
		}
		return false;
	}

	private static String compressRule(RuleOptions ruleOpt) {
		CustomProperties prop = new CustomProperties();
		ruleOpt.writeProperty(prop, 0);
		return NetUtil.compressString(prop.encode("RuleData"));
	}

	/** Tab-join a room's map list (the wire format of the "map" message payload) */
	private static String joinMaps(NetRoomInfo roomInfo) {
		StringBuilder sb = new StringBuilder();
		int maxMap = roomInfo.mapList.size();
		for(int i = 0; i < maxMap; i++) {
			sb.append(roomInfo.mapList.get(i));
			if(i < maxMap - 1) sb.append('\t');
		}
		return sb.toString();
	}
}
