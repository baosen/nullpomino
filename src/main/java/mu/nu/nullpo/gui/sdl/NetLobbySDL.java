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
package mu.nu.nullpo.gui.sdl;

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
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.Locale;
import java.util.zip.Adler32;

import org.apache.log4j.Logger;

import mu.nu.nullpo.game.component.RuleOptions;
import mu.nu.nullpo.game.net.NetBaseClient;
import mu.nu.nullpo.game.net.NetMessageListener;
import mu.nu.nullpo.game.net.NetPlayerClient;
import mu.nu.nullpo.game.net.NetPlayerInfo;
import mu.nu.nullpo.game.net.NetRoomInfo;
import mu.nu.nullpo.game.net.NetUtil;
import mu.nu.nullpo.game.play.GameEngine;
import mu.nu.nullpo.game.play.GameManager;
import mu.nu.nullpo.game.subsystem.mode.NetDummyMode;
import mu.nu.nullpo.gui.net.NetLobby;
import mu.nu.nullpo.gui.net.NetLobbyListener;
import mu.nu.nullpo.util.CustomProperties;

/**
 * SDL version of the netplay lobby. Implements the same network protocol as
 * NetLobbyFrame but without any Swing/AWT dependencies.
 */
public class NetLobbySDL implements NetLobby, NetMessageListener {
	static final Logger log = Logger.getLogger(NetLobbySDL.class);

	/** NetPlayerClient */
	public NetPlayerClient netPlayerClient;

	/** Rule data */
	public RuleOptions ruleOptPlayer, ruleOptLock;

	/** Map list */
	public LinkedList<String> mapList;

	/** Event listeners */
	protected LinkedList<NetLobbyListener> listeners = new LinkedList<NetLobbyListener>();

	/** Current game mode */
	protected NetDummyMode netDummyMode;

	/** Property file for lobby settings */
	protected CustomProperties propConfig;

	/** Property file for global settings */
	protected CustomProperties propGlobal;

	/** Lobby log writer */
	protected PrintWriter writerLobbyLog;

	/** Room log writer */
	protected PrintWriter writerRoomLog;

	/** Rated-game rule name list */
	protected LinkedList<String>[] listRatedRuleName;

	/** Same-room player info list */
	protected LinkedList<NetPlayerInfo> sameRoomPlayerInfoList = new LinkedList<NetPlayerInfo>();

	/** Server list loaded from config */
	protected LinkedList<String> serverList = new LinkedList<String>();

	/** Player name */
	protected String playerName = "";

	/** Team name */
	protected String playerTeam = "";

	// --- NetLobby interface implementation ---

	public NetPlayerClient getNetPlayerClient() {
		return netPlayerClient;
	}

	public RuleOptions getRuleOptPlayer() {
		return ruleOptPlayer;
	}

	public void setRuleOptPlayer(RuleOptions ruleopt) {
		this.ruleOptPlayer = ruleopt;
	}

	public RuleOptions getRuleOptLock() {
		return ruleOptLock;
	}

	public LinkedList<String> getMapList() {
		return mapList;
	}

	public LinkedList<NetPlayerInfo> updateSameRoomPlayerInfoList() {
		sameRoomPlayerInfoList.clear();

		if((netPlayerClient != null) && (netPlayerClient.isConnected())) {
			int roomID = netPlayerClient.getYourPlayerInfo().roomID;

			for(NetPlayerInfo pInfo: netPlayerClient.getPlayerInfoList()) {
				if(pInfo.roomID == roomID) {
					sameRoomPlayerInfoList.add(pInfo);
				}
			}
		}

		return sameRoomPlayerInfoList;
	}

	public LinkedList<NetPlayerInfo> getSameRoomPlayerInfoList() {
		return sameRoomPlayerInfoList;
	}

	public void setNetDummyMode(NetDummyMode m) {
		netDummyMode = m;
	}

	public NetDummyMode getNetDummyMode() {
		return netDummyMode;
	}

	public void addListener(NetLobbyListener l) {
		listeners.add(l);
	}

	public boolean removeListener(NetLobbyListener l) {
		return listeners.remove(l);
	}

	@SuppressWarnings("unchecked")
	public void init() {
		// Read lobby configuration file
		propConfig = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/setting/netlobby.cfg");
			propConfig.load(in);
			in.close();
		} catch(IOException e) {}

