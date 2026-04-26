// SPDX-FileCopyrightText: 2010 NullNoname
// SPDX-License-Identifier: BSD-3-Clause
package nullpomino.game.net;

import java.io.Serializable;
import java.nio.channels.SocketChannel;
import java.util.Arrays;

import nullpomino.game.component.RuleOptions;
import nullpomino.game.play.GameEngine;

/**
 * Player information
 */
public class NetPlayerInfo implements Serializable {
	/** Serial version */
	private static final long serialVersionUID = 1L;

	private static final int EXPORT_FIELD_COUNT = 27;
	private static final int RATING_INDEX = 12;
	private static final int PLAY_COUNT_INDEX = 16;
	private static final int WIN_COUNT_INDEX = 20;
	private static final int SP_PERSONAL_BEST_INDEX = 24;
	private static final int PLAY_COUNT_NOW_INDEX = 25;
	private static final int WIN_COUNT_NOW_INDEX = 26;

	/** Default rating for multiplayer games */
	public static final int DEFAULT_MULTIPLAYER_RATING = 1500;

	/** Name */
	public String strName = "";

	/** Country code */
	public String strCountry = "";

	/** Host */
	public String strHost = "";

	/** Team name */
	public String strTeam = "";

	/** Rules in use */
	public RuleOptions ruleOpt = null;

	/** Multiplayer rating */
	public int[] rating = new int[GameEngine.MAX_GAMESTYLE];

	/** Rating backup (internal use) */
	public int[] ratingBefore = new int[GameEngine.MAX_GAMESTYLE];

	/** Number of rated multiplayer games played */
	public int[] playCount = new int[GameEngine.MAX_GAMESTYLE];

	/** Number of games played in current room */
	public int playCountNow = 0;

	/** Number of rated multiplayer games win */
	public int[] winCount = new int[GameEngine.MAX_GAMESTYLE];

	/** Number of wins in current room */
	public int winCountNow = 0;

	/** Single player personal records */
	public NetSPPersonalBest spPersonalBest = new NetSPPersonalBest();

	/** User ID */
	public int uid = -1;

	/** Current room ID */
	public int roomID = -1;

	/** Game seat number (-1 if spectator) */
	public int seatID = -1;

	/** Join queue number (-1 if not in queue) */
	public int queueID = -1;

	/** true if "Ready" sign */
	public boolean ready = false;

	/** true if playing now */
	public boolean playing = false;

	/** true if connected */
	public boolean connected = false;

	/** true if this player is using tripcode */
	public boolean isTripUse = false;

	/** Real host name (for internal use) */
	public String strRealHost = "";

	/** Real IP (for internal use) */
	public String strRealIP = "";

	/** SocketChannel of this player (for internal use) */
	public SocketChannel channel = null;

	/**
	 * Constructor
	 */
	public NetPlayerInfo() {
	}

	/**
	 * Copy constructor
	 * @param n Copy source
	 */
	public NetPlayerInfo(NetPlayerInfo n) {
		copy(n);
	}

	/**
	 * String array constructor (Uses importStringArray)
	 * @param pdata String array (String[12])
	 */
	public NetPlayerInfo(String[] pdata) {
		importStringArray(pdata);
	}

	/**
	 * String constructor (Uses importString)
	 * @param str String(Divided by ;)
	 */
	public NetPlayerInfo(String str) {
		importString(str);
	}

	/**
	 * Copy from other NetPlayerInfo
	 * @param n Copy source
	 */
	public void copy(NetPlayerInfo n) {
		strName = n.strName;
		strCountry = n.strCountry;
		strHost = n.strHost;
		strTeam = n.strTeam;

		if(n.ruleOpt != null) {
			ruleOpt = new RuleOptions(n.ruleOpt);
		} else {
			ruleOpt = null;
		}

		rating = Arrays.copyOf(n.rating, GameEngine.MAX_GAMESTYLE);
		ratingBefore = Arrays.copyOf(n.ratingBefore, GameEngine.MAX_GAMESTYLE);
		playCount = Arrays.copyOf(n.playCount, GameEngine.MAX_GAMESTYLE);
		winCount = Arrays.copyOf(n.winCount, GameEngine.MAX_GAMESTYLE);
		spPersonalBest = new NetSPPersonalBest(n.spPersonalBest);

		playCountNow = n.playCountNow;
		winCountNow = n.winCountNow;

		uid = n.uid;
		roomID = n.roomID;
		seatID = n.seatID;
		queueID = n.queueID;
		ready = n.ready;
		playing = n.playing;
		connected = n.connected;
		isTripUse = n.isTripUse;
		strRealHost = n.strRealHost;
		strRealIP = n.strRealIP;
		channel = n.channel;
	}

