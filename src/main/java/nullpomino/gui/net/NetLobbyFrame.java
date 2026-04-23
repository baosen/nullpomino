/*
    Copyright (c) 2010, NullNoname
    All rights reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright
          notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright
          notice, this list of conditions and the following disclaimer in the
          documentation and/or other materials provided with the distribution.
        * Neither the name of NullNoname nor the names of its
          contributors may be used to endorse or promote products derived from
          this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
    AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
    IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
    LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
    CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
    SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
    INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
    CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
    ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGE.
*/
package nullpomino.gui.net;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.Adler32;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.net.NetBaseClient;
import nullpomino.game.net.NetMessageListener;
import nullpomino.game.net.NetPlayerClient;
import nullpomino.game.net.NetPlayerInfo;
import nullpomino.game.net.NetRoomInfo;
import nullpomino.game.net.NetUtil;
import nullpomino.game.play.GameEngine;
import nullpomino.game.play.GameManager;
import nullpomino.game.subsystem.mode.NetDummyMode;
import nullpomino.gui.sdl.NormalFontSDL;
import nullpomino.gui.sdl.widget.ChatLogSDL;
import nullpomino.util.CustomProperties;
import nullpomino.util.GeneralUtil;

import org.apache.log4j.Logger;

/**
 * NullpoMino NetLobby — session and protocol holder used by the SDL3 netplay UI states.
 *
 * Historically this was a Swing {@code JFrame} that rendered the whole lobby UI
 * itself.  The SDL rewrite keeps the class name + package so that {@code NetDummyMode}
 * and the ~17 game-mode subclasses that reference {@code NetLobbyFrame} continue to
 * compile unchanged, but all UI responsibility has moved out: the concrete screens
 * live in {@code nullpomino.gui.sdl.StateNet*SDL} and read/mutate this object via
 * its public fields and helper methods.
 *
 * Threading model:
 *   {@link #netOnMessage} runs on the reader thread inside {@link NetBaseClient};
 *   it queues the raw message.  The SDL game thread calls {@link #pump()} once per
 *   frame which drains the queue and runs {@link #dispatchMessage} + any
 *   {@link NetLobbyListener} callbacks on a single thread.  This removes the latent
 *   race that existed in the Swing version (tables mutated from the network thread).
 */
public class NetLobbyFrame implements NetMessageListener {
	/** Lobby-mode flag used by states to decide which button set to enable. */
	public static final int LOBBYMODE_DISCONNECTED = 0;
	public static final int LOBBYMODE_LOBBY = 1;
	public static final int LOBBYMODE_INROOM = 2;

	/** Log */
	public static final Logger log = Logger.getLogger(NetLobbyFrame.class);

	// ---------------- Public session data (game modes read these) ----------------

	/** NetPlayerClient — protocol transport. */
	public NetPlayerClient netPlayerClient;

	/** Rule data sent to the server for the current player. */
	public RuleOptions ruleOptPlayer;

	/** Rule data received from a rule-locked room. */
	public RuleOptions ruleOptLock;

	/** Map list received from the server for map-enabled rooms. */
	public LinkedList<String> mapList = new LinkedList<String>();

	// ---------------- Lobby state ----------------

	/** Lobby chat log (buffer + renderable). */
	public final ChatLogSDL chatLogLobby = new ChatLogSDL(0, 0, 0, 0);

	/** Room chat log (buffer + renderable). */
	public final ChatLogSDL chatLogRoom = new ChatLogSDL(0, 0, 0, 0);

	/** Known rooms (kept in order received). Mutated on the game thread from dispatchMessage. */
	public final LinkedList<NetRoomInfo> roomList = new LinkedList<NetRoomInfo>();

	/** Rated-room preset list for the currently-selected rule style. */
	public final LinkedList<NetRoomInfo> presets = new LinkedList<NetRoomInfo>();

	/** Rated-rule name list per style (indexed by game style). Populated by {@code rulelist} messages. */
	@SuppressWarnings("unchecked")
	public final LinkedList<String>[] listRatedRuleName = new LinkedList[GameEngine.MAX_GAMESTYLE];

	/** Rule catalog (all .rul files in config/rule/) for the rule-change screen. */
	public final LinkedList<RuleEntry> ruleEntries = new LinkedList<RuleEntry>();

	/**
	 * Per-style multiplayer ranking rows decoded from server {@code mpranking}
	 * messages.  Each row is [rank, name, rating, playCount, winCount].
	 */
	public final String[][][] mpRankingRows = new String[GameEngine.MAX_GAMESTYLE][][];

	/** Where the local player sits in each style's ranking; -1 if unranked. */
	public final int[] mpRankingMyRank = new int[GameEngine.MAX_GAMESTYLE];

	/** Set to true by {@code dispatchMessage} whenever a new mpranking response arrives. */
	public volatile boolean mpRankingDirty;

	/** Server list for the server-select screen. Edited via {@link #saveServerList()}. */
	public final LinkedList<String> serverList = new LinkedList<String>();

	/** Current high-level lobby mode — read by SDL states to enable/disable widgets. */
	public int lobbyMode = LOBBYMODE_DISCONNECTED;

	/** Create Room form state — persisted across invocations. {@code null} until first open. */
	public NetRoomInfo backupRoomInfo;

	/** Which flavour of room the create-room form will build on OK. */
	public enum RoomCreateMode { MULTIPLAYER, SINGLE_PLAYER, RATED }

	/** Current selection in the create-room form's MODE TYPE dropdown. */
	public RoomCreateMode createRoomMode = RoomCreateMode.MULTIPLAYER;

	/** Game style for the active rated-room form (0-3). */
	public int createRoomStyle;

	/** Set to true when a {@code ratedpresets} message has been received and {@link #presets} is fresh. */
	public volatile boolean presetsDirty;

	/**
	 * Wall-clock time of the most recent {@link #connectToServer} call.
	 * States that guard on {@code netPlayerClient.isConnected()} honour a short
	 * grace window after this timestamp so mid-flight reconnects (e.g. /name
	 * command) don't bounce the user back to server-select while the socket
	 * finishes its handshake.
	 */
	public volatile long lastConnectAt;