		// Load global settings
		propGlobal = new CustomProperties();
		try {
			FileInputStream in = new FileInputStream("config/setting/global.cfg");
			propGlobal.load(in);
			in.close();
		} catch(IOException e) {}

		// Rated-game rule name list
		listRatedRuleName = new LinkedList[GameEngine.MAX_GAMESTYLE];
		for(int i = 0; i < GameEngine.MAX_GAMESTYLE; i++) {
			listRatedRuleName[i] = new LinkedList<String>();
		}

		// Map list
		mapList = new LinkedList<String>();

		// Load player name and team from config unless they were already injected by the caller
		if((playerName == null) || (playerName.length() <= 0)) {
			playerName = propConfig.getProperty("serverselect.txtfldPlayerName.text", "");
		}
		if(playerTeam == null) {
			playerTeam = "";
		}
		if(playerTeam.length() <= 0) {
			playerTeam = propConfig.getProperty("serverselect.txtfldPlayerTeam.text", "");
		}

		// Load server list
		serverList.clear();
		String serverListFile = GameManager.isDevBuild()
			? "config/setting/netlobby_serverlist_dev.cfg"
			: "config/setting/netlobby_serverlist.cfg";
		if(!loadServerList(serverListFile)) {
			String defaultFile = GameManager.isDevBuild()
				? "config/list/netlobby_serverlist_default_dev.lst"
				: "config/list/netlobby_serverlist_default.lst";
			loadServerList(defaultFile);
		}