	/**
	 * Import from String array
	 * @param pdata String array (String[27])
	 */
	public void importStringArray(String[] pdata) {
		strName = NetUtil.urlDecode(pdata[0]);
		strCountry = NetUtil.urlDecode(pdata[1]);
		strHost = NetUtil.urlDecode(pdata[2]);
		strTeam = NetUtil.urlDecode(pdata[3]);
		roomID = Integer.parseInt(pdata[4]);
		uid = Integer.parseInt(pdata[5]);
		seatID = Integer.parseInt(pdata[6]);
		queueID = Integer.parseInt(pdata[7]);
		ready = Boolean.parseBoolean(pdata[8]);
		playing = Boolean.parseBoolean(pdata[9]);
		connected = Boolean.parseBoolean(pdata[10]);
		isTripUse = Boolean.parseBoolean(pdata[11]);
		readIntArray(pdata, RATING_INDEX, rating);
		readIntArray(pdata, PLAY_COUNT_INDEX, playCount);
		readIntArray(pdata, WIN_COUNT_INDEX, winCount);
		if(pdata.length > SP_PERSONAL_BEST_INDEX) {
			spPersonalBest.importString(NetUtil.decompressString(pdata[SP_PERSONAL_BEST_INDEX]));
		}
		if(pdata.length > PLAY_COUNT_NOW_INDEX) playCountNow = Integer.parseInt(pdata[PLAY_COUNT_NOW_INDEX]);
		if(pdata.length > WIN_COUNT_NOW_INDEX) winCountNow = Integer.parseInt(pdata[WIN_COUNT_NOW_INDEX]);
	}

	/**
	 * Import from String (Divided by ;)
	 * @param str String
	 */
	public void importString(String str) {
		importStringArray(str.split(";", -1));
	}

	/**
	 * Export to String array
	 * @return String array (String[27])
	 */
	public String[] exportStringArray() {
		String[] pdata = new String[EXPORT_FIELD_COUNT];
		pdata[0] = NetUtil.urlEncode(strName);
		pdata[1] = NetUtil.urlEncode(strCountry);
		pdata[2] = NetUtil.urlEncode(strHost);
		pdata[3] = NetUtil.urlEncode(strTeam);
		pdata[4] = Integer.toString(roomID);
		pdata[5] = Integer.toString(uid);
		pdata[6] = Integer.toString(seatID);
		pdata[7] = Integer.toString(queueID);
		pdata[8] = Boolean.toString(ready);
		pdata[9] = Boolean.toString(playing);
		pdata[10] = Boolean.toString(connected);
		pdata[11] = Boolean.toString(isTripUse);
		writeIntArray(pdata, RATING_INDEX, rating);
		writeIntArray(pdata, PLAY_COUNT_INDEX, playCount);
		writeIntArray(pdata, WIN_COUNT_INDEX, winCount);
		pdata[SP_PERSONAL_BEST_INDEX] = NetUtil.compressString(spPersonalBest.exportString());
		pdata[PLAY_COUNT_NOW_INDEX] = Integer.toString(playCountNow);
		pdata[WIN_COUNT_NOW_INDEX] = Integer.toString(winCountNow);
		return pdata;
	}

	/**
	 * Export to String (Divided by ;)
	 * @return String
	 */
	public String exportString() {
		return String.join(";", exportStringArray());
	}

	private static void readIntArray(String[] source, int startIndex, int[] target) {
		for(int i = 0; i < target.length; i++) {
			target[i] = Integer.parseInt(source[startIndex + i]);
		}
	}

	private static void writeIntArray(String[] target, int startIndex, int[] source) {
		for(int i = 0; i < source.length; i++) {
			target[startIndex + i] = Integer.toString(source[i]);
		}
	}

	/**
	 * Reset play flags
	 */
	public void resetPlayState() {
		ready = false;
		playing = false;
	}

	/**
	 * Delete this player
	 */
	public void delete() {
		ruleOpt = null;
	}
}