	/** ID of room being viewed in detail-view mode; -1 when creating a brand-new room. */
	public int currentViewDetailRoomID = -1;

	/**
	 * Raw (un-hashed) form of our current player name — whatever the user typed,
	 * including any {@code #tripkey} suffix. Seeded by {@link #connectToServer}
	 * and updated on our own {@code changename} broadcast. Used to persist the
	 * original trip key to config instead of the server's hashed form so that
	 * next-session login re-derives the same tripcode.
	 */
	private String lastRawOwnName = "";

	/**
	 * Raw form of a pending {@code /name} request. Set in {@link #sendChangeName}
	 * and consumed by the {@code changename} broadcast handler (or cleared by
	 * {@code changenamefail}). Includes any preserved {@code #tripkey} suffix
	 * that the server would merge in so the saved identity matches exactly what
	 * the next login would reconstruct.
	 */
	private String pendingOwnRaw;

	// ---------------- Callbacks ----------------

	/** Registered NetLobbyListeners (StateNetGameSDL is one). */
	protected LinkedList<NetLobbyListener> listeners = new LinkedList<NetLobbyListener>();

	/** Active game mode (special listener that survives lobby tab switches). */
	protected NetDummyMode netDummyMode;

	/** Player-list filter for players in the same room (refilled by {@link #updateSameRoomPlayerInfoList}). */
	protected LinkedList<NetPlayerInfo> sameRoomPlayerInfoList = new LinkedList<NetPlayerInfo>();

	// ---------------- Message pump ----------------

	protected final ConcurrentLinkedQueue<String[]> pendingMessages = new ConcurrentLinkedQueue<String[]>();
	protected volatile boolean pendingDisconnect;
	protected volatile Throwable pendingDisconnectEx;

	// ---------------- Config / localization ----------------

	public CustomProperties propConfig;
	public CustomProperties propGlobal;
	public CustomProperties propObserver;
	protected CustomProperties propDefaultModeDesc;
	protected CustomProperties propModeDesc;
	protected CustomProperties propLangDefault;
	protected CustomProperties propLang;

	// ---------------- Lifecycle ----------------

	public NetLobbyFrame() {
		for(int i = 0; i < listRatedRuleName.length; i++) {
			listRatedRuleName[i] = new LinkedList<String>();
		}
		for(int i = 0; i < mpRankingMyRank.length; i++) mpRankingMyRank[i] = -1;
	}

	/** Initialize config, localization, rule list, server list. Safe to call once. */
	public void init() {
		propConfig = loadPropsOrEmpty("config/setting/netlobby.cfg");
		propGlobal = loadPropsOrEmpty("config/setting/global.cfg");
		propObserver = loadPropsOrEmpty("config/setting/netobserver.cfg");
		propDefaultModeDesc = loadPropsOrEmpty("config/lang/modedesc_default.properties");
		propModeDesc = loadPropsOrEmpty("config/lang/modedesc_" + Locale.getDefault().getCountry() + ".properties");
		propLangDefault = loadPropsOrEmpty("config/lang/netlobby_default.properties");
		propLang = loadPropsOrEmpty("config/lang/netlobby_" + Locale.getDefault().getCountry() + ".properties");

		// Chat buffers: larger capacity so history packets from the server don't evict live chat.
		chatLogLobby.capacity = 600;
		chatLogRoom.capacity = 600;

		// Rule file catalog
		String[] ruleFiles = getRuleFileList();
		if(ruleFiles != null) createRuleEntries(ruleFiles);
		else log.error("Rule file directory (config/rule) not found");

		// Server list — prefer the user-edited file, fall back to the bundled default
		loadServerList();

		// Fire init callbacks
		for(NetLobbyListener l : listeners) if(l != null) l.netlobbyOnInit(this);
		if(netDummyMode != null) netDummyMode.netlobbyOnInit(this);
	}

	/** Clean shutdown: save config, disconnect, fire exit callbacks. */
	public void shutdown() {
		saveConfig();

		if(netPlayerClient != null) {
			if(netPlayerClient.isConnected()) netPlayerClient.send("disconnect\n");
			netPlayerClient.threadRunning = false;
			netPlayerClient.interrupt();
			netPlayerClient = null;
		}

		if(listeners != null) {
			for(NetLobbyListener l : listeners) l.netlobbyOnExit(this);
			listeners = null;
		}
		if(netDummyMode != null) {
			netDummyMode.netlobbyOnExit(this);
			netDummyMode = null;
		}
	}

	/**
	 * Drain pending network messages from the reader thread onto the game thread.
	 * Call once per SDL frame from each lobby state's {@code update()}.
	 */
	public void pump() {
		String[] msg;
		while((msg = pendingMessages.poll()) != null) {
			try {
				dispatchMessage(msg);
			} catch(IOException e) {
				log.error("Exception while dispatching net message " + msg[0], e);
			} catch(Throwable t) {
				log.error("Unexpected error in dispatchMessage " + msg[0], t);
			}
		}
		if(pendingDisconnect) {
			pendingDisconnect = false;
			Throwable ex = pendingDisconnectEx;
			pendingDisconnectEx = null;
			try {
				dispatchDisconnect(ex);
			} catch(Throwable t) {
				log.error("Error in disconnect handler", t);
			}
		}
	}

	// ---------------- NetMessageListener (reader thread) ----------------

	@Override
	public void netOnMessage(NetBaseClient client, String[] message) throws IOException {
		pendingMessages.offer(message);
	}

	@Override
	public void netOnDisconnect(NetBaseClient client, Throwable ex) {
		pendingDisconnectEx = ex;
		pendingDisconnect = true;
	}

	// ---------------- Dispatch (game thread) ----------------