		// Listener callback
		for(NetLobbyListener l: listeners) {
			if(l != null) l.netlobbyOnInit(this);
		}
		if(netDummyMode != null) {
			netDummyMode.netlobbyOnInit(this);
		}
	}

	public void shutdown() {
		// Save config
		saveConfig();

		// Close log writers
		if(writerLobbyLog != null) {
			writerLobbyLog.flush();
			writerLobbyLog.close();
			writerLobbyLog = null;
		}
		if(writerRoomLog != null) {
			writerRoomLog.flush();
			writerRoomLog.close();
			writerRoomLog = null;
		}

		// Disconnect
		if(netPlayerClient != null) {
			if(netPlayerClient.isConnected()) {
				netPlayerClient.send("disconnect\n");
			}
			netPlayerClient.threadRunning = false;
			netPlayerClient.interrupt();
			netPlayerClient = null;
		}

		// Listener callback
		if(listeners != null) {
			for(NetLobbyListener l: listeners) {
				l.netlobbyOnExit(this);
			}
			listeners = null;
		}
		if(netDummyMode != null) {
			netDummyMode.netlobbyOnExit(this);
			netDummyMode = null;
		}
	}

	public void setVisible(boolean visible) {
		if(visible) {
			// Connect to the first server in the list
			if(!serverList.isEmpty()) {
				connectToServer(serverList.getFirst());
			} else {
				log.error("No servers in server list");
			}
		}
	}

	// --- Connection logic ---

	/**
	 * Connect to a server
	 * @param strServer Server address in "host:port" format
	 */
	protected void connectToServer(String strServer) {
		int portSpliter = strServer.indexOf(":");
		if(portSpliter == -1) portSpliter = strServer.length();

		String strHost = strServer.substring(0, portSpliter);
		log.debug("Host:" + strHost);

		int port = NetPlayerClient.DEFAULT_PORT;
		try {
			String strPort = strServer.substring(portSpliter + 1, strServer.length());
			port = Integer.parseInt(strPort);
		} catch (Exception e2) {
			log.debug("Failed to get port number; Try to use default port");
		}
		log.debug("Port:" + port);

		netPlayerClient = new NetPlayerClient(strHost, port, playerName, playerTeam);
		netPlayerClient.setDaemon(true);
		netPlayerClient.addListener(this);
		netPlayerClient.start();
	}

	// --- NetMessageListener implementation ---

	public void netOnMessage(NetBaseClient client, String[] message) throws IOException {
		// Connection completion
		if(message[0].equals("welcome")) {
			// Create lobby log file
			if(writerLobbyLog == null) {
				try {
					GregorianCalendar currentTime = new GregorianCalendar();
					int month = currentTime.get(Calendar.MONTH) + 1;
					String filename = String.format(
							"log/lobby_%04d_%02d_%02d_%02d_%02d_%02d.txt",
							currentTime.get(Calendar.YEAR), month, currentTime.get(Calendar.DATE),
							currentTime.get(Calendar.HOUR_OF_DAY),
							currentTime.get(Calendar.MINUTE), currentTime.get(Calendar.SECOND)
					);
					writerLobbyLog = new PrintWriter(filename);
				} catch (Exception e) {
					log.warn("Failed to create lobby log file", e);
				}
			}
			if(writerRoomLog == null) {
				try {
					GregorianCalendar currentTime = new GregorianCalendar();
					int month = currentTime.get(Calendar.MONTH) + 1;
					String filename = String.format(
							"log/room_%04d_%02d_%02d_%02d_%02d_%02d.txt",
							currentTime.get(Calendar.YEAR), month, currentTime.get(Calendar.DATE),
							currentTime.get(Calendar.HOUR_OF_DAY),
							currentTime.get(Calendar.MINUTE), currentTime.get(Calendar.SECOND)
					);
					writerRoomLog = new PrintWriter(filename);
				} catch (Exception e) {
					log.warn("Failed to create room log file", e);
				}
			}
		}
		// Successful login
		if(message[0].equals("loginsuccess")) {
			log.info("Login successful: " + NetUtil.urlDecode(message[1]));
			sendMyRuleDataToServer();
		}
		// Login failure
		if(message[0].equals("loginfail")) {
			if((message.length > 1) && message[1].equals("DIFFERENT_VERSION")) {
				log.error("Login failed: Different version (client:" + GameManager.getVersionMajor() + " server:" + message[2] + ")");
			} else if((message.length > 1) && message[1].equals("DIFFERENT_BUILD")) {
				log.error("Login failed: Different build type (client:" + GameManager.getBuildTypeString() + " server:" + message[2] + ")");
			} else {
				String reason = "";
				for(int i = 1; i < message.length; i++) reason += message[i] + " ";
				log.error("Login failed: " + reason);
			}
		}
		// Banned
		if(message[0].equals("banned")) {
			log.error("Banned from server");
		}
		// Rule data transmission success
		if(message[0].equals("ruledatasuccess")) {
			log.info("Rule data sent successfully");

			// Dispatch loginOK to listeners
			for(NetLobbyListener l: listeners) {
				l.netlobbyOnLoginOK(this, netPlayerClient);
			}
			if(netDummyMode != null) netDummyMode.netlobbyOnLoginOK(this, netPlayerClient);
		}
		// Rule data transmission failure
		if(message[0].equals("ruledatafail")) {
			sendMyRuleDataToServer();
		}
		// Rule receive (for rule-locked games)
		if(message[0].equals("rulelock")) {
			if(ruleOptLock == null) ruleOptLock = new RuleOptions();

			String strRuleData = NetUtil.decompressString(message[1]);

			CustomProperties prop = new CustomProperties();
			prop.decode(strRuleData);
			ruleOptLock.readProperty(prop, 0);

			log.info("Received rule data (" + ruleOptLock.strRuleName + ")");
		}
		// Rated-game rule list
		if(message[0].equals("rulelist")) {
			int style = Integer.parseInt(message[1]);

			if(style < listRatedRuleName.length) {
				listRatedRuleName[style].clear();

				for(int i = 0; i < message.length - 2; i++) {
					String name = NetUtil.urlDecode(message[2 + i]);
					listRatedRuleName[style].add(name);
				}
			}
		}
		// Room create/join success
		if(message[0].equals("roomcreatesuccess") || message[0].equals("roomjoinsuccess")) {
			int roomID = Integer.parseInt(message[1]);
			int seatID = Integer.parseInt(message[2]);
			int queueID = Integer.parseInt(message[3]);

			netPlayerClient.getYourPlayerInfo().roomID = roomID;
			netPlayerClient.getYourPlayerInfo().seatID = seatID;
			netPlayerClient.getYourPlayerInfo().queueID = queueID;

			if(roomID != -1) {
				NetRoomInfo roomInfo = netPlayerClient.getRoomInfo(roomID);

				// Dispatch roomJoin to listeners
				for(NetLobbyListener l: listeners) {
					l.netlobbyOnRoomJoin(this, netPlayerClient, roomInfo);
				}
				if(netDummyMode != null) netDummyMode.netlobbyOnRoomJoin(this, netPlayerClient, roomInfo);
			} else {
				// Returned to lobby
				for(NetLobbyListener l: listeners) {
					l.netlobbyOnRoomLeave(this, netPlayerClient);
				}
				if(netDummyMode != null) netDummyMode.netlobbyOnRoomLeave(this, netPlayerClient);
			}
		}
		// Map receive
		if(message[0].equals("map")) {
			String strDecompressed = NetUtil.decompressString(message[1]);
			String[] strMaps = strDecompressed.split("\t");

			mapList.clear();

			int maxMap = strMaps.length;
			for(int i = 0; i < maxMap; i++) {
				mapList.add(strMaps[i]);
			}

			log.debug("Received " + mapList.size() + " maps");
		}
		// Replay data
		if(message[0].equals("replay")) {
			long sChecksum = Long.parseLong(message[1]);

			Adler32 checksumObj = new Adler32();
			checksumObj.update(NetUtil.stringToBytes(message[2]));

			if(checksumObj.getValue() == sChecksum) {
				String strReplay = NetUtil.decompressString(message[2]);
				CustomProperties prop = new CustomProperties();
				prop.decode(strReplay);

				try {
					FileOutputStream out = new FileOutputStream("replay/netreplay.rep");
					prop.store(out, "NullpoMino NetReplay from " + netPlayerClient.getHost());
					out.close();
					log.info("Replay saved to replay/netreplay.rep");
				} catch (IOException e) {
					log.error("Failed to write replay to replay/netreplay.rep", e);
				}
			}
		}

		// Dispatch all messages to listeners and mode
		if(listeners != null) {
			for(NetLobbyListener l: listeners) {
				l.netlobbyOnMessage(this, netPlayerClient, message);
			}
		}
		if(netDummyMode != null) netDummyMode.netlobbyOnMessage(this, netPlayerClient, message);
	}

	public void netOnDisconnect(NetBaseClient client, Throwable ex) {
		if(ex != null) {
			log.info("Server Disconnected", ex);
		} else {
			log.info("Server Disconnected");
		}

		// Dispatch disconnect to listeners
		if(listeners != null) {
			for(NetLobbyListener l: listeners) {
				if(l != null) {
					l.netlobbyOnDisconnect(this, netPlayerClient, ex);
				}
			}
		}
		if(netDummyMode != null) netDummyMode.netlobbyOnDisconnect(this, netPlayerClient, ex);
	}

	// --- Helper methods ---

	/**
	 * Send the player's rule data to the server
	 */
	public void sendMyRuleDataToServer() {
		if(ruleOptPlayer == null) ruleOptPlayer = new RuleOptions();

		CustomProperties prop = new CustomProperties();
		ruleOptPlayer.writeProperty(prop, 0);
		String strRuleTemp = prop.encode("RuleData");
		String strRuleData = NetUtil.compressString(strRuleTemp);
		log.debug("RuleData uncompressed:" + strRuleTemp.length() + " compressed:" + strRuleData.length());

		// Checksum calculation
		Adler32 checksumObj = new Adler32();
		checksumObj.update(NetUtil.stringToBytes(strRuleData));
		long sChecksum = checksumObj.getValue();

		// Send
		netPlayerClient.send("ruledata\t" + sChecksum + "\t" + strRuleData + "\n");
	}

	/**
	 * Load server list from a file
	 * @param filename File to load from
	 * @return true if successful
	 */
	protected boolean loadServerList(String filename) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String str;
			while((str = in.readLine()) != null) {
				str = str.trim();
				if(str.length() > 0) {
					serverList.add(str);
				}
			}
			in.close();
			return !serverList.isEmpty();
		} catch(IOException e) {
			return false;
		}
	}

	/**
	 * Save lobby config (player name, team, etc.)
	 */
	protected void saveConfig() {
		if(propConfig == null) return;

		propConfig.setProperty("serverselect.txtfldPlayerName.text", playerName);
		propConfig.setProperty("serverselect.txtfldPlayerTeam.text", playerTeam);

		try {
			FileOutputStream out = new FileOutputStream("config/setting/netlobby.cfg");
			propConfig.store(out, "NullpoMino NetLobby Config");
			out.close();
		} catch (IOException e) {
			log.warn("Failed to save NetLobby config file", e);
		}
	}
}