	/**
	 * Core protocol dispatch — called from {@link #pump()} after the reader thread
	 * has queued a message.  Mirrors the old {@code netOnMessage} switch but writes
	 * into chat buffers and plain collections instead of Swing models.
	 */
	protected void dispatchMessage(String[] message) throws IOException {
		if(message.length == 0) return;
		String cmd = message[0];

		if("welcome".equals(cmd)) {
			chatLogLobby.appendSystem(String.format(getUIText("SysMsg_ServerConnected"),
					netPlayerClient.getHost(), netPlayerClient.getPort()), NormalFontSDL.COLOR_BLUE);
			if(message.length > 1) chatLogLobby.appendSystem(getUIText("SysMsg_ServerVersion") + message[1], NormalFontSDL.COLOR_BLUE);
			if(message.length > 2) chatLogLobby.appendSystem(getUIText("SysMsg_NumberOfPlayers") + message[2], NormalFontSDL.COLOR_BLUE);

		} else if("loginsuccess".equals(cmd)) {
			chatLogLobby.appendSystem(getUIText("SysMsg_LoginOK"), NormalFontSDL.COLOR_BLUE);
			if(message.length > 1) {
				chatLogLobby.appendSystem(getUIText("SysMsg_YourNickname") + convTripCode(NetUtil.urlDecode(message[1])),
						NormalFontSDL.COLOR_BLUE);
			}
			chatLogLobby.appendSystem(getUIText("SysMsg_YourUID") + netPlayerClient.getPlayerUID(), NormalFontSDL.COLOR_BLUE);
			chatLogLobby.appendSystem(getUIText("SysMsg_SendRuleDataStart"), NormalFontSDL.COLOR_BLUE);
			sendMyRuleDataToServer();

		} else if("loginfail".equals(cmd)) {
			lobbyMode = LOBBYMODE_DISCONNECTED;
			String reason;
			if(message.length > 1 && "DIFFERENT_VERSION".equals(message[1])) {
				String strClientVer = String.valueOf(GameManager.getVersionMajor());
				String strServerVer = message.length > 2 ? message[2] : "?";
				reason = String.format(getUIText("SysMsg_LoginFailDifferentVersion"), strClientVer, strServerVer);
			} else if(message.length > 1 && "DIFFERENT_BUILD".equals(message[1])) {
				String strClientBuildType = GameManager.getBuildTypeString();
				String strServerBuildType = message.length > 2 ? message[2] : "?";
				reason = String.format(getUIText("SysMsg_LoginFailDifferentBuild"), strClientBuildType, strServerBuildType);
			} else {
				StringBuilder sb = new StringBuilder(getUIText("SysMsg_LoginFail"));
				for(int i = 1; i < message.length; i++) { sb.append(message[i]); sb.append(' '); }
				reason = sb.toString();
			}
			chatLogLobby.appendSystem(reason, NormalFontSDL.COLOR_RED);

		} else if("banned".equals(cmd)) {
			lobbyMode = LOBBYMODE_DISCONNECTED;
			Calendar cStart = message.length > 1 ? GeneralUtil.importCalendarString(message[1]) : null;
			Calendar cExpire = (message.length > 2 && message[2].length() > 0) ? GeneralUtil.importCalendarString(message[2]) : null;
			String strStart = cStart != null ? GeneralUtil.getCalendarString(cStart) : "???";
			String strExpire = cExpire != null ? GeneralUtil.getCalendarString(cExpire) : getUIText("SysMsg_Banned_Permanent");
			chatLogLobby.appendSystem(String.format(getUIText("SysMsg_Banned"), strStart, strExpire), NormalFontSDL.COLOR_RED);

		} else if("ruledatasuccess".equals(cmd)) {
			chatLogLobby.appendSystem(getUIText("SysMsg_SendRuleDataOK"), NormalFontSDL.COLOR_BLUE);
			lobbyMode = LOBBYMODE_LOBBY;
			for(NetLobbyListener l : listeners) l.netlobbyOnLoginOK(this, netPlayerClient);
			if(netDummyMode != null) netDummyMode.netlobbyOnLoginOK(this, netPlayerClient);

		} else if("ruledatafail".equals(cmd)) {
			sendMyRuleDataToServer();

		} else if("rulelock".equals(cmd) && message.length > 1) {
			if(ruleOptLock == null) ruleOptLock = new RuleOptions();
			String strRuleData = NetUtil.decompressString(message[1]);
			CustomProperties prop = new CustomProperties();
			prop.decode(strRuleData);
			ruleOptLock.readProperty(prop, 0);
			log.info("Received rule data (" + ruleOptLock.strRuleName + ")");

		} else if("rulelist".equals(cmd) && message.length >= 2) {
			int style = Integer.parseInt(message[1]);
			if(style >= 0 && style < listRatedRuleName.length) {
				listRatedRuleName[style].clear();
				for(int i = 2; i < message.length; i++) {
					listRatedRuleName[style].add(NetUtil.urlDecode(message[i]));
				}
			}

		} else if("playerlist".equals(cmd) || "playerupdate".equals(cmd)
				|| "playernew".equals(cmd) || "playerlogout".equals(cmd)) {
			// No list mutation needed — SDL states read netPlayerClient.getPlayerInfoList() live.
			if("playerlogout".equals(cmd) && message.length > 1) {
				NetPlayerInfo p = new NetPlayerInfo(message[1]);
				NetPlayerInfo me = netPlayerClient.getYourPlayerInfo();
				if(me != null && p.roomID == me.roomID) {
					chatLogRoom.appendSystem(formatLeaveRoom(p), NormalFontSDL.COLOR_BLUE);
				}
			}

		} else if("playerenter".equals(cmd) && message.length > 1) {
			int uid = Integer.parseInt(message[1]);
			NetPlayerInfo pInfo = netPlayerClient.getPlayerInfoByUID(uid);
			if(pInfo != null) chatLogRoom.appendSystem(formatEnterRoom(pInfo), NormalFontSDL.COLOR_BLUE);

		} else if("playerleave".equals(cmd) && message.length > 1) {
			int uid = Integer.parseInt(message[1]);
			NetPlayerInfo pInfo = netPlayerClient.getPlayerInfoByUID(uid);
			if(pInfo != null) chatLogRoom.appendSystem(formatLeaveRoom(pInfo), NormalFontSDL.COLOR_BLUE);

		} else if("changeteam".equals(cmd) && message.length > 1) {
			int uid = Integer.parseInt(message[1]);
			NetPlayerInfo pInfo = netPlayerClient.getPlayerInfoByUID(uid);
			if(pInfo != null) {
				String text;
				if(message.length > 3) {
					String strTeam = NetUtil.urlDecode(message[3]);
					text = String.format(getUIText("SysMsg_ChangeTeam"), getPlayerNameWithTripCode(pInfo), strTeam);
				} else {
					text = String.format(getUIText("SysMsg_ChangeTeam_None"), getPlayerNameWithTripCode(pInfo));
				}
				ChatLogSDL target = (lobbyMode == LOBBYMODE_INROOM) ? chatLogRoom : chatLogLobby;
				// Green to match /name success — both are user-initiated command
				// responses and should share the same visual acknowledgment.
				target.appendSystem(text, NormalFontSDL.COLOR_GREEN);
			}

		} else if("roomlist".equals(cmd) && message.length >= 2) {
			int size = Integer.parseInt(message[1]);
			roomList.clear();
			for(int i = 0; i < size && i + 2 < message.length; i++) {
				roomList.add(new NetRoomInfo(message[2 + i]));
			}

		} else if("ratedpresets".equals(cmd)) {
			presets.clear();
			if(message.length > 1) {
				for(int i = 1; i < message.length; i++) {
					String preset = NetUtil.decompressString(message[i]);
					presets.add(new NetRoomInfo(preset));
				}
			}
			presetsDirty = true;

		} else if("roomcreate".equals(cmd) && message.length > 1) {
			roomList.add(new NetRoomInfo(message[1]));

		} else if("roomupdate".equals(cmd) && message.length > 1) {
			NetRoomInfo updated = new NetRoomInfo(message[1]);
			for(int i = 0; i < roomList.size(); i++) {
				if(roomList.get(i).roomID == updated.roomID) {
					roomList.set(i, updated);
					break;
				}
			}

		} else if("roomdelete".equals(cmd) && message.length > 1) {
			NetRoomInfo deleted = new NetRoomInfo(message[1]);
			for(int i = 0; i < roomList.size(); i++) {
				if(roomList.get(i).roomID == deleted.roomID) {
					roomList.remove(i);
					break;
				}
			}

		} else if(("roomcreatesuccess".equals(cmd) || "roomjoinsuccess".equals(cmd)) && message.length >= 4) {
			int roomID = Integer.parseInt(message[1]);
			int seatID = Integer.parseInt(message[2]);
			int queueID = Integer.parseInt(message[3]);

			NetPlayerInfo myInfo = netPlayerClient.getYourPlayerInfo();
			if(myInfo != null) {
				myInfo.roomID = roomID;
				myInfo.seatID = seatID;
				myInfo.queueID = queueID;
			}

			if(roomID != -1) {
				NetRoomInfo roomInfo = netPlayerClient.getRoomInfo(roomID);
				if(roomInfo != null && myInfo != null) {
					String fmt;
					if(seatID == -1 && queueID == -1)       fmt = getUIText("SysMsg_StatusChange_Spectator");
					else if(seatID == -1)                    fmt = getUIText("SysMsg_StatusChange_Queue");
					else                                     fmt = getUIText("SysMsg_StatusChange_Joined");
					chatLogRoom.appendSystem(String.format(fmt, getPlayerNameWithTripCode(myInfo)), NormalFontSDL.COLOR_BLUE);
					chatLogRoom.appendSystem(getUIText("SysMsg_RoomJoin_Title") + roomInfo.strName, NormalFontSDL.COLOR_BLUE);
					chatLogRoom.appendSystem(getUIText("SysMsg_RoomJoin_ID") + roomInfo.roomID, NormalFontSDL.COLOR_BLUE);
					if(roomInfo.ruleLock) {
						chatLogRoom.appendSystem(getUIText("SysMsg_RoomJoin_Rule") + roomInfo.ruleName, NormalFontSDL.COLOR_BLUE);
					}
					lobbyMode = LOBBYMODE_INROOM;
					for(NetLobbyListener l : listeners) l.netlobbyOnRoomJoin(this, netPlayerClient, roomInfo);
					if(netDummyMode != null) netDummyMode.netlobbyOnRoomJoin(this, netPlayerClient, roomInfo);
				}
			} else {
				chatLogRoom.appendSystem(getUIText("SysMsg_RoomJoin_Lobby"), NormalFontSDL.COLOR_BLUE);
				lobbyMode = LOBBYMODE_LOBBY;
				for(NetLobbyListener l : listeners) l.netlobbyOnRoomLeave(this, netPlayerClient);
				if(netDummyMode != null) netDummyMode.netlobbyOnRoomLeave(this, netPlayerClient);
			}

		} else if("roomjoinfail".equals(cmd)) {
			chatLogRoom.appendSystem(getUIText("SysMsg_RoomJoinFail"), NormalFontSDL.COLOR_RED);

		} else if("roomkicked".equals(cmd) && message.length > 3) {
			String strKickMsg = String.format(getUIText("SysMsg_Kicked_" + message[1]),
					NetUtil.urlDecode(message[3]), message[2]);
			chatLogLobby.appendSystem(strKickMsg, NormalFontSDL.COLOR_RED);

		} else if("map".equals(cmd) && message.length > 1) {
			String decompressed = NetUtil.decompressString(message[1]);
			String[] strMaps = decompressed.split("\t");
			mapList.clear();
			for(String m : strMaps) mapList.add(m);
			log.debug("Received " + mapList.size() + " maps");

		} else if("lobbychat".equals(cmd) && message.length > 4) {
			int uid = Integer.parseInt(message[1]);
			NetPlayerInfo pInfo = netPlayerClient.getPlayerInfoByUID(uid);
			if(pInfo != null) {
				Calendar calendar = GeneralUtil.importCalendarString(message[3]);
				chatLogLobby.appendUser(getPlayerNameWithTripCode(pInfo), calendar, NetUtil.urlDecode(message[4]));
			}

		} else if("chat".equals(cmd) && message.length > 4) {
			int uid = Integer.parseInt(message[1]);
			NetPlayerInfo pInfo = netPlayerClient.getPlayerInfoByUID(uid);
			if(pInfo != null) {
				Calendar calendar = GeneralUtil.importCalendarString(message[3]);
				chatLogRoom.appendUser(getPlayerNameWithTripCode(pInfo), calendar, NetUtil.urlDecode(message[4]));
			}

		} else if(("lobbychath".equals(cmd) || "chath".equals(cmd)) && message.length > 3) {
			String strUsername = convTripCode(NetUtil.urlDecode(message[1]));
			Calendar calendar = GeneralUtil.importCalendarString(message[2]);
			String body = NetUtil.urlDecode(message[3]);
			ChatLogSDL target = "lobbychath".equals(cmd) ? chatLogLobby : chatLogRoom;
			target.appendUser(strUsername, calendar, body);

		} else if("changestatus".equals(cmd) && message.length > 2) {
			int uid = Integer.parseInt(message[2]);
			NetPlayerInfo pInfo = netPlayerClient.getPlayerInfoByUID(uid);
			if(pInfo != null) {
				String mode = message[1];
				String fmt = null;
				if("watchonly".equals(mode)) fmt = getUIText("SysMsg_StatusChange_Spectator");
				else if("joinqueue".equals(mode)) fmt = getUIText("SysMsg_StatusChange_Queue");
				else if("joinseat".equals(mode)) fmt = getUIText("SysMsg_StatusChange_Joined");
				if(fmt != null) chatLogRoom.appendSystem(String.format(fmt, getPlayerNameWithTripCode(pInfo)), NormalFontSDL.COLOR_BLUE);
			}

		} else if("autostartbegin".equals(cmd) && message.length > 1) {
			chatLogRoom.appendSystem(String.format(getUIText("SysMsg_AutoStartBegin"), message[1]), NormalFontSDL.COLOR_GREEN);

		} else if("start".equals(cmd)) {
			chatLogRoom.appendSystem(getUIText("SysMsg_GameStart"), NormalFontSDL.COLOR_GREEN);

		} else if("dead".equals(cmd) && message.length > 2) {
			String name = convTripCode(NetUtil.urlDecode(message[2]));
			if(message.length > 6) {
				chatLogRoom.appendSystem(String.format(getUIText("SysMsg_KO"),
						convTripCode(NetUtil.urlDecode(message[6])), name), NormalFontSDL.COLOR_GREEN);
			}

		} else if("finish".equals(cmd)) {
			chatLogRoom.appendSystem(getUIText("SysMsg_GameEnd"), NormalFontSDL.COLOR_GREEN);
			if(message.length > 3 && message[3].length() > 0) {
				boolean flagTeamWin = message.length > 4 && Boolean.parseBoolean(message[4]);
				String strWinner = flagTeamWin
						? String.format(getUIText("SysMsg_WinnerTeam"), NetUtil.urlDecode(message[3]))
						: String.format(getUIText("SysMsg_Winner"), convTripCode(NetUtil.urlDecode(message[3])));
				chatLogRoom.appendSystem(strWinner, NormalFontSDL.COLOR_GREEN);
			}

		} else if("rating".equals(cmd) && message.length > 5) {
			String strPlayerName = convTripCode(NetUtil.urlDecode(message[3]));
			int ratingNow = Integer.parseInt(message[4]);
			int ratingChange = Integer.parseInt(message[5]);
			chatLogRoom.appendSystem(String.format(getUIText("SysMsg_Rating"),
					strPlayerName, ratingNow, ratingChange), NormalFontSDL.COLOR_GREEN);

		} else if("mpranking".equals(cmd) && message.length >= 4) {
			int style = Integer.parseInt(message[1]);
			int myRank = Integer.parseInt(message[2]);
			if(style >= 0 && style < mpRankingRows.length) {
				String strPData = NetUtil.decompressString(message[3]);
				String[] rows = strPData.split("\t");
				String[][] decoded = new String[rows.length][];
				int validCount = 0;
				for(int i = 0; i < rows.length; i++) {
					if(rows[i].length() == 0) continue;
					String[] fields = rows[i].split(";");
					if(fields.length < 5) continue;
					String rankStr = (Integer.parseInt(fields[0]) == -1) ? "N/A" : String.valueOf(Integer.parseInt(fields[0]) + 1);
					decoded[validCount++] = new String[] {
						rankStr,
						convTripCode(NetUtil.urlDecode(fields[1])),
						fields[2],
						fields[3],
						fields[4],
					};
				}
				String[][] trimmed = new String[validCount][];
				System.arraycopy(decoded, 0, trimmed, 0, validCount);
				mpRankingRows[style] = trimmed;
				mpRankingMyRank[style] = myRank;
				mpRankingDirty = true;
			}

		} else if("changename".equals(cmd) && message.length > 3) {
			// Server broadcast: "changename\t<uid>\t<oldname>\t<newname>"
			int uid = Integer.parseInt(message[1]);
			String oldName = NetUtil.urlDecode(message[2]);
			String newName = NetUtil.urlDecode(message[3]);
			// If the rename was our own, persist the raw name (with the real
			// #tripkey, not the server's hashed ' !<code>' form) so next-session
			// login reproduces the same tripcode. Fall back to the broadcast
			// form for renames we didn't originate (e.g. admin-driven).
			if(netPlayerClient != null && uid == netPlayerClient.getPlayerUID()) {
				String toSave = (pendingOwnRaw != null) ? pendingOwnRaw : newName;
				propConfig.setProperty("serverselect.txtfldPlayerName.text", toSave);
				lastRawOwnName = toSave;
				pendingOwnRaw = null;
			}
			// Broadcast happens to everyone; post in both logs so it's visible
			// whether the user is on the lobby screen or already in a room.
			String renameMsg = String.format(getUIText("SysMsg_ChangeName"), oldName, newName);
			chatLogLobby.appendSystem(renameMsg, NormalFontSDL.COLOR_GREEN);
			chatLogRoom.appendSystem(renameMsg, NormalFontSDL.COLOR_GREEN);

		} else if("changenamefail".equals(cmd)) {
			// The attempt was rejected; drop the pending raw so it can't bleed
			// into a later successful rename by the same player.
			pendingOwnRaw = null;
			String reason = message.length > 1 ? message[1] : "UNKNOWN";
			String hint;
			if("DUPLICATE".equals(reason)) hint = "NAME ALREADY IN USE";
			else if("EMPTY".equals(reason)) hint = "NAME CANNOT BE EMPTY";
			else if("PLAYING".equals(reason)) hint = "CANNOT RENAME WHILE PLAYING";
			else hint = "RENAME FAILED: " + reason;
			// The user could have typed /name from either the lobby or a room
			// chat; post to both logs so whichever is active shows the error.
			chatLogLobby.appendSystem(hint, NormalFontSDL.COLOR_RED);
			chatLogRoom.appendSystem(hint, NormalFontSDL.COLOR_RED);

		} else if("announce".equals(cmd) && message.length > 1) {
			String strMessage = "<ADMIN>: " + NetUtil.urlDecode(message[1]);
			chatLogLobby.appendSystem(strMessage, NormalFontSDL.COLOR_RED);
			chatLogRoom.appendSystem(strMessage, NormalFontSDL.COLOR_RED);

		} else if("spdownload".equals(cmd) && message.length > 2) {
			long sChecksum = Long.parseLong(message[1]);
			Adler32 checksumObj = new Adler32();
			checksumObj.update(NetUtil.stringToBytes(message[2]));
			if(checksumObj.getValue() == sChecksum) {
				String strReplay = NetUtil.decompressString(message[2]);
				CustomProperties prop = new CustomProperties();
				prop.decode(strReplay);
				FileOutputStream out = null;
				try {
					out = new FileOutputStream("replay/netreplay.rep");
					prop.store(out, "NullpoMino NetReplay from " + netPlayerClient.getHost());
					chatLogLobby.appendSystem(getUIText("SysMsg_ReplaySaved"), NormalFontSDL.COLOR_PURPLE);
					chatLogRoom.appendSystem(getUIText("SysMsg_ReplaySaved"), NormalFontSDL.COLOR_PURPLE);
				} catch(IOException e) {
					log.error("Failed to write replay to replay/netreplay.rep", e);
				} finally {
					if(out != null) try { out.close(); } catch(IOException ignore) {}
				}
			}
		}
		// Game-stat + mpranking messages carry through to listeners / modes so the SDL
		// ranking / room states can pick them up in render.

		for(NetLobbyListener l : listeners) l.netlobbyOnMessage(this, netPlayerClient, message);
		if(netDummyMode != null) netDummyMode.netlobbyOnMessage(this, netPlayerClient, message);
	}

	/** Run disconnect handler on the game thread. */
	protected void dispatchDisconnect(Throwable ex) {
		lobbyMode = LOBBYMODE_DISCONNECTED;
		roomList.clear();

		if(ex != null) {
			chatLogLobby.appendSystem(getUIText("SysMsg_DisconnectedError") + " " + ex.getLocalizedMessage(), NormalFontSDL.COLOR_RED);
			log.info("Server Disconnected", ex);
		} else {
			chatLogLobby.appendSystem(getUIText("SysMsg_DisconnectedOK"), NormalFontSDL.COLOR_RED);
			log.info("Server Disconnected (normal)");
		}

		if(listeners != null) {
			for(NetLobbyListener l : listeners) if(l != null) l.netlobbyOnDisconnect(this, netPlayerClient, ex);
		}
		if(netDummyMode != null) netDummyMode.netlobbyOnDisconnect(this, netPlayerClient, ex);
	}

	// ---------------- Actions ----------------

	/**
	 * Open a player connection.  The caller provides the server host:port and the
	 * displayed name/team.  Starts a background reader thread on {@link NetPlayerClient}.
	 */
	public void connectToServer(String playerName, String playerTeam, String host, int port) {
		propConfig.setProperty("serverselect.txtfldPlayerName.text", playerName);
		propConfig.setProperty("serverselect.txtfldPlayerTeam.text", playerTeam);
		lastRawOwnName = (playerName == null) ? "" : playerName;
		pendingOwnRaw = null;

		netPlayerClient = new NetPlayerClient(host, port, playerName, playerTeam == null ? "" : playerTeam.trim());
		netPlayerClient.setDaemon(true);
		netPlayerClient.addListener(this);
		netPlayerClient.start();
		lastConnectAt = System.currentTimeMillis();

		chatLogLobby.clear();
		roomList.clear();
	}

	/** Send a chat message to either the lobby or the current room. */
	public void sendChat(boolean roomchat, String strMsg) {
		if(strMsg == null || strMsg.length() == 0 || netPlayerClient == null) return;
		String msg = strMsg;
		// Command dispatch is case-insensitive so '/NAME', '/Name', '/name'
		// all work identically. Argument text keeps its original case.
		String lower = msg.toLowerCase();
		if(lower.startsWith("/team")) {
			String arg = msg.length() > 5 ? msg.substring(5).trim() : "";
			netPlayerClient.send("changeteam\t" + NetUtil.urlEncode(arg) + "\n");
		} else if(lower.startsWith("/name ") || lower.equals("/name")) {
			String arg = lower.equals("/name") ? "" : msg.substring("/name ".length()).trim();
			sendChangeName(arg, roomchat);
		} else if(lower.equals("/help") || lower.equals("/?")) {
			printHelp(roomchat);
		} else if(roomchat) {
			netPlayerClient.send("chat\t" + NetUtil.urlEncode(msg) + "\n");
		} else {
			netPlayerClient.send("lobbychat\t" + NetUtil.urlEncode(msg) + "\n");
		}
	}

	/**
	 * Emit a short local-only system message listing every chat command. The log
	 * line appears in whichever chat the user typed from, so lobby /help goes to
	 * the lobby log and room /help goes to the room log.
	 */
	private void printHelp(boolean roomchat) {
		ChatLogSDL log = roomchat ? chatLogRoom : chatLogLobby;
		log.appendSystem("COMMANDS: /NAME <NICK>[#TRIP]   /TEAM [<NAME>]   /HELP", NormalFontSDL.COLOR_YELLOW);
	}

	/**
	 * Send a {@code changename} request to the server. The server handles
	 * duplicate-name checking and "cannot rename while playing" validation,
	 * and broadcasts a playerupdate on success. The /name command entry point
	 * lives in {@link #sendChat}.
	 */
	private void sendChangeName(String newName, boolean roomchat) {
		if(newName == null || newName.trim().length() == 0) {
			ChatLogSDL log = roomchat ? chatLogRoom : chatLogLobby;
			log.appendSystem("USAGE: /NAME <NICKNAME>[#TRIPCODE]", NormalFontSDL.COLOR_YELLOW);
			return;
		}
		if(netPlayerClient == null || !netPlayerClient.isConnected()) return;
		String trimmed = newName.trim();
		// Remember the raw form the server will effectively adopt: if the user
		// didn't supply a new '#tripkey', the server keeps the existing one —
		// carry the corresponding portion of our previous raw name forward so
		// the saved identity stays reproducible across sessions.
		if(trimmed.indexOf('#') != -1) {
			pendingOwnRaw = trimmed;
		} else {
			int hashIdx = (lastRawOwnName == null) ? -1 : lastRawOwnName.indexOf('#');
			pendingOwnRaw = (hashIdx == -1) ? trimmed : trimmed + lastRawOwnName.substring(hashIdx);
		}
		netPlayerClient.send("changename\t" + NetUtil.urlEncode(trimmed) + "\n");
	}

	/**
	 * Join a room (participant or watcher). Clears the room chat buffer before sending
	 * so the receiving {@code roomjoinsuccess} handler starts a clean log.
	 */
	public void joinRoom(int roomID, boolean watch) {
		if(netPlayerClient == null) return;
		NetPlayerInfo me = netPlayerClient.getYourPlayerInfo();
		if(me == null || me.roomID != roomID) {
			chatLogRoom.clear();
			netPlayerClient.send("roomjoin\t" + roomID + "\t" + watch + "\n");
		}
	}

	/**
	 * Send the player's RuleOptions to the server (compressed, checksummed).
	 * Called automatically after login, or again on {@code ruledatafail}.
	 */
	public void sendMyRuleDataToServer() {
		if(netPlayerClient == null) return;
		if(ruleOptPlayer == null) ruleOptPlayer = new RuleOptions();
		CustomProperties prop = new CustomProperties();
		ruleOptPlayer.writeProperty(prop, 0);
		String strRuleTemp = prop.encode("RuleData");
		String strRuleData = NetUtil.compressString(strRuleTemp);

		Adler32 checksumObj = new Adler32();
		checksumObj.update(NetUtil.stringToBytes(strRuleData));
		long sChecksum = checksumObj.getValue();
		netPlayerClient.send("ruledata\t" + sChecksum + "\t" + strRuleData + "\n");
	}

	// ---------------- Listener mgmt ----------------

	public void addListener(NetLobbyListener l) { listeners.add(l); }
	public boolean removeListener(NetLobbyListener l) { return listeners.remove(l); }
	public void setNetDummyMode(NetDummyMode m) { netDummyMode = m; }
	public NetDummyMode getNetDummyMode() { return netDummyMode; }

	// ---------------- Data helpers ----------------

	/** Refill {@link #sameRoomPlayerInfoList} from the player-client snapshot. */
	public LinkedList<NetPlayerInfo> updateSameRoomPlayerInfoList() {
		sameRoomPlayerInfoList.clear();
		if(netPlayerClient == null) return sameRoomPlayerInfoList;
		NetPlayerInfo me = netPlayerClient.getYourPlayerInfo();
		if(me == null) return sameRoomPlayerInfoList;
		LinkedList<NetPlayerInfo> all = new LinkedList<NetPlayerInfo>(netPlayerClient.getPlayerInfoList());
		for(NetPlayerInfo p : all) {
			if(p.roomID == me.roomID) sameRoomPlayerInfoList.add(p);
		}
		return sameRoomPlayerInfoList;
	}

	public LinkedList<NetPlayerInfo> getSameRoomPlayerInfoList() { return sameRoomPlayerInfoList; }

	/** Localization lookup — checks locale-specific, default, then the shared SDL UI dictionary. */
	public String getUIText(String key) {
		if(key == null) return "";
		String result = propLang == null ? null : propLang.getProperty(key);
		if(result == null && propLangDefault != null) result = propLangDefault.getProperty(key);
		if(result == null) {
			// Fall back to the main SDL UI dictionary (many labels are shared).
			result = nullpomino.gui.sdl.NullpoMinoSDL.getUIText(key);
			if(result == null || result.equals(key)) result = key;
		}
		return result;
	}

	public String getModeDesc(final String modeName) {
		if(modeName == null) return "";
		String norm = modeName.replace(' ', '_').replace('(', 'l').replace(')', 'r');
		String result = propModeDesc == null ? null : propModeDesc.getProperty(norm);
		if(result == null && propDefaultModeDesc != null) result = propDefaultModeDesc.getProperty(norm, norm);
		return result == null ? norm : result;
	}

	public String getPlayerNameWithTripCode(NetPlayerInfo pInfo) { return convTripCode(pInfo.strName); }

	public String convTripCode(String s) {
		// The server stores names with a space before the '!' hash marker
		// ("Bob !ABCHASH") — login sanitises '!' in the nickname portion to
		// '?' so this sequence can only ever be the tripcode separator. Strip
		// the space for display so the nickname reads as "Bob!ABCHASH" with no
		// visual gap.
		String strName = (s == null) ? "" : s.replace(" !", "!");
		if(propLang == null || !propLang.getProperty("TripSeparator_EnableConvert", false)) return strName;
		strName = strName.replace(getUIText("TripSeparator_True"), getUIText("TripSeparator_False"));
		strName = strName.replace("!", getUIText("TripSeparator_True"));
		strName = strName.replace("?", getUIText("TripSeparator_False"));
		return strName;
	}

	/** @return list of .rul files under config/rule/, sorted on non-Windows. */
	public String[] getRuleFileList() {
		File dir = new File("config/rule");
		FilenameFilter filter = new FilenameFilter() {
			public boolean accept(File d, String name) { return name.endsWith(".rul"); }
		};
		String[] list = dir.list(filter);
		if(list != null && !System.getProperty("os.name", "").startsWith("Windows")) Arrays.sort(list);
		return list;
	}

	/** Populate {@link #ruleEntries} from a list of .rul filenames. */
	public void createRuleEntries(String[] filelist) {
		ruleEntries.clear();
		if(filelist == null) return;
		for(String filename : filelist) {
			RuleEntry entry = new RuleEntry();
			File file = new File("config/rule/" + filename);
			entry.filename = filename;
			entry.filepath = file.getPath();
			CustomProperties prop = new CustomProperties();
			FileInputStream in = null;
			try {
				in = new FileInputStream(file);
				prop.load(in);
				entry.rulename = prop.getProperty("0.ruleopt.strRuleName", "");
				entry.style = prop.getProperty("0.ruleopt.style", 0);
			} catch(Exception e) {
				entry.rulename = "";
				entry.style = -1;
			} finally {
				if(in != null) try { in.close(); } catch(IOException ignore) {}
			}
			ruleEntries.add(entry);
		}
	}

	public LinkedList<RuleEntry> getSubsetEntries(int style) {
		LinkedList<RuleEntry> sub = new LinkedList<RuleEntry>();
		for(RuleEntry e : ruleEntries) if(e.style == style) sub.add(e);
		return sub;
	}

	/** Default room-list row formatter — used by the SDL lobby's room table. */
	public String[] createRoomListRowData(NetRoomInfo r) {
		String[] rowData = new String[8];
		rowData[0] = Integer.toString(r.roomID);
		rowData[1] = r.strName;
		rowData[2] = r.rated ? getUIText("RoomTable_Rated_True") : getUIText("RoomTable_Rated_False");
		rowData[3] = r.ruleLock ? r.ruleName.toUpperCase() : getUIText("RoomTable_RuleName_Any");
		rowData[4] = r.strMode;
		rowData[5] = r.playing ? getUIText("RoomTable_Status_Playing") : getUIText("RoomTable_Status_Waiting");
		rowData[6] = r.playerSeatedCount + "/" + r.maxPlayers;
		rowData[7] = Integer.toString(r.spectatorCount);
		return rowData;
	}

	// ---------------- Config persistence ----------------

	public void saveConfig() {
		if(propConfig == null) return;
		try {
			FileOutputStream out = new FileOutputStream("config/setting/netlobby.cfg");
			propConfig.store(out, "NullpoMino NetLobby Config");
			out.close();
		} catch(IOException e) {
			log.warn("Failed to save netlobby config", e);
		}
	}

	public void saveGlobalConfig() {
		if(propGlobal == null) return;
		try {
			FileOutputStream out = new FileOutputStream("config/setting/global.cfg");
			propGlobal.store(out, "NullpoMino Global Config");
			out.close();
		} catch(IOException e) {
			log.warn("Failed to save global config", e);
		}
	}

	// ---------------- Server list ----------------

	public void loadServerList() {
		serverList.clear();
		String primary = "config/setting/netlobby_serverlist.cfg";
		String fallback = GameManager.isDevBuild()
				? "config/list/netlobby_serverlist_default_dev.lst"
				: "config/list/netlobby_serverlist_default.lst";
		String devPrimary = "config/setting/netlobby_serverlist_dev.cfg";

		String src = GameManager.isDevBuild() && new File(devPrimary).exists() ? devPrimary
				: (new File(primary).exists() ? primary : fallback);

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(src));
			String line;
			while((line = br.readLine()) != null) {
				line = line.trim();
				if(line.length() > 0 && !line.startsWith("#")) serverList.add(line);
			}
		} catch(IOException ignore) {
		} finally {
			if(br != null) try { br.close(); } catch(IOException ignore2) {}
		}
	}

	public void saveServerList() {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter("config/setting/netlobby_serverlist.cfg");
			for(String s : serverList) pw.println(s);
		} catch(IOException e) {
			log.warn("Failed to save server list", e);
		} finally {
			if(pw != null) pw.close();
		}
	}

	// ---------------- Internals ----------------

	private static CustomProperties loadPropsOrEmpty(String path) {
		CustomProperties prop = new CustomProperties();
		FileInputStream in = null;
		try {
			in = new FileInputStream(path);
			prop.load(in);
		} catch(IOException ignore) {
		} finally {
			if(in != null) try { in.close(); } catch(IOException ignore) {}
		}
		return prop;
	}

	private String formatEnterRoom(NetPlayerInfo pInfo) {
		String name = getPlayerNameWithTripCode(pInfo);
		if(pInfo.strHost.length() > 0) return String.format(getUIText("SysMsg_EnterRoomWithHost"), name, pInfo.strHost);
		return String.format(getUIText("SysMsg_EnterRoom"), name);
	}

	private String formatLeaveRoom(NetPlayerInfo pInfo) {
		String name = getPlayerNameWithTripCode(pInfo);
		if(pInfo.strHost.length() > 0) return String.format(getUIText("SysMsg_LeaveRoomWithHost"), name, pInfo.strHost);
		return String.format(getUIText("SysMsg_LeaveRoom"), name);
	}

	// ---------------- Nested types ----------------

	/** Rule catalog entry produced by {@link #createRuleEntries}. */
	public static class RuleEntry {
		public String filename;
		public String filepath;
		public String rulename;
		public int style;
	}
}
